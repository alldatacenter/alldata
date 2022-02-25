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
var batchUtils = require('utils/batch_scheduled_requests');

App.MainServiceInfoConfigsView = Em.View.extend({

  templateName: require('templates/main/service/info/configs'),

  didInsertElement: function () {
    var self = this;
    App.router.get('mainController').isLoading.call(App.router.get('clusterController'), 'isConfigsPropertiesLoaded').done(function () {
      self.get('controller').loadStep();
    });
    this.resetConfigTabSelection();
  },

  /**
   * If user A is on the Service Configs page and B starts some Wizard, user A should be moved-out and then moved-in this page
   * It's done to properly disable "admin"-elements
   * This code can't be moved to the controller, because it should work only if user is in the configs page (this view exists)
   */
  simulateRefresh: function() {
    App.router.transitionTo('main.services.service.summary', this.get('controller.content'));
    App.router.transitionTo('main.services.service.configs', this.get('controller.content'));
  }.observes('App.router.wizardWatcherController.isWizardRunning'),

  willDestroyElement: function() {
    this.get('controller').clearStep();
  },

  /**
   * reset selection flag of tabs on entering Configs page
   */
  resetConfigTabSelection: function() {
    App.Tab.find().filterProperty('serviceName', this.get('controller.content.serviceName')).setEach('isActive', false);
  },

  /**
   * Number of components that should be restarted
   * @type {number}
   */
  componentsCount: null,

  /**
   * Number of hosts with components that should be restarted
   * @type {number}
   */
  hostsCount: null,

  /**
   * @type {boolean}
   */
  isStopCommand: true,

  /**
   * @method updateComponentInformation
   */
  updateComponentInformation: function() {
    var hc = this.get('controller.content.restartRequiredHostsAndComponents');
    var componentsCount = 0;
    var hosts = Em.keys(hc);
    hosts.forEach(function (host) {
      componentsCount += hc[host].length;
    });
    this.setProperties({
      componentsCount: componentsCount,
      hostsCount: hosts.length
    });
  }.observes('controller.content.restartRequiredHostsAndComponents'),

  /**
   * @type {string}
   */
  rollingRestartSlaveComponentName: function() {
    return batchUtils.getRollingRestartComponentName(this.get('controller.content.serviceName'));
  }.property('controller.content.serviceName'),
  
  /**
   * @type {boolean}
   */
  isRollingRestartSlaveComponentPresent: function() {
    return App.SlaveComponent.find(this.get('rollingRestartSlaveComponentName')).get('totalCount') > 0;
  }.property('rollingRestartSlaveComponentName'),

  /**
   * @type {string}
   */
  rollingRestartActionName : function() {
    var componentName = this.get('rollingRestartSlaveComponentName');
    return componentName ? Em.I18n.t('rollingrestart.dialog.title').format(App.format.role(componentName, false)) : '';
  }.property('rollingRestartSlaveComponentName')

});
