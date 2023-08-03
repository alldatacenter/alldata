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
call dbo.removeForeignKeysAndTable('x_policy_item_rowfilter')
GO
call dbo.removeForeignKeysAndTable('x_policy_item_datamask')
GO
call dbo.removeForeignKeysAndTable('x_datamask_type_def')
GO

CREATE TABLE dbo.x_datamask_type_def(
	id bigint IDENTITY NOT NULL,
	guid varchar(64) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	def_id bigint NOT NULL,
	item_id bigint NOT NULL,
	name varchar(1024) NOT NULL,
	label varchar(1024) NOT NULL,
	description varchar(1024) DEFAULT NULL NULL,
	transformer varchar(1024) DEFAULT NULL NULL,
	datamask_options varchar(1024) DEFAULT NULL NULL,
	rb_key_label varchar(1024) DEFAULT NULL NULL,
	rb_key_description varchar(1024) DEFAULT NULL NULL,
	sort_order int DEFAULT 0  NULL,
	CONSTRAINT x_datamask_type_def_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
CREATE TABLE dbo.x_policy_item_datamask(
	id bigint IDENTITY NOT NULL,
	guid varchar(64) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	policy_item_id bigint NOT NULL,
	type bigint NOT NULL,
	condition_expr varchar(1024) DEFAULT NULL NULL,
	value_expr varchar(1024) DEFAULT NULL NULL,
	CONSTRAINT x_policy_item_datamask_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
CREATE TABLE dbo.x_policy_item_rowfilter(
	id bigint IDENTITY NOT NULL,
	guid varchar(64) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	policy_item_id bigint NOT NULL,
	filter_expr varchar(1024) DEFAULT NULL NULL,
	CONSTRAINT x_policy_item_rowfilter_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
ALTER TABLE dbo.x_datamask_type_def ADD CONSTRAINT x_datamask_type_def_FK_def_id FOREIGN KEY(def_id) REFERENCES dbo.x_service_def (id)
GO
ALTER TABLE dbo.x_datamask_type_def ADD CONSTRAINT x_datamask_type_def_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_datamask_type_def ADD CONSTRAINT x_datamask_type_def_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_item_datamask ADD CONSTRAINT x_policy_item_datamask_FK_policy_item_id FOREIGN KEY(policy_item_id) REFERENCES dbo.x_policy_item (id)
GO
ALTER TABLE dbo.x_policy_item_datamask ADD CONSTRAINT x_policy_item_datamask_FK_type FOREIGN KEY(type) REFERENCES dbo.x_datamask_type_def (id)
GO
ALTER TABLE dbo.x_policy_item_datamask ADD CONSTRAINT x_policy_item_datamask_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_item_datamask ADD CONSTRAINT x_policy_item_datamask_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_item_rowfilter ADD CONSTRAINT x_policy_item_rowfilter_FK_policy_item_id FOREIGN KEY(policy_item_id) REFERENCES dbo.x_policy_item (id)
GO
ALTER TABLE dbo.x_policy_item_rowfilter ADD CONSTRAINT x_policy_item_rowfilter_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_item_rowfilter ADD CONSTRAINT x_policy_item_rowfilter_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
CREATE NONCLUSTERED INDEX x_datamask_type_def_IDX_def_id ON dbo.x_datamask_type_def(def_id ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_item_datamask_IDX_policy_item_id ON dbo.x_policy_item_datamask(policy_item_id ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_item_rowfilter_IDX_policy_item_id ON dbo.x_policy_item_rowfilter(policy_item_id ASC)
GO

-- add datamask_options column in x_access_type_def table if not exist
IF NOT EXISTS(select * from SYS.SYSCOLUMNS where tname = 'x_access_type_def' and cname = 'datamask_options') THEN
	IF NOT EXISTS(select * from SYS.SYSCOLUMNS where tname = 'x_access_type_def' and cname = 'rowfilter_options') THEN
		ALTER TABLE dbo.x_access_type_def ADD (datamask_options VARCHAR(1024) DEFAULT NULL NULL,rowfilter_options VARCHAR(1024) DEFAULT NULL NULL);
	END IF;
END IF;
GO
-- add datamask_options column in x_resource_def table if not exist
IF NOT EXISTS(select * from SYS.SYSCOLUMNS where tname = 'x_resource_def' and cname = 'datamask_options') THEN
	IF NOT EXISTS(select * from SYS.SYSCOLUMNS where tname = 'x_resource_def' and cname = 'rowfilter_options') THEN
		ALTER TABLE dbo.x_resource_def ADD (datamask_options VARCHAR(1024) DEFAULT NULL NULL,rowfilter_options VARCHAR(1024) DEFAULT NULL NULL);
	END IF;	
END IF;
GO

exit
