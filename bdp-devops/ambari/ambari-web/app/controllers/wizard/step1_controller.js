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
var arrayUtils = require('utils/array_utils');

/**
 * @typedef {Em.Object} StackType
 * @property {string} stackName
 * @property {App.Stack[]} stacks
 * @property {boolean} isSelected
 */

/**
 * @type {Em.Object}
 */
var StackType = Em.Object.extend({
  stackName: '',
  stacks: [],
  isSelected: Em.computed.someBy('stacks', 'isSelected', true)
});

App.WizardStep1Controller = Em.Controller.extend({

  name: 'wizardStep1Controller',

  /**
   * Skip repo-validation
   *
   * @type {bool}
   */
  skipValidationChecked: false,

  /**
   * @type {App.Stack}
   */
  selectedStack: Em.computed.findBy('content.stacks', 'isSelected', true),

  /**
   * @type {App.ServiceSimple[]}
   */
  servicesForSelectedStack: Em.computed.filterBy('selectedStack.stackServices', 'isHidden', false),

  /**
   * Some network issues exist if there is no stack with <code>stackDefault</code> = false
   *
   * @type {boolean}
   */
  networkIssuesExist: function() {
    if (this.get('content.stacks') && this.get('content.stacks.length') > 1) {
      return this.get('content.stacks').everyProperty('stackDefault', true);
    }
    return false;
  }.property('content.stacks.@each.stackDefault'),

  /**
   * No stacks have repo update URL section (aka "latest") defined in repoinfo.xml
   *
   * @type {boolean}
   */
  stackRepoUpdateLinkExists: Em.computed.someBy('content.stacks', 'stackRepoUpdateLinkExists', true),

  optionsToSelect: {
    'usePublicRepo': {
      index: 0,
      isSelected: true
    },
    'useLocalRepo': {
      index: 1,
      isSelected: false,
      'uploadFile': {
        index: 0,
        name: 'uploadFile',
        file: '',
        hasError: false,
        isSelected: true
      },
      'enterUrl': {
        index: 1,
        name: 'enterUrl',
        url: '',
        placeholder: Em.I18n.t('installer.step1.useLocalRepo.enterUrl.placeholder'),
        hasError: false,
        isSelected: false
      }
    }
  },

  /**
   * Checks if user selected to input url or upload file but didn't do it
   * true  - url-radio is checked but url-field is empty
   *       - file-radio is checked but file is not selected
   * false - otherwise
   *
   * @type {boolean}
   */
  readInfoIsNotProvided: function () {
    var useLocalRepo = this.get('optionsToSelect.useLocalRepo');
    if(Em.get(useLocalRepo, 'uploadFile.isSelected')) {
      return !Em.get(useLocalRepo, 'uploadFile.file');
    }
    if (Em.get(useLocalRepo, 'enterUrl.isSelected')) {
      return !Em.get(useLocalRepo, 'enterUrl.url');
    }
    return false;
  }.property('optionsToSelect.useLocalRepo.isSelected', 'optionsToSelect.useLocalRepo.uploadFile.isSelected',
    'optionsToSelect.useLocalRepo.uploadFile.file', 'optionsToSelect.useLocalRepo.enterUrl.url'),

  /**
   * List of stacks grouped by <code>stackNameVersion</code>
   *
   * @type {StackType[]}
   */
  availableStackTypes: function () {
    var stacks = this.get('content.stacks');
    return stacks ? stacks.mapProperty('stackNameVersion').uniq().sort().reverse().map(function (stackName) {
      return StackType.create({
        stackName: stackName,
        stacks: stacks.filterProperty('stackNameVersion', stackName).sort(arrayUtils.sortByIdAsVersion).reverse()
      })
    }) : [];
  }.property('content.stacks.@each.stackNameVersion'),

  /**
   * @type {StackType}
   */
  selectedStackType: Em.computed.findBy('availableStackTypes', 'isSelected', true),

  isLoadingComplete: Em.computed.equal('wizardController.loadStacksRequestsCounter', 0),

  /**
   * Load selected file to current page content
   */
  readVersionInfo: function () {
    var data = {};
    var isXMLdata = false;
    if (this.get("optionsToSelect.useLocalRepo.enterUrl.isSelected")) {
      var url = this.get("optionsToSelect.useLocalRepo.enterUrl.url");
      data = {
        VersionDefinition: {
          version_url: url
        }
      };
      App.db.setLocalRepoVDFData(url);
    }
    else {
      if (this.get("optionsToSelect.useLocalRepo.uploadFile.isSelected")) {
        isXMLdata = true;
        // load from file browser
        data = this.get("optionsToSelect.useLocalRepo.uploadFile.file");
        App.db.setLocalRepoVDFData(data);
      }
    }
    return App.router.get('installerController').postVersionDefinitionFile(isXMLdata, data);
  },

  /**
   * On click handler for removing OS
   */
  removeOS: function (event) {
    if (this.get('selectedStack.useRedhatSatellite')) {
      return;
    }
    var osToRemove = event.context;
    Em.set(osToRemove, 'isSelected', false);
  },

  /**
   * On click handler for adding new OS
   */
  addOS: function (event) {
    var osToAdd = event.context;
    Em.set(osToAdd, 'isSelected', true);
  },

  /**
   * Use Local Repo if some network issues exist
   */
  onNetworkIssuesExist: function() {
    if (this.get('networkIssuesExist')) {
      this.get('content.stacks').forEach(function(stack) {
        if (stack.get('useLocalRepo') !== true) {
          stack.setProperties({
            usePublicRepo: false,
            useLocalRepo: true
          });
          stack.cleanReposBaseUrls();
        }
      });
    }
  }.observes('networkIssuesExist'),

  /**
   * Select stack with field equal to the value
   * Example:
   * <pre>
   *   selectStackBy('id', 'HDP-2.5-2.5.0.0'); // select stack with id = 'HDP-2.5-2.5.0.0'
   *   selectStackBy('stackNameVersion', 'HDP-2.5'); // select first stack with stackNameVersion = 'HDP-2.5'
   * </pre>
   *
   * @param {string} field
   * @param {string} value
   */
  selectStackBy: function (field, value) {
    this.get('content.stacks').setEach('isSelected', false);
    this.get('content.stacks').findProperty(field, value).set('isSelected', true);
  },

  /**
   * Restore base urls for selected stack when user select to use public repository
   */
  usePublicRepo: function () {
    var selectedStack = this.get('selectedStack');
    if (selectedStack) {
      selectedStack.setProperties({
        useRedhatSatellite: false,
        usePublicRepo: true,
        useLocalRepo: false
      });
      selectedStack.restoreReposBaseUrls();
    }
  },

  /**
   * Clean base urls for selected stack when user select to use local repository
   */
  useLocalRepo: function () {
    var selectedStack = this.get('selectedStack');
    if (selectedStack) {
      selectedStack.setProperties({
        usePublicRepo: false,
        useLocalRepo: true
      });
      selectedStack.cleanReposBaseUrls();
    }
  },

  /**
   * Restores url value to be its default value.
   * @method doRestoreDefaultValue
   */
  doRestoreDefaultValue: function (event) {
    var repo = event.contexts[0];
    repo.set('baseUrl', repo.get('baseUrlInit'));
  },

  /**
   * Restores url value to empty string.
   * @method doRestoreToEmpty
   */
  doRestoreToEmpty: function (event) {
    var repo = event.contexts[0];
    repo.set('baseUrl', '');
  },

  /**
   * Click-handler for left-tabs with stack types
   * Select first available stack with stackName equal to chosen
   *
   * @param {{context: StackType}} event
   */
  selectRepoInList: function (event) {
    var id = this.get('availableStackTypes').findProperty('stackName', event.context.stackName).get('stacks.firstObject.id');
    this.selectStackBy('id', id);
  },

  /**
   * Click-handler for StackVersion-tabs
   *
   * @param {{context: App.Stack}} event
   */
  changeVersion: function (event) {
    this.selectStackBy('id', event.context.get('id'));
  },

  /**
   * Show popup with options to upload new version
   *
   * @returns {App.ModalPopup}
   */
  uploadVdf: function () {
    return App.ModalPopup.show({

      controller: this,

      header: Em.I18n.t('installer.step1.addVersion.title'),

      primary: Em.I18n.t('installer.step1.useLocalRepo.readButton'),

      disablePrimary: Em.computed.alias('controller.readInfoIsNotProvided'),

      'data-qa': 'vdf-modal',

      /**
       * Try to read version info from the url or file (if provided)
       */
      onPrimary: function () {
        var controller = this.get('controller');
        controller.readVersionInfo().done(function (response) {
          // load successfully, so make this local stack repo as selectedStack
          var newStackId = response.stackNameVersion + '-' + response.actualVersion;
          var oldStackNameVersion = controller.get('selectedStack.stackNameVersion');
          controller.selectStackBy('id', newStackId);
          if (oldStackNameVersion && oldStackNameVersion !== response.stackNameVersion) {
            App.showAlertPopup(Em.I18n.t('common.warning'), Em.I18n.t('installer.step1.addVersion.stackChanged.popup.body').format(oldStackNameVersion, response.stackNameVersion));
          }
          Ember.run.next(function () {
            App.tooltip($("[rel=skip-validation-tooltip]"), {html: true, placement: 'left'});
            $("[rel=use-redhat-tooltip]").tooltip({placement: 'right'});
          });
        });
        this.restoreUploadOptions();
        this._super();
      },

      /**
       * Disable url/file fields on popup-close
       */
      onSecondary: function () {
        this.restoreUploadOptions();
        this._super();
      },

      /**
       * Disable url/file fields on popup-close
       */
      onClose: function () {
        this.restoreUploadOptions();
        this._super();
      },

      /**
       * Deselect file/url radio
       */
      restoreUploadOptions: function () {
        this.set('controller.optionsToSelect.useLocalRepo.enterUrl.isSelected', false);
        this.set('controller.optionsToSelect.useLocalRepo.enterUrl.url', '');
        this.set('controller.optionsToSelect.useLocalRepo.uploadFile.isSelected', true);
        this.set('controller.optionsToSelect.useLocalRepo.uploadFile.file', '');
      },

      bodyClass: Em.View.extend({

        controller: this,

        templateName: require('templates/wizard/step1/vdf_upload'),

        /**
         * Wrapper for 'upload-file' elements
         *
         * @type {Em.View}
         */
        uploadFileView: Em.View.extend({

          classNames: ['clearfix'],

          /**
           * Radio button for Use local Repo > Upload VDF file
           *
           * @type {App.RadioButtonView}
           */
          uploadFileRadioButton: App.RadioButtonView.extend({
            labelTranslate: 'installer.step1.useLocalRepo.uploadFile',
            checked: Em.computed.alias('controller.optionsToSelect.useLocalRepo.uploadFile.isSelected')
          }),

          /**
           * Is File API available
           *
           * @type {bool}
           */
          isFileApi: window.File && window.FileReader && window.FileList,

          /**
           * Upload file is disabled when some stack is selected or url-field is selected
           *
           * @type {boolean}
           */
          fileBrowserDisabled: Em.computed.alias('controller.optionsToSelect.useLocalRepo.enterUrl.isSelected'),

          /**
           * Input to select vdf-file
           *
           * @type {Em.View}
           */
          fileInputView: Em.View.extend({
            template: Em.Handlebars.compile('<input type="file" {{bindAttr class="controller.optionsToSelect.useLocalRepo.enterUrl.isSelected:disabled"}} {{QAAttr "vdf-input"}}/>'),

            change: function (e) {
              var self = this;
              if (e.target.files && e.target.files.length === 1) {
                var file = e.target.files[0];
                var reader = new FileReader();

                reader.onload = (function () {
                  return function (event) {
                    self.set('controller.optionsToSelect.useLocalRepo.uploadFile.file', event.target.result);
                  };
                })(file);
                reader.readAsText(file);
              }
            }

          }),

          click: function () {
            if (!this.set('controller.optionsToSelect.useLocalRepo.uploadFile.isSelected')) {
              this.set('controller.optionsToSelect.useLocalRepo.enterUrl.isSelected', false);
              this.set('controller.optionsToSelect.useLocalRepo.uploadFile.isSelected', true);
              this.set('controller.optionsToSelect.useLocalRepo.enterUrl.hasError', false);
              this.set('controller.optionsToSelect.useLocalRepo.uploadFile.hasError', false);
            }
          }
        }),

        /**
         * Wrapper for 'enter-url' elements
         *
         * @type {Em.View}
         */
        enterUrlView: Em.View.extend({

          /**
           * Url-field is disable when some stack is selected or upload file is selected
           *
           * @type {boolean}
           */
          enterUrlFieldDisabled: Em.computed.alias('controller.optionsToSelect.useLocalRepo.uploadFile.isSelected'),

          /**
           * Input for file upload
           *
           * @type {Em.TextField}
           */
          enterUrlField: Em.TextField.extend({
            classNameBindings: [':input-block-level', 'controller.optionsToSelect.useLocalRepo.uploadFile.isSelected:disabled']
          }),

          /**
           * Radio button for Use local Repo > Enter Url of VDF file
           *
           * @type {App.RadioButtonView}
           */
          enterUrlRadioButton: App.RadioButtonView.extend({
            labelTranslate: 'installer.step1.useLocalRepo.enterUrl',
            checked: Em.computed.alias('controller.optionsToSelect.useLocalRepo.enterUrl.isSelected')
          }),

          click: function () {
            if (!this.set('controller.optionsToSelect.useLocalRepo.enterUrl.isSelected')) {
              this.set('controller.optionsToSelect.useLocalRepo.enterUrl.isSelected', true);
              this.set('controller.optionsToSelect.useLocalRepo.uploadFile.isSelected', false);
              this.set('controller.optionsToSelect.useLocalRepo.enterUrl.hasError', false);
              this.set('controller.optionsToSelect.useLocalRepo.uploadFile.hasError', false);
            }
          }
        })

      })
    });
  }

});
