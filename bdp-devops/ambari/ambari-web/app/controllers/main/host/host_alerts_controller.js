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

App.MainHostAlertsController = Em.ArrayController.extend({
  name: 'mainHostAlertsController',

  selectedHost: Em.computed.alias('App.router.mainHostDetailsController.content'),

  /**
   * List of all <code>App.AlertInstance</code> by Host
   * @type {App.AlertInstance[]}
   */
  content: function () {
    return App.AlertInstanceLocal.find().toArray().filterProperty('host', this.get('selectedHost'));
  }.property('App.router.mainAlertInstancesController.isLoaded', 'selectedHost'),

  /**
   * Open details page of the selected alertDefinition
   * @param {object} event
   * @method routeToAlertDefinition
   */
  routeToAlertDefinition: function (event) {
    var alertDefinition = App.AlertDefinition.find(event.context);
    App.router.transitionTo('main.alerts.alertDetails', alertDefinition);
  }
});