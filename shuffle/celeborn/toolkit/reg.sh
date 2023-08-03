#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#set -x
REG_HOME="$(
  cd "$(dirname "$0")"
  pwd
)"
REG_HOSTS=("core-1-1" "core-1-2" "core-1-3" "core-1-4" "core-1-5" "core-1-6" "core-1-7" "core-1-8")
if [[ -n ${REG_CUSTOM_REG_HOSTS} ]];then
  echo "using custom hosts"
  REG_HOSTS=${REG_CUSTOM_REG_HOSTS}
fi
REG_CONF_DIR=${REG_HOME}/conf
REG_TOOLS=${REG_HOME}/tools
REG_SCRIPTS=${REG_HOME}/scripts
REG_CELEBORN_DIST=${REG_HOME}/dist

HIVEBENCH_DIR=${REG_HOME}/hive-testbench
HIVEBENCH_QUERY_DIR=${HIVEBENCH_DIR}/spark-queries-tpcds
HIVEBENCH_SCHEMA=tpcds_bin_partitioned_orc_1000

HIBEN_DIR=${REG_HOME}/hibench3
HIBEN_CONF_DIR=${HIBEN_DIR}/conf

CHECK_TPCDS_PYTHON=${REG_SCRIPTS}/check.py

CELEBORN_CONF_DIR=${REG_CONF_DIR}/celeborn
CELEBORN_CLIENT_INSTALL_DIR=/opt/apps/CELEBORN/celeborn-current/spark3

REG_RESULT=${REG_HOME}/result

SPARK3_HOME=/opt/apps/SPARK3/spark3-current

loglevel=1

CELEBORN_DIST=""
REG_CURRENT_RUNNING_DATE=$(date +%Y%m%d%H%M%S)
ESS_RESULT=/home/hadoop/ess

function log() {
  local msg
  logtype=$1
  shift
  msg="$@"
  if [[ -z ${msg} ]]; then
    while read msg; do
      output_msg ${logtype} ${msg}
    done
  else
    output_msg ${logtype} ${msg}
  fi
}

function getCelebornDist() {
  cd ${REG_CELEBORN_DIST}
  tar xf apache-celeborn*.tgz
  rm -rf ./apache-celeborn*.tgz
  CELEBORN_DIST="$(basename `cd apache-celeborn* && pwd`)"
  cd -
}

function output_msg() {
  logtype=$1
  shift
  msg="$@"
  datetime=$(date +'%F %H:%M:%S.%3N')
  logformat="[${logtype}] ${datetime} | ${msg}"
  {
    case $logtype in
    debug)
      [[ $loglevel -le 0 ]] && echo -e "${logformat}"
      ;;
    info)
      [[ $loglevel -le 1 ]] && echo -e "${logformat}"
      ;;
    warn)
      [[ $loglevel -le 2 ]] && echo -e "${logformat}"
      ;;
    error)
      [[ $loglevel -le 3 ]] && echo -e "${logformat}"
      ;;
    esac
  }
}

function runTerasort() {
  updateCeleborn
  start=$(($(date +%s%N) / 1000000))

  if [ "$1" == "regression" ]; then
    killOneWorkerRandomly
  fi

  cp ${HIBEN_CONF_DIR}/spark.conf.celeborn ${HIBEN_CONF_DIR}/spark.conf
  ${HIBEN_DIR}/bin/workloads/micro/terasort/spark/run.sh

  end=$(($(date +%s%N) / 1000000))
  duration=$(((end - start) / 1000))
  sec=$(bc <<<"scale=3; ($end - $start)/1000")
  elapse_time=$(printf "%d:%02d:%02d, %s seconds" $(($duration / 3600)) $((($duration / 60) % 60)) $(($duration % 60)) $sec)
  log info "Run terasort finished. Time token: $elapse_time"
}

function runManySplits() {
  updateCeleborn
  start=$(($(date +%s%N) / 1000000))

  cp ${HIBEN_CONF_DIR}/spark.conf.split ${HIBEN_CONF_DIR}/spark.conf
  ${HIBEN_DIR}/bin/workloads/micro/terasort/spark/run.sh

  end=$(($(date +%s%N) / 1000000))
  duration=$(((end - start) / 1000))
  sec=$(bc <<<"scale=3; ($end - $start)/1000")
  elapse_time=$(printf "%d:%02d:%02d, %s seconds" $(($duration / 3600)) $((($duration / 60) % 60)) $(($duration % 60)) $sec)
  log info "Run terasort with many splits finished. Time token: $elapse_time"
}

