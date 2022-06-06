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

var stringUtils = require('utils/string_utils');
var numberUtils = require('utils/number_utils');
var blueprintUtils = require('utils/blueprint');

App.AssignMasterOnStep7Controller = Em.Controller.extend(App.BlueprintMixin, App.AssignMasterComponents, {

  name: "assignMasterOnStep7Controller",

  useServerValidation: false,

  showInstalledMastersFirst: false,

  configWidgetContext: {},

  configActionComponent: {},

  content: function () {
    return this.get('configWidgetContext.controller.content') || {};
  }.property('configWidgetContext.controller.content'),

  popup: null,

  mastersToCreate: [],

  markSavedComponentsAsInstalled: true,

  /**
   * Array of master component names, that should be addable
   * Are used in HA wizards to add components, that are not addable for other wizards
   * @type {Array}
   * @override
   */
  mastersAddableInHA: Em.computed.alias('App.components.isMasterAddableOnlyOnHA'),

  /**
   * Marks component add/delete action to be performed ahead.
   * @param context {Object} Context of the calling function
   * @param action {String} ADD|DELETE
   * @param hostComponent {Object}
   * @public
   * @method {execute}
   */
  execute: function (context, action, hostComponent) {
    this.set('configWidgetContext', context);
    this.set('content', context.get('controller.content'));
    this.set('configActionComponent', hostComponent);
    
    switch (action) {
      case 'ADD':
        this.clearRecommendations();
        if (hostComponent.componentName === "HIVE_SERVER_INTERACTIVE") {
          this.getPendingBatchRequests(hostComponent);  
        } else {
          this.showPopup(hostComponent);
        }
        break;
      case 'DELETE':
        this.set('mastersToCreate', [hostComponent.componentName]);
        this.removeMasterComponent();
        break;
    }
  },

  getPendingBatchRequests: function(hostComponent) {
    var self = this;
    // Send Ajax request to get status of pending batch requests
    App.ajax.send({
      name : 'request_schedule.get.pending',
      sender: self,
      error : 'pendingBatchRequestsAjaxError',
      success: 'pendingBatchRequestsAjaxSuccess',
      data: {
        hostComponent: hostComponent
      }
    });
  },

  pendingBatchRequestsAjaxError: function(data) {
    var error = Em.I18n.t('services.service.actions.run.yarnRefreshQueues.error');
    if (data && data.responseText) {
      try {
        var json = JSON.parse(data.responseText);
        error += json.message;
      } catch (err) {}
    }
    App.showAlertPopup(Em.I18n.t('services.service.actions.run.yarnRefreshQueues.error'), error, null);
  },

  pendingBatchRequestsAjaxSuccess : function(data, opt, params) {
    var self = this;
    if (this.shouldShowAlertOnBatchRequest(data)) {
      App.showAlertPopup(Em.I18n.t('services.service.actions.hsi.alertPopup.header'),
        Em.I18n.t('services.service.actions.hsi.alertPopup.body'), function() {
        var configWidgetContext = self.get('configWidgetContext');
        var config = configWidgetContext.get('config');
        configWidgetContext.toggleProperty('controller.forceUpdateBoundaries');
        var value = config.get('initialValue');
        config.set('value', value);
        configWidgetContext.setValue(value);
        configWidgetContext.sendRequestRorDependentConfigs(config);
        this.hide();
      });
    } else {
      this.showPopup(params.hostComponent);
    }
  },

  shouldShowAlertOnBatchRequest: function(data) {
    var showAlert = false;
    if (data.hasOwnProperty('items') && data.items.length > 0) {
      data.items.forEach( function(_item) {
        if (_item && _item.RequestSchedule && _item.RequestSchedule.batch && _item.RequestSchedule.batch.batch_requests) {
          _item.RequestSchedule.batch.batch_requests.forEach(function (batchRequest) {
            // Check if a DELETE request on HIVE_SERVER_INTERACTIVE is in progress
            if (batchRequest.request_type === "DELETE" && batchRequest.request_uri.indexOf("HIVE_SERVER_INTERACTIVE") > -1) {
              showAlert = true;
            }
          });
        }
      });
    }
    return showAlert;
  },

  showPopup: function(hostComponent) {
    var missingDependentServices = this.getAllMissingDependentServices();
    var isNonWizardPage = !this.get('content.controllerName');
    if (missingDependentServices.length && isNonWizardPage) {
      this.showInstallServicesPopup(missingDependentServices);
    } else {
      this.set('mastersToCreate', [hostComponent.componentName]);
      this.showAssignComponentPopup();
    }
  },

  /**
   * Used to set showAddControl/showRemoveControl flag
   * @param componentName
   * @override
   */
  updateComponent: function(componentName) {
    this._super(componentName);

    if (!this.get('mastersToCreate').contains(componentName)) {
      this.get("selectedServicesMasters").filterProperty("component_name", componentName).forEach(function(c) {
        c.set('showAddControl', false);
        c.set('showRemoveControl', false);
      });
    }
  },

  /**
   * Assign Master page will be displayed in the popup
   * @private
   * @method
   */
  showAssignComponentPopup: function () {
    var self = this;
    // Master component hosts should be loaded only when content.controller name is not defined i.e non-wizard pages
    if (!this.get('content.controllerName')) {
      this.loadMasterComponentHosts();
    }
    var mastersToCreate = this.get('mastersToCreate');
    var masterToCreateDisplayName = App.format.role(mastersToCreate[0]);
    var configWidgetContext = this.get('configWidgetContext');
    var config = this.get('configWidgetContext.config');
    var popup = App.ModalPopup.show({
      classNames: ['wizard-modal-wrapper', 'add-service-wizard-modal'],
      modalDialogClasses: ['modal-xlg'],
      header: Em.I18n.t('assign.master.popup.header').format(masterToCreateDisplayName),
      bodyClass: App.AssignMasterOnStep7View.extend({
        controller: self
      }),
      primary: Em.I18n.t('common.select'),
      onPrimary: function () {
        self.submit();
      },
      onSecondary: function() {
        this.showWarningPopup();
      },
      onClose: function () {
        this.showWarningPopup();
      },
      showWarningPopup: function() {
        var mainPopupContext = this;
        App.ModalPopup.show({
          encodeBody: false,
          header: Em.I18n.t('common.warning'),
          primaryClass: 'btn-warning',
          body: Em.I18n.t('assign.master.popup.cancel.body').format(masterToCreateDisplayName),
          onPrimary: function () {
            configWidgetContext.toggleProperty('controller.forceUpdateBoundaries');
            var value = config.get('initialValue');
            config.set('value', value);
            configWidgetContext.setValue(value);
            configWidgetContext.sendRequestRorDependentConfigs(config);
            this.hide();
            mainPopupContext.hide();
          }
        });
      },
      didInsertElement: function () {
        this._super();
        this.fitHeight();
        self.set('configWidgetContext.controller.saveInProgress', false);
      }
    });
    this.set('popup', popup);
  },

  /**
   * Displays the popup to install required service dependencies for being added component with this config change
   * @param missingDependentServices {String[]}   Array of service display names
   */
  showInstallServicesPopup: function (missingDependentServices) {
    var displayServices = stringUtils.getFormattedStringFromArray(missingDependentServices);
    var configWidgetContext = this.get('configWidgetContext');
    var config = this.get('configWidgetContext.config');
    var configDisplayName = config.get('displayName').toLowerCase();
    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step7.missing.service.header'),
      body: Em.I18n.t('installer.step7.missing.service.body').format(displayServices, configDisplayName),
      primaryClass: 'btn-danger',
      onPrimary: function () {
        configWidgetContext.toggleProperty('controller.forceUpdateBoundaries');
        var value = config.get('initialValue');
        config.set('value', value);
        configWidgetContext.setValue(value);
        configWidgetContext.sendRequestRorDependentConfigs(config);
        this._super();
      },
      secondary: null,
      showCloseButton: false,
      didInsertElement: function () {
        this._super();
        configWidgetContext.set('controller.saveInProgress', false);
      }
    });
  },

  /**
   * This method is used while installing or adding a service
   * Removes the masterComponent that was previously being tracked to be added to the cluster
   * @private
   * @method {removeMasterComponent}
   */
  removeMasterComponent: function () {
    var componentsToDelete = this.get('mastersToCreate');
    var componentsFromConfigs = this.get('content.componentsFromConfigs');
    if (this.get('content.controllerName')) {
      var masterComponentHosts = this.get('content.masterComponentHosts');
      var recommendationsHostGroups = this.get('content.recommendationsHostGroups');
      componentsToDelete.forEach(function (_componentName) {
        masterComponentHosts = masterComponentHosts.rejectProperty('component', _componentName);
        recommendationsHostGroups.blueprint.host_groups.forEach(function(hostGroup){
          hostGroup.components = hostGroup.components.rejectProperty('name', _componentName);
        }, this);
        componentsFromConfigs = componentsFromConfigs.without(_componentName);
      }, this);
      this.get('content').set('masterComponentHosts', masterComponentHosts);
      this.set('content.componentsFromConfigs', componentsFromConfigs);
      this.set('content.recommendationsHostGroups', recommendationsHostGroups);
    } else {
      this.clearComponentsToBeAdded(componentsToDelete[0]);
      var hostComponent = App.HostComponent.find().findProperty('componentName', componentsToDelete[0]);
      if (hostComponent) {
        App.set('componentToBeDeleted', Em.Object.create({
          componentName: componentsToDelete[0],
          hostName: hostComponent.get('hostName')
        }));
      }
    }
    var configActionComponent = this.get('configActionComponent');
    this.get('configWidgetContext.config').set('configActionComponent', configActionComponent);
  },

  /**
   * Success callback after loading active host list
   * @override
   * @method loadWizardHostsSuccessCallback
   */
   loadWizardHostsSuccessCallback: function (data) {
    var parentController = this.get('content.controllerName');
    if (parentController) {
      this._super(data);
    } else {
      var result = [];
      data.items.forEach(function (host) {
        var hostName = host.Hosts.host_name,
          cpu = host.Hosts.cpu_count,
          memory = host.Hosts.total_mem.toFixed(2);
        result.push(Em.Object.create({
          host_name: hostName,
          cpu: cpu,
          memory: memory,
          disk_info: host.Hosts.disk_info,
          maintenance_state: host.Hosts.maintenance_state,
          host_info: Em.I18n.t('installer.step5.hostInfo').fmt(hostName, numberUtils.bytesToSize(memory, 1, 'parseFloat', 1024), cpu)
        }));
      }, this);
      this.set('hosts', result);
      this.sortHosts(this.get('hosts'));
      this.set('isHostsLoaded', true);
    }
  },

  /**
   *  This method is called on Service->config page and is responsible to load the "Assign Master popup"
   *  with the installed master component hosts.
   * @private
   * @method {loadMasterComponentHosts}
   */
  loadMasterComponentHosts: function () {
    var stackMasterComponents = App.get('components.masters').uniq();
    var masterComponentHosts = [];
    App.HostComponent.find().filter(function (component) {
      return stackMasterComponents.contains(component.get('componentName'));
    }).forEach(function (item) {
      masterComponentHosts.push({
        component: item.get('componentName'),
        hostName: item.get('hostName'),
        isInstalled: true,
        serviceId: item.get('service.id'),
        display_name: item.get('displayName')
      })
    });
    this.set("masterComponentHosts", masterComponentHosts);
  },

  /**
   * Returns array of dependent services that are yet not installed in the cluster
   * @private
   * @method getAllMissingDependentServices
   * @return  missingDependentServices {Array}
   */
  getAllMissingDependentServices: function () {
    var configActionComponentName = this.get('configActionComponent').componentName;
    var componentStackService = App.StackServiceComponent.find(configActionComponentName).get('stackService');
    var missing = [];
    componentStackService.collectMissingDependencies(this.installedStackServices(), App.StackService.find(), missing);
    return missing.mapProperty('displayName');
  },

  installedStackServices: function() {
    return App.Service.find().map(function(each) {
      return App.StackService.find(each.get('serviceName'));
    });
  },

  /**
   * This method saves masterComponent layout that is used on subsequent "Review" and "Install start and Test services" pages.
   * @private
   * @method {saveMasterComponentHosts}
   */
  saveMasterComponentHosts: function() {
    var controller = App.router.get(this.get('content.controllerName'));
    var componentsFromConfigs = this.get('content.componentsFromConfigs');
    controller.saveMasterComponentHosts(this, true);
    controller.loadMasterComponentHosts(true);
    componentsFromConfigs = componentsFromConfigs.concat(this.get('mastersToCreate'));
    this.set('content.componentsFromConfigs', componentsFromConfigs);
  },

  /**
   * This method saves host group layout that is used for blueprint validation call made while transitioning to "Review" page.
   * @private
   * @method {saveRecommendationsHostGroups}
   */
  saveRecommendationsHostGroups: function() {
    var recommendationsHostGroups = this.get('content.recommendationsHostGroups');
    var mastersToCreate = this.get('mastersToCreate');
    mastersToCreate.forEach(function(componentName) {
      var hostName = this.getSelectedHostNames(componentName)[0];
      if (hostName && recommendationsHostGroups) {
        var hostGroups = recommendationsHostGroups.blueprint_cluster_binding.host_groups;
        var isHostPresent = false;
        var i = 0;
        while (i < hostGroups.length) {
          var hosts = hostGroups[i].hosts;
          isHostPresent = hosts.someProperty('fqdn', hostName);
          if (isHostPresent) break;
          i++;
        }
        if (isHostPresent) {
          var hostGroupName = hostGroups[i].name;
          var hostGroup = recommendationsHostGroups.blueprint.host_groups.findProperty('name', hostGroupName);
          var addHostComponentInGroup = !hostGroup.components.someProperty('name', componentName);
          if (addHostComponentInGroup) {
            hostGroup.components.pushObject({name: componentName});
          }
        }
      }
    }, this);
    this.set('content.recommendationsHostGroups', recommendationsHostGroups);
  },

  /**
   * Get the fqdn hostnames as selected by the user for the component.
   * @param componentName
   * @return {String[]}
   */
  getSelectedHostNames: function(componentName) {
    var selectedServicesMasters = this.get('selectedServicesMasters');
    return selectedServicesMasters.filterProperty('component_name', componentName).mapProperty('selectedHost');
  },

  /**
   * set App.componentToBeAdded to use it on subsequent validation call while saving configuration
   * @param componentName {String}
   * @param hostNames {String[]}
   * @method {setGlobalComponentToBeAdded}
   */
  setGlobalComponentToBeAdded: function(componentName, hostNames) {
    var componentToBeAdded = Em.Object.create({
       componentName: componentName,
       hostNames: hostNames
    });
    App.set('componentToBeAdded', componentToBeAdded);
  },

  /**
   * clear 'componentToBeDeleted' object
   * @param componentName {String}
   * @public
   * @method {clearComponentsToBeDeleted}
   */
  clearComponentsToBeDeleted: function(componentName) {
    var componentsToBeDeleted = App.get('componentToBeDeleted');
    if (!App.isEmptyObject(componentsToBeDeleted) && componentsToBeDeleted.get('componentName') === componentName) {
      App.set('componentToBeDeleted', {});
    }
  },

  /**
   * clear 'componentToBeAdded' object
   * @param componentName  {String}
   */
  clearComponentsToBeAdded: function(componentName) {
    var componentsToBeAdded = App.get('componentToBeAdded');
    if (!App.isEmptyObject(componentsToBeAdded) && componentsToBeAdded.get('componentName') === componentName) {
      App.set('componentToBeAdded', {});
    }
  },

  /**
   * Submit button click handler
   * @method submit
   */
  submit: function () {
    var self = this;
    App.get('router.mainAdminKerberosController').getKDCSessionState(function() {
      self.get('popup').hide();
      var context = self.get('configWidgetContext');
      context.toggleProperty('controller.forceUpdateBoundaries');
      var configActionComponent = self.get('configActionComponent');
      var componentHostNames = self.getSelectedHostNames(configActionComponent.componentName);
      var config = self.get('configWidgetContext.config');

      // TODO remove after stack advisor is able to handle this case
      // workaround for hadoop.proxyuser.{{hiveUser}}.hosts after adding Hive Server Interactive from Install Wizard
      var serviceConfigs = context.get('controller.stepConfigs').findProperty('serviceName', context.get('controller.selectedService.serviceName')).get('configs');
      var dependencies = context.get('config.configAction.dependencies');

      if (self.get('content.controllerName')) {
        self.saveMasterComponentHosts();
        self.saveRecommendationsHostGroups();

        // TODO remove after stack advisor is able to handle this case
        // workaround for hadoop.proxyuser.{{hiveUser}}.hosts after adding Hive Server Interactive from Install Wizard
        var miscConfigs = context.get('controller.stepConfigs').findProperty('serviceName', 'MISC').get('configs');
        serviceConfigs = serviceConfigs.concat(miscConfigs);
      } else {
        self.setGlobalComponentToBeAdded(configActionComponent.componentName, componentHostNames);
        self.clearComponentsToBeDeleted(configActionComponent.componentName);
      }

      configActionComponent.hostNames = componentHostNames;
      config.set('configActionComponent', configActionComponent);

       var oldValueKey = context.get('controller.wizardController.name') === 'installerController' ? 'initialValue' : 'savedValue';
       context.get('controller').loadConfigRecommendations([{
        type: App.config.getConfigTagFromFileName(config.get('fileName')),
        name: config.get('name'),
        old_value: config.get(oldValueKey)
      }]);
    });
  },

  /**
   *
   * @param {Em.Object} context
   * @param {object} blueprintObject
   */
  saveRecommendations: function(context, blueprintObject) {
    var oldValueKey = context.get('controller.wizardController.name') === 'installerController' ? 'initialValue' : 'savedValue';
    var config = this.get('configWidgetContext.config');

    context.get('controller').loadRecommendationsSuccess({
      resources: [
        {
          recommendations: {
            blueprint: {
              configurations: blueprintObject
            }
          }
        }
      ]
    }, null, {
      dataToSend: {
        changed_configurations: [{
          type: App.config.getConfigTagFromFileName(config.get('fileName')),
          name: config.get('name'),
          old_value: config.get(oldValueKey)
        }]
      }
    });
  },

  /**
   *
   * @param dependencies
   * @param serviceConfigs
   * @returns {{}}
   */
  getDependenciesForeignKeys: function(dependencies, serviceConfigs) {
    var foreignKeys = {};
    if (dependencies.foreignKeys) {
      dependencies.foreignKeys.forEach(function (dependency) {
        var matchingProperty = serviceConfigs.find(function (property) {
          return property.get('filename') === App.config.getOriginalFileName(dependency.fileName) && property.get('name') === dependency.propertyName;
        });
        if (matchingProperty) {
          foreignKeys[dependency.key] = matchingProperty.get('value');
        }
      });
    }
    return foreignKeys;
  },

  /**
   *
   * @param dependencies
   * @param context
   * @returns {Array}
   */
  getMasterComponents: function(dependencies, context) {
    var masterComponents = [];
    if (this.get('content.controllerName')) {
      var savedMasterComponents = context.get('controller.content.masterComponentHosts').filter(function (componentObject) {
        return dependencies.initializer.componentNames.contains(componentObject.component);
      });
      masterComponents = savedMasterComponents.map(function (componentObject) {
        var masterComponent = Em.getProperties(componentObject, ['component', 'hostName']);
        masterComponent.isInstalled = true;
        return masterComponent;
      });
    } else {
      var hostsMap = blueprintUtils.getComponentForHosts();
      Em.keys(hostsMap).forEach(function (hostName) {
        hostsMap[hostName].forEach(function (componentName) {
          if (dependencies.initializer.componentNames.contains(componentName)) {
            masterComponents.push({
              component: componentName,
              hostName: hostName,
              isInstalled: true
            });
          }
        });
      });
    }
    return masterComponents;
  },

  getHosts: function () {
    var result,
      parentController = this.get('content.controllerName');
    if (parentController) {
      result = this._super();
    } else {
      result = this.get('hosts').mapProperty('host_name');
    }
    return result;
  },

  clearStepOnExit: Em.K
});