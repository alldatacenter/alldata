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
select 'delimiter start';

CREATE OR REPLACE FUNCTION truncate_rms_tables()
RETURNS void AS $$
BEGIN
    truncate table x_rms_mapping_provider;
    truncate table x_rms_resource_mapping;
    truncate table x_rms_notification;
    truncate table x_rms_service_resource CASCADE;
END;
$$ LANGUAGE plpgsql;
select 'delimiter end';

select truncate_rms_tables();
select 'delimiter end';

select 'delimiter start';
CREATE OR REPLACE FUNCTION create_unique_index_for_x_rms_service_resource()
RETURNS void AS $$
DECLARE
    v_attnum1 integer := 0;
BEGIN
    select attnum into v_attnum1 from pg_attribute where attrelid in(select oid from pg_class where relname='x_rms_service_resource') and attname in('resource_signature');
    IF v_attnum1 > 0 THEN
        IF exists (select * from pg_index where indrelid in(select oid from pg_class where relname='x_rms_service_resource') and indkey[0]=v_attnum1) THEN
            DROP INDEX IF EXISTS x_rms_service_resource_IDX_resource_signature;
        END IF;
    END IF;
    select attnum into v_attnum1 from pg_attribute where attrelid in(select oid from pg_class where relname='x_rms_service_resource') and attname in('resource_signature');
    IF v_attnum1 > 0 THEN
        IF not exists (select * from pg_constraint where conrelid in(select oid from pg_class where relname='x_rms_service_resource') and conname='x_rms_service_resource_UK_resource_signature' and contype='u') THEN
            IF not exists (select * from pg_index where indrelid in(select oid from pg_class where relname='x_rms_service_resource') and indkey[0]=v_attnum1) THEN
                ALTER TABLE x_rms_service_resource ADD CONSTRAINT x_rms_service_resource_UK_resource_signature UNIQUE(resource_signature);
            END IF;
        END IF;
    END IF;
END;
$$ LANGUAGE plpgsql;
select 'delimiter end';

select create_unique_index_for_x_rms_service_resource();
select 'delimiter end';


