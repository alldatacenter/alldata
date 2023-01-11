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

call dbo.removeForeignKeysAndTable('x_role_ref_role')
GO
call dbo.removeForeignKeysAndTable('x_policy_ref_role')
GO
call dbo.removeForeignKeysAndTable('x_role_ref_group')
GO
call dbo.removeForeignKeysAndTable('x_role_ref_user')
GO
call dbo.removeForeignKeysAndTable('x_role')
GO

CREATE TABLE dbo.x_role(
id bigint IDENTITY NOT NULL,
create_time datetime DEFAULT NULL NULL,
update_time datetime DEFAULT NULL NULL,
added_by_id bigint DEFAULT NULL NULL,
upd_by_id bigint  DEFAULT NULL NULL,
version bigint  DEFAULT 0 NOT NULL,
name varchar(255) NOT NULL,
description varchar(1024) DEFAULT NULL NULL,
role_options varchar(4000) DEFAULT NULL NULL,
role_text text DEFAULT NULL NULL,
CONSTRAINT x_role_PK_id PRIMARY KEY CLUSTERED(id),
CONSTRAINT x_role_UK_name UNIQUE NONCLUSTERED (name)
)
GO
ALTER TABLE dbo.x_role ADD CONSTRAINT x_role_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_role ADD CONSTRAINT x_role_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES dbo.x_portal_user (id)
GO

CREATE TABLE dbo.x_role_ref_user(
id bigint IDENTITY NOT NULL,
create_time datetime DEFAULT NULL NULL,
update_time datetime DEFAULT NULL NULL,
added_by_id bigint  DEFAULT NULL NULL,
upd_by_id bigint DEFAULT NULL NULL,
role_id bigint NOT NULL,
user_id bigint DEFAULT NULL NULL,
user_name varchar(767) DEFAULT NULL NULL,
priv_type int DEFAULT NULL NULL,
CONSTRAINT x_role_ref_user_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
ALTER TABLE dbo.x_role_ref_user ADD CONSTRAINT x_role_ref_user_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_role_ref_user ADD CONSTRAINT x_role_ref_user_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_role_ref_user ADD CONSTRAINT x_role_ref_user_FK_role_id FOREIGN KEY (role_id) REFERENCES dbo.x_role (id)
GO
ALTER TABLE dbo.x_role_ref_user ADD CONSTRAINT x_role_ref_user_FK_user_id FOREIGN KEY (user_id) REFERENCES dbo.x_user (id)
GO

CREATE TABLE dbo.x_role_ref_group(
id bigint IDENTITY NOT NULL,
create_time datetime DEFAULT NULL NULL,
update_time datetime DEFAULT NULL NULL,
added_by_id bigint DEFAULT NULL NULL,
upd_by_id bigint DEFAULT NULL NULL,
role_id bigint NOT NULL,
group_id bigint DEFAULT NULL NULL,
group_name varchar(767) DEFAULT NULL NULL,
priv_type int DEFAULT NULL,
 CONSTRAINT x_role_ref_grp_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
ALTER TABLE dbo.x_role_ref_group ADD CONSTRAINT x_role_ref_grp_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_role_ref_group ADD CONSTRAINT x_role_ref_grp_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_role_ref_group ADD CONSTRAINT x_role_ref_grp_FK_role_id FOREIGN KEY (role_id) REFERENCES dbo.x_role (id)
GO
ALTER TABLE dbo.x_role_ref_group ADD CONSTRAINT x_role_ref_grp_FK_group_id FOREIGN KEY (group_id) REFERENCES dbo.x_group (id)
GO

CREATE TABLE dbo.x_policy_ref_role(
id bigint IDENTITY NOT NULL,
create_time datetime DEFAULT NULL NULL,
update_time datetime DEFAULT NULL NULL,
added_by_id bigint DEFAULT NULL NULL,
upd_by_id bigint DEFAULT NULL NULL,
policy_id bigint NOT NULL,
role_id bigint NOT NULL,
role_name varchar(255) DEFAULT NULL NULL,
 CONSTRAINT x_pol_ref_role_PK_id PRIMARY KEY CLUSTERED(id),
 CONSTRAINT x_pol_ref_role_UK_polId_roleId UNIQUE NONCLUSTERED (policy_id, role_id)
 )
GO
ALTER TABLE dbo.x_policy_ref_role ADD CONSTRAINT x_pol_ref_role_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_ref_role ADD CONSTRAINT x_pol_ref_role_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_ref_role ADD CONSTRAINT x_pol_ref_role_FK_policy_id FOREIGN KEY (policy_id) REFERENCES dbo.x_policy (id)
GO
ALTER TABLE dbo.x_policy_ref_role ADD CONSTRAINT x_pol_ref_role_FK_role_id FOREIGN KEY (role_id) REFERENCES dbo.x_role (id)
GO

CREATE TABLE dbo.x_role_ref_role(
id bigint IDENTITY NOT NULL,
create_time datetime DEFAULT NULL NULL,
update_time datetime DEFAULT NULL NULL,
added_by_id bigint DEFAULT NULL NULL,
upd_by_id bigint DEFAULT NULL NULL,
role_ref_id bigint DEFAULT NULL NULL,
role_id bigint NOT NULL,
role_name varchar(255) DEFAULT NULL NULL,
priv_type int DEFAULT NULL,
 CONSTRAINT x_role_ref_role_PK_id PRIMARY KEY CLUSTERED(id)
 )
GO
ALTER TABLE dbo.x_role_ref_role ADD CONSTRAINT x_role_ref_role_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_role_ref_role ADD CONSTRAINT x_role_ref_role_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_role_ref_role ADD CONSTRAINT x_role_ref_role_FK_role_ref_id FOREIGN KEY (role_ref_id) REFERENCES dbo.x_role (id)
GO
exit