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


const App = require('app');

App.HostsHeartbeatView = Em.View.extend({
  templateName: require('templates/main/admin/stack_upgrade/custom_cluster_checks/custom_cluster_checks_hosts_heartbeat'),

  hostNames: function () {
    return App.get('allHostNames').filter( (hostId) => {
      return this.get('check.failed_on').contains(hostId);
    });
  }.property(),

  removeHost: function (host) {
    const controller =  App.router.get('mainHostDetailsController');
    controller.set('content', host);
    controller.validateAndDeleteHost();
  },

  startRemoveHost: function (event) {
    const hostName = event.context;
    const fields = 'fields=Hosts/rack_info,Hosts/host_name,Hosts/maintenance_state,Hosts/public_host_name,Hosts/' +
      'cpu_count,Hosts/ph_cpu_count,alerts_summary,Hosts/host_status,Hosts/host_state,Hosts/last_heartbeat_time,Hosts/' +
      'ip,host_components/HostRoles/state,host_components/HostRoles/maintenance_state,host_components/HostRoles/' +
      'stale_configs,host_components/HostRoles/service_name,host_components/HostRoles/display_name,host_components/' +
      'HostRoles/desired_admin_state,host_components/metrics/dfs/namenode/ClusterId,host_components/metrics/dfs/' +
      'FSNamesystem/HAState,metrics/disk,metrics/load/load_one,Hosts/total_mem,Hosts/os_arch,Hosts/os_type,metrics/cpu/' +
      'cpu_system,metrics/cpu/cpu_user,metrics/memory/mem_total,metrics/memory/mem_free,stack_versions/' +
      'HostStackVersions,stack_versions/repository_versions/RepositoryVersions/repository_version,stack_versions/' +
      'repository_versions/RepositoryVersions/id,stack_versions/repository_versions/RepositoryVersions/' +
      'display_name&minimal_response=true,host_components/logging&page_size=10&from=0&sortBy=Hosts/host_name.asc';
    const url = App.apiPrefix + '/clusters/' + App.clusterName + '/hosts/?Hosts/host_name.in(' + hostName + ')&' + fields;
    const host = App.Host.find(hostName);
    if (!host.get('isLoaded')) {
      App.HttpClient.get(url, App.hostsMapper, {
        complete: () => {
          const host = App.Host.find(hostName);
          this.removeHost(host);
        }
      });
    } else {
      this.removeHost(host);
    }
  }
});