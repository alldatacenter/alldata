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
var stringUtils = require('utils/string_utils');

App.ClientComponent = DS.Model.extend({
  service: DS.belongsTo('App.Service'),
  componentName: DS.attr('string'),
  displayName: DS.attr('string'),
  installedCount: DS.attr('number', {defaultValue: 0}),
  installFailedCount: DS.attr('number', {defaultValue: 0}),
  initCount: DS.attr('number', {defaultValue: 0}),
  unknownCount: DS.attr('number', {defaultValue: 0}),
  startedCount: DS.attr('number', {defaultValue: 0}),
  totalCount: DS.attr('number', {defaultValue: 0}),
  stackInfo: DS.belongsTo('App.StackServiceComponent'),
  hostNames: DS.attr('array'),
  staleConfigHosts: DS.attr('array'),

  /**
   * Determines if component may be deleted
   *
   * @type {boolean}
   */
  allowToDelete: function () {
    return this.get('totalCount') === (this.get('installedCount') + this.get('installFailedCount') + this.get('initCount') + this.get('unknownCount'));
  }.property('totalCount', 'installedCount', 'installFailedCount', 'initCount', 'unknownCount'),

  summaryLabelClassName: function () {
    return 'label_for_'+this.get('componentName').toLowerCase();
  }.property('componentName'),

  summaryValueClassName: function () {
    return 'value_for_'+this.get('componentName').toLowerCase();
  }.property('componentName'),

  displayNamePluralized: function () {
    return stringUtils.pluralize(this.get('installedCount'), this.get('displayName'));
  }.property('installedCount')
});

App.ClientComponent.getModelByComponentName = function(componentName) {
  if (App.HostComponent.isMaster(componentName)) {
    return App.MasterComponent.find(componentName)
  } else if (App.HostComponent.isSlave(componentName)) {
    return App.SlaveComponent.find(componentName)
  } else if (App.HostComponent.isClient(componentName)) {
    return App.ClientComponent.find(componentName)
  }
  return null;
};

App.ClientComponent.FIXTURES = [];
