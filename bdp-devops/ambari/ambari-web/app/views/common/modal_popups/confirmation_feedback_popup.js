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
 * Show confirmation popup
 * After sending command watch status of query,
 * and in case of failure provide ability to retry to launch an operation.
 *
 * @param {Function} primary - "OK" button click handler
 * @param {Object} bodyMessage - confirmMsg:{String},
 confirmButton:{String},
 additionalWarningMsg:{String},
 * @param {Function} secondary - "Cancel" button click handler
 * @return {*}
 */
App.showConfirmationFeedBackPopup = function (primary, bodyMessage, secondary) {
  if (!primary) {
    return false;
  }
  return App.ModalPopup.show({
    header: Em.I18n.t('popup.confirmation.commonHeader'),
    bodyClass: Em.View.extend({
      templateName: require('templates/common/modal_popups/confirmation_feedback')
    }),
    query: Em.Object.create({status: "INIT"}),
    primary: bodyMessage? bodyMessage.confirmButton : Em.I18n.t('ok'),
    onPrimary: function () {
      this.set('query.status', "INIT");
      this.set('disablePrimary', true);
      this.set('disableSecondary', true);
      this.set('statusMessage', Em.I18n.t('popup.confirmationFeedBack.sending'));
      this.hide();
      primary(this.get('query'), this.get('runMmOperation'));
    },
    statusMessage: bodyMessage? bodyMessage.confirmMsg : Em.I18n.t('question.sure'),
    additionalWarningMsg: bodyMessage? bodyMessage.additionalWarningMsg : null,
    putInMaintenance: bodyMessage ? bodyMessage.putInMaintenance : null,
    runMmOperation: false,
    turnOnMmMsg: bodyMessage ? bodyMessage.turnOnMmMsg : null,
    watchStatus: function() {
      if (this.get('query.status') === "SUCCESS") {
        this.hide();
      } else if(this.get('query.status') === "FAIL") {
        this.set('primaryClass', 'btn-primary');
        this.set('primary', Em.I18n.t('common.retry'));
        this.set('disablePrimary', false);
        this.set('disableSecondary', false);
        this.set('statusMessage', Em.I18n.t('popup.confirmationFeedBack.query.fail'));
      }
    }.observes('query.status'),
    onSecondary: function () {
      this.hide();
      if (secondary) {
        secondary();
      }
    }
  });
};