#!/usr/bin/env bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

: "${DEBUG:=0}"

validate_input_arguments(){

# the first argument is the csv file | username, group1, group2
CSV_FILE="$1"
: "${CSV_FILE:?"Missing csv file input for the post-user creation hook"}"

# the second argument is the cluster security type
SECURITY_TYPE=$2
: "${SECURITY_TYPE:?"Missing security type input for the post-user creation hook"}"

# the last argument is the user with dfs administrator privileges
HDFS_USR=${@: -1}
}


# wraps the command passed in as argument to be called via the ambari sudo
ambari_sudo(){

ARG_STR="$1"
CMD_STR="/var/lib/ambari-server/ambari-sudo.sh su '$HDFS_USR' -l -s /bin/bash -c '$ARG_STR'"
echo "Executing command: [ $CMD_STR ]"
eval "$CMD_STR"
}

setup_security(){
if [ "$SECURITY_TYPE" ==  "KERBEROS" ]
then
  HDFS_PRINCIPAL=$3
: "${HDFS_PRINCIPAL:?"Missing hdfs principal for the post-user creation hook"}"

  HDFS_KEYTAB=$4
: "${HDFS_KEYTAB:?"Missing hdfs principal for the post-user creation hook"}"

  echo "The cluster is secure, calling kinit ..."
  kinit_cmd="/usr/bin/kinit -kt $HDFS_KEYTAB $HDFS_PRINCIPAL"

  ambari_sudo "$kinit_cmd"
else
 echo "The cluster security type is $SECURITY_TYPE"
fi
}

check_tools(){
echo "Checking for required tools ..."

# check for hadoop
ambari_sudo "type hadoop > /dev/null 2>&1 || { echo >&2 \"hadoop client not installed\"; exit 1; }"

# check for the hdfs
ambari_sudo "hadoop fs -ls / > /dev/null 2>&1 || { echo >&2 \"hadoop dfs not available\"; exit 1; }"

echo "Checking for required tools ... DONE."

}

prepare_input(){
# perform any specific logic on the arguments
echo "Processing post user creation hook payload ..."

JSON_INPUT="$CSV_FILE.json"
echo "Generating json file $JSON_INPUT ..."

echo "[" | cat > "$JSON_INPUT"
while read -r LINE
do
  USR_NAME=$(echo "$LINE" | awk -F, '{print $1}')
  echo "Processing user name: $USR_NAME"

  # encoding the username
  USR_NAME=$(printf "%q" "$USR_NAME")

  cat <<EOF >> "$JSON_INPUT"
    {
    "target":"/user/$USR_NAME",
    "type":"directory",
    "action":"create",
    "owner":"$USR_NAME",
    "group":"hdfs",
    "manageIfExists": "true"
  },
EOF
done <"$CSV_FILE"

# Setting read permissions on the generated file
chmod 644 $JSON_INPUT

# deleting the last line
sed -i '$ d' "$JSON_INPUT"

# appending json closing elements to the end of the file
echo $'}\n]' | cat >> "$JSON_INPUT"
echo "Generating file $JSON_INPUT ... DONE."
echo "Processing post user creation hook payload ... DONE."

}


# encapsulates the logic of the post user creation script
main(){

echo $DEBUG;

if [ "$DEBUG" != "0" ]; then echo "Switch debug ON";set -x; else echo "debug: OFF"; fi

echo "Executing user hook with parameters: $*"

validate_input_arguments "$@"

setup_security "$@"

check_tools

prepare_input

# the default implementation creates user home folders; the first argument must be the username
ambari_sudo "yarn jar /var/lib/ambari-server/resources/stack-hooks/before-START/files/fast-hdfs-resource.jar $JSON_INPUT"

if [ "$DEBUG" -gt "0" ]; then echo "Switch debug OFF";set -x;unset DEBUG; else echo "debug: OFF"; fi
unset DEBUG
}


main "$@"
