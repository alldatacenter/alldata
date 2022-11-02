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

App.MainDashboardServiceStormView = App.MainDashboardServiceView.extend({
  templateName: require('templates/main/service/services/storm'),
  serviceName: 'STORM',

  /**
   * this parameter is used to fiter hosts by component name
   * used in mainHostController.filterByComponent() method
   */
  filterComponent: Em.Object.create({componentName: 'SUPERVISOR'}),

  freeSlotsPercentage: Em.computed.percents('service.freeSlots', 'service.totalSlots'),

  superVisorsLive: Em.computed.alias('service.superVisorsStarted'),

  superVisorsTotal: Em.computed.alias('service.superVisorsTotal'),

  nimbusUptimeFormatted: function() {
    return this.get('service.nimbusUptime') || Em.I18n.t('services.service.summary.notRunning');
  }.property('service.nimbusUptime'),

  isSupervisorCreated: function () {
    return this.isServiceComponentCreated('SUPERVISOR');
  }.property('App.router.clusterController.isComponentsStateLoaded')
});
