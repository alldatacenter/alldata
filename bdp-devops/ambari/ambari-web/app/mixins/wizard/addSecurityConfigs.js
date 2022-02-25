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
var blueprintUtils = require('utils/blueprint');

/**
 * Mixin for loading and setting secure configs
 *
 * @type {Ember.Mixin}
 */
App.AddSecurityConfigs = Em.Mixin.create({

  kerberosDescriptor: {},

  kerberosDescriptorProperties: require('data/configs/wizards/kerberos_descriptor_properties'),

  /**
   * security configs, which values should be modified after APPLY CONFIGURATIONS stage
   */
  secureConfigs: [
    {
      name: 'zookeeper_principal_name',
      serviceName: 'ZOOKEEPER'
    },
    {
      name: 'knox_principal_name',
      serviceName: 'KNOX'
    },
    {
      name: 'storm_principal_name',
      serviceName: 'STORM'
    },
    {
      name: 'nimbus_principal_name',
      serviceName: 'STORM'
    }
  ],

  /**
   *
   * @param {object[]} items - stack descriptor json response
   * @returns {App.ServiceConfigProperty[]}
   */
  createServicesStackDescriptorConfigs: function (items) {
    var self = this;
    var configs = [];
    var clusterConfigs = [];
    var kerberosDescriptor = Em.get(items, 'KerberosDescriptor.kerberos_descriptor');
    this.set('kerberosDescriptor', kerberosDescriptor);
    // generate configs for root level properties object, currently realm, keytab_dir
    clusterConfigs = clusterConfigs.concat(this.expandKerberosStackDescriptorProps(kerberosDescriptor.properties, 'Cluster', 'stackConfigs'));
    // generate configs for root level identities object, currently spnego property
    clusterConfigs = clusterConfigs.concat(this.createConfigsByIdentities(kerberosDescriptor.identities, 'Cluster'));
    kerberosDescriptor.services.forEach(function (service) {
      var serviceName = service.name;
      // generate configs for service level identity objects
      configs = configs.concat(self.createResourceConfigs(service, serviceName));
      // generate configs for service component level identity  object
      Em.getWithDefault(service, 'components', []).forEach(function (component) {
        configs = configs.concat(self.createResourceConfigs(component, serviceName));
      });
    });
    // unite cluster, service and component configs
    configs = configs.concat(clusterConfigs);
    self.processConfigReferences(kerberosDescriptor, configs);
    return configs;
  },

  /**
   *
   * @param {Object} resource
   * @param {String} serviceName
   * @return {Array}
   */
  createResourceConfigs: function (resource, serviceName) {
    var identityConfigs = [];
    var resourceConfigs = [];
    if (resource.identities) {
      identityConfigs = this.createConfigsByIdentities(resource.identities, serviceName);
    }
    if (resource.configurations) {
      resource.configurations.forEach(function (_configuration) {
        for (var key in _configuration) {
          resourceConfigs = resourceConfigs.concat(this.expandKerberosStackDescriptorProps(_configuration[key], serviceName, key));
        }
      }, this);
    }
    return identityConfigs.concat(resourceConfigs);
  },

  /**
   * Create service properties based on component identity
   *
   * @param {object[]} identities
   * @param {string} serviceName
   * @returns {App.ServiceConfigProperty[]}
   */
  createConfigsByIdentities: function (identities, serviceName) {
    var self = this;
    var configs = [];
    identities.forEach(function (identity) {
      var defaultObject = {
        isConfigIdentity: true,
        isOverridable: false,
        // the user should not be changing the principal name and keytab file values for identity items
        // that reference some other identity item,
        // if an identity entry has a "reference" attribute or the name attribute starts with a "/"
        isVisible: !Boolean(identity.reference || identity.name.startsWith('/')),
        isSecureConfig: true,
        serviceName: serviceName,
        name: identity.name,
        identityType: identity.principal && identity.principal.type
      };
      self.parseIdentityObject(identity).forEach(function (item) {
        configs.push(App.ServiceConfigProperty.create($.extend({}, defaultObject, item)));
      });
    });

    return configs;
  },

  /**
   * Bootstrap base object according to identity info. Generate objects will be converted to
   * App.ServiceConfigProperty model class instances.
   *
   * @param {object} identity
   * @returns {object[]}
   */
  parseIdentityObject: function (identity) {
    var result = [];
    var name = identity.name;
    var self = this;
    Em.keys(identity).without('name').forEach(function (item) {
      var configObject = {};
      var prop = identity[item];
      var itemValue = prop[{keytab: 'file', principal: 'value'}[item]];
      var predefinedProperty;
      // skip inherited property without `configuration` and `keytab` or `file` values
      if (!prop.configuration && !itemValue) return;
      // inherited property with value should not observe value from reference
      if (name.startsWith('/') && !itemValue) {
        configObject.referenceProperty = name.substring(1) + ':' + item;
        configObject.isEditable = false;
      }
      Em.setProperties(configObject, {
        recommendedValue: itemValue,
        initialValue: itemValue,
        defaultValue: itemValue,
        value: itemValue
      });
      configObject.filename = prop.configuration ? prop.configuration.split('/')[0] : 'cluster-env';
      configObject.name = prop.configuration ? prop.configuration.split('/')[1] : name + '_' + item;
      predefinedProperty = self.get('kerberosDescriptorProperties').findProperty('name', configObject.name);
      configObject.displayName = self._getDisplayNameForConfig(configObject.name, configObject.filename);
      configObject.index = predefinedProperty && !Em.isNone(predefinedProperty.index) ? predefinedProperty.index : Infinity;
      result.push(configObject);
    });
    return result;
  },

  /**
   * Get new config display name basing on its name and filename
   * If config <code>fileName</code> is `cluster-env`, normalizing for its <code>name</code> is used (@see App.format.normalizeName)
   * If config is predefined in the <code>secureProperties</code> (and it's displayName isn't empty there), predefined displayName is used
   * Otherwise - config <code>name</code> is returned
   *
   * @param {string} name config name
   * @param {string} fileName config filename
   * @returns {String} new config display name
   * @method _getDisplayNameForConfig
   * @private
   */
  _getDisplayNameForConfig: function (name, fileName) {
    var predefinedConfig = App.config.get('kerberosIdentitiesMap')[App.config.configId(name, fileName)];
    return (predefinedConfig && predefinedConfig.displayName)
      ? predefinedConfig.displayName
      : fileName == 'cluster-env' ? App.format.normalizeName(name) : name;
  },

  /**
   * Wrap kerberos properties to App.ServiceConfigProperty model class instances.
   *
   * @param {object} kerberosProperties
   * @param {string} serviceName
   * @param {string} filename
   * @returns {App.ServiceConfigProperty[]}
   */
  expandKerberosStackDescriptorProps: function (kerberosProperties, serviceName, filename) {
    var configs = [];

    for (var propertyName in kerberosProperties) {
      var predefinedProperty = this.get('kerberosDescriptorProperties').findProperty('name', propertyName);
      var value = kerberosProperties[propertyName];
      var isRequired = ['additional_realms', 'principal_suffix'].contains(propertyName) ? false : value !== "";
      var propertyObject = {
        name: propertyName,
        value: value,
        defaultValue: value,
        recommendedValue: value,
        initialValue: value,
        serviceName: serviceName,
        filename: filename,
        displayName: serviceName == "Cluster" ? App.format.normalizeName(propertyName) : propertyName,
        isOverridable: false,
        isEditable: propertyName != 'realm',
        isRequired: isRequired,
        isSecureConfig: true,
        placeholderText: predefinedProperty && !Em.isNone(predefinedProperty.index) ? predefinedProperty.placeholderText : '',
        index: predefinedProperty && !Em.isNone(predefinedProperty.index) ? predefinedProperty.index : Infinity
      };
      configs.push(App.ServiceConfigProperty.create(propertyObject));
    }

    return configs;
  },


  /**
   * Take care about configs that should observe value from referenced configs.
   * Reference is set with `referenceProperty` key.
   *
   * @param {object[]} kerberosDescriptor
   * @param {App.ServiceConfigProperty[]} configs
   */
  processConfigReferences: function (kerberosDescriptor, configs) {
    var identities = kerberosDescriptor.identities;

    /**
     * Returns indentity object with additional attribute `referencePath`.
     * Reference path depends on how deep identity is. Each level separated by `/` sign.
     *
     * @param {object} identity
     * @param {string} [prefix=false] prefix to append e.g. 'SERVICE_NAME'
     * @returns {object} identity object
     */
    var setReferencePath = function(identity, prefix) {
      var name = Em.getWithDefault(identity, 'name', false);
      if (name) {
        if (prefix) {
          name = prefix + '/' + name;
        }
        identity.referencePath = name;
      }
      return identity;
    };

    // map all identities and add attribute `referencePath`
    // `referencePath` is a path to identity it can be 1-3 levels
    // 1 for "/global" identity e.g. `/spnego`
    // 2 for "/SERVICE/identity"
    // 3 for "/SERVICE/COMPONENT/identity"
    identities = identities.map(function(i) {
      return setReferencePath(i);
    })
    .concat(kerberosDescriptor.services.map(function (service) {
      var serviceName = Em.getWithDefault(service, 'name', false);
      var serviceIdentities = Em.getWithDefault(service, 'identities', []).map(function(i) {
        return setReferencePath(i, serviceName);
      });
      var componentIdentities = Em.getWithDefault(service || {}, 'components', []).map(function(i) {
        var componentName = Em.getWithDefault(i, 'name', false);
        return Em.getWithDefault(i, 'identities', []).map(function(componentIdentity) {
          return setReferencePath(componentIdentity, serviceName + '/' + componentName);
        });
      }).reduce(function(p, c) {
        return p.concat(c);
      }, []);
      serviceIdentities.pushObjects(componentIdentities);
      return serviceIdentities;
    }).reduce(function (p, c) {
      return p.concat(c);
    }, []));
    // clean up array
    identities = identities.compact().without(undefined);
    configs.forEach(function (item) {
      var reference = item.get('referenceProperty');
      if (!!reference) {
        // first find identity by `name`
        // if not found try to find by `referencePath`
        var identity = Em.getWithDefault(identities.findProperty('name', reference.split(':')[0]) || {}, reference.split(':')[1], false) ||
              Em.getWithDefault(identities.findProperty('referencePath', reference.split(':')[0]) || {}, reference.split(':')[1], false);
        if (identity && !!identity.configuration) {
          item.set('observesValueFrom', identity.configuration.split('/')[1]);
        } else {
          item.set('observesValueFrom', reference.replace(':', '_'));
        }
      }
    });
  },

  /**
   * update the kerberos descriptor to be put on cluster resource with user customizations
   * @param kerberosDescriptor {Object}
   * @param configs {Object}
   */
  updateKerberosDescriptor: function (kerberosDescriptor, configs) {
    configs.forEach(function (_config) {
      var isConfigUpdated;
      var isStackResource = true;
      isConfigUpdated = this.updateResourceIdentityConfigs(kerberosDescriptor, _config, isStackResource);
      if (!isConfigUpdated) {
        kerberosDescriptor.services.forEach(function (_service) {
          isConfigUpdated = this.updateResourceIdentityConfigs(_service, _config);
          if (!isConfigUpdated) {
            Em.getWithDefault(_service, 'components', []).forEach(function (_component) {
              isConfigUpdated = this.updateResourceIdentityConfigs(_component, _config);
            }, this);
          }
        }, this);
      }
    }, this);
  },

  /**
   * Updates the identity configs or configurations at a resource. A resource could be
   * 1) Stack
   * 2) Service
   * 3) Component
   * @param resource
   * @param config
   * @param isStackResource
   * @return boolean
   */
  updateResourceIdentityConfigs: function (resource, config, isStackResource) {
    var isConfigUpdated;
    var identities = resource.identities;
    var properties = !!isStackResource ? resource.properties : resource.configurations;
    isConfigUpdated = this.updateDescriptorConfigs(properties, config);
    if (!isConfigUpdated) {
      if (identities) {
        isConfigUpdated = this.updateDescriptorIdentityConfig(identities, config);
      }
    }
    return isConfigUpdated;
  },

  /**
   * This function updates stack/service/component level configurations of the kerberos descriptor
   * with the values entered by the user on the rendered ui
   * @param configurations
   * @param config
   * @return boolean
   */
  updateDescriptorConfigs: function (configurations, config) {
    var isConfigUpdated;
    if (!!configurations) {
      if (Array.isArray(configurations)) {
        configurations.forEach(function (_configuration) {
          for (var key in _configuration) {
            if (Object.keys(_configuration[key]).contains(config.name) && App.config.getConfigTagFromFileName(config.filename) === key) {
              _configuration[key][config.name] = config.value;
              isConfigUpdated = true
            }
          }
        }, this);
      } else if (Object.keys(configurations).contains(config.name) && App.config.getConfigTagFromFileName(config.filename) === 'stackConfigs') {
        configurations[config.name] = config.value;
        isConfigUpdated = true;
      }
    }
    return isConfigUpdated;
  },


  /**
   * This function updates stack/service/component level kerberos descriptor identities (principal and keytab)
   * with the values entered by the user on the rendered ui
   * @param identities
   * @param config
   * @return boolean
   */
  updateDescriptorIdentityConfig: function (identities, config) {
    var isConfigUpdated = false;
    identities.forEach(function (identity) {
      var keys = Em.keys(identity).without('name');
      keys.forEach(function (item) {
        var prop = identity[item];

        // compare ui rendered config against identity with `configuration attribute` (Most of the identities have `configuration attribute`)
        var isIdentityWithConfig = (prop.configuration && prop.configuration.split('/')[0] === App.config.getConfigTagFromFileName(config.filename) && prop.configuration.split('/')[1] === config.name);

        // compare ui rendered config against identity without `configuration attribute` (For example spnego principal and keytab)
        var isIdentityWithoutConfig = (!prop.configuration && identity.name === config.name.split('_')[0] && item === config.name.split('_')[1]);

        if (isIdentityWithConfig || isIdentityWithoutConfig) {
          prop[{keytab: 'file', principal: 'value'}[item]] = config.value;
          isConfigUpdated = true;
        }
      });
    }, this);
    return isConfigUpdated;
  },

  /**
   * Make request for cluster descriptor configs.
   *
   * @param {string[]|null} [services=null] services to be added
   * @returns {$.ajax}
   * @method loadClusterDescriptorConfigs
   */
  loadClusterDescriptorConfigs: function (services) {
    var servicesParam = services ? 'additional_services=' + services.join(',') : null,
        queryParams = ['evaluate_when=true'].concat(servicesParam).compact().join('&');

    return App.ajax.send({
      sender: this,
      name: 'admin.kerberize.cluster_descriptor',
      data: {
        queryParams: '?' + queryParams
      }
    });
  },

  loadClusterDescriptorStackConfigs: function () {
    return App.ajax.send({
      sender: this,
      name: 'admin.kerberize.cluster_descriptor.stack'
    });
  },

  mergeDescriptorStackWithConfigs: function (descriptorStack, descriptorConfigs) {
    var result = [];
    var stackConfigs = this.createServicesStackDescriptorConfigs(descriptorStack);
    var currentConfigs = this.createServicesStackDescriptorConfigs(descriptorConfigs);

    stackConfigs.forEach(function (stackConfig) {
      var currentConfig =  currentConfigs.filterProperty('name', stackConfig.get('name')).findProperty('filename', stackConfig.get('filename'));
      if (currentConfig) {
        currentConfigs = currentConfigs.without(currentConfig);
        result.push(currentConfig);
      } else {
        result.push(stackConfig);
      }
    });

    // add all properties from descriptor/COMPOSITE, that are absent in descriptor/STACK as custom properties
    currentConfigs.setEach('isUserProperty', true);
    result = result.concat(currentConfigs);

    return result;
  },

  /**
   * Prepare step configs using stack descriptor properties.
   *
   * @param {App.ServiceConfigProperty[]} configs
   * @param {App.ServiceConfigProperty[]} stackConfigs converted kerberos descriptor
   */
  setStepConfigs: function(configs, stackConfigs, showAdminProperties) {
    var configProperties = this.prepareConfigProperties(configs, showAdminProperties),
        stackConfigProperties = stackConfigs ? this.prepareConfigProperties(stackConfigs, showAdminProperties) : [],
        alterProperties = ['value','initialValue', 'defaultValue'];
    if (this.get('wizardController.name') === 'addServiceController') {
      // config properties for installed services should be disabled on Add Service Wizard
      configProperties.forEach(function(item) {
        if (this.get('installedServiceNames').contains(item.get('serviceName')) || item.get('serviceName') == 'Cluster') {
          item.set('isEditable', false);
        } else if (stackConfigs) {
          var stackConfigProperty = stackConfigProperties.filterProperty('filename', item.get('filename')).findProperty('name', item.get('name'));
          if (stackConfigProperty) {
            alterProperties.forEach(function (alterProperty) {
              item.set(alterProperty, stackConfigProperty.get(alterProperty));
            });
          }
        }
      }, this);
      // Concat properties that are present in the stack's kerberos  descriptor but not in the cluster kerberos descriptor
      stackConfigProperties.forEach(function(_stackConfigProperty){
        var isPropertyInClusterDescriptor = configProperties.filterProperty('filename', _stackConfigProperty.get('filename')).someProperty('name', _stackConfigProperty.get('name'));
        if (!isPropertyInClusterDescriptor) {
          if (this.get('installedServiceNames').contains(_stackConfigProperty.get('serviceName')) || _stackConfigProperty.get('serviceName') === 'Cluster') {
            _stackConfigProperty.set('isEditable', false);
          }
          configProperties.pushObject(_stackConfigProperty);
        }
      }, this);
    }
    configProperties = App.config.sortConfigs(configProperties);
    var stepConfigs = this.createServiceConfig(configProperties);
    this.get('stepConfigs').pushObjects(stepConfigs);
    return stepConfigs;
  },

  /**
   * Filter configs by installed services for Kerberos Wizard or by installed + selected services
   * for Add Service Wizard.
   * Set property value observer.
   * Set realm property with value from previous configuration step.
   * Set appropriate category for all configs.
   * Hide KDC related credentials properties if kerberos was manually enabled.
   *
   * @param {App.ServiceConfigProperty[]} configs
   * @returns {App.ServiceConfigProperty[]}
   */
  prepareConfigProperties: function(configs, showAdminProperties) {
    var self = this;
    // stored configs from previous steps (Configure Kerberos or Customize Services for ASW)
    var storedServiceConfigs = this.get('wizardController.content.serviceConfigProperties');
    var installedServiceNames = ['Cluster', 'AMBARI'].concat(App.Service.find().mapProperty('serviceName'));
    var configProperties = configs.slice(0);
    var siteProperties = App.configsCollection.getAll();
    var realmValue;
    // override stored values
    App.config.mergeStoredValue(configProperties, this.get('wizardController').loadCachedStepConfigValues(this));

    // show admin properties in add service wizard
    if (showAdminProperties) {
      installedServiceNames = installedServiceNames.concat(this.get('selectedServiceNames'));
    }
    configProperties = configProperties.filter(function(item) {
      return installedServiceNames.contains(item.get('serviceName'));
    });
    if (this.get('wizardController.name') !== 'addServiceController') {
      realmValue = storedServiceConfigs.findProperty('name', 'realm').value;
      configProperties.findProperty('name', 'realm').set('value', realmValue);
      configProperties.findProperty('name', 'realm').set('savedValue', realmValue);
      configProperties.findProperty('name', 'realm').set('recommendedValue', realmValue);
    }

    configProperties.setEach('isSecureConfig', false);
    configProperties.forEach(function(property, item, allConfigs) {
      if (['spnego_keytab', 'spnego_principal'].contains(property.get('name'))) {
        property.addObserver('value', self, 'spnegoPropertiesObserver');
      }
      if (property.get('observesValueFrom') && allConfigs.someProperty('name', property.get('observesValueFrom'))) {
        var observedValue = Em.get(allConfigs.findProperty('name', property.get('observesValueFrom')), 'value');
        property.set('value', observedValue);
        property.set('recommendedValue', observedValue);
        property.set('isVisible', true);
      }
      if (property.get('serviceName') === 'Cluster') {
        property.set('category', 'Global');
      }
      else {
        property.set('category', property.get('serviceName'));
      }
      // All user identity except storm should be grouped under "Ambari Principals" category
      if (property.get('identityType') == 'user') property.set('category', 'Ambari Principals');
      var siteProperty = siteProperties.findProperty('name', property.get('name'));
      if (siteProperty) {
        if (siteProperty.category === property.get('category')) {
          property.set('displayName',siteProperty.displayName);
          if (siteProperty.index) {
            property.set('index', siteProperty.index);
          }
        }
        if (siteProperty.displayType) {
          property.set('displayType', siteProperty.displayType);
        }
      }
      this.tweakConfigProperty(property);
    },this);

    return configProperties;
  },

  /**
   * Function to override kerberos descriptor's property values
   */
  tweakConfigProperty: function(config) {
    var defaultHiveMsPort = "9083",
        hiveMSHosts,
        port,
        hiveMSHostNames,
        configValue;
    if (config.name === 'templeton.hive.properties') {
      hiveMSHosts = App.HostComponent.find().filterProperty('componentName', 'HIVE_METASTORE');
      if (hiveMSHosts.length > 1) {
        hiveMSHostNames = hiveMSHosts.mapProperty('hostName');
        port = config.value.match(/:[0-9]{2,4}/);
        port = port ? port[0].slice(1) : defaultHiveMsPort;
        for (var i = 0; i < hiveMSHostNames.length; i++) {
          hiveMSHostNames[i] = "thrift://" + hiveMSHostNames[i] + ":" + port;
        }
        configValue = config.value.replace(/thrift.+[0-9]{2,},/i, hiveMSHostNames.join('\\,') + ",");
        config.set('value', configValue);
        config.set('recommendedValue', configValue);
      }
    }
  },

  /**
   * Sync up values between inherited property and its reference.
   *
   * @param {App.ServiceConfigProperty} configProperty
   */
  spnegoPropertiesObserver: function(configProperty) {
    var stepConfig = this.get('stepConfigs').findProperty('name', 'KERBEROS') || this.get('stepConfigs').findProperty('name', 'ADVANCED');

    stepConfig.get('configs').forEach(function(config) {
      if (config.get('observesValueFrom') === configProperty.get('name')) {
        Em.run.once(this, function() {
          config.set('value', configProperty.get('value'));
          config.set('recommendedValue', configProperty.get('value'));
        });
      }
    }, this);
  },
    /**
   * Prepare all necessary data for recommendations payload.
   *
   * #mutates initialConfigValues
   * @returns {$.Deferred.promise()}
   */
  bootstrapRecommendationPayload: function(kerberosDescriptor) {
    var dfd = $.Deferred();
    var self = this;

    this.getServicesConfigurations().then(function(configurations) {
      var recommendations = self.getBlueprintPayloadObject(configurations, kerberosDescriptor);
      self.set('servicesConfigurations', configurations);
      self.set('initialConfigValues', recommendations.blueprint.configurations);
      dfd.resolve(recommendations);
    });
    return dfd.promise();
  },

  getServicesConfigurations: function() {
    var dfd = $.Deferred();
    var self = this;
    this.getConfigTags().then(function() {
      App.router.get('configurationController').getConfigsByTags(self.get('serviceConfigTags')).done(function (configurations) {
        dfd.resolve(configurations);
      });
    });

    return dfd.promise();
  },

  /**
   * Returns payload for recommendations request.
   * Takes services' configurations and merge them with kerberos descriptor properties.
   *
   * @param {object[]} configurations services' configurations fetched from API
   * @param {App.ServiceConfigProperty[]} kerberosDescriptor descriptor configs
   * @returns {object} payload for recommendations request
   */
  getBlueprintPayloadObject: function(configurations, kerberosDescriptor) {
    var recommendations = blueprintUtils.generateHostGroups(App.get('allHostNames'));
    var mergedConfigurations = this.mergeDescriptorToConfigurations(configurations, this.createServicesStackDescriptorConfigs(kerberosDescriptor));
    recommendations.blueprint.configurations = mergedConfigurations.reduce(function(p, c) {
      p[c.type] = {};
      p[c.type].properties = c.properties;
      return p;
    }, {});

    if (this.get('isWithinAddService')) {
      this.get('content.masterComponentHosts').filterProperty('isInstalled', false).forEach(function(item) {
        var hostGroupName = blueprintUtils.getHostGroupByFqdn(recommendations, item.hostName);
        blueprintUtils.addComponentToHostGroup(recommendations, item.component, hostGroupName);
      }, this);
    }

    return recommendations;
  },

  /**
   * Returns map with appropriate action and properties to process with.
   * Key is an action e.g. `add`, `update`, `delete` and value is  an object `fileName` -> `propertyName`: `propertyValue`.
   *
   * @param {object} recommendedConfigurations
   * @param {object[]} servicesConfigurations services' configurations fetched from API
   * @param {App.ServiceConfigProperty[]} allConfigs all current configurations stored in controller, basically kerberos descriptor
   * @returns {object}
   */
  groupRecommendationProperties: function(recommendedConfigurations, servicesConfigurations, allConfigs) {
    var resultMap = {
      update: {},
      add: {},
      delete: {}
    };

    /**
     * Adds property to associated group `add`,`delete`,`update`.
     *
     * @param {object} propertyMap <code>resultMap</code> object
     * @param {string} name property name
     * @param {string} propertyValue property value
     * @param {string} fileName property file name
     * @return {object} <code>resultMap</code>
     * @param {string} group, `add`,`delete`,`update`
     */
    var addProperty = function(propertyMap, name, propertyValue, fileName, group) {
      var ret = $.extend(true, {}, propertyMap);
      if (ret.hasOwnProperty(group)) {
        if (!ret[group].hasOwnProperty(fileName)) {
          ret[group][fileName] = {};
        }
        ret[group][fileName][name] = propertyValue;
      }
      return ret;
    };

    return Em.keys(recommendedConfigurations || {}).reduce(function(acc, fileName) {
      var propertyMap = acc;
      var recommendedProperties = Em.getWithDefault(recommendedConfigurations, fileName + '.properties', {});
      var recommendedAttributes = Em.getWithDefault(recommendedConfigurations, fileName + '.property_attributes', {});
      // check for properties that should be delted
      Em.keys(recommendedAttributes).forEach(function(propertyName) {
        var attribute = recommendedAttributes[propertyName];
        // delete properties which are present in kerberos descriptor
        if (attribute.hasOwnProperty('delete') && allConfigs.filterProperty('filename', fileName).someProperty('name', propertyName)) {
          propertyMap = addProperty(propertyMap, propertyName, '', fileName, 'delete');
        }
      });

      return Em.keys(recommendedProperties).reduce(function(a, propertyName) {
        var propertyValue = recommendedProperties[propertyName];
        // check if property exist in saved configurations on server
        var isExist = Em.getWithDefault(servicesConfigurations.findProperty('type', fileName) || {}, 'properties', {}).hasOwnProperty(propertyName);
        if (!isExist) {
          return addProperty(a, propertyName, propertyValue, fileName, 'add');
        }
        // when property exist check that it present in current step configs (kerberos descriptor)
        // and add it as property to `update`
        if (allConfigs.filterProperty('filename', fileName).someProperty('name', propertyName)) {
          return addProperty(a, propertyName, propertyValue, fileName, 'update');
        }
        return a;
      }, propertyMap);
    }, resultMap);
  },

  /**
   *
   * @method getServiceByFilename
   * @param {string}fileName
   * @returns {string}
   */
  getServiceByFilename: function(fileName) {
    // core-site properties goes to HDFS
    if (fileName === 'core-site' && App.Service.find().someProperty('serviceName', 'HDFS')) {
      return 'HDFS';
    }
    var associatedService = App.StackService.find().filter(function(service) {
      return Em.keys(service.get('configTypes')).contains(fileName);
    })[0];
    return associatedService ? associatedService.get('serviceName') : '';
  },

  loadServerSideConfigsRecommendations: function(recommendations) {
    return App.ajax.send({
      'name': 'config.recommendations',
      'sender': this,
      'data': {
        stackVersionUrl: App.get('stackVersionURL'),
        dataToSend: {
          recommend: 'configurations',
          hosts: App.get('allHostNames'),
          services: this.get('serviceNames'),
          recommendations: recommendations
        }
      },
      'success': 'loadRecommendationsSuccess',
      'error': 'loadRecommendationsError'
    });
  },

  loadRecommendationsError: function(req, ajaxOpts, error, opt) {
    var resp;
    try {
      resp = $.parseJSON(req.responseText);
    } catch (e) { }
    return App.ModalPopup.show({
      header: Em.I18n.t('common.error'),
      secondary: false,
      bodyClass: App.AjaxDefaultErrorPopupBodyView.extend({
        type: opt.type || 'GET',
        url: opt.url,
        status: req.status,
        message: resp && resp.message || req.responseText
      })
    });
  },

  /**
   * Callback executed when all configs specified by tags are loaded.
   * Here we handle configurations for instlled services and Kerberos.
   * Gather needed info for recommendation request such as configurations object.
   *
   * @override
   */
  getConfigTagsSuccess: function(data) {
    // here we get all installed services including KERBEROS
    var serviceNames = App.Service.find().mapProperty('serviceName').concat(['KERBEROS']).uniq();
    // collect all config types for selected services
    var installedServiceSites = Array.prototype.concat.apply([], App.config.get('preDefinedServiceConfigs').filter(function(serviceConfig) {
      return serviceNames.contains(Em.get(serviceConfig, 'serviceName'));
    }).map(function (service) {
      // when service have no configs return <code>null</code> instead return config types
      if (!service.get('configTypes')) return null;
      return Object.keys(service.get('configTypes'));
    }, this).compact()).uniq(); // cleanup <code>null</code>

    // take all configs for selected services by config types recieved from API response
    var serviceConfigTags = Em.keys(data.Clusters.desired_configs).reduce(function(tags, site) {
      if (data.Clusters.desired_configs.hasOwnProperty(site)) {
        // push cluster-env.xml also since it not associated with any service but need to further processing
        if (installedServiceSites.contains(site) || site === 'cluster-env') {
          tags.push({
            siteName: site,
            tagName: data.Clusters.desired_configs[site].tag,
            newTagName: null
          });
        }
      }
      return tags;
    }, []);
    // store configurations
    this.set('serviceConfigTags', serviceConfigTags);
    this.set('isAppliedConfigLoaded', true);
  },

  /**
   * Add/update property in `properties` object for each config type with
   * associated kerberos descriptor config value.
   *
   * @private
   * @param {object[]} configurations
   * @param {App.ServiceConfigProperty[]} kerberosDescriptor
   * @returns {object[]}
   */
  mergeDescriptorToConfigurations: function(configurations, kerberosDescriptor) {
    return configurations.map(function(configType) {
      var properties = $.extend({}, configType.properties);
      var filteredDescriptor = kerberosDescriptor.filterProperty('filename', configType.type);
      if (filteredDescriptor.length) {
        filteredDescriptor.forEach(function(descriptorConfig) {
          var configName = Em.get(descriptorConfig, 'name');
          properties[configName] = Em.get(descriptorConfig, 'value');
        });
      }
      return {
        type: configType.type,
        version: configType.version,
        tag: configType.tag,
        properties: properties
      };
    });
  },

  postKerberosDescriptor: function (kerberosDescriptor) {
    return App.ajax.send({
      name: 'admin.kerberos.cluster.artifact.create',
      sender: this,
      data: {
        artifactName: 'kerberos_descriptor',
        data: {
          artifact_data: this.removeIdentityReferences(kerberosDescriptor)
        }
      }
    });
  },

  /**
   * Send request to update kerberos descriptor
   * @param kerberosDescriptor
   * @returns {$.ajax|*}
   */
  putKerberosDescriptor: function (kerberosDescriptor) {
    return App.ajax.send({
      name: 'admin.kerberos.cluster.artifact.update',
      sender: this,
      data: {
        artifactName: 'kerberos_descriptor',
        data: {
          artifact_data: this.removeIdentityReferences(kerberosDescriptor)
        }
      },
      success: 'unkerberizeCluster',
      error: 'unkerberizeCluster'
    });
  },

  /**
   * The UI should ignore Kerberos identity references
   * when setting the user-supplied Kerberos descriptor
   * @param {object} kerberosDescriptor
   * @returns {object}
   */
  removeIdentityReferences: function(kerberosDescriptor) {
    const notReference = (identity) => (Em.isNone(identity.reference) && !identity.name.startsWith('/'));
    kerberosDescriptor.services.forEach((service) => {
      if (service.identities) {
        service.identities = service.identities.filter(notReference);
      }
      if (service.components) {
        service.components.forEach((component) => {
          if (component.identities) {
            component.identities = component.identities.filter(notReference);
          }
        });
      }
    });
    return kerberosDescriptor;
  }

});
