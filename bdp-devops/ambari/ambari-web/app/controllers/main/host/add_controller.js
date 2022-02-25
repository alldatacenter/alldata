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

App.AddHostController = App.WizardController.extend({

  name: 'addHostController',

  totalSteps: 7,

  /**
   * @type {string}
   */
  displayName: Em.I18n.t('hosts.add.header'),

  /**
   * Used for hiding back button in wizard
   */
  hideBackButton: true,

  /**
   * All wizards data will be stored in this variable
   *
   * cluster - cluster name
   * hosts - hosts, ssh key, repo info, etc.
   * services - services list
   * hostsInfo - list of selected hosts
   * slaveComponentHosts, hostSlaveComponents - info about slave hosts
   * masterComponentHosts - info about master hosts
   * serviceConfigGroups - info about selected config group for service
   * configGroups - all config groups
   * config??? - to be described later
   */
  content: Em.Object.create({
    cluster: null,
    hosts: null,
    installOptions: null,
    services: null,
    slaveComponentHosts: null,
    masterComponentHosts: null,
    serviceConfigProperties: null,
    advancedServiceConfig: null,
    controllerName: 'addHostController',
    serviceConfigGroups: null,
    configGroups: null
  }),

  /**
   * Load data for all steps until <code>current step</code>
   */
  loadMap: {
    '1': [
      {
        type: 'sync',
        callback: function () {
          this.load('hosts');
          this.load('installOptions');
          this.load('cluster');
        }
      }
    ],
    '2': [
      {
        type: 'sync',
        callback: function () {
          this.loadServices();
        }
      }
    ],
    '3': [
      {
        type: 'async',
        callback: function () {
          var self = this,
            dfd = $.Deferred();
          this.loadClients();
          this.loadServices();
          this.loadMasterComponentHosts().done(function () {
            self.loadSlaveComponentHosts();
            self.load('hosts');
            dfd.resolve();
          });
          return dfd.promise();
        }
      }
    ],
    '5': [
      {
        type: 'sync',
        callback: function () {
          this.loadServiceConfigProperties();
          this.getServiceConfigGroups();
        }
      }
    ]
  },

  /**
   * save info about wizard progress, particularly current step of wizard
   * @param currentStep
   * @param completed
   */
  setCurrentStep: function (currentStep, completed) {
    this._super(currentStep, completed);
    App.clusterStatus.setClusterStatus({
      wizardControllerName: this.get('name'),
      localdb: App.db.data
    });
    var self = this;
    Em.run.next(function(){
      if (self.isConfigGroupsEmpty()) {
        self.disableStep(4);
      }
    });   
  },

  /**
   * return new object extended from clusterStatusTemplate
   * @return Object
   */
  getCluster: function () {
    return jQuery.extend({}, this.get('clusterStatusTemplate'), {name: App.router.getClusterName()});
  },

  /**
   * Remove host from model. Used at <code>Confirm hosts</code> step
   * @param hosts Array of hosts, which we want to delete
   */
  removeHosts: function (hosts) {
    var dbHosts = this.getDBProperty('hosts');
    hosts.forEach(function (_hostInfo) {
      var host = _hostInfo.name;
      delete dbHosts[host];
    });
    this.setDBProperty('hosts', dbHosts);
  },

  disableStep: function(step) {
    this.get('isStepDisabled').findProperty('step', step).set('value', true);
  },

  isConfigGroupsEmpty: function() {
    return !this.get('content.configGroups') || !this.get('content.configGroups').length; 
  },

  /**
   * Load services data. Will be used at <code>Select services(step4)</code> step
   */
  loadServices: function () {
    var services = this.getDBProperty('services');
    if (!services) {
      services = {
        selectedServices: [],
        installedServices: []
      };
      App.StackService.find().forEach(function (item) {
        var isInstalled = App.Service.find().someProperty('serviceName', item.get('serviceName'));
        item.set('isSelected', isInstalled);
        item.set('isInstalled', isInstalled);
        if (isInstalled) {
          services.selectedServices.push(item.get('serviceName'));
          services.installedServices.push(item.get('serviceName'));
        }
      }, this);
      this.setDBProperty('services', services);
    } else {
      App.StackService.find().forEach(function (item) {
        var isSelected = services.selectedServices.contains(item.get('serviceName'));
        var isInstalled = services.installedServices.contains(item.get('serviceName'));
        item.set('isSelected', isSelected);
        item.set('isInstalled', isInstalled);
      }, this);
    }
    this.set('content.services', App.StackService.find());
  },

 /**
   * Load slave component hosts data for using in required step controllers
   * TODO move to mixin
   */
  loadSlaveComponentHosts: function () {
   var props = this.getDBProperties(['slaveComponentHosts', 'hosts']);
    var slaveComponentHosts = props.slaveComponentHosts || [];
    if (slaveComponentHosts.length) {
      var hosts = props.hosts || {},
          host_names = Em.keys(hosts);
      slaveComponentHosts.forEach(function (component) {
        component.hosts.forEach(function (host) {
          //Em.set(host, 'hostName', hosts[host.host_id].name);
          for (var i = 0; i < host_names.length; i++) {
            if (hosts[host_names[i]].id === host.host_id) {
              host.hostName = host_names[i];
              break;
            }
          }
        });
      });
    }
    this.set("content.slaveComponentHosts", slaveComponentHosts);
  },

  /**
   * Generate clients list for selected services and save it to model
   */
  saveClients: function () {
    var serviceComponents = App.StackServiceComponent.find();
    var services = this.get('content.services').filterProperty('isInstallable').filterProperty('isSelected');
    var clients = this.getClientsToInstall(services, serviceComponents);
    this.setDBProperty('clientInfo', clients);
    this.set('content.clients', clients);
  },

  /**
   * get list of clients which will be installed on host
   * @param services {Array} of service objects
   * @param components {Array} of component objects
   * @returns {Array} returns array of clients
   * @method getClientsToInstall;
   */
  getClientsToInstall: function(services, components) {
    var clients = [];
    services.forEach(function (_service) {
      var serviceClients = components.filter(function(component) {
        return (component.get('serviceName') == _service.get('serviceName') && component.get('isClient'));
      });
      if (serviceClients.length) {
        serviceClients.forEach(function(client) {
          clients.push({
            component_name: client.get('componentName'),
            display_name: client.get('displayName'),
            isInstalled: false
          });
        });
      }
    }, this);
    return clients;
  },
  /**
   *  Apply config groups from step4 Configurations
   */
  applyConfigGroup: function () {
    var serviceConfigGroups = this.get('content.configGroups');
    serviceConfigGroups.forEach(function (group) {
      if (group.configGroups.someProperty('ConfigGroup.group_name', group.selectedConfigGroup)) {
        var configGroup = group.configGroups.findProperty('ConfigGroup.group_name', group.selectedConfigGroup);
        group.hosts.forEach(function (host) {
          configGroup.ConfigGroup.hosts.push({
            host_name: host
          });
        }, this);
        delete configGroup.href;
        App.ajax.send({
          name: 'config_groups.update_config_group',
          sender: this,
          data: {
            id: configGroup.ConfigGroup.id,
            configGroup: configGroup
          }
        });
      }
    }, this);
  },

  /**
   * Load information about selected config groups
   */
  getServiceConfigGroups: function () {
    var serviceConfigGroups = this.getDBProperty('serviceConfigGroups');
    this.set('content.configGroups', serviceConfigGroups);
  },

  /**
   * Save information about selected config groups
   */
  saveServiceConfigGroups: function () {
    this.setDBProperty('serviceConfigGroups', this.get('content.configGroups'));
  },

  /**
   * Set content.configGroups for step4
   */
  loadServiceConfigGroups: function () {
    var selectedServices = [];
    this.loadServiceConfigGroupsBySlaves(selectedServices);
    this.loadServiceConfigGroupsByClients(selectedServices);
    this.sortServiceConfigGroups(selectedServices);
    this.set('content.configGroups', selectedServices);
  },
  /**
   * sort config groups by name
   * @param selectedServices
   */
  sortServiceConfigGroups: function (selectedServices) {
    selectedServices.forEach(function (selectedService) {
      selectedService.configGroups.sort(function (cfgA, cfgB) {
        if (cfgA.ConfigGroup.group_name < cfgB.ConfigGroup.group_name) return -1;
        if (cfgA.ConfigGroup.group_name > cfgB.ConfigGroup.group_name) return 1;
        return 0;
      });
    });
  },
  /**
   * load service config groups by slave components,
   * push them into selectedServices
   * @param selectedServices
   */
  loadServiceConfigGroupsBySlaves: function (selectedServices) {
    var slaveComponentHosts = this.get('content.slaveComponentHosts');
    if (slaveComponentHosts && slaveComponentHosts.length > 0) {
      slaveComponentHosts.forEach(function (slave) {
        if (slave.hosts.length > 0) {
          if (slave.componentName !== "CLIENT") {
            var service = App.StackServiceComponent.find(slave.componentName).get('stackService');
            var serviceName = service.get('serviceName');
            var configGroups = this.get('content.configGroups').filterProperty('ConfigGroup.tag', serviceName);
            var configGroupsNames = configGroups.mapProperty('ConfigGroup.group_name');
            var defaultGroupName = 'Default';
            var selectedService = selectedServices.findProperty('serviceId', serviceName);
            configGroupsNames.unshift(defaultGroupName);
            if (selectedService) {
              Em.set(selectedService, 'hosts', Em.getWithDefault(selectedService, 'hosts', []).concat(slave.hosts.mapProperty('hostName')).uniq());
            } else {
              selectedServices.push({
                serviceId: serviceName,
                displayName: service.get('displayName'),
                hosts: slave.hosts.mapProperty('hostName'),
                configGroupsNames: configGroupsNames,
                configGroups: configGroups,
                selectedConfigGroup: defaultGroupName
              });
            }
          }
        }
      }, this);
      return true;
    }
    return false;
  },
  /**
   * load service config groups by clients,
   * push them into selectedServices
   * @param selectedServices
   */
  loadServiceConfigGroupsByClients: function (selectedServices) {
    var slaveComponentHosts = this.get('content.slaveComponentHosts');
    var clients = this.get('content.clients');
    var client = slaveComponentHosts && slaveComponentHosts.findProperty('componentName', 'CLIENT');
    var selectedClientHosts = client && client.hosts.mapProperty('hostName');
    if (clients && selectedClientHosts && clients.length > 0 && selectedClientHosts.length > 0) {
      this.loadClients();
      clients.forEach(function (client) {
        var service = App.StackServiceComponent.find(client.component_name).get('stackService');
        var serviceName = service.get('serviceName');
        var serviceMatch = selectedServices.findProperty('serviceId', serviceName);
        if (serviceMatch) {
          serviceMatch.hosts = serviceMatch.hosts.concat(selectedClientHosts).uniq();
        } else {
          var configGroups = this.get('content.configGroups').filterProperty('ConfigGroup.tag', serviceName);
          var configGroupsNames = configGroups.mapProperty('ConfigGroup.group_name').sort();
          var defaultGroupName = 'Default';
          configGroupsNames.unshift(defaultGroupName);
          selectedServices.push({
            serviceId: serviceName,
            displayName: service.get('displayName'),
            hosts: selectedClientHosts,
            configGroupsNames: configGroupsNames,
            configGroups: configGroups,
            selectedConfigGroup: defaultGroupName
          });
        }
      }, this);
      return true;
    }
    return false;
  },

  loadServiceConfigProperties: function () {
    var serviceConfigProperties = App.db.get('AddService', 'serviceConfigProperties');
    if (!serviceConfigProperties || !serviceConfigProperties.length) {
      serviceConfigProperties = App.db.get('Installer', 'serviceConfigProperties');
    }
    this.set('content.serviceConfigProperties', serviceConfigProperties);
  },

  /**
   * Remove all loaded data.
   * Created as copy for App.router.clearAllSteps
   */
  clearAllSteps: function () {
    this.clearInstallOptions();
    // clear temporary information stored during the install
    this.set('content.cluster', this.getCluster());
  },

  clearStorageData: function () {
    this._super();
    this.resetDbNamespace();
  },

  /**
   * Clear all temporary data
   */
  finish: function () {
    this.clearAllSteps();
    this.clearStorageData();
  },

  /**
   * send request to server in order to install services
   * @param isRetry
   * @param callback
   * @param errorCallback
   */
  installServices: function (isRetry, callback, errorCallback) {
    callback = callback || Em.K;
    this.set('content.cluster.oldRequestsId', []);
    this.set('content.cluster.status', 'PENDING');
    var clusterName = this.get('content.cluster.name');
    var hostNames = [];
    var hosts = this.getDBProperty('hosts');
    for (var hostname in hosts) {
      if(!hosts[hostname].isInstalled) {
        hostNames.push(hostname);
      }
    }
    if(!clusterName || hostNames.length === 0) return false;

    App.ajax.send({
      name: "common.host_components.update",
      sender: this,
      data: {
        "context": Em.I18n.t('requestInfo.installComponents'),
        "query": "HostRoles/host_name.in(" + hostNames.join(',') + ")",
        "HostRoles": {"state": "INSTALLED"},
        "level": "HOST_COMPONENT"
      },
      success: 'installServicesSuccessCallback',
      error: 'installServicesErrorCallback'
    }).then(callback, errorCallback || callback);
    return true;
  }
});
