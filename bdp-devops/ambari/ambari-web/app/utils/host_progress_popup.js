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
var dataUtils = require('utils/data_manipulation');

/**
 * Host information shown in the operations popup
 * @typedef {Em.Object} wrappedHost
 * @property {string} name
 * @property {string} publicName
 * @property {string} displayName
 * @property {number} progress
 * @property {boolean} isInProgress
 * @property {string} serviceName
 * @property {string} status
 * @property {number} isVisible
 * @property {string} icon
 * @property {string} barColor
 * @property {string} barWidth
 */

/**
 * Task information shown in the operations popup
 * @typedef {Em.Object} wrappedTask
 * @property {string} id
 * @property {string} hostName
 * @property {string} command
 * @property {string} commandDetail
 * @property {string} status
 * @property {string} role
 * @property {string} stderr
 * @property {string} stdout
 * @property {number} request_id
 * @property {boolean} isVisible
 * @property {string} startTime
 * @property {string} duration
 * @property {string} icon
 */

/**
 * Service information shown in the operations popup
 * @typedef {Em.Object} wrappedService
 * @property {string} id
 * @property {string} displayName
 * @property {string} progress
 * @property {string} status
 * @property {string} userName
 * @property {boolean} isRunning
 * @property {string} name
 * @property {boolean} isVisible
 * @property {string} startTime
 * @property {string} duration
 * @property {string} icon
 * @property {string} barColor
 * @property {boolean} isInProgress
 * @property {string} barWidth
 * @property {number} sourceRequestScheduleId
 * @property {string} contextCommand
 */

/**
 * App.HostPopup is for the popup that shows up upon clicking already-performed or currently-in-progress operations
 * Allows to abort executing operations
 *
 * @type {Em.Object}
 * @class {HostPopup}
 */
