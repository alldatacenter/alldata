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

App.HighAvailabilityWizardStep9Controller = App.HighAvailabilityProgressPageController.extend(App.WizardEnableDone, {

  name:"highAvailabilityWizardStep9Controller",

  commands: ['startSecondNameNode', 'installZKFC', 'startZKFC', 'installPXF', 'reconfigureRanger', 'reconfigureHBase', 'reconfigureAMS', 'reconfigureAccumulo', 'reconfigureHawq', 'deleteSNameNode', 'stopHDFS', 'startAllServices'],

  hbaseSiteTag: "",
  accumuloSiteTag: "",
  hawqSiteTag: "",
  secondNameNodeHost: "",

  initializeTasks: function () {
    this._super();
    var tasksToRemove = [];

    // find hostname where second namenode will be installed
    this.set('secondNameNodeHost', this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').findProperty('isInstalled', false).hostName);

    if (!App.Service.find().someProperty('serviceName', 'PXF') || this.isPxfComponentInstalled()) {
      tasksToRemove.push('installPXF');
    }
    if (!App.Service.find().someProperty('serviceName', 'RANGER')) {
      tasksToRemove.push('reconfigureRanger');
    }
    if (!App.Service.find().someProperty('serviceName', 'HBASE')) {
      tasksToRemove.push('reconfigureHBase');
    }
    if (!App.Service.find().someProperty('serviceName', 'AMBARI_METRICS')) {
      tasksToRemove.push('reconfigureAMS');
    }
    if (!App.Service.find().someProperty('serviceName', 'ACCUMULO')) {
      tasksToRemove.push('reconfigureAccumulo');
    }
    if (!App.Service.find().someProperty('serviceName', 'HAWQ')) {
      tasksToRemove.push('reconfigureHawq');
    }
    this.removeTasks(tasksToRemove);
  },

  startSecondNameNode: function () {
    var hostName = this.get('secondNameNodeHost');
    this.updateComponent('NAMENODE', hostName, "HDFS", "Start");
  },

  installZKFC: function () {
    var hostNames = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').mapProperty('hostName');
    this.createInstallComponentTask('ZKFC', hostNames, "HDFS");
  },

  startZKFC: function () {
    var hostNames = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').mapProperty('hostName');
    this.updateComponent('ZKFC', hostNames, "HDFS", "Start");
  },

  isPxfComponentInstalled: function () {
    var pxfComponent = this.getSlaveComponentHosts().findProperty('componentName', 'PXF');

    if (pxfComponent !== undefined) {
      var host;
      // check if PXF is already installed on the host assigned for additional NameNode
      for (var i = 0; i < pxfComponent.hosts.length; i++) {
        host = pxfComponent.hosts[i];
        if (host.hostName === this.get('secondNameNodeHost'))
          return true;
      }
    }

    return false;
  },

  installPXF: function () {
    this.createInstallComponentTask('PXF', this.get('secondNameNodeHost'), "PXF");
  },

  reconfigureRanger: function () {
    var data = this.get('content.serviceConfigProperties');
    var siteNames = ['ranger-env'];
    var configs = [];
    configs.push({
      Clusters: {
        desired_config: this.reconfigureSites(siteNames, data, Em.I18n.t('admin.highAvailability.step4.save.configuration.note').format(App.format.role('NAMENODE', false)))
      }
    });
    if (App.Service.find().someProperty('serviceName', 'YARN')) {
      siteNames = [];
      var yarnAuditConfig = data.items.findProperty('type', 'ranger-yarn-audit');
      if (yarnAuditConfig) {
        if ('xasecure.audit.destination.hdfs.dir' in yarnAuditConfig.properties) {
          siteNames.push('ranger-yarn-audit');
          configs.push({
            Clusters: {
              desired_config: this.reconfigureSites(siteNames, data, Em.I18n.t('admin.highAvailability.step4.save.configuration.note').format(App.format.role('NAMENODE', false)))
            }
          });
        }
      }
    }
    if (App.Service.find().someProperty('serviceName', 'STORM')) {
      siteNames = [];
      var stormPluginConfig = data.items.findProperty('type', 'ranger-storm-plugin-properties');
      if (stormPluginConfig) {
        if ('xasecure.audit.destination.hdfs.dir' in stormPluginConfig.properties) {
          siteNames.push('ranger-storm-plugin-properties');
        }
      }
      var stormAuditConfig = data.items.findProperty('type', 'ranger-storm-audit');
      if (stormAuditConfig) {
        if ('xasecure.audit.destination.hdfs.dir' in stormAuditConfig.properties) {
          siteNames.push('ranger-storm-audit');
        }
      }
      if (siteNames.length) {
        configs.push({
          Clusters: {
            desired_config: this.reconfigureSites(siteNames, data, Em.I18n.t('admin.highAvailability.step4.save.configuration.note').format(App.format.role('NAMENODE', false)))
          }
        });
      }
    }
    if (App.Service.find().someProperty('serviceName', 'KAFKA')) {
      siteNames = [];
      var kafkaAuditConfig = data.items.findProperty('type', 'ranger-kafka-audit');
      if (kafkaAuditConfig) {
        if ('xasecure.audit.destination.hdfs.dir' in kafkaAuditConfig.properties) {
          siteNames.push('ranger-kafka-audit');
          configs.push({
            Clusters: {
              desired_config: this.reconfigureSites(siteNames, data, Em.I18n.t('admin.highAvailability.step4.save.configuration.note').format(App.format.role('NAMENODE', false)))
            }
          });
        }
      }
    }
    if (App.Service.find().someProperty('serviceName', 'KNOX')) {
      siteNames = [];
      var knoxPluginConfig = data.items.findProperty('type', 'ranger-knox-plugin-properties');
      if (knoxPluginConfig) {
        if ('xasecure.audit.destination.hdfs.dir' in knoxPluginConfig.properties) {
          siteNames.push('ranger-knox-plugin-properties');
        }
      }
      var knoxAuditConfig = data.items.findProperty('type', 'ranger-knox-audit');
      if (knoxAuditConfig) {
        if ('xasecure.audit.destination.hdfs.dir' in knoxAuditConfig.properties) {
          siteNames.push('ranger-knox-audit');
        }
      }
      if(siteNames.length) {
        configs.push({
          Clusters: {
            desired_config: this.reconfigureSites(siteNames, data, Em.I18n.t('admin.highAvailability.step4.save.configuration.note').format(App.format.role('NAMENODE', false)))
          }
        });
      }
    }
    if (App.Service.find().someProperty('serviceName', 'ATLAS')) {
      siteNames = [];
      var atlasAuditConfig = data.items.findProperty('type', 'ranger-atlas-audit');
      if (atlasAuditConfig) {
        if ('xasecure.audit.destination.hdfs.dir' in atlasAuditConfig.properties) {
          siteNames.push('ranger-atlas-audit');
          configs.push({
            Clusters: {
              desired_config: this.reconfigureSites(siteNames, data, Em.I18n.t('admin.highAvailability.step4.save.configuration.note').format(App.format.role('NAMENODE', false)))
            }
          });
        }
      }
    }
    if (App.Service.find().someProperty('serviceName', 'HIVE')) {
      siteNames = [];
      var hivePluginConfig = data.items.findProperty('type', 'ranger-hive-plugin-properties');
      if (hivePluginConfig) {
        if ('xasecure.audit.destination.hdfs.dir' in hivePluginConfig.properties) {
          siteNames.push('ranger-hive-plugin-properties');
        }
      }
      var hiveAuditConfig = data.items.findProperty('type', 'ranger-hive-audit');
      if (hiveAuditConfig) {
        if ('xasecure.audit.destination.hdfs.dir' in hiveAuditConfig.properties) {
          siteNames.push('ranger-hive-audit');
        }
      }
      if(siteNames.length) {
        configs.push({
          Clusters: {
            desired_config: this.reconfigureSites(siteNames, data, Em.I18n.t('admin.highAvailability.step4.save.configuration.note').format(App.format.role('NAMENODE', false)))
          }
        });
      }
    }
    if (App.Service.find().someProperty('serviceName', 'RANGER_KMS')) {
      siteNames = [];
      var rangerKMSConfig = data.items.findProperty('type', 'ranger-kms-audit');
      if (rangerKMSConfig) {
        if ('xasecure.audit.destination.hdfs.dir' in rangerKMSConfig.properties) {
          siteNames.push('ranger-kms-audit');
          configs.push({
            Clusters: {
              desired_config: this.reconfigureSites(siteNames, data, Em.I18n.t('admin.highAvailability.step4.save.configuration.note').format(App.format.role('NAMENODE', false)))
            }
          });
        }
      }
    }

    App.ajax.send({
      name: 'common.service.multiConfigurations',
      sender: this,
      data: {
        configs: configs
      },
      success: 'saveConfigTag',
      error: 'onTaskError'
    });
  },

  /**
   *
   * @param {array} siteNames
   * @param {object} data
   */
  saveReconfiguredConfigs: function(siteNames, data) {
    var note = Em.I18n.t('admin.highAvailability.step4.save.configuration.note').format(App.format.role('NAMENODE', false));
    var configData = this.reconfigureSites(siteNames, data, note);
    return App.ajax.send({
      name: 'common.service.configurations',
      sender: this,
      data: {
        desired_config: configData
      },
      success: 'saveConfigTag',
      error: 'onTaskError'
    });
  },

  reconfigureHBase: function () {
    var data = this.get('content.serviceConfigProperties');
    var siteNames = ['hbase-site'];
    if (App.Service.find().someProperty('serviceName', 'RANGER')) {
      var hbasePluginConfig = data.items.findProperty('type', 'ranger-hbase-plugin-properties');
      if (hbasePluginConfig) {
        if ('xasecure.audit.destination.hdfs.dir' in hbasePluginConfig.properties) {
          siteNames.push('ranger-hbase-plugin-properties');
        }
      }
      var hbaseAuditConfig = data.items.findProperty('type', 'ranger-hbase-audit');
      if (hbaseAuditConfig) {
        if ('xasecure.audit.destination.hdfs.dir' in hbaseAuditConfig.properties) {
          siteNames.push('ranger-hbase-audit');
        }
      }
    }
    this.saveReconfiguredConfigs(siteNames, data);
  },

  reconfigureAMS: function () {
    this.saveReconfiguredConfigs(['ams-hbase-site'], this.get('content.serviceConfigProperties'));
  },

  reconfigureAccumulo: function () {
    this.saveReconfiguredConfigs(['accumulo-site'], this.get('content.serviceConfigProperties'));
  },

  reconfigureHawq: function () {
    this.saveReconfiguredConfigs(['hawq-site', 'hdfs-client'], this.get('content.serviceConfigProperties'));
  },

  saveConfigTag: function () {
    App.clusterStatus.setClusterStatus({
      clusterName: this.get('content.cluster.name'),
      clusterState: 'HIGH_AVAILABILITY_DEPLOY',
      wizardControllerName: this.get('content.controllerName'),
      localdb: App.db.data
    });
    this.onTaskCompleted();
  },

  startAllServices: function () {
    this.startServices(false);
  },

  stopHDFS: function () {
    this.stopServices(["HDFS"], true);
  },

  deleteSNameNode: function () {
    var hostName = this.get('content.masterComponentHosts').findProperty('component', 'SECONDARY_NAMENODE').hostName;
    return App.ajax.send({
      name: 'common.delete.host_component',
      sender: this,
      data: {
        componentName: 'SECONDARY_NAMENODE',
        hostName: hostName
      },
      success: 'onTaskCompleted',
      error: 'onTaskError'
    });
  }

});
