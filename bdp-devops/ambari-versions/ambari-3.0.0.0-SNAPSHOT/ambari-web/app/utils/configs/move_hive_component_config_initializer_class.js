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
require('utils/configs/move_component_config_initializer_class');

/**
 * Common class for Initializers which are used when some HIVE's component is moved from one host to another
 *
 * @type {MoveComponentConfigInitializerClass}
 * @augments {MoveComponentConfigInitializerClass}
 */
App.MoveHiveComponentConfigInitializerClass = App.MoveComponentConfigInitializerClass.extend({

  initializerTypes: [
    {name: 'hive_hosts_with_components', method: '_initAsHostsWithComponentsConsideringMovedComponent'}
  ],

  /**
   * Initializer for configs with value equal to the list of hosts where some components exist
   * This list is affected by component's moving.
   * Example: movedComponent is <code>'Component1'</code>, list of needed components is <code>['Component1', 'Component2']</code>
   * So, when hosts for each needed component will be mapped, host where 'Component1' was before moving ('host1') will be skipped
   * and host where 'Component1' is moved will be added ('host2')
   * But, if 'Component2' exists of the 'host1', 'host1' will be added to the hosts list
   *
   * @param {configProperty} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {reassignComponentDependencies} dependencies
   * @param {object} initializer
   * @returns {object}
   * @private
   * @method _initAsHostsWithComponentsConsideringMovedComponent
   */
  _initAsHostsWithComponentsConsideringMovedComponent: function (configProperty, localDB, dependencies, initializer) {
    var allHosts = [];
    initializer.components.forEach(function(component) {
      var hosts = localDB.masterComponentHosts.filterProperty('component', component).mapProperty('hostName');
      if (component === initializer.movedComponent) {
        hosts = hosts.without(dependencies.sourceHostName);
        hosts.pushObject(dependencies.targetHostName);
      }
      allHosts = allHosts.concat(hosts);
    });
    Em.set(configProperty, 'value', allHosts.uniq().sort().join(','));
    return configProperty;
  }

});

App.MoveHiveComponentConfigInitializerClass.reopenClass({

  /**
   * Settings for <code>hive_hosts_with_components</code>-initializer
   * Used for configs with value equal to the hosts list where needed components exist.
   * This list is affected by component's moving
   *
   * @param {string|string[]} neededComponents
   * @param {string} movedComponent
   * @returns {{type: string, components: string[], movedComponent: string}}
   */
  getHostsWithComponentsConfig: function (neededComponents, movedComponent) {
    return {
      type: 'hive_hosts_with_components',
      components: Em.makeArray(neededComponents),
      movedComponent: movedComponent
    }
  }

});