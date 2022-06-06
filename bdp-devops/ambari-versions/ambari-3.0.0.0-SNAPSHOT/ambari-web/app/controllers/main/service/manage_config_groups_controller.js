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
var validator = require('utils/validator');
var hostsManagement = require('utils/hosts');
var numberUtils = require('utils/number_utils');

App.ManageConfigGroupsController = Em.Controller.extend(App.ConfigOverridable, {

  name: 'manageConfigGroupsController',

  /**
   * Determines if needed data is already loaded
   * Loading chain starts at <code>loadHosts</code> and is complete on the <code>loadConfigGroups</code> (if user on
   * the Installer) or on the <code>_onLoadConfigGroupsSuccess</code> (otherwise)
   * @type {boolean}
   */
  isLoaded: false,

  /**
   * Determines if user currently is on the Cluster Installer
   * @type {boolean}
   */
  isInstaller: false,

  /**
   * Determines if user currently is on the Add Service Wizard
   * @type {boolean}
   */
  isAddService: false,

  /**
   * Current service name
   * @type {string}
   */
  serviceName: null,

  /**
   * @type {App.ConfigGroup[]}
   */
  configGroups: [],

  /**
   * @type {App.ConfigGroup[]}
   */
  originalConfigGroups: [],

  /**
   * @type {App.ConfigGroup}
   */
  selectedConfigGroup: null,

  /**
   * @type {string[]}
   */
  selectedHosts: [],

  /**
   * List of all hosts in the cluster
   * @type {{
   *  id: string,
   *  ip: string,
   *  osType: string,
   *  osArch: string,
   *  hostName: string,
   *  publicHostName: string,
   *  cpu: number,
   *  memory: number,
   *  diskTotal: string,
   *  diskFree: string,
   *  disksMounted: number,
   *  hostComponents: {
   *    componentName: string,
   *    displayName: string
   *  }[]
   * }[]}
   */
  clusterHosts: [],

  /**
   * trigger <code>selectDefaultGroup</code> after group delete
   * @type {null}
   */
  groupDeleteTrigger: null,

  /**
   * List of available service components for <code>serviceName</code>
   * @type {{componentName: string, displayName: string, selected: boolean}[]}
   */
  componentsForFilter: function () {
    return App.StackServiceComponent.find().filterProperty('serviceName', this.get('serviceName')).map(function (component) {
      return Em.Object.create({
        componentName: component.get('componentName'),
        displayName: App.format.role(component.get('componentName'), false),
        selected: false
      });
    });
  }.property('serviceName'),

  /**
   * Determines when host may be deleted from config group
   * @type {boolean}
   */
  isDeleteHostsDisabled: function () {
    var selectedConfigGroup = this.get('selectedConfigGroup');
    if (selectedConfigGroup) {
      return selectedConfigGroup.get('isDefault') || this.get('selectedHosts').length === 0;
    }
    return true;
  }.property('selectedConfigGroup', 'selectedConfigGroup.hosts.length', 'selectedHosts.length'),

  /**
   * Map with modified/deleted/created config groups
   * @type {{
   *  toClearHosts: App.ConfigGroup[],
   *  toDelete: App.ConfigGroup[],
   *  toSetHosts: App.ConfigGroup[],
   *  toCreate: App.ConfigGroup[]
   * }}
   */
  hostsModifiedConfigGroups: {},

  /**
   * Trim the tooltip text to show first 500 characters of properties list
   * @type {string}
   */
  tooltipText: function() {
    var selectedConfigGroup = this.get('selectedConfigGroup'),
      propertiesList = selectedConfigGroup.get('propertiesList'),
      trimLength = 500,
      trimmedText = "",
      noOfRemainingProperties = 0,
      index = 0,
      propertyText = "",
      addDots = false;
    if(propertiesList.length > trimLength) {
      // Adjust trim length based on occurrence of <br/> around trim length
      index = propertiesList.substring(trimLength-10, trimLength+10).indexOf("<br/>");
      if(index > -1) {
        trimLength = trimLength - 10 + index;
      } else {
        addDots = true;
      }
      trimmedText = propertiesList.substring(0, trimLength);
      if(addDots) {
        trimmedText += " ...";
      }
      noOfRemainingProperties = (propertiesList.substring(trimLength).match(new RegExp("<br/>", "g")) || []).length - 1;
      if(noOfRemainingProperties > 0) {
        propertyText = (noOfRemainingProperties > 1) ? "properties" : "property";
        trimmedText += "<br/> and " + noOfRemainingProperties + " more " + propertyText;
      }
    } else {
      trimmedText = propertiesList;
    }
    return trimmedText;
  }.property('selectedConfigGroup.propertiesList'),

  /**
   * Check when some config group was changed and updates <code>hostsModifiedConfigGroups</code> once
   * @method hostsModifiedConfigGroupsObs
   */
  hostsModifiedConfigGroupsObs: function() {
    Em.run.once(this, this.hostsModifiedConfigGroupsObsOnce);
  }.observes('selectedConfigGroup.hosts.@each', 'selectedConfigGroup.hosts.length', 'selectedConfigGroup.description', 'configGroups', 'isLoaded'),

  /**
   * Update <code>hostsModifiedConfigGroups</code>-value
   * Called once in the <code>hostsModifiedConfigGroupsObs</code>
   * @method hostsModifiedConfigGroupsObsOnce
   * @returns {boolean}
   */
  hostsModifiedConfigGroupsObsOnce: function() {
    if (!this.get('isLoaded')) {
      return false;
    }
    var groupsToClearHosts = [];
    var groupsToDelete = [];
    var groupsToSetHosts = [];
    var groupsToCreate = [];
    var groups = this.get('configGroups');
    var originalGroups = [];
    var originalGroupsMap = {};

    this.get('originalConfigGroups').forEach(function(item){
      if (!item.is_default) {
        originalGroupsMap[item.id] = item;
        originalGroups.push(item);
      }
    }, this);

    groups.forEach(function (groupRecord) {
      if (!groupRecord.get('isDefault')) {
        var originalGroup = originalGroupsMap[groupRecord.get('id')];
        if (originalGroup) {
          if (!(JSON.stringify(groupRecord.get('hosts').slice().sort()) === JSON.stringify(originalGroup.hosts.sort()))) {
            groupsToClearHosts.push(groupRecord);
            if (groupRecord.get('hosts').length) {
              groupsToSetHosts.push(groupRecord);
            }
            // should update name or description
          } else if (groupRecord.get('description') !== originalGroup.description || groupRecord.get('name') !== originalGroup.name) {
            groupsToSetHosts.push(groupRecord);
          }
          delete originalGroupsMap[groupRecord.get('id')];
        } else {
          groupsToCreate.push({
            id: groupRecord.get('id'),
            name: groupRecord.get('name'),
            description: groupRecord.get('description'),
            hosts: groupRecord.get('hosts').slice(0),
            service_id: groupRecord.get('serviceName'),
            desired_configs: groupRecord.get('desiredConfigs'),
            properties: groupRecord.get('properties')
          });
        }
      }
    });

    //groups to delete
    for (var id in originalGroupsMap) {
      groupsToDelete.push(App.ServiceConfigGroup.find(id));
    }

    this.set('hostsModifiedConfigGroups', {
      toClearHosts: groupsToClearHosts,
      toDelete: groupsToDelete,
      toSetHosts: groupsToSetHosts,
      toCreate: groupsToCreate,
      initialGroups: originalGroups
    });
  },

  /**
   * Determines if some changes were done with config groups
   * @use hostsModifiedConfigGroups
   * @type {boolean}
   */
  isHostsModified: function () {
    if (!this.get('isLoaded')) {
      return false;
    }
    var ignoreKeys = ['initialGroups'];
    var modifiedGroups = this.get('hostsModifiedConfigGroups');
    return Em.keys(modifiedGroups).map(function (key) {
      return ignoreKeys.contains(key) ? 0 : Em.get(modifiedGroups[key], 'length');
    }).reduce(Em.sum, 0) > 0;
  }.property('hostsModifiedConfigGroups'),

  /**
   * Resort config groups according to order:
   * default group first, other - last
   * @method resortConfigGroup
   */
  resortConfigGroup: function() {
    var configGroups = Em.copy(this.get('configGroups'));
    if(configGroups.length < 2) return;
    var defaultConfigGroup = configGroups.findProperty('isDefault');
    configGroups.removeObject(defaultConfigGroup);
    var sorted = [defaultConfigGroup].concat(configGroups.sortProperty('name'));

    this.removeObserver('configGroups.@each.name', this, 'resortConfigGroup');
    this.set('configGroups', sorted);
    this.addObserver('configGroups.@each.name', this, 'resortConfigGroup');
  }.observes('configGroups.@each.name'),

  /**
   * Load hosts from server or
   *  get them from installerController if user on the install wizard
   *  get them from isAddServiceController if user on the add service wizard
   * @method loadHosts
   */
  loadHosts: function() {
    this.set('isLoaded', false);
    if (this.get('isInstaller') && !this.get('isAddService')) {
      var hostNames = App.router.get('installerController').get('allHosts').mapProperty('hostName').join();
      this.loadInstallerHostsFromServer(hostNames);
    } else {
      this.loadHostsFromServer();
    }
    this.loadConfigGroups(this.get('serviceName'));
  },

  /**
   * Request all hosts directly from server
   * @method loadHostsFromServer
   * @return {$.ajax}
   */
  loadHostsFromServer: function() {
    return App.ajax.send({
      name: 'hosts.config_groups',
      sender: this,
      data: {},
      success: '_loadHostsFromServerSuccessCallback',
      error: '_loadHostsFromServerErrorCallback'
    });
  },

  /**
   * Success-callback for <code>loadHostsFromServer</code>
   * Parse hosts response and wrap them into Ember.Object
   * @param {object} data
   * @method _loadHostsFromServerSuccessCallback
   * @private
   */
  _loadHostsFromServerSuccessCallback: function (data) {
    var wrappedHosts = [],
        newlyAddedHostComponentsMap = this.getNewlyAddedHostComponentsMap();

    data.items.forEach(function (host) {
      var hostComponents = [];
      var diskInfo = host.Hosts.disk_info.filter(function (item) {
        return /^ext|^ntfs|^fat|^xfs/i.test(item.type);
      });
      if (diskInfo.length) {
        diskInfo = diskInfo.reduce(function (a, b) {
          return {
            available: parseInt(a.available) + parseInt(b.available),
            size: parseInt(a.size) + parseInt(b.size)
          };
        });
      }
      host.host_components.forEach(function (hostComponent) {
        hostComponents.push(Em.Object.create({
          componentName: hostComponent.HostRoles.component_name,
          displayName: App.format.role(hostComponent.HostRoles.component_name, false)
        }));
      }, this);
      if (this.get('isAddService') && newlyAddedHostComponentsMap[host.Hosts.host_name]) {
        hostComponents.pushObjects(newlyAddedHostComponentsMap[host.Hosts.host_name]);
      }
      wrappedHosts.pushObject(Em.Object.create({
          id: host.Hosts.host_name,
          ip: host.Hosts.ip,
          osType: host.Hosts.os_type,
          osArch: host.Hosts.os_arch,
          hostName: host.Hosts.host_name,
          publicHostName: host.Hosts.public_host_name,
          cpu: host.Hosts.cpu_count,
          memory: host.Hosts.total_mem,
          diskTotal: numberUtils.bytesToSize(diskInfo.size, 0, undefined, 1024),
          diskFree: numberUtils.bytesToSize(diskInfo.available, 0, undefined, 1024),
          disksMounted: host.Hosts.disk_info.length,
          hostComponents: hostComponents
        }
      ));
    }, this);

    this.set('clusterHosts', wrappedHosts);
  },

  /**
   * Error-callback for <code>loadHostsFromServer</code>
   * @method _loadHostsFromServerErrorCallback
   * @private
   */
  _loadHostsFromServerErrorCallback: function () {
    this.set('clusterHosts', []);
  },

  /**
   *
   * @returns {{}}
   */
  getNewlyAddedHostComponentsMap: function () {
    var newlyAddedHostComponentsMap = {};
    var masters = App.router.get('addServiceController.content.masterComponentHosts') || [];
    var slaves = App.router.get('addServiceController.content.slaveComponentHosts') || [];
    var clients = (App.router.get('addServiceController.content.clients') || []);

    clients = clients.filterProperty('isInstalled', false).map(function (component) {
      return Em.Object.create({
        componentName: component.component_name,
        displayName: component.display_name
      });
    });

    masters.forEach(function (component) {
      if (!component.isInstalled) {
        if (!newlyAddedHostComponentsMap[component.hostName]) {
          newlyAddedHostComponentsMap[component.hostName] = [];
        }
        newlyAddedHostComponentsMap[component.hostName].push(Em.Object.create({
          componentName: component.component,
          displayName: component.display_name
        }));
      }
    });

    slaves.forEach(function (component) {
      component.hosts.forEach(function (host) {
        if (!host.isInstalled) {
          if (!newlyAddedHostComponentsMap[host.hostName]) {
            newlyAddedHostComponentsMap[host.hostName] = [];
          }
          if (component.componentName === 'CLIENT') {
            newlyAddedHostComponentsMap[host.hostName].pushObjects(clients);
          } else {
            newlyAddedHostComponentsMap[host.hostName].push(Em.Object.create({
              componentName: component.componentName,
              displayName: component.displayName
            }));
          }
        }
      });
    });
    return newlyAddedHostComponentsMap;
  },

  /**
   * Load config groups from server if user is on the already installed cluster
   * If not - use loaded data form wizardStep7Controller
   * @param {string} serviceName
   * @method loadConfigGroups
   */
  loadConfigGroups: function (serviceName) {
    if (this.get('isInstaller')) {
      var configGroups = App.router.get('wizardStep7Controller.selectedService.configGroups').slice(0);
      var originalConfigGroups = this.generateOriginalConfigGroups(configGroups);
      this.setProperties({
        configGroups: configGroups,
        originalConfigGroups: originalConfigGroups,
        isLoaded: true
      });
    }
    else {
      this.set('serviceName', serviceName);
      App.ajax.send({
        name: 'service.load_config_groups',
        data: {
          serviceName: serviceName
        },
        sender: this,
        success: '_onLoadConfigGroupsSuccess'
      });
    }
  },

  /**
   * Success-callback for <code>loadConfigGroups</code>
   * @param {object} data
   * @private
   * @method _onLoadConfigGroupsSuccess
   */
  _onLoadConfigGroupsSuccess: function (data) {
    var serviceName = this.get('serviceName');

    App.configGroupsMapper.map(data, false, [serviceName]);

    var configGroups = App.ServiceConfigGroup.find().filterProperty('serviceName', serviceName);
    var rawConfigGroups = this.generateOriginalConfigGroups(configGroups);
    var groupToTypeToTagMap = {};

    rawConfigGroups.forEach(function (item) {
      if (Array.isArray(item.desired_configs)) {
        item.desired_configs.forEach(function (config) {
          if (!groupToTypeToTagMap[item.name]) {
            groupToTypeToTagMap[item.name] = {};
          }
          groupToTypeToTagMap[item.name][config.type] = config.tag;
        });
      }
    });

    this.set('configGroups', configGroups);
    this.set('originalConfigGroups', rawConfigGroups);
    this.loadProperties(groupToTypeToTagMap);
    this.set('isLoaded', true);
  },

  /**
   *
   * @param {Array} configGroups
   * @returns {Array}
   */
  generateOriginalConfigGroups: function(configGroups) {
    var self = this;
    return configGroups.map(function (item) {
      return self.createOriginalRecord(item);
    });
  },

  /**
   *  Return object to use for loading to model with correct names for object keys
   * @param configGroup - config group object from model
   * @returns {Object}
   */
  createOriginalRecord: function (configGroup) {
    return {
      id: configGroup.get('id'),
      name: configGroup.get('name'),
      service_name: configGroup.get('serviceName'),
      description: configGroup.get('description'),
      hosts: configGroup.get('hosts').slice(0),
      service_id: configGroup.get('serviceName'),
      desired_configs: configGroup.get('desiredConfigs'),
      is_default: configGroup.get('isDefault'),
      child_config_groups: configGroup.get('childConfigGroups') ? configGroup.get('childConfigGroups').mapProperty('id') : [],
      parent_config_group_id: configGroup.get('parentConfigGroup.id'),
      properties: configGroup.get('properties')
    };
  },

  /**
   *
   * @param {object} groupToTypeToTagMap
   * @method loadProperties
   */
  loadProperties: function (groupToTypeToTagMap) {
    var typeTagToGroupMap = {};
    var urlParams = [];
    for (var group in groupToTypeToTagMap) {
      var overrideTypeTags = groupToTypeToTagMap[group];
      for (var type in overrideTypeTags) {
        var tag = overrideTypeTags[type];
        typeTagToGroupMap[type + "///" + tag] = group;
        urlParams.push('(type=' + type + '&tag=' + tag + ')');
      }
    }
    var params = urlParams.join('|');
    if (urlParams.length) {
      App.ajax.send({
        name: 'config.host_overrides',
        sender: this,
        data: {
          params: params,
          typeTagToGroupMap: typeTagToGroupMap
        },
        success: '_onLoadPropertiesSuccess'
      });
    }
  },

  /**
   * Success-callback for <code>loadProperties</code>
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @private
   * @method _onLoadPropertiesSuccess
   */
  _onLoadPropertiesSuccess: function (data, opt, params) {
    var groupToPropertiesMap = {};
    data.items.forEach(function (configs) {
      var group = params.typeTagToGroupMap[configs.type + "///" + configs.tag];
      if (!groupToPropertiesMap[group]) {
        groupToPropertiesMap[group] = [];
      }
      for (var config in configs.properties) {
        groupToPropertiesMap[group].push({
          name: config,
          value: configs.properties[config],
          type: configs.type
        });
      }
    }, this);
    for (var g in groupToPropertiesMap) {
      this.get('configGroups').findProperty('name', g).set('properties', groupToPropertiesMap[g]);
    }
  },

  /**
   * Show popup with properties overridden in the selected config group
   * @method showProperties
   */
  showProperties: function () {
    var properties = this.get('selectedConfigGroup.propertiesList').htmlSafe();
    if (properties) {
      App.showAlertPopup(Em.I18n.t('services.service.config_groups_popup.properties'), properties);
    }
  },

  /**
   * Show popup with hosts to add to the selected config group
   * @returns {boolean}
   * @method addHosts
   */
  addHosts: function () {
    if (this.get('selectedConfigGroup.isAddHostsDisabled')) {
      return false;
    }
    var availableHosts = this.get('selectedConfigGroup.availableHosts');
    var popupDescription = {
      header: Em.I18n.t('hosts.selectHostsDialog.title'),
      dialogMessage: Em.I18n.t('hosts.selectHostsDialog.message').format(this.get('selectedConfigGroup.displayName'))
    };
    hostsManagement.launchHostsSelectionDialog(availableHosts, [], false, this.get('componentsForFilter'), this.addHostsCallback.bind(this), popupDescription);
  },

  /**
   * Remove selected hosts from default group (<code>selectedConfigGroup.parentConfigGroup</code>) and add them to the <code>selectedConfigGroup</code>
   * @param {string[]} selectedHosts
   * @method addHostsCallback
   */
  addHostsCallback: function (selectedHosts) {
    if (selectedHosts) {
      var sortedHosts;
      var group = this.get('selectedConfigGroup');
      var parentGroupHosts = group.get('parentConfigGroup.hosts');
      var newHostsForParentGroup = parentGroupHosts.filter(function(hostName) {
        return !selectedHosts.contains(hostName);
      });
      group.get('hosts').pushObjects(selectedHosts);
      sortedHosts = group.get('hosts').sort();
      group.set('hosts', sortedHosts);
      group.set('parentConfigGroup.hosts', newHostsForParentGroup);
    }
  },

  /**
   * Delete hosts from <code>selectedConfigGroup</code> and move them to the Default group (<code>selectedConfigGroup.parentConfigGroup</code>)
   * @method deleteHosts
   */
  deleteHosts: function () {
    if (this.get('isDeleteHostsDisabled')) {
      return;
    }
    var hosts = this.get('selectedHosts').slice();
    var newHosts = [];
    var selectedGroup = this.get('selectedConfigGroup');
    var parentGroup = this.get('selectedConfigGroup.parentConfigGroup');
    selectedGroup.get('hosts').forEach(function(host) {
      if (!hosts.contains(host)) {
        newHosts.pushObject(host);
      }
    });
    selectedGroup.set('hosts', newHosts);
    parentGroup.set('hosts', parentGroup.get('hosts').pushObjects(hosts).slice().sort());
    this.set('selectedHosts', []);
  },

  /**
   * show popup for confirmation delete config group
   * @method confirmDelete
   */
  confirmDelete: function () {
    var self = this;
    App.showConfirmationPopup(function() {
      self.deleteConfigGroup();
    });
  },

  /**
   * delete selected config group (stored in the <code>selectedConfigGroup</code>)
   * then select default config group
   * @method deleteConfigGroup
   */
  deleteConfigGroup: function () {
    var selectedConfigGroup = this.get('selectedConfigGroup');
    if (this.get('isDeleteGroupDisabled')) {
      return;
    }
    //move hosts of group to default group (available hosts)
    this.set('selectedHosts', selectedConfigGroup.get('hosts'));
    this.deleteHosts();
    this.get('configGroups').removeObject(selectedConfigGroup);
    App.configGroupsMapper.deleteRecord(selectedConfigGroup);
    this.set('selectedConfigGroup', this.get('configGroups').findProperty('isDefault'));
    this.propertyDidChange('groupDeleteTrigger');
  },

  /**
   * rename new config group (not allowed for default group)
   * @method renameConfigGroup
   */
  renameConfigGroup: function () {
    if(this.get('selectedConfigGroup.isDefault')) {
      return;
    }
    var self = this;
    var renameGroupPopup = App.ModalPopup.show({
      header: Em.I18n.t('services.service.config_groups.rename_config_group_popup.header'),

      bodyClass: Em.View.extend({
        templateName: require('templates/main/service/new_config_group')
      }),

      configGroupName: self.get('selectedConfigGroup.name'),

      configGroupDesc: self.get('selectedConfigGroup.description'),

      warningMessage: null,

      isDescriptionDirty: false,

      validate: function () {
        var warningMessage = '';
        var originalGroup = self.get('selectedConfigGroup');
        var groupName = this.get('configGroupName').trim();
        if (originalGroup.get('description') !== this.get('configGroupDesc') && !this.get('isDescriptionDirty')) {
          this.set('isDescriptionDirty', true);
        }
        if (originalGroup.get('name').trim() === groupName) {
          if (this.get('isDescriptionDirty')) {
            warningMessage = '';
          } else {
            warningMessage = Em.I18n.t("config.group.selection.dialog.err.name.exists");
          }
        } else {
          if (self.get('configGroups').mapProperty('name').contains(groupName)) {
            warningMessage = Em.I18n.t("config.group.selection.dialog.err.name.exists");
          }
          else if (groupName && !validator.isValidConfigGroupName(groupName)) {
            warningMessage = Em.I18n.t("form.validator.configGroupName");
          }
        }
        this.set('warningMessage', warningMessage);
      }.observes('configGroupName', 'configGroupDesc'),

      disablePrimary: function () {
        return !(this.get('configGroupName').trim().length > 0 && (this.get('warningMessage') !== null && !this.get('warningMessage')));
      }.property('warningMessage', 'configGroupName', 'configGroupDesc'),

      onPrimary: function () {
        self.get('selectedConfigGroup').setProperties({
          name: this.get('configGroupName'),
          description: this.get('configGroupDesc')
        });
        App.store.fastCommit();
        this.hide();
      }
    });
    this.set('renameGroupPopup', renameGroupPopup);
  },

  /**
   * add new config group (or copy existing)
   * @param {boolean} duplicated true - copy <code>selectedConfigGroup</code>, false - create a new one
   * @method addConfigGroup
   */
  addConfigGroup: function (duplicated) {
    duplicated = (duplicated === true);

    var self = this;

    var addGroupPopup = App.ModalPopup.show({
      header: Em.I18n.t('services.service.config_groups.add_config_group_popup.header'),

      bodyClass: Em.View.extend({
        templateName: require('templates/main/service/new_config_group')
      }),

      configGroupName: duplicated ? self.get('selectedConfigGroup.name') + ' Copy' : "",

      configGroupDesc: duplicated ? self.get('selectedConfigGroup.description') + ' (Copy)' : "",

      warningMessage: '',

      didInsertElement: function(){
        this._super();
        this.validate();
        this.$('input').focus();
      },

      validate: function () {
        var warningMessage = '';
        var groupName = this.get('configGroupName').trim();
        if (self.get('configGroups').mapProperty('name').contains(groupName)) {
          warningMessage = Em.I18n.t("config.group.selection.dialog.err.name.exists");
        }
        else if (groupName && !validator.isValidConfigGroupName(groupName)) {
          warningMessage = Em.I18n.t("form.validator.configGroupName");
        }
        this.set('warningMessage', warningMessage);
      }.observes('configGroupName'),

      disablePrimary: function () {
        return !(this.get('configGroupName').trim().length > 0 && !this.get('warningMessage'));
      }.property('warningMessage', 'configGroupName'),

      onPrimary: function () {
        var defaultConfigGroup = self.get('configGroups').findProperty('isDefault'),
          properties = [], serviceName = self.get('serviceName'),
          groupName = this.get('configGroupName').trim(),
          newGroupId = (new Date()).getTime();

        if (duplicated) {
          self.get('selectedConfigGroup.properties').forEach(function (item) {
            var property = App.ServiceConfigProperty.create($.extend(false, {}, item));
            property.set('group', App.ServiceConfigGroup.find(newGroupId));
            properties.push(property);
          });
        }

        App.store.safeLoad(App.ServiceConfigGroup, {
          id: newGroupId,
          name: groupName,
          description: this.get('configGroupDesc'),
          isDefault: false,
          parent_config_group_id: App.ServiceConfigGroup.getParentConfigGroupId(serviceName),
          service_id: serviceName,
          service_name: serviceName,
          hosts: [],
          desired_configs: duplicated ? self.get('selectedConfigGroup.desiredConfigs') : [],
          properties: duplicated ? properties : [],
          is_temporary: true
        });
        App.store.fastCommit();
        var childConfigGroups = defaultConfigGroup.get('childConfigGroups').mapProperty('id');
        childConfigGroups.push(newGroupId);
        App.store.safeLoad(App.ServiceConfigGroup, App.configGroupsMapper.generateDefaultGroup(self.get('serviceName'), defaultConfigGroup.get('hosts'), childConfigGroups));
        App.store.fastCommit();
        self.get('configGroups').pushObject(App.ServiceConfigGroup.find(newGroupId));
        this.hide();
      }
    });
    this.set('addGroupPopup', addGroupPopup);
  },

  /**
   * Duplicate existing config group
   * @method duplicateConfigGroup
   */
  duplicateConfigGroup: function() {
    this.addConfigGroup(true);
  },

  /**
   * Show popup with config groups
   * User may edit/create/delete them
   * @param {Em.Controller} controller
   * @param {App.Service} service
   * @returns {App.ModalPopup}
   * @method manageConfigurationGroups
   */
  manageConfigurationGroups: function (controller, service) {
    var configsController = this;
    var serviceData = (controller && controller.get('selectedService')) || service;
    var serviceName = serviceData.get('serviceName');
    var displayName = serviceData.get('displayName');
    this.setProperties({
      isInstaller: !!controller,
      serviceName: serviceName
    });
    if (controller) {
      configsController.set('isAddService', controller.get('content.controllerName') == 'addServiceController');
    }
    return App.ModalPopup.show({

      header: Em.I18n.t('services.service.config_groups_popup.header').format(displayName),

      bodyClass: App.MainServiceManageConfigGroupView.extend({
        serviceName: serviceName,
        displayName: displayName,
        controller: configsController
      }),

      classNames: ['common-modal-wrapper', 'manage-configuration-group-popup'],
      modalDialogClasses: ['modal-lg'],

      primary: Em.I18n.t('common.save'),

      autoHeight: false,

      subViewController: configsController,

      /**
       * handle onPrimary action particularly in wizard
       * @param {Em.Controller} controller
       * @param {object} modifiedConfigGroups
       */
      onPrimaryWizard: function (controller, modifiedConfigGroups) {
        controller.set('selectedService.configGroups', configsController.get('configGroups'));
        controller.selectedServiceObserver();
        if (controller.get('name') == "wizardStep7Controller") {
          if (controller.get('selectedService.selected') === false && modifiedConfigGroups.toDelete.length > 0) {
            controller.setGroupsToDelete(modifiedConfigGroups.toDelete);
          }
          configsController.persistConfigGroups();
          this.updateConfigGroupOnServicePage();
        }
        this.hide();
      },

      onClose: function () {
        //<code>_super</code> has to be called before <code>resetGroupChanges</code>
        var originalGroups = this.get('subViewController.originalConfigGroups').slice(0);
        this._super();
        this.resetGroupChanges(originalGroups);
      },

      onSecondary: function () {
        this.onClose();
      },

      /**
       * reset group changes made by user
       * @param {Array} originalGroups
       */
      resetGroupChanges: function (originalGroups) {
        if (this.get('subViewController.isHostsModified')) {
          App.ServiceConfigGroup.find().clear();
          App.store.safeLoadMany(App.ServiceConfigGroup, originalGroups);
        }
      },

      /**
       * run requests which delete config group and clear its hosts
       * @param {Function} finishFunction
       * @param {object} modifiedConfigGroups
       */
      runClearCGQueue: function (finishFunction, modifiedConfigGroups) {
        var counter = 0;
        var dfd = $.Deferred();
        var doneFunction = function (xhr, text, errorThrown) {
          counter--;
          if (counter === 0) dfd.resolve();
          finishFunction(xhr, text, errorThrown);
        };

        modifiedConfigGroups.toClearHosts.forEach(function (cg) {
          counter++;
          configsController.updateConfigurationGroup(cg, doneFunction, doneFunction)
        }, this);
        modifiedConfigGroups.toDelete.forEach(function (cg) {
          counter++;
          configsController.deleteConfigurationGroup(cg, doneFunction, doneFunction);
        }, this);
        if (counter === 0) dfd.resolve();
        return dfd.promise();
      },

      /**
       * run requests which change properties of config group
       * @param {Function} finishFunction
       * @param {object} modifiedConfigGroups
       */
      runModifyCGQueue: function (finishFunction, modifiedConfigGroups) {
        var counter = 0;
        var dfd = $.Deferred();
        var doneFunction = function (xhr, text, errorThrown) {
          counter--;
          if (counter === 0) dfd.resolve();
          finishFunction(xhr, text, errorThrown);
        };

        modifiedConfigGroups.toSetHosts.forEach(function (cg) {
          counter++;
          configsController.updateConfigurationGroup(cg, doneFunction, doneFunction);
        }, this);
        if (counter === 0) dfd.resolve();
        return dfd.promise();
      },

      /**
       * run requests which create new config group
       * @param {Function} finishFunction
       * @param {object} modifiedConfigGroups
       */
      runCreateCGQueue: function (finishFunction, modifiedConfigGroups) {
        var counter = 0;
        var dfd = $.Deferred();
        var doneFunction = function (xhr, text, errorThrown) {
          counter--;
          if (counter === 0) dfd.resolve();
          finishFunction(xhr, text, errorThrown);
        };

        modifiedConfigGroups.toCreate.forEach(function (cg) {
          counter++;
          configsController.postNewConfigurationGroup(cg, doneFunction);
        }, this);
        if (counter === 0) dfd.resolve();
        return dfd.promise();
      },

      onPrimary: function () {
        var modifiedConfigGroups = configsController.get('hostsModifiedConfigGroups');
        var errors = [];
        var self = this;
        var finishFunction = function (xhr, text, errorThrown) {
          if (xhr && typeof (errorThrown) === 'string') {
            var error = xhr.status + "(" + errorThrown + ") ";
            try {
              var json = $.parseJSON(xhr.responseText);
              error += json.message;
            } catch (err) {
            }
            errors.push(error);
          }
        };

        // Save modified config-groups
        if (controller) {
          //called only in Wizard
          return this.onPrimaryWizard(controller, modifiedConfigGroups);
        }

        this.runClearCGQueue(finishFunction, modifiedConfigGroups).done(function () {
          self.runModifyCGQueue(finishFunction, modifiedConfigGroups).done(function () {
            self.runCreateCGQueue(finishFunction, modifiedConfigGroups).done(function () {
              if (errors.length > 0) {
                self.get('subViewController').set('errorMessage', errors.join(". "));
              } else {
                if (!self.get('isAddService') && !self.get('isInstaller')) {
                  //update service config versions only if it is service configs page
                  App.router.get('mainServiceInfoConfigsController').loadServiceConfigVersions().done(function () {
                    self.updateConfigGroupOnServicePage();
                    self.hide();
                  });
                } else {
                  self.updateConfigGroupOnServicePage();
                  self.hide();
                }
              }
            });
          });
        });
      },

      updateConfigGroupOnServicePage: function () {
        var selectedConfigGroup = configsController.get('selectedConfigGroup');
        var managedConfigGroups = configsController.get('configGroups').slice(0);
        if (!controller) {
          controller = App.router.get('mainServiceInfoConfigsController');
          //controller.set('configGroups', managedConfigGroups);
          controller.loadConfigGroups([controller.get('content.serviceName')]);
        } else {
          controller.set('selectedService.configGroups', managedConfigGroups);
        }

        var selectEventObject = {};
        //check whether selectedConfigGroup exists
        if (selectedConfigGroup && controller.get('configGroups').someProperty('name', selectedConfigGroup.get('name'))) {
          selectEventObject.context = selectedConfigGroup;
        } else {
          selectEventObject.context = managedConfigGroups.findProperty('isDefault', true);
        }
        controller.selectConfigGroup(selectEventObject);
      },

      updateButtons: function () {
        var modified = this.get('subViewController.isHostsModified');
        this.set('disablePrimary', !modified);
      }.observes('subViewController.isHostsModified')
    });
  },

  loadInstallerHostsFromServer: function (hostNames) {
    return App.ajax.send({
      name: 'hosts.info.install',
      sender: this,
      data: {
        hostNames: hostNames
      },
      success: 'loadInstallerHostsSuccessCallback'
    });
  },

  loadInstallerHostsSuccessCallback: function (data) {
    const allHosts = App.router.get('installerController.allHosts').toMapByProperty('hostName'),
      slaveComponents = App.router.get('installerController.content.slaveComponentHosts'),
      clientComponents = App.router.get('installerController.content.clients'),
      clients = clientComponents.map(client => ({
        componentName: client.component_name,
        displayName: client.display_name
      }));
    let hosts = [];

    slaveComponents.forEach(component => {
      component.hosts.forEach(rawHost => {
        let host = allHosts[rawHost.hostName];
        if (!host.hostComponents) {
          host.hostComponents = [];
        }
        if (component.componentName === 'CLIENT') {
          host.hostComponents.pushObjects(clients);
        }
      });
    });

    data.items.forEach(host => {
      let disksOverallCapacity = 0,
        diskFree = 0;
      host.Hosts.disk_info.forEach(disk => {
        disksOverallCapacity += parseFloat(disk.size);
        diskFree += parseFloat(disk.available);
      });
      hosts.pushObject(Em.Object.create({
        id: host.Hosts.host_name,
        ip: host.Hosts.ip,
        osType: host.Hosts.os_type,
        osArch: host.Hosts.os_arch,
        hostName: host.Hosts.host_name,
        publicHostName: host.Hosts.public_host_name,
        cpu: host.Hosts.cpu_count,
        memory: host.Hosts.total_mem.toFixed(2),
        diskInfo: host.Hosts.disk_info,
        diskTotal: disksOverallCapacity / (1024 * 1024),
        diskFree: diskFree / (1024 * 1024),
        hostComponents: (allHosts[host.Hosts.host_name] && allHosts[host.Hosts.host_name].hostComponents) || []
      }));
    });

    this.set('clusterHosts', hosts);
  }

});
