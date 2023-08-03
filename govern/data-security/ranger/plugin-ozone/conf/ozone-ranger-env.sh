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

classpathmunge () {
        escaped=`echo $1 | sed -e 's:\*:\\\\*:g'`
        if ! echo ${CLASSPATH} | /bin/egrep -q "(^|:)${escaped}($|:)" ; then
           if [ "$2" = "before" ] ; then
              CLASSPATH=$1:${CLASSPATH}
           else
              CLASSPATH=${CLASSPATH}:$1
           fi
        fi
}
classpathmunge /etc/ozone/conf
classpathmunge '/usr/hdp/current/hadoop-hdfs-client/*'
classpathmunge '/usr/hdp/current/hadoop-hdfs-client/lib/*'
classpathmunge '/etc/hadoop/conf'
export CLASSPATH
unset classpathmunge
