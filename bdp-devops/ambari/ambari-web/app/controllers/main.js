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

App.MainController = Em.Controller.extend({
  name: 'mainController',
  isUserActive: true,
  checkActivenessInterval: null,
  lastUserActiveTime: null,
  userTimeOut: 0,
  userTimeOutModal: null,

  updateTitle: function(){
    var name = App.router.get('clusterController.clusterName');
    if(App.router.get('clusterInstallCompleted')) {
      if (name && App.router.get('clusterController').get('isLoaded')) {
        name = name.length > 13 ? name.substr(0, 10) + "..." : name;
      } else {
        name = Em.I18n.t('common.loading');
      }
      $('title').text(Em.I18n.t('app.name.subtitle').format(name));
    }
  }.observes('App.router.clusterController.clusterName', 'App.router.clusterInstallCompleted', 'App.router.clusterController.isLoaded'),

  isClusterDataLoaded: Em.computed.alias('App.router.clusterController.isLoaded'),

  clusterDataLoadedPercent: Em.computed.alias('App.router.clusterController.clusterDataLoadedPercent'),
  /**
   * run all processes and cluster's data loading
   */
  initialize: function(){
    // Since we use only defaultTransaction, we can stub <code>removeCleanRecords</code> method,
    // because it would remove from and add records to the same (default) transaction
    App.store.defaultTransaction.reopen({
      removeCleanRecords: Em.K
    });
    const startSubscription = App.router.get('updateController').startSubscriptions.bind(App.router.get('updateController'));
    App.StompClient.connect()
      .done(startSubscription)
      .fail((dfd) => {dfd.always(startSubscription)});
    App.router.get('clusterController').loadClusterData();
  },

  dataLoading: function () {
    var self = this;
    var dfd = $.Deferred();
    if (App.router.get('clusterController.isLoaded')) {
      dfd.resolve();
    } else {
      var interval = setInterval(function () {
        if (self.get('isClusterDataLoaded')) {
          dfd.resolve();
          clearInterval(interval);
        }
      }, 50);
    }
    return dfd.promise();
  },

  /**
   *
   * @param isLoaded {Boolean}
   * @param opts {Object}
   * {
   *   period {Number}
   * }
   * @return {*|{then}}
   */
  isLoading: function(isLoaded, opts) {
    var dfd = $.Deferred();
    var self = this;
    opts = opts || {};
    var period =  opts.period || 20;
    if (this.get(isLoaded)) {
      dfd.resolve();
    } else {
      var interval = setInterval(function () {
        if (self.get(isLoaded)) {
          dfd.resolve();
          clearInterval(interval);
        }
      }, period);
    }
    return dfd.promise();
  },

  startPolling: function () {
    if (App.router.get('applicationController.isExistingClusterDataLoaded')) {
      App.router.get('updateController').set('isWorking', true);
    }
  }.observes('App.router.applicationController.isExistingClusterDataLoaded'),

  stopPolling: function(){
    App.router.get('updateController').set('isWorking', false);
  },

  reloadTimeOut: null,

  pageReload: function () {

    clearTimeout(this.get("reloadTimeOut"));

    this.set('reloadTimeOut',
    setTimeout(function () {
      if (App.clusterStatus.get('isInstalled')) {
        location.reload();
      }
    }, App.pageReloadTime)
    );
  }.observes("App.router.location.lastSetURL", "App.clusterStatus.isInstalled"),

  setAmbariServerVersion: function (data) {
    var clientVersion = App.get('version');
    var serverVersion = (data.RootServiceComponents.component_version).toString();
    this.set('ambariServerVersion', serverVersion);
    if (clientVersion) {
      this.set('versionConflictAlertBody', Em.I18n.t('app.versionMismatchAlert.body').format(serverVersion, clientVersion));
      this.set('isServerClientVersionMismatch', clientVersion !== serverVersion);
    } else {
      this.set('isServerClientVersionMismatch', false);
    }
    App.set('isManagedMySQLForHiveEnabled', App.config.isManagedMySQLForHiveAllowed(data.RootServiceComponents.properties['server.os_family']));
  },

  monitorInactivity: function() {
    var timeout = Number(App.router.get('clusterController.ambariProperties')['user.inactivity.timeout.default']);
    var readonly_timeout = Number(App.router.get('clusterController.ambariProperties')['user.inactivity.timeout.role.readonly.default']);
    var isAdmin = App.get('isAdmin');
    if (isAdmin && timeout > 0) {
      this.set('userTimeOut', timeout * 1000);
    } else if (!isAdmin && readonly_timeout > 0) {
      this.set('userTimeOut', readonly_timeout * 1000);
    }
    if (this.get('userTimeOut') > 0) {
      this.startMonitorInactivity();
    }
  },

  startMonitorInactivity: function() {
    this.set('isUserActive', true);
    this.set('lastUserActiveTime', Date.now());

    this.rebindActivityEventMonitors();
    if (!this.get('checkActivenessInterval')) {
      this.set('checkActivenessInterval', window.setInterval(this.checkActiveness, 1000));
    }
  },

  /* this will be triggerred by user driven events: 'mousemove', 'keypress' and 'click' */
  keepActive: function() {
    var scope = App.router.get('mainController');
    if (scope.get('isUserActive')) {
      scope.set('lastUserActiveTime', Date.now());
    }
  },

  checkActiveness: function() {
    var scope = App.router.get('mainController');
    if (!scope.isOnWizard()) {
      var remainTime = scope.get('userTimeOut') - (Date.now() - scope.get('lastUserActiveTime'));
      if (remainTime < 0) {
        scope.set('isUserActive', false);
        scope.unbindActivityEventMonitors();
        clearInterval(scope.get('checkActivenessInterval'));
        App.router.logOff({});
      } else if (remainTime < App.inactivityRemainTime * 1000 && !scope.userTimeOutModal) {
        // show alert 60 seconds before logging user out
        scope.userTimeOutModal = App.ModalPopup.show({
          primary: Em.I18n.t('common.timeout.warning.popup.primary'),
          secondary: Em.I18n.t('common.timeout.warning.popup.secondary'),
          third: false,
          header: Em.I18n.t('common.timeout.warning.popup.header'),
          showCloseButton: false,
          bodyClass: Ember.View.extend({
            template: Ember.Handlebars.compile('<p>{{view.beforeMsg}}<b>{{view.remainTime}}</b>{{view.afterMsg}}</p>'),
            beforeMsg: Em.I18n.t('common.timeout.warning.popup.body.before'),
            afterMsg: Em.I18n.t('common.timeout.warning.popup.body.after'),
            remainTime: App.inactivityRemainTime,
            didInsertElement: function() {
              var self = this;
              setInterval(function(){self.countDown();}, 1000)
            },
            countDown: function() {
              if (this.get('remainTime') > 0) {
                this.set('remainTime', this.get('remainTime') - 1);
              }
              if (this.get('remainTime') == 0) {
                App.router.logOff({});
              }
            }
          }),
          onPrimary: function() {
            scope.keepActive();
            scope.userTimeOutModal.hide();
            delete scope.userTimeOutModal;
          },
          onSecondary: function() {
            scope.userTimeOutModal.hide();
            delete scope.userTimeOutModal;
            App.router.logOff({});
          }
        });
      }
    }
  },

  rebindActivityEventMonitors: function() {
    this.unbindActivityEventMonitors();
    this.bindActivityEventMonitors();
  },

  isOnWizard: function() {
    var isWizard = window.location.href.indexOf('/step') != -1;
    var isUpgrade = window.location.href.indexOf('/stack/upgrade') != -1;
    return isWizard || isUpgrade;
  },

  bindActivityEventMonitors: function() {
    $(window).bind('mousemove', this.keepActive);
    $(window).bind('keypress', this.keepActive);
    $(window).bind('click', this.keepActive);
    // iframes need to be monitored as well
    var iframes = $('iframe');
    if (iframes.length > 0) {
      for (var i = 0; i < iframes.length; i++) {
        var iframe = iframes[i];
        $(iframe.contentWindow).bind('mousemove', this.keepActive);
        $(iframe.contentWindow).bind('keypress', this.keepActive);
        $(iframe.contentWindow).bind('click', this.keepActive);
      }
    }
  },

  unbindActivityEventMonitors: function() {
    $(window).unbind('mousemove', this.keepActive);
    $(window).unbind('keypress', this.keepActive);
    $(window).unbind('click', this.keepActive);
    // iframes need to be monitored as well
    var iframes = $('iframe');
    if (iframes.length > 0) {
      for (var i = 0; i < iframes.length; i++) {
        var iframe = iframes[i];
        $(iframe.contentWindow).unbind('mousemove', this.keepActive);
        $(iframe.contentWindow).unbind('keypress', this.keepActive);
        $(iframe.contentWindow).unbind('click', this.keepActive);
      }
    }
  }
});
