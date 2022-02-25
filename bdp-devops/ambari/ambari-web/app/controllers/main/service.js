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
var misc = require('utils/misc');

App.MainServiceController = Em.ArrayController.extend(App.SupportClientConfigsDownload, {

  name: 'mainServiceController',

  /**
   * @type {Ember.Object[]}
   */
  content: function () {
    if (!App.router.get('clusterController.isLoaded')) {
      return [];
    }
    return misc.sortByOrder(App.StackService.find().mapProperty('serviceName'), App.Service.find().toArray());
  }.property('App.router.clusterController.isLoaded').volatile(),

  /**
   * Current cluster
   * @type {Ember.Object}
   */
  cluster: function () {
    if (!App.router.get('clusterController.isClusterDataLoaded')) {
      return null;
    }
    return App.Cluster.find().objectAt(0);
  }.property('App.router.clusterController.isClusterDataLoaded'),

  /**
   * Check if all services are installed
   * true - all installed, false - not all
   * @type {bool}
   */
  isAllServicesInstalled: function () {
    if (!this.get('content')) return false;
    var notAvailableServices = App.ServiceSimple.find().filterProperty('doNotShowAndInstall').mapProperty('name');
    var availableServices = App.ServiceSimple.find().filterProperty('doNotShowAndInstall', false);
    var installedServices = this.get('content').filter(function (service) {
      return !notAvailableServices.contains(service.get('serviceName'));
    });
    return installedServices.length == availableServices.length;
  }.property('content.@each', 'content.length'),

  /**
   * Should "Start All"-button be disabled
   * @type {bool}
   */
  isStartAllDisabled: function () {
    if (this.get('isStartStopAllClicked') == true) {
      return true;
    }
    var stoppedServices = this.get('content').filter(function (_service) {
      return (_service.get('healthStatus') === 'red' && !App.get('services.clientOnly').contains(_service.get('serviceName')));
    });
    return (stoppedServices.length === 0); // all green status
  }.property('isStartStopAllClicked', 'content.@each.healthStatus'),

  /**
   * Should "Stop All"-button be disabled
   * @type {bool}
   */
  isStopAllDisabled: function () {
    if (this.get('isStartStopAllClicked') == true) {
      return true;
    }
    return !this.get('content').someProperty('healthStatus', 'green');
  }.property('isStartStopAllClicked', 'content.@each.healthStatus'),

  /**
   * Should "Refresh All"-button be disabled
   * @type {bool}
   */
  isRestartAllRequiredDisabled: Em.computed.everyBy('content', 'isRestartRequired', false),

  /**
   * @type {bool}
   */
  isStartStopAllClicked: Em.computed.notEqual('App.router.backgroundOperationsController.runningOperationsCount', 0),

  /**
   * Callback for <code>start all service</code> button
   * @return {App.ModalPopup|null}
   * @method startAllService
   */
  startAllService: function (event) {
    return this.startStopAllService(event, 'STARTED');
  },

  /**
   * Callback for <code>stop all service</code> button
   * @return {App.ModalPopup|null}
   * @method stopAllService
   */
  stopAllService: function (event) {
    return this.startStopAllService(event, 'INSTALLED');
  },

  /**
   * Common method for "start-all", "stop-all" calls
   * @param {object} event
   * @param {string} state 'STARTED|INSTALLED'
   * @returns {App.ModalPopup|null}
   * @method startStopAllService
   */
  startStopAllService: function(event, state) {
    if ($(event.target).hasClass('disabled') || $(event.target.parentElement).hasClass('disabled')) {
      return null;
    }
    var self = this;
    var bodyMessage = Em.Object.create({
      confirmMsg: state == 'INSTALLED' ? Em.I18n.t('services.service.stopAll.confirmMsg') : Em.I18n.t('services.service.startAll.confirmMsg'),
      confirmButton: state == 'INSTALLED' ? Em.I18n.t('services.service.stop.confirmButton') : Em.I18n.t('services.service.start.confirmButton')
    });

    if (state == 'INSTALLED' && App.Service.find().filterProperty('serviceName', 'HDFS').someProperty('workStatus', App.HostComponentStatus.started)) {
      App.router.get('mainServiceItemController').checkNnLastCheckpointTime(function () {
        return App.showConfirmationFeedBackPopup(function (query) {
          self.allServicesCall(state, query);
        }, bodyMessage);
      });
    } else {
      return App.showConfirmationFeedBackPopup(function (query) {
        self.allServicesCall(state, query);
      }, bodyMessage);
    }
  },

  /**
   * Download client configs for all services
   */
  downloadAllClientConfigs: function() {
    this.downloadClientConfigsCall({resourceType: this.resourceTypeEnum.CLUSTER});
  },

  /**
   * Do request to server for "start|stop" all services
   * @param {string} state "STARTED|INSTALLED"
   * @param {object} query
   * @method allServicesCall
   * @return {$.ajax}
   */
  allServicesCall: function (state, query) {
    var context = (state == 'INSTALLED') ? App.BackgroundOperationsController.CommandContexts.STOP_ALL_SERVICES :
      App.BackgroundOperationsController.CommandContexts.START_ALL_SERVICES;
    return App.ajax.send({
      name: 'common.services.update',
      sender: this,
      data: {
        context: context,
        ServiceInfo: {
          state: state
        },
        query: query
      },
      success: 'allServicesCallSuccessCallback',
      error: 'allServicesCallErrorCallback',
      showLoadingPopup: true
    });
  },

  /**
   * Restart all services - restarts by sending one RESTART command
   */
  restartAllServices: function () {
    App.ajax.send({
      name: 'restart.allServices',
      sender: this,
      showLoadingPopup: true
    });
  },

  /**
   * Restart all services - stops all services, then starts them back
   */
  stopAndStartAllServices: function () {
    this.silentStopAllServices();
  },

  /**
   * Silent stop all services - without user confirmation
   * @returns {$.ajax}
   */
  silentStopAllServices: function () {
    return App.ajax.send({
      name: 'common.services.update',
      sender: this,
      data: {
        context: App.BackgroundOperationsController.CommandContexts.STOP_ALL_SERVICES,
        ServiceInfo: {
          state: 'INSTALLED'
        }
      },
      success: 'silentStopSuccess',
      showLoadingPopup: true
    });
  },

  isStopAllServicesFailed: function() {
    var workStatuses = App.Service.find().mapProperty('workStatus');
    for (var i = 0; i < workStatuses.length; i++) {
      if (workStatuses[i] !== 'INSTALLED' && workStatuses[i] !== 'STOPPING') {
        return true;
      }
    }
    return false;
  },

  /**
   * Success callback for silent stop
   */
  silentStopSuccess: function () {
    var self = this;

    App.router.get('userSettingsController').dataLoading('show_bg').done(function (initValue) {
      if (initValue) {
        App.router.get('backgroundOperationsController').showPopup();
      }

      Em.run.later(function () {
        self.set('shouldStart', true);
      }, App.bgOperationsUpdateInterval);
    });
  },

  /**
   * Silent start all services - without user confirmation
   */
  silentStartAllServices: function () {
    if (
      !App.router.get('backgroundOperationsController').get('runningOperationsCount')
      && this.get('shouldStart')
      && !this.isStopAllServicesFailed()
    ) {
      this.set('shouldStart', false);
      return App.ajax.send({
        name: 'common.services.update',
        sender: this,
        data: {
          context: App.BackgroundOperationsController.CommandContexts.START_ALL_SERVICES,
          ServiceInfo: {
            state: 'STARTED'
          }
        },
        success: 'silentCallSuccessCallback',
        showLoadingPopup: true
      });
    }
  }.observes('shouldStart', 'controllers.backgroundOperationsController.runningOperationsCount'),

  /**
   * Success callback for silent start
   */
  silentCallSuccessCallback: function () {
    // load data (if we need to show this background operations popup) from persist
    App.router.get('userSettingsController').dataLoading('show_bg').done(function (initValue) {
      if (initValue) {
        App.router.get('backgroundOperationsController').showPopup();
      }
    });
  },

  /**
   * Success-callback for all-services request
   * @param {object} data
   * @param {object} xhr
   * @param {object} params
   * @method allServicesCallSuccessCallback
   */
  allServicesCallSuccessCallback: function (data, xhr, params) {
    params.query.set('status', 'SUCCESS');

    // load data (if we need to show this background operations popup) from persist
    App.router.get('userSettingsController').dataLoading('show_bg').done(function (initValue) {
      if (initValue) {
        App.router.get('backgroundOperationsController').showPopup();
      }
    });
  },

  /**
   * Error-callback for all-services request
   * @param {object} request
   * @param {object} ajaxOptions
   * @param {string} error
   * @param {object} opt
   * @param {object} params
   * @method allServicesCallErrorCallback
   */
  allServicesCallErrorCallback: function (request, ajaxOptions, error, opt, params) {
    params.query.set('status', 'FAIL');
    App.ajax.defaultErrorHandler(request, opt.url, opt.type, request.status);
  },

  /**
   * "Add-service"-click handler
   * @method gotoAddService
   */
  gotoAddService: function () {
    if (this.get('isAllServicesInstalled')) {
      return;
    }
    App.router.get('addServiceController').setDBProperty('onClosePath', 'main.services.index');
    App.router.transitionTo('main.serviceAdd');
  },

  /**
   * Show confirmation popup and send request to restart all host components with stale_configs=true
   */
  restartAllRequired: function () {
    var self = this;
    if (!this.get('isRestartAllRequiredDisabled')) {
      return App.showConfirmationPopup(function () {
            self.restartHostComponents();
          }, Em.I18n.t('services.service.refreshAll.confirmMsg').format(
              App.Service.find().filterProperty('isRestartRequired').mapProperty('displayName').uniq().join(', ')),
          null,
          null,
          Em.I18n.t('services.service.restartAll.confirmButton')
      );
    } else {
      return null;
    }
  },

  /**
   * Send request restart host components from hostComponentsToRestart
   * @returns {$.ajax}
   */
  restartHostComponents: function () {
    App.ajax.send({
      name: 'restart.staleConfigs',
      sender: this,
      success: 'restartAllRequiredSuccessCallback',
      showLoadingPopup: true
    });
  },

  /**
   * Success callback for restartAllRequired
   */
  restartAllRequiredSuccessCallback: function () {
    // load data (if we need to show this background operations popup) from persist
    App.router.get('userSettingsController').dataLoading('show_bg').done(function (initValue) {
      if (initValue) {
        App.router.get('backgroundOperationsController').showPopup();
      }
    });
  }
});
