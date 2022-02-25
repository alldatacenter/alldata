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
App.Poll = Em.Object.extend(App.ReloadPopupMixin, {
  name: '',
  stage: '',
  label: '',
  isVisible: true,
  isStarted: false,
  isPolling: true,
  clusterName: null,
  requestId: null,
  temp: false,
  progress: 0,
  url: null,
  testUrl: null,
  data: null,
  isError: false,
  isSuccess: false,
  POLL_INTERVAL: 4000,
  polledData: [],
  numPolls: 0,
  mockDataPrefix: '/data/wizard/deploy/5_hosts',
  currentTaskId: null,

  barWidth: Em.computed.format('width: {0}%;', 'progress'),

  isCompleted: Em.computed.or('isError', 'isSuccess'),

  showLink: Em.computed.or('isPolling', 'isStarted'),

  start: function () {
    if (Em.isNone(this.get('requestId'))) {
      this.setRequestId();
    } else {
      this.startPolling();
    }
  },

  setRequestId: function () {
    if (App.get('testMode')) {
      this.set('requestId', '1');
      this.doPolling();
      return;
    }
    var self = this;
    var url = this.get('url');
    var method = 'PUT';
    var data = this.get('data');

    $.ajax({
      type: method,
      url: url,
      data: data,
      dataType: 'text',
      timeout: App.timeout,
      success: function (data) {
        var jsonData = jQuery.parseJSON(data);
        if (Em.isNone(jsonData)) {
          self.set('isSuccess', true);
          self.set('isError', false);
        } else {
          var requestId = Em.get(jsonData, 'Requests.id');
          self.set('requestId', requestId);
          self.doPolling();
        }
      },

      error: function () {
        self.set('isError', true);
        self.set('isSuccess', false);
      },

      statusCode: require('data/statusCodes')
    });
  },

  /**
   * set current task id and send request
   * @param taskId
   */
  updateTaskLog: function (taskId) {
    this.set('currentTaskId', taskId);
    this.pollTaskLog();
  },

  doPolling: function () {
    if (!Em.isNone(this.get('requestId'))) {
      this.startPolling();
    }
  },

  /**
   * server call to obtain task logs
   */
  pollTaskLog: function () {
    if (!Em.isNone(this.get('currentTaskId'))) {
      App.ajax.send({
        name: 'background_operations.get_by_task',
        sender: this,
        data: {
          requestId: this.get('requestId'),
          taskId: this.get('currentTaskId')
        },
        success: 'pollTaskLogSuccessCallback'
      });
    }
  },

  /**
   * update logs of current task
   * @param data
   */
  pollTaskLogSuccessCallback: function (data) {
    var currentTask = this.get('polledData').findProperty('Tasks.id', data.Tasks.id);
    currentTask.Tasks.stdout = data.Tasks.stdout;
    currentTask.Tasks.stderr = data.Tasks.stderr;
    Em.propertyDidChange(this, 'polledData');
  },

  /**
   * start polling operation data
   * @return {Boolean}
   */
  startPolling: function () {
    if (Em.isNone(this.get('requestId'))) return false;

    this.pollTaskLog();
    App.ajax.send({
      name: 'background_operations.get_by_request',
      sender: this,
      data: {
        requestId: this.get('requestId'),
        callback: this.startPolling
      },
      success: 'reloadSuccessCallback',
      error: 'reloadErrorCallback'
    });
    return true;
  },

  reloadSuccessCallback: function (data) {
    var self = this;
    var result = this.parseInfo(data);
    this._super();
    if (!result) {
      window.setTimeout(function () {
        self.startPolling();
      }, this.POLL_INTERVAL);
    }
  },

  reloadErrorCallback: function (request, ajaxOptions, error, opt, params) {
    this._super(request, ajaxOptions, error, opt, params);
    if (request.status && !this.get('isSuccess')) {
      this.set('isError', true);
    }
  },

  stopPolling: function () {
    //this.set('isSuccess', true);
  },

  replacePolledData: function (polledData) {
    var currentTaskId = this.get('currentTaskId');
    if (!Em.isNone(currentTaskId)) {
      var task = this.get('polledData').findProperty('Tasks.id', currentTaskId);
      var currentTask = polledData.findProperty('Tasks.id', currentTaskId);
      if (task && currentTask) {
        currentTask.Tasks.stdout = task.Tasks.stdout;
        currentTask.Tasks.stderr = task.Tasks.stderr;
      }
    }
    this.set('polledData', polledData);
  },


  calculateProgressByTasks: function (tasksData) {
    var queuedTasks = tasksData.filterProperty('Tasks.status', 'QUEUED').length;
    var completedTasks = tasksData.filter(function (task) {
      return ['COMPLETED', 'FAILED', 'ABORTED', 'TIMEDOUT'].contains(task.Tasks.status);
    }).length;
    var inProgressTasks = tasksData.filterProperty('Tasks.status', 'IN_PROGRESS').length;
    return Math.ceil(((queuedTasks * 0.09) + (inProgressTasks * 0.35) + completedTasks ) / tasksData.length * 100)
  },

  isPollingFinished: function (polledData) {
    var runningTasks;
    runningTasks = polledData.filterProperty('Tasks.status', 'QUEUED').length;
    runningTasks += polledData.filterProperty('Tasks.status', 'IN_PROGRESS').length;
    runningTasks += polledData.filterProperty('Tasks.status', 'PENDING').length;
    if (runningTasks === 0) {
      if (polledData.everyProperty('Tasks.status', 'COMPLETED')) {
        this.set('isSuccess', true);
        this.set('isError', false);
      } else if (polledData.someProperty('Tasks.status', 'FAILED') || polledData.someProperty('Tasks.status', 'TIMEDOUT') || polledData.someProperty('Tasks.status', 'ABORTED')) {
        this.set('isSuccess', false);
        this.set('isError', true);
      }
      return true;
    } else {
      return false;
    }
  },


  parseInfo: function (polledData) {
    var tasksData = polledData.tasks;
    var requestId = this.get('requestId');
    if (polledData.Requests && !Em.isNone(polledData.Requests.id) && polledData.Requests.id != requestId) {
      // We don't want to use non-current requestId's tasks data to
      // determine the current install status.
      // Also, we don't want to keep polling if it is not the
      // current requestId.
      return false;
    }
    this.replacePolledData(tasksData);
    var totalProgress = this.calculateProgressByTasks(tasksData);
    this.set('progress', totalProgress.toString());
    return this.isPollingFinished(tasksData);
  }

});

