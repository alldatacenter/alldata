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
IF (OBJECT_ID('x_policy_ref_resource_FK_policy_id') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_policy_ref_resource] DROP CONSTRAINT x_policy_ref_resource_FK_policy_id
END
GO
IF (OBJECT_ID('x_policy_ref_resource_FK_resource_def_id') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_policy_ref_resource] DROP CONSTRAINT x_policy_ref_resource_FK_resource_def_id
END
GO
IF (OBJECT_ID('x_policy_ref_resource_UK') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_ref_resource] DROP CONSTRAINT x_policy_ref_resource_UK
END
GO
IF (OBJECT_ID('x_policy_ref_access_type_FK_policy_id') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_policy_ref_access_type] DROP CONSTRAINT x_policy_ref_access_type_FK_policy_id
END
GO
IF (OBJECT_ID('x_policy_ref_access_type_FK_access_def_id') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_policy_ref_access_type] DROP CONSTRAINT x_policy_ref_access_type_FK_access_def_id
END
GO
IF (OBJECT_ID('x_policy_ref_access_type_UK') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_ref_access_type] DROP CONSTRAINT x_policy_ref_access_type_UK
END
GO
IF (OBJECT_ID('x_policy_ref_condition_FK_policy_id') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_policy_ref_condition] DROP CONSTRAINT x_policy_ref_condition_FK_policy_id
END
GO
IF (OBJECT_ID('x_policy_ref_condition_FK_condition_def_id') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_policy_ref_condition] DROP CONSTRAINT x_policy_ref_condition_FK_condition_def_id
END
GO
IF (OBJECT_ID('x_policy_ref_condition_UK') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_ref_condition] DROP CONSTRAINT x_policy_ref_condition_UK
END
GO
IF (OBJECT_ID('x_policy_ref_datamask_type_FK_policy_id') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_policy_ref_datamask_type] DROP CONSTRAINT x_policy_ref_datamask_type_FK_policy_id
END
GO
IF (OBJECT_ID('x_policy_ref_datamask_type_FK_datamask_def_id') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_policy_ref_datamask_type] DROP CONSTRAINT x_policy_ref_datamask_type_FK_datamask_def_id
END
GO
IF (OBJECT_ID('x_policy_ref_datamask_type_UK') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_ref_datamask_type] DROP CONSTRAINT x_policy_ref_datamask_type_UK
END
GO
IF (OBJECT_ID('x_policy_ref_user_FK_policy_id') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_policy_ref_user] DROP CONSTRAINT x_policy_ref_user_FK_policy_id
END
GO
IF (OBJECT_ID('x_policy_ref_user_FK_user_id') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_policy_ref_user] DROP CONSTRAINT x_policy_ref_user_FK_user_id
END
GO
IF (OBJECT_ID('x_policy_ref_user_UK') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_ref_user] DROP CONSTRAINT x_policy_ref_user_UK
END
GO
IF (OBJECT_ID('x_policy_ref_group_FK_policy_id') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_policy_ref_group] DROP CONSTRAINT x_policy_ref_group_FK_policy_id
END
GO
IF (OBJECT_ID('x_policy_ref_group_FK_group_id') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_policy_ref_group] DROP CONSTRAINT x_policy_ref_group_FK_group_id
END
GO
IF (OBJECT_ID('x_policy_ref_group_UK') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_ref_group] DROP CONSTRAINT x_policy_ref_group_UK
END
GO
IF (OBJECT_ID('x_policy_ref_group') IS NOT NULL)
BEGIN
  DROP TABLE [dbo].[x_policy_ref_group]
END
GO
IF (OBJECT_ID('x_policy_ref_user') IS NOT NULL)
BEGIN
  DROP TABLE [dbo].[x_policy_ref_user]
END
GO
IF (OBJECT_ID('x_policy_ref_datamask_type') IS NOT NULL)
BEGIN
  DROP TABLE [dbo].[x_policy_ref_datamask_type]
END
GO
IF (OBJECT_ID('x_policy_ref_condition') IS NOT NULL)
BEGIN
  DROP TABLE [dbo].[x_policy_ref_condition]
