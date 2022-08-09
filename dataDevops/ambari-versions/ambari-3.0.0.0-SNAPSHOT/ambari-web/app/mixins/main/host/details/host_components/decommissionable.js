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
var uiEffects = require('utils/ui_effects');

/**
 * Mixin for <code>App.HostComponentView</code>
 * Contains code for processing components with allowed decommission
 * @type {Em.Mixin}
 */
App.Decommissionable = Em.Mixin.create({

  /**
   * Should be redeclared in views that use this mixin
   * @type {String}
   */
  componentForCheckDecommission: '',

  /**
   * Is component in decommission process right know
   * @type {boolean}
   */
  isComponentDecommissioning: false,

  /**
   * May component be decommissioned
   * @type {boolean}
   */
  isComponentDecommissionAvailable: false,

  /**
   * May component be recommissioned
   * @type {boolean}
   */
  isComponentRecommissionAvailable: false,

  /**
   * Timer id for decommission status polling
   * @type {number}
   */
  decommissionStatusPollingTimer: null,

  /**
   * Interval for decommission status polling
   * @type {number}
   */
  POLLING_INTERVAL: 6000,

  /**
   * Component with stopped masters can't be docommissioned
   * @type {bool}
   */
  isComponentDecommissionDisable: function () {
    var masterComponent = this.get('content.service.hostComponents').findProperty('componentName', this.get('componentForCheckDecommission'));
    if (masterComponent && masterComponent.get('workStatus') != App.HostComponentStatus.started) return true;
    return this.get('content.service.workStatus') != App.HostComponentStatus.started;
  }.property('content.service.workStatus', 'content.service.hostComponents.@each.workStatus'),

  /**
   * @override App.HostComponentView.isRestartableComponent
   */
  isRestartableComponent: function () {
    return this.get('isComponentDecommissionAvailable') && App.get('components.restartable').contains(this.get('content.componentName'));
  }.property('isComponentDecommissionAvailable'),

  /**
   * Tooltip message shows if decommission/recommission is disabled
   * when masters for current component is down
   * @type {string}
   */
  decommissionTooltipMessage: function () {
    if (this.get('isComponentDecommissionDisable') && (this.get('isComponentRecommissionAvailable') || this.get('isComponentDecommissionAvailable'))) {
      var decom = this.get('isComponentRecommissionAvailable') ? Em.I18n.t('common.recommission') : Em.I18n.t('common.decommission');
      return Em.I18n.t('hosts.decommission.tooltip.warning').format(decom, App.format.role(this.get('componentForCheckDecommission'), false));
    }
    return '';
  }.property('isComponentDecommissionDisable', 'isComponentRecommissionAvailable', 'isComponentDecommissionAvailable', 'componentForCheckDecommission'),
  /**
   * Recalculated component status based on decommission
   * @type {string}
   */
  statusClass: function () {

    //Class when install failed
    if (this.get('workStatus') === App.HostComponentStatus.install_failed) {
      return 'health-status-color-red glyphicon glyphicon-cog';
    }

    //Class when installing
    if (this.get('workStatus') === App.HostComponentStatus.installing) {
      return 'health-status-color-blue glyphicon glyphicon-cog';
    }

    if (this.get('isComponentRecommissionAvailable') && (this.get('isStart') || this.get('workStatus') == 'INSTALLED')) {
      return 'health-status-DEAD-ORANGE';
    }

    //For all other cases
    return 'health-status-' + App.HostComponentStatus.getKeyName(this.get('workStatus'));

  }.property('workStatus', 'isComponentRecommissionAvailable', 'isComponentDecommissioning'),

  /**
   * Return host component text status
   * @type {String}
   */
  componentTextStatus: function () {
    let componentTextStatus = this.get('content.componentTextStatus');
    if (this.get('isComponentRecommissionAvailable')) {
      if (this.get('isComponentDecommissioning')) {
        componentTextStatus = Em.I18n.t('hosts.host.decommissioning');
      } else {
        componentTextStatus = Em.I18n.t('hosts.host.decommissioned');
      }
    }
    return componentTextStatus;
  }.property('workStatus', 'isComponentRecommissionAvailable', 'isComponentDecommissioning'),

  /**
   * For Stopping or Starting states, also for decommissioning
   * @type {bool}
   */
  isInProgress: function () {
    return (this.get('workStatus') === App.HostComponentStatus.stopping ||
      this.get('workStatus') === App.HostComponentStatus.starting) ||
      this.get('isDecommissioning');
  }.property('workStatus', 'isDecommissioning'),

  /**
   * load Recommission/Decommission status of component
   */
  loadComponentDecommissionStatus: function () {
    return this.getDesiredAdminState();
  },

  /**
   * Get desired_admin_state status from server
   */
  getDesiredAdminState: function () {
    if (Em.isNone(this.get('content'))) return null;
    return App.ajax.send({
      name: 'host.host_component.slave_desired_admin_state',
      sender: this,
      data: {
        hostName: this.get('content.hostName'),
        componentName: this.get('content.componentName')
      },
      success: 'getDesiredAdminStateSuccessCallback',
      error: 'getDesiredAdminStateErrorCallback'
    });
  },

  /**
   * pass received value or null to <code>setDesiredAdminState</code>
   * @param {Object} response
   * @returns {String|null}
   */
  getDesiredAdminStateSuccessCallback: function (response) {
    var status = response.HostRoles.desired_admin_state;
    if (!Em.isNone(status)) {
      this.setDesiredAdminState(status);
      return status;
    }
    return null;
  },

  /**
   * error callback of <code>getDesiredAdminState</code>
   */
  getDesiredAdminStateErrorCallback: Em.K,

  /**
   * compute decommission state by desiredAdminState
   * @param {Object} status
   */
  setDesiredAdminState: Em.K,

  /**
   * Get component decommission status from server
   * @returns {$.ajax}
   */
  getDecommissionStatus: function () {
    return App.ajax.send({
      name: 'host.host_component.decommission_status',
      sender: this,
      data: {
        hostName: this.get('content.hostName'),
        componentName: this.get('componentForCheckDecommission'),
        serviceName: this.get('content.service.serviceName')
      },
      success: 'getDecommissionStatusSuccessCallback',
      error: 'getDecommissionStatusErrorCallback'
    });
  },

  /**
   * pass received value or null to <code>setDecommissionStatus</code>
   * @param {Object} response
   * @returns {Object|null}
   */
  getDecommissionStatusSuccessCallback: function (response) {
    var statusObject = response.ServiceComponentInfo;
    if (!Em.isNone(statusObject)) {
      statusObject.component_state = response.host_components[0].HostRoles.state;
      this.setDecommissionStatus(statusObject);
      return statusObject;
    }
    return null;
  },

  /**
   * Set null to <code>decommissionedStatusObject</code> if server returns error
   * @returns {null}
   */
  getDecommissionStatusErrorCallback: Em.K,

  /**
   * compute decommission state by component info
   * @param {Object} status
   */
  setDecommissionStatus: Em.K,

  /**
   * set decommission and recommission flags according to status
   * @param status
   */
  setStatusAs: function (status) {
    switch (status) {
      case "INSERVICE":
        this.set('isComponentRecommissionAvailable', false);
        this.set('isComponentDecommissioning', false);
        this.set('isComponentDecommissionAvailable', this.get('isStart'));
        break;
      case "DECOMMISSIONING":
        this.set('isComponentRecommissionAvailable', true);
        this.set('isComponentDecommissioning', true);
        this.set('isComponentDecommissionAvailable', false);
        break;
      case "DECOMMISSIONED":
        this.set('isComponentRecommissionAvailable', true);
        this.set('isComponentDecommissioning', false);
        this.set('isComponentDecommissionAvailable', false);
        break;
      case "RS_DECOMMISSIONED":
        this.set('isComponentRecommissionAvailable', true);
        this.set('isComponentDecommissioning', this.get('isStart'));
        this.set('isComponentDecommissionAvailable', false);
        break;
    }
  },

  /**
   * Do blinking for 1 minute
   */
  doBlinking: function () {
    var workStatus = this.get('workStatus');
    var pulsate = [App.HostComponentStatus.starting, App.HostComponentStatus.stopping, App.HostComponentStatus.installing].contains(workStatus);

    if (!pulsate) {
      var component = this.get('content');
      if (component && workStatus !== "INSTALLED") {
        pulsate = this.get('isDecommissioning');
      }
    }
    if (pulsate && !this.get('isBlinking')) {
      this.set('isBlinking', true);
      this.pulsate();
    }
  },

  /**
   * perform the pulsation
   */
  pulsate: function() {
    var self = this;

    uiEffects.pulsate(self.$('.components-health'), 1000, function () {
      self.set('isBlinking', false);
      self.doBlinking();
    });
  },

  /**
   * Start blinking when host component is starting/stopping/decommissioning
   */
  startBlinking: function () {
    this.$('.components-health').stop(true, true);
    this.$('.components-health').css({opacity: 1.0});
    this.doBlinking();
  }.observes('workStatus', 'isComponentRecommissionAvailable', 'isDecommissioning'),

  didInsertElement: function () {
    var self = this;
    this._super();
    App.router.get('mainController').isLoading.call(App.router.get('clusterController'), 'isServiceContentFullyLoaded').done(function () {
      self.loadComponentDecommissionStatus();
    });
  },

  willDestroyElement: function () {
    this._super();
    var timer = this.get('decommissionStatusPollingTimer');
    if (timer) {
      clearInterval(timer);
      this.set('decommissionStatusPollingTimer', null);
    }
  },

  /**
   * Update Decommission status only one time when component was changed
   */
  updateDecommissionStatus: function () {
    var self = this;
    App.router.get('mainController').isLoading.call(App.router.get('clusterController'), 'isServiceContentFullyLoaded').done(function () {
      Em.run.once(self, 'loadComponentDecommissionStatus');
    });
  }.observes('content.workStatus', 'content.passiveState'),

  /**
   * Update Decommission status periodically
   */
  decommissionStatusPolling: function () {
    var self = this;
    this.set('decommissionStatusPollingTimer', setTimeout(function () {
      self.loadComponentDecommissionStatus().done(function() {
        self.decommissionStatusPolling();
      });
    }, this.POLLING_INTERVAL));
  },

  /**
   * Start Decommission status polling if it is not started yet
   */
  startDecommissionStatusPolling: function () {
    if (!this.get('decommissionStatusPollingTimer')) {
      this.decommissionStatusPolling();
    }
  },

  decommissionView: Em.View.extend({
    classNameBindings: ['parentView.noActionAvailable'],
    tagName: 'li',
    templateName: require('templates/main/host/decommission'),

    text: Em.computed.ifThenElse('parentView.isComponentDecommissionAvailable', Em.I18n.t('common.decommission'), Em.I18n.t('common.recommission')),

    didInsertElement: function () {
      this._super();
      App.tooltip($("[rel='decommissionTooltip']"));
    },

    click: function () {
      if (!this.get('parentView.isComponentDecommissionDisable')) {
        if (this.get('parentView.isComponentDecommissionAvailable')) {
          this.get('controller').decommission(this.get('parentView.content'), this.get('parentView.startDecommissionStatusPolling').bind(this.get('parentView')));
        } else {
          this.get('controller').recommission(this.get('parentView.content'), this.get('parentView.startDecommissionStatusPolling').bind(this.get('parentView')));
        }
      }
    }
  })
});
