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

require('mappers/alert_instances_mapper');

describe('App.alertInstanceMapper', function () {

  var json = {
      "items": [
        {
          "Alert": {
            "component_name": "AMBARI_AGENT",
            "host_name": "c6401.ambari.apache.org",
            "id": 2,
            "instance": null,
            "label": "Host Disk Usage",
            "latest_timestamp": 1415224354954,
            "maintenance_state": "OFF",
            "name": "ambari_agent_disk_usage",
            "original_timestamp": 1414695835400,
            "scope": "HOST",
            "service_name": "AMBARI",
            "state": "OK",
            "text": "Capacity Used: [1.26%, 6.6 GB], Capacity Total: [525.3 GB]"
          }
        },
        {
          "Alert": {
            "component_name": null,
            "host_name": null,
            "id": 3,
            "instance": null,
            "label": "Percent DataNodes Available",
            "latest_timestamp": 1415224362617,
            "maintenance_state": "OFF",
            "name": "datanode_process_percent",
            "original_timestamp": 1414695787466,
            "scope": "SERVICE",
            "service_name": "HDFS",
            "state": "CRITICAL",
            "text": "affected: [1], total: [1]"
          }
        }
      ]
    };

  it('load new records', function () {
    App.alertInstanceMapper.map(json);

    expect(App.AlertInstance.find().content.length).to.equal(2);
  });

  it('delete inexistent record', function () {
    App.alertInstanceMapper.map({
      items: [
        json.items[0]
      ]
    });

    expect(App.AlertInstance.find().content.length).to.equal(1);
  });

  it('model should be empty', function () {
    App.alertInstanceMapper.map({items: []});

    expect(App.AlertInstance.find().content).to.be.empty;
  });
});
