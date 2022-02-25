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

App.ConfigWithOverrideRecommendationParser = Em.Mixin.create(App.ConfigRecommendationParser, {

  /**
   * Method that goes through all configs
   * and apply recommendations to overrides when it's needed
   *
   * @param {Object} recommendationObject
   * @param {Object[]} configs
   * @param {Object[]} parentProperties
   * @param {App.ServiceConfigGroup} configGroup
   */
  updateOverridesByRecommendations: function (recommendationObject, configs, parentProperties, configGroup) {
    Em.assert('Config groups should be defined and not default', configGroup && configGroup.get('name') && !configGroup.get('isDefault'));
    this.parseRecommendations(recommendationObject, configs, parentProperties, configGroup,
      this._updateOverride.bind(this), this._removeOverride.bind(this), this._updateOverrideBoundaries.bind(this));
  },

  /**
   * Update override by recommendations
   * includes add/update actions
   *
   * @param config
   * @param recommendedValue
   * @param parentProperties
   * @param configGroup
   * @protected
   */
  _updateOverride: function(config, recommendedValue, parentProperties, configGroup) {
    var name = Em.get(config, 'name'),
      fileName = Em.get(config, 'filename'),
      group = Em.get(config, 'group.name'),
      value = Em.get(config, 'value');

    var updateValue = this.allowUpdateProperty(parentProperties, name, fileName, group, value);
    var override = config.getOverride(configGroup.get('name'));
    if (override) {
      this._updateConfigByRecommendation(override, recommendedValue, parentProperties);
    } else if (updateValue) {
      this._addConfigOverrideRecommendation(config, recommendedValue, parentProperties, configGroup);
    }
  },

  /**
   * Remove override by recommendations
   *
   * @param property
   * @param configs
   * @param parentProperties
   * @param configGroup
   * @protected
   */
  _removeOverride: function(property, configs, parentProperties, configGroup) {
    var config = property.getOverride(configGroup.get('name'));
    config = config ? config : property;
    this._removeConfigByRecommendation(config, property.get('overrides') || [], parentProperties);
  },

  /**
   * Add override by recommendations
   *
   * @param config
   * @param recommendedValue
   * @param configGroup
   * @param parentProperties
   * @protected
   */
  _addConfigOverrideRecommendation: function (config, recommendedValue, parentProperties, configGroup) {
    var popupProperty = this.getRecommendation(Em.get(config, 'name'), Em.get(config, 'filename'), configGroup.get('name')),
      initialValue = popupProperty ? popupProperty.value : null;
    var coreObject = {
      "value": recommendedValue,
      "recommendedValue": recommendedValue,
      "initialValue": initialValue,
      "savedValue": !this.useInitialValue(Em.get(config, 'serviceName')) && !Em.isNone(initialValue) ? initialValue : null,
      "isEditable": true,
      "errorMessage": '',
      "warnMessage": ''
    };
    var override = App.config.createOverride(config, coreObject, configGroup);

    this.applyRecommendation(Em.get(config, 'name'),
                             Em.get(config, 'filename'),
                             configGroup.get('name'),
                             recommendedValue,
                             this._getInitialValue(override),
                             parentProperties,
                             Em.get(config, 'isEditable'));
  },

  /**
   * Update override valueAttributes by recommendations
   *
   * @param {Object} stackProperty
   * @param {string} attr
   * @param {Number|String|Boolean} value
   * @param {String} name
   * @param {String} fileName
   * @param {App.ServiceConfigGroup} configGroup
   * @protected
   */
  _updateOverrideBoundaries: function(stackProperty, attr, value, name, fileName, configGroup) {
    if (!stackProperty.valueAttributes[configGroup.get('name')]) {
      stackProperty.valueAttributes[configGroup.get('name')] = {};
    }
    Em.set(stackProperty.valueAttributes[configGroup.get('name')], attr, value);
  }
});