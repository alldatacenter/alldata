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
-- sync_source_info CLOB NOT NULL,

DECLARE
        v_column_exists number:=0;
        v_count number:=0;
BEGIN
        select count(*) into v_column_exists from user_tab_cols
        where column_name = upper('sync_source_info') and table_name = upper('x_ugsync_audit_info');
        IF (v_column_exists = 1) THEN
                select count(*) into v_count from user_tab_cols
                where table_name = upper('x_ugsync_audit_info')
                        and column_name = upper('sync_source_info_copy')
                        and DATA_TYPE = upper('VARCHAR2');
                IF (v_count = 0) THEN
                        execute immediate 'ALTER TABLE x_ugsync_audit_info ADD sync_source_info_copy CLOB';
                        commit;
                        execute immediate 'UPDATE x_ugsync_audit_info SET sync_source_info_copy = sync_source_info';
                        commit;
                        execute immediate 'ALTER TABLE x_ugsync_audit_info MODIFY sync_source_info_copy NOT NULL';
                        commit;
                        execute immediate 'ALTER TABLE x_ugsync_audit_info DROP COLUMN sync_source_info';
                        commit;
                        execute immediate 'ALTER TABLE x_ugsync_audit_info RENAME COLUMN sync_source_info_copy TO sync_source_info';
                        commit;
            END IF;
        END IF;
END;/
