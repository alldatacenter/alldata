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
	IF EXISTS(select * from SYS.SYSCOLUMNS where tname = 'x_user' and cname='user_name') THEN
		IF NOT EXISTS(select * from SYS.SYSCONSTRAINT where constraint_name = 'x_user_UK_user_name') THEN
			select table_id into tableID from SYS.SYSTAB where table_name = 'x_user';
			select column_id into columnID from SYS.SYSTABCOL where table_id=tableID and column_name = 'user_name';
			IF NOT EXISTS(select * from SYS.SYSIDXCOL where table_id=tableID and column_id=columnID) THEN
				ALTER TABLE dbo.x_user ALTER user_name varchar(767) NOT NULL;
				ALTER TABLE dbo.x_user ADD CONSTRAINT x_user_UK_user_name UNIQUE NONCLUSTERED (user_name);
			END IF;
		END IF;
	END IF;
END
GO
exit
