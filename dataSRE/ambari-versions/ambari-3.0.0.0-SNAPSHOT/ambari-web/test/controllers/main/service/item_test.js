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

App = require('app');
require('ember');
require('models/host_component');
require('views/common/modal_popup');
require('mixins/common/persist');
require('controllers/application');
require('controllers/global/background_operations_controller');
require('controllers/global/cluster_controller');
require('controllers/main/service/reassign_controller');
require('controllers/main/service/item');
var batchUtils = require('utils/batch_scheduled_requests');
var testHelpers = require('test/helpers');
var stackServiceModel = {
  'HDFS': Em.Object.create({
    serviceName: 'HDFS',
    requiredServices: ['ZOOKEEPER']
  }),
  'YARN': Em.Object.create({
    serviceName: 'YARN',
    requiredServices: ['MAPREDUCE2', 'HDFS']
  }),
  'MAPREDUCE2': Em.Object.create({
    serviceName: 'MAPREDUCE2',
    requiredServices: ['YARN']
  }),
  'TEZ': Em.Object.create({
    serviceName: 'TEZ',
    requiredServices: ['YARN']
  }),
  'HIVE': Em.Object.create({
    serviceName: 'HIVE',
    requiredServices: ['YARN', 'TEZ']
  })
};

describe('App.MainServiceItemController', function () {

  describe('#setStartStopState', function () {
    var tests = [
      {
        serviceController: {
          serviceName: "YARN"
        },
        backgroundOperationsController: {
          services: [
            {
              isRunning: true,
              dependentService: "ALL_SERVICES"
            }
          ]
        },
        isPending: true,
        m: 'operaion is active because all services are running'
      },
      {
        serviceController: {
          serviceName: "HBASE"
        },
        backgroundOperationsController: {
          services: [
            {
              isRunning: true,
              dependentService: "HBASE"
            }
          ]
        },
        isPending: true,
        m: 'operaion is active button because current service is running'
      },
      {
        serviceController: {
          serviceName: "HDFS"
        },
        backgroundOperationsController: {
          services: [

          ]
        },
        isPending: true,
        m: 'pending is true - backgroundOperationsController.services is empty'
      },
      {
        serviceController: {
          serviceName: "HBASE"
        },
        backgroundOperationsController: {
          services: [
            {
              isRunning: false,
              dependentService: "ALL_SERVICES"
            }
          ]
        },
        isPending: false,
        m: 'pending is false - operation is not running'
      },
      {
        serviceController: {
          serviceName: "HBASE"
        },
        backgroundOperationsController: {
          services: [
            {
              isRunning: true,
              dependentService: "HDFS"
            }
          ]
        },
        isPending: false,
        m: 'pending is false - current service is not running'
      }
    ];

    tests.forEach(function (test) {
      describe(test.m, function () {

        var mainServiceItemController;

        beforeEach(function () {
          sinon.stub(App.router, 'get', function(k) {
            if ('backgroundOperationsController.services' === k) return test.backgroundOperationsController.services;
            return Em.get(App.router, k);
          });
          mainServiceItemController = App.MainServiceItemController.create({content: {serviceName: test.serviceController.serviceName}});
          mainServiceItemController.setStartStopState();
        });

        afterEach(function () {
          App.router.get.restore();
        });

        it('isPending is ' + test.isPending, function () {
          expect(mainServiceItemController.get('isPending')).to.equal(test.isPending);
        });

      });
    })
  });

  describe('#reassignMaster()', function () {
    var tests = [
      {
        host_components: [
          {componentName: "RESOURCEMANGER"}
        ],
        componentName: "RESOURCEMANGER",
        result: true,
        m: 'run reassignMaster'
      },
      {
        host_components: [
          {componentName: "RESOURCEMANGER"}
        ],
        componentName: "DATANODE",
        result: false,
        m: 'don\t run reassignMaster'
      }
    ];

    tests.forEach(function (test) {
      describe(test.m, function () {

        var reassignMasterController = App.ReassignMasterController.create({currentStep: ''});

        beforeEach(function () {
          sinon.stub(reassignMasterController, 'saveComponentToReassign', Em.K);
          sinon.stub(reassignMasterController, 'setCurrentStep', Em.K);
          sinon.stub(App.router, 'transitionTo', Em.K);
          var mainServiceItemController = App.MainServiceItemController.create({});
          sinon.stub(App.HostComponent, 'find', function() {
            return test.host_components
          });
          sinon.stub(App.router, 'get', function(k) {
            if ('reassignMasterController' === k) return reassignMasterController;
            return Em.get(App.router, k);
          });
          mainServiceItemController.reassignMaster(test.componentName);
        });

        afterEach(function () {
          reassignMasterController.saveComponentToReassign.restore();
          reassignMasterController.setCurrentStep.restore();
          App.HostComponent.find.restore();
          App.router.transitionTo.restore();
          App.router.get.restore();
        });

        it('saveComponentToReassign is ' + (test.result ? '' : 'not') + ' called once', function () {
          expect(reassignMasterController.saveComponentToReassign.calledOnce).to.equal(test.result);
        });

        it('setCurrentStep is ' + (test.result ? '' : 'not') + ' called once', function () {
          expect(reassignMasterController.setCurrentStep.calledOnce).to.equal(test.result);
        });

      });
    }, this);
  });

  describe("#doAction", function () {

    var el = document.createElement("BUTTON");
    el.disabled = false;
    var tests = [
      {
        event: {
          target: el,
          context: {
            action: 'runSmokeTest'
          }
        },
        m: "run runSmokeTest"
      },
      {
        event: {
          target: el,
          context: {
            action: 'refreshConfigs'
          }
        },
        m: "run refreshConfigs"
      },
      {
        event: {
          target: el,
          context: {
            action: 'restartAllHostComponents'
          }
        },
        m: "run restartAllHostComponents"
      },
      {
        event: {
          target: el,
          context: {
            action: 'rollingRestart'
          }
        },
        m: "run rollingRestart"
      }
    ];

    tests.forEach(function (test) {
      var mainServiceItemController = App.MainServiceItemController.create({});
      mainServiceItemController.set(test.event.context.action, Em.K);
      beforeEach(function () {
        sinon.spy(mainServiceItemController, test.event.context.action);
      });
      afterEach(function () {
        mainServiceItemController[test.event.context.action].restore();
      });
      it(test.m, function () {
        mainServiceItemController.doAction(test.event);
        expect(mainServiceItemController[test.event.context.action].calledOnce).to.equal(!test.event.target.disabled);
      });
    });
  });

  describe("#startService , #stopService", function () {
    var mainServiceItemController = App.MainServiceItemController.create({startStopPopup: Em.K});
    beforeEach(function () {
      sinon.spy(mainServiceItemController, "startStopPopup");
    });
    afterEach(function () {
      mainServiceItemController.startStopPopup.restore();
    });
    it("start service", function () {
      mainServiceItemController.startService();
      expect(mainServiceItemController.startStopPopup.calledWith(App.HostComponentStatus.started)).to.equal(true);
    });
    it("stop service", function () {
      mainServiceItemController.stopService();
      expect(mainServiceItemController.startStopPopup.calledWith(App.HostComponentStatus.stopped)).to.equal(true);
    });
  });

  describe("#turnOnOffPassive", function () {
    var mainServiceItemController = App.MainServiceItemController.create({turnOnOffPassiveRequest: Em.K});
    beforeEach(function () {
      sinon.spy(batchUtils, "turnOnOffPassiveRequest");
      mainServiceItemController.set('content', {serviceName: ''});
    });
    afterEach(function () {
      batchUtils.turnOnOffPassiveRequest.restore();
    });
    it("turns on/off passive mode for service", function () {
      mainServiceItemController.turnOnOffPassive({}).onPrimary();
      expect(batchUtils.turnOnOffPassiveRequest.calledOnce).to.equal(true);
    });
  });

  describe("#runSmokeTest", function () {
    var tests = [
      {
        content: {
          id: "YARN",
          service_name: "YARN",
          work_status: "STARTED"
        },
        startSmoke: true,
        serviceName: "MAPREDUCE2",
        m: "don't run smoke test primary for MAPREDUCE2"
      },
      {
        content: {
          id: "YARN",
          service_name: "YARN",
          work_status: "STOPPED"
        },
        startSmoke: false,
        serviceName: "MAPREDUCE2",
        m: "run smoke test primary for MAPREDUCE2"
      },
      {
        m: "run smoke test primary for all services (not MAPREDUCE2)",
        startSmoke: true,
        default: true
      }
    ];

    tests.forEach(function (test) {
      var mainServiceItemController = test.default ? App.MainServiceItemController.create({runSmokeTestPrimary: Em.K}) :
        App.MainServiceItemController.create({content: {serviceName: test.serviceName}, runSmokeTestPrimary: Em.K});
      beforeEach(function () {
        sinon.spy(mainServiceItemController, "runSmokeTestPrimary");
      });
      afterEach(function () {
        mainServiceItemController.runSmokeTestPrimary.restore();
      });
      it(test.m, function () {
        if (!test.default) {
          App.store.safeLoad(App.Service, test.content);
        }
        mainServiceItemController.runSmokeTest({}).onPrimary();
        expect(mainServiceItemController.runSmokeTestPrimary.calledOnce).to.equal(test.startSmoke);
      });
    });
  });

  describe("#startStopPopup", function () {
    var mainServiceItemController = App.MainServiceItemController.create({
      content: {
        serviceName: 'HDFS',
        hostComponents: [
          Em.Object.create({
            componentName: 'NAMENODE',
            workStatus: 'INSTALLED'
          })
        ]
      }
    });
    var mainServiceItemControllerHdfsStarted = App.MainServiceItemController.create({
      content: {
        serviceName: 'HDFS',
        hostComponents: [
          Em.Object.create({
            componentName: 'NAMENODE',
            workStatus: 'STARTED'
          })
        ]
      }
    });
    beforeEach(function () {
      sinon.spy(mainServiceItemController, "startStopPopupPrimary");
      sinon.spy(mainServiceItemControllerHdfsStarted, "startStopPopupPrimary");
      sinon.spy(Em.I18n, "t");
      sinon.stub(mainServiceItemControllerHdfsStarted, 'checkNnLastCheckpointTime', function(callback) {
        return callback;
      });
    });
    afterEach(function () {
      mainServiceItemController.startStopPopupPrimary.restore();
      mainServiceItemControllerHdfsStarted.startStopPopupPrimary.restore();
      mainServiceItemControllerHdfsStarted.checkNnLastCheckpointTime.restore();
      Em.I18n.t.restore();
    });
    it("start start/stop service popup", function () {
      mainServiceItemController.startStopPopup("").onPrimary();
      expect(mainServiceItemController.startStopPopupPrimary.calledOnce).to.equal(true);
    });

    it ("should popup warning to check last checkpoint time if work status is STARTED", function() {
      mainServiceItemControllerHdfsStarted.startStopPopup("INSTALLED");
      expect(mainServiceItemControllerHdfsStarted.checkNnLastCheckpointTime.calledOnce).to.equal(true);
    });

    describe("modal messages", function() {

      beforeEach(function () {
        sinon.stub(App.StackService, 'find').returns([
          Em.Object.create({
            serviceName: 'HDFS',
            displayName: 'HDFS',
            isInstalled: true,
            isSelected: true,
            requiredServices:["ZOOKEEPER"]
          }),
          Em.Object.create({
            serviceName: 'HIVE',
            displayName: 'Hive',
            isInstalled: true,
            isSelected: true
          }),
          Em.Object.create({
            serviceName: 'HBASE',
            displayName: 'HBase',
            isInstalled: true,
            isSelected: true,
            requiredServices:["HDFS", "ZOOKEEPER"]
          }),
          Em.Object.create({
            serviceName: 'YARN',
            displayName: 'YARN',
            isInstalled: true,
            isSelected: true,
            requiredServices:["HDFS"]
          }),
          Em.Object.create({
            serviceName: 'SPARK',
            displayName: 'Spark',
            isInstalled: true,
            isSelected: true,
            requiredServices:["HIVE"]
          })
        ]);
      });

      it ("should confirm stop if serviceHealth is INSTALLED", function() {
        mainServiceItemController.startStopPopup("INSTALLED");
        expect(Em.I18n.t.calledWith('services.service.stop.confirmMsg')).to.be.ok;
        expect(Em.I18n.t.calledWith('services.service.stop.confirmButton')).to.be.ok;
      });

      it ("should confirm start if serviceHealth is not INSTALLED", function() {
        mainServiceItemController.startStopPopup("");
        expect(Em.I18n.t.calledWith('services.service.start.confirmMsg')).to.be.ok;
        expect(Em.I18n.t.calledWith('services.service.start.confirmButton')).to.be.ok;
      });

      it ("should not display a dependent list if it is to start a service", function() {
        var _mainServiceItemController = App.MainServiceItemController.create(
            {content: {serviceName: "HDFS", passiveState:'OFF'}});
        _mainServiceItemController.startStopPopup("");
        expect(Em.I18n.t.calledWith('services.service.stop.warningMsg.dependent.services')).to.not.be.ok;
      });

      describe ("should display dependent list if other services depend on the one to be stopped", function() {
        beforeEach(function () {
          var _mainServiceItemController = App.MainServiceItemController.create(
            {content: {
              serviceName: 'HDFS',
              passiveState:'OFF',
              hostComponents: [
                Em.Object.create({
                  componentName: 'NAMENODE',
                  workStatus: 'INSTALLED'
                })
              ]
            }}
          );
          _mainServiceItemController.startStopPopup("INSTALLED");
          this.dependencies = Em.I18n.t('services.service.stop.warningMsg.dependent.services').format("HDFS", "HBase,YARN");
          this.msg = Em.I18n.t('services.service.stop.warningMsg.turnOnMM').format("HDFS");
          this.fullMsg = _mainServiceItemController.addAdditionalWarningMessage("INSTALLED", this.msg, "HDFS");
        });

        it('turnOnMM message is shown', function () {
          expect(Em.I18n.t.calledWith('services.service.stop.warningMsg.turnOnMM')).to.be.ok;
        });
        it('message about dependent services is shown', function () {
          expect(Em.I18n.t.calledWith('services.service.stop.warningMsg.dependent.services')).to.be.ok;
        });
        it('full message is valid', function () {
          expect(this.fullMsg).to.be.equal(this.msg + " " + this.dependencies);
        });
      });

      describe("should display the dependent service if another service depends on the one to be stopped", function() {

        beforeEach(function () {
          var _mainServiceItemController = App.MainServiceItemController.create(
            {content: {serviceName: "HIVE", passiveState:'OFF'}});
          _mainServiceItemController.startStopPopup("INSTALLED");
          this.dependencies = Em.I18n.t('services.service.stop.warningMsg.dependent.services').format("HIVE", "Spark");
          this.msg = Em.I18n.t('services.service.stop.warningMsg.turnOnMM').format("HIVE");
          this.fullMsg = _mainServiceItemController.addAdditionalWarningMessage("INSTALLED", this.msg, "HIVE");
        });

        it('message about dependent services is shown', function () {
          expect(Em.I18n.t.calledWith('services.service.stop.warningMsg.dependent.services')).to.be.ok;
        });
        it('full message is valid', function () {
          expect(this.fullMsg).to.be.equal(this.msg + " " + this.dependencies);
        });
      });

      afterEach(function () {
        App.StackService.find.restore();
      });
    });
  });

  describe("#restartAllHostComponents", function () {
    var temp = batchUtils.restartAllServiceHostComponents;
    var mainServiceItemController = App.MainServiceItemController.create({
      content: {
        serviceName: 'HDFS',
        hostComponents: [
          Em.Object.create({
            componentName: 'NAMENODE',
            workStatus: 'STARTED'
          })
        ]
      }
    });
    beforeEach(function () {
      batchUtils.restartAllServiceHostComponents = Em.K;
      sinon.spy(batchUtils, "restartAllServiceHostComponents");
      sinon.stub(App.Service, 'find', function() {
        return Em.Object.create({serviceTypes: []});
      });
      sinon.stub(mainServiceItemController, 'checkNnLastCheckpointTime', function() {
        return true;
      });
    });
    afterEach(function () {
      batchUtils.restartAllServiceHostComponents.restore();
      batchUtils.restartAllServiceHostComponents = temp;
      App.Service.find.restore();
      mainServiceItemController.checkNnLastCheckpointTime.restore();
    });

    it("start restartAllHostComponents for service", function () {
      var controller = App.MainServiceItemController.create({
        content: {
          serviceName: 'HDFS',
          hostComponents: [
            Em.Object.create({
              componentName: 'NAMENODE',
              workStatus: 'INSTALLED'
            })
          ]
        }
      });
      controller.restartAllHostComponents({}).onPrimary();
      expect(batchUtils.restartAllServiceHostComponents.calledOnce).to.equal(true);
    });

    it("check last checkpoint time for NameNode before start restartAllHostComponents for service", function () {
      mainServiceItemController.restartAllHostComponents({});
      expect(mainServiceItemController.checkNnLastCheckpointTime.calledOnce).to.equal(true);
    });
  });

  describe("#rollingRestart", function () {
    var temp = batchUtils.launchHostComponentRollingRestart;
    beforeEach(function () {
      batchUtils.launchHostComponentRollingRestart = Em.K;
      sinon.spy(batchUtils, "launchHostComponentRollingRestart");
    });
    afterEach(function () {
      batchUtils.launchHostComponentRollingRestart.restore();
      batchUtils.launchHostComponentRollingRestart = temp;
    });

    var mainServiceItemController = App.MainServiceItemController.create();

    it("start restartAllHostComponents for service", function () {
      mainServiceItemController.rollingRestart();
      expect(batchUtils.launchHostComponentRollingRestart.calledOnce).to.equal(true);
    });
  });

  describe("#parseNnCheckPointTime", function () {
    var tests = [
      {
        m: "NameNode has JMX data, the last checkpoint time is less than 12 hours ago",
        data:
        {"href" : "",
          "ServiceComponentInfo" : {
            "cluster_name" : "c123",
            "component_name" : "NAMENODE",
            "service_name" : "HDFS"
          },
          "host_components" : [
            {
              "href" : "",
              "HostRoles" : {
                "cluster_name" : "c123",
                "component_name" : "NAMENODE",
                "host_name" : "c6401.ambari.apache.org"
              },
              "metrics" : {
                "dfs" : {
                  "FSNamesystem" : {
                    "HAState" : "active",
                    "LastCheckpointTime" : 1435775648000
                  }
                }
              }
            }
          ]
        },
        result: 0
      },
      {
        m: "NameNode has JMX data, the last checkpoint time is > 12 hours ago",
        data:
          {"href" : "",
            "ServiceComponentInfo" : {
              "cluster_name" : "c123",
              "component_name" : "NAMENODE",
              "service_name" : "HDFS"
            },
            "host_components" : [
              {
                "href" : "",
                "HostRoles" : {
                  "cluster_name" : "c123",
                  "component_name" : "NAMENODE",
                  "host_name" : "c6401.ambari.apache.org"
                },
                "metrics" : {
                  "dfs" : {
                    "FSNamesystem" : {
                      "HAState" : "active",
                      "LastCheckpointTime" : 1435617248000
                    }
                  }
                }
              }
            ]
          },
        result: 1
      },
      {
        m: "NameNode has no JMX data available",
        data:
        {"href" : "",
          "ServiceComponentInfo" : {
            "cluster_name" : "c123",
            "component_name" : "NAMENODE",
            "service_name" : "HDFS"
          },
          "host_components" : [
            {
              "href" : "",
              "HostRoles" : {
                "cluster_name" : "c123",
                "component_name" : "NAMENODE",
                "host_name" : "c6401.ambari.apache.org"
              },
              "metrics" : {
                "dfs" : {
                  "FSNamesystem" : {
                    "HAState" : "active"
                  }
                }
              }
            }
          ]
        },
        result: 0
      },
      {
        m: "HA enabled, both active and standby NN has JMX data normally.",
        data:
        {"href" : "",
          "ServiceComponentInfo" : {
            "cluster_name" : "c123",
            "component_name" : "NAMENODE",
            "service_name" : "HDFS"
          },
          "host_components" : [
            {
              "href" : "",
              "HostRoles" : {
                "cluster_name" : "c123",
                "component_name" : "NAMENODE",
                "host_name" : "c6401.ambari.apache.org"
              },
              "metrics" : {
                "dfs" : {
                  "FSNamesystem" : {
                    "HAState" : "active",
                    "LastCheckpointTime" : 1435775648000
                  }
                }
              }
            },
            {
              "href" : "",
              "HostRoles" : {
                "cluster_name" : "c123",
                "component_name" : "NAMENODE",
                "host_name" : "c6402.ambari.apache.org"
              },
              "metrics" : {
                "dfs" : {
                  "FSNamesystem" : {
                    "HAState" : "standby",
                    "LastCheckpointTime" : 1435775648000
                  }
                }
              }
            }
          ]
        },
        result: 0
      },
      {
        m: "HA enabled, both NamoNodes are standby NN",
        data:
        {"href" : "",
          "ServiceComponentInfo" : {
            "cluster_name" : "c123",
            "component_name" : "NAMENODE",
            "service_name" : "HDFS"
          },
          "host_components" : [
            {
              "href" : "",
              "HostRoles" : {
                "cluster_name" : "c123",
                "component_name" : "NAMENODE",
                "host_name" : "c6401.ambari.apache.org"
              },
              "metrics" : {
                "dfs" : {
                  "FSNamesystem" : {
                    "HAState" : "standby",
                    "LastCheckpointTime" : 1435775648000
                  }
                }
              }
            },
            {
              "href" : "",
              "HostRoles" : {
                "cluster_name" : "c123",
                "component_name" : "NAMENODE",
                "host_name" : "c6402.ambari.apache.org"
              },
              "metrics" : {
                "dfs" : {
                  "FSNamesystem" : {
                    "HAState" : "standby",
                    "LastCheckpointTime" : 1435775648000
                  }
                }
              }
            }
          ]
        },
        result: 0
      },
      {
        m: "HA enabled, active NN has no JMX data, use the standby's data",
        data:
        {"href" : "",
          "ServiceComponentInfo" : {
            "cluster_name" : "c123",
            "component_name" : "NAMENODE",
            "service_name" : "HDFS"
          },
          "host_components" : [
            {
              "href" : "",
              "HostRoles" : {
                "cluster_name" : "c123",
                "component_name" : "NAMENODE",
                "host_name" : "c6401.ambari.apache.org"
              },
              "metrics" : {
                "dfs" : {
                  "FSNamesystem" : {
                    "HAState" : "active"
                  }
                }
              }
            },
            {
              "href" : "",
              "HostRoles" : {
                "cluster_name" : "c123",
                "component_name" : "NAMENODE",
                "host_name" : "c6402.ambari.apache.org"
              },
              "metrics" : {
                "dfs" : {
                  "FSNamesystem" : {
                    "HAState" : "standby",
                    "LastCheckpointTime" : 1435775648000
                  }
                }
              }
            }
          ]
        },
        result: 0
      },
      {
        m: "HA enabled, both NameNodes no JMX data",
        data:
        {"href" : "",
          "ServiceComponentInfo" : {
            "cluster_name" : "c123",
            "component_name" : "NAMENODE",
            "service_name" : "HDFS"
          },
          "host_components" : [
            {
              "href" : "",
              "HostRoles" : {
                "cluster_name" : "c123",
                "component_name" : "NAMENODE",
                "host_name" : "c6401.ambari.apache.org"
              },
              "metrics" : {
                "dfs" : {
                  "FSNamesystem" : {
                    "HAState" : "active"
                  }
                }
              }
            },
            {
              "href" : "",
              "HostRoles" : {
                "cluster_name" : "c123",
                "component_name" : "NAMENODE",
                "host_name" : "c6402.ambari.apache.org"
              },
              "metrics" : {
                "dfs" : {
                  "FSNamesystem" : {
                    "HAState" : "standby"
                  }
                }
              }
            }
          ]
        },
        result: 0
      }
    ];

    beforeEach(function () {
      sinon.stub(App, 'dateTime').returns(1435790048000);
      sinon.stub(App.HDFSService, 'find').returns([
        Em.Object.create({
          hostComponents: []
        })
      ]);
    });

    afterEach(function () {
      App.dateTime.restore();
      App.HDFSService.find.restore();
    });

    tests.forEach(function (test) {
      it(test.m, function () {
        var mainServiceItemController = App.MainServiceItemController.create({isNNCheckpointTooOld: null});
        mainServiceItemController.parseNnCheckPointTime(test.data, null, {});
        expect(mainServiceItemController.get('nameNodesWithOldCheckpoints.length')).to.equal(test.result);
      });
    });
  });


  describe("#isStartDisabled", function () {
    var tests = [
      {
        content: {
          isStarted: true
        },
        nonClientServiceComponents: [
          Em.Object.create({
            installedCount: 0
          })
        ],
        isPending: false,
        disabled: true,
        m: "disabled since service state is started and no component is stopped"
      },
      {
        content: {
          isStarted: true
        },
        nonClientServiceComponents: [
          Em.Object.create({
            installedCount: 0
          }),
          Em.Object.create({
            installedCount: 2
          })
        ],
        isPending: false,
        disabled: false,
        m: "enabled although service state is started, 2 components are stopped"
      },
      {
        content: {
          isStarted: false
        },
        nonClientServiceComponents: [
          Em.Object.create({
            installedCount: 0
          }),
          Em.Object.create({
            installedCount: 0
          })
        ],
        isPending: false,
        disabled: false,
        m: "enabled although all components are stopped service state is not started"
      },
      {
        content: {
          isStarted: true
        },
        nonClientServiceComponents: [
          Em.Object.create({
            installedCount: 0
          })
        ],
        isPending: true,
        disabled: true,
        m: "disabled since state is pending."
      },
    ];
    tests.forEach(function (test) {
      it(test.m, function () {
        var mainServiceItemController = App.MainServiceItemController.create({content: test.content, isPending: test.isPending, nonClientServiceComponents: test.nonClientServiceComponents});
        expect(mainServiceItemController.get('isStartDisabled')).to.equal(test.disabled);
      });
    });
  });

  describe("#isStopDisabled", function () {
    var tests = [
      {
        content: {
          isStarted: false
        },
        nonClientServiceComponents: [
          Em.Object.create({
            startedCount: 0
          })
        ],
        isPending: true,
        disabled: true,
        m: "disabled because of pending"
      },
      {
        content: {
          isStarted: true
        },
        nonClientServiceComponents: [
          Em.Object.create({
            startedCount: 0
          })
        ],
        isPending: false,
        disabled: false,
        m: "enabled because healthStatus is green although no components are started"
      },
      {
        content: {
          isStarted: false
        },
        nonClientServiceComponents: [
          Em.Object.create({
            startedCount: 1
          })
        ],
        isPending: false,
        disabled: false,
        m: "enabled because atleast 1 component is started."
      },
      {
        content: {
          isStarted: false
        },
        nonClientServiceComponents: [
          Em.Object.create({
            startedCount: 0
          })
        ],
        isPending: false,
        disabled: true,
        m: "disabled because healthStatus is not green and no started components"
      }
    ];
    tests.forEach(function (test) {
      it(test.m, function () {
        var mainServiceItemController = App.MainServiceItemController.create({content: test.content, isPending: test.isPending, nonClientServiceComponents: test.nonClientServiceComponents});
        expect(mainServiceItemController.get('isStopDisabled')).to.equal(test.disabled);
      });
    });
  });

  describe("#isHAWQStopDisabled", function () {
    var tests = [
      {
        content: {
          serviceName: 'HAWQ',
          healthStatus: 'green',
          hostComponents: [
            {
              componentName: 'HAWQMASTER',
              workStatus: 'STARTED'
            }, {
              componentName: 'HAWQSTANDBY',
              workStatus: 'STARTED'
            }]
        },
        isPending: true,
        disabled: true,
        m: "disabled because of pending"
      },
      {
        content: {
          serviceName: 'HAWQ',
          healthStatus: 'red',
          hostComponents: [
            {
              componentName: 'HAWQMASTER',
              workStatus: 'INSTALLED'
            }, {
              componentName: 'HAWQSTANDBY',
              workStatus: 'STARTED'
            }]
        },
        isPending: false,
        disabled: true,
        m: "disabled because HAWQMASTER is stopped and health is red"
      },
      {
        content: {
          serviceName: 'HAWQ',
          healthStatus: 'green',
          hostComponents: [
            {
              componentName: 'HAWQMASTER',
              workStatus: 'STARTED'
            }, {
              componentName: 'HAWQSTANDBY',
              workStatus: 'INSTALLED'
            }]
        },
        isPending: false,
        disabled: false,
        m: "enabled because HAWQMASTER is started"
      }
    ];
    tests.forEach(function (test) {
      it(test.m, function () {
        var mainServiceItemController = App.MainServiceItemController.create({content: test.content, isPending: test.isPending});
        expect(mainServiceItemController.get('isStopDisabled')).to.equal(test.disabled);
      });
    });
  });

  describe("#isPXFStopDisabled", function () {

    var hostComponentStub;

    before(function () {
      hostComponentStub = sinon.stub(App.HostComponent.find(), 'filterProperty');
    });
    after(function () {
      hostComponentStub.restore();
    });

    var tests = [
      {
        content: {
          serviceName: 'PXF',
        },
        isPending: false,
        pxfWorkstatus: [{"workStatus": "STARTED"}, {"workStatus": "STARTED"}],
        disabled: false,
        m: "Enabled because all agents are started."
      },
      {
        content: {
          serviceName: 'PXF',
        },
        isPending: false,
        pxfWorkstatus: [{"workStatus": "INSTALLED"}, {"workStatus": "STARTED"}],
        disabled: false,
        m: "Enabled because atleast one agent is started."
      },
      {
        content: {
          serviceName: 'PXF',
        },
        isPending: false,
        pxfWorkstatus: [{"workStatus": "INSTALLED"}, {"workStatus": "INSTALLED"}],
        disabled: true,
        m: "Disabled because all PXF agents are down."
      }
    ];
    tests.forEach(function (test) {
      it(test.m, function () {
        hostComponentStub.withArgs('componentName', 'PXF').returns(test.pxfWorkstatus);
        var mainServiceItemController = App.MainServiceItemController.create({content: test.content, isPending: test.isPending});
        expect(mainServiceItemController.get('isStopDisabled')).to.equal(test.disabled);
      });
    });
  });

  describe("#runRebalancer", function () {

    beforeEach(function () {
      sinon.stub(App.router, 'get', function(k) {
        if ('applicationController' === k) {
          return Em.Object.create({
            dataLoading: function() {
              return {done: Em.K}
            }
          });
        }
        return Em.get(App.router, k);
      });
    });

    afterEach(function () {
      App.router.get.restore();
    });

    it("run rebalancer", function () {
      var mainServiceItemController = App.MainServiceItemController.create({content: {runRebalancer: false}});
      mainServiceItemController.runRebalancer().onPrimary();
      expect(mainServiceItemController.get("content.runRebalancer")).to.equal(true);
    });
  });

  describe("#runCompaction", function () {

    beforeEach(function () {
      sinon.stub(App.router, 'get', function(k) {
        if ('applicationController' === k) {
          return Em.Object.create({
            dataLoading: function() {
              return {done: Em.K}
            }
          });
        }
        return Em.get(App.router, k);
      });
    });

    afterEach(function () {
      App.router.get.restore();
    });

    it("run compaction", function () {
      var mainServiceItemController = App.MainServiceItemController.create({content: {runCompaction: false}});
      mainServiceItemController.runCompaction().onPrimary();
      expect(mainServiceItemController.get("content.runCompaction")).to.equal(true);
    });
  });

  describe("#runSmokeTestPrimary", function () {
    beforeEach(function () {
      sinon.stub(App, 'get').withArgs('clusterName').returns('myCluster');
    });

    afterEach(function () {
      App.get.restore();
    });

    var tests = [
      {
        data: {
          'serviceName': "HDFS",
          'displayName': "HDFS",
          'query': "test"
        },
        "RequestInfo": {
          "context": "HDFS Service Check",
          "command" : "HDFS_SERVICE_CHECK"
        },
        "Requests/resource_filters": [{"service_name" : "HDFS"}]
      },
      {
        data: {
          'serviceName': "KERBEROS",
          'displayName': "Kerberos",
          'query': "test"
        },
        "RequestInfo": {
          "context": "Kerberos Service Check",
          "command" : "KERBEROS_SERVICE_CHECK",
          "operation_level": {
            "level": "CLUSTER",
            "cluster_name": "myCluster"
          }
        },
        "Requests/resource_filters": [{"service_name" : "KERBEROS"}]
      }
    ];

    tests.forEach(function (test) {

      var mainServiceItemController = App.MainServiceItemController.create({content: {serviceName: test.data.serviceName,
        displayName: test.data.displayName}});
      describe('send request to run smoke test for ' + test.data.serviceName, function () {

        beforeEach(function () {
          mainServiceItemController.set("runSmokeTestErrorCallBack", Em.K);
          mainServiceItemController.set("runSmokeTestSuccessCallBack", Em.K);
          mainServiceItemController.runSmokeTestPrimary(test.data.query);
          this.args = testHelpers.findAjaxRequest('name', 'service.item.smoke')[0];
          this.data = this.args.data;
          this.data = JSON.parse(App.ajax.fakeGetUrl('service.item.smoke').format(this.data).data);
        });

        it('ajax request is sent', function () {
          expect(this.args).exists;
        });

        it('RequestInfo.context is valid', function () {
          expect(this.data.RequestInfo.context).to.equal(test.RequestInfo.context);
        });

        it('RequestInfo.command is valid', function () {
          expect(this.data.RequestInfo.command).to.equal(test.RequestInfo.command);
        });

        it('Requests/resource_filter.0.serviceName is valid', function () {
          expect(this.data["Requests/resource_filters"][0].serviceName).to.equal(test["Requests/resource_filters"][0].serviceName);
        });

        it('RequestInfo.operation_level is valid', function () {
          expect(this.data.RequestInfo.operation_level).to.be.deep.equal(test.RequestInfo.operation_level);
        });

      });
    });
  });

  describe('#downloadClientConfigs()', function () {

    var mainServiceItemController = App.MainServiceItemController.create({
      content: {
        clientComponents: [
          Em.Object.create({
            totalCount: 1,
            componentName: 'C1',
            displayName: 'd1'
          })
        ],
        serviceName: 'S1'
      }
    });

    beforeEach(function () {
      sinon.stub(mainServiceItemController, 'downloadClientConfigsCall', Em.K);
    });
    afterEach(function () {
      mainServiceItemController.downloadClientConfigsCall.restore();
    });

    it('should launch $.fileDownload method', function () {
      mainServiceItemController.downloadClientConfigs();
      expect(mainServiceItemController.downloadClientConfigsCall.calledWith({
        serviceName: 'S1',
        componentName: 'C1',
        resourceType: mainServiceItemController.resourceTypeEnum.SERVICE_COMPONENT
      })).to.be.true;
    });
    it('should launch $.fileDownload method, event passed', function () {
      var event = {
        label: 'label1',
        name: 'name1'
      };
      mainServiceItemController.downloadClientConfigs(event);
      expect(mainServiceItemController.downloadClientConfigsCall.calledWith({
        serviceName: 'S1',
        componentName: 'name1',
        resourceType: mainServiceItemController.resourceTypeEnum.SERVICE_COMPONENT
      })).to.be.true;
    });
  });

  describe('#downloadAllClientConfigs()', function () {

    var mainServiceItemController = App.MainServiceItemController.create({
      content: {
        clientComponents: [
          Em.Object.create({
            totalCount: 1,
            componentName: 'C1',
            displayName: 'd1'
          }),
          Em.Object.create({
            totalCount: 1,
            componentName: 'C2',
            displayName: 'd2'
          })
        ],
        serviceName: 'S1'
      }
    });

    beforeEach(function () {
      sinon.stub(mainServiceItemController, 'downloadClientConfigsCall', Em.K);
    });
    afterEach(function () {
      mainServiceItemController.downloadClientConfigsCall.restore();
    });

    it('should call downloadClientConfigsCall method for all clients', function () {
      mainServiceItemController.downloadAllClientConfigs();
      expect(mainServiceItemController.downloadClientConfigsCall.calledWith({
        serviceName: 'S1',
        resourceType: mainServiceItemController.resourceTypeEnum.SERVICE
      })).to.be.true;
    });
  });

  describe('#startLdapKnox() and #stopLdapKnox() should call startStopLdapKnox once: ', function () {


    var mainServiceItemController = App.MainServiceItemController.create({content: {serviceName: 'KNOX',
      displayName: 'Knox'}});

    beforeEach(function () {
      sinon.stub(mainServiceItemController, 'startStopLdapKnox', function(){
        return true;
      });
    });
    afterEach(function () {
      mainServiceItemController.startStopLdapKnox.restore();
    });

    var tests = [
      {
        methodName: 'startLdapKnox',
        callback: mainServiceItemController.startLdapKnox
      },
      {
        methodName: 'stopLdapKnox',
        callback: mainServiceItemController.stopLdapKnox
      }
    ];

    tests.forEach(function(test){
      it(test.methodName + ' should call startStopLdapKnox method', function () {
        test.callback.call(mainServiceItemController);
        expect(mainServiceItemController.startStopLdapKnox.calledOnce).to.be.true;
      });
    },this);

  });

  describe("#executeCustomCommand", function () {
    var data = {
      data: {
        'serviceName': "SAMPLESRV",
        'displayName': "SAMPLESRV",
        'query': "test"
      },
      "RequestInfo": {
        "context": "Execute Custom Commands",
        "command" : "SAMPLESRVCUSTOMCOMMANDS"
      },
      "Requests/resource_filters": [{"service_name" : "SAMPLESRV"}]
    };

    var context = {
      label: 'Execute Custom Commands',
      service: data.data.serviceName,
      component: data.data.serviceName,
      command: data.RequestInfo.command
    };

    var mainServiceItemController = App.MainServiceItemController.create({
      content: {
        serviceName: data.data.serviceName,
        displayName: data.data.displayName
      }
    });

    before(function () {
      mainServiceItemController.set("executeCustomCommandErrorCallback", Em.K);
      mainServiceItemController.set("executeCustomCommandSuccessCallback", Em.K);
      sinon.spy(App, 'showConfirmationPopup');
    });

    after(function () {
      App.showConfirmationPopup.restore();
    });

    it('shows a confirmation popup', function () {
      mainServiceItemController.executeCustomCommand(context);
      expect(App.showConfirmationPopup.calledOnce).to.equal(true);
    });
  });

  describe("#findDependentServices()", function() {
    var mainServiceItemController;

    beforeEach(function() {
      mainServiceItemController = App.MainServiceItemController.create({});
      sinon.stub(App.StackService, 'find', function (serviceName) {
        return stackServiceModel[serviceName];
      });
      this.mockService = sinon.stub(App.Service, 'find');
    });
    afterEach(function() {
      App.StackService.find.restore();
      this.mockService.restore();
    });

    it("no services", function() {
      this.mockService.returns([]);
      expect(mainServiceItemController.findDependentServices(['S1'])).to.be.empty;
    });

    it("service has dependencies", function() {
      this.mockService.returns([
        Em.Object.create({ serviceName: 'HDFS' }),
        Em.Object.create({ serviceName: 'YARN' }),
        Em.Object.create({ serviceName: 'MAPREDUCE2' }),
        Em.Object.create({ serviceName: 'TEZ' }),
        Em.Object.create({ serviceName: 'HIVE' })
      ]);
      expect(mainServiceItemController.findDependentServices(['YARN', 'MAPREDUCE2'])).to.eql(['TEZ', 'HIVE']);
    });

    it("service has no dependencies", function() {
       this.mockService.returns([
         Em.Object.create({ serviceName: 'HDFS' }),
         Em.Object.create({ serviceName: 'YARN' }),
         Em.Object.create({ serviceName: 'MAPREDUCE2' }),
         Em.Object.create({ serviceName: 'TEZ' }),
         Em.Object.create({ serviceName: 'HIVE' })
      ]);
      expect(mainServiceItemController.findDependentServices(['HIVE'])).to.be.empty;
    });

    it("service has no dependencies (except interdependent)", function() {
      this.mockService.returns([
        Em.Object.create({ serviceName: 'HDFS' }),
        Em.Object.create({ serviceName: 'YARN' }),
        Em.Object.create({ serviceName: 'MAPREDUCE2' })
      ]);
      expect(mainServiceItemController.findDependentServices(['YARN', 'MAPREDUCE2'])).to.be.empty;
    });

  });

  describe("#deleteService()", function() {
    var mainServiceItemController;

    beforeEach(function() {
      mainServiceItemController = App.MainServiceItemController.create({});
      this.mockDependentServices = sinon.stub(mainServiceItemController, 'findDependentServices');
      sinon.stub(mainServiceItemController, 'dependentServicesWarning');
      sinon.stub(mainServiceItemController, 'servicesDisplayNames', function(servicesDisplayNames) {
        return servicesDisplayNames;
      });
      this.allowUninstallServices = sinon.stub(mainServiceItemController, 'allowUninstallServices');
      this.mockService = sinon.stub(App.Service, 'find');
      this.mockRangerPluginEnabled = sinon.stub(mainServiceItemController, 'isRangerPluginEnabled');
      sinon.stub(App, 'showConfirmationPopup');
      sinon.stub(App.format, 'role', function(name) {return name});
      sinon.stub(mainServiceItemController, 'kerberosDeleteWarning');
      sinon.stub(mainServiceItemController, 'showLastWarning');

      mainServiceItemController.reopen({
        interDependentServices: []
      })
    });
    afterEach(function() {
      mainServiceItemController.allowUninstallServices.restore();
      mainServiceItemController.servicesDisplayNames.restore();
      this.mockDependentServices.restore();
      this.mockService.restore();
      mainServiceItemController.dependentServicesWarning.restore();
      App.showConfirmationPopup.restore();
      App.format.role.restore();
      mainServiceItemController.kerberosDeleteWarning.restore();
      this.mockRangerPluginEnabled.restore();
    });

    it("Kerberos delete should show specific warning", function() {
      mainServiceItemController.deleteService('KERBEROS');
      expect(mainServiceItemController.kerberosDeleteWarning.
        calledWith(Em.I18n.t('services.service.delete.popup.header'))).to.be.true;
    });

    it("RANGER delete should show specific warning", function() {
      this.mockRangerPluginEnabled.returns(true);
      mainServiceItemController.deleteService('RANGER');
      expect(App.ModalPopup.show.calledWith({
        secondary: null,
        header: Em.I18n.t('services.service.delete.popup.header'),
        encodeBody: false,
        body: Em.I18n.t('services.service.delete.popup.ranger')
      })).to.be.true;
    });

    it("only one service installed", function() {
      this.mockDependentServices.returns(['S2']);
      this.mockService.returns(Em.Object.create({length: 1}));
      mainServiceItemController.deleteService('S1');
      expect(App.ModalPopup.show.calledWith({
        secondary: null,
        header: Em.I18n.t('services.service.delete.popup.header'),
        encodeBody: false,
        body: Em.I18n.t('services.service.delete.lastService.popup.body').format('S1')
      })).to.be.true;
    });

    it("service has installed dependent services", function() {
      this.mockDependentServices.returns(['S2']);
      this.mockService.returns([Em.Object.create({workStatus: App.Service.statesMap.stopped}), Em.Object.create({workStatus: App.Service.statesMap.stopped})]);
      mainServiceItemController.deleteService('S1');
      expect(mainServiceItemController.dependentServicesWarning.calledWith('S1', ['S2'])).to.be.true;
    });

    it("service has not dependent services, and stopped", function() {
      this.mockDependentServices.returns([]);
      this.allowUninstallServices.returns(true);
      this.mockService.returns([Em.Object.create({workStatus: App.Service.statesMap.stopped}), Em.Object.create({workStatus: App.Service.statesMap.stopped})]);
      mainServiceItemController.deleteService('S1');
      expect(mainServiceItemController.showLastWarning.calledOnce).to.be.true;
    });

    it("service has not dependent services, and install failed", function() {
      this.mockDependentServices.returns([]);
      this.allowUninstallServices.returns(true);
      this.mockService.returns([Em.Object.create({workStatus: App.Service.statesMap.install_failed}), Em.Object.create({workStatus: App.Service.statesMap.install_failed})]);
      mainServiceItemController.deleteService('S1');
      expect(mainServiceItemController.showLastWarning.calledOnce).to.be.true;
    });

    it("service has not dependent services, and not stopped", function() {
      this.mockDependentServices.returns([]);
      this.mockService.returns(Em.Object.create({workStatus: App.Service.statesMap.started}));
      mainServiceItemController.deleteService('S1');
      expect(App.ModalPopup.show.calledWith({
        secondary: null,
        header: Em.I18n.t('services.service.delete.popup.header'),
        encodeBody: false,
        body: Em.I18n.t('services.service.delete.popup.mustBeStopped').format('S1')
      })).to.be.true;
    });
  });

  describe("#kerberosDeleteWarning()", function () {
    var mainServiceItemController;

    beforeEach(function() {
      mainServiceItemController = App.MainServiceItemController.create({});
      sinon.stub(App.router, 'transitionTo');
    });

    afterEach(function() {
      App.router.transitionTo.restore();
    });

    it("App.ModalPopup.show should be called", function() {
      var popup = mainServiceItemController.kerberosDeleteWarning('header');
      expect(App.ModalPopup.show.calledOnce).to.be.true;
      popup.onSecondary();
      expect(App.router.transitionTo.calledWith('main.admin.adminKerberos.index')).to.be.true;
    });
  });

  describe("#dependentServicesWarning()", function() {
    var mainServiceItemController;

    beforeEach(function() {
      mainServiceItemController = App.MainServiceItemController.create({});
      sinon.stub(App.format, 'role', function(name) {return name});
    });
    afterEach(function() {
      App.format.role.restore();
    });

    it("App.ModalPopup.show should be called", function() {
      mainServiceItemController.dependentServicesWarning('S1', ['S2']);
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });
  });

  describe("#confirmDeleteService()", function() {
    var mainServiceItemController;

    beforeEach(function () {
      App.ModalPopup.show.restore();
      mainServiceItemController = App.MainServiceItemController.create();
    });

    describe('confirmation popup', function () {

      beforeEach(function () {
        sinon.stub(App.ModalPopup, 'show', Em.K);
        mainServiceItemController.confirmDeleteService('S1', [], '');
      });

      it("App.ModalPopup.show should be called", function() {
        expect(App.ModalPopup.show.calledOnce).to.be.true;
      });

    });

    describe('progress popup', function () {

      var cases = [
        {
          serviceName: 'S0',
          dependentServiceNames: [],
          serviceNames: ['S0'],
          message: 's0',
          title: 'no dependent services'
        },
        {
          serviceName: 'S1',
          dependentServiceNames: ['S2', 'S3', 'S4'],
          serviceNames: ['S1', 'S2', 'S3', 'S4'],
          message: 's1, s2, s3 and s4',
          title: 'dependent services present'
        }
      ];

      cases.forEach(function (item) {

        describe(item.title, function () {

          beforeEach(function () {
            sinon.stub(App.ModalPopup, 'show', function (options) {
              options._super = Em.K;
              if (options.onPrimary) {
                options.onPrimary();
              }
              return options;
            });
            sinon.stub(App.Service, 'find', function (serviceName) {
              return Em.Object.create({
                displayName: serviceName.toLowerCase()
              });
            });
            sinon.stub(mainServiceItemController, 'deleteServiceCall', Em.K);
            mainServiceItemController.confirmDeleteService(item.serviceName, item.dependentServiceNames, '');
          });

          afterEach(function () {
            App.Service.find.restore();
            mainServiceItemController.deleteServiceCall.restore();
          });

          it('modal popups display', function () {
            expect(App.ModalPopup.show.calledTwice).to.be.true;
          });

          it('progress popup message', function () {
            expect(mainServiceItemController.get('deleteServiceProgressPopup.message')).to.equal(Em.I18n.t('services.service.delete.progressPopup.message').format(item.message));
          });

          it('delete service call', function () {
            expect(mainServiceItemController.deleteServiceCall.calledOnce).to.be.true;
          });

          it('delete service call arguments', function () {
            expect(mainServiceItemController.deleteServiceCall.calledWith(item.serviceNames)).to.be.true;
          });

          it('progress popup close', function () {
            mainServiceItemController.get('deleteServiceProgressPopup').onClose();
            expect(mainServiceItemController.get('deleteServiceProgressPopup')).to.be.null;
          });

        });

      });

    });

  });

  describe('#interDependentServices', function() {
    var mainServiceItemController;

    beforeEach(function() {
      sinon.stub(App.StackService, 'find', function (serviceName) {
        return stackServiceModel[serviceName];
      });
      mainServiceItemController = App.MainServiceItemController.create({
        content: {}
      });
    });

    afterEach(function() {
      App.StackService.find.restore();
    });

    it('get interdependent services for YARN', function() {
      mainServiceItemController.set('content', Em.Object.create({
        serviceName: 'YARN'
      }));
      expect(mainServiceItemController.get('interDependentServices')).to.eql(['MAPREDUCE2']);
    });

    it('get interdependent services for MAPREDUCE2', function() {
      mainServiceItemController.set('content', Em.Object.create({
        serviceName: 'MAPREDUCE2'
      }));
      expect(mainServiceItemController.get('interDependentServices')).to.eql(['YARN']);
    });
  });

  describe("#deleteServiceCall()", function() {
    var mainServiceItemController;
    var service;

    beforeEach(function() {
      mainServiceItemController = App.MainServiceItemController.create({});
      service = Em.Object.create({serviceName: 'S1', deleteInProgress: false});
      sinon.stub( App.Service, 'find', function () {
        return [service];
      });
      mainServiceItemController.deleteServiceCall(['S1', 'S2']);
    });

    afterEach(function () {
      App.Service.find.restore();
    });

    it("App.ajax.send should be called", function() {
      var args = testHelpers.findAjaxRequest('name', 'common.delete.service');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(mainServiceItemController);
      expect(args[0].data).to.be.eql({
        serviceName : 'S1',
        servicesToDeleteNext: ['S2']
      });
    });

    it('service is marked as deleted', function () {
      expect(service.get('deleteInProgress')).to.be.true;
    });

  });

  describe("#deleteServiceCallSuccessCallback()", function() {
    var mainServiceItemController;

    beforeEach(function() {
      mainServiceItemController = App.MainServiceItemController.create({});
      sinon.stub(mainServiceItemController, 'saveConfigs', Em.K);
      sinon.stub(mainServiceItemController, 'deleteServiceCall', Em.K);
      mainServiceItemController.reopen({
        interDependentServices: []
      })
    });
    afterEach(function() {
      mainServiceItemController.saveConfigs.restore();
      mainServiceItemController.deleteServiceCall.restore();
    });

    it("saveConfigs should be called", function() {
      mainServiceItemController.deleteServiceCallSuccessCallback([], null, {});
      expect(mainServiceItemController.deleteServiceCall.called).to.be.false;
      expect(mainServiceItemController.saveConfigs.calledOnce).to.be.true;
    });

    it("deleteServiceCall should be called", function() {
      mainServiceItemController.deleteServiceCallSuccessCallback([], null, {servicesToDeleteNext: true});
      expect(mainServiceItemController.deleteServiceCall.calledOnce).to.be.true;
      expect(mainServiceItemController.saveConfigs.called).to.be.false;
    });
  });

  describe("#restartLLAP()", function () {
    var mainServiceItemController;

    beforeEach(function () {
      mainServiceItemController = App.MainServiceItemController.create();
      sinon.stub(mainServiceItemController, 'restartLLAPAndRefreshQueueRequest', Em.K);
      sinon.stub(mainServiceItemController, 'restartLLAPRequest', Em.K);
      this.mockService = sinon.stub(App.Service, 'find');
    });
    afterEach(function () {
      mainServiceItemController.restartLLAPAndRefreshQueueRequest.restore();
      mainServiceItemController.restartLLAPRequest.restore();
      this.mockService.restore();
    });

    [
      {
        m: 'should call only restartLLAPRequest',
        isRestartRequired: false,
        toCall: 'restartLLAPRequest'
      },
      {
        m: 'should call only restartLLAPAndRefreshQueueRequest',
        isRestartRequired: true,
        toCall: 'restartLLAPAndRefreshQueueRequest'
      }
    ].forEach(function (test) {
        it(test.m, function () {
          this.mockService.returns(Em.Object.create({
            serviceName: 'YARN',
            isRestartRequired: test.isRestartRequired
          }));
          var confirmationPopup = mainServiceItemController.restartLLAP();
          confirmationPopup.onPrimary();
          expect(mainServiceItemController[test.toCall].calledOnce).to.be.true;
        });
      });
  });

  describe("#saveConfigs()", function () {
    var mainServiceItemController;

    beforeEach(function () {
      mainServiceItemController = App.MainServiceItemController.create();
      sinon.stub(mainServiceItemController, 'getServiceConfigToSave').returns({});
      sinon.stub(mainServiceItemController, 'putChangedConfigurations');
      sinon.stub(mainServiceItemController, 'confirmServiceDeletion');
    });

    afterEach(function () {
      mainServiceItemController.getServiceConfigToSave.restore();
      mainServiceItemController.putChangedConfigurations.restore();
      mainServiceItemController.confirmServiceDeletion.restore();
    });

    it("empty stepConfigs", function() {
      mainServiceItemController.set('stepConfigs', []);
      mainServiceItemController.saveConfigs();
      expect(mainServiceItemController.confirmServiceDeletion.calledOnce).to.be.true;
      expect(mainServiceItemController.putChangedConfigurations.called).to.be.false;
    });

    it("stepConfigs has configs", function() {
      mainServiceItemController.set('stepConfigs', [Em.Object.create({serviceName: 'S1'})]);
      mainServiceItemController.saveConfigs();
      expect(mainServiceItemController.putChangedConfigurations.calledWith([{}], 'confirmServiceDeletion')).to.be.true;
      expect(mainServiceItemController.confirmServiceDeletion.called).to.be.false;
    });
  });

  describe('#confirmServiceDeletion', function () {

    var mainServiceItemController = App.MainServiceItemController.create({deleteServiceProgressPopup: null});

    [
      {
        content: Em.Object.create({serviceName: 'DRUID'}),
        stackServices: [
          Em.Object.create({id: 'DRUID', displayName: 'Druid', serviceName: 'DRUID', requiredServices: []})
        ],
        m: 'No required services',
        e: Em.I18n.t('services.service.delete.service.success.confirmation').format('Druid')
      },
      {
        content: Em.Object.create({serviceName: 'MAPREDUCE2'}),
        stackServices: [
          Em.Object.create({id: 'MAPREDUCE2', serviceName: 'MAPREDUCE2', requiredServices: ['YARN'], displayName: 'MapReduce2'}),
          Em.Object.create({id: 'YARN', serviceName: 'YARN', requiredServices: ['MAPREDUCE2'], displayName: 'YARN'}),
        ],
        m: 'One required service',
        e: Em.I18n.t('services.service.delete.service.success.confirmation.plural').format('MapReduce2, YARN')
      }
    ].forEach(function(test) {
      describe(test.m, function () {

        beforeEach(function () {
          sinon.stub(App.StackService, 'find', function (id) {
            return arguments.length ? test.stackServices.findProperty('id', id) : test.stackServices;
          });
          sinon.spy(App, 'showAlertPopup');
          mainServiceItemController.set('content', test.content);
          mainServiceItemController.confirmServiceDeletion();
        });

        afterEach(function () {
          App.StackService.find.restore();
          App.showAlertPopup.restore();
        });

        it('Popup body has display service names', function () {
          expect(App.showAlertPopup.args[0][1]).to.be.equal(test.e);
        });

      });
    });

  });

  describe("#isRangerPluginEnabled()", function () {
    var mainServiceItemController;

    beforeEach(function() {
      mainServiceItemController = App.MainServiceItemController.create();
      this.mock = sinon.stub(App.router, 'get');
    });

    afterEach(function() {
      this.mock.restore();
    });

    it("should return false", function() {
      this.mock.returns([Em.Object.create({
        isDisplayed: true,
        status: 'Disabled'
      })]);
      expect(mainServiceItemController.isRangerPluginEnabled()).to.be.false;
    });

    it("should return true", function() {
      this.mock.returns([Em.Object.create({
        isDisplayed: true,
        status: 'Enabled'
      })]);
      expect(mainServiceItemController.isRangerPluginEnabled()).to.be.true;
    });
  });

  describe('#dependentServiceNames', function () {

    var controller,
      serviceName = 's0',
      dependentServiceNames = ['s1', 's2'],
      testCases = [
        {
          isConfigsPropertiesLoaded: true,
          dependentServiceNames: dependentServiceNames,
          message: 'model is ready'
        },
        {
          isConfigsPropertiesLoaded: false,
          dependentServiceNames: [],
          message: 'model is not ready'
        }
      ];

    beforeEach(function () {
      controller = App.MainServiceItemController.create({
        content: {
          serviceName: serviceName
        }
      });
      sinon.stub(App.StackService, 'find').returns(Em.Object.create({
        dependentServiceNames: dependentServiceNames
      }));
    });

    afterEach(function () {
      App.StackService.find.restore();
    });

    testCases.forEach(function (test) {

      it(test.message, function () {
        App.set('router.clusterController.isConfigsPropertiesLoaded', test.isConfigsPropertiesLoaded);
        expect(controller.get('dependentServiceNames')).to.eql(test.dependentServiceNames);
      });

    });

  });

  describe('#applyRecommendedValues', function () {

    var controller;
    var configsS1;
    var configsS2;

      beforeEach(function () {
      controller = App.MainServiceItemController.create({
        stepConfigs: [
          Em.Object.create({
            serviceName: 's1',
            configs: [
              Em.Object.create({
                name: 'p1',
                value: 'v1'
              }),
              Em.Object.create({
                name: 'p2',
                value: 'v2'
              })
            ]
          }),
          Em.Object.create({
            serviceName: 's2',
            configs: [
              Em.Object.create({
                name: 'p3',
                value: 'v3'
              }),
              Em.Object.create({
                name: 'p4',
                value: 'v4'
              })
            ]
          })
        ],
        changedProperties: [
          {
            serviceName: 's1',
            propertyName: 'p1',
            recommendedValue: 'r1',
            initialValue: 'i1',
            saveRecommended: false
          },
          {
            serviceName: 's1',
            propertyName: 'p2',
            recommendedValue: 'r2',
            initialValue: 'i2',
            saveRecommended: true
          },
          {
            serviceName: 's2',
            propertyName: 'p3',
            recommendedValue: 'r3',
            initialValue: 'i3',
            saveRecommended: true
          }
        ]
      });
    });

    beforeEach(function () {
      controller.applyRecommendedValues(controller.get('stepConfigs'));
      configsS1 = controller.get('stepConfigs').findProperty('serviceName', 's1').get('configs');
      configsS2 = controller.get('stepConfigs').findProperty('serviceName', 's2').get('configs');
    });

    it('should update properties with saveRecommended flag set to true', function () {
      expect(configsS1.findProperty('name', 'p1').get('value')).to.equal('i1');
      expect(configsS1.findProperty('name', 'p2').get('value')).to.equal('r2');
      expect(configsS2.findProperty('name', 'p3').get('value')).to.equal('r3');
      expect(configsS2.findProperty('name', 'p4').get('value')).to.equal('v4');
    });

  });

  describe('#startStopPopupErrorCallback', function () {

    beforeEach(function () {
      sinon.spy(App.ajax, 'defaultErrorHandler');
    });

    afterEach(function () {
      App.ajax.defaultErrorHandler.restore();
    });

    it('`App.ajax.defaultErrorHandler` should be called', function () {
      App.MainServiceItemController.create().startStopPopupErrorCallback({}, {}, '', {}, {query: Em.Object.create()});
      expect(App.ajax.defaultErrorHandler.calledOnce).to.be.true;
    });

  });
  
  describe('#clientComponents', function() {
    beforeEach(function() {
      sinon.stub(App.StackServiceComponent, 'find').returns([
        Em.Object.create({
          componentName: 'C1',
          displayName: 'c1',
          serviceName: 'S1',
          isClient: true
        })
      ]);
    });
    afterEach(function() {
      App.StackServiceComponent.find.restore();
    });
    
    it('should return list of clients', function() {
      var ctrl = App.MainServiceItemController.create({
        content: {
          serviceName: 'S1'
        }
      });
      ctrl.propertyDidChange('clientComponents');
      expect(ctrl.get('clientComponents')).to.be.eql([
        {
          action:  'downloadAllClientConfigs',
          context: {
            label: Em.I18n.t('common.all.clients')
          }
        },
        {
          action: 'downloadClientConfigs',
          context: {
            name: 'C1',
            label: 'c1'
          }
        }
      ]);
    });
  });
  
  describe('#sitesToLoad', function() {
    beforeEach(function() {
      sinon.stub(App.StackService, 'find').returns([
        Em.Object.create({
          serviceName: 'S1',
          configTypeList: ['type1']
        })
      ]);
    });
    afterEach(function() {
      App.StackService.find.restore();
    });
    
    it('should return sites', function() {
      var ctrl = App.MainServiceItemController.create({
        content: {
          serviceName: 'S1'
        },
        configDependentServiceNames: ['S1'],
        serviceConfigsMap: {
          'S1': ['type2']
        }
      });
      ctrl.propertyDidChange('sitesToLoad');
      expect(ctrl.get('sitesToLoad')).to.be.eql(['type1', 'type2', 'cluster-env']);
    });
  });
  
  describe('#startStopPopupSuccessCallback', function() {
    var ctrl;
    var mock = {
      dataLoading: function() {
        return {
          done: function(callback) {
            callback(true);
          }
        }
      },
      showPopup: sinon.spy()
    };
    beforeEach(function() {
      ctrl = App.MainServiceItemController.create();
      sinon.stub(App.router, 'get').returns(mock);
    });
    afterEach(function() {
      App.router.get.restore();
    });
    
    it('showPopup should be called when call successful', function() {
      var params = {query: Em.Object.create()};
      ctrl.startStopPopupSuccessCallback({Requests: []}, {}, params);
      expect(params.query.get('status')).to.be.equal('SUCCESS');
      expect(mock.showPopup.calledOnce).to.be.true;
    });
    it('should set FAIL status when call failed', function() {
      var params = {query: Em.Object.create()};
      ctrl.startStopPopupSuccessCallback(null, {}, params);
      expect(params.query.get('status')).to.be.equal('FAIL');
    });
  });
  
  describe('#checkNnLastCheckpointTime', function() {
    var ctrl;
    var callback = sinon.spy();
    beforeEach(function() {
      ctrl = App.MainServiceItemController.create();
      sinon.stub(ctrl, 'pullNnCheckPointTime').returns({complete: Em.clb});
      sinon.stub(ctrl, 'getHdfsUser').returns({done: Em.clb});
      sinon.stub(ctrl, 'getMessageForOldCheckpoints').returns('message');
      sinon.stub(App, 'showConfirmationFeedBackPopup');
      sinon.stub(App, 'showConfirmationPopup');
    });
    afterEach(function() {
      ctrl.pullNnCheckPointTime.restore();
      ctrl.getHdfsUser.restore();
      ctrl.getMessageForOldCheckpoints.restore();
      App.showConfirmationFeedBackPopup.restore();
      App.showConfirmationPopup.restore();
    });
    
    it('showConfirmationFeedBackPopup should be called', function() {
      ctrl.set('nameNodesWithOldCheckpoints', [{}]);
      ctrl.checkNnLastCheckpointTime(callback);
      expect(App.showConfirmationFeedBackPopup.calledWith(callback, 'message')).to.be.true;
    });
    it('callback should be called', function() {
      ctrl.set('nameNodesWithOldCheckpoints', []);
      ctrl.set('isNameNodeCheckpointUnavailable', false);
      ctrl.checkNnLastCheckpointTime(callback);
      expect(callback.calledOnce).to.be.true;
    });
    it('showConfirmationPopup should be called', function() {
      ctrl.set('nameNodesWithOldCheckpoints', []);
      ctrl.set('isNameNodeCheckpointUnavailable', true);
      ctrl.checkNnLastCheckpointTime(callback);
      expect(App.showConfirmationPopup.calledWith(
        callback, Em.I18n.t('services.service.stop.HDFS.warningMsg.checkPointNA'), null,
        Em.I18n.t('common.warning'), Em.I18n.t('common.proceedAnyway'), 'danger')
      ).to.be.true;
    });
  });
  
  describe('#pullNnCheckPointTime', function() {
    beforeEach(function() {
      sinon.stub(App.HDFSService, 'find').returns([
        Em.Object.create({
          hostComponents: [
            Em.Object.create({
              haNameSpace: 'ha',
              clusterIdValue: 'c1'
            })
          ]
        })
      ]);
    });
    afterEach(function() {
      App.HDFSService.find.restore();
    });
    
    it('App.ajax.send should be called', function() {
      var ctrl = App.MainServiceItemController.create();
      ctrl.pullNnCheckPointTime('ha');
      expect(testHelpers.findAjaxRequest('name', 'common.service.hdfs.getNnCheckPointTime')[0].data).to.be.eql({
        clusterIdValue: 'c1'
      });
    });
  });
  
  describe('#startStopWithMmode', function() {
    var ctrl;
    beforeEach(function() {
      ctrl = App.MainServiceItemController.create({
        content: {
          serviceName: 's1'
        }
      });
      sinon.stub(ctrl, 'startStopPopupPrimary').returns({
        complete: Em.clb
      });
      sinon.stub(batchUtils, 'turnOnOffPassiveRequest').returns({
        complete: Em.clb
      });
    });
    afterEach(function() {
      ctrl.startStopPopupPrimary.restore();
      batchUtils.turnOnOffPassiveRequest.restore();
    });
    
    it('turnOnOffPassiveRequest should be called when turning off maintenance', function() {
      ctrl.startStopWithMmode('STARTED', '', true, [], [], '');
      expect(batchUtils.turnOnOffPassiveRequest.calledWith('OFF', Em.I18n.t('passiveState.turnOff'), 'S1')).to.be.true;
    });
    it('startStopPopupPrimary should be called when turning on maintenance', function() {
      ctrl.startStopWithMmode('INSTALLED', '', true, [], [], '');
      expect(ctrl.startStopPopupPrimary.calledWith('INSTALLED', '', [], [], '')).to.be.true;
    });
    it('startStopPopupPrimary should be called', function() {
      ctrl.startStopWithMmode('INSTALLED', '', false, [], [], '');
      expect(ctrl.startStopPopupPrimary.calledWith('INSTALLED', '', [], [], '')).to.be.true;
    });
  });
  
  describe('#refreshYarnQueues', function() {
    beforeEach(function() {
      sinon.stub(App, 'showConfirmationPopup', Em.clb);
      sinon.stub(App.MasterComponent, 'find').returns(Em.Object.create({
        hostNames: ['host1']
      }));
    });
    afterEach(function() {
      App.showConfirmationPopup.restore();
      App.MasterComponent.find.restore();
    });
    
    it('App.ajax.send should be called', function() {
      var ctrl = App.MainServiceItemController.create();
      ctrl.refreshYarnQueues();
      expect(testHelpers.findAjaxRequest('name', 'service.item.refreshQueueYarnRequest')[0].data).to.be.eql({
        command: "REFRESHQUEUES",
        context: Em.I18n.t('services.service.actions.run.yarnRefreshQueues.context'),
        hosts: 'host1',
        serviceName: "YARN",
        componentName: "RESOURCEMANAGER",
        forceRefreshConfigTags: "capacity-scheduler"
      });
    });
  });
  
  describe('#refreshYarnQueuesSuccessCallback', function() {
    beforeEach(function() {
      sinon.stub(App.router, 'get').returns({showPopup: sinon.spy()});
    });
    afterEach(function() {
      App.router.get.restore();
    });
    
    it('showPopup should be called', function() {
      var ctrl = App.MainServiceItemController.create();
      ctrl.refreshYarnQueuesSuccessCallback({Requests: {id: 1}});
      expect(App.router.get('backgroundOperationsController').showPopup.calledOnce).to.be.true;
    });
  });
  
  describe('#refreshYarnQueuesErrorCallback', function() {
    beforeEach(function() {
      sinon.stub(App, 'showAlertPopup');
    });
    afterEach(function() {
      App.showAlertPopup.restore();
    });
    
    it('showAlertPopup should be called', function() {
      var ctrl = App.MainServiceItemController.create();
      ctrl.refreshYarnQueuesErrorCallback({
        responseText: JSON.stringify({
          message: 'hello'
        })
      });
      expect(App.showAlertPopup.calledWith(
        Em.I18n.t('services.service.actions.run.yarnRefreshQueues.error'),
        Em.I18n.t('services.service.actions.run.yarnRefreshQueues.error') + 'hello'
      )).to.be.true;
    });
  });
  
  describe('#startStopLdapKnox', function() {
    beforeEach(function() {
      sinon.stub(App.HostComponent, 'find').returns([
        Em.Object.create({
          componentName: 'KNOX_GATEWAY',
          hostName: 'host1'
        })
      ]);
      sinon.stub(App, 'showConfirmationPopup', Em.clb);
    });
    afterEach(function() {
      App.HostComponent.find.restore();
      App.showConfirmationPopup.restore();
    });
    
    it('App.ajax.send should be called after confirmation', function() {
      var ctrl = App.MainServiceItemController.create();
      ctrl.startStopLdapKnox('command1', 'context1');
      expect(testHelpers.findAjaxRequest('name', 'service.item.startStopLdapKnox')[0].data).to.be.eql({
        command: 'command1',
        context: 'context1',
        host: 'host1',
        serviceName: "KNOX",
        componentName: "KNOX_GATEWAY"
      });
    });
  });
  
  describe('#startStopLdapKnoxSuccessCallback', function() {
    beforeEach(function() {
      sinon.stub(App.router, 'get').returns({showPopup: sinon.spy()});
    });
    afterEach(function() {
      App.router.get.restore();
    });
    
    it('showPopup should be called', function() {
      var ctrl = App.MainServiceItemController.create();
      ctrl.startStopLdapKnoxSuccessCallback({Requests: {id: 1}});
      expect(App.router.get('backgroundOperationsController').showPopup.calledOnce).to.be.true;
    });
  });
  
  describe('#startStopLdapKnoxErrorCallback', function() {
    beforeEach(function() {
      sinon.stub(App, 'showAlertPopup');
    });
    afterEach(function() {
      App.showAlertPopup.restore();
    });
    
    it('showAlertPopup should be called', function() {
      var ctrl = App.MainServiceItemController.create();
      ctrl.startStopLdapKnoxErrorCallback({
        responseText: JSON.stringify({
          message: 'hello'
        })
      });
      expect(App.showAlertPopup.calledWith(
        Em.I18n.t('services.service.actions.run.startStopLdapKnox.error'),
        Em.I18n.t('services.service.actions.run.yarnRefreshQueues.error') + 'hello'
      )).to.be.true;
    });
  });
  
  describe('#restartLLAPRequest', function() {
    beforeEach(function() {
      sinon.stub(App.HostComponent, 'find').returns([
        Em.Object.create({
          componentName: 'HIVE_SERVER_INTERACTIVE',
          hostName: 'host1'
        })
      ]);
    });
    afterEach(function() {
      App.HostComponent.find.restore();
    });
    
    it('App.ajax.send should be called', function() {
      var ctrl = App.MainServiceItemController.create();
      ctrl.restartLLAPRequest();
      expect(testHelpers.findAjaxRequest('name', 'service.item.executeCustomCommand')[0].data).to.be.eql({
        command: 'RESTART_LLAP',
        context: Em.I18n.t('services.service.actions.run.restartLLAP'),
        hosts: 'host1',
        serviceName: "HIVE",
        componentName: "HIVE_SERVER_INTERACTIVE"
      });
    });
  });
  
  describe('#restartLLAPAndRefreshQueueRequest', function() {
    beforeEach(function() {
      sinon.stub(App.MasterComponent, 'find').returns(Em.Object.create({
        hostNames: ['host1']
      }));
    });
    afterEach(function() {
      App.MasterComponent.find.restore();
    });
    
    it('App.ajax.send should be called', function() {
      var ctrl = App.MainServiceItemController.create();
      ctrl.restartLLAPAndRefreshQueueRequest();
      expect(testHelpers.findAjaxRequest('name', 'common.batch.request_schedules')[0].data).to.be.eql({
        intervalTimeSeconds: 1,
        tolerateSize: 0,
        batches: [
          {
            "order_id": 1,
            "type": "POST",
            "uri": "/clusters/mycluster/requests",
            "RequestBodyInfo": {
              "RequestInfo": {
                "context": "Refresh YARN Capacity Scheduler",
                "command": "REFRESHQUEUES",
                "parameters/forceRefreshConfigTags": "capacity-scheduler"
              },
              "Requests/resource_filters": [{
                "service_name": "YARN",
                "component_name": "RESOURCEMANAGER",
                "hosts": 'host1'
              }]
            }
          },
          {
            "order_id": 2,
            "type": "POST",
            "uri": "/clusters/mycluster/requests",
            "RequestBodyInfo": {
              "RequestInfo": {"context": "Restart LLAP", "command": "RESTART_LLAP"},
              "Requests/resource_filters": [{
                "service_name": "HIVE",
                "component_name": "HIVE_SERVER_INTERACTIVE",
                "hosts": 'host1'
              }]
            }
          }
        ]
      });
    });
  });
  
  describe('#requestSuccessCallback', function() {
    var mock = {
      dataLoading: function() {
        return {
          done: function(clb) {
            clb(true);
          }
        }
      },
      showPopup: sinon.spy()
    };
    beforeEach(function() {
      sinon.stub(App.router, 'get').returns(mock);
    });
    afterEach(function() {
      App.router.get.restore();
    });
    
    it('showPopup should be called', function() {
      var ctrl = App.MainServiceItemController.create();
      ctrl.requestSuccessCallback();
      expect(mock.showPopup.calledOnce).to.be.true;
    });
  });
  
  describe('#rebalanceHdfsNodesSuccessCallback', function() {
    var mock = {
      showPopup: sinon.spy()
    };
    beforeEach(function() {
      sinon.stub(App.router, 'get').returns(mock);
    });
    afterEach(function() {
      App.router.get.restore();
    });
    
    it('showPopup should be called', function() {
      var ctrl = App.MainServiceItemController.create();
      ctrl.rebalanceHdfsNodesSuccessCallback({Requests: {id: 1}});
      expect(mock.showPopup.calledOnce).to.be.true;
    });
  });
  
  describe('#regenerateKeytabFileOperationsRequestSuccess', function() {
    var mock = {
      showPopup: sinon.spy()
    };
    beforeEach(function() {
      sinon.stub(App.router, 'get').returns(mock);
    });
    afterEach(function() {
      App.router.get.restore();
    });
    
    it('showPopup should be called', function() {
      var ctrl = App.MainServiceItemController.create();
      ctrl.regenerateKeytabFileOperationsRequestSuccess();
      expect(mock.showPopup.calledOnce).to.be.true;
    });
  });
  
  describe('#regenerateKeytabFileOperationsRequestError', function() {
    beforeEach(function() {
      sinon.stub(App, 'showAlertPopup');
    });
    afterEach(function() {
      App.showAlertPopup.restore();
    });
    
    it('showAlertPopup should be called', function() {
      var ctrl = App.MainServiceItemController.create({
        content: {
          serviceName: 'S1'
        }
      });
      ctrl.regenerateKeytabFileOperationsRequestError();
      expect(App.showAlertPopup.calledWith(
        Em.I18n.t('common.error'), Em.I18n.t('alerts.notifications.regenerateKeytab.service.error').format('S1')
      )).to.be.true;
    });
  });
  
});