function runSkewJoin() {
  updateCeleborn
  start=$(($(date +%s%N) / 1000000))

  if [ "$1" == "regression" ]; then
    killOneWorkerRandomly
  fi

  spark-sql --properties-file ${REG_CONF_DIR}/spark-skewjoin.conf -e "select max(fa),max(length(f1)),max(length(f2)),max(length(f3)),max(length(f4)),max(fb),max(length(f6)),max(length(f7)),max(length(f8)),max(length(f9)) from table1 a inner join table2 b on a.fa=b.fb;"

  end=$(($(date +%s%N) / 1000000))
  duration=$(((end - start) / 1000))
  sec=$(bc <<<"scale=3; ($end - $start)/1000")
  elapse_time=$(printf "%d:%02d:%02d, %s seconds" $(($duration / 3600)) $((($duration / 60) % 60)) $(($duration % 60)) $sec)
  log info "Run skew join finished. Time token: $elapse_time"
}

function switchToESS() {
  cp ${REG_CONF_DIR}/spark-ess.conf ${REG_CONF_DIR}/spark.conf
}

function switchToCeleborn() {
  cp ${REG_CONF_DIR}/spark-celeborn.conf ${REG_CONF_DIR}/spark.conf
}

function switchToCelebornAndDuplicate() {
  cp ${REG_CONF_DIR}/spark-celeborn-dup.conf ${REG_CONF_DIR}/spark.conf
}

function runTPCDSOnESS() {
  echo -e "Run TPC-DS Suite at ${REG_CURRENT_RUNNING_DATE}"
  echo -e "Run TPC-DS suite on ESS"
  switchToESS
  if [ -z ${CELEBORN_SKIP_TPCDS} ]; then
  singleTPCDS
    if [[ $? -ne 0 ]]; then
      echo -e "Run TPC-DS suite on ESS failed"
      exit -1
    fi
  fi
  cp -r ${HIVEBENCH_QUERY_DIR} ${REG_RESULT}/${REG_CURRENT_RUNNING_DATE}/ess
  echo -e "finish TPC-DS on ESS \n"
}

function runTPCDSOnCelebornWithoutReplication() {
  echo -e "Run TPC-DS suite on celeborn"
  switchToCeleborn
  updateCeleborn
  if [[ -z ${CELEBORN_SKIP_TPCDS} ]]; then
    singleTPCDS
    if [[ $? -ne 0 ]]; then
      echo -e "Run TPC-DS suite on ESS failed"
      exit -1
    fi
  fi

  cp -r ${HIVEBENCH_QUERY_DIR} ${REG_RESULT}/${REG_CURRENT_RUNNING_DATE}/celeborn
  checkTPCDSResult ${HIVEBENCH_QUERY_DIR} ${ESS_RESULT}
  echo -e "finish TPC-DS on celeborn \n"
}

function runTPCDSOnCelebornWithReplication() {
  echo -e "Run TPC-DS suite on celeborn Duplicate"
  switchToCelebornAndDuplicate
  updateCeleborn

  if [[ -z ${CELEBORN_SKIP_TPCDS} ]]; then
    if [[ $1 == "regression" ]]; then
      singleTPCDS $1
    else
      singleTPCDS
    fi
    if [[ $? -ne 0 ]]; then
      echo -e "Run TPC-DS suite on ESS failed"
      exit -1
    fi
  fi

  cp -r ${HIVEBENCH_QUERY_DIR} ${REG_RESULT}/${REG_CURRENT_RUNNING_DATE}/celeborn-dup
  checkTPCDSResult ${HIVEBENCH_QUERY_DIR} ${ESS_RESULT}
  echo -e "finish TPC-DS on celeborn \n"
}

function runTPCDSSuite() {
  mkdir -p ${REG_RESULT}/${REG_CURRENT_RUNNING_DATE}/
  if [[ -n ${CELEBORN_RUN_ESS} ]]; then
    runTPCDSOnESS
  fi

  getCelebornDist
  # only run TPC-DS on Celeborn with one replication in benchmark scenario
  if [[ $1 != "regression" ]]; then
    runTPCDSOnCelebornWithoutReplication
  fi

  runTPCDSOnCelebornWithReplication $1

  ${SPARK3_HOME}/sbin/stop-thriftserver.sh
}

