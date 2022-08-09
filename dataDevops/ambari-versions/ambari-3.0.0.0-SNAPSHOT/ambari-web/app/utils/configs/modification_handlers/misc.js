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
require('utils/configs/modification_handlers/modification_handler');

module.exports = App.ServiceConfigModificationHandler.create({
  serviceId : 'MISC',
  getDependentConfigChanges : function(changedConfig, selectedServices, allConfigs, securityEnabled) {
    var affectedProperties = [];
    var newValue = changedConfig.get("value");
    var curConfigs = "";
    var affectedPropertyName;
    var affectedPropery;
    if (changedConfig.get("name") == "hdfs_user") {
      curConfigs = allConfigs.findProperty("serviceName", "HDFS").get("configs");
      affectedPropertyName = "dfs.permissions.superusergroup";
      affectedPropery = curConfigs.findProperty("name", affectedPropertyName);
      if (affectedPropery && newValue != affectedPropery.get('value')) {
        affectedProperties.push({
          serviceName : "HDFS",
          sourceServiceName : "MISC",
          propertyName : affectedPropertyName,
          propertyDisplayName : affectedPropertyName,
          newValue : newValue,
          curValue : affectedPropery.get("value"),
          changedPropertyName : "hdfs_user",
          remove : false,
          filename : 'hdfs-site.xml'
        });
      }

      affectedPropertyName = "dfs.cluster.administrators";
      affectedPropery = curConfigs.findProperty("name", affectedPropertyName);
      if (affectedPropery && $.trim(newValue) != $.trim(affectedPropery.get("value"))) {
        affectedProperties.push({
          serviceName : "HDFS",
          sourceServiceName : "MISC",
          propertyName : affectedPropertyName,
          propertyDisplayName : affectedPropertyName,
          newValue : " " + $.trim(newValue),
          curValue : affectedPropery.get("value"),
          changedPropertyName : "hdfs_user",
          remove : false,
          filename : 'hdfs-site.xml'
        });
      }
    } else if (changedConfig.get("name") == "yarn_user") {
      curConfigs = allConfigs.findProperty('serviceName', 'YARN').get('configs');
      var currentUsers,
          currentGroups;
      var initialUser = changedConfig.get('initialValue');
      var newUser = newValue;
      var currentAclValue = curConfigs.findProperty("name", "yarn.admin.acl").get("value");
      var currentAclValueSplits = $.trim(currentAclValue).split(/\s+/).filter(function(i) { return !Em.isEmpty(i); });
      if (currentAclValueSplits.length == 2) {
        currentUsers = currentAclValueSplits[0];
        currentGroups = currentAclValueSplits[1];
      } else {
        currentUsers = currentAclValueSplits.length > 0 ? currentAclValueSplits.shift() : '';
        currentGroups = currentAclValueSplits.join(" ");
      }
      var currentUserList = currentUsers.split(',').filter(function(i) { return !Em.isEmpty(i); });
      if (!currentUserList.contains(newUser)) {
        var currentUserIndex = currentUserList.indexOf(initialUser);
        if (currentUserIndex > -1) {
          currentUserList.splice(currentUserIndex, 1);
        }
        if (!currentUserList.contains(newUser)) {
          currentUserList.push(newUser);
        }
        var newAclValue = $.trim(currentUserList.join(',') + ' ' + currentGroups);
        if (currentAclValue != newAclValue) {
          affectedProperties.push({
            serviceName : "YARN",
            sourceServiceName : "MISC",
            propertyName : "yarn.admin.acl",
            propertyDisplayName : "yarn.admin.acl",
            newValue : newAclValue,
            curValue : currentAclValue,
            changedPropertyName : "yarn_user",
            remove : false,
            filename : 'yarn-site.xml'
          });
        }
      }
    } else if (changedConfig.get("name") == "user_group") {
      if (!selectedServices.contains('YARN')) {
        return [];
      }
      if (selectedServices.contains("MAPREDUCE2")) {
        curConfigs = allConfigs.findProperty("serviceName", "MAPREDUCE2").get("configs");
        if ($.trim(newValue) != $.trim(curConfigs.findProperty("name", "mapreduce.cluster.administrators").get("value"))) {
          affectedProperties.push({
            serviceName : "MAPREDUCE2",
            sourceServiceName : "MISC",
            propertyName : "mapreduce.cluster.administrators",
            propertyDisplayName : "mapreduce.cluster.administrators",
            newValue : " " + $.trim(newValue),
            curValue : curConfigs.findProperty("name", "mapreduce.cluster.administrators").get("value"),
            changedPropertyName : "user_group",
            filename : 'mapred-site.xml'
          });
        }
      }
      if (selectedServices.contains("YARN")) {
        curConfigs = allConfigs.findProperty("serviceName", "YARN").get("configs");
        if (newValue != curConfigs.findProperty("name", "yarn.nodemanager.linux-container-executor.group").get("value")) {
          affectedProperties.push({
            serviceName : "YARN",
            sourceServiceName : "MISC",
            propertyName : "yarn.nodemanager.linux-container-executor.group",
            propertyDisplayName : "yarn.nodemanager.linux-container-executor.group",
            newValue : newValue,
            curValue : curConfigs.findProperty("name", "yarn.nodemanager.linux-container-executor.group").get("value"),
            changedPropertyName : "user_group",
            filename : 'yarn-site.xml'
          })
        }
      }
    }
    return affectedProperties;
  }
});