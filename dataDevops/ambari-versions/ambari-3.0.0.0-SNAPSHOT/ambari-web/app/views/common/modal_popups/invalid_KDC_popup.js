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
var credentialsUtils = require('utils/credentials');

/**
 * @param {Object} ajaxOpt - callbek funciton when clicking save
 * @param {Object} message - warning message
 * @return {*}
 */
App.showInvalidKDCPopup = function (ajaxOpt, message) {
  return App.ModalPopup.show({
    primary: Em.I18n.t('common.save'),
    header: Em.I18n.t('popup.invalid.KDC.header'),
    principal: "",
    password: "",

    /**
     * Store Admin credentials checkbox value
     *
     * @type {boolean}
     */
    storeCredentials: false,

    /**
     * Status of persistent storage. Returns <code>true</code> if persistent storage is available.
     * @type {boolean}
     */
    storePersisted: Em.computed.alias('App.isCredentialStorePersistent'),

    /**
     * Disable checkbox if persistent storage not available
     *
     * @type {boolean}
     */
    checkboxDisabled: Em.computed.not('storePersisted'),

    /**
     * Returns storage type used to save credentials e.g. <b>persistent</b>, <b>temporary</b> (default)
     *
     * @type {string}
     */
    storageType: Em.computed.ifThenElse('storeCredentials', credentialsUtils.STORE_TYPES.PERSISTENT, credentialsUtils.STORE_TYPES.TEMPORARY),

    /**
     * Message to display in tooltip regarding persistent storage state.
     *
     * @type {string}
     */
    hintMessage: Em.computed.ifThenElse('storePersisted', Em.I18n.t('admin.kerberos.credentials.store.hint.supported'), Em.I18n.t('admin.kerberos.credentials.store.hint.not.supported')),

    bodyClass: Em.View.extend({
      warningMsg: message + Em.I18n.t('popup.invalid.KDC.msg'),
      templateName: require('templates/common/modal_popups/invalid_KDC_popup')
    }),

    didInsertElement: function() {
      this._super();
      App.tooltip(this.$('[rel="tooltip"]'));
    },

    onClose: function() {
      this.hide();
      if (ajaxOpt.kdcCancelHandler) {
        ajaxOpt.kdcCancelHandler();
      }
    },

    onSecondary: function() {
      this.onClose();
    },
  
    onPrimary: function () {
      this.hide();
      var resource = credentialsUtils.createCredentialResource(this.get('principal'), this.get('password'), this.get('storageType'));
      App.get('router.clusterController').createKerberosAdminSession(resource, ajaxOpt);
    }
  });
};
