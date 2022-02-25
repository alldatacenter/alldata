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
var dataManipulation = require('utils/data_manipulation');

App.alertDefinitionSummaryMapper = App.QuickDataMapper.create({

  config: {},

  map: function(data) {
    console.time('App.alertDefinitionSummaryMapper execution time');

    if (!data.alerts_summary_grouped) return;
    var alertDefinitions = App.AlertDefinition.find();
    var alertDefinitionsMap = alertDefinitions.toArray().toMapByProperty('id');
    var summaryMap = {};
    data.alerts_summary_grouped.forEach(function(alertDefinitionSummary) {
      var alertDefinition = alertDefinitionsMap[alertDefinitionSummary.definition_id];
      if (alertDefinition) {
        var summary = {},
          timestamp = 0;
        Em.keys(alertDefinitionSummary.summary).forEach(function(status) {
          summary[status] = {
            count: alertDefinitionSummary.summary[status].count,
            maintenanceCount: alertDefinitionSummary.summary[status].maintenance_count
          };
          if (alertDefinitionSummary.summary[status].latest_text) {
            summary[status].latestText = alertDefinitionSummary.summary[status].latest_text;
          }
          if (alertDefinitionSummary.summary[status].original_timestamp > timestamp) {
            timestamp = alertDefinitionSummary.summary[status].original_timestamp;
          }
        });
        summaryMap[alertDefinitionSummary.definition_id] = {
          summary: summary,
          lastTriggered: App.dateTimeWithTimeZone(parseInt(timestamp, 10)),
          lastTriggeredRaw: timestamp
        };
      }
    });

    alertDefinitions.forEach(function (d) {
      var id = d.get('id');
      if (alertDefinitionsMap[id].get('stateManager.currentState.name') !== 'saved') {
        alertDefinitionsMap[id].get('stateManager').transitionTo('saved');
      }
      alertDefinitionsMap[id].setProperties(summaryMap[id]);
      if (!alertDefinitionsMap[id].get('enabled')) {
        // clear summary for disabled alert definitions
        alertDefinitionsMap[id].set('summary', {});
      }
    });
    // set alertsCount and hasCriticalAlerts for each service
    var groupedByServiceName = dataManipulation.groupPropertyValues(alertDefinitions, 'service.serviceName');
    var groupedByComponentName = dataManipulation.groupPropertyValues(alertDefinitions, 'componentName');
    var services = App.Service.find();
    var servicesMap = services.toArray().toMapByProperty('id');
    Object.keys(groupedByServiceName).forEach(function(serviceName) {
      var service = servicesMap[serviceName];
      if (service) {
        service.setProperties({
          criticalCount: groupedByServiceName[serviceName].map((alertDefinition) => {
            return alertDefinition.getWithDefault('summary.CRITICAL.count', 0);
          }).reduce(Em.sum, 0),
          warningCount: groupedByServiceName[serviceName].map((alertDefinition) => {
            return alertDefinition.getWithDefault('summary.WARNING.count', 0);
          }).reduce(Em.sum, 0)
        });

        service.get('hostComponents').filterProperty('isMaster').forEach(function (master) {
          master.setProperties({
            criticalCount: groupedByServiceName[serviceName].map((alertDefinition) => {
              return alertDefinition.getWithDefault('summary.CRITICAL.count', 0);
            }).reduce(Em.sum, 0),
            warningCount: groupedByServiceName[serviceName].map((alertDefinition) => {
              return alertDefinition.getWithDefault('summary.WARNING.count', 0);
            }).reduce(Em.sum, 0)
          });
        });
      }
    });
    console.timeEnd('App.alertDefinitionSummaryMapper execution time');

  }
});
