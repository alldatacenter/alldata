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

App.ReassignMasterWizardStep2View = App.AssignMasterComponentsView.extend({

  title: Em.I18n.t('installer.step5.reassign.header'),

  alertMessage: function () {
    if (this.get('controller.content.reassign.component_name') === 'NAMENODE' && App.get('isHaEnabled')) {
      return Em.I18n.t('services.reassign.step2.body.namenodeHA').format(this.get('controller.content.reassign.display_name'));
    }
    return Em.I18n.t('services.reassign.step2.body').format(this.get('controller.content.reassign.display_name'));
  }.property('controller.content.reassign.component_name', 'controller.content.reassign.display_name')
});
