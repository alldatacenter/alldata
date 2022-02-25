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

App.ReloadPopupMixin = Em.Mixin.create({

  reloadPopup: null,

  reloadSuccessCallback: function () {
    this.closeReloadPopup();
  },

  reloadErrorCallback: function (jqXHR, ajaxOptions, error, opt, params) {
    if (jqXHR.status) {
      this.closeReloadPopup();
      if (params.shouldUseDefaultHandler) {
        App.ajax.defaultErrorHandler(jqXHR, opt.url, opt.type, jqXHR.status);
      }
    } else {
      var timeout = Em.isNone(params.timeout) ? App.get('timeout') : params.timeout;
      this.showReloadPopup(params.reloadPopupText);
      if (params.callback) {
        var self = this;
        window.setTimeout(function () {
          params.callback.apply(self, params.args || []);
        }, timeout);
      }
    }
  },

  popupText: function(text) {
    return text || Em.I18n.t('app.reloadPopup.text');
  },

  showReloadPopup: function (text) {
    var self = this,
      bodyText = this.popupText(text);
    if (!this.get('reloadPopup')) {
      this.set('reloadPopup', App.ModalPopup.show({
        primary: null,
        secondary: null,
        showFooter: false,
        header: this.t('app.reloadPopup.header'),
        bodyClass: Ember.View.extend({
          template: Ember.Handlebars.compile("<div id='reload_popup' class='alert alert-info'>" +
            "{{view App.SpinnerView}}" +
            "<div><span>" + bodyText + "</span><a href='javascript:void(null)' onclick='location.reload();'>"
            + this.t('app.reloadPopup.link') + "</a></div>")
        }),
        onClose: function () {
          self.set('reloadPopup', null);
          this._super();
        }
      }));
    }
  },

  closeReloadPopup: function () {
    var reloadPopup = this.get('reloadPopup');
    if (reloadPopup) {
      reloadPopup.onClose();
    }
  }

});
