#!/bin/bash
basedir=$(cd `dirname $0`/..; pwd)
sysctl -w vm.max_map_count=2000000
$DORIS_HOME/doris-be/bin/start_be.sh --daemon

tail -f /dev/null

