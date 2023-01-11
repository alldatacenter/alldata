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

DECLARE @SQL nvarchar(4000),
  @TableName sysname,
  @ColumnName sysname,
  @DefaultConstraint varchar(256),
  @SchemaName sysname,
  @Definition varchar(256),
  @Return int

-- Cursor of all default constraints in the current database
DECLARE cursObjects CURSOR FAST_FORWARD
FOR
SELECT object_name(d.parent_object_id) TableName, col_name(parent_object_id, parent_column_id) ColumnName, d.name DefaultConstraint, schema_name(d.schema_id) SchemaName, definition FROM sys.default_constraints d INNER JOIN sys.columns c ON d.parent_column_id = c.column_id WHERE d.is_system_named = 1

OPEN cursObjects

-- get the first row
FETCH NEXT FROM cursObjects
 INTO @TableName, @ColumnName, @DefaultConstraint, @SchemaName, @Definition

-- Set the return code to 0
SET @Return = 0

-- start a transaction
BEGIN TRAN

-- Cycle through the rows of the cursor to change default constraint name
WHILE ((@@FETCH_STATUS = 0) AND (@Return = 0))
 BEGIN
  -- Drop current constraint
  SET @SQL = 'alter table '+@SchemaName+'.'+@TableName+' drop constraint ' + @DefaultConstraint
  EXEC @Return = sp_executesql @SQL

 -- Create constraint
  SET @SQL = 'alter table '+@SchemaName+'.'+@TableName+' add constraint df_'+@TableName+'_'+@ColumnName+' default

'+@Definition+' for '+@ColumnName
  EXEC @Return = sp_executesql @SQL

  -- Get the next row
  FETCH NEXT FROM cursObjects
   INTO @TableName, @ColumnName, @DefaultConstraint, @SchemaName, @Definition
 END

-- Close cursor
CLOSE cursObjects
DEALLOCATE cursObjects

-- Check to see if the WHILE loop exited with an error.
IF (@Return = 0)
    COMMIT TRAN
ELSE
  BEGIN
    ROLLBACK TRAN
    SET @SQL = 'Error encountered in ['+ @SchemaName + '].[' + @Tablename + '].[' + @ColumnName + ']'
    RAISERROR(@SQL, 16, 1)
  END
GO


GO
IF (OBJECT_ID('x_group$x_group_UK_group_name') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_group] DROP CONSTRAINT x_group$x_group_UK_group_name;
END
GO
  ALTER TABLE [dbo].[x_group] ALTER COLUMN group_name nvarchar(767);
  ALTER TABLE [dbo].[x_group] ADD CONSTRAINT  [x_group$x_group_UK_group_name] UNIQUE (group_name);
  ALTER TABLE [dbo].[x_group] ALTER COLUMN descr nvarchar(4000);
GO

IF (OBJECT_ID('x_group_users$x_group_users_UK_uid_gname') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_group_users] DROP CONSTRAINT [x_group_users$x_group_users_UK_uid_gname];
END
GO
  ALTER TABLE [dbo].[x_group_users] ALTER COLUMN group_name nvarchar(767);
  ALTER TABLE [dbo].[x_group_users] ADD CONSTRAINT  [x_group_users$x_group_users_UK_uid_gname] UNIQUE (user_id,group_name);
  ALTER TABLE [dbo].[x_group_groups] ALTER COLUMN group_name nvarchar(1024);
GO

IF (OBJECT_ID('df_x_security_zone_ref_group_group_name') IS NOT NULL)
BEGIN
 ALTER TABLE [dbo].[x_security_zone_ref_group] DROP CONSTRAINT df_x_security_zone_ref_group_group_name;
END
GO
 ALTER TABLE [dbo].[x_security_zone_ref_group] ALTER COLUMN group_name nvarchar(767);
 ALTER TABLE [dbo].[x_security_zone_ref_group] ADD CONSTRAINT df_x_security_zone_ref_group_group_name DEFAULT NULL FOR group_name ;
GO

IF (OBJECT_ID('df_x_policy_ref_group_group_name') IS NOT NULL)
BEGIN
 ALTER TABLE [dbo].[x_policy_ref_group] DROP CONSTRAINT df_x_policy_ref_group_group_name;
