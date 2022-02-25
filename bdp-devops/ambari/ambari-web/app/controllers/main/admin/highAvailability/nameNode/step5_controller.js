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

App.HighAvailabilityWizardStep5Controller = App.HighAvailabilityProgressPageController.extend({

  name:"highAvailabilityWizardStep5Controller",

  commands: ['stopAllServices', 'installNameNode', 'installJournalNodes', 'reconfigureHDFS', 'startJournalNodes', 'disableSNameNode'],

  hdfsSiteTag : "",
  coreSiteTag : "",

  stopAllServices: function () {
    this.stopServices([], true, true);
  },

  installNameNode: function () {
    var hostName = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').findProperty('isInstalled', false).hostName;
    this.createInstallComponentTask('NAMENODE', hostName, "HDFS");
  },

  installJournalNodes: function () {
    var hostNames = this.get('content.masterComponentHosts').filterProperty('component', 'JOURNALNODE').mapProperty('hostName');
    this.createInstallComponentTask('JOURNALNODE', hostNames, "HDFS");
  },

  startJournalNodes: function () {
    var hostNames = this.get('content.masterComponentHosts').filterProperty('component', 'JOURNALNODE').mapProperty('hostName');
    this.updateComponent('JOURNALNODE', hostNames, "HDFS", "Start");
  },

  disableSNameNode: function () {
    var hostName = this.get('content.masterComponentHosts').findProperty('component', 'SECONDARY_NAMENODE').hostName;
    return App.ajax.send({
      name: 'common.host.host_component.passive',
      sender: this,
      data: {
        hostName: hostName,
        passive_state: "ON",
        componentName: 'SECONDARY_NAMENODE'
      },
      success: 'onTaskCompleted',
      error: 'onTaskError'
    });
  },

  reconfigureHDFS: function () {
    var data = this.get('content.serviceConfigProperties');
    if (App.get('isKerberosEnabled')) {
      this.reconfigureSecureHDFS();
    } else {
      this.updateConfigProperties(data);
    }
  },

  /**
   * Update service configurations
   * @param {Object} data - config object to update
   */
  updateConfigProperties: function(data) {
    var siteNames = ['hdfs-site', 'core-site'].concat(this.getRangerSiteNames(data));
    var note = Em.I18n.t('admin.highAvailability.step4.save.configuration.note').format(App.format.role('NAMENODE', false));
    var configData = this.reconfigureSites(siteNames, data, note);
    return App.ajax.send({
      name: 'common.service.configurations',
      sender: this,
      data: {
        desired_config: configData
      },
      error: 'onTaskError',
      success: 'installHDFSClients'
    });
  },

  getRangerSiteNames: function(data) {
    var siteNames = [];
    if (App.Service.find().someProperty('serviceName', 'RANGER')) {
      var hdfsPluginConfig = data.items.findProperty('type', 'ranger-hdfs-plugin-properties');
      if (hdfsPluginConfig) {
        if ('xasecure.audit.destination.hdfs.dir' in hdfsPluginConfig.properties) {
          siteNames.push('ranger-hdfs-plugin-properties');
        }
      }
      var hdfsAuditConfig = data.items.findProperty('type', 'ranger-hdfs-audit');
      if (hdfsAuditConfig) {
        if ('xasecure.audit.destination.hdfs.dir' in hdfsAuditConfig.properties) {
          siteNames.push('ranger-hdfs-audit');
        }
      }
    }
    return siteNames;
  },

  installHDFSClients: function () {
    var nnHostNames = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').mapProperty('hostName');
    var jnHostNames = this.get('content.masterComponentHosts').filterProperty('component', 'JOURNALNODE').mapProperty('hostName');
    var hostNames = nnHostNames.concat(jnHostNames).uniq();
    this.createInstallComponentTask('HDFS_CLIENT', hostNames, 'HDFS');
    App.router.get(this.get('content.controllerName')).saveHdfsClientHosts(hostNames);
    App.clusterStatus.setClusterStatus({
      clusterName: this.get('content.cluster.name'),
      clusterState: 'HIGH_AVAILABILITY_DEPLOY',
      wizardControllerName: this.get('content.controllerName'),
      localdb: App.db.data
    });
  },

  /**
   * Process configurations for hdfs on kerberized cluster.
   * Secure properties will be applied to hdfs and core site after installing JournalNode.
   * For this case we need to pull updated configurations from API, merge them with configurations created
   * during `Review` step and store new configurations.
   */
  reconfigureSecureHDFS: function() {
    App.router.get('highAvailabilityWizardStep3Controller').loadConfigsTags.call(this);
  },

  onLoadConfigsTags: function() {
    App.router.get('highAvailabilityWizardStep3Controller').onLoadConfigsTags.apply(this, [].slice.call(arguments));
  },

  onLoadConfigs: function(data) {
    var self = this;
    var configController = App.router.get('highAvailabilityWizardStep3Controller');
    var configItems = data.items.map(function(item) {
      var fileName = Em.get(item, 'type');
      var configTypeObject = self.get('content.serviceConfigProperties').items.findProperty('type', fileName);
      if (configTypeObject) {
        $.extend(item.properties, configTypeObject.properties);
      }
      return item;
    });
    configItems = configController.removeConfigs(configController.get('configsToRemove'), {items: configItems});
    this.updateConfigProperties(configItems);
  }
});
