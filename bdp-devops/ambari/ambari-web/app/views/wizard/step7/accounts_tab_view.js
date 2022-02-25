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

App.AccountsTabOnStep7View = Em.View.extend(App.WizardMiscPropertyChecker, {

  templateName: require('templates/wizard/step7/accounts_tab'),

  /**
   * Property objects to represent table rows
   */
  properties: function () {
    return this.get('controller.stepConfigs').findProperty('serviceName', 'MISC').get('configs').filterProperty('displayType', 'user');
  }.property('controller.stepConfigsCreated'),

  propertyChanged: function () {
    var changedProperty = this.get('properties').find(function (prop) {
      return prop.get('editDone');
    });
    if(!changedProperty) {
      return;
    }
    var stepConfigs = this.get('controller.stepConfigs');
    var serviceId = changedProperty.get('serviceName');
    return this.showConfirmationDialogIfShouldChangeProps(changedProperty, stepConfigs, serviceId);
  }.observes('properties.@each.editDone'),

  checkboxes: function () {
    var miscConfigs = this.get('controller.stepConfigs').findProperty('serviceName', 'MISC').get('configs');
    const miscProperties = ['sysprep_skip_create_users_and_groups', 'ignore_groupsusers_create', 'override_uid'];
    const checkboxArr = [];
    miscProperties.forEach(property => {
      if (miscConfigs.findProperty('name', property)) checkboxArr.push(miscConfigs.findProperty('name', property));
    });
    return checkboxArr;
  }.property('controller.stepConfigsCreated')

});
