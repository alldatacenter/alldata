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
var date = require('utils/date/date');

App.WizardStep9HostLogPopupBodyView = Em.View.extend({

  classNames: ['col-sm-12'],

  templateName: require('templates/wizard/step9/step9HostTasksLogPopup'),

  /**
   * Does host lost heartbeat
   * @type {bool}
   */
  isHeartbeatLost: Em.computed.equal('parentView.host.status', 'heartbeat_lost'),

  /**
   * Does host doesn't have scheduled tasks for install
   * @type {bool}
   */
  isNoTasksScheduled: Em.computed.alias('parentView.host.isNoTasksForInstall'),

  /**
   * Is log-box hidden
   * @type {bool}
   */
  isLogWrapHidden: true,

  /**
   * Is log-textarea visible
   * @type {bool}
   */
  showTextArea: false,

  /**
   * No tasks shown
   * @type {bool}
   */
  isEmptyList: true,

  /**
   * Is task logs loaded
   * @type {Boolean}
   */
  isTaskLoaded: true,

  /**
   * Checks if no visible tasks are in popup
   * @method visibleTasks
   */
  visibleTasks: function () {
    this.set("isEmptyList", true);
    if (this.get('category.value')) {
      var filter = this.get('category.value');
      var tasks = this.get('tasks');
      tasks.setEach("isVisible", false);

      if (filter == "all") {
        tasks.setEach("isVisible", true);
      }
      else if (filter == "pending") {
        tasks.filterProperty("status", "pending").setEach("isVisible", true);
        tasks.filterProperty("status", "queued").setEach("isVisible", true);
      }
      else if (filter == "in_progress") {
        tasks.filterProperty("status", "in_progress").setEach("isVisible", true);
      }
      else if (filter == "failed") {
        tasks.filterProperty("status", "failed").setEach("isVisible", true);
      }
      else if (filter == "completed") {
        tasks.filterProperty("status", "completed").setEach("isVisible", true);
      }
      else if (filter == "aborted") {
        tasks.filterProperty("status", "aborted").setEach("isVisible", true);
      }
      else if (filter == "timedout") {
        tasks.filterProperty("status", "timedout").setEach("isVisible", true);
      }

      if (tasks.filterProperty("isVisible", true).length > 0) {
        this.set("isEmptyList", false);
      }
    }
  }.observes('category', 'tasks'),

  /**
   * List categories (implements possible values for task status)
   * @type {Em.Object[]}
   */
  categories: [
    Em.Object.create({value: 'all', label: Em.I18n.t('installer.step9.hostLog.popup.categories.all') }),
    Em.Object.create({value: 'pending', label: Em.I18n.t('installer.step9.hostLog.popup.categories.pending')}),
    Em.Object.create({value: 'in_progress', label: Em.I18n.t('installer.step9.hostLog.popup.categories.in_progress')}),
    Em.Object.create({value: 'failed', label: Em.I18n.t('installer.step9.hostLog.popup.categories.failed') }),
    Em.Object.create({value: 'completed', label: Em.I18n.t('installer.step9.hostLog.popup.categories.completed') }),
    Em.Object.create({value: 'aborted', label: Em.I18n.t('installer.step9.hostLog.popup.categories.aborted') }),
    Em.Object.create({value: 'timedout', label: Em.I18n.t('installer.step9.hostLog.popup.categories.timedout') })
  ],

  /**
   * Current category
   * @type {Em.Object}
   */
  category: null,

  /**
   * List of tasks
   * @type {Em.Object[]}
   */
  tasks: function () {
    var tasksArr = [];
    var host = this.get('parentView.host');
    var tasks = this.getStartedTasks(host);
    tasks = tasks.sortProperty('Tasks.id');
    if (tasks.length) {
      tasks.forEach(function (_task) {
        var taskInfo = Em.Object.create({});
        taskInfo.set('id', _task.Tasks.id);
        taskInfo.set('requestId', _task.Tasks.request_id);
        taskInfo.set('command', _task.Tasks.command.toLowerCase() === 'service_check' ? '' : _task.Tasks.command.toLowerCase());
        taskInfo.set('commandDetail', App.format.commandDetail(_task.Tasks.command_detail, _task.Tasks.request_inputs, _task.Tasks.ops_display_name));
        taskInfo.set('status', App.format.taskStatus(_task.Tasks.status));
        taskInfo.set('role', App.format.role(_task.Tasks.role, false));
        taskInfo.set('stderr', _task.Tasks.stderr);
        taskInfo.set('stdout', _task.Tasks.stdout);
        taskInfo.set('outputLog', _task.Tasks.output_log);
        taskInfo.set('errorLog', _task.Tasks.error_log);
        taskInfo.set('startTime',  date.startTime(_task.Tasks.start_time));
        taskInfo.set('duration', date.durationSummary(_task.Tasks.start_time, _task.Tasks.end_time));
        taskInfo.set('isVisible', true);
        taskInfo.set('icon', '');
        taskInfo.set('hostName', _task.Tasks.host_name);
        if (taskInfo.get('status') == 'pending' || taskInfo.get('status') == 'queued') {
          taskInfo.set('icon', 'glyphicon glyphicon-cog');
        } else if (taskInfo.get('status') == 'in_progress') {
          taskInfo.set('icon', 'icon-cogs');
        } else if (taskInfo.get('status') == 'completed') {
          taskInfo.set('icon', 'glyphicon glyphicon-ok');
        } else if (taskInfo.get('status') == 'failed') {
          taskInfo.set('icon', 'glyphicon glyphicon-exclamation-sign');
        } else if (taskInfo.get('status') == 'aborted') {
          taskInfo.set('icon', 'glyphicon glyphicon-minus');
        } else if (taskInfo.get('status') == 'timedout') {
          taskInfo.set('icon', 'glyphicon glyphicon-time');
        }
        tasksArr.push(taskInfo);
      }, this);
    }
    this.set('isTaskLoaded', true);
    return tasksArr;
  }.property('parentView.c.logTasksChangesCounter'),

  /**
   * Navigate to task list from task view
   * @method backToTaskList
   */
  backToTaskList: function () {
    this.destroyClipBoard();
    this.set("isLogWrapHidden", true);
  },

  /**
   * Get list of host's started tasks
   * @param {object} host
   * @returns {object[]}
   * @method getStartedTasks
   */
  getStartedTasks: function (host) {
    return host.logTasks.filter(function (task) {
      return task.Tasks.status;
    });
  },

  /**
   * Open new window with task's log
   * @method openTaskLogInDialog
   */
  openTaskLogInDialog: function () {
    var newWindow = window.open();
    var newDocument = newWindow.document;
    newDocument.write('<pre>' + this.get('formattedLogsForOpenedTask') + '<pre>');
    newDocument.close();
  },

  /**
   * Currently open task
   * @type {Em.Object}
   */
  openedTask: function () {
    return this.get('tasks').findProperty('id', this.get('parentView.c.currentOpenTaskId'))
  }.property('parentView.c.currentOpenTaskId', 'tasks.[]'),

  /**
   * @type {string}
   */
  formattedLogsForOpenedTask: function () {
    var stderr = this.get('openedTask.stderr');
    var stdout = this.get('openedTask.stdout');
    return 'stderr: \n' + stderr + '\n stdout:\n' + stdout;
  }.property('openedTask.stderr', 'openedTask.stdout'),

  /**
   * Click-handler for toggle task's log view (textarea to box and back)
   * @param {object} event
   * @method toggleTaskLog
   */
  toggleTaskLog: function (event) {
    if (this.get('isLogWrapHidden')) {
      var taskInfo = event.context;
      this.set("isLogWrapHidden", false);
      this.set('parentView.c.currentOpenTaskId', taskInfo.id);
      this.set('parentView.c.currentOpenTaskRequestId', taskInfo.requestId);
      this.set('isTaskLoaded', false);
      this.get('parentView.c').loadCurrentTaskLog();
      $(".modal").scrollTop(0);
      $(".modal-body").scrollTop(0);
    }
    else {
      this.set("isLogWrapHidden", true);
      this.set('parentView.c.currentOpenTaskId', 0);
      this.set('parentView.c.currentOpenTaskRequestId', 0);
    }
  },

  /**
   * Create (if doesn't exist) or destroy (if exists) clipboard textarea
   * @method textTrigger
   */
  textTrigger: function () {
    if (this.get('showClipBoard')) {
      this.destroyClipBoard();
    }
    else {
      this.createClipBoard();
    }
  },

  /**
   * @type {boolean}
   */
  showClipBoard: false,

  /**
   * Create clipboard with task's log
   * @method createClipBoard
   */
  createClipBoard: function () {
    this.set('showClipBoard', true);
    $('.task-detail-log-maintext').css('display', 'none');
  },

  /**
   * Destroy clipboard with task's log
   * @method destroyClipBoard
   */
  destroyClipBoard: function () {
    this.set('showClipBoard', false);
    $('.task-detail-log-maintext').css('display', 'block');
  }
});
