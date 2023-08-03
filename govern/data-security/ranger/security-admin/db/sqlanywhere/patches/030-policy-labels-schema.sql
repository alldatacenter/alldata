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
call dbo.removeForeignKeysAndTable('x_policy_label_map')
GO
call dbo.removeForeignKeysAndTable('x_policy_label')
GO

CREATE TABLE dbo.x_policy_label (
        id bigint IDENTITY NOT NULL,
        guid varchar(64) DEFAULT NULL NULL,
        create_time datetime DEFAULT NULL NULL,
        update_time datetime DEFAULT NULL NULL,
        added_by_id bigint DEFAULT NULL NULL,
        upd_by_id bigint DEFAULT NULL NULL,
        label_name varchar(512) DEFAULT NULL,
        CONSTRAINT x_policy_label_PK_id PRIMARY KEY CLUSTERED(id),
        CONSTRAINT x_policy_label_UK_label_name UNIQUE NONCLUSTERED (label_name)
)
GO
CREATE TABLE dbo.x_policy_label_map (
        id bigint IDENTITY NOT NULL,
        guid varchar(64) DEFAULT NULL NULL,
        create_time datetime DEFAULT NULL NULL,
        update_time datetime DEFAULT NULL NULL,
        added_by_id bigint DEFAULT NULL NULL,
        upd_by_id bigint DEFAULT NULL NULL,
        policy_id bigint NOT NULL,
        policy_label_id bigint NOT NULL,
        CONSTRAINT x_policy_label_map_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
ALTER TABLE dbo.x_policy_label ADD CONSTRAINT x_policy_label_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_label ADD CONSTRAINT x_policy_label_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_label_map ADD CONSTRAINT x_policy_label_map_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_label_map ADD CONSTRAINT x_policy_label_map_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_label_map ADD CONSTRAINT x_policy_label_map_FK_policy_id FOREIGN KEY (policy_id) REFERENCES dbo.x_policy (id)
GO
ALTER TABLE dbo.x_policy_label_map ADD CONSTRAINT x_policy_label_map_FK_policy_label_id FOREIGN KEY (policy_label_id) REFERENCES dbo.x_policy_label (id)
GO
ALTER TABLE dbo.x_policy_label_map ADD CONSTRAINT [x_policy_label_map$x_policy_label_map_pid_plid] UNIQUE (policy_id, policy_label_id)
GO
CREATE NONCLUSTERED INDEX x_policy_label_IDX_label_id ON dbo.x_policy_label(id ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_label_IDX_label_name ON dbo.x_policy_label(label_name ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_label_IDX_label_map_id ON dbo.x_policy_label_map(id ASC)
GO
exit
