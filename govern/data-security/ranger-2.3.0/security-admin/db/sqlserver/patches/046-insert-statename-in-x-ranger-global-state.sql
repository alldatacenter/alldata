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
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
IF EXISTS (SELECT *
           FROM   sys.objects
           WHERE  object_id = OBJECT_ID(N'dbo.getXportalUIdByLoginId')
                  AND type IN ( N'FN', N'IF', N'TF', N'FS', N'FT' ))
  DROP FUNCTION dbo.getXportalUIdByLoginId
  PRINT 'Dropped function dbo.getXportalUIdByLoginId'

GO
PRINT 'Creating function dbo.getXportalUIdByLoginId'
GO
CREATE FUNCTION dbo.getXportalUIdByLoginId
(

        @inputValue varchar(200)
)
RETURNS int
AS
BEGIN
        Declare @myid int;

        Select @myid = id from x_portal_user where x_portal_user.login_id = @inputValue;

        return @myid;

END
GO

PRINT 'Created function dbo.getXportalUIdByLoginId successfully'
GO


GO
IF EXISTS(select * from INFORMATION_SCHEMA.columns where table_name = 'x_ranger_global_state' and column_name = 'state_name')
	BEGIN
		
		IF NOT EXISTS(select * from x_ranger_global_state where state_name='RangerRole')
		BEGIN
			INSERT INTO x_ranger_global_state (create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1,'RangerRole','{"Version":"1"}');
		END;
		
		IF NOT EXISTS(select * from x_ranger_global_state where state_name='RangerUserStore')
		BEGIN
			INSERT INTO x_ranger_global_state (create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1,'RangerUserStore','{"Version":"1"}');
		END;
		
		IF NOT EXISTS(select * from x_ranger_global_state where state_name='RangerSecurityZone')
		BEGIN
			INSERT INTO x_ranger_global_state (create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1,'RangerSecurityZone','{"Version":"1"}');
		END;
	END;
GO
EXIT
