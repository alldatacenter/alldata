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

App.upgradeTaskView = Em.View.extend({
  templateName: require('templates/main/admin/stack_upgrade/upgrade_task'),

  /**
   * view observed directly
   * @type {?Em.View}
   */
  outsideView: null,

  /**
   * @type {boolean}
   */
  isExpanded: false,

  /**
   * @type {boolean}
   */
  errorLogOpened: false,

  /**
   * @type {boolean}
   */
  outputLogOpened: false,

  /**
   * @type {App.upgradeEntity}
   * @default null
   */
  content: null,

  /**
   * @type {Array}
   */
  tasks: [],

  /**
   * @type {string}
   */
  logTabId: Em.computed.format('{0}-log-tab', 'elementId'),

  /**
   * @type {string}
   */
  errorTabId:  Em.computed.format('{0}-error-tab', 'elementId'),

  /**
   * @type {string}
   */
  logTabIdLink: Em.computed.format('#{0}','logTabId'),

  /**
   * @type {string}
   */
  errorTabIdLInk: Em.computed.format('#{0}','errorTabId'),

  didInsertElement: function() {
    if (this.get('outsideView') && this.get('content')) {
      this.set('isExpanded', true);
      this.doPolling();
    }
  },

  toggleExpanded: function () {
    var isExpanded = this.get('isExpanded');
    this.toggleProperty('isExpanded', !isExpanded);
    if (!isExpanded) {
      this.doPolling();
    } else {
      this.set('content.isContentLoaded', true);
    }
  },

  /**
   *
   */
  doPolling: function () {
    var self = this;
    var task = this.get('content');
    var isExpanded = this.get('outsideView') ? this.get('outsideView.isDetailsOpened') : this.get('isExpanded');

    if (task && isExpanded) {
      this.get('controller').getUpgradeTask(task).complete(function () {
        task.set('isContentLoaded', true);
        if (!task.get('isCompleted')) {
          self.set('timer', setTimeout(function () {
            self.doPolling(task);
          }, App.bgOperationsUpdateInterval));
        }
      });
    } else {
      clearTimeout(this.get('timer'));
    }
  }.observes('outsideView.isDetailsOpened'),

  willDestroyElement: function () {
    clearTimeout(this.get('timer'));
  },

  /**
   * open error log in textarea to give ability to cope content
   * @param {object} event
   */
  copyErrLog: function(event) {
    this.toggleProperty('errorLogOpened');
  },

  /**
   * open stdout log in textarea to give ability to cope content
   * @param {object} event
   */
  copyOutLog: function(event) {
    this.toggleProperty('outputLogOpened');
  },

  /**
   * open error log in new window
   */
  openErrorLog: function () {
    this.openLogWindow(this.get('content.stderr'));
  },

  /**
   * open stdout log in new window
   */
  openOutLog: function () {
    this.openLogWindow(this.get('content.stdout'));
  },

  /**
   * open logs in new window
   * @param {string} log
   */
  openLogWindow: function(log) {
    var newWindow = window.open(),
      newDocument = newWindow.document,
      outputWrapper = newDocument.createElement('pre'),
      output = newDocument.createTextNode(log);
    outputWrapper.appendChild(output);
    newDocument.body.appendChild(outputWrapper);
    newDocument.close();
  }
});
