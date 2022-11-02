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

App.ConfigRecommendationParser = Em.Mixin.create(App.ConfigRecommendations, {

  stepConfigs: [],

  modifiedFileNames: [],

  /**
   * Adds new modified file name if it's not in the list yet
   *
   * @param filename
   */
  addModifiedFileName: function(filename) {
    App.assertExists(filename);
    if (!this.get('modifiedFileNames').contains(filename))
      this.get('modifiedFileNames').push(filename);
  },

  /**
   * Method that goes through all configs
   * and apply recommendations using callbacks
   *
   * @param recommendationObject
   * @param configs
   * @param parentProperties
   * @param configGroup
   * @param updateCallback
   * @param removeCallback
   * @param updateBoundariesCallback
   */
  parseRecommendations: function(recommendationObject, configs, parentProperties, configGroup,
                                 updateCallback, removeCallback, updateBoundariesCallback) {

    App.assertObject(recommendationObject);
    App.assertArray(configs);
    App.assertFunction(updateCallback);
    App.assertFunction(removeCallback);
    App.assertFunction(updateBoundariesCallback);
    var propertiesToDelete = [];
    configs.forEach(function (config) {
      var name = Em.get(config, 'name'),
          fileName = Em.get(config, 'filename'),
          value = Em.get(config, 'value'),
          recommendations = recommendationObject[App.config.getConfigTagFromFileName(fileName)];

      if (recommendations) {

        if (recommendations.properties) {
          var recommendedValue = App.config.formatValue(recommendations.properties[name]);
          if (!Em.isNone(recommendedValue)) {
            /** update config **/
            updateCallback(config, recommendedValue, parentProperties, configGroup);

            delete recommendations.properties[name];
          }
        }

        if (recommendations.property_attributes) {
          var propertyAttributes = recommendations.property_attributes[name];
          if (propertyAttributes) {
            var stackProperty = App.configsCollection.getConfigByName(name, fileName);
            for (var attr in propertyAttributes) {
              if (attr === 'delete' && this.allowUpdateProperty(parentProperties, name, fileName, null, value)) {
                propertiesToDelete.push(config);
              } else if (attr === 'visible' || stackProperty) {
                /** update config boundaries **/
                updateBoundariesCallback(stackProperty, attr, propertyAttributes[attr], name, fileName, configGroup);
              }
            }
            Em.tryInvoke(config, 'validate');
          }
        }
      }
    }, this);

    if (propertiesToDelete.length) {
      propertiesToDelete.forEach(function (property) {
        /** remove config **/
        removeCallback(property, configs, parentProperties, configGroup);

      }, this);
    }
  },

  /**
   * Method that goes through all configs
   * and apply recommendations to configs when it's needed
   *
   * @param {Object} recommendationObject
   * @param {Object[]} configs
   * @param {Object[]} parentProperties
   */
  updateConfigsByRecommendations: function (recommendationObject, configs, parentProperties) {
    this.parseRecommendations(recommendationObject, configs, parentProperties, null,
      this._updateConfigByRecommendation.bind(this), this._removeConfigByRecommendation.bind(this), this._updateBoundaries.bind(this));
  },

  /**
   * This method goes through all config recommendations
   * and trying to add new properties
   *
   * @param {Object} recommendationObject
   * @param {Object[]} parentProperties
   */
  addByRecommendations: function (recommendationObject, parentProperties) {
    App.assertObject(recommendationObject);

    for (var site in recommendationObject) {
      var properties = recommendationObject[site].properties;
      if (properties && Object.keys(properties).length) {
        var stepConfig = App.config.getStepConfigForProperty(this.get('stepConfigs'), site), configs = [];
        if (stepConfig) {
          for (var propertyName in properties) {
            if (this.allowUpdateProperty(parentProperties, propertyName, site)) {
              configs.pushObject(this._createNewProperty(propertyName, site, stepConfig.get('serviceName'), properties[propertyName], parentProperties));
            }
          }
          if (configs.length) {
            var mergedConfigs = configs.concat(stepConfig.get('configs'));
            stepConfig.set('configs', mergedConfigs);
            stepConfig.propertyDidChange('redrawConfigs');
          }
        }
      }
    }
  },

  /**
   * Update config based on recommendations
   *
   * @param {recommendation} config
   * @param {String} recommendedValue
   * @param {String[]} [parentProperties]
   * @returns {recommendation}
   * @protected
   */
  _updateConfigByRecommendation: function (config, recommendedValue, parentProperties) {
    App.assertObject(config);
    var name = Em.get(config, 'name'),
        fileName = Em.get(config, 'filename'),
        group = Em.get(config, 'group.name'),
        value = Em.get(config, 'value'),
        isUserOverriden = Em.get(config, 'didUserOverrideValue');;
    Em.set(config, 'recommendedValue', recommendedValue);
    if (this.allowUpdateProperty(parentProperties, name, fileName, group, value)) {
      var allowConfigUpdate = true;
      // workaround for capacity-scheduler
      if (this.get('currentlyChangedConfig')) {
        var cId = App.config.configId(this.get('currentlyChangedConfig.name'), this.get('currentlyChangedConfig.fileName'));
        if (App.config.configId(name, fileName) === cId) {
          allowConfigUpdate = false;
        }
      }

      if (isUserOverriden && name !== "capacity-scheduler") {
        allowConfigUpdate = false;
      }

      if (allowConfigUpdate) {
        Em.setProperties(config, {
          value: recommendedValue,
          errorMessage: '',
          warnMessage: ''
        });
      }
      if (!Em.isNone(recommendedValue) && !Em.get(config, 'hiddenBySection')) {
        Em.set(config, 'isVisible', true);
      }
      let recommendationExists = this.getRecommendation(name, fileName, group);
      if (recommendationExists && isUserOverriden) {
        this.removeRecommendation(name, fileName, group);
      } else if (!isUserOverriden) {
        this.applyRecommendation(name, fileName, group, recommendedValue, this._getInitialValue(config), parentProperties, Em.get(config, 'isEditable'));
      }
    }
    if (this.updateInitialOnRecommendations(Em.get(config, 'serviceName'))) {
      Em.set(config, 'initialValue', recommendedValue);
    }
    Em.tryInvoke(config, 'validate');
    return config;
  },
  
  /**
   * Add config based on recommendations
   *
   * @param name
   * @param fileName
   * @param serviceName
   * @param recommendedValue
   * @param parentProperties
   * @protected
   */
  _createNewProperty: function (name, fileName, serviceName, recommendedValue, parentProperties) {
    App.assertExists(name, 'name');
    App.assertExists(fileName, 'fileName');
    App.assertExists(serviceName, 'serviceName');

    var coreObject = this._getCoreProperties(serviceName, recommendedValue, this._getInitialFromRecommendations(name, fileName)),
      newConfig = App.config.getDefaultConfig(name, fileName, coreObject),
      addedPropertyObject = App.ServiceConfigProperty.create(newConfig);

    this.applyRecommendation(name, fileName, "Default",
      recommendedValue, null, parentProperties, true);

    return addedPropertyObject;
  },

  /**
   *
   * @param serviceName
   * @param recommendedValue
   * @param initialValue
   * @returns {{value: *, recommendedValue: *, initialValue: *, savedValue: *}}
   * @private
   */
  _getCoreProperties: function(serviceName, recommendedValue, initialValue) {
    return {
      "value": recommendedValue,
      "recommendedValue": recommendedValue,
      "initialValue": this.updateInitialOnRecommendations(serviceName) ? recommendedValue : initialValue,
      "savedValue": !this.useInitialValue(serviceName) ? initialValue : null,
      "isNotSaved": Em.isNone(initialValue)
    }
  },


  /**
   * Remove config based on recommendations
   *
   * @param config
   * @param configsCollection
   * @param parentProperties
   * @protected
   */
  _removeConfigByRecommendation: function (config, configsCollection, parentProperties) {
    App.assertObject(config);
    App.assertArray(configsCollection);

    if (this._configHasInitialValue(config)) {
      this.addModifiedFileName(Em.get(config, 'filename'));
    }

    configsCollection.removeObject(config);

    this.applyRecommendation(Em.get(config, 'name'), Em.get(config, 'filename'), Em.get(config, 'group.name'),
      null, this._getInitialValue(config), parentProperties, Em.get(config, 'isEditable'));
  },

  /**
   * Defines if property was defined on initial load or was saved.
   *
   * @param config
   * @returns {boolean}
   * @private
   */
  _configHasInitialValue: function(config) {
    App.assertObject(config);
    return !Em.isNone(Em.get(config, 'savedValue')) && !Em.isNone(Em.get(config, 'initialValue'));
  },

  /**
   * Update config valueAttributes by recommendations
   *
   * @param {Object} stackProperty
   * @param {string} attr
   * @param {Number|String|Boolean} value
   * @param {String} name
   * @param {String} fileName
   * @protected
   */
  _updateBoundaries: function(stackProperty, attr, value, name, fileName) {
    if (attr === 'visible') {
      var p = App.config.findConfigProperty(this.get('stepConfigs'), name, App.config.getOriginalFileName(fileName));
      if (p) {
        p.set('isVisible', value);
      }
    }
    if (stackProperty) {
      if (!Em.get(stackProperty, 'valueAttributes')) {
        stackProperty.valueAttributes = {};
      }
      Em.set(stackProperty.valueAttributes, attr, value);
    }
    return stackProperty || null;
  },

  /**
   * Get initial config value that was before recommendations was applied
   *
   * @param name
   * @param fileName
   * @returns {*}
   * @protected
   */
  _getInitialFromRecommendations: function(name, fileName) {
    try {
      return this.getRecommendation(name, fileName).initialValue;
    } catch(e) {
      return null;
    }
  },

  /**
   * Get default config value
   * <code>savedValue<code> for installed services
   * <code>initialValue<code> for new services
   *
   * @param configProperty
   * @returns {*}
   * @protected
   */
  _getInitialValue: function (configProperty) {
    if (!configProperty) return null;
    return this.useInitialValue(Em.get(configProperty, 'serviceName')) ?
      Em.get(configProperty, 'initialValue') : Em.get(configProperty, 'savedValue');
  },

  /**
   * Update initial only when <code>initialValue<code> is used
   *
   * @param {string} serviceName
   * @returns {boolean}
   */
  updateInitialOnRecommendations: function(serviceName) {
    return this.useInitialValue(serviceName);
  },

  /**
   * Defines if initialValue of config can be used on current controller
   * if not savedValue is used instead
   *
   * @param {String} serviceName
   * @return {boolean}
   */
  useInitialValue: function (serviceName) {
    return false;
  },

  /**
   * Defines if recommendation allowed to be applied
   *
   * @param {Array} parentProperties
   * @param {string} name
   * @param {string} fileName
   * @param {string} configGroup
   * @param {*} savedValue
   * @returns {boolean}
   */
  allowUpdateProperty: function (parentProperties, name, fileName, configGroup, savedValue) {
    try {
      return Em.get(this.getRecommendation(name, fileName, configGroup), 'saveRecommended');
    } catch (e) {
      return true;
    }
  }
});
