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

------create tables---------
CREATE TABLE registries(
 id BIGINT NOT NULL,
 registy_name VARCHAR(255) NOT NULL,
 registry_type VARCHAR(255) NOT NULL,
 registry_uri VARCHAR(255) NOT NULL,
 CONSTRAINT PK_registries PRIMARY KEY (id));

CREATE TABLE mpacks(
 id BIGINT NOT NULL,
 mpack_name VARCHAR(255) NOT NULL,
 mpack_version VARCHAR(255) NOT NULL,
 mpack_uri VARCHAR(255),
 registry_id BIGINT,
 CONSTRAINT PK_mpacks PRIMARY KEY (id),
 CONSTRAINT uni_mpack_name_version UNIQUE(mpack_name, mpack_version),
 CONSTRAINT FK_registries FOREIGN KEY (registry_id) REFERENCES registries(id));

CREATE TABLE stack(
  stack_id NUMBER(19) NOT NULL,
  stack_name VARCHAR2(255) NOT NULL,
  stack_version VARCHAR2(255) NOT NULL,
  mpack_id NUMBER(19),
  CONSTRAINT PK_stack PRIMARY KEY (stack_id),
  CONSTRAINT FK_mpacks FOREIGN KEY (mpack_id) REFERENCES mpacks(id),
  CONSTRAINT UQ_stack UNIQUE (stack_name, stack_version));

CREATE TABLE extension(
  extension_id NUMERIC(19) NOT NULL,
  extension_name VARCHAR2(255) NOT NULL,
  extension_version VARCHAR2(255) NOT NULL,
  CONSTRAINT PK_extension PRIMARY KEY (extension_id),
  CONSTRAINT UQ_extension UNIQUE(extension_name, extension_version));

CREATE TABLE extensionlink(
  link_id NUMERIC(19) NOT NULL,
  stack_id NUMERIC(19) NOT NULL,
  extension_id NUMERIC(19) NOT NULL,
  CONSTRAINT PK_extensionlink PRIMARY KEY (link_id),
  CONSTRAINT FK_extensionlink_stack_id FOREIGN KEY (stack_id) REFERENCES stack(stack_id),
  CONSTRAINT FK_extensionlink_extension_id FOREIGN KEY (extension_id) REFERENCES extension(extension_id),
  CONSTRAINT UQ_extension_link UNIQUE(stack_id, extension_id));

CREATE TABLE adminresourcetype (
  resource_type_id NUMBER(10) NOT NULL,
  resource_type_name VARCHAR(255) NOT NULL,
  CONSTRAINT PK_adminresourcetype PRIMARY KEY (resource_type_id));

CREATE TABLE adminresource (
  resource_id NUMBER(19) NOT NULL,
  resource_type_id NUMBER(10) NOT NULL,
  CONSTRAINT PK_adminresource PRIMARY KEY (resource_id),
  CONSTRAINT FK_resource_resource_type_id FOREIGN KEY (resource_type_id) REFERENCES adminresourcetype(resource_type_id));

CREATE TABLE clusters (
  cluster_id NUMBER(19) NOT NULL,
  resource_id NUMBER(19) NOT NULL,
  upgrade_id NUMBER(19),
  cluster_info VARCHAR2(255) NULL,
  cluster_name VARCHAR2(100) NOT NULL UNIQUE,
  provisioning_state VARCHAR2(255) DEFAULT 'INIT' NOT NULL,
  security_type VARCHAR2(32) DEFAULT 'NONE' NOT NULL,
  desired_cluster_state VARCHAR2(255) NULL,
  desired_stack_id NUMBER(19) NOT NULL,
  CONSTRAINT PK_clusters PRIMARY KEY (cluster_id),
  CONSTRAINT FK_clusters_desired_stack_id FOREIGN KEY (desired_stack_id) REFERENCES stack(stack_id),
  CONSTRAINT FK_clusters_resource_id FOREIGN KEY (resource_id) REFERENCES adminresource(resource_id));

CREATE TABLE clusterconfig (
  config_id NUMBER(19) NOT NULL,
  version_tag VARCHAR2(255) NOT NULL,
  version NUMBER(19) NOT NULL,
  type_name VARCHAR2(255) NOT NULL,
  cluster_id NUMBER(19) NOT NULL,
  stack_id NUMBER(19) NOT NULL,
  selected NUMBER(1) DEFAULT 0 NOT NULL,
  config_data CLOB NOT NULL,
  config_attributes CLOB,
  create_timestamp NUMBER(19) NOT NULL,
  unmapped SMALLINT DEFAULT 0 NOT NULL,
  selected_timestamp NUMBER(19) DEFAULT 0 NOT NULL,
  CONSTRAINT PK_clusterconfig PRIMARY KEY (config_id),
  CONSTRAINT FK_clusterconfig_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id),
  CONSTRAINT FK_clusterconfig_stack_id FOREIGN KEY (stack_id) REFERENCES stack(stack_id),
  CONSTRAINT UQ_config_type_tag UNIQUE (cluster_id, type_name, version_tag),
  CONSTRAINT UQ_config_type_version UNIQUE (cluster_id, type_name, version));

CREATE TABLE ambari_configuration (
  category_name VARCHAR2(100) NOT NULL,
  property_name VARCHAR2(100) NOT NULL,
  property_value VARCHAR2(2048),
  CONSTRAINT PK_ambari_configuration PRIMARY KEY (category_name, property_name));

CREATE TABLE serviceconfig (
  service_config_id NUMBER(19) NOT NULL,
  cluster_id NUMBER(19) NOT NULL,
  service_name VARCHAR(255) NOT NULL,
  version NUMBER(19) NOT NULL,
  create_timestamp NUMBER(19) NOT NULL,
  stack_id NUMBER(19) NOT NULL,
  user_name VARCHAR(255) DEFAULT '_db' NOT NULL,
  group_id NUMBER(19),
  note CLOB,
  CONSTRAINT PK_serviceconfig PRIMARY KEY (service_config_id),
  CONSTRAINT FK_serviceconfig_stack_id FOREIGN KEY (stack_id) REFERENCES stack(stack_id),
  CONSTRAINT UQ_scv_service_version UNIQUE (cluster_id, service_name, version));


CREATE TABLE hosts (
  host_id NUMBER(19) NOT NULL,
  host_name VARCHAR2(255) NOT NULL,
  cpu_count INTEGER NOT NULL,
  cpu_info VARCHAR2(255) NULL,
  discovery_status VARCHAR2(2000) NULL,
  host_attributes CLOB NULL,
  ipv4 VARCHAR2(255) NULL,
  ipv6 VARCHAR2(255) NULL,
  last_registration_time INTEGER NOT NULL,
  os_arch VARCHAR2(255) NULL,
  os_info VARCHAR2(1000) NULL,
  os_type VARCHAR2(255) NULL,
  ph_cpu_count INTEGER NOT NULL,
  public_host_name VARCHAR2(255) NULL,
  rack_info VARCHAR2(255) NOT NULL,
  total_mem INTEGER NOT NULL,
  CONSTRAINT PK_hosts PRIMARY KEY (host_id),
  CONSTRAINT UQ_hosts_host_name UNIQUE (host_name));

CREATE TABLE serviceconfighosts (
  service_config_id NUMBER(19) NOT NULL,
  host_id NUMBER(19) NOT NULL,
  CONSTRAINT PK_serviceconfighosts PRIMARY KEY (service_config_id, host_id),
  CONSTRAINT FK_scvhosts_host_id FOREIGN KEY (host_id) REFERENCES hosts(host_id),
  CONSTRAINT FK_scvhosts_scv FOREIGN KEY (service_config_id) REFERENCES serviceconfig(service_config_id));

CREATE TABLE serviceconfigmapping (
  service_config_id NUMBER(19) NOT NULL,
  config_id NUMBER(19) NOT NULL,
  CONSTRAINT PK_serviceconfigmapping PRIMARY KEY (service_config_id, config_id),
  CONSTRAINT FK_scvm_config FOREIGN KEY (config_id) REFERENCES clusterconfig(config_id),
  CONSTRAINT FK_scvm_scv FOREIGN KEY (service_config_id) REFERENCES serviceconfig(service_config_id));

CREATE TABLE clusterservices (
  service_name VARCHAR2(255) NOT NULL,
  cluster_id NUMBER(19) NOT NULL,
  service_enabled NUMBER(10) NOT NULL,
  CONSTRAINT PK_clusterservices PRIMARY KEY (service_name, cluster_id),
  CONSTRAINT FK_clusterservices_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id));

CREATE TABLE clusterstate (
  cluster_id NUMBER(19) NOT NULL,
  current_cluster_state VARCHAR2(255) NULL,
  current_stack_id NUMBER(19) NULL,
  CONSTRAINT PK_clusterstate PRIMARY KEY (cluster_id),
  CONSTRAINT FK_clusterstate_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id),
  CONSTRAINT FK_cs_current_stack_id FOREIGN KEY (current_stack_id) REFERENCES stack(stack_id));

CREATE TABLE repo_version (
  repo_version_id NUMBER(19) NOT NULL,
  stack_id NUMBER(19) NOT NULL,
  version VARCHAR2(255) NOT NULL,
  display_name VARCHAR2(128) NOT NULL,
  repo_type VARCHAR2(255) DEFAULT 'STANDARD' NOT NULL,
  hidden NUMBER(1) DEFAULT 0 NOT NULL,
  resolved NUMBER(1) DEFAULT 0 NOT NULL,
  legacy NUMBER(1) DEFAULT 0 NOT NULL,
  version_url VARCHAR(1024),
  version_xml CLOB,
  version_xsd VARCHAR(512),
  parent_id NUMBER(19),
  CONSTRAINT PK_repo_version PRIMARY KEY (repo_version_id),
  CONSTRAINT FK_repoversion_stack_id FOREIGN KEY (stack_id) REFERENCES stack(stack_id),
  CONSTRAINT UQ_repo_version_display_name UNIQUE (display_name),
  CONSTRAINT UQ_repo_version_stack_id UNIQUE (stack_id, version));

CREATE TABLE repo_os (
  id NUMBER(19) NOT NULL,
  repo_version_id NUMBER(19) NOT NULL,
  family VARCHAR(255) DEFAULT '' NOT NULL,
  ambari_managed NUMBER(1) DEFAULT 1,
  CONSTRAINT PK_repo_os_id PRIMARY KEY (id),
  CONSTRAINT FK_repo_os_id_repo_version_id FOREIGN KEY (repo_version_id) REFERENCES repo_version (repo_version_id));

CREATE TABLE repo_definition (
  id NUMBER(19) NOT NULL,
  repo_os_id NUMBER(19),
  repo_name VARCHAR(255) NOT NULL,
  repo_id VARCHAR(255) NOT NULL,
  base_url CLOB NOT NULL,
  distribution CLOB,
  components CLOB,
  unique_repo NUMBER(1) DEFAULT 1,
  mirrors CLOB,
  CONSTRAINT PK_repo_definition_id PRIMARY KEY (id),
  CONSTRAINT FK_repo_definition_repo_os_id FOREIGN KEY (repo_os_id) REFERENCES repo_os (id));

CREATE TABLE repo_tags (
  repo_definition_id NUMBER(19) NOT NULL,
  tag VARCHAR(255) NOT NULL,
  CONSTRAINT FK_repo_tag_definition_id FOREIGN KEY (repo_definition_id) REFERENCES repo_definition (id));

CREATE TABLE repo_applicable_services (
  repo_definition_id NUMBER(19) NOT NULL,
  service_name VARCHAR(255) NOT NULL,
  CONSTRAINT FK_repo_app_service_def_id FOREIGN KEY (repo_definition_id) REFERENCES repo_definition (id));

