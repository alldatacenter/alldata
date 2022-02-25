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
require('utils/configs/config_initializer_class');

/**
 * Basic class for all Initializers used for configs which are affected by some component's moving from
 * one host to the another
 *
 * @type {ConfigInitializerClass}
 * @augments {ConfigInitializerClass}
 */
App.MoveComponentConfigInitializerClass = App.ConfigInitializerClass.extend({

  initializerTypes: [
    {name: 'hosts_with_component', method: '_initAsHostsWithComponentConsideringMoving'},
    {name: 'target_host', method: '_initAsTargetHost'}
  ],

  /**
   * @override
   * @param {object} settings
   */
  setup: function (settings) {
    this._updateInitializers(settings);
  },

  /**
   * @override
   */
  cleanup: function () {
    this._restoreInitializers();
  },

  /**
   * Initializer for configs with vlaue equal to the hostName where component will be moved (with port)
   *
   * @param {object} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {reassignComponentDependencies} dependencies
   * @param {object} initializer
   * @returns {object}
   * @private
   * @method _initAsTargetHost
   */
  _initAsTargetHost: function (configProperty, localDB, dependencies, initializer) {
    var hostName = dependencies.targetHostName;
    var port = initializer.port;
    Em.set(configProperty, 'value', hostName + ':' + port);
    return configProperty;
  },

  /**
   * Initializer for configs with value equal to the hosts list where needed component exists
   * Value considers component-moving, so <code>targetHostName</code> will be added to the list
   * and <code>sourceHostName</code> will be removed
   * Hosts are sorted by name and linked with ','
   * Value examples: 'host1', 'host1,host2,host3'
   *
   * @param {configProperty} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {reassignComponentDependencies} dependencies
   * @param {object} initializer
   * @returns {object}
   * @private
   * @method _initAsHostsWithComponentConsideringMoving
   */
  _initAsHostsWithComponentConsideringMoving: function (configProperty, localDB, dependencies, initializer) {
    var hosts = localDB.masterComponentHosts.filterProperty('component', initializer.component).mapProperty('hostName');
    hosts = hosts.without(dependencies.sourceHostName);
    hosts.pushObject(dependencies.targetHostName);
    Em.set(configProperty, 'value', hosts.uniq().sort().join(','));
    return configProperty;
  }

});

App.MoveComponentConfigInitializerClass.reopenClass({

  /**
   * Settings for <code>target_host</code>-initializer
   * Used for configs with value equal to the new host where component is moved
   * <code>port</code> is added to the end
   *
   * @param {number|string} port
   * @returns {{type: string, port: (number|string)}}
   */
  getTargetHostConfig: function (port) {
    port = port || '';
    return {
      type: 'target_host',
      port: port
    }
  },

  /**
   * Settings for <code>hosts_with_component</code>-initializer
   * Used for configs with value equal to the hosts where needed component exists
   * List of hosts considers moving component (source-host with be excluded, and target-host will be added)
   *
   * @param {string} component
   * @returns {{type: string, component: string}}
   */
  getHostsWithComponentConfig: function (component) {
    return {
      type: 'hosts_with_component',
      component: component
    }
  }

});