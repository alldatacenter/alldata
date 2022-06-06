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

App.MainConfigHistorySearchBoxView = App.SearchBoxView.extend({

  /**
   * @type {Array}
   * @const
   */
  modifiedOptions: [
    'Past 1 hour',
    'Past 1 Day',
    'Past 2 Days',
    'Past 7 Days',
    'Past 14 Days',
    'Past 30 Days'
  ],

  /**
   * describe filter columns
   * @type {Array}
   * @const
   */
  keyFilterMap: [
    {
      label: Em.I18n.t('common.service'),
      key: 'serviceVersion',
      type: 'select',
      column: 1
    },
    {
      label: Em.I18n.t('common.configGroup'),
      key: 'configGroup',
      type: 'select',
      column: 2
    },
    {
      label: Em.I18n.t('dashboard.configHistory.table.created.title'),
      key: 'createTime',
      type: 'range',
      column: 3
    },
    {
      label: Em.I18n.t('common.author'),
      key: 'author',
      type: 'string',
      column: 4
    },
    {
      label: Em.I18n.t('common.notes'),
      key: 'notes',
      type: 'string',
      column: 5
    }
  ],

  /**
   * populated dynamically in <code>getGroupsAvailableValues<code>
   * @type {object}
   */
  groupsNameIdMap: {},

  /**
   * 'valueMatches' callback for visualsearch.js
   * @param facetValue
   * @param searchTerm
   * @param callback
   */
  valueMatches: function (facetValue, searchTerm, callback) {
    this.showHideClearButton();
    switch (this.get('keyFilterMap').findProperty('label', facetValue).key) {
      case 'serviceVersion':
        this.getServiceVersionAvailableValues(facetValue, callback);
        break;
      case 'configGroup':
        this.getConfigGroupAvailableValues(facetValue, callback);
        break;
      case 'createTime':
        this.getCreateTimeAvailableValues(facetValue, callback);
        break;
      case 'author':
        this.getAuthorAvailableValues(facetValue, callback);
        break;
      case 'notes':
        this.getNotesAvailableValues(facetValue, callback);
        break;
    }
  },

  /**
   *
   * @param {string} facetValue
   * @param {Function} callback
   */
  getServiceVersionAvailableValues: function(facetValue, callback) {
    callback(this.rejectUsedValues(App.StackService.find().mapProperty('displayName'), facetValue), {preserveOrder: true});
  },

  /**
   *
   * @param {string} facetValue
   * @param {Function} callback
   */
  getConfigGroupAvailableValues: function(facetValue, callback) {
    const configGroups = App.ServiceConfigVersion.find().mapProperty('groupName').uniq().compact();
    callback(this.rejectUsedValues(configGroups, facetValue));
    this.requestFacetSuggestions(facetValue, callback);
  },

  /**
   *
   * @param {string} facetValue
   * @param {Function} callback
   */
  getCreateTimeAvailableValues: function(facetValue, callback) {
    callback(this.rejectUsedValues(this.get('modifiedOptions'), facetValue), {preserveOrder: true});
  },

  /**
   *
   * @param {string} facetValue
   * @param {Function} callback
   */
  getAuthorAvailableValues: function(facetValue, callback) {
    callback(this.rejectUsedValues(App.ServiceConfigVersion.find().mapProperty('author').uniq(), facetValue));
    this.requestFacetSuggestions(facetValue, callback);
  },

  /**
   *
   * @param {string} facetValue
   * @param {Function} callback
   */
  getNotesAvailableValues: function(facetValue, callback) {
    callback(this.rejectUsedValues(App.ServiceConfigVersion.find().mapProperty('notes').uniq(), facetValue));
    this.requestFacetSuggestions(facetValue, callback);
  },


  /**
   * request suggestions for facet from server
   * @param {string} facetValue
   * @param {Function} callback
   */
  requestFacetSuggestions: function (facetValue, callback) {
    const name = this.get('keyFilterMap').findProperty('label', facetValue).key;
    this.get('controller').getSearchBoxSuggestions(name).done((suggestions) => {
      if (suggestions.length) {
        callback(suggestions);
      }
    });
  },

  /**
   *
   * @param {string} category
   * @param {string} label
   */
  mapLabelToValue: function(category, label) {
    switch (category) {
      case 'createTime':
        return this.computeCreateTimeRange(label);
      case 'serviceVersion':
        return App.StackService.find().findProperty('displayName', label).get('serviceName');
      default:
        return label;
    }
  },

  /**
   *
   * @param {string} value
   * @returns {Array} [startTime, endTime]
   */
  computeCreateTimeRange: function(value) {
    let time = "";
    const curTime = App.dateTime();

    switch (value) {
      case 'Past 1 hour':
        time = curTime - 3600000;
        break;
      case 'Past 1 Day':
        time = curTime - 86400000;
        break;
      case 'Past 2 Days':
        time = curTime - 172800000;
        break;
      case 'Past 7 Days':
        time = curTime - 604800000;
        break;
      case 'Past 14 Days':
        time = curTime - 1209600000;
        break;
      case 'Past 30 Days':
        time = curTime - 2592000000;
        break;
      case 'Any':
        time = "";
        break;
    }
    return [time, ''];
  }
});
