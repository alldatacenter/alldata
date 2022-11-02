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

App.BackgroundOperationsController = Em.Controller.extend({
  name: 'backgroundOperationsController',

  runningOperationsCount : function() {
    return this.get('services').filterProperty('isRunning').length;
  }.property('services.@each.isRunning'),

  /**
   * List of requests
   */
  services: Em.A([]),
  serviceTimestamp: null,

  /**
   * Number of operation to load
   */
  operationsCount: 10,

  /**
   * Possible levels:
   * REQUESTS_LIST
   * HOSTS_LIST
   * TASKS_LIST
   * TASK_DETAILS
   */
  levelInfo: Em.Object.create({
    name: 'REQUESTS_LIST',
    requestId: null,
    taskId: null
  }),

  subscribeToUpdates: function () {
    this.requestMostRecent(() => {
      App.StompClient.subscribe('/events/requests', this.updateRequests.bind(this));
    });
  },

  updateRequests: function(event) {
    if (this.isUpgradeRequest({Requests: {request_context: event.requestContext}})) {
      return;
    }
    const request = this.get('services').findProperty('id', event.requestId);
    const context = this.parseRequestContext(event.requestContext);
    const visibleOperationsCount = this.get('operationsCount');
    const map = this.generateTasksMapOfRequest(event, request);
    const updatedState = {
      progress: Math.floor(event.progressPercent),
      status: event.requestStatus,
      userName: event.userName,
      isRunning: this.isRunning(event.requestStatus),
      startTime: App.dateTimeWithTimeZone(event.startTime),
      endTime: event.endTime > 0 ? App.dateTimeWithTimeZone(event.endTime) : event.endTime,
      previousTaskStatusMap: map.currentTaskStatusMap,
      hostsMap: map.hostsMap
    };

    if (request) {
      request.setProperties(updatedState);
    } else {
      this.get('services').unshift(Em.Object.create(updatedState, {
        id: event.requestId,
        name: context.requestContext,
        displayName: context.requestContext,
        tasks: event.Tasks
      }));
      if (this.get('services').length >= visibleOperationsCount) {
        this.set('isShowMoreAvailable', true);
        this.get('services').pop();
      }
    }
    this.set('serviceTimestamp', App.dateTime());
    this.propertyDidChange('services');
  },

  /**
   *
   * @param {object} event
   * @param {Em.Object} request
   * @returns {{}}
   */
  generateTasksMapOfRequest: function(event, request) {
    const hostsMap = request ? request.get('hostsMap') : {};
    const previousTaskStatusMap = request ? request.get('previousTaskStatusMap') : {};
    const currentTaskStatusMap = {};
    event.Tasks.forEach((task) => {
      const host = hostsMap[task.hostName];
      if (host) {
        const existedTask = host.logTasks.findProperty('Tasks.id', task.id);
        if (existedTask) {
          existedTask.Tasks.status = task.status;
        } else {
          host.logTasks.push(this.convertTaskFromEventToApi(task));
        }
        host.isModified = (host.isModified) ? true : previousTaskStatusMap[task.id] !== task.status;
      } else {
        hostsMap[task.hostName] = {
          name: task.hostName,
          publicName: task.hostName,
          logTasks: [this.convertTaskFromEventToApi(task)],
          isModified: previousTaskStatusMap[task.id] !== task.status
        };
      }
      currentTaskStatusMap[task.id] = task.status;
    }, this);
    return {
      currentTaskStatusMap,
      hostsMap
    }
  },

  convertTaskFromEventToApi: function(task) {
    return {
      Tasks: {
        status: task.status,
        host_name: task.hostName,
        id: task.id,
        request_id: task.requestId
      }
    }
  },

  handleTaskUpdates: function() {
    const levelInfo = this.get('levelInfo');
    if (!levelInfo.get('requestId') || !levelInfo.get('taskId')) {
      return;
    }
    const request = this.get('services').findProperty('id', levelInfo.get('requestId'));
    const taskStatus = request.get('previousTaskStatusMap')[levelInfo.get('taskId')];
    if (levelInfo.get('name') === 'TASK_DETAILS' && !this.isFinished(taskStatus)) {
      App.StompClient.subscribe(`/events/tasks/${levelInfo.get('taskId')}`, (updatedTask) => {
        this.updateTask(updatedTask);
        if (this.isFinished(updatedTask.status)) {
          App.StompClient.unsubscribe(`/events/tasks/${updatedTask.id}`);
        }
      });
    }
  }.observes('levelInfo.name'),

  updateTask: function(updatedTask) {
    const request = this.get('services').findProperty('id', updatedTask.requestId);
    const host = request.get('hostsMap')[updatedTask.hostName];
    const task = host.logTasks.findProperty('Tasks.id', updatedTask.id);
    task.Tasks.status = updatedTask.status;
    task.Tasks.stdout = updatedTask.stdout;
    task.Tasks.stderr = updatedTask.stderr;
    task.Tasks.structured_out = updatedTask.structured_out;
    task.Tasks.output_log = updatedTask.outLog;
    task.Tasks.error_log = updatedTask.errorLog;
    this.set('serviceTimestamp', App.dateTime());
  },

  /**
   * Get requests data from server
   * @param callback
   */
  requestMostRecent: function (callback) {
    var queryParams = this.getQueryParams();
    App.ajax.send({
      'name': queryParams.name,
      'sender': this,
      'success': queryParams.successCallback,
      'callback': callback,
      'data': queryParams.data
    });
    return !this.isInitLoading();
  },

  /**
   * indicate whether data for current level has already been loaded or not
   * @return {Boolean}
   */
  isInitLoading: function () {
    var levelInfo = this.get('levelInfo');
    var request = this.get('services').findProperty('id', levelInfo.get('requestId'));

    if (levelInfo.get('name') === 'HOSTS_LIST') {
      return Boolean(request && !request.get('hostsLevelLoaded'));
    }
    return false;
  },
  /**
   * construct params of ajax query regarding displayed level
   */
  getQueryParams: function () {
    var levelInfo = this.get('levelInfo');
    var count = this.get('operationsCount');
    var result = {
      name: 'background_operations.get_most_recent',
      successCallback: 'callBackForMostRecent',
      data: {
        'operationsCount': count
      }
    };
    if (levelInfo.get('name') === 'TASK_DETAILS') {
      result.name = 'background_operations.get_by_task';
      result.successCallback = 'callBackFilteredByTask';
      result.data = {
        'taskId': levelInfo.get('taskId'),
        'requestId': levelInfo.get('requestId')
      };
    } else if (levelInfo.get('name') === 'TASKS_LIST' || levelInfo.get('name') === 'HOSTS_LIST') {
      result.name = 'background_operations.get_by_request';
      result.successCallback = 'callBackFilteredByRequest';
      result.data = {
        'requestId': levelInfo.get('requestId')
      };
    }
    return result;
  },

  /**
   * Push hosts and their tasks to request
   * @param data
   */
  callBackFilteredByRequest: function (data) {
    var requestId = data.Requests.id;
    var requestInputs = data.Requests.inputs;
    var request = this.get('services').findProperty('id', requestId);
    var hostsMap = {};
    var previousTaskStatusMap = request.get('previousTaskStatusMap');
    var currentTaskStatusMap = {};
    data.tasks.forEach(function (task) {
      var host = hostsMap[task.Tasks.host_name];
      task.Tasks.request_id = requestId;
      task.Tasks.request_inputs = requestInputs;
      if (host) {
        host.logTasks.push(task);
        host.isModified = true;
      } else {
        hostsMap[task.Tasks.host_name] = {
          name: task.Tasks.host_name,
          publicName: task.Tasks.host_name,
          logTasks: [task],
          isModified: true
        };
      }
      currentTaskStatusMap[task.Tasks.id] = task.Tasks.status;
    }, this);
    request.set('previousTaskStatusMap', currentTaskStatusMap);
    request.set('hostsMap', hostsMap);
    request.set('hostsLevelLoaded', true);
    this.set('serviceTimestamp', App.dateTime());
  },
  /**
   * Update task, with uploading two additional properties: stdout and stderr
   * @param data
   * @param ajaxQuery
   * @param params
   */
  callBackFilteredByTask: function (data, ajaxQuery, params) {
    var request = this.get('services').findProperty('id', data.Tasks.request_id);
    var host = request.get('hostsMap')[data.Tasks.host_name];
    var task = host.logTasks.findProperty('Tasks.id', data.Tasks.id);
    task.Tasks.status = data.Tasks.status;
    task.Tasks.stdout = data.Tasks.stdout;
    task.Tasks.stderr = data.Tasks.stderr;

    // Put some command information to task object
    task.Tasks.command = data.Tasks.command;
    task.Tasks.custom_command_name = data.Tasks.custom_command_name;
    task.Tasks.structured_out = data.Tasks.structured_out;

    task.Tasks.output_log = data.Tasks.output_log;
    task.Tasks.error_log = data.Tasks.error_log;
    this.set('serviceTimestamp', App.dateTime());
  },

  /**
   * returns true if it's upgrade equest
   * use this flag to exclude upgrade requests from bgo
   * @param {object} request
   * @returns {boolean}
   */
  isUpgradeRequest: function(request) {
    var context = Em.get(request, 'Requests.request_context');
    return context ? /(upgrading|downgrading)/.test(context.toLowerCase()) : false;
  },
  /**
   * Prepare, received from server, requests for host component popup
   * @param data
   */
  callBackForMostRecent: function (data) {
    var currentRequestIds = [];
    var countIssued = this.get('operationsCount');
    var countGot = data.itemTotal;

    data.items.forEach(function (request) {
      if (this.isUpgradeRequest(request)) {
        return;
      }
      var rq = this.get("services").findProperty('id', request.Requests.id);
      var isRunning = this.isRunning(request.Requests.request_status);
      var requestParams = this.parseRequestContext(request.Requests.request_context);
      const requestState = {
        progress: Math.floor(request.Requests.progress_percent),
        status: request.Requests.request_status,
        userName: request.Requests.user_name || Em.I18n.t('hostPopup.default.userName'),
        isRunning: isRunning,
        startTime: App.dateTimeWithTimeZone(request.Requests.start_time),
        endTime: request.Requests.end_time > 0 ? App.dateTimeWithTimeZone(request.Requests.end_time) : request.Requests.end_time
      };
      this.assignScheduleId(request, requestParams);
      currentRequestIds.push(request.Requests.id);

      if (rq) {
        rq.setProperties(requestState);
      } else {
        rq = Em.Object.create(requestState, {
          id: request.Requests.id,
          name: requestParams.requestContext,
          displayName: requestParams.requestContext,
          hostsMap: {},
          tasks: [],
          dependentService: requestParams.dependentService,
          sourceRequestScheduleId: request.Requests.request_schedule && request.Requests.request_schedule.schedule_id,
          previousTaskStatusMap: {},
          contextCommand: requestParams.contextCommand
        });
        this.get("services").unshift(rq);
        //To sort DESC by request id
        this.set("services", this.get("services").sortProperty('id').reverse());
      }
    }, this);
    this.removeOldRequests(currentRequestIds);
    this.set('isShowMoreAvailable', countGot >= countIssued);
    this.set('serviceTimestamp', App.dateTimeWithTimeZone());
  },

  isShowMoreAvailable: null,

  /**
   * remove old requests
   * as API returns 10, or  20 , or 30 ...etc latest request, the requests that absent in response should be removed
   * @param currentRequestIds
   */
  removeOldRequests: function (currentRequestIds) {
    var services = this.get('services');

    for (var i = 0, l = services.length; i < l; i++) {
      if (!currentRequestIds.contains(services[i].id)) {
        services.splice(i, 1);
        i--;
        l--;
      }
    }
  },

  /**
   * identify whether request or task is running by status
   * @param status
   * @return {Boolean}
   */
  isRunning: function (status) {
    return ['IN_PROGRESS', 'QUEUED', 'PENDING'].contains(status);
  },

  /**
   * identify whether request or task is finished by status
   * @param status
   * @return {Boolean}
   */
  isFinished: function (status) {
    return ['FAILED', 'ABORTED', 'COMPLETED'].contains(status);
  },

  /**
   * identify whether there is only one host in request
   * @param inputs
   * @return {Boolean}
   */
  isOneHost: function (inputs) {
    if (!inputs) {
      return false;
    }
    inputs = JSON.parse(inputs);
    if (inputs && inputs.included_hosts) {
      return inputs.included_hosts.split(',').length < 2;
    }
    return false
  },
  /**
   * assign schedule_id of request to null if it's Recommission operation
   * @param request
   * @param requestParams
   */
  assignScheduleId: function (request, requestParams) {
    var oneHost = this.isOneHost(request.Requests.inputs);
    if (request.Requests.request_schedule && oneHost && /Recommission/.test(requestParams.requestContext)) {
      request.Requests.request_schedule.schedule_id = null;
    }
  },

  /**
   * parse request context and if keyword "_PARSE_" is present then format it
   * @param {string} requestContext
   * @return {Object}
   */
  parseRequestContext: function (requestContext) {
    var context = {};
    if (requestContext) {
      if (requestContext.indexOf(App.BackgroundOperationsController.CommandContexts.PREFIX) !== -1) {
        context = this.getRequestContextWithPrefix(requestContext);
      } else {
        context.requestContext = requestContext;
      }
    } else {
      context.requestContext = Em.I18n.t('requestInfo.unspecified');
    }
    return context;
  },

  /**
   *
   * @param {string} requestContext
   * @returns {{requestContext: *, dependentService: *, contextCommand: *}}
   */
  getRequestContextWithPrefix: function (requestContext) {
    var contextSplits = requestContext.split('.'),
        parsedRequestContext,
        contextCommand = contextSplits[1],
        service = contextSplits[2];

    switch (contextCommand) {
      case "STOP":
      case "START":
        if (service === 'ALL_SERVICES') {
          parsedRequestContext = Em.I18n.t("requestInfo." + contextCommand.toLowerCase()).format(Em.I18n.t('common.allServices'));
        } else {
          parsedRequestContext = Em.I18n.t("requestInfo." + contextCommand.toLowerCase()).format(App.format.role(service, true));
        }
        break;
      case "ROLLING-RESTART":
        parsedRequestContext = Em.I18n.t("rollingrestart.rest.context").format(App.format.role(service, true), contextSplits[3], contextSplits[4]);
        break;
    }
    return {
      requestContext: parsedRequestContext,
      dependentService: service,
      contextCommand: contextCommand
    }
  },

  popupView: null,

  /**
   * Onclick handler for background operations number located right to logo
   */
  showPopup: function () {
    // load the checkbox on footer first, then show popup.
    var self = this;
    App.router.get('userSettingsController').dataLoading('show_bg').done(function (initValue) {
      App.updater.immediateRun('requestMostRecent');

      App.HostPopup.set("breadcrumbs", [ App.HostPopup.get("rootBreadcrumb") ]);

      if (self.get('popupView') && App.HostPopup.get('isBackgroundOperations')) {
        self.set('popupView.isNotShowBgChecked', !initValue);
        self.set('popupView.isOpen', true);
        var el = $(self.get('popupView.element'));
        el.appendTo('#wrapper');
        el.find('.modal').show();
      } else {
        self.set('popupView', App.HostPopup.initPopup("", self, true));
        self.set('popupView.isNotShowBgChecked', !initValue);
      }
    });
  },

  /**
   * Called on logout
   */
  clear: function () {
    // set operations count to default value
    this.set('operationsCount', 10);
  }

});

