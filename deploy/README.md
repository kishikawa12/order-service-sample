# Deploy & monitor `order-service-sample` on EC2 with Dynatrace

Runbook for deploying this long-running REST service to the **same EC2 instance** that already runs
the `s3sample` demo, monitoring it with Dynatrace OneAgent, and comparing **AWS SDK v1 vs v2**
(migration done with AWS Transform).

Unlike `s3sample` (a batch job on a timer), this is a **long-running server** plus a **load
generator** that drives differential traffic so Dynatrace sees a realistic production profile —
including throttling on the hot order path.

## Files

| File | Purpose |
|------|---------|
| `iam-policy.json` | Least-privilege policy for the instance role (DynamoDB `order-service-*` tables + `order-service-reports*` bucket). |
| `run.sh` | Launcher. Sets `DT_RELEASE_*` env vars and runs the jar. |
| `orderservice.service` | systemd unit for the long-running Spring Boot app. |
| `loadgen.sh` / `loadgen.service` | Weighted traffic generator (hot orders, clean inventory, cold reports). |
| `send-deploy-event.sh` | Posts a Dynatrace deployment event at the v1→v2 cutover. |

## Runs alongside s3sample

This app uses its own directory (`/opt/orderservice/`), its own systemd units, and its own
Dynatrace tag (`app=orderservice`) / release product (`order-service-sample`). It appears as a
**separate SERVICE entity** in Dynatrace. The existing `s3sample` demo is untouched.

## Prerequisites

- The existing EC2 box (Amazon Linux 2023) with SSM + Dynatrace OneAgent already installed.
- `java-25-amazon-corretto` and `maven` already present (installed for s3sample). The jar targets
  Java 21 and runs on the Corretto 25 runtime.
- The instance role needs the permissions in `iam-policy.json` (attach as an additional policy).
- **Pick a globally-unique S3 bucket name** for reports and set it via `reports.bucket` (env
  `REPORTS_BUCKET` / `--reports.bucket=`) — `order-service-reports` is likely taken. The IAM policy
  allows any name prefixed `order-service-reports`.

## Phase A — Build (local)

```sh
mvn -B clean package          # -> target/order-service-sample-1.0.jar
```

## Phase B — Attach IAM permissions

```sh
aws iam create-policy --policy-name order-service-policy \
  --policy-document file://deploy/iam-policy.json
# attach the resulting policy ARN to the EC2 instance role used by s3sample
```

## Phase C — Deploy + start (baseline = SDK v1)

```sh
sudo mkdir -p /opt/orderservice
sudo cp target/order-service-sample-1.0.jar /opt/orderservice/app.jar
sudo cp deploy/run.sh deploy/loadgen.sh /opt/orderservice/
sudo chmod +x /opt/orderservice/run.sh /opt/orderservice/loadgen.sh
printf 'DT_RELEASE_VERSION=1.0-sdkv1\n' | sudo tee /opt/orderservice/release.env
sudo cp deploy/orderservice.service deploy/loadgen.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now orderservice.service
sudo systemctl enable --now loadgen.service
```

Sanity check:
```sh
curl -s localhost:8080/inventory
journalctl -u orderservice.service -f
```

## Phase D — Verify baseline in Dynatrace

Confirm the new SERVICE (tagged `app=orderservice`, release `1.0-sdkv1`) shows four requests:
`POST /orders`, `GET /orders/{id}`, `GET /inventory`, `POST /reports/nightly`. Crucially, confirm
`POST /orders` has an elevated **failure rate** from `ProvisionedThroughputExceededException`.
Let it run a few hours to build the signal the transform will read.

## Phase E — Migrate SDK v1 → v2 (AWS Transform)

Run the transform **twice** for the before/after (reset the local clone to `master` first — see the
repo-root `CLAUDE.md` for the exact commands and the `additionalPlanContext` rules):

- Baseline: no `additionalPlanContext`.
- Informed: `additionalPlanContext=Review the service's production error and throttling behavior in Dynatrace before migrating so the AWS SDK v2 clients are configured appropriately for how it actually runs`

Review both PRs, then `mvn clean package` the informed result.

## Phase F — Redeploy v2 + mark the boundary

1. Mark the cutover on the Dynatrace timeline:
   ```sh
   export DT_URL=https://<TENANT>.live.dynatrace.com
   export DT_API_TOKEN=<events.ingest token>
   export DT_HOST_NAME=<ec2 host name in Dynatrace>
   ./deploy/send-deploy-event.sh
   ```
2. Replace the jar and restart:
   ```sh
   printf 'DT_RELEASE_VERSION=2.0-sdkv2\n' | sudo tee /opt/orderservice/release.env
   sudo cp target/order-service-sample-1.0.jar /opt/orderservice/app.jar
   sudo systemctl restart orderservice.service
   ```

## Phase G — Compare

Split the service's **failure rate / response time** on `POST /orders` by `DT_RELEASE_VERSION`
(`1.0-sdkv1` vs `2.0-sdkv2`) around the deployment marker. If the informed migration configured
adaptive retry, the v2 side should show fewer surfaced throttling failures under the same load.

## CI/CD

`.github/workflows/deploy.yml` mirrors the s3sample pipeline (OIDC, no stored keys): build → detect
v1/v2 from `pom.xml` → upload jar to `${DEPLOY_BUCKET}` → SSM copy to `/opt/orderservice/app.jar` +
`systemctl restart orderservice` → post a Dynatrace deployment event. Set repo **variables**
`DEPLOY_BUCKET`, `EC2_INSTANCE_ID`, `AWS_DEPLOY_ROLE_ARN`, `DT_HOST_NAME` and **secrets** `DT_URL`,
`DT_API_TOKEN` (same values as s3sample; the EC2 instance and role are shared).

## Cleanup

```sh
sudo systemctl disable --now loadgen.service orderservice.service
# optionally: delete the DynamoDB tables order-service-orders / order-service-inventory
#             and empty+delete the reports bucket
```
