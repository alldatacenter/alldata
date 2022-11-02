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

App.UpgradeVersionBoxView = Em.View.extend({
  templateName: require('templates/main/admin/stack_upgrade/upgrade_version_box'),
  classNames: ['version-box'],
  classNameBindings: ['versionClass'],

  /**
   * @type {string}
   * @constant
   */
  PROGRESS_STATUS: 'IN_PROGRESS',

  /**
   * @type {boolean}
   */
  upgradeCheckInProgress: false,

  /**
   * progress of version installation
   * @type {number}
   */
  installProgress: function() {
    if (App.get('testMode')) return 100;

    var installRequest, requestIds = this.get('controller').getRepoVersionInstallId();
    if (requestIds) {
      installRequest = App.router.get('backgroundOperationsController.services').findProperty('id', requestIds[0]);
    }
    return (installRequest) ? installRequest.get('progress') : 0;
  }.property('App.router.backgroundOperationsController.serviceTimestamp'),

  /**
   * version is upgrading or downgrading
   * @type {boolean}
   */
  isUpgrading: function () {
    return (this.get('controller.upgradeVersion') === this.get('content.displayName') ||
            this.get('controller.fromVersion') === this.get('content.repositoryVersion'))
            && App.get('upgradeState') !== 'NOT_REQUIRED';
  }.property('App.upgradeState', 'content.displayName', 'controller.upgradeVersion'),

  isRepoUrlsEditDisabled: function () {
    return ['INSTALLING', 'UPGRADING'].contains(this.get('content.status')) || this.get('isUpgrading') || (!App.isAuthorized('AMBARI.MANAGE_STACK_VERSIONS'));
  }.property('content.status', 'isUpgrading'),

  /**
   * @type {string}
   */
  versionClass: function () {
    return this.get('content.status') === 'CURRENT' ? 'current-version-box' : '';
  }.property('content.status'),

  /**
   * @type {boolean}
   */
  isOutOfSync: Em.computed.equal('content.status', 'OUT_OF_SYNC'),

  /**
   * map containing version (id, label)
   * this is used as param for <code>showHosts<code> method
   * @type {Object}
   */
  versionStateMap: {
    'current': {
      'type': 'CURRENT',
      'value': ['CURRENT'],
      'property': 'currentHosts',
      'label': Em.I18n.t('admin.stackVersions.hosts.popup.header.current')
    },
    'installed': {
      'type': 'INSTALLED',
      'value': ['INSTALLED'],
      'property': 'installedHosts',
      'label': Em.I18n.t('admin.stackVersions.hosts.popup.header.installed')
    },
    'not_installed': {
      'type': 'NOT_INSTALLED',
      'value': ['INSTALLING', 'INSTALL_FAILED', 'OUT_OF_SYNC'],
      'property': 'notInstalledHosts',
      'label': Em.I18n.t('admin.stackVersions.hosts.popup.header.not_installed')
    }
  },

  /**
   * @type {object}
   * @default null
   */
  content: null,

  currentLabelClass: 'label label-success',

  /**
   * map of properties which correspond to particular state of Upgrade version
   * @type {object}
   */
  statePropertiesMap: function () {
    return {
      'CURRENT': {
        isLabel: true,
        text: Em.I18n.t('common.current'),
        class: this.get('currentLabelClass')
      },
      'NOT_REQUIRED': {
        isButton: true,
        text: Em.I18n.t('admin.stackVersions.version.installNow'),
        action: 'installRepoVersionPopup'
      },
      'LOADING': {
        isSpinner: true,
        class: 'spinner'
      },
      'INSTALLING': {
        iconClass: 'glyphicon glyphicon-cog',
        isLink: true,
        text: Em.I18n.t('hosts.host.stackVersions.status.installing'),
        action: 'showProgressPopup'
      },
      'INSTALLED': {
        iconClass: 'glyphicon glyphicon-ok',
        isButtonGroup: true,
        text: Em.I18n.t('common.installed'),
        action: null
      },
      'SUSPENDED': {
        isButton: true,
        text: Em.I18n.t('admin.stackUpgrade.dialog.resume'),
        action: 'resumeUpgrade'
      },
      'CURRENT_PATCH': {
        isLabel: true,
        text: Em.I18n.t('common.current'),
        class: this.get('currentLabelClass')
      },
      'CURRENT_PATCH_REVERTABLE': {
        isButtonGroup: true,
        text: Em.I18n.t('common.current'),
        action: null,
        buttons: [
          {
            text: Em.I18n.t('common.revert'),
            action: 'confirmRevertPatchUpgrade'
          }
        ]
      }
    };
  }.property(),

  /**
   * object that describes how content should be displayed
   * @type {Em.Object}
   * TODO remove <code>isUpgrading</code> condition when transition of version states in API fixed
   */
  stateElement: function () {
    var statePropertiesMap = this.get('statePropertiesMap');
    var status = this.get('content.status');
    var element = Em.Object.create({
      status: status,
      isInstalling: Em.computed.equal('status', 'INSTALLING'),
      buttons: [],
      isDisabled: false
    });
    var isSuspended = App.get('upgradeSuspended');

    if (status === 'CURRENT' && this.get('content.isPatch') && !this.get('isUpgrading')) {
      if (this.get('content.stackVersion.supportsRevert')) {
        element.setProperties(statePropertiesMap['CURRENT_PATCH_REVERTABLE']);
      } else {
        element.setProperties(statePropertiesMap['CURRENT_PATCH']);
      }
    }
    else if (['INSTALLING', 'CURRENT'].contains(status)) {
      element.setProperties(statePropertiesMap[status]);
    }
    else if (status === 'NOT_REQUIRED') {
      this.processNotRequiredState(element);
    }
    else if ((status === 'INSTALLED' && !this.get('isUpgrading')) || (['INSTALL_FAILED', 'OUT_OF_SYNC'].contains(status))) {
      this.processPreUpgradeState(element);
    }
    else if (this.get('isUpgrading') && !isSuspended) {
      this.processUpgradingState(element);
    }
    else if (isSuspended) {
      this.processSuspendedState(element);
    }
    //For restricted upgrade wizard should be disabled in any state
    if (this.get('controller.isWizardRestricted') || (!App.isAuthorized('CLUSTER.UPGRADE_DOWNGRADE_STACK'))) {
      element.set('isDisabled', true);
    }
    return element;
  }.property(
    'content.status',
    'controller.isDowngrade',
    'isUpgrading',
    'controller.requestInProgress',
    'controller.requestInProgressRepoId',
    'parentView.repoVersions.@each.status',
    'isCurrentStackPresent'
  ),

  /**
   * check if actions of NOT_REQUIRED stack version disabled
   * @returns {boolean}
   */
  isDisabledOnInit: function() {
    return  this.get('controller.requestInProgress') ||
            !this.get('isCurrentStackPresent') ||
            !this.get('content.isCompatible') ||
            (App.get('upgradeIsRunning') && !App.get('upgradeSuspended')) ||
            this.get('parentView.repoVersions').someProperty('status', 'INSTALLING');
  },

  /**
   * @param {Em.Object} element
   */
  processSuspendedState: function(element) {
    element.setProperties(this.get('statePropertiesMap')['SUSPENDED']);
    var text = this.get('controller.isDowngrade')
      ? Em.I18n.t('admin.stackUpgrade.dialog.resume.downgrade')
      : Em.I18n.t('admin.stackUpgrade.dialog.resume');
    element.set('text', this.get('isVersionColumnView') ? Em.I18n.t('common.resume'): text);
    element.set('isDisabled', this.get('controller.requestInProgress'));
  },

  /**
   * @param {Em.Object} element
   */
  processUpgradingState: function(element) {
    element.set('isLink', true);
    element.set('action', 'openUpgradeDialog');
    if (['HOLDING', 'HOLDING_FAILED', 'HOLDING_TIMEDOUT', 'ABORTED'].contains(App.get('upgradeState'))) {
      element.set('iconClass', 'glyphicon glyphicon-pause');
      if (this.get('controller.isDowngrade')) {
        element.set('text', Em.I18n.t('admin.stackVersions.version.downgrade.pause'));
      }
      else {
        element.set('text', Em.I18n.t('admin.stackVersions.version.upgrade.pause'));
      }
    }
    else {
      element.set('iconClass', 'glyphicon glyphicon-cog');
      if (this.get('controller.isDowngrade')) {
        element.set('text', Em.I18n.t('admin.stackVersions.version.downgrade.running'));
      }
      else {
        element.set('text', Em.I18n.t('admin.stackVersions.version.upgrade.running'));
      }
    }
  },

  /**
   * @param {Em.Object} element
   */
  processPreUpgradeState: function(element) {
    var currentVersion = this.get('controller.currentVersion');
    var status = this.get('content.status');
    var isVersionHigherThanCurrent = stringUtils.compareVersions(
        this.get('content.repositoryVersion'),
        Em.get(currentVersion, 'repository_version')
      ) === 1;
    var isDisabled = this.isDisabledOnInstalled();
    if (Em.get(currentVersion, 'stack_name') !== this.get('content.stackVersionType') || isVersionHigherThanCurrent) {
      switch (status){
        case 'OUT_OF_SYNC':
          if (Em.isNone(currentVersion)) {
            element.set('text', Em.I18n.t('admin.stackVersions.version.installError'));
            element.set('iconClass', 'glyphicon glyphicon-warning-sign');
            element.set('isLabel', true);
          } else {
            element.set('isButton', true);
            element.set('text', this.get('isVersionColumnView') ? Em.I18n.t('common.reinstall') : Em.I18n.t('admin.stackVersions.version.reinstall'));
            element.set('action', 'installRepoVersionPopup');
          }
          break;
        case 'INSTALL_FAILED':
          if (Em.isNone(currentVersion)) {
            element.set('text', Em.I18n.t('admin.stackVersions.version.installError'));
            element.set('iconClass', 'glyphicon glyphicon-warning-sign');
            element.set('isLabel', true);
          } else {
            element.set('isButton', true);
            element.set('text', this.get('isVersionColumnView') ? Em.I18n.t('common.reinstall') : Em.I18n.t('admin.stackVersions.version.reinstall'));
            element.set('action', 'installRepoVersionPopup');
          }
          break;
        default:
          var isVersionColumnView = this.get('isVersionColumnView');
          var stackServices = this.get('content.stackServices');
          var isUpgradable = stackServices && (this.get('content.isStandard') || stackServices.some( function(stackService){
              return stackService.get('isUpgradable');
          }));
          var isPatch = this.get('content.isPatch');
          var isMaint = this.get('content.isMaint');

          element.set('isButtonGroup', true);
          if (isUpgradable){
            element.set('text', isVersionColumnView ? Em.I18n.t('common.upgrade') : Em.I18n.t('admin.stackVersions.version.performUpgrade'));
            element.set('action', 'confirmUpgrade');
            element.get('buttons').pushObject({
              text: isVersionColumnView ? Em.I18n.t('common.reinstall') : Em.I18n.t('admin.stackVersions.version.reinstall'),
              action: 'installRepoVersionPopup',
              isDisabled: isDisabled
            });

            element.get('buttons').pushObject({
              text: Em.I18n.t('admin.stackVersions.version.preUpgradeCheck'),
              action: 'showUpgradeOptions',
              isDisabled: isDisabled
            });
          }
          else{
            element.set('iconClass', 'icon-ok');
            element.set('text', Em.I18n.t('common.installed'))
          }

          if ( isPatch || isMaint ) {
            element.get('buttons').pushObject({
              text: Em.I18n.t('common.hide'),
              action: 'confirmDiscardRepoVersion',
              isDisabled: isDisabled
            });
          }

      }
      element.set('isDisabled', isDisabled);
    }
    else {
      element.setProperties(this.get('statePropertiesMap')['INSTALLED']);
      if (this.get('content.isPatch') || this.get('content.isMaint')) {
        element.get('buttons').pushObject({
          text: Em.I18n.t('common.hide'),
          action: 'confirmDiscardRepoVersion',
          isDisabled: isDisabled
        });
      }
    }
  },

  /**
   * @param {Em.Object} element
   */
  processNotRequiredState: function(element) {
    var isDisabledOnInit = this.isDisabledOnInit();
    var requestInProgressRepoId = this.get('controller.requestInProgressRepoId');
    var status = this.get('content.status');

    if (requestInProgressRepoId && requestInProgressRepoId === this.get('content.id')) {
      element.setProperties(this.get('statePropertiesMap')['LOADING']);
    } else {
      element.setProperties(this.get('statePropertiesMap')[status]);
    }
    element.set('isDisabled', isDisabledOnInit);
    element.set('isButtonGroup', true);
    element.set('isButton', false);
    element.get('buttons').pushObject({
      text: Em.I18n.t('common.hide'),
      action: 'confirmDiscardRepoVersion',
      isDisabled: isDisabledOnInit
    });
  },

  /**
   * check if actions of INSTALLED stack version disabled
   * @returns {boolean}
   */
  isDisabledOnInstalled: function() {
    return !this.get('isCurrentStackPresent') ||
      !App.isAuthorized('CLUSTER.UPGRADE_DOWNGRADE_STACK') ||
      this.get('controller.requestInProgress') ||
      this.get('parentView.repoVersions').someProperty('status', 'INSTALLING') ||
      (this.get('controller.isDowngrade') &&
        (this.get('controller.currentVersion.repository_name') === this.get('controller.upgradeVersion'))
      );
  },

  didInsertElement: function () {
    App.tooltip($('.link-tooltip'), {title: Em.I18n.t('admin.stackVersions.version.linkTooltip')});
    App.tooltip($('.hosts-tooltip'));
    App.tooltip($('.out-of-sync-badge'), {title: Em.I18n.t('hosts.host.stackVersions.status.out_of_sync')});
    if (!this.get('content.isCompatible')) {
      App.tooltip(this.$(".repo-version-tooltip"), {
        title: Em.I18n.t('admin.stackVersions.version.noCompatible.tooltip')
      });
    }
    Em.run.later(this, function () {
      if (this.get('state') !== 'inDOM') {
        return;
      }
      if (this.get('maintenanceHosts').length + this.get('notRequiredHosts').length) {
        App.tooltip(this.$('.hosts-section'), {placement: 'bottom', title: Em.I18n.t('admin.stackVersions.version.hostsInfoTooltip').format(
          this.get('maintenanceHosts').length + this.get('notRequiredHosts').length, this.get('maintenanceHosts').length, this.get('notRequiredHosts').length
        )});
      }
    }, 1000);
  },

  willDestroyElement: function () {
    $('.link-tooltip').tooltip('destroy');
    $('.hosts-tooltip').tooltip('destroy');
    $('.out-of-sync-badge').tooltip('destroy');
  },

  isCurrentStackPresent: Ember.computed('parentView.repoVersions.@each.stackVersion.state', function () {
    return this.get('parentView.repoVersions').someProperty('stackVersion.state', 'CURRENT');
  }),

  /**
   * run custom action of controller
   */
  runAction: function (event) {
    var target = event && event.target,
      action = event && event.context || this.get('stateElement.action');
    if (target && ($(target).hasClass('disabled') || $(target).parent().hasClass('disabled'))) {
      return;
    }
    if (action) {
      this.get('controller')[action](this.get('content'));
    }
  },

  /**
   * @param App.RepositoryVersion
   * */
  getStackVersionNumber: function(repository){
    var stackVersion = null;
    var systems = repository.get('operatingSystems');

    systems.forEach(function (os) {
      os.get('repositories').forEach(function (repo) {
        stackVersion = repo.get('stackVersion');
        if (null != stackVersion)
          return stackVersion;
      });
    });

    return stackVersion;
  },

  /**
   * show popup with repositories to edit
   * @return {App.ModalPopup}
   */
  editRepositories: function () {
    var self = this;
    var repoRecord = App.RepositoryVersion.find(this.get('content.id'));
    //make deep copy of repoRecord
    var repo = Em.Object.create({
      repoVersionId: repoRecord.get('id'),
      displayName: repoRecord.get('displayName'),
      repositoryVersion: repoRecord.get('displayName'),
      stackVersion: self.getStackVersionNumber(repoRecord),
      useRedhatSatellite: repoRecord.get('useRedhatSatellite'),
      operatingSystems: repoRecord.get('operatingSystems').map(function (os) {
        return Em.Object.create({
          osType: os.get('osType'),
          isSelected: true,
          repositories: os.get('repositories').map(function (repository) {
            return Em.Object.create({
              repoName: repository.get('repoName'),
              repoId: repository.get('repoId'),
              baseUrl: repository.get('baseUrl'),
              hasError: false
            });
          })
        });
      })
    });

    return this.get('isRepoUrlsEditDisabled') ? null : App.ModalPopup.show({
      classNames: ['repository-list', 'common-modal-wrapper'],
      modalDialogClasses: ['modal-lg'],
      skipValidation: false,
      autoHeight: false,
      /**
       * @type {boolean}
       */
      serverValidationFailed: false,
      bodyClass: Ember.View.extend({
        content: repo,
        skipCheckBox: App.CheckboxView.extend({
          repoBinding: 'parentView.content',
          checkboxClassNames: ["align-checkbox"],
          change: function() {
            this.get('repo.operatingSystems').forEach(function(os) {
              os.get('repositories').forEach(function(repo) {
                repo.set('skipValidation', this.get('checked'));
              }, this);
            }, this);
          }
        }),

        /**
         * set <code>disablePrimary</code> of popup depending on base URL validation
         */
        uiValidation: function () {
          var disablePrimary = !(App.get('isAdmin') && !App.get('isOperator'));

          this.get('content.operatingSystems').forEach(function (os) {
            os.get('repositories').forEach(function (repo) {
              disablePrimary = !disablePrimary ? repo.get('hasError') : disablePrimary;
            }, this);
          }, this);
          this.set('parentView.disablePrimary', disablePrimary);
        },
        templateName: require('templates/main/admin/stack_upgrade/edit_repositories'),
        didInsertElement: function () {
          App.tooltip($("[rel=skip-validation-tooltip], [rel=use-redhat-tooltip]"), {placement: 'right'});
        },
        willDestroyElement: function () {
          $("[rel=skip-validation-tooltip], [rel=use-redhat-tooltip]").tooltip('destroy');
        }
      }),
      header: Em.I18n.t('common.repositories'),
      primary: Em.I18n.t('common.save'),
      disablePrimary: false,
      onPrimary: function () {
        var self = this;
        App.get('router.mainAdminStackAndUpgradeController').saveRepoOS(repo, this.get('skipValidation')).done(function(data){
          if (data.length > 0) {
            self.set('serverValidationFailed', true);
            self.set('disablePrimary', true);
          } else {
            self.hide();
          }
        })
      }
    });
  },

  /**
   * shows popup with listed hosts wich has current state of hostStackVersion
   * @param {object} event
   * @returns {App.ModalPopup}
   * @method showHostsListPopup
   */
  showHosts: function (event) {
    var status = event.contexts[0];
    var displayName = this.get('content.displayName');
    var hosts = this.get(status['property']);
    var self = this;
    var title = status.type === 'CURRENT'
      ? Em.I18n.t('admin.stackVersions.hosts.popup.current.title').format(displayName, hosts.length)
      : Em.I18n.t('admin.stackVersions.hosts.popup.title').format(displayName, status.label, hosts.length);
    hosts.sort();
    if (hosts.length) {
      return App.ModalPopup.show({
        bodyClass: Ember.View.extend({
          title: title,
          hosts: hosts,
          template: Em.Handlebars.compile('<h4>{{view.title}}</h4><div class="limited-height-2">{{#each view.hosts}}<div>{{this}}</div>{{/each}}</div>')
        }),
        header: Em.I18n.t('admin.stackVersions.hosts.popup.header').format(status.label),
        primary: Em.I18n.t('admin.stackVersions.hosts.popup.primary'),
        secondary: Em.I18n.t('common.close'),
        onPrimary: function () {
          this.hide();
          if ($('.version-box-popup .modal')) {
            $('.version-box-popup .modal .modal-footer .btn-success').click();
          }
          self.filterHostsByStack(displayName, status.value);
        }
      });
    }
  },

  /**
   * goes to the hosts page with content filtered by repo_version_name and repo_version_state
   * @param {string} displayName
   * @param {Array} states
   * @method filterHostsByStack
   */
  filterHostsByStack: function (displayName, states) {
    if (Em.isNone(displayName) || Em.isNone(states) || !states.length) return;
    App.router.get('mainHostController').filterByStack(displayName, states);
    App.router.get('mainHostController').set('showFilterConditionsFirstLoad', true);
    App.router.get('mainHostController').set('filterChangeHappened', true);
    App.router.transitionTo('hosts.index');
  },

  /**
   * Not installed hosts should exclude 1.not required hosts 2. Maintenance Mode hosts,
   * or it maybe confusing to users
   * @type {Array}
   */
  notInstalledHosts: function () {
    var notInstalledHosts = this.get('content.notInstalledHosts') || App.get('allHostNames');
    var notRequiredHosts = this.get('notRequiredHosts');
    var maintenanceHosts = this.get('maintenanceHosts');
    if (notInstalledHosts.length && notRequiredHosts.length) {
      notRequiredHosts.forEach(function(not_required) {
        var index = notInstalledHosts.indexOf(not_required);
        if (index > -1) {
          notInstalledHosts.splice(index, 1);
        }
      });
    }
    if (notInstalledHosts.length && maintenanceHosts.length) {
      maintenanceHosts.forEach(function(mm_host) {
        var index = notInstalledHosts.indexOf(mm_host);
        if (index > -1) {
          notInstalledHosts.splice(index, 1);
        }
      });
    }
    return notInstalledHosts;
  }.property('content.notInstalledHosts', 'notRequiredHosts', 'maintenanceHosts'),

  /**
   * @type {Array}
   */
  maintenanceHosts: function () {
    return App.Host.find().filterProperty('passiveState', 'ON').mapProperty('hostName') || [];
  }.property(''),

  /**
   * Host with no HDP component is not required to install new version
   * @type {Array}
   */
  notRequiredHosts: function () {
    var notRequiredHosts = [];
    App.Host.find().forEach(function(host) {
      if (!host.get('hostComponents').someProperty('isHDPComponent')) {
        notRequiredHosts.push(host.get('hostName'));
      }
    });
    return notRequiredHosts.uniq() || [];
  }.property(''),

  /**
   * @type {Array}
   */
  installedHosts: function () {
    return this.get('content.installedHosts') || [];
  }.property('content.installedHosts'),

  /**
   * @type {Array}
   */
  currentHosts: function () {
    return this.get('content.currentHosts') || [];
  }.property('content.currentHosts')
});
