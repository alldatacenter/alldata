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

CREATE OR REPLACE FUNCTION dbo.getXportalUIdByLoginId (input_val CHAR(60))
RETURNS INTEGER
BEGIN
  DECLARE myid INTEGER;
  SELECT x_portal_user.id into myid FROM x_portal_user WHERE x_portal_user.login_id=input_val;
  RETURN (myid);
END;
GO

BEGIN
	IF EXISTS(select * from SYS.SYSCOLUMNS where tname = 'x_ranger_global_state' and cname='state_name') THEN
		
		IF NOT EXISTS(select * from x_ranger_global_state where state_name='RangerRole') THEN
			INSERT INTO x_ranger_global_state (create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1,'RangerRole','{"Version":"1"}');
		END IF;
		
		IF NOT EXISTS(select * from x_ranger_global_state where state_name='RangerUserStore') THEN
			INSERT INTO x_ranger_global_state (create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1,'RangerUserStore','{"Version":"1"}');
		END IF;
		
		IF NOT EXISTS(select * from x_ranger_global_state where state_name='RangerSecurityZone') THEN
			INSERT INTO x_ranger_global_state (create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1,'RangerSecurityZone','{"Version":"1"}');
		END IF;
	END IF;
END
GO
EXIT