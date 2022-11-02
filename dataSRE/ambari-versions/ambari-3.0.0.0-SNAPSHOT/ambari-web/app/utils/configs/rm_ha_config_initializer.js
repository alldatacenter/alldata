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
require('utils/configs/hosts_based_initializer_mixin');

/**
 * Settings for <code>rm_hosts_with_port</code> initializer
 * Used for configs that have to be updated with Yarn Resourcemanager HA hosts
 * addresses and a port to the <code>rmhost1:port,rmhost2:port</code>
 *
 * @param {int} port
 * @returns {{type: string, port: *}}
 */
function getRmHaHostsWithPort(port) {
  return {
    type: 'rm_hosts_with_port',
    port: port
  };
}

/**
 * Initializer for configs that are updated when Resource Manager HA-mode is activated
 *
 * @class {RmHaConfigInitializer}
 */
App.RmHaConfigInitializer = App.HaConfigInitializerClass.create(App.HostsBasedInitializerMixin, {

  initializers: function () {
    return {
      'yarn.resourcemanager.hostname.rm1': this.getHostWithPortConfig('RESOURCEMANAGER', true, '', '', ''),
      'yarn.resourcemanager.hostname.rm2': this.getHostWithPortConfig('RESOURCEMANAGER', false,'', '', ''),
      'yarn.resourcemanager.zk-address': this.getHostsWithPortConfig('ZOOKEEPER_SERVER', '', '', ',', 'zkClientPort', true),
      'yarn.resourcemanager.resource-tracker.address.rm1': this.getHostWithPortConfig('RESOURCEMANAGER', true, '', '', 'trackerAddressPort', true),
      'yarn.resourcemanager.resource-tracker.address.rm2': this.getHostWithPortConfig('RESOURCEMANAGER', false, '', '', 'trackerAddressPort', true),
      'yarn.resourcemanager.webapp.address.rm1': this.getHostWithPortConfig('RESOURCEMANAGER', true, '', '', 'webAddressPort', true),
      'yarn.resourcemanager.webapp.address.rm2': this.getHostWithPortConfig('RESOURCEMANAGER', false, '', '', 'webAddressPort', true),
      'yarn.resourcemanager.webapp.https.address.rm1': this.getHostWithPortConfig('RESOURCEMANAGER', true, '', '', 'httpsWebAddressPort', true),
      'yarn.resourcemanager.webapp.https.address.rm2': this.getHostWithPortConfig('RESOURCEMANAGER', false, '', '', 'httpsWebAddressPort', true),
      'yarn.resourcemanager.ha': getRmHaHostsWithPort(8032),
      'yarn.resourcemanager.scheduler.ha': getRmHaHostsWithPort(8030),
      'hadoop.proxyuser.{{yarnUser}}.hosts': this.getComponentsHostsConfig(['RESOURCEMANAGER'])
    };
  }.property(),

  initializerTypes: [
    {name: 'rm_hosts_with_port', method: '_initRmHaHostsWithPort'}
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
   * Initializer for configs that should be updated with yarn resourcemanager ha host addresses with port
   *
   * @param {configProperty} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {HaConfigDependencies} dependencies
   * @param {object} initializer
   * @returns {object}
   * @private
   * @method _initRmHaHostsWithPort
   */
  _initRmHaHostsWithPort: function (configProperty, localDB, dependencies, initializer) {
    var rmHosts = localDB.masterComponentHosts.filterProperty('component', 'RESOURCEMANAGER').getEach('hostName');
    for (var rmHost in rmHosts) {
      rmHosts[rmHost] = rmHosts[rmHost] + ":" + initializer.port;
    }
    var value = rmHosts.join(',');
    Em.setProperties(configProperty, {
      'value': value,
      'recommendedValue': value
    });
   return configProperty;
  }

});
