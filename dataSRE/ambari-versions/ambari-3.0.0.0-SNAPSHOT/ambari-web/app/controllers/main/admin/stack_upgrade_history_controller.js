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
var stringUtils = require('utils/string_utils');

App.MainAdminStackUpgradeHistoryController = Em.ArrayController.extend({
  name: 'mainAdminStackUpgradeHistoryController',

  startIndex: 1,

  resetStartIndex: false,

  /**
   * status of tasks/items/groups which should be grayed out and disabled
   * @type {Array}
   */
  nonActiveStates: ['PENDING'],

  /**
   * mutable properties of Upgrade Task
   * @type {Array}
   */
  taskDetailsProperties: ['status', 'stdout', 'stderr', 'error_log', 'host_name', 'output_log'],

  /**
   * Current upgrade record clicked on the UI
   * @type {App.StackUpgradeHistory|null}
   * */
  currentUpgradeRecord: null,

  isDowngrade: false,

  upgradeData: null,

  /**
   * List of all <code>App.StackUpgradeHistory</code>. Latest one at the beginning
   * @type {App.StackUpgradeHistory[]}
   */
  content: App.StackUpgradeHistory.find(),

  upgradeHistoryUrl: function() {
    return App.get('apiPrefix') + '/clusters/' + App.get('clusterName') + '/upgrades?fields=Upgrade';
  }.property('App.clusterName'),

  upgradeUpgradeRecordUrl: function() {
    var record = this.get('currentUpgradeRecord');
    return App.get('apiPrefix') + '/clusters/' + App.get('clusterName') + '/upgrades/' + record.get('requestId');
  }.property('App.clusterName', 'currentUpgradeRecord'),

  loadStackUpgradeHistoryToModel: function () {
    console.log('Load stack upgrade history');
    var dfd = $.Deferred();
    App.HttpClient.get(this.get('upgradeHistoryUrl'), App.stackUpgradeHistoryMapper, {
      complete: function () {
        dfd.resolve();
      }
    });
    return dfd.promise();
  },

  loadStackUpgradeRecord: function () {
    var record = this.get('currentUpgradeRecord');
    this.set('isDowngrade', ('DOWNGRADE' == record.get('direction')))
    var dfd = $.Deferred();
    var self = this;
    if (record != null) {
      App.ajax.send({
        name: 'admin.upgrade.data',
        sender: this,
        data: {
          id: record.get('requestId')
        },
        success: 'loadUpgradeRecordSuccessCallback'
      }).then(dfd.resolve).always(function () {
      });
    } else {
      dfd.resolve();
    }
    return dfd.promise();
  },

  loadUpgradeRecordSuccessCallback: function(newData){
    if (Em.isNone(newData)) {
      var record = this.get('currentUpgradeRecord');
      console.debug('No data returned for upgrad record ' + record.get('requestId'))
      return;
    }
    var upgradeGroups = [];
    if (newData.upgrade_groups) {
      var nonActiveStates = this.get('nonActiveStates');
      //wrap all entities into App.finishedUpgradeEntity
      newData.upgrade_groups.forEach(function (newGroup) {
      var hasExpandableItems = newGroup.upgrade_items.some(function (item) {
            return !nonActiveStates.contains(item.UpgradeItem.status);
          }),
          oldGroup = App.finishedUpgradeEntity.create({type: 'GROUP', hasExpandableItems: hasExpandableItems}, newGroup.UpgradeGroup),
          upgradeItems = [];
        newGroup.upgrade_items.forEach(function (item) {
          var oldItem = App.finishedUpgradeEntity.create({type: 'ITEM'}, item.UpgradeItem);
          this.formatMessages(oldItem);
          oldItem.set('tasks', []);
          upgradeItems.pushObject(oldItem);
        }, this);
        upgradeItems.reverse();
        oldGroup.set('upgradeItems', upgradeItems);
        upgradeGroups.pushObject(oldGroup);
      }, this);
      upgradeGroups.reverse();
      this.set('upgradeData', Em.Object.create({
        upgradeGroups: upgradeGroups,
        upgrade_groups: newData.upgrade_groups,
        Upgrade: newData.Upgrade
      }));
    }
  },

  /**
   * format upgrade item text
   * @param {App.finishedUpgradeEntity} oldItem
   */
  formatMessages: function (oldItem) {
    var text = oldItem.get('text');
    var messages = [];

    try {
      var messageArray = JSON.parse(text);
      for (var i = 0; i < messageArray.length; i++) {
        messages.push(messageArray[i].message);
      }
      oldItem.set('text', messages.join(' '));
    } catch (err) {
      console.warn('Upgrade Item has malformed text');
    }
    oldItem.set('messages', messages);
  },

  /**
   * request Upgrade Item and its tasks from server
   * @param {Em.Object} item
   * @param {Function} customCallback
   * @return {$.ajax}
   */
  getUpgradeItem: function (item) {
    return App.ajax.send({
      name: 'admin.upgrade.upgrade_item',
      sender: this,
      data: {
        upgradeId: item.get('request_id'),
        groupId: item.get('group_id'),
        stageId: item.get('stage_id')
      },
      success: 'getUpgradeItemSuccessCallback'
    });
  },

  /**
   * success callback of <code>getTasks</code>
   * @param {object} data
   */
  getUpgradeItemSuccessCallback: function (data) {
    this.get('upgradeData.upgradeGroups').forEach(function (group) {
      if (group.get('group_id') === data.UpgradeItem.group_id) {
        group.get('upgradeItems').forEach(function (item) {
          if (item.get('stage_id') === data.UpgradeItem.stage_id) {
            if (item.get('tasks.length')) {
              data.tasks.forEach(function (task) {
                var currentTask = item.get('tasks').findProperty('id', task.Tasks.id);
                this.get('taskDetailsProperties').forEach(function (property) {
                  if (!Em.isNone(task.Tasks[property])) {
                    currentTask.set(property, task.Tasks[property]);
                  }
                }, this);
              }, this);
            } else {
              var tasks = [];
              data.tasks.forEach(function (task) {
                tasks.pushObject(App.finishedUpgradeEntity.create({
                  type: 'TASK',
                  group_id: data.UpgradeItem.group_id
                }, task.Tasks));
              });
              item.set('tasks', tasks);
            }
            item.set('isTasksLoaded', true);
          }
        }, this);
      }
    }, this);
  },

  /**
   * request Upgrade Task
   * @param {Em.Object} task
   * @return {$.ajax}
   */
  getUpgradeTask: function (task) {
    return App.ajax.send({
      name: 'admin.upgrade.upgrade_task',
      sender: this,
      data: {
        upgradeId: task.get('request_id'),
        groupId: task.get('group_id'),
        stageId: task.get('stage_id'),
        taskId: task.get('id'),
        task: task
      },
      success: 'getUpgradeTaskSuccessCallback'
    });
  },

  getUpgradeTaskSuccessCallback: function (data, xhr, params) {
    this.get('taskDetailsProperties').forEach(function (property) {
      params.task.set(property, data.Tasks[property]);
    }, this);
  },

  /**
   * status of Upgrade request
   * @type {string}
   */
  requestStatus: function () {
    if (this.get('upgradeData')){
      if (this.get('upgradeData.Upgrade')) {
        return this.get('upgradeData.Upgrade.request_status');
      } else {
        return '';
      }
    } else {
      return ''
    }
  }.property('upgradeData.Upgrade.request_status')
});
