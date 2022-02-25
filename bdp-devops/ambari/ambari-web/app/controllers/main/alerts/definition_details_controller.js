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

var validator = require('utils/validator');

App.MainAlertDefinitionDetailsController = Em.Controller.extend({

  name: 'mainAlertDefinitionDetailsController',

  alerts: function () {
    return App.AlertInstanceLocal.find().toArray()
        .filterProperty('definitionId', this.get('content.id'));
  }.property('App.router.mainAlertInstancesController.isLoaded', 'App.router.mainAlertInstancesController.reload'),

  // stores object with editing form data (label)
  editing: Em.Object.create({
    label: Em.Object.create({
      name: 'label',
      isEditing: false,
      value: '',
      originalValue: '',
      isError: false,
      bindingValue: 'content.label'
    })
  }),

  /**
   * Host to count of alerts on this host during last day map
   * @type {Object}
   */
  lastDayAlertsCount: null,

  /**
   * List of all group names related to alert definition
   * @type {Array}
   */
  groupsList: Em.computed.mapBy('content.groups', 'displayName'),

  /**
   * Validation function to define if label field populated correctly
   * @method labelValidation
   */
  labelValidation: function () {
    this.set('editing.label.isError', !this.get('editing.label.value').trim());
  }.observes('editing.label.value'),

  /**
   * Set init values for variables
   */
  clearStep: function () {
    var editing = this.get('editing');
    Em.keys(editing).forEach(function (key) {
      editing.get(key).set('isEditing', false);
    });
  },

  /**
   * Load alert instances for current alertDefinition
   * Start updating loaded data
   * @method loadAlertInstances
   */
  loadAlertInstances: function () {
    App.router.get('mainAlertInstancesController').loadAlertInstancesByAlertDefinition(this.get('content.id'));
    App.router.set('mainAlertInstancesController.isUpdating', true);
    this.loadAlertInstancesHistory();
  },

  /**
   * Load alert instances history data
   * used to count instances number of the last 24 hour
   * @method loadAlertInstancesHistory
   */
  loadAlertInstancesHistory: function () {
    this.set('lastDayAlertsCount', null);
    return App.ajax.send({
      name: 'alerts.get_instances_history',
      sender: this,
      data: {
        definitionName: this.get('content.name'),
        timestamp: App.dateTime() - 86400000 // timestamp for time 24-hours ago
      },
      success: 'loadAlertInstancesHistorySuccess'
    });
  },

  /**
   * Success-callback for <code>loadAlertInstancesHistory</code>
   */
  loadAlertInstancesHistorySuccess: function (data) {
    var lastDayAlertsCount = {};
    data.items.forEach(function (alert) {
      if (!lastDayAlertsCount[alert.AlertHistory.host_name]) {
        lastDayAlertsCount[alert.AlertHistory.host_name] = 0;
      }
      lastDayAlertsCount[alert.AlertHistory.host_name]++;
    });
    this.set('lastDayAlertsCount', lastDayAlertsCount);
  },

  /**
   * Edit button handler
   * @param {object} event
   * @method edit
   */
  edit: function (event) {
    var element = event.context;
    var value = this.get(element.get('bindingValue'));
    element.set('originalValue', value);
    element.set('value', value);
    element.set('isEditing', true);
  },

  /**
   * Cancel button handler
   * @param {object} event
   * @method cancelEdit
   */
  cancelEdit: function (event) {
    var element = event.context;
    element.set('value', element.get('originalValue'));
    element.set('isEditing', false);
  },

  /**
   * Save button handler, could save label of alert definition
   * @param {object} event
   * @returns {$.ajax}
   * @method saveEdit
   */
  saveEdit: function (event) {
    var element = event.context;
    this.set(element.get('bindingValue'), element.get('value'));
    element.set('isEditing', false);

    var data = Em.Object.create({});
    var propertyName = "AlertDefinition/" + element.get('name');
    data.set(propertyName, element.get('value'));
    var alertDefinitionId = this.get('content.id');
    return App.ajax.send({
      name: 'alerts.update_alert_definition',
      sender: this,
      data: {
        id: alertDefinitionId,
        data: data
      }
    });
  },

  /**
   * Onclick handler for save button on Save popup
   * Save changes of label and configs
   */
  saveLabelAndConfigs: function () {
    var configsController = App.router.get('mainAlertDefinitionConfigsController');
    if (configsController.get('canEdit')) {
      configsController.saveConfigs();
    }
    if (this.get('editing.label.isEditing')) {
      this.saveEdit({
        context: this.get('editing.label')
      });
    }
  },

  /**
   * "Delete" button handler
   * @method deleteAlertDefinition
   */
  deleteAlertDefinition: function () {
    var alertDefinition = this.get('content');
    var self = this;
    App.showConfirmationPopup(function () {
      App.ajax.send({
        name: 'alerts.delete_alert_definition',
        sender: self,
        success: 'deleteAlertDefinitionSuccess',
        error: 'deleteAlertDefinitionError',
        data: {
          id: alertDefinition.get('id')
        }
      });
    }, null, function () {
    });
  },

  /**
   * Success-callback for <code>deleteAlertDefinition</code>
   * @method deleteAlertDefinitionSuccess
   */
  deleteAlertDefinitionSuccess: function () {
    App.router.transitionTo('main.alerts.index');
  },

  /**
   * Error-callback for <code>deleteAlertDefinition</code>
   * @method deleteAlertDefinitionError
   */
  deleteAlertDefinitionError: function (xhr, textStatus, errorThrown, opt) {
    xhr.responseText = "{\"message\": \"" + xhr.statusText + "\"}";
    App.ajax.defaultErrorHandler(xhr, opt.url, 'DELETE', xhr.status);
  },

  /**
   * "Disable / Enable" button handler
   * @method toggleState
   */
  toggleState: function () {
    var alertDefinition = this.get('content');
    var self = this;
    var bodyMessage = Em.Object.create({
      confirmMsg: alertDefinition.get('enabled') ? Em.I18n.t('alerts.table.state.enabled.confirm.msg') : Em.I18n.t('alerts.table.state.disabled.confirm.msg'),
      confirmButton: alertDefinition.get('enabled') ? Em.I18n.t('alerts.table.state.enabled.confirm.btn') : Em.I18n.t('alerts.table.state.disabled.confirm.btn')
    });

    return App.showConfirmationFeedBackPopup(function () {
      self.toggleDefinitionState(alertDefinition);
    }, bodyMessage);
  },

  /**
   * Enable/disable alertDefinition
   * @param {object} alertDefinition
   * @param {string} property
   * @returns {$.ajax}
   * @method toggleDefinitionState
   */
  toggleDefinitionState: function (alertDefinition) {
    var newState = !alertDefinition.get('enabled');
    alertDefinition.set('enabled', newState);
    return App.ajax.send({
      name: 'alerts.update_alert_definition',
      sender: this,
      data: {
        id: alertDefinition.get('id'),
        data: {
          "AlertDefinition/enabled": newState
        }
      }
    });
  },

  globalAlertsRepeatTolerance: function () {
    return App.router.get('clusterController.clusterEnv.properties.alerts_repeat_tolerance') || "1";
  }.property('App.router.clusterController.clusterEnv'),

  enableRepeatTolerance: function (enable) {
    var alertDefinition = this.get('content');
    alertDefinition.set('repeat_tolerance_enabled', enable);
    return App.ajax.send({
      name: 'alerts.update_alert_definition',
      sender: this,
      data: {
        id: alertDefinition.get('id'),
        data: {
          "AlertDefinition/repeat_tolerance_enabled": enable
        }
      }
    });
  },

  editRepeatTolerance: function () {
    var self = this;
    var alertDefinition = this.get('content');

    var alertsRepeatTolerance = App.router.get('clusterController.clusterEnv.properties.alerts_repeat_tolerance') || "1";

    return App.ModalPopup.show({
      classNames: ['forty-percent-width-modal'],
      header: Em.I18n.t('alerts.actions.editRepeatTolerance.header'),
      primary: Em.I18n.t('common.save'),
      secondary: Em.I18n.t('common.cancel'),
      inputValue: self.get('content.repeat_tolerance_enabled') ? self.get('content.repeat_tolerance') || 1 : alertsRepeatTolerance,
      errorMessage: Em.I18n.t('alerts.actions.editRepeatTolerance.error'),
      isInvalid: function () {
        var intValue = Number(this.get('inputValue'));
        return this.get('inputValue') !== 'DEBUG' && (!validator.isValidInt(intValue) || intValue < 1 || intValue > 99);
      }.property('inputValue'),
      isChanged: function () {
        return Number(this.get('inputValue')) !== alertsRepeatTolerance;
      }.property('inputValue'),
      doRestoreDefaultValue: function () {
        this.set('inputValue', alertsRepeatTolerance);
        this.$('[data-toggle=tooltip]').tooltip('destroy');
      },
      disablePrimary: Em.computed.alias('isInvalid'),
      onPrimary: function () {
        if (this.get('isInvalid')) {
          return;
        }
        var input = this.get('inputValue');
        self.set('content.repeat_tolerance', input);
        self.enableRepeatTolerance(input !== alertsRepeatTolerance);
        App.ajax.send({
          name: 'alerts.update_alert_definition',
          sender: self,
          data: {
            id: alertDefinition.get('id'),
            data: {
              "AlertDefinition/repeat_tolerance": input
            }
          }
        });
        this.hide();
      },
      didInsertElement: function () {
        this._super();
        App.tooltip(this.$('[data-toggle=tooltip]'));
      },
      bodyClass: Ember.View.extend({
        templateName: require('templates/common/modal_popups/prompt_popup'),
        title: Em.I18n.t('alerts.actions.editRepeatTolerance.title'),
        description: Em.I18n.t('alerts.actions.editRepeatTolerance.description'),
        label: Em.I18n.t('alerts.actions.editRepeatTolerance.label')
      })
    });
  },

  /**
   * Define if label or configs are in edit mode
   * @type {Boolean}
   */
  isEditing: Em.computed.or('editing.label.isEditing', 'App.router.mainAlertDefinitionConfigsController.canEdit'),

  /**
   * If some configs or label are changed and user navigates away, show this popup with propose to save changes
   * @param {String} path
   * @method showSavePopup
   */
  showSavePopup: function (callback) {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('common.warning'),
      bodyClass: Em.View.extend({
        template: Ember.Handlebars.compile('{{t alerts.saveChanges}}')
      }),
      primary: Em.I18n.t('common.save'),
      secondary: Em.I18n.t('common.discard'),
      third: Em.I18n.t('common.cancel'),
      disablePrimary: Em.computed.or('App.router.mainAlertDefinitionDetailsController.editing.label.isError', 'App.router.mainAlertDefinitionConfigsController.hasErrors'),
      onPrimary: function () {
        self.saveLabelAndConfigs();
        callback();
        this.hide();
      },
      onSecondary: function () {
        callback();
        this.hide();
      },
      onThird: function () {
        this.hide();
      }
    });
  }

});