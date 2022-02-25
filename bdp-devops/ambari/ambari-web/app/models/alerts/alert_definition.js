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

function computedOnSummaryState(state) {
  return Em.computed('summary.' + state, 'summary.' + state + '.count', 'summary.' + state + '.maintenanceCount', function () {
    var summary = Em.get(this, 'summary');
    return !!summary[state] && !!(summary[state].count || summary[state].maintenanceCount);
  });
}

App.AlertDefinition = DS.Model.extend({

  name: DS.attr('string'),
  label: DS.attr('string'),
  description: DS.attr('string'),
  service: DS.belongsTo('App.Service'),
  serviceName: DS.attr('string'),
  componentName: DS.attr('string'),
  enabled: DS.attr('boolean'),
  repeat_tolerance_enabled: DS.attr('boolean'),
  repeat_tolerance: DS.attr('number'),
  scope: DS.attr('string'),
  interval: DS.attr('number'),
  type: DS.attr('string'),
  helpUrl: DS.attr('string'),
  groups: DS.hasMany('App.AlertGroup'),
  reporting: DS.hasMany('App.AlertReportDefinition'),
  parameters: DS.hasMany('App.AlertDefinitionParameter'),
  lastTriggered: 0,
  lastTriggeredRaw: 0,

  //relates only to SCRIPT-type alert definition
  location: DS.attr('string'),
  //relates only to AGGREGATE-type alert definition
  alertName: DS.attr('string'),
  //relates only to WEB and METRIC types of alert definition
  uri: DS.belongsTo('App.AlertMetricsUriDefinition'),
  //relates only METRIC-type alert definition
  jmx: DS.belongsTo('App.AlertMetricsSourceDefinition'),
  ganglia: DS.belongsTo('App.AlertMetricsSourceDefinition'),
  ams: DS.belongsTo('App.AlertMetricsAmsDefinition'),
  //relates only PORT-type alert definition
  defaultPort: DS.attr('number'),
  portUri: DS.attr('string'),

  /**
   * Raw data from AlertDefinition/source
   * used to format request content for updating alert definition
   * @type {Object}
   */
  rawSourceData: {},

  /**
   * Counts of alert grouped by their status
   * Format:
   * <code>
   *   {
   *    "CRITICAL": {
   *      count: 1,
   *      maintenanceCount: 0
   *    },
   *    "OK": {
   *      count: 0,
   *      maintenanceCount: 1
   *    },
   *    "UNKNOWN": {
   *      count: 0,
   *      maintenanceCount: 0
   *    },
   *    "WARNING": {
   *      count: 1,
   *      maintenanceCount: 1
   *    }
   *   }
   * </code>
   * @type {object}
   */
  summary: {},

  /**
   * Formatted timestamp for latest alert triggering for current alertDefinition
   * @type {string}
   */
  lastTriggeredFormatted: function () {
    let lastTriggered = this.get('lastTriggered');
    return lastTriggered ? dateUtils.dateFormat(lastTriggered) : '';
  }.property('lastTriggered'),

  /**
   * Formatted timestamp with <code>$.timeago</code>
   * @type {string}
   */
  lastTriggeredAgoFormatted: function () {
    var lastTriggered = this.get('lastTriggeredRaw');
    return lastTriggered ? $.timeago(new Date(lastTriggered)) : '';
  }.property('lastTriggeredRaw'),

  lastTriggeredVerboseDisplay: function () {
    var lastTriggered = this.get('lastTriggered');
    return Em.I18n.t('models.alert_definition.triggered.verbose').format(dateUtils.dateFormat(lastTriggered));
  }.property('lastTriggered'),

  /**
   * Formatted timestamp in format: for 4 days
   * @type {string}
   */
  lastTriggeredForFormatted: function () {
    var lastTriggered = this.get('lastTriggeredRaw');
    var previousSuffixAgo = $.timeago.settings.strings.suffixAgo;
    var previousPrefixAgo = $.timeago.settings.strings.prefixAgo;
    $.timeago.settings.strings.suffixAgo = null;
    $.timeago.settings.strings.prefixAgo = 'for';
    var triggeredFor = lastTriggered ? $.timeago(new Date(lastTriggered)) : '';
    $.timeago.settings.strings.suffixAgo = previousSuffixAgo;
    $.timeago.settings.strings.prefixAgo = previousPrefixAgo;
    return triggeredFor;
  }.property('lastTriggeredRaw'),

  /**
   * Formatted displayName for <code>componentName</code>
   * @type {String}
   */
  componentNameFormatted: Em.computed.formatRole('componentName', false),

  hostCnt: function () {
    var order = this.get('order'),
      summary = this.get('summary'),
      hostCnt = 0;
    order.forEach(function (state) {
      hostCnt += summary[state] ? summary[state].count + summary[state].maintenanceCount : 0;
    });
    return hostCnt;
  }.property('order', 'summary'),

  latestText: function () {
    var order = this.get('order'), summary = this.get('summary'), text = '';
    order.forEach(function (state) {
      var cnt = summary[state] ? summary[state].count + summary[state].maintenanceCount : 0;
      if (cnt > 0) {
        text = Em.getWithDefault(summary[state], 'latestText', '');
      }
    });
    return text;
  }.property('summary'),

  latestTextSummary: function () {
    var latestText = this.get('latestText');
    var ellipsis = '...';
    var summaryLength = 400;
    if(latestText.length > summaryLength) {
      latestText = latestText.substring(0, summaryLength - ellipsis.length) + ellipsis;
    }
    return latestText;
  }.property('latestText'),

  isAmbariService: Em.computed.equal('service._id', 'AMBARI'),

  isAmbariAgentComponent: Em.computed.equal('componentName', 'AMBARI_AGENT'),

  isHostAlertDefinition: Em.computed.and('isAmbariService', 'isAmbariAgentComponent'),

  typeIconClass: Em.computed.getByKey('typeIcons', 'type'),

  isTypeAggregate: Em.computed.equal('type', 'AGGREGATE'),

  /**
   * if this definition is in state: CRITICAL / WARNING, if true, will show up in alerts fast access popup
   * instances with maintenance mode ON are ignored
   * @type {boolean}
   */
  isCriticalOrWarning: Em.computed.or('summary.CRITICAL.count', 'summary.WARNING.count'),

  /**
   * if this definition is in state: CRITICAL
   * @type {boolean}
   */
  isCritical: computedOnSummaryState('CRITICAL'),

  /**
   * if this definition is in state: WARNING
   * @type {boolean}
   */
  isWarning: computedOnSummaryState('WARNING'),

  /**
   * if this definition is in state: OK
   * @type {boolean}
   */
  isOK: computedOnSummaryState('OK'),

  /**
   * if this definition is in state: UNKNOWN
   * @type {boolean}
   */
  isUnknown: computedOnSummaryState('UNKNOWN'),

  /**
   * For alerts we will have processes which are not typical
   * cluster services - like Ambari-Server. This method unifies
   * cluster services and other services into a common display-name.
   * @see App.AlertInstance#serviceDisplayName()
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
   * Determines if alert definition has help url
   * @type {boolean}
   */
  hasHelpUrl: Em.computed.bool('helpUrl'),

  /**
   * List of css-classes for alert types
   * @type {object}
   */
  typeIcons: {
    'METRIC': 'glyphicon glyphicon-flash',
    'SCRIPT': 'glyphicon glyphicon-file',
    'WEB': 'glyphicon glyphicon-globe',
    'PORT': 'glyphicon glyphicon-log-in',
    'AGGREGATE': 'glyphicon glyphicon-plus',
    'SERVER': 'glyphicon glyphicon-oil',
    'RECOVERY': 'glyphicon glyphicon-oil',
    'AMS': 'glyphicon glyphicon-stats'
  },

  /**
   * Sort on load definitions by this severity order
   */
  severityOrder: ['CRITICAL', 'WARNING', 'OK', 'UNKNOWN', 'PENDING'],
  order: ['OK', 'WARNING', 'CRITICAL', 'UNKNOWN']
});

