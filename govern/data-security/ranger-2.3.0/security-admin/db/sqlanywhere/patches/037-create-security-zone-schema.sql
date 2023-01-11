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

IF NOT EXISTS(select * from SYS.SYSCOLUMNS where tname = 'x_policy_export_audit' and cname = 'zone_name') THEN
		ALTER TABLE dbo.x_policy_export_audit ADD zone_name varchar(255) DEFAULT NULL NULL;
END IF;
GO

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

call dbo.removeForeignKeysAndTable('x_security_zone_ref_resource')
GO
call dbo.removeForeignKeysAndTable('x_security_zone_ref_group')
GO
call dbo.removeForeignKeysAndTable('x_security_zone_ref_user')
GO
call dbo.removeForeignKeysAndTable('x_security_zone_ref_service')
GO
call dbo.removeForeignKeysAndTable('x_security_zone_ref_tag_srvc')
GO
call dbo.removeForeignKeysAndTable('x_ranger_global_state')
GO
call dbo.removeForeignKeysAndTable('x_security_zone')
GO
CREATE TABLE dbo.x_security_zone(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	version bigint DEFAULT NULL NULL,
	name varchar(255) NOT NULL,
	jsonData text DEFAULT NULL NULL,
	description varchar(1024) DEFAULT NULL NULL,
	CONSTRAINT x_security_zone_PK_id PRIMARY KEY CLUSTERED(id),
	CONSTRAINT x_security_zone_UK_name UNIQUE NONCLUSTERED(name)
)
GO
ALTER TABLE dbo.x_security_zone ADD CONSTRAINT x_security_zone_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_security_zone ADD CONSTRAINT x_security_zone_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
INSERT INTO x_security_zone(create_time, update_time, added_by_id, upd_by_id, version, name, jsonData, description) VALUES (NULL, NULL, 1, 1, 1, ' ', '', 'Unzoned zone');
GO
CREATE TABLE dbo.x_ranger_global_state(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	version bigint DEFAULT NULL NULL,
	state_name varchar(255) NOT NULL,
	app_data varchar(255) DEFAULT NULL NULL,
	CONSTRAINT x_ranger_global_state_PK_id PRIMARY KEY CLUSTERED(id),
	CONSTRAINT x_ranger_global_state_UK_state_name UNIQUE NONCLUSTERED(state_name)
)
GO
ALTER TABLE dbo.x_ranger_global_state ADD CONSTRAINT x_ranger_global_state_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_ranger_global_state ADD CONSTRAINT x_ranger_global_state_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
IF EXISTS(select * from SYS.SYSCOLUMNS where tname = 'x_service' and cname='name') THEN
        IF NOT EXISTS(select * from SYS.SYSCONSTRAINT where constraint_name = 'x_service_UK_name') THEN
                ALTER TABLE dbo.x_service ALTER name varchar(255) NOT NULL;
                ALTER TABLE dbo.x_service ADD CONSTRAINT x_service_UK_name UNIQUE NONCLUSTERED (name);
        END IF;
