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

DECLARE
	v_count number:=0;
BEGIN
	select count(*) into v_count from user_tab_cols where table_name='X_DATA_HIST' and column_name IN('OBJ_ID', 'OBJ_CLASS_TYPE');
	if (v_count = 2) THEN
		v_count:=0;
		select count(*) into v_count from user_ind_columns where table_name='X_DATA_HIST' and column_name IN('OBJ_ID', 'OBJ_CLASS_TYPE') and index_name='X_DATA_HIST_IDX_OBJID_CLSTYPE';
		if (v_count = 0) THEN
			execute immediate 'CREATE INDEX x_data_hist_idx_objid_clstype ON x_data_hist(obj_id, obj_class_type)';
			commit;
		end if;
	end if;
END;/
