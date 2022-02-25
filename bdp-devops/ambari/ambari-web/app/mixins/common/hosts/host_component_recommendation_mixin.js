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
 * @typedef {object} RecommendComponentObject
 * @property {string} componentName name of the component
 * @property {number} [size=0] desired components size
 * @property {string[]} [hosts=[]] hosts assigned to component
 */

/**
 * @typedef {object} HostComponentRecommendationOptions
 * @property {string[]} hosts list of host names, in most cases all available host names
 * @property {RecommendComponentObject[]} components list of components
 * @property {string[]} services list of service names
 * @property {object} [blueprint=null] when null blueprint will be created by <code>HostComponentRecommendationOptions.components</code> attribute
 */

/**
 * @typedef {object} HostRecommendationRequestData
 * @property {string} stackVersionUrl stack version url
 * @property {string[]} hosts host names
 * @property {string[]} services service names
 * @property {string} recommend recommendation type e.g. 'host_groups'
 * @property {object} recommendations blueprint object
 */

/**
 * Contains methods to get server recommendation and validation for host components
 * @type {Em.Mixin}
 */
App.HostComponentRecommendationMixin = Em.Mixin.create(App.BlueprintMixin, {

  /**
   * Get recommendations for selected components
   * @method getRecommendedHosts
   * @param {HostComponentRecommendationOptions} recommend list of the components
   * @return {$.Deferred}
   */
  getRecommendedHosts: function(options) {
    var opts = $.extend({
      services: [],
      hosts: [],
      components: [],
      blueprint: null
    }, options || {});

    opts.components = this.formatRecommendComponents(opts.components);
    return this.loadComponentsRecommendationsFromServer(this.getRecommendationRequestData(opts));
  },

  /**
   * @method formatRecommendComponents
   * @param {RecommendComponentsObject[]} components
   * @returns {Em.Object[]}
   */
  formatRecommendComponents: function(components) {
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
   * Returns request data for recommendation request
   * @param {HostComponentRecommendationOptions} options
   * @return {HostRecommendationRequestData}
   * @method getRecommendationRequestData
   */
  getRecommendationRequestData: function(options) {
    return {
      recommend: 'host_groups',
      stackVersionUrl: App.get('stackVersionURL'),
      hosts: options.hosts,
      services: options.services,
      recommendations: options.blueprint || this.getComponentsBlueprint(options.components)
    };
  },

  /**
   * Get recommendations info from API
   * @method loadComponentsRecommendationsFromServer
   * @param {object} recommendationData
   * @returns {$.Deferred}
   */
  loadComponentsRecommendationsFromServer: function(recommendationData) {
    return App.ajax.send({
      name: 'wizard.loadrecommendations',
      sender: this,
      data: recommendationData,
      success: 'loadRecommendationsSuccessCallback',
      error: 'loadRecommendationsErrorCallback'
    });
  },

  loadRecommendationsSuccessCallback: function() {},
  loadRecommendationsErrorCallback: function() {}
});
