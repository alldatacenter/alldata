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

App.Section = DS.Model.extend({

  id: DS.attr('string'),

  /**
   * @type {string}
   */
  name: DS.attr('string'),

  /**
   * @type {string}
   */
  displayName: DS.attr('string'),

  /**
   * @type {number}
   */
  rowIndex: DS.attr('number', {defaultValue: 1}),

  /**
   * @type {number}
   */
  rowSpan: DS.attr('number', {defaultValue: 1}),

  /**
   * @type {number}
   */
  columnIndex: DS.attr('number', {defaultValue: 1}),

  /**
   * @type {number}
   */
  columnSpan: DS.attr('number', {defaultValue: 1}),

  /**
   * @type {number}
   */
  sectionColumns: DS.attr('number', {defaultValue: 1}),

  /**
   * @type {number}
   */
  sectionRows: DS.attr('number', {defaultValue: 1}),

  /**
   * @type {App.SubSection[]}
   */
  subSections: DS.hasMany('App.SubSection'),

  /**
   * @type {App.Tab}
   */
  tab: DS.belongsTo('App.Tab'),

  /**
   * Number of the errors in all subsections in the current section
   * @type {number}
   */
  errorsCount: function () {
    var errors = this.get('subSections').filterProperty('isSectionVisible').mapProperty('errorsCount');
    return errors.length ? errors.reduce(Em.sum, 0) : 0;
  }.property('subSections.@each.errorsCount', 'subSections.@each.isSectionVisible'),

  /**
   * @type {boolean}
   */
  isFirstRow: Em.computed.equal('rowIndex', 0),

  /**
   * @type {boolean}
   */
  isMiddleRow: function () {
    return this.get('rowIndex') != 0 && (this.get('rowIndex') + this.get('rowSpan') < this.get('tab.rows'));
  }.property('rowIndex', 'rowSpan', 'tab.rows'),

  /**
   * @type {boolean}
   */
  isLastRow: function () {
    return this.get('rowIndex') + this.get('rowSpan') == this.get('tab.rows');
  }.property('rowIndex', 'rowSpan', 'tab.rows'),

  /**
   * @type {boolean}
   */
  isFirstColumn: Em.computed.equal('columnIndex', 0),

  /**
   * @type {boolean}
   */
  isMiddleColumn: function () {
    return this.get('columnIndex') != 0 && (this.get('columnIndex') + this.get('columnSpan') < this.get('tab.columns'));
  }.property('columnIndex', 'columnSpan', 'tab.columns'),

  /**
   * @type {boolean}
   */
  isLastColumn: function () {
    return this.get('columnIndex') + this.get('columnSpan') == this.get('tab.columns');
  }.property('columnIndex', 'columnSpan', 'tab.columns'),

  /**
   * Determines if section is filtered out (all it's subsections should be hidden)
   * @type {boolean}
   */
  isHiddenByFilter: Em.computed.everyBy('subSections', 'isSectionVisible', false)

});


App.Section.FIXTURES = [];

