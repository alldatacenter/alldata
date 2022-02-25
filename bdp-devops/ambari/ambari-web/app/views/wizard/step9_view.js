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
var date = require('utils/date/date');

App.WizardStep9View = App.TableView.extend({

  templateName: require('templates/wizard/step9'),

  /**
   * Overall progress-bar color
   * @type {string}
   */
  barColor: '',

  /**
   * Overall result message
   * @type {string}
   */
  resultMsg: '',

  /**
   * Overall message color
   * @type {string}
   */
  resultMsgColor: '',

  /**
   * When progress is 100, step is completed
   * @type {bool}
   */
  isStepCompleted: Em.computed.equal('controller.progress', '100'),

  /**
   * Number of visible hosts
   * @type {string}
   */
  displayLength: "25",

  /**
   * Same to <code>controller.hosts</code>
   * @type {object[]}
   */
  content: function () {
    return this.get('controller.hosts');
  }.property('controller.hosts'),

  /**
   * Active category
   * @type {Ember.Object}
   */
  selectedCategory: Em.computed.findBy('categories', 'isActive', true),

  /**
   * Ember Object category. This object also contains
   * <code>
   *   hostStatus: {String} A valid status of a host.
   *               Used to filter hosts in that status.
   *   hostsCount: {Int} Dynamic count of hosts displayed in the category label
   *   label : {String} status and hosts in that status displayed which consists as a category on the page
   *   isActive: {boolean} Gets set when the category is selected/clicked by the user
   *   itemClass: {computed property} Binds the category link to active class when user clicks on the link
   * </code>
   */
  categoryObject: Em.Object.extend({
    hostsCount: 0,
    label: Em.computed.format('{0} ({1})', 'value', 'hostsCount'),
    isActive: false,
    itemClass: Em.computed.ifThenElse('isActive', 'active', '')
  }),

  /**
   * Domputed property creates the category objects on the load of the page and sets 'All' as the active category
   * @Returns {Em.Object[]} All created categories which are binded and iterated in the template
   */
  categories: function () {
    return [
      this.categoryObject.create({value: Em.I18n.t('common.all'), hostStatus: 'all', isActive: true}),
      this.categoryObject.create({value: Em.I18n.t('installer.step9.hosts.status.label.inProgress'), hostStatus: 'inProgress'}),
      this.categoryObject.create({value: Em.I18n.t('installer.step9.hosts.status.label.warning'), hostStatus: 'warning'}),
      this.categoryObject.create({value: Em.I18n.t('common.success'), hostStatus: 'success'}),
      this.categoryObject.create({value: Em.I18n.t('common.fail'), hostStatus: 'failed', last: true })
    ];
  }.property(),

  /**
   * True if <code>controller.hostsWithHeartbeatLost</code> contains some values
   * @type {bool}
   */
  isHostHeartbeatLost: Em.computed.bool('controller.hostsWithHeartbeatLost.length'),

  /**
   * Css-string to overall progress-bar width-property
   * @type {string}
   */
  barWidth: Em.computed.format('width: {0}%;', 'controller.progress'),

  /**
   * Filter hosts info shown up on bottom of the box. Set by filter function, when 'seletedCategory' changed
   * @type {string}
   */
  filteredHostsInfo: '',

  /**
   * Message for overall progress
   * @type {string}
   */
  progressMessage: Em.computed.i18nFormat('installer.step9.overallProgress', 'controller.progress'),

  /**
   * Run <code>countCategoryHosts</code>, <code>filter</code> only once
   * @method hostStatusObserver
   */
  hostStatusObserver: function(){
    Em.run.once(this, 'countCategoryHosts');
    Em.run.once(this, 'filter');
  }.observes('content.@each.status'),

  /**
   * Count each category hosts to update label
   * @method countCategoryHosts
   */
  countCategoryHosts: function () {
    var counters = {
      "info": 0,
      "pending": 0,
      "in_progress": 0,
      "heartbeat_lost": 0,
      "warning": 0,
      "success": 0,
      "failed": 0
    };

    this.get('content').forEach(function (host) {
      if (counters[host.get('status')] !== undefined) {
        counters[host.get('status')]++;
      }
    }, this);
    counters["all"] = this.get('content.length');
    counters["inProgress"] = counters["info"] + counters["pending"] + counters["in_progress"];
    counters["failed"] += counters["heartbeat_lost"];
    this.get('categories').forEach(function(category) {
      category.set('hostsCount', counters[category.get('hostStatus')]);
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
    var content = this.get('content');
    var selectedCategory = this.get('selectedCategory');
    if (!selectedCategory || selectedCategory.get('hostStatus') === 'all') {
      result = content;
    } else if (selectedCategory.get('hostStatus') == 'inProgress') {
      result = content.filter(function (_host) {
        return (_host.get('status') == 'info' || _host.get('status') == 'pending' || _host.get('status') == 'in_progress');
      });
    } else if (selectedCategory.get('hostStatus') == 'failed') {
      result = content.filter(function (_host) {
        return (_host.get('status') == 'failed' || _host.get('status') == 'heartbeat_lost');
      });
    } else {
      result = content.filterProperty('status', selectedCategory.get('hostStatus'));
    }
    this.set('filteredContent', result);
    this.set('filteredHostsInfo', Em.I18n.t('installer.step9.hosts.filteredHostsInfo').format(result.get('length'), content.get('length')));
  },

  /**
   * On click handler for 'show all' link
   * @method showAllHosts
   */
  showAllHosts: function () {
    this.get('categories').forEach(function (category) {
      category.set('isActive', (category.get('hostStatus') === 'all'));
    });
  },

  /**
   * Trigger on Category click
   * @param {Object} event
   * @method selectCategory
   */
  selectCategory: function (event) {
    var categoryStatus = event.context.get('hostStatus');
    this.get('categories').forEach(function (category) {
      category.set('isActive', (category.get('hostStatus') === categoryStatus));
    });
  },

  didInsertElement: function () {
    this.onStatus();
    this.get('controller').navigateStep();
  },

  /**
   * Set <code>resultMsg</code>, <code>resultMsg</code>, <code>resultMsgColor</code> according to
   * <code>controller.status</code>, <code>controller.startCallFailed</code>, <code>isHostHeartbeatLost</code>
   * @method onStatus
   */
  onStatus: function () {
    if (this.get('controller.status') === 'info') {
      this.set('resultMsg', '');
      this.set('barColor', 'progress-bar-info');
    } else if (this.get('controller.status') === 'warning') {
      this.set('barColor', 'progress-bar-warning');
      this.set('resultMsg', Em.I18n.t('installer.step9.status.warning'));
      this.set('resultMsgColor', 'alert-warning');
    } else if (this.get('controller.status') === 'failed') {
      this.set('barColor', 'progress-bar-danger');
      this.set('resultMsgColor', 'alert-danger');
      if (this.get('isHostHeartbeatLost')) {
        // When present requests succeeds but some host components are in UNKNOWN or INSTALL_FAILED state and
        // hosts are in HEARTBEAT_LOST state
        this.set('resultMsg', Em.I18n.t('installer.step9.status.hosts.heartbeat_lost').format(this.get('controller.hostsWithHeartbeatLost').length));
      } else if (this.get('controller.startCallFailed')) {
        this.set('resultMsg', Em.I18n.t('installer.step9.status.start.services.failed'));
      } else {
        this.set('resultMsg', Em.I18n.t('installer.step9.status.failed'));
      }
    } else if (this.get('controller.status') === 'success') {
      this.set('barColor', 'progress-bar-success');
      this.set('resultMsg', this.get('controller.content.cluster.status') === 'START_SKIPPED' ? Em.I18n.t('installer.step9.status.skipStartSuccess') : Em.I18n.t('installer.step9.status.success'));
      this.set('resultMsgColor', 'alert-success');
    }
  }.observes('controller.status', 'controller.content.cluster.status', 'controller.startCallFailed','isHostHeartbeatLost'),

  /**
   * Show popup with info about failed hosts
   * @return {App.ModalPopup}
   * @method hostWithInstallFailed
   */
  hostWithInstallFailed: function () {
    var controller = this.get('controller');
    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step9.host.heartbeat_lost.header'),
      classNames: ['common-modal-wrapper'],
      modalDialogClasses: ['modal-lg'],
      autoHeight: false,
      secondary: null,

      bodyClass: Em.View.extend({
        templateName: require('templates/wizard/step9/step9_install_host_popup'),
        c: controller,
        failedHosts: function () {
          return controller.get('hostsWithHeartbeatLost');
        }.property()
      })
    });
  }

});

