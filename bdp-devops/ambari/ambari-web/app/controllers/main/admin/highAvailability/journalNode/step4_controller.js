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

App.ManageJournalNodeWizardStep4Controller = App.ManageJournalNodeProgressPageController.extend({
  name: 'manageJournalNodeWizardStep4Controller',
  clusterDeployState: 'JOURNALNODE_MANAGEMENT',
  tasksMessagesPrefix: 'admin.manageJournalNode.wizard.step',

  commands: ['stopStandbyNameNode', 'stopAllServices', 'installJournalNodes', 'deleteJournalNodes', 'reconfigureHDFS'],

  hdfsSiteTag: "",

  stopStandbyNameNode: function () {
    // save who's active and who's standby at this point in time
    var hostName = this.get('content.standByNN.host_name');
    this.updateComponent('NAMENODE', hostName, 'HDFS',  'INSTALLED');
  },

  stopAllServices: function () {
    this.stopServices([], true, true);
  },

  installJournalNodes: function () {
    var hostNames = App.router.get('manageJournalNodeWizardController').getJournalNodesToAdd();
    if (hostNames && hostNames.length > 0) {
      this.createInstallComponentTask('JOURNALNODE', hostNames, "HDFS");
    } else {
      this.onTaskCompleted();
    }
  },

  deleteJournalNodes: function () {
    var hosts = App.router.get('manageJournalNodeWizardController').getJournalNodesToDelete();
    if (hosts && hosts.length > 0) {
      hosts.forEach(function (host) {
        this.deleteComponent('JOURNALNODE', host);
      }, this);
    } else {
      this.onTaskCompleted();
    }
  },

  reconfigureHDFS: function () {
    this.updateConfigProperties(this.get('content.serviceConfigProperties'));
  },

  /**
   * Update service configurations
   * @param {Object} data - config object to update
   */
  updateConfigProperties: function (data) {
    var siteNames = ['hdfs-site'];
    var configData = this.reconfigureSites(siteNames, data, Em.I18n.t('admin.manageJournalNode.step4.save.configuration.note').format(App.format.role('NAMENODE', false)));
    App.ajax.send({
      name: 'common.service.configurations',
      sender: this,
      data: {
        desired_config: configData
      },
      success: 'installHDFSClients',
      error: 'onTaskError'
    });
  },

  installHDFSClients: function () {
    var nnHostNames = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').mapProperty('hostName');
    var jnHostNames = this.get('content.masterComponentHosts').filterProperty('component', 'JOURNALNODE').mapProperty('hostName');
    var hostNames = nnHostNames.concat(jnHostNames).uniq();
    this.createInstallComponentTask('HDFS_CLIENT', hostNames, 'HDFS');
    App.clusterStatus.setClusterStatus({
      clusterName: this.get('content.cluster.name'),
      clusterState: 'JOURNALNODE_MANAGEMENT',
      wizardControllerName: this.get('content.controllerName'),
      localdb: App.db.data
    });
  }

});