END
GO
 ALTER TABLE [dbo].[x_policy_ref_group] ALTER COLUMN group_name nvarchar(4000);
 ALTER TABLE [dbo].[x_policy_ref_group] ADD CONSTRAINT df_x_policy_ref_group_group_name DEFAULT NULL FOR group_name;
GO

IF (OBJECT_ID('df_x_role_ref_group_group_name') IS NOT NULL)
BEGIN
 ALTER TABLE [dbo].[x_role_ref_group] DROP CONSTRAINT df_x_role_ref_group_group_name;
END
GO
 ALTER TABLE [dbo].[x_role_ref_group] ALTER COLUMN group_name nvarchar(767);
 ALTER TABLE [dbo].[x_role_ref_group] ADD CONSTRAINT df_x_role_ref_group_group_name DEFAULT NULL FOR group_name ;
GO

IF (OBJECT_ID('x_portal_user$x_portal_user_UK_login_id') IS NOT NULL)
BEGIN
 ALTER TABLE [dbo].[x_portal_user] DROP CONSTRAINT x_portal_user$x_portal_user_UK_login_id;
END
GO
IF (OBJECT_ID('df_x_portal_user_login_id') IS NOT NULL)
BEGIN
 ALTER TABLE [dbo].[x_portal_user] DROP CONSTRAINT df_x_portal_user_login_id;
END
GO
 ALTER TABLE [dbo].[x_portal_user] ALTER COLUMN login_id nvarchar(767);
 ALTER TABLE [dbo].[x_portal_user] ADD CONSTRAINT df_x_portal_user_login_id DEFAULT NULL FOR login_id ;
 ALTER TABLE [dbo].[x_portal_user] ADD CONSTRAINT [x_portal_user$x_portal_user_UK_login_id] UNIQUE (login_id) ;
GO

IF (OBJECT_ID('df_x_portal_user_first_name') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_portal_user] DROP CONSTRAINT df_x_portal_user_first_name;
END
GO
IF (OBJECT_ID('df_x_portal_user_last_name') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_portal_user] DROP CONSTRAINT df_x_portal_user_last_name;
END
GO
IF EXISTS(SELECT * FROM sys.indexes WHERE name = 'x_portal_user_name' AND object_id = OBJECT_ID('x_portal_user'))
BEGIN
  DROP INDEX [x_portal_user_name] ON [dbo].[x_portal_user];
END
GO
 ALTER TABLE [dbo].[x_portal_user] ALTER COLUMN first_name nvarchar(767);
 ALTER TABLE [dbo].[x_portal_user] ADD CONSTRAINT df_x_portal_user_first_name DEFAULT NULL FOR first_name ;
 CREATE NONCLUSTERED INDEX [x_portal_user_name] ON [x_portal_user] ([first_name] ASC) WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY];
 ALTER TABLE [dbo].[x_portal_user] ALTER COLUMN last_name nvarchar(767);
 ALTER TABLE [dbo].[x_portal_user] ADD CONSTRAINT df_x_portal_user_last_name DEFAULT NULL FOR last_name ;
GO
IF (OBJECT_ID('x_sz_ref_user_FK_user_name') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_security_zone_ref_user] DROP CONSTRAINT x_sz_ref_user_FK_user_name;
END
GO
IF (OBJECT_ID('df_x_security_zone_ref_user_user_name') IS NOT NULL)
BEGIN
 ALTER TABLE [dbo].[x_security_zone_ref_user] DROP CONSTRAINT df_x_security_zone_ref_user_user_name;
END
GO
IF (OBJECT_ID('x_user$x_user_UK_user_name') IS NOT NULL)
BEGIN
 ALTER TABLE [dbo].[x_user] DROP CONSTRAINT x_user$x_user_UK_user_name;
