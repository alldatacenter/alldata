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
require('views/main/service/item');

var view;

function getView() {
  return App.MainServiceItemView.create({
    controller: Em.Object.create({
      content: Em.Object.create({
        hostComponents: []
      }),
      setStartStopState: sinon.spy(),
      loadConfigs: sinon.spy()
    })
  });
}

describe('App.MainServiceItemView', function () {

  App.TestAliases.testAsComputedAlias(getView(), 'serviceName', 'controller.content.serviceName', 'string');

  App.TestAliases.testAsComputedAlias(getView(), 'displayName', 'controller.content.displayName', 'string');

  beforeEach(function() {
    view = getView();
  });

  describe('#mastersExcludedCommands', function () {
    view = getView();
    var nonCustomAction = ['RESTART_ALL', 'RUN_SMOKE_TEST', 'REFRESH_CONFIGS', 'ROLLING_RESTART', 'TOGGLE_PASSIVE', 'TOGGLE_NN_HA', 'TOGGLE_RM_HA', 'MOVE_COMPONENT', 'DOWNLOAD_CLIENT_CONFIGS', 'MASTER_CUSTOM_COMMAND'];
    var keys = Object.keys(view.get('mastersExcludedCommands'));
    var mastersExcludedCommands = [];
    for (var i = 0; i < keys.length; i++) {
      mastersExcludedCommands[i] = view.get('mastersExcludedCommands')[keys[i]];
    }
    var allMastersExcludedCommands = mastersExcludedCommands.reduce(function (previous, current) {
      return previous.concat(current);
    });
    var actionMap = App.HostComponentActionMap.getMap(view);

    var customActionsArray = [];
    Object.keys(actionMap).forEach(function (iter) {
      customActionsArray.push(actionMap[iter]);
    });
    var customActions = customActionsArray.mapProperty('customCommand').filter(function (action) {
      return !nonCustomAction.contains(action);
    }).uniq();

    // remove null and undefined from the list
    customActions = customActions.filter(function (value) {
      return !Em.isNone(value);
    });

    customActions.forEach(function (action) {
      it(action + ' should be present in App.MainServiceItemView mastersExcludedCommands object', function () {
        expect(allMastersExcludedCommands).to.contain(action);
      });
    });
  });

  describe('#observeMaintenance', function () {

    var cases = [
      {
        isMaintenanceSet: true,
        isServicesInfoLoaded: true,
        isServiceConfigsLoaded: true,
        observeMaintenanceOnceCallCount: 0,
        title: 'actions array set, services info loaded'
      },
      {
        isMaintenanceSet: true,
        isServicesInfoLoaded: false,
        isServiceConfigsLoaded: true,
        observeMaintenanceOnceCallCount: 0,
        title: 'actions array set, services info not loaded'
      },
      {
        isMaintenanceSet: false,
        isServicesInfoLoaded: true,
        isServiceConfigsLoaded: true,
        observeMaintenanceOnceCallCount: 1,
        title: 'actions array not set, services info loaded'
      },
      {
        isMaintenanceSet: false,
        isServicesInfoLoaded: false,
        isServiceConfigsLoaded: true,
        observeMaintenanceOnceCallCount: 0,
        title: 'actions array not set, services info not loaded'
      }
    ];

    beforeEach(function () {
      sinon.stub(view, 'observeMaintenanceOnce', Em.K);
    });

    afterEach(function () {
      view.observeMaintenanceOnce.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        view.setProperties({
          'isMaintenanceSet': item.isMaintenanceSet,
          'controller.isServicesInfoLoaded': item.isServicesInfoLoaded,
          'controller.isServiceConfigsLoaded': item.isServiceConfigsLoaded
        });
        view.observeMaintenance();
        expect(view.observeMaintenanceOnce.callCount).to.equal(item.observeMaintenanceOnceCallCount);
      });
    });

  });

  describe('#observeMaintenanceOnce', function () {

    var mastersExcludedCommands = {
        NAMENODE: ["DECOMMISSION", "REBALANCEHDFS"],
        RESOURCEMANAGER: ["DECOMMISSION", "REFRESHQUEUES"],
        HBASE_MASTER: ["DECOMMISSION"],
        KNOX_GATEWAY: ["STARTDEMOLDAP", "STOPDEMOLDAP"]
      },
      hasConfigTab = true,
      testCases = [
        {
          serviceName: "HDFS",
          displayName: "HDFS",
          supportDeleteViaUi: true,
          isSingleNode: true,
          serviceTypes: ["HA_MODE"],
          slaveComponents: [
            Em.Object.create({
              componentName: 'DATANODE',
              totalCount: 1
            })
          ],
          clientComponents: [
            Em.Object.create({
              componentName: 'HDFS_CLIENT',
              totalCount: 1
            })
          ],
          hostComponents: [
            Em.Object.create({
              componentName: 'NAMENODE',
              isNotInstalled: true,
              isMaster: true,
              isSlave: false
            }),
            Em.Object.create({
              componentName: 'SECONDARY_NAMENODE',
              isMaster: true,
              isSlave: false
            })
          ],
          result: [
            {"action": "startService", "label": "Start", "cssClass": "glyphicon glyphicon-play enabled", "disabled": false},
            {"action": "stopService", "label": "Stop", "cssClass": "glyphicon glyphicon-stop enabled", "disabled": false},
            {"action": "restartAllHostComponents", "context": "HDFS", "label": "Restart All", "cssClass": "glyphicon glyphicon-time", "disabled": false},
            {"action": "rollingRestart", "label": "Restart DataNodes", "cssClass": "glyphicon glyphicon-time", "disabled": false, "context": "DATANODE"},
            {"action": "reassignMaster", "context": "NAMENODE", "label": "Move NameNode", "cssClass": "glyphicon glyphicon-share-alt", "disabled": false},
            {"action": "reassignMaster", "context": "SECONDARY_NAMENODE", "label": "Move SNameNode", "cssClass": "glyphicon glyphicon-share-alt", "disabled": false},
            {"action": "enableHighAvailability", "label": "Enable NameNode HA", "cssClass": "glyphicon glyphicon-arrow-up", "isHidden": false, "disabled": true},
            {"action": "runSmokeTest", "label": "Run Service Check", "cssClass": "glyphicon glyphicon-thumbs-up", "disabled": false},
            {"action": "turnOnOffPassive", "context": "Turn On Maintenance Mode for HDFS", "label": "Turn On Maintenance Mode", "cssClass": "icon-medkit", "disabled": false},
            {"action": "rebalanceHdfsNodes", "customCommand": "REBALANCEHDFS", "context": "Rebalance HDFS", "label": "Rebalance HDFS", "cssClass": "glyphicon glyphicon-refresh", "disabled": false},
            {"action": "downloadClientConfigs", "label": "Download Client Configs", "cssClass": "glyphicon glyphicon-download-alt", "isHidden": false, "disabled": false, hasSubmenu: false, submenuOptions: []}
          ]
        },
        {
          serviceName: "ZOOKEEPER",
          displayName: "ZooKeeper",
          serviceTypes: [],
          slaveComponents: [],
          clientComponents: [
            Em.Object.create({
              componentName: 'ZOOKEEPER_CLIENT',
              totalCount: 1
            })
          ],
          hostComponents: [
            Em.Object.create({
              componentName: 'ZOOKEEPER_SERVER',
              isMaster: true,
              isSlave: false
            })
          ],
          controller: [
            {'addDisabledTooltipZOOKEEPER_SERVER': ''},
            {'isAddDisabled-ZOOKEEPER_SERVER': 'disabled'}
          ],
          result: [
            {"action": "startService", "label": "Start", "cssClass": "glyphicon glyphicon-play enabled", "disabled": false},
            {"action": "stopService", "label": "Stop", "cssClass": "glyphicon glyphicon-stop enabled", "disabled": false},
            {"action": "restartAllHostComponents", "context": "ZOOKEEPER", "label": "Restart All", "cssClass": "glyphicon glyphicon-time", "disabled": false},
            {"action": "runSmokeTest", "label": "Run Service Check", "cssClass": "glyphicon glyphicon-thumbs-up", "disabled": false},
            {"action": "turnOnOffPassive", "context": "Turn On Maintenance Mode for ZooKeeper", "label": "Turn On Maintenance Mode", "cssClass": "icon-medkit", "disabled": false},
            {"cssClass": "glyphicon glyphicon-plus", "label": "Add ZooKeeper Server", "service": "ZOOKEEPER", "component": "ZOOKEEPER_SERVER", "action": "addComponent", "disabled": "", tooltip: ''},
            {"action": "downloadClientConfigs", "label": "Download Client Configs", "cssClass": "glyphicon glyphicon-download-alt", "isHidden": false, "disabled": false, "hasSubmenu": false, "submenuOptions": []}
          ]
        },
        {
          serviceName: "YARN",
          displayName: "YARN",
          serviceTypes: ['HA_MODE'],
          slaveComponents: [
            Em.Object.create({
              componentName: 'NODEMANAGER',
              totalCount: 1
            })
          ],
          clientComponents: [
            Em.Object.create({
              componentName: 'YARN_CLIENT',
              totalCount: 1
            })
          ],
          hostComponents: [
            Em.Object.create({
              componentName: 'APP_TIMELINE_SERVER',
              isMaster: true,
              isSlave: false
            }),
            Em.Object.create({
              componentName: 'RESOURCEMANAGER',
              isMaster: true,
              isSlave: false,
              isNotInstalled: false
            })
          ],
          result: [
            {"action": "startService", "label": "Start", "cssClass": "glyphicon glyphicon-play enabled", "disabled": false},
            {"action": "stopService", "label": "Stop", "cssClass": "glyphicon glyphicon-stop enabled", "disabled": false},
            {"action": "refreshYarnQueues", "customCommand": "REFRESHQUEUES", "context": "Refresh YARN Capacity Scheduler", "label": "Refresh YARN Capacity Scheduler", "cssClass": "glyphicon glyphicon-refresh", "disabled": false},
            {"action": "restartAllHostComponents", "context": "YARN", "label": "Restart All", "cssClass": "glyphicon glyphicon-time", "disabled": false},
            {"action": "rollingRestart", "label": "Restart NodeManagers", "cssClass": "glyphicon glyphicon-time", "disabled": false, "context": "NODEMANAGER"},
            {"action": "reassignMaster", "context": "APP_TIMELINE_SERVER", "label": "Move App Timeline Server", "cssClass": "glyphicon glyphicon-share-alt", "disabled": false},
            {"action": "reassignMaster", "context": "RESOURCEMANAGER", "label": "Move ResourceManager", "cssClass": "glyphicon glyphicon-share-alt", "disabled": false},
            {"action": "enableRMHighAvailability", "label": "Enable ResourceManager HA", "cssClass": "glyphicon glyphicon-arrow-up", "isHidden": false, disabled: false},
            {"action": "runSmokeTest", "label": "Run Service Check", "cssClass": "glyphicon glyphicon-thumbs-up", "disabled": false},
            {"action": "turnOnOffPassive", "context": "Turn On Maintenance Mode for YARN", "label": "Turn On Maintenance Mode", "cssClass": "icon-medkit", "disabled": false},
            {"action": "downloadClientConfigs", "label": "Download Client Configs", "cssClass": "glyphicon glyphicon-download-alt", "isHidden": false, "disabled": false, "hasSubmenu": false, "submenuOptions": []}
          ]
        },
        {
          serviceName: "MAPREDUCE2",
          displayName: "MapReduce2",
          serviceTypes: [],
          slaveComponents: [],
          clientComponents: [
            Em.Object.create({
              componentName: 'MAPREDUCE2_CLIENT',
              totalCount: 1
            })
          ],
          hostComponents: [
            Em.Object.create({
              componentName: 'HISTORYSERVER',
              isMaster: true,
              isSlave: false
            })
          ],
          result: [
            {"action": "startService", "label": "Start", "cssClass": "glyphicon glyphicon-play enabled", "disabled": false},
            {"action": "stopService", "label": "Stop", "cssClass": "glyphicon glyphicon-stop enabled", "disabled": false},
            {"action": "restartAllHostComponents", "context": "MAPREDUCE2", "label": "Restart All", "cssClass": "glyphicon glyphicon-time", "disabled": false},
            {"action": "runSmokeTest", "label": "Run Service Check", "cssClass": "glyphicon glyphicon-thumbs-up", "disabled": false},
            {"action": "turnOnOffPassive", "context": "Turn On Maintenance Mode for MapReduce2", "label": "Turn On Maintenance Mode", "cssClass": "icon-medkit", "disabled": false},
            {"action": "downloadClientConfigs", "label": "Download Client Configs", "cssClass": "glyphicon glyphicon-download-alt", "isHidden": false, "disabled": false, "hasSubmenu": false, "submenuOptions": []}
          ]
        },
        {
          serviceName: "KAFKA",
          displayName: "Kafka",
          serviceTypes: [],
          slaveComponents: [],
          clientComponents: [],
          hostComponents: [
            Em.Object.create({
              componentName: 'KAFKA_BROKER',
              isMaster: true,
              isSlave: false
            })
          ],
          result: [
            {"action": "startService", "label": "Start", "cssClass": "glyphicon glyphicon-play enabled", "disabled": false},
            {"action": "stopService", "label": "Stop", "cssClass": "glyphicon glyphicon-stop enabled", "disabled": false},
            {"action": "restartAllHostComponents", "context": "KAFKA", "label": "Restart All", "cssClass": "glyphicon glyphicon-time", "disabled": false},
            {"action": "runSmokeTest", "label": "Run Service Check", "cssClass": "glyphicon glyphicon-thumbs-up", "disabled": false},
            {"action": "turnOnOffPassive", "context": "Turn On Maintenance Mode for Kafka", "label": "Turn On Maintenance Mode", "cssClass": "icon-medkit", "disabled": false},
            {"action": "downloadClientConfigs", "label": "Download Client Configs", "cssClass": "glyphicon glyphicon-download-alt", "isHidden": true, "disabled": false, "hasSubmenu": false, "submenuOptions": []}
          ]
        },
        {
          serviceName: "FLUME",
          displayName: "Flume",
          serviceTypes: [],
          clientComponents: [],
          slaveComponents: [
            Em.Object.create({
              componentName: 'FLUME_HANDLER',
              totalCount: 1
            })
          ],
          hostComponents: [
          ],
          controller: [
            {'addDisabledTooltipFLUME_HANDLER': ''},
            {'isAddDisabled-FLUME_HANDLER': ''}
          ],
          result: [
            {"action": "startService", "label": "Start", "cssClass": "glyphicon glyphicon-play enabled", "disabled": false},
            {"action": "stopService", "label": "Stop", "cssClass": "glyphicon glyphicon-stop enabled", "disabled": false},
            {"action": "refreshConfigs", "label": "Refresh configs", "cssClass": "glyphicon glyphicon-refresh", "disabled": false},
            {"action": "restartAllHostComponents", "context": "FLUME", "label": "Restart All", "cssClass": "glyphicon glyphicon-time", "disabled": false},
            {"action": "rollingRestart", "label": "Restart Flumes", "cssClass": "glyphicon glyphicon-time", "disabled": false, "context": "FLUME_HANDLER"},
            {"action": "runSmokeTest", "label": "Run Service Check", "cssClass": "glyphicon glyphicon-thumbs-up", "disabled": false},
            {"action": "turnOnOffPassive", "context": "Turn On Maintenance Mode for Flume", "label": "Turn On Maintenance Mode", "cssClass": "icon-medkit", "disabled": false},
            {"cssClass": "glyphicon glyphicon-plus", "label": "Add Flume Component", "service": "FLUME", "component": "FLUME_HANDLER", "action": "addComponent", "disabled": '', tooltip: ''},
            {"action": "downloadClientConfigs", "label": "Download Client Configs", "cssClass": "glyphicon glyphicon-download-alt", "isHidden": true, "disabled": false, "hasSubmenu": false, "submenuOptions": []}
          ]
        },
        {
          serviceName: "HBASE",
          displayName: "HBase",
          serviceTypes: [],
          slaveComponents: [
            Em.Object.create({
              componentName: 'HBASE_REGIONSERVER',
              totalCount: 1
            })
          ],
          clientComponents: [
            Em.Object.create({
              componentName: 'HBASE_CLIENT',
              totalCount: 1
            })
          ],
          hostComponents: [
            Em.Object.create({
              componentName: 'HBASE_MASTER',
              isMaster: true,
              isSlave: false
            })
          ],
          controller: [
            {'addDisabledTooltipHBASE_MASTER': ''},
            {'isAddDisabled-HBASE_MASTER': ''}
          ],
          result: [
            {"action": "startService", "label": "Start", "cssClass": "glyphicon glyphicon-play enabled", "disabled": false},
            {"action": "stopService", "label": "Stop", "cssClass": "glyphicon glyphicon-stop enabled", "disabled": false},
            {"action": "restartAllHostComponents", "context": "HBASE", "label": "Restart All", "cssClass": "glyphicon glyphicon-time", "disabled": false},
            {"action": "rollingRestart", "label": "Restart RegionServers", "cssClass": "glyphicon glyphicon-time", "disabled": false, "context": "HBASE_REGIONSERVER"},
            {"action": "runSmokeTest", "label": "Run Service Check", "cssClass": "glyphicon glyphicon-thumbs-up", "disabled": false},
            {"action": "turnOnOffPassive", "context": "Turn On Maintenance Mode for HBase", "label": "Turn On Maintenance Mode", "cssClass": "icon-medkit", "disabled": false},
            {"cssClass": "glyphicon glyphicon-plus", "label": "Add HBase Master", "service": "HBASE", "component": "HBASE_MASTER", "action": "addComponent", "disabled": '', tooltip: ''},
            {"action": "downloadClientConfigs", "label": "Download Client Configs", "cssClass": "glyphicon glyphicon-download-alt", "isHidden": false, "disabled": false, "hasSubmenu": false, "submenuOptions": []}
          ]
        },
        {
          serviceName: "OOZIE",
          displayName: "Oozie",
          serviceTypes: [],
          slaveComponents: [],
          clientComponents: [
            Em.Object.create({
              componentName: 'OOZIE_CLIENT',
              totalCount: 1
            })
          ],
          hostComponents: [
            Em.Object.create({
              componentName: 'OOZIE_SERVER',
              isMaster: true,
              isSlave: false
            })
          ],
          result: [
            {"action": "startService", "label": "Start", "cssClass": "glyphicon glyphicon-play enabled", "disabled": false},
            {"action": "stopService", "label": "Stop", "cssClass": "glyphicon glyphicon-stop enabled", "disabled": false},
            {"action": "restartAllHostComponents", "context": "OOZIE", "label": "Restart All", "cssClass": "glyphicon glyphicon-time", "disabled": false},
            {"action": "reassignMaster", "context": "OOZIE_SERVER", "label": "Move Oozie Server", "cssClass": "glyphicon glyphicon-share-alt", "disabled": false},
            {"action": "runSmokeTest", "label": "Run Service Check", "cssClass": "glyphicon glyphicon-thumbs-up", "disabled": false},
            {"action": "turnOnOffPassive", "context": "Turn On Maintenance Mode for Oozie", "label": "Turn On Maintenance Mode", "cssClass": "icon-medkit", "disabled": false},
            {"cssClass": "glyphicon glyphicon-plus", "label": "Add Oozie Server", "service": "OOZIE", "component": "OOZIE_SERVER", "action": "addComponent", "disabled": "disabled", tooltip: Em.I18n.t('services.summary.allHostsAlreadyRunComponent').format('OOZIE_SERVER')},
            {"action": "downloadClientConfigs", "label": "Download Client Configs", "cssClass": "glyphicon glyphicon-download-alt", "isHidden": false, "disabled": false, "hasSubmenu": false, "submenuOptions": []}
          ]
        },
        {
          serviceName: "KNOX",
          displayName: "Knox",
          serviceTypes: [],
          slaveComponents: [],
          clientComponents: [],
          hostComponents: [
            Em.Object.create({
              componentName: 'KNOX_GATEWAY',
              isMaster: true,
              isSlave: false
            })
          ],
          result: [
            {"action": "startService", "label": "Start", "cssClass": "glyphicon glyphicon-play enabled", "disabled": false},
            {"action": "stopService", "label": "Stop", "cssClass": "glyphicon glyphicon-stop enabled", "disabled": false},
            {"action": "restartAllHostComponents", "context": "KNOX", "label": "Restart All", "cssClass": "glyphicon glyphicon-time", "disabled": false},
            {"action": "runSmokeTest", "label": "Run Service Check", "cssClass": "glyphicon glyphicon-thumbs-up", "disabled": false},
            {"action": "turnOnOffPassive", "context": "Turn On Maintenance Mode for Knox", "label": "Turn On Maintenance Mode", "cssClass": "icon-medkit", "disabled": false},
            {"action": "startLdapKnox", "customCommand": "STARTDEMOLDAP", "context": "Start Demo LDAP", "label": "Start Demo LDAP", "cssClass": "icon icon-play-sign", "disabled": false},
            {"action": "stopLdapKnox", "customCommand": "STOPDEMOLDAP", "context": "Stop Demo LDAP", "label": "Stop Demo LDAP", "cssClass": "glyphicon glyphicon-stop", "disabled": false},
            {"action": "downloadClientConfigs", "label": "Download Client Configs", "cssClass": "glyphicon glyphicon-download-alt", "isHidden": true, "disabled": false, "hasSubmenu": false, "submenuOptions": []}
          ]
        },
        {
          serviceName: "STORM",
          displayName: "Storm",
          serviceTypes: [],
          slaveComponents: [],
          clientComponents: [],
          hostComponents: [
            Em.Object.create({
              componentName: 'NIMBUS',
              isMaster: true,
              isSlave: false
            })
          ],
          result: [
            {"action": "startService", "label": "Start", "cssClass": "glyphicon glyphicon-play enabled", "disabled": false},
            {"action": "stopService", "label": "Stop", "cssClass": "glyphicon glyphicon-stop enabled", "disabled": false},
            {"action": "restartAllHostComponents", "context": "STORM", "label": "Restart All", "cssClass": "glyphicon glyphicon-time", "disabled": false},
            {"action": "runSmokeTest", "label": "Run Service Check", "cssClass": "glyphicon glyphicon-thumbs-up", "disabled": false},
            {"action": "turnOnOffPassive", "context": "Turn On Maintenance Mode for Storm", "label": "Turn On Maintenance Mode", "cssClass": "icon-medkit", "disabled": false},
            {"action": "downloadClientConfigs", "label": "Download Client Configs", "cssClass": "glyphicon glyphicon-download-alt", "isHidden": true, "disabled": false, "hasSubmenu": false, "submenuOptions": []}
          ]
        }
      ];

    beforeEach(function () {

      sinon.stub(App, 'get', function (k) {
        switch (k) {
          case 'isSingleNode':
            return view.get('controller.content.serviceName') === 'HDFS';
          case 'supports.autoRollbackHA':
          case 'isRMHaEnabled':
          case 'isHaEnabled':
            return false;
          case 'components.rollinRestartAllowed':
            return ["DATANODE", "JOURNALNODE", "ZKFC", "NODEMANAGER", "GANGLIA_MONITOR", "HBASE_REGIONSERVER", "SUPERVISOR", "FLUME_HANDLER"];
          case 'components.reassignable':
            return ["NAMENODE", "SECONDARY_NAMENODE", "APP_TIMELINE_SERVER", "RESOURCEMANAGER", "WEBHCAT_SERVER", "OOZIE_SERVER"];
          case 'services.supportsServiceCheck':
            return ["HDFS", "MAPREDUCE2", "YARN", "HIVE", "HBASE", "PIG", "SQOOP", "OOZIE", "ZOOKEEPER", "FALCON", "STORM", "FLUME", "SLIDER", "KNOX", "KAFKA"];
          case 'services.supportsDeleteViaUI':
            return ["HDFS", "MAPREDUCE2", "YARN", "HIVE", "HBASE", "PIG", "SQOOP", "OOZIE", "ZOOKEEPER", "FALCON", "STORM", "FLUME", "SLIDER", "KNOX", "KAFKA"];
          case 'components.addableToHost':
            return ["DATANODE", "HDFS_CLIENT", "MAPREDUCE2_CLIENT", "NODEMANAGER", "YARN_CLIENT", "TEZ_CLIENT", "GANGLIA_MONITOR", "HCAT", "HIVE_CLIENT", "HIVE_METASTORE", "HIVE_SERVER", "WEBHCAT_SERVER", "HBASE_CLIENT", "HBASE_MASTER", "HBASE_REGIONSERVER", "PIG", "SQOOP", "OOZIE_CLIENT", "OOZIE_SERVER", "ZOOKEEPER_CLIENT", "ZOOKEEPER_SERVER", "FALCON_CLIENT", "SUPERVISOR", "FLUME_HANDLER", "METRICS_MONITOR", "KAFKA_BROKER", "KERBEROS_CLIENT", "KNOX_GATEWAY", "SLIDER", "SPARK_CLIENT"];
          case 'allHostNames.length':
            return 2;
          default:
            return Em.get(App, k);
        }
      });

      sinon.stub(App.HostComponent, 'find', function () {
        return [
          Em.Object.create({
            hostName: 'host1',
            componentName: 'NAMENODE'
          }),
          Em.Object.create({
            hostName: 'host1',
            componentName: 'SECONDARY_NAMENODE'
          }),
          Em.Object.create({
            hostName: 'host1',
            componentName: 'APP_TIMELINE_SERVER'
          }),
          Em.Object.create({
            hostName: 'host1',
            componentName: 'RESOURCEMANAGER'
          }),
          Em.Object.create({
            hostName: 'host1',
            componentName: 'OOZIE_SERVER'
          })
        ];
      });

      /*eslint-disable complexity */
      sinon.stub(App.StackServiceComponent, 'find', function (id) {
        switch (id) {
          case 'NAMENODE':
            return Em.Object.create({ customCommands: ["DECOMMISSION", "REBALANCEHDFS"] });
          case 'RESOURCEMANAGER':
            return Em.Object.create({ customCommands: ["DECOMMISSION", "REFRESHQUEUES"] });
          case 'HBASE_MASTER':
            return Em.Object.create({ customCommands: ["DECOMMISSION"] });
          case 'KNOX_GATEWAY':
            return Em.Object.create({ customCommands: ["STARTDEMOLDAP", "STOPDEMOLDAP"] });
          case 'HIVE_SERVER_INTERACTIVE':
            return Em.Object.create({ customCommands: ["RESTART_LLAP"] });
          case 'HISTORYSERVER':
          case 'SECONDARY_NAMENODE':
          case 'ZOOKEEPER_SERVER':
          case 'APP_TIMELINE_SERVER':
          case 'KAFKA_BROKER':
          case 'OOZIE_SERVER':
          case 'NIMBUS':
            return Em.Object.create({ customCommands: [] });
          default:
            return [
              Em.Object.create({
                customCommands: ["DECOMMISSION", "REBALANCEHDFS"],
                componentName: 'NAMENODE'
              }),
              Em.Object.create({
                customCommands: ["STARTDEMOLDAP", "STOPDEMOLDAP"],
                componentName: 'KNOX_GATEWAY'
              })
            ];
        }
      });
      sinon.stub(App.router, 'get').returns(0);
      /*eslint-enable complexity */
    });

    afterEach(function () {
      App.get.restore();
      App.HostComponent.find.restore();
      App.StackServiceComponent.find.restore();
      App.router.get.restore();
    });

    testCases.forEach(function (testCase) {

      describe('Maintenance for ' + testCase.serviceName + ' service', function () {

        beforeEach(function () {
          view.reopen({
            controller: Em.Object.create({
              content: Em.Object.create({
                hostComponents: testCase.hostComponents,
                slaveComponents: testCase.slaveComponents,
                clientComponents: testCase.clientComponents,
                serviceName: testCase.serviceName,
                displayName: testCase.displayName,
                serviceTypes: testCase.serviceTypes,
                installedClients: 1,
                passiveState: 'OFF',
                hasMasterOrSlaveComponent: true
              }),
              isSeveralClients: false,
              clientComponents: [],
              isStartDisabled: false,
              isStopDisabled: false,
              isSmokeTestDisabled: false
            }),
            mastersExcludedCommands: mastersExcludedCommands,
            hasConfigTab: hasConfigTab
          });
          if (testCase.controller) {
            testCase.controller.forEach(function (item) {
              Object.keys(item).forEach(function (key) {
                view.set('controller.' + key, item[key]);
              });
            });
          }
          view.observeMaintenanceOnce();
        });
        testCase.result.forEach(function (option, index) {
          Object.keys(option).forEach(function (key) {
            it(option.action + ', key - ' + key, function () {
              var r = view.get('maintenance')[index];
              expect(Em.get(option, key)).to.eql(Em.get(r, key));
            });
          });
        });

        it('maintenance is updated', function () {
          var oldMaintenance = JSON.parse(JSON.stringify(view.get('maintenance')));
          view.set('controller.content.passiveState', 'ON');
          view.observeMaintenanceOnce();
          expect(view.get('maintenance')).to.not.eql(oldMaintenance);
          expect(view.get('isMaintenanceSet')).to.be.true;
        });
      });

    });
  });

  describe('#clearIsMaintenanceSet', function () {
    it('isMaintenanceSet should be false', function () {
      view.set('isMaintenanceSet', true);
      view.clearIsMaintenanceSet();
      expect(view.get('isMaintenanceSet')).to.be.false;
    });
  });

  describe('#isMaintenanceActive', function() {

    it('isMaintenanceActive should be false when maintenance empty', function() {
      view.set('state', 'inDOM');
      view.set('maintenance', []);
      expect(view.get('isMaintenanceActive')).to.be.false;
    });

    it('isMaintenanceActive should be true when maintenance not empty', function() {
      view.set('state', 'inDOM');
      view.set('maintenance', [{}]);
      expect(view.get('isMaintenanceActive')).to.be.true;
    });

    it('isMaintenanceActive should be true when state not inDOM', function() {
      view.set('state', '');
      view.set('maintenance', [{}]);
      expect(view.get('isMaintenanceActive')).to.be.true;
    });
  });

  describe('#hasConfigTab', function() {
    beforeEach(function() {
      this.mockAuthorized = sinon.stub(App, 'havePermissions');
      this.mockGet = sinon.stub(App, 'get').returns(['S2']);
    });
    afterEach(function() {
      this.mockAuthorized.restore();
      this.mockGet.restore();
    });

    it('should return false when have not permissions', function() {
      this.mockAuthorized.returns(false);
      view.set('controller.content.serviceName', 'S1');
      expect(view.get('hasConfigTab')).to.be.false;
    });

    it('should return false when service does not have config types', function() {
      this.mockAuthorized.returns(true);
      view.set('controller.content.serviceName', 'S2');
      expect(view.get('hasConfigTab')).to.be.false;
    });

    it('should return true when have permissions', function() {
      this.mockAuthorized.returns(true);
      view.set('controller.content.serviceName', 'S1');
      expect(view.get('hasConfigTab')).to.be.true;
    });
  });

  describe('#hasHeatmapTab', function() {
    beforeEach(function() {
      sinon.stub(App.StackService, 'find').returns(Em.Object.create({
        hasHeatmapSection: true
      }));
    });
    afterEach(function() {
      App.StackService.find.restore();
    });

    it('should return false when service does not have heatmaps', function() {
      view.reopen({
        hasMasterOrSlaveComponent: false
      });
      expect(view.get('hasHeatmapTab')).to.be.false;
    });
    it('should return true when service has heatmaps', function() {
      view.reopen({
        hasMasterOrSlaveComponent: true
      });
      expect(view.get('hasHeatmapTab')).to.be.true;
    });
  });

  describe('#didInsertElement', function() {

    it('setStartStopState should be called', function() {
      view.didInsertElement();
      expect(view.get('controller').setStartStopState.calledOnce).to.be.true;
    });
  });

  describe('#willInsertElement', function() {
    beforeEach(function() {
      sinon.stub(view, 'addObserver');
    });
    afterEach(function() {
      view.addObserver.restore();
    });

    it('loadConfigs should be called', function() {
      view.willInsertElement();
      expect(view.get('controller').loadConfigs.calledOnce).to.be.true;
    });
    it('addObserver should be called', function() {
      view.set('maintenanceObsFields', ['foo']);
      view.willInsertElement();
      expect(view.addObserver.calledWith('controller.foo')).to.be.true;
    });
  });

  describe('#willDestroyElement', function() {
    beforeEach(function() {
      sinon.stub(view, 'removeObserver');
    });
    afterEach(function() {
      view.removeObserver.restore();
    });

    it('addObserver should be called', function() {
      view.set('maintenanceObsFields', ['foo']);
      view.willDestroyElement();
      expect(view.removeObserver.calledWith('controller.foo')).to.be.true;
    });
  });

  describe('#service', function() {

    beforeEach(function() {
      sinon.stub(App.HDFSService, 'find').returns(Em.A([{}]));
      sinon.stub(App.YARNService, 'find').returns(Em.A([{}]));
      sinon.stub(App.HBaseService, 'find').returns(Em.A([{}]));
      sinon.stub(App.FlumeService, 'find').returns(Em.A([{}]));
    });
    afterEach(function() {
      App.HDFSService.find.restore();
      App.YARNService.find.restore();
      App.HBaseService.find.restore();
      App.FlumeService.find.restore();
    });

    ['HDFS', 'YARN', 'HBASE', 'FLUME'].forEach(function(service) {
      it('should return object of ' + service, function() {
        view.set('controller.content.serviceName', service);
        expect(view.get('service')).to.be.an.object;
      });
    });

    it('should return content', function() {
      view.set('controller.content', Em.Object.create({
        serviceName: 'S1'
      }));
      expect(view.get('service')).to.be.an.object;
    });
  });
});

