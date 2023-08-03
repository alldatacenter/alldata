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

if [[ -z $1 ]]; then
        echo "No argument provided.."
        echo "Usage: $0 tag-source-class-name [<more-arguments>]"
        exit -1;
fi
action=$1
className=
realScriptPath=`readlink -f $0`
realScriptDir=`dirname $realScriptPath`
cd $realScriptDir
cdir=`pwd`

if [ "${action}" == "file" ]; then
	action=file
	className=org.apache.ranger.tagsync.source.file.FileTagSource

elif [ "${action}" == "atlasrest" ]; then
	action=atlasrest
	className=org.apache.ranger.tagsync.source.atlasrest.AtlasRESTTagSource
else
	className=${action}
fi

shift

if [ -f ${cdir}/conf/java_home.sh ]; then
	. ${cdir}/conf/java_home.sh
fi

for custom_env_script in `find ${cdir}/conf.dist/ -name "ranger-tagsync-env*"`; do
       	if [ -f $custom_env_script ]; then
               	. $custom_env_script
        fi
done

if [ "$JAVA_HOME" != "" ]; then
       	export PATH=$JAVA_HOME/bin:$PATH
fi

logdir=/var/log/ranger/tagsync-$action

if [ ! -d $logdir ]; then
	mkdir -p $logdir
	chmod 777 $logdir
fi

cp="${cdir}/conf:${cdir}/dist/*:${cdir}/lib/*"

cd ${cdir}
umask 0077

java -Dproc_rangertagsync-${action} ${JAVA_OPTS} -Dlogdir="${logdir}" -Dlogback.configurationFile=file:/etc/ranger/tagsync/conf/logback.xml -cp "${cp}" ${className} $* > ${logdir}/tagsync.out 2>&1

