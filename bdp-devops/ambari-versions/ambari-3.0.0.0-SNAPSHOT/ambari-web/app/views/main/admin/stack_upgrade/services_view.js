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

App.MainAdminStackServicesView = Em.View.extend({
  templateName: require('templates/main/admin/stack_upgrade/services'),

  isAddServiceAvailable: function () {
    return App.isAuthorized('CLUSTER.UPGRADE_DOWNGRADE_STACK');
  }.property('App.supports.opsDuringRollingUpgrade', 'App.upgradeState', 'App.isAdmin'),

  /**
   * @type {Array}
   */
  services: function() {
    var services = App.supports.installGanglia ? App.StackService.find() : App.StackService.find().without(App.StackService.find('GANGLIA'));
    var controller = this.get('controller');

    services.map(function(s) {
      s.set('serviceVersionDisplay', controller.get('serviceVersionsMap')[s.get('serviceName')]);
      s.set('isInstalled', App.Service.find().someProperty('serviceName', s.get('serviceName')));
      return s;
    });
    return services;
  }.property('App.router.clusterController.isLoaded', 'controller.serviceVersionsMap'),

  didInsertElement: function () {
    if (!App.get('stackVersionsAvailable')) {
      this.get('controller').loadStackVersionsToModel(true).done(function () {
        App.set('stackVersionsAvailable', App.StackVersion.find().content.length > 0);
      });
      this.get('controller').loadRepositories();
    }
  },

  /**
   * launch Add Service wizard
   * @param event
   */
  goToAddService: function (event) {
    if (!App.isAuthorized('SERVICE.ADD_DELETE_SERVICES') || !App.supports.enableAddDeleteServices) {
      return;
    } else if (event.context == "KERBEROS") {
      App.router.get('mainAdminKerberosController').checkAndStartKerberosWizard();
      App.router.get('kerberosWizardController').setDBProperty('onClosePath', 'main.admin.stackAndUpgrade.services');
    } else {
      App.router.get('addServiceController').set('serviceToInstall', event.context);
      App.router.get('addServiceController').setDBProperty('onClosePath', 'main.admin.stackAndUpgrade.services');
      App.get('router').transitionTo('main.serviceAdd');
    }
  },

  /**
   * List of all repo-groups
   * @type {Object[][]}
   */
  allRepositoriesGroups: function () {
    var repos = this.get('controller.allRepos');
    var reposGroup = [];
    var repositories = [];
    reposGroup.set('stackVersion', App.get('currentStackVersionNumber'));
    if (repos) {
      repos.forEach(function (group) {
        group.repositories.forEach (function(repo) {
          var cur_repo = Em.Object.create({
            'repoId': repo.repoId,
            'id': repo.repoId + '-' + repo.osType,
            'repoName' : repo.repoName,
            'stackName' : repo.stackName,
            'stackVersion' : repo.stackVersion,
            'baseUrl': repo.baseUrl,
            'originalBaseUrl': repo.baseUrl,
            'osType': repo.osType,
            'onEdit': false,
            'empty-error': !repo.baseUrl,
            'undo': false,
            'clearAll': repo.baseUrl
          });
          var cur_group = reposGroup.findProperty('name', group.name);
          if (!cur_group) {
            cur_group = Ember.Object.create({
              name: group.name,
              repositories: []
            });
            reposGroup.push(cur_group);
          }
          cur_group.repositories.push(cur_repo);
          repositories.push(cur_repo);
        });
      });
    }
    this.set('allRepos', repositories);
    return reposGroup;
  }.property('controller.allRepos'),

  /**
   * Onclick handler for edit action of each repo, enter edit mode
   * @param {object} event
   */
  onEditClick:function (event) {
    var targetRepo = this.get('allRepos').findProperty('id', event.context.get('id'));
    if (targetRepo) {
      targetRepo.set('onEdit', true);
    }
  },

  /**
   * Onclick handler for undo action of each repo group
   * @method undoGroupLocalRepository
   * @param {object} event
   */
  undoGroupLocalRepository: function (event) {
    this.doActionForGroupLocalRepository(event, 'originalBaseUrl');
  },

  /**
   * Handler for clear icon click
   * @method clearGroupLocalRepository
   * @param {object} event
   */
  clearGroupLocalRepository: function (event) {
    this.doActionForGroupLocalRepository(event, '');
  },

  /**
   * Common handler for repo groups actions
   * @method doActionForGroupLocalRepository
   * @param {object} event
   * @param {string} newBaseUrlField
   */
  doActionForGroupLocalRepository: function (event, newBaseUrlField) {
    var targetRepo = this.get('allRepos').findProperty('id', event.context.get('id'));
    if (targetRepo) {
      targetRepo.set('baseUrl', Em.isEmpty(newBaseUrlField) ? '' : Em.get(targetRepo, newBaseUrlField));
    }
  },

  /**
   * Handler when editing any repo group BaseUrl
   * @method editGroupLocalRepository
   */
  editGroupLocalRepository: function () {
    var repos = this.get('allRepos');
    repos.forEach(function (targetRepo) {
      targetRepo.set('undo', targetRepo.get('baseUrl') != targetRepo.get('originalBaseUrl'));
      targetRepo.set('clearAll', targetRepo.get('baseUrl'));
      targetRepo.set('empty-error', !targetRepo.get('baseUrl'));

    });
  }.observes('allRepos.@each.baseUrl'),

  /**
   * onSuccess callback for save Repo URL.
   */
  doSaveRepoUrlsSuccessCallback: function (response, request, data) {
    var id = data.repoId + '-' + data.osType;
    var targetRepo = this.get('allRepos').findProperty('id', id);
    if (!targetRepo) {
      return;
    } else {

      var modalCloseHandler = function() {
        this.hide();
        targetRepo.set('baseUrl', data.data.Repositories.base_url);
        targetRepo.set('originalBaseUrl', data.data.Repositories.base_url);
        targetRepo.set('onEdit', false);
      };

      App.ModalPopup.show({
        header: Em.I18n.t('admin.cluster.repositories.popup.header.success'),
        secondary: null,
        onPrimary: modalCloseHandler,
        onClose: modalCloseHandler,
        message: Em.I18n.t('admin.cluster.repositories.popup.body.success'),
        bodyClass: Em.View.extend({
          template: Em.Handlebars.compile('<div class="alert alert-success">{{{message}}}</div>')
        })
      })
    }
  },

  /**
   * onError callback for save Repo URL.
   */
  doSaveRepoUrlsErrorCallback: function (request, ajaxOptions, error, data) {
    var self = this;
    var id = data.url.split('/')[10] + '-' + data.url.split('/')[8];
    var targetRepo = this.get('allRepos').findProperty('id', id);
    if (targetRepo) {
      App.ModalPopup.show({
        header: Em.I18n.t('admin.cluster.repositories.popup.header.fail'),
        primary: Em.I18n.t('common.saveAnyway'),
        secondary: Em.I18n.t('common.revert'),
        third: Em.I18n.t('common.cancel'),
        onPrimary: function () {
          // save anyway: Go ahead and save with Repo URL validation turned off and close Dialog when done.
          this.hide();
          self.doSaveRepoUrls(id, false);
        },
        onSecondary: function () {
          // Revert: Close dialog, revert URL value, go back to non-Edit mode
          this.hide();
          targetRepo.set('baseUrl', targetRepo.get('originalBaseUrl'));
          targetRepo.set('onEdit', false);
        },
        onThird: function () {
          // cancel: Close dialog but stay in Edit mode
          this.hide();
        },
        message: Em.I18n.t('admin.cluster.repositories.popup.body.fail'),
        bodyClass: Em.View.extend({
          template: Em.Handlebars.compile('<div class="alert alert-warning">{{{message}}}</div>')
        })
      })
    }
  },

  /**
   * Check validation and Save the customized local urls
   */
  doSaveRepoUrls: function (id, verifyBaseUrl) {
    var targetRepo = this.get('allRepos').findProperty('id', id);
    App.ajax.send({
      name: 'wizard.advanced_repositories.valid_url',
      sender: this,
      data: {
        stackName: targetRepo.stackName,
        stackVersion: targetRepo.stackVersion,
        repoId: targetRepo.repoId,
        osType: targetRepo.osType,
        data: {
          'Repositories': {
            'base_url': targetRepo.baseUrl,
            "verify_base_url": verifyBaseUrl
          }
        }
      },
      success: 'doSaveRepoUrlsSuccessCallback',
      error: 'doSaveRepoUrlsErrorCallback'
    });
  },
  /**
   * Check validation and Save the customized local urls
   */
  saveRepoUrls: function (event) {
    this.doSaveRepoUrls(event.context.get('id'), true);
  },

  /**
   * on click handler 'Cancel' for current repo in edit mode
   */
  doCancel: function (event) {
    var targetRepo = this.get('allRepos').findProperty('id', event.context.get('id'));
    if (targetRepo) {
      targetRepo.set('baseUrl', targetRepo.get('originalBaseUrl'));
      targetRepo.set('onEdit', false);
    }
  }
});
