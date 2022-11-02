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

App.WizardStep0Controller = Em.Controller.extend({

  name: 'wizardStep0Controller',

  /**
   * Is step submitted
   * @type {bool}
   */
  hasSubmitted: false,

  /**
   * validate cluster name
   * set <code>clusterNameError</code> if validation fails
   */
  invalidClusterName: function () {
    var MAX_CLUSTER_NAME_LENGTH = 100;
    var clusterName = this.get('content.cluster.name');
    if (clusterName == '' && this.get('hasSubmitted')) {
      this.set('clusterNameError', Em.I18n.t('installer.step0.clusterName.error.required'));
      return true;
    } else if (clusterName == '' ) {
      return true;
    } else if (clusterName.length > MAX_CLUSTER_NAME_LENGTH) {
      this.set('clusterNameError', Em.I18n.t('installer.step0.clusterName.error.tooLong'));
      return true;
    } else if (/\s/.test(clusterName)) {
      this.set('clusterNameError', Em.I18n.t('installer.step0.clusterName.error.whitespace'));
      return true;
    } else if (/[^\w\-]/gi.test(clusterName)) {
      this.set('clusterNameError', Em.I18n.t('installer.step0.clusterName.error.specialChar'));
      return true;
    } else {
      this.set('clusterNameError', '');
      return false;
    }
  }.property('hasSubmitted', 'content.cluster.name'),

  /**
   * calculates by <code>invalidClusterName</code> property
   * todo: mix this and previous variables in one
   */
  clusterNameError: '',

  loadStep: function () {
    this.set('hasSubmitted', false);
    this.set('clusterNameError', '');
  },

  /**
   * @type {boolean}
   */
  hasNotStacksAvailable: false,

  /**
   * @type {boolean}
   */
  isSubmitDisabled: Em.computed.or('invalidClusterName', 'App.router.btnClickInProgress', 'hasNotStacksAvailable'),

  /**
   * Onclick handler for <code>next</code> button
   * Disable 'Next' button while it is already under process. (using Router's property 'nextBtnClickInProgress')
   * @method submit
   */
  submit: function () {
    if(App.get('router.nextBtnClickInProgress')){
      return;
    }
    this.set('hasSubmitted', true);
    if (!this.get('invalidClusterName')) {
      App.clusterStatus.set('clusterName', this.get('content.cluster.name'));
      this.set('content.cluster.status', 'PENDING');
      this.set('content.cluster.isCompleted', false);
      App.set('router.nextBtnClickInProgress', true);
      App.router.send('next');
    }
  }

});
