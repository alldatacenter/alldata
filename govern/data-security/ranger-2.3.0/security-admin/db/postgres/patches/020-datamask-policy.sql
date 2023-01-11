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

DROP TABLE IF EXISTS x_policy_item_datamask;
DROP SEQUENCE IF EXISTS x_policy_item_datamask_def_seq;
DROP TABLE IF EXISTS x_datamask_type_def;
DROP SEQUENCE IF EXISTS x_datamask_type_def_seq;

CREATE SEQUENCE x_datamask_type_def_seq;
CREATE TABLE x_datamask_type_def (
  id BIGINT DEFAULT nextval('x_datamask_type_def_seq'::regclass),
  guid VARCHAR(64) DEFAULT NULL NULL,
  create_time TIMESTAMP DEFAULT NULL NULL,
  update_time TIMESTAMP DEFAULT NULL NULL,
  added_by_id BIGINT DEFAULT NULL NULL,
  upd_by_id BIGINT DEFAULT NULL NULL,
  def_id BIGINT NOT NULL,    
  item_id BIGINT NOT NULL,    
  name VARCHAR(1024) NOT NULL,
  label VARCHAR(1024) NOT NULL,
  description VARCHAR(1024) DEFAULT NULL NULL,
  transformer VARCHAR(1024) DEFAULT NULL NULL,
  datamask_options VARCHAR(1024) DEFAULT NULL NULL,
  rb_key_label VARCHAR(1024) DEFAULT NULL NULL,
  rb_key_description VARCHAR(1024) DEFAULT NULL NULL,
  sort_order INT DEFAULT '0' NULL,
  primary key (id),      
  CONSTRAINT x_datamask_type_def_FK_def_id FOREIGN KEY (def_id) REFERENCES x_service_def (id) ,
  CONSTRAINT x_datamask_type_def_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
  CONSTRAINT x_datamask_type_def_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
CREATE INDEX x_datamask_type_def_IDX_def_id ON x_datamask_type_def(def_id);

CREATE SEQUENCE x_policy_item_datamask_seq;
CREATE TABLE x_policy_item_datamask (
  id BIGINT DEFAULT nextval('x_policy_item_datamask_seq'::regclass),
  guid VARCHAR(64) DEFAULT NULL NULL,
  create_time TIMESTAMP DEFAULT NULL NULL,
  update_time TIMESTAMP DEFAULT NULL NULL,
  added_by_id BIGINT DEFAULT NULL NULL,
  upd_by_id BIGINT DEFAULT NULL NULL,
  policy_item_id BIGINT NOT NULL, 
  type BIGINT NOT NULL,
  condition_expr VARCHAR(1024) DEFAULT NULL NULL,
  value_expr VARCHAR(1024) DEFAULT NULL NULL,
  primary key (id), 
  CONSTRAINT x_policy_item_datamask_FK_policy_item_id FOREIGN KEY (policy_item_id) REFERENCES x_policy_item (id) ,
  CONSTRAINT x_policy_item_datamask_FK_type FOREIGN KEY (type) REFERENCES x_datamask_type_def (id),
  CONSTRAINT x_policy_item_datamask_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
  CONSTRAINT x_policy_item_datamask_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
CREATE INDEX x_policy_item_datamask_IDX_policy_item_id ON x_policy_item_datamask(policy_item_id);

DROP TABLE IF EXISTS x_policy_item_rowfilter;
DROP SEQUENCE IF EXISTS x_policy_item_rowfilter_seq;

CREATE SEQUENCE x_policy_item_rowfilter_seq;
CREATE TABLE x_policy_item_rowfilter (
  id BIGINT DEFAULT nextval('x_policy_item_rowfilter_seq'::regclass),
  guid VARCHAR(64) DEFAULT NULL NULL,
  create_time TIMESTAMP DEFAULT NULL NULL,
  update_time TIMESTAMP DEFAULT NULL NULL,
  added_by_id BIGINT DEFAULT NULL NULL,
  upd_by_id BIGINT DEFAULT NULL NULL,
  policy_item_id BIGINT NOT NULL, 
  filter_expr VARCHAR(1024) DEFAULT NULL NULL,
  primary key (id), 
  CONSTRAINT x_policy_item_rowfilter_FK_policy_item_id FOREIGN KEY (policy_item_id) REFERENCES x_policy_item (id) ,
  CONSTRAINT x_policy_item_rowfilter_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
  CONSTRAINT x_policy_item_rowfilter_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
CREATE INDEX x_policy_item_rowfilter_IDX_policy_item_id ON x_policy_item_rowfilter(policy_item_id);

select 'delimiter start';

/* add x_access_type_def.datamask_options column if it does not exist */
CREATE OR REPLACE FUNCTION add_datamask_options_to_x_access_type_def_table() 
RETURNS void AS $$
DECLARE
 exists_access_type_def_datamask_options integer := 0;
 exists_access_type_def_rowfilter_options integer := 0;
BEGIN
 select count(*) into exists_access_type_def_datamask_options from pg_attribute where attrelid in(select oid from pg_class where relname='x_access_type_def') and attname='datamask_options';
 select count(*) into exists_access_type_def_rowfilter_options from pg_attribute where attrelid in(select oid from pg_class where relname='x_access_type_def') and attname='rowfilter_options';
 IF exists_access_type_def_datamask_options = 0 THEN
 	ALTER TABLE x_access_type_def ADD COLUMN datamask_options VARCHAR(1024) DEFAULT NULL NULL;
 END IF;
 IF exists_access_type_def_rowfilter_options = 0 THEN
 	ALTER TABLE x_access_type_def ADD COLUMN rowfilter_options VARCHAR(1024) DEFAULT NULL NULL;
 END IF;
END;
$$ LANGUAGE plpgsql;
select 'delimiter end';

/* add x_resource_def.datamask_options column if it does not exist */
CREATE OR REPLACE FUNCTION add_datamask_options_to_x_resource_def_table() 
RETURNS void AS $$
DECLARE
 exists_resource_def_datamask_options integer := 0;
 exists_resource_def_rowfilter_options integer := 0;
BEGIN
 select count(*) into exists_resource_def_datamask_options from pg_attribute where attrelid in(select oid from pg_class where relname='x_resource_def') and attname='datamask_options';
 select count(*) into exists_resource_def_rowfilter_options from pg_attribute where attrelid in(select oid from pg_class where relname='x_resource_def') and attname='rowfilter_options';
 IF exists_resource_def_datamask_options = 0 THEN
 	ALTER TABLE x_resource_def ADD COLUMN datamask_options VARCHAR(1024) DEFAULT NULL NULL;
 END IF;
 IF exists_resource_def_rowfilter_options = 0 THEN
 	ALTER TABLE x_resource_def ADD COLUMN rowfilter_options VARCHAR(1024) DEFAULT NULL NULL;
 END IF;
END;
$$ LANGUAGE plpgsql;
select 'delimiter end';

-- function callLocalUDFs
CREATE OR REPLACE FUNCTION callLocalUDFs() 
RETURNS void AS
$$ 
BEGIN
	perform add_datamask_options_to_x_access_type_def_table();
	perform add_datamask_options_to_x_resource_def_table();
END;
$$ LANGUAGE plpgsql;
select callLocalUDFs();
select 'delimiter end';