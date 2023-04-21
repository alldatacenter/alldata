#!/usr/bin/env bash



export LOG_DIR=/opt/edp/${service.serviceName}/log
export PID_DIR=/opt/edp/${service.serviceName}/data/alertmanager

export HOSTNAME=`hostname`

log=$LOG_DIR/alertmanager-$HOSTNAME.out
pid=$PID_DIR/alertmanager.pid

echo "========================start alertmanager========================"

exec_command="alertmanager --config.file=/opt/edp/${service.serviceName}/conf/alertmanager.yml --storage.path="/opt/edp/${service.serviceName}/data/alertmanager" --cluster.advertise-address=0.0.0.0:${conf['alertmanager.http.port']} "
echo "nohup $exec_command > $log 2>&1 &"
nohup $exec_command > $log 2>&1 &
echo $! > $pid

tail -f /dev/null