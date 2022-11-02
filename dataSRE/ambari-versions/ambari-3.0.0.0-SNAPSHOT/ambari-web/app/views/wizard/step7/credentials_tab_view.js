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

App.CredentialsTabOnStep7View = Em.View.extend({

  templateName: require('templates/wizard/step7/credentials_tab'),

  didInsertElement: function () {
    this.setRows();
  },

  /**
   * Row objects to represent table rows
   */
  rows: [],

  /**
   * Array of all used properties on tab to provide binding for enabling/disabling next button
   */
  properties: [],

  /**
   * Set rows in accordance to data fetched from themes API
   */
  setRows: function () {
    if (this.get('controller.stepConfigsCreated')) {
      var result = [];
      var properties = [];
      var self = this;
      var passwordProperty, usernameProperty;
      App.Tab.find().filterProperty('name', 'credentials').forEach(function (tab) {
        if (tab.get('isCategorized')) {
          tab.get('sections').findProperty('name', 'credentials').get('subSections').forEach(function (row) {
            passwordProperty = null;
            usernameProperty = null;
            row.get('configProperties').forEach(function (id) {
              var config = App.configsCollection.getConfig(id);
              var stepConfig = config && self.get('controller.stepConfigs').findProperty('serviceName', Em.get(config, 'serviceName')).get('configs').findProperty('name', Em.get(config, 'name'));
              if (stepConfig) {
                if (stepConfig.get('displayType') === 'password') {
                  passwordProperty = stepConfig;
                } else {
                  usernameProperty = stepConfig;
                }
                properties.push(stepConfig);
              }
            });
            if (passwordProperty) {
              result.push({
                displayName: row.get('displayName'),
                passwordProperty: passwordProperty,
                usernameProperty: usernameProperty
              });
            }
          });
        }
      });
      this.set('rows', result);
      this.set('properties', properties);
    }
  }.observes('controller.stepConfigsCreated'),

  /**
   * Update appropriate credentialsTabNextEnabled flag in accordance to properties validation results
   */
  updateNextDisabled: function () {
    var rows = this.get('rows');
    var result = !rows.someProperty('passwordProperty.error', true) && !rows.someProperty('usernameProperty.error', true);
    this.set('controller.credentialsTabNextEnabled', result);
  }.observes('properties.@each.error')

});
