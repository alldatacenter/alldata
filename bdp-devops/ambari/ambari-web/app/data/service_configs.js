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
require('models/configs/objects/service_config_category');

/**
 * This
 * @type {Array} Array of non-service tabs that will appears on Customizes services page
 */
module.exports = [
  Em.Object.create({
    serviceName: 'MISC',
    displayName: 'Misc',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'Users and Groups', displayName : 'Services Accounts'}),
      App.ServiceConfigCategory.create({ name: 'Notifications', displayName : 'Notifications', isCustomView: true, customView: App.NotificationsConfigsView, siteFileName: 'alert_notification'})
    ],
    configTypes: {
      "cluster-env": {supports: {final: false}}
    },
    configs: []
  })
];
