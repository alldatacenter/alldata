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

App.MainAlertDefinitionsController = Em.ArrayController.extend({

  name: 'mainAlertDefinitionsController',

  /**
   * Define whether restore filter conditions from local db
   * @type {Boolean}
   */
  showFilterConditionsFirstLoad: false,

  contentUpdater: null,

  /**
   * List of all <code>App.AlertDefinition</code>
   * @type {App.AlertDefinition[]}
   */
  content: App.AlertDefinition.find(),

  /**
   * Generates key for alert summary that represents current state
   */
  getSummaryCache: function () {
    var res = '';
    this.get('content').forEach(function(o) {
      var summary = o.get('summary');
      o.get('order').forEach(function (state) {
        res += summary[state] ? summary[state].count + summary[state].maintenanceCount : 0;
      });
    });

    return res;
   },

  generateCacheByKey: function(key) {
    if (key === 'summary') {
      return this.getSummaryCache();
    }

    return this.get('content').mapProperty(key).join('');
  },

  contentWasChanged: function(key) {
    var updatedCache = this.generateCacheByKey(key);
    if (this.get('cache.' + key) !== updatedCache) {
      this.set('cache.' + key, updatedCache);
      this.propertyDidChange('contentUpdater');
    }
  },

  cache: {
    'label': '',
    'summary': '',
    'serviceName': '',
    'lastTriggered': '',
    'enabled': ''
  },

  /**
   * Enable/disable alertDefinition confirmation popup
   * @param {object} event
   * @method toggleState
   * @return {App.ModalPopup}
   */
  toggleState: function (event) {
    var alertDefinition = event.context;
    var self = this;
    var bodyMessage = Em.Object.create({
      confirmMsg: alertDefinition.get('enabled') ? Em.I18n.t('alerts.table.state.enabled.confirm.msg') : Em.I18n.t('alerts.table.state.disabled.confirm.msg'),
      confirmButton: alertDefinition.get('enabled') ? Em.I18n.t('alerts.table.state.enabled.confirm.btn') : Em.I18n.t('alerts.table.state.disabled.confirm.btn')
    });

    return App.showConfirmationFeedBackPopup(function (query) {
      self.toggleDefinitionState(alertDefinition);
    }, bodyMessage);
  },

  /**
   * Enable/disable alertDefinition
   * @param {object} alertDefinition
   * @returns {$.ajax}
   * @method toggleDefinitionState
   */
  toggleDefinitionState: function (alertDefinition) {
    var newState = !alertDefinition.get('enabled');
    alertDefinition.set('enabled', newState);
    Em.run.next(function () {
      App.tooltip($('.enable-disable-button'));
    });
    return App.ajax.send({
      name: 'alerts.update_alert_definition',
      sender: this,
      data: {
        id: alertDefinition.get('id'),
        data: {
          "AlertDefinition/enabled": newState
        }
      }
    });
  },

  /**
   *  ========================== alerts notifications dropdown dialog =========================
   */

  /**
   * Number of all critical and warning alert instances
   * Calculation is based on each <code>alertDefinitions.summary</code>
   * @type {Number}
   */
  unhealthyAlertInstancesCount: function () {
    return this.get('criticalAlertInstancesCount') + this.get('warningAlertInstancesCount');
  }.property('criticalAlertInstancesCount', 'warningAlertInstancesCount'),

  criticalAlertInstancesCount: function () {
    return this.get('content').map(function (alertDefinition) {
      return alertDefinition.getWithDefault('summary.CRITICAL.count', 0);
    }).reduce(Em.sum, 0);
  }.property('content.@each.summary'),

  warningAlertInstancesCount: function () {
    return this.get('content').map(function (alertDefinition) {
      return alertDefinition.getWithDefault('summary.WARNING.count', 0);
    }).reduce(Em.sum, 0);
  }.property('content.@each.summary'),

  /**
   * if critical alerts exist, the alert badge should be red.
   * @type {Boolean}
   */
  isCriticalAlerts: function () {
    return this.get('content').invoke('getWithDefault', 'summary.CRITICAL.count', 0).reduce(Em.sum, 0) !== 0;
  }.property('content.@each.summary')
});
