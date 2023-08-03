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

CREATE OR REPLACE PROCEDURE sp_dropobject(ObjName IN varchar2,ObjType IN varchar2)
IS
v_counter integer;
BEGIN
if (ObjType = 'TABLE') then
    select count(*) into v_counter from user_tables where table_name = upper(ObjName);
    if (v_counter > 0) then
      execute immediate 'drop table ' || ObjName || ' cascade constraints';
    end if;
end if;
  if (ObjType = 'PROCEDURE') then
    select count(*) into v_counter from User_Objects where object_type = 'PROCEDURE' and OBJECT_NAME = upper(ObjName);
      if (v_counter > 0) then
        execute immediate 'DROP PROCEDURE ' || ObjName;
      end if;
  end if;
  if (ObjType = 'FUNCTION') then
    select count(*) into v_counter from User_Objects where object_type = 'FUNCTION' and OBJECT_NAME = upper(ObjName);
      if (v_counter > 0) then
        execute immediate 'DROP FUNCTION ' || ObjName;
      end if;
  end if;
  if (ObjType = 'TRIGGER') then
    select count(*) into v_counter from User_Triggers where TRIGGER_NAME = upper(ObjName);
      if (v_counter > 0) then
        execute immediate 'DROP TRIGGER ' || ObjName;
      end if;
  end if;
  if (ObjType = 'VIEW') then
    select count(*) into v_counter from User_Views where VIEW_NAME = upper(ObjName);
      if (v_counter > 0) then
        execute immediate 'DROP VIEW ' || ObjName;
      end if;
  end if;
  if (ObjType = 'SEQUENCE') then
    select count(*) into v_counter from user_sequences where sequence_name = upper(ObjName);
      if (v_counter > 0) then
        execute immediate 'DROP SEQUENCE ' || ObjName;
      end if;
  end if;
  if (ObjType = 'INDEX') then
    select count(*) into v_counter from user_indexes where index_name = upper(ObjName);
      if (v_counter > 0) then
        execute immediate 'DROP INDEX ' || ObjName;
      end if;
  end if;
END;
/
call sp_dropobject('XA_ACCESS_AUDIT','TABLE');
call sp_dropobject('XA_ACCESS_AUDIT_SEQ','SEQUENCE');
call sp_dropobject('xa_access_audit_added_by_id','INDEX');
call sp_dropobject('xa_access_audit_upd_by_id','INDEX');
call sp_dropobject('xa_access_audit_cr_time','INDEX');
call sp_dropobject('xa_access_audit_up_time','INDEX');
call sp_dropobject('xa_access_audit_event_time','INDEX');
commit;