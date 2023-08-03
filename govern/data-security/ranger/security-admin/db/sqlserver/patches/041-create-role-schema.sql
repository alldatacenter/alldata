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
IF (OBJECT_ID('x_role_ref_role') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_role_ref_role]
END

IF (OBJECT_ID('x_policy_ref_role') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_ref_role]
END

IF (OBJECT_ID('x_role_ref_group') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_role_ref_group]
END

IF (OBJECT_ID('x_role_ref_user') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_role_ref_user]
END

IF (OBJECT_ID('x_role') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_role]
END


GO
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_role](
 [id] [bigint] IDENTITY(1,1) NOT NULL,
[create_time] [datetime2] DEFAULT NULL NULL,
[update_time] [datetime2] DEFAULT NULL NULL,
[added_by_id] [bigint] DEFAULT NULL NULL,
[upd_by_id] [bigint] DEFAULT NULL NULL,
[version] [bigint] DEFAULT NULL NULL,
[name] [varchar](255) NOT NULL,
[description] [varchar](1024) DEFAULT NULL NULL,
[role_options] [varchar](4000) DEFAULT NULL NULL,
[role_text] [nvarchar](max) DEFAULT NULL NULL,
  PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],

 CONSTRAINT [x_role$x_role_UK_name] UNIQUE NONCLUSTERED
(
        [name] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
ALTER TABLE [dbo].[x_role] WITH CHECK ADD CONSTRAINT [x_role_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_role] WITH CHECK ADD CONSTRAINT [x_role_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_role_ref_user](
[id] [bigint] IDENTITY(1,1) NOT NULL,
[create_time] [datetime2] DEFAULT NULL NULL,
[update_time] [datetime2] DEFAULT NULL NULL,
[added_by_id] [bigint] DEFAULT NULL NULL,
[upd_by_id] [bigint] DEFAULT NULL NULL,
[role_id] [bigint] NOT NULL,
[user_id] [bigint] DEFAULT NULL NULL,
[user_name] [varchar](767) DEFAULT NULL NULL,
[priv_type] [int] DEFAULT NULL NULL,
  PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
ALTER TABLE [dbo].[x_role_ref_user] WITH CHECK ADD CONSTRAINT [x_role_ref_user_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_role_ref_user] WITH CHECK ADD CONSTRAINT [x_role_ref_user_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_role_ref_user] WITH CHECK ADD CONSTRAINT [x_role_ref_user_FK_role_id] FOREIGN KEY([role_id]) REFERENCES [dbo].[x_role] ([id])
GO
ALTER TABLE [dbo].[x_role_ref_user] WITH CHECK ADD CONSTRAINT [x_role_ref_user_FK_user_id] FOREIGN KEY([user_id]) REFERENCES [dbo].[x_user] ([id])
GO


SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_role_ref_group](
[id] [bigint] IDENTITY(1,1) NOT NULL,
[create_time] [datetime2] DEFAULT NULL NULL,
[update_time] [datetime2] DEFAULT NULL NULL,
[added_by_id] [bigint] DEFAULT NULL NULL,
[upd_by_id] [bigint] DEFAULT NULL NULL,
[role_id] [bigint] NOT NULL,
[group_id] [bigint] DEFAULT NULL NULL,
[group_name] [varchar](767) DEFAULT NULL NULL,
[priv_type] [int] DEFAULT NULL NULL,
 PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
ALTER TABLE [dbo].[x_role_ref_group] WITH CHECK ADD CONSTRAINT [x_role_ref_group_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_role_ref_group] WITH CHECK ADD CONSTRAINT [x_role_ref_group_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_role_ref_group] WITH CHECK ADD CONSTRAINT [x_role_ref_group_FK_role_id] FOREIGN KEY([role_id]) REFERENCES [dbo].[x_role] ([id])
GO
ALTER TABLE [dbo].[x_role_ref_group] WITH CHECK ADD CONSTRAINT [x_role_ref_group_FK_group_id] FOREIGN KEY([group_id]) REFERENCES [dbo].[x_group] ([id])
GO

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_policy_ref_role](
[id] [bigint] IDENTITY(1,1) NOT NULL,
[create_time] [datetime2] DEFAULT NULL NULL,
[update_time] [datetime2] DEFAULT NULL NULL,
[added_by_id] [bigint] DEFAULT NULL NULL,
[upd_by_id] [bigint] DEFAULT NULL NULL,
[policy_id] [bigint] NOT NULL,
[role_id] [bigint] NOT NULL,
[role_name] [varchar](255) DEFAULT NULL NULL,
 PRIMARY KEY CLUSTERED
  (
  [id] ASC
  ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
    CONSTRAINT [x_policy_ref_role$x_policy_ref_role_UK_polId_roleId] UNIQUE NONCLUSTERED
(
        [policy_id] ASC, [role_id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
ALTER TABLE [dbo].[x_policy_ref_role] WITH CHECK ADD CONSTRAINT [x_policy_ref_role_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_ref_role] WITH CHECK ADD CONSTRAINT [x_policy_ref_role_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_ref_role] WITH CHECK ADD CONSTRAINT [x_policy_ref_role_FK_policy_id] FOREIGN KEY([policy_id]) REFERENCES [dbo].[x_policy] ([id])
GO
ALTER TABLE [dbo].[x_policy_ref_role] WITH CHECK ADD CONSTRAINT [x_policy_ref_role_FK_role_id] FOREIGN KEY([role_id]) REFERENCES [dbo].[x_role] ([id])
GO

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_role_ref_role](
[id] [bigint] IDENTITY(1,1) NOT NULL,
[create_time] [datetime2] DEFAULT NULL NULL,
[update_time] [datetime2] DEFAULT NULL NULL,
[added_by_id] [bigint] DEFAULT NULL NULL,
[upd_by_id] [bigint] DEFAULT NULL NULL,
[role_ref_id] [bigint] DEFAULT NULL NULL,
[role_id] [bigint] NOT NULL,
[role_name] [varchar](255) DEFAULT NULL NULL,
[priv_type] [int] DEFAULT NULL NULL,
  PRIMARY KEY CLUSTERED
  (
  [id] ASC
  ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
)ON [PRIMARY]
GO
ALTER TABLE [dbo].[x_role_ref_role] WITH CHECK ADD CONSTRAINT [x_role_ref_role_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_role_ref_role] WITH CHECK ADD CONSTRAINT [x_role_ref_role_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_role_ref_role] WITH CHECK ADD CONSTRAINT [x_role_ref_role_FK_role_ref_id] FOREIGN KEY([role_ref_id]) REFERENCES [dbo].[x_role] ([id])
GO
exit