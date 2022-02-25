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

/**
 * Show confirmation popup
 * @param {string} selectedServiceName
 * @param {App.ConfigGroup} selectedConfigGroup
 * @param {App.ServiceConfig[]} dependentStepConfigs
 * @param {Object[]} configs
 * @return {App.ModalPopup}
 */
App.showSelectGroupsPopup = function (selectedServiceName, selectedConfigGroup, dependentStepConfigs, configs) {
  return App.ModalPopup.show({
    encodeBody: false,
    primary: Em.I18n.t('common.save'),
    header: Em.I18n.t('popup.dependent.configs.select.config.group.header'),
    selectedServiceName: selectedServiceName,
    selectedConfigGroup: selectedConfigGroup,
    dependentStepConfigs: dependentStepConfigs,
    selectedGroups: {},
    didInsertElement: function () {
      this._super();
      this.set('selectedGroups', $.extend({}, selectedConfigGroup.get('dependentConfigGroups')));
    },
    bodyClass: Em.CollectionView.extend({
      content: dependentStepConfigs,
      itemViewClass: Em.View.extend({
        templateName: require('templates/common/modal_popups/select_groups_popup'),
        didInsertElement: function () {
          this.set('selectedGroup', this.get('parentView.parentView.selectedConfigGroup.dependentConfigGroups')[this.get('serviceName')]);
        },
        hasGroups: Em.computed.bool('groups.length'),
        serviceName: Em.computed.alias('content.serviceName'),
        selectedGroup: null,
        updateGroup: function () {
          var dependentGroups = $.extend({}, this.get('parentView.parentView.selectedConfigGroup.dependentConfigGroups'));
          dependentGroups[this.get('serviceName')] = this.get('selectedGroup');
          this.set('parentView.parentView.selectedConfigGroup.dependentConfigGroups', dependentGroups);
        }.observes('selectedGroup'),
        groups: function () {
          return this.get('content').get('configGroups').filterProperty('isDefault', false).mapProperty('name');
        }.property('content')
      })
    }),
    onPrimary: function () {
      this._super();
      Object.keys(this.get('selectedConfigGroup.dependentConfigGroups')).forEach(function (serviceName) {
        var selectedGroupName = this.get('selectedConfigGroup.dependentConfigGroups')[serviceName];
        var currentGroupName = this.get('selectedGroups')[serviceName] || "";
        var configGroup = this.get('dependentStepConfigs').findProperty('serviceName', serviceName).get('configGroups').findProperty('name', selectedGroupName);
        if (!configGroup) return; //There can be no dependent config group.
        if (selectedGroupName !== currentGroupName) {
          /** changing config group for recommendations **/
          configs.filterProperty('serviceName', serviceName).filterProperty('configGroup', selectedGroupName).forEach(function (c) {
            configs.removeObject(c);
          });
          configs.filterProperty('serviceName', serviceName).filterProperty('configGroup', currentGroupName).setEach('configGroup', selectedGroupName);
          /** danger part!!!! changing config group ***/
          this.applyOverridesToConfigGroups(serviceName, configGroup, currentGroupName, selectedGroupName);
        }
      }, this);
    },
    
    /**
     *
     * @param serviceName
     * @param configGroup
     * @param currentGroupName
     * @param selectedGroupName
     */
    applyOverridesToConfigGroups: function(serviceName, configGroup, currentGroupName, selectedGroupName) {
      this.get('dependentStepConfigs').findProperty('serviceName', serviceName).get('configs').forEach(function (cp) {
        var dependentConfig = configs.filterProperty('propertyName', cp.get('name'))
        .filterProperty('fileName', App.config.getConfigTagFromFileName(cp.get('filename')))
        .findProperty('configGroup', selectedGroupName);
        var recommendedValue = dependentConfig && Em.get(dependentConfig, 'recommendedValue');
        if (cp.get('overrides')) {
          var currentGroupOverride = cp.get('overrides').findProperty('group.name', currentGroupName);
          if (currentGroupOverride && currentGroupOverride.get('initialValue') !== currentGroupOverride.get('recommendedValue')) {
            currentGroupOverride.set('group', configGroup);
            currentGroupOverride.set('recommendedValue', recommendedValue);
            currentGroupOverride.set('value', recommendedValue);
          } else {
            var selectedGroupOverride = cp.get('overrides').findProperty('group.name', configGroup.get('name'));
            if (selectedGroupOverride) {
              selectedGroupOverride.set('recommendedValue', recommendedValue);
              selectedGroupOverride.set('value', recommendedValue);
            } else {
              App.config.createOverride(cp, {
                "value": recommendedValue,
                "recommendedValue": recommendedValue,
                "isEditable": true
              }, configGroup);
            }
          }
        } else {
          App.config.createOverride(cp, {
            "value": recommendedValue,
            "recommendedValue": recommendedValue,
            "isEditable": true
          }, configGroup);
        }
      }, this)
    },
    onSecondary: function () {
      this._super();
      this.get('selectedConfigGroup').set('dependentConfigGroups', this.get('selectedGroups'));
    }
  });
};