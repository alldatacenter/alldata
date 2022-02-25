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
require('controllers/main/admin/highAvailability_controller');
require('models/host_component');
require('models/host');
require('utils/ajax/ajax');

describe('App.HighAvailabilityProgressPageController', function () {

  var controller = App.HighAvailabilityProgressPageController.create();

  describe('#reconfigureSites()', function () {
    var tests = [
      {
        siteNames: ["site1", "site2"],
        data: {
          items: [
            {
              type: "site1",
              properties: {
                site1_property1: "site1_property1_value",
                site1_property2: "site1_property2_value"
              },
              properties_attributes: {
                final: {
                  site1_property1: "true"
                }
              }
            },
            {
              type: "site2",
              properties: {
                site2_property1: "site2_property1_value",
                site2_property2: "site2_property2_value"
              }
            },
            {
              type: "site3",
              properties: {
                site3_property: "site3_property_value"
              }
            }
          ]
        },
        note: 'note1',
        result: [
          {
            type: "site1",
            properties: {
              site1_property1: "site1_property1_value",
              site1_property2: "site1_property2_value"
            },
            service_config_version_note: 'note1',
            properties_attributes: {
              final: {
                site1_property1: "true"
              }
            }
          },
          {
            type: "site2",
            properties: {
              site2_property1: "site2_property1_value",
              site2_property2: "site2_property2_value"
            },
            service_config_version_note: 'note1'
          }
        ]
      },
      {
        siteNames: ["site1"],
        data: {
          items: [
            {
              type: "site1",
              properties: {
                site1_property1: "site1_property1_value",
                site1_property2: "site1_property2_value"
              },
              properties_attributes: {
                final: {
                  site1_property1: "true"
                }
              }
            }
          ]
        },
        note: 'note2',
        result: [
          {
            type: "site1",
            properties: {
              site1_property1: "site1_property1_value",
              site1_property2: "site1_property2_value"
            },
            service_config_version_note: 'note2',
            properties_attributes: {
              final: {
                site1_property1: "true"
              }
            }
          }
        ]
      }];

    beforeEach(function () {
      sinon.stub(Date.prototype, 'getTime').returns(1);
    });

    afterEach(function () {
      Date.prototype.getTime.restore();
    });

    tests.forEach(function(t, index) {
      it("reconfigures configs after HA #" + (index + 1), function() {
        controller.set('content', t.content);
        expect(controller.reconfigureSites(t.siteNames, t.data, t.note)).to.eql(t.result);
      });
    });
  });

});
