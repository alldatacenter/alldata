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
var filters = require('views/common/filter_view');
var sort = require('views/common/sort_view');

App.MainConfigHistoryView = App.TableView.extend(App.TableServerViewMixin, {
  templateName: require('templates/main/dashboard/config_history'),

  controllerBinding: 'App.router.mainConfigHistoryController',

  /**
   * @type {boolean}
   * @default false
   */
  filteringComplete: false,
  isInitialRendering: true,

  /**
   * return filtered number of all content number information displayed on the page footer bar
   * @returns {String}
   */
  filteredContentInfo: Em.computed.i18nFormat('tableView.filters.filteredConfigVersionInfo', 'filteredCount', 'totalCount'),

  willInsertElement: function () {
    var
      controllerName = this.get('controller.name'),
      savedSortConditions = App.db.getSortingStatuses(controllerName) || [];

    if (savedSortConditions.everyProperty('status', 'sorting')) {
      savedSortConditions.push({
        name: "createTime",
        status: "sorting_desc"
      });
      App.db.setSortingStatuses(controllerName, savedSortConditions);
    }
    if (!this.get('controller.showFilterConditionsFirstLoad')) {
      this.clearFilterConditionsFromLocalStorage();
    }
    this._super();
  },
  didInsertElement: function () {
    this.addObserver('startIndex', this, 'updatePagination');
    this.addObserver('displayLength', this, 'updatePagination');
    this.set('isInitialRendering', true);
    this.refresh(true);
    this.get('controller').subscribeToUpdates();
  },

  /**
   * stop polling after leaving config history page
   */
  willDestroyElement: function () {
    this.get('controller').unsubscribeOfUpdates();
  },

  sortView: sort.serverWrapperView,
  versionSort: sort.fieldView.extend({
    column: 1,
    name: 'serviceVersion',
    displayName: Em.I18n.t('dashboard.configHistory.table.version.title'),
    classNames: ['first']
  }),
  configGroupSort: sort.fieldView.extend({
    column: 2,
    name: 'configGroup',
    displayName: Em.I18n.t('dashboard.configHistory.table.configGroup.title')
  }),
  modifiedSort: sort.fieldView.extend({
    column: 3,
    name: 'createTime',
    displayName: Em.I18n.t('dashboard.configHistory.table.created.title')
  }),
  authorSort: sort.fieldView.extend({
    column: 4,
    name: 'author',
    displayName: Em.I18n.t('common.author')
  }),
  notesSort: sort.fieldView.extend({
    column: 5,
    name: 'notes',
    displayName: Em.I18n.t('common.notes')
  }),

  ConfigVersionView: Em.View.extend({
    tagName: 'tr',
    showLessNotes: true,
    toggleShowLessStatus: function () {
      this.toggleProperty('showLessNotes');
    },
    didInsertElement: function () {
      App.tooltip(this.$("[rel='Tooltip']"), {html: false});
    },

    // Define if show plain text label or link
    isServiceLinkDisabled: function () {
      return this.get('content.serviceName') === 'KERBEROS' && !App.Service.find().someProperty('serviceName', 'KERBEROS') || this.get('content.isConfigGroupDeleted');
    }.property('content.serviceName', 'content.isConfigGroupDeleted')
  }),

  /**
   * refresh table content
   * @param {boolean} shouldUpdateCounter
   */
  refresh: function (shouldUpdateCounter) {
    var self = this;
    this.set('filteringComplete', false);
    this.get('controller').load(shouldUpdateCounter).done(function () {
      self.refreshDone.apply(self);
    });
  },

  /**
   * callback executed after refresh call done
   * @method refreshDone
   */
  refreshDone: function () {
    this.set('isInitialRendering', false);
    this.set('filteringComplete', true);
    this.propertyDidChange('pageContent');
    this.set('controller.resetStartIndex', false);
    App.loadTimer.finish('Config History Page');
  },

  saveStartIndex: function() {
    App.db.setStartIndex(this.get('controller.name'), this.get('startIndex'));
  }.observes('startIndex'),

  filter: Em.K
});
