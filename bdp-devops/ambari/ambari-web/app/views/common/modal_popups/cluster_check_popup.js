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

var App = require('app');
require('views/main/admin/stack_upgrade/custom_cluster_checks/custom_cluster_checks_view');
require('views/main/admin/stack_upgrade/custom_cluster_checks/custom_cluster_checks_autostart_view');
require('views/main/admin/stack_upgrade/custom_cluster_checks/custom_cluster_checks_maintenance_view');
require('views/main/admin/stack_upgrade/custom_cluster_checks/custom_services_checks');
require('views/main/admin/stack_upgrade/custom_cluster_checks/custom_cluster_checks_prev_upgrade_view');
require('views/main/admin/stack_upgrade/custom_cluster_checks/custom_cluster_checks_host_version_view');
require('views/main/admin/stack_upgrade/custom_cluster_checks/custom_cluster_checks_snn');
require('views/main/admin/stack_upgrade/custom_cluster_checks/custom_cluster_checks_atlas_view');
require('views/main/admin/stack_upgrade/custom_cluster_checks/add_metastore_view');
require('views/main/admin/stack_upgrade/custom_cluster_checks/custom_cluster_ckecks_host_hearbeat_view');
require('views/main/admin/stack_upgrade/custom_cluster_checks/custom_cluster_ckecks_alerts_view');
require('views/main/admin/stack_upgrade/custom_cluster_checks/custom_cluster_checks_service_warning_view');
require('views/main/admin/stack_upgrade/custom_cluster_checks/custom_cluster_checks_components_installation');

const customCheckViewsMap = {
  'SERVICES_UP': App.ServiceUpCheckView,
  'AUTO_START_DISABLED': App.AutostartDisabledCheckView,
  'HOSTS_MASTER_MAINTENANCE': App.MasterMaintenanceDisabledCheckView,
  'PREVIOUS_UPGRADE_COMPLETED': App.PrevUpgradeNotCompletedView,
  'HOSTS_REPOSITORY_VERSION': App.HostHaveVersionInstalledCheckView,
  'SECONDARY_NAMENODE_MUST_BE_DELETED': App.deleteSNNView,
  'SERVICES_HIVE_MULTIPLE_METASTORES': App.addMetastoreView,
  'SERVICE_CHECK' : App.ServicesChecksView,
  'SERVICE_PRESENCE_CHECK': App.AtlasInstalledCheckView,
  'HOSTS_HEARTBEAT' : App.HostsHeartbeatView,
  'HEALTH': App.AlertsChecksView,
  'COMPONENTS_INSTALLATION': App.ComponentsInstallationFailedView
};

function mapUpgradeChecks(items) {
  return items.map(item => Em.getProperties(item.UpgradeChecks, ['failed_on', 'reason', 'check', 'customView']));
}

function setCustomCheckViews(checks) {
  checks.forEach(function (item) {
    const check = item.UpgradeChecks;
    if (customCheckViewsMap[check.id]) {
      check.customView = customCheckViewsMap[check.id].extend({check: check});
    }
    else{
      if (check.status == 'WARNING' && check.check_type == 'SERVICE'){
        check.customView = App.servicesWarningView.extend({check: check});
      }
    }

  })
}

/**
 * popup to display requirements that are not met
 * for current action
 * @param data
 * @param popup
 * @param configs
 * @returns {*|void}
 */
App.showClusterCheckPopup = function (data, popup, configs) {
  var fails = data.items.filterProperty('UpgradeChecks.status', 'FAIL'),
    warnings = data.items.filterProperty('UpgradeChecks.status', 'WARNING'),
    bypass = data.items.filterProperty('UpgradeChecks.status', 'BYPASS'),
    configsMergeConflicts = configs ? configs.filterProperty('wasModified', false) : [],
    configsRecommendations = configs ? configs.filterProperty('wasModified', true) : [],
    primary,
    secondary;
  popup = popup || {};
  
  setCustomCheckViews(fails);
  setCustomCheckViews(warnings);

  if (Em.isNone(popup.primary)) {
    primary = fails.length ? Em.I18n.t('common.dismiss') : Em.I18n.t('common.proceedAnyway');
  }
  else {
    primary = popup.primary;
  }

  if (Em.isNone(popup.secondary)) {
    secondary = fails.length ? false : Em.I18n.t('common.cancel');
  }
  else {
    secondary = popup.secondary;
  }

  return App.ModalPopup.show({
    primary: primary,
    secondary: secondary,
    header: popup.header,
    classNames: ['cluster-check-popup'],
    modalDialogClasses: ['modal-xlg'],
    bodyClass: Em.View.extend({
      failTitle: popup.failTitle,
      failAlert: popup.failAlert,
      warningTitle: popup.warningTitle,
      warningAlert: popup.warningAlert,
      templateName: require('templates/common/modal_popups/cluster_check_dialog'),
      warnings: mapUpgradeChecks(warnings),
      fails: mapUpgradeChecks(fails),
      bypass: mapUpgradeChecks(bypass), // errors that can be bypassed
      hasConfigsMergeConflicts: configsMergeConflicts.length > 0,
      hasConfigsRecommendations: configsRecommendations.length > 0,
      configsMergeTable: App.getMergeConflictsView(configsMergeConflicts),
      configsRecommendTable: App.getNewStackRecommendationsView(configsRecommendations),
      servicesChecksView: App.ServicesChecksView,
      isAllPassed: !fails.length && !warnings.length && !bypass.length
      && !configsMergeConflicts.length && !configsRecommendations.length
    }),
    closeParent: function () {
      popup.closeParent();
    },
    onPrimary: function () {
      this._super();
      if (!popup.noCallbackCondition && popup.callback) {
        popup.callback();
      }
    },
    didInsertElement: function () {
      this._super();
      this.fitHeight();
    }
  });
};

App.getMergeConflictsView = function (configs) {
  return Em.View.extend({
    templateName: require('templates/main/admin/stack_upgrade/upgrade_configs_merge_table'),
    configs: configs
  });
};

App.getNewStackRecommendationsView = function (configs) {
  return Em.View.extend({
    templateName: require('templates/main/admin/stack_upgrade/upgrade_configs_recommend_table'),
    configs: configs
  });
};
