--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

CREATE TABLE clusters (cluster_id BIGINT NOT NULL, resource_id BIGINT NOT NULL, cluster_info VARCHAR(255) NOT NULL, cluster_name VARCHAR(100) NOT NULL UNIQUE, desired_cluster_state VARCHAR(255) NOT NULL, desired_stack_version VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id));



CREATE TABLE clusterconfig (version_tag VARCHAR(255) NOT NULL, type_name VARCHAR(255) NOT NULL, cluster_id BIGINT NOT NULL, config_data VARCHAR(32000) NOT NULL, create_timestamp BIGINT NOT NULL, PRIMARY KEY (cluster_id, type_name, version_tag));



CREATE TABLE clusterconfigmapping (cluster_id bigint NOT NULL, type_name VARCHAR(255) NOT NULL, version_tag VARCHAR(255) NOT NULL, create_timestamp BIGINT NOT NULL, selected INTEGER NOT NULL DEFAULT 0, user_name VARCHAR(255) NOT NULL DEFAULT '_db', PRIMARY KEY (cluster_id, type_name, create_timestamp));



CREATE TABLE clusterservices (service_name VARCHAR(255) NOT NULL, cluster_id BIGINT NOT NULL, service_enabled INTEGER NOT NULL, PRIMARY KEY (service_name, cluster_id));



CREATE TABLE clusterstate (cluster_id BIGINT NOT NULL, current_cluster_state VARCHAR(255) NOT NULL, current_stack_version VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id));



CREATE TABLE componentconfigmapping (cluster_id BIGINT NOT NULL, component_name VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, config_type VARCHAR(255) NOT NULL, timestamp BIGINT NOT NULL, config_tag VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, component_name, service_name, config_type));



CREATE TABLE hostcomponentconfigmapping (cluster_id BIGINT NOT NULL, component_name VARCHAR(255) NOT NULL, host_name VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, config_type VARCHAR(255) NOT NULL, timestamp BIGINT NOT NULL, config_tag VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, component_name, host_name, service_name, config_type));



CREATE TABLE hcdesiredconfigmapping (cluster_id BIGINT NOT NULL, component_name VARCHAR(255) NOT NULL, host_name VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, config_type VARCHAR(255) NOT NULL, timestamp BIGINT NOT NULL, config_tag VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, component_name, host_name, service_name, config_type));



CREATE TABLE hostcomponentdesiredstate (cluster_id BIGINT NOT NULL, component_name VARCHAR(255) NOT NULL, desired_stack_version VARCHAR(255) NOT NULL, desired_state VARCHAR(255) NOT NULL, host_name VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, component_name, host_name, service_name));



CREATE TABLE hostcomponentstate (cluster_id BIGINT NOT NULL, component_name VARCHAR(255) NOT NULL, current_stack_version VARCHAR(255) NOT NULL, current_state VARCHAR(255) NOT NULL, host_name VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, component_name, host_name, service_name));



CREATE TABLE hosts (host_name VARCHAR(255) NOT NULL, cpu_count INTEGER NOT NULL, ph_cpu_count INTEGER, cpu_info VARCHAR(255) NOT NULL, discovery_status VARCHAR(2000) NOT NULL, disks_info VARCHAR(10000) NOT NULL, host_attributes VARCHAR(20000) NOT NULL, ipv4 VARCHAR(255), ipv6 VARCHAR(255), public_host_name VARCHAR(255), last_registration_time BIGINT NOT NULL, os_arch VARCHAR(255) NOT NULL, os_info VARCHAR(1000) NOT NULL, os_type VARCHAR(255) NOT NULL, rack_info VARCHAR(255) NOT NULL, total_mem BIGINT NOT NULL, PRIMARY KEY (host_name));



CREATE TABLE hoststate (agent_version VARCHAR(255) NOT NULL, available_mem BIGINT NOT NULL, current_state VARCHAR(255) NOT NULL, health_status VARCHAR(255), host_name VARCHAR(255) NOT NULL, time_in_state BIGINT NOT NULL,  PRIMARY KEY (host_name));



CREATE TABLE servicecomponentdesiredstate (component_name VARCHAR(255) NOT NULL, cluster_id BIGINT NOT NULL, desired_stack_version VARCHAR(255) NOT NULL, desired_state VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, PRIMARY KEY (component_name, cluster_id, service_name));



CREATE TABLE serviceconfigmapping (cluster_id BIGINT NOT NULL, service_name VARCHAR(255) NOT NULL, config_type VARCHAR(255) NOT NULL, timestamp BIGINT NOT NULL, config_tag VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, service_name, config_type));



