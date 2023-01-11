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
	v_column_exists number := 0;
BEGIN
  Select count(*) into v_column_exists
    from user_tab_cols
    where column_name = upper('res_topologies')
      and table_name = upper('x_resource');

  if (v_column_exists = 0) then
      execute immediate 'ALTER TABLE  x_resource ADD  res_topologies CLOB DEFAULT NULL NULL';
      commit;
  end if;
  v_column_exists:=0;
  Select count(*) into v_column_exists
    from user_tab_cols
    where column_name = upper('res_services')
      and table_name = upper('x_resource');

  if (v_column_exists = 0) then
      execute immediate 'ALTER TABLE  x_resource ADD  res_services CLOB DEFAULT NULL NULL';
      commit;
  end if;
  v_column_exists:=0;
Select count(*) into v_column_exists
    from user_tab_cols
    where column_name = upper('ip_address')
      and table_name = upper('x_perm_map');

  if (v_column_exists = 0) then
      execute immediate 'ALTER TABLE  x_perm_map ADD  ip_address CLOB DEFAULT NULL NULL';
      commit;
  end if;
end;/