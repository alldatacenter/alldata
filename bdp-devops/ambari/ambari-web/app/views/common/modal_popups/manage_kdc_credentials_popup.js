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
 * @return {*}
 */
App.showManageCredentialsPopup = function () {
  return App.ModalPopup.show({
    header: Em.I18n.t('admin.kerberos.credentials.store.menu.label'),
    bodyClass: App.ManageCredentialsFormView,
    primary: Em.I18n.t('common.save'),

    thirdClass: 'pull-left btn-danger',
    third: Em.computed.ifThenElse('formView.isRemovable', Em.I18n.t('common.remove'), null),

    isCredentialsRemoved: false,

    disablePrimary: Em.computed.alias('formView.isSubmitDisabled'),

    formView: Em.computed.findBy('childViews', 'viewName', 'manageCredentialsForm'),

    onPrimary: function() {
      var self = this;
      var formView = this.get('formView');
      if (formView) {
        formView.saveKDCCredentials().always(function() {
          self.hide();
        });
      } else {
        this.hide();
      }
    },

    onThird: function() {
      var self = this;
      if (this.get('formView')) {
        this.get('formView').removeKDCCredentials().deferred.always(function() {
          self.hide();
        });
      }
    }
  });
};
