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
module.exports = [
  {
    value: Em.I18n.t('common.all'),
    isHealthStatus: true,
    healthStatus: '',
    healthClass: '',
    isActive: true,
    isVisible: false
  },
  {
    value: Em.I18n.t('hosts.host.healthStatusCategory.green'),
    isHealthStatus: true,
    class: App.healthIconClassGreen,
    healthStatus: 'HEALTHY',
    healthClass: 'health-status-LIVE'
  },
  {
    value: Em.I18n.t('hosts.host.healthStatusCategory.red'),
    isHealthStatus: true,
    class: App.healthIconClassRed,
    healthStatus: 'UNHEALTHY',
    healthClass: 'health-status-DEAD-RED'
  },
  {
    value: Em.I18n.t('hosts.host.healthStatusCategory.orange'),
    isHealthStatus: true,
    class: App.healthIconClassOrange,
    healthStatus: 'ALERT',
    healthClass: 'health-status-DEAD-ORANGE'
  },
  {
    value: Em.I18n.t('hosts.host.healthStatusCategory.yellow'),
    isHealthStatus: true,
    class: App.healthIconClassYellow,
    healthStatus: 'UNKNOWN',
    healthClass: 'health-status-DEAD-YELLOW'
  },
  {
    value: Em.I18n.t('hosts.host.alerts.label'),
    hostProperty: 'criticalWarningAlertsCount',
    class: 'glyphicon glyphicon-exclamation-sign',
    isHealthStatus: false,
    healthClass: 'health-status-WITH-ALERTS',
    healthStatus: 'health-status-WITH-ALERTS',
    column: 7,
    type: 'custom',
    filterValue: ['>0', '>0']
  },
  {
    value: Em.I18n.t('common.restart'),
    hostProperty: 'componentsWithStaleConfigsCount',
    class: 'glyphicon glyphicon-refresh',
    isHealthStatus: false,
    healthClass: 'health-status-RESTART',
    healthStatus: 'health-status-RESTART',
    column: 8,
    type: 'string',
    filterValue: 'true'
  },
  {
    value: Em.I18n.t('common.passive_state'),
    hostProperty: 'componentsInPassiveStateCount',
    class: 'passive-state icon-medkit',
    isHealthStatus: false,
    healthClass: 'health-status-PASSIVE_STATE',
    healthStatus: 'health-status-PASSIVE_STATE',
    column: 9,
    type: 'string',
    filterValue: ['ON','IMPLIED_FROM_HOST','IMPLIED_FROM_SERVICE','IMPLIED_FROM_SERVICE_AND_HOST']
  }
];
