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
IF (OBJECT_ID('x_plugin_info_UK') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_plugin_info] DROP CONSTRAINT x_plugin_info_UK
END
GO
IF (OBJECT_ID('x_plugin_info') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_plugin_info]
END
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_plugin_info](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [service_name] [varchar](255) NOT NULL,
        [app_type] [varchar](128) NOT NULL,
        [host_name] [varchar](255) NOT NULL,
        [ip_address] [varchar](64) NOT NULL,
        [info] [varchar](1024) NOT NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
CONSTRAINT [x_plugin_info$x_plugin_info_UK] UNIQUE NONCLUSTERED
(
        [service_name] ASC, [host_name] ASC, [app_type] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_plugin_info_IDX_service_name] ON [x_plugin_info]
(
   [service_name] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_plugin_info_IDX_host_name] ON [x_plugin_info]
(
   [host_name] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO

exit
