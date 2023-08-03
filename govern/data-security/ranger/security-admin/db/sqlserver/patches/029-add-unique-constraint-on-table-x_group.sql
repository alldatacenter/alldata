
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
IF EXISTS(select * from INFORMATION_SCHEMA.columns where table_name = 'x_group' and column_name = 'group_name')
BEGIN
        IF NOT EXISTS(select * from INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE where table_name='x_group' and column_name='group_name' and constraint_name = 'x_group$x_group_UK_group_name')
    BEGIN
            IF NOT EXISTS(select * from INFORMATION_SCHEMA.TABLE_CONSTRAINTS where table_name='x_group' and constraint_name = 'x_group$x_group_UK_group_name' and CONSTRAINT_TYPE='UNIQUE')
            BEGIN
                ALTER TABLE [dbo].[x_group] ALTER COLUMN [group_name] [varchar](767) NOT NULL;
                ALTER TABLE [dbo].[x_group] ADD CONSTRAINT [x_group$x_group_UK_group_name] UNIQUE ([group_name]);
            END
    END
END
GO
IF EXISTS(select * from INFORMATION_SCHEMA.columns where table_name = 'x_group_users' and column_name = 'group_name')
BEGIN
        IF NOT EXISTS(select * from INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE where table_name='x_group_users' and column_name='group_name' and constraint_name = 'x_group_users$x_group_users_UK_uid_gname')
    BEGIN
            IF NOT EXISTS(select * from INFORMATION_SCHEMA.TABLE_CONSTRAINTS where table_name='x_group_users' and constraint_name = 'x_group_users$x_group_users_UK_uid_gname' and CONSTRAINT_TYPE='UNIQUE')
            BEGIN
                ALTER TABLE [dbo].[x_group_users] ALTER COLUMN [group_name] [varchar](767) NOT NULL;
                ALTER TABLE [dbo].[x_group_users] ADD CONSTRAINT [x_group_users$x_group_users_UK_uid_gname] UNIQUE (user_id,group_name);
            END
    END
END
GO
exit
