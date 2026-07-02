#!/bin/sh
# Launcher for orderservice.service.
# DT_RELEASE_VERSION is injected by systemd from /opt/orderservice/release.env,
# which CI/CD writes on every deploy. Falls back to the v1 label for manual runs.

set -eu

export DT_RELEASE_PRODUCT="order-service-sample"
export DT_RELEASE_VERSION="${DT_RELEASE_VERSION:-1.0-sdkv1}"
export DT_RELEASE_STAGE="demo"
export DT_TAGS="app=orderservice"

exec /usr/bin/java -jar /opt/orderservice/app.jar
