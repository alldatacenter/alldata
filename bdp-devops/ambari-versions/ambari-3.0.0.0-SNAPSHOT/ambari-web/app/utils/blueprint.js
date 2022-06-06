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

/**
 * @typedef {object} BlueprintObject
 * @property {BlueprintMappings} blueprint
 * @property {BlueprintClusterBindings} blueprint_cluster_bindings
 */
/**
 * @typedef {object} BlueprintMappings
 * @property {BlueprintMappingsHostGroup[]} host_groups
 */
/**
 * @typedef {object[]} BlueprintMappingsHostGroup
 * @property {BlueprintHostGroupComponent[]} components
 * @property {string} name host group name
 */
/**
 * @typedef {object} BlueprintHostGroupComponent
 * @property {string} name component name
 */
/**
 * @typedef {object} BlueprintClusterBindings
 * @property {BlueprintClusterBindingsHostGroup[]} host_groups
 */
/**
 * @typedef {object} BlueprintClusterBindingsHostGroup
 * @property {BlueprintClusterBindingsHostGroupHosts[]} hosts
 * @property {string} name host group name
 */
/**
 * @typedef {object} BlueprintClusterBindingsHostGroupHosts
 * @property {string} fqdn host fqdn
 */
module.exports = {

  /**
   * @param {BlueprintObject} masterBlueprint
   * @param {BlueprintObject} slaveBlueprint
   * @return {BlueprintObject}
   */
  mergeBlueprints: function(masterBlueprint, slaveBlueprint) {
    console.time('mergeBlueprints');
    var self = this;

    // Check edge cases
    if (!slaveBlueprint && !masterBlueprint) {
      throw 'slaveBlueprint or masterBlueprint should not be empty';
    }
    else
      if (slaveBlueprint && !masterBlueprint) {
        return slaveBlueprint;
      }
      else
        if (!slaveBlueprint && masterBlueprint) {
          return masterBlueprint;
        }

    // Work with main case (both blueprint are presented)
    var matches = self.matchGroups(masterBlueprint, slaveBlueprint);

    var res = {
      blueprint: { host_groups: [] },
      blueprint_cluster_binding: { host_groups: [] }
    };

    var tmpObj = {hosts: [], components: []};
    var masterBluePrintHostGroupsCluster = this.blueprintToObject(masterBlueprint, 'blueprint_cluster_binding.host_groups');
    var slaveBluePrintHostGroupsCluster = this.blueprintToObject(slaveBlueprint, 'blueprint_cluster_binding.host_groups');
    var masterBluePrintHostGroupsBlueprint = this.blueprintToObject(masterBlueprint, 'blueprint.host_groups');
    var slaveBluePrintHostGroupsBlueprint = this.blueprintToObject(slaveBlueprint, 'blueprint.host_groups');

    matches.forEach(function (match, i) {
      var group_name = 'host-group-' + (i + 1);

      var masterComponents = match.g1 ? Em.getWithDefault(masterBluePrintHostGroupsBlueprint, match.g1, tmpObj).components : [];
      var slaveComponents = match.g2 ? Em.getWithDefault(slaveBluePrintHostGroupsBlueprint, match.g2, tmpObj).components : [];

      res.blueprint.host_groups.push({
        name: group_name,
        components: masterComponents.concat(slaveComponents)
      });

      var hosts = match.g1 ? Em.getWithDefault(masterBluePrintHostGroupsCluster, match.g1, tmpObj).hosts :
        Em.getWithDefault(slaveBluePrintHostGroupsCluster, match.g2, tmpObj).hosts;

      res.blueprint_cluster_binding.host_groups.push({
        name: group_name,
        hosts: hosts
      });
    });
    console.timeEnd('mergeBlueprints');
    return res;
  },

  /**
   * Convert <code>blueprint</code>-object to the array with keys equal to the host-groups names
   * Used to improve performance when user try to search value in the blueprint using host-group name as search-field
   * Example:
   *  Before:
   *  <pre>
   *    // blueprint
   *    {
   *      blueprint: {
   *        host_groups: [
   *          {
   *            components: [{}, {}, ...],
   *            name: 'n1'
   *          },
   *          {
   *            components: [{}, {}, ...],
   *            name: 'n2'
   *          }
   *        ]
   *      },
   *      blueprint_cluster_binding: {
   *        host_groups: [
   *          {
   *            hosts: [{}, {}, ...],
   *            name: 'n1'
   *          },
   *          {
   *            hosts: [{}, {}, ...],
   *            name: 'n2'
   *          }
   *        ]
   *      }
   *    }
   *  </pre>
   *  Return:
   *  <pre>
   *    // field = 'blueprint_cluster_binding.host_groups'
   *    {
   *      n1: {
   *        hosts: [{}, {}, ...],
   *        name: 'n1'
   *      },
   *      n2: {
   *        hosts: [{}, {}, ...],
   *        name: 'n2'
   *      }
   *    }
   *
   *    // field = 'blueprint.host_groups'
   *    {
   *      n1: {
   *        components: [{}, {}, ...],
   *        name: 'n1'
   *      },
   *      n2: {
   *        components: [{}, {}, ...],
   *        name: 'n2'
   *      }
   *    }
   *  </pre>
   * @param {object} blueprint
   * @param {string} field
   * @returns {object}
   */
  blueprintToObject: function(blueprint, field) {
    var valueToMap = Em.get(blueprint, field);
    if (!Array.isArray(valueToMap)) {
      return {};
    }
    return valueToMap.toMapByProperty('name');
  },

  matchGroups: function(masterBlueprint, slaveBlueprint) {
    var self = this;
    var res = [];

    var groups1 = masterBlueprint.blueprint_cluster_binding.host_groups;
    var groups2 = slaveBlueprint.blueprint_cluster_binding.host_groups;

    var groups1_used = groups1.map(function() { return false; });
    var groups2_used = groups2.map(function() { return false; });

    self.matchGroupsWithLeft(groups1, groups2, groups1_used, groups2_used, res, false);
    self.matchGroupsWithLeft(groups2, groups1, groups2_used, groups1_used, res, true);

    return res;
  },

  matchGroupsWithLeft: function(groups1, groups2, groups1_used, groups2_used, res, inverse) {
    var gs2 = this.groupsToObject(groups2);
    for (var i = 0; i < groups1.length; i++) {
      if (groups1_used[i]) {
        continue;
      }

      var group1 = groups1[i];
      groups1_used[i] = true;

      var group2 = gs2[group1.hosts.mapProperty('fqdn').join(',')];
      if (group2) {
        groups2_used[group2.index] = true;
      }

      var item = {};

      if (inverse) {
        item.g2 = group1.name;
        if (group2) {
          item.g1 = group2.name;
        }
      }
      else {
        item.g1 = group1.name;
        if (group2) {
          item.g2 = group2.name;
        }
      }
      res.push(item);
    }

    // remove unneeded property
    groups2.forEach(function(group) {
      delete group.index;
    });
  },

  /**
   * Convert array of objects to the one object to improve performance with searching objects in the provided array
   * Example:
   *  Before:
   *  <pre>
   *    // groups
   *    [
   *      {
   *        hosts: [
   *          {fqdn: 'h1'}, {fqdn: 'h2'}
   *        ],
   *        name: 'n1'
   *      },
   *      {
   *        hosts: [
   *          {fqdn: 'h3'}, {fqdn: 'h4'}
   *        ]
   *      }
   *    ]
   *  </pre>
   *  Return:
   *  <pre>
   *    {
   *      'h1,h2': {
   *        hosts: [
   *          {fqdn: 'h1'}, {fqdn: 'h2'}
   *        ],
   *        name: 'n1',
   *        index: 0
   *      },
   *      'h3,h4': {
   *        hosts: [
   *          {fqdn: 'h3'}, {fqdn: 'h4'}
   *        ],
   *        name: 'n2',
   *        index: 1
   *      }
   *    }
   *  </pre>
   * @param {{hosts: object[], name: string}[]} groups
   * @returns {object}
   */
  groupsToObject: function (groups) {
    var ret = {};
    groups.forEach(function (group, index) {
      var key = group.hosts.mapProperty('fqdn').join(',');
      ret[key] = group;
      ret[key].index = index;
    });
    return ret;
  },

  /**
   * Remove from blueprint all components expect given components
   * @param {object} blueprint
   * @param {string[]} components
   */
  filterByComponents: function(blueprint, components) {
    if (!blueprint) {
      return null;
    }

    var res = JSON.parse(JSON.stringify(blueprint));
    var emptyGroups = [];

    for (var i = 0; i < res.blueprint.host_groups.length; i++) {
      res.blueprint.host_groups[i].components = res.blueprint.host_groups[i].components.filter(function(c) {
        return components.contains(c.name);
      });

      if (res.blueprint.host_groups[i].components.length == 0) {
        emptyGroups.push(res.blueprint.host_groups[i].name);
      }
    }

    res.blueprint.host_groups = res.blueprint.host_groups.filter(function(g) {
      return !emptyGroups.contains(g.name);
    });

    res.blueprint_cluster_binding.host_groups = res.blueprint_cluster_binding.host_groups.filter(function(g) {
      return !emptyGroups.contains(g.name);
    });

    return res;
  },

  addComponentsToBlueprint: function(blueprint, components) {
    var res = JSON.parse(JSON.stringify(blueprint));

    res.blueprint.host_groups.forEach(function(group) {
      components.forEach(function(component) {
        group.components.push({ name: component });
      });
    });

    return res;
  },

  /**
   * @method buildConfigsJSON - generates JSON according to blueprint format
   * @param {Em.Array} stepConfigs - array of Ember Objects
   * @returns {Object}
   * Example:
   * {
   *   "yarn-env": {
   *     "properties": {
   *       "content": "some value",
   *       "yarn_heapsize": "1024",
   *       "resourcemanager_heapsize": "1024",
   *     }
   *   },
   *   "yarn-log4j": {
   *     "properties": {
   *       "content": "some other value"
   *     }
   *   }
   * }
   */
  buildConfigsJSON: function (stepConfigs) {
    var configurations = {};
    stepConfigs.forEach(function (stepConfig) {
      var configs = stepConfig.get('configs');
      if (!configs) return false;
      configs.forEach(function (config) {
        var type = App.config.getConfigTagFromFileName(config.get('filename'));
        if (!configurations[type]) {
          configurations[type] = {properties: {}}
        }
        configurations[type]['properties'][config.get('name')] = config.get('value');
      });
    });
    return configurations;
  },

  /**
   * @method generateHostGroups
   * @param {Array} hostNames - list of all hostNames
   * @returns {{blueprint: {host_groups: Array}, blueprint_cluster_binding: {host_groups: Array}}}
   */
  generateHostGroups: function(hostNames) {
    var recommendations = {
      blueprint: {
        host_groups: []
      },
      blueprint_cluster_binding: {
        host_groups: []
      }
    };
    var hostsMap = this.getComponentForHosts();

    for (var i = 0; i <= hostNames.length; i++) {
      var host_group = {
        name: "host-group-" + (i + 1),
        components: []
      };
      var hcFiltered = hostsMap[hostNames[i]];
      if (Em.isNone(hcFiltered)) continue;
      for (var j = 0; j < hcFiltered.length; j++) {
        host_group.components.push({
          name: hcFiltered[j]
        });
      }
      recommendations.blueprint.host_groups.push(host_group);
      recommendations.blueprint_cluster_binding.host_groups.push({
        name: "host-group-" + (i + 1),
        hosts: [{
          fqdn: hostNames[i]
        }]
      });
    }
    return recommendations;
  },

  /**
   * Small helper method to update hostMap
   * it perform update of object only
   * if unique component per host is added
   *
   * @param {Object} hostMapObject
   * @param {string[]} hostNames
   * @param {string} componentName
   * @returns {Object}
   * @private
   */
  _generateHostMap: function(hostMapObject, hostNames, componentName) {
    Em.assert('hostMapObject, hostNames, componentName should be defined', !!hostMapObject && !!hostNames && !!componentName);
    if (!hostNames.length) return hostMapObject;
    hostNames.forEach(function(hostName) {
      if (!hostMapObject[hostName])
        hostMapObject[hostName] = [];

      if (!hostMapObject[hostName].contains(componentName))
        hostMapObject[hostName].push(componentName);
    });
    return hostMapObject;
  },

  /**
   * Clean up host groups from components that should be removed
   *
   * @param hostGroups
   * @param serviceNames
   */
  removeDeletedComponents: function(hostGroups, serviceNames) {
    var components = [];
    App.StackService.find().forEach(function(s) {
      if (serviceNames.contains(s.get('serviceName'))) {
        components = components.concat(s.get('serviceComponents').mapProperty('componentName'));
      }
    });

    hostGroups.blueprint.host_groups.forEach(function(hg) {
      hg.components = hg.components.filter(function(c) {
        return !components.contains(c.name);
      })
    });
    return hostGroups;
  },

  /**
   * collect all component names that are present on hosts
   * @returns {object}
   */
  getComponentForHosts: function() {
    var hostsMap = {};
    var componentModels = [App.ClientComponent, App.SlaveComponent, App.MasterComponent];
    componentModels.forEach(function(_model){
      _model.find().forEach(function(c) {
        hostsMap = this._generateHostMap(hostsMap, c.get('hostNames'), c.get('componentName'));
      }, this);  
    }, this);

    this.changeHostsMapForConfigActions(hostsMap);
    return hostsMap;
  },

  /**
   * Adds or removes the component name entry as saved in App.componentToBeAdded and App.componentToBeDeleted from the 'hostsMap'
   * @param hostsMap {object}
   * @private
   * @method {changeHostsMapForConfigActions}
   */
  changeHostsMapForConfigActions: function(hostsMap) {
    var componentToBeAdded =  App.get('componentToBeAdded');
    var componentToBeDeleted =  App.get('componentToBeDeleted');
    if (!App.isEmptyObject(componentToBeAdded)) {
      hostsMap = this._generateHostMap(hostsMap, componentToBeAdded.get('hostNames'), componentToBeAdded.get('componentName'));
    } else if (!App.isEmptyObject(componentToBeDeleted) && hostsMap[componentToBeDeleted.hostName]) {
      var index = hostsMap[componentToBeDeleted.hostName].indexOf(componentToBeDeleted.componentName);
      if (index > -1) {
        hostsMap[componentToBeDeleted.hostName].splice(index, 1);
      }
    }
  },
  /**
   * Returns host-group name by fqdn.
   * Returns <code>null</code> when not found.
   *
   * @param  {object} blueprint
   * @param  {string} fqdn
   * @returns {string|null}
   */
  getHostGroupByFqdn: function(blueprint, fqdn) {
    return Em.getWithDefault(blueprint || {}, 'blueprint_cluster_binding.host_groups', [])
      .filter(function(i) {
        return Em.getWithDefault(i, 'hosts', []).mapProperty('fqdn').contains(fqdn);
      })
      .mapProperty('name')[0] || null;
  },

  /**
   * Add component to specified host group.
   *
   * @param  {object} blueprint
   * @param  {string} componentName
   * @param  {string} hostGroupName
   * @return {object}
   */
  addComponentToHostGroup: function(blueprint, componentName, hostGroupName) {
    var hostGroup = blueprint.blueprint.host_groups.findProperty('name', hostGroupName);
    if (hostGroup) {
      if (!hostGroup.hasOwnProperty('components')) {
        hostGroup.components = [];
      }
      if (!hostGroup.components.someProperty('name', componentName)) {
        hostGroup.components.pushObject({name: componentName});
      }
    }
    return blueprint;
  }
};
