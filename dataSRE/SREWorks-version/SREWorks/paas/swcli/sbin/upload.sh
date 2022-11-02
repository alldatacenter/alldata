#!/bin/bash

set -x
set -e

ROOT_DIR=/app
OSS_ENDPOINT=oss-accelerate.aliyuncs.com
OSSUTIL_URL=$1
OSS_ACCESS_KEY_ID=$2
OSS_ACCESS_KEY_SECRET=$3
OSS_PATH="oss://abm-storage"
apk add --update --no-cache libc6-compat
wget -q -O /ossutil ${OSSUTIL_URL}
chmod +x /ossutil
/ossutil cp -f /swcli-linux-amd64 ${OSS_PATH}/swcli-linux-amd64 --endpoint=${OSS_ENDPOINT} --access-key-id=${OSS_ACCESS_KEY_ID} --access-key-secret=${OSS_ACCESS_KEY_SECRET}
/ossutil cp -f /swcli-linux-arm64 ${OSS_PATH}/swcli-linux-arm64 --endpoint=${OSS_ENDPOINT} --access-key-id=${OSS_ACCESS_KEY_ID} --access-key-secret=${OSS_ACCESS_KEY_SECRET}
/ossutil cp -f /swcli-darwin-amd64 ${OSS_PATH}/swcli-darwin-amd64 --endpoint=${OSS_ENDPOINT} --access-key-id=${OSS_ACCESS_KEY_ID} --access-key-secret=${OSS_ACCESS_KEY_SECRET}
/ossutil cp -f /swcli-windows-amd64 ${OSS_PATH}/swcli-windows-amd64 --endpoint=${OSS_ENDPOINT} --access-key-id=${OSS_ACCESS_KEY_ID} --access-key-secret=${OSS_ACCESS_KEY_SECRET}
/ossutil set-acl ${OSS_PATH}/swcli-linux-amd64 public-read --endpoint=${OSS_ENDPOINT} --access-key-id=${OSS_ACCESS_KEY_ID} --access-key-secret=${OSS_ACCESS_KEY_SECRET}
/ossutil set-acl ${OSS_PATH}/swcli-linux-arm64 public-read --endpoint=${OSS_ENDPOINT} --access-key-id=${OSS_ACCESS_KEY_ID} --access-key-secret=${OSS_ACCESS_KEY_SECRET}
/ossutil set-acl ${OSS_PATH}/swcli-darwin-amd64 public-read --endpoint=${OSS_ENDPOINT} --access-key-id=${OSS_ACCESS_KEY_ID} --access-key-secret=${OSS_ACCESS_KEY_SECRET}
/ossutil set-acl ${OSS_PATH}/swcli-windows-amd64 public-read --endpoint=${OSS_ENDPOINT} --access-key-id=${OSS_ACCESS_KEY_ID} --access-key-secret=${OSS_ACCESS_KEY_SECRET}
