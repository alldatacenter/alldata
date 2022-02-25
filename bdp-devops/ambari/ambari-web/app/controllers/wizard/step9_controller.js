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

App.WizardStep9Controller = Em.Controller.extend(App.ReloadPopupMixin, {

  name: 'wizardStep9Controller',

  /**
   *  Array of host Objects that are successfully registered on "Confirm Host Options" page
   *  <code>
   *    {
   *      name: {String} host name.
   *      status: {String} Current status of the host. This field is used in the step 9 view to set the css class of the
   *             host progress bar and set the appropriate message. Possible values: info, warning, failed, heartbeat_lost and success
   *      logTasks: {Array} Tasks that are scheduled on the host for the current request.
   *      message: {String} Message to be shown in front of the host name.
   *      progress: {Int} Progress of the tasks on the host. Amount of tasks completed for all the tasks scheduled on the host.
   *      isNoTasksForInstall: {Boolean} Gets set when no task is scheduled for the host on install phase.
   *    }
   *  </code>
   *  @type {Array.<{name: string, status: string, logTasks: object[], message: string, progress: number, isNoTasksForInstall: bool}>}
   */
  hosts: [],

  /**
   * Overall progress of <Install,Start and Test> page shown as progress bar on the top of the page
   * @type {string}
   */
  progress: '0',

  /**
   * Json file for the mock data to be used in mock mode
   * @type {string}
   */
  mockDataPrefix: '/data/wizard/deploy/5_hosts',

  /**
   * Current Request data polled from the API: api/v1/clusters/{clusterName}/requests/{RequestId}?fields=tasks/Tasks/command,
   * tasks/Tasks/exit_code,tasks/Tasks/start_time,tasks/Tasks/end_time,tasks/Tasks/host_name,tasks/Tasks/id,tasks/Tasks/role,
   * tasks/Tasks/status&minimal_response=true
   * @type {Object[]}
   */
  polledData: [],

  /**
   * This flag is only used in UI mock mode as a counter for number of polls.
   * @type {number}
   */
  numPolls: 1,

  /**
   * Interval in milliseconds between API calls While polling the request status for Install and, Start and Test tasks
   * @type {number}
   */
  POLL_INTERVAL: 4000,

  /**
   * Array of objects
   * <code>
   *   {
   *     hostName: {String} Name of host that has stopped heartbeating to ambari-server
   *     componentNames: {Sting} Name of all components that are on the host
   *   }
   * </code>
   * @type {Object[]}
   */
  hostsWithHeartbeatLost: [],

  /**
   * Flag is set in the start all services error callback function
   * @type {bool}
   */
  startCallFailed: false,

  /**
   * Status of the page. Possible values: <info, warning, failed and success>.
   * This property is used in the step-9 view for displaying the appropriate color of the overall progress bar and
   * the appropriate result message at the bottom of the page
   * @type {string}
   */
  status: 'info',

  skipServiceChecks: false,

  /**
   * This computed property is used to determine if the Next button and back button on the page should be disabled. The property is
   * used in the template to grey out Next and Back buttons. Although clicking on the greyed out button do trigger the event and
   * calls submit and back function of the controller.
   * @type {bool}
   */
  isSubmitDisabled: function () {
    var validStates = ['STARTED', 'START FAILED', 'START_SKIPPED'];
    var controllerName = this.get('content.controllerName');
    if (controllerName == 'addHostController' || controllerName == 'addServiceController') {
      validStates.push('INSTALL FAILED');
    }
    return !validStates.contains(this.get('content.cluster.status')) || App.get('router.btnClickInProgress');
  }.property('content.cluster.status'),

  isNextButtonDisabled: Em.computed.or('App.router.nextBtnClickInProgress', 'isSubmitDisabled'),

  /**
   * Observer function: Enables previous steps link if install task failed in installer wizard.
   * @method togglePreviousSteps
   */
  togglePreviousSteps: function () {
    if (App.get('testMode')) {
      return;
    }
    var installerController = App.router.get('installerController');
    if ('INSTALL FAILED' === this.get('content.cluster.status') && this.get('content.controllerName') == 'installerController') {
      installerController.setStepsEnable();
    } else {
      installerController.setLowerStepsDisable(9);
    }
  }.observes('content.cluster.status', 'content.controllerName'),

  /**
   * Computed property to determine if the Retry button should be made visible on the page.
   * @type {bool}
   */
  showRetry: Em.computed.equal('content.cluster.status', 'INSTALL FAILED'),

  /**
   * Observer function: Calls {hostStatusUpdates} function once with change in a host status from any registered hosts.
   * @method hostStatusObserver
   */
  hostStatusObserver: function () {
    Em.run.once(this, 'updateStatus');
  }.observes('hosts.@each.status'),

  /**
   * A flag that gets set with installation failure.
   * @type {bool}
   */
  installFailed: false,

  /**
   * Incremental flag that triggers an event in step 9 view to change the tasks related data and icons of hosts.
   * @type {number}
   */
  logTasksChangesCounter: 0,

  /**
   * <code>taskId</code> of current open task
   * @type {number}
   */
  currentOpenTaskId: 0,

  /**
   * <code>requestId</code> of current open task
   * @type {number}
   */
  currentOpenTaskRequestId: 0,

  /**
   * True if stage transition is completed.
   * On true, polling will be stopped.
   * @type {bool}s
   */
  parseHostInfo: false,

  isPolling: true,

  changeParseHostInfo: function (value) {
    this.set('parseHostInfo', value);
    this.parseHostInfoPolling();
  },

  parseHostInfoPolling: function () {
    var self = this;
    var result = this.get('parseHostInfo');
    if (!this.get('isPolling')) {
      if (this.get('content.cluster.status') === 'INSTALL FAILED') {
        this.isAllComponentsInstalled();
      }
      return;
    }
    if (result !== true) {
      window.setTimeout(function () {
        if (self.get('currentOpenTaskId')) {
          self.loadCurrentTaskLog();
        }
        if (App.router.loggedIn) {
          self.doPolling();
        }
      }, this.get('POLL_INTERVAL'));
    }
  },

  /**
   * Observer function: Updates {status} field of the controller.
   * @method updateStatus
   */
  updateStatus: function () {
    var status = 'info';
    if (this.get('hosts').someProperty('status', 'failed')
      || this.get('hosts').someProperty('status', 'heartbeat_lost')
      || this.get('startCallFailed')) {
      status = 'failed';
    } else if (this.get('hosts').someProperty('status', 'warning')) {
      if (this.isStepFailed()) {
        status = 'failed';
      } else {
        status = 'warning';
      }
    } else if (this.get('progress') == '100' && this.get('content.cluster.status') !== 'INSTALL FAILED') {
      status = 'success';
    }
    this.set('status', status);
  }.observes('progress'),

  /**
   * This function is called when a click event happens on Next button of "Install, Start and Test" page
   * @method submit
   */
  submit: function () {
    App.router.send('next');
  },

  /**
   * This function is called when a click event happens on back button of "Install, Start and Test" page
   * @method back
   */
  back: function () {
    if (!this.get('isSubmitDisabled')) {
      App.router.send('back');
    }
  },

  /**
   * navigateStep is called by App.WizardStep9View's didInsertElement and "retry" from router.
   * content.cluster.status can be:
   * PENDING: set upon successful transition from step 1 to step 2
   * INSTALLED: set upon successful completion of install phase as well as successful invocation of start services API
   * STARTED: set up on successful completion of start phase
   * INSTALL FAILED: set up upon encountering a failure in install phase
   * START FAILED: set upon unsuccessful invocation of start services API and also upon encountering a failure
   * during start phase

   * content.cluster.isCompleted
   * set to false upon successful transition from step 1 to step 2
   * set to true upon successful start of services in this step
   * note: looks like this is the same thing as checking content.cluster.status == 'STARTED'
   * @method navigateStep
   */
  navigateStep: function () {
    if (App.get('testMode')) {
      // this is for repeatedly testing out installs in test mode
      this.set('content.cluster.status', 'PENDING');
      this.set('content.cluster.isCompleted', false);
      this.set('content.cluster.requestId', 1);
    }
    var needPolling = false;
    var clusterStatus = this.get('content.cluster.status');
    if (this.get('content.cluster.isCompleted') === false) {
      if (clusterStatus !== 'INSTALL FAILED' && clusterStatus !== 'START FAILED') {
        needPolling = true;
      }
    } else {
      // handle STARTED
      // the cluster has successfully installed and started
      this.set('progress', '100');
    }
    this.loadStep();
    this.loadLogData(needPolling);
  },

  /**
   * This is called on initial page load, refreshes and retry event.
   * clears all in memory stale data for retry event.
   * @method clearStep
   */
  clearStep: function () {
    this.get('hosts').clear();
    this.set('hostsWithHeartbeatLost', []);
    this.set('startCallFailed', false);
    this.set('status', 'info');
    this.set('progress', '0');
    this.set('numPolls', 1);
  },

  /**
   * This is called on initial page load, refreshes and retry event.
   * @method loadStep
   */
  loadStep: function () {
    this.clearStep();
    this.loadHosts();
  },

  /**
   * Reset status and message of all hosts when retry install
   * @method resetHostsForRetry
   */
  resetHostsForRetry: function () {
    var hosts = this.get('content.hosts');
    for (var name in hosts) {
      if (hosts.hasOwnProperty(name)) {
        hosts[name].status = "pending";
        hosts[name].message = 'Waiting';
        hosts[name].isNoTasksForInstall = false;
      }
    }
    this.set('content.hosts', hosts);
  },

  /**
   * Sets the <code>hosts</code> array for the controller
   * @method loadHosts
   */
  loadHosts: function () {
    var hosts = this.get('content.hosts');

    //flag identify whether get all hosts or only NewHosts(newly added) hosts
    var getOnlyNewHosts = (this.get('content.controllerName') !== 'addServiceController');

    for (var index in hosts) {
      if (hosts[index].bootStatus === 'REGISTERED' && (!getOnlyNewHosts || !hosts[index].isInstalled)) {
        var hostInfo = App.HostInfo.create({
          name: hosts[index].name,
          status: (hosts[index].status) ? hosts[index].status : 'info',
          logTasks: [],
          message: (hosts[index].message) ? hosts[index].message : 'Waiting',
          progress: 0,
          isNoTasksForInstall: false
        });
        this.get('hosts').pushObject(hostInfo);
      }
    }
  },

  /**
   * Set new polled data
   * @param polledData sets the <code>polledData</code> object of the controller
   * @method replacePolledData
   */
  replacePolledData: function (polledData) {
    this.get('polledData').clear();
    this.set('polledData', polledData);
  },

  /**
   * Get the appropriate message for the host as per the running task
   * @param {object} task
   * @returns {String}
   * @method displayMessage
   */
  displayMessage: function (task) {
    var role = App.format.role(task.role, false);
    /* istanbul ignore next */
    switch (task.command) {
      case 'INSTALL':
        switch (task.status) {
          case 'PENDING':
            return Em.I18n.t('installer.step9.serviceStatus.install.pending') + role;
          case 'QUEUED' :
            return Em.I18n.t('installer.step9.serviceStatus.install.queued') + role;
          case 'IN_PROGRESS':
            return Em.I18n.t('installer.step9.serviceStatus.install.inProgress') + role;
          case 'COMPLETED' :
            return Em.I18n.t('installer.step9.serviceStatus.install.completed') + role;
          case 'FAILED':
            return Em.I18n.t('installer.step9.serviceStatus.install.failed') + role;
        }
        break;
      case 'UNINSTALL':
        switch (task.status) {
          case 'PENDING':
            return Em.I18n.t('installer.step9.serviceStatus.uninstall.pending') + role;
          case 'QUEUED' :
            return Em.I18n.t('installer.step9.serviceStatus.uninstall.queued') + role;
          case 'IN_PROGRESS':
            return Em.I18n.t('installer.step9.serviceStatus.uninstall.inProgress') + role;
          case 'COMPLETED' :
            return Em.I18n.t('installer.step9.serviceStatus.uninstall.completed') + role;
          case 'FAILED':
            return Em.I18n.t('installer.step9.serviceStatus.uninstall.failed') + role;
        }
        break;
      case 'START' :
        switch (task.status) {
          case 'PENDING':
            return Em.I18n.t('installer.step9.serviceStatus.start.pending') + role;
          case 'QUEUED' :
            return Em.I18n.t('installer.step9.serviceStatus.start.queued') + role;
          case 'IN_PROGRESS':
            return Em.I18n.t('installer.step9.serviceStatus.start.inProgress') + role;
          case 'COMPLETED' :
            return role + Em.I18n.t('installer.step9.serviceStatus.start.completed');
          case 'FAILED':
            return role + Em.I18n.t('installer.step9.serviceStatus.start.failed');
        }
        break;
      case 'STOP' :
        switch (task.status) {
          case 'PENDING':
            return Em.I18n.t('installer.step9.serviceStatus.stop.pending') + role;
          case 'QUEUED' :
            return Em.I18n.t('installer.step9.serviceStatus.stop.queued') + role;
          case 'IN_PROGRESS':
            return Em.I18n.t('installer.step9.serviceStatus.stop.inProgress') + role;
          case 'COMPLETED' :
            return role + Em.I18n.t('installer.step9.serviceStatus.stop.completed');
          case 'FAILED':
            return role + Em.I18n.t('installer.step9.serviceStatus.stop.failed');
        }
        break;
      case 'CUSTOM_COMMAND':
        role = App.format.commandDetail(task.command_detail, task.request_input, task.ops_display_name);
      case 'EXECUTE' :
      case 'SERVICE_CHECK' :
        switch (task.status) {
          case 'PENDING':
            return Em.I18n.t('installer.step9.serviceStatus.execute.pending') + role;
          case 'QUEUED' :
            return Em.I18n.t('installer.step9.serviceStatus.execute.queued') + role;
          case 'IN_PROGRESS':
            return Em.I18n.t('installer.step9.serviceStatus.execute.inProgress') + role;
          case 'COMPLETED' :
            return role + Em.I18n.t('installer.step9.serviceStatus.execute.completed');
          case 'FAILED':
            return role + Em.I18n.t('installer.step9.serviceStatus.execute.failed');
        }
        break;
      case 'ABORT' :
        switch (task.status) {
          case 'PENDING':
            return Em.I18n.t('installer.step9.serviceStatus.abort.pending') + role;
          case 'QUEUED' :
            return Em.I18n.t('installer.step9.serviceStatus.abort.queued') + role;
          case 'IN_PROGRESS':
            return Em.I18n.t('installer.step9.serviceStatus.abort.inProgress') + role;
          case 'COMPLETED' :
            return role + Em.I18n.t('installer.step9.serviceStatus.abort.completed');
          case 'FAILED':
            return role + Em.I18n.t('installer.step9.serviceStatus.abort.failed');
        }
    }
    return '';
  },

  /**
   * Run start/check services after installation phase.
   * Does Ajax call to start all services
   * @return {$.ajax|null}
   * @method launchStartServices
   */
  launchStartServices: function (callback) {
    var data = {};
    var name = '';
    callback = callback || Em.K;
    switch(this.get('content.controllerName')) {
      case 'addHostController':
        name = 'common.host_components.update';
        var hostnames = [];
        var hosts = this.get('wizardController').getDBProperty('hosts');
        for (var hostname in hosts) {
          if(this.get('hosts').findProperty('name', hostname)){
            hostnames.push(hostname);
          }
        }
        data = {
          "context": Em.I18n.t("requestInfo.startHostComponents"),
          "query": "HostRoles/component_name.in(" + App.get('components.slaves').join(',') + ")&HostRoles/state=INSTALLED&HostRoles/host_name.in(" + hostnames.join(',') + ")",
          "HostRoles": { "state": "STARTED"}
        };
        break;
      case 'addServiceController':
        var servicesList = this.get('content.services').filterProperty('isSelected').filterProperty('isInstalled', false).mapProperty('serviceName');
        if (servicesList.contains('OOZIE')) {
          servicesList = servicesList.concat(['HDFS', 'YARN', 'MAPREDUCE2']);
        }
        name = 'common.services.update';
        data = {
          "context": Em.I18n.t("requestInfo.startAddedServices"),
          "ServiceInfo": { "state": "STARTED" },
          "urlParams": "ServiceInfo/state=INSTALLED&ServiceInfo/service_name.in(" + servicesList.join(",") + ")&params/run_smoke_test=true&params/reconfigure_client=false"
        };
        break;
      default:
        name = 'common.services.update';
        data = {
          "context": Em.I18n.t("requestInfo.startServices"),
          "ServiceInfo": { "state": "STARTED" },
          "urlParams": "ServiceInfo/state=INSTALLED&params/run_smoke_test=" + !this.get('skipServiceChecks') + "&params/reconfigure_client=false"
        };
    }

    if (App.get('testMode')) {
      this.set('numPolls', 6);
    }

    return App.ajax.send({
      name: name,
      sender: this,
      data: data,
      success: 'launchStartServicesSuccessCallback',
      error: 'launchStartServicesErrorCallback'
    }).then(callback, callback);
  },

  /**
   * Success callback function for start services task.
   * @param {object} jsonData Contains Request id to poll.
   * @method launchStartServicesSuccessCallback
   */
  launchStartServicesSuccessCallback: function (jsonData) {
    var clusterStatus = {};
    if (jsonData) {
      var requestId = jsonData.Requests.id;
      clusterStatus = {
        status: 'INSTALLED',
        requestId: requestId,
        isStartError: false,
        isCompleted: false
      };
      this.hostHasClientsOnly(false);
      this.saveClusterStatus(clusterStatus);
    }
    else {
      this.hostHasClientsOnly(true);
      clusterStatus = {
        status: 'STARTED',
        isStartError: false,
        isCompleted: true
      };
      this.saveClusterStatus(clusterStatus);
      this.set('status', 'success');
      this.set('progress', '100');
    }
    // We need to do recovery if there is a browser crash
    App.clusterStatus.setClusterStatus({
      clusterState: 'SERVICE_STARTING_3',
      wizardControllerName: this.get('content.controllerName'),
      localdb: App.db.data
    });

    if (jsonData) {
      this.startPolling();
    }
  },

  /**
   * This function will be called for Add host wizard only.
   * @param {bool} jsonError Boolean is true when API to start services returns 200 ok and no json data
   * @method hostHasClientsOnly
   */
  hostHasClientsOnly: function (jsonError) {
    this.get('hosts').forEach(function (host) {
      var OnlyClients = true;
      var tasks = host.get('logTasks');
      tasks.forEach(function (task) {
        var component = App.StackServiceComponent.find().findProperty('componentName', task.Tasks.role);
        if (!(component && component.get('isClient'))) {
          OnlyClients = false;
        }
      });
      if (OnlyClients || jsonError) {
        host.set('status', 'success');
        host.set('progress', '100');
      }
    });
  },

  /**
   * Error callback function for start services task.
   * @param {object} jqXHR
   * @param {object} ajaxOptions
   * @param {string} error
   * @param {object} opt
   * @method launchStartServicesErrorCallback
   */
  launchStartServicesErrorCallback: function (jqXHR, ajaxOptions, error, opt) {
    this.set('startCallFailed', true);
    var clusterStatus = {
      status: 'INSTALL FAILED',
      isStartError: false,
      isCompleted: false
    };
    this.saveClusterStatus(clusterStatus);
    this.get('hosts').forEach(function (host) {
      host.set('progress', '100');
    });
    this.set('progress', '100');

    console.log("Error starting installed services: ", jqXHR, ajaxOptions, error, opt);
    App.ModalPopup.show({
      encodeBody: false,
      primary: Em.I18n.t('ok'),
      header: Em.I18n.t('installer.step9.service.start.header'),
      secondary: false,
      body: Em.I18n.t('installer.step9.service.start.failed'),
      'data-qa': 'start-failed-modal'
    });
  },

  /**
   * Marks a host's status as "success" if all tasks are in COMPLETED state
   * @method onSuccessPerHost
   */
  onSuccessPerHost: function (actions, contentHost) {
    var status = this.get('content.cluster.status');
    if (actions.everyProperty('Tasks.status', 'COMPLETED') && 
        (status === 'INSTALLED' || status === 'STARTED') || App.get('supports.skipComponentStartAfterInstall')) {
      contentHost.set('status', 'success');
    }
  },

  /**
   * marks a host's status as "warning" if at least one of the tasks is FAILED, ABORTED, or TIMEDOUT and marks host's status as "failed" if at least one master component install task is FAILED.
   * note that if the master failed to install because of ABORTED or TIMEDOUT, we don't mark it as failed, because this would mark all hosts as "failed" and makes it difficult for the user
   * to find which host FAILED occurred on, if any
   * @param actions {Array} of tasks retrieved from polled data
   * @param contentHost {Object} A host object
   * @method onErrorPerHost
   */
  onErrorPerHost: function (actions, contentHost) {
    if (!actions) return;
    if (actions.someProperty('Tasks.status', 'FAILED') || actions.someProperty('Tasks.status', 'ABORTED') || actions.someProperty('Tasks.status', 'TIMEDOUT')) {
      contentHost.set('status', 'warning');
    }
    if ((this.get('content.cluster.status') === 'PENDING' && actions.someProperty('Tasks.status', 'FAILED')) || (this.isMasterFailed(actions))) {
      contentHost.get('status') !== 'heartbeat_lost' ? contentHost.set('status', 'failed') : '';
    }
  },

  /**
   * Check if some master component is failed
   * @param {object} polledData data polled from API.
   * @returns {bool}  true if there is at least one FAILED task of master component install
   * @method isMasterFailed
   */
  isMasterFailed: function (polledData) {
    var result = false;
    polledData.filterProperty('Tasks.command', 'INSTALL').filterProperty('Tasks.status', 'FAILED').mapProperty('Tasks.role').forEach(
      function (role) {
        if (!App.get('components.slaves').contains(role)) {
          result = true;
        }
      }
    );
    return result;
  },

  /**
   * Mark a host status as in_progress if the any task on the host if either in IN_PROGRESS, QUEUED or PENDONG state.
   * @param actions {Array} of tasks retrieved from polled data
   * @param contentHost {Object} A host object
   * @method onInProgressPerHost
   */
  onInProgressPerHost: function (actions, contentHost) {
    var runningAction = actions.findProperty('Tasks.status', 'IN_PROGRESS');
    if (runningAction === undefined || runningAction === null) {
      runningAction = actions.findProperty('Tasks.status', 'QUEUED');
    }
    if (runningAction === undefined || runningAction === null) {
      runningAction = actions.findProperty('Tasks.status', 'PENDING');
    }
    if (runningAction !== null && runningAction !== undefined) {
      contentHost.set('status', 'in_progress');
      contentHost.set('message', this.displayMessage(runningAction.Tasks));
    }
  },

  /**
   * Calculate progress of tasks per host
   * @param {object} actions
   * @param {object} contentHost
   * @return {Number}
   * @method progressPerHost
   */
  progressPerHost: function (actions, contentHost) {
    var progress = 0;
    var actionsPerHost = actions.length;
    var completedActions = 0;
    var queuedActions = 0;
    var inProgressActions = 0;
    var installProgressFactor = App.get('supports.skipComponentStartAfterInstall') ? 100 : 33;
    actions.forEach(function (action) {
      completedActions += +(['COMPLETED', 'FAILED', 'ABORTED', 'TIMEDOUT'].contains(action.Tasks.status));
      queuedActions += +(action.Tasks.status === 'QUEUED');
      inProgressActions += +(action.Tasks.status === 'IN_PROGRESS');
    }, this);
    /** for the install phase (PENDING), % completed per host goes up to 33%; floor(100 / 3)
     * for the start phase (INSTALLED), % completed starts from 34%
     * when task in queued state means it's completed on 9%
     * in progress - 35%
     * completed - 100%
     */
    switch (this.get('content.cluster.status')) {
      case 'PENDING':
        progress = actionsPerHost ? (Math.ceil(((queuedActions * 0.09) + (inProgressActions * 0.35) + completedActions ) / actionsPerHost * installProgressFactor)) : installProgressFactor;
        break;
      case 'INSTALLED':
        progress = actionsPerHost ? (33 + Math.floor(((queuedActions * 0.09) + (inProgressActions * 0.35) + completedActions ) / actionsPerHost * 67)) : 100;
        break;
      default:
        progress = 100;
        break;
    }
    contentHost.set('progress', progress.toString());
    return progress;
  },

  /**
   * Check if step completed successfully
   * @param {object} polledData data retrieved from API
   * @returns {bool}
   * @method isSuccess
   */
  isSuccess: function (polledData) {
    return polledData.everyProperty('Tasks.status', 'COMPLETED');
  },

  /**
   * Check if step is failed
   * Return true if:
   *  1. any of the master/client components failed to install
   *  OR
   *  2. at least 50% of the slave host components for the particular service component fails to install
   *  @method isStepFailed
   */
  isStepFailed: function () {
    var failed = false;
    var polledData = this.get('polledData');
    polledData.filterProperty('Tasks.command', 'INSTALL').mapProperty('Tasks.role').uniq().forEach(function (role) {
      if (failed) {
        return;
      }
      var actionsPerRole = polledData.filterProperty('Tasks.role', role);
      if (App.get('components.slaves').contains(role)) {
        // check slave components for success factor.
        // partial failure for slave components are allowed.
        var actionsFailed = actionsPerRole.filterProperty('Tasks.status', 'FAILED');
        var actionsAborted = actionsPerRole.filterProperty('Tasks.status', 'ABORTED');
        var actionsTimedOut = actionsPerRole.filterProperty('Tasks.status', 'TIMEDOUT');
        if ((((actionsFailed.length + actionsAborted.length + actionsTimedOut.length) / actionsPerRole.length) * 100) > 50) {
          failed = true;
        }
      } else if (actionsPerRole.someProperty('Tasks.status', 'FAILED') || actionsPerRole.someProperty('Tasks.status', 'ABORTED') ||
        actionsPerRole.someProperty('Tasks.status', 'TIMEDOUT')) {
        // check non-salve components (i.e., masters and clients).  all of these must be successfully installed.
        failed = true;
      }
    }, this);
    return failed;
  },

  /**
   * polling from ui stops only when no action has 'PENDING', 'QUEUED' or 'IN_PROGRESS' status
   * Makes a state transition
   * PENDING -> INSTALLED
   * PENDING -> INSTALL FAILED
   * INSTALLED -> STARTED
   * INSTALLED -> START_FAILED
   * @param polledData json data retrieved from API
   * @method setFinishState
   */
  setFinishState: function (polledData) {
    if (this.get('content.cluster.status') === 'INSTALLED') {
      Em.run.next(this, function(){
        this.changeParseHostInfo(this.isServicesStarted(polledData));
      });
      return;
    } else if (this.get('content.cluster.status') === 'PENDING') {
      this.setIsServicesInstalled(polledData);
      return;
    } else if (this.get('content.cluster.status') === 'INSTALL FAILED' || this.get('content.cluster.status') === 'START FAILED'
      || this.get('content.cluster.status') === 'STARTED') {
      this.set('progress', '100');
      this.changeParseHostInfo(true);
      return;
    }
    this.changeParseHostInfo(true);
  },

  /**
   * @param polledData JSON data retrieved from API
   * @returns {bool} Has "Start All Services" request completed successfully
   * @method isServicesStarted
   */
  isServicesStarted: function (polledData) {
    var clusterStatus = {};
    if (!polledData.someProperty('Tasks.status', 'PENDING') && !polledData.someProperty('Tasks.status', 'QUEUED') && !polledData.someProperty('Tasks.status', 'IN_PROGRESS')) {
      this.set('progress', '100');
      clusterStatus = {
        status: 'INSTALLED',
        requestId: this.get('content.cluster.requestId'),
        isCompleted: true
      };
      if (this.isSuccess(polledData)) {
        clusterStatus.status = 'STARTED';
        var serviceStartTime = App.dateTime();
        clusterStatus.installTime = ((parseInt(serviceStartTime) - parseInt(this.get('content.cluster.installStartTime'))) / 60000).toFixed(2);
      }
      else {
        clusterStatus.status = 'START FAILED'; // 'START FAILED' implies to step10 that installation was successful but start failed
      }
      this.saveClusterStatus(clusterStatus);
      this.saveInstalledHosts(this);
      return true;
    }
    return false;
  },

  /**
   * @param polledData Json data retrieved from API
   * @method setIsServicesInstalled
   */
  setIsServicesInstalled: function (polledData) {
    var clusterStatus = {};
    var self = this;

    if (!polledData.someProperty('Tasks.status', 'PENDING') && !polledData.someProperty('Tasks.status', 'QUEUED') && !polledData.someProperty('Tasks.status', 'IN_PROGRESS')) {
      clusterStatus = {
        status: 'PENDING',
        requestId: this.get('content.cluster.requestId'),
        isCompleted: false
      };
      if (this.get('status') === 'failed') {
        clusterStatus.status = 'INSTALL FAILED';
        this.saveClusterStatus(clusterStatus);
        this.setProperties({
          progress: '100',
          isPolling: false
        });
        this.get('hosts').forEach(function (host) {
          host.set('progress', '100');
        });
        this.isAllComponentsInstalled().done(function () {
          self.changeParseHostInfo(false);
        });
        return;
      }
      if (App.get('supports.skipComponentStartAfterInstall')) {
        clusterStatus.status = 'START_SKIPPED';
        clusterStatus.isCompleted = true;
        this.saveClusterStatus(clusterStatus);
        this.get('hosts').forEach(function (host) {
          host.set('status', 'success');
          host.set('message', Em.I18n.t('installer.step9.host.status.success'));
          host.set('progress', '100');
        });
        this.set('progress', '100');
        self.saveInstalledHosts(self);
        this.changeParseHostInfo(true);
      } else {
        this.set('progress', '34');
        this.isAllComponentsInstalled().done(function () {
          self.saveInstalledHosts(self);
          self.changeParseHostInfo(true);
        });
        return;
      }
    }
    this.changeParseHostInfo(false);
  },

  /**
   * This is done at HostRole level.
   * @param {Ember.Enumerable} tasksPerHost
   * @param {Object} host
   * @method setLogTasksStatePerHost
   */
  setLogTasksStatePerHost: function (tasksPerHost, host) {
    tasksPerHost.forEach(function (_task) {
      var task = host.logTasks.findProperty('Tasks.id', _task.Tasks.id);
      if (task) {
        task.Tasks.status = _task.Tasks.status;
        task.Tasks.start_time = _task.Tasks.start_time;
        task.Tasks.end_time = _task.Tasks.end_time;
        task.Tasks.exit_code = _task.Tasks.exit_code;
      } else {
        host.logTasks.pushObject(_task);
      }
    }, this);
  },

  /**
   * Parses the Json data retrieved from API and sets the task on the host of {hosts} array binded to template
   * @param polledData Json data retrieved from API
   * @method setParseHostInfo
   */
  setParseHostInfo: function (polledData) {
    var self = this;
    var totalProgress = 0;
    var tasksData = polledData.tasks || [];
    var requestId = this.get('content.cluster.requestId');
    tasksData.setEach('Tasks.request_id', requestId);
    if (polledData.Requests && polledData.Requests.id && polledData.Requests.id != requestId) {
      // We don't want to use non-current requestId's tasks data to
      // determine the current install status.
      // Also, we don't want to keep polling if it is not the
      // current requestId.
      this.changeParseHostInfo(false);
      return;
    }
    this.replacePolledData(tasksData);
    var tasksHostMap = {};
    tasksData.forEach(function (task) {
      if (tasksHostMap[task.Tasks.host_name]) {
        tasksHostMap[task.Tasks.host_name].push(task);
      } else {
        tasksHostMap[task.Tasks.host_name] = [task];
      }
    });

    this.get('hosts').forEach(function (_host) {
      var actionsPerHost = tasksHostMap[_host.name] || []; // retrieved from polled Data
      if (actionsPerHost.length === 0) {
        if (this.get('content.cluster.status') === 'PENDING' || this.get('content.cluster.status') === 'INSTALL FAILED') {
          _host.set('progress', '33');
          _host.set('isNoTasksForInstall', true);
          _host.set('status', 'pending');
        }
        if (this.get('content.cluster.status') === 'INSTALLED' || this.get('content.cluster.status') === 'FAILED' || App.get('supports.skipComponentStartAfterInstall')) {
          _host.set('progress', '100');
          _host.set('status', 'success');
        }
      } else {
        _host.set('isNoTasksForInstall', false);
      }
      this.setLogTasksStatePerHost(actionsPerHost, _host);
      this.onSuccessPerHost(actionsPerHost, _host);     // every action should be a success
      this.onErrorPerHost(actionsPerHost, _host);     // any action should be a failure
      this.onInProgressPerHost(actionsPerHost, _host);  // current running action for a host
      totalProgress += self.progressPerHost(actionsPerHost, _host);
      if (_host.get('progress') == '33' && _host.get('status') != 'failed' && _host.get('status') != 'warning') {
        _host.set('message', this.t('installer.step9.host.status.nothingToInstall'));
        _host.set('status', 'pending');
      }
    }, this);
    this.set('logTasksChangesCounter', this.get('logTasksChangesCounter') + 1);
    totalProgress = Math.floor(totalProgress / this.get('hosts.length'));
    this.set('progress', totalProgress.toString());
    this.setFinishState(tasksData);
  },

  /**
   * Starts polling to the API
   * @method startPolling
   */
  startPolling: function () {
    this.set('isSubmitDisabled', true);
    this.doPolling();
  },

  /**
   * This function calls API just once to fetch log data of all tasks.
   * @method loadLogData
   */
  loadLogData: function (startPolling) {
    var requestsId = this.get('wizardController').getDBProperty('cluster').oldRequestsId;
    if (App.get('testMode')) {
      this.set('POLL_INTERVAL', 1);
    }
    this.getLogsByRequest(!!startPolling, requestsId[requestsId.length-1]);
  },

  /**
   * Load form server <code>stderr, stdout</code> of current open task
   * @method loadCurrentTaskLog
   */
  loadCurrentTaskLog: function () {
    var taskId = this.get('currentOpenTaskId');
    var requestId = this.get('currentOpenTaskRequestId');
    var clusterName = this.get('content.cluster.name');
    if (!taskId) {
      return;
    }
    App.ajax.send({
      name: 'background_operations.get_by_task',
      sender: this,
      data: {
        'taskId': taskId,
        'requestId': requestId,
        'clusterName': clusterName
      },
      success: 'loadCurrentTaskLogSuccessCallback',
      error: 'loadCurrentTaskLogErrorCallback'
    });
  },

  /**
   * success callback function for getting log data of the opened task
   * @param {object} data
   * @method loadCurrentTaskLogSuccessCallback
   */
  loadCurrentTaskLogSuccessCallback: function (data) {
    var taskId = this.get('currentOpenTaskId');
    if (taskId) {
      var host = this.get('hosts').findProperty('name', data.Tasks.host_name);
      if (host) {
        var currentTask = host.get('logTasks').findProperty('Tasks.id', data.Tasks.id);
        if (currentTask) {
          currentTask.Tasks.stderr = data.Tasks.stderr;
          currentTask.Tasks.stdout = data.Tasks.stdout;
          currentTask.Tasks.output_log = data.Tasks.output_log;
          currentTask.Tasks.error_log = data.Tasks.error_log;
        }
      }
    }
    this.set('logTasksChangesCounter', this.get('logTasksChangesCounter') + 1);
  },

  /**
   * Error callback function for getting log data of the opened task
   * @method loadCurrentTaskLogErrorCallback
   */
  loadCurrentTaskLogErrorCallback: function () {
    this.set('currentOpenTaskId', 0);
  },

  /**
   * Function polls the API to retrieve data for the request.
   * @param {bool} polling whether to continue polling for status or not
   * @param {number} requestId
   * @method getLogsByRequest
   * @return {$.ajax|null}
   */
  getLogsByRequest: function (polling, requestId) {
    return App.ajax.send({
      name: 'wizard.step9.load_log',
      sender: this,
      data: {
        polling: polling,
        cluster: this.get('content.cluster.name'),
        requestId: requestId,
        numPolls: this.get('numPolls'),
        callback: this.getLogsByRequest,
        args: [polling, requestId],
        timeout: 3000
      },
      success: 'reloadSuccessCallback',
      error: 'reloadErrorCallback'
    });
  },

  /**
   * Success callback for get log by request
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method reloadSuccessCallback
   */
  reloadSuccessCallback: function (data, opt, params) {
    var parsedData = jQuery.parseJSON(data);
    this._super();
    this.set('isPolling', params.polling);
    this.setParseHostInfo(parsedData);
  },

  /**
   * Error-callback for get log by request
   * @param {object} jqXHR
   * @param {string} ajaxOptions
   * @param {string} error
   * @param {object} opt
   * @param {object} params
   * @method reloadErrorCallback
   */
  reloadErrorCallback: function (jqXHR, ajaxOptions, error, opt, params) {
    if (jqXHR.status) {
      this.closeReloadPopup();
      this.loadLogData(true);
    } else {
      this._super(jqXHR, ajaxOptions, error, opt, params);
    }
  },

  /**
   * Delegates the function call to {getLogsByRequest} with appropriate params
   * @method doPolling
   */
  doPolling: function () {
    var requestId = this.get('content.cluster.requestId');
    if (App.get('testMode')) {
      this.incrementProperty('numPolls');
    }
    this.getLogsByRequest(true, requestId);
  },

  /**
   * Check that all components are in INSTALLED state before issuing start command
   * @method isAllComponentsInstalled
   * @return {$.ajax|null}
   */
  isAllComponentsInstalled: function () {
    var dfd = $.Deferred();
    App.ajax.send({
      name: 'wizard.step9.installer.get_host_status',
      sender: this,
      data: {
        cluster: this.get('content.cluster.name')
      },
      success: 'isAllComponentsInstalledSuccessCallback',
      error: 'isAllComponentsInstalledErrorCallback'
    }).complete(function () {
      dfd.resolve();
    });
    return dfd.promise();
  },

  /**
   * Success callback function for API checking host state and host_components state.
   * @param {Object} jsonData
   * @method isAllComponentsInstalledSuccessCallback
   */
  isAllComponentsInstalledSuccessCallback: function (jsonData) {
    var clusterStatus = {
      status: 'INSTALL FAILED',
      isStartError: true,
      isCompleted: false
    };
    var usedHostWithHeartbeatLost = false;
    var hostsWithHeartbeatLost = [];
    var usedHosts = this.get('content.masterComponentHosts').filterProperty('isInstalled', false).mapProperty('hostName');
    this.get('content.slaveComponentHosts').forEach(function(component) {
      usedHosts = usedHosts.concat(component.hosts.filterProperty('isInstalled', false).mapProperty('hostName'));
    });
    usedHosts = usedHosts.uniq();
    jsonData.items.filterProperty('Hosts.host_state', 'HEARTBEAT_LOST').forEach(function (host) {
      var hostComponentObj = {hostName: host.Hosts.host_name};
      var componentArr = [];
      host.host_components.forEach(function (_hostComponent) {
        var componentName = App.format.role(_hostComponent.HostRoles.component_name, false);
        componentArr.pushObject(componentName);
      }, this);
      hostComponentObj.componentNames = stringUtils.getFormattedStringFromArray(componentArr);
      hostsWithHeartbeatLost.pushObject(hostComponentObj);
      if (!usedHostWithHeartbeatLost && usedHosts.contains(host.Hosts.host_name)) {
        usedHostWithHeartbeatLost = true;
      }
    }, this);
    this.set('hostsWithHeartbeatLost', hostsWithHeartbeatLost);
    if (usedHostWithHeartbeatLost) {
      this.get('hosts').forEach(function (host) {
        if (hostsWithHeartbeatLost.someProperty(('hostName'), host.get('name'))) {
          host.set('status', 'heartbeat_lost');
        } else if (host.get('status') !== 'failed' && host.get('status') !== 'warning') {
          host.set('message', Em.I18n.t('installer.step9.host.status.startAborted'));
        }
        host.set('progress', '100');
      });
      this.set('progress', '100');
      this.saveClusterStatus(clusterStatus);
    } else if (this.get('content.cluster.status') === 'PENDING' && this.get('isPolling')) {
      if (App.get('supports.skipComponentStartAfterInstall')) {
        this.set('progress', '100');
        this.changeParseHostInfo(true);
      } else {
        this.launchStartServices();
      }
    }
  },

  /**
   * Error callback function for API checking host state and host_components state
   * @method isAllComponentsInstalledErrorCallback
   */
  isAllComponentsInstalledErrorCallback: function () {
    var clusterStatus = {
      status: 'INSTALL FAILED',
      isStartError: true,
      isCompleted: false
    };
    this.set('progress', '100');
    this.get('hosts').forEach(function (host) {
      if (host.get('status') !== 'failed' && host.get('status') !== 'warning') {
        host.set('message', Em.I18n.t('installer.step9.host.status.startAborted'));
        host.set('progress', '100');
      }
    });
    this.saveClusterStatus(clusterStatus);
  },

  /**
   * save cluster status in the parentController and localdb
   * @param {object} clusterStatus
   * @method saveClusterStatus
   */
  saveClusterStatus: function (clusterStatus) {
    if (!App.get('testMode')) {
      App.router.get(this.get('content.controllerName')).saveClusterStatus(clusterStatus);
    }
    else {
      this.set('content.cluster', clusterStatus);
    }
  },

  /**
   * save cluster status in the parentController and localdb
   * @param {object} context
   * @method saveInstalledHosts
   */
  saveInstalledHosts: function (context) {
    if (!App.get('testMode')) {
      App.router.get(this.get('content.controllerName')).saveInstalledHosts(context);
    }
  },

  /**
   * Load ambari property to determine if we should run service checks on deploy
   */
  loadDoServiceChecksFlag: function () {
    var def = $.Deferred();
    App.ajax.send({
      name: 'ambari.service',
      sender: this,
      data: {
        fields: '?fields=RootServiceComponents/properties/skip.service.checks'
      },
      success: 'loadDoServiceChecksFlagSuccessCallback'
    }).complete(function(){
      def.resolve();
    });
    return def.promise();
  },

  /**
   *  Callback function Load ambari property to determine if we should run service checks on deploy
   * @param {Object} data
   * @method loadDoServiceChecksFlagSuccessCallback
   */
  loadDoServiceChecksFlagSuccessCallback: function (data) {
    var properties = Em.get(data, 'RootServiceComponents.properties');
    if(properties && properties.hasOwnProperty('skip.service.checks')){
      this.set('skipServiceChecks', properties['skip.service.checks'] === 'true');
    }
  }

});
