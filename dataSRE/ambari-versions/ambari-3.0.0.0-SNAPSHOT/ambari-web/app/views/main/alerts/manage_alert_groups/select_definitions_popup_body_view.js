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

App.SelectDefinitionsPopupBodyView = App.TableView.extend({

  templateName: require('templates/main/alerts/add_definition_to_group_popup'),

  controllerBinding: 'App.router.manageAlertGroupsController',

  isPaginate: true,

  filteredContent: [],

  filteredContentObs: function () {
    Em.run.once(this, this.filteredContentObsOnce);
  }.observes('parentView.availableDefs.@each.filtered'),

  filteredContentObsOnce: function () {
    var filtered = this.get('parentView.availableDefs').filterProperty('filtered') || [];
    this.set('filteredContent', filtered);
  },

  /**
   * @type {boolean}
   */
  showOnlySelectedDefs: false,

  filterComponent: null,

  filterService: null,

  /**
   * @type {boolean}
   */
  isDisabled: Em.computed.not('parentView.isLoaded'),

  didInsertElement: function () {
    var initialDefs = this.get('initialDefs');
    initialDefs.setEach('filtered', true);
    this.set('parentView.availableDefs', initialDefs);
    this.set('parentView.isLoaded', true);
    this.filteredContentObsOnce();
  },

  /**
   * Default filter isn't needed
   */
  filter: Em.K,

  /**
   * filter definitions by componentName and serviceName
   */
  filterDefs: function () {
    var showOnlySelectedDefs = this.get('showOnlySelectedDefs');
    var filterComponent = this.get('filterComponent');
    var filterService = this.get('filterService');

    this.get('parentView.availableDefs').forEach(function (defObj) {
      var matchComponent = filterComponent ? (defObj.get('componentName') === filterComponent.get('componentName')) : true,
          matchService = filterService ? (defObj.get('serviceName') === filterService.get('serviceName')) : true,
          filtered = (matchComponent && matchService);

      defObj.set('filtered', showOnlySelectedDefs ? (filtered && defObj.get('selected')) : filtered);
    }, this);
    this.set('startIndex', 1);
  }.observes('parentView.availableDefs', 'filterService.serviceName', 'filterComponent.componentName', 'showOnlySelectedDefs'),

  defSelectMessage: function () {
    var defs = this.get('parentView.availableDefs');
    return this.t('alerts.actions.manage_alert_groups_popup.selectDefsDialog.selectedDefsLink')
           .format(defs.filterProperty('selected').get('length'), defs.get('length'));
  }.property('parentView.availableDefs.@each.selected'),

  /**
   * apply component filter
   * @param {object|null} event
   */
  selectFilterComponent: function (event) {
    if (event && event.context.get('componentName')) {
      var currentFilter = this.get('filterComponent');
      if (currentFilter) {
        currentFilter.set('selected', false);
      }
      if (currentFilter && currentFilter.get('componentName') === event.context.get('componentName')) {
        // selecting the same filter deselects it.
        this.set('filterComponent', null);
      } else {
        this.set('filterComponent', event.context);
        event.context.set('selected', true);
      }
    }
  },

  /**
   * apply service filter
   * @param {object|null} event
   */
  selectFilterService: function (event) {
    if (event && event.context.get('serviceName')) {
      var currentFilter = this.get('filterService');
      if (currentFilter) {
        currentFilter.set('selected', false);
      }
      if (currentFilter && currentFilter.get('serviceName') === event.context.get('serviceName')) {
        // selecting the same filter deselects it.
        this.set('filterService', null);
      } else {
        this.set('filterService', event.context);
        event.context.set('selected', true);
      }
    }
  },

  /**
   * Determines if all alert definitions are selected
   * @type {boolean}
   */
  allDefsSelected: false,

  /**
   * Inverse selection for alert definitions
   * @method toggleSelectAllDefs
   */
  toggleSelectAllDefs: function () {
    this.get('parentView.availableDefs').filterProperty('filtered').setEach('selected', this.get('allDefsSelected'));
  }.observes('allDefsSelected'),

  toggleShowSelectedDefs: function () {
    if (this.get('filterComponent')) this.get('filterComponent').set('selected', false);
    if (this.get('filterService')) this.get('filterService').set('selected', false);
    this.set('filterComponent', null);
    this.set('filterService', null);
    this.toggleProperty('showOnlySelectedDefs');
  }
});
