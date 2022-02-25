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

describe('App.NameNodeFederationWizardController', function () {
  var controller;
  beforeEach(function () {
    controller = App.NameNodeFederationWizardController.create();
  });

  after(function () {
    controller.destroy();
  });

  describe('#setCurrentStep', function () {
    it('should set cluster status', function () {
      sinon.stub(App.clusterStatus, 'setClusterStatus');
      controller.set('content', {cluster: {name: 'test'}});
      controller.setCurrentStep();
      expect(App.clusterStatus.setClusterStatus.calledWith({
        clusterName: 'test',
        wizardControllerName: 'nameNodeFederationWizardController',
        localdb: App.db.data
      })).to.be.true;
      App.clusterStatus.setClusterStatus.restore();
    });
  });

  describe('#saveNameServiceId', function () {
    it('should set db property and controller property to param', function () {
      sinon.stub(controller, 'setDBProperty');
      controller.saveNameServiceId(1);
      expect(controller.get('content.nameServiceId')).to.equal(1);
      expect(controller.setDBProperty.calledWith('nameServiceId', 1)).to.be.true;
      controller.setDBProperty.restore();
    });
  });

  describe('#loadNameServiceId', function () {
    it('should get version from db and set as local prop', function () {
      sinon.stub(controller, 'getDBProperty').returns(2);
      controller.loadNameServiceId();
      expect(controller.get('content.nameServiceId')).to.equal(2);
      controller.getDBProperty.restore();
    });
  });

  describe('#saveServiceConfigProperties', function () {
    it('should build serverConfig data', function() {
      var mock = Ember.Object.create({
        stepConfigs: [
          Ember.Object.create({
            configs: [
              Ember.Object.create({filename: 'test1', name: 't1', value: 1}),
              Ember.Object.create({filename: 'test2', name: 't2', value: 2}),
              Ember.Object.create({filename: 'test3', name: 't3', value: 3})
            ]
          })
        ],
        serverConfigData: {
          items: [{type: 'test1', properties: {}}, {type: 'test2', properties: {}}]
        }
      });
      controller.saveServiceConfigProperties(mock);
      expect(controller.get('content.serviceConfigProperties').items[0].properties).to.be.eql({
        't1': 1
      });
      expect(controller.get('content.serviceConfigProperties').items[1].properties).to.be.eql({
        't2': 2
      });
    })
  });

  describe('#clearAllSteps', function() {
    it('should call clearInstallOptions and set current cluster status', function () {
      sinon.stub(controller, 'clearInstallOptions');
      sinon.stub(controller, 'getCluster').returns({someProp: 1});
      controller.clearAllSteps();
      expect(controller.clearInstallOptions.calledOnce).to.be.true;
      expect(controller.get('content.cluster')).to.be.eql({someProp: 1});
      controller.clearInstallOptions.restore();
      controller.getCluster.restore();
    });
  });

  describe('#finish', function() {
    it('should call resetDbNamespace, set finesh flag to true and call updateController.updateAll', function(){
      var mock = {
        updateAll: function () {
        }
      };
      sinon.stub(mock, 'updateAll');
      sinon.stub(App.router, 'get').returns(mock);
      sinon.stub(controller, 'resetDbNamespace');
      controller.finish();
      expect(controller.resetDbNamespace.calledOnce).to.be.true;
      expect(controller.get('isFinished')).to.be.true;
      expect(mock.updateAll.calledOnce).to.be.true;
      App.router.get.restore();
      mock.updateAll.restore();
      controller.resetDbNamespace.restore();
    });
  });
});