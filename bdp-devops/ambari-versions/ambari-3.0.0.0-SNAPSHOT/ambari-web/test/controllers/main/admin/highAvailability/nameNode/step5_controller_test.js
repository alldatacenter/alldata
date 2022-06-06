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


describe('App.HighAvailabilityWizardStep5Controller', function() {
  var controller;

  beforeEach(function() {
    controller = App.HighAvailabilityWizardStep5Controller.create({
      content: Em.Object.create()
    });
  });

  describe('#installNameNode', function() {
    beforeEach(function() {
      sinon.stub(controller, 'createInstallComponentTask');
    });
    afterEach(function() {
      controller.createInstallComponentTask.restore();
    });

    it('createInstallComponentTask should be called', function() {
      controller.set('content.masterComponentHosts', [{
        component: 'NAMENODE',
        isInstalled: false,
        hostName: 'host1'
      }]);
      controller.installNameNode();
      expect(controller.createInstallComponentTask.calledWith('NAMENODE', 'host1', 'HDFS')).to.be.true;
    });
  });

  describe('#installJournalNodes', function() {
    beforeEach(function() {
      sinon.stub(controller, 'createInstallComponentTask');
    });
    afterEach(function() {
      controller.createInstallComponentTask.restore();
    });

    it('createInstallComponentTask should be called', function() {
      controller.set('content.masterComponentHosts', [{
        component: 'JOURNALNODE',
        hostName: 'host1'
      }]);
      controller.installJournalNodes();
      expect(controller.createInstallComponentTask.calledWith('JOURNALNODE', ['host1'], 'HDFS')).to.be.true;
    });
  });

  describe('#startJournalNodes', function() {
    beforeEach(function() {
      sinon.stub(controller, 'updateComponent');
    });
    afterEach(function() {
      controller.updateComponent.restore();
    });

    it('updateComponent should be called', function() {
      controller.set('content.masterComponentHosts', [{
        component: 'JOURNALNODE',
        hostName: 'host1'
      }]);
      controller.startJournalNodes();
      expect(controller.updateComponent.calledWith('JOURNALNODE', ['host1'], 'HDFS', 'Start')).to.be.true;
    });
  });

  describe('#disableSNameNode', function() {

    it('App.ajax.send should be called', function() {
      controller.set('content.masterComponentHosts', [
        {
          component: 'SECONDARY_NAMENODE',
          hostName: 'host1'
        }
      ]);
      controller.disableSNameNode();
      var args = testHelpers.findAjaxRequest('name', 'common.host.host_component.passive');
      expect(args[0]).to.be.eql({
        name: 'common.host.host_component.passive',
        sender: controller,
        data: {
          hostName: 'host1',
          passive_state: "ON",
          componentName: 'SECONDARY_NAMENODE'
        },
        success: 'onTaskCompleted',
        error: 'onTaskError'
      });
    });
  });

  describe('#reconfigureHDFS', function() {
    beforeEach(function() {
      sinon.stub(controller, 'reconfigureSecureHDFS');
      sinon.stub(controller, 'updateConfigProperties');
    });
    afterEach(function() {
      controller.reconfigureSecureHDFS.restore();
      controller.updateConfigProperties.restore();
      App.set('isKerberosEnabled', false);
    });

    it('reconfigureSecureHDFS should be called', function() {
      App.set('isKerberosEnabled', true);
      controller.reconfigureHDFS();
      expect(controller.reconfigureSecureHDFS.calledOnce).to.be.true;
      expect(controller.updateConfigProperties.called).to.be.false;
    });

    it('updateConfigProperties should be called', function() {
      controller.set('content.serviceConfigProperties', {});
      controller.reconfigureHDFS();
      expect(controller.reconfigureSecureHDFS.called).to.be.false;
      expect(controller.updateConfigProperties.calledWith({})).to.be.true;
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
      controller.updateConfigProperties({});
      var args = testHelpers.findAjaxRequest('name', 'common.service.configurations');
      expect(args[0]).to.be.eql({
        name: 'common.service.configurations',
        sender: controller,
        data: {
          desired_config: {}
        },
        error: 'onTaskError',
        success: 'installHDFSClients'
      });
    });
  });

  describe('#getRangerSiteNames', function() {
    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns([{serviceName: 'RANGER'}]);
    });
    afterEach(function() {
      App.Service.find.restore();
    });

    it('should return Ranger site names', function() {
      var data = {
        items: [
          {
            type: 'ranger-hdfs-plugin-properties',
            properties: {
              'xasecure.audit.destination.hdfs.dir': 'foo'
            }
          },
          {
            type: 'ranger-hdfs-audit',
            properties: {
              'xasecure.audit.destination.hdfs.dir': 'foo'
            }
          }
        ]
      };
      expect(controller.getRangerSiteNames(data)).to.be.eql([
        'ranger-hdfs-plugin-properties',
        'ranger-hdfs-audit'
      ]);
    });
  });

  describe('#installHDFSClients', function() {
    var mock = {
      saveHdfsClientHosts: sinon.spy()
    };
    beforeEach(function() {
      sinon.stub(controller, 'createInstallComponentTask');
      sinon.stub(App.router, 'get').returns(mock);
      sinon.stub(App.clusterStatus, 'setClusterStatus');
      controller.set('content.masterComponentHosts', [
        Em.Object.create({
          component: 'NAMENODE',
          hostName: 'host1'
        }),
        Em.Object.create({
          component: 'JOURNALNODE',
          hostName: 'host2'
        })
      ]);
      controller.installHDFSClients();
    });
    afterEach(function() {
      controller.createInstallComponentTask.restore();
      App.router.get.restore();
      App.clusterStatus.setClusterStatus.restore();
    });

    it('createInstallComponentTask should be called', function() {
      expect(controller.createInstallComponentTask.calledWith('HDFS_CLIENT', ['host1', 'host2'], 'HDFS')).to.be.true;
    });

    it('saveHdfsClientHosts should be called', function() {
      expect(mock.saveHdfsClientHosts.calledWith(['host1', 'host2'])).to.be.true;
    });

    it('setClusterStatus should be called', function() {
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });
  });

  describe('#reconfigureSecureHDFS', function() {
    var mock = {
      loadConfigsTags: sinon.spy()
    };
    beforeEach(function() {
      sinon.stub(App.router, 'get').returns(mock);
    });
    afterEach(function() {
      App.router.get.restore();
    });

    it('loadConfigsTags should be called', function() {
      controller.reconfigureSecureHDFS();
      expect(mock.loadConfigsTags.calledOnce).to.be.true;
    });
  });

  describe('#onLoadConfigsTags', function() {
    var mock = {
      onLoadConfigsTags: sinon.spy()
    };
    beforeEach(function() {
      sinon.stub(App.router, 'get').returns(mock);
    });
    afterEach(function() {
      App.router.get.restore();
    });

    it('onLoadConfigsTags should be called', function() {
      controller.onLoadConfigsTags({});
      expect(mock.onLoadConfigsTags.calledWith({})).to.be.true;
    });
  });

  describe('#onLoadConfigs', function() {
    var mock = Em.Object.create({
      removeConfigs: sinon.spy(),
      configsToRemove: [{}]
    });
    var data = {
      items: [
        {
          type: 't1',
          properties: {}
        }
      ]
    };
    beforeEach(function() {
      sinon.stub(App.router, 'get').returns(mock);
      sinon.stub(controller, 'updateConfigProperties');
      controller.set('content.serviceConfigProperties', {
        items: [
          {
            type: 't1',
            properties: {
              foo: 'bar'
            }
          }
        ]
      });
      controller.onLoadConfigs(data);
    });
    afterEach(function() {
      App.router.get.restore();
      controller.updateConfigProperties.restore();
    });

    it('removeConfigs should be called', function() {
      expect(mock.removeConfigs.calledWith([{}], {items: [{
        type: 't1',
        properties: {
          foo: 'bar'
        }
      }]})).to.be.true;
    });

    it('updateConfigProperties should be called', function() {
      expect(controller.updateConfigProperties.calledOnce).to.be.true;
    });
  });
});
