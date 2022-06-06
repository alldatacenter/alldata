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

/**
 * Config for each ajax-request
 *
 * Fields example:
 *  mock - testMode url
 *  real - real url (without API prefix)
 *  type - request type (also may be defined in the format method)
 *  format - function for processing ajax params after default formatRequest. May be called with one or two parameters (data, opt). Return ajax-params object
 *  testInProduction - can this request be executed on production tests (used only in tests)
 *
 * @type {Object}
 */
var urls = {

  'common.cluster.update' : {
    'type': 'PUT',
    'real': '/clusters/{clusterName}',
    'mock': '/data/wizard/deploy/poll_1.json',
    'format': function (data) {
      return {
        data: JSON.stringify(data.data)
      };
    }
  },

  'common.services.update' : {
    'real': '/clusters/{clusterName}/services?{urlParams}',
    'mock': '/data/wizard/deploy/poll_1.json',
    'format': function (data) {
      return {
        type: 'PUT',
        data: JSON.stringify({
          RequestInfo: {
            "context": data.context,
            "operation_level": {
              "level": "CLUSTER",
              "cluster_name" : data.clusterName
            }
          },
          Body: {
            ServiceInfo: data.ServiceInfo
          }
        })
      };
    }
  },

  'common.service.update' : {
    'real': '/clusters/{clusterName}/services/{serviceName}',
    'mock': '/data/wizard/deploy/poll_1.json',
    'format': function (data) {
      return {
        type: 'PUT',
        data: JSON.stringify({
          RequestInfo: {
            "context": data.context,
            "operation_level": {
              "level": "SERVICE",
              "cluster_name" : data.clusterName,
              "service_name" : data.serviceName
            }
          },
          Body: {
            ServiceInfo: data.ServiceInfo
          }
        })
      };
    }
  },

  'common.service_component.info' : {
    'real': '/clusters/{clusterName}/services/{serviceName}/components/{componentName}?{urlParams}',
    'mock': '/data/wizard/deploy/poll_1.json'
  },

  'common.service.hdfs.getNnCheckPointTime': {
    'real': '/clusters/{clusterName}/services/HDFS/components/NAMENODE?fields=host_components/metrics/dfs/FSNamesystem/HAState,host_components/metrics/dfs/FSNamesystem/LastCheckpointTime,host_components/metrics/dfs/namenode/ClusterId',
    'mock': ''
  },

  'common.host_component.getNnCheckPointTime': {
    'real': '/clusters/{clusterName}/hosts/{host}/host_components/NAMENODE?fields=metrics/dfs/FSNamesystem/HAState,metrics/dfs/FSNamesystem/LastCheckpointTime',
    'mock': ''
  },

  'common.host_component.update': {
    'real': '/clusters/{clusterName}/host_components',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          RequestInfo: {
            context: data.context,
            query: data.query
          },
          Body: {
            "HostRoles": data.HostRoles
          }
        })
      }
    }
  },

  'common.host.host_components.create': {
    'real': '/clusters/{clusterName}/hosts',
    'mock': '',
    'type': 'POST',
    'format': function (data) {
      return {
        data: JSON.stringify({
          RequestInfo: {
            "query": data.query
          },
          Body: {
            "host_components": data.host_components
          }
        })
      }
    }
  },

  'common.host.host_components.update': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components?{urlParams}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          RequestInfo: {
            "context": data.context,
            "operation_level": {
              level: "HOST",
              cluster_name: data.clusterName,
              host_names: data.hostName
            },
            query: data.query
          },
          Body: {
            "HostRoles": data.HostRoles
          }
        })
      }
    }
  },

  'common.host.host_component.update': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}?{urlParams}',
    'mock': '/data/wizard/deploy/2_hosts/poll_9.json',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          RequestInfo: {
            "context": data.context,
            "operation_level": {
              level: "HOST_COMPONENT",
              cluster_name: data.clusterName,
              host_name: data.hostName,
              service_name: data.serviceName || null
            }
          },
          Body: {
            "HostRoles": data.HostRoles
          }
        })
      }
    }
  },

  'common.hosts.all': {
    'real': '/clusters/{clusterName}/host_components?{urlParams}&minimal_response=true',
    'mock': ''
  },

  'common.service.configurations': {
    'real':'/clusters/{clusterName}',
    'mock':'',
    'format': function (data) {
      return {
        type: 'PUT',
        data: JSON.stringify({
          Clusters: {
            desired_config: data.desired_config
          }
        })
      }
    }
  },

  'common.service.multiConfigurations': {
    'real':'/clusters/{clusterName}',
    'mock':'',
    'format': function (data) {
      return {
        type: 'PUT',
        data: JSON.stringify(data.configs)
      }
    }
  },

  'common.across.services.configurations': {
    'type': 'PUT',
    'real':'/clusters/{clusterName}',
    'mock':'/data/services/ambari.json',
    'format': function(data) {
      return {
        dataType: 'text',
        data: data.data
      }
    }
  },

  'common.request.polling': {
    'real': '/clusters/{clusterName}/requests/{requestId}?fields=tasks/Tasks/request_id,tasks/Tasks/command,tasks/Tasks/command_detail,tasks/Tasks/ops_display_name,tasks/Tasks/start_time,tasks/Tasks/end_time,tasks/Tasks/exit_code,tasks/Tasks/host_name,tasks/Tasks/id,tasks/Tasks/role,tasks/Tasks/status,tasks/Tasks/structured_out,Requests/*&tasks/Tasks/stage_id={stageId}',
    'mock': '/data/background_operations/host_upgrade_tasks.json'
  },

  'ambari.service.load_server_version': {
    'real': '/services/AMBARI?fields=components/RootServiceComponents/component_version&components/RootServiceComponents/component_name=AMBARI_SERVER&minimal_response=true',
    'mock': '/data/services/ambari.json'
  },


  'service.flume.agent.command': {
    'real': '/clusters/{clusterName}/hosts/{host}/host_components/FLUME_HANDLER',
    'mock': '',
    'format': function (data) {
      return {
        type: 'PUT',
        data: JSON.stringify({
          "RequestInfo": {
            "context": data.context,
            "flume_handler": data.agentName,
            "operation_level": {
              level: "HOST_COMPONENT",
              cluster_name: data.clusterName,
              service_name: "FLUME",
              host_name: data.host
            }
          },
          "Body": {
            "HostRoles": {
              "state": data.state
            }
          }
        })
      }
    }
  },

  'common.host_components.update': {
    'real': '/clusters/{clusterName}/host_components?{urlParams}',
    'mock': '/data/wizard/deploy/poll_1.json',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          RequestInfo: {
            "context": data.context,
            "operation_level": {
              level: data.level || "CLUSTER",
              cluster_name: data.clusterName
            },
            query: data.query
          },
          Body: {
            "HostRoles": data.HostRoles
          }
        })
      }
    }
  },

  'common.hosts.delete': {
    'real': '/clusters/{clusterName}/hosts{urlParams}',
    'type': 'DELETE',
    'format': function (data) {
      return {
        data: JSON.stringify({
          RequestInfo: {
            query: data.query
          }
        })
      }
    }
  },

  'common.service.passive': {
    'real': '/clusters/{clusterName}/services/{serviceName}',
    'mock': '',
    'format': function (data) {
      return {
        type: 'PUT',
        data: JSON.stringify({
          RequestInfo: {
            "context": data.requestInfo
          },
          Body: {
            ServiceInfo: {
              maintenance_state: data.passive_state
            }
          }
        })
      };
    }
  },

  'common.service.host_component.update': {
    'real': '/clusters/{clusterName}/host_components',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          RequestInfo: {
            context: data.context,
            operation_level: {
              level: 'SERVICE',
              cluster_name: data.clusterName,
              service_name: data.serviceName
            },
            query: data.query
          },
          Body: {
            HostRoles: {
              state: data.state
            }
          }
        })
      }
    }
  },

  'common.host.host_component.passive': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          RequestInfo: {
            "context": data.context
          },
          Body: {
            HostRoles: {
              maintenance_state: data.passive_state
            }
          }
        })
      };
    }
  },

  'common.host.with_host_component': {
    'real': '/clusters/{clusterName}/hosts?host_components/HostRoles/component_name={componentName}&minimal_response=true',
    'mock': ''
  },

  'common.batch.request_schedules': {
    'real': '/clusters/{clusterName}/request_schedules',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify([{
          "RequestSchedule": {
            "batch": [{
              "requests": data.batches
            }, {
              "batch_settings": {
                "batch_separation_in_seconds": data.intervalTimeSeconds,
                "task_failure_tolerance": data.tolerateSize
              }
            }]
          }
        }])
      }
    }
  },

  'common.delete.host': {
    'real': '/clusters/{clusterName}/hosts/{hostName}',
    'type': 'DELETE'
  },
  'common.delete.host_component': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}',
    'type': 'DELETE'
  },
  'common.delete.user': {
    'real': '/users/{user}',
    'type': 'DELETE'
  },
  'common.delete.config_group': {
    'real': '/clusters/{clusterName}/config_groups/{id}',
    'type': 'DELETE'
  },
  'common.delete.cluster': {
    'real': '/clusters/{name}',
    'type': 'DELETE'
  },
  'common.delete.service': {
    'real': '/clusters/{clusterName}/services/{serviceName}',
    'mock': '/data/services/ambari.json',
    'type': 'DELETE'
  },
  'common.delete.request_schedule': {
    'real': '/clusters/{clusterName}/request_schedules/{request_schedule_id}',
    'type': 'DELETE'
  },
  'common.get.request.status': {
    'real': '/clusters/{clusterName}/requests/{requestId}?fields=Requests/request_status',
    'type': 'GET'
  },
  'alerts.load_alert_groups': {
    'real': '/clusters/{clusterName}/alert_groups?fields=*',
    'mock': 'data/alerts/alertGroups.json'
  },
  'alerts.load_an_alert_group': {
    'real': '/clusters/{clusterName}/alert_groups/{group_id}',
    'mock': 'data/alerts/alertGroup.json'
  },
  'alert_groups.create': {
    'real': '/clusters/{clusterName}/alert_groups',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify({
          "AlertGroup": {
            "name": data.name,
            "definitions": data.definitions,
            "targets": data.targets
          }
        })
      };
    }
  },
  'alert_groups.update': {
    'real': '/clusters/{clusterName}/alert_groups/{group_id}',
    'mock': '',
    'format': function (data) {
      return {
        type: 'PUT',
        data: JSON.stringify({
          "AlertGroup": {
            "name": data.name,
            "definitions": data.definitions,
            "targets": data.targets
          }
        })
      };
    }
  },
  'alert_groups.delete': {
    'real': '/clusters/{clusterName}/alert_groups/{group_id}',
    'mock': '',
    'format': function (data) {
      return {
        type: 'DELETE'
      };
    }
  },
  'alerts.load_all_alert_definitions': {
    'real': '/clusters/{clusterName}/alert_definitions?fields=*',
    'mock': 'data/alerts/alertDefinitions.json'
  },
  'alerts.notifications': {
    'real': '/alert_targets?fields=*',
    'mock': '/data/alerts/alertNotifications.json'
  },
  'alerts.instances': {
    'real': '/clusters/{clusterName}/alerts?fields=*',
    'mock': '/data/alerts/alert_instances.json'
  },
  'alerts.instances.unhealthy': {
    'real': '/clusters/{clusterName}/alerts?fields=*&Alert/state.in(CRITICAL,WARNING)&{paginationInfo}',
    'mock': '/data/alerts/alert_instances.json'
  },
  'alerts.instances.by_definition': {
    'real': '/clusters/{clusterName}/alerts?fields=*&Alert/definition_id={definitionId}',
    'mock': '/data/alerts/alert_instances.json'
  },
  'alerts.instances.by_host': {
    'real': '/clusters/{clusterName}/alerts?fields=*&Alert/host_name={hostName}',
    'mock': '/data/alerts/alert_instances.json'
  },
  'alerts.update_alert_definition': {
    'real': '/clusters/{clusterName}/alert_definitions/{id}',
    'mock': '',
    'format': function (data) {
      return {
        type: 'PUT',
        data: JSON.stringify(data.data)
      }
    }
  },
  'alerts.create_alert_definition': {
    'real': '/clusters/{clusterName}/alert_definitions/',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify(data.data)
      }
    }
  },
  'alerts.delete_alert_definition': {
    'real': '/clusters/{clusterName}/alert_definitions/{id}',
    'mock': '',
    'type': 'DELETE'
  },
  'alerts.create_alert_notification': {
    'real': '/alert_targets?{urlParams}',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify(data.data)
      }
    }
  },
  'alerts.update_alert_notification': {
    'real': '/alert_targets/{id}',
    'mock': '',
    'format': function (data) {
      return {
        type: 'PUT',
        data: JSON.stringify(data.data)
      }
    }
  },
  'alerts.delete_alert_notification': {
    'real': '/alert_targets/{id}',
    'mock': '',
    'type': 'DELETE'
  },
  'alerts.get_instances_history': {
    'real': '/clusters/{clusterName}/alert_history?(AlertHistory/definition_name={definitionName})&(AlertHistory/timestamp>={timestamp})',
    'mock': '/data/alerts/alert_instances_history.json'
  },
  'background_operations.get_most_recent': {
    'real': '/clusters/{clusterName}/requests?to=end&page_size={operationsCount}&fields=' +
    'Requests/end_time,Requests/id,Requests/progress_percent,Requests/request_context,' +
    'Requests/request_status,Requests/start_time,Requests/cluster_name,Requests/user_name&minimal_response=true',
    'mock': '/data/background_operations/list_on_start.json',
    'testInProduction': true
  },
  'background_operations.get_by_request': {
    'real': '/clusters/{clusterName}/requests/{requestId}?fields=*,tasks/Tasks/request_id,tasks/Tasks/command,tasks/Tasks/command_detail,tasks/Tasks/ops_display_name,tasks/Tasks/host_name,tasks/Tasks/id,tasks/Tasks/role,tasks/Tasks/status&minimal_response=true',
    'mock': '/data/background_operations/task_by_request{requestId}.json',
    'testInProduction': true
  },
  'background_operations.get_by_task': {
    'real': '/clusters/{clusterName}/requests/{requestId}/tasks/{taskId}',
    'mock': '/data/background_operations/list_on_start.json',
    'testInProduction': true
  },
  'background_operations.abort_request': {
    'real': '/clusters/{clusterName}/requests/{requestId}',
    'mock': '',
    'format': function () {
      return {
        type: 'PUT',
        data: JSON.stringify({
          "Requests": {
            "request_status": "ABORTED",
            "abort_reason": Em.I18n.t('hostPopup.bgop.abortRequest.reason')
          }
        })
      };
    }
  },
  'service.item.smoke': {
    'real': '/clusters/{clusterName}/requests',
    'mock': '/data/wizard/deploy/poll_1.json',
    'format': function (data) {
      var requestData = {
        "RequestInfo": {
          "context": data.displayName + " Service Check",
          "command": data.actionName
        },
        "Requests/resource_filters": [{"service_name": data.serviceName}]
      };
      if (data.operationLevel) {
        requestData.RequestInfo.operation_level = data.operationLevel;
      }
      return {
        'type': 'POST',
        data: JSON.stringify(requestData)
      };
    }
  },
  'service.item.rebalanceHdfsNodes': {
    'real': '/clusters/{clusterName}/requests',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify({
          RequestInfo: {
            'context': Em.I18n.t('services.service.actions.run.rebalanceHdfsNodes.context'),
            'command': 'REBALANCEHDFS',
            'namenode': JSON.stringify({threshold: data.threshold})
          },
          "Requests/resource_filters": [{
            'service_name': 'HDFS',
            'component_name': 'NAMENODE',
            'hosts': data.hosts
          }]
        })
      }
    }
  },

  'cancel.background.operation': {
    'real': '/clusters/{clusterName}/requests/{requestId}',
    'mock': '',
    'format': function (data) {
      return {
        type: 'PUT',
        data: JSON.stringify({
          RequestInfo: {
            'context': 'Cancel operation',
            "parameters": {
              "cancel_policy": "SIGKILL"
            }
          },
          "Requests/request_status": 'ABORTED',
          "Requests/abort_reason": "Cancel background operation"
        })
      }
    }
  },


  'service.item.refreshQueueYarnRequest': {
    'real': '/clusters/{clusterName}/requests',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify({
          RequestInfo: {
            'context': data.context,
            'command': data.command,
            'parameters/forceRefreshConfigTags': data.forceRefreshConfigTags
          },
          "Requests/resource_filters": [{
            "service_name": data.serviceName,
            "component_name": data.componentName,
            'hosts': data.hosts
          }]
        })
      }
    }
  },

  'service.item.startStopLdapKnox': {
    'real': '/clusters/{clusterName}/requests',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify({
          RequestInfo: {
            'context': data.context,
            'command': data.command
          },
          "Requests/resource_filters": [{
            "service_name": data.serviceName,
            "component_name": data.componentName,
            'hosts': data.host
          }]
        })
      }
    }
  },

  'service.item.updateHBaseReplication': {
    'real': '/clusters/{clusterName}/requests',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify({
          RequestInfo: {
            'context': Em.I18n.t('services.service.actions.run.updateHBaseReplication.context'),
            'command': 'UPDATE_REPLICATION',
            "parameters": {
              "replication_cluster_keys": data.replication_cluster_keys,
              "replication_peers": data.replication_peers
            }
          },
          "Requests/resource_filters": [{
            'service_name': 'HBASE',
            'component_name': 'HBASE_MASTER',
            'hosts': data.hosts
          }]
        })
      }
    }
  },

  'service.item.stopHBaseReplication': {
    'real': '/clusters/{clusterName}/requests',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify({
          RequestInfo: {
            'context': Em.I18n.t('services.service.actions.run.stopHBaseReplication.context'),
            'command': 'STOP_REPLICATION',
            "parameters": {
              "replication_peers": data.replication_peers
            }
          },
          "Requests/resource_filters": [{
            'service_name': 'HBASE',
            'component_name': 'HBASE_MASTER',
            'hosts': data.hosts
          }]
        })
      }
    }
  },



  'service.item.executeCustomCommand': {
    'real': '/clusters/{clusterName}/requests',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify({
          RequestInfo: {
            'context': data.context,
            'command': data.command
          },
          "Requests/resource_filters": [{
            "service_name": data.serviceName,
            "component_name": data.componentName,
            'hosts': data.hosts
          }]
        })
      }
    }
  },
  /*************************CONFIG THEME****************************************/

  'configs.theme': {
    'real': '{stackVersionUrl}/services/{serviceName}/themes?ThemeInfo/default=true&fields=*',
    'mock': '/data/configurations/theme.json'
  },

  'configs.theme.services': {
    'real': '{stackVersionUrl}/services?StackServices/service_name.in({serviceNames})&themes/ThemeInfo/default=true&fields=themes/*',
    'mock': '/data/configurations/theme_services.json'
  },

  /*************************CONFIG QUICKLINKS****************************************/

  'configs.quicklinksconfig': {
    'real': '{stackVersionUrl}/services/{serviceName}/quicklinks?QuickLinkInfo/default=true&fields=*',
    'mock': '/data/configurations/quicklinks.json'
  },

  'configs.quicklinksconfig.services': {
    'real': '{stackVersionUrl}/services?StackServices/service_name.in({serviceNames})&quicklinks/QuickLinkInfo/default=true&fields=quicklinks/*',
    'mock': '/data/configurations/quicklinks_services.json'
  },

  /*************************CONFIG GROUPS***************************************/

  'configs.config_groups.load.all': {
    'real': '/clusters/{clusterName}/config_groups?fields=*',
    'mock': '/data/configurations/config_groups.json'
  },

  'configs.config_groups.load.services': {
    'real': '/clusters/{clusterName}/config_groups?ConfigGroup/tag.in({serviceNames})&fields=*',
    'mock': '/data/configurations/config_groups.json'
  },

  /*************************STACK CONFIGS**************************************/

  'configs.stack_configs.load.cluster_configs': {
    'real': '{stackVersionUrl}?fields=configurations/*,Versions/config_types/*',
    'mock': '/data/stacks/HDP-2.2/configurations.json'
  },

  'configs.stack_configs.load.all': {
    'real': '{stackVersionUrl}/services?fields=configurations/*,StackServices/config_types/*',
    'mock': '/data/stacks/HDP-2.2/configurations.json'
  },

  'configs.stack_configs.load.services': {
    'real': '{stackVersionUrl}/services?StackServices/service_name.in({serviceList})&fields=configurations/*,configurations/dependencies/*,StackServices/config_types/*',
    'mock': '/data/stacks/HDP-2.2/configurations.json'
  },

  'configs.stack_configs.load.service': {
    'real': '{stackVersionUrl}/services/{serviceName}?fields=configurations/*,StackServices/config_types/*',
    'mock': '/data/stacks/HDP-2.2/configurations.json'
  },

  /*************************CONFIG VERSIONS*************************************/

  'configs.config_versions.load': {
    'real': '/clusters/{clusterName}/configurations/service_config_versions?service_name={serviceName}&service_config_version={configVersion}&fields=*',
    'mock': '/data/configurations/config_versions.json'
  },

  'configs.config_versions.load.group': {
    'real': '/clusters/{clusterName}/configurations/service_config_versions?service_name={serviceName}&group_id={id}&fields=*',
    'mock': '/data/configurations/config_versions.json'
  },

  'configs.config_versions.load.current_versions': {
    'real': '/clusters/{clusterName}/configurations/service_config_versions?service_name.in({serviceNames})&is_current=true&fields=*',
    'mock': '/data/configurations/config_versions.json'
  },


  'service.load_config_groups': {
    'real': '/clusters/{clusterName}/config_groups?ConfigGroup/tag={serviceName}&fields=*',
    'mock': '/data/configurations/config_group.json'
  },
  'reassign.load_configs': {
    'real': '/clusters/{clusterName}/configurations?{urlParams}',
    'mock': ''
  },

  'reassign.save_configs': {
    'real': '/clusters/{clusterName}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          Clusters: {
            desired_config: {
              "type": data.siteName,
              "properties": data.properties,
              "service_config_version_note": data.service_config_version_note

            }
          }
        })
      }
    }
  },
  'config.cluster': {
    'real': '{stackVersionUrl}/configurations?fields=*',
    'mock': ''
  },
  'config.advanced': {
    'real': '{stackVersionUrl}/services/{serviceName}/configurations?fields=*',
    'mock': '/data/wizard/stack/hdp/version{stackVersion}/{serviceName}.json'
  },
  'config.advanced.multiple.services': {
    'real': '{stackVersionUrl}/services?StackServices/service_name.in({serviceNames})&fields=configurations/*',
    'mock': '/data/wizard/stack/hdp/version{stackVersion}/{serviceName}.json'
  },
  'config.advanced.partial': {
    'real': '{stackVersionUrl}/services/?StackServices/service_name.in({serviceList})&fields=configurations/*{queryFilter}',
    'mock': ''
  },
  'config.config_types': {
    'real': '{stackVersionUrl}/services/{serviceName}?fields=StackServices/config_types',
    'mock': ''
  },
  'config.tags': {
    'real': '/clusters/{clusterName}?fields=Clusters/desired_configs',
    'mock': '/data/clusters/cluster.json'
  },
  'config.tags.site': {
    'real': '/clusters/{clusterName}?fields=Clusters/desired_configs/{site}',
    'mock': ''
  },
  'config.tags_and_groups': {
    'real': '/clusters/{clusterName}?fields=Clusters/desired_configs,config_groups/*{urlParams}',
    'mock': '/data/clusters/tags_and_groups.json'
  },
  'config_groups.all_fields': {
    'real': '/clusters/{clusterName}/config_groups?fields=*',
    'mock': ''
  },
  'config_groups.get_config_group_by_id': {
    'real': '/clusters/{clusterName}/config_groups/{id}',
    'mock': ''
  },
  'config_groups.update_config_group': {
    'real': '/clusters/{clusterName}/config_groups/{id}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify(
          [
            data.configGroup
          ]
        )
      }
    }
  },
  'config.on_site': {
    'real': '/clusters/{clusterName}/configurations?{params}',
    'mock': '/data/configurations/cluster_level_configs.json?{params}'
  },
  'config.host_overrides': {
    'real': '/clusters/{clusterName}/configurations?{params}',
    'mock': '/data/configurations/host_level_overrides_configs.json?{params}'
  },

  'config.tags.selected': {
    'real': '/clusters/{clusterName}/configurations?type.in({tags})',
    'mock': '/data/configuration/cluster_env_site.json'
  },

  'credentials.store.info': {
    'real': '/clusters/{clusterName}?fields=Clusters/credential_store_properties',
    'mock': ''
  },

  'credentials.list': {
    'real': '/clusters/{clusterName}/credentials?fields=Credential/*',
    'mock': ''
  },

  'credentials.get': {
    'real': '/clusters/{clusterName}/credentials/{alias}',
    'mock': ''
  },

  'credentials.create': {
    'real': '/clusters/{clusterName}/credentials/{alias}',
    'mock': '',
    type: 'POST',
    'format': function(data) {
      return {
        data: JSON.stringify({
          Credential: data.resource
        })
      };
    }
  },

  'credentials.update': {
    'real': '/clusters/{clusterName}/credentials/{alias}',
    'mock': '',
    'type': 'PUT',
    'format': function(data) {
      return {
        data: JSON.stringify({
          Credential: data.resource
        })
      };
    }
  },

  'credentials.delete': {
    'real': '/clusters/{clusterName}/credentials/{alias}',
    'mock': '',
    'type':'DELETE'
  },

  'host.host_component.add_new_component': {
    'real': '/clusters/{clusterName}/hosts?Hosts/host_name={hostName}',
    'mock': '/data/wizard/deploy/poll_1.json',
    'format': function (data) {
      return {
        type: 'POST',
        data: data.data
      }
    }
  },

  'host.host_component.add_new_components': {
    'real': '/clusters/{clusterName}/hosts',
    'mock': '/data/wizard/deploy/poll_1.json',
    'format': function (data) {
      return {
        type: 'POST',
        data: data.data
      }
    }
  },

  'host.host_component.delete_components': {
    'real': '/clusters/{clusterName}/host_components',
    'format': function (data) {
      return {
        type: 'DELETE',
        data: data.data
      }
    }
  },

  'host.host_component.slave_desired_admin_state': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}/?fields=HostRoles/desired_admin_state',
    'mock': '/data/hosts/HDP2/decommission_state.json'
  },
  'host.host_component.decommission_status': {
    'real': '/clusters/{clusterName}/services/{serviceName}/components/{componentName}/?fields=ServiceComponentInfo,host_components/HostRoles/state',
    'mock': ''
  },
  'host_components.hbase_regionserver.active': {
    'real': '/clusters/{clusterName}/host_components?HostRoles/component_name=HBASE_REGIONSERVER&HostRoles/maintenance_state=OFF&HostRoles/desired_admin_state=INSERVICE&HostRoles/host_name.in({hostNames})',
    'mock': ''
  },
  'host.host_component.decommission_status_datanode': {
    'real': '/clusters/{clusterName}/host_components?HostRoles/component_name=NAMENODE&HostRoles/host_name.in({hostNames})&fields=metrics/dfs/namenode',
    'mock': '/data/hosts/HDP2/decommission_state.json'
  },
  'host.host_component.decommission_status_regionserver': {
    'real': '/clusters/{clusterName}/host_components?HostRoles/component_name=HBASE_MASTER&HostRoles/host_name={hostName}&fields=metrics/hbase/master/liveRegionServersHosts,metrics/hbase/master/deadRegionServersHosts&minimal_response=true'
  },
  'host.region_servers.in_inservice': {
    'real': '/clusters/{clusterName}/host_components?HostRoles/component_name=HBASE_REGIONSERVER&HostRoles/desired_admin_state=INSERVICE&fields=HostRoles/host_name&minimal_response=true',
    'mock': ''
  },
  'host.host_component.decommission_slave': {
    'real': '/clusters/{clusterName}/requests',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify({
          RequestInfo: {
            'context': data.context,
            'command': data.command,
            'parameters': {
              'slave_type': data.slaveType,
              'excluded_hosts': data.hostName
            },
            'operation_level': {
              level: "HOST_COMPONENT",
              cluster_name: data.clusterName,
              host_name: data.hostName,
              service_name: data.serviceName
            }
          },
          "Requests/resource_filters": [{"service_name": data.serviceName, "component_name": data.componentName}]
        })
      }
    }
  },

  'host.host_component.refresh_configs': {
    'real': '/clusters/{clusterName}/requests',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify({
          "RequestInfo": {
            "command": "CONFIGURE",
            "context": data.context
          },
          "Requests/resource_filters": data.resource_filters
        })
      }
    }
  },

  'hosts.metrics': {
    'real': '/clusters/{clusterName}/hosts?fields={metricName}',
    'mock': '/data/cluster_metrics/cpu_1hr.json'
  },
  'hosts.metrics.host_component': {
    'real': '/clusters/{clusterName}/services/{serviceName}/components/{componentName}?fields=host_components/{metricName}',
    'mock': '/data/cluster_metrics/cpu_1hr.json'
  },
  'service.metrics.flume.channel_fill_percent': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=host_components/metrics/flume/flume/CHANNEL/ChannelFillPercentage[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/flume/channelFillPct.json',
    'testInProduction': true
  },
  'service.metrics.flume.channel_size': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=host_components/metrics/flume/flume/CHANNEL/ChannelSize[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/flume/channelSize.json',
    'testInProduction': true
  },
  'service.metrics.flume.sink_drain_success': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=host_components/metrics/flume/flume/SINK/EventDrainSuccessCount[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/flume/sinkDrainSuccessCount.json',
    'testInProduction': true
  },
  'service.metrics.flume.sink_connection_failed': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=host_components/metrics/flume/flume/SINK/ConnectionFailedCount[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/flume/sinkConnectionFailedCount.json',
    'testInProduction': true
  },
  'service.metrics.flume.source_accepted': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=host_components/metrics/flume/flume/SOURCE/EventAcceptedCount[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/flume/sourceEventAccepted.json',
    'testInProduction': true
  },
  'service.metrics.flume.channel_size_for_all': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=metrics/flume/flume/CHANNEL/ChannelSize/rate[{fromSeconds},{toSeconds},{stepSeconds}]'
  },
  'service.metrics.flume.channel_size_for_all.mma': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=metrics/flume/flume/CHANNEL/ChannelSize/rate/avg[{fromSeconds},{toSeconds},{stepSeconds}],metrics/flume/flume/CHANNEL/ChannelSize/rate/max[{fromSeconds},{toSeconds},{stepSeconds}],metrics/flume/flume/CHANNEL/ChannelSize/rate/min[{fromSeconds},{toSeconds},{stepSeconds}]'
  },
  'service.metrics.flume.channel_size_for_all.sum': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=metrics/flume/flume/CHANNEL/ChannelSize/rate/sum[{fromSeconds},{toSeconds},{stepSeconds}]'
  },
  'service.metrics.flume.gc': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=host_components/metrics/jvm/gcTimeMillis[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/flume/jvmGcTime.json',
    'testInProduction': true
  },
  'service.metrics.flume.jvm_heap_used': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=host_components/metrics/jvm/memHeapUsedM[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/flume/jvmMemHeapUsedM.json',
    'testInProduction': true
  },
  'service.metrics.flume.jvm_threads_runnable': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=host_components/metrics/jvm/threadsRunnable[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/flume/jvmThreadsRunnable.json',
    'testInProduction': true
  },
  'service.metrics.flume.cpu_user': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=host_components/metrics/cpu/cpu_user[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '',
    'testInProduction': true
  },
  'service.metrics.flume.incoming_event_put_successCount': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=metrics/flume/flume/CHANNEL/EventPutSuccessCount/rate[{fromSeconds},{toSeconds},{stepSeconds}]'
  },
  'service.metrics.flume.incoming_event_put_successCount.mma': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=metrics/flume/flume/CHANNEL/EventPutSuccessCount/rate/avg[{fromSeconds},{toSeconds},{stepSeconds}],metrics/flume/flume/CHANNEL/EventPutSuccessCount/rate/max[{fromSeconds},{toSeconds},{stepSeconds}],metrics/flume/flume/CHANNEL/EventPutSuccessCount/rate/min[{fromSeconds},{toSeconds},{stepSeconds}]'
  },
  'service.metrics.flume.incoming_event_put_successCount.sum': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=metrics/flume/flume/CHANNEL/EventPutSuccessCount/rate/sum[{fromSeconds},{toSeconds},{stepSeconds}]'
  },
  'service.metrics.flume.outgoing_event_take_success_count': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=metrics/flume/flume/CHANNEL/EventTakeSuccessCount/rate[{fromSeconds},{toSeconds},{stepSeconds}]'
  },
  'service.metrics.flume.outgoing_event_take_success_count.mma': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=metrics/flume/flume/CHANNEL/EventTakeSuccessCount/rate/avg[{fromSeconds},{toSeconds},{stepSeconds}],metrics/flume/flume/CHANNEL/EventTakeSuccessCount/rate/max[{fromSeconds},{toSeconds},{stepSeconds}],metrics/flume/flume/CHANNEL/EventTakeSuccessCount/rate/min[{fromSeconds},{toSeconds},{stepSeconds}]'
  },
  'service.metrics.flume.outgoing_event_take_success_count.sum': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=metrics/flume/flume/CHANNEL/EventTakeSuccessCount/rate/sum[{fromSeconds},{toSeconds},{stepSeconds}]'
  },
  'service.metrics.hbase.cluster_requests': {
    'real': '/clusters/{clusterName}/services/HBASE/components/HBASE_MASTER?fields=metrics/hbase/master/cluster_requests[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hbase/cluster_requests.json',
    'testInProduction': true
  },
  'service.metrics.hbase.hlog_split_size': {
    'real': '/clusters/{clusterName}/services/HBASE/components/HBASE_MASTER?fields=metrics/hbase/master/splitSize_avg_time[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hbase/hlog_split_size.json',
    'testInProduction': true
  },
  'service.metrics.hbase.hlog_split_time': {
    'real': '/clusters/{clusterName}/services/HBASE/components/HBASE_MASTER?fields=metrics/hbase/master/splitTime_avg_time[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hbase/hlog_split_time.json',
    'testInProduction': true
  },
  'service.metrics.hbase.regionserver_queuesize': {
    'real': '/clusters/{clusterName}/services/HBASE/components/HBASE_REGIONSERVER?fields=metrics/hbase/regionserver/flushQueueSize[{fromSeconds},{toSeconds},{stepSeconds}],metrics/hbase/regionserver/compactionQueueSize[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hbase/regionserver_queuesize.json',
    'testInProduction': true
  },
  'service.metrics.hbase.regionserver_regions': {
    'real': '/clusters/{clusterName}/services/HBASE/components/HBASE_REGIONSERVER?fields=metrics/hbase/regionserver/regions[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hbase/regionserver_regions.json',
    'testInProduction': true
  },
  'service.metrics.hbase.regionserver_rw_requests': {
    'real': '/clusters/{clusterName}/services/HBASE/components/HBASE_REGIONSERVER?fields=metrics/hbase/regionserver/readRequestsCount[{fromSeconds},{toSeconds},{stepSeconds}],metrics/hbase/regionserver/writeRequestsCount[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hbase/regionserver_rw_requests.json',
    'testInProduction': true
  },
  'service.metrics.ambari_metrics.master.average_load': {
    'real': '/clusters/{clusterName}/services/AMBARI_METRICS/components/METRICS_COLLECTOR?fields=metrics/hbase/master/AverageLoad[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/ambari_metrics/master_average_load.json',
    'testInProduction': true
  },
  'service.metrics.ambari_metrics.region_server.store_files': {
    'real': '/clusters/{clusterName}/services/AMBARI_METRICS/components/METRICS_COLLECTOR?fields=metrics/hbase/regionserver/storefiles[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/ambari_metrics/regionserver_store_files.json',
    'testInProduction': true
  },
  'service.metrics.ambari_metrics.region_server.regions': {
    'real': '/clusters/{clusterName}/services/AMBARI_METRICS/components/METRICS_COLLECTOR?fields=metrics/hbase/regionserver/regions[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/ambari_metrics/regionserver_regions.json',
    'testInProduction': true
  },
  'service.metrics.ambari_metrics.region_server.request': {
    'real': '/clusters/{clusterName}/services/AMBARI_METRICS/components/METRICS_COLLECTOR?fields=metrics/hbase/regionserver/requests._rate[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/ambari_metrics/regionserver_requests.json',
    'testInProduction': true
  },
  'service.metrics.ambari_metrics.region_server.block_cache_hit_percent': {
    'real': '/clusters/{clusterName}/services/AMBARI_METRICS/components/METRICS_COLLECTOR?fields=metrics/hbase/regionserver/blockCacheHitPercent[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/ambari_metrics/regionserver_blockcache_hitpercent.json',
    'testInProduction': true
  },
  'service.metrics.ambari_metrics.region_server.compaction_queue_size': {
    'real': '/clusters/{clusterName}/services/AMBARI_METRICS/components/METRICS_COLLECTOR?fields=metrics/hbase/regionserver/compactionQueueSize[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/ambari_metrics/regionserver_compaction_queue_size.json',
    'testInProduction': true
  },
  'service.metrics.ambari_metrics.aggregated': {
    'real': '/clusters/{clusterName}/services/AMBARI_METRICS/components/METRICS_COLLECTOR?fields={fields}',
    'mock': '/data/services/metrics/ambari_metrics/master_average_load.json',
    'testInProduction': true
  },
  'service.metrics.hdfs.block_status': {
    'real': '/clusters/{clusterName}/hosts/{nameNodeName}/host_components/NAMENODE?fields=metrics/dfs/FSNamesystem/PendingReplicationBlocks[{fromSeconds},{toSeconds},{stepSeconds}],metrics/dfs/FSNamesystem/UnderReplicatedBlocks[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hdfs/block_status.json',
    'testInProduction': true
  },
  'service.metrics.hdfs.file_operations': {
    'real': '/clusters/{clusterName}/hosts/{nameNodeName}/host_components/NAMENODE?fields=metrics/dfs/namenode/FileInfoOps[{fromSeconds},{toSeconds},{stepSeconds}],metrics/dfs/namenode/CreateFileOps[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hdfs/file_operations.json',
    'testInProduction': true
  },
  'service.metrics.hdfs.gc': {
    'real': '/clusters/{clusterName}/hosts/{nameNodeName}/host_components/NAMENODE?fields=metrics/jvm/gcTimeMillis[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hdfs/gc.json',
    'testInProduction': true
  },
  'service.metrics.hdfs.io': {
    'real': '/clusters/{clusterName}/services/HDFS/components/DATANODE?fields=metrics/dfs/datanode/bytes_written[{fromSeconds},{toSeconds},{stepSeconds}],metrics/dfs/datanode/bytes_read[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hdfs/io.json',
    'testInProduction': true
  },
  'service.metrics.hdfs.jvm_heap': {
    'real': '/clusters/{clusterName}/hosts/{nameNodeName}/host_components/NAMENODE?fields=metrics/jvm/memNonHeapUsedM[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/memNonHeapCommittedM[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/memHeapUsedM[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/memHeapCommittedM[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hdfs/jvm_heap.json',
    'testInProduction': true
  },
  'service.metrics.hdfs.jvm_threads': {
    'real': '/clusters/{clusterName}/hosts/{nameNodeName}/host_components/NAMENODE?fields=metrics/jvm/threadsRunnable[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/threadsBlocked[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/threadsWaiting[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/threadsTimedWaiting[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hdfs/jvm_threads.json',
    'testInProduction': true
  },
  'service.metrics.hdfs.rpc': {
    'real': '/clusters/{clusterName}/hosts/{nameNodeName}/host_components/NAMENODE?fields=metrics/rpc/client/RpcQueueTime_avg_time[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hdfs/rpc.json',
    'testInProduction': true
  },
  'service.metrics.hdfs.space_utilization': {
    'real': '/clusters/{clusterName}/hosts/{nameNodeName}/host_components/NAMENODE?fields=metrics/dfs/FSNamesystem/CapacityRemaining[{fromSeconds},{toSeconds},{stepSeconds}],metrics/dfs/FSNamesystem/CapacityUsed[{fromSeconds},{toSeconds},{stepSeconds}],metrics/dfs/FSNamesystem/CapacityTotal[{fromSeconds},{toSeconds},{stepSeconds}],metrics/dfs/FSNamesystem/CapacityNonDFSUsed[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hdfs/space_utilization.json',
    'testInProduction': true
  },
  'service.metrics.yarn.gc': {
    'real': '/clusters/{clusterName}/hosts/{resourceManager}/host_components/RESOURCEMANAGER?fields=metrics/jvm/gcTimeMillis[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/yarn/gc.json',
    'testInProduction': true
  },
  'service.metrics.yarn.jobs_threads': {
    'real': '/clusters/{clusterName}/hosts/{resourceManager}/host_components/RESOURCEMANAGER?fields=metrics/jvm/threadsRunnable[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/threadsBlocked[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/threadsWaiting[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/threadsTimedWaiting[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/yarn/jvm_threads.json',
    'testInProduction': true
  },
  'service.metrics.yarn.rpc': {
    'real': '/clusters/{clusterName}/hosts/{resourceManager}/host_components/RESOURCEMANAGER?fields=metrics/rpc/RpcQueueTime_avg_time[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/yarn/rpc.json',
    'testInProduction': true
  },
  'service.metrics.yarn.jobs_heap': {
    'real': '/clusters/{clusterName}/hosts/{resourceManager}/host_components/RESOURCEMANAGER?fields=metrics/jvm/memNonHeapUsedM[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/memNonHeapCommittedM[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/memHeapUsedM[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/memHeapCommittedM[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/yarn/jvm_heap.json',
    'testInProduction': true
  },
  'service.metrics.yarn.queue.allocated': {
    'real': '/clusters/{clusterName}/hosts/{resourceManager}/host_components/RESOURCEMANAGER?fields=metrics/yarn/Queue/root/AvailableMB[{fromSeconds},{toSeconds},{stepSeconds}],metrics/yarn/Queue/root/PendingMB[{fromSeconds},{toSeconds},{stepSeconds}],metrics/yarn/Queue/root/AllocatedMB[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '',
    'testInProduction': true
  },
  'service.metrics.yarn.queue.allocated.container': {
    'real': '/clusters/{clusterName}/hosts/{resourceManager}/host_components/RESOURCEMANAGER?fields=metrics/yarn/Queue/root/AllocatedContainers[{fromSeconds},{toSeconds},{stepSeconds}],metrics/yarn/Queue/root/ReservedContainers[{fromSeconds},{toSeconds},{stepSeconds}],metrics/yarn/Queue/root/PendingContainers[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '',
    'testInProduction': true
  },
  'service.metrics.yarn.node.manager.statuses': {
    'real': '/clusters/{clusterName}/hosts/{resourceManager}/host_components/RESOURCEMANAGER?fields=metrics/yarn/ClusterMetrics/NumActiveNMs[{fromSeconds},{toSeconds},{stepSeconds}],metrics/yarn/ClusterMetrics/NumDecommissionedNMs[{fromSeconds},{toSeconds},{stepSeconds}],metrics/yarn/ClusterMetrics/NumLostNMs[{fromSeconds},{toSeconds},{stepSeconds}],metrics/yarn/ClusterMetrics/NumRebootedNMs[{fromSeconds},{toSeconds},{stepSeconds}],metrics/yarn/ClusterMetrics/NumUnhealthyNMs[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '',
    'testInProduction': true
  },
  'service.metrics.yarn.queue.memory.resource': {
    'real': '/clusters/{clusterName}/hosts/{resourceManager}/host_components/RESOURCEMANAGER?fields=',
    'mock': '',
    'format': function (data, opt) {
      var field1 = 'metrics/yarn/Queue/{queueName}/AllocatedMB[{fromSeconds},{toSeconds},{stepSeconds}]';
      var field2 = 'metrics/yarn/Queue/{queueName}/AvailableMB[{fromSeconds},{toSeconds},{stepSeconds}]';
      if (opt.url != null && data.queueNames != null && data.queueNames.length > 0) {
        data.queueNames.forEach(function (q) {
          data.queueName = q;
          opt.url += (formatUrl(field1, data) + ",");
          opt.url += (formatUrl(field2, data) + ",");
        });
      } else {
        opt.url += (formatUrl(field1, data) + ",");
        opt.url += (formatUrl(field2, data) + ",");
      }
    },
    'testInProduction': true
  },
  'service.metrics.yarn.queue.apps.states.current': {
    'real': '/clusters/{clusterName}/hosts/{resourceManager}/host_components/RESOURCEMANAGER?fields=metrics/yarn/Queue/root/AppsPending[{fromSeconds},{toSeconds},{stepSeconds}],metrics/yarn/Queue/root/AppsRunning[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '',
    'testInProduction': true
  },
  'service.metrics.yarn.queue.apps.states.finished': {
    'real': '/clusters/{clusterName}/hosts/{resourceManager}/host_components/RESOURCEMANAGER?fields=metrics/yarn/Queue/root/AppsKilled[{fromSeconds},{toSeconds},{stepSeconds}],metrics/yarn/Queue/root/AppsFailed[{fromSeconds},{toSeconds},{stepSeconds}],metrics/yarn/Queue/root/AppsSubmitted[{fromSeconds},{toSeconds},{stepSeconds}],metrics/yarn/Queue/root/AppsCompleted[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '',
    'testInProduction': true
  },
  'service.metrics.kafka.broker.topic': {
    'real': '/clusters/{clusterName}/services/KAFKA/components/KAFKA_BROKER?fields=metrics/kafka/server/BrokerTopicMetrics/AllTopicsBytesInPerSec/1MinuteRate[{fromSeconds},{toSeconds},{stepSeconds}],metrics/kafka/server/BrokerTopicMetrics/AllTopicsBytesOutPerSec/1MinuteRate[{fromSeconds},{toSeconds},{stepSeconds}],metrics/kafka/server/BrokerTopicMetrics/AllTopicsMessagesInPerSec/1MinuteRate[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': ''
  },
  'service.metrics.kafka.controller.KafkaController': {
    'real': '/clusters/{clusterName}/services/KAFKA/components/KAFKA_BROKER?fields=metrics/kafka/controller/KafkaController/ActiveControllerCount[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': ''
  },
  'service.metrics.kafka.controller.ControllerStats': {
    'real': '/clusters/{clusterName}/services/KAFKA/components/KAFKA_BROKER?fields=metrics/kafka/controller/ControllerStats/LeaderElectionRateAndTimeMs/1MinuteRate[{fromSeconds},{toSeconds},{stepSeconds}],metrics/kafka/controller/ControllerStats/UncleanLeaderElectionsPerSec/1MinuteRate[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': ''
  },
  'service.metrics.kafka.log.LogFlushStats': {
    'real': '/clusters/{clusterName}/services/KAFKA/components/KAFKA_BROKER?fields=metrics/kafka/log/LogFlushStats/LogFlushRateAndTimeMs/1MinuteRate[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': ''
  },
  'service.metrics.kafka.server.ReplicaManager': {
    'real': '/clusters/{clusterName}/services/KAFKA/components/KAFKA_BROKER?fields=metrics/kafka/server/ReplicaManager/PartitionCount[{fromSeconds},{toSeconds},{stepSeconds}],metrics/kafka/server/ReplicaManager/UnderReplicatedPartitions[{fromSeconds},{toSeconds},{stepSeconds}],metrics/kafka/server/BrokerTopicMetrics/ReplicaManager/LeaderCount[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': ''
  },
  'service.metrics.kafka.server.ReplicaFetcherManager': {
    'real': '/clusters/{clusterName}/services/KAFKA/components/KAFKA_BROKER?fields=metrics/kafka/server/ReplicaFetcherManager/Replica-MaxLag[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': ''
  },
  'service.metrics.storm.nimbus': {
    'real': '/clusters/{clusterName}/services/STORM/components/NIMBUS?fields={metricsTemplate}',
    'mock': ''
  },
  'dashboard.cluster_metrics.cpu': {
    'real': '/clusters/{clusterName}?fields=metrics/cpu/Nice._avg[{fromSeconds},{toSeconds},{stepSeconds}],metrics/cpu/System._avg[{fromSeconds},{toSeconds},{stepSeconds}],metrics/cpu/User._avg[{fromSeconds},{toSeconds},{stepSeconds}],metrics/cpu/Idle._avg[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/cluster_metrics/cpu_1hr.json',
    'testInProduction': true
  },
  'dashboard.cluster_metrics.load': {
    'real': '/clusters/{clusterName}/?fields=metrics/load/1-min._avg[{fromSeconds},{toSeconds},{stepSeconds}],metrics/load/CPUs._avg[{fromSeconds},{toSeconds},{stepSeconds}],metrics/load/Nodes._avg[{fromSeconds},{toSeconds},{stepSeconds}],metrics/load/Procs._avg[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/cluster_metrics/load_1hr.json',
    'testInProduction': true
  },
  'dashboard.cluster_metrics.memory': {
    'real': '/clusters/{clusterName}/?fields=metrics/memory/Buffer._avg[{fromSeconds},{toSeconds},{stepSeconds}],metrics/memory/Cache._avg[{fromSeconds},{toSeconds},{stepSeconds}],metrics/memory/Share._avg[{fromSeconds},{toSeconds},{stepSeconds}],metrics/memory/Swap._avg[{fromSeconds},{toSeconds},{stepSeconds}],metrics/memory/Total._avg[{fromSeconds},{toSeconds},{stepSeconds}],metrics/memory/Use._avg[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/cluster_metrics/memory_1hr.json',
    'testInProduction': true
  },
  'dashboard.cluster_metrics.network': {
    'real': '/clusters/{clusterName}/?fields=metrics/network/In._avg[{fromSeconds},{toSeconds},{stepSeconds}],metrics/network/Out._avg[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/cluster_metrics/network_1hr.json',
    'testInProduction': true
  },
  'host.metrics.aggregated': {
    'real': '/clusters/{clusterName}/hosts/{hostName}?fields={fields}',
    'mock': '/data/hosts/metrics/cpu.json'
  },
  'host.metrics.cpu': {
    'real': '/clusters/{clusterName}/hosts/{hostName}?fields=metrics/cpu/cpu_user[{fromSeconds},{toSeconds},{stepSeconds}],metrics/cpu/cpu_wio[{fromSeconds},{toSeconds},{stepSeconds}],metrics/cpu/cpu_nice[{fromSeconds},{toSeconds},{stepSeconds}],metrics/cpu/cpu_aidle[{fromSeconds},{toSeconds},{stepSeconds}],metrics/cpu/cpu_system[{fromSeconds},{toSeconds},{stepSeconds}],metrics/cpu/cpu_idle[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/hosts/metrics/cpu.json',
    'testInProduction': true
  },
  'host.metrics.disk': {
    'real': '/clusters/{clusterName}/hosts/{hostName}?fields=metrics/disk/disk_total[{fromSeconds},{toSeconds},{stepSeconds}],metrics/disk/disk_free[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/hosts/metrics/disk.json',
    'testInProduction': true
  },
  'host.metrics.load': {
    'real': '/clusters/{clusterName}/hosts/{hostName}?fields=metrics/load/load_fifteen[{fromSeconds},{toSeconds},{stepSeconds}],metrics/load/load_one[{fromSeconds},{toSeconds},{stepSeconds}],metrics/load/load_five[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/hosts/metrics/load.json',
    'testInProduction': true
  },
  'host.metrics.memory': {
    'real': '/clusters/{clusterName}/hosts/{hostName}?fields=metrics/memory/swap_free[{fromSeconds},{toSeconds},{stepSeconds}],metrics/memory/mem_shared[{fromSeconds},{toSeconds},{stepSeconds}],metrics/memory/mem_free[{fromSeconds},{toSeconds},{stepSeconds}],metrics/memory/mem_cached[{fromSeconds},{toSeconds},{stepSeconds}],metrics/memory/mem_buffers[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/hosts/metrics/memory.json',
    'testInProduction': true
  },
  'host.metrics.network': {
    'real': '/clusters/{clusterName}/hosts/{hostName}?fields=metrics/network/bytes_in[{fromSeconds},{toSeconds},{stepSeconds}],metrics/network/bytes_out[{fromSeconds},{toSeconds},{stepSeconds}],metrics/network/pkts_in[{fromSeconds},{toSeconds},{stepSeconds}],metrics/network/pkts_out[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/hosts/metrics/network.json',
    'testInProduction': true
  },
  'host.metrics.processes': {
    'real': '/clusters/{clusterName}/hosts/{hostName}?fields=metrics/process/proc_total[{fromSeconds},{toSeconds},{stepSeconds}],metrics/process/proc_run[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/hosts/metrics/processes.json',
    'testInProduction': true
  },
  'admin.security_status': {
    'real': '/clusters/{clusterName}?fields=Clusters/security_type',
    'mock': '',
    'format': function () {
      return {
        timeout: 10000
      };
    }
  },
  'cluster.load_cluster_name': {
    'real': '/clusters?fields=Clusters/security_type,Clusters/version,Clusters/cluster_id',
    'mock': '/data/clusters/info.json'
  },
  'cluster.load_last_upgrade': {
    'real': '/clusters/{clusterName}/upgrades?fields=Upgrade/*',
    'mock': '/data/stack_versions/upgrades.json'
  },
  'cluster.update_upgrade_version': {
    'real': '/stacks/{stackName}/versions?fields=services/StackServices,Versions',
    'mock': '/data/wizard/stack/stacks.json',
    'format': function (data) {
      return {
        data: data.data
      };
    }
  },
  'cluster.load_repositories': {
    'real': '/stacks/{stackName}/versions/{stackVersion}/operating_systems?fields=repositories/*,OperatingSystems/*',
    'mock': '/data/stacks/HDP-2.1/operating_systems.json',
    'format': function (data) {
      return {
        data: data.data
      };
    }
  },
  'cluster.load_repo_version': {
    'real': '/stacks/{stackName}/versions?fields=repository_versions/operating_systems/repositories/*,repository_versions/operating_systems/OperatingSystems/*,repository_versions/RepositoryVersions/display_name&repository_versions/RepositoryVersions/repository_version={repositoryVersion}',
    'mock': ''
  },
  'cluster.load_detailed_repo_version': {
    'real': '/clusters/{clusterName}/stack_versions?fields=repository_versions/RepositoryVersions/repository_version,ClusterStackVersions/stack,ClusterStackVersions/version&minimal_response=true',
    'mock': '/data/stack_versions/stack_version_all.json'
  },
  'cluster.load_current_repo_stack_services': {
    'real': '/clusters/{clusterName}/stack_versions?fields=repository_versions/RepositoryVersions/stack_services,ClusterStackVersions/stack,ClusterStackVersions/version',
    'mock': '/data/stack_versions/stack_version_all.json'
  },
  'cluster.save_provisioning_state': {
    'real': '/clusters/{clusterName}',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          "Clusters": {
            "provisioning_state": data.state
          }
        })
      };
    }
  },

  'cluster.logging.searchEngine': {
    real: '/clusters/{clusterName}/logging/searchEngine?{query}',
    mock: ''
  },
  'admin.high_availability.polling': {
    'real': '/clusters/{clusterName}/requests/{requestId}?fields=tasks/*,Requests/*',
    'mock': '/data/background_operations/host_upgrade_tasks.json'
  },
  'admin.high_availability.getNnCheckPointStatus': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/NAMENODE',
    'mock': ''
  },
  'admin.high_availability.getNnCheckPointsStatuses': {
    'real': '/clusters/{clusterName}/host_components?HostRoles/component_name=NAMENODE&HostRoles/host_name.in({hostNames})&fields=HostRoles/desired_state,metrics/dfs/namenode&minimal_response=true',
    'mock': ''
  },
  'admin.high_availability.getJnCheckPointStatus': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/JOURNALNODE?fields=metrics',
    'mock': ''
  },
  'admin.high_availability.getHostComponent': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}',
    'mock': ''
  },
  'common.create_component': {
    'real': '/clusters/{clusterName}/services?ServiceInfo/service_name={serviceName}',
    'mock': '',
    'type': 'POST',
    'format': function (data) {
      return {
        data: JSON.stringify({
          "components": [
            {
              "ServiceComponentInfo": {
                "component_name": data.componentName
              }
            }
          ]
        })
      }
    }
  },
  'admin.high_availability.load_configs': {
    'real': '/clusters/{clusterName}/configurations?(type=core-site&tag={coreSiteTag})|(type=hdfs-site&tag={hdfsSiteTag})',
    'mock': ''
  },
  'admin.save_configs': {
    'real': '/clusters/{clusterName}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          Clusters: {
            desired_config: {
              "type": data.siteName,
              "properties": data.properties
            }
          }
        })
      }
    }
  },
  'admin.high_availability.load_hbase_configs': {
    'real': '/clusters/{clusterName}/configurations?type=hbase-site&tag={hbaseSiteTag}',
    'mock': ''
  },
  'admin.security.cluster_configs': {
    'real': '/clusters/{clusterName}',
    'mock': '',
    'format': function () {
      return {
        timeout: 10000
      };
    }
  },
  'admin.security.cluster_configs.kerberos': {
    'real': '/clusters/{clusterName}/configurations/service_config_versions?service_name=KERBEROS&is_current=true',
    'mock': '',
    'format': function () {
      return {
        timeout: 10000
      };
    }
  },
  'admin.get.all_configurations': {
    'real': '/clusters/{clusterName}/configurations?{urlParams}',
    'mock': '',
    'format': function () {
      return {
        timeout: 10000
      };
    }
  },

  'kerberos.session.state': {
    'real': '/clusters/{clusterName}/services/KERBEROS?fields=Services/attributes/kdc_validation_result,Services/attributes/kdc_validation_failure_details',
    'mock': ''
  },

  'admin.kerberize.cluster': {
    'type': 'PUT',
    'real': '/clusters/{clusterName}',
    'mock': '/data/wizard/kerberos/kerberize_cluster.json',
    'format' : function (data) {
      return {
        data: JSON.stringify(data.data)
      }
    }
  },
  'admin.unkerberize.cluster': {
    'type': 'PUT',
    'real': '/clusters/{clusterName}',
    'format': function (data) {
      return {
        data: JSON.stringify({
          Clusters: {
            security_type: "NONE"
          }
        })
      }
    }
  },

  'admin.kerberize.cluster.force': {
    'type': 'PUT',
    'real': '/clusters/{clusterName}?force_toggle_kerberos=true',
    'mock': '/data/wizard/kerberos/kerberize_cluster.json',
    'format': function (data) {
      return {
        data: JSON.stringify({
          Clusters: {
            security_type: "KERBEROS"
          }
        })
      }
    }
  },

  'admin.unkerberize.cluster.skip': {
    'type': 'PUT',
    'real': '/clusters/{clusterName}?manage_kerberos_identities=false',
    'mock': '',
    'format': function (data) {
      return {
        data: JSON.stringify({
          Clusters: {
            security_type: "NONE"
          }
        })
      }
    }
  },

  'get.cluster.artifact': {
    'real': '/clusters/{clusterName}/artifacts/{artifactName}?fields=artifact_data',
    'mock': '/data/wizard/kerberos/stack_descriptors.json'
  },
  'admin.kerberize.stack_descriptor': {
    'real': '/clusters/{clusterName}/kerberos_descriptors/STACK',
    'mock': '/data/wizard/kerberos/stack_descriptors.json'
  },
  'admin.kerberize.cluster_descriptor_artifact': {
    'real': '/clusters/{clusterName}/artifacts/kerberos_descriptor?fields=artifact_data',
    'mock': '/data/wizard/kerberos/stack_descriptors.json'
  },
  'admin.kerberize.cluster_descriptor': {
    'real': '/clusters/{clusterName}/kerberos_descriptors/COMPOSITE{queryParams}',
    'mock': '/data/wizard/kerberos/stack_descriptors.json'
  },
  'admin.kerberize.cluster_descriptor.stack': {
    'real': '/clusters/{clusterName}/kerberos_descriptors/STACK',
    'mock': '/data/wizard/kerberos/stack_descriptors.json'
  },
  'admin.kerberos.cluster.artifact.create': {
    'type': 'POST',
    'real': '/clusters/{clusterName}/artifacts/{artifactName}',
    'format' : function (data) {
      return {
        data: JSON.stringify(data.data)
      }
    }
  },
  'admin.kerberos.cluster.artifact.update': {
    'type': 'PUT',
    'real': '/clusters/{clusterName}/artifacts/{artifactName}',
    'format' : function (data) {
      return {
        data: JSON.stringify(data.data)
      }
    }
  },
  'admin.kerberos.cluster.csv': {
    'real': '/clusters/{clusterName}/kerberos_identities?fields=*&format=csv',
    'mock': '',
    'format': function(data) {
      return {
        dataType: 'text',
        data: data.data
      }
    }
  },
  'admin.poll.kerberize.cluster.request': {
    'real': '/clusters/{clusterName}/requests/{requestId}?fields=stages/Stage/context,stages/Stage/status,stages/Stage/progress_percent,stages/tasks/*,Requests/*',
    'mock': '/data/wizard/kerberos/kerberize_cluster.json'
  },
  'admin.stack_upgrade.run_upgrade': {
    'real': '/clusters/{clusterName}',
    'mock': '',
    'format': function (data) {
      return {
        type: 'PUT',
        data: data.data
      };
    }
  },
  'admin.user.create': {
    'real': '/users/{user}',
    'mock': '/data/users/users.json',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify(data.data)
      }
    }
  },

  'admin.user.edit': {
    'real': '/users/{user}',
    'mock': '/data/users/users.json',
    'format': function (data) {
      return {
        type: 'PUT',
        data: data.data
      }
    }
  },

  'admin.stack_upgrade.do_poll': {
    'real': '/clusters/{cluster}/requests/{requestId}?fields=tasks/*',
    'mock': '/data/wizard/{mock}'
  },
  'admin.upgrade.data': {
    'real': '/clusters/{clusterName}/upgrades/{id}?upgrade_groups/UpgradeGroup/status!=PENDING&fields=' +
    'Upgrade/progress_percent,Upgrade/request_context,Upgrade/request_status,Upgrade/direction,Upgrade/downgrade_allowed,' +
    'upgrade_groups/UpgradeGroup,' +
    'Upgrade/*,' +
    'upgrade_groups/upgrade_items/UpgradeItem/status,' +
    'upgrade_groups/upgrade_items/UpgradeItem/display_status,' +
    'upgrade_groups/upgrade_items/UpgradeItem/context,' +
    'upgrade_groups/upgrade_items/UpgradeItem/group_id,' +
    'upgrade_groups/upgrade_items/UpgradeItem/progress_percent,' +
    'upgrade_groups/upgrade_items/UpgradeItem/request_id,' +
    'upgrade_groups/upgrade_items/UpgradeItem/skippable,' +
    'upgrade_groups/upgrade_items/UpgradeItem/stage_id,' +
    'upgrade_groups/upgrade_items/UpgradeItem/text&' +
    'minimal_response=true',
    'mock': '/data/stack_versions/upgrade.json'
  },
  'admin.upgrade.state': {
    'real': '/clusters/{clusterName}/upgrades/{id}?fields=Upgrade/*',
    'mock': '/data/stack_versions/upgrade.json'
  },
  'admin.upgrade.finalizeContext': {
    'real': '/clusters/{clusterName}/upgrades/{id}?upgrade_groups/upgrade_items/UpgradeItem/status=HOLDING&fields=upgrade_groups/upgrade_items/UpgradeItem/context',
    'mock': '/data/stack_versions/upgrade.json'
  },
  'admin.upgrade.upgrade_item': {
    'real': '/clusters/{clusterName}/upgrades/{upgradeId}/upgrade_groups/{groupId}/upgrade_items/{stageId}?fields=' +
    'UpgradeItem/group_id,' +
    'UpgradeItem/stage_id,' +
    'tasks/Tasks/command_detail,' +
    'tasks/Tasks/host_name,' +
    'tasks/Tasks/role,' +
    'tasks/Tasks/request_id,' +
    'tasks/Tasks/stage_id,' +
    'tasks/Tasks/status,' +
    'tasks/Tasks/structured_out&' +
    'minimal_response=true',
    'mock': '/data/stack_versions/upgrade_item.json'
  },
  'admin.upgrade.upgrade_task': {
    'real': '/clusters/{clusterName}/upgrades/{upgradeId}/upgrade_groups/{groupId}/upgrade_items/{stageId}/tasks/{taskId}',
    'mock': ''
  },
  'admin.upgrade.service_checks': {
    'real': '/clusters/{clusterName}/upgrades/{upgradeId}/upgrade_groups?upgrade_items/UpgradeItem/status=COMPLETED&upgrade_items/tasks/Tasks/status.in(FAILED,ABORTED,TIMEDOUT)&upgrade_items/tasks/Tasks/command=SERVICE_CHECK&fields=upgrade_items/tasks/Tasks/command_detail,tasks/Tasks/ops_display_name,upgrade_items/tasks/Tasks/status&minimal_response=true'
  },
  'admin.upgrade.update.options': {
    'real': '/clusters/{clusterName}/upgrades/{upgradeId}',
    'mock': '/data/stack_versions/start_upgrade.json',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          "Upgrade": {
            "skip_failures": data.skipComponentFailures,
            "skip_service_check_failures": data.skipSCFailures
          }
        })
      }
    }
  },
  'admin.upgrade.start': {
    'real': '/clusters/{clusterName}/upgrades',
    'mock': '/data/stack_versions/start_upgrade.json',
    'type': 'POST',
    'format': function (data) {
      return {
        timeout : 600000,
        data: JSON.stringify({
          "Upgrade": {
            "repository_version_id": data.id,
            "upgrade_type": data.type,
            "skip_failures": data.skipComponentFailures,
            "skip_service_check_failures": data.skipSCFailures,
            "direction": "UPGRADE"
          }
        })
      }
    }
  },
  'admin.downgrade.start': {
    'real': '/clusters/{clusterName}/upgrades',
    'mock': '/data/stack_versions/start_upgrade.json',
    'type': 'POST',
    'format': function (data) {
      return {
        data: JSON.stringify({
          "Upgrade": {
            "upgrade_type": data.upgradeType,
            "direction": "DOWNGRADE"
          }
        })
      }
    }
  },
  'admin.upgrade.abort': {
    'real': '/clusters/{clusterName}/upgrades/{upgradeId}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          "Upgrade": {
            "request_status": "ABORTED",
            "suspended": "false"
          }
        })
      }
    }
  },
  'admin.upgrade.suspend': {
    'real': '/clusters/{clusterName}/upgrades/{upgradeId}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          "Upgrade": {
            "request_status": "ABORTED",
            "suspended": "true"
          }
        })
      }
    }
  },
  'admin.upgrade.retry': {
    'real': '/clusters/{clusterName}/upgrades/{upgradeId}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          "Upgrade": {
            "request_status": "PENDING"
          }
        })
      }
    }
  },
  'admin.upgrade.revert': {
    'real': '/clusters/{clusterName}/upgrades',
    'mock': '/data/stack_versions/start_upgrade.json',
    'type': 'POST',
    'format': function (data) {
      return {
        timeout : 600000,
        data: JSON.stringify({
          "Upgrade": {
            "revert_upgrade_id": data.upgradeId
          }
        })
      }
    }
  },
  'admin.upgrade.upgradeItem.setState': {
    'real': '/clusters/{clusterName}/upgrades/{upgradeId}/upgrade_groups/{groupId}/upgrade_items/{itemId}',
    'mock': '',
    type: 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          "UpgradeItem" : {
            "status" : data.status
          }
        })
      };
    }
  },
  'admin.stack_versions.all': {
    'real': '/clusters/{clusterName}/stack_versions?fields=ClusterStackVersions/*,repository_versions/RepositoryVersions/*&minimal_response=true',
    'mock': '/data/stack_versions/stack_version_all.json'
  },
  'admin.stack_version.install.repo_version': {
    'real': '/clusters/{clusterName}/stack_versions',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify({
          ClusterStackVersions: data.ClusterStackVersions
        })
      }
    },
    'mock': ''
  },

  'admin.stack_versions.edit.repo': {
    'real': '/stacks/{stackName}/versions/{stackVersion}/repository_versions/{repoVersionId}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify(data.repoVersion)
      }
    }
  },
  'admin.stack_versions.validate.repo': {
    'real': '/stacks/{stackName}/versions/{stackVersion}/operating_systems/{osType}/repositories/{repoId}?validate_only=true',
    'mock': '',
    'type': 'POST',
    'format': function (data) {
      return {
        data: JSON.stringify({
          "Repositories": {
            "base_url": data.baseUrl,
            "repo_name": data.repoName
          }
        })
      }
    }
  },

  'admin.stack_versions.discard': {
    'real': '/stacks/{stackName}/versions/{stackVersion}/repository_versions/{id}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          "RepositoryVersions":{
            "hidden": "true"
          }
        })
      }
    }
  },

  'admin.upgrade.pre_upgrade_check': {
    'real': '/clusters/{clusterName}/rolling_upgrades_check?fields=*&UpgradeChecks/repository_version_id={id}&UpgradeChecks/upgrade_type={type}',
    'mock': '/data/stack_versions/pre_upgrade_check.json'
  },

  'admin.upgrade.get_supported_upgradeTypes': {
    'real': '/stacks/{stackName}/versions/{stackVersion}/compatible_repository_versions?CompatibleRepositoryVersions/repository_version={toVersion}',
    'mock': '/data/stack_versions/supported_upgrade_types.json'
  },

  'admin.upgrade.get_compatible_versions': {
    'real': '/stacks/{stackName}/versions/{stackVersion}/compatible_repository_versions?fields=CompatibleRepositoryVersions/repository_version&minimal_response=true',
    'mock': '/data/stack_versions/supported_upgrade_types.json'
  },

  'admin.kerberos_security.checks': {
    //TODO when api will be known
    'real': '',
    'mock': '/data/stack_versions/pre_upgrade_check.json'
  },

  'admin.kerberos_security.test_connection': {
    'real': '/kdc_check/{kdcHostname}',
    'mock': '',
    'format': function () {
      return {
        dataType: 'text'
      };
    }
  },

  'admin.kerberos_security.regenerate_keytabs': {
    'real': '/clusters/{clusterName}?regenerate_keytabs={type}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          "Clusters" : {
            "security_type" : "KERBEROS"
          }
        })
      }
    }
  },

  'admin.kerberos_security.regenerate_keytabs.service' : {
    'real': '/clusters/{clusterName}?regenerate_keytabs=all&regenerate_components={serviceName}&config_update_policy=none',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          "Clusters" : {
            "security_type" : "KERBEROS"
          }
        })
      }
    }
  },

  'admin.kerberos_security.regenerate_keytabs.host' : {
    'real': '/clusters/{clusterName}?regenerate_keytabs=all&regenerate_hosts={hostName}&config_update_policy=none',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          "Clusters" : {
            "security_type" : "KERBEROS"
          }
        })
      }
    }
  },

  'wizard.step1.post_version_definition_file.xml': {
    'real': '/version_definitions?dry_run=true',
    'mock': '',
    'format': function (data) {
      return {
        headers: {
          'X-Requested-By': 'ambari',
          'Content-Type': 'text/xml'
        },
        type: 'POST',
        data: data.data
      }
    }
  },
  'wizard.step1.post_version_definition_file.url': {
    'real': '/version_definitions?dry_run=true',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify(data.data)
      }
    }
  },
  'wizard.step8.post_version_definition_file.xml': {
    'real': '/version_definitions',
    'mock': '',
    'format': function (data) {
      return {
        headers: {
          'X-Requested-By': 'ambari',
          'Content-Type': 'text/xml'
        },
        type: 'POST',
        data: data.data
      }
    }
  },
  'wizard.step8.post_version_definition_file': {
    'real': '/version_definitions',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify(data.data)
      }
    }
  },
  'wizard.step1.get_repo_version_by_id': {
    'real': '/stacks/{stackName}/versions?fields=repository_versions/operating_systems/repositories/*' +
    ',repository_versions/RepositoryVersions/*' +
    '&repository_versions/RepositoryVersions/id={repoId}&Versions/stack_version={stackVersion}',
    'mock': ''
  },

  'wizard.step1.get_supported_os_types': {
    'real': '/stacks/{stackName}/versions/{stackVersion}?fields=operating_systems/repositories/Repositories',
    'mock': ''
  },

  'wizard.advanced_repositories.valid_url': {
    'real': '/stacks/{stackName}/versions/{stackVersion}/operating_systems/{osType}/repositories/{repoId}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify(data.data)
      }
    }
  },
  'wizard.get_version_definitions': {
    'real': '/version_definitions'
  },
  'wizard.delete_repository_versions': {
    'real': '/stacks/{stackName}/versions/{stackVersion}/repository_versions/{id}',
    'type': 'DELETE'
  },
  'wizard.service_components': {
    'real': '{stackUrl}/services?fields=StackServices/*,components/*,components/dependencies/Dependencies/scope,components/dependencies/Dependencies/service_name,components/dependencies/Dependencies/type,artifacts/Artifacts/artifact_name',
    'mock': '/data/stacks/HDP-2.1/service_components.json'
  },
  'wizard.step9.installer.get_host_status': {
    'real': '/clusters/{cluster}/hosts?fields=Hosts/host_state,host_components/HostRoles/state',
    'mock': '/data/wizard/deploy/5_hosts/get_host_state.json'
  },
  'wizard.step9.load_log': {
    'real': '/clusters/{cluster}/requests/{requestId}?fields=tasks/Tasks/command,tasks/Tasks/command_detail,tasks/Tasks/ops_display_name,tasks/Tasks/exit_code,tasks/Tasks/start_time,tasks/Tasks/end_time,tasks/Tasks/host_name,tasks/Tasks/id,tasks/Tasks/role,tasks/Tasks/status&minimal_response=true',
    'mock': '/data/wizard/deploy/5_hosts/poll_{numPolls}.json',
    'format': function () {
      return {
        dataType: 'text'
      };
    }
  },

  'wizard.step8.existing_cluster_names': {
    'real': '/clusters',
    'mock': ''
  },

  'wizard.step8.create_cluster': {
    'real': '/clusters/{cluster}',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        dataType: 'text',
        data: data.data
      }
    }
  },

  'wizard.step8.create_selected_services': {
    'type': 'POST',
    'real': '/clusters/{cluster}/services',
    'mock': '/data/stacks/HDP-2.1/recommendations.json',
    'format': function (data) {
      return {
        dataType: 'text',
        data: data.data
      }
    }
  },

  'wizard.step8.create_components': {
    'real': '/clusters/{cluster}/services?ServiceInfo/service_name={serviceName}',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        dataType: 'text',
        data: data.data
      }
    }
  },

  'wizard.step8.register_host_to_cluster': {
    'real': '/clusters/{cluster}/hosts',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        dataType: 'text',
        data: data.data
      }
    }
  },

  'wizard.step8.register_host_to_component': {
    'real': '/clusters/{cluster}/hosts',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        dataType: 'text',
        data: data.data
      }
    }
  },

  'wizard.step8.apply_configuration_groups': {
    'real': '/clusters/{cluster}/config_groups',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        dataType: 'text',
        data: data.data
      }
    }
  },

  'wizard.step8.set_local_repos': {
    'real': '{stackVersionURL}/operating_systems/{osType}/repositories/{repoId}',
    'mock': '',
    'format': function (data) {
      return {
        type: 'PUT',
        dataType: 'text',
        data: data.data
      }
    }
  },
  'wizard.step3.jdk_check': {
    'real': '/requests',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify({
          "RequestInfo": {
            "context": "Check hosts",
            "action": "check_host",
            "parameters": {
              "threshold": "60",
              "java_home": data.java_home,
              "jdk_location": data.jdk_location,
              "check_execute_list": "java_home_check"
            }
          },
          "Requests/resource_filters": [{
            "hosts": data.host_names
          }]
        })
      }
    }
  },
  'wizard.step3.jdk_check.get_results': {
    'real': '/requests/{requestIndex}?fields=*,tasks/Tasks/host_name,tasks/Tasks/status,tasks/Tasks/structured_out',
    'mock': '/data/requests/host_check/jdk_check_results.json'
  },
  'wizard.step3.host_info': {
    'real': '/hosts?fields=Hosts/total_mem,Hosts/cpu_count,Hosts/disk_info,Hosts/last_agent_env,Hosts/host_name,Hosts/os_type,Hosts/os_arch,Hosts/os_family,Hosts/ip',
    'mock': '/data/wizard/bootstrap/two_hosts_information.json',
    'format': function () {
      return {
        contentType: 'application/json'
      };
    }
  },


  'wizard.loadrecommendations': {
    'real': '{stackVersionUrl}/recommendations',
    'mock': '/data/stacks/HDP-2.1/recommendations.json',
    'type': 'POST',
    'format': function (data) {
      var q = {
        hosts: data.hosts,
        services: data.services,
        recommend: data.recommend
      };

      if (data.recommendations) {
        q.recommendations = data.recommendations;
      }

      return {
        data: JSON.stringify(q)
      }
    }
  },


  // TODO: merge with wizard.loadrecommendations query
  'config.recommendations': {
    'real': '{stackVersionUrl}/recommendations',
    'mock': '/data/configurations/recommendations/configuration_dependencies.json',
    //'mock': '/data/stacks/HDP-2.1/recommendations_configs.json',
    'type': 'POST',
    'format': function (data) {
      return {
        data: JSON.stringify(data.dataToSend)
      }
    }
  },

  'config.validations': {
    'real': '{stackVersionUrl}/validations',
    'mock': '/data/stacks/HDP-2.1/validations.json',
    'type': 'POST',
    'format': function (data) {
      return {
        data: JSON.stringify({
          hosts: data.hosts,
          services: data.services,
          validate: data.validate,
          recommendations: data.recommendations
        })
      }
    }
  },


  'preinstalled.checks': {
    'real': '/requests',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify({
          "RequestInfo": data.RequestInfo,
          "Requests/resource_filters": [data.resource_filters]
        })
      }
    }
  },

  'preinstalled.checks.tasks': {
    'real': '/requests/{requestId}?fields=Requests/inputs,Requests/request_status,tasks/Tasks/host_name,' +
    'tasks/Tasks/structured_out/host_resolution_check/hosts_with_failures,' +
    'tasks/Tasks/structured_out/host_resolution_check/failed_count,' +
    'tasks/Tasks/structured_out/installed_packages,' +
    'tasks/Tasks/structured_out/last_agent_env_check,' +
    'tasks/Tasks/structured_out/transparentHugePage,' +
    'tasks/Tasks/stdout,' +
    'tasks/Tasks/stderr,' +
    'tasks/Tasks/error_log,' +
    'tasks/Tasks/command_detail,' +
    'tasks/Tasks/status' +
    '&minimal_response=true',
    'mock': '/data/requests/host_check/1.json'
  },

  'persist.get.text': {
    'real': '/persist/{key}',
    'mock': '',
    'type': 'GET',
    'format': function() {
      return {
        dataType: 'text'
      }
    }
  },

  'persist.get': {
    'real': '/persist/{key}',
    'mock': '',
    'type': 'GET'
  },
  'persist.post': {
    'real': '/persist',
    'mock': '',
    'type': 'POST',
    'format': function (data) {
      return {
        data: JSON.stringify(data.keyValuePair)
      }
    }
  },

  'wizard.step3.rerun_checks': {
    'real': '/hosts?fields=Hosts/last_agent_env',
    'mock': '/data/wizard/bootstrap/two_hosts_information.json',
    'format': function () {
      return {
        contentType: 'application/json'
      };
    }
  },
  'wizard.step3.bootstrap': {
    'real': '/bootstrap/{bootRequestId}',
    'mock': '/data/wizard/bootstrap/poll_{numPolls}.json'
  },
  'wizard.step3.is_hosts_registered': {
    'real': '/hosts?fields=Hosts/host_status',
    'mock': '/data/wizard/bootstrap/single_host_registration.json'
  },
  'wizard.stacks': {
    'real': '/stacks',
    'mock': '/data/wizard/stack/stacks2.json'
  },
  'wizard.stacks_versions': {
    'real': '/stacks/{stackName}/versions?fields=Versions,operating_systems/repositories/Repositories',
    'mock': '/data/wizard/stack/{stackName}_versions.json'
  },

  'wizard.stacks_versions_definitions': {
    'real': '/version_definitions?fields=VersionDefinition/stack_default,VersionDefinition/stack_repo_update_link_exists,VersionDefinition/max_jdk,VersionDefinition/min_jdk,operating_systems/repositories/Repositories/*,operating_systems/OperatingSystems/*,VersionDefinition/stack_services,VersionDefinition/repository_version' +
      '&VersionDefinition/show_available=true&VersionDefinition/stack_name={stackName}',
    'mock': '/data/wizard/stack/{stackName}_version_definitions.json'
  },
  'wizard.launch_bootstrap': {
    'real': '/bootstrap',
    'mock': '/data/wizard/bootstrap/bootstrap.json',
    'type': 'POST',
    'format': function (data) {
      return {
        contentType: 'application/json',
        data: data.bootStrapData,
        popup: data.popup
      }
    }
  },
  'router.login': {
    'real': '/users/{loginName}?fields=*,privileges/PrivilegeInfo/cluster_name,privileges/PrivilegeInfo/permission_name',
    'mock': '/data/users/user_{usr}.json',
    'format': function (data) {
      var statusCode = jQuery.extend({}, require('data/statusCodes'));
      statusCode['403'] = function () {
        console.log("Error code 403: Forbidden.");
      };
      return {
        statusCode: statusCode
      };
    }
  },
  'users.all': {
    real: '/users/?fields=*',
    mock: '/data/users/users.json'
  },
  'users.privileges': {
    real: '/privileges?fields=*',
    mock: '/data/users/privileges.json'
  },
  'router.user.privileges': {
    real: '/users/{userName}/privileges?fields=*',
    mock: '/data/users/privileges_{userName}.json'
  },
  'router.user.authorizations': {
    real: '/users/{userName}/authorizations?fields=*',
    mock: '/data/users/privileges_{userName}.json'
  },
  'router.login.clusters': {
    'real': '/clusters?fields=Clusters/provisioning_state,Clusters/security_type,Clusters/version,Clusters/cluster_id',
    'mock': '/data/clusters/info.json'
  },
  'router.login.message': {
    'real': '/settings/motd',
    'mock': '/data/settings/motd.json'
  },
  'router.logoff': {
    'real': '/logout',
    'mock': '',
    format: function() {
      // Workaround for sign off within Basic Authorization
      return {
        username: Date.now(),
        password: Date.now()
      };
    }
  },
  'ambari.service': {
    'real': '/services/AMBARI/components/AMBARI_SERVER{fields}',
    'mock': '/data/services/ambari_server.json'
  },

  'config_groups.create': {
    'real': '/clusters/{clusterName}/config_groups',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify(data.data)
      }
    }
  },
  'config_groups.update': {
    'real': '/clusters/{clusterName}/config_groups/{id}',
    'mock': '',
    'format': function (data) {
      return {
        type: 'PUT',
        data: JSON.stringify(data.data)
      }
    }
  },
  'request_schedule.get': {
    'real': '/clusters/{clusterName}/request_schedules/{request_schedule_id}',
    'mock': ''
  },
  'request_schedule.get.pending': {
    'real': '/clusters/{clusterName}/request_schedules?fields=*&(RequestSchedule/status.in(SCHEDULED,IN_PROGRESS))',
    'mock': ''
  },
  'restart.hostComponents': {
    'real': '/clusters/{clusterName}/requests',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify({
          "RequestInfo": {
            "command": "RESTART",
            "context": data.context,
            "operation_level": data.operation_level
          },
          "Requests/resource_filters": data.resource_filters
        })
      }
    }
  },

  'restart.allServices': {
    'real': '/clusters/{clusterName}/requests',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify({
          "RequestInfo": {
            "command": "RESTART",
            "context": 'Restart all services',
            "operation_level": 'host_component'
          },
          "Requests/resource_filters": [
            {
              "hosts_predicate": "HostRoles/cluster_name=" + data.clusterName
            }
          ]
        })
      }
    }
  },

  'restart.staleConfigs': {
    'real': "/clusters/{clusterName}/requests",
    'mock': "",
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify({
          "RequestInfo": {
            "command": "RESTART",
            "context": "Restart all required services",
            "operation_level": "host_component"
          },
          "Requests/resource_filters": [
            {
              "hosts_predicate": "HostRoles/stale_configs=true&HostRoles/cluster_name=" + data.clusterName
            }
          ]
        })
      }
    }
  },

  'restart.custom.filter': {
    'real': "/clusters/{clusterName}/requests",
    'mock': "",
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify({
          "RequestInfo": {
            "command": "RESTART",
            "context": data.context,
            "operation_level": "host_component"
          },
          "Requests/resource_filters": [
            {
              "hosts_predicate": data.filter
            }
          ]
        })
      }
    }
  },

  'bulk_request.decommission': {
    'real': '/clusters/{clusterName}/requests',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify({
          'RequestInfo': {
            'context': data.context,
            'command': 'DECOMMISSION',
            'parameters': data.parameters,
            'operation_level': {
              'level': "CLUSTER",
              'cluster_name': data.clusterName
            }
          },
          "Requests/resource_filters": [{"service_name": data.serviceName, "component_name": data.componentName}]
        })
      }
    }
  },

  'bulk_request.hosts.passive_state': {
    'real': '/clusters/{clusterName}/hosts',
    'mock': '',
    'format': function (data) {
      return {
        type: 'PUT',
        data: JSON.stringify({
          RequestInfo: {
            context: data.requestInfo,
            query: 'Hosts/host_name.in(' + data.hostNames + ')'
          },
          Body: {
            Hosts: {
              maintenance_state: data.passive_state
            }
          }
        })
      }
    }
  },


  'bulk_request.hosts.update_rack_id': {
    'real': '/clusters/{clusterName}/hosts',
    'mock': '',
    'format': function(data) {
      return {
        type: 'PUT',
        data: JSON.stringify({
          RequestInfo: {
            context: data.requestInfo,
            query: 'Hosts/host_name.in(' + data.hostNames + ')'
          },
          Body: {
            Hosts: {
              rack_info: data.rackId
            }
          }
        })
      }
    }
  },

  'bulk_request.hosts.all_components.passive_state': {
    'real': '/clusters/{clusterName}/host_components',
    'mock': '',
    'format': function (data) {
      return {
        type: 'PUT',
        data: JSON.stringify({
          RequestInfo: {
            context: data.requestInfo,
            query: data.query
          },
          Body: {
            HostRoles: {
              maintenance_state: data.passive_state
            }
          }
        })
      }
    }
  },
  'views.info': {
    'real': '/views',
    'mock': '/data/views/views.json'
  },
  /**
   * Get all instances of all views across versions
   */
  'views.instances': {
    'real': '/views?fields=versions/instances/ViewInstanceInfo,versions/ViewVersionInfo/label&versions/ViewVersionInfo/system=false',
    'mock': '/data/views/instances.json'
  },
  'host.host_component.flume.metrics': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/FLUME_HANDLER?fields=metrics/flume/flume/{flumeComponent}/*',
    'mock': ''
  },
  'host.host_component.flume.metrics.timeseries': {
    'real': '',
    'mock': '',
    format: function (data) {
      return {
        url: data.url
      }
    }
  },
  'host.host_components.filtered': {
    'real': '/clusters/{clusterName}/hosts?{fields}',
    'mock': '',
    format: function (data) {
      return {
        headers: {
          'X-Http-Method-Override': 'GET'
        },
        type: 'POST',
        data: JSON.stringify({
          "RequestInfo": {"query": data.parameters}
        })
      };
    }
  },
  'host.stack_versions.install': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/stack_versions',
    'mock': '',
    'type': 'POST',
    'format': function (data) {
      return {
        data: JSON.stringify({
          "HostStackVersions": {
            "stack": data.version.get('stack'),
            "version": data.version.get('version'),
            "repository_version": data.version.get('repoVersion')
          }
        })
      }
    }
  },

  'host.logging': {
    'real': '/clusters/{clusterName}/hosts/{hostName}?fields=host_components/logging,host_components/HostRoles/service_name{fields}{query}&minimal_response=true',
    'mock': ''
  },
  'components.filter_by_status': {
    'real': '/clusters/{clusterName}/components?fields=host_components/HostRoles/host_name,ServiceComponentInfo/component_name,ServiceComponentInfo/started_count{urlParams}&minimal_response=true',
    'mock': ''
  },
  'components.get_category': {
    'real': '/clusters/{clusterName}/components?fields=ServiceComponentInfo/component_name,ServiceComponentInfo/service_name,ServiceComponentInfo/category,ServiceComponentInfo/recovery_enabled,ServiceComponentInfo/total_count&minimal_response=true',
    'mock': ''
  },
  'components.get.staleConfigs': {
    'real': '/clusters/{clusterName}/components?host_components/HostRoles/stale_configs=true' +
    '&fields=host_components/HostRoles/host_name&minimal_response=true',
    'mock': ''
  },
  'components.update': {
    'real': '/clusters/{clusterName}/components?{urlParams}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          RequestInfo: {
            query: data.query
          },
          ServiceComponentInfo: data.ServiceComponentInfo
        })
      }
    }
  },
  'hosts.all.install': {
    'real': '/hosts?minimal_response=true',
    'mock': ''
  },
  'hosts.all': {
    'real': '/clusters/{clusterName}/hosts?minimal_response=true',
    'mock': '/data/hosts/HDP2/hosts.json'
  },
  'hosts.with_public_host_names': {
    'real': '/clusters/{clusterName}/hosts?fields=Hosts/public_host_name&minimal_response=true',
    'mock': ''
  },
  'hosts.for_quick_links': {
    'real': '/clusters/{clusterName}/hosts?Hosts/host_name.in({hosts})&fields=Hosts/public_host_name{urlParams}&minimal_response=true',
    'mock': '/data/hosts/quick_links.json'
  },
  'hosts.confirmed.install': {
    'real': '/hosts?fields=Hosts/cpu_count,Hosts/disk_info,Hosts/total_mem,Hosts/maintenance_state&minimal_response=true',
    'mock': ''
  },
  'hosts.confirmed': {
    'real': '/clusters/{clusterName}/hosts?fields=host_components/HostRoles/state&minimal_response=true',
    'mock': '/data/hosts/HDP2/hosts.json'
  },
  'hosts.with_searchTerm': {
    'real': '/clusters/{clusterName}/hosts?fields=Hosts/{facet}&minimal_response=true&page_size={page_size}',
    'mock': '',
    format: function (data) {
      return {
        headers: {
          'X-Http-Method-Override': 'GET'
        },
        type: 'POST',
        data: JSON.stringify({
          "RequestInfo": {"query": (data.searchTerm ? "Hosts/"+ data.facet +".matches(.*" + data.searchTerm + ".*)" : "")}
        })
      };
    }
  },
  'hosts.confirmed.minimal': {
    'real': '/clusters/{clusterName}/hosts?fields=host_components/HostRoles/state&minimal_response=true',
    'mock': '/data/hosts/HDP2/hosts.json'
  },
  'hosts.heartbeat_lost': {
    'real': '/clusters/{clusterName}/hosts?Hosts/host_state=HEARTBEAT_LOST',
    'mock': ''
  },
  'host_components.all': {
    'real': '/clusters/{clusterName}/host_components?fields=HostRoles/host_name&minimal_response=true',
    'mock': ''
  },
  'host_components.with_services_names': {
    'real': '/clusters/{clusterName}/host_components?fields=component/ServiceComponentInfo/service_name,HostRoles/host_name&minimal_response=true',
    'mock': ''
  },
  'components.get_installed': {
    'real': '/clusters/{clusterName}/components',
    'mock': ''
  },
  'hosts.heatmaps': {
    'real': '/clusters/{clusterName}/hosts?fields=Hosts/rack_info,Hosts/host_name,Hosts/public_host_name,Hosts/os_type,Hosts/ip,host_components,metrics/disk,metrics/cpu/cpu_system,metrics/cpu/cpu_user,metrics/memory/mem_total,metrics/memory/mem_free&minimal_response=true',
    'mock': '/data/hosts/HDP2/hosts.json'
  },
  'namenode.cpu_wio': {
    'real': '/clusters/{clusterName}/hosts/{nnHost}?fields=metrics/cpu',
    'mock': '/data/cluster_metrics/cpu.json'
  },

  'custom_action.create': {
    'real': '/requests',
    'mock': '',
    'format': function (data) {
      var requestInfo = {
        context: 'Check host',
        action: 'check_host',
        parameters: {}
      };
      $.extend(true, requestInfo, data.requestInfo);
      return {
        type: 'POST',
        data: JSON.stringify({
          'RequestInfo': requestInfo,
          'Requests/resource_filters': [{
            hosts: data.filteredHosts.join(',')
          }]
        })
      }
    }
  },

  'cluster.custom_action.create': {
    'real': '/clusters/{clusterName}/requests',
    'mock': '',
    'format': function (data) {
      var requestInfo = {
        context: 'Check host',
        action: 'check_host',
        parameters: {}
      };
      $.extend(true, requestInfo, data.requestInfo);
      return {
        type: 'POST',
        data: JSON.stringify({
          'RequestInfo': requestInfo,
          'Requests/resource_filters': [{
            hosts: data.filteredHosts.join(',')
          }]
        })
      }
    }
  },

  'custom_action.request': {
    'real': '/requests/{requestId}/tasks/{taskId}',
    'mock': '/data/requests/1.json',
    'format': function (data) {
      return {
        requestId: data.requestId,
        taskId: data.taskId || ''
      }
    }
  },
  'hosts.high_availability.wizard': {
    'real': '/clusters/{clusterName}/hosts?fields=Hosts/cpu_count,Hosts/disk_info,Hosts/total_mem,Hosts/maintenance_state&minimal_response=true',
    'mock': ''
  },
  'hosts.security.wizard': {
    'real': '/clusters/{clusterName}/hosts?fields=host_components/HostRoles/service_name&minimal_response=true',
    'mock': ''
  },
  'host_component.installed.on_hosts': {
    'real': '/clusters/{clusterName}/host_components?HostRoles/component_name={componentName}&HostRoles/host_name.in({hostNames})&fields=HostRoles/host_name&minimal_response=true',
    'mock': ''
  },
  'hosts.by_component.one': {
    'real': '/clusters/{clusterName}/hosts?host_components/HostRoles/component_name.in({componentNames})&fields=host_components,Hosts/cpu_count,Hosts/disk_info,Hosts/total_mem,Hosts/ip,Hosts/os_type,Hosts/os_arch,Hosts/public_host_name&page_size=1&minimal_response=true',
    'mock': ''
  },
  'hosts.by_component.all': {
    'real': '/clusters/{clusterName}/hosts?host_components/HostRoles/component_name.in({componentNames})&fields=host_components,Hosts/cpu_count,Hosts/disk_info,Hosts/total_mem,Hosts/ip,Hosts/os_type,Hosts/os_arch,Hosts/public_host_name&minimal_response=true',
    'mock': ''
  },
  'hosts.config_groups': {
    'real': '/clusters/{clusterName}/hosts?fields=Hosts/cpu_count,Hosts/disk_info,Hosts/total_mem,Hosts/ip,Hosts/os_type,Hosts/os_arch,Hosts/public_host_name,host_components&minimal_response=true',
    'mock': ''
  },
  'hosts.info.install': {
    'real': '/hosts?Hosts/host_name.in({hostNames})&fields=Hosts/cpu_count,Hosts/disk_info,Hosts/total_mem,Hosts/ip,Hosts/os_type,Hosts/os_arch,Hosts/public_host_name&minimal_response=true',
    'mock': ''
  },
  'hosts.ips': {
    'real': '/hosts?Hosts/host_name.in({hostNames})&fields=Hosts/ip',
    'mock': ''
  },
  'hosts.host_components.pre_load': {
    real: '',
    mock: '/data/hosts/HDP2/hosts.json',
    format: function (data) {
      return {
        url: data.url
      }
    }
  },
  'hosts.metrics.lazy_load': {
    real: '',
    mock: '/data/hosts/HDP2/hosts.json',
    format: function (data) {
      return {
        url: data.url,
        headers: {
          'X-Http-Method-Override': 'GET'
        },
        type: 'POST',
        data: JSON.stringify({
          "RequestInfo": {"query": data.parameters}
        })
      }
    }
  },
  'hosts.bulk.operations': {
    real: '/clusters/{clusterName}/hosts?fields=Hosts/host_name,Hosts/host_state,Hosts/maintenance_state,' +
    'host_components/HostRoles/state,host_components/HostRoles/maintenance_state,' +
    'Hosts/total_mem,stack_versions/HostStackVersions,stack_versions/repository_versions/RepositoryVersions/repository_version,' +
    'stack_versions/repository_versions/RepositoryVersions/id,' +
    'host_components/HostRoles/stale_configs' +
    'host_components/HostRoles/service_name&minimal_response=true',
    mock: '',
    format: function (data) {
      return {
        headers: {
          'X-Http-Method-Override': 'GET'
        },
        type: 'POST',
        data: JSON.stringify({
          "RequestInfo": {"query": data.parameters}
        })
      }
    }
  },

  'logtail.get': {
    'real': '/clusters/{clusterName}/logging/searchEngine?component_name={logComponentName}&host_name={hostName}&pageSize={pageSize}&startIndex={startIndex}',
    'mock': ''
  },

  'service.serviceConfigVersions.get': {
    real: '/clusters/{clusterName}/configurations/service_config_versions?service_name={serviceName}&fields=service_config_version,user,hosts,group_id,group_name,is_current,createtime,service_name,service_config_version_note,stack_id,is_cluster_compatible&sortBy=service_config_version.desc&minimal_response=true',
    mock: '/data/configurations/service_versions.json'
  },
  'service.serviceConfigVersions.get.current': {
    real: '/clusters/{clusterName}/configurations/service_config_versions?service_name.in({serviceNames})&is_current=true&fields=*',
    mock: '/data/configurations/service_versions.json'
  },
  'service.serviceConfigVersions.get.current.not.default': {
    real: '/clusters/{clusterName}/configurations/service_config_versions?is_current=true&group_id>0&fields=*',
    mock: '/data/configurations/service_versions.json'
  },
  'service.serviceConfigVersions.get.total': {
    real: '/clusters/{clusterName}/configurations/service_config_versions?page_size=1&minimal_response=true',
    mock: '/data/configurations/service_versions_total.json'
  },
  'service.serviceConfigVersion.get': {
    real: '/clusters/{clusterName}/configurations/service_config_versions?service_name={serviceName}&service_config_version={serviceConfigVersion}',
    mock: '/data/configurations/service_version.json'
  },
  'service.serviceConfigVersions.get.multiple': {
    real: '/clusters/{clusterName}/configurations/service_config_versions?(service_name={serviceName}%26service_config_version.in({serviceConfigVersions})){additionalParams}',
    mock: '/data/configurations/service_version.json',
    format: function (data) {
      return {
        serviceConfigVersions: data.serviceConfigVersions.join(','),
        additionalParams: data.additionalParams || ''
      }
    }
  },
  'service.serviceConfigVersions.get.suggestions': {
    real: '/clusters/{clusterName}/configurations/service_config_versions?fields={key}&minimal_response=true',
    mock: ''
  },
  'service.serviceConfigVersion.revert': {
    'real': '/clusters/{clusterName}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify(data.data)
      }
    }
  },

  'service.mysql.clean': {
    'real': '/clusters/{clusterName}/requests',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify({
          "RequestInfo": {
            "command" : "CLEAN", "context" : "Clean MYSQL Server"
          },
          "Requests/resource_filters": [{
              "service_name" : "HIVE",
              "component_name" : "MYSQL_SERVER",
              "hosts": data.host
          }]
        })
      }
    }
  },

  'service.mysql.configure': {
    'real': '/clusters/{clusterName}/requests',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify({
          "RequestInfo": {
            "command" : "CONFIGURE", "context" : "Configure MYSQL Server"
          },
          "Requests/resource_filters": [{
              "service_name" : "HIVE",
              "component_name" : "MYSQL_SERVER",
              "hosts": data.host
          }]
        })
      }
    }
  },

  'service.mysql.testHiveConnection': {
    'real': '/requests',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify({
          "RequestInfo": {
            "context": "Check host",
            "action": "check_host",
            "parameters":{
              "db_name": data.db_name,
              "user_name": data.db_user,
              "user_passwd": data.db_pass,
              "db_connection_url": data.db_connection_url,
              "jdk_location": data.jdk_location,
              "threshold":"60",
              "ambari_server_host": "c6403.ambari.apache.org",
              "check_execute_list":"db_connection_check",
              "java_home": data.java_home,
              "jdk_name": data.jdk_name
            }
          },
          "Requests/resource_filters": [{"hosts": data.hosts}]})
      }
    }
  },

  'widgets.get': {
    real: '/clusters/{clusterName}/widgets?{urlParams}',
    mock: '/data/widget_layouts/{sectionName}_WIDGETS.json'
  },

  'widgets.all.shared.get': {
    real: '/clusters/{clusterName}/widgets?WidgetInfo/scope=CLUSTER&fields=*',
    mock: '/data/widget_layouts/all_shared_widgets.json'
  },

  'widgets.all.mine.get': {
    real: '/clusters/{clusterName}/widgets?WidgetInfo/scope=USER&WidgetInfo/author={loginName}&fields=*',
    mock: '/data/widget_layouts/all_mine_widgets.json'
  },

  'widgets.layout.stackDefined.get': {
    real: '{stackVersionURL}/services/{serviceName}/artifacts/widget_descriptor',
    mock: '/data/widget_layouts/HBASE/stack_layout.json'
  },

  'widget.layout.id.get': {
    real: '/clusters/{clusterName}/widget_layouts/{layoutId}',
    mock: '/data/widget_layouts/{serviceName}/default_dashboard.json'
  },

  'widget.layout.name.get': {
    real: '/clusters/{clusterName}/widget_layouts?WidgetLayoutInfo/layout_name={name}',
    mock: '/data/widget_layouts/{serviceName}/default_dashboard.json'
  },

  'widget.layout.delete': {
    real: '/clusters/{clusterName}/widget_layouts/{layoutId}',
    type: 'DELETE'
  },

  'widget.layout.get': {
    real: '/clusters/{clusterName}/widget_layouts?{urlParams}',
    mock: '/data/widget_layouts/{serviceName}/default_dashboard.json'
  },

  'widget.layout.edit': {
    real: '/clusters/{clusterName}/widget_layouts/{layoutId}',
    mock: '',
    format: function (data) {
      return {
        type: 'PUT',
        data: JSON.stringify(data.data)
      }
    }
  },

  'widget.layout.create': {
    real: '/clusters/{clusterName}/widget_layouts',
    mock: '',
    format: function (data) {
      return {
        type: 'POST',
        data: JSON.stringify(data.data)
      }
    }
  },

  'widgets.layout.userDefined.get': {
    real: '/users/{loginName}/widget_layouts?section_name={sectionName}',
    mock: '/data/widget_layouts/HBASE/empty_user_layout.json'
  },

  'widgets.layouts.get': {
    real: '/users?widget_layouts/section_name={sectionName}&widget_layouts/scope=CLUSTER',
    mock: '/data/widget_layouts/HBASE/layouts.json'
  },

  'widgets.layouts.active.get': {
    real: '/users/{userName}/activeWidgetLayouts?{urlParams}',
    mock: '/data/widget_layouts/{sectionName}.json'
  },

  'widgets.layouts.all.active.get': {
    real: '/users/{userName}/activeWidgetLayouts',
    mock: ''
  },


  'widget.activelayouts.edit': {
    real: '/users/{userName}/activeWidgetLayouts/',
    mock: '',
    format: function (data) {
      return {
        type: 'PUT',
        data: JSON.stringify(data.data)
      }
    }
  },


  'widget.action.delete': {
    real: '/clusters/{clusterName}/widgets/{id}',
    mock: '',
    format: function (data) {
      return {
        type: 'DELETE'
      }
    }
  },

  'widgets.serviceComponent.metrics.get': {
    real: '/clusters/{clusterName}/services/{serviceName}/components/{componentName}?fields={metricPaths}&format=null_padding',
    mock: '/data/metrics/{serviceName}/Append_num_ops_&_Delete_num_ops.json'
  },

  'widgets.hostComponent.metrics.get': {
    real: '/clusters/{clusterName}/host_components?HostRoles/component_name={componentName}{hostComponentCriteria}&fields={metricPaths}&format=null_padding{selectedHostsParam}',
    mock: '/data/metrics/{serviceName}/Append_num_ops.json'
  },

  'widgets.hosts.metrics.get': {
    real: '/clusters/{clusterName}/hosts?fields={metricPaths}',
    mock: '/data/metrics/{serviceName}/Append_num_ops.json'
  },

  'widgets.wizard.metrics.get': {
    real: '{stackVersionURL}/services?artifacts/Artifacts/artifact_name=metrics_descriptor&StackServices/service_name.in({serviceNames})&fields=artifacts/*',
    mock: '/data/metrics/HBASE/definition.json'
  },

  'widgets.wizard.add': {
    real: '/clusters/{clusterName}/widgets/',
    mock: '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify(data.data)
      };
    }
  },

  'widgets.wizard.edit': {
    real: '/clusters/{clusterName}/widgets/{widgetId}',
    mock: '',
    'format': function (data) {
      return {
        type: 'PUT',
        data: JSON.stringify(data.data)
      };
    }
  },

  'service.components.load': {
    real: '/clusters/{clusterName}/services?fields=components&minimal_response=true',
    mock: '/data/services/components.json'
  },

  'nameNode.federation.formatNameNode': {
    'real': '/clusters/{clusterName}/requests',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify({
          "RequestInfo": {
            "command" : "FORMAT", "context" : "Format NameNode"
          },
          "Requests/resource_filters": [{
            "service_name" : "HDFS",
            "component_name" : "NAMENODE",
            "hosts": data.host
          }]
        })
      }
    }
  },

  'nameNode.federation.formatZKFC': {
    'real': '/clusters/{clusterName}/requests',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify({
          "RequestInfo": {
            "command" : "FORMAT", "context" : "Format ZKFC"
          },
          "Requests/resource_filters": [{
            "service_name" : "HDFS",
            "component_name" : "ZKFC",
            "hosts": data.host
          }]
        })
      }
    }
  },
  'nameNode.federation.bootstrapNameNode': {
    'real': '/clusters/{clusterName}/requests',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify({
          "RequestInfo": {
            "command" : "BOOTSTRAP_STANDBY", "context" : "Bootstrap NameNode"
          },
          "Requests/resource_filters": [{
            "service_name" : "HDFS",
            "component_name" : "NAMENODE",
            "hosts": data.host
          }]
        })
      }
    }
  },
  'hiveServerInteractive.getStatus': {
    real: '',
    mock: '',
    format: function (data) {
      return {
        url: 'http://' + data.hsiHost + ':' + data.port + '/leader'
      }
    }
  }
};
/**
 * Replace data-placeholders to its values
 *
 * @param {String} url
 * @param {Object} data
 * @return {String}
 */
