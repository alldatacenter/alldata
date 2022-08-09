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

/**
 * Mixin should be used for Em.View of table that uses server to filter, sort, paginate content
 */
App.TableServerViewMixin = Em.Mixin.create({
  filteringComplete: true,
  timeOut: null,
  /**
   * filter delay time, used to combine filters change into one content update
   */
  filterWaitingTime: 500,

  /**
   * count of filtered items
   */
  filteredCount: Em.computed.alias('controller.filteredCount'),
  /**
   * total count of items
   */
  totalCount: Em.computed.alias('controller.totalCount'),

  /**
   * data requested from server
   */
  content: Em.computed.alias('controller.content'),

  /**
   * content already filtered on server-side
   */
  filteredContent: Em.computed.alias('content'),
  /**
   * sort and slice recieved content by pagination parameters
   */
  pageContent: function () {
    var content = this.get('filteredContent').toArray();
    if (content.length > ((this.get('endIndex') - this.get('startIndex')) + 1)) {
      content = content.slice(0, (this.get('endIndex') - this.get('startIndex')) + 1);
    }
    return content.sort(function (a, b) {
      return a.get('index') - b.get('index');
    });
  }.property('filteredContent.length'),

  /**
   * compute applied filter and run content update from server
   * @param iColumn
   * @param value
   * @param type
   */
  updateFilter: function (iColumn, value, type) {
    // Do not even trigger update flow if it's a blank update
    if (this.isBlankFilterUpdate(iColumn, value, type)) { return; }

    var self = this;
    this.set('controller.resetStartIndex', false);
    if (!this.get('filteringComplete')) {
      clearTimeout(this.get('timeOut'));
      this.set('timeOut', setTimeout(function () {
        self.updateFilter(iColumn, value, type);
      }, this.get('filterWaitingTime')));
    } else {
      clearTimeout(this.get('timeOut'));
      this.set('controller.resetStartIndex', true);
      //save filter only when it's applied
      this.saveFilterConditions(iColumn, value, type, false);
      this.refresh();
    }
    return true;
  },

  updateComboFilter: function(filterConditions) {
    clearTimeout(this.get('timeOut'));
    this.set('controller.resetStartIndex', true);
    this.set('filterConditions', filterConditions);
    this.saveAllFilterConditions();
    this.refresh();
  },

  /**
   * 1) Has previous saved filter
   * 2) Value to update is empty
   * 3) Value to update is same as before
   * Returns true if (!1&&2 || 1&&3)
   * @param iColumn
   * @param value
   * @param type
   * @returns {boolean|*}
   */
  isBlankFilterUpdate: function(iColumn, value, type) {
    var result = false;
    var filterConfitions = this.get('filterConditions');
    var filterCondition = filterConfitions? filterConfitions.findProperty('iColumn', iColumn) : null;
    if ((!filterCondition && Em.isEmpty(value))
    || (filterCondition && filterCondition.value == value)
    || (filterCondition && typeof filterCondition.value == 'object'
        && JSON.stringify(filterCondition.value) == JSON.stringify(value))) {
      result = true;
    }
    return result;
  },

  /**
   * success callback for updater request
   */
  updaterSuccessCb: function () {
    this.set('filteringComplete', true);
    this.propertyDidChange('pageContent');
    App.loadTimer.finish('Hosts Page');
  },

  /**
   * error callback for updater request
   */
  updaterErrorCb: function () {
    this.set('requestError', arguments);
  },

  /**
   * synchronize properties of view with controller to generate query parameters
   */
  updatePagination: function (key) {
    if (!Em.isNone(this.get('displayLength'))) {
      App.db.setDisplayLength(this.get('controller.name'), this.get('displayLength'));
    }
    if (!Em.isNone(this.get('startIndex'))) {
      App.db.setStartIndex(this.get('controller.name'), this.get('startIndex'));
    }

    if (key !== 'SKIP_REFRESH') {
      this.refresh();
    }
  },

  /**
   * Placeholder for `saveStartIndex`
   */
  saveStartIndex: Em.K,

  /**
   * when new filter applied page index should be reset to first page
   */
  resetStartIndex: function () {
    if (this.get('controller.resetStartIndex') && this.get('filteredCount') > 0) {
      this.set('startIndex', 1);
      this.saveStartIndex();
      this.updatePagination('SKIP_REFRESH');
    }
  }.observes('controller.resetStartIndex')
});
