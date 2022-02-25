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
var dateUtils = require('utils/date/date');

App.LogTailView = Em.View.extend(App.InfiniteScrollMixin, {
  startIndex: 0,
  selectedTailCount: 50,
  autoResize: false,
  logRows: Em.A([]),
  totalCount: 0,
  totalItems: 0,
  lastLogTime: 0,
  isDataReady: false,
  refreshEnd: true,
  pollLogs: true,
  pollLogInterval: 2000,
  pollLogTimeoutId: null,

  oldLogsAvailable: false,
  oldLogsIsFetching: false,

  templateName: require('templates/common/log_tail'),

  content: null,

  isVisible: Em.computed.not('App.isClusterUser'),

  /**
   * elements size are:
   * .modal margin 40px x 2
   * .modal inner padding 15px x 2
   * .top-wrap-header height + margin 60px
   * .modal-footer 60px
   * .log-tail-popup-info 45px
   *
   * @property {number} resizeDelta
   */
  resizeDelta: 15*2 + 60 + 60 + 40 + 45,

  didInsertElement: function() {
    var self = this;
    this.infiniteScrollInit(this.$('.log-tail-content'), {
      appendHtml: '<span id="empty-span"></span>'
    });
    this.fetchRows({
      startIndex: 0
    }).then(function(data) {
      var logs = self.fetchRowsSuccess(data);
      self.infiniteScrollSetDataAvailable(true);
      self.appendLogRows(logs.reverse());
      self.saveLastTimestamp(logs);
      self.set('isDataReady', true);
      self.scrollToBottom();
      self.startLogPolling();
    });
    this.subscribeResize();
  },

  scrollToBottom: function() {
    Em.run.next(this, function() {
      this.$('.log-tail-content').scrollTop(this.$('.log-tail-content').prop('scrollHeight'));
    });
  },

  _infiniteScrollHandler: function(e) {
    var self = this;
    this._super(e);
    if ($(e.target).scrollTop() === 0) {
      if (this.get('noOldLogs') && !this.get('oldLogsIsFetching')) return;
      self.set('oldLogsIsFetching', true);
      this.fetchRows(this.oldestLogs()).then(function(data) {
        var oldestLog = self.get('logRows.0.logtime');
        var logRows = self.fetchRowsSuccess(data).filter(function(i) {
          return parseInt(i.get('logtime'), 10) < parseInt(oldestLog, 10);
        });
        if (logRows.length) {
          self.get('logRows').unshiftObjects(logRows.reverse());
        } else {
          self.set('noOldLogs', true);
        }
        self.set('oldLogsIsFetching', false);
      });
    }
  },

  willDestroyElement: function() {
    this._super();
    this.stopLogPolling();
    this.unsubscribeResize();
    this.get('logRows').clear();
  },

  resizeHandler: function() {
    // window.innerHeight * 0.1 = modal popup top 5% from both sides = 10%
    if (this.get('state') === 'destroyed') return;
    var newSize = $(window).height() - this.get('resizeDelta') - window.innerHeight*0.08;
    this.$().find('.log-tail-content.pre-styled').css('maxHeight', newSize + 'px');
  },

  unsubscribeResize: function() {
    if (!this.get('autoResize')) return;
    $(window).off('resize', this.resizeHandler.bind(this));
  },

  subscribeResize: function() {
    if (!this.get('autoResize')) return;
    this.resizeHandler();
    $(window).on('resize', this.resizeHandler.bind(this));
  },

  fetchRows: function(opts) {
    var options = $.extend({}, true, {
      startIndex: this.get('startIndex'),
      pageSize: this.get('selectedTailCount')
    }, opts || {});
    return App.ajax.send({
      sender: this,
      name: 'logtail.get',
      data: {
        hostName: this.get('content.hostName'),
        logComponentName: this.get('content.logComponentName'),
        pageSize: "" + options.pageSize,
        startIndex: "" + options.startIndex
      },
      error: 'fetchRowsError'
    });
  },

  fetchRowsSuccess: function(data) {
    var logRows = Em.getWithDefault(data, 'logList', []).map(function(logItem, i) {
      var item = App.keysUnderscoreToCamelCase(App.permit(logItem, ['log_message', 'logtime', 'level', 'id']));
      item.logtimeFormatted = dateUtils.dateFormat(parseInt(item.logtime, 10), 'YYYY-MM-DD HH:mm:ss,SSS');
      return Em.Object.create(item);
    });
    if (logRows.length === 0) {
      this.infiniteScrollSetDataAvailable(false);
      return [];
    }
    return logRows;
  },

  fetchRowsError: function() {},

  /** actions **/

  openInLogSearch: function() {},

  refreshContent: function() {
    var self = this,
        allIds;
    if (!this.get('refreshEnd')) {
      return $.Deferred().resolve().promise();
    }
    this.set('refreshEnd', false);
    //this.infiniteScrollSetDataAvailable(true);
    allIds = this.get('logRows').mapProperty('id');
    return this.fetchRows(this.currentPage()).then(function(data) {
      var logRows = self.fetchRowsSuccess(data).filter(function(i) {
        return parseInt(i.logtime, 10) > parseInt(self.get('lastLogTime'), 10) && !allIds.contains(i.get('id'));
      });
      if (logRows.length) {
        self.appendLogRows(logRows.reverse());
        self.saveLastTimestamp(logRows);
      }
      self.set('refreshEnd', true);
    });
  },

  appendLogRows: function(logRows) {
    this.get('logRows').pushObjects(logRows);
  },

  saveLastTimestamp: function(logRows) {
    this.set('lastLogTime', Em.getWithDefault(logRows, '0.logtime', 0));
    return this.get('lastLogTime');
  },

  currentPage: function() {
    return {
      startIndex: 0,
      pageSize: this.get('selectedTailCount')
    };
  },

  nextPage: function() {
    var newIndex = this.get('startIndex') + this.get('selectedTailCount');
    if (newIndex < 0) {
      newIndex = 0;
    }
    this.set('startIndex', newIndex);
    return {
      startIndex: newIndex,
      pageSize: this.get('selectedTailCount')
    };
  },

  oldestLogs: function() {
    return {
      startIndex: this.get('logRows.length'),
      pageSize: this.get('selectedTailCount')
    };
  },

  startLogPolling: function() {
    var self = this;
    if (!this.get('pollLogs') || this.get('state') === 'destroyed') return;
    this.set('pollLogTimeoutId', setTimeout(function() {
      self.stopLogPolling();
      self.refreshContent().then(self.startLogPolling.bind(self));
    }, this.get('pollLogInterval')));
  },

  stopLogPolling: function() {
    if (!this.get('pollLogs') || this.get('pollLogTimeoutId') === null) return;
    clearTimeout(this.get('pollLogTimeoutId'));
  }
});
