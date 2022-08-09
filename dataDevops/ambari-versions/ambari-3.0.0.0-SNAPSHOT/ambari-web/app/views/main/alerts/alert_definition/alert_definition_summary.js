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

App.AlertDefinitionSummary = Em.View.extend({

  templateName: require('templates/main/alerts/alert_definition/alert_definition_summary'),

  /**
   * Bound from the template
   * @type {App.AlertDefinition}
   */
  content: null,

  hasMultipleCount: Em.computed.gt('content.hostCnt', 0),

  isActiveDefinitionState: Em.computed.and('hasMultipleCount', 'content.enabled'),

  definitionState: function () {
    var content = this.get('content');
    if (!content) {
      return [];
    }
    var order = content.get('order');
    var summary = content.get('summary');
    var hostCnt = content.get('hostCnt');
    var showCounts = hostCnt > 1;
    var ret = [];
    order.forEach(function (state) {
      var shortState = App.AlertDefinition.shortState[state];
      var _stateSummary = {
        state: 'alert-state-' + state,
        count: '',
        maintenanceCount: ''
      };
      if (summary[state]) {
        if (summary[state].count) {
          var count = shortState;
          if (showCounts) {
            count += ' (' + summary[state].count + ')';
          }
          _stateSummary.count = count;
        }
        // add status with maintenance mode icon
        if (summary[state].maintenanceCount) {
          var maintenanceCount = shortState;
          if (showCounts) {
            maintenanceCount += ' (' + summary[state].maintenanceCount + ')';
          }
          _stateSummary.maintenanceCount = maintenanceCount;
        }
      }
      ret.push(_stateSummary);
    });
    return ret;
  }.property('content.summary', 'content.hostCnt')

});