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
require('controllers/main/admin/highAvailability/journalNode/step6_controller');

describe('App.ManageJournalNodeWizardStep7Controller', function () {
  var controller;

  beforeEach(function () {
    controller = App.ManageJournalNodeWizardStep6Controller.create();
  });

  describe('#startJournalNodes', function () {

    beforeEach(function () {
      sinon.stub(controller, 'updateComponent');
      sinon.stub(App.HostComponent, 'find').returns([
        {
          componentName: 'JOURNALNODE',
          hostName: 'h0'
        },
        {
          componentName: 'JOURNALNODE',
          hostName: 'h1'
        }
      ]);
      controller.startJournalNodes();
    });

    afterEach(function () {
      controller.updateComponent.restore();
      App.HostComponent.find.restore();
    });

    it('updateComponent should be called', function () {
      expect(controller.updateComponent.calledOnce).to.be.true;
    });

    it('updateComponent should be called with correct arguments', function () {
      expect(controller.updateComponent.firstCall.args).to.eql(['JOURNALNODE', ['h0', 'h1'], 'HDFS', 'Start']);
    });
  });
});
