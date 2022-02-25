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
 * Mixin for wizard controller for showing command progress on wizard pages
 * This should
 * @type {Ember.Mixin}
 */
App.wizardProgressPageControllerMixin = Em.Mixin.create(App.InstallComponent, {
  controllerName: '',
  clusterDeployState: 'WIZARD_DEPLOY',
  status: 'IN_PROGRESS',
  tasks: [],
  commands: [],
  currentRequestIds: [], //todo: replace with using requestIds from tasks
  logs: [],
  currentTaskId: null,
  POLL_INTERVAL: 4000,
  isSubmitDisabled: true,
  isBackButtonDisabled: true,
  stages: [],
  /**
   * List of statuses that inform about the end of request progress.
   * @type {String[]}
   */
  completedStatuses: ['COMPLETED', 'FAILED', 'TIMEDOUT', 'ABORTED'],
  currentPageRequestId: null,
  isSingleRequestPage: false,
  isCommandLevelRetry: Em.computed.not('isSingleRequestPage'),
  showRetry: false,
  /**
   * Show whether tasks data was loaded
   * @type {Boolean}
   */
  isLoading: false,

  k: Em.K,
  /**
   *  tasksMessagesPrefix should be overloaded by any controller including the mixin
   */
  tasksMessagesPrefix: '',

  loadStep: function () {
    this.clearStep();
    var self = this;
    if (!self.isSingleRequestPage) {
      this.initStep();
    } else {
      var requestIds = this.get('content.tasksRequestIds');
      var currentRequestId = requestIds && requestIds[0][0];
      if (!currentRequestId) {
        this.set('isLoaded', false);
        this.submitRequest();
      } else {
        self.set('currentPageRequestId', currentRequestId);
        self.doPollingForPageRequest();
      }
    }
  },

  initStep: function () {
    this.initializeTasks();
    if (!this.isSingleRequestPage) {
      this.loadTasks();
    }
    this.addObserver('tasks.@each.status', this, 'onTaskStatusChange');
    if (this.isSingleRequestPage) {
      var dfd = $.Deferred();
      var dfdObject = {
        deferred: dfd,
        isJqueryPromise: true
      };
      this.onTaskStatusChange(dfdObject);
      return dfd.promise();
    } else {
      this.onTaskStatusChange();
    }
  },

  clearStep: function () {
    this.removeObserver('tasks.@each.status', this, 'onTaskStatusChange');
    this.set('isSubmitDisabled', true);
    this.set('isBackButtonDisabled', true);
    this.set('tasks', []);
    this.set('currentRequestIds', []);
    this.set('isLoaded', false);
  },

  /**
   * Clear stages info for single page request.
   */
  clearStage: function() {
    this.setDBProperties({
      tasksRequestIds: null,
      tasksStatuses: null
    });
    this.set('showRetry', false);
    this.set('content.tasksRequestIds', null);
    this.set('content.tasksStatuses', null);
    this.set('content.currentTaskId', null);
    this.get('stages').clear();
  },

  retry: function () {
    this.set('showRetry', false);
    this.get('tasks').setEach('status','PENDING');
    this.loadStep();
  },

  submitRequest: function () {
    var self = this;
    return App.ajax.send({
      name: this.get('request.ajaxName'),
      data: this.get('request.ajaxData'),
      sender: this,
      error: 'onSingleRequestError',
      success: 'submitRequestSuccess',
      kdcCancelHandler: function() {
        self.set('status', 'FAILED');
        self.set('isLoaded', true);
        self.set('showRetry', true);
      }
    });
  },

  submitRequestSuccess: function(data, result, request) {
    if (data) {
      this.set('currentPageRequestId', data.Requests.id);
      this.doPollingForPageRequest();
    } else {
      //Step has been successfully completed
      if (request.status === 200) {
        this.set('status', 'COMPLETED');
        this.set('isSubmitDisabled', false);
        this.set('isLoaded', true);
      }
    }
  },

  doPollingForPageRequest: function () {
    App.ajax.send({
      name: 'admin.poll.kerberize.cluster.request',
      sender: this,
      data: {
        requestId: this.get('currentPageRequestId')
      },
      success: 'initializeStages'
    });
  },

  initializeStages: function (data) {
    var self = this;
    var stages = [];
    this.set('logs', []);
    data.stages.forEach(function (_stage) {
      stages.pushObject(Em.Object.create(_stage.Stage));
    }, this);
    if (!this.get('stages').length) {
      this.get('stages').pushObjects(stages);
      this.initStep().done(function(){
        self.updatePageWithPolledData(data);
      });
    } else {
      this.updatePageWithPolledData(data);
    }
  },

  updatePageWithPolledData: function(data) {
    // If all tasks completed no need to update each task status.
    // Preferable to skip polling of data for completed tasks after page refresh.
    if (this.get('status') === 'COMPLETED') return;
    var self = this;
    var tasks = [];
    var currentPageRequestId = this.get('currentPageRequestId');
    var currentTaskId = this.get('currentTaskId');
    var currentTask = this.get('tasks').findProperty('id', currentTaskId);
    var currentStage =  data.stages.findProperty('Stage.stage_id', currentTask.get('stageId'));
    var tasksInCurrentStage =  currentStage.tasks;
    this.set('logs',tasksInCurrentStage);

    this.setRequestIds(this.get('currentTaskId'), [this.get('currentPageRequestId')]);

    if (!tasksInCurrentStage.someProperty('Tasks.status', 'PENDING') && !tasksInCurrentStage.someProperty('Tasks.status', 'QUEUED') && !tasksInCurrentStage.someProperty('Tasks.status', 'IN_PROGRESS')) {
      this.set('currentRequestIds', []);
      if (tasksInCurrentStage.someProperty('Tasks.status', 'FAILED') || tasksInCurrentStage.someProperty('Tasks.status', 'TIMEDOUT') || tasksInCurrentStage.someProperty('Tasks.status', 'ABORTED')) {
        this.setTaskStatus(currentTaskId, 'FAILED');
      } else {
        this.setTaskStatus(currentTaskId, 'COMPLETED');
      }
    } else {
      var completedActions = tasksInCurrentStage.filterProperty('Tasks.status', 'COMPLETED').length
        + tasksInCurrentStage.filterProperty('Tasks.status', 'FAILED').length
        + tasksInCurrentStage.filterProperty('Tasks.status', 'ABORTED').length
        + tasksInCurrentStage.filterProperty('Tasks.status', 'TIMEDOUT').length;
      var queuedActions = tasksInCurrentStage.filterProperty('Tasks.status', 'QUEUED').length;
      var inProgressActions = tasksInCurrentStage.filterProperty('Tasks.status', 'IN_PROGRESS').length;
      var progress = Math.floor(((queuedActions * 0.09) + (inProgressActions * 0.35) + completedActions ) / tasksInCurrentStage.length * 100);
      this.get('tasks').findProperty('id', currentTaskId).set('progress', progress);
    }

    // start polling if current request not completed
    if (!(this.get('completedStatuses').contains(this.get('status')))) {
      window.setTimeout(function () {
        self.doPollingForPageRequest();
      }, self.POLL_INTERVAL);
    }
  },

  initializeTasks: function () {
    var self = this;
    var commands = this.isSingleRequestPage ? this.get('stages') : this.get('commands');
    var currentStep = App.router.get(this.get('content.controllerName') + '.currentStep');
    var tasksMessagesPrefix = this.get('tasksMessagesPrefix');
    // check that all stages have been completed for single request type
    var allStagesCompleted = commands.everyProperty('status', 'COMPLETED');
    for (var i = 0; i < commands.length; i++) {
      this.get('tasks').pushObject(Ember.Object.create({
        title: self.isSingleRequestPage ? commands[i].get('context') : Em.I18n.t(tasksMessagesPrefix + currentStep + '.task' + i + '.title'),
        // set COMPLETED status for task if all stages completed successfully
        status: allStagesCompleted ? 'COMPLETED' : 'PENDING',
        id: i,
        stageId: self.isSingleRequestPage ? commands[i].get('stage_id') : null,
        command: self.isSingleRequestPage ? 'k' : commands[i],
        showRetry: false,
        showRollback: false,
        name: self.isSingleRequestPage ? commands[i].get('context') : Em.I18n.t(tasksMessagesPrefix + currentStep + '.task' + i + '.title'),
        displayName: self.isSingleRequestPage ? commands[i].get('context') : Em.I18n.t(tasksMessagesPrefix + currentStep + '.task' + i + '.title'),
        progress: 0,
        isRunning: false,
        requestIds: self.isSingleRequestPage ? [this.get('stages')[0].request_id] : []
      }));
    }
    this.set('isLoaded', true);
  },

  loadTasks: function () {
    var self = this;
    var loadedStatuses = this.get('content.tasksStatuses');
    var loadedRequestIds = this.get('content.tasksRequestIds');
    if (loadedStatuses && loadedStatuses.length === this.get('tasks').length) {
      this.get('tasks').forEach(function (task, i) {
        self.setTaskStatus(task.get('id'), loadedStatuses[i]);
        self.setRequestIds(task.get('id'), loadedRequestIds[i]);
      });
      if (loadedStatuses.contains('IN_PROGRESS')) {
        var curTaskId = this.get('tasks')[loadedStatuses.indexOf('IN_PROGRESS')].get('id');
        this.set('currentRequestIds', this.get('content.requestIds'));
        this.set('currentTaskId', curTaskId);
        this.doPolling();
      } else if (loadedStatuses.contains('QUEUED')) {
        var curTaskId = this.get('tasks')[loadedStatuses.indexOf('QUEUED')].get('id');
        this.set('currentTaskId', curTaskId);
        this.runTask(curTaskId);
      }
    }
  },

  /**
   * remove tasks by command name
   */
  removeTasks: function(commands) {
    var tasks = this.get('tasks');

    commands.forEach(function(command) {
      var cmd = tasks.filterProperty('command', command);
      var index = null;

      if (cmd.length === 0) {
        return false;
      } else {
        index = tasks.indexOf( cmd[0] );
      }

      tasks.splice( index, 1 );
    });
  },

  setTaskStatus: function (taskId, status) {
    this.get('tasks').findProperty('id', taskId).set('status', status);
  },

  setTaskCanSkip: function (taskId, canSkip) {
    this.get('tasks').findProperty('id', taskId).set('canSkip', true);
  },

  setRequestIds: function (taskId, requestIds) {
    this.get('tasks').findProperty('id', taskId).set('requestIds', requestIds);
  },

  retryTask: function () {
    var task = this.get('tasks').findProperty('status', 'FAILED');
    task.set('showRetry', false);
    task.set('showRollback', false);
    this.set('isSubmitDisabled', true);
    this.set('isBackButtonDisabled', true);
    task.set('status', 'PENDING');
  },

  onTaskStatusChange: function (dfdObject) {
    var statuses = this.get('tasks').mapProperty('status');
    var tasksRequestIds = this.get('tasks').mapProperty('requestIds');
    var requestIds = this.get('currentRequestIds');
    // save task info
    App.router.get(this.get('content.controllerName')).saveTasksStatuses(statuses);
    App.router.get(this.get('content.controllerName')).saveTasksRequestIds(tasksRequestIds);
    App.router.get(this.get('content.controllerName')).saveRequestIds(requestIds);
    // call saving of cluster status asynchronous
    // synchronous executing cause problems in Firefox
    var successCallbackData;
    if (dfdObject && dfdObject.isJqueryPromise) {
      successCallbackData =  {deferred: dfdObject.deferred};
    }
    App.clusterStatus.setClusterStatus({
      clusterName: App.router.getClusterName(),
      clusterState: this.get('clusterDeployState'),
      wizardControllerName: this.get('content.controllerName'),
      localdb: App.db.data
    }, {successCallback: this.statusChangeCallback, sender: this, successCallbackData: successCallbackData});
  },

  /**
   * Method that called after saving persist data to server.
   * Switch task according its status.
   */
  statusChangeCallback: function (data) {
    if (!this.get('tasks').someProperty('status', 'IN_PROGRESS') && !this.get('tasks').someProperty('status', 'QUEUED') && !this.get('tasks').someProperty('status', 'FAILED')) {
      var nextTask = this.get('tasks').findProperty('status', 'PENDING');
      if (nextTask) {
        this.set('status', 'IN_PROGRESS');
        var taskStatus = this.isSingleRequestPage ? 'IN_PROGRESS' : 'QUEUED';
        this.setTaskStatus(nextTask.get('id'), taskStatus);
        this.set('currentTaskId', nextTask.get('id'));
        this.runTask(nextTask.get('id'));
      } else {
        this.set('status', 'COMPLETED');
        this.set('isSubmitDisabled', false);
        this.set('isBackButtonDisabled', false);
      }
    } else if (this.get('tasks').someProperty('status', 'FAILED')) {
      this.set('status', 'FAILED');
      this.set('isBackButtonDisabled', false);
      if (this.get('isCommandLevelRetry')) {
        this.get('tasks').findProperty('status', 'FAILED').set('showRetry', true);
      } else {
        this.set('showRetry', true);
      }

      if (this.get('tasks').someProperty('canSkip', true)) {
        this.get('tasks').findProperty('canSkip', true).set('showSkip', true);
      }

      if (App.supports.autoRollbackHA) {
        this.get('tasks').findProperty('status', 'FAILED').set('showRollback', true);
      }
    }
    this.get('tasks').filterProperty('status', 'COMPLETED').setEach('showRetry', false);
    this.get('tasks').filterProperty('status', 'COMPLETED').setEach('showRollback', false);
    this.get('tasks').filterProperty('status', 'COMPLETED').setEach('showSkip', false);
    this.get('tasks').filterProperty('status', 'IN_PROGRESS').setEach('showSkip', false);

    if (data && data.deferred) {
      data.deferred.resolve();
    }
  },

  /**
   * Run command of appropriate task
   */
  runTask: function (taskId) {
    this[this.get('tasks').findProperty('id', taskId).get('command')]();
  },

  onTaskError: function () {
    this.setTaskStatus(this.get('currentTaskId'), 'FAILED');
  },

  onTaskErrorWithSkip: function () {
    this.onTaskError();
    this.setTaskCanSkip(this.get('currentTaskId'), true);
  },

  onSingleRequestError: function (jqXHR, ajaxOptions, error, opt) {
    App.ajax.defaultErrorHandler(jqXHR, opt.url, opt.type, jqXHR.status);
    this.set('status', 'FAILED');
    this.set('isLoaded', true);
    this.set('showRetry', true);
  },

  onTaskCompleted: function () {
    this.setTaskStatus(this.get('currentTaskId'), 'COMPLETED');
  },

  /**
   * check whether component installed on specified hosts
   * @param {string} componentName
   * @param {string[]} hostNames
   * @return {$.ajax}
   */
  checkInstalledComponents: function (componentName, hostNames) {
    return App.ajax.send({
      name: 'host_component.installed.on_hosts',
      sender: this,
      data: {
        componentName: componentName,
        hostNames: hostNames.join(',')
      }
    });
  },

  /**
   * make server call to stop services
   * if stopListedServicesFlag == false; stop all services excluding the services passed as parameters
   * if stopListedServicesFlag == true; stop only services passed as parameters
   * if namenode or secondary namenode then stop all services
   * @param services, stopListedServicesFlag, stopAllServices
   * @returns {$.ajax}
   */
  stopServices: function (services, stopListedServicesFlag, stopAllServices) {
    var stopAllServices = stopAllServices || false;
    var stopListedServicesFlag = stopListedServicesFlag || false;
    var data = {
      'ServiceInfo': {
        'state': 'INSTALLED'
      }
    };
    if (stopAllServices) {
      data.context = "Stop all services";
    } else {
      if(!services || !services.length) {
        services = App.Service.find().mapProperty('serviceName').filter(function (service) {
          return service != 'HDFS';
        });
      }
      var servicesList;
      if (stopListedServicesFlag) {
        servicesList = services.join(',');
      } else {
        servicesList =  App.Service.find().mapProperty("serviceName").filter(function (s) {
          return !services.contains(s)
        }).join(',');
      }
      data.context = "Stop required services";
      data.urlParams = "ServiceInfo/service_name.in(" + servicesList + ")";
    }
    return App.ajax.send({
      name: 'common.services.update',
      sender: this,
      data: data,
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  /*tep4
   * make server call to start services
   * if startListedServicesFlag == false; start all services excluding the services passed as parameters
   * if startListedServicesFlag == true; start only services passed as parameters
   * if no parameters are passed; start all services
   * and run smoke tests if runSmokeTest is true
   * @param runSmokeTest
   * @param services
   * @param startListedServicesFlag
   * @returns {$.ajax}
   */
  startServices: function (runSmokeTest, services, startListedServicesFlag) {
    var startListedServicesFlag = startListedServicesFlag || false;
    var skipServiceCheck = App.router.get('clusterController.ambariProperties')['skip.service.checks'] === "true";
    var data = {
      'ServiceInfo': {
        'state': 'STARTED'
      }
    };
    var servicesList;
    if (services && services.length) {
      if (startListedServicesFlag) {
        servicesList = services.join(',');
      } else {
        servicesList =  App.Service.find().mapProperty("serviceName").filter(function (s) {
          return !services.contains(s)
        }).join(',');
      }
      data.context = "Start required services";
      data.urlParams = "ServiceInfo/service_name.in(" + servicesList + ")";
    } else {
      data.context = "Start all services";
    }

    if (runSmokeTest) {
      data.urlParams = data.urlParams ? data.urlParams + '&' : '';
      data.urlParams += 'params/run_smoke_test=' + !skipServiceCheck;
    }

    return App.ajax.send({
      name: 'common.services.update',
      sender: this,
      data: data,
      success: 'startPolling',
      error: 'startServicesErrorCallback'
    });
  },

  startServicesErrorCallback: function (jqXHR, ajaxOptions, error, opt) {
    App.ajax.defaultErrorHandler(jqXHR, opt.url, opt.type, jqXHR.status);
    this.onTaskError(jqXHR, ajaxOptions, error, opt);
  },

  /**
   * Create component on single or multiple hosts.
   *
   * @method createComponent
   * @param {string} componentName - name of the component
   * @param {(string|string[])} hostName - host/hosts where components should be installed
   * @param {string} serviceName - name of the services
   */
  createComponent: function (componentName, hostName, serviceName) {
    var hostNames = (Array.isArray(hostName)) ? hostName : [hostName];
    var self = this;

    this.set('showRetry', false);

    this.checkInstalledComponents(componentName, hostNames).then(function (data) {
      var hostsWithComponents = data.items.mapProperty('HostRoles.host_name');
      var result = hostNames.map(function(item) {
        return {
          componentName: componentName,
          hostName: item,
          hasComponent: hostsWithComponents.contains(item)
        };
      });
      var hostsWithoutComponents = result.filterProperty('hasComponent', false).mapProperty('hostName');
      var taskNum = 1;
      var requestData = {
        "RequestInfo": {
          "query": hostsWithoutComponents.map(function(item) {
            return 'Hosts/host_name=' + item;
          }).join('|')
        },
        "Body": {
          "host_components": [
            {
              "HostRoles": {
                "component_name": componentName
              }
            }
          ]
        }
      };
      if (!!hostsWithoutComponents.length) {
        self.updateAndCreateServiceComponent(componentName).done(function () {
          App.ajax.send({
            name: 'wizard.step8.register_host_to_component',
            sender: self,
            data: {
              data: JSON.stringify(requestData),
              hostName: result.mapProperty('hostName'),
              componentName: componentName,
              serviceName: serviceName,
              taskNum: taskNum,
              cluster: App.get('clusterName')
            },
            success: 'onCreateComponent',
            error: 'onCreateComponent'
          });
        });
      } else {
        self.onCreateComponent(null, null, {
          hostName: result.mapProperty('hostName'),
          componentName: componentName,
          serviceName: serviceName,
          taskNum: taskNum
        }, self);
      }
    });
  },

  onCreateComponent: function () {
    var hostName = arguments[2].hostName;
    var componentName = arguments[2].componentName;
    var taskNum = arguments[2].taskNum;
    var serviceName = arguments[2].serviceName;
    this.updateComponent(componentName, hostName, serviceName, "Install", taskNum);
  },

  onCreateComponentError: function (error) {
    if (error.responseText.indexOf('org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException') !== -1) {
      this.onCreateComponent();
    } else {
      this.onTaskError();
    }
  },

  /**
   * Update component status on selected hosts.
   *
   * @param {string} componentName
   * @param {(string|string[]|null)} hostName - use null to update components on all hosts
   * @param {string} serviceName
   * @param {string} context
   * @param {number} taskNum
   * @returns {$.ajax}
   */
  updateComponent: function (componentName, hostName, serviceName, context, taskNum) {
    if (hostName && !(hostName instanceof Array)) {
      hostName = [hostName];
    }
    var state = context.toLowerCase() == "start" ? "STARTED" : "INSTALLED";
    return App.ajax.send({
      name: 'common.host_components.update',
      sender: this,
      data: {
        HostRoles: {
          state: state
        },
        query: 'HostRoles/component_name=' + componentName + (hostName ? '&HostRoles/host_name.in(' + hostName.join(',') + ')' : '') + '&HostRoles/maintenance_state=OFF',
        context: context + " " + App.format.role(componentName, false),
        hostName: hostName,
        taskNum: taskNum || 1,
        componentName: componentName,
        serviceName: serviceName
      },
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  /**
   * Update state for array of components of different services and on different hosts
   *
   * @param {Array} components - array of components object with fields serviceName, hostName and componentName
   * @param {String} state - new state to update
   */
  updateComponentsState: function (components, state) {
    components.forEach(function (component) {
      App.ajax.send({
        name: 'common.host.host_component.update',
        sender: this,
        data: {
          hostName: component.hostName,
          serviceName: component.serviceName,
          componentName: component.componentName,
          HostRoles: {
            state: state
          },
          taskNum: components.length
        },
        success: 'startPolling',
        error: 'onTaskError'
      });
    }, this)
  },

  startPolling: function (data) {
    if (data) {
      this.get('currentRequestIds').push(data.Requests.id);
      var tasksCount = arguments[2].taskNum || 1;
      if (tasksCount === this.get('currentRequestIds').length) {
        this.setRequestIds(this.get('currentTaskId'), this.get('currentRequestIds'));
        this.doPolling();
      }
    } else {
      this.onTaskCompleted();
    }
  },

  doPolling: function () {
    this.setTaskStatus(this.get('currentTaskId'), 'IN_PROGRESS');
    var requestIds = this.get('currentRequestIds');
    this.set('logs', []);
    for (var i = 0; i < requestIds.length; i++) {
      App.ajax.send({
        name: 'background_operations.get_by_request',
        sender: this,
        data: {
          requestId: requestIds[i]
        },
        success: 'parseLogs',
        error: 'onTaskError'
      });
    }
  },

  parseLogs: function (logs) {
    this.get('logs').pushObject(logs.tasks);
    if (this.get('currentRequestIds').length === this.get('logs').length) {
      var tasks = [];
      this.get('logs').forEach(function (logs) {
        tasks.pushObjects(logs);
      }, this);
      var self = this;
      var currentTaskId = this.get('currentTaskId');
      if (!tasks.someProperty('Tasks.status', 'PENDING') && !tasks.someProperty('Tasks.status', 'QUEUED') && !tasks.someProperty('Tasks.status', 'IN_PROGRESS')) {
        this.set('currentRequestIds', []);
        if (tasks.someProperty('Tasks.status', 'FAILED') || tasks.someProperty('Tasks.status', 'TIMEDOUT') || tasks.someProperty('Tasks.status', 'ABORTED')) {
          this.setTaskStatus(currentTaskId, 'FAILED');
        } else {
          this.setTaskStatus(currentTaskId, 'COMPLETED');
        }
      } else {
        var actionsPerHost = tasks.length;
        var completedActions = tasks.filterProperty('Tasks.status', 'COMPLETED').length
          + tasks.filterProperty('Tasks.status', 'FAILED').length
          + tasks.filterProperty('Tasks.status', 'ABORTED').length
          + tasks.filterProperty('Tasks.status', 'TIMEDOUT').length;
        var queuedActions = tasks.filterProperty('Tasks.status', 'QUEUED').length;
        var inProgressActions = tasks.filterProperty('Tasks.status', 'IN_PROGRESS').length;
        var progress = Math.floor(((queuedActions * 0.09) + (inProgressActions * 0.35) + completedActions ) / actionsPerHost * 100);
        this.get('tasks').findProperty('id', currentTaskId).set('progress', progress);
        window.setTimeout(function () {
          self.doPolling();
        }, self.POLL_INTERVAL);
      }
    }
  },

  showHostProgressPopup: function (event) {
    if (!['IN_PROGRESS', 'FAILED', 'COMPLETED'].contains(Em.get(event.context, 'status')) || !event.contexts[0].requestIds.length) {
      return;
    }
    var popupTitle = event.contexts[0].title,
     requestIds = event.contexts[0].requestIds,
     stageId = event.contexts[0].stageId,
     hostProgressPopupController = App.router.get('highAvailabilityProgressPopupController');
    hostProgressPopupController.initPopup(popupTitle, requestIds, this, true, stageId);
  },

  done: function () {
    if (!this.get('isSubmitDisabled')) {
      this.set('isSubmitDisabled', true);
      this.removeObserver('tasks.@each.status', this, 'onTaskStatusChange');
      App.router.send('next');
    }
  },

  back: function () {
    if (!this.get('isBackButtonDisabled')) {
      this.set('isBackButtonDisabled', true);
      this.removeObserver('tasks.@each.status', this, 'onTaskStatusChange');
      App.router.send('back');
    }
  },

  /**
   * Delete component on single hosts.
   *
   * @method deleteComponent
   * @param {string} componentName - name of the component
   * @param {string} hostName - host from where components should be deleted
   */
  deleteComponent: function (componentName, hostName) {
    App.ajax.send({
      name: 'common.delete.host_component',
      sender: this,
      data: {
        componentName: componentName,
        hostName: hostName
      },
      success: 'onTaskCompleted',
      error: 'onDeleteHostComponentsError'
    });
  },

  onDeleteHostComponentsError: function (error) {
    // If the component does not exist on the host, NoSuchResourceException is thrown.
    // If NoSuchResourceException is thrown, there is no action required and operation should continue.
    if (error.responseText.indexOf('org.apache.ambari.server.controller.spi.NoSuchResourceException') !== -1) {
      this.onTaskCompleted();
    } else {
      this.onTaskError();
    }
  },

  /**
   * Same as <code>createComponent</code> but with kdc session check and status changes
   * when KDC auth dialog dissmised.
   *
   * @see createComponent
   */
  createInstallComponentTask: function(componentName, hostName, serviceName, options) {
    var self = this;
    App.get('router.mainAdminKerberosController').getKDCSessionState(function() {
      self.createComponent(componentName, hostName, serviceName);
    }, function() {
      self.onTaskError();
    });
  }
});
