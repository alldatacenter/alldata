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
var testHelpers = require('test/helpers');


describe('App.HighAvailabilityWizardStep9Controller', function() {
  var controller;

  beforeEach(function() {
    controller = App.HighAvailabilityWizardStep9Controller.create({
      content: Em.Object.create()
    });
  });

  describe('#initializeTasks', function() {
    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns([{
        serviceName: 'PXF'
      }]);
      sinon.stub(controller, 'isPxfComponentInstalled').returns(true);
      sinon.stub(controller, 'removeTasks');
      controller.set('content.masterComponentHosts', [
        {
          component: 'NAMENODE',
          isInstalled: false,
          hostName: 'host1'
        }
      ]);
    });
    afterEach(function() {
      App.Service.find.restore();
      controller.isPxfComponentInstalled.restore();
      controller.removeTasks.restore();
    });

    it('removeTasks should be called', function() {
      controller.initializeTasks();
      expect(controller.get('secondNameNodeHost')).to.be.equal('host1');
      expect(controller.removeTasks.calledWith([
        'installPXF',
        'reconfigureRanger',
        'reconfigureHBase',
        'reconfigureAMS',
        'reconfigureAccumulo',
        'reconfigureHawq'
      ])).to.be.true;
    });
  });

  describe('#startSecondNameNode', function() {
    beforeEach(function() {
      sinon.stub(controller, 'updateComponent');
    });
    afterEach(function() {
      controller.updateComponent.restore();
    });

    it('updateComponent should be called', function() {
      controller.set('secondNameNodeHost', 'host1');
      controller.startSecondNameNode();
      expect(controller.updateComponent.calledWith('NAMENODE', 'host1', "HDFS", "Start")).to.be.true;
    });
  });

  describe('#installZKFC', function() {
    beforeEach(function() {
      sinon.stub(controller, 'createInstallComponentTask');
    });
    afterEach(function() {
      controller.createInstallComponentTask.restore();
    });

    it('createInstallComponentTask should be called', function() {
      controller.set('content.masterComponentHosts', [
        {
          component: 'NAMENODE',
          hostName: 'host1'
        }
      ]);
      controller.installZKFC();
      expect(controller.createInstallComponentTask.calledWith('ZKFC', ['host1'], "HDFS")).to.be.true;
    });
  });

  describe('#startZKFC', function() {
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
          hostName: 'host1'
        }
      ]);
      controller.startZKFC();
      expect(controller.updateComponent.calledWith('ZKFC', ['host1'], "HDFS", "Start")).to.be.true;
    });
  });

  describe('#isPxfComponentInstalled', function() {
    beforeEach(function() {
      this.mock = sinon.stub(controller, 'getSlaveComponentHosts');
      controller.set('secondNameNodeHost', 'host1');
    });
    afterEach(function() {
      this.mock.restore();
    });

    it('should return true', function() {
      this.mock.returns([
        {
          componentName: 'PXF',
          hosts: [{
            hostName: 'host1'
          }]
        }
      ]);
      expect(controller.isPxfComponentInstalled()).to.be.true;
    });

    it('should return false', function() {
      this.mock.returns([]);
      expect(controller.isPxfComponentInstalled()).to.be.false;
    });
  });

  describe('#installPXF', function() {
    beforeEach(function() {
      sinon.stub(controller, 'createInstallComponentTask');
    });
    afterEach(function() {
      controller.createInstallComponentTask.restore();
    });

    it('createInstallComponentTask should be called', function() {
      controller.set('secondNameNodeHost', 'host1');
      controller.installPXF();
      expect(controller.createInstallComponentTask.calledWith('PXF', 'host1', "PXF")).to.be.true;
    });
  });

  describe('#saveReconfiguredConfigs', function() {
    beforeEach(function() {
      sinon.stub(controller, 'reconfigureSites').returns({});
    });
    afterEach(function() {
      controller.reconfigureSites.restore();
    });

    it('App.ajax.send should be called', function() {
      controller.saveReconfiguredConfigs(['site1'], {});
      var args = testHelpers.findAjaxRequest('name', 'common.service.configurations');
      expect(args[0]).to.be.eql({
        name: 'common.service.configurations',
        sender: controller,
        data: {
          desired_config: {}
        },
        success: 'saveConfigTag',
        error: 'onTaskError'
      });
    });
  });

  describe('#reconfigureHBase', function() {
    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns([
        {
          serviceName: 'RANGER'
        }
      ]);
      sinon.stub(controller, 'saveReconfiguredConfigs');
    });
    afterEach(function() {
      App.Service.find.restore();
      controller.saveReconfiguredConfigs.restore();
    });

    it('saveReconfiguredConfigs should be called', function() {
      var data = {
        items: [
          {
            type: 'ranger-hbase-plugin-properties',
            properties: {
              'xasecure.audit.destination.hdfs.dir': ''
            }
          },
          {
            type: 'ranger-hbase-audit',
            properties: {
              'xasecure.audit.destination.hdfs.dir': ''
            }
          }
        ]
      };
      controller.set('content.serviceConfigProperties', data);
      controller.reconfigureHBase();
      expect(controller.saveReconfiguredConfigs.calledWith([
        'hbase-site', 'ranger-hbase-plugin-properties', 'ranger-hbase-audit'
      ], data)).to.be.true;
    });
  });

  describe('#reconfigureAMS', function() {
    beforeEach(function() {
      sinon.stub(controller, 'saveReconfiguredConfigs');
    });
    afterEach(function() {
      controller.saveReconfiguredConfigs.restore();
    });

    it('saveReconfiguredConfigs should be called', function() {
      controller.set('content.serviceConfigProperties', {});
      controller.reconfigureAMS();
      expect(controller.saveReconfiguredConfigs.calledWith(['ams-hbase-site'], {})).to.be.true;
    });
  });

  describe('#reconfigureAccumulo', function() {
    beforeEach(function() {
      sinon.stub(controller, 'saveReconfiguredConfigs');
    });
    afterEach(function() {
      controller.saveReconfiguredConfigs.restore();
    });

    it('saveReconfiguredConfigs should be called', function() {
      controller.set('content.serviceConfigProperties', {});
      controller.reconfigureAccumulo();
      expect(controller.saveReconfiguredConfigs.calledWith(['accumulo-site'], {})).to.be.true;
    });
  });

  describe('#reconfigureHawq', function() {
    beforeEach(function() {
      sinon.stub(controller, 'saveReconfiguredConfigs');
    });
    afterEach(function() {
      controller.saveReconfiguredConfigs.restore();
    });

    it('saveReconfiguredConfigs should be called', function() {
      controller.set('content.serviceConfigProperties', {});
      controller.reconfigureHawq();
      expect(controller.saveReconfiguredConfigs.calledWith(['hawq-site', 'hdfs-client'], {})).to.be.true;
    });
  });

  describe('#saveConfigTag', function() {
    beforeEach(function() {
      sinon.stub(App.clusterStatus, 'setClusterStatus');
      sinon.stub(controller, 'onTaskCompleted');
      controller.saveConfigTag();
    });
    afterEach(function() {
      App.clusterStatus.setClusterStatus.restore();
      controller.onTaskCompleted.restore();
    });

    it('App.clusterStatus.setClusterStatus should be called', function() {
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });

    it('onTaskCompleted should be called', function() {
      expect(controller.onTaskCompleted.calledOnce).to.be.true;
    });
  });

  describe('#startAllServices', function() {
    beforeEach(function() {
      sinon.stub(controller, 'startServices');
    });
    afterEach(function() {
      controller.startServices.restore();
    });

    it('startServices should be called', function() {
      controller.startAllServices();
      expect(controller.startServices.calledWith(false)).to.be.true;
    });
  });

  describe('#stopHDFS', function() {
    beforeEach(function() {
      sinon.stub(controller, 'stopServices');
    });
    afterEach(function() {
      controller.stopServices.restore();
    });

    it('stopServices should be called', function() {
      controller.stopHDFS();
      expect(controller.stopServices.calledWith(['HDFS'], true)).to.be.true;
    });
  });

  describe('#deleteSNameNode', function() {

    it('App.ajax.send should be called', function() {
      controller.set('content.masterComponentHosts', [
        {
          component: 'SECONDARY_NAMENODE',
          hostName: 'host1'
        }
      ]);
      controller.deleteSNameNode();
      var args = testHelpers.findAjaxRequest('name', 'common.delete.host_component');
      expect(args[0]).to.be.eql({
        name: 'common.delete.host_component',
        sender: controller,
        data: {
          componentName: 'SECONDARY_NAMENODE',
          hostName: 'host1'
        },
        success: 'onTaskCompleted',
        error: 'onTaskError'
      });
    });
  });
});
