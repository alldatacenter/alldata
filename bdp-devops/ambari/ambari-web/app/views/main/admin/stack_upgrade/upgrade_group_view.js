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

App.upgradeGroupView = Em.View.extend({
  templateName: require('templates/main/admin/stack_upgrade/upgrade_group'),

  /**
   * customized view of progress bar that show completed/total counters
   * @class App.ProgressBarView
   */
  progressBarView: App.ProgressBarView.extend({
    classNames: ['progress-counter'],
    template: Ember.Handlebars.compile('<div class="progress-bar" {{bindAttr style="view.progressWidth"}}></div>' +
    '<div class="counters-label">{{view.completedTasks}}/{{view.totalTasks}}</div>')
  }),

  /**
   * Only one UpgradeGroup or UpgradeItem could be expanded at a time
   * @param {object} event
   */
  toggleExpanded: function (event) {
    var isExpanded = event.context.get('isExpanded');
    event.contexts[1].filterProperty('isExpanded').forEach(function (item) {
      this.collapseLowerLevels(item);
      item.set('isExpanded', false);
    }, this);
    this.collapseLowerLevels(event.context);
    event.context.set('isExpanded', !isExpanded);
    if (!isExpanded && event.context.get('type') === 'ITEM') {
      this.doPolling(event.context);
    }
  },

  /**
   * collapse sub-entities of current
   * @param {App.upgradeEntity} entity
   */
  collapseLowerLevels: function (entity) {
    if (entity.get('isExpanded')) {
      if (entity.type === 'ITEM') {
        entity.get('tasks').setEach('isExpanded', false);
      } else if (entity.type === 'GROUP') {
        entity.get('upgradeItems').forEach(function (item) {
          this.collapseLowerLevels(item);
          item.set('isExpanded', false);
        }, this);
      }
    }
  },

  /**
   * poll for tasks when item is expanded
   */
  doPolling: function (item) {
    var self = this;

    if (item && item.get('isExpanded')) {
      this.get('controller').getUpgradeItem(item).complete(function () {
        if (!item.get('isCompleted')) {
          self.set('timer', setTimeout(function () {
            self.doPolling(item);
          }, App.bgOperationsUpdateInterval));
        }
      });
    } else {
      clearTimeout(this.get('timer'));
    }
  },

  willDestroyElement: function () {
    clearTimeout(this.get('timer'));
  }
});
