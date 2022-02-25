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

App.MainServiceManageConfigGroupView = Em.View.extend({

  templateName: require('templates/main/service/manage_configuration_groups_popup'),

  /**
   * Select Config Group
   * @type {App.ConfigGroup}
   */
  selectedConfigGroup: null,

  /**
   * Determines if "Delete Config Group" button is disabled
   * @type {boolean}
   */
  isRemoveButtonDisabled: true,

  /**
   * Determines if "Rename Config Group" button is disabled
   * @type {boolean}
   */
  isRenameButtonDisabled: true,

  /**
   * Determines if "Duplicate Config Group" button is disabled
   * @type {boolean}
   */
  isDuplicateButtonDisabled: true,

  /**
   * Tooltip for "Add Config Group" button
   * @type {string}
   */
  addButtonTooltip: Em.I18n.t('services.service.config_groups_popup.addButton'),

  /**
   * Tooltip for "Remove Config Group" button
   * @type {string}
   */
  removeButtonTooltip: Em.I18n.t('services.service.config_groups_popup.removeButton'),

  /**
   * Tooltip for "Rename Config Group" button
   * @type {string}
   */
  renameButtonTooltip: Em.I18n.t('services.service.config_groups_popup.renameButton'),

  /**
   * Tooltip for "Duplicate Config Group" button
   * @type {string}
   */
  duplicateButtonTooltip: Em.I18n.t('services.service.config_groups_popup.duplicateButton'),

  /**
   * Tooltip for "Remove Host From Config Group" button
   * @type {string}
   */
  removeHostTooltip: Em.I18n.t('services.service.config_groups_popup.removeHost'),

  /**
   * Tooltip for "Add Host To Config Group" button
   * @type {string}
   */
  addHostTooltip: function () {
    var selectedConfigGroup = this.get('controller.selectedConfigGroup');
    if (!selectedConfigGroup.get('isDefault') && selectedConfigGroup.get('isAddHostsDisabled')) {
      return Em.I18n.t('services.service.config_groups_popup.addHostDisabled');
    } else {
      return Em.I18n.t('services.service.config_groups_popup.addHost');
    }
  }.property('controller.selectedConfigGroup.isDefault', 'controller.selectedConfigGroup.isAddHostsDisabled'),

  willInsertElement: function() {
    this.get('controller').loadHosts();
  },

  willDestroyElement: function () {
    this.get('controller.configGroups').clear();
    this.get('controller.originalConfigGroups').clear();
  },

  showTooltip: function () {
    if (!this.get('controller.isLoaded')) return false;
    Em.run.next(function(){
      App.tooltip($('.properties-link'), {html: true});
      App.tooltip($("[rel='button-info']"));
      App.tooltip($("[rel='button-info-dropdown']"), {placement: 'left'});
    });
  }.observes('controller.isLoaded'),

  /**
   * Disable actions remove and rename for Default config group
   * @method buttonObserver
   */
  buttonObserver: function () {
    var selectedConfigGroup = this.get('controller.selectedConfigGroup');
    this.set('isRemoveButtonDisabled', selectedConfigGroup.get('isDefault'));
    this.set('isRenameButtonDisabled', selectedConfigGroup.get('isDefault'));
    this.set('isDuplicateButtonDisabled', false);
  }.observes('controller.selectedConfigGroup'),

  /**
   * Prevent user to select more than 1 config group
   * Select last one of "selected"
   * Clean up <code>controller.selectedHosts</code>
   * @method onGroupSelect
   */
  onGroupSelect: function () {
    var selectedConfigGroup = this.get('selectedConfigGroup');
    // to unable user select more than one config group at a time
    if (selectedConfigGroup.length) {
      this.set('controller.selectedConfigGroup', selectedConfigGroup[selectedConfigGroup.length - 1]);
    }
    if (selectedConfigGroup.length > 1) {
      this.set('selectedConfigGroup', selectedConfigGroup[selectedConfigGroup.length - 1]);
    }
    this.set('controller.selectedHosts', []);
  }.observes('selectedConfigGroup'),

  /**
   * Select default config group after all config groups are loaded
   * @method selectDefaultGroup
   */
  selectDefaultGroup: function () {
    if (this.get('controller.isLoaded')) {
      this.set('selectedConfigGroup', [this.get('controller.configGroups').findProperty('isDefault')]);
    }
  }.observes('controller.isLoaded', 'controller.groupDeleteTrigger')

});
