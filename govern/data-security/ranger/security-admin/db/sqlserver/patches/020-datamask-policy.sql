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
IF (OBJECT_ID('x_datamask_type_def_FK_def_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_datamask_type_def] DROP CONSTRAINT x_datamask_type_def_FK_def_id
END
GO
IF (OBJECT_ID('x_datamask_type_def_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_datamask_type_def] DROP CONSTRAINT x_datamask_type_def_FK_added_by_id
END
GO
IF (OBJECT_ID('x_datamask_type_def_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_datamask_type_def] DROP CONSTRAINT x_datamask_type_def_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_policy_item_datamask_FK_policy_item_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_datamask] DROP CONSTRAINT x_policy_item_datamask_FK_policy_item_id
END
GO
IF (OBJECT_ID('x_policy_item_datamask_FK_type') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_datamask] DROP CONSTRAINT x_policy_item_datamask_FK_type
END
GO
IF (OBJECT_ID('x_policy_item_datamask_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_datamask] DROP CONSTRAINT x_policy_item_datamask_FK_added_by_id
END
GO
IF (OBJECT_ID('x_policy_item_datamask_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_datamask] DROP CONSTRAINT x_policy_item_datamask_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_policy_item_rowfilter_FK_policy_item_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_rowfilter] DROP CONSTRAINT x_policy_item_rowfilter_FK_policy_item_id
END
GO
IF (OBJECT_ID('x_policy_item_rowfilter_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_rowfilter] DROP CONSTRAINT x_policy_item_rowfilter_FK_added_by_id
END
GO
IF (OBJECT_ID('x_policy_item_rowfilter_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_rowfilter] DROP CONSTRAINT x_policy_item_rowfilter_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_policy_item_rowfilter') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_item_rowfilter]
END
GO
IF (OBJECT_ID('x_policy_item_datamask') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_item_datamask]
END
GO
IF (OBJECT_ID('x_datamask_type_def') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_datamask_type_def]
END
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_datamask_type_def](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[guid] [varchar](64) DEFAULT NULL NULL,
	[create_time] [datetime2] DEFAULT NULL NULL,
	[update_time] [datetime2] DEFAULT NULL NULL,
	[added_by_id] [bigint] DEFAULT NULL NULL,
	[upd_by_id] [bigint] DEFAULT NULL NULL,
	[def_id] [bigint] NOT NULL,
	[item_id] [bigint] NOT NULL,
	[name] [varchar](1024) NOT NULL,
	[label] [varchar](1024) NOT NULL,
	[description] [varchar](1024) DEFAULT NULL NULL,
	[transformer] [varchar](1024) DEFAULT NULL NULL,
	[datamask_options] [varchar](1024) DEFAULT NULL NULL,
	[rb_key_label] [varchar](1024) DEFAULT NULL NULL,
	[rb_key_description] [varchar](1024) DEFAULT NULL NULL,
	[sort_order] [int] DEFAULT 0 NULL,
	PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
CREATE TABLE [dbo].[x_policy_item_datamask](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[guid] [varchar](64) DEFAULT NULL NULL,
	[create_time] [datetime2] DEFAULT NULL NULL,
	[update_time] [datetime2] DEFAULT NULL NULL,
	[added_by_id] [bigint] DEFAULT NULL NULL,
	[upd_by_id] [bigint] DEFAULT NULL NULL,
	[policy_item_id] [bigint] NOT NULL,
	[type] [bigint] NOT NULL,
	[condition_expr] [varchar](1024) DEFAULT NULL NULL,
	[value_expr] [varchar](1024) DEFAULT NULL NULL,
	PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
CREATE TABLE [dbo].[x_policy_item_rowfilter](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[guid] [varchar](64) DEFAULT NULL NULL,
	[create_time] [datetime2] DEFAULT NULL NULL,
	[update_time] [datetime2] DEFAULT NULL NULL,
	[added_by_id] [bigint] DEFAULT NULL NULL,
	[upd_by_id] [bigint] DEFAULT NULL NULL,
	[policy_item_id] [bigint] NOT NULL,
	[filter_expr] [varchar](1024) DEFAULT NULL NULL,
	PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
ALTER TABLE [dbo].[x_datamask_type_def] WITH CHECK ADD CONSTRAINT [x_datamask_type_def_FK_def_id] FOREIGN KEY([def_id]) REFERENCES [dbo].[x_service_def] ([id])
GO
ALTER TABLE [dbo].[x_datamask_type_def] WITH CHECK ADD CONSTRAINT [x_datamask_type_def_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_datamask_type_def] WITH CHECK ADD CONSTRAINT [x_datamask_type_def_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_item_datamask] WITH CHECK ADD CONSTRAINT [x_policy_item_datamask_FK_policy_item_id] FOREIGN KEY([policy_item_id]) REFERENCES [dbo].[x_policy_item] ([id])
GO
ALTER TABLE [dbo].[x_policy_item_datamask] WITH CHECK ADD CONSTRAINT [x_policy_item_datamask_FK_type] FOREIGN KEY([type]) REFERENCES [dbo].[x_datamask_type_def] ([id])
GO
ALTER TABLE [dbo].[x_policy_item_datamask] WITH CHECK ADD CONSTRAINT [x_policy_item_datamask_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_item_datamask] WITH CHECK ADD CONSTRAINT [x_policy_item_datamask_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_item_rowfilter] WITH CHECK ADD CONSTRAINT [x_policy_item_rowfilter_FK_policy_item_id] FOREIGN KEY([policy_item_id]) REFERENCES [dbo].[x_policy_item] ([id])
GO
ALTER TABLE [dbo].[x_policy_item_rowfilter] WITH CHECK ADD CONSTRAINT [x_policy_item_rowfilter_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_item_rowfilter] WITH CHECK ADD CONSTRAINT [x_policy_item_rowfilter_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
CREATE NONCLUSTERED INDEX [x_datamask_type_def_IDX_def_id] ON [x_datamask_type_def]
(
   [def_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_policy_item_datamask_IDX_policy_item_id] ON [x_policy_item_datamask]
(
   [policy_item_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_policy_item_rowfilter_IDX_policy_item_id] ON [x_policy_item_rowfilter]
(
   [policy_item_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
-- add datamask_options column in x_access_type_def table if not exist
IF NOT EXISTS(select * from INFORMATION_SCHEMA.columns where table_name = 'x_access_type_def' and column_name in('datamask_options','rowfilter_options'))
BEGIN
	ALTER TABLE [dbo].[x_access_type_def] ADD [datamask_options] [varchar](1024) DEFAULT NULL NULL,[rowfilter_options] [varchar](1024) DEFAULT NULL NULL;
END
GO
-- add datamask_options column in x_resource_def table if not exist
IF NOT EXISTS(select * from INFORMATION_SCHEMA.columns where table_name = 'x_resource_def' and column_name in ('datamask_options','rowfilter_options'))
BEGIN
	ALTER TABLE [dbo].[x_resource_def] ADD [datamask_options] [varchar](1024) DEFAULT NULL NULL,[rowfilter_options] [varchar](1024) DEFAULT NULL NULL;
END
GO

exit