function killOneWorkerRandomly() {
  WORKER_INDEX=$(shuf -i1-8 -n1)
  SLEEP_TIME=$(shuf -i500-800 -n1)
  # if regression ,kill a worker in a random time between 500s and 800s
  echo -e "Will kill worker core-1-${WORKER_INDEX} in ${SLEEP_TIME} seconds"
  ssh core-1-${WORKER_INDEX} "sleep ${SLEEP_TIME}s && jps | grep Worker | awk '{print \$1}' | xargs kill -9 " &
}

function singleTPCDS() {
  set -e
  set -o pipefail

  start=$(($(date +%s%N) / 1000000))
  log info "Run TPC-DS regression started."

  ${SPARK3_HOME}/sbin/stop-thriftserver.sh

  ${SPARK3_HOME}/sbin/start-thriftserver.sh --properties-file=${REG_CONF_DIR}/spark.conf --hiveconf hive.server2.thrift.port=10001

  # waiting for the spark thrift server get ready
  sleep 50

  if [ "$1" == "regression" ]; then
    killOneWorkerRandomly
  fi

  for i in $(ls ${HIVEBENCH_QUERY_DIR}/q*.sql); do

    echo $i

    beeline --showStartEndTime=true -u jdbc:hive2://master-1-1:10001/${HIVEBENCH_SCHEMA} -f $i >${HIVEBENCH_QUERY_DIR}/$(basename $i | cut -d . -f1).out 2>/home/hadoop/hive-testbench/spark-queries-tpcds/$(basename $i | cut -d . -f1).err

    if [[ $? -ne 0 ]]; then
      echo Query ${BASE_NAME} Failed.
      return -1
    else
      echo Query ${BASE_NAME} Finished.
    fi

  done

  end=$(($(date +%s%N) / 1000000))
  duration=$(((end - start) / 1000))
  sec=$(bc <<<"scale=3; ($end - $start)/1000")
  elapse_time=$(printf "%d:%02d:%02d, %s seconds" $(($duration / 3600)) $((($duration / 60) % 60)) $(($duration % 60)) $sec)
  log info "Run TPC-DS regression finished. Time token: $elapse_time"
  set +e
  set +o pipefail
}

function checkTPCDSResult() {
  SOURCE_DIR=$1
  DEST_DIR=$2
  for query in $(ls ${SOURCE_DIR}/q*.out); do

    QUERY_NAME=$(basename $query | cut -d . -f1)
    echo -n "========= checking ${QUERY_NAME} ..."
    ${CHECK_TPCDS_PYTHON} ${SOURCE_DIR}/${QUERY_NAME}.out ${DEST_DIR}/${QUERY_NAME}.out
    if [[ $? -ne 0 ]]; then
      echo " Failed ========="
      exit -1
    fi
    echo " success "

  done
}

