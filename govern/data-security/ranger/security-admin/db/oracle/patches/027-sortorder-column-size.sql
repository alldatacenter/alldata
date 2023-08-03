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
	v_column_exists1 number:=0;
BEGIN
  Select count(*) into v_column_exists1
    from user_tab_cols
    where (column_name = upper('sort_order'))
      and table_name = upper('x_policy_resource_map') and DATA_TYPE='NUMBER' and DATA_PRECISION=3;

  if (v_column_exists1 = 1) then
      execute immediate 'ALTER TABLE x_policy_resource_map MODIFY(sort_order NUMBER(10) DEFAULT 0)';
      commit;
  end if;
end;/