var formatUrl = function (url, data) {
  if (!url) return null;
  var keys = url.match(/\{\w+\}/g);
  keys = (keys === null) ? [] : keys;
  if (keys) {
    keys.forEach(function (key) {
      var raw_key = key.substr(1, key.length - 2);
      var replace;
      if (!data || !data[raw_key]) {
        replace = '';
      }
      else {
        replace = data[raw_key];
      }
      url = url.replace(new RegExp(key, 'g'), replace);
    });
  }
  return url;
};

/**
 * this = object from config
 * @return {Object}
 */
var formatRequest = function (data) {
  var opt = {
    type: this.type || 'GET',
    timeout: App.timeout,
    dataType: 'json',
    headers: {}
  };
  if (App.get('testMode')) {
    opt.url = formatUrl(this.mock ? this.mock : '', data);
    opt.type = 'GET';
  }
  else {
    var prefix = this.apiPrefix != null ? this.apiPrefix : App.apiPrefix;
    opt.url = prefix + formatUrl(this.real, data);
  }

  if (this.format) {
    jQuery.extend(opt, this.format(data, opt));
  }
  var statusCode = jQuery.extend({}, require('data/statusCodes'));
  statusCode['404'] = function () {
    console.log("Error code 404: URI not found. -> " + opt.url);
  };
  opt.statusCode = statusCode;
  return opt;
};

