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
 * @class PreInstallChecksController
 * @type {Em.Controller}
 */
App.PreInstallChecksController = Em.Controller.extend({

  name: 'preInstallChecksController',

  /**
   * Detects when pre install checks where run
   *
   * @type {boolean}
   */
  preInstallChecksWhereRun: false,

  /**
   * Set initial settings
   *
   * @method loadStep
   */
  loadStep: function () {
    this.set('preInstallChecksWhereRun', false);
  },

  /**
   * Show warning-popup for user that pre install checks where not run
   * User may skip run (primary) or execute them (secondary)
   *
   * @param {function} afterChecksCallback function called on primary-click
   * @returns {App.ModalPopup}
   * @method notRunChecksWarnPopup
   */
  notRunChecksWarnPopup: function (afterChecksCallback) {
    Em.assert('`afterChecksCallback` should be a function, you have provided ' + Em.typeOf(afterChecksCallback), 'function' === Em.typeOf(afterChecksCallback));
    var self = this;
    return App.ModalPopup.show({

      header: Em.I18n.t('installer.step7.preInstallChecks.notRunChecksWarnPopup.header'),
      body: Em.I18n.t('installer.step7.preInstallChecks.notRunChecksWarnPopup.body'),
      primary: Em.I18n.t('installer.step7.preInstallChecks.notRunChecksWarnPopup.primary'),
      secondary: Em.I18n.t('installer.step7.preInstallChecks.notRunChecksWarnPopup.secondary'),

      onPrimary: function () {
        this._super();
        afterChecksCallback();
      },

      onSecondary: function () {
        this._super();
        self.runPreInstallChecks();
      }

    });
  },

  /**
   * Run pre install checks
   *
   * @method runPreInstallChecks
   */
  runPreInstallChecks: function () {
    this.set('preInstallChecksWhereRun', true);
    return App.ModalPopup.show({

      header: Em.I18n.t('installer.step7.preInstallChecks.checksPopup.header'),
      secondary: '',

      body: Em.View.extend({

      })

    });
  }

});