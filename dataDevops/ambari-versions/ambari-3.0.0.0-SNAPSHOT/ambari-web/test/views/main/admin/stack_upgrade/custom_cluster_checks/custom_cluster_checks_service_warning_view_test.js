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

describe('App.servicesWarningView', function () {
  var view;
  beforeEach(function () {
    view = App.servicesWarningView.create({});
  });

  describe('#goToConfigs', function() {
    it('should go to service configs and close all 2 dialogs', function () {
      var service = {};
      var mock = {closeParent: function () {}, onClose: function () {}};
      sinon.stub(mock, 'closeParent');
      sinon.stub(mock, 'onClose');
      sinon.stub(App.router, 'transitionTo');
      sinon.stub(view, 'get').returns(mock);
      view.goToConfigs({context: service});
      expect(App.router.transitionTo.calledWith('services.service.configs', service));
      expect(mock.closeParent.calledOnce).to.be.equal(true);
      expect(mock.onClose.calledOnce).to.be.equal(true);
      App.router.transitionTo.restore();
      view.get.restore();
      mock.closeParent.restore();
      mock.onClose.restore();
    });
  });
});