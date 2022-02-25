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


// load all mixins here

require('mixins/common/blueprint');
require('mixins/common/kdc_credentials_controller_mixin');
require('mixins/common/localStorage');
require('mixins/common/infinite_scroll_mixin');
require('mixins/common/persist');
require('mixins/common/reload_popup');
require('mixins/common/serverValidator');
require('mixins/common/table_server_view_mixin');
require('mixins/common/table_server_mixin');
require('mixins/common/track_request_mixin');
require('mixins/common/loading_overlay_support');
require('mixins/main/dashboard/widgets/editable');
require('mixins/main/dashboard/widgets/editable_with_limit');
require('mixins/main/dashboard/widgets/namenode_widget');
require('mixins/main/dashboard/widgets/single_numeric_threshold');
require('mixins/main/host/details/host_components/decommissionable');
require('mixins/main/host/details/host_components/install_component');
require('mixins/main/host/details/actions/install_new_version');
require('mixins/main/host/details/actions/check_host');
require('mixins/main/host/details/support_client_configs_download');
require('mixins/main/service/groups_mapping');
require('mixins/main/service/themes_mapping');
require('mixins/main/service/configs/config_overridable');
require('mixins/main/service/configs/widget_popover_support');
require('mixins/main/service/configs/component_actions_by_configs');
require('mixins/main/service/summary/hdfs_summary_widgets');
require('mixins/routers/redirections');
require('mixins/wizard/wizardProgressPageController');
require('mixins/wizard/wizardDeployProgressController');
require('mixins/wizard/wizardProgressPageView');
require('mixins/wizard/wizardEnableDone');
require('mixins/wizard/selectHost');
require('mixins/wizard/addSecurityConfigs');
require('mixins/wizard/wizard_menu_view');
require('mixins/wizard/wizard_misc_property_checker');
require('mixins/wizard/assign_master_components');
require('mixins/wizard/wizardHostsLoading');
require('mixins/common/configs/widgets/unique/num_llap_nodes');
require('mixins/common/configs/config_recommendations');
require('mixins/common/configs/config_recommendation_parser');
require('mixins/common/configs/config_with_override_recommendation_parser');
require('mixins/common/configs/enhanced_configs');
require('mixins/common/configs/configs_saver');
require('mixins/common/configs/configs_loader');
require('mixins/common/configs/configs_comparator');
require('mixins/common/configs/toggle_isrequired');
require('mixins/common/hosts/host_component_recommendation_mixin');
require('mixins/common/hosts/host_component_validation_mixin');
require('mixins/common/widgets/export_metrics_mixin');
require('mixins/common/widgets/time_range_mixin');
require('mixins/common/widgets/widget_mixin');
require('mixins/common/widgets/widget_section');
require('mixins/unit_convert/base_unit_convert_mixin');
require('mixins/unit_convert/convert_unit_widget_view_mixin');
require('mixins/main/service/configs/hive_interactive_check');