CREATE TABLE servicecomponentdesiredstate (
  id NUMBER(19) NOT NULL,
  component_name VARCHAR2(255) NOT NULL,
  cluster_id NUMBER(19) NOT NULL,
  desired_repo_version_id NUMBER(19) NOT NULL,
  desired_state VARCHAR2(255) NOT NULL,
  service_name VARCHAR2(255) NOT NULL,
  recovery_enabled SMALLINT DEFAULT 0 NOT NULL,
  repo_state VARCHAR2(255) DEFAULT 'NOT_REQUIRED' NOT NULL,
  CONSTRAINT pk_sc_desiredstate PRIMARY KEY (id),
  CONSTRAINT UQ_scdesiredstate_name UNIQUE(component_name, service_name, cluster_id),
  CONSTRAINT FK_scds_desired_repo_id FOREIGN KEY (desired_repo_version_id) REFERENCES repo_version (repo_version_id),
  CONSTRAINT srvccmponentdesiredstatesrvcnm FOREIGN KEY (service_name, cluster_id) REFERENCES clusterservices (service_name, cluster_id));

CREATE TABLE hostcomponentdesiredstate (
  id NUMBER(19) NOT NULL,
  cluster_id NUMBER(19) NOT NULL,
  component_name VARCHAR2(255) NOT NULL,
  desired_state VARCHAR2(255) NOT NULL,
  host_id NUMBER(19) NOT NULL,
  service_name VARCHAR2(255) NOT NULL,
  admin_state VARCHAR2(32) NULL,
  maintenance_state VARCHAR2(32) NOT NULL,
  blueprint_provisioning_state VARCHAR2(255) DEFAULT 'NONE',
  restart_required NUMBER(1) DEFAULT 0 NOT NULL,
  CONSTRAINT PK_hostcomponentdesiredstate PRIMARY KEY (id),
  CONSTRAINT UQ_hcdesiredstate_name UNIQUE (component_name, service_name, host_id, cluster_id),
  CONSTRAINT FK_hcdesiredstate_host_id FOREIGN KEY (host_id) REFERENCES hosts (host_id),
  CONSTRAINT hstcmpnntdesiredstatecmpnntnme FOREIGN KEY (component_name, service_name, cluster_id) REFERENCES servicecomponentdesiredstate (component_name, service_name, cluster_id));

CREATE TABLE hostcomponentstate (
  id NUMBER(19) NOT NULL,
  cluster_id NUMBER(19) NOT NULL,
  component_name VARCHAR2(255) NOT NULL,
  version VARCHAR2(32) DEFAULT 'UNKNOWN' NOT NULL,
  current_state VARCHAR2(255) NOT NULL,
  last_live_state VARCHAR2(255) DEFAULT 'UNKNOWN' NOT NULL,
  host_id NUMBER(19) NOT NULL,
  service_name VARCHAR2(255) NOT NULL,
  upgrade_state VARCHAR2(32) DEFAULT 'NONE' NOT NULL,
  CONSTRAINT pk_hostcomponentstate PRIMARY KEY (id),
  CONSTRAINT FK_hostcomponentstate_host_id FOREIGN KEY (host_id) REFERENCES hosts (host_id),
  CONSTRAINT hstcomponentstatecomponentname FOREIGN KEY (component_name, service_name, cluster_id) REFERENCES servicecomponentdesiredstate (component_name, service_name, cluster_id));

CREATE INDEX idx_host_component_state on hostcomponentstate(host_id, component_name, service_name, cluster_id);

CREATE TABLE hoststate (
  agent_version VARCHAR2(255) NULL,
  available_mem NUMBER(19) NOT NULL,
  current_state VARCHAR2(255) NOT NULL,
  health_status VARCHAR2(255) NULL,
  host_id NUMBER(19) NOT NULL,
  time_in_state NUMBER(19) NOT NULL,
  maintenance_state VARCHAR2(512),
  CONSTRAINT PK_hoststate PRIMARY KEY (host_id),
  CONSTRAINT FK_hoststate_host_id FOREIGN KEY (host_id) REFERENCES hosts (host_id));

CREATE TABLE host_version (
  id NUMBER(19) NOT NULL,
  repo_version_id NUMBER(19) NOT NULL,
  host_id NUMBER(19) NOT NULL,
  state VARCHAR2(32) NOT NULL,
  CONSTRAINT PK_host_version PRIMARY KEY (id),
  CONSTRAINT FK_host_version_host_id FOREIGN KEY (host_id) REFERENCES hosts (host_id),
  CONSTRAINT FK_host_version_repovers_id FOREIGN KEY (repo_version_id) REFERENCES repo_version (repo_version_id),
  CONSTRAINT UQ_host_repo UNIQUE(host_id, repo_version_id));

CREATE TABLE servicedesiredstate (
  cluster_id NUMBER(19) NOT NULL,
  desired_host_role_mapping NUMBER(10) NOT NULL,
  desired_repo_version_id NUMBER(19) NOT NULL,
  desired_state VARCHAR2(255) NOT NULL,
  service_name VARCHAR2(255) NOT NULL,
  maintenance_state VARCHAR2(32) NOT NULL,
  credential_store_enabled SMALLINT DEFAULT 0 NOT NULL,
  CONSTRAINT PK_servicedesiredstate PRIMARY KEY (cluster_id, service_name),
  CONSTRAINT FK_repo_version_id FOREIGN KEY (desired_repo_version_id) REFERENCES repo_version (repo_version_id),
  CONSTRAINT servicedesiredstateservicename FOREIGN KEY (service_name, cluster_id) REFERENCES clusterservices (service_name, cluster_id));

CREATE TABLE adminprincipaltype (
  principal_type_id NUMBER(10) NOT NULL,
  principal_type_name VARCHAR(255) NOT NULL,
  CONSTRAINT PK_adminprincipaltype PRIMARY KEY (principal_type_id));

CREATE TABLE adminprincipal (
  principal_id NUMBER(19) NOT NULL,
  principal_type_id NUMBER(10) NOT NULL,
  CONSTRAINT PK_adminprincipal PRIMARY KEY (principal_id),
  CONSTRAINT FK_principal_principal_type_id FOREIGN KEY (principal_type_id) REFERENCES adminprincipaltype(principal_type_id));

CREATE TABLE users (
  user_id NUMBER(10) NOT NULL,
  principal_id NUMBER(19) NOT NULL,
  user_name VARCHAR2(255) NULL,
  active INTEGER DEFAULT 1 NOT NULL,
  consecutive_failures INTEGER DEFAULT 0 NOT NULL,
  active_widget_layouts VARCHAR2(1024) DEFAULT NULL,
  display_name VARCHAR2(255) NOT NULL,
  local_username VARCHAR2(255) NOT NULL,
  create_time NUMBER(19) NOT NULL,
  version NUMBER(19) DEFAULT 0 NOT NULL,
  CONSTRAINT PK_users PRIMARY KEY (user_id),
  CONSTRAINT FK_users_principal_id FOREIGN KEY (principal_id) REFERENCES adminprincipal(principal_id),
  CONSTRAINT UNQ_users_0 UNIQUE (user_name));

CREATE TABLE user_authentication (
  user_authentication_id NUMBER(10),
  user_id NUMBER(10) NOT NULL,
  authentication_type VARCHAR(50) NOT NULL,
  authentication_key VARCHAR(2048),
  create_time NUMBER(19) NOT NULL,
  update_time NUMBER(19) NOT NULL,
  CONSTRAINT PK_user_authentication PRIMARY KEY (user_authentication_id),
  CONSTRAINT FK_user_authentication_users FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE groups (
  group_id NUMBER(10) NOT NULL,
  principal_id NUMBER(19) NOT NULL,
  group_name VARCHAR2(255) NOT NULL,
  ldap_group NUMBER(10) DEFAULT 0,
  group_type VARCHAR(255) DEFAULT 'LOCAL' NOT NULL,
  CONSTRAINT PK_groups PRIMARY KEY (group_id),
  CONSTRAINT FK_groups_principal_id FOREIGN KEY (principal_id) REFERENCES adminprincipal(principal_id),
  CONSTRAINT UNQ_groups_0 UNIQUE (group_name, ldap_group));

CREATE TABLE members (
  member_id NUMBER(10),
  group_id NUMBER(10) NOT NULL,
  user_id NUMBER(10) NOT NULL,
  CONSTRAINT PK_members PRIMARY KEY (member_id),
  CONSTRAINT FK_members_group_id FOREIGN KEY (group_id) REFERENCES groups (group_id),
  CONSTRAINT FK_members_user_id FOREIGN KEY (user_id) REFERENCES users (user_id),
  CONSTRAINT UNQ_members_0 UNIQUE (group_id, user_id));

CREATE TABLE requestschedule (
  schedule_id NUMBER(19),
  cluster_id NUMBER(19) NOT NULL,
  description VARCHAR2(255),
  status VARCHAR2(255),
  batch_separation_seconds smallint,
  batch_toleration_limit smallint,
  batch_toleration_limit_per_batch smallint,
  pause_after_first_batch VARCHAR2(1),
  authenticated_user_id NUMBER(10),
  create_user VARCHAR2(255),
  create_timestamp NUMBER(19),
  update_user VARCHAR2(255),
  update_timestamp NUMBER(19),
  minutes VARCHAR2(10),
  hours VARCHAR2(10),
  days_of_month VARCHAR2(10),
  month VARCHAR2(10),
  day_of_week VARCHAR2(10),
  yearToSchedule VARCHAR2(10),
  startTime VARCHAR2(50),
  endTime VARCHAR2(50),
  last_execution_status VARCHAR2(255),
  CONSTRAINT PK_requestschedule PRIMARY KEY (schedule_id));

CREATE TABLE request (
  request_id NUMBER(19) NOT NULL,
  cluster_id NUMBER(19),
  request_schedule_id NUMBER(19),
  command_name VARCHAR(255),
  create_time NUMBER(19) NOT NULL,
  end_time NUMBER(19) NOT NULL,
  exclusive_execution NUMBER(1) DEFAULT 0 NOT NULL,
  inputs BLOB,
  request_context VARCHAR(255),
  request_type VARCHAR(255),
  start_time NUMBER(19) NOT NULL,
  status VARCHAR(255) DEFAULT 'PENDING' NOT NULL,
  display_status VARCHAR(255) DEFAULT 'PENDING' NOT NULL,
  cluster_host_info BLOB NOT NULL,
  user_name VARCHAR(255),
  CONSTRAINT PK_request PRIMARY KEY (request_id),
  CONSTRAINT FK_request_schedule_id FOREIGN KEY (request_schedule_id) REFERENCES requestschedule (schedule_id));

CREATE TABLE stage (
  stage_id NUMBER(19) NOT NULL,
  request_id NUMBER(19) NOT NULL,
  cluster_id NUMBER(19) NULL,
  skippable NUMBER(1) DEFAULT 0 NOT NULL,
  supports_auto_skip_failure NUMBER(1) DEFAULT 0 NOT NULL,
  log_info VARCHAR2(255) NULL,
  request_context VARCHAR2(255) NULL,
  command_params BLOB,
  host_params BLOB,
  command_execution_type VARCHAR2(32) DEFAULT 'STAGE' NOT NULL,
  status VARCHAR(255) DEFAULT 'PENDING' NOT NULL,
  display_status VARCHAR(255) DEFAULT 'PENDING' NOT NULL,
  CONSTRAINT PK_stage PRIMARY KEY (stage_id, request_id),
  CONSTRAINT FK_stage_request_id FOREIGN KEY (request_id) REFERENCES request (request_id));

CREATE TABLE host_role_command (
  task_id NUMBER(19) NOT NULL,
  attempt_count NUMBER(5) NOT NULL,
  retry_allowed NUMBER(1) DEFAULT 0 NOT NULL,
  event CLOB NULL,
  exitcode NUMBER(10) NOT NULL,
  host_id NUMBER(19),
  last_attempt_time NUMBER(19) NOT NULL,
  request_id NUMBER(19) NOT NULL,
  role VARCHAR2(255) NULL,
  role_command VARCHAR2(255) NULL,
  stage_id NUMBER(19) NOT NULL,
  start_time NUMBER(19) NOT NULL,
  original_start_time NUMBER(19) NOT NULL,
  end_time NUMBER(19),
  status VARCHAR2(255) DEFAULT 'PENDING' NOT NULL,
  auto_skip_on_failure NUMBER(1) DEFAULT 0 NOT NULL,
  std_error BLOB NULL,
  std_out BLOB NULL,
  output_log VARCHAR2(255) NULL,
  error_log VARCHAR2(255) NULL,
  structured_out BLOB NULL,
  command_detail VARCHAR2(255) NULL,
  custom_command_name VARCHAR2(255) NULL,
  ops_display_name VARCHAR2(255),
  is_background SMALLINT DEFAULT 0 NOT NULL,
  CONSTRAINT PK_host_role_command PRIMARY KEY (task_id),
  CONSTRAINT FK_host_role_command_host_id FOREIGN KEY (host_id) REFERENCES hosts (host_id),
  CONSTRAINT FK_host_role_command_stage_id FOREIGN KEY (stage_id, request_id) REFERENCES stage (stage_id, request_id));

CREATE TABLE execution_command (
  task_id NUMBER(19) NOT NULL,
  command BLOB NULL,
  CONSTRAINT PK_execution_command PRIMARY KEY (task_id),
  CONSTRAINT FK_execution_command_task_id FOREIGN KEY (task_id) REFERENCES host_role_command (task_id));

CREATE TABLE role_success_criteria (
  role VARCHAR2(255) NOT NULL,
  request_id NUMBER(19) NOT NULL,
  stage_id NUMBER(19) NOT NULL,
  success_factor NUMBER(19, 4) NOT NULL,
  CONSTRAINT PK_role_success_criteria PRIMARY KEY (role, request_id, stage_id),
  CONSTRAINT role_success_criteria_stage_id FOREIGN KEY (stage_id, request_id) REFERENCES stage (stage_id, request_id));

CREATE TABLE requestresourcefilter (
  filter_id NUMBER(19) NOT NULL,
  request_id NUMBER(19) NOT NULL,
  service_name VARCHAR2(255),
  component_name VARCHAR2(255),
  hosts BLOB,
  CONSTRAINT PK_requestresourcefilter PRIMARY KEY (filter_id),
  CONSTRAINT FK_reqresfilter_req_id FOREIGN KEY (request_id) REFERENCES request (request_id));

CREATE TABLE requestoperationlevel (
  operation_level_id NUMBER(19) NOT NULL,
  request_id NUMBER(19) NOT NULL,
  level_name VARCHAR2(255),
  cluster_name VARCHAR2(255),
  service_name VARCHAR2(255),
  host_component_name VARCHAR2(255),
  host_id NUMBER(19) NULL,      -- unlike most host_id columns, this one allows NULLs because the request can be at the service level
  CONSTRAINT PK_requestoperationlevel PRIMARY KEY (operation_level_id),
  CONSTRAINT FK_req_op_level_req_id FOREIGN KEY (request_id) REFERENCES request (request_id));

CREATE TABLE key_value_store (
  "key" VARCHAR2(255) NOT NULL,
  "value" CLOB NULL,
  CONSTRAINT PK_key_value_store PRIMARY KEY ("key"));

CREATE TABLE hostconfigmapping (
  create_timestamp NUMBER(19) NOT NULL,
  host_id NUMBER(19) NOT NULL,
  cluster_id NUMBER(19) NOT NULL,
  type_name VARCHAR2(255) NOT NULL,
  selected NUMBER(10) NOT NULL,
  service_name VARCHAR2(255) NULL,
  version_tag VARCHAR2(255) NOT NULL,
  user_name VARCHAR(255) DEFAULT '_db',
  CONSTRAINT PK_hostconfigmapping PRIMARY KEY (create_timestamp, host_id, cluster_id, type_name),
  CONSTRAINT FK_hostconfmapping_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id),
  CONSTRAINT FK_hostconfmapping_host_id FOREIGN KEY (host_id) REFERENCES hosts (host_id));

