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
    v_column_exists number:=0;
    v_count number:=0;
BEGIN
    select count(*) into v_column_exists from user_tab_cols where lower(table_name)='x_portal_user' and lower(column_name) in('notes', 'other_attributes', 'sync_source') and lower(DATA_TYPE) = 'varchar2';
    IF (v_column_exists = 3) THEN
        select count(*) into v_count from user_tab_cols where lower(table_name)='x_portal_user' and lower(column_name) in('notes_copy', 'other_attributes_copy', 'sync_source_copy');
        IF (v_count = 0) THEN
            execute immediate 'ALTER TABLE x_portal_user ADD(notes_copy CLOB DEFAULT NULL NULL, other_attributes_copy CLOB DEFAULT NULL NULL, sync_source_copy CLOB DEFAULT NULL NULL)';
            commit;
            execute immediate 'UPDATE x_portal_user SET notes_copy = notes, other_attributes_copy = other_attributes, sync_source_copy = sync_source';
            commit;
            execute immediate 'ALTER TABLE x_portal_user DROP(notes, other_attributes, sync_source)';
            commit;
            execute immediate 'ALTER TABLE x_portal_user RENAME COLUMN notes_copy TO notes';
            execute immediate 'ALTER TABLE x_portal_user RENAME COLUMN other_attributes_copy TO other_attributes';
            execute immediate 'ALTER TABLE x_portal_user RENAME COLUMN sync_source_copy TO sync_source';
            commit;
        END IF;
    END IF;

    v_column_exists:=0;
    v_count:=0;

    select count(*) into v_column_exists from user_tab_cols where lower(table_name)='x_user' and lower(column_name) in('descr', 'other_attributes', 'sync_source') and lower(DATA_TYPE) = 'varchar2';
    IF (v_column_exists = 3) THEN
        select count(*) into v_count from user_tab_cols where lower(table_name)='x_user' and lower(column_name) in('descr_copy', 'other_attributes_copy', 'sync_source_copy');
        IF (v_count = 0) THEN
            execute immediate 'ALTER TABLE x_user ADD(descr_copy CLOB DEFAULT NULL NULL, other_attributes_copy CLOB DEFAULT NULL NULL, sync_source_copy CLOB DEFAULT NULL NULL)';
            commit;
            execute immediate 'UPDATE x_user SET descr_copy = descr, other_attributes_copy = other_attributes, sync_source_copy = sync_source';
            commit;
            execute immediate 'ALTER TABLE x_user DROP(descr, other_attributes, sync_source)';
            commit;
            execute immediate 'ALTER TABLE x_user RENAME COLUMN descr_copy TO descr';
            execute immediate 'ALTER TABLE x_user RENAME COLUMN other_attributes_copy TO other_attributes';
            execute immediate 'ALTER TABLE x_user RENAME COLUMN sync_source_copy TO sync_source';
            commit;
        END IF;
    END IF;

    v_column_exists:=0;
    v_count:=0;

    select count(*) into v_column_exists from user_tab_cols where lower(table_name)='x_group' and lower(column_name) in('descr', 'other_attributes', 'sync_source') and lower(DATA_TYPE) = 'varchar2';
    IF (v_column_exists = 3) THEN
        select count(*) into v_count from user_tab_cols where lower(table_name)='x_group' and lower(column_name) in('descr_copy', 'other_attributes_copy', 'sync_source_copy');
        IF (v_count = 0) THEN
            execute immediate 'ALTER TABLE x_group ADD(descr_copy CLOB DEFAULT NULL NULL, other_attributes_copy CLOB DEFAULT NULL NULL, sync_source_copy CLOB DEFAULT NULL NULL)';
            commit;
            execute immediate 'UPDATE x_group SET descr_copy = descr, other_attributes_copy = other_attributes, sync_source_copy = sync_source';
            commit;
            execute immediate 'ALTER TABLE x_group DROP(descr, other_attributes, sync_source)';
            commit;
            execute immediate 'ALTER TABLE x_group RENAME COLUMN descr_copy TO descr';
            execute immediate 'ALTER TABLE x_group RENAME COLUMN other_attributes_copy TO other_attributes';
            execute immediate 'ALTER TABLE x_group RENAME COLUMN sync_source_copy TO sync_source';
            commit;
        END IF;
    END IF;
    commit;
END;/
