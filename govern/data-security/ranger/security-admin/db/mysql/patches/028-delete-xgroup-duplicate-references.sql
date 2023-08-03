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

-- --------------------------------------------------------------------------------
-- Procedure which shall remove duplicate entries from x_group table
-- Duplicate entries were previously created due to unavailablity of unique index
-- --------------------------------------------------------------------------------
DELIMITER $$

DROP PROCEDURE if exists deleteXGroupDuplicateReferences $$
CREATE PROCEDURE `deleteXGroupDuplicateReferences`()
BEGIN
Block1: BEGIN

DECLARE donecursor1 INT;
DECLARE group_name1 varchar(1024);
DECLARE mingroupid1 bigint;
DECLARE id2 bigint;

DECLARE cursor1 CURSOR FOR 
	SELECT group_name,min(id) FROM x_group GROUP BY group_name HAVING count(group_name)>1;

DECLARE CONTINUE HANDLER FOR NOT FOUND SET donecursor1 = 1;
	OPEN cursor1;
		REPEAT
		FETCH cursor1 into group_name1, mingroupid1;
			Block2: BEGIN
				DECLARE donecursor2 INT DEFAULT 0;
				DECLARE cursor2 CURSOR FOR SELECT id FROM x_group WHERE group_name= group_name1 AND id > mingroupid1;
				DECLARE CONTINUE HANDLER FOR NOT FOUND SET donecursor2 = 1;
				OPEN cursor2;
				REPEAT
				FETCH cursor2 INTO id2;
					UPDATE x_group_users SET p_group_id=mingroupid1 where p_group_id=id2;
				UNTIL donecursor2 END REPEAT;
				CLOSE cursor2;
			END Block2;
		UNTIL donecursor1 END REPEAT;
	CLOSE cursor1;
END Block1;

Block3: BEGIN

DECLARE donecursor3 INT;
DECLARE group_name3 varchar(1024);
DECLARE user_id3 bigint;
DECLARE minrowid3 bigint;

DECLARE cursor3 CURSOR FOR 
	SELECT group_name,user_id,min(id) FROM x_group_users GROUP BY group_name,user_id HAVING count(1)>1;

DECLARE CONTINUE HANDLER FOR NOT FOUND SET donecursor3 = 1;
	OPEN cursor3;
		REPEAT
		FETCH cursor3 into group_name3, user_id3, minrowid3;
			DELETE FROM x_group_users WHERE group_name=group_name3 AND user_id=user_id3 AND id > minrowid3;
		UNTIL donecursor3 END REPEAT;
	CLOSE cursor3;
END Block3;

Block4: BEGIN

DECLARE donecursor4 INT;
DECLARE group_name4 varchar(1024);
DECLARE group_id4 bigint;
DECLARE minrowid4 bigint;

DECLARE cursor4 CURSOR FOR 
	SELECT group_name,min(id) FROM x_group GROUP BY group_name HAVING count(1)>1;

DECLARE CONTINUE HANDLER FOR NOT FOUND SET donecursor4 = 1;
	OPEN cursor4;
		REPEAT
		FETCH cursor4 into group_name4, minrowid4;
			DELETE FROM x_group WHERE group_name=group_name4 AND id > minrowid4;
		UNTIL donecursor4 END REPEAT;
	CLOSE cursor4;
END Block4;

END $$
DELIMITER ;
call deleteXGroupDuplicateReferences();

DROP PROCEDURE if exists deleteXGroupDuplicateReferences;
