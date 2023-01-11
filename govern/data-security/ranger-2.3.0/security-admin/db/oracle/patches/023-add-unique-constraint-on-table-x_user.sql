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
  	select count(*) into v_count from user_tab_cols where table_name='X_USER' and column_name='USER_NAME';
  	if (v_count = 1) then
  		v_count:=0;
  		select count(*) into v_count from user_constraints where table_name='X_USER' and constraint_name='X_USER_UK_USER_NAME' and constraint_type='U';
		if (v_count = 0) then
			v_count:=0;
			select count(*) into v_count from user_ind_columns WHERE table_name='X_USER' and column_name='USER_NAME' and index_name='X_USER_UK_USER_NAME';
			if (v_count = 0) then
				execute immediate 'ALTER TABLE x_user MODIFY(user_name VARCHAR(767)) ADD CONSTRAINT x_user_UK_user_name UNIQUE (user_name)';
			end if;
			commit;
		end if;
	end if;
end;/
