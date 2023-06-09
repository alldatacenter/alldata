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

# This is a heavily modified version of drillbit.sh, drill-config.sh and
# runbit, modified for use in YARN and performing a single operation:
# launching a Drillbit and waiting for Drillbit exit.
#
# Under YARN, the typical way to launch a Java app is to do all the
# setup in Java code in the launch context. However, Drill depends on
# drill-env.sh to set site-specific options before launch. This script
# performs Drill launch, integrating options from drill-env.sh.
#
# Input environment variables:
#
# DRILL_DEBUG
#     Used to debug this script. Dumps debugging information.
#     Set from the drill.yarn.drillbit.debug-launch config parameter.
# DRILL_HOME
#     Identifies the Drill software to use: either at a fixed
#     location, or localized. Set from the drill.yarn.drill-install
#     localize and drill-home config parameters. Note that this
#     variable tells this script where to find the drill-env.sh
#     file (in $DRILL_HOME/conf), and so DRILL_HOME cannot be
#     overridden in drill-env.sh.
# DRILL_LOG_DIR
#     The location to which to write log files. Often set in drill-env.sh.
#     Non-YARN launch provides a number of default locations. This variable
#     is ignored in a YARN launch if the drill.yarn.drillbit.yarn-logs is
#     true (the default.) Set the config variable to false to use this
#     value for the long directory under YARN.
# DRILL_YARN_LOG_DIR
#     If using the YARN log directory, this variable points to that location.
#     If drill.yarn.drillbit.disable-yarn-logs is true, then this variable is not
#     set and the DRILL_LOG_DIR is used instead.
# DRILL_MAX_DIRECT_MEMORY:
#     The amount of direct memory set in the
#     drill.yarn.drillbit.max-direct-memory config parameter.
#     When Drill is run outside of YARN, this value is set in drill-env.sh.
#     But doing so under YARN decouples the Drill memory settings from the
#     YARN settings. If you do not set the config parameter, Drill will
#     default to the value set in drill-env.sh.
# DRILL_HEAP
#     The amount of Java heap memory set in the
#     drill.yarn.drillbit.heap setting. Same override rules as
#     DRILL_MAX_DIRECT_MEMORY.
# DRILL_JAVA_OPTS
#     The standard JVM options needed to launch Drill. Must be set in
#     drill-env.sh.
# DRILL_JVM_OPTS
#     Additional YARN-specific JVM options set in the
#     drill.yarn.drillbit.vm-args config parameter. Note that the YARN-specific
#     options are in addition to (not an override of) the DRILL_JAVA_OPTS
#     values.
# SERVER_GC_OPTS
#     Garbage collection (GC) related JVM options set in drill-env.sh. Not
#     overridden in YARN.
# HADOOP_HOME
#     Location of the Hadoop software and configuration. Can be
#     set with the drill.yarn.hadoop.home or in drill-env.sh. If both are set, the
#     latter value overrides the former.
# JAVA_HOME
#     Set by YARN, bypassing Drill's usual process for searching for JAVA_HOME.
# DRILL_CONF_DIR
#     Location of Drill's configuration files. Non-YARN launch provides a set of
#     defaults. Under YARN, this value must either be set explicitly using the
#     drill.yarn.drillbit.conf-dir parameter, or will default to
#     the (typically localized) $DRILL_HOME/conf directory.
# DRILL_CLASSPATH_PREFIX
#     Optional extra classpath added before Drill's own jars. Set from the
#     drill.yarn.drillbit.prefix-class-path config parameter, or in
#     drill-env.sh. As with all class path settings, if both values are set,
#     drill-env.sh takes precedence.
# EXTN_CLASSPATH
#     Optional extra classpath added after Drill's own jars but before
#     3rd party jars. Allows overriding Drill's bundled version of Hadoop
#     and so on. Allows adding jars needed by plug-ins. Config parameter
#     is drill.yarn.drillbit.extn-class-path.
# HADOOP_CLASSPATH
#     As above, but for the Hadoop class path. Config parameter is
#     drill.yarn.drillbit.hadoop-class-path. This is a legacy setting. Use
#     drill.yarn.drillbit.extn-class-path for new deployments.
# HBASE_CLASSPATH
#     As above, but for the Hbase class path. Config parameter is
#     drill.yarn.drillbit.hbase-class-path. This is a legacy setting. Use
#     drill.yarn.drillbit.extn-class-path for new deployments.
# DRILL_CLASSPATH
#     Optional extra classpath after all Drill-provided jars. This is the
#     typical place to add jars needed by plugins, etc. (Note, no need to set
#     this if the jars reside in the $DRILL_HOME/jars/3rdparty directory.)
#     Config parameter is drill.yarn.drillbit.drill-classpath.
# DRILL_JVM_OPTS
#     Additional JVM options passed via YARN from the
#     drill.yarn.drillbit.vm-args parameter.
# ENABLE_GC_LOG
#     Enables Java GC logging. Passed from the drill.yarn.drillbit.log-gc
#     garbage collection option.

if [ -n "$DRILL_DEBUG" ]; then
  echo
  echo "Drillbit Environment from YARN:"
  echo "-----------------------------------"
  env
  echo "-----------------------------------"
fi

# DRILL_HOME is set by the AM to point to the Drill distribution.

# In YARN, configuration defaults to the the standard location.

DRILL_CONF_DIR=${DRILL_CONF_DIR:-$DRILL_HOME/conf}

# On some distributions, YARN sets HADOOP_CLASSPATH, but for a different
# purpose than the one to which Drill puts that variable. Unset the YARN
# version and only use the variable if drill-env.sh sets it.

unset HADOOP_CLASSPATH
unset HBASE_CLASSPATH

# Replace the values with the values from the Drill-on-YARN config
# file, if given.

if [ -n "$DRILL_HADOOP_CLASSPATH" ]; then
  export HADOOP_CLASSPATH="$DRILL_HADOOP_CLASSPATH"
fi
if [ -n "$DRILL_HBASE_CLASSPATH" ]; then
  export HBASE_CLASSPATH="$DRILL_HBASE_CLASSPATH"
fi

# HADOOP_HOME can be set by Drill-on-YARN, or in drill-env.sh. The
# latter takes precedence. It seems that YARN does not use the
# HADOOP_HOME variable; instead it provides HADOOP_COMMON_HOME,
# HADOOP_HDFS_HOME, and HADOOP_YARN_HOME.
#
# Default HADOOP_COMMON_HOME if HADOOP_HOME is not otherwise set in a YARN
# environment.

if [ -n "$HADOOP_HOME" ]; then
  export HADOOP_HOME="$HADOOP_COMMON_HOME"
fi

# Use Drill's standard configuration, including drill-env.sh.

. "$DRILL_HOME/bin/drill-config.sh"

# Debugging information

if [ -n "$DRILL_DEBUG" ]; then
  echo "Command:"
  "$DRILL_HOME/bin/runbit" debug
  echo
  echo "Local Environment:"
  echo "-----------------------------------"
  set
  echo "-----------------------------------"
fi

# Launch Drill itself.
# Passes along Drill's exit code as our own.

echo "`date` Starting drillbit on `hostname` under YARN, logging to $DRILLBIT_LOG_PATH"
echo "`ulimit -a`" >> "$DRILLBIT_LOG_PATH" 2>&1

"$DRILL_HOME/bin/runbit" exec
