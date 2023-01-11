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
IF NOT EXISTS(SELECT * FROM sys.indexes WHERE name = 'x_rms_service_resource_IDX_resource_signature' AND object_id = OBJECT_ID('x_rms_service_resource'))
BEGIN
	CREATE NONCLUSTERED INDEX [x_rms_service_resource_IDX_resource_signature] ON [x_rms_service_resource]
	(
	   [resource_signature] ASC
	)
	WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
END
Go

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

BEGIN
	TRUNCATE TABLE [dbo].[x_rms_mapping_provider];
	TRUNCATE TABLE [dbo].[x_rms_notification];
	TRUNCATE TABLE [dbo].[x_rms_resource_mapping];
	TRUNCATE TABLE [dbo].[x_rms_service_resource];
	ALTER TABLE [dbo].[x_rms_resource_mapping]  WITH CHECK ADD CONSTRAINT [x_rms_res_map_FK_hl_res_id] FOREIGN KEY([hl_resource_id])
	REFERENCES [dbo].[x_rms_service_resource] ([id]);
	ALTER TABLE [dbo].[x_rms_resource_mapping]  WITH CHECK ADD CONSTRAINT [x_rms_res_map_FK_ll_res_id] FOREIGN KEY([ll_resource_id])
	REFERENCES [dbo].[x_rms_service_resource] ([id]);
END
GO
EXIT;
