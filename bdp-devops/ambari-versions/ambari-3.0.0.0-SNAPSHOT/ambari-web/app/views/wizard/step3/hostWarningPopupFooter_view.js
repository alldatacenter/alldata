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

App.WizardStep3HostWarningPopupFooter = Em.View.extend({

  templateName: require('templates/wizard/step3/step3_host_warning_popup_footer'),

  classNames: ['modal-footer', 'host-checks-update'],

  footerControllerBinding: 'App.router.wizardStep3Controller',

  progressWidth: Em.computed.format('width:{0}%', 'footerController.checksUpdateProgress'),

  isUpdateInProgress: function () {
    return !this.get('checkHostFinished') || (this.get('footerController.checksUpdateProgress') > 0) &&
      (this.get('footerController.checksUpdateProgress') < 100);
  }.property('checkHostFinished', 'footerController.checksUpdateProgress'),

  updateStatusClass: function () {
    var status = this.get('footerController.checksUpdateStatus');
    if (status === 'SUCCESS') {
      return 'text-success';
    }
    else {
      if (status === 'FAILED') {
        return 'text-danger';
      }
      else {
        return null;
      }
    }
  }.property('footerController.checksUpdateStatus'),

  updateStatus: function () {
    var status = this.get('footerController.checksUpdateStatus');
    if (status === 'SUCCESS') {
      return Em.I18n.t('installer.step3.warnings.updateChecks.success');
    } else if (status === 'FAILED') {
      return Em.I18n.t('installer.step3.warnings.updateChecks.failed');
    } else {
      return null;
    }
  }.property('footerController.checksUpdateStatus')

});
