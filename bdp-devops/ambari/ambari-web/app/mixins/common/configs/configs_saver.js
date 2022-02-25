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
var lazyLoading = require('utils/lazy_loading');
var stringUtils = require('utils/string_utils');

/**
 * Mixin for saving configs
 * Contains methods for
 * - generation JSON for saving configs and groups
 * - format properties if needed
 * - requests to save configs and groups
 */
App.ConfigsSaverMixin = Em.Mixin.create({

  /**
   * @type {boolean}
   */
  saveConfigsFlag: true,

  /**
   * file names of changed configs
   * @type {string[]}
   */
  modifiedFileNames: [],

  /**
   * List of heapsize properties not to be parsed
   * @type {string[]}
   */
  heapsizeException: ['hadoop_heapsize', 'yarn_heapsize', 'nodemanager_heapsize', 'resourcemanager_heapsize',
    'apptimelineserver_heapsize', 'jobhistory_heapsize', 'nfsgateway_heapsize', 'accumulo_master_heapsize',
    'accumulo_tserver_heapsize', 'accumulo_monitor_heapsize', 'accumulo_gc_heapsize', 'accumulo_other_heapsize',
    'hbase_master_heapsize', 'hbase_regionserver_heapsize', 'metrics_collector_heapsize', 'hive_heapsize'],

  /**
   * Regular expression for heapsize properties detection
   * @type {regexp}
   */
  heapsizeRegExp: /_heapsize|_newsize|_maxnewsize|_permsize|_maxpermsize$/,

  /**
   * If some of services has such type core-site can be saved
   *
   * @type {string}
   */
  coreSiteServiceType: 'HCFS',

  /**
   * Core site can be used by multiple services
   * If some of services listed below is selected/installed than core site can be saved
   *
   * @type {string[]}
   */
  coreSiteServiceNames: ['HDFS', 'GLUSTERFS', 'RANGER_KMS'],

  /**
   * List of services which configs should be saved
   *
   * @type {App.StackService[]}
   */
  currentServices: function() {
    return [App.StackService.find(this.get('content.serviceName'))];
  }.property('content.serviceName'),

  /**
   * clear info to default
   * @method clearSaveInfo
   */
  clearSaveInfo: function() {
    this.set('modifiedFileNames', []);
  },

  /**
   * method to run saving configs
   * @method saveStepConfigs
   */
  saveStepConfigs: function() {
    if (!this.get("isSubmitDisabled")) {
      this.startSave();
      this.showWarningPopupsBeforeSave();
    }
  },

  /**
   * get config group object for current service
   * @param serviceName
   * @returns {App.ConfigGroup}
   */
  getGroupFromModel: function(serviceName) {
    if (this.get('selectedService.serviceName') === serviceName) {
      return this.get('selectedConfigGroup');
    } else {
      var groups = App.ServiceConfigGroup.find().filterProperty('serviceName', serviceName);
      if (this.get('selectedConfigGroup.isDefault')) {
        return groups.length ? groups.findProperty('isDefault', true) : null;
      } else {
        return groups.length ? groups.findProperty('name', this.get('selectedConfigGroup.dependentConfigGroups')[serviceName]) : null;
      }
    }
  },

  /**
   * Save changed configs and config groups
   * @method saveConfigs
   */
  saveConfigs: function () {
    if (this.get('selectedConfigGroup.isDefault')) {
      this.saveConfigsForDefaultGroup();
    } else {
      this.saveConfigsForNonDefaultGroup();
    }
  },

  saveConfigsForNonDefaultGroup: function() {
    this.get('stepConfigs').forEach(function(stepConfig) {
      var serviceName = stepConfig.get('serviceName');
      var configs = stepConfig.get('configs');
      var configGroup = this.getGroupFromModel(serviceName);
      if (configGroup && !configGroup.get('isDefault')) {
        var overriddenConfigs = this.getConfigsForGroup(configs, configGroup.get('name'));

        if (Em.isArray(overriddenConfigs) && this.isOverriddenConfigsModified(overriddenConfigs, configGroup)) {
          var successCallback = this.get('content.serviceName') === serviceName ? 'putConfigGroupChangesSuccess' : null;
          this.saveGroup(overriddenConfigs, configGroup, this.get('serviceConfigVersionNote'), successCallback);
        }
      }
    }, this);
  },

  /**
   * @param {Array} overriddenConfigs
   * @returns {boolean}
   */
  isOverriddenConfigsModified: function(overriddenConfigs, group) {
    var hasChangedConfigs = overriddenConfigs.some(function(config) {
      return config.get('savedValue') !== config.get('value') || config.get('savedIsFinal') !== config.get('isFinal');
    });
    var overriddenConfigsNames = overriddenConfigs.mapProperty('name');
    return hasChangedConfigs || group.get('properties').some(function (property) {
        return !overriddenConfigsNames.contains(Em.get(property, 'name'));
      });
  },

  saveConfigsForDefaultGroup: function() {
    var data = [];
    this.get('stepConfigs').forEach(function(stepConfig) {
      var serviceConfig = this.getServiceConfigToSave(stepConfig.get('serviceName'), stepConfig.get('configs'));

      if (serviceConfig)  {
        data.push(serviceConfig);
      }
    }, this);

    if (data.length) {
      this.putChangedConfigurations(data, 'doPUTClusterConfigurationSiteSuccessCallback');
    } else {
      this.onDoPUTClusterConfigurations();
    }
  },

  /*********************************** 0. HELPERS ********************************************/

  /**
   * tells controller in saving configs was started
   * for now just changes flag <code>saveInProgress<code> to true
   * @private
   * @method startSave
   */
  startSave: function() {
    this.set("saveInProgress", true);
  },

  /**
   * tells controller that save has been finished
   * for now just changes flag <code>saveInProgress<code> to true
   * @private
   * @method completeSave
   */
  completeSave: function() {
    this.set("saveInProgress", false);
  },

  /**
   * Are some unsaved changes available
   * @returns {boolean}
   * @method hasUnsavedChanges
   */
  hasUnsavedChanges: function () {
    return !Em.isNone(this.get('hash')) && this.get('hash') !== this.getHash();
  },

  /*********************************** 1. PRE SAVE CHECKS ************************************/

  /**
   * show some warning popups before user save configs
   * @private
   * @method showWarningPopupsBeforeSave
   */
  showWarningPopupsBeforeSave: function() {
    var self = this;
    if (this.isDirChanged()) {
      App.showConfirmationPopup(function() {
          self.showChangedDependentConfigs(null, function() {
            self.restartServicePopup();
          });
        },
        Em.I18n.t('services.service.config.confirmDirectoryChange').format(self.get('content.displayName')),
        this.completeSave.bind(this)
      );
    } else {
      self.showChangedDependentConfigs(null, function() {
        self.restartServicePopup();
      }, this.completeSave.bind(this));
    }
  },

  /**
   * Runs config validation before save
   * @private
   * @method restartServicePopup
   */
  restartServicePopup: function () {
    this.serverSideValidation()
      .done(this.saveConfigs.bind(this))
      .fail(this.completeSave.bind(this));
  },

  /**
   * Define if user has changed some dir properties
   * @return {Boolean}
   * @private
   * @method isDirChanged
   */
  isDirChanged: function () {
    var dirChanged = false;
    var serviceName = this.get('content.serviceName');

    if (serviceName === 'HDFS') {
      var hdfsConfigs = this.get('stepConfigs').findProperty('serviceName', 'HDFS').get('configs');
      if ((hdfsConfigs.findProperty('name', 'dfs.namenode.name.dir') && hdfsConfigs.findProperty('name', 'dfs.namenode.name.dir').get('isNotDefaultValue')) ||
        (hdfsConfigs.findProperty('name', 'dfs.namenode.checkpoint.dir') && hdfsConfigs.findProperty('name', 'dfs.namenode.checkpoint.dir').get('isNotDefaultValue')) ||
        (hdfsConfigs.findProperty('name', 'dfs.datanode.data.dir') && hdfsConfigs.findProperty('name', 'dfs.datanode.data.dir').get('isNotDefaultValue'))) {
        dirChanged = true;
      }
    }
    return dirChanged;
  },

  /*********************************** 2. GENERATING DATA TO SAVE ****************************/

  /**
   * get config properties for that fileNames that was changed
   * @param stepConfigs
   * @private
   * @returns {Array}
   */
  getModifiedConfigs: function(stepConfigs) {
    var modifiedConfigs = stepConfigs
      // get only modified and created configs
      .filter(function (config) {
        return config.get('isNotDefaultValue') || config.get('isNotSaved');
      })
      // get file names and add file names that was modified, for example after property removing
      .mapProperty('filename').concat(this.get('modifiedFileNames')).uniq()
      // get configs by filename
      .map(function (fileName) {
        return stepConfigs.filterProperty('filename', fileName);
      });

    if (!!modifiedConfigs.length) {
      // concatenate results
      modifiedConfigs = modifiedConfigs.reduce(function (current, prev) {
        return current.concat(prev);
      });
    }
    return modifiedConfigs;
  },

  /**
   * get configs that belongs to config group
   * @param stepConfigs
   * @private
   * @param configGroupName
   */
  getConfigsForGroup: function(stepConfigs, configGroupName) {
    var overridenConfigs = [];

    stepConfigs.filterProperty('overrides').forEach(function (config) {
      overridenConfigs = overridenConfigs.concat(config.get('overrides'));
    });
    // find custom original properties that assigned to selected config group
    return overridenConfigs.concat(
      stepConfigs.filterProperty('group').filter(function (config) {
        return config.get('group.name') == configGroupName;
      })
    );
  },

  /**
   *
   * @param serviceName
   * @param configs
   * @private
   * @returns {*}
   */
  getServiceConfigToSave: function(serviceName, configs) {

    if (serviceName === 'YARN') {
      configs = App.config.textareaIntoFileConfigs(configs, 'capacity-scheduler.xml');
    }

    //generates list of properties that was changed
    var modifiedConfigs = this.getModifiedConfigs(configs);
    var serviceFileNames = Object.keys(App.StackService.find(serviceName).get('configTypes')).map(function (type) {
      return App.config.getOriginalFileName(type);
    });

    // save modified original configs that have no group and are not Undefined label
    modifiedConfigs = this.saveSiteConfigs(modifiedConfigs.filter(function (config) {
      return !config.get('group') && !config.get('isUndefinedLabel');
    }));

    if (!Em.isArray(modifiedConfigs) || modifiedConfigs.length == 0) return null;

    var fileNamesToSave = modifiedConfigs.mapProperty('filename').concat(this.get('modifiedFileNames')).filter(function(filename) {
      return serviceFileNames.contains(filename);
    }).uniq();

    var configsToSave = this.generateDesiredConfigsJSON(modifiedConfigs, fileNamesToSave, this.get('serviceConfigVersionNote'));

    if (configsToSave.length > 0) {
      return JSON.stringify({
        Clusters: {
          desired_config: configsToSave
        }
      });
    } else {
      return null;
    }
  },

  /**
   * save site configs
   * @param configs
   * @private
   * @method saveSiteConfigs
   */
  saveSiteConfigs: function (configs) {
    this.formatConfigValues(configs);
    return configs;
  },

  /**
   * Represent boolean value as string (true => 'true', false => 'false') and trim other values
   * @param serviceConfigProperties
   * @private
   * @method formatConfigValues
   */
  formatConfigValues: function (serviceConfigProperties) {
    serviceConfigProperties.forEach(function (_config) {
      if (typeof _config.get('value') === "boolean") _config.set('value', _config.value.toString());
      _config.set('value', App.config.trimProperty(_config, true));
    });
  },

  /*********************************** 3. GENERATING JSON TO SAVE *****************************/

  /**
   * Map that contains last used timestamp.
   * There is a case when two config groups can update same filename almost simultaneously
   * so they have equal timestamp and this causes collision. So to prevent this we need to check
   * if specific filename with specific timestamp is not saved yet.
   *
   * @type {Object}
   */
  _timeStamps: {},

  /**
   * generating common JSON object for desired configs
   * @param configsToSave
   * @param fileNamesToSave
   * @param {string} [serviceConfigNote='']
   * @param {boolean} [ignoreVersionNote=false]
   * @returns {Array}
   */
  generateDesiredConfigsJSON: function(configsToSave, fileNamesToSave, serviceConfigNote, ignoreVersionNote) {
    var desired_config = [];
    if (Em.isArray(configsToSave) && Em.isArray(fileNamesToSave) && fileNamesToSave.length && configsToSave.length) {
      serviceConfigNote = serviceConfigNote || "";

      fileNamesToSave.forEach(function(fName) {

        if (this.allowSaveSite(fName)) {
          var properties = configsToSave.filterProperty('filename', fName);
          var type = App.config.getConfigTagFromFileName(fName);
          desired_config.push(this.createDesiredConfig(type, properties, serviceConfigNote, ignoreVersionNote));
        }
      }, this);
    }
    return desired_config;
  },

  /**
   * For some file names we have a restriction
   * and can't save them, in this case method will return false
   *
   * @param fName
   * @returns {boolean}
   */
  allowSaveSite: function(fName) {
    switch(App.config.getConfigTagFromFileName(fName)) {
      case 'mapred-queue-acls':
        return false;
      case 'core-site':
        return this.allowSaveCoreSite();
      default:
        return true;
    }
  },

  /**
   * Defines conditions in which core-site can be saved
   *
   * @returns {boolean}
   */
  allowSaveCoreSite: function() {
    return this.get('currentServices').some(function(service) {
      return (this.get('coreSiteServiceNames').contains(service.get('serviceName'))
        || this.get('coreSiteServiceType') === service.get('serviceType'));
    }, this);
  },

  /**
   * generating common JSON object for desired config
   * @param {string} type - file name without '.xml'
   * @param {App.ConfigProperty[]} properties - array of properties from model
   * @param {string} [serviceConfigNote='']
   * @param {boolean} [ignoreVersionNote=false]
   * @returns {{type: string, tag: string, properties: {}, properties_attributes: {}|undefined, service_config_version_note: string|undefined}}
   */
  createDesiredConfig: function(type, properties, serviceConfigNote, ignoreVersionNote) {
    Em.assert('type should be defined', type);
    var desired_config = {
      "type": type,
      "properties": {}
    };
    if (!ignoreVersionNote) {
      desired_config.service_config_version_note = serviceConfigNote || "";
    }
    var attributes = { final: {}, password: {}, user: {}, group: {}, text: {}, additional_user_property: {}, not_managed_hdfs_path: {}, value_from_property_file: {} };
    if (Em.isArray(properties)) {
      properties.forEach(function(property) {

        if (Em.get(property, 'isRequiredByAgent') !== false) {
          const name = stringUtils.unicodeEscape(Em.get(property, 'name'), /[\/]/g);
          desired_config.properties[name] = this.formatValueBeforeSave(property);
          /**
           * add is final value
           */
          if (Em.get(property, 'isFinal')) {
            attributes.final[name] = "true";
          }
          if (Em.get(property,'propertyType') != null) {
            Em.get(property,'propertyType').map(function(propType) {
              attributes[propType.toLowerCase()][name] = "true";
            });
          }
        }
      }, this);
    }

    if (Object.keys(attributes.final).length || Object.keys(attributes.password).length) {
      desired_config.properties_attributes = attributes;
    }
    return desired_config;
  },

  /**
   * format value before save performs some changing of values
   * according to the rules that includes heapsizeException trimming and some custom rules
   * @param {App.ConfigProperty} property
   * @returns {string}
   */
  formatValueBeforeSave: function(property) {
    var name = Em.get(property, 'name');
    var value = Em.get(property, 'value');
    var kdcTypesMap = App.router.get('mainAdminKerberosController.kdcTypesValues');

    if (this.addM(name, value)) {
      return value += "m";
    }
    if (typeof value === "boolean") {
      return value.toString();
    }
    switch (name) {
      case 'kdc_type':
        return Em.keys(kdcTypesMap).filter(function(key) {
            return kdcTypesMap[key] === value;
        })[0];
      case 'storm.zookeeper.servers':
      case 'nimbus.seeds':
        if (Em.isArray(value)) {
          return JSON.stringify(value).replace(/"/g, "'");
        } else {
          return value;
        }
        break;
      default:
        return App.config.trimProperty(property);
    }
  },

  /**
   * Site object name follow the format *permsize/*heapsize and the value NOT ends with "m"
   *
   * @param name
   * @param value
   * @returns {*|boolean}
   */
  addM: function (name, value) {
    return this.get('heapsizeRegExp').test(name)
      && !this.get('heapsizeException').contains(name)
      && !(value).endsWith("m");
  },

  /*********************************** 4. AJAX REQUESTS **************************************/

  /**
   * save config group
   * @param overriddenConfigs
   * @param selectedConfigGroup
   * @param configVersionNote
   * @param successCallback
   */
  saveGroup: function(overriddenConfigs, selectedConfigGroup, configVersionNote, successCallback) {
    var fileNamesToSave = overriddenConfigs.mapProperty('filename').uniq();
    var group = Ember.typeOf(selectedConfigGroup) === "instance" ? selectedConfigGroup.toJSON() : selectedConfigGroup;
    var groupHosts = group.hosts.map(function (hostName) { return { "host_name": hostName }; });

    var groupData = {
      ConfigGroup: {
        "cluster_name": App.get('clusterName') || this.get('clusterName'),
        "group_name": group.name,
        "tag": group.service_id,
        "service_name": group.service_id,
        "description": group.description,
        "hosts": groupHosts,
        "service_config_version_note": configVersionNote || "",
        "desired_configs": this.generateDesiredConfigsJSON(overriddenConfigs, fileNamesToSave, null, true)
      }
    };

    if (group.is_temporary) {
      this.createConfigGroup(groupData, successCallback);
    } else {
      groupData.ConfigGroup.id = group.id;
      this.updateConfigGroup(groupData, successCallback);
    }
  },

  /**
   *
   * @param data
   * @param successCallback
   * @returns {*|$.ajax}
   */
  createConfigGroup: function(data, successCallback) {
    var ajaxOptions = {
      name: 'wizard.step8.apply_configuration_groups',
      sender: this,
      data: {
        data: JSON.stringify(data)
      }
    };
    if (successCallback) {
      ajaxOptions.success = successCallback;
    }
    return App.ajax.send(ajaxOptions);
  },

  /**
   * persist properties of config groups to server
   * show result popup if <code>showPopup</code> is true
   * @param data {Object}
   * @param [successCallback] {String}
   * @method putConfigGroupChanges
   */
  updateConfigGroup: function (data, successCallback) {
    var ajaxOptions = {
      name: 'config_groups.update_config_group',
      sender: this,
      data: {
        id: data.ConfigGroup.id,
        configGroup: data
      }
    };
    if (successCallback) {
      ajaxOptions.success = successCallback;
    }
    return App.ajax.send(ajaxOptions);
  },

  /**
   * Saves configuration of set of sites. The provided data
   * contains the site name and tag to be used.
   * @param {Object[]} services
   * @param {String} [successCallback]
   * @param {Function} [alwaysCallback]
   * @return {$.ajax}
   * @method putChangedConfigurations
   */
  putChangedConfigurations: function (services, successCallback, alwaysCallback) {
    var ajaxData = {
      name: 'common.across.services.configurations',
      sender: this,
      data: {
        data: '[' + services.toString() + ']'
      },
      error: 'doPUTClusterConfigurationSiteErrorCallback'
    };
    if (successCallback) {
      ajaxData.success = successCallback;
    }
    if (alwaysCallback) {
      ajaxData.callback = alwaysCallback;
    }
    return App.ajax.send(ajaxData);
  },

  /*********************************** 5. AFTER SAVE INFO ************************************/

  /**
   * @private
   * @method putConfigGroupChangesSuccess
   */
  putConfigGroupChangesSuccess: function () {
    this.set('saveConfigsFlag', true);
    this.onDoPUTClusterConfigurations();
  },

  /**
   * @private
   * @method doPUTClusterConfigurationSiteSuccessCallback
   */
  doPUTClusterConfigurationSiteSuccessCallback: function () {
    var doConfigActions = true;
    this.onDoPUTClusterConfigurations(doConfigActions);
  },

  /**
   * @private
   * @method doPUTClusterConfigurationSiteErrorCallback
   */
  doPUTClusterConfigurationSiteErrorCallback: function () {
    this.set('saveConfigsFlag', false);
    this.onDoPUTClusterConfigurations();
  },

  /**
   * On save configs handler. Open save configs popup with appropriate message
   * and clear config dependencies list.
   * @param  {Boolean} doConfigActions
   * @private
   * @method onDoPUTClusterConfigurations
   */
  onDoPUTClusterConfigurations: function (doConfigActions) {
    var status = 'unknown',
      result = {
        flag: this.get('saveConfigsFlag'),
        message: null,
        value: null
      },
      extendedModel = App.Service.extendedModel[this.get('content.serviceName')],
      currentService = extendedModel ? App[extendedModel].find(this.get('content.serviceName')) : App.Service.find(this.get('content.serviceName'));

    if (!result.flag) {
      result.message = Em.I18n.t('services.service.config.failSaveConfig');
    }

    App.router.get('clusterController').updateClusterData();
    var popupOptions = this.getSaveConfigsPopupOptions(result);
    if (currentService) {
      App.router.get('clusterController').triggerQuickLinksUpdate();
    }

    //  update configs for service actions
    App.router.get('mainServiceItemController').loadConfigs();

    this.showSaveConfigsPopup(
      popupOptions.header,
      result.flag,
      popupOptions.message,
      popupOptions.messageClass,
      popupOptions.value,
      status,
      popupOptions.urlParams,
      doConfigActions);
    this.clearAllRecommendations();
  },

  /**
   *
   * @param {object} result
   * @returns {object}
   */
  getSaveConfigsPopupOptions: function(result) {
    var options;
    if (result.flag === true) {
      options = {
        header: Em.I18n.t('services.service.config.saved'),
        message: Em.I18n.t('services.service.config.saved.message'),
        messageClass: 'alert alert-success',
        urlParams: ',ServiceComponentInfo/installed_count,ServiceComponentInfo/total_count'
      };

      if (this.get('content.serviceName') === 'HDFS') {
        options.urlParams += '&ServiceComponentInfo/service_name.in(HDFS)'
      }
    } else {
      options = {
        urlParams: '',
        header: Em.I18n.t('common.failure'),
        message: result.message,
        messageClass: 'alert alert-error',
        value: result.value
      }
    }
    return options;
  },

  /**
   * Show save configs popup
   * @return {App.ModalPopup}
   * @private
   * @method showSaveConfigsPopup
   */
  showSaveConfigsPopup: function (header, flag, message, messageClass, value, status, urlParams, doConfigActions) {
    var self = this;
    return App.ModalPopup.show({
      header: header,
      primary: Em.I18n.t('ok'),
      secondary: null,
      isBackgroundPopupToBeShown: false,

      onPrimary: function () {
        this.hide();
        if (!flag) {
          self.completeSave();
        }
        this.showBackgroundPopup();
      },
      onClose: function () {
        this.hide();
        self.completeSave();
        this.showBackgroundPopup();
      },
      showBackgroundPopup: function() {
        if (this.get('isBackgroundPopupToBeShown')) {
          App.router.get('backgroundOperationsController').showPopup();
        }
      },
      disablePrimary: true,
      bodyClass: Ember.View.extend({
        flag: flag,
        message: function () {
          return this.get('isLoaded') ? message : Em.I18n.t('services.service.config.saving.message');
        }.property('isLoaded'),
        messageClass: function () {
          return this.get('isLoaded') ? messageClass : 'alert alert-info';
        }.property('isLoaded'),
        setDisablePrimary: function () {
          this.get('parentView').set('disablePrimary', !this.get('isLoaded'));
        }.observes('isLoaded'),
        runningHosts: [],
        runningComponentCount: 0,
        unknownHosts: [],
        unknownComponentCount: 0,
        siteProperties: value,
        isLoaded: false,
        componentsFilterSuccessCallback: function (response) {
          var count = 0,
            view = this,
            lazyLoadHosts = function (dest) {
              lazyLoading.run({
                initSize: 20,
                chunkSize: 50,
                delay: 50,
                destination: dest,
                source: hosts,
                context: view
              });
            },
            /**
             * Map components for their hosts
             * Return format:
             * <code>
             *   {
             *    host1: [component1, component2, ...],
             *    host2: [component3, component4, ...]
             *   }
             * </code>
             * @return {object}
             */
              setComponents = function (item, components) {
              item.host_components.forEach(function (c) {
                var name = c.HostRoles.host_name;
                if (!components[name]) {
                  components[name] = [];
                }
                components[name].push(App.format.role(item.ServiceComponentInfo.component_name, false));
              });
              return components;
            },
            /**
             * Map result of <code>setComponents</code> to array
             * @return {{name: string, components: string}[]}
             */
              setHosts = function (components) {
              var hosts = [];
              Em.keys(components).forEach(function (key) {
                hosts.push({
                  name: key,
                  components: components[key].join(', ')
                });
              });
              return hosts;
            },
            components = {},
            hosts = [];
          switch (status) {
            case 'unknown':
              response.items.filter(function (item) {
                return (item.ServiceComponentInfo.total_count > (item.ServiceComponentInfo.started_count + item.ServiceComponentInfo.installed_count));
              }).forEach(function (item) {
                var total = item.ServiceComponentInfo.total_count,
                  started = item.ServiceComponentInfo.started_count,
                  installed = item.ServiceComponentInfo.installed_count,
                  unknown = total - (started + installed);
                components = setComponents(item, components);
                count += unknown;
              });
              hosts = setHosts(components);
              this.set('unknownComponentCount', count);
              lazyLoadHosts(this.get('unknownHosts'));
              break;
            case 'started':
              response.items.filterProperty('ServiceComponentInfo.started_count').forEach(function (item) {
                var started = item.ServiceComponentInfo.started_count;
                components = setComponents(item, components);
                count += started;
                hosts = setHosts(components);
              });
              this.set('runningComponentCount', count);
              lazyLoadHosts(this.get('runningHosts'));
              break;
          }
        },
        componentsFilterErrorCallback: function () {
          this.set('isLoaded', true);
        },
        didInsertElement: function () {
          var context = this;
          var dfd = App.ajax.send({
            name: 'components.filter_by_status',
            sender: this,
            data: {
              clusterName: App.get('clusterName'),
              urlParams: urlParams
            },
            success: 'componentsFilterSuccessCallback',
            error: 'componentsFilterErrorCallback'
          });

          dfd.done(function() {
            if (doConfigActions && self.doConfigActions) {
              self.doConfigActions.bind(self)();
              var isBackgroundPopupToBeShown = self.isComponentActionsPresent.bind(self)();
              context.set('parentView.isBackgroundPopupToBeShown',isBackgroundPopupToBeShown);
            }
            if (flag) {
              self.loadStep();
            }
          });
        },
        getDisplayMessage: function () {
          var displayMsg = [];
          var siteProperties = this.get('siteProperties');
          if (siteProperties) {
            siteProperties.forEach(function (_siteProperty) {
              var displayProperty = _siteProperty.siteProperty;
              var displayNames = _siteProperty.displayNames;

              if (displayNames && displayNames.length) {
                if (displayNames.length === 1) {
                  displayMsg.push(displayProperty + Em.I18n.t('as') + displayNames[0]);
                } else {
                  var name;
                  displayNames.forEach(function (_name, index) {
                    if (index === 0) {
                      name = _name;
                    } else if (index === siteProperties.length - 1) {
                      name = name + Em.I18n.t('and') + _name;
                    } else {
                      name = name + ', ' + _name;
                    }
                  }, this);
                  displayMsg.push(displayProperty + Em.I18n.t('as') + name);

                }
              } else {
                displayMsg.push(displayProperty);
              }
            }, this);
          }
          return displayMsg;

        }.property('siteProperties'),

        runningHostsMessage: Em.computed.i18nFormat('services.service.config.stopService.runningHostComponents', 'runningComponentCount', 'runningHosts.length'),

        unknownHostsMessage: Em.computed.i18nFormat('services.service.config.stopService.unknownHostComponents', 'unknownComponentCount', 'unknownHosts.length'),

        templateName: require('templates/main/service/info/configs_save_popup')
      })
    })
  },

  /*********************************** 6. ADDITIONAL *******************************************/

  /**
   * If some configs are changed and user navigates away or select another config-group, show this popup with propose to save changes
   * @param {String} path
   * @param {object} callback - callback with action to change configs view(change group or version)
   * @return {App.ModalPopup}
   * @method showSavePopup
   */
  showSavePopup: function (transitionCallback, callback) {
    var self = this;
    var passwordWasChanged = this.get('passwordConfigsAreChanged');
    return App.ModalPopup.show({
      header: Em.I18n.t('common.warning'),
      bodyClass: Em.View.extend({
        templateName: require('templates/common/configs/save_configuration'),
        classNames: ['col-md-12'],
        showSaveWarning: true,
        showPasswordChangeWarning: passwordWasChanged,
        notesArea: Em.TextArea.extend({
          value: passwordWasChanged ? Em.I18n.t('dashboard.configHistory.info-bar.save.popup.notesForPasswordChange') : '',
          classNames: ['full-width'],
          placeholder: Em.I18n.t('dashboard.configHistory.info-bar.save.popup.placeholder'),
          didInsertElement: function () {
            if (this.get('value')) {
              this.onChangeValue();
            }
          },
          onChangeValue: function() {
            this.get('parentView.parentView').set('serviceConfigNote', this.get('value'));
          }.observes('value')
        })
      }),
      footerClass: Em.View.extend({
        templateName: require('templates/main/service/info/save_popup_footer'),
        isSaveDisabled: function() {
          return self.get('isSubmitDisabled');
        }.property()
      }),
      primary: Em.I18n.t('common.save'),
      secondary: Em.I18n.t('common.cancel'),
      onSave: function () {
        self.set('serviceConfigVersionNote', this.get('serviceConfigNote'));
        self.saveStepConfigs();
        this.hide();
      },
      onDiscard: function () {
        self.set('preSelectedConfigVersion', null);
        if (transitionCallback) {
          transitionCallback();
        } else if (callback) {
          self.doCancel();
          // Prevent multiple popups
          self.set('hash', self.getHash());
          callback();
        }
        this.hide();
      },
      onCancel: function () {
        this.hide();
      }
    });
  }
});
