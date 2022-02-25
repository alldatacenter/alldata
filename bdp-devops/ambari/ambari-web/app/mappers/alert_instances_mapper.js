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

App.alertInstanceMapper = App.QuickDataMapper.create({

  model : App.AlertInstance,

  modelLocal: App.AlertInstanceLocal,

  config : {
    id: 'Alert.id',
    label: 'Alert.label',
    definition_name: 'Alert.definition_name',
    definition_id: 'Alert.definition_id',
    service_id: 'Alert.service_name',
    service_name: 'Alert.service_name',
    component_name: 'Alert.component_name',
    host_id: 'Alert.host_name',
    host_name: 'Alert.host_name',
    scope: 'Alert.scope',
    original_timestamp: 'Alert.original_timestamp',
    original_raw_timestamp: 'Alert.original_timestamp',
    latest_timestamp: 'Alert.latest_timestamp',
    maintenance_state: 'Alert.maintenance_state',
    instance: 'Alert.instance',
    state: 'Alert.state',
    text: 'Alert.text',
    repeat_tolerance: 'Alert.repeat_tolerance',
    repeat_tolerance_remaining: 'Alert.repeat_tolerance_remaining'
  },

  map: function(json) {
    return this.parse(json, this.get('model'));
  },

  mapLocal: function(json) {
    return this.parse(json, this.get('modelLocal'));
  },

  parse: function(json, model) {
    console.time('App.alertInstanceMapper execution time');
    if (json.items) {
      var alertInstances = [];
      var alertsToDelete = model.find().mapProperty('id');

      json.items.forEach(function (item) {
        var alert = this.parseIt(item, this.get('config'));
        alert.original_timestamp = App.dateTimeWithTimeZone(alert.original_timestamp);
        alert.latest_timestamp = App.dateTimeWithTimeZone(alert.latest_timestamp);
        alertInstances.push(alert);
        alertsToDelete = alertsToDelete.without(alert.id);
      }, this);

      if (alertsToDelete.length > 0) {
        model.find().clear();
      }

      App.store.safeLoadMany(model, alertInstances);
      console.timeEnd('App.alertInstanceMapper execution time');
    }
  }

});
