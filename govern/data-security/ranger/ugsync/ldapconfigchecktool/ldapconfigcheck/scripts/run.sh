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


AUTH=1

usage() {
  echo "usage: run.sh
 -a         ignore authentication properties
 -d <arg>   {all|users|groups}
 -h                       show help.
 -i <arg>            Input file name
 -o <arg>            Output directory
 -r <arg>             {all|users|groups}"
  exit 1
}

cdir=`pwd`
cp="${cdir}/lib/*:${cdir}/conf"
OUTDIR="${cdir}/output/"
JAVA_CMD="java -cp ${cdir}/lib/ldapconfigcheck.jar:${cp} org.apache.ranger.ldapconfigcheck.LdapConfigCheckMain"
INPUTFILE=""
while getopts "i:o:d:r:ah" opt; do
  case $opt in
    i) INFILE=$OPTARG
    INPUTFILE=$OPTARG
	JAVA_CMD="$JAVA_CMD -i $OPTARG"
	;;
    o) OUTDIR=$OPTARG
	;;
    d) DISCOVER=$OPTARG
	JAVA_CMD="$JAVA_CMD -d $OPTARG"
	;;
    r) RETRIEVE=$OPTARG
	JAVA_CMD="$JAVA_CMD -r $OPTARG"
	;;
    a) AUTH=0
	JAVA_CMD="$JAVA_CMD -a"
	;;
    h) usage
	;;
    \?) echo -e \\n"Option -$OPTARG not allowed."
	usage
	;;
  esac
done

JAVA_CMD="$JAVA_CMD -o $OUTDIR"

echo "JAVA commnad = $JAVA_CMD"

if [ "${INPUTFILE}" != "" ]
then
	prompt="Ldap Bind Password:"
	read -p "$prompt" -s password
	JAVA_CMD="$JAVA_CMD -p $password"
fi

if [${AUTH} == 1]
then
	prompt="Sample Authentication User Password:"
	read -p "$prompt" -s authPassword
	JAVA_CMD="$JAVA_CMD -u $authPassword"
fi

if [ "${JAVA_HOME}" != "" ]
then
	export JAVA_HOME
	PATH="${JAVA_HOME}/bin:${PATH}"
	export PATH
fi

cd ${cdir}
$JAVA_CMD
