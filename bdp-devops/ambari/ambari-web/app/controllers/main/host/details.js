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
var hostsManagement = require('utils/hosts');
var stringUtils = require('utils/string_utils');
require('utils/configs/add_component_config_initializer');

App.MainHostDetailsController = Em.Controller.extend(App.SupportClientConfigsDownload, App.InstallComponent, App.InstallNewVersion, App.CheckHostMixin, App.TrackRequestMixin, {

  name: 'mainHostDetailsController',

  /**
   * Viewed host
   * @type {App.Host|null}
   */
  content: null,

  /**
   * Is check host procedure finished
   * @type {bool}
   */
  checkHostFinished: null,

  /**
   * Does user come from hosts page
   * @type {bool}
   */
  isFromHosts: false,

  /**
   * Determines whether we are adding Hive Server2 component
   * @type {bool}
   */
  addHiveServer: false,

  /**
   * Determines whether we are adding ZooKeeper Server component
   * @type {bool}
   */
  addZooKeeperServer: false,

  /**
   * path to page visited before
   * @type {string}
   */
  referer: '',

  /**
   *  Host on which Hive Metastore will be added
   * @type {string}
   */
  hiveMetastoreHost: '',

  /**
   * Deferred object will be resolved when Oozie configs are downloaded
   * @type {object}
   */
  isOozieConfigLoaded: $.Deferred(),

  /**
   * @type {bool}
   */
  isOozieServerAddable: true,

  isConfigsLoadingInProgress: false,

  addDeleteComponentsMap: {
    'ZOOKEEPER_SERVER': {
      addPropertyName: 'addZooKeeperServer',
      deletePropertyName: 'fromDeleteZkServer',
      configTagsCallbackName: 'loadZookeeperConfigs',
      configsCallbackName: 'saveZkConfigs'
    },
    'HIVE_METASTORE': {
      deletePropertyName: 'deleteHiveMetaStore',
      hostPropertyName: 'hiveMetastoreHost',
      configTagsCallbackName: 'loadHiveConfigs',
      configsCallbackName: 'onLoadHiveConfigs'
    },
    'WEBHCAT_SERVER': {
      deletePropertyName: 'deleteWebHCatServer',
      hostPropertyName: 'webhcatServerHost',
      configTagsCallbackName: 'loadWebHCatConfigs',
      configsCallbackName: 'onLoadHiveConfigs'
    },
    'HIVE_SERVER': {
      addPropertyName: 'addHiveServer',
      deletePropertyName: 'deleteHiveServer',
      configTagsCallbackName: 'loadHiveConfigs',
      configsCallbackName: 'onLoadHiveConfigs'
    },
    'NIMBUS': {
      deletePropertyName: 'deleteNimbusHost',
      hostPropertyName: 'nimbusHost',
      configTagsCallbackName: 'loadStormConfigs',
      configsCallbackName: 'onLoadStormConfigs'
    },
    'ATLAS_SERVER': {
      deletePropertyName: 'deleteAtlasServer',
      hostPropertyName: 'atlasServer',
      configTagsCallbackName: 'loadAtlasConfigs',
      configsCallbackName: 'onLoadAtlasConfigs'
    },
    'RANGER_KMS_SERVER': {
      deletePropertyName: 'deleteRangerKMSServer',
      hostPropertyName: 'rangerKMSServerHost',
      configTagsCallbackName: 'loadRangerConfigs',
      configsCallbackName: 'onLoadRangerConfigs'
    }
  },

  zooKeeperRelatedServices: [
    {
      serviceName: 'HIVE',
      typesToLoad: ['hive-site', 'webhcat-site'],
      typesToSave: ['hive-site', 'webhcat-site']
    },
    {
      serviceName: 'YARN',
      typesToLoad: ['yarn-site', 'zoo.cfg'],
      typesToSave: ['yarn-site']
    },
    {
      serviceName: 'HBASE',
      typesToLoad: ['hbase-site'],
      typesToSave: ['hbase-site']
    },
    {
      serviceName: 'ACCUMULO',
      typesToLoad: ['accumulo-site'],
      typesToSave: ['accumulo-site']
    },
    {
      serviceName: 'KAFKA',
      typesToLoad: ['kafka-broker'],
      typesToSave: ['kafka-broker']
    },
    {
      serviceName: 'ATLAS',
      typesToLoad: ['application-properties', 'infra-solr-env'],
      typesToSave: ['application-properties']
    },
    {
      serviceName: 'STORM',
      typesToLoad: ['storm-site'],
      typesToSave: ['storm-site']
    }
  ],

  /**
   * Determines whether adding/deleting host component requires configs changes
   * @type {Boolean}
   */
  isReconfigureRequired: false,

  /**
   * Contains component-related config properties loaded from server
   * @type {Object|null}
   */
  configs: null,

  /**
   * Array of all properties affected by adding/deleting host component
   * @type {Array}
   */
  allPropertiesToChange: [],

  /**
   * Array of editable properties affected by adding/deleting host component
   * @type {Array}
   */
  recommendedPropertiesToChange: [],

  /**
   * Array of non-editable properties affected by adding/deleting host component
   * @type {Array}
   */
  requiredPropertiesToChange: [],

  /**
   * Properties affected by adding/deleting host component, grouped by service, formatted for PUT call
   * @type {Array}
   */
  groupedPropertiesToChange: [],

  hasPropertiesToChange: Em.computed.or('recommendedPropertiesToChange.length', 'requiredPropertiesToChange.length'),

  addDeleteComponentPopupBody: Em.View.extend({
    templateName: require('templates/main/host/details/addDeleteComponentPopup'),
    commonMessage: '',
    manualKerberosWarning: App.get('router.mainAdminKerberosController.isManualKerberos') ?
      Em.I18n.t('hosts.host.manualKerberosWarning') : '',
    lastComponent: false,
    lastComponentError: '',
    fromServiceSummary: false,
    selectedHost: null,
    anyHostsWithoutComponent: true
  }),

  saveLoadedConfigs: function (data) {
    this.set('configs', {
      items: data.items.map((item) => {
        return {
          type: item.type,
          properties_attributes: item.properties_attributes,
          properties: Em.copy(item.properties)
        }
      })
    });
  },

  clearConfigsChanges: function (shouldKeepLoadedConfigs) {
    var arrayNames = ['allPropertiesToChange', 'recommendedPropertiesToChange', 'requiredPropertiesToChange', 'groupedPropertiesToChange'];
    this.abortRequests();
    arrayNames.forEach(function (arrayName) {
      this.get(arrayName).clear();
    }, this);
    this.set('isReconfigureRequired', false);
    if (!shouldKeepLoadedConfigs) {
      this.set('configs', null);
    }
  },

  applyConfigsCustomization: function () {
    this.get('recommendedPropertiesToChange').forEach(function (property) {
      var value = property.saveRecommended ? property.recommendedValue : property.initialValue,
        filename = property.propertyFileName;
      if (this.get('groupedPropertiesToChange.length')) {
        var group = this.get('groupedPropertiesToChange').find(function (item) {
          return item.properties.hasOwnProperty(filename);
        });
        if (group) {
          group.properties[filename][property.propertyName] = value;
        }
      }
    }, this);
  },

  /**
   * Open dashboard page
   * @method routeHome
   */
  routeHome: function () {
    App.router.transitionTo('main.dashboard.index');
  },

  /**
   * List of active (not in passive state) host components
   * @type {Ember.Enumerable}
   */
  serviceActiveComponents: function () {
    return this.get('content.hostComponents').filterProperty('service.isInPassive', false);
  }.property('content.hostComponents'),

  /**
   * List of active host components which aren't clients
   * @type {Ember.Enumerable}
   */
  serviceNonClientActiveComponents: Em.computed.filterBy('serviceActiveComponents', 'isClient', false),

  /**
   * send command to server to start selected host component
   * @param {object} event
   * @method startComponent
   */
  startComponent: function (event) {
    var self = this;
    var component = event.context;
    return App.showConfirmationPopup(function () {
      var context = Em.I18n.t('requestInfo.startHostComponent') + " " + component.get('displayName');
      self.sendComponentCommand(component, context, App.HostComponentStatus.started);
    }, Em.I18n.t('question.sure.start').format(component.get('displayName')));
  },

  /**
   * send command to server to stop selected host component
   * @param {object} event
   * @method startComponent
   */
  stopComponent: function (event) {
    var self = this;
    var component = event.context;
    if (event.context.get('componentName') === 'NAMENODE' ) {
      this.checkNnLastCheckpointTime(function () {
        return App.showConfirmationPopup(function () {
          var context = Em.I18n.t('requestInfo.stopHostComponent') + " " + component.get('displayName');
          self.sendComponentCommand(component, context, App.HostComponentStatus.stopped);
        }, Em.I18n.t('question.sure.stop').format(component.get('displayName')));
      });
    } else {
      return App.showConfirmationPopup(function () {
        var context = Em.I18n.t('requestInfo.stopHostComponent') + " " + component.get('displayName');
        self.sendComponentCommand(component, context, App.HostComponentStatus.stopped);
      }, Em.I18n.t('question.sure.stop').format(component.get('displayName')));
    }
  },
  /**
   * PUTs a command to server to start/stop a component. If no
   * specific component is provided, all components are started.
   * @param {object} component  When <code>null</code> all startable components are started.
   * @param {String} context  Context under which this command is beign sent.
   * @param {String} state - desired state of component can be 'STARTED' or 'STOPPED'
   * @method sendComponentCommand
   */
  sendComponentCommand: function (component, context, state) {
    var data = {
      hostName: this.get('content.hostName'),
      context: context,
      component: component,
      HostRoles: {
        state: state
      }
    };
    if (Array.isArray(component)) {
      data.query = "HostRoles/component_name.in(" + component.mapProperty('componentName').join(',') + ")";
    } else {
      data.componentName = component.get('componentName');
      data.serviceName = component.get('service.serviceName');
    }
    App.ajax.send({
      name: (Array.isArray(component)) ? 'common.host.host_components.update' : 'common.host.host_component.update',
      sender: this,
      data: data,
      success: 'sendComponentCommandSuccessCallback',
      error: 'ajaxErrorCallback',
      showLoadingPopup: true
    });
  },

  /**
   * Success callback for stop/start host component request
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method stopComponentSuccessCallback
   */
  sendComponentCommandSuccessCallback: function (data, opt, params) {
    var running = (params.HostRoles.state === App.HostComponentStatus.stopped) ? App.HostComponentStatus.stopping : App.HostComponentStatus.starting;
    params.component.set('workStatus', running);
    if (App.get('testMode')) {
      this.mimicWorkStatusChange(params.component, running, params.HostRoles.state);
    }
    this.showBackgroundOperationsPopup();
  },

  /**
   * Return true if hdfs user data is loaded via App.MainServiceInfoConfigsController
   */
  getHdfsUser: function () {
    var self = this;
    var dfd = $.Deferred();
    var miscController = App.MainAdminServiceAccountsController.create();
    miscController.loadUsers();
    miscController.addObserver('dataIsLoaded', this, function() {
      if (miscController.get('dataIsLoaded') && miscController.get('users')) {
        if (miscController.get('users').someProperty('name', 'hdfs_user')) {
          self.set('hdfsUser', miscController.get('users').findProperty('name', 'hdfs_user').get('value'));
        } else {
          self.set('hdfsUser', '&lt;hdfs-user&gt;');
        }
        dfd.resolve();
        miscController.destroy();
      }
    });
    return dfd.promise();
  },

  /**
   * this function will be called from :1) stop NN 2) restart NN 3) stop all components
   * @param callback - callback function to continue next operation
   * @param hostname - namenode host (by default is current host)
   */
  checkNnLastCheckpointTime: function(callback, hostName) {
    var self = this;
    this.pullNnCheckPointTime(hostName).complete(function () {
      var isNNCheckpointTooOld = self.get('isNNCheckpointTooOld');
      self.set('isNNCheckpointTooOld', null);
      if (isNNCheckpointTooOld) {
        // too old
        self.getHdfsUser().done(function() {
          var msg = Em.Object.create({
            confirmMsg: Em.I18n.t('services.service.stop.HDFS.warningMsg.checkPointTooOld').format(App.nnCheckpointAgeAlertThreshold) +
              Em.I18n.t('services.service.stop.HDFS.warningMsg.checkPointTooOld.makeSure') +
              Em.I18n.t('services.service.stop.HDFS.warningMsg.checkPointTooOld.instructions.singleHost.login').format(isNNCheckpointTooOld) +
              Em.I18n.t('services.service.stop.HDFS.warningMsg.checkPointTooOld.instructions').format(self.get('hdfsUser')),
            confirmButton: Em.I18n.t('common.next')
          });
          return App.showConfirmationFeedBackPopup(callback, msg);
        });
      } else if (isNNCheckpointTooOld == null) {
        // not available
        return App.showConfirmationPopup(
          callback, Em.I18n.t('services.service.stop.HDFS.warningMsg.checkPointNA'), null,
          Em.I18n.t('common.warning'), Em.I18n.t('common.proceedAnyway'), 'danger'
        );
      } else {
        // still young
        callback();
      }
    });
  },

  pullNnCheckPointTime: function (hostName) {
    return App.ajax.send({
      name: 'common.host_component.getNnCheckPointTime',
      sender: this,
      data: {
        host: hostName || this.get('content.hostName')
      },
      success: 'parseNnCheckPointTime'
    });
  },

  parseNnCheckPointTime: function (data) {
    var lastCheckpointTime = Em.get(data, 'metrics.dfs.FSNamesystem.LastCheckpointTime');
    var hostName = Em.get(data, 'HostRoles.host_name');

    if (Em.get(data, 'metrics.dfs.FSNamesystem.HAState') === 'active') {
      if (!lastCheckpointTime) {
        this.set("isNNCheckpointTooOld", null);
      } else {
        var time_criteria = App.nnCheckpointAgeAlertThreshold; // time in hours to define how many hours ago is too old
        var time_ago = (Math.round(App.dateTime() / 1000) - (time_criteria * 3600)) *1000;
        if (lastCheckpointTime <= time_ago) {
          // too old, set the effected hostName
          this.set("isNNCheckpointTooOld", hostName);
        } else {
          // still young
          this.set("isNNCheckpointTooOld", false);
        }
      }
    } else if (Em.get(data, 'metrics.dfs.FSNamesystem.HAState') === 'standby') {
      this.set("isNNCheckpointTooOld", false);
    }
  },

  /**
   * mimic status transition in test mode
   * @param entity
   * @param transitionalState
   * @param finalState
   */
  mimicWorkStatusChange: function (entity, transitionalState, finalState) {
    if (Em.isArray(entity)) {
      entity.forEach(function (item) {
        item.set('workStatus', transitionalState);
        setTimeout(function () {
          item.set('workStatus', finalState);
        }, App.testModeDelayForActions);
      });
    } else {
      entity.set('workStatus', transitionalState);
      setTimeout(function () {
        entity.set('workStatus', finalState);
      }, App.testModeDelayForActions);
    }
  },

  /**
   * load data (if we need to show this background operations popup) from persist
   * @param callback
   */
  showBackgroundOperationsPopup: function (callback) {
    App.router.get('userSettingsController').dataLoading('show_bg').done(function (initValue) {
      if (initValue) {
        App.router.get('backgroundOperationsController').showPopup();
      }
      if (typeof callback === 'function') {
        callback();
      }
    });
  },

  /**
   * Send command to server to delete selected host component
   * @param {object} event
   * @method deleteComponent
   */
  deleteComponent: function (event) {
    if ($(event.target).closest('li').hasClass('disabled')) {
      return;
    }
    const component = event.context;
    const componentName = component.get('componentName');
    const componentsMapItem = this.get('addDeleteComponentsMap')[componentName];

    if (componentsMapItem) {
      this.deleteAndReconfigureComponent(componentsMapItem, component);
    } else if (componentName === 'JOURNALNODE') {
      return App.showConfirmationPopup(function () {
        App.router.transitionTo('main.services.manageJournalNode');
      }, Em.I18n.t('hosts.host.deleteComponent.popup.deleteJournalNodeMsg'));
    } else {
      return this.showReconfigurationPopupPreDelete(component, () => this._doDeleteHostComponent(componentName));
    }
  },

  /**
   *
   * @param {object} componentsMapItem
   * @param {App.HostComponent} component
   * @returns {App.ModalPopup}
   */
  deleteAndReconfigureComponent: function(componentsMapItem, component) {
    if (componentsMapItem.deletePropertyName) {
      this.set(componentsMapItem.deletePropertyName, true);
    }
    this.loadComponentRelatedConfigs(componentsMapItem.configTagsCallbackName, componentsMapItem.configsCallbackName);
    return this.showReconfigurationPopupPreDelete(component, () => {
      this._doDeleteHostComponent(component.get('componentName')).done(() => {
        this.applyConfigsCustomization();
        this.putConfigsToServer(this.get('groupedPropertiesToChange'), component.get('componentName'));
        this.clearConfigsChanges();
      });
    });
  },

  showReconfigurationPopupPreDelete: function (component, primary = Em.K, commonMessage) {
    const isLastComponent = (this.getTotalComponent(component) === 1),
      componentDisplayName = component.get('displayName');

    return App.ModalPopup.show({
      header: Em.I18n.t('popup.confirmation.commonHeader'),
      controller: this,
      hasPropertiesToChange: false,
      classNameBindings: ['controller.hasPropertiesToChange:common-modal-wrapper'],
      modalDialogClasses: function () {
        return this.get('controller.hasPropertiesToChange') ? ['modal-xlg'] : [];
      }.property('controller.hasPropertiesToChange'),
      primary: Em.I18n.t('hosts.host.deleteComponent.popup.confirm'),
      bodyClass: this.get('addDeleteComponentPopupBody').extend({
        commonMessage: commonMessage || Em.I18n.t('hosts.host.deleteComponent.popup.msg1').format(componentDisplayName),
        recommendedPropertiesToChange: this.get('recommendedPropertiesToChange'),
        requiredPropertiesToChange: this.get('requiredPropertiesToChange'),
        lastComponentError: Em.I18n.t('hosts.host.deleteComponent.popup.warning').format(componentDisplayName),
        lastComponent: isLastComponent
      }),
      isChecked: !isLastComponent,
      disablePrimary: function () {
        return (this.get('controller.isReconfigureRequired') && this.get('controller.isConfigsLoadingInProgress')) || !this.get('isChecked');
      }.property('controller.isReconfigureRequired', 'controller.isConfigsLoadingInProgress', 'isChecked'),
      onPrimary: function () {
        this._super();
        primary();
      },
      onSecondary: function () {
        this._super();
        this.get('controller').clearConfigsChanges();
      },
      onClose: function () {
        this._super();
        this.get('controller').clearConfigsChanges();
      }
    });
  },

  /**
   * get total count of host-components
   * @method getTotalComponent
   * @param component
   * @return {Number}
   */
  getTotalComponent: function (component) {
    var count;
    if (component.get('isSlave')) {
      count = App.SlaveComponent.find(component.get('componentName')).get('totalCount');
    } else if (component.get('isClient')) {
      count = App.ClientComponent.find(component.get('componentName')).get('totalCount');
    } else {
      count = App.HostComponent.find().filterProperty('componentName', component.get('componentName')).get('length');
    }
    return count || 0;
  },

  /**
   * Trigger to reset list of master/slaves components on the view
   * @type {bool}
   */
  redrawComponents: false,

  /**
   * Deletes the given host component, or all host components.
   *
   * @param {string} componentName
   * @return  {$.ajax}
   * @method _doDeleteHostComponent
   */
  _doDeleteHostComponent: function (componentName) {
    return App.ajax.send({
      name: (Em.isNone(componentName)) ? 'common.delete.host' : 'common.delete.host_component',
      sender: this,
      data: {
        componentName: componentName || '',
        hostName: this.get('content.hostName')
      },
      success: '_doDeleteHostComponentSuccessCallback',
      error: '_doDeleteHostComponentErrorCallback',
      showLoadingPopup: true
    });
  },

  /**
   * Error of delete component(s) request
   * @type {object}
   */
  _deletedHostComponentError: null,

  /**
   * Success callback for delete host component request
   * @method _doDeleteHostComponentSuccessCallback
   */
  _doDeleteHostComponentSuccessCallback: function (response, request, data) {
    this.set('_deletedHostComponentError', null);
  },

  /**
   * Error-callback for delete host component request
   * @param {object} xhr
   * @param {string} textStatus
   * @param {object} errorThrown
   * @param {object} data
   * @method _doDeleteHostComponentErrorCallback
   */
  _doDeleteHostComponentErrorCallback: function (xhr, textStatus, errorThrown, data) {
    this.set('_deletedHostComponentError', {xhr: xhr, url: data.url, method: 'DELETE'});
  },

  /**
   * Send command to server to upgrade selected host component
   * @param {object} event
   * @method upgradeComponent
   */
  upgradeComponent: function (event) {
    var self = this;
    var component = event.context;
    return App.showConfirmationPopup(function () {
      App.ajax.send({
        name: 'host.host_component.upgrade',
        sender: self,
        data: {
          component: component,
          hostName: self.get('content.hostName'),
          componentName: component.get('componentName'),
          data: JSON.stringify({
            RequestInfo: {
              "context": Em.I18n.t('requestInfo.upgradeHostComponent') + " " + component.get('displayName')
            },
            Body: {
              HostRoles: {
                stack_id: 'HDP-1.2.2',
                state: 'INSTALLED'
              }
            }
          })
        },
        success: 'upgradeComponentSuccessCallback',
        error: 'ajaxErrorCallback'
      });
    }, Em.I18n.t('question.sure.upgrade').format(component.get('displayName')));
  },

  /**
   * Success callback for upgrade host component request
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method upgradeComponentSuccessCallback
   */
  upgradeComponentSuccessCallback: function (data, opt, params) {
    if (App.get('testMode')) {
      this.mimicWorkStatusChange(params.component, App.HostComponentStatus.starting, App.HostComponentStatus.started);
    }
    this.showBackgroundOperationsPopup();
  },

  /**
   * Send command to server to restart selected components
   * @param {object} event
   * @method restartComponent
   */
  restartComponent: function (event) {
    var component = event.context;
    if (event.context.get('componentName') === 'NAMENODE') {
      this.checkNnLastCheckpointTime(function () {
        return App.showConfirmationPopup(function () {
          batchUtils.restartHostComponents([component], Em.I18n.t('rollingrestart.context.selectedComponentOnSelectedHost').format(component.get('displayName')), "HOST_COMPONENT");
        }, Em.I18n.t('question.sure.restart').format(component.get('displayName')));
      });
    } else {
      return App.showConfirmationPopup(function () {
        batchUtils.restartHostComponents([component], Em.I18n.t('rollingrestart.context.selectedComponentOnSelectedHost').format(component.get('displayName')), "HOST_COMPONENT");
      }, Em.I18n.t('question.sure.restart').format(component.get('displayName')));
    }
  },

  /**
   * add component as <code>addComponent<code> method but perform
   * kdc session state if cluster is secure;
   * @param event
   */
  addComponentWithCheck: function (event) {
    App.get('router.mainAdminKerberosController').getSecurityType(function (event) {
      App.get('router.mainAdminKerberosController').getKDCSessionState(this.addComponent.bind(this, event));
    }.bind(this, event));
  },
  /**
   * Send command to server to install selected host component
   * @param {object} event
   * @method addComponent
   */
  addComponent: function (event) {
    var
      component = event.context,
      hostName = this.get('content.hostName'),
      componentName = component.get('componentName'),
      missedComponents = event.fromServiceSummary ? [] : this.checkComponentDependencies(componentName, {
        scope: 'host',
        installedComponents: this.get('content.hostComponents').mapProperty('componentName')
      }),
      isManualKerberos = App.get('router.mainAdminKerberosController.isManualKerberos'),
      manualKerberosWarning = isManualKerberos ? Em.I18n.t('hosts.host.manualKerberosWarning') : '',
      componentsMapItem = this.get('addDeleteComponentsMap')[componentName];

    if (missedComponents.length) {
      const popupMessage = Em.I18n.t('host.host.addComponent.popup.dependedComponents.body').format(component.get('displayName'),
        stringUtils.getFormattedStringFromArray(missedComponents.map(function (cName) {
          return App.StackServiceComponent.find(cName).get('displayName');
        })));
      return App.showAlertPopup(Em.I18n.t('host.host.addComponent.popup.dependedComponents.header'), popupMessage);
    }

    if (componentsMapItem) {
      this.addAndReconfigureComponent(componentsMapItem, hostName, component, event.fromServiceSummary);
    } else if (componentName === 'JOURNALNODE') {
      return App.showConfirmationPopup(function () {
        App.router.transitionTo('main.services.manageJournalNode');
      }, Em.I18n.t('hosts.host.addComponent.' + componentName) + manualKerberosWarning);
    } else {
      return this.showAddComponentPopup(component, hostName, () => {
        // hostname should be taken via this.get('content.hostName') because the host could be selected later
        this.installHostComponentCall(this.get('content.hostName'), component);
      }, event.fromServiceSummary);
    }
  },

  /**
   *
   * @param {object} componentsMapItem
   * @param {string} hostName
   * @param {App.HostComponent} component
   * @param {boolean} fromServiceSummary
   * @returns {*}
   */
  addAndReconfigureComponent: function(componentsMapItem, hostName, component, fromServiceSummary) {
    if (componentsMapItem.hostPropertyName) {
      this.set(componentsMapItem.hostPropertyName, hostName);
    }
    if (componentsMapItem.addPropertyName) {
      this.set(componentsMapItem.addPropertyName, true);
    }
    this.loadComponentRelatedConfigs(componentsMapItem.configTagsCallbackName, componentsMapItem.configsCallbackName);
    return this.showAddComponentPopup(component, hostName, () => {
      this.installAndReconfigureComponent(this.get('content.hostName'), component, componentsMapItem);
    }, fromServiceSummary);
  },

  /**
   *
   * @param {string} hostName
   * @param {App.HostComponent} component
   * @param {object} componentsMapItem
   */
  installAndReconfigureComponent: function(hostName, component, componentsMapItem) {
    this.applyConfigsCustomization();
    this.installHostComponentCall(hostName, component).done(() => {
      this.putConfigsToServer(this.get('groupedPropertiesToChange'), component.get('componentName'));
    }).always(() => {
      this.set(componentsMapItem.addPropertyName, false);
      this.clearConfigsChanges();
    });
  },

  showAddComponentPopup: function (component, hostName, primary, fromServiceSummary) {
    var self = this,
      componentName = component.get('componentName'),
      componentDisplayName = component.get('displayName'),
      componentsMapItem = this.get('addDeleteComponentsMap')[componentName],
      commonMessage = Em.I18n.t('hosts.host.addComponent.msg').format(componentDisplayName);

    return App.ModalPopup.show({
      header: Em.I18n.t('popup.confirmation.commonHeader'),
      controller: self,
      hasPropertiesToChange: false,
      classNameBindings: ['hasPropertiesToChange:common-modal-wrapper'],
      modalDialogClasses: function () {
        return this.get('controller.hasPropertiesToChange') ? ['modal-xlg'] : [];
      }.property('controller.hasPropertiesToChange'),
      primary: Em.I18n.t('hosts.host.addComponent.popup.confirm'),
      bodyClass: self.get('addDeleteComponentPopupBody').extend({
        commonMessage: commonMessage,
        fromServiceSummary: fromServiceSummary,
        addComponentMsg: Em.I18n.t('hosts.host.addComponent.msg').format(componentDisplayName),
        selectHostMsg: Em.I18n.t('services.summary.selectHostForComponent').format(componentDisplayName),
        thereIsNoHostsMsg: Em.I18n.t('services.summary.allHostsAlreadyRunComponent').format(componentDisplayName),
        hostsWithoutComponent: function () {
          if (this.get('fromServiceSummary')) {
            const hostsWithComponent = App.HostComponent.find().filterProperty('componentName', componentName).mapProperty('hostName');
            return App.get('allHostNames').filter((hostname) => !hostsWithComponent.contains(hostname));
          } else {
            return [];
          }
        }.property('fromServiceSummary'),
        anyHostsWithoutComponent: Em.computed.or('!fromServiceSummary', 'hostsWithoutComponent.length'),
        selectedHostObserver: function () {
          const selectedHostName = this.get('selectedHost');
          if (!self.get('content')) {
            self.set('content', {});
          }
          self.setProperties({
            'content.hostName': selectedHostName
          });
          if (componentsMapItem) {
            const configs = self.get('configs');
            const params = configs && configs.params || {};
            if (componentsMapItem.hostPropertyName) {
              self.set(componentsMapItem.hostPropertyName, selectedHostName);
            }
            if (componentsMapItem.addPropertyName) {
              self.set(componentsMapItem.addPropertyName, true);
            }
            if (configs) {
              self.clearConfigsChanges(true);
              self.set('isReconfigureRequired', true);
              self.set('isConfigsLoadingInProgress', true);
              self[componentsMapItem.configsCallbackName](configs, null, params);
            } else {
              self.loadComponentRelatedConfigs(componentsMapItem.configTagsCallbackName, componentsMapItem.configsCallbackName);
            }
          }
        }.observes('selectedHost')
      }),
      disablePrimary: Em.computed.and('controller.isReconfigureRequired', 'controller.isConfigsLoadingInProgress'),
      onPrimary: function () {
        this._super();
        if (primary) {
          primary();
        }
      },
      onSecondary: function () {
        this._super();
        self.clearConfigsChanges();
      },
      onClose: function () {
        this._super();
        self.clearConfigsChanges();
      }
    });
  },

  loadComponentRelatedConfigs: function (configTagsCallbackName, configsCallbackName) {
    this.clearConfigsChanges();
    this.set('isReconfigureRequired', true);
    this.set('isConfigsLoadingInProgress', true);
    this.isServiceMetricsLoaded(() => {
      this.loadConfigs(configTagsCallbackName, configsCallbackName);
    });
  },

  /**
   * Success callback for install host component request (sent in <code>addNewComponentSuccessCallback</code>)
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method installNewComponentSuccessCallback
   */
  installNewComponentSuccessCallback: function (data, opt, params) {
    if (!data || !data.Requests || !data.Requests.id) {
      return false;
    }

    if (App.get('testMode')) {
      this.mimicWorkStatusChange(params.component, App.HostComponentStatus.installing, App.HostComponentStatus.stopped);
    }

    this.showBackgroundOperationsPopup();
    return true;
  },

  /**
   * Call <code>setRackInfo</code> function to show Set Rack Id popup
   * @param data
   */
  setRackId: function (data) {
    var rack = data.context.get('rack');
    var hosts = [data.context];
    var operationData = {message: Em.I18n.t('hosts.host.details.setRackId')};
    hostsManagement.setRackInfo(operationData, hosts, rack);
  },

  /**
   * Success callback for load configs request
   * @param {object} data
   * @method loadOozieConfigs
   */
  loadOozieConfigs: function (data) {
    return App.ajax.send({
      name: 'admin.get.all_configurations',
      sender: this,
      data: {
        urlParams: '(type=oozie-env&tag=' + data.Clusters.desired_configs['oozie-env'].tag + ')'
      },
      success: 'onLoadOozieConfigs',
      error: 'onLoadConfigsErrorCallback'
    });
  },

  /**
   * get Oozie database config and set databaseType
   * @param {object} data
   * @method onLoadOozieConfigs
   */
  onLoadOozieConfigs: function (data) {
    var configs = {};
    data.items.forEach(function (item) {
      $.extend(configs, item.properties);
    });
    this.set('isOozieServerAddable', !(Em.isEmpty(configs["oozie_database"]) || configs["oozie_database"] === 'New Derby Database'));
    this.get('isOozieConfigLoaded').resolve();
  },


  /**
   * Success callback for Storm load configs request
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method loadStormConfigs
   */
  loadStormConfigs: function (data, opt, params) {
    var request = App.ajax.send({
      name: 'admin.get.all_configurations',
      sender: this,
      data: {
        urlParams: '(type=storm-site&tag=' + data.Clusters.desired_configs['storm-site'].tag + ')'
      },
      success: params.callback
    });
    this.trackRequest(request);
  },

  /**
   * Success callback for Atlas load configs request
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method loadAtlasConfigs
   */
  loadAtlasConfigs: function (data, opt, params) {
    var request = App.ajax.send({
      name: 'admin.get.all_configurations',
      sender: this,
      data: {
        urlParams: '(type=application-properties&tag=' + data.Clusters.desired_configs['application-properties'].tag + ')'
      },
      success: params.callback
    });
    this.trackRequest(request);
  },

  /**
   * Update zk configs
   * @param {object} configs
   * @method updateZkConfigs
   */
  updateZkConfigs: function (configs) {
    const portValue = configs['zoo.cfg'] && Em.get(configs['zoo.cfg'], 'clientPort'),
      zkPort = typeof portValue === 'undefined' ? '2181' : portValue,
      infraSolrZnode = configs['infra-solr-env'] ? Em.get(configs['infra-solr-env'], 'infra_solr_znode') : '/ambari-solr',
      initializer = App.AddZooKeeperComponentsInitializer,
      hostComponentsTopology = {
        masterComponentHosts: []
      },
      propertiesToChange = this.get('allPropertiesToChange'),
      masterComponents = this.bootstrapHostsMapping('ZOOKEEPER_SERVER');
    if (this.get('fromDeleteHost') || this.get('fromDeleteZkServer')) {
      this.set('fromDeleteHost', false);
      this.set('fromDeleteZkServer', false);
      let removedHost = masterComponents.findProperty('hostName', this.get('content.hostName'));
      if (!Em.isNone(removedHost)) {
        Em.set(removedHost, 'isInstalled', false);
      }
    } else if (this.get('addZooKeeperServer')) {
      this.set('addZooKeeperServer', false);
      masterComponents.push({
        component: 'ZOOKEEPER_SERVER',
        hostName: this.get('content.hostName'),
        isInstalled: true
      });
    }
    const dependencies = {
      zkClientPort: zkPort,
      infraSolrZnode
    };
    hostComponentsTopology.masterComponentHosts = masterComponents;
    Em.keys(configs).forEach(fileName => {
      const properties = configs[fileName];
      Em.keys(properties).forEach(propertyName => {
        const currentValue = properties[propertyName],
          propertyDef = {
            fileName,
            name: propertyName,
            value: currentValue
          },
          configProperty = initializer.initialValue(propertyDef, hostComponentsTopology, dependencies);
        initializer.updateSiteObj(configs[fileName], configProperty);
        if (currentValue !== configs[fileName][propertyName]) {
          const service = App.config.get('serviceByConfigTypeMap')[fileName],
            configObject = App.configsCollection.getConfigByName(propertyName, fileName),
            displayName = configObject && configObject.displayName;
          propertiesToChange.pushObject({
            propertyFileName: fileName,
            propertyName,
            propertyTitle: configObject && Em.I18n.t('installer.controls.serviceConfigPopover.title').format(displayName, displayName === propertyName ? '' : propertyName),
            propertyDescription: configObject && configObject.description,
            serviceDisplayName: service && service.get('displayName'),
            initialValue: currentValue,
            recommendedValue: propertyDef.value
          });
        }
      });
    });
  },

  /**
   *
   * @param {string} componentName
   * @param {string[]} [hostNames]
   * @returns {}
   */
  bootstrapHostsMapping: function(componentName, hostNames) {
    if (Em.isNone(hostNames)) {
      hostNames = App.HostComponent.find().filterProperty('componentName', componentName).mapProperty('hostName');
    }
    return hostNames.map(function(hostName) {
      return {
        component: componentName,
        hostName: hostName,
        isInstalled: true
      };
    });
  },

  /**
   * update and save Storm related configs to server
   * @param {object} data
   * @method onLoadStormConfigs
   */
  onLoadStormConfigs: function (data) {
    const nimbusHost = this.get('nimbusHost'),
      stormNimbusHosts = this.getStormNimbusHosts(),
      configs = {},
      attributes = {},
      propertiesToChange = this.get('allPropertiesToChange');

    this.saveLoadedConfigs(data);
    data.items.forEach(item => {
      configs[item.type] = item.properties;
      attributes[item.type] = item.properties_attributes || {};
    });

    this.updateZkConfigs(configs);

    const propertyName = 'nimbus.seeds',
      propertyFileName = 'storm-site',
      nimbusSeedsInit = configs[propertyFileName][propertyName],
      nimbusSeedsRecommended = JSON.stringify(stormNimbusHosts).replace(/"/g, "'");
    configs[propertyFileName][propertyName] = nimbusSeedsRecommended;
    if (this.get('isReconfigureRequired') && nimbusSeedsInit !== nimbusSeedsRecommended) {
      const service = App.config.get('serviceByConfigTypeMap')[propertyFileName],
        configObject = App.configsCollection.getConfigByName(propertyName, propertyFileName),
        displayName = configObject && configObject.displayName;
      propertiesToChange.pushObject({
        propertyFileName,
        propertyName,
        propertyTitle: configObject && Em.I18n.t('installer.controls.serviceConfigPopover.title').format(displayName, displayName === propertyName ? '' : propertyName),
        propertyDescription: configObject && configObject.description,
        serviceDisplayName: service && service.get('displayName'),
        initialValue: nimbusSeedsInit,
        recommendedValue: nimbusSeedsRecommended
      });
    }
    const groups = [
      {
        properties: {
          [propertyFileName]: configs[propertyFileName]
        },
        properties_attributes: {
          [propertyFileName]: attributes[propertyFileName]
        }
      }
    ];
    this.setConfigsChanges(groups);
  },

  onLoadAtlasConfigs: function(data) {
    const atlasServer = this.get('atlasServer'),
      atlasServerHosts = this.getAtlasServerHosts(),
      configs = {},
      attributes = {},
      propertiesToChange = this.get('allPropertiesToChange');

    this.saveLoadedConfigs(data);
    data.items.forEach(item => {
      configs[item.type] = item.properties;
      attributes[item.type] = item.properties_attributes || {};
    });

    const propertyFileName = 'application-properties',
      propertyName = 'atlas.rest.address',
      atlasAddresses = configs[propertyFileName][propertyName],
      hostMask = atlasAddresses.split(',')[0].replace(/([https|http]*\:\/\/)(.*?)(:[0-9]+)/, '$1{hostname}$3'),
      atlasAddressesRecommended = atlasServerHosts.map(hostName => hostMask.replace('{hostname}', hostName)).join(',');
    configs[propertyFileName][propertyName] = atlasAddressesRecommended;
    if (this.get('isReconfigureRequired') && atlasAddresses !== atlasAddressesRecommended) {
      var service = App.config.get('serviceByConfigTypeMap')[propertyFileName],
        configObject = App.configsCollection.getConfigByName(propertyName, propertyFileName),
        displayName = configObject && configObject.displayName;
      propertiesToChange.pushObject({
        propertyFileName,
        propertyName,
        propertyTitle: configObject && Em.I18n.t('installer.controls.serviceConfigPopover.title').format(displayName, displayName === propertyName ? '' : propertyName),
        propertyDescription: configObject && configObject.description,
        serviceDisplayName: service && service.get('displayName'),
        initialValue: atlasAddresses,
        recommendedValue: atlasAddressesRecommended
      });
    }
    const groups = [
      {
        properties: {
          [propertyFileName]: configs[propertyFileName]
        },
        properties_attributes: {
          [propertyFileName]: attributes[propertyFileName]
        }
      }
    ];
    this.setConfigsChanges(groups);
  },

  /**
   * Success callback for load configs request
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method loadWebHCatConfigs
   */
  loadWebHCatConfigs: function (data, opt, params) {
    const urlParams = this.getUrlParamsForConfigsRequest(data, ['hive-site', 'webhcat-site', 'hive-env', 'core-site']),
      request = App.ajax.send({
        name: 'admin.get.all_configurations',
        sender: this,
        data: {
          webHCat: true,
          urlParams
        },
        success: params.callback
      });
    this.trackRequest(request);
    return request;
  },

  /**
   * Success callback for load configs request
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method loadHiveConfigs
   */
  loadHiveConfigs: function (data, opt, params) {
    const urlParams = this.getUrlParamsForConfigsRequest(data, ['hive-site', 'webhcat-site', 'hive-env', 'core-site']),
      request = App.ajax.send({
        name: 'admin.get.all_configurations',
        sender: this,
        data: {
          urlParams
        },
        success: params.callback
      });
    this.trackRequest(request);
    return request;
  },

  /**
   * update and save Hive related configs to server
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method onLoadHiveConfigs
   */
  onLoadHiveConfigs: function (data, opt, params) {
    let port = "";
    const configs = {},
      attributes = {},
      userSetup = {},
      localDB = {
        masterComponentHosts: this.getHiveHosts()
      },
      dependencies = {
        hiveMetastorePort: ""
      },
      initializer = params.webHCat ? App.AddWebHCatComponentsInitializer : App.AddHiveComponentsInitializer;
    this.saveLoadedConfigs(data);
    this.set('configs.params', {
      webHCat: params.webHCat
    });
    data.items.forEach(item => {
      configs[item.type] = item.properties;
      attributes[item.type] = item.properties_attributes || {};
    });
    const propertiesToChange = this.get('allPropertiesToChange');

    port = configs['hive-site']['hive.metastore.uris'].match(/:[0-9]{2,4}/);
    port = port ? port[0].slice(1) : "9083";

    dependencies.hiveMetastorePort = port;

    if (params.webHCat) {
      userSetup.webhcatUser = configs['hive-env']['webhcat_user'];
    } else {
      userSetup.hiveUser = configs['hive-env']['hive_user'];
    }

    initializer.setup(userSetup);

    ['hive-site', 'webhcat-site', 'hive-env', 'core-site'].forEach(fileName => {
      if (configs[fileName]) {
        Em.keys(configs[fileName]).forEach(propertyName => {
          const currentValue = configs[fileName][propertyName],
            propertyDef = {
              fileName,
              name: propertyName,
              value: currentValue
            },
            configProperty = initializer.initialValue(propertyDef, localDB, dependencies);
          initializer.updateSiteObj(configs[fileName], configProperty);
          if (this.get('isReconfigureRequired') && currentValue !== configs[fileName][propertyName]) {
            const service = App.config.get('serviceByConfigTypeMap')[fileName],
              configObject = App.configsCollection.getConfigByName(propertyName, fileName),
              displayName = configObject && configObject.displayName;
            propertiesToChange.pushObject({
              propertyFileName: fileName,
              propertyName,
              propertyTitle: configObject && Em.I18n.t('installer.controls.serviceConfigPopover.title').format(displayName, displayName === propertyName ? '' : propertyName),
              propertyDescription: configObject && configObject.description,
              serviceDisplayName: service && service.get('displayName'),
              initialValue: currentValue,
              recommendedValue: propertyDef.value
            });
          }
        });
      }
    });

    initializer.cleanup();

    var groups = [
      {
        properties: {
          'hive-site': configs['hive-site'],
          'webhcat-site': configs['webhcat-site'],
          'hive-env': configs['hive-env']
        },
        properties_attributes: {
          'hive-site': attributes['hive-site'],
          'webhcat-site': attributes['webhcat-site'],
          'hive-env': attributes['hive-env']
        }
      },
      {
        properties: {
          'core-site': configs['core-site']
        },
        properties_attributes: {
          'core-site': attributes['core-site']
        }
      }
    ];
    this.setConfigsChanges(groups);
  },

  /**
   * save configs' sites in batch
   * @param groups
   * @param componentName
   */
  putConfigsToServer: function (groups, componentName) {
    const dfd = $.Deferred();
    const requests = [];
    if (groups.length) {
      groups.forEach(function (group) {
        const desiredConfigs = [];
        const properties = group.properties;

        for (let site in properties) {
          if (!properties.hasOwnProperty(site) || Em.isNone(properties[site])) continue;
          desiredConfigs.push({
            "type": site,
            "properties": properties[site],
            "properties_attributes": group.properties_attributes[site],
            "service_config_version_note": Em.I18n.t('hosts.host.configs.save.note').format(App.format.role(componentName, false))
          });
        }
        if (desiredConfigs.length > 0) {
          requests.push(App.ajax.send({
            name: 'common.service.configurations',
            sender: this,
            data: {
              desired_config: desiredConfigs,
              componentName: componentName
            }
          }));
        }
      }, this);
      $.when(requests).done(dfd.resolve).fail(dfd.reject).then(() => {
        // If user adding component which require config changes from Configs page then configs should be reloaded
        if (App.router.get('location.location.hash').contains('configs')) {
          App.router.get('mainServiceInfoConfigsController').loadStep();
        }
      });
    } else {
      dfd.resolve();
    }
    return dfd.promise();
  },

  /**
   * Delete Hive Metastore is performed
   * @type {bool}
   */
  deleteHiveMetaStore: false,

  /**
   * Delete WebHCat Server is performed
   *
   * @type {bool}
   */
  deleteWebHCatServer: false,

  getHiveHosts: function () {
    var self = this;
    var removePerformed = this.get('fromDeleteHost') || this.get('deleteHiveMetaStore') || this.get('deleteHiveServer') || this.get('deleteWebHCatServer');
    var hiveMasterComponents = ['WEBHCAT_SERVER', 'HIVE_METASTORE', 'HIVE_SERVER'];
    var masterComponentsMap = hiveMasterComponents.map(function(componentName) {
      return self.bootstrapHostsMapping(componentName);
    }).reduce(function(p,c) {
      return p.concat(c);
    });

    if (removePerformed) {
      self.setProperties({
        deleteHiveMetaStore: false,
        deleteHiveServer: false,
        deleteWebHCatServer: false,
        fromDeleteHost: false
      });
      masterComponentsMap = masterComponentsMap.map(function(masterComponent) {
        masterComponent.isInstalled = masterComponent.hostName !== self.get('content.hostName');
        return masterComponent;
      });
    }

    if (!!this.get('hiveMetastoreHost')) {
      masterComponentsMap.push({
        component: 'HIVE_METASTORE',
        hostName: this.get('hiveMetastoreHost'),
        isInstalled: !removePerformed
      });
      this.set('hiveMetastoreHost', '');
    }

    if (!!this.get('webhcatServerHost')) {
      masterComponentsMap.push({
        component: 'WEBHCAT_SERVER',
        hostName: this.get('webhcatServerHost'),
        isInstalled: !removePerformed
      });
      this.set('webhcatServerHost', '');
    }

    return masterComponentsMap;
  },

  /**
   * Success callback for load configs request
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method loadRangerConfigs
   */
  loadRangerConfigs: function (data, opt, params) {
    const urlParams = this.getUrlParamsForConfigsRequest(data, ['core-site', 'hdfs-site', 'kms-env', 'kms-site']),
      request = App.ajax.send({
        name: 'admin.get.all_configurations',
        sender: this,
        data: {
          urlParams
        },
        success: params.callback
      });
    this.trackRequest(request);
  },

  /**
   * update and save Hive hive.metastore.uris config to server
   * @param {object} data
   * @method onLoadRangerConfigs
   */
  onLoadRangerConfigs: function (data) {
    const hdfsProperties = [
        {
          type: 'core-site',
          name: 'hadoop.security.key.provider.path'
        },
        {
          type: 'hdfs-site',
          name: 'dfs.encryption.key.provider.uri'
        }
      ],
      kmsSiteProperties = [
        {
          name: 'hadoop.kms.cache.enable',
          notHaValue: 'true',
          haValue: 'false'
        },
        {
          name: 'hadoop.kms.cache.timeout.ms',
          notHaValue: '600000',
          haValue: '0'
        },
        {
          name: 'hadoop.kms.current.key.cache.timeout.ms',
          notHaValue: '30000',
          haValue: '0'
        },
        {
          name: 'hadoop.kms.authentication.signer.secret.provider',
          notHaValue: 'random',
          haValue: 'zookeeper'
        },
        {
          name: 'hadoop.kms.authentication.signer.secret.provider.zookeeper.auth.type',
          notHaValue: 'kerberos',
          haValue: 'none'
        },
        {
          name: 'hadoop.kms.authentication.signer.secret.provider.zookeeper.connection.string',
          notHaValue: '#HOSTNAME#:#PORT#,...',
          haValue: this.getZookeeperConnectionString()
        }
      ],
      rkmsHosts = this.getRangerKMSServerHosts(),
      rkmsHostsStr = rkmsHosts.join(';'),
      isHA = rkmsHosts.length > 1,
      rkmsPort = data.items.findProperty('type', 'kms-env').properties['kms_port'],
      newValue = 'kms://http@' + rkmsHostsStr + ':' + rkmsPort + '/kms',
      coreSiteConfigs = data.items.findProperty('type', 'core-site'),
      hdfsSiteConfigs = data.items.findProperty('type', 'hdfs-site'),
      kmsSiteConfigs = data.items.findProperty('type', 'kms-site'),
      groups = [
        {
          properties: {
            'core-site': coreSiteConfigs.properties,
            'hdfs-site': hdfsSiteConfigs.properties
          },
          properties_attributes: {
            'core-site': coreSiteConfigs.properties_attributes,
            'hdfs-site': hdfsSiteConfigs.properties_attributes
          }
        },
        {
          properties: {
            'kms-site': kmsSiteConfigs.properties
          },
          properties_attributes: {
            'kms-site': kmsSiteConfigs.properties_attributes
          }
        }
      ],
      propertiesToChange = this.get('allPropertiesToChange');

    this.saveLoadedConfigs(data);

    hdfsProperties.forEach(property => {
      const typeConfigs = data.items.findProperty('type', property.type).properties,
        currentValue = typeConfigs[property.name],
        pattern = new RegExp('^kms:\\/\\/http@(.+):' + rkmsPort + '\\/kms$'),
        patternMatch = currentValue && currentValue.match(pattern),
        currentHostsList = patternMatch && patternMatch[1].split(';').sort().join(';');
      if (currentHostsList !== rkmsHostsStr) {
        typeConfigs[property.name] = newValue;
        if (this.get('isReconfigureRequired')) {
          const propertyFileName = property.type,
            propertyName = property.name,
            service = App.config.get('serviceByConfigTypeMap')[propertyFileName],
            configObject = App.configsCollection.getConfigByName(propertyName, propertyFileName),
            displayName = configObject && configObject.displayName;
          propertiesToChange.pushObject({
            propertyFileName,
            propertyName,
            propertyTitle: configObject && Em.I18n.t('installer.controls.serviceConfigPopover.title').format(displayName, displayName === propertyName ? '' : propertyName),
            propertyDescription: configObject && configObject.description,
            serviceDisplayName: service && service.get('displayName'),
            initialValue: currentValue,
            recommendedValue: newValue,
            saveRecommended: true
          });
        }
      }
    });

    kmsSiteProperties.forEach(function (property) {
      var currentValue = kmsSiteConfigs.properties[property.name];
      var newValue = isHA ? property.haValue : property.notHaValue;
      kmsSiteConfigs.properties[property.name] = newValue;

      propertiesToChange.pushObject({
        propertyFileName: 'kms-site',
        propertyName: property.name,
        serviceDisplayName: App.Service.find().findProperty('serviceName', 'RANGER_KMS').get('displayName'),
        initialValue: currentValue,
        recommendedValue: newValue,
        saveRecommended: true
      });
    });
    this.setConfigsChanges(groups);
  },

  /**
   * Delete Hive Metastore is performed
   * @type {bool}
   */
  deleteRangerKMSServer: false,

  getRangerKMSServerHosts: function () {
    var rkmsHosts = App.HostComponent.find().filterProperty('componentName', 'RANGER_KMS_SERVER').mapProperty('hostName');
    var rangerKMSServerHost = this.get('rangerKMSServerHost');

    if (!!rangerKMSServerHost) {
      rkmsHosts.push(rangerKMSServerHost);
    }

    if (this.get('fromDeleteHost') || this.get('deleteRangerKMSServer')) {
      return rkmsHosts.without(this.get('content.hostName'));
    }
    return rkmsHosts.sort();
  },

  getZookeeperConnectionString: function () {
    var zookeeperHosts = App.MasterComponent.find('ZOOKEEPER_SERVER').get('hostNames');
    return zookeeperHosts.map(function (host) {
      return host + ':2181';
    }).join(',');
  },

  /**
   * Delete Storm Nimbus is performed
   * @type {bool}
   */
  deleteNimbusHost: false,

  getStormNimbusHosts: function () {
    var
      stormNimbusHosts = App.HostComponent.find().filterProperty('componentName', 'NIMBUS').mapProperty('hostName'),
      nimbusHost = this.get('nimbusHost');

    if (!!nimbusHost) {
      stormNimbusHosts.push(nimbusHost);
      this.set('nimbusHost', '');
    }

    if (this.get('fromDeleteHost') || this.get('deleteNimbusHost')) {
      this.set('deleteNimbusHost', false);
      this.set('fromDeleteHost', false);
      return stormNimbusHosts.without(this.get('content.hostName'));
    }
    return stormNimbusHosts.sort();
  },

  getAtlasServerHosts: function () {
    var
      atlasServerHosts = App.HostComponent.find().filterProperty('componentName', 'ATLAS_SERVER').mapProperty('hostName'),
      atlasServer = this.get('atlasServer');

    if (!!atlasServer) {
      atlasServerHosts.push(atlasServer);
      this.set('atlasServer', '');
    }

    if (this.get('fromDeleteHost') || this.get('deleteAtlasServer')) {
      this.set('deleteAtlasServer', false);
      this.set('fromDeleteHost', false);
      return atlasServerHosts.without(this.get('content.hostName'));
    }
    return atlasServerHosts.sort();
  },

  /**
   * Send command to server to resfresh configs of selected component
   * @param {object} event
   * @method refreshComponentConfigs
   */
  refreshComponentConfigs: function (event) {
    var self = this;
    return App.showConfirmationPopup(function () {
      var component = event.context;
      var context = Em.I18n.t('requestInfo.refreshComponentConfigs').format(component.get('displayName'));
      self.sendRefreshComponentConfigsCommand(component, context);
    });
  },

  /**
   * PUTs a command to server to refresh configs of host component.
   * @param {object} component
   * @param {object} context Context under which this command is beign sent.
   * @method sendRefreshComponentConfigsCommand
   */
  sendRefreshComponentConfigsCommand: function (component, context) {
    var resource_filters = [
      {
        service_name: component.get('service.serviceName'),
        component_name: component.get('componentName'),
        hosts: component.get('host.hostName')
      }
    ];
    App.ajax.send({
      name: 'host.host_component.refresh_configs',
      sender: this,
      data: {
        resource_filters: resource_filters,
        context: context
      },
      success: 'refreshComponentConfigsSuccessCallback',
      showLoadingPopup: true
    });
  },

  /**
   * Success callback for refresh host component configs request
   * @method refreshComponentConfigsSuccessCallback
   */
  refreshComponentConfigsSuccessCallback: function () {
    this.showBackgroundOperationsPopup();
  },

  /**
   * Load configs
   * This function when used without a callback should be always used from successcallback function of the promise `App.router.get('mainController').isLoading.call(App.router.get('clusterController'), 'isServiceContentFullyLoaded').done(promise)`
   * This is required to make sure that service metrics API determining the HA state of components is loaded
   * @method loadConfigs
   */
  loadConfigs: function (configTagsCallback, configsCallback) {
    var request = App.ajax.send({
      name: 'config.tags',
      sender: this,
      data: {
        callback: configsCallback
      },
      success: configTagsCallback,
      error: 'onLoadConfigsErrorCallback'
    });
    this.trackRequest(request);
  },

  /**
   * onLoadConfigsErrorCallback
   * @method onLoadConfigsErrorCallback
   */
  onLoadConfigsErrorCallback: function () {
    this.get('isOozieConfigLoaded').reject();
  },

  /**
   * Success callback for load configs request
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method loadZookeeperConfigs
   */
  loadZookeeperConfigs: function (data, opt, params) {
    var urlParams = this.constructZookeeperConfigUrlParams(data);
    if (urlParams.length > 0) {
      var request = App.ajax.send({
        name: 'reassign.load_configs',
        sender: this,
        data: {
          urlParams: urlParams.join('|')
        },
        success: params.callback
      });
      this.trackRequest(request);
      return true;
    }
    this.set('isConfigsLoadingInProgress', false);
    return false;
  },

  /**
   * construct URL params for query, that load configs
   * @param data {Object}
   * @return {Array}
   */
  constructZookeeperConfigUrlParams: function (data) {
    var urlParams = [];
    var services = App.Service.find();
    var zooKeeperRelatedServices = this.get('zooKeeperRelatedServices').slice(0);
    if (App.get('isHaEnabled')) {
      zooKeeperRelatedServices.push({
        serviceName: 'HDFS',
        typesToLoad: ['core-site'],
        typesToSave: ['core-site']
      });
    }
    zooKeeperRelatedServices.forEach(function (service) {
      if (services.someProperty('serviceName', service.serviceName)) {
        service.typesToLoad.forEach(function (type) {
          if (data.Clusters.desired_configs[type]) {
            urlParams.push('(type=' + type + '&tag=' + data.Clusters.desired_configs[type].tag + ')');
          }
        });
      }
    });
    return urlParams;
  },

  /**
   * save new ZooKeeper configs to server
   * @param {object} data
   * @method saveZkConfigs
   */
  saveZkConfigs: function (data) {
    var configs = {};
    var attributes = {};
    this.saveLoadedConfigs(data);
    data.items.forEach(function (item) {
      configs[item.type] = item.properties;
      attributes[item.type] = item.properties_attributes || {};
    }, this);

    this.updateZkConfigs(configs);
    var groups = [];
    var installedServiceNames = App.Service.find().mapProperty('serviceName');
    var zooKeeperRelatedServices = this.get('zooKeeperRelatedServices').slice(0);
    if (App.get('isHaEnabled')) {
      zooKeeperRelatedServices.push({
        serviceName: 'HDFS',
        typesToLoad: ['core-site'],
        typesToSave: ['core-site']
      });
    }
    zooKeeperRelatedServices.forEach(function (service) {
      if (installedServiceNames.contains(service.serviceName)) {
        var group = {
          properties: {},
          properties_attributes: {}
        };
        service.typesToSave.forEach(function (type) {
          group.properties[type] = configs[type];
          group.properties_attributes[type] = attributes[type];
        });
        groups.push(group);
      }
    });
    this.setConfigsChanges(groups);
  },

  /**
   * Is deleteHost action id fired
   * @type {bool}
   */
  fromDeleteHost: false,

  /**
   * Is ZooKeeper Server being deleted from host
   * @type {bool}
   */
  fromDeleteZkServer: false,

  /**
   * Send command to server to install selected host component
   * @param {Object} event
   * @method installComponent
   */
  installComponent: function (event) {
    var self = this;
    var component = event.context;
    var componentName = component.get('componentName');
    var displayName = component.get('displayName');

    return App.ModalPopup.show({
      primary: Em.I18n.t('hosts.host.installComponent.popup.confirm'),
      header: Em.I18n.t('popup.confirmation.commonHeader'),
      installComponentMessage: Em.I18n.t('hosts.host.installComponent.msg').format(displayName),
      bodyClass: Em.View.extend({
        templateName: require('templates/main/host/details/installComponentPopup')
      }),
      onPrimary: function () {
        var _this = this;
        App.get('router.mainAdminKerberosController').getSecurityType(function () {
          App.get('router.mainAdminKerberosController').getKDCSessionState(function () {
            _this.hide();

            App.ajax.send({
              name: 'common.host.host_component.update',
              sender: self,
              data: {
                hostName: self.get('content.hostName'),
                serviceName: component.get('service.serviceName'),
                componentName: componentName,
                component: component,
                context: Em.I18n.t('requestInfo.installHostComponent') + " " + displayName,
                HostRoles: {
                  state: 'INSTALLED'
                }
              },
              success: 'installComponentSuccessCallback',
              error: 'ajaxErrorCallback',
              showLoadingPopup: true
            });
          })
        });
      }
    });
  },

  /**
   * Success callback for install component request
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method installComponentSuccessCallback
   */
  installComponentSuccessCallback: function (data, opt, params) {
    if (App.get('testMode')) {
      this.mimicWorkStatusChange(params.component, App.HostComponentStatus.installing, App.HostComponentStatus.stopped);
    }
    this.showBackgroundOperationsPopup();
  },

  /**
   * Send command to server to run decommission on DATANODE, TASKTRACKER, NODEMANAGER, REGIONSERVER
   * @param {App.HostComponent} component
   * @param {callback} callback
   * @method decommission
   */
  decommission: function (component, callback) {
    var self = this;
    return App.showConfirmationPopup(function () {
      self.runDecommission.call(self, self.get('content.hostName'), component.get('service.serviceName'));
      if (callback) {
        callback()
      }
    }, Em.I18n.t('question.sure.decommission').format(component.get('service.serviceName')));
  },
  /**
   * identify correct component to run decommission on them by service name,
   * in result run proper decommission method
   * @param hostName
   * @param svcName
   */
  runDecommission: function (hostName, svcName) {
    switch (svcName) {
      case 'HDFS':
        this.doDecommission(hostName, svcName, "NAMENODE", "DATANODE");
        break;
      case 'YARN':
        this.doDecommission(hostName, svcName, "RESOURCEMANAGER", "NODEMANAGER");
        break;
      case 'HBASE':
        this.warnBeforeDecommission(hostName);
    }
  },

  /**
   * Send command to server to run recommission on DATANODE, TASKTRACKER, NODEMANAGER
   * @param {App.HostComponent} component
   * @param {callback} callback
   * @method recommission
   */
  recommission: function (component, callback) {
    var self = this;
    return App.showConfirmationPopup(function () {
      self.runRecommission.call(self, self.get('content.hostName'), component.get('service.serviceName'));
      if (callback) {
        callback()
      }
    }, Em.I18n.t('question.sure.recommission').format(component.get('service.serviceName')));
  },
  /**
   * identify correct component to run recommission on them by service name,
   * in result run proper recommission method
   * @param hostName
   * @param svcName
   */
  runRecommission: function (hostName, svcName) {
    switch (svcName) {
      case 'HDFS':
        this.doRecommissionAndStart(hostName, svcName, "NAMENODE", "DATANODE");
        break;
      case 'YARN':
        this.doRecommissionAndStart(hostName, svcName, "RESOURCEMANAGER", "NODEMANAGER");
        break;
      case 'HBASE':
        this.doRecommissionAndStart(hostName, svcName, "HBASE_MASTER", "HBASE_REGIONSERVER");
    }
    this.showBackgroundOperationsPopup();
  },

  /**
   * Performs Decommission (for DN, TT and NM)
   * @param {string} hostName
   * @param {string} serviceName
   * @param {string} componentName
   * @param {string} slaveType
   * @method doDecommission
   */
  doDecommission: function (hostName, serviceName, componentName, slaveType) {
    var contextNameString = 'hosts.host.' + slaveType.toLowerCase() + '.decommission';
    var context = Em.I18n.t(contextNameString);
    App.ajax.send({
      name: 'host.host_component.decommission_slave',
      sender: this,
      data: {
        context: context,
        command: 'DECOMMISSION',
        hostName: hostName,
        serviceName: serviceName,
        componentName: componentName,
        slaveType: slaveType
      },
      success: 'decommissionSuccessCallback',
      error: 'decommissionErrorCallback',
      showLoadingPopup: true
    });
  },

  /**
   * check is hbase regionserver in mm. If so - run decommission
   * otherwise shows warning
   * @method warnBeforeDecommission
   * @param {string} hostNames - list of host when run from bulk operations or current host
   */
  warnBeforeDecommission: function (hostNames) {
    if (this.get('content.hostComponents').findProperty('componentName', 'HBASE_REGIONSERVER').get('passiveState') == "OFF") {
      this.showHbaseActiveWarning();
    } else {
      this.checkRegionServerState(hostNames);
    }
  },

  /**
   *  send call to check is this regionserver last in cluster which has desired_admin_state property "INSERVICE"
   * @method checkRegionServerState
   * @param hostNames
   */
  checkRegionServerState: function (hostNames) {
    return App.ajax.send({
      name: 'host.region_servers.in_inservice',
      sender: this,
      data: {
        hostNames: hostNames
      },
      success: 'checkRegionServerStateSuccessCallback'
    });
  },

  /**
   * check is this regionserver last in cluster which has desired_admin_state property "INSERVICE"
   * @method checkRegionServerStateSuccessCallback
   * @param data
   * @param opt
   * @param params
   */
  checkRegionServerStateSuccessCallback: function (data, opt, params) {
    var hostArray = params.hostNames.split(",");
    var decommissionPossible = (data.items.mapProperty('HostRoles.host_name').filter(function (hostName) {
      return !hostArray.contains(hostName);
    }, this).length >= 1);
    if (decommissionPossible) {
      this.doDecommissionRegionServer(params.hostNames, "HBASE", "HBASE_MASTER", "HBASE_REGIONSERVER");
    } else {
      this.showRegionServerWarning();
    }
  },

  /**
   * show warning that regionserver is last in cluster which has desired_admin_state property "INSERVICE"
   * @method showRegionServerWarning
   * @param hostNames
   */
  showRegionServerWarning: function () {
    return App.ModalPopup.show({
      header: Em.I18n.t('common.warning'),
      message: Em.I18n.t('hosts.host.hbase_regionserver.decommission.warning'),
      bodyClass: Ember.View.extend({
        template: Em.Handlebars.compile('<div class="alert alert-warning">{{message}}</div>')
      }),
      secondary: false
    });
  },

  /**
   * shows warning: put hbase regionserver in passive state
   * @method showHbaseActiveWarning
   * @return {App.ModalPopup}
   */
  showHbaseActiveWarning: function () {
    return App.ModalPopup.show({
      header: Em.I18n.t('common.warning'),
      message: Em.I18n.t('hostPopup.recommendation.beforeDecommission').format(App.format.components["HBASE_REGIONSERVER"]),
      bodyClass: Ember.View.extend({
        template: Em.Handlebars.compile('<div class="alert alert-warning">{{message}}</div>')
      }),
      secondary: false
    });
  },

  /**
   * Performs Decommission (for RegionServer)
   * @method doDecommissionRegionServer
   * @param {string} hostNames - list of host when run from bulk operations or current host
   * @param {string} serviceName - serviceName
   * @param {string} componentName - master compoent name
   * @param {string} slaveType - slave component name
   */
  doDecommissionRegionServer: function (hostNames, serviceName, componentName, slaveType) {
    var batches = [
      {
        "order_id": 1,
        "type": "POST",
        "uri": "/clusters/" + App.get('clusterName') + "/requests",
        "RequestBodyInfo": {
          "RequestInfo": {
            "context": Em.I18n.t('hosts.host.regionserver.decommission.batch1'),
            "command": "DECOMMISSION",
            "exclusive": "true",
            "parameters": {
              "slave_type": slaveType,
              "excluded_hosts": hostNames
            },
            'operation_level': {
              level: "HOST_COMPONENT",
              cluster_name: App.get('clusterName'),
              host_name: hostNames,
              service_name: serviceName
            }
          },
          "Requests/resource_filters": [
            {"service_name": serviceName, "component_name": componentName}
          ]
        }
      }];
    var id = 2;
    var hAray = hostNames.split(",");
    for (var i = 0; i < hAray.length; i++) {
      batches.push({
        "order_id": id,
        "type": "PUT",
        "uri": "/clusters/" + App.get('clusterName') + "/hosts/" + hAray[i] + "/host_components/" + slaveType,
        "RequestBodyInfo": {
          "RequestInfo": {
            context: Em.I18n.t('hosts.host.regionserver.decommission.batch2'),
            exclusive: true,
            operation_level: {
              level: "HOST_COMPONENT",
              cluster_name: App.get('clusterName'),
              host_name: hostNames,
              service_name: serviceName || null
            }
          },
          "Body": {
            HostRoles: {
              state: "INSTALLED"
            }
          }
        }
      });
      id++;
    }
    batches.push({
      "order_id": id,
      "type": "POST",
      "uri": "/clusters/" + App.get('clusterName') + "/requests",
      "RequestBodyInfo": {
        "RequestInfo": {
          "context": Em.I18n.t('hosts.host.regionserver.decommission.batch3'),
          "command": "DECOMMISSION",
          "service_name": serviceName,
          "component_name": componentName,
          "parameters": {
            "slave_type": slaveType,
            "excluded_hosts": hostNames,
            "mark_draining_only": true
          },
          'operation_level': {
            level: "HOST_COMPONENT",
            cluster_name: App.get('clusterName'),
            host_name: hostNames,
            service_name: serviceName
          }
        },
        "Requests/resource_filters": [
          {"service_name": serviceName, "component_name": componentName}
        ]
      }
    });
    App.ajax.send({
      name: 'common.batch.request_schedules',
      sender: this,
      data: {
        intervalTimeSeconds: 1,
        tolerateSize: 0,
        batches: batches
      },
      success: 'decommissionSuccessCallback',
      error: 'decommissionErrorCallback',
      showLoadingPopup: true
    });
  },

  /**
   * Error callback for decommission requests
   * @param {object} request
   * @param {object} ajaxOptions
   * @param {string} error
   * @method decommissionErrorCallback
   */
  decommissionErrorCallback: function (request, ajaxOptions, error) {
  },

  /**
   * Success ajax response for Recommission/Decommission slaves
   * @param {object} data
   * @method decommissionSuccessCallback
   * @return {Boolean}
   */
  decommissionSuccessCallback: function (data) {
    if (data && (data.Requests || data.resources[0].RequestSchedule)) {
      this.showBackgroundOperationsPopup();
      return true;
    } else {
      return false;
    }
  },

  /**
   * Performs Recommission and Start
   * @param {string} hostNames
   * @param {string} serviceName
   * @param {string} componentName
   * @param {string} slaveType
   * @method doRecommissionAndStart
   */
  doRecommissionAndStart: function (hostNames, serviceName, componentName, slaveType) {
    var contextNameString_1 = 'hosts.host.' + slaveType.toLowerCase() + '.recommission';
    var context_1 = Em.I18n.t(contextNameString_1);
    var contextNameString_2 = 'requestInfo.startHostComponent.' + slaveType.toLowerCase();
    var startContext = Em.I18n.t(contextNameString_2);
    var params = {
      "slave_type": slaveType,
      "included_hosts": hostNames
    };
    if (serviceName == "HBASE") {
      params.mark_draining_only = true;
    }
    var batches = [
      {
        "order_id": 1,
        "type": "POST",
        "uri": "/clusters/" + App.get('clusterName') + "/requests",
        "RequestBodyInfo": {
          "RequestInfo": {
            "context": context_1,
            "command": "DECOMMISSION",
            "exclusive": "true",
            "parameters": params,
            'operation_level': {
              level: "HOST_COMPONENT",
              cluster_name: App.get('clusterName'),
              host_name: hostNames,
              service_name: serviceName
            }
          },
          "Requests/resource_filters": [
            {"service_name": serviceName, "component_name": componentName}
          ]
        }
      }];
    var id = 2;
    var hAray = hostNames.split(",");
    for (var i = 0; i < hAray.length; i++) {
      batches.push({
        "order_id": id,
        "type": "PUT",
        "uri": "/clusters/" + App.get('clusterName') + "/hosts/" + hAray[i] + "/host_components/" + slaveType,
        "RequestBodyInfo": {
          "RequestInfo": {
            context: startContext,
            operation_level: {
              level: "HOST_COMPONENT",
              cluster_name: App.get('clusterName'),
              host_name: hostNames,
              service_name: serviceName || null
            }
          },
          "Body": {
            HostRoles: {
              state: "STARTED"
            }
          }
        }
      });
      id++;
    }
    App.ajax.send({
      name: 'common.batch.request_schedules',
      sender: this,
      data: {
        intervalTimeSeconds: 1,
        tolerateSize: 1,
        batches: batches
      },
      success: 'decommissionSuccessCallback',
      error: 'decommissionErrorCallback',
      showLoadingPopup: true
    });
  },

  /**
   * Handler for host-menu items actions
   * @param {object} option
   * @method doAction
   */
  doAction: function (option) {
    switch (option.context.action) {
      case "deleteHost":
        this.validateAndDeleteHost();
        break;
      case "startAllComponents":
        if (!this.get('content.isNotHeartBeating')) this.doStartAllComponents();
        break;
      case "stopAllComponents":
        if (!this.get('content.isNotHeartBeating')) this.doStopAllComponents();
        break;
      case "restartAllComponents":
        if (!this.get('content.isNotHeartBeating')) this.doRestartAllComponents();
        break;
      case "onOffPassiveModeForHost":
        this.onOffPassiveModeForHost(option.context);
        break;
      case "setRackId":
        this.setRackIdForHost();
        break;
      case "checkHost":
        this.runHostCheckConfirmation();
        break;
      case "regenerateKeytabFileOperations":
        this.regenerateKeytabFileOperations();
        break;
    }
  },

  /**
   * Turn On/Off Passive Mode for host
   * @param {object} context
   * @method onOffPassiveModeForHost
   */
  onOffPassiveModeForHost: function (context) {
    var state = context.active ? 'ON' : 'OFF';
    var self = this;
    var message = Em.I18n.t('hosts.host.details.for.postfix').format(context.label);
    var popupInfo = Em.I18n.t('hosts.passiveMode.popup').format(context.active ? 'On' : 'Off', this.get('content.hostName'));
    if (state === 'OFF') {
      var currentHostVersion = this.get('content.stackVersions') && this.get('content.stackVersions').findProperty('isCurrent'),
        hostVersion = currentHostVersion && currentHostVersion.get('repoVersion'),
        currentVersion = App.StackVersion.find().findProperty('isCurrent'),
        clusterVersion = currentVersion && currentVersion.get('repositoryVersion.repositoryVersion');
      if (hostVersion !== clusterVersion) {
        var msg = Em.I18n.t("hosts.passiveMode.popup.version.mismatch").format(this.get('content.hostName'), clusterVersion);
        popupInfo += '<br/><div class="alert alert-warning">' + msg + '</div>';
      }
    }
    return App.showConfirmationPopup(function () {
        self.hostPassiveModeRequest(state, message);
      }, popupInfo);
  },

  /**
   * Set rack id for host
   * @method setRackIdForHost
   */
  setRackIdForHost: function () {
    var hostNames = [{hostName: this.get('content.hostName')}];
    var rack = this.get('content.rack');
    var operationData = {message: Em.I18n.t('hosts.host.details.setRackId')};
    hostsManagement.setRackInfo(operationData, hostNames, rack);
  },

  /**
   * Send request to get passive state for host
   * @param {string} state
   * @param {string} message
   * @method hostPassiveModeRequest
   */
  hostPassiveModeRequest: function (state, message) {
    App.ajax.send({
      name: 'bulk_request.hosts.passive_state',
      sender: this,
      data: {
        hostNames: this.get('content.hostName'),
        passive_state: state,
        requestInfo: message
      },
      success: 'updateHost'
    });
  },

  /**
   * Success callback for receiving host passive state
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method updateHost
   */
  updateHost: function (data, opt, params) {
    this.set('content.passiveState', params.passive_state);
    batchUtils.infoPassiveState(params.passive_state);
  },

  /**
   * Send request to get passive state for hostComponent
   * @param {object} component - hostComponentn object
   * @param {string} state
   * @param {string} message
   * @method hostPassiveModeRequest
   */
  updateComponentPassiveState: function (component, state, message) {
    App.ajax.send({
      name: 'common.host.host_component.passive',
      sender: this,
      data: {
        hostName: this.get('content.hostName'),
        componentName: component.get('componentName'),
        component: component,
        passive_state: state,
        context: message
      },
      success: 'updateHostComponent'
    });
  },

  /**
   * Success callback for receiving hostComponent passive state
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method updateHost
   */
  updateHostComponent: function (data, opt, params) {
    params.component.set('passiveState', params.passive_state);
    batchUtils.infoPassiveState(params.passive_state);
  },

  /**
   * Show confirmation popup for action "start all components"
   * @method doStartAllComponents
   */
  doStartAllComponents: function () {
    var self = this;
    var components = this.get('serviceNonClientActiveComponents');

    if (components && components.get('length')) {
      return App.showConfirmationPopup(function () {
        self.sendComponentCommand(components, Em.I18n.t('hosts.host.maintainance.startAllComponents.context'), App.HostComponentStatus.started);
      }, Em.I18n.t('question.sure.startAll'));
    }
  },

  /**
   * Show confirmation popup for action "stop all components"
   * @method doStopAllComponents
   */
  doStopAllComponents: function () {
    var self = this;
    var components = this.get('serviceNonClientActiveComponents');

    if (components && components.get('length')) {
      if (components.someProperty('componentName', 'NAMENODE') &&
        this.get('content.hostComponents').filterProperty('componentName', 'NAMENODE').someProperty('workStatus', App.HostComponentStatus.started)) {
        this.checkNnLastCheckpointTime(function () {
          App.showConfirmationPopup(function () {
            self.sendComponentCommand(components, Em.I18n.t('hosts.host.maintainance.stopAllComponents.context'), App.HostComponentStatus.stopped);
          }, Em.I18n.t('question.sure.stopAll'));
        });
      } else {
        return App.showConfirmationPopup(function () {
          self.sendComponentCommand(components, Em.I18n.t('hosts.host.maintainance.stopAllComponents.context'), App.HostComponentStatus.stopped);
        }, Em.I18n.t('question.sure.stopAll'));
      }
    }
  },

  /**
   * Show confirmation popup for action "restart all components"
   * @method doRestartAllComponents
   */
  doRestartAllComponents: function () {
    var self = this;
    var components = this.get('serviceActiveComponents');

    if (components && components.get('length')) {
      if (components.someProperty('componentName', 'NAMENODE') &&
        this.get('content.hostComponents').filterProperty('componentName', 'NAMENODE').someProperty('workStatus', App.HostComponentStatus.started)) {
        this.checkNnLastCheckpointTime(function () {
          App.showConfirmationPopup(function () {
            batchUtils.restartHostComponents(components, Em.I18n.t('rollingrestart.context.allOnSelectedHost').format(self.get('content.hostName')), "HOST");
          }, Em.I18n.t('question.sure.restartAll'));
        });
      } else {
        return App.showConfirmationPopup(function () {
          batchUtils.restartHostComponents(components, Em.I18n.t('rollingrestart.context.allOnSelectedHost').format(self.get('content.hostName')), "HOST");
        }, Em.I18n.t('question.sure.restartAll'));
      }
    }
  },

  /**
   * get info about host-components, exactly:
   *  - host-components grouped by status, features
   *  - flag, that indicate whether ZooKeeper Server is installed
   * @return {Object}
   */
  getHostComponentsInfo: function (hostComponents) {
    const componentsOnHost = hostComponents || this.get('content.hostComponents');
    const stoppedStates = [App.HostComponentStatus.stopped,
      App.HostComponentStatus.install_failed,
      App.HostComponentStatus.upgrade_failed,
      App.HostComponentStatus.init,
      App.HostComponentStatus.unknown];
    const container = {
      isReconfigureRequired: false,
      lastComponents: [],
      masterComponents: [],
      nonAddableMasterComponents: [],
      lastMasterComponents: [],
      runningComponents: [],
      nonDeletableComponents: [],
      unknownComponents: [],
      toDecommissionComponents: []
    };
    const addDeleteComponentsMap = this.get('addDeleteComponentsMap');

    if (componentsOnHost && componentsOnHost.get('length') > 0) {
      componentsOnHost.forEach(function (cInstance) {
        if (addDeleteComponentsMap[cInstance.get('componentName')]) {
          container.isReconfigureRequired = true;
        }
        let isLastComponent = false;
        if (this.getTotalComponent(cInstance) === 1) {
          container.lastComponents.push(cInstance.get('displayName'));
          isLastComponent = true;
        }
        const workStatus = cInstance.get('workStatus');

        if (cInstance.get('isMaster')) {
          const displayName = cInstance.get('displayName');
          container.masterComponents.push(displayName);
          if (!App.StackServiceComponent.find(cInstance.get('componentName')).get('isMasterAddableInstallerWizard'))  {
            container.nonAddableMasterComponents.push(displayName);
          }
          if (isLastComponent) {
            container.lastMasterComponents.push(displayName);
          }
        }
        if (stoppedStates.indexOf(workStatus) < 0) {
          container.runningComponents.push(cInstance.get('displayName'));
        }
        if (!cInstance.get('isDeletable')) {
          container.nonDeletableComponents.push(cInstance.get('displayName'));
        }
        if (workStatus === App.HostComponentStatus.unknown) {
          container.unknownComponents.push(cInstance.get('displayName'));
        }
        if (App.get('components.decommissionAllowed').contains(cInstance.get('componentName')) && !cInstance.get('view.isComponentRecommissionAvailable')) {
          container.toDecommissionComponents.push(cInstance.get('displayName'));
        }
      }, this);
    }
    return container;
  },

  /**
   * Run host check confirmation
   * @method runHostCheckConfirmation
   */
  runHostCheckConfirmation: function () {
    var self = this;
    var popupInfo = Em.I18n.t('hosts.checkHost.popup').format(this.get('content.hostName'));

    return App.showConfirmationPopup(function () {
      self.runHostCheck();
    }, popupInfo);
  },

  getDataForHostCheck: function () {
    var hostName = this.get('content.hostName');
    var jdk_location = App.router.get('clusterController.ambariProperties.jdk_location');
    var RequestInfo = {
      "action": "check_host",
      "context": "Check host",
      "parameters": {
        "hosts" : hostName,
        "check_execute_list": "last_agent_env_check,installed_packages,existing_repos,transparentHugePage",
        "jdk_location" : jdk_location,
        "threshold": "20"
      }
    };

    return {
      RequestInfo: RequestInfo,
      resource_filters: {"hosts": hostName}
    };
  },

  /**
   * Callback for runHostCheckConfirmation
   * @method runHostCheck
   */
  runHostCheck: function () {
    var dataForCheckHostRequest = this.getDataForHostCheck();

    this.setProperties({
      stopChecking: false,
      checkHostFinished: false,
      isRerun: false
    });
    this.setBootHostsProp();
    this.showHostWarningsPopup();
    this.requestToPerformHostCheck(dataForCheckHostRequest);
  },

  /**
   * Shape controller's bootHosts property needed to host check
   * @method setBootHostsProp
   */
  setBootHostsProp: function () {
    var host = this.get('content');
    var bootHosts = [];

    host.name = host.get('hostName');
    bootHosts.push(host);

    this.set('bootHosts', bootHosts);
  },

  /**
   * Open popup that contain hosts' warnings
   * @return {App.ModalPopup}
   * @method showHostWarningsPopup
   */
  showHostWarningsPopup: function () {
    var self = this;

    return App.ModalPopup.show({

      header: Em.I18n.t('installer.step3.warnings.popup.header'),

      secondary: Em.I18n.t('installer.step3.hostWarningsPopup.rerunChecks'),

      primary: Em.I18n.t('common.close'),

      autoHeight: false,

      onPrimary: function () {
        self.set('checksUpdateStatus', null);
        this.hide();
      },

      onClose: function () {
        self.set('checksUpdateStatus', null);
        this.hide();
      },

      onSecondary: function () {
        self.set('checkHostFinished', false);
        self.rerunChecks();
      },

      didInsertElement: function () {
        this._super();
        this.fitHeight();
      },

      footerClass: App.WizardStep3HostWarningPopupFooter.reopen({
        footerControllerBinding: 'App.router.mainHostDetailsController',
        checkHostFinished: function () {
          return this.get('footerController.checkHostFinished');
        }.property('footerController.checkHostFinished')
      }),

      bodyClass: App.WizardStep3HostWarningPopupBody.reopen({
        bodyControllerBinding: 'App.router.mainHostDetailsController',
        checkHostFinished: function () {
          return this.get('bodyController.checkHostFinished');
        }.property('bodyController.checkHostFinished')
      })
    });
  },


  /**
   * Deletion of hosts not supported for this version
   * @method validateAndDeleteHost
   */
  validateAndDeleteHost: function () {
    const container = this.getHostComponentsInfo();

    if (container.nonDeletableComponents.length > 0) {
      this.raiseDeleteComponentsError(container, 'nonDeletableList');
      return;
    } else if (container.nonAddableMasterComponents.length > 0) {
      this.raiseDeleteComponentsError(container, 'masterList');
      return;
    } else if (container.runningComponents.length > 0) {
      this.raiseDeleteComponentsError(container, 'runningList');
      return;
    } else if(container.lastMasterComponents.length > 0) {
      this.raiseDeleteComponentsError(container, 'lastMasterList');
      return;
    }

    this.set('fromDeleteHost', true);

    if (container.isReconfigureRequired) {
      this.reconfigureAndDeleteHost(container);
    } else {
      this.confirmDeleteHost(container);
    }
  },

  /**
   *
   * @param {object} container
   */
  reconfigureAndDeleteHost: function(container) {
    const addDeleteComponentsMap = this.get('addDeleteComponentsMap');
    const hostName = this.get('content.hostName');
    const reconfiguredComponents = [];
    const componentStub = Em.Object.create();

    this.get('content.hostComponents').forEach((component) => {
      const componentsMapItem = addDeleteComponentsMap[component.get('componentName')];
      if (componentsMapItem) {
        reconfiguredComponents.push(component.get('displayName'));
        if (componentsMapItem.hostPropertyName) {
          this.set(componentsMapItem.hostPropertyName, hostName);
        }
        if (componentsMapItem.addPropertyName) {
          this.set(componentsMapItem.addPropertyName, true);
        }
        this.loadComponentRelatedConfigs(componentsMapItem.configTagsCallbackName, componentsMapItem.configsCallbackName);
      }
    });
    this.showReconfigurationPopupPreDelete(componentStub, () => {
      this.confirmDeleteHost(container);
    }, Em.I18n.t('hosts.host.delete.componentsRequireReconfigure').format(reconfiguredComponents.join(', ')));
  },

  /**
   * Show popup with info about reasons why host can't be deleted
   * @param {string[]} components
   * @param {string} type
   * @method raiseDeleteComponentsError
   */
  raiseDeleteComponentsError: function (container, type) {
    App.ModalPopup.show({
      header: Em.I18n.t('hosts.cant.do.popup.title'),
      type: type,
      showBodyEnd: Em.computed.existsIn('type', ['runningList', 'masterList', 'lastMasterList']),
      container: container,
      components: function(){
        var container = this.get('container');
        switch (this.get('type')) {
          case 'masterList':
            return container.nonAddableMasterComponents;
          case 'nonDeletableList':
            return container.nonDeletableComponents;
          case 'runningList':
            return container.runningComponents;
          case 'lastMasterList':
            return container.lastMasterComponents;
        }
      }.property('type'),
      componentsStr: function () {
        return this.get('components').join(", ");
      }.property('components.[]'),
      componentsBody: Em.computed.i18nFormat('hosts.cant.do.popup.' + type + '.body', 'components.length'),
      componentsBodyEnd: function () {
        if (this.get('showBodyEnd')) {
          return Em.I18n.t('hosts.cant.do.popup.' + type + '.body.end').format(App.get('components.decommissionAllowed').map(function(c){return App.format.role(c, false)}).join(", "));
        }
        return '';
      }.property(),
      bodyClass: Em.View.extend({
        templateName: require('templates/main/host/details/raiseDeleteComponentErrorPopup')
      }),
      secondary: null
    });
  },

  /**
   * Show confirmation popup to delete host
   * @param {Object} container
   * @method confirmDeleteHost
   */
  confirmDeleteHost: function (container) {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('hosts.delete.popup.title'),
      deletePopupBody: Em.I18n.t('hosts.delete.popup.body').format(self.get('content.publicHostName')),
      lastComponent: function () {
        if (container.lastComponents && container.lastComponents.length) {
          this.set('isChecked', false);
          return true;
        } else {
          this.set('isChecked', true);
          return false;
        }
      }.property(),
      disablePrimary: Em.computed.not('isChecked'),
      isChecked: false,
      lastComponentError: Em.View.extend({
        template: Em.Handlebars.compile(Em.I18n.t('hosts.delete.popup.body.msg4').format(container.lastComponents))
      }),
      unknownComponents: function () {
        if (container.unknownComponents && container.unknownComponents.length) {
          return container.unknownComponents.join(", ");
        }
        return '';
      }.property(),
      decommissionWarning: Em.View.extend({
        template: Em.Handlebars.compile(Em.I18n.t('hosts.delete.popup.body.msg7').format(container.toDecommissionComponents.join(', ')))
      }),
      toDecommissionComponents: container.toDecommissionComponents,
      bodyClass: Em.View.extend({
        templateName: require('templates/main/host/details/doDeleteHostPopup')
      }),
      onPrimary: function () {
        this.hide();
        self.doDeleteHost();
      }
    });
  },

  /**
   * send DELETE calls to components of host and after delete host itself
   * @method doDeleteHost
   */
  doDeleteHost: function () {
    const allComponents = this.get('content.hostComponents');
    const addDeleteComponentsMap = this.get('addDeleteComponentsMap');
    const deleteRequests = [];
    let deleteError = null;
    const dfd = $.Deferred();

    if (allComponents.get('length') > 0) {
      allComponents.forEach(function (component) {
        deleteRequests.push(this._doDeleteHostComponent(component.get('componentName')));
      }, this);
      $.when(deleteRequests).done(() => {
        if (this.get('isReconfigureRequired')) {
          const reconfiguredComponents = allComponents
            .filter((component) => addDeleteComponentsMap[component.get('componentName')])
            .mapProperty('displayName').join(', ');
          this.applyConfigsCustomization();
          this.putConfigsToServer(this.get('groupedPropertiesToChange'), reconfiguredComponents);
          this.clearConfigsChanges();
        }
        this.deleteHostCall();
      }).fail(() => {
        deleteError = this.get('_deletedHostComponentError');
        deleteError.xhr.responseText = "{\"message\": \"" + deleteError.xhr.statusText + "\"}";
        App.ajax.defaultErrorHandler(deleteError.xhr, deleteError.url, deleteError.type, deleteError.xhr.status);
      }).always(dfd.resolve);
    } else {
      dfd.resolve();
    }
    return dfd.promise();
  },

  deleteHostCall: function() {
    return App.ajax.send({
      name: 'common.delete.host',
      sender: this,
      data: {
        hostName: this.get('content.hostName')
      },
      success: 'deleteHostCallSuccessCallback',
      error: 'deleteHostCallErrorCallback',
      showLoadingPopup: true
    });
  },

  deleteHostCallSuccessCallback: function (data, rq, requestBody) {
    App.router.get('updateController').updateHost(function () {
      App.router.transitionTo('hosts.index');
    });
    if (!!(requestBody && requestBody.hostName)) {
      var remainingHosts = App.db.getSelectedHosts().removeObject(requestBody.hostName);
      App.db.setSelectedHosts(remainingHosts);
      App.hostsMapper.deleteRecord(App.Host.find().findProperty('hostName', requestBody.hostName));
    }
    App.router.get('clusterController').getAllHostNames();
  },
  deleteHostCallErrorCallback: function (xhr, textStatus, errorThrown, opt) {
    xhr.responseText = "{\"message\": \"" + xhr.statusText + "\"}";
    App.ajax.defaultErrorHandler(xhr, opt.url, 'DELETE', xhr.status);
  },

  /**
   * Send command to server to restart all host components with stale configs
   * @method restartAllStaleConfigComponents
   */
  restartAllStaleConfigComponents: function () {
    var self = this;
    var staleComponents = self.get('content.componentsWithStaleConfigs');
    if (staleComponents.someProperty('componentName', 'NAMENODE') &&
      this.get('content.hostComponents').filterProperty('componentName', 'NAMENODE').someProperty('workStatus', App.HostComponentStatus.started)) {
      this.checkNnLastCheckpointTime(function () {
        App.showConfirmationPopup(function () {
          batchUtils.restartHostComponents(staleComponents, Em.I18n.t('rollingrestart.context.allWithStaleConfigsOnSelectedHost').format(self.get('content.hostName')), "HOST");
        });
      });
    } else {
      return App.showConfirmationPopup(function () {
        batchUtils.restartHostComponents(staleComponents, Em.I18n.t('rollingrestart.context.allWithStaleConfigsOnSelectedHost').format(self.get('content.hostName')), "HOST");
      });
    }

  },

  /**
   * open Reassign Master Wizard with selected component
   * @param {object} event
   * @method moveComponent
   */
  moveComponent: function (event) {
    var component = event.context;
    if ($(event.target).closest('li').hasClass('disabled')) {
      return;
    }
    return App.showConfirmationPopup(function () {
      var reassignMasterController = App.router.get('reassignMasterController');
      reassignMasterController.saveComponentToReassign(component);
      reassignMasterController.setCurrentStep('1');
      App.router.transitionTo('reassign');
    }, Em.I18n.t('question.sure.move').format(component.get('displayName')));
  },

  /**
   * Restart clients host components to apply config changes
   * @param {object} event
   * @method refreshConfigs
   */
  refreshConfigs: function (event) {
    var self = this;
    var components = event.context;
    if (components.get('length') > 0) {
      return App.showConfirmationPopup(function () {
        batchUtils.restartHostComponents(components, Em.I18n.t('rollingrestart.context.allClientsOnSelectedHost').format(self.get('content.hostName')), "HOST");
      }, Em.I18n.t('question.sure.refresh').format(self.get('content.hostName')) );
    }
  },

  toggleMaintenanceMode: function (event) {
    var state, message, self = this;
    if (event.context.get('isImpliedState')) return null;
    state = event.context.get('passiveState') === "ON" ? "OFF" : "ON";
    message = Em.I18n.t('passiveState.turn' + state.toCapital() + 'For').format(event.context.get('displayName'));
    return App.showConfirmationPopup(function () {
      self.updateComponentPassiveState(event.context, state, message);
    }, Em.I18n.t('question.sure.maintenance').format(event.context.get('displayName')) );
  },

  downloadClientConfigs: function (event) {
    this.downloadClientConfigsCall({
      hostName: event.context.get('hostName'),
      componentName: event.context.get('componentName'),
      resourceType: this.resourceTypeEnum.HOST_COMPONENT
    });
  },

  /**
   *  This controller action is called from the template when user clicks to download configs for "All Clients On Host"
   */
  downloadAllClientConfigs: function () {
    var self = this;
    this.downloadClientConfigsCall({
      hostName: self.get('content.hostName'),
      resourceType: this.resourceTypeEnum.HOST
    });
  },

  installClients: function (components) {
    var clientsToInstall = [],
      clientsToAdd = [],
      missedComponents = [],
      dependentComponents = [],
      self = this;
    components.forEach(function (component) {
      if (['INIT', 'INSTALL_FAILED'].contains(component.get('workStatus'))) {
        clientsToInstall.push(component);
      } else if (typeof component.get('workStatus') == 'undefined') {
        clientsToAdd.push(component);
      }
    });
    clientsToAdd.forEach(function (component, index, array) {
      var dependencies = this.checkComponentDependencies(component.get('componentName'), {
        scope: 'host',
        installedComponents: this.get('content.hostComponents').mapProperty('componentName')
      }).reject(function (componentName) {
        return array.mapProperty('componentName').contains(componentName);
      });
      if (dependencies.length) {
        missedComponents.pushObjects(dependencies);
        dependentComponents.push(component.get('displayName'));
      }
    }, this);
    missedComponents = missedComponents.uniq();
    if (missedComponents.length) {
      var popupMessage = Em.I18n.t('host.host.addComponent.popup.clients.dependedComponents.body').format(stringUtils.getFormattedStringFromArray(dependentComponents),
        stringUtils.getFormattedStringFromArray(missedComponents.map(function (componentName) {
          return App.StackServiceComponent.find(componentName).get('displayName');
        })));
      App.showAlertPopup(Em.I18n.t('host.host.addComponent.popup.dependedComponents.header'), popupMessage);
    } else {
      App.get('router.mainAdminKerberosController').getSecurityType(function () {
        App.get('router.mainAdminKerberosController').getKDCSessionState(function () {
          var sendInstallCommand = function () {
            if (clientsToInstall.length) {
              self.sendComponentCommand(clientsToInstall, Em.I18n.t('host.host.details.installClients'), 'INSTALLED');
            }
          };
          if (clientsToAdd.length) {
            var message = stringUtils.getFormattedStringFromArray(clientsToAdd.mapProperty('displayName')),
              componentObject = Em.Object.create({
                displayName: message
              });
            self.showAddComponentPopup(componentObject, self.get('content.hostName'), function () {
              sendInstallCommand();
              clientsToAdd.forEach(function (component) {
                self.installHostComponentCall(self.get('content.hostName'), component);
              });
            });
          } else {
            sendInstallCommand();
          }
        });
      }.bind(this));
    }
  },

  /**
   * Check if all required components are installed on host.
   * Available options:
   *  scope: 'host' - dependency level `host`,`cluster` or `*`.
   *  hostName: 'example.com' - host name to search installed components
   *  installedComponents: ['A', 'B'] - names of installed components
   *
   * By default scope level is `*`
   * For host level dependency you should specify at least `hostName` or `installedComponents` attribute.
   *
   * @param {String} componentName
   * @param {Object} opt - options. Allowed options are `hostName`, `installedComponents`, `scope`.
   * @return {Array} - names of missed components
   */
  checkComponentDependencies: function (componentName, opt) {
    opt = opt || {};
    opt.scope = opt.scope || '*';
    var installedComponents;
    switch (opt.scope) {
      case 'host':
        Em.assert("You should pass at least `hostName` or `installedComponents` to options.", opt.hostName || opt.installedComponents);
        installedComponents = opt.installedComponents || App.HostComponent.find().filterProperty('hostName', opt.hostName).mapProperty('componentName').uniq();
        break;
      default:
        // @todo: use more appropriate value regarding installed components
        installedComponents = opt.installedComponents || App.HostComponent.find().mapProperty('componentName').uniq();
        break;
    }
    var component = App.StackServiceComponent.find(componentName);
    const excludeExclusiveDependencies = (d) => d.type !== 'exclusive';
    return component.missingDependencies(installedComponents, opt)
    .filter(excludeExclusiveDependencies) //If type is "exclusive" the dependent component should never be co-hosted.
    .map(function(componentDependency) {
      return componentDependency.chooseCompatible();
    });
  },

  /**
   * On click handler for custom command from items menu
   * @param context
   */
  executeCustomCommand: function(event) {
    var controller = this;
    var context = event.context;
    return App.showConfirmationPopup(function() {
      App.ajax.send({
        name : 'service.item.executeCustomCommand',
        sender: controller,
        data : {
          command : context.command,
          context : context.context || Em.I18n.t('services.service.actions.run.executeCustomCommand.context').format(context.command),
          hosts : context.hosts,
          serviceName : context.service,
          componentName : context.component
        },
        success : 'executeCustomCommandSuccessCallback',
        error : 'executeCustomCommandErrorCallback'
      });
    });
  },

  executeCustomCommandSuccessCallback  : function(data, ajaxOptions, params) {
    if (data.Requests.id) {
      App.router.get('userSettingsController').dataLoading('show_bg').done(function (initValue) {
        if (initValue) {
          App.router.get('backgroundOperationsController').showPopup();
        }
      });
    }
  },
  executeCustomCommandErrorCallback : function(data) {
    var error = Em.I18n.t('services.service.actions.run.executeCustomCommand.error');
    if(data && data.responseText){
      try {
        var json = $.parseJSON(data.responseText);
        error += json.message;
      } catch (err) {}
    }
    App.showAlertPopup(Em.I18n.t('services.service.actions.run.executeCustomCommand.error'), error);
  },

  /**
   * Call callback after loading service metrics
   * @param callback
   */
  isServiceMetricsLoaded: function(callback) {
    App.router.get('mainController').isLoading.call(App.router.get('clusterController'), 'isServiceContentFullyLoaded').done(callback);
  },

  setConfigsChangesForDisplay: function () {
    if (App.get('router.clusterController.isConfigsPropertiesLoaded')) {
      this.get('allPropertiesToChange').forEach(function (property) {
        var stackProperty = App.configsCollection.getConfigByName(property.propertyName, property.propertyFileName);
        if (stackProperty && (!stackProperty.isEditable || !stackProperty.isReconfigurable)) {
          this.get('requiredPropertiesToChange').pushObject(property);
        } else {
          Em.set(property, 'saveRecommended', true);
          this.get('recommendedPropertiesToChange').pushObject(property);
        }
      }, this);
      this.set('isConfigsLoadingInProgress', false);
      this.removeObserver('App.router.clusterController.isConfigsPropertiesLoaded', this, 'setConfigsChangesForDisplay');
    }
  },

  setConfigsChanges: function (groups) {
    this.get('groupedPropertiesToChange').pushObjects(groups);
    if (this.get('allPropertiesToChange.length')) {
      if (App.get('router.clusterController.isConfigsPropertiesLoaded')) {
        this.setConfigsChangesForDisplay();
      } else {
        this.addObserver('App.router.clusterController.isConfigsPropertiesLoaded', this, 'setConfigsChangesForDisplay');
      }
    } else {
      this.set('isConfigsLoadingInProgress', false);
    }
  },
  
  recoverHost: function() {
    var components = this.get('content.hostComponents');
    var hostName = this.get('content.hostName');
    var self = this;
    var batches = [
      {
        "order_id": 1,
        "type": "PUT",
        "uri": "/clusters/" + App.get('clusterName') + "/hosts/" + hostName + "/host_components",
        "RequestBodyInfo": {
          "RequestInfo": {
            context: Em.I18n.t('hosts.host.recover.initAllComponents.context'),
            operation_level: {
              level: "HOST",
              cluster_name: App.get('clusterName'),
              host_name: hostName
            },
            query: "HostRoles/component_name.in(" + components.mapProperty('componentName').join(',') + ")"
          },
          "Body": {
            HostRoles: {
              state: "INIT"
            }
          }
        }
    }];
    batches.push(
      {
        "order_id": 2,
        "type": "PUT",
        "uri": "/clusters/" + App.get('clusterName') + "/hosts/" + hostName + "/host_components",
        "RequestBodyInfo": {
          "RequestInfo": {
            context: Em.I18n.t('hosts.host.recover.installAllComponents.context'),
            operation_level: {
              level: "HOST",
              cluster_name: App.get('clusterName'),
              host_name: hostName
            },
            query: "HostRoles/component_name.in(" + components.mapProperty('componentName').join(',') + ")"
          },
          "Body": {
            HostRoles: {
              state: "INSTALLED"
            }
          }
        }
    });

    if(App.get('isKerberosEnabled')) {
      batches.push({
        "order_id": 3,
        "type": "PUT",
        "uri": "/clusters/" + App.get('clusterName'),
        "RequestBodyInfo": {
          "RequestInfo": {
            context: Em.I18n.t('hosts.host.recover.regenerateKeytabs.context'),
            query: "regenerate_keytabs=all&regenerate_hosts=" + hostName + "&config_update_policy=none",
          },
          "Body": {
            Clusters: {
             security_type: "KERBEROS"
            }
          }
        }
      });
    }
    App.get('router.mainAdminKerberosController').getSecurityType(function () {
      App.get('router.mainAdminKerberosController').getKDCSessionState(function () {
        self._doRecoverHost(batches);
      });
    });
  },

  _doRecoverHost: function (batches) {
    App.ajax.send ({
      name: 'common.batch.request_schedules',
      sender: this,
      data: {
        intervalTimeSeconds: 1,
        tolerateSize: 0,
        batches: batches
      },
      success:'recoverHostSuccessCallback',
      showLoadingPopup: true
    });
  },

  recoverHostSuccessCallback: function (data) {
    if (data && (data.Requests || data.resources[0].RequestSchedule)) {
      this.showBackgroundOperationsPopup();
      return true;
    } else {
      return false;
    }
  },

  recoverHostDisabled: function() {

    var isDisabled = false;
    var allowedStates = [App.HostComponentStatus.stopped, App.HostComponentStatus.install_failed, App.HostComponentStatus.init];
    this.get('content.hostComponents').forEach(function (component) {
      isDisabled = isDisabled ? true : !allowedStates.contains(component.get('workStatus'));
    });
    return isDisabled;
  }.property('content.hostComponents.@each.workStatus'),

  confirmRecoverHost: function() {
    var self = this;
    var componentsNotStopped = [];
    var allowedStates = [App.HostComponentStatus.stopped, App.HostComponentStatus.install_failed, App.HostComponentStatus.init];
    this.get('content.hostComponents').forEach(function (component) {
      if(!allowedStates.contains(component.get('workStatus'))) {
        componentsNotStopped.push(component.get('componentName'));
      }
    });
    if(componentsNotStopped.length) {
      App.ModalPopup.show({
        header: Em.I18n.t('hosts.recover.error.popup.title'),
        recoverErrorPopupBody: Em.I18n.t('hosts.recover.error.popup.body').format(componentsNotStopped.toString()),
        componentsStr: componentsNotStopped.toString(),
        bodyClass: Em.View.extend({
          templateName: require('templates/main/host/details/recoverHostErrorPopup')
        }),
        secondary: false
      });
    } else {
      App.ModalPopup.show({
        header: Em.I18n.t('hosts.recover.popup.title'),
        bodyClass: Em.View.extend({
          templateName: require('templates/main/host/details/recoverHostPopup')
        }),
        primary: Em.I18n.t('yes'),
        secondary: Em.I18n.t('no'),
        onPrimary: function () {
          self.recoverHost();
          this.hide();
        }
      });
    }
  },


  regenerateKeytabFileOperations: function () {
    var self = this;
    var hostName = this.content.get('hostName');
    var clusterName = App.get('clusterName');
    return App.showConfirmationPopup(function() {
      return App.ajax.send({
        name: "admin.kerberos_security.regenerate_keytabs.host",
        sender: self,
        data: {
          clusterName: clusterName,
          hostName: hostName
        },
        success: 'regenerateKeytabFileOperationsRequestSuccess',
        error: 'regenerateKeytabFileOperationsRequestError'
      });
    }, Em.I18n.t('question.sure.regenerateKeytab.host').format(hostName));
  },

  regenerateKeytabFileOperationsRequestSuccess: function(){
    App.router.get('backgroundOperationsController').showPopup();
  },

  regenerateKeytabFileOperationsRequestError: function () {
    App.showAlertPopup(Em.I18n.t('common.error'), Em.I18n.t('alerts.notifications.regenerateKeytab.host.error').format(this.content.get('hostName')));
  },

  /**
   * Returns URL parameters for configs request by certain tags
   * @param {object} data - object with desired configs tags received from API
   * @param {string[]} configTypes - list of config types
   * @returns {string}
   */
  getUrlParamsForConfigsRequest: function (data, configTypes) {
    return configTypes.map(type => {
      const tag = Em.get(data, `Clusters.desired_configs.${type}.tag`);
      return tag ? `(type=${type}&tag=${tag})` : null;
    }).compact().join('|');
  }

});
