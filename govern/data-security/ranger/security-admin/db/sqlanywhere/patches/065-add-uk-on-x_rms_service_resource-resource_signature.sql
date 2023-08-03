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

BEGIN
    TRUNCATE TABLE dbo.x_rms_mapping_provider;
    TRUNCATE TABLE dbo.x_rms_resource_mapping;
    TRUNCATE TABLE dbo.x_rms_notification;
    TRUNCATE TABLE dbo.x_rms_service_resource;
END
GO
DROP INDEX IF EXISTS x_rms_service_resource_IDX_resource_signature;
GO
CREATE UNIQUE INDEX IF NOT EXISTS x_rms_service_resource_UK_resource_signature ON x_rms_service_resource(resource_signature);
GO
EXIT
