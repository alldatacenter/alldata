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

CREATE OR REPLACE PROCEDURE spmodifysortordercolumn(TableName IN varchar2)
IS
	v_column_exists number := 0;
BEGIN
  Select count(*) into v_column_exists
    from user_tab_cols
    where (column_name = upper('sort_order'))
      and table_name = upper(TableName) and DATA_TYPE='NUMBER' and DATA_PRECISION=3;

  if (v_column_exists > 0) then
    execute immediate 'ALTER TABLE ' || TableName || ' MODIFY(sort_order NUMBER(10) DEFAULT 0)';
    commit;
  end if;
END;/
/

call spmodifysortordercolumn('x_service_config_def');
call spmodifysortordercolumn('x_resource_def');
call spmodifysortordercolumn('x_access_type_def');
call spmodifysortordercolumn('x_policy_condition_def');
call spmodifysortordercolumn('x_context_enricher_def');
call spmodifysortordercolumn('x_enum_element_def');
call spmodifysortordercolumn('x_policy_resource_map');
call spmodifysortordercolumn('x_policy_item');
call spmodifysortordercolumn('x_policy_item_access');
call spmodifysortordercolumn('x_policy_item_condition');
call spmodifysortordercolumn('x_policy_item_user_perm');
call spmodifysortordercolumn('x_policy_item_group_perm');
call spmodifysortordercolumn('x_datamask_type_def');
