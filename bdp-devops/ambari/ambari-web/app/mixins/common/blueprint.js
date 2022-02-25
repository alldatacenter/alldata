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
var dataManipulation = require('utils/data_manipulation');

App.BlueprintMixin = Em.Mixin.create({

  /**
   * returns blueprint for selected components
   * @method getComponentsBlueprint
   * @param  {Em.Object[]} components components objects or model @see App.HostComponent
   *  required properties are <code>hostName, componentName</code>
   * @param {string[]} [allHostNames=[]] host names, provide when host groups without components should be created
   * @return {Object} blueprint object
   */
  getComponentsBlueprint: function(components, allHostNames) {
    var hosts = components.mapProperty('hostName').concat(allHostNames || []).uniq();
    var mappedComponents = dataManipulation.groupPropertyValues(components, 'hostName');
    var res = {
      blueprint: { host_groups: [] },
      blueprint_cluster_binding: { host_groups: [] }
    };

    hosts.forEach(function (host, i) {
      var group_name = 'host-group-' + (i + 1);

      res.blueprint.host_groups.push({
        name: group_name,
        components: mappedComponents[host] ? mappedComponents[host].map(function (c) {
          return { name: Em.get(c, 'componentName') };
        }) : []
      });

      res.blueprint_cluster_binding.host_groups.push({
        name: group_name,
        hosts: [
          { fqdn: host }
        ]
      });
    });
    return res;
  },

  /**
   * returns blueprint for all currently installed master, slave and client components
   * @method getCurrentMasterSlaveBlueprint
   */
  getCurrentMasterSlaveBlueprint: function () {
    return this.getComponentsBlueprint(App.HostComponent.find());
  },

  /**
   * Returns blueprint for all currently installed slave and client components
   */
  getCurrentSlaveBlueprint: function () {
    var self = this;
    var fullBlueprint = self.getCurrentMasterSlaveBlueprint();

    var components = App.StackServiceComponent.find().filter(function (c) {
      return c.get('isSlave') || c.get('isClient');
    }).mapProperty("componentName");

    return blueprintUtils.filterByComponents(fullBlueprint, components);
  }
});
