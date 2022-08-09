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

describe('App.ServicesChecksView', function () {
  var view;
  beforeEach(function () {
    view = App.ServicesChecksView.create({check: {failed_on: ['test1', 'test2']}});
  });

  describe('#isSmokeTestDisabled', function () {
    it('should set content of mainServiceItemController and get isSmokeTestDisabled', function () {
      var mock = Em.Object.create({isSmokeTestDisabled: true });
      var service = {};
      sinon.stub(App.router, 'get').returns(mock);
      expect(view.isSmokeTestDisabled(service)).to.be.equal(true);
      expect(mock.get('content')).to.be.eql(service);
      App.router.get.restore();
    });
  });

  describe('#runSmokeTest', function () {
    it('should call runSmokeTest of mainServiceItemController with proper params', function () {
      var service = {};
      var controller = App.router.get('mainServiceItemController');
      sinon.stub(controller, 'runSmokeTest');
      view.runSmokeTest({context: service});
      expect(controller.runSmokeTest.calledWith(service)).to.be.equal(true);
      controller.runSmokeTest.restore();
    });
  });
});