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

App.RepositoryVersion = DS.Model.extend({
  displayName: DS.attr('string'),
  type: DS.attr('string'), // "PATCH" /* STANDARD, (future: SERVICE) */
  repositoryVersion: DS.attr('string'),
  upgradePack: DS.attr('string'),
  stackVersionType: DS.attr('string'),
  stackVersionNumber: DS.attr('string'),
  useRedhatSatellite: DS.attr('boolean'),
  operatingSystems: DS.hasMany('App.OS'),
  stackServices: DS.hasMany('App.ServiceSimple'),
  stackVersion: DS.belongsTo('App.StackVersion'),
  stack: Em.computed.concat(' ', 'stackVersionType', 'stackVersionNumber'),
  hidden: DS.attr('boolean'),
  displayNameSimple: function() {
    return this.get('stackVersionType') + '-' + this.get('repositoryVersion').split('-')[0];
  }.property('stackVersionType', 'repositoryVersion'),
  
  displayNameFull: function() {
    return this.get('stackVersionType') + '-' + this.get('repositoryVersion');
  }.property('stackVersionType', 'repositoryVersion'),

  isPatch: Em.computed.equal('type', 'PATCH'),

  isMaint: Em.computed.equal('type', 'MAINT'),

  isService: Em.computed.equal('type', 'SERVICE'),

  isStandard: Em.computed.equal('type', 'STANDARD'),

  /**
   * status used until corresponding stack version get created
   * @type {string}
   */
  defaultStatus: 'NOT_REQUIRED',

  /**
   * @type {boolean}
   * @default false
   */
  isCompatible: false,

  /**
   * @type {string}
   */
  status: Em.computed.firstNotBlank('stackVersion.state', 'defaultStatus'),

  /**
   * @type {Array}
   */
  notInstalledHosts: function () {
    return this.get('stackVersion.notInstalledHosts').length || this.get('stackVersion.installedHosts').length || this.get('stackVersion.currentHosts').length
          ? this.get('stackVersion.notInstalledHosts')
          : App.get('allHostNames');
  }.property('stackVersion.notInstalledHosts'),

  /**
   * @type {Array}
   */
  installedHosts: function () {
    return this.get('stackVersion.installedHosts') || [];
  }.property('stackVersion.installedHosts'),

  /**
   * @type {Array}
   */
  currentHosts: function () {
    return this.get('stackVersion.currentHosts') || [];
  }.property('stackVersion.currentHosts'),

  /**
   * @type {boolean}
   */
  noInstalledHosts: function () {
    return (this.get('stackVersion')) ? this.get('stackVersion.noInstalledHosts') : true;
  }.property('stackVersion.noInstalledHosts'),

  /**
   * @type {boolean}
   */
  noCurrentHosts: function () {
    return (this.get('stackVersion')) ? this.get('stackVersion.noCurrentHosts') : true;
  }.property('stackVersion.noCurrentHosts'),

  /**
   * @type {boolean}
   */
  noInitHosts: function () {
    return (this.get('stackVersion')) ? this.get('stackVersion.noInitHosts') : false;
  }.property('stackVersion.noInitHosts'),

  /**
   * @type {string}
   */
  noInitHostsTooltip: Em.computed.ifThenElse('noInitHosts', Em.I18n.t('admin.stackVersions.version.emptyHostsTooltip'), Em.I18n.t('admin.stackVersions.version.hostsTooltip')),

  /**
   * @type {string}
   */
  noCurrentHostsTooltip: Em.computed.ifThenElse('noCurrentHosts', Em.I18n.t('admin.stackVersions.version.emptyHostsTooltip'), Em.I18n.t('admin.stackVersions.version.hostsTooltip')),

  /**
   * @type {string}
   */
  noInstalledHostsTooltip: Em.computed.ifThenElse('noInstalledHosts', Em.I18n.t('admin.stackVersions.version.emptyHostsTooltip'), Em.I18n.t('admin.stackVersions.version.hostsTooltip')),

  /**
   * @type {boolean}
   */
  isVisible: true,

  /**
   * @type {boolean}
   */
  isCurrent: Em.computed.equal('status', 'CURRENT')
});

App.RepositoryVersion.FIXTURES = [];
