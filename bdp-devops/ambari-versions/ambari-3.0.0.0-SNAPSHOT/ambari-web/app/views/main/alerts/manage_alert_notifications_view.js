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

App.ManageAlertNotificationsView = Em.View.extend({

  templateName: require('templates/main/alerts/manage_alert_notifications_popup'),

  /**
   * @type {App.AlertNotification}
   */
  selectedAlertNotification: null,

  selectedAlertNotificationGroups: function () {
    return this.get('controller.selectedAlertNotification.groups').toArray().mapProperty('displayName').join(', ');
  }.property('controller.selectedAlertNotification', 'controller.selectedAlertNotification.groups.@each.displayName', 'controller.isLoaded'),

  someAlertNotificationIsSelected: Em.computed.bool('controller.selectedAlertNotification'),

  /**
   * @type {boolean}
   */
  isEditButtonDisabled: Em.computed.or('!someAlertNotificationIsSelected', '!controller.selectedAlertNotification.enabled'),

  /**
   * @type {boolean}
   */
  isRemoveButtonDisabled: Em.computed.or('!someAlertNotificationIsSelected'),

  /**
   * @type {boolean}
   */
  isDuplicateButtonDisabled: Em.computed.or('!someAlertNotificationIsSelected', '!controller.selectedAlertNotification.enabled'),

  /**
   * @type {boolean}
   */
  isEnableOrDisableButtonDisabled: Em.computed.or('!someAlertNotificationIsSelected'),

  /**
   * Show EMAIL information if selected alert notification has type EMAIL
   * @type {boolean}
   */
  showEmailDetails: Em.computed.equal('controller.selectedAlertNotification.type', 'EMAIL'),

  /**
   * Show SNMP information if selected alert notification has type SNMP
   * @type {boolean}
   */
  showSNMPDetails: Em.computed.equal('controller.selectedAlertNotification.type', 'SNMP'),

  /**
   * Show Custom SNMP information if selected alert notification has type Custom SNMP
   * @type {boolean}
   */
  showCustomSNMPDetails: Em.computed.equal('controller.selectedAlertNotification.type', 'Custom SNMP'),
  
  email: function () {
    return this.get('controller.selectedAlertNotification.properties')['ambari.dispatch.recipients'];
  }.property('controller.selectedAlertNotification.properties'),

  /**
   * @type {string}
   */
  severities: function () {
    return this.get('controller.selectedAlertNotification.alertStates').join(', ');
  }.property('controller.selectedAlertNotification.alertStates'),

  selectedAlertNotificationTypeText: function() {
    return this.get('controller').getNotificationTypeText(this.get('controller.selectedAlertNotification.type'))
  }.property('controller.selectedAlertNotification', 'controller.isLoaded'),

  editAlertNotification: function () {
    if(!this.get('isEditButtonDisabled')) {
      this.get('controller').editAlertNotification();
    }
  },

  duplicateAlertNotification: function () {
    if(!this.get('isDuplicateButtonDisabled')) {
      this.get('controller').duplicateAlertNotification();
    }
  },

  enableOrDisableAlertNotification: function (e) {
    if(!this.get('isEnableOrDisableButtonDisabled')) {
      this.$("[rel='button-info-dropdown']").tooltip('destroy');
      this.get('controller').enableOrDisableAlertNotification(e);
    }
  },

  /**
   * Prevent user select more than 1 alert notification
   * @method onAlertNotificationSelect
   */
  onAlertNotificationSelect: function () {
    var selectedAlertNotification = this.get('selectedAlertNotification');
    if (selectedAlertNotification && selectedAlertNotification.length) {
      this.set('controller.selectedAlertNotification', selectedAlertNotification[selectedAlertNotification.length - 1]);
    }
    if (selectedAlertNotification && selectedAlertNotification.length > 1) {
      this.set('selectedAlertNotification', selectedAlertNotification[selectedAlertNotification.length - 1]);
    }
    if(this.$("[rel='button-info-dropdown']")) {
      this.$("[rel='button-info-dropdown']").tooltip('destroy');
    }
    Em.run.later(this, function () {
      App.tooltip(self.$("[rel='button-info-dropdown']").parent().not(".disabled").children(), {placement: 'left'});
    }, 50);
  }.observes('selectedAlertNotification'),

  /**
   * Set first alert notification as selected (if they are already loaded)
   * Add some tooltips on manage buttons
   * @method onLoad
   */
  onLoad: function () {
    var self = this;
    if (this.get('controller.isLoaded')) {
      var notifications = this.get('controller.alertNotifications');
      if (notifications && notifications.length) {
        this.set('selectedAlertNotification', this.get('controller.selectedAlertNotification') || notifications[0]);
      } else {
        this.set('selectedAlertNotification', null);
      }
      Em.run.later(this, function () {
        App.tooltip(self.$("[rel='button-info']"));
      }, 50);
    }
  }.observes('controller.isLoaded'),

  willDestroyElement: function () {
    this.$("[rel='button-info']").tooltip('destroy');
    this.$("[rel='button-info-dropdown']").tooltip('destroy');
  },

  willInsertElement: function () {
    this.get('controller').loadAlertNotifications();
  },

  didInsertElement: function () {
    this.onLoad();
  }

});