App.AlertDefinition.shortState = {
  'CRITICAL': 'CRIT',
  'WARNING': 'WARN',
  'OK': 'OK',
  'UNKNOWN': 'UNKWN',
  'PENDING': 'NONE'
};

App.AlertDefinition.reopenClass({

  /**
   * Return function to sort list of AlertDefinitions by their status
   * It sorts according to <code>severityOrder</code>
   * @param {boolean} order true - DESC, false - ASC
   * @returns {Function}
   * @method getSortDefinitionsByStatus
   */
  getSortDefinitionsByStatus: function (order) {
    return function (a, b) {
      var aSummary = a.get('summary'),
        bSummary = b.get('summary'),
        stOrder = a.get('severityOrder'),
        ret = 0;
      for (var i = 0; i < stOrder.length; i++) {
        var aV = Em.isNone(aSummary[stOrder[i]]) ? 0 : aSummary[stOrder[i]].count + aSummary[stOrder[i]].maintenanceCount,
          bV = Em.isNone(bSummary[stOrder[i]]) ? 0 : bSummary[stOrder[i]].count + bSummary[stOrder[i]].maintenanceCount;
        ret = bV - aV;
        if (ret !== 0) {
          break;
        }
      }
      return order ? ret : -ret;
    };
  }

});

App.AlertDefinitionParameter = DS.Model.extend({
  name: DS.attr('string'),
  displayName: DS.attr('string'),
  units: DS.attr('string'),
  value: DS.attr('string'),
  description: DS.attr('string'),
  type: DS.attr('string'),
  threshold: DS.attr('string'),
  visibility: DS.attr('string')
});

