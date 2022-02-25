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
require('views/main/admin/highAvailability/resourceManager/wizard_view');

describe('App.RMHighAvailabilityWizardView', function () {
  var view;

  beforeEach(function() {
    view = App.RMHighAvailabilityWizardView.create({
      controller: Em.Object.create({
        content: Em.Object.create(),
        setLowerStepsDisable: Em.K,
        setDBProperty: Em.K
      })
    });
  });

  describe("#didInsertElement()", function () {

    beforeEach(function () {
      sinon.spy(view.get('controller'), 'setLowerStepsDisable');
    });
    afterEach(function () {
      view.get('controller').setLowerStepsDisable.restore();
    });

    it("setLowerStepsDisable should not be called", function () {
      view.set('controller.currentStep', 3);
      view.didInsertElement();
      expect(view.get('controller').setLowerStepsDisable.called).to.be.false;
    });

    it("call setLowerStepsDisable", function () {
      view.set('controller.currentStep', 4);
      view.didInsertElement();
      expect(view.get('controller').setLowerStepsDisable.calledOnce).to.be.true;
    });
  });

  describe("#willInsertElement", function() {

    beforeEach(function() {
      sinon.stub(view, 'loadHosts');
    });
    afterEach(function() {
      view.loadHosts.restore();
    });

    it("loadHosts should be called", function() {
      view.willInsertElement();
      expect(view.loadHosts.calledOnce).to.be.true;
      expect(view.get('isLoaded')).to.be.false;
    });
  });

  describe("#loadHosts()", function() {

    it("App.ajax.send should be called", function() {
      view.loadHosts();
      var args = testHelpers.findAjaxRequest('name', 'hosts.high_availability.wizard');
      expect(args).to.be.exist;
    });
  });

  describe("#loadHostsSuccessCallback()", function() {
    var data = {
      items: [
        {
          Hosts: {
            host_name: 'host1'
          }
        }
      ]
    };

    beforeEach(function() {
      sinon.stub(view.get('controller'), 'setDBProperty');
    });
    afterEach(function() {
      view.get('controller').setDBProperty.restore();
    });

    it("setDBProperty should be called", function () {
      view.loadHostsSuccessCallback(data);
      expect(view.get('isLoaded')).to.be.true;
      expect(view.get('controller.content.hosts')).to.be.eql({
        "host1": {
          "name": "host1",
          "bootStatus": "REGISTERED",
          "isInstalled": true
        }
      });
      expect(view.get('controller').setDBProperty.calledWith('hosts', {
        "host1": {
          "name": "host1",
          "bootStatus": "REGISTERED",
          "isInstalled": true
        }
      })).to.be.true;
    });
  });

  describe("#loadHostsErrorCallback()", function() {

    it("isLoaded should be true", function() {
      view.loadHostsErrorCallback();
      expect(view.get('isLoaded')).to.be.true;
    });
  });
});
