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

GO
IF NOT EXISTS(select * from INFORMATION_SCHEMA.columns where table_name = 'x_tag_def' and column_name = 'tag_attrs_def_text')
BEGIN
	ALTER TABLE [dbo].[x_tag_def] ADD [tag_attrs_def_text] [nvarchar](max) DEFAULT NULL NULL;
END
IF NOT EXISTS(select * from INFORMATION_SCHEMA.columns where table_name = 'x_tag' and column_name = 'tag_attrs_text')
BEGIN
	ALTER TABLE [dbo].[x_tag] ADD [tag_attrs_text] [nvarchar](max) DEFAULT NULL NULL;
END
IF NOT EXISTS(select * from INFORMATION_SCHEMA.columns where table_name = 'x_service_resource' and column_name = 'service_resource_elements_text')
BEGIN
	ALTER TABLE [dbo].[x_service_resource] ADD [service_resource_elements_text] [nvarchar](max) DEFAULT NULL NULL;
END
IF NOT EXISTS(select * from INFORMATION_SCHEMA.columns where table_name = 'x_service_resource' and column_name = 'tags_text')
BEGIN
	ALTER TABLE [dbo].[x_service_resource] ADD [tags_text] [nvarchar](max) DEFAULT NULL NULL;
END
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
IF EXISTS (
        SELECT type_desc, type
        FROM sys.procedures WITH(NOLOCK)
        WHERE NAME = 'removeConstraints'
            AND type = 'P'
      )
BEGIN
	 PRINT 'Proc exist with name dbo.removeConstraints'
     DROP PROCEDURE dbo.removeConstraints
	 PRINT 'Proc dropped dbo.removeConstraints'
END
GO
CREATE PROCEDURE dbo.removeConstraints
	-- Add the parameters for the stored procedure here
	@tablename nvarchar(100)
AS
BEGIN

  DECLARE @stmt VARCHAR(300);

  -- Cursor to generate ALTER TABLE DROP CONSTRAINT statements
  DECLARE cur CURSOR FOR
     SELECT 'ALTER TABLE ' + OBJECT_SCHEMA_NAME(parent_object_id) + '.' + OBJECT_NAME(parent_object_id) +
                    ' DROP CONSTRAINT ' + name
     FROM sys.foreign_keys
     WHERE OBJECT_SCHEMA_NAME(referenced_object_id) = 'dbo' AND
                OBJECT_NAME(referenced_object_id) = @tablename;

   OPEN cur;
   FETCH cur INTO @stmt;

   -- Drop each found foreign key constraint
   WHILE @@FETCH_STATUS = 0
     BEGIN
       EXEC (@stmt);
       FETCH cur INTO @stmt;
     END

  CLOSE cur;
  DEALLOCATE cur;

END
GO

EXEC dbo.removeConstraints 'x_tag_attr_def'
GO

EXEC dbo.removeConstraints 'x_tag_attr'
GO

EXEC dbo.removeConstraints 'x_service_resource_element'
GO

EXEC dbo.removeConstraints 'x_service_resource_element_val'
GO

EXIT
