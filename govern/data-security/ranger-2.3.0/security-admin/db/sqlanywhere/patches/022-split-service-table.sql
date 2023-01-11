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
call dbo.removeForeignKeysAndTable('x_service_version_info')
GO

CREATE TABLE dbo.x_service_version_info(
	id bigint IDENTITY NOT NULL,
	service_id bigint NOT NULL,
	policy_version bigint NOT NULL DEFAULT 0,
	policy_update_time datetime DEFAULT NULL NULL,
	tag_version bigint NOT NULL DEFAULT 0,
	tag_update_time datetime DEFAULT NULL NULL,
	CONSTRAINT x_service_version_info_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
ALTER TABLE dbo.x_service_version_info ADD CONSTRAINT x_service_version_info_service_id FOREIGN KEY(service_id) REFERENCES dbo.x_service (id)
GO
CREATE NONCLUSTERED INDEX x_service_version_info_IDX_service_id ON dbo.x_service_version_info(service_id ASC)
GO

exit
