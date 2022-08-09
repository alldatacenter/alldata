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

App.SubSection = DS.Model.extend({

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
   * @type {boolean}
   */
  border: DS.attr('boolean', {defaultValue: false}),

  /**
   * @type {number}
   */
  rowIndex: DS.attr('number', {defaultValue: 1}),

  /**
   * @type {number}
   */
  columnIndex: DS.attr('number', {defaultValue: 1}),

  /**
   * @type {number}
   */
  rowSpan: DS.attr('number', {defaultValue: 1}),

  /**
   * @type {number}
   */
  columnSpan: DS.attr('number', {defaultValue: 1}),

  /**
   * @type {App.Section}
   */
  section: DS.belongsTo('App.Section'),

  /**
   * @type {String[]}
   */
  configProperties: DS.attr('array', {defaultValue: []}),

  /**
   * @type {App.SubSectionTab[]}
   */
  subSectionTabs: DS.hasMany('App.SubSectionTab'),


  dependsOn: DS.attr('array', {defaultValue: []}),

  /**
   * @type {boolean}
   */
  leftVerticalSplitter: DS.attr('boolean', {defaultValue: true}),

  /**
   * @type {App.ServiceConfigProperty[]}
   */
  configs: [],

  /**
   * @type {boolean}
   */
  hasTabs: Em.computed.bool('subSectionTabs.length'),

  someSubSectionTabIsVisible: Em.computed.someBy('subSectionTabs', 'isVisible', true),

  showTabs: Em.computed.and('hasTabs', 'someSubSectionTabIsVisible'),

  visibleProperties: function() {
    return this.get('configs').filter(function(c) {
      return c.get('isVisible') && !c.get('hiddenBySection');
    });
  }.property('configs.@each.isVisible', 'configs.@each.hiddenBySection'),

  visibleTabs: Em.computed.filterBy('subSectionTabs', 'isVisible', true),

  /**
   * Number of the errors in all configs
   * @type {number}
   */
  errorsCount: function () {
    var propertiesWithErrors = this.get('visibleProperties').filter(function(c) {
      return !c.get('isValid') || !c.get('isValidOverride');
    }).length;
    var tabsWithErrors = this.get('visibleTabs').mapProperty('errorsCount').reduce(Em.sum, 0);
    return propertiesWithErrors + tabsWithErrors;
  }.property('visibleProperties.@each.isValid', 'visibleProperties.@each.isValidOverride', 'visibleTabs.@each.errorsCount'),

  /**
   * @type {boolean}
   */
  addLeftVerticalSplitter: Em.computed.and('!isFirstColumn', 'leftVerticalSplitter'),

  /**
   * @type {boolean}
   */
  addRightVerticalSplitter: Em.computed.not('isLastColumn'),

  /**
   * @type {boolean}
   */
  showTopSplitter: Em.computed.and('!isFirstRow', '!border'),

  /**
   * @type {boolean}
   */
  isFirstRow: Em.computed.equal('rowIndex', 0),

  /**
   * @type {boolean}
   */
  isMiddleRow: function () {
    return this.get('rowIndex') != 0 && (this.get('rowIndex') + this.get('rowSpan') < this.get('section.sectionRows'));
  }.property('rowIndex', 'rowSpan', 'section.sectionRows'),

  /**
   * @type {boolean}
   */
  isLastRow: function () {
    return this.get('rowIndex') + this.get('rowSpan') == this.get('section.sectionRows');
  }.property('rowIndex', 'rowSpan', 'section.sectionRows'),

  /**
   * @type {boolean}
   */
  isFirstColumn: Em.computed.equal('columnIndex', 0),

  /**
   * @type {boolean}
   */
  isMiddleColumn: function () {
    return this.get('columnIndex') != 0 && (this.get('columnIndex') + this.get('columnSpan') < this.get('section.sectionColumns'));
  }.property('columnIndex', 'columnSpan', 'section.sectionColumns'),

  /**
   * @type {boolean}
   */
  isLastColumn: function () {
    return this.get('columnIndex') + this.get('columnSpan') == this.get('section.sectionColumns');
  }.property('columnIndex', 'columnSpan', 'section.sectionColumns'),

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
    var configs = this.get('configs').filter(function(c) {
      return !c.get('hiddenBySection') && c.get('isVisible');
    });
    return configs.length ? configs.everyProperty('isHiddenByFilter', true) && !this.get('someSubSectionTabIsVisible'): false;
  }.property('configs.@each.isHiddenByFilter'),

  /**
   * @type {boolean}
   */
  someConfigIsVisible: Em.computed.someBy('configs', 'isVisible', true),

  /**
   * Determines if subsection is visible
   * @type {boolean}
   */
  isSectionVisible: Em.computed.and('!isHiddenByFilter', '!isHiddenByConfig', 'someConfigIsVisible')

});


App.SubSection.FIXTURES = [];

