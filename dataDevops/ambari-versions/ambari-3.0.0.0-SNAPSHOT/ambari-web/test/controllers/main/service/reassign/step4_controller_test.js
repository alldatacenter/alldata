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

require('controllers/main/service/reassign/step4_controller');
require('controllers/main/service/reassign_controller');
var testHelpers = require('test/helpers');

describe('App.ReassignMasterWizardStep4Controller', function () {

  var controller = App.ReassignMasterWizardStep4Controller.create({
    content: Em.Object.create({
      reassign: Em.Object.create(),
      reassignHosts: Em.Object.create()
    })
  });

  describe('#getHostComponentsNames()', function () {
    it('No host-components', function () {
      controller.set('hostComponents', []);
      expect(controller.getHostComponentsNames()).to.be.empty;
    });
    it('one host-components', function () {
      controller.set('hostComponents', ['COMP1']);
      expect(controller.getHostComponentsNames()).to.equal('Comp1');
    });
    it('ZKFC host-components', function () {
      controller.set('hostComponents', ['COMP1', 'ZKFC']);
      expect(controller.getHostComponentsNames()).to.equal('Comp1+ZKFC');
    });
  });

  describe('#testDBConnection', function() {
    beforeEach(function() {
      controller.set('requiredProperties', Em.A([]));
      controller.set('content.configs', Em.Object.create({
        'hive-site': {
          'javax.jdo.option.ConnectionDriverName': 'mysql'
        }
      }));
      controller.set('content.reassign.component_name', 'HIVE_SERVER');
      sinon.stub(controller, 'getConnectionProperty', Em.K);
      sinon.stub(App.router, 'get', Em.K);
    });

    afterEach(function() {
      controller.getConnectionProperty.restore();
      App.router.get.restore();
    });

    describe('tests database connection', function() {

      beforeEach(function () {
        sinon.stub(controller, 'prepareDBCheckAction', Em.K);
      });

      afterEach(function () {
        controller.prepareDBCheckAction.restore();
      });

      it('prepareDBCheckAction is called once', function() {
        controller.testDBConnection();
        expect(controller.prepareDBCheckAction.calledOnce).to.be.true;
      });

    });

    it('tests prepareDBCheckAction', function() {
      controller.prepareDBCheckAction();
      var args = testHelpers.findAjaxRequest('name', 'cluster.custom_action.create');
      expect(args).exists;
    });

  });

  describe('#removeUnneededTasks()', function () {
    var isHaEnabled = false;
    var commands;
    var commandsForDB;

    beforeEach(function () {
      sinon.stub(App, 'get', function () {
        return isHaEnabled;
      });

      commands = [
        { id: 1, command: 'stopRequiredServices' },
        { id: 2, command: 'cleanMySqlServer' },
        { id: 3, command: 'createHostComponents' },
        { id: 4, command: 'putHostComponentsInMaintenanceMode' },
        { id: 5, command: 'reconfigure' },
        { id: 6, command: 'installHostComponents' },
        { id: 7, command: 'startZooKeeperServers' },
        { id: 8, command: 'startNameNode' },
        { id: 9, command: 'deleteHostComponents' },
        { id: 10, command: 'configureMySqlServer' },
        { id: 11, command: 'startMySqlServer' },
        { id: 12, command: 'startNewMySqlServer' },
        { id: 13, command: 'startRequiredServices' }
      ];

      commandsForDB = [
        { id: 1, command: 'createHostComponents' },
        { id: 2, command: 'installHostComponents' },
        { id: 3, command: 'configureMySqlServer' },
        { id: 4, command: 'restartMySqlServer' },
        { id: 5, command: 'testDBConnection' },
        { id: 6, command: 'stopRequiredServices' },
        { id: 7, command: 'cleanMySqlServer' },
        { id: 8, command: 'putHostComponentsInMaintenanceMode' },
        { id: 9, command: 'reconfigure' },
        { id: 10, command: 'deleteHostComponents' },
        { id: 11, command: 'configureMySqlServer' },
        { id: 12, command: 'startRequiredServices' }
      ];
    });

    afterEach(function () {
      App.get.restore();
    });

    it('hasManualSteps is false', function () {
      controller.set('tasks', commands);
      controller.set('content.hasManualSteps', false);

      controller.removeUnneededTasks();
      expect(controller.get('tasks').mapProperty('id')).to.eql([1, 3, 4, 6, 9, 12, 13]);
    });

    it('reassign component is not NameNode and HA disabled', function () {
      controller.set('tasks', commands);
      controller.set('content.hasManualSteps', true);
      controller.set('content.reassign.component_name', 'COMP1');
      isHaEnabled = false;

      controller.removeUnneededTasks();
      expect(controller.get('tasks').mapProperty('id')).to.eql([1, 3, 4, 6]);
    });

    it('reassign component is not NameNode and HA enabled', function () {
      controller.set('tasks', commands);
      controller.set('content.hasManualSteps', true);
      controller.set('content.reassign.component_name', 'COMP1');
      isHaEnabled = true;

      controller.removeUnneededTasks();
      expect(controller.get('tasks').mapProperty('id')).to.eql([1, 3, 4, 6]);
    });

    it('reassign component is NameNode and HA disabled', function () {
      controller.set('tasks', commands);
      controller.set('content.hasManualSteps', true);
      controller.set('content.reassign.component_name', 'NAMENODE');
      isHaEnabled = false;

      controller.removeUnneededTasks();
      expect(controller.get('tasks').mapProperty('id')).to.eql([1, 3, 4, 6]);
    });

    it('reassign component is NameNode and HA enabled', function () {
      controller.set('tasks', commands);
      controller.set('content.hasManualSteps', true);
      controller.set('content.reassign.component_name', 'NAMENODE');
      isHaEnabled = true;

      controller.removeUnneededTasks();
      expect(controller.get('tasks').mapProperty('id')).to.eql([1, 3, 4, 6, 7, 8]);
    });

    it('component with reconfiguration', function () {
      controller.set('tasks', commands);
      controller.set('content.hasManualSteps', false);
      controller.set('content.reassign.component_name', 'COMP1');
      controller.set('wizardController', {
        isComponentWithReconfiguration: true
      });

      controller.removeUnneededTasks();
      expect(controller.get('tasks').mapProperty('id')).to.eql([1, 3, 4, 5, 6, 9, 13]);
    });

    it('reassign component is HiveServer and db type is mysql', function () {
      controller.set('tasks', commandsForDB);
      controller.set('content.hasManualSteps', false);
      controller.set('content.databaseType', 'mysql');
      controller.set('content.reassign.component_name', 'HIVE_SERVER');
      isHaEnabled = false;

      controller.removeUnneededTasks();
      expect(controller.get('tasks').mapProperty('id')).to.eql([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]);
    });

    it('reassign component is HiveServer and db type is not mysql', function () {
      controller.set('tasks', commandsForDB);
      controller.set('content.hasManualSteps', false);
      controller.set('content.databaseType', 'derby');
      controller.set('content.reassign.component_name', 'HIVE_SERVER');
      isHaEnabled = false;

      controller.removeUnneededTasks();
      expect(controller.get('tasks').mapProperty('id')).to.eql([1, 2, 6, 8, 9, 10, 12]);
    });

    it('reassign component is Oozie Server and db type is derby', function () {
      controller.set('tasks', commandsForDB);
      controller.set('content.hasManualSteps', true);
      controller.set('content.databaseType', 'derby');
      controller.set('content.reassign.component_name', 'OOZIE_SERVER');
      isHaEnabled = false;

      controller.removeUnneededTasks();
      expect(controller.get('tasks').mapProperty('id')).to.eql([1,2,6,8,9]);
    });

    it('reassign component is Oozie Server and db type is mysql', function () {
      controller.set('content.hasManualSteps', false);
      controller.set('content.databaseType', 'mysql');
      controller.set('content.reassign.component_name', 'OOZIE_SERVER');
      isHaEnabled = false;

      controller.set('tasks', commandsForDB);
      controller.removeUnneededTasks();
      expect(controller.get('tasks').mapProperty('id')).to.eql([1,2,3,4,5,6,7,8,9,10,11,12]);
    });

    it('reassign component is Mysql Server', function () {
      controller.set('content.hasManualSteps', false);
      controller.set('content.databaseType', 'mysql');
      controller.set('content.reassign.component_name', 'MYSQL_SERVER');
      isHaEnabled = false;

      controller.set('tasks', commandsForDB);
      controller.removeUnneededTasks();
      expect(controller.get('tasks').mapProperty('id')).to.eql([1,2,3,4,5,6,8,9,10,11,12]);
    });
  });

  describe("#stopRequiredServices()", function() {
    before(function () {
      controller.set('wizardController', App.get('router.reassignMasterController'));
      sinon.stub(controller, 'stopServices', Em.K);
    });
    after(function () {
      controller.stopServices.restore();
    });
    it('stopServices is called with valid list of services', function() {
      controller.set('content.reassign.component_name', 'HISTORYSERVER');
      controller.set('content.componentsToStopAllServices', ['NAMENODE', 'SECONDARY_NAMENODE'])
      controller.stopRequiredServices();
      expect(controller.stopServices.calledWith(['MAPREDUCE2', 'HIVE', 'PIG', 'OOZIE'], true)).to.be.true;
    });
  });

  describe('#initializeTasks()', function () {
    beforeEach(function () {
      controller.set('tasks', []);
      sinon.stub(controller, 'getHostComponentsNames', Em.K);
      sinon.stub(controller, 'removeUnneededTasks', Em.K);
      this.mock = sinon.stub(controller, 'isComponentWithDB');
    });
    afterEach(function () {
      controller.removeUnneededTasks.restore();
      controller.getHostComponentsNames.restore();
      this.mock.restore();
    });
    it('No commands (isComponentWithDB = false)', function () {
      controller.set('commands', []);
      controller.set('commandsForDB', []);
      this.mock.returns(false);
      controller.initializeTasks();

      expect(controller.get('tasks')).to.be.empty;
    });

    it('No commands (isComponentWithDB = true)', function () {
      controller.set('commands', []);
      controller.set('commandsForDB', []);
      this.mock.returns(true);
      controller.initializeTasks();

      expect(controller.get('tasks')).to.be.empty;
    });

    it('One command', function () {
      controller.set('commands', ['COMMAND1']);
      controller.set('commandsForDB', ['COMMAND1']);
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

  describe('#stopServices()', function () {
    it('request is sent', function () {
      var servicesToStop;
      controller.stopServices(servicesToStop, true, true);
      var args = testHelpers.findAjaxRequest('name', 'common.services.update');
      expect(args).exists;
    });
  });

  describe('#createHostComponents()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'createComponent', Em.K);
    });
    afterEach(function () {
      controller.createComponent.restore();
      controller.set('dependentHostComponents', []);
    });

    it('createComponent should be called for each host-component', function () {
      controller.set('hostComponents', ['COMP1']);
      controller.set('dependentHostComponents', ['COMP2']);
      controller.set('content.reassignHosts.target', 'host1');
      controller.set('content.reassign.service_id', 'SERVICE1');

      controller.createHostComponents();

      expect(controller.get('multiTaskCounter')).to.equal(2);
      expect(controller.createComponent.getCall(0).args).to.be.eql([
        'COMP1', 'host1', 'SERVICE1'
      ]);
      expect(controller.createComponent.getCall(1).args).to.be.eql([
        'COMP2', 'host1', 'SERVICE1'
      ]);
    });
  });

  describe('#onCreateComponent()', function () {

    beforeEach(function () {
      sinon.stub(controller, 'onComponentsTasksSuccess', Em.K);
    });

    afterEach(function () {
      controller.onComponentsTasksSuccess.restore();
    });

    it('onComponentsTasksSuccess is called once', function () {
      controller.onCreateComponent();
      expect(controller.onComponentsTasksSuccess.calledOnce).to.be.true;
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

  describe('#installHostComponents()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'updateComponent', Em.K);
    });
    afterEach(function () {
      controller.updateComponent.restore();
      controller.set('dependentHostComponents', []);
    });

    it('No host-components', function () {
      controller.set('hostComponents', []);

      controller.installHostComponents();

      expect(controller.get('multiTaskCounter')).to.equal(0);
      expect(controller.updateComponent.called).to.be.false;
    });
    it('createComponent should be called for each host-component', function () {
      controller.set('hostComponents', ['COMP1']);
      controller.set('dependentHostComponents', ['COMP2']);
      controller.set('content.reassignHosts.target', 'host1');
      controller.set('content.reassign.service_id', 'SERVICE1');

      controller.installHostComponents();

      expect(controller.get('multiTaskCounter')).to.equal(2);
      expect(controller.updateComponent.getCall(0).args).to.be.eql([
        'COMP1', 'host1', 'SERVICE1', 'Install', 2
      ]);
      expect(controller.updateComponent.getCall(1).args).to.be.eql([
        'COMP2', 'host1', 'SERVICE1', 'Install', 2
      ]);
    });
  });

  describe('#reconfigure()', function () {

    beforeEach(function () {
      sinon.stub(controller, 'saveClusterStatus', Em.K);
      sinon.stub(controller, 'saveConfigsToServer', Em.K);
    });

    afterEach(function () {
      controller.saveClusterStatus.restore();
      controller.saveConfigsToServer.restore();
    });

    it('saveClusterStatus is called once', function () {
      controller.reconfigure();
      expect(controller.saveClusterStatus.calledOnce).to.be.true;
    });

    it('saveConfigsToServer is called once', function () {
      controller.reconfigure();
      expect(controller.saveConfigsToServer.calledOnce).to.be.true;
    });
  });

  describe('#loadStep()', function () {
    beforeEach(function () {
      controller.set('content.reassign.service_id', 'service1');
      sinon.stub(controller, 'onTaskStatusChange', Em.K);
      sinon.stub(controller, 'initializeTasks', Em.K);
      sinon.stub(controller, 'setDependentHostComponents');
      this.mockGet = sinon.stub(App, 'get').returns(true);
    });
    afterEach(function () {
      controller.setDependentHostComponents.restore();
      controller.onTaskStatusChange.restore();
      controller.initializeTasks.restore();
      this.mockGet.restore();
    });

    it('reassign component is NameNode and HA enabled', function () {
      controller.set('content.reassign.component_name', 'NAMENODE');

      controller.loadStep();
      expect(controller.get('hostComponents')).to.eql(['NAMENODE', 'ZKFC']);
      expect(controller.get('serviceName')).to.eql(['service1']);
    });
    it('reassign component is NameNode and HA disabled', function () {
      this.mockGet.returns(false);
      controller.set('content.reassign.component_name', 'NAMENODE');

      controller.loadStep();
      expect(controller.get('hostComponents')).to.eql(['NAMENODE']);
      expect(controller.get('serviceName')).to.eql(['service1']);
    });
    it('reassign component is JOBTRACKER and HA enabled', function () {
      controller.set('content.reassign.component_name', 'JOBTRACKER');

      controller.loadStep();
      expect(controller.get('hostComponents')).to.eql(['JOBTRACKER']);
      expect(controller.get('serviceName')).to.eql(['service1']);
    });
    it('reassign component is RESOURCEMANAGER and HA enabled', function () {
      controller.set('content.reassign.component_name', 'RESOURCEMANAGER');

      controller.loadStep();
      expect(controller.get('hostComponents')).to.eql(['RESOURCEMANAGER']);
      expect(controller.get('serviceName')).to.eql(['service1']);
    });
    it('setDependentHostComponents should be called', function () {
      controller.set('content.reassign.component_name', 'RESOURCEMANAGER');

      controller.loadStep();
      expect(controller.setDependentHostComponents.calledOnce).to.be.true;
    });
  });

  describe('#saveConfigsToServer()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'getServiceConfigData', Em.K);
      controller.saveConfigsToServer([1]);
      this.args = testHelpers.findAjaxRequest('name', 'common.across.services.configurations');
    });
    afterEach(function () {
      controller.getServiceConfigData.restore();
    });
    it('getServiceConfigData is called with valid data', function () {
      expect(controller.getServiceConfigData.calledWith([1])).to.be.true;
    });
    it('request is sent', function () {
      expect(this.args).exists;
    });
  });

  describe('#getComponentDir()', function () {
    var configs = {
      'hdfs-site': {
        'dfs.name.dir': 'case1',
        'dfs.namenode.name.dir': 'case2',
        'dfs.namenode.checkpoint.dir': 'case3'
      },
      'core-site': {
        'fs.checkpoint.dir': 'case4'
      }
    };

    it('unknown component name', function () {
      expect(controller.getComponentDir(configs, 'COMP1')).to.be.empty;
    });
    it('NAMENODE component', function () {
      expect(controller.getComponentDir(configs, 'NAMENODE')).to.equal('case2');
    });
    it('SECONDARY_NAMENODE component', function () {
      expect(controller.getComponentDir(configs, 'SECONDARY_NAMENODE')).to.equal('case3');
    });
  });

  describe('#saveClusterStatus()', function () {
    var mock = {
      saveComponentDir: Em.K,
      saveSecureConfigs: Em.K
    };
    beforeEach(function () {
      sinon.stub(App.clusterStatus, 'setClusterStatus', Em.K);
      sinon.stub(App.router, 'get', function() {
        return mock;
      });
      sinon.spy(mock, 'saveComponentDir');
      sinon.spy(mock, 'saveSecureConfigs');
    });
    afterEach(function () {
      App.clusterStatus.setClusterStatus.restore();
      App.router.get.restore();
      mock.saveSecureConfigs.restore();
      mock.saveComponentDir.restore();
    });

    it('componentDir undefined and secureConfigs is empty', function () {
      expect(controller.saveClusterStatus([], null)).to.be.false;
    });
    it('componentDir defined and secureConfigs is empty', function () {
      expect(controller.saveClusterStatus([], 'dir1')).to.be.true;
      expect(mock.saveComponentDir.calledWith('dir1')).to.be.true;
      expect(mock.saveSecureConfigs.calledWith([])).to.be.true;
    });
    it('componentDir undefined and secureConfigs has data', function () {
      expect(controller.saveClusterStatus([1], null)).to.be.true;
      expect(mock.saveComponentDir.calledWith(null)).to.be.true;
      expect(mock.saveSecureConfigs.calledWith([1])).to.be.true;
    });
    it('componentDir defined and secureConfigs has data', function () {
      expect(controller.saveClusterStatus([1], 'dir1')).to.be.true;
      expect(mock.saveComponentDir.calledWith('dir1')).to.be.true;
      expect(mock.saveSecureConfigs.calledWith([1])).to.be.true;
    });
  });

  describe('#onSaveConfigs()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'onTaskCompleted', Em.K);
    });
    afterEach(function () {
      controller.onTaskCompleted.restore();
    });

    it('onTaskCompleted called once', function () {
      controller.onSaveConfigs();
      expect(controller.onTaskCompleted.calledOnce).to.be.true;
    });
  });

  describe('#startZooKeeperServers()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'updateComponent', Em.K);
    });
    afterEach(function () {
      controller.updateComponent.restore();
    });

    it('updateComponent called with valid arguments', function () {
      controller.set('content.masterComponentHosts', [{
        component: 'ZOOKEEPER_SERVER',
        hostName: 'host1'
      }]);
      controller.startZooKeeperServers();
      expect(controller.updateComponent.calledWith('ZOOKEEPER_SERVER', ['host1'], 'ZOOKEEPER', 'Start')).to.be.true;
    });
  });

  describe('#startNameNode()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'updateComponent', Em.K);
      controller.set('content.masterComponentHosts', [
        {
          component: 'NAMENODE',
          hostName: 'host1'
        },
        {
          component: 'NAMENODE',
          hostName: 'host2'
        }
      ]);
    });
    afterEach(function () {
      controller.updateComponent.restore();
    });

    it('reassign host does not match current', function () {
      controller.set('content.reassignHosts.source', 'host3');
      controller.set('content.reassignHosts.target', 'host2');
      controller.startNameNode();
      expect(controller.updateComponent.getCall(0).args[0]).to.be.equal('NAMENODE');
      expect(controller.updateComponent.getCall(0).args[1][0]).to.be.equal('host1');
      expect(controller.updateComponent.getCall(0).args[2]).to.be.equal('HDFS');
      expect(controller.updateComponent.getCall(0).args[3]).to.be.equal('Start');
    });

    it('reassign host matches current', function () {
      controller.set('content.reassignHosts.target', 'host2');
      controller.startNameNode();
      expect(controller.updateComponent.calledWith('NAMENODE', ['host1'], 'HDFS', 'Start')).to.be.true;
    });
  });

  describe('#startServices()', function () {
    beforeEach(function () {
      sinon.stub(App.router, 'get').returns({"skip.service.checks": "false"});
      controller.startServices();
      this.args = testHelpers.findAjaxRequest('name', 'common.services.update');
    });
    afterEach(function () {
      App.router.get.restore();
    });
    it('request is sent', function () {
      expect(this.args).exists;
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

  describe('#done()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'removeObserver', Em.K);
      sinon.stub(App.router, 'send', Em.K);
    });
    afterEach(function () {
      controller.removeObserver.restore();
      App.router.send.restore();
    });

    it('submit disabled', function () {
      controller.set('isSubmitDisabled', true);
      controller.done();
      expect(App.router.send.called).to.be.false;
    });
    it('submit enabled and does not have manual steps', function () {
      controller.set('isSubmitDisabled', false);
      controller.set('content.hasManualSteps', false);
      controller.done();
      expect(controller.removeObserver.calledWith('tasks.@each.status', controller, 'onTaskStatusChange')).to.be.true;
      expect(App.router.send.calledWith('complete')).to.be.true;
    });
    it('submit enabled and has manual steps', function () {
      controller.set('isSubmitDisabled', false);
      controller.set('content.hasManualSteps', true);
      controller.done();
      expect(controller.removeObserver.calledWith('tasks.@each.status', controller, 'onTaskStatusChange')).to.be.true;
      expect(App.router.send.calledWith('next')).to.be.true;
    });
  });

  describe('#getServiceConfigData()', function () {
    var services = [];
    var stackServices = [];
    beforeEach(function () {
      sinon.stub(App.Service, 'find', function () {
        return services;
      });
      sinon.stub(App.StackService, 'find', function () {
        return stackServices;
      });
    });
    afterEach(function () {
      App.Service.find.restore();
      App.StackService.find.restore();
    });

    it('No services', function () {
      services = [];
      controller.set('content.reassign.component_name', 'COMP1');
      expect(controller.getServiceConfigData({})).to.eql([]);
    });
    it('No services in stackServices', function () {
      services = [Em.Object.create({serviceName: 'S1'})];
      stackServices = [];
      controller.set('content.reassign.component_name', 'COMP1');
      expect(controller.getServiceConfigData({}, {})).to.eql([]);
    });
    it('Services in stackServices, but configTypesRendered is empty', function () {
      services = [Em.Object.create({serviceName: 'S1'})];
      stackServices = [Em.Object.create({
        serviceName: 'S1',
        configTypesRendered: {}
      })];
      controller.set('content.reassign.component_name', 'COMP1');
      expect(controller.getServiceConfigData({}, {})[0]).to.equal("{\"Clusters\":{\"desired_config\":[]}}");
    });
    it('Services in stackServices, and configTypesRendered has data, but configs is empty', function () {
      services = [Em.Object.create({serviceName: 'S1'})];
      stackServices = [
        Em.Object.create({
          serviceName: 'S1',
          configTypesRendered: {'type1': {}}
        })
      ];
      controller.set('content.reassign.component_name', 'COMP1');
      expect(controller.getServiceConfigData({}, {})[0]).to.equal("{\"Clusters\":{\"desired_config\":[]}}");
    });
    it('Services in stackServices, and configTypesRendered has data, and configs present', function () {
      services = [Em.Object.create({serviceName: 'S1'})];
      stackServices = [
        Em.Object.create({
          serviceName: 'S1',
          configTypesRendered: {'type1': {}}
        })
      ];
      var configs = {
        'type1': {
          'prop1': 'value1'
        }
      };
      controller.set('content.reassign.component_name', 'COMP1');
      expect(JSON.parse(controller.getServiceConfigData(configs, {})[0]).Clusters.desired_config.length).to.equal(1);
    });
  });

  describe('#cleanMySqlServer()', function () {
    beforeEach(function() {
      sinon.stub(App.HostComponent, 'find', function() {
        return Em.A([
          Em.Object.create({
            'componentName': 'MYSQL_SERVER',
            'hostName': 'host1'
          })
        ]);
      });
    });
    afterEach(function() {
      App.HostComponent.find.restore();
    });

    it('component_name is C1', function () {
      controller.set('content.reassign.component_name', 'C1');
      controller.cleanMySqlServer();
      var args = testHelpers.findAjaxRequest('name', 'service.mysql.clean');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].data).to.be.eql({
        host: 'host1'
      });
    });

    it('component_name is MYSQL_SERVER', function () {
      controller.set('content.reassign.component_name', 'MYSQL_SERVER');
      controller.set('content.reassignHosts.target', 'host2');
      controller.cleanMySqlServer();
      var args = testHelpers.findAjaxRequest('name', 'service.mysql.clean');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].data).to.be.eql({
        host: 'host2'
      });
    });
  });

  describe('#configureMySqlServer()', function () {
    beforeEach(function() {
      sinon.stub(App.HostComponent, 'find', function() {
        return Em.A([
          Em.Object.create({
            'componentName': 'MYSQL_SERVER',
            'hostName': 'host1'
          })
        ]);
      });
    });
    afterEach(function() {
      App.HostComponent.find.restore();
    });

    it('component_name is C1', function () {
      controller.set('content.reassign.component_name', 'C1');
      controller.configureMySqlServer();
      var args = testHelpers.findAjaxRequest('name', 'service.mysql.configure');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].data).to.be.eql({
        host: 'host1'
      });
    });

    it('component_name is MYSQL_SERVER', function () {
      controller.set('content.reassign.component_name', 'MYSQL_SERVER');
      controller.set('content.reassignHosts.target', 'host2');
      controller.configureMySqlServer();
      var args = testHelpers.findAjaxRequest('name', 'service.mysql.configure');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].data).to.be.eql({
        host: 'host2'
      });
    });
  });

  describe("#startRequiredServices()", function() {
    beforeEach(function () {
      sinon.stub(controller, 'startServices', Em.K);
    });
    afterEach(function () {
      controller.startServices.restore();
    });
    it("component has related services", function() {
      controller.set('content.reassign.component_name', 'HISTORYSERVER');
      controller.startRequiredServices();
      expect(controller.startServices.calledWith(false, ['MAPREDUCE2', 'HIVE', 'PIG', 'OOZIE'], true)).to.be.true;
    });
    it("component does not have related services", function() {
      controller.set('content.reassign.component_name', 'C1');
      controller.startRequiredServices();
      expect(controller.startServices.calledWith(true)).to.be.true;
    });
  });

  describe("#startMySqlServer()", function() {
    beforeEach(function () {
      sinon.stub(App.HostComponent, 'find').returns([
        Em.Object.create({
          componentName: 'MYSQL_SERVER',
          hostName: 'host1'
        })
      ]);
    });
    afterEach(function () {
      App.HostComponent.find.restore();
    });
    it("valid request is sent", function() {
      controller.startMySqlServer();
      var args = testHelpers.findAjaxRequest('name', 'common.host.host_component.update');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].data).to.be.eql({
        context: "Start MySQL Server",
        hostName: 'host1',
        serviceName: "HIVE",
        componentName: "MYSQL_SERVER",
        HostRoles: {
          state: "STARTED"
        }
      });
    });
  });

  describe("#restartMySqlServer()", function() {
    beforeEach(function () {
      sinon.stub(App.HostComponent, 'find').returns([
        Em.Object.create({
          componentName: 'MYSQL_SERVER',
          hostName: 'host1'
        })
      ]);
    });
    afterEach(function () {
      App.HostComponent.find.restore();
    });
    it("valid request is sent", function() {
      controller.set('content', Em.Object.create({
        cluster: Em.Object.create({
          name: 'cl1'
        })
      }));
      controller.restartMySqlServer();
      var args = testHelpers.findAjaxRequest('name', 'restart.hostComponents');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].data).to.be.eql({
        context: 'Restart MySql Server',
        resource_filters: [{
          component_name: "MYSQL_SERVER",
          hosts: 'host1',
          service_name: "HIVE"
        }],
        operation_level: {
          level: "HOST_COMPONENT",
          cluster_name: 'cl1',
          service_name: "HIVE",
          hostcomponent_name: "MYSQL_SERVER"
        }
      });
    });
  });

  describe("#startNewMySqlServer()", function() {

    beforeEach(function () {
      controller.set('content', Em.Object.create({
        reassignHosts: Em.Object.create({
          target: 'host1'
        })
      }));
      controller.startNewMySqlServer();
      this.args = testHelpers.findAjaxRequest('name', 'common.host.host_component.update');
    });

    it('valid request is sent', function() {
      expect(this.args[0]).exists;
      expect(this.args[0].sender).to.be.eql(controller);
      expect(this.args[0].data).to.be.eql({
        context: "Start MySQL Server",
        hostName: 'host1',
        serviceName: "HIVE",
        componentName: "MYSQL_SERVER",
        HostRoles: {
          state: "STARTED"
        }
      });
    });
  });

  describe.skip("#prepareDBCheckAction()", function() {
    beforeEach(function () {
      sinon.stub(App.router, 'get').returns({
        'jdk_location': 'jdk_location',
        'jdk.name': 'jdk.name',
        'java.home': 'java.home'
      });
      sinon.stub(controller, 'getConnectionProperty').returns('prop1');
      controller.set('content.reassignHosts', Em.Object.create({target: 'host1'}));
      controller.reopen({
        dbType: 'type1',
        requiredProperties: [],
        preparedDBProperties: {}
      });
      controller.prepareDBCheckAction();
      this.args = testHelpers.findAjaxRequest('name', 'cluster.custom_action.create');
    });
    afterEach(function () {
      App.router.get.restore();
      controller.getConnectionProperty.restore();
    });
    it('valid request is sent', function () {
      expect(this.args[0]).exists;
      expect(this.args[0].data).to.eql({
        requestInfo: {
          "context": "Check host",
          "action": "check_host",
          "parameters": {
            "db_name": "type1",
            "jdk_location": "jdk_location",
            "jdk_name": "jdk.name",
            "java_home": "java.home",
            "threshold": 60,
            "ambari_server_host": "",
            "check_execute_list": "db_connection_check"
          }
        },
        filteredHosts: ['host1']
      });
    });
  });

  describe('#setDependentHostComponents', function() {
    beforeEach(function() {
      sinon.stub(App.Host, 'find').returns(Em.Object.create({
        hostComponents: [
          Em.Object.create({
            componentName: 'C1'
          })
        ]
      }));
      sinon.stub(App.Service, 'find').returns([
        Em.Object.create({
          serviceName: 'S1'
        }),
        Em.Object.create({
          serviceName: 'S2'
        })
      ]);
      sinon.stub(App.StackServiceComponent, 'find').returns(Em.Object.create({
        dependencies: [
          Em.Object.create({
            componentName: 'C1',
            serviceName: 'S1',
            scope: 'host'
          }),
          Em.Object.create({
            componentName: 'C2',
            serviceName: 'S2',
            scope: 'host'
          }),
          Em.Object.create({
            componentName: 'C3',
            serviceName: 'S3',
            scope: 'host'
          })
        ]
      }));
    });
    afterEach(function() {
      App.Host.find.restore();
      App.Service.find.restore();
      App.StackServiceComponent.find.restore();
    });

    it('should set dependentHostComponents', function() {
      controller.setDependentHostComponents();
      expect(controller.get('dependentHostComponents')).to.be.eql(['C2']);
    });
  });
});
