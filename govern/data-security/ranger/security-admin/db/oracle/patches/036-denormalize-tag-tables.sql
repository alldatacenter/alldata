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
        select count(*) into v_count from user_tab_cols where table_name='X_TAG_DEF' and column_name='TAG_ATTRS_DEF_TEXT';
        if (v_count = 0) then 
                execute immediate 'ALTER TABLE X_TAG_DEF ADD TAG_ATTRS_DEF_TEXT CLOB DEFAULT NULL NULL';
        end if; 
        select count(*) into v_count from user_tab_cols where table_name='X_TAG' and column_name='TAG_ATTRS_TEXT';
        if (v_count = 0) then 
                execute immediate 'ALTER TABLE X_TAG ADD TAG_ATTRS_TEXT CLOB DEFAULT NULL NULL';
        end if; 
        select count(*) into v_count from user_tab_cols where table_name='X_SERVICE_RESOURCE' and column_name='SERVICE_RESOURCE_ELEMENTS_TEXT';
        if (v_count = 0) then 
                execute immediate 'ALTER TABLE X_SERVICE_RESOURCE ADD SERVICE_RESOURCE_ELEMENTS_TEXT CLOB DEFAULT NULL NULL';
        end if; 
        select count(*) into v_count from user_tab_cols where table_name='X_SERVICE_RESOURCE' and column_name='TAGS_TEXT';
        if (v_count = 0) then 
                execute immediate 'ALTER TABLE X_SERVICE_RESOURCE ADD TAGS_TEXT CLOB DEFAULT NULL NULL';
        end if; 
        commit; 
END;/

CREATE OR REPLACE PROCEDURE removeConstraints(ObjName IN varchar2) IS
BEGIN
FOR rec IN(
select owner, constraint_name
from all_constraints
where owner = sys_context('userenv','current_schema')
and table_name = ObjName
and constraint_type = 'R')
LOOP
execute immediate 'ALTER TABLE ' || rec.owner || '.' || ObjName || ' DROP CONSTRAINT ' || rec.constraint_name;
END LOOP;
END;/
/

CALL removeConstraints('X_TAG_ATTR_DEF');
CALL removeConstraints('X_TAG_ATTR');
CALL removeConstraints('X_SERVICE_RESOURCE_ELEMENT');
CALL removeConstraints('X_SERVICE_RESOURCE_ELEMENT_VAL');
