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
var objectUtils = require('utils/object_utils');

App.ConfigsComparator = Em.Mixin.create({

  isCompareMode: false,

  compareServiceVersion: null,

  selectedVersion: null,

  isVersionDefault: Em.K,
  /**
   * load version configs for comparison
   * @param allConfigs
   * @return {object}
   * @public
   * @method loadCompareVersionConfigs
   */
  loadCompareVersionConfigs: function (allConfigs) {
    var dfd = $.Deferred();
    var self = this;
    var compareServiceVersions = [];

    if (this.get('compareServiceVersion')) {
      if (!this.isVersionDefault(this.get('compareServiceVersion').get('version'))) {
        compareServiceVersions = [this.get('compareServiceVersion').get('version'), this.get('selectedVersion')];
      } else {
        compareServiceVersions = [this.get('compareServiceVersion').get('version')];
      }
      this.set('isCompareMode', true);
      this.getCompareVersionConfigs(compareServiceVersions).done(function (json) {
        allConfigs.setEach('isEditable', false);
        self.initCompareConfig(allConfigs, json);
        dfd.resolve(true);
      }).fail(function () {
        self.set('compareServiceVersion', null);
        dfd.resolve(true);
      });
    } else {
      self.set('isCompareMode', false);
      allConfigs.setEach('isComparison', false);
      dfd.resolve(false);
    }
    return dfd.promise();
  },

  /**
   * attach analogical config to each property for comparison
   * @param allConfigs
   * @param json
   * @private
   * @method initCompareConfig
   */
  initCompareConfig: function(allConfigs, json) {
    var serviceVersionMap = {};
    var configNamesMap = allConfigs.toWickMapByProperty('name');
    var serviceName = this.get('content.serviceName');
    var compareVersionNumber = this.get('compareServiceVersion.version');
    //indicate whether compared versions are from non-default group
    var compareNonDefaultVersions = (json.items.length > 1);

    serviceVersionMap[compareVersionNumber] = {};
    if (compareNonDefaultVersions) {
      serviceVersionMap[this.get('selectedVersion')] = {};
    }

    json.items.forEach(function (item) {
      item.configurations.forEach(function (configuration) {
        if (serviceName === 'YARN' && configuration.type === 'capacity-scheduler') {
          this.addCompareCSConfigs(configuration, serviceVersionMap, item);
        } else {
          for (const prop in configuration.properties) {
            const name = JSON.parse('"' + prop + '"');
            serviceVersionMap[item.service_config_version][name + '-' + configuration.type] = {
              name: name,
              value: configuration.properties[prop],
              type: configuration.type,
              tag: configuration.tag,
              version: configuration.version,
              service_config_version: item.service_config_version,
              filename: App.config.getOriginalFileName(configuration.type)
            };
            if (Em.isNone(configNamesMap[name])) {
              allConfigs.push(this.getMockConfig(name, serviceName, App.config.getOriginalFileName(configuration.type)));
            }
          }
        }
        if (configuration.properties_attributes && configuration.properties_attributes.final) {
          for (var final in configuration.properties_attributes.final) {
            var config = serviceVersionMap[item.service_config_version][final + '-' + configuration.type];
            if (config) {
              config.isFinal = (configuration.properties_attributes.final[final] === 'true');
            }
          }
        }
      }, this);
    }, this);

    this.addCompareConfigs(compareNonDefaultVersions, allConfigs, serviceVersionMap);
  },

  /**
   *
   * @param {boolean} compareNonDefaultVersions
   * @param {Array} allConfigs
   * @param {object} serviceVersionMap
   */
  addCompareConfigs: function(compareNonDefaultVersions, allConfigs, serviceVersionMap) {
    var compareVersionNumber = this.get('compareServiceVersion.version');
    var serviceName = this.get('content.serviceName');
    if (compareNonDefaultVersions) {
      allConfigs.forEach(function (serviceConfig) {
        if (Em.get(serviceConfig, 'isRequiredByAgent') !== false) {
          this.setCompareConfigs(serviceConfig, serviceVersionMap, compareVersionNumber, this.get('selectedVersion'));
        }
      }, this);
    } else {
      var serviceCfgVersionMap = serviceVersionMap[this.get('compareServiceVersion.version')] || {};
      var allConfigsMap = {};
      allConfigs.forEach(function (serviceConfig) {
        var id = serviceConfig.name + '-' + App.config.getConfigTagFromFileName(serviceConfig.filename);
        allConfigsMap[id] = serviceConfig;
        if (Em.get(serviceConfig, 'isRequiredByAgent') !== false) {
          var compareConfig = serviceCfgVersionMap[id];
          this.setCompareDefaultGroupConfig(serviceConfig, compareConfig);
        }
      }, this);
      if (allConfigs.length !== Object.keys(serviceCfgVersionMap).length) {
        Object.keys(serviceCfgVersionMap).forEach(id => {
          if (!allConfigsMap[id]) {
            var mockConfig = this.getMockConfig(serviceCfgVersionMap[id].name, serviceName, serviceCfgVersionMap[id].filename);
            this.setCompareDefaultGroupConfig(mockConfig, serviceCfgVersionMap[id]);
            allConfigs.push(mockConfig);
          }
        });
      }
    }
  },

  /**
   * init compare configs for Capacity-scheduler
   * @param {object} configuration
   * @param {object} serviceVersionMap
   * @param {object} item
   */
  addCompareCSConfigs: function(configuration, serviceVersionMap, item) {
    var configsToSkip = App.config.getPropertiesFromTheme('YARN');
    // put all properties in a single textarea for capacity-scheduler
    var value = '';
    for (var prop in configuration.properties) {
      if (configsToSkip.contains(App.config.configId(prop, configuration.type))) {
        serviceVersionMap[item.service_config_version][prop + '-' + configuration.type] = {
          name: prop,
          value: configuration.properties[prop],
          type: configuration.type,
          tag: configuration.tag,
          version: configuration.version,
          service_config_version: item.service_config_version
        };
      } else {
        value += prop + '=' + configuration.properties[prop] + '\n';
      }
    }
    serviceVersionMap[item.service_config_version][configuration.type + '-' + configuration.type] = {
      name: configuration.type,
      value: value,
      type: configuration.type,
      tag: configuration.tag,
      version: configuration.version,
      service_config_version: item.service_config_version
    };
  },

  /**
   * set compare properties to service config of non-default group
   * @param serviceConfig
   * @param serviceVersionMap
   * @param compareVersion
   * @param selectedVersion
   * @private
   * @method setCompareConfigs
   */
  setCompareConfigs: function (serviceConfig, serviceVersionMap, compareVersion, selectedVersion) {
    var tag = App.config.getConfigTagFromFileName(Em.get(serviceConfig, 'filename'));
    var compareConfig = serviceVersionMap[compareVersion][Em.get(serviceConfig, 'name') + '-' + tag];
    var selectedConfig = serviceVersionMap[selectedVersion][Em.get(serviceConfig, 'name') + '-' + tag];
    var compareConfigs = [];

    Em.set(serviceConfig, 'isComparison', true);

    if (!Em.get(serviceConfig, 'isCustomGroupConfig')) {
      Em.set(serviceConfig, 'hasCompareDiffs', false);
    }

    if (compareConfig && selectedConfig) {
      compareConfigs.push(this.getComparisonConfig(serviceConfig, compareConfig));
      compareConfigs.push(this.getComparisonConfig(serviceConfig, selectedConfig));
      Em.set(serviceConfig, 'hasCompareDiffs', this.hasCompareDiffs(compareConfigs[0], compareConfigs[1]));
    } else if (compareConfig && !selectedConfig) {
      compareConfigs.push(this.getComparisonConfig(serviceConfig, compareConfig));
      compareConfigs.push(this.getMockComparisonConfig(selectedConfig, selectedVersion));
      Em.set(serviceConfig, 'hasCompareDiffs', true);
    } else if (!compareConfig && selectedConfig) {
      compareConfigs.push(this.getMockComparisonConfig(selectedConfig, compareVersion));
      compareConfigs.push(this.getComparisonConfig(serviceConfig, selectedConfig));
      Em.set(serviceConfig, 'hasCompareDiffs', true);
    }

    Em.set(serviceConfig, 'compareConfigs', compareConfigs);
  },

  /**
   * init attributes and wrap mock compare config into App.ServiceConfigProperty
   * @param serviceConfig
   * @param compareServiceVersion
   * @return {object}
   * @private
   * @method getMockComparisonConfig
   */
  getMockComparisonConfig: function (serviceConfig, compareServiceVersion) {
    var compareObject = $.extend(true, {isComparison: false}, serviceConfig);
    Em.setProperties(compareObject, {
      isEditable: false,
      serviceVersion: App.ServiceConfigVersion.find(this.get('content.serviceName') + "_" + compareServiceVersion),
      isMock: true,
      displayType: 'label'
    });
    compareObject = App.ServiceConfigProperty.create(compareObject);
    compareObject.set('value', Em.I18n.t('common.property.undefined'));
    return compareObject;
  },

  /**
   * init attributes and wrap compare config into App.ServiceConfigProperty
   * @param serviceConfig
   * @param compareConfig
   * @return {object}
   * @private
   * @method getComparisonConfig
   */
  getComparisonConfig: function (serviceConfig, compareConfig) {
    var compareObject = $.extend(true, {isComparison: false, isOriginalSCP: false},  serviceConfig);
    Em.set(compareObject, 'isEditable', false);

    if (compareConfig) {
      if (Em.get(serviceConfig, 'isMock')) {
        Em.set(compareObject, 'displayType', 'string');
        Em.set(compareObject, 'isMock', false);
      }
      Em.set(compareObject, 'serviceVersion', App.ServiceConfigVersion.find(this.get('content.serviceName') + "_" + compareConfig.service_config_version));
      compareObject = App.ServiceConfigProperty.create(compareObject);
      compareObject.setProperties({
        isFinal: Boolean(compareConfig.isFinal),
        value: App.config.formatPropertyValue(serviceConfig, compareConfig.value),
        compareConfigs: null,
        isOriginalSCP: false
      });
    }
    return compareObject;
  },

  /**
   * set compare properties to service config of default group
   * @param serviceConfig
   * @param compareConfig
   * @private
   * @method setCompareDefaultGroupConfig
   */
  setCompareDefaultGroupConfig: function (serviceConfig, compareConfig) {
    var compareObject = {};
    var isEmptyProp = App.isEmptyObject(serviceConfig);

    Em.set(serviceConfig, 'compareConfigs', []);
    Em.set(serviceConfig, 'isComparison', true);
    //if config isn't reconfigurable then it can't have changed value to compare
    if (compareConfig) {
      compareObject = this.getComparisonConfig(serviceConfig, compareConfig);
      Em.set(serviceConfig, 'hasCompareDiffs', Em.get(serviceConfig, 'isMock') || this.hasCompareDiffs(serviceConfig, compareObject));
      Em.get(serviceConfig, 'compareConfigs').push(compareObject);
      // user custom property or property that was added during upgrade
    } else {
      if (Em.get(serviceConfig, 'value') !== 'Undefined') {
        var addToComparison = !Em.get(serviceConfig, 'isCustomGroupConfig') && (Em.get(serviceConfig, 'isUserProperty') || !isEmptyProp && !compareConfig && Em.get(serviceConfig, 'isRequiredByAgent') !== false);
        if (addToComparison) {
          Em.get(serviceConfig, 'compareConfigs').push(this.getMockComparisonConfig(serviceConfig, this.get('compareServiceVersion.version')));
          Em.set(serviceConfig, 'hasCompareDiffs', true);
        }
      }
    }

    return serviceConfig;
  },

  /**
   * check value and final attribute of original and compare config for differences
   * if config is password, it won't be shown in the comparison
   *
   * @param originalConfig
   * @param compareConfig
   * @return {Boolean}
   * @private
   * @method hasCompareDiffs
   */
  hasCompareDiffs: function (originalConfig, compareConfig) {
    var displayType = Em.get(originalConfig, 'displayType');
    var notShownTypes = ['password'];
    if (notShownTypes.contains(displayType)) {
      return false;
    }
    var originalValue = App.config.trimProperty({ value: Em.get(originalConfig, 'value'), displayType: 'string' });
    var compareValue = App.config.trimProperty({ value: Em.get(compareConfig, 'value'), displayType: 'string' });

    if (originalValue.toArray) {
      originalValue = originalValue.toArray();
    }
    if (compareValue.toArray) {
      compareValue = compareValue.toArray();
    }

    if (originalValue instanceof Array) {
      originalValue.sort();
    }
    if (compareValue instanceof Array) {
      compareValue.sort();
    }

    return (!objectUtils.deepEqual(originalValue, compareValue)) ||
            (!!Em.get(originalConfig, 'isFinal') !== !!Em.get(compareConfig, 'isFinal'));
  },

  /**
   * generate mock config object
   * @param name
   * @param serviceName
   * @param filename
   * @return {Object}
   * @private
   * @method getMockConfig
   */
  getMockConfig: function (name, serviceName, filename) {
    var undefinedConfig = App.configsCollection.getConfigByName(name, filename) || {
      description: name,
      displayName: name,
      isOverridable: false,
      isReconfigurable: false,
      isRequired: false,
      isRequiredByAgent: true,
      isSecureConfig: false,
      isUserProperty: true,
      isVisible: true,
      isOriginalSCP: false,
      name: name,
      filename: filename,
      serviceName: serviceName,
      category: App.config.getDefaultCategory(false, filename),
      value: Em.I18n.t('common.property.undefined'),
      isMock: true,
      displayType: 'label'
    };
    return App.ServiceConfigProperty.create(undefinedConfig);
  },

  /**
   * get configs of chosen version from server to compare
   * @param compareServiceVersions
   * @return {$.ajax}
   * @private
   * @method getCompareVersionConfigs
   */
  getCompareVersionConfigs: function (compareServiceVersions) {
    this.set('versionLoaded', false);

    return App.ajax.send({
      name: 'service.serviceConfigVersions.get.multiple',
      sender: this,
      data: {
        serviceName: this.get('content.serviceName'),
        serviceConfigVersions: compareServiceVersions
      }
    });
  }

});
