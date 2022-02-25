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

App.MainAdminServiceAutoStartController = Em.Controller.extend({
  name: 'mainAdminServiceAutoStartController',

  /**
   * @type {?object}
   * @default null
   */
  clusterConfigs: null,

  /**
   * @type {Array}
   */
  componentsConfigsCached: [],

  /**
   * @type {object}
   */
  componentsConfigsCachedMap: function() {
    const map = {};
    this.get('componentsConfigsCached').forEach((component) => {
      map[component.component_name] = component.recovery_enabled === 'true'
    });
    return map;
  }.property('componentsConfigsCached.@each.recovery_enabled'),

  /**
   * @type {Array}
   */
  componentsConfigsGrouped: [],

  /**
   * @type {boolean}
   */
  isLoaded: false,

  /**
   * @type {boolean}
   */
  isGeneralRecoveryEnabled: false,

  /**
   * @type {boolean}
   */
  isGeneralRecoveryEnabledCached: false,

  /**
   * @type {boolean}
   */
  saveInProgress: false,

  /**
   * @type {boolean}
   */
  isModified: function() {
    return this.get('isGeneralModified') || this.get('isComponentModified');
  }.property('isGeneralModified', 'isComponentModified'),

  /**
   * @type {boolean}
   */
  isGeneralModified: function() {
    return this.get('isGeneralRecoveryEnabled') !== this.get('isGeneralRecoveryEnabledCached')
  }.property('isGeneralRecoveryEnabled', 'isGeneralRecoveryEnabledCached'),

  /**
   * @type {boolean}
   */
  isComponentModified: function() {
    const componentsConfigsCachedMap = this.get('componentsConfigsCachedMap');

    return this.get('componentsConfigsGrouped').some((component) => {
      return component.get('recoveryEnabled') !== componentsConfigsCachedMap[component.get('componentName')];
    });
  }.property('componentsConfigsGrouped.@each.recoveryEnabled', 'componentsConfigsCachedMap'),

  parseComponentConfigs: function(componentsConfigsCached) {
    componentsConfigsCached.sortPropertyLight('service_name');
    const componentsConfigsGrouped = [];
    const servicesMap = componentsConfigsCached.mapProperty('service_name').uniq().toWickMap();
  
    componentsConfigsCached.forEach((component) => {
      componentsConfigsGrouped.push(Em.Object.create({
        serviceDisplayName: App.format.role(component.service_name, true),
        isFirst: servicesMap[component.service_name],
        componentName: component.component_name,
        displayName: App.format.role(component.component_name, false),
        recoveryEnabled: component.recovery_enabled === 'true'
      }));
      servicesMap[component.service_name] = false;
    });
    return componentsConfigsGrouped;
  },

  load: function() {
    const dfd = $.Deferred();
    const clusterConfigController = App.router.get('configurationController');
    clusterConfigController.updateConfigTags().always(() => {
      clusterConfigController.getCurrentConfigsBySites(['cluster-env']).done((data) => {
        this.set('clusterConfigs', data[0].properties);
        this.set('isGeneralRecoveryEnabled', data[0].properties.recovery_enabled === 'true');
        this.set('isGeneralRecoveryEnabledCached', this.get('isGeneralRecoveryEnabled'));
        this.loadComponentsConfigs().then(() => {
          this.set('isLoaded', true);
          dfd.resolve();
        }, () => dfd.reject());
      });
    });
    
    return dfd;
  },

  loadComponentsConfigs: function () {
    return App.ajax.send({
      name: 'components.get_category',
      sender: this,
      success: 'loadComponentsConfigsSuccess'
    });
  },

  loadComponentsConfigsSuccess: function (data) {
    const restartableComponents = data.items.mapProperty('ServiceComponentInfo').filter((component) => {
      // Hide clients, as the are not restartable, components which are not installed
      return App.StackServiceComponent.find(component.component_name).get('isRestartable') && component.total_count > 0;
    });
    this.set('componentsConfigsCached', restartableComponents);
    this.set('componentsConfigsGrouped', this.parseComponentConfigs(restartableComponents));
  },

  saveClusterConfigs: function (clusterConfigs, recoveryEnabled) {
    clusterConfigs.recovery_enabled = String(recoveryEnabled);
    return App.ajax.send({
      name: 'admin.save_configs',
      sender: this,
      data: {
        siteName: 'cluster-env',
        properties: clusterConfigs
      }
    });
  },

  saveComponentSettingsCall: function(recoveryEnabled, components) {
    return App.ajax.send({
      name: 'components.update',
      sender: this,
      data: {
        ServiceComponentInfo: {
          recovery_enabled: recoveryEnabled
        },
        query: 'ServiceComponentInfo/component_name.in(' + components.join(',') + ')'
      }
    });
  },

  syncStatus: function () {
    const componentsConfigsGrouped = this.get('componentsConfigsGrouped');
    this.set('isGeneralRecoveryEnabledCached', this.get('isGeneralRecoveryEnabled'));
    this.get('componentsConfigsCached').forEach((component) => {
      const actualComponent = componentsConfigsGrouped.findProperty('componentName', component.component_name);
      Ember.set(component, 'recovery_enabled', String(actualComponent.get('recoveryEnabled')));
    });
    this.propertyDidChange('componentsConfigsCached');
  },

  restoreCachedValues: function () {
    this.set('isGeneralRecoveryEnabled', this.get('isGeneralRecoveryEnabledCached'));
    this.set('componentsConfigsGrouped', this.parseComponentConfigs(this.get('componentsConfigsCached')));
  },

  filterComponentsByChange: function(components, value) {
    const map = this.get('componentsConfigsCachedMap');

    return components.filter((component) => {
      return component.get('recoveryEnabled') !== map[component.get('componentName')]
          && component.get('recoveryEnabled') === value;
    }).mapProperty('componentName');
  },

  /**
   * If some configs are changed and user navigates away or select another config-group, show this popup with propose to save changes
   * @param {object} transitionCallback - callback with action to change configs view
   * @return {App.ModalPopup}
   * @method showSavePopup
   */
  showSavePopup: function (transitionCallback) {
    var self = this;
    var title = '';
    var body = '';
    if (typeof transitionCallback === 'function') {
      title = Em.I18n.t('admin.serviceAutoStart.save.popup.transition.title');
      body = Em.I18n.t('admin.serviceAutoStart.save.popup.transition.body');
    } else {
      title = Em.I18n.t('admin.serviceAutoStart.save.popup.title');
      body = Em.I18n.t('admin.serviceAutoStart.save.popup.body');
    }
    return App.ModalPopup.show({
      header: title,
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile(body)
      }),
      footerClass: Em.View.extend({
        templateName: require('templates/main/service/info/save_popup_footer')
      }),
      primary: Em.I18n.t('common.save'),
      secondary: Em.I18n.t('common.cancel'),
      onSave: function () {
        let clusterConfigsCall, enabledComponentsCall, disabledComponentsCall;

        if (self.get('isGeneralModified')) {
          clusterConfigsCall = self.saveClusterConfigs(self.get('clusterConfigs'), self.get('isGeneralRecoveryEnabled'));
        }

        const enabledComponents = self.filterComponentsByChange(self.get('componentsConfigsGrouped'), true);
        const disabledComponents = self.filterComponentsByChange(self.get('componentsConfigsGrouped'), false);

        if (enabledComponents.length) {
          enabledComponentsCall = self.saveComponentSettingsCall('true', enabledComponents);
        }
        if (disabledComponents.length) {
          disabledComponentsCall = self.saveComponentSettingsCall('false', disabledComponents);
        }
        self.set('saveInProgress', true);
        $.when(clusterConfigsCall, enabledComponentsCall, disabledComponentsCall).done(function () {
          if (typeof transitionCallback === 'function') {
            transitionCallback();
          }
          self.syncStatus();
        }).always(function() {
          self.set('saveInProgress', false);
        });
        this.hide();
      },
      onDiscard: function () {
        self.restoreCachedValues();
        if (typeof transitionCallback === 'function') {
          transitionCallback();
        }
        this.hide();
      },
      onCancel: function () {
        this.hide();
      }
    });
  }
});