CREATE TABLE servicedesiredstate (cluster_id BIGINT NOT NULL, desired_host_role_mapping INTEGER NOT NULL, desired_stack_version VARCHAR(255) NOT NULL, desired_state VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, service_name));



CREATE TABLE roles (role_name VARCHAR(255) NOT NULL, PRIMARY KEY (role_name));



CREATE TABLE users (user_id INTEGER, ldap_user INTEGER NOT NULL DEFAULT 0, user_name VARCHAR(255) NOT NULL, create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, user_password VARCHAR(255), PRIMARY KEY (user_id), UNIQUE (ldap_user, user_name));


CREATE TABLE execution_command (command BLOB, task_id BIGINT NOT NULL, PRIMARY KEY (task_id));


CREATE TABLE host_role_command (task_id BIGINT NOT NULL, attempt_count SMALLINT NOT NULL, event VARCHAR(32000) NOT NULL, exitcode INTEGER NOT NULL, host_name VARCHAR(255) NOT NULL, last_attempt_time BIGINT NOT NULL, request_id BIGINT NOT NULL, role VARCHAR(255), stage_id BIGINT NOT NULL, start_time BIGINT NOT NULL, status VARCHAR(255), std_error BLOB, std_out BLOB, role_command VARCHAR(255), PRIMARY KEY (task_id));


CREATE TABLE role_success_criteria (role VARCHAR(255) NOT NULL, request_id BIGINT NOT NULL, stage_id BIGINT NOT NULL, success_factor FLOAT NOT NULL, PRIMARY KEY (role, request_id, stage_id));


CREATE TABLE stage (stage_id BIGINT NOT NULL, request_id BIGINT NOT NULL, cluster_id BIGINT NOT NULL, log_info VARCHAR(255) NOT NULL, request_context VARCHAR(255), PRIMARY KEY (stage_id, request_id));


CREATE TABLE ClusterHostMapping (cluster_id BIGINT NOT NULL, host_name VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, host_name));


CREATE TABLE user_roles (role_name VARCHAR(255) NOT NULL, user_id INTEGER NOT NULL, PRIMARY KEY (role_name, user_id));


CREATE TABLE key_value_store ("key" VARCHAR(255), "value" VARCHAR(20000), PRIMARY KEY("key"));


CREATE TABLE hostconfigmapping (cluster_id bigint NOT NULL, host_name VARCHAR(255) NOT NULL, type_name VARCHAR(255) NOT NULL, version_tag VARCHAR(255) NOT NULL, service_name VARCHAR(255), create_timestamp BIGINT NOT NULL, selected INTEGER NOT NULL DEFAULT 0, user_name VARCHAR(255) NOT NULL DEFAULT '_db', PRIMARY KEY (cluster_id, host_name, type_name, create_timestamp));



CREATE TABLE metainfo ("metainfo_key" VARCHAR(255), "metainfo_value" VARCHAR(20000), PRIMARY KEY("metainfo_key"));


CREATE TABLE ambari_sequences (sequence_name VARCHAR(255) PRIMARY KEY, "value" BIGINT NOT NULL);



