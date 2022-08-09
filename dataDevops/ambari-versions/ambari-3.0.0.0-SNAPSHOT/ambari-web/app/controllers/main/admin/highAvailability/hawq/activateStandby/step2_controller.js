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

/**
 * @typedef {object} hawqActivateStandbyConfigDependencies
 */

var App = require('app');
require('utils/configs/hawq_activate_standby_config_initializer');

App.ActivateHawqStandbyWizardStep2Controller = Em.Controller.extend({
  name: "activateHawqStandbyWizardStep2Controller",

  selectedService: null,

  versionLoaded: true,

  hideDependenciesInfoBar: true,

  isLoaded: false,

  isSubmitDisabled: Em.computed.not('isLoaded'),

  loadStep: function () {
    this.renderConfigs();
  },

  /**
   * Render configs to show them in <code>App.ServiceConfigView</code>
   */
  renderConfigs: function () {
    newHawqMaster = App.HostComponent.find().findProperty('componentName','HAWQSTANDBY').get('hostName');

    var configs = require('data/configs/wizards/hawq_activate_standby_properties').haConfig;

    var serviceConfig = App.ServiceConfig.create({
      serviceName: configs.serviceName,
      displayName: configs.displayName,
      configCategories: [],
      showConfig: true,
      configs: []
    });

    configs.configCategories.forEach(function (configCategory) {
      if (App.Service.find().someProperty('serviceName', configCategory.name)) {
        serviceConfig.configCategories.pushObject(configCategory);
      }
    }, this);

    this.renderConfigProperties(configs, serviceConfig);
    this.setDynamicConfigValues(serviceConfig);
    this.setProperties({
      selectedService: serviceConfig,
      isLoaded: true
    });
  },


  setDynamicConfigValues: function (configs) {
    var topologyLocalDB = this.get('content').getProperties(['masterComponentHosts']);
    configs.configs.forEach(function (config) {
      App.HawqActivateStandbyConfigInitializer.initialValue(config, topologyLocalDB);
    });
    App.HawqActivateStandbyConfigInitializer.cleanup();
    return configs;
  },

  /**
   * Load child components to service config object
   * @param _componentConfig
   * @param componentConfig
   */
  renderConfigProperties: function (_componentConfig, componentConfig) {
    _componentConfig.configs.forEach(function (_serviceConfigProperty) {
      var serviceConfigProperty = App.ServiceConfigProperty.create(_serviceConfigProperty);
      componentConfig.configs.pushObject(serviceConfigProperty);
      serviceConfigProperty.set('isEditable', serviceConfigProperty.get('isReconfigurable'));
    }, this);
  },

  submit: function () {
    if (!this.get('isSubmitDisabled')) {
      App.get('router.mainAdminKerberosController').getKDCSessionState(function() {
        App.router.send("next");
      });
    }
  }

});
