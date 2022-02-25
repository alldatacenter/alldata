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

var controller;

describe('App.MainAlertInstancesController', function () {

  beforeEach(function () {
    controller = App.MainAlertInstancesController.create({});
  });

  afterEach(function () {
    clearTimeout(controller.get('updateTimer'));
  });

  describe('#fetchAlertInstances', function () {

    describe('loading instances from correct endpoint', function () {

      it('should load by Host name', function () {

        controller.loadAlertInstancesByHost('host');
        var callArgs = testHelpers.findAjaxRequest('name', 'alerts.instances.by_host')[0];
        expect(callArgs.name).to.equal('alerts.instances.by_host');
        expect(callArgs.data.hostName).to.equal('host');

      });

      it('should load by AlertDefinition id', function () {

        controller.loadAlertInstancesByAlertDefinition('1');
        var callArgs = testHelpers.findAjaxRequest('name', 'alerts.instances.by_definition')[0];
        expect(callArgs.name).to.equal('alerts.instances.by_definition');
        expect(callArgs.data.definitionId).to.equal('1');

      });

      it('should load all', function () {
        controller.loadAlertInstances();
        var callArgs = testHelpers.findAjaxRequest('name', 'alerts.instances')[0];
        expect(callArgs).to.exists;

      });

    });

  });

});