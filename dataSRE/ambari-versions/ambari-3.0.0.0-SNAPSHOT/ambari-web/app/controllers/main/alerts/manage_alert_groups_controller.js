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

function stringify(obj, property) {
  return JSON.stringify(obj.get(property).slice().sort());
}

function groupsAreNotEqual(group1, group2) {
  return stringify(group1, 'definitions') !== stringify(group2, 'definitions') ||
  stringify(group1, 'notifications') !== stringify(group2, 'notifications');
}

function mapToEmObjects(collection, fields, renamedFields) {
  var _renamedFields = arguments.length === 3 ? renamedFields : [];
  return collection.map(function (item) {
    var ret = Em.Object.create(Em.getProperties(item, fields));
    _renamedFields.forEach(function (renamedField) {
      var [realName, newName] = renamedField.split(':');
      Em.set(ret, newName, Em.get(item, realName));
    });
    return ret;
  });
}

var AlertGroupClone = Em.Object.extend({
  displayName: function () {
    var name = App.config.truncateGroupName(this.get('name'));
    return this.get('default') ? name + ' Default' : name;
  }.property('name', 'default'),
  label: Em.computed.format('{0} ({1})', 'displayName', 'definitions.length')
});

App.ManageAlertGroupsController = Em.Controller.extend({

  name: 'manageAlertGroupsController',

  /**
   * @type {boolean}
   */
  isLoaded: false,

  /**
   * Property used to trigger Alert Groups Filter content updating
   * @type {Boolean}
   */
  changeTrigger: false,

  /**
   * @type {App.AlertGroup[]}
   */
  alertGroups: [],

  /**
   * @type {App.AlertGroup[]}
   */
  originalAlertGroups: [],

  /**
   * @type {App.AlertGroup}
   */
  selectedAlertGroup: null,

  /**
   * @type {App.AlertDefinition[]}
   */
  selectedDefinitions: [],

  /**
   * List of all Alert Notifications
   * @type {App.AlertNotification[]}
   */
  alertNotifications: function () {
    return this.get('isLoaded') ? mapToEmObjects(App.AlertNotification.find(), ['id', 'name', 'description', 'type', 'global']) : [];
  }.property('isLoaded'),

  /**
   * List of all global Alert Notifications
   * @type {App.AlertNotification[]}
   */
  alertGlobalNotifications: Em.computed.filterBy('alertNotifications', 'global', true),

  /**
   * @type {boolean}
   */
  isRemoveButtonDisabled: true,

  /**
   * @type {boolean}
   */
  isRenameButtonDisabled: true,

  /**
   * @type {boolean}
   */
  isDuplicateButtonDisabled: true,

  /**
   * @type {boolean}
   */
  isDeleteDefinitionsDisabled: function () {
    var selectedGroup = this.get('selectedAlertGroup');
    return selectedGroup ? selectedGroup.default || this.get('selectedDefinitions').length === 0 : true;
  }.property('selectedAlertGroup.definitions.length', 'selectedDefinitions.length'),

  /**
   * observes if any group changed including: group name, newly created group, deleted group, group with definitions/notifications changed
   * @type {{toDelete: App.AlertGroup[], toSet: App.AlertGroup[], toCreate: App.AlertGroup[]}}
   */
  defsModifiedAlertGroups: {},

  /**
   * Determines if some group was edited/created/deleted
   * @type {boolean}
   */
  isDefsModified: Em.computed.and('isLoaded', 'isDefsModifiedAlertGroups'),

  isDefsModifiedAlertGroups: Em.computed.or('defsModifiedAlertGroups.toSet.length', 'defsModifiedAlertGroups.toCreate.length', 'defsModifiedAlertGroups.toDelete.length'),

  /**
   * Check when some config group was changed and updates <code>defsModifiedAlertGroups</code> once
   * @method defsModifiedAlertGroupsObs
   */
  defsModifiedAlertGroupsObs: function() {
    Em.run.once(this, this.defsModifiedAlertGroupsObsOnce);
  }.observes('selectedAlertGroup.definitions.[]', 'selectedAlertGroup.notifications.[]', 'alertGroups', 'isLoaded'),

  /**
   * Update <code>defsModifiedAlertGroups</code>-value
   * Called once in the <code>defsModifiedAlertGroupsObs</code>
   * @method defsModifiedAlertGroupsObsOnce
   * @returns {boolean}
   */
  defsModifiedAlertGroupsObsOnce: function() {
    if (!this.get('isLoaded')) {
      return;
    }
    var groupsToDelete = [];
    var groupsToSet = [];
    var groupsToCreate = [];
    var groups = this.get('alertGroups'); //current alert groups
    var originalGroups = this.get('originalAlertGroups'); // original alert groups
    var mappedOriginalGroups = {}; // map is faster than `originalGroups.findProperty('id', ...)`
    originalGroups.forEach(function(group) {
      mappedOriginalGroups[group.get('id')] = group;
    });
    var originalGroupsIds = originalGroups.mapProperty('id');

    groups.forEach(function (group) {
      var originalGroup = mappedOriginalGroups[group.get('id')];
      if (originalGroup) {
        // should update definitions or notifications
        if (groupsAreNotEqual(group, originalGroup) || group.get('name') !== originalGroup.get('name')) {
          // should update name
          groupsToSet.push(group.set('id', originalGroup.get('id')));
        }
        originalGroupsIds = originalGroupsIds.without(group.get('id'));
      }
      else {
        // should add new group
        groupsToCreate.push(group);
      }
    });
    // should delete groups
    originalGroupsIds.forEach(function (id) {
      groupsToDelete.push(originalGroups.findProperty('id', id));
    });

    this.set('defsModifiedAlertGroups', {
      toDelete: groupsToDelete,
      toSet: groupsToSet,
      toCreate: groupsToCreate
    });
  },

  /**
   * Load all Alert Notifications from server
   * @returns {$.ajax}
   * @method loadAlertNotifications
   */
  loadAlertNotifications: function () {
    this.setProperties({
      isLoaded: false,
      alertGroups: [],
      originalAlertGroups: [],
      selectedAlertGroup: null,
      isRemoveButtonDisabled: true,
      isRenameButtonDisabled: true,
      isDuplicateButtonDisabled: true
    });
    return App.ajax.send({
      name: 'alerts.notifications',
      sender: this,
      success: 'getAlertNotificationsSuccessCallback',
      error: 'getAlertNotificationsErrorCallback'
    });
  },

  /**
   * Success-callback for load alert notifications request
   * @param {object} json
   * @method getAlertNotificationsSuccessCallback
   */
  getAlertNotificationsSuccessCallback: function (json) {
    App.alertNotificationMapper.map(json);
    this.loadAlertGroups();
  },

  /**
   * Error-callback for load alert notifications request
   * @method getAlertNotificationsErrorCallback
   */
  getAlertNotificationsErrorCallback: function () {
    this.set('isLoaded', true);
  },

  /**
   * Load all alert groups from alert group model
   * @method loadAlertGroups
   */
  loadAlertGroups: function () {
    var alertGroups = App.AlertGroup.find().map(function (group) {
      var definitions = mapToEmObjects(
        group.get('definitions'),
        ['name', 'serviceName', 'componentName', 'label', 'id'],
        ['service.displayName:serviceNameDisplay', 'componentNameFormatted:componentNameDisplay']
      );

      var targets = mapToEmObjects(group.get('targets'), ['name', 'id', 'description', 'type', 'global']);

      var hash = Em.getProperties(group, ['id', 'name', 'default', 'isAddDefinitionsDisabled']);
      hash.definitions = definitions;
      hash.notifications = targets;
      return AlertGroupClone.create(hash);
    });
    this.setProperties({
      alertGroups: alertGroups,
      isLoaded: true,
      originalAlertGroups: this.copyAlertGroups(alertGroups),
      selectedAlertGroup: alertGroups[0]
    });
  },

  /**
   * Enable/disable "Remove"/"Rename"/"Duplicate" buttons basing on <code>controller.selectedAlertGroup</code>
   * @method buttonObserver
   */
  buttonObserver: function () {
    var selectedAlertGroup = this.get('selectedAlertGroup');
    var flag = selectedAlertGroup && selectedAlertGroup.get('default');
    this.setProperties({
      isRemoveButtonDisabled: flag,
      isRenameButtonDisabled: flag,
      isDuplicateButtonDisabled: false
    });
  }.observes('selectedAlertGroup'),

  /**
   * @method resortAlertGroup
   */
  resortAlertGroup: function () {
    var alertGroups = Em.copy(this.get('alertGroups'));
    if (alertGroups.length < 2) {
      return;
    }
    var defaultGroups = alertGroups.filterProperty('default');
    defaultGroups.forEach(function (defaultGroup) {
      alertGroups.removeObject(defaultGroup);
    });
    var sorted = defaultGroups.sortProperty('name').concat(alertGroups.sortProperty('name'));

    this.removeObserver('alertGroups.@each.name', this, 'resortAlertGroup');
    this.set('alertGroups', sorted);
    this.addObserver('alertGroups.@each.name', this, 'resortAlertGroup');
  }.observes('alertGroups.@each.name'),

  /**
   * remove definitions from group
   * @method deleteDefinitions
   */
  deleteDefinitions: function () {
    if (this.get('isDeleteDefinitionsDisabled')) {
      return;
    }
    var groupDefinitions = this.get('selectedAlertGroup.definitions');
    this.get('selectedDefinitions').slice().forEach(function (defObj) {
      groupDefinitions.removeObject(defObj);
    }, this);
    this.set('selectedDefinitions', []);
  },

  /**
   * Provides alert definitions which are available for inclusion in
   * non-default alert groups.
   * @param {App.AlertGroup} selectedAlertGroup
   * @method getAvailableDefinitions
   * @return {{name: string, serviceName: string, componentName: string, serviceNameDisplay: string, componentNameDisplay: string, label: string, id: number}[]}
   */
  getAvailableDefinitions: function (selectedAlertGroup) {
    if (selectedAlertGroup.get('default')) return [];
    var usedDefinitionsMap = {};
    var availableDefinitions = [];
    var sharedDefinitions = App.AlertDefinition.find();

    usedDefinitionsMap = selectedAlertGroup.get('definitions').toWickMapByProperty('name');

    selectedAlertGroup.get('definitions').forEach(function (def) {
      usedDefinitionsMap[def.name] = true;
    });
    sharedDefinitions.forEach(function (sharedDef) {
      if (!usedDefinitionsMap[sharedDef.get('name')]) {
        availableDefinitions.pushObject(sharedDef);
      }
    });
    return mapToEmObjects(
      availableDefinitions,
      ['name', 'serviceName', 'componentName', 'label', 'id'],
      ['service.displayName:serviceNameDisplay', 'componentNameFormatted:componentNameDisplay']
    );
  },

  /**
   * add alert definitions to a group
   * @method addDefinitions
   */
  addDefinitions: function () {
    if (this.get('selectedAlertGroup.isAddDefinitionsDisabled')) {
      return false;
    }
    var availableDefinitions = this.getAvailableDefinitions(this.get('selectedAlertGroup'));
    var validComponents = App.StackServiceComponent.find().map(function (component) {
      return Em.Object.create({
        componentName: component.get('componentName'),
        displayName: App.format.role(component.get('componentName'), false),
        selected: false
      });
    });
    var validServices = App.Service.find().map(function (service) {
      return Em.Object.create({
        serviceName: service.get('serviceName'),
        displayName: App.format.role(service.get('serviceName'), true),
        selected: false
      });
    });
    return this.launchDefsSelectionDialog(availableDefinitions, [], validServices, validComponents);
  },

  /**
   * Launch a table view of all available definitions to choose
   * @method launchDefsSelectionDialog
   * @return {App.ModalPopup}
   */
  launchDefsSelectionDialog: function (initialDefs, selectedDefs, validServices, validComponents) {
    var self = this;
    return App.ModalPopup.show({

      classNames: ['common-modal-wrapper', 'full-height-modal'],

      modalDialogClasses: ['modal-lg'],

      header: Em.I18n.t('alerts.actions.manage_alert_groups_popup.selectDefsDialog.title'),

      /**
       * @type {string}
       */
      dialogMessage: Em.I18n.t('alerts.actions.manage_alert_groups_popup.selectDefsDialog.message').format(this.get('selectedAlertGroup.displayName')),

      /**
       * @type {string|null}
       */
      warningMessage: null,

      /**
       * @type {App.AlertDefinition[]}
       */
      availableDefs: [],

      onPrimary: function () {
        this.set('warningMessage', null);
        var arrayOfSelectedDefs = this.get('availableDefs').filterProperty('selected', true);
        if (arrayOfSelectedDefs.length < 1) {
          this.set('warningMessage', Em.I18n.t('alerts.actions.manage_alert_groups_popup.selectDefsDialog.message.warning'));
          return;
        }
        self.addDefinitionsCallback(arrayOfSelectedDefs);
        this.hide();
      },

      /**
       * Primary button should be disabled while alert definitions are not loaded
       * @type {boolean}
       */
      disablePrimary: Em.computed.not('isLoaded'),

      onSecondary: function () {
        self.addDefinitionsCallback(null);
        this.hide();
      },

      bodyClass: App.SelectDefinitionsPopupBodyView.extend({

        filterComponents: validComponents,

        filterServices: validServices,

        initialDefs: initialDefs

      })

    });
  },

  /**
   * add alert definitions callback
   * @method addDefinitionsCallback
   */
  addDefinitionsCallback: function (selectedDefs) {
    var group = this.get('selectedAlertGroup');
    if (selectedDefs) {
      group.get('definitions').pushObjects(selectedDefs);
    }
  },

  /**
   * copy alert groups for backup, to compare with current alert groups, so will know if some groups changed/added/deleted
   * @param {App.AlertGroup[]} originGroups
   * @return {App.AlertGroup[]}
   * @method copyAlertGroups
   */
  copyAlertGroups: function (originGroups) {
    var alertGroups = [];
    originGroups.forEach(function (alertGroup) {
      var copiedGroup = Em.Object.create($.extend(true, {}, alertGroup));
      alertGroups.pushObject(copiedGroup);
    });
    return alertGroups;
  },

  /**
   * Create a new alert group
   * @param {Em.Object} newAlertGroupData
   * @param {callback} callback Callback function for Success or Error handling
   * @return {App.AlertGroup} Returns the created alert group
   * @method postNewAlertGroup
   */
  postNewAlertGroup: function (newAlertGroupData, callback) {
    // create a new group with name , definition and notifications
    var data = {
      name: newAlertGroupData.get('name')
    };
    if (newAlertGroupData.get('definitions').length > 0) {
      data.definitions = newAlertGroupData.get('definitions').mapProperty('id');
    }
    if (newAlertGroupData.get('notifications').length > 0) {
      data.targets = newAlertGroupData.get('notifications').mapProperty('id');
    }
    var sendData = {
      name: 'alert_groups.create',
      data: data,
      success: 'successFunction',
      error: 'errorFunction',
      successFunction: function () {
        if (callback) {
          callback();
        }
      },
      errorFunction: function (xhr, text, errorThrown) {
        if (callback) {
          callback(xhr, text, errorThrown);
        }
      }
    };
    sendData.sender = sendData;
    App.ajax.send(sendData);
    return newAlertGroupData;
  },

  /**
   * PUTs the new alert group information on the server.
   * Changes possible here are the name, definitions, notifications
   *
   * @param {App.AlertGroup} alertGroup
   * @param {Function} successCallback
   * @param {Function} errorCallback
   * @method updateAlertGroup
   */
  updateAlertGroup: function (alertGroup, successCallback, errorCallback) {
    var sendData = {
      name: 'alert_groups.update',
      data: {
        group_id: alertGroup.id,
        name: alertGroup.get('name'),
        definitions: alertGroup.get('definitions').mapProperty('id'),
        targets: alertGroup.get('notifications').mapProperty('id')
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
    App.ajax.send(sendData);
  },

  /**
   * Request for deleting alert group
   * @param {App.AlertGroup} alertGroup
   * @param {callback} successCallback
   * @param {callback} errorCallback
   * @method removeAlertGroup
   */
  removeAlertGroup: function (alertGroup, successCallback, errorCallback) {
    var sendData = {
      name: 'alert_groups.delete',
      data: {
        group_id: alertGroup.id
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
    App.ajax.send(sendData);
  },

  /**
   * confirm delete alert group
   * @method confirmDelete
   */
  confirmDelete: function () {
    if (this.get('isRemoveButtonDisabled')) return;
    var self = this;
    App.showConfirmationPopup(function () {
      self.deleteAlertGroup();
    });
  },

  /**
   * delete selected alert group
   * @method deleteAlertGroup
   */
  deleteAlertGroup: function () {
    var selectedAlertGroup = this.get('selectedAlertGroup');
    if (this.get('isDeleteAlertDisabled')) {
      return;
    }
    this.get('alertGroups').removeObject(selectedAlertGroup);
    this.set('selectedAlertGroup', this.get('alertGroups')[0]);
  },

  /**
   * Rename non-default alert group
   * @method renameAlertGroup
   */
  renameAlertGroup: function () {

    if (this.get('selectedAlertGroup.default')) {
      return;
    }
    var self = this;
    var popup = App.ModalPopup.show({

      header: Em.I18n.t('alerts.actions.manage_alert_groups_popup.renameButton'),

      bodyClass: Ember.View.extend({
        templateName: require('templates/main/alerts/create_new_alert_group')
      }),

      /**
       * @type {string}
       */
      alertGroupName: self.get('selectedAlertGroup.name'),

      alertGroupNameIsEmpty: function () {
        return !this.get('alertGroupName').trim().length;
      }.property('alertGroupName'),

      /**
       * @type {string|null}
       */
      warningMessage: function () {
        var originalGroup = self.get('selectedAlertGroup');
        var groupName = this.get('alertGroupName').trim();

        if (originalGroup.get('name').trim() === groupName) {
          return Em.I18n.t("alerts.actions.manage_alert_groups_popup.addGroup.exist");
        }
        if (self.get('alertGroups').mapProperty('displayName').contains(groupName)) {
          return Em.I18n.t("alerts.actions.manage_alert_groups_popup.addGroup.exist");
        }
        if (groupName && !validator.isValidAlertGroupName(groupName)) {
          return Em.I18n.t("form.validator.alertGroupName");
        }
        return '';
      }.property('alertGroupName'),

      /**
       * Primary button is disabled while user doesn't input valid group name
       * @type {boolean}
       */
      disablePrimary: Em.computed.or('alertGroupNameIsEmpty', 'warningMessage'),

      onPrimary: function () {
        self.set('selectedAlertGroup.name', this.get('alertGroupName'));
        this._super();
      }

    });
    this.set('renameGroupPopup', popup);
  },

  /**
   * Create new alert group
   * @param {boolean} duplicated is new group a copy of the existing group
   * @method addAlertGroup
   */
  addAlertGroup: function (duplicated) {
    duplicated = duplicated === true;
    var self = this;
    var popup = App.ModalPopup.show({

      header: Em.I18n.t('alerts.actions.manage_alert_groups_popup.addButton'),

      bodyClass: Em.View.extend({
        templateName: require('templates/main/alerts/create_new_alert_group')
      }),

      /**
       * Name for new alert group
       * @type {string}
       */
      alertGroupName: duplicated ? self.get('selectedAlertGroup.name') + ' Copy' : "",

      alertGroupNameIsEmpty: function () {
        return !this.get('alertGroupName').trim().length;
      }.property('alertGroupName'),

      /**
       * @type {string}
       */
      warningMessage: function () {
        var groupName = this.get('alertGroupName').trim();
        if (self.get('alertGroups').mapProperty('displayName').contains(groupName)) {
          return Em.I18n.t("alerts.actions.manage_alert_groups_popup.addGroup.exist");
        }
        if (groupName && !validator.isValidAlertGroupName(groupName)) {
          return Em.I18n.t("form.validator.alertGroupName");
        }
        return '';
      }.property('alertGroupName'),

      /**
       * Primary button is disabled while user doesn't input valid group name
       * @type {boolean}
       */
      disablePrimary: Em.computed.or('alertGroupNameIsEmpty', 'warningMessage'),

      onPrimary: function () {
        var newAlertGroup = AlertGroupClone.create({
          name: this.get('alertGroupName').trim(),
          default: false,
          definitions: duplicated ? self.get('selectedAlertGroup.definitions').slice(0) : [],
          notifications: self.get('alertGlobalNotifications'),
          isAddDefinitionsDisabled: false
        });
        self.get('alertGroups').pushObject(newAlertGroup);
        self.set('selectedAlertGroup', newAlertGroup);
        this.hide();
      }

    });
    this.set('addGroupPopup', popup);
  },

  /**
   * @method duplicateAlertGroup
   */
  duplicateAlertGroup: function () {
    this.addAlertGroup(true);
  }

});
