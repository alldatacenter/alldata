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

call dbo.removeForeignKeysAndTable('x_rms_notification')
GO
call dbo.removeForeignKeysAndTable('x_rms_resource_mapping')
GO
call dbo.removeForeignKeysAndTable('x_rms_mapping_provider')
GO
call dbo.removeForeignKeysAndTable('x_rms_service_resource')
GO

CREATE TABLE dbo.x_rms_service_resource(
id BIGINT IDENTITY NOT NULL,
guid VARCHAR(64) NOT NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
version BIGINT DEFAULT NULL NULL,
service_id BIGINT NOT NULL,
resource_signature VARCHAR(128) DEFAULT NULL NULL,
is_enabled tinyint DEFAULT '1' NOT NULL,
service_resource_elements_text TEXT DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_rms_notification_FK_hl_service_id FOREIGN KEY(hl_service_id) REFERENCES x_service(id),
CONSTRAINT x_rms_notification_FK_ll_service_id FOREIGN KEY(ll_service_id) REFERENCES x_service(id)
);
GO
CREATE INDEX x_rms_service_resource_IDX_service_id ON x_rms_service_resource(service_id);
GO

CREATE TABLE dbo.x_rms_notification (
id BIGINT IDENTITY NOT NULL ,
hms_name VARCHAR(128)  DEFAULT NULL NULL,
notification_id BIGINT  DEFAULT NULL NULL,
change_timestamp TIMESTAMP  DEFAULT NULL NULL,
change_type VARCHAR(64) DEFAULT  NULL NULL,
hl_resource_id BIGINT DEFAULT NULL NULL,
hl_service_id BIGINT DEFAULT NULL  NULL,
ll_resource_id BIGINT DEFAULT NULL NULL,
ll_service_id BIGINT  DEFAULT NULL NULL,
PRIMARY KEY (id),
CONSTRAINT x_rms_notification_FK_hl_service_id FOREIGN KEY(hl_service_id) REFERENCES x_service(id),
CONSTRAINT x_rms_notification_FK_ll_service_id FOREIGN KEY(ll_service_id) REFERENCES x_service(id)
);
GO

CREATE INDEX x_rms_notification_IDX_notification_id ON x_rms_notification(notification_id);
CREATE INDEX x_rms_notification_IDX_hms_name_notification_id ON x_rms_notification(hms_name, notification_id);
CREATE INDEX x_rms_notification_IDX_hl_service_id ON x_rms_notification(hl_service_id);
CREATE INDEX x_rms_notification_IDX_ll_service_id ON x_rms_notification(ll_service_id);
GO

CREATE TABLE dbo.x_rms_resource_mapping(
id BIGINT  IDENTITY NOT NULL ,
change_timestamp TIMESTAMP  DEFAULT NULL NULL,
hl_resource_id BIGINT NOT NULL,
ll_resource_id BIGINT NOT NULL,
PRIMARY KEY (id),
CONSTRAINT x_rms_res_map_UK_hl_res_id_ll_res_id UNIQUE(hl_resource_id, ll_resource_id),
CONSTRAINT x_rms_res_map_FK_hl_res_id FOREIGN KEY(hl_resource_id) REFERENCES x_rms_service_resource(id),
CONSTRAINT x_rms_res_map_FK_ll_res_id FOREIGN KEY(ll_resource_id) REFERENCES x_rms_service_resource(id)
);
GO

CREATE INDEX x_rms_resource_mapping_IDX_hl_resource_id ON x_rms_resource_mapping(hl_resource_id);
CREATE INDEX x_rms_resource_mapping_IDX_ll_resource_id ON x_rms_resource_mapping(ll_resource_id);
GO

CREATE TABLE dbo.x_rms_mapping_provider (
id BIGINT IDENTITY NOT NULL ,
change_timestamp TIMESTAMP DEFAULT NULL NULL,
name VARCHAR(128) DEFAULT  NULL NULL,
last_known_version BIGINT NOT NULL,
PRIMARY KEY (id),
CONSTRAINT x_rms_mapping_provider_UK_name UNIQUE(name)
);
GO
EXIT