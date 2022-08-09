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

/**
 * @typedef {object} nnHaConfigDependencies
 * @property {string} namespaceId
 * @property {object} serverConfigs
 * @property {string|number} nnHttpPort
 * @property {string|number} nnHttpsPort
 * @property {string|number} nnRpcPort
 * @property {string|number} zkClientPort
 */

var App = require('app');

require('utils/configs/nn_ha_config_initializer');

App.ManageJournalNodeWizardStep2Controller = Em.Controller.extend({
  name: "manageJournalNodeWizardStep2Controller",
  selectedService: null,
  stepConfigs: [],
  serverConfigData: {},
  moveJNConfig: {
    serviceName: 'MISC',
    displayName: 'MISC',
    configCategories: [
      App.ServiceConfigCategory.create({name: 'HDFS', displayName: 'HDFS'})
    ],
    sites: ['hdfs-site'],
    configs: []
  },
  once: false,
  isLoaded: false,
  versionLoaded: true,
  hideDependenciesInfoBar: true,

  isNextDisabled: Em.computed.not('isLoaded'),

  clearStep: function () {
    this.get('stepConfigs').clear();
    this.set('serverConfigData', {});
  },

  loadStep: function () {
    this.clearStep();
    this.loadConfigsTags();
  },

  loadConfigsTags: function () {
    App.ajax.send({
      name: 'config.tags',
      sender: this,
      success: 'onLoadConfigsTags',
      error: 'onTaskError'
    });
  },


  onLoadConfigsTags: function (data) {
    var urlParams = [];
    var hdfsSiteTag = data.Clusters.desired_configs['hdfs-site'].tag;
    urlParams.push('(type=hdfs-site&tag=' + hdfsSiteTag + ')');
    this.set("hdfsSiteTag", {name: "hdfsSiteTag", value: hdfsSiteTag});

    App.ajax.send({
      name: 'admin.get.all_configurations',
      sender: this,
      data: {
        urlParams: urlParams.join('|')
      },
      success: 'onLoadConfigs',
      error: 'onTaskError'
    });
  },

  onLoadConfigs: function (data) {
    this.set('serverConfigData', data);
    this.set('content.nameServiceId', data.items[0].properties['dfs.nameservices']);
    this.tweakServiceConfigs();
    this.renderServiceConfigs();
    this.set('isLoaded', true);
  },

  /**
   * Generate set of data used to correctly initialize config values and names
   */
  _prepareDependencies: function () {
    var ret = {};
    var configsFromServer = this.get('serverConfigData.items');
    ret.namespaceId = this.get('content.nameServiceId');
    ret.serverConfigs = configsFromServer;
    return ret;
  },

  /**
   * Generate set of data with information about cluster topology
   * Used in the configs' initialization process
   *
   * @returns {extendedTopologyLocalDB}
   * @private
   * @method _prepareLocalDB
   */
  _prepareLocalDB: function () {
    var localDB = this.get('content').getProperties(['masterComponentHosts', 'slaveComponentHosts', 'hosts']);
    localDB.installedServices = App.Service.find().mapProperty('serviceName');
    return localDB;
  },

  tweakServiceConfigs: function () {
    var localDB = this._prepareLocalDB();
    var dependencies = this._prepareDependencies();
    if (App.get('hasNameNodeFederation')) {
      this.setNameSpaceConfigs();
    } else {
      this.get('moveJNConfig').configs.pushObject({
        "name": "dfs.namenode.shared.edits.dir",
        "displayName": "dfs.namenode.shared.edits.dir",
        "description": " The URI which identifies the group of JNs where the NameNodes will write/read edits.",
        "isReconfigurable": false,
        "recommendedValue": "qjournal://node1.example.com:8485;node2.example.com:8485;node3.example.com:8485/mycluster",
        "value": "qjournal://node1.example.com:8485;node2.example.com:8485;node3.example.com:8485/mycluster",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      });
      this.get('moveJNConfig.configs').forEach(function (config) {
        App.NnHaConfigInitializer.initialValue(config, localDB, dependencies);
        config.isOverridable = false;
      });
    }
  },

  setNameSpaceConfigs: function () {
    const namespaces = this.get('content.nameServiceId').split(',');
    const namespaceConfigValue = this.get('content.masterComponentHosts').filterProperty('component', 'JOURNALNODE').map(function (node) {
      return node.hostName + ':8485'
    }).join(';');
    namespaces.forEach((namespace) => {
      this.get('moveJNConfig.configs').pushObject({
        "name": "dfs.namenode.shared.edits.dir." + namespace,
        "displayName": "dfs.namenode.shared.edits.dir." + namespace,
        "isReconfigurable": false,
        "recommendedValue": "qjournal://" + namespaceConfigValue + '/' + namespace,
        "value": "qjournal://" + namespaceConfigValue + '/' + namespace,
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      });
    });
  },

  renderServiceConfigs: function () {
    var _serviceConfig = this.get('moveJNConfig');
    var serviceConfig = App.ServiceConfig.create({
      serviceName: _serviceConfig.serviceName,
      displayName: _serviceConfig.displayName,
      configCategories: [],
      showConfig: true,
      configs: []
    });

    _serviceConfig.configCategories.forEach(function (_configCategory) {
      if (App.Service.find().someProperty('serviceName', _configCategory.name)) {
        serviceConfig.configCategories.pushObject(_configCategory);
      }
    }, this);

    this.loadComponentConfigs(_serviceConfig, serviceConfig);

    this.get('stepConfigs').pushObject(serviceConfig);
    this.set('selectedService', this.get('stepConfigs').objectAt(0));
    this.set('once', true);
  },

  /**
   * Load child components to service config object
   * @param _componentConfig
   * @param componentConfig
   */
  loadComponentConfigs: function (_componentConfig, componentConfig) {
    _componentConfig.configs.forEach(function (_serviceConfigProperty) {
      var serviceConfigProperty = App.ServiceConfigProperty.create(_serviceConfigProperty);
      componentConfig.configs.pushObject(serviceConfigProperty);
      serviceConfigProperty.set('isEditable', serviceConfigProperty.get('isReconfigurable'));
    }, this);
  }
});
