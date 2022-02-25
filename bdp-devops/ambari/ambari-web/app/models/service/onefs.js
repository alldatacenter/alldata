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

App.ONEFSService = App.Service.extend({
  nameNodeStartTime: DS.attr('number'),
  jvmMemoryHeapUsed: DS.attr('number'),
  jvmMemoryHeapMax: DS.attr('number'),
  capacityUsed: DS.attr('number'),
  capacityTotal: DS.attr('number'),
  capacityRemaining: DS.attr('number'),
  capacityNonDfsUsed: DS.attr('number'),
  dfsTotalBlocks: DS.attr('number'),
  dfsCorruptBlocks: DS.attr('number'),
  dfsMissingBlocks: DS.attr('number'),
  dfsUnderReplicatedBlocks: DS.attr('number'),
  dfsTotalFiles: DS.attr('number'),
  metricsNotAvailable: DS.attr('boolean'),
  decommissionDataNodes: DS.hasMany('App.HostComponent'),
  liveDataNodes: DS.hasMany('App.HostComponent'),
  deadDataNodes: DS.hasMany('App.HostComponent'),
  upgradeStatus: DS.attr('string'),
  nfsGatewaysStarted: DS.attr('number', {defaultValue: 0}),
  nfsGatewaysInstalled: DS.attr('number', {defaultValue: 0}),
  nfsGatewaysTotal: DS.attr('number', {defaultValue: 0})
});

App.ONEFSService.FIXTURES = [];
