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

App.ServiceConfigGroup = DS.Model.extend({
  /**
   * @property {string}
   */
  id: DS.attr('string'),

  name: DS.attr('string'),
  serviceName: DS.attr('string'),
  description: DS.attr('string'),
  hosts: DS.attr('array'),
  configVersions: DS.hasMany('App.ConfigVersion'),
  service: DS.belongsTo('App.Service'),
  desiredConfigs: DS.attr('array', {defaultValue: []}),

  /**
   * define if group is persisted on server or is just UI representation
   * temporary groups are deleted from store after persisting on server by <code>App.ServiceConfigGroup.deleteTemporaryRecords</code> method
   * @property {boolean}
   */
  isTemporary: DS.attr('boolean', {defaultValue: false}),

  /**
   * define if group is default
   * @type {boolean}
   */
  isDefault: DS.attr('boolean', {defaultValue: false}),

  /**
   * this flag is used for installed services' config groups
   * if user make changes to them - mark this flag to true
   * @default [false]
   */
  isForUpdate: DS.attr('boolean', {defaultValue: false}),

  /**
   * mark config groups for installed services
   * @default [false]
   */
  isForInstalledService: DS.attr('boolean', {defaultValue: false}),

  /**
   * all hosts that belong to cluster
   */
  clusterHostsBinding: 'App.router.manageConfigGroupsController.clusterHosts',

  /**
   * list of group names that shows which config
   * groups should be updated as dependent when current is changed
   * @type App.ServiceConfigGroup[]
   */
  dependentConfigGroups: DS.attr('object', {defaultValue: {}}),

  /**
   * Parent configuration group for this group.
   * When {@link #isDefault} is true, this value is <code>null</code>
   * When {@link #isDefault} is false, this represents the configuration
   * deltas that are applied on the default.
   */
  parentConfigGroup: DS.belongsTo('App.ServiceConfigGroup'),

  /**
   * Children configuration groups for this group.
   * When {@link #isDefault} is false, this value is <code>null</code>
   * When {@link #isDefault} is true, this represents the various
   * configuration groups that override the default.
   */
  childConfigGroups: DS.hasMany('App.ServiceConfigGroup'),

  hash: DS.attr('string'),

  /**
   * Provides a display friendly name. This includes trimming
   * names to a certain length.
   */
  displayName: function () {
    return App.config.truncateGroupName(this.get('name'));
  }.property('name'),

  /**
   *
   */
  displayNameHosts: Em.computed.format('{0} ({1})', 'displayName', 'hosts.length'),

  switchGroupTextShort: function() {
    return  Em.I18n.t('services.service.config_groups.switchGroupTextShort').format(this.get('displayName'));
  }.property('displayName'),

  switchGroupTextFull: function() {
    return  Em.I18n.t('services.service.config_groups.switchGroupTextFull').format(this.get('displayName'));
  }.property('displayName'),

  /**
   * Provides hosts which are available for inclusion in
   * non-default configuration groups.
   * @type {Array}
   */
  availableHosts: function () {
    if (this.get('isDefault')) return [];
    var unusedHostsMap = this.get('parentConfigGroup.hosts').toWickMap();
    var availableHosts = [];
    var sharedHosts = this.get('clusterHosts');
    // parentConfigGroup.hosts(hosts from default group) - are available hosts, which don't belong to any group
    sharedHosts.forEach(function (host) {
      if (unusedHostsMap[host.get('id')]) {
        availableHosts.pushObject(Ember.Object.create({
          selected: false,
          host: host,
          hostComponentNames: host.get('hostComponents').mapProperty('componentName')
        }));
      }
    });
    return availableHosts;
  }.property('isDefault', 'parentConfigGroup', 'childConfigGroups', 'parentConfigGroup.hosts.@each', 'clusterHosts'),

  /**
   * @type {boolean}
   */
  isAddHostsDisabled: Em.computed.or('isDefault', '!availableHosts.length'),

  /**
   * @type {Array}
   */
  properties: DS.attr('array', {defaultValue: []}),

  /**
   * @type {string}
   */
  propertiesList: function () {
    var result = '';

    if (Array.isArray(this.get('properties'))) {
      this.get('properties').forEach(function (item) {
        result += _.escape(item.name) + " : " + _.escape(item.value) + '<br/>';
      }, this);
    }
    return result;
  }.property('properties.length')
});

App.ServiceConfigGroup.FIXTURES = [];

App.ServiceConfigGroup.getParentConfigGroupId = function(serviceName) {
  return App.ServiceConfigGroup.groupId(serviceName, App.ServiceConfigGroup.defaultGroupName);
};

App.ServiceConfigGroup.groupId = function(serviceName, groupName) {
  return serviceName + "_" + groupName;
};

App.ServiceConfigGroup.defaultGroupName = 'Default';

App.ServiceConfigGroup.deletedGroupName = 'Deleted';

/**
 * Delete all records with isTemporary:true
 * @method
 */
App.ServiceConfigGroup.deleteTemporaryRecords = function () {
  App.ServiceConfigGroup.find().filterProperty('isTemporary').forEach(function(record){
    App.configGroupsMapper.deleteRecord(record);
  }, this);
};