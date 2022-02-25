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

App.WidgetWizardStep1Controller = Em.Controller.extend({
  name: "widgetWizardStep1Controller",

  /**
   * Types:
   * - GAUGE
   * - NUMBER
   * - GRAPH
   * - TEMPLATE
   * @type {string}
   */
  widgetType: '',

  /**
   * @type {boolean}
   */
  isSubmitDisabled: Em.computed.not('widgetType'),

  /**
   * @type {App.WidgetType}
   */
  options: function () {
    var selectedType = this.get('widgetType');
    return App.WidgetType.find().map(function(option) {
      return {
        name: option.get('name'),
        displayName: option.get('displayName'),
        iconPath: option.get('iconPath'),
        description: option.get('description')
      }
    });
  }.property('widgetType'),

  /**
   * choose widget type and proceed to the next step
   * @param {object} event
   */
  chooseOption: function (event) {
    this.set('widgetType', event.context);
    $("[rel='selectable-tooltip']").trigger('mouseleave');
    this.next();
  },

  loadStep: function () {
    this.clearStep();
  },

  clearStep: function () {
    this.set('widgetType', '');
  },

  next: function () {
    App.router.send('next');
  }
});

