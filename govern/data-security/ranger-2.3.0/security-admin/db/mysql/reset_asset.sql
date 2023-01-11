-- Licensed to the Apache Software Foundation (ASF) under one or more
-- contributor license agreements.  See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.
-- The ASF licenses this file to You under the Apache License, Version 2.0
-- (the "License"); you may not use this file except in compliance with
-- the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

update x_asset set config = '{"username":"policymgr","password":"policymgr","fs.default.name":"hdfs://sandbox.hortonworks.com:8020","hadoop.security.authorization":"false","hadoop.security.authentication":"simple","hadoop.security.auth_to_local":"RULE:[2:$1@$0]([rn]m@.*)s/.*/yarn/         RULE:[2:$1@$0](jhs@.*)s/.*/mapred/         RULE:[2:$1@$0]([nd]n@.*)s/.*/hdfs/         RULE:[2:$1@$0](hm@.*)s/.*/hbase/         RULE:[2:$1@$0](rs@.*)s/.*/hbase/         DEFAULT","dfs.datanode.kerberos.principal":"","dfs.namenode.kerberos.principal":"","dfs.secondary.namenode.kerberos.principal":"","commonNameForCertificate":""}' where asset_name = 'hadoopdev';
update x_asset set config = '{"username":"policymgr","password":"policymgr","fs.default.name":"hdfs://sandbox.hortonworks.com:8020","hadoop.security.authorization":"false","hadoop.security.authentication":"simple","hadoop.security.auth_to_local":"RULE:[2:$1@$0]([rn]m@.*)s/.*/yarn/         RULE:[2:$1@$0](jhs@.*)s/.*/mapred/         RULE:[2:$1@$0]([nd]n@.*)s/.*/hdfs/         RULE:[2:$1@$0](hm@.*)s/.*/hbase/         RULE:[2:$1@$0](rs@.*)s/.*/hbase/         DEFAULT","dfs.datanode.kerberos.principal":"","dfs.namenode.kerberos.principal":"","dfs.secondary.namenode.kerberos.principal":"","hbase.master.kerberos.principal":"","hbase.rpc.engine":"org.apache.hadoop.hbase.ipc.SecureRpcEngine","hbase.rpc.protection":"privacy","hbase.security.authentication":"simple","hbase.zookeeper.property.clientPort":"2181","hbase.zookeeper.quorum":"sandbox.hortonworks.com","commonNameForCertificate":""}' where asset_name = 'hbase' ;
update x_asset set config = '{"username":"policymgr","password":"","jdbc.driverClassName":"org.apache.hive.jdbc.HiveDriver","jdbc.url":"jdbc:hive2://sandbox.hortonworks.com:10000/default","commonNameForCertificate":""}'  where asset_name = 'dev-hive' ;
commit ;
