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

App.HostStackVersion = DS.Model.extend({
  stack: DS.attr('string'),
  version: DS.attr('string'),
  repo: DS.belongsTo('App.Repository'),
  repoVersion: DS.attr('string'),
  displayName: DS.attr('string'),
  isVisible: DS.attr('boolean', {defaultValue: true}),
  /**
   * possible property value defined at App.HostStackVersion.statusDefinition
   * @type {string}
   */
  status: DS.attr('string'),
  host: DS.belongsTo('App.Host'),
  hostName: DS.attr('string'),

  /**
   * @type {boolean}
   */
  isCurrent: Em.computed.equal('status', 'CURRENT'),

  /**
   * @type {boolean}
   */
  isInstalling: Em.computed.equal('status', 'INSTALLING'),

  /**
   * @type {boolean}
   */
  isOutOfSync: Em.computed.equal('status', 'OUT_OF_SYNC'),
  /**
   * @type {string}
   */
  displayStatus: function() {
    return App.HostStackVersion.formatStatus(this.get('status'));
  }.property('status'),

  /**
   * @type {boolean}
   */
  installEnabled: Em.computed.existsIn('status', ['OUT_OF_SYNC', 'INSTALL_FAILED']),

  installDisabled: function(){
    return !this.get('installEnabled') || App.router.get('wizardWatcherController.isNonWizardUser');
  }.property('installEnabled', 'App.routerwizardWatcherController.isNonWizardUser')
});

App.HostStackVersion.FIXTURES = [];

/**
 * definition of possible statuses of Stack Version
 * @type {Array}
 */
App.HostStackVersion.statusDefinition = [
  "INSTALLED",
  "INSTALLING",
  "INSTALL_FAILED",
  "OUT_OF_SYNC",
  "CURRENT",
  "UPGRADING",
  "UPGRADE_FAILED"
];

/**
 * translate status to label
 * @param status
 * @return {string}
 */
App.HostStackVersion.formatStatus = function (status) {
  return App.HostStackVersion.statusDefinition.contains(status) ?
    Em.I18n.t('hosts.host.stackVersions.status.' + status.toLowerCase()) :
    stringUtils.upperUnderscoreToText(status);
};
