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
App.AddServiceController = App.WizardController.extend(App.AddSecurityConfigs, {

  name: 'addServiceController',

  totalSteps: 7,

  /**
   * @type {string}
   */
  displayName: Em.I18n.t('services.add.header'),

  /**
   * Used for hiding back button in wizard
   */
  hideBackButton: true,

  /**
   * @type {string}
   * @default null
   */
  serviceToInstall: null,

  /**
   *
   */
  installClientQueueLength: 0,

  areInstalledConfigGroupsLoaded: false,

  /**
   * All wizards data will be stored in this variable
   *
   * cluster - cluster name
   * installOptions - ssh key, repo info, etc.
   * services - services list
   * hosts - list of selected hosts
   * slaveComponentHosts, - info about slave hosts
   * masterComponentHosts - info about master hosts
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
    controllerName: 'addServiceController',
    configGroups: [],
    clients: [],
    additionalClients: [],
    installedHosts: {}
  }),

  loadMap: {
    '1': [
      {
        type: 'sync',
        callback: function () {
          this.loadServices();
        }
      }
    ],
    '2': [
      {
        type: 'async',
        callback: function () {
          var dfd = $.Deferred();
          var self = this;
          this.loadHosts().done(function () {
            self.loadMasterComponentHosts().done(function () {
              self.load('hosts');
              self.loadRecommendations();
              dfd.resolve();
            });
          });
          return dfd.promise();
        }
      }
    ],
    '3': [
      {
        type: 'async',
        callback: function () {
          var dfd = $.Deferred();
          var self = this;
          this.loadHosts().done(function () {
            self.loadServices();
            self.loadClients();
            self.loadSlaveComponentHosts();//depends on loadServices
            dfd.resolve();
          });
          return dfd.promise();
        }
      }
    ],
    '4': [
      {
        type: 'async',
        callback: function () {
          var self = this;
          var dfd = $.Deferred();
          this.load('cluster');
          this.set('content.additionalClients', []);
          this.set('installClientQueueLength', 0);
          this.set('installClietsQueue', App.ajaxQueue.create({abortOnError: false}));
          this.loadKerberosDescriptorConfigs().done(function() {
            self.loadServiceConfigGroups();
            self.loadConfigThemes().then(function() {
              self.loadServiceConfigProperties();
              self.loadCurrentHostGroups();
              dfd.resolve();
            });
          });
          return dfd.promise();
        }
      }
    ]
  },

  setCurrentStep: function (currentStep, completed) {
    this._super(currentStep, completed);
    App.clusterStatus.setClusterStatus({
      wizardControllerName: this.get('name'),
      localdb: App.db.data
    });
  },

  loadCurrentHostGroups: function () {
    this.set("content.recommendationsHostGroups", this.getDBProperty('recommendationsHostGroups'));
  },

  /**
   * Load services data. Will be used at <code>Select services(step4)</code> step
   */
  loadServices: function () {
    var services = this.getDBProperty('services');
    var stackServices = App.StackService.find();
    if (services) {
      stackServices.forEach(function (item) {
        var isSelected = services.selectedServices.contains(item.get('serviceName')) || item.get('serviceName') == this.get('serviceToInstall');
        var isInstalled = services.installedServices.contains(item.get('serviceName'));
        item.set('isSelected', isSelected || (this.get("currentStep") == "1" ? isInstalled : isSelected));
        item.set('isInstalled', isInstalled);
      }, this);
      this.setSkipSlavesStep(App.StackService.find().filterProperty('isSelected').filterProperty('isInstalled', false), 3);
    } else {
      services = {
        selectedServices: [],
        installedServices: []
      };
      stackServices.forEach(function (item) {
        var isInstalled = App.Service.find().someProperty('id', item.get('serviceName'));
        var isSelected = item.get('serviceName') === this.get('serviceToInstall') || item.get('coSelectedServices').contains(this.get('serviceToInstall'));
        item.set('isSelected', isInstalled || isSelected);
        item.set('isInstalled', isInstalled);
        var serviceName = item.get('serviceName');
        if (isInstalled) {
          services.selectedServices.push(serviceName);
          services.installedServices.push(serviceName);
        }
        else {
          if (isSelected) {
            services.selectedServices.push(serviceName);
          }
        }
      }, this);
      this.setDBProperty('services', services);
    }
    App.store.fastCommit();
    this.set('serviceToInstall', null);
    this.set('content.services', stackServices);
    var self = this;
    this.loadServiceVersionFromVersionDefinitions().complete(function () {
      var serviceVersionsMap = self.get('serviceVersionsMap');
      stackServices.forEach(function (stackService) {
        var serviceName = stackService.get('serviceName');
        Em.set(stackService, 'serviceVersionDisplay', serviceVersionsMap[serviceName]);
      });
      self.set('content.services', stackServices);
    });
  },

  /**
   * Save data to model
   * @param stepController App.WizardStep4Controller
   */
  saveServices: function (stepController) {
    var services = {
      selectedServices: [],
      installedServices: []
    };
    var selectedServices = stepController.get('content').filterProperty('isSelected', true).filterProperty('isInstalled', false);
    var selectedServiceNames = selectedServices.mapProperty('serviceName');
    services.selectedServices.pushObjects(selectedServiceNames);
    services.installedServices.pushObjects(stepController.get('content').filterProperty('isInstalled', true).mapProperty('serviceName'));
    // save services that already installed but ignored on choose services page
    // these services marked by `isInstallable` flag with value `false`, for example `Kerberos` service
    services.installedServices.pushObjects(App.Service.find().mapProperty('serviceName').filter(function(serviceName) {
      return !services.installedServices.contains(serviceName);
    }));
    this.setDBProperty('services', services);

    this.set('content.selectedServiceNames', selectedServiceNames);
    this.setDBProperty('selectedServiceNames', selectedServiceNames);
    this.setSkipSlavesStep(selectedServices, 3);
  },

  /**
   * Save Master Component Hosts data to Main Controller
   * @param stepController App.WizardStep5Controller
   */
  saveMasterComponentHosts: function (stepController) {
    var masterComponentHosts = [];
    var installedComponentsMap = App.HostComponent.find().toArray().toMapByProperty('componentName');

    stepController.get('selectedServicesMasters').forEach(function (_component) {
      var installedComponent = installedComponentsMap[_component.component_name];
      masterComponentHosts.push({
        display_name: _component.display_name,
        component: _component.component_name,
        hostName: _component.selectedHost,
        serviceId: _component.serviceId,
        isInstalled: !!installedComponent,
        workStatus: installedComponent && installedComponent.get('workStatus')
      });
    });

    this.setDBProperty('masterComponentHosts', masterComponentHosts);
    this.set('content.masterComponentHosts', masterComponentHosts);

    this.set('content.skipMasterStep', masterComponentHosts.everyProperty('isInstalled', true));
    this.get('isStepDisabled').findProperty('step', 2).set('value', this.get('content.skipMasterStep'));
  },

  /**
   * Load master component hosts data for using in required step controllers
   */
  loadMasterComponentHosts: function () {
    var self = this,
        dfd = $.Deferred(),
        ASSIGN_MASTER_STEP = 2,
        DEPLOYMENT_STEPS = ['6', '7', '8'];
    this._super().done(function () {
      self.set('content.skipMasterStep', App.StackService.find().filterProperty('isSelected').filterProperty('hasMaster').everyProperty('isInstalled', true));
      self.get('isStepDisabled').findProperty('step', ASSIGN_MASTER_STEP).set('value', self.get('content.skipMasterStep') || DEPLOYMENT_STEPS.contains(self.get('currentStep')));
      dfd.resolve();
    });
    return dfd.promise();
  },

  /**
   * Does service have any configs
   * @param {string} serviceName
   * @returns {boolean}
   */
  isServiceNotConfigurable: function (serviceName) {
    return App.get('services.noConfigTypes').contains(serviceName);
  },

  /**
   * Should Config Step be skipped (based on selected services list)
   * @returns {boolean}
   */
  skipConfigStep: function () {
    var skipConfigStep = true;
    this.get('content.services')
      .filterProperty('isSelected', true)
      .filterProperty('isInstalled', false)
      .mapProperty('serviceName')
      .map(function (serviceName) {
      skipConfigStep = skipConfigStep && this.isServiceNotConfigurable(serviceName);
    }, this);
    return skipConfigStep;
  },

  loadServiceConfigProperties: function () {
    this._super();
    if (!this.get('content.services')) {
      this.loadServices();
    }
    if (this.get('currentStep') > 1 && this.get('currentStep') < 6) {
      this.set('content.skipConfigStep', this.skipConfigStep());
      this.get('isStepDisabled').findProperty('step', 4).set('value', this.get('content.skipConfigStep'));
    }
  },

  /**
   * Load kerberos descriptor configuration
   * @returns {$.Deferred}
   */
  loadKerberosDescriptorConfigs: function() {
    var self = this,
        dfd = $.Deferred(),
        mergedDescriptorConfigs;
    if (App.get('isKerberosEnabled')) {
      this.loadClusterDescriptorStackConfigs().then(function (stackProperties) {
        self.loadClusterDescriptorConfigs().then(function(properties) {
          self.set('kerberosDescriptorData', properties);
          mergedDescriptorConfigs = self.mergeDescriptorStackWithConfigs(stackProperties, properties);
          self.set('kerberosDescriptorConfigs', mergedDescriptorConfigs);
        }).always(function(){
          dfd.resolve();
        });
      });
    } else {
      dfd.resolve();
    }
    return dfd.promise();
  },

  saveServiceConfigProperties: function (stepController) {
    this._super(stepController);
    if (this.get('currentStep') > 1 && this.get('currentStep') < 6) {
      this.set('content.skipConfigStep', this.skipConfigStep());
      this.get('isStepDisabled').findProperty('step', 4).set('value', this.get('content.skipConfigStep'));
    }
  },

  /**
   * Load master component hosts data for using in required step controllers
   */
  loadSlaveComponentHosts: function () {
    var props = this.getDBProperties(['slaveComponentHosts', 'hosts']);
    var slaveComponentHosts = props.slaveComponentHosts,
      hosts = props.hosts || {},
      hostNames = Em.keys(hosts);
    if (!Em.isNone(slaveComponentHosts)) {
      slaveComponentHosts.forEach(function (component) {
        component.hosts.forEach(function (host) {
          //Em.set(host, 'hostName', hosts[host.host_id].name);
          for (var i = 0; i < hostNames.length; i++) {
            if (hosts[hostNames[i]].id === host.host_id) {
              host.hostName = hostNames[i];
              break;
            }
          }
        });
      });
    }

    this.set('content.installedHosts', this.getDBProperty('hosts') || this.get('content.hosts'));
    this.set("content.slaveComponentHosts", slaveComponentHosts);
  },

  /**
   * Generate clients list for selected services and save it to model
   */
  saveClients: function () {
    var clients = [];
    var clientComponents = App.StackServiceComponent.find().filterProperty('isClient');
    this.get('content.services')
    .filterProperty('isSelected')
    .filterProperty('isInstalled', false)
    .forEach(function (_service) {
      var serviceClients = clientComponents.filterProperty('serviceName', _service.get('serviceName'));
      serviceClients.forEach(function (client) {
        clients.push({
          component_name: client.get('componentName'),
          display_name: client.get('displayName'),
          isInstalled: false
        });
      }, this);
    }, this);

    this.setDBProperty('clientInfo', clients);
    this.set('content.clients', clients);
  },

  /**
   * Load information about hosts with clients components
   */
  loadClients: function () {
    var clients = this.getDBProperty('clientInfo');
    if (clients) {
      this.set('content.clients', clients);
    } else {
      this.saveClients();
    }
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

  /**
   * Clear all temporary data
   */
  finish: function () {
    this.clearAllSteps();
    this.clearStorageData();
    this.clearServiceConfigProperties();
    this.resetDbNamespace();
    App.router.get('updateController').updateAll();
  },

  /**
   * genarates data for ajax request to launch install services
   * @method generateDataForInstallServices
   * @param {Array} selectedServices
   * @returns {{context: *, ServiceInfo: {state: string}, urlParams: string}}
   */
  generateDataForInstallServices: function(selectedServices) {
    if (selectedServices.contains('OOZIE')) {
      selectedServices = selectedServices.concat(['HDFS', 'YARN', 'MAPREDUCE2']);
    }
    return {
      "context": Em.I18n.t('requestInfo.installServices'),
      "ServiceInfo": {"state": "INSTALLED"},
      "urlParams": "ServiceInfo/service_name.in(" + selectedServices.join(',') + ")"
    };
  },

  /**
   * main method for installing additional clients and services
   * @param {function} callback
   * @method installServices
   */
  installServices: function (callback) {
    this.set('content.cluster.oldRequestsId', []);
    this.installAdditionalClients().done(() => {
      this.installSelectedServices(callback);
    });
  },

  /**
   * method to install added services
   * @param {function} callback
   * @method installSelectedServices
   */
  installSelectedServices: function (callback) {
    var selectedServices = this.get('content.services').filterProperty('isInstalled', false).filterProperty('isSelected').mapProperty('serviceName');
    var dependentServices = this.getServicesBySelectedSlaves();
    var data = this.generateDataForInstallServices(selectedServices.concat(dependentServices));
    this.installServicesRequest('common.services.update', data, callback.bind(this));
  },

  installServicesRequest: function (name, data, callback) {
    callback = callback || Em.K;
    App.ajax.send({
      name: name,
      sender: this,
      data: data,
      success: 'installServicesSuccessCallback',
      error: 'installServicesErrorCallback'
    }).then(callback, callback);
  },

  /**
   * return list of services by selected and not installed slave components
   * @returns {Array}
   */
  getServicesBySelectedSlaves: function () {
    const result = [];
    this.get('content.slaveComponentHosts').forEach(function (slaveComponent) {
      if (slaveComponent.hosts.someProperty('isInstalled', false)) {
        const stackComponent = App.StackServiceComponent.find(slaveComponent.componentName);
        if (stackComponent.get('isLoaded')) {
          result.push(stackComponent.get('serviceName'));
        }
      }
    });
    return result.uniq();
  },

  /**
   * installs clients before install new services
   * on host where some components require this
   * @method installAdditionalClients
   */
  installAdditionalClients: function () {
    var dfd = $.Deferred();
    var count = 0;
    if (this.get('content.additionalClients.length') > 0) {
      this.get('content.additionalClients').forEach((c) => {
        if (c.hostNames.length > 0) {
          this.get('installClietsQueue').addRequest({
            name: 'common.host_component.update',
            sender: this,
            data: {
              query: 'HostRoles/component_name='+ c.componentName + '&HostRoles/host_name.in(' + c.hostNames.join() + ')',
              context: 'Install ' + App.format.role(c.componentName, false),
              HostRoles: {
                state: 'INSTALLED'
              },
              counter: count++,
              deferred: dfd
            },
            success: 'installClientSuccess',
            error: 'installClientError'
          });
        }
      }, this);
      if (!this.get('installClietsQueue.queue.length')) {
        return dfd.resolve();
      }
      this.set('installClientQueueLength', this.get('installClietsQueue.queue.length'));
      App.get('router.wizardStep8Controller').set('servicesInstalled', true);
      this.get('installClietsQueue').start();
    } else {
      dfd.resolve();
    }
    return dfd.promise();
  },

  /**
   * callback for when install clients success
   * @param data
   * @param opt
   * @param params
   * @method installClientComplete
   */
  installClientSuccess: function(data, opt, params) {
    if (this.get('installClientQueueLength') - 1 === params.counter) {
      params.deferred.resolve();
    }
  },

  /**
   * callback for when install clients fail
   * @param request
   * @param ajaxOptions
   * @param error
   * @param opt
   * @param params
   */
  installClientError: function(request, ajaxOptions, error, opt, params) {
    if (this.get('installClientQueueLength') - 1 === params.counter) {
      params.deferred.resolve();
    }
  },

  loadServiceConfigGroups: function () {
    this._super();
    this.set('areInstalledConfigGroupsLoaded', !Em.isNone(this.getDBProperty('serviceConfigGroups')));
  },

  clearStorageData: function () {
    this._super();
    this.set('areInstalledConfigGroupsLoaded', false);
  }

});
