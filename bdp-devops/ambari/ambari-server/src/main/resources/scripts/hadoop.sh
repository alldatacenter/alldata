#!/bin/bash
#
#   Licensed to the Apache Software Foundation (ASF) under one
#   or more contributor license agreements.  See the NOTICE file
#   distributed with this work for additional information
#   regarding copyright ownership.  The ASF licenses this file
#   to you under the Apache License, Version 2.0 (the
#   "License"); you may not use this file except in compliance
#   with the License.  You may obtain a copy of the License at
#  
#       http://www.apache.org/licenses/LICENSE-2.0
#  
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
# 



# This is a resource agent for controlling hadoop daemons from 
# cluster.

# Source function library
. /etc/init.d/functions 

# OCF_ROOT is ./usr/lib/ocf
: ${OCF_FUNCTIONS_DIR=$(dirname $0)}
. ${OCF_FUNCTIONS_DIR}/ocf-shellfuncs



# Source networking configuration
[ -f /etc/sysconfig/network ] && . /etc/sysconfig/network

# Check that networking is up
[ "${NETWORKING}" = "no" ] && exit ${OCF_ERR_INSTALLED}

# Pull in Hadoop facts

. /etc/default/hadoop
. /etc/hadoop/conf/hadoop-env.sh

if [ "${OCF_RESKEY_daemon}" == "namenode" ]; then
  user="${HADOOP_NAMENODE_USER}"
else
  user="${HADOOP_JOBTRACKER_USER}"
fi

# The program being managed
program=hadoop-daemon.sh
DAEMON=${HADOOP_HOME}/bin/$program
# the HA probe script
HAPROBE=${HADOOP_HOME}/monitor/haprobe.sh 



#This isn't in the 5x so here is a rewrite of the core operations
# Input: a command and arguments
# out: 0 or OCF_ERR_GENERIC.
ocf_run() {

  out=`"$@" 2>&1`
  #`"$@"`
  retval=$?
  if ((${retval} == 0))
  then
    ocf_log info ${out}
  else
    echo $out
    ocf_log err ${out}
    ocf_log err "Command $* failed with return code ${retval}"
    retval=${OCF_ERR_GENERIC} 
  fi
  return ${retval};
}


#this is here as the ocf command is missing
ocf_is_decimal () { 
  let i=10#$1 2>/dev/null; 
}

# Generate the metadata about this cluster entry

# IMPORTANT WARNING FOR PEOPLE MAINTAINING THIS
# NO NOT PUT ANY QUOTES IN THE DESCRIPTION TEXT.
# --THESE ARE CONVERTED INTO ATTRIBUTES FOR SCHEMA VALIDATION; QUOTES BREAKS THIS
#

