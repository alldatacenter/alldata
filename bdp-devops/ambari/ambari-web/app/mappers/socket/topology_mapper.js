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
var stringUtils = require('utils/string_utils');

App.topologyMapper = App.QuickDataMapper.create({

  /**
   *
   * @param {object} event
   */
  map: function(event) {
    const hosts = event.clusters[App.get('clusterId')].hosts;

    if (event.clusters[App.get('clusterId')].components) {
      this.applyComponentTopologyChanges(event.clusters[App.get('clusterId')].components, event.eventType);
    }
    if (hosts) {
      const hostNames = hosts.filterProperty('hostName').mapProperty('hostName');
      if (event.eventType === 'DELETE') {
        App.get('allHostNames').removeObjects(hostNames);
      } else if (event.eventType === 'UPDATE') {
        App.get('allHostNames').pushObjects(hostNames);
      }
      /**
       * Since event message doesn't contain entire data of host and UI model doesn't contain all hosts of cluster,
       * App.Host model should be updated only through <code>updateHost</code> request.
       */
      App.router.get('updateController').updateHost(Em.K, null, true);
    }
  },

  /**
   *
   * @param {Array} components
   * @param {string} eventType
   */
  applyComponentTopologyChanges: function(components, eventType) {
    components.forEach((component) => {
      component.hostNames.forEach((hostName, index) => {
        if (eventType === 'UPDATE' && !component.commandParams.version) {
          this.addServiceIfNew(component.serviceName);
          this.createHostComponent(component, hostName, component.publicHostNames[index]);
          App.componentsStateMapper.updateComponentCountOnCreate(component);
        } else if (eventType === 'DELETE') {
          this.deleteHostComponent(component, hostName);
          this.deleteServiceIfHasNoComponents(component.serviceName);
          App.componentsStateMapper.updateComponentCountOnDelete(component);
        }
      });
    });
  },

  /**
   *
   * @param {string} serviceName
   */
  addServiceIfNew: function(serviceName) {
    if (!App.Service.find(serviceName).get('isLoaded')) {
      App.store.safeLoad(App.Service, {
        id: serviceName,
        service_name: serviceName,
        work_status: 'INIT',
        passive_state: 'OFF',
        host_components: []
      });
    }
  },

  /**
   *
   * @param {string} serviceName
   */
  deleteServiceIfHasNoComponents: function(serviceName) {
    if (App.Service.find(serviceName).get('isLoaded') &&
        App.Service.find(serviceName).get('hostComponents.length') === 0) {
      this.deleteRecord(App.Service.find(serviceName));
    }
  },

  /**
   *
   * @param {object} component
   * @param {string} hostName
   */
  deleteHostComponent: function(component, hostName) {
    const id = App.HostComponent.getId(component.componentName, hostName);
    if (!App.HostComponent.find(id).get('isLoaded')) {
      //App.HostComponent does not contain all host-components of cluster
      return;
    }
    this.deleteRecord(App.HostComponent.find(id));
    const host = App.Host.find(hostName);
    this.updateHostComponentsOfHost(host, host.get('hostComponents').rejectProperty('id', id).mapProperty('id'));

    const service = App.Service.find(component.serviceName);
    this.updateHostComponentsOfService(service, service.get('hostComponents').rejectProperty('id', id).mapProperty('id'));
  },

  /**
   *
   * @param {object} component
   * @param {string} hostName
   * @param {string} publicHostName
   */
  createHostComponent: function(component, hostName, publicHostName) {
    const id = App.HostComponent.getId(component.componentName, hostName);
    const host = App.Host.find(hostName);
    const service = App.Service.find(component.serviceName);

    App.store.safeLoad(App.HostComponent, {
      id: id,
      host_id: hostName,
      display_name: component.displayName,
      service_id: component.serviceName,
      host_name: hostName,
      passive_state: 'OFF',
      work_status: 'INIT',
      public_host_name: publicHostName,
      component_name: component.componentName
    });

    if (host.get('isLoaded')) {
      this.updateHostComponentsOfHost(host, host.get('hostComponents').mapProperty('id').concat(id));
    }
    this.updateHostComponentsOfService(service, service.get('hostComponents').mapProperty('id').concat(id));
  },

  /**
   *
   * @param {Em.Object} host
   * @param {Array} hostComponents
   */
  updateHostComponentsOfHost: function(host, hostComponents) {
    const updatedHost = {};
    for (let i in App.hostsMapper.config) {
      if (host.get(stringUtils.underScoreToCamelCase(i)) !== undefined) {
        updatedHost[i] = host.get(stringUtils.underScoreToCamelCase(i));
      }
    }
    updatedHost.host_components = hostComponents;
    App.store.safeLoad(App.Host, updatedHost);
  },

  /**
   *
   * @param {Em.Object} service
   * @param {Array} hostComponents
   */
  updateHostComponentsOfService: function(service, hostComponents) {
    const cacheService = App.cache['services'].findProperty('ServiceInfo.service_name', service.get('serviceName'));
    if (cacheService) {
      cacheService.host_components = hostComponents;
    }
  }
});
