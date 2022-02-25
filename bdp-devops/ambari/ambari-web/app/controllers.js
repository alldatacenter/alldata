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


// load all controllers here

require('controllers/application');
require('controllers/login_controller');
require('controllers/wizard');
require('controllers/installer');
require('controllers/experimental');
require('controllers/global/background_operations_controller');
require('controllers/global/wizard_watcher_controller');
require('controllers/global/user_settings_controller');
require('controllers/global/errors_handler_controller');
require('controllers/main');
require('controllers/main/dashboard');
require('controllers/main/dashboard/config_history_controller');
require('controllers/main/admin');
require('controllers/main/admin/service_auto_start');
require('controllers/main/admin/highAvailability_controller');
require('controllers/main/admin/highAvailability/nameNode/wizard_controller');
require('controllers/main/admin/highAvailability/progress_controller');
require('controllers/main/admin/highAvailability/progress_popup_controller');
require('controllers/main/admin/highAvailability/nameNode/rollback_controller');
require('controllers/main/admin/highAvailability/nameNode/step1_controller');
require('controllers/main/admin/highAvailability/nameNode/step2_controller');
require('controllers/main/admin/highAvailability/nameNode/step3_controller');
require('controllers/main/admin/highAvailability/nameNode/step4_controller');
require('controllers/main/admin/highAvailability/nameNode/step5_controller');
require('controllers/main/admin/highAvailability/nameNode/step6_controller');
require('controllers/main/admin/highAvailability/nameNode/step7_controller');
require('controllers/main/admin/highAvailability/nameNode/step8_controller');
require('controllers/main/admin/highAvailability/nameNode/step9_controller');
require('controllers/main/admin/highAvailability/nameNode/rollbackHA/step1_controller');
require('controllers/main/admin/highAvailability/nameNode/rollbackHA/step2_controller');
require('controllers/main/admin/highAvailability/nameNode/rollbackHA/step3_controller');
require('controllers/main/admin/highAvailability/nameNode/rollbackHA/rollback_wizard_controller');
require('controllers/main/admin/highAvailability/resourceManager/wizard_controller');
require('controllers/main/admin/highAvailability/resourceManager/step1_controller');
require('controllers/main/admin/highAvailability/resourceManager/step2_controller');
require('controllers/main/admin/highAvailability/resourceManager/step3_controller');
require('controllers/main/admin/highAvailability/resourceManager/step4_controller');
require('controllers/main/admin/federation/wizard_controller');
require('controllers/main/admin/federation/step1_controller');
require('controllers/main/admin/federation/step2_controller');
require('controllers/main/admin/federation/step3_controller');
require('controllers/main/admin/federation/step4_controller');
require('controllers/main/admin/highAvailability/hawq/addStandby/wizard_controller');
require('controllers/main/admin/highAvailability/hawq/addStandby/step1_controller');
require('controllers/main/admin/highAvailability/hawq/addStandby/step2_controller');
require('controllers/main/admin/highAvailability/hawq/addStandby/step3_controller');
require('controllers/main/admin/highAvailability/hawq/addStandby/step4_controller');
require('controllers/main/admin/highAvailability/hawq/removeStandby/wizard_controller');
require('controllers/main/admin/highAvailability/hawq/removeStandby/step1_controller');
require('controllers/main/admin/highAvailability/hawq/removeStandby/step2_controller');
require('controllers/main/admin/highAvailability/hawq/removeStandby/step3_controller');
require('controllers/main/admin/highAvailability/hawq/activateStandby/wizard_controller');
require('controllers/main/admin/highAvailability/hawq/activateStandby/step1_controller');
require('controllers/main/admin/highAvailability/hawq/activateStandby/step2_controller');
require('controllers/main/admin/highAvailability/hawq/activateStandby/step3_controller');
require('controllers/main/admin/highAvailability/rangerAdmin/wizard_controller');
require('controllers/main/admin/highAvailability/rangerAdmin/step1_controller');
require('controllers/main/admin/highAvailability/rangerAdmin/step2_controller');
require('controllers/main/admin/highAvailability/rangerAdmin/step3_controller');
require('controllers/main/admin/highAvailability/rangerAdmin/step4_controller');
require('controllers/main/admin/highAvailability/journalNode/wizard_controller');
require('controllers/main/admin/highAvailability/journalNode/progress_controller');
require('controllers/main/admin/highAvailability/journalNode/step1_controller');
require('controllers/main/admin/highAvailability/journalNode/step2_controller');
require('controllers/main/admin/highAvailability/journalNode/step3_controller');
require('controllers/main/admin/highAvailability/journalNode/step4_controller');
require('controllers/main/admin/highAvailability/journalNode/step5_controller');
require('controllers/main/admin/highAvailability/journalNode/step6_controller');
require('controllers/main/admin/highAvailability/journalNode/step7_controller');
require('controllers/main/admin/stack_and_upgrade_controller');
require('controllers/main/admin/stack_upgrade_history_controller');
require('controllers/main/admin/serviceAccounts_controller');
require('utils/polling');
require('controllers/main/admin/kerberos');
require('controllers/main/admin/kerberos/wizard_controller');
require('controllers/main/admin/kerberos/disable_controller');
require('controllers/main/admin/kerberos/progress_controller');
require('controllers/main/admin/kerberos/step1_controller');
require('controllers/main/admin/kerberos/step2_controller');
require('controllers/main/admin/kerberos/step3_controller');
require('controllers/main/admin/kerberos/step4_controller');
require('controllers/main/admin/kerberos/step5_controller');
require('controllers/main/admin/kerberos/step6_controller');
require('controllers/main/admin/kerberos/step7_controller');
require('controllers/main/admin/kerberos/step8_controller');
require('controllers/main/alert_definitions_controller');
require('controllers/main/alerts/alert_definitions_actions_controller');
require('controllers/main/alerts/add_alert_definition/add_alert_definition_controller');
require('controllers/main/alerts/add_alert_definition/step1_controller');
require('controllers/main/alerts/add_alert_definition/step2_controller');
require('controllers/main/alerts/add_alert_definition/step3_controller');
require('controllers/main/alerts/definition_details_controller');
require('controllers/main/alerts/definition_configs_controller');
require('controllers/main/alerts/alert_instances_controller');
require('controllers/main/alerts/manage_alert_groups_controller');
require('controllers/main/alerts/manage_alert_notifications_controller');
require('controllers/main/service');
require('controllers/main/service/item');
require('controllers/main/service/info/summary');
require('controllers/main/service/info/configs');
require('controllers/main/service/info/audit');
require('controllers/main/service/add_controller');
require('controllers/main/service/reassign_controller');
require('controllers/main/service/reassign/step1_controller');
require('controllers/main/service/reassign/step2_controller');
require('controllers/main/service/reassign/step3_controller');
require('controllers/main/service/reassign/step4_controller');
require('controllers/main/service/reassign/step5_controller');
require('controllers/main/service/reassign/step6_controller');
require('controllers/main/service/manage_config_groups_controller');
require('controllers/main/service/widgets/create/wizard_controller');
require('controllers/main/service/widgets/create/step1_controller');
require('controllers/main/service/widgets/create/step2_controller');
require('controllers/main/service/widgets/create/step3_controller');
require('controllers/main/service/widgets/edit_controller');
require('controllers/main/host');
require('controllers/main/host/bulk_operations_controller');
require('controllers/main/host/details');
require('controllers/main/host/configs_service');
require('controllers/main/host/add_controller');
require('controllers/main/host/combo_search_box');
require('controllers/main/host/addHost/step4_controller');
require('controllers/main/host/host_alerts_controller');
require('controllers/main/charts');
require('controllers/main/charts/heatmap_metrics/heatmap_metric');
require('controllers/main/charts/heatmap');
require('controllers/main/service/info/heatmap');
require('controllers/main/service/info/metric');
require('controllers/main/views_controller');
require('controllers/main/views/details_controller');
require('controllers/wizard/step0_controller');
require('controllers/wizard/step1_controller');
require('controllers/wizard/step2_controller');
require('controllers/wizard/step3_controller');
require('controllers/wizard/step4_controller');
require('controllers/wizard/step5_controller');
require('controllers/wizard/step6_controller');
require('controllers/wizard/step7_controller');
require('controllers/wizard/step7/assign_master_controller');
require('controllers/wizard/step7/pre_install_checks_controller');
require('controllers/wizard/step8_controller');
require('controllers/wizard/step9_controller');
require('controllers/wizard/step10_controller');
require('controllers/global/cluster_controller');
require('controllers/global/update_controller');
require('controllers/global/configuration_controller');
require('controllers/main/service/reassign/step7_controller');