ALTER TABLE clusterconfig ADD CONSTRAINT FK_clusterconfig_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE clusterservices ADD CONSTRAINT FK_clusterservices_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE clusterconfigmapping ADD CONSTRAINT FK_clusterconfigmapping_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE clusterstate ADD CONSTRAINT FK_clusterstate_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE componentconfigmapping ADD CONSTRAINT FK_componentconfigmapping_config_tag FOREIGN KEY (cluster_id, config_type, config_tag) REFERENCES clusterconfig (cluster_id, type_name, version_tag);
ALTER TABLE componentconfigmapping ADD CONSTRAINT FK_componentconfigmapping_component_name FOREIGN KEY (component_name, cluster_id, service_name) REFERENCES servicecomponentdesiredstate (component_name, cluster_id, service_name);
ALTER TABLE hostcomponentconfigmapping ADD CONSTRAINT FK_hostcomponentconfigmapping_config_tag FOREIGN KEY (cluster_id, config_type, config_tag) REFERENCES clusterconfig (cluster_id, type_name, version_tag);
ALTER TABLE hostcomponentconfigmapping ADD CONSTRAINT FK_hostcomponentconfigmapping_cluster_id FOREIGN KEY (cluster_id, component_name, host_name, service_name) REFERENCES hostcomponentstate (cluster_id, component_name, host_name, service_name);
ALTER TABLE hcdesiredconfigmapping ADD CONSTRAINT FK_hostcomponentdesiredconfigmapping_config_tag FOREIGN KEY (cluster_id, config_type, config_tag) REFERENCES clusterconfig (cluster_id, type_name, version_tag);
ALTER TABLE hcdesiredconfigmapping ADD CONSTRAINT FK_hostcomponentdesiredconfigmapping_cluster_id FOREIGN KEY (cluster_id, component_name, host_name, service_name) REFERENCES hostcomponentdesiredstate (cluster_id, component_name, host_name, service_name);
ALTER TABLE hostcomponentdesiredstate ADD CONSTRAINT FK_hostcomponentdesiredstate_host_name FOREIGN KEY (host_name) REFERENCES hosts (host_name);
ALTER TABLE hostcomponentdesiredstate ADD CONSTRAINT FK_hostcomponentdesiredstate_component_name FOREIGN KEY (component_name, cluster_id, service_name) REFERENCES servicecomponentdesiredstate (component_name, cluster_id, service_name);
ALTER TABLE hostcomponentstate ADD CONSTRAINT FK_hostcomponentstate_component_name FOREIGN KEY (component_name, cluster_id, service_name) REFERENCES servicecomponentdesiredstate (component_name, cluster_id, service_name);
ALTER TABLE hostcomponentstate ADD CONSTRAINT FK_hostcomponentstate_host_name FOREIGN KEY (host_name) REFERENCES hosts (host_name);
ALTER TABLE hoststate ADD CONSTRAINT FK_hoststate_host_name FOREIGN KEY (host_name) REFERENCES hosts (host_name);
ALTER TABLE servicecomponentdesiredstate ADD CONSTRAINT FK_servicecomponentdesiredstate_service_name FOREIGN KEY (service_name, cluster_id) REFERENCES clusterservices (service_name, cluster_id);
ALTER TABLE serviceconfigmapping ADD CONSTRAINT FK_serviceconfigmapping_config_tag FOREIGN KEY (cluster_id, config_type, config_tag) REFERENCES clusterconfig (cluster_id, type_name, version_tag);
ALTER TABLE serviceconfigmapping ADD CONSTRAINT FK_serviceconfigmapping_service_name FOREIGN KEY (service_name, cluster_id) REFERENCES clusterservices (service_name, cluster_id);
ALTER TABLE servicedesiredstate ADD CONSTRAINT FK_servicedesiredstate_service_name FOREIGN KEY (service_name, cluster_id) REFERENCES clusterservices (service_name, cluster_id);
ALTER TABLE execution_command ADD CONSTRAINT FK_execution_command_task_id FOREIGN KEY (task_id) REFERENCES host_role_command (task_id);
ALTER TABLE host_role_command ADD CONSTRAINT FK_host_role_command_stage_id FOREIGN KEY (stage_id, request_id) REFERENCES stage (stage_id, request_id);
ALTER TABLE host_role_command ADD CONSTRAINT FK_host_role_command_host_name FOREIGN KEY (host_name) REFERENCES hosts (host_name);
ALTER TABLE role_success_criteria ADD CONSTRAINT FK_role_success_criteria_stage_id FOREIGN KEY (stage_id, request_id) REFERENCES stage (stage_id, request_id);
ALTER TABLE stage ADD CONSTRAINT FK_stage_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE ClusterHostMapping ADD CONSTRAINT FK_ClusterHostMapping_host_name FOREIGN KEY (host_name) REFERENCES hosts (host_name);
ALTER TABLE ClusterHostMapping ADD CONSTRAINT FK_ClusterHostMapping_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE user_roles ADD CONSTRAINT FK_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users (user_id);
ALTER TABLE user_roles ADD CONSTRAINT FK_user_roles_role_name FOREIGN KEY (role_name) REFERENCES roles (role_name);
ALTER TABLE hostconfigmapping ADD CONSTRAINT FK_hostconfigmapping_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE hostconfigmapping ADD CONSTRAINT FK_hostconfigmapping_host_name FOREIGN KEY (host_name) REFERENCES hosts (host_name);

-- BEGIN;

insert into ambari_sequences(sequence_name, "value")
select 'cluster_id_seq', 1 FROM SYSIBM.SYSDUMMY1
union all
select 'user_id_seq', 2 FROM SYSIBM.SYSDUMMY1
union all
select 'host_role_command_id_seq', 1 FROM SYSIBM.SYSDUMMY1;

