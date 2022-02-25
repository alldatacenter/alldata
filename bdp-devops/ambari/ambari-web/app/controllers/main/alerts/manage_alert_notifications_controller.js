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
var validator = require('utils/validator');

App.ManageAlertNotificationsController = Em.Controller.extend({

  name: 'manageAlertNotificationsController',

  /**
   * Are alert notifications loaded
   * @type {boolean}
   */
  isLoaded: false,

  /**
   * Create/Edit modal popup object
   * used to hide popup
   * @type {App.ModalPopup}
   */
  createEditPopup: null,

  /**
   * Map of edit inputs shown in Create/Edit Notification popup
   * @type {Object}
   */
  inputFields: Em.Object.create({
    name: {
      label: Em.I18n.t('common.name'),
      value: '',
      defaultValue: ''
    },
    groups: {
      label: Em.I18n.t('common.groups'),
      value: [],
      defaultValue: []
    },
    global: {
      label: Em.I18n.t('alerts.actions.manage_alert_notifications_popup.global'),
      value: false,
      defaultValue: false,
      disabled: false
    },
    allGroups: Em.Object.create({
      value: '',
      defaultValue: 'custom',
      disabled: false,
      isAll: Em.computed.equal('value', 'all')
    }),
    method: {
      label: Em.I18n.t('alerts.actions.manage_alert_notifications_popup.method'),
      value: '',
      defaultValue: ''
    },
    email: {
      label: Em.I18n.t('alerts.actions.manage_alert_notifications_popup.email'),
      value: '',
      defaultValue: ''
    },
    SMTPServer: {
      label: Em.I18n.t('alerts.actions.manage_alert_notifications_popup.SMTPServer'),
      value: '',
      defaultValue: ''
    },
    SMTPPort: {
      label: Em.I18n.t('alerts.actions.manage_alert_notifications_popup.SMTPPort'),
      value: '',
      defaultValue: ''
    },
    SMTPUseAuthentication: Em.Object.create({
      label: Em.I18n.t('alerts.actions.manage_alert_notifications_popup.SMTPUseAuthentication'),
      value: false,
      defaultValue: false,
      invertedValue: Em.computed.not('value')
    }),
    SMTPUsername: {
      label: Em.I18n.t('alerts.actions.manage_alert_notifications_popup.SMTPUsername'),
      value: '',
      defaultValue: ''
    },
    SMTPPassword: {
      label: Em.I18n.t('alerts.actions.manage_alert_notifications_popup.SMTPPassword'),
      value: '',
      defaultValue: ''
    },
    retypeSMTPPassword: {
      label: Em.I18n.t('alerts.actions.manage_alert_notifications_popup.retypeSMTPPassword'),
      value: '',
      defaultValue: ''
    },
    SMTPSTARTTLS: {
      label: Em.I18n.t('alerts.actions.manage_alert_notifications_popup.SMTPSTARTTLS'),
      value: false,
      defaultValue: false
    },
    emailFrom: {
      label: Em.I18n.t('alerts.actions.manage_alert_notifications_popup.emailFrom'),
      value: '',
      defaultValue: ''
    },
    version: {
      label: Em.I18n.t('alerts.actions.manage_alert_notifications_popup.version'),
      value: '',
      defaultValue: ''
    },
    OIDs: {
      label: Em.I18n.t('alerts.actions.manage_alert_notifications_popup.OIDs'),
      value: '',
      defaultValue: ''
    },
    community: {
      label: Em.I18n.t('alerts.actions.manage_alert_notifications_popup.community'),
      value: '',
      defaultValue: ''
    },
    host: {
      label: Em.I18n.t('alerts.actions.manage_alert_notifications_popup.host'),
      value: '',
      defaultValue: ''
    },
    port: {
      label: Em.I18n.t('alerts.actions.manage_alert_notifications_popup.port'),
      value: '',
      defaultValue: ''
    },
    severityFilter: {
      label: Em.I18n.t('alerts.actions.manage_alert_notifications_popup.severityFilter'),
      value: [],
      defaultValue: []
    },
    description: {
      label: Em.I18n.t('common.description'),
      value: '',
      defaultValue: ''
    },
    scriptDispatchProperty: {
      label: Em.I18n.t('alerts.actions.manage_alert_notifications_popup.scriptDispatchProperty'),
      value: '',
      defaultValue: ''
    },
    scriptFileName: {
      label: Em.I18n.t('alerts.actions.manage_alert_notifications_popup.scriptFileName'),
      value: '',
      defaultValue: ''
    },
    customProperties: Em.A([])
  }),

  /**
   * List of available options for Enable or Disable
   * used in settings of SelectedAlertNotification
   * @type {Object}
   */
  enableOrDisable: {
    enable: "enable",
    disable: "disable"
  },

  /**
   * List of available Notification types
   * used in Type combobox
   * @type {Array}
   */
  methods: ['EMAIL', 'SNMP', 'Custom SNMP', 'Alert Script'],

  /**
   * List of available value for Severity Filter
   * used in Severity Filter combobox
   * @type {Array}
   */
  severities: ['OK', 'WARNING', 'CRITICAL', 'UNKNOWN'],

  /**
   * List of available SNMP versions
   * @type {Array}
   */
  SNMPVersions: ['SNMPv1', 'SNMPv2c'],

  /**
   * List of all Alert Notifications
   * @type {App.AlertNotification[]}
   */
  alertNotifications: function () {
    return this.get('isLoaded') ? App.AlertNotification.find().toArray() : [];
  }.property('isLoaded'),

  /**
   * List of all Alert Groups
   * @type {App.AlertGroup[]}
   */
  allAlertGroups: function () {
    return this.get('isLoaded') ? App.AlertGroup.find().toArray() : [];
  }.property('isLoaded'),

  /**
   * Selected Alert Notification
   * @type {App.AlertNotification}
   */
  selectedAlertNotification: null,

  /**
   * Addable to <code>selectedAlertNotification.properties</code> custom property
   * @type {{name: string, value: string}}
   */
  newCustomProperty: {name: '', value: ''},

  /**
   * @type {string[]}
   */
  customPropertyNames: Em.computed.mapBy('inputFields.customProperties', 'name'),

  /**
   * Check if custom property name exists in the <code>inputFields.customProperties</code>
   *
   * @type {boolean}
   */
  isNewCustomPropertyExists: Em.computed.existsInByKey('newCustomProperty.name', 'customPropertyNames'),

  /**
   * Check if custom property name exists in the <code>ignoredCustomProperties</code>
   *
   * @type {boolean}
   */
  isNewCustomPropertyIgnored: Em.computed.existsInByKey('newCustomProperty.name', 'ignoredCustomProperties'),

  /**
   * Check if custom property name is valid according to the <code>validator.isValidConfigKey</code>
   *
   * @type {boolean}
   */
  isNewCustomPropertyNameValid: function () {
    return validator.isValidConfigKey(this.get('newCustomProperty.name'));
  }.property('newCustomProperty.name'),

  /**
   * Error message for the new custom property name
   *
   * @type {string}
   */
  errorMessageForNewCustomPropertyName: function () {
    var isNewCustomPropertyIgnored = this.get('isNewCustomPropertyIgnored');
    var isNewCustomPropertyExists = this.get('isNewCustomPropertyExists');
    var flag = this.get('isNewCustomPropertyNameValid');
    if (flag) {
      if (isNewCustomPropertyExists || isNewCustomPropertyIgnored) {
        return Em.I18n.t('alerts.notifications.addCustomPropertyPopup.error.propertyExists');
      }
    }
    else {
      return Em.I18n.t('alerts.notifications.addCustomPropertyPopup.error.invalidPropertyName');
    }
    return '';
  }.property('isNewCustomPropertyNameValid', 'isNewCustomPropertyExists', 'isNewCustomPropertyIgnored'),

  /**
   * If some error with new custom property
   *
   * @type {boolean}
   */
  isErrorWithNewCustomPropertyName: Em.computed.bool('errorMessageForNewCustomPropertyName'),

  /**
   * List custom property names that shouldn't be displayed on Edit page
   * @type {string[]}
   */
  ignoredCustomProperties: [
    'ambari.dispatch.credential.password',
    'ambari.dispatch.credential.username',
    'ambari.dispatch.recipients',
    'ambari.dispatch.snmp.community',
    'ambari.dispatch.snmp.oids.trap',
    'ambari.dispatch.snmp.oids.subject',
    'ambari.dispatch.snmp.oids.body',
    'ambari.dispatch.snmp.port',
    'ambari.dispatch.snmp.version',
    'mail.smtp.auth',
    'mail.smtp.from',
    'mail.smtp.host',
    'mail.smtp.port',
    'mail.smtp.starttls.enable',
    'ambari.dispatch-property.script',
    'ambari.dispatch-property.script.filename'
  ],

  validationMap: {
    EMAIL: [
      {
        errorKey: 'emailToError',
        validator: 'emailToValidation'
      },
      {
        errorKey: 'emailFromError',
        validator: 'emailFromValidation'
      },
      {
        errorKey: 'smtpPortError',
        validator: 'smtpPortValidation'
      },
      {
        errorKey: 'smtpUsernameError',
        validator: 'smtpUsernameValidation'
      },
      {
        errorKey: 'smtpPasswordError',
        validator: 'smtpPasswordValidation'
      },
      {
        errorKey: 'passwordError',
        validator: 'retypePasswordValidation'
      }
    ],
    SNMP: [
      {
        errorKey: 'portError',
        validator: 'portValidation'
      },
      {
        errorKey: 'hostError',
        validator: 'hostsValidation'
      }
    ],
    CustomSNMP: [
      {
        errorKey: 'portError',
        validator: 'portValidation'
      },
      {
        errorKey: 'hostError',
        validator: 'hostsValidation'
      }
    ],
    AlertScript:[
     {
       errorKey: 'scriptFileNameError',
       validator: 'scriptFileNameValidation',
     }
    ]
  },

  /**
   * Load all Alert Notifications from server
   * @method loadAlertNotifications
   */
  loadAlertNotifications: function () {
    this.set('isLoaded', false);
    return App.ajax.send({
      name: 'alerts.notifications',
      sender: this,
      success: 'getAlertNotificationsSuccessCallback',
      error: 'getAlertNotificationsErrorCallback'
    });
  },

  /**
   * Success-callback for load alert notifications request
   * @param {object} json
   * @method getAlertNotificationsSuccessCallback
   */
  getAlertNotificationsSuccessCallback: function (json) {
    App.alertNotificationMapper.map(json);
    this.set('isLoaded', true);
  },

  /**
   * Error-callback for load alert notifications request
   * @method getAlertNotificationsErrorCallback
   */
  getAlertNotificationsErrorCallback: function () {
    this.set('isLoaded', true);
  },

  /**
   * Add Notification button handler
   * @method addAlertNotification
   */
  addAlertNotification: function () {
    var inputFields = this.get('inputFields');
    inputFields.setProperties({
      'global.disabled': false
    });
    Em.keys(inputFields).forEach(function (key) {
      inputFields.set(key + '.value', inputFields.get(key + '.defaultValue'));
    });
    inputFields.set('severityFilter.value', ['OK', 'WARNING', 'CRITICAL', 'UNKNOWN']);
    inputFields.set('customProperties',Em.A([]));
    this.showCreateEditPopup(false);
  },

  /**
   * Edit Notification button handler
   * @method editAlertNotification
   */
  editAlertNotification: function () {
    this.fillEditCreateInputs();
    this.showCreateEditPopup(true);
  },

  /**
   * Fill inputs of Create/Edit popup form
   * @param addCopyToName define whether add 'Copy of ' to name
   * @method fillEditCreateInputs
   */
  fillEditCreateInputs: function (addCopyToName) {
    var inputFields = this.get('inputFields');
    var selectedAlertNotification = this.get('selectedAlertNotification');
    var methodValue = this.getNotificationTypeText(selectedAlertNotification.get('type'));
      var properties = selectedAlertNotification.get('properties');
    inputFields.set('name.value', (addCopyToName ? 'Copy of ' : '') + selectedAlertNotification.get('name'));
    inputFields.set('groups.value', selectedAlertNotification.get('groups').toArray());
    inputFields.set('email.value', properties['ambari.dispatch.recipients'] ?
      properties['ambari.dispatch.recipients'].join(', ') : '');
    inputFields.set('SMTPServer.value', properties['mail.smtp.host']);
    inputFields.set('SMTPPort.value', properties['mail.smtp.port']);
    inputFields.set('SMTPUseAuthentication.value', properties['mail.smtp.auth'] !== "false");
    inputFields.set('SMTPUsername.value', properties['ambari.dispatch.credential.username']);
    inputFields.set('SMTPPassword.value', properties['ambari.dispatch.credential.password']);
    inputFields.set('retypeSMTPPassword.value', properties['ambari.dispatch.credential.password']);
    inputFields.set('SMTPSTARTTLS.value', properties['mail.smtp.starttls.enable'] !== "false");
    inputFields.set('emailFrom.value', properties['mail.smtp.from']);
    inputFields.set('version.value', properties['ambari.dispatch.snmp.version']);
    inputFields.set('OIDs.value', properties['ambari.dispatch.snmp.oids.trap']);
    inputFields.set('community.value', properties['ambari.dispatch.snmp.community']);
    inputFields.set('host.value', properties['ambari.dispatch.recipients'] ?
      properties['ambari.dispatch.recipients'].join(', ') : '');
    inputFields.set('port.value', properties['ambari.dispatch.snmp.port']);
    inputFields.set('severityFilter.value', selectedAlertNotification.get('alertStates'));
    inputFields.set('global.value', selectedAlertNotification.get('global'));
    inputFields.set('allGroups.value', selectedAlertNotification.get('global') ? 'all' : 'custom');
    inputFields.set('scriptDispatchProperty.value', properties['ambari.dispatch-property.script'] || '');
    inputFields.set('scriptFileName.value', properties['ambari.dispatch-property.script.filename'] || '');
    // not allow to edit global field
    inputFields.set('global.disabled', true);
    inputFields.set('description.value', selectedAlertNotification.get('description'));
    inputFields.set('method.value', methodValue);
    inputFields.get('customProperties').clear();
    var ignoredCustomProperties = this.get('ignoredCustomProperties');
    Em.keys(properties).forEach(function (k) {
      if (ignoredCustomProperties.contains(k)) return;
      inputFields.get('customProperties').pushObject({
        name: k,
        value: properties[k],
        defaultValue: properties[k]
      });
    });
  },

  /**
   * Show Edit or Create Notification popup
   * @param {boolean} isEdit true - edit, false - create
   * @returns {App.ModalPopup}
   * @method showCreateEditPopup
   */
  showCreateEditPopup: function (isEdit) {
    var self = this;
    var createEditPopup = App.ModalPopup.show({
      header: isEdit ? Em.I18n.t('alerts.actions.manage_alert_notifications_popup.editHeader') : Em.I18n.t('alerts.actions.manage_alert_notifications_popup.addHeader'),
      classNames: ['create-edit-alert-notification-popup'],
      marginBottom: 130,
      bodyClass: Em.View.extend({
        controller: this,
        templateName: require('templates/main/alerts/create_alert_notification'),

        didInsertElement: function () {
          App.tooltip($('.checkbox-tooltip'));
          this.nameValidation();
          this.emailToValidation();
          this.emailFromValidation();
          this.smtpPortValidation();
          this.hostsValidation();
          this.portValidation();
          this.smtpUsernameValidation();
          this.smtpPasswordValidation();
          this.retypePasswordValidation();
          this.scriptFileNameValidation();
        },

        isEmailMethodSelected: Em.computed.equal('controller.inputFields.method.value', 'EMAIL'),

        isSNMPMethodSelected: Em.computed.equal('controller.inputFields.method.value', 'SNMP'),

        isCustomSNMPMethodSelected: Em.computed.equal('controller.inputFields.method.value', 'Custom SNMP'),

        isAlertScriptMethodSelected: Em.computed.equal('controller.inputFields.method.value', 'Alert Script'),

        methodObserver: function () {
          var currentMethod = this.get('controller.inputFields.method.value'),
            validationMap = self.get('validationMap');
          self.get('methods').forEach(function (method) {
            // Replace blank spaces with empty character
            var mapKey = method.replace(/\s/g, "");
            var validations = validationMap[mapKey];
            if (method === currentMethod) {
              validations.mapProperty('validator').forEach(function (key) {
                this.get(key).call(this);
              }, this);
            } else {
              validations.mapProperty('errorKey').forEach(function (key) {
                this.set(key, false);
              }, this);
            }
          }, this);
        }.observes('controller.inputFields.method.value'),

        nameValidation: function () {
          var newName = this.get('controller.inputFields.name.value').trim();
          var errorMessage = '';
          // on editing, save current notification name
          if (newName && !this.get('currentName')) {
            this.set('currentName', newName);
          }
          var nameExistsCondition = isEdit ? newName && newName !== this.get('currentName') : !!newName;
          if (!newName) {
            this.set('nameError', true);
            errorMessage = Em.I18n.t('alerts.actions.manage_alert_notifications_popup.error.name.empty');
          } else if (nameExistsCondition && self.get('alertNotifications').mapProperty('name').contains(newName)) {
            this.set('nameError', true);
            errorMessage = Em.I18n.t('alerts.actions.manage_alert_notifications_popup.error.name.existed');
          } else if (newName && !validator.isValidAlertNotificationName(newName)){
            this.set('nameError', true);
            errorMessage = Em.I18n.t('form.validator.alertNotificationName');
          } else {
            this.set('nameError', false);
          }
          this.set('controller.inputFields.name.errorMsg', errorMessage);
        }.observes('controller.inputFields.name.value'),

        emailToValidation: function () {
          var emailToError = false;
          if (this.get('isEmailMethodSelected')) {
            var inputValues = this.get('controller.inputFields.email.value').trim().split(',');
            emailToError = inputValues.some(function(emailTo) {
              return emailTo && !validator.isValidEmail(emailTo.trim());
            })
          }
          this.set('emailToError', emailToError);
          this.set('controller.inputFields.email.errorMsg', emailToError ? Em.I18n.t('alerts.notifications.error.email') : null);
        }.observes('controller.inputFields.email.value'),

        emailFromValidation: function () {
          var emailFrom = this.get('controller.inputFields.emailFrom.value');
          if (emailFrom && !validator.isValidEmail(emailFrom)) {
            this.set('emailFromError', true);
            this.set('controller.inputFields.emailFrom.errorMsg', Em.I18n.t('alerts.notifications.error.email'));
          } else {
            this.set('emailFromError', false);
            this.set('controller.inputFields.emailFrom.errorMsg', null);
          }
        }.observes('controller.inputFields.emailFrom.value'),

        smtpPortValidation: function () {
          var value = this.get('controller.inputFields.SMTPPort.value');
          if (value && (!validator.isValidInt(value) || value < 0)) {
            this.set('smtpPortError', true);
            this.set('controller.inputFields.SMTPPort.errorMsg', Em.I18n.t('alerts.notifications.error.integer'));
          } else {
            this.set('smtpPortError', false);
            this.set('controller.inputFields.SMTPPort.errorMsg', null);
          }
        }.observes('controller.inputFields.SMTPPort.value'),

        hostsValidation: function() {
          var inputValue = this.get('controller.inputFields.host.value').trim(),
            hostError = false;
          if (!this.get('isEmailMethodSelected') && !this.get('isAlertScriptMethodSelected')) {
            var array = inputValue.split(',');
            hostError = array.some(function(hostname) {
              return hostname && !validator.isHostname(hostname.trim());
            });
            hostError = hostError || inputValue==='';
          }
          this.set('hostError', hostError);
          this.set('controller.inputFields.host.errorMsg', hostError ? Em.I18n.t('alerts.notifications.error.host') : null);
        }.observes('controller.inputFields.host.value'),

        portValidation: function () {
          var value = this.get('controller.inputFields.port.value');
          if (value && (!validator.isValidInt(value) || value < 0)) {
            this.set('portError', true);
            this.set('controller.inputFields.port.errorMsg', Em.I18n.t('alerts.notifications.error.integer'));
          } else {
            this.set('portError', false);
            this.set('controller.inputFields.port.errorMsg', null);
          }
        }.observes('controller.inputFields.port.value'),

        smtpUsernameValidation: function () {
          var smtpUsernameError = false;
          var errorMessage = null;
          if(this.get('isEmailMethodSelected')) {
            if (this.get('controller.inputFields.SMTPUseAuthentication.value')) {
              if (Em.isBlank(this.get('controller.inputFields.SMTPUsername.value'))) {
                smtpUsernameError = true;
                errorMessage = Em.I18n.t('alerts.notifications.error.SMTPUsername');
              }
            }
          }
          this.set('smtpUsernameError', smtpUsernameError);
          this.set('controller.inputFields.SMTPUsername.errorMsg', errorMessage);
        }.observes('controller.inputFields.SMTPUsername.value', 'controller.inputFields.SMTPUseAuthentication.value'),

        smtpPasswordValidation: function () {
          var smtpPasswordError = false;
          var errorMessage = null;
          if(this.get('isEmailMethodSelected')) {
            if (this.get('controller.inputFields.SMTPUseAuthentication.value')) {
              if (Em.isBlank(this.get('controller.inputFields.SMTPPassword.value'))) {
                smtpPasswordError = true;
                errorMessage = Em.I18n.t('alerts.notifications.error.SMTPPassword');
              }
            }
          }
          this.set('smtpPasswordError', smtpPasswordError);
          this.set('controller.inputFields.SMTPPassword.errorMsg', errorMessage);
        }.observes('controller.inputFields.SMTPPassword.value','controller.inputFields.SMTPUseAuthentication.value'),

        retypePasswordValidation: function () {
          var passwordValue = this.get('controller.inputFields.SMTPPassword.value');
          var retypePasswordValue = this.get('controller.inputFields.retypeSMTPPassword.value');
          if (passwordValue !== retypePasswordValue) {
            this.set('passwordError', true);
            this.set('controller.inputFields.retypeSMTPPassword.errorMsg', Em.I18n.t('alerts.notifications.error.retypePassword'));
          } else {
            this.set('passwordError', false);
            this.set('controller.inputFields.retypeSMTPPassword.errorMsg', null);
          }
        }.observes('controller.inputFields.retypeSMTPPassword.value', 'controller.inputFields.SMTPPassword.value'),


        scriptFileNameValidation:function(){
          var scriptFileNameValue = this.get('controller.inputFields.scriptFileName.value').trim();
          if(!Em.isBlank(scriptFileNameValue) && !validator.isValidFileName(scriptFileNameValue)){
             this.set('scriptFileNameError',true);
             this.set('controller.inputFields.scriptFileName.errorMsg',Em.I18n.t('alerts.actions.manage_alert_notifications_popup.error.scriptFileName.invalid'));
          }else{
             this.set('scriptFileNameError',false);
             this.set('controller.inputFields.scriptFileName.errorMsg',null);
          }
        }.observes('controller.inputFields.scriptFileName.value'),

        someErrorExists: Em.computed.or('nameError', 'emailToError', 'emailFromError', 'smtpPortError', 'hostError', 'portError', 'smtpUsernameError', 'smtpPasswordError', 'passwordError','scriptFileNameError'),

        setParentErrors: function () {
          this.set('parentView.hasErrors', this.get('someErrorExists'));
        }.observes('someErrorExists'),

        groupsSelectView: Em.Select.extend({
          attributeBindings: ['disabled'],
          init: function () {
            this._super();
            this.set('parentView.groupSelect', this);
          }
        }),

        groupSelect: null,

        /**
         * Select all alert-groups if <code>allGroups.value</code> is 'custom'
         * @method selectAllGroups
         */
        selectAllGroups: function () {
          if (this.get('controller.inputFields.allGroups.value') === 'custom') {
            this.set('groupSelect.selection', this.get('groupSelect.content').slice());
          }
        },

        /**
         * Deselect all alert-groups if <code>allGroups.value</code> is 'custom'
         * @method clearAllGroups
         */
        clearAllGroups: function () {
          if (this.get('controller.inputFields.allGroups.value') === 'custom') {
            this.set('groupSelect.selection', []);
          }
        },

        severitySelectView: Em.Select.extend({
          init: function () {
            this._super();
            this.set('parentView.severitySelect', this);
          }
        }),

        severitySelect: null,

        /**
         * Determines if all alert-groups are selected
         * @type {boolean}
         */
        allGroupsSelected: Em.computed.equalProperties('groupSelect.selection.length', 'groupSelect.content.length'),

        /**
         * Determines if no one alert-group is selected
         * @type {boolean}
         */
        noneGroupsSelected: Em.computed.empty('groupSelect.selection'),

        /**
         * Determines if all severities are selected
         * @type {boolean}
         */
        allSeveritySelected: Em.computed.equalProperties('severitySelect.selection.length', 'severitySelect.content.length'),

        /**
         * Determines if no one severity is selected
         * @type {boolean}
         */
        noneSeveritySelected: Em.computed.empty('severitySelect.selection'),

        /**
         * Select all severities
         * @method selectAllSeverity
         */
        selectAllSeverity: function () {
          this.set('severitySelect.selection', this.get('severitySelect.content').slice());
        },

        /**
         * Deselect all severities
         * @method clearAllSeverity
         */
        clearAllSeverity: function () {
          this.set('severitySelect.selection', []);
        }
      }),

      isSaving: false,

      hasErrors: false,

      primary: Em.I18n.t('common.save'),

      disablePrimary: Em.computed.or('isSaving', 'hasErrors'),

      onPrimary: function () {
        this.set('isSaving', true);
        var apiObject = self.formatNotificationAPIObject();
        if (isEdit) {
          self.updateAlertNotification(apiObject);
        } else {
          self.createAlertNotification(apiObject);
        }
      },
      hide: function () {
        self.set('createEditPopup', null);
        return this._super(...arguments);
      }
    });
    this.set('createEditPopup', createEditPopup);
    return createEditPopup;
  },

  /**
   * Create API-formatted object from data populate by user
   * @returns {Object}
   * @method formatNotificationAPIObject
   */
  formatNotificationAPIObject: function () {
    var inputFields = this.get('inputFields');
    var properties = {};
    if (inputFields.get('method.value') === 'EMAIL') {
      properties['ambari.dispatch.recipients'] = inputFields.get('email.value').replace(/\s/g, '').split(',');
      properties['mail.smtp.host'] = inputFields.get('SMTPServer.value');
      properties['mail.smtp.port'] = inputFields.get('SMTPPort.value');
      properties['mail.smtp.from'] = inputFields.get('emailFrom.value');
      properties['mail.smtp.auth'] = inputFields.get('SMTPUseAuthentication.value');
      if (inputFields.get('SMTPUseAuthentication.value')) {
        properties['ambari.dispatch.credential.username'] = inputFields.get('SMTPUsername.value');
        properties['ambari.dispatch.credential.password'] = inputFields.get('SMTPPassword.value');
        properties['mail.smtp.starttls.enable'] = inputFields.get('SMTPSTARTTLS.value');
      }
    } else if(inputFields.get('method.value') === 'SNMP') {
      properties['ambari.dispatch.snmp.version'] = inputFields.get('version.value');
      properties['ambari.dispatch.snmp.community'] = inputFields.get('community.value');
      properties['ambari.dispatch.recipients'] = inputFields.get('host.value').replace(/\s/g, '').split(',');
      properties['ambari.dispatch.snmp.port'] = inputFields.get('port.value');
    } else if(inputFields.get('method.value') === 'Custom SNMP') {
      properties['ambari.dispatch.snmp.version'] = inputFields.get('version.value');
      properties['ambari.dispatch.snmp.oids.trap'] = inputFields.get('OIDs.value');
      properties['ambari.dispatch.snmp.oids.subject'] = inputFields.get('OIDs.value');
      properties['ambari.dispatch.snmp.oids.body'] = inputFields.get('OIDs.value');
      properties['ambari.dispatch.snmp.community'] = inputFields.get('community.value');
      properties['ambari.dispatch.recipients'] = inputFields.get('host.value').replace(/\s/g, '').split(',');
      properties['ambari.dispatch.snmp.port'] = inputFields.get('port.value');
    }else if (inputFields.get('method.value') === 'Alert Script') {
      var scriptDispatchProperty = inputFields.get('scriptDispatchProperty.value').trim();
      if( scriptDispatchProperty != '')
          properties['ambari.dispatch-property.script'] = scriptDispatchProperty;

      var scriptFileName = inputFields.get('scriptFileName.value').trim();
      if( scriptFileName != '')
          properties['ambari.dispatch-property.script.filename'] = scriptFileName;
    }
    inputFields.get('customProperties').forEach(function (customProperty) {
      properties[customProperty.name] = customProperty.value;
    });
    var apiObject = {
      AlertTarget: {
        name: inputFields.get('name.value'),
        description: inputFields.get('description.value'),
        global: inputFields.get('allGroups.value') === 'all',
        notification_type: this.getNotificationType(inputFields.get('method.value')),
        alert_states: inputFields.get('severityFilter.value'),
        properties: properties
      }
    };
    if (inputFields.get('allGroups.value') === 'custom') {
      apiObject.AlertTarget.groups = inputFields.get('groups.value').mapProperty('id');
    }
    return apiObject;
  },

  getNotificationType: function(text) {
    var notificationType = text;
    if(notificationType === "Custom SNMP") {
      notificationType = "SNMP";
    } else if(notificationType === "SNMP") {
      notificationType = "AMBARI_SNMP";
    } else if(notificationType === "Alert Script"){
      notificationType = "ALERT_SCRIPT";
    }
    return notificationType;
  },

  getNotificationTypeText: function(notificationType) {
    var notificationTypeText = notificationType;
    if(notificationType === "SNMP") {
      notificationTypeText = "Custom SNMP";
    } else if(notificationType === "AMBARI_SNMP") {
      notificationTypeText = "SNMP";
    } else if(notificationType === "ALERT_SCRIPT"){
      notificationTypeText = "Alert Script";
    }
    return notificationTypeText;
  },

  /**
   * Send request to server to create Alert Notification
   * @param {object} apiObject (@see formatNotificationAPIObject)
   * @returns {$.ajax}
   * @method createAlertNotification
   */
  createAlertNotification: function (apiObject) {
    return App.ajax.send({
      name: 'alerts.create_alert_notification',
      sender: this,
      data: {
        data: apiObject
      },
      success: 'createAlertNotificationSuccessCallback',
      error: 'saveErrorCallback'
    });
  },

  /**
   * Success callback for <code>createAlertNotification</code>
   * @method createAlertNotificationSuccessCallback
   */
  createAlertNotificationSuccessCallback: function () {
    this.loadAlertNotifications();
    var createEditPopup = this.get('createEditPopup');
    if (createEditPopup) {
      createEditPopup.hide();
    }
  },

  /**
   * Send request to server to update Alert Notification
   * @param {object} apiObject (@see formatNotificationAPIObject)
   * @returns {$.ajax}
   * @method updateAlertNotification
   */
  updateAlertNotification: function (apiObject) {
    if(apiObject!=null){
      if(apiObject.AlertTarget.global == false){
        this.get('selectedAlertNotification').set('global',false);
      } else {
        this.get('selectedAlertNotification').set('global',true);
      }
    }
    return App.ajax.send({
      name: 'alerts.update_alert_notification',
      sender: this,
      data: {
        data: apiObject,
        id: this.get('selectedAlertNotification.id')
      },
      success: 'updateAlertNotificationSuccessCallback',
      error: 'saveErrorCallback'
    });
  },

  /**
   * Success callback for <code>updateAlertNotification</code>
   * @method updateAlertNotificationSuccessCallback
   */
  updateAlertNotificationSuccessCallback: function () {
    this.loadAlertNotifications();
    var createEditPopup = this.get('createEditPopup');
    if (createEditPopup) {
      createEditPopup.hide();
    }
  },

  /**
   * Error callback for <code>createAlertNotification</code> and <code>updateAlertNotification</code>
   * @method saveErrorCallback
   */
  saveErrorCallback: function () {
    this.set('createEditPopup.isSaving', false);
  },

  /**
   * Delete Notification button handler
   * @return {App.ModalPopup}
   * @method deleteAlertNotification
   */
  deleteAlertNotification: function () {
    var self = this;
    return App.showConfirmationPopup(function () {
        App.ajax.send({
          name: 'alerts.delete_alert_notification',
          sender: self,
          data: {
            id: self.get('selectedAlertNotification.id')
          },
          success: 'deleteAlertNotificationSuccessCallback'
        });
      }, Em.I18n.t('alerts.actions.manage_alert_notifications_popup.confirmDeleteBody').format(this.get('selectedAlertNotification.name')),
      null, Em.I18n.t('alerts.actions.manage_alert_notifications_popup.confirmDeleteHeader'), Em.I18n.t('common.delete'));
  },

  /**
   * Success callback for <code>deleteAlertNotification</code>
   * @method deleteAlertNotificationSuccessCallback
   */
  deleteAlertNotificationSuccessCallback: function () {
    this.loadAlertNotifications();
    var selectedAlertNotification = this.get('selectedAlertNotification');
    selectedAlertNotification.deleteRecord();
    this.set('selectedAlertNotification', null);
  },

  /**
   * Duplicate Notification button handler
   * @method duplicateAlertNotification
   */
  duplicateAlertNotification: function () {
    this.fillEditCreateInputs(true);
    this.showCreateEditPopup();
  },

  /**
   * Enable or Disable Notification button handler
   * @method enableOrDisableAlertNotification
   */
  enableOrDisableAlertNotification: function (e) {
    return App.ajax.send({
      name: 'alerts.update_alert_notification',
      sender: this,
      data: {
        data: {
          AlertTarget: {
            enabled: e.context !== "disable"
          }
        },
        id: this.get('selectedAlertNotification.id')
      },
      success: 'enableOrDisableAlertNotificationSuccessCallback',
      error: 'saveErrorCallback'
    });
  },

  /**
   * Success callback for <code>enableOrDisableAlertNotification</code>
   * @method enableOrDisableAlertNotificationSuccessCallback
   */
  enableOrDisableAlertNotificationSuccessCallback: function () {
    this.loadAlertNotifications();
    var createEditPopup = this.get('createEditPopup');
    if (createEditPopup) {
      createEditPopup.hide();
    }
  },

  /**
   * Show popup with form for new custom property
   * @method addCustomPropertyHandler
   * @return {App.ModalPopup}
   */
  addCustomPropertyHandler: function () {
    var self = this;

    return App.ModalPopup.show({

      controllerBinding: 'App.router.manageAlertNotificationsController',

      header: Em.I18n.t('alerts.notifications.addCustomPropertyPopup.header'),

      primary: Em.I18n.t('common.add'),

      bodyClass: Em.View.extend({
        templateName: require('templates/main/alerts/add_custom_config_to_alert_notification_popup')
      }),

      disablePrimary: Em.computed.alias('controller.isErrorWithNewCustomPropertyName'),

      onPrimary: function () {
        self.addCustomProperty();
        self.set('newCustomProperty', {name: '', value: ''}); // cleanup
        this.hide();
      }

    });
  },

  /**
   * Add Custom Property to <code>selectedAlertNotification</code> (push it to <code>properties</code>-field)
   * @method addCustomProperty
   */
  addCustomProperty: function () {
    var newCustomProperty = this.get('newCustomProperty');
    this.get('inputFields.customProperties').pushObject({
      name: newCustomProperty.name,
      value: newCustomProperty.value,
      defaultValue: newCustomProperty.value
    });
  },

  /**
   * "Remove"-button click handler
   * Delete customProperty from <code>inputFields.customProperties</code>
   * @param {object} e
   * @method removeCustomProperty
   */
  removeCustomPropertyHandler: function (e) {
    var customProperties = this.get('inputFields.customProperties');
    this.set('inputFields.customProperties', customProperties.without(e.context));
  }

});
