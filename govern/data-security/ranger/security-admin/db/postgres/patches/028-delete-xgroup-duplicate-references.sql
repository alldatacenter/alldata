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
select 'delimiter start';
CREATE OR REPLACE FUNCTION delete_xgroup_duplicate_references()
RETURNS void AS $$
DECLARE
	donecursor1 BIGINT:=0;
	group_name1 VARCHAR(1024);
	mingroupid1 BIGINT:=0;
	id2 BIGINT:=0;
	group_name3 VARCHAR(1024);
	user_id3 BIGINT:=0;
	minrowid3 BIGINT:=0;
	group_name4 VARCHAR(1024);
	group_id4 BIGINT:=0;
	minrowid4 BIGINT:=0;

	cursor1 cursor for SELECT group_name, min(id) FROM x_group GROUP BY group_name HAVING count(group_name)>1;
	cursor2 cursor for SELECT id FROM x_group WHERE group_name = group_name1 AND id > mingroupid1;
	cursor3 cursor for SELECT group_name,user_id,min(id) FROM x_group_users GROUP BY group_name,user_id HAVING count(1)>1;
	cursor4 cursor for SELECT group_name,min(id) FROM x_group GROUP BY group_name HAVING count(1)>1;

BEGIN 
	OPEN cursor1;
	LOOP
		FETCH cursor1 into group_name1, mingroupid1;
		EXIT WHEN NOT FOUND;
	  	OPEN cursor2;
	  	LOOP
			FETCH cursor2 INTO id2;
			EXIT WHEN NOT FOUND;
			UPDATE x_group_users SET p_group_id=mingroupid1 where p_group_id=id2;
		END LOOP;
		CLOSE cursor2;
	END LOOP;
	CLOSE cursor1;

	OPEN cursor3;
	LOOP
		FETCH cursor3 into group_name3, user_id3, minrowid3;
		EXIT WHEN NOT FOUND;
		DELETE FROM x_group_users WHERE group_name=group_name3 AND user_id=user_id3 AND id > minrowid3;
	END LOOP;
	CLOSE cursor3;

	OPEN cursor4;
	LOOP
		FETCH cursor4 into group_name4, minrowid4;
		EXIT WHEN NOT FOUND;
	  	DELETE FROM x_group WHERE group_name=group_name4 AND id > minrowid4;
	END LOOP;
	CLOSE cursor4;
END;
$$ LANGUAGE plpgsql;
select delete_xgroup_duplicate_references();
select 'delimiter end';