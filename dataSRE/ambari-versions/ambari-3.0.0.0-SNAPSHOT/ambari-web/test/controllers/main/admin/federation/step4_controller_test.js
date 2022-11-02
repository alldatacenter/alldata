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

describe('App.NameNodeFederationWizardStep3Controller', function () {
  var controller;
  var mocks = [
    {component: 'NAMENODE', isInstalled: false, hostName: 'test1'},
    {component: 'NAMENODE', isInstalled: false, hostName: 'test2'},
    {component: 'NAMENODE', isInstalled: true, hostName: 'test3'},
    {component: 'TEST', isInstalled: false, hostName: 'test4'}
  ];
  beforeEach(function () {
    controller = App.NameNodeFederationWizardStep4Controller.create();
  });

  after(function () {
    controller.destroy();
  });

  describe('#removeUnneededTasks', function () {
    beforeEach(function () {
      sinon.stub(controller, 'removeTasks');
    });
    afterEach(function () {
      controller.removeTasks.restore();
      App.Service.find.restore();
    });

    it('should call remove tasks with 3 services', function () {
      sinon.stub(App.Service, 'find').returns([{serviceName: 'AMBARI_INFRA_SOLR'}, {serviceName: 'HIVE'}]);
      controller.removeUnneededTasks();
      expect(controller.removeTasks.calledWith(['startInfraSolr', 'startRangerAdmin', 'startRangerUsersync'])).to.be.true;
    });

    it('should not call remove tasks', function () {
      sinon.stub(App.Service, 'find').returns([]);
      controller.removeUnneededTasks();
      expect(controller.removeTasks.calledOnce).to.be.false;
    });

    it('should call remove tasks with one param', function () {
      sinon.stub(App.Service, 'find').returns([{serviceName: 'RANGER'}, {serviceName: 'HIVE'}]);
      controller.removeUnneededTasks();
      expect(controller.removeTasks.calledWith(['startInfraSolr'])).to.be.true;
    });
  });

  describe('#newNameNodeHosts', function () {
    it('Should filter in NAMENODE and not installed hosts and map them to their names', function () {
      controller.set('content.masterComponentHosts', mocks);
      expect(controller.get('newNameNodeHosts')).to.be.eql(['test1', 'test2']);
    });
  });

  describe('#reconfigureServices', function () {
    var defautBody, aditionalBody;
    beforeEach(function () {
      controller.set('content.serviceConfigProperties', {items: []});
      defaultBody = {
        Clusters: {
          desired_config: controller.reconfigureSites(['hdfs-site'], {items: []}, Em.I18n.t('admin.nameNodeFederation.wizard,step4.save.configuration.note'))
        }
      };
      aditionalBody = {
        Clusters: {
          desired_config: controller.reconfigureSites(['ranger-tagsync-site'], {items: []}, Em.I18n.t('admin.nameNodeFederation.wizard,step4.save.configuration.note'))
        }
      };
    });

    afterEach(function () {
      App.Service.find.restore();
    });

    it('should send request with default data when RANGER is not installed', function () {
      sinon.stub(App.Service, 'find').returns([{serviceName: 'HIVE'}]);
      controller.reconfigureServices();
      expect(App.ajax.send.calledWith({
        name: 'common.service.multiConfigurations',
        sender: controller,
        data: {
          configs: [defaultBody]
        },
        error: 'onTaskError',
        success: 'installHDFSClients'
      }));
    });

    it('should send request with default and custom data when RANGER is installed', function () {
      sinon.stub(App.Service, 'find').returns([{serviceName: 'HIVE'}]);
      controller.reconfigureServices();
      expect(App.ajax.send.calledWith({
        name: 'common.service.multiConfigurations',
        sender: controller,
        data: {
          configs: [defaultBody, aditionalBody]
        },
        error: 'onTaskError',
        success: 'installHDFSClients'
      }));
    });
  });

  describe('#installHDFSClients', function () {
    it('should call createInstallComponentTask with proper params', function () {
      sinon.stub(controller, 'createInstallComponentTask');
      sinon.stub(App.HostComponent, 'find').returns([
        {componentName: 'JOURNALNODE', hostName: 'test5'},
        {componentName: 'TEST', hostName: 'test6'},
      ]);
      controller.set('content.masterComponentHosts', mocks);
      controller.installHDFSClients();
      expect(controller.createInstallComponentTask.calledWith(
        'HDFS_CLIENT',
        ['test1', 'test2', 'test3', 'test5'],
        'HDFS'
      )).to.be.true;
      controller.createInstallComponentTask.restore();
      App.HostComponent.find.restore();
    });
  });

  describe('#startJournalNodes', function () {
    it('should call updateComponent with proper params', function () {
      sinon.stub(App.HostComponent, 'find').returns([
        {componentName: 'JOURNALNODE', hostName: 'test5'},
        {componentName: 'TEST', hostName: 'test6'},
      ]);
      sinon.stub(controller, 'updateComponent');
      controller.startJournalNodes();
      expect(controller.updateComponent.calledWith(
        'JOURNALNODE', ['test5'], "HDFS", "Start"
      )).to.be.true;
      App.HostComponent.find.restore();
      controller.updateComponent.restore();
    });
  });

  describe('#startNameNodes', function () {
    it('should call updateComponent with proper params', function () {
      sinon.stub(controller, 'updateComponent');
      controller.set('content.masterComponentHosts', mocks);
      controller.startNameNodes();
      expect(controller.updateComponent.calledWith(
        'NAMENODE', ['test3'], "HDFS", "Start"
      )).to.be.true;
      controller.updateComponent.restore();
    });
  });

  describe('#startZKFCs', function () {
    it('should call updateComponent with proper params', function () {
      sinon.stub(controller, 'updateComponent');
      controller.set('content.masterComponentHosts', mocks);
      controller.startZKFCs();
      expect(controller.updateComponent.calledWith(
        'ZKFC', ['test3'], "HDFS", "Start"
      )).to.be.true;
      controller.updateComponent.restore();
    });
  });

  describe('#formatNameNode', function () {
    it('should send ajax request with first of newNameNodeHosts', function () {
      controller.set('content.masterComponentHosts', mocks);
      controller.formatNameNode();
      expect(App.ajax.send.calledWith({
        name: 'nameNode.federation.formatNameNode',
        sender: controller,
        data: {
          host: 'test1'
        },
        success: 'startPolling',
        error: 'onTaskError'
      })).to.be.true;
    });
  });

  describe('#formatZKFC', function () {
    it('should send ajax request with first of newNameNodeHosts', function () {
      controller.set('content.masterComponentHosts', mocks);
      controller.formatZKFC();
      expect(App.ajax.send.calledWith({
        name: 'nameNode.federation.formatZKFC',
        sender: controller,
        data: {
          host: 'test1'
        },
        success: 'startPolling',
        error: 'onTaskError'
      })).to.be.true;
    });
  });

  describe('#startRangerAdmin', function () {
    it('should call updateComponent with proper params', function () {
      sinon.stub(controller, 'updateComponent');
      sinon.stub(App.HostComponent, 'find').returns([
        {componentName: 'RANGER_ADMIN', hostName: 'test5'},
        {componentName: 'TEST', hostName: 'test6'},
      ]);
      controller.startRangerAdmin();
      expect(controller.updateComponent.calledWith(
        'RANGER_ADMIN', ['test5'], "RANGER", "Start"
      )).to.be.true;
      controller.updateComponent.restore();
      App.HostComponent.find.restore();
    });
  });

  describe('#startRangerUsersync', function () {
    it('should call updateComponent with proper params', function () {
      sinon.stub(controller, 'updateComponent');
      sinon.stub(App.HostComponent, 'find').returns([
        {componentName: 'RANGER_USERSYNC', hostName: 'test5'},
        {componentName: 'TEST', hostName: 'test6'},
      ]);
      controller.startRangerUsersync();
      expect(controller.updateComponent.calledWith(
        'RANGER_USERSYNC', ['test5'], "RANGER", "Start"
      )).to.be.true;
      controller.updateComponent.restore();
      App.HostComponent.find.restore();
    });
  });

  describe('#bootstrapNameNode', function () {
    it('should send ajax request with first of newNameNodeHosts', function () {
      controller.set('content.masterComponentHosts', mocks);
      controller.bootstrapNameNode();
      expect(App.ajax.send.calledWith({
        name: 'nameNode.federation.bootstrapNameNode',
        sender: controller,
        data: {
          host: 'test2'
        },
        success: 'startPolling',
        error: 'onTaskError'
      })).to.be.true;
    });
  });
});