function hostStatus(statuses) {
  return Em.computed('isHostCompleted', 'obj.status', function () {
    statuses = Em.makeArray(statuses);
    return this.get('isHostCompleted') && statuses.contains(this.get('obj.status'));
  });
}

App.HostStatusView = Em.View.extend({

  tagName: 'tr',

  /**
   * Current host
   * @type {Em.Object}
   */
  obj: null,

  /**
   * wizardStep9Controller
   * @type {App.WizardStep9Controller}
   */
  controller: null,

  /**
   * Progress-bar color for current host
   * @type {string}
   */
  barColor: '',

  /**
   * Css-string to progress-bar width-property
   * @type {string}
   */
  barWidth: Em.computed.format('width: {0}%;','obj.progress'),

  /**
   * Is current host failed
   * @type {bool}
   */
  isFailed: hostStatus(['failed', 'heartbeat_lost']),

  /**
   * Is current host successfully installed
   * @type {bool}
   */
  isSuccess: hostStatus('success'),

  /**
   * Current host has warnings
   * @type {bool}
   */
  isWarning: hostStatus('warning'),

  /**
   * Current host completed all its tasks
   * @type {bool}
   */
  isHostCompleted: function () {
    return this.get('obj.progress') == 100;
  }.property('obj.progress'),

  didInsertElement: function () {
    this.onStatus();
  },

  /**
   * Set <code>barColor</code>, <code>obj.progress</code>, <code>obj.message</code> according to
   * <code>obj.status</code>, <code>obj.progress</code>, <code>controller.progress</code>
   * @method onStatus
   */
  onStatus: function () {
    if (this.get('obj.status') === 'info') {
      this.set('barColor', 'progress-bar-info');
    } else if (this.get('obj.status') === 'warning') {
      this.set('barColor', 'progress-bar-warning');
      if (this.get('obj.progress') === '100') {
        this.set('obj.message', Em.I18n.t('installer.step9.host.status.warning'));
      }
    } else if (this.get('obj.status') === 'failed') {
      this.set('barColor', 'progress-bar-danger');
      if (this.get('obj.progress') === '100') {
        this.set('obj.message', Em.I18n.t('installer.step9.host.status.failed'));
      }
    } else if (this.get('obj.status') === 'heartbeat_lost') {
      this.set('barColor', 'progress-bar-danger');
      if (this.get('obj.progress') === '100') {
        this.set('obj.message', Em.I18n.t('installer.step9.host.heartbeat_lost'));
      }
    } else if (this.get('obj.status') === 'success' && this.get('isHostCompleted') && parseInt(this.get('controller.progress')) > 34) {
        this.set('barColor', 'progress-bar-success');
        this.set('obj.message', Em.I18n.t('installer.step9.host.status.success'));
    }
  }.observes('obj.status', 'obj.progress', 'controller.progress'),

  /**
   * Show popup with host logs
   * @return {App.ModalPopup}
   * @method hostLogPopup
   */
  hostLogPopup: function () {
    var controller = this.get('controller');
    var host = this.get('obj');

    return App.ModalPopup.show({

      header: host.get('name'),

      classNames: ['common-modal-wrapper'],
      modalDialogClasses: ['modal-lg'],
      'data-qa': 'host-log-modal',

      autoHeight: false,

      secondary: null,

      /**
       * Current host
       * @type {Em.Object}
       */
      host: host,

      /**
       * wizardStep9Controller
       * @type {App.WizardStep9Controller}
       */
      c: controller,

      onClose: function () {
        this.set('c.currentOpenTaskId', 0);
        this.hide();
      },

      bodyClass: App.WizardStep9HostLogPopupBodyView
    });
  }

});
