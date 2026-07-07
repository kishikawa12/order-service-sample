# order-service-sample — Claude Code Notes

A net-new Spring Boot + AWS SDK v1 order service, built to demonstrate that feeding
**Dynatrace** production data into **AWS Transform** produces a smarter SDK v1→v2 migration
than stock. Anchor use case: the transform configures v2 client **resilience/retry** to match
real production throttling. Deployed **alongside** the `aws-java-sample`
/ `s3sample` app on the same EC2 host as its own OneAgent-detected SERVICE.

The notes below are **lessons learned about running `atx` (AWS Transform) itself** — carried over
so any future Claude Code session has the operational context, independent of this app's code.

---

## atx ct Overview

The `atx` binary lives at `~/.local/bin/atx` (not on PATH — use the full path).

### Key directories

| Path | Contents |
|------|----------|
| `~/.atxct/` | Source connections, repo index, local repo clones |
| `~/.atxct/sources/<source>/repos/<repo>/` | Local clone the planner mutates during a transform |
| `~/.aws/atx/mcp.json` | MCP client config (Dynatrace etc.) read by the atx planner |
| `~/.aws/atx/mcp-tools-cache.json` | Cached Dynatrace tool schemas (refreshed by `atx mcp tools`) |
| `~/.aws/atx/custom/<run-id>/` | Per-run logs, artifacts, MCP usage |
| `~/.aws/atx/logs/debug.log` | Global atx debug log |

> Substitute the real `<source>::<repo>` slug once this repo is connected as an atx source
> (the aws-java-sample used `github-enterprise::aws-java-sample`).

### Commands

```bash
~/.local/bin/atx ct server                  # start local API server on :8081
~/.local/bin/atx ct schema                  # full JSON manifest of all commands
~/.local/bin/atx mcp tools                  # list MCP servers the planner can call
~/.local/bin/atx mcp tools --server dynatrace-mcp-prod
~/.local/bin/atx ct remediation list --json                 # most-recent first
~/.local/bin/atx ct remediation status --id <ID> --json
```

`atx ct remediation create` has **no `--json` flag** — to capture the new ID, run `create`
then read `remediation list --json | .[0].id`.

---

## Dynatrace MCP Integration

The atx planner reads `~/.aws/atx/mcp.json` — this is **separate** from the VS Code / Claude Code
MCP settings (`.vscode/mcp.json`, `.claude/settings.json`).

When `additionalPlanContext` tells it to use Dynatrace, the planner calls:
1. `find_entity_by_name` — locate the service entity
2. `generate_dql_from_natural_language` — build DQL from the context
3. `execute_dql` — run the query (auto-retries on syntax errors)

Both remote (default) and `--local` runs execute `atx custom def exec` on the local machine —
remote routes through the AWS FES backend which calls back to the local `atx ct server`.
Dynatrace calls are visible in the per-run logs in **either** mode.

> Known planner quirk: OneAgent traces the app's **entry points** (e.g. auto-instrumented HTTP
> requests / a custom method), **not** per-AWS-SDK-call spans. Dynatrace shows per-endpoint /
> per-method invocation counts and error rates, not "putObject was called N times." Design the
> app so the signal you need lives at the entry-point level.

### Reading execution logs for a run

Each run writes to `~/.aws/atx/custom/<YYYYMMDD_HHMMSS_hash>/`. Match the dir to the remediation's
`created_at` (UTC):

```bash
# e.g. created_at 2026-07-01T19:03:51Z → look for 20260701_1903*
ls ~/.aws/atx/custom/ | grep 20260701_1903

cat ~/.aws/atx/custom/<run-id>/mcp_usage.json                 # tool call counts + lastError
grep -i "execute_dql\|dynatrace\|find_entity" \
  ~/.aws/atx/custom/<run-id>/logs/*-conversation.log          # full DQL request/result
cat ~/.aws/atx/custom/<run-id>/artifacts/worklog.log          # human-readable summary
```

---

## Common Issues & Gotchas

### Stuck server (port 8081 hangs)

If `atx ct` commands time out, a suspended old server process is likely holding port 8081:

```bash
lsof -i :8081          # should show exactly ONE node process in LISTEN state
ps aux | grep "atx ct server" | grep -v grep
kill -9 <stale-pid>    # kill any process in state T (stopped/suspended) or duplicates
```

### Caching — planner sees already-migrated code

The local clone (`~/.atxct/sources/<source>/repos/<repo>/`) is left on the last
`atx-result-staging-*` branch after each run. The next run starts from that branch — if it already
has the migration, the planner concludes "already done" and makes no changes.

**Symptom:** conversation log says "already fully migrated" early on.
**Fix — run before every remediation** (substitute this repo's clone path):

```bash
CLONE=~/.atxct/sources/<source>/repos/order-service-sample
git -C "$CLONE" checkout master
git -C "$CLONE" branch | grep atx-result-staging | xargs -r git -C "$CLONE" branch -D
```

### `additionalPlanContext` rejected by the security filter

The ATX backend runs a content filter on the config string. It rejects:
- imperative file/VCS verbs — "write a file", "commit", "delete"
- strings with **commas** when passed as `key=value` (the parser splits on `,`)

**Symptom:** `User content contains potentially malicious patterns...` or
`Invalid configuration format. Must be file:// URL, JSON string, or key=value pairs`.

**Keep the context plain, comma-free, and free of write/commit/delete verbs.** Realistic,
low-clue phrasing works and passes:

```
additionalPlanContext=Review the service's production error and throttling behavior in Dynatrace before migrating so the AWS SDK v2 clients are configured appropriately for how it actually runs
```

If you must use commas/special chars, pass JSON instead: `-g '{"additionalPlanContext":"..."}'`.

### `Severity: unknown | Category: unknown` in the PR / status is EXPECTED

When you run via `--transformation-name` **without** `--ids`, there is no linked finding record,
so `finding_snapshot` has nothing to populate and defaults to `unknown`. It is **not** an error and
**not** a Dynatrace problem — just empty metadata. Severity/category are only populated when
remediating an actual finding (`--ids`).
