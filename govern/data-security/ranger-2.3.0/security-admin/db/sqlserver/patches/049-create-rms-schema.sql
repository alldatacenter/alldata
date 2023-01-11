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
IF (OBJECT_ID('x_rms_service_res_FK_service_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_rms_service_resource] DROP CONSTRAINT x_rms_service_res_FK_service_id
END
GO
IF (OBJECT_ID('x_rms_notification_FK_hl_service_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_rms_notification] DROP CONSTRAINT x_rms_notification_FK_hl_service_id
END
GO
IF (OBJECT_ID('x_rms_notification_FK_ll_service_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_rms_notification] DROP CONSTRAINT x_rms_notification_FK_ll_service_id
END
GO
IF (OBJECT_ID('x_rms_res_map_FK_hl_res_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_rms_resource_mapping] DROP CONSTRAINT x_rms_res_map_FK_hl_res_id
END
GO
IF (OBJECT_ID('x_rms_res_map_FK_ll_res_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_rms_resource_mapping] DROP CONSTRAINT x_rms_res_map_FK_ll_res_id
END
GO
IF (OBJECT_ID('x_rms_notification') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_rms_notification]
END
GO
IF (OBJECT_ID('x_rms_resource_mapping') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_rms_resource_mapping]
END
GO
IF (OBJECT_ID('x_rms_mapping_provider') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_rms_mapping_provider]
END
GO
IF (OBJECT_ID('x_rms_service_resource') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_rms_service_resource]
END
GO


SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_rms_service_resource](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](64) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [version] [bigint] DEFAULT NULL NULL,
        [service_id] [bigint] NOT NULL,
        [resource_signature] [varchar](128) DEFAULT NULL NULL,
        [is_enabled] [tinyint] DEFAULT 1 NOT NULL,
        [service_resource_elements_text] [nvarchar](max) DEFAULT NULL NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
CONSTRAINT [x_rms_service_resource$x_service_res_UK_guid] UNIQUE NONCLUSTERED
(
        [guid] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
ALTER TABLE [dbo].[x_rms_service_resource] WITH CHECK ADD CONSTRAINT [x_rms_service_res_FK_service_id] FOREIGN KEY([service_id]) REFERENCES [dbo].[x_service] ([id])
GO
CREATE NONCLUSTERED INDEX [x_rms_service_resource_IDX_service_id] ON [x_rms_service_resource]
(
   [service_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO


SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_rms_notification](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [hms_name] [VARCHAR](128) NULL DEFAULT NULL,
        [notification_id] [bigint] NULL DEFAULT NULL,
        [change_timestamp] [datetime2] DEFAULT NULL NULL,
        [change_type] [VARCHAR](64) NULL DEFAULT  NULL,
        [hl_resource_id] [bigint] NULL DEFAULT NULL,
        [hl_service_id] [bigint] NULL DEFAULT NULL,
        [ll_resource_id] [bigint] NULL DEFAULT NULL,
        [ll_service_id] [bigint] NULL DEFAULT NULL,
        
PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
ALTER TABLE [dbo].[x_rms_notification]  WITH CHECK ADD CONSTRAINT [x_rms_notification_FK_hl_service_id] FOREIGN KEY([hl_service_id])
REFERENCES [dbo].[x_service] ([id])
ALTER TABLE [dbo].[x_rms_notification]  WITH CHECK ADD CONSTRAINT [x_rms_notification_FK_ll_service_id] FOREIGN KEY([ll_service_id])
REFERENCES [dbo].[x_service] ([id])
GO
CREATE NONCLUSTERED INDEX [x_rms_notification_IDX_notification_id] ON [x_rms_notification]
(
   [notification_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_rms_notification_IDX_hms_name_notification_id] ON [x_rms_notification]
(
   [hms_name] ASC,[notification_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_rms_notification_IDX_hl_service_id] ON [x_rms_notification]
(
   [hl_service_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_rms_notification_IDX_ll_service_id] ON [x_rms_notification]
(
   [ll_service_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
GO

CREATE TABLE [dbo].[x_rms_resource_mapping](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [change_timestamp] [datetime2] DEFAULT NULL NULL,
        [hl_resource_id] [bigint] NOT NULL,
        [ll_resource_id] [bigint] NOT NULL,
PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [x_rms_res_map_UK_hl_res_id_ll_res_id] UNIQUE NONCLUSTERED
(
        [hl_resource_id] ASC, [ll_resource_id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
ALTER TABLE [dbo].[x_rms_resource_mapping]  WITH CHECK ADD CONSTRAINT [x_rms_res_map_FK_hl_res_id] FOREIGN KEY([hl_resource_id])
REFERENCES [dbo].[x_rms_service_resource] ([id])
ALTER TABLE [dbo].[x_rms_resource_mapping]  WITH CHECK ADD CONSTRAINT [x_rms_res_map_FK_ll_res_id] FOREIGN KEY([ll_resource_id])
REFERENCES [dbo].[x_rms_service_resource] ([id])
GO
CREATE NONCLUSTERED INDEX [x_rms_resource_mapping_IDX_hl_resource_id] ON [x_rms_resource_mapping]
(
   [hl_resource_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_rms_resource_mapping_IDX_ll_resource_id] ON [x_rms_resource_mapping]
(
   [ll_resource_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
GO

CREATE TABLE [dbo].[x_rms_mapping_provider](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [change_timestamp] [datetime2] DEFAULT NULL NULL,
        [name] [VARCHAR](128) NOT NULL,
        [last_known_version] [bigint] NOT NULL,
PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [x_rms_mapping_provider_UK_name] UNIQUE NONCLUSTERED
(
        [name] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO

exit
