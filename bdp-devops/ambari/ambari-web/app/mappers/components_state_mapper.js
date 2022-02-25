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

App.componentsStateMapper = App.QuickDataMapper.create({

  model: App.Service,

  clientModel: App.ClientComponent,
  clientMap: {
    id: 'ServiceComponentInfo.component_name',
    service_id: 'ServiceComponentInfo.service_name',
    stack_info_id: 'ServiceComponentInfo.component_name',
    component_name: 'ServiceComponentInfo.component_name',
    display_name: 'ServiceComponentInfo.display_name',
    service_name: 'ServiceComponentInfo.service_name',
    installed_count: 'ServiceComponentInfo.installed_count',
    install_failed_count: 'ServiceComponentInfo.install_failed_count',
    init_count: 'ServiceComponentInfo.init_count',
    unknown_count: 'ServiceComponentInfo.unknown_count',
    started_count: 'ServiceComponentInfo.started_count',
    total_count: 'ServiceComponentInfo.total_count',
    host_names: 'host_names',
    stale_config_hosts: 'stale_config_hosts'
  },

  slaveModel: App.SlaveComponent,
  masterModel: App.MasterComponent,

  paths: {
    INSTALLED_PATH: 'ServiceComponentInfo.installed_count',
    INSTALL_FAILED_PATH: 'ServiceComponentInfo.install_failed_count',
    INIT_PATH: 'ServiceComponentInfo.init_count',
    UNKNOWN_PATH: 'ServiceComponentInfo.unknown_count',
    STARTED_PATH: 'ServiceComponentInfo.started_count',
    TOTAL_PATH: 'ServiceComponentInfo.total_count'
  },
  configMap: {
    'DATANODE': {
      data_nodes_started: 'STARTED_PATH',
      data_nodes_installed: 'INSTALLED_PATH',
      data_nodes_total: 'TOTAL_PATH'
    },
    'NFS_GATEWAY': {
      nfs_gateways_started: 'STARTED_PATH',
      nfs_gateways_installed: 'INSTALLED_PATH',
      nfs_gateways_total: 'TOTAL_PATH'
    },
    'NODEMANAGER': {
      node_managers_started: 'STARTED_PATH',
      node_managers_installed: 'INSTALLED_PATH',
      node_managers_total: 'TOTAL_PATH'
    },
    'HAWQSEGMENT': {
      hawq_segments_started: 'STARTED_PATH',
      hawq_segments_installed: 'INSTALLED_PATH',
      hawq_segments_total: 'TOTAL_PATH'
    },
    'PXF': {
      pxfs_started: 'STARTED_PATH',
      pxfs_installed: 'INSTALLED_PATH',
      pxfs_total: 'TOTAL_PATH'
    },
    'HBASE_REGIONSERVER': {
      region_servers_started: 'STARTED_PATH',
      region_servers_installed: 'INSTALLED_PATH',
      region_servers_total: 'TOTAL_PATH'
    },
    'PHOENIX_QUERY_SERVER': {
      phoenix_servers_started: 'STARTED_PATH',
      phoenix_servers_installed: 'INSTALLED_PATH',
      phoenix_servers_total: 'TOTAL_PATH'
    },
    'GANGLIA_MONITOR': {
      ganglia_monitors_started: 'STARTED_PATH',
      ganglia_monitors_installed: 'INSTALLED_PATH',
      ganglia_monitors_total: 'TOTAL_PATH'
    },
    'SUPERVISOR': {
      super_visors_started: 'STARTED_PATH',
      super_visors_installed: 'INSTALLED_PATH',
      super_visors_total: 'TOTAL_PATH'
    },
    'RANGER_TAGSYNC': {
      ranger_tagsyncs_started: 'STARTED_PATH',
      ranger_tagsyncs_installed: 'INSTALLED_PATH',
      ranger_tagsyncs_total: 'TOTAL_PATH'
    },
    'MAPREDUCE2_CLIENT': {
      map_reduce2_clients: 'INSTALLED_PATH'
    },
    'FLUME_HANDLER': {
      flume_handlers_total: 'TOTAL_PATH'
    }
  },
  /**
   * get formatted component config
   * @param componentName
   * @return {Object}
   */
  getComponentConfig: function (componentName) {
    const config = {};
    const isClient = App.StackServiceComponent.find(componentName).get('isClient');
    const defaultValue = isClient ? { installed_clients: 'TOTAL_PATH' } : {};
    const componentConfig = Em.getWithDefault(this.get('configMap'), componentName, defaultValue);
    const paths = this.get('paths');

    for (var property in componentConfig) {
      if (paths[componentConfig[property]]) {
        config[property] = paths[componentConfig[property]];
      }
    }
    return config;
  },
  /**
   * get service extended model if it has one
   * @param serviceName
   * @return {Object|null}
   */
  getExtendedModel: function (serviceName) {
    if (App[App.Service.extendedModel[serviceName]]) {
      return App[App.Service.extendedModel[serviceName]].find(serviceName);
    }
    return null;
  },

  map: function (json) {
    console.time('App.componentsStateMapper execution time');

    var clients = [];
    var slaves = [];
    var masters = [];
    var hasNewComponents = false;
    var staleConfigHostsMap = App.cache.staleConfigsComponentHosts;

    if (json.items) {
      if (!App.isEmptyObject(Em.getWithDefault(App, 'cache.services', {}))) {
        hasNewComponents = json.items.mapProperty('ServiceComponentInfo.total_count').reduce(Em.sum, 0) >
          App.cache.services.mapProperty('host_components.length').reduce(Em.sum, 0);
      }
      json.items.forEach(function (item) {
        var cacheService = App.cache['services'].findProperty('ServiceInfo.service_name', item.ServiceComponentInfo.service_name);

        item.stale_config_hosts = staleConfigHostsMap[item.ServiceComponentInfo.component_name] || [];
        if (item.ServiceComponentInfo.category === 'CLIENT') {
          item.host_names = item.host_components.mapProperty('HostRoles.host_name');
          clients.push(this.parseIt(item, this.clientMap));
        } else if (item.ServiceComponentInfo.category === 'SLAVE') {
          // for now map for slaves and clients are equal but it may vary in future.
          item.host_names = item.host_components.mapProperty('HostRoles.host_name');
          slaves.push(this.parseIt(item, this.clientMap));
        } else if (item.ServiceComponentInfo.category === 'MASTER') {
          item.host_names = item.host_components.mapProperty('HostRoles.host_name');
          masters.push(this.parseIt(item, this.clientMap));
        }
        if (cacheService) {
          cacheService.client_components = clients.filterProperty('service_name', cacheService.ServiceInfo.service_name).mapProperty('component_name');
          cacheService.slave_components = slaves.filterProperty('service_name', cacheService.ServiceInfo.service_name).mapProperty('component_name');
          cacheService.master_components = masters.filterProperty('service_name', cacheService.ServiceInfo.service_name).mapProperty('component_name');
        }

        this.mapExtendedModelComponents(item);
      }, this);
    }
    App.store.safeLoadMany(this.clientModel, clients);
    App.store.safeLoadMany(this.slaveModel, slaves);
    App.store.safeLoadMany(this.masterModel, masters);

    if (hasNewComponents) {
      App.get('router.clusterController').triggerQuickLinksUpdate();
    }

    console.timeEnd('App.componentsStateMapper execution time');
  },

  /**
   *
   * @param {object} item
   */
  mapExtendedModelComponents: function(item) {
    const serviceName = item.ServiceComponentInfo.service_name;
    const service = App.Service.find(serviceName);
    const cacheService = App.cache['services'].findProperty('ServiceInfo.service_name', serviceName);
    const extendedModel = this.getExtendedModel(serviceName);
    const parsedItem = this.parseIt(item, this.getComponentConfig(item.ServiceComponentInfo.component_name));

    if (cacheService) {
      for (let i in parsedItem) {
        if (service.get('isLoaded')) {
          cacheService[i] = parsedItem[i];
          service.set(stringUtils.underScoreToCamelCase(i), parsedItem[i]);
          if (extendedModel && extendedModel.get('isLoaded')) {
            extendedModel.set(stringUtils.underScoreToCamelCase(i), parsedItem[i]);
          }
        }
      }
    }
  },

  /**
   *
   * @param {string} componentName
   * @param {Array} hosts
   */
  updateStaleConfigsHosts: function(componentName, hosts) {
    const staleConfigHostsMap = App.cache.staleConfigsComponentHosts;
    const model = App.ClientComponent.getModelByComponentName(componentName);

    staleConfigHostsMap[componentName] = hosts;
    if (model) {
      model.set('staleConfigHosts', hosts);
    }
  },

  /**
   * used to synchronize updates from websocket with updates from REST API
   * @param {App.ClientComponent} model
   * @returns {{ServiceComponentInfo: {component_name, service_name, installed_count, install_failed_count, init_count, unknown_count, started_count, total_count}}}
   */
  componentStateToJSON: function(model) {
    return {
      ServiceComponentInfo: {
        component_name: model.get('componentName'),
        service_name: model.get('service.serviceName'),
        installed_count: model.get('installedCount'),
        install_failed_count: model.get('installFailedCount'),
        init_count: model.get('initCount'),
        unknown_count: model.get('unknownCount'),
        started_count: model.get('startedCount'),
        total_count: model.get('totalCount')
      }
    };
  },

  /**
   * @param {object} componentState
   */
  updateComponentCountOnStateChange: function(componentState) {
    const model = App.ClientComponent.getModelByComponentName(componentState.componentName);
    const acceptedStates = ['STARTED', 'INSTALLED', 'INIT', 'UNKNOWN', 'INSTALL_FAILED'];

    if (model && model.get('isLoaded') && componentState.currentState) {
      if (acceptedStates.contains(componentState.previousState)) {
        const previousStateProp = this.statusToProperty(componentState.previousState);
        model.set(previousStateProp, model.get(previousStateProp) - 1);
      }

      if (acceptedStates.contains(componentState.currentState)) {
        const currentStateProp = this.statusToProperty(componentState.currentState);
        model.set(currentStateProp, model.get(currentStateProp) + 1);
      }
      this.mapExtendedModelComponents(this.componentStateToJSON(model));
    }
  },

  /**
   * @param {object} componentState
   */
  updateComponentCountOnDelete: function(componentState) {
    const model = App.ClientComponent.getModelByComponentName(componentState.componentName);
    const acceptedStates = ['STARTED', 'INSTALLED', 'INIT', 'UNKNOWN', 'INSTALL_FAILED'];

    if (model) {
      if (acceptedStates.contains(componentState.lastComponentState)) {
        const currentStateProp = this.statusToProperty(componentState.lastComponentState);
        model.set(currentStateProp, model.get(currentStateProp) - 1);
      }
      model.set('totalCount', model.get('totalCount') - 1);
      this.mapExtendedModelComponents(this.componentStateToJSON(model));
    }
  },

  /**
   * @param {object} componentState
   */
  updateComponentCountOnCreate: function(componentState) {
    const model = App.ClientComponent.getModelByComponentName(componentState.componentName);

    if (model && model.get('isLoaded')) {
      model.set('initCount', model.get('initCount') + 1);
      model.set('totalCount', model.get('totalCount') + 1);
      this.mapExtendedModelComponents(this.componentStateToJSON(model));
    }
  },

  /**
   *
   * @param {string} status
   * @returns {string}
   */
  statusToProperty: function(status) {
    return stringUtils.underScoreToCamelCase(status.toLowerCase() + "_count");
  }
});
