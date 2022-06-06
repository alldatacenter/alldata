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

App.AddAlertDefinitionStep1Controller = Em.Controller.extend({

  name: 'addAlertDefinitionStep1',

  /**
   * List of available alert definition types
   * @type {{name: string, isActive: boolean}[]}
   */
  alertDefinitionsTypes: function () {
    return App.AlertType.find().map(function(option) {
      return Em.Object.create({
        name: option.get('name'),
        displayName: option.get('displayName'),
        icon: option.get('iconPath'),
        description: option.get('description')
      });
    });
  }.property(),

  /**
   * Set selectedType if it exists in the wizard controller
   * @method loadStep
   */
  loadStep: function() {
    this.set('content.selectedType', '');
  },

  /**
   * Handler for select alert definition type selection
   * @param {object} e
   * @method selectType
   */
  selectType: function(e) {
    var type = e.context,
        types = this.get('alertDefinitionsTypes');
    this.set('content.selectedType', type.name);
    $("[rel='selectable-tooltip']").trigger('mouseleave');
    App.router.send('next');
  }

});