metadata() {
  cat <<EOT
<?xml version="1.0"?>
<!DOCTYPE resource-agent SYSTEM "ra-api-1-modified.dtd">
<resource-agent version="rgmanager 2.0" name="hadoop">
  <version>1.0</version>

  <longdesc lang="en">
    Apache Hadoop resource agent
  </longdesc>
  <shortdesc lang="en">
    hadoop resource agent
  </shortdesc>

  <parameters>
    <parameter name="name" unique="1" primary="1">
      <shortdesc lang="en">
        Symbolic name for this hadoop service
      </shortdesc>
      <longdesc lang="en">
        Symbolic name for this hadoop service, e.g. NameNode Process 
      </longdesc>
      <content type="string"/>
    </parameter>

    <parameter name="daemon" unique="0" required="1">
      <shortdesc lang="en">
        The hadoop daemon name to run
      </shortdesc>
      <longdesc lang="en">
        The hadoop daemon name to run, e.g. namenode
      </longdesc>
      <content type="string"/>
    </parameter>

    <parameter name="ambariproperties" unique="0" required="0">
      <shortdesc lang="en">
        Ambari properties as comma separated key value pairs
      </shortdesc>
      <longdesc lang="en">
        Example property value:
        ambariproperties="server=localhost,port=8080,protocol=http,user=admin,password=admin,cluster=c1,output=/var/log/ambari_relocate.log"
      </longdesc>
      <content type="string"/>
    </parameter>

    <parameter name="url" unique="0" required="0">
      <shortdesc lang="en">
        URL to probe, use empty string or null to indicate undefined
      </shortdesc>
      <longdesc lang="en">
        URL to probe, use empty string or null to indicate undefined
      </longdesc>
      <content type="string"/>
    </parameter>
  
    <parameter name="pid" unique="0" required="0">
      <shortdesc lang="en">
        The filename of any .pid file to monitor.
      </shortdesc>
      <longdesc lang="en">
        The filename of any .pid file identifying a process to monitor.
        This is of little benefit when monitoring a live cluster, as the HTTP and IPC
        probes are more rigorous. Probing the process by pay of the pid file
        is most useful during startup, as it can detect the failure of a process
        early.
      </longdesc>
      <content type="string"/>
    </parameter>
       
    <parameter name="path" unique="0" required="0">
      <shortdesc lang="en">
        The directory path in HDFS to probe
      </shortdesc>
      <longdesc lang="en">
        The path in the HDFS filesystem to probe; default is "/"
      </longdesc>
      <content type="string"/>
    </parameter>
    
    <parameter name="boottime" unique="0" required="0">
      <shortdesc lang="en">
        The time in milliseconds that the service is required to be live by.
      </shortdesc>
      <longdesc lang="en">
        The time in milliseconds that the service is required to be live by.
        For the Namenode, this includes the time to replay the edit log.
      </longdesc>
      <content type="integer" default="180000"/>
    </parameter>    
    
    <parameter name="probetime" unique="0" required="0">
      <shortdesc lang="en">
        The time in milliseconds that a probe should take.
      </shortdesc>
      <longdesc lang="en">
        The maximum time in milliseconds that a probe should take. This must be
        long enough to cover GC pauses, so that a long GC does not get mistaken
        for a hung process.
      </longdesc>
      <content type="integer" default="120000"/>
    </parameter>
 
     <parameter name="stoptime" unique="0" required="0">
      <shortdesc lang="en">
        The time in milliseconds that the service is required to be stop gracefully by.
      </shortdesc>
      <longdesc lang="en">
        The time in milliseconds that the service is required to to come to 
        a clean halt.
        If the process has not finished by the end of this time period, it
        is forcefully killed via a kill-9 command.
      </longdesc>
      <content type="integer" default="60000"/>
    </parameter>    
    
    <parameter name="waitfs" unique="0" required="0">
      <shortdesc lang="en">
        flag to indicate whether or not the filesystem needs to come up first
      </shortdesc>
      <longdesc lang="en">
      Indicate that the HA monitor should wait until the fs is live before
      declaring that the service is live
      </longdesc>
      <content type="boolean" default="false"/>
    </parameter>    
    
  </parameters>

  <actions>
  
    <!-- start time doesnt provide a timeout hint as waitfs actions
     may need to block startup for an extended period of time. -->
    <action name="start" />
    <action name="stop" timeout="100s"/>
    <!-- includes shutdown time and edit log time -->
    <action name="recover" timeout="4m"/>

    <!-- Regular status check -->
    <action name="monitor" interval="20s" timeout="120s"/>
    <action name="status" interval="20s" timeout="120s"/>

    <!-- Depth checks -->
    <!-- This depth checks hdfs is accessible --> 
    <!-- <action name="monitor" depth="10" interval="30s" timeout="120s"/> --> 
    <!-- <action name="status" depth="10" interval="30s" timeout="120s"/> --> 

    <action name="meta-data" timeout="5s"/>
    <action name="validate-all" timeout="5s"/>
  </actions>
</resource-agent>
EOT
}

#If you want to test the scripts, set some properties
# export OCF_RESKEY_httpport="50070"
# export OCF_RESKEY_daemon="namenode"
# export OCF_RESKEY_ip="localhost"
# export OCF_CHECK_LEVEL="100"


# Start the operation
start() {
  assert_binary

  ocf_log info "Starting hadoop-${OCF_RESKEY_daemon}"
  daemon --user ${user} --check ${DAEMON} ${DAEMON} --config /etc/hadoop/conf start ${OCF_RESKEY_daemon}
  RETVAL=$?
  if [ ${RETVAL} -ne 0 ]; then
    ocf_log err "Failed to start ${DAEMON}: ${RETVAL}"
    return ${RETVAL}
  fi
  sleep 15

  dfs_bootstrap_check
  RETVAL=$?
  echo
  if [ ${RETVAL} -ne 0 ]; then
    ocf_log err "Failed to start dfs_bootstrap_check}: ${RETVAL}"
    return ${OCF_ERR_GENERIC}
  fi
  return 0
}

