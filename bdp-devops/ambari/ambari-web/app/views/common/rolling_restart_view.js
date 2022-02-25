/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');
var numberUtils = require('utils/number_utils');

/**
 * View content of the rolling restart dialog.
 *
 * Callers provide the context in which this dialog is invoked.
 */
App.RollingRestartView = Em.View.extend({
  templateName : require('templates/common/rolling_restart_view'),

  /**
   * Component name for components that should be restarted
   * @type {String}
   */
  hostComponentName : null,

  /**
   * Service name for components that should be restarted
   * @type {String}
   */
  serviceName : null,

  /**
   * If service is in Maintenance Mode
   * @type {bool}
   */
  isServiceInMM: false,

  /**
   * If true service will be put in Maintenance mode before rolling restart
   * @type {bool}
   */
  turnOnMm: false,


  /**
   * Restart only components with <code>staleConfigs</code>
   * @type {bool}
   */
  staleConfigsOnly : false,

  /**
   * We should do rolling restart for components if we run
   * restart for service and service is in Maintenance mode
   * @type {bool}
   */
  skipMaintenance: false,

  /**
   * Count of host components in one batch
   * @type {Number}
   */
  batchSize : -1,

  /**
   * Delay between batches
   * @type {Number}
   */
  interBatchWaitTimeSeconds : -1,

  /**
   * @type {Number}
   */
  tolerateSize : -1,

  /**
   * Shows if all needed info is loaded and initialized
   * @type {Bool}
   */
  isLoaded: false,
  /**
   * List of error in batch-request properties
   * @type {Array}
   */
  errors : [],
  /**
   * List of warnings in batch-request properties, do not disable submit button
   * @type {Array}
   */
  warnings : [],
  /**
   * Set initial values for batch-request properties
   */
  initialize : function() {
    if (this.get('batchSize') == -1 && this.get('interBatchWaitTimeSeconds') == -1 && this.get('tolerateSize') == -1) {
      var restartCount = this.get('restartHostComponents.length');
      var batchSize = 1;
      if (restartCount > 10 && this.get('hostComponentName') !== 'DATANODE') {
        batchSize = Math.ceil(restartCount / 10);
      }
      var tolerateCount = batchSize;
      this.set('batchSize', batchSize);
      this.set('tolerateSize', tolerateCount);
      this.set('interBatchWaitTimeSeconds', 120);
      this.set('isLoaded', true);
    }
  },

  /**
   * Validate batch-request properties
   * List of errors is saved to <code>errors</code>
   */
  validate : function() {
    var displayName = pluralize(this.get('hostComponentDisplayName'));
    var componentName = this.get('hostComponentName');
    var totalCount = this.get('restartHostComponents.length');
    var bs = this.get('batchSize');
    var ts = this.get('tolerateSize');
    var wait = this.get('interBatchWaitTimeSeconds');
    var errors = [];
    var warnings = [];
    var bsError, tsError, waitError;
    if (totalCount < 1) {
      errors.push(Em.I18n.t('rollingrestart.dialog.msg.noRestartHosts').format(displayName));
    } else {
      if (componentName === 'DATANODE') {
        // specific case for DataNodes batch size is more than 1
        if (bs > 1) {
          warnings.push(Em.I18n.t('rollingrestart.dialog.warn.datanode.batch.size'));
        }
        bsError = numberUtils.validateInteger(bs, 1, NaN);
      } else {
        bsError = numberUtils.validateInteger(bs, 1, totalCount);
      }
      tsError = numberUtils.validateInteger(ts, 0, totalCount);
      if (bsError != null) {
        errors.push(Em.I18n.t('rollingrestart.dialog.err.invalid.batchsize').format(bsError));
      }
      if (tsError != null) {
        errors.push(Em.I18n.t('rollingrestart.dialog.err.invalid.toleratesize').format(tsError));
      }
    }
    waitError = numberUtils.validateInteger(wait, 0, NaN);
    if (waitError != null) {
      errors.push(Em.I18n.t('rollingrestart.dialog.err.invalid.waitTime').format(waitError));
    }
    this.set('errors', errors);
    this.set('warnings', warnings);
  }.observes('batchSize', 'interBatchWaitTimeSeconds', 'tolerateSize', 'restartHostComponents', 'hostComponentDisplayName'),

  /**
   * Formatted <code>hostComponentName</code>
   * @type {String}
   */
  hostComponentDisplayName: Em.computed.formatRole('hostComponentName', false),

  /**
   * List of all host components
   * @type {Array}
   */
  allHostComponents : [],

  /**
   * List of host components without components in out-of-service state
   * @type {Array}
   */
  nonMaintainanceHostComponents : Em.computed.filterBy('allHostComponents', 'passiveState', 'OFF'),

  /**
   * List of host components with host in Maintenance mode
   * @type {Array}
   */
  componentsWithMaintenanceHost: Em.computed.filterBy('allHostComponents', 'hostPassiveState', 'ON'),

  /**
   * List of host components without components in out-of-service state
   * If <code>staleConfigsOnly</code> is true, components with <code>staleConfigs</code> = false are also filtered
   * @type {Array}
   */
  restartHostComponents: function () {
    var hostComponents = (this.get('skipMaintenance')) ? this.get('allHostComponents') : this.get('nonMaintainanceHostComponents');

    if (this.get('staleConfigsOnly')) {
      hostComponents = hostComponents.filterProperty('staleConfigs');
    }
    return hostComponents;
  }.property('nonMaintainanceHostComponents', 'staleConfigsOnly'),
  /**
   * @type {String}
   */
  suggestTurnOnMaintenanceMsg : function() {
    if (!this.get('isServiceInMM')) {
      return Em.I18n.t('rollingrestart.dialog.msg.serviceNotInMM').format(this.get('serviceName'));
    } else {
      return null;
    }
  }.property('isServiceInMM'),

  /**
   * @type {String}
   */
  restartMessage : function() {
    return Em.I18n.t('rollingrestart.dialog.msg.restart').format(pluralize(this.get('hostComponentDisplayName')));
  }.property('hostComponentDisplayName'),

  /**
   * @type {String}
   */
  maintainanceMessage : function() {
    var count = this.get('componentsWithMaintenanceHost.length');
    if (count > 0) {
      var name = this.get('hostComponentDisplayName');
      return Em.I18n.t('rollingrestart.dialog.msg.maintainance').format(count, pluralize(name));
    }
    return null;
  }.property('componentsWithMaintenanceHost', 'hostComponentDisplayName'),

  /**
   * @type {String}
   */
  batchSizeMessage : function() {
    return Em.I18n.t('rollingrestart.dialog.msg.componentsAtATime').format(pluralize(this.get('hostComponentDisplayName')));
  }.property('hostComponentDisplayName'),

  /**
   * @type {String}
   */
  staleConfigsOnlyMessage : function() {
    return Em.I18n.t('rollingrestart.dialog.msg.staleConfigsOnly').format(pluralize(this.get('hostComponentDisplayName')));
  }.property('hostComponentDisplayName')

});
