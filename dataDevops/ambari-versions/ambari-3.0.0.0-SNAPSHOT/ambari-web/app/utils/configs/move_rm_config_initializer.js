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
 * Settings for <code>rm_ha_depended</code>-initializer
 * Used for configs with value equal to the host name (host where component is moved)
 *
 * @param {boolean} rmHaShouldBeEnabled
 * @returns {{type: string, rmHaShouldBeEnabled: boolean}}
 */
function getRmHaDependedConfig(rmHaShouldBeEnabled) {
  return {
    type: 'rm_ha_depended',
    rmHaShouldBeEnabled: Boolean(rmHaShouldBeEnabled)
  };
}

function getRmHaHawqConfig(rmHaShouldBeEnabled) {
  return {
    type: 'rm_ha_hawq',
    rmHaShouldBeEnabled: Boolean(rmHaShouldBeEnabled)
  };
}

/**
 * Initializer for configs which should be affected when Resource Manager is moved from one host to another
 * If Resource Manager HA-mode is already activated, several configs are also updated
 *
 * @instance MoveComponentConfigInitializerClass
 */
App.MoveRmConfigInitializer = App.MoveComponentConfigInitializerClass.create({

  initializerTypes: [
    {name: 'rm_ha_depended', method: '_initAsRmHaDepended'},
    {name: 'rm_ha_hawq', method: '_initAsRmHaHawq'}
  ],

  initializers: {
    'yarn.resourcemanager.hostname.{{suffix}}': getRmHaDependedConfig(true),
    'yarn.resourcemanager.webapp.address.{{suffix}}': getRmHaDependedConfig(true),
    'yarn.resourcemanager.webapp.https.address.{{suffix}}': getRmHaDependedConfig(true),
    'yarn.resourcemanager.resource-tracker.address.{{suffix}}': getRmHaDependedConfig(true),
    'yarn.resourcemanager.ha': getRmHaHawqConfig(true),
    'yarn.resourcemanager.scheduler.ha': getRmHaHawqConfig(true)
  },

  /**
   * Initializer for configs with value equal to the target hostName
   * and based on <code>App.isRMHaEnabled</code>
   * Value example: 'host1:port1'
   *
   * @param {configProperty} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {reassignComponentDependencies} dependencies
   * @param {object} initializer
   * @returns {object}
   * @private
   * @method _initAsRmHaDepended
   */
  _initAsRmHaDepended: function (configProperty, localDB, dependencies, initializer) {
    if (App.get('isRMHaEnabled') === initializer.rmHaShouldBeEnabled) {
      var value = Em.get(configProperty, 'value');
      var parts = value.split(':');
      parts[0] = dependencies.targetHostName;
      Em.set(configProperty, 'value', parts.join(':'));
    }
    return configProperty;
  },

  /**
   * This function to generate "rm1_host:port,rm2_host:port" for HAWQ configuration (RM HA specific)
   * configProperty = {
   *   filename: "yarn-client",
   *   name: "yarn.resourcemanager.ha",
   *   value: "ip-10-32-36-162.ore1.vpc.pivotal.io:8050"
   * }
   *
   * dependencies = {
   *   rm1: "ip-10-32-38-225.ore1.vpc.pivotal.io",
   *   rm2: "ip-10-32-38-162.ore1.vpc.pivotal.io",
   *   sourceHostName: "ip-10-32-36-162.ore1.vpc.pivotal.io",
   *   targetHostName: "ip-10-32-38-34.ore1.vpc.pivotal.io"
   * }
   *
   * dependencies is defined by ReassignMasterWizardStep4Controller._getRmAdditionalDependencies()
   * Move RM Wizard has 2 combobox in HA case, but it seems to gurantee users can change only one of them.
   * (Thus, there is only one pair of sourceHostName and targetHostName)
   *
   */
  _initAsRmHaHawq: function (configProperty, localDB, dependencies, initializer) {
    if (App.get('isRMHaEnabled') === initializer.rmHaShouldBeEnabled) {

      // which of rm1 and rm2 is changed?
      var rm1, rm2;
      if(dependencies.rm1 === dependencies.sourceHostName){
        rm1 = dependencies.targetHostName;
        rm2 = dependencies.rm2;
      }else{
        rm1 = dependencies.rm1;
        rm2 = dependencies.targetHostName;
      }

      // https://github.com/apache/hadoop/blob/release-2.7.1/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-api/src/main/java/org/apache/hadoop/yarn/conf/YarnConfiguration.java#L133-L163
      var DEFAULT_RM_PORT = 8032;
      var DEFAULT_RM_SCHEDULER_PORT = 8030;

      if(configProperty.name === "yarn.resourcemanager.ha"){
        var newValue = rm1 + ":" + DEFAULT_RM_PORT + "," + rm2 + ":" + DEFAULT_RM_PORT;
      }else{
        var newValue = rm1 + ":" + DEFAULT_RM_SCHEDULER_PORT + "," + rm2 + ":" + DEFAULT_RM_SCHEDULER_PORT;
      }

      Em.set(configProperty, 'value', newValue);
    }
    return configProperty;
  }

});
