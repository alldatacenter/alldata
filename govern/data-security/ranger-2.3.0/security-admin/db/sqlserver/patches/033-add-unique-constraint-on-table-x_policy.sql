

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
IF EXISTS(select * from INFORMATION_SCHEMA.columns where table_name = 'x_policy' and column_name = 'name')
BEGIN
	IF NOT EXISTS(select * from INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE where table_name='x_policy' and column_name='name' and constraint_name = 'x_policy$x_policy_UK_name_service')
	BEGIN
		IF NOT EXISTS(select * from INFORMATION_SCHEMA.TABLE_CONSTRAINTS where table_name='x_policy' and constraint_name = 'x_policy$x_policy_UK_name_service' and CONSTRAINT_TYPE='UNIQUE')
		BEGIN
			UPDATE [dbo].[x_policy] set name=concat(name, '-duplicate-',id) where id in (select id from (select id from [dbo].[x_policy] where concat(service,name) in (select concat(service,name) from [dbo].[x_policy] group by service,name having count(*) >1)) as tmp);
			ALTER TABLE [dbo].[x_policy] ADD CONSTRAINT [x_policy$x_policy_UK_name_service] UNIQUE ([name],[service]);
		END
	END
END
GO
exit
