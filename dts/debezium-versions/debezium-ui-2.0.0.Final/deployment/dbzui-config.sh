#!/bin/sh
# vim:sw=4:ts=4:et

set -e

if [ -z "${DEBEZIUM_BASE_URI:-}" ]; then
    DEBEZIUM_BASE_URI="http://127.0.0.1:8080/api"
fi

echo "Starting Debezium UI with base URI: ${DEBEZIUM_BASE_URI}"

echo 'window.UI_CONFIG={"artifacts":{"type":"rest","url":"'"${DEBEZIUM_BASE_URI}"'"},"deployment.mode":"validation.disabled","mode":"prod"};' > /usr/share/nginx/html/config.js
