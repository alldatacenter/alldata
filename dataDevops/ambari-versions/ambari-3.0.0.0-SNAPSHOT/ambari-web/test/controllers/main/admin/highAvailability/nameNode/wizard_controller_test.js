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

describe('App.HighAvailabilityWizardController', function() {
  var controller;

  beforeEach(function() {
    controller = App.HighAvailabilityWizardController.create();
  });

  describe('#setCurrentStep', function() {
    beforeEach(function() {
      sinon.stub(App.clusterStatus, 'setClusterStatus');
    });
    afterEach(function() {
      App.clusterStatus.setClusterStatus.restore();
    });

    it('App.clusterStatus.setClusterStatus should be called', function() {
      controller.setCurrentStep();
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });
  });

  describe('#getCluster', function() {
    beforeEach(function() {
      sinon.stub(App.router, 'getClusterName').returns('c1');
    });
    afterEach(function() {
      App.router.getClusterName.restore();
    });

    it('should return cluster object', function() {
      controller.set('clusterStatusTemplate', {});
      expect(controller.getCluster()).to.be.eql({
        name: 'c1'
      });
    });
  });

  describe('#saveClusterStatus', function() {
    beforeEach(function() {
      sinon.stub(controller, 'save');
    });
    afterEach(function() {
      controller.save.restore();
    });

    it('cluster status should be saved', function() {
      controller.set('content.cluster', {});
      controller.saveClusterStatus({requestId: [1], oldRequestsId: []});
      expect(controller.get('content.cluster')).to.be.eql({
        requestId: [1],
        oldRequestsId: [1]
      });
      expect(controller.save.calledWith('cluster')).to.be.true;
    });
  });

  describe('#saveHdfsUser', function() {
    beforeEach(function() {
      sinon.stub(App.db, 'setHighAvailabilityWizardHdfsUser');
    });
    afterEach(function() {
      App.db.setHighAvailabilityWizardHdfsUser.restore();
    });

    it('App.db.setHighAvailabilityWizardHdfsUser should be called', function() {
      controller.set('content.hdfsUser', 'hdfs');
      controller.saveHdfsUser();
      expect(App.db.setHighAvailabilityWizardHdfsUser.calledWith('hdfs')).to.be.true;
    });
  });

  describe('#saveTasksStatuses', function() {
    beforeEach(function() {
      sinon.stub(App.db, 'setHighAvailabilityWizardTasksStatuses');
    });
    afterEach(function() {
      App.db.setHighAvailabilityWizardTasksStatuses.restore();
    });

    it('App.db.setHighAvailabilityWizardHdfsUser should be called', function() {
      controller.saveTasksStatuses([{}]);
      expect(App.db.setHighAvailabilityWizardTasksStatuses.calledWith([{}])).to.be.true;
      expect(controller.get('content.tasksStatuses')).to.be.eql([{}]);
    });
  });

  describe('#saveConfigTag', function() {
    beforeEach(function() {
      sinon.stub(App.db, 'setHighAvailabilityWizardConfigTag');
    });
    afterEach(function() {
      App.db.setHighAvailabilityWizardConfigTag.restore();
    });

    it('App.db.setHighAvailabilityWizardConfigTag should be called', function() {
      var tag = {name: 'tag1', value: 'val'};
      controller.saveConfigTag(tag);
      expect(App.db.setHighAvailabilityWizardConfigTag.calledWith(tag)).to.be.true;
      expect(controller.get('content.tag1')).to.be.equal(tag.value);
    });
  });

  describe('#saveHdfsClientHosts', function() {
    beforeEach(function() {
      sinon.stub(App.db, 'setHighAvailabilityWizardHdfsClientHosts');
    });
    afterEach(function() {
      App.db.setHighAvailabilityWizardHdfsClientHosts.restore();
    });

    it('App.db.setHighAvailabilityWizardHdfsClientHosts should be called', function() {
      controller.saveHdfsClientHosts(['host1']);
      expect(App.db.setHighAvailabilityWizardHdfsClientHosts.calledWith(['host1'])).to.be.true;
      expect(controller.get('content.hdfsClientHostNames')).to.be.eql(['host1']);
    });
  });

  describe('#saveServiceConfigProperties', function() {
    beforeEach(function() {
      sinon.stub(controller, 'setDBProperty');
    });
    afterEach(function() {
      controller.setDBProperty.restore();
    });

    it('setDBProperty should be called', function() {
      var ctrl = Em.Object.create({
        serverConfigData: {
          items: [
            {
              type: 'f1',
              properties: {
                'xasecure.audit.destination.hdfs.dir': ''
              }
            }
          ]
        },
        stepConfigs: [
          Em.Object.create({
            configs: [
              Em.Object.create({
                filename: 'f1',
                name: 'xasecure.audit.destination.hdfs.dir',
                value: 'val1'
              }),
              Em.Object.create({
                filename: 'f1',
                name: 'p2',
                value: 'val2'
              })
            ]
          })
        ]
      });
      controller.saveServiceConfigProperties(ctrl);
      expect(JSON.stringify(ctrl.get('serverConfigData'))).to.be.equal(JSON.stringify({
        items: [
          {
            type: 'f1',
            properties: {
              'xasecure.audit.destination.hdfs.dir': 'val1',
              p2: 'val2'
            }
          }
        ]
      }));
      expect(controller.get('content.serviceConfigProperties')).to.be.eql(ctrl.get('serverConfigData'));
      expect(controller.setDBProperty.calledWith('serviceConfigProperties', ctrl.get('serverConfigData'))).to.be.true;
    });
  });

  describe('#loadHdfsClientHosts', function() {
    beforeEach(function() {
      sinon.stub(App.db, 'getHighAvailabilityWizardHdfsClientHosts').returns(['host1']);
    });
    afterEach(function() {
      App.db.getHighAvailabilityWizardHdfsClientHosts.restore();
    });

    it('hdfsClientHostNames should be array of hosts', function() {
      controller.loadHdfsClientHosts();
      expect(controller.get('content.hdfsClientHostNames')).to.be.eql(['host1']);
    });
  });

  describe('#loadConfigTag', function() {
    beforeEach(function() {
      sinon.stub(App.db, 'getHighAvailabilityWizardConfigTag').returns('val');
    });
    afterEach(function() {
      App.db.getHighAvailabilityWizardConfigTag.restore();
    });

    it('tag value should be "val"', function() {
      controller.loadConfigTag('tag1');
      expect(controller.get('content.tag1')).to.be.equal('val');
    });
  });

  describe('#loadHdfsUser', function() {
    beforeEach(function() {
      sinon.stub(App.db, 'getHighAvailabilityWizardHdfsUser').returns('hdfs');
    });
    afterEach(function() {
      App.db.getHighAvailabilityWizardHdfsUser.restore();
    });

    it('hdfsUser value should be "hdfs"', function() {
      controller.loadHdfsUser();
      expect(controller.get('content.hdfsUser')).to.be.equal('hdfs');
    });
  });

  describe('#loadTasksStatuses', function() {
    beforeEach(function() {
      sinon.stub(App.db, 'getHighAvailabilityWizardTasksStatuses').returns([{}]);
    });
    afterEach(function() {
      App.db.getHighAvailabilityWizardTasksStatuses.restore();
    });

    it('tasksStatuses value should be set', function() {
      controller.loadTasksStatuses();
      expect(controller.get('content.tasksStatuses')).to.be.eql([{}]);
    });
  });

  describe('#loadServiceConfigProperties', function() {
    beforeEach(function() {
      sinon.stub(controller, 'getDBProperty').returns([{}]);
    });
    afterEach(function() {
      controller.getDBProperty.restore();
    });

    it('serviceConfigProperties value should be set', function() {
      controller.loadServiceConfigProperties();
      expect(controller.get('content.serviceConfigProperties')).to.be.eql([{}]);
    });
  });

  describe('#saveRequestIds', function() {
    beforeEach(function() {
      sinon.stub(App.db, 'setHighAvailabilityWizardRequestIds');
    });
    afterEach(function() {
      App.db.setHighAvailabilityWizardRequestIds.restore();
    });

    it('setHighAvailabilityWizardRequestIds should be called', function() {
      controller.saveRequestIds([1]);
      expect(controller.get('content.requestIds')).to.be.eql([1]);
      expect(App.db.setHighAvailabilityWizardRequestIds.calledWith([1])).to.be.true;
    });
  });

  describe('#loadRequestIds', function() {
    beforeEach(function() {
      sinon.stub(App.db, 'getHighAvailabilityWizardRequestIds').returns([1]);
    });
    afterEach(function() {
      App.db.getHighAvailabilityWizardRequestIds.restore();
    });

    it('requestIds should be set', function() {
      controller.loadRequestIds();
      expect(controller.get('content.requestIds')).to.be.eql([1]);
    });
  });

  describe('#saveNameServiceId', function() {
    beforeEach(function() {
      sinon.stub(App.db, 'setHighAvailabilityWizardNameServiceId');
    });
    afterEach(function() {
      App.db.setHighAvailabilityWizardNameServiceId.restore();
    });

    it('setHighAvailabilityWizardNameServiceId should be called', function() {
      controller.saveNameServiceId('ha');
      expect(controller.get('content.nameServiceId')).to.be.equal('ha');
      expect(App.db.setHighAvailabilityWizardNameServiceId.calledWith('ha')).to.be.true;
    });
  });

  describe('#loadNameServiceId', function() {
    beforeEach(function() {
      sinon.stub(App.db, 'getHighAvailabilityWizardNameServiceId').returns('ha');
    });
    afterEach(function() {
      App.db.getHighAvailabilityWizardNameServiceId.restore();
    });

    it('nameServiceId value should be set', function() {
      controller.loadNameServiceId();
      expect(controller.get('content.nameServiceId')).to.be.equal('ha');
    });
  });

  describe('#saveTasksRequestIds', function() {
    beforeEach(function() {
      sinon.stub(App.db, 'setHighAvailabilityWizardTasksRequestIds');
    });
    afterEach(function() {
      App.db.setHighAvailabilityWizardTasksRequestIds.restore();
    });

    it('setHighAvailabilityWizardTasksRequestIds should be called', function() {
      controller.saveTasksRequestIds([1]);
      expect(controller.get('content.tasksRequestIds')).to.be.eql([1]);
      expect(App.db.setHighAvailabilityWizardTasksRequestIds.calledWith([1])).to.be.true;
    });
  });

  describe('#loadTasksRequestIds', function() {
    beforeEach(function() {
      sinon.stub(App.db, 'getHighAvailabilityWizardTasksRequestIds').returns([1]);
    });
    afterEach(function() {
      App.db.getHighAvailabilityWizardTasksRequestIds.restore();
    });

    it('tasksRequestIds value should be set', function() {
      controller.loadTasksRequestIds();
      expect(controller.get('content.tasksRequestIds')).to.be.eql([1]);
    });
  });

  describe('#clearAllSteps', function() {
    beforeEach(function() {
      sinon.stub(controller, 'clearInstallOptions');
      sinon.stub(controller, 'getCluster').returns({name: 'c1'});
    });
    afterEach(function() {
      controller.clearInstallOptions.restore();
      controller.getCluster.restore();
    });

    it('clearInstallOptions should be called', function() {
      controller.clearAllSteps();
      expect(controller.clearInstallOptions.calledOnce).to.be.true;
      expect(controller.get('content.cluster')).to.be.eql({name: 'c1'});
    });
  });

  describe('#clearTasksData', function() {
    beforeEach(function() {
      sinon.stub(controller, 'saveTasksStatuses');
      sinon.stub(controller, 'saveRequestIds');
      sinon.stub(controller, 'saveTasksRequestIds');
      controller.clearTasksData();
    });
    afterEach(function() {
      controller.saveRequestIds.restore();
      controller.saveTasksStatuses.restore();
      controller.saveTasksRequestIds.restore();
    });

    it('saveTasksStatuses should be called', function() {
      expect(controller.saveTasksStatuses.calledWith(undefined)).to.be.true;
    });

    it('saveRequestIds should be called', function() {
      expect(controller.saveRequestIds.calledWith(undefined)).to.be.true;
    });

    it('saveTasksRequestIds should be called', function() {
      expect(controller.saveTasksRequestIds.calledWith(undefined)).to.be.true;
    });
  });

  describe('#finish', function() {
    var mock = {
      updateAll: Em.K
    };
    beforeEach(function() {
      sinon.spy(mock, 'updateAll');
      sinon.stub(App.router, 'get').returns(mock);
      sinon.stub(controller, 'resetDbNamespace');
      controller.finish();
    });
    afterEach(function() {
      mock.updateAll.restore();
      App.router.get.restore();
      controller.resetDbNamespace.restore();
    });

    it('resetDbNamespace should be called', function() {
      expect(controller.resetDbNamespace.calledOnce).to.be.true;
    });

    it('updateAll should be called', function() {
      expect(mock.updateAll.calledOnce).to.be.true;
    });
  });
});
