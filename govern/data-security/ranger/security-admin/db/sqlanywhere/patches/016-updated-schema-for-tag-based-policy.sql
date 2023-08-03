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

CREATE OR REPLACE PROCEDURE dbo.removeForeignKeysAndTable (IN table_name varchar(100))
AS
BEGIN
	DECLARE @stmt VARCHAR(300)
	DECLARE @tblname VARCHAR(300)
	DECLARE @drpstmt VARCHAR(1000)
	DECLARE cur CURSOR FOR select 'alter table dbo.' + table_name + ' drop constraint ' + role from SYS.SYSFOREIGNKEYS where foreign_creator ='dbo' and foreign_tname = table_name
	OPEN cur WITH HOLD
		fetch cur into @stmt
		WHILE (@@sqlstatus = 0)
		BEGIN
			execute(@stmt)
			fetch cur into @stmt
		END
	close cur
	DEALLOCATE CURSOR cur
	SET @tblname ='dbo.' + table_name;
	SET @drpstmt = 'DROP TABLE IF EXISTS ' + @tblname;
	execute(@drpstmt)
END
GO
call dbo.removeForeignKeysAndTable('x_service_resource_element_val')
GO
call dbo.removeForeignKeysAndTable('x_tag_resource_map')
GO
call dbo.removeForeignKeysAndTable('x_tag_attr')
GO
call dbo.removeForeignKeysAndTable('x_tag_attr_def')
GO
call dbo.removeForeignKeysAndTable('x_service_resource_element')
GO
call dbo.removeForeignKeysAndTable('x_service_resource')
GO
call dbo.removeForeignKeysAndTable('x_tag')
GO
call dbo.removeForeignKeysAndTable('x_tag_def')
GO

