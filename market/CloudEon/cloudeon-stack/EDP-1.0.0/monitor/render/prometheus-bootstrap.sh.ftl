#!/usr/bin/env bash



export LOG_DIR=/opt/edp/${service.serviceName}/log
export PID_DIR=/opt/edp/${service.serviceName}/data/prometheus

export HOSTNAME=`hostname`

log=$LOG_DIR/prometheus-$HOSTNAME.out
pid=$PID_DIR/prometheus.pid

echo "========================start prometheus========================"

exec_command="prometheus --config.file=/opt/edp/${service.serviceName}/conf/prometheus.yml --storage.tsdb.path="/opt/edp/${service.serviceName}/data/prometheus"  --web.listen-address=0.0.0.0:${conf['prometheus.http.port']} --web.enable-lifecycle"
echo "nohup $exec_command > $log 2>&1 &"
nohup $exec_command > $log 2>&1 &
echo $! > $pid

tail -f /dev/null