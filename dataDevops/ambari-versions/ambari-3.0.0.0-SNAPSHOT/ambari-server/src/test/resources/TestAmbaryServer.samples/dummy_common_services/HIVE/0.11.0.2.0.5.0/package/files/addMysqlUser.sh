#!/bin/sh
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

mysqldservice=$1
mysqldbuser=$2
mysqldbpasswd=$3
mysqldbhost=$4
myhostname=$(hostname -f)

service $mysqldservice start
echo "Adding user $mysqldbuser@$mysqldbhost and $mysqldbuser@localhost"
mysql -u root -e "CREATE USER '$mysqldbuser'@'$mysqldbhost' IDENTIFIED BY '$mysqldbpasswd';"
mysql -u root -e "CREATE USER '$mysqldbuser'@'localhost' IDENTIFIED BY '$mysqldbpasswd';"
mysql -u root -e "GRANT ALL PRIVILEGES ON *.* TO '$mysqldbuser'@'$mysqldbhost';"
mysql -u root -e "GRANT ALL PRIVILEGES ON *.* TO '$mysqldbuser'@'localhost';"
if [ '$(mysql -u root -e "select user from mysql.user where user='$mysqldbuser' and host='$myhostname'" | grep "$mysqldbuser")' != '0' ]; then
  echo "Adding user $mysqldbuser@$myhostname";
  mysql -u root -e "CREATE USER '$mysqldbuser'@'$myhostname' IDENTIFIED BY '$mysqldbpasswd';";
  mysql -u root -e "GRANT ALL PRIVILEGES ON *.* TO '$mysqldbuser'@'$myhostname';";
fi
mysql -u root -e "flush privileges;"
service $mysqldservice stop
