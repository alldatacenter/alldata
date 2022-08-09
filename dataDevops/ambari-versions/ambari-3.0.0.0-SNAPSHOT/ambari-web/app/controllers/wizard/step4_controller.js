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

App.WizardStep4Controller = Em.ArrayController.extend({

  name: 'wizardStep4Controller',

  /**
   * List of Services
   * @type {Object[]}
   */
  content: [],

  /**
   * Check / Uncheck 'Select All' checkbox with one argument; Check / Uncheck all other checkboxes with more arguments
   * @type {bool}
   */
  isAllChecked: function(key, value) {
    if (arguments.length > 1) {
      this.filterProperty('isDisabled', false).filterProperty('isDFS', false).setEach('isSelected', value);
      return value;
    }
    return this.filterProperty('isInstalled', false).
      filterProperty('isHiddenOnSelectServicePage', false).
      filterProperty('isDFS', false).
      everyProperty('isSelected', true);
  }.property('@each.isSelected'),

  /**
   * Is Submit button disabled
   * @type {bool}
   */
  isSubmitDisabled: function () {
    return this.filterProperty('isSelected', true).filterProperty('isInstalled', false).length === 0 || App.get('router.btnClickInProgress');
  }.property('@each.isSelected', 'App.router.btnClickInProgress'),

  /**
   * List of validation errors. Look to #createError method for information
   * regarding object structure.
   *
   * @type {Object[]}
   */
  errorStack: [],

  isAddServiceWizard: Em.computed.equal('content.controllerName', 'addServiceController'),

  isOzoneInstalled: function() {
    let isOzone = this.findProperty('serviceName', 'OZONE');
    return isOzone && isOzone.get('isInstalled');
  }.property('@each.isInstalled'),

  /**
   * Services which are HDFS compatible
   */
  fileSystems: function() {
    let fileSystems = [];
    const self = this;
    this.filterProperty('isDFS', true).forEach((fs) => {
      if (self.get('isAddServiceWizard') && self.get('isOzoneInstalled') &&
        fs.get('serviceName') === 'HDFS') {
        return;
      }
      fileSystems.push(fs);
    });

    return fileSystems.map(function(fs) {
      return App.FileSystem.create({content: fs, services: fileSystems});
    });
  }.property('@each.isDFS'),

  /**
   * Drop errorStack content on selected state changes.
   */
  clearErrors: function() {
    if (!this.get('errorStack').someProperty('isAccepted', false)) {
      this.set('errorStack', []);
    }
  }.observes('@each.isSelected'),

  /**
   * Check if multiple distributed file systems were selected
   * @return {bool}
   * @method multipleDFSs
   */
  multipleDFSs: function () {
    return this.filterProperty('isDFS',true).filterProperty('isSelected',true).length > 1;
  },

  /**
   * Warn user if he tries to install Spark with HDP 2.2
   * @param {function} callback
   * @method sparkValidation
   */
  sparkValidation: function (callback) {
    var sparkService = this.findProperty('serviceName', 'SPARK');
    if (sparkService && !sparkService.get('isInstalled') &&
      App.get('currentStackName') === 'HDP' && App.get('currentStackVersionNumber') === '2.2') {
      if(sparkService.get('isSelected')) {
        this.addValidationError({
          id: 'sparkWarning',
          type: 'WARNING',
          callback: this.sparkWarningPopup,
          callbackParams: [callback]
        });
      }
      else {
        //Spark is selected, remove the Spark error from errorObject array
        var sparkError = this.get('errorStack').filterProperty('id',"sparkWarning");
        if(sparkError) {
           this.get('errorStack').removeObject(sparkError[0]);
        }
      }
    }
  },

  /**
   * Onclick handler for <code>Next</code> button.
   * Disable 'Next' button while it is already under process. (using Router's property 'nextBtnClickInProgress')
   * @method submit
   */
  submit: function () {
    if(App.get('router.nextBtnClickInProgress')) {
      return;
    }
    if (!this.get('isSubmitDisabled')) {
      this.unSelectServices();
      this.setGroupedServices();
      if (this.validate()) {
        this.set('errorStack', []);
        App.router.send('next');
      }
    }
  },

  /**
   * Set isSelected based on property doNotShowAndInstall
   */
  unSelectServices: function () {
    this.filterProperty('isSelected',true).filterProperty('doNotShowAndInstall', true).setEach('isSelected', false);
  },

  /**
   * Check if validation passed:
   *  - required file system services selected
   *  - dependencies between services
   *  - monitoring services selected (not required)
   *
   * @return {Boolean}
   * @method validate
   */
  validate: function () {
    var result;
    var self = this;

    // callback function to reset `isAccepted` needs to be called everytime when a popup from errorStack is dismissed/proceed by user action
    var callback = function (id) {
      var check = self.get('errorStack').findProperty('id', id);
      if (check) {
        check.isAccepted = true;
      }
    };
    this.serviceDependencyValidation(callback);
    this.fileSystemServiceValidation(callback);
    if (this.get('wizardController.name') === 'installerController') {
      this.serviceValidation(callback, 'AMBARI_METRICS', 'ambariMetricsCheck');
      this.serviceValidation(callback, 'SMARTSENSE', 'smartSenseCheck');
      this.serviceValidation(callback, 'RANGER', 'rangerCheck');
      this.serviceValidation(callback, 'ATLAS', 'atlasCheck');
    }
    this.dependentServiceValidation('RANGER', 'AMBARI_INFRA_SOLR', 'ambariRangerInfraCheck', callback);
    this.dependentServiceValidation('ATLAS', 'AMBARI_INFRA_SOLR', 'ambariAtlasInfraCheck', callback);
    this.dependentServiceValidation('ATLAS', 'HBASE', 'ambariAtlasHbaseCheck', callback);
    this.dependentServiceValidation('LOGSEARCH', 'AMBARI_INFRA_SOLR', 'ambariLogsearchCheck', callback);
    this.sparkValidation(callback);
    if (!!this.get('errorStack').filterProperty('isShown', false).length) {
      var firstError = this.get('errorStack').findProperty('isShown', false);
      this.showError(firstError);
      result = false;
    } else {
      result = true;
    }
    return result;
  },

  /**
   * display validation warning if dependent service not selected
   * @param {string} selectedService
   * @param {string} dependentService
   * @param {string} checkId
   * @param {Function} callback
   */
  dependentServiceValidation: function(selectedService, dependentService, checkId, callback) {
    var selected = this.findProperty('serviceName', selectedService);
    var dependent = this.findProperty('serviceName', dependentService);
    if (selected && selected.get('isSelected') && dependent && !dependent.get('isSelected')) {
      this.serviceValidation(callback, dependentService, checkId);
    } else {
      var unNeededError = this.get('errorStack').filterProperty('id', checkId);
      if (unNeededError) {
        this.get('errorStack').removeObject(unNeededError[0]);
      }
    }
  },

  /**
   * Check whether user selected service to install and go to next step
   * @param callback {Function}
   * @param serviceName {string}
   * @param id {string}
   * @method serviceValidation
   */
  serviceValidation: function(callback, serviceName, id) {
    var service = this.findProperty('serviceName', serviceName);
    if (service) {
      if (!service.get('isSelected')) {
        this.addValidationError({
          id: id,
          type: 'WARNING',
          callback: this.serviceCheckPopup,
          callbackParams: [callback]
        });
      }
      else {
        //metrics is selected, remove the metrics error from errorObject array
        var metricsError = this.get('errorStack').filterProperty('id', id);
        if (metricsError) {
          this.get('errorStack').removeObject(metricsError[0]);
        }
      }
    }
  },

  /**
   * Create error and push it to stack.
   *
   * @param {Object} errorObject - look to #createError
   * @return {Boolean}
   * @method addValidationError
   */
  addValidationError: function (errorObject) {
    if (!this.get('errorStack').someProperty('id', errorObject.id)) {
      this.get('errorStack').push(this.createError(errorObject));
      return true;
    }
    return false;
  },

  /**
   * Show current error by passed error object.
   *
   * @param {Object} errorObject
   * @method showError
   */
  showError: function (errorObject) {
    return errorObject.callback.apply(errorObject.callbackContext, errorObject.callbackParams.concat(errorObject.id));
  },

  /**
   * Default primary button("Ok") callback for warning popups.
   *  Change isShown state for last shown error.
   *  Call #submit() method.
   *
   *  @param {function} callback
   *  @param {string} id
   *  @method onPrimaryPopupCallback
   */
  onPrimaryPopupCallback: function(callback, id) {
    var firstError = this.get('errorStack').findProperty('isShown', false);
    if (firstError) {
      firstError.isShown = true;
    }
    if (callback) {
      callback(id);
    }
    this.submit();
  },

  /**
   * Create error object with passed options.
   * Available options:
   *  id - {String}
   *  type - {String}
   *  isShowed - {Boolean}
   *  callback - {Function}
   *  callbackContext
   *  callbackParams - {Array}
   *
   * @param {Object} opt
   * @return {Object}
   * @method createError
   */
  createError: function(opt) {
    var options = {
      // {String} error identifier
      id: '',
      // {String} type of error CRITICAL|WARNING
      type: 'CRITICAL',
      // {Boolean} error was shown
      isShown: false,
      // {Boolean} error was accepted by user
      isAccepted: false,
      // {Function} callback to execute
      callback: null,
      // context which execute from
      callbackContext: this,
      // {Array} params applied to callback
      callbackParams: []
    };
    $.extend(options, opt);
    return options;
  },

  /**
   * Checks if a filesystem is present in the Stack
   *
   * @method isDFSStack
   */
  isDFSStack: function () {
	  var bDFSStack = false;
    var dfsServices = ['HDFS', 'GLUSTERFS', 'OZONE'];
    var availableServices = this.filterProperty('isInstalled',false);
    availableServices.forEach(function(service){
      if (dfsServices.contains(service.get('serviceName')) || service.get('serviceType') == 'HCFS' ) {
        bDFSStack=true;
      }
    },this);
    return bDFSStack;
  },

  /**
   * Checks if a filesystem is selected and only one filesystem is selected
   * @param {function} callback
   * @method isFileSystemCheckFailed
   */
  fileSystemServiceValidation: function(callback) {
    if(this.isDFSStack()){
      const self = this;
      var primaryDFS = this.findProperty('isPrimaryDFS',true);
      if (primaryDFS) {
        var primaryDfsDisplayName = primaryDFS.get('displayNameOnSelectServicePage');
        var primaryDfsServiceName = primaryDFS.get('serviceName');
        //if multiple DFS are not selected, remove the related error from the error array
        let removeFsError = function () {
          let fsError = self.get('errorStack').findProperty('id',"multipleDFS");
          if(fsError)
          {
            self.get('errorStack').removeObject(fsError);
          }
        };
        if (this.multipleDFSs()) {
          var dfsServices = this.filterProperty('isDFS',true).filterProperty('isSelected',true).mapProperty('serviceName');
          //special case for HDFS and OZONE
          if (dfsServices.length === 2 && dfsServices.includes('HDFS') && dfsServices.includes('OZONE')) {
            removeFsError();
            return;
          }
          var services = dfsServices.map(function (item){
            return {
              serviceName: item,
              selected: item === primaryDfsServiceName
            };
          });
          this.addValidationError({
            id: 'multipleDFS',
            callback: this.needToAddServicePopup,
            callbackParams: [services, 'multipleDFS', primaryDfsDisplayName, callback]
          });
        }
        else
        {
          removeFsError();
        }
      }
    }
  },

  /**
   * Checks if a dependent service is selected without selecting the main service.
   * @param {function} callback
   * @method serviceDependencyValidation
   */
  serviceDependencyValidation: function(callback) {
    var selectedServices = this.filterProperty('isSelected', true);
    var availableServices = this.get('content');
    var missingDependencies = [];
    selectedServices.forEach(function(service) {
      service.collectMissingDependencies(selectedServices, availableServices, missingDependencies);
    });
    this.cleanExistingServiceCheckErrors();
    this.addServiceCheckErrors(missingDependencies, callback);
  },

  cleanExistingServiceCheckErrors() {
    var existingServiceCheckErrors = this.get('errorStack').filter(function (error) {
      return error.id.startsWith('serviceCheck_');
    });
    this.get('errorStack').removeObjects(existingServiceCheckErrors);
  },

  addServiceCheckErrors(missingDependencies, callback) {
    for(var i = 0; i < missingDependencies.length; i++) {
      this.addValidationError({
        id: 'serviceCheck_' + missingDependencies[i].get('serviceName'),
        callback: this.needToAddMissingDependency,
        callbackParams: [missingDependencies[i], 'serviceCheck', callback]
      });
    }
  },

  /**
   * Select co hosted services which not showed on UI.
   *
   * @method setGroupedServices
   */
  setGroupedServices: function() {
    this.forEach(function(service){
      var coSelectedServices = service.get('coSelectedServices');
      coSelectedServices.forEach(function(groupedServiceName) {
        var groupedService = this.findProperty('serviceName', groupedServiceName);
        if (groupedService.get('isSelected') !== service.get('isSelected')) {
          groupedService.set('isSelected',service.get('isSelected'));
        }
      },this);
    },this);
  },

  /**
   * Select/deselect services
   * @param {object[]|object} services array of objects
   *  <code>
   *    [
   *      {
   *        service: 'HDFS',
   *        selected: true
   *      },
   *      ....
   *    ]
   *  </code>
   * @param {string} i18nSuffix
   * @param {string} serviceName
   * @param {function} callback
   * @param {string} id
   * @return {App.ModalPopup}
   * @method needToAddServicePopup
   */

  needToAddServicePopup: function (services, i18nSuffix, serviceName, callback, id) {
    var self = this;
    return App.ModalPopup.show({
      'data-qa': 'need-add-service-confirmation-modal',
      header: Em.I18n.t('installer.step4.' + i18nSuffix + '.popup.header').format(serviceName),
      body: Em.I18n.t('installer.step4.' + i18nSuffix + '.popup.body').format(serviceName, serviceName),
      onPrimary: function () {
        Em.makeArray(services).forEach(function (service) {
          self.findProperty('serviceName', service.serviceName).set('isSelected', service.selected);
        });
        self.onPrimaryPopupCallback(callback, id);
        this.hide();
      },
      onSecondary: function () {
        if (callback) {
          callback(id);
        }
        this._super();
      },
      onClose: function () {
        if (callback) {
          callback(id);
        }
        this._super();
      }
    });
  },

  needToAddMissingDependency: function (missingDependency, i18nSuffix, callback, id) {
    var self = this;
    var displayName = missingDependency.get('displayName');
    if (missingDependency.get('hasMultipleOptions')) {
      return this.showDependencyPopup(
        id,
        Em.I18n.t('installer.step4.' + i18nSuffix + '.popup.header').format(displayName),
        Em.I18n.t('installer.step4.' + i18nSuffix + '.popup.body.multiOptions').format(displayName, missingDependency.get('displayOptions')),
        callback
      );
    } else {
      return this.showDependencyPopup(
        id,
        Em.I18n.t('installer.step4.' + i18nSuffix + '.popup.header').format(displayName),
        Em.I18n.t('installer.step4.' + i18nSuffix + '.popup.body').format(displayName, missingDependency.get('serviceName')),
        callback,
        function () {
          missingDependency.selectFirstCompatible();
          self.onPrimaryPopupCallback(callback, id);
          this.hide();
        }
      );
    }
  },

  showDependencyPopup: function(id, header, body, callback, primaryAction) {
    return App.ModalPopup.show({
        'data-qa': 'need-add-service-confirmation-modal',
        header: header,
        body: body,
        onPrimary: primaryAction || function() { this.onClose(); },
        onSecondary: function() {
          this.onClose();
        },
        onClose: function() {
          if (callback) {
            callback(id);
          }
          this._super();
        }
      });
  },

  /**
   * Show popup with info about not selected service
   * @param {function} callback
   * @param {string} id
   * @return {App.ModalPopup}
   * @method serviceCheckPopup
   */
  serviceCheckPopup: function (callback, id) {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step4.limitedFunctionality.popup.header'),
      //body: Em.I18n.t('installer.step4.' + id + '.popup.body'),
      bodyClass: Em.View.extend({
        serviceHeader: Em.I18n.t('installer.step4.' + id + '.popup.header'),
        serviceBody: Em.I18n.t('installer.step4.' + id + '.popup.body'),
        templateName: require('templates/wizard/step4/step4_service_validation_popup')
      }),
      primary: Em.I18n.t('common.proceedAnyway'),
      primaryClass: 'btn-warning',
      onPrimary: function () {
        self.onPrimaryPopupCallback(callback);
        this.hide();
      },
      onSecondary: function () {
        if (callback) {
          callback(id);
        }
        this._super();
      },
      onClose: function () {
        if (callback) {
          callback(id);
        }
        this._super();
      }
    });
  },

  /**
   * Show popup with Spark installation warning
   * @param {function} callback
   * @param {string} id
   * @return {App.ModalPopup}
   * @method sparkWarningPopup
   */
  sparkWarningPopup: function (callback, id) {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('common.warning'),
      body: Em.I18n.t('installer.step4.sparkWarning.popup.body'),
      primary: Em.I18n.t('common.proceed'),
      onPrimary: function () {
        self.onPrimaryPopupCallback(callback);
        this.hide();
      },
      onSecondary: function () {
        if (callback) {
          callback(id);
        }
        this._super();
      },
      onClose: function () {
        if (callback) {
          callback(id);
        }
       this._super();
      }
    });
  }
});
