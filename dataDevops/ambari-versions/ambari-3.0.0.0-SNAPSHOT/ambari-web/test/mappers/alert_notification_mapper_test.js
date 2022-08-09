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
require('mappers/alert_notification_mapper');

describe('App.alertNotificationMapper', function () {
  var mapper = App.alertNotificationMapper,
    model = App.AlertNotification,
    records = model.find();

  before(function () {
    records.clear();
  });

  afterEach(function () {
    records.clear();
  });

  describe('#map', function () {
    var cases = [
      {
        previousAlertNotificationsFullMapBefore: null,
        modelJSONBefore: [],
        json: {},
        previousAlertNotificationsFullMapAfter: {},
        modelContentAfter: [],
        title: 'no cached items, empty JSON'
      },
      {
        previousAlertNotificationsFullMapBefore: {
          '1': {}
        },
        modelJSONBefore: [],
        json: {},
        previousAlertNotificationsFullMapAfter: {
          '1': {}
        },
        modelContentAfter: [],
        title: 'cached items present, empty JSON passed'
      },
      {
        previousAlertNotificationsFullMapBefore: null,
        modelJSONBefore: [],
        json: {
          items: []
        },
        previousAlertNotificationsFullMapAfter: {},
        modelContentAfter: [],
        title: 'no cached items, empty records array'
      },
      {
        previousAlertNotificationsFullMapBefore: {
          '1': {}
        },
        modelJSONBefore: [],
        json: {
          items: []
        },
        previousAlertNotificationsFullMapAfter: {},
        modelContentAfter: [],
        title: 'cached items present, empty records array'
      },
      {
        previousAlertNotificationsFullMapBefore: null,
        modelJSONBefore: [],
        json: {
          items: [
            {
              AlertTarget: {
                id: 1,
                name: 'n1',
                notification_type: 'SNMP',
                description: 'd1',
                global: true,
                enabled: true,
                properties: {
                  p: 'v'
                }
              }
            },
            {
              AlertTarget: {
                id: 2,
                name: 'n2',
                notification_type: 'EMAIL',
                description: 'd2',
                global: false,
                enabled: false,
                alert_states: ['OK', 'WARNING', 'CRITICAL', 'UNKNOWN']
              }
            }
          ]
        },
        previousAlertNotificationsFullMapAfter: {
          '1': {
            id: 1,
            name: 'n1',
            type: 'SNMP',
            description: 'd1',
            global: true,
            enabled: true,
            groups: [1, 2]
          },
          '2': {
            id: 2,
            name: 'n2',
            type: 'EMAIL',
            description: 'd2',
            global: false,
            enabled: false
          }
        },
        modelContentAfter: [
          {
            id: 1,
            name: 'n1',
            type: 'SNMP',
            description: 'd1',
            global: true,
            enabled: true,
            properties: {
              p: 'v'
            }
          },
          {
            id: 2,
            name: 'n2',
            type: 'EMAIL',
            description: 'd2',
            global: false,
            enabled: false,
            alertStates: ['OK', 'WARNING', 'CRITICAL', 'UNKNOWN']
          }
        ],
        title: 'no cached items, non-empty records array'
      },
      {
        previousAlertNotificationsFullMapBefore: {
          '1': {}
        },
        modelJSONBefore: [],
        json: {
          items: []
        },
        previousAlertNotificationsFullMapAfter: {},
        modelContentAfter: [],
        title: 'cached items present, empty records array'
      },
      {
        previousAlertNotificationsFullMapBefore: null,
        modelJSONBefore: [
          {
            id: 3,
            name: 'n3',
            type: 'SNMP',
            description: 'd3',
            global: false,
            enabled: true
          }
        ],
        json: {
          items: [
            {
              AlertTarget: {
                id: 1,
                name: 'n1',
                notification_type: 'SNMP',
                description: 'd1',
                global: true,
                enabled: true,
                properties: {
                  p: 'v'
                }
              }
            },
            {
              AlertTarget: {
                id: 2,
                name: 'n2',
                notification_type: 'EMAIL',
                description: 'd2',
                global: false,
                enabled: false,
                alert_states: ['OK', 'WARNING', 'CRITICAL', 'UNKNOWN']
              }
            }
          ]
        },
        previousAlertNotificationsFullMapAfter: {
          '1': {
            id: 1,
            name: 'n1',
            type: 'SNMP',
            description: 'd1',
            global: true,
            enabled: true,
            groups: [1, 2]
          },
          '2': {
            id: 2,
            name: 'n2',
            type: 'EMAIL',
            description: 'd2',
            global: false,
            enabled: false
          }
        },
        modelContentAfter: [
          {
            id: 3,
            name: 'n3',
            type: 'SNMP',
            description: 'd3',
            global: false,
            enabled: true
          },
          {
            id: 1,
            name: 'n1',
            type: 'SNMP',
            description: 'd1',
            global: true,
            enabled: true,
            properties: {
              p: 'v'
            }
          },
          {
            id: 2,
            name: 'n2',
            type: 'EMAIL',
            description: 'd2',
            global: false,
            enabled: false,
            alertStates: ['OK', 'WARNING', 'CRITICAL', 'UNKNOWN']
          }
        ],
        title: 'previous model records present, no cached items, non-empty records array'
      },
      {
        previousAlertNotificationsFullMapBefore: {
          '1': {
            name: 'n1'
          },
          '2': {
            id: 2,
            name: 'n2',
            type: 'EMAIL',
            description: 'd2',
            global: true,
            enabled: true
          }
        },
        modelJSONBefore: [
          {
            id: 1,
            name: 'n1',
            notification_type: 'EMAIL',
            global: true,
            enabled: true,
            properties: {
              p0: 'v0'
            }
          }
        ],
        json: {
          items: [
            {
              AlertTarget: {
                id: 1,
                name: 'n1',
                notification_type: 'SNMP',
                description: 'd1',
                global: true,
                enabled: true,
                properties: {
                  p: 'v'
                }
              }
            },
            {
              AlertTarget: {
                id: 2,
                name: 'n2',
                notification_type: 'EMAIL',
                description: 'd2',
                global: false,
                enabled: false,
                alert_states: ['OK', 'WARNING', 'CRITICAL', 'UNKNOWN']
              }
            }
          ]
        },
        previousAlertNotificationsFullMapAfter: {
          1: {
            id: 1,
            name: 'n1',
            type: 'SNMP',
            description: 'd1',
            global: true,
            enabled: true,
            groups: [1, 2]
          },
          2: {
            id: 2,
            name: 'n2',
            type: 'EMAIL',
            description: 'd2',
            global: false,
            enabled: false
          }
        },
        modelContentAfter: [
          {
            id: 1,
            name: 'n1',
            type: 'SNMP',
            description: 'd1',
            global: true,
            enabled: true,
            properties: {
              p: 'v'
            }
          },
          {
            id: 2,
            name: 'n2',
            type: 'EMAIL',
            description: 'd2',
            global: false,
            enabled: false,
            alertStates: ['OK', 'WARNING', 'CRITICAL', 'UNKNOWN']
          }
        ],
        title: 'update cache and model'
      }
    ];

    cases.forEach(function (test) {
      describe(test.title, function () {
        before(function () {
          App.cache.alertNotificationsGroupsMap = {
            1: [1, 2]
          };
        });

        beforeEach(function () {
          App.store.safeLoadMany(model, test.modelJSONBefore);
          App.cache.previousAlertNotificationsFullMap = test.previousAlertNotificationsFullMapBefore;
          mapper.map(test.json);
        });

        after(function () {
          App.cache.previousAlertNotificationsFullMap = {};
          App.cache.alertNotificationsGroupsMap = {};
        });

        it('previousAlertNotificationsFullMap', function () {
          expect(App.cache.previousAlertNotificationsFullMap).to.eql(test.previousAlertNotificationsFullMapAfter);
        });

        it('records count', function () {
          expect(records.get('length')).to.equal(test.modelContentAfter.length);
        });

        it('records', function () {
          testHelpers.nestedExpect(test.modelContentAfter, records.toArray());
        });
      });
    });
  });

  describe('#_setPropertiesToEachModel', function () {
    beforeEach(function () {
      App.store.safeLoadMany(model, [
        {
          id: 1,
          properties: {
            p: 'v'
          }
        },
        {
          id: 2
        }
      ]);
    });
    it('should set and update specified property', function () {
      mapper._setPropertiesToEachModel('properties', {
        '1': {
          p1: 'v1'
        },
        '2': {
          p2: 'v2'
        }
      });
      testHelpers.nestedExpect([
        {
          id: 1,
          properties: {
            p1: 'v1'
          }
        },
        {
          id: 2,
          properties: {
            p2: 'v2'
          }
        }
      ], records.toArray());
    });
  });
});
