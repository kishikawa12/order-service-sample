# order-service-sample

A Spring Boot REST order service on **AWS SDK v1** (DynamoDB + S3), built to demonstrate that
feeding **Dynatrace** production telemetry into **AWS Transform** produces a smarter SDK v1→v2
migration than the stock transform.

## The point of the demo

AWS Transform mechanically ports SDK v1 → v2. The interesting question is what it does with client
**resilience configuration**. SDK v2 changed the retry model entirely (`RetryPolicy` →
`RetryStrategy`; `STANDARD` vs `ADAPTIVE` retry modes).

- **Stock transform** maps v1's retry config to a v2 default — one-size-fits-all.
- **Dynatrace-informed transform** sees that the `POST /orders` path is *throttling in production*
  (`ProvisionedThroughputExceededException`) and has the context to configure **adaptive retry**
  on that specific DynamoDB client, while leaving the clean paths on defaults.

That difference — a concrete, reviewable code diff justified by real telemetry — is the demo.

## How the app creates the signal

OneAgent auto-instruments each REST endpoint as a distinct service request (no custom entry-point
config), so Dynatrace reports per-endpoint traffic and error rates out of the box.

| Endpoint | AWS v1 call | Profile | Dynatrace shows |
|---|---|---|---|
| `POST /orders` | DynamoDB `putItem` | hot write, **under-provisioned table (1 WCU)** | high traffic **+ throttling errors** |
| `GET /orders/{id}` | DynamoDB `getItem` | hot read | high traffic |
| `GET /inventory` | DynamoDB `scan` (on-demand table) | clean | moderate traffic, no errors |
| `POST /reports/nightly` | S3 `putObject` | cold, ~1/day | low traffic |

The under-provisioned orders table + the load generator (`deploy/loadgen.sh`) produce **real**
throttling — the canonical AWS scenario that maps to adaptive retry.

## Running the migration comparison

1. Deploy the app and let the load generator drive differential traffic for a few hours (see
   [deploy/README.md](deploy/README.md)). Confirm in Dynatrace that `POST /orders` shows throttling.
2. Run AWS Transform **twice** on a clean branch each time (reset the local clone to `master`
   first — see `CLAUDE.md`):
   - **Baseline:** no `additionalPlanContext`.
   - **Informed:** with a realistic, low-clue context that points the planner at production behavior:
     ```
     additionalPlanContext=Review the service's production error and throttling behavior in Dynatrace before migrating so the AWS SDK v2 clients are configured appropriately for how it actually runs
     ```
3. Diff the two PRs. The informed run should configure `RetryMode.ADAPTIVE` (plus tuned
   timeout/backoff) on the orders DynamoDB client and prioritize the hot paths; the baseline
   applies a uniform default.

> Operational notes for running AWS Transform (`atx`) — commands, Dynatrace MCP log locations, the
> caching fix, the `additionalPlanContext` security-filter rules — are in `CLAUDE.md`.

## Build

```sh
mvn clean package     # -> target/order-service-sample-1.0.jar (self-contained)
```

Targets Java 21 (runs fine on the Corretto 25 runtime on EC2). AWS SDK v1 `1.12.600`.
