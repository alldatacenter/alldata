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

require('mixins/common/blueprint');

var App = require('app');

/**
 * @typedef {object} HostValidationRequestData
 * @property {string} stackVersionUrl stack version url
 * @property {string[]} hosts host names
 * @property {string[]} services service names
 * @property {string} validate validation type e.g. 'host_groups'
 * @property {object} recommendations blueprint object
 */

App.HostComponentValidationMixin = Em.Mixin.create(App.BlueprintMixin, {
  /**
   * Validate host components
   * @method validateSelectedHostComponents
   * @param {HostComponentRecommendationOptions} options
   * @return {$.Deferred}
   */
  validateSelectedHostComponents: function(options) {
    var opts = $.extend({
      services: [],
      blueprint: null,
      hosts: [],
      components: []
    }, options || {});

    opts.components = this.formatValidateComponents(opts.components);
    return this.getHostComponentValidationRequest(this.getHostComponentValidationParams(opts));
  },

  /**
   * @method formatValidateComponents
   * @param {RecommendComponentObject[]} components
   * @returns {Em.Object[]}
   */
  formatValidateComponents: function(components) {
    var res = [];
    if (!components) return [];
    components.forEach(function(component) {
      var componentName = Em.get(component, 'componentName');
      if (Em.get(component, 'hosts.length')) {
        Em.get(component, 'hosts').forEach(function(hostName) {
          res.push(Em.Object.create({
            componentName: componentName,
            hostName: hostName
          }));
        });
      }
    });
    return res;
  },

  /**
   * Returns request data for validation request
   * @method getHostComponentValidationParams
   * @return {HostValidationRequestData}
   */
  getHostComponentValidationParams: function(options) {
    return {
      stackVersionUrl: App.get('stackVersionURL'),
      hosts: options.hosts,
      services: options.services,
      validate: 'host_groups',
      recommendations: options.blueprint || this.getComponentsBlueprint(options.components)
    };
  },

  /**
   * Performs request to validate components location
   * @method getHostComponentValidationRequest
   * @param {object} validationData
   * @returns {$.Deferred}
   */
  getHostComponentValidationRequest: function(validationData) {
    return App.ajax.send({
      name: 'config.validations',
      sender: this,
      data: validationData,
      success: 'updateValidationsSuccessCallback',
      error: 'updateValidationsErrorCallback'
    });
  },

  updateValidationsSuccessCallback: function() {},
  updateValidationsErrorCallback: function() {}
});