CREATE TABLE metainfo (
  "metainfo_key" VARCHAR2(255) NOT NULL,
  "metainfo_value" CLOB NULL,
  CONSTRAINT PK_metainfo PRIMARY KEY ("metainfo_key"));

CREATE TABLE ClusterHostMapping (
  cluster_id NUMBER(19) NOT NULL,
  host_id NUMBER(19) NOT NULL,
  CONSTRAINT PK_ClusterHostMapping PRIMARY KEY (cluster_id, host_id),
  CONSTRAINT FK_clhostmapping_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id),
  CONSTRAINT FK_clusterhostmapping_host_id FOREIGN KEY (host_id) REFERENCES hosts (host_id));

CREATE TABLE ambari_sequences (
  sequence_name VARCHAR2(50) NOT NULL,
  sequence_value NUMBER(38) NULL,
  CONSTRAINT PK_ambari_sequences PRIMARY KEY (sequence_name));

CREATE TABLE configgroup (
  group_id NUMBER(19),
  cluster_id NUMBER(19) NOT NULL,
  group_name VARCHAR2(255) NOT NULL,
  tag VARCHAR2(1024) NOT NULL,
  description VARCHAR2(1024),
  create_timestamp NUMBER(19) NOT NULL,
  service_name VARCHAR(255),
  CONSTRAINT PK_configgroup PRIMARY KEY (group_id),
  CONSTRAINT FK_configgroup_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id));

CREATE TABLE confgroupclusterconfigmapping (
  config_group_id NUMBER(19) NOT NULL,
  cluster_id NUMBER(19) NOT NULL,
  config_type VARCHAR2(255) NOT NULL,
  version_tag VARCHAR2(255) NOT NULL,
  user_name VARCHAR2(255) DEFAULT '_db',
  create_timestamp NUMBER(19) NOT NULL,
  CONSTRAINT PK_confgroupclustercfgmapping PRIMARY KEY (config_group_id, cluster_id, config_type),
  CONSTRAINT FK_cgccm_gid FOREIGN KEY (config_group_id) REFERENCES configgroup (group_id),
  CONSTRAINT FK_confg FOREIGN KEY (version_tag, config_type, cluster_id) REFERENCES clusterconfig (version_tag, type_name, cluster_id));

CREATE TABLE configgrouphostmapping (
  config_group_id NUMBER(19) NOT NULL,
  host_id NUMBER(19) NOT NULL,
  CONSTRAINT PK_configgrouphostmapping PRIMARY KEY (config_group_id, host_id),
  CONSTRAINT FK_cghm_cgid FOREIGN KEY (config_group_id) REFERENCES configgroup (group_id),
  CONSTRAINT FK_cghm_host_id FOREIGN KEY (host_id) REFERENCES hosts (host_id));

CREATE TABLE requestschedulebatchrequest (
  schedule_id NUMBER(19),
  batch_id NUMBER(19),
  request_id NUMBER(19),
  request_type VARCHAR2(255),
  request_uri VARCHAR2(1024),
  request_body BLOB,
  request_status VARCHAR2(255),
  return_code smallint,
  return_message VARCHAR2(2000),
  CONSTRAINT PK_requestschedulebatchrequest PRIMARY KEY (schedule_id, batch_id),
  CONSTRAINT FK_rsbatchrequest_schedule_id FOREIGN KEY (schedule_id) REFERENCES requestschedule (schedule_id));

CREATE TABLE blueprint (
  blueprint_name VARCHAR2(255) NOT NULL,
  stack_id NUMBER(19) NOT NULL,
  security_type VARCHAR2(32) DEFAULT 'NONE' NOT NULL,
  security_descriptor_reference VARCHAR(255),
  CONSTRAINT PK_blueprint PRIMARY KEY (blueprint_name),
  CONSTRAINT FK_blueprint_stack_id FOREIGN KEY (stack_id) REFERENCES stack(stack_id));

CREATE TABLE hostgroup (
  blueprint_name VARCHAR2(255) NOT NULL,
  name VARCHAR2(255) NOT NULL,
  cardinality VARCHAR2(255) NOT NULL,
  CONSTRAINT PK_hostgroup PRIMARY KEY (blueprint_name, name),
  CONSTRAINT FK_hg_blueprint_name FOREIGN KEY (blueprint_name) REFERENCES blueprint(blueprint_name));

CREATE TABLE hostgroup_component (
  blueprint_name VARCHAR2(255) NOT NULL,
  hostgroup_name VARCHAR2(255) NOT NULL,
  name VARCHAR2(255) NOT NULL,
  provision_action VARCHAR2(255),
  CONSTRAINT PK_hostgroup_component PRIMARY KEY (blueprint_name, hostgroup_name, name),
  CONSTRAINT FK_hgc_blueprint_name FOREIGN KEY (blueprint_name, hostgroup_name) REFERENCES hostgroup(blueprint_name, name));

CREATE TABLE blueprint_configuration (
  blueprint_name VARCHAR2(255) NOT NULL,
  type_name VARCHAR2(255) NOT NULL,
  config_data CLOB NOT NULL,
  config_attributes CLOB,
  CONSTRAINT PK_blueprint_configuration PRIMARY KEY (blueprint_name, type_name),
  CONSTRAINT FK_cfg_blueprint_name FOREIGN KEY (blueprint_name) REFERENCES blueprint(blueprint_name));

CREATE TABLE blueprint_setting (
  id NUMBER(19) NOT NULL,
  blueprint_name VARCHAR2(255) NOT NULL,
  setting_name VARCHAR2(255) NOT NULL,
  setting_data CLOB NOT NULL,
  CONSTRAINT PK_blueprint_setting PRIMARY KEY (id),
  CONSTRAINT UQ_blueprint_setting_name UNIQUE(blueprint_name,setting_name),
  CONSTRAINT FK_blueprint_setting_name FOREIGN KEY (blueprint_name) REFERENCES blueprint(blueprint_name));

CREATE TABLE hostgroup_configuration (
  blueprint_name VARCHAR2(255) NOT NULL,
  hostgroup_name VARCHAR2(255) NOT NULL,
  type_name VARCHAR2(255) NOT NULL,
  config_data CLOB NOT NULL,
  config_attributes CLOB,
  CONSTRAINT PK_hostgroup_configuration PRIMARY KEY (blueprint_name, hostgroup_name, type_name),
  CONSTRAINT FK_hg_cfg_bp_hg_name FOREIGN KEY (blueprint_name, hostgroup_name) REFERENCES hostgroup(blueprint_name, name));

CREATE TABLE viewmain (view_name VARCHAR(255) NOT NULL,
  label VARCHAR(255),
  description VARCHAR(2048),
  version VARCHAR(255),
  build VARCHAR(128),
  resource_type_id NUMBER(10) NOT NULL,
  icon VARCHAR(255),
  icon64 VARCHAR(255),
  archive VARCHAR(255),
  mask VARCHAR(255),
  system_view NUMBER(1) DEFAULT 0 NOT NULL,
  CONSTRAINT PK_viewmain PRIMARY KEY (view_name),
  CONSTRAINT FK_view_resource_type_id FOREIGN KEY (resource_type_id) REFERENCES adminresourcetype(resource_type_id));


CREATE table viewurl(
  url_id NUMBER ,
  url_name VARCHAR(255) NOT NULL ,
  url_suffix VARCHAR(255) NOT NULL,
  CONSTRAINT PK_viewurl PRIMARY KEY(url_id)
);


