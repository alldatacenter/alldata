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
var stringUtils = require('utils/string_utils');
var validator = require('utils/validator');

App.InstallerController = App.WizardController.extend(App.Persist, {

  name: 'installerController',

  isCheckInProgress: false,

  totalSteps: 11,

  isInstallerWizard: true,

  content: Em.Object.create({
    cluster: null,
    installOptions: null,
    hosts: null,
    services: null,
    slaveComponentHosts: null,
    masterComponentHosts: null,
    serviceConfigProperties: null,
    advancedServiceConfig: null,
    configGroups: [],
    slaveGroupProperties: null,
    stacks: null,
    clients: [],
    // list of components, that was added from configs page via AssignMasterOnStep7Controller
    componentsFromConfigs: [],
    /**
     * recommendations for host groups loaded from server
     */
    recommendations: null,
    /**
     * recommendationsHostGroups - current component assignment after 5 and 6 steps, 
     * or adding hiveserver2 interactive on "configure services" page
     * (uses for host groups validation and to load recommended configs)
     */
    recommendationsHostGroups: null,
    controllerName: 'installerController'
  }),

  /**
   * Wizard properties in local storage, which should be cleaned right after wizard has been finished
   */
  dbPropertiesToClean: [
    'service',
    'hosts',
    'masterComponentHosts',
    'slaveComponentHosts',
    'cluster',
    'allHostNames',
    'installOptions',
    'allHostNamesPattern',
    'serviceComponents',
    'clientInfo',
    'selectedServiceNames',
    'serviceConfigGroups',
    'serviceConfigProperties',
    'fileNamesToUpdate',
    'bootStatus',
    'stacksVersions',
    'currentStep',
    'serviceInfo',
    'hostInfo',
    'recommendations',
    'recommendationsHostGroups',
    'recommendationsConfigs',
    'componentsFromConfigs',
    'operatingSystems',
    'repositories'
  ],

  init: function () {
    this._super();
    this.get('isStepDisabled').setEach('value', true);
    this.get('isStepDisabled').pushObject(Ember.Object.create({
      step: 0,
      value: true
    }));
  },
  /**
   * redefined connectOutlet method to avoid view loading by unauthorized user
   * @param view
   * @param content
   */
  connectOutlet: function (view, content) {
    if (App.db.getAuthenticated()) {
      this._super(view, content);
    }
  },

  getCluster: function () {
    return jQuery.extend({}, this.get('clusterStatusTemplate'));
  },

  getHosts: function () {
    return [];
  },

  /**
   * Remove host from model. Used at <code>Confirm hosts(step2)</code> step
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

  cancelInstall: function() {
    return App.showConfirmationPopup(() => {
      App.router.get('applicationController').goToAdminView();
    });
  },

  /**
   * Load services data. Will be used at <code>Select services(step4)</code> step
   */
  loadServices: function () {
    var dfd = $.Deferred();
    var self = this;
    var stackServices = App.StackService.find().mapProperty('serviceName');
    if (!(stackServices.length && App.StackService.find().objectAt(0).get('stackVersion') === App.get('currentStackVersionNumber'))) {
      this.loadServiceComponents().complete(function () {
        self.set('content.services', App.StackService.find().forEach(function (item) {
          // user the service version from VersionDefinition
          var serviceInStack = App.Stack.find().findProperty('isSelected').get('stackServices').findProperty('name', item.get('serviceName'));
          var serviceVersionDisplay = serviceInStack ? serviceInStack.get('latestVersion') : item.get('serviceVersion');
          item.set('serviceVersionDisplay', serviceVersionDisplay);
        }));
        dfd.resolve();
      });
    } else {
      dfd.resolve();
    }
    return dfd.promise();
  },

  /**
   * total set of hosts registered to cluster, analog of App.Host model,
   * used in Installer wizard until hosts are installed
   */
  allHosts: function () {
    var rawHosts = this.get('content.hosts');
    var masterComponents = this.get('content.masterComponentHosts');
    var slaveComponents = this.get('content.slaveComponentHosts');
    var hosts = [];
    masterComponents.forEach(function (component) {
      var host = rawHosts[component.hostName];
      if (host.hostComponents) {
        host.hostComponents.push(Em.Object.create({
          componentName: component.component,
          displayName: component.display_name
        }));
      } else {
        rawHosts[component.hostName].hostComponents = [
          Em.Object.create({
            componentName: component.component,
            displayName: component.display_name
          })
        ]
      }
    });
    slaveComponents.forEach(function (component) {
      component.hosts.forEach(function (rawHost) {
        var host = rawHosts[rawHost.hostName];
        if (host.hostComponents) {
          host.hostComponents.push(Em.Object.create({
            componentName: component.componentName,
            displayName: component.displayName
          }));
        } else {
          rawHosts[rawHost.hostName].hostComponents = [
            Em.Object.create({
              componentName: component.componentName,
              displayName: component.displayName
            })
          ]
        }
      });
    });

    for (var hostName in rawHosts) {
      var host = rawHosts[hostName];
      hosts.pushObject(Em.Object.create({
          id: host.name,
          hostName: host.name,
          hostComponents: host.hostComponents || []
        }
      ))
    }
    return hosts;
  }.property('content.hosts'),

  stacks: [],

  /**
   * stack names used as auxiliary data to query stacks by name
   */
  stackNames: [],

  /**
   * Load stacks data from server or take exist data from in memory variable {{content.stacks}}
   * The series of API calls will be called  When landing first time on Select Stacks page
   * or on hitting refresh post select stacks page in installer wizard
   */
  loadStacks: function () {
    var stacks = this.get('content.stacks');
    var dfd = $.Deferred();
    if (stacks && stacks.get('length')) {
      App.set('currentStackVersion', App.Stack.find().findProperty('isSelected').get('stackNameVersion'));
      dfd.resolve(true);
    } else {
      App.ajax.send({
        name: 'wizard.stacks',
        sender: this,
        success: 'loadStacksSuccessCallback',
        error: 'loadStacksErrorCallback'
      }).complete(function () {
        dfd.resolve(false);
      });
    }
    return dfd.promise();
  },

  /**
   * Send queries to load versions for each stack
   */
  loadStacksSuccessCallback: function (data) {
    this.get('stacks').clear();
    this.set('stackNames', data.items.mapProperty('Stacks.stack_name'));
  },

  /**
   * onError callback for loading stacks data
   */
  loadStacksErrorCallback: function () {
  },

  /**
   * query every stack names from server
   * @return {Array}
   */
  loadStacksVersions: function () {
    var requests = [];
    const dfd = $.Deferred();
    this.get('stackNames').forEach(function (stackName) {
      requests.push(App.ajax.send({
        name: 'wizard.stacks_versions_definitions',
        sender: this,
        data: {
          stackName: stackName,
          dfd: dfd
        },
        success: 'loadStacksVersionsDefinitionsSuccessCallback',
        error: 'loadStacksVersionsErrorCallback'
      }));
    }, this);
    this.set('loadStacksRequestsCounter', requests.length);
    return dfd.promise();
  },

  /**
   * Counter for counting number of successful requests to load stack versions
   */
  loadStacksRequestsCounter: 0,

  /**
   * Parse loaded data and create array of stacks objects
   */
  loadStacksVersionsDefinitionsSuccessCallback: function (data, opt, params) {
    var stacks = App.db.getStacks();
    var oses = App.db.getOses();
    var repos = App.db.getRepos();
    this.decrementProperty('loadStacksRequestsCounter');
    var isStacksExistInDb = stacks && stacks.length;
    if (isStacksExistInDb) {
      stacks.forEach(function (_stack) {
        var stack = data.items.findProperty('VersionDefinition.id', _stack.id);
        if (stack) {
          stack.VersionDefinition.is_selected = _stack.is_selected;
        }
      }, this);
    }
    // if data.items is empty, we show error modal end return to back step
    if (data.items && data.items.length) {
      data.items.sortProperty('VersionDefinition.stack_version').reverse().forEach(function (versionDefinition) {
        // to display repos panel, should map all available operating systems including empty ones
        var stackInfo = {};
        stackInfo.isStacksExistInDb = isStacksExistInDb;
        stackInfo.stacks = stacks;
        stackInfo.oses = oses;
        stackInfo.repos = repos;
        this.getSupportedOSList(versionDefinition, stackInfo, params.dfd);
      }, this);
    } else {
      App.showAlertPopup(Em.I18n.t('common.error'), Em.I18n.t('installer.step1.noVersionDefinitions'), function() {
        App.router.send('back');
      });
    }
  },

  mergeChanges: function (repos, oses, stacks) {
    var _repos = repos || [];
    var _oses = oses || [];
    var _stacks = stacks || [];
    _repos.forEach(function (repo) {
      if (App.Repository.find(repo.id).get('isLoaded')) {
        App.Repository.find(repo.id).set('baseUrl', repo.base_url);
      }
    });
    _oses.forEach(function (os) {
      if (App.OperatingSystem.find().findProperty('id', os.id)) {
        App.OperatingSystem.find().findProperty('id', os.id).set('isSelected', os.is_selected);
      }
    });
    //should delete the record on going to step 2, on going back to step 1, still need the record
    if (App.router.get('currentState.name') != "step1") {
      App.OperatingSystem.find().filterProperty('isSelected', false).forEach(function (os) {
        App.stackMapper.deleteRecord(os);
      });
    }
    _stacks.forEach(function (_stack) {
      var stack = App.Stack.find().findProperty('id', _stack.id);
      if (stack) {
        stack.set('useRedhatSatellite', _stack.use_redhat_satellite);
      }
    });
  },

  setSelected: function (isStacksExistInDb, dfd) {
    if (!isStacksExistInDb) {
      var stacks = App.Stack.find();
      stacks.setEach('isSelected', false);
      stacks.sortProperty('id').set('lastObject.isSelected', true);
    }
    this.set('content.stacks', App.Stack.find());
    App.set('currentStackVersion', App.Stack.find().findProperty('isSelected').get('stackNameVersion'));
    dfd.resolve();
  },

  /**
   * Get the the repo version (to install) info, this data will be POST
   * @method startDeploy
   */
  getSelectedRepoVersionData: function () {
    var vdfData = App.db.getLocalRepoVDFData();
    var selectedStack = App.Stack.find().findProperty('isSelected', true);
    var isXMLdata = false;
    var data = {};
    if (selectedStack && selectedStack.get('showAvailable')) {
      //meaning user selected a public repo
      data = {
        "VersionDefinition": {
          "available": selectedStack.get('id')
        }
      };
      isXMLdata = false;
    } else if (vdfData && validator.isValidURL(vdfData)) {
      // meaning user uploaded a VDF via entering URL
      data = {
        "VersionDefinition": {
          "version_url": vdfData
        }
      };
      isXMLdata = false;
    } else if (vdfData) {
      // meaning user uploaded a local VDF.xml file
      isXMLdata = true;
      data = vdfData;
    } else {
      return null;
    }
    return {
      isXMLdata: isXMLdata,
      data: data
    };
  },

  /**
   * onError callback for loading stacks data
   */
  loadStacksVersionsErrorCallback: function () {
  },

  /**
   * check server version and web client version
   */
  checkServerClientVersion: function () {
    var dfd = $.Deferred();
    var self = this;
    self.getServerVersion().done(function () {
      dfd.resolve();
    });
    return dfd.promise();
  },
  getServerVersion: function () {
    return App.ajax.send({
      name: 'ambari.service',
      sender: this,
      data: {
        fields: '?fields=RootServiceComponents/component_version,RootServiceComponents/properties/server.os_family&minimal_response=true'
      },
      success: 'getServerVersionSuccessCallback',
      error: 'getServerVersionErrorCallback'
    });
  },
  getServerVersionSuccessCallback: function (data) {
    var clientVersion = App.get('version');
    var serverVersion = data.RootServiceComponents.component_version.toString();
    this.set('ambariServerVersion', serverVersion);
    if (clientVersion) {
      this.set('versionConflictAlertBody', Em.I18n.t('app.versionMismatchAlert.body').format(serverVersion, clientVersion));
      this.set('isServerClientVersionMismatch', clientVersion !== serverVersion);
    } else {
      this.set('isServerClientVersionMismatch', false);
    }
    App.set('isManagedMySQLForHiveEnabled', App.config.isManagedMySQLForHiveAllowed(data.RootServiceComponents.properties['server.os_family']));
  },
  getServerVersionErrorCallback: function () {
  },

  /**
   * set stacks from server to content and local DB
   */
  setStacks: function () {
    App.db.setStacks(App.Stack.find().slice());
    this.set('content.stacks', App.Stack.find());
    App.db.setOses(App.OperatingSystem.find().slice());
    App.db.setRepos(App.Repository.find().slice());
  },

  /**
   * Save data to model
   * @param stepController App.WizardStep4Controller
   */
  saveServices: function (stepController) {
    var selectedServiceNames = [];
    var installedServiceNames = [];
    stepController.filterProperty('isSelected').forEach(function (item) {
      selectedServiceNames.push(item.get('serviceName'));
    });
    stepController.filterProperty('isInstalled').forEach(function (item) {
      installedServiceNames.push(item.get('serviceName'));
    });
    this.set('content.services', App.StackService.find());
    this.set('content.selectedServiceNames', selectedServiceNames);
    this.set('content.installedServiceNames', installedServiceNames);
    this.setDBProperties({
      selectedServiceNames: selectedServiceNames,
      installedServiceNames: installedServiceNames
    });
  },

  /**
   * Save Master Component Hosts data to Main Controller
   * @param stepController App.WizardStep5Controller
   * @param  skip  {Boolean}
   */
  saveMasterComponentHosts: function (stepController, skip) {
    var obj = stepController.get('selectedServicesMasters'),
      hosts = this.getDBProperty('hosts');

    var masterComponentHosts = [];
    obj.forEach(function (_component) {
      masterComponentHosts.push({
        display_name: _component.get('display_name'),
        component: _component.get('component_name'),
        serviceId: _component.get('serviceId'),
        isInstalled: false,
        host_id: hosts[_component.get('selectedHost')].id
      });
    });

    this.set('content.masterComponentHosts', masterComponentHosts);
    if (!skip) {
      this.setDBProperty('masterComponentHosts', masterComponentHosts);
    }
  },

  /**
   * Load master component hosts data for using in required step controllers
   * @param inMemory {Boolean}: Load master component hosts from memory
   */
  loadMasterComponentHosts: function (inMemory) {
    var props = this.getDBProperties(['masterComponentHosts', 'hosts']);
    var masterComponentHosts = !!inMemory ? this.get("content.masterComponentHosts") : props.masterComponentHosts,
      hosts = props.hosts || {},
      hostNames = Em.keys(hosts);
    if (Em.isNone(masterComponentHosts)) {
      masterComponentHosts = [];
    } else {
      masterComponentHosts.forEach(function (component) {
        for (var i = 0; i < hostNames.length; i++) {
          if (hosts[hostNames[i]].id === component.host_id) {
            component.hostName = hostNames[i];
            break;
          }
        }
      });
    }
    this.set("content.masterComponentHosts", masterComponentHosts);
  },

  loadCurrentHostGroups: function () {
    this.set("content.recommendationsHostGroups", this.getDBProperty('recommendationsHostGroups'));
  },

  loadRecommendationsConfigs: function () {
    App.router.set("wizardStep7Controller.recommendationsConfigs", this.getDBProperty('recommendationsConfigs'));
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
          for (var i = 0; i < hostNames.length; i++) {
            if (hosts[hostNames[i]].id === host.host_id) {
              host.hostName = hostNames[i];
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
   * @param stepController step4WizardController
   */
  saveClients: function (stepController) {
    var clients = [];
    stepController.get('content').filterProperty('isSelected', true).forEach(function (_service) {
      var client = _service.get('serviceComponents').filterProperty('isClient', true);
      client.forEach(function (clientComponent) {
        clients.pushObject({
          component_name: clientComponent.get('componentName'),
          display_name: clientComponent.get('displayName'),
          isInstalled: false
        });
      }, this);
    }, this);
    this.setDBProperty('clientInfo', clients);
    this.set('content.clients', clients);
  },

  /*
   * Post version definition file (.xml) to server, DRY_RUN = TRUE
   */
  postVersionDefinitionFile: function (isXMLdata, data) {
    var dfd = $.Deferred();
    var name = isXMLdata? 'wizard.step1.post_version_definition_file.xml' : 'wizard.step1.post_version_definition_file.url';

    App.ajax.send({
      name: name,
      sender: this,
      data: {
        dfd: dfd,
        data: data
      },
      success: 'postVersionDefinitionFileSuccessCallback',
      error: 'postVersionDefinitionFileErrorCallback'
    });
    return dfd.promise();
  },

  /**
   * onSuccess callback for postVersionDefinitionFile.
   */
  postVersionDefinitionFileSuccessCallback: function (_data, request, dataInfo) {
    if (_data.resources.length && _data.resources[0].VersionDefinition) {
      var data = _data.resources[0];
      // load the data info to display for details and contents panel
      data.VersionDefinition.id = Em.get(dataInfo, 'data.VersionDefinition.available') || data.VersionDefinition.id;
      var response = {
        id : data.VersionDefinition.id,
        stackVersion : data.VersionDefinition.stack_version,
        stackName: data.VersionDefinition.stack_name,
        type: data.VersionDefinition.type,
        stackNameVersion: data.VersionDefinition.stack_name + '-' + data.VersionDefinition.stack_version, /// HDP-2.3
        actualVersion: data.VersionDefinition.repository_version, /// 2.3.4.0-3846
        version: data.VersionDefinition.release ? data.VersionDefinition.release.version: null, /// 2.3.4.0
        releaseNotes: data.VersionDefinition.release ? data.VersionDefinition.release.notes: null,
        displayName: data.VersionDefinition.release ? data.VersionDefinition.stack_name + '-' + data.VersionDefinition.release.version :
        data.VersionDefinition.stack_name + '-' + data.VersionDefinition.repository_version, //HDP-2.3.4.0
        repoVersionFullName : data.VersionDefinition.stack_name + '-' + data.VersionDefinition.repository_version,
        osList: data.operating_systems,
        updateObj: data
      };
      var services = [];
      data.VersionDefinition.services.forEach(function (service) {
        services.push({
          name: service.name,
          version: service.versions[0].version,
          components: service.versions[0].components
        });
      });
      response.services = services;

      // to display repos panel, should map all available operating systems including empty ones
      var stackInfo = {};
      stackInfo.dfd = dataInfo.dfd;
      stackInfo.response = response;
      this.incrementProperty('loadStacksRequestsCounter');
      this.getSupportedOSListSuccessCallback(data, null, {
        stackName: data.VersionDefinition.stack_name,
        stackVersion: data.VersionDefinition.stack_version,
        versionDefinition: data,
        stackInfo: stackInfo
      });
    }
  },

  /*
   * Post version definition file (.xml) to server in step 8
   */
  postVersionDefinitionFileStep8: function (isXMLdata, data) {
    var dfd = $.Deferred();
    var name = isXMLdata == true? 'wizard.step8.post_version_definition_file.xml' : 'wizard.step8.post_version_definition_file';
    App.ajax.send({
      name: name,
      sender: this,
      data: {
        dfd: dfd,
        data: data
      },
      success: 'postVersionDefinitionFileStep8SuccessCallback',
      error: 'postVersionDefinitionFileErrorCallback'
    });
    return dfd.promise();
  },
  /**
   * onSuccess callback for postVersionDefinitionFile.
   */
  postVersionDefinitionFileStep8SuccessCallback: function (response, request, data) {
    if (response.resources.length && response.resources[0].VersionDefinition) {
      data.dfd.resolve(
        {
          stackName: response.resources[0].VersionDefinition.stack_name,
          id: response.resources[0].VersionDefinition.id,
          stackVersion: response.resources[0].VersionDefinition.stack_version
        });
    }
  },

  /**
   * onError callback for postVersionDefinitionFile.
   */
  postVersionDefinitionFileErrorCallback: function (request, ajaxOptions, error, data, params) {
    params.dfd.reject(data);
    var header = Em.I18n.t('installer.step1.useLocalRepo.uploadFile.error.title');
    var body = '';
    if(request && request.responseText) {
      try {
        var json = $.parseJSON(request.responseText);
        body = json.message;
      } catch (err) {}
    }
    App.db.setLocalRepoVDFData(undefined);
    App.showAlertPopup(header, body);
  },

  getSupportedOSList: function (versionDefinition, stackInfo, dfd) {
    this.incrementProperty('loadStacksRequestsCounter');
    return App.ajax.send({
      name: 'wizard.step1.get_supported_os_types',
      sender: this,
      data: {
        stackName: versionDefinition.VersionDefinition.stack_name,
        stackVersion: versionDefinition.VersionDefinition.stack_version,
        versionDefinition: versionDefinition,
        stackInfo: stackInfo,
        dfd: dfd
      },
      success: 'getSupportedOSListSuccessCallback',
      error: 'getSupportedOSListErrorCallback'
    });
  },

  showStackErrorAndSkipStepIfNeeded: function (response) {
    var stackName = response.Versions.stack_name;
    var stackVersion = response.Versions.stack_version;
    var header = Em.I18n.t('installer.step1.useLocalRepo.getSurpottedOs.stackError.title').format(stackName, stackVersion);
    var body = response.Versions['stack-errors'].join('. ');
    this.decrementProperty('loadStacksRequestsCounter');
    App.showAlertPopup(header, body);
    if (this.get('loadStacksRequestsCounter') === 0 && !App.Stack.find().toArray().length) {
      var wizardStep0Controller = App.router.get('wizardStep0Controller');
      wizardStep0Controller.set('hasNotStacksAvailable', true);
      App.router.send('gotoStep' + 0);
      var installationErrorHeader = Em.I18n.t('installer.step1.useLocalRepo.getSurpottedOs.noStacksError.title');
      var installationErrorBody = Em.I18n.t('installer.step1.useLocalRepo.getSurpottedOs.noStacksError.body');
      App.showAlertPopup(installationErrorHeader, installationErrorBody);
    }
  },

  /**
   * onSuccess callback for getSupportedOSList.
   */
  getSupportedOSListSuccessCallback: function (response, request, data) {
    var self = this;
    var stack_default = data.versionDefinition.VersionDefinition.stack_default;
    var existedOS = data.versionDefinition.operating_systems;
    var existedMap = {};
    existedOS.map(function (existedOS) {
      existedOS.isSelected = true;
      existedMap[existedOS.OperatingSystems.os_type] = existedOS;
    });
    if (response.Versions && response.Versions['stack-errors'] && response.Versions['stack-errors'].length) {
      this.showStackErrorAndSkipStepIfNeeded(response);
      return;
    }
    response.operating_systems.forEach(function(supportedOS) {
      if(!existedMap[supportedOS.OperatingSystems.os_type]) {
        supportedOS.isSelected = false;
        existedOS.push(supportedOS);
      } else {
        if (stack_default) { // only overwrite if it is stack default, otherwise use url from /version_definition
          existedMap[supportedOS.OperatingSystems.os_type].repositories.forEach(function (repo) {
            supportedOS.repositories.forEach(function (supportedRepo) {
              if (supportedRepo.Repositories.repo_id == repo.Repositories.repo_id) {
                repo.Repositories.base_url = supportedRepo.Repositories.base_url;
                repo.Repositories.default_base_url = supportedRepo.Repositories.default_base_url;
                repo.Repositories.latest_base_url = supportedRepo.Repositories.latest_base_url;
                repo.Repositories.components = supportedRepo.Repositories.components;
                repo.Repositories.distribution = supportedRepo.Repositories.distribution;
              }
            });
          });
        }
        else{
          existedMap[supportedOS.OperatingSystems.os_type].repositories.forEach(function (repo) {
            supportedOS.repositories.forEach(function (supportedRepo) {
              if (supportedRepo.Repositories.repo_id == repo.Repositories.repo_id) {
                repo.Repositories.components = supportedRepo.Repositories.components;
                repo.Repositories.distribution = supportedRepo.Repositories.distribution;
              }
            });
          });
        }
      }
    });

    App.stackMapper.map(data.versionDefinition);

    if (!this.decrementProperty('loadStacksRequestsCounter')) {
      if (data.stackInfo.dfd) {
        data.stackInfo.dfd.resolve(data.stackInfo.response);
      } else {
        var versionData = this.getSelectedRepoVersionData();
        if (versionData) {
          this.postVersionDefinitionFile(versionData.isXMLdata, versionData.data).done(function (versionInfo) {
            self.mergeChanges(data.stackInfo.repos, data.stackInfo.oses, data.stackInfo.stacks);
            App.Stack.find().setEach('isSelected', false);
            var stackId = Em.get(versionData, 'data.VersionDefinition.available') || versionInfo.stackNameVersion + "-" + versionInfo.actualVersion;
            App.Stack.find().findProperty('id', stackId).set('isSelected', true);
            self.setSelected(data.stackInfo.isStacksExistInDb, data.dfd);
          }).fail(function () {
            self.setSelected(data.stackInfo.isStacksExistInDb, data.dfd);
          });
        } else {
          this.setSelected(data.stackInfo.isStacksExistInDb, data.dfd);
        }
      }
    }
  },

  /**
   * onError callback for getSupportedOSList
   */
  getSupportedOSListErrorCallback: function (request, ajaxOptions, error, data, params) {
    var header = Em.I18n.t('installer.step1.useLocalRepo.getSurpottedOs.error.title');
    var body = "";
    if(request && request.responseText){
      try {
        var json = $.parseJSON(request.responseText);
        body = json.message;
      } catch (err) {}
    }
    App.showAlertPopup(header, body);
  },

  updateRepoOSInfo: function (repoToUpdate, repo) {
    var deferred = $.Deferred();
    var repoVersion = this.prepareRepoForSaving(repo);
    App.ajax.send({
      name: 'admin.stack_versions.edit.repo',
      sender: this,
      data: {
        stackName: repoToUpdate.stackName,
        stackVersion: repoToUpdate.stackVersion,
        repoVersionId: repoToUpdate.id,
        repoVersion: repoVersion
      }
    }).success(function() {
      deferred.resolve([]);
    }).error(function() {
      deferred.resolve([]);
    });
    return deferred.promise();
  },

  /**
   * transform repo data into json for
   * saving changes to repository version
   * @param {Em.Object} repo
   * @returns {{operating_systems: Array}}
   */
  prepareRepoForSaving: function(repo) {
    var repoVersion = { "operating_systems": [] };
    var ambariManagedRepositories = !repo.get('useRedhatSatellite');
    var k = 0;
    repo.get('operatingSystems').forEach(function (os) {
      if (os.get('isSelected')) {
        repoVersion.operating_systems.push({
          "OperatingSystems": {
            "os_type": os.get("osType"),
            "ambari_managed_repositories": ambariManagedRepositories
          },
          "repositories": []
        });
        os.get('repositories').forEach(function (repository) {
          if (!(repository.get('isGPL') && _.isEmpty(repository.get('baseUrl')))) {
            repoVersion.operating_systems[k].repositories.push({
              "Repositories": {
                "base_url": repository.get('baseUrl'),
                "repo_id": repository.get('repoId'),
                "repo_name": repository.get('repoName'),
                "components": repository.get('components'),
                "tags": repository.get('tags'),
                "distribution": repository.get('distribution'),
                "applicable_services" : repository.get('applicable_services')
              }
            });
          }
        });
        k++;
      }
    });
    return repoVersion;
  },

  /**
   * Check validation of the customized local urls
   */
  checkRepoURL: function (wizardStep1Controller) {
    var selectedStack = this.get('content.stacks').findProperty('isSelected', true);
    selectedStack.set('reload', true);
    var nameVersionCombo = selectedStack.get('stackNameVersion');
    var stackName = nameVersionCombo.split('-')[0];
    var stackVersion = nameVersionCombo.split('-')[1];
    var dfd = $.Deferred();
    if (selectedStack && selectedStack.get('operatingSystems')) {
      this.set('validationCnt', selectedStack.get('operatingSystems').filterProperty('isSelected').filterProperty('isEmpty', false).map(function (os) {
        return os.get('repositories').filterProperty('showRepo', true).length;
      }).reduce(Em.sum, 0));
      var verifyBaseUrl = !wizardStep1Controller.get('skipValidationChecked') && !wizardStep1Controller.get('selectedStack.useRedhatSatellite');
      if (!verifyBaseUrl) {
        dfd.resolve();
      }
      //for redhat satellite/spacewalk the os urls will be empty
      var useRedhatSatellite = wizardStep1Controller.get('selectedStack.useRedhatSatellite');
      selectedStack.get('operatingSystems').forEach(function (os) {
        if (os.get('isSelected') && (useRedhatSatellite || !os.get('isEmpty'))) {
          os.get('repositories').forEach(function (repo) {
            if (repo.get('showRepo')) {
              repo.setProperties({
                errorTitle: '',
                errorContent: '',
                validation: 'INPROGRESS'
              });
              this.set('content.isCheckInProgress', true);
              App.ajax.send({
                name: 'wizard.advanced_repositories.valid_url',
                sender: this,
                data: {
                  stackName: stackName,
                  stackVersion: stackVersion,
                  repoId: repo.get('repoId'),
                  osType: os.get('osType'),
                  osId: os.get('id'),
                  dfd: dfd,
                  data: {
                    'Repositories': {
                      'base_url': repo.get('baseUrl'),
                      'repo_name': repo.get('repoName'),
                      "verify_base_url": verifyBaseUrl
                    }
                  }
                },
                success: 'checkRepoURLSuccessCallback',
                error: 'checkRepoURLErrorCallback'
              });
            }
          }, this);
        } else if (os.get('isSelected') && os.get('isEmpty')) {
          os.set('isSelected', false);
        }
      }, this);
    }
    return dfd.promise();
  },
  /**
   * onSuccess callback for check Repo URL.
   */
  checkRepoURLSuccessCallback: function (response, request, data) {
    var selectedStack = this.get('content.stacks').findProperty('isSelected');
    if (selectedStack && selectedStack.get('operatingSystems')) {
      var os = selectedStack.get('operatingSystems').findProperty('id', data.osId);
      var repo = os.get('repositories').findProperty('repoId', data.repoId);
      if (repo) {
        repo.set('validation', 'OK');
      }
    }
    this.set('validationCnt', this.get('validationCnt') - 1);
    if (!this.get('validationCnt')) {
      this.set('content.isCheckInProgress', false);
      data.dfd.resolve();
    }
  },

  /**
   * onError callback for check Repo URL.
   */
  checkRepoURLErrorCallback: function (request, ajaxOptions, error, data, params) {
    var selectedStack = this.get('content.stacks').findProperty('isSelected', true);
    if (selectedStack && selectedStack.get('operatingSystems')) {
      var os = selectedStack.get('operatingSystems').findProperty('id', params.osId);
      var repo = os.get('repositories').findProperty('repoId', params.repoId);
      if (repo) {
        repo.setProperties({
          validation: 'INVALID',
          errorTitle: request.status + ":" + request.statusText,
          errorContent: $.parseJSON(request.responseText) ? $.parseJSON(request.responseText).message : ""
        });
      }
    }
    this.set('content.isCheckInProgress', false);
    params.dfd.reject();
  },

  loadMap: {
    '0': [
      {
        type: 'sync',
        callback: function () {
          this.load('cluster');
        }
      }
    ],
    '1': [
      {
        type: 'async',
        callback: function () {
          var dfd = $.Deferred();

          this.loadStacks().done(function(stacksLoaded) {
            App.router.get('clusterController').loadAmbariProperties().always(function() {
              dfd.resolve(stacksLoaded);
            });
          });

          return dfd.promise();
        }
      },
      {
        type: 'async',
        callback: function (stacksLoaded) {
          var dfd = $.Deferred();

          if (!stacksLoaded) {
            this.loadStacksVersions().done(function () {
              dfd.resolve(true);
            });
          } else {
            dfd.resolve(stacksLoaded);
          }

          return dfd.promise();
        }
      }
    ],
    '2': [
      {
        type: 'sync',
        callback: function () {
          this.load('installOptions');
        }
      }
    ],
    '3': [
      {
        type: 'sync',
        callback: function () {
          this.loadConfirmedHosts();
        }
      }
    ],
    '4': [
      {
        type: 'async',
        callback: function () {
          return this.loadServices();
        }
      }
    ],
    '5': [
      {
        type: 'sync',
        callback: function () {
          this.setSkipSlavesStep(App.StackService.find().filterProperty('isSelected'), 6);
          this.loadMasterComponentHosts();
          this.loadConfirmedHosts();
          this.loadComponentsFromConfigs();
          this.loadRecommendations();
        }
      }
    ],
    '6': [
      {
        type: 'sync',
        callback: function () {
          this.loadSlaveComponentHosts();
          this.loadClients();
          this.loadComponentsFromConfigs();
          this.loadRecommendations();
        }
      }
    ],
    '7': [
      {
        type: 'async',
        callback: function () {
          var dfd = $.Deferred();
          var self = this;
          this.loadServiceConfigGroups();
          this.loadCurrentHostGroups();
          this.loadRecommendationsConfigs();
          this.loadComponentsFromConfigs();
          this.loadConfigThemes().then(function() {
            self.loadServiceConfigProperties();
            dfd.resolve();
          });
          return dfd.promise();
        }
      }
    ]
  },


  clearConfigActionComponents: function() {
    var masterComponentHosts = this.get('content.masterComponentHosts');
    var componentsAddedFromConfigAction = this.get('content.componentsFromConfigs');

    if (componentsAddedFromConfigAction && componentsAddedFromConfigAction.length) {
      componentsAddedFromConfigAction.forEach(function(_masterComponent){
        masterComponentHosts = masterComponentHosts.rejectProperty('component', _masterComponent);
      });
    }
    this.set('content.masterComponentHosts', masterComponentHosts);
    this.setDBProperty('masterComponentHosts', masterComponentHosts);
  },


  /**
   * Clear all temporary data
   */
  finish: function () {
    this.setCurrentStep('0');
    this.clearStorageData();
    this.clearServiceConfigProperties();
    App.router.get('userSettingsController').postUserPref('show_bg', true);
    App.themesMapper.resetModels();
  },

  /**
   * Save cluster provisioning state to the server
   * @param state cluster provisioning state
   */
  setClusterProvisioningState: function (state) {
    return App.ajax.send({
      name: 'cluster.save_provisioning_state',
      sender: this,
      data: {
        state: state
      }
    });
  },

  setStepsEnable: function () {
    for (var i = 0; i <= this.totalSteps; i++) {
      this.get('isStepDisabled').findProperty('step', i).set('value', i > this.get('currentStep'));
    }
  }.observes('currentStep'),

  setLowerStepsDisable: function (stepNo) {
    for (var i = 0; i < stepNo; i++) {
      var step = this.get('isStepDisabled').findProperty('step', i);
      step.set('value', true);
    }
  },


  /**
   * Compare jdk versions used for ambari and selected stack.
   * Validation check will fire only for non-custom jdk configuration.
   *
   * @param {Function} successCallback
   * @param {Function} failCallback
   */
  validateJDKVersion: function (successCallback, failCallback) {
    var selectedStack = App.Stack.find().findProperty('isSelected', true),
        currentJDKVersion = App.router.get('clusterController.ambariProperties')['java.version'],
        // use min as max, or max as min version, in case when some of them missed
        minJDKVersion = selectedStack.get('minJdkVersion') || selectedStack.get('maxJdkVersion'),
        maxJDKVersion = selectedStack.get('maxJdkVersion') || selectedStack.get('minJdkVersion'),
        t = Em.I18n.t,
        fCallback = failCallback || function() {},
        sCallback = successCallback || function() {};

    // Skip jdk check if min and max required version not set in stack definition.
    if (!minJDKVersion && !maxJDKVersion) {
      sCallback();
      return;
    }

    if (currentJDKVersion) {
      if (stringUtils.compareVersions(currentJDKVersion, minJDKVersion) < 0 ||
          stringUtils.compareVersions(maxJDKVersion, currentJDKVersion) < 0) {
        // checks and process only minor part for now
        var versionDistance = parseInt(maxJDKVersion.split('.')[1], 10) - parseInt(minJDKVersion.split('.')[1], 10);
        var versionsList = [minJDKVersion];
        for (var i = 1; i < versionDistance + 1; i++) {
          versionsList.push("" + minJDKVersion.split('.')[0] + '.' + (+minJDKVersion.split('.')[1] + i));
        }
        var versionsString = stringUtils.getFormattedStringFromArray(versionsList, t('or'));
        var popupBody = t('popup.jdkValidation.body').format(selectedStack.get('stackName') + ' ' + selectedStack.get('stackVersion'), versionsString, currentJDKVersion);
        App.showConfirmationPopup(sCallback, popupBody, fCallback, t('popup.jdkValidation.header'), t('common.proceedAnyway'), 'danger');
        return;
      }
    }
    sCallback();
  }

});
