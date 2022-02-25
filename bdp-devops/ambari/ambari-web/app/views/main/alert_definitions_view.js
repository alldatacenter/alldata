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
var sort = require('views/common/sort_view');

App.MainAlertDefinitionsView = App.TableView.extend({

  templateName: require('templates/main/alerts'),

  content: [],

  contentObs: function () {
    Em.run.once(this, this.contentObsOnce);
  }.observes('controller.content.[]', 'App.router.clusterController.isAlertsLoaded'),

  contentObsOnce: function() {
    var content = this.get('controller.content') && App.get('router.clusterController.isAlertsLoaded') ?
      this.get('controller.content').toArray() : [];
    if (this.get('childViews').someProperty('name', 'SortWrapperView')) {
      content = this.get('childViews').findProperty('name', 'SortWrapperView').getSortedContent(content);
    }
    this.set('content', content);
  },

  willInsertElement: function () {
    if (!this.get('controller.showFilterConditionsFirstLoad')) {
      this.clearFilterConditionsFromLocalStorage();
    }
    // on load alters should be sorted by status
    var controllerName = this.get('controller.name'),
      savedSortConditions = App.db.getSortingStatuses(controllerName) || [];
    if (savedSortConditions.everyProperty('status', 'sorting')) {
      savedSortConditions.push({
        name: "summary",
        status: "sorting_desc"
      });
      App.db.setSortingStatuses(controllerName, savedSortConditions);
    }
    this._super();
  },

  didInsertElement: function () {
    var self = this;
    Em.run.next(function () {
      self.set('isInitialRendering', false);
      self.contentObsOnce();
      self.tooltipsUpdater();
    });
  },

  willDestroyElement: function () {
    $(".timeago").tooltip('destroy');
    this.removeObserver('pageContent.length', this, 'tooltipsUpdater');
  },

  /**
   * Save <code>startIndex</code> to the localStorage
   * @method saveStartIndex
   */
  saveStartIndex: function() {
    App.db.setStartIndex(this.get('controller.name'), this.get('startIndex'));
  }.observes('startIndex'),

  /**
   * Clear <code>startIndex</code> from the localStorage
   * @method clearStartIndex
   */
  clearStartIndex: function () {
    App.db.setStartIndex(this.get('controller.name'), null);
  },

  /**
   * @type {number}
   */
  totalCount: Em.computed.alias('content.length'),

  colPropAssoc: ['', 'label', 'summary', 'serviceDisplayName', 'type', 'lastTriggered', 'enabled', 'groups'],

  sortView: sort.wrapperView,

  /**
   * Define whether initial view rendering has finished
   * @type {Boolean}
   */
  isInitialRendering: true,

  /**
   * Sorting header for <label>alertDefinition.label</label>
   * @type {Em.View}
   */
  nameSort: sort.fieldView.extend({
    column: 1,
    name: 'label',
    displayName: Em.I18n.t('alerts.table.header.definitionName')
  }),

  /**
   * Sorting header for <label>alertDefinition.status</label>
   * @type {Em.View}
   */
  statusSort: sort.fieldView.extend({
    column: 2,
    name: 'summary',
    displayName: Em.I18n.t('common.status'),
    type: 'alert_status',
    status: 'sorting_desc'
  }),

  /**
   * Sorting header for <label>alertDefinition.service.serviceName</label>
   * @type {Em.View}
   */
  serviceSort: sort.fieldView.extend({
    column: 3,
    name: 'serviceDisplayName',
    displayName: Em.I18n.t('common.service'),
    type: 'string'
  }),

  /**
   * Sorting header for <label>alertDefinition.type</label>
   * @type {Em.View}
   */
  typeSort: sort.fieldView.extend({
    column: 4,
    name: 'type',
    displayName: Em.I18n.t('common.type'),
    type: 'string'
  }),

  /**
   * Sorting header for <label>alertDefinition.lastTriggeredSort</label>
   * @type {Em.View}
   */
  lastTriggeredSort: sort.fieldView.extend({
    column: 5,
    name: 'lastTriggered',
    displayName: Em.I18n.t('alerts.table.header.lastTriggered'),
    type: 'number'
  }),

  /**
   * Sorting header for <label>alertDefinition.enabled</label>
   * @type {Em.View}
   */
  enabledSort: sort.fieldView.extend({
    template:Em.Handlebars.compile('<span {{bindAttr class="view.status :column-name"}}>{{t alerts.table.state}}</span>'),
    column: 6,
    name: 'enabled'
  }),

  /**
   * Filtered number of all content number information displayed on the page footer bar
   * @returns {String}
   */
  filteredContentInfo: Em.computed.i18nFormat('alerts.filters.filteredAlertsInfo', 'filteredCount', 'totalCount'),

  /**
   * Determines how display "back"-link - as link or text
   * @type {string}
   */
  paginationLeftClass: function () {
    if (this.get("startIndex") > 1) {
      return "paginate_previous";
    }
    return "paginate_disabled_previous";
  }.property("startIndex", 'filteredCount'),

  /**
   * Determines how display "next"-link - as link or text
   * @type {string}
   */
  paginationRightClass: function () {
    if (this.get("endIndex") < this.get("filteredCount")) {
      return "paginate_next";
    }
    return "paginate_disabled_next";
  }.property("endIndex", 'filteredCount'),

  /**
   * Show previous-page if user not in the first page
   * @method previousPage
   */
  previousPage: function () {
    if (this.get('paginationLeftClass') === 'paginate_previous') {
      this._super();
    }
    this.tooltipsUpdater();
  },

  /**
   * Show next-page if user not in the last page
   * @method nextPage
   */
  nextPage: function () {
    if (this.get('paginationRightClass') === 'paginate_next') {
      this._super();
    }
    this.tooltipsUpdater();
  },

  /**
   * Update tooltips when <code>pageContent</code> is changed
   * @method tooltipsUpdater
   */
  tooltipsUpdater: function () {
    Em.run.next(this, function () {
      App.tooltip($(".timeago"));
    });
  },

  updateFilter: function (iColumn, value, type) {
    if (!this.get('isInitialRendering')) {
      this._super(iColumn, value, type);
    }
    this.tooltipsUpdater();
  }

});
