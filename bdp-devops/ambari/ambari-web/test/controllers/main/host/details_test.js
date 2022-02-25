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
require('controllers/main/host/details');
require('models/service');
require('models/host_component');
require('models/host_stack_version');
var batchUtils = require('utils/batch_scheduled_requests');
var hostsManagement = require('utils/hosts');
var testHelpers = require('test/helpers');
var controller;

function getController() {
  return App.MainHostDetailsController.create(App.InstallComponent, {
    content: Em.Object.create({
      hostComponents: []
    })
  });
}
describe('App.MainHostDetailsController', function () {

  beforeEach(function () {
    controller = getController();
    sinon.stub(controller, 'trackRequest');
  });

  App.TestAliases.testAsComputedFilterBy(getController(), 'serviceNonClientActiveComponents', 'serviceActiveComponents', 'isClient', false);

  describe('#routeHome()', function () {

    beforeEach(function () {
      sinon.stub(App.router, 'transitionTo', Em.K);
    });

    afterEach(function () {
      App.router.transitionTo.restore();
    });

    it('transition to dashboard', function () {
      controller.routeHome();
      expect(App.router.transitionTo.calledWith('main.dashboard.index')).to.be.true;
    });
  });

  describe('#startComponent()', function () {

    var event = {
      context: Em.Object.create({
        displayName: 'comp'
      })
    };

    beforeEach(function () {
      sinon.stub(App, 'showConfirmationPopup', function (callback) {
        callback();
      });
      sinon.stub(controller, 'sendComponentCommand');
      controller.startComponent(event);
    });

    afterEach(function () {
      App.showConfirmationPopup.restore();
      controller.sendComponentCommand.restore();
    });

    it('confirmation popup is shown', function () {
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
    });

    it('call sendComponentCommand', function () {
      expect(controller.sendComponentCommand.calledWith(Em.Object.create({
        displayName: 'comp'
      })), Em.I18n.t('requestInfo.startHostComponent') + " comp", App.HostComponentStatus.started).to.be.true;

    });
  });

  describe('#stopComponent()', function () {

    beforeEach(function () {
      sinon.stub(App, 'showConfirmationPopup', Em.clb);
      sinon.stub(controller, 'checkNnLastCheckpointTime', Em.clb);
      sinon.stub(controller, 'sendComponentCommand');
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      controller.sendComponentCommand.restore();
      controller.checkNnLastCheckpointTime.restore();
    });

    it('call sendComponentCommand', function () {
      var event = {
        context: Em.Object.create({
          displayName: 'comp'
        })
      };
      controller.stopComponent(event);
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      expect(controller.sendComponentCommand.calledWith(Em.Object.create({
        displayName: 'comp'
      })), Em.I18n.t('requestInfo.stopHostComponent') + " comp", App.HostComponentStatus.stopped).to.be.true;
    });
    it('stop NN, should check last NN checkpoint before stop', function () {
      var event = {
        context: Em.Object.create({
          displayName: 'NameNode',
          componentName: 'NAMENODE'
        })
      };
      controller.stopComponent(event);
      expect(controller.checkNnLastCheckpointTime.calledOnce).to.be.true;
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      expect(controller.sendComponentCommand.calledWith(event.context, Em.I18n.t('requestInfo.stopHostComponent') + " NameNode", App.HostComponentStatus.stopped)).to.be.true;
    });
  });

  describe("#pullNnCheckPointTime()", function() {
    it("valid request is sent", function() {
      controller.pullNnCheckPointTime('host1');
      var args = testHelpers.findAjaxRequest('name', 'common.host_component.getNnCheckPointTime');
      expect(args[0]).to.exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].data).to.be.eql({
        host: 'host1'
      });
    });
  });

  describe('#sendComponentCommand()', function () {

    describe('single component', function () {
      var component;
      beforeEach(function () {
        controller.set('content.hostName', 'host1');
        component = Em.Object.create({
          service: {serviceName: 'S1'},
          componentName: 'COMP1'
        });

        controller.sendComponentCommand(component, {}, 'state');
      });

      it('1st call endpoint is valid', function () {
        var args = testHelpers.findAjaxRequest('name', 'common.host.host_component.update');
        expect(args).to.exists;
      });

      it('1st call data is valid', function () {
        var args = testHelpers.findAjaxRequest('name', 'common.host.host_component.update');
        expect(args[0]).to.exists;
        expect(args[0].data).to.be.eql({
          "hostName": "host1",
          "context": {},
          "component": component,
          "HostRoles": {
            "state": "state"
          },
          "componentName": "COMP1",
          "serviceName": "S1"
        });
      });
    });

    describe('multiple component', function () {
      var component;
      beforeEach(function () {
        controller.set('content.hostName', 'host1');
        component = [
          Em.Object.create({
            service: {serviceName: 'S1'},
            componentName: 'COMP1'
          }),
          Em.Object.create({
            service: {serviceName: 'S1'},
            componentName: 'COMP2'
          })
        ];
        controller.sendComponentCommand(component, {}, 'state');
      });

      it('1st call endpoint is valid', function () {
        var args = testHelpers.findAjaxRequest('name', 'common.host.host_components.update');
        expect(args).exists;
      });

      it('1st call data is valid', function () {
        var args = testHelpers.findAjaxRequest('name', 'common.host.host_components.update');
        expect(args[0]).exists;
        expect(args[0].data).to.be.eql({
          "hostName": "host1",
          "context": {},
          "component": component,
          "HostRoles": {
            "state": "state"
          },
          "query": "HostRoles/component_name.in(COMP1,COMP2)"
        });
      });
    });

  });

  describe('#sendComponentCommandSuccessCallback()', function () {

    var params = {
      component: Em.Object.create({}),
      HostRoles: {
        state: App.HostComponentStatus.stopped
      }
    };

    beforeEach(function () {
      sinon.stub(controller, 'mimicWorkStatusChange', Em.K);
      sinon.stub(controller, 'showBackgroundOperationsPopup', Em.K);
      controller.sendComponentCommandSuccessCallback({}, {}, params);
    });

    afterEach(function () {
      controller.showBackgroundOperationsPopup.restore();
      controller.mimicWorkStatusChange.restore();
    });

    it('mimicWorkStatusChange is not called', function () {
      expect(controller.mimicWorkStatusChange.called).to.be.false;
    });
    it('showBackgroundOperationsPopup is called once', function () {
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
  });

  describe('#ajaxErrorCallback()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'ajaxErrorCallback', Em.K);
    });
    afterEach(function () {
      controller.ajaxErrorCallback.restore();
    });
    it('call mainServiceItemController.ajaxErrorCallback', function () {
      controller.ajaxErrorCallback('request', 'ajaxOptions', 'error', 'opt', 'params');
      expect(controller.ajaxErrorCallback.calledWith('request', 'ajaxOptions', 'error', 'opt', 'params')).to.be.true;
    });
  });

  describe('#showBackgroundOperationsPopup()', function () {
    var mock = {
      done: function (callback) {
        callback(this.initValue);
      }
    };
    var bgController = {
      showPopup: Em.K
    };

    beforeEach(function () {
      var stub = sinon.stub(App.router, 'get');
      stub.withArgs('userSettingsController').returns({
        dataLoading: function () {
          return mock;
        }
      });
      stub.withArgs('backgroundOperationsController').returns(bgController);
      sinon.spy(bgController, 'showPopup');
      sinon.spy(mock, 'done');
      this.callback = sinon.stub();
    });

    afterEach(function () {
      bgController.showPopup.restore();
      mock.done.restore();
      App.router.get.restore();
    });

    it('initValue is true, callback is undefined', function () {
      mock.initValue = true;
      controller.showBackgroundOperationsPopup();
      expect(mock.done.calledOnce).to.be.true;
      expect(bgController.showPopup.calledOnce).to.be.true;
    });

    it('initValue is false, callback is defined', function () {
      mock.initValue = false;
      controller.showBackgroundOperationsPopup(this.callback);
      expect(mock.done.calledOnce).to.be.true;
      expect(bgController.showPopup.calledOnce).to.be.false;
      expect(this.callback.calledOnce).to.be.true;
    });
  });

  describe('#serviceActiveComponents', function () {

    it('No host-components', function () {
      controller.set('content', {hostComponents: []});
      expect(controller.get('serviceActiveComponents')).to.be.empty;
    });

    it('No host-components in active state', function () {
      controller.set('content', {
        hostComponents: [Em.Object.create({
          service: {
            isInPassive: true
          }
        })]
      });
      expect(controller.get('serviceActiveComponents')).to.be.empty;
    });
    it('Host-components in active state', function () {
      controller.set('content', {
        hostComponents: [Em.Object.create({
          service: {
            isInPassive: false
          }
        })]
      });
      expect(controller.get('serviceActiveComponents')).to.eql([Em.Object.create({
        service: {
          isInPassive: false
        }
      })]);
    });
  });

  describe('#serviceNonClientActiveComponents', function () {

    it('No active host-components', function () {
      controller.reopen({
        serviceActiveComponents: []
      });
      controller.set('serviceActiveComponents', []);
      expect(controller.get('serviceNonClientActiveComponents')).to.be.empty;
    });

    it('Active host-component is client', function () {
      controller.reopen({
        serviceActiveComponents: [Em.Object.create({
          isClient: true
        })]
      });
      expect(controller.get('serviceNonClientActiveComponents')).to.be.empty;
    });
    it('Active host-component is not client', function () {
      controller.reopen({
        serviceActiveComponents: [Em.Object.create({
          isClient: false
        })]
      });
      expect(controller.get('serviceNonClientActiveComponents')).to.eql([Em.Object.create({
        isClient: false
      })]);
    });
  });

  describe('#deleteComponent()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'deleteAndReconfigureComponent');
      sinon.spy(App, 'showConfirmationPopup');
      sinon.stub(controller, 'showReconfigurationPopupPreDelete');
      sinon.stub(App.router, 'transitionTo');
    });
    afterEach(function () {
      controller.deleteAndReconfigureComponent.restore();
      App.showConfirmationPopup.restore();
      controller.showReconfigurationPopupPreDelete.restore();
      App.router.transitionTo.restore();
    });

    it('showConfirmationPopup should be called', function () {
      var popup = controller.deleteComponent({context: Em.Object.create({
        componentName: 'JOURNALNODE'
      })});

      expect(App.showConfirmationPopup.calledOnce).to.be.true;

      popup.onPrimary();

      expect(App.router.transitionTo.calledWith('main.services.manageJournalNode')).to.be.true;
    });

    it('deleteAndReconfigureComponent should be called', function () {
      controller.deleteComponent({context: Em.Object.create({
        componentName: 'ZOOKEEPER_SERVER'
      })});

      expect(controller.deleteAndReconfigureComponent.calledOnce).to.be.true;
    });

    it('showReconfigurationPopupPreDelete should be called', function () {
      controller.deleteComponent({context: Em.Object.create({
        componentName: 'DATANODE'
      })});

      expect(controller.showReconfigurationPopupPreDelete.calledOnce).to.be.true;
    });

  });

  describe('#deleteAndReconfigureComponent', function() {
    beforeEach(function() {
      sinon.stub(controller, 'loadComponentRelatedConfigs');
      sinon.stub(controller, 'showReconfigurationPopupPreDelete', function(c, callback) {
        callback();
      });
      sinon.stub(controller, '_doDeleteHostComponent').returns({
        done: Em.clb
      });
      sinon.stub(controller, 'applyConfigsCustomization');
      sinon.stub(controller, 'putConfigsToServer');
      sinon.stub(controller, 'clearConfigsChanges');
      controller.deleteAndReconfigureComponent({
        deletePropertyName: 'prop1',
        configTagsCallbackName: Em.K,
        configsCallbackName: Em.K
      }, Em.Object.create({componentName: 'C1'}));
    });
    afterEach(function() {
      controller.loadComponentRelatedConfigs.restore();
      controller.showReconfigurationPopupPreDelete.restore();
      controller._doDeleteHostComponent.restore();
      controller.applyConfigsCustomization.restore();
      controller.putConfigsToServer.restore();
      controller.clearConfigsChanges.restore();
    });

    it('should set deleteProperty', function() {
      expect(controller.get('prop1')).to.be.true;
    });
    it('loadComponentRelatedConfigs should be called', function() {
      expect(controller.loadComponentRelatedConfigs.calledOnce).to.be.true;
    });
    it('showReconfigurationPopupPreDelete should be called', function() {
      expect(controller.showReconfigurationPopupPreDelete.calledOnce).to.be.true;
    });
    it('_doDeleteHostComponent should be called', function() {
      expect(controller._doDeleteHostComponent.calledWith('C1')).to.be.true;
    });
    it('applyConfigsCustomization should be called', function() {
      expect(controller.applyConfigsCustomization.calledOnce).to.be.true;
    });
    it('putConfigsToServer should be called', function() {
      expect(controller.putConfigsToServer.calledOnce).to.be.true;
    });
    it('clearConfigsChanges should be called', function() {
      expect(controller.clearConfigsChanges.calledOnce).to.be.true;
    });

  });

  describe('#mimicWorkStatusChange()', function () {

    var clock;
    beforeEach(function () {
      clock = sinon.useFakeTimers();
    });
    afterEach(function () {
      clock.restore();
    });

    it('change status of object', function () {
      var entity = Em.Object.create({
        workStatus: ''
      });
      controller.mimicWorkStatusChange(entity, 'STATE1', 'STATE2');
      expect(entity.get('workStatus')).to.equal('STATE1');
      clock.tick(App.testModeDelayForActions);
      expect(entity.get('workStatus')).to.equal('STATE2');
    });
    it('change status of objects in array', function () {
      var entity = [Em.Object.create({
        workStatus: ''
      })];
      controller.mimicWorkStatusChange(entity, 'STATE1', 'STATE2');
      expect(entity[0].get('workStatus')).to.equal('STATE1');
      clock.tick(App.testModeDelayForActions);
      expect(entity[0].get('workStatus')).to.equal('STATE2');
    });
  });

  describe('#upgradeComponent()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
    });

    it('confirm popup should be displayed', function () {
      var popup = controller.upgradeComponent({context: Em.Object.create()});
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      var args = testHelpers.findAjaxRequest('name', 'host.host_component.upgrade');
      expect(args).exists;
    });
  });

  describe('#restartComponent()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(batchUtils, "restartHostComponents", Em.K);
      sinon.stub(controller, 'checkNnLastCheckpointTime', Em.clb);
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      batchUtils.restartHostComponents.restore();
      controller.checkNnLastCheckpointTime.restore();
    });

    it('popup should be displayed', function () {
      var popup = controller.restartComponent({context: Em.Object.create({'displayName': 'Comp1'})});
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(batchUtils.restartHostComponents.calledOnce).to.be.true;
    });

    it('restart NN, should check last NN checkpoint before restart', function () {
      var event = {
        context: Em.Object.create({
          displayName: 'NameNode',
          componentName: 'NAMENODE'
        })
      };
      controller.restartComponent(event);
      expect(controller.checkNnLastCheckpointTime.calledOnce).to.equal(true);
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
    });
  });

  describe('#addComponent()', function () {
    var cases = [
      {
        componentName: 'RESOURCEMANAGER',
        showAddComponentPopupCallCount: 1,
        showConfirmationPopupCallCount: 0
      },
      {
        componentName: 'JOURNALNODE',
        showAddComponentPopupCallCount: 0,
        showConfirmationPopupCallCount: 1
      },
      {
        componentName: 'HIVE_CLIENT',
        showAddComponentPopupCallCount: 1,
        showConfirmationPopupCallCount: 0
      }
    ];
    beforeEach(function () {
      sinon.stub(controller, "checkComponentDependencies", Em.K);
      sinon.stub(controller, "showAddComponentPopup", Em.K);
      sinon.stub(controller, 'addAndReconfigureComponent');
      sinon.stub(App, "showConfirmationPopup", Em.K);
      controller.set('content', {
        hostComponents: [Em.Object.create({
          componentName: "HDFS_CLIENT"
        })]
      });
    });

    afterEach(function () {
      controller.checkComponentDependencies.restore();
      controller.showAddComponentPopup.restore();
      controller.addAndReconfigureComponent.restore();
      App.showConfirmationPopup.restore();
    });

    cases.forEach(function (testCase) {

      describe('add ' + testCase.componentName, function () {

        beforeEach(function () {
          var event = {
            context: Em.Object.create({
              componentName: testCase.componentName
            })
          };
          controller.addComponent(event);
        });

        it('controller.showAddComponentPopup', function () {
          expect(controller.showAddComponentPopup.callCount).to.equal(testCase.showAddComponentPopupCallCount);
        });

        it('App.showConfirmationPopup', function () {
          expect(App.showConfirmationPopup.callCount).to.equal(testCase.showConfirmationPopupCallCount);
        });

      });

    });

    it('should call addAndReconfigureComponent when adding ZOOKEEPER_SERVER', function () {
      controller.addComponent({
        context: Em.Object.create({
          componentName: 'ZOOKEEPER_SERVER'
        }),
        fromServiceSummary: true
      });
      expect(controller.addAndReconfigureComponent.calledOnce).to.be.true;
    });

  });

  describe('#addAndReconfigureComponent', function() {
    beforeEach(function() {
      sinon.stub(controller, 'loadComponentRelatedConfigs');
      sinon.stub(controller, 'showAddComponentPopup', function(c, h, callback) {
        callback();
      });
      sinon.stub(controller, 'installAndReconfigureComponent');
      controller.addAndReconfigureComponent({
        hostPropertyName: 'host',
        addPropertyName: 'prop1'
      }, 'host1', {}, true);
    });
    afterEach(function() {
      controller.loadComponentRelatedConfigs.restore();
      controller.showAddComponentPopup.restore();
      controller.installAndReconfigureComponent.restore();
    });

    it('hostPropertyName should be set', function() {
      expect(controller.get('host')).to.be.equal('host1');
    });
    it('addPropertyName should be set', function() {
      expect(controller.get('prop1')).to.be.equal(true);
    });
    it('loadComponentRelatedConfigs should be called', function() {
      expect(controller.loadComponentRelatedConfigs.calledOnce).to.be.true;
    });
    it('showAddComponentPopup should be called', function() {
      expect(controller.showAddComponentPopup.calledOnce).to.be.true;
    });
    it('installAndReconfigureComponent should be called', function() {
      expect(controller.installAndReconfigureComponent.calledOnce).to.be.true;
    });
  });

  describe('#installAndReconfigureComponent', function() {
    beforeEach(function() {
      sinon.stub(controller, 'applyConfigsCustomization');
      sinon.stub(controller, 'installHostComponentCall').returns({
        done: function(callback) {
          callback();
          return {
            always: Em.clb
          }
        }
      });
      sinon.stub(controller, 'putConfigsToServer');
      sinon.stub(controller, 'clearConfigsChanges');

      controller.set('groupedPropertiesToChange', [{}]);

      controller.installAndReconfigureComponent('host1', Em.Object.create({
        componentName: 'C1'
      }), {
        addPropertyName: 'prop1'
      });
    });
    afterEach(function() {
      controller.applyConfigsCustomization.restore();
      controller.installHostComponentCall.restore();
      controller.putConfigsToServer.restore();
      controller.clearConfigsChanges.restore();
    });

    it('applyConfigsCustomization should be called', function() {
      expect(controller.applyConfigsCustomization.calledOnce).to.be.true;
    });
    it('installHostComponentCall should be called', function() {
      expect(controller.installHostComponentCall.calledOnce).to.be.true;
    });
    it('putConfigsToServer should be called', function() {
      expect(controller.putConfigsToServer.calledWith([{}], 'C1')).to.be.true;
    });
    it('clearConfigsChanges should be called', function() {
      expect(controller.clearConfigsChanges.calledOnce).to.be.true;
    });
    it('addPropertyName should be false', function() {
      expect(controller.get('prop1')).to.be.false;
    });

  });

  describe("#loadOozieConfigs()", function() {
    it("valid request is sent", function() {
      controller.loadOozieConfigs({Clusters: {
        desired_configs: {
          'oozie-env': {
            tag: 'tag'
          }
        }
      }});
      var args = testHelpers.findAjaxRequest('name', 'admin.get.all_configurations');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].data).to.be.eql({
        urlParams: '(type=oozie-env&tag=tag)'
      });
    });
  });

  describe("#loadStormConfigs()", function() {
    it("valid request is sent", function() {
      controller.loadStormConfigs({Clusters: {
        desired_configs: {
          'storm-site': {
            tag: 'tag'
          }
        }
      }}, null, {});
      var args = testHelpers.findAjaxRequest('name', 'admin.get.all_configurations');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].data).to.be.eql({
        urlParams: '(type=storm-site&tag=tag)'
      });
    });
  });

  describe("#onLoadStormConfigs()", function() {

    var data = {items: [
      {
        type: 'storm-site',
        properties: {
          'nimbus.seeds': ''
        }
      }
    ]};

    beforeEach(function () {
      sinon.stub(controller, 'getStormNimbusHosts').returns("host1");
      sinon.stub(controller, 'updateZkConfigs', Em.K);
      sinon.stub(controller, 'putConfigsToServer', Em.K);
      sinon.stub(controller, 'saveLoadedConfigs', Em.K);
      controller.set('nimbusHost', 'host2');
      controller.onLoadStormConfigs(data);
    });
    afterEach(function () {
      controller.getStormNimbusHosts.restore();
      controller.updateZkConfigs.restore();
      controller.putConfigsToServer.restore();
      controller.saveLoadedConfigs.restore();
    });
    it("updateZkConfigs called with valid arguments", function() {
      expect(controller.updateZkConfigs.calledWith({'storm-site': {
        'nimbus.seeds': "'host1'"
      }})).to.be.true;
    });
  });

  describe("#loadHiveConfigs()", function() {
    it("valid request is sent", function() {
      controller.loadHiveConfigs({Clusters: {
        desired_configs: {
          'hive-site': {
            tag: 'tag'
          },
          'webhcat-site': {
            tag: 'tag'
          },
          'hive-env': {
            tag: 'tag'
          },
          'core-site': {
            tag: 'tag'
          }
        }
      }}, null, {});
      var args = testHelpers.findAjaxRequest('name', 'admin.get.all_configurations');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].data).to.be.eql({
        urlParams: '(type=hive-site&tag=tag)|(type=webhcat-site&tag=tag)|(type=hive-env&tag=tag)|(type=core-site&tag=tag)'
      });
    });
  });

  describe("#loadAtlasConfigs()", function() {
    it("valid request is sent", function() {
      controller.loadAtlasConfigs({Clusters: {
        desired_configs: {
          'application-properties': {
            tag: 'tag'
          }
        }
      }}, null, {});
      var args = testHelpers.findAjaxRequest('name', 'admin.get.all_configurations');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].data).to.be.eql({
        urlParams: '(type=application-properties&tag=tag)'
      });
    });
  });

  describe("#loadRangerConfigs()", function() {
    it("valid request is sent", function() {
      controller.loadRangerConfigs({Clusters: {
        desired_configs: {
          'hdfs-site': {
            tag: 'tag'
          },
          'kms-env': {
            tag: 'tag'
          },
          'core-site': {
            tag: 'tag'
          },
          'kms-site': {
            tag: 'tag'
          }
        }
      }}, null, {});
      var args = testHelpers.findAjaxRequest('name', 'admin.get.all_configurations');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].data).to.be.eql({
        urlParams: '(type=core-site&tag=tag)|(type=hdfs-site&tag=tag)|(type=kms-env&tag=tag)|(type=kms-site&tag=tag)'
      });
    });
  });

  describe("#getRangerKMSServerHosts()", function() {
    beforeEach(function(){
      sinon.stub(App.HostComponent, 'find').returns([{
        componentName: 'RANGER_KMS_SERVER',
        hostName: 'host1'
      }]);
      controller.set('rangerKMSServerHost', 'host2');
      controller.set('content.hostName', 'host1');
      controller.set('deleteRangerKMSServer', true);
      controller.set('fromDeleteHost', true);
      this.hosts = controller.getRangerKMSServerHosts();
    });
    afterEach(function(){
      App.HostComponent.find.restore();
    });
    it('hosts list is valid', function() {
      expect(this.hosts).to.eql(['host2']);
    });
  });

  describe("#getStormNimbusHosts()", function() {
    beforeEach(function(){
      sinon.stub(App.HostComponent, 'find').returns([{
        componentName: 'NIMBUS',
        hostName: 'host1'
      }]);
      controller.set('nimbusHost', 'host2');
      controller.set('content.hostName', 'host1');
      controller.set('deleteNimbusHost', true);
      controller.set('fromDeleteHost', true);
      this.hosts = controller.getStormNimbusHosts();
    });
    afterEach(function(){
      App.HostComponent.find.restore();
    });
    it("hosts list is valid", function() {
      expect(this.hosts).to.eql(['host2']);
    });
    it('nimbusHost is empty', function () {
      expect(controller.get('nimbusHost')).to.be.empty;
    });
    it('deleteNimbusHost is false', function () {
      expect(controller.get('deleteNimbusHost')).to.be.false;
    });
    it('fromDeleteHost is false', function () {
      expect(controller.get('fromDeleteHost')).to.be.false;
    });
  });

  describe('#showAddComponentPopup()', function () {

    it('should display add component confirmation', function () {
      controller.showAddComponentPopup(Em.Object.create());
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });
  });

  describe('#installNewComponentSuccessCallback()', function () {

    beforeEach(function () {
      sinon.stub(controller, "showBackgroundOperationsPopup", Em.K);
    });
    afterEach(function () {
      controller.showBackgroundOperationsPopup.restore();
    });

    it('data is null', function () {
      expect(controller.installNewComponentSuccessCallback(null, {}, {})).to.be.false;
      expect(controller.showBackgroundOperationsPopup.called).to.be.false;
    });
    it('data.Requests is null', function () {
      var data = {Requests: null};
      expect(controller.installNewComponentSuccessCallback(data, {}, {})).to.be.false;
      expect(controller.showBackgroundOperationsPopup.called).to.be.false;
    });
    it('data.Requests.id is null', function () {
      var data = {Requests: {id: null}};
      expect(controller.installNewComponentSuccessCallback(data, {}, {})).to.be.false;
      expect(controller.showBackgroundOperationsPopup.called).to.be.false;
    });
    it('data.Requests.id is correct', function () {
      var data = {Requests: {id: 1}};
      expect(controller.installNewComponentSuccessCallback(data, {}, {component: []})).to.be.true;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
  });

  describe('#refreshComponentConfigs()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(controller, "sendRefreshComponentConfigsCommand", Em.K);
    });

    afterEach(function () {
      App.showConfirmationPopup.restore();
      controller.sendRefreshComponentConfigsCommand.restore();
    });

    it('popup should be displayed', function () {
      var popup = controller.refreshComponentConfigs({context: Em.Object.create({'displayName': 'Comp1'})});
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.sendRefreshComponentConfigsCommand.calledOnce).to.be.true;
    });
  });

  describe('#sendRefreshComponentConfigsCommand()', function () {
    it('Query should be sent', function () {
      var component = Em.Object.create({
        service: {},
        componentName: 'COMP1',
        host: {}
      });
      controller.sendRefreshComponentConfigsCommand(component, {});
      var args = testHelpers.findAjaxRequest('name', 'host.host_component.refresh_configs');
      expect(args[0]).exists;
    });
  });

  describe('#loadConfigs()', function () {
    it('Query should be sent', function () {
      controller.loadConfigs();
      var args = testHelpers.findAjaxRequest('name', 'config.tags');
      expect(args).exists;
    });
  });

  describe('#constructZookeeperConfigUrlParams()', function () {

    function loadService(serviceName) {
      App.store.safeLoad(App.Service, {
        id: serviceName,
        service_name: serviceName
      });
    }

    var data = {
      Clusters: {
        desired_configs: {
          'core-site': {
            tag: 1
          },
          'hbase-site': {
            tag: 1
          },
          'webhcat-site': {
            tag: 1
          },
          'hive-site': {
            tag: 1
          },
          'storm-site': {
            tag: 1
          },
          'yarn-site': {
            tag: 1
          },
          'zoo.cfg': {
            tag: 1
          },
          'accumulo-site': {
            tag: 1
          },
          'application-properties': {
            tag: 1
          }
        }
      }
    };

    afterEach(function () {
      App.Service.find().clear();
    });

    it('URL params should be empty', function () {
      App.Service.find().clear();
      expect(controller.constructZookeeperConfigUrlParams(data)).to.eql([]);
    });

    it('isHaEnabled = true', function () {
      loadService('HDFS');
      sinon.stub(App, 'get').returns(true);
      expect(controller.constructZookeeperConfigUrlParams(data)).to.eql(['(type=core-site&tag=1)']);
      App.get.restore();
    });

    it('HBASE is installed', function () {
      loadService('HBASE');
      App.propertyDidChange('isHaEnabled');
      expect(controller.constructZookeeperConfigUrlParams(data)).to.eql(['(type=hbase-site&tag=1)']);
    });

    it('HIVE is installed', function () {
      loadService('HIVE');
      expect(controller.constructZookeeperConfigUrlParams(data)).to.eql(['(type=hive-site&tag=1)', '(type=webhcat-site&tag=1)']);
    });

    it('STORM is installed', function () {
      loadService('STORM');
      expect(controller.constructZookeeperConfigUrlParams(data)).to.eql(['(type=storm-site&tag=1)']);
    });

    it('YARN is installed', function () {
      loadService('YARN');
      expect(controller.constructZookeeperConfigUrlParams(data)).to.eql(['(type=yarn-site&tag=1)', '(type=zoo.cfg&tag=1)']);
    });

    it('ACCUMULO is installed', function () {
      loadService('ACCUMULO');
      expect(controller.constructZookeeperConfigUrlParams(data)).to.eql(['(type=accumulo-site&tag=1)']);
    });

    it('ATLAS is installed, AMBARI_INFRA_SOLR isn\'t installed', function () {
      loadService('ATLAS');
      expect(controller.constructZookeeperConfigUrlParams(data)).to.eql(['(type=application-properties&tag=1)']);
    });
  });

  describe('#loadZookeeperConfigs()', function () {
    var mockUrlParams = [];
    beforeEach(function () {
      sinon.stub(controller, "constructZookeeperConfigUrlParams", function () {
        return mockUrlParams;
      });
    });
    afterEach(function () {
      controller.constructZookeeperConfigUrlParams.restore();
    });

    it('url params is empty', function () {
      expect(controller.loadZookeeperConfigs(null, null, {})).to.be.false;
      var args = testHelpers.findAjaxRequest('name', 'reassign.load_configs');
      expect(args).not.exists;
    });
    it('url params are correct', function () {
      mockUrlParams = ['param1'];
      var args = testHelpers.findAjaxRequest('name', 'reassign.load_configs');
      expect(args).exists;
    });
    it('isConfigsLoadingInProgress is false', function () {
      mockUrlParams = [];
      controller.set('isConfigsLoadingInProgress', true);
      controller.loadZookeeperConfigs(null, null, {});
      expect(controller.get('isConfigsLoadingInProgress')).to.be.false;
    });
  });

  describe('#saveZkConfigs()', function () {

    var data = {
      items: [
        {
          type: 'yarn-site',
          properties: {
            p: 'ys'
          },
          properties_attributes: {
            p: 'pa_ys'
          }
        },
        {
          type: 'hive-site',
          properties: {
            hs: 'hs'
          },
          properties_attributes: {
            hs: 'pa_hs'
          }
        },
        {
          type: 'webhcat-site',
          properties: {
            ws: 'ws'
          },
          properties_attributes: {
            ws: 'pa_ws'
          }
        },
        {
          type: 'hbase-site',
          properties: {
            hbs: 'hbs'
          },
          properties_attributes: {
            hbs: 'pa_hbs'
          }
        },
        {
          type: 'accumulo-site',
          properties: {
            as: 'as'
          },
          properties_attributes: {
            as: 'pa_as'
          }
        }
      ]
    };

    beforeEach(function () {
      sinon.stub(controller, 'setConfigsChanges', Em.K);
      sinon.stub(controller, 'updateZkConfigs', Em.K);
      sinon.stub(controller, 'saveLoadedConfigs', Em.K);
      sinon.stub(App.Service, 'find', function() {
        return [
          Em.Object.create({ serviceName: 'HIVE' }),
          Em.Object.create({ serviceName: 'YARN' }),
          Em.Object.create({ serviceName: 'HBASE' }),
          Em.Object.create({ serviceName: 'ACCUMULO' })
        ];
      });

      controller.saveZkConfigs(data);
      this.groups = controller.setConfigsChanges.args[0][0];
    });
    afterEach(function () {
      App.Service.find.restore();
      controller.updateZkConfigs.restore();
      controller.setConfigsChanges.restore();
      controller.saveLoadedConfigs.restore();
    });

      it('configs for YARN', function () {
        var expected = {
          properties: {
            'yarn-site': {
              p: 'ys'
            }
          },
          properties_attributes: {
            'yarn-site': {
              p: 'pa_ys'
            }
          }
        };
        expect(this.groups[1]).to.be.eql(expected);
      });

      it('configs for HIVE', function () {
        var expected = {
          "properties": {
            "hive-site": {
              "hs": "hs"
            },
            "webhcat-site": {
              "ws": "ws"
            }
          },
          "properties_attributes": {
            "hive-site": {
              "hs": "pa_hs"
            },
            "webhcat-site": {
              "ws": "pa_ws"
            }
          }
        };
        expect(this.groups[0]).to.be.eql(expected);
      });

      it('configs for HBASE', function () {
        var expected = {
          "properties": {
            "hbase-site": {
              "hbs": "hbs"
            }
          },
          "properties_attributes": {
            "hbase-site": {
              "hbs": "pa_hbs"
            }
          }
        };
        expect(this.groups[2]).to.be.eql(expected);
      });

      it('configs for ACCUMULO', function () {
        var expected = {
          "properties": {
            "accumulo-site": {
              "as": "as"
            }
          },
          "properties_attributes": {
            "accumulo-site": {
              "as": "pa_as"
            }
          }
        };
        expect(this.groups[3]).to.be.eql(expected);
      });

  });

  describe("#putConfigsToServer()", function () {
    it("no groups", function () {
      controller.putConfigsToServer([]);
      var args = testHelpers.filterAjaxRequests('name', 'common.service.configurations');
      expect(args).to.be.empty;
    });
    it("configs is empty", function () {
      controller.putConfigsToServer([{}]);
      var args = testHelpers.filterAjaxRequests('name', 'common.service.configurations');
      expect(args).to.be.empty;
    });
    it("configs is correct", function () {
      controller.putConfigsToServer([{'properties': {'site': {}}, 'properties_attributes': {'site': {}}}]);
      var args = testHelpers.filterAjaxRequests('name', 'common.service.configurations');
      expect(args).to.have.property('length').equal(1);
    });
  });

  describe('#updateZkConfigs()', function () {
    var makeHostComponentModel = function(componentName, hostNames) {
      return hostNames.map(function(hostName) {
        return {
          componentName: componentName,
          hostName: hostName
        };
      });
    };

    var tests = [
      {
        appGetterStubs: {
          isHaEnabled: true
        },
        hostComponentModel: makeHostComponentModel('ZOOKEEPER_SERVER', ['host1', 'host2']),
        configs: {
          "core-site": {
            "ha.zookeeper.quorum": "host2:8080"
          }
        },
        m: 'NameNode HA enabled, ha.zookeeper.quorum config should be updated',
        e: {
          configs: {
            "core-site": {
              "ha.zookeeper.quorum": "host1:2181,host2:2181"
            }
          }
        }
      },
      {
        appGetterStubs: {
          isHaEnabled: false
        },
        hostComponentModel: makeHostComponentModel('ZOOKEEPER_SERVER', ['host1', 'host2']),
        configs: {
          "core-site": {
            "ha.zookeeper.quorum": "host3:8080"
          }
        },
        m: 'NameNode HA disabled, ha.zookeeper.quorum config should be untouched',
        e: {
          configs: {
            "core-site": {
              "ha.zookeeper.quorum": "host3:8080"
            }
          }
        }
      },
      {
        hostComponentModel: makeHostComponentModel('ZOOKEEPER_SERVER', ['host1', 'host2']),
        configs: {
          "hbase-site": {
            "hbase.zookeeper.quorum": "host3"
          }
        },
        m: 'hbase.zookeeper.quorum property update test',
        e: {
          configs: {
            "hbase-site": {
              "hbase.zookeeper.quorum": "host1,host2"
            }
          }
        }
      },
      {
        hostComponentModel: makeHostComponentModel('ZOOKEEPER_SERVER', ['host1', 'host2']),
        ctrlStubs: {
          'content.hostName': 'host2',
          fromDeleteHost: true,
          allPropertiesToChange: []
        },
        configs: {
          "zoo.cfg": {
            "clientPort": "1919"
          },
          "accumulo-site": {
            "instance.zookeeper.host": "host3:2020"
          }
        },
        m: 'instance.zookeeper.host property update test, zookeper marked to delete from host2',
        e: {
          configs: {
            "zoo.cfg": {
              "clientPort": "1919"
            },
            "accumulo-site": {
              "instance.zookeeper.host": "host1:1919"
            }
          }
        }
      },
      {
        hostComponentModel: makeHostComponentModel('ZOOKEEPER_SERVER', ['host1', 'host2']),
        configs: {
          "webhcat-site": {
            "templeton.zookeeper.hosts": "host3:2020"
          }
        },
        m: 'templeton.zookeeper.hosts property update test',
        e: {
          configs: {
            "webhcat-site": {
              "templeton.zookeeper.hosts": "host1:2181,host2:2181"
            }
          }
        }
      },
      {
        hostComponentModel: makeHostComponentModel('ZOOKEEPER_SERVER', ['host1', 'host2']),
        configs: {
          "hive-site": {
            "hive.cluster.delegation.token.store.zookeeper.connectString": "host3:2020"
          }
        },
        m: 'hive.cluster.delegation.token.store.zookeeper.connectString property update test',
        e: {
          configs: {
            "hive-site": {
              "hive.cluster.delegation.token.store.zookeeper.connectString": "host1:2181,host2:2181"
            }
          }
        }
      },
      {
        hostComponentModel: makeHostComponentModel('ZOOKEEPER_SERVER', ['host1', 'host2']),
        configs: {
          "storm-site": {
            "storm.zookeeper.servers": "['host3','host2']"
          }
        },
        m: 'storm.zookeeper.servers property update test',
        e: {
          configs: {
            "storm-site": {
              "storm.zookeeper.servers": "['host1','host2']"
            }
          }
        }
      },
      {
        appGetterStubs: {
          isRMHaEnabled: true
        },
        hostComponentModel: makeHostComponentModel('ZOOKEEPER_SERVER', ['host1', 'host2']),
        configs: {
          "yarn-site": {
            "yarn.resourcemanager.zk-address": "host3:2181"
          }
        },
        m: 'yarn.resourcemanager.zk-address property, ResourceManager HA enabled. Property value should be changed.',
        e: {
          configs: {
            "yarn-site": {
              "yarn.resourcemanager.zk-address": "host1:2181,host2:2181"
            }
          }
        }
      },
      {
        appGetterStubs: {
          currentStackVersionNumber: '2.2'
        },
        hostComponentModel: makeHostComponentModel('ZOOKEEPER_SERVER', ['host1', 'host2']),
        configs: {
          "hive-site": {
            "hive.zookeeper.quorum": "host3:2181"
          }
        },
        m: 'hive.zookeeper.quorum property, current stack version is 2.2 property should be updated.',
        e: {
          configs: {
            "hive-site": {
              "hive.zookeeper.quorum": "host1:2181,host2:2181"
            }
          }
        }
      },
      {
        appGetterStubs: {
          currentStackVersionNumber: '2.2'
        },
        hostComponentModel: makeHostComponentModel('ZOOKEEPER_SERVER', ['host1', 'host2']),
        configs: {
          "yarn-site": {
            "hadoop.registry.zk.quorum": "host3:2181"
          }
        },
        m: 'hadoop.registry.zk.quorum property, current stack version is 2.2 property should be changed.',
        e: {
          configs: {
            "yarn-site": {
              "hadoop.registry.zk.quorum": "host1:2181,host2:2181"
            }
          }
        }
      },
      {
        m: 'ATLAS configs',
        hostComponentModel: makeHostComponentModel('ZOOKEEPER_SERVER', ['host1', 'host2']),
        configs: {
          'application-properties': {
            'atlas.audit.hbase.zookeeper.quorum': '',
            'atlas.graph.index.search.solr.zookeeper-url': '',
            'atlas.graph.storage.hostname': '',
            'atlas.kafka.zookeeper.connect': ''
          }
        },
        e: {
          configs: {
            'application-properties': {
              'atlas.audit.hbase.zookeeper.quorum': 'host1,host2',
              'atlas.graph.index.search.solr.zookeeper-url': 'host1:2181/ambari-solr,host2:2181/ambari-solr',
              'atlas.graph.storage.hostname': 'host1,host2',
              'atlas.kafka.zookeeper.connect': 'host1:2181,host2:2181'
            }
          }
        }
      },
      {
        m: 'ATLAS configs with custom solr',
        hostComponentModel: makeHostComponentModel('ZOOKEEPER_SERVER', ['host1', 'host2']),
        configs: {
          'infra-solr-env': {
            'infra_solr_znode': '/custom-solr'
          },
          'application-properties': {
            'atlas.audit.hbase.zookeeper.quorum': '',
            'atlas.graph.index.search.solr.zookeeper-url': '',
            'atlas.graph.storage.hostname': '',
            'atlas.kafka.zookeeper.connect': ''
          }
        },
        e: {
          configs: {
            'infra-solr-env': {
              'infra_solr_znode': '/custom-solr'
            },
            'application-properties': {
              'atlas.audit.hbase.zookeeper.quorum': 'host1,host2',
              'atlas.graph.index.search.solr.zookeeper-url': 'host1:2181/custom-solr,host2:2181/custom-solr',
              'atlas.graph.storage.hostname': 'host1,host2',
              'atlas.kafka.zookeeper.connect': 'host1:2181,host2:2181'
            }
          }
        }
      }
    ];

    tests.forEach(function(test) {
      describe(test.m, function() {

        beforeEach(function() {
          if (test.appGetterStubs) {
            Em.keys(test.appGetterStubs).forEach(function(key) {
              sinon.stub(App, 'get').withArgs(key).returns(test.appGetterStubs[key]);
            });
          }
          if (test.ctrlStubs) {
            var stub = sinon.stub(controller, 'get');
            Em.keys(test.ctrlStubs).forEach(function(key) {
              stub.withArgs(key).returns(test.ctrlStubs[key]);
            });
          }
          sinon.stub(App.HostComponent, 'find').returns(test.hostComponentModel);
          controller.updateZkConfigs(test.configs);
        });

        afterEach(function () {
          if (test.ctrlStubs) {
            controller.get.restore();
          }
          if (test.appGetterStubs) {
            App.get.restore();
          }
          App.HostComponent.find.restore();
        });

        it('configs are mapped correctly', function () {
          expect(test.configs).to.be.eql(test.e.configs);
        });

      });
    });
  });

  describe('#installComponent()', function () {

    it('popup should be displayed', function () {
      var event = {context: Em.Object.create()};
      var popup = controller.installComponent(event);
      expect(App.ModalPopup.show.calledOnce).to.be.true;
      popup.onPrimary();
      var args = testHelpers.findAjaxRequest('name', 'common.host.host_component.update');
      expect(args).exists;
    });
  });

  describe('#decommission()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(controller, "runDecommission", Em.K);
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      controller.runDecommission.restore();
    });

    it('popup should be displayed', function () {
      var popup = controller.decommission(Em.Object.create({service: {}}));
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.runDecommission.calledOnce).to.be.true;
    });
  });

  describe('#recommission()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(controller, "runRecommission", Em.K);
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      controller.runRecommission.restore();
    });

    it('popup should be displayed', function () {
      var popup = controller.recommission(Em.Object.create({service: {}}));
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.runRecommission.calledOnce).to.be.true;
    });
  });

  describe('#runDecommission()', function () {

    beforeEach(function () {
      sinon.stub(controller, "doDecommission", Em.K);
      sinon.stub(controller, "showBackgroundOperationsPopup", Em.K);
    });

    afterEach(function () {
      controller.doDecommission.restore();
      controller.showBackgroundOperationsPopup.restore();
    });

    it('HDFS service', function () {
      controller.runDecommission('host1', 'HDFS');
      expect(controller.doDecommission.calledWith('host1', 'HDFS', "NAMENODE", "DATANODE")).to.be.true;
    });
    it('YARN service', function () {
      controller.runDecommission('host1', 'YARN');
      expect(controller.doDecommission.calledWith('host1', 'YARN', "RESOURCEMANAGER", "NODEMANAGER")).to.be.true;
    });

    describe('HBASE service', function () {

      beforeEach(function () {
        sinon.stub(controller, 'warnBeforeDecommission', Em.K);
      });

      afterEach(function () {
        controller.warnBeforeDecommission.restore();
      });
      it('warnBeforeDecommission is called with valid arguments', function () {
        controller.runDecommission('host1', 'HBASE');
        expect(controller.warnBeforeDecommission.calledWith('host1')).to.be.true;
      });

    });

  });

  describe('#runRecommission()', function () {

    beforeEach(function () {
      sinon.stub(controller, "doRecommissionAndStart", Em.K);
      sinon.stub(controller, "showBackgroundOperationsPopup", Em.K);
    });

    afterEach(function () {
      controller.doRecommissionAndStart.restore();
      controller.showBackgroundOperationsPopup.restore();
    });

    it('HDFS service', function () {
      controller.runRecommission('host1', 'HDFS');
      expect(controller.doRecommissionAndStart.calledWith('host1', 'HDFS', "NAMENODE", "DATANODE")).to.be.true;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
    it('YARN service', function () {
      controller.runRecommission('host1', 'YARN');
      expect(controller.doRecommissionAndStart.calledWith('host1', 'YARN', "RESOURCEMANAGER", "NODEMANAGER")).to.be.true;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
    it('HBASE service', function () {
      controller.runRecommission('host1', 'HBASE');
      expect(controller.doRecommissionAndStart.calledWith('host1', 'HBASE', "HBASE_MASTER", "HBASE_REGIONSERVER")).to.be.true;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
  });

  describe('#doDecommission()', function () {
    it('Query should be sent', function () {
      controller.doDecommission('', '', '', '');
      var args = testHelpers.findAjaxRequest('name', 'host.host_component.decommission_slave');
      expect(args).exists;
    });
  });

  describe('#doDecommissionRegionServer()', function () {
    it('Query should be sent', function () {
      controller.doDecommissionRegionServer('', '', '', '');
      var args = testHelpers.findAjaxRequest('name', 'host.host_component.recommission_and_restart');
      expect(args).exists;
    });
  });

  describe('#warnBeforeDecommission()', function () {
    beforeEach(function () {
      sinon.stub(controller, "showHbaseActiveWarning", Em.K);
      sinon.stub(controller, "checkRegionServerState", Em.K);
    });
    afterEach(function () {
      controller.checkRegionServerState.restore();
      controller.showHbaseActiveWarning.restore();
    });

    it('Component in passive state', function () {
      controller.set('content.hostComponents', [Em.Object.create({
        componentName: 'HBASE_REGIONSERVER',
        passiveState: 'ON'
      })]);
      controller.warnBeforeDecommission('host1');
      expect(controller.checkRegionServerState.calledOnce).to.be.true;
    });
    it('Component is not in passive state', function () {
      controller.set('content.hostComponents', [Em.Object.create({
        componentName: 'HBASE_REGIONSERVER',
        passiveState: 'OFF'
      })]);
      controller.warnBeforeDecommission('host1');
      expect(controller.showHbaseActiveWarning.calledOnce).to.be.true;
    });
  });

  describe('#checkRegionServerState()', function () {
    var result;
    beforeEach(function () {
      result = controller.checkRegionServerState('host1');
    });
    it('returns object', function () {
      expect(result).to.be.an('object');
    });
    it('request is sent with correct data', function () {
      var args = testHelpers.findAjaxRequest('name', 'host.region_servers.in_inservice');
      expect(args[0]).exists;
      expect(args[0].data.hostNames).to.be.equal('host1');
    });
  });

  describe('#checkRegionServerStateSuccessCallback()', function () {
    beforeEach(function () {
      sinon.stub(controller, "doDecommissionRegionServer", Em.K);
      sinon.stub(controller, "showRegionServerWarning", Em.K);
    });
    afterEach(function () {
      controller.doDecommissionRegionServer.restore();
      controller.showRegionServerWarning.restore();
    });

    it('Decommission all regionservers', function () {
      var data = {
        items: [
          {
            HostRoles: {
              host_name: 'host1'
            }
          },
          {
            HostRoles: {
              host_name: 'host2'
            }
          }
        ]
      };
      controller.checkRegionServerStateSuccessCallback(data, {}, {hostNames: 'host1,host2'});
      expect(controller.showRegionServerWarning.calledOnce).to.be.true;
    });
    it('Decommission one of two regionservers', function () {
      var data = {
        items: [
          {
            HostRoles: {
              host_name: 'host1'
            }
          },
          {
            HostRoles: {
              host_name: 'host2'
            }
          }
        ]
      };
      controller.checkRegionServerStateSuccessCallback(data, {}, {hostNames: 'host1'});
      expect(controller.doDecommissionRegionServer.calledWith('host1', "HBASE", "HBASE_MASTER", "HBASE_REGIONSERVER")).to.be.true;
    });
    it('Decommission one of three regionservers', function () {
      var data = {
        items: [
          {
            HostRoles: {
              host_name: 'host1'
            }
          },
          {
            HostRoles: {
              host_name: 'host2'
            }
          },
          {
            HostRoles: {
              host_name: 'host3'
            }
          }
        ]
      };
      controller.checkRegionServerStateSuccessCallback(data, {}, {hostNames: 'host1'});
      expect(controller.doDecommissionRegionServer.calledWith('host1', "HBASE", "HBASE_MASTER", "HBASE_REGIONSERVER")).to.be.true;
    });
  });

  describe('#showRegionServerWarning()', function () {
    it('modal popup is shown', function () {
      controller.showRegionServerWarning();
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });
  });

  describe('#doRecommissionAndStart()', function () {
    it('Query should be sent', function () {
      controller.doRecommissionAndStart('', '', '', '');
      var args = testHelpers.findAjaxRequest('name', 'host.host_component.recommission_and_restart');
      expect(args).exists;
    });
  });

  describe('#decommissionSuccessCallback()', function () {

    beforeEach(function () {
      sinon.stub(controller, "showBackgroundOperationsPopup", Em.K);
    });
    afterEach(function () {
      controller.showBackgroundOperationsPopup.restore();
    });

    it('data is null', function () {
      expect(controller.decommissionSuccessCallback(null)).to.be.false;
      expect(controller.showBackgroundOperationsPopup.called).to.be.false;
    });
    it('data has Requests', function () {
      var data = {Requests: []};
      expect(controller.decommissionSuccessCallback(data)).to.be.true;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
    it('data has resources', function () {
      var data = {
        resources: [
          {RequestSchedule: {}}
        ]
      };
      expect(controller.decommissionSuccessCallback(data)).to.be.true;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
  });

  describe('#doAction()', function () {

    beforeEach(function () {
      sinon.stub(controller, "validateAndDeleteHost", Em.K);
      sinon.stub(controller, "doStartAllComponents", Em.K);
      sinon.stub(controller, "doStopAllComponents", Em.K);
      sinon.stub(controller, "doRestartAllComponents", Em.K);
      sinon.stub(controller, "onOffPassiveModeForHost", Em.K);
      sinon.stub(controller, "setRackIdForHost", Em.K);
    });

    afterEach(function () {
      controller.validateAndDeleteHost.restore();
      controller.doStartAllComponents.restore();
      controller.doStopAllComponents.restore();
      controller.doRestartAllComponents.restore();
      controller.onOffPassiveModeForHost.restore();
      controller.setRackIdForHost.restore();
    });

    it('"deleteHost" action', function () {
      var option = {context: {action: "deleteHost"}};
      controller.doAction(option);
      expect(controller.validateAndDeleteHost.calledOnce).to.be.true;
    });

    it('"startAllComponents" action, isNotHeartBeating = false', function () {
      var option = {context: {action: "startAllComponents"}};
      controller.set('content', {isNotHeartBeating: false});
      controller.doAction(option);
      expect(controller.doStartAllComponents.calledOnce).to.be.true;
    });

    it('"startAllComponents" action, isNotHeartBeating = true', function () {
      var option = {context: {action: "startAllComponents"}};
      controller.set('content', {isNotHeartBeating: true});
      controller.doAction(option);
      expect(controller.doStartAllComponents.called).to.be.false;
    });

    it('"stopAllComponents" action, isNotHeartBeating = false', function () {
      var option = {context: {action: "stopAllComponents"}};
      controller.set('content', {isNotHeartBeating: false});
      controller.doAction(option);
      expect(controller.doStopAllComponents.calledOnce).to.be.true;
    });

    it('"stopAllComponents" action, isNotHeartBeating = true', function () {
      var option = {context: {action: "stopAllComponents"}};
      controller.set('content', {isNotHeartBeating: true});
      controller.doAction(option);
      expect(controller.doStopAllComponents.called).to.be.false;
    });

    it('"restartAllComponents" action, isNotHeartBeating = false', function () {
      var option = {context: {action: "restartAllComponents"}};
      controller.set('content', {isNotHeartBeating: false});
      controller.doAction(option);
      expect(controller.doRestartAllComponents.calledOnce).to.be.true;
    });

    it('"restartAllComponents" action, isNotHeartBeating = true', function () {
      var option = {context: {action: "restartAllComponents"}};
      controller.set('content', {isNotHeartBeating: true});
      controller.doAction(option);
      expect(controller.doRestartAllComponents.called).to.be.false;
    });

    it('"onOffPassiveModeForHost" action', function () {
      var option = {context: {action: "onOffPassiveModeForHost"}};
      controller.doAction(option);
      expect(controller.onOffPassiveModeForHost.calledWith({action: "onOffPassiveModeForHost"})).to.be.true;
    });

    it('"setRackId" action', function () {
      var option = {context: {action: "setRackId"}};
      controller.doAction(option);
      expect(controller.setRackIdForHost.calledOnce).to.be.true;
    });
  });

  describe("#setRackIdForHost()", function() {
    beforeEach(function(){
      sinon.stub(hostsManagement, 'setRackInfo', Em.K);
    });
    afterEach(function() {
      hostsManagement.setRackInfo.restore();
    });
    it('setRackInfo called with valid arguments', function() {
      controller.set('content.rack', 'rack');
      controller.set('content.hostName', 'host1');
      controller.setRackIdForHost();
      expect(hostsManagement.setRackInfo.calledWith({message: Em.I18n.t('hosts.host.details.setRackId')}, [{hostName: 'host1'}], 'rack')).to.be.true;
    });
  });

  describe('#onOffPassiveModeForHost()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(controller, "hostPassiveModeRequest", Em.K);
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      controller.hostPassiveModeRequest.restore();
    });

    it('popup should be displayed, active = true', function () {
      var popup = controller.onOffPassiveModeForHost({active: true});
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.hostPassiveModeRequest.calledWith('ON')).to.be.true;
    });
    it('popup should be displayed, active = false', function () {
      var popup = controller.onOffPassiveModeForHost({active: false});
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.hostPassiveModeRequest.calledWith('OFF')).to.be.true;
    });
  });

  describe('#hostPassiveModeRequest()', function () {
    it('Query should be sent', function () {
      controller.hostPassiveModeRequest('', '');
      var args = testHelpers.findAjaxRequest('name', 'bulk_request.hosts.passive_state');
      expect(args).exists;
    });
  });

  describe('#doStartAllComponents()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(controller, 'sendComponentCommand', Em.K);
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      controller.sendComponentCommand.restore();
    });

    it('serviceNonClientActiveComponents is empty', function () {
      controller.reopen({
        serviceNonClientActiveComponents: Em.A([])
      });
      controller.doStartAllComponents();
      expect(App.showConfirmationPopup.called).to.be.false;
    });
    it('serviceNonClientActiveComponents is correct', function () {
      controller.reopen({
        serviceNonClientActiveComponents: Em.A([{}])
      });
      var popup = controller.doStartAllComponents();
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.sendComponentCommand.calledWith(
          controller.get('serviceNonClientActiveComponents'),
          Em.I18n.t('hosts.host.maintainance.startAllComponents.context'),
          App.HostComponentStatus.started)
      ).to.be.true;
    });
  });

  describe('#doStopAllComponents()', function () {
    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(controller, 'sendComponentCommand', Em.K);
      sinon.stub(controller, 'checkNnLastCheckpointTime', function(callback){
        callback();
      });
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      controller.sendComponentCommand.restore();
      controller.checkNnLastCheckpointTime.restore();
    });

    it('serviceNonClientActiveComponents is empty', function () {
      controller.reopen({
        serviceNonClientActiveComponents: []
      });
      controller.doStopAllComponents();
      expect(App.showConfirmationPopup.called).to.be.false;
    });

    it('serviceNonClientActiveComponents is correct', function () {
      controller.reopen({
        serviceNonClientActiveComponents: Em.A([{}])
      });

      var popup = controller.doStopAllComponents();
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.sendComponentCommand.calledWith(
          controller.get('serviceNonClientActiveComponents'),
          Em.I18n.t('hosts.host.maintainance.stopAllComponents.context'),
          App.HostComponentStatus.stopped)
      ).to.be.true;
    });
    it('serviceNonClientActiveComponents is correct, NAMENODE started', function () {
      controller.reopen({
        serviceNonClientActiveComponents: Em.A([Em.Object.create({
          componentName: 'NAMENODE',
          workStatus: 'STARTED'
        })])
      });
      controller.set('content.hostComponents', [Em.Object.create({
        componentName: 'NAMENODE',
        workStatus: 'STARTED'
      })]);
      controller.doStopAllComponents();
      expect(controller.checkNnLastCheckpointTime.calledOnce).to.be.true;
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
    });
  });

  describe('#doRestartAllComponents()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(batchUtils, 'restartHostComponents', Em.K);
      sinon.stub(controller, 'checkNnLastCheckpointTime', function(callback){
        callback();
      });
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      batchUtils.restartHostComponents.restore();
      controller.checkNnLastCheckpointTime.restore();
    });

    it('serviceActiveComponents is empty', function () {
      controller.reopen({
        serviceActiveComponents: []
      });
      controller.doRestartAllComponents();
      expect(App.showConfirmationPopup.called).to.be.false;
    });

    it('serviceActiveComponents is correct', function () {
      var components = [{}];
      controller.reopen({
        serviceActiveComponents: components
      });

      var popup = controller.doRestartAllComponents();
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(batchUtils.restartHostComponents.calledWith(components)).to.be.true;
    });
    it('serviceActiveComponents is correct, NAMENODE started', function () {
      controller.reopen({
        serviceActiveComponents: Em.A([Em.Object.create({
          componentName: 'NAMENODE',
          workStatus: 'STARTED'
        })])
      });
      controller.set('content.hostComponents', [Em.Object.create({
        componentName: 'NAMENODE',
        workStatus: 'STARTED'
      })]);
      controller.doRestartAllComponents();
      expect(controller.checkNnLastCheckpointTime.calledOnce).to.be.true;
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
    });
  });

  describe('#getHostComponentsInfo()', function () {

    var result = {
      isReconfigureRequired: false,
      lastComponents: [],
      masterComponents: [],
      nonAddableMasterComponents: [],
      lastMasterComponents: [],
      runningComponents: [],
      nonDeletableComponents: [],
      unknownComponents: [],
      toDecommissionComponents: []
    };

    beforeEach(function () {
      this.stub = sinon.stub(App.HostComponent, 'find').returns([{
        id: 'TASKTRACKER_host1',
        componentName: 'TASKTRACKER'
      }]);
      this.mockTotal = sinon.stub(controller, 'getTotalComponent');
      this.mockTotal.returns(2);
    });

    afterEach(function () {
      this.stub.restore();
      this.mockTotal.restore();
    });

    it('content.hostComponents is null', function () {
      controller.set('content', {hostComponents: null});
      expect(controller.getHostComponentsInfo()).to.eql(result);
    });

    it('content.hostComponents is empty', function () {
      controller.set('content', {hostComponents: []});
      expect(controller.getHostComponentsInfo()).to.eql(result);
    });

    it('content.hostComponents has ZOOKEEPER_SERVER', function () {
      controller.set('content', {
        hostComponents: [Em.Object.create({
          componentName: 'ZOOKEEPER_SERVER',
          workStatus: 'INIT',
          isDeletable: true
        })]
      });
      expect(controller.getHostComponentsInfo().isReconfigureRequired).to.be.true;
    });

    it('content.hostComponents has last component', function () {
      controller.set('content', {
        hostComponents: [Em.Object.create({
          componentName: 'TASKTRACKER',
          displayName: 'TaskTracker',
          workStatus: 'INIT',
          isDeletable: true
        })]
      });
      this.mockTotal.returns(1);
      expect(controller.getHostComponentsInfo().lastComponents).to.eql(['TaskTracker']);
    });

    it('content.hostComponents has master non-deletable component', function () {
      controller.set('content', {
        hostComponents: [Em.Object.create({
          componentName: 'TASKTRACKER',
          workStatus: 'INIT',
          isDeletable: false,
          isMaster: true,
          displayName: 'ZK1'
        })]
      });
      expect(controller.getHostComponentsInfo().masterComponents).to.eql(['ZK1']);
      expect(controller.getHostComponentsInfo().nonDeletableComponents).to.eql(['ZK1']);
    });

    it('content.hostComponents has running component', function () {
      controller.set('content', {
        hostComponents: [Em.Object.create({
          componentName: 'TASKTRACKER',
          workStatus: 'STARTED',
          isDeletable: true,
          displayName: 'ZK1'
        })]
      });
      expect(controller.getHostComponentsInfo().runningComponents).to.eql(['ZK1']);
    });

    it('content.hostComponents has non-deletable component', function () {
      controller.set('content', {
        hostComponents: [Em.Object.create({
          componentName: 'TASKTRACKER',
          workStatus: 'INIT',
          isDeletable: false,
          displayName: 'ZK1'
        })]
      });
      expect(controller.getHostComponentsInfo().nonDeletableComponents).to.eql(['ZK1']);
    });

    it('content.hostComponents has component with UNKNOWN state', function () {
      controller.set('content', {
        hostComponents: [Em.Object.create({
          componentName: 'TASKTRACKER',
          workStatus: 'UNKNOWN',
          isDeletable: false,
          displayName: 'ZK1'
        })]
      });
      expect(controller.getHostComponentsInfo().unknownComponents).to.eql(['ZK1']);
    });

  });

  describe('#validateAndDeleteHost()', function () {

    beforeEach(function () {
      sinon.spy(controller, "showReconfigurationPopupPreDelete");
      sinon.stub(controller, "getHostComponentsInfo", function () {
        return this.get('mockHostComponentsInfo');
      });
      sinon.stub(controller, "raiseDeleteComponentsError", Em.K);
      sinon.stub(controller, "confirmDeleteHost", Em.K);
      sinon.stub(controller, 'reconfigureAndDeleteHost');
    });
    afterEach(function () {
      controller.showReconfigurationPopupPreDelete.restore();
      controller.getHostComponentsInfo.restore();
      controller.raiseDeleteComponentsError.restore();
      controller.confirmDeleteHost.restore();
      controller.reconfigureAndDeleteHost.restore();
    });

    it('nonDeletableComponents exist', function () {
      controller.set('mockHostComponentsInfo', {
        nonDeletableComponents: [
          {}
        ]
      });
      controller.validateAndDeleteHost();
      expect(controller.raiseDeleteComponentsError.calledWith({
        nonDeletableComponents: [
          {}
        ]
      }, 'nonDeletableList')).to.be.true;
    });
    it('nonAddableMasterComponents exist', function () {
      controller.set('mockHostComponentsInfo', {
        nonDeletableComponents: [],
        nonAddableMasterComponents: [
        {}
        ]
      });
      controller.validateAndDeleteHost();
      expect(controller.raiseDeleteComponentsError.calledWith({
        nonDeletableComponents: [],
        nonAddableMasterComponents: [
          {}
        ]
      }, 'masterList')).to.be.true;
    });
    it('runningComponents exist', function () {
      controller.set('mockHostComponentsInfo', {
        nonAddableMasterComponents: [],
        nonDeletableComponents: [],
        runningComponents: [{}]
      });
      controller.validateAndDeleteHost();
      expect(controller.raiseDeleteComponentsError.calledWith({
        nonAddableMasterComponents: [],
        nonDeletableComponents: [],
        runningComponents: [{}]
      }, 'runningList')).to.be.true;
    });
    it('lastMasterComponents exist', function () {
      controller.set('mockHostComponentsInfo', {
        nonAddableMasterComponents: [],
        nonDeletableComponents: [],
        runningComponents: [],
        lastMasterComponents: [{}]
      });
      controller.validateAndDeleteHost();
      expect(controller.raiseDeleteComponentsError.calledWith({
        nonAddableMasterComponents: [],
        nonDeletableComponents: [],
        runningComponents: [],
        lastMasterComponents: [{}]
      }, 'lastMasterList')).to.be.true;
    });
    it('isReconfigureRequired = true', function () {
      controller.set('mockHostComponentsInfo', {
        nonAddableMasterComponents: [],
        nonDeletableComponents: [],
        runningComponents: [],
        unknownComponents: [],
        lastComponents: [],
        lastMasterComponents: [],
        isReconfigureRequired: true
      });
      controller.validateAndDeleteHost();
      expect(controller.reconfigureAndDeleteHost.calledWith({
        nonAddableMasterComponents: [],
        nonDeletableComponents: [],
        runningComponents: [],
        unknownComponents: [],
        lastComponents: [],
        lastMasterComponents: [],
        isReconfigureRequired: true
      })).to.be.true;
    });
    it('isReconfigureRequired = false', function () {
      controller.set('mockHostComponentsInfo', {
        nonAddableMasterComponents: [],
        nonDeletableComponents: [],
        runningComponents: [],
        unknownComponents: [],
        lastComponents: [],
        lastMasterComponents: [],
        isReconfigureRequired: false
      });
      controller.validateAndDeleteHost();
      expect(controller.confirmDeleteHost.calledWith({
        nonAddableMasterComponents: [],
        nonDeletableComponents: [],
        runningComponents: [],
        unknownComponents: [],
        lastComponents: [],
        lastMasterComponents: [],
        isReconfigureRequired: false
      })).to.be.true;
    });
  });

  describe('#reconfigureAndDeleteHost', function() {
    beforeEach(function() {
      sinon.stub(controller, 'loadComponentRelatedConfigs');
      sinon.stub(controller, 'showReconfigurationPopupPreDelete', function(c, callback, msg) {
        callback();
      });
      sinon.stub(controller, 'confirmDeleteHost');
      controller.set('content', {
        hostComponents: [
          Em.Object.create({
            componentName: 'DATANODE',
            displayName: 'Datanode'
          }),
          Em.Object.create({
            componentName: 'ZOOKEEPER_SERVER',
            displayName: 'Zookeeper Server'
          })
        ],
        hostName: 'host1'
      });
      controller.set('addDeleteComponentsMap', {
        'ZOOKEEPER_SERVER': {
          hostPropertyName: 'zkhost',
          addPropertyName: 'prop1',
          configTagsCallbackName: 'clb1',
          configsCallbackName: 'clb2'
        }
      });

      controller.reconfigureAndDeleteHost({});
    });
    afterEach(function() {
      controller.loadComponentRelatedConfigs.restore();
      controller.showReconfigurationPopupPreDelete.restore();
      controller.confirmDeleteHost.restore();
    });

    it('hostPropertyName should be "host1"', function() {
      expect(controller.get('zkhost')).to.be.equal('host1')
    });
    it('addPropertyName should be true', function() {
      expect(controller.get('prop1')).to.be.true;
    });
    it('loadComponentRelatedConfigs should be called', function() {
      expect(controller.loadComponentRelatedConfigs.calledWith('clb1', 'clb2')).to.be.true;
    });
    it('showReconfigurationPopupPreDelete should be called', function() {
      expect(controller.showReconfigurationPopupPreDelete.calledOnce).to.be.true;
    });
    it('confirmDeleteHost should be called', function() {
      expect(controller.confirmDeleteHost.calledWith({})).to.be.true;
    });

  });

  describe('#raiseDeleteComponentsError()', function () {
    it('Popup should be displayed', function () {
      controller.raiseDeleteComponentsError([], '');
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });
  });

  describe('#confirmDeleteHost()', function () {

    beforeEach(function () {
      sinon.stub(controller, 'doDeleteHost');
    });

    afterEach(function () {
      controller.doDeleteHost.restore();
    });

    it('Popup should be displayed', function () {
      var popup = controller.confirmDeleteHost({toDecommissionComponents:[]});
      expect(App.ModalPopup.show.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.doDeleteHost.calledOnce).to.be.true;
    });
  });

  describe('#setRackId', function () {
    beforeEach(function () {
      sinon.stub(hostsManagement, 'setRackInfo', Em.K);

    });
    afterEach(function () {
      hostsManagement.setRackInfo.restore();
    });
    it('should call setRackInfo with appropriate arguments', function () {
      var mockedHost = Em.Object.create({
        rack: 'rackId'
      });
      controller.setRackId({
        context: mockedHost
      });
      expect(hostsManagement.setRackInfo.calledWith({message: Em.I18n.t('hosts.host.details.setRackId')}, [mockedHost], 'rackId')).to.be.true;
    });
  });

  describe('#restartAllStaleConfigComponents()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(batchUtils, "restartHostComponents", Em.K);
      sinon.stub(controller, 'checkNnLastCheckpointTime', function(callback){
        callback();
      });
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      batchUtils.restartHostComponents.restore();
      controller.checkNnLastCheckpointTime.restore();
    });

    it('popup should be displayed', function () {
      controller.set('content', {
        componentsWithStaleConfigs: [
          {}
        ]
      });
      var popup = controller.restartAllStaleConfigComponents();
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(batchUtils.restartHostComponents.calledWith([
        {}
      ])).to.be.true;
    });

    it('popup ro check NameNode checkpoint should be displayed first', function () {
      controller.set('content.componentsWithStaleConfigs', [Em.Object.create({
        componentName: 'NAMENODE',
        workStatus: 'STARTED'
      })]);
      controller.set('content.hostComponents', [Em.Object.create({
        componentName: 'NAMENODE',
        workStatus: 'STARTED'
      })]);
      controller.restartAllStaleConfigComponents();
      expect(controller.checkNnLastCheckpointTime.calledOnce).to.be.true;
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
    });
  });

  describe.skip('#moveComponent()', function () {

    var jQueryMock,
      mock = {
        saveComponentToReassign: Em.K,
        getSecurityStatus: Em.K,
        setCurrentStep: Em.K
      },
      cases = [
        {
          isDisabled: false,
          showConfirmationPopupCallCount: 1,
          title: 'popup should be displayed'
        },
        {
          isDisabled: true,
          showConfirmationPopupCallCount: 0,
          title: 'popup shouldn\'t be displayed'
        }
      ];

    beforeEach(function () {
      jQueryMock = sinon.stub(window, '$');
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(App.router, 'get').withArgs('reassignMasterController').returns(mock);
      sinon.stub(App.router, 'transitionTo', Em.K);
      sinon.spy(mock, "saveComponentToReassign");
      sinon.spy(mock, "getSecurityStatus");
      sinon.spy(mock, "setCurrentStep");
    });

    afterEach(function () {
      window.$.restore();
      App.showConfirmationPopup.restore();
      App.router.get.restore();
      App.router.transitionTo.restore();
      mock.saveComponentToReassign.restore();
      mock.getSecurityStatus.restore();
      mock.setCurrentStep.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        jQueryMock.returns({
          closest: function () {
            return {
              hasClass: function () {
                return item.isDisabled;
              }
            }
          }
        });
        var popup = controller.moveComponent({context: {}});
        expect(App.showConfirmationPopup.callCount).to.equal(item.showConfirmationPopupCallCount);
        if (item.showConfirmationPopupCallCount) {
          popup.onPrimary();
          expect(App.router.get.calledWith('reassignMasterController')).to.be.true;
          expect(mock.saveComponentToReassign.calledWith({})).to.be.true;
          expect(mock.getSecurityStatus.calledOnce).to.be.true;
          expect(mock.setCurrentStep.calledWith('1')).to.be.true;
          expect(App.router.transitionTo.calledWith('reassign')).to.be.true;
        }
      });
    });

  });

  describe('#refreshConfigs()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(batchUtils, "restartHostComponents", Em.K);
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      batchUtils.restartHostComponents.restore();
    });

    it('No components', function () {
      var event = {context: Em.A([])};
      controller.refreshConfigs(event);
      expect(App.showConfirmationPopup.called).to.be.false;
    });
    it('Some components present', function () {
      var event = {context: Em.A([Em.Object.create()])};
      var popup = controller.refreshConfigs(event);
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(batchUtils.restartHostComponents.calledWith(event.context)).to.be.true;
    });
  });

  describe('#getTotalComponent()', function () {

    beforeEach(function () {
      sinon.stub(App.SlaveComponent, 'find', function () {
        return Em.Object.create({
          componentName: "SLAVE",
          totalCount: 1
        });
      });
      sinon.stub(App.ClientComponent, 'find', function () {
        return Em.Object.create({
          componentName: "CLIENT",
          totalCount: 1
        });
      });
      sinon.stub(App.HostComponent, 'find', function () {
        return [Em.Object.create({
          componentName: "MASTER",
          totalCount: 1
        })]
      });
    });
    afterEach(function () {
      App.SlaveComponent.find.restore();
      App.ClientComponent.find.restore();
      App.HostComponent.find.restore();
    });

    it('component is slave', function () {
      expect(controller.getTotalComponent(Em.Object.create({
        componentName: "SLAVE",
        isSlave: true
      }))).to.equal(1);
    });
    it('component is client', function () {
      expect(controller.getTotalComponent(Em.Object.create({
        componentName: "CLIENT",
        isClient: true
      }))).to.equal(1);
    });
    it('component is master', function () {
      expect(controller.getTotalComponent(Em.Object.create({
        componentName: "MASTER"
      }))).to.equal(1);
    });
    it('unknown component', function () {
      expect(controller.getTotalComponent(Em.Object.create({
        componentName: "UNKNOWN"
      }))).to.equal(0);
    });
  });

  describe('#downloadClientConfigsCall', function () {

    beforeEach(function () {
      sinon.stub(controller, 'downloadClientConfigsCall', Em.K);
    });
    afterEach(function () {
      controller.downloadClientConfigsCall.restore();
    });

    it('should launch controller.downloadClientConfigsCall method', function () {
      controller.downloadClientConfigs({
        context: Em.Object.create({
          componentName: 'name',
          hostName: 'host1'
        })
      });
      expect(controller.downloadClientConfigsCall.calledWith({
        componentName: 'name',
        hostName: 'host1',
        resourceType: controller.resourceTypeEnum.HOST_COMPONENT
      })).to.be.true;
    });
  });

  describe('#downloadAllClientConfigs', function () {

    beforeEach(function () {
      sinon.stub(controller, 'downloadClientConfigsCall', Em.K);
      sinon.stub(controller, 'get').withArgs('content.hostName').returns('host1');
    });
    afterEach(function () {
      controller.downloadClientConfigsCall.restore();
      controller.get.restore();
    });

    it('should launch controller.downloadClientConfigsCall method', function () {
      controller.downloadAllClientConfigs();
      expect(controller.downloadClientConfigsCall.calledWith({
        hostName: 'host1',
        resourceType: controller.resourceTypeEnum.HOST
      })).to.be.true;
    });
  });

  describe('#executeCustomCommands', function () {
    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
    });

    it('confirm popup should be displayed', function () {
      var popup = controller.executeCustomCommand({context: Em.Object.create()});
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      var args = testHelpers.findAjaxRequest('name', 'service.item.executeCustomCommand');
      expect(args).exists;
    });
  });

  describe('#_doDeleteHostComponent()', function () {
    it('single component', function () {
      controller.set('content.hostName', 'host1');
      var componentName = 'COMP';
      controller._doDeleteHostComponent(componentName);
      var args = testHelpers.findAjaxRequest('name', 'common.delete.host_component');
      expect(args[0]).exists;
      expect(args[0].data).to.be.eql({
        componentName: 'COMP',
        hostName: 'host1'
      });
    });
    it('all components', function () {
      controller.set('content.hostName', 'host1');
      controller._doDeleteHostComponent(null);
      var args = testHelpers.findAjaxRequest('name', 'common.delete.host');
      expect(args[0]).exists;
      expect(args[0].data).to.be.eql({
        componentName: '',
        hostName: 'host1'
      });
    });
  });

  describe('#_doDeleteHostComponentSuccessCallback()', function () {
    var data = {
      componentName: 'COMPONENT',
      hostName: 'h1'
    };

    beforeEach(function () {
      controller.set('_deletedHostComponentError', {});
      controller._doDeleteHostComponentSuccessCallback({}, {}, data);
    });

    it('should reset `_deletedHostComponentError`', function () {
      expect(controller.get('_deletedHostComponentError')).to.be.null;
    });
  });

  describe('#upgradeComponentSuccessCallback()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'showBackgroundOperationsPopup', Em.K);
      sinon.stub(controller, 'mimicWorkStatusChange', Em.K);
    });
    afterEach(function () {
      controller.mimicWorkStatusChange.restore();
      controller.showBackgroundOperationsPopup.restore();
    });
    it('testMode is false', function () {
      controller.upgradeComponentSuccessCallback({}, {}, {component: "COMP"});
      expect(controller.mimicWorkStatusChange.called).to.be.false;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
  });

  describe('#refreshComponentConfigsSuccessCallback()', function () {

    beforeEach(function () {
      sinon.stub(controller, 'showBackgroundOperationsPopup', Em.K);
    });

    afterEach(function () {
      controller.showBackgroundOperationsPopup.restore();
    });

    it('call showBackgroundOperationsPopup', function () {
      controller.refreshComponentConfigsSuccessCallback();
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
  });

  describe('#_doDeleteHostComponentErrorCallback()', function () {
    it('should set error to _deletedHostComponentError', function () {
      controller._doDeleteHostComponentErrorCallback({}, 'textStatus', {}, {url: 'url'});
      expect(controller.get('_deletedHostComponentError')).to.be.eql({xhr: {}, url: 'url', method: 'DELETE'});
    });
  });

  describe('#installComponentSuccessCallback()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'showBackgroundOperationsPopup', Em.K);
      sinon.stub(controller, 'mimicWorkStatusChange', Em.K);
    });
    afterEach(function () {
      controller.mimicWorkStatusChange.restore();
      controller.showBackgroundOperationsPopup.restore();
    });

    it('testMode is false', function () {
      controller.installComponentSuccessCallback({}, {}, {component: "COMP"});
      expect(controller.mimicWorkStatusChange.called).to.be.false;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
  });

  describe('#showHbaseActiveWarning()', function () {
    it('popup should be displayed', function () {
      controller.showHbaseActiveWarning(Em.Object.create({service: {}}));
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });
  });

  describe('#updateHost()', function () {

    beforeEach(function () {
      sinon.stub(batchUtils, "infoPassiveState", Em.K);
    });

    afterEach(function () {
      batchUtils.infoPassiveState.restore();
    });

    it('popup should be displayed', function () {
      controller.updateHost({}, {}, {passive_state: 'state'});
      expect(controller.get('content.passiveState')).to.equal('state');
      expect(batchUtils.infoPassiveState.calledWith('state')).to.be.true;
    });
  });

  describe('#updateComponentPassiveState()', function () {
    it('popup should be displayed', function () {
      controller.set('content.hostName', 'host1');
      var component = Em.Object.create({
        componentName: 'COMP1'
      });
      controller.updateComponentPassiveState(component, 'state', 'message');
      var args = testHelpers.findAjaxRequest('name', 'common.host.host_component.passive');
      expect(args[0]).exists;
      expect(args[0].data).to.be.eql({
        "hostName": "host1",
        "componentName": "COMP1",
        "component": component,
        "passive_state": "state",
        "context": "message"
      });
    });
  });

  describe('#updateHostComponent()', function () {

    var params = {
      component: Em.Object.create(),
      passive_state: 'state'
    };

    beforeEach(function () {
      sinon.stub(batchUtils, "infoPassiveState", Em.K);
    });

    afterEach(function () {
      batchUtils.infoPassiveState.restore();
    });

    it('popup should be displayed', function () {
      controller.updateHostComponent({}, {}, params);
      expect(params.component.get('passiveState')).to.equal('state');
      expect(batchUtils.infoPassiveState.calledWith('state')).to.be.true;
    });
  });

  describe('#toggleMaintenanceMode()', function () {
    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(controller, 'updateComponentPassiveState');
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      controller.updateComponentPassiveState.restore();
    });
    it('passive state is ON', function () {
      var event = {
        context: Em.Object.create({
          passiveState: 'ON'
        })
      };
      var popup = controller.toggleMaintenanceMode(event);
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.updateComponentPassiveState.calledWith(Em.Object.create({
        passiveState: 'ON'
      }), 'OFF')).to.be.true;
    });
    it('passive state is OFF', function () {
      var event = {
        context: Em.Object.create({
          passiveState: 'OFF'
        })
      };
      var popup = controller.toggleMaintenanceMode(event);
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.updateComponentPassiveState.calledWith(Em.Object.create({
        passiveState: 'OFF'
      }), 'ON')).to.be.true;
    });
    it('isImpliedState is true', function () {
      var event = {
        context: Em.Object.create({
          isImpliedState: true
        })
      };
      var result = controller.toggleMaintenanceMode(event);
      expect(App.showConfirmationPopup.calledOnce).to.be.false;
      expect(result).to.be.null;
    });
  });

  describe('#installClients()', function () {

    var cases = [
        {
          context: [
            Em.Object.create({
              componentName: 'c0',
              workStatus: 'INSTALLED'
            }),
            Em.Object.create({
              componentName: 'c1',
              workStatus: 'INIT'
            }),
            Em.Object.create({
              componentName: 'c2',
              workStatus: 'INSTALL_FAILED'
            })
          ],
          dependencies: {
            c0: [],
            c1: [],
            c2: []
          },
          getSecurityTypeCalled: null, //should have same value as getKDCSessionStateCalled, always
          getKDCSessionStateCalled: true,
          sendComponentCommandCalled: true,
          showAlertPopupCalled: false,
          title: 'No clients to add, some clients to install'
        },
        {
          context: [
            Em.Object.create({
              componentName: 'c3',
              displayName: 'c3'
            })
          ],
          dependencies: {
            c3: []
          },
          getSecurityTypeCalled: null, //should have same value as getKDCSessionStateCalled, always
          getKDCSessionStateCalled: true,
          sendComponentCommandCalled: false,
          showAlertPopupCalled: false,
          title: 'No clients to install, some clients to add'
        },
        {
          context: [
            Em.Object.create({
              componentName: 'c4',
              displayName: 'c4'
            })
          ],
          dependencies: {
            c4: ['c5']
          },
          getSecurityTypeCalled: null, //should have same value as getKDCSessionStateCalled, always
          getKDCSessionStateCalled: false,
          sendComponentCommandCalled: false,
          showAlertPopupCalled: true,
          title: 'Clients to add have unresolved dependencies'
        },
        {
          context: [
            Em.Object.create({
              componentName: 'c5',
              displayName: 'c5'
            }),
            Em.Object.create({
              componentName: 'c6',
              displayName: 'c6'
            })
          ],
          dependencies: {
            c5: ['c6'],
            c6: ['c5']
          },
          getSecurityTypeCalled: null, //should have same value as getKDCSessionStateCalled, always
          getKDCSessionStateCalled: true,
          sendComponentCommandCalled: false,
          showAlertPopupCalled: false,
          title: 'Clients to add have mutual dependencies'
        }
      ];

    beforeEach(function () {
      sinon.stub(controller, 'sendComponentCommand', Em.K);
      sinon.stub(controller, 'showAddComponentPopup', Em.K);
      sinon.stub(App.get('router.mainAdminKerberosController'), 'getKDCSessionState', function (arg) {
        return arg();
      });
      sinon.stub(App.get('router.mainAdminKerberosController'), 'getSecurityType', function (arg) {
        return arg();
      });
      sinon.stub(App, 'showAlertPopup', Em.K);
      sinon.stub(App.StackServiceComponent, 'find', function (componentName) {
        return Em.Object.create({
          displayName: componentName
        });
      });
      controller.set('content.hostComponents', []);
    });
    afterEach(function () {
      controller.sendComponentCommand.restore();
      controller.showAddComponentPopup.restore();
      App.get('router.mainAdminKerberosController').getKDCSessionState.restore();
      App.get('router.mainAdminKerberosController').getSecurityType.restore();
      App.showAlertPopup.restore();
      App.StackServiceComponent.find.restore();
      controller.checkComponentDependencies.restore();
    });

    cases.forEach(function (item) {
      describe(item.title, function () {

        beforeEach(function () {
          sinon.stub(controller, 'checkComponentDependencies', function (componentName) {
            return item.dependencies[componentName];
          });
          controller.installClients(item.context);
        });

        it('getSecurityType is ' + (item.getKDCSessionStateCalled ? '' : 'not') + ' called', function() {
          expect(App.get('router.mainAdminKerberosController').getSecurityType.calledOnce).to.equal(item.getKDCSessionStateCalled);
        });

        it('getKDCSessionState is ' + (item.getKDCSessionStateCalled ? '' : 'not') + ' called', function() {
          expect(App.get('router.mainAdminKerberosController').getKDCSessionState.calledOnce).to.equal(item.getKDCSessionStateCalled);
        });

        it('sendComponentCommand is ' + (item.sendComponentCommandCalled ? '' : 'not') + ' called', function() {
          expect(controller.sendComponentCommand.calledOnce).to.equal(item.sendComponentCommandCalled);
        });

        it('showAlertPopup is ' + (item.showAlertPopupCalled ? '' : 'not') + ' called', function() {
          expect(App.showAlertPopup.calledOnce).to.equal(item.showAlertPopupCalled);
        });

      });
    });
  });

  describe("#executeCustomCommandErrorCallback()", function () {
    beforeEach(function () {
      sinon.stub($, 'parseJSON');
      sinon.spy(App, 'showAlertPopup');
    });
    afterEach(function () {
      App.showAlertPopup.restore();
      $.parseJSON.restore();
    });
    it("data empty", function () {
      controller.executeCustomCommandErrorCallback(null);

      expect(App.showAlertPopup.calledWith(Em.I18n.t('services.service.actions.run.executeCustomCommand.error'), Em.I18n.t('services.service.actions.run.executeCustomCommand.error'))).to.be.true;
      expect($.parseJSON.called).to.be.false;
    });
    it("responseText empty", function () {
      var data = {
        responseText: null
      };
      controller.executeCustomCommandErrorCallback(data);

      expect(App.showAlertPopup.calledWith(Em.I18n.t('services.service.actions.run.executeCustomCommand.error'), Em.I18n.t('services.service.actions.run.executeCustomCommand.error'))).to.be.true;
      expect($.parseJSON.called).to.be.false;
    });
    it("data empty (2)", function () {
      var data = {
        responseText: "test"
      };
      controller.executeCustomCommandErrorCallback(data);
      expect(App.showAlertPopup.calledWith(Em.I18n.t('services.service.actions.run.executeCustomCommand.error'), Em.I18n.t('services.service.actions.run.executeCustomCommand.error'))).to.be.true;
      expect($.parseJSON.calledWith('test')).to.be.true;
    });
  });

  describe('#doDeleteHost', function() {
    beforeEach(function() {
      sinon.stub(controller, '_doDeleteHostComponent').returns($.Deferred().resolve().promise());
      sinon.stub(controller, 'applyConfigsCustomization');
      sinon.stub(controller, 'putConfigsToServer');
      sinon.stub(controller, 'clearConfigsChanges');
      sinon.stub(controller, 'deleteHostCall');
      controller.set('content', Em.Object.create({
        hostComponents: [
          Em.Object.create({componentName: 'ZOOKEEPER_SERVER', displayName: 'Zookeeper Server'})
        ]
      }));
      controller.set('isReconfigureRequired', true);
      controller.set('groupedPropertiesToChange', []);


      controller.doDeleteHost();
    });
    afterEach(function() {
      controller._doDeleteHostComponent.restore();
      controller.applyConfigsCustomization.restore();
      controller.putConfigsToServer.restore();
      controller.clearConfigsChanges.restore();
      controller.deleteHostCall.restore();
    });

    it('_doDeleteHostComponent should be called', function() {
      expect(controller._doDeleteHostComponent.calledWith('ZOOKEEPER_SERVER')).to.be.true;
    });
    it('applyConfigsCustomization should be called', function() {
      expect(controller.applyConfigsCustomization.calledOnce).to.be.true;
    });
    it('putConfigsToServer should be called', function() {
      expect(controller.putConfigsToServer.calledWith([], 'Zookeeper Server')).to.be.true;
    });
    it('clearConfigsChanges should be called', function() {
      expect(controller.clearConfigsChanges.calledOnce).to.be.true;
    });
    it('deleteHostCall should be called', function() {
      expect(controller.deleteHostCall.calledOnce).to.be.true;
    });
  });

  describe('#deleteHostCall', function() {

    it('App.ajax.send should be called', function() {
      controller.deleteHostCall();
      var args = testHelpers.findAjaxRequest('name', 'common.delete.host');
      expect(args[0]).exists;
    });
  });

  describe("#deleteHostCallSuccessCallback", function () {
    var mock;
    beforeEach(function () {
      mock = {
        updateHost: function (callback) {
          callback();
        },
        getAllHostNames: Em.K
      };
      sinon.stub(App.router, 'get').withArgs('updateController').returns(mock).withArgs('clusterController').returns(mock);
      sinon.spy(mock, 'updateHost');
      sinon.spy(mock, 'getAllHostNames');
      sinon.stub(controller, 'loadConfigs', Em.K);
      sinon.stub(App.router, 'transitionTo', Em.K);
      sinon.stub(controller, 'isServiceMetricsLoaded', Em.clb);
      controller.deleteHostCallSuccessCallback();
    });

    afterEach(function () {
      App.router.get.restore();
      mock.updateHost.restore();
      mock.getAllHostNames.restore();
      controller.loadConfigs.restore();
      controller.isServiceMetricsLoaded.restore();
      App.router.transitionTo.restore();
    });

    it('updateController is used', function () {
      expect(App.router.get.calledWith('updateController')).to.be.true;
    });
    it('updateHost is called once', function () {
      expect(mock.updateHost.calledOnce).to.be.true;
    });
    it('loadConfigs is not called', function () {
      expect(controller.loadConfigs.called).to.be.false;
    });
    it('user is moved to the hosts', function () {
      expect(App.router.transitionTo.calledWith('hosts.index')).to.be.true;
    });
    it('clusterController is used', function () {
      expect(App.router.get.calledWith('clusterController')).to.be.true;
    });
    it('getAllHostNames is called once', function () {
      expect(mock.getAllHostNames.calledOnce).to.be.true;
    });
  });

  describe("#deleteHostCallErrorCallback", function () {

    beforeEach(function () {
      sinon.stub(App.ajax, 'defaultErrorHandler', Em.K);
      controller.deleteHostCallErrorCallback({
        status: 'status',
        statusText: "statusText"
      }, 'textStatus', 'errorThrown', {url: 'url'});
    });

    afterEach(function () {
      App.ajax.defaultErrorHandler.restore();
    });

    it('defaultErrorHandler is called once', function () {
      expect(App.ajax.defaultErrorHandler.calledOnce).to.be.true;
    });
  });

  describe('#installVersionConfirmation()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(controller, 'installVersion', Em.K);
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      controller.installVersion.restore();
    });

    it('confirm popup should be displayed', function () {
      var event = {context: Em.Object.create({displayName: 'displayName'})};
      var popup = controller.installVersionConfirmation(event);
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.installVersion.calledWith(event)).to.be.true;
    });
  });

  describe("#installVersion()", function () {
    it("call App.ajax.send", function () {
      controller.set('content.hostName', 'host1');
      controller.installVersion({context: {}});
      var args = testHelpers.findAjaxRequest('name', 'host.stack_versions.install');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].data).to.be.eql({
        hostName: 'host1',
        version: {}
      });
    });
  });

  describe("#installVersionSuccessCallback()", function () {
    var version = Em.Object.create({
      id: 1,
      status: 'INIT'
    });
    beforeEach(function () {
      this.mock = sinon.stub(App.HostStackVersion, 'find');
      this.mock.returns(version);
      sinon.stub(App.db, 'set', Em.K);
      sinon.stub(App.clusterStatus, 'setClusterStatus', Em.K);
      controller.installVersionSuccessCallback({Requests: {id: 1}}, {}, {version: version});
    });
    afterEach(function () {
      this.mock.restore();
      App.db.set.restore();
      App.clusterStatus.setClusterStatus.restore();
    });
    it("status is INSTALLING", function () {
      expect(version.get('status')).to.equal('INSTALLING');
    });
    it('valid data is saved to the localDB', function () {
      expect(App.db.set.calledWith('repoVersionInstall', 'id', [1])).to.be.true;
    });
    it('clusterStatus is updated', function () {
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });
  });

  describe('#getHiveHosts()', function () {

    var cases = [
      {
        'input': {
          'hiveMetastoreHost': '',
          'fromDeleteHost': false,
          'deleteHiveMetaStore': false,
          'deleteWebHCatServer': false
        },
        'hiveHosts': ['h1', 'h2', 'h4'],
        'title': 'adding HiveServer2'
      },
      {
        'input': {
          'hiveMetastoreHost': 'h0',
          'fromDeleteHost': false,
          'deleteHiveMetaStore': false,
          'deleteWebHCatServer': false
        },
        'hiveHosts': ['h0', 'h1', 'h2', 'h4'],
        'title': 'adding Hive Metastore'
      },
      {
        'input': {
          'webhcatServerHost': 'h0',
          'fromDeleteHost': false,
          'deleteHiveMetaStore': false,
          'deleteWebHCatServer': false
        },
        'hiveHosts': ['h0', 'h1', 'h2', 'h4'],
        'title': 'adding WebHCat Server'
      },
      {
        'input': {
          'hiveMetastoreHost': '',
          'content.hostName': 'h1',
          'fromDeleteHost': false,
          'deleteHiveMetaStore': true,
          'deleteWebHCatServer': false
        },
        'hiveHosts': ['h2', 'h4'],
        'title': 'deleting Hive component'
      },
      {
        'input': {
          'hiveMetastoreHost': '',
          'content.hostName': 'h4',
          'fromDeleteHost': false,
          'deleteHiveMetaStore': false,
          'deleteWebHCatServer': true
        },
        'hiveHosts': ['h1', 'h2'],
        'title': 'deleting WebHCat Server'
      },
      {
        'input': {
          'hiveMetastoreHost': '',
          'content.hostName': 'h2',
          'fromDeleteHost': true,
          'deleteHiveMetaStore': false,
          'deleteWebHCatServer': false
        },
        'hiveHosts': ['h1', 'h4'],
        'title': 'deleting host with Hive component'
      },
      {
        'input': {
          'webhcatServerHost': '',
          'content.hostName': 'h2',
          'fromDeleteHost': true,
          'deleteHiveMetaStore': false,
          'deleteWebHCatServer': false
        },
        'hiveHosts': ['h1', 'h4'],
        'title': 'deleting host with WebHCat Server'
      }
    ];

    before(function () {
      sinon.stub(App.HostComponent, 'find').returns([
        {
          componentName: 'HIVE_METASTORE',
          hostName: 'h2'
        },
        {
          componentName: 'HIVE_METASTORE',
          hostName: 'h1'
        },
        {
          componentName: 'HIVE_SERVER',
          hostName: 'h3'
        },
        {
          componentName: 'WEBHCAT_SERVER',
          hostName: 'h4'
        }
      ]);
    });

    after(function () {
      App.HostComponent.find.restore();
    });

    cases.forEach(function (item) {
      describe(item.title, function () {

        beforeEach(function () {
          Em.keys(item.input).forEach(function (key) {
            controller.set(key, item.input[key]);
          });
          this.hostsMap = controller.getHiveHosts().toArray();
          this.expectedHosts = this.hostsMap.filter(function(hostInfo) {
            return ['WEBHCAT_SERVER', 'HIVE_METASTORE'].contains(hostInfo.component) && hostInfo.isInstalled === true;
          }).mapProperty('hostName').uniq();
        });

        it(JSON.stringify(item.hiveHosts) + ' are in the list', function () {
          expect(this.expectedHosts).to.include.same.members(item.hiveHosts);
        });
        it('hiveMetastoreHost is empty', function () {
          expect(controller.get('hiveMetastoreHost')).to.be.empty;
        });
        it('webhcatServerHost is empty', function () {
          expect(controller.get('webhcatServerHost')).to.be.empty;
        });
        it('fromDeleteHost is false', function () {
          expect(controller.get('fromDeleteHost')).to.be.false;
        });
        it('deleteHiveMetaStore is false', function () {
          expect(controller.get('deleteHiveMetaStore')).to.be.false;
        });
      });
    });

  });

  describe('#onLoadRangerConfigs()', function () {

    var cases = [
      {
        'zookeeperHosts': ['host1'],
        'kmsPort': 'port',
        'title': 'single host',
        'hostToInstall': undefined,
        'result': [
          {
            properties: {
              'core-site': {'hadoop.security.key.provider.path': 'kms://http@host1:port/kms'},
              'hdfs-site': {'dfs.encryption.key.provider.uri': 'kms://http@host1:port/kms'}
            },
            properties_attributes: {
              'core-site': undefined,
              'hdfs-site': undefined
            }
          },
          {
            properties: {
              'kms-site': {
                'hadoop.kms.cache.enable': 'true',
                'hadoop.kms.cache.timeout.ms': '600000',
                'hadoop.kms.current.key.cache.timeout.ms': '30000',
                'hadoop.kms.authentication.signer.secret.provider': 'random',
                'hadoop.kms.authentication.signer.secret.provider.zookeeper.auth.type': 'kerberos',
                'hadoop.kms.authentication.signer.secret.provider.zookeeper.connection.string': '#HOSTNAME#:#PORT#,...'
              }
            },
            properties_attributes: {
              'kms-site': undefined
            }
          }
        ]
      },
      {
        'zookeeperHosts': ['host1', 'host2'],
        'kmsPort': 'port',
        'title': 'two hosts',
        'hostToInstall': 'host2',
        'result': [
          {
            properties: {
              'core-site': {'hadoop.security.key.provider.path': 'kms://http@host2;host1:port/kms'},
              'hdfs-site': {'dfs.encryption.key.provider.uri': 'kms://http@host1;host2:port/kms'}
            },
            properties_attributes: {
              'core-site': undefined,
              'hdfs-site': undefined
            }
          },
          {
            properties: {
              'kms-site': {
                'hadoop.kms.cache.enable': 'false',
                'hadoop.kms.cache.timeout.ms': '0',
                'hadoop.kms.current.key.cache.timeout.ms': '0',
                'hadoop.kms.authentication.signer.secret.provider': 'zookeeper',
                'hadoop.kms.authentication.signer.secret.provider.zookeeper.auth.type': 'none',
                'hadoop.kms.authentication.signer.secret.provider.zookeeper.connection.string': 'host1:2181,host2:2181'
              }
            },
            properties_attributes: {
              'kms-site': undefined
            }
          }
        ]
      }
    ];

    beforeEach(function () {
      sinon.spy(controller, 'setConfigsChanges');
      sinon.stub(App.Service, 'find', function () {
        return [
          Em.Object.create({
            displayName: 'service',
            serviceName: 'RANGER_KMS'
          })
        ];
      });
      sinon.stub(controller, 'saveLoadedConfigs', Em.K);
    });

    afterEach(function () {
      controller.setConfigsChanges.restore();
      App.Service.find.restore();
      controller.saveLoadedConfigs.restore();
    });

    cases.forEach(function (item) {
      describe(item.title, function () {

        var data = {
          items: [
            {
              type: 'kms-env',
              properties: {'kms_port': item.kmsPort}
            },
            {
              type: 'core-site',
              properties: {
                'hadoop.security.key.provider.path': 'kms://http@host2;host1:port/kms'
              }
            },
            {
              type: 'hdfs-site',
              properties: {
                'dfs.encryption.key.provider.uri': 'kms://http@host2:port/kms'
              }
            },
            {
              type: 'kms-site',
              properties: {
                'hadoop.kms.cache.enable': 'true',
                'hadoop.kms.cache.timeout.ms': '600000',
                'hadoop.kms.current.key.cache.timeout.ms': '30000',
                'hadoop.kms.authentication.signer.secret.provider': 'random',
                'hadoop.kms.authentication.signer.secret.provider.zookeeper.auth.type': 'kerberos',
                'hadoop.kms.authentication.signer.secret.provider.zookeeper.connection.string': '#HOSTNAME#:#PORT#,...'
              }
            }
          ]
        };

        beforeEach(function () {
          controller.set('rangerKMSServerHost', item.hostToInstall);
          sinon.stub(App.MasterComponent, 'find').returns(Em.Object.create({hostNames: item.zookeeperHosts}))
          sinon.stub(controller, 'getRangerKMSServerHosts').returns(item.zookeeperHosts);
          controller.onLoadRangerConfigs(data);
        });

        afterEach(function () {
          App.MasterComponent.find.restore();
        });

        it('setConfigsChanges is called with valid arguments', function () {
          expect(controller.setConfigsChanges.calledWith(item.result)).to.be.true;
        });
      });
    });

  });

  describe("#parseNnCheckPointTime", function () {
    var tests = [
      {
        m: "NameNode on this host has JMX data, the last checkpoint time is less than 12 hours ago",
        data:
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
        result: false
      },
      {
        m: "NameNode on this host has JMX data, the last checkpoint time is > 12 hours ago",
        data:
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
        },
        result: "c6401.ambari.apache.org"
      },
      {
        m: "NameNode(standby) on this host has JMX data",
        data:
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
                "LastCheckpointTime" : 1435617248000
              }
            }
          }
        },
        result: false
      },
      {
        m: "NameNode on this host has no JMX data",
        data:
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
        result: null
      },
      {
        m: "NameNode on this host has no JMX data",
        data:
        {
          "href" : "",
          "HostRoles" : {
            "cluster_name" : "c123",
            "component_name" : "NAMENODE",
            "host_name" : "c6401.ambari.apache.org"
          },
          "metrics" : {
          }
        },
        result: null
      }
    ];

    beforeEach(function () {
      sinon.stub(App, 'dateTime').returns(1435790048000);
    });

    afterEach(function () {
      App.dateTime.restore();
    });

    tests.forEach(function (test) {
      it(test.m, function () {
        var mainHostDetailsController = App.MainHostDetailsController.create({isNNCheckpointTooOld: null});
        mainHostDetailsController.parseNnCheckPointTime(test.data);
        expect(mainHostDetailsController.get('isNNCheckpointTooOld')).to.equal(test.result);
      });
    });
  });

  describe("#checkComponentDependencies()", function() {

    beforeEach(function () {
      this.mock = sinon.stub(App.StackServiceComponent, 'find');
      sinon.stub(App.HostComponent, 'find').returns([{
        hostName: 'host1',
        componentName: 'C1'
      }]);
    });
    afterEach(function () {
      this.mock.restore();
      App.HostComponent.find.restore();
    });

    it("no dependencies", function () {
      var opt = {scope: '*'};
      this.mock.withArgs('C1').returns(App.StackServiceComponent.createRecord({
          'dependencies': []
      }));
      expect(controller.checkComponentDependencies('C1', opt)).to.be.empty;
    });
    it("dependecies already installed", function () {
      var opt = {scope: '*', installedComponents: ['C2']};
      this.mock.withArgs('C1').returns(App.StackServiceComponent.createRecord({
        dependencies: [{componentName: 'C2'}]
      }));
      this.mock.withArgs('C2').returns(App.StackServiceComponent.createRecord({ componentName: 'C2' }));
      expect(controller.checkComponentDependencies('C1', opt)).to.be.empty;
    });
    it("dependecies should be added", function () {
      var opt = {scope: '*', installedComponents: ['C2']};
      this.mock.returns([
        App.StackServiceComponent.createRecord({componentName: 'C1'}),
        App.StackServiceComponent.createRecord({componentName: 'C2'}),
        App.StackServiceComponent.createRecord({componentName: 'C3'})
      ]);
      this.mock.withArgs('C1').returns(App.StackServiceComponent.createRecord({
        dependencies: [{componentName: 'C3'}]
      }));
      this.mock.withArgs('C2').returns(App.StackServiceComponent.createRecord({ componentName: 'C2' }));
      this.mock.withArgs('C3').returns(App.StackServiceComponent.createRecord({ componentName: 'C3' }));
      expect(controller.checkComponentDependencies('C1', opt)).to.eql(['C3']);
    });
    it("dependecies should be excluded when exclusive type", function () {
      var opt = {scope: '*', installedComponents: ['C2']};
      this.mock.returns([
        App.StackServiceComponent.createRecord({componentName: 'C1'}),
        App.StackServiceComponent.createRecord({componentName: 'C2'}),
        App.StackServiceComponent.createRecord({componentName: 'C3'})
      ]);
      this.mock.withArgs('C1').returns(App.StackServiceComponent.createRecord({
        dependencies: [{componentName: 'C3', type: 'exclusive'}]
      }));
      this.mock.withArgs('C2').returns(App.StackServiceComponent.createRecord({ componentName: 'C2' }));
      this.mock.withArgs('C3').returns(App.StackServiceComponent.createRecord({ componentName: 'C3' }));
      expect(controller.checkComponentDependencies('C1', opt)).to.be.empty;
    });
    it("dependecies already installed by component type", function () {
      var opt = {scope: '*', installedComponents: ['C3']};
      this.mock.withArgs('C1').returns(App.StackServiceComponent.createRecord({
        dependencies: [{componentName: 'C2'}]
      }));
      this.mock.withArgs('C2').returns(App.StackServiceComponent.createRecord({ componentName: 'C2', componentType: 'HCFS_CLIENT' }));
      this.mock.withArgs('C3').returns(App.StackServiceComponent.createRecord({ componentName: 'C3', componentType: 'HCFS_CLIENT' }));
      expect(controller.checkComponentDependencies('C1', opt)).to.be.empty;
    });
    it("scope is host", function () {
      var opt = {scope: 'host', hostName: 'host1'};
      this.mock.returns([
        App.StackServiceComponent.createRecord({componentName: 'C1'}),
        App.StackServiceComponent.createRecord({componentName: 'C2'}),
        App.StackServiceComponent.createRecord({componentName: 'C3'})
      ]);
      this.mock.withArgs('C1').returns(App.StackServiceComponent.createRecord({
        dependencies: [{componentName: 'C3', scope: 'host'}]
      }));
      this.mock.withArgs('C3').returns(App.StackServiceComponent.createRecord({ componentName: 'C3' }));
      expect(controller.checkComponentDependencies('C1', opt)).to.eql(['C3']);
    });
  });

  describe('#onLoadHiveConfigs', function() {

    beforeEach(function() {
      sinon.stub(controller, 'setConfigsChanges', Em.K);
      sinon.stub(controller, 'saveLoadedConfigs', Em.K);
      controller.set('configs', {});
    });

    afterEach(function() {
      controller.setConfigsChanges.restore();
      controller.saveLoadedConfigs.restore();
    });

    var makeHostComponentModel = function(componentName, hostNames) {
      if (Em.isArray(componentName)) {
        return componentName.map(function(cName, index) {
          return makeHostComponentModel(cName, hostNames[index]);
        }).reduce(function(p,c) { return p.concat(c); }, []);
      }
      return hostNames.map(function(hostName) {
        return {
          componentName: componentName,
          hostName: hostName
        };
      });
    };

    var makeFileNameProps = function(fileName, configs) {
      var ret = {
        type: fileName,
        properties: {}
      };
      var propRet = {};
      configs.forEach(function(property) {
        propRet[property[0]] = property[1];
      });
      ret.properties = propRet;
      return ret;
    };

    var makeEmptyPropAttrs = function() {
      var fileNames = Array.prototype.slice.call(arguments);
      var ret = {};
      fileNames.forEach(function(fileName) {
        ret[fileName] = {};
      });
      return ret;
    };

    var inlineComponentHostInfo = function(hostComponentModel) {
      return hostComponentModel.mapProperty('componentName').uniq()
        .map(function(componentName) {
          return componentName + ":" + hostComponentModel.filterProperty('componentName', componentName).mapProperty('hostName').join();
        }).join(',');
    };

    var tests = [
      {
        hostComponentModel: makeHostComponentModel(['HIVE_SERVER', 'HIVE_METASTORE'], [['host1', 'host2'], ['host1']]),
        configs: {
          items: [
            makeFileNameProps('hive-site', [
              ['hive.metastore.uris', 'thrift://host1:9090']
            ]),
            makeFileNameProps('hive-env', [
              ['hive_user', 'hive_user_val']
            ]),
            makeFileNameProps('webhcat-site', [
              ['templeton.hive.properties', 'hive.metastore.local=false,hive.metastore.uris=thrift://host1:9083,hive.metastore.sasl.enabled=false']
            ]),
            makeFileNameProps('core-site', [
              ['hadoop.proxyuser.hive_user_val.hosts', 'host1']
            ])
          ]
        },
        m: 'Components: {0}, appropriate configs should be changed, thrift port 9090, Controller stubs: {1}',
        e: {
          configs: [
            {
              "properties": {
                "hive-site": makeFileNameProps('hive-site', [
                  ['hive.metastore.uris', 'thrift://host1:9090']
                ]).properties,
                "webhcat-site": makeFileNameProps('webhcat-site', [
                  ['templeton.hive.properties', 'hive.metastore.local=false,hive.metastore.uris=thrift://host1:9090,hive.metastore.sasl.enabled=false']
                ]).properties,
                "hive-env": makeFileNameProps('hive-env', [
                  ['hive_user', 'hive_user_val']
                ]).properties
              },
              "properties_attributes": makeEmptyPropAttrs("hive-site", "webhcat-site", "hive-env")
            },
            {
              "properties": {
                "core-site": makeFileNameProps('core-site', [
                  ['hadoop.proxyuser.hive_user_val.hosts', 'host1,host2']
                ]).properties
              },
              "properties_attributes": makeEmptyPropAttrs("core-site")
            },
          ]
        }
      },
      {
        hostComponentModel: makeHostComponentModel(['HIVE_SERVER', 'HIVE_METASTORE', 'WEBHCAT_SERVER'], [['host1', 'host2'], ['host1'], ['host2']]),
        ctrlStubs: {
          webhcatServerHost: 'host3'
        },
        configs: {
          items: [
            makeFileNameProps('hive-site', [
              ['hive.metastore.uris', 'thrift://host1']
            ]),
            makeFileNameProps('hive-env', [
              ['hive_user', 'hive_user_val']
            ]),
            makeFileNameProps('webhcat-site', [
              ['templeton.hive.properties', 'hive.metastore.local=false,hive.metastore.uris=thrift://host1:9083,hive.metastore.sasl.enabled=false']
            ]),
            makeFileNameProps('core-site', [
              ['hadoop.proxyuser.hive_user_val.hosts', 'host1']
            ])
          ]
        },
        m: 'Components: {0}, appropriate configs should be changed, thrift port should be default 9083, Controller Stubs: {1}',
        e: {
          configs: [
            {
              "properties": {
                "hive-site": makeFileNameProps('hive-site', [
                  ['hive.metastore.uris', 'thrift://host1:9083']
                ]).properties,
                "webhcat-site": makeFileNameProps('webhcat-site', [
                  ['templeton.hive.properties', 'hive.metastore.local=false,hive.metastore.uris=thrift://host1:9083,hive.metastore.sasl.enabled=false']
                ]).properties,
                "hive-env": makeFileNameProps('hive-env', [
                  ['hive_user', 'hive_user_val']
                ]).properties
              },
              "properties_attributes": makeEmptyPropAttrs("hive-site", "webhcat-site", "hive-env")
            },
            {
              "properties": {
                "core-site": makeFileNameProps('core-site', [
                  ['hadoop.proxyuser.hive_user_val.hosts', 'host1,host2,host3']
                ]).properties
              },
              "properties_attributes": makeEmptyPropAttrs("core-site")
            },
          ]
        }
      },
      {
        hostComponentModel: makeHostComponentModel(['HIVE_SERVER', 'HIVE_METASTORE', 'WEBHCAT_SERVER'], [['host1'], ['host1'], ['host1']]),
        ctrlStubs: {
          webhcatServerHost: 'host3',
          hiveMetastoreHost: 'host2'
        },
        configs: {
          items: [
            makeFileNameProps('hive-site', [
              ['hive.metastore.uris', 'thrift://host1:1111']
            ]),
            makeFileNameProps('hive-env', [
              ['hive_user', 'hive_user_val']
            ]),
            makeFileNameProps('webhcat-site', [
              ['templeton.hive.properties', 'hive.metastore.local=false,hive.metastore.uris=thrift://host1:9083,hive.metastore.sasl.enabled=false']
            ]),
            makeFileNameProps('core-site', [
              ['hadoop.proxyuser.hive_user_val.hosts', 'host1']
            ])
          ]
        },
        m: 'Components: {0}, appropriate configs should be changed, thrift port should be 1111, Controller Stubs: {1}',
        e: {
          configs: [
            {
              "properties": {
                "hive-site": makeFileNameProps('hive-site', [
                  ['hive.metastore.uris', 'thrift://host1:1111,thrift://host2:1111']
                ]).properties,
                "webhcat-site": makeFileNameProps('webhcat-site', [
                  ['templeton.hive.properties', 'hive.metastore.local=false,hive.metastore.uris=thrift://host1:1111\\,thrift://host2:1111,hive.metastore.sasl.enabled=false']
                ]).properties,
                "hive-env": makeFileNameProps('hive-env', [
                  ['hive_user', 'hive_user_val']
                ]).properties
              },
              "properties_attributes": makeEmptyPropAttrs("hive-site", "webhcat-site", "hive-env")
            },
            {
              "properties": {
                "core-site": makeFileNameProps('core-site', [
                  ['hadoop.proxyuser.hive_user_val.hosts', 'host1,host2,host3']
                ]).properties
              },
              "properties_attributes": makeEmptyPropAttrs("core-site")
            },
          ]
        }
      },
      {
        hostComponentModel: makeHostComponentModel(['HIVE_SERVER', 'HIVE_METASTORE', 'WEBHCAT_SERVER'], [['host1', 'host2'], ['host1','host2'], ['host1', 'host3']]),
        ctrlStubs: {
          fromDeleteHost: true,
          'content.hostName': 'host2',
          webhcatServerHost: '',
          hiveMetastoreHost: ''
        },
        webHCat: true,
        configs: {
          items: [
            makeFileNameProps('hive-site', [
              ['hive.metastore.uris', 'thrift://host1:1111']
            ]),
            makeFileNameProps('hive-env', [
              ['webhcat_user', 'webhcat_user_val']
            ]),
            makeFileNameProps('webhcat-site', [
              ['templeton.hive.properties', 'hive.metastore.local=false,hive.metastore.uris=thrift://host1:9083,hive.metastore.sasl.enabled=false']
            ]),
            makeFileNameProps('core-site', [
              ['hadoop.proxyuser.webhcat_user_val.hosts', 'host1']
            ])
          ]
        },
        m: 'Change WebHCat proxyuser',
        e: {
          configs: [
            {
              "properties": {
                "hive-site": makeFileNameProps('hive-site', [
                  ['hive.metastore.uris', 'thrift://host1:1111']
                ]).properties,
                "webhcat-site": makeFileNameProps('webhcat-site', [
                  ['templeton.hive.properties', 'hive.metastore.local=false,hive.metastore.uris=thrift://host1:9083,hive.metastore.sasl.enabled=false']
                ]).properties,
                "hive-env": makeFileNameProps('hive-env', [
                  ['webhcat_user', 'webhcat_user_val']
                ]).properties
              },
              "properties_attributes": makeEmptyPropAttrs("hive-site", "webhcat-site", "hive-env")
            },
            {
              "properties": {
                "core-site": makeFileNameProps('core-site', [
                  ['hadoop.proxyuser.webhcat_user_val.hosts', 'host1,host3']
                ]).properties
              },
              "properties_attributes": makeEmptyPropAttrs("core-site")
            },
          ]
        }
      },
      {
        hostComponentModel: makeHostComponentModel(['HIVE_SERVER', 'HIVE_METASTORE', 'WEBHCAT_SERVER'], [['host1', 'host2'], ['host1','host2'], ['host1', 'host3']]),
        ctrlStubs: {
          fromDeleteHost: true,
          'content.hostName': 'host2',
          webhcatServerHost: '',
          hiveMetastoreHost: ''
        },
        configs: {
          items: [
            makeFileNameProps('hive-site', [
              ['hive.metastore.uris', 'thrift://host1:1111']
            ]),
            makeFileNameProps('hive-env', [
              ['hive_user', 'hive_user_val']
            ]),
            makeFileNameProps('webhcat-site', [
              ['templeton.hive.properties', 'hive.metastore.local=false,hive.metastore.uris=thrift://host1:9083,hive.metastore.sasl.enabled=false']
            ]),
            makeFileNameProps('core-site', [
              ['hadoop.proxyuser.hive_user_val.hosts', 'host1']
            ])
          ]
        },
        m: 'Components: {0}, appropriate configs should be changed, thrift port should be default 9083, Controller Stubs: {1}',
        e: {
          configs: [
            {
              "properties": {
                "hive-site": makeFileNameProps('hive-site', [
                  ['hive.metastore.uris', 'thrift://host1:1111']
                ]).properties,
                "webhcat-site": makeFileNameProps('webhcat-site', [
                  ['templeton.hive.properties', 'hive.metastore.local=false,hive.metastore.uris=thrift://host1:1111,hive.metastore.sasl.enabled=false']
                ]).properties,
                "hive-env": makeFileNameProps('hive-env', [
                  ['hive_user', 'hive_user_val']
                ]).properties
              },
              "properties_attributes": makeEmptyPropAttrs("hive-site", "webhcat-site", "hive-env")
            },
            {
              "properties": {
                "core-site": makeFileNameProps('core-site', [
                  ['hadoop.proxyuser.hive_user_val.hosts', 'host1,host3']
                ]).properties
              },
              "properties_attributes": makeEmptyPropAttrs("core-site")
            }
          ]
        }
      }
    ];

    tests.forEach(function(test) {
      describe(test.m.format(inlineComponentHostInfo(test.hostComponentModel), test.ctrlStubs ? JSON.stringify(test.ctrlStubs) : 'None'), function() {

        beforeEach(function () {
          if (test.appGetterStubs) {
            Em.keys(test.appGetterStubs).forEach(function(key) {
              sinon.stub(App, 'get').withArgs(key).returns(test.appGetterStubs[key]);
            });
          }
          if (test.ctrlStubs) {
            var stub = sinon.stub(controller, 'get');
            Em.keys(test.ctrlStubs).forEach(function(key) {
              stub.withArgs(key).returns(test.ctrlStubs[key]);
            });
          }
          sinon.stub(App.HostComponent, 'find').returns(test.hostComponentModel);
        });

        afterEach(function () {
          if (test.ctrlStubs) {
            controller.get.restore();
          }
          if (test.appGetterStubs) {
            App.get.restore();
          }
          App.HostComponent.find.restore();
        });

        it('saveConfigsBatch is called with correct configs', function () {
          controller.onLoadHiveConfigs(test.configs, null, {webHCat: test.webHCat});
          var configs = controller.setConfigsChanges.args[0];
          var properties = configs[0];
          expect(properties).to.be.eql(test.e.configs);
        });

      });
    });
  });

  describe('#setConfigsChangesForDisplay', function () {

    var propertiesToChange = [
        {
          propertyName: 'n0',
          propertyFileName: 'f0'
        },
        {
          propertyName: 'n1',
          propertyFileName: 'f1'
        },
        {
          propertyName: 'n2',
          propertyFileName: 'f2'
        },
        {
          propertyName: 'n3',
          propertyFileName: 'f3'
        }
      ],
      result = {
        recommendedPropertiesToChange: [
          {
            propertyName: 'n0',
            propertyFileName: 'f0',
            saveRecommended: true
          },
          {
            propertyName: 'n3',
            propertyFileName: 'f3',
            saveRecommended: true
          }
        ],
        requiredPropertiesToChange: [
          {
            propertyName: 'n1',
            propertyFileName: 'f1'
          },
          {
            propertyName: 'n2',
            propertyFileName: 'f2'
          }
        ]
      };

    beforeEach(function () {
      controller.setProperties({
        allPropertiesToChange: propertiesToChange,
        recommendedPropertiesToChange: [],
        requiredPropertiesToChange: []
      });
      sinon.stub(App.configsCollection, 'getConfigByName', function (propertyName) {
        var map = {
          n0: {
            isEditable: true,
            isReconfigurable: true
          },
          n1: {
            isEditable: true,
            isReconfigurable: false
          },
          n2: {
            isEditable: false,
            isReconfigurable: false
          }
        };
        return map[propertyName];
      });
      sinon.stub(App, 'get').withArgs('router.clusterController.isConfigsPropertiesLoaded').returns(true);
      controller.set('isConfigsLoadingInProgress', true);
      controller.setConfigsChangesForDisplay();
    });

    afterEach(function () {
      App.configsCollection.getConfigByName.restore();
      App.get.restore();
    });

    it('editable changes', function () {
      expect(controller.get('recommendedPropertiesToChange').toArray()).to.eql(result.recommendedPropertiesToChange);
    });

    it('non-editable changes', function () {
      expect(controller.get('requiredPropertiesToChange').toArray()).to.eql(result.requiredPropertiesToChange);
    });

    it('isConfigsLoadingInProgress', function () {
      expect(controller.get('isConfigsLoadingInProgress')).to.be.false;
    });

  });

  describe('#clearConfigsChanges', function () {

    beforeEach(function () {
      sinon.stub(controller, 'abortRequests', Em.K);
      controller.setProperties({
        allPropertiesToChange: [{}],
        recommendedPropertiesToChange: [{}],
        requiredPropertiesToChange: [{}],
        groupedPropertiesToChange: [{}],
        isReconfigureRequired: true,
        configs: {}
      });
    });

    afterEach(function () {
      controller.abortRequests.restore();
    });

    describe('default case', function () {

      beforeEach(function () {
        controller.clearConfigsChanges();
      });

      it('allPropertiesToChange', function () {
        expect(controller.get('allPropertiesToChange')).to.have.length(0);
      });

      it('recommendedPropertiesToChange', function () {
        expect(controller.get('recommendedPropertiesToChange')).to.have.length(0);
      });

      it('groupedPropertiesToChange', function () {
        expect(controller.get('groupedPropertiesToChange')).to.have.length(0);
      });

      it('isReconfigureRequired', function () {
        expect(controller.get('isReconfigureRequired')).to.be.false;
      });

      it('configs', function () {
        expect(controller.get('configs')).to.be.null;
      });

    });

    describe('no loaded configs cleanup', function () {

      beforeEach(function () {
        controller.clearConfigsChanges(true);
      });

      it('configs shouldn\'t be cleared', function () {
        expect(controller.get('configs')).to.not.be.null;
      });

    });

  });

  describe('#saveLoadedConfigs', function () {

    var data = {
      items: [
        {
          type: 't0',
          properties: {
            p0: 'v0',
            p1: 'v1'
          },
          properties_attributes: {}
        },
        {
          type: 't1',
          properties: {
            p2: 'v2',
            p3: 'v3'
          },
          properties_attributes: {}
        }
      ]
    };

    it('should store data in configs object', function () {
      controller.set('configs', null);
      controller.saveLoadedConfigs(data);
      expect(controller.get('configs')).to.eql(data);
    });

  });

  describe('#loadComponentRelatedConfigs', function() {
    beforeEach(function() {
      sinon.stub(controller, 'clearConfigsChanges');
      sinon.stub(controller, 'isServiceMetricsLoaded', Em.clb);
      sinon.stub(controller, 'loadConfigs');

      controller.loadComponentRelatedConfigs('clb1', 'clb2');
    });
    afterEach(function() {
      controller.clearConfigsChanges.restore();
      controller.isServiceMetricsLoaded.restore();
      controller.loadConfigs.restore();
    });

    it('clearConfigsChanges should be called', function() {
      expect(controller.clearConfigsChanges.calledOnce).to.be.true;
    });
    it('isServiceMetricsLoaded should be called', function() {
      expect(controller.isServiceMetricsLoaded.calledOnce).to.be.true;
    });
    it('loadConfigs should be called', function() {
      expect(controller.loadConfigs.calledWith('clb1', 'clb2')).to.be.true;
    });
    it('isReconfigureRequired should be true', function() {
      expect(controller.get('isReconfigureRequired')).to.be.true;
    });
    it('isConfigsLoadingInProgress should be true', function() {
      expect(controller.get('isConfigsLoadingInProgress')).to.be.true;
    });
  });

  describe('#applyConfigsCustomization', function() {
    var groupedPropertiesToChange = [
      {
        properties: {
          site1: {
            prop1: 'val1',
            prop2: 'val2'
          }
        }
      }
    ];

    it('should apply recommended value to properties', function() {
      controller.set('groupedPropertiesToChange', groupedPropertiesToChange);
      controller.set('recommendedPropertiesToChange', [
        {
          saveRecommended: true,
          recommendedValue: 'rec1',
          propertyFileName: 'site1',
          propertyName: 'prop1'
        },
        {
          saveRecommended: false,
          initialValue: 'init1',
          propertyFileName: 'site1',
          propertyName: 'prop2'
        }
      ]);

      controller.applyConfigsCustomization();

      expect(groupedPropertiesToChange[0].properties['site1']['prop1']).to.be.equal('rec1');
      expect(groupedPropertiesToChange[0].properties['site1']['prop2']).to.be.equal('init1');
    });
  });

  describe('#getHdfsUser', function() {
    var usersController = Em.Object.create({
      loadUsers: sinon.spy(),
      dataIsLoaded: false,
      users: [Em.Object.create({
        name: 'hdfs_user',
        value: 'val'
      })]
    });

    beforeEach(function() {
      sinon.stub(App.MainAdminServiceAccountsController, 'create').returns(usersController);
    });
    afterEach(function() {
      App.MainAdminServiceAccountsController.create.restore();
    });

    it('should load and set hdfs_user value', function() {
      controller.getHdfsUser();

      expect(usersController.loadUsers.calledOnce).to.be.true;

      usersController.set('dataIsLoaded', true);

      expect(controller.get('hdfsUser')).to.be.equal('val')
    });
  });

  describe('#getUrlParamsForConfigsRequest', function () {
    var cases = [
      {
        data: {
          Clusters: {
            desired_configs: {
              't0-site': {
                tag: 'v0'
              },
              't0-env': {
                tag: 'v1'
              },
              't0-log4j': {
                tag: 'v2'
              },
              't2-site': {
                tag: 'v3'
              }
            }
          }
        },
        types: ['t0-site', 't0-env', 't0-log4j', 't1-site'],
        result: '(type=t0-site&tag=v0)|(type=t0-env&tag=v1)|(type=t0-log4j&tag=v2)',
        title: 'several types available'
      },
      {
        data: {
          Clusters: {
            desired_configs: {
              't1-site': {
                tag: 'v4'
              },
              't2-env': {
                tag: 'v5'
              },
              't2-log4j': {
                tag: 'v6'
              }
            }
          }
        },
        types: ['t1-site', 't1-env', 't1-log4j'],
        result: '(type=t1-site&tag=v4)',
        title: 'single type available'
      },
      {
        data: {
          Clusters: {
            desired_configs: {
              't3-site': {
                tag: 'v7'
              },
              't3-env': {
                tag: 'v8'
              },
              't3-log4j': {
                tag: 'v9'
              }
            }
          }
        },
        types: ['t2-site', 't2-env', 't2-log4j'],
        result: '',
        title: 'no types available'
      }
    ];

    cases.forEach(function (test) {
      describe(test.title, function () {
        it('should return ' + test.result, function () {
          expect(controller.getUrlParamsForConfigsRequest(test.data, test.types)).to.equal(test.result);
        });
      });
    });
  });
});
