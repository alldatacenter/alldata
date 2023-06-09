#!/bin/bash
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

# Launch script for the Drill Application Master (AM).
# This script runs under YARN and assumes the environment that YARN provides to an AM.
# This script likely will not work from the command line.
#
# Environment variables set by the client:
#
# DRILL_DEBUG       Set to 1 to print environment and other information to
#                   diagnose problems.
# DRILL_AM_HEAP     AM heap memory. (The AM uses no direct memory.)
# DRILL_AM_JAVA_OPT Optional additional JVM options for the AM, such as
#                   options to enable debugging.
#
# The following environment variables are set in the AM launch context,
# not used by this script, but used the the AM itself.
#
# DRILL_AM_APP_ID   Informs the AM of its YARN application ID.
#                   (Strangely, YARN provides no way for an AM to learn this
#                   from YARN itself.)
# YARN_RM_WEBAPP    Informs the AM of the URL to the YARN RM web app.
#                   Again, YARN informs the Client of this information, but
#                   not the AM.
# DRILL_ARCHIVE     The DFS path to the Drill archive used to localize Drillbit
#                   code.
# SITE_ARCHIVE      The DFS path to the optional site archive used to localize
#                   Drillbit configuration.
#
# Further, this script infers DRILL_HOME from the location
# of the script itself. The site directory (if used) is provided
# via the --config command-line option.

# YARN requires that the AM run as a child process until completion; so this script
# does not launch the AM in the background.

# This script is run from $DRILL_HOME/bin, wherever the user has configured it.

bin=`dirname "${BASH_SOURCE-$0}"`
bin=`cd "$bin">/dev/null; pwd`
DRILL_HOME=`cd "$bin/..">/dev/null; pwd`

if [ -n "$DRILL_DEBUG" ]; then
  echo
  echo "Drill AM launch script"
  echo "DRILL_HOME: $DRILL_HOME"
fi

# AM-specific options for drill-config.sh. The AM
# code is in the tools folder which is not loaded by
# the Drillbit, only by the AM and client.
#
# Add the Hadoop config directory which we need to gain access to
# YARN and HDFS. This is an odd location to add the config dir,
# but if we add it sooner, Jersey complains with many class not
# found errors for reasons not yet known. Note that, to add the
# Hadoop config here, the Drill 1.6 $DRILL_HOME/conf/core-site.xml file
# MUST have been removed or renamed else Hadoop will pick up
# our dummy file instead of the real Hadoop file.

DRILL_TOOL_CP="$DRILL_HOME/jars/tools/*:$HADOOP_CONF_DIR"

# Use Drill's standard configuration, including drill-env.sh.
# The AM discards most of the information, but does use JAVA
# and a few others.

. "$DRILL_HOME/bin/drill-config.sh"

# DRILL_AM_HEAP and DRILL_AM_JAVA_OPTS are set by the
# Drill client via YARN. To set these, use the following
# configuration options:
#
# DRILL_AM_HEAP: drill.yarn.am.heap
# DRILL_AM_JAVA_OPTS: drill.yarn.am.vm-args

DRILL_AM_HEAP="${DRILL_AM_HEAP:-512M}"

# AM logging setup. Note: the Drillbit log file uses the default name
# of logback.xml.
# The AM uses a non-default log configuration file name.
# So, we must tell the AM to use an AM-specific file
# else we'll get warnings about the log.query.path system property
# not being set (and we won't pick up the AM logging settings.)
# See http://logback.qos.ch/manual/configuration.html
# The name provided must be on the class path. By adding
# the site dir before $DRILL_HOME/conf, the user can
# provide a custom config without editing the default one.
# If this is wrong, you will see files such as
# log.path_IS_UNDEFINED in the launch directory.

AM_LOG_CONF="-Dlogback.configurationFile=drill-am-log.xml"
#SITE_OPT="-Ddrill.yarn.siteDir=$DRILL_CONF_DIR"

AM_JAVA_OPTS="-Xms$DRILL_AM_HEAP -Xmx$DRILL_AM_HEAP -XX:MaxPermSize=512M"
AM_JAVA_OPTS="$AM_JAVA_OPTS $SITE_OPT $DRILL_AM_JAVA_OPTS $AM_LOG_CONF"
if [ -n "$DRILL_JAVA_LIB_PATH" ]; then
  AM_JAVA_OPTS="$AM_JAVA_OPTS -Djava.library.path=$DRILL_JAVA_LIB_PATH"
fi

# drill-config.sh built the class path.
# Note that the class path uses the Hadoop, YARN and DFS jars
# packaged with Drill; not those from the YARN-provided
# environment variables in the launch context.

AMCMD="$JAVA $AM_JAVA_OPTS ${args[@]} -cp $CP org.apache.drill.yarn.appMaster.DrillApplicationMaster"

if [ -n "$DRILL_DEBUG" ]; then
  echo "AM launch environment:"
  echo "-----------------------------------"
  env
  echo "-----------------------------------"
  echo "Command:"
  echo "$AMCMD"
fi

# Note: no need to capture output, YARN does that for us.
# AM is launched as a child process of caller, replacing this script.

# Replace this script process with the AM. Needed so that
# the YARN node manager can kill the AM if necessary by killing
# the PID for this script.

exec $AMCMD
