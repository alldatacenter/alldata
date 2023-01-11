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
DECLARE tableID INT = 0;
DECLARE columnID INT = 0;
DECLARE guTableID INT = 0;
DECLARE guColumnID INT = 0;
        IF EXISTS(select * from SYS.SYSCOLUMNS where tname = 'x_group' and cname='group_name') THEN
                IF NOT EXISTS(select * from SYS.SYSCONSTRAINT where constraint_name = 'x_group_UK_group_name') THEN
                        select table_id into tableID from SYS.SYSTAB where table_name = 'x_group';
                        select column_id into columnID from SYS.SYSTABCOL where table_id=tableID and column_name = 'group_name';
                        IF NOT EXISTS(select * from SYS.SYSIDXCOL where table_id=tableID and column_id=columnID) THEN
                                ALTER TABLE dbo.x_group ALTER group_name varchar(767) NOT NULL;
                                ALTER TABLE dbo.x_group ADD CONSTRAINT x_group_UK_group_name UNIQUE NONCLUSTERED (group_name);
                        END IF;
                END IF;
        END IF;
        IF EXISTS(select * from SYS.SYSCOLUMNS where tname = 'x_group_users' and cname='group_name') THEN
                IF NOT EXISTS(select * from SYS.SYSCONSTRAINT where constraint_name = 'x_group_users_UK_uid_gname') THEN
                        select table_id into guTableID from SYS.SYSTAB where table_name = 'x_group_users';
                        select column_id into guColumnID from SYS.SYSTABCOL where table_id=guTableID and column_name = 'group_name';
                        IF NOT EXISTS(select * from SYS.SYSIDXCOL where table_id=guTableID and column_id=guColumnID) THEN
                                ALTER TABLE dbo.x_group_users ALTER group_name varchar(767) NOT NULL;
                                alter table dbo.x_group_users drop constraint x_group_users_FK_user_id;
                                ALTER TABLE dbo.x_group_users ALTER user_id bigint NOT NULL;
                                ALTER TABLE dbo.x_group_users ADD CONSTRAINT x_group_users_FK_user_id FOREIGN KEY(user_id) REFERENCES dbo.x_user (id);
                                ALTER TABLE dbo.x_group_users ADD CONSTRAINT x_group_users_UK_uid_gname UNIQUE NONCLUSTERED (user_id,group_name);
                        END IF;
                END IF;
        END IF;
END
GO
