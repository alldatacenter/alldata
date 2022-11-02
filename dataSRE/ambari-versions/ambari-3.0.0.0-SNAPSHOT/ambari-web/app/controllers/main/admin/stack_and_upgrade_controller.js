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

App.MainAdminStackAndUpgradeController = Em.Controller.extend(App.LocalStorage, {
  name: 'mainAdminStackAndUpgradeController',

  /**
   * @type {boolean}
   */
  isLoaded: false,

  /**
   * @type {object}
   * @default null
   */
  upgradeData: null,

  /**
   * @type {number}
   * @default null
   */
  upgradeId: null,

  /**
   * Start version of upgrade
   * @type {string}
   * @default null
   */
  fromVersion: null,

  /**
   * @type {string}
   * @default null
   */
  upgradeVersion: null,

  /**
   * @type {string}
   * @default null
   */
  upgradeType: null,

  /**
   * @type {Em.Object}
   */
  upgradeTypeConfig: Em.computed.findByKey('upgradeMethods', 'type', 'upgradeType'),

  /**
   * @type {boolean}
   */
  cantBeStarted: Em.computed.alias('upgradeTypeConfig.cantBeStarted'),

  /**
   * @type {boolean}
   */
  showPauseButton: Em.computed.and('!App.upgradeSuspended', '!App.upgradeCompleted', '!App.upgradeInit'),

  /**
   * @type {boolean}
   * @default true
   */
  downgradeAllowed: true,

  /**
   * @type {string}
   * @default null
   */
  upgradeTypeDisplayName: null,

  /**
   * @type {object}
   * @default null
   */
  failuresTolerance: null,

  /**
   * @type {boolean}
   * @default false
   */
  isDowngrade: false,

  /**
   * flag which indicate that upgrade suspended
   * @type {boolean}
   * @default false
   */
  isSuspended: false,

  /**
   * version that currently applied to server
   * should be plain object, because stored to localStorage
   * @type {object|null}
   */
  currentVersion: null,

  /**
   * versions to which cluster could be upgraded
   * @type {Array}
   */
  targetVersions: [],

  /**
   * @type {object}
   * @default null
   */
  slaveComponentStructuredInfo: null,

  /**
   * @type {Array}
   */
  serviceCheckFailuresServicenames: [],

  /**
   * @type {boolean}
   * @default false
   */
  isUpgradeTypesLoaded: false,

  /**
   * @type {string}
   */
  getSupportedUpgradeError: '',

  /**
   * Restricted type of upgrade, can't be viewed in wizard.
   * It's status visible only in upgrade status label
   * @type {boolean}
   * @default false
   */
  isWizardRestricted: false,

  /**
   * @type {string}
   */
  wizardModalTitle: function () {
    var repoVersion = App.RepositoryVersion.find().findProperty('repositoryVersion', this.get('toVersion'));
    return this.getUpgradeDowngradeHeader(
      this.get('upgradeTypeDisplayName'),
      this.get('upgradeVersion'),
      this.get('isDowngrade'),
      repoVersion
    );
  }.property('upgradeTypeDisplayName', 'upgradeVersion', 'isDowngrade'),


  /**
   * @param {string} upgradeType
   * @param {string} upgradeVersion
   * @param {boolean} isDowngrade
   * @param {boolean} isPatch
   * @returns {string}
   */
  getUpgradeDowngradeHeader: function(upgradeType, upgradeVersion, isDowngrade, repoVersion) {
    if (isDowngrade) {
      return Em.I18n.t('admin.stackUpgrade.dialog.downgrade.header').format(upgradeVersion);
    }
    if (repoVersion && repoVersion.get('isPatch')) {
      return Em.I18n.t('admin.stackUpgrade.dialog.upgrade.patch.header').format(upgradeType, upgradeVersion);
    }
    if (repoVersion && repoVersion.get('isMaint')) {
      return Em.I18n.t('admin.stackUpgrade.dialog.upgrade.maint.header').format(upgradeType, upgradeVersion);
    }
    return Em.I18n.t('admin.stackUpgrade.dialog.upgrade.header').format(upgradeType, upgradeVersion);
  },

  /**
   * methods through which cluster could be upgraded, "allowed" indicated if the method is allowed
   * by stack upgrade path
   * @type {Array}
   */
  upgradeMethods: [
    Em.Object.create({
      displayName: Em.I18n.t('admin.stackVersions.version.upgrade.upgradeOptions.RU.title'),
      type: 'ROLLING',
      icon: "glyphicon glyphicon-dashboard",
      description: Em.I18n.t('admin.stackVersions.version.upgrade.upgradeOptions.RU.description'),
      selected: false,
      allowed: true,
      isCheckComplete: false,
      isCheckRequestInProgress: false,
      precheckResultsMessage: '',
      precheckResultsTitle: '',
      action: '',
      isWizardRestricted: false
    }),
    Em.Object.create({
      displayName: Em.I18n.t('admin.stackVersions.version.upgrade.upgradeOptions.EU.title'),
      type: 'NON_ROLLING',
      icon: "icon-bolt",
      description: Em.I18n.t('admin.stackVersions.version.upgrade.upgradeOptions.EU.description'),
      selected: false,
      allowed: true,
      isCheckComplete: false,
      isCheckRequestInProgress: false,
      precheckResultsMessage: '',
      precheckResultsTitle: '',
      action: '',
      isWizardRestricted: false
    }),
    Em.Object.create({
      displayName: Em.I18n.t('admin.stackVersions.version.upgrade.upgradeOptions.HOU.title'),
      type: 'HOST_ORDERED',
      icon: "icon-bolt",
      description: '',
      selected: false,
      allowed: false,
      isCheckComplete: false,
      isCheckRequestInProgress: false,
      precheckResultsMessage: '',
      precheckResultsTitle: '',
      action: '',
      isWizardRestricted: !App.supports.enabledWizardForHostOrderedUpgrade,
      cantBeStarted: true
    })
  ],

  /**
   * pre-check messages map
   * @type {object}
   */
  preCheckMessages: {
    'WARNING': {
      template: 'admin.stackUpgrade.preCheck.warning.message',
      precheckResultsMessageClass: 'ORANGE',
      isPrecheckFailed: false,
      precheckResultsMessageIconClass: 'glyphicon glyphicon-warning-sign'
    },
    'BYPASS': {
      template: 'admin.stackUpgrade.preCheck.bypass.message',
      precheckResultsMessageClass: 'RED',
      isPrecheckFailed: false,
      precheckResultsMessageIconClass: 'glyphicon glyphicon-remove',
      bypassedFailures: true
    },
    'FAIL': {
      template: 'admin.stackUpgrade.preCheck.fail.message',
      precheckResultsMessageClass: 'RED',
      isPrecheckFailed: true,
      precheckResultsMessageIconClass: 'glyphicon glyphicon-remove'
    }
  },

  runningCheckRequests: [],

  /**
   * @type {boolean} true if some request that should disable actions is in progress
   */
  requestInProgress: false,

  /**
   * @type {number} repo id, request for which is currently in progress
   */
  requestInProgressRepoId: null,

  /**
   * @type {boolean} true while no updated upgrade info is loaded after retry
   */
  isRetryPending: false,

  /**
   * properties that stored to localStorage to resume wizard progress
   */
  wizardStorageProperties: [
    'fromVersion',
    'upgradeId',
    'upgradeVersion',
    'toVersion',
    'currentVersion',
    'upgradeTypeDisplayName',
    'upgradeType',
    'failuresTolerance',
    'isDowngrade',
    'downgradeAllowed',
    'isSuspended',
    'isWizardRestricted'
  ],

  /**
   * mutable properties of Upgrade Task
   * @type {Array}
   */
  taskDetailsProperties: ['status', 'stdout', 'stderr', 'error_log', 'host_name', 'output_log'],

  /**
   * Context for Finalize item
   * @type {string}
   */
  finalizeContext: 'Confirm Finalize',

  /**
   * Context for Slave component failures manual item
   * @type {string}
   */
  slaveFailuresContext: "Check Component Versions",

  /**
   * Context for Service check (may include slave component) failures manual item
   * @type {string}
   */
  serviceCheckFailuresContext: "Verifying Skipped Failures",

  /**
   * Check if current item is Finalize
   * @type {boolean}
   */
  isFinalizeItem: false,

  isLoadUpgradeDataPending: false,

  /**
   * path to the mock json
   * @type {String}
   */
  mockRepoUrl: '/data/stack_versions/repo_versions_all.json',

  /**
   * api to get RepoVersions
   * @type {String}
   */
  realRepoUrl: function () {
    return App.get('apiPrefix') + '/stacks?fields=versions/repository_versions/RepositoryVersions,' +
      'versions/repository_versions/operating_systems/*,versions/repository_versions/operating_systems/repositories/*';
  }.property('App.stackVersionURL'),

  /**
   * path to the mock json
   * @type {String}
   */
  mockStackUrl: '/data/stack_versions/stack_version_all.json',

  /**
   * api to get ClusterStackVersions with repository_versions (use to init data load)
   * @type {String}
   */
  realStackUrl: function () {
    return App.get('apiPrefix') + '/clusters/' + App.get('clusterName') +
      '/stack_versions?fields=*,repository_versions/*,repository_versions/operating_systems/OperatingSystems/*,repository_versions/operating_systems/repositories/*';
  }.property('App.clusterName'),

  /**
   * api to get ClusterStackVersions without repository_versions (use to update data)
   * @type {String}
   */
  realUpdateUrl: function () {
    return App.get('apiPrefix') + '/clusters/' + App.get('clusterName') + '/stack_versions?fields=ClusterStackVersions/*';
  }.property('App.clusterName'),

  /**
   * Determines if list of services with checks that failed and were skipped by user during the upgrade is loaded
   * @type {boolean}
   */
  areSkippedServiceChecksLoaded: false,

  /**
   * List of services with checks that failed and were skipped by user during the upgrade
   * @type {array}
   */
  skippedServiceChecks: [],

  /**
   * status of tasks/items/groups which should be grayed out and disabled
   * @type {Array}
   */
  nonActiveStates: ['PENDING', 'ABORTED'],

  /**
   * status of Upgrade request
   * @type {string}
   */
  requestStatus: function () {
    if (this.get('upgradeData.Upgrade') && App.get('upgradeSuspended')) {
      return 'SUSPENDED';
    } else if (this.get('upgradeData.Upgrade')){
      return this.get('upgradeData.Upgrade.request_status');
    } else {
      return 'INIT';
    }
  }.property('upgradeData.Upgrade.request_status', 'App.upgradeSuspended'),

  init: function () {
    this.initDBProperties();
  },

  /**
   * restore data from localStorage
   */
  initDBProperties: function () {
    var props = this.getDBProperties(this.get('wizardStorageProperties'));
    Em.keys(props).forEach(function (k) {
      if (!Em.isNone(props[k])) {
        this.set(k, props[k]);
      }
    }, this);
  },

  /**
   * load all data:
   * - upgrade data
   * - stack versions
   * - repo versions
   */
  load: function () {
    var dfd = $.Deferred();
    var self = this;

    this.loadStackVersionsToModel(true).done(function () {
      self.loadRepoVersionsToModel().done(function() {
        self.loadCompatibleVersions().done(function() {
          self.updateCurrentStackVersion();
          dfd.resolve();
        });
      });
    });
    return dfd.promise();
  },

  updateCurrentStackVersion: function(){
    var currentVersion = App.StackVersion.find().findProperty('state', 'CURRENT');
    if (currentVersion) {
      this.set('currentVersion', {
        stack_name: currentVersion.get('repositoryVersion.stackVersionType'),
        repository_version: currentVersion.get('repositoryVersion.repositoryVersion'),
        repository_name: currentVersion.get('repositoryVersion.displayName'),
        id: currentVersion.get('repositoryVersion.id')
      });
    }
  },

  /**
   * load upgrade tasks by upgrade id
   * @return {$.Deferred}
   * @param {boolean} onlyState
   */
  loadUpgradeData: function (onlyState) {
    var upgradeId = this.get('upgradeId'),
      deferred = $.Deferred(),
      self = this;

    if (Em.isNone(upgradeId)) {
      deferred.resolve();
    } else {
      this.set('isLoadUpgradeDataPending', true);
      App.ajax.send({
        name: (onlyState) ? 'admin.upgrade.state' : 'admin.upgrade.data',
        sender: this,
        data: {
          id: upgradeId
        },
        success: 'loadUpgradeDataSuccessCallback'
      }).then(deferred.resolve).always(function () {
          self.set('isLoadUpgradeDataPending', false);
        });
    }
    return deferred.promise();
  },

  /**
   * parse and push upgrade tasks to controller
   * @param data
   */
  loadUpgradeDataSuccessCallback: function (data) {
    if (Em.isNone(data)) return;
    App.set('upgradeState', data.Upgrade.request_status);
    this.setDBProperty('upgradeState', data.Upgrade.request_status);
    this.set('isSuspended', data.Upgrade.suspended);
    this.setDBProperty('isSuspended', data.Upgrade.suspended);
    if (data.upgrade_groups) {
      this.updateUpgradeData(data);
    }
    if (this.get('isRetryPending') && data.Upgrade.request_status !== 'ABORTED') {
      this.setProperties({
        requestInProgress: false,
        isRetryPending: false
      });
    }
    if (data.Upgrade.request_status === 'COMPLETED') {
      this.finish();
    }
  },

  loadCompatibleVersions: function() {
    return App.ajax.send({
      name: 'admin.upgrade.get_compatible_versions',
      sender: this,
      data: {
        stackName: App.get('currentStackName'),
        stackVersion: App.get('currentStackVersionNumber')
      },
      success: 'loadCompatibleVersionsSuccessCallback'
    });
  },

  /**
   *
   * @param {object} data
   */
  loadCompatibleVersionsSuccessCallback: function(data) {
    App.RepositoryVersion.find().forEach(function(repo) {
      var version = repo.get('repositoryVersion');
      repo.set('isCompatible', data.items.someProperty('CompatibleRepositoryVersions.repository_version', version));
    });
  },

  /**
   * update data of Upgrade
   * @param {object} newData
   */
  updateUpgradeData: function (newData) {
    var oldData = this.get('upgradeData'),
      nonActiveStates = this.get('nonActiveStates'),
      groupsMap = {},
      itemsMap = {};

    if (Em.isNone(oldData) || (newData.upgrade_groups.length !== oldData.upgradeGroups.length)) {
      this.initUpgradeData(newData);
    } else {
      //create entities maps
      newData.upgrade_groups.forEach(function (newGroup) {
        groupsMap[newGroup.UpgradeGroup.group_id] = newGroup.UpgradeGroup;
        newGroup.upgrade_items.forEach(function (item) {
          itemsMap[item.UpgradeItem.stage_id] = item.UpgradeItem;
        })
      });

      //update existed entities with new data
      oldData.upgradeGroups.forEach(function (oldGroup) {
        oldGroup.set('status', groupsMap[oldGroup.get('group_id')].status);
        oldGroup.set('display_status', groupsMap[oldGroup.get('group_id')].display_status);
        oldGroup.set('progress_percent', groupsMap[oldGroup.get('group_id')].progress_percent);
        oldGroup.set('completed_task_count', groupsMap[oldGroup.get('group_id')].completed_task_count);
        oldGroup.upgradeItems.forEach(function (item) {
          item.set('status', itemsMap[item.get('stage_id')].status);
          item.set('display_status', itemsMap[item.get('stage_id')].display_status);
          item.set('progress_percent', itemsMap[item.get('stage_id')].progress_percent);
        });
        var hasExpandableItems = oldGroup.upgradeItems.some(function (item) {
          return !nonActiveStates.contains(item.get('status'));
        });
        oldGroup.set('hasExpandableItems', hasExpandableItems);
      });
      oldData.set('Upgrade', newData.Upgrade);
    }
  },

  /**
   * change structure of Upgrade
   * In order to maintain nested views in template object should have direct link to its properties, for example
   * item.UpgradeItem.<properties> -> item.<properties>
   * @param {object} newData
   */
  initUpgradeData: function (newData) {
    var upgradeGroups = [],
      nonActiveStates = this.get('nonActiveStates');

    //wrap all entities into App.upgradeEntity
    newData.upgrade_groups.forEach(function (newGroup) {
      var hasExpandableItems = newGroup.upgrade_items.some(function (item) {
          return !nonActiveStates.contains(item.UpgradeItem.status);
        }),
        oldGroup = App.upgradeEntity.create({type: 'GROUP', hasExpandableItems: hasExpandableItems}, newGroup.UpgradeGroup),
        upgradeItems = [];
      newGroup.upgrade_items.forEach(function (item) {
        var oldItem = App.upgradeEntity.create({type: 'ITEM'}, item.UpgradeItem);
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
      Upgrade: newData.Upgrade
    }));
    this.set('downgradeAllowed', newData.Upgrade.downgrade_allowed);
    this.setDBProperty('downgradeAllowed', newData.Upgrade.downgrade_allowed);
  },

  /**
   * format upgrade item text
   * @param {App.upgradeEntity} oldItem
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
  getUpgradeItem: function (item, customCallback) {
    return App.ajax.send({
      name: 'admin.upgrade.upgrade_item',
      sender: this,
      data: {
        upgradeId: item.get('request_id'),
        groupId: item.get('group_id'),
        stageId: item.get('stage_id')
      },
      success: customCallback || 'getUpgradeItemSuccessCallback'
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
                tasks.pushObject(App.upgradeEntity.create({
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
   * Failures info may includes service_check and host_component failures. These two types should be displayed separately.
   */
  getServiceCheckItemSuccessCallback: function(data) {
    var task = data.tasks[0];
    var info = {
      hosts: [],
      host_detail: {}
    };

    if (task && task.Tasks && task.Tasks.structured_out && task.Tasks.structured_out.failures) {
      this.set('serviceCheckFailuresServicenames', task.Tasks.structured_out.failures.service_check || []);
      if (task.Tasks.structured_out.failures.host_component) {
        for (var hostname in task.Tasks.structured_out.failures.host_component){
          info.hosts.push(hostname);
        }
        info.host_detail = task.Tasks.structured_out.failures.host_component;
      }
      this.set('slaveComponentStructuredInfo', info);
    }
  },

  getSlaveComponentItemSuccessCallback: function(data) {
    var info = data.tasks[0];
    if (info && info.Tasks && info.Tasks.structured_out) {
      this.set('slaveComponentStructuredInfo', info.Tasks.structured_out);
    }
  },

  /**
   * downgrade confirmation popup
   * @param {object} event
   */
  confirmDowngrade: function (event) {
    var self = this;
    if(!this.get('currentVersion')){
      this.updateCurrentStackVersion();
    }
    var currentVersion = this.get('currentVersion');
    return App.showConfirmationPopup(
      function() {
        self.downgrade.call(self, currentVersion, event);
      },
      Em.I18n.t('admin.stackUpgrade.downgrade.body').format(currentVersion.repository_name),
      null,
      Em.I18n.t('admin.stackUpgrade.dialog.downgrade.header').format(this.get('upgradeVersion')),
      Em.I18n.t('admin.stackUpgrade.downgrade.proceed')
    );
  },

  /**
   * make call to start downgrade process
   * @param {object} currentVersion
   * @param {object} event
   */
  downgrade: function (currentVersion, event) {
    var self = this;
    this.set('requestInProgress', true);
    this.abortUpgrade().done(function() {
      var interval = setInterval(function() {
        if (self.get('upgradeData.Upgrade.request_status') == 'ABORTED') {
          clearInterval(interval);
          self.startDowngrade(currentVersion);
        }
      }, 1000);
    });
  },

  /**
   * abort upgrade (in order to start Downgrade)
   */
  abortUpgrade: function () {
    var errorCallback = this.get('isDowngrade') ? 'abortDowngradeErrorCallback' : 'abortUpgradeErrorCallback';
    return App.ajax.send({
      name: 'admin.upgrade.abort',
      sender: this,
      data: {
        upgradeId: this.get('upgradeId'),
        isDowngrade: this.get('isDowngrade')
      },
      error: errorCallback
    });
  },

  /**
   * just request ro start downgrade,
   * should be performed only if <code>abortUpgrade<code> was completed
   */
  startDowngrade: function(currentVersion) {
   App.ajax.send({
      name: 'admin.downgrade.start',
      sender: this,
      data: {
        value: currentVersion.repository_version,
        label: this.get('upgradeVersion'),
        id: currentVersion.id,
        isDowngrade: true,
        upgradeType: this.get('upgradeType')
      },
      success: 'upgradeSuccessCallback',
      callback: function() {
        this.sender.set('requestInProgress', false);
      }
    });
  },

  /**
   * suspend upgrade (in order to restart it later)
   */
  abortUpgradeWithSuspend: function () {
    var errorCallback = this.get('isDowngrade') ? 'abortDowngradeErrorCallback' : 'abortUpgradeErrorCallback';
    return App.ajax.send({
      name: 'admin.upgrade.suspend',
      sender: this,
      data: {
        upgradeId: this.get('upgradeId'),
        isDowngrade: this.get('isDowngrade')
      },
      error: errorCallback
    });
  },

  /**
   * error callback of <code>abortUpgrade()</code>
   * @param {object} data
   */
  abortUpgradeErrorCallback: function (data) {
    var header = Em.I18n.t('admin.stackUpgrade.state.paused.fail.header');
    var body = Em.I18n.t('admin.stackUpgrade.state.paused.fail.body');
    if (data && data.responseText) {
      try {
        var json = $.parseJSON(data.responseText);
        body = body + ' ' + json.message;
      } catch (err) {}
    }
    App.showAlertPopup(header, body);
  },

  /**
   * error callback of <code>abortDowngrade()</code>
   * @param {object} data
   */
  abortDowngradeErrorCallback: function (data) {
    var header = Em.I18n.t('admin.stackDowngrade.state.paused.fail.header');
    var body = Em.I18n.t('admin.stackDowngrade.state.paused.fail.body');
    if(data && data.responseText){
      try {
        var json = $.parseJSON(data.responseText);
        body = body + ' ' + json.message;
      } catch (err) {}
    }
    App.showAlertPopup(header, body);
  },

  retryUpgrade: function () {
    this.setProperties({
      requestInProgress: true,
      isRetryPending: true
    });
    return App.ajax.send({
      name: 'admin.upgrade.retry',
      sender: this,
      data: {
        upgradeId: this.get('upgradeId')
      }
    });
  },

  /**
   * make call to start upgrade process and show popup with current progress
   * @param {object} version
   */
  upgrade: function (version) {
    this.set('requestInProgress', true);
    App.ajax.send({
      name: 'admin.upgrade.start',
      sender: this,
      data: version,
      success: 'upgradeSuccessCallback',
      error: 'upgradeErrorCallback',
      callback: function() {
        this.sender.set('requestInProgress', false);
      }
    });
    this.setDBProperty('currentVersion', this.get('currentVersion'));

    // Show a "preparing the upgrade..." dialog in case the api call returns too slow
    if (App.router.get('currentState.name') != 'stackUpgrade') {
      this.showPreparingUpgradeIndicator();
    }
  },

  /**
   * Should progress bar be displayed when preparing upgrade,
   * should show after Upgrade Options window and before Upgrade Wizard
   * @method showPreparingUpgradeIndicator
   */
  showPreparingUpgradeIndicator: function () {
    return App.ModalPopup.show({
      header: Em.I18n.t('admin.stackUpgrade.dialog.prepareUpgrade.header'),
      showFooter: false,
      showCloseButton: false,
      bodyClass: Em.View.extend({
        templateName: require('templates/wizard/step8/step8_log_popup'),
        controllerBinding: 'App.router.mainAdminStackAndUpgradeController',

        /**
         * Css-property for progress-bar
         * @type {string}
         */
        barWidth: 'width: 100%;',
        progressBarClass: 'progress log_popup',

        /**
         * Hide popup when upgrade wizard is open
         * @method autoHide
         */
        autoHide: function () {
          if (!this.get('controller.requestInProgress')) {
            this.get('parentView').hide();
          }
        }.observes('controller.requestInProgress')
      })
    });
  },

  /**
   * error callback of <code>upgrade()</code>
   * @param {object} data
   */
  upgradeErrorCallback: function (data) {
    var header = Em.I18n.t('admin.stackVersions.upgrade.start.fail.title');
    var body = "";
    if (data && data.responseText) {
      try {
        var json = $.parseJSON(data.responseText);
        body = json.message;
      } catch (err) {}
    }
    App.showAlertPopup(header, body);
  },

  /**
   * success callback of <code>upgrade()</code>
   * @param {object} data
   */
  upgradeSuccessCallback: function (data, opt, params) {
    this.set('upgradeData', null);
    this.set('upgradeId', data.resources[0].Upgrade.request_id);
    this.set('toVersion', params.value);
    this.set('upgradeVersion', params.label);
    this.set('isDowngrade', !!params.isDowngrade);
    var upgradeMethod = this.get('upgradeMethods').findProperty('type', params.type),
        upgradeTypeDisplayName = null,
        upgradeType = null,
        isWizardRestricted = false;

    if (upgradeMethod) {
      upgradeTypeDisplayName = upgradeMethod.get('displayName');
      upgradeType = upgradeMethod.get('type');
      isWizardRestricted = upgradeMethod.get('isWizardRestricted');
    }

    this.set('isWizardRestricted', isWizardRestricted);
    this.set('upgradeType', upgradeType);
    this.set('upgradeTypeDisplayName', upgradeTypeDisplayName);
    this.set('failuresTolerance', Em.Object.create({
      skipComponentFailures: params.skipComponentFailures == 'true',
      skipSCFailures: params.skipSCFailures == 'true'
    }));
    this.setDBProperties({
      upgradeVersion: params.label,
      upgradeId: data.resources[0].Upgrade.request_id,
      toVersion: params.value,
      upgradeState: 'PENDING',
      isDowngrade: !!params.isDowngrade,
      upgradeType: upgradeType,
      isWizardRestricted: isWizardRestricted,
      upgradeTypeDisplayName: upgradeTypeDisplayName,
      failuresTolerance: Em.Object.create({
        skipComponentFailures: params.skipComponentFailures == 'true',
        skipSCFailures: params.skipSCFailures == 'true'
      })
    });
    App.set('upgradeState', 'PENDING');
    App.clusterStatus.setClusterStatus({
      wizardControllerName: this.get('name'),
      localdb: App.db.data
    });
    this.load();
    App.router.get('wizardWatcherController').setUser(App.router.get('mainAdminStackAndUpgradeController').get('name'));
    this.openUpgradeDialog();
  },

  /**
   * success callback of updating upgrade options including failures tolerance. etc
   * @param {object} data
   */
  updateOptionsSuccessCallback: function (data, opt, params) {
    this.set('failuresTolerance', Em.Object.create({
      skipComponentFailures: params.skipComponentFailures == 'true',
      skipSCFailures: params.skipSCFailures == 'true'
    }));
  },

  /**
   * run upgrade checks and add results to each method object and set selected method
   * @param {Em.Object} version
   */
  runUpgradeMethodChecks: function(version) {
    this.get('upgradeMethods').forEach(function (method) {
      if (method.get('allowed')) {
        this.runPreUpgradeCheckOnly({
          id: version.get('id'),
          label: version.get('displayName'),
          type: method.get('type')
        });
      } else {
        //if method not supported in current stack version, mark as check completed
        method.setProperties({
          isCheckComplete: false,
          isCheckRequestInProgress: false,
          action: ''
        });
      }
    }, this);
  },

  getConfigsWarnings: function (configsMergeWarning) {
    var configs = [];
    if (configsMergeWarning && Em.get(configsMergeWarning, 'UpgradeChecks.status') === 'WARNING') {
      var configsMergeCheckData = Em.get(configsMergeWarning, 'UpgradeChecks.failed_detail');
      if (configsMergeCheckData && Em.isArray(configsMergeCheckData)) {
        configs = configsMergeCheckData.reduce(function (allConfigs, item) {
          const isDeprecated = Em.isNone(item.new_stack_value),
                willBeRemoved = Em.isNone(item.result_value),
                configInfo = App.configsCollection.getConfigByName(item.property, item.type) || {};

          return allConfigs.concat({
            type: item.type,
            name: item.property,
            serviceName: configInfo.serviceName,
            wasModified: (!isDeprecated && !willBeRemoved && Em.compare(item.current, item.result_value) === 0),
            currentValue: item.current,
            recommendedValue: isDeprecated ? Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.deprecated') : item.new_stack_value,
            isDeprecated: isDeprecated,
            resultingValue: willBeRemoved ? Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.willBeRemoved') : item.result_value,
            willBeRemoved: willBeRemoved
          });
        }, []);
      }
    }
    return configs;
  },

  showUpgradeOptions: function ( version ) {
    this.upgradeOptions(false, version, true);
  },

  /**
   * Open upgrade options window: upgrade type and failures tolerance
   * @param {boolean} isInUpgradeWizard
   * @param {object} version
   * @return App.ModalPopup
   */
  upgradeOptions: function (isInUpgradeWizard, version, preUpgradeShow) {
    var self = this,
      upgradeMethods = this.get('upgradeMethods'),
      runningCheckRequests = this.get('runningCheckRequests');
    if (!isInUpgradeWizard) {
      upgradeMethods.setEach('isCheckRequestInProgress', true);
      upgradeMethods.setEach('selected', false);
      var request = this.getSupportedUpgradeTypes(Ember.Object.create({
        stackName: App.get('currentStackVersion').split('-')[0],
        stackVersion: App.get('currentStackVersion').split('-')[1],
        toVersion: version.get('repositoryVersion')
      })).done(function () {
          if (App.get('router.currentState.name') === 'versions' && App.get('router.currentState.parentState.name') === 'stackAndUpgrade') {
            self.runUpgradeMethodChecks(version);
          }
      }).always(function () {
        self.set('isUpgradeTypesLoaded', true);
        self.set('runningCheckRequests', runningCheckRequests.rejectProperty('type', 'ALL'));
        });
      request.type = 'ALL';
      this.get('runningCheckRequests').push(request);
    } else {
      this.set('isUpgradeTypesLoaded', true);
    }

    const modal = App.ModalPopup.show({
      encodeBody: false,
      primary: function() {
        if ( preUpgradeShow ) return false;
        if ( isInUpgradeWizard || this.get('controller.getSupportedUpgradeError') ) return Em.I18n.t('ok');
        return Em.I18n.t('common.proceed');
      }.property('controller.getSupportedUpgradeError'),
      secondary: function() {
        if (preUpgradeShow) return Em.I18n.t('common.dismiss');
        if (this.get('controller.getSupportedUpgradeError')) return null;
        return Em.I18n.t('common.cancel');
      }.property('controller.getSupportedUpgradeError'),
      secondaryClass: preUpgradeShow ? 'btn-success' : '',
      classNames: ['upgrade-options-popup'],
      header: preUpgradeShow ? Em.I18n.t('admin.stackVersions.version.preUpgrade.header') : Em.I18n.t('admin.stackVersions.version.upgrade.upgradeOptions.header'),
      controller: this,
      showFooter: function() {
        return this.get('controller.isUpgradeTypesLoaded') || preUpgradeShow;
      }.property('controller.isUpgradeTypesLoaded'),
      bodyClass: Em.View.extend({
        templateName: require('templates/main/admin/stack_upgrade/upgrade_options'),
        didInsertElement: function () {
          App.tooltip($(".failure-tolerance-tooltip"), {
            placement: "top",
            title: Em.I18n.t('admin.stackVersions.version.upgrade.upgradeOptions.tolerance.tooltip')
          });
          Em.run.later(this, function () {
            App.tooltip($(".img-thumbnail.check-failed"), {
              placement: "bottom",
              title: Em.I18n.t('admin.stackVersions.version.upgrade.upgradeOptions.preCheck.failed.tooltip')
            });
            App.tooltip($(".not-allowed-by-version"), {
              placement: "bottom",
              title: Em.I18n.t('admin.stackVersions.version.upgrade.upgradeOptions.notAllowed')
            });
          }, 1000);
        },
        upgradeMethods: function () {
          self.updateSelectedMethod(isInUpgradeWizard);
          return self.get('upgradeMethods');
        }.property().volatile(),
        isInUpgradeWizard: isInUpgradeWizard,
        showPreUpgradeChecks: App.get('supports.preUpgradeCheck') && !isInUpgradeWizard,
        versionText: (function () {
          if ( preUpgradeShow ) return Em.I18n.t('admin.stackVersions.version.preUpgrade.bodyMsg.version').format(version.get('displayName'));
          if ( isInUpgradeWizard ) return '';
          return Em.I18n.t('admin.stackVersions.version.upgrade.upgradeOptions.bodyMsg.version').format(version.get('displayName'));
        })(),
        selectMethod: function (event) {
          if (isInUpgradeWizard || !event.context.get('allowed') || event.context.get('isPrecheckFailed')) return;
          var selectedMethod = event.context;
          self.updateSelectedMethod(isInUpgradeWizard);
          self.get('upgradeMethods').forEach(function (method) {
            method.set('selected', false);
          });
          selectedMethod.set('selected', true);
          this.set('parentView.selectedMethod', selectedMethod);
        },
        runAction: function (event) {
          var method = event.context,
            action = method.get('action');
          if (action) {
            this.get(action)(event);
          }
        },
        rerunCheck: function (event) {
          self.runPreUpgradeCheckOnly({
            id: version.get('id'),
            label: version.get('displayName'),
            id: version.get('id'),
            type: event.context.get('type')
          });
        },
        openMessage: function (event) {
          if (isInUpgradeWizard || !event.context.get('allowed')) return;
          var data = event.context.get('precheckResultsData');

          var failTitle = Em.I18n.t('popup.clusterCheck.Upgrade.fail.title');
          var failAlert = new Em.Handlebars.SafeString(Em.I18n.t('popup.clusterCheck.Upgrade.fail.alert'));
          var bypassedFailures = data.items.filterProperty('UpgradeChecks.status', 'BYPASS').length > 0;
          if (data.items.filterProperty('UpgradeChecks.status', 'ERROR').length == 0 && bypassedFailures) {
            failTitle = Em.I18n.t('popup.clusterCheck.Upgrade.bypassed-failures.title');
            failAlert = new Em.Handlebars.SafeString(Em.I18n.t('popup.clusterCheck.Upgrade.bypassed-failures.alert'));
          }

          var header = Em.I18n.t('popup.clusterCheck.Upgrade.header').format(version.get('displayName')),
            warningTitle = Em.I18n.t('popup.clusterCheck.Upgrade.warning.title'),
            warningAlert = new Em.Handlebars.SafeString(Em.I18n.t('popup.clusterCheck.Upgrade.warning.alert')),
            configsMergeWarning = data.items.findProperty('UpgradeChecks.id', "CONFIG_MERGE"),
            popupData = {
              items: data.items.rejectProperty('UpgradeChecks.id', 'CONFIG_MERGE')
            },
            configs = self.getConfigsWarnings(configsMergeWarning);
          App.showClusterCheckPopup(popupData, {
            header: header,
            failTitle: failTitle,
            failAlert: failAlert,
            warningTitle: warningTitle,
            warningAlert: warningAlert,
            primary: Em.I18n.t('admin.stackVersions.version.upgrade.upgradeOptions.preCheck.rerun'),
            secondary: Em.I18n.t('common.cancel'),
            bypassedFailures: bypassedFailures,
            callback: function () {
              self.runPreUpgradeCheckOnly.call(self, {
                id: version.get('id'),
                label: version.get('displayName'),
                id: version.get('id'),
                type: event.context.get('type')
              });
            },
            closeParent: function() {
              modal.onClose();
            }
          }, configs);
        },
        upgradeShow: !preUpgradeShow
      }),

      /**
       * @type {Em.Object}
       * @default null
       */
      selectedMethod: null,
      skipComponentFailures: self.get('failuresTolerance.skipComponentFailures'),
      skipSCFailures: self.get('failuresTolerance.skipSCFailures'),
      disablePrimary: function () {
        if (isInUpgradeWizard || this.get('controller.getSupportedUpgradeError')) return false;
        var selectedMethod = this.get('selectedMethod');
        if (selectedMethod) {
          if (App.get('supports.preUpgradeCheck')) {
            return selectedMethod.get('isPrecheckFailed') || selectedMethod.get('isCheckRequestInProgress');
          } else {
            return false;
          }
        } else {
          return true;
        }
      }.property('selectedMethod', 'selectedMethod.isPrecheckFailed', 'selectedMethod.isCheckRequestInProgress'),
      onPrimary: function () {
        this.hide();
        if (isInUpgradeWizard) {
          return App.ajax.send({
            name: 'admin.upgrade.update.options',
            sender: self,
            data: {
              upgradeId: self.get('upgradeId'),
              skipComponentFailures: Boolean(this.get('skipComponentFailures')).toString(),
              skipSCFailures: Boolean(this.get('skipSCFailures')).toString()
            },
            success: 'updateOptionsSuccessCallback'
          });
        } else {
          var upgradeMethod = self.get('upgradeMethods').findProperty('selected');
          version.upgradeType = upgradeMethod.get('type');
          version.upgradeTypeDisplayName = upgradeMethod.get('displayName');
          version.skipComponentFailures = this.get('skipComponentFailures');
          version.skipSCFailures = this.get('skipSCFailures');

          var toVersion = version.get('displayName');
          var bodyMessage = Em.Object.create({
            confirmButton: Em.I18n.t('yes'),
            confirmMsg: upgradeMethod.get('type') === 'ROLLING' ?
              Em.I18n.t('admin.stackVersions.version.upgrade.upgradeOptions.RU.confirm.msg').format(toVersion) :
              Em.I18n.t('admin.stackVersions.version.upgrade.upgradeOptions.EU.confirm.msg').format(toVersion)
          });
          return App.showConfirmationFeedBackPopup(function (query) {
            return self.runPreUpgradeCheck.call(self, version);
          }, bodyMessage);
        }
      }
    });

    return modal;
  },

  /**
   * open upgrade options from upgrade wizard
   */
  openUpgradeOptions: function () {
    if (this.get('isDowngrade')) return;
    this.upgradeOptions(true, null);
  },

  /**
   * upgrade confirmation popup including upgrade options: upgrade type and failures tolerance
   * @param {object} version
   * @return App.ModalPopup
   */
  confirmUpgrade: function (version) {
    this.upgradeOptions(false, version);
  },

  /**
   * send request for pre upgrade check only
   */
  runPreUpgradeCheckOnly: function (data) {
    if (App.get('supports.preUpgradeCheck')) {
      var method = this.get('upgradeMethods').findProperty('type', data.type);
      method.setProperties({
        isCheckComplete: false,
        isCheckRequestInProgress: true,
        action: ''
      });
      var request = App.ajax.send({
        name: "admin.upgrade.pre_upgrade_check",
        sender: this,
        data: data,
        success: 'runPreUpgradeCheckOnlySuccess',
        error: 'runPreUpgradeCheckOnlyError',
        callback: function () {
          var runningCheckRequests = this.sender.get('runningCheckRequests');
          method.set('isCheckRequestInProgress', false);
          this.sender.set('runningCheckRequests', runningCheckRequests.rejectProperty('type', this.data.type));
        }
      });
      request.type = data.type;
      this.get('runningCheckRequests').push(request);
    }
  },

  /**
   * send request to get available upgrade tye names
   */
  getSupportedUpgradeTypes: function(data) {
    this.set('isUpgradeTypesLoaded', false);
    this.set('getSupportedUpgradeError', '');
    return App.ajax.send({
      name: "admin.upgrade.get_supported_upgradeTypes",
      sender: this,
      data: data,
      success: "getSupportedUpgradeTypesSuccess",
      error: "getSupportedUpgradeTypesError"
    });
  },

  /**
   * success callback of <code>getSupportedUpgradeTypes()</code>
   * @param data {object}
   */
  getSupportedUpgradeTypesSuccess: function (data) {
    var supportedUpgradeTypes = data.items[0] && data.items[0].CompatibleRepositoryVersions.upgrade_types;
    this.get('upgradeMethods').forEach(function (method) {
      method.set('allowed', Boolean(supportedUpgradeTypes && supportedUpgradeTypes.contains(method.get('type'))));
    });
  },

  /**
   * error callback of <code>getSupportedUpgradeTypes()</code>
   * @param xhr {object}
   */
  getSupportedUpgradeTypesError: function (xhr) {
    var response;
    try {
      response = JSON.parse(xhr.responseText);
    } catch (e) {
      response = {message: xhr.statusText};
    }
    this.set('getSupportedUpgradeError', response.message);
  },

  /**
   * success callback of <code>runPreUpgradeCheckOnly()</code>
   * Show a message how many fails/warnings/bypass/passed
   * on clicking that message a popup window show up
   * @param data {object}
   * @param opt {object}
   * @param params {object}
   */
  runPreUpgradeCheckOnlySuccess: function (data, opt, params) {
    var properties = {
      precheckResultsTitle: Em.I18n.t('admin.stackVersions.version.upgrade.upgradeOptions.preCheck.msg.title'),
      precheckResultsData: data,
      isCheckComplete: true,
      action: 'openMessage',
      precheckResultsMessage: '',
      recheckResultsMessageClass: 'GREEN',
      isPrecheckFailed: false,
      precheckResultsMessageIconClass: 'glyphicon glyphicon-ok',
      bypassedFailures: false
    };

    Object.keys(this.get('preCheckMessages')).forEach(function(status) {
      if (data.items.someProperty('UpgradeChecks.status', status)) {
        properties = this.formatPreCheckMessage(status, data, properties);
      }
    }, this);

    if (!properties.precheckResultsMessage) {
      properties.precheckResultsMessage = Em.I18n.t('admin.stackVersions.version.upgrade.upgradeOptions.preCheck.allPassed');
    }
    this.get('upgradeMethods').findProperty('type', params.type).setProperties(properties);
    this.updateSelectedMethod(false);
    this.addPrecheckMessageTooltip();
  },

  /**
   * @method formatPreCheckMessage
   * @param {string} type
   * @param {object} data
   * @param {object} defaults
   * @returns {object}
   */
  formatPreCheckMessage: function(type, data, defaults) {
    var length = data.items.filterProperty('UpgradeChecks.status', type).length;
    var properties = this.get('preCheckMessages')[type] || {};
    var message = Em.I18n.t(properties.template).format(length, defaults.precheckResultsMessage);
    defaults = $.extend(defaults, properties);
    delete defaults.template;
    defaults.precheckResultsMessage = message;
    return defaults;
  },

  addPrecheckMessageTooltip: function() {
    Em.run.later(this, function () {
      // add tooltip for the type with preCheck errors
      App.tooltip($(".img-thumbnail.check-failed"), {
        placement: "bottom",
        title: Em.I18n.t('admin.stackVersions.version.upgrade.upgradeOptions.preCheck.failed.tooltip')
      });
      // destroy the tooltip for the type wo preCheck errors
      $(".img-thumbnail").not(".check-failed").not(".not-allowed-by-version").tooltip("destroy");
    }, 1000);
  },

  runPreUpgradeCheckOnlyError: function (request, ajaxOptions, error, data, params) {
    var method = this.get('upgradeMethods').findProperty('type', params.type);
    method.setProperties({
      precheckResultsMessage: Em.I18n.t('admin.stackVersions.version.upgrade.upgradeOptions.preCheck.msg.failed.link'),
      precheckResultsTitle: Em.I18n.t('admin.stackVersions.version.upgrade.upgradeOptions.preCheck.msg.failed.title'),
      precheckResultsMessageClass: 'RED',
      isPrecheckFailed: true,
      precheckResultsMessageIconClass: 'glyphicon glyphicon-warning-sign',
      action: 'rerunCheck'
    });
  },

  /**
   * In Upgrade Wizard: update which method already been selected on open
   * Not in upgrade wizard: de-select the method with pre-check errors
   * @param isInUpgradeWizard {boolean}
   */
  updateSelectedMethod: function (isInUpgradeWizard) {
    if (isInUpgradeWizard) {
      this.get('upgradeMethods').forEach(function (method) {
        method.set('selected', method.get('type') === this.get('upgradeType'));
      }, this);
    } else {
      var ruMethod = this.get('upgradeMethods').findProperty('type', 'ROLLING');
      var euMethod = this.get('upgradeMethods').findProperty('type', 'NON_ROLLING');
      if (ruMethod && ruMethod.get('isPrecheckFailed')) ruMethod.set('selected', false);
      if (euMethod && euMethod.get('isPrecheckFailed')) euMethod.set('selected', false);
    }
  },

  /**
   * send request for pre upgrade check
   * @param version
   */
  runPreUpgradeCheck: function(version) {
    var params = {
      value: version.get('repositoryVersion'),
      label: version.get('displayName'),
      type: version.get('upgradeType'),
      skipComponentFailures: version.get('skipComponentFailures') ? 'true' : 'false',
      skipSCFailures: version.get('skipSCFailures') ? 'true' : 'false',
      id: version.get('id'),
      targetStack: version.get('displayName')
    };
    if (App.get('supports.preUpgradeCheck')) {
      this.set('requestInProgress', true);
      App.ajax.send({
        name: "admin.upgrade.pre_upgrade_check",
        sender: this,
        data: params,
        success: "runPreUpgradeCheckSuccess",
        error: "runPreUpgradeCheckError"
      });
    } else {
      this.upgrade(params);
    }
  },

  /**
   * success callback of <code>runPreUpgradeCheckSuccess()</code>
   * if there are some fails - it shows popup else run upgrade
   * @param data {object}
   * @param opt {object}
   * @param params {object}
   * @returns {App.ModalPopup|undefined}
   */
  runPreUpgradeCheckSuccess: function (data, opt, params) {
    var self = this;
    if (data.items.someProperty('UpgradeChecks.status', 'FAIL') || data.items.someProperty('UpgradeChecks.status', 'WARNING') || data.items.someProperty('UpgradeChecks.status', 'BYPASS')) {
      this.set('requestInProgress', false);
      var hasFails = data.items.someProperty('UpgradeChecks.status', 'FAIL'),
        header = Em.I18n.t('popup.clusterCheck.Upgrade.header').format(params.label),
        failTitle = Em.I18n.t('popup.clusterCheck.Upgrade.fail.title'),
        failAlert = new Em.Handlebars.SafeString(Em.I18n.t('popup.clusterCheck.Upgrade.fail.alert')),
        warningTitle = Em.I18n.t('popup.clusterCheck.Upgrade.warning.title'),
        warningAlert = new Em.Handlebars.SafeString(Em.I18n.t('popup.clusterCheck.Upgrade.warning.alert')),
        bypassedFailures = data.items.someProperty('UpgradeChecks.status', 'BYPASS').length > 0,
        configsMergeWarning = data.items.findProperty('UpgradeChecks.id', 'CONFIG_MERGE'),
        popupData = {
          items: data.items.rejectProperty('UpgradeChecks.id', 'CONFIG_MERGE')
        },
        configs = this.getConfigsWarnings(configsMergeWarning);
      App.showClusterCheckPopup(popupData, {
        header: header,
        failTitle: failTitle,
        failAlert: failAlert,
        warningTitle: warningTitle,
        warningAlert: warningAlert,
        bypassedFailures: bypassedFailures,
        noCallbackCondition: hasFails,
        callback: function () {
          self.upgrade(params);
        }
      }, configs, params.label);
    } else {
      this.upgrade(params);
    }
  },

  runPreUpgradeCheckError: function() {
    this.set('requestInProgress', false);
  },

  confirmRetryUpgrade: function (version) {
    var self = this;
    return App.showConfirmationPopup(
      function () {
        self.retryUpgrade();
      },
      Em.I18n.t('admin.stackUpgrade.upgrade.retry.confirm.body').format(version.get('displayName')),
      null,
      this.getUpgradeDowngradeHeader(version.get('upgradeTypeDislayName'), version.get('displayName'))
    );
  },

  confirmRetryDowngrade: function () {
    var self = this,
      currentVersion = this.get('currentVersion');
    return App.showConfirmationPopup(
      function() {
        self.retryUpgrade();
      },
      Em.I18n.t('admin.stackUpgrade.downgrade.retry.body').format(currentVersion.repository_name),
      null,
      Em.I18n.t('admin.stackUpgrade.dialog.downgrade.header').format(currentVersion.repository_name),
      Em.I18n.t('admin.stackUpgrade.downgrade.proceed')
    );
  },

  /**
   * confirmation popup before install repository version
   */
  installRepoVersionPopup: function (repo) {
    var availableServices = repo.get('stackServices').filter(function(service) {
      return App.Service.find(service.get('name')).get('isLoaded') && service.get('isAvailable') && service.get('isUpgradable');
    }, this);
    if (!availableServices.length && !repo.get('isStandard')){
      return App.showAlertPopup( Em.I18n.t('admin.stackVersions.upgrade.installPackage.fail.title'), Em.I18n.t('admin.stackVersions.upgrade.installPackage.fail.noAvailableServices').format(repo.get('displayName')) );
    }
    var self = this;
    var bodyContent = repo.get('isPatch')
      ? Em.I18n.t('admin.stackVersions.version.install.patch.confirm')
      : Em.I18n.t('admin.stackVersions.version.install.confirm');
    return App.ModalPopup.show({
      header: Em.I18n.t('popup.confirmation.commonHeader'),
      popupBody: bodyContent.format(repo.get('displayName')),
      skipDependencyCheck: false,
      bodyClass: Em.View.extend({
        classNames: ['install-repo-confirmation'],
        content: availableServices,
        showAvailableServices: repo.get('isPatch') || repo.get('isMaint'),
        templateName: require('templates/common/modal_popups/install_repo_confirmation')
      }),
      onPrimary: function () {
        self.installRepoVersion(repo);
        this._super();
      }
    });
  },

  /**
   * sends request to install repoVersion to the cluster
   * and create clusterStackVersion resourse
   * @param {Em.Object} repo
   * @return {$.ajax}
   * @method installRepoVersion
   */
  installRepoVersion: function (repo) {
    this.set('requestInProgress', true);
    this.set('requestInProgressRepoId', repo.get('id'));

    var data = {
      ClusterStackVersions: {
        stack: repo.get('stackVersionType'),
        version: repo.get('stackVersionNumber'),
        repository_version: repo.get('repositoryVersion')
      },
      id: repo.get('id')
    };
    return App.ajax.send({
      name: 'admin.stack_version.install.repo_version',
      sender: this,
      data: data,
      success: 'installRepoVersionSuccess',
      error: 'installRepoVersionError',
      callback: function() {
        this.sender.set('requestInProgress', false);
        this.sender.set('requestInProgressRepoId', null);
      }
    });
  },
  
  /**
   * TODO unify and move modal view into common/modal_popups
   * @param {string[]} hosts
   * @param {string} title
   * @param {string} operation
   * @param {Function} primary
   */
  showReinstallRemoveConfirmation: function({hosts, title, operation, primary = () => {}}) {
    return App.ModalPopup.show({
      header: title,
      visibleHosts: hosts.join("\n"),
      expanded: true,
      onPrimary: function() {
        primary(hosts);
        this._super();
      },
    
      bodyClass: Em.View.extend({
        templateName: require('templates/main/host/bulk_operation_confirm_popup'),
        message: Em.I18n.t('hosts.bulkOperation.confirmation.hosts').format(operation, hosts.length),
        textareaVisible: false,
        textTrigger: function() {
          this.toggleProperty('textareaVisible');
        },
        putHostNamesToTextarea: function() {
          var hostNames = this.get('parentView.visibleHosts');
          if (this.get('textareaVisible')) {
            var wrapper = $(".task-detail-log-maintext");
            $('.task-detail-log-clipboard').html(hostNames).width(wrapper.width()).height(250);
            Em.run.next(function() {
              $('.task-detail-log-clipboard').select();
            });
          }
        }.observes('textareaVisible')
      })
    });
  },
  
  removeOutOfSyncComponents: function (event) {
    const hosts = App.RepositoryVersion.find(event.context.repoId).get('stackVersion.outOfSyncHosts');
    return this.showReinstallRemoveConfirmation({
      hosts,
      title: Em.I18n.t('admin.stackVersions.version.errors.outOfSync.remove.title'),
      operation: Em.I18n.t('hosts.host.maintainance.removeFailedComponents.context'),
      primary: () => {
        App.get('router.mainAdminKerberosController').getKDCSessionState(() => {
          App.ajax.send({
            name: 'host.host_component.delete_components',
            sender: this,
            data: {
              hosts,
              data: JSON.stringify({
                RequestInfo: {
                  query: 'HostRoles/host_name.in(' + hosts.join(',') + ')&HostRoles/state=INSTALL_FAILED'
                }
              })
            }
          });
        });
      }
    });
  },
  
  reinstallOutOfSyncComponents: function (event) {
    const hosts = App.RepositoryVersion.find(event.context.repoId).get('stackVersion.outOfSyncHosts');
    return this.showReinstallRemoveConfirmation({
      hosts,
      title: Em.I18n.t('admin.stackVersions.version.errors.outOfSync.reinstall.title'),
      operation: Em.I18n.t('hosts.host.maintainance.reinstallFailedComponents.context'),
      primary: () => {
        App.get('router.mainAdminKerberosController').getKDCSessionState(() => {
          App.ajax.send({
            name: 'common.host_components.update',
            sender: this,
            data: {
              HostRoles: {
                state: 'INSTALLED'
              },
              query: 'HostRoles/host_name.in(' + hosts.join(',') + ')&HostRoles/state=INSTALL_FAILED',
              context: Em.I18n.t('hosts.host.maintainance.reinstallFailedComponents.context')
            },
            success: 'reinstallOutOfSyncComponentsSuccessCallback'
          });
        });
      }
    });
  },
  
  reinstallOutOfSyncComponentsSuccessCallback: function (data, opt, params, req) {
    if (!data && req.status == 200) {
      return App.ModalPopup.show({
        header: Em.I18n.t('rolling.nothingToDo.header'),
        body: Em.I18n.t('rolling.nothingToDo.body').format(params.noOpsMessage || Em.I18n.t('hosts.host.maintainance.allComponents.context')),
        secondary: false
      });
    }
    return App.router.get('userSettingsController').dataLoading('show_bg').done(function (initValue) {
      if (initValue) {
        App.router.get('backgroundOperationsController').showPopup();
      }
    });
  },

  /**
   * transform repo data into json for
   * saving changes to repository version
   * @param {Em.Object} repo
   * @returns {{operating_systems: Array}}
   */
  prepareRepoForSaving: function(repo) {
    var repoVersion = { "operating_systems": [] };
    var ambari_managed_repositories = !repo.get('useRedhatSatellite');
    repo.get('operatingSystems').forEach(function (os, k) {
      repoVersion.operating_systems.push({
        "OperatingSystems": {
          "os_type": os.get("osType"),
          "ambari_managed_repositories": ambari_managed_repositories
        },
        "repositories": []
      });
      os.get('repositories').forEach(function (repository) {
        repoVersion.operating_systems[k].repositories.push({
          "Repositories": {
            "base_url": repository.get('baseUrl'),
            "repo_id": repository.get('repoId'),
            "repo_name": repository.get('repoName')
          }
        });
      });
    });
    return repoVersion;
  },

  /**
   * Return stack version for the repo object
   * @param {Em.Object} repo
   * */
  getStackVersionNumber: function(repo){
    var stackVersionNumber = repo.get('stackVersion');
    if(null == stackVersionNumber)
      stackVersionNumber = App.get('currentStackVersion');
    return stackVersionNumber;
  },

  /**
   * perform validation if <code>skip<code> is  false and run save if
   * validation successfull or run save without validation is <code>skip<code> is true
   * @param {Em.Object} repo
   * @param {boolean} skip
   * @returns {$.Deferred}
   */
  saveRepoOS: function (repo, skip) {
    var self = this;
    var deferred = $.Deferred();
    this.validateRepoVersions(repo, skip).done(function(data) {
      if (data.length > 0) {
        deferred.resolve(data);
      } else {
        var repoVersion = self.prepareRepoForSaving(repo);
        var stackVersionNumber = self.getStackVersionNumber(repo);

        App.ajax.send({
          name: 'admin.stack_versions.edit.repo',
          sender: this,
          data: {
            stackName: App.get('currentStackName'),
            stackVersion: stackVersionNumber,
            repoVersionId: repo.get('repoVersionId'),
            repoVersion: repoVersion
          }
        }).success(function() {
          deferred.resolve([]);
        });
      }
    });
    return deferred.promise();
  },

  /**
   * send request for validation for each repository
   * @param {Em.Object} repo
   * @param {boolean} skip
   * @returns {*}
   */
  validateRepoVersions: function(repo, skip) {
    var deferred = $.Deferred(),
      totalCalls = 0,
      invalidUrls = [];

    if (skip) {
      deferred.resolve(invalidUrls);
    } else {
      var stackVersionNumber = this.getStackVersionNumber(repo);
      repo.get('operatingSystems').forEach(function (os) {
        if (os.get('isSelected')) {
          os.get('repositories').forEach(function (repo) {
            totalCalls++;
            this.validationCall(repo, os, stackVersionNumber)
            .success(function () {
              totalCalls--;
              if (totalCalls === 0) deferred.resolve(invalidUrls);
            })
            .error(function () {
              repo.set('hasError', true);
              invalidUrls.push(repo);
              totalCalls--;
              if (totalCalls === 0) deferred.resolve(invalidUrls);
            });
          }, this);
        } else {
          return deferred.resolve(invalidUrls);
        }
      }, this);
    }
    return deferred.promise();
  },

  /**
   *
   * @param {Em.Object} repo
   * @param {Em.Object} os
   * @param {string} stackVersionNumber
   */
  validationCall: function(repo, os, stackVersionNumber) {
    return App.ajax.send({
      name: 'admin.stack_versions.validate.repo',
      sender: this,
      data: {
        repo: repo,
        repoName: repo.get('repoName'),
        repoId: repo.get('repoId'),
        baseUrl: repo.get('baseUrl'),
        osType: os.get('osType'),
        stackName: App.get('currentStackName'),
        stackVersion: stackVersionNumber
      }
    })
  },

  /**
   * success callback for <code>installRepoVersion()<code>
   * saves request id to the db
   * @param data
   * @param opt
   * @param params
   * @method installStackVersionSuccess
   */
  installRepoVersionSuccess: function (data, opt, params) {
    if (data && data.statusText === "timeout") {
      App.showAlertPopup(Em.I18n.t('admin.stackVersions.upgrade.installPackage.fail.title'), Em.I18n.t('admin.stackVersions.upgrade.installPackage.fail.timeout'));
      return false;
    }
    var version = App.RepositoryVersion.find(params.id);
    App.db.set('repoVersionInstall', 'id', [data.Requests.id]);
    App.clusterStatus.setClusterStatus({
      wizardControllerName: this.get('name'),
      localdb: App.db.data
    });
    version.set('defaultStatus', 'INSTALLING');
    if (version.get('stackVersion')) {
      version.set('stackVersion.state', 'INSTALLING');
    }
  },

  /**
   * error callback for <code>installRepoVersion()<code>
   * show the error message
   * @param data
   * @method installStackVersionSuccess
   */
  installRepoVersionError: function (data, opt, params) {
    var header = Em.I18n.t('admin.stackVersions.upgrade.installPackage.fail.title');
    var body = "";
    if (data && data.responseText) {
      try {
        var json = $.parseJSON(data.responseText);
        body = json.message;
      } catch (err) {}
    }
    if (data && data.statusText === "timeout") {
      body = Em.I18n.t('admin.stackVersions.upgrade.installPackage.fail.timeout');
    }
    var version = App.RepositoryVersion.find(params.id);
    version.set('defaultStatus', 'INSTALL_FAILED');
    if (version.get('stackVersion')) {
      version.set('stackVersion.state', 'INSTALL_FAILED');
    }
    App.showAlertPopup(header, body);
  },

  /**
   * opens a popup with installations state per host
   * @param {Em.Object} version
   * @method showProgressPopup
   */
  showProgressPopup: function(version) {
    var popupTitle = Em.I18n.t('admin.stackVersions.details.install.hosts.popup.title').format(version.get('displayName'));
    var requestIds = this.getRepoVersionInstallId();
    var hostProgressPopupController = App.router.get('highAvailabilityProgressPopupController');
    hostProgressPopupController.initPopup(popupTitle, requestIds, this);
  },

  getRepoVersionInstallId: function() {
    if (App.get('testMode')) return [1];

    var requestIds = App.db.get('repoVersionInstall', 'id');
    var lastRepoVersionInstall = App.router.get('backgroundOperationsController.services').find(function(request) {
      return request.get('name').startsWith('Install version');
    });
    if (lastRepoVersionInstall &&
      (!requestIds || !requestIds.contains(lastRepoVersionInstall.get('id')))) {
      requestIds = [lastRepoVersionInstall.get('id')];
    }
    return requestIds || [];
  },

  /**
   * reset upgradeState to NOT_REQUIRED when upgrade is COMPLETED
   * and clean auxiliary data
   */
  finish: function () {
    var upgradeVersion = this.get('upgradeVersion') && this.get('upgradeVersion').match(/[a-zA-Z]+\-\d+\.\d+/);
    this.setDBProperties({
      fromVersion: undefined,
      upgradeId: undefined,
      upgradeState: 'NOT_REQUIRED',
      upgradeVersion: undefined,
      currentVersion: undefined,
      upgradeTypeDisplayName: undefined,
      upgradeType: undefined,
      isWizardRestricted: false,
      failuresTolerance: undefined,
      isDowngrade: undefined,
      downgradeAllowed: undefined
    });
    this.initDBProperties();
    App.clusterStatus.setClusterStatus({
      localdb: App.db.data
    });
    if (upgradeVersion && upgradeVersion[0]) {
      App.set('currentStackVersion', upgradeVersion[0]);
    }
    App.set('upgradeState', 'NOT_REQUIRED');
  },

  /**
   * Check <code>App.upgradeState</code> for HOLDING
   * If it is, send request to check if current item is Finalize
   * @method updateFinalize
   */
  updateFinalize: function () {
    var upgradeState = App.get('upgradeState');
    if (upgradeState === 'HOLDING') {
      return App.ajax.send({
        name: 'admin.upgrade.finalizeContext',
        sender: this,
        success: 'updateFinalizeSuccessCallback',
        error: 'updateFinalizeErrorCallback'
      })
    }
    else {
      this.set('isFinalizeItem', false);
    }
  }.observes('App.upgradeState'),

  /**
   *
   * @param {object|null} data
   * @method updateFinalizeSuccessCallback
   */
  updateFinalizeSuccessCallback: function (data) {
    var context = data ? Em.get(data, 'items.firstObject.upgrade_groups.firstObject.upgrade_items.firstObject.UpgradeItem.context') : '';
    this.set('isFinalizeItem', context === this.get('finalizeContext'));
  },

  updateFinalizeErrorCallback: function() {
    this.set('isFinalizeItem', false);
  },

  /**
   * show dialog with tasks of upgrade
   * @return {App.ModalPopup}
   */
  openUpgradeDialog: function () {
    if (App.isAuthorized('CLUSTER.UPGRADE_DOWNGRADE_STACK') && !this.get('isWizardRestricted')) {
      App.propertyDidChange('upgradeSuspended');
      App.router.transitionTo('admin.stackUpgrade');
    }
  },

  /**
   * returns url to get data for repoVersion or clusterStackVersion
   * @param {Boolean} stack true if load clusterStackVersion
   * @param {Boolean} fullLoad true if load all data
   * @returns {String}
   * @method getUrl
   */
  getUrl: function(stack, fullLoad) {
    if (App.get('testMode')) {
      return stack ? this.get('mockStackUrl') : this.get('mockRepoUrl')
    } else {
      if (fullLoad) {
        return stack ? this.get('realStackUrl') : this.get('realRepoUrl');
      } else {
        return this.get('realUpdateUrl');
      }
    }
  },

  /**
   * get stack versions from server and push it to model
   * @return {*}
   * @method loadStackVersionsToModel
   */
  loadStackVersionsToModel: function (fullLoad) {
    var dfd = $.Deferred();
    App.HttpClient.get(this.getUrl(true, fullLoad), App.stackVersionMapper, {
      complete: function () {
        dfd.resolve();
      }
    });
    return dfd.promise();
  },

  /**
   * get repo versions from server and push it to model
   * @return {*}
   * @params {Boolean} isUpdate - if true loads part of data that need to be updated
   * @method loadRepoVersionsToModel()
   */
  loadRepoVersionsToModel: function () {
    var dfd = $.Deferred();
    App.HttpClient.get(this.getUrl(false, true), App.repoVersionMapper, {
      complete: function () {
        dfd.resolve();
      }
    });
    return dfd.promise();
  },

  /**
   * set status to Upgrade item
   * @param item
   * @param status
   */
  setUpgradeItemStatus: function(item, status) {
    this.set('requestInProgress', true);
    return App.ajax.send({
      name: 'admin.upgrade.upgradeItem.setState',
      sender: this,
      data: {
        upgradeId: item.get('request_id'),
        itemId: item.get('stage_id'),
        groupId: item.get('group_id'),
        status: status
      },
      callback: function() {
        this.sender.set('requestInProgress', false);
      }
    }).done(function () {
      item.set('status', status);
    });
  },

  currentVersionObserver: function () {
    var versionNumber = this.get('currentVersion.repository_version');
    var currentVersionObject = App.RepositoryVersion.find().findProperty('status', 'CURRENT');
    var versionName = currentVersionObject && currentVersionObject.get('stackVersionType');
    App.set('isStormMetricsSupported', versionName != 'HDP' || stringUtils.compareVersions(versionNumber, '2.2.2') > -1 || !versionNumber);
  }.observes('currentVersion.repository_version'),

  /**
   * get the installed repositories of HDP from server
   */
  loadRepositories: function () {
    if (App.router.get('clusterController.isLoaded') && App.get('currentStackVersion')) {
      var nameVersionCombo = App.get('currentStackVersion');
      var stackName = nameVersionCombo.split('-')[0];
      var stackVersion = nameVersionCombo.split('-')[1];
      App.ajax.send({
        name: 'cluster.load_repositories',
        sender: this,
        data: {
          stackName: stackName,
          stackVersion: stackVersion
        },
        success: 'loadRepositoriesSuccessCallback',
        error: 'loadRepositoriesErrorCallback'
      });
    }
  }.observes('App.router.clusterController.isLoaded'),

  loadRepositoriesSuccessCallback: function (data) {
    var allRepos = [];
    data.items.forEach(function (os) {
      os.repositories.forEach(function (repository) {
        var osType = repository.Repositories.os_type;
        var repo = Em.Object.create({
          baseUrl: repository.Repositories.base_url,
          osType: osType,
          repoId: repository.Repositories.repo_id,
          repoName : repository.Repositories.repo_name,
          stackName : repository.Repositories.stack_name,
          stackVersion : repository.Repositories.stack_version,
          isFirst: false
        });
        var group = allRepos.findProperty('name', osType);
        if (!group) {
          group = {
            name: osType,
            repositories: []
          };
          repo.set('isFirst', true);
          allRepos.push(group);
        }
        group.repositories.push(repo);
      });
    }, this);
    allRepos.stackVersion = App.get('currentStackVersionNumber');
    this.set('allRepos', allRepos);
  },

  loadRepositoriesErrorCallback: function (request, ajaxOptions, error) {

  },

  /**
   * @returns {$.ajax}
   */
  suspendUpgrade: function () {
    var self = this;
    return this.abortUpgradeWithSuspend().done(function () {
      App.set('upgradeState', 'ABORTED');
      self.setDBProperty('upgradeState', 'ABORTED');
      App.clusterStatus.setClusterStatus({
        wizardControllerName: self.get('name'),
        localdb: App.db.data
      });
    });
  },

  /**
   * @returns {$.ajax}
   */
  resumeUpgrade: function() {
    var self = this;
    this.retryUpgrade().done(function () {
      App.set('upgradeState', 'PENDING');
      App.propertyDidChange('upgradeSuspended');
      self.setDBProperty('upgradeState', 'PENDING');
      App.clusterStatus.setClusterStatus({
        wizardControllerName: self.get('name'),
        localdb: App.db.data
      });
    });
  },

  /**
   *
   * @param {Em.Object} version
   */
  confirmRevertPatchUpgrade: function(version) {
    var self = this;
    var currentStack = App.RepositoryVersion.find(this.get('currentVersion.id'));
    App.ModalPopup.show({
      header: Em.I18n.t('popup.confirmation.commonHeader'),
      popupBody: Em.I18n.t('admin.stackVersions.upgrade.patch.revert.confirmation'),
      bodyClass: Em.View.extend({
        classNames: ['revert-patch-upgrade-confirmation'],
        servicesToBeReverted: this.getServicesToBeReverted(version, currentStack),
        stackFromVersion: version.get('displayName'),
        stackToVersion: currentStack.get('displayNameSimple'),
        templateName: require('templates/common/modal_popups/revert_patch_upgrade_confirmation')
      }),
      onPrimary: function () {
        self.revertPatchUpgrade(version);
        this._super();
      }
    });
  },

  /**
   *
   * @param {Em.Object} version
   * @param {Em.Object} currentStack
   * @returns {Array}
   */
  getServicesToBeReverted: function(version, currentStack) {
    return version.get('stackServices').filter(function(_service) {
      var originalService = App.Service.find(_service.get('name'));
      var isOriginal = originalService.get('desiredRepositoryVersionId') === version.get('id')
      return (originalService.get('isLoaded') && _service.get('isAvailable')) && isOriginal;
    }).map(function(_service) {
      return {
        displayName: _service.get('displayName'),
        fromVersion: _service.get('latestVersion'),
        toVersion: currentStack.get('stackServices').findProperty('name', _service.get('name')).get('latestVersion')
      }
    });
  },

  /**
   *
   * @param {Em.Object} version
   * @returns {$.ajax}
   */
  revertPatchUpgrade: function (version) {
    this.set('requestInProgress', true);
    return App.ajax.send({
      name: 'admin.upgrade.revert',
      sender: this,
      success: 'upgradeSuccessCallback',
      error: 'upgradeErrorCallback',
      callback: function () {
        this.sender.set('requestInProgress', false);
      },
      data: {
        upgradeId: version.get('stackVersion').get('revertUpgradeId'),
        id: version.get('id'),
        value: version.get('repositoryVersion'),
        label: version.get('displayName'),
        isDowngrade: true
      }
    });
  },

  /**
   * @param {App.RepositoryVersion} version
   * @returns {Em.Object}
   */
  confirmDiscardRepoVersion: function(version) {
    var self = this;
    return App.showConfirmationPopup(function() {
      self.discardRepoVersion(version);
    });
  },

  /**
   * @param {App.RepositoryVersion} version
   * @returns {$.ajax}
   */
  discardRepoVersion: function(version) {
    this.set('requestInProgress', true);
    return App.ajax.send({
      name: 'admin.stack_versions.discard',
      sender: this,
      callback: function () {
        this.sender.set('requestInProgress', false);
      },
      data: {
        id: version.get('id'),
        stackName: version.get('stackVersionType'),
        stackVersion: version.get('stackVersionNumber')
      },
    });
  },

  /**
   * restore last Upgrade data
   * @param {object} lastUpgradeData
   */
  restoreLastUpgrade: function(lastUpgradeData) {
    var self = this;
    var upgradeType = this.get('upgradeMethods').findProperty('type', lastUpgradeData.Upgrade.upgrade_type);
    const isDowngrade = lastUpgradeData.Upgrade.direction === 'DOWNGRADE';
    this.setDBProperties({
      toVersion: lastUpgradeData.Upgrade.associated_version,
      upgradeId: lastUpgradeData.Upgrade.request_id,
      isDowngrade,
      upgradeState: lastUpgradeData.Upgrade.request_status,
      upgradeType: lastUpgradeData.Upgrade.upgrade_type,
      isWizardRestricted: upgradeType.get('isWizardRestricted'),
      downgradeAllowed: lastUpgradeData.Upgrade.downgrade_allowed,
      upgradeTypeDisplayName: upgradeType.get('displayName'),
      isSuspended: lastUpgradeData.Upgrade.suspended,
      failuresTolerance: {
        skipComponentFailures: lastUpgradeData.Upgrade.skip_failures,
        skipSCFailures: lastUpgradeData.Upgrade.skip_service_check_failures
      }
    });
    this.initDBProperties();
    App.set('upgradeState', lastUpgradeData.Upgrade.request_status);
    this.loadRepoVersionsToModel().done(function () {
      var version = App.RepositoryVersion.find().findProperty('repositoryVersion', lastUpgradeData.Upgrade.associated_version);
      var upgradeVersion = version && version.get('displayName');
      self.setDBProperty('upgradeVersion', upgradeVersion);
      self.set('upgradeVersion', upgradeVersion);
    });
  },

  /**
   * Build table from configs list and open new window to show this table
   * @param configs
   */
  openConfigsInNewWindow: function (configs) {
    var newWindow;
    var output = '';

    output += '<table style="text-align: left;"><thead><tr>' +
        '<th>' + Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.configType') + '</th>' +
        '<th>' + Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.serviceName') + '</th>' +
        '<th>' + Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.propertyName') + '</th>' +
        '<th>' + Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.currentValue') + '</th>' +
        '<th>' + Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.recommendedValue') + '</th>' +
        '<th>' + Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.resultingValue') + '</th>' +
        '</tr></thead><tbody>';

    configs.context.forEach(function (config) {
      output += '<tr>' +
          '<td>' + config.type + '</td>' +
          '<td>' + App.format.role(config.serviceName) + '</td>' +
          '<td>' + config.name + '</td>' +
          '<td>' + config.currentValue + '</td>' +
          '<td>' + config.recommendedValue + '</td>' +
          '<td>' + config.resultingValue + '</td>' +
          '</tr>';
    });

    output += '</tbody></table>';
    newWindow = window.open();
    newWindow.document.write(output);
    newWindow.focus();
  },

  serviceVersionsMap: {},

  /**
   * load version for services to display on admin service page
   * it should be fetched from the repo which corresponds to the desiredRepositoryVersionId of the service
   */
  getServiceVersionFromRepo: function () {
    if (!App.router.get('clusterController.isLoaded') || !App.router.get('clusterController.isStackVersionsLoaded')) {
      return;
    }

    var currentStackName = App.get('currentStackName');
    var currentStackVersionNumber = App.get('currentStackVersionNumber');
    var map = this.get('serviceVersionsMap');

    var stackServices = App.StackService.find().filter(function (service) {
      return service.get('stackName') === currentStackName && service.get('stackVersion') === currentStackVersionNumber;
    });

    stackServices.forEach(function (service) {
      var serviceName = service.get('serviceName');

      var installedService = App.Service.find().findProperty('serviceName', serviceName);
      var desiredRepositoryVersionId = installedService ? installedService.get('desiredRepositoryVersionId') : null;
      var serviceVersion = "";
      var currentStackRepoVersions = App.RepositoryVersion.find().filter(function (repoVersion) {
        return repoVersion.get('stackVersionType') === currentStackName && repoVersion.get('stackVersionNumber') === currentStackVersionNumber;
      });

      //Get service version from the current standard repo for stack services which are not installed else search by id
      if(!desiredRepositoryVersionId) {
        currentStackRepoVersions.forEach(function(repoVersion) {
          if(repoVersion.get('isCurrent') && repoVersion.get('isStandard')) {
            serviceVersion = repoVersion.get('stackServices').findProperty('name', serviceName).get('latestVersion');
          }
        })
      } else {
        var repoVersion = currentStackRepoVersions.findProperty('id', desiredRepositoryVersionId);
        serviceVersion = repoVersion.get('stackServices').findProperty('name', serviceName).get('latestVersion');
      }
      map[serviceName] = serviceVersion;
    });
  }.observes('App.router.clusterController.isLoaded', 'App.router.clusterController.isStackVersionsLoaded')

});