App.HostPopup = Em.Object.create({

  name: 'hostPopup',

  /**
   * @type {object[]}
   */
  servicesInfo: [],

  /**
   * @type {wrappedHost[]}
   */
  hosts: [],

  /**
   * @type {?object[]}
   */
  inputData: null,

  /**
   * @type {string}
   */
  serviceName: '',

  /**
   * @type {?Number}
   */
  currentServiceId: null,

  /**
   * @type {?Number}
   */
  previousServiceId: null,

  /**
   * @type {string}
   */
  popupHeaderName: '',

  //This is what the breadcrumbs will be reset to every time the modal is opened.
  rootBreadcrumb: null,

  /**
   * @type {object[]}
   */
  breadcrumbs: [],

  operationInfo: null,

  /**
   * @type {?App.Controller}
   */
  dataSourceController: null,

  /**
   * @type {bool}
   */
  isBackgroundOperations: false,

  /**
   * @type {?string}
   */
  currentHostName: null,

  /**
   * @type {?App.ModalPopup}
   */
  isPopup: null,

  /**
   * @type {object}
   */
  detailedProperties: {
    stdout: 'stdout',
    stderr: 'stderr',
    outputLog: 'output_log',
    errorLog: 'error_log'
  },

  /**
   * @type {object}
   */
  barColorMap: {
    'FAILED': 'progress-bar-danger',
    'ABORTED': 'progress-bar-warning',
    'TIMEDOUT': 'progress-bar-warning',
    'IN_PROGRESS': 'progress-bar-info',
    'COMPLETED': 'progress-bar-success'
  },

  /**
   * map to get css class with styles by service status
   *
   * @type {object}
   */
  statusesStyleMap: {
    'FAILED': ['FAILED', 'icon-exclamation-sign', 'progress-bar-danger', false],
    'ABORTED': ['ABORTED', 'glyphicon glyphicon-minus', 'progress-bar-warning', false],
    'TIMEDOUT': ['TIMEDOUT', 'glyphicon glyphicon-time', 'progress-bar-warning', false],
    'IN_PROGRESS': ['IN_PROGRESS', 'icon-cogs', 'progress-bar-info', true],
    'COMPLETED': ['SUCCESS', 'glyphicon glyphicon-ok', 'progress-bar-success', false]
  },

  /**
   * View with "Abort Request"-button
   *
   * @type {Em.View}
   */
  abortIcon: Em.View.extend({
    tagName: 'a',
    classNames: ['abort-icon'],
    template: Em.Handlebars.compile('<span class="icon icon-remove-circle"></span>'),
    click: function () {
      this.get('controller').abortRequest(this.get('servicesInfo'));
      return false;
    },
    didInsertElement: function () {
      App.tooltip($(this.get('element')), {
        placement: "top",
        title: Em.I18n.t('hostPopup.bgop.abortRequest.title')
      });
    },
    willDestroyElement: function () {
      $(this.get('element')).tooltip('destroy');
    }
  }),

  /**
   * View with status icon (and tooltip on it)
   *
   * @type {Em.View}
   */
  statusIcon: Em.View.extend({
    tagName: 'i',
    classNames: ["service-status"],
    classNameBindings: ['servicesInfo.status', 'servicesInfo.icon', 'additionalClass'],
    attributeBindings: ['data-original-title'],
    'data-original-title': function () {
      return this.get('servicesInfo.status');
    }.property('servicesInfo.status'),
    didInsertElement: function () {
      App.tooltip($(this.get('element')));
    },
    willDestroyElement: function () {
      $(this.get('element')).tooltip('destroy');
    }
  }),

  /**
   * Determines if background operation can be aborted depending on its status
   *
   * @param status
   * @returns {boolean}
   */
  isAbortableByStatus: function (status) {
    var statuses = this.get('statusesStyleMap');
    return !Em.keys(statuses).contains(status) || status === 'IN_PROGRESS';
  },

  /**
   * Send request to abort operation
   *
   * @method abortRequest
   */
  abortRequest: function (serviceInfo) {
    var requestName = serviceInfo.get('name');
    var self = this;
    App.showConfirmationPopup(function () {
      serviceInfo.set('isAbortable', false);
      return App.ajax.send({
        name: 'background_operations.abort_request',
        sender: self,
        data: {
          requestId: serviceInfo.get('id'),
          requestName: requestName,
          serviceInfo: serviceInfo
        },
        success: 'abortRequestSuccessCallback',
        error: 'abortRequestErrorCallback'
      });
    }, Em.I18n.t('hostPopup.bgop.abortRequest.confirmation.body').format(requestName));
    return false;
  },

  /**
   * Method called on successful sending request to abort operation
   *
   * @return {App.ModalPopup}
   * @method abortRequestSuccessCallback
   */
  abortRequestSuccessCallback: function (response, request, data) {
    return App.ModalPopup.show({
      header: Em.I18n.t('hostPopup.bgop.abortRequest.modal.header'),
      encodeBody: false,
      body: Em.I18n.t('hostPopup.bgop.abortRequest.modal.body').format(data.requestName),
      secondary: null
    });
  },

  /**
   * Method called on unsuccessful sending request to abort operation
   *
   * @method abortRequestErrorCallback
   */
  abortRequestErrorCallback: function (xhr, textStatus, error, opt, data) {
    data.serviceInfo.set('isAbortable', this.isAbortableByStatus(data.serviceInfo.status));
    App.ajax.defaultErrorHandler(xhr, opt.url, 'PUT', xhr.status);
  },

  /**
   * Entering point of this component
   *
   * @param {String} serviceName
   * @param {Object} controller
   * @param {Boolean} isBackgroundOperations
   * @param {Integer} requestId
   * @method initPopup
   */
  initPopup: function (serviceName, controller, isBackgroundOperations, requestId) {
    if (App.get('isClusterUser')) return;

    if (!isBackgroundOperations) {
      this.clearHostPopup();
      this.set("rootBreadcrumb", { label: serviceName });
    } else {
      this.set("rootBreadcrumb", { label: Em.I18n.t("common.backgroundOperations") });
    }

    this.setProperties({
      breadcrumbs: [ this.get("rootBreadcrumb") ],
      currentServiceId: requestId,
      serviceName: serviceName,
      dataSourceController: controller,
      isBackgroundOperations: isBackgroundOperations,
      inputData: controller.get("services"),
      servicesInfo: []
    });

    if (isBackgroundOperations) {
      this.onServiceUpdate();
    } else {
      this.onHostUpdate();
    }

    return this.createPopup();
  },

  /**
   * clear info popup data
   *
   * @method clearHostPopup
   */
  clearHostPopup: function () {
    this.setProperties({
      servicesInfo: [],
      host: null,
      inputData: null,
      serviceName: '',
      currentServiceId: null,
      previousServiceId: null,
      popupHeaderName: '',
      dataSourceController: null,
      currentHostName: null
    });
    if(this.get('isPopup')) {
      this.get('isPopup').remove();
    }
  },

  /**
   * Depending on tasks status
   *
   * @param {object[]} tasks
   * @return {*[]} [Status, Icon type, Progressbar color, is IN_PROGRESS]
   * @method getStatus
   */
  getStatus: function (tasks) {
    var isCompleted = true;
    var tasksLength = tasks.length;
    for (var i = 0; i < tasksLength; i++) {
      var taskStatus = tasks[i].Tasks.status;
      if (taskStatus !== 'COMPLETED') {
        isCompleted = false;
      }
      if (taskStatus === 'FAILED') {
        return ['FAILED', 'icon-exclamation-sign', 'progress-bar-danger', false];
      }
      if (taskStatus === 'ABORTED') {
        return ['ABORTED', 'glyphicon glyphicon-minus', 'progress-bar-warning', false];
      }
      if (taskStatus === 'TIMEDOUT') {
        return ['TIMEDOUT', 'glyphicon glyphicon-time', 'progress-bar-warning', false];
      }
      if (taskStatus === 'IN_PROGRESS') {
        return ['IN_PROGRESS', 'icon-cogs', 'progress-bar-info', true]
      }
    }
    if (isCompleted) {
      return ['SUCCESS', 'glyphicon glyphicon-ok', 'progress-bar-success', false];
    }
    return ['PENDING', 'glyphicon glyphicon-cog', 'progress-bar-info', true];
  },

  /**
   * Progress of host or service depending on tasks status
   * If no tasks, progress is 0
   *
   * @param {?object[]} tasks
   * @return {Number} percent of completion
   * @method getProgress
   */
  getProgress: function (tasks) {
    if (!tasks || !tasks.length) {
      return 0;
    }

    var groupedByStatus = dataUtils.groupPropertyValues(tasks, 'Tasks.status');

    var completedActions = Em.getWithDefault(groupedByStatus, 'COMPLETED.length', 0) +
      Em.getWithDefault(groupedByStatus, 'FAILED.length', 0) +
      Em.getWithDefault(groupedByStatus, 'ABORTED.length', 0) +
      Em.getWithDefault(groupedByStatus, 'TIMEDOUT.length', 0);
    var queuedActions = Em.getWithDefault(groupedByStatus, 'QUEUED.length', 0);
    var inProgressActions = Em.getWithDefault(groupedByStatus, 'IN_PROGRESS.length', 0);

    return Math.ceil((queuedActions * 0.09 + inProgressActions * 0.35 + completedActions ) / tasks.length * 100);
  },

  /**
   * Count number of operations for select box options
   *
   * @param {?Object[]} obj
   * @param {progressPopupCategoryObject[]} categories
   * @method setSelectCount
   */
  setSelectCount: function (obj, categories) {
    if (!obj) {
      return;
    }
    var groupedByStatus = dataUtils.groupPropertyValues(obj, 'status');

    categories.findProperty("value", 'all').set("count", obj.length);
    categories.findProperty("value", 'pending').set("count", Em.getWithDefault(groupedByStatus, 'pending.length', 0) + Em.getWithDefault(groupedByStatus, 'queued.length', 0));
    categories.findProperty("value", 'in_progress').set("count", Em.getWithDefault(groupedByStatus, 'in_progress.length', 0));
    categories.findProperty("value", 'failed').set("count", Em.getWithDefault(groupedByStatus, 'failed.length', 0));
    categories.findProperty("value", 'completed').set("count", Em.getWithDefault(groupedByStatus, 'success.length', 0) + Em.getWithDefault(groupedByStatus, 'completed.length', 0));
    categories.findProperty("value", 'aborted').set("count", Em.getWithDefault(groupedByStatus, 'aborted.length', 0));
    categories.findProperty("value", 'timedout').set("count", Em.getWithDefault(groupedByStatus, 'timedout.length', 0));
  },

  /**
   * For Background operation popup calculate number of running Operations, and set popup header
   *
   * @param {bool} isServiceListHidden
   * @method setBackgroundOperationHeader
   */
  setBackgroundOperationHeader: function (isServiceListHidden) {
    if (this.get('isBackgroundOperations') && !isServiceListHidden) {
      var numRunning = App.router.get('backgroundOperationsController.runningOperationsCount');
      this.set("popupHeaderName", numRunning + Em.I18n.t('hostPopup.header.postFix').format(numRunning === 1 ? "" : "s"));
    }
  },

  /**
   * Create services obj data structure for popup
   * Set data for services
   *
   * @param {bool} isServiceListHidden
   * @method onServiceUpdate
   */
  onServiceUpdate: function (isServiceListHidden) {
    var servicesInfo = this.get("servicesInfo");
    var currentServices = [];

    var inputData = this.get("inputData");
    if (inputData) {
      inputData.forEach(function (service, index) {
        var updatedService;
        var id = service.id;
        currentServices.push(id);
        var existedService = servicesInfo.findProperty('id', id);
        updatedService = existedService;
        if (existedService) {
          updatedService = this.updateService(existedService, service);
        }
        else {
          updatedService = this.createService(service);
          servicesInfo.insertAt(index, updatedService);
        }
        updatedService.set('isAbortable', App.isAuthorized('SERVICE.START_STOP') && this.isAbortableByStatus(service.status));
      }, this);
    }

    this.removeOldServices(servicesInfo, currentServices);
    this.setBackgroundOperationHeader(isServiceListHidden);
  },

  /**
   * Create service object from transmitted data
   *
   * @param {object} service
   * @return {wrappedService}
   * @method createService
   */
  createService: function (service) {
    var statuses = this.get('statusesStyleMap');
    var pendingStatus = ['PENDING', 'glyphicon glyphicon-cog', 'progress-bar-info', true];
    var status = statuses[service.status] || pendingStatus;
    return Em.Object.create({
      id: service.id,
      displayName: service.displayName,
      progress: service.progress,
      status: App.format.taskStatus(status[0]),
      userName: service.userName,
      isRunning: service.isRunning,
      name: service.name,
      isVisible: true,
      startTime: date.startTime(service.startTime),
      duration: date.durationSummary(service.startTime, service.endTime),
      icon: status[1],
      barColor: status[2],
      isInProgress: status[3],
      barWidth: "width:" + service.progress + "%;",
      sourceRequestScheduleId: service.sourceRequestScheduleId,
      contextCommand: service.contextCommand
    });
  },

  /**
   * Update properties of existed service with new data
   *
   * @param {wrappedService} service
   * @param {object} newData
   * @returns {wrappedService}
   * @method updateService
   */
  updateService: function (service, newData) {
    var statuses = this.get('statusesStyleMap');
    var pendingStatus = ['PENDING', 'glyphicon glyphicon-cog', 'progress-bar-info', true];
    var status = statuses[newData.status] || pendingStatus;
    return service.setProperties({
      progress: newData.progress,
      status: App.format.taskStatus(status[0]),
      userName: newData.userName,
      isRunning: newData.isRunning,
      startTime: date.startTime(newData.startTime),
      duration: date.durationSummary(newData.startTime, newData.endTime),
      icon: status[1],
      barColor: status[2],
      isInProgress: status[3],
      barWidth: "width:" + newData.progress + "%;",
      sourceRequestScheduleId: newData.get && newData.get('sourceRequestScheduleId'),
      contextCommand: newData.get && newData.get('contextCommand')
    });
  },

  /**
   * remove old requests
   * as API returns 10, or  20 , or 30 ...etc latest request, the requests that absent in response should be removed
   *
   * @param {wrappedService[]} services
   * @param {number[]} currentServicesIds
   * @method removeOldServices
   */
  removeOldServices: function (services, currentServicesIds) {
    for (var i = 0, l = services.length; i < l; i++) {
      if (!currentServicesIds.contains(services[i].id)) {
        services.splice(i, 1);
        i--;
        l--;
      }
    }
  },

  /**
   * Wrap task as Ember-object
   *
   * @param {Object} _task
   * @return {wrappedTask}
   * @method createTask
   */
  createTask: function (_task) {
    return Em.Object.create({
      id: _task.Tasks.id,
      hostName: _task.Tasks.host_name,
      command: _task.Tasks.command.toLowerCase() === 'service_check' ? '' : _task.Tasks.command.toLowerCase(),
      commandDetail: App.format.commandDetail(_task.Tasks.command_detail, _task.Tasks.request_inputs, _task.Tasks.ops_display_name),
      status: App.format.taskStatus(_task.Tasks.status),
      role: App.format.role(_task.Tasks.role, false),
      stderr: _task.Tasks.stderr,
      stdout: _task.Tasks.stdout,
      request_id: _task.Tasks.request_id,
      isVisible: true,
      startTime: date.startTime(_task.Tasks.start_time),
      duration: date.durationSummary(_task.Tasks.start_time, _task.Tasks.end_time),
      icon: function () {
        var statusIconMap = {
          'pending': 'glyphicon glyphicon-cog',
          'queued': 'glyphicon glyphicon-cog',
          'in_progress': 'icon-cogs',
          'completed': 'glyphicon glyphicon-ok',
          'failed': 'icon-exclamation-sign',
          'aborted': 'glyphicon glyphicon-minus',
          'timedout': 'glyphicon glyphicon-time'
        };
        return statusIconMap[this.get('status')] || 'glyphicon glyphicon-cog';
      }.property('status')
    });
  },

  /**
   * Create hosts and tasks data structure for popup
   * Set data for hosts and tasks
   *
   * @method onHostUpdate
   */
  onHostUpdate: function () {
    var self = this;
    if (this.get("inputData")) {
      var hostsMap = this._getHostsMap();
      var existedHosts = self.get('hosts');

      if (existedHosts && existedHosts.length && this.get('currentServiceId') === this.get('previousServiceId')) {
        this._processingExistingHostsWithSameService(hostsMap);
      } else {
        var hostsArr = this._hostMapProcessing(hostsMap);
        hostsArr = hostsArr.sortProperty('name');
        hostsArr.setEach("serviceName", this.get("serviceName"));
        self.set("hosts", hostsArr);
        self.set('previousServiceId', this.get('currentServiceId'));
      }
    }
    var operation = this.get('servicesInfo').findProperty('id', this.get('currentServiceId'));
    this.set('operationInfo', !operation || operation && operation.get('progress') === 100 ? null : operation);
  },

  /**
   * Generate hosts map for further processing <code>inputData</code>
   *
   * @returns {object}
   * @private
   * @method _getHostsMap
   */
  _getHostsMap: function () {
    var hostsData;
    var hostsMap = {};
    var inputData = this.get('inputData');

    if (this.get('isBackgroundOperations') && this.get("currentServiceId")) {
      //hosts popup for Background Operations
      hostsData = inputData.findProperty("id", this.get("currentServiceId"));
    } else {
      if (this.get("serviceName")) {
        //hosts popup for Wizards
        hostsData = inputData.findProperty("name", this.get("serviceName"));
      }
    }

    if (hostsData) {
      if (hostsData.hostsMap) {
        //hosts data come from Background Operations as object map
        hostsMap = hostsData.hostsMap;
      } else {
        if (hostsData.hosts) {
          //hosts data come from Wizard as array
          hostsMap = hostsData.hosts.toMapByProperty('name');
        }
      }
    }

    return hostsMap;
  },

  /**
   *
   * @param {object} hostsMap
   * @returns {wrappedHost[]}
   * @private
   * @method _hostMapProcessing
   */
  _hostMapProcessing: function (hostsMap) {
    var self = this;
    var hostsArr = [];
    for (var hostName in hostsMap) {
      if (!hostsMap.hasOwnProperty(hostName)) {
        continue;
      }
      var _host = hostsMap[hostName];
      var tasks = _host.logTasks;
      var hostInfo = Em.Object.create({
        name: hostName,
        publicName: _host.publicName,
        displayName: Em.computed.truncate('name', 43, 40),
        progress: 0,
        status: App.format.taskStatus("PENDING"),
        serviceName: _host.serviceName,
        isVisible: true,
        icon: "glyphicon glyphicon-cog",
        barColor: "progress-bar-info",
        barWidth: "width:0%;"
      });

      if (tasks.length) {
        tasks = tasks.sortProperty('Tasks.id');
        var hostStatus = self.getStatus(tasks);
        var hostProgress = self.getProgress(tasks);
        hostInfo.setProperties({
          status: App.format.taskStatus(hostStatus[0]),
          icon: hostStatus[1],
          barColor: hostStatus[2],
          isInProgress: hostStatus[3],
          progress: hostProgress,
          barWidth: "width:" + hostProgress + "%;"
        });
      }
      hostInfo.set('logTasks', tasks);
      hostsArr.push(hostInfo);
    }
    return hostsArr;
  },

  /**
   *
   * @param {object} hostsMap
   * @private
   * @method _processingExistingHostsWithSameService
   */
  _processingExistingHostsWithSameService: function (hostsMap) {
    var self = this;
    var existedHosts = self.get('hosts');
    var detailedProperties = this.get('detailedProperties');
    var detailedPropertiesKeys = Em.keys(detailedProperties);

    existedHosts.forEach(function (host) {
      var newHostInfo = hostsMap[host.get('name')];

      //update only hosts with changed tasks or currently opened tasks of host
      if (newHostInfo &&
          (!this.get('isBackgroundOperations') ||
            newHostInfo.isModified ||
            this.get('currentHostName') === host.get('name'))) {
        var hostStatus = self.getStatus(newHostInfo.logTasks);
        var hostProgress = self.getProgress(newHostInfo.logTasks);

        host.setProperties({
          status: App.format.taskStatus(hostStatus[0]),
          icon: hostStatus[1],
          barColor: hostStatus[2],
          isInProgress: hostStatus[3],
          progress: hostProgress,
          barWidth: "width:" + hostProgress + "%;",
          logTasks: newHostInfo.logTasks
        });

        var existTasks = host.get('tasks');

        if (existTasks) {
          newHostInfo.logTasks.forEach(function (_task) {
            var existTask = existTasks.findProperty('id', _task.Tasks.id);

            if (existTask) {
              var status = _task.Tasks.status;

              detailedPropertiesKeys.forEach(function (key) {
                var name = detailedProperties[key];
                var value = _task.Tasks[name];

                if (!Em.isNone(value)) {
                  existTask.set(key, value);
                }
              }, this);

              existTask.setProperties({
                status: App.format.taskStatus(status),
                startTime: date.startTime(_task.Tasks.start_time),
                duration: date.durationSummary(_task.Tasks.start_time, _task.Tasks.end_time)
              });

              existTask = self._handleRebalanceHDFS(_task, existTask);
            } else {
              existTasks.pushObject(this.createTask(_task));
            }
          }, this);
        }
      }
    }, this);
  },

  /**
   * Custom processing for "Rebalance HDFS"-task
   *
   * @param {object} task
   * @param {object} existTask
   * @returns {object}
   * @private
   * @method _handleRebalanceHDFS
   */
  _handleRebalanceHDFS: function (task, existTask) {
    var barColorMap = this.get('barColorMap');
    var isRebalanceHDFSTask = task.Tasks.command === 'CUSTOM_COMMAND' && task.Tasks.custom_command_name === 'REBALANCEHDFS';
    existTask.set('isRebalanceHDFSTask', isRebalanceHDFSTask);
    if (isRebalanceHDFSTask) {
      var structuredOut = task.Tasks.structured_out || {};
      var status = task.Tasks.status;
      existTask.setProperties({
        dataMoved: structuredOut.dataMoved || '0',
        dataLeft: structuredOut.dataLeft || '0',
        dataBeingMoved: structuredOut.dataBeingMoved || '0',
        barColor: barColorMap[status],
        isInProgress: status === 'IN_PROGRESS',
        isNotComplete: ['QUEUED', 'IN_PROGRESS'].contains(status),
        completionProgressStyle: 'width:' + (structuredOut.completePercent || 0) * 100 + '%;',
        command: task.Tasks.command,
        custom_command_name: task.Tasks.custom_command_name
      });
    }
    return existTask;
  },

  /**
   * Show popup
   *
   * @return {App.ModalPopup} PopupObject For testing purposes
   */
  createPopup: function () {
    var self = this;
    var isBackgroundOperations = this.get('isBackgroundOperations');

    this.set('isPopup', App.ModalPopup.show({

      /**
       * Controls visiblity of Task Details view.
       * @type {boolean}
       */
      isLogWrapHidden: true,

      /**
       * Controls visiblity of Tasks view.
       * @type {boolean}
       */
      isTaskListHidden: true,

      /**
       * Controls visiblity of Hosts view.
       * @type {boolean}
       */
      isHostListHidden: true,

      /**
       * Controls visiblity of Background Operations view.
       * @type {boolean}
       */
      isServiceListHidden: false,

      /**
       * Single function to handle changing the currently displayed view in the modal.
       * Use this rather than setting the booleans above directly.
       */
      switchView: function(to) {
        switch (to) {
          case "OPS_LIST":
            this.set("isLogWrapHidden", true);
            this.set("isTaskListHidden", true);
            this.set("isHostListHidden", true);
            this.set("isServiceListHidden", false);
            break;
          case "HOSTS_LIST":
            this.set("isLogWrapHidden", true);
            this.set("isTaskListHidden", true);
            this.set("isHostListHidden", false);
            this.set("isServiceListHidden", true);
            break;
          case "TASKS_LIST":
            this.set("isLogWrapHidden", true);
            this.set("isTaskListHidden", false);
            this.set("isHostListHidden", true);
            this.set("isServiceListHidden", true);
            break;
          case "TASK_DETAILS":
            this.set("isLogWrapHidden", false);
            this.set("isTaskListHidden", true);
            this.set("isHostListHidden", true);
            this.set("isServiceListHidden", true);
            break;
        }
      },

      /**
       * @type {boolean}
       */
      isHideBodyScroll: true,

      /**
       * no need to track is it loaded when popup contain only list of hosts
       * @type {bool}
       */
      isLoaded: !isBackgroundOperations,

      /**
       * is BG-popup opened
       * @type {bool}
       */
      isOpen: false,

      /**
       * @type {object}
       */
      detailedProperties: self.get('detailedProperties'),

      isVisible: function() {
        return !(App.get('isClusterUser') && isBackgroundOperations);
      }.property('App.isClusterUser'),

      didInsertElement: function () {
        this._super();
        this.set('isOpen', true);
      },

      /**
       * @type {Em.View}
       */
      headerClass: App.BreadcrumbsView.extend({
        controller: this,
        items: function () {
          let items = this.get('controller.breadcrumbs');
          items = items.map(item => App.BreadcrumbItem.extend(item).create());
          if (items.length) {
            items.get('lastObject').setProperties({
              disabled: true,
              isLast: true
            });
          }
          return items;
        }.property('controller.breadcrumbs')
      }),

      /**
       * @type {Em.View}
       */
      titleClass: Em.View.extend({
        controller: this,
        template: Em.Handlebars.compile('{{popupHeaderName}} ' +
          '{{#unless view.parentView.isHostListHidden}}{{#if controller.operationInfo.isAbortable}}' +
          '{{view controller.abortIcon servicesInfoBinding="controller.operationInfo"}}' +
          '{{/if}}{{/unless}}')
      }),

      /**
       * @type {Em.View}
       */
      footerClass: Em.View.extend({
        classNames: ["modal-footer"],
        templateName: require('templates/common/host_progress_popup_footer')
      }),

      /**
       * @type {String[]}
       */
      classNames: ['common-modal-wrapper', 'host-progress-popup', 'full-height-modal'],
      modalDialogClasses: ['modal-lg'],

      /**
       * Auto-display BG-popup
       *
       * @type {bool}
       */
      isNotShowBgChecked: null,

      /**
       * Save user pref about auto-display BG-popup
       *
       * @method updateNotShowBgChecked
       */
      updateNotShowBgChecked: function () {
        var curVal = !this.get('isNotShowBgChecked');
        if (!App.get('testMode')) {
          App.router.get('userSettingsController').postUserPref('show_bg', curVal);
        }
      }.observes('isNotShowBgChecked'),

      autoHeight: false,

      /**
       * @method closeModelPopup
       */
      closeModelPopup: function () {
        this.set('isOpen', false);
        if (isBackgroundOperations) {
          $(this.get('element')).detach();
          App.router.get('backgroundOperationsController').set('levelInfo.name', 'OPS_LIST');
        } else {
          this.hide();
          self.set('isPopup', null);
        }
      },

      onPrimary: function () {
        this.closeModelPopup();
      },

      onClose: function () {
        this.closeModelPopup();
      },

      secondary: null,

      bodyClass: App.HostProgressPopupBodyView.extend({
        controller: self
      })

    }));

    return this.get('isPopup');
  }
});