CREATE TABLE viewinstance (
  view_instance_id NUMBER(19),
  resource_id NUMBER(19) NOT NULL,
  view_name VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  label VARCHAR(255),
  description VARCHAR(2048),
  visible CHAR(1),
  icon VARCHAR(255),
  icon64 VARCHAR(255),
  xml_driven CHAR(1),
  alter_names NUMBER(1) DEFAULT 1 NOT NULL,
  cluster_handle NUMBER(19),
  cluster_type VARCHAR(100) DEFAULT 'LOCAL_AMBARI' NOT NULL,
  short_url NUMBER,
  CONSTRAINT PK_viewinstance PRIMARY KEY (view_instance_id),
  CONSTRAINT FK_instance_url_id FOREIGN KEY (short_url) REFERENCES viewurl(url_id),
  CONSTRAINT FK_viewinst_view_name FOREIGN KEY (view_name) REFERENCES viewmain(view_name),
  CONSTRAINT FK_viewinstance_resource_id FOREIGN KEY (resource_id) REFERENCES adminresource(resource_id),
  CONSTRAINT UQ_viewinstance_name UNIQUE (view_name, name),
  CONSTRAINT UQ_viewinstance_name_id UNIQUE (view_instance_id, view_name, name));

CREATE TABLE viewinstancedata (
  view_instance_id NUMBER(19),
  view_name VARCHAR(255) NOT NULL,
  view_instance_name VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  user_name VARCHAR(255) NOT NULL,
  value VARCHAR(2000),
  CONSTRAINT PK_viewinstancedata PRIMARY KEY (view_instance_id, name, user_name),
  CONSTRAINT FK_viewinstdata_view_name FOREIGN KEY (view_instance_id, view_name, view_instance_name) REFERENCES viewinstance(view_instance_id, view_name, name));

CREATE TABLE viewinstanceproperty (
  view_name VARCHAR(255) NOT NULL,
  view_instance_name VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  value VARCHAR(2000),
  CONSTRAINT PK_viewinstanceproperty PRIMARY KEY (view_name, view_instance_name, name),
  CONSTRAINT FK_viewinstprop_view_name FOREIGN KEY (view_name, view_instance_name) REFERENCES viewinstance(view_name, name));

CREATE TABLE viewparameter (
  view_name VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(2048),
  label VARCHAR(255),
  placeholder VARCHAR(255),
  default_value VARCHAR(2000),
  cluster_config VARCHAR(255),
  required CHAR(1),
  masked CHAR(1),
  CONSTRAINT PK_viewparameter PRIMARY KEY (view_name, name),
  CONSTRAINT FK_viewparam_view_name FOREIGN KEY (view_name) REFERENCES viewmain(view_name));

CREATE TABLE viewresource (view_name VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  plural_name VARCHAR(255),
  id_property VARCHAR(255),
  subResource_names VARCHAR(255),
  provider VARCHAR(255),
  service VARCHAR(255),
  "resource" VARCHAR(255),
  CONSTRAINT PK_viewresource PRIMARY KEY (view_name, name),
  CONSTRAINT FK_viewres_view_name FOREIGN KEY (view_name) REFERENCES viewmain(view_name));

CREATE TABLE viewentity (
  id NUMBER(19) NOT NULL,
  view_name VARCHAR(255) NOT NULL,
  view_instance_name VARCHAR(255) NOT NULL,
  class_name VARCHAR(255) NOT NULL,
  id_property VARCHAR(255),
  CONSTRAINT PK_viewentity PRIMARY KEY (id),
  CONSTRAINT FK_viewentity_view_name FOREIGN KEY (view_name, view_instance_name) REFERENCES viewinstance(view_name, name));

CREATE TABLE adminpermission (
  permission_id NUMBER(19) NOT NULL,
  permission_name VARCHAR(255) NOT NULL,
  resource_type_id NUMBER(10) NOT NULL,
  permission_label VARCHAR(255),
  principal_id NUMBER(19) NOT NULL,
  sort_order SMALLINT DEFAULT 1 NOT NULL,
  CONSTRAINT PK_adminpermission PRIMARY KEY (permission_id),
  CONSTRAINT FK_permission_resource_type_id FOREIGN KEY (resource_type_id) REFERENCES adminresourcetype(resource_type_id),
  CONSTRAINT FK_permission_principal_id FOREIGN KEY (principal_id) REFERENCES adminprincipal(principal_id),
  CONSTRAINT UQ_perm_name_resource_type_id UNIQUE (permission_name, resource_type_id));

CREATE TABLE roleauthorization (
  authorization_id VARCHAR(100) NOT NULL,
  authorization_name VARCHAR(255) NOT NULL,
  CONSTRAINT PK_roleauthorization PRIMARY KEY (authorization_id));

CREATE TABLE permission_roleauthorization (
  permission_id NUMBER(19) NOT NULL,
  authorization_id VARCHAR(100) NOT NULL,
  CONSTRAINT PK_permsn_roleauthorization PRIMARY KEY (permission_id, authorization_id),
  CONSTRAINT FK_permission_roleauth_aid FOREIGN KEY (authorization_id) REFERENCES roleauthorization(authorization_id),
  CONSTRAINT FK_permission_roleauth_pid FOREIGN KEY (permission_id) REFERENCES adminpermission(permission_id));

CREATE TABLE adminprivilege (
  privilege_id NUMBER(19),
  permission_id NUMBER(19) NOT NULL,
  resource_id NUMBER(19) NOT NULL,
  principal_id NUMBER(19) NOT NULL,
  CONSTRAINT PK_adminprivilege PRIMARY KEY (privilege_id),
  CONSTRAINT FK_privilege_permission_id FOREIGN KEY (permission_id) REFERENCES adminpermission(permission_id),
  CONSTRAINT FK_privilege_principal_id FOREIGN KEY (principal_id) REFERENCES adminprincipal(principal_id),
  CONSTRAINT FK_privilege_resource_id FOREIGN KEY (resource_id) REFERENCES adminresource(resource_id));

CREATE TABLE widget (
  id NUMBER(19) NOT NULL,
  widget_name VARCHAR2(255) NOT NULL,
  widget_type VARCHAR2(255) NOT NULL,
  metrics CLOB,
  time_created NUMBER(19) NOT NULL,
  author VARCHAR2(255),
  description VARCHAR2(2048),
  default_section_name VARCHAR2(255),
  scope VARCHAR2(255),
  widget_values CLOB,
  properties CLOB,
  cluster_id NUMBER(19) NOT NULL,
  tag VARCHAR2(255),
  CONSTRAINT PK_widget PRIMARY KEY (id)
);

CREATE TABLE widget_layout (
  id NUMBER(19) NOT NULL,
  layout_name VARCHAR2(255) NOT NULL,
  section_name VARCHAR2(255) NOT NULL,
  scope VARCHAR2(255) NOT NULL,
  user_name VARCHAR2(255) NOT NULL,
  display_name VARCHAR2(255),
  cluster_id NUMBER(19) NOT NULL,
  CONSTRAINT PK_widget_layout PRIMARY KEY (id)
);

CREATE TABLE widget_layout_user_widget (
  widget_layout_id NUMBER(19) NOT NULL,
  widget_id NUMBER(19) NOT NULL,
  widget_order smallint,
  CONSTRAINT PK_widget_layout_user_widget PRIMARY KEY (widget_layout_id, widget_id),
  CONSTRAINT FK_widget_id FOREIGN KEY (widget_id) REFERENCES widget(id),
  CONSTRAINT FK_widget_layout_id FOREIGN KEY (widget_layout_id) REFERENCES widget_layout(id));

CREATE TABLE artifact (
  artifact_name VARCHAR2(255) NOT NULL,
  foreign_keys VARCHAR2(255) NOT NULL,
  artifact_data CLOB NOT NULL,
  CONSTRAINT PK_artifact PRIMARY KEY (artifact_name, foreign_keys));

CREATE TABLE topology_request (
  id NUMBER(19) NOT NULL,
  action VARCHAR(255) NOT NULL,
  cluster_id NUMBER(19) NOT NULL,
  bp_name VARCHAR(100) NOT NULL,
  cluster_properties CLOB,
  cluster_attributes CLOB,
  description VARCHAR(1024),
  provision_action VARCHAR(255),
  CONSTRAINT PK_topology_request PRIMARY KEY (id),
  CONSTRAINT FK_topology_request_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters(cluster_id));

CREATE TABLE topology_hostgroup (
  id NUMBER(19) NOT NULL,
  name VARCHAR(255) NOT NULL,
  group_properties CLOB,
  group_attributes CLOB,
  request_id NUMBER(19) NOT NULL,
  CONSTRAINT PK_topology_hostgroup PRIMARY KEY (id),
  CONSTRAINT FK_hostgroup_req_id FOREIGN KEY (request_id) REFERENCES topology_request(id));

CREATE TABLE topology_host_info (
  id NUMBER(19) NOT NULL,
  group_id NUMBER(19) NOT NULL,
  fqdn VARCHAR(255),
  host_id NUMBER(19),
  host_count INTEGER,
  predicate VARCHAR(2048),
  rack_info VARCHAR(255),
  CONSTRAINT PK_topology_host_info PRIMARY KEY (id),
  CONSTRAINT FK_hostinfo_group_id FOREIGN KEY (group_id) REFERENCES topology_hostgroup(id),
  CONSTRAINT FK_hostinfo_host_id FOREIGN KEY (host_id) REFERENCES hosts(host_id));

CREATE TABLE topology_logical_request (
  id NUMBER(19) NOT NULL,
  request_id NUMBER(19) NOT NULL,
  description VARCHAR(1024),
  CONSTRAINT PK_topology_logical_request PRIMARY KEY (id),
  CONSTRAINT FK_logicalreq_req_id FOREIGN KEY (request_id) REFERENCES topology_request(id));

CREATE TABLE topology_host_request (
  id NUMBER(19) NOT NULL,
  logical_request_id NUMBER(19) NOT NULL,
  group_id NUMBER(19) NOT NULL,
  stage_id NUMBER(19) NOT NULL,
  host_name VARCHAR(255),
  status VARCHAR2(255),
  status_message VARCHAR2(1024),
  CONSTRAINT PK_topology_host_request PRIMARY KEY (id),
  CONSTRAINT FK_hostreq_group_id FOREIGN KEY (group_id) REFERENCES topology_hostgroup(id),
  CONSTRAINT FK_hostreq_logicalreq_id FOREIGN KEY (logical_request_id) REFERENCES topology_logical_request(id));

CREATE TABLE topology_host_task (
  id NUMBER(19) NOT NULL,
  host_request_id NUMBER(19) NOT NULL,
  type VARCHAR(255) NOT NULL,
  CONSTRAINT PK_topology_host_task PRIMARY KEY (id),
  CONSTRAINT FK_hosttask_req_id FOREIGN KEY (host_request_id) REFERENCES topology_host_request (id));

CREATE TABLE topology_logical_task (
  id NUMBER(19) NOT NULL,
  host_task_id NUMBER(19) NOT NULL,
  physical_task_id NUMBER(19),
  component VARCHAR(255) NOT NULL,
  CONSTRAINT PK_topology_logical_task PRIMARY KEY (id),
  CONSTRAINT FK_ltask_hosttask_id FOREIGN KEY (host_task_id) REFERENCES topology_host_task (id),
  CONSTRAINT FK_ltask_hrc_id FOREIGN KEY (physical_task_id) REFERENCES host_role_command (task_id));

CREATE TABLE setting (
  id NUMBER(19) NOT NULL,
  name VARCHAR(255) NOT NULL UNIQUE,
  setting_type VARCHAR(255) NOT NULL,
  content CLOB NOT NULL,
  updated_by VARCHAR(255) DEFAULT '_db' NOT NULL,
  update_timestamp NUMBER(19) NOT NULL,
  CONSTRAINT PK_setting PRIMARY KEY (id)
);

-- Remote Cluster table

CREATE TABLE remoteambaricluster(
  cluster_id NUMBER(19) NOT NULL,
  name VARCHAR(255) NOT NULL,
  username VARCHAR(255) NOT NULL,
  url VARCHAR(255) NOT NULL,
  password VARCHAR(255) NOT NULL,
  CONSTRAINT PK_remote_ambari_cluster PRIMARY KEY (cluster_id),
  CONSTRAINT UQ_remote_ambari_cluster UNIQUE (name));