stop() {
  HADOOP_STOP_TIMEOUT=${OCF_RESKEY_stoptime}
  ocf_log info "Stopping hadoop-${OCF_RESKEY_daemon} with timeout ${HADOOP_STOP_TIMEOUT}"
  daemon --user ${user} --check ${DAEMON} ${DAEMON} --config /etc/hadoop/conf stop ${OCF_RESKEY_daemon}
  RETVAL=$?
  ocf_log info "stop command issued, retval is ${RETVAL}"
  if [ ${RETVAL} -ne 0 ]; then
    ocf_log err "Failed to stop ${program} ${OCF_RESKEY_daemon}"
    return ${OCF_ERR_GENERIC}
  fi
    
  echo
  # Spin waiting for shutdown
    
#  while url_check
#  do
#    ocf_log debug "Resource has not stopped yet, waiting"
#    sleep 15
#  done
#  
  return ${OCF_SUCCESS}
}

#
# Verify the binary is installed
#
# Usage: verify_binary
# Result: $OCF_ERR_INSTALLED = binary not installed
#         0                  = binary installed
#
verify_binary() {
    # Report that $prog does not exist, or is not executable
  if [ ! -x "${DAEMON}" ];  then
    ocf_log err "Binary ${DAEMON} doesn't exist"
    return ${OCF_ERR_INSTALLED}
  fi
  return ${OCF_SUCCESS}
}

assert_binary() {
  verify_binary || exit $?
}




# status checking. 
# This exits during its execution, as this simplifies
# the logic for different layers of check
status_check() {

#    assert_arguments_are_valid
    ocf_log info "Checking ${OCF_RESKEY_daemon}, Level ${OCF_CHECK_LEVEL}"

    #look for the check level as in some tests it isn't set 
    if [ "x" == "x${OCF_CHECK_LEVEL}" ]
    then 
      ocf_log err "Environment variable OCF_CHECK_LEVEL not set"
      exit ${OCF_ERR_ARGS}
    fi
    retval=0
    # website check
#    url_check
#    retval=$?

#    retval=pid_check
#    if [ $retval -ne 0 ]
#    then
#      exit ${retval}
#    fi
#  
#    [ "${OCF_CHECK_LEVEL}" -lt 10 ] && exit ${retval}

    # Depth level 10 check


    dfs_check
    retval=$?
#
#    if [ $? -ne 0 ]; then
#      retval=${OCF_NOT_RUNNING}
#    fi
    exit ${retval}
}


# HA probe
dfs_check() {

  ocf_run "${HAPROBE}" --file ${OCF_RESKEY_path}  --pid ${OCF_RESKEY_pid} --url ${OCF_RESKEY_url} --timeout ${OCF_RESKEY_probetime}
  if [ $? -ne 0 ]
  then
    ocf_log warn "Service ${OCF_RESKEY_daemon} is not running according to checks: -file ${OCF_RESKEY_path}  --pid ${OCF_RESKEY_pid} --url ${OCF_RESKEY_url} "
    return ${OCF_NOT_RUNNING}
  fi
  return ${OCF_SUCCESS}
}

# Run a bootstrap check
# this can include different probes and timeouts
dfs_bootstrap_check() {

  ocf_run "${HAPROBE}" --file ${OCF_RESKEY_path}  --pid ${OCF_RESKEY_pid} --url ${OCF_RESKEY_url} --timeout ${OCF_RESKEY_probetime} --boottimeout ${OCF_RESKEY_boottime} --waitfs ${OCF_RESKEY_waitfs}
  if [ $? -ne 0 ]
  then
    ocf_log warn "Service ${OCF_RESKEY_daemon} is not booting according to checks: -file ${OCF_RESKEY_path}  --pid ${OCF_RESKEY_pid} --url ${OCF_RESKEY_url} "
    return ${OCF_NOT_RUNNING}
  fi
  return ${OCF_SUCCESS}
}


