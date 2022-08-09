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
var dateUtils = require('utils/date/date');

App.AlertInstance = DS.Model.extend({
  id: DS.attr('number'),
  label: DS.attr('string'),
  definitionName: DS.attr('string'),
  definitionId: DS.attr('number'),
  service: DS.belongsTo('App.Service'),
  serviceName: DS.attr('string'),
  componentName: DS.attr('string'),
  host: DS.belongsTo('App.Host'),
  hostName: DS.attr('string'),
  scope: DS.attr('string'),
  originalTimestamp: DS.attr('number'),
  originalRawTimestamp: DS.attr('number'),
  latestTimestamp: DS.attr('number'),
  maintenanceState: DS.attr('string'),
  instance: DS.attr('string'),
  state: DS.attr('string'),
  text: DS.attr('string'),
  repeatTolerance: DS.attr('number'),
  repeatToleranceRemaining: DS.attr('number'),
  notification: DS.hasMany('App.AlertNotification'),

  /**
   * @type {boolean}
   */
  isMaintenanceStateOn: Em.computed.equal('maintenanceState', 'ON'),

  /**
   * @type {string}
   */
  shortStateMsg: Em.computed.getByKey('shortState', 'state'),

  /**
   * @type {string}
   */
  stateClass: function () {
    return 'alert-state-' + (this.get('isMaintenanceStateOn') ? 'PENDING' : this.get('state'));
  }.property('isMaintenanceStateOn'),

  /**
   * For alerts we will have processes which are not typical
   * cluster services - like Ambari-Server. This method unifies
   * cluster services and other services into a common display-name.
   * @see App.AlertDefinition#serviceDisplayName()
   */
  serviceDisplayName: function () {
    var serviceName = this.get('service.displayName');
    if (!serviceName) {
      serviceName = this.get('serviceName');
      if (serviceName) {
        serviceName = serviceName.toCapital();
      }
    }
    return serviceName;
  }.property('serviceName', 'service.displayName'),

  /**
   * Formatted timestamp for latest instance triggering
   * @type {string}
   */
  lastCheckedFormatted: function () {
    return dateUtils.dateFormat(this.get('latestTimestamp'));
  }.property('latestTimestamp'),

  /**
   * Formatted timestamp for latest instance triggering
   * @type {string}
   */
  lastTriggeredFormatted: function () {
    return dateUtils.dateFormat(this.get('originalTimestamp'));
  }.property('originalTimestamp'),

  /**
   * Formatted timestamp with <code>$.timeago</code>
   * @type {string}
   */
  lastTriggeredAgoFormatted: function () {
    var lastTriggered = this.get('originalRawTimestamp');
    return lastTriggered ? $.timeago(new Date(lastTriggered)) : '';
  }.property('originalTimestamp'),

  lastTriggeredVerboseDisplay: function () {
    var originalTimestamp = this.get('originalTimestamp');
    var latestTimestamp = this.get('latestTimestamp');
    return Em.I18n.t('models.alert_instance.tiggered.verbose').format(
        dateUtils.dateFormat(originalTimestamp),
        dateUtils.dateFormat(latestTimestamp));
  }.property('originalTimestamp', 'latestTimestamp'),

  /**
   * Formatted timestamp with <code>$.timeago</code>
   * @type {string}
   */
  lastTriggeredForFormatted: function () {
    var lastTriggered = this.get('originalRawTimestamp');
    var previousSuffixAgo = $.timeago.settings.strings.suffixAgo;
    var previousPrefixAgo = $.timeago.settings.strings.prefixAgo;
    $.timeago.settings.strings.suffixAgo = null;
    $.timeago.settings.strings.prefixAgo = 'for';
    var triggeredFor = lastTriggered ? $.timeago(new Date(lastTriggered)) : '';
    $.timeago.settings.strings.suffixAgo = previousSuffixAgo;
    $.timeago.settings.strings.prefixAgo = previousPrefixAgo;
    return triggeredFor;
  }.property('originalTimestamp'),

  /**
  * escaped '<' and '>' special characters.
  * @type {string}
  */  
  escapeSpecialCharactersFromTooltip: function () {
    var displayedText = this.get('text');
    return displayedText.replace(/[<>]/g, '');
  }.property('text'),

  /**
   * Formatted lastChecked and lastTriggered timestamp
   * @returns {string}
   */
  statusChangedAndLastCheckedFormatted: Em.computed.i18nFormat('models.alert_definition.triggered.checked', 'lastTriggeredFormatted', 'lastCheckedFormatted'),

  /**
   * List of css-classes for alert instance status
   * @type {object}
   */
  typeIcons: {
    'DISABLED': 'glyphicon glyphicon-off'
  },

  repeatToleranceReceived: function () {
    return this.get('repeatTolerance') - this.get('repeatToleranceRemaining');
  }.property('repeatToleranceRemaining', 'repeatTolerance'),

  retryText: function () {
    return this.get('state') === 'OK' ? '' : Em.I18n.t('models.alert_definition.check.retry').format(this.get('repeatToleranceReceived'), this.get('repeatTolerance'));
  }.property('state','repeatToleranceRemaining', 'repeatTolerance'),

  /**
   * Define if definition serviceName is Ambari
   * Used in some logic in templates to distinguish definitions with Ambari serviceName
   * @returns {boolean}
   */
  isAmbariServiceName: Em.computed.equal('serviceName', 'AMBARI'),

  shortState: {
    'CRITICAL': 'CRIT',
    'WARNING': 'WARN',
    'OK': 'OK',
    'UNKNOWN': 'UNKWN',
    'PENDING': 'NONE'
  }
});

App.AlertInstance.FIXTURES = [];
