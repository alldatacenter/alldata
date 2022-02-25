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
 * This Mixin is used for views that should "wait" while some external action is executed (e.g., ajax-request).
 * You should set <code>fieldToObserve</code> to fieldName which value determines begin and end of such action
 * When <code>fieldToObserve</code>-value becomes truly, overlay is shown
 * Overlay size is taken from current view size
 *
 * Usage:
 *
 * <pre>
 *   Em.View.extend(App.LoadingOverlaySupport, {
 *    fieldToObserve: 'someFieldName'
 *   });
 * </pre>
 *
 * Don't forget to add <code><div class="loading-overlay"></div></code> to your view's template
 *
 * @type {Em.Mixin}
 */
App.LoadingOverlaySupport = Em.Mixin.create({

  /**
   * @type {string}
   */
  fieldToObserve: '',

  /**
   * Determines if <code>handleFieldChanges</code> should be call on <code>didInsertElement</code>
   *
   * @type {boolean}
   * @default {true}
   */
  handleFieldChangesOnDidInsert: true,

  init() {
    const fieldToObserve = this.get('fieldToObserve');
    Em.assert('`fieldToObserve` should be defined', fieldToObserve);
    this.addObserver(fieldToObserve, this, 'handleFieldChanges');
    return this._super(...arguments);
  },

  /**
   * Don't allow change `fieldToObserve` after view is initialized. This may cause a lot of errors
   */
  doNotChangeFieldToObserve: function () {
    Em.assert('Do not change `fieldToObserve` after view is initialized', false);
  }.observes('fieldToObserve'),

  /**
   * Value in the <code>fieldToObserve</code> may be <code>truly</code> on view init, so
   * <code>handleFieldChanges</code> should be called to check this
   *
   * Example use case:
   *  Switching tabs on the configs page for some service. Every tab should be disabled when switching on it
   *  if needed property is set to <code>true</code>
   *
   * @returns {*}
   */
  didInsertElement() {
    this._super(...arguments);
    if (this.get('handleFieldChangesOnDidInsert')) {
      this.handleFieldChanges();
    }
  },

  /**
   * Remove observer `handleFieldChanges`
   */
  willDestroyElement() {
    const fieldToObserve = this.get('fieldToObserve');
    this.removeObserver(fieldToObserve, this, 'handleFieldChanges');
    this._cleanUp();
  },

  /**
   * Show overlay when `fieldToObserve`-value is set to some truly
   * Remove overlay otherwise
   */
  handleFieldChanges() {
    const fieldToObserve = this.get('fieldToObserve');
    const fieldValue = this.get(fieldToObserve);
    const overlay = this.$('.loading-overlay');
    let polling = this.get('_polling');
    if (fieldValue) {
      if (!polling) {
        polling = setInterval(() => {
          if (fieldValue && overlay) {
            overlay.addClass('overlay-visible').css({
              width: this.$().width(),
              height: this.$().height()
            });
          }
          else {
            this._cleanUp();
          }
        }, 50);
        this.set('_polling', polling);
      }
    }
    else {
      this._cleanUp();
    }
  },

  /**
   * @private
   */
  _cleanUp() {
    clearInterval(this.get('_polling'));
    this.set('_polling', null);
    const overlay = this.$('.loading-overlay');
    if (overlay) {
      overlay.removeClass('overlay-visible');
    }
  }

});