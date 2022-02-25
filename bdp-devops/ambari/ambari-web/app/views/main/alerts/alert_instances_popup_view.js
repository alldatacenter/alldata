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

/**
 * Option for "filter by state" drop down
 * @typedef {object} categoryObject
 * @property {string} value "all|warning|critical"
 * @property {number} count number of items with <code>state</code> equal to <code>this.value</code>
 * @property {string} labelPath key in the messages.js
 * @property {string} label localized label
 */
var categoryObject = Em.Object.extend({
  value: '',
  count: 0,
  labelPath: '',
  label: function () {
    return Em.I18n.t(this.get('labelPath')).format(this.get('count'));
  }.property('count', 'labelPath')
});

App.AlertInstancesPopupView = App.TableView.extend(App.TableServerViewMixin, {

  templateName: require('templates/main/alerts/alert_notifications_popup'),

  updaterBinding: 'App.router.updateController',

  controllerBinding: 'App.router.mainAlertInstancesController',

  /**
   * Number of all critical and warning alert instances
   * @type {Boolean}
   */
  alertsNumberBinding: 'App.router.mainAlertDefinitionsController.unhealthyAlertInstancesCount',
  criticalNumberBinding: 'App.router.mainAlertDefinitionsController.criticalAlertInstancesCount',
  warningNumberBinding: 'App.router.mainAlertDefinitionsController.warningAlertInstancesCount',

  isPaginate: false,

  willInsertElement: function () {
    this._super();
    this.updateAlertInstances();
  },

  didInsertElement: function () {
    this.filter();
    this.addObserver('filteringComplete', this, this.overlayObserver);
    this.overlayObserver();
    this.$(".filter-select ").click((event) => event.stopPropagation());
    return this._super();
  },

  content: function () {
    return this.get('controller.unhealthyAlertInstances');
  }.property('controller.unhealthyAlertInstances.@each.state'),

  isLoaded: Em.computed.bool('controller.unhealthyAlertInstances'),

  isAlertEmptyList: Em.computed.or('isAlertContentEmptyList', 'isAlertFilterdEmptyList'),
  isAlertContentEmptyList: Em.computed.empty('content'),
  isAlertFilterdEmptyList:  Em.computed.everyBy('content', 'isVisible', false),

  displayLength: 100,

  /**
   * Update list of shown alert instances
   * @method updateAlertInstances
   */
  updateAlertInstances: function () {
    var self = this,
      displayLength = this.get('displayLength'),
      startIndex = this.get('startIndex');
    if (!displayLength) return; // wait while table-info is loaded
    this.get('updater').set('queryParamsForUnhealthyAlertInstances', {
      from: startIndex - 1,
      page_size: displayLength
    });
    this.set('filteringComplete', false);
    this.get('updater').updateUnhealthyAlertInstances(function() {
      self.set('filteringComplete', true);
    });
  }.observes('displayLength', 'startIndex', 'alertsNumber'),

  /**
   * Show spinner when filter/sorting request is in processing
   * @method overlayObserver
   */
  overlayObserver: function() {
    var $tbody = this.$('.table.alerts-table'),
      $overlay = this.$('.table-overlay'),
      $spinner = $($overlay).find('.spinner');
    if (!this.get('filteringComplete')) {
      if (!$tbody) return;
      var tbodyPos =  $tbody.position();
      if (!tbodyPos) return;
      $spinner.css('display', 'block');
      $overlay.css({
        top: tbodyPos.top + 1,
        left: tbodyPos.left + 1,
        width: $tbody.width() - 1,
        height: $tbody.height() - 1
      });
    }
  },

  categories: function () {
    var allCnt = this.get('alertsNumber');
    var criticalCnt = this.get('criticalNumber');
    var warningCnt = this.get('warningNumber');
    return [
      categoryObject.create({
        value: 'all',
        labelPath: 'alerts.dropdown.dialog.filters.all',
        count: allCnt
      }),
      categoryObject.create({
        value: 'alert-state-CRITICAL',
        labelPath: 'alerts.dropdown.dialog.filters.critical',
        count: criticalCnt
      }),
      categoryObject.create({
        value: 'alert-state-WARNING',
        labelPath: 'alerts.dropdown.dialog.filters.warning',
        count: warningCnt
      })
    ];
  }.property('criticalNumber', 'warningNumber', 'alertsNumber'),

  selectedCategory: null,

  /**
   * Filter notifications by state
   * @method filter
   */
  filter: function() {
    var selectedState = this.get('selectedCategory.value');
    var content = this.get('content');
    if (selectedState == 'all') {
      content.setEach("isVisible", true);
      this.set('filteredContent', content);
    } else {
      this.set('filteredContent', content.filter(function (item) {
        if (item.get('stateClass') == selectedState) {
          item.set('isVisible', true);
          return true;
        } else {
          item.set('isVisible', false);
          return false;
        }
      }));
    }
  }.observes('content.length', 'selectedCategory'),

  /**
   * Router transition to alert definition details page
   * @param event
   */
  gotoAlertDetails: function (event) {
    if (event && event.context) {
      var definition = App.AlertDefinition.find().findProperty('id', event.context.get('definitionId'));
      App.router.transitionTo('main.alerts.alertDetails', definition);
    }
  },

  /**
   * Router transition to alert definition page
   */
  gotoAllAlerts: function () {
    App.router.transitionTo('main.alerts.index');
  }

});