App.AlertReportDefinition = DS.Model.extend({
  type: DS.attr('string'),
  text: DS.attr('string'),
  value: DS.attr('number')
});

App.AlertMetricsSourceDefinition = DS.Model.extend({
  propertyList: [],
  value: DS.attr('string')
});

App.AlertMetricsUriDefinition = DS.Model.extend({
  http: DS.attr('string'),
  https: DS.attr('string'),
  httpsProperty: DS.attr('string'),
  httpsPropertyValue: DS.attr('string'),
  connectionTimeout: DS.attr('number')
});

App.AlertMetricsAmsDefinition = DS.Model.extend({
  value: DS.attr('string'),
  minimalValue: DS.attr('number'),
  interval: DS.attr('number')
});

App.AlertDefinition.FIXTURES = [];
App.AlertReportDefinition.FIXTURES = [];
App.AlertMetricsSourceDefinition.FIXTURES = [];
App.AlertMetricsUriDefinition.FIXTURES = [];
App.AlertMetricsAmsDefinition.FIXTURES = [];
App.AlertDefinitionParameter.FIXTURES = [];


App.AlertType = DS.Model.extend({
  name: DS.attr('string'),
  displayName: DS.attr('string'),
  iconPath: DS.attr('string'),
  description: DS.attr('string'),
  properties: DS.attr('array')
});

App.AlertType.FIXTURES = [
  {
    id: 'PORT',
    name: 'PORT',
    icon_path: 'glyphicon glyphicon-log-in',
    display_name: 'Port',
    description: Em.I18n.t('alerts.add.wizard.step1.body.port.description')
  },
  {
    id: 'WEB',
    name: 'WEB',
    icon_path: 'glyphicon glyphicon-globe',
    display_name: 'Web',
    description: Em.I18n.t('alerts.add.wizard.step1.body.web.description')
  },
  {
    id: 'METRIC',
    name: 'METRIC',
    display_name: 'Metric',
    icon_path: 'glyphicon glyphicon-flash',
    description: Em.I18n.t('alerts.add.wizard.step1.body.metric.description')
  },
  {
    id: 'SCRIPT',
    name: 'SCRIPT',
    icon_path: 'glyphicon glyphicon-file',
    display_name: 'Script',
    description: Em.I18n.t('alerts.add.wizard.step1.body.script.description')
  },
  {
    id: 'AGGREGATE',
    name: 'AGGREGATE',
    icon_path: 'glyphicon glyphicon-plus',
    display_name: 'Aggregate',
    description: Em.I18n.t('alerts.add.wizard.step1.body.aggregate.description')
  },
  {
    id: 'RAW',
    name: 'RAW',
    icon_path: 'glyphicon glyphicon-align-justify',
    display_name: 'Raw',
    description: Em.I18n.t('alerts.add.wizard.step1.body.raw.description')
  }
];
