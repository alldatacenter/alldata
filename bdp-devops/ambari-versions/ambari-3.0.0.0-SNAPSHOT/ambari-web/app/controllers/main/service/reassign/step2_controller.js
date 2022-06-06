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

App.ReassignMasterWizardStep2Controller = Em.Controller.extend(App.AssignMasterComponents, {

  name: "reassignMasterWizardStep2Controller",

  useServerValidation: false,

  mastersToShow: function () {
    return [this.get('content.reassign.component_name')];
  }.property('content.reassign.component_name'),

  mastersToMove: function () {
    return [this.get('content.reassign.component_name')];
  }.property('content.reassign.component_name'),

  /**
   * Show 'Current: <host>' for masters with single instance
   */
  additionalHostsList: function () {
    if (this.get('servicesMastersToShow.length') === 1) {
      return [
        {
          label: Em.I18n.t('services.reassign.step2.currentHost'),
          host: App.HostComponent.find().findProperty('componentName', this.get('content.reassign.component_name')).get('hostName')
        }
      ];
    }
    return [];
  }.property('content.reassign.component_name', 'servicesMastersToShow.length'),

  /**
   * Assignment is valid only if for one master component host was changed
   * @returns {boolean}
   */
  customClientSideValidation: function () {
    var reassigned = 0;
    var existedComponents = App.HostComponent.find().filterProperty('componentName', this.get('content.reassign.component_name')).mapProperty('hostName');
    var newComponents = this.get('servicesMasters').filterProperty('component_name', this.get('content.reassign.component_name')).mapProperty('selectedHost');
    existedComponents.forEach(function (host) {
      if (!newComponents.contains(host)) {
        reassigned++;
      }
    }, this);
    return reassigned === 1;
  }

});

