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

var controller;

describe('App.MainAlertDefinitionActionsController', function () {

  beforeEach(function () {
    controller = App.MainAlertDefinitionActionsController.create({});
  });

  describe('#actionHandler', function () {

    beforeEach(function () {
      sinon.stub(controller, 'createNewAlertDefinition', Em.K);
      sinon.stub(controller, 'manageAlertGroups', Em.K);
      sinon.stub(controller, 'manageNotifications', Em.K);
    });

    afterEach(function () {
      controller.createNewAlertDefinition.restore();
      controller.manageAlertGroups.restore();
      controller.manageNotifications.restore();
    });

    it('should call proper methods', function () {

      controller.actionHandler({context: {action: 'createNewAlertDefinition'}});
      controller.actionHandler({context: {action: 'manageAlertGroups'}});
      controller.actionHandler({context: {action: 'manageNotifications'}});
      expect(controller.createNewAlertDefinition.calledOnce).to.be.ok;
      expect(controller.manageAlertGroups.calledOnce).to.be.ok;
      expect(controller.manageNotifications.calledOnce).to.be.ok;

    });

  });

  describe('#manageAlertGroups', function () {

    it('should show modal popup', function () {
      controller.manageAlertGroups();
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });

  });

  describe('#createNewAlertDefinition', function () {

    beforeEach(function () {
      sinon.stub(App.router, 'transitionTo', Em.K);
    });

    afterEach(function () {
      App.router.transitionTo.restore();
    });

    it('should navigate to wizard', function () {

      controller.createNewAlertDefinition();
      expect(App.router.transitionTo.calledWith('alertAdd')).to.be.true;

    });

  });

});