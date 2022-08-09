#!/usr/bin/env bash
#
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
#

export hdfs_user=$1
shift
export conf_dir=$1
shift
export bin_dir=$1
shift
export mark_dir=$1
shift
export name_dirs=$*

export EXIT_CODE=0
export command="namenode -format"
export list_of_non_empty_dirs=""

mark_file=/var/run/hadoop/hdfs/namenode-formatted
if [[ -f ${mark_file} ]] ; then
  /var/lib/ambari-agent/ambari-sudo.sh rm -f ${mark_file}
  /var/lib/ambari-agent/ambari-sudo.sh mkdir -p ${mark_dir}
fi

if [[ ! -d $mark_dir ]] ; then
  for dir in `echo $name_dirs | tr ',' ' '` ; do
    echo "NameNode Dirname = $dir"
    cmd="ls $dir | wc -l  | grep -q ^0$"
    eval $cmd
    if [[ $? -ne 0 ]] ; then
      (( EXIT_CODE = $EXIT_CODE + 1 ))
      list_of_non_empty_dirs="$list_of_non_empty_dirs $dir"
    fi
  done

  if [[ $EXIT_CODE == 0 ]] ; then
    /var/lib/ambari-agent/ambari-sudo.sh su ${hdfs_user} - -s /bin/bash -c "export PATH=$PATH:$bin_dir ; yes Y | hdfs --config ${conf_dir} ${command}"
    (( EXIT_CODE = $EXIT_CODE | $? ))
  else
    echo "ERROR: Namenode directory(s) is non empty. Will not format the namenode. List of non-empty namenode dirs ${list_of_non_empty_dirs}"
  fi
else
  echo "${mark_dir} exists. Namenode DFS already formatted"
fi

exit $EXIT_CODE