/**
 * transform GET to POST call
 * @param {object} opt
 * @returns {object} opt
 */
var doGetAsPost = function(opt) {
  var delimiterPos = opt.url.indexOf('?');
  var fieldsIndex = opt.url.indexOf('&fields');

  opt.type = "POST";
  opt.headers["X-Http-Method-Override"] = "GET";
  if (delimiterPos !== -1) {
    var query = fieldsIndex !== -1 ? opt.url.substring(delimiterPos + 1, fieldsIndex) : opt.url.substr(delimiterPos + 1);
    opt.data = JSON.stringify({
      "RequestInfo": {"query" : query}
    });
    if (fieldsIndex !== -1) {
      opt.url = opt.url.substr(0, delimiterPos) + '?' + opt.url.substr(fieldsIndex + 1) + '&_=' + App.dateTime();
    } else {
      opt.url = opt.url.substr(0, delimiterPos)  + '?_=' + App.dateTime();
    }
  } else {
    opt.url += '?_=' + App.dateTime();
  }
  return opt;
};

/**
 * Wrapper for all ajax requests
 *
 * @type {Object}
 */
var ajax = Em.Object.extend({
  /**
   * max number of symbols in URL of GET call
   * @const
   * @type {number}
   */
  MAX_GET_URL_LENGTH: 2048,

  consoleMsg: function(name, url) {
    return Em.I18n.t('app.logger.ajax').format(name, url ? url.substr(7, 100) : '');
  },

  /**
   * Send ajax request
   *
   * @param {Object} config
   * @return {$.ajax} jquery ajax object
   *
   * config fields:
   *  name - url-key in the urls-object *required*
   *  sender - object that send request (need for proper callback initialization) *required*
   *  data - object with data for url-format
   *  beforeSend - method-name for ajax beforeSend response callback
   *  success - method-name for ajax success response callback
   *  error - method-name for ajax error response callback
   *  callback - callback from <code>App.updater.run</code> library
   */
  send: function (config) {

    if (!config.sender) {
      console.warn('Ajax sender should be defined!');
      return null;
    }

    var loadingPopup = null;
    var loadingPopupTimeout = null;
    if(config.hasOwnProperty("showLoadingPopup") && config.showLoadingPopup === true) {
      loadingPopupTimeout = setTimeout(function() {
        loadingPopup = App.ModalPopup.show({
          header: Em.I18n.t('common.loading.eclipses'),
          backdrop: false,
          primary: false,
          secondary: false,
          bodyClass: Em.View.extend({
            template: Em.Handlebars.compile('{{view App.SpinnerView}}')
          })
        });
      }, 500);
    }

    // default parameters
    var params = {
      clusterName: (App.get('clusterName') || App.clusterStatus.get('clusterName'))
    };

    // extend default parameters with provided
    if (config.hasOwnProperty("data") && config.data) {
      jQuery.extend(params, config.data);
    }

    var opt = {};
    if (!urls[config.name]) {
      console.warn('Invalid name provided `' + config.name + '`!');
      return null;
    }
    opt = formatRequest.call(urls[config.name], params);

    var consoleMsg = this.consoleMsg(config.name, opt.url);

    App.logger.setTimer(consoleMsg);

    if (opt.url && opt.url.length > this.get('MAX_GET_URL_LENGTH')) {
      opt = doGetAsPost(opt);
    }

    opt.context = this;

    // object sender should be provided for processing beforeSend, success and error responses
    opt.beforeSend = function (xhr) {
      if (config.beforeSend) {
        config.sender[config.beforeSend](opt, xhr, params);
      }
    };
    opt.success = function (data, textStatus, request) {
      if (config.success) {
        config.sender[config.success](data, opt, params, request);
      }
    };
    opt.error = function (request, ajaxOptions, error) {
      var KDCErrorMsg = this.getKDCErrorMgs(request);
      if (!Em.isNone(KDCErrorMsg)) {
        this.defaultErrorKDCHandler(opt, KDCErrorMsg);
      } else if (config.error) {
        config.sender[config.error](request, ajaxOptions, error, opt, params);
      } else {
        this.defaultErrorHandler(request, opt.url, opt.type);
      }
    };
    opt.complete = function () {
      if (loadingPopupTimeout) {
        clearTimeout(loadingPopupTimeout);
      }
      if(loadingPopup) {
        Em.tryInvoke(loadingPopup, 'hide');
      }
      App.logger.logTimerIfMoreThan(consoleMsg, 1000);
      if (config.callback) {
        config.callback();
      }
    };

    /**
     * run this handler when click cancle on KDC error popup
     */
    if (config.kdcCancelHandler) {
      opt.kdcCancelHandler = config.kdcCancelHandler;
    }

    if ($.mocho) {
      opt.url = 'http://' + $.hostName + opt.url;
    }
    return $.ajax(opt);
  },

  // A single instance of App.ModalPopup view
  modalPopup: null,

  /**
   * Upon error with one of these statuses modal should be displayed
   * @type {Array}
   */
  statuses: [500, 401, 407, 413],

  /**
   * defaultErrorHandler function is referred from App.ajax.send function and App.HttpClient.defaultErrorHandler function
   * @jqXHR {jqXHR Object}
   * @url {string}
   * @method {String} Http method
   * @showStatus {number} HTTP response code which should be shown. Default is 500.
   */
  defaultErrorHandler: function (jqXHR, url, method, showStatus) {
    method = method || 'GET';
    var self = this;
    showStatus = (Em.isNone(showStatus)) ? this.get('statuses') : [showStatus];
    try {
      var json = $.parseJSON(jqXHR.responseText);
      var message = json.message;
    } catch (err) {
    }

    if (showStatus.contains(jqXHR.status) && !this.get('modalPopup')) {
      this.set('modalPopup', App.ModalPopup.show({
        elementId: 'default-error-modal',
        header: Em.I18n.t('common.error'),
        secondary: false,
        onPrimary: function () {
          this.hide();
          self.set('modalPopup', null);
        },
        bodyClass: App.AjaxDefaultErrorPopupBodyView.extend({
          type: method,
          url: url,
          status: jqXHR.status,
          message: message
        })
      }));
    }
  },

  /**
   * defines if it's admin session expiration error
   * and if so returns short error message
   * @param jqXHR
   * @returns {string|null}
   */
  getKDCErrorMgs: function(jqXHR) {
    try {
      var message = $.parseJSON(jqXHR.responseText).message;
    } catch (err) {}
    if (jqXHR.status === 400 && message) {
      return App.format.kdcErrorMsg(message, true);
    }
  },

  /**
   * default handler for admin session expiration error
   * @param {object} opt
   * @param {string} msg
   * @returns {*}
   */
  defaultErrorKDCHandler: function(opt, msg) {
    return App.showInvalidKDCPopup(opt, msg);
  },

  /**
   * Abort all requests stored in the certain array
   * @param requestsArray
   */
  abortRequests: function (requestsArray) {
    requestsArray.forEach(function (xhr) {
      xhr.isForcedAbort = true;
      Em.tryInvoke(xhr, 'abort');
    });
    requestsArray.clear();
  }

});

/**
 * Add few access-methods for test purposes
 */
if ($.mocho) {
  ajax.reopen({
    /**
     * Don't use it anywhere except tests!
     * @returns {Array}
     */
    fakeGetUrlNames: function () {
      return Em.keys(urls);
    },

    /**
     * Don't use it anywhere except tests!
     * @param name
     * @returns {*}
     */
    fakeGetUrl: function (name) {
      return urls[name];
    },

    /**
     * Don't use it anywhere except tests!
     * @param url
     * @param data
     * @returns {String}
     */
    fakeFormatUrl: function (url, data) {
      return formatUrl(url, data);
    },

    /**
     * Don't use it anywhere except tests!
     * @param urlObj
     * @param data
     * @returns {Object}
     */
    fakeFormatRequest: function (urlObj, data) {
      return formatRequest.call(urlObj, data);
    },

    /**
     * Don't use it anywhere except tests!
     * @param urlObj
     * @param data
     * @returns {Object}
     */
    fakeDoGetAsPost: function (urlObj, data) {
      return doGetAsPost.call(urlObj, data);
    }
  });
}

App.ajax = ajax.create({});
