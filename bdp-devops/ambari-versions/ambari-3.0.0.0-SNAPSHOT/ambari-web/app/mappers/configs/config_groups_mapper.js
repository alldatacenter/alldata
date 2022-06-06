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

/**
 * THIS IS NOT USED FOR NOW
 * FOR CONFIG GROUPS WE ARE USING OLD MODELS AND LOGIC
 */

var App = require('app');

App.configGroupsMapper = App.QuickDataMapper.create({
  model: App.ServiceConfigGroup,
  config: {
    id: 'ConfigGroup.id',
    name: 'ConfigGroup.group_name',
    service_name: 'ConfigGroup.tag',
    description: 'ConfigGroup.description',
    hosts: 'hosts',
    service_id: 'ConfigGroup.tag',
    desired_configs: 'ConfigGroup.desired_configs'
  },

  /**
   * using this config when saving group from config_version api
   */
  config2: {
    id: 'group_id',
    name: 'group_name',
    service_name: 'service_name',
    hosts: 'hosts',
    service_id: 'service_name'
  },


  map: function (json, mapFromVersions, serviceNames) {
    console.time('App.configGroupsMapper');
    if (serviceNames && serviceNames.length > 0) {
      var configGroups = [];

      /**
       * ex: { "HDFS": ["host1", "host2"], "YARN": ["host1"] }
       * this property is used to store host names for default config group.
       * While parsing data for not default groups host names will be excluded from this list.
       * In case there is no not default config groups for some service <code>hostNamesForService<code>
       * will not contain property for this service which mean all host belongs to default group
       */
      var hostNamesForService = {};
      var configGroupsForService = {};

      if (json && json.items) {
        json.items.forEach(function (configGroup) {
          if (![App.ServiceConfigGroup.defaultGroupName, App.ServiceConfigGroup.deletedGroupName].contains(configGroup.group_name)) {
            if (mapFromVersions) {
              configGroup.id = App.ServiceConfigGroup.groupId(configGroup.service_name, configGroup.group_name);
            } else {
              configGroup.id = App.ServiceConfigGroup.groupId(configGroup.ConfigGroup.tag, configGroup.ConfigGroup.group_name);
              configGroup.hosts = configGroup.ConfigGroup.hosts.mapProperty('host_name').sort();
              configGroup.service_name = configGroup.ConfigGroup.tag;
            }

            /**
             * creating (if not exists) field in <code>hostNamesForService<code> with host names for default group
             */
            if (!hostNamesForService[configGroup.service_name]) {
              hostNamesForService[configGroup.service_name] = App.get('allHostNames').slice(0);
            }

            if (!configGroupsForService[configGroup.service_name]) {
              configGroupsForService[configGroup.service_name] = [configGroup.id];
            }
            configGroupsForService[configGroup.service_name].push(configGroup.id);

            /**
             * excluding host names that belongs for current config group from default group
             */
            configGroup.hosts.forEach(function (host) {
              hostNamesForService[configGroup.service_name].splice(hostNamesForService[configGroup.service_name].indexOf(host), 1);
            });
            configGroup = this.parseIt(configGroup, (mapFromVersions ? this.get('config2') : this.get('config')));
            configGroup.parent_config_group_id = App.ServiceConfigGroup.getParentConfigGroupId(configGroup.service_name);
            configGroups.push(configGroup);
          }
        }, this);
      }

      /**
       * generating default config groups
       */
      serviceNames.forEach(function (serviceName) {
        configGroups.push(this.generateDefaultGroup(serviceName, hostNamesForService[serviceName], configGroupsForService[serviceName]));
      }, this);


      configGroups.sort(function (configGroupA, configGroupB) {
        return configGroupA.is_default || (configGroupA.name > configGroupB.name);
      });
      App.store.safeLoadMany(this.get('model'), configGroups);
    }
    console.timeEnd('App.configGroupsMapper');
  },

  /**
   * Get array with all hosts
   *
   * @returns {String[]}
   * @private
   */
  _getAllHosts: function() {
    return App.get('allHostNames.length') ? App.get('allHostNames') : Object.keys(App.get('router.installerController.content.hosts'));
  },

  /**
   * generate mock object for default config group
   * @param {string} serviceName
   * @param {string[]} [hostNames=null]
   * @param {Array} childConfigGroups
   * @returns {{id: string, name: string, service_name: string, description: string, host_names: [string], service_id: string, is_default: boolean}}
   */
  generateDefaultGroup: function (serviceName, hostNames, childConfigGroups) {
    return {
      id: App.ServiceConfigGroup.getParentConfigGroupId(serviceName),
      name: 'Default',
      service_name: serviceName,
      description: 'Default cluster level ' + App.format.role(serviceName, true) + ' configuration',
      hosts: hostNames ? hostNames.slice() : this._getAllHosts().slice(),
      child_config_groups: childConfigGroups ? childConfigGroups.uniq() : [],
      service_id: serviceName,
      desired_configs: [],
      properties: [],
      is_default: true
    }
  }
});
