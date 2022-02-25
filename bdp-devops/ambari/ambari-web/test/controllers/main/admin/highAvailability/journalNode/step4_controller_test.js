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
require('controllers/main/admin/highAvailability/journalNode/step1_controller');
var testHelpers = require('test/helpers');

describe('App.ManageJournalNodeWizardStep4Controller', function () {
  var controller;

  beforeEach(function () {
    controller = App.ManageJournalNodeWizardStep4Controller.create({
      content: Em.Object.create()
    });
  });

  describe('#stopStandbyNameNode', function() {

    beforeEach(function() {
      sinon.stub(controller, 'updateComponent');
    });

    afterEach(function() {
      controller.updateComponent.restore();
    });

    it('updateComponent should be called', function() {
      controller.set('content.standByNN', {
        host_name: 'host1'
      });
      controller.stopStandbyNameNode();
      expect(controller.updateComponent.calledWith('NAMENODE', 'host1', 'HDFS', 'INSTALLED')).to.be.true;
    });
  });

  describe('#installJournalNodes', function() {
    var wizardController = {
      getJournalNodesToAdd: Em.K
    };

    beforeEach(function() {
      this.mock = sinon.stub(wizardController, 'getJournalNodesToAdd');
      sinon.stub(App.router, 'get').returns(wizardController);
      sinon.stub(controller, 'createInstallComponentTask');
      sinon.stub(controller, 'onTaskCompleted');
    });

    afterEach(function() {
      App.router.get.restore();
      this.mock.restore();
      controller.createInstallComponentTask.restore();
      controller.onTaskCompleted.restore();
    });

    it('onTaskCompleted should be called when no journalnodes to add', function() {
      this.mock.returns([]);
      controller.installJournalNodes();
      expect(controller.onTaskCompleted.calledOnce).to.be.true;
      expect(controller.createInstallComponentTask.called).to.be.false;
    });

    it('createInstallComponentTask should be called when there are journalnodes to add', function() {
      this.mock.returns(['host1']);
      controller.installJournalNodes();
      expect(controller.onTaskCompleted.called).to.be.false;
      expect(controller.createInstallComponentTask.calledWith('JOURNALNODE', ['host1'], "HDFS")).to.be.true;
    });
  });

  describe('#deleteJournalNodes', function() {
    var wizardController = {
      getJournalNodesToDelete: Em.K
    };

    beforeEach(function() {
      this.mock = sinon.stub(wizardController, 'getJournalNodesToDelete');
      sinon.stub(App.router, 'get').returns(wizardController);
      sinon.stub(controller, 'deleteComponent');
      sinon.stub(controller, 'onTaskCompleted');
    });

    afterEach(function() {
      App.router.get.restore();
      this.mock.restore();
      controller.deleteComponent.restore();
      controller.onTaskCompleted.restore();
    });

    it('onTaskCompleted should be called when no journalnodes to delete', function() {
      this.mock.returns([]);
      controller.deleteJournalNodes();
      expect(controller.onTaskCompleted.calledOnce).to.be.true;
      expect(controller.deleteComponent.called).to.be.false;
    });

    it('deleteComponent should be called when there are journalnodes to delete', function() {
      this.mock.returns(['host1']);
      controller.deleteJournalNodes();
      expect(controller.onTaskCompleted.called).to.be.false;
      expect(controller.deleteComponent.calledWith('JOURNALNODE', 'host1')).to.be.true;
    });
  });

  describe('#reconfigureHDFS', function() {

    beforeEach(function() {
      sinon.stub(controller, 'updateConfigProperties');
    });

    afterEach(function() {
      controller.updateConfigProperties.restore();
    });

    it('updateConfigProperties should be called', function() {
      controller.set('content.serviceConfigProperties', [
        {}
      ]);
      controller.reconfigureHDFS();
      expect(controller.updateConfigProperties.calledWith([{}])).to.be.true;
    });
  });

  describe('#updateConfigProperties', function() {

    beforeEach(function() {
      sinon.stub(controller, 'reconfigureSites').returns({});
    });

    afterEach(function() {
      controller.reconfigureSites.restore();
    });

    it('App.ajax.send should be called', function() {
      controller.updateConfigProperties();
      var args = testHelpers.findAjaxRequest('name', 'common.service.configurations');
      expect(args[0]).to.be.eql({
        name: 'common.service.configurations',
        sender: controller,
        data: {
          desired_config: {}
        },
        success: 'installHDFSClients',
        error: 'onTaskError'
      });
    });
  });

  describe('#installHDFSClients', function() {

    beforeEach(function() {
      sinon.stub(controller, 'createInstallComponentTask');
      sinon.stub(App.clusterStatus, 'setClusterStatus');
      controller.set('content.masterComponentHosts', [
        {
          component: 'NAMENODE',
          hostName: 'host1'
        },
        {
          component: 'JOURNALNODE',
          hostName: 'host2'
        }
      ]);
    });

    afterEach(function() {
      controller.createInstallComponentTask.restore();
      App.clusterStatus.setClusterStatus.restore();
    });

    it('App.clusterStatus.setClusterStatus should be called', function() {
      controller.installHDFSClients();
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });

    it('createInstallComponentTask should be called', function() {
      controller.installHDFSClients();
      expect(controller.createInstallComponentTask.calledWith('HDFS_CLIENT', ['host1', 'host2'], 'HDFS')).to.be.true;
    });
  });

});

