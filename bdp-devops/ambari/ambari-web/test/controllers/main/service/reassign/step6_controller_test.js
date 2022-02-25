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

require('controllers/main/service/reassign/step6_controller');
var controller;
var testHelpers = require('test/helpers');

describe('App.ReassignMasterWizardStep6Controller', function () {

  beforeEach(function () {
    controller = App.ReassignMasterWizardStep6Controller.create({
      content: Em.Object.create({
        reassign: Em.Object.create(),
        reassignHosts: Em.Object.create()
      }),
      startServices: Em.K,
      tasks: []
    });
  });

  describe('#initializeTasks()', function () {
    it('No commands', function () {
      controller.set('commands', []);
      controller.set('hostComponents', []);
      controller.initializeTasks();

      expect(controller.get('tasks')).to.be.empty;
    });
    it('One command', function () {
      controller.set('commands', ['COMMAND1']);
      controller.initializeTasks();

      expect(controller.get('tasks')[0].get('id')).to.equal(0);
      expect(controller.get('tasks')[0].get('command')).to.equal('COMMAND1');
    });
  });

  describe('#hideRollbackButton()', function () {

    it('No showRollback command', function () {
      controller.set('tasks', [Em.Object.create({
        showRollback: false
      })]);
      controller.hideRollbackButton();
      expect(controller.get('tasks')[0].get('showRollback')).to.be.false;
    });
    it('showRollback command is present', function () {
      controller.set('tasks', [Em.Object.create({
        showRollback: true
      })]);
      controller.hideRollbackButton();
      expect(controller.get('tasks')[0].get('showRollback')).to.be.false;
    });
  });

  describe('#onComponentsTasksSuccess()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'onTaskCompleted', Em.K);
    });
    afterEach(function () {
      controller.onTaskCompleted.restore();
    });

    it('One host-component', function () {
      controller.set('multiTaskCounter', 1);
      controller.set('hostComponents', [
        {}
      ]);
      controller.onComponentsTasksSuccess();
      expect(controller.get('multiTaskCounter')).to.equal(0);
      expect(controller.onTaskCompleted.calledOnce).to.be.true;
    });
    it('two host-components', function () {
      controller.set('multiTaskCounter', 2);
      controller.set('hostComponents', [
        {},
        {}
      ]);
      controller.onComponentsTasksSuccess();
      expect(controller.get('multiTaskCounter')).to.equal(1);
      expect(controller.onTaskCompleted.called).to.be.false;
    });
  });


  describe('#loadStep()', function () {
    var isHaEnabled = true;

    beforeEach(function () {
      controller.set('content.reassign.service_id', 'service1');
      sinon.stub(controller, 'onTaskStatusChange', Em.K);
      sinon.stub(controller, 'initializeTasks', Em.K);
      sinon.stub(App, 'get', function () {
        return isHaEnabled;
      });
    });
    afterEach(function () {
      controller.onTaskStatusChange.restore();
      controller.initializeTasks.restore();
      App.get.restore();
    });

    it('reassign component is NameNode and HA enabled', function () {
      isHaEnabled = true;
      controller.set('content.reassign.component_name', 'NAMENODE');

      controller.loadStep();
      expect(controller.get('hostComponents')).to.eql(['NAMENODE', 'ZKFC']);
    });
    it('reassign component is NameNode and HA disabled', function () {
      isHaEnabled = false;
      controller.set('content.reassign.component_name', 'NAMENODE');

      controller.loadStep();
      expect(controller.get('hostComponents')).to.eql(['NAMENODE']);
    });
    it('reassign component is RESOURCEMANAGER', function () {
      controller.set('content.reassign.component_name', 'RESOURCEMANAGER');

      controller.loadStep();
      expect(controller.get('hostComponents')).to.eql(['RESOURCEMANAGER']);
    });
  });

  describe('#loadStep() for reassign NameNode with PXF service installed', function () {

    var serviceStub, hostComponentStub, pxfHosts, dataNodeHosts;

    var commands = [
      'stopMysqlService',
      'putHostComponentsInMaintenanceMode',
      'stopHostComponentsInMaintenanceMode',
      'deleteHostComponents',
      'startAllServices'
    ];

    var reassignHosts = {
      "source": "c6401.ambari.apache.org",
      "target": "c6403.ambari.apache.org"
    };

    beforeEach(function () {
      controller.set('content.reassign.service_id', 'HDFS');
      controller.set('content.reassign.component_name', 'NAMENODE');
      controller.set('commands', commands.copy());
      controller.set('content.reassignHosts', reassignHosts);
      sinon.stub(controller, 'onTaskStatusChange', Em.K);
      sinon.stub(controller, 'initializeTasks', Em.K);
      serviceStub = sinon.stub(App.Service.find(), 'someProperty');
      hostComponentStub = sinon.stub(App.HostComponent.find(), 'filterProperty');
      serviceStub.withArgs('serviceName', 'PXF').returns(true);
    });

    afterEach(function () {
      controller.onTaskStatusChange.restore();
      controller.initializeTasks.restore();
      serviceStub.restore();
      hostComponentStub.restore();
    });

    var setUpHosts = function (_pxfHosts, _dataNodeHosts) {
      hostComponentStub.withArgs('componentName', 'PXF').returns(_pxfHosts);
      hostComponentStub.withArgs('componentName', 'DATANODE').returns(_dataNodeHosts);
    };

    it('does not delete PXF from source host if PXF and DATANODE are not installed on the source host', function () {
      pxfHosts = [{"hostName": "c6402.ambari.apache.org"}];
      dataNodeHosts = [{"hostName": "c6402.ambari.apache.org"}];
      setUpHosts(pxfHosts, dataNodeHosts);

      controller.loadStep();
      expect(controller.get('hostComponents')).to.eql(['NAMENODE']);
    });

    it('does not delete PXF from source host if PXF is not installed on the source host and DATANODE is installed on the source host', function () {
      pxfHosts = [{"hostName": "c6402.ambari.apache.org"}];
      dataNodeHosts = [{"hostName": "c6401.ambari.apache.org"}, {"hostName": "c6402.ambari.apache.org"}];
      setUpHosts(pxfHosts, dataNodeHosts);

      controller.loadStep();
      expect(controller.get('hostComponents')).to.eql(['NAMENODE']);
    });

    it('deletes PXF from source host if PXF is installed on the source host and DATANODE is not installed on the source host', function () {
      pxfHosts = [{"hostName": "c6401.ambari.apache.org"}, {"hostName": "c6402.ambari.apache.org"}];
      dataNodeHosts = [{"hostName": "c6402.ambari.apache.org"}];
      setUpHosts(pxfHosts, dataNodeHosts);

      controller.loadStep();
      expect(controller.get('hostComponents')).to.eql(['NAMENODE', 'PXF']);
    });

    it('does not delete PXF from source host if PXF and DATANODE are installed on the source host', function () {
      pxfHosts = [{"hostName": "c6401.ambari.apache.org"}, {"hostName": "c6402.ambari.apache.org"}];
      dataNodeHosts = [{"hostName": "c6401.ambari.apache.org"}, {"hostName": "c6402.ambari.apache.org"}];
      setUpHosts(pxfHosts, dataNodeHosts);

      controller.loadStep();
      expect(controller.get('hostComponents')).to.eql(['NAMENODE']);
    });

    it('does not install PXF on the target host if PXF is already installed on the target host', function () {
      pxfHosts = [{"hostName": "c6403.ambari.apache.org"}];
      setUpHosts(pxfHosts, []);

      controller.loadStep();
      expect(controller.get('commands').indexOf('installPxf')).to.eql(-1);
    });

    it('installs PXF on the target host if PXF is not installed on the target host', function () {
      pxfHosts = [{"hostName": "c6401.ambari.apache.org"}, {"hostName": "c6402.ambari.apache.org"}];
      setUpHosts(pxfHosts, []);

      controller.loadStep();
      expect(controller.get('commands').indexOf('installPxf')).to.be.equal(4);
    });

  });

  describe('#deleteHostComponents()', function () {

    it('No host components', function () {
      controller.set('hostComponents', []);
      controller.set('content.reassignHosts.source', 'host1');
      controller.deleteHostComponents();
      var args = testHelpers.findAjaxRequest('name', 'common.delete.host_component');
      expect(args).not.exists;
    });
    it('delete two components', function () {
      controller.set('hostComponents', [1, 2]);
      controller.set('content.reassignHosts.source', 'host1');
      controller.deleteHostComponents();
      var args = testHelpers.filterAjaxRequests('name', 'common.delete.host_component');
      expect(args).to.have.property('length').equal(2);
      expect(args[0][0].data).to.eql({
        "hostName": "host1",
        "componentName": 1
      });
      expect(args[1][0].data).to.eql({
        "hostName": "host1",
        "componentName": 2
      });
    });
  });

  describe('#onDeleteHostComponentsError()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'onComponentsTasksSuccess', Em.K);
      sinon.stub(controller, 'onTaskError', Em.K);
    });
    afterEach(function () {
      controller.onComponentsTasksSuccess.restore();
      controller.onTaskError.restore();
    });

    it('task success', function () {
      var error = {
        responseText: 'org.apache.ambari.server.controller.spi.NoSuchResourceException'
      };
      controller.onDeleteHostComponentsError(error);
      expect(controller.onComponentsTasksSuccess.calledOnce).to.be.true;
    });
    it('unknown error', function () {
      var error = {
        responseText: ''
      };
      controller.onDeleteHostComponentsError(error);
      expect(controller.onTaskError.calledOnce).to.be.true;
    });
  });

  describe('#stopMysqlService()', function () {
    it('stopMysqlService', function () {
      controller.stopMysqlService();
      var args = testHelpers.findAjaxRequest('name', 'common.host.host_component.update');
      expect(args[0]).exists;
    });
  });

  describe('#putHostComponentsInMaintenanceMode()', function () {
    beforeEach(function(){
      sinon.stub(controller, 'onComponentsTasksSuccess', Em.K);
      controller.set('content.reassignHosts.source', 'source');
    });
    afterEach(function(){
      controller.onComponentsTasksSuccess.restore();
    });
    it('No host-components', function () {
      controller.set('hostComponents', []);
      controller.putHostComponentsInMaintenanceMode();
      var args = testHelpers.findAjaxRequest('name', 'common.host.host_component.passive');
      expect(args).not.exists;
      expect(controller.get('multiTaskCounter')).to.equal(0);
    });
    it('One host-components', function () {
      controller.set('hostComponents', [{}]);
      controller.putHostComponentsInMaintenanceMode();
      var args = testHelpers.findAjaxRequest('name', 'common.host.host_component.passive');
      expect(args).exists;
      expect(controller.get('multiTaskCounter')).to.equal(1);
    });
  });

  describe("#removeTasks()", function() {
    it("no tasks to delete", function() {
      controller.set('tasks', [Em.Object.create()]);
      controller.removeTasks([]);
      expect(controller.get('tasks').length).to.equal(1);
    });
    it("one task to delete", function() {
      controller.set('tasks', [Em.Object.create({command: 'task1'})]);
      controller.removeTasks(['task1']);
      expect(controller.get('tasks')).to.be.empty;
    });
  });

  describe("#startAllServices()", function() {
    beforeEach(function () {
      sinon.stub(controller, 'startServices', Em.K);
    });
    afterEach(function () {
      controller.startServices.restore();
    });
    it("startServices is called with valid arguments", function () {
      controller.startAllServices();
      expect(controller.startServices.calledWith(true)).to.be.true;
    });
  });
});