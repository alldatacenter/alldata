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
var stringUtils = require('utils/string_utils');

App.stackUpgradeHistoryMapper = App.QuickDataMapper.create({
  model: App.StackUpgradeHistory,

  config: {
    "id": "Upgrade.request_id",
    "request_id": "Upgrade.request_id",
    "upgrade_id": "Upgrade.upgrade_id",
    "cluster_name": "Upgrade.cluster_name",
    "direction": "Upgrade.direction",
    "associated_version": "Upgrade.associated_version",
    "versions" : "Upgrade.versions",
    "end_time":"Upgrade.end_time",
    "start_time":"Upgrade.start_time",
    "create_time": "Upgrade.create_time",
    "request_status": "Upgrade.request_status",
    "upgrade_type": "Upgrade.upgrade_type",
    "downgrade_allowed": "Upgrade.downgrade_allowed",
    "skip_failures": "Upgrade.skip_failures",
    "skip_service_check_failures": "Upgrade.skip_service_check_failures"
  },

  map: function (json) {
    App.set('isStackUpgradeHistoryLoaded',false);
    var model = this.get('model');
    var result = [];
    json.items.forEach(function(item) {
      var parseResult = this.parseIt(item, this.get('config'));
      if (parseResult.direction === 'UPGRADE') {
        parseResult.to_version = parseResult.associated_version;
      }
      else {
        parseResult.from_version = parseResult.associated_version;
      }
      result.push(parseResult);
    }, this);

    App.store.safeLoadMany(this.get('model'), result);
    App.set('isStackUpgradeHistoryLoaded',true);
  }
});
