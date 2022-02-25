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

App.ServiceRestartView = Em.View.extend({

  templateName: require('templates/common/service_restart'),

  initDefaultConfigs: function () {
    this.set('useRolling', true);
    this.set('showAdvancedOptions', false);
    this.set('showBatchRackOptions', !!this.get('slaveComponents'));
    this.set('batchesOfHosts', true);
    this.set('noOfHostsInBatch', 10);
    this.set('batchIntervalHosts', 120);
    this.set('percentRackStarted', 100);
    this.set('batchIntervalRacks', 120);
    this.set('isRetryChecked', true);
    this.set('noOfRetriesPerHost', 2);
    this.set('maxFailuresTolerated', 10);
    this.set('maxFailuresBatch', 2);
    this.set('maxFailuresRack', 2);
    this.set('suppressAlerts', true);
    this.set('pauseAfterFirst', false);
  },


  getRestartConfigs: function() {
    return Em.Object.create({
      batchInterval: this.get('batchesOfHosts') ? this.get('batchIntervalHosts') : this.get('batchIntervalRacks'),
      maxFailures: this.get('maxFailuresTolerated'),
      maxFailuresPerBatch: this.get('maxFailuresTolerated'),
    });
  },

  getNoOfHosts: function (component) {
    if (this.get('batchesOfHosts')) return this.get('noOfHostsInBatch');
    return Math.ceil(this.get('percentRackStarted')*100/(this.get('componentHostRackInfoMap')[component].size()));
  },

  rollingRestartRadioButton: App.RadioButtonView.extend({
    labelTranslate: 'common.rolling',
    checked: Em.computed.alias('parentView.useRolling'),
    click: function () {
      this.set('parentView.useRolling', true);
    }
  }),

  expressRestartRadioButton: App.RadioButtonView.extend({
    labelTranslate: 'common.express',
    checked: Em.computed.not('parentView.useRolling'),
    click: function () {
      this.set('parentView.useRolling', false);
    }
  }),

  batchesOfHostsRadioButton: App.RadioButtonView.extend({
    labelTranslate: 'service.restart.rolling.batchesOfHosts',
    checked: Em.computed.alias('parentView.batchesOfHosts'),
    click: function () {
      this.set('parentView.batchesOfHosts', true);
    }
  }),

  rackByRackRadioButton: App.RadioButtonView.extend({
    labelTranslate: 'service.restart.rolling.rackByRack',
    checked: Em.computed.not('parentView.batchesOfHosts'),
    click: function () {
      this.set('parentView.batchesOfHosts', false);
    }
  }),

  toggleAdvancedOptions: function () {
    this.toggleProperty('showAdvancedOptions');
  }

});