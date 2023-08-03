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
call dbo.removeForeignKeysAndTable('x_ugsync_audit_info')
GO

CREATE TABLE dbo.x_ugsync_audit_info(
        id bigint IDENTITY NOT NULL,
        create_time datetime DEFAULT NULL NULL,
        update_time datetime DEFAULT NULL NULL,
        added_by_id bigint DEFAULT NULL NULL,
        upd_by_id bigint DEFAULT NULL NULL,
        event_time datetime DEFAULT NULL NULL,
        user_name varchar(255) NOT  NULL,
        sync_source varchar(128) NOT NULL,
        no_of_new_users bigint NOT NULL,
        no_of_new_groups bigint NOT NULL,
        no_of_modified_users bigint NOT NULL,
        no_of_modified_groups bigint NOT NULL,
        sync_source_info varchar(4000) NOT NULL,
        session_id varchar(255) DEFAULT NULL NULL,
        CONSTRAINT x_ugsync_audit_info_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
CREATE NONCLUSTERED INDEX x_ugsync_audit_info_etime ON dbo.x_ugsync_audit_info(event_time ASC)
GO
CREATE NONCLUSTERED INDEX x_ugsync_audit_info_sync_src ON dbo.x_ugsync_audit_info(sync_source ASC)
GO
CREATE NONCLUSTERED INDEX x_ugsync_audit_info_uname ON dbo.x_ugsync_audit_info(user_name ASC)
GO
exit
