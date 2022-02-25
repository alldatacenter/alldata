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

App.MainServiceInfoConfigsController = Em.Controller.extend(App.AddSecurityConfigs, App.ConfigsLoader,
  App.ServerValidatorMixin, App.EnhancedConfigsMixin, App.ThemesMappingMixin, App.ConfigsSaverMixin,
  App.ConfigsComparator, App.ComponentActionsByConfigs, {

  name: 'mainServiceInfoConfigsController',
  
  /**
   * Recommendations data will be completed on server side,
   * UI doesn't have to send all cluster data as hosts, configurations, config-groups, etc.
   */
  isRecommendationsAutoComplete: true,

  isHostsConfigsPage: false,

  isRecommendedLoaded: true,

  /**
   * Is true while request to recommendations is in progress
   * @type {Boolean}
   */
  recommendationsInProgress: false,

  dataIsLoaded: false,

  stepConfigs: [], //contains all field properties that are viewed in this service

  selectedService: null,

  selectedConfigGroup: null,

  groupsStore: App.ServiceConfigGroup.find(),

  /**
   * Configs tab for current service
   *
   * @type {App.Tab[]}
   */
  activeServiceTabs: function () {
    var selectedServiceName = this.get('selectedService.serviceName');
    return selectedServiceName ? App.Tab.find().rejectProperty('isCategorized').filterProperty('serviceName', selectedServiceName) : [];
  }.property('selectedService.serviceName'),

  /**
   * Currently opened configs tab
   *
   * @type {App.Tab}
   */
  activeTab: Em.computed.findBy('activeServiceTabs', 'isActive', true),

  /**
   * config groups for current service
   * @type {App.ConfigGroup[]}
   */
  configGroups: function() {
    return this.get('groupsStore').filterProperty('serviceName', this.get('content.serviceName'));
  }.property('content.serviceName', 'groupsStore.@each.serviceName'),

  defaultGroup: function() {
    return this.get('configGroups').findProperty('isDefault');
  }.property('configGroups'),

  isNonDefaultGroupSelectedInCompare: function() {
    return this.get('isCompareMode') && this.get('selectedConfigGroup') && !this.get('selectedConfigGroup.isDefault');
  }.property('selectedConfigGroup', 'isCompareMode'),

  dependentConfigGroups: function() {
    if (this.get('dependentServiceNames.length') === 0) return [];
    return this.get('groupsStore').filter(function(group) {
      return this.get('dependentServiceNames').contains(group.get('serviceName'));
    }, this);
  }.property('content.serviceName', 'dependentServiceNames', 'groupsStore.length', 'groupsStore.@each.name'),

  allConfigs: [],

  /**
   * Determines if save configs is in progress
   * @type {boolean}
   */
  saveInProgress: false,

  isCompareMode: false,

  preSelectedConfigVersion: null,

  /**
   * contain Service Config Property, when user proceed from Select Config Group dialog
   */
  overrideToAdd: null,

  /**
   * version selected to view
   */
  selectedVersion: null,

  /**
   * currently displayed service config version
   * @type {App.ServiceConfigVersion}
   */
  selectedVersionRecord: function() {
    const id = App.serviceConfigVersionsMapper.makeId(this.get('content.serviceName'), this.get('selectedVersion'));
    return App.ServiceConfigVersion.find(id);
  }.property('selectedVersion'),

  /**
   * note passed on configs save
   * @type {string}
   */
  serviceConfigVersionNote: '',

  versionLoaded: false,

  /**
   * Determines when data about config groups is loaded
   * Including recommendations with information about hosts in the each group
   * @type {boolean}
   */
  configGroupsAreLoaded: false,

  dependentServiceNames: [],
  /**
   * defines which service configs need to be loaded to stepConfigs
   * @type {string[]}
   */
  servicesToLoad: function() {
    return [this.get('content.serviceName')].concat(this.get('dependentServiceNames')).uniq();
  }.property('content.serviceName', 'dependentServiceNames.length'),

  /**
   * @type {boolean}
   */
  isCurrentSelected: function () {
    return App.ServiceConfigVersion.find(this.get('content.serviceName') + "_" + this.get('selectedVersion')).get('isCurrent');
  }.property('selectedVersion', 'content.serviceName', 'dataIsLoaded', 'versionLoaded'),

  /**
   * @type {boolean}
   */
  canEdit: function () {
    return (this.get('selectedVersion') == this.get('currentDefaultVersion') || !this.get('selectedConfigGroup.isDefault'))
        && !this.get('isCompareMode') && App.isAuthorized('SERVICE.MODIFY_CONFIGS');
  }.property('selectedVersion', 'isCompareMode', 'currentDefaultVersion', 'selectedConfigGroup.isDefault'),

  serviceConfigs: Em.computed.alias('App.config.preDefinedServiceConfigs'),

  /**
   * Number of errors in the configs in the selected service (only for AdvancedTab if App supports Enhanced Configs)
   * @type {number}
   */
  errorsCount: function() {
    return this.get('selectedService.configsWithErrors').filter(function(c) {
      return Em.isNone(c.get('isInDefaultTheme'));
    }).length;
  }.property('selectedService.configsWithErrors'),

  /**
   * Determines if Save-button should be disabled
   * Disabled if some configs have invalid values for selected service
   * or save-process currently in progress
   *
   * @type {boolean}
   */
  isSubmitDisabled: function () {
    if (!this.get('selectedService')) return true;
    return this.get('selectedService').get('errorCount') !== 0 || this.get('saveInProgress') || this.get('recommendationsInProgress');
  }.property('selectedService.errorCount', 'saveInProgress', 'recommendationsInProgress'),

  /**
   * Determines if some config value is changed
   * @type {boolean}
   */
  isPropertiesChanged: Em.computed.alias('selectedService.isPropertiesChanged'),

  /**
   * Filter text will be located here
   * @type {string}
   */
  filter: '',

  /**
   * List of filters for config properties to populate filter combobox
   * @type {{attributeName: string, attributeValue: boolean, caption: string}[]}
   */
  propertyFilters: [
    {
      attributeName: 'isOverridden',
      attributeValue: true,
      caption: 'common.combobox.dropdown.overridden',
      dependentOn: 'isNonDefaultGroupSelectedInCompare',
      disabledOnCondition: 'isNonDefaultGroupSelectedInCompare'
    },
    {
      attributeName: 'isFinal',
      attributeValue: true,
      caption: 'common.combobox.dropdown.final'
    },
    {
      attributeName: 'hasCompareDiffs',
      attributeValue: true,
      caption: 'common.combobox.dropdown.changed',
      dependentOn: 'isCompareMode',
      canBeExcluded: true
    },
    {
      attributeName: 'hasIssues',
      attributeValue: true,
      caption: 'common.combobox.dropdown.issues'
    }
  ],

  /**
   * Dropdown menu items in filter combobox
   * @type {{attributeName: string, attributeValue: string, name: string, selected: boolean}[]}
   */
  filterColumns: function () {
    var filterColumns = [];

    this.get('propertyFilters').forEach(function(filter) {
      if (filter.canBeExcluded && !(Em.isNone(filter.dependentOn) || this.get(filter.dependentOn))) {
        return; // exclude column
      }
      filterColumns.push(Ember.Object.create({
        attributeName: filter.attributeName,
        attributeValue: filter.attributeValue,
        name: this.t(filter.caption),
        selected: filter.dependentOn ? this.get(filter.dependentOn) : false,
        isDisabled: filter.disabledOnCondition ? this.get(filter.disabledOnCondition) : false
      }));
    }, this);
    return filterColumns;
  }.property('propertyFilters', 'isCompareMode', 'isNonDefaultGroupSelectedInCompare'),

  /**
   * Detects of some of the `password`-configs has not default value
   *
   * @type {boolean}
   */
  passwordConfigsAreChanged: function () {
    return this.get('stepConfigs')
      .findProperty('serviceName', this.get('selectedService.serviceName'))
      .get('configs')
      .filterProperty('displayType', 'password')
      .someProperty('isNotDefaultValue');
  }.property('stepConfigs.@each.configs', 'selectedService.serviceName'),

  /**
   * indicate whether service config version belongs to default config group
   * @param {object} version
   * @return {Boolean}
   * @private
   * @method isVersionDefault
   */
  isVersionDefault: function(version) {
    return App.ServiceConfigVersion.find(this.get('content.serviceName') + "_" + version).get('groupName') === App.ServiceConfigGroup.defaultGroupName;
  },

  /**
   * clear and set properties to default value
   * @method clearStep
   */
  clearStep: function () {
    this.abortRequests();
    App.set('componentToBeAdded', {});
    App.set('componentToBeDeleted', {});
    this.clearLoadInfo();
    this.clearSaveInfo();
    this.clearRecommendations();
    this.setProperties({
      saveInProgress: false,
      isInit: true,
      hash: null,
      dataIsLoaded: false,
      versionLoaded: false,
      filter: '',
      serviceConfigVersionNote: '',
      dependentServiceNames: [],
      configGroupsAreLoaded: false
    });
    this.get('filterColumns').setEach('selected', false);
    this.clearConfigs();
  },

  clearConfigs: function() {
    this.get('selectedConfigGroup', null);
    this.get('allConfigs').invoke('destroy');
    this.get('stepConfigs').invoke('destroy');
    this.set('stepConfigs', []);
    this.set('allConfigs', []);
    this.set('selectedService', null);
  },

  /**
   * "Finger-print" of the <code>stepConfigs</code>. Filled after first configGroup selecting
   * Used to determine if some changes were made (when user navigates away from this page)
   * @type {String|null}
   */
  hash: null,

  /**
   * Is this initial config group changing
   * @type {Boolean}
   */
  isInit: true,

  /**
   * Returns dependencies at all levels for service including dependencies for its childs, children dependencies
   * and so on.
   *
   * @param  {String} serviceName name of services to get dependencies
   * @returns {String[]}
   */
  getServicesDependencies: function(serviceName) {
    var dependencies = Em.getWithDefault(App.StackService.find(serviceName), 'dependentServiceNames', []);
    var loop = function(dependentServices, allDependencies) {
      return dependentServices.reduce(function(all, name) {
        var service = App.StackService.find(name);
        if (!service) {
          return all;
        }
        var serviceDependencies = service.get('dependentServiceNames');
        if (!serviceDependencies.length) {
          return all.concat(name);
        }
        var missed = _.intersection(_.difference(serviceDependencies, all), serviceDependencies);
        if (missed.length) {
          return loop(missed, all.concat(missed));
        }
        return all;
      }, allDependencies || dependentServices);
    };

    return loop(dependencies).uniq().without(serviceName).toArray();
  },

  /**
   * On load function
   * @method loadStep
   */
  loadStep: function () {
    var serviceName = this.get('content.serviceName'), self = this;
    this.clearStep();
    this.set('dependentServiceNames', this.getServicesDependencies(serviceName));
    this.trackRequestChain(this.loadConfigTheme(serviceName).always(function () {
      if (self.get('preSelectedConfigVersion')) {
        self.loadPreSelectedConfigVersion();
      } else {
        self.loadCurrentVersions();
      }
      self.trackRequest(self.loadServiceConfigVersions());
    }));
  },
  
  saveConfigs: function() {
    const newVersionToBeCreated = Math.max.apply(null, App.ServiceConfigVersion.find().mapProperty('version')) + 1;
    const isDefault = this.get('selectedConfigGroup.name') === App.ServiceConfigGroup.defaultGroupName;
    this.set('currentDefaultVersion', isDefault ? newVersionToBeCreated : this.get('currentDefaultVersion'));
    this._super();
  },

  /**
   * Generate "finger-print" for current <code>stepConfigs[0]</code>
   * Used to determine, if user has some unsaved changes (comparing with <code>hash</code>)
   * @returns {string|null}
   * @method getHash
   */
  getHash: function () {
    if (!this.get('selectedService.configs.length')) {
      return null;
    }
    var hash = {};
    var sortedProperties = this.get('selectedService.configs').slice().sort(function(a, b) {
      var first = a.get('id') || App.config.configId(a.get('name'), a.get('filename'));
      var second = b.get('id') || App.config.configId(b.get('name'), b.get('filename'));
      if (first < second) return -1;
      if (first > second) return 1;
      return 0;
    });
    sortedProperties.forEach(function (config) {
      var configId = '';
      if (config.isRequiredByAgent) {
        configId = config.get('id') || App.config.configId(config.get('name'), config.get('filename'));
        hash[configId] = {
          value: App.config.formatPropertyValue(config),
          overrides: [],
          isFinal: config.get('isFinal')
        };
        if (!config.get('overrides')) return;
        if (!config.get('overrides.length')) return;

        config.get('overrides').forEach(function (override) {
          hash[configId].overrides.push(App.config.formatPropertyValue(override));
        });
      }
    });
    return JSON.stringify(hash);
  },

  parseConfigData: function(data) {
    this.loadKerberosIdentitiesConfigs().done(identityConfigs => {
      this.prepareConfigObjects(data, identityConfigs);
      this.loadCompareVersionConfigs(this.get('allConfigs')).done(() => {
        this.addOverrides(data, this.get('allConfigs'));
        this.onLoadOverrides(this.get('allConfigs'));
        this.updateAttributesFromTheme(this.get('content.serviceName'));
      });
    });
  },

  prepareConfigObjects: function(data, identitiesMap) {
    this.get('stepConfigs').clear();

    var configs = [];
    data.items.forEach(function (version) {
      if (version.group_name === App.ServiceConfigGroup.defaultGroupName) {
        version.configurations.forEach(function (configObject) {
          configs = configs.concat(App.config.getConfigsFromJSON(configObject, true));
        });
      }
    });

    configs = App.config.sortConfigs(configs);
    /**
     * if property defined in stack but somehow it missed from cluster properties (can be after stack upgrade)
     * ui should add this properties to step configs
     */
    configs = this.mergeWithStackProperties(configs);

    var filenames = configs.mapProperty('fileName').uniq();
    //put properties from capacity-scheduler.xml into one config with textarea view
    if (filenames.contains('capacity-scheduler.xml')) {
      configs = App.config.addYarnCapacityScheduler(configs);
    }

    this.setPropertyIsVisible(configs);
    this.setPropertyIsEditable(configs, identitiesMap);
    this.set('allConfigs', configs);
  },

  setPropertyIsVisible: function (configs) {
    if (this.get('content.serviceName') === 'KERBEROS') {
      var kdc_type = configs.findProperty('name', 'kdc_type');
      if (kdc_type.get('value') === 'none') {
        configs.findProperty('name', 'kdc_hosts').set('isVisible', false);
        configs.findProperty('name', 'admin_server_host').set('isVisible', false);
        configs.findProperty('name', 'domains').set('isVisible', false);
      } else if (kdc_type.get('value') === 'active-directory') {
        configs.findProperty('name', 'container_dn').set('isVisible', true);
        configs.findProperty('name', 'ldap_url').set('isVisible', true);
      } else if (kdc_type.get('value') === 'ipa') {
        configs.findProperty('name', 'group').set('isVisible', true);
        configs.findProperty('name', 'manage_krb5_conf').set('value', false);//TODO
        configs.findProperty('name', 'install_packages').set('value', false);//TODO
        configs.findProperty('name', 'admin_server_host').set('isVisible', false);
        configs.findProperty('name', 'domains').set('isVisible', false);
      }
    }
  },

  /**
   * Set <code>isEditable<code> property based on selected group, security
   * and controller restriction
   * @param configs
   * @param identitiesMap
   */
  setPropertyIsEditable: function (configs, identitiesMap) {
    if (!this.get('selectedConfigGroup.isDefault') || !this.get('canEdit')) {
      configs.setEach('isEditable', false);
    } else if (App.get('isKerberosEnabled')) {
      configs.forEach(function (c) {
        if (identitiesMap[c.get('id')]) {
          c.set('isConfigIdentity', true);
          c.set('isEditable', false);
          c.set('isSecureConfig', true);
          c.set('description', App.config.kerberosIdentitiesDescription(c.get('description')));
        }
      });
    }
  },

  /**
   * Load config Identities
   *
   * @returns {*}
   */
  loadKerberosIdentitiesConfigs: function () {
    var dfd = $.Deferred();
    if (App.get('isKerberosEnabled')) {
      this.loadClusterDescriptorConfigs().then(function (kerberosDescriptor) {
        dfd.resolve(App.config.parseDescriptor(kerberosDescriptor));
      });
    } else {
      dfd.resolve(true);
    }
    return dfd.promise();
  },

  /**
   * adds properties form stack that doesn't belong to cluster
   * to step configs
   * also set recommended value if isn't exists
   *
   * @return {App.ServiceConfigProperty[]}
   * @method mergeWithStackProperties
   */
  mergeWithStackProperties: function (configs) {
    App.config.getPropertiesFromTheme(this.get('content.serviceName')).forEach(function (advanced_id) {
      if (!configs.someProperty('id', advanced_id)) {
        var advanced = App.configsCollection.getConfig(advanced_id);
        if (advanced) {
          advanced.savedValue = null;
          advanced.isNotSaved = advanced.isRequiredByAgent;
          configs.pushObject(App.ServiceConfigProperty.create(advanced));
        }
      }
    });
    return configs;
  },

  addOverrides: function(data, allConfigs) {
    var self = this;
    data.items.forEach(function(group) {
      if (![App.ServiceConfigGroup.defaultGroupName, App.ServiceConfigGroup.deletedGroupName].contains(group.group_name)) {
        var configGroup = App.ServiceConfigGroup.find().filterProperty('serviceName', group.service_name).findProperty('name', group.group_name);
        group.configurations.forEach(function(config) {
          for (var prop in config.properties) {
            var fileName = App.config.getOriginalFileName(config.type);
            var serviceConfig = allConfigs.filterProperty('name', prop).findProperty('filename', fileName);
            if (serviceConfig) {
              var value = App.config.formatPropertyValue(serviceConfig, config.properties[prop]);
              var isFinal = !!(config.properties_attributes && config.properties_attributes.final && config.properties_attributes.final[prop]);
              var overridePlainObject = {
                "value": value,
                "isVisible": self.get('selectedConfigGroup.isDefault') || configGroup.get('name') === self.get('selectedConfigGroup.name'),
                "savedValue": value,
                "isFinal": isFinal,
                "savedIsFinal": isFinal,
                "isEditable": self.get('canEdit') && configGroup.get('name') === self.get('selectedConfigGroup.name')
              };
              App.config.createOverride(serviceConfig, overridePlainObject, configGroup);
            } else {
              var isEditable = self.get('canEdit') && configGroup.get('name') === self.get('selectedConfigGroup.name');
              var propValue = config.properties[prop];
              var newConfig = App.ServiceConfigProperty.create(App.config.createDefaultConfig(prop, fileName, false, {
                overrides: [],
                displayType: 'label',
                value: 'Undefined',
                isPropertyOverridable: false,
                overrideValues: [propValue],
                overrideIsFinalValues: [false]
              }));
              newConfig.get('overrides').push(App.config.createCustomGroupConfig({
                propertyName: prop,
                filename: fileName,
                value: propValue,
                savedValue: propValue,
                isEditable: isEditable
              }, configGroup));
              allConfigs.push(newConfig);
            }
          }
        });
      }
    });
  },

  /**
   * @param allConfigs
   * @private
   * @method onLoadOverrides
   */
  onLoadOverrides: function (allConfigs) {
    this.get('servicesToLoad').forEach(function(serviceName) {
      var configGroups = serviceName === this.get('content.serviceName') ? this.get('configGroups') : this.get('dependentConfigGroups').filterProperty('serviceName', serviceName);
      var configTypes = App.StackService.find(serviceName).get('configTypeList');
      var configsByService = this.get('allConfigs').filter(function (c) {
        return configTypes.contains(App.config.getConfigTagFromFileName(c.get('filename')));
      });
      var serviceConfig = App.config.createServiceConfig(serviceName, configGroups, configsByService, configsByService.length);
      this.addHostNamesToConfigs(serviceConfig);
      this.get('stepConfigs').pushObject(serviceConfig);
    }, this);

    var selectedService = this.get('stepConfigs').findProperty('serviceName', this.get('content.serviceName'));
    this.set('selectedService', selectedService);
    this.checkOverrideProperty(selectedService);

    var isRangerPresent =  App.Service.find().someProperty('serviceName', 'RANGER');
    if (isRangerPresent) {
      App.router.get('mainServiceInfoSummaryController').updateRangerPluginsStatus();
      this.setVisibilityForRangerProperties(selectedService);
      this.loadConfigRecommendations(null, this._onLoadComplete.bind(this));
      App.loadTimer.finish('Service Configs Page');
    } else {
      var mainController = App.get('router.mainController');
      var clusterController = App.get('router.clusterController');
      var self = this;
      mainController.isLoading.call(clusterController, 'clusterEnv').done(function () {
        var isExternalRangerSetup = clusterController.get("clusterEnv")["properties"]["enable_external_ranger"];
        if (isExternalRangerSetup) {
          self.setVisibilityForRangerProperties(selectedService);
        } else {
          App.config.removeRangerConfigs(self.get('stepConfigs'));
        }
        self.loadConfigRecommendations(null, self._onLoadComplete.bind(self));
        App.loadTimer.finish('Service Configs Page');
      });
    }
  },

  /**
   * @method _getRecommendationsForDependenciesCallback
   */
  _onLoadComplete: function () {
    this.get('stepConfigs').forEach(function(serviceConfig){
      serviceConfig.set('initConfigsLength', serviceConfig.get('configs.length'));
    });
    this.setProperties({
      dataIsLoaded: true,
      versionLoaded: true,
      isInit: false,
      hash: this.getHash()
    });
  },

  /**
   * hide properties from Advanced ranger category that match pattern
   * if property with dependentConfigPattern is false otherwise don't hide
   * @param serviceConfig
   * @private
   * @method setVisibilityForRangerProperties
   */
  setVisibilityForRangerProperties: function(serviceConfig) {
    var category = "Advanced ranger-{0}-plugin-properties".format(this.get('content.serviceName').toLowerCase());
    if (serviceConfig.configCategories.findProperty('name', category)) {
      var patternConfig = serviceConfig.configs.findProperty('dependentConfigPattern');
      if (patternConfig) {
        var value = patternConfig.get('value') === true || ["yes", "true"].contains(patternConfig.get('value').toLowerCase());

        serviceConfig.configs.filter(function(c) {
          if (c.get('category') === category && c.get('name').match(patternConfig.get('dependentConfigPattern')) && c.get('name') !== patternConfig.get('name'))
            c.set('isVisible', value);
        });
      }
    }
  },

  /**
   * Allow update property if recommendations
   * is based on changing property
   *
   * @param parentProperties
   * @returns {boolean}
   * @override
   */
  allowUpdateProperty: function(parentProperties) {
    return !!(parentProperties && parentProperties.length);
  },

  /**
   * trigger App.config.createOverride
   * @param {Object[]} stepConfig
   * @private
   * @method checkOverrideProperty
   */
  checkOverrideProperty: function (stepConfig) {
    var overrideToAdd = this.get('overrideToAdd');
    var value = !!this.get('overrideToAdd.widget') ? Em.get(overrideToAdd, 'value') : '';
    if (overrideToAdd) {
      overrideToAdd = stepConfig.configs.filter(function(c){
        return c.name == overrideToAdd.name && c.filename === overrideToAdd.filename;
      });
      if (overrideToAdd[0]) {
        App.config.createOverride(overrideToAdd[0], {"isEditable": true, "value": value}, this.get('selectedConfigGroup'));
        this.set('overrideToAdd', null);
      }
    }
  },

  /**
   *
   * @param serviceConfig
   */
  addHostNamesToConfigs: function(serviceConfig) {
    serviceConfig.get('configCategories').forEach(function(c) {
      if (c.showHost) {
        var stackComponent = App.StackServiceComponent.find(c.name),
          value = this.getComponentHostValue(c.name);
        var hProperty = App.config.createHostNameProperty(serviceConfig.get('serviceName'), c.name, value, stackComponent);
        serviceConfig.get('configs').push(App.ServiceConfigProperty.create(hProperty));
      }
    }, this);
  },

  /**
   * Method to get host for master or slave component
   *
   * @param componentName
   * @returns {Array}
   */
  getComponentHostValue: function(componentName) {
    var stackComponent = App.StackServiceComponent.find(componentName);
    var component = stackComponent.get('isMaster') ? App.MasterComponent.find(componentName) : App.SlaveComponent.find(componentName);
    return component.get('hostNames') || []
  },

  /**
   * Trigger loadSelectedVersion
   * @method doCancel
   */
  doCancel: function () {
    this.set('preSelectedConfigVersion', null);
    App.set('componentToBeAdded', {});
    App.set('componentToBeDeleted', {});
    this.clearRecommendations();
    this.loadSelectedVersion(this.get('selectedVersion'), this.get('selectedConfigGroup'));
  },

  /**
   * trigger restartAllServiceHostComponents(batchUtils) if confirmed in popup
   * @method restartAllStaleConfigComponents
   * @return App.showConfirmationFeedBackPopup
   */
  restartAllStaleConfigComponents: function () {
    var self = this;
    var serviceDisplayName = this.get('content.displayName');
    var bodyMessage = Em.Object.create({
      confirmMsg: Em.I18n.t('services.service.restartAll.confirmMsg').format(serviceDisplayName),
      confirmButton: Em.I18n.t('services.service.restartAll.confirmButton'),
      additionalWarningMsg: this.get('content.passiveState') === 'OFF' ? Em.I18n.t('services.service.restartAll.warningMsg.turnOnMM').format(serviceDisplayName) : null
    });

    var isNNAffected = false;
    var restartRequiredHostsAndComponents = this.get('content.restartRequiredHostsAndComponents');
    for (var hostName in restartRequiredHostsAndComponents) {
      restartRequiredHostsAndComponents[hostName].forEach(function (hostComponent) {
        if (hostComponent === 'NameNode')
         isNNAffected = true;
      })
    }
    if (this.get('content.serviceName') === 'HDFS' && isNNAffected &&
      this.get('content.hostComponents').filterProperty('componentName', 'NAMENODE').someProperty('workStatus', App.HostComponentStatus.started)) {
      App.router.get('mainServiceItemController').checkNnLastCheckpointTime(function () {
        return App.showConfirmationFeedBackPopup(function (query) {
          var selectedService = self.get('content.id');
          batchUtils.restartAllServiceHostComponents(serviceDisplayName, selectedService, true, query);
        }, bodyMessage);
      });
    } else {
      return App.showConfirmationFeedBackPopup(function (query) {
        var selectedService = self.get('content.id');
        batchUtils.restartAllServiceHostComponents(serviceDisplayName, selectedService, true, query);
      }, bodyMessage);
    }
  },

  /**
   * trigger launchHostComponentRollingRestart(batchUtils)
   * @method rollingRestartStaleConfigSlaveComponents
   */
  rollingRestartStaleConfigSlaveComponents: function (componentName) {
    batchUtils.launchHostComponentRollingRestart(componentName.context, this.get('content.displayName'), this.get('content.passiveState') === "ON", true);
  },

  /**
   * trigger showItemsShouldBeRestarted popup with hosts that requires restart
   * @param {{context: object}} event
   * @method showHostsShouldBeRestarted
   */
  showHostsShouldBeRestarted: function (event) {
    var restartRequiredHostsAndComponents = event.context.restartRequiredHostsAndComponents;
    var hosts = [];
    for (var hostName in restartRequiredHostsAndComponents) {
      hosts.push(hostName);
    }
    var hostsText = hosts.length === 1 ? Em.I18n.t('common.host') : Em.I18n.t('common.hosts');
    hosts = hosts.join(', ');
    this.showItemsShouldBeRestarted(hosts, Em.I18n.t('service.service.config.restartService.shouldBeRestarted').format(hostsText));
  },

  /**
   * trigger showItemsShouldBeRestarted popup with components that requires restart
   * @param {{context: object}} event
   * @method showComponentsShouldBeRestarted
   */
  showComponentsShouldBeRestarted: function (event) {
    var restartRequiredHostsAndComponents = event.context.restartRequiredHostsAndComponents;
    var hostsComponets = [];
    var componentsObject = {};
    for (var hostName in restartRequiredHostsAndComponents) {
      restartRequiredHostsAndComponents[hostName].forEach(function (hostComponent) {
        hostsComponets.push(hostComponent);
        if (componentsObject[hostComponent] != undefined) {
          componentsObject[hostComponent]++;
        } else {
          componentsObject[hostComponent] = 1;
        }
      })
    }
    var componentsList = [];
    for (var obj in componentsObject) {
      var componentDisplayName = componentsObject[obj] > 1 ? obj + 's' : obj;
      componentsList.push(componentsObject[obj] + ' ' + componentDisplayName);
    }
    var componentsText = componentsList.length === 1 ? Em.I18n.t('common.component') : Em.I18n.t('common.components');
    hostsComponets = componentsList.join(', ');
    this.showItemsShouldBeRestarted(hostsComponets, Em.I18n.t('service.service.config.restartService.shouldBeRestarted').format(componentsText));
  },

  /**
   * Show popup with selectable (@see App.SelectablePopupBodyView) list of items
   * @param {string} content string with comma-separated list of hostNames or componentNames
   * @param {string} header popup header
   * @returns {App.ModalPopup}
   * @method showItemsShouldBeRestarted
   */
  showItemsShouldBeRestarted: function (content, header) {
    return App.ModalPopup.show({
      content: content,
      header: header,
      bodyClass: App.SelectablePopupBodyView,
      secondary: null
    });
  },

  /**
   * trigger manageConfigurationGroups
   * @method manageConfigurationGroup
   */
  manageConfigurationGroup: function () {
    App.router.get('manageConfigGroupsController').manageConfigurationGroups(null, this.get('content'));
  },

  /**
   * If user changes cfg group if some configs was changed popup with propose to save changes must be shown
   * @param {object} event - triggered event for selecting another config-group
   * @method selectConfigGroup
   */
  selectConfigGroup: function (event) {
    var self = this;

    function callback() {
      self.doSelectConfigGroup(event);
    }

    if (!this.get('isInit')) {
      if (this.hasUnsavedChanges()) {
        this.showSavePopup(null, callback);
        return;
      }
    }
    callback();
  },

  /**
   * switch view to selected group
   * @param event
   * @method selectConfigGroup
   */
  doSelectConfigGroup: function (event) {
    App.loadTimer.start('Service Configs Page');
    var configGroupVersions = App.ServiceConfigVersion.find().filterProperty('groupId', event.context.get('id'));
    //check whether config group has config versions
    if (event.context.get('isDefault')) {
      this.loadCurrentVersions();
    } else if (configGroupVersions.length > 0) {
      this.loadSelectedVersion(configGroupVersions.findProperty('isCurrent').get('version'), event.context);
    } else {
      this.loadSelectedVersion(null, event.context);
    }
  }
});
