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
'use strict';

angular.module('ambariAdminConsole')
.factory('Group', ['$http', '$q', 'Settings', '$translate', 'Cluster', function($http, $q, Settings, $translate, Cluster) {
  var $t = $translate.instant;
  var types = {
    LOCAL: {
      VALUE: 'LOCAL',
      LABEL_KEY: 'common.local'
    },
    PAM: {
      VALUE: 'PAM',
      LABEL_KEY: 'common.pam'
    },
    LDAP: {
      VALUE: 'LDAP',
      LABEL_KEY: 'common.ldap'
    }
  };

  function Group(item) {
    if (typeof item === 'string') {
      this.group_name = item;
    } else if (typeof item === 'object') {
      angular.extend(this, item.Groups);
    }
  }

  Group.prototype.save = function() {
    return $http({
      method : 'POST',
      url: Settings.baseUrl + '/groups',
      data:{
        'Groups/group_name': this.group_name
      }
    });
  };

  Group.prototype.destroy = function() {
    var deferred = $q.defer();
    $http.delete(Settings.baseUrl + '/groups/' +this.group_name)
    .then(function() {
      deferred.resolve();
    })
    .catch(function(data) {
      deferred.reject(data);
    });

    return deferred.promise;
  };

  Group.prototype.saveMembers = function() {
    var self = this;
    var deferred = $q.defer();

    var members = [];
    angular.forEach(this.members, function(member) {
      members.push({
        'MemberInfo/user_name' : member,
        'MemberInfo/group_name' : self.group_name
      });
    });

    $http({
      method: 'PUT',
      url: Settings.baseUrl + '/groups/' + this.group_name + '/members',
      data: members
    })
    .then(function(data) {
      deferred.resolve(data.data);
    })
    .catch(function(data) {
      deferred.reject(data);
    });
    return deferred.promise;
  };

  Group.removeMemberFromGroup = function(groupName, memberName) {
    return $http.delete(Settings.baseUrl + '/groups/'+groupName + '/members/'+memberName);
  };

  Group.addMemberToGroup = function(groupName, memberName) {
    return $http.post(Settings.baseUrl + '/groups/' + groupName + '/members/'+memberName);
  };

  Group.all = function() {
    var deferred = $q.defer();

    $http.get(Settings.baseUrl + '/groups?fields=*')
    .then(function(data) {
      deferred.resolve(data.data.items);
    })
    .catch(function(data) {
      deferred.reject(data);
    });

    return deferred.promise;
  };

  Group.listByName = function(name) {
    return $http.get(Settings.baseUrl + '/groups?'
      + 'Groups/group_name.matches(.*'+name+'.*)'
    );
  };

  Group.getPrivileges = function(groupId) {
    return $http.get(Settings.baseUrl + '/groups/' + groupId + '/privileges', {
      params:{
        'fields': '*'
      }
    });
  };

  Group.get = function (group_name) {
    var deferred = $q.defer();
    $http({
      method: 'GET',
      url: Settings.baseUrl + '/groups/' + group_name +
      '?fields=Groups,privileges/PrivilegeInfo/*,members/MemberInfo'
    }).then(function (data) {
      deferred.resolve(Group.makeGroup(data.data));
    });

    return deferred.promise;
  };

  Group.getTypes = function () {
    return types;
  };

  /**
     * Generate group info to display by response data from API.
     * Generally this is a single point to manage all required and useful data
     * needed to use as context for views/controllers.
     *
     * @param {Object} group - object from API response
     * @returns {Object}
     */
  Group.makeGroup = function(data) {
    var group = new Group(data.Groups.group_name);
    group.groupTypeName = $t(types[data.Groups.group_type].LABEL_KEY);
    group.group_type = data.Groups.group_type;
    group.ldap_group = data.Groups.ldap_group;
    group.privileges = data.privileges;
    group.members = data.members;
    group.roles = Cluster.sortRoles(data.privileges.filter(function(item) {
      return item.PrivilegeInfo.type === 'CLUSTER' || item.PrivilegeInfo.type === 'AMBARI';
    }).map(function(item) {
      return item.PrivilegeInfo;
    }));
    return group;
  };

  return Group;
}]);