# this is a PID check 
pid_check() {
  ocf_run "${HAPROBE} --pid ${OCF_RESKEY_pid}"
  if [ $? -ne 0 ]; then
    return ${OCF_NOT_RUNNING}
  fi
  return ${OCF_SUCCESS}
}

# fill in the default values of a service
fill_in_defaults() {
  : ${OCF_RESKEY_boottime="180000"}
  : ${OCF_RESKEY_daemon="namenode"}
  : ${OCF_RESKEY_httpport="50070"}
  : ${OCF_RESKEY_ip="localhost"}
  : ${OCF_RESKEY_path="/"}
  : ${OCF_RESKEY_pid="null"}
  : ${OCF_RESKEY_probetime="120000"}
  : ${OCF_RESKEY_stoptime="60000"}
  : ${OCF_RESKEY_url="http://localhost:50070/"}
  : ${OCF_RESKEY_waitfs="false"}
}

dump_environment() {
  ocf_log info `env`
}

# Relocate Ambari managed master to current host on failover
execute_ambari_relocate_probe() {
  retval = parse_and_validate_ambari_properties
  if [ $retval -eq 2 ] ; then
    return 0
  elif [ $retval -eq 1 ] ; then
    exit ${retval}
  fi

  if [ -z "$AMBARI_RELOCATE_PROBE" ] ; then
    AMBARI_RELOCATE_PROBE="relocate_resources.py"
  fi

  NEW_HOSTNAME=$(hostname -f)

  if [ "${OCF_RESKEY_daemon}" == "namenode" ] ; then
    SERVICE_NAME="HDFS"
    COMP_NAME="NAMENODE"
  elif [ "${OCF_RESKEY_daemon}" == "jobtracker" ] ; then
    SERVICE_NAME="MAPREDUCE"
    COMP_NAME="JOBTRACKER"
  elif [ "${OCF_RESKEY_daemon}" == "historyserver" ] ; then
    SERVICE_NAME="MAPREDUCE"
    COMP_NAME="JOBTRACKER"
  else
    ocf_log err "Unknown daemon ${OCF_RESKEY_daemon}"
    return ${OCF_ERR_ARGS};
  fi

  if [ -n "${AMBARI_OUTPUT}" ]; then
    OUPUT_FILE_CMD="-o ${AMBARI_OUTPUT}"
  fi

  "${AMBARI_RELOCATE_PROBE}" -s ${AMBARI_SERVER} -p ${AMBARI_PORT} -r ${AMBARI_PROTOCOL} -c ${AMBARI_CLUSTER} -e "${SERVICE_NAME}" -m "${COMP_NAME}" -n "${NEW_HOSTNAME}" -u "${AMBARI_USER}" -w "${AMBARI_PASSWD} ${OUPUT_FILE_CMD}"

  retval=$?
  if [ $retval -eq 0 ] ; then
    ocf_log info "Ambari master successfully relocated."
  elif [ $retval -eq 1 ] ; then
    ocf_log error "Ambari relocate master failed. Continuing with failover..."
  elif [ $retval -eq 2 ] ; then
    ocf_log info "No action required from ambari probe."
  elif [ $retval -eq 3 ] ; then
    ocf_log err "Ambari relocate request verification failed. Exiting..."
    exit ${retval}
  else
    ocf_log error "Unknown return code from ambari probe ${retval}."
  fi

  return $retval
}

