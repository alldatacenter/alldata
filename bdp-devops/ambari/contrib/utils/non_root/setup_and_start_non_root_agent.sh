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

# Run this on every ambari-agent node where you wish to have ambari-agent running as non-root

# The script functions:
# a) adds ambari user
# b) adds required sudo priviligies for it into /etc/sudoers
# c) Starts ambari-agent as ambari user

useradd ambari

echo 'Defaults    exempt_group = ambari' >> /etc/sudoers
echo 'Defaults    !env_reset,env_delete-=PATH' >> /etc/sudoers
echo 'Defaults: ambari !requiretty' >> /etc/sudoers

echo 'ambari        ALL=(ALL)       NOPASSWD:SETENV: /usr/bin/yum,/usr/bin/zypper,/usr/bin/apt-get, /bin/mkdir, /usr/bin/test, /bin/ln, /bin/chown, /bin/chmod, /bin/chgrp, /usr/sbin/groupadd, /usr/sbin/groupmod, /usr/sbin/useradd, /usr/sbin/usermod, /bin/cp, /usr/sbin/setenforce, /usr/bin/test, /usr/bin/stat, /bin/mv, /bin/sed, /bin/rm, /bin/kill, /bin/readlink, /usr/bin/pgrep, /bin/cat, /usr/bin/unzip, /bin/tar, /usr/bin/tee, /usr/bin/hdp-select, /usr/bin/conf-select, /usr/hdp/current/hadoop-client/sbin/hadoop-daemon.sh, /usr/lib/hadoop/bin/hadoop-daemon.sh, /usr/lib/hadoop/sbin/hadoop-daemon.sh, /usr/hdp/current/ranger-admin/setup.sh, /usr/hdp/current/ranger-usersync/setup.sh, /usr/bin/ranger-usersync-stop, /usr/bin/ranger-usersync-start, /sbin/chkconfig gmond off, /sbin/chkconfig gmetad off, /etc/init.d/httpd *, /sbin/service hdp-gmetad start, /sbin/service hdp-gmond start, /usr/hdp/current/ranger-admin/ranger_credential_helper.py, /usr/hdp/current/ranger-kms/ranger_credential_helper.py, /usr/sbin/gmond, /usr/sbin/update-rc.d ganglia-monitor *, /usr/sbin/update-rc.d gmetad *, /etc/init.d/apache2 *, /usr/sbin/service hdp-gmond *, /usr/sbin/service hdp-gmetad *, /usr/sbin/service mysql *, /sbin/service mysqld *, /sbin/service mysql *, /bin/su hdfs *, /bin/su ambari-qa *, /bin/su ranger *, /bin/su zookeeper *, /bin/su knox *,/bin/su falcon *,/bin/su ams *, /bin/su flume *,/bin/su hbase *,/bin/su spark *,/bin/su accumulo *,/bin/su hive *, /bin/su hcat *,/bin/su kafka *,/bin/su mapred *,/bin/su oozie *,/bin/su sqoop *,/bin/su storm *,/bin/su tez *,/bin/su atlas *,/bin/su yarn *,/bin/su kms *,/bin/su mysql *' >> /etc/sudoers

sed -i.bak 's/run_as_user\s*=\s*.*$/run_as_user=ambari/g' '/etc/ambari-agent/conf/ambari-agent.ini'
su - ambari -c '/usr/sbin/ambari-agent restart'