CREATE TABLE remoteambariclusterservice(
  id NUMBER(19) NOT NULL,
  cluster_id NUMBER(19) NOT NULL,
  service_name VARCHAR(255) NOT NULL,
  CONSTRAINT PK_remote_ambari_service PRIMARY KEY (id),
  CONSTRAINT FK_remote_ambari_cluster_id FOREIGN KEY (cluster_id) REFERENCES remoteambaricluster(cluster_id)
);

-- Remote Cluster table ends

-- upgrade tables
CREATE TABLE upgrade (
  upgrade_id NUMBER(19) NOT NULL,
  cluster_id NUMBER(19) NOT NULL,
  request_id NUMBER(19) NOT NULL,
  direction VARCHAR2(255) DEFAULT 'UPGRADE' NOT NULL,
  orchestration VARCHAR2(255) DEFAULT 'STANDARD' NOT NULL,
  upgrade_package VARCHAR2(255) NOT NULL,
  upgrade_package_stack VARCHAR2(255) NOT NULL,
  upgrade_type VARCHAR2(32) NOT NULL,
  repo_version_id NUMBER(19) NOT NULL,
  skip_failures NUMBER(1) DEFAULT 0 NOT NULL,
  skip_sc_failures NUMBER(1) DEFAULT 0 NOT NULL,
  downgrade_allowed NUMBER(1) DEFAULT 1 NOT NULL,
  revert_allowed NUMBER(1) DEFAULT 0 NOT NULL,
  suspended NUMBER(1) DEFAULT 0 NOT NULL,
  CONSTRAINT PK_upgrade PRIMARY KEY (upgrade_id),
  FOREIGN KEY (cluster_id) REFERENCES clusters(cluster_id),
  FOREIGN KEY (request_id) REFERENCES request(request_id),
  FOREIGN KEY (repo_version_id) REFERENCES repo_version(repo_version_id)
);

CREATE TABLE upgrade_group (
  upgrade_group_id NUMBER(19) NOT NULL,
  upgrade_id NUMBER(19) NOT NULL,
  group_name VARCHAR2(255) DEFAULT '' NOT NULL,
  group_title VARCHAR2(1024) DEFAULT '' NOT NULL,
  CONSTRAINT PK_upgrade_group PRIMARY KEY (upgrade_group_id),
  FOREIGN KEY (upgrade_id) REFERENCES upgrade(upgrade_id)
);

CREATE TABLE upgrade_item (
  upgrade_item_id NUMBER(19) NOT NULL,
  upgrade_group_id NUMBER(19) NOT NULL,
  stage_id NUMBER(19) NOT NULL,
  state VARCHAR2(255) DEFAULT 'NONE' NOT NULL,
  hosts CLOB,
  tasks CLOB,
  item_text CLOB,
  CONSTRAINT PK_upgrade_item PRIMARY KEY (upgrade_item_id),
  FOREIGN KEY (upgrade_group_id) REFERENCES upgrade_group(upgrade_group_id)
);

CREATE TABLE upgrade_history(
  id NUMBER(19) NOT NULL,
  upgrade_id NUMBER(19) NOT NULL,
  service_name VARCHAR2(255) NOT NULL,
  component_name VARCHAR2(255) NOT NULL,
  from_repo_version_id NUMBER(19) NOT NULL,
  target_repo_version_id NUMBER(19) NOT NULL,
  CONSTRAINT PK_upgrade_hist PRIMARY KEY (id),
  CONSTRAINT FK_upgrade_hist_upgrade_id FOREIGN KEY (upgrade_id) REFERENCES upgrade (upgrade_id),
  CONSTRAINT FK_upgrade_hist_from_repo FOREIGN KEY (from_repo_version_id) REFERENCES repo_version (repo_version_id),
  CONSTRAINT FK_upgrade_hist_target_repo FOREIGN KEY (target_repo_version_id) REFERENCES repo_version (repo_version_id),
  CONSTRAINT UQ_upgrade_hist UNIQUE (upgrade_id, component_name, service_name)
);

CREATE TABLE servicecomponent_version(
  id NUMBER(19) NOT NULL,
  component_id NUMBER(19) NOT NULL,
  repo_version_id NUMBER(19) NOT NULL,
  state VARCHAR2(32) NOT NULL,
  user_name VARCHAR2(255) NOT NULL,
  CONSTRAINT PK_sc_version PRIMARY KEY (id),
  CONSTRAINT FK_scv_component_id FOREIGN KEY (component_id) REFERENCES servicecomponentdesiredstate (id),
  CONSTRAINT FK_scv_repo_version_id FOREIGN KEY (repo_version_id) REFERENCES repo_version (repo_version_id)
);

CREATE TABLE ambari_operation_history(
  id NUMBER(19) NOT NULL,
  from_version VARCHAR2(255) NOT NULL,
  to_version VARCHAR2(255) NOT NULL,
  start_time NUMBER(19) NOT NULL,
  end_time NUMBER(19),
  operation_type VARCHAR2(255) NOT NULL,
  comments CLOB,
  CONSTRAINT PK_ambari_operation_history PRIMARY KEY (id)
);

-- tasks indices --
CREATE INDEX idx_stage_request_id ON stage (request_id);
CREATE INDEX idx_hrc_request_id ON host_role_command (request_id);
CREATE INDEX idx_hrc_status_role ON host_role_command (status, role);
CREATE INDEX idx_rsc_request_id ON role_success_criteria (request_id);

-------- altering tables by creating foreign keys ----------
-- #1: This should always be an exceptional case. FK constraints should be inlined in table definitions when possible
--     (reorder table definitions if necessary).
-- #2: Oracle has a limitation of 30 chars in the constraint names name, and we should use the same constraint names in all DB types.
ALTER TABLE clusters ADD CONSTRAINT FK_clusters_upgrade_id FOREIGN KEY (upgrade_id) REFERENCES upgrade (upgrade_id);

-- Kerberos
CREATE TABLE kerberos_principal (
  principal_name VARCHAR2(255) NOT NULL,
  is_service NUMBER(1) DEFAULT 1 NOT NULL,
  cached_keytab_path VARCHAR2(255),
  CONSTRAINT PK_kerberos_principal PRIMARY KEY (principal_name)
);

CREATE TABLE kerberos_keytab (
  keytab_path VARCHAR2(255) NOT NULL,
  owner_name VARCHAR2(255),
  owner_access VARCHAR2(255),
  group_name VARCHAR2(255),
  group_access VARCHAR2(255),
  is_ambari_keytab NUMBER(1) DEFAULT 0 NOT NULL,
  write_ambari_jaas NUMBER(1) DEFAULT 0 NOT NULL,
  CONSTRAINT PK_kerberos_keytab PRIMARY KEY (keytab_path)
);

CREATE TABLE kerberos_keytab_principal (
  kkp_id NUMBER(19) DEFAULT 0 NOT NULL,
  keytab_path VARCHAR2(255) NOT NULL,
  principal_name VARCHAR2(255) NOT NULL,
  host_id NUMBER(19),
  is_distributed NUMBER(1) DEFAULT 0 NOT NULL,
  CONSTRAINT PK_kkp PRIMARY KEY (kkp_id),
  CONSTRAINT FK_kkp_keytab_path FOREIGN KEY (keytab_path) REFERENCES kerberos_keytab (keytab_path),
  CONSTRAINT FK_kkp_host_id FOREIGN KEY (host_id) REFERENCES hosts (host_id),
  CONSTRAINT FK_kkp_principal_name FOREIGN KEY (principal_name) REFERENCES kerberos_principal (principal_name),
  CONSTRAINT UNI_kkp UNIQUE(keytab_path, principal_name, host_id)
);

CREATE TABLE kkp_mapping_service (
  kkp_id NUMBER(19) DEFAULT 0 NOT NULL,
  service_name VARCHAR(255) NOT NULL,
  component_name VARCHAR(255) NOT NULL,
  CONSTRAINT PK_kkp_mapping_service PRIMARY KEY (kkp_id, service_name, component_name),
  CONSTRAINT FK_kkp_service_principal FOREIGN KEY (kkp_id) REFERENCES kerberos_keytab_principal (kkp_id)
);

CREATE TABLE kerberos_descriptor
(
   kerberos_descriptor_name   VARCHAR2(255) NOT NULL,
   kerberos_descriptor        CLOB NOT NULL,
   CONSTRAINT PK_kerberos_descriptor PRIMARY KEY (kerberos_descriptor_name)
);

-- Kerberos (end)

-- Alerting Framework
CREATE TABLE alert_definition (
  definition_id NUMBER(19) NOT NULL,
  cluster_id NUMBER(19) NOT NULL,
  definition_name VARCHAR2(255) NOT NULL,
  service_name VARCHAR2(255) NOT NULL,
  component_name VARCHAR2(255),
  scope VARCHAR2(255) DEFAULT 'ANY' NOT NULL,
  label VARCHAR2(255),
  help_url VARCHAR2(512),
  description CLOB,
  enabled NUMBER(1) DEFAULT 1 NOT NULL,
  schedule_interval NUMBER(10) NOT NULL,
  source_type VARCHAR2(255) NOT NULL,
  alert_source CLOB NOT NULL,
  hash VARCHAR2(64) NOT NULL,
  ignore_host NUMBER(1) DEFAULT 0 NOT NULL,
  repeat_tolerance NUMBER(10) DEFAULT 1 NOT NULL,
  repeat_tolerance_enabled NUMBER(1) DEFAULT 0 NOT NULL,
  CONSTRAINT PK_alert_definition PRIMARY KEY (definition_id),
  FOREIGN KEY (cluster_id) REFERENCES clusters(cluster_id),
  CONSTRAINT uni_alert_def_name UNIQUE(cluster_id,definition_name)
);

CREATE TABLE alert_history (
  alert_id NUMBER(19) NOT NULL,
  cluster_id NUMBER(19) NOT NULL,
  alert_definition_id NUMBER(19) NOT NULL,
  service_name VARCHAR2(255) NOT NULL,
  component_name VARCHAR2(255),
  host_name VARCHAR2(255),
  alert_instance VARCHAR2(255),
  alert_timestamp NUMBER(19) NOT NULL,
  alert_label VARCHAR2(1024),
  alert_state VARCHAR2(255) NOT NULL,
  alert_text CLOB,
  CONSTRAINT PK_alert_history PRIMARY KEY (alert_id),
  FOREIGN KEY (alert_definition_id) REFERENCES alert_definition(definition_id),
  FOREIGN KEY (cluster_id) REFERENCES clusters(cluster_id)
);

CREATE TABLE alert_current (
  alert_id NUMBER(19) NOT NULL,
  definition_id NUMBER(19) NOT NULL,
  history_id NUMBER(19) NOT NULL UNIQUE,
  maintenance_state VARCHAR2(255) NOT NULL,
  original_timestamp NUMBER(19) NOT NULL,
  latest_timestamp NUMBER(19) NOT NULL,
  latest_text CLOB,
  occurrences NUMBER(19) DEFAULT 1 NOT NULL,
  firmness VARCHAR2(255) DEFAULT 'HARD' NOT NULL,
  CONSTRAINT PK_alert_current PRIMARY KEY (alert_id),
  FOREIGN KEY (definition_id) REFERENCES alert_definition(definition_id),
  FOREIGN KEY (history_id) REFERENCES alert_history(alert_id)
);

CREATE TABLE alert_group (
  group_id NUMBER(19) NOT NULL,
  cluster_id NUMBER(19) NOT NULL,
  group_name VARCHAR2(255) NOT NULL,
  is_default NUMBER(1) DEFAULT 0 NOT NULL,
  service_name VARCHAR2(255),
  CONSTRAINT PK_alert_group PRIMARY KEY (group_id),
  CONSTRAINT uni_alert_group_name UNIQUE(cluster_id,group_name)
);

