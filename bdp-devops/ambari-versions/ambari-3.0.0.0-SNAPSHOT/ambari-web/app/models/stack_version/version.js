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

App.StackVersion = DS.Model.extend({
  clusterName: DS.attr('string'),
  stack: DS.attr('string'),
  version: DS.attr('string'),
  name: Em.computed.concat(' ', 'stack', 'version'),
  state: DS.attr('string'),
  repositoryVersion: DS.belongsTo('App.RepositoryVersion'),
  notInstalledHosts: DS.attr('array'),
  installingHosts: DS.attr('array'),
  installedHosts: DS.attr('array'),
  installFailedHosts: DS.attr('array'),
  outOfSyncHosts: DS.attr('array'),
  upgradingHosts: DS.attr('array'),
  upgradedHosts: DS.attr('array'),
  upgradeFailedHosts: DS.attr('array'),
  currentHosts: DS.attr('array'),
  supportsRevert: DS.attr('boolean'),
  revertUpgradeId: DS.attr('number'),

  noInstalledHosts: Em.computed.empty('installedHosts'),

  noCurrentHosts: Em.computed.empty('currentHosts'),

  noInitHosts: Em.computed.empty('notInstalledHosts'),

  isCurrent: Em.computed.equal('state', 'CURRENT'),

  isOutOfSync: Em.computed.equal('state', 'OUT_OF_SYNC')
});

App.StackVersion.FIXTURES = [];

