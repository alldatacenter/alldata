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
require('views/main/service/info/summary');
var batchUtils = require('utils/batch_scheduled_requests');

describe('App.MainServiceInfoSummaryView', function() {

  var view = App.MainServiceInfoSummaryView.create({
    monitorsLiveTextView: Em.View.create(),
    controller: Em.Object.create({
      content: Em.Object.create({
        id: 'HDFS',
        serviceName: 'HDFS',
        hostComponents: []
      })
    }),
    alertsController: Em.Object.create(),
    service: Em.Object.create()
  });

  App.TestAliases.testAsComputedAlias(view, 'servicesHaveClients', 'App.services.hasClient', 'boolean');

  App.TestAliases.testAsComputedAlias(view, 'serviceName', 'service.serviceName', 'string');

  describe('#servers', function () {
    it('services shouldn\'t have servers except FLUME and ZOOKEEPER', function () {
      expect(view.get('servers')).to.be.empty;
    });

    describe('if one server exists then first server should have isComma and isAnd property false', function () {

      beforeEach(function () {
        view.set('controller.content', Em.Object.create({
          id: 'ZOOKEEPER',
          serviceName: 'ZOOKEEPER',
          hostComponents: [
            Em.Object.create({
              displayName: '',
              isMaster: true
            })
          ]
        }));
      });

      it('isComma', function () {
        expect(view.get('servers').objectAt(0).isComma).to.equal(false);});

      it('isAnd', function () {
        expect(view.get('servers').objectAt(0).isAnd).to.equal(false);
      });
    });

    describe('if more than one servers exist then first server should have isComma - true and isAnd - false', function() {

      beforeEach(function () {
        view.set('controller.content', Em.Object.create({
          id: 'ZOOKEEPER',
          serviceName: 'ZOOKEEPER',
          hostComponents: [
            Em.Object.create({
              displayName: '',
              isMaster: true
            }),
            Em.Object.create({
              displayName: '',
              isMaster: true
            })
          ]
        }));
      });

      it('0 isComma', function () {
        expect(view.get('servers').objectAt(0).isComma).to.equal(true);
      });

      it('0 isAnd', function () {
        expect(view.get('servers').objectAt(0).isAnd).to.equal(false);
      });

      it('1 isComma', function () {
        expect(view.get('servers').objectAt(1).isComma).to.equal(false);
      });

      it('1 isAnd', function () {
        expect(view.get('servers').objectAt(1).isAnd).to.equal(false);
      });

    });

    describe('if more than two servers exist then second server should have isComma - false and isAnd - true', function () {

      beforeEach(function () {
        view.set('controller.content', Em.Object.create({
          id: 'ZOOKEEPER',
          serviceName: 'ZOOKEEPER',
          hostComponents: [
            Em.Object.create({
              displayName: '',
              isMaster: true
            }),
            Em.Object.create({
              displayName: '',
              isMaster: true
            }),
            Em.Object.create({
              displayName: '',
              isMaster: true
            })
          ]
        }));
      });

      it('0 isComma', function () {
        expect(view.get('servers').objectAt(0).isComma).to.equal(true);
      });

      it('0 isAnd', function () {
        expect(view.get('servers').objectAt(0).isAnd).to.equal(false);
      });

      it('1 isComma', function () {
        expect(view.get('servers').objectAt(1).isComma).to.equal(false);
      });

      it('1 isAnd', function () {
        expect(view.get('servers').objectAt(1).isAnd).to.equal(true);
      });

      it('2 isComma', function () {
        expect(view.get('servers').objectAt(2).isComma).to.equal(false);
      });

      it('2 isAnd', function () {
        expect(view.get('servers').objectAt(2).isAnd).to.equal(false);
      });

    });

  });

  describe('#hasAlertDefinitions', function () {

    beforeEach(function () {
      sinon.stub(App.AlertDefinition, 'find', function () {
        return [
          {
            serviceName: 'HDFS'
          },
          {
            serviceName: 'YARN'
          }
        ];
      });
    });

    afterEach(function () {
      App.AlertDefinition.find.restore();
    });

    it('should return true if at least one alert definition for this service exists', function () {
      view.set('controller.content', Em.Object.create({
        serviceName: 'HDFS'
      }));
      expect(view.get('hasAlertDefinitions')).to.be.true;
    });

    it('should return false if there is no alert definition for this service', function () {
      view.set('controller.content', Em.Object.create({
        serviceName: 'ZOOKEEPER'
      }));
      expect(view.get('hasAlertDefinitions')).to.be.false;
    });

  });

  describe("#restartAllStaleConfigComponents", function () {

    describe('trigger restartAllServiceHostComponents', function () {

      beforeEach(function () {
        view.set('controller.content', Em.Object.create({
          serviceName: "HDFS"
        }));
        view.set('service', Em.Object.create({
          displayName: 'HDFS'
        }));
        sinon.stub(batchUtils, "restartAllServiceHostComponents", Em.K);
      });

      afterEach(function () {
        batchUtils.restartAllServiceHostComponents.restore();
      });

      it('batch request is started', function () {
        view.restartAllStaleConfigComponents().onPrimary();
        expect(batchUtils.restartAllServiceHostComponents.calledOnce).to.equal(true);
      });

    });

    describe('trigger check last check point warning before triggering restartAllServiceHostComponents', function () {

      var mainServiceItemController;

      beforeEach(function () {
        view.set('controller.content', Em.Object.create({
          serviceName: "HDFS",
          hostComponents: [{
            componentName: 'NAMENODE',
            workStatus: 'STARTED'
          }],
          restartRequiredHostsAndComponents: {
            "host1": ['NameNode'],
            "host2": ['DataNode', 'ZooKeeper']
          }
        }));
        view.set('service', Em.Object.create({
          displayName: 'HDFS'
        }));
        mainServiceItemController = App.MainServiceItemController.create({});
        sinon.stub(mainServiceItemController, 'checkNnLastCheckpointTime', function() {
          return true;
        });
        sinon.stub(App.router, 'get', function(k) {
          if ('mainServiceItemController' === k) {
            return mainServiceItemController;
          }
          return Em.get(App.router, k);
        });
      });

      afterEach(function () {
        mainServiceItemController.checkNnLastCheckpointTime.restore();
        App.router.get.restore();
      });

      it('NN Last CheckPoint is checked', function () {
        view.restartAllStaleConfigComponents();
        expect(mainServiceItemController.checkNnLastCheckpointTime.calledOnce).to.equal(true);
      });

    });

  });

  describe("#setComponentsContent()", function() {

    beforeEach(function() {
      sinon.stub(Em.run, 'next', Em.clb);
      sinon.stub(view, 'updateComponentList');
      view.set('service', Em.Object.create({
        hostComponents: [],
        slaveComponents: [],
        clientComponents: []
      }));
      view.setProperties({
        mastersLength: 0,
        slavesLength: 0,
        clientsLength: 0,
        mastersObj: [
          {
            components: ['master']
          }
        ],
        slavesObj: ['slave'],
        clientObj: ['client']
      });
    });
    afterEach(function() {
      Em.run.next.restore();
      view.updateComponentList.restore();
    });

    it("service is null", function() {
      view.set('service', null);
      view.setComponentsContent();
      expect(Em.run.next.calledOnce).to.be.true;
      expect(view.updateComponentList.called).to.be.false
    });

    it("update master length", function() {
      view.set('mastersLength', 1);
      view.setComponentsContent();
      expect(Em.run.next.calledOnce).to.be.true;
      expect(view.updateComponentList.calledWith(['master'], [])).to.be.true;
      expect(view.get('mastersLength')).to.be.equal(0);
    });

    it("update slave length", function() {
      view.set('slavesLength', 1);
      view.setComponentsContent();
      expect(Em.run.next.calledOnce).to.be.true;
      expect(view.updateComponentList.calledWith(['slave'], [])).to.be.true;
      expect(view.get('slavesLength')).to.be.equal(0);
    });

    it("update client length", function() {
      view.set('clientsLength', 1);
      view.setComponentsContent();
      expect(Em.run.next.calledOnce).to.be.true;
      expect(view.updateComponentList.calledWith(['client'], [])).to.be.true;
      expect(view.get('clientsLength')).be.equal(0);
    });
  });

  describe("#clientsHostText", function() {

    it("no installed clients", function() {
      view.set('controller.content.installedClients', []);
      view.propertyDidChange('clientsHostText');
      expect(view.get('clientsHostText')).to.be.empty;
    });

    it("has many clients", function() {
      view.set('controller.content.installedClients', [1]);
      view.reopen({
        hasManyClients: true
      });
      view.propertyDidChange('clientsHostText');
      expect(view.get('clientsHostText')).to.be.equal(Em.I18n.t('services.service.summary.viewHosts'));
    });

    it("otherwise", function() {
      view.set('controller.content.installedClients', [1]);
      view.reopen({
        hasManyClients: false
      });
      view.propertyDidChange('clientsHostText');
      expect(view.get('clientsHostText')).to.be.equal(Em.I18n.t('services.service.summary.viewHost'));
    });
  });

  describe("#serversHost", function() {

    it("should return empty object", function() {
      view.set('controller.content', Em.Object.create({
        id: 'S1',
        hostComponents: []
      }));
      view.propertyDidChange('serversHost');
      expect(view.get('serversHost')).to.be.empty;
    });

    it("should return server object", function() {
      view.set('controller.content', Em.Object.create({
        id: 'ZOOKEEPER',
        hostComponents: [
          Em.Object.create({
            isMaster: true
          })
        ]
      }));
      view.propertyDidChange('serversHost');
      expect(view.get('serversHost')).to.eql(Em.Object.create({
        isMaster: true
      }));
    });
  });

  describe("#updateComponentList()", function() {

    it("add components to empty source", function() {
      var source = [],
          data = [{id: 1}];
      view.updateComponentList(source, data);
      expect(source.mapProperty('id')).to.eql([1]);
    });

    it("add components to exist source", function() {
      var source = [{id: 1}],
        data = [{id: 1}, {id: 2}];
      view.updateComponentList(source, data);
      expect(source.mapProperty('id')).to.eql([1, 2]);
    });

    it("remove components from exist source", function() {
      var source = [{id: 1}, {id: 2}],
        data = [{id: 1}];
      view.updateComponentList(source, data);
      expect(source.mapProperty('id')).to.eql([1]);
    });
  });

  describe("#componentNameView", function () {
    var componentNameView;

    beforeEach(function () {
      componentNameView = view.get('componentNameView').create();
    });

    describe("#displayName", function () {

      it("component is MYSQL_SERVER", function () {
        componentNameView.set('comp', Em.Object.create({
          componentName: 'MYSQL_SERVER'
        }));
        componentNameView.propertyDidChange('displayName');
        expect(componentNameView.get('displayName')).to.equal(Em.I18n.t('services.hive.databaseComponent'));
      });

      it("any component", function () {
        componentNameView.set('comp', Em.Object.create({
          componentName: 'C1',
          displayName: 'c1'
        }));
        componentNameView.propertyDidChange('displayName');
        expect(componentNameView.get('displayName')).to.equal('c1');
      });
    });
  });


  describe("#getServiceModel()", function() {

    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns({serviceName: 'S1'});
      sinon.stub(App.HDFSService, 'find').returns([{serviceName: 'HDFS'}]);
    });
    afterEach(function() {
      App.Service.find.restore();
      App.HDFSService.find.restore();
    });

    it("HDFS service", function() {
      expect(view.getServiceModel('HDFS')).to.eql({serviceName: 'HDFS'});
    });

    it("Simple model service", function() {
      expect(view.getServiceModel('S1')).to.eql({serviceName: 'S1'});
    });
  });

  describe("#updateComponentInformation()", function () {
    it("should count hosts and components", function () {
      view.set('controller.content.restartRequiredHostsAndComponents', {
        'host1': ['c1', 'c2']
      });
      view.updateComponentInformation();
      expect(view.get('componentsCount')).to.equal(2);
      expect(view.get('hostsCount')).to.equal(1);
    });
  });

  describe("#rollingRestartSlaveComponentName ", function() {

    beforeEach(function() {
      sinon.stub(batchUtils, 'getRollingRestartComponentName').returns('C1');
    });
    afterEach(function() {
      batchUtils.getRollingRestartComponentName.restore();
    });

    it("should returns component name", function() {
      view.set('serviceName', 'S1');
      view.propertyDidChange('rollingRestartSlaveComponentName');
      expect(view.get('rollingRestartSlaveComponentName')).to.equal('C1');
    });
  });

  describe("#rollingRestartActionName ", function() {

    beforeEach(function() {
      sinon.stub(App.format, 'role').returns('C1');
    });
    afterEach(function() {
      App.format.role.restore();
    });

    it("rollingRestartSlaveComponentName is set", function() {
      view.reopen({
        rollingRestartSlaveComponentName: 'C1'
      });
      view.propertyDidChange('rollingRestartActionName');
      expect(view.get('rollingRestartActionName')).to.equal(Em.I18n.t('rollingrestart.dialog.title').format('C1'));
    });

    it("rollingRestartSlaveComponentName is null", function() {
      view.reopen({
        rollingRestartSlaveComponentName: null
      });
      view.propertyDidChange('rollingRestartActionName');
      expect(view.get('rollingRestartActionName')).to.be.null;
    });
  });

  describe("#rollingRestartStaleConfigSlaveComponents() ", function() {

    beforeEach(function() {
      sinon.stub(batchUtils, 'launchHostComponentRollingRestart');
    });
    afterEach(function() {
      batchUtils.launchHostComponentRollingRestart.restore();
    });

    it("launchHostComponentRollingRestart should be called", function() {
      view.get('service').setProperties({
        displayName: 's1',
        passiveState: 'ON'
      });
      view.rollingRestartStaleConfigSlaveComponents({context: 'C1'});
      expect(batchUtils.launchHostComponentRollingRestart.calledWith(
        'C1', 's1', true, true
      )).to.be.true;
    });
  });

  App.TestAliases.testAsComputedOr(view, 'showComponentsTitleForNonMasters', ['!mastersLength', 'hasMultipleMasterGroups']);

  App.TestAliases.testAsComputedGt(view, 'hasMultipleMasterGroups', 'mastersObj.length', 1);
});