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
 * @typedef {object} hawqHaConfigDependencies
 */

var App = require('app');
require('utils/configs/hawq_ha_config_initializer');

App.AddHawqStandbyWizardStep3Controller = Em.Controller.extend({
  name: "addHawqStandbyWizardStep3Controller",

  // Used in service_config.hbs
  // selectedService
  // hideDependenciesInfoBar
  // versionLoaded

  selectedService: null,

  hawqProps: null,

  hideDependenciesInfoBar: true,

  versionLoaded: true,

  // Used in step3.hbs
  // isLoaded
  // isSubmitDisabled
  // hawqMasterDirectoryCleanUpMessage
  isLoaded: false,

  isSubmitDisabled: Em.computed.not('isLoaded'),

  loadStep: function () {
    this.renderConfigs();
  },

  content: Em.Object.create({
    controllerName: 'addHawqStandbyWizardStep3Controller'
  }),

  /**
   * Render configs to show them in <code>App.ServiceConfigView</code>
   */
  renderConfigs: function () {

    var configs = require('data/configs/wizards/hawq_ha_properties').haConfig;

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
    App.ajax.send({
      name: 'config.tags',
      sender: this,
      success: 'loadConfigTagsSuccessCallback',
      error: '',
      data: {
        serviceConfig: serviceConfig
      }
    });

  },

  loadConfigTagsSuccessCallback: function (data, opt, params) {
    var urlParams = '(type=hawq-site&tag=' + data.Clusters.desired_configs['hawq-site'].tag + ')';
    App.ajax.send({
      name: 'reassign.load_configs',
      sender: this,
      data: {
        urlParams: urlParams,
        serviceConfig: params.serviceConfig
      },
      success: 'loadConfigsSuccessCallback',
      error: 'loadConfigsSuccessCallback'
    });
  },

  loadConfigsSuccessCallback: function (data, opt, params) {
    params = params.serviceConfig ? params.serviceConfig : {};
    this.setDynamicConfigValues(params, data);
    this.setProperties({
      selectedService: params,
      isLoaded: true,
      hawqProps: data
    });
  },

  setDynamicConfigValues: function (configs, data) {
    var topologyLocalDB = this.get('content').getProperties(['masterComponentHosts']);
    configs.configs.forEach(function (config) {
      App.HawqHaConfigInitializer.initialValue(config, topologyLocalDB);
    });
    App.HawqHaConfigInitializer.cleanup();
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
      const dataDir = this.get('hawqProps').items[0].properties['hawq_master_directory'];
      const hawqStandby = this.get('content.hawqHosts.newHawqStandby');
      App.showConfirmationPopup(
        function() {
          App.get('router.mainAdminKerberosController').getKDCSessionState(function() {
            App.router.send("next");
          });
        },
        Em.I18n.t('admin.addHawqStandby.wizard.step3.confirm.dataDir.body').format(dataDir, hawqStandby),
        null,
        Em.I18n.t('admin.addHawqStandby.wizard.step3.confirm.dataDir.title'),
        "Confirm"
      );
    }
  },

});
