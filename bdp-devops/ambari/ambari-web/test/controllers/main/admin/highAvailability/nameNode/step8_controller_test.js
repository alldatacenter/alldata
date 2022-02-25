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

describe('App.HighAvailabilityWizardStep8Controller', function() {
  var controller;

  beforeEach(function() {
    controller = App.HighAvailabilityWizardStep8Controller.create({
      content: Em.Object.create()
    });
  });

  describe('#done', function() {
    var mock = {
      getKDCSessionState: Em.clb
    };
    beforeEach(function() {
      sinon.stub(App, 'get').returns(mock);
      sinon.spy(mock, 'getKDCSessionState');
      sinon.stub(App.router, 'send');
    });
    afterEach(function() {
      App.get.restore();
      mock.getKDCSessionState.restore();
      App.router.send.restore();
    });

    it('App.router.send should be called', function() {
      controller.done();
      expect(mock.getKDCSessionState.calledOnce).to.be.true;
      expect(App.router.send.calledWith('next')).to.be.true;
    });
  });
});
