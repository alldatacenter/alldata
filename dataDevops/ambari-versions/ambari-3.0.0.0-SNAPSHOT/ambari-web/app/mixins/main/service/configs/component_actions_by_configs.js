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
var stringUtils = require('utils/string_utils');

/**
 * Mixin with methods for component actions that needs to be done when a config with specific value is saved
 * Used in the service config controller
 * @type {Em.Mixin}
 */
App.ComponentActionsByConfigs = Em.Mixin.create({

  configAction: null,

  /**
   * Flag to check if Yarn Queues have been refreshed already
   */
  isYarnQueueRefreshed: false,

  /**
   * Do component add/delete actions as inferred from value of service configs
   * @public
   * @method doConfigActions
   */
  doConfigActions: function () {
    var serviceConfigs = this.get('stepConfigs').findProperty('serviceName', this.get('content.serviceName')).get('configs');
    var configActionComponents = serviceConfigs.filterProperty('configActionComponent');
    this.isYarnQueueRefreshed = false;
    this.doComponentDeleteActions(configActionComponents);
    this.doComponentAddActions(configActionComponents);
    this.showPopup();
  },

  /**
   * Method to show confirmation popup before sending an ajax request
   */
  showPopup: function() {
    var config_actions = App.ConfigAction.find().filterProperty('actionType', 'showPopup');
    var self = this;
    if (config_actions.length) {
      config_actions.forEach(function (config_action) {
        var configs = self.get('allConfigs').filterProperty('filename', config_action.get('fileName')).filter(function (item) {
          return item.get('value') !== item.get('initialValue');
        });

        if (configs.length) {
          var hostComponents = App.HostComponent.find();
          if (config_action.get('fileName') === 'capacity-scheduler.xml' && !self.isYarnQueueRefreshed) {
            var isRMRunning = hostComponents.some(function (component) {
              return component.get('componentName') === 'RESOURCEMANAGER' && component.get('isRunning');
            });
            if (isRMRunning) {
              var hsiInstance = hostComponents.filterProperty('componentName', 'HIVE_SERVER_INTERACTIVE');
              if (self.get('content.serviceName') === 'HIVE') {
                // Auto refresh yarn capacity scheduler if capacity-scheduler configs are changed from Hive configs page
                self.popupPrimaryButtonCallback(config_action);
                // Show a popup to restart HSI if HSI is enabled
                if (hsiInstance.length > 0) {
                  self.showHsiRestartPopup(hsiInstance);
                }
              } else {
                self.configAction = config_action;
                var body = config_action.get('popupProperties').body;
                if (config_action.get('popupProperties').hasOwnProperty('conditionalWarning') && config_action.get('popupProperties').conditionalWarning === true) {
                  // Check if Hive Server 2 Interactive is enabled and show a warning message if it is enabled
                  if (hsiInstance.length > 0) {
                    body += "<br/><br/>" + config_action.get('popupProperties').warningMessage;
                  }
                }
                App.showConfirmationPopup(function () {
                  self.popupPrimaryButtonCallback(config_action);
                }, body, null, Em.I18n.t('popup.confirmation.commonHeader'), config_action.get('popupProperties').primaryButton.label, 'success', 'refresh_yarn_queues')
              }
            }
          }
        }
      });
    }
  },

  showHsiRestartPopup: function (components) {
    var self = this;
    App.showConfirmationPopup(function () {
      self.hsiRestartPopupPrimaryButtonCallback(components);
    }, Em.I18n.t('popup.confirmation.hsiRestart.body'), null, Em.I18n.t('popup.confirmation.commonHeader'), Em.I18n.t('popup.confirmation.hsiRestart.buttonText'), 'success', 'restart_hsi')
  },

  hsiRestartPopupPrimaryButtonCallback: function (components) {
    batchUtils.restartHostComponents(components, Em.I18n.t('rollingrestart.context.selectedComponentOnSelectedHost').format(components[0].get('displayName')), "HOST_COMPONENT");
  },

  popupPrimaryButtonCallback: function (config_action) {
    var hosts = App.Service.find(config_action.get('serviceName')).get('hostComponents').filterProperty('componentName', config_action.get('componentName')).mapProperty('hostName');
    var self = this;
    App.ajax.send({
      name : config_action.get('popupProperties').primaryButton.metaData.name,
      sender: self,
      data : {
        command : config_action.get('popupProperties').primaryButton.metaData.command,
        context : config_action.get('popupProperties').primaryButton.metaData.context,
        hosts : hosts.join(','),
        serviceName : config_action.get('serviceName'),
        componentName : config_action.get('componentName'),
        forceRefreshConfigTags : config_action.get('configName')
      },
      error : 'popupPrimaryButtonCallbackError'
    });
  },

  popupPrimaryButtonCallbackError: function(data) {
    var error = this.configAction.get('popupProperties').errorMessage;
    if(data && data.responseText){
      try {
        var json = $.parseJSON(data.responseText);
        error += json.message;
      } catch (err) {}
    }
    App.showAlertPopup(this.configAction.get('popupProperties').errorMessage, error, null);
  },
  
  /**
   * Method informs if any component will be added/deleted on saving configurations
   * @return {boolean}
   * @public
   * @method isComponentActionsPresent
   */
  isComponentActionsPresent: function () {
    var serviceConfigs = this.get('stepConfigs').findProperty('serviceName', this.get('content.serviceName')).get('configs');
    var configActionComponents = serviceConfigs.filterProperty('configActionComponent');
    return !!(this.getComponentsToDelete(configActionComponents).length + this.getComponentsToAdd(configActionComponents).length);
  },

  /**
   * Get Component that will be deleted on saving configurations
   * @param configActionComponents {Object}
   * @return {Array}
   * @private
   * @method getComponentsToDelete
   */
  getComponentsToDelete: function (configActionComponents) {
    const hostComponents = App.HostComponent.find();
    const componentsToDelete = [];

    configActionComponents.filterProperty('configActionComponent.action', 'delete')
      .map((item) => item.configActionComponent)
      .forEach(function (_componentToDelete) {
      const installedHosts = hostComponents.filterProperty('componentName', _componentToDelete.componentName).mapProperty('hostName');
      _componentToDelete.hostNames.forEach((hostsToDelete) => {
        if (installedHosts.contains(hostsToDelete)) {
          componentsToDelete.push({
            componentName: _componentToDelete.componentName,
            hostName: hostsToDelete,
            isClient: _componentToDelete.isClient
          })
        }
      });
    }, this);
    return componentsToDelete;
  },

  /**
   * Get Component that will be added on saving configurations
   * @param configActionComponents {Object}
   * @return {array}
   * @private
   * @method getComponentsToDelete
   */
  getComponentsToAdd: function (configActionComponents) {
    const componentsToAdd = [];

    configActionComponents
      .filterProperty('configActionComponent.action', 'add')
      .map((item) => item.configActionComponent)
      .forEach(function (_componentToAdd) {
      const serviceNameForComponent = App.StackServiceComponent.find(_componentToAdd.componentName).get('serviceName');
      let hostsToInstall = _componentToAdd.hostNames;

      App.Service.find(serviceNameForComponent)
        .get('hostComponents').filterProperty('componentName', _componentToAdd.componentName).mapProperty('hostName')
        .forEach((installedHost) => {
          // List of host components to be added should not include ones that are already present in the cluster.
          hostsToInstall = _componentToAdd.hostNames.without(installedHost);
        });
      hostsToInstall.forEach((hostToInstall) => {
        componentsToAdd.push({
          componentName: _componentToAdd.componentName,
          hostName: hostToInstall,
          isClient: _componentToAdd.isClient
        });
      });
    }, this);
    return componentsToAdd;
  },

  /**
   * Do component Delete actions as inferred from value of service config
   * @param configActionComponents Object[]
   * @private
   * @method {configActionComponents}
   */
  doComponentDeleteActions: function (configActionComponents) {
    var componentsToDelete = this.getComponentsToDelete(configActionComponents);
    if (componentsToDelete.length) {
      componentsToDelete.forEach((componentToDelete) => {
        const componentName = componentToDelete.componentName;
        const hostName = componentToDelete.hostName;
        const displayName = App.StackServiceComponent.find(componentName).get('displayName');
        const context = Em.I18n.t('requestInfo.stop').format(displayName);
        const batches = [];

        this.setRefreshYarnQueueRequest(batches);
        batches.push(this.getInstallHostComponentsRequest(hostName, componentName, context));
        batches.push(this.getDeleteHostComponentRequest(hostName, componentName));
        this.setOrderIdForBatches(batches);
        App.ajax.send({
          name: 'common.batch.request_schedules',
          sender: {
            checkIfComponentWasDeleted: this.checkIfComponentWasDeleted
          },
          data: {
            intervalTimeSeconds: 60,
            tolerateSize: 0,
            batches: batches,
            displayName: displayName,
            hostName: hostName
          },
          success: 'checkIfComponentWasDeleted'
        });
      });
    }
  },

  checkIfComponentWasDeleted: function (resp, req, data) {
    var scheduleId = resp.resources[0].RequestSchedule.id;
    var self = this;
    setTimeout(function () {
      batchUtils.getRequestSchedule(scheduleId, function (resp) {
        var lastStatus = resp.RequestSchedule.last_execution_status;
        var status = resp.RequestSchedule.status;
        if (lastStatus === 'FAILED' || status === 'FAILED') {
          App.showAlertPopup(
            Em.I18n.t('hosts.bulkOperation.delete.component.failed.header'),
            Em.I18n.t('hosts.bulkOperation.delete.component.failed.body').format(data.displayName, data.hostName));
        }
      }, function () {});
    }, data.batches.length * 60000)
  },

  /**
   * Do component Add actions as inferred from value of service config
   * @param configActionComponents Object[]
   * @private
   * @method {doComponentAddActions}
   */
  doComponentAddActions: function (configActionComponents) {
    var componentsToAdd = this.getComponentsToAdd(configActionComponents);

    if (componentsToAdd.length) {
      var allComponentsToAdd = componentsToAdd.concat(this.getDependentComponents(componentsToAdd));
      var allComponentsToAddHosts = allComponentsToAdd.mapProperty('hostName').uniq();

      allComponentsToAddHosts.forEach(function (_hostName) {
        var hostComponents = allComponentsToAdd.filterProperty('hostName', _hostName).mapProperty('componentName').uniq();
        var masterHostComponents = allComponentsToAdd.filterProperty('hostName', _hostName).filterProperty('isClient', false).mapProperty('componentName').uniq();
        var displayNames = masterHostComponents.map(function (item) {
          return App.StackServiceComponent.find().findProperty('componentName', item).get('displayName');
        });

        var displayStr = stringUtils.getFormattedStringFromArray(displayNames);
        var context = Em.I18n.t('requestInfo.start').format(displayStr);
        var batches = [];
        this.setCreateComponentRequest(batches, hostComponents);
        batches.push(this.getCreateHostComponentsRequest(_hostName, hostComponents));
        batches.push(this.getInstallHostComponentsRequest(_hostName, hostComponents));
        this.setRefreshYarnQueueRequest(batches);
        batches.push(this.getStartHostComponentsRequest(_hostName, masterHostComponents, context));
        this.setOrderIdForBatches(batches);

        App.ajax.send({
          name: 'common.batch.request_schedules',
          sender: this,
          data: {
            intervalTimeSeconds: 1,
            tolerateSize: 0,
            batches: batches
          }
        });
      }, this);
    }
  },

  /**
   * @method getDependentComponents
   * @param {Array} componentsToAdd
   * @returns {Array}
   */
  getDependentComponents: function(componentsToAdd) {
    var dependentComponents = [];

    componentsToAdd.forEach(function (_component) {
      var componentToAdd = App.StackServiceComponent.find(_component.componentName);
      var installedComponents = App.HostComponent.find().filterProperty('hostName', _component.hostName).mapProperty('componentName').uniq();
      var dependencies = componentToAdd.missingDependencies(installedComponents, {'scope': 'host'}).map(function (_dependency) {
        return {
          componentName: _dependency.chooseCompatible(),
          hostName: _component.hostName,
          isClient: App.StackServiceComponent.find(_dependency.componentName).get('isClient')
        }
      }, this);
      var dependenciesToInstall = dependencies.filter(function (_dependencyToAdd) {
        var isInstalled = App.HostComponent.find().filterProperty('componentName', _dependencyToAdd.componentName).someProperty('hostName', _dependencyToAdd.hostName);
        var isAddedToInstall = dependentComponents.filterProperty('componentName', _dependencyToAdd.componentName).someProperty('hostName', _dependencyToAdd.hostName);
        return !(isInstalled || isAddedToInstall);
      }, this);
      dependentComponents = dependentComponents.concat(dependenciesToInstall);
    }, this);
    return dependentComponents;
  },

  /**
   * Sets order_id for each batch request in the `batches` array
   * @param batches {Array}
   * @private
   * @method {setOrderIdForBatches}
   */
  setOrderIdForBatches: function (batches) {
    batches.forEach(function (_batch, index) {
      _batch.order_id = index + 1;
    }, this);
  },

  /**
   * Gets the API request to create multiple components on a host
   * @param hostName {String}
   * @param components {String[]}|{String}
   * @return {Object} Deferred promise
   */
  getCreateHostComponentsRequest: function (hostName, components) {
    var query = "Hosts/host_name.in(" + hostName + ")";
    components = (Array.isArray(components)) ? components : [components];
    var hostComponent = components.map(function (_componentName) {
      return {
        "HostRoles": {
          "component_name": _componentName
        }
      }
    }, this);

    return {
      "type": 'POST',
      "uri": "/clusters/" + App.get('clusterName') + "/hosts",
      "RequestBodyInfo": {
        "RequestInfo": {
          "query": query
        },
        "Body": {
          "host_components": hostComponent
        }
      }
    };
  },

  /**
   * Gets the API request to install multiple components on a host
   * @param hostName {String}
   * @param components {String[]}
   * @param context {String} Optional
   * @return {Object}
   */
  getInstallHostComponentsRequest: function (hostName, components, context) {
    context = context || Em.I18n.t('requestInfo.installComponents');
    return this.getUpdateHostComponentsRequest(hostName, components, App.HostComponentStatus.stopped, context);
  },

  /**
   * Gets the API request to start multiple components on a host
   * @param hostName {String}
   * @param components {String[]}
   * @param context {String} Optional
   * @return {Object}
   */
  getStartHostComponentsRequest: function (hostName, components, context) {
    context = context || Em.I18n.t('requestInfo.startHostComponents');
    return this.getUpdateHostComponentsRequest(hostName, components, App.HostComponentStatus.started, context);
  },


  /**
   * Gets the API request to start/stop multiple components on a host
   * @param hostName {String}
   * @param components
   * @param desiredState
   * @param context {String}
   * @private
   * @method {getUpdateHostComponentsRequest}
   * @return {Object}
   */
  getUpdateHostComponentsRequest: function (hostName, components, desiredState, context) {
    components = (Array.isArray(components)) ? components : [components];
    var query = "HostRoles/component_name.in(" + components.join(',') + ")";

    return {
      "type": 'PUT',
      "uri": "/clusters/" + App.get('clusterName') + "/hosts/" + hostName + "/host_components",
      "RequestBodyInfo": {
        "RequestInfo": {
          "context": context,
          "operation_level": {
            "level": "HOST",
            "cluster_name": App.get('clusterName'),
            "host_names": hostName
          },
          "query": query
        },
        "Body": {
          "HostRoles": {
            "state": desiredState
          }
        }
      }
    };
  },

  /**
   * Gets the API request to delete component on a host
   * @param hostName {String}
   * @param component {String}
   * @private
   * @method {getDeleteHostComponentRequest}
   * @return {Object}
   */
  getDeleteHostComponentRequest: function (hostName, component) {
    return {
      "type": 'DELETE',
      "uri": "/clusters/" + App.get('clusterName') + "/hosts/" + hostName + "/host_components/" + component
    }
  },

  /**
   * Add `Create component` as a request in the batched API call
   * @param batches  {Array}
   * @param hostComponents {Array}
   * @private
   * @method {setCreateComponentRequest}
   */
  setCreateComponentRequest: function (batches, hostComponents) {
    var stackServices = App.StackServiceComponent.find(),
        services = App.Service.find();

    hostComponents.forEach(function (_componentName) {
      var serviceName = stackServices.findProperty('componentName', _componentName).get('serviceName');
      var serviceComponents = services.findProperty('serviceName', serviceName).get('serviceComponents');
      if (!serviceComponents.contains(_componentName)) {
        batches.push({
          "type": 'POST',
          "uri": "/clusters/" + App.get('clusterName') + "/services/" + serviceName + "/components/" + _componentName
        });
      }
    });
  },

  /**
   * Add `Refresh YARN Queue` as a request in the batched API call
   * @param batches  {Array}
   * @private
   * @method {setRefreshYarnQueueRequest}
   */
  setRefreshYarnQueueRequest: function (batches) {
    var capacitySchedulerConfigs = this.get('allConfigs')
                                  .filterProperty('filename', 'capacity-scheduler.xml')
                                  .filter(function (item) {
      return item.get('value') !== item.get('initialValue');
    });

    if (capacitySchedulerConfigs.length) {
      var serviceName = 'YARN';
      var componentName = 'RESOURCEMANAGER';
      var hostNames = App.Service.find(serviceName).get('hostComponents')
                                                   .filterProperty('componentName', componentName)
                                                   .mapProperty('hostName');
      // Set the flag to true
      this.isYarnQueueRefreshed = true;
      batches.push({
        "type": 'POST',
        "uri": "/clusters/" + App.get('clusterName') + "/requests",
        "RequestBodyInfo": {
          "RequestInfo": {
            "context": Em.I18n.t('services.service.actions.run.yarnRefreshQueues.context'),
            "command": "REFRESHQUEUES",
            "parameters/forceRefreshConfigTags": "capacity-scheduler"
          },
          "Requests/resource_filters": [
            {
              service_name: serviceName,
              component_name: componentName,
              hosts: hostNames.join(',')
            }
          ]
        }
      });
    }
  }
});