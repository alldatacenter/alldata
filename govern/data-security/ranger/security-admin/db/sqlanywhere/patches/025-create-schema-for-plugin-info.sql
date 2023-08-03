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
call dbo.removeForeignKeysAndTable('x_plugin_info')
GO

CREATE TABLE dbo.x_plugin_info(
        id bigint IDENTITY NOT NULL,
        create_time datetime DEFAULT NULL NULL,
        update_time datetime DEFAULT NULL NULL,
        service_name varchar(255) NOT NULL,
        app_type varchar(128) NOT NULL,
        host_name varchar(255) NOT NULL,
        ip_address varchar(64) NOT NULL,
        info varchar(1024) NOT NULL,
        CONSTRAINT x_plugin_info_PK_id PRIMARY KEY CLUSTERED(id),
        CONSTRAINT x_plugin_info_UK UNIQUE NONCLUSTERED (service_name, host_name, app_type)
)
GO
CREATE NONCLUSTERED INDEX x_plugin_info_IDX_service_name ON dbo.x_plugin_info(service_name ASC)
GO
CREATE NONCLUSTERED INDEX x_plugin_info_IDX_host_name ON dbo.x_plugin_info(host_name ASC)
GO
exit
