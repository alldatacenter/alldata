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

require('controllers/wizard/step5_controller');

App.HighAvailabilityWizardStep2Controller = Em.Controller.extend(App.BlueprintMixin, App.AssignMasterComponents, {

  name:"highAvailabilityWizardStep2Controller",

  useServerValidation: false,

  mastersToShow: ['NAMENODE', 'JOURNALNODE'],

  mastersToAdd: ['NAMENODE', 'JOURNALNODE', 'JOURNALNODE', 'JOURNALNODE'],

  showCurrentPrefix: ['NAMENODE'],

  showAdditionalPrefix: ['NAMENODE'],

  showInstalledMastersFirst: true,

  JOURNALNODES_COUNT_MINIMUM: 3, // TODO get this from stack
  
  renderComponents: function(masterComponents) {
    this._super(masterComponents);
    this.showHideJournalNodesAddRemoveControl();
  },

  addComponent: function(componentName) {
    this._super(componentName);
    this.showHideJournalNodesAddRemoveControl();
  },

  removeComponent: function(componentName, serviceComponentId) {
    this._super(componentName, serviceComponentId);
    this.showHideJournalNodesAddRemoveControl()
  },

  showHideJournalNodesAddRemoveControl: function() {
    var jns = this.get('selectedServicesMasters').filterProperty('component_name', 'JOURNALNODE');
    var maxNumMasters = this.getMaxNumberOfMasters('JOURNALNODE');
    var showRemoveControl = jns.get('length') > this.get('JOURNALNODES_COUNT_MINIMUM');
    var showAddControl = jns.get('length') < maxNumMasters;
    jns.forEach(function(item) {
      item.set('showAddControl', false);
      item.set('showRemoveControl', showRemoveControl);
    });
    jns.set('lastObject.showAddControl', showAddControl);
  }

});

