#!/bin/sh
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

# Delete 2 dirs
sudo -u hdfs hadoop fs -rm -r /tmp/some999
sudo -u hdfs hadoop fs -rm -r /tmp/some888
# Create
sudo -u hdfs hadoop fs -mkdir -p /tmp/some999/more/dirs/for/recursive/tests
# Create + permissions + owner
sudo -u hdfs hadoop fs -mkdir -p /tmp/some888/more/dirs/for/recursive/tests
sudo -u hdfs hadoop fs -chown hadoop:hadoop /tmp/some888/more/dirs/for/recursive/tests
sudo -u hdfs hadoop fs -chmod 777 /tmp/some888/more/dirs/for/recursive/tests
# Empty dirs with permissions/owners to last dir"
sudo -u hdfs hadoop fs -mkdir -p /tmp/some888/and_more/and_dirs/_andfor/recursive/tests
sudo -u hdfs hadoop fs -chmod 777 /tmp/some888/and_more/and_dirs/_andfor/recursive/tests
sudo -u hdfs hadoop fs -chown hadoop:hadoop /tmp/some888/and_more/and_dirs/_andfor/recursive/tests
# Empty dirs with permissions/owners to last file
sudo -u hdfs hadoop fs -touchz /tmp/some888/file.txt
sudo -u hdfs hadoop fs -chown hadoop:hadoop /tmp/some888/file.txt
sudo -u hdfs hadoop fs -chmod 777 /tmp/some888/file.txt
# Empty dirs with permissions/owners to last file
sudo -u hdfs hadoop fs -touchz /tmp/some888/and_more/and_dirs/file2.txt
sudo -u hdfs hadoop fs -chown hadoop:hadoop /tmp/some888/and_more/and_dirs/file2.txt
sudo -u hdfs hadoop fs -chmod 777 /tmp/some888/and_more/and_dirs/file2.txt
# Recursive permissions
sudo -u hdfs hadoop fs -chmod -R 700 /tmp/some888
sudo -u hdfs hadoop fs -chown -R hive:hive /tmp/some999


