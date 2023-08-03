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

BASEDIR=`dirname ${PRG}`

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

# Construct ATLAS_CONF where atlas-properties reside
export ATLAS_CONF=/usr/hdp/current/atlas-server/conf/

# log dir for applications
ATLAS_LOG_DIR="/var/log/atlas"
ATLAS_LOG_FILE="classification-updater.log"
LOG_CONFIG="${BASEDIR}/atlas-log4j.xml"

# Construct Atlas classpath. 
for i in "/usr/hdp/current/atlas-server/server/webapp/atlas/WEB-INF/lib/"*.jar; do
  ATLASCPPATH="${ATLASCPPATH}:$i"
done

for i in "${BASEDIR}/"*.jar; do
  ATLASCPPATH="${ATLASCPPATH}:$i"
done

echo "Logging: ${ATLAS_LOG_DIR}/${ATLAS_LOG_FILE}"
echo "Log config: ${LOG_CONFIG}"

TIME=`date +%Y%m%d%H%M%s`
CP="${ATLASCPPATH}:${ATLAS_CONF}"

# If running in cygwin, convert pathnames and classpath to Windows format.
if [ "${CYGWIN}" == "true" ]
then
   ATLAS_LOG_DIR=`cygpath -w ${ATLAS_LOG_DIR}`
   ATLAS_LOG_FILE=`cygpath -w ${ATLAS_LOG_FILE}`
   CP=`cygpath -w -p ${CP}`
fi

JAVA_PROPERTIES="$ATLAS_OPTS -Datlas.log.dir=$ATLAS_LOG_DIR -Datlas.log.file=$ATLAS_LOG_FILE -Dlog4j.configuration=file://$LOG_CONFIG"

IMPORT_ARGS=$@
JVM_ARGS=

JAVA_PROPERTIES="${JAVA_PROPERTIES} ${JVM_ARGS}"

"${JAVA_BIN}" ${JAVA_PROPERTIES} -cp "${CP}" org.apache.atlas.tools.BulkFetchAndUpdate $IMPORT_ARGS

RETVAL=$?
[ $RETVAL -eq 0 ] && echo Done!
[ $RETVAL -ne 0 ] && echo Failed!
exit $RETVAL
