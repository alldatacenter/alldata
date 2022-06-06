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
var filters = require('views/common/filter_view'),
    sort = require('views/common/sort_view');

App.MainHostAlertsView = App.TableView.extend({
  templateName: require('templates/main/host/host_alerts'),

  content: function () {
    var criticalAlerts = [],
      warningAlerts = [],
      okAlerts = [],
      otherAlerts = [];
    var content = this.get('controller.content');
    if (content) {
      content.forEach(function (alert) {
        switch (alert.get('state')) {
          case 'CRITICAL':
            criticalAlerts.push(alert);
            break;
          case 'WARNING':
            warningAlerts.push(alert);
            break;
          case 'OK':
            okAlerts.push(alert);
            break;
          default:
            otherAlerts.push(alert);
        }
      });
      return [].concat(criticalAlerts, warningAlerts, okAlerts, otherAlerts);
    }
    return [];
  }.property('controller.content.[]'),

  willInsertElement: function () {
    var hostName = this.get('parentView.controller.content.hostName');
    App.router.get('mainAlertInstancesController').loadAlertInstancesByHost(hostName);
    App.router.set('mainAlertInstancesController.isUpdating', true);

    // on load alters should be sorted by state
    var controllerName = this.get('controller.name'),
      savedSortConditions = App.db.getSortingStatuses(controllerName) || [];
    if (savedSortConditions.everyProperty('status', 'sorting')) {
      savedSortConditions.push({
        name: "state",
        status: "sorting_asc"
      });
      App.db.setSortingStatuses(controllerName, savedSortConditions);
    }

    this._super();
  },

  didInsertElement: function () {
    this.tooltipsUpdater();
  },

  /**
   * @type {number}
   */
  totalCount: Em.computed.alias('content.length'),

  colPropAssoc: ['', 'serviceName', 'label', 'latestTimestamp', 'state', 'text'],

  sortView: sort.wrapperView,

  /**
   * Sorting header for <label>alertDefinition.label</label>
   * @type {Em.View}
   */
  nameSort: sort.fieldView.extend({
    column: 2,
    name: 'label',
    displayName: Em.I18n.t('alerts.definition.name')
  }),

  /**
   * Sorting header for <label>alertDefinition.status</label>
   * @type {Em.View}
   */
  statusSort: sort.fieldView.extend({
    column: 4,
    name: 'state',
    displayName: Em.I18n.t('common.status'),
    type: 'select'
  }),

  /**
   * Sorting header for <label>alertDefinition.service.serviceName</label>
   * @type {Em.View}
   */
  serviceSort: sort.fieldView.extend({
    column: 1,
    name: 'serviceName',
    displayName: Em.I18n.t('common.service'),
    type: 'string'
  }),

  /**
   * Sorting header for <label>alertDefinition.label</label>
   * @type {Em.View}
   */
  textSort: sort.fieldView.extend({
    column: 5,
    name: 'text',
    displayName: Em.I18n.t('alerts.table.header.check.response')
  }),


  /**
   * Filtering header for <label>alertDefinition.label</label>
   * @type {Em.View}
   */
  nameFilterView: filters.createTextView({
    column: 2,
    fieldType: 'filter-input-width',
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'string');
    }
  }),

  /**
   * Filtering header for <label>alertDefinition.status</label>
   * @type {Em.View}
   */
  stateFilterView: filters.createSelectView({
    column: 4,
    fieldType: 'filter-input-width',
    content: [
      {
        value: '',
        label: Em.I18n.t('common.all')
      },
      {
        value: 'OK',
        label: 'OK'
      },
      {
        value: 'WARNING',
        label: 'WARNING'
      },
      {
        value: 'CRITICAL',
        label: 'CRITICAL'
      },
      {
        value: 'UNKNOWN',
        label: 'UNKNOWN'
      }
    ],
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'select');
    }
  }),

  /**
   * Filtering header for <label>alertDefinition.service.serviceName</label>
   * @type {Em.View}
   */
  serviceFilterView: filters.createSelectView({
    column: 1,
    fieldType: 'filter-input-width',
    content: function () {
      return [
        {
          value: '',
          label: Em.I18n.t('common.all')
        }
      ].concat(App.Service.find().map(function (service) {
            return {
              value: service.get('serviceName'),
              label: service.get('displayName')
            }
          })).concat({
            value: 'AMBARI',
            label: Em.I18n.t('app.name')
          });
    }.property('App.router.clusterController.isLoaded'),
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'select');
    }
  }),

  /**
   * Filtering header for <label>alertDefinition.service.serviceName</label>
   * @type {Em.View}
   */
  textView: filters.createTextView({
    column: 5,
    fieldType: 'filter-input-width',
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'string');
    }
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
    return this.get('startIndex') > 1 ? 'paginate_previous' : 'paginate_disabled_previous';
  }.property('startIndex', 'filteredCount'),

  /**
   * Determines how display "next"-link - as link or text
   * @type {string}
   */
  paginationRightClass: function () {
    return this.get('endIndex') < this.get('filteredCount') ? 'paginate_next' : 'paginate_disabled_next';
  }.property('endIndex', 'filteredCount'),

  /**
   * Show previous-page if user not in the first page
   * @method previousPage
   */
  previousPage: function () {
    if (this.get('paginationLeftClass') === 'paginate_previous') {
      this._super();
    }
  },

  /**
   * Show next-page if user not in the last page
   * @method nextPage
   */
  nextPage: function () {
    if (this.get('paginationRightClass') === 'paginate_next') {
      this._super();
    }
  },

  /**
   * Update tooltips when <code>pageContent</code> is changed
   * @method tooltipsUpdater
   */
  tooltipsUpdater: function () {
    Em.run.once(this,this.tooltipsUpdaterOnce);
  }.observes('pageContent.[]'),

  tooltipsUpdaterOnce: function() {
    var self = this;
    Em.run.next(this, function () {
      App.tooltip(self.$('.timeago, .alert-text'));
    });
  },

  /**
   * Run <code>clearFilter</code> in the each child filterView
   */
  clearFilters: function() {
    this.set('filterConditions', []);
    this.get('childViews').forEach(function(childView) {
      Em.tryInvoke(childView, 'clearFilter');
    });
  },

  /**
   * Tooltips should be removed if some filter is applied or cleared
   *
   * @method clearTooltips
   */
  clearTooltips: function () {
    var $elements = this.$('.timeago, .alert-text');
    if ($elements) {
      $elements.tooltip('destroy');
    }
  }.observes('filteredCount'),

  willDestroyElement: function() {
    this.clearTooltips();
  }

});
