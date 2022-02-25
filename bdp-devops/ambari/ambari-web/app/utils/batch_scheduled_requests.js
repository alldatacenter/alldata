/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

/**
 * Default success callback for ajax-requests in this module
 * @type {Function}
 */
var defaultSuccessCallback = function(data, ajaxOptions, params) {
  App.router.get('userSettingsController').dataLoading('show_bg').done(function(initValue) {
    params.query && params.query.set('status', 'SUCCESS');
    if (initValue) {
      App.router.get('backgroundOperationsController').showPopup();
    }
  });
};
/**
 * Default error callback for ajax-requests in this module
 * @param {Object} xhr
 * @param {String} textStatus
 * @param {String} error
 * @param {Object} opt
 * @param {Object} params
 * @type {Function}
 */
var defaultErrorCallback = function(xhr, textStatus, error, opt, params) {
  params.query && params.query.set('status', 'FAIL');
  App.ajax.defaultErrorHandler(xhr, opt.url, 'POST', xhr.status);
};

/**
 * Contains helpful utilities for handling batch and scheduled requests.
 */
module.exports = {

  /**
   * Some services have components which have a need for rolling restarts. This
   * method returns the name of the host-component which supports rolling
   * restarts for a service.
   * @param {String} serviceName
   */
  getRollingRestartComponentName: function(serviceName) {
    var rollingRestartComponents = {
      HDFS: 'DATANODE',
      YARN: 'NODEMANAGER',
      HBASE: 'HBASE_REGIONSERVER',
      STORM: 'SUPERVISOR'
    };
    return rollingRestartComponents[serviceName] ? rollingRestartComponents[serviceName] : null;
  },

  /**
   * Facade-function for restarting host components of specific service
   * @param {String} serviceDisplayName for which service hostComponents should be restarted
   * @param {String} serviceName for which service hostComponents should be restarted
   * @param {bool} staleConfigsOnly restart only hostComponents with <code>staleConfig</code> true
   * @param {Object} query
   * @param {bool} runMmOperation
   */
  restartAllServiceHostComponents: function(serviceDisplayName, serviceName, staleConfigsOnly, query, runMmOperation) {
    const context = staleConfigsOnly ?
      Em.I18n.t('rollingrestart.context.allWithStaleConfigsForSelectedService').format(serviceDisplayName) :
      Em.I18n.t('rollingrestart.context.allForSelectedService').format(serviceDisplayName);

    if (runMmOperation) {
      this.turnOnOffPassiveRequest('ON', Em.I18n.t('passiveState.turnOnFor').format(serviceName), serviceName);
    }
    this.getComponentsFromServer({
      services: [serviceName],
      staleConfigs: staleConfigsOnly ? staleConfigsOnly : null,
      passiveState: 'OFF',
      displayParams: ['host_components/HostRoles/component_name']
    }, data => {
      const hostComponents = this.getRestartComponentsArray(data);
      this.restartHostComponents(hostComponents, context, 'SERVICE', query);
    });
  },

  restartCertainServiceHostComponents: function (serviceName, components, hosts, label, query, runMmOperation) {
    const context = Em.I18n.t('rollingrestart.context.allForSelectedService').format(label);
    if (runMmOperation) {
      this.turnOnOffPassiveRequest('ON', Em.I18n.t('passiveState.turnOnFor').format(serviceName), serviceName);
    }
    this.getComponentsFromServer({
      services: serviceName && [serviceName],
      hosts,
      components,
      passiveState: 'OFF',
      displayParams: ['host_components/HostRoles/component_name']
    }, data => {
      const hostComponents = this.getRestartComponentsArray(data);
      this.restartHostComponents(hostComponents, context, 'SERVICE', query);
    });
  },

  getRestartComponentsArray: function (data) {
    let hostComponents = [];
    data.items.forEach(host => {
      host.host_components.forEach(hostComponent => {
        hostComponents.push(Em.Object.create({
          componentName: hostComponent.HostRoles.component_name,
          hostName: host.Hosts.host_name
        }));
      });
    });
    return hostComponents;
  },

  /**
   * construct URL from parameters for request in <code>getComponentsFromServer()</code>
   * @param {object} options
   * @return {{fields: string, params: string}}
   */
  constructComponentsCallUrl: function (options) {
    var multipleValueParams = {
        'services': 'host_components/HostRoles/service_name.in(<entity-names>)',
        'hosts': 'Hosts/host_name.in(<entity-names>)',
        'components': 'host_components/HostRoles/component_name.in(<entity-names>)'
      },
      singleValueParams = {
        staleConfigs: 'host_components/HostRoles/stale_configs=',
        passiveState: 'Hosts/maintenance_state=',
        workStatus: 'host_components/HostRoles/state='
      },
      displayParams = options.displayParams || [],
      urlParams = '',
      addAmpersand = false;

    for (var i in multipleValueParams) {
      var arrayParams = options[i];
      if (Array.isArray(arrayParams) && arrayParams.length > 0) {
        if (addAmpersand) {
          urlParams += '&';
          addAmpersand = false;
        }
        urlParams += multipleValueParams[i].replace('<entity-names>', arrayParams.join(','));
        addAmpersand = true;
      }
    }

    for (var j in singleValueParams) {
      var param = options[j];
      if (!Em.isNone(param)) {
        urlParams += (addAmpersand) ? '&' : '';
        urlParams += singleValueParams[j] + param.toString();
        addAmpersand = true;
      }
    }
    var params = urlParams,
      fields = '';
    displayParams.forEach(function (displayParam, index, array) {
      if (index === 0) {
        fields += (addAmpersand) ? '&' : '';
        fields += 'fields=';
      }
      fields += displayParam;
      fields += (array.length === (index + 1)) ? '' : ",";
    });
    fields += '&minimal_response=true';

    return {fields: fields.substring(1, fields.length), params: params};
  },

  /**
   * make GET call to server in order to obtain host-components
   * which correspond to filter params from <code>options</code>
   * @param options
   * @param callback
   */
  getComponentsFromServer: function (options, callback) {
    var request_parameters = this.constructComponentsCallUrl(options);
    return App.ajax.send({
      name: 'host.host_components.filtered',
      sender: this,
      data: {
        parameters: request_parameters.params,
        fields: request_parameters.fields,
        callback: callback
      },
      success: 'getComponentsFromServerSuccessCallback'
    });
  },

  /**
   * pass request outcome to <code>callback()<code>
   * @param data
   * @param opt
   * @param params
   */
  getComponentsFromServerSuccessCallback: function (data, opt, params) {
    params.callback(data);
  },

  /**
   * Restart list of host components
   * @param {Ember.Enumerable} hostComponentsList list of host components should be restarted
   * @param {String} context message to show in BG popup
   * @param {String} level - operation level, can be ("CLUSTER", "SERVICE", "HOST", "HOSTCOMPONENT")
   * @param {String} query
   */
  restartHostComponents: function (hostComponentsList, context, level, query) {
    context = context || Em.I18n.t('rollingrestart.context.default');
    /**
     * Format: {
     *  'DATANODE': ['host1', 'host2'],
     *  'NAMENODE': ['host1', 'host3']
     *  ...
     * }
     */
    var componentToHostsMap = {};
    var hosts = [];
    hostComponentsList.forEach(function(hc) {
      var hostName = hc.get('hostName');
      var componentName = hc.get('componentName');
      if (!componentToHostsMap[componentName]) {
        componentToHostsMap[componentName] = [];
      }
      componentToHostsMap[componentName].push(hostName);
      hosts.push(hostName);
    });
    var resource_filters = [];
    for (var componentName in componentToHostsMap) {
      if (componentToHostsMap.hasOwnProperty(componentName)) {
        resource_filters.push({
          service_name: App.StackServiceComponent.find(componentName).get('serviceName'),
          component_name: componentName,
          hosts: componentToHostsMap[componentName].join(",")
        });
      }
    }
    if (hostComponentsList.length > 0) {
      var serviceComponentName = hostComponentsList[0].get("componentName");
      var serviceName = App.StackServiceComponent.find(serviceComponentName).get('serviceName');
      var operation_level = this.getOperationLevelObject(level, serviceName,
        serviceComponentName);
    }


    if (resource_filters.length) {
      App.ajax.send({
        name: 'restart.hostComponents',
        sender: {
          successCallback: defaultSuccessCallback,
          errorCallback: defaultErrorCallback
        },
        data: {
          context: context,
          resource_filters: resource_filters,
          query: query,
          operation_level: operation_level
        },
        success: 'successCallback',
        error: 'errorCallback',
        showLoadingPopup: true
      });
    }
  },

  /**
   * @param {String} level - operation level name, can be ("CLUSTER", "SERVICE", "HOST", "HOSTCOMPONENT")
   * @param {String} serviceName
   * @param {String} componentName
   * @returns {Object} {{level: *, cluster_name: *}} - operation level object
   * @method getOperationLevelObject - create operation level object to be included into ajax query
   */
  getOperationLevelObject: function(level, serviceName, componentName) {
    var operationLevel = {
      "level": level,
      "cluster_name": App.get("clusterName")
    };
    if (level === "SERVICE") {
      operationLevel["service_name"] = serviceName;
    }
    else {
      if (level !== "HOST") {
        operationLevel["service_name"] = serviceName;
        operationLevel["hostcomponent_name"] = componentName;
      }
    }
    return operationLevel;
  },

  turnOnOffPassiveRequest: function(state, message, serviceName, callback) {
    return App.ajax.send({
      'name': 'common.service.passive',
      'sender': {
        'successCallback': callback || defaultSuccessCallback,
        'errorCallback': defaultErrorCallback
      },
      'data': {
        'requestInfo': message,
        'serviceName': serviceName,
        'passive_state': state
      },
      'success': 'successCallback'
    });
  },

  /**
   * Makes a REST call to the server requesting the rolling restart of the
   * provided host components.
   * @param {Array} restartHostComponents list of host components should be restarted
   * @param {Number} batchSize size of each batch
   * @param {Number} intervalTimeSeconds delay between two batches
   * @param {Number} tolerateSize task failure tolerance
   * @param {callback} [successCallback]
   * @param {callback} [errorCallback]
   */
  _doPostBatchRollingRestartRequest: function(restartHostComponents, batchSize, intervalTimeSeconds, tolerateSize, successCallback, errorCallback) {
    successCallback = successCallback || defaultSuccessCallback;
    errorCallback = errorCallback || defaultErrorCallback;

    if (!restartHostComponents.length) {
      return;
    }
    return App.ajax.send({
      name: 'common.batch.request_schedules',
      sender: {
        successCallback: successCallback,
        errorCallback: errorCallback
      },
      data: {
        intervalTimeSeconds: intervalTimeSeconds,
        tolerateSize: tolerateSize,
        batches: this.getBatchesForRollingRestartRequest(restartHostComponents, batchSize)
      },
      success: 'successCallback',
      error: 'errorCallback',
      showLoadingPopup: true
    });
  },

  /**
   * Create list of batches for rolling restart request
   * @param {Array} restartHostComponents list host components should be restarted
   * @param {Number} batchSize size of each batch
   * @returns {Array} list of batches
   */
  getBatchesForRollingRestartRequest: function(restartHostComponents, batchSize, previousOrderId) {
    var hostIndex = 0,
        batches = [],
        batchCount = Math.ceil(restartHostComponents.length / batchSize),
        sampleHostComponent = restartHostComponents.objectAt(0),
        componentName = sampleHostComponent.get('componentName'),
        serviceName = sampleHostComponent.get('serviceName') || sampleHostComponent.get('service.serviceName');
        previousOrderId = previousOrderId || 0;
    for ( var count = 0; count < batchCount; count++) {
      var hostNames = [];
      for ( var hc = 0; hc < batchSize && hostIndex < restartHostComponents.length; hc++) {
        hostNames.push(restartHostComponents.objectAt(hostIndex++).get('hostName'));
      }
      if (hostNames.length) {
        batches.push({
          "order_id" : previousOrderId + count + 1,
          "type" : "POST",
          "uri" : "/clusters/" + App.get('clusterName') + "/requests",
          "RequestBodyInfo" : {
            "RequestInfo" : {
              "context" : "_PARSE_.ROLLING-RESTART." + componentName + "." + (count + 1) + "." + batchCount,
              "command" : "RESTART"
            },
            "Requests/resource_filters": [{
              "service_name" : serviceName,
              "component_name" : componentName,
              "hosts" : hostNames.join(",")
            }]
          }
        });
      }
    }
    return batches;
  },

  /**
   * Launches dialog to handle rolling restarts of host components.
   *
   * Rolling restart is supported only for components listed in <code>getRollingRestartComponentName</code>
   * @see getRollingRestartComponentName
   * @param {String} hostComponentName
   *           Type of host-component to restart across cluster
   *          (ex: DATANODE)
   * @param {bool} staleConfigsOnly
   *           Pre-select host-components which have stale
   *          configurations
   */
  launchHostComponentRollingRestart: function(hostComponentName, serviceName, isMaintenanceModeOn, staleConfigsOnly, skipMaintenance) {
    if (App.get('components.rollinRestartAllowed').contains(hostComponentName)) {
      return this.showRollingRestartPopup(hostComponentName, serviceName, isMaintenanceModeOn, staleConfigsOnly, null, skipMaintenance);
    }
    return this.showWarningRollingRestartPopup(hostComponentName);
  },

  /**
   * Show popup with rolling restart dialog
   * @param {String} hostComponentName name of the host components that should be restarted
   * @param {String} serviceName
   * @param {boolean} isMaintenanceModeOn
   * @param {bool} staleConfigsOnly restart only components with <code>staleConfigs</code> = true
   * @param {App.hostComponent[]} [hostComponents] list of hostComponents that should be restarted
   * @param {boolean} [skipMaintenance]
   * Using this parameter will reset hostComponentName
   */
  showRollingRestartPopup: function(hostComponentName, serviceName, isMaintenanceModeOn, staleConfigsOnly, hostComponents, skipMaintenance) {
    hostComponents = hostComponents || [];
    var componentDisplayName = App.format.role(hostComponentName, false);
    var self = this;
    if (!componentDisplayName) {
      componentDisplayName = hostComponentName;
    }
    var title = Em.I18n.t('rollingrestart.dialog.title').format(componentDisplayName);
    var viewExtend = {
      turnOnMmMsg: Em.I18n.t('passiveState.turnOnFor').format(serviceName),
      turnOnMm: false,
      staleConfigsOnly : staleConfigsOnly,
      hostComponentName : hostComponentName,
      skipMaintenance: skipMaintenance,
      serviceName: serviceName,
      isServiceInMM: isMaintenanceModeOn,
      didInsertElement: function () {
        var view = this;

        this.set('parentView.innerView', this);
        if (hostComponents.length) {
          view.initialize();
          return;
        }

        self.getComponentsFromServer({
          components: [hostComponentName],
          displayParams: ['host_components/HostRoles/stale_configs', 'Hosts/maintenance_state', 'host_components/HostRoles/maintenance_state'],
          staleConfigs: staleConfigsOnly ? staleConfigsOnly : null
        }, function (data) {
          var wrappedHostComponents = [];
          data.items.forEach(function (host) {
            host.host_components.forEach(function(hostComponent) {
              wrappedHostComponents.push(Em.Object.create({
                componentName: hostComponent.HostRoles.component_name,
                serviceName: App.StackServiceComponent.find(hostComponent.HostRoles.component_name).get('serviceName'),
                hostName: host.Hosts.host_name,
                staleConfigs: hostComponent.HostRoles.stale_configs,
                hostPassiveState: host.Hosts.maintenance_state,
                passiveState: hostComponent.HostRoles.maintenance_state
              }));
            });
          });
          view.set('allHostComponents', wrappedHostComponents);
          view.initialize();
        });
      }
    };
    if (hostComponents.length) {
      viewExtend.allHostComponents = hostComponents;
    }

    App.ModalPopup.show({
      header : title,
      hostComponentName : hostComponentName,
      serviceName: serviceName,
      isServiceInMM: isMaintenanceModeOn,
      staleConfigsOnly : staleConfigsOnly,
      skipMaintenance: skipMaintenance,
      innerView : null,
      bodyClass : App.RollingRestartView.extend(viewExtend),
      classNames : [ 'rolling-restart-popup' ],
      primary : Em.I18n.t('rollingrestart.dialog.primary'),
      onPrimary : function() {
        var dialog = this;
        var restartComponents = this.get('innerView.restartHostComponents');
        var batchSize = this.get('innerView.batchSize');
        var waitTime = this.get('innerView.interBatchWaitTimeSeconds');
        var tolerateSize = this.get('innerView.tolerateSize');
        if (this.get('innerView.turnOnMm')) {
          self.turnOnOffPassiveRequest('ON', Em.I18n.t('passiveState.turnOnFor').format(serviceName), serviceName.toUpperCase());
        }
        self._doPostBatchRollingRestartRequest(restartComponents, batchSize, waitTime, tolerateSize, function(data, ajaxOptions, params) {
          dialog.hide();
          defaultSuccessCallback(data, ajaxOptions, params);
        });
      },
      updateButtons : function() {
        var errors = this.get('innerView.errors');
        this.set('disablePrimary', (errors != null && errors.length > 0))
      }.observes('innerView.errors')
    });
  },

  /**
   * Service rolling restart popup
   * @param {String} serviceName: name of the service that should be restarted
   * @param {function} mastersForRestart: Callback function to retrieve master components for restart
   * @param {function} workersForRestart: Callback function to retrieve worker components for restart
   */
  showServiceRestartPopup: function (serviceName, mastersForRestart, workersForRestart) {

    let self = this;

    let viewExtend = {

      masterComponents: undefined,

      slaveComponents: undefined,

      didInsertElement: function() {
        this.set('parentView.innerView', this);
        if (mastersForRestart) this.set('masterComponents', mastersForRestart(serviceName));
        if (workersForRestart) {
          this.set('slaveComponents', workersForRestart(serviceName));
          this.set('componentHostRackInfoMap', this.componentHostRackInfo());
        }
        this.initDefaultConfigs();
      },

      componentHostRackInfo: function () {
        const slaveComponents = this.get('slaveComponents');
        const componentNames = slaveComponents.mapProperty('componentName').uniq();
        const hostModel = App.Host.find();
        let componentRackInfo = {};
        componentNames.forEach((component) => {
          let hostNames = slaveComponents.filterProperty('componentName', component).mapProperty('hostName');
          let hostRackInfo = {};
          hostNames.forEach((hostName) => {
            hostRackInfo[hostName] = hostModel.findProperty('hostName', hostName).get('rack');
          });
          componentRackInfo[component] = hostRackInfo;
        });
        return componentRackInfo;
      }
    };

    App.ModalPopup.show({
      header: Em.I18n.t('common.configure.restart'),
      bodyClass: App.ServiceRestartView.extend(viewExtend),
      primary: Em.I18n.t('common.restart'),
      primaryClass: 'btn-warning',
      classNames: ['common-modal-wrapper'],
      modalDialogClasses: ['modal-lg'],

      onPrimary: function () {
        const isRollingRestart = this.get('innerView.useRolling');
        if (isRollingRestart) {
          //let rollingRestartConfigs = this.get('innerView').getRestartConfigs();
          //restart configs will be applied when we have the BE capability to override them in individual batches.
          const masterComps = this.get('innerView.masterComponents');
          let masterBatch = [];
          let orderId = 0;
          if (!!masterComps) {
            let batchesForRestart = self.rollingRestartMastersBatches(masterComps, serviceName);
            masterBatch.push(...batchesForRestart);
            orderId += batchesForRestart.length;
          }

          const slaveComps = this.get('innerView.slaveComponents');
          let workersBatch = [];
          if (!!slaveComps) {
            const componentNames = slaveComps.mapProperty('componentName').uniq();
            componentNames.forEach((component) => {
              let restartComponent = slaveComps.filterProperty('componentName', component);
              let batchSize = this.get('innerView').getNoOfHosts(component);
              let batchesForRestart = self.getBatchesForRollingRestartRequest(restartComponent, batchSize, orderId);
              workersBatch.push(...batchesForRestart);
              orderId += batchesForRestart.length;
            });
          }
          self.sendRollingRestartRequest(masterBatch, workersBatch);
        } else {
          const query = Em.Object.create({status: 'INIT'});
          const serviceDisplayName = App.Service.find().findProperty('serviceName', serviceName).get('displayName');
          self.restartAllServiceHostComponents(serviceDisplayName, serviceName, false, query, false);
        }
        this._super();
      }
    })
  },


  /**
   * Creates batches for masters to be used for rolling restart.
   * @param {App.hostComponent[]} [hostComponents] list of hostComponents that should be restarted
   * @param {String} serviceName: Name of the service to be restarted.
   */

  rollingRestartMastersBatches: function (hostComponents, serviceName) {
    let batches = [];
    for (let i = 0; i < hostComponents.length; i++) {
      const hostName = hostComponents[i].get('hostName');
      const component = hostComponents[i].get('componentName');
      const context = "RESTART " + hostComponents[i].get('displayName');
      batches.push({
        "order_id": i + 1,
        "type": 'POST',
        "uri": "/clusters/" + App.get('clusterName') + "/requests/",
        "RequestBodyInfo": {
          "RequestInfo": {
            "command": "RESTART",
            "context": context,
          },
          "Requests/resource_filters": [{
            "service_name": serviceName,
            "component_name": component,
            "hosts": hostName
          }]
        }
      })
    }
    return batches;
  },

  sendRollingRestartRequest: function (mastersBatch, workersBatch) {

    let batches = [];
    if (mastersBatch && mastersBatch.length > 0) batches.push(...mastersBatch);
    if (workersBatch && workersBatch.length > 0) batches.push(...workersBatch);

    if (batches.length > 0) {
      App.ajax.send({
        name: 'common.batch.request_schedules',
        sender: this,
        data: {
          intervalTimeSeconds: 1,
          tolerateSize: 0,
          batches: batches
        },
        success: 'serviceRestartSuccess',
        showLoadingPopup: true
      });
    }
  },

  /**
   * Callback function for restartMastersBatchRequest that shows BG Modal if restart request sent successfully
   * TODO replace it with a progress view that shows rolling restart tasks
   */
  serviceRestartSuccess: function (data) {
    if (data && (data.Requests || data.resources[0].RequestSchedule)) {
      App.router.get('userSettingsController').dataLoading('show_bg').done(function (initValue) {
        if (initValue) {
          App.router.get('backgroundOperationsController').showPopup();
        }
        if (typeof callback === 'function') {
          callback();
        }
      });
      return true;
    } else {
      return false;
    }
  },

  /**
   * Show warning popup about not supported host components
   * @param {String} hostComponentName
   */
  showWarningRollingRestartPopup: function(hostComponentName) {
    var componentDisplayName = App.format.role(hostComponentName, false) || hostComponentName;
    var title = Em.I18n.t('rollingrestart.dialog.title').format(componentDisplayName);
    var msg = Em.I18n.t('rollingrestart.notsupported.hostComponent').format(componentDisplayName);

    return App.ModalPopup.show({
      header : title,
      secondary : false,
      msg : msg,
      bodyClass : Em.View.extend({
        template : Em.Handlebars.compile('<div class="alert alert-warning">{{msg}}</div>')
      })
    });
  },

  /**
   * Warn user that alerts will be updated in few minutes
   * @param {String} passiveState
   */
  infoPassiveState: function(passiveState) {
    var enabled = passiveState == 'OFF' ? 'enabled' : 'suppressed';
    App.ModalPopup.show({
      header: Em.I18n.t('common.information'),
      secondary: null,
      bodyClass: Em.View.extend({
        template: Em.Handlebars.compile('<p>{{view.message}}</p>'),
        message: Em.I18n.t('hostPopup.warning.alertsTimeOut').format(passiveState.toLowerCase(), enabled)
      })
    });
  },

  /**
   * Retrieves the latest information about a specific request schedule
   * identified by 'requestScheduleId'
   *
   * @param {Number} requestScheduleId ID of the request schedule to get
   * @param {Function} successCallback Called with request_schedule data from server. An
   *          empty object returned for invalid ID.
   * @param {Function} errorCallback Optional error callback. Default behavior is to
   *          popup default error dialog.
   */
  getRequestSchedule: function(requestScheduleId, successCallback, errorCallback) {
    if (requestScheduleId != null && !isNaN(requestScheduleId) && requestScheduleId > -1) {
      errorCallback = errorCallback || defaultErrorCallback;
      App.ajax.send({
        name : 'request_schedule.get',
        sender : {
          successCallbackFunction: successCallback,
          errorCallbackFunction: errorCallback
        },
        data : {
          request_schedule_id : requestScheduleId
        },
        success : 'successCallbackFunction',
        error : 'errorCallbackFunction'
      });
    } else {
      successCallback({});
    }
  },

  /**
   * Attempts to abort a specific request schedule identified by 'requestScheduleId'
   *
   * @param {Number} requestScheduleId ID of the request schedule to get
   * @param {Function} successCallback Called when request schedule successfully aborted
   * @param {Function} errorCallback Optional error callback. Default behavior is to
   *          popup default error dialog.
   */
  doAbortRequestSchedule: function(requestScheduleId, successCallback, errorCallback) {
    if (requestScheduleId != null && !isNaN(requestScheduleId) && requestScheduleId > -1) {
      errorCallback = errorCallback || defaultErrorCallback;
      App.ajax.send({
        name : 'common.delete.request_schedule',
        sender : {
          successCallbackFunction: successCallback,
          errorCallbackFunction: errorCallback
        },
        data : {
          request_schedule_id : requestScheduleId
        },
        success : 'successCallbackFunction',
        error : 'errorCallbackFunction'
      });
    } else {
      successCallback({});
    }
  }
};
