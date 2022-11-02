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
require('controllers/main/admin/kerberos/step4_controller');

App.MainAdminKerberosController = App.KerberosWizardStep4Controller.extend({
  name: 'mainAdminKerberosController',

  /**
   * @type {boolean}
   * @deafult false
   */
  securityEnabled: false,

  /**
   * @type {boolean}
   * @default false
   */
  defaultKerberosLoaded: false,

  /**
   * @type {boolean}
   * @default false
   */
  dataIsLoaded: false,

  /**
   * @type {boolean}
   * @default true
   */
  isRecommendedLoaded: true,

  /**
   * @type {boolean}
   * @default false
   */
  isEditMode: false,

  /**
   * @type {string}
   */
  kdc_type: '',

  kdcTypesValues: {
    'mit-kdc': Em.I18n.t('admin.kerberos.wizard.step1.option.kdc'),
    'active-directory': Em.I18n.t('admin.kerberos.wizard.step1.option.ad'),
    'ipa': Em.I18n.t('admin.kerberos.wizard.step1.option.ipa'),
    'none': Em.I18n.t('admin.kerberos.wizard.step1.option.manual')
  },

  getAddSecurityWizardStatus: function () {
    return App.db.getSecurityWizardStatus();
  },
  setAddSecurityWizardStatus: function (status) {
    App.db.setSecurityWizardStatus(status);
  },

  setDisableSecurityStatus: function (status) {
    App.db.setDisableSecurityStatus(status);
  },
  getDisableSecurityStatus: function (status) {
    return App.db.getDisableSecurityStatus();
  },

  notifySecurityOff: false,
  notifySecurityAdd: false,

  notifySecurityOffPopup: function () {
    var self = this;
    this.checkServiceWarnings().then(function() {
      App.ModalPopup.show({
        header: Em.I18n.t('popup.confirmation.commonHeader'),
        primary: Em.I18n.t('ok'),
        onPrimary: function () {
          App.db.setSecurityDeployCommands(undefined);
          self.setDisableSecurityStatus("RUNNING");
          App.router.transitionTo('disableSecurity');
          this.hide();
        },
        bodyClass: Ember.View.extend({
          templateName: require('templates/main/admin/kerberos/notify_security_off_popup')
        })
      });
    });
  },

  /**
   * Show confirmation popup for regenerate keytabs
   * @method regenerateKeytabs
   * @param callback function (optional)
   * @return {App.ModalPopup}
   */
  regenerateKeytabs: function (callback) {
    var self = this;

    return App.ModalPopup.show({

      /**
       * True - regenerate keytabs only for missing hosts and components, false - regenerate for all hosts and components
       * @type {boolean}
       */
      regenerateKeytabsOnlyForMissing: false,

      header: Em.I18n.t('admin.kerberos.button.regenerateKeytabs'),

      bodyClass: Em.View.extend({
        templateName: require('templates/main/admin/kerberos/regenerate_keytabs_popup_body')
      }),

      onPrimary: function () {
        this._super();
        return self.restartServicesAfterRegenerate(this.get('regenerateKeytabsOnlyForMissing'), callback);
      }
    });
  },

  /**
   * Show confirmation popup for restarting all services and after confirmation regenerate keytabs
   *
   * @param regenerateKeytabsOnlyForMissing {Boolean}
   * @param callback (optional)
   * @returns {*}
   */
  restartServicesAfterRegenerate: function (regenerateKeytabsOnlyForMissing, callback) {
    var self = this;

    return App.ModalPopup.show({

      /**
       * True - automatically restart services, false - user will have to restart required services manually
       * @type {boolean}
       */
      restartComponents: false,

      header: Em.I18n.t('admin.kerberos.button.regenerateKeytabs'),

      bodyClass: Em.View.extend({
        templateName: require('templates/main/admin/kerberos/restart_services_after_regenerate_body')
      }),

      onPrimary: function () {
        this._super();
        var popupContext = this;
        // Keytabs can either be regenerated directly or after updating kerberos descriptor in the callback function
        if (Em.typeOf(callback) === 'function') {
          callback().done(function () {
            self.regenerateKeytabsRequest(regenerateKeytabsOnlyForMissing, popupContext.get('restartComponents'));
          });
        } else {
          self.regenerateKeytabsRequest(regenerateKeytabsOnlyForMissing, popupContext.get('restartComponents'));
        }
      }
    });
  },

  /**
   * Send request to regenerate keytabs
   * @param {boolean} missingOnly determines type of regeneration - missing|all
   * @param {boolean} withAutoRestart determines if the system should automatically restart all services or not after regeneration
   * @returns {$.ajax}
   */
  regenerateKeytabsRequest: function (missingOnly, withAutoRestart) {
    missingOnly = missingOnly || false;

    return App.ajax.send({
      name: "admin.kerberos_security.regenerate_keytabs",
      sender: this,
      data: {
        type: missingOnly ? 'missing' : 'all',
        withAutoRestart: withAutoRestart || false
      },
      success: "regenerateKeytabsSuccess"
    });
  },

  /**
   * Success callback of <code>regenerateKeytabs</code>
   * show background operations popup if appropriate option is set
   *
   * @param data
   * @param opt
   * @param params
   * @param request
   */
  regenerateKeytabsSuccess: function (data, opt, params, request) {
    var self = this;
    App.router.get('userSettingsController').dataLoading('show_bg').done(function (initValue) {
      if (initValue) {
        App.router.get('backgroundOperationsController').showPopup();
      }
      self.set('needsRestartAfterRegenerate', params.withAutoRestart);
    });
  },

  /**
   * Do request to server for restarting all services
   * @method restartAllServices
   * @return {$.ajax}
   */
  restartAllServices: function () {
    if (!App.router.get('backgroundOperationsController.runningOperationsCount')) {
      if (this.get('needsRestartAfterRegenerate')) {
        this.set('needsRestartAfterRegenerate', false);
        App.router.get('mainServiceController').restartAllServices();
      }
    }
  }.observes('controllers.backgroundOperationsController.runningOperationsCount'),

  /**
   * performs cluster check before kerbefos security
   * wizard starts if <code>preKerberizeCheck<code> supports is true
   * otherwise runs <code>startKerberosWizard<code>
   * @method checkAndStartKerberosWizard
   */
  checkAndStartKerberosWizard: function () {
    if (App.get('supports.preKerberizeCheck')) {
      App.ajax.send({
        name: "admin.kerberos_security.checks",
        sender: this,
        success: "runSecurityCheckSuccess"
      });
    } else {
      this.startKerberosWizard();
    }
  },

  /**
   * success callback of <code>checkAndStartKerberosWizard()</code>
   * if there are some fails - it shows popup else open security wizard
   * @param data {object}
   * @param opt {object}
   * @param params {object}
   */
  runSecurityCheckSuccess: function (data, opt, params) {
    //TODO correct check
    if (data.items.someProperty('UpgradeChecks.status', "FAIL")) {
      var
        hasFails = data.items.someProperty('UpgradeChecks.status', 'FAIL'),
        header = Em.I18n.t('popup.clusterCheck.Security.header').format(params.label),
        title = Em.I18n.t('popup.clusterCheck.Security.title'),
        alert = Em.I18n.t('popup.clusterCheck.Security.alert');

      App.showClusterCheckPopup(data, {
        header: header,
        failTitle: title,
        failAlert: alert,
        noCallbackCondition: hasFails
      });
    } else {
      this.startKerberosWizard();
    }
  },

  startKerberosWizard: function () {
    var self = this;
    this.checkServiceWarnings().then(function() {
      self.setAddSecurityWizardStatus('RUNNING');
      App.router.get('kerberosWizardController').setDBProperty('onClosePath', 'main.admin.adminKerberos.index');
      App.router.transitionTo('adminKerberos.adminAddKerberos');
    });
  },

  /**
   * Loads the security status from server (security_enabled property in cluster-env configuration)
   */
  loadSecurityStatusFromServer: function () {
    if (App.get('testMode')) {
      this.set('securityEnabled', !App.get('testEnableSecurity'));
      this.set('dataIsLoaded', true);
    } else {
      //get Security Status From Server
      this.getSecurityType();
      return this.getSecurityStatus();
    }
  },

  /**
   * Load security status from server.
   * @returns {$.Deferred}
   */
  getSecurityStatus: function () {
    var self = this;
    var dfd = $.Deferred();
    if (App.get('testMode')) {
      this.set('securityEnabled', !App.get('testEnableSecurity'));
      this.set('dataIsLoaded', true);
      dfd.resolve();
    } else {
      //get Security Status From Server
      App.ajax.send({
        name: 'admin.security_status',
        sender: this,
        success: 'getSecurityStatusSuccessCallback',
        error: 'errorCallback'
      })
        .always(function() {
          self.getSecurityType(function() {
            dfd.resolve();
          });
        });
    }
    return dfd.promise();
  },

  getSecurityStatusSuccessCallback: function (data) {
    this.set('dataIsLoaded', true);
    var securityType = data.Clusters.security_type;
    this.set('securityEnabled', securityType === 'KERBEROS');
  },

  errorCallback: function (jqXHR) {
    this.set('dataIsLoaded', true);
    // Show the error popup if the API call received a response from the server.
    // jqXHR.status will be empty when browser cancels the request. Refer to AMBARI-5921 for more info
    if (!!jqXHR.status) {
      this.showSecurityErrorPopup();
    }
  },

  showSecurityErrorPopup: function () {
    App.ModalPopup.show({
      header: Em.I18n.t('common.error'),
      secondary: false,
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile('<p>{{t admin.security.status.error}}</p>')
      })
    });
  },

  /**
   * Override <code>App.KerberosWizardStep4Controller</code>
   */
  clearStep: function() {
    this.set('isEditMode', false);
    this._super();
  },

  /**
   * Override <code>App.KerberosWizardStep4Controller</code>
   *
   * @param {App.ServiceConfigProperty[]} properties
   */
  setStepConfigs: function (properties) {
    this.get('stepConfigs').clear();
    this._super(properties);
    this.set('selectedService', this.get('stepConfigs')[0]);
    this.get('stepConfigs').forEach(function (serviceConfig) {
      serviceConfig.set('initConfigsLength', serviceConfig.get('configs.length'));
    });
  },

  /**
   * Override <code>App.KerberosWizardStep4Controller</code>
   *
   * @param {App.ServiceConfigProperty[]} configs
   * @returns {App.ServiceConfigProperty[]}
   */
  prepareConfigProperties: function (configs) {
    var self = this;
    var configProperties = configs.slice(0);
    var siteProperties = App.configsCollection.getAll();
    var installedServiceNames = ['Cluster'].concat(App.Service.find().mapProperty('serviceName'));
    configProperties = configProperties.filter(function (item) {
      return installedServiceNames.contains(item.get('serviceName'));
    });
    configProperties.setEach('isSecureConfig', false);
    configProperties.forEach(function (property, item, allConfigs) {
      if (['spnego_keytab', 'spnego_principal'].contains(property.get('name'))) {
        property.addObserver('value', self, 'spnegoPropertiesObserver');
      }
      if (property.get('observesValueFrom')) {
        var observedValue = allConfigs.findProperty('name', property.get('observesValueFrom')).get('value');
        property.set('value', observedValue);
        property.set('recommendedValue', observedValue);
      }
      if (property.get('serviceName') == 'Cluster') {
        property.set('category', 'Global');
      } else {
        property.set('category', property.get('serviceName'));
      }
      // All user identity should be grouped under "Ambari Principals" category
      if (property.get('identityType') == 'user') property.set('category', 'Ambari Principals');
      var siteProperty = siteProperties.findProperty('name', property.get('name'));
      if (siteProperty) {
        if (siteProperty.category === property.get('category')) {
          property.set('displayName', siteProperty.displayName);
          if (siteProperty.index) {
            property.set('index', siteProperty.index);
          }
        }
        if (siteProperty.displayType) {
          property.set('displayType', siteProperty.displayType);
        }
      }
    });
    configProperties.setEach('isEditable', false);
    return configProperties;
  },

  getKDCSessionState: function (callback, kdcCancelHandler) {
    var self = this;
    if (this.get('securityEnabled') || App.get('isKerberosEnabled')) {
      this.getSecurityType(function () {
        if (!self.get('isManualKerberos')) {
          App.ajax.send({
            name: 'kerberos.session.state',
            sender: self,
            data: {
              callback: callback
            },
            success: 'checkState',
            kdcCancelHandler: kdcCancelHandler
          })
        } else {
          callback();
        }
      });
    } else {
      callback();
    }
  },

  /**
   * Determines security type.
   *
   * @param {function} [callback] callback function to execute
   * @returns {$.Deferred|null}
   */
  getSecurityType: function (callback) {
    if (this.get('securityEnabled') || App.get('isKerberosEnabled')) {
      if (!this.get('kdc_type')) {
        return App.ajax.send({
          name: 'admin.security.cluster_configs.kerberos',
          sender: this,
          data: {
            clusterName: App.get('clusterName'),
            additionalCallback: callback
          },
          success: 'getSecurityTypeSuccess'
        });
      } else {
        if (Em.typeOf(callback) === 'function') {
          callback();
        }
        return $.Deferred().resolve().promise;
      }
    } else if (Em.typeOf(callback) === 'function') {
      callback();
    } else {
      return $.Deferred().resolve().promise;
    }
  },

  getSecurityTypeSuccess: function (data, opt, params) {
    var kdcType = data.items && data.items[0] &&
        Em.getWithDefault(Em.getWithDefault(data.items[0], 'configurations', []).findProperty('type', 'kerberos-env') || {}, 'properties.kdc_type', 'none') || 'none';
    this.set('kdc_type', kdcType);
    if (Em.typeOf(params.additionalCallback) === 'function') {
      params.additionalCallback();
    }
  },

  isManualKerberos: Em.computed.equal('kdc_type', 'none'),

  checkState: function (data, opt, params) {
    var res = Em.get(data, 'Services.attributes.kdc_validation_result');
    var message = Em.get(data, 'Services.attributes.kdc_validation_failure_details');
    if (res.toUpperCase() === "OK") {
      params.callback();
    } else {
      App.showInvalidKDCPopup(opt, App.format.kdcErrorMsg(message, false));
    }
  },

  /**
   * Determines if some config value is changed
   * @type {boolean}
   */
  isPropertiesChanged: Em.computed.someBy('stepConfigs', 'isPropertiesChanged', true),

  /**
   * Determines if the save button is disabled
   */
  isSaveButtonDisabled: Em.computed.or('isSubmitDisabled', '!isPropertiesChanged'),

  /**
   * Determines if the `Disbale Kerberos` and `Regenerate Keytabs` button are disabled
   */
  isKerberosButtonsDisabled: Em.computed.not('isSaveButtonDisabled'),


  makeConfigsEditable: function () {
    if (this.get('stepConfigs') && this.get('stepConfigs.length')) {
      this.set('isEditMode', true);
      this.get('stepConfigs').forEach(function (_stepConfig) {
        _stepConfig.get('configs').setEach('isEditable', true);
        _stepConfig.get('configs').forEach(function (_config) {
          _config.set('isEditable', _config.get('name') != 'realm');
        });
      }, this);
    }
  },

  _updateConfigs: function () {
    this.makeConfigsUneditable(true);
  },

  /**
   * @method makeConfigsUneditable
   * @param configsUpdated
   */
  makeConfigsUneditable: function (configsUpdated) {
    this.set('isEditMode', false);
    this.get('stepConfigs').forEach(function (_stepConfig) {
      _stepConfig.get('configs').forEach(function (_config) {
        if (configsUpdated === true) {  // configsUpdated should be checked for boolean true
          _config.set('savedValue', _config.get('value'));
          _config.set('defaultValue', _config.get('value'));
        } else {
          _config.set('value', _config.get('savedValue') || _config.get('defaultValue'));
        }
        _config.set('isEditable', false);
      });
    }, this);
  },

  /**
   * Update kerberos descriptor and regenerate keytabs
   */
  submit: function (context) {
    var callback;
    var self = this;
    var kerberosDescriptor = this.get('kerberosDescriptor');
    var configs = [];
    this.get('stepConfigs').forEach(function (_stepConfig) {
      configs = configs.concat(_stepConfig.get('configs'));
    });
    callback = function () {
      return App.ajax.send({
        name: 'admin.kerberos.cluster.artifact.update',
        sender: self,
        data: {
          artifactName: 'kerberos_descriptor',
          data: {
            artifact_data: kerberosDescriptor
          }
        },
        success: '_updateConfigs',
        error: 'createKerberosDescriptor'
      });
    };
    this.updateKerberosDescriptor(kerberosDescriptor, configs);
    if (this.get('isManualKerberos')) {
      callback().done(function () {
        self.regenerateKeytabsRequest(false,false);
      });
    } else {
      this.restartServicesAfterRegenerate(false, callback);
    }
  },

  createKerberosDescriptor: function (requestData, ajaxOptions, error, opt, params) {
    if (requestData && requestData.status === 404) {
      const {artifactName, data} = params;
      App.ajax.send({
        name: 'admin.kerberos.cluster.artifact.create',
        sender: self,
        data: {
          artifactName,
          data
        },
        success: '_updateConfigs'
      });
    }
  },

  /**
   * List of the warnings regarding specific services before enabling/disabling Kerberos.
   *
   * @type {String[]}
   */
  serviceAlerts: function() {
    var messages = [];
    var serviceAlertMap = {
      YARN: Em.I18n.t('admin.kerberos.service.alert.yarn')
    };
    var installedServices = App.Service.find().mapProperty('serviceName');
    Em.keys(serviceAlertMap).forEach(function(serviceName) {
      if (installedServices.contains(serviceName)) {
        messages.push(serviceAlertMap[serviceName]);
      }
    });
    return messages;
  }.property(),

  /**
   * Check for additional info to display before enabling/disabling kerberos and show appropriate
   * messages in popup if needed.
   * @returns {$.Deferred} - promise
   */
  checkServiceWarnings: function() {
    var dfd = $.Deferred();
    this.displayServiceWarnings(this.get('serviceAlerts'), dfd);
    return dfd.promise();
  },

  /**
   * Show appropriate message regarding changes affected after enabling/disabling Kerberos
   *
   * @param {String[]} messages - list of the messages to display
   * @param {$.Deferred} dfd - used to break recursive calls and reject/resolve promise returned by <code>checkServiceWarnings</code>
   */
  displayServiceWarnings: function(messages, dfd) {
    var self = this;
    if (!messages.get('length')) {
      dfd.resolve();
    } else {
      App.showConfirmationPopup(function() {
        self.displayServiceWarnings(messages.slice(1), dfd);
      }, messages[0], function() {
        dfd.reject();
      }, Em.I18n.t('common.warning'), Em.I18n.t('common.proceedAnyway'));
    }
  },

  showManageKDCCredentialsPopup: function() {
    return App.showManageCredentialsPopup();
  },

  loadStep: function() {
    var self = this;
    if (this.get('isRecommendedLoaded') === false) {
      return;
    }
    this.clearStep();
    this.getDescriptor().then(function (properties) {
      self.setStepConfigs(self.createServicesStackDescriptorConfigs(properties));
    }).always(function() {
      self.set('isRecommendedLoaded', true);
    });
  },

  showDownloadCsv: function () {
    var hasUpgradePrivilege = App.isAuthorized('CLUSTER.UPGRADE_DOWNGRADE_STACK');
    return hasUpgradePrivilege;
  }.property(),

  downloadCSV: function() {
    App.router.get('kerberosWizardStep5Controller').getCSVData(false);
  }

});
