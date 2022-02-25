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

App.WizardWatcherController = Em.Controller.extend(App.Persist, {
  name: 'wizardWatcherController',

  /**
   * @const
   */
  PREF_KEY: 'wizard-data',

  /**
   * name of user who working with wizard
   * @type {string|null}
   */
  wizardUser: null,

  /**
   * @type {string|null}
   */
  controllerName: null,

  /**
   * define whether Wizard is running
   * @type {boolean}
   */
  isWizardRunning: Em.computed.bool('wizardUser'),

  /**
   * @type {string}
   */
  wizardDisplayName: function() {
    const controllerName = this.get('controllerName');
    return controllerName ? Em.I18n.t('wizard.inProgress').format(App.router.get(controllerName).get('displayName'), this.get('wizardUser')) : '';
  }.property('controllerName'),

  /**
   * define whether logged in user is the one who started wizard
   * @type {boolean}
   */
  isNonWizardUser: function() {
    return this.get('isWizardRunning') && this.get('wizardUser') !== App.router.get('loginName');
  }.property('App.router.loginName', 'wizardUser').volatile(),

  /**
   * set user who launched wizard
   * @returns {$.ajax}
   */
  setUser: function(controllerName) {
    return this.postUserPref(this.get('PREF_KEY'), {
      userName: App.router.get('loginName'),
      controllerName: controllerName
    });
  },

  /**
   * reset user who launched wizard
   * @returns {$.ajax}
   */
  resetUser: function() {
    return this.postUserPref(this.get('PREF_KEY'), null);
  },

  /**
   * get user who launched wizard
   * @returns {$.ajax}
   */
  getUser: function() {
    return this.getUserPref(this.get('PREF_KEY'));
  },

  getUserPrefSuccessCallback: function(data) {
    if (Em.isNone(data)) {
      this.set('wizardUser', null);
      this.set('controllerName', null);
    } else {
      this.set('wizardUser', data.userName);
      this.set('controllerName', data.controllerName);
    }
  },

  getUserPrefErrorCallback: function () {
    this.resetUser();
  }
});
