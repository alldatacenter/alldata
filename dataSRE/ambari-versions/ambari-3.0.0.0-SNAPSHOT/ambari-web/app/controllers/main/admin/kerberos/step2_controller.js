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
require('controllers/wizard/step7_controller');

App.KerberosWizardStep2Controller = App.WizardStep7Controller.extend(App.KDCCredentialsControllerMixin, {
  name: "kerberosWizardStep2Controller",

  isKerberosWizard: true,

  selectedServiceNames: ['KERBEROS'],

  allSelectedServiceNames: ['KERBEROS'],

  componentName: 'KERBEROS_CLIENT',

  installedServiceNames: [],

  servicesInstalled: false,

  addMiscTabToPage: false,

  kerberosConfigMap: {
    'ad': {
      configNames: ['ldap_url', 'container_dn', 'ad_create_attributes_template'],
      type: Em.I18n.t('admin.kerberos.wizard.step1.option.ad')
    },
    'mit': {
      configNames: ['kdc_create_attributes'],
      type: Em.I18n.t('admin.kerberos.wizard.step1.option.kdc')
    },
    'ipa': {
      configNames: ['ipa_user_group'],
      type: Em.I18n.t('admin.kerberos.wizard.step1.option.ipa')
    }
  },

  /**
   * @type {boolean} true if test connection to hosts is in progress
   */
  testConnectionInProgress: false,

  /**
   * Should Back-button be disabled
   * @type {boolean}
   */
  isBackBtnDisabled: Em.computed.or('testConnectionInProgress', 'App.router.nextBtnClickInProgress'),

  /**
   * Should Next-button be disabled
   * @type {boolean}
   */
  isSubmitDisabled: function () {
    if (!this.get('stepConfigs.length') || this.get('testConnectionInProgress')
      || this.get('submitButtonClicked') || App.get('router.nextBtnClickInProgress')) return true;
    return (!this.get('stepConfigs').filterProperty('showConfig', true).everyProperty('errorCount', 0) || this.get("miscModalVisible"));
  }.property('stepConfigs.@each.errorCount', 'miscModalVisible', 'submitButtonClicked', 'testConnectionInProgress', 'App.router.nextBtnClickInProgress'),

  hostNames: Em.computed.alias('App.allHostNames'),

  serviceConfigTags: [],

  clearStep: function () {
    this._super();
    this.set('configs', []);
    this.get('serviceConfigTags').clear();
    this.set('servicesInstalled', false);
  },

  isConfigsLoaded: Em.computed.alias('wizardController.stackConfigsLoaded'),

  /**
   * On load function
   * @method loadStep
   */

  loadStep: function () {
    if (!App.StackService.find().someProperty('serviceName', 'KERBEROS') || !this.get('isConfigsLoaded')) {
      return false;
    }
    this.clearStep();
    App.config.setPreDefinedServiceConfigs(this.get('addMiscTabToPage'));
    var stored = this.get('content.serviceConfigProperties');

    this.set('configs', stored ? App.config.mergeStoredValue(this.getKerberosConfigs(), stored) : this.getKerberosConfigs());

    this.filterConfigs(this.get('configs'));
    if (!this.get('wizardController.skipClientInstall')) {
      this.initializeKDCStoreProperties(this.get('configs'));
    }
    this.applyServicesConfigs(this.get('configs'));
    if (!this.get('wizardController.skipClientInstall')) {
      this.updateKDCStoreProperties(this.get('stepConfigs').findProperty('serviceName', 'KERBEROS').get('configs'));
    }
  },

  /**
   * @method getKerberosConfigs
   * @returns {Array.<T>|*}
   */
  getKerberosConfigs: function() {
    var kerberosConfigTypes = Em.keys(App.config.get('preDefinedServiceConfigs').findProperty('serviceName', 'KERBEROS').get('configTypes'));

    return App.configsCollection.getAll().filter(function(configProperty) {
      var fileName = Em.getWithDefault(configProperty, 'fileName', false);
      var isService = ['KERBEROS'].contains(Em.get(configProperty, 'serviceName'));
      var isFileName = fileName && kerberosConfigTypes.contains(App.config.getConfigTagFromFileName(fileName));
      return isService || isFileName;
    });
  },

  /**
   * Make Active Directory or IPA specific configs visible if user has selected AD or IPA option
   * @param configs
   */
  filterConfigs: function (configs) {
    var kdcType = this.get('content.kerberosOption');
    var kerberosWizardController = this.get('controllers.kerberosWizardController');
    var manageIdentitiesConfig = configs.findProperty('name', 'manage_identities');
    configs.filterProperty('serviceName', 'KERBEROS').setEach('isVisible', true);
    this.setKDCTypeProperty(configs);
    if (kdcType !== Em.I18n.t('admin.kerberos.wizard.step1.option.ad')) {
        kerberosWizardController.overrideVisibility(configs, false, kerberosWizardController.get('exceptionsForNonAdOption'), true);
    }
    if (kdcType === Em.I18n.t('admin.kerberos.wizard.step1.option.manual')) {
      if (kerberosWizardController.get('skipClientInstall')) {
        kerberosWizardController.overrideVisibility(configs, false, kerberosWizardController.get('exceptionsOnSkipClient'));
      }
      return;
    } else if (manageIdentitiesConfig) {
      manageIdentitiesConfig.isVisible = false;
      manageIdentitiesConfig.value = 'true';
    }

    this.setConfigVisibility('ad', configs, kdcType);
    this.setConfigVisibility('mit', configs, kdcType);
    this.setConfigVisibility('ipa', configs, kdcType);
  },

  /**
   *
   * @param {string} type
   * @param {Array} configs
   * @param {string} kdcType
   */
  setConfigVisibility: function(type, configs, kdcType) {
    var typeSettings = this.get('kerberosConfigMap')[type];

    typeSettings.configNames.forEach(function (_configName) {
      var config = configs.findProperty('name', _configName);
      if (config) {
        config.isVisible = kdcType === typeSettings.type;
      }
    }, this);
  },


  submit: function () {
    var self = this;
    if (this.get('isSubmitDisabled')) return false;
    App.set('router.nextBtnClickInProgress', true);
    this.get('wizardController').deleteKerberosService().always(function () {
      self.configureKerberos();
    });
  },

  configureKerberos: function () {
    var self = this;
    var wizardController = App.router.get(this.get('content.controllerName'));
    var callback = function () {
      self.createConfigurations().done(function () {
        self.createKerberosAdminSession().done(function () {
          App.set('router.nextBtnClickInProgress', false);
          App.router.send('next');
        });
      });
    };
    if (wizardController.get('skipClientInstall')) {
      callback();
    } else {
      wizardController.createKerberosResources(callback);
    }
  },

  createConfigurations: function () {
    var service = App.StackService.find().findProperty('serviceName', 'KERBEROS'),
        serviceConfigTags = [],
        allConfigData = [],
        serviceConfigData = [];

    Object.keys(service.get('configTypes')).forEach(function (type) {
      if (!serviceConfigTags.someProperty('type', type)) {
        var obj = this.createKerberosSiteObj(type);
        obj.service_config_version_note = Em.I18n.t('admin.kerberos.wizard.configuration.note');
        serviceConfigTags.pushObject(obj);
      }
    }, this);

    Object.keys(service.get('configTypesRendered')).forEach(function (type) {
      var serviceConfigTag = serviceConfigTags.findProperty('type', type);
      if (serviceConfigTag) {
        serviceConfigData.pushObject(serviceConfigTag);
      }
    }, this);
    if (serviceConfigData.length) {
      allConfigData.pushObject(JSON.stringify({
        Clusters: {
          desired_config: serviceConfigData
        }
      }));
    }
    return App.ajax.send({
      name: 'common.across.services.configurations',
      sender: this,
      data: {
        data: '[' + allConfigData.toString() + ']'
      }
    });
  },

  createKerberosSiteObj: function (site) {
    var properties = {};
    var content = this.get('stepConfigs')[0].get('configs');
    var configs = content.filterProperty('filename', site + '.xml');
    // properties that should be formated as hosts
    var hostProperties = ['kdc_hosts', 'realm'];

    configs.forEach(function (_configProperty) {
      // do not pass any globals whose name ends with _host or _hosts
      if (_configProperty.isRequiredByAgent !== false) {
        if (hostProperties.contains(_configProperty.name)) {
          properties[_configProperty.name] = App.config.trimProperty({displayType: 'host', value: _configProperty.value});
        } else {
          properties[_configProperty.name] = App.config.trimProperty(_configProperty);
        }
      }
    }, this);
    this.tweakKdcTypeValue(properties);
    this.tweakManualKdcProperties(properties);
    this.tweakIpaKdcProperties(properties);
    return {"type": site, "properties": properties};
  },

  tweakKdcTypeValue: function (properties) {
    var kdcTypesValues = App.router.get('mainAdminKerberosController.kdcTypesValues');
    for (var prop in kdcTypesValues) {
      if (kdcTypesValues.hasOwnProperty(prop)) {
        if (kdcTypesValues[prop] === properties['kdc_type']) {
          properties['kdc_type'] = prop;
        }
      }
    }
  },

  tweakManualKdcProperties: function (properties) {
    var kerberosWizardController = this.get('controllers.kerberosWizardController');
    if (properties['kdc_type'] === 'none' || kerberosWizardController.get('skipClientInstall')) {
      if (properties.hasOwnProperty('manage_identities')) {
        properties['manage_identities'] = 'false';
      }
      if (properties.hasOwnProperty('install_packages')) {
        properties['install_packages'] = 'false';
      }
      if (properties.hasOwnProperty('manage_krb5_conf')) {
        properties['manage_krb5_conf'] = 'false';
      }
    }
  },

  tweakIpaKdcProperties: function (properties) {
    if (typeof properties['kdc_type'] === 'undefined') {
      return;
    }
    if (this.get('content.kerberosOption') === App.router.get('mainAdminKerberosController.kdcTypesValues')['ipa']) {
      if (properties.hasOwnProperty('install_packages')) {
        properties['install_packages'] = 'false';
      }
      if (properties.hasOwnProperty('manage_krb5_conf')) {
        properties['manage_krb5_conf'] = 'false';
      }
    }
  },

  /**
   * puts kerberos admin credentials in the live cluster session
   * @returns {*} jqXHr
   */
  createKerberosAdminSession: function (configs) {
    configs = configs || this.get('stepConfigs')[0].get('configs');
    if (!this.get('wizardController.skipClientInstall')) {
      return this.createKDCCredentials(configs);
    }

    var adminPrincipalValue = configs.findProperty('name', 'admin_principal').value;
    var adminPasswordValue = configs.findProperty('name', 'admin_password').value;
    return App.ajax.send({
      name: 'common.cluster.update',
      sender: this,
      data: {
        clusterName: App.get('clusterName'),
        data: [{
          session_attributes: {
            kerberos_admin: {principal: adminPrincipalValue, password: adminPasswordValue}
          }
        }]
      }
    });
  },

  /**
   * shows popup with to warn user
   * @param {Function} primary
   */
  showConnectionInProgressPopup: function(primary) {
    var primaryText = Em.I18n.t('common.exitAnyway');
    var msg = Em.I18n.t('services.service.config.connection.exitPopup.msg');
    App.showConfirmationPopup(primary, msg, null, null, primaryText);
  },

  setKDCTypeProperty: function(configs) {
    var selectedOption = this.get('content.kerberosOption');
    var kdcTypeProperty = configs.filterProperty('filename', 'kerberos-env.xml').findProperty('name', 'kdc_type');
    var kdcValuesMap = App.router.get('mainAdminKerberosController.kdcTypesValues');
    var kdcTypeValue = Em.keys(kdcValuesMap).filter(function(typeAlias) {
      return Em.get(kdcValuesMap, typeAlias) === selectedOption;
    })[0];
    if (kdcTypeProperty) {
      Em.set(kdcTypeProperty, 'value', kdcValuesMap[kdcTypeValue]);
    }
  },

  /**
   * Override App.WizardStep7Controller#overrideConfigIsRequired
   *
   * @override
   */
  overrideConfigIsRequired: function() {}
});
