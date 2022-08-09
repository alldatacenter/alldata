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

App.ServiceConfig = Ember.Object.extend({
  serviceName: '',
  configCategories: [],
  configCategoriesMap: function() {
    var categoriesMap = {};
    this.get('configCategories').forEach(function(c) {
      if (!categoriesMap[c.get('name')]) categoriesMap[c.get('name')] = c;
    });
    return categoriesMap;
  }.property('configCategories.[]'),
  configs: [],
  restartRequired: false,
  restartRequiredMessage: '',
  restartRequiredHostsAndComponents: {},
  configGroups: [],
  dependentServiceNames: [],
  initConfigsLength: 0, // configs length after initialization in order to watch changes

  errorCount: Em.computed.alias('configsWithErrors.length'),

  /**
   * Properties for which some aggregations should be calculated
   * like <code>configsWithErrors<code>, <code>changedConfigProperties<code> etc.
   *
   * @type {Object[]}
   */
  activeProperties: [],

  configsWithErrors: [],

  /**
   * Collection of properties that were changed:
   * for saved properties use - <code>isNotDefaultValue<code>
   * for not saved properties (on wizards, for new services) use
   *    - <code>isNotInitialValue<code>
   * for added properties use - <code>isNotSaved<code>
   * @type {Object[]}
   */
  changedConfigProperties: [],

  setActiveProperties: function() {
    Em.run.once(this, 'setActivePropertiesOnce');
  }.observes('configs.@each.isActive', 'configs.@each.isRequiredByAgent', 'configs.@each.value', 'configs.@each.isUndefinedLabel'),

  setActivePropertiesOnce: function() {
    if (this.get('isDestroyed')) return false;
    var activeProperties = this.get('configs').filter(function(c) {
      return (c.get('isActive') || c.get('isUndefinedLabel')) && (c.get('isRequiredByAgent') || c.get('isRequired'));
    });
    this.set('activeProperties', activeProperties);
  },

  setChangedConfigProperties: function() {
    Em.run.once(this, 'setChangedConfigPropertiesOnce');
  }.observes('activeProperties.@each.isNotDefaultValue', 'activeProperties.@each.isNotSaved', 'activeProperties.@each.isNotInitialValue'),

  setChangedConfigPropertiesOnce: function() {
    var changedConfigProperties = this.get('activeProperties').filter(function(c) {
      return c.get('isNotDefaultValue') || c.get('isNotSaved') || c.get('isNotInitialValue');
    }, this);
    this.set('changedConfigProperties', changedConfigProperties);
  },

  setConfigsWithErrors: function() {
    Em.run.once(this, 'setConfigsWithErrorsOnce');
  }.observes('activeProperties.@each.isValid', 'activeProperties.@each.isValidOverride', 'activeProperties.length'),

  setConfigsWithErrorsOnce: function() {
    var configsWithErrors = this.get('activeProperties').filter(function(c) {
      return (!c.get('isValid') || !c.get('isValidOverride'));
    });
    this.set('configsWithErrors', configsWithErrors);
  },

  observeErrors: function() {
    this.get('configCategories').setEach('errorCount', 0);
    this.get('configsWithErrors').forEach(function(c) {
      //configurations with widget shouldn't affect advanced category error counter
      if (this.get('configCategoriesMap')[c.get('category')] && !c.get('isInDefaultTheme')) {
        this.get('configCategoriesMap')[c.get('category')].incrementProperty('errorCount');
      }
    }, this);
  }.observes('configsWithErrors'),

  configTypes: function() {
    return App.StackService.find(this.get('serviceName')).get('configTypeList') || [];
  }.property('serviceName'),

  radioConfigs: Em.computed.filterBy('configs', 'displayType', 'radio button'),

  observeForeignKeys: function() {
    //TODO refactor or move this logic to other place
    Em.run.once(this, 'updateVisibilityByForeignKeys');
  }.observes('radioConfigs.@each.value'),

  updateVisibilityByForeignKeys: function() {
    var configs = this.get('configs');
    configs.forEach(function (item) {
      if (item.get('isVisible')) {
        var options = item.get('options');
        if (options && options.someProperty('foreignKeys')) {
          var options = options.filterProperty('foreignKeys');
          options.forEach(function (opt) {
            opt.foreignKeys.forEach(function (key) {
              var config = configs.findProperty('name', key);
              if (config) {
                config.set('isVisible', item.get('value') === opt.displayName);
              }
            });
          });
        }
      }
    });
  },

  /**
   * Config with overrides that has values that differs from saved
   *
   * @type {Object[]}
   */
  configsWithChangedOverrides: Em.computed.filterBy('activeProperties', 'isOverrideChanged', true),

  /**
   * Defines if some configs were added/removed
   * @type {boolean}
   */
  configsLengthWasChanged: Em.computed.notEqualProperties('configs.length', 'initConfigsLength'),

  /**
   * @type {boolean}
   */
  isPropertiesChanged: Em.computed.or(
    'configsLengthWasChanged',
    'changedConfigProperties.length',
    'configsWithChangedOverrides.length'),

  init: function() {
    this._super();
    this.set('dependentServiceNames', App.StackService.find(this.get('serviceName')).get('dependentServiceNames') || []);
    this.observeForeignKeys();
    this.setActiveProperties();
  },

  hasConfigIssues: Em.computed.someBy('activeProperties', 'hasIssues', true)
});

App.ConfigSiteTag = Ember.Object.extend({
  site: DS.attr('string'),
  tag: DS.attr('string'),
  /**
   * Object map of hostname->override-tag for overrides.
   * <b>Creators should set new object here.<b>
   */
  hostOverrides: null
});
