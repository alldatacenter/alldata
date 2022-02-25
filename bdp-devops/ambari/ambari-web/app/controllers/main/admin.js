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

App.MainAdminController = Em.Controller.extend({
  name: 'mainAdminController',

  /**
   * @type {string}
   * @default null
   */
  category: null,

  /**
   * Check if access page available.
   * Turn on if YARN service is installed with Application Timeline Server component and TEZ installed too.
   *
   * @type {Boolean}
   */
  isAccessAvailable: function () {
    var dependencies = {
      services: ['YARN', 'TEZ']
    };
    var serviceNames = App.Service.find().mapProperty('serviceName')
      .filter(function (serviceName) {
        return dependencies.services.contains(serviceName);
      });
    var isAppTimelineServerAvailable = App.HostComponent.find().someProperty('componentName', 'APP_TIMELINE_SERVER');
    return (dependencies.services.length === serviceNames.length && isAppTimelineServerAvailable);
  }.property()
});