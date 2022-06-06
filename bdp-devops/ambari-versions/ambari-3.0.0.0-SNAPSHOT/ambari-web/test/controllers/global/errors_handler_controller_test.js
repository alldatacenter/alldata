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

require('controllers/global/errors_handler_controller');

describe('App.ErrorsHandlerController', function () {
  var controller;

  beforeEach(function() {
    controller = App.ErrorsHandlerController.create();
  });

  describe("#loadErrorLogs()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'getUserPref');
    });

    afterEach(function() {
      controller.getUserPref.restore();
    });

    it("getUserPref should be called", function() {
      controller.loadErrorLogs();
      expect(controller.getUserPref.calledWith('errors')).to.be.true;
    });
  });

  describe("#saveErrorLogs()", function () {

    beforeEach(function() {
      localStorage.removeItem('errors');
      sinon.stub(controller, 'postUserPref');
    });

    afterEach(function() {
      controller.postUserPref.restore();
    });

    it("postUserPref should be called", function() {
      controller.saveErrorLogs('err', 'url', 1, 2, {});
      var args = controller.postUserPref.getCall(0).args;
      expect(JSON.stringify(args[1][Object.keys(args[1])[0]])).to.be.equal(JSON.stringify({
        "file": "url",
        "line": 1,
        "col": 2,
        "error": "err"
      }));
      expect(args[0]).to.be.equal('errors');
    });
  });

  describe("#getUserPrefSuccessCallback()", function () {

    it("should set errors to localStorage", function() {
      controller.getUserPrefSuccessCallback({data: {}});
      expect(localStorage.getObject('errors')).to.be.eql({
        data: {}
      })
    });
  });
});
