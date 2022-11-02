/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//load all mappers
require('mappers/server_data_mapper');
require('mappers/stack_service_mapper');
require('mappers/stack_mapper');
require('mappers/stack_version_mapper');
require('mappers/configs/themes_mapper');
require('mappers/configs/stack_config_properties_mapper');
require('mappers/configs/config_groups_mapper');
require('mappers/configs/service_config_version_mapper');
require('mappers/repository_version_mapper');
require('mappers/quicklinks_mapper');
require('mappers/hosts_mapper');
require('mappers/cluster_mapper');
require('mappers/users_mapper');
require('mappers/service_mapper');
require('mappers/service_metrics_mapper');
require('mappers/components_state_mapper');
require('mappers/alert_definitions_mapper');
require('mappers/alert_definition_summary_mapper');
require('mappers/alert_instances_mapper');
require('mappers/alert_groups_mapper');
require('mappers/alert_notification_mapper');
require('mappers/widget_mapper');
require('mappers/widget_layout_mapper');
require('mappers/stack_upgrade_history_mapper');
require('mappers/socket/topology_mapper');
require('mappers/socket/service_state_mapper');
require('mappers/socket/host_component_status_mapper');
require('mappers/socket/alert_summary_mapper');
require('mappers/socket/host_state_mapper');
require('mappers/socket/alert_definitions_mapper_adapter');
require('mappers/socket/alert_groups_mapper_adapter');
require('mappers/socket/upgrade_state_mapper');
