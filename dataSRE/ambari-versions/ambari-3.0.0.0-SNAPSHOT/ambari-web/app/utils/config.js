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
require('utils/configs_collection');
var stringUtils = require('utils/string_utils');
var validator = require('utils/validator');

var configTagFromFileNameMap = {};
var PASSWORD = "PASSWORD";

App.config = Em.Object.create({

  CONFIG_GROUP_NAME_MAX_LENGTH: 18,

  /**
   * filename exceptions used to support substandard sitenames which don't have "xml" extension
   * @type {string[]}
   */
  filenameExceptions: ['alert_notification'],

  preDefinedServiceConfigs: [],

  /**
   * Map for methods used to parse hosts lists from certain config properties
   */
  uniqueHostsListParsers: [
    {
      propertyName: 'templeton.hive.properties',
      type: 'webhcat-site',
      method: 'getTempletonHiveHosts'
    }
  ],

  /**
   *
   * Returns file name version that stored on server.
   *
   * Example:
   *   App.config.getOriginalFileName('core-site') // returns core-site.xml
   *   App.config.getOriginalFileName('zoo.cfg') // returns zoo.cfg
   *
   * @param {String} fileName
   * @method getOriginalFileName
   **/
  getOriginalFileName: function (fileName) {
    if (/\.xml$/.test(fileName)) return fileName;
    return this.get('filenameExceptions').contains(fileName) ? fileName : fileName + '.xml';
  },

  /**
   * truncate Config Group name to <CONFIG_GROUP_NAME_MAX_LENGTH> length and paste "..." in the middle
   */
  truncateGroupName: function (name) {
    if (name && name.length > App.config.CONFIG_GROUP_NAME_MAX_LENGTH) {
      var middle = Math.floor(App.config.CONFIG_GROUP_NAME_MAX_LENGTH / 2);
      name = name.substring(0, middle) + "..." + name.substring(name.length - middle);
    }
    return name;
  },

  /**
   * Check if Hive installation with new MySQL database created via Ambari is allowed
   * @param osFamily
   * @returns {boolean}
   */
  isManagedMySQLForHiveAllowed: function (osFamily) {
    var osList = ['redhat5', 'suse11'];
    return !osList.contains(osFamily);
  },

  /**
   *
   * Returns the configuration tagName from supplied filename
   *
   * Example:
   *   App.config.getConfigTagFromFileName('core-site.xml') // returns core-site
   *   App.config.getConfigTagFromFileName('zoo.cfg') // returns zoo.cfg
   *
   * @param {String} fileName
   * @method getConfigTagFromFileName
   **/
  getConfigTagFromFileName: function (fileName) {
    if (configTagFromFileNameMap[fileName]) {
      return configTagFromFileNameMap[fileName];
    }
    var ret = fileName.endsWith('.xml') ? fileName.slice(0, -4) : fileName;
    configTagFromFileNameMap[fileName] = ret;
    return ret;
  },

  /**
   *
   * @param name
   * @param fileName
   * @returns {string}
   */
  configId: function(name, fileName) {
    return name + "__" + App.config.getConfigTagFromFileName(fileName);
  },

  setPreDefinedServiceConfigs: function (isMiscTabToBeAdded) {
    var stackServices = App.StackService.find().filterProperty('id');
    // Only include services that has configTypes related to them for service configuration page
    var servicesWithConfigTypes = stackServices.filter(function (service) {
      var configtypes = service.get('configTypes');
      return configtypes && !!Object.keys(configtypes).length;
    }, this);

    var allTabs;
    if (isMiscTabToBeAdded) {
      var nonServiceTab = require('data/service_configs');
      var miscService = nonServiceTab.findProperty('serviceName', 'MISC');
      var tagTypes = {};
      servicesWithConfigTypes.mapProperty('configTypes').forEach(function (configTypes) {
        for (var fileName in configTypes) {
          if (fileName.endsWith('-env') && !miscService.get('configTypes')[fileName]) {
            tagTypes[fileName] = configTypes[fileName];
          }
        }
      });
      miscService.set('configTypes', $.extend(miscService.get('configTypes'), tagTypes));
      allTabs = servicesWithConfigTypes.concat(nonServiceTab);
    } else {
      allTabs = servicesWithConfigTypes;
    }
    this.set('preDefinedServiceConfigs', allTabs);
  },

  secureConfigs: require('data/configs/wizards/secure_mapping'),

  secureConfigsMap: function () {
    var ret = {};
    this.get('secureConfigs').forEach(function (sc) {
      ret[sc.name] = true;
    });
    return ret;
  }.property('secureConfigs.[]'),

  kerberosIdentities: require('data/configs/wizards/kerberos_identities').configProperties,

  kerberosIdentitiesMap: function() {
    var map = {};

    this.get('kerberosIdentities').forEach(function (c) {
      map[this.configId(c.name, c.filename)] = c;
    }, this);
    return map;
  }.property('kerberosIdentities'),

  customStackMapping: require('data/custom_stack_map'),

  mapCustomStack: function () {
    var
      baseStackFolder = App.get('currentStackName'),
      singMap = {
        "1": [">", ">="],
        "-1": ["<", "<="],
        "0": ["=", ">=","<="]
      };

    this.get('customStackMapping').every(function (stack) {
      if(stack.stackName == App.get('currentStackName')){
        var versionCompare = Em.compare(App.get('currentStackVersionNumber'), stack.stackVersionNumber);
        if(singMap[versionCompare+""].contains(stack.sign)){
          baseStackFolder = stack.baseStackFolder;
          return false;
        }
      }
      return true;
    });

    return baseStackFolder;
  },

  allPreDefinedSiteProperties: require('data/configs/site_properties').configProperties,

  preDefinedSiteProperties: function () {
    var serviceNames = App.StackService.find().mapProperty('serviceName').concat('MISC');
    return this.get('allPreDefinedSiteProperties').filter(function(p) {
      return serviceNames.contains(p.serviceName);
    });
  }.property().volatile(),

  /**
   * map of <code>preDefinedSiteProperties</code> provide search by index
   * @type {object}
   */
  preDefinedSitePropertiesMap: function () {
    var map = {};

    this.get('preDefinedSiteProperties').forEach(function (c) {
      map[this.configId(c.name, c.filename)] = c;
    }, this);
    return map;
  }.property('preDefinedSiteProperties'),

  serviceByConfigTypeMap: function () {
    var ret = {};
    App.StackService.find().forEach(function(s) {
      s.get('configTypeList').forEach(function (ct) {
        ret[ct] = s;
      });
    });
    return ret;
  }.property(),

  /**
   * Generate configs collection with Ember or plain config objects
   * from config JSON
   *
   * @param configJSON
   * @param useEmberObject
   * @returns {Array}
   */
  getConfigsFromJSON: function(configJSON, useEmberObject) {
    var configs = [],
      filename = App.config.getOriginalFileName(configJSON.type),
      properties = configJSON.properties,
      attributes = [];
      ['FINAL', 'PASSWORD', 'USER', 'GROUP', 'TEXT', 'ADDITIONAL_USER_PROPERTY', 'NOT_MANAGED_HDFS_PATH', 'VALUE_FROM_PROPERTY_FILE'].forEach(function (attribute){
        var json = {};
        json[attribute] = Em.get(configJSON, 'properties_attributes.' + attribute.toLowerCase()) || {};
        attributes.push(json);
      });

    for (var index in properties) {
      var serviceConfigObj = this.getDefaultConfig(index, filename);

      if (serviceConfigObj.isRequiredByAgent !== false) {
        serviceConfigObj.value = serviceConfigObj.savedValue = this.formatPropertyValue(serviceConfigObj, properties[index]);
        serviceConfigObj.isFinal = serviceConfigObj.savedIsFinal = attributes[0]['FINAL'][index] === "true";

        var propertyType = [];
        // iterate through all the attributes, except for FINAL
        for (var i=1; i<attributes.length; i++) {
          for (var type in attributes[i]) {
            if (attributes[i][type][index] === "true") {
              propertyType.push(type);

              if (type === PASSWORD) {
                serviceConfigObj.displayType = "password";
              }
            }
          }
        }
        serviceConfigObj.propertyType = propertyType;
        serviceConfigObj.isEditable = serviceConfigObj.isReconfigurable;
      }

      if (useEmberObject) {
        configs.push(App.ServiceConfigProperty.create(serviceConfigObj));
      } else {
        configs.push(serviceConfigObj);
      }
    }
    return configs;
  },

  /**
   * Get config from configsCollections or
   * generate new default config in collection does not contain
   * such config
   *
   * @param name
   * @param fileName
   * @param coreObject
   * @returns {*|Object}
   */
  getDefaultConfig: function(name, fileName, coreObject) {
    name = JSON.parse('"' + name + '"');
    var cfg = App.configsCollection.getConfigByName(name, fileName) ||
      App.config.createDefaultConfig(name, fileName, false);
    if (Em.typeOf(coreObject) === 'object') {
      Em.setProperties(cfg, coreObject);
    }
    return cfg;
  },

  /**
   * This method sets default values for config property
   * These property values has the lowest priority and can be overridden be stack/UI
   * config property but is used when such properties are absent in stack/UI configs
   * @param {string} name
   * @param {string} fileName
   * @param {boolean} definedInStack
   * @param {Object} [coreObject]
   * @returns {Object}
   */
  createDefaultConfig: function(name, fileName, definedInStack, coreObject) {
    var service = this.get('serviceByConfigTypeMap')[App.config.getConfigTagFromFileName(fileName)];
    var serviceName = service ? service.get('serviceName') : 'MISC';
    var tpl = {
      /** core properties **/
      id: this.configId(name, fileName),
      name: name,
      filename: this.getOriginalFileName(fileName),
      value: '',
      savedValue: null,
      isFinal: false,
      savedIsFinal: null,
      /** UI and Stack properties **/
      recommendedValue: null,
      recommendedIsFinal: null,
      supportsFinal: this.shouldSupportFinal(serviceName, fileName),
      supportsAddingForbidden: this.shouldSupportAddingForbidden(serviceName, fileName),
      serviceName: serviceName,
      displayName: name,
      displayType: (coreObject && coreObject.propertyType && coreObject.propertyType.contains(PASSWORD)) ? 'password' : this.getDefaultDisplayType(coreObject ? coreObject.value : ''),
      description: '',
      category: this.getDefaultCategory(definedInStack, fileName),
      isSecureConfig: this.getIsSecure(name),
      showLabel: true,
      isVisible: true,
      isUserProperty: !definedInStack,
      isRequired: definedInStack,
      group: null,
      isRequiredByAgent:  true,
      isReconfigurable: true,
      unit: null,
      hasInitialValue: false,
      isOverridable: true,
      index: Infinity,
      dependentConfigPattern: null,
      options: null,
      radioName: null,
      widgetType: null,
      errorMessage: '',
      warnMessage: ''
    };
    return Object.keys(coreObject|| {}).length ?
      $.extend(tpl, coreObject) : tpl;
  },

  /**
   * This method creates host name properties
   * @param serviceName
   * @param componentName
   * @param value
   * @param stackComponent
   * @returns Object
   */
  createHostNameProperty: function(serviceName, componentName, value, stackComponent) {
    var hostOrHosts = stackComponent.get('isMultipleAllowed') ? 'hosts' : 'host';
    var name = componentName.toLowerCase() + '_' + hostOrHosts;
    var filename = serviceName.toLowerCase() + "-site.xml";
    return {
      "id": App.config.configId(name, filename),
      "name": name,
      "displayName":  stackComponent.get('displayName') + ' ' + (value.length > 1 ? 'hosts' : 'host'),
      "value": value,
      "recommendedValue": value,
      "description": "The " + hostOrHosts + " that has been assigned to run " + stackComponent.get('displayName'),
      "displayType": "component" + hostOrHosts.capitalize(),
      "isOverridable": false,
      "isRequiredByAgent": false,
      "serviceName": serviceName,
      "filename": filename,
      "category": componentName,
      "index": 0
    }
  },

  /**
   * Get component name from config name string
   *
   * @param configName
   * @returns {string}
   */
  getComponentName: function(configName) {
    var match = configName.match(/^(.*)_host[s]?$/) || [],
      component = match[1];
    return component ? component.toUpperCase() : "";
  },

  /**
   * This method merge properties form <code>stackConfigProperty<code> which are taken from stack
   * with <code>UIConfigProperty<code> which are hardcoded on UI
   * @param coreObject
   * @param stackProperty
   * @param preDefined
   * @param [propertiesToSkip]
   */
  mergeStaticProperties: function(coreObject, stackProperty, preDefined, propertiesToSkip) {
    propertiesToSkip = propertiesToSkip || ['name', 'filename', 'value', 'savedValue', 'isFinal', 'savedIsFinal'];
    for (var k in coreObject) {
      if (coreObject.hasOwnProperty(k)) {
        if (!propertiesToSkip.contains(k)) {
          coreObject[k] = this.getPropertyIfExists(k, coreObject[k], stackProperty, preDefined);
        }
      }
    }
    return coreObject;
  },

  /**
   * This method using for merging some properties from two objects
   * if property exists in <code>firstPriority<code> result will be it's property
   * else if property exists in <code>secondPriority<code> result will be it's property
   * otherwise <code>defaultValue<code> will be returned
   * @param {String} propertyName
   * @param {*} defaultValue=null
   * @param {Em.Object|Object} firstPriority
   * @param {Em.Object|Object} [secondPriority=null]
   * @returns {*}
   */
  getPropertyIfExists: function(propertyName, defaultValue, firstPriority, secondPriority) {
    firstPriority = firstPriority || {};
    secondPriority = secondPriority || {};
    var fp = Em.get(firstPriority, propertyName);
    if (firstPriority && !Em.isNone(fp)) {
      return fp;
    }
    else {
      var sp = Em.get(secondPriority, propertyName);
      if (secondPriority && !Em.isNone(sp)) {
        return sp;
      } else {
        return defaultValue;
      }
    }
  },

  /**
   * Get displayType for properties that has not defined value
   * @param value
   * @returns {string}
   */
  getDefaultDisplayType: function(value) {
    return value && !stringUtils.isSingleLine(value) ? 'multiLine' : 'string';
  },

  /**
   * Get category for properties that has not defined value
   * @param stackConfigProperty
   * @param fileName
   * @returns {string}
   */
  getDefaultCategory: function(stackConfigProperty, fileName) {
    return (stackConfigProperty ? 'Advanced ' : 'Custom ') + this.getConfigTagFromFileName(fileName);
  },

  /**
   * Get isSecureConfig for properties that has not defined value
   * @param propertyName
   * @returns {boolean}
   */
  getIsSecure: function(propertyName) {
    return !!this.get('secureConfigsMap')[propertyName];
  },

  /**
   * Returns description, formatted if needed
   *
   * @param {String} description
   * @param {String} displayType
   * @returns {String}
   */
  getDescription: function(description, displayType) {
    var additionalDescription = Em.I18n.t('services.service.config.password.additionalDescription');
    if ('password' === displayType) {
      if (description && !description.contains(additionalDescription)) {
        return description + '\n' + additionalDescription;
      } else {
        return additionalDescription;
      }
    }
    return description
  },

  /**
   * parse Kerberos descriptor
   *
   * @param kerberosDescriptor
   * @returns {{}}
   */
  parseDescriptor: function(kerberosDescriptor) {
    var identitiesMap = {};
    Em.get(kerberosDescriptor, 'KerberosDescriptor.kerberos_descriptor.services').forEach(function (service) {
      this.parseIdentities(service, identitiesMap);
      if (Array.isArray(service.components)) {
        service.components.forEach(function (component) {
          this.parseIdentities(component, identitiesMap);
        }, this);
      }
    }, this);
    return identitiesMap;
  },

  /**
   * Looking for configs identities and add them to <code>identitiesMap<code>
   *
   * @param item
   * @param identitiesMap
   */
  parseIdentities: function (item, identitiesMap) {
    if (item.identities) {
      item.identities.forEach(function (identity) {

        Em.keys(identity).without('name').forEach(function (item) {
          if (identity[item].configuration) {
            var cfg = identity[item].configuration.split('/'), name = cfg[1], fileName = cfg[0];
            identitiesMap[App.config.configId(name, fileName)] = true;
          }
        });
      });
    }
    return identitiesMap;
  },

  /**
   * Update description for disabled kerberos configs which are identities
   *
   * @param description
   * @returns {*}
   */
  kerberosIdentitiesDescription: function(description) {
    if (!description) return Em.I18n.t('services.service.config.secure.additionalDescription');
    description = description.trim();
    return (description.endsWith('.') ? description + ' ' : description + '. ') +
      Em.I18n.t('services.service.config.secure.additionalDescription');
  },

  /**
   * Get view class based on display type of config
   *
   * @param displayType
   * @param dependentConfigPattern
   * @param unit
   * @returns {*}
   */
  getViewClass: function (displayType, dependentConfigPattern, unit) {
    switch (displayType) {
      case 'user':
      case 'group':
        return App.ServiceConfigTextFieldUserGroupWithID;
      case 'checkbox':
      case 'boolean':
        return dependentConfigPattern ? App.ServiceConfigCheckboxWithDependencies : App.ServiceConfigCheckbox;
      case 'boolean-inverted':
        return App.ServiceConfigCheckbox;
      case 'password':
        return App.ServiceConfigPasswordField;
      case 'combobox':
        return App.ServiceConfigComboBox;
      case 'radio button':
        return App.ServiceConfigRadioButtons;
      case 'directories':
        return App.ServiceConfigTextArea;
      case 'directory':
        return App.ServiceConfigTextField;
      case 'content':
        return App.ServiceConfigTextAreaContent;
      case 'multiLine':
        return App.ServiceConfigTextArea;
      case 'custom':
        return App.ServiceConfigBigTextArea;
      case 'componentHost':
        return App.ServiceConfigMasterHostView;
      case 'label':
        return App.ServiceConfigLabelView;
      case 'componentHosts':
        return App.ServiceConfigComponentHostsView;
      case 'supportTextConnection':
        return App.checkConnectionView;
      case 'capacityScheduler':
        return App.CapacitySceduler;
      default:
        return unit ? App.ServiceConfigTextFieldWithUnit : App.ServiceConfigTextField;
    }
  },

  /**
   * Returns error validator function based on config type
   *
   * @param displayType
   * @returns {Function}
   */
  getErrorValidator: function (displayType) {
    switch (displayType) {
      case 'checkbox':
      case 'custom':
        return function () {
          return ''
        };
      case 'int':
        return function (value) {
          return !validator.isValidInt(value) && !validator.isConfigValueLink(value)
            ? Em.I18n.t('errorMessage.config.number.integer') : '';
        };
      case 'float':
        return function (value) {
          return !validator.isValidFloat(value) && !validator.isConfigValueLink(value)
            ? Em.I18n.t('errorMessage.config.number.float') : '';
        };
      case 'directories':
      case 'directory':
        return function (value, name) {
          if (App.config.isDirHeterogeneous(name)) {
            if (!validator.isValidDataNodeDir(value)) return Em.I18n.t('errorMessage.config.directory.heterogeneous');
          } else {
            if (!validator.isValidDir(value)) return Em.I18n.t('errorMessage.config.directory.default');
          }
          if (!validator.isAllowedDir(value)) {
            return Em.I18n.t('errorMessage.config.directory.allowed');
          }
          return validator.isNotTrimmedRight(value) ? Em.I18n.t('errorMessage.config.spaces.trailing') : '';
        };
      case 'email':
        return function (value) {
          return !validator.isValidEmail(value) ? Em.I18n.t('errorMessage.config.mail') : '';
        };
      case 'supportTextConnection':
      case 'host':
        return function (value) {
          return validator.isNotTrimmed(value) ? Em.I18n.t('errorMessage.config.spaces.trim') : '';
        };
      case 'password':
        return function (value, name, retypedPassword) {
          if (name === 'ranger_admin_password') {
            if (String(value).length < 9) {
              return Em.I18n.t('errorMessage.config.password.length').format(9);
            }
          }
          return value !== retypedPassword ? Em.I18n.t('errorMessage.config.password') : '';
        };
      case 'user':
      case 'database':
      case 'db_user':
        return function (value) {
          return !validator.isValidDbName(value) ? Em.I18n.t('errorMessage.config.user') : '';
        };
      case 'ldap_url':
        return function (value) {
          return !validator.isValidLdapsURL(value) ? Em.I18n.t('errorMessage.config.ldapUrl') : '';
        };
      default:
        return function (value, name) {
          if (['javax.jdo.option.ConnectionURL', 'oozie.service.JPAService.jdbc.url'].contains(name)
            && !validator.isConfigValueLink(value) && validator.isConfigValueLink(value)) {
            return Em.I18n.t('errorMessage.config.spaces.trim');
          }
          return validator.isNotTrimmedRight(value) ? Em.I18n.t('errorMessage.config.spaces.trailing') : '';
        };
    }
  },

  /**
   * Returns warning validator function based on config type
   *
   * @param displayType
   * @returns {Function}
   */
  getWarningValidator: function(displayType) {
    switch (displayType) {
      case 'int':
      case 'float':
        return function (value, name, filename, stackConfigProperty, unitLabel) {
          stackConfigProperty = stackConfigProperty || App.configsCollection.getConfigByName(name, filename);
          var maximum = Em.get(stackConfigProperty || {}, 'valueAttributes.maximum'),
            minimum = Em.get(stackConfigProperty || {}, 'valueAttributes.minimum'),
            min = validator.isValidFloat(minimum) ? parseFloat(minimum) : NaN,
            max = validator.isValidFloat(maximum) ? parseFloat(maximum) : NaN,
            val = validator.isValidFloat(value) ? parseFloat(value) : NaN;

          if (!isNaN(val) && !isNaN(max) && val > max) {
            return Em.I18n.t('config.warnMessage.outOfBoundaries.greater').format(max + unitLabel);
          }
          if (!isNaN(val) && !isNaN(min) && val < min) {
            return Em.I18n.t('config.warnMessage.outOfBoundaries.less').format(min + unitLabel);
          }
          return '';
        };
      default:
        return function () { return ''; }
    }
  },

  /**
   * Defines if config support heterogeneous devices
   *
   * @param {string} name
   * @returns {boolean}
   */
  isDirHeterogeneous: function(name) {
    return ['dfs.datanode.data.dir'].contains(name);
  },

  /**
   * format property value depending on displayType
   * and one exception for 'kdc_type'
   * @param serviceConfigProperty
   * @param [originalValue]
   * @returns {*}
   */
  formatPropertyValue: function(serviceConfigProperty, originalValue) {
    var value = Em.isNone(originalValue) ? Em.get(serviceConfigProperty, 'value') : originalValue,
        displayType = Em.get(serviceConfigProperty, 'displayType') || Em.get(serviceConfigProperty, 'valueAttributes.type');
    if (Em.get(serviceConfigProperty, 'name') === 'kdc_type') {
      return App.router.get('mainAdminKerberosController.kdcTypesValues')[value];
    }
    if ( /^\s+$/.test("" + value)) {
      return " ";
    }
    switch (displayType) {
      case 'int':
        if (/\d+m$/.test(value) ) {
          return value.slice(0, value.length - 1);
        } else {
          var int = parseInt(value);
          return isNaN(int) ? "" : int.toString();
        }
      case 'float':
        var float = parseFloat(value);
        return isNaN(float) ? "" : float.toString();
      case 'componentHosts':
        if (typeof(value) == 'string') {
          return value.replace(/\[|]|'|&apos;/g, "").split(',');
        }
        return value;
      case 'content':
      case 'string':
      case 'multiLine':
      case 'directories':
      case 'directory':
        return this.trimProperty({ displayType: displayType, value: value });
      default:
        return value;
    }

  },

  /**
   * Format float value
   *
   * @param {*} value
   * @returns {string|*}
   */
  formatValue: function(value) {
    return validator.isValidFloat(value) ? parseFloat(value).toString() : value;
  },

  /**
   * Get step config by file name
   *
   * @param stepConfigs
   * @param fileName
   * @returns {Object|null}
   */
  getStepConfigForProperty: function (stepConfigs, fileName) {
    return stepConfigs.find(function (s) {
      return s.get('configTypes').contains(App.config.getConfigTagFromFileName(fileName));
    });
  },

  /**
   *
   * @param configs
   * @returns {Object[]}
   */
  sortConfigs: function(configs) {
    return configs.sort(function(a, b) {
      if (Em.get(a, 'index') > Em.get(b, 'index')) return 1;
      if (Em.get(a, 'index') < Em.get(b, 'index')) return -1;
      if (Em.get(a, 'name') > Em.get(b, 'name')) return 1;
      if (Em.get(a, 'name') < Em.get(b, 'name')) return -1;
      return 0;
    });
  },

  /**
   * create new ServiceConfig object by service name
   * @param {string} serviceName
   * @param {App.ServiceConfigGroup[]} [configGroups]
   * @param {App.ServiceConfigProperty[]} [configs]
   * @param {Number} [initConfigsLength]
   * @return {App.ServiceConfig}
   * @method createServiceConfig
   */
  createServiceConfig: function (serviceName, configGroups, configs, initConfigsLength) {
    var preDefinedServiceConfig = App.config.get('preDefinedServiceConfigs').findProperty('serviceName', serviceName);
    return App.ServiceConfig.create({
      serviceName: preDefinedServiceConfig.get('serviceName'),
      displayName: preDefinedServiceConfig.get('displayName'),
      configCategories: preDefinedServiceConfig.get('configCategories'),
      configs: configs || [],
      configGroups: configGroups || [],
      initConfigsLength: initConfigsLength || 0
    });
  },

  /**
   * GETs all cluster level sites in one call.
   *
   * @return {$.ajax}
   */
  loadConfigsByTags: function (tags) {
    var urlParams = [];
    tags.forEach(function (_tag) {
      urlParams.push('(type=' + _tag.siteName + '&tag=' + _tag.tagName + ')');
    });
    var params = urlParams.join('|');
    return App.ajax.send({
      name: 'config.on_site',
      sender: this,
      data: {
        params: params
      }
    });
  },

  configTypesInfoMap: {},

  /**
   * Get config types and config type attributes from stack service
   *
   * @param service
   * @return {object}
   */
  getConfigTypesInfoFromService: function (service) {
    var configTypesInfoMap = this.get('configTypesInfoMap');
    if (configTypesInfoMap[service]) {
      // don't recalculate
      return configTypesInfoMap[service];
    }
    var configTypes = service.get('configTypes');
    var configTypesInfo = {
      items: [],
      supportsFinal: [],
      supportsAddingForbidden: []
    };
    if (configTypes) {
      for (var key in configTypes) {
        if (configTypes.hasOwnProperty(key)) {
          configTypesInfo.items.push(key);
          if (configTypes[key].supports && configTypes[key].supports.final === "true") {
            configTypesInfo.supportsFinal.push(key);
          }
          if (configTypes[key].supports && configTypes[key].supports.adding_forbidden === "true"){
            configTypesInfo.supportsAddingForbidden.push(key);
          }
        }
      }
    }
    configTypesInfoMap[service] = configTypesInfo;
    this.set('configTypesInfoMap', configTypesInfoMap);
    return configTypesInfo;
  },

  /**
   *
   * @param configs
   */
  addYarnCapacityScheduler: function(configs) {
    var value = '', savedValue = '', recommendedValue = '',
      excludedConfigs = App.config.getPropertiesFromTheme('YARN');

    var connectedConfigs = configs.filter(function(config) {
      return !excludedConfigs.contains(App.config.configId(config.get('name'), config.get('filename'))) && (config.get('filename') === 'capacity-scheduler.xml');
    });
    var names = connectedConfigs.mapProperty('name');

    connectedConfigs.forEach(function (config) {
      value += config.get('name') + '=' + config.get('value') + '\n';
      if (!Em.isNone(config.get('savedValue'))) {
        savedValue += config.get('name') + '=' + config.get('savedValue') + '\n';
      }
      if (!Em.isNone(config.get('recommendedValue'))) {
        recommendedValue += config.get('name') + '=' + config.get('recommendedValue') + '\n';
      }
    }, this);

    var isFinal = connectedConfigs.someProperty('isFinal', true);
    var savedIsFinal = connectedConfigs.someProperty('savedIsFinal', true);
    var recommendedIsFinal = connectedConfigs.someProperty('recommendedIsFinal', true);

    var cs = App.config.createDefaultConfig('capacity-scheduler', 'capacity-scheduler.xml', true, {
      'value': value,
      'serviceName': 'YARN',
      'savedValue': savedValue || null,
      'recommendedValue': recommendedValue || null,
      'isFinal': isFinal,
      'savedIsFinal': savedIsFinal,
      'recommendedIsFinal': recommendedIsFinal,
      'category': 'CapacityScheduler',
      'displayName': 'Capacity Scheduler',
      'description': 'Capacity Scheduler properties',
      'displayType': 'capacityScheduler'
    });

    configs = configs.filter(function(c) {
      return !(names.contains(c.get('name')) && (c.get('filename') === 'capacity-scheduler.xml'));
    });
    configs.push(App.ServiceConfigProperty.create(cs));
    return configs;
  },

  /**
   *
   * @param serviceName
   * @returns {Array}
   */
  getPropertiesFromTheme: function (serviceName) {
    var properties = [];
    App.Tab.find().rejectProperty('isCategorized').forEach(function (t) {
      if (!t.get('isAdvanced') && t.get('serviceName') === serviceName) {
        t.get('sections').forEach(function (s) {
          s.get('subSections').forEach(function (ss) {
            properties = properties.concat(ss.get('configProperties'));
          });
        });
      }
    }, this);
    return properties;
  },

  /**
   * transform one config with textarea content
   * into set of configs of file
   * @param configs
   * @param filename
   * @return {*}
   */
  textareaIntoFileConfigs: function (configs, filename) {
    var configsTextarea = configs.findProperty('name', 'capacity-scheduler');
    var stackConfigs = App.configsCollection.getAll();
    if (configsTextarea && !App.get('testMode')) {
      var properties = configsTextarea.get('value').split('\n');

      properties.forEach(function (_property) {
        var name, value, isUserProperty;
        if (_property) {
          _property = _property.split(/=(.+)/);
          name = _property[0];
          value = (_property[1]) ? _property[1] : "";
          isUserProperty = !stackConfigs.filterProperty('filename', 'capacity-scheduler.xml').findProperty('name', name);

          configs.push(Em.Object.create({
            name: name,
            value: value,
            savedValue: value,
            serviceName: configsTextarea.get('serviceName'),
            filename: filename,
            isFinal: configsTextarea.get('isFinal'),
            isNotDefaultValue: configsTextarea.get('isNotDefaultValue'),
            isRequiredByAgent: configsTextarea.get('isRequiredByAgent'),
            isUserProperty: isUserProperty,
            group: null
          }));
        }
      });
      return configs.without(configsTextarea);
    }
    return configs;
  },

  /**
   * trim trailing spaces for all properties.
   * trim both trailing and leading spaces for host displayType and hive/oozie datebases url.
   * for directory or directories displayType format string for further using.
   * for password and values with spaces only do nothing.
   * @param {Object} property
   * @returns {*}
   */
  trimProperty: function (property) {
    var displayType = Em.get(property, 'displayType');
    var value = Em.get(property, 'value');
    var name = Em.get(property, 'name');
    var rez;
    switch (displayType) {
      case 'directories':
      case 'directory':
        rez = value.replace(/,/g, ' ').trim().split(/\s+/g).join(',');
        break;
      case 'host':
        rez = value.trim();
        break;
      case 'password':
        break;
      default:
        if (name == 'javax.jdo.option.ConnectionURL' || name == 'oozie.service.JPAService.jdbc.url') {
          rez = value.trim();
        }
        rez = (typeof value == 'string') ? value.replace(/(\s+$)/g, '') : value;
    }
    return ((rez == '') || (rez == undefined)) ? value : rez;
  },

  /**
   * Generate minimal config property object used in *_properties.js files.
   * Example:
   * <code>
   *   var someProperties = App.config.generateConfigPropertiesByName([
   *    'property_1', 'property_2', 'property_3'], { category: 'General', filename: 'myFileName'});
   *   // someProperties contains Object[]
   *   [
   *    {
   *      name: 'property_1',
   *      displayName: 'property_1',
   *      isVisible: true,
   *      isReconfigurable: true,
   *      category: 'General',
   *      filename: 'myFileName'
   *    },
   *    .......
   *   ]
   * </code>
   * @param {string[]} names
   * @param {Object} properties - additional properties which will merge with base object definition
   * @returns {object[]}
   * @method generateConfigPropertiesByName
   */
  generateConfigPropertiesByName: function (names, properties) {
    return names.map(function (item) {
      var baseObj = {
        name: item
      };
      if (properties) return $.extend(baseObj, properties);
      else return baseObj;
    });
  },

  /**
   * load cluster stack configs from server and run mapper
   * @returns {$.ajax}
   * @method loadConfigsFromStack
   */
  loadClusterConfigsFromStack: function () {
    return App.ajax.send({
      name: 'configs.stack_configs.load.cluster_configs',
      sender: this,
      data: {
        stackVersionUrl: App.get('stackVersionURL')
      },
      success: 'saveConfigsToModel'
    });
  },

  /**
   * load stack configs from server and run mapper
   * @param {String[]} [serviceNames=null]
   * @returns {$.ajax}
   * @method loadConfigsFromStack
   */
  loadConfigsFromStack: function (serviceNames) {
    serviceNames = serviceNames || [];
    var name = serviceNames.length > 0 ? 'configs.stack_configs.load.services' : 'configs.stack_configs.load.all';
    return App.ajax.send({
      name: name,
      sender: this,
      data: {
        stackVersionUrl: App.get('stackVersionURL'),
        serviceList: serviceNames.join(',')
      },
      success: 'saveConfigsToModel'
    });
  },

  /**
   * Runs <code>stackConfigPropertiesMapper<code>
   * @param {object} data
   * @method saveConfigsToModel
   */
  saveConfigsToModel: function (data) {
    App.stackConfigPropertiesMapper.map(data);
  },

  /**
   * Check if config filename supports final attribute
   * @param serviceName
   * @param filename
   * @returns {boolean}
   */
  shouldSupportFinal: function (serviceName, filename) {
    var unsupportedServiceNames = ['MISC', 'Cluster'];
    if (!serviceName || unsupportedServiceNames.contains(serviceName) || !filename) {
      return false;
    } else {
      var stackService = App.StackService.find(serviceName);
      if (!stackService) {
        return false;
      }
      return !!this.getConfigTypesInfoFromService(stackService).supportsFinal.find(function (configType) {
        return filename.startsWith(configType);
      });
    }
  },

  shouldSupportAddingForbidden: function(serviceName, filename) {
    var unsupportedServiceNames = ['MISC', 'Cluster'];
    if (!serviceName || unsupportedServiceNames.contains(serviceName) || !filename) {
      return false;
    } else {
      var stackServiceName = App.StackService.find().findProperty('serviceName', serviceName);
      if (!stackServiceName) {
        return false;
      }
      var stackService = App.StackService.find(serviceName);
      return !!this.getConfigTypesInfoFromService(stackService).supportsAddingForbidden.find(function (configType) {
        return filename.startsWith(configType);
      });
    }
  },

  /**
   * Remove all ranger-related configs, that should be available only if Ranger is installed
   * @param configs - stepConfigs object
   */
  removeRangerConfigs: function (configs) {
    configs.forEach(function (service) {
      var filteredConfigs = [];
      service.get('configs').forEach(function (config) {
        if (!/^ranger-/.test(config.get('filename'))) {
          filteredConfigs.push(config);
        }
      });
      service.set('configs', filteredConfigs);
      var filteredCategories = [];
      service.get('configCategories').forEach(function (category) {
        if (!/ranger-/.test(category.get('name'))) {
          filteredCategories.push(category);
        }
      });
      service.set('configCategories', filteredCategories);
    });
  },

  /**
   * Create config with non default config group. Some custom config properties
   * can be created and assigned to non-default config group.
   *
   * @param {Em.Object} override - config value
   * @param {Em.Object} configGroup - config group to set
   * @return {Object}
   **/
  createCustomGroupConfig: function (override, configGroup) {
    App.assertObject(override);
    App.assertEmberObject(configGroup);

    var newOverride = App.ServiceConfigProperty.create(this.createDefaultConfig(override.propertyName, override.filename, false, override));

    newOverride.setProperties({
      'isOriginalSCP': false,
      'overrides': null,
      'group': configGroup,
      'parentSCP': null,
      isCustomGroupConfig: true
    });

    if (!configGroup.get('properties.length')) {
      configGroup.set('properties', Em.A([]));
    }
    configGroup.set('properties', configGroup.get('properties').concat(newOverride));

    return newOverride;
  },


  /**
   * @param {App.ServiceConfigProperty} serviceConfigProperty
   * @param {Object} override - plain object with properties that is different from parent SCP
   * @param {App.ServiceConfigGroup} configGroup
   * @returns {App.ServiceConfigProperty}
   */
  createOverride: function(serviceConfigProperty, override, configGroup) {
    App.assertObject(serviceConfigProperty);
    App.assertEmberObject(configGroup);

    if (Em.isNone(serviceConfigProperty.get('overrides'))) serviceConfigProperty.set('overrides', []);

    var newOverride = App.ServiceConfigProperty.create(serviceConfigProperty);

    newOverride.setProperties({ 'savedValue': null, 'savedIsFinal': null });

    if (!Em.isNone(override)) {
      for (var key in override) {
        newOverride.set(key, override[key]);
      }
    }

    newOverride.setProperties({
      'isOriginalSCP': false,
      'overrides': null,
      'group': configGroup,
      'parentSCP': serviceConfigProperty
    });

    if (!configGroup.get('properties.length')) {
      configGroup.set('properties', Em.A([]));
    }
    configGroup.set('properties', configGroup.get('properties').concat(newOverride));

    serviceConfigProperty.get('overrides').pushObject(newOverride);

    var savedOverrides = serviceConfigProperty.get('overrides').filter(function (override) {
      return !Em.isNone(Em.get(override, 'savedValue'));
    });
    serviceConfigProperty.set('overrideValues', savedOverrides.mapProperty('savedValue'));
    serviceConfigProperty.set('overrideIsFinalValues', savedOverrides.mapProperty('savedIsFinal'));

    return newOverride;
  },


  /**
   * Merge values in "stored" to "base" if name matches, it's a value only merge.
   * @param base {Array} Em.Object
   * @param stored {Array} Object
   * @returns {Object[]|Em.Object[]} base
   */
  mergeStoredValue: function(base, stored) {
    if (stored) {
      base.forEach(function (p) {
        var sp = stored.filterProperty('filename', p.filename).findProperty('name', p.name);
        if (sp) {
          Em.set(p, 'value', Em.get(sp, 'value'));
        }
      });
    }
    return base;
  },


  /**
   * Helper method to get property from the <code>stepConfigs</code>
   *
   * @param {String} name - config property name
   * @param {String} fileName - config property filename
   * @param {Object[]} stepConfigs
   * @return {App.ServiceConfigProperty|Boolean} - App.ServiceConfigProperty instance or <code>false</code> when property not found
   */
  findConfigProperty: function(stepConfigs, name, fileName) {
    if (!name || !fileName) return false;
    if (stepConfigs && stepConfigs.length) {
      return stepConfigs.mapProperty('configs').filter(function(item) {
        return item.length;
      }).reduce(function(p, c) {
        if (p) {
          return p.concat(c);
        }
      }).filterProperty('filename', fileName).findProperty('name', name);
    }
    return false;
  },

  getTempletonHiveHosts: function (value) {
    var pattern = /thrift:\/\/.+:\d+/,
      patternMatch = value.match(pattern);
    return patternMatch ? patternMatch[0].split('\\,') : [];
  },

  /**
   * Update config property value based on its current value and list of zookeeper server hosts.
   * Used to prevent sort order issues.
   * <code>siteConfigs</code> object formatted according server's persist format e.g.
   *
   * <code>
   *   {
   *     'yarn-site': {
   *       'property_name1': 'property_value1'
   *       'property_name2': 'property_value2'
   *       .....
   *     }
   *   }
   * </code>
   *
   * @method updateHostsListValue
   * @param {Object} siteConfigs - prepared site config object to store
   * @param {String} propertyType - type of the property to update
   * @param {String} propertyName - name of the property to update
   * @param {String} hostsList - list of ZooKeeper Server names to set as config property value
   * @param {Boolean} isArray - determines whether value string is formatted as array
   * @return {String} - result value
   */
  updateHostsListValue: function(siteConfigs, propertyType, propertyName, hostsList, isArray) {
    var value = hostsList,
      propertyHosts = (siteConfigs[propertyName] || ''),
      hostsToSet = hostsList,
      parser = this.get('uniqueHostsListParsers').find(function (property) {
        return property.type === propertyType && property.propertyName === propertyName;
      });
    if (parser) {
      propertyHosts = this.get(parser.method)(propertyHosts);
      hostsToSet = this.get(parser.method)(hostsToSet);
    } else {
      if (isArray) {
        var pattern = /(^\[|]$)/g;
        propertyHosts = propertyHosts.replace(pattern, '');
        hostsToSet = hostsToSet.replace(pattern, '');
      }
      propertyHosts = propertyHosts.split(',');
      hostsToSet = hostsToSet.split(',');
    }

    if (!Em.isEmpty(siteConfigs[propertyName])) {
      var diffLength = propertyHosts.filter(function(hostName) {
        return !hostsToSet.contains(hostName);
      }).length;
      if (diffLength == 0 && propertyHosts.length == hostsToSet.length) {
        value = siteConfigs[propertyName];
      }
    }
    siteConfigs[propertyName] = value;
    return value;
  },

  /**
   * Load cluster-env configs mapped to array
   * @return {*|{then}}
   */
  getConfigsByTypes: function (sites) {
    const dfd = $.Deferred();
    App.router.get('configurationController').getCurrentConfigsBySites(sites.mapProperty('site')).done((configs) => {
      dfd.resolve(this.getMappedConfigs(configs, sites));
    });
    return dfd.promise();
  },

  /**
   *
   * @param configs
   * @param sites
   */
  getMappedConfigs: function (configs, sites) {
    const result = [];
    configs.forEach(function (config) {
      var configsArray = [];
      var configsObject = config.properties;
      for (var property in configsObject) {
        if (configsObject.hasOwnProperty(property)) {
          configsArray.push(Em.Object.create({
            name: property,
            value: configsObject[property],
            filename: App.config.getOriginalFileName(config.type)
          }));
        }
      }
      result.push(Em.Object.create({
        serviceName: sites.findProperty('site', config.type).serviceName,
        configs: configsArray
      }));
    });

    return result;
  }
});
