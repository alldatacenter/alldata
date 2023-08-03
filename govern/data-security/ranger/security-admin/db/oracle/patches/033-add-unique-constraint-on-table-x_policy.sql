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
	duplicate VARCHAR2(11):='-duplicate-';
BEGIN
	select count(*) into v_count from user_tab_cols where table_name='X_POLICY' and column_name IN('NAME','SERVICE');
	if (v_count = 2) then
		v_count:=0;
		select count(*) into v_count from user_constraints where table_name='X_POLICY' and constraint_name='X_POLICY_UK_NAME_SERVICE' and constraint_type='U';
		if (v_count = 0) then
			v_count:=0;
			select count(*) into v_count from user_ind_columns WHERE table_name='X_POLICY' and column_name IN('NAME','SERVICE') and index_name='X_POLICY_UK_NAME_SERVICE';
			if (v_count = 0) THEN
				sql_stmt := 'UPDATE x_policy set name=concat(concat(name,:1),id) where id in (select id from (select id from x_policy where concat(service,name) in (select concat(service,name) from x_policy group by service,name having count(*) >1)))';
				EXECUTE IMMEDIATE sql_stmt USING duplicate;
				EXECUTE IMMEDIATE 'ALTER TABLE X_POLICY ADD CONSTRAINT x_policy_UK_name_service UNIQUE (NAME,SERVICE)';
			end if;
			commit;
		end if;
	end if;
end;/
