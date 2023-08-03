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
        select count(*) into v_count from user_tab_cols where table_name='X_SERVICE_DEF' and column_name='DISPLAY_NAME';
        if (v_count = 0) then
                execute immediate 'ALTER TABLE x_service_def ADD display_name VARCHAR(1024) DEFAULT NULL NULL';
                execute immediate 'UPDATE x_service_def SET display_name=name';
                execute immediate 'UPDATE x_service_def SET display_name=:val where name=:searchVal' using 'Hadoop SQL', 'hive';
        end if;

        v_count:=0;
        select count(*) into v_count from user_tab_cols where table_name='X_SERVICE' and column_name='DISPLAY_NAME';
        if (v_count = 0) then
                execute immediate 'ALTER TABLE x_service ADD display_name VARCHAR(255) DEFAULT NULL NULL';
                execute immediate 'UPDATE x_service SET display_name=name';
        end if;
        commit;

        v_count:=0;
        select count(*) into v_count from user_tab_cols where table_name='X_PORTAL_USER' and column_name='OTHER_ATTRIBUTES';
        if (v_count = 0) then
                execute immediate 'ALTER TABLE x_portal_user ADD other_attributes CLOB DEFAULT NULL NULL';
        end if;

        v_count:=0;
        select count(*) into v_count from user_tab_cols where table_name='X_USER' and column_name='OTHER_ATTRIBUTES';
        if (v_count = 0) then
                execute immediate 'ALTER TABLE x_user ADD other_attributes CLOB DEFAULT NULL NULL';
        end if;

        v_count:=0;
        select count(*) into v_count from user_tab_cols where table_name='X_GROUP' and column_name='OTHER_ATTRIBUTES';
        if (v_count = 0) then
                execute immediate 'ALTER TABLE X_GROUP ADD other_attributes CLOB DEFAULT NULL NULL';
        end if;
        commit;
END;/
