#!/usr/bin/env bash

CURDIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
. $CURDIR/../shell_config.sh

user=readonly
# Support IPv6 host, the first case is ipv6 the second is not
if [[ ${CLICKHOUSE_HOST} =~ .*:.* ]]; then
    address=[${CLICKHOUSE_HOST}]
else
    address=${CLICKHOUSE_HOST}
fi
port=${CLICKHOUSE_PORT_HTTP}
url="${CLICKHOUSE_PORT_HTTP_PROTO}://readonly@$address:$port/?session_id=test"
select="SELECT name, value, changed FROM system.settings WHERE name = 'readonly'"

${CLICKHOUSE_CURL} -sS $url --data-binary "$select"
