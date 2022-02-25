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

App.alertNotificationMapper = App.QuickDataMapper.create({

  model: App.AlertNotification,

  config: {
    id: 'AlertTarget.id',
    name: 'AlertTarget.name',
    type: 'AlertTarget.notification_type',
    description: 'AlertTarget.description',
    global: 'AlertTarget.global',
    enabled: 'AlertTarget.enabled'
  },

  map: function (json) {
    if(Em.isNone(App.cache['previousAlertNotificationsFullMap'])) {
      App.cache['previousAlertNotificationsFullMap'] = {};
    }
    console.time('App.alertNotificationMapper execution time');
    if (json.items) {
      var result = [];
      var notificationsProperties = {};
      var notificationsAlertStates = {};
      var groupsMap = App.cache['alertNotificationsGroupsMap'];
      var notifications = {};
      var self = this;

      json.items.forEach(function (item) {
        var notification = this.parseIt(item, this.config);
        var groups = groupsMap && groupsMap[notification.id];
        if (groups) {
          notification.groups = groups;
        }

        var previousNotification = App.cache['previousAlertNotificationsFullMap'][notification.id] ? App.cache['previousAlertNotificationsFullMap'][notification.id] : {};
        var changedFields = self.getDiscrepancies(notification, previousNotification, ['name', 'type', 'description', 'global', 'enabled', 'groups']);
        if (Object.keys(changedFields).length) {
          result.push(notification);
        }

        notifications[notification.id] = notification;
        notificationsProperties[item.AlertTarget.id] = item.AlertTarget.properties;
        notificationsAlertStates[item.AlertTarget.id] = item.AlertTarget.alert_states;
      }, this);

      App.store.safeLoadMany(this.get('model'), result);
      App.cache['previousAlertNotificationsFullMap'] = notifications;
      this._setPropertiesToEachModel('properties', notificationsProperties);
      this._setPropertiesToEachModel('alertStates', notificationsAlertStates);
    }
    console.timeEnd('App.alertNotificationMapper execution time');
  },

  /**
   * Set values from <code>propertyMap</code> for <code>propertyName</code> for each record in model
   * @param {string} propertyName
   * @param {object} propertiesMap record_id to value map
   * @method setPropertiesToEachModel
   * @private
   */
  _setPropertiesToEachModel: function (propertyName, propertiesMap) {
    for (var recordId in propertiesMap) {
      if (propertiesMap.hasOwnProperty(recordId)) {
        App.AlertNotification.find(recordId).set(propertyName, propertiesMap[recordId]);
      }
    }
  }
});