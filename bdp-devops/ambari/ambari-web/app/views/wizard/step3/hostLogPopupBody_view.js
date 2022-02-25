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

App.WizardStep3HostLogPopupBody = Em.View.extend({

  templateName: require('templates/wizard/step3/step3_host_log_popup'),

  /**
   * Host's boot log
   * @type {string}
   */
  bootLog: Em.computed.alias('parentView.host.bootLog'),

  /**
   * Is textarea view active
   * @type {bool}
   */
  isTextArea: false,

  /**
   * Textbox with host's boot log
   * @type {Ember.TextArea}
   */
  textArea: Em.TextArea.extend({

    didInsertElement: function () {
      /* istanbul ignore next: simple DOM manipulations */
      $(this.get('element'))
        .width($(this.get('parentView').get('element')).width() - 10)
        .height($(this.get('parentView').get('element')).height())
        .select()
        .css('resize', 'none');
    },

    /**
     * Edit disabled
     * @type {bool}
     */
    readOnly: true,

    /**
     * <code>parentView.bootLog</code>
     * @type {string}
     */
    value: Em.computed.alias('content')

  }),

  didInsertElement: function () {
    var self = this;
    var button = $(this.get('element')).find('.textTrigger');
    button.click(function () {
      $(this).text(self.get('isTextArea') ? Em.I18n.t('installer.step3.hostLogPopup.highlight') : Em.I18n.t('installer.step3.hostLogPopup.copy'));
      self.set('isTextArea', !self.get('isTextArea'));
    });
    /* istanbul ignore next: difficult to test */
    $(this.get('element')).find('.content-area')
      .mouseenter(
      function () {
        button.css('visibility', 'visible');
      })
      .mouseleave(
      function () {
        button.css('visibility', 'hidden');
      });
  }

});
