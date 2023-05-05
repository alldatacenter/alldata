#!/bin/bash

set -o errexit -o nounset -o pipefail
source env.sh

if [[ -z "${NETDATA_CI_PATH}" ]]; then
    echo "no ci path"
    exit 0
fi
find "/netdata/${NETDATA_CI_PATH}" -type d -maxdepth 1 -mtime +1 -exec rm -rfv {} \;
