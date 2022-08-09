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

App.SubSectionTab = DS.Model.extend({

  id: DS.attr('string'),

  /**
   * @type {string}
   */
  name: DS.attr('string'),

  /**
   * theme from which this is coming from , eg: default, database, credentials, etc.
   */
  themeName: DS.attr('string'),

  /**
   * @type {string}
   */
  displayName: DS.attr('string'),

  /**
   * @type {App.Section}
   */
  subSection: DS.belongsTo('App.SubSection'),

  /**
   * @type {String[]}
   */
  configProperties: DS.attr('array', {defaultValue: []}),

  /**
   * @type {App.ServiceConfigProperty[]}
   */
  configs: [],


  dependsOn: DS.attr('array', {defaultValue: []}),

  /**
   * @type {boolean}
   */
  isActive: DS.attr('boolean', {defaultValue: false}),

  visibleProperties: function() {
    return this.get('configs').filter(function(c) {
      return c.get('isVisible') && !c.get('hiddenBySection');
    });
  }.property('configs.@each.isVisible', 'configs.@each.hiddenBySection'),

  /**
   * Number of the errors in all configs
   * @type {number}
   */
  errorsCount: function () {
    return this.get('visibleProperties').filter(function(config) {
      return !config.get('isValid') || !config.get('isValidOverride');
    }).length;
  }.property('visibleProperties.@each.isValid', 'visibleProperties.@each.isValidOverride'),

  /**
   * If the visibility of subsection is dependent on a value of some config
   */
  isHiddenByConfig: false,

  /**
   * Determines if subsection is filtered by checking it own configs
   * If there is no configs, subsection can't be hidden
   * @type {boolean}
   */
  isHiddenByFilter: function () {
    var configs = this.get('visibleProperties');
    return configs.length ? configs.everyProperty('isHiddenByFilter', true) : false;
  }.property('configs.@each.isHiddenByFilter').volatile(),

  /**
   * @type {boolean}
   */
  someConfigIsVisible: Em.computed.gt('visibleProperties.length', 0),

  /**
   * Determines if subsection is visible
   * @type {boolean}
   */
  isVisible: function() {
    return !this.get('isHiddenByFilter') && !this.get('isHiddenByConfig') && this.get('someConfigIsVisible');
  }.property('isHiddenByFilter', 'isHiddenByConfig', 'someConfigIsVisible').volatile()

});


App.SubSectionTab.FIXTURES = [];

