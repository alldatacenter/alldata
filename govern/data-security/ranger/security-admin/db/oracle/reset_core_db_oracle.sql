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

-- create or replace procedure
commit;
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
  if (ObjType = 'CONSTRAINT') then
    select count(*) into v_counter from user_constraints where constraint_name = upper(ObjName);
      if (v_counter > 0) then
        execute immediate 'DROP CONSTRAINT ' || ObjName;
      end if;
  end if;
END;
/
-- sequence
call sp_dropobject('SEQ_GEN_IDENTITY','SEQUENCE');
call sp_dropobject('X_ACCESS_AUDIT_SEQ','SEQUENCE');
call sp_dropobject('X_ASSET_SEQ','SEQUENCE');
call sp_dropobject('X_AUDIT_MAP_SEQ','SEQUENCE');
call sp_dropobject('X_AUTH_SESS_SEQ','SEQUENCE');
call sp_dropobject('X_CRED_STORE_SEQ','SEQUENCE');
call sp_dropobject('X_DB_BASE_SEQ','SEQUENCE');
call sp_dropobject('X_GROUP_SEQ','SEQUENCE');
call sp_dropobject('X_GROUP_GROUPS_SEQ','SEQUENCE');
call sp_dropobject('X_GROUP_USERS_SEQ','SEQUENCE');
call sp_dropobject('X_PERM_MAP_SEQ','SEQUENCE');
call sp_dropobject('X_POLICY_EXPORT_SEQ','SEQUENCE');
call sp_dropobject('X_PORTAL_USER_SEQ','SEQUENCE');
call sp_dropobject('X_PORTAL_USER_ROLE_SEQ','SEQUENCE');
call sp_dropobject('X_RESOURCE_SEQ','SEQUENCE');
call sp_dropobject('X_TRX_LOG_SEQ','SEQUENCE');
call sp_dropobject('X_USER_SEQ','SEQUENCE');
call sp_dropobject('X_DB_VERSION_H_SEQ','SEQUENCE');
call sp_dropobject('V_TRX_LOG_SEQ','SEQUENCE');
call sp_dropobject('XA_ACCESS_AUDIT_SEQ','SEQUENCE');
commit;

-- drop table
call sp_dropobject('vx_trx_log','VIEW');
call sp_dropobject('x_perm_map','TABLE');
call sp_dropobject('x_audit_map','TABLE');
call sp_dropobject('x_trx_log','TABLE');
call sp_dropobject('x_resource','TABLE');
call sp_dropobject('x_policy_export_audit','TABLE');
call sp_dropobject('x_group_users','TABLE');
call sp_dropobject('x_user','TABLE');
call sp_dropobject('x_group_groups','TABLE');
call sp_dropobject('X_GROUP','TABLE');
call sp_dropobject('x_db_base','TABLE');
call sp_dropobject('x_cred_store','TABLE');
call sp_dropobject('x_auth_sess','TABLE');
call sp_dropobject('x_asset','TABLE');
call sp_dropobject('xa_access_audit','TABLE');
call sp_dropobject('x_portal_user_role','TABLE');
call sp_dropobject('x_portal_user','TABLE');

commit;