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


AMBARI_PASSHPHRASE="DEV"
export AMBARI_JVM_ARGS="$AMBARI_JVM_ARGS -Xms512m -Xmx2048m -XX:MaxPermSize=128m -Djava.security.auth.login.config=$ROOT/etc/ambari-server/conf/krb5JAASLogin.conf -Djava.security.krb5.conf=/etc/krb5.conf -Djavax.security.auth.useSubjectCredsOnly=false -Dcom.sun.jndi.ldap.connect.pool.protocol=\"plain ssl\" -Dcom.sun.jndi.ldap.connect.pool.maxsize=20 -Dcom.sun.jndi.ldap.connect.pool.timeout=300000"
export PATH=$PATH:$ROOT/var/lib/ambari-server
export PYTHONPATH=$ROOT/usr/lib/ambari-server/lib:$PYTHONPATH

# customize python binary for ambari
# export PYTHON=/usr/bin/python2

# to add additional directory or jar to server classpath use SERVER_CLASSPATH variable
# export SERVER_CLASSPATH=/etc/hadoop/conf/secure
