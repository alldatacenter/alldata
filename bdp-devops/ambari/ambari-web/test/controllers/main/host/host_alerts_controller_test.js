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
require('controllers/main/host/host_alerts_controller');

var controller;

function getController() {
  return App.MainHostAlertsController.create();
}

describe('App.MainHostAlertsController', function () {

  beforeEach(function() {
    controller = getController();
  });

  App.TestAliases.testAsComputedAlias(getController(), 'selectedHost', 'App.router.mainHostDetailsController.content', 'object');

  describe("#routeToAlertDefinition()", function () {

    beforeEach(function () {
      sinon.stub(App.AlertDefinition, 'find').returns('alertDefinition');
      sinon.stub(App.router, 'transitionTo', Em.K);
    });
    afterEach(function () {
      App.AlertDefinition.find.restore();
      App.router.transitionTo.restore();
    });

    it("transitionTo is called with valid route and data", function () {
      controller.routeToAlertDefinition({context: 'id'});
      expect(App.router.transitionTo.calledWith('main.alerts.alertDetails', 'alertDefinition')).to.be.true;
    });
  });

});
