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

App.hostComponentStatusMapper = App.QuickDataMapper.create({

  config: {
    workStatus: 'currentState',
    staleConfigs: 'staleConfigs',
    passiveState: 'maintenanceState'
  },

  /**
   * @param {object} event
   */
  map: function (event) {
    event.hostComponents.forEach((componentState) => {
      const hostComponent = App.HostComponent.find(componentState.componentName + '_' + componentState.hostName);
      this.updatePropertiesByConfig(hostComponent, componentState, this.config);
      this.updateComponentsWithStaleConfigs(componentState);
      App.componentsStateMapper.updateComponentCountOnStateChange(componentState);
    });
  },

  /**
   * @param {object} componentState
   */
  updateComponentsWithStaleConfigs: function (componentState) {
    const staleConfigHostsMap = App.cache.staleConfigsComponentHosts;

    if (!Em.isNone(componentState.staleConfigs)) {
      const hosts = staleConfigHostsMap[componentState.componentName] || [];
      if (componentState.staleConfigs) {
        hosts.push(componentState.hostName);
      } else {
        hosts.removeObject(componentState.hostName);
      }
      App.componentsStateMapper.updateStaleConfigsHosts(componentState.componentName, hosts);
    }
  }
});
