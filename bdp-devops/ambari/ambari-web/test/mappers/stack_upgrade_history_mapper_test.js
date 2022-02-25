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

/*eslint-disable */

var App = require('app');

require('mappers/stack_upgrade_history_mapper');

describe('App.stackUpgradeHistoryMapper', function () {

  describe('#map', function () {

    var data = {
        "href" : "http://bdavm079.svl.ibm.com:8080/api/v1/clusters/bi/upgrades?fields=Upgrade",
        "items" : [
          {
            "href" : "http://bdavm079.svl.ibm.com:8080/api/v1/clusters/bi/upgrades/7",
            "Upgrade" : {
              "cluster_name" : "bi",
              "create_time" : 1463779169144,
              "direction" : "UPGRADE",
              "downgrade_allowed" : true,
              "end_time" : 1463779266087,
              "exclusive" : false,
              "associated_version" : "2.3.6.0-3712",
              "pack" : "nonrolling-upgrade-2.4",
              "progress_percent" : 100.0,
              "request_context" : "Upgrading to 2.4.0.0-169",
              "request_id" : 7,
              "request_status" : "ABORTED",
              "skip_failures" : false,
              "skip_service_check_failures" : false,
              "start_time" : 1463779170159,
              "suspended" : false,
              "to_version" : "2.4.0.0-169",
              "type" : "INTERNAL_REQUEST",
              "upgrade_type" : "NON_ROLLING"
            }
          },
          {
            "href" : "http://bdavm079.svl.ibm.com:8080/api/v1/clusters/bi/upgrades/8",
            "Upgrade" : {
              "cluster_name" : "bi",
              "create_time" : 1463779266212,
              "direction" : "DOWNGRADE",
              "downgrade_allowed" : true,
              "end_time" : 1463779299440,
              "exclusive" : false,
              "associated_version" : "2.3.6.0-3712",
              "pack" : "nonrolling-upgrade-2.4",
              "progress_percent" : 100.0,
              "request_context" : "Downgrading to 2.3.6.0-3712",
              "request_id" : 8,
              "request_status" : "COMPLETED",
              "skip_failures" : false,
              "skip_service_check_failures" : false,
              "start_time" : 1463779267220,
              "suspended" : false,
              "to_version" : "2.3.6.0-3712",
              "type" : "INTERNAL_REQUEST",
              "upgrade_type" : "NON_ROLLING"
            }
          },
          {
            "href" : "http://bdavm079.svl.ibm.com:8080/api/v1/clusters/bi/upgrades/9",
            "Upgrade" : {
              "cluster_name" : "bi",
              "create_time" : 1463780699654,
              "direction" : "UPGRADE",
              "downgrade_allowed" : true,
              "end_time" : 1463780757685,
              "exclusive" : false,
              "associated_version" : "2.3.6.0-3712",
              "pack" : "nonrolling-upgrade-2.4",
              "progress_percent" : 100.0,
              "request_context" : "Upgrading to 2.4.0.0-169",
              "request_id" : 9,
              "request_status" : "ABORTED",
              "skip_failures" : false,
              "skip_service_check_failures" : false,
              "start_time" : 1463780700670,
              "suspended" : false,
              "to_version" : "2.4.0.0-169",
              "type" : "INTERNAL_REQUEST",
              "upgrade_type" : "NON_ROLLING"
            }
          },
          {
            "href" : "http://bdavm079.svl.ibm.com:8080/api/v1/clusters/bi/upgrades/10",
            "Upgrade" : {
              "cluster_name" : "bi",
              "create_time" : 1463780757799,
              "direction" : "DOWNGRADE",
              "downgrade_allowed" : true,
              "end_time" : 1463780794009,
              "exclusive" : false,
              "associated_version" : "2.3.6.0-3712",
              "pack" : "nonrolling-upgrade-2.4",
              "progress_percent" : 100.0,
              "request_context" : "Downgrading to 2.3.6.0-3712",
              "request_id" : 10,
              "request_status" : "COMPLETED",
              "skip_failures" : false,
              "skip_service_check_failures" : false,
              "start_time" : 1463780758807,
              "suspended" : false,
              "to_version" : "2.3.6.0-3712",
              "type" : "INTERNAL_REQUEST",
              "upgrade_type" : "NON_ROLLING"
            }
          },
          {
            "href" : "http://bdavm079.svl.ibm.com:8080/api/v1/clusters/bi/upgrades/11",
            "Upgrade" : {
              "cluster_name" : "bi",
              "create_time" : 1463781287967,
              "direction" : "UPGRADE",
              "downgrade_allowed" : true,
              "end_time" : 1463781341452,
              "exclusive" : false,
              "associated_version" : "2.3.6.0-3712",
              "pack" : "nonrolling-upgrade-2.4",
              "progress_percent" : 100.0,
              "request_context" : "Upgrading to 2.4.0.0-169",
              "request_id" : 11,
              "request_status" : "ABORTED",
              "skip_failures" : false,
              "skip_service_check_failures" : false,
              "start_time" : 1463781288984,
              "suspended" : false,
              "to_version" : "2.4.0.0-169",
              "type" : "INTERNAL_REQUEST",
              "upgrade_type" : "NON_ROLLING"
            }
          },
          {
            "href" : "http://bdavm079.svl.ibm.com:8080/api/v1/clusters/bi/upgrades/12",
            "Upgrade" : {
              "cluster_name" : "bi",
              "create_time" : 1463781341576,
              "direction" : "DOWNGRADE",
              "downgrade_allowed" : true,
              "end_time" : 1463781371778,
              "exclusive" : false,
              "associated_version" : "2.3.6.0-3712",
              "pack" : "nonrolling-upgrade-2.4",
              "progress_percent" : 100.0,
              "request_context" : "Downgrading to 2.3.6.0-3712",
              "request_id" : 12,
              "request_status" : "COMPLETED",
              "skip_failures" : false,
              "skip_service_check_failures" : false,
              "start_time" : 1463781342585,
              "suspended" : false,
              "to_version" : "2.3.6.0-3712",
              "type" : "INTERNAL_REQUEST",
              "upgrade_type" : "NON_ROLLING"
            }
          },
          {
            "href" : "http://bdavm079.svl.ibm.com:8080/api/v1/clusters/bi/upgrades/13",
            "Upgrade" : {
              "cluster_name" : "bi",
              "create_time" : 1464120656181,
              "direction" : "UPGRADE",
              "downgrade_allowed" : true,
              "end_time" : 1464120881477,
              "exclusive" : false,
              "associated_version" : "2.3.6.0-3712",
              "pack" : "upgrade-2.4",
              "progress_percent" : 100.0,
              "request_context" : "Upgrading to 2.4.0.0-169",
              "request_id" : 13,
              "request_status" : "ABORTED",
              "skip_failures" : false,
              "skip_service_check_failures" : false,
              "start_time" : 1464120657198,
              "suspended" : false,
              "to_version" : "2.4.0.0-169",
              "type" : "INTERNAL_REQUEST",
              "upgrade_type" : "ROLLING"
            }
          },
          {
            "href" : "http://bdavm079.svl.ibm.com:8080/api/v1/clusters/bi/upgrades/14",
            "Upgrade" : {
              "cluster_name" : "bi",
              "create_time" : 1464120881574,
              "direction" : "DOWNGRADE",
              "downgrade_allowed" : true,
              "end_time" : 1464120918774,
              "exclusive" : false,
              "associated_version" : "2.3.6.0-3712",
              "pack" : "upgrade-2.4",
              "progress_percent" : 100.0,
              "request_context" : "Downgrading to 2.3.6.0-3712",
              "request_id" : 14,
              "request_status" : "COMPLETED",
              "skip_failures" : false,
              "skip_service_check_failures" : false,
              "start_time" : 1464120882580,
              "suspended" : false,
              "to_version" : "2.3.6.0-3712",
              "type" : "INTERNAL_REQUEST",
              "upgrade_type" : "ROLLING"
            }
          },
          {
            "href" : "http://bdavm079.svl.ibm.com:8080/api/v1/clusters/bi/upgrades/15",
            "Upgrade" : {
              "cluster_name" : "bi",
              "create_time" : 1464120943986,
              "direction" : "UPGRADE",
              "downgrade_allowed" : true,
              "end_time" : 1464121132856,
              "exclusive" : false,
              "associated_version" : "2.3.6.0-3712",
              "pack" : "upgrade-2.4",
              "progress_percent" : 100.0,
              "request_context" : "Upgrading to 2.4.0.0-169",
              "request_id" : 15,
              "request_status" : "ABORTED",
              "skip_failures" : false,
              "skip_service_check_failures" : false,
              "start_time" : 1464120945002,
              "suspended" : false,
              "to_version" : "2.4.0.0-169",
              "type" : "INTERNAL_REQUEST",
              "upgrade_type" : "ROLLING"
            }
          },
          {
            "href" : "http://bdavm079.svl.ibm.com:8080/api/v1/clusters/bi/upgrades/16",
            "Upgrade" : {
              "cluster_name" : "bi",
              "create_time" : 1464121132981,
              "direction" : "DOWNGRADE",
              "downgrade_allowed" : true,
              "end_time" : 1464121167178,
              "exclusive" : false,
              "associated_version" : "2.3.6.0-3712",
              "pack" : "upgrade-2.4",
              "progress_percent" : 100.0,
              "request_context" : "Downgrading to 2.3.6.0-3712",
              "request_id" : 16,
              "request_status" : "COMPLETED",
              "skip_failures" : false,
              "skip_service_check_failures" : false,
              "start_time" : 1464121133988,
              "suspended" : false,
              "to_version" : "2.3.6.0-3712",
              "type" : "INTERNAL_REQUEST",
              "upgrade_type" : "ROLLING"
            }
          },
          {
            "href" : "http://bdavm079.svl.ibm.com:8080/api/v1/clusters/bi/upgrades/17",
            "Upgrade" : {
              "cluster_name" : "bi",
              "create_time" : 1464121207511,
              "direction" : "UPGRADE",
              "downgrade_allowed" : true,
              "end_time" : 1464121301821,
              "exclusive" : false,
              "associated_version" : "2.3.6.0-3712",
              "pack" : "nonrolling-upgrade-2.4",
              "progress_percent" : 100.0,
              "request_context" : "Upgrading to 2.4.0.0-169",
              "request_id" : 17,
              "request_status" : "ABORTED",
              "skip_failures" : false,
              "skip_service_check_failures" : false,
              "start_time" : 1464121208524,
              "suspended" : false,
              "to_version" : "2.4.0.0-169",
              "type" : "INTERNAL_REQUEST",
              "upgrade_type" : "NON_ROLLING"
            }
          },
          {
            "href" : "http://bdavm079.svl.ibm.com:8080/api/v1/clusters/bi/upgrades/18",
            "Upgrade" : {
              "cluster_name" : "bi",
              "create_time" : 1464121301933,
              "direction" : "DOWNGRADE",
              "downgrade_allowed" : true,
              "end_time" : 1464121336149,
              "exclusive" : false,
              "associated_version" : "2.3.6.0-3712",
              "pack" : "nonrolling-upgrade-2.4",
              "progress_percent" : 100.0,
              "request_context" : "Downgrading to 2.3.6.0-3712",
              "request_id" : 18,
              "request_status" : "COMPLETED",
              "skip_failures" : false,
              "skip_service_check_failures" : false,
              "start_time" : 1464121302941,
              "suspended" : false,
              "to_version" : "2.3.6.0-3712",
              "type" : "INTERNAL_REQUEST",
              "upgrade_type" : "NON_ROLLING"
            }
          }
        ]
      };

    var upgradeParseResult = {
        'clusterName':'bi',
        'createTime':1464121301933,
        "direction" : "DOWNGRADE",
        "downgradeAllowed" : true,
        "endTime" : 1464121336149,
        "requestId" : 18,
        "requestStatus" : "COMPLETED",
        "skipFailures" : false,
        "skipServiceCheckFailures" : false,
        "startTime" : 1464121302941,
        "upgradeType" : "NON_ROLLING"
    };

    beforeEach(function () {
      App.resetDsStoreTypeMap(App.StackUpgradeHistory);
      sinon.stub(App.store, 'commit', Em.K);
    });

    afterEach(function(){
      App.store.commit.restore();
    });

    it('Parse upgrade records returned by the Ambari server', function () {
      App.stackUpgradeHistoryMapper.map(data);
      var all_records = App.StackUpgradeHistory.find();
      var upgrades = all_records.toArray();
      expect(upgrades.length).to.eql(12);
      var total_downgrades = 0;
      var total_upgrades = 0;
      upgrades.forEach(function(upgrade){
        var direction = upgrade.get('direction')
        if ('DOWNGRADE' === direction){
          total_downgrades++;
        }
        if ('UPGRADE' === direction){
          total_upgrades++;
        }
      });
      expect(total_upgrades).to.eql(6);
      expect(total_downgrades).to.eql(6);

      var record = App.StackUpgradeHistory.find().findProperty('requestId', 18);
      Em.keys(upgradeParseResult).forEach(function (key) {
        expect(record.get(key)).to.eql(upgradeParseResult[key]);
      });
    });
  });
});
