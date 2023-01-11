-- Licensed to the Apache Software Foundation(ASF) under one or more
-- contributor license agreements.  See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.
-- The ASF licenses this file to You under the Apache License, Version 2.0
--(the "License"); you may not use this file except in compliance with
-- the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

CREATE TABLE dbo.xa_access_audit(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	audit_type int DEFAULT 0 NOT NULL,
	access_result int DEFAULT 0 NULL,
	access_type varchar(255) DEFAULT NULL NULL,
	acl_enforcer varchar(255) DEFAULT NULL NULL,
	agent_id varchar(255) DEFAULT NULL NULL,
	client_ip varchar(255) DEFAULT NULL NULL,
	client_type varchar(255) DEFAULT NULL NULL,
	policy_id bigint DEFAULT 0 NULL,
	repo_name varchar(255) DEFAULT NULL NULL,
	repo_type int DEFAULT 0 NULL,
	result_reason varchar(255) DEFAULT NULL NULL,
	session_id varchar(255) DEFAULT NULL NULL,
	event_time datetime DEFAULT NULL NULL,
	request_user varchar(255) DEFAULT NULL NULL,
	action varchar(2000) DEFAULT NULL NULL,
	request_data varchar(4000) DEFAULT NULL NULL,
	resource_path varchar(4000) DEFAULT NULL NULL,
	resource_type varchar(255) DEFAULT NULL NULL,
	seq_num bigint DEFAULT 0 NULL,
	event_count bigint DEFAULT 1 NULL,
	event_dur_ms bigint DEFAULT 1 NULL,
	CONSTRAINT xa_access_audit_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
CREATE NONCLUSTERED INDEX xa_access_audit_cr_time ON dbo.xa_access_audit(create_time ASC)
GO
CREATE NONCLUSTERED INDEX xa_access_audit_event_time ON dbo.xa_access_audit(event_time ASC)
GO
CREATE NONCLUSTERED INDEX xa_access_audit_added_by_id ON dbo.xa_access_audit(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX xa_access_audit_upd_by_id ON dbo.xa_access_audit(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX xa_access_audit_up_time ON dbo.xa_access_audit(update_time ASC)
GO
exit