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
require('controllers/main/admin/highAvailability_controller');
require('models/host_component');
require('models/host');
require('utils/ajax/ajax');

describe('App.MainAdminHighAvailabilityController', function () {

  var controller = App.MainAdminHighAvailabilityController.create();

  describe('#enableHighAvailability()', function () {

    var hostComponents = [];

    beforeEach(function () {
      sinon.stub(App.router, 'transitionTo', Em.K);
      sinon.stub(App.HostComponent, 'find', function(){
        return hostComponents;
      });
      sinon.spy(controller, "showErrorPopup");
    });

    afterEach(function () {
      App.router.transitionTo.restore();
      controller.showErrorPopup.restore();
      App.HostComponent.find.restore();
      App.get.restore();
    });

    describe('NAMENODE in INSTALLED state', function () {
      beforeEach(function () {
        hostComponents = [
          Em.Object.create({
            componentName: 'NAMENODE',
            workStatus: 'INSTALLED'
          }),
          Em.Object.create({
            componentName: 'ZOOKEEPER_SERVER',
            workStatus: 'INSTALLED'
          }),
          Em.Object.create({
            componentName: 'ZOOKEEPER_SERVER',
            workStatus: 'INSTALLED'
          }),
          Em.Object.create({
            componentName: 'ZOOKEEPER_SERVER',
            workStatus: 'INSTALLED'
          })
        ];
        sinon.stub(App, 'get', function() {
          return 3;
        });
        this.result = controller.enableHighAvailability();
      });

      it('enableHighAvailability result is false', function () {
        expect(this.result).to.be.false;
      });

      it('showErrorPopup is called once', function () {
        expect(controller.showErrorPopup.calledOnce).to.be.true;
      });

    });

    describe('Cluster has less than 3 ZOOKEPER_SERVER components', function () {
      hostComponents = [
        Em.Object.create({
          componentName: 'NAMENODE',
          workStatus: 'STARTED'
        })
      ];

      beforeEach(function () {
        sinon.stub(App, 'get', function(){
          return 3;
        });
        this.result = controller.enableHighAvailability();
      });

      it('enableHighAvailability result is false', function () {
        expect(this.result).to.be.false;
      });

      it('showErrorPopup is called', function () {
        expect(controller.showErrorPopup.called).to.be.true;
      });

    });

    describe('total hosts number less than 3', function () {
      hostComponents = [
        Em.Object.create({
          componentName: 'NAMENODE',
          workStatus: 'STARTED'
        }),
        Em.Object.create({
          componentName: 'ZOOKEEPER_SERVER',
          workStatus: 'INSTALLED'
        }),
        Em.Object.create({
          componentName: 'ZOOKEEPER_SERVER',
          workStatus: 'INSTALLED'
        }),
        Em.Object.create({
          componentName: 'ZOOKEEPER_SERVER',
          workStatus: 'INSTALLED'
        })
      ];

      beforeEach(function () {
        sinon.stub(App, 'get', function () {
          return 1;
        });
        this.result = controller.enableHighAvailability();
      });

      it('enableHighAvailability result is false', function () {
        expect(this.result).to.be.false;
      });

      it('showErrorPopup is called once', function () {
        expect(controller.showErrorPopup.calledOnce).to.be.true;
      });

    });

    describe('All checks passed', function () {
      beforeEach(function () {
        hostComponents = [
          Em.Object.create({
            componentName: 'NAMENODE',
            workStatus: 'STARTED'
          }),
          Em.Object.create({
            componentName: 'ZOOKEEPER_SERVER',
            workStatus: 'INSTALLED'
          }),
          Em.Object.create({
            componentName: 'ZOOKEEPER_SERVER',
            workStatus: 'INSTALLED'
          }),
          Em.Object.create({
            componentName: 'ZOOKEEPER_SERVER',
            workStatus: 'INSTALLED'
          })
        ];
        sinon.stub(App, 'get', function() {
          return 3;
        });
        this.result = controller.enableHighAvailability();
      });

      it('enableHighAvailability result is true', function () {
        expect(this.result).to.be.true;
      });

      it('user is moved to enable HA', function () {
        expect(App.router.transitionTo.calledWith('main.services.enableHighAvailability')).to.be.true;
      });

      it('showErrorPopup is not called', function () {
        expect(controller.showErrorPopup.calledOnce).to.be.false;
      });

    });
  });

  describe('#joinMessage()', function () {
    it('message is empty', function () {
      var message = [];
      expect(controller.joinMessage(message)).to.be.empty;
    });
    it('message is array from two strings', function () {
      var message = ['yes', 'no'];
      expect(controller.joinMessage(message)).to.equal('yes<br/>no');
    });
    it('message is string', function () {
      var message = 'hello';
      expect(controller.joinMessage(message)).to.equal('<p>hello</p>');
    });
  });

  describe('#manageJournalNode()', function () {

    beforeEach(function () {
      this.mock = sinon.stub(App.HostComponent, 'find');
      sinon.stub(App.router, 'transitionTo', Em.K);
      sinon.spy(controller, "showErrorPopup");
    });

    afterEach(function () {
      App.router.transitionTo.restore();
      controller.showErrorPopup.restore();
      App.HostComponent.find.restore();
    });

    it('should show error popup if there is no NNs', function () {
      this.mock.returns([]);
      var result = controller.manageJournalNode();
      expect(result).to.be.false;
      expect(controller.showErrorPopup.calledOnce).to.be.true;
    });

    it('should show error popup if there is no NNs (2)', function () {
      this.mock.returns([
        Em.Object.create({
          componentName: 'NAMENODE',
          displayNameAdvanced: 'Active NameNode'
        }),
        Em.Object.create({
          componentName: 'NAMENODE'
        })
      ]);
      var result = controller.manageJournalNode();
      expect(result).to.be.false;
      expect(controller.showErrorPopup.calledOnce).to.be.true;
    });

    it('should call transition to wizard if we have both standby and active NNs', function () {
      this.mock.returns([
        Em.Object.create({
          componentName: 'NAMENODE',
          displayNameAdvanced: 'Active NameNode'
        }),
        Em.Object.create({
          componentName: 'NAMENODE',
          displayNameAdvanced: 'Standby NameNode'
        })
      ]);
      var result = controller.manageJournalNode();
      expect(result).to.be.true;
      expect(App.router.transitionTo.calledOnce).to.be.true;
    });
  });

});
