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

DECLARE
	v_count number:=0;
BEGIN
	select count(*) into v_count from user_ind_columns where table_name='X_POLICY_CHANGE_LOG' and index_name='XPLCYCHNGLOG_UK_SRVCID_PLCYVER';
	if (v_count = 0) THEN
		execute immediate 'TRUNCATE TABLE X_POLICY_CHANGE_LOG';
		execute immediate 'CREATE UNIQUE INDEX XPLCYCHNGLOG_UK_SRVCID_PLCYVER ON X_POLICY_CHANGE_LOG(service_id, policy_version)';
		commit;
	end if;

	select count(*) into v_count from user_ind_columns where table_name='X_TAG_CHANGE_LOG' and index_name='XTAGCHNGLOG_UK_SRVCID_TAGVER';
	if (v_count = 0) THEN
		execute immediate 'TRUNCATE TABLE X_TAG_CHANGE_LOG';
		execute immediate 'CREATE UNIQUE INDEX XTAGCHNGLOG_UK_SRVCID_TAGVER ON X_TAG_CHANGE_LOG(service_id, service_tags_version)';
		commit;
	end if;
END;/
