#!/usr/bin/env bash



export LOG_DIR=/opt/edp/${service.serviceName}/log
export PID_DIR=/opt/edp/${service.serviceName}/data/nodeexporter

export HOSTNAME=`hostname`

log=$LOG_DIR/nodeexporter-$HOSTNAME.out
pid=$PID_DIR/nodeexporter.pid

echo "========================start nodeexporter========================"

exec_command="node_exporter  --web.listen-address=0.0.0.0:${conf['nodeexporter.http.port']} --path.procfs /host/proc  --path.sysfs /host/sys --path.rootfs /host/root  "
echo "nohup $exec_command > $log 2>&1 &"
nohup $exec_command > $log 2>&1 &
echo $! > $pid

tail -f /dev/null