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
IF EXISTS(SELECT * FROM sys.indexes WHERE name = 'x_service_res_IDX_resource_signature' AND object_id = OBJECT_ID('x_service_resource'))
BEGIN
	DROP INDEX [x_service_res_IDX_resource_signature] ON [x_service_resource];
END
IF NOT EXISTS(SELECT * FROM sys.indexes WHERE name = 'x_service_res_IDX_svc_id_resource_signature' AND object_id = OBJECT_ID('x_service_resource'))
BEGIN
	CREATE UNIQUE NONCLUSTERED INDEX [x_service_res_IDX_svc_id_resource_signature] ON [x_service_resource] ([service_id] ASC, [resource_signature] ASC)WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY];
END
GO
exit
