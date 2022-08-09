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

function getController() {
  return App.KerberosWizardStep8Controller.create({});
}

describe('App.KerberosWizardStep8Controller', function() {
  var controller;

  beforeEach(function() {
    controller = getController();
  });

  describe("#startServices()", function () {

    beforeEach(function() {
      sinon.stub(App.router, 'get').returns({
        "skip.service.checks": "true"
      });
    });

    afterEach(function() {
      App.router.get.restore();
    });

    it("App.ajax.send should be called", function() {
      controller.startServices();
      var args = testHelpers.findAjaxRequest('name', 'common.services.update');
      expect(args[0]).to.be.eql({
        name: 'common.services.update',
        sender: controller,
        data: {
          "context": "Start services",
          "ServiceInfo": {
            "state": "STARTED"
          },
          urlParams: "params/run_smoke_test=false"
        },
        success: 'startPolling',
        error: 'startServicesErrorCallback'
      });
    });
  });

});