/**
 * Each background operation has a context in which it operates.
 * Generally these contexts are fixed messages. However, we might
 * want to associate semantics to this context - like showing, disabling
 * buttons when certain operations are in progress.
 *
 * To make this possible we have command contexts where the context
 * is not a human readable string, but a pattern indicating the command
 * it is running. When UI shows these, they are translated into human
 * readable strings.
 *
 * General pattern of context names is "_PARSE_.{COMMAND}.{ID}[.{Additional-Data}...]"
 */
App.BackgroundOperationsController.CommandContexts = {
  PREFIX : "_PARSE_",
  /**
   * Stops all services
   */
  STOP_ALL_SERVICES : "_PARSE_.STOP.ALL_SERVICES",
  /**
   * Starts all services
   */
  START_ALL_SERVICES : "_PARSE_.START.ALL_SERVICES",
  /**
   * Starts service indicated by serviceID.
   * @param {String} serviceID Parameter {0}. Example: HDFS
   */
  START_SERVICE : "_PARSE_.START.{0}",
  /**
   * Stops service indicated by serviceID.
   * @param {String} serviceID Parameter {0}. Example: HDFS
   */
  STOP_SERVICE : "_PARSE_.STOP.{0}",
  /**
   * Performs rolling restart of componentID in batches.
   * This context is the batchNumber batch out of totalBatchCount batches.
   * @param {String} componentID Parameter {0}. Example "DATANODE"
   * @param {Number} batchNumber Parameter {1}. Batch number of this batch. Example 3.
   * @param {Number} totalBatchCount Parameter {2}. Total number of batches. Example 10.
   */
  ROLLING_RESTART : "_PARSE_.ROLLING-RESTART.{0}.{1}.{2}"
};
