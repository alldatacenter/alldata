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

App.MainAlertsManageAlertGroupView = Em.View.extend({

  templateName: require('templates/main/alerts/manage_alert_groups_popup'),

  /**
   * @type {object}
   */
  selectedAlertGroup: null,

  /**
   * Observer to prevent user select more than 1 alert group
   * @method onGroupSelect
   */
  onGroupSelect: function () {
    var selectedAlertGroup = this.get('selectedAlertGroup');
    // to unable user select more than one alert group at a time
    if (selectedAlertGroup && selectedAlertGroup.length) {
      this.set('controller.selectedAlertGroup', selectedAlertGroup[selectedAlertGroup.length - 1]);
    }
    if (selectedAlertGroup && selectedAlertGroup.length > 1) {
      this.set('selectedAlertGroup', selectedAlertGroup[selectedAlertGroup.length - 1]);
    }
    this.set('controller.selectedDefinitions', []);
  }.observes('selectedAlertGroup'),

  /**
   * Observer to provide two-way binding for group selection
   */
  setGroupInController: function () {
    if (!this.get('controller.isLoaded')) return;
    this.set('selectedAlertGroup', [this.get('controller.selectedAlertGroup')]);
  }.observes('controller.selectedAlertGroup'),

  /**
   * Select first alert group when all groups are loaded
   * @method onLoad
   */
  onLoad: function () {
    if (this.get('controller.isLoaded')) {
      this.set('selectedAlertGroup', [this.get('controller.alertGroups.firstObject')]);
      this.setTooltips();
    }
  }.observes('controller.isLoaded'),

  /**
   * Load alert groups, definitions and notification to have all new data
   * Useful if user delete some definition and immediately open "Manage Alert Groups" popup
   * @method willInsertElement
   */
  willInsertElement: function() {
    this.get('controller').loadAlertNotifications();
  },

  /**
   * Add tooltips and try to select first alert group
   * @method didInsertElement
   */
  didInsertElement: function () {
    this.onLoad();
  },

  /**
   * Setup tooltips for view elements
   */
  setTooltips: function () {
    Em.run.next(function(){
      App.tooltip($("[rel='button-info']"));
      App.tooltip($("[rel='button-info-dropdown']"), {placement: 'left'});
    });
  },

  willDestroyElement: function () {
    $("[rel='button-info']").tooltip('destroy');
    $("[rel='button-info-dropdown']").tooltip('destroy');
  },

  /**
   * Tooltip for "Add group"-button
   * @type {string}
   */
  addButtonTooltip: Em.I18n.t('alerts.actions.manage_alert_groups_popup.addButton'),

  /**
   * Tooltip for "Remove group"-button
   * @type {string}
   */
  removeButtonTooltip: Em.computed.ifThenElse('controller.isRemoveButtonDisabled', Em.I18n.t('alerts.actions.manage_alert_groups_popup.removeButtonDisabled'), Em.I18n.t('alerts.actions.manage_alert_groups_popup.removeButton')),

  /**
   * Tooltip for "Rename"-button
   * @type {string}
   */
  renameButtonTooltip: Em.I18n.t('alerts.actions.manage_alert_groups_popup.renameButton'),

  /**
   * Tooltip for "Duplicate group"-button
   * @type {string}
   */
  duplicateButtonTooltip: Em.I18n.t('alerts.actions.manage_alert_groups_popup.duplicateButton'),

  /**
   * Tooltip for "Add definition to group"-button
   * @type {string}
   */
  addDefinitionTooltip: function () {
    if (this.get('controller.selectedAlertGroup.default')) {
      return Em.I18n.t('alerts.actions.manage_alert_groups_popup.addDefinitionToDefault');
    }
    if (this.get('controller.selectedAlertGroup.isAddDefinitionsDisabled')) {
      return Em.I18n.t('alerts.actions.manage_alert_groups_popup.addDefinitionDisabled');
    }
    return Em.I18n.t('alerts.actions.manage_alert_groups_popup.addDefinition');
  }.property('controller.selectedAlertGroup.default', 'controller.selectedAlertGroup.isAddDefinitionsDisabled'),

  /**
   * Tooltip for "Remove definition from group"-button
   * @type {string}
   */
  removeDefinitionTooltip: function () {
    if (this.get('controller.selectedAlertGroup.default')) {
      return Em.I18n.t('alerts.actions.manage_alert_groups_popup.removeDefinitionDisabled');
    }
    if (this.get('controller.isDeleteDefinitionsDisabled')) {
      return Em.I18n.t('common.nothingToDelete');
    }
    return Em.I18n.t('alerts.actions.manage_alert_groups_popup.removeDefinition');
  }.property('controller.selectedAlertGroup.default', 'controller.isDeleteDefinitionsDisabled')

});
