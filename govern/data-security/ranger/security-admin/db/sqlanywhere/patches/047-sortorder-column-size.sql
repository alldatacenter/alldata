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

CREATE OR REPLACE PROCEDURE dbo.alterSortOrderColumn (IN table_name varchar(100))
AS
BEGIN
  DECLARE @stmt VARCHAR(300)
  IF EXISTS(select * from SYS.SYSCOLUMNS where tname = table_name and cname='sort_order' and coltype='tinyint')
  BEGIN
    SET @stmt = 'ALTER TABLE dbo.' + table_name + ' ALTER sort_order INT DEFAULT 0 NULL'
    execute(@stmt)
  END
END
GO

call dbo.alterSortOrderColumn('x_service_config_def')
GO
call dbo.alterSortOrderColumn('x_resource_def')
GO
call dbo.alterSortOrderColumn('x_access_type_def')
GO
call dbo.alterSortOrderColumn('x_policy_condition_def')
GO
call dbo.alterSortOrderColumn('x_context_enricher_def')
GO
call dbo.alterSortOrderColumn('x_enum_element_def')
GO
call dbo.alterSortOrderColumn('x_policy_resource_map')
GO
call dbo.alterSortOrderColumn('x_policy_item')
GO
call dbo.alterSortOrderColumn('x_policy_item_access')
GO
call dbo.alterSortOrderColumn('x_policy_item_condition')
GO
call dbo.alterSortOrderColumn('x_policy_item_user_perm')
GO
call dbo.alterSortOrderColumn('x_policy_item_group_perm')
GO
call dbo.alterSortOrderColumn('x_datamask_type_def')
GO
exit