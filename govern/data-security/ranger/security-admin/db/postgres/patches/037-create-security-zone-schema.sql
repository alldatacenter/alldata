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

CREATE OR REPLACE FUNCTION getXportalUIdByLoginId(input_val varchar(100))
RETURNS bigint LANGUAGE SQL AS $$ SELECT x_portal_user.id FROM x_portal_user
WHERE x_portal_user.login_id = $1; $$;

CREATE OR REPLACE FUNCTION getModulesIdByName(input_val varchar(100))
RETURNS bigint LANGUAGE SQL AS $$ SELECT x_modules_master.id FROM x_modules_master
WHERE x_modules_master.module = $1; $$;

select 'delimiter start';
CREATE OR REPLACE FUNCTION add_security_zone_permissions()
RETURNS void AS $$
DECLARE
 v_column_exists integer := 0;
BEGIN
 select count(*) into v_column_exists from x_modules_master where module='Security Zone';
 IF v_column_exists = 0 THEN
 	INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),'Security Zone','');
 END IF;

 v_column_exists:=0;
 select count(*) into v_column_exists from x_user_module_perm where user_id=getXportalUIdByLoginId('admin') and module_id=getModulesIdByName('Security Zone');
 IF v_column_exists = 0 THEN
 	INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (getXportalUIdByLoginId('admin'),getModulesIdByName('Security Zone'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
 END IF;

 v_column_exists:=0;
 select count(*) into v_column_exists from x_user_module_perm where user_id=getXportalUIdByLoginId('rangerusersync') and module_id=getModulesIdByName('Security Zone');
 IF v_column_exists = 0 THEN
 	INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (getXportalUIdByLoginId('rangerusersync'),getModulesIdByName('Security Zone'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
 END IF;

 v_column_exists:=0;
 select count(*) into v_column_exists from x_user_module_perm where user_id=getXportalUIdByLoginId('rangertagsync') and module_id=getModulesIdByName('Security Zone');
 IF v_column_exists = 0 THEN
 	INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (getXportalUIdByLoginId('rangertagsync'),getModulesIdByName('Security Zone'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
 END IF;
END;
$$ LANGUAGE plpgsql;
select 'delimiter end';

select add_security_zone_permissions();
select 'delimiter end';

commit;

select 'delimiter start';
CREATE OR REPLACE FUNCTION add_zone_x_policy_export_audit()
RETURNS void AS $$
DECLARE
 v_column_exists integer := 0;
BEGIN
 select count(*) into v_column_exists from pg_attribute where attrelid in(select oid from pg_class where relname='x_policy_export_audit') and attname='zone_name';
 IF v_column_exists = 0 THEN
  ALTER TABLE x_policy_export_audit ADD COLUMN zone_name VARCHAR(255) DEFAULT NULL NULL;
 END IF;
END;
$$ LANGUAGE plpgsql;
select 'delimiter end';

select add_zone_x_policy_export_audit();
select 'delimiter end';

select 'delimiter start';
CREATE OR REPLACE FUNCTION remove_x_policy_zone_id()
RETURNS void AS $$
DECLARE
 v_column_exists integer := 0;
BEGIN
  select count(*) into v_column_exists from pg_attribute where attrelid in(select oid from pg_class where relname='x_policy') and attname='zone_id';
   IF v_column_exists > 0 THEN
   		ALTER TABLE x_policy DROP CONSTRAINT x_policy_FK_zone_id;
     	ALTER TABLE x_policy DROP COLUMN zone_id ;
   END IF;
END;
$$ LANGUAGE plpgsql;
select 'delimiter end';

select remove_x_policy_zone_id();
select 'delimiter end';

DROP TABLE IF EXISTS x_security_zone_ref_group CASCADE;
DROP SEQUENCE IF EXISTS x_sec_zone_ref_group_seq;
DROP TABLE IF EXISTS x_security_zone_ref_user CASCADE;
DROP SEQUENCE IF EXISTS x_sec_zone_ref_user_seq;
DROP TABLE IF EXISTS x_security_zone_ref_resource CASCADE;
DROP SEQUENCE IF EXISTS x_sec_zone_ref_resource_seq;
DROP TABLE IF EXISTS x_security_zone_ref_service CASCADE;
DROP SEQUENCE IF EXISTS x_sec_zone_ref_service_seq;
DROP TABLE IF EXISTS x_security_zone_ref_tag_srvc CASCADE;
DROP SEQUENCE IF EXISTS x_sec_zone_ref_tag_srvc_seq;
DROP TABLE IF EXISTS x_ranger_global_state CASCADE;
DROP SEQUENCE IF EXISTS x_ranger_global_state_seq;
DROP TABLE IF EXISTS x_security_zone CASCADE;
DROP SEQUENCE IF EXISTS x_security_zone_seq;

CREATE SEQUENCE x_security_zone_seq;
CREATE TABLE x_security_zone (
id BIGINT DEFAULT nextval('x_security_zone_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
version BIGINT DEFAULT NULL NULL,
name varchar(255) NOT NULL,
jsonData text DEFAULT NULL NULL,
description VARCHAR(1024) DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_security_zone_UK_name UNIQUE (name),
CONSTRAINT x_security_zone_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_security_zone_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);

CREATE SEQUENCE x_ranger_global_state_seq;
CREATE TABLE x_ranger_global_state (
id BIGINT DEFAULT nextval('x_ranger_global_state_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
version BIGINT DEFAULT NULL NULL,
state_name varchar(255) NOT NULL,
app_data varchar(255) DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_ranger_global_state_UK_state_name UNIQUE (state_name),
CONSTRAINT x_ranger_global_state_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_ranger_global_state_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);

CREATE SEQUENCE x_sec_zone_ref_service_seq;
CREATE TABLE x_security_zone_ref_service (
id BIGINT DEFAULT nextval('x_sec_zone_ref_service_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
zone_id BIGINT DEFAULT NULL NULL,
service_id BIGINT DEFAULT NULL NULL,
service_name varchar(255) NULL DEFAULT NULL::character varying,
primary key (id),
CONSTRAINT x_sz_ref_service_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_ref_service_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_ref_service_FK_zone_id FOREIGN KEY (zone_id) REFERENCES x_security_zone (id),
CONSTRAINT x_sz_ref_service_FK_service_id FOREIGN KEY (service_id) REFERENCES x_service (id),
CONSTRAINT x_sz_ref_service_FK_service_name FOREIGN KEY (service_name) REFERENCES x_service (name)
);

CREATE SEQUENCE x_sec_zone_ref_tag_srvc_seq;
CREATE TABLE x_security_zone_ref_tag_srvc (
id BIGINT DEFAULT nextval('x_sec_zone_ref_tag_srvc_SEQ'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
zone_id BIGINT DEFAULT NULL NULL,
tag_srvc_id BIGINT DEFAULT NULL NULL,
tag_srvc_name varchar(255) NULL DEFAULT NULL::character varying,
primary key (id),
CONSTRAINT x_sz_refTagSrvc_FK_aded_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_refTagSrvc_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_refTagSrvc_FK_zone_id FOREIGN KEY (zone_id) REFERENCES x_security_zone (id),
CONSTRAINT x_sz_refTagSrvc_FK_tag_srvc_id FOREIGN KEY (tag_srvc_id) REFERENCES x_service (id),
CONSTRAINT x_sz_refTagSrvc_FK_tag_srvc_name FOREIGN KEY (tag_srvc_name) REFERENCES x_service (name)
);

CREATE SEQUENCE x_sec_zone_ref_resource_seq;
CREATE TABLE x_security_zone_ref_resource (
id BIGINT DEFAULT nextval('x_sec_zone_ref_resource_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
zone_id BIGINT DEFAULT NULL NULL,
resource_def_id BIGINT DEFAULT NULL NULL,
resource_name varchar(255) NULL DEFAULT NULL::character varying,
primary key (id),
CONSTRAINT x_sz_ref_res_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_ref_res_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_ref_service_FK_zone_id FOREIGN KEY (zone_id) REFERENCES x_security_zone (id),
CONSTRAINT x_sz_ref_res_FK_resource_def_id FOREIGN KEY (resource_def_id) REFERENCES x_resource_def (id)
);

CREATE SEQUENCE x_sec_zone_ref_user_seq;
CREATE TABLE x_security_zone_ref_user (
id BIGINT DEFAULT nextval('x_sec_zone_ref_user_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
zone_id BIGINT DEFAULT NULL NULL,
user_id BIGINT DEFAULT NULL NULL,
user_name varchar(255) NULL DEFAULT NULL::character varying,
user_type SMALLINT DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_sz_ref_user_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_ref_user_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_ref_user_FK_zone_id FOREIGN KEY (zone_id) REFERENCES x_security_zone (id),
CONSTRAINT x_sz_ref_user_FK_user_id FOREIGN KEY (user_id) REFERENCES x_user (id),
CONSTRAINT x_sz_ref_user_FK_user_name FOREIGN KEY (user_name) REFERENCES x_user (user_name)
);

CREATE SEQUENCE x_sec_zone_ref_group_seq;
CREATE TABLE x_security_zone_ref_group (
id BIGINT DEFAULT nextval('x_sec_zone_ref_group_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
zone_id BIGINT DEFAULT NULL NULL,
group_id BIGINT DEFAULT NULL NULL,
group_name varchar(255) NULL DEFAULT NULL::character varying,
group_type SMALLINT DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_sz_ref_group_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_ref_group_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_ref_group_FK_zone_id FOREIGN KEY (zone_id) REFERENCES x_security_zone (id),
CONSTRAINT x_sz_ref_group_FK_group_id FOREIGN KEY (group_id) REFERENCES x_group (id)
);

select 'delimiter start';
CREATE OR REPLACE FUNCTION add_unzone_entry()
RETURNS void AS $$
DECLARE
 v_column_exists integer := 0;
BEGIN
  select count(*) into v_column_exists from x_security_zone where id=1 and name=' ';
   IF v_column_exists = 0 THEN
   		INSERT INTO x_security_zone(create_time, update_time, added_by_id, upd_by_id, version, name, jsonData, description) VALUES (current_timestamp, current_timestamp, getXportalUIdByLoginId('admin'), getXportalUIdByLoginId('admin'), 1, ' ', '', 'Unzoned zone');
   	END IF;
END;
$$ LANGUAGE plpgsql;
select 'delimiter end';

select add_unzone_entry();
select 'delimiter end';

select 'delimiter start';
CREATE OR REPLACE FUNCTION add_x_policy_zone_id()
RETURNS void AS $$
DECLARE
 v_column_exists integer := 0;
BEGIN
  select count(*) into v_column_exists from pg_attribute where attrelid in(select oid from pg_class where relname='x_policy') and attname='zone_id';
   IF v_column_exists = 0 THEN
     ALTER TABLE x_policy ADD COLUMN zone_id BIGINT DEFAULT 1 NOT NULL,ADD CONSTRAINT x_policy_FK_zone_id FOREIGN KEY(zone_id) REFERENCES x_security_zone(id);
   END IF;
END;
$$ LANGUAGE plpgsql;
select 'delimiter end';

select add_x_policy_zone_id();
select 'delimiter end';

