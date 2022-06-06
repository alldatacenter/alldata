/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

require('models/repository');

App.WizardStep1View = Em.View.extend({

  templateName: require('templates/wizard/step1'),

  didInsertElement: function () {
    if (this.get('controller.isLoadingComplete') && this.get('state') === 'inDOM') {
      Em.run.next(() => {
        $("[rel=skip-validation-tooltip]").tooltip({ placement: 'right'});
        $("[rel=use-redhat-tooltip]").tooltip({ placement: 'right'});
        $('.add-os-button,.redhat-label').tooltip();
        this.$().on('mouseover', '.version-contents-body .table-hover > tbody > tr', function () {
          App.tooltip($(this).find('.action .icon'), {placement: 'bottom'});
          App.tooltip($(this).find('.icon-undo'), {placement: 'bottom'});
        });
        if (this.get('controller.selectedStack.showAvailable')) {
          // first time load
          if (this.get('controller.selectedStack.useRedhatSatellite')) {
            // restore `use local repo` on page refresh
            this.get('controller').useLocalRepo();
          }
        } else {
          var selected = this.get('controller.content.stacks') && this.get('controller.content.stacks').findProperty('showAvailable');
          if (!selected) {
            // network disconnection
            Em.trySet(this, 'controller.selectedStack.useLocalRepo', true);
            Em.trySet(this, 'controller.selectedStack.usePublicRepo', false);
          }
        }
      });
    }
  }.observes('controller.isLoadingComplete'),

  willDestroyElement: function () {
    $("[rel=skip-validation-tooltip]").tooltip('destroy');
    $("[rel=use-redhat-tooltip]").tooltip('destroy');
    $('.add-os-button,.redhat-label').tooltip('destroy');
    $('.icon-undo').tooltip('destroy');
    $('.action .icon').tooltip('destroy');
  },

  /**
   * Show possible reasons why Public Repo is disabled
   *
   * @returns {App.ModalPopup}
   */
  openPublicOptionDisabledWindow: function () {
    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step1.selectUseRepoOptions.public.networkLost.popup.title'),
      bodyClass: Ember.View.extend({
        templateName: require('templates/wizard/step1/public_option_disabled_window_body')
      }),
      secondary: false
    });
  },

  removeOS() {
    $('.action .icon').tooltip('destroy');
    return this.get('controller').removeOS(...arguments);
  },

  editableRepoView: Em.View.extend({
    templateName: require('templates/wizard/step1/editable_repo'),
    classNames: ['editable-repo'],
    /**
     * @type {boolean}
     */
    isEditing: false,

    /**
     * @type {?App.Repository}
     */
    repository: null,

    /**
     * @type {boolean}
     */
    showEditIcon: Em.computed.and('controller.selectedStack.useRedhatSatellite', '!isEditing'),

    /**
     * @type {boolean}
     */
    showRevertIcon: function() {
      return this.get('isEditing') && (this.get('repository.repoId') !== this.get('repository.originalRepoId'));
    }.property('isEditing', 'repository.repoId'),

    didInsertElement: function() {
      this.set('isEditing', false);
    }.observes('controller.selectedStack.useRedhatSatellite'),

    revertToOriginal: function() {
      this.set('repository.repoId', this.get('repository.originalRepoId'))
    },

    editRepoId: function() {
      this.set('isEditing', true);
    }
  }),

  /**
   * Disable submit button flag
   *
   * @type {bool}
   */
  isSubmitDisabled: Em.computed.or('invalidFormatUrlExist', 'isNoOsChecked', 'isNoOsFilled', 'controller.content.isCheckInProgress', 'App.router.btnClickInProgress', '!controller.isLoadingComplete'),

  /**
   * Show warning message flag
   *
   * @type {bool}
   */
  warningExist: Em.computed.or('invalidFormatUrlExist', 'isNoOsChecked', 'isNoOsFilled'),

  skipVerifyBaseUrl: Em.computed.or('controller.selectedStack.skipValidationChecked', 'controller.selectedStack.useRedhatSatellite'),

  verifyBaseUrl: Em.computed.not('skipVerifyBaseUrl'),

  showWarning: Em.computed.and('warningExist', 'verifyBaseUrl'),

  /**
   * Onclick handler for recheck repos urls. Used in Advanced Repository Options.
   */
  retryRepoUrls: function () {
    App.router.get('installerController').checkRepoURL(this.get('controller'));
  },

  /**
   * Radio button for use Public repo
   *
   * @type {App.RadioButtonView}
   */
  usePublicRepoRadioButton: App.RadioButtonView.extend({
    labelTranslate: 'installer.step1.selectUseRepoOptions.public',
    checked: Em.computed.alias('controller.selectedStack.usePublicRepo'),

    change: function () {
      this.get('controller').usePublicRepo();
    }
  }),

  /**
   * Checkbox for use Public repo
   *
   * @type {App.RadioButtonView}
   */
  useLocalRepoRadioButton: App.RadioButtonView.extend({
    labelTranslate: 'installer.step1.selectUseRepoOptions.local',
    checked: Em.computed.alias('controller.selectedStack.useLocalRepo'),

    change: function () {
      this.get('controller').useLocalRepo();
    }
  }),

  /**
   * User already selected all OSes
   *
   * @type {boolean}
   */
  allOsesSelected: Em.computed.everyBy('controller.selectedStack.operatingSystems', 'isSelected', true),

  /**
   * Disallow adding OS if all OSes are already added or user select <code>useRedhatSatellite</code>
   *
   * @type {boolean}
   */
  isAddOsButtonDisabled: Em.computed.or('allOsesSelected', 'controller.selectedStack.useRedhatSatellite'),

  /**
   * Tooltip for Add OS Button
   * Empty if this button is enabled
   *
   * @type {string}
   */
  addOsButtonTooltip: Em.computed.ifThenElse('allOsesSelected', Em.I18n.t('installer.step1.addOs.disabled.tooltip'), ''),

  /**
   * Tooltip for useRedhatSatellite block
   * Empty if usage Redhat is enabled
   *
   * @type {string}
   */
  redhatDisabledTooltip: Em.computed.ifThenElse('controller.selectedStack.usePublicRepo', Em.I18n.t('installer.step1.advancedRepo.useRedhatSatellite.disabled.tooltip'), ''),
  /**
   * List of all repositories under selected stack operatingSystems
   *
   * @type {App.Repository[]}
   */
  allRepositories: function () {
    return this.getWithDefault('controller.selectedStack.repositories', []);
  }.property('controller.selectedStack.repositories.[]'),

  /**
   * Verify if some repo has invalid base-url
   * Ignore if <code>useRedhatSatellite</code> is true for selected stack
   *
   * @type {bool}
   */
  invalidFormatUrlExist: function () {
    var allRepositories = this.get('allRepositories');
    if (!allRepositories) {
      return false;
    }
    if (this.get('controller.selectedStack.useRedhatSatellite')) {
      allRepositories = allRepositories.filter(this.isRedhat);
    }
    return allRepositories.someProperty('invalidFormatError', true);
  }.property('controller.selectedStack.useRedhatSatellite', 'allRepositories.@each.invalidFormatError'),

  /**
   * Verify if some invalid repo-urls exist
   * @type {bool}
   */
  invalidUrlExist: Em.computed.someBy('allRepositories', 'validation', 'INVALID'),

  /**
   * If all repo links are unchecked
   * @type {bool}
   */
  isNoOsChecked: Em.computed.everyBy('controller.selectedStack.operatingSystems', 'isSelected', false),

  /**
   *
   * @param {App.Repository} item
   * @returns {boolean}
   */
  isRedhat: function(item) {
    return Boolean(item.get('osType') && item.get('osType').contains('redhat'));
  },

  /**
   * If all OSes are empty
   * @type {bool}
   */
  isNoOsFilled: function () {
    var operatingSystems = this.get('controller.selectedStack.operatingSystems');
    if (this.get('controller.selectedStack.useRedhatSatellite') || Em.isNone(operatingSystems)) {
      return false;
    }
    var selectedOS = operatingSystems.filterProperty('isSelected', true);
    return selectedOS.everyProperty('isNotFilled', true);
  }.property('controller.selectedStack.operatingSystems.@each.isSelected', 'controller.selectedStack.operatingSystems.@each.isNotFilled', 'controller.selectedStack.useRedhatSatellite'),

  popoverView: Em.View.extend({
    tagName: 'i',
    classNameBindings: ['repository.validationClassName'],
    attributeBindings: ['repository.errorTitle:data-original-title', 'repository.errorContent:data-content'],
    didInsertElement: function () {
      App.popover($(this.get('element')), {
        template: '<div class="popover"><div class="arrow"></div><div class="popover-inner"><h3 class="popover-title"></h3><div class="popover-content"></div></div></div>',
        trigger: 'hover'
      });
    }
  }),

  /**
   * @type {App.CheckboxView}
   */
  redhatCheckBoxView: App.CheckboxView.extend({
    checkedBinding: 'controller.selectedStack.useRedhatSatellite',
    disabledBinding: 'controller.selectedStack.usePublicRepo',
    click: function () {
      if (!this.get('disabled')) {
        if (this.get('controller.selectedStack.useRedhatSatellite')) {
          this.toggleProperty('controller.selectedStack.useRedhatSatellite');
        } else {
          App.showConfirmationPopup(
            () => this.toggleProperty('controller.selectedStack.useRedhatSatellite'),
            Em.I18n.t('installer.step1.advancedRepo.useRedhatSatellite.warning'),
            Em.K,
            Em.I18n.t('installer.step1.advancedRepo.useRedhatSatellite.message')
          );
        }
      }
      return false;
    }
  }),

  repositoryTextField: Ember.TextField.extend({
    repository: null,
    placeholderBinding: "repository.placeholder",
    valueBinding: "repository.baseUrl"
  }),

  /**
   * Handler when editing any repo BaseUrl
   *
   * @method editLocalRepository
   */
  editLocalRepository: function () {
    //upload to content
    var repositories = this.get('allRepositories');
    if (!repositories) {
      return;
    }
    repositories.forEach(function (repository) {
      if (repository.get('lastBaseUrl') !== repository.get('baseUrl')) {
        repository.setProperties({
          lastBaseUrl: repository.get('baseUrl'),
          validation: 'PENDING'
        });
      }
    }, this);
  }.observes('allRepositories.@each.baseUrl')

});
