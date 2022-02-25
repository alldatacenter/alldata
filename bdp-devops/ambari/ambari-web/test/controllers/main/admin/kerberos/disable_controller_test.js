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


describe('App.KerberosDisableController', function() {
  var controller;

  beforeEach(function() {
    controller = App.KerberosDisableController.create();
  });

  describe("#loadStep()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'loadTasksStatuses');
      sinon.stub(controller, 'loadTasksRequestIds');
      sinon.stub(controller, 'loadRequestIds');
      controller.loadStep();
    });

    afterEach(function() {
      controller.loadTasksStatuses.restore();
      controller.loadTasksRequestIds.restore();
      controller.loadRequestIds.restore();
    });

    it("controllerName should be set", function() {
      expect(controller.get('content.controllerName')).to.be.equal('kerberosDisableController');
    });

    it("loadTasksStatuses should be called", function() {
      expect(controller.loadTasksStatuses.calledOnce).to.be.true;
    });

    it("loadTasksRequestIds should be called", function() {
      expect(controller.loadTasksRequestIds.calledOnce).to.be.true;
    });

    it("loadRequestIds should be called", function() {
      expect(controller.loadRequestIds.calledOnce).to.be.true;
    });
  });

  describe("#unkerberize()", function () {

    it("App.ajax.send should be called", function() {
      controller.unkerberize();
      var args = testHelpers.findAjaxRequest('name', 'admin.unkerberize.cluster');
      expect(args[0]).to.be.eql({
        name: 'admin.unkerberize.cluster',
        sender: controller,
        success: 'startPolling',
        error: 'onTaskErrorWithSkip'
      });
    });
  });

  describe("#skipTask()", function () {

    it("App.ajax.send should be called", function() {
      controller.skipTask();
      var args = testHelpers.findAjaxRequest('name', 'admin.unkerberize.cluster.skip');
      expect(args[0]).to.be.eql({
        name: 'admin.unkerberize.cluster.skip',
        sender: controller,
        success: 'startPolling',
        error: 'onTaskError'
      });
    });
  });

  describe("#deleteKerberos()", function () {

    it("App.ajax.send should be called", function() {
      controller.deleteKerberos();
      var args = testHelpers.findAjaxRequest('name', 'common.delete.service');
      expect(args[0]).to.be.eql({
        name: 'common.delete.service',
        sender: controller,
        data: {
          serviceName: 'KERBEROS'
        },
        success: 'onTaskCompleted',
        error: 'onTaskCompleted'
      });
    });
  });

  describe("#startAllServices()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'startServices');
    });

    afterEach(function() {
      controller.startServices.restore();
    });

    it("startServices should be called", function() {
      controller.startAllServices();
      expect(controller.startServices.calledOnce).to.be.true;
    });
  });
});
