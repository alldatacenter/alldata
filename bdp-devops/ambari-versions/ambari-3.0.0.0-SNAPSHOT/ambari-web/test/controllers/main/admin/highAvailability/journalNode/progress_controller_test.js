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
require('controllers/main/admin/highAvailability/journalNode/progress_controller');


describe('App.ManageJournalNodeProgressPageController', function () {
  var controller;

  beforeEach(function() {
    controller = App.ManageJournalNodeProgressPageController.create();
  });

  describe('#reconfigureSites', function() {

    beforeEach(function() {
      sinon.stub(App, 'dateTime').returns(1);
    });

    afterEach(function() {
      App.dateTime.restore();
    });

    it('should return array of sites', function() {
      var siteNames = ['site1', 'site2'];
      var data = {items: [
        {
          type: 'site1',
          properties: {},
          properties_attributes: {}
        }
      ]};
      expect(controller.reconfigureSites(siteNames, data, 'note')).to.be.eql([
        {
          "properties": {},
          "properties_attributes": {},
          "service_config_version_note": "note",
          "type": "site1"
        },
        {
          "properties": undefined,
          "service_config_version_note": "note",
          "type": "site2"
        }
      ]);
    });
  });
});
