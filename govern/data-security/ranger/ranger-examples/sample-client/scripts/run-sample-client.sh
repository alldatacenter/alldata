#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

usage() {
  echo "usage: run-sample-client.sh
  -n <arg> Hostname to connect to
  -h       show help."
  exit 1
}
JARS=
  for i in lib/*.jar
do
    JARS="${JARS}:$i"
done
JAVA_CMD="java -Dlogback.configurationFile=file:lib/logback.xml -cp ${JARS} org.apache.ranger.examples.sampleclient.SampleClient"
while getopts "n:h" opt; do
  case $opt in
    n) HOST=$OPTARG
	JAVA_CMD="$JAVA_CMD -h $HOST"
	;;
    h) usage
	;;
    \?) echo -e \\n"Option -$OPTARG not allowed."
	usage
	;;
  esac
done

if [[ $HOST == https*  ]] ;
then
  prompt="SSL Configuration File:"
  read -p "$prompt" config
  JAVA_CMD="$JAVA_CMD -c $config"
fi
prompt="Kerberos Login (y/n)? "
read -p "$prompt" -n 1 -r
printf "\n"
if [[ $REPLY =~ ^[Yy]$ ]]
then
  prompt="Sample Kerberos Principal:"
  read -r -p "$prompt" userName
  prompt="Sample Kerberos Keytab:"
  read -r -p "$prompt" password
  printf "\n"
  JAVA_CMD="$JAVA_CMD -k kerberos -u $userName -p $password"
elif [[ $REPLY =~ ^[Nn]$ ]]
then
  prompt="Sample Authentication User Name:"
  read -r -p "$prompt" userName
  prompt="Sample Authentication User Password:"
  read -r -p "$prompt" -s password
  printf "\n"
  JAVA_CMD="$JAVA_CMD -k basic -u $userName -p $password"
else
  printf "Incorrect response \n"
  exit
fi

printf "Java command : $JAVA_CMD\n"
$JAVA_CMD
