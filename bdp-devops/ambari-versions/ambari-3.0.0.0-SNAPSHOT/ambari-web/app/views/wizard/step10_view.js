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

App.WizardStep10View = Em.View.extend({

  templateName: require('templates/wizard/step10'),

  didInsertElement: function () {
    this.get('controller').loadStep();
  },

  serviceRestartText: function () {
    var controller = this.get('controller');
    if (controller.get('isAddServiceWizard'))
      return Em.I18n.t('common.important.strong') + Em.I18n.t('installer.step10.staleServicesRestartRequired');
    else
      return '';
  }.property('controller.isAddServiceWizard')
});
