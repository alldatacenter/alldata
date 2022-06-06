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

allargs=$@

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

# Construct Atlas classpath using jars from hook/kafka/atlas-kafka-plugin-impl/ directory.
for i in "${BASEDIR}/hook/kafka/atlas-kafka-plugin-impl/"*.jar; do
  ATLASCPPATH="${ATLASCPPATH}:$i"
done

if [ -z "${ATLAS_CONF_DIR}" ] && [ -e /etc/atlas/conf ];then
    ATLAS_CONF_DIR=/etc/atlas/conf
fi
ATLASCPPATH=${ATLASCPPATH}:${ATLAS_CONF_DIR}

# log dir for applications
ATLAS_LOG_DIR="${ATLAS_LOG_DIR:-/var/log/atlas}"
export ATLAS_LOG_DIR
LOGFILE="$ATLAS_LOG_DIR/import-kafka.log"

TIME=`date +%Y%m%d%H%M%s`

#Add Kafka conf in classpath
if [ ! -z "$KAFKA_CONF_DIR" ]; then
    KAFKA_CONF=$KAFKA_CONF_DIR
elif [ ! -z "$KAFKA_HOME" ]; then
    KAFKA_CONF="$KAFKA_HOME/conf"
elif [ -e /etc/kafka/conf ]; then
    KAFKA_CONF="/etc/kafka/conf"
else
    echo "Could not find a valid KAFKA configuration"
    exit 1
fi

echo Using Kafka configuration directory "[$KAFKA_CONF]"


if [ -f "${KAFKA_CONF}/kafka-env.sh" ]; then
  . "${KAFKA_CONF}/kafka-env.sh"
fi

if [ -z "$KAFKA_HOME" ]; then
    if [ -d "${BASEDIR}/../kafka" ]; then
        KAFKA_HOME=${BASEDIR}/../kafka
    else
        echo "Please set KAFKA_HOME to the root of Kafka installation"
        exit 1
    fi
fi

KAFKA_CP="${KAFKA_CONF}"

for i in "${KAFKA_HOME}/libs/"*.jar; do
    KAFKA_CP="${KAFKA_CP}:$i"
done


#Add hadoop conf in classpath
if [ ! -z "$HADOOP_CLASSPATH" ]; then
    HADOOP_CP=$HADOOP_CLASSPATH
elif [ ! -z "$HADOOP_HOME" ]; then
    HADOOP_CP=`$HADOOP_HOME/bin/hadoop classpath`
elif [ $(command -v hadoop) ]; then
    HADOOP_CP=`hadoop classpath`
   #echo $HADOOP_CP
else
    echo "Environment variable HADOOP_CLASSPATH or HADOOP_HOME need to be set"
    exit 1
fi

CP="${ATLASCPPATH}:${HADOOP_CP}:${KAFKA_CP}"

# If running in cygwin, convert pathnames and classpath to Windows format.
if [ "${CYGWIN}" == "true" ]
then
   ATLAS_LOG_DIR=`cygpath -w ${ATLAS_LOG_DIR}`
   LOGFILE=`cygpath -w ${LOGFILE}`
   KAFKA_CP=`cygpath -w ${KAFKA_CP}`
   HADOOP_CP=`cygpath -w ${HADOOP_CP}`
   CP=`cygpath -w -p ${CP}`
fi

JAVA_PROPERTIES="$ATLAS_OPTS -Datlas.log.dir=$ATLAS_LOG_DIR -Datlas.log.file=import-kafka.log
-Dlog4j.configuration=atlas-kafka-import-log4j.xml"
shift

while [[ ${1} =~ ^\-D ]]; do
  JAVA_PROPERTIES="${JAVA_PROPERTIES} ${1}"
  shift
done

echo "Log file for import is $LOGFILE"

"${JAVA_BIN}" ${JAVA_PROPERTIES} -cp "${CP}" org.apache.atlas.kafka.bridge.KafkaBridge $allargs

RETVAL=$?
[ $RETVAL -eq 0 ] && echo Kafka Data Model imported successfully!!!
[ $RETVAL -ne 0 ] && echo Failed to import Kafka Data Model!!!

exit $RETVAL

