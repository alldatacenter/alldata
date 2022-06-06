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
# limitations under the License

ROOT=`echo "${RPM_INSTALL_PREFIX}" | sed 's|/$||g'`

AMBARI_SERVER_EXECUTABLE_LINK="${ROOT}/usr/sbin/ambari-server"
AMBARI_SERVER_EXECUTABLE="${ROOT}/etc/init.d/ambari-server"


# needed for upgrade though ambari-2.2.2
rm -f "$AMBARI_SERVER_EXECUTABLE_LINK"
ln -s "$AMBARI_SERVER_EXECUTABLE" "$AMBARI_SERVER_EXECUTABLE_LINK"

exit 0