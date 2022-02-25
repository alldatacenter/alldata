#!/bin/bash
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License. See accompanying LICENSE file.
#

# resolve links - $0 may be a softlink
PRG="${0}"

[[ `uname -s` == *"CYGWIN"* ]] && CYGWIN=true

while [ -h "${PRG}" ]; do
  ls=`ls -ld "${PRG}"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "${PRG}"`/"$link"
  fi
done

echo ">>>>> $PRG"

BASEDIR=`dirname ${PRG}`
BASEDIR=`cd ${BASEDIR}/..;pwd`

echo ">>>>> $BASEDIR"

if test -z "${JAVA_HOME}"
then
    JAVA_BIN=`which java`
    JAR_BIN=`which jar`
else
    JAVA_BIN="${JAVA_HOME}/bin/java"
    JAR_BIN="${JAVA_HOME}/bin/jar"
fi
export JAVA_BIN

if [ ! -e "${JAVA_BIN}" ] || [ ! -e "${JAR_BIN}" ]; then
  echo "$JAVA_BIN and/or $JAR_BIN not found on the system. Please make sure java and jar commands are available."
  exit 1
fi

# Construct Atlas classpath using jars from hook/hbase/atlas-hbase-plugin-impl/ directory.
for i in "${BASEDIR}/hook/hbase/atlas-hbase-plugin-impl/"*.jar; do
  ATLASCPPATH="${ATLASCPPATH}:$i"
done

if [ -z "${ATLAS_CONF_DIR}" ] && [ -e /etc/atlas/conf ];then
    ATLAS_CONF_DIR=/etc/atlas/conf
fi
ATLASCPPATH=${ATLASCPPATH}:${ATLAS_CONF_DIR}

# log dir for applications
ATLAS_LOG_DIR="${ATLAS_LOG_DIR:-/var/log/atlas}"
export ATLAS_LOG_DIR
LOGFILE="$ATLAS_LOG_DIR/import-hbase.log"

TIME=`date +%Y%m%d%H%M%s`

#Add HBase conf in classpath
if [ ! -z "$HBASE_CONF_DIR" ]; then
    HBASE_CONF=$HBASE_CONF_DIR
elif [ ! -z "$HBASE_HOME" ]; then
    HBASE_CONF="$HBASE_HOME/conf"
elif [ -e /etc/hbase/conf ]; then
    HBASE_CONF="/etc/hbase/conf"
else
    echo "Could not find a valid HBASE configuration"
    exit 1
fi

echo Using HBase configuration directory "[$HBASE_CONF]"


if [ -f "${HBASE_CONF}/hbase-env.sh" ]; then
  . "${HBASE_CONF}/hbase-env.sh"
fi

if [ -z "$HBASE_HOME" ]; then
    if [ -d "${BASEDIR}/../hbase" ]; then
        HBASE_HOME=${BASEDIR}/../hbase
    else
        echo "Please set HBASE_HOME to the root of HBase installation"
        exit 1
    fi
fi

HBASE_CP="${HBASE_CONF}"

for i in "${HBASE_HOME}/lib/"*.jar; do
    HBASE_CP="${HBASE_CP}:$i"
done

#Add hadoop conf in classpath
if [ ! -z "$HADOOP_CLASSPATH" ]; then
    HADOOP_CP=$HADOOP_CLASSPATH
elif [ ! -z "$HADOOP_HOME" ]; then
    HADOOP_CP=`$HADOOP_HOME/bin/hadoop classpath`
elif [ $(command -v hadoop) ]; then
    HADOOP_CP=`hadoop classpath`
    echo $HADOOP_CP
else
    echo "Environment variable HADOOP_CLASSPATH or HADOOP_HOME need to be set"
    exit 1
fi

CP="${HBASE_CP}:${HADOOP_CP}:${ATLASCPPATH}"

# If running in cygwin, convert pathnames and classpath to Windows format.
if [ "${CYGWIN}" == "true" ]
then
   ATLAS_LOG_DIR=`cygpath -w ${ATLAS_LOG_DIR}`
   LOGFILE=`cygpath -w ${LOGFILE}`
   HBASE_CP=`cygpath -w ${HBASE_CP}`
   HADOOP_CP=`cygpath -w ${HADOOP_CP}`
   CP=`cygpath -w -p ${CP}`
fi

JAVA_PROPERTIES="$ATLAS_OPTS -Datlas.log.dir=$ATLAS_LOG_DIR -Datlas.log.file=import-hbase.log
-Dlog4j.configuration=atlas-hbase-import-log4j.xml"

IMPORT_ARGS=
JVM_ARGS=

while true
do
  option=$1
  shift

  case "$option" in
    -n) IMPORT_ARGS="$IMPORT_ARGS -n $1"; shift;;
    -t) IMPORT_ARGS="$IMPORT_ARGS -t $1"; shift;;
    -f) IMPORT_ARGS="$IMPORT_ARGS -f $1"; shift;;
    --namespace) IMPORT_ARGS="$IMPORT_ARGS --namespace $1"; shift;;
    --table) IMPORT_ARGS="$IMPORT_ARGS --table $1"; shift;;
    --filename) IMPORT_ARGS="$IMPORT_ARGS --filename $1"; shift;;
    "") break;;
    *) JVM_ARGS="$JVM_ARGS $option"
  esac
done

JAVA_PROPERTIES="${JAVA_PROPERTIES} ${JVM_ARGS}"

echo "Log file for import is $LOGFILE"

"${JAVA_BIN}" ${JAVA_PROPERTIES} -cp "${CP}" org.apache.atlas.hbase.bridge.HBaseBridge $IMPORT_ARGS

RETVAL=$?
[ $RETVAL -eq 0 ] && echo HBase Data Model imported successfully!!!
[ $RETVAL -ne 0 ] && echo Failed to import HBase Data Model!!!

exit $RETVAL
