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
 * Show alert popup
 *
 * @return {*}
 */
App.showConfigValidationFailedPopup = function (primary, secondary) {
  return App.ModalPopup.show({
    header: Em.I18n.t('installer.step7.popup.validation.failed.header'),
    primary: Em.I18n.t('common.proceedAnyway'),
    primaryClass: 'btn-danger',
    marginBottom: 200,
    onPrimary: function () {
      this._super();
      primary();
    },
    onSecondary: function () {
      this._super();
      secondary();
    },
    onClose: function () {
      this._super();
      secondary();
    },
    body: Em.I18n.t('installer.step7.popup.validation.request.failed.body')
  });
};
