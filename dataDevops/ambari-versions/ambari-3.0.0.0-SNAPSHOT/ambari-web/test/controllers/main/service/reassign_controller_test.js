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
require('models/cluster');
require('controllers/wizard');
require('controllers/main/service/reassign_controller');

describe('App.ReassignMasterController', function () {

  var reassignMasterController;

  beforeEach(function () {
    reassignMasterController = App.ReassignMasterController.create({});
  });

  describe('#totalSteps', function () {

    var cases = [
      {
        componentName: 'ZOOKEEPER_SERVER',
        result: 4
      },
      {
        componentName: 'RESOURCE_MANAGER',
        result: 4
      },
      {
        componentName: 'OOZIE_SERVER',
        result: 6
      },
      {
        componentName: 'APP_TIMELINE_SERVER',
        result: 6
      },
      {
        componentName: 'NAMENODE',
        result: 6
      }
    ];

    cases.forEach(function (c) {
      it('check ' + c.componentName, function () {
        reassignMasterController.set('content.reassign', {'component_name': c.componentName});
        expect(reassignMasterController.get('totalSteps')).to.equal(c.result);
        reassignMasterController.set('content.reassign', {service_id:null});
      });
    });
  });

  describe('#saveMasterComponentHosts', function () {

    var stepController = Em.Object.create({
        selectedServicesMasters: [
          Em.Object.create({
            display_name: 'd0',
            component_name: 'c0',
            selectedHost: 'h0',
            serviceId: 's0'
          }),
          Em.Object.create({
            display_name: 'd1',
            component_name: 'c1',
            selectedHost: 'h1',
            serviceId: 's1'
          })
        ]
      }),
      masterComponentHosts = [
        {
          display_name: 'd0',
          component: 'c0',
          hostName: 'h0',
          serviceId: 's0',
          isInstalled: true
        },
        {
          display_name: 'd1',
          component: 'c1',
          hostName: 'h1',
          serviceId: 's1',
          isInstalled: true
        }
      ];

    beforeEach(function () {
      sinon.stub(App.db, 'setMasterComponentHosts', Em.K);
      sinon.stub(reassignMasterController, 'setDBProperty', Em.K);
      reassignMasterController.saveMasterComponentHosts(stepController);
    });

    afterEach(function () {
      App.db.setMasterComponentHosts.restore();
      reassignMasterController.setDBProperty.restore();
    });

    it('setMasterComponentHosts is called once', function () {
      expect(App.db.setMasterComponentHosts.calledOnce).to.be.true;
    });

    it('setDBProperty is called once', function () {
      expect(reassignMasterController.setDBProperty.calledOnce).to.be.true;
    });

    it('setMasterComponentHosts is called with valid arguments', function () {
      expect(App.db.setMasterComponentHosts.calledWith(masterComponentHosts)).to.be.true;
    });

    it('setDBProperty is called with valid arguments', function () {
      expect(reassignMasterController.setDBProperty.calledWith('masterComponentHosts', masterComponentHosts)).to.be.true;
    });

    it('masterComponentHosts are equal to ' + JSON.stringify(masterComponentHosts), function () {
      expect(reassignMasterController.get('content.masterComponentHosts')).to.eql(masterComponentHosts);
    });

  });
  
  describe('#updateUserConfigs', function() {
    
    it('should update user and group from configs', function() {
      reassignMasterController.set('content.configs', {
        'hadoop-env': {
          'hdfs_user': 'u1'
        },
        'cluster-env': {
          'user_group': 'g1'
        }
      });
      reassignMasterController.updateUserConfigs();
      expect(reassignMasterController.get('content.hdfsUser')).to.be.equal('u1');
      expect(reassignMasterController.get('content.group')).to.be.equal('g1');
      
    });
  });

});
