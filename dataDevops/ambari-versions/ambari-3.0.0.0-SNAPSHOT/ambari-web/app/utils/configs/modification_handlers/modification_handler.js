/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

/**
 * Config modification handler for a given service.
 * 
 * This handler should take care of scenarios that arise from modification of a
 * config. Scenarios like updating dependency configs, or doing any special
 * processing.
 */
App.ServiceConfigModificationHandler = Em.Object.extend({

  /**
   * Service whose configs this handler will react to.
   */
  serviceId : DS.attr('string'),

  /**
   * Given a changed config, provide an array of changes to dependent configs
   * across all services. Dependent configs can be created, updated and deleted.
   * 
   *  Example: Here, setting 'hdfs_user' updates 'dfs.permissions.superusergroup'. 
   *  [
   *   {
   *     serviceName: "HDFS",
   *     sourceServiceName: "MISC",  // Cross service configs
   *     propertyName: "dfs.permissions.superusergroup",
   *     propertyDisplayName: "dfs.permissions.superusergroup",
   *     filename: 'hdfs-site',
   *     remove: false,
   *     curValue: "hdfs",
   *     newValue: "my_hdfs",
   *     changedPropertyName: "hdfs_user"
   *   }
   *  ]
   *  
   * @param changedConfig
   * @param selectedServices
   * @param allConfigs
   * @param securityEnabled
   * @return Array of dependent config updates
   */
  getDependentConfigChanges : function(changedConfig, selectedServices, allConfigs, securityEnabled) {
    return [];
  },

  getConfig: function(allConfigs, configName, configFilename, configServiceName) {
    return allConfigs.findProperty('serviceName', configServiceName).get('configs').find(function(config) {
      return configName == config.get('name') && (configFilename == null || configFilename == config.get('filename'));
    });
  }

});