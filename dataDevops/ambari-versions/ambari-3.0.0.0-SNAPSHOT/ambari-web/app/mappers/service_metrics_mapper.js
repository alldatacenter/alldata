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
var misc = require('utils/misc');
var stringUtils = require('utils/string_utils');
var dateUtils = require('utils/date/date');
var previousMasterComponentIds = [];

App.serviceMetricsMapper = App.QuickDataMapper.create({

  model: App.Service,
  config: {
    id: 'ServiceInfo.service_name',
    service_name: 'ServiceInfo.service_name',
    work_status: 'ServiceInfo.state',
    passive_state: 'ServiceInfo.passive_state',
    $rand: Math.random(),
    $alerts: [1, 2, 3],
    host_components: 'host_components',
    tool_tip_content: 'tool_tip_content',
    installed_clients: 'installed_clients',
    client_components: 'client_components',
    slave_components: 'slave_components',
    master_components: 'master_components',
    desired_repository_version_id: 'ServiceInfo.desired_repository_version_id'
  },
  hdfsConfig: {
    version: 'nameNodeComponent.host_components[0].metrics.dfs.namenode.Version',
    active_name_nodes: 'active_name_nodes',
    standby_name_nodes: 'standby_name_nodes',
    journal_nodes: 'journal_nodes',
    name_node_id: 'name_node_id',
    sname_node_id: 'sname_node_id',
    metrics_not_available: 'metrics_not_available',
    name_node_start_time_values: 'name_node_start_time_values',
    jvm_memory_heap_used_values: 'jvm_memory_heap_used_values',
    jvm_memory_heap_max_values: 'jvm_memory_heap_max_values',
    live_data_nodes: 'live_data_nodes',
    dead_data_nodes: 'dead_data_nodes',
    decommission_data_nodes: 'decommission_data_nodes',
    capacity_used: 'nameNodeComponent.host_components[0].metrics.dfs.FSNamesystem.CapacityUsed',
    capacity_total: 'nameNodeComponent.host_components[0].metrics.dfs.FSNamesystem.CapacityTotal',
    capacity_remaining: 'nameNodeComponent.host_components[0].metrics.dfs.FSNamesystem.CapacityRemaining',
    capacity_non_dfs_used: 'nameNodeComponent.host_components[0].metrics.dfs.FSNamesystem.CapacityNonDFSUsed',
    dfs_total_blocks_values: 'dfs_total_blocks_values',
    dfs_corrupt_blocks_values: 'dfs_corrupt_blocks_values',
    dfs_missing_blocks_values: 'dfs_missing_blocks_values',
    dfs_under_replicated_blocks_values: 'dfs_under_replicated_blocks_values',
    dfs_total_files_values: 'dfs_total_files_values',
    work_status_values: 'work_status_values',
    upgrade_status_values: 'upgrade_status_values',
    safe_mode_status_values: 'safe_mode_status_values',
    name_node_rpc_values: 'name_node_rpc_values',
    data_nodes_started: 'data_nodes_started',
    data_nodes_installed: 'data_nodes_installed',
    data_nodes_total: 'data_nodes_total',
    nfs_gateways_started: 'nfs_gateways_started',
    nfs_gateways_installed: 'nfs_gateways_installed',
    nfs_gateways_total: 'nfs_gateways_total'
  },
  activeNameNodeConfig: {
    name_node_start_time_values: 'metrics.runtime.StartTime',
    jvm_memory_heap_used_values: 'metrics.jvm.HeapMemoryUsed',
    jvm_memory_heap_max_values: 'metrics.jvm.HeapMemoryMax',
    dfs_total_blocks_values: 'metrics.dfs.FSNamesystem.BlocksTotal',
    dfs_corrupt_blocks_values: 'metrics.dfs.FSNamesystem.CorruptBlocks',
    dfs_missing_blocks_values: 'metrics.dfs.FSNamesystem.MissingBlocks',
    dfs_under_replicated_blocks_values: 'metrics.dfs.FSNamesystem.UnderReplicatedBlocks',
    dfs_total_files_values: 'metrics.dfs.namenode.TotalFiles',
    work_status_values: 'HostRoles.state',
    upgrade_status_values: 'metrics.dfs.namenode.UpgradeFinalized',
    safe_mode_status_values: 'metrics.dfs.namenode.Safemode',
    name_node_rpc_values: 'metrics.rpc.client.RpcQueueTime_avg_time',
  },
  onefsConfig: {
    metrics_not_available: 'metrics_not_available',
    name_node_start_time: 'metrics.runtime.StartTime',
    capacity_used: 'metrics.dfs.FSNamesystem.CapacityUsed',
    capacity_total: 'metrics.dfs.FSNamesystem.CapacityTotal',
    capacity_remaining: 'metrics.dfs.FSNamesystem.CapacityRemaining',
    dfs_corrupt_blocks: 'metrics.dfs.FSNamesystem.CorruptBlocks',
    dfs_under_replicated_blocks: 'metrics.dfs.FSNamesystem.UnderReplicatedBlocks',
    dfs_missing_blocks: 'metrics.dfs.FSNamesystem.MissingBlocks',
    live_data_nodes: 'live_data_nodes',
    dead_data_nodes: 'dead_data_nodes',
    decommission_data_nodes: 'decommission_data_nodes',
    dfs_total_files: 'metrics.dfs.namenode.TotalFiles',
    jvm_memory_heap_used: 'metrics.jvm.memHeapUsedM',
    jvm_memory_heap_max: 'metrics.jvm.memHeapCommittedM',
    upgrade_status: 'metrics.dfs.namenode.UpgradeFinalized'
  },
  yarnConfig: {
    resource_manager_start_time: 'resourceManagerComponent.host_components[0].metrics.runtime.StartTime',
    jvm_memory_heap_used: 'resourceManagerComponent.host_components[0].metrics.jvm.HeapMemoryUsed',
    jvm_memory_heap_max: 'resourceManagerComponent.host_components[0].metrics.jvm.HeapMemoryMax',
    containers_allocated: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.AllocatedContainers',
    containers_pending: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.PendingContainers',
    containers_reserved: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.ReservedContainers',
    apps_submitted: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.AppsSubmitted',
    apps_running: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.AppsRunning',
    apps_pending: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.AppsPending',
    apps_completed: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.AppsCompleted',
    apps_killed: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.AppsKilled',
    apps_failed: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.AppsFailed',
    node_managers_count_active: 'resourceManagerComponent.host_components[0].metrics.yarn.ClusterMetrics.NumActiveNMs',
    node_managers_count_lost: 'resourceManagerComponent.host_components[0].metrics.yarn.ClusterMetrics.NumLostNMs',
    node_managers_count_unhealthy: 'resourceManagerComponent.host_components[0].metrics.yarn.ClusterMetrics.NumUnhealthyNMs',
    node_managers_count_rebooted: 'resourceManagerComponent.host_components[0].metrics.yarn.ClusterMetrics.NumRebootedNMs',
    node_managers_count_decommissioned: 'resourceManagerComponent.host_components[0].metrics.yarn.ClusterMetrics.NumDecommissionedNMs',
    allocated_memory: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.AllocatedMB',
    available_memory: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.AvailableMB',
    reserved_memory: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.ReservedMB',
    queue: 'resourceManagerComponent.queue',
    node_managers_started: 'node_managers_started',
    node_managers_installed: 'node_managers_installed',
    node_managers_total: 'node_managers_total',
    app_timeline_server_id: 'app_timeline_server_id',
    resource_manager_id: 'resource_manager_id',
    active_resource_manager_id: 'active_resource_manager_id'
  },
  mapReduce2Config: {
    map_reduce2_clients: 'map_reduce2_clients',
    job_history_server_id: 'job_history_server_id'
  },
  hbaseConfig: {
    master_start_time: 'masterComponent.host_components[0].metrics.hbase.master.MasterStartTime',
    master_active_time: 'masterComponent.host_components[0].metrics.hbase.master.MasterActiveTime',
    master_id: 'master_id',
    average_load: 'masterComponent.host_components[0].metrics.hbase.master.AverageLoad',
    heap_memory_used: 'masterComponent.host_components[0].metrics.jvm.HeapMemoryUsed',
    heap_memory_max: 'masterComponent.host_components[0].metrics.jvm.HeapMemoryMax',
    regions_in_transition: 'regions_in_transition',
    region_servers_started: 'region_servers_started',
    region_servers_installed: 'region_servers_installed',
    region_servers_total: 'region_servers_total',
    phoenix_servers_started: 'phoenix_servers_started',
    phoenix_servers_installed: 'phoenix_servers_installed',
    phoenix_servers_total: 'phoenix_servers_total'
  },
  stormConfig: {
    total_tasks: 'restApiComponent.tasksTotal',
    total_slots: 'restApiComponent.slotsTotal',
    free_slots: 'restApiComponent.slotsFree',
    used_slots: 'restApiComponent.slotsUsed',
    topologies: 'restApiComponent.topologies',
    total_executors: 'restApiComponent.executorsTotal',
    nimbus_uptime: 'restApiComponent.nimbusUptime',
    super_visors_started: 'super_visors_started',
    super_visors_installed: 'super_visors_installed',
    super_visors_total: 'super_visors_total'
  },
  rangerConfig: {
    ranger_tagsyncs_started: 'ranger_tagsyncs_started',
    ranger_tagsyncs_installed: 'ranger_tagsyncs_installed',
    ranger_tagsyncs_total: 'ranger_tagsyncs_total'
  },
  flumeConfig: {
    flume_handlers_total: 'flume_handlers_total'
  },
  flumeAgentConfig: {
    name: 'HostComponentProcess.name',
    status: 'HostComponentProcess.status',
    host_id: 'HostComponentProcess.host_name',
    host_name: 'HostComponentProcess.host_name',
    channels_count: 'HostComponentProcess.channels_count',
    sources_count: 'HostComponentProcess.sources_count',
    sinks_count: 'HostComponentProcess.sinks_count'
  },

  model3: App.HostComponent,
  config3: {
    work_status: 'HostRoles.state',
    passive_state: 'HostRoles.maintenance_state',
    display_name: 'HostRoles.display_name',
    component_name: 'HostRoles.component_name',
    host_id: 'HostRoles.host_name',
    host_name: 'HostRoles.host_name',
    public_host_name: 'HostRoles.public_host_name',
    stale_configs: 'HostRoles.stale_configs',
    ha_status: 'HostRoles.ha_state',
    display_name_advanced: 'display_name_advanced',
    admin_state: 'HostRoles.desired_admin_state',
    cluster_id_value: 'metrics.dfs.namenode.ClusterId'
  },

  /**
   * components which have additional relations and filtered for <code>computeAdditionalRelations</code>
   * @type {Array}
   * @const
   */
  ADVANCED_COMPONENTS: ['SECONDARY_NAMENODE', 'RESOURCEMANAGER', 'NAMENODE', 'HBASE_MASTER', 'RESOURCEMANAGER', 'HIVE_SERVER_INTERACTIVE'],

  hostNameIpMap: {},

  map: function (json) {
    console.time('App.serviceMetricsMapper execution time');
    if (json.items) {

      // Host components
      var hostComponents = [];
      var services = App.cache['services'];
      var previousComponentStatuses = App.cache['previousComponentStatuses'];
      var lastKnownStatusesLength = Em.keys(previousComponentStatuses).length;
      var previousComponentPassiveStates = App.cache['previousComponentPassiveStates'];
      var result = [];
      var advancedHostComponents = [];
      var hostComponentIdsMap = {};
      var hiveInteractiveServers = [];

      /**
       * services contains constructed service-components structure from components array
       */
      services.setEach('components', []);

      json.items.forEach(function (component) {
        var serviceName = component.ServiceComponentInfo.service_name;
        var service = services.findProperty('ServiceInfo.service_name', serviceName);
        if (service) {
          service.components.push(component);
        }
        component.host_components.forEach(function (host_component) {
          var id = host_component.HostRoles.component_name + "_" + host_component.HostRoles.host_name;
          hostComponentIdsMap[id] = true;
          previousComponentStatuses[id] = host_component.HostRoles.state;
          previousComponentPassiveStates[id] = host_component.HostRoles.maintenance_state;
          this.config3.ha_status = host_component.HostRoles.component_name == "HBASE_MASTER" ?
            'metrics.hbase.master.IsActiveMaster' : 'HostRoles.ha_state';
          var comp = this.parseIt(host_component, this.config3);
          comp.id = id;
          comp.service_id = serviceName;
          if (comp.component_name === 'HIVE_SERVER_INTERACTIVE') {
            hiveInteractiveServers.push(comp);
          }
          hostComponents.push(comp);
          if (this.get('ADVANCED_COMPONENTS').contains(comp.component_name)) {
            advancedHostComponents.push(comp);
          }
        }, this);
      }, this);

      this.computeAdditionalRelations(advancedHostComponents, services);
      //load master components to model
      previousMasterComponentIds.forEach(function (id) {
        if (!hostComponentIdsMap[id]) {
          var hostComponent = App.HostComponent.find(id);
          if (hostComponent.get('isLoaded')) {
            this.deleteRecord(hostComponent);
          }
          var serviceCache = services.findProperty('ServiceInfo.service_name', hostComponent.get('service.serviceName'));
          if (serviceCache) {
            serviceCache.host_components = serviceCache.host_components.without(hostComponent.get('id'));
          }
        } else if (id.startsWith('NAMENODE')) {
          var component = App.HostComponent.find(id),
            haNameSpace = component.get('haNameSpace');
          if (component.get('isLoaded') && haNameSpace) {
            hostComponents.findProperty('id', id).ha_name_space = haNameSpace;
          }
        }
      }, this);
      previousMasterComponentIds = hostComponents.mapProperty('id');

      App.store.safeLoadMany(this.get('model3'), hostComponents);

      if (hiveInteractiveServers.length > 1) {
        this.loadInteractiveServerStatuses(hiveInteractiveServers);
      }
      //parse service metrics from components
      services.forEach(function (item) {
        hostComponents.filterProperty('service_id', item.ServiceInfo.service_name).mapProperty('id').forEach(function (hostComponent) {
          if (!item.host_components.contains(hostComponent)) {
            item.host_components.push(hostComponent);
          }
        }, this);
        item.host_components.sort();

        var extendedModelInfo = this.mapExtendedModel(item);
        if (extendedModelInfo) {
          extendedModelInfo.passive_state = App.Service.find(item.ServiceInfo.service_name).get('passiveState');
          result.push(extendedModelInfo);
        }
      }, this);

      var stackServices = App.StackService.find().mapProperty('serviceName');
      result = misc.sortByOrder(stackServices, result);

      //load services to model
      App.store.safeLoadMany(this.get('model'), result);

      // check for new components
      if (lastKnownStatusesLength > 0) {
        if (lastKnownStatusesLength < Em.keys(App.cache.previousComponentStatuses).length) {
          App.get('router.clusterController').triggerQuickLinksUpdate();
        }
      }
    }
    console.timeEnd('App.serviceMetricsMapper execution time');
  },

  loadInteractiveServerStatuses: function (hiveInteractiveServers) {
    var self = this;
    App.router.get('configurationController').getCurrentConfigsBySites(['hive-site', 'hive-interactive-site']).done(function (configs) {
      var hiveWebUiPort = configs.findProperty('type', 'hive-interactive-site').properties['hive.server2.webui.port'];
      var hostNames = hiveInteractiveServers.mapProperty('host_name');
      var notDefinedHostIp = hostNames.find(function (hostName) {
        return !self.get('hostNameIpMap')[hostName];
      });
      if (notDefinedHostIp) {
        self.getHostNameIpMap(hostNames).done(function () {
          self.getHiveServersInteractiveStatus(hiveInteractiveServers, hiveWebUiPort);
        });
      } else {
        self.getHiveServersInteractiveStatus(hiveInteractiveServers, hiveWebUiPort);
      }
    });
  },

  getHostNameIpMap: function (hostNames) {
    var self = this;
    return App.ajax.send({
      name: 'hosts.ips',
      data: {
        hostNames: hostNames
      },
      sender: self
    }).success(function (data) {
      data.items.forEach(function(hostData) {
        var ip = hostData.Hosts.ip;
        var hostName = hostData.Hosts.host_name;
        self.get('hostNameIpMap')[hostName] = ip;
      })
    });
  },

  getHiveServersInteractiveStatus: function(hiveInteractiveServers, hiveWebUiPort) {
    var self = this;
    hiveInteractiveServers.forEach(function (hiveInteractiveServer) {
      App.ajax.send({
        name: 'hiveServerInteractive.getStatus',
        data: {
          hsiHost: self.hostNameIpMap[hiveInteractiveServer.host_name],
          port: hiveWebUiPort
        },
        sender: self
      }).success(function (isActive) {

        var advancedName = Em.I18n.t('quick.links.label.' +  (isActive ? 'active': 'standby')) + ' ' + hiveInteractiveServer.display_name;
        var hiveInteractiveServerComponent = App.HostComponent.find().find(function (component) {
          return component.get('hostName') === hiveInteractiveServer.host_name && component.get('componentName') === 'HIVE_SERVER_INTERACTIVE';
        });
        if (hiveInteractiveServerComponent){
          hiveInteractiveServerComponent.set('displayNameAdvanced', advancedName);
        }
      });
    });
  },

  /**
   * verify that service component has host components
   * @param {object} component
   * @param {string} name
   * @returns {boolean}
   */
  isHostComponentPresent: function(component, name) {
    return Boolean(component.ServiceComponentInfo
           && component.ServiceComponentInfo.component_name === name
           && Array.isArray(component.host_components)
           && component.host_components.length > 0);
  },

  /**
   * Generate service mapped object and load data to extended models.
   *
   * @method mapExtendedModel
   * @param {Object} item - json presents service information
   * @returns {Boolean|Object} - mapped info
   */
  mapExtendedModel: function(item) {
    var finalJson = false;
    if (item && item.ServiceInfo && item.ServiceInfo.service_name == "HDFS") {
      finalJson = this.hdfsMapper(item);
      finalJson.rand = Math.random();
      App.store.safeLoad(App.HDFSService, finalJson);
    } else if (item && item.ServiceInfo && item.ServiceInfo.service_name == "ONEFS") {
      finalJson = this.onefsMapper(item);
      finalJson.rand = Math.random();
      App.store.safeLoad(App.ONEFSService, finalJson);
    } else if (item && item.ServiceInfo && item.ServiceInfo.service_name == "HBASE") {
      finalJson = this.hbaseMapper(item);
      finalJson.rand = Math.random();
      App.store.safeLoad(App.HBaseService, finalJson);
    } else if (item && item.ServiceInfo && item.ServiceInfo.service_name == "FLUME") {
      finalJson = this.flumeMapper(item);
      finalJson.rand = Math.random();
      App.store.safeLoadMany(App.FlumeAgent, finalJson.agentJsons);
      App.store.safeLoad(App.FlumeService, finalJson);
    } else if (item && item.ServiceInfo && item.ServiceInfo.service_name == "YARN") {
      finalJson = this.yarnMapper(item);
      finalJson.rand = Math.random();
      App.store.safeLoad(App.YARNService, finalJson);
    } else if (item && item.ServiceInfo && item.ServiceInfo.service_name == "MAPREDUCE2") {
      finalJson = this.mapreduce2Mapper(item);
      finalJson.rand = Math.random();
      App.store.safeLoad(App.MapReduce2Service, finalJson);
    } else if (item && item.ServiceInfo && item.ServiceInfo.service_name == "STORM") {
      finalJson = this.stormMapper(item);
      finalJson.rand = Math.random();
      this.mapQuickLinks(finalJson, item);
      App.store.safeLoad(App.StormService, finalJson);
    } else if (item && item.ServiceInfo && item.ServiceInfo.service_name == "RANGER") {
      finalJson = this.rangerMapper(item);
      finalJson.rand = Math.random();
      App.store.safeLoad(App.RangerService, finalJson);
    } else {
      finalJson = this.parseIt(item, this.config);
      finalJson.rand = Math.random();
      this.mapQuickLinks(finalJson, item);
    }

    return finalJson;
  },
  /**
   * compute display name of host-components
   * compute tooltip content of services by host-components
   * @param hostComponents
   * @param services
   */
  computeAdditionalRelations: function (hostComponents, services) {
    var isSecondaryNamenode = hostComponents.findProperty('component_name', 'SECONDARY_NAMENODE');
    var isRMHAEnabled = hostComponents.filterProperty('component_name', 'RESOURCEMANAGER').length > 1;
    services.setEach('tool_tip_content', '');
    // set tooltip for client-only services
    var clientOnlyServiceNames = App.get('services.clientOnly');
    clientOnlyServiceNames.forEach(function (serviceName) {
      var service = services.findProperty('ServiceInfo.service_name', serviceName);
      if (service) {
        service.tool_tip_content = Em.I18n.t('services.service.summary.clientOnlyService.ToolTip');
      }
    });
    hostComponents.forEach(function (hostComponent) {
      var service = services.findProperty('ServiceInfo.service_name', hostComponent.service_id);
      if (hostComponent) {
        // set advanced nameNode display name for HA and Federation, Active NameNodes or Standby NameNodes
        // this is useful on three places: 1) HDFS health status hover tooltip, 2) HDFS service summary 3) NameNode component on host detail page
        if (hostComponent.component_name === 'NAMENODE' && !isSecondaryNamenode) {
          var hdfs = this.hdfsMapper(service);
          var hostName = hostComponent.host_id;
          var activeNNText = Em.I18n.t('services.service.summary.nameNode.active');
          var standbyNNText = Em.I18n.t('services.service.summary.nameNode.standby');
          if (hdfs) {
            // active_name_node_id format : NAMENODE_c6401.ambari.apache.org
            if (hdfs.active_name_nodes && hdfs.active_name_nodes.contains(`NAMENODE_${hostName}`)) {
              hostComponent.display_name_advanced = activeNNText;
            } else if (hdfs.standby_name_nodes && hdfs.standby_name_nodes.contains(`NAMENODE_${hostName}`)) {
              hostComponent.display_name_advanced = standbyNNText;
            } else {
              hostComponent.display_name_advanced = null;
            }
          }
        } else if (hostComponent.component_name === 'HBASE_MASTER') {
          if (hostComponent.work_status === 'STARTED') {
            (hostComponent.ha_status === true || hostComponent.ha_status == 'true') ?
              hostComponent.display_name_advanced = this.t('dashboard.services.hbase.masterServer.active') :
              hostComponent.display_name_advanced = this.t('dashboard.services.hbase.masterServer.standby');
          } else {
            hostComponent.display_name_advanced = null;
          }
        } else if (hostComponent.component_name === 'RESOURCEMANAGER' && isRMHAEnabled && hostComponent.work_status === 'STARTED') {
          switch (hostComponent.ha_status) {
            case 'ACTIVE':
              hostComponent.display_name_advanced = Em.I18n.t('dashboard.services.yarn.resourceManager.active');
              break;
            case 'STANDBY':
              hostComponent.display_name_advanced = Em.I18n.t('dashboard.services.yarn.resourceManager.standby');
              break;
          }
        } else if (hostComponent.component_name === 'HIVE_SERVER_INTERACTIVE') {
          var hiveInteractiveServerComponent = App.HostComponent.find().find(function (component) {
            return component.get('hostName') === hostComponent.host_name && component.get('componentName') === 'HIVE_SERVER_INTERACTIVE';
          });
          if (hiveInteractiveServerComponent) {
            hostComponent.display_name_advanced = hiveInteractiveServerComponent.get('displayNameAdvanced');
          }
        }
        if (service) {
          if (hostComponent.display_name_advanced) {
            service.tool_tip_content += hostComponent.display_name_advanced + " " + App.HostComponentStatus.getTextStatus(hostComponent.work_status) + "<br/>";
          } else {
            service.tool_tip_content += App.format.role(hostComponent.component_name, false) + " " + App.HostComponentStatus.getTextStatus(hostComponent.work_status) + "<br/>";
          }
        }
      }
    }, this)
  },

  setNameNodeMetricsProperties: function (item, hostComponent) {
    const activeNameNodeConfig = this.activeNameNodeConfig,
      activeNameNodeConfigKeys = Object.keys(activeNameNodeConfig);
    activeNameNodeConfigKeys.forEach(key => {
      item[key][Em.get(hostComponent, 'HostRoles.host_name')] = Em.get(hostComponent, activeNameNodeConfig[key]);
    });
  },

  /**
   * Map quick links to services:OOZIE,GANGLIA
   * @param finalJson
   * @param item
   */
  mapQuickLinks: function (finalJson, item) {
    if (!(item && item.ServiceInfo)) return;
    var quickLinks = {
      OOZIE: [19],
      GANGLIA: [20],
      STORM: [31],
      FALCON: [32],
      RANGER: [33],
      SPARK: [34],
      ACCUMULO: [35],
      ATLAS: [36],
      AMBARI_METRICS: [37],
      LOGSEARCH: [38]
    };
    if (quickLinks[item.ServiceInfo.service_name])
      finalJson.quick_links = quickLinks[item.ServiceInfo.service_name];
  },

  onefsMapper: function (item) {
    var finalConfig = jQuery.extend({}, this.config);
    item.components.forEach(function (component) {
      if (this.isHostComponentPresent(component, 'ONEFS_CLIENT')) {
        item.metrics = component.metrics;
        item.decommission_data_nodes = [];
        item.dead_data_nodes = [];
        item.live_data_nodes = [];
        if (component.metrics && component.metrics.dfs && component.metrics.dfs.namenode) {
          item.metrics_not_available = false;
          for (var host in App.parseJSON(component.metrics.dfs.namenode.DecomNodes)) {
            item.decommission_data_nodes.push('DATANODE' + '_' + host);
          }
          for (var host in App.parseJSON(component.metrics.dfs.namenode.DeadNodes)) {
            item.dead_data_nodes.push('DATANODE' + '_' + host);
          }
          for (var host in App.parseJSON(component.metrics.dfs.namenode.LiveNodes)) {
            item.live_data_nodes.push('DATANODE' + '_' + host);
          }
        } else {
          item.metrics_not_available = true;
        }
        finalConfig = jQuery.extend(finalConfig, this.onefsConfig);
      }
    }, this);
    return this.parseIt(item, finalConfig);
  },

  hdfsMapper: function (item) {
    let finalConfig = jQuery.extend({}, this.config);
    // Change the JSON so that it is easy to map
    const hdfsConfig = this.hdfsConfig,
      activeNameNodeConfigInitial = Object.keys(this.activeNameNodeConfig).reduce((obj, key) => Object.assign({}, obj, {
        [key]: {}
      }), {});
    Object.assign(item, activeNameNodeConfigInitial);
    item.components.forEach(component => {
      const hostComponents = component.host_components,
        firstHostComponent = hostComponents[0];
      if (this.isHostComponentPresent(component, 'NAMENODE')) {
        //enabled HA
        if (hostComponents.length > 1) {
          let nameSpacesWithActiveNameNodes = [],
            unknownNameNodes = [];
          item.active_name_nodes = [];
          item.standby_name_nodes = [];
          hostComponents.forEach(hc => {
            const haState = Em.get(hc, 'metrics.dfs.FSNamesystem.HAState'),
              hostName = Em.get(hc, 'HostRoles.host_name'),
              clusterIdValue = Em.get(hc, 'metrics.dfs.namenode.ClusterId'),
              id = `NAMENODE_${hostName}`;
            switch (haState) {
              case 'active':
                nameSpacesWithActiveNameNodes.push(clusterIdValue);
                item.active_name_nodes.push(id);
                break;
              case 'standby':
                item.standby_name_nodes.push(id);
                break;
              default:
                unknownNameNodes.push({
                  hostName,
                  clusterIdValue
                });
                break;
            }
            this.setNameNodeMetricsProperties(item, hc);
          });
          unknownNameNodes.forEach(nameNode => {
            if (nameSpacesWithActiveNameNodes.contains(nameNode.clusterIdValue)) {
              item.standby_name_nodes.push(`NAMENODE_${nameNode.hostName}`);
            }
          });
        } else {
          this.setNameNodeMetricsProperties(item, firstHostComponent);
        }

        item.nameNodeComponent = component;
        finalConfig = jQuery.extend(finalConfig, hdfsConfig);
        // Get the live, dead & decommission nodes from string json
        if (firstHostComponent.metrics && firstHostComponent.metrics.dfs && firstHostComponent.metrics.dfs.namenode) {
          item.metrics_not_available = false;
          var decommissionNodesJson = App.parseJSON(firstHostComponent.metrics.dfs.namenode.DecomNodes);
          var deadNodesJson = App.parseJSON(firstHostComponent.metrics.dfs.namenode.DeadNodes);
          var liveNodesJson = App.parseJSON(firstHostComponent.metrics.dfs.namenode.LiveNodes);
        } else {
          item.metrics_not_available = true;
        }
        item.decommission_data_nodes = [];
        item.dead_data_nodes = [];
        item.live_data_nodes = [];
        for (let host in decommissionNodesJson) {
          item.decommission_data_nodes.push(`DATANODE_${host}`);
        }
        for (let host in deadNodesJson) {
          item.dead_data_nodes.push(`DATANODE_${host}`);
        }
        for (let host in liveNodesJson) {
          item.live_data_nodes.push(`DATANODE_${host}`);
        }
        item.name_node_id = `NAMENODE_${firstHostComponent.HostRoles.host_name}`;
      }
      if (this.isHostComponentPresent(component, 'JOURNALNODE')) {
        item.journal_nodes = hostComponents.map(hc => `JOURNALNODE_${hc.HostRoles.host_name}`);
      }
      if (this.isHostComponentPresent(component, 'SECONDARY_NAMENODE')) {
        item.sname_node_id = `SECONDARY_NAMENODE_${firstHostComponent.HostRoles.host_name}`;
      }
    });
    // Map
    let finalJson = this.parseIt(item, finalConfig);
    finalJson.quick_links = [1, 2, 3, 4];

    return finalJson;
  },

  yarnMapper: function (item) {
    var self = this;
    var finalConfig = jQuery.extend({}, this.config);
    // Change the JSON so that it is easy to map
    var yarnConfig = this.yarnConfig;
    item.components.forEach(function (component) {
      if (this.isHostComponentPresent(component, "RESOURCEMANAGER")) {
        item.resourceManagerComponent = component;

        // if YARN has two host components, ACTIVE one should be first in component.host_components array for proper metrics mapping
        if (component.host_components.length === 2) {
          var activeRM = component.host_components.findProperty('HostRoles.ha_state', 'ACTIVE');
          var activeHostComponentIndex = component.host_components.indexOf(activeRM);
          self.setActiveAsFirstHostComponent(component, activeHostComponentIndex);
          if (activeRM) {
            item.active_resource_manager_id = "RESOURCEMANAGER" + "_" + activeRM.HostRoles.host_name;
          }
        }

        if (component.host_components[0].metrics && component.host_components[0].metrics.yarn) {
          var root = component.host_components[0].metrics.yarn.Queue.root;
          component.queue = JSON.stringify({
            'root': self.parseObject(root)
          });
        }
        // extend config
        finalConfig = jQuery.extend(finalConfig, yarnConfig);
        item.resource_manager_id = "RESOURCEMANAGER" + "_" + component.host_components[0].HostRoles.host_name;
      }
      if (this.isHostComponentPresent(component, "APP_TIMELINE_SERVER")) {
        item.app_timeline_server_id = "APP_TIMELINE_SERVER" + "_" + component.host_components[0].HostRoles.host_name;
      }
    }, this);
    // Map
    var finalJson = this.parseIt(item, finalConfig);
    finalJson.quick_links = [23, 24, 25, 26];
    return finalJson;
  },

  parseObject: function (obj) {
    var res = {};
    for (var p in obj) {
      if (obj.hasOwnProperty(p)) {
        if (obj[p] instanceof Object) {
          res[p] = this.parseObject(obj[p]);
        }
      }
    }
    return res;
  },

  mapreduce2Mapper: function (item) {
    var finalConfig = jQuery.extend({}, this.config);
    // Change the JSON so that it is easy to map
    var mapReduce2Config = this.mapReduce2Config;
    item.components.forEach(function (component) {
      if (this.isHostComponentPresent(component, "HISTORYSERVER")) {
        item.jobHistoryServerComponent = component;
        finalConfig = jQuery.extend(finalConfig, mapReduce2Config);
        item.job_history_server_id = "HISTORYSERVER" + "_" + component.host_components[0].HostRoles.host_name;
      }
    }, this);
    // Map
    var finalJson = this.parseIt(item, finalConfig);
    finalJson.quick_links = [27, 28, 29, 30];

    return finalJson;
  },
  hbaseMapper: function (item) {
    var self = this;
    // Change the JSON so that it is easy to map
    var finalConfig = jQuery.extend({}, this.config);
    var hbaseConfig = this.hbaseConfig;
    item.components.forEach(function (component) {
      if (this.isHostComponentPresent(component, "HBASE_MASTER")) {
        item.masterComponent = component;
        finalConfig = jQuery.extend(finalConfig, hbaseConfig);
        if (component.host_components.length) {
          var activeMaster = component.host_components.findProperty('metrics.hbase.master.IsActiveMaster', 'true');
          var activeHostComponentIndex = component.host_components.indexOf(activeMaster);
          self.setActiveAsFirstHostComponent(component, activeHostComponentIndex);
          var regionsArray = null;
          if (!Em.none(Em.get(component.host_components[0], 'metrics.master.AssignmentManager.ritCount'))) {
            regionsArray = App.parseJSON(component.host_components[0].metrics.master.AssignmentManager.ritCount);
          }
          //regions_in_transition can have various type of value: null, array or int
          if (Array.isArray(regionsArray)) {
            item.regions_in_transition = regionsArray.length;
          } else {
            item.regions_in_transition = regionsArray == null ? 0 : regionsArray;
          }
        }
        item.master_id = "HBASE_MASTER" + "_" + component.host_components[0].HostRoles.host_name;
      }
    }, this);
    // Map
    var finalJson = this.parseIt(item, finalConfig);
    finalJson.average_load = parseFloat(finalJson.average_load).toFixed(2);
    finalJson.quick_links = [13, 14, 15, 16, 17, 18];
    return finalJson;
  },

  /**
   * Sets the active host component as the first host component
   * @param component {Object}
   * @param activeHostComponentIndex {Number}
   */
  setActiveAsFirstHostComponent: function (component, activeHostComponentIndex) {
    // important: active component always at host_components[0];
    if (activeHostComponentIndex && activeHostComponentIndex !== -1) {
      var tmp = component.host_components[activeHostComponentIndex];
      component.host_components[activeHostComponentIndex] = component.host_components[0];
      component.host_components[0] = tmp;
    }
  },

  /**
   * Flume is different from other services, in that the important
   * data is in customizable channels. Hence we directly transfer
   * data into the JSON object.
   */
  flumeMapper: function (item) {
    var self = this;
    var finalConfig = jQuery.extend({}, this.config);
    var flumeConfig = this.flumeConfig;
    finalConfig = jQuery.extend(finalConfig, flumeConfig);
    var finalJson = self.parseIt(item, finalConfig);
    var flumeHandlers = item.components.findProperty('ServiceComponentInfo.component_name', "FLUME_HANDLER");
    flumeHandlers = flumeHandlers ? flumeHandlers.host_components : [];
    finalJson.agents = [];
    finalJson.agentJsons = [];
    flumeHandlers.forEach(function (flumeHandler) {
      var hostName = flumeHandler.HostRoles.host_name;
      flumeHandler.processes.forEach(function (process) {
        var agentJson = self.parseIt(process, self.flumeAgentConfig);
        var agentId = agentJson.name + "-" + hostName;
        finalJson.agents.push(agentId);
        agentJson.id = agentId;
        finalJson.agentJsons.push(agentJson);
      });
    });
    return finalJson;
  },

  /**
   * Storm mapper
   */
  stormMapper: function (item) {
    var finalConfig = jQuery.extend({}, this.config);
    var stormConfig = this.stormConfig;
    var metricsInfoComponent = /^2.1/.test(App.get('currentStackVersionNumber')) ? 'STORM_REST_API' : 'STORM_UI_SERVER';
    var metricsPath = {
      STORM_REST_API: 'metrics.api.cluster.summary',
      STORM_UI_SERVER: 'metrics.api.v1.cluster.summary'
    }[metricsInfoComponent];
    var restApiMetrics = {};

    item.components.forEach(function (component) {
      var componentName = component.ServiceComponentInfo.component_name;
      if (component.ServiceComponentInfo && componentName == metricsInfoComponent) {
        if (Em.get(component, metricsPath)) {
          $.extend(restApiMetrics, App.keysDottedToCamelCase(Em.get(component, metricsPath)));
          if (metricsInfoComponent == 'STORM_UI_SERVER') {
            restApiMetrics.topologies = Em.get(component, 'metrics.api.v1.topology.summary.length');
            if (stringUtils.compareVersions(App.get('currentStackVersionNumber'), '2.3') > -1) {
              restApiMetrics.nimbusUptime = Em.getWithDefault(component, 'metrics.api.v1.nimbus.summary.0.nimbusUpTime', Em.I18n.t('services.service.summary.notRunning'));
            }
          } else {
            restApiMetrics.nimbusUptime = dateUtils.timingFormat(restApiMetrics.nimbusUptime * 1000);
          }
        }
        finalConfig = jQuery.extend({}, finalConfig, stormConfig);
      }
    });
    item.restApiComponent = restApiMetrics;
    return this.parseIt(item, finalConfig);
  },

  /**
   * Ranger mapper
   */
  rangerMapper: function (item) {
    var finalConfig = jQuery.extend({}, this.config);
    var rangerConfig = this.rangerConfig;
        finalConfig = jQuery.extend({}, finalConfig, rangerConfig);
    return this.parseIt(item, finalConfig);
  }
});
