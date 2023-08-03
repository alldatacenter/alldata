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
	sql_stmt VARCHAR2(1000);
BEGIN
	select count(*) into v_count from user_tab_cols where table_name='X_POLICY' and column_name IN('SERVICE','RESOURCE_SIGNATURE');
	if (v_count = 2) then
		v_count:=0;
		select count(*) into v_count from user_constraints where table_name='X_POLICY' and constraint_name='X_POLICY_UK_SERVICE_SIGNATURE' and constraint_type='U';
		if (v_count = 0) then
			v_count:=0;
			select count(*) into v_count from user_ind_columns WHERE table_name='X_POLICY' and column_name IN('SERVICE','RESOURCE_SIGNATURE') and index_name='X_POLICY_UK_SERVICE_SIGNATURE';
			if (v_count = 0) THEN
				EXECUTE immediate 'ALTER TABLE X_POLICY ADD CONSTRAINT X_POLICY_UK_SERVICE_SIGNATURE UNIQUE (SERVICE,RESOURCE_SIGNATURE)';
			end if;
			commit;
		end if;
	end if;
end;/
