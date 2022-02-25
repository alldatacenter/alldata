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

require('mixins/main/service/configs/component_actions_by_configs');
var testHelpers = require('test/helpers');
var stringUtils = require('utils/string_utils');


var mixin;

describe('App.ComponentActionsByConfigs', function () {

  beforeEach(function() {
    mixin = Em.Object.create(App.ComponentActionsByConfigs, {
      stepConfigs: [],
      content: Em.Object.create()
    });
  });

  describe("#doConfigActions()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'doComponentDeleteActions');
      sinon.stub(mixin, 'doComponentAddActions');
      mixin.set('allConfigs', [Em.Object.create({
        filename: ''
      })]);
      mixin.set('stepConfigs', [Em.Object.create({
        serviceName: 'S1',
        configs: [{
          configActionComponent: {}
        }]
      })]);
      mixin.set('content.serviceName', 'S1');
      mixin.doConfigActions();
    });

    afterEach(function() {
      mixin.doComponentDeleteActions.restore();
      mixin.doComponentAddActions.restore();
    });

    it("doComponentDeleteActions should be called", function() {
      expect(mixin.doComponentDeleteActions.calledWith([{
        configActionComponent: {}
      }])).to.be.true;
    });

    it("doComponentAddActions should be called", function() {
      expect(mixin.doComponentAddActions.calledWith([{
        configActionComponent: {}
      }])).to.be.true;
    });
  });

  describe("#isComponentActionsPresent()", function () {

    beforeEach(function() {
      this.mockDelete = sinon.stub(mixin, 'getComponentsToDelete').returns(1);
      this.mockAdd = sinon.stub(mixin, 'getComponentsToAdd').returns(1);
      mixin.set('stepConfigs', [Em.Object.create({
        serviceName: 'S1',
        configs: [{
          configActionComponent: {}
        }]
      })]);
      mixin.set('content.serviceName', 'S1');
    });

    afterEach(function() {
      this.mockAdd.restore();
      this.mockDelete.restore();
    });

    it("no delete or add components", function() {
      this.mockAdd.returns([]);
      this.mockDelete.returns([]);
      expect(mixin.isComponentActionsPresent()).to.be.false;
    });

    it("has delete and no add components", function() {
      this.mockAdd.returns([]);
      this.mockDelete.returns([{}]);
      expect(mixin.isComponentActionsPresent()).to.be.true;
    });

    it("no delete and has add components", function() {
      this.mockAdd.returns([{}]);
      this.mockDelete.returns([]);
      expect(mixin.isComponentActionsPresent()).to.be.true;
    });
  });

  describe("#getComponentsToDelete()", function () {

    beforeEach(function() {
      sinon.stub(App.HostComponent, 'find').returns([
        Em.Object.create({
          componentName: 'C1',
          hostName: 'host1'
        })
      ]);
    });

    afterEach(function() {
      App.HostComponent.find.restore();
    });

    it("should return array of components to delete", function() {
      var configActionComponents = [
        {
          configActionComponent: {
            action: 'delete',
            componentName: 'C1',
            hostNames: ['host1'],
            isClient: false
          }
        }
      ];
      expect(mixin.getComponentsToDelete(configActionComponents)).to.be.eql([{
        componentName: 'C1',
        hostName: 'host1',
        isClient: false
      }]);
    });
  });

  describe("#getComponentsToAdd()", function () {

    beforeEach(function() {
      sinon.stub(App.StackServiceComponent, 'find').returns(Em.Object.create({
        componentName: 'C1',
        serviceName: 'S1'
      }));
      sinon.stub(App.Service, 'find').returns(Em.Object.create({
        serviceName: 'S1',
        hostComponents: [
          Em.Object.create({
            componentName: 'C1',
            hostName: 'host2'
          })
        ]
      }));
    });

    afterEach(function() {
      App.StackServiceComponent.find.restore();
      App.Service.find.restore();
    });

    it("should return array of components to add", function() {
      var configActionComponents = [
        {
          configActionComponent: {
            action: 'add',
            componentName: 'C1',
            hostNames: ['host1'],
            isClient: false
          }
        }
      ];
      expect(mixin.getComponentsToAdd(configActionComponents)).to.be.eql([{
        componentName: 'C1',
        hostName: 'host1',
        isClient: false
      }]);
    });
  });

  describe("#doComponentDeleteActions()", function () {

    beforeEach(function() {
      this.mockDelete = sinon.stub(mixin, 'getComponentsToDelete');
      sinon.stub(App.StackServiceComponent, 'find').returns([
        Em.Object.create({
          componentName: 'C1',
          displayName: 'c1'
        })
      ]);
      sinon.stub(mixin, 'setRefreshYarnQueueRequest');
      sinon.stub(mixin, 'getInstallHostComponentsRequest').returns({});
      sinon.stub(mixin, 'getDeleteHostComponentRequest').returns({});
      sinon.stub(mixin, 'setOrderIdForBatches');
    });

    afterEach(function() {
      this.mockDelete.restore();
      App.StackServiceComponent.find.restore();
      mixin.setRefreshYarnQueueRequest.restore();
      mixin.getDeleteHostComponentRequest.restore();
      mixin.setOrderIdForBatches.restore();
      mixin.getInstallHostComponentsRequest.restore();
    });

    it("App.ajax.send should not be called", function() {
      this.mockDelete.returns([]);
      mixin.doComponentDeleteActions();
      expect(testHelpers.findAjaxRequest('name', 'common.batch.request_schedules')).to.not.exists;
    });

    it("App.ajax.send should be called", function() {
      this.mockDelete.returns([{
        componentName: 'C1',
        hostName: 'host1'
      }]);
      mixin.doComponentDeleteActions();

      var args = testHelpers.findAjaxRequest('name', 'common.batch.request_schedules');
      expect(args[0]).to.be.eql({
        name: 'common.batch.request_schedules',
        sender: {checkIfComponentWasDeleted: mixin.checkIfComponentWasDeleted},
        success : "checkIfComponentWasDeleted",
        data: {
          intervalTimeSeconds: 60,
          tolerateSize: 0,
          batches: [{}, {}],
          displayName: undefined,
          hostName: 'host1'
        }
      });
    });
  });

  describe("#doComponentAddActions()", function () {

    beforeEach(function() {
      this.mockAdd = sinon.stub(mixin, 'getComponentsToAdd');
      sinon.stub(mixin, 'getDependentComponents').returns([]);
      sinon.stub(App.StackServiceComponent, 'find').returns([
        Em.Object.create({
          componentName: 'C1',
          displayName: 'c1'
        })
      ]);
      sinon.stub(mixin, 'setCreateComponentRequest');
      sinon.stub(mixin, 'getCreateHostComponentsRequest').returns({});
      sinon.stub(mixin, 'getInstallHostComponentsRequest').returns({});
      sinon.stub(mixin, 'setRefreshYarnQueueRequest');
      sinon.stub(mixin, 'getStartHostComponentsRequest').returns({});
      sinon.stub(mixin, 'setOrderIdForBatches');
      sinon.stub(stringUtils, 'getFormattedStringFromArray').returns('c1');
    });

    afterEach(function() {
      this.mockAdd.restore();
      mixin.getDependentComponents.restore();
      App.StackServiceComponent.find.restore();
      mixin.setCreateComponentRequest.restore();
      mixin.getCreateHostComponentsRequest.restore();
      mixin.getInstallHostComponentsRequest.restore();
      mixin.setRefreshYarnQueueRequest.restore();
      mixin.getStartHostComponentsRequest.restore();
      mixin.setOrderIdForBatches.restore();
      stringUtils.getFormattedStringFromArray.restore();
    });

    it("App.ajax.send should not be called", function() {
      this.mockAdd.returns([]);
      mixin.doComponentAddActions();
      expect(testHelpers.findAjaxRequest('name', 'common.batch.request_schedules')).to.not.exists;
    });

    it("App.ajax.send should be called", function() {
      this.mockAdd.returns([{
        componentName: 'C1',
        hostName: 'host1',
        isClient: false
      }]);
      mixin.doComponentAddActions();
      var args = testHelpers.findAjaxRequest('name', 'common.batch.request_schedules');
      expect(args[0]).to.be.eql({
        name: 'common.batch.request_schedules',
        sender: mixin,
        data: {
          intervalTimeSeconds: 1,
          tolerateSize: 0,
          batches: [{}, {}, {}]
        }
      });
    });
  });

  describe("#getDependentComponents()", function () {

    beforeEach(function() {
      var mock = sinon.stub(App.StackServiceComponent, 'find');
      mock.returns([
        App.StackServiceComponent.createRecord({componentName: 'C1'}),
        App.StackServiceComponent.createRecord({componentName: 'C2'})
      ]);
      mock.withArgs('C1').returns(
        App.StackServiceComponent.createRecord({
          componentName: 'C1',
          dependencies: [{ scope: 'host', componentName: 'C2' }],
          isClient: false
        })
      );
      mock.withArgs('C2').returns(App.StackServiceComponent.createRecord({componentName: 'C2'}));
      sinon.stub(App.HostComponent, 'find').returns([]);
    });

    afterEach(function() {
      App.StackServiceComponent.find.restore();
      App.HostComponent.find.restore();
    });

    it("should return dependent components", function() {
      var componentsToAdd = [{
        componentName: 'C1',
        hostName: 'host1'
      }];
      expect(mixin.getDependentComponents(componentsToAdd)).to.be.eql([
        {
          componentName: 'C2',
          hostName: 'host1',
          isClient: false
        }
      ]);
    });
  });

  describe("#setOrderIdForBatches()", function () {

    it("should set order_id", function() {
      var batches = [{}, {}, {}];
      mixin.setOrderIdForBatches(batches);
      expect(batches).to.be.eql([
        {order_id: 1},
        {order_id: 2},
        {order_id: 3}
      ]);
    });
  });

  describe("#getCreateHostComponentsRequest()", function () {

    it("App.ajax.send should be called", function() {
      expect(mixin.getCreateHostComponentsRequest('host1', ['C1'])).to.be.eql({
        "type": 'POST',
        "uri": "/clusters/mycluster/hosts",
        "RequestBodyInfo": {
          "RequestInfo": {
            "query": "Hosts/host_name.in(host1)"
          },
          "Body": {
            "host_components": [{
              "HostRoles": {
                "component_name": 'C1'
              }
            }]
          }
        }
      });
    });
  });

  describe("#getInstallHostComponentsRequest()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'getUpdateHostComponentsRequest').returns({});
    });

    afterEach(function() {
      mixin.getUpdateHostComponentsRequest.restore();
    });

    it("should return request object", function() {
      expect(mixin.getInstallHostComponentsRequest('host1', [{componentName: 'C1'}])).to.be.eql({});
      expect(mixin.getUpdateHostComponentsRequest.calledWith(
        'host1',
        [{componentName: 'C1'}],
        App.HostComponentStatus.stopped,
        Em.I18n.t('requestInfo.installComponents'))).to.be.true;
    });
  });

  describe("#getStartHostComponentsRequest()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'getUpdateHostComponentsRequest').returns({});
    });

    afterEach(function() {
      mixin.getUpdateHostComponentsRequest.restore();
    });

    it("should return request object", function() {
      expect(mixin.getStartHostComponentsRequest('host1', [{componentName: 'C1'}])).to.be.eql({});
      expect(mixin.getUpdateHostComponentsRequest.calledWith(
        'host1',
        [{componentName: 'C1'}],
        App.HostComponentStatus.started,
        Em.I18n.t('requestInfo.startHostComponents'))).to.be.true;
    });
  });

  describe("#getUpdateHostComponentsRequest()", function () {

    it("should return request object", function() {
      expect(mixin.getUpdateHostComponentsRequest('host1', ['C1', 'C2'], 'INSTALLED', 'context')).to.be.eql({
        "type": 'PUT',
        "uri": "/clusters/mycluster/hosts/host1/host_components",
        "RequestBodyInfo": {
          "RequestInfo": {
            "context": 'context',
            "operation_level": {
              "level": "HOST",
              "cluster_name": 'mycluster',
              "host_names": 'host1'
            },
            "query": "HostRoles/component_name.in(C1,C2)"
          },
          "Body": {
            "HostRoles": {
              "state": 'INSTALLED'
            }
          }
        }
      });
    });
  });

  describe("#getDeleteHostComponentRequest()", function () {

    it("should return request object", function() {
      expect(mixin.getDeleteHostComponentRequest('host1', 'C1')).to.be.eql({
        "type": 'DELETE',
        "uri": "/clusters/mycluster/hosts/host1/host_components/C1"
      });
    });
  });

  describe("#setCreateComponentRequest()", function () {

    beforeEach(function() {
      sinon.stub(App.StackServiceComponent, 'find').returns([Em.Object.create({
        componentName: 'C1',
        serviceName: 'S1'
      })]);
      sinon.stub(App.Service, 'find').returns([Em.Object.create({
        serviceName: 'S1',
        serviceComponents: []
      })]);
    });

    afterEach(function() {
      App.StackServiceComponent.find.restore();
      App.Service.find.restore();
    });

    it("should add batch", function() {
      var batches = [];
      mixin.setCreateComponentRequest(batches, ["C1"]);
      expect(batches).to.be.eql([
        {
          "type": 'POST',
          "uri": "/clusters/mycluster/services/S1/components/C1"
        }
      ]);

    });
  });

  describe("#setRefreshYarnQueueRequest()", function () {

    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns(Em.Object.create({
        hostComponents: [Em.Object.create({
          componentName: 'RESOURCEMANAGER',
          hostName: 'host1'
        })]
      }));
    });

    afterEach(function() {
      App.Service.find.restore();
    });

    it("should not add a batch", function() {
      var batches = [];
      mixin.set('allConfigs', []);
      mixin.setRefreshYarnQueueRequest(batches);
      expect(batches).to.be.empty;
    });

    it("should add a batch", function() {
      var batches = [];
      mixin.set('allConfigs', [Em.Object.create({
        filename: 'capacity-scheduler.xml',
        value: 'val1',
        initialValue: 'val2'
      })]);
      mixin.setRefreshYarnQueueRequest(batches);
      expect(batches).to.be.eql([{
        "type": 'POST',
        "uri": "/clusters/mycluster/requests",
        "RequestBodyInfo": {
          "RequestInfo": {
            "context": Em.I18n.t('services.service.actions.run.yarnRefreshQueues.context'),
            "command": "REFRESHQUEUES",
            "parameters/forceRefreshConfigTags": "capacity-scheduler"
          },
          "Requests/resource_filters": [
            {
              service_name: "YARN",
              component_name: "RESOURCEMANAGER",
              hosts: "host1"
            }
          ]
        }
      }]);
    });
  });

  describe('#showPopup', function () {

    var testCases = [
      {
        configActions: [],
        popupPrimaryButtonCallbackCallCount: 0,
        showHsiRestartPopupCallCount: 0,
        showConfirmationPopupCallCount: 0,
        title: 'no config actions'
      },
      {
        configActions: [
          {
            actionType: 'none'
          },
          {
            actionType: null
          },
          {}
        ],
        popupPrimaryButtonCallbackCallCount: 0,
        showHsiRestartPopupCallCount: 0,
        showConfirmationPopupCallCount: 0,
        title: 'no popup config actions'
      },
      {
        configActions: [
          Em.Object.create({
            actionType: 'showPopup',
            fileName: 'f0'
          })
        ],
        mixinProperties: {
          allConfigs: [
            Em.Object.create({
              filename: 'f1',
              value: 0,
              initialValue: 1
            })
          ]
        },
        popupPrimaryButtonCallbackCallCount: 0,
        showHsiRestartPopupCallCount: 0,
        showConfirmationPopupCallCount: 0,
        title: 'no associated configs'
      },
      {
        configActions: [
          Em.Object.create({
            actionType: 'showPopup',
            fileName: 'f2'
          })
        ],
        mixinProperties: {
          allConfigs: [
            Em.Object.create({
              filename: 'f2',
              value: 0,
              initialValue: 0
            })
          ]
        },
        popupPrimaryButtonCallbackCallCount: 0,
        showHsiRestartPopupCallCount: 0,
        showConfirmationPopupCallCount: 0,
        title: 'no changes in associated configs'
      },
      {
        configActions: [
          Em.Object.create({
            actionType: 'showPopup',
            fileName: 'f3'
          })
        ],
        mixinProperties: {
          allConfigs: [
            Em.Object.create({
              filename: 'f3',
              value: 0,
              initialValue: 1
            })
          ]
        },
        popupPrimaryButtonCallbackCallCount: 0,
        showHsiRestartPopupCallCount: 0,
        showConfirmationPopupCallCount: 0,
        title: 'no capacity-scheduler actions defined'
      },
      {
        configActions: [
          Em.Object.create({
            actionType: 'showPopup',
            fileName: 'capacity-scheduler.xml'
          })
        ],
        mixinProperties: {
          allConfigs: [
            Em.Object.create({
              filename: 'capacity-scheduler.xml',
              value: 0,
              initialValue: 1
            })
          ],
          isYarnQueueRefreshed: true
        },
        popupPrimaryButtonCallbackCallCount: 0,
        showHsiRestartPopupCallCount: 0,
        showConfirmationPopupCallCount: 0,
        title: 'YARN queue refreshed'
      },
      {
        configActions: [
          Em.Object.create({
            actionType: 'showPopup',
            fileName: 'capacity-scheduler.xml'
          })
        ],
        hostComponents: [
          Em.Object.create({
            componentName: 'RESOURCEMANAGER'
          }),
          Em.Object.create({
            componentName: 'RESOURCEMANAGER',
            isRunning: false
          }),
          Em.Object.create({
            componentName: 'COMPONENT',
            isRunning: true
          })
        ],
        mixinProperties: {
          allConfigs: [
            Em.Object.create({
              filename: 'capacity-scheduler.xml',
              value: 0,
              initialValue: 1
            })
          ],
          isYarnQueueRefreshed: false
        },
        popupPrimaryButtonCallbackCallCount: 0,
        showHsiRestartPopupCallCount: 0,
        showConfirmationPopupCallCount: 0,
        title: 'no ResourceManagers running'
      },
      {
        configActions: [
          Em.Object.create({
            actionType: 'showPopup',
            fileName: 'capacity-scheduler.xml'
          })
        ],
        hostComponents: [
          Em.Object.create({
            componentName: 'RESOURCEMANAGER'
          }),
          Em.Object.create({
            componentName: 'RESOURCEMANAGER',
            isRunning: false
          }),
          Em.Object.create({
            componentName: 'RESOURCEMANAGER',
            isRunning: true
          }),
          Em.Object.create({
            componentName: 'HIVE_SERVER_INTERACTIVE'
          })
        ],
        mixinProperties: {
          'allConfigs': [
            Em.Object.create({
              filename: 'capacity-scheduler.xml',
              value: 0,
              initialValue: 1
            })
          ],
          'isYarnQueueRefreshed': false,
          'content.serviceName': 'HIVE'
        },
        popupPrimaryButtonCallbackCallCount: 1,
        showHsiRestartPopupCallCount: 1,
        showConfirmationPopupCallCount: 0,
        title: 'change from Hive page, Hive Server Interactive present'
      },
      {
        configActions: [
          Em.Object.create({
            actionType: 'showPopup',
            fileName: 'capacity-scheduler.xml'
          })
        ],
        hostComponents: [
          Em.Object.create({
            componentName: 'RESOURCEMANAGER'
          }),
          Em.Object.create({
            componentName: 'RESOURCEMANAGER',
            isRunning: false
          }),
          Em.Object.create({
            componentName: 'RESOURCEMANAGER',
            isRunning: true
          })
        ],
        mixinProperties: {
          'allConfigs': [
            Em.Object.create({
              filename: 'capacity-scheduler.xml',
              value: 0,
              initialValue: 1
            })
          ],
          'isYarnQueueRefreshed': false,
          'content.serviceName': 'HIVE'
        },
        popupPrimaryButtonCallbackCallCount: 1,
        showHsiRestartPopupCallCount: 0,
        showConfirmationPopupCallCount: 0,
        title: 'change from Hive page, no Hive Server Interactive'
      },
      {
        configActions: [
          Em.Object.create({
            actionType: 'showPopup',
            fileName: 'capacity-scheduler.xml',
            popupProperties: {
              primaryButton: {}
            }
          })
        ],
        hostComponents: [
          Em.Object.create({
            componentName: 'RESOURCEMANAGER'
          }),
          Em.Object.create({
            componentName: 'RESOURCEMANAGER',
            isRunning: false
          }),
          Em.Object.create({
            componentName: 'RESOURCEMANAGER',
            isRunning: true
          })
        ],
        mixinProperties: {
          'allConfigs': [
            Em.Object.create({
              filename: 'capacity-scheduler.xml',
              value: 0,
              initialValue: 1
            })
          ],
          'isYarnQueueRefreshed': false,
          'content.serviceName': 'YARN'
        },
        popupPrimaryButtonCallbackCallCount: 0,
        showHsiRestartPopupCallCount: 0,
        showConfirmationPopupCallCount: 1,
        title: 'change from YARN page'
      }
    ];

    testCases.forEach(function (test) {

      describe(test.title, function () {

        beforeEach(function () {
          sinon.stub(App.ConfigAction, 'find').returns(test.configActions);
          sinon.stub(App.HostComponent, 'find').returns(test.hostComponents || []);
          sinon.stub(mixin, 'popupPrimaryButtonCallback', Em.K);
          sinon.stub(mixin, 'showHsiRestartPopup', Em.K);
          sinon.stub(App, 'showConfirmationPopup', Em.K);
          mixin.setProperties(test.mixinProperties);
          mixin.showPopup();
        });

        afterEach(function () {
          App.ConfigAction.find.restore();
          App.HostComponent.find.restore();
          mixin.popupPrimaryButtonCallback.restore();
          mixin.showHsiRestartPopup.restore();
          App.showConfirmationPopup.restore();
        });

        it('popup callback', function () {
          expect(mixin.popupPrimaryButtonCallback.callCount).to.eql(test.popupPrimaryButtonCallbackCallCount);
        });

        it('HSI restart popup', function () {
          expect(mixin.showHsiRestartPopup.callCount).to.eql(test.showHsiRestartPopupCallCount);
        });

        it('confirmation popup', function () {
          expect(App.showConfirmationPopup.callCount).to.eql(test.showConfirmationPopupCallCount);
        });

      });

    });

  });

});