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

App.WizardStep0View = Em.View.extend({

  tagName: "form",

  attributeBindings: ['autocomplete'],

  /**
   * Disable autocomplete for form's fields
   */
  autocomplete: 'off',

  templateName: require('templates/wizard/step0'),

  didInsertElement: function () {
    App.popover($("[rel=popover]"), {'placement': 'right', 'trigger': 'hover'});
    this.get('controller').loadStep();
  },

  /**
   * Is some error with provided cluster name
   * @type {bool}
   */
  onError: function () {
    return !Em.isEmpty(this.get('controller.clusterNameError'));
  }.property('controller.clusterNameError')

});

/**
 * Field for cluster name
 * @type {Ember.TextField}
 */
App.WizardStep0ViewClusterNameInput = Em.TextField.extend({

  /**
   * Submit form if "Enter" pressed
   * @method keyPress
   * @param {object} event
   * @returns {bool}
   */
  keyPress: function(event) {
    if (event.keyCode == 13) {
      this.get('parentView.controller').submit();
      return false;
    }
    return true;
  }
});
