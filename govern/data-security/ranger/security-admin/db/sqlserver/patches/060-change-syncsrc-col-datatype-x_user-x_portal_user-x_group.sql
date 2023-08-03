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

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
IF EXISTS (
        SELECT type_desc, type
        FROM sys.procedures WITH(NOLOCK)
        WHERE NAME = 'alterDataTypesOfColumn'
            AND type = 'P'
      )
BEGIN
	 PRINT 'Proc exist with name dbo.alterDataTypesOfColumn'
     DROP PROCEDURE dbo.alterDataTypesOfColumn
	 PRINT 'Proc dropped dbo.alterDataTypesOfColumn'
END
GO
CREATE PROCEDURE dbo.alterDataTypesOfColumn
	@tablename nvarchar(100), @columnname nvarchar(100)
AS
BEGIN
  IF EXISTS(select * from INFORMATION_SCHEMA.columns where table_name = @tablename and column_name = @columnname and CHARACTER_MAXIMUM_LENGTH=4000)
  BEGIN
    DECLARE @ObjectName VARCHAR(100);
    DECLARE @stmt VARCHAR(100);
    SELECT @ObjectName = OBJECT_NAME([default_object_id]) FROM SYS.COLUMNS WHERE [object_id] = OBJECT_ID('[dbo].[' + @tablename + ']') AND [name] = @columnname;
    IF @ObjectName IS NOT NULL
    BEGIN
      SET @stmt = 'ALTER TABLE [dbo].[' + @tablename + '] DROP CONSTRAINT ' + @ObjectName
      EXEC (@stmt);
    END
    IF NOT EXISTS(select name from SYS.sysobjects where parent_obj in (select id from SYS.sysobjects where name=@tablename) and name=@ObjectName)
    BEGIN
      SET @stmt = 'ALTER TABLE [dbo].[' + @tablename + '] ALTER COLUMN [' + @columnname + '] [nvarchar] (max) NULL'
      EXEC (@stmt);
    END
  END
END
GO

EXEC dbo.alterDataTypesOfColumn 'x_portal_user', 'notes'
GO
EXEC dbo.alterDataTypesOfColumn 'x_portal_user', 'other_attributes'
GO
EXEC dbo.alterDataTypesOfColumn 'x_portal_user', 'sync_source'
GO
EXEC dbo.alterDataTypesOfColumn 'x_user', 'descr'
GO
EXEC dbo.alterDataTypesOfColumn 'x_user', 'other_attributes'
GO
EXEC dbo.alterDataTypesOfColumn 'x_user', 'sync_source'
GO
EXEC dbo.alterDataTypesOfColumn 'x_group', 'descr'
GO
EXEC dbo.alterDataTypesOfColumn 'x_group', 'other_attributes'
GO
EXEC dbo.alterDataTypesOfColumn 'x_group', 'sync_source'
GO
EXIT