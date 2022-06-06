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

/**
 * By Step 7, we have the following information stored in App.db and set on this
 * controller by the router.
 *
 *   selectedServices: App.db.selectedServices (the services that the user selected in Step 4)
 *   masterComponentHosts: App.db.masterComponentHosts (master-components-to-hosts mapping the user selected in Step 5)
 *   slaveComponentHosts: App.db.slaveComponentHosts (slave-components-to-hosts mapping the user selected in Step 6)
 *
 */

/**
 * @typedef {object} masterComponentHost
 * @property {string} component
 * @property {string} hostName
 * @property {boolean} isInstalled is component already installed on the this host or just going to be installed
 */

/**
 * @typedef {object} topologyLocalDB
 * @property {object[]} hosts list of hosts with information of their disks usage and dirs
 * @property {masterComponentHost[]} masterComponentHosts
 * @property {?object[]} slaveComponentHosts
 */

App.WizardStep7Controller = Em.Controller.extend(App.ServerValidatorMixin, App.EnhancedConfigsMixin, App.ToggleIsRequiredMixin, App.GroupsMappingMixin, App.AddSecurityConfigs, App.KDCCredentialsControllerMixin, {

  name: 'wizardStep7Controller',

  /**
   * Contains all field properties that are viewed in this step
   * @type {object[]}
   */
  stepConfigs: [],

  hash: null,

  selectedService: null,

  addMiscTabToPage: true,

  /**
   * Is Submit-click processing now
   * @type {bool}
   */
  submitButtonClicked: false,

  isRecommendedLoaded: false,

  /**
   * Indicates if all stepConfig objects are created
   */
  stepConfigsCreated: false,

  /**
   * Define state of next button on credentials tab
   */
  credentialsTabNextEnabled: false,

  /**
   * Define state of next button on databases tab
   */
  databasesTabNextEnabled: false,

  /**
   * used in services_config.js view to mark a config with security icon
   */
  secureConfigs: require('data/configs/wizards/secure_mapping'),

  /**
   * If configChangeObserver Modal is shown
   * @type {bool}
   */
  miscModalVisible: false,

  overrideToAdd: null,

  isInstaller: true, //todo: refactor using of this property

  /**
   * Is installer controller used
   * @type {bool}
   */
  isInstallWizard: Em.computed.equal('content.controllerName', 'installerController'),

  /**
   * Is add service controller used
   * @type {bool}
   */
  isAddServiceWizard: Em.computed.equal('content.controllerName', 'addServiceController'),

  /**
   * List of config groups
   * @type {object[]}
   */
  configGroups: [],

  /**
   * List of config group to be deleted
   * @type {object[]}
   */
  groupsToDelete: [],

  preSelectedConfigGroup: null,

  /**
   * Currently selected config group
   * @type {object}
   */
  selectedConfigGroup: null,

  /**
   * Config tags of actually installed services
   * @type {array}
   */
  serviceConfigTags: [],

  /**
   * Are applied to service configs loaded
   * @type {bool}
   */
  isAppliedConfigLoaded: true,

  /**
   * Is there validation issues
   * @type {bool}
   */
  hasErrors: false,

  /**
   * Total number of recommendation and validation issues
   * @type {number}
   */
  issuesCounter: Em.computed.sumProperties('validationsCounter', 'suggestionsCounter'),

  /**
   * Number of ui-side validation issues
   * @type {number}
   */
  validationsCounter: 0,

  /**
   * Number of ui-side suggestion issues
   * @type {number}
   */
  suggestionsCounter: 0,

  /**
   * Tab objects to represent each config category tab
   */
  tabs: [],

  isConfigsLoaded: Em.computed.and('wizardController.stackConfigsLoaded', 'isAppliedConfigLoaded'),

  transitionInProgress: Em.computed.alias('App.router.btnClickInProgress'),

  /**
   * PreInstall Checks allowed only for Install
   * @type {boolean}
   */
  supportsPreInstallChecks: function () {
    return App.get('supports.preInstallChecks') && this.get('isInstallWizard');
  }.property('App.supports.preInstallChecks', 'wizardController.name'),

  /**
   * Number of errors in the configs in the selected service
   * @type {number}
   */
  errorsCount: function() {
    return this.get('selectedService.configsWithErrors').filter(function(c) {
      return Em.isNone(c.get('isInDefaultTheme'));
    }).length;
  }.property('selectedService.configsWithErrors.length'),

  /**
   * Should Next-button be disabled
   * @type {bool}
   */
  isSubmitDisabled: function () {
    if (!this.get('stepConfigs.length')) return true;
    if (this.get('submitButtonClicked')) return true;
    if (App.get('router.btnClickInProgress')) return true;
    return !this.get('stepConfigs').filterProperty('showConfig', true).everyProperty('errorCount', 0)
      || this.get("miscModalVisible")
      || !!this.get('configErrorList.criticalIssues.length');
  }.property('stepConfigs.@each.errorCount', 'miscModalVisible', 'submitButtonClicked', 'App.router.btnClickInProgress', 'configErrorList.criticalIssues.length'),

  /**
   * List of selected to install service names
   * @type {string[]}
   */
  selectedServiceNames: function () {
    return this.get('content.services').filterProperty('isSelected', true).filterProperty('isInstalled', false).mapProperty('serviceName');
  }.property('content.services', 'content.services.@each.isSelected', 'content.services.@each.isInstalled', 'content.stacks.@each.isSelected').cacheable(),

  /**
   * List of installed and selected to install service names
   * @type {string[]}
   */
  allSelectedServiceNames: function () {
    return this.get('content.services').filter(function (service) {
      return service.get('isInstalled') || service.get('isSelected');
    }).mapProperty('serviceName');
  }.property('content.services', 'content.services.@each.isSelected', 'content.services.@each.isInstalled', 'content.stacks.@each.isSelected').cacheable(),

  /**
   * List of installed service names
   * @type {string[]}
   */
  installedServiceNames: function () {
    var serviceNames = this.get('content.services').filterProperty('isInstalled').mapProperty('serviceName');
    if (!this.get('isInstallWizard')) {
      serviceNames = serviceNames.filter(function (_serviceName) {
        return !App.get('services.noConfigTypes').contains(_serviceName);
      });
    }
    return serviceNames;
  }.property('content.services').cacheable(),

  installedServices: function () {
    var self = this;
    return App.StackService.find().toArray().toMapByCallback('serviceName', function (item) {
      return self.get('installedServiceNames').contains(item.get('serviceName'));
    });
  }.property('installedServiceNames.length'),

  requiredChanges: function () {
    return this.get('recommendations').filterProperty('isEditable', false);
  }.property('recommendatios.@each.isEditable'),

  /**
   * List of master components
   * @type {Ember.Enumerable}
   */
  masterComponentHosts: Em.computed.alias('content.masterComponentHosts'),

  /**
   * List of slave components
   * @type {Ember.Enumerable}
   */
  slaveComponentHosts: Em.computed.alias('content.slaveGroupProperties'),

  /**
   * Filter text will be located here
   * @type {string}
   */
  filter: '',

  /**
   * Defines if configs can be editable
   * @type {boolean}
   */
  canEdit: true,

  /**
   * list of dependencies that are user to set init value of config
   *
   * @type {Object}
   */
  configDependencies: function() {
    var dependencies = {};
    var hiveMetastore = App.configsCollection.getConfigByName('hive.metastore.uris', 'hive-site.xml');
    var clientPort = App.configsCollection.getConfigByName('clientPort', 'zoo.cfg.xml');
    var atlasTls = App.configsCollection.getConfigByName('atlas.enableTLS', 'application-properties.xml');
    var atlasHttpPort = App.configsCollection.getConfigByName('atlas.server.http.port', 'application-properties.xml');
    var atlasHttpsPort = App.configsCollection.getConfigByName('atlas.server.https.port', 'application-properties.xml');

    if (hiveMetastore) dependencies['hive.metastore.uris'] = hiveMetastore.recommendedValue;
    if (clientPort) dependencies.clientPort = clientPort.recommendedValue;
    if (atlasTls) dependencies['atlas.enableTLS'] = atlasTls.recommendedValue;
    if (atlasHttpPort) dependencies['atlas.server.http.port'] = atlasHttpPort.recommendedValue;
    if (atlasHttpsPort) dependencies['atlas.server.https.port'] = atlasHttpsPort.recommendedValue;
    return dependencies;
  }.property(),

  /**
   * List of filters for config properties to populate filter combobox
   */
  propertyFilters: [
    {
      attributeName: 'isOverridden',
      attributeValue: true,
      caption: 'common.combobox.dropdown.overridden'
    },
    {
      attributeName: 'isFinal',
      attributeValue: true,
      caption: 'common.combobox.dropdown.final'
    },
    {
      attributeName: 'hasIssues',
      attributeValue: true,
      caption: 'common.combobox.dropdown.issues'
    }
  ],

  issuesFilterText: function () {
    return !this.get('transitionInProgress') && this.get('issuesFilterSelected') ?
        Em.I18n.t('installer.step7.showingPropertiesWithIssues') : '';
  }.property('isSubmitDisabled', 'submitButtonClicked', 'filterColumns.@each.selected'),

  /**
   * @type {string}
   */
  issuesFilterLinkText: function () {
    var issuesAttrSelected = this.get('issuesFilterSelected');
    if (!this.get('transitionInProgress')) {
      if (issuesAttrSelected) {
        return Em.I18n.t('installer.step7.showAllProperties');
      }
      if (this.get('hasStepConfigIssues')) {
        return Em.I18n.t('installer.step7.showPropertiesWithIssues');
      }
    }
    return '';
  }.property('isSubmitDisabled', 'submitButtonClicked', 'filterColumns.@each.selected'),

  /**
   * Dropdown menu items in filter combobox
   */
  filterColumns: function () {
    return this.get('propertyFilters').map(function (filter) {
      return Ember.Object.create({
        attributeName: filter.attributeName,
        attributeValue: filter.attributeValue,
        name: this.t(filter.caption),
        selected: false
      });
    }, this);
  }.property('propertyFilters'),

  /**
   * Clear controller's properties:
   *  <ul>
   *    <li>stepConfigs</li>
   *    <li>filter</li>
   *  </ul>
   *  and deselect all <code>filterColumns</code>
   * @method clearStep
   */
  clearStep: function () {
    this.abortRequests();
    this.setProperties({
      configValidationGlobalMessage: [],
      submitButtonClicked: false,
      isSubmitDisabled: true,
      isRecommendedLoaded: false,
      stepConfigsCreated: false,
      initialRecommendations: []
    });
    App.ServiceConfigGroup.find().filterProperty('isDefault', false).forEach(function (record) {
      App.configGroupsMapper.deleteRecord(record);
    });
    this.get('stepConfigs').clear();
    this.set('filter', '');
    this.get('filterColumns').setEach('selected', false);
  },

  clearLastSelectedService: function () {
    this.get('tabs').filterProperty('selectedServiceName').setEach('selectedServiceName', null);
  },

  /**
   * Generate "finger-print" for current <code>stepConfigs[0]</code>
   * Used to determine, if user has some unsaved changes (comparing with <code>hash</code>)
   * @returns {string|null}
   * @method getHash
   */
  getHash: function () {
    if (!this.get('stepConfigs')[0]) {
      return null;
    }
    var hash = {};
    this.get('stepConfigs').forEach(function(stepConfig){
      stepConfig.configs.forEach(function (config) {
        hash[config.get('name')] = {value: config.get('value'), overrides: [], isFinal: config.get('isFinal')};
        if (!config.get('overrides')) return;
        if (!config.get('overrides.length')) return;

        config.get('overrides').forEach(function (override) {
          hash[config.get('name')].overrides.push(override.get('value'));
        });
      });
    });
    return JSON.stringify(hash);
  },

   /**
   * Are some changes available
   */
  hasChanges: function () {
    return this.get('hash') !== this.getHash();
  },

  /**
   * load all overrides for all config groups
   *
   * @returns {*|$.ajax}
   */
  loadOverrides: function() {
    return App.ajax.send({
      name: 'service.serviceConfigVersions.get.current.not.default',
      sender: this,
      success: 'parseOverrides'
    })
  },

  /**
   * add overriden configs to stepConfigs
   *
   * @param data
   */
  parseOverrides: function(data) {
    var self = this;
    data.items.forEach(function(group) {
      var stepConfig = self.get('stepConfigs').findProperty('serviceName', group.service_name),
        serviceConfigs = stepConfig.get('configs'),
        configGroup = App.ServiceConfigGroup.find().filterProperty('serviceName', group.service_name).findProperty('name', group.group_name);

      var isEditable = self.get('canEdit') && configGroup.get('name') === stepConfig.get('selectedConfigGroup.name');

      group.configurations.forEach(function (config) {
        for (var prop in config.properties) {
          var fileName = App.config.getOriginalFileName(config.type);
          var serviceConfig = serviceConfigs.filterProperty('name', prop).findProperty('filename', fileName);
          if (serviceConfig && serviceConfig.get('isOriginalSCP')) {
            var value = App.config.formatPropertyValue(serviceConfig, config.properties[prop]);
            var isFinal = !!(config.properties_attributes && config.properties_attributes.final && config.properties_attributes.final[prop]);

            App.config.createOverride(serviceConfig, {
              "value": value,
              "savedValue": value,
              "isFinal": isFinal,
              "savedIsFinal": isFinal,
              "isEditable": isEditable
            }, configGroup);

          } else {

            serviceConfigs.push(App.config.createCustomGroupConfig({
              propertyName: prop,
              filename: fileName,
              value: config.properties[prop],
              savedValue: config.properties[prop],
              isEditable: isEditable
            }, configGroup));

          }
        }
      });
    });
    this.onLoadOverrides();
  },

  onLoadOverrides: function () {
    this.get('stepConfigs').forEach(function(stepConfig) {
      stepConfig.set('configGroups', App.ServiceConfigGroup.find().filterProperty('serviceName', stepConfig.get('serviceName')));

      stepConfig.get('configGroups').filterProperty('isDefault', false).forEach(function (configGroup) {
        configGroup.set('hash', this.get('wizardController').getConfigGroupHash(configGroup));
      }, this);

      this.addOverride(stepConfig);
      // override if a property isRequired or not
      this.overrideConfigIsRequired(stepConfig);
    }, this);
    console.timeEnd('loadConfigGroups execution time: ');
  },

  /**
   * Set <code>isEditable</code>-property to <code>serviceConfigProperty</code>
   * Based on user's permissions and selected config group
   * @param {Ember.Object} serviceConfigProperty
   * @param {bool} defaultGroupSelected
   * @returns {Ember.Object} Updated config-object
   * @method _updateIsEditableFlagForConfig
   */
  _updateIsEditableFlagForConfig: function (serviceConfigProperty, defaultGroupSelected) {
    if (App.isAuthorized('AMBARI.ADD_DELETE_CLUSTERS')) {
      if (App.get('isKerberosEnabled') &&
          serviceConfigProperty.get('isConfigIdentity') &&
          !App.StackService.find().filterProperty('isSelected').mapProperty('serviceName').contains(Em.get(serviceConfigProperty, 'serviceName'))) {
        serviceConfigProperty.set('isEditable', false);
      } else if (defaultGroupSelected && !Em.get(serviceConfigProperty, 'group')) {
        if (serviceConfigProperty.get('serviceName') === 'MISC') {
          var service = App.config.get('serviceByConfigTypeMap')[App.config.getConfigTagFromFileName(serviceConfigProperty.get('filename'))];
          serviceConfigProperty.set('isEditable', service && !this.get('installedServiceNames').contains(service.get('serviceName')));
        } else {
          serviceConfigProperty.set('isEditable', serviceConfigProperty.get('isEditable') && serviceConfigProperty.get('isReconfigurable'));
        }
      } else if (!(Em.get(serviceConfigProperty, 'group') && Em.get(serviceConfigProperty, 'group.name') === this.get('selectedConfigGroup.name'))) {
        serviceConfigProperty.set('isEditable', false);
      }
    } else {
      serviceConfigProperty.set('isEditable', false);
    }
    return serviceConfigProperty;
  },

  /**
   * Set configs with overrides, recommended defaults to stepConfig
   * @param {Ember.Object} stepConfig
   * @method loadComponentConfigs
   */
  addOverride: function (stepConfig) {
    var overrideToAdd = this.get('overrideToAdd');
    if (overrideToAdd) {
      overrideToAdd = stepConfig.get('configs').filterProperty('filename', overrideToAdd.filename)
        .findProperty('name', overrideToAdd.name);
      if (overrideToAdd) {
        App.config.createOverride(overrideToAdd, {isEditable: true}, stepConfig.get('selectedConfigGroup'));
        this.set('overrideToAdd', null);
      }
    }
  },

  /**
   * On load function
   * @method loadStep
   */
  loadStep: function () {
    if (!this.get('isConfigsLoaded')) {
      return;
    }
    console.time('wizard loadStep: ');
    this.clearStep();

    var self = this;
    App.config.setPreDefinedServiceConfigs(this.get('addMiscTabToPage'));

    var storedConfigs = this.get('content.serviceConfigProperties');
    var configs = storedConfigs && storedConfigs.length ? storedConfigs : App.configsCollection.getAll();

    this.set('groupsToDelete', this.get('wizardController').getDBProperty('groupsToDelete') || []);
    if (this.get('wizardController.name') === 'addServiceController' && !storedConfigs) {
      App.router.get('configurationController').getConfigsByTags(this.get('serviceConfigTags')).done(function (loadedConfigs) {
        configs = self.setInstalledServiceConfigs(configs, loadedConfigs, self.get('installedServiceNames'));
        self.applyServicesConfigs(configs);
      });
    } else {
      this.applyServicesConfigs(configs, true);
    }
  },

  /**
   * Update hawq configuration depending on the state of the cluster
   * @param {Array} configs
   */
  updateHawqConfigs: function (configs) {
    if (this.get('wizardController.name') === 'addServiceController') {
      if (App.get('isHaEnabled')) this.addHawqConfigsOnNnHa(configs);
      if (App.get('isRMHaEnabled')) this.addHawqConfigsOnRMHa(configs);
    }
    if (this.get('content.hosts') && Object.keys(this.get('content.hosts')).length === 1) this.removeHawqStandbyHostAddressConfig(configs);
    return configs
  },

  /**
   * Remove hawq_standby_address_host config from HAWQ configs
   * @param {Array} configs
   */
  removeHawqStandbyHostAddressConfig: function(configs) {
    var hawqStandbyAddressHostIndex = configs.indexOf(configs.findProperty('name', 'hawq_standby_address_host'));
    if (hawqStandbyAddressHostIndex > -1) configs.removeAt(hawqStandbyAddressHostIndex) ;
    return configs
  },

  applyServicesConfigs: function (configs, isRestoring) {
    var self = this;
    console.time('applyServicesConfigs execution time: ');
    if (!this.get('installedServiceNames').contains('HAWQ') && this.get('allSelectedServiceNames').contains('HAWQ')) {
      this.updateHawqConfigs(configs);
    }
    if (App.get('isKerberosEnabled') && this.get('wizardController.name') === 'addServiceController') {
      this.addKerberosDescriptorConfigs(configs, this.get('wizardController.kerberosDescriptorConfigs') || [], isRestoring);
    }
    var stepConfigs = this.createStepConfigs();
    var serviceConfigs = this.renderConfigs(stepConfigs, configs);
    this.addUidAndGidRepresentations(serviceConfigs);
    // if HA is enabled -> Make some reconfigurations
    if (this.get('wizardController.name') === 'addServiceController') {
      this.updateComponentActionConfigs(configs, serviceConfigs);
      if (App.get('isHaEnabled')) {
        serviceConfigs = this._reconfigureServicesOnNnHa(serviceConfigs);
      }
    }

    var rangerService = App.StackService.find().findProperty('serviceName', 'RANGER');
    const isRangerServicePresent = rangerService && (rangerService.get('isInstalled') || rangerService.get('isSelected'));
    var infraSolrService = App.StackService.find().findProperty('serviceName', 'AMBARI_INFRA_SOLR');
    const isInfraSolrPresent = infraSolrService && (infraSolrService.get('isInstalled') || infraSolrService.get('isSelected'));
    if(isRangerServicePresent && (this.get('wizardController.name') === 'installerController' || this.get('wizardController.name') === 'addServiceController')) {
      this.setRangerPluginsEnabled(serviceConfigs);
      if (isInfraSolrPresent) {
        this.setSolrCloudOn(serviceConfigs);
      }
    }

    this.set('stepConfigs', serviceConfigs);
    this.set('stepConfigsCreated', true);
    this.updateConfigAttributesFromThemes();
    this.checkHostOverrideInstaller();
    this.selectProperService();
    var isInstallerWizard = this.get('isInstallWizard');
    var isRangerServiceAbsent =  rangerService && !rangerService.get('isInstalled') && !rangerService.get('isSelected');
    if (isRangerServiceAbsent) {
      var isExternalRangerSetup;
      if (isInstallerWizard) {
        isExternalRangerSetup = configs.filterProperty('fileName','cluster-env.xml').findProperty('name','enable_external_ranger');
        if (Em.isNone(isExternalRangerSetup) || isExternalRangerSetup.value !== "true") {
          App.config.removeRangerConfigs(this.get('stepConfigs'));
        }
        console.timeEnd('applyServicesConfigs execution time: ');
        console.time('loadConfigRecommendations execution time: ');
        self.loadConfigRecommendations(null, self.completeConfigLoading.bind(self));
      } else {
        var mainController = App.get('router.mainController');
        var clusterController = App.get('router.clusterController');
        mainController.isLoading.call(clusterController, 'clusterEnv').done(function () {
          isExternalRangerSetup = clusterController.get("clusterEnv")["properties"]["enable_external_ranger"];
          if (isExternalRangerSetup !== "true") {
            App.config.removeRangerConfigs(self.get('stepConfigs'));
          }
          console.timeEnd('applyServicesConfigs execution time: ');
          console.time('loadConfigRecommendations execution time: ');
          self.loadConfigRecommendations(null, self.completeConfigLoading.bind(self));
        });
      }

    } else {
      console.timeEnd('applyServicesConfigs execution time: ');
      console.time('loadConfigRecommendations execution time: ');
      self.loadConfigRecommendations(null, self.completeConfigLoading.bind(self));
    }

  },

  /**
  * Sets the value of ranger-<service_name>-plugin-enabled to "Yes" if ranger authorization /
  * is supported for that service.
  * @param stepConfigs Object[]
  */
  setRangerPluginsEnabled: function(stepConfigs) {
    var rangerServiceConfigs = stepConfigs.findProperty('serviceName', 'RANGER').get('configs');
    var services = this.get('selectedServiceNames').filter(service => service != 'RANGER');

    services.forEach(function(serviceName) {
      var pluginEnabledPropertyName = 'ranger-' + serviceName.toLowerCase() + '-plugin-enabled';
      var pluginEnabledProperty = rangerServiceConfigs.findProperty('name', pluginEnabledPropertyName);
      //Kafka and Storm plugins need to be enabled only if cluster is kerberized
      if (pluginEnabledProperty && (serviceName === 'STORM' || serviceName === 'KAFKA')) {
        if (App.get('isKerberosEnabled')) {
          Em.set(pluginEnabledProperty, 'value', 'Yes');
        }
      } else if (pluginEnabledProperty) {
        Em.set(pluginEnabledProperty, 'value', 'Yes');
      }
    });
  },

  setSolrCloudOn: function(stepConfigs) {
    var rangerServiceConfigs = stepConfigs.findProperty('serviceName', 'RANGER').get('configs');
    var solrCloudEnabledProperty = rangerServiceConfigs.findProperty('name', 'is_solrCloud_enabled');
    Em.set(solrCloudEnabledProperty, 'value', 'true');
  },

  /**
   *
   * Makes installed service's configs resulting into component actions (add/delete) non editable on Add Service Wizard
   * @param configs Object[]
   * @param  stepConfigs Object[]
   * @private
   * @method updateComponentActionConfigs
   */
  updateComponentActionConfigs: function(configs, stepConfigs) {
    App.ConfigAction.find().forEach(function(item){
      var configName = item.get('configName');
      var fileName = item.get('fileName');
      var config = configs.filterProperty('filename', fileName).findProperty('name', configName);
      if (config) {
        var isServiceInstalled = App.Service.find().findProperty('serviceName', config.serviceName);
        // service already installed or is being added in add service wizard
        if (isServiceInstalled || stepConfigs.someProperty("serviceName", config.serviceName)) {
          var serviceConfigs = stepConfigs.findProperty('serviceName', config.serviceName).get('configs');
          var serviceConfig = serviceConfigs.filterProperty('filename', fileName).findProperty('name', configName);
          var notEditableText = " " + Em.I18n.t('installer.step7.addWizard.notEditable');
          serviceConfig.set('description', serviceConfig.get('description') + notEditableText);
          serviceConfig.set('isReconfigurable', false);
          serviceConfig.set('isEditable', false);
          serviceConfig.set('disabledAsComponentAction', true);
          config.isReconfigurable = false;
          config.isEditable = false;
          config.disabledAsComponentActio = true;
        }
      }
    }, this);
  },

  completeConfigLoading: function() {
    this.get('stepConfigs').forEach(function(service) {
      App.configTheme.resolveConfigThemeConditions(service.get('configs'));
    });
    this.clearRecommendationsByServiceName(App.StackService.find().filter(function (s) {
      return s.get('isSelected') && !s.get('isInstalled');
    }).mapProperty('serviceName'));
    this.saveInitialRecommendations();
    this.set('isRecommendedLoaded', true);
    console.timeEnd('loadConfigRecommendations execution time: ');
    console.timeEnd('wizard loadStep: ');
    if (this.get('content.skipConfigStep')) {
      App.router.send('next');
    }
    this.set('hash', this.getHash());
  },

  /**
   * Update initialValues only while loading recommendations first time
   *
   * @param serviceName
   * @returns {boolean}
   * @override
   */
  updateInitialOnRecommendations: function(serviceName) {
    return this._super(serviceName) && !this.get('isRecommendedLoaded');
  },

  /**
   * Mark descriptor properties in configuration object.
   *
   * @param {Object[]} configs - config properties to change
   * @param {App.ServiceConfigProperty[]} descriptor - parsed kerberos descriptor
   * @method addKerberosDescriptorConfigs
   */
  addKerberosDescriptorConfigs: function (configs, descriptor, isRestoring) {
    var servicesToBeInstalled = this.get('content.services').filterProperty('isSelected').mapProperty('serviceName');
    descriptor.forEach(function (item) {
      var service = item.get('serviceName');
      if (!servicesToBeInstalled.contains(service)) return false;
      var name = item.get('name');
      var filename = Em.get(item, 'filename');
      var property = configs.filterProperty('serviceName', service).findProperty('name', name);
      var propertyObj = {
        isSecureConfig: true,
        value: Em.get(isRestoring && property ? property : item, 'value'),
        defaultValue: Em.get(item, 'value'),
        displayName: name,
        isOverridable: false,
        isConfigIdentity: Em.get(item, 'isConfigIdentity'),
        isUserProperty: !!Em.get(item, 'isUserProperty'),
        category: (Em.get(item, 'isUserProperty') ? 'Custom ' : 'Advanced ') + filename
      };
      if (property) {
        Em.setProperties(property, $.extend(propertyObj, {
          savedValue: Em.get(item, 'value'),
          description: Em.get(item, 'isConfigIdentity')
              ? App.config.kerberosIdentitiesDescription(Em.get(property, 'description'))
              : Em.get(property, 'description')
        }));
      } else {
        configs.push(App.ServiceConfigProperty.create(App.config.getDefaultConfig(name, filename, propertyObj)));
      }
    });
  },

  /**
   * Load config groups
   * and (if some services are already installed) load config groups for installed services
   * @method checkHostOverrideInstaller
   */
  checkHostOverrideInstaller: function () {
    if (this.get('wizardController.name') !== 'kerberosWizardController' && this.get('content.configGroups.length')) {
      this.restoreConfigGroups(this.get('content.configGroups'));
    } else {
      if (this.get('installedServiceNames').length > 0 && !this.get('wizardController.areInstalledConfigGroupsLoaded')) {
        console.time('loadConfigGroups execution time: ');
        this.loadConfigGroups(this.get('allSelectedServiceNames')).done(this.loadOverrides.bind(this));
      } else {
        App.store.fastCommit();
        App.configGroupsMapper.map(null, false, this.get('allSelectedServiceNames'));
        this.onLoadOverrides();
      }
    }
  },

  /**
   * Create stepConfigs array with all info except configs list
   *
   * @return {Object[]}
   * @method createStepConfigs
   */
  createStepConfigs: function() {
    var stepConfigs = [];
    App.config.get('preDefinedServiceConfigs').forEach(function (service) {
      var serviceName = service.get('serviceName');
      if (['MISC'].concat(this.get('allSelectedServiceNames')).contains(serviceName)) {
        var serviceConfig = App.config.createServiceConfig(serviceName);
        serviceConfig.set('showConfig', App.StackService.find(serviceName).get('isInstallable'));
        if (this.get('wizardController.name') === 'addServiceController') {
          serviceConfig.set('selected', !this.get('installedServiceNames').concat('MISC').contains(serviceName));
          if (serviceName === 'MISC') {
            serviceConfig.set('configCategories', serviceConfig.get('configCategories').rejectProperty('name', 'Notifications'));
          }
        } else if (this.get('wizardController.name') === 'kerberosWizardController') {
          serviceConfig.set('showConfig', true);
        }
        stepConfigs.pushObject(serviceConfig);
      }
    }, this);
    return stepConfigs;
  },


  /**
   * For Namenode HA, HAWQ service requires additional config parameters in hdfs-client.xml
   * This method ensures that these additional parameters are added to hdfs-client.xml
   * @param configs existing configs on cluster
   * @returns {Object[]} existing configs + additional config parameters in hdfs-client.xml
   */
  addHawqConfigsOnNnHa: function(configs) {
    var nameService = configs.findProperty('id', App.config.configId('dfs.nameservices', 'hdfs-site')).value;
    var propertyNames = [
      'dfs.nameservices',
      'dfs.ha.namenodes.' + nameService,
      'dfs.namenode.rpc-address.'+ nameService +'.nn1',
      'dfs.namenode.rpc-address.'+ nameService +'.nn2',
      'dfs.namenode.http-address.'+ nameService +'.nn1',
      'dfs.namenode.http-address.'+ nameService +'.nn2'
    ];

    propertyNames.forEach(function(propertyName, propertyIndex) {
      var propertyFromHdfs = configs.findProperty('id', App.config.configId(propertyName, 'hdfs-site'));
      var newProperty = App.config.createDefaultConfig(propertyName, 'hdfs-client.xml', true);
      Em.setProperties(newProperty, {
        serviceName: 'HAWQ',
        description: propertyFromHdfs.description,
        displayName: propertyFromHdfs.displayName,
        displayType: 'string',
        index: propertyIndex,
        isOverridable: false,
        isReconfigurable: false,
        value: propertyFromHdfs.value,
        recommendedValue: propertyFromHdfs.recommendedValue
      });

      configs.push(App.ServiceConfigProperty.create(newProperty));
    });
    return configs;
  },

  /**
   * For ResourceManager HA, HAWQ service requires additional config parameters in yarn-client.xml
   * This method ensures that these additional parameters are added to yarn-client.xml
   * @param configs existing configs on cluster
   * @returns {Object[]} existing configs + additional config parameters in yarn-client.xml
   */
  addHawqConfigsOnRMHa: function(configs) {
    var rmHost1 = configs.findProperty('id', App.config.configId('yarn.resourcemanager.hostname.rm1', 'yarn-site')).value ;
    var rmHost2 = configs.findProperty('id', App.config.configId('yarn.resourcemanager.hostname.rm2', 'yarn-site')).value ;
    var yarnConfigToBeAdded = [
      {
        name: 'yarn.resourcemanager.ha',
        displayName: 'yarn.resourcemanager.ha',
        description: 'Comma separated yarn resourcemanager host addresses with port',
        port: '8032'
      },
      {
        name: 'yarn.resourcemanager.scheduler.ha',
        displayName: 'yarn.resourcemanager.scheduler.ha',
        description: 'Comma separated yarn resourcemanager scheduler addresses with port',
        port: '8030'
      }
    ];

    yarnConfigToBeAdded.forEach(function(propertyDetails) {
      var newProperty = App.config.createDefaultConfig(propertyDetails.name, 'yarn-client.xml', true);
      var value = rmHost1 + ':' + propertyDetails.port + ',' + rmHost2 + ':' + propertyDetails.port;
      Em.setProperties(newProperty, {
        serviceName: 'HAWQ',
        description: propertyDetails.description,
        displayName: propertyDetails.displayName,
        isOverridable: false,
        isReconfigurable: false,
        value: value,
        recommendedValue: value
      });

      configs.push(App.ServiceConfigProperty.create(newProperty));
    });
    return configs;
  },

  /**
   * Set the uid property for user properties. The uid is later used to help map the user and uid values in adjacent columns
   * @param {object} miscSvc
   * @param {string} svcName
   * @private
   */
  _setUID: function (miscSvc, svcName) {
    var user = miscSvc.configs.findProperty('name', svcName + '_user');
    if (user) {
      var uid = miscSvc.configs.findProperty('name', user.value + '_uid');
      if (uid) {
        user.set('ugid', uid);
      }
    }
  },

  /**
   * Set the gid property for group properties. The gid is later used to help map the group and gid values in adjacent columns
   * @param {object} miscSvc
   * @param {string} svcName
   * @private
   */
  _setGID: function (miscSvc, svcName) {
    var group = miscSvc.configs.findProperty('name', svcName + '_group');
    if (group) {
      var gid = miscSvc.configs.findProperty('name', group.value + '_gid');
      if (gid) {
        group.set('ugid', gid);
      }
    }
  },

  /**
   * render configs, distribute them by service
   * and wrap each in ServiceConfigProperty object
   * @param stepConfigs
   * @param configs
   * @return {App.ServiceConfig[]}
   */
  renderConfigs: function (stepConfigs, configs) {
    var localDB = {
      hosts: this.get('wizardController.content.hosts'),
      masterComponentHosts: this.get('wizardController.content.masterComponentHosts'),
      slaveComponentHosts: this.get('wizardController.content.slaveComponentHosts'),
      selectedStack: {}
    };
    var selectedRepoVersion,
        repoVersion;
    if (this.get('wizardController.name') === 'addServiceController') {
      repoVersion = App.RepositoryVersion.find().filter(function(i) {
        return i.get('stackVersionType') === App.get('currentStackName') &&
          i.get('stackVersionNumber') === App.get('currentStackVersionNumber');
      })[0];
      if (repoVersion) {
        selectedRepoVersion = Em.get(repoVersion, 'repositoryVersion').split('-')[0];
      }
    } else {
      selectedRepoVersion = Em.getWithDefault(App.Stack.find().findProperty('isSelected', true) || {}, 'repositoryVersion', false);
    }
    if (selectedRepoVersion) {
      localDB.selectedStack = selectedRepoVersion;
    }
    var configsByService = {}, dependencies = this.get('configDependencies');

    configs.forEach(function (_config) {
      if (!configsByService[_config.serviceName]) {
        configsByService[_config.serviceName] = [];
      }
      var serviceConfigProperty = App.ServiceConfigProperty.create(_config);
      this.updateHostOverrides(serviceConfigProperty, _config);
      if (this.get('wizardController.name') === 'addServiceController') {
        this._updateIsEditableFlagForConfig(serviceConfigProperty, true);
        //since the override_uid and ignore_groupusers_create changes are not saved to the database post install, they should be editable only
        //during initial cluster installation
        if (['override_uid', 'ignore_groupsusers_create'].contains(serviceConfigProperty.get('name'))) {
          serviceConfigProperty.set('isEditable', false);
        }
      }
      if (!this.get('content.serviceConfigProperties.length') && !serviceConfigProperty.get('hasInitialValue')) {
        App.ConfigInitializer.initialValue(serviceConfigProperty, localDB, dependencies);
      }
      configsByService[_config.serviceName].pushObject(serviceConfigProperty);
    }, this);

    stepConfigs.forEach(function (service) {
      if (service.get('serviceName') === 'YARN') {
        configsByService[service.get('serviceName')] = App.config.addYarnCapacityScheduler(configsByService[service.get('serviceName')]);
      }
      service.set('configs', configsByService[service.get('serviceName')] || []);
      if (['addServiceController', 'installerController'].contains(this.get('wizardController.name'))) {
        this.addHostNamesToConfigs(service, localDB.masterComponentHosts, localDB.slaveComponentHosts);
      }
    }, this);
    return stepConfigs;
  },

  addUidAndGidRepresentations: function(serviceConfigs) {
    //map the uids to the corresponding users
    var miscSvc = serviceConfigs.findProperty('serviceName', 'MISC');
    if (miscSvc) {
      //iterate through the list of users and groups and assign the uid/gid accordingly
      //user properties are servicename_user
      //uid properties are value of servicename_user + _uid
      //group properties are servicename_group
      //gid properties are value of servicename_group + _gid
      //we will map the users/uids and groups/gids based on this assumption
      this.get('selectedServiceNames').forEach(function (serviceName) {
        this._setUID(miscSvc, serviceName.toLowerCase());
        this._setGID(miscSvc, serviceName.toLowerCase());
      }, this);

      //for zookeeper, the user property name does not follow the convention that users for other services do. i.e. the user property name is not servicename_user as is the case with other services
      //the user property name is zk_user and not zookeeper_user, hence set the uid for zk_user separately
      this._setUID(miscSvc, 'zk');
      //the user property name is mapred_user and not mapreduce2_user for mapreduce2 service, hence set the uid for mapred_user separately
      this._setUID(miscSvc, 'mapred');
      //for haddop, the group property name does not follow the convention that groups for other services do. i.e. the group property name is not servicename_group as is the case with other services
      //the group property name is user_group and not zookeeper_group, hence set the gid for user_group separately
      this._setGID(miscSvc, 'user');

      // uid/gid properties are displayed in a separate column, hence prevent the properties from showing up on a separate line
      miscSvc.configs.filterProperty('displayType', 'uid_gid').setEach('isVisible', false);
    }
  },

  /**
   * Add host name properties to appropriate categories (for installer and add service)
   *
   * @param {Object} serviceConfig
   * @param {Object[]} masterComponents - info from localStorage
   * @param {Object[]} slaveComponents - info from localStorage
   */
  addHostNamesToConfigs: function(serviceConfig, masterComponents, slaveComponents) {
    serviceConfig.get('configCategories').forEach(function(c) {
      if (c.showHost) {
        var componentName = c.name;
        var value = this.getComponentHostValue(componentName, masterComponents, slaveComponents);
        var stackComponent = App.StackServiceComponent.find(componentName);
        var hProperty = App.config.createHostNameProperty(serviceConfig.get('serviceName'), componentName, value, stackComponent);
        var newConfigName = Em.get(hProperty, 'name');
        if (!serviceConfig.get('configs').someProperty('name', newConfigName)) {
          serviceConfig.get('configs').push(App.ServiceConfigProperty.create(hProperty));
        }
      }
    }, this);
  },

  /**
   * Method to get host for master or slave component
   *
   * @param componentName
   * @param masterComponents
   * @param slaveComponents
   * @returns {Array}
   */
  getComponentHostValue: function(componentName, masterComponents, slaveComponents) {
    var value = [];
    var masters = masterComponents && masterComponents.filterProperty('component', componentName);
    if (masters.length) {
      value = masters.mapProperty('hostName');
    } else {
      var slaves = slaveComponents && slaveComponents.findProperty('componentName', componentName);
      if (slaves) {
        value = slaves.hosts.mapProperty('hostName');
      }
    }
    return value || [];
  },

  /**
   * create new child configs from overrides, attach them to parent config
   * override - value of config, related to particular host(s)
   * @param configProperty
   * @param storedConfigProperty
   */
  updateHostOverrides: function (configProperty, storedConfigProperty) {
    if (!Em.isEmpty(storedConfigProperty.overrides)) {
      var overrides = [];
      storedConfigProperty.overrides.forEach(function (overrideEntry) {
        // create new override with new value
        var newSCP = App.ServiceConfigProperty.create(configProperty);
        newSCP.set('value', overrideEntry.value);
        newSCP.set('isOriginalSCP', false); // indicated this is overridden value,
        newSCP.set('parentSCP', configProperty);
        overrides.pushObject(newSCP);
      });
      configProperty.set('overrides', overrides);
    }
  },

  /**
   * When NameNode HA is enabled some configs based on <code>dfs.nameservices</code> should be changed
   * This happens only if service is added AFTER NN HA is enabled
   *
   * @param {App.ServiceConfig[]} serviceConfigs
   * @method _reconfigureServiceOnNnHa
   * @private
   * @returns {App.ServiceConfig[]}
   */
  _reconfigureServicesOnNnHa: function (serviceConfigs) {
    var selectedServiceNames = this.get('selectedServiceNames');
    var nameServiceId = serviceConfigs.findProperty('serviceName', 'HDFS').configs.findProperty('name', 'dfs.nameservices');
    if (nameServiceId) {
      Em.A([
        {
          serviceName: 'HBASE',
          configToUpdate: 'hbase.rootdir'
        },
        {
          serviceName: 'ACCUMULO',
          configToUpdate: 'instance.volumes'
        },
        {
          serviceName: 'HAWQ',
          configToUpdate: 'hawq_dfs_url',
          regexPattern: /(^.*:[0-9]+)(?=\/)/,
          replacementValue: nameServiceId.get('value')
        }
      ]).forEach(function (c) {
        if (selectedServiceNames.contains(c.serviceName) && nameServiceId) {
          var cfg = serviceConfigs.findProperty('serviceName', c.serviceName).configs.findProperty('name', c.configToUpdate);
          var regexPattern = /\/\/.*:[0-9]+/i;
          var replacementValue = '//' + nameServiceId.get('value');
          if (!Em.isNone(c.regexPattern) && !Em.isNone(c.replacementValue)) {
            regexPattern = c.regexPattern;
            replacementValue = c.replacementValue;
          }
          var newValue = cfg.get('value').replace(regexPattern, replacementValue);
          cfg.setProperties({
            value: newValue,
            recommendedValue: newValue
          });
        }
      });
    }
    return serviceConfigs;
  },

  /**
   * Select previously selected service if not within the tab for the first time
   * Select first addable service for <code>addServiceWizard</code>
   * Select first service at all in other cases
   * @method selectProperService
   */
  selectProperService: function () {
    var activeTab = this.get('tabs').findProperty('isActive', true);
    var tabSelectedServiceName = activeTab ? activeTab.get('selectedServiceName') : null;
    var lastSelectedService = tabSelectedServiceName ? this.get('stepConfigs').findProperty('serviceName', tabSelectedServiceName) : null
    if(tabSelectedServiceName && lastSelectedService) {
      this.set('selectedService', lastSelectedService);
    } else if (this.get('wizardController.name') === 'addServiceController') {
      this.set('selectedService', this.get('stepConfigs').filterProperty('selected', true).get('firstObject'));
    } else {
      this.set('selectedService', this.get('stepConfigs').filterProperty('showConfig', true).objectAt(0));
    }
  },

  /**
   * Load config tags
   * @return {$.ajax|null}
   * @method getConfigTags
   */
  getConfigTags: function (resetFlag) {
    if (resetFlag) this.set('isAppliedConfigLoaded', false);
    return App.ajax.send({
      name: 'config.tags',
      sender: this,
      success: 'getConfigTagsSuccess'
    });
  },

  getServicesConfigurations: function() {
    var dfd = $.Deferred();
    var configs, servicesConfigurations;
    configs = this.get('wizardController').getConfigsAndFilenames(this, true).serviceConfigProperties;
    servicesConfigurations = configs.reduce(function(configTags, property) {
      var fileName = App.config.getConfigTagFromFileName(property.filename),
          configType;
      if (!configTags.someProperty('type', fileName)) {
        configTags.push({
          type: fileName,
          properties: {}
        });
      }
      configType = configTags.findProperty('type', fileName);
      configType.properties[property.name] = property.value;
      return configTags;
    }, []);
    dfd.resolve(servicesConfigurations);
    return dfd.promise();
  },

  /**
   * Success callback for config tags request
   * Updates <code>serviceConfigTags</code> with tags received from server
   * @param {object} data
   * @method getConfigTagsSuccess
   */
  getConfigTagsSuccess: function (data) {
    var installedServiceSites = [];
    App.StackService.find().filterProperty('isInstalled').forEach(function (service) {
      if (!service.get('configTypes')) return;
      var configTypes = Object.keys(service.get('configTypes'));
      installedServiceSites = installedServiceSites.concat(configTypes);
    }, this);
    installedServiceSites = installedServiceSites.uniq();
    var serviceConfigTags = [];
    for (var site in data.Clusters.desired_configs) {
      if (data.Clusters.desired_configs.hasOwnProperty(site)) {
        if (installedServiceSites.contains(site) || site === 'cluster-env') {
          serviceConfigTags.push({
            siteName: site,
            tagName: data.Clusters.desired_configs[site].tag,
            newTagName: null
          });
        }
      }
    }
    this.set('serviceConfigTags', serviceConfigTags);
    this.set('isAppliedConfigLoaded', true);
  },

  /**
   * set configs actual values from server
   * @param configs
   * @param configsByTags
   * @param installedServiceNames
   * @method setInstalledServiceConfigs
   */
  setInstalledServiceConfigs: function (configs, configsByTags, installedServiceNames) {
    var configsMap = {};
    var finalAttrMap = {};
    var passwordAttrMap = {};

    configsByTags.forEach(function (configSite) {
      configsMap[configSite.type] = configSite.properties || {};
      finalAttrMap[configSite.type] = configSite.properties_attributes && configSite.properties_attributes.final || {};
      passwordAttrMap[configSite.type] = configSite.properties_attributes && configSite.properties_attributes.password || {};
    });
    var allConfigs = configs.filter(function (_config) {
      // filter out alert_notification configs on add service //TODO find better place for this!
      if (_config.filename === 'alert_notification') return false;
      if (['MISC'].concat(installedServiceNames).contains(_config.serviceName)) {
        var type = _config.filename ? App.config.getConfigTagFromFileName(_config.filename) : null;
        var mappedConfigValue = type && configsMap[type] ? configsMap[type][_config.name] : null;
        if (Em.isNone(mappedConfigValue)) {
          //for now ranger plugin properties are not sending by recommendations if they are missed - it should be added
          return _config.serviceName === 'MISC' || /^ranger-/.test(_config.filename);
        }
        if (_config.savedValue != mappedConfigValue) {
          _config.savedValue = App.config.formatPropertyValue(_config, mappedConfigValue);
        }
        _config.value = App.config.formatPropertyValue(_config, mappedConfigValue);
        _config.hasInitialValue = true;
        this.updateDependencies(_config);
        delete configsMap[type][_config.name];
        return true;
      }
      return true;
    }, this);
    //add user properties
    Em.keys(configsMap).forEach(function (filename) {
      Em.keys(configsMap[filename]).forEach(function (propertyName) {
        allConfigs.push(App.config.createDefaultConfig(propertyName, App.config.getOriginalFileName(filename), false, {
          value: configsMap[filename][propertyName],
          savedValue: configsMap[filename][propertyName],
          propertyType: passwordAttrMap[filename][propertyName] === 'true' ? ['PASSWORD'] : null,
          isFinal: finalAttrMap[filename][propertyName] === 'true',
          hasInitialValue: true
        }));
      });
    });
    return allConfigs;
  },

  /**
   * update dependencies according to current config value
   *
   * @param config
   */
  updateDependencies: function(config) {
    if (config.filename === 'hive-site.xml') {
      if (config.name === 'hive.metastore.uris') {
        this.get('configDependencies')['hive.metastore.uris'] = config.savedValue;
      }
      else
        if (config.name === 'clientPort') {
          this.get('configDependencies').clientPort = config.savedValue;
        }
    }
    if (config.filename === 'application-properties.xml') {
      if (this.get('configDependencies').hasOwnProperty(config.name)) {
        this.get('configDependencies')[config.name] = config.savedValue;
      }
    }
  },

  /**
   * Add group ids to <code>groupsToDelete</code>
   * Also save <code>groupsToDelete</code> to local storage
   * @param {Ember.Object[]} groups
   * @method setGroupsToDelete
   */
  setGroupsToDelete: function (groups) {
    var groupsToDelete = this.get('groupsToDelete');
    groups.forEach(function (group) {
      if (!group.get('isTemporary')) {
        groupsToDelete.push({
          id: group.get('id')
        });
      }
    });
    this.get('wizardController').setDBProperty('groupsToDelete', groupsToDelete);
  },

  /**
   * Update <code>configGroups</code> with selected service configGroups
   * Also set default group to first position
   * Update <code>selectedConfigGroup</code> with new default group
   * @method selectedServiceObserver
   */
  selectedServiceObserver: function () {
    if (this.get('selectedService') && this.get('selectedService.serviceName') !== 'MISC') {
      var serviceGroups = this.get('selectedService.configGroups');
      serviceGroups.forEach(function (item, index, array) {
        if (item.isDefault) {
          array.unshift(item);
          array.splice(index + 1, 1);
        }
      });
      this.set('configGroups', serviceGroups);
      this.set('selectedConfigGroup', serviceGroups.findProperty('isDefault'));
    }
  }.observes('selectedService.configGroups.[]'),

  /**
   * load default groups for each service in case of initial load
   * @param serviceConfigGroups
   * @method restoreConfigGroups
   */
  restoreConfigGroups: function (serviceConfigGroups) {
    var services = this.get('stepConfigs');

    services.forEach(function (service) {
      if (service.get('serviceName') === 'MISC') return;
      var serviceRawGroups = serviceConfigGroups.filterProperty('service_name', service.serviceName);
      if (serviceRawGroups.length) {
        App.store.safeLoadMany(App.ServiceConfigGroup, serviceRawGroups);
        serviceRawGroups.forEach(function(item){
          var modelGroup = App.ServiceConfigGroup.find(item.id);
          modelGroup.set('properties', []);
          item.properties.forEach(function (propertyData) {
            var overriddenSCP, parentSCP = service.configs.filterProperty('filename', propertyData.filename).filterProperty('isOriginalSCP').findProperty('name', propertyData.name);
            if (parentSCP) {
              App.config.createOverride(parentSCP, propertyData, modelGroup)
            } else {
              overriddenSCP = App.config.createCustomGroupConfig({
                propertyName: propertyData.name,
                filename: propertyData.filename,
                value: propertyData.value,
                savedValue: propertyData.value,
                isEditable: false
              }, modelGroup);
              this.get('stepConfigs').findProperty('serviceName', service.serviceName).get('configs').pushObject(overriddenSCP);
            }
          }, this);
          modelGroup.set('hash', this.get('wizardController').getConfigGroupHash(modelGroup));
        }, this);
        service.set('configGroups', App.ServiceConfigGroup.find().filterProperty('serviceName', service.get('serviceName')));
      }
    }, this);
  },

  /**
   * Click-handler on config-group to make it selected
   * @param {object} event
   * @method selectConfigGroup
   */
  selectConfigGroup: function (event) {
    this.set('selectedConfigGroup', event.context);
  },

  /**
   * Rebuild list of configs switch of config group:
   * on default - display all configs from default group and configs from non-default groups as disabled
   * on non-default - display all from default group as disabled and configs from selected non-default group
   * @method switchConfigGroupConfigs
   */
  switchConfigGroupConfigs: function () {
    var serviceConfigs = this.get('selectedService.configs'),
      selectedGroup = this.get('selectedConfigGroup'),
      overrides = [];
    if (!selectedGroup) return;

    var displayedConfigGroups = this._getDisplayedConfigGroups();
    displayedConfigGroups.forEach(function (group) {
      overrides.pushObjects(group.get('properties'));
    });
    serviceConfigs.forEach(function (config) {
      this._setEditableValue(config);
      this._setOverrides(config, overrides);
    }, this);
  }.observes('selectedConfigGroup'),

  /**
   * Get list of config groups to display
   * Returns empty array if no <code>selectedConfigGroup</code>
   * @return {Array}
   * @method _getDisplayedConfigGroups
   */
  _getDisplayedConfigGroups: function () {
    var selectedGroup = this.get('selectedConfigGroup');
    if (!selectedGroup) return [];
    return selectedGroup.get('isDefault') ?
      this.get('selectedService.configGroups').filterProperty('isDefault', false) :
      [this.get('selectedConfigGroup')];
  },

  /**
   * Set <code>isEditable</code> property to <code>config</code>
   * @param {Ember.Object} config
   * @return {Ember.Object} updated config-object
   * @method _setEditableValue
   */
  _setEditableValue: function (config) {
    var selectedGroup = this.get('selectedConfigGroup');
    if (!selectedGroup) return config;
    if (App.get('isKerberosEnabled') &&
        config.get('isConfigIdentity') &&
        !App.StackService.find().filterProperty('isSelected').mapProperty('serviceName').contains(Em.get(config, 'serviceName'))) {
      config.set('isEditable', false);
    } else {
      var isEditable = config.get('isEditable'),
        isServiceInstalled = this.get('installedServiceNames').contains(this.get('selectedService.serviceName'));
      if (isServiceInstalled) {
        isEditable = config.get('isReconfigurable') && selectedGroup.get('isDefault') && !config.get('disabledAsComponentAction');
      } else {
        isEditable = selectedGroup.get('isDefault') && !config.get('disabledAsComponentAction');
      }
      if (config.get('group')) {
        isEditable = config.get('group.name') === this.get('selectedConfigGroup.name');
      }
      config.set('isEditable', isEditable);
    }
    return config;
  },

  /**
   * Set <code>overrides</code> property to <code>config</code>
   * @param {Ember.Object} config
   * @param {Ember.Enumerable} overrides
   * @returns {Ember.Object}
   * @method _setOverrides
   */
  _setOverrides: function (config, overrides) {
    if (config.get('group')) return config;
    var selectedGroup = this.get('selectedConfigGroup'),
      overrideToAdd = this.get('overrideToAdd'),
      configOverrides = overrides.filterProperty('id', config.get('id'));
    if (!selectedGroup) return config;
    if (overrideToAdd && overrideToAdd.get('id') === config.get('id')) {
      var valueForOverride = (config.get('widget') || config.get('displayType') === 'checkbox') ? config.get('value') : '';
      var group = this.get('selectedService.configGroups').findProperty('name', selectedGroup.get('name'));
      var newSCP = App.config.createOverride(config, {value: valueForOverride, recommendedValue: valueForOverride}, group);
      configOverrides.push(newSCP);
      this.set('overrideToAdd', null);
    }
    configOverrides.setEach('isEditable', !selectedGroup.get('isDefault'));
    configOverrides.setEach('parentSCP', config);
    config.set('overrides', configOverrides);
    return config;
  },

  /**
   * @param serviceName
   * @returns {boolean}
   * @override
   */
  useInitialValue: function(serviceName) {
    return !App.Service.find(serviceName).get('serviceName', serviceName);
  },

  /**
   *
   * @param parentProperties
   * @param name
   * @param fileName
   * @param configGroup
   * @param savedValue
   * @returns {*}
   * @override
   */
  allowUpdateProperty: function(parentProperties, name, fileName, configGroup, savedValue) {
    if (name.contains('proxyuser')) return true;
    if (['installerController'].contains(this.get('wizardController.name')) || !!(parentProperties && parentProperties.length)) {
      return true;
    } else if (['addServiceController'].contains(this.get('wizardController.name'))) {
      var stackProperty = App.configsCollection.getConfigByName(name, fileName);
      if (!stackProperty || !this.get('installedServices')[stackProperty.serviceName]) {
        return true;
      } else if (stackProperty.propertyDependsOn.length) {
        return stackProperty.propertyDependsOn.some(function (p) {
          var service = App.config.get('serviceByConfigTypeMap')[p.type];
          return service && !this.get('installedServices')[service.get('serviceName')];
        }, this);
      } else {
        return !Em.isNone(savedValue) && stackProperty.recommendedValue === savedValue;
      }
    }
    return true;
  },

  /**
   * remove config based on recommendations
   * @param config
   * @param configsCollection
   * @param parentProperties
   * @protected
   * @override
   */
  _removeConfigByRecommendation: function (config, configsCollection, parentProperties) {
    this._super(config, configsCollection, parentProperties);
    /**
     * need to update wizard info when removing configs for installed services;
     */
    var installedServices = this.get('installedServices'), wizardController = this.get('wizardController'),
      fileNamesToUpdate = wizardController ? wizardController.getDBProperty('fileNamesToUpdate') || [] : [],
      fileName = Em.get(config, 'filename'), serviceName = Em.get(config, 'serviceName');
    var modifiedFileNames = this.get('modifiedFileNames');
    if (modifiedFileNames && !modifiedFileNames.contains(fileName)) {
      modifiedFileNames.push(fileName);
    } else if (wizardController && installedServices[serviceName]) {
      if (!fileNamesToUpdate.contains(fileName)) {
        fileNamesToUpdate.push(fileName);
      }
    }
    if (wizardController) {
      wizardController.setDBProperty('fileNamesToUpdate', fileNamesToUpdate.uniq());
    }
  },
  /**
   * @method manageConfigurationGroup
   */
  manageConfigurationGroup: function () {
    App.router.get('manageConfigGroupsController').manageConfigurationGroups(this);
  },

  /**
   * Check whether hive New MySQL database is on the same host as Ambari server MySQL server
   * @return {$.ajax|null}
   * @method checkMySQLHost
   */
  checkMySQLHost: function () {
    // get ambari database type and hostname
    return App.ajax.send({
      name: 'ambari.service',
      data: {
        fields : "?fields=hostComponents/RootServiceHostComponents/properties/server.jdbc.database_name,hostComponents/RootServiceHostComponents/properties/server.jdbc.url,hostComponents/RootServiceHostComponents/properties/server.jdbc.database"
      },
      sender: this,
      success: 'getAmbariDatabaseSuccess'
    });
  },

  /**
   * Success callback for ambari database, get Ambari DB type and DB server hostname, then
   * Check whether hive New MySQL database is on the same host as Ambari server MySQL server
   * @param {object} data
   * @method getAmbariDatabaseSuccess
   */
  getAmbariDatabaseSuccess: function (data) {
    var ambariServerDBType = Em.getWithDefault(data.hostComponents, '0.RootServiceHostComponents.properties', {})['server.jdbc.database'],
        ambariServerHostName = Em.getWithDefault(data.hostComponents, '0.RootServiceHostComponents.host_name', false),
        hiveConnectionURL = Em.getWithDefault(App.config.findConfigProperty(this.get('stepConfigs'), 'javax.jdo.option.ConnectionURL', 'hive-site.xml') || {}, 'value', '');
    if (ambariServerHostName) {
      this.set('mySQLServerConflict', ambariServerDBType.contains('mysql') && hiveConnectionURL.contains(ambariServerHostName));
    } else {
      this.set('mySQLServerConflict', false);
    }
  },

  /**
   * Check if new MySql database was chosen for Hive service
   * and it is not located on the same host as Ambari server
   * that using MySql database too.
   *
   * @method resolveHiveMysqlDatabase
   */
  resolveHiveMysqlDatabase: function () {
    var hiveService = this.get('content.services').findProperty('serviceName', 'HIVE');
    if (!hiveService || !hiveService.get('isSelected') || hiveService.get('isInstalled')) {
      return this.moveNext();
    }
    var hiveDBType = this.get('stepConfigs').findProperty('serviceName', 'HIVE').configs.findProperty('name', 'hive_database').value;
    if (hiveDBType === 'New MySQL Database') {
      var self = this;
      return this.checkMySQLHost().done(function () {
        self.mySQLWarningHandler();
      });
    }
    return this.moveNext();
  },

  /**
   * Show warning popup about MySQL-DB issues (on post-submit)
   *
   * @returns {*}
   * @method mySQLWarningHandler
   */
  mySQLWarningHandler: function () {
    var self = this;
    if (this.get('mySQLServerConflict')) {
      // error popup before you can proceed
      return App.ModalPopup.show({
        header: Em.I18n.t('installer.step7.popup.mySQLWarning.header'),
        body:Em.I18n.t('installer.step7.popup.mySQLWarning.body'),
        secondary: Em.I18n.t('installer.step7.popup.mySQLWarning.button.gotostep5'),
        primary: Em.I18n.t('installer.step7.popup.mySQLWarning.button.dismiss'),
        encodeBody: false,
        onPrimary: function () {
          this._super();
          self.set('submitButtonClicked', false);
        },
        onSecondary: function () {
          var parent = this;
          return App.ModalPopup.show({
            header: Em.I18n.t('installer.step7.popup.mySQLWarning.confirmation.header'),
            body: Em.I18n.t('installer.step7.popup.mySQLWarning.confirmation.body'),
            onPrimary: function () {
              this.hide();
              parent.hide();
              // go back to step 5: assign masters and disable default navigation warning
              if (self.get('isInstallWizard')) {
                App.router.get('installerController').gotoStep(5, true);
              }
              else {
                if (self.get('isAddServiceWizard')) {
                  App.router.get('addServiceController').gotoStep(2, true);
                }
              }
            },
            onSecondary: function () {
              this._super();
              self.set('submitButtonClicked', false);
            }
          });
        }
      });
    }
    return this.moveNext();
  },

  checkDatabaseConnectionTest: function () {
    var deferred = $.Deferred();
    var configMap = [
      {
        serviceName: 'OOZIE',
        ignored: [Em.I18n.t('installer.step7.oozie.database.new')]
      },
      {
        serviceName: 'HIVE',
        ignored: [Em.I18n.t('installer.step7.hive.database.new.mysql'), Em.I18n.t('installer.step7.hive.database.new.postgres')]
      }
    ];
    configMap.forEach(function (config) {
      var isConnectionNotTested = false;
      var service = this.get('content.services').findProperty('serviceName', config.serviceName);
      if (service && service.get('isSelected') && !service.get('isInstalled')) {
        var serviceConfigs = this.get('stepConfigs').findProperty('serviceName', config.serviceName).configs;
        var serviceDatabase = serviceConfigs.findProperty('name', config.serviceName.toLowerCase() + '_database').get('value');
        if (!config.ignored.contains(serviceDatabase)) {
          var filledProperties = App.db.get('tmp', config.serviceName + '_connection');
          if (!filledProperties || App.isEmptyObject(filledProperties)) {
            isConnectionNotTested = true;
          } else {
            for (var key in filledProperties) {
              if (serviceConfigs.findProperty('name', key).get('value') !== filledProperties[key])
                isConnectionNotTested = true;
            }
          }
        }
      }
      config.isCheckIgnored = isConnectionNotTested;
    }, this);
    var ignoredServices = configMap.filterProperty('isCheckIgnored', true);
    if (ignoredServices.length) {
      var displayedServiceNames = ignoredServices.mapProperty('serviceName').map(function (serviceName) {
        return this.get('content.services').findProperty('serviceName', serviceName).get('displayName');
      }, this);
      this.showDatabaseConnectionWarningPopup(displayedServiceNames, deferred);
    }
    else {
      deferred.resolve();
    }
    return deferred;
  },

  showChangesWarningPopup: function(goToNextStep) {
    return App.ModalPopup.show({
      header: Em.I18n.t('common.warning'),
      body: Em.I18n.t('services.service.config.exitChangesPopup.body'),
      secondary: Em.I18n.t('common.cancel'),
      primary: Em.I18n.t('yes'),
      onPrimary: function () {
        if (goToNextStep) {
          goToNextStep();
          this.hide();
        }
      },
      onSecondary: function () {
        this.hide();
        App.set('router.btnClickInProgress', false);
        App.set('router.backBtnClickInProgress', false);
      }
    });
  },

  showDatabaseConnectionWarningPopup: function (serviceNames, deferred) {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step7.popup.database.connection.header'),
      body: Em.I18n.t('installer.step7.popup.database.connection.body').format(serviceNames.join(', ')),
      secondary: Em.I18n.t('common.cancel'),
      primary: Em.I18n.t('common.proceedAnyway'),
      onPrimary: function () {
        deferred.resolve();
        this._super();
      },
      onClose: function () {
        this.onSecondary();
      },
      onSecondary: function () {
        this._super();
        self.set('submitButtonClicked', false);
        App.set('router.nextBtnClickInProgress', false);
        deferred.reject();
      }
    });
  },

  showOozieDerbyWarningPopup: function(callback) {
    var self = this;
    if (this.get('selectedServiceNames').contains('OOZIE')) {
      var databaseType = Em.getWithDefault(App.config.findConfigProperty(this.get('stepConfigs'), 'oozie_database', 'oozie-env.xml') || {}, 'value', '');
      if (databaseType === Em.I18n.t('installer.step7.oozie.database.new')) {
        return App.ModalPopup.show({
          header: Em.I18n.t('common.warning'),
          body: Em.I18n.t('installer.step7.popup.oozie.derby.warning'),
          secondary: Em.I18n.t('common.cancel'),
          primary: Em.I18n.t('common.proceedAnyway'),
          onPrimary: function() {
            this.hide();
            if (callback) {
              callback();
            }
          },
          onSecondary: function() {
            App.set('router.nextBtnClickInProgress', false);
            self.set('submitButtonClicked', false);
            this.hide();
          },
          onClose: function() {
            this.onSecondary();
          }
        });
      }
    }
    if (callback) {
      callback();
    }
    return false;
  },

  /**
   * Proceed to the next step
   **/
  moveNext: function () {
    App.set('router.nextBtnClickInProgress', false);
    App.router.send('next');
    this.set('submitButtonClicked', false);
  },

  /**
   * Click-handler on Next button
   * Disable "Submit"-button while server-side processes are running
   * @method submit
   */
  submit: function () {
    if (this.get('isSubmitDisabled') || App.get('router.nextBtnClickInProgress')) {
      return false;
    }
    App.set('router.nextBtnClickInProgress', true);
    if (this.get('supportsPreInstallChecks')) {
      var preInstallChecksController = App.router.get('preInstallChecksController');
      if (preInstallChecksController.get('preInstallChecksWhereRun')) {
        return this.postSubmit();
      }
      return preInstallChecksController.notRunChecksWarnPopup(this.postSubmit.bind(this));
    }
    return this.postSubmit();
  },

  postSubmit: function () {
    this.set('submitButtonClicked', true);
    if (this.get('isInstallWizard')) {
      this.serverSideValidationCallback();
    } else {
      this.serverSideValidation().done(() => {
        this.serverSideValidationCallback();
      }).fail(value => {
        if ("invalid_configs" === value) {
          if (this.get('isAddServiceWizard')) {
            this.get('configErrorList.issues').clear();
            this.get('configErrorList.criticalIssues').clear();
          }
          this.set('submitButtonClicked', false);
          App.set('router.nextBtnClickInProgress', false);
        } else {
          // Failed due to validation mechanism failure.
          // Should proceed with other checks
          this.serverSideValidationCallback();
        }
      });
    }
  },

  /**
   * @method serverSideValidationCallback
   */
  serverSideValidationCallback: function() {
    var self = this;
    this.showOozieDerbyWarningPopup(function() {
      self.checkDatabaseConnectionTest().done(function () {
        self.resolveHiveMysqlDatabase();
      });
    });
  },

  toggleIssuesFilter: function () {
    var errorServices = [],
        issueServices = [];

    this.get('filterColumns').findProperty('attributeName', 'hasIssues').toggleProperty('selected');

    // if currently selected service does not have issue, jump to the first service with issue.
    if (this.get('selectedService.errorCount') === 0 )
    {
      this.get('stepConfigs').filterProperty('showConfig', true).forEach(function(service) {
        if (service.get('errorCount') > 0) errorServices.push(service);
        if (service.get('hasConfigIssues') === true) issueServices.push(service);
      });
      if (errorServices.length === 0 && issueServices.length === 0) return;
      this.switchToService(errorServices.length > 0 ? errorServices[0] : issueServices[0]);
    }
  },

  switchToService: function(service) {
    this.set('selectedService', service);
    $('a[href="#' + service.serviceName + '"]').tab('show');
  },


  issuesFilterSelected: function() {
    return this.get('filterColumns').findProperty('attributeName', 'hasIssues').get('selected');
  }.property('filterColumns.@each.selected'),

  hasStepConfigIssues: function() {
    return !this.get('stepConfigs').filterProperty('showConfig', true).everyProperty('hasConfigIssues', false);
  }.property('stepConfigs.@each.hasConfigIssues'),

  checkDescriptor: function() {
    if (App.get('isKerberosEnabled')) {
      return App.ajax.send({
        sender: self,
        name: 'admin.kerberize.cluster_descriptor_artifact'
      });
    }
    return $.Deferred().resolve().promise();
  },

  /**
   * Store status of kerberos descriptor located in cluster artifacts.
   * This status needed for Add Service Wizard to select appropriate method to create
   * or update descriptor.
   *
   * @param  {Boolean} isExists <code>true</code> if cluster descriptor present
   */
  storeClusterDescriptorStatus: function(isExists) {
    this.get('wizardController').setDBProperty('isClusterDescriptorExists', isExists);
  },

   /**
   * Get all configs, that should be stored in kerberos_descriptor
   * @returns {Array}
   */
  getDescriptorConfigs: function () {
     return this.get('stepConfigs').reduce(function (allConfigs, service) {
      return allConfigs.concat(service.get('configs').filterProperty('isSecureConfig'));
    }, []);
  },

  selectService: function (event) {
    this.get('stepConfigs').forEach((service) => {
      service.set('isActive', service.get('serviceName') === event.context.serviceName);
    });
    this.set('selectedService', event.context);
    var activeTabs = this.get('tabs').findProperty('isActive', true);
    if (activeTabs) {
      activeTabs.set('selectedServiceName', event.context.serviceName);
    }
  },

  /**
   * Set initial state for <code>tabs</code>
   */
  initTabs: function () {
    var storedConfigs = this.get('content.serviceConfigProperties');
    var tabs = [
      Em.Object.create({
        name: 'credentials',
        displayName: 'Credentials',
        icon: 'glyphicon-lock',
        isActive: false,
        isDisabled: false,
        isSkipped: false,
        validateOnSwitch: false,
        tabView: App.CredentialsTabOnStep7View
      }),
      Em.Object.create({
        name: 'databases',
        displayName: 'Databases',
        icon: 'glyphicon-align-justify',
        isActive: false,
        isDisabled: false,
        isSkipped: false,
        validateOnSwitch: false,
        tabView: App.DatabasesTabOnStep7View
      }),
      Em.Object.create({
        name: 'directories',
        displayName: 'Directories',
        icon: 'glyphicon-folder-open',
        isActive: false,
        isDisabled: false,
        isSkipped: false,
        validateOnSwitch: false,
        selectedServiceName: null,
        tabView: App.DirectoriesTabOnStep7View
      }),
      Em.Object.create({
        name: 'accounts',
        displayName: 'Accounts',
        icon: 'glyphicon-user',
        isActive: false,
        isDisabled: false,
        isSkipped: false,
        validateOnSwitch: false,
        tabView: App.AccountsTabOnStep7View
      }),
      Em.Object.create({
        name: 'all-configurations',
        displayName: 'All Configurations',
        icon: 'glyphicon-wrench',
        isActive: false,
        isDisabled: false,
        isSkipped: false,
        validateOnSwitch: true,
        selectedServiceName: null,
        tabView: App.ServicesConfigView
      })
    ];

    this.set('tabs', tabs);

    this.setSkippedTabs();

    if (storedConfigs && storedConfigs.length) {
      tabs.findProperty('name', 'all-configurations').set('isActive', true);
    } else {
      tabs.findProperty('isDisabled', false).set('isActive', true);
      this.disableTabs();
    }

  },

  setSkippedTabs: function () {
    var servicesWithCredentials = App.Tab.find().filterProperty('themeName', 'credentials').mapProperty('serviceName');
    var servicesWithDatabase = App.Tab.find().filterProperty('themeName', 'database').mapProperty('serviceName');
    var selectedServices = this.get('content.selectedServiceNames');
    var tabs = this.get('tabs');
    var disableCredentials = true;
    var disableDatabases = true;

    for (var i = 0; i < selectedServices.length; i++) {
      var serviceName = selectedServices[i];
      disableCredentials = disableCredentials && servicesWithCredentials.indexOf(serviceName) === -1;
      disableDatabases = disableDatabases && servicesWithDatabase.indexOf(serviceName) === -1;
      if (!disableCredentials && !disableDatabases) break;
    }

    tabs.findProperty('name', 'credentials').setProperties({
      isDisabled: disableCredentials,
      isSkipped: disableCredentials
    });
    tabs.findProperty('name', 'databases').setProperties({
      isDisabled: disableDatabases,
      isSkipped: disableDatabases
    });
  },

  /**
   * Get index of current (active) tab
   */
  currentTabIndex: function () {
    return this.get('tabs').findIndex(function (tab) {
      return tab.get('isActive');
    });
  }.property('tabs.@each.isActive'),

  /**
   * Get name of current (active) tab
   */
  currentTabName: function () {
    var activeTab = this.get('tabs').findProperty('isActive');
    return activeTab ? activeTab.get('name') : '';
  }.property('tabs.@each.isActive'),

  /**
   * Make selected tab active, show tab's content
   * @param event
   * @returns {boolean}
   */
  selectTab: function (event) {
    var tab = event.context;
    if (!tab.get('isDisabled')) {
      $('a[href=#' + tab.name + ']').tab('show');
      this.get('tabs').setEach('isActive', false);
      tab.set('isActive', true);
    }
    return false;
  },

  /**
   * Get disabled state for next button
   */
  isNextDisabled: function () {
    var tabName = this.get('currentTabName');
    switch (tabName) {
      case 'credentials':
        return !this.get('credentialsTabNextEnabled');
      case 'databases':
        return !this.get('databasesTabNextEnabled');
      case 'all-configurations':
        return this.get('isSubmitDisabled');
      default:
        return false;
    }
  }.property('tabs.@each.isActive', 'isSubmitDisabled', 'credentialsTabNextEnabled', 'databasesTabNextEnabled'),

  /**
   * Set isDisabled state for tabs
   */
  disableTabs: function () {
    this.get('tabs').rejectProperty('isSkipped').setEach('isDisabled', !this.get('tabs').findProperty('name', 'credentials').get('isDisabled') && !this.get('credentialsTabNextEnabled'));
  }.observes('credentialsTabNextEnabled'),

  /**
   * Select specified service and put property name in filter input
   * @param event - jQuery event
   */
  showConfigProperty: function (event) {
    var serviceName = event.context.serviceName;
    var propertyName = event.context.propertyName || event.context.name;
    var stepConfig = this.get('stepConfigs').findProperty('serviceName', serviceName)
      || this.get('stepConfigs').findProperty('displayName', serviceName);
    this.set('selectedService', stepConfig);
    this.get('filterColumns').setEach('selected', false);
    Em.run.next(this, function () {
      this.set('filter', propertyName);
    });

    this.get('stepConfigs').setEach('isActive', false);
    stepConfig.set('isActive', true);
  },

  /**
   * Show bell animation
   */
  ringBell: function () {
    $('#issues-bell').addClass('animated');
    $('#issues-counter').addClass('animated');
    setTimeout(function () {
      $('#issues-bell').removeClass('animated');
    }, 2000);
    setTimeout(function () {
      $('#issues-counter').removeClass('animated');
    }, 300);
  },

  /**
   * Set <code>issuesCounter</code> and run bell animation if needed
   */
  setIssues: function () {
    var recommendations = this.get('changedProperties.length');
    var validations = this.get('stepConfigs').mapProperty('configsWithErrors.length').reduce(Em.sum, 0);
    var configErrorList = this.get('configErrorList');
    this.set('suggestionsCounter', recommendations + configErrorList.get('issues.length') + configErrorList.get('criticalIssues.length'));
    if (validations !== this.get('validationsCounter')) {
      this.ringBell();
    }
    this.set('hasErrors', Boolean(validations + configErrorList.get('criticalIssues.length')));
    this.set('validationsCounter', validations);
  }.observes('changedProperties.length', 'stepConfigs.@each.configsWithErrors.length', 'configErrorList.issues.length', 'configErrorList.criticalIssues.length'),

  /**
   * Next button action handler
   */
  next: function (index) {
    var tabs = this.get('tabs');
    var currentTabIndex = typeof(index) === 'number' ? index : this.get('currentTabIndex');
    if (tabs.length - 1 > currentTabIndex) {
      tabs[currentTabIndex].set('isActive', false);
      if (tabs[currentTabIndex + 1].get('isDisabled')) {
        this.next(++currentTabIndex);
      } else {
        tabs[currentTabIndex + 1].set('isActive', true);
      }
    } else {
      this.submit();
    }
  },

  /**
   * Back button action handler
   */
  back: function (index) {
    var tabs = this.get('tabs');
    var currentTabIndex = typeof(index) === 'number' ? index : this.get('currentTabIndex');
    if (currentTabIndex > 0) {
      tabs[currentTabIndex].set('isActive', false);
      if (tabs[currentTabIndex - 1].get('isDisabled')) {
        this.back(--currentTabIndex);
      } else {
        tabs[currentTabIndex - 1].set('isActive', true);
      }
    } else {
      App.router.send('back');
    }
  },

  updateConfigAttributesFromThemes: function () {
    this.get('allSelectedServiceNames').forEach(serviceName => this.updateAttributesFromTheme(serviceName));
  },

  validateOnTabSwitch: function () {
    const activeTab = this.get('tabs')[this.get('currentTabIndex')];
    if (activeTab && activeTab.get('validateOnSwitch')) {
      if (this.get('requestTimer')) {
        clearTimeout(this.get('requestTimer'));
      }
      if (this.get('validationRequest')) {
        this.get('validationRequest').abort();
      }
      if (this.get('recommendationsInProgress')) {
        this.valueObserver();
      } else {
        this.runServerSideValidation().done(() => this.set('validationRequest', null));
      }
    }
  }.observes('currentTabIndex')

});
