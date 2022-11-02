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

App.RAHighAvailabilityWizardStep3Controller = Em.Controller.extend({
  name: 'rAHighAvailabilityWizardStep3Controller',

  isLoaded: false,

  versionLoaded: true,

  hideDependenciesInfoBar: true,

  stepConfigs: [
    App.ServiceConfig.create({
      serviceName: 'MISC',
      showConfig: true
    })
  ],

  loadStep: function () {
    var self = this;
    App.get('router.mainController.isLoading').call(App.get('router.clusterController'), 'isConfigsPropertiesLoaded').done(function () {
      var stepConfig = self.get('stepConfigs.firstObject'),
        configs = [],
        configCategories = [],
        installedServices = App.Service.find().mapProperty('serviceName');
      self.get('wizardController.configs').forEach(function (config) {
        var service = App.config.get('serviceByConfigTypeMap')[config.siteName];
        if (service) {
          var serviceName = service.get('serviceName'),
            serviceDisplayName = service.get('displayName');
          if (installedServices.contains(serviceName)) {
            var property = App.configsCollection.getConfigByName(config.propertyName, config.siteName) || {};
            if (!configCategories.someProperty('name'), serviceName) {
              configCategories.push(App.ServiceConfigCategory.create({
                name: serviceName,
                displayName: serviceDisplayName
              }));
            }
            configs.push(App.ServiceConfigProperty.create(property, {
              category: serviceName,
              value: self.get('content.loadBalancerURL'),
              isEditable: false
            }));
          }
        }
      });
      stepConfig.setProperties({
        configs: configs,
        configCategories: configCategories
      });
      self.setProperties({
        isLoaded: true,
        selectedService: stepConfig
      });
    });
  }
});

