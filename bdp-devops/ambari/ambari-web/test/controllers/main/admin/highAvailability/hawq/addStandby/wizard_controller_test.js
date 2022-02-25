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
require('controllers/main/admin/highAvailability/hawq/addStandby/wizard_controller');

function getController() {
  return App.AddHawqStandbyWizardController.create();
}
var controller;

describe('App.AddHawqStandbyWizardController', function () {

  beforeEach(function () {
    controller = getController();
  });
  
  describe('#loadMap', function() {
    describe('#step1', function() {
      var step;
      beforeEach(function() {
        step = controller.get('loadMap')['1'];
        sinon.stub(controller, 'load');
      });
      afterEach(function() {
        controller.load.restore();
      });
      
      it('load should be called', function() {
        step[0].callback.apply(controller);
        expect(controller.load.calledWith('cluster')).to.be.true;
      });
    });
    describe('#step2', function() {
      var step;
      beforeEach(function() {
        step = controller.get('loadMap')['2'];
        sinon.stub(controller, 'loadHawqHosts');
        sinon.stub(controller, 'loadServicesFromServer');
        sinon.stub(controller, 'loadMasterComponentHosts').returns({done: Em.clb});
        sinon.stub(controller, 'loadConfirmedHosts');
        step[0].callback.apply(controller);
      });
      afterEach(function() {
        controller.loadHawqHosts.restore();
        controller.loadServicesFromServer.restore();
        controller.loadMasterComponentHosts.restore();
        controller.loadConfirmedHosts.restore();
      });
    
      it('loadHawqHosts should be called', function() {
        expect(controller.loadHawqHosts.calledOnce).to.be.true;
      });
      it('loadServicesFromServer should be called', function() {
        expect(controller.loadServicesFromServer.calledOnce).to.be.true;
      });
      it('loadMasterComponentHosts should be called', function() {
        expect(controller.loadMasterComponentHosts.calledOnce).to.be.true;
      });
      it('loadConfirmedHosts should be called', function() {
        expect(controller.loadConfirmedHosts.calledOnce).to.be.true;
      });
    });
    describe('#step4', function() {
      var step;
      beforeEach(function() {
        step = controller.get('loadMap')['4'];
        sinon.stub(controller, 'loadTasksStatuses');
        sinon.stub(controller, 'loadTasksRequestIds');
        sinon.stub(controller, 'loadRequestIds').returns({done: Em.clb});
        sinon.stub(controller, 'loadConfigs');
        step[0].callback.apply(controller);
      });
      afterEach(function() {
        controller.loadTasksStatuses.restore();
        controller.loadTasksRequestIds.restore();
        controller.loadRequestIds.restore();
        controller.loadConfigs.restore();
      });
    
      it('loadTasksStatuses should be called', function() {
        expect(controller.loadTasksStatuses.calledOnce).to.be.true;
      });
      it('loadTasksRequestIds should be called', function() {
        expect(controller.loadTasksRequestIds.calledOnce).to.be.true;
      });
      it('loadRequestIds should be called', function() {
        expect(controller.loadRequestIds.calledOnce).to.be.true;
      });
      it('loadConfigs should be called', function() {
        expect(controller.loadConfigs.calledOnce).to.be.true;
      });
    });
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
  
  describe('#saveHawqHosts', function() {
    beforeEach(function() {
      sinon.stub(controller, 'setDBProperty');
      controller.saveHawqHosts(['host1']);
    });
    afterEach(function() {
      controller.setDBProperty.restore();
    });
    
    it('hosts should be set to DB', function() {
      expect(controller.setDBProperty.calledWith('hawqHosts', ['host1'])).to.be.true;
    });
  
    it('hosts should be set to content', function() {
      expect(controller.get('content.hawqHosts')).to.be.eql(['host1']);
    });
  });
  
  describe('#loadHawqHosts', function() {
    beforeEach(function() {
      sinon.stub(controller, 'getDBProperty').returns(['host1']);
      controller.loadHawqHosts();
    });
    afterEach(function() {
      controller.getDBProperty.restore();
    });
    
    it('hosts should be set to content', function() {
      expect(controller.get('content.hawqHosts')).to.be.eql(['host1']);
    });
  });
  
  describe('#saveConfigs', function() {
    beforeEach(function() {
      sinon.stub(controller, 'setDBProperty');
      controller.saveConfigs([{}]);
    });
    afterEach(function() {
      controller.setDBProperty.restore();
    });
    
    it('configs should be set to DB', function() {
      expect(controller.setDBProperty.calledWith('configs', [{}])).to.be.true;
    });
    
    it('configs should be set to content', function() {
      expect(controller.get('content.configs')).to.be.eql([{}]);
    });
  });
  
  describe('#loadConfigs', function() {
    beforeEach(function() {
      sinon.stub(controller, 'getDBProperty').returns([{}]);
      controller.loadHawqHosts();
    });
    afterEach(function() {
      controller.getDBProperty.restore();
    });
    
    it('configs should be set to content', function() {
      expect(controller.get('content.configs')).to.be.eql([{}]);
    });
  });
  
  describe('#clearAllSteps', function() {
    beforeEach(function() {
      sinon.stub(controller, 'clearInstallOptions');
      sinon.stub(controller, 'getCluster').returns({clusterName: 'c1'});
      controller.clearAllSteps();
    });
    afterEach(function() {
      controller.clearInstallOptions.restore();
      controller.getCluster.restore();
    });
    
    it('clearInstallOptions should be called', function() {
      expect(controller.clearInstallOptions.calledOnce).to.be.true;
    });
  
    it('cluster should be set', function() {
      expect(controller.get('content.cluster')).to.be.eql({clusterName: 'c1'});
    });
  });
  
  describe('#finish', function() {
    var mock = {updateAll: sinon.spy()};
    beforeEach(function() {
      sinon.stub(controller, 'resetDbNamespace');
      sinon.stub(App.router, 'get').returns(mock);
      controller.finish();
    });
    afterEach(function() {
      controller.resetDbNamespace.restore();
      App.router.get.restore();
    });
    
    it('resetDbNamespace should be called', function() {
      expect(controller.resetDbNamespace.calledOnce).to.be.true;
    });
  
    it('updateAll should be called', function() {
      expect(mock.updateAll.called).to.be.true;
    });
  
    it('isFinished should be true', function() {
      expect(controller.get('isFinished')).to.be.true;
    });
  });
  
});
