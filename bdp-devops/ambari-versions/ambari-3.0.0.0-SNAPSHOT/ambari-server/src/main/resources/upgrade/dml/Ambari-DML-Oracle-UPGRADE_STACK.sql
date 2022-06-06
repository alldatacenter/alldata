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

-- Update stack_name and stack_version
-- &1 - stack_name
-- &2 - stack_version

UPDATE ambari.clusters SET desired_stack_version = '{"stackName":"&1","stackVersion":"&2"}';
UPDATE ambari.clusterstate SET current_stack_version = '{"stackName":"&1","stackVersion":"&2"}';
UPDATE ambari.hostcomponentdesiredstate SET desired_stack_version = '{"stackName":"&1","stackVersion":"&2"}';
UPDATE ambari.hostcomponentstate SET current_stack_version = '{"stackName":"&1","stackVersion":"&2"}';
UPDATE ambari.servicecomponentdesiredstate SET desired_stack_version = '{"stackName":"&1","stackVersion":"&2"}';
UPDATE ambari.servicedesiredstate SET desired_stack_version = '{"stackName":"&1","stackVersion":"&2"}';
UPDATE ambari.hostcomponentstate SET current_state = 'INSTALLED' where current_state = 'UPGRADING';

commit;
