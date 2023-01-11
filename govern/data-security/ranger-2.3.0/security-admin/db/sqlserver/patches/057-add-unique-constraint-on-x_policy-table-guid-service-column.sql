

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


IF EXISTS(select * from INFORMATION_SCHEMA.columns where table_name = 'x_policy' and column_name in ('guid','service','zone_id'))
BEGIN
	IF NOT EXISTS(select * from INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE where table_name='x_policy' and column_name='guid' and constraint_name = 'x_policy$x_policy_UK_guid_service_zone')
	BEGIN
		IF NOT EXISTS(select * from INFORMATION_SCHEMA.TABLE_CONSTRAINTS where table_name='x_policy' and constraint_name = 'x_policy$x_policy_UK_guid_service_zone' and CONSTRAINT_TYPE='UNIQUE')
		BEGIN
			ALTER TABLE [dbo].[x_policy] ADD CONSTRAINT [x_policy$x_policy_UK_guid_service_zone] UNIQUE ([guid],[service],[zone_id]);
		END
	END
END
GO
exit