CREATE TABLE dbo.x_tag_def(
	id bigint IDENTITY NOT NULL,
	guid varchar(64) NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	version bigint DEFAULT NULL NULL,
	name varchar(255) NOT NULL,
	source varchar(128) DEFAULT NULL NULL,
	is_enabled tinyint DEFAULT 0 NOT NULL,
	CONSTRAINT x_tag_def_PK_id PRIMARY KEY CLUSTERED(id),
	CONSTRAINT x_tag_def_UK_guid UNIQUE NONCLUSTERED (guid),
	CONSTRAINT x_tag_def_UK_name UNIQUE NONCLUSTERED (name)
)
GO
CREATE TABLE dbo.x_tag(
	id bigint IDENTITY NOT NULL,
	guid varchar(64) NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	version bigint DEFAULT NULL NULL,
	type bigint NOT NULL,
	CONSTRAINT x_tag_PK_id PRIMARY KEY CLUSTERED(id),
	CONSTRAINT x_tag_UK_guid UNIQUE NONCLUSTERED (guid)
)
GO
CREATE TABLE dbo.x_service_resource(
	id bigint IDENTITY NOT NULL,
	guid varchar(64) NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	version bigint DEFAULT NULL NULL,
	service_id bigint NOT NULL,
	resource_signature varchar(128) DEFAULT NULL NULL,
	is_enabled tinyint DEFAULT 1 NOT NULL,
	CONSTRAINT x_service_res_PK_id PRIMARY KEY CLUSTERED(id),
	CONSTRAINT x_service_res_UK_guid UNIQUE NONCLUSTERED (guid)
)
GO
CREATE TABLE dbo.x_service_resource_element(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	res_id bigint NOT NULL,
	res_def_id bigint NOT NULL,
	is_excludes tinyint DEFAULT 0 NOT NULL,
	is_recursive tinyint DEFAULT 0 NOT NULL,
	CONSTRAINT x_srvc_res_el_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
CREATE TABLE dbo.x_tag_attr_def(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	tag_def_id bigint NOT NULL,
	name varchar(255) NOT NULL,
	type varchar(50) NOT NULL,
	CONSTRAINT x_tag_attr_def_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
CREATE TABLE dbo.x_tag_attr(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	tag_id bigint NOT NULL,
	name varchar(255) NOT NULL,
	value varchar(512) DEFAULT NULL NULL,
	CONSTRAINT x_tag_attr_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
CREATE TABLE dbo.x_tag_resource_map(
	id bigint IDENTITY NOT NULL,
	guid varchar(64) NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	tag_id bigint NOT NULL,
	res_id bigint NOT NULL,
	CONSTRAINT x_tag_res_map_PK_id PRIMARY KEY CLUSTERED(id),
	CONSTRAINT x_tag_res_map_UK_guid UNIQUE NONCLUSTERED (guid)
)
GO
CREATE TABLE dbo.x_service_resource_element_val(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	res_element_id bigint NOT NULL,
	value varchar(1024) NOT NULL,
	sort_order int DEFAULT 0  NULL,
	CONSTRAINT x_srvc_res_el_val_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
ALTER TABLE dbo.x_tag_def ADD CONSTRAINT x_tag_def_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_tag_def ADD CONSTRAINT x_tag_def_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_tag ADD CONSTRAINT x_tag_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_tag ADD CONSTRAINT x_tag_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_tag ADD CONSTRAINT x_tag_FK_type FOREIGN KEY(type) REFERENCES dbo.x_tag_def (id)
GO
ALTER TABLE dbo.x_service_resource ADD CONSTRAINT x_service_res_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_service_resource ADD CONSTRAINT x_service_res_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_service_resource ADD CONSTRAINT x_service_res_FK_service_id FOREIGN KEY(service_id) REFERENCES dbo.x_service (id)
GO
ALTER TABLE dbo.x_service_resource_element ADD CONSTRAINT x_srvc_res_el_FK_res_def_id FOREIGN KEY(res_def_id) REFERENCES dbo.x_resource_def (id)
GO
ALTER TABLE dbo.x_service_resource_element ADD CONSTRAINT x_srvc_res_el_FK_res_id FOREIGN KEY(res_id) REFERENCES dbo.x_service_resource (id)
GO
ALTER TABLE dbo.x_service_resource_element ADD CONSTRAINT x_srvc_res_el_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_service_resource_element ADD CONSTRAINT x_srvc_res_el_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_tag_attr_def ADD CONSTRAINT x_tag_attr_def_FK_tag_def_id FOREIGN KEY(tag_def_id) REFERENCES dbo.x_tag_def (id)
GO
ALTER TABLE dbo.x_tag_attr_def ADD CONSTRAINT x_tag_attr_def_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_tag_attr_def ADD CONSTRAINT x_tag_attr_def_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_tag_attr ADD CONSTRAINT x_tag_attr_FK_tag_id FOREIGN KEY(tag_id) REFERENCES dbo.x_tag (id)
GO
ALTER TABLE dbo.x_tag_attr ADD CONSTRAINT x_tag_attr_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_tag_attr ADD CONSTRAINT x_tag_attr_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_tag_resource_map ADD CONSTRAINT x_tag_res_map_FK_tag_id FOREIGN KEY(tag_id) REFERENCES dbo.x_tag (id)
GO
ALTER TABLE dbo.x_tag_resource_map ADD CONSTRAINT x_tag_res_map_FK_res_id FOREIGN KEY(res_id) REFERENCES dbo.x_service_resource (id)
GO
ALTER TABLE dbo.x_tag_resource_map ADD CONSTRAINT x_tag_res_map_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_tag_resource_map ADD CONSTRAINT x_tag_res_map_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_service_resource_element_val ADD CONSTRAINT x_srvc_res_el_val_FK_res_el_id FOREIGN KEY(res_element_id) REFERENCES dbo.x_service_resource_element (id)
GO
ALTER TABLE dbo.x_service_resource_element_val ADD CONSTRAINT x_srvc_res_el_val_FK_add_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_service_resource_element_val ADD CONSTRAINT x_srvc_res_el_val_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
CREATE NONCLUSTERED INDEX x_tag_def_IDX_added_by_id ON dbo.x_tag_def(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_tag_def_IDX_upd_by_id ON dbo.x_tag_def(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_tag_IDX_type ON dbo.x_tag(type ASC)
GO
CREATE NONCLUSTERED INDEX x_tag_IDX_added_by_id ON dbo.x_tag(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_tag_IDX_upd_by_id ON dbo.x_tag(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_service_res_IDX_added_by_id ON dbo.x_service_resource(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_service_res_IDX_upd_by_id ON dbo.x_service_resource(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_srvc_res_el_IDX_added_by_id ON dbo.x_service_resource_element(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_srvc_res_el_IDX_upd_by_id ON dbo.x_service_resource_element(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_tag_attr_def_IDX_tag_def_id ON dbo.x_tag_attr_def(tag_def_id ASC)
GO
CREATE NONCLUSTERED INDEX x_tag_attr_def_IDX_added_by_id ON dbo.x_tag_attr_def(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_tag_attr_def_IDX_upd_by_id ON dbo.x_tag_attr_def(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_tag_attr_IDX_tag_id ON dbo.x_tag_attr(tag_id ASC)
GO
CREATE NONCLUSTERED INDEX x_tag_attr_IDX_added_by_id ON dbo.x_tag_attr(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_tag_attr_IDX_upd_by_id ON dbo.x_tag_attr(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_tag_res_map_IDX_tag_id ON dbo.x_tag_resource_map(tag_id ASC)
GO
CREATE NONCLUSTERED INDEX x_tag_res_map_IDX_res_id ON dbo.x_tag_resource_map(res_id ASC)
GO
CREATE NONCLUSTERED INDEX x_tag_res_map_IDX_added_by_id ON dbo.x_tag_resource_map(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_tag_res_map_IDX_upd_by_id ON dbo.x_tag_resource_map(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_srvc_res_el_val_IDX_resel_id ON dbo.x_service_resource_element_val(res_element_id ASC)
GO
CREATE NONCLUSTERED INDEX x_srvc_res_el_val_IDX_addby_id ON dbo.x_service_resource_element_val(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_srvc_res_el_val_IDX_updby_id ON dbo.x_service_resource_element_val(upd_by_id ASC)
GO
INSERT INTO dbo.x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(GETDATE(),GETDATE(),1,1,'Tag Based Policies','')
GO

IF NOT EXISTS(select * from SYS.SYSCOLUMNS where tname = 'x_service_def' and cname = 'def_options') THEN
		ALTER TABLE dbo.x_service_def ADD def_options varchar(1024) DEFAULT NULL NULL;
END IF;
GO

IF NOT EXISTS(select * from SYS.SYSCOLUMNS where tname = 'x_policy_item' and cname='item_type') THEN
	IF NOT EXISTS(select * from SYS.SYSCOLUMNS where tname = 'x_policy_item' and cname='is_enabled') THEN
		IF NOT EXISTS(select * from SYS.SYSCOLUMNS where tname = 'x_policy_item' and cname='comments') THEN
			ALTER TABLE dbo.x_policy_item ADD (item_type int DEFAULT 0 NOT NULL,is_enabled tinyint DEFAULT 1 NOT NULL,comments varchar(255) DEFAULT NULL NULL);
		END IF;
	END IF;
END IF;
GO

IF NOT EXISTS(select * from SYS.SYSCOLUMNS where tname = 'x_service' and cname='tag_service') THEN
	IF NOT EXISTS(select * from SYS.SYSCOLUMNS where tname = 'x_service' and cname='tag_version') THEN
		IF NOT EXISTS(select * from SYS.SYSCOLUMNS where tname = 'x_service' and cname='tag_update_time') THEN
			ALTER TABLE dbo.x_service ADD (tag_service bigint DEFAULT NULL NULL,tag_version bigint DEFAULT 0 NOT NULL,tag_update_time datetime DEFAULT NULL NULL), ADD CONSTRAINT x_service_FK_tag_service FOREIGN KEY(tag_service) REFERENCES dbo.x_service (id);
		END IF;
	END IF;
END IF;
GO

exit
