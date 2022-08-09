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
 * Mixin for wizard controller for showing command progress on wizard pages
 * This should
 * @type {Ember.Mixin}
 */
App.wizardDeployProgressControllerMixin = Em.Mixin.create({

  /**
   * Ajax-requests count
   * @type {number}
   */
  ajaxQueueLength: 0,

  /**
   * Ajax-requests queue
   * @type {App.ajaxQueue}
   */
  ajaxRequestsQueue: null,

  /**
   * This flag when turned to true launches deploy progress bar
   */
  isDeployStarted: '',


  /**
   * We need to do a lot of ajax calls async in special order. To do this,
   * generate array of ajax objects and then send requests step by step. All
   * ajax objects are stored in <code>ajaxRequestsQueue</code>
   *
   * @param {Object} params object with ajax-request parameters like url, type, data etc
   * @method addRequestToAjaxQueue
   */
  addRequestToAjaxQueue: function (params) {
    if (App.get('testMode')) return;

    params = jQuery.extend({
      sender: this,
      error: 'ajaxQueueRequestErrorCallback'
    }, params);
    params.data.cluster = this.get('clusterName');

    this.get('ajaxRequestsQueue').addRequest(params);
  },

  /**
   * Navigate to next step after all requests are sent
   * @method ajaxQueueFinished
   */
  ajaxQueueFinished: function () {
    App.router.send('next');
  },

  /**
   * Error callback for each queued ajax-request
   * @param {object} xhr
   * @param {string} status
   * @param {string} error
   * @method ajaxQueueRequestErrorCallback
   */
  ajaxQueueRequestErrorCallback: function (xhr, status, error) {
    var responseText;
    try{
      responseText = JSON.parse(xhr.responseText);
    } catch(e) {
     responseText = {message: xhr.responseText}
    }
    var controller = App.router.get(App.clusterStatus.wizardControllerName);
    controller.registerErrPopup(Em.I18n.t('common.error'), responseText.message);
    this.set('hasErrorOccurred', true);
    // an error will break the ajax call chain and allow submission again
    this.set('isSubmitDisabled', false);
    this.set('isBackBtnDisabled', false);
    App.router.get(this.get('content.controllerName')).setStepsEnable();
  }
});


