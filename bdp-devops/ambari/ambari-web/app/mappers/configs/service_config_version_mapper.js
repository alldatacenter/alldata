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

App.serviceConfigVersionsMapper = App.QuickDataMapper.create({
  model: App.ServiceConfigVersion,
  config: {
    service_name: 'service_name',
    service_id: 'service_name',
    version: "service_config_version",
    create_time: 'createtime',
    raw_create_time: 'createtime',
    group_id: 'group_id',
    group_name: 'group_name',
    hosts: 'hosts',
    author: 'user',
    notes: 'service_config_version_note',
    is_current: 'is_current',
    index: 'index',
    stack_version: 'stack_id',
    is_compatible: 'is_cluster_compatible'
  },
  map: function (json) {
    console.time('App.serviceConfigVersionsMapper');
    var result = [];
    var itemIds = {};
    var serviceToHostMap = {};

    if (json && json.items) {
      var currentVersionsMap = this.getCurrentVersionMap();

      json.items.forEach(function (item, index) {
        var parsedItem = this.parseIt(item, this.get('config'));
        parsedItem.id = this.makeId(parsedItem.service_name, parsedItem.version);
        parsedItem.group_id = parsedItem.group_id === -1 ? parsedItem.service_name + '_default' : parsedItem.group_id;
        parsedItem.is_requested = true;
        parsedItem.create_time = App.dateTimeWithTimeZone(parsedItem.create_time);
        itemIds[parsedItem.id] = true;
        parsedItem.index = index;
        if (serviceToHostMap[item.service_name]) {
          serviceToHostMap[item.service_name] = serviceToHostMap[item.service_name].concat(item.hosts);
        } else {
          serviceToHostMap[item.service_name] = item.hosts;
        }

        // if loaded only latest versions(later than current), then current version should be reset
        if (parsedItem.is_current && currentVersionsMap[parsedItem.service_name + "_" + parsedItem.group_name]) {
          currentVersionsMap[parsedItem.service_name + "_" + parsedItem.group_name].set('isCurrent', false);
        }
        result.push(parsedItem);
      }, this);

      var itemTotal = parseInt(json.itemTotal);
      if (!isNaN(itemTotal)) {
        App.router.set('mainConfigHistoryController.filteredCount', itemTotal);
      }

      this.setHostsForDefaultCG(serviceToHostMap, result);

      // If on config history page, need to clear the model
      if (App.router.get('currentState.name') === 'configHistory') {
        this.get('model').find().clear();
      }
      App.store.safeLoadMany(this.get('model'), result);
      console.timeEnd('App.serviceConfigVersionsMapper');
    }
  },

  /**
   * sets hostNames for default config group by excluding hostNames
   * that belongs to not default groups from list of all hosts
   * @param {object} serviceToHostMap
   * @param {array} result
   */
  setHostsForDefaultCG: function(serviceToHostMap, result) {
    Object.keys(serviceToHostMap).forEach(function(sName) {
      var defaultHostNames = App.get('allHostNames');
      for (var i = 0; i < serviceToHostMap[sName].length; i++) {
        defaultHostNames = defaultHostNames.without(serviceToHostMap[sName][i]);
      }
      var defVer = result.find(function(v) {
        return v.is_current && v.group_name === App.ServiceConfigGroup.defaultGroupName && v.service_name === sName;
      });
      if (defVer) {
        defVer.hosts = defaultHostNames;
      }
    });
  },

  getCurrentVersionMap: function() {
    var currentVersionsMap = {};
    App.ServiceConfigVersion.find().forEach(function (v) {
      if (v.get('isCurrent')) {
        currentVersionsMap[v.get('serviceName') + "_" + v.get('groupName')] = v;
      }
    });
    return currentVersionsMap;
  },

  /**
   * Conventional method to generate id for instances of <code>App.ServiceConfigVersion</code> model.
   *
   * @param {String} serviceName
   * @param {Number|String} version
   * @returns {String}
   */
  makeId: function(serviceName, version) {
    return serviceName + '_' + version;
  }

});
