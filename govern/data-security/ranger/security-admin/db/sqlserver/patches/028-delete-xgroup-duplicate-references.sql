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

IF (OBJECT_ID('delete_xgroup_duplicate_references') IS NOT NULL)
BEGIN
    DROP PROCEDURE [dbo].[delete_xgroup_duplicate_references]
END
GO
CREATE PROCEDURE delete_xgroup_duplicate_references
AS BEGIN
	DECLARE @donecursor1 bigint
	DECLARE @group_name1 varchar(1024)
	DECLARE @mingroupid1 bigint
	DECLARE @id2 bigint
	DECLARE @group_name3 varchar(1024)
	DECLARE @user_id3 bigint
	DECLARE @minrowid3 bigint
	DECLARE @group_name4 varchar(1024)
	DECLARE @group_id4 bigint
	DECLARE @minrowid4 bigint
	DECLARE cursor1 CURSOR FOR SELECT group_name, min(id) FROM x_group GROUP BY group_name HAVING count(group_name)>1
	OPEN cursor1
		FETCH NEXT FROM cursor1 INTO @group_name1, @mingroupid1
		WHILE (@@FETCH_STATUS = 0)
		BEGIN
			DECLARE cursor2 CURSOR FOR SELECT id FROM x_group WHERE group_name = @group_name1 AND id > @mingroupid1
			OPEN cursor2
				FETCH NEXT FROM cursor2 INTO @id2
				WHILE (@@FETCH_STATUS = 0)
				BEGIN
					UPDATE x_group_users SET p_group_id=@mingroupid1 where p_group_id=@id2
					FETCH NEXT FROM cursor2 INTO @id2 
				END
			CLOSE cursor2
			DEALLOCATE cursor2
			FETCH NEXT FROM cursor1 INTO @group_name1, @mingroupid1
		END
	CLOSE cursor1
	DEALLOCATE cursor1
	
	DECLARE cursor3 CURSOR FOR SELECT group_name,user_id,min(id) FROM x_group_users GROUP BY group_name,user_id HAVING count(1)>1
	OPEN cursor3
		FETCH NEXT FROM cursor3 INTO @group_name3, @user_id3, @minrowid3
		WHILE (@@FETCH_STATUS = 0)
		BEGIN
			DELETE FROM x_group_users WHERE group_name=@group_name3 AND user_id=@user_id3 AND id > @minrowid3
			FETCH NEXT FROM cursor3 INTO @group_name3, @user_id3, @minrowid3
		END
	CLOSE cursor3
	DEALLOCATE cursor3

	DECLARE cursor4 CURSOR FOR SELECT group_name,min(id) FROM x_group GROUP BY group_name HAVING count(1)>1
	OPEN cursor4
		FETCH NEXT FROM cursor4 INTO @group_name4, @minrowid4
		WHILE (@@FETCH_STATUS = 0)
		BEGIN
	  		DELETE FROM x_group WHERE group_name=@group_name4 AND id > @minrowid4
	  		FETCH NEXT FROM cursor4 INTO @group_name4, @minrowid4
		END
	CLOSE cursor4
	DEALLOCATE cursor4
END
GO
IF (OBJECT_ID('delete_xgroup_duplicate_references') IS NOT NULL)
BEGIN
	EXEC delete_xgroup_duplicate_references
END
GO
exit