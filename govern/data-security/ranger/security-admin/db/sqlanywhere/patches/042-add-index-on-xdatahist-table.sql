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
DECLARE columnID1 INT = 0;
DECLARE columnID2 INT = 0;
DECLARE guTableID INT = 0;
DECLARE guColumnID INT = 0;
	IF EXISTS(select * from SYS.SYSCOLUMNS where tname = 'x_data_hist' and cname in('obj_id', 'obj_class_type')) THEN
		select table_id into tableID from SYS.SYSTAB where table_name = 'x_data_hist';
		select column_id into columnID1 from SYS.SYSTABCOL where table_id=tableID and column_name = 'obj_id';
		select column_id into columnID2 from SYS.SYSTABCOL where table_id=tableID and column_name = 'obj_class_type';
		IF NOT EXISTS(select * from SYS.SYSIDXCOL where table_id=tableID and column_id in (columnID1, columnID2)) THEN
			CREATE NONCLUSTERED INDEX x_data_hist_idx_objid_objclstype ON dbo.x_data_hist(obj_id ASC, obj_class_type ASC);
		END IF;
	END IF;
END
GO