insert into Roles(role_name)
select 'admin' FROM SYSIBM.SYSDUMMY1
union all
select 'user' FROM SYSIBM.SYSDUMMY1;

insert into Users(user_id, user_name, user_password)
select 1,'admin','538916f8943ec225d97a9a86a2c6ec0818c1cd400e09e03b660fdaaec4af29ddbb6f2b1033b81b00' FROM SYSIBM.SYSDUMMY1;

insert into user_roles(role_name, user_id)
select 'admin',1 FROM SYSIBM.SYSDUMMY1;

insert into metainfo("metainfo_key", "metainfo_value")
select 'version','1.2.5' FROM SYSIBM.SYSDUMMY1;

-- COMMIT;

-- ambari log4j DDL


CREATE TABLE workflow (
  workflowId VARCHAR(20000), workflowName VARCHAR(20000),
  parentWorkflowId VARCHAR(20000),  
  workflowContext VARCHAR(20000), userName VARCHAR(20000),
  startTime BIGINT, lastUpdateTime BIGINT,
  numJobsTotal INTEGER, numJobsCompleted INTEGER,
  inputBytes BIGINT, outputBytes BIGINT,
  duration BIGINT,
  PRIMARY KEY (workflowId),
  FOREIGN KEY (parentWorkflowId) REFERENCES workflow(workflowId)
);



CREATE TABLE job (
  jobId VARCHAR(20000), workflowId VARCHAR(20000), jobName VARCHAR(20000), workflowEntityName VARCHAR(20000),
  userName VARCHAR(20000), queue VARCHAR(20000), acls VARCHAR(20000), confPath VARCHAR(20000), 
  submitTime BIGINT, launchTime BIGINT, finishTime BIGINT, 
  maps INTEGER, reduces INTEGER, status VARCHAR(20000), priority VARCHAR(20000), 
  finishedMaps INTEGER, finishedReduces INTEGER, 
  failedMaps INTEGER, failedReduces INTEGER, 
  mapsRuntime BIGINT, reducesRuntime BIGINT,
  mapCounters VARCHAR(20000), reduceCounters VARCHAR(20000), jobCounters VARCHAR(20000), 
  inputBytes BIGINT, outputBytes BIGINT,
  PRIMARY KEY(jobId),
  FOREIGN KEY(workflowId) REFERENCES workflow(workflowId)
);



CREATE TABLE task (
  taskId VARCHAR(20000), jobId VARCHAR(20000), taskType VARCHAR(20000), splits VARCHAR(20000), 
  startTime BIGINT, finishTime BIGINT, status VARCHAR(20000), error VARCHAR(20000), counters VARCHAR(20000), 
  failedAttempt VARCHAR(20000), 
  PRIMARY KEY(taskId), 
  FOREIGN KEY(jobId) REFERENCES job(jobId)
);



CREATE TABLE taskAttempt (
  taskAttemptId VARCHAR(20000), taskId VARCHAR(20000), jobId VARCHAR(20000), taskType VARCHAR(20000), taskTracker VARCHAR(20000), 
  startTime BIGINT, finishTime BIGINT, 
  mapFinishTime BIGINT, shuffleFinishTime BIGINT, sortFinishTime BIGINT, 
  locality VARCHAR(20000), avataar VARCHAR(20000), 
  status VARCHAR(20000), error VARCHAR(20000), counters VARCHAR(20000), 
  inputBytes BIGINT, outputBytes BIGINT,
  PRIMARY KEY(taskAttemptId), 
  FOREIGN KEY(jobId) REFERENCES job(jobId), 
  FOREIGN KEY(taskId) REFERENCES task(taskId)
); 



CREATE TABLE hdfsEvent (
  timestamp BIGINT,
  userName VARCHAR(20000),
  clientIP VARCHAR(20000),
  operation VARCHAR(20000),
  srcPath VARCHAR(20000),
  dstPath VARCHAR(20000),
  permissions VARCHAR(20000)
);



CREATE TABLE mapreduceEvent (
  timestamp BIGINT,
  userName VARCHAR(20000),
  clientIP VARCHAR(20000),
  operation VARCHAR(20000),
  target VARCHAR(20000),
  result VARCHAR(20000),
  description VARCHAR(20000),
  permissions VARCHAR(20000)
);



CREATE TABLE clusterEvent (
  timestamp BIGINT, 
  service VARCHAR(20000), status VARCHAR(20000), 
  error VARCHAR(20000), data VARCHAR(20000) , 
  host VARCHAR(20000), rack VARCHAR(20000)
);