# Read Ambari properties as comma separated key value pairs from cluster.conf
# Property name: 'ambariproperties'.
# Example property value:
# ambariproperties="server=localhost,port=8080,protocol=http,user=admin,password=admin,cluster=c1,output=/var/log/ambari_relocate.log"
parse_and_validate_ambari_properties() {
  if [ -n "${OCF_RESKEY_ambariproperties}" ] ; then
    ocf_log info "Ambari properties found: ${OCF_RESKEY_ambariproperties}"

    IFS=',' read -ra properties <<< $OCF_RESKEY_ambariproperties

    for i in $properties; do
      if [[ "$i" == "server"* ]] ; then AMBARI_SERVER=$(echo $i | cut -d"=" -f2); fi
      if [[ "$i" == "port"* ]] ; then AMBARI_PORT=$(echo $i | cut -d"=" -f2); fi
      if [[ "$i" == "protocol"* ]] ; then AMBARI_PROTOCOL=$(echo $i | cut -d"=" -f2); fi
      if [[ "$i" == "user"* ]] ; then AMBARI_USER=$(echo $i | cut -d"=" -f2); fi
      if [[ "$i" == "password"* ]] ; then AMBARI_PASSWD=$(echo $i | cut -d"=" -f2); fi
      if [[ "$i" == "cluster"* ]] ; then AMBARI_CLUSTER=$(echo $i | cut -d"=" -f2); fi
      if [[ "$i" == "output"* ]] ; then AMBARI_OUTPUT=$(echo $i | cut -d"=" -f2); fi
    done

    if [ -z "${AMBARI_SERVER}" ] ; then
      ocf_log err "required ambari property 'server' is unset"
      return 1
    fi

    if [ -z "${AMBARI_PORT}" ] ; then
      ocf_log err "required ambari property 'port' is unset"
      return 1
    fi

    if [ -z "${AMBARI_PROTOCOL}" ] ; then
      ocf_log err "required ambari property 'protocol' is unset"
      return 1
    fi

    if [ -z "${AMBARI_USER}" ] ; then
      ocf_log err "required ambari property 'user' is unset"
      return 1
    fi

    if [ -z "${AMBARI_PASSWD}" ] ; then
      ocf_log err "required ambari property 'password' is unset"
      return 1
    fi

    if [ -z "${AMBARI_CLUSTER}" ] ; then
      ocf_log err "required ambari property 'cluster' is unset"
      return 1
    fi

  else
    ocf_log info "No Ambari properties found."
    return 2
  fi
}

# validate the arguments to the service.
# this assumes that the defaults have been pushed in so only check for existence of the mandatory properties
# and that the numeric properties are valid
validate_arguments_and_state() {
  if [ "x" == "x${OCF_RESKEY_daemon}" ] ; then
    dump_environment
    ocf_log err "required property 'daemon' is unset"
    return ${OCF_ERR_ARGS};
  fi

  if ! ocf_is_decimal "${OCF_RESKEY_boottime}"; then
    ocf_log err "Option 'boottime' is not numeric!"
    return ${OCF_ERR_CONFIGURED}
  fi

  if ! ocf_is_decimal "${OCF_RESKEY_probetime}"; then
    ocf_log err "Option 'probetime' is not numeric!"
    return ${OCF_ERR_CONFIGURED}
  fi

  verify_binary
  return $?
}

# validate the arguments; exit with an error code
# if they are not
assert_arguments_are_valid() {
 validate_arguments_and_state
 retval=$?
 [ ${retval} -ne 0 ] && exit ${retval}
}


# ================================================================================
# This is the live code
# ================================================================================
# Entry point checks parameters
fill_in_defaults

# then switch on the argument
case "$1" in
  start)
    assert_arguments_are_valid
    [ $? -eq 0 ] && exit 0
    execute_ambari_relocate_probe
    start
    exit $?
    ;;

  stop)
#    assert_arguments_are_valid
    if ! stop; then
      exit ${OCF_ERR_GENERIC}
    fi
    exit 0
    ;;

  status|monitor)
    # check the status of the live system
    status_check 
    ;;

  meta-data)
    # generate the metadata
    metadata
    exit 0
    ;;

  recover|restart)
 #   validate_arguments_and_state
    execute_ambari_relocate_probe
    ocf_log info "Service restart"
    $0 stop  || exit ${OCF_ERR_GENERIC}
    $0 start || exit ${OCF_ERR_GENERIC}
    exit 0
    ;;

  validate-all)
    validate_arguments_and_state
    exit $?
    ;;

  # this is a non-standard operation to work out what is going on  
  diagnostics)
    echo PATH=${PATH}
    echo java is at `which java`
    echo JAVA_HOME is ${JAVA_HOME}
    dump_environment
    exit 0
    ;;

  *)
    echo $"Usage: $0 {start|stop|status|monitor|restart|recover|validate-all|meta-data|diagnostics}"
    exit ${OCF_ERR_UNIMPLEMENTED}
    ;;

esac

