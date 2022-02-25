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

App.ConfigsLoader = Em.Mixin.create(App.GroupsMappingMixin, {

  /**
   * version of default config group, configs of which currently applied
   */
  currentDefaultVersion: null,

  /**
   * defines if service config versions are loaded to model
   * @type {boolean}
   */
  allVersionsLoaded: false,

  /**
   * this method should be used in clear step method
   * @method clearLoadInfo
   */
  clearLoadInfo: function() {
    this.set('allVersionsLoaded', false);
  },

  /**
   * loads all versions that is not saved on UI for current moment
   * @returns {$.ajax}
   */
  loadServiceConfigVersions: function () {
    this.set('allVersionsLoaded', false);
    return App.ajax.send({
      name: 'service.serviceConfigVersions.get',
      data: {
        serviceName: this.get('content.serviceName')
      },
      sender: this,
      success: 'loadServiceConfigVersionsSuccess'
    })
  },

  /**
   * success handler for <code>loadServiceConfigVersions<code>
   * @param data
   */
  loadServiceConfigVersionsSuccess: function (data) {
    if (Em.get(data, 'items.length')) {
      App.serviceConfigVersionsMapper.map(data);
      var currentDefault = data.items.filterProperty('group_name', App.ServiceConfigGroup.defaultGroupName).findProperty('is_current');
      if (currentDefault) {
        this.set('currentDefaultVersion', currentDefault.service_config_version);
      }
    }
    this.set('allVersionsLoaded', true);
    if (!this.get('preSelectedConfigVersion')) {
      this.set('selectedVersion', this.get('currentDefaultVersion'));
    }
    this.set('preSelectedConfigVersion', null);
  },

  /**
   * @method loadPreSelectedConfigVersion
   */
  loadPreSelectedConfigVersion: function() {
    var preSelectedVersion = this.get('preSelectedConfigVersion');

    this.set('selectedVersion', this.get('preSelectedConfigVersion.version'));
    /** handling redirecting from config history page **/
    var self = this;
    this.loadConfigGroups(this.get('servicesToLoad')).done(function() {
      var selectedGroup = App.ServiceConfigGroup.find().find(function(g) {
        return g.get('serviceName') === preSelectedVersion.get('serviceName')
          && (g.get('name') === preSelectedVersion.get('groupName')
              || preSelectedVersion.get('groupName') === App.ServiceConfigGroup.defaultGroupName && g.get('isDefault'));
      });
      self.set('selectedConfigGroup', selectedGroup);
      self.loadSelectedVersion(preSelectedVersion.get('version'), selectedGroup);
      preSelectedVersion = null;
    });
  },

  /**
   * loads current versions of current and dependent services
   * and all current version for config groups
   * @method loadCurrentVersions
   */
  loadCurrentVersions: function() {
    this.set('isCompareMode', false);
    this.set('compareServiceVersion', null);
    this.set('versionLoaded', false);
    this.set('selectedVersion', this.get('currentDefaultVersion'));
    this.set('preSelectedConfigVersion', null);
    this.trackRequestChain(App.ajax.send({
      name: 'service.serviceConfigVersions.get.current',
      sender: this,
      data: {
        serviceNames: this.get('servicesToLoad').join(',')
      },
      success: 'loadCurrentVersionsSuccess'
    }));
  },

  /**
   * success handler for <code>loadCurrentVersions<code>
   * @param data
   * @param opt
   * @param params
   */
  loadCurrentVersionsSuccess: function (data, opt, params) {
    var self = this;
    var serviceGroups;
    App.configGroupsMapper.map(data, true, params.serviceNames.split(','));
    this.loadConfigGroups(params.serviceNames.split(',')).done(function () {
      serviceGroups = App.ServiceConfigGroup.find().filterProperty('serviceName', self.get('content.serviceName'));
      if (self.get('isHostsConfigsPage')) {
        self.set('selectedConfigGroup', serviceGroups.find(function (cg) {
              return !cg.get('isDefault') && cg.get('hosts').contains(self.get('host.hostName'));
            }) || serviceGroups.findProperty('isDefault'));
      } else {
        self.set('selectedConfigGroup', serviceGroups.findProperty('isDefault'));
      }
      self.parseConfigData(data);
    });
  },

  /**
   * loads selected versions of current service
   * @method loadSelectedVersion
   */
  loadSelectedVersion: function (version, switchToGroup, stayInCompare) {
    if (!stayInCompare) {
      this.set('isCompareMode', false);
      this.set('compareServiceVersion', null);
    }
    this.set('versionLoaded', false);
    version = version || this.get('currentDefaultVersion');
    this.clearRecommendationsInfo();
    if (version === this.get('currentDefaultVersion') && (!switchToGroup || switchToGroup.get('isDefault'))) {
      // current version with default group
      this.set('selectedVersion', this.get('currentDefaultVersion'));
      this.loadCurrentVersions();
    } else {
      // - version with DEFAULT config group but not CURRENT
      // - ANY version with NON_DEFAULT config group
      this.loadDefaultGroupVersion(version, switchToGroup);
    }
  },

  /**
   * version with NON_DEFAULT group require properties from CURRENT version of DEFAULT group to correctly display page
   * @param {string} version
   * @param {?Em.Object} switchToGroup
   */
  loadDefaultGroupVersion: function(version, switchToGroup) {
    var versions = this.isVersionDefault(version) ? [version] : [this.get('currentDefaultVersion'), version];
    var selectedVersion = versions.length > 1 ? versions[1] : versions[0];

    this.setSelectedConfigGroup(version, switchToGroup);
    this.set('selectedVersion', selectedVersion);
    this.trackRequest(App.ajax.send({
      name: 'service.serviceConfigVersions.get.multiple',
      sender: this,
      data: {
        serviceName: this.get('content.serviceName'),
        serviceConfigVersions: versions,
        additionalParams: this.get('dependentServiceNames.length') ? '|(service_name.in(' + this.get('dependentServiceNames') + ')%26is_current=true)' : ''
      },
      success: 'loadSelectedVersionsSuccess'
    }));
  },

  /**
   *
   * @param {string} version
   * @param {?Em.Object} switchToGroup
   */
  setSelectedConfigGroup: function(version, switchToGroup) {
    switchToGroup = (this.isVersionDefault(version) && !switchToGroup)
      ? this.get('configGroups').findProperty('isDefault')
      : switchToGroup;

    if (this.get('dataIsLoaded') && switchToGroup) {
      this.set('selectedConfigGroup', switchToGroup);
    }
  },

  /**
   * success handler for <code>loadSelectedVersionsSuccess<code>
   * @param data
   */
  loadSelectedVersionsSuccess: function(data) {
    this.parseConfigData(data);
  }
});
