#!/bin/sh
# Posts a Dynatrace deployment event so charts get a marker line at the moment you cut over
# from SDK v1 to v2. Run on the redeploy.
#
# Required env vars:
#   DT_URL        e.g. https://abc12345.live.dynatrace.com
#   DT_API_TOKEN  API token with the events.ingest scope
#   DT_HOST_NAME  the EC2 host name as it appears in Dynatrace
#
# Optional (defaults shown):
#   DEPLOY_VERSION  version label of the NEW release   (default 2.0-sdkv2)
#   DEPLOY_NAME     deployment name                     (default sdk-v1-to-v2)
#   DEPLOY_TITLE    event title on the timeline         (default "Migrate order-service to AWS SDK v2")

set -eu

: "${DT_URL:?set DT_URL}"
: "${DT_API_TOKEN:?set DT_API_TOKEN}"
: "${DT_HOST_NAME:?set DT_HOST_NAME}"

DEPLOY_VERSION="${DEPLOY_VERSION:-2.0-sdkv2}"
DEPLOY_NAME="${DEPLOY_NAME:-sdk-v1-to-v2}"
DEPLOY_TITLE="${DEPLOY_TITLE:-Migrate order-service to AWS SDK v2}"

curl -sS -X POST "${DT_URL%/}/api/v2/events/ingest" \
  -H "Authorization: Api-Token ${DT_API_TOKEN}" \
  -H "Content-Type: application/json" \
  -d @- <<JSON
{
  "eventType": "CUSTOM_DEPLOYMENT",
  "title": "${DEPLOY_TITLE}",
  "entitySelector": "type(HOST),entityName(\"${DT_HOST_NAME}\")",
  "properties": {
    "dt.event.deployment.version": "${DEPLOY_VERSION}",
    "dt.event.deployment.name": "${DEPLOY_NAME}"
  }
}
JSON
