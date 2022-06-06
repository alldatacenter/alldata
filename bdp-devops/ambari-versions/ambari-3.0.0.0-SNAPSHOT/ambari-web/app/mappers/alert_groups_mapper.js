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

/**
 * Mapper for <code>App.AlertGroup</code>
 * Save general information
 * Doesn't save not changed data (check it using <code>App.cache['previousAlertGroupsFullMap']</code>)
 * Use <code>App.cache['previousAlertGroupsMap']</code> to store map alertDefinitions-alertGroups. This map is used
 * in the <code>App.AlertDefinitionsMapper</code> to correctly link alertDefinitions and alertGroups
 */
App.alertGroupsMapper = App.QuickDataMapper.create({

  model: App.AlertGroup,

  config: {
    id: 'AlertGroup.id',
    name: 'AlertGroup.name',
    default: 'AlertGroup.default',
    targets_key: 'AlertGroup.targets',
    targets_type: 'array',
    targets: {
      item: 'id'
    }
  },

  map: function (json, ignoreDelete) {
    if(Em.isNone(App.cache['previousAlertGroupsFullMap'])) {
      App.cache['previousAlertGroupsFullMap'] = {};
    }
    if (!Em.isNone(json, 'items')) {
      console.time('App.alertGroupsMapper execution time');
      var alertGroups = [],
        self = this,
        groupsMap = {},
        groupsToDelete = App.AlertGroup.find().mapProperty('id'),
        /**
         * AlertGroups-map for <code>App.AlertDefinitionsMappers</code>
         * Format:
         * <code>
         *   {
         *    alert_definition1_id: [alert_group1_id, alert_group2_id],
         *    alert_definition2_id: [alert_group3_id, alert_group1_id],
         *    ...
         *   }
         * </code>
         * @type {object}
         */
        alertDefinitionsGroupsMap = {},
        alertNotificationsGroupsMap = {};

      json.items.forEach(function(item) {
        var group = self.parseIt(item, self.get('config'));
        groupsToDelete = groupsToDelete.without(group.id);
        group.targets = [];
        group.definitions = [];
        if (item.AlertGroup.definitions) {
          item.AlertGroup.definitions.forEach(function (definition) {
            if (!group.definitions.contains(definition.id)) {
              group.definitions.push(definition.id);
            }
            if (Em.isNone(alertDefinitionsGroupsMap[definition.id])) {
              alertDefinitionsGroupsMap[definition.id] = [];
            }
            alertDefinitionsGroupsMap[definition.id].push(group.id);
          });
        }
        if (item.AlertGroup.targets) {
          item.AlertGroup.targets.forEach(function (target) {
            if (!group.targets.contains(target.id)) {
              group.targets.push(target.id);
            }
            if (Em.isNone(alertNotificationsGroupsMap[target.id])) {
              alertNotificationsGroupsMap[target.id] = [];
            }
            alertNotificationsGroupsMap[target.id].push(group.id);
          });
        }

        groupsMap[group.id] = group;
        var previousGroup = App.cache['previousAlertGroupsFullMap'][group.id] ? App.cache['previousAlertGroupsFullMap'][group.id] : {};
        var changedFields = self.getDiscrepancies(group, previousGroup, ['name', 'description', 'default', 'targets', 'definitions']);
        if (Object.keys(changedFields).length) {
          alertGroups.push(group);
        }

      }, this);

      if (!ignoreDelete) {
        groupsToDelete.forEach(function(groupId) {
          self.deleteRecord(App.AlertGroup.find(groupId));
        });
      }

      App.cache['previousAlertGroupsMap'] = alertDefinitionsGroupsMap;
      App.cache['previousAlertGroupsFullMap'] = groupsMap;
      App.cache['alertNotificationsGroupsMap'] = alertNotificationsGroupsMap;
      // initial load takes much more time than others, but it's OK (all data should be saved first time)
      App.store.safeLoadMany(this.get('model'), alertGroups);
      console.timeEnd('App.alertGroupsMapper execution time');
    }
  }
});
