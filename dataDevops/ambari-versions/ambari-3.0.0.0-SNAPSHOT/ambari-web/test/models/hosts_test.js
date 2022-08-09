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

require('models/hosts');

function getModel() {
  return App.HostInfo.create();
}

describe('App.HostInfo', function () {

  App.TestAliases.testAsComputedGetByKey(getModel(), 'bootStatusForDisplay', 'bootStatusForDisplayMap', 'bootStatus', {defaultValue: 'Registering', map: {PENDING: 'Preparing',
    REGISTERED: 'Success',
    FAILED: 'Failed',
    RUNNING: 'Installing',
    DONE: 'Registering',
    REGISTERING: 'Registering'}});

  App.TestAliases.testAsComputedGetByKey(getModel(), 'bootBarColor', 'bootBarColorMap', 'bootStatus', {defaultValue: 'progress-bar-info', map: {
    REGISTERED: 'progress-bar-success',
    FAILED: 'progress-bar-danger',
    PENDING: 'progress-bar-info',
    RUNNING: 'progress-bar-info',
    DONE: 'progress-bar-info',
    REGISTERING: 'progress-bar-info'
  }});

  App.TestAliases.testAsComputedGetByKey(getModel(), 'bootStatusColor', 'bootStatusColorMap', 'bootStatus', {defaultValue: 'text-info', map: {
    REGISTERED: 'text-success',
    FAILED: 'text-danger',
    PENDING: 'text-info',
    RUNNING: 'text-info',
    DONE: 'text-info',
    REGISTERING: 'text-info'
  }});

  App.TestAliases.testAsComputedExistsIn(getModel(), 'isBootDone', 'bootStatus', ['REGISTERED', 'FAILED']);

});
