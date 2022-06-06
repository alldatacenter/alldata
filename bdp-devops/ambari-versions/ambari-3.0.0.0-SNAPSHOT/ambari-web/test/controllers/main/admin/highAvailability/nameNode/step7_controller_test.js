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

describe('App.HighAvailabilityWizardStep7Controller', function() {
  var controller;

  beforeEach(function() {
    controller = App.HighAvailabilityWizardStep7Controller.create({
      content: Em.Object.create()
    });
  });

  describe('#initializeTasks', function() {
    beforeEach(function() {
      sinon.stub(controller, 'removeTasks');
      sinon.stub(App.Service, 'find').returns(Em.Object.create({isLoaded: false}));
      sinon.stub(App.ClientComponent, 'getModelByComponentName').returns(Em.Object.create({
        installedCount: 0
      }));
    });
    afterEach(function() {
      controller.removeTasks.restore();
      App.Service.find.restore();
      App.ClientComponent.getModelByComponentName.restore();
    });

    it('removeTasks should be called', function() {
      controller.initializeTasks();
      expect(controller.removeTasks.calledWith(['startAmbariInfra', 'startRanger', 'startMysqlServer'])).to.be.true;
    });
  });

  describe('#startAmbariInfra', function() {
    beforeEach(function() {
      sinon.stub(controller, 'startServices');
    });
    afterEach(function() {
      controller.startServices.restore();
    });

    it('startServices should be called', function() {
      controller.startAmbariInfra();
      expect(controller.startServices.calledWith(false, ['AMBARI_INFRA_SOLR'], true)).to.be.true;
    });
  });

  describe('#startRanger', function() {
    beforeEach(function() {
      sinon.stub(controller, 'updateComponent');
    });
    afterEach(function() {
      controller.updateComponent.restore();
    });

    it('updateComponent should be called', function() {
      controller.set('content.masterComponentHosts', [
        {
          component: 'RANGER_ADMIN',
          hostName: 'host1'
        }
      ]);
      controller.startRanger();
      expect(controller.updateComponent.calledWith('RANGER_ADMIN', ['host1'], "RANGER", "Start")).to.be.true;
    });
  });

  describe('#startMysqlServer', function() {
    beforeEach(function() {
      sinon.stub(App.MasterComponent, 'find').returns(Em.Object.create({
        hostNames: ['host1']
      }));
      sinon.stub(controller, 'updateComponent');
    });
    afterEach(function() {
      App.MasterComponent.find.restore();
      controller.updateComponent.restore();
    });

    it('updateComponent should be called', function() {
      controller.startMysqlServer();
      expect(controller.updateComponent.calledWith('MYSQL_SERVER', ['host1'], "HIVE", "Start")).to.be.true;
    });
  });

  describe('#startZooKeeperServers', function() {
    beforeEach(function() {
      sinon.stub(controller, 'updateComponent');
    });
    afterEach(function() {
      controller.updateComponent.restore();
    });

    it('updateComponent should be called', function() {
      controller.set('content.masterComponentHosts', [
        {
          component: 'ZOOKEEPER_SERVER',
          hostName: 'host1'
        }
      ]);
      controller.startZooKeeperServers();
      expect(controller.updateComponent.calledWith('ZOOKEEPER_SERVER', ['host1'], "ZOOKEEPER", "Start")).to.be.true;
    });
  });

  describe('#startNameNode', function() {
    beforeEach(function() {
      sinon.stub(controller, 'updateComponent');
    });
    afterEach(function() {
      controller.updateComponent.restore();
    });

    it('updateComponent should be called', function() {
      controller.set('content.masterComponentHosts', [
        {
          component: 'NAMENODE',
          hostName: 'host1',
          isInstalled: true
        }
      ]);
      controller.startNameNode();
      expect(controller.updateComponent.calledWith('NAMENODE', 'host1', "HDFS", "Start")).to.be.true;
    });
  });
});
