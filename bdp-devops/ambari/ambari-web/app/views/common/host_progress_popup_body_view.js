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
var batchUtils = require('utils/batch_scheduled_requests');
var fileUtils = require('utils/file_utils');

/**
 * @typedef {object} TaskRelationObject
 * @property {string} type relation type 'service', 'component'
 * @property {string} [value] optional value of relation e.g. name of component or service
 */

/**
 * Option for "filter by state" dropdown
 * @typedef {object} progressPopupCategoryObject
 * @property {string} value "all|pending|in progress|failed|completed|aborted|timedout"
 * @property {number} count number of items with <code>state</code> equal to <code>this.value</code>
 * @property {string} labelPath key in the messages.js
 * @property {string} label localized label
 */
var categoryObject = Em.Object.extend({
  value: '',
  count: 0,
  labelPath: '',
  label: function () {
    return Em.I18n.t(this.get('labelPath')).format(this.get('count'));
  }.property('count', 'labelPath')
});

/**
 * @class HostProgressPopupBodyView
 * @type {Em.View}
 */
App.HostProgressPopupBodyView = App.TableView.extend({

  templateName: require('templates/common/host_progress_popup'),

  /**
   * @type {boolean}
   */
  showTextArea: false,

  /**
   * @type {boolean}
   */
  isServiceEmptyList: true,

  /**
   * @type {boolean}
   */
  isTasksEmptyList: true,

  /**
   * @type {number}
   */
  sourceRequestScheduleId: -1,

  /**
   * @type {boolean}
   */
  sourceRequestScheduleRunning: false,

  /**
   * @type {boolean}
   */
  sourceRequestScheduleAborted: false,

  /**
   * @type {?string}
   */
  sourceRequestScheduleCommand: null,

  /**
   * @type {string}
   */
  clipBoardContent: null,

  /**
   * @type {boolean}
   */
  isClipBoardActive: false,

  /**
   * Determines that host information become loaded and mapped with <code>App.hostsMapper</code>.
   * This property play in only when Log Search service installed.
   *
   * @type {boolean}
   */
  hostInfoLoaded: true,

  /**
   * Clipboard for task logs
   * Used when user click "Copy" and textarea with task stderr and stdout is shown
   * Should be destroyed (call `destroy`) when used is moved out from task logs level or when BG-ops modal is closed
   *
   * @type {?Clipboard}
   */
  taskLogsClipboard: null,

  /**
   * Alias for <code>controller.hosts</code>
   *
   * @type {wrappedHost[]}
   */
  hosts: function () {
    return this.get('controller.hosts')
  }.property('controller.hosts.[]'),

  /**
   * Alias for <code>controller.servicesInfo</code>
   *
   * @type {wrappedService[]}
   */
  services: function () {
    return this.get('controller.servicesInfo');
  }.property('controller.servicesInfo.[]'),

  /**
   * @type {number}
   */
  openedTaskId: 0,

  /**
   * Return task detail info of opened task
   *
   * @type {wrappedTask}
   */
  openedTask: function () {
    if (!(this.get('openedTaskId') && this.get('tasks'))) {
      return Em.Object.create();
    }
    return this.get('tasks').findProperty('id', this.get('openedTaskId'));
  }.property('tasks', 'tasks.@each.stderr', 'tasks.@each.stdout', 'openedTaskId'),

  /**
   * stderr and stdout joined together for clipboard
   *
   * @type {string}
   */
  formattedLogsForOpenedTask: function () {
    var stderr = this.get('openedTask.stderr');
    var stdout = this.get('openedTask.stdout');
    return 'stderr: \n' + stderr + '\n stdout:\n' + stdout;
  }.property('openedTask.stderr', 'openedTask.stdout'),

  /**
   * @type {object}
   */
  filterMap: {
    pending: ["pending", "queued"],
    in_progress: ["in_progress", "upgrading"],
    failed: ["failed"],
    completed: ["completed", "success"],
    aborted: ["aborted"],
    timedout: ["timedout"]
  },

  /**
   * Determines if "Show More ..."-link should be shown
   * @type {boolean}
   */
  isShowMore: true,

  /**
   * @type {boolean}
   */
  pagination: true,

  /**
   * @type {boolean}
   */
  isPaginate: false,

  /**
   * Select box, display names and values
   *
   * @type {progressPopupCategoryObject[]}
   */
  categories: [
    categoryObject.create({value: 'all', labelPath: 'hostPopup.status.category.all'}),
    categoryObject.create({value: 'pending', labelPath: 'hostPopup.status.category.pending'}),
    categoryObject.create({value: 'in_progress', labelPath: 'hostPopup.status.category.inProgress'}),
    categoryObject.create({value: 'failed', labelPath: 'hostPopup.status.category.failed'}),
    categoryObject.create({value: 'completed', labelPath: 'hostPopup.status.category.success'}),
    categoryObject.create({value: 'aborted', labelPath: 'hostPopup.status.category.aborted'}),
    categoryObject.create({value: 'timedout', labelPath: 'hostPopup.status.category.timedout'})
  ],

  /**
   * Selected option is bound to this values
   * @type {?progressPopupCategoryObject}
   */
  serviceCategory: null,

  /**
   * @type {?progressPopupCategoryObject}
   */
  hostCategory: null,

  /**
   * @type {?progressPopupCategoryObject}
   */
  taskCategory: null,

  /**
   * Indicates current level displayed.
   *
   * @type {string}
   */
  currentLevel: "",

  /**
   * flag to indicate whether level data has already been loaded
   * applied only to HOSTS_LIST and TASK_DETAILS levels, whereas async query used to obtain data
   *
   * @type {boolean}
   */
  isLevelLoaded: true,

  /**
   * <code>switchLevel</code> for some <code>controller.dataSourceController</code> should be customized
   * So, this map contains information about customize-methods
   * Format: key - <code>dataSourceController.name</code>, value - method name in this view
   * Method is called with same arguments as <code>switchLevel</code> is
   *
   * @type {object}
   */
  customControllersSwitchLevelMap: {
    highAvailabilityProgressPopupController: '_switchLevelForHAProgressPopupController'
  },

  /**
   * @type {boolean}
   */
  isHostEmptyList: Em.computed.empty('pageContent'),

  /**
   * @type {wrappedHost}
   */
  currentHost: Em.computed.findByKey('hosts', 'name', 'controller.currentHostName'),

  /**
   * Tasks for current shown host (<code>currentHost</code>)
   *
   * @type {wrappedTask[]}
   */
  tasks: function () {
    var currentHost = this.get('currentHost');
    return currentHost ? currentHost.get('tasks') : [];
  }.property('currentHost.tasks', 'currentHost.tasks.@each.status'),


  /**
   * Message about aborting operation
   * Custom for Rolling Restart
   *
   * @type {string}
   */
  requestScheduleAbortLabel: function () {
    return 'ROLLING-RESTART' === this.get('sourceRequestScheduleCommand') ?
      Em.I18n.t("hostPopup.bgop.abort.rollingRestart"):
      Em.I18n.t("common.abort");
  }.property('sourceRequestScheduleCommand'),

  didInsertElement: function () {
    this.updateHostInfo();
    this.subscribeResize();
  },

  willDestroyElement: function () {
    if (this.get('controller.dataSourceController.name') === 'highAvailabilityProgressPopupController') {
      this.set('controller.dataSourceController.isTaskPolling', false);
    }
    this.unsubscribeResize();
  },

  selectServiceCategory: function (e) {
    this.set('serviceCategory', e.context);
  },

  selectHostCategory: function (e) {
    this.set('hostCategory', e.context);
  },

  selectTaskCategory: function (e) {
    this.set('taskCategory', e.context);
  },

  /**
   * Subscribe for window <code>resize</code> event.
   *
   * @method subscribeResize
   */
  subscribeResize: function() {
    var self = this;
    $(window).on('resize', this.resizeHandler.bind(this));
    Em.run.next(this, function() {
      self.resizeHandler();
    });
  },

  /**
   * Remove event listener for window <code>resize</code> event.
   */
  unsubscribeResize: function() {
    $(window).off('resize', this.resizeHandler.bind(this));
  },

  /**
   * This method handles window resize and fit modal body content according to visible items for each <code>level</code>.
   *
   * @method resizeHandler
   */
  resizeHandler: function() {
    const parentView = this.get('parentView');

    if (!parentView || !parentView.$ || !parentView.$() || this.get('state') === 'destroyed' || !parentView.get('isOpen')) return;

    var modal = parentView.$().find('.modal'),
        headerHeight = $(modal).find('.modal-header').outerHeight(true),
        modalFooterHeight = $(modal).find('.modal-footer').outerHeight(true),
        taskTopWrapHeight = $(modal).find('.top-wrap:visible').outerHeight(true),
        modalTopOffset = $(modal).offset().top,
        contentPaddingBottom = parseFloat($(modal).find('.modal-dialog').css('marginBottom')) || 0,
        hostsPageBarHeight = $(modal).find('#host-info tfoot').outerHeight(true),
        logComponentFileNameHeight = $(modal).find('#host-info tfoot').outerHeight(true),
        levelName = this.get('currentLevel'),
        boLevelHeightMap = {
          'REQUESTS_LIST': {
            height: window.innerHeight - 2*modalTopOffset - headerHeight - taskTopWrapHeight - modalFooterHeight - contentPaddingBottom,
            target: '#service-info'
          },
          'HOSTS_LIST': {
            height: window.innerHeight - 2*modalTopOffset - headerHeight - taskTopWrapHeight - modalFooterHeight - contentPaddingBottom - hostsPageBarHeight,
            target: '#host-info'
          },
          'TASKS_LIST': {
            height: window.innerHeight - 2*modalTopOffset - headerHeight - taskTopWrapHeight - modalFooterHeight - contentPaddingBottom,
            target: '#host-log'
          },
          'TASK_DETAILS': {
            height: window.innerHeight - 2*modalTopOffset - headerHeight - taskTopWrapHeight - modalFooterHeight - contentPaddingBottom,
            target: ['.task-detail-log-info', '.log-tail-content.pre-styled']
          }
        },
        currentLevelHeight,
        resizeTarget;

    if (levelName && levelName in boLevelHeightMap) {
      resizeTarget = boLevelHeightMap[levelName].target;
      currentLevelHeight = boLevelHeightMap[levelName].height;
      if (!Em.isArray(resizeTarget)) {
        resizeTarget = [resizeTarget];
      }
      resizeTarget.forEach(function(target) {
        if (target === '.log-tail-content.pre-styled') {
          currentLevelHeight -= logComponentFileNameHeight;
        }
        $(target).css('maxHeight', currentLevelHeight + 'px');
      });
    }
  },

  /**
   * Preset values on init
   *
   * @method setOnStart
   */
  setOnStart: function () {
    this.set('serviceCategory', this.get('categories').findProperty('value', 'all'));

    if (this.get("controller.isBackgroundOperations")) {
      this.get('controller').setSelectCount(this.get("services"), this.get('categories'));
      this.updateHostInfo();
    } else {
      this.get('parentView').switchView("HOSTS_LIST");
      this.set('hostCategory', this.get('categories').findProperty('value', 'all'));
    }
  },

  /**
   * force popup to show list of operations
   *
   * @method resetState
   */
  resetState: function () {
    if (this.get('parentView.isOpen')) {
      this.get('parentView').switchView("OPS_LIST");
      this.get("controller").setBackgroundOperationHeader(false);
      this.get('controller.hosts').clear();
      this.setOnStart();
      this.rerender();
    }
  }.observes('parentView.isOpen'),

  /**
   * When popup is opened, and data after polling has changed, update this data in component
   *
   * @method updateHostInfo
   */
  updateHostInfo: function () {
    if (!this.get('parentView.isOpen')) {
      return;
    }
    this.set('parentView.isLoaded', false);
    this.get("controller").set("inputData", this.get("controller.dataSourceController.services"));
    this.get("controller").onServiceUpdate(this.get('parentView.isServiceListHidden'));
    this.get("controller").onHostUpdate();
    this.set('parentView.isLoaded', true);
    this.set("hosts", this.get("controller.hosts"));
    this.set("services", this.get("controller.servicesInfo"));
    this.set('isLevelLoaded', true);
  }.observes("controller.dataSourceController.serviceTimestamp"),

  /**
   * Depending on service filter, set which services should be shown
   *
   * @method visibleServices
   */
  visibleServices: function () {
    if (this.get("services")) {
      this.set("isServiceEmptyList", true);
      if (this.get('serviceCategory.value')) {
        var filter = this.get('serviceCategory.value');
        var services = this.get('services');
        this.set("isServiceEmptyList", this.setVisibility(filter, services));
      }
    }
  }.observes('serviceCategory', 'services', 'services.@each.status'),

  /**
   * Depending on hosts filter, set which hosts should be shown
   *
   * @method filter
   */
  filter: function () {
    var filter = this.get('hostCategory.value');
    var hosts = this.get('hosts') || [];
    var filterMap = this.get('filterMap');
    if (!filter || !hosts.length) {
      return;
    }
    if (filter === 'all') {
      this.set('filteredContent', hosts);
    }
    else {
      this.set('filteredContent', hosts.filter(function (item) {
        return filterMap[filter].contains(item.status);
      }));
    }
  }.observes('hosts.length', 'hostCategory.value'),

  /**
   * Reset startIndex property back to 1 when filter type has been changed.
   *
   * @method resetIndex
   */
  resetIndex: function () {
    if (this.get('hostCategory.value')) {
      this.set('startIndex', 1);
    }
  }.observes('hostCategory.value'),

  /**
   * Depending on tasks filter, set which tasks should be shown
   *
   * @method visibleTasks
   */
  visibleTasks: function () {
    this.set("isTasksEmptyList", true);
    if (this.get('taskCategory.value') && this.get('tasks')) {
      var filter = this.get('taskCategory.value');
      var tasks = this.get('tasks');
      this.set("isTasksEmptyList", this.setVisibility(filter, tasks));
    }
  }.observes('taskCategory', 'tasks', 'tasks.@each.status'),

  /**
   * Depending on selected filter type, set object visibility value
   *
   * @param filter
   * @param obj
   * @return {bool} isEmptyList
   * @method setVisibility
   */
  setVisibility: function (filter, obj) {
    var isEmptyList = true;
    var filterMap = this.get('filterMap');
    if (filter === "all") {
      obj.setEach("isVisible", true);
      isEmptyList = !obj.length;
    }
    else {
      obj.forEach(function (item) {
        item.set('isVisible', filterMap[filter].contains(item.status));
        isEmptyList = isEmptyList ? !item.get('isVisible') : false;
      }, this)
    }
    return isEmptyList;
  },

  /**
   * Depending on currently viewed tab, call setSelectCount function
   *
   * @method updateSelectView
   */
  updateSelectView: function () {
    var isPaginate;
    if (this.get('parentView.isHostListHidden')) {
      if (this.get('parentView.isTaskListHidden')) {
        if (!this.get('parentView.isServiceListHidden')) {
          this.get('controller').setSelectCount(this.get("services"), this.get('categories'));
        }
      }
      else {
        this.get('controller').setSelectCount(this.get("tasks"), this.get('categories'));
      }
    }
    else {
      //since lazy loading used for hosts, we need to get hosts info directly from controller, that always contains entire array of data
      this.get('controller').setSelectCount(this.get("controller.hosts"), this.get('categories'));
      isPaginate = true;

    }
    this.set('isPaginate', !!isPaginate);
  }.observes('tasks.@each.status', 'hosts.@each.status', 'parentView.isTaskListHidden', 'parentView.isHostListHidden', 'services.@each.status'),

  setBreadcrumbs: function (level) {
    const breadcrumbs = [];
    const self = this;
    const opsCrumb = this.get("controller.rootBreadcrumb");

    if (opsCrumb) {
      opsCrumb.action = function () { self.switchLevel("OPS_LIST"); }

      const opCrumb = {
        label: this.get("controller.serviceName"),
        action: function () { self.switchLevel("HOSTS_LIST", self.get('controller.servicesInfo').findProperty('id', self.get('controller.currentServiceId'))); }
      }

      const hostCrumb = {
        label: this.get("controller.currentHostName"),
        action: function () { self.switchLevel("TASKS_LIST", self.get('currentHost')); }
      }

      const taskCrumb = {
        itemView: Em.View.extend({
          tagName: "span",
          controller: self,
          template: Em.Handlebars.compile('<i style="margin-left: 20px;" {{bindAttr class="openedTask.status :task-detail-status-ico openedTask.icon"}}></i>{{openedTask.commandDetail}}')
        })
      }

      switch (level) {
        case "OPS_LIST":
          breadcrumbs.push(opsCrumb);
          break;
        case "HOSTS_LIST":
          breadcrumbs.push(opsCrumb);
          if (opCrumb.label === breadcrumbs[0].label) {
            breadcrumbs.pop();
          }
          breadcrumbs.push(opCrumb);
          break;
        case "TASKS_LIST":
          breadcrumbs.push(opsCrumb);
          if (opCrumb.label === breadcrumbs[0].label) {
            breadcrumbs.pop();
          }
          breadcrumbs.push(opCrumb);
          breadcrumbs.push(hostCrumb);
          break;
        case "TASK_DETAILS":
          breadcrumbs.push(opsCrumb);
          if (opCrumb.label === breadcrumbs[0].label) {
            breadcrumbs.pop();
          }
          breadcrumbs.push(opCrumb);
          breadcrumbs.push(hostCrumb);
          breadcrumbs.push(taskCrumb);
          break;
      }

      this.set('controller.breadcrumbs', breadcrumbs);
    }
  },

  /**
   * Sets up and tears down the different views in the modal when switching.
   * Calls switchView() on the controller to perform the actual view switch.
   *
   * @param {string} levelName
   * @method switchLevel
   */
  switchLevel: function (levelName, context) {
    const prevLevel = this.get('controller.dataSourceController.levelInfo.name');

    //leaving level - do any cleanup here
    switch (prevLevel) {
      case "OPS_LIST":
        break;
      case "HOSTS_LIST":
        break;
      case "TASKS_LIST":
        break;
      case "TASK_DETAILS":
        this.destroyClipBoard();
        break;
    }

    //entering level - do any setup here
    switch (levelName) {
      case "OPS_LIST":
        this.gotoOps();
        break;
      case "HOSTS_LIST":
        this.gotoHosts(context);
        break;
      case "TASKS_LIST":
        this.gotoTasks(context);
        break;
      case "TASK_DETAILS":
        this.goToTaskDetails(context);
        break;
    }

    if (!this.get("controller.isBackgroundOperations")) {
      var customControllersSwitchLevelMap = this.get('customControllersSwitchLevelMap');
      var args = [].slice.call(arguments);
      Em.tryInvoke(this, customControllersSwitchLevelMap[this.get('controller.dataSourceController.name')], args);
    }
  },

  changeLevel: function(levelName) {
    if (this.get("controller.isBackgroundOperations")) {
      var dataSourceController = this.get('controller.dataSourceController');
      var levelInfo = dataSourceController.get('levelInfo');

      levelInfo.set('taskId', this.get('openedTaskId'));
      levelInfo.set('requestId', this.get('controller.currentServiceId'));
      levelInfo.set('name', levelName);
      this.set('isLevelLoaded', dataSourceController.requestMostRecent());
    }

    this.set('currentLevel', levelName); //NOTE: setting this triggers levelDidChange() which updates the breadcrumbs
  },

  //This is triggered by changeLevel() -- (and probably some other things) --
  //when "controller.dataSourceController.levelInfo.name" is set
  levelDidChange: function() {
    var levelName = this.get('currentLevel'),
        self = this;

    if (this.get("parentView.isOpen")) {
      self.setBreadcrumbs(levelName);
    }

    if (levelName && this.get('isLevelLoaded')) {
      Em.run.next(this, function() {
        self.resizeHandler();
      });
    }
  }.observes('currentLevel', 'isLevelLoaded'),

  popupIsOpenDidChange: function() {
    const hostComponentLogs = this.get('hostComponentLogs');

    if (!this.get('parentView.isOpen') && hostComponentLogs) {
      hostComponentLogs.clear();
    }
  }.observes('parentView.isOpen'),

  /**
   * Switch-level custom method for <code>highAvailabilityProgressPopupController</code>
   *
   * @param {string} levelName
   * @private
   */
  _switchLevelForHAProgressPopupController: function (levelName) {
    var dataSourceController = this.get('controller.dataSourceController');
    if (levelName === 'TASK_DETAILS') {
      this.set('isLevelLoaded', false);
      dataSourceController.startTaskPolling(this.get('openedTask.request_id'), this.get('openedTask.id'));
      Em.keys(this.get('parentView.detailedProperties')).forEach(function (key) {
        dataSourceController.addObserver('taskInfo.' + this.get('parentView.detailedProperties')[key], this, 'updateTaskInfo');
      }, this);
    } else {
      dataSourceController.stopTaskPolling();
    }
  },

  /**
   * @method updateTaskInfo
   */
  updateTaskInfo: function () {
    var dataSourceController = this.get('controller.dataSourceController');
    var openedTask = this.get('openedTask');
    if (openedTask && openedTask.get('id') == dataSourceController.get('taskInfo.id')) {
      this.set('isLevelLoaded', true);
      Em.keys(this.get('parentView.detailedProperties')).forEach(function (key) {
        openedTask.set(key, dataSourceController.get('taskInfo.' + key));
      }, this);
    }
  },

  /**
   * Onclick handler for button <-Tasks
   *
   * @method backToTaskList
   */
  backToTaskList: function () {
    this.switchLevel("TASKS_LIST", true);
  },

  /**
   * Onclick handler for button <-Hosts
   *
   * @method backToHostList
   */
  backToHostList: function () {
    this.switchLevel("HOSTS_LIST", true);
  },

  /**
   * Onclick handler for button <-Operations
   * TODO: This is still used somewhere outside the Background Operations heirarchy.
   *
   * @method backToServiceList
   */
  backToServiceList: function () {
    this.switchLevel("OPS_LIST", true);
  },

  /**
   * Onclick handler for Show more ..
   *
   * @method requestMoreOperations
   */
  requestMoreOperations: function () {
    var BGOController = App.router.get('backgroundOperationsController');
    var count = BGOController.get('operationsCount');
    BGOController.set('operationsCount', count + 10);
    BGOController.requestMostRecent();
  },

  /**
   * @method setShowMoreAvailable
   */
  setShowMoreAvailable: function () {
    if (this.get('parentView.isOpen')) {
      this.set('isShowMore', App.router.get("backgroundOperationsController.isShowMoreAvailable"));
    }
  }.observes('parentView.isOpen', 'App.router.backgroundOperationsController.isShowMoreAvailable'),

  gotoOps: function () {
    this.get('controller.hosts').clear();
    var dataSourceController = this.get('controller.dataSourceController');
    dataSourceController.requestMostRecent();
    this.get("controller").setBackgroundOperationHeader(false);

    this.changeLevel("OPS_LIST");
    this.get("parentView").switchView("OPS_LIST");
  },

  /**
   * Onclick handler for selected Service (Operation)
   *
   * @param {{context: wrappedService}} event
   * @method gotoHosts
   */
  onOpClick: function(event) {
    this.switchLevel("HOSTS_LIST", event.context);
  },

  gotoHosts: function (service) {
    this.get("controller").set("serviceName", service.get("name"));
    this.get("controller").set("currentServiceId", service.get("id"));
    this.get("controller").set("currentHostName", null);
    this.get("controller").onHostUpdate();
    this.get('hostComponentLogs').clear();

    this.changeLevel("HOSTS_LIST");

    var servicesInfo = this.get("controller.hosts");
    this.set("controller.operationInfo", service);

    //apply lazy loading on cluster with more than 100 nodes
    this.set('hosts', servicesInfo.length > 100 ? servicesInfo.slice(0, 50) : servicesInfo);
    $(".modal").scrollTop(0);
    $(".modal-body").scrollTop(0);
    if (servicesInfo.length > 100) {
      Em.run.next(this, function () {
        this.set('hosts', this.get('hosts').concat(servicesInfo.slice(50, servicesInfo.length)));
      });
    }
    // Determine if source request schedule is present
    this.set('sourceRequestScheduleId', service.get("sourceRequestScheduleId"));
    this.set('sourceRequestScheduleCommand', service.get('contextCommand'));
    this.refreshRequestScheduleInfo();

    this.set('hostCategory', this.get('categories').findProperty('value', 'all'));

    this.get("parentView").switchView("HOSTS_LIST");
  },

  /**
   * Navigate to host details logs tab with preset filter.
   */
  navigateToHostLogs: function() {
    var relationType = this._determineRoleRelation(this.get('openedTask')),
        hostModel = App.Host.find().findProperty('hostName', this.get('openedTask.hostName')),
        queryParams = [],
        model;

    if (relationType.type === 'component') {
      model = App.StackServiceComponent.find().findProperty('componentName', relationType.value);
      queryParams.push('service_name=' + model.get('serviceName'));
      queryParams.push('component_name=' + relationType.value);
    }
    if (relationType.type === 'service') {
      queryParams.push('service_name=' + relationType.value);
    }
    App.router.transitionTo('main.hosts.hostDetails.logs', hostModel, { query: ''});
    if (this.get('parentView') && typeof this.get('parentView').onClose === 'function') this.get('parentView').onClose();
  },

  /**
  * Determines if opened task related to service or component.
  *
  * @return {boolean} <code>true</code> when relates to service or component
  */
  isLogsLinkVisible: function() {
    if (!this.get('openedTask') || !this.get('openedTask.id')) return false;
    return !!this._determineRoleRelation(this.get('openedTask'));
  }.property('openedTask'),

  /**
   * @param  {wrappedTask} taskInfo
   * @return {boolean|TaskRelationObject}
   */
  _determineRoleRelation: function(taskInfo) {
    var foundComponentName,
        foundServiceName,
        componentNames = App.StackServiceComponent.find().mapProperty('componentName'),
        serviceNames = App.StackService.find().mapProperty('serviceName'),
        taskLog = this.get('currentHost.logTasks').findProperty('Tasks.id', Em.get(taskInfo, 'id')) || {},
        role = Em.getWithDefault(taskLog, 'Tasks.role', false),
        eqlFn = function(compare) {
          return function(item) {
            return item === compare;
          };
        };

    if (!role) {
      return false;
    }
    // component service check
    if (role.endsWith('_SERVICE_CHECK')) {
      role = role.replace('_SERVICE_CHECK', '');
    }
    foundComponentName = componentNames.filter(eqlFn(role))[0];
    foundServiceName = serviceNames.filter(eqlFn(role))[0];
    if (foundComponentName || foundServiceName) {
      return {
        type: foundComponentName ? 'component' : 'service',
        value: foundComponentName || foundServiceName
      }
    }
    return false;
  },

  /**
   * @type {boolean}
   */
  isRequestSchedule: function () {
    var id = this.get('sourceRequestScheduleId');
    return id != null && !isNaN(id) && id > -1;
  }.property('sourceRequestScheduleId'),

  /**
   * @method refreshRequestScheduleInfo
   */
  refreshRequestScheduleInfo: function () {
    var self = this;
    var id = this.get('sourceRequestScheduleId');
    batchUtils.getRequestSchedule(id, function (data) {
      var status = Em.get(data || {}, 'RequestSchedule.status');
      if (status) {
        switch (status) {
          case 'DISABLED':
            self.set('sourceRequestScheduleRunning', false);
            self.set('sourceRequestScheduleAborted', true);
            break;
          case 'COMPLETED':
            self.set('sourceRequestScheduleRunning', false);
            self.set('sourceRequestScheduleAborted', false);
            break;
          case 'SCHEDULED':
            self.set('sourceRequestScheduleRunning', true);
            self.set('sourceRequestScheduleAborted', false);
            break;
        }
      }
      else {
        self.set('sourceRequestScheduleRunning', false);
        self.set('sourceRequestScheduleAborted', false);
      }
    }, function () {
      self.set('sourceRequestScheduleRunning', false);
      self.set('sourceRequestScheduleAborted', false);
    });
  }.observes('sourceRequestScheduleId'),

  /**
   * Attempts to abort the current request schedule
   *
   * @param {{context: number}} event
   * @method doAbortRequestSchedule
   */
  doAbortRequestSchedule: function (event) {
    var self = this;
    var id = event.context;
    batchUtils.doAbortRequestSchedule(id, function () {
      self.refreshRequestScheduleInfo();
    });
  },

  /**
   * Onclick handler for selected Host
   *
   * @param {{context: wrappedHost}} event
   * @method gotoTasks
   */
  onHostClick: function (event) {
    this.switchLevel("TASKS_LIST", event.context);
  },

  gotoTasks: function (host) {
    var tasksInfo = [];

    if (host.logTasks) {
      host.logTasks.forEach(function (_task) {
        tasksInfo.pushObject(this.get("controller").createTask(_task));
      }, this);
    }

    if (tasksInfo.length) {
      this.get("controller").set("currentHostName", host.publicName);
    }

    const currentHost = this.get("currentHost");
    if (currentHost) {
      currentHost.set("tasks", tasksInfo);
    }
    this.preloadHostModel(Em.getWithDefault(host || {}, 'name', false));
    this.set('taskCategory', this.get('categories').findProperty('value', 'all'));
    $(".modal").scrollTop(0);
    $(".modal-body").scrollTop(0);

    this.changeLevel("TASKS_LIST");
    this.get("parentView").switchView("TASKS_LIST");
  },

  /**
   * Will add <code>App.Host</code> model with relevant host info by specified host name only when it missed
   * and Log Search service installed.
   * This update needed to get actual status of logs for components located on this host and get appropriate model
   * for navigation to hosts/:host_id/logs route.
   *
   * @method preloadHostModel
   * @param {string} hostName host name to get info
   */
  preloadHostModel: function(hostName) {
    var self = this,
        fields;
    if (!hostName) {
      self.set('hostInfoLoaded', true);
      return;
    }
    if (this.get('isLogSearchInstalled') && App.get('supports.logSearch') && !App.Host.find().someProperty('hostName', hostName)) {
      this.set('hostInfoLoaded', false);
      fields = ['stack_versions/repository_versions/RepositoryVersions/repository_version',
                'host_components/HostRoles/host_name'];
      App.router.get('updateController').updateLogging(hostName, fields, function(data) {
        App.hostsMapper.map({ items: [data] });
      }).always(function() {
        self.set('hostInfoLoaded', true);
      });
    } else {
      self.set('hostInfoLoaded', true);
    }
  },

  /**
   * @method stopRebalanceHDFS
   * @returns {App.ModalPopup}
   */
  stopRebalanceHDFS: function () {
    var hostPopup = this;
    return App.showConfirmationPopup(function () {
      App.ajax.send({
        name: 'cancel.background.operation',
        sender: hostPopup,
        data: {
          requestId: hostPopup.get('controller.currentServiceId')
        }
      });
      hostPopup.backToServiceList();
    });
  },

  /**
   * Onclick handler for selected Task
   *
   * @method openTaskLogInDialog
   */
  openTaskLogInDialog: function () {
    var target = ".task-detail-log-info",
        activeHostLog = this.get('hostComponentLogs').findProperty('isActive', true),
        activeHostLogSelector = activeHostLog ? activeHostLog.get('tabClassNameSelector') + '.active' : false;

    if (this.get('isClipBoardActive')) {
      this.destroyClipBoard();
    }
    if (activeHostLog && $(activeHostLogSelector).length) {
      target = activeHostLogSelector;
    }
    var newWindow = window.open();
    var newDocument = newWindow.document;
    newDocument.write('<pre>' + this.get('textAreaValue') + '</pre>');
    newDocument.close();
  },

  onTaskClick: function (event) {
    this.switchLevel("TASK_DETAILS", event.context);
  },

  /**
   * Onclick event for show task detail info
   *
   * @param {{context: wrappedTask}} event
   * @method goToTaskDetails
   */
  goToTaskDetails: function (taskInfo) {
    const self = this;
    var taskLogsClipboard = new Clipboard('.btn.copy-clipboard', {
      text: function() {
        return self.get('textAreaValue');
      }
    });

    this.set('taskLogsClipboard', taskLogsClipboard);
    if (this.get('isClipBoardActive')) {
      this.destroyClipBoard();
    }

    this.set('openedTaskId', taskInfo.id);

    if (this.get("controller.isBackgroundOperations")) {
      var dataSourceController = this.get('controller.dataSourceController');
      dataSourceController.requestMostRecent();
      this.set('isLevelLoaded', false);
    }

    $(".modal").scrollTop(0);
    $(".modal-body").scrollTop(0);

    this.changeLevel("TASK_DETAILS");
    this.get("parentView").switchView("TASK_DETAILS");
  },

  /**
   * Onclick event for copy to clipboard button
   *
   * @method textTrigger
   */
  textTrigger: function () {
    return this.get('isClipBoardActive') ? this.destroyClipBoard() : this.createClipBoard();
  },

  /**
   * @type {string}
   */
  textAreaValue: function () {
    return this.get('isLogComponentActive') ? this.get('clipBoardContent') : this.get('formattedLogsForOpenedTask');
  }.property('clipBoardContent', 'formattedLogsForOpenedTask', 'isLogComponentActive'),

  /**
   * Create Clip Board
   *
   * @method createClipBoard
   */
  createClipBoard: function () {
    this.set('isClipBoardActive', true);
  },

  /**
   * Destroy Clip Board
   *
   * @method destroyClipBoard
   */
  destroyClipBoard: function () {
    var logElement = this.get('isLogComponentActive') ? $('.log-component-tab.active .log-tail-content'): $(".task-detail-log-maintext");
    logElement.css('display', 'block');
    this.set('isClipBoardActive', false);
    const taskLogsClipboard = this.get('taskLogsClipboard');
    if (taskLogsClipboard) {
      Em.tryInvoke(taskLogsClipboard, 'destroy');
    }
  },

  /**
   * @type {boolean}
   */
  isLogSearchInstalled: function() {
    return App.Service.find().someProperty('serviceName', 'LOGSEARCH');
  }.property(),

  /**
   * Host component logs associated with selected component on 'TASK_DETAILS' level.
   *
   * @property {object[]}
   */
  hostComponentLogs: function() {
    var relationType,
        componentName,
        hostName,
        linkTailTpl = '/#/logs/serviceLogs;hosts={0};components={2};query=%5B%7B"id":0,"name":"path","label":"Path","value":"{1}","isExclude":false%7D%5D';

    if (this.get('openedTask.id')) {
      relationType = this._determineRoleRelation(this.get('openedTask'));
      if (relationType.type === 'component') {
        hostName = this.get('currentHost.name');
        componentName = relationType.value;
        return App.HostComponentLog.find()
          .filterProperty('hostComponent.host.hostName', hostName)
          .filterProperty('hostComponent.componentName', componentName)
          .reduce(function(acc, item, index) {
            var logComponentName = item.get('name'),
                componentHostName = item.get('hostComponent.host.hostName');
            acc.pushObjects(item.get('logFileNames').map(function(logFileName, idx) {
              var tabClassName = logComponentName + '_' + index + '_' + idx;
              return Em.Object.create({
                hostName: componentHostName,
                logComponentName: logComponentName,
                fileName: logFileName,
                tabClassName: tabClassName,
                tabClassNameSelector: '.' + tabClassName,
                displayedFileName: fileUtils.fileNameFromPath(logFileName),
                linkTail: linkTailTpl.format(
                  encodeURIComponent(hostName),
                  encodeURIComponent(logFileName),
                  encodeURIComponent(logComponentName)
                ),
                isActive: false
              });
            }));
            return acc;
          }, []);
      }
    }
    return [];
  }.property('openedTaskId', 'isLevelLoaded'),

  /**
   * @type {boolean}
   */
  isLogComponentActive: Em.computed.someBy('hostComponentLogs', 'isActive', true),

  /**
   * Determines if there are component logs for selected component within 'TASK_DETAILS' level.
   *
   * @property {boolean}
   */
  hostComponentLogsExists: Em.computed.and('isLogSearchInstalled', 'hostComponentLogs.length', 'parentView.isOpen'),

  /**
   * Minimum required content to embed in App.LogTailView. This property observes current active host component log.
   *
   * @property {object}
   */
  logTailViewContent: function() {
    if (!this.get('hostComponentLog')) {
      return null;
    }
    return Em.Object.create({
      hostName: this.get('currentHost.name'),
      logComponentName: this.get('hostComponentLog.name')
    });
  }.property('hostComponentLogs.@each.isActive'),

  logTailView: App.LogTailView.extend({

    isActiveDidChange: function() {
      var self = this;
      if (this.get('content.isActive') === false) return;
      setTimeout(function() {
        self.scrollToBottom();
        self.storeToClipBoard();
      }, 500);
    }.observes('content.isActive'),

    logRowsLengthDidChange: function() {
      if (!this.get('content.isActive') || this.get('state') === 'destroyed') return;
      this.storeToClipBoard();
    }.observes('logRows.length'),

    /**
     * Stores log content to use for clip board.
     */
    storeToClipBoard: function() {
      this.get('parentView').set('clipBoardContent', this.get('logRows').map(function(i) {
        return i.get('logtimeFormatted') + ' ' + i.get('level') + ' ' + i.get('logMessage');
      }).join('\n'));
    }
  }),

  setActiveLogTab: function(e) {
    var content = e.context;
    this.set('clipBoardContent', null);
    this.get('hostComponentLogs').without(content).setEach('isActive', false);
    if (this.get('isClipBoardActive')) {
      this.destroyClipBoard();
    }
    content.set('isActive', true);
  },

  setActiveTaskLogTab: function() {
    this.set('clipBoardContent', null);
    this.get('hostComponentLogs').setEach('isActive', false);
    if (this.get('isClipBoardActive')) {
      this.destroyClipBoard();
    }
  }
});
