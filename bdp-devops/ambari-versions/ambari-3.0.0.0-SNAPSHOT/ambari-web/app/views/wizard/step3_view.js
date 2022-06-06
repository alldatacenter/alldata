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

App.WizardStep3View = App.TableView.extend({

  templateName: require('templates/wizard/step3'),

  /**
   * List of hosts
   * Same to <code>controller.hosts</code>
   * @type {Ember.Enumerable}
   */
  content:function () {
    return this.get('controller.hosts');
  }.property('controller.hosts.length'),

  /**
   * Message with info about registration result
   * @type {string}
   */
  message:'',

  /**
   * Text to link for hosts' warnings popup
   * @type {string}
   */
  linkText: '',

  /**
   * Registration status
   * @type {string}
   */
  status: '',

  /**
   * Active category
   * @type {string}
   */
  selectedCategory: Em.computed.findBy('categories', 'isActive', true),

  /**
   * Message about other registered hosts (not included in current registration)
   * @type {string}
   */
  registeredHostsMessage: '',

  /**
   * Number of visible hosts on page
   * @type {string}
   */
  displayLength: "25",

  /**
   * All checkboxes on page are checked
   * @type {bool}
   */
  pageChecked: false,

  /**
   * bootStatus category object
   * @type {Ember.Object}
   */
  categoryObject: Em.Object.extend({
    hostsCount: 0,
    label: Em.computed.format('{0} ({1})', 'value', 'hostsCount'),
    isActive: false,
    itemClass: Em.computed.ifThenElse('isActive', 'active', '')
  }),

  /**
   * List of bootStatus categories
   * @type {categoryObject[]}
   */
  categories: function () {
    return [
      this.categoryObject.create({value: Em.I18n.t('common.all'), hostsBootStatus: 'ALL', isActive: true}),
      this.categoryObject.create({value: Em.I18n.t('installer.step3.hosts.status.installing'), hostsBootStatus: 'RUNNING'}),
      this.categoryObject.create({value: Em.I18n.t('installer.step3.hosts.status.registering'), hostsBootStatus: 'REGISTERING'}),
      this.categoryObject.create({value: Em.I18n.t('common.success'), hostsBootStatus: 'REGISTERED' }),
      this.categoryObject.create({value: Em.I18n.t('common.fail'), hostsBootStatus: 'FAILED', last: true })
    ];
  }.property(),

  didInsertElement: function () {
    this.get('controller').loadStep();
    this.$().on('mouseover', '#confirm-hosts-table tr', function () {
      App.tooltip($(this).find('.action .glyphicon'), {placement: 'bottom'});
    });
  },

  /**
   * Select checkboxes of hosts on page
   * @method onPageChecked
   */
  onPageChecked: function () {
    if (this.get('selectionInProgress')) return;
    this.get('pageContent').setEach('isChecked', this.get('pageChecked'));
  }.observes('pageChecked'),

  /**
   * Select checkboxes of all hosts
   * @method selectAll
   */
  selectAll: function () {
    this.get('content').setEach('isChecked', true);
  },

  /**
   * Reset checkbox of all hosts
   * @method unSelectAll
   */
  unSelectAll: function() {
    this.get('content').setEach('isChecked', false);
  },

  /**
   * Call <code>watchSelection</code> only once
   * @method watchSelectionOnce
   */
  watchSelectionOnce: function () {
    Em.run.once(this, 'watchSelection');
  }.observes('content.@each.isChecked', 'content.@each.bootStatus', 'pageContent'),

  /**
   * Watch selection and calculate such flags as:
   * <ul>
   *  <li>noHostsSelected</li>
   *  <li>selectedHostsCount</li>
   *  <li>pageChecked</li>
   * </ul>
   * @method watchSelection
   */
  watchSelection: function() {
    this.set('selectionInProgress', true);
    this.set('pageChecked', !!this.get('pageContent.length') && this.get('pageContent').everyProperty('isChecked', true));
    this.set('selectionInProgress', false);
    var noNotRunningHostsSelected = true;
    var selectedNotRunningHostsCount = 0;
    var selectedHostsCount = 0;
    this.get('content').forEach(function(host){
      var isSelectedAndNotRunning = host.get('isChecked') && (host.get('bootStatus') !== 'RUNNING');
      selectedHostsCount += host.get('isChecked') ? 1 : 0;
      selectedNotRunningHostsCount += isSelectedAndNotRunning ? 1 : 0;
      noNotRunningHostsSelected = (noNotRunningHostsSelected) ? !isSelectedAndNotRunning : noNotRunningHostsSelected;
    });
    this.set('noNotRunningHostsSelected', noNotRunningHostsSelected);
    this.set('selectedNotRunningHostsCount', selectedNotRunningHostsCount);
    this.set('selectedHostsCount', selectedHostsCount);
  },

  /**
   * Update <code>registeredHostsMessage</code> according to <code>controller.registeredHots.length</code>
   * @method setRegisteredHosts
   */
  setRegisteredHosts: function(){
    this.set('registeredHostsMessage',Em.I18n.t('installer.step3.warning.registeredHosts').format(this.get('controller.registeredHosts').length));
  }.observes('controller.registeredHosts'),

  /**
   * Call filters and counters one time
   * @method hostBootStatusObserver
   */
  hostBootStatusObserver: function(){
    Em.run.once(this, 'countCategoryHosts');
    Em.run.once(this, 'filter');
    Em.run.once(this, 'monitorStatuses');
  }.observes('content.@each.bootStatus'),

  /**
   * Calculate host count grouped by <code>bootStatus</code>
   * @method countCategoryHosts
   */
  countCategoryHosts: function () {
    var counters = {
      "RUNNING": 0,
      "REGISTERING": 0,
      "REGISTERED": 0,
      "FAILED": 0
    };
    this.get('content').forEach(function (host) {
      if (!Em.isNone(counters[host.get('bootStatus')])) {
        counters[host.get('bootStatus')]++;
      }
    }, this);
    counters["ALL"] = this.get('content.length');
    this.get('categories').forEach(function(category) {
      category.set('hostsCount', counters[category.get('hostsBootStatus')]);
    }, this);
  },


  /**
   * Filter hosts by category
   * @method filter
   */
  filter: function () {
    var self = this;
    Em.run.next(function () {
      self.doFilter();
    });
  }.observes('selectedCategory'),

  /**
   * Real filter-method
   * Called from <code>filter</code> in Em.run.next-wrapper
   * @method doFilter
   */
  doFilter: function() {
    var result = [];
    var selectedCategory = this.get('selectedCategory');
    if (!selectedCategory || selectedCategory.get('hostsBootStatus') === 'ALL') {
      result = this.get('content');
    } else {
      result = this.get('content').filterProperty('bootStatus', this.get('selectedCategory.hostsBootStatus'));
    }
    this.set('filteredContent', result);
  },

  /**
   * Trigger on Category click
   * @param {Object} event
   * @method selectCategory
   */
  selectCategory: function (event) {
    var categoryStatus = event.context.get('hostsBootStatus');
    this.get('categories').forEach(function (category) {
      category.set('isActive', (category.get('hostsBootStatus') === categoryStatus));
    });
    this.watchSelection();
  },

  /**
   * Select "All" hosts category
   * run registration of failed hosts again
   * @method retrySelectedHosts
   */
  retrySelectedHosts: function () {
    var eventObject = {context: Em.Object.create({hostsBootStatus: 'ALL'})};
    this.selectCategory(eventObject);
    this.get('controller').retrySelectedHosts();
  },

  /**
   * Update <code>status</code>, <code>linkText</code>, <code>message</code> according to hosts statuses
   * @method monitorStatuses
   */
  monitorStatuses: function() {
    var hosts = this.get('controller.bootHosts');
    var failedHosts = hosts.filterProperty('bootStatus', 'FAILED').length;

    if (hosts.length === 0) {
      this.set('status', 'alert-warning');
      this.set('linkText', '');
      this.set('message', Em.I18n.t('installer.step3.warnings.missingHosts'));
    }
    else {
      if (!this.get('controller.isWarningsLoaded')) {
        this.set('status', 'alert-info');
        this.set('linkText', '');
        this.set('message', Em.I18n.t('installer.step3.warning.loading'));
      }
      else {
        if (this.get('controller.isHostHaveWarnings') || this.get('controller.repoCategoryWarnings.length') || this.get('controller.diskCategoryWarnings.length') || this.get('controller.jdkCategoryWarnings.length')
          || this.get('controller.hostCheckWarnings.length') || this.get('controller.thpCategoryWarnings.length')) {
          this.set('status', 'alert-warning');
          this.set('linkText', Em.I18n.t('installer.step3.warnings.linkText'));
          this.set('message', Em.I18n.t('installer.step3.warnings.fails').format(hosts.length - failedHosts));
        }
        else {
          this.set('status', 'alert-success');
          this.set('linkText', Em.I18n.t('installer.step3.noWarnings.linkText'));
          if (failedHosts == 0) {
            // all are ok
            this.set('message', Em.I18n.t('installer.step3.warnings.noWarnings').format(hosts.length));
          }
          else {
            if (failedHosts == hosts.length) {
              // all failed
              this.set('status', 'alert-warning');
              this.set('linkText', '');
              this.set('message', Em.I18n.t('installer.step3.warnings.allFailed').format(failedHosts));
            }
            else {
              // some failed
              this.set('message', Em.I18n.t('installer.step3.warnings.someWarnings').format((hosts.length - failedHosts), failedHosts));
            }
          }
        }
      }
    }

  }.observes('controller.isWarningsLoaded','controller.hostCheckWarnings', 'controller.isHostHaveWarnings', 'controller.repoCategoryWarnings', 'controller.diskCategoryWarnings', 'controller.jdkCategoryWarnings', 'controller.thpCategoryWarnings')
});

//todo: move it inside WizardStep3View
App.WizardHostView = Em.View.extend({

  tagName: 'tr',

  classNameBindings: ['hostInfo.bootStatus', 'hostInfo.isChecked:active'],

  /**
   * Host from parent view
   * @type {Object}
   */
  hostInfo: null,

  /**
   * Remove selected host
   * @method remove
   */
  remove: function () {
    this.get('controller').removeHost(this.get('hostInfo'));
  },

  /**
   * Retry register selected host
   * @method retry
   */
  retry: function() {
    this.get('controller').retryHost(this.get('hostInfo'));
  }

});


