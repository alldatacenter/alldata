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
IF (OBJECT_ID('x_service_version_info_service_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service_version_info] DROP CONSTRAINT x_service_version_info_service_id
END
GO
IF (OBJECT_ID('x_service_version_info') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_service_version_info]
END
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_service_version_info](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[service_id] [bigint] NOT NULL,
	[policy_version] [bigint] NOT NULL DEFAULT 0,
	[policy_update_time] [datetime2] DEFAULT NULL NULL,
	[tag_version] [bigint] NOT NULL DEFAULT 0,
	[tag_update_time] [datetime2] DEFAULT NULL NULL,
	PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
ALTER TABLE [dbo].[x_service_version_info] WITH CHECK ADD CONSTRAINT [x_service_version_info_service_id] FOREIGN KEY([service_id]) REFERENCES [dbo].[x_service] ([id])
GO
CREATE NONCLUSTERED INDEX [x_service_version_info_IDX_service_id] ON [x_service_version_info]
(
   [service_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO

exit
