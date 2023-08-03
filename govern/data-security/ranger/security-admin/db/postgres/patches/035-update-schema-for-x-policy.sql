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

DROP TABLE IF EXISTS x_policy_ref_group CASCADE;
DROP SEQUENCE IF EXISTS x_policy_ref_group_seq;
DROP TABLE IF EXISTS x_policy_ref_user CASCADE;
DROP SEQUENCE IF EXISTS x_policy_ref_user_seq;
DROP TABLE IF EXISTS x_policy_ref_datamask_type CASCADE;
DROP SEQUENCE IF EXISTS x_policy_ref_datamask_type_seq;
DROP TABLE IF EXISTS x_policy_ref_condition CASCADE;
DROP SEQUENCE IF EXISTS x_policy_ref_condition_seq;
DROP TABLE IF EXISTS x_policy_ref_access_type CASCADE;
DROP SEQUENCE IF EXISTS x_policy_ref_access_type_seq;
DROP TABLE IF EXISTS x_policy_ref_resource CASCADE;
DROP SEQUENCE IF EXISTS x_policy_ref_resource_seq;

CREATE SEQUENCE x_policy_ref_resource_seq;
CREATE TABLE x_policy_ref_resource(
id BIGINT DEFAULT nextval('x_policy_ref_resource_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
policy_id BIGINT NOT NULL,
resource_def_id BIGINT NOT NULL,
resource_name varchar(4000) DEFAULT NULL,
primary key(id),
CONSTRAINT x_p_ref_res_UK_polId_resDefId UNIQUE (policy_id, resource_def_id),
CONSTRAINT x_p_ref_res_FK_policy_id FOREIGN KEY(policy_id) REFERENCES x_policy(id),
CONSTRAINT x_p_ref_res_FK_resource_def_id FOREIGN KEY(resource_def_id) REFERENCES x_resource_def(id),
CONSTRAINT x_p_ref_res_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_p_ref_res_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);
commit;

CREATE SEQUENCE x_policy_ref_access_type_seq;
CREATE TABLE x_policy_ref_access_type(
id BIGINT DEFAULT nextval('x_policy_ref_access_type_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
policy_id BIGINT NOT NULL,
access_def_id BIGINT NOT NULL,
access_type_name varchar(4000) DEFAULT NULL,
primary key(id),
CONSTRAINT x_p_ref_acc_UK_polId_accDefId UNIQUE(policy_id, access_def_id),
CONSTRAINT x_p_ref_acc_FK_policy_id FOREIGN KEY(policy_id) REFERENCES x_policy(id),
CONSTRAINT x_p_ref_acc_FK_acc_def_id FOREIGN KEY(access_def_id) REFERENCES x_access_type_def(id),
CONSTRAINT x_p_ref_acc_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_p_ref_acc_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);
commit;

CREATE SEQUENCE x_policy_ref_condition_seq;
CREATE TABLE x_policy_ref_condition(
id BIGINT DEFAULT nextval('x_policy_ref_condition_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
policy_id BIGINT NOT NULL,
condition_def_id BIGINT NOT NULL,
condition_name varchar(4000) DEFAULT NULL,
primary key(id),
CONSTRAINT x_p_ref_cond_UK_polId_cDefId UNIQUE(policy_id, condition_def_id),
CONSTRAINT x_p_ref_cond_FK_policy_id FOREIGN KEY(policy_id) REFERENCES x_policy(id),
CONSTRAINT x_p_ref_cond_FK_cond_def_id FOREIGN KEY(condition_def_id) REFERENCES x_policy_condition_def(id),
CONSTRAINT x_p_ref_cond_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_p_ref_cond_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);
commit;

CREATE SEQUENCE x_policy_ref_datamask_type_seq;
CREATE TABLE x_policy_ref_datamask_type(
id BIGINT DEFAULT nextval('x_policy_ref_datamask_type_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
policy_id BIGINT NOT NULL,
datamask_def_id BIGINT NOT NULL,
datamask_type_name varchar(4000) DEFAULT NULL,
primary key(id),
CONSTRAINT x_p_ref_dmk_UK_polId_dDefId UNIQUE(policy_id, datamask_def_id),
CONSTRAINT x_p_ref_dmk_FK_policy_id FOREIGN KEY(policy_id) REFERENCES x_policy(id),
CONSTRAINT x_p_ref_dmk_FK_dmk_def_id FOREIGN KEY(datamask_def_id) REFERENCES x_datamask_type_def(id),
CONSTRAINT x_p_ref_dmk_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_p_ref_dmk_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);
commit;

CREATE SEQUENCE x_policy_ref_user_seq;
CREATE TABLE x_policy_ref_user(
id BIGINT DEFAULT nextval('x_policy_ref_user_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
policy_id BIGINT NOT NULL,
user_id BIGINT NOT NULL,
user_name varchar(4000) DEFAULT NULL,
primary key(id),
CONSTRAINT x_p_ref_usr_UK_polId_userId UNIQUE(policy_id, user_id),
CONSTRAINT x_p_ref_usr_FK_policy_id FOREIGN KEY(policy_id) REFERENCES x_policy(id),
CONSTRAINT x_p_ref_usr_FK_user_id FOREIGN KEY(user_id) REFERENCES x_user(id),
CONSTRAINT x_p_ref_usr_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_p_ref_usr_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);
commit;

CREATE SEQUENCE x_policy_ref_group_seq;
CREATE TABLE x_policy_ref_group(
id BIGINT DEFAULT nextval('x_policy_ref_group_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
policy_id BIGINT NOT NULL,
group_id BIGINT NOT NULL,
group_name varchar(4000) DEFAULT NULL,
primary key(id),
CONSTRAINT x_p_ref_grp_UK_polId_grpId UNIQUE(policy_id, group_id),
CONSTRAINT x_p_ref_grp_FK_policy_id FOREIGN KEY(policy_id) REFERENCES x_policy(id),
CONSTRAINT x_p_ref_grp_FK_group_id FOREIGN KEY(group_id) REFERENCES x_group(id),
CONSTRAINT x_p_ref_grp_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_p_ref_grp_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);
commit;
select 'delimiter start';
CREATE OR REPLACE FUNCTION add_x_policy_json()
RETURNS void AS $$
DECLARE
 v_column_exists integer := 0;
BEGIN
  select count(*) into v_column_exists from pg_attribute where attrelid in(select oid from pg_class where relname='x_policy') and attname='policy_text';
   IF v_column_exists = 0 THEN
     ALTER TABLE x_policy ADD COLUMN policy_text TEXT DEFAULT NULL NULL;
   END IF;
END;
$$ LANGUAGE plpgsql;
select 'delimiter end';

select add_x_policy_json();
select 'delimiter end';

select 'delimiter start';
CREATE OR REPLACE FUNCTION remove_foreign_key(objName varchar(4000))
RETURNS void AS $$
declare
 tableName VARCHAR(256);
 constraintName VARCHAR(512);
 query varchar(4000);
 curs CURSOR FOR SELECT table_name,constraint_name from information_schema.key_column_usage where constraint_catalog=current_catalog and table_name=objName and position_in_unique_constraint notnull;
begin
  OPEN curs;
  loop
	FETCH curs INTO tableName,constraintName;
	EXIT WHEN NOT FOUND;
	query :='ALTER TABLE ' || objName || ' drop constraint ' || constraintName;
	execute query;
  end loop;
  close curs;
END;
$$ LANGUAGE plpgsql;
select 'delimiter end';

CREATE OR REPLACE FUNCTION removekeys()
RETURNS void AS
$$
BEGIN
	perform remove_foreign_key('x_policy_item');
	perform remove_foreign_key('x_policy_item_access');
	perform remove_foreign_key('x_policy_item_condition');
	perform remove_foreign_key('x_policy_item_datamask');
	perform remove_foreign_key('x_policy_item_group_perm');
	perform remove_foreign_key('x_policy_resource');
	perform remove_foreign_key('x_policy_resource_map');
	perform remove_foreign_key('x_policy_item_user_perm');
	perform remove_foreign_key('x_policy_item_rowfilter');
END;
$$ LANGUAGE plpgsql;
select removekeys();

select 'delimiter end';

commit;
select 'delimiter start';
CREATE OR REPLACE FUNCTION update_TagDefAccessTypes_for_atlas()
RETURNS void AS $$
DECLARE
 new_atlas_def_name VARCHAR(1024);
 v_record_exists integer := 0;
BEGIN
select count(*) into v_record_exists from x_db_version_h where version = 'J10013';
IF v_record_exists = 1 THEN
	select name into new_atlas_def_name from x_service_def where name like 'atlas.%';
	select count(*) into v_record_exists from x_access_type_def where def_id in(select id from x_service_def where name='tag') and name in('atlas:read','atlas:create','atlas:update','atlas:delete','atlas:all');
	IF v_record_exists > 0 THEN
		update x_access_type_def set name=(new_atlas_def_name || ':read')where def_id=100 and name='atlas:read';
		update x_access_type_def set name=(new_atlas_def_name || ':create') where def_id=100 and name='atlas:create';
		update x_access_type_def set name=(new_atlas_def_name || ':update') where def_id=100 and name='atlas:update';
		update x_access_type_def set name=(new_atlas_def_name || ':delete') where def_id=100 and name='atlas:delete';
		update x_access_type_def set name=(new_atlas_def_name || ':all') where def_id=100 and name='atlas:all';
	 END IF;
	 select count(*) into v_record_exists from x_access_type_def_grants where atd_id in (select id from x_access_type_def where def_id in (select id from x_service_def where name='tag') and name like 'atlas%') and implied_grant in ('atlas:read','atlas:create','atlas:update','atlas:delete','atlas:all');
	 IF v_record_exists > 0 THEN
		update x_access_type_def_grants set implied_grant=(new_atlas_def_name || ':read') where implied_grant='atlas:read';
		update x_access_type_def_grants set implied_grant=(new_atlas_def_name || ':create') where implied_grant='atlas:create';
		update x_access_type_def_grants set implied_grant=(new_atlas_def_name || ':update') where implied_grant='atlas:update';
		update x_access_type_def_grants set implied_grant=(new_atlas_def_name || ':delete') where implied_grant='atlas:delete';
		update x_access_type_def_grants set implied_grant=(new_atlas_def_name || ':all') where implied_grant='atlas:all';
	 END IF;
 END IF;
END;
$$ LANGUAGE plpgsql;
select 'delimiter end';

select update_TagDefAccessTypes_for_atlas();
commit;
select 'delimiter end';

