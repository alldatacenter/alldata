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

require('controllers/main/service/info/configs');

App.MainAdminServiceAccountsController = App.MainServiceInfoConfigsController.extend({
  name: 'mainAdminServiceAccountsController',
  users: null,
  serviceConfigTags: [],
  content: Em.Object.create({
    serviceName: 'MISC'
  }),
  loadUsers: function () {
    this.set('selectedService', this.get('content.serviceName') ? this.get('content.serviceName') : "MISC");
    this.loadServiceConfig();
  },
  loadServiceConfig: function () {
    App.router.get('configurationController').getCurrentConfigsBySites().done((serverConfigs) => {
      this.createConfigObject(serverConfigs);
    });
  },

  /**
   * Generate configuration object that will be rendered
   *
   * @param {Object[]} serverConfigs
   */
  createConfigObject: function(serverConfigs) {
    var configs = [];
    serverConfigs.forEach(function(configObject) {
      configs = configs.concat(App.config.getConfigsFromJSON(configObject, true));
    });
    var miscConfigs = configs.filterProperty('displayType', 'user').filterProperty('category', 'Users and Groups');
    miscConfigs.setEach('isVisible', true);
    this.set('users', miscConfigs);
    this.set('dataIsLoaded', true);
  },

  /**
   * sort miscellaneous configs by specific order
   * @param sortOrder
   * @param arrayToSort
   * @return {Array}
   */
  sortByOrder: function (sortOrder, arrayToSort) {
    var sorted = [];
    if (sortOrder && sortOrder.length > 0) {
      sortOrder.forEach(function (name) {
        var user = arrayToSort.findProperty('name', name);
        if (user) {
          sorted.push({
            isVisible: user.get('isVisible'),
            displayName: user.get('displayName'),
            value: user.get('value')
          });
        }
      });
      return sorted;
    } else {
      return arrayToSort;
    }
  }
});
