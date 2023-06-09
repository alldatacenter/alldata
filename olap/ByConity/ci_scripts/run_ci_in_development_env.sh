#!/bin/bash

export BYCONITY_CONF_PATH=/etc/byconity
export BYCONITY_BIN=../build/programs
export TZ='Europe/Moscow'
export CLICKHOUSE_HOST='127.0.0.1'
export CLICKHOUSE_PORT_TCP=9010
export CLICKHOUSE_PORT_HTTP=8123
export CLICKHOUSE_PORT_HTTP_PROTO="http"
export CLICKHOUSE_CURL="curl"
export PATH=$BYCONITY_BIN:$PATH
export CLICKHOUSE_CONFIG=$BYCONITY_CONF_PATH/byconity-server.xml


$BYCONITY_BIN/../../tests/clickhouse-test --print-time --use-skip-list --order asc --test-runs 1 -q $BYCONITY_BIN/../../tests/queries --run cnch_stateless --b $BYCONITY_BIN/clickhouse

# To run single test case, for example uncomment the below line and comment the above line
#$BYCONITY_BIN/../../tests/clickhouse-test --print-time --use-skip-list --order asc --test-runs 1 -q $BYCONITY_BIN/../../tests/queries --run cnch_stateless --b $BYCONITY_BIN/clickhouse 11003_complex_hash_dictionary*

