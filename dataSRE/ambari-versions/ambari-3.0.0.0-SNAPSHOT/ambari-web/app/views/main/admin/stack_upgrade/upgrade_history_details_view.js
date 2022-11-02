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
var date = require('utils/date/date');

App.MainAdminStackUpgradeHistoryDetailsView = Em.View.extend({
  controllerBinding: 'App.router.mainAdminStackUpgradeHistoryController',
  templateName: require('templates/main/admin/stack_upgrade/upgrade_history_details'),
  isReady: false,

  willInsertElement: function(){
    var self = this;
    this.get('controller').loadStackUpgradeRecord().done(function(){
      self.populateUpgradeHistoryRecord();
    });
  },

  willDestroyElement: function () {
    this.set('isReady', false);
  },

  populateUpgradeHistoryRecord: function(){
    var upgradeData = this.get('controller').get('upgradeData')
    this.set('isReady', (upgradeData != null))
  },

  /**
   * progress value is rounded to floor
   * @type {number}
   */
  overallProgress: function () {
    return Math.floor(this.get('controller.upgradeData.Upgrade.progress_percent'));
  }.property('controller.upgradeData.Upgrade.progress_percent'),

  /**
   * label of Upgrade status
   * @type {string}
   */
  upgradeStatusLabel: function() {
    var labelKey = null;
    switch (this.get('controller.upgradeData.Upgrade.request_status')) {
      case 'QUEUED':
      case 'PENDING':
      case 'IN_PROGRESS':
        labelKey = 'admin.stackUpgrade.state.inProgress';
        break;
      case 'COMPLETED':
        labelKey = 'admin.stackUpgrade.state.completed';
        break;
      case 'ABORTED':
        labelKey = 'admin.stackUpgrade.state.paused';
        break;
      case 'TIMEDOUT':
      case 'FAILED':
      case 'HOLDING_FAILED':
      case 'HOLDING_TIMEDOUT':
      case 'HOLDING':
        labelKey = 'admin.stackUpgrade.state.paused';
        break;
    }
    if (labelKey) {
      labelKey += (this.get('controller.isDowngrade')) ? '.downgrade' : "";
      return Em.I18n.t(labelKey);
    } else {
      return "";
    }
  }.property('controller.upgradeData.Upgrade.request_status', 'controller.isDowngrade')
});