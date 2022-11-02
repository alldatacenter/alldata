/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

require('mappers/alert_definition_summary_mapper');

describe('App.alertDefinitionSummaryMapper', function () {

  describe('#map', function () {

    var testModels = [
        App.AlertDefinition.createRecord({id: 1, enabled: true, type: 'PORT'}),
        App.AlertDefinition.createRecord({id: 2, enabled: true, type: 'METRICS'}),
        App.AlertDefinition.createRecord({id: 3, enabled: true, type: 'WEB'}),
        App.AlertDefinition.createRecord({id: 4, enabled: true, type: 'AGGREGATE'}),
        App.AlertDefinition.createRecord({id: 5, enabled: true, type: 'SCRIPT'}),
        App.AlertDefinition.createRecord({id: 6, enabled: false, type: 'SCRIPT', summary: {OK: 1}})
      ],
      dataToMap = {
        alerts_summary_grouped: [
          {
            definition_id: 1,
            summary: {
              OK: {
                count: 1,
                original_timestamp: 1,
                maintenance_count: 0,
                latest_text: "Connection failed: [Errno 111] Connection refused to c6407.ambari.apache.org:60000"
              },
              WARNING: {
                count: 1,
                original_timestamp: 2,
                maintenance_count: 0,
                latest_text: "Connection failed: [Errno 111] Connection refused to c6407.ambari.apache.org:60000"
              },
              CRITICAL: {count: 0, original_timestamp: 0, maintenance_count: 1},
              UNKNOWN: {count: 0, original_timestamp: 0, maintenance_count: 0}
            }
          },
          {
            definition_id: 2,
            summary: {
              OK: {
                count: 1,
                original_timestamp: 1,
                maintenance_count: 0,
                latest_text: "HTTP 200 response in 0.000 seconds"
              },
              WARNING: {
                count: 5,
                original_timestamp: 2,
                maintenance_count: 0,
                latest_text: "Connection failed: [Errno 111] Connection refused to c6407.ambari.apache.org:60000"
              },
              CRITICAL: {count: 1, original_timestamp: 1, maintenance_count: 0},
              UNKNOWN: {count: 1, original_timestamp: 3, maintenance_count: 0}
            }
          },
          {
            definition_id: 3,
            summary: {
              OK: {
                count: 1,
                original_timestamp: 1,
                maintenance_count: 0,
                latest_text: "HTTP 200 response in 0.000 seconds"
              },
              WARNING: {count: 2, original_timestamp: 2, maintenance_count: 2},
              CRITICAL: {
                count: 3,
                original_timestamp: 4,
                maintenance_count: 0,
                latest_text: "Connection failed: [Errno 111] Connection refused to c6407.ambari.apache.org:60000"
              },
              UNKNOWN: {count: 4, original_timestamp: 3, maintenance_count: 0}
            }
          },
          {
            definition_id: 4,
            summary: {
              OK: {
                count: 4,
                original_timestamp: 1,
                maintenance_count: 0,
                latest_text: "HTTP 200 response in 0.000 seconds"
              },
              WARNING: {count: 3, original_timestamp: 2, maintenance_count: 0},
              CRITICAL: {count: 2, original_timestamp: 1, maintenance_count: 0},
              UNKNOWN: {
                count: 1,
                original_timestamp: 2,
                maintenance_count: 0,
                latest_text: "Connection failed: [Errno 111] Connection refused to c6407.ambari.apache.org:60000"
              }
            }
          },
          {
            definition_id: 5,
            summary: {
              OK: {
                count: 1,
                original_timestamp: 1,
                maintenance_count: 0,
                latest_text: "Connection failed: [Errno 111] Connection refused to c6407.ambari.apache.org:60000"
              },
              WARNING: {count: 1, original_timestamp: 2, maintenance_count: 0},
              CRITICAL: {count: 1, original_timestamp: 3, maintenance_count: 0},
              UNKNOWN: {count: 1, original_timestamp: 4, maintenance_count: 0}
            }
          }
        ]
      };

    beforeEach(function () {
      sinon.stub(App.AlertDefinition, 'find').returns(testModels);
      App.alertDefinitionSummaryMapper.map(dataToMap);
    });

    afterEach(function () {
      App.AlertDefinition.find.restore();
    });

    it('should map summary info for 1st alert', function () {
      expect(App.AlertDefinition.find().findProperty('id', 1).get('lastTriggered')).to.equal(2);
      expect(App.AlertDefinition.find().findProperty('id', 1).get('summary')).to.eql({
        OK: {
          count: 1,
          maintenanceCount: 0,
          latestText: "Connection failed: [Errno 111] Connection refused to c6407.ambari.apache.org:60000"
        },
        WARNING: {
          count: 1,
          maintenanceCount: 0,
          latestText: "Connection failed: [Errno 111] Connection refused to c6407.ambari.apache.org:60000"
        },
        CRITICAL: {count: 0, maintenanceCount: 1},
        UNKNOWN: {count: 0, maintenanceCount: 0}
      });
    });

    it('should map summary info for 2nd alert', function () {
      expect(App.AlertDefinition.find().findProperty('id', 2).get('lastTriggered')).to.equal(3);
      expect(App.AlertDefinition.find().findProperty('id', 2).get('summary')).to.eql({
        OK: {
          count: 1,
          maintenanceCount: 0,
          latestText: "HTTP 200 response in 0.000 seconds"
        },
        WARNING: {
          count: 5,
          maintenanceCount: 0,
          latestText: "Connection failed: [Errno 111] Connection refused to c6407.ambari.apache.org:60000"
        },
        CRITICAL: {count: 1, maintenanceCount: 0},
        UNKNOWN: {count: 1, maintenanceCount: 0}
      });
    });

    it('should map summary info for 3rd alert', function () {
      expect(App.AlertDefinition.find().findProperty('id', 3).get('lastTriggered')).to.equal(4);
      expect(App.AlertDefinition.find().findProperty('id', 3).get('summary')).to.eql({
        OK: {
          count: 1,
          maintenanceCount: 0,
          latestText: "HTTP 200 response in 0.000 seconds"
        },
        WARNING: {count: 2, maintenanceCount: 2},
        CRITICAL: {
          count: 3,
          maintenanceCount: 0,
          latestText: "Connection failed: [Errno 111] Connection refused to c6407.ambari.apache.org:60000"
        },
        UNKNOWN: {count: 4, maintenanceCount: 0}
      });
    });

    it('should map summary info for 4th alert', function () {
      expect(App.AlertDefinition.find().findProperty('id', 4).get('lastTriggered')).to.equal(2);
      expect(App.AlertDefinition.find().findProperty('id', 4).get('summary')).to.eql({
        OK: {
          count: 4,
          maintenanceCount: 0,
          latestText: "HTTP 200 response in 0.000 seconds"
        },
        WARNING: {count: 3, maintenanceCount: 0},
        CRITICAL: {count: 2, maintenanceCount: 0},
        UNKNOWN: {
          count: 1,
          maintenanceCount: 0,
          latestText: "Connection failed: [Errno 111] Connection refused to c6407.ambari.apache.org:60000"
        }
      });
    });

    it('should map summary info for 5th alert', function () {
      expect(App.AlertDefinition.find().findProperty('id', 5).get('lastTriggered')).to.equal(4);
      expect(App.AlertDefinition.find().findProperty('id', 5).get('summary')).to.eql({
        OK: {
          count: 1,
          maintenanceCount: 0,
          latestText: "Connection failed: [Errno 111] Connection refused to c6407.ambari.apache.org:60000"
        },
        WARNING: {count: 1, maintenanceCount: 0},
        CRITICAL: {count: 1, maintenanceCount: 0},
        UNKNOWN: {count: 1, maintenanceCount: 0}
      });
    });

    it('should clear summary for disabled definitions', function () {
      expect(App.AlertDefinition.find().findProperty('id', 6).get('summary')).to.eql({});
    });

  });

});