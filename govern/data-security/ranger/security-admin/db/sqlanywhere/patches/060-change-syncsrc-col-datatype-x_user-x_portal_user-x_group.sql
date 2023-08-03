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

CREATE OR REPLACE PROCEDURE dbo.alterColumnSyncSourceDataType (IN table_name1 varchar(100), IN table_name2 varchar(100), IN table_name3 varchar(100))
AS
BEGIN
  DECLARE @stmt VARCHAR(300)
  IF EXISTS(select * from SYS.SYSCOLUMNS where tname = table_name1 and cname in('notes', 'other_attributes', 'sync_source') and coltype='varchar')
  BEGIN
      SET @stmt = 'ALTER TABLE dbo.' + table_name1 + ' ALTER (notes text DEFAULT NULL NULL, other_attributes text DEFAULT NULL NULL, sync_source text DEFAULT NULL NULL)'
      execute(@stmt)
  END

  IF EXISTS(select * from SYS.SYSCOLUMNS where tname = table_name2 and cname in('descr', 'other_attributes', 'sync_source') and coltype='varchar')
  BEGIN
      SET @stmt = 'ALTER TABLE dbo.' + table_name2 + ' ALTER (descr text DEFAULT NULL NULL, other_attributes text DEFAULT NULL NULL, sync_source text DEFAULT NULL NULL)'
      execute(@stmt)
  END

  IF EXISTS(select * from SYS.SYSCOLUMNS where tname = table_name3 and cname in('descr', 'other_attributes', 'sync_source') and coltype='varchar')
  BEGIN
      SET @stmt = 'ALTER TABLE dbo.' + table_name3 + ' ALTER (descr text DEFAULT NULL NULL, other_attributes text DEFAULT NULL NULL, sync_source text DEFAULT NULL NULL)'
      execute(@stmt)
  END
END
GO

call dbo.alterColumnSyncSourceDataType('x_portal_user', 'x_user', 'x_group')
GO
EXIT