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
 *
 * @param {Function} primary - "OK" button click handler
 * @param {String} body - additional text constant. Will be placed in the popup-body
 * @param {Function} secondary
 * @param {String} header
 * @param {String} primaryText
 * @param {String} primaryStyle
 * @return {*}
 */
App.showConfirmationPopup = function (primary, body, secondary, header, primaryText, primaryStyle = 'success', staticId) {
  var primaryClass = {
    'success': 'btn-success',
    'warning': 'btn-warning',
    'danger': 'btn-danger'
  }[primaryStyle];
  if (!primary) {
    return false;
  }
  return App.ModalPopup.show({
    'data-qa': 'confirmation-modal',
    encodeBody: false,
    primary: primaryText || Em.I18n.t('ok'),
    header: header || Em.I18n.t('popup.confirmation.commonHeader'),
    body: body || Em.I18n.t('question.sure'),
    primaryClass: primaryClass,
    primaryId: staticId ? staticId + '_primary' : '',
    secondaryId: staticId ? staticId + '_secondary' : '',
    thirdId: staticId ? staticId + '_third' : '',
    onPrimary: function () {
      this.hide();
      primary();
    },
    onSecondary: function () {
      this.hide();
      if (secondary) {
        secondary();
      }
    },
    onClose:  function () {
      this.hide();
      if (secondary) {
        secondary();
      }
    }
  });
};