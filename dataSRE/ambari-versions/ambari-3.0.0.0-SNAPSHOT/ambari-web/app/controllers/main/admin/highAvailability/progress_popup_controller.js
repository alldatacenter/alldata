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

App.HighAvailabilityProgressPopupController = Ember.Controller.extend({

  name: 'highAvailabilityProgressPopupController',

  /**
   * Id of current request
   * @type {Array}
   */
  requestIds: [],

  /**
   * Title for popup header
   * @type {String}
   */
  popupTitle: '',

  /**
   * Array with Hosts tasks data used in <code>App.HostPopup</code>
   * @type {Array}
   */
  services: [],

  /**
   * Timestamp used in <code>App.HostPopup</code>
   * @type {Number}
   */
  serviceTimestamp: null,

  /**
   * Progress controller. Used to get tasks data.
   * @type {App.HighAvailabilityProgressPageController}
   */
  progressController: null,

  /**
   * Requests data with tasks
   * @type {Array}
   */
  hostsData: [],

  /**
   *  StageId for the command.
   *  @type {Number}
   */
  stageId: null,

  /**
   * During loading and calculations show popup with spinner
   * @type {Object}
   */
  spinnerPopup: null,

  isTaskPolling: false,

  taskInfo: null,

  /**
   * Get info for <code>requestIds</code> and initialize <code>App.HostPopup</code>
   * @param popupTitle {String}
   * @param requestIds {Array}
   * @param progressController {App.HighAvailabilityProgressPageController}
   * @param showSpinner {Boolean}
   * @param stageId {Number}
   */
  initPopup: function (popupTitle, requestIds, progressController, showSpinner, stageId) {
    if (showSpinner) {
      var loadingPopup = App.ModalPopup.show({
        header: Em.I18n.t('common.loading.eclipses'),
        primary: false,
        secondary: false,
        bodyClass: Ember.View.extend({
          template: Ember.Handlebars.compile('{{view App.SpinnerView}}')
        })
      });
      this.set('spinnerPopup', loadingPopup);
    }
    this.setProperties({
      progressController: progressController,
      popupTitle: popupTitle,
      requestIds: requestIds,
      hostsData: [],
      stageId: stageId
    });
    this.getHosts();
  },

  /**
   * Send AJAX request to get hosts tasks data
   */
  getHosts: function (successCallback) {
    var requestIds = this.get('requestIds');
    var stageId = this.get('stageId');
    var name = 'background_operations.get_by_request';
    if (!Em.isNone(stageId)) {
      name = 'common.request.polling';
      if (stageId === 0) {
        stageId = '0';
      }
    }
    if (Em.isNone(successCallback)) {
      successCallback = 'onGetHostsSuccess';
    }
    requestIds.forEach(function (requestId) {
      App.ajax.send({
        name: name,
        sender: this,
        data: {
          requestId: requestId,
          stageId: stageId
        },
        success: successCallback
      })
    }, this);
  },

  doPolling: function () {
    var self = this;
    this.set('progressController.logs', []);
    setTimeout(function () {
      self.getHosts('doPollingSuccessCallback');
    }, App.bgOperationsUpdateInterval);
  },

  doPollingSuccessCallback: function (data) {
    this.set('hostsData', [data]);
    var hostsData = this.get('hostsData');
    this.set('progressController.logs', data.tasks);
    if (this.isRequestRunning(hostsData)) {
      this.doPolling();
    }
  },

  /**
   * Callback for <code>getHosts</code> request
   * @param data
   */
  onGetHostsSuccess: function (data) {
    var hostsData = this.get('hostsData');
    hostsData.push(data);
    if (this.get('requestIds.length') === this.get('hostsData.length')) {
      var popupTitle = this.get('popupTitle');
      this.calculateHostsData(hostsData);
      App.HostPopup.initPopup(popupTitle, this, false, this.get("requestIds")[0]);
      if (this.isRequestRunning(hostsData)) {
        if (this.get('progressController.name') === 'mainAdminStackAndUpgradeController') {
          this.doPolling();
        }
        this.addObserver('progressController.logs.length', this, 'getDataFromProgressController');
      }
    }
    if (this.get('spinnerPopup')) {
      this.get('spinnerPopup').hide();
      this.set('spinnerPopup', null);
    }
  },

  /**
   * Convert data to format used in <code>App.HostPopup</code>
   * @param data {Array}
   */
  calculateHostsData: function (data) {
    var hosts = [];
    var hostsMap = {};
    var popupTitle = this.get('popupTitle');
    var id = this.get('requestIds')[0];

    data.forEach(function (request) {
      request.tasks.forEach(function (task) {
        var host = task.Tasks.host_name;
        if (hostsMap[host]) {
          hostsMap[host].logTasks.push(task);
        } else {
          hostsMap[host] = {
            name: task.Tasks.host_name,
            publicName: task.Tasks.host_name,
            logTasks: [task]
          };
        }
      });
    });
    for (var host in hostsMap) {
      hosts.push(hostsMap[host]);
    }
    this.set('services', [
      {id: id, name: popupTitle, hosts: hosts}
    ]);
    this.set('serviceTimestamp', App.dateTime());
    if (!this.isRequestRunning(data)) {
      this.removeObserver('progressController.logs.length', this, 'getDataFromProgressController');
    }
  },

  /**
   * Get hosts tasks data from <code>progressController</code>
   */
  getDataFromProgressController: function () {
    var data = this.get('hostsData');
    var tasksData = [];
    var stageId = this.get('stageId');
    // If the progress page is broken into stages then update host with only stage's tasks
    if (!!stageId) {
      tasksData = this.get('progressController.logs').filterProperty('Tasks.stage_id', stageId);
    } else {
      tasksData = this.get('progressController.logs');
    }
    if (tasksData.length) {
      var tasks = [];
      tasksData.forEach(function (logs) {
        tasks.pushObjects(logs);
      }, this);
      data.forEach(function (request) {
        tasks = tasks.filterProperty('Tasks.request_id', request.Requests.id);
        request.tasks = tasks;
      });
      this.calculateHostsData(data);
    }
  },

  /**
   * Identify whether request is running by task counters
   * @param requests {Array}
   * @return {Boolean}
   */
  isRequestRunning: function (requests) {
    var result = false;
    requests.forEach(function (request) {
      if ((request.Requests.task_count -
          (request.Requests.aborted_task_count +
           request.Requests.completed_task_count +
           request.Requests.failed_task_count +
           request.Requests.timed_out_task_count -
           request.Requests.queued_task_count)) > 0) {
        result = true;
      }
    });
    return result;
  },

  startTaskPolling: function (requestId, taskId) {
    this.setProperties({
      isTaskPolling: true,
      taskInfo: {
        id: taskId,
        requestId: requestId
      }
    });
    App.updater.run(this, 'updateTask', 'isTaskPolling', App.bgOperationsUpdateInterval);
    App.updater.immediateRun('updateTask');
  },

  stopTaskPolling: function () {
    this.set('isTaskPolling', false);
  },

  updateTask: function (callback) {
    App.ajax.send({
      name: 'background_operations.get_by_task',
      sender: this,
      data: {
        requestId: this.get('taskInfo.requestId'),
        taskId: this.get('taskInfo.id')
      },
      success: 'updateTaskSuccessCallback',
      callback: callback
    })
  },

  updateTaskSuccessCallback: function (data) {
    this.setProperties({
      'taskInfo.stderr': data.Tasks.stderr,
      'taskInfo.stdout': data.Tasks.stdout,
      'taskInfo.outputLog': data.Tasks.output_log,
      'taskInfo.errorLog': data.Tasks.error_log,
      'isTaskPolling': !['FAILED', 'COMPLETED', 'TIMEDOUT', 'ABORTED'].contains(data.Tasks.status)
    });
  }

});
