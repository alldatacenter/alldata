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
var stringUtils = require('utils/string_utils');

/**
 * Mixin with preconfigured initializers that helps to build conditional execution
 * based on exit code from App.ConfigInitializerClass._initializerFlowCode.
 * Each control flow initializer should has attribute <b>isChecker: true</b>
 * Each handler should return exit code value based on App.ConfigInitializerClass._initializerFlowCode.
 *
 * There are few methods:
 * @see App.ConfigInitializerClass.flowNext
 * @see App.ConfigInitializerClass.flowSkipNext
 * @see App.ConfigInitializerClass.flowSkipAll
 *
 * For details and examples @see App.AddComponentConfigInitializer
 *
 * @mixin App.ControlFlowInitializerMixin
 */
App.ControlFlowInitializerMixin = Em.Mixin.create({

  initializerTypes: [
    {
      name: 'namenode_ha_enabled',
      method: '_initNameNodeHACheck'
    },
    {
      name: 'resourcemanager_ha_enabled',
      method: '_initResourceManagerHACheck'
    },
    {
      name: 'hdp_stack_version_checker',
      method: '_initHDPStackVersionCheck'
    }
  ],

  /**
   * Control flow initializer based on minimal stack version.
   *
   * @param  {string} minStackVersionNumber
   * @return {object}
   */
  getHDPStackVersionControl: function(minStackVersionNumber) {
    return { type: 'hdp_stack_version_checker', isChecker: true, stackVersion: minStackVersionNumber };
  },

  /**
   * getHDPStackVersionControl handler.
   * When stack version satisfies passed minStackVersionNumber computation process will continue.
   * If not all next computation will be skipped.
   *
   * @param  {configProperty} configProperty
   * @param  {topologyLocalDB} localDB
   * @param  {dependencies} dependencies
   * @param  {initializer} initializer
   * @return {number} _initializerFlowCode exit code
   */
  _initHDPStackVersionCheck: function(configProperty, localDB, dependencies, initializer) {
    return (stringUtils.compareVersions(App.get('currentStackVersionNumber'), initializer.stackVersion) > -1) ?
      this.flowNext() :
      this.flowSkipAll();
  },

  /**
   * Control flow initializer based on NameNode HA Status.
   *
   * @return {initializer}
   */
  getNameNodeHAControl: function() {
    return { type: 'namenode_ha_enabled', isChecker: true };
  },

  /**
   * getNameNodeHAControl handler.
   * When NameNode HA enabled next computation will be performed, either next will be skipped.
   *
   * @param  {configProperty} configProperty
   * @param  {topologyLocalDB} localDB
   * @param  {dependencies} dependencies
   * @param  {initializer} initializer
   * @return {number} _initializerFlowCode exit code
   */
  _initNameNodeHACheck: function(configProperty, localDB, dependencies) {
    return App.get('isHaEnabled') ? this.flowNext() : this.flowSkipNext();
  },

  /**
   * Control flow initializer based on ResourceManager HA Status.
   *
   * @return {initializer}
   */
  getResourceManagerHAControl: function(trueBranch, falseBranch) {
    return { type: 'resourcemanager_ha_enabled', isChecker: true };
  },

  /**
   * getResourceManagerHAControl handler.
   * When ResourceManager HA enabled next computation will be performed, either next will be skipped.
   *
   * @param  {configProperty} configProperty
   * @param  {topologyLocalDB} localDB
   * @param  {dependencies} dependencies
   * @param  {initializer} initializer
   * @return {number} _initializerFlowCode exit code
   */
  _initResourceManagerHACheck: function(configProperty, localDB, dependencies) {
    return App.get('isRMHaEnabled') ? this.flowNext() : this.flowSkipNext();
  }

});