function updateCeleborn() {
  if [[ "CELEBORN_DIST" == "" ]]; then
    echo -e "CELEBORN_DIST must not be null, abort \n"
    exit -1
  fi
  # stop master
  echo -e "restart master node \n"
  export CELEBORN_CONF_DIR=${REG_CONF_DIR}/celeborn
  ${REG_CELEBORN_DIST}/${CELEBORN_DIST}/sbin/stop-master.sh
  jps | grep Master | awk '{print $1}' | xargs kill -9 > /dev/null 2>&1

  rm -rf ${CELEBORN_CLIENT_INSTALL_DIR}/*
  cp ${REG_CELEBORN_DIST}/${CELEBORN_DIST}/spark/* ${CELEBORN_CLIENT_INSTALL_DIR}/

  for host in "${REG_HOSTS[@]}"; do
    echo -e "update ${host} \n"
    ssh ${host} "export CELEBORN_CONF_DIR=/home/hadoop/conf ; /home/hadoop/${CELEBORN_DIST}/sbin/stop-worker.sh"
    ssh ${host} "rm -rf /home/hadoop/${CELEBORN_DIST}"
    ssh ${host} "rm -rf /mnt/disk1/celeborn-worker/shuffle_data/*"
    ssh ${host} "rm -rf /mnt/disk2/celeborn-worker/shuffle_data/*"
    ssh ${host} "rm -rf /mnt/disk3/celeborn-worker/shuffle_data/*"
    ssh ${host} "rm -rf /mnt/disk4/celeborn-worker/shuffle_data/*"
    scp -r ${REG_CELEBORN_DIST}/${CELEBORN_DIST}/ ${host}:~/ > /dev/null 2>&1
    scp -r ${REG_CELEBORN_DIST}/${CELEBORN_DIST}/spark/* ${host}:${CELEBORN_CLIENT_INSTALL_DIR}/ > /dev/null 2>&1
  done

  for host in "${REG_HOSTS[@]}"; do
    echo -e "kill worker on ${host}"
    ssh ${host} "jps | grep Worker | awk '{print \$1}' | xargs kill -9" > /dev/null 2>&1
  done

  ${REG_CELEBORN_DIST}/${CELEBORN_DIST}/sbin/start-master.sh

  sleep 10

  for host in "${REG_HOSTS[@]}"; do
    echo -e "start worker on ${host} \n"
    ssh ${host} "export CELEBORN_CONF_DIR=/home/hadoop/conf ; /home/hadoop/${CELEBORN_DIST}/sbin/start-worker.sh celeborn://master-1-1:9097"
  done
}

function cleanHdfs() {
  hdfs dfs -expunge -immediate
}

function setupEnv() {
  # install tmux and grafana
  sudo yum install -y tmux
  sudo yum install -y ${REG_TOOLS}/grafana-enterprise-8.5.0-1.x86_64.rpm
  sudo systemctl start grafana-server
  ssh-key-gen -t RSA -p ''
  sudo cp /home/hadoop/.ssh/id_rsa.pub /home/emr-user/id_rsa.pub
  sudo chown emr-user:emr-user /home/emr-user/id_rsa.pub

  #set ssh login
  for host in "${HOSTS[@]}"; do
    ssh-keyscan ${host} >>~/.ssh/known_hosts
    su - emr-user "ssh-keyscan ${host} >> /home/emr-user/.ssh/known_hosts"
    #    change to emr-user user
    sudo su emr-user
    cd ~/
    scp id_rsa.pub ${host}:~/
    ssh ${host} "sudo mkdir /home/hadoop/.ssh"
    ssh ${host} "sudo cat /home/emr-user/id_rsa.pub >> /home/hadoop/.ssh/authorized_keys"
    ssh ${host} "sudo chmown -R hadoop:hadoop /home/hadoop/.ssh"
    ssh ${host} "sudo chmod -R 700 /home/hadoop/.ssh"
    exit
    #    change to hadoop user
    ssh-keyscan ${host} >>/home/hadoop/.ssh/known_hosts
  done

  #set node exporter
  echo -e
  nohup ${REG_TOOLS}/node_exporter-1.3.1.linux-amd64/node_exporter &
  for host in "${HOSTS[@]}"; do
    scp -r node_exporter-1.3.1.linux-amd64 ${host}:~/
    ssh ${host} "nohup /home/hadoop/node_exporter-1.3.1.linux-amd64/node_exporter &"
  done

  ${REG_SCRIPTS}/genConfs.py ${REG_HOME}

  #TPC-DS 1TB
  ${HIVEBENCH_DIR}/tpcds-build.sh
  ${HIVEBENCH_DIR}/tpcds-setup.sh 1000

  #Terasort 1TB
  ${HIBEN_DIR}/bin/workloads/micro/terasort/prepare/prepare.sh

  #set directory permission
  for host in "${HOSTS[@]}"; do
    scp -r ${CELEBORN_CONF_DIR} $host:~/
    ssh ${host} "sudo chown hadoop:hadoop -R ${CELEBORN_CLIENT_INSTALL_DIR}"
  done
  #generate skew join data

  sudo mkdir -p /tmp/spark-events
  sudo chown hadoop:hadoop -R /tmp/spark-events
  spark-shell --conf spark.executor.instances=200 -i ${REG_SCRIPTS}/genSkewData.scala
}

function cleanSparkEventDir() {
  rm -rf /tmp/spark-events/*
}

echo -e "start at ${REG_CURRENT_RUNNING_DATE}"
cleanSparkEventDir
rm -rf ${REG_RESULT}
case $1 in
"setup")
  setupEnv
  ;;

"regression")
  runTPCDSSuite regression
  runTerasort regression
  runSkewJoin regression
  ;;

"benchmark")
  runTPCDSSuite
  runTerasort
  runSkewJoin
  runManySplits
  ;;
esac

zip ${REG_RESULT}/${REG_CURRENT_RUNNING_DATE}.zip -r ${REG_RESULT}/${REG_CURRENT_RUNNING_DATE}
rm -rf ${REG_RESULT}/${REG_CURRENT_RUNNING_DATE}

echo -e "done at $(date)"
