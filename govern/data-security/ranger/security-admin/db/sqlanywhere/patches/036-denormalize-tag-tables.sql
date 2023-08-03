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

IF NOT EXISTS(select * from SYS.SYSCOLUMNS where tname = 'x_tag_def' and cname = 'tag_attrs_def_text') THEN
		ALTER TABLE dbo.x_tag_def ADD tag_attrs_def_text text DEFAULT NULL NULL;
END IF;
IF NOT EXISTS(select * from SYS.SYSCOLUMNS where tname = 'x_tag' and cname = 'tag_attrs_text') THEN
		ALTER TABLE dbo.x_tag ADD tag_attrs_text text DEFAULT NULL NULL;
END IF;
IF NOT EXISTS(select * from SYS.SYSCOLUMNS where tname = 'x_service_resource' and cname = 'service_resource_elements_text') THEN
		ALTER TABLE dbo.x_service_resource ADD service_resource_elements_text text DEFAULT NULL NULL;
END IF;
IF NOT EXISTS(select * from SYS.SYSCOLUMNS where tname = 'x_service_resource' and cname = 'tags_text') THEN
		ALTER TABLE dbo.x_service_resource ADD tags_text text DEFAULT NULL NULL;
END IF;
GO

CREATE PROCEDURE dbo.removeTagForeignKeyConstraint (IN table_name varchar(100))
AS
BEGIN
        DECLARE @stmt VARCHAR(300)
        DECLARE cur CURSOR FOR
                select 'alter table dbo.' + table_name + ' drop constraint ' + role
                from SYS.SYSFOREIGNKEYS
                where foreign_creator ='dbo' and foreign_tname = table_name

        OPEN cur WITH HOLD
                fetch cur into @stmt
                if (@@sqlstatus = 2)
                BEGIN
                        close cur
                        DEALLOCATE CURSOR cur
                END

                WHILE (@@sqlstatus = 0)
                BEGIN

                        execute(@stmt)
                        fetch cur into @stmt
                END
        close cur
        DEALLOCATE CURSOR cur

END
GO

call dbo.removeTagForeignKeyConstraint('x_tag_attr_def')
GO

call dbo.removeTagForeignKeyConstraint('x_tag_attr')
GO

call dbo.removeTagForeignKeyConstraint('x_service_resource_element')
GO

call dbo.removeTagForeignKeyConstraint('x_service_resource_element_val')
GO

exit
