

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

IF EXISTS(select * from INFORMATION_SCHEMA.columns where table_name = 'x_data_hist' and column_name in('obj_id', 'obj_class_type'))
BEGIN
	IF NOT EXISTS(select * from sys.indexes where name='x_data_hist_idx_objid_objclstype')
	BEGIN
		CREATE NONCLUSTERED INDEX [x_data_hist_idx_objid_objclstype] ON [x_data_hist]([obj_id] ASC, [obj_class_type] ASC)WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY];
	END
END
GO
exit