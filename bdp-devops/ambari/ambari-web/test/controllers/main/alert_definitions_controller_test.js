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

require('controllers/main/alert_definitions_controller');
require('models/alerts/alert_definition');
var testHelpers = require('test/helpers');

var controller;
describe('App.MainAlertDefinitionsController', function() {

  beforeEach(function() {

    controller = App.MainAlertDefinitionsController.create();

  });

  describe('#toggleDefinitionState', function() {

    beforeEach(function() {
      controller.reopen({
        content: [
          App.AlertDefinition.createRecord({id: 1, enabled: true})
        ]
      });
    });

    it('should do ajax-request', function() {
      var alertDefinition = controller.get('content')[0];
      controller.toggleDefinitionState(alertDefinition);
      var args = testHelpers.findAjaxRequest('name', 'alerts.update_alert_definition');
      expect(args).to.exists;
    });

  });

  describe('#isCriticalAlerts', function () {

    beforeEach(function () {
      controller.set('content', Em.A([
        Em.Object.create({summary: {CRITICAL: {count: 0}}}),
        Em.Object.create({summary: {CRITICAL: {}}})
      ]));
    });

    it('if summary is undefined, 0 should be used', function () {
      expect(controller.get('isCriticalAlerts')).to.be.false;
    });

    it('should be true, if some CRITICAL count is greater than 0', function () {
      controller.get('content').pushObject(Em.Object.create({summary: {CRITICAL: {count: 1}}}));
      expect(controller.get('isCriticalAlerts')).to.be.true;
    });

  });

  describe("#toggleState()", function() {

    beforeEach(function() {
      sinon.stub(App, 'showConfirmationFeedBackPopup', Em.clb);
      sinon.stub(controller, 'toggleDefinitionState');
    });
    afterEach(function() {
      App.showConfirmationFeedBackPopup.restore();
      controller.toggleDefinitionState.restore();
    });

    it("toggleDefinitionState should be called", function() {
      var def = Em.Object.create();
      controller.toggleState({context: def});
      expect(App.showConfirmationFeedBackPopup.calledOnce).to.be.true;
      expect(controller.toggleDefinitionState.calledWith(def)).to.be.true;
    });
  });

});
