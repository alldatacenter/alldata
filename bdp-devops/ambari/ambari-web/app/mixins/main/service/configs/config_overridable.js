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

/**
 * Mixin with methods for config groups and overrides processing
 * Used in the installer step7, service configs page and others
 * @type {Em.Mixin}
 */
App.ConfigOverridable = Em.Mixin.create({

  /**
   *
   * @method createOverrideProperty
   */
  createOverrideProperty: function (event) {
    var serviceConfigProperty = event.contexts[0];
    var serviceConfigController = this.get('isView') ? this.get('controller') : this;
    var selectedConfigGroup = serviceConfigController.get('selectedConfigGroup');
    var isInstaller = this.get('controller.name') === 'wizardStep7Controller';
    var configGroups = (isInstaller) ? serviceConfigController.get('selectedService.configGroups') : serviceConfigController.get('configGroups');

    //user property is added, and it has not been saved, not allow override
    if (serviceConfigProperty.get('isUserProperty') && serviceConfigProperty.get('isNotSaved') && !isInstaller) {
      App.ModalPopup.show({
        header: Em.I18n.t('services.service.config.configOverride.head'),
        body: Em.I18n.t('services.service.config.configOverride.body'),
        secondary: false
      });
      return;
    }
    if (selectedConfigGroup.get('isDefault')) {
      // Launch dialog to pick/create Config-group
      this.launchConfigGroupSelectionCreationDialog(
        this.get('service.serviceName'),
        configGroups,
        serviceConfigProperty,
        function (selectedGroupInPopup) {
          if (selectedGroupInPopup) {
            serviceConfigController.set('overrideToAdd', serviceConfigProperty);
            serviceConfigController.set('selectedConfigGroup', selectedGroupInPopup);
          }
        },
        isInstaller
      );
    }
    else {
      var valueForOverride = (serviceConfigProperty.get('widget') || serviceConfigProperty.get('displayType') == 'checkbox') ? serviceConfigProperty.get('value') : '';
      App.config.createOverride(serviceConfigProperty, {
        "value": valueForOverride,
        "isEditable": true
      }, selectedConfigGroup);
    }
    Em.$('body>.tooltip').remove();
  },

  /**
   * Open popup with list of config groups
   * User may select existing group or create a new one
   * @param {string} serviceId service name like 'HDFS', 'HBASE' etc
   * @param {App.ConfigGroup[]} configGroups
   * @param {App.ConfigProperty} configProperty
   * @param {Function} callback function called after config group is selected (or new one is created)
   * @param {Boolean} isInstaller determines if user is currently on the installer
   * @return {App.ModalPopup}
   * @method launchConfigGroupSelectionCreationDialog
   */
  launchConfigGroupSelectionCreationDialog: function (serviceId, configGroups, configProperty, callback, isInstaller) {
    var self = this;
    var availableConfigGroups = configGroups.slice();
    // delete Config Groups, that already have selected property overridden
    var alreadyOverriddenGroups = [];
    if (configProperty.get('overrides')) {
      alreadyOverriddenGroups = configProperty.get('overrides').mapProperty('group.name');
    }
    var result = [];
    availableConfigGroups.forEach(function (group) {
      if (!group.get('isDefault') && (!alreadyOverriddenGroups.length || !alreadyOverriddenGroups.contains(Em.get(group, 'name')))) {
        result.push(group);
      }
    }, this);
    availableConfigGroups = result;
    var selectedConfigGroup = availableConfigGroups && availableConfigGroups.length > 0 ?
      availableConfigGroups[0] : null;
    var serviceName = App.format.role(serviceId, true);

    return App.ModalPopup.show({
      classNames: ['common-modal-wrapper'],
      modalDialogClasses: ['modal-lg'],
      header: Em.I18n.t('config.group.selection.dialog.title').format(serviceName),
      subTitle: Em.I18n.t('config.group.selection.dialog.subtitle').format(serviceName),
      selectExistingGroupLabel: Em.I18n.t('config.group.selection.dialog.option.select').format(serviceName),
      noGroups: Em.I18n.t('config.group.selection.dialog.no.groups').format(serviceName),
      createNewGroupLabel: Em.I18n.t('config.group.selection.dialog.option.create').format(serviceName),
      createNewGroupDescription: Em.I18n.t('config.group.selection.dialog.option.create.msg').format(serviceName),
      warningMessage: '&nbsp;',
      isWarning: false,
      optionSelectConfigGroup: true,
      optionCreateConfigGroup: Em.computed.not('optionSelectConfigGroup'),
      hasExistedGroups: Em.computed.bool('availableConfigGroups.length'),
      availableConfigGroups: availableConfigGroups,
      selectedConfigGroup: selectedConfigGroup,
      newConfigGroupName: '',
      disablePrimary: function () {
        return !(this.get('optionSelectConfigGroup') || (this.get('newConfigGroupName').trim().length > 0 && !this.get('isWarning')));
      }.property('newConfigGroupName', 'optionSelectConfigGroup', 'warningMessage'),
      onPrimary: function () {
        var popup = this;
        if (this.get('optionSelectConfigGroup')) {
          var selectedConfigGroup = this.get('selectedConfigGroup');
          popup.hide();
          callback(selectedConfigGroup);
          if (!isInstaller) {
            App.get('router.mainServiceInfoConfigsController').doSelectConfigGroup({context: selectedConfigGroup});
          }
        } else {
          var newConfigGroupName = this.get('newConfigGroupName').trim();
          var newConfigGroup = {
            id: (new Date()).getTime(),
            name: newConfigGroupName,
            is_default: false,
            parent_config_group_id: App.ServiceConfigGroup.getParentConfigGroupId(serviceId),
            description: Em.I18n.t('config.group.description.default').format(new Date().toDateString()),
            service_id: serviceId,
            service_name: serviceId,
            hosts: [],
            desired_configs: [],
            properties: []
          };
          if (!isInstaller) {
            self.postNewConfigurationGroup(newConfigGroup, function () {
              newConfigGroup = App.ServiceConfigGroup.find().filterProperty('serviceName', serviceId).findProperty('name', newConfigGroupName);
              self.saveGroupConfirmationPopup(newConfigGroupName);
              callback(newConfigGroup);
              popup.hide();
            });
          } else {
            newConfigGroup.is_temporary = true;
            App.store.safeLoad(App.ServiceConfigGroup, newConfigGroup);
            App.store.fastCommit();
            newConfigGroup = App.ServiceConfigGroup.find(newConfigGroup.id);
            configGroups.pushObject(newConfigGroup);
            self.persistConfigGroups();
            callback(newConfigGroup);
            popup.hide();
          }
        }
      },
      onSecondary: function () {
        this.hide();
        callback(null);
      },
      doSelectConfigGroup: function (event) {
        var configGroup = event.context;
        this.set('selectedConfigGroup', configGroup);
      },
      validate: function () {
        var msg = '&nbsp;';
        var isWarning = false;
        var optionSelect = this.get('optionSelectConfigGroup');
        if (!optionSelect) {
          var nn = this.get('newConfigGroupName').trim();
          if (nn) {
            if (!validator.isValidConfigGroupName(nn)) {
              msg = Em.I18n.t("form.validator.configGroupName");
              isWarning = true;
            } else if (configGroups.mapProperty('name').contains(nn)) {
              msg = Em.I18n.t("config.group.selection.dialog.err.name.exists");
              isWarning = true;
            }
          }
        }
        this.set('warningMessage', msg);
        this.set('isWarning', isWarning);
      }.observes('newConfigGroupName', 'optionSelectConfigGroup'),
      bodyClass: Em.View.extend({
        templateName: require('templates/common/configs/selectCreateConfigGroup'),
        controllerBinding: 'App.router.mainServiceInfoConfigsController',
        selectConfigGroupRadioButton: App.RadioButtonView.extend({
          label: Em.computed.alias('parentView.parentView.selectExistingGroupLabel'),
          checked: Em.computed.alias('parentView.parentView.optionSelectConfigGroup'),
          disabled: Em.computed.not('parentView.parentView.hasExistedGroups'),
          click: function () {
            if (this.get('disabled')) {
              return;
            }
            this.set('parentView.parentView.optionSelectConfigGroup', true);
          },
          didInsertElement: function () {
            if (!this.get('parentView.parentView.hasExistedGroups')) {
              this.set('parentView.parentView.optionSelectConfigGroup', false);
            }
          }
        }),
        createConfigGroupRadioButton: App.RadioButtonView.extend({
          label: Em.computed.alias('parentView.parentView.createNewGroupLabel'),
          checked: Em.computed.not('parentView.parentView.optionSelectConfigGroup'),
          click: function () {
            this.set('parentView.parentView.optionSelectConfigGroup', false);
          }
        })
      })
    });
  },

  /**
   * Create a new config-group for a service.
   *
   * @param {object} newConfigGroupData config group to post to server
   * @param {Function} [callback] Callback function for Success or Error handling
   * @return {$.ajax}
   * @method postNewConfigurationGroup
   */
  postNewConfigurationGroup: function (newConfigGroupData, callback) {
    var typeToPropertiesMap = {};
    newConfigGroupData.properties.forEach(function (property) {
      if (!typeToPropertiesMap[property.get('type')]) {
        typeToPropertiesMap[property.get('type')] = {};
      }
      typeToPropertiesMap[property.get('type')][property.get('name')] = property.get('value');
    });
    var newGroupData = {
      "ConfigGroup": {
        "group_name": newConfigGroupData.name,
        "tag": newConfigGroupData.service_id,
        "description": newConfigGroupData.description,
        "service_name": newConfigGroupData.service_id,
        "desired_configs": newConfigGroupData.desired_configs.map(function (cst) {
          var type = Em.get(cst, 'site') || Em.get(cst, 'type');
          return {
            type: type,
            properties: typeToPropertiesMap[type]
          };
        }),
        "hosts": newConfigGroupData.hosts.map(function (h) {
          return {
            host_name: h
          };
        })
      }
    };
    return App.ajax.send({
      name: 'config_groups.create',
      sender: this,
      data: {
        data: [newGroupData],
        modelData: newConfigGroupData
      },
      success: 'postNewConfigurationGroupSuccess',
      error: 'postNewConfigurationGroupError'
    }).always(function (xhr, text, errorThrown) {
      if (callback) {
        callback(xhr, text, errorThrown);
      }
    });
  },

  /**
   *
   * @param {string} response
   * @param {object} opt
   * @param {object} params
   */
  postNewConfigurationGroupSuccess: function (response, opt, params) {
    var modelData = params.modelData;
    modelData.id = response.resources[0].ConfigGroup.id;
    App.store.safeLoad(App.ServiceConfigGroup, modelData);
    App.store.fastCommit();
    App.ServiceConfigGroup.deleteTemporaryRecords();
  },

  /**
   *
   * @param {object} xhr
   * @param {string} text
   * @param {Error} errorThrown
   */
  postNewConfigurationGroupError: Em.K,

  /**
   * PUTs the new configuration-group on the server.
   * Changes possible here are the name, description and
   * host memberships of the configuration-group.
   *
   * @param {App.ConfigGroup} configGroup Configuration group to update
   * @param {Function} successCallback
   * @param {Function} errorCallback
   * @return {$.ajax}
   * @method updateConfigurationGroup
   */
  updateConfigurationGroup: function (configGroup, successCallback, errorCallback) {
    var sendData = {
      name: 'config_groups.update',
      data: {
        id: configGroup.get('id'),
        data: this.getConfigGroupData(configGroup)
      },
      success: 'successFunction',
      error: 'errorFunction',
      successFunction: function () {
        if (successCallback) {
          successCallback();
        }
      },
      errorFunction: function (xhr, text, errorThrown) {
        if (errorCallback) {
          errorCallback(xhr, text, errorThrown);
        }
      }
    };
    sendData.sender = sendData;
    return App.ajax.send(sendData);
  },

  /**
   *
   * @param {Em.Object} configGroup
   * @returns {{ConfigGroup: {group_name: *, description: *, tag: *, hosts: *, desired_configs: (Array|*)}}}
   */
  getConfigGroupData: function (configGroup) {
    var desiredConfigs = configGroup.get('desiredConfigs') || [];

    return {
      ConfigGroup: {
        group_name: configGroup.get('name'),
        description: configGroup.get('description'),
        tag: configGroup.get('service.id'),
        service_name: configGroup.get('service.id'),
        hosts: configGroup.get('hosts').map(function (h) {
          return {
            host_name: h
          };
        }),
        desired_configs: desiredConfigs.map(function (cst) {
          return {
            type: Em.get(cst, 'site') || Em.get(cst, 'type'),
            tag: Em.get(cst, 'tag')
          };
        })
      }
    };
  },

  /**
   * launch dialog where can be assigned another group to host
   * @param {App.ConfigGroup} selectedGroup
   * @param {App.ConfigGroup[]} configGroups
   * @param {String} hostName
   * @param {Function} callback
   * @return {App.ModalPopup}
   * @method launchSwitchConfigGroupOfHostDialog
   */
  launchSwitchConfigGroupOfHostDialog: function (selectedGroup, configGroups, hostName, callback) {
    var self = this;
    return App.ModalPopup.show({
      classNames: ['change-config-group-modal'],
      header: Em.I18n.t('config.group.host.switch.dialog.title'),
      configGroups: configGroups,
      selectedConfigGroup: selectedGroup,
      disablePrimary: function () {
        return this.get('selectedConfigGroup.name') === selectedGroup.get('name');
      }.property('selectedConfigGroup.name'),
      onPrimary: function () {
        var newGroup = this.get('selectedConfigGroup');
        if (selectedGroup.get('isDefault')) {
          selectedGroup.set('hosts.length', selectedGroup.get('hosts.length') - 1);
        } else {
          selectedGroup.get('hosts').removeObject(hostName);
        }
        if (!selectedGroup.get('isDefault')) {
          self.updateConfigurationGroup(selectedGroup, Em.K, Em.K);
        }

        if (newGroup.get('isDefault')) {
          newGroup.set('hosts.length', newGroup.get('hosts.length') + 1);
        } else {
          newGroup.get('hosts').pushObject(hostName);
        }
        callback(newGroup);
        if (!newGroup.get('isDefault')) {
          self.updateConfigurationGroup(newGroup, Em.K, Em.K);
        }
        this.hide();
      },
      bodyClass: Em.View.extend({
        templateName: require('templates/utils/config_launch_switch_config_group_of_host')
      })
    });
  },

  /**
   * Do request to delete config group
   * @param {App.ConfigGroup} configGroup
   * @param {Function} [successCallback]
   * @param {Function} [errorCallback]
   * @return {$.ajax}
   * @method deleteConfigurationGroup
   */
  deleteConfigurationGroup: function (configGroup, successCallback, errorCallback) {
    var self = this;
    var sendData = {
      name: 'common.delete.config_group',
      sender: this,
      data: {
        id: configGroup.get('id')
      },
      success: 'successFunction',
      error: 'errorFunction',
      successFunction: function (data, xhr, params) {
        self.deleteConfigurationGroupSuccess(data, xhr, params);
        if (successCallback) {
          successCallback();
        }
      },
      errorFunction: function (xhr, text, errorThrown) {
        if (errorCallback) {
          errorCallback(xhr, text, errorThrown);
        }
      }
    };
    sendData.sender = sendData;
    return App.ajax.send(sendData);
  },


  /**
   *
   * @param {object} data
   * @param {object} xhr
   * @param {object} params
   */
  deleteConfigurationGroupSuccess: function (data, xhr, params) {
    var groupFromModel = App.ServiceConfigGroup.find().findProperty('id', params.id);
    if (groupFromModel) {
      if (groupFromModel.get('stateManager.currentState.name') !== 'saved') {
        groupFromModel.get('stateManager').transitionTo('loaded');
      }
      App.configGroupsMapper.deleteRecord(groupFromModel);
    }
  },

  /**
   * Launches a dialog where an existing config-group can be selected, or a new
   * one can be created. This is different than the config-group management
   * dialog where host membership can be managed.
   *
   * The callback will be passed the created/selected config-group in the form
   * of {id:2, name:'New hardware group'}. In the case of dialog being cancelled,
   * the callback is provided <code>null</code>
   *
   * @param {String} groupName
   *  is closed, cancelled or OK is pressed.
   * @return {App.ModalPopup}
   * @method saveGroupConfirmationPopup
   */
  saveGroupConfirmationPopup: function (groupName) {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('config.group.save.confirmation.header'),
      secondary: Em.I18n.t('config.group.save.confirmation.manage.button'),
      groupName: groupName,
      bodyClass: Em.View.extend({
        templateName: require('templates/common/configs/saveConfigGroup')
      }),
      onPrimary: function () {
        if (self.get('controller.name') === 'mainServiceInfoConfigsController') {
          self.get('controller').loadConfigGroups([self.get('controller.content.serviceName')]).done(function () {
            var group = App.ServiceConfigGroup.find().find(function (g) {
              return g.get('serviceName') === self.get('controller.content.serviceName') && g.get('name') === groupName;
            });
            self.get('controller').doSelectConfigGroup({context: group});
          });
        }
        this._super();
      },
      onSecondary: function () {
        App.router.get('manageConfigGroupsController').manageConfigurationGroups(null, self.get('controller.content'));
        this.hide();
      }
    });
  },

  /**
   * Persist config groups created in step7 wizard controller
   * @method persistConfigGroups
   */
  persistConfigGroups: function () {
    var installerController = App.router.get('installerController');
    var step7Controller = App.router.get('wizardStep7Controller');
    installerController.saveServiceConfigGroups(step7Controller, step7Controller.get('content.controllerName') === 'addServiceController');
    App.clusterStatus.setClusterStatus({
      localdb: App.db.data
    });
  }

});
