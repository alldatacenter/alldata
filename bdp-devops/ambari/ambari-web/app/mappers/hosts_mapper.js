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

App.hostsMapper = App.QuickDataMapper.create({

  model: App.Host,
  config: {
    id: 'Hosts.host_name',
    host_name: 'Hosts.host_name',
    public_host_name: 'Hosts.public_host_name',
    cluster_id: 'cluster_id',// Hosts.cluster_name
    rack: 'Hosts.rack_info',
    host_components_key: 'host_components',
    host_components_type: 'array',
    host_components: {
      item: 'id'
    },
    alerts_summary: 'alerts_summary',
    critical_warning_alerts_count: 'critical_warning_alerts_count',
    cpu: 'Hosts.cpu_count',
    cpu_physical: 'Hosts.ph_cpu_count',
    memory: 'Hosts.total_mem',
    has_jce_policy: "Hosts.last_agent_env.hasUnlimitedJcePolicy",
    disk_info: 'Hosts.disk_info',
    disk_total: 'metrics.disk.disk_total',
    disk_free: 'metrics.disk.disk_free',
    health_status: 'Hosts.host_status',
    state: 'Hosts.host_state',
    load_one: 'metrics.load.load_one',
    load_five: 'metrics.load.load_five',
    load_fifteen: 'metrics.load.load_fifteen',
    cpu_system: 'metrics.cpu.cpu_system',
    cpu_user: 'metrics.cpu.cpu_user',
    mem_total: 'metrics.memory.mem_total',
    mem_free: 'metrics.memory.mem_free',
    last_heart_beat_time: "Hosts.last_heartbeat_time",
    os_arch: 'Hosts.os_arch',
    os_type: 'Hosts.os_type',
    ip: 'Hosts.ip',
    passive_state: 'Hosts.maintenance_state',
    index: 'index'
  },
  hostComponentConfig: {
    component_name: 'HostRoles.component_name',
    display_name: 'HostRoles.display_name',
    service_id: 'HostRoles.service_name',
    passive_state: 'HostRoles.maintenance_state',
    work_status: 'HostRoles.state',
    stale_configs: 'HostRoles.stale_configs',
    host_name: 'host_name',
    public_host_name: 'public_host_name',
    admin_state: 'HostRoles.desired_admin_state',
    cluster_id_value: 'metrics.dfs.namenode.ClusterId'
  },
  stackVersionConfig: {
    id: 'HostStackVersions.id',
    stack: 'HostStackVersions.stack',
    repo_id: 'repository_versions[0].RepositoryVersions.id',
    repo_version: 'repository_versions[0].RepositoryVersions.repository_version',
    display_name: 'repository_versions[0].RepositoryVersions.display_name',
    version: 'HostStackVersions.version',
    status: 'HostStackVersions.state',
    host_name: 'host_name',
    host_id: 'host_name',
    is_visible: 'is_visible'
  },
  hostComponentLogsConfig: {
    name: 'logging.name',
    service_name: 'HostRoles.service_name',
    host_name: 'HostRoles.host_name',
    log_file_names_type: 'array',
    log_file_names_key: 'logging.logs',
    log_file_names: {
      item: 'name'
    }
  },
  map: function (json, returnMapped) {
    returnMapped = !!returnMapped;
    console.time('App.hostsMapper execution time');
    if (json.items) {
      var hostsWithFullInfo = [];
      var hostIds = {};
      var components = [];
      var stackVersions = [];
      var componentsIdMap = {};
      var cacheServices = App.cache['services'];
      var currentServiceComponentsMap = this.buildServiceComponentMap(cacheServices);
      var newHostComponentsMap = {};
      var selectedHosts = App.db.getSelectedHosts();
      var clusterName = App.get('clusterName');
      var advancedHostComponents = [];
      var hostComponentLogs = [];

      // Create a map for quick access on existing hosts
      var hosts = App.Host.find().toArray();
      var hostsMap = {};
      for (var p = 0; p < hosts.length; p++) {
        hostsMap[hosts[p].get('hostName')] = hosts[p];
      }

      // Use normal for loop instead of foreach to enhance performance
      for (var index = 0; index < json.items.length; index++) {
        var item = json.items[index];
        var notStartedComponents = [];
        var componentsInPassiveState = [];
        var componentsWithStaleConfigs = [];

        item.host_components = item.host_components || [];
        for (var i = 0; i < item.host_components.length; i++){
          var host_component = item.host_components[i];
          var id = host_component.HostRoles.component_name + "_" + item.Hosts.host_name;
          var component = this.parseIt(host_component, this.hostComponentConfig);
          var serviceName = host_component.HostRoles.service_name;

          host_component.id = id;
          component.id = id;
          component.host_id = item.Hosts.host_name;
          component.host_name = item.Hosts.host_name;
          component.public_host_name = item.Hosts.public_host_name;
          components.push(component);
          componentsIdMap[id] = component;
          if (!newHostComponentsMap[serviceName]) {
            newHostComponentsMap[serviceName] = [];
          }
          if (!currentServiceComponentsMap[serviceName]) {
            currentServiceComponentsMap[serviceName] = [];
          }
          if (!currentServiceComponentsMap[serviceName][id]) {
            newHostComponentsMap[serviceName].push(id);
          }
          if (App.serviceMetricsMapper.get('ADVANCED_COMPONENTS').contains(host_component.HostRoles.component_name)) {
            advancedHostComponents.push(id);
          }
          if (component.work_status !== App.HostComponentStatus.started) {
            notStartedComponents.push(id);
          }
          if (component.stale_configs) {
            componentsWithStaleConfigs.push(id);
          }
          if (component.passive_state !== 'OFF') {
            componentsInPassiveState.push(id);
          }
          if (host_component.hasOwnProperty('logging')) {
            var logParsed = this.parseIt(host_component, this.hostComponentLogsConfig);
            logParsed.id = logParsed.host_name + '_' + logParsed.name;
            logParsed.host_component_id = host_component.id;
            component.component_logs_id = logParsed.id;
            hostComponentLogs.push(logParsed);
          }
        }

        var currentVersion = item.stack_versions.findProperty('HostStackVersions.state', 'CURRENT');
        var currentVersionNumber = currentVersion && currentVersion.repository_versions
          ? Em.get(currentVersion.repository_versions[0], 'RepositoryVersions.repository_version') : '';
        for (var j = 0; j < item.stack_versions.length; j++) {
          var stackVersion = item.stack_versions[j];
          var versionNumber = Em.get(stackVersion.repository_versions[0], 'RepositoryVersions.repository_version');
          var isDifferentStack = currentVersion && (stackVersion.HostStackVersions.stack !== currentVersion.HostStackVersions.stack);
          var isCompatible = App.RepositoryVersion.find(Em.get(stackVersion.repository_versions[0], 'RepositoryVersions.id')).get('isCompatible');
          stackVersion.host_name = item.Hosts.host_name;
          if (isDifferentStack && !isCompatible) {
            stackVersion.is_visible = false;
          } else {
            stackVersion.is_visible = isDifferentStack
            || (App.get('supports.displayOlderVersions') || stringUtils.compareVersions(versionNumber, currentVersionNumber) >= 0)
            || !currentVersionNumber;
          }
          stackVersions.push(this.parseIt(stackVersion, this.stackVersionConfig));
        }

        item.cluster_id = clusterName;
        var existingHost = hostsMap[item.Hosts.host_name];
        // There is no need to override existing index in host detail view since old model(already have indexes) will not be cleared.
        item.index = (existingHost && !json.itemTotal)? existingHost.get('index'): index;

        this.config = $.extend(this.config, {
          stack_versions_key: 'stack_versions',
          stack_versions_type: 'array',
          stack_versions: {
            item: 'HostStackVersions.id'
          }
        });
        var parsedItem = this.parseIt(item, this.config);

        parsedItem.selected = selectedHosts.contains(parsedItem.host_name);
        parsedItem.not_started_components = notStartedComponents;
        parsedItem.components_in_passive_state = componentsInPassiveState;
        parsedItem.components_with_stale_configs = componentsWithStaleConfigs;
        parsedItem.is_filtered = true;

        hostIds[item.Hosts.host_name] = parsedItem;

        hostsWithFullInfo.push(parsedItem);
      }

      if(returnMapped){
        return hostsWithFullInfo;
      }


      for (var k = 0; k < advancedHostComponents.length; k++) {
        var key = advancedHostComponents[k];
        if (componentsIdMap[key]) {
          var existingRecord = App.HostComponent.find(key);
          componentsIdMap[key].display_name_advanced = existingRecord.get('displayNameAdvanced');
          componentsIdMap[key].ha_name_space = existingRecord.get('haNameSpace');
        }
      }

      //"itemTotal" present only for Hosts page request
      if (!Em.isNone(json.itemTotal)) {
        App.Host.find().setEach('isFiltered', false);
        App.Host.find().clear();
        //App.HostComponent.find contains master components which requested across the app hence it should not be cleared
      }
      App.store.safeLoadMany(App.HostStackVersion, stackVersions);
      App.store.safeLoadMany(App.HostComponentLog, hostComponentLogs);
      App.store.safeLoadMany(App.HostComponent, components);
      App.store.safeLoadMany(App.Host, hostsWithFullInfo);
      var itemTotal = parseInt(json.itemTotal);
      if (!isNaN(itemTotal)) {
        App.router.set('mainHostController.filteredCount', itemTotal);
      }
      //bind host-components with service records
      this.addNewHostComponents(newHostComponentsMap, cacheServices);
    }
    console.timeEnd('App.hostsMapper execution time');
  },

  /**

   * set metric fields of hosts
   * @param {object} data
   */
  setMetrics: function (data) {
    var hosts = this.get('model').find();
    for (var i = 0; i < hosts.content.length; i++) {
      var host = hosts.objectAt(i);
      var hostMetrics = data.items.findProperty('Hosts.host_name', host.get('hostName'));
      host.setProperties({
        diskTotal: Em.get(hostMetrics, 'metrics.disk.disk_total'),
        diskFree: Em.get(hostMetrics, 'metrics.disk.disk_free'),
        loadOne: Em.get(hostMetrics, 'metrics.load.load_one')
      });
    }
  },

  /**
   * build map that include loaded host-components to avoid duplicate loading
   * @param cacheServices
   * @return {Object}
   */
  buildServiceComponentMap: function (cacheServices) {
    var loadedServiceComponentsMap = {};

    cacheServices.forEach(function (cacheService) {
      var componentsMap = {};

      cacheService.host_components.forEach(function (componentId) {
        componentsMap[componentId] = true;
      });
      loadedServiceComponentsMap[cacheService.ServiceInfo.service_name] = componentsMap;
    });
    return loadedServiceComponentsMap;
  },

  /**
   * add only new host-components to every service
   * to update service - host-component relations in model
   * @param {object} newHostComponentsMap
   * @param {Array} cacheServices
   * @return {boolean}
   */
  addNewHostComponents: function (newHostComponentsMap, cacheServices) {
    if (!newHostComponentsMap || !cacheServices) return false;
    cacheServices.forEach(function (service) {
      if (newHostComponentsMap[service.ServiceInfo.service_name]) {
        newHostComponentsMap[service.ServiceInfo.service_name].forEach(function (componentId) {
          service.host_components.push(componentId)
        });
      }
    }, this);
    return true;
  }
});
