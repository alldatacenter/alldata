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

require('controllers/main/service/reassign/step2_controller');
require('models/host_component');

describe('App.ReassignMasterWizardStep2Controller', function () {


  var controller = App.ReassignMasterWizardStep2Controller.create({
    content: Em.Object.create({
      reassign: Em.Object.create({}),
      services: []
    }),
    renderComponents: Em.K,
    multipleComponents: []
  });

  describe('#customClientSideValidation', function () {
    var hostComponents = [];

    beforeEach(function () {
      sinon.stub(App.HostComponent, 'find', function () {
        return hostComponents;
      });
    });
    afterEach(function () {
      App.HostComponent.find.restore();
    });
    it('No host-components, reassigned equal 0', function () {
      expect(controller.customClientSideValidation()).to.be.false;
    });
    it('Reassign component match existed components, reassigned equal 0', function () {
      controller.set('content.reassign.component_name', 'COMP1');
      hostComponents = [Em.Object.create({
        componentName: 'COMP1',
        hostName: 'host1'
      })];
      controller.set('servicesMasters', [{
        component_name: 'COMP1',
        selectedHost: 'host1'
      }]);

      expect(controller.customClientSideValidation()).to.be.false;
    });
    it('Reassign component do not match existed components, reassigned equal 1', function () {
      controller.set('content.reassign.component_name', 'COMP1');
      hostComponents = [Em.Object.create({
        componentName: 'COMP1',
        hostName: 'host1'
      })];
      controller.set('servicesMasters', []);

      expect(controller.customClientSideValidation()).to.be.true;
    });
    it('Reassign component do not match existed components, reassigned equal 2', function () {
      controller.set('content.reassign.component_name', 'COMP1');
      hostComponents = [
        Em.Object.create({
          componentName: 'COMP1',
          hostName: 'host1'
        }),
        Em.Object.create({
          componentName: 'COMP1',
          hostName: 'host2'
        })
      ];
      controller.set('servicesMasters', []);

      expect(controller.customClientSideValidation()).to.be.false;
    });

    it('submitDisabled is already true', function () {
      expect(controller.customClientSideValidation()).to.be.false;
    });
  });

  describe("#mastersToShow", function() {
    it("should be like array with `content.reassign.component_name`", function() {
      controller.set('content.reassign.component_name', 'C1');
      controller.propertyDidChange('mastersToShow');
      expect(controller.get('mastersToShow')).to.eql(['C1']);
    });
  });

  describe("#mastersToMove", function() {
    it("should be like array with `content.reassign.component_name`", function() {
      controller.set('content.reassign.component_name', 'C1');
      controller.propertyDidChange('mastersToMove');
      expect(controller.get('mastersToMove')).to.eql(['C1']);
    });
  });

  describe("#additionalHostsList", function () {
    beforeEach(function () {
      sinon.stub(App.HostComponent, 'find').returns([Em.Object.create({
        componentName: 'C1',
        hostName: 'host1'
      })]);
    });
    afterEach(function () {
      App.HostComponent.find.restore();
    });
    it("servicesMastersToShow empty", function () {
      controller.reopen({
        servicesMastersToShow: []
      });
      controller.set('content.reassign.component_name', 'C1');
      controller.propertyDidChange('additionalHostsList');
      expect(controller.get('additionalHostsList')).to.be.empty;
    });
    it("servicesMastersToShow has one master", function () {
      controller.reopen({
        servicesMastersToShow: [{}]
      });
      controller.set('content.reassign.component_name', 'C1');
      controller.propertyDidChange('additionalHostsList');
      expect(controller.get('additionalHostsList')).to.eql([{
        label: Em.I18n.t('services.reassign.step2.currentHost'),
        host: 'host1'
      }]);
    });
  });
});
