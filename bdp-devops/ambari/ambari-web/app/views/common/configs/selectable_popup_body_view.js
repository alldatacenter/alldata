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
 * Body-view for popups where shown content may be switched to textarea-view
 * @type {Ember.View}
 */
App.SelectablePopupBodyView = Em.View.extend({

  templateName: require('templates/common/selectable_popup'),

  /**
   * True - if editable textarea is visible, false - otherwise
   * @type {boolean}
   */
  textareaVisible: false,

  /**
   * Switch <code>textareaVisible</code> value
   * @method textTrigger
   */
  textTrigger: function () {
    this.toggleProperty('textareaVisible');
  },

  /**
   * Set data to the visible textarea
   * @method putContentToTextarea
   */
  putContentToTextarea: function () {
    var content = this.get('parentView.content');
    if (this.get('textareaVisible')) {
      var wrapper = $(".task-detail-log-maintext");
      $('.task-detail-log-clipboard').html(content).css('width', wrapper.css('width')).height(wrapper.height());
      Em.run.next(function () {
        $('.task-detail-log-clipboard').select();
      });
    }
  }.observes('textareaVisible')

});
