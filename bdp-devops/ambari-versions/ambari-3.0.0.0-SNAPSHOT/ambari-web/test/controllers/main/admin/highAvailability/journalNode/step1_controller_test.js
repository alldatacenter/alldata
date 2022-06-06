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


describe('App.ManageJournalNodeWizardStep1Controller', function () {
  var controller;

  beforeEach(function () {
    controller = App.ManageJournalNodeWizardStep1Controller.create({
      getMaxNumberOfMasters: function () {
        return 3;
      },
      updateComponent: Em.K,
      addableComponents: ['C1']
    });
  });

  describe('#renderComponents', function () {

    beforeEach(function () {
      sinon.stub(controller, 'updateJournalNodeInfo');
      sinon.stub(controller, 'showHideJournalNodesAddRemoveControl');
    });

    afterEach(function () {
      controller.updateJournalNodeInfo.restore();
      controller.showHideJournalNodesAddRemoveControl.restore();
    });

    it('updateJournalNodeInfo should be called', function () {
      controller.renderComponents([]);
      expect(controller.updateJournalNodeInfo.calledOnce).to.be.true;
    });

    it('showHideJournalNodesAddRemoveControl should be called', function () {
      controller.renderComponents([]);
      expect(controller.showHideJournalNodesAddRemoveControl.calledOnce).to.be.true;
    });

    it('nextButtonCheckTrigger should be false', function () {
      controller.set('nextButtonCheckTrigger', true);
      controller.renderComponents([]);
      expect(controller.get('nextButtonCheckTrigger')).to.be.false;
    });
  });

  describe('#generateJournalNodeComponents', function () {

    beforeEach(function () {
      sinon.stub(App.HostComponent, 'find').returns([
        Em.Object.create({
          componentName: 'JOURNALNODE',
          service: {
            serviceName: 'S1'
          },
          hostName: 'host1'
        })
      ]);
      sinon.stub(controller, 'createComponentInstallationObject', function (obj) {
        return obj;
      });
    });

    afterEach(function () {
      App.HostComponent.find.restore();
      controller.createComponentInstallationObject.restore();
    });

    it('should return array with JOURNALNODE components', function () {
      expect(controller.generateJournalNodeComponents()).to.be.eql([
        Em.Object.create({
          componentName: 'JOURNALNODE',
          serviceName: 'S1',
          isInstalled: true
        })
      ]);
    });
  });

  describe('#showHideJournalNodesAddRemoveControl', function () {

    it('showAddControl should be false and showRemoveControl should be false for first journalnode', function () {
      var journalNodes = [
        Em.Object.create({component_name: 'JOURNALNODE'}),
        Em.Object.create({component_name: 'JOURNALNODE'}),
        Em.Object.create({component_name: 'JOURNALNODE'})
      ];
      controller.set('selectedServicesMasters', journalNodes);
      controller.showHideJournalNodesAddRemoveControl();
      expect(journalNodes[0].get('showAddControl')).to.be.false;
      expect(journalNodes[0].get('showRemoveControl')).to.be.false;
    });

    it('showAddControl should be false and showRemoveControl should be true for first journalnode', function () {
      var journalNodes = [
        Em.Object.create({component_name: 'JOURNALNODE'}),
        Em.Object.create({component_name: 'JOURNALNODE'}),
        Em.Object.create({component_name: 'JOURNALNODE'}),
        Em.Object.create({component_name: 'JOURNALNODE'})
      ];
      controller.set('selectedServicesMasters', journalNodes);
      controller.showHideJournalNodesAddRemoveControl();
      expect(journalNodes[0].get('showAddControl')).to.be.false;
      expect(journalNodes[0].get('showRemoveControl')).to.be.true;
    });

    it('showAddControl should be true and showRemoveControl should be false for second journalnode', function () {
      var journalNodes = [
        Em.Object.create({component_name: 'JOURNALNODE'}),
        Em.Object.create({component_name: 'JOURNALNODE'})
      ];
      controller.set('selectedServicesMasters', journalNodes);
      controller.showHideJournalNodesAddRemoveControl();
      expect(journalNodes[1].get('showAddControl')).to.be.true;
      expect(journalNodes[1].get('showRemoveControl')).to.be.false;
    });
  });

  describe('#updateJournalNodeInfo', function() {

    beforeEach(function() {
      sinon.stub(App.HostComponent, 'find').returns([
        Em.Object.create({
          componentName: 'JOURNALNODE',
          hostName: 'host1'
        })
      ]);
    });

    afterEach(function() {
      App.HostComponent.find.restore();
    });

    it('isInstalled and showCurrentPrefix should be true for installed hosts', function() {
      var journalNodes = [
        Em.Object.create({
          component_name: 'JOURNALNODE',
          selectedHost: 'host1'
        }),
        Em.Object.create({
          component_name: 'JOURNALNODE',
          selectedHost: 'host2'
        })
      ];
      controller.set('selectedServicesMasters', journalNodes);
      controller.updateJournalNodeInfo();
      expect(journalNodes[0]).to.be.eql(Em.Object.create({
        "component_name": "JOURNALNODE",
        "isInstalled": true,
        "selectedHost": "host1",
        "showCurrentPrefix": true
      }));
      expect(journalNodes[1]).to.be.eql(Em.Object.create({
        component_name: 'JOURNALNODE',
        selectedHost: 'host2'
      }));
    });
  });

  describe('#loadStepCallback', function() {

    beforeEach(function() {
      sinon.stub(controller, 'renderComponents');
      sinon.stub(controller, 'updateComponent');
    });

    afterEach(function() {
      controller.renderComponents.restore();
      controller.updateComponent.restore();
    });

    it('renderComponents should be called', function() {
      controller.loadStepCallback([], controller);
      expect(controller.renderComponents.calledWith([])).to.be.true;
    });

    it('updateComponent should be called', function() {
      controller.set('addableComponents', ['C1']);
      controller.loadStepCallback([], controller);
      expect(controller.updateComponent.calledWith('C1')).to.be.true;
    });

    it('isRecommendationsLoaded should be true', function() {
      controller.loadStepCallback([], controller);
      expect(controller.get('isRecommendationsLoaded')).to.be.true;
    });
  });

  describe('#nextButtonDisabled', function() {

    beforeEach(function() {
      sinon.stub(App.HostComponent, 'find').returns([
        Em.Object.create({
          componentName: 'JOURNALNODE',
          hostName: 'host1'
        })
      ]);
    });

    afterEach(function() {
      App.HostComponent.find.restore();
    });

    it('should return true when hosts identical', function() {
      controller.set('selectedServicesMasters', [
        Em.Object.create({
          component_name: 'JOURNALNODE',
          selectedHost: 'host1'
        })
      ]);
      controller.propertyDidChange('nextButtonDisabled');
      expect(controller.get('nextButtonDisabled')).to.be.true;
    });

    it('should return false when hosts have been changed', function() {
      controller.set('selectedServicesMasters', [
        Em.Object.create({
          component_name: 'JOURNALNODE',
          selectedHost: 'host2'
        })
      ]);
      controller.propertyDidChange('nextButtonDisabled');
      expect(controller.get('nextButtonDisabled')).to.be.false;
    });
  });
});
