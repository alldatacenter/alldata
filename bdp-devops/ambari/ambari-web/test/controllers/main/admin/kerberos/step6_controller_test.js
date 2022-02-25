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

describe('App.KerberosWizardStep6Controller', function() {
  var controller;

  beforeEach(function() {
    controller = App.KerberosWizardStep6Controller.create({ commands: ['stopServices'] });
  });


  describe('#checkComponentsRemoval', function() {

    afterEach(function () {
      Em.tryInvoke(App.get, 'restore');
      Em.tryInvoke(App.Service.find, 'restore');
      Em.tryInvoke(App.HostComponent.find, 'restore');
    });

    var tests = [
      { yarnInstalled: true, doesATSSupportKerberos: false, commands: ['stopServices', 'deleteATS'], ATSInstalled: true},
      { yarnInstalled: false, doesATSSupportKerberos: true, commands: ['stopServices'], ATSInstalled: true},
      { yarnInstalled: false, doesATSSupportKerberos: false, commands: ['stopServices'], ATSInstalled: true},
      { yarnInstalled: true, doesATSSupportKerberos: true, commands: ['stopServices'], ATSInstalled: false},
      { yarnInstalled: true, doesATSSupportKerberos: true, commands: ['stopServices'], ATSInstalled: true}
    ];

    tests.forEach(function(test) {
      var message = 'YARN installed: {0}, ATS supported: {1} list of commands should be {2}'.format(test.yarnInstalled, test.doesATSSupportKerberos, test.commands.toString());
      describe(message, function () {
        beforeEach(function () {
          sinon.stub(App, 'get').withArgs('doesATSSupportKerberos').returns(test.doesATSSupportKerberos);
          sinon.stub(App.Service, 'find').returns(test.yarnInstalled ? [Em.Object.create({serviceName: 'YARN'})] : []);
          sinon.stub(App.HostComponent, 'find').returns(test.ATSInstalled ? [Em.Object.create({componentName: 'APP_TIMELINE_SERVER'})] : []);
          controller.checkComponentsRemoval();
        });

        it('commands are valid', function () {
          expect(controller.get('commands').toArray()).to.eql(test.commands);
        });

      });
    });
  });


  describe("#stopServices()", function () {

    it("App.ajax.send should be called", function() {
      controller.stopServices();
      var args = testHelpers.findAjaxRequest('name', 'common.services.update');
      expect(args[0]).to.be.eql({
        name: 'common.services.update',
        data: {
          context: "Stop services",
          "ServiceInfo": {
            "state": "INSTALLED"
          }
        },
        sender: controller,
        success: 'startPolling',
        error: 'onTaskError'
      });
    });
  });

  describe("#loadStep()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'checkComponentsRemoval');
    });

    afterEach(function() {
      controller.checkComponentsRemoval.restore();
    });

    it("loadStep should be called", function() {
      controller.loadStep();
      expect(controller.checkComponentsRemoval.calledOnce).to.be.true;
    });
  });

  describe("#deleteATS()", function () {

    beforeEach(function() {
      sinon.stub(App.HostComponent, 'find').returns([Em.Object.create({
        componentName: 'APP_TIMELINE_SERVER',
        hostName: 'host1'
      })]);
    });

    afterEach(function() {
      App.HostComponent.find.restore();
    });

    it("deleteATS should be called", function() {
      controller.deleteATS();
      var args = testHelpers.findAjaxRequest('name', 'common.delete.host_component');
      expect(args[0]).to.be.eql({
        name: 'common.delete.host_component',
        sender: controller,
        data: {
          componentName: 'APP_TIMELINE_SERVER',
          hostName: 'host1'
        },
        success: 'onDeleteATSSuccess',
        error: 'onDeleteATSError'
      });
    });
  });

  describe("#onDeleteATSSuccess", function () {

    beforeEach(function() {
      sinon.stub(controller, 'onTaskCompleted');
    });

    afterEach(function() {
      controller.onTaskCompleted.restore();
    });

    it("onDeleteATSSuccess should be called", function() {
      controller.onDeleteATSSuccess();
      expect(controller.onTaskCompleted.calledOnce).to.be.true;
    });
  });

  describe("#onDeleteATSError()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'onDeleteATSSuccess');
    });

    afterEach(function() {
      controller.onDeleteATSSuccess.restore();
    });

    it("onDeleteATSSuccess should not be called", function() {
      controller.onDeleteATSError({responseText: ""});
      expect(controller.onDeleteATSSuccess.called).to.be.false;
    });

    it("onDeleteATSSuccess should be called", function() {
      controller.onDeleteATSError({responseText: "org.apache.ambari.server.controller.spi.NoSuchResourceException"});
      expect(controller.onDeleteATSSuccess.calledOnce).to.be.true;
    });
  });

});