CREATE TABLE alert_target (
  target_id NUMBER(19) NOT NULL,
  target_name VARCHAR2(255) NOT NULL UNIQUE,
  notification_type VARCHAR2(64) NOT NULL,
  properties CLOB,
  description VARCHAR2(1024),
  is_global NUMBER(1) DEFAULT 0 NOT NULL,
  is_enabled NUMBER(1) DEFAULT 1 NOT NULL,
  CONSTRAINT PK_alert_target PRIMARY KEY (target_id)
);

CREATE TABLE alert_target_states (
  target_id NUMBER(19) NOT NULL,
  alert_state VARCHAR2(255) NOT NULL,
  FOREIGN KEY (target_id) REFERENCES alert_target(target_id)
);

CREATE TABLE alert_group_target (
  group_id NUMBER(19) NOT NULL,
  target_id NUMBER(19) NOT NULL,
  CONSTRAINT PK_alert_group_target PRIMARY KEY (group_id, target_id),
  FOREIGN KEY (group_id) REFERENCES alert_group(group_id),
  FOREIGN KEY (target_id) REFERENCES alert_target(target_id)
);

CREATE TABLE alert_grouping (
  definition_id NUMBER(19) NOT NULL,
  group_id NUMBER(19) NOT NULL,
  CONSTRAINT PK_alert_grouping PRIMARY KEY (group_id, definition_id),
  FOREIGN KEY (definition_id) REFERENCES alert_definition(definition_id),
  FOREIGN KEY (group_id) REFERENCES alert_group(group_id)
);

CREATE TABLE alert_notice (
  notification_id NUMBER(19) NOT NULL,
  target_id NUMBER(19) NOT NULL,
  history_id NUMBER(19) NOT NULL,
  notify_state VARCHAR2(255) NOT NULL,
  uuid VARCHAR2(64) NOT NULL UNIQUE,
  CONSTRAINT PK_alert_notice PRIMARY KEY (notification_id),
  FOREIGN KEY (target_id) REFERENCES alert_target(target_id),
  FOREIGN KEY (history_id) REFERENCES alert_history(alert_id)
);

CREATE INDEX idx_alert_history_def_id on alert_history(alert_definition_id);
CREATE INDEX idx_alert_history_service on alert_history(service_name);
CREATE INDEX idx_alert_history_host on alert_history(host_name);
CREATE INDEX idx_alert_history_time on alert_history(alert_timestamp);
CREATE INDEX idx_alert_history_state on alert_history(alert_state);
CREATE INDEX idx_alert_group_name on alert_group(group_name);
CREATE INDEX idx_alert_notice_state on alert_notice(notify_state);

