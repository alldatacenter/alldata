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

App = require('app');

require('controllers/main/service/reassign/step7_controller');
var controller;
var testHelpers = require('test/helpers');

describe('App.ReassignMasterWizardStep7Controller', function () {

  beforeEach(function () {
    controller = App.ReassignMasterWizardStep7Controller.create({
      content: Em.Object.create({
        reassign: Em.Object.create(),
        reassignHosts: Em.Object.create()
      })
    });
  });

  describe('#initializeTasks()', function () {
    it('should set isLoaded to true', function () {
      controller.set('isLoaded', false);

      controller.initializeTasks();
      expect(controller.get('isLoaded')).to.be.true;
    });
  });

  describe("#putHostComponentsInMaintenanceMode()", function() {
    it("no host-components", function() {
      controller.set('hostComponents', []);
      controller.putHostComponentsInMaintenanceMode();
      var args = testHelpers.findAjaxRequest('name', 'common.host.host_component.passive');
      expect(args).not.exists;
      expect(controller.get('multiTaskCounter')).to.equal(0);
    });
    it("one host-component", function() {
      controller.set('hostComponents', ['C1']);
      controller.set('content.reassignHosts.target', 'host1');
      controller.putHostComponentsInMaintenanceMode();
      var args = testHelpers.findAjaxRequest('name', 'common.host.host_component.passive');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].data).to.be.eql({
        hostName: 'host1',
        passive_state: "ON",
        componentName: 'C1'
      });
      expect(controller.get('multiTaskCounter')).to.equal(0);
    });
    it("two host-components", function() {
      controller.set('hostComponents', ['C1', 'C2']);
      controller.putHostComponentsInMaintenanceMode();
      var args = testHelpers.filterAjaxRequests('name', 'common.host.host_component.passive');
      expect(args).to.have.property('length').equal(2);
      expect(controller.get('multiTaskCounter')).to.equal(0);
    });
  });

  describe("#deleteHostComponents()", function() {
    it("no host-components", function() {
      controller.set('hostComponents', []);
      controller.deleteHostComponents();
      var args = testHelpers.findAjaxRequest('name', 'common.delete.host_component');
      expect(args).not.exists;
      expect(controller.get('multiTaskCounter')).to.equal(0);
    });
    it("one host-component", function() {
      controller.set('hostComponents', ['C1']);
      controller.set('content.reassignHosts.target', 'host1');
      controller.deleteHostComponents();
      var args = testHelpers.findAjaxRequest('name', 'common.delete.host_component');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].data).to.be.eql({
        hostName: 'host1',
        componentName: 'C1'
      });
      expect(controller.get('multiTaskCounter')).to.equal(0);
    });
    it("two host-components", function() {
      controller.set('hostComponents', ['C1', 'C2']);
      controller.deleteHostComponents();
      var args = testHelpers.filterAjaxRequests('name', 'common.delete.host_component');
      expect(args).to.have.property('length').equal(2);
      expect(controller.get('multiTaskCounter')).to.equal(0);
    });
  });
});
