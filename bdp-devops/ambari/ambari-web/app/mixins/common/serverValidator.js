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
var blueprintUtils = require('utils/blueprint');

/**
 * @typedef {object} ConfigsValidationRequestData
 * @property {string} stackVersionUrl stack version url
 * @property {string[]} hosts host names
 * @property {string[]} services service names
 * @property {string} validate validation type e.g. 'configurations'
 * @property {object} recommendations blueprint object
 */

/**
 * @typedef {object} ConfigsValidationOptions
 * @property {string[]} hosts host names
 * @property {string[]} services service names
 * @property {object} blueprint service configurations blueprint
 */
App.ServerValidatorMixin = Em.Mixin.create({

  /**
   * defines if we use validation and recommendation on wizards
   * depend on this flag some properties will be taken from different places
   * @type {boolean}
   */
  isWizard: function() {
    return this.get('wizardController') && ['addServiceController', 'installerController'].contains(this.get('wizardController.name'));
  }.property('wizardController.name'),

  /**
   * recommendation configs loaded from server
   * (used only during install)
   * @type {Object}
   */
  recommendationsConfigs: null,

  /**
   * Collection of all config validation errors
   *
   * @type {Object[]}
   */
  configErrorList: Em.Object.create({
    issues: [],
    criticalIssues: []
  }),

  /**
   * Ajax request object to validation API
   * @type {$.ajax}
   */
  validationRequest: null,

  /**
   * Timer id returned by setTimeout for calling function to send validation request
   */
  requestTime: 0,

  /**
   * Map with allowed error types
   *
   * @type {Object}
   */
  errorTypes: {
    CRITICAL_ERROR: 'NOT_APPLICABLE',
    ERROR: 'ERROR',
    WARN: 'WARN',
    GENERAL: 'GENERAL'
  },

  /**
   * by default loads data from model otherwise must be overridden as computed property
   * refer to \assets\data\stacks\HDP-2.1\recommendations_configs.json to learn structure
   * (shouldn't contain configurations filed)
   * @type {Object}
   */
  hostNames: function() {
    return this.get('isWizard')
        ? Object.keys(this.get('content.hosts'))
        : App.get('allHostNames');
  }.property('isWizard', 'content.hosts', 'App.allHostNames'),

  /**
   * by default loads data from model otherwise must be overridden as computed property
   * @type {Array} - of strings (serviceNames)
   */
  serviceNames: function() {
    // When editing a service we validate only that service's configs.
    // However, we should pass the IDs of services installed, or else,
    // default value calculations will alter.
    return this.get('isWizard') ? this.get('allSelectedServiceNames') : App.Service.find().mapProperty('serviceName');
  }.property('isWizard', 'allSelectedServiceNames'),

  /**
   * by default loads data from model otherwise must be overridden as computed property
   * can be used for service|host configs pages
   * @type {Array} of strings (hostNames)
   */
  hostGroups: function() {
    return this.get('content.recommendationsHostGroups') || blueprintUtils.generateHostGroups(App.get('allHostNames'));
  }.property('content.recommendationsHostGroups', 'App.allHostNames', 'App.componentToBeAdded', 'App.componentToBeDeleted'),

  /**
   * controller that is child of this mixin has to contain stepConfigs
   * @type {Array}
   */
  stepConfigs: null,

  serverSideValidation: function () {
    var deferred = $.Deferred(),
      self = this,
      primary = function() { deferred.resolve(); },
      secondary = function() { deferred.reject('invalid_configs'); };
    this.set('configErrorList', Em.Object.create({
      issues: [],
      criticalIssues: []
    }));

    this.runServerSideValidation().done(function() {
      if (self.get('configErrorList.issues.length') || self.get('configErrorList.criticalIssues.length')) {
        App.showConfigValidationPopup(self.get('configErrorList'), primary, secondary, self);
      } else {
        deferred.resolve();
      }
    }).fail(function() {
      App.showConfigValidationFailedPopup(primary, secondary);
    });

    return deferred.promise();
  },

  /**
   * @method serverSideValidation
   * send request to validate configs
   * @returns {*}
   */
  runServerSideValidation: function () {
    var self = this;
    var recommendations = this.get('hostGroups');
    var stepConfigs = this.get('stepConfigs');
    var dfd = $.Deferred();

    this.getBlueprintConfigurations(this.get('stepConfigs'))
      .done(function(blueprintConfigurations) {
        recommendations.blueprint.configurations = blueprintConfigurations;
        var request = self.validateSelectedConfigs({
          hosts: self.get('hostNames'),
          services: self.get('serviceNames'),
          blueprint: recommendations
        });
        self.set('validationRequest', request);
        request.done(dfd.resolve).fail(dfd.reject);
      });
    return dfd.promise();
  },

  /**
   * Perform service config validation
   * @param validateSelectedConfigs
   * @param {ConfigsValidationOptions} options
   * @returns {$.Deferred}
   */
  validateSelectedConfigs: function(options) {
    var opts = $.extend({
      services: [],
      hosts: [],
      blueprint: null
    }, options || {});

    return this.getServiceConfigsValidationRequest(this.getServiceConfigsValidationParams(opts));
  },

  /**
   * @method getServiceConfigsValidationRequest
   * @param {ConfigsValidationRequestData} validationData
   * @returns {$.Deferred}
   */
  getServiceConfigsValidationRequest: function(validationData) {
    return App.ajax.send({
      name: 'config.validations',
      sender: this,
      data: validationData,
      success: 'validationSuccess'
    });
  },

  /**
   * @method getServiceConfigsValidationParams
   * @param {ConfigsValidationOptions} options
   * @returns {ConfigsValidationRequestData}
   */
  getServiceConfigsValidationParams: function(options) {
    return {
      stackVersionUrl: App.get('stackVersionURL'),
      hosts: options.hosts,
      services: options.services,
      validate: 'configurations',
      recommendations: options.blueprint
    };
  },

  /**
   * Return JSON for blueprint configurations
   * @param {App.ServiceConfigs[]} serviceConfigs
   * @returns {*}
   */
  getBlueprintConfigurations: function (serviceConfigs) {
    var dfd = $.Deferred();
    // check if we have configs from 'cluster-env', if not, then load them, as they are mandatory for validation request
    if (!serviceConfigs.findProperty('serviceName', 'MISC')) {
      App.config.getConfigsByTypes([{site: 'cluster-env', serviceName: 'MISC'}]).done(function(configs) {
        dfd.resolve(blueprintUtils.buildConfigsJSON(serviceConfigs.concat(configs)));
      });
    } else {
      dfd.resolve(blueprintUtils.buildConfigsJSON(serviceConfigs));
    }
    return dfd.promise();
  },

  /**
   * Creates config validation error object
   *
   * @param type - error type, see <code>errorTypes<code>
   * @param property - config property object
   * @param messages - array of messages
   * @returns {{type: String, isError: boolean, isWarn: boolean, isGeneral: boolean, messages: Array}}
   */
  createErrorMessage: function (type, property, messages) {
    const errorTypes = this.get('errorTypes');
    let error = {
      type: type,
      isCriticalError: type === errorTypes.CRITICAL_ERROR,
      isError: type === errorTypes.ERROR,
      isWarn: type === errorTypes.WARN,
      isGeneral: type === errorTypes.GENERAL,
      cssClass: this.get('isInstallWizard') ? '' : (type === errorTypes.ERROR ? 'error' : 'warning'),
      messages: Em.makeArray(messages)
    };

    Em.assert('Unknown config error type ' + type, error.isError || error.isWarn || error.isGeneral || error.isCriticalError);
    if (property) {
      const value = Em.get(property, 'value');
      error.id = Em.get(property, 'id');
      error.serviceName = Em.get(property, 'serviceDisplayName') || App.StackService.find(Em.get(property, 'serviceName')).get('displayName');
      error.propertyName = Em.get(property, 'name');
      error.filename = Em.get(property, 'filename');
      error.value = value && Em.get(property, 'displayType') === 'password' ? new Array(value.length + 1).join('*') : value;
      error.description = Em.get(property, 'description');
    }
    return error;
  },


  /**
   * Parse data from server to
   *  <code>configErrorsMap<code> and
   *  <code>generalErrors<code>
   *
   * @param data
   * @returns {{configErrorsMap: {}, generalErrors: Array}}
   */
  parseValidation: function(data) {
    var configErrorsMap = {},  generalErrors = [];

    data.resources.forEach(function(r) {
      r.items.forEach(function(item){
        if (item.type == "configuration") {
          var configId = (item['config-name'] && item['config-type']) && App.config.configId(item['config-name'], item['config-type']);
          if (configId) {
            if (configErrorsMap[configId]) {
              configErrorsMap[configId].messages.push(item.message);
            } else {
              configErrorsMap[configId] = {
                type: item.level,
                messages: [item.message],
                name: item['config-name'],
                filename: item['config-type']
              }
            }
          } else {
            generalErrors.push({
              type: this.get('errorTypes').GENERAL,
              messages: [item.message]
            });
          }
        }
      }, this);
    }, this);

    return {
      configErrorsMap: configErrorsMap,
      generalErrors: generalErrors
    }
  },

  /**
   * Generates list of all config errors that should be displayed in popup
   *
   * @param configErrorsMap
   * @param generalErrors
   * @returns {Array}
   */
  collectAllIssues: function(configErrorsMap, generalErrors)  {
    var errorTypes = this.get('errorTypes');
    var configErrorList = {};
    configErrorList[errorTypes.WARN] = [];
    configErrorList[errorTypes.ERROR] = [];
    configErrorList[errorTypes.CRITICAL_ERROR] = [];
    configErrorList[errorTypes.GENERAL] = [];

    this.get('stepConfigs').forEach(function(service) {
      service.get('configs').forEach(function(property) {
        if (property.get('isVisible') && !property.get('hiddenBySection')) {
          var serverIssue = configErrorsMap[property.get('id')];
          if (serverIssue) {
            configErrorList[serverIssue.type].push(this.createErrorMessage(serverIssue.type, property, serverIssue.messages));
          } else if (property.get('warnMessage')) {
            configErrorList[errorTypes.WARN].push(this.createErrorMessage(errorTypes.WARN, property, [property.get('warnMessage')]));
          }
        }
      }, this);
    }, this);

    generalErrors.forEach(function(serverIssue) {
      configErrorList[errorTypes.GENERAL].push(this.createErrorMessage(errorTypes.GENERAL, null, serverIssue.messages));
    }, this);

    Em.keys(configErrorsMap).forEach(function (id) {
      var serverIssue = configErrorsMap[id];
      if (!configErrorList[serverIssue.type].someProperty('id', id)) {
        var filename = Em.get(serverIssue, 'filename'),
          service = App.config.get('serviceByConfigTypeMap')[filename],
          property = {
            id: id,
            name: Em.get(serverIssue, 'name'),
            filename: App.config.getOriginalFileName(filename),
            serviceDisplayName: service && Em.get(service, 'displayName')
          };
        configErrorList[serverIssue.type].push(this.createErrorMessage(serverIssue.type, property, serverIssue.messages));
      }
    }, this);

    return Em.Object.create({
      criticalIssues: configErrorList[errorTypes.CRITICAL_ERROR],
      issues: configErrorList[errorTypes.ERROR].concat(configErrorList[errorTypes.WARN], configErrorList[errorTypes.GENERAL])
    });
  },

  /**
   * @method validationSuccess
   * success callback after getting response from server
   * go through the step configs and set warn and error messages
   * @param data
   */
  validationSuccess: function(data) {
    var parsed = this.parseValidation(data);
    this.set('configErrorList', this.collectAllIssues(parsed.configErrorsMap, parsed.generalErrors));
  },
  
  valueObserver: function () {
    var self = this;
    if (this.get('isInstallWizard') && this.get('currentTabName') === 'all-configurations') {
      if (this.get('requestTimer')) clearTimeout(this.get('requestTimer'));
      self.set('requestTimer', setTimeout(function () {
        if (self.get('validationRequest')) {
          self.get('validationRequest').abort();
        }
        if (self.get('recommendationsInProgress')) {
          self.valueObserver();
        } else {
          self.runServerSideValidation().done(function () {
            self.set('validationRequest', null);
            self.set('requestTimer', 0);
          });
        }
      }, 500));
    }
  }.observes('selectedService.configs.@each.value')

});
