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

App.MainAlertDefinitionSearchBoxView = App.SearchBoxView.extend({

  /**
   * describe filter columns
   * @type {Array}
   * @const
   */
  keyFilterMap: [
    {
      label: Em.I18n.t('common.status'),
      key: 'summary',
      type: 'alert_status',
      column: 2
    },
    {
      label: Em.I18n.t('alerts.table.header.definitionName'),
      key: 'label',
      type: 'string',
      column: 1
    },
    {
      label: Em.I18n.t('common.service'),
      key: 'serviceDisplayName',
      type: 'select',
      column: 3
    },
    {
      label: Em.I18n.t('alerts.table.header.lastTriggered'),
      key: 'lastTriggered',
      type: 'date',
      column: 5
    },
    {
      label: Em.I18n.t('alerts.table.state'),
      key: 'enabled',
      type: 'enable_disable',
      column: 6
    },
    {
      label: Em.I18n.t('common.group'),
      key: 'groups',
      type: 'alert_group',
      column: 7
    }
  ],

  enabledDisabledMap: {
    'enabled': Em.I18n.t('alerts.table.state.enabled'),
    'disabled': Em.I18n.t('alerts.table.state.disabled')
  },

  lastTriggeredOptions: [
    'Past 1 hour',
    'Past 1 Day',
    'Past 2 Days',
    'Past 7 Days',
    'Past 14 Days',
    'Past 30 Days'
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
      case 'summary':
        this.getSummaryAvailableValues(facetValue, callback);
        break;
      case 'label':
        this.getLabelAvailableValues(facetValue, callback);
        break;
      case 'serviceDisplayName':
        this.getServiceAvailableValues(facetValue, callback);
        break;
      case 'lastTriggered':
        this.getTriggeredAvailableValues(facetValue, callback);
        break;
      case 'enabled':
        this.getEnabledAvailableValues(facetValue, callback);
        break;
      case 'groups':
        this.getGroupsAvailableValues(facetValue, callback);
        break;
    }
  },

  /**
   *
   * @param {string} facetValue
   * @param {Function} callback
   */
  getSummaryAvailableValues: function(facetValue, callback) {
    callback(this.rejectUsedValues(Object.keys(App.AlertDefinition.shortState), facetValue), {preserveOrder: true});
  },

  /**
   *
   * @param {string} facetValue
   * @param {Function} callback
   */
  getLabelAvailableValues: function(facetValue, callback) {
    callback(this.rejectUsedValues(App.AlertDefinition.find().mapProperty('label').uniq(), facetValue));
  },

  /**
   *
   * @param {string} facetValue
   * @param {Function} callback
   */
  getServiceAvailableValues: function(facetValue, callback) {
    callback(this.rejectUsedValues(App.AlertDefinition.find().mapProperty('serviceDisplayName').uniq(), facetValue));
  },

  /**
   *
   * @param {string} facetValue
   * @param {function} callback
   */
  getTriggeredAvailableValues: function(facetValue, callback) {
    callback(this.rejectUsedValues(this.get('lastTriggeredOptions'), facetValue), {preserveOrder: true});
  },

  /**
   *
   * @param {string} facetValue
   * @param {Function} callback
   */
  getEnabledAvailableValues: function(facetValue, callback) {
    callback(this.rejectUsedValues(Object.values(this.get('enabledDisabledMap')), facetValue), {preserveOrder: true});
  },

  /**
   *
   * @param {string} facetValue
   * @param {Function} callback
   */
  getGroupsAvailableValues: function(facetValue, callback) {
    const alertGroups = App.AlertGroup.find();
    const map = {};
    alertGroups.forEach((group) => {
      map[group.get('displayName')] = group.get('id');
    });
    this.set('groupsNameIdMap', map);
    callback(this.rejectUsedValues(alertGroups.mapProperty('displayName'), facetValue));
  },

  /**
   *
   * @param {string} category
   * @param {string} label
   */
  mapLabelToValue: function(category, label) {
    const enabledDisabledMap = this.get('enabledDisabledMap');
    const groupsNameIdMap = this.get('groupsNameIdMap');

    switch (category) {
      case 'enabled':
        return Object.keys(enabledDisabledMap)[Object.values(enabledDisabledMap).indexOf(label)];
      case 'groups':
        return groupsNameIdMap[label];
      default:
        return label;
    }
  }
});