END
GO
IF (OBJECT_ID('x_policy_ref_access_type') IS NOT NULL)
BEGIN
  DROP TABLE [dbo].[x_policy_ref_access_type]
END
GO
IF (OBJECT_ID('x_policy_ref_resource') IS NOT NULL)
BEGIN
  DROP TABLE [dbo].[x_policy_ref_resource]
END
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_policy_ref_resource] (
  [id] [bigint] IDENTITY (1, 1) NOT NULL,
  [guid] [varchar](1024) DEFAULT NULL NULL,
  [create_time] [datetime2] DEFAULT NULL NULL,
  [update_time] [datetime2] DEFAULT NULL NULL,
  [added_by_id] [bigint] DEFAULT NULL NULL,
  [upd_by_id] [bigint] DEFAULT NULL NULL,
  [policy_id] [bigint] NOT NULL,
  [resource_def_id] [bigint] NOT NULL,
  [resource_name] [varchar](4000) DEFAULT NULL NULL,
  PRIMARY KEY CLUSTERED
  (
  [id] ASC
  ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
  CONSTRAINT [x_policy_ref_resource$x_policy_ref_resource_UK] UNIQUE NONCLUSTERED
  (
	[policy_id] ASC, [resource_def_id] ASC
  )WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_policy_ref_access_type] (
  [id] [bigint] IDENTITY (1, 1) NOT NULL,
  [guid] [varchar](1024) DEFAULT NULL NULL,
  [create_time] [datetime2] DEFAULT NULL NULL,
  [update_time] [datetime2] DEFAULT NULL NULL,
  [added_by_id] [bigint] DEFAULT NULL NULL,
  [upd_by_id] [bigint] DEFAULT NULL NULL,
  [policy_id] [bigint] NOT NULL,
  [access_def_id] [bigint] NOT NULL,
  [access_type_name] [varchar](4000) DEFAULT NULL NULL,
  PRIMARY KEY CLUSTERED
  (
  [id] ASC
  ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
    CONSTRAINT [x_policy_ref_access_type$x_policy_ref_access_type_UK] UNIQUE NONCLUSTERED
(
        [policy_id] ASC, [access_def_id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_policy_ref_condition] (
  [id] [bigint] IDENTITY (1, 1) NOT NULL,
  [guid] [varchar](1024) DEFAULT NULL NULL,
  [create_time] [datetime2] DEFAULT NULL NULL,
  [update_time] [datetime2] DEFAULT NULL NULL,
  [added_by_id] [bigint] DEFAULT NULL NULL,
  [upd_by_id] [bigint] DEFAULT NULL NULL,
  [policy_id] [bigint] NOT NULL,
  [condition_def_id] [bigint] NOT NULL,
  [condition_name] [varchar](4000) DEFAULT NULL NULL,
  PRIMARY KEY CLUSTERED
  (
  [id] ASC
  ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
    CONSTRAINT [x_policy_ref_condition$x_policy_ref_condition_UK] UNIQUE NONCLUSTERED
(
        [policy_id] ASC, [condition_def_id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_policy_ref_datamask_type] (
  [id] [bigint] IDENTITY (1, 1) NOT NULL,
  [guid] [varchar](1024) DEFAULT NULL NULL,
  [create_time] [datetime2] DEFAULT NULL NULL,
  [update_time] [datetime2] DEFAULT NULL NULL,
  [added_by_id] [bigint] DEFAULT NULL NULL,
  [upd_by_id] [bigint] DEFAULT NULL NULL,
  [policy_id] [bigint] NOT NULL,
  [datamask_def_id] [bigint] NOT NULL,
  [datamask_type_name] [varchar](4000) DEFAULT NULL NULL,
  PRIMARY KEY CLUSTERED
  (
  [id] ASC
  ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
    CONSTRAINT [x_policy_ref_datamask_type$x_policy_ref_datamask_type_UK] UNIQUE NONCLUSTERED
(
        [policy_id] ASC, [datamask_def_id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_policy_ref_user] (
  [id] [bigint] IDENTITY (1, 1) NOT NULL,
  [guid] [varchar](1024) DEFAULT NULL NULL,
  [create_time] [datetime2] DEFAULT NULL NULL,
  [update_time] [datetime2] DEFAULT NULL NULL,
  [added_by_id] [bigint] DEFAULT NULL NULL,
  [upd_by_id] [bigint] DEFAULT NULL NULL,
  [policy_id] [bigint] NOT NULL,
  [user_id] [bigint] NOT NULL,
  [user_name] [varchar](4000) DEFAULT NULL NULL,
  PRIMARY KEY CLUSTERED
  (
  [id] ASC
  ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
    CONSTRAINT [x_policy_ref_user$x_policy_ref_user_UK] UNIQUE NONCLUSTERED
(
        [policy_id] ASC, [user_id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_policy_ref_group] (
  [id] [bigint] IDENTITY (1, 1) NOT NULL,
  [guid] [varchar](1024) DEFAULT NULL NULL,
  [create_time] [datetime2] DEFAULT NULL NULL,
  [update_time] [datetime2] DEFAULT NULL NULL,
  [added_by_id] [bigint] DEFAULT NULL NULL,
  [upd_by_id] [bigint] DEFAULT NULL NULL,
  [policy_id] [bigint] NOT NULL,
  [group_id] [bigint] NOT NULL,
  [group_name] [varchar](4000) DEFAULT NULL NULL,
  PRIMARY KEY CLUSTERED
  (
  [id] ASC
  ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
    CONSTRAINT [x_policy_ref_group$x_policy_ref_group_UK] UNIQUE NONCLUSTERED
(
        [policy_id] ASC, [group_id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
ALTER TABLE [dbo].[x_policy_ref_resource] WITH CHECK ADD CONSTRAINT [x_policy_ref_resource_FK_policy_id] FOREIGN KEY ([policy_id])
REFERENCES [dbo].[x_policy] ([id])
ALTER TABLE [dbo].[x_policy_ref_resource] CHECK CONSTRAINT [x_policy_ref_resource_FK_policy_id]
ALTER TABLE [dbo].[x_policy_ref_resource] WITH CHECK ADD CONSTRAINT [x_policy_ref_resource_FK_resource_def_id] FOREIGN KEY ([resource_def_id])
REFERENCES [dbo].[x_resource_def] ([id])
ALTER TABLE [dbo].[x_policy_ref_resource] CHECK CONSTRAINT [x_policy_ref_resource_FK_resource_def_id]
ALTER TABLE [dbo].[x_policy_ref_resource] WITH CHECK ADD CONSTRAINT [x_policy_ref_resource_FK_added_by] FOREIGN KEY ([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_ref_resource] CHECK CONSTRAINT [x_policy_ref_resource_FK_added_by]
ALTER TABLE [dbo].[x_policy_ref_resource] WITH CHECK ADD CONSTRAINT [x_policy_ref_resource_FK_upd_by] FOREIGN KEY ([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_ref_access_type] WITH CHECK ADD CONSTRAINT [x_policy_ref_access_type_FK_policy_id] FOREIGN KEY ([policy_id])
REFERENCES [dbo].[x_policy] ([id])
ALTER TABLE [dbo].[x_policy_ref_access_type] CHECK CONSTRAINT [x_policy_ref_access_type_FK_policy_id]
ALTER TABLE [dbo].[x_policy_ref_access_type] WITH CHECK ADD CONSTRAINT [x_policy_ref_access_type_FK_access_def_id] FOREIGN KEY ([access_def_id])
REFERENCES [dbo].[x_access_type_def] ([id])
ALTER TABLE [dbo].[x_policy_ref_access_type] CHECK CONSTRAINT [x_policy_ref_access_type_FK_access_def_id]
ALTER TABLE [dbo].[x_policy_ref_access_type] WITH CHECK ADD CONSTRAINT [x_policy_ref_access_type_FK_added_by] FOREIGN KEY ([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_ref_access_type] CHECK CONSTRAINT [x_policy_ref_access_type_FK_added_by]
ALTER TABLE [dbo].[x_policy_ref_access_type] WITH CHECK ADD CONSTRAINT [x_policy_ref_access_type_FK_upd_by] FOREIGN KEY ([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_ref_condition] WITH CHECK ADD CONSTRAINT [x_policy_ref_condition_FK_policy_id] FOREIGN KEY ([policy_id])
REFERENCES [dbo].[x_policy] ([id])
ALTER TABLE [dbo].[x_policy_ref_condition] CHECK CONSTRAINT [x_policy_ref_condition_FK_policy_id]
ALTER TABLE [dbo].[x_policy_ref_condition] WITH CHECK ADD CONSTRAINT [x_policy_ref_condition_FK_condition_def_id] FOREIGN KEY ([condition_def_id])
REFERENCES [dbo].[x_policy_condition_def] ([id])
ALTER TABLE [dbo].[x_policy_ref_condition] CHECK CONSTRAINT [x_policy_ref_condition_FK_condition_def_id]
ALTER TABLE [dbo].[x_policy_ref_condition] WITH CHECK ADD CONSTRAINT [x_policy_ref_condition_FK_added_by] FOREIGN KEY ([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_ref_condition] CHECK CONSTRAINT [x_policy_ref_condition_FK_added_by]
ALTER TABLE [dbo].[x_policy_ref_condition] WITH CHECK ADD CONSTRAINT [x_policy_ref_condition_FK_upd_by] FOREIGN KEY ([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_ref_datamask_type] WITH CHECK ADD CONSTRAINT [x_policy_ref_datamask_type_FK_policy_id] FOREIGN KEY ([policy_id])
REFERENCES [dbo].[x_policy] ([id])
ALTER TABLE [dbo].[x_policy_ref_datamask_type] CHECK CONSTRAINT [x_policy_ref_datamask_type_FK_policy_id]
ALTER TABLE [dbo].[x_policy_ref_datamask_type] WITH CHECK ADD CONSTRAINT [x_policy_ref_datamask_type_FK_datamask_def_id] FOREIGN KEY ([datamask_def_id])
REFERENCES [dbo].[x_datamask_type_def] ([id])
ALTER TABLE [dbo].[x_policy_ref_datamask_type] CHECK CONSTRAINT [x_policy_ref_datamask_type_FK_datamask_def_id]
ALTER TABLE [dbo].[x_policy_ref_datamask_type] WITH CHECK ADD CONSTRAINT [x_policy_ref_datamask_type_FK_added_by] FOREIGN KEY ([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_ref_datamask_type] CHECK CONSTRAINT [x_policy_ref_datamask_type_FK_added_by]
ALTER TABLE [dbo].[x_policy_ref_datamask_type] WITH CHECK ADD CONSTRAINT [x_policy_ref_datamask_type_FK_upd_by] FOREIGN KEY ([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])


ALTER TABLE [dbo].[x_policy_ref_user] WITH CHECK ADD CONSTRAINT [x_policy_ref_user_FK_policy_id] FOREIGN KEY ([policy_id])
REFERENCES [dbo].[x_policy] ([id])
ALTER TABLE [dbo].[x_policy_ref_user] CHECK CONSTRAINT [x_policy_ref_user_FK_policy_id]

ALTER TABLE [dbo].[x_policy_ref_user] WITH CHECK ADD CONSTRAINT [x_policy_ref_user_FK_user_id] FOREIGN KEY ([user_id])
REFERENCES [dbo].[x_user] ([id])
ALTER TABLE [dbo].[x_policy_ref_user] CHECK CONSTRAINT [x_policy_ref_user_FK_user_id]

ALTER TABLE [dbo].[x_policy_ref_user] WITH CHECK ADD CONSTRAINT [x_policy_ref_user_FK_added_by] FOREIGN KEY ([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_ref_user] CHECK CONSTRAINT [x_policy_ref_user_FK_added_by]

ALTER TABLE [dbo].[x_policy_ref_user] WITH CHECK ADD CONSTRAINT [x_policy_ref_user_FK_upd_by] FOREIGN KEY ([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_ref_user] CHECK CONSTRAINT [x_policy_ref_user_FK_upd_by]

ALTER TABLE [dbo].[x_policy_ref_group] WITH CHECK ADD CONSTRAINT [x_policy_ref_group_FK_policy_id] FOREIGN KEY ([policy_id])
REFERENCES [dbo].[x_policy] ([id])
ALTER TABLE [dbo].[x_policy_ref_group] CHECK CONSTRAINT [x_policy_ref_group_FK_policy_id]
ALTER TABLE [dbo].[x_policy_ref_group] WITH CHECK ADD CONSTRAINT [x_policy_ref_group_FK_group_id] FOREIGN KEY ([group_id])
REFERENCES [dbo].[x_group] ([id])
ALTER TABLE [dbo].[x_policy_ref_group] CHECK CONSTRAINT [x_policy_ref_group_FK_group_id]
ALTER TABLE [dbo].[x_policy_ref_group] WITH CHECK ADD CONSTRAINT [x_policy_ref_group_FK_added_by] FOREIGN KEY ([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_ref_group] CHECK CONSTRAINT [x_policy_ref_group_FK_added_by]
ALTER TABLE [dbo].[x_policy_ref_group] WITH CHECK ADD CONSTRAINT [x_policy_ref_group_FK_upd_by] FOREIGN KEY ([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
IF NOT EXISTS (SELECT
    *
  FROM INFORMATION_SCHEMA.columns
  WHERE table_name = 'x_policy'
  AND column_name = 'policy_text')
BEGIN
  ALTER TABLE [dbo].[x_policy] ADD [policy_text]  [nvarchar](max)  DEFAULT NULL NULL;
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

EXEC dbo.removeConstraints 'x_policy_item'
GO
EXEC dbo.removeConstraints 'x_policy_item_access'
GO
EXEC dbo.removeConstraints 'x_policy_item_condition'
GO
EXEC dbo.removeConstraints 'x_policy_item_datamask'
GO
EXEC dbo.removeConstraints 'x_policy_item_group_perm'
GO
EXEC dbo.removeConstraints 'x_policy_item_user_perm'
GO
EXEC dbo.removeConstraints 'x_policy_item_rowfilter'
GO
EXEC dbo.removeConstraints 'x_policy_resource'
GO
EXEC dbo.removeConstraints 'x_policy_resource_map'
GO

IF EXISTS (select version from x_db_version_h where version = 'J10013')
BEGIN
	IF EXISTS(select name from x_service_def where name like 'atlas.%')
	BEGIN
		DECLARE @new_atlas_def_name VARCHAR(100);
		set @new_atlas_def_name=(select name from x_service_def where name like 'atlas.%')
		IF EXISTS(select * from x_access_type_def where def_id in(select id from x_service_def where name='tag') and name in('atlas:read','atlas:create','atlas:update','atlas:delete','atlas:all'))
		BEGIN
			update x_access_type_def set name=concat(@new_atlas_def_name , ':read') where def_id=100 and name='atlas:read';
			update x_access_type_def set name=concat(@new_atlas_def_name , ':create') where def_id=100 and name='atlas:create';
			update x_access_type_def set name=concat(@new_atlas_def_name , ':update') where def_id=100 and name='atlas:update';
			update x_access_type_def set name=concat(@new_atlas_def_name , ':delete') where def_id=100 and name='atlas:delete';
			update x_access_type_def set name=concat(@new_atlas_def_name , ':all') where def_id=100 and name='atlas:all';
		END
		IF EXISTS(select * from x_access_type_def_grants where atd_id in (select id from x_access_type_def where def_id in (select id from x_service_def where name='tag') and name like 'atlas%') and implied_grant in ('atlas:read','atlas:create','atlas:update','atlas:delete','atlas:all'))
		BEGIN
			update x_access_type_def_grants set implied_grant=concat(@new_atlas_def_name , ':read') where implied_grant='atlas:read';
			update x_access_type_def_grants set implied_grant=concat(@new_atlas_def_name , ':create') where implied_grant='atlas:create';
			update x_access_type_def_grants set implied_grant=concat(@new_atlas_def_name , ':update') where implied_grant='atlas:update';
			update x_access_type_def_grants set implied_grant=concat(@new_atlas_def_name , ':delete') where implied_grant='atlas:delete';
			update x_access_type_def_grants set implied_grant=concat(@new_atlas_def_name , ':all') where implied_grant='atlas:all';
		END
	END
END
GO
EXIT