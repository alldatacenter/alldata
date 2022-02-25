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

App.WizardStep7View = Em.View.extend({

  templateName: function () {
    return require(this.get('controller.isInstallWizard') ? 'templates/wizard/step7_with_category_tabs' : 'templates/wizard/step7');
  }.property('controller.content.controllerName'),

  willInsertElement: function () {
    if (this.get('controller.isInstallWizard')) {
      this.get('controller').initTabs();
      this.get('controller').loadStep();
    }
  },

  willDestroyElement: function () {
    this.get('controller').clearStep();
    this.get('controller').clearLastSelectedService();
  },

  /**
   * Link to model with credentials tab
   */
  credentialsTab: function () {
    return App.Tab.find().findProperty('name', 'credentials_tab');
  }.property()

});
