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
IF (OBJECT_ID('x_policy_label_map') IS NOT NULL)
BEGIN
        DROP TABLE [dbo].[x_policy_label_map]
END
GO
IF (OBJECT_ID('x_policy_label') IS NOT NULL)
BEGIN
        DROP TABLE [dbo].[x_policy_label]
END
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_policy_label] (
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](64) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [label_name] [varchar](512) DEFAULT NULL,
        PRIMARY KEY CLUSTERED
        (
        [id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
        CONSTRAINT [x_policy_label$x_policy_label_UK_label_name] UNIQUE NONCLUSTERED
        (
        [label_name] ASC
        )WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE dbo.x_policy_label_map (
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](64) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [policy_id] [bigint] DEFAULT NULL,
        [policy_label_id] [bigint] DEFAULT NULL,
        PRIMARY KEY CLUSTERED
        (
        [id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
)
GO
ALTER TABLE [dbo].[x_policy_label] WITH CHECK ADD CONSTRAINT [x_policy_label_FK_added_by_id] FOREIGN KEY ([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_label] WITH CHECK ADD CONSTRAINT x_policy_label_FK_upd_by_id FOREIGN KEY ([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_label_map] WITH CHECK ADD CONSTRAINT [x_policy_label_map_FK_added_by_id] FOREIGN KEY ([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_label_map] WITH CHECK ADD CONSTRAINT [x_policy_label_map_FK_upd_by_id] FOREIGN KEY ([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_label_map] WITH CHECK ADD CONSTRAINT [x_policy_label_map_FK_policy_id] FOREIGN KEY ([policy_id]) REFERENCES [dbo].[x_policy] ([id])
GO
ALTER TABLE [dbo].[x_policy_label_map] WITH CHECK ADD CONSTRAINT [x_policy_label_map_FK_policy_label_id] FOREIGN KEY ([policy_label_id]) REFERENCES [dbo].[x_policy_label] ([id])
GO
ALTER TABLE [dbo].[x_policy_label_map] WITH CHECK ADD CONSTRAINT [x_policy_label_map$x_policy_label_map_pid_plid] UNIQUE (policy_id, policy_label_id)
GO
CREATE NONCLUSTERED INDEX [x_policy_label_IDX_label_id] ON [dbo].[x_policy_label]
(
[id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX x_policy_label_IDX_label_name ON [dbo].[x_policy_label]
(
[label_name] ASC
)WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX x_policy_label_IDX_label_map_id ON [dbo].[x_policy_label_map]
(
[id] ASC
)WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
exit
