#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Run the Drill-on-YARN client to launch Drill on a YARN cluster.
# Uses the $DRILL_HOME/conf/drill-on-yarn.conf file for client options.
# Uses the $DRILL_HOME/conf/drill-cluster.conf file for cluster options.
#
# The config files (along with the Drill config files) MUST be in the
# $DRILL_HOME directory so that they are properly localized. Drill-on-YARN does not permit
# placing configuration files outside the $DRILL_HOME directory.
#
# Requires the location of Hadoop home. Maybe passed using the --hadoop option,
# set in the environment, or set in $DRILL_HOME/conf/yarn-env.sh.

usage="Usage: drill-on-yarn.sh start|stop|status"

bin=`dirname "${BASH_SOURCE-$0}"`
bin=`cd "$bin">/dev/null; pwd`
export DRILL_HOME=`cd "$bin/..">/dev/null; pwd`

# Use Drillbit's config script. We throw away most of the information, we really just
# need JAVA_HOME and HADOOP_CONF_DIR or HADOOP_HOME.

DRILL_TOOL_CP="$DRILL_HOME/jars/tools/*"
. "$DRILL_HOME/bin/drill-config.sh"

# Hadoop config or home is required
if [ -z "$HADOOP_CONF_DIR" ]; then
  if [ -z "$HADOOP_HOME" ]; then
    echo "Hadoop home undefined: set HADOOP_CONF_DIR, HADOOP_HOME" >&2
    exit 1
  fi
  HADOOP_CONF_DIR="$HADOOP_HOME/etc/hadoop"
fi

DRILL_CLIENT_HEAP=${DRILL_CLIENT_HEAP:-512M}
VM_OPTS="-Xms$DRILL_CLIENT_HEAP -Xmx$DRILL_CLIENT_HEAP $DRILL_CLIENT_VM_OPTS"
VM_OPTS="$VM_OPTS -Dlogback.configurationFile=yarn-client-log.xml"
#VM_OPTS="$VM_OPTS -Ddrill.yarn.siteDir=$DRILL_CONF_DIR"

# Add Hadoop configuration at the end of the class path. This will
# fail if the 1.6-and earlier core-site.xml file resides in the conf
# directory.

CP="$CP:$HADOOP_CONF_DIR"

if [ ${#args[@]} = 0 ]; then
  echo $usage
  exit 1
fi

CLIENT_CMD="$JAVA $VM_OPTS -cp $CP org.apache.drill.yarn.client.DrillOnYarn ${args[@]}"

case ${args[0]} in
debug)
  env
  echo "Command: $CLIENT_CMD"
  ;;
*)
  exec $CLIENT_CMD
  ;;
esac
