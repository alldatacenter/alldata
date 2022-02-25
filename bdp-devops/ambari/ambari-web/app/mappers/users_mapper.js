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

App.usersMapper = App.QuickDataMapper.create({
  model : App.User,
  config : {
    id : 'Users.user_name',
    user_name : 'Users.user_name',
    admin: 'Users.admin',
    operator: 'Users.operator',
    permissions: 'permissions',
    user_type: 'Users.user_type',
    cluster_user: 'Users.cluster_user'
  },
  map: function (json) {
    var self = this;
    json.items.forEach(function (item) {
      var result= [];
      if(!App.User.find().someProperty("userName", item.Users.user_name)) {
        item.permissions = [];
        var privileges = item.privileges || [];
        if (privileges.length) {
          item.permissions = privileges.mapProperty('PrivilegeInfo.permission_name');
        }
        item.Users.admin = self.isAdmin(item.permissions);
        item.Users.operator = self.isOperator(item.permissions);
        item.Users.cluster_user = self.isClusterUser(item.permissions);
        result.push(self.parseIt(item, self.config));
        App.store.safeLoadMany(self.get('model'), result);
      }
    });
  },

  /**
   * Check if user is admin.
   * @param {Array} permissionList
   * @return {Boolean}
   **/
  isAdmin: function(permissionList) {
    //TODO: Separate cluster operator from admin
    return permissionList.indexOf('AMBARI.ADMINISTRATOR') > -1 || permissionList.indexOf('CLUSTER.ADMINISTRATOR') > -1;
  },

  /**
   * Check if user is operator.
   * @param {Array} permissionList
   * @return {Boolean}
   **/
  isOperator: function(permissionList) {
    return permissionList.indexOf('CLUSTER.ADMINISTRATOR') > -1 && !(permissionList.indexOf('AMBARI.ADMINISTRATOR') > -1);
  },

  /**
   * Determines that user has only one permission CLUSTER.USER.
   *
   * @param {String[]} permissionList
   * @return {Boolean}
   */
  isClusterUser: function(permissionList) {
    return permissionList.length === 1 && permissionList[0] === 'CLUSTER.USER';
  }
});
