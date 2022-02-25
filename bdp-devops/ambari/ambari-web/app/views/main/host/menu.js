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

App.MainHostMenuView = Em.CollectionView.extend({
  tagName: 'ul',
  classNames: ["nav", "nav-tabs", "background-text"],
  host: null,

  content: function () {
    return [
      Em.Object.create({
        name: 'summary',
        label: Em.I18n.t('common.summary'),
        routing: 'summary',
        id: 'host-details-summary-tab'
      }),
      Em.Object.create({
        name: 'configs',
        label: Em.I18n.t('common.configs'),
        routing: 'configs',
        id: 'host-details-summary-configs'
      }),
      Em.Object.create({
        name: 'alerts',
        label: Em.I18n.t('hosts.host.alerts.label'),
        routing: 'alerts',
        badgeText: '0',
        badgeClasses: 'label',
        id: 'host-details-summary-alerts'
      }),
      Em.Object.create({
        name: 'versions',
        label: Em.I18n.t('hosts.host.menu.stackVersions'),
        routing: 'stackVersions',
        hidden: !App.get('stackVersionsAvailable'),
        id: 'host-details-summary-version'
      }),
      Em.Object.create({
        name: 'logs',
        label: Em.I18n.t('hosts.host.menu.logs'),
        routing: 'logs',
        hidden: function () {
          if (App.get('supports.logSearch')) {
            return !(App.Service.find().someProperty('serviceName', 'LOGSEARCH') && App.isAuthorized('SERVICE.VIEW_OPERATIONAL_LOGS'));
          }
          return true;
        }.property('App.supports.logSearch'),
        id: 'host-details-summary-logs'
      })
    ];
  }.property('App.stackVersionsAvailable'),

  /**
   * Update Alerts menu option counter text and class
   */
  updateAlertCounter: function () {
    var criticalWarningCount = this.get('host.criticalWarningAlertsCount');
    var criticalCount = this.get('host.alertsSummary.CRITICAL');
    var warningCount = this.get('host.alertsSummary.WARNING');
    var badgeText = "" + criticalWarningCount;
    var badgeClasses = "label";
    if (criticalCount > 0) {
      badgeClasses += " alerts-crit-count";
    } else if (warningCount > 0) {
      badgeClasses += " alerts-warn-count";
    }
    var alertOption = this.get('content').findProperty('name', 'alerts');
    alertOption.set('badgeText', badgeText);
    alertOption.set('badgeClasses', badgeClasses);
  }.observes('host.alertsSummary.CRITICAL', 'host.alertsSummary.WARNING', 'host.criticalWarningAlertsCount'),

  init: function () {
    this._super();
    this.updateAlertCounter();
    this.activateView();
  },

  activateView: function () {
    var defaultRoute = App.router.get('currentState.name') || "summary";
    $.each(this._childViews, function () {
      this.set('active', this.get('content.routing') === defaultRoute ? 'active' : '');
    });
  }.observes('App.router.currentState.name'),

  deactivateChildViews: function () {
    this.get('_childViews').setEach('active', '');
  },

  itemViewClass: Em.View.extend({
    classNameBindings: ["active"],
    active: "",
    template: Ember.Handlebars.compile('{{#unless view.content.hidden}}<a {{action hostNavigate view.content.routing }} {{bindAttr id="view.content.id"}} href="#"> {{unbound view.content.label}} ' +
    '{{#if view.content.badgeText}} ' +
    '<span {{bindAttr class="view.content.badgeClasses"}}> ' +
    '{{view.content.badgeText}}' +
    '</span>  {{/if}}</a>{{/unless}}')
  })
});
