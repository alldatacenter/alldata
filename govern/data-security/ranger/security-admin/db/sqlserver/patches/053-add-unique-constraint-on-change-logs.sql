

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

IF NOT EXISTS(select * from sys.indexes where name='x_policy_change_log_uk_service_id_policy_version' AND object_id = OBJECT_ID('x_policy_change_log'))
BEGIN
	TRUNCATE TABLE x_policy_change_log;
	CREATE UNIQUE NONCLUSTERED INDEX [x_policy_change_log_uk_service_id_policy_version] ON [x_policy_change_log]([service_id] ASC, [policy_version] ASC)WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY];
END
GO
IF NOT EXISTS(select * from sys.indexes where name='x_tag_change_log_uk_service_id_service_tags_version'  AND object_id = OBJECT_ID('x_tag_change_log'))
BEGIN
	TRUNCATE TABLE x_tag_change_log;
	CREATE UNIQUE NONCLUSTERED INDEX [x_tag_change_log_uk_service_id_service_tags_version] ON [x_tag_change_log]([service_id] ASC, [service_tags_version] ASC)WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY];
END
GO
exit
