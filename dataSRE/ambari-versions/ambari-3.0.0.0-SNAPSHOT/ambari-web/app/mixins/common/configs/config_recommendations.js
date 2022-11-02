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
 * @typedef {object} recommendation
 * @property {boolean} saveRecommended - by default is true (checkbox binding)
 * @property {boolean} saveRecommendedDefault - used for cancel operation to restore previous state (saved checkbox value)
 * @property {boolean} isDeleted - true if property was deleted
 * @property {boolean} notDefined - true if property was added
 * @property {string} propertyName
 * @property {string} propertyFileName - file name without '.xml'
 * @property {string} configGroup - name of config group, by default "Default"
 * @property {string} serviceName
 * @property {string} serviceDisplayName
 * @property {string} initialValue
 * @property {string} recommendedValue
 * @property {boolean} allowChangeGroup - flag that allows to change config group for config from dependent not default group
 * @property {string[]} parentConfigs - list of properties based on which current recommendation was performed
 */

App.ConfigRecommendations = Em.Mixin.create({

  /**
   * List of recommendations that was applied to configs
   *
   * @type {recommendation[]}
   */
  recommendations: [],

  /**
   * Update recommendation property if exists
   * otherwise add new
   *
   * @param {string} name
   * @param {string} fileName
   * @param {string} configGroupName
   * @param {string} recommendedValue
   * @param {string} initialValue
   * @param {Object[]} parentProperties
   * @param {boolean} isEditable
   * @returns {recommendation}
   */
  applyRecommendation: function (name, fileName, configGroupName, recommendedValue, initialValue, parentProperties, isEditable) {
    try {
      var parentPropertyIds = this.formatParentProperties(parentProperties);
      var recommendation = this.getRecommendation(name, fileName, configGroupName);
      if (recommendation) {
        return this.updateRecommendation(recommendation, recommendedValue, parentPropertyIds);
      }
      return this.addRecommendation(name, fileName, configGroupName, recommendedValue, initialValue, parentPropertyIds, isEditable);
    } catch(e) {
      console.error(e.message);
    }
  },

  /**
   * Format objects like {name: {String}, type: {String}} to config Id
   *
   * @param parentProperties
   * @returns {*}
   */
  formatParentProperties: function(parentProperties) {
    return Em.isArray(parentProperties) ? parentProperties.map(function (p) {
      return App.config.configId(p.name, p.type);
    }) : [];
  },

  /**
   * Add new recommendation
   *
   * @param {string} name
   * @param {string} fileName
   * @param {string} configGroupName
   * @param {string} recommendedValue
   * @param {string} initialValue
   * @param {string[]} parentPropertyIds
   * @param {boolean} isEditable
   * @returns {recommendation}
   */
  addRecommendation: function (name, fileName, configGroupName, recommendedValue, initialValue, parentPropertyIds, isEditable) {
    Em.assert('name and fileName should be defined', name && fileName);
    const site = App.config.getConfigTagFromFileName(fileName);
    const service = App.config.get('serviceByConfigTypeMap')[site];
    const configObject = App.configsCollection.getConfigByName(name, fileName);
    const displayName = configObject && configObject.displayName;

    const recommendation = {
      saveRecommended: true,
      saveRecommendedDefault: true,
      propertyFileName: site,
      propertyName: name,
      propertyTitle: configObject && Em.I18n.t('installer.controls.serviceConfigPopover.title').format(displayName, displayName === name ? '' : name),
      propertyDescription: configObject && configObject.description,

      isDeleted: Em.isNone(recommendedValue),
      notDefined: Em.isNone(initialValue),

      configGroup: configGroupName || "Default",
      initialValue,
      parentConfigs: parentPropertyIds || [],
      serviceName: service.get('serviceName'),
      allowChangeGroup: false,//TODO groupName!= "Default" && (service.get('serviceName') != this.get('selectedService.serviceName'))
      //TODO&& (App.ServiceConfigGroup.find().filterProperty('serviceName', service.get('serviceName')).length > 1), //TODO
      serviceDisplayName: service.get('displayName'),
      recommendedValue: recommendedValue,
      isEditable: isEditable !== false
    };
    this.get('recommendations').pushObject(recommendation);
    return recommendation;
  },

  /**
   * Remove recommendation
   * based on unique identifiers
   *
   * @param {string} name
   * @param {string} fileName
   * @param {string} configGroupName
   */
  removeRecommendation: function (name, fileName, configGroupName) {
    this.removeRecommendationObject(this.getRecommendation(name, fileName, configGroupName));
  },

  /**
   * Remove recommended Object
   *
   * @param {recommendation} recommendation
   */
  removeRecommendationObject: function (recommendation) {
    Em.assert('recommendation should be defined object', recommendation && typeof recommendation === 'object');
    this.get('recommendations').removeObject(recommendation);
  },

  /**
   * Update recommended object
   *
   * @param recommendation
   * @param recommendedValue
   * @param parentPropertyIds
   * @returns {*|recommendation|null}
   */
  updateRecommendation: function (recommendation, recommendedValue, parentPropertyIds) {
    Em.assert('recommendation should be defined object', recommendation && typeof recommendation === 'object');
    Em.set(recommendation, 'recommendedValue', recommendedValue);
    if (parentPropertyIds && parentPropertyIds.length) {
      var mergedProperties = parentPropertyIds.concat(Em.get(recommendation, 'parentConfigs') || []).uniq();
      Em.set(recommendation, 'parentConfigs', mergedProperties);
    }
    return recommendation;
  },

  /**
   *
   * @param recommendation
   * @param saveRecommendation
   */
  saveRecommendation: function (recommendation, saveRecommendation) {
    Em.assert('recommendation should be defined object', recommendation && typeof recommendation === 'object');
    if (recommendation.saveRecommended !== saveRecommendation) {
      Em.setProperties(recommendation, {
        'saveRecommended': !!saveRecommendation,
        'saveRecommendedDefault': !!saveRecommendation
      });
      return true;
    }
    return false;
  },

  /**
   * Get single recommendation
   *
   * @param name
   * @param fileName
   * @param configGroupName
   * @returns {recommendation|null}
   */
  getRecommendation: function (name, fileName, configGroupName) {
    Em.assert('name and fileName should be defined', name && fileName);
    return this.get('recommendations').find(function (dcv) {
      return dcv.propertyName === name
        && dcv.propertyFileName === App.config.getConfigTagFromFileName(fileName)
        && dcv.configGroup === (configGroupName || "Default");
    }) || null;
  },

  /**
   * Clear recommendations that are
   * same as initial value
   *
   * @method cleanUpRecommendations
   */
  cleanUpRecommendations: function () {
    var cleanDependentList = this.get('recommendations').filter(function (d) {
      var service = this.get('stepConfigs').findProperty('serviceName', d.serviceName);
      var serviceConfigs = service && service.get('configs') || [];
      var configId = App.config.configId(d.propertyName, d.propertyFileName);
      var config = serviceConfigs.findProperty('id', configId);
      return !((Em.isNone(d.initialValue) && Em.isNone(d.recommendedValue)) || d.initialValue == d.recommendedValue) &&
        (!config || Em.get(config, 'isNotDefaultValue'));
    }, this);
    this.set('recommendations', cleanDependentList);
  },

  /**
   * Remove all recommendations
   *
   * @method clearAllRecommendations
   */
  clearAllRecommendations: function () {
    this.set('recommendations', []);
  },

  /**
   * Clear values for dependent configs for given services
   *
   * @method clearRecommendationsByServiceName
   */
  clearRecommendationsByServiceName: function (serviceNames) {
    var filteredRecommendations = this.get('recommendations').reject(function (c) {
      return serviceNames.contains(c.serviceName);
    }, this);
    this.set('recommendations', filteredRecommendations);
  }

});
