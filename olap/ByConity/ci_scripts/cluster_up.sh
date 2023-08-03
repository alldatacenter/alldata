set -e -x -a

APP_ROOT=$GITHUB_WORKSPACE
BIN_PATH=${APP_ROOT}/build/programs
ARTIFACT_FOLDER_PATH=/test_output
CONFIG_PATH=${APP_ROOT}/ci_scripts

if [ -n "$ENABLE_IPV6" ]; then
  IP_ADDRESS=BYTED_HOST_IPV6
else
  IP_ADDRESS=$(hostname -I | cut -d " " -f 1) # container's ipv4 address
fi

sed -i "s#ip_address_replace_me#${IP_ADDRESS}#"  ${APP_ROOT}/ci_scripts/config/*.xml
sed -i "s#root_path_replace_me#${CONFIG_PATH}#"  ${APP_ROOT}/ci_scripts/config/*.xml

# create log folder
SERVICES=("tso0" "server" "vw-default" "vw-write" "daemon-manager" "resource-manager0" "resource-manager1" "udf_manager" "udf_script" "clickhouse-test-log")
for service in "${SERVICES[@]}"; do
      mkdir -p "${ARTIFACT_FOLDER_PATH}/${service}"
done

# create folder to store data as local disk
for ((i=0; i<10; i++))
do
  mkdir -p /data/byconity_server/server_local_disk/data/$i
  mkdir -p /data/byconity_worker/data/$i
  mkdir -p /data/byconity_worker-write/data/$i
done

# disable  CLICKHOUSE WATCHDOG
CLICKHOUSE_WATCHDOG_ENABLE=0
$BIN_PATH/clickhouse install
cp $APP_ROOT/tests/clickhouse-test /usr/bin/clickhouse-test

# start services
ASAN_OPTIONS=halt_on_error=false,log_path=/test_output/tso0/asan.tso0.log nohup ${BIN_PATH}/tso-server --config-file ${APP_ROOT}/ci_scripts/config/tso.xml >/dev/null 2>&1 &
#sleep 2
#ASAN_OPTIONS=halt_on_error=false,log_path=/test_output/tso1/asan.tso1.log nohup ${BIN_PATH}/tso-server --config-file  ${APP_ROOT}/ci_scripts/config/tso1.xml >/dev/null 2>&1 &
#sleep 2
#ASAN_OPTIONS=halt_on_error=false,log_path=/test_output/tso2/asan.tso2.log nohup ${BIN_PATH}/tso-server --config-file  ${APP_ROOT}/ci_scripts/config/tso2.xml >/dev/null 2>&1 &
sleep 2
#ASAN_OPTIONS=halt_on_error=false,log_path=/test_output/resource-manager1/asan.rm1.log nohup ${BIN_PATH}/resource_manager --config-file  ${APP_ROOT}/ci_scripts/config/rm1.xml  >/dev/null 2>&1 &
#sleep 2
ASAN_OPTIONS=halt_on_error=false,log_path=/test_output/server/asan.server.log nohup ${BIN_PATH}/clickhouse-server --config-file  ${APP_ROOT}/ci_scripts/config/server.xml >/dev/null 2>&1 &
sleep 2

ASAN_OPTIONS=halt_on_error=false,log_path=/test_output/vw-default/asan.worker-default.log WORKER_ID='default-worker-0' WORKER_GROUP_ID='default' VIRTUAL_WAREHOUSE_ID='vw_default' nohup  ${BIN_PATH}/clickhouse-server --config-file   ${APP_ROOT}/ci_scripts/config/worker.xml >/dev/null 2>&1 &
sleep 2
ASAN_OPTIONS=halt_on_error=false,log_path=/test_output/vw-write/asan.worker-write.log WORKER_ID='write-worker-0' WORKER_GROUP_ID='write' VIRTUAL_WAREHOUSE_ID='vw_write' nohup  ${BIN_PATH}/clickhouse-server --config-file   ${APP_ROOT}/ci_scripts/config/worker-write.xml >/dev/null 2>&1 &
sleep 2

ASAN_OPTIONS=halt_on_error=false,log_path=/test_output/vw-default/asan.worker-default1.log WORKER_ID='default-worker-1' WORKER_GROUP_ID='default' VIRTUAL_WAREHOUSE_ID='vw_default' nohup  ${BIN_PATH}/clickhouse-server --config-file   ${APP_ROOT}/ci_scripts/config/worker1.xml >/dev/null 2>&1 &
sleep 2
ASAN_OPTIONS=halt_on_error=false,log_path=/test_output/vw-write/asan.worker-write1.log WORKER_ID='write-worker-1' WORKER_GROUP_ID='write' VIRTUAL_WAREHOUSE_ID='vw_write' nohup  ${BIN_PATH}/clickhouse-server --config-file   ${APP_ROOT}/ci_scripts/config/worker-write1.xml >/dev/null 2>&1 &
sleep 2

ASAN_OPTIONS=halt_on_error=false,log_path=/test_output/daemon-manager/asan.daemon_manager.log nohup ${BIN_PATH}/daemon-manager  --config-file  ${APP_ROOT}/ci_scripts/config/dm.xml  >/dev/null 2>&1 &
sleep 2
# ASAN_OPTIONS=halt_on_error=false,log_path=/test_output/udf_manager/asan.udf_manager.log nohup ${BIN_PATH}/udf_manager_server --config-file  ${APP_ROOT}/ci_scripts/config/udf-manager.xml >/dev/null 2>&1 &
# udf_script log has been defiend in server.xml     <udf_path>/builds/dp/test_output/udf_script</udf_path>
sleep 5

# show service status
ps -aux


