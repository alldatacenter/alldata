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

App.ServiceSimple = DS.Model.extend({
  id: DS.attr('string'),
  name: DS.attr('string'),
  displayName: DS.attr('string'),
  latestVersion: DS.attr('string'),
  isAvailable: DS.attr('boolean'),
  isUpgradable: DS.attr('boolean'),
  isHidden: Em.computed.alias('doNotShowAndInstall'),

  doNotShowAndInstall: function () {
    var skipServices = ['KERBEROS'];
    if(!App.get('supports.installGanglia')) {
      skipServices.push('GANGLIA');
    }
    return skipServices.contains(this.get('name'));
  }.property('name')
});

App.ServiceSimple.FIXTURES = [];
