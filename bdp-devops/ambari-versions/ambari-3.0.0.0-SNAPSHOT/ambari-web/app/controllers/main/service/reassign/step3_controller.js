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

App.ReassignMasterWizardStep3Controller = Em.Controller.extend({
  name: 'reassignMasterWizardStep3Controller',

  componentSpecificTypesMap: {
    'NAMENODE': [
      {
        serviceName: 'HBASE',
        configTypes: ['hbase-site']
      },
      {
        serviceName: 'ACCUMULO',
        configTypes: ['accumulo-site']
      },
      {
        serviceName: 'HAWQ',
        configTypes: ['hawq-site', 'hdfs-client']
      }
    ],
    'RESOURCEMANAGER': [
      {
        serviceName: 'HAWQ',
        configTypes: ['hawq-site', 'yarn-client']
      }
    ]
  },

  /**
   * additional configs with template values
   * Part of value to substitute has following format: "<replace-value>"
   */
  additionalConfigsMap: [
    {
      componentName: 'RESOURCEMANAGER',
      configs: {
        'yarn-site': {
          'yarn.resourcemanager.address': '<replace-value>:8050',
          'yarn.resourcemanager.admin.address': '<replace-value>:8141',
          'yarn.resourcemanager.resource-tracker.address': '<replace-value>:8025',
          'yarn.resourcemanager.scheduler.address': '<replace-value>:8030',
          'yarn.resourcemanager.webapp.address': '<replace-value>:8088',
          'yarn.resourcemanager.webapp.https.address': '<replace-value>:8090',
          'yarn.resourcemanager.hostname': '<replace-value>'
        }
      }
    },
    {
      componentName: 'JOBTRACKER',
      configs: {
        'mapred-site': {
          'mapred.job.tracker.http.address': '<replace-value>:50030',
          'mapred.job.tracker': '<replace-value>:50300'
        }
      }
    },
    {
      componentName: 'SECONDARY_NAMENODE',
      configs: {
        'hdfs-site': {
          'dfs.secondary.http.address': '<replace-value>:50090'
        }
      },
      configs_Hadoop2: {
        'hdfs-site': {
          'dfs.namenode.secondary.http-address': '<replace-value>:50090'
        }
      }
    },
    {
      componentName: 'NAMENODE',
      configs: {
        'hdfs-site': {
          'dfs.http.address': '<replace-value>:50070',
          'dfs.https.address': '<replace-value>:50470'
        },
        'core-site': {
          'fs.default.name': 'hdfs://<replace-value>:8020'
        }
      },
      configs_Hadoop2: {
        'hdfs-site': {
          'dfs.namenode.rpc-address': '<replace-value>:8020',
          'dfs.namenode.http-address': '<replace-value>:50070',
          'dfs.namenode.https-address': '<replace-value>:50470'
        },
        'core-site': {
          'fs.defaultFS': 'hdfs://<replace-value>:8020'
        }
      }
    },
    {
      componentName: 'APP_TIMELINE_SERVER',
      configs: {
        'yarn-site': {
          'yarn.timeline-service.webapp.address': '<replace-value>:8188',
          'yarn.timeline-service.webapp.https.address': '<replace-value>:8190',
          'yarn.timeline-service.address': '<replace-value>:10200'
        }
      }
    },
    {
      componentName: 'TIMELINE_READER',
      configs: {
        'yarn-site': {
          'yarn.timeline-service.reader.webapp.address': '<replace-value>:8198',
          'yarn.timeline-service.reader.webapp.https.address': '<replace-value>:8199'
        }
      }
    },
    {
      componentName: 'OOZIE_SERVER',
      configs: {
        'oozie-site': {
          'oozie.base.url': 'http://<replace-value>:11000/oozie'
        },
        'core-site': {
          'hadoop.proxyuser.oozie.hosts': '<replace-value>'
        }
      }
    },
    {
      componentName: 'HIVE_METASTORE',
      configs: {
        'hive-site': {}
      }
    },
    {
      componentName: 'MYSQL_SERVER',
      configs: {
        'hive-site': {
          'javax.jdo.option.ConnectionURL': 'jdbc:mysql://<replace-value>/hive?createDatabaseIfNotExist=true'
        }
      }
    },
    {
      componentName: 'HISTORYSERVER',
      configs: {
        'mapred-site': {
          'mapreduce.jobhistory.webapp.address': '<replace-value>:19888',
          'mapreduce.jobhistory.address': '<replace-value>:10020'
        }
      }
    }
  ],

  secureConfigsMap: [
    {
      componentName: 'NAMENODE',
      configs: [
        {
          site: 'hdfs-site',
          keytab: 'dfs.namenode.keytab.file',
          principal: 'dfs.namenode.kerberos.principal'
        },
        {
          site: 'hdfs-site',
          keytab: 'dfs.web.authentication.kerberos.keytab',
          principal: 'dfs.web.authentication.kerberos.principal'
        }
      ]
    },
    {
      componentName: 'SECONDARY_NAMENODE',
      configs: [
        {
          site: 'hdfs-site',
          keytab: 'dfs.secondary.namenode.keytab.file',
          principal: 'dfs.secondary.namenode.kerberos.principal'
        },
        {
          site: 'hdfs-site',
          keytab: 'dfs.web.authentication.kerberos.keytab',
          principal: 'dfs.web.authentication.kerberos.principal'
        }
      ]
    },
    {
      componentName: 'RESOURCEMANAGER',
      configs: [
        {
          site: 'yarn-site',
          keytab: 'yarn.resourcemanager.keytab',
          principal: 'yarn.resourcemanager.principal'
        },
        {
          site: 'yarn-site',
          keytab: 'yarn.resourcemanager.webapp.spnego-keytab-file',
          principal: 'yarn.resourcemanager.webapp.spnego-principal'
        }
      ]
    },
    {
      componentName: 'OOZIE_SERVER',
      configs: [
        {
          site: 'oozie-site',
          keytab: 'oozie.authentication.kerberos.keytab',
          principal: 'oozie.authentication.kerberos.principal'
        },
        {
          site: 'oozie-site',
          keytab: 'oozie.service.HadoopAccessorService.keytab.file',
          principal: 'oozie.service.HadoopAccessorService.kerberos.principal'
        }
      ]
    },
    {
      componentName: 'WEBHCAT_SERVER',
      configs: [
        {
          site: 'webhcat-site',
          keytab: 'templeton.kerberos.keytab',
          principal: 'templeton.kerberos.principal'
        }
      ]
    },
    {
      componentName: 'HIVE_SERVER',
      configs: [
        {
          site: 'hive-site',
          keytab: 'hive.server2.authentication.kerberos.keytab',
          principal: 'hive.server2.authentication.kerberos.principal'
        },
        {
          site: 'hive-site',
          keytab: 'hive.server2.authentication.spnego.keytab',
          principal: 'hive.server2.authentication.spnego.principal'
        }
      ]
    },
    {
      componentName: 'HIVE_METASTORE',
      configs: [
        {
          site: 'hive-site',
          keytab: 'hive.metastore.kerberos.keytab.file',
          principal: 'hive.metastore.kerberos.principal'
        }
      ]
    }

  ],

  isLoaded: false,

  versionLoaded: true,

  hideDependenciesInfoBar: true,

  configs: null,

  configsAttributes: null,

  secureConfigs: [],

  stepConfigs: [],

  propertiesToChange: {},

  isSubmitDisabled: Em.computed.or('!isLoaded', 'submitButtonClicked'),

  /**
   * Is Submit-click processing now
   * @type {bool}
   */
  submitButtonClicked: false,

  loadStep: function () {
    this.set('submitButtonClicked', false);
    if (this.get('wizardController.isComponentWithReconfiguration')) {
      this.set('isLoaded', false);
      App.ajax.send({
        name: 'config.tags',
        sender: this,
        success: 'onLoadConfigsTags'
      });
    }
    else{
      this.set('isLoaded', true);
    }
  },

  clearStep: function () {
    this.setProperties({
      configs: null,
      configsAttributes: null,
      secureConfigs: [],
      propertiesToChange: {}
    });
  },

  onLoadConfigsTags: function (data) {
    var urlParams = this.getConfigUrlParams(this.get('content.reassign.component_name'), data);

    App.ajax.send({
      name: 'reassign.load_configs',
      sender: this,
      data: {
        urlParams: urlParams.join('|')
      },
      success: 'onLoadConfigs'
    });
  },

  getConfigUrlParams: function (componentName, data) {
    var urlParams = [];

    this.get('wizardController.serviceToConfigSiteMap')[componentName].forEach(function(site){
      if (data.Clusters.desired_configs[site]) {
        urlParams.push('(type=' + site + '&tag=' + data.Clusters.desired_configs[site].tag + ')');
      }
    });

    // specific cases for certain components
    var specificTypes = this.get('componentSpecificTypesMap')[componentName];
    if (specificTypes) {
      var services = App.Service.find();
      specificTypes.forEach(function (service) {
        if (services.someProperty('serviceName', service.serviceName)) {
          service.configTypes.forEach(function (site) {
            urlParams.push('(type=' + site + '&tag=' + data.Clusters.desired_configs[site].tag + ')');
          });
        }
      });
    }

    return urlParams;
  },

  renderServiceConfigs: function (configs) {
    var self = this,
      configCategories = [],
      displayedConfigs = [],
      serviceConfig = App.ServiceConfig.create({
        serviceName: 'MISC',
        configCategories: configCategories,
        showConfig: true,
        configs: displayedConfigs
      });
    App.get('router.mainController.isLoading').call(App.get('router.clusterController'), 'isConfigsPropertiesLoaded').done(function () {
      Em.keys(self.get('propertiesToChange')).forEach(function (type) {
        var service = App.config.get('serviceByConfigTypeMap')[type];
        if (service) {
          var serviceName = service.get('serviceName');
          if (!configCategories.someProperty('name', serviceName)) {
            configCategories.push(App.ServiceConfigCategory.create({
              name: serviceName,
              displayName: service.get('displayName')
            }));
          }
          this.get('propertiesToChange')[type].forEach(function (property) {
            var propertyName = property.name,
              stackProperty = App.configsCollection.getConfigByName(propertyName, type) || {},
              displayedProperty = App.ServiceConfigProperty.create({
                name: propertyName,
                displayName: propertyName,
                fileName: type
              }, stackProperty, {
                value: configs[type][propertyName],
                category: serviceName,
                isEditable: Boolean(stackProperty.isEditable !== false && !property.isSecure)
              });
            displayedConfigs.push(displayedProperty);
          });
        }
      }, self);
      self.setProperties({
        stepConfigs: [serviceConfig],
        selectedService: serviceConfig,
        isLoaded: true
      });
    });
  },

  onLoadConfigs: function (data) {
    // Find hawq-site.xml location
    var hawqSiteIndex = -1;
    for(var i = 0; i < data.items.length; i++){
      if(data.items[i].type == 'hawq-site'){
        hawqSiteIndex = i;
        break;
      }
    }

    // if certain services are deployed, include related site files to additionalConfigsMap and relatedServicesMap.
    if(hawqSiteIndex >= 0){ // if HAWQ is deployed
      var hawqSiteProperties = {
        'hawq_rm_yarn_address': '<replace-value>:8050',
        'hawq_rm_yarn_scheduler_address': '<replace-value>:8030'
      }

      var rmComponent = this.get('additionalConfigsMap').findProperty('componentName', "RESOURCEMANAGER");
      rmComponent.configs["hawq-site"] = hawqSiteProperties;

      if(data.items[hawqSiteIndex].properties["hawq_global_rm_type"].toLowerCase() === "yarn"){
        this.get('wizardController.relatedServicesMap')['RESOURCEMANAGER'].append('HAWQ');
      }

    }

    var componentName = this.get('content.reassign.component_name');
    var targetHostName = this.get('content.reassignHosts.target');
    var configs = {};
    var attributes = {};
    var secureConfigs = [];

    data.items.forEach(function (item) {
      configs[item.type] = item.properties;
      if (item.properties_attributes) {
        attributes[item.type] = item.properties_attributes;
      }
    });

    this.set('configsAttributes', attributes);

    this.setAdditionalConfigs(configs, componentName, targetHostName);
    this.setSecureConfigs(secureConfigs, configs, componentName);

    this.set('secureConfigs', secureConfigs);

    switch (componentName) {
      case 'NAMENODE':
        App.MoveNameNodeConfigInitializer.setup(this._getNnInitializerSettings(configs));
        configs = this.setDynamicConfigs(configs, App.MoveNameNodeConfigInitializer);
        App.MoveNameNodeConfigInitializer.cleanup();
        break;
      case 'RESOURCEMANAGER':
        App.MoveRmConfigInitializer.setup(this._getRmInitializerSettings(configs));
        var additionalDependencies = this._getRmAdditionalDependencies(configs);
        configs = this.setDynamicConfigs(configs, App.MoveRmConfigInitializer, additionalDependencies);
        App.MoveRmConfigInitializer.cleanup();
        break;
      case 'HIVE_METASTORE':
        App.MoveHmConfigInitializer.setup(this._getHiveInitializerSettings(configs));
        configs = this.setDynamicConfigs(configs, App.MoveHmConfigInitializer);
        App.MoveHmConfigInitializer.cleanup();
        break;
      case 'HIVE_SERVER':
        App.MoveHsConfigInitializer.setup(this._getHiveInitializerSettings(configs));
        configs = this.setDynamicConfigs(configs, App.MoveHsConfigInitializer);
        App.MoveHsConfigInitializer.cleanup();
        break;
      case 'WEBHCAT_SERVER':
        App.MoveWsConfigInitializer.setup(this._getWsInitializerSettings(configs));
        configs = this.setDynamicConfigs(configs, App.MoveWsConfigInitializer);
        App.MoveWsConfigInitializer.cleanup();
        break;
      case 'OOZIE_SERVER':
        App.MoveOSConfigInitializer.setup(this._getOsInitializerSettings(configs));
        configs = this.setDynamicConfigs(configs, App.MoveOSConfigInitializer);
        App.MoveOSConfigInitializer.cleanup();
    }

    this.renderServiceConfigs(configs);
    this.set('configs', configs);
  },

  /**
   * set additional configs
   * configs_Hadoop2 - configs which belongs to Hadoop 2 stack only
   * @param configs
   * @param componentName
   * @param replaceValue
   * @return {Boolean}
   */
  setAdditionalConfigs: function (configs, componentName, replaceValue) {
    var component = this.get('additionalConfigsMap').findProperty('componentName', componentName);

    if (Em.isNone(component)) return false;
    var additionalConfigs = (component.configs_Hadoop2) ? component.configs_Hadoop2 : component.configs;

    for (var site in additionalConfigs) {
      if (additionalConfigs.hasOwnProperty(site)) {
        for (var property in additionalConfigs[site]) {
          if (additionalConfigs[site].hasOwnProperty(property)) {
            if (App.get('isHaEnabled') && componentName === 'NAMENODE' && (['fs.defaultFS', 'dfs.namenode.rpc-address', 'dfs.namenode.http-address', 'dfs.namenode.https-address'].contains(property))) continue;

            configs[site][property] = additionalConfigs[site][property].replace('<replace-value>', replaceValue);
            if (!this.get('propertiesToChange').hasOwnProperty(site)) {
              this.get('propertiesToChange')[site] = [];
            }
            this.get('propertiesToChange')[site].push({
              name: property
            });
          }
        }
      }
    }
    return true;
  },

  /**
   * set secure configs for component
   * @param secureConfigs
   * @param configs
   * @param componentName
   * @return {Boolean}
   */
  setSecureConfigs: function (secureConfigs, configs, componentName) {
    var securityEnabled = App.get('isKerberosEnabled');
    var component = this.get('secureConfigsMap').findProperty('componentName', componentName);
    if (Em.isNone(component) || !securityEnabled) return false;

    component.configs.forEach(function (config) {
      secureConfigs.push({
        keytab: configs[config.site][config.keytab],
        principal: configs[config.site][config.principal]
      });
      if (!this.get('propertiesToChange').hasOwnProperty(config.site)) {
        this.get('propertiesToChange')[config.site] = [];
      }
      this.get('propertiesToChange')[config.site].push(
        {
          name: config.keytab,
          isSecure: true
        },
        {
          name: config.principal,
          isSecure: true
        }
      );
    }, this);
    return true;
  },

  /**
   * Get additional dependencies-data for App.MoveNameNodeConfigInitializer
   *
   * @param {object} configs
   * @returns {object}
   * @private
   * @method _getNnInitializerSettings
   */
  _getNnInitializerSettings: function (configs) {
    var ret = {};
    if (App.get('isHaEnabled')) {
      const configsObject = configs['hdfs-site'],
        nameSpaces = configsObject['dfs.nameservices'].split(','),
        nameSpacesCount = nameSpaces.length,
        propertyNames = Object.keys(configsObject);
      for (let i = 0; i < nameSpacesCount; i++) {
        const nameSpace = nameSpaces[i],
          propertyNameStart = `dfs.namenode.http-address.${nameSpace}.`,
          httpAddressPropertiesNames = propertyNames.filter(propertyName => propertyName.startsWith(propertyNameStart)),
          matchingPropertyName = httpAddressPropertiesNames.find(propertyName => configsObject[propertyName].startsWith(this.get('content.reassignHosts.source')));
        if (matchingPropertyName) {
          const nameNodeSuffixMatch = matchingPropertyName.match(new RegExp(`${propertyNameStart}(\\w+)`));
          ret.namespaceId = nameSpace;
          ret.suffix = nameNodeSuffixMatch && nameNodeSuffixMatch[1];
          break;
        }
      }
    }
    return ret;
  },

  /**
   * Settings used to the App.MoveRmConfigInitializer setup
   *
   * @param {object} configs
   * @returns {{suffix: string}}
   * @private
   * @method _getRmInitializerSettings
   */
  _getRmInitializerSettings: function (configs) {
    return {
      suffix: configs['yarn-site']['yarn.resourcemanager.hostname.rm1'] === this.get('content.reassignHosts.source') ? 'rm1': 'rm2'
    };
  },

  /**
   * Get additional dependencies-data for App.MoveRmConfigInitializer
   *
   * @param {object} configs
   * @returns {object}
   * @private
   * @method _getRmAdditionalDependencies
   */
  _getRmAdditionalDependencies: function (configs) {
    var ret = {};
    var rm1 = configs['yarn-site']['yarn.resourcemanager.hostname.rm1'];
    if (rm1) {
      ret.rm1 = rm1;
    }
    var rm2 = configs['yarn-site']['yarn.resourcemanager.hostname.rm2'];
    if (rm2) {
      ret.rm2 = rm2;
    }
    return ret;
  },

  /**
   * Settings used to the App.MoveHsConfigInitializer and App.MoveHmConfigInitializer setup
   *
   * @param {object} configs
   * @returns {{hiveUser: string}}
   * @private
   * @method _getHiveInitializerSettings
   */
  _getHiveInitializerSettings: function (configs) {
    return {
      hiveUser: configs['hive-env']['hive_user']
    };
  },

  /**
   * Settings used to the App.MoveWsConfigInitializer setup
   *
   * @param {object} configs
   * @returns {{webhcatUser: string}}
   * @private
   * @method _getWsInitializerSettings
   */
  _getWsInitializerSettings: function (configs) {
    return {
      webhcatUser: configs['hive-env']['webhcat_user']
    };
  },

  /**
   * Settings used to the App.MoveOSConfigInitializer setup
   *
   * @param {object} configs
   * @returns {object}
   * @private
   * @method _getOsInitializerSettings
   */
  _getOsInitializerSettings: function (configs) {
    var ret = {};
    var cfg = configs['oozie-env']['oozie_user'];
    if (cfg) {
      ret.oozieUser = cfg;
    }
    return ret;
  },

  /**
   * Set config values according to the new cluster topology
   *
   * @param {object} configs
   * @param {MoveComponentConfigInitializerClass} initializer
   * @param {object} [additionalDependencies={}]
   * @returns {object}
   * @method setDynamicConfigs
   */
  setDynamicConfigs: function (configs, initializer, additionalDependencies) {
    additionalDependencies = additionalDependencies || {};
    var topologyDB = this._prepareTopologyDB(),
      dependencies = this._prepareDependencies(additionalDependencies),
      initializerObjects = initializer.get('initializers'),
      uniqueInitializerObjects = initializer.get('uniqueInitializers');
    Em.keys(configs).forEach(function (site) {
      Em.keys(configs[site]).forEach(function (config) {
        // temporary object for initializer
        var cfg = {
          name: config,
          filename: site,
          value: configs[site][config]
        };
        configs[site][config] = initializer.initialValue(cfg, topologyDB, dependencies).value;
        if (initializerObjects[config] || uniqueInitializerObjects[config]) {
          if (!this.get('propertiesToChange').hasOwnProperty(site)) {
            this.get('propertiesToChange')[site] = [];
          }
          this.get('propertiesToChange')[site].push({
            name: config
          });
        }
      }, this);
    }, this);
    return configs;
  },

  /**
   *
   * @returns {extendedTopologyLocalDB}
   * @private
   * @method _prepareTopologyDB
   */
  _prepareTopologyDB: function () {
    var ret = this.get('content').getProperties(['masterComponentHosts', 'slaveComponentHosts', 'hosts']);
    ret.installedServices = App.Service.find().mapProperty('serviceName');
    return ret;
  },

  /**
   * Create dependencies for Config Initializers
   *
   * @param {object} additionalDependencies  some additional information that should be added
   * @returns {reassignComponentDependencies}
   * @private
   * @method _prepareDependencies
   */
  _prepareDependencies: function (additionalDependencies) {
    additionalDependencies = additionalDependencies || {};
    var ret = {};
    ret.sourceHostName = this.get('content.reassignHosts.source');
    ret.targetHostName = this.get('content.reassignHosts.target');
    return Em.merge(ret, additionalDependencies);
  },

  updateServiceConfigs: function () {
    var configs = this.get('configs');
    if (configs) {
      this.get('selectedService.configs').forEach(function (property) {
        var type = App.config.getConfigTagFromFileName(property.fileName);
        configs[type][property.name] = property.value;
      }, this);
    }
  },

  submit: function() {
    if (!this.get('submitButtonClicked')) {
      this.set('submitButtonClicked', true);
      App.get('router.mainAdminKerberosController').getKDCSessionState(function() {
        App.router.send("next");
      });
    }
  }
});
