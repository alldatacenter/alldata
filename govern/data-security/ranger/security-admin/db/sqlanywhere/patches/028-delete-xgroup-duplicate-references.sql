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

CREATE OR REPLACE PROCEDURE delete_xgroup_duplicate_references()
BEGIN
	DECLARE donecursor1 bigint;
	DECLARE group_name1 varchar(1024);
	DECLARE mingroupid1 bigint;
	DECLARE id2 bigint;
	DECLARE group_name3 varchar(1024);
	DECLARE user_id3 bigint;
	DECLARE minrowid3 bigint;
	DECLARE group_name4 varchar(1024);
	DECLARE group_id4 bigint;
	DECLARE minrowid4 bigint;

	DECLARE cursor1 CURSOR FOR SELECT group_name, min(id) FROM x_group GROUP BY group_name HAVING count(group_name)>1;
	DECLARE cursor2 CURSOR FOR SELECT id FROM x_group WHERE group_name = group_name1 AND id > mingroupid1;
	DECLARE cursor3 CURSOR FOR SELECT group_name,user_id,min(id) FROM x_group_users GROUP BY group_name,user_id HAVING count(1)>1;
	DECLARE cursor4 CURSOR FOR SELECT group_name,min(id) FROM x_group GROUP BY group_name HAVING count(1)>1;

	SET donecursor1=0;
	SET mingroupid1=0;
	SET id2=0;
	SET user_id3=0;
	SET minrowid3=0;
	SET group_id4=0;
	SET minrowid4=0;

	OPEN cursor1;
	loopc1: LOOP
		FETCH cursor1 INTO group_name1, mingroupid1;
		IF SQLCODE <> 0 THEN LEAVE loopc1 END IF;
		OPEN cursor2;
		loopc2: LOOP
			FETCH cursor2 INTO id2;
			IF SQLCODE <> 0 THEN LEAVE loopc2 END IF;
			UPDATE x_group_users SET p_group_id=mingroupid1 where p_group_id=id2;
		END LOOP;
		CLOSE cursor2;
	END LOOP;
	CLOSE cursor1;

	OPEN cursor3;
	loopc3: LOOP
		FETCH cursor3 INTO group_name3, user_id3, minrowid3;
		IF SQLCODE <> 0 THEN LEAVE loopc3 END IF;
		DELETE FROM x_group_users WHERE group_name=group_name3 AND user_id=user_id3 AND id > minrowid3;
	END LOOP;
	CLOSE cursor3;

	OPEN cursor4;
	loopc4: LOOP
		FETCH cursor4 INTO group_name4, minrowid4;
		IF SQLCODE <> 0 THEN LEAVE loopc4 END IF;
	  	DELETE FROM x_group WHERE group_name=group_name4 AND id > minrowid4;
	END LOOP;
	CLOSE cursor4;
END;
GO
EXEC delete_xgroup_duplicate_references;
GO
exit