END
GO
 ALTER TABLE [dbo].[x_user] ALTER COLUMN user_name nvarchar(767);
 ALTER TABLE [dbo].[x_user] ADD CONSTRAINT [x_user$x_user_UK_user_name] UNIQUE (user_name);
 ALTER TABLE [dbo].[x_security_zone_ref_user] ALTER COLUMN user_name nvarchar(767);
 ALTER TABLE [dbo].[x_security_zone_ref_user] ADD CONSTRAINT df_x_security_zone_ref_user_user_name DEFAULT NULL FOR user_name ;
 ALTER TABLE [dbo].[x_security_zone_ref_user] WITH CHECK ADD CONSTRAINT [x_sz_ref_user_FK_user_name] FOREIGN KEY([user_name]) REFERENCES [dbo].[x_user] ([user_name]);
 ALTER TABLE [dbo].[x_user] ALTER COLUMN descr nvarchar(4000);
GO
IF EXISTS(SELECT * FROM sys.indexes WHERE name = 'x_ugsync_audit_info_uname' AND object_id = OBJECT_ID('x_ugsync_audit_info'))
BEGIN
 DROP INDEX  x_ugsync_audit_info_uname ON [dbo].[x_ugsync_audit_info];
END
GO
 ALTER TABLE [dbo].[x_ugsync_audit_info] ALTER COLUMN user_name nvarchar(767);
 CREATE NONCLUSTERED INDEX [x_ugsync_audit_info_uname] ON [x_ugsync_audit_info] ([user_name] ASC ) 
 WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY];
GO

IF (OBJECT_ID('df_x_policy_ref_user_user_name') IS NOT NULL)
BEGIN
 ALTER TABLE [dbo].[x_policy_ref_user] DROP CONSTRAINT df_x_policy_ref_user_user_name;
END
GO
 ALTER TABLE [dbo].[x_policy_ref_user] ALTER COLUMN user_name nvarchar(4000);
 ALTER TABLE [dbo].[x_policy_ref_user] ADD CONSTRAINT df_x_policy_ref_user_user_name DEFAULT NULL FOR user_name ;
GO

IF (OBJECT_ID('df_x_role_ref_user_user_name') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_role_ref_user] DROP CONSTRAINT df_x_role_ref_user_user_name;
END
GO
 ALTER TABLE [dbo].[x_role_ref_user] ALTER COLUMN user_name nvarchar(767);
 ALTER TABLE [dbo].[x_role_ref_user] ADD CONSTRAINT df_x_role_ref_user_user_name DEFAULT NULL FOR user_name ;
GO

IF (OBJECT_ID('x_role$x_role_UK_name') IS NOT NULL)
BEGIN
 ALTER TABLE [dbo].[x_role] DROP CONSTRAINT x_role$x_role_UK_name;
END
GO

IF (OBJECT_ID('df_x_role_description') IS NOT NULL)
BEGIN
 ALTER TABLE [dbo].[x_role] DROP CONSTRAINT df_x_role_description;
END
GO
 ALTER TABLE [dbo].[x_role] ALTER COLUMN name nvarchar(255);
 ALTER TABLE [dbo].[x_role] ADD CONSTRAINT [x_role$x_role_UK_name] UNIQUE (name);
 ALTER TABLE [dbo].[x_role] ALTER COLUMN description nvarchar(1024);
 ALTER TABLE [dbo].[x_role] ADD CONSTRAINT df_x_role_description DEFAULT NULL FOR description ;
GO

IF (OBJECT_ID('df_x_policy_ref_role_role_name') IS NOT NULL)
BEGIN
 ALTER TABLE [dbo].[x_policy_ref_role] DROP CONSTRAINT df_x_policy_ref_role_role_name;
END
GO
 ALTER TABLE [dbo].[x_policy_ref_role] ALTER COLUMN role_name nvarchar(255);
 ALTER TABLE [dbo].[x_policy_ref_role] ADD CONSTRAINT df_x_policy_ref_role_role_name DEFAULT NULL FOR role_name ;
GO
IF (OBJECT_ID('df_x_role_ref_role_role_name') IS NOT NULL)
BEGIN
 ALTER TABLE [dbo].[x_role_ref_role] DROP CONSTRAINT df_x_role_ref_role_role_name;
END
GO
 ALTER TABLE [dbo].[x_role_ref_role] ALTER COLUMN role_name nvarchar(255);
 ALTER TABLE [dbo].[x_role_ref_role] ADD CONSTRAINT df_x_role_ref_role_role_name DEFAULT NULL FOR role_name ;
GO
exit