END IF;
GO
CREATE TABLE dbo.x_security_zone_ref_service(
        id bigint IDENTITY NOT NULL,
        create_time datetime DEFAULT NULL NULL,
        update_time datetime DEFAULT NULL NULL,
        added_by_id bigint DEFAULT NULL NULL,
        upd_by_id bigint DEFAULT NULL NULL,
        zone_id bigint DEFAULT NULL NULL,
        service_id bigint DEFAULT NULL NULL,
        service_name varchar(255) DEFAULT NULL NULL,
        CONSTRAINT x_sz_ref_service_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
ALTER TABLE dbo.x_security_zone_ref_service ADD CONSTRAINT x_sz_ref_service_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_security_zone_ref_service ADD CONSTRAINT x_sz_ref_service_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_security_zone_ref_service ADD CONSTRAINT x_sz_ref_service_FK_zone_id FOREIGN KEY(zone_id) REFERENCES dbo.x_security_zone (id)
GO
ALTER TABLE dbo.x_security_zone_ref_service ADD CONSTRAINT x_sz_ref_service_FK_service_id FOREIGN KEY(service_id) REFERENCES dbo.x_service (id)
GO
ALTER TABLE dbo.x_security_zone_ref_service ADD CONSTRAINT x_sz_ref_service_FK_service_name FOREIGN KEY(service_name) REFERENCES dbo.x_service (name)
GO
CREATE TABLE dbo.x_security_zone_ref_tag_srvc(
        id bigint IDENTITY NOT NULL,
        create_time datetime DEFAULT NULL NULL,
        update_time datetime DEFAULT NULL NULL,
        added_by_id bigint DEFAULT NULL NULL,
        upd_by_id bigint DEFAULT NULL NULL,
        zone_id bigint DEFAULT NULL NULL,
        tag_srvc_id bigint DEFAULT NULL NULL,
        tag_srvc_name varchar(255) DEFAULT NULL NULL,
        CONSTRAINT x_sz_ref_tag_srvc_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
ALTER TABLE dbo.x_security_zone_ref_tag_srvc ADD CONSTRAINT x_sz_ref_tag_service_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_security_zone_ref_tag_srvc ADD CONSTRAINT x_sz_ref_tag_service_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_security_zone_ref_tag_srvc ADD CONSTRAINT x_sz_ref_tag_service_FK_zone_id FOREIGN KEY(zone_id) REFERENCES dbo.x_security_zone (id)
GO
ALTER TABLE dbo.x_security_zone_ref_tag_srvc ADD CONSTRAINT x_sz_ref_tag_service_FK_tag_service_id FOREIGN KEY(tag_srvc_id) REFERENCES dbo.x_service (id)
GO
ALTER TABLE dbo.x_security_zone_ref_tag_srvc ADD CONSTRAINT x_sz_ref_tag_service_FK_tag_service_name FOREIGN KEY(tag_srvc_name) REFERENCES dbo.x_service (name)
GO
CREATE TABLE dbo.x_security_zone_ref_resource(
        id bigint IDENTITY NOT NULL,
        create_time datetime DEFAULT NULL NULL,
        update_time datetime DEFAULT NULL NULL,
        added_by_id bigint DEFAULT NULL NULL,
        upd_by_id bigint DEFAULT NULL NULL,
        zone_id bigint DEFAULT NULL NULL,
        resource_def_id bigint DEFAULT NULL NULL,
        resource_name varchar(255) DEFAULT NULL NULL,
        CONSTRAINT x_sz_ref_resource_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
ALTER TABLE dbo.x_security_zone_ref_resource ADD CONSTRAINT x_sz_ref_resource_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_security_zone_ref_resource ADD CONSTRAINT x_sz_ref_resource_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_security_zone_ref_resource ADD CONSTRAINT x_sz_ref_resource_FK_zone_id FOREIGN KEY(zone_id) REFERENCES dbo.x_security_zone (id)
GO
ALTER TABLE dbo.x_security_zone_ref_resource ADD CONSTRAINT x_sz_ref_resource_FK_service_id FOREIGN KEY(resource_def_id) REFERENCES dbo.x_resource_def (id)
GO
CREATE TABLE dbo.x_security_zone_ref_user(
        id bigint IDENTITY NOT NULL,
        create_time datetime DEFAULT NULL NULL,
        update_time datetime DEFAULT NULL NULL,
        added_by_id bigint DEFAULT NULL NULL,
        upd_by_id bigint DEFAULT NULL NULL,
        zone_id bigint DEFAULT NULL NULL,
        user_id bigint DEFAULT NULL NULL,
        user_name varchar(767) DEFAULT NULL NULL,
        user_type tinyint DEFAULT NULL,
        CONSTRAINT x_sz_ref_auser_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
ALTER TABLE dbo.x_security_zone_ref_user ADD CONSTRAINT x_sz_ref_auser_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_security_zone_ref_user ADD CONSTRAINT x_sz_ref_auser_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_security_zone_ref_user ADD CONSTRAINT x_sz_ref_auser_FK_zone_id FOREIGN KEY(zone_id) REFERENCES dbo.x_security_zone (id)
GO
ALTER TABLE dbo.x_security_zone_ref_user ADD CONSTRAINT x_sz_ref_auser_FK_user_id FOREIGN KEY(user_id) REFERENCES dbo.x_user (id)
GO
ALTER TABLE dbo.x_security_zone_ref_user ADD CONSTRAINT x_sz_ref_auser_FK_user_name FOREIGN KEY(user_name) REFERENCES dbo.x_user (user_name)
GO
CREATE TABLE dbo.x_security_zone_ref_group(
        id bigint IDENTITY NOT NULL,
        create_time datetime DEFAULT NULL NULL,
        update_time datetime DEFAULT NULL NULL,
        added_by_id bigint DEFAULT NULL NULL,
        upd_by_id bigint DEFAULT NULL NULL,
        zone_id bigint DEFAULT NULL NULL,
        group_id bigint DEFAULT NULL NULL,
        group_name varchar(767) DEFAULT NULL NULL,
        group_type tinyint DEFAULT NULL,
        CONSTRAINT x_sz_ref_agroup_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
ALTER TABLE dbo.x_security_zone_ref_group ADD CONSTRAINT x_sz_ref_agrp_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_security_zone_ref_group ADD CONSTRAINT x_sz_ref_agrp_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_security_zone_ref_group ADD CONSTRAINT x_sz_ref_agrp_FK_zone_id FOREIGN KEY(zone_id) REFERENCES dbo.x_security_zone (id)
GO
ALTER TABLE dbo.x_security_zone_ref_group ADD CONSTRAINT x_sz_ref_agrp_FK_group_id FOREIGN KEY(group_id) REFERENCES dbo.x_group (id)
GO
CREATE OR REPLACE FUNCTION dbo.getXportalUIdByLoginId (input_val CHAR(60))
RETURNS INTEGER
BEGIN
  DECLARE myid INTEGER;
  SELECT x_portal_user.id into myid FROM x_portal_user WHERE x_portal_user.login_id=input_val;
  RETURN (myid);
END;
GO
CREATE OR REPLACE FUNCTION dbo.getModulesIdByName (input_val CHAR(60))
RETURNS INTEGER
BEGIN
  DECLARE myid INTEGER;
  SELECT x_modules_master.id into myid FROM x_modules_master WHERE x_modules_master.module=input_val;
  RETURN (myid);
END;
GO
IF NOT EXISTS(select * from x_security_zone where id = 1 and name=' ') THEN
	INSERT INTO x_security_zone(create_time, update_time, added_by_id, upd_by_id, version, name, jsonData, description) VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, dbo.getXportalUIdByLoginId('admin'), dbo.getXportalUIdByLoginId('admin'), 1, ' ', '', 'Unzoned zone');
END IF;
GO
IF NOT EXISTS(select * from SYS.SYSCOLUMNS where tname = 'x_policy' and cname='zone_id') THEN
	ALTER TABLE dbo.x_policy ADD zone_id bigint DEFAULT 1 NOT NULL;
	ALTER TABLE dbo.x_policy ADD CONSTRAINT x_policy_FK_zone_id FOREIGN KEY(zone_id) REFERENCES dbo.x_security_zone (id);
END IF;
GO
IF NOT EXISTS(select * from x_modules_master where module = 'Security Zone') THEN
	INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(GETDATE(),GETDATE(),dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),'Security Zone','')
END IF;
GO
IF NOT EXISTS(select * from x_user_module_perm where user_id=dbo.getXportalUIdByLoginId('admin') and module_id=dbo.getModulesIdByName('Security Zone')) THEN
	INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('admin'),dbo.getModulesIdByName('Security Zone'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
END IF;
GO
IF NOT EXISTS(select * from x_user_module_perm where user_id=dbo.getXportalUIdByLoginId('rangerusersync') and module_id=dbo.getModulesIdByName('Security Zone')) THEN
	INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangerusersync'),dbo.getModulesIdByName('Security Zone'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
END IF;
GO
IF NOT EXISTS(select * from x_user_module_perm where user_id=dbo.getXportalUIdByLoginId('rangertagsync') and module_id=dbo.getModulesIdByName('Security Zone')) THEN
	INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangertagsync'),dbo.getModulesIdByName('Security Zone'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
END IF;
GO

exit
