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
	TRUNCATE x_policy_change_log;
	TRUNCATE x_tag_change_log;
	CREATE NONCLUSTERED UNIQUE INDEX x_policy_change_log_uk_service_id_policy_version ON dbo.x_policy_change_log((service_id, policy_version) ASC);
	CREATE NONCLUSTERED UNIQUE INDEX x_tag_change_log_uk_service_id_service_tags_version ON dbo.x_tag_change_log((service_id, service_tags_version) ASC);
END
GO
