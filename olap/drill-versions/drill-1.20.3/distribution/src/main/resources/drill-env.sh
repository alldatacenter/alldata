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

# This file provides a variety of site-specific settings to control Drill
# launch settings. These are settings required when launching the Drillbit
# or sqlline processes using Java. Some settings are for both, some for one
# or the other.
#
# Variables may be set in one of four places:
#
#   Environment (per run)
#   drill-env.sh (this file, per site)
#   distrib-env.sh (per distribution)
#   drill-config.sh (Drill defaults)
#
# Properties "inherit" from items lower on the list, and may be "overridden" by items
# higher on the list. In the environment, just set the variable:
#
#   export FOO=value
#
# To support inheritance from the environment, you must set values as shown below:
#
#   export FOO=${FOO:-"value"}
#
# or a more specialized form.

# Amount of total memory for the Drillbit process. This value is defined as the limit
# that the startup script will try to enforce on the Drill JVM. The values can be
# defined in terms of percentage of the available system memory, or in terms of actual
# values, similar to how we define the actual JVM memory parameters like Heap Size.
# There is no default and depends on how much can be allotted on a machine.
# This enables Drill's memory auto-configuration logic to kick in, and should be unset
# if the intent is to not use the auto-configuration.

#export DRILLBIT_MAX_PROC_MEM=${DRILLBIT_MAX_PROC_MEM:-"13G"}

# Amount of heap memory for the Drillbit process. Values are those supported by
# the Java -Xms option. The default is 4G.

#export DRILL_HEAP=${DRILL_HEAP:-"4G"}

# Maximum amount of direct memory to allocate to the Drillbit in the format
# supported by -XX:MaxDirectMemorySize. Default is 8G.

#export DRILL_MAX_DIRECT_MEMORY=${DRILL_MAX_DIRECT_MEMORY:-"8G"}

# Native library path passed to Java. Note: use this form instead
# of the old form of DRILLBIT_JAVA_OPTS="-Djava.library.path=<dir>"
# The old form is not compatible with Drill-on-YARN.

# export DRILL_JAVA_LIB_PATH="<lib1>:<lib2>"

# Value for the code cache size for the Drillbit. Because the Drillbit generates
# code, it benefits from a large cache. Default is 1G.

#export DRILLBIT_CODE_CACHE_SIZE=${DRILLBIT_CODE_CACHE_SIZE:-"1G"}

# Provide a customized host name for when the default mechanism is not accurate

#export DRILL_HOST_NAME=`hostname`

# Base name for Drill log files. Files are named ${DRILL_LOG_NAME}.out, etc.

# DRILL_LOG_NAME="drillbit"

# Location to place Drill logs. Set to $DRILL_HOME/log by default.

#export DRILL_LOG_DIR=${DRILL_LOG_DIR:-$DRILL_HOME/log}

# Location to place the Drillbit pid file when running as a daemon using
# drillbit.sh start.
# Set to $DRILL_HOME by default.

#export DRILL_PID_DIR=${DRILL_PID_DIR:-$DRILL_HOME}

# Default (Standard) CGroup Location: /sys/fs/cgroup
# Specify the cgroup location if it is different from the default
#export SYS_CGROUP_DIR=${SYS_CGROUP_DIR:-"/sys/fs/cgroup"}

# CGroup to which the Drillbit belongs when running as a daemon using drillbit.sh start .
# Drill will use CGroup for CPU enforcement only.

# Unset $DRILLBIT_CGROUP by default
#export DRILLBIT_CGROUP=${DRILLBIT_CGROUP:-"drillcpu"}

# Custom JVM arguments to pass to the both the Drillbit and sqlline. Typically
# used to override system properties as shown below. Empty by default.

#export DRILL_JAVA_OPTS="$DRILL_JAVA_OPTS -Dproperty=value"

# As above, but only for the Drillbit. Empty by default.

#export DRILLBIT_JAVA_OPTS="$DRILLBIT_JAVA_OPTS -Dproperty=value"

# Process priority (niceness) for the Drillbit when running as a daemon.
# Defaults to 0.

#export DRILL_NICENESS=${DRILL_NICENESS:-0}

# Custom class path for Drill. In general, you should put your custom libraries into
# your site directory's jars subfolder ($DRILL_HOME/conf/jars by default, but can be
# customized with DRILL_CONF_DIR or the --config argument. But, if you must reference
# jar files in other locations, you can add them here. These jars are added to the
# Drill classpath after all Drill-provided jars. Empty by default.

# custom="/your/path/here:/your/second/path"
# if [ -z "$DRILL_CLASSPATH" ]; then
#   export DRILL_CLASSPATH=${DRILL_CLASSPATH:$custom}
# else
#   export DRILL_CLASSPATH="$custom"
# fi

# Extension classpath for things like HADOOP, HBase and so on. Set as above.

# EXTN_CLASSPATH=...

# Note that one environment variable can't be set here: DRILL_CONF_DIR.
# That variable tells Drill the location of this file, so this file can't
# set it. Instead, you can set it in the environment, or using the
# --config option of drillbit.sh or sqlline.

#-----------------------------------------------------------------------------
# The following are "advanced" options seldom used except when diagnosing
# complex issues.
#
# The prefix class path appears before any Drill-provided classpath entries.
# Use it to override Drill jars with specialized versions.

#export DRILL_CLASSPATH_PREFIX=...

# Enable garbage collection logging in the Drillbit. Logging goes to
# $DRILL_LOG_DIR/drillbit.gc. A value of 1 enables logging, all other values
# (including the default unset value) disables logging.

#export SERVER_LOG_GC=${SERVER_LOG_GC:-1}

# JVM options when running the sqlline Drill client.
# These are used ONLY in non-embedded mode; these
# are client-only settings. (The Drillbit settings are used when Drill
# is embedded.)

#export SQLLINE_JAVA_OPTS=""

# Arguments passed to sqlline (the Drill shell) at all times: whether
# Drill is embedded in Sqlline or not.

#export DRILL_SHELL_JAVA_OPTS="..."

# Location Drill should use for temporary files, such as downloaded dynamic UDFs jars.
# Set to "/tmp" by default.
#
# export DRILL_TMP_DIR="..."

# Block to put environment variable known to both Sqlline and Drillbit, but needs to be
# differently set for both. OR set for one and unset for other.
#
# if [ "$DRILLBIT_CONTEXT" = "1" ]; then
#   Set environment variable value to be consumed by Drillbit
# else
#   Set environment variable value to be consumed by Sqlline
# fi
#