---------inserting some data-----------
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('kkp_id_seq', 0);
-- In order for the first ID to be 1, must initialize the ambari_sequences table with a sequence_value of 0.
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('host_role_command_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('user_id_seq', 1);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('user_authentication_id_seq', 1);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('group_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('member_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('cluster_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('host_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('configgroup_id_seq', 1);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('requestschedule_id_seq', 1);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('resourcefilter_id_seq', 1);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('viewentity_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('operation_level_id_seq', 1);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('view_instance_id_seq', 1);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('resource_type_id_seq', 4);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('resource_id_seq', 2);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('principal_type_id_seq', 8);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('principal_id_seq', 13);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('permission_id_seq', 7);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('privilege_id_seq', 1);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('config_id_seq', 1);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('host_version_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('service_config_id_seq', 1);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('alert_definition_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('alert_group_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('alert_target_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('alert_history_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('alert_notice_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('alert_current_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('repo_version_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('repo_os_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('repo_definition_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('upgrade_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('upgrade_group_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('upgrade_item_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('stack_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('mpack_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('extension_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('link_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('widget_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('widget_layout_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('topology_host_info_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('topology_host_request_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('topology_host_task_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('topology_logical_request_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('topology_logical_task_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('topology_request_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('topology_host_group_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('setting_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('hostcomponentstate_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('servicecomponentdesiredstate_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('upgrade_history_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('blueprint_setting_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('ambari_operation_history_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('remote_cluster_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('remote_cluster_service_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('servicecomponent_version_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('hostcomponentdesiredstate_id_seq', 0);

INSERT INTO metainfo("metainfo_key", "metainfo_value") values ('version', '${ambariSchemaVersion}');

insert into adminresourcetype (resource_type_id, resource_type_name)
  select 1, 'AMBARI' from dual
  union all
  select 2, 'CLUSTER' from dual
  union all
  select 3, 'VIEW' from dual;

insert into adminresource (resource_id, resource_type_id)
  select 1, 1 from dual;

insert into adminprincipaltype (principal_type_id, principal_type_name)
  select 1, 'USER' from dual
  union all
  select 2, 'GROUP' from dual
  union all
  select 8, 'ROLE' from dual;

insert into adminprincipal (principal_id, principal_type_id)
  select 1, 1 from dual
  union all
  select 7, 8 from dual
  union all
  select 8, 8 from dual
  union all
  select 9, 8 from dual
  union all
  select 10, 8 from dual
  union all
  select 11, 8 from dual
  union all
  select 12, 8 from dual
  union all
  select 13, 8 from dual;

-- Insert the default administrator user.
insert into users(user_id, principal_id, user_name, display_name, local_username, create_time)
  SELECT 1, 1, 'admin', 'Administrator', 'admin', (SYSDATE - DATE '1970-01-01') * 86400000 from dual;

-- Insert the LOCAL authentication data for the default administrator user.
-- The authentication_key value is the salted digest of the password: admin
insert into user_authentication(user_authentication_id, user_id, authentication_type, authentication_key, create_time, update_time)
  SELECT 1, 1, 'LOCAL', '538916f8943ec225d97a9a86a2c6ec0818c1cd400e09e03b660fdaaec4af29ddbb6f2b1033b81b00', (SYSDATE - DATE '1970-01-01') * 86400000, (SYSDATE - DATE '1970-01-01') * 86400000 from dual;

insert into adminpermission(permission_id, permission_name, resource_type_id, permission_label, principal_id, sort_order)
  select 1, 'AMBARI.ADMINISTRATOR', 1, 'Ambari Administrator', 7, 1 from dual
  union all
  select 2, 'CLUSTER.USER', 2, 'Cluster User', 8, 6 from dual
  union all
  select 3, 'CLUSTER.ADMINISTRATOR', 2, 'Cluster Administrator', 9, 2 from dual
  union all
  select 4, 'VIEW.USER', 3, 'View User', 10, 7 from dual
  union all
  select 5, 'CLUSTER.OPERATOR', 2, 'Cluster Operator', 11, 3 from dual
  union all
  select 6, 'SERVICE.ADMINISTRATOR', 2, 'Service Administrator', 12, 4 from dual
  union all
  select 7, 'SERVICE.OPERATOR', 2, 'Service Operator', 13, 5 from dual;

INSERT INTO roleauthorization(authorization_id, authorization_name)
  SELECT 'VIEW.USE', 'Use View' FROM dual UNION ALL
  SELECT 'SERVICE.VIEW_METRICS', 'View metrics' FROM dual UNION ALL
  SELECT 'SERVICE.VIEW_STATUS_INFO', 'View status information' FROM dual UNION ALL
  SELECT 'SERVICE.VIEW_CONFIGS', 'View configurations' FROM dual UNION ALL
  SELECT 'SERVICE.COMPARE_CONFIGS', 'Compare configurations' FROM dual UNION ALL
  SELECT 'SERVICE.VIEW_ALERTS', 'View service-level alerts' FROM dual UNION ALL
  SELECT 'SERVICE.START_STOP', 'Start/Stop/Restart Service' FROM dual UNION ALL
  SELECT 'SERVICE.DECOMMISSION_RECOMMISSION', 'Decommission/recommission' FROM dual UNION ALL
  SELECT 'SERVICE.RUN_SERVICE_CHECK', 'Run service checks' FROM dual UNION ALL
  SELECT 'SERVICE.TOGGLE_MAINTENANCE', 'Turn on/off maintenance mode' FROM dual UNION ALL
  SELECT 'SERVICE.RUN_CUSTOM_COMMAND', 'Perform service-specific tasks' FROM dual UNION ALL
  SELECT 'SERVICE.MODIFY_CONFIGS', 'Modify configurations' FROM dual UNION ALL
  SELECT 'SERVICE.MANAGE_CONFIG_GROUPS', 'Manage configuration groups' FROM dual UNION ALL
  SELECT 'SERVICE.MANAGE_ALERTS', 'Manage service-level alerts' from dual UNION ALL
  SELECT 'SERVICE.MOVE', 'Move to another host' FROM dual UNION ALL
  SELECT 'SERVICE.ENABLE_HA', 'Enable HA' FROM dual UNION ALL
  SELECT 'SERVICE.TOGGLE_ALERTS', 'Enable/disable service-level alerts' FROM dual UNION ALL
  SELECT 'SERVICE.ADD_DELETE_SERVICES', 'Add/delete services' FROM dual UNION ALL
  SELECT 'SERVICE.VIEW_OPERATIONAL_LOGS', 'View service operational logs' from dual UNION ALL
  SELECT 'SERVICE.SET_SERVICE_USERS_GROUPS', 'Set service users and groups' FROM dual UNION ALL
  SELECT 'SERVICE.MANAGE_AUTO_START', 'Manage service auto-start' FROM dual UNION ALL
  SELECT 'HOST.VIEW_METRICS', 'View metrics' FROM dual UNION ALL
  SELECT 'HOST.VIEW_STATUS_INFO', 'View status information' FROM dual UNION ALL
  SELECT 'HOST.VIEW_CONFIGS', 'View configuration' FROM dual UNION ALL
  SELECT 'HOST.TOGGLE_MAINTENANCE', 'Turn on/off maintenance mode' FROM dual UNION ALL
  SELECT 'HOST.ADD_DELETE_COMPONENTS', 'Install components' FROM dual UNION ALL
  SELECT 'HOST.ADD_DELETE_HOSTS', 'Add/Delete hosts' FROM dual UNION ALL
  SELECT 'CLUSTER.VIEW_METRICS', 'View metrics' FROM dual UNION ALL
  SELECT 'CLUSTER.VIEW_STATUS_INFO', 'View status information' FROM dual UNION ALL
  SELECT 'CLUSTER.VIEW_CONFIGS', 'View configuration' FROM dual UNION ALL
  SELECT 'CLUSTER.VIEW_STACK_DETAILS', 'View stack version details' FROM dual UNION ALL
  SELECT 'CLUSTER.VIEW_ALERTS', 'View cluster-level alerts' FROM dual UNION ALL
  SELECT 'CLUSTER.MANAGE_CREDENTIALS', 'Manage external credentials' from dual UNION ALL
  SELECT 'CLUSTER.MODIFY_CONFIGS', 'Modify cluster configurations' from dual UNION ALL
  SELECT 'CLUSTER.MANAGE_CONFIG_GROUPS', 'Manage cluster config groups' from dual UNION ALL
  SELECT 'CLUSTER.MANAGE_ALERTS', 'Manage cluster-level alerts' from dual UNION ALL
  SELECT 'CLUSTER.MANAGE_USER_PERSISTED_DATA', 'Manage cluster-level user persisted data' from dual UNION ALL
  SELECT 'CLUSTER.TOGGLE_ALERTS', 'Enable/disable cluster-level alerts' FROM dual UNION ALL
  SELECT 'CLUSTER.TOGGLE_KERBEROS', 'Enable/disable Kerberos' FROM dual UNION ALL
  SELECT 'CLUSTER.UPGRADE_DOWNGRADE_STACK', 'Upgrade/downgrade stack' FROM dual UNION ALL
  SELECT 'CLUSTER.RUN_CUSTOM_COMMAND', 'Perform custom cluster-level actions' FROM dual UNION ALL
  SELECT 'CLUSTER.MANAGE_AUTO_START', 'Manage service auto-start configuration' FROM dual UNION ALL
  SELECT 'CLUSTER.MANAGE_ALERT_NOTIFICATIONS', 'Manage alert notifications configuration' FROM dual UNION ALL
  SELECT 'CLUSTER.MANAGE_WIDGETS', 'Manage widgets' FROM dual UNION ALL
  SELECT 'AMBARI.ADD_DELETE_CLUSTERS', 'Create new clusters' FROM dual UNION ALL
  SELECT 'AMBARI.RENAME_CLUSTER', 'Rename clusters' FROM dual UNION ALL
  SELECT 'AMBARI.MANAGE_SETTINGS', 'Manage settings' FROM dual UNION ALL
  SELECT 'AMBARI.MANAGE_CONFIGURATION', 'Manage ambari configuration' FROM dual UNION ALL
  SELECT 'AMBARI.MANAGE_USERS', 'Manage users' FROM dual UNION ALL
  SELECT 'AMBARI.MANAGE_GROUPS', 'Manage groups' FROM dual UNION ALL
  SELECT 'AMBARI.MANAGE_VIEWS', 'Manage Ambari Views' FROM dual UNION ALL
  SELECT 'AMBARI.ASSIGN_ROLES', 'Assign roles' FROM dual UNION ALL
  SELECT 'AMBARI.MANAGE_STACK_VERSIONS', 'Manage stack versions' FROM dual UNION ALL
  SELECT 'AMBARI.EDIT_STACK_REPOS', 'Edit stack repository URLs' FROM dual UNION ALL
  SELECT 'AMBARI.VIEW_STATUS_INFO', 'View status information' FROM dual UNION ALL
  SELECT 'AMBARI.RUN_CUSTOM_COMMAND', 'Perform custom administrative actions' FROM dual;

-- Set authorizations for View User role
INSERT INTO permission_roleauthorization(permission_id, authorization_id)
  SELECT permission_id, 'VIEW.USE' FROM adminpermission WHERE permission_name='VIEW.USER';

-- Set authorizations for Cluster User role
INSERT INTO permission_roleauthorization(permission_id, authorization_id)
  SELECT permission_id, 'SERVICE.VIEW_METRICS' FROM adminpermission WHERE permission_name='CLUSTER.USER' UNION ALL
  SELECT permission_id, 'SERVICE.VIEW_STATUS_INFO' FROM adminpermission WHERE permission_name='CLUSTER.USER' UNION ALL
  SELECT permission_id, 'SERVICE.VIEW_CONFIGS' FROM adminpermission WHERE permission_name='CLUSTER.USER' UNION ALL
  SELECT permission_id, 'SERVICE.COMPARE_CONFIGS' FROM adminpermission WHERE permission_name='CLUSTER.USER' UNION ALL
  SELECT permission_id, 'SERVICE.VIEW_ALERTS' FROM adminpermission WHERE permission_name='CLUSTER.USER' UNION ALL
  SELECT permission_id, 'HOST.VIEW_METRICS' FROM adminpermission WHERE permission_name='CLUSTER.USER' UNION ALL
  SELECT permission_id, 'HOST.VIEW_STATUS_INFO' FROM adminpermission WHERE permission_name='CLUSTER.USER' UNION ALL
  SELECT permission_id, 'HOST.VIEW_CONFIGS' FROM adminpermission WHERE permission_name='CLUSTER.USER' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_METRICS' FROM adminpermission WHERE permission_name='CLUSTER.USER' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_STATUS_INFO' FROM adminpermission WHERE permission_name='CLUSTER.USER' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_CONFIGS' FROM adminpermission WHERE permission_name='CLUSTER.USER' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_STACK_DETAILS' FROM adminpermission WHERE permission_name='CLUSTER.USER' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_ALERTS' FROM adminpermission WHERE permission_name='CLUSTER.USER' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_USER_PERSISTED_DATA' FROM adminpermission WHERE permission_name='CLUSTER.USER';

-- Set authorizations for Service Operator role
INSERT INTO permission_roleauthorization(permission_id, authorization_id)
  SELECT permission_id, 'SERVICE.VIEW_METRICS' FROM adminpermission WHERE permission_name='SERVICE.OPERATOR' UNION ALL
  SELECT permission_id, 'SERVICE.VIEW_STATUS_INFO' FROM adminpermission WHERE permission_name='SERVICE.OPERATOR' UNION ALL
  SELECT permission_id, 'SERVICE.VIEW_CONFIGS' FROM adminpermission WHERE permission_name='SERVICE.OPERATOR' UNION ALL
  SELECT permission_id, 'SERVICE.COMPARE_CONFIGS' FROM adminpermission WHERE permission_name='SERVICE.OPERATOR' UNION ALL
  SELECT permission_id, 'SERVICE.VIEW_ALERTS' FROM adminpermission WHERE permission_name='SERVICE.OPERATOR' UNION ALL
  SELECT permission_id, 'SERVICE.START_STOP' FROM adminpermission WHERE permission_name='SERVICE.OPERATOR' UNION ALL
  SELECT permission_id, 'SERVICE.DECOMMISSION_RECOMMISSION' FROM adminpermission WHERE permission_name='SERVICE.OPERATOR' UNION ALL
  SELECT permission_id, 'SERVICE.RUN_SERVICE_CHECK' FROM adminpermission WHERE permission_name='SERVICE.OPERATOR' UNION ALL
  SELECT permission_id, 'SERVICE.TOGGLE_MAINTENANCE' FROM adminpermission WHERE permission_name='SERVICE.OPERATOR' UNION ALL
  SELECT permission_id, 'SERVICE.RUN_CUSTOM_COMMAND' FROM adminpermission WHERE permission_name='SERVICE.OPERATOR' UNION ALL
  SELECT permission_id, 'HOST.VIEW_METRICS' FROM adminpermission WHERE permission_name='SERVICE.OPERATOR' UNION ALL
  SELECT permission_id, 'HOST.VIEW_STATUS_INFO' FROM adminpermission WHERE permission_name='SERVICE.OPERATOR' UNION ALL
  SELECT permission_id, 'HOST.VIEW_CONFIGS' FROM adminpermission WHERE permission_name='SERVICE.OPERATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_METRICS' FROM adminpermission WHERE permission_name='SERVICE.OPERATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_STATUS_INFO' FROM adminpermission WHERE permission_name='SERVICE.OPERATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_CONFIGS' FROM adminpermission WHERE permission_name='SERVICE.OPERATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_STACK_DETAILS' FROM adminpermission WHERE permission_name='SERVICE.OPERATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_ALERTS' FROM adminpermission WHERE permission_name='SERVICE.OPERATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_USER_PERSISTED_DATA' FROM adminpermission WHERE permission_name='SERVICE.OPERATOR';

-- Set authorizations for Service Administrator role
INSERT INTO permission_roleauthorization(permission_id, authorization_id)
  SELECT permission_id, 'SERVICE.VIEW_METRICS' FROM adminpermission WHERE permission_name='SERVICE.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.VIEW_STATUS_INFO' FROM adminpermission WHERE permission_name='SERVICE.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.VIEW_CONFIGS' FROM adminpermission WHERE permission_name='SERVICE.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.COMPARE_CONFIGS' FROM adminpermission WHERE permission_name='SERVICE.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.VIEW_ALERTS' FROM adminpermission WHERE permission_name='SERVICE.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.START_STOP' FROM adminpermission WHERE permission_name='SERVICE.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.DECOMMISSION_RECOMMISSION' FROM adminpermission WHERE permission_name='SERVICE.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.RUN_SERVICE_CHECK' FROM adminpermission WHERE permission_name='SERVICE.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.TOGGLE_MAINTENANCE' FROM adminpermission WHERE permission_name='SERVICE.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.RUN_CUSTOM_COMMAND' FROM adminpermission WHERE permission_name='SERVICE.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.MODIFY_CONFIGS' FROM adminpermission WHERE permission_name='SERVICE.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.MANAGE_CONFIG_GROUPS' FROM adminpermission WHERE permission_name='SERVICE.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.VIEW_OPERATIONAL_LOGS' FROM adminpermission WHERE permission_name='SERVICE.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.MANAGE_AUTO_START' FROM adminpermission WHERE permission_name='SERVICE.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'HOST.VIEW_METRICS' FROM adminpermission WHERE permission_name='SERVICE.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'HOST.VIEW_STATUS_INFO' FROM adminpermission WHERE permission_name='SERVICE.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'HOST.VIEW_CONFIGS' FROM adminpermission WHERE permission_name='SERVICE.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_METRICS' FROM adminpermission WHERE permission_name='SERVICE.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_STATUS_INFO' FROM adminpermission WHERE permission_name='SERVICE.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_CONFIGS' FROM adminpermission WHERE permission_name='SERVICE.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_STACK_DETAILS' FROM adminpermission WHERE permission_name='SERVICE.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_CONFIG_GROUPS' FROM adminpermission WHERE permission_name='SERVICE.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_ALERTS' FROM adminpermission WHERE permission_name='SERVICE.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_USER_PERSISTED_DATA' FROM adminpermission WHERE permission_name='SERVICE.ADMINISTRATOR';

-- Set authorizations for Cluster Operator role
INSERT INTO permission_roleauthorization(permission_id, authorization_id)
  SELECT permission_id, 'SERVICE.VIEW_METRICS' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'SERVICE.VIEW_STATUS_INFO' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'SERVICE.VIEW_CONFIGS' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'SERVICE.COMPARE_CONFIGS' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'SERVICE.VIEW_ALERTS' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'SERVICE.START_STOP' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'SERVICE.DECOMMISSION_RECOMMISSION' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'SERVICE.RUN_SERVICE_CHECK' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'SERVICE.TOGGLE_MAINTENANCE' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'SERVICE.RUN_CUSTOM_COMMAND' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'SERVICE.MODIFY_CONFIGS' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'SERVICE.MANAGE_CONFIG_GROUPS' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'SERVICE.MOVE' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'SERVICE.ENABLE_HA' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'SERVICE.VIEW_OPERATIONAL_LOGS' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'SERVICE.MANAGE_AUTO_START' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'HOST.VIEW_METRICS' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'HOST.VIEW_STATUS_INFO' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'HOST.VIEW_CONFIGS' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'HOST.TOGGLE_MAINTENANCE' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'HOST.ADD_DELETE_COMPONENTS' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'HOST.ADD_DELETE_HOSTS' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_METRICS' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_STATUS_INFO' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_CONFIGS' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_STACK_DETAILS' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_CONFIG_GROUPS' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_ALERTS' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_CREDENTIALS' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_AUTO_START' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_WIDGETS' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_USER_PERSISTED_DATA' FROM adminpermission WHERE permission_name='CLUSTER.OPERATOR';

-- Set authorizations for Cluster Administrator role
INSERT INTO permission_roleauthorization(permission_id, authorization_id)
  SELECT permission_id, 'SERVICE.VIEW_METRICS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.VIEW_STATUS_INFO' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.VIEW_CONFIGS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.COMPARE_CONFIGS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.VIEW_ALERTS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.START_STOP' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.DECOMMISSION_RECOMMISSION' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.RUN_SERVICE_CHECK' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.TOGGLE_MAINTENANCE' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.RUN_CUSTOM_COMMAND' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.MODIFY_CONFIGS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.MANAGE_CONFIG_GROUPS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.MANAGE_ALERTS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.MOVE' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.ENABLE_HA' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.TOGGLE_ALERTS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.ADD_DELETE_SERVICES' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.VIEW_OPERATIONAL_LOGS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.SET_SERVICE_USERS_GROUPS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.MANAGE_AUTO_START' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'HOST.VIEW_METRICS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'HOST.VIEW_STATUS_INFO' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'HOST.VIEW_CONFIGS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'HOST.TOGGLE_MAINTENANCE' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'HOST.ADD_DELETE_COMPONENTS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'HOST.ADD_DELETE_HOSTS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_METRICS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_STATUS_INFO' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_CONFIGS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_STACK_DETAILS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_ALERTS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_CREDENTIALS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MODIFY_CONFIGS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_CONFIG_GROUPS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_ALERTS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.TOGGLE_ALERTS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.TOGGLE_KERBEROS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.UPGRADE_DOWNGRADE_STACK' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_USER_PERSISTED_DATA' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_AUTO_START' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_ALERT_NOTIFICATIONS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_WIDGETS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.RUN_CUSTOM_COMMAND' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR';

-- Set authorizations for Administrator role
INSERT INTO permission_roleauthorization(permission_id, authorization_id)
  SELECT permission_id, 'VIEW.USE' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.VIEW_METRICS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.VIEW_STATUS_INFO' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.VIEW_CONFIGS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.COMPARE_CONFIGS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.VIEW_ALERTS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.START_STOP' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.DECOMMISSION_RECOMMISSION' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.RUN_SERVICE_CHECK' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.TOGGLE_MAINTENANCE' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.RUN_CUSTOM_COMMAND' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.MODIFY_CONFIGS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.MANAGE_CONFIG_GROUPS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.MANAGE_ALERTS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.MOVE' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.ENABLE_HA' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.TOGGLE_ALERTS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.ADD_DELETE_SERVICES' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.VIEW_OPERATIONAL_LOGS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.SET_SERVICE_USERS_GROUPS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'SERVICE.MANAGE_AUTO_START' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'HOST.VIEW_METRICS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'HOST.VIEW_STATUS_INFO' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'HOST.VIEW_CONFIGS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'HOST.TOGGLE_MAINTENANCE' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'HOST.ADD_DELETE_COMPONENTS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'HOST.ADD_DELETE_HOSTS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_METRICS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_STATUS_INFO' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_CONFIGS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_STACK_DETAILS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.VIEW_ALERTS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_CREDENTIALS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MODIFY_CONFIGS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_CONFIG_GROUPS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_ALERTS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.TOGGLE_ALERTS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.TOGGLE_KERBEROS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.UPGRADE_DOWNGRADE_STACK' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_USER_PERSISTED_DATA' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_AUTO_START' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_ALERT_NOTIFICATIONS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_WIDGETS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.RUN_CUSTOM_COMMAND' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'AMBARI.ADD_DELETE_CLUSTERS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'AMBARI.RENAME_CLUSTER' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'AMBARI.MANAGE_SETTINGS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'AMBARI.MANAGE_CONFIGURATION' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'AMBARI.MANAGE_USERS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'AMBARI.MANAGE_GROUPS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'AMBARI.MANAGE_VIEWS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'AMBARI.ASSIGN_ROLES' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'AMBARI.MANAGE_STACK_VERSIONS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'AMBARI.EDIT_STACK_REPOS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'AMBARI.VIEW_STATUS_INFO' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'AMBARI.RUN_CUSTOM_COMMAND' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR';

insert into adminprivilege (privilege_id, permission_id, resource_id, principal_id)
  select 1, 1, 1, 1 from dual;

commit;

-- Quartz tables

CREATE TABLE qrtz_job_details
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    JOB_NAME  VARCHAR2(200) NOT NULL,
    JOB_GROUP VARCHAR2(200) NOT NULL,
    DESCRIPTION VARCHAR2(250) NULL,
    JOB_CLASS_NAME   VARCHAR2(250) NOT NULL,
    IS_DURABLE VARCHAR2(1) NOT NULL,
    IS_NONCONCURRENT VARCHAR2(1) NOT NULL,
    IS_UPDATE_DATA VARCHAR2(1) NOT NULL,
    REQUESTS_RECOVERY VARCHAR2(1) NOT NULL,
    JOB_DATA BLOB NULL,
    CONSTRAINT QRTZ_JOB_DETAILS_PK PRIMARY KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
);
CREATE TABLE qrtz_triggers
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    TRIGGER_NAME VARCHAR2(200) NOT NULL,
    TRIGGER_GROUP VARCHAR2(200) NOT NULL,
    JOB_NAME  VARCHAR2(200) NOT NULL,
    JOB_GROUP VARCHAR2(200) NOT NULL,
    DESCRIPTION VARCHAR2(250) NULL,
    NEXT_FIRE_TIME NUMBER(13) NULL,
    PREV_FIRE_TIME NUMBER(13) NULL,
    PRIORITY NUMBER(13) NULL,
    TRIGGER_STATE VARCHAR2(16) NOT NULL,
    TRIGGER_TYPE VARCHAR2(8) NOT NULL,
    START_TIME NUMBER(13) NOT NULL,
    END_TIME NUMBER(13) NULL,
    CALENDAR_NAME VARCHAR2(200) NULL,
    MISFIRE_INSTR NUMBER(2) NULL,
    JOB_DATA BLOB NULL,
    CONSTRAINT QRTZ_TRIGGERS_PK PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    CONSTRAINT QRTZ_TRIGGER_TO_JOBS_FK FOREIGN KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
      REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME,JOB_NAME,JOB_GROUP)
);
CREATE TABLE qrtz_simple_triggers
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    TRIGGER_NAME VARCHAR2(200) NOT NULL,
    TRIGGER_GROUP VARCHAR2(200) NOT NULL,
    REPEAT_COUNT NUMBER(7) NOT NULL,
    REPEAT_INTERVAL NUMBER(12) NOT NULL,
    TIMES_TRIGGERED NUMBER(10) NOT NULL,
    CONSTRAINT QRTZ_SIMPLE_TRIG_PK PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    CONSTRAINT QRTZ_SIMPLE_TRIG_TO_TRIG_FK FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
	REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);
CREATE TABLE qrtz_cron_triggers
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    TRIGGER_NAME VARCHAR2(200) NOT NULL,
    TRIGGER_GROUP VARCHAR2(200) NOT NULL,
    CRON_EXPRESSION VARCHAR2(120) NOT NULL,
    TIME_ZONE_ID VARCHAR2(80),
    CONSTRAINT QRTZ_CRON_TRIG_PK PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    CONSTRAINT QRTZ_CRON_TRIG_TO_TRIG_FK FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
      REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);
CREATE TABLE qrtz_simprop_triggers
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    TRIGGER_NAME VARCHAR2(200) NOT NULL,
    TRIGGER_GROUP VARCHAR2(200) NOT NULL,
    STR_PROP_1 VARCHAR2(512) NULL,
    STR_PROP_2 VARCHAR2(512) NULL,
    STR_PROP_3 VARCHAR2(512) NULL,
    INT_PROP_1 NUMBER(10) NULL,
    INT_PROP_2 NUMBER(10) NULL,
    LONG_PROP_1 NUMBER(13) NULL,
    LONG_PROP_2 NUMBER(13) NULL,
    DEC_PROP_1 NUMERIC(13,4) NULL,
    DEC_PROP_2 NUMERIC(13,4) NULL,
    BOOL_PROP_1 VARCHAR2(1) NULL,
    BOOL_PROP_2 VARCHAR2(1) NULL,
    CONSTRAINT QRTZ_SIMPROP_TRIG_PK PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    CONSTRAINT QRTZ_SIMPROP_TRIG_TO_TRIG_FK FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
      REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);
CREATE TABLE qrtz_blob_triggers
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    TRIGGER_NAME VARCHAR2(200) NOT NULL,
    TRIGGER_GROUP VARCHAR2(200) NOT NULL,
    BLOB_DATA BLOB NULL,
    CONSTRAINT QRTZ_BLOB_TRIG_PK PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    CONSTRAINT QRTZ_BLOB_TRIG_TO_TRIG_FK FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);
CREATE TABLE qrtz_calendars
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    CALENDAR_NAME  VARCHAR2(200) NOT NULL,
    CALENDAR BLOB NOT NULL,
    CONSTRAINT QRTZ_CALENDARS_PK PRIMARY KEY (SCHED_NAME,CALENDAR_NAME)
);
CREATE TABLE qrtz_paused_trigger_grps
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    TRIGGER_GROUP  VARCHAR2(200) NOT NULL,
    CONSTRAINT QRTZ_PAUSED_TRIG_GRPS_PK PRIMARY KEY (SCHED_NAME,TRIGGER_GROUP)
);
CREATE TABLE qrtz_fired_triggers
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    ENTRY_ID VARCHAR2(95) NOT NULL,
    TRIGGER_NAME VARCHAR2(200) NOT NULL,
    TRIGGER_GROUP VARCHAR2(200) NOT NULL,
    INSTANCE_NAME VARCHAR2(200) NOT NULL,
    FIRED_TIME NUMBER(13) NOT NULL,
    SCHED_TIME NUMBER(13) NOT NULL,
    PRIORITY NUMBER(13) NOT NULL,
    STATE VARCHAR2(16) NOT NULL,
    JOB_NAME VARCHAR2(200) NULL,
    JOB_GROUP VARCHAR2(200) NULL,
    IS_NONCONCURRENT VARCHAR2(1) NULL,
    REQUESTS_RECOVERY VARCHAR2(1) NULL,
    CONSTRAINT QRTZ_FIRED_TRIGGER_PK PRIMARY KEY (SCHED_NAME,ENTRY_ID)
);
CREATE TABLE qrtz_scheduler_state
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    INSTANCE_NAME VARCHAR2(200) NOT NULL,
    LAST_CHECKIN_TIME NUMBER(13) NOT NULL,
    CHECKIN_INTERVAL NUMBER(13) NOT NULL,
    CONSTRAINT QRTZ_SCHEDULER_STATE_PK PRIMARY KEY (SCHED_NAME,INSTANCE_NAME)
);
CREATE TABLE qrtz_locks
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    LOCK_NAME  VARCHAR2(40) NOT NULL,
    CONSTRAINT QRTZ_LOCKS_PK PRIMARY KEY (SCHED_NAME,LOCK_NAME)
);

create index idx_qrtz_j_req_recovery on qrtz_job_details(SCHED_NAME,REQUESTS_RECOVERY);
create index idx_qrtz_j_grp on qrtz_job_details(SCHED_NAME,JOB_GROUP);

create index idx_qrtz_t_j on qrtz_triggers(SCHED_NAME,JOB_NAME,JOB_GROUP);
create index idx_qrtz_t_jg on qrtz_triggers(SCHED_NAME,JOB_GROUP);
create index idx_qrtz_t_c on qrtz_triggers(SCHED_NAME,CALENDAR_NAME);
create index idx_qrtz_t_g on qrtz_triggers(SCHED_NAME,TRIGGER_GROUP);
create index idx_qrtz_t_state on qrtz_triggers(SCHED_NAME,TRIGGER_STATE);
create index idx_qrtz_t_n_state on qrtz_triggers(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP,TRIGGER_STATE);
create index idx_qrtz_t_n_g_state on qrtz_triggers(SCHED_NAME,TRIGGER_GROUP,TRIGGER_STATE);
create index idx_qrtz_t_next_fire_time on qrtz_triggers(SCHED_NAME,NEXT_FIRE_TIME);
create index idx_qrtz_t_nft_st on qrtz_triggers(SCHED_NAME,TRIGGER_STATE,NEXT_FIRE_TIME);
create index idx_qrtz_t_nft_misfire on qrtz_triggers(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME);
create index idx_qrtz_t_nft_st_misfire on qrtz_triggers(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_STATE);
create index idx_qrtz_t_nft_st_misfire_grp on qrtz_triggers(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_GROUP,TRIGGER_STATE);

create index idx_qrtz_ft_trig_inst_name on qrtz_fired_triggers(SCHED_NAME,INSTANCE_NAME);
create index idx_qrtz_ft_inst_job_req_rcvry on qrtz_fired_triggers(SCHED_NAME,INSTANCE_NAME,REQUESTS_RECOVERY);
create index idx_qrtz_ft_j_g on qrtz_fired_triggers(SCHED_NAME,JOB_NAME,JOB_GROUP);
create index idx_qrtz_ft_jg on qrtz_fired_triggers(SCHED_NAME,JOB_GROUP);
create index idx_qrtz_ft_t_g on qrtz_fired_triggers(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP);
create index idx_qrtz_ft_tg on qrtz_fired_triggers(SCHED_NAME,TRIGGER_GROUP);
