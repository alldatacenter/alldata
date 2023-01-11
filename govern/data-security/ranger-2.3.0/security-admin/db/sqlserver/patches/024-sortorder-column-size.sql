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

IF EXISTS(select * from INFORMATION_SCHEMA.columns where table_name = 'x_policy_item_user_perm' and column_name = 'sort_order' and DATA_TYPE='tinyint')
BEGIN
	DECLARE @ObjectName VARCHAR(100);
	SELECT @ObjectName = OBJECT_NAME([default_object_id]) FROM SYS.COLUMNS WHERE [object_id] = OBJECT_ID('[dbo].[x_policy_item_user_perm]') AND [name] = 'sort_order';
	IF @ObjectName IS NOT NULL
	BEGIN
		EXEC('ALTER TABLE [dbo].[x_policy_item_user_perm] DROP CONSTRAINT ' + @ObjectName)
	END
	IF NOT EXISTS(select name from SYS.sysobjects where parent_obj in (select id from SYS.sysobjects where name='x_policy_item_user_perm') and name=@ObjectName)
	BEGIN
		ALTER TABLE [dbo].[x_policy_item_user_perm] ALTER COLUMN [sort_order] [int]
	END
END
GO
IF EXISTS(select * from INFORMATION_SCHEMA.columns where table_name = 'x_policy_item_group_perm' and column_name = 'sort_order' and DATA_TYPE='tinyint')
BEGIN
	DECLARE @ObjectName VARCHAR(100);
	SELECT @ObjectName = OBJECT_NAME([default_object_id]) FROM SYS.COLUMNS WHERE [object_id] = OBJECT_ID('[dbo].[x_policy_item_group_perm]') AND [name] = 'sort_order';
	IF @ObjectName IS NOT NULL
	BEGIN
		EXEC('ALTER TABLE [dbo].[x_policy_item_group_perm] DROP CONSTRAINT ' + @ObjectName)
	END
	IF NOT EXISTS(select name from SYS.sysobjects where parent_obj in (select id from SYS.sysobjects where name='x_policy_item_group_perm') and name=@ObjectName)
	BEGIN
		ALTER TABLE [dbo].[x_policy_item_group_perm] ALTER COLUMN [sort_order] [int]
	END
END
GO
exit
