--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--
\connect :dbname;

INSERT INTO ambari.serviceconfigmapping (cluster_id, service_name, config_type, config_tag, timestamp)
    SELECT cluster_id, 'MAPREDUCE', type_name, version_tag, create_timestamp from ambari.clusterconfig 
        WHERE type_name = 'global' ORDER BY create_timestamp DESC LIMIT 1;

INSERT INTO ambari.serviceconfigmapping (cluster_id, service_name, config_type, config_tag, timestamp)
    SELECT cluster_id, 'MAPREDUCE', type_name, version_tag, create_timestamp from ambari.clusterconfig
         WHERE type_name = 'mapred-site' ORDER BY create_timestamp DESC LIMIT 1;
