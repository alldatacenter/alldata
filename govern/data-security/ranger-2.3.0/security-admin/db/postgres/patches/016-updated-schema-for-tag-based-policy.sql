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
DROP TABLE IF EXISTS x_service_resource_element_val CASCADE;
DROP SEQUENCE IF EXISTS x_service_res_el_val_seq;
DROP TABLE IF EXISTS x_tag_resource_map CASCADE;
DROP SEQUENCE IF EXISTS x_tag_resource_map_seq;
DROP TABLE IF EXISTS x_tag_attr CASCADE;
DROP SEQUENCE IF EXISTS x_tag_attr_seq;
DROP TABLE IF EXISTS x_tag_attr_def CASCADE;
DROP SEQUENCE IF EXISTS x_tag_attr_def_seq;
DROP TABLE IF EXISTS x_service_resource_element CASCADE;
DROP SEQUENCE IF EXISTS x_service_resource_element_seq;
DROP TABLE IF EXISTS x_service_resource CASCADE;
DROP SEQUENCE IF EXISTS x_service_resource_seq;
DROP TABLE IF EXISTS x_tag CASCADE;
DROP SEQUENCE IF EXISTS x_tag_seq;
DROP TABLE IF EXISTS x_tag_def CASCADE;
DROP SEQUENCE IF EXISTS x_tag_def_seq;
commit;
CREATE SEQUENCE x_tag_def_seq;
CREATE TABLE x_tag_def (
id BIGINT DEFAULT nextval('x_tag_def_seq'::regclass),
guid VARCHAR(64) NOT NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
version BIGINT DEFAULT NULL NULL,
name VARCHAR(255) NOT NULL,
source VARCHAR(128) DEFAULT NULL NULL,
is_enabled BOOLEAN DEFAULT '0' NOT NULL,
PRIMARY KEY (id),
CONSTRAINT x_tag_def_UK_guid UNIQUE (guid),
CONSTRAINT x_tag_def_UK_name UNIQUE (name),
CONSTRAINT x_tag_def_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_tag_def_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
CREATE INDEX x_tag_def_IDX_added_by_id ON x_tag_def(added_by_id);
CREATE INDEX x_tag_def_IDX_upd_by_id ON x_tag_def(upd_by_id);
commit;

CREATE SEQUENCE x_tag_seq;
CREATE TABLE x_tag(
id BIGINT DEFAULT nextval('x_tag_seq'::regclass),
guid VARCHAR(64) NOT NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
version BIGINT DEFAULT NULL NULL,
type BIGINT NOT NULL,
primary key (id),
CONSTRAINT x_tag_UK_guid UNIQUE (guid),
CONSTRAINT x_tag_FK_type FOREIGN KEY (type) REFERENCES x_tag_def (id),
CONSTRAINT x_tag_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_tag_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
CREATE INDEX x_tag_IDX_type ON x_tag(type);
CREATE INDEX x_tag_IDX_added_by_id ON x_tag(added_by_id);
CREATE INDEX x_tag_IDX_upd_by_id ON x_tag(upd_by_id);
commit;

CREATE SEQUENCE x_service_resource_seq;
CREATE TABLE x_service_resource(
id BIGINT DEFAULT nextval('x_service_resource_seq'::regclass),
guid VARCHAR(64) NOT NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
version BIGINT DEFAULT NULL NULL,
service_id BIGINT NOT NULL,
resource_signature VARCHAR(128) DEFAULT NULL NULL,
is_enabled BOOLEAN DEFAULT '1' NOT NULL,
primary key (id),
CONSTRAINT x_service_res_UK_guid UNIQUE (guid),
CONSTRAINT x_service_res_FK_service_id FOREIGN KEY (service_id) REFERENCES x_service (id),
CONSTRAINT x_service_res_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_service_res_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
CREATE INDEX x_service_res_IDX_added_by_id ON x_service_resource(added_by_id);
CREATE INDEX x_service_res_IDX_upd_by_id ON x_service_resource(upd_by_id);
commit;

CREATE SEQUENCE x_service_resource_element_seq;
CREATE TABLE x_service_resource_element(
id BIGINT DEFAULT nextval('x_service_resource_element_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
res_id BIGINT NOT NULL,
res_def_id BIGINT NOT NULL,
is_excludes BOOLEAN DEFAULT '0' NOT NULL,
is_recursive BOOLEAN DEFAULT '0' NOT NULL,
primary key (id),
CONSTRAINT x_srvc_res_el_FK_res_def_id FOREIGN KEY (res_def_id) REFERENCES x_resource_def (id),
CONSTRAINT x_srvc_res_el_FK_res_id FOREIGN KEY (res_id) REFERENCES x_service_resource (id),
CONSTRAINT x_srvc_res_el_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_srvc_res_el_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
CREATE INDEX x_srvc_res_el_IDX_added_by_id ON x_service_resource_element(added_by_id);
CREATE INDEX x_srvc_res_el_IDX_upd_by_id ON x_service_resource_element(upd_by_id);
commit;

CREATE SEQUENCE x_tag_attr_def_seq;
CREATE TABLE x_tag_attr_def(
id BIGINT DEFAULT nextval('x_tag_attr_def_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
tag_def_id BIGINT NOT NULL,
name VARCHAR(255) NOT NULL,
type VARCHAR(50) NOT NULL,
primary key (id),
CONSTRAINT x_tag_attr_def_FK_tag_def_id FOREIGN KEY (tag_def_id) REFERENCES x_tag_def (id),
CONSTRAINT x_tag_attr_def_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_tag_attr_def_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
CREATE INDEX x_tag_attr_def_IDX_tag_def_id ON x_tag_attr_def(tag_def_id);
CREATE INDEX x_tag_attr_def_IDX_added_by_id ON x_tag_attr_def(added_by_id);
CREATE INDEX x_tag_attr_def_IDX_upd_by_id ON x_tag_attr_def(upd_by_id);
commit;

CREATE SEQUENCE x_tag_attr_seq;
CREATE TABLE x_tag_attr(
id BIGINT DEFAULT nextval('x_tag_attr_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
tag_id BIGINT NOT NULL,
name VARCHAR(255) NOT NULL,
value VARCHAR(512) DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_tag_attr_FK_tag_id FOREIGN KEY (tag_id) REFERENCES x_tag (id),
CONSTRAINT x_tag_attr_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_tag_attr_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
CREATE INDEX x_tag_attr_IDX_tag_id ON x_tag_attr(tag_id);
CREATE INDEX x_tag_attr_IDX_added_by_id ON x_tag_attr(added_by_id);
CREATE INDEX x_tag_attr_IDX_upd_by_id ON x_tag_attr(upd_by_id);
commit;

CREATE SEQUENCE x_tag_resource_map_seq;
CREATE TABLE x_tag_resource_map(
id BIGINT NOT NULL,
guid VARCHAR(64) NOT NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
tag_id BIGINT NOT NULL,
res_id BIGINT NOT NULL,
primary key (id),
CONSTRAINT x_tag_res_map_UK_guid UNIQUE (guid),
CONSTRAINT x_tag_res_map_FK_tag_id FOREIGN KEY (tag_id) REFERENCES x_tag (id),
CONSTRAINT x_tag_res_map_FK_res_id FOREIGN KEY (res_id) REFERENCES x_service_resource (id),
CONSTRAINT x_tag_res_map_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_tag_res_map_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
CREATE INDEX x_tag_res_map_IDX_tag_id ON x_tag_resource_map(tag_id);
CREATE INDEX x_tag_res_map_IDX_res_id ON x_tag_resource_map(res_id);
CREATE INDEX x_tag_res_map_IDX_added_by_id ON x_tag_resource_map(added_by_id);
CREATE INDEX x_tag_res_map_IDX_upd_by_id ON x_tag_resource_map(upd_by_id);
commit;

CREATE SEQUENCE x_service_res_el_val_seq;
CREATE TABLE x_service_resource_element_val(
id BIGINT DEFAULT nextval('x_service_res_el_val_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
res_element_id BIGINT NOT NULL,
value VARCHAR(1024) NOT NULL,
sort_order INT DEFAULT '0' NULL,
primary key (id),
CONSTRAINT x_srvc_res_el_val_FK_res_el_id FOREIGN KEY (res_element_id) REFERENCES x_service_resource_element (id),
CONSTRAINT x_srvc_res_el_val_FK_add_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_srvc_res_el_val_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
CREATE INDEX x_srvc_res_el_val_IDX_resel_id ON x_service_resource_element_val(res_element_id);
CREATE INDEX x_srvc_res_el_val_IDX_addby_id ON x_service_resource_element_val(added_by_id);
CREATE INDEX x_srvc_res_el_val_IDX_updby_id ON x_service_resource_element_val(upd_by_id);
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(current_timestamp,current_timestamp,1,1,'Tag Based Policies','');
commit;

-- function add_column_x_service_def_options
select 'delimiter start';
CREATE OR REPLACE FUNCTION add_column_x_service_def_options()
RETURNS void AS $$
DECLARE
 v_column_exists integer := 0;
BEGIN
 select count(*) into v_column_exists from pg_attribute where attrelid in(select oid from pg_class where relname='x_service_def') and attname='def_options';
 IF v_column_exists = 0 THEN
 	ALTER TABLE x_service_def ADD COLUMN def_options VARCHAR(1024) DEFAULT NULL NULL;
 END IF;
END;
$$ LANGUAGE plpgsql;
select 'delimiter end';

-- function add_column_x_policy_item_item_type
CREATE OR REPLACE FUNCTION add_column_x_policy_item_item_type()
RETURNS void AS
$$
DECLARE
 v_column1_exists integer := 0;
 v_column2_exists integer := 0;
 v_column3_exists integer := 0;
BEGIN
 select count(*) into v_column1_exists from pg_attribute where attrelid in(select oid from pg_class where relname='x_policy_item') and attname='item_type';
 select count(*) into v_column2_exists from pg_attribute where attrelid in(select oid from pg_class where relname='x_policy_item') and attname='is_enabled';
 select count(*) into v_column3_exists from pg_attribute where attrelid in(select oid from pg_class where relname='x_policy_item') and attname='comments';
 IF v_column1_exists = 0 AND v_column3_exists = 0 AND v_column3_exists = 0 THEN
 	ALTER TABLE x_policy_item ADD COLUMN item_type INT DEFAULT 0 NOT NULL,ADD COLUMN is_enabled BOOLEAN DEFAULT '1' NOT NULL,ADD COLUMN comments VARCHAR(255) DEFAULT NULL NULL;
 END IF;
END;
$$ LANGUAGE plpgsql;
select 'delimiter end';

-- function add_tag_columns_x_service
CREATE OR REPLACE FUNCTION add_tag_columns_x_service() 
RETURNS void AS
$$ 
DECLARE
 v_column1_exists integer := 0;
 v_column2_exists integer := 0;
 v_column3_exists integer := 0;
BEGIN
 select count(*) into v_column1_exists from pg_attribute where attrelid in(select oid from pg_class where relname='x_service') and attname='tag_service';
 select count(*) into v_column2_exists from pg_attribute where attrelid in(select oid from pg_class where relname='x_service') and attname='tag_version';
 select count(*) into v_column3_exists from pg_attribute where attrelid in(select oid from pg_class where relname='x_service') and attname='tag_update_time';
 IF v_column1_exists = 0 AND v_column3_exists = 0 AND v_column3_exists = 0 THEN
	ALTER TABLE x_service ADD COLUMN tag_service BIGINT DEFAULT NULL NULL,ADD COLUMN tag_version BIGINT DEFAULT 0 NOT NULL,ADD COLUMN tag_update_time TIMESTAMP DEFAULT NULL NULL,ADD CONSTRAINT x_service_FK_tag_service FOREIGN KEY (tag_service) REFERENCES x_service(id);
 END IF;
END;
$$ LANGUAGE plpgsql;
select 'delimiter end';

-- function callLocalUDFs
CREATE OR REPLACE FUNCTION callLocalUDFs() 
RETURNS void AS
$$ 
BEGIN
	perform add_column_x_service_def_options();
	perform add_column_x_policy_item_item_type();
	perform add_tag_columns_x_service();
END;
$$ LANGUAGE plpgsql;
select callLocalUDFs();
select 'delimiter end';
