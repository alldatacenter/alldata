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

 App.QueueMappingComponent = Em.Component.extend({
   layoutName: 'components/queueMapping',

   queues: null,
   mappings: null,
   mappingsOverrideEnable: null,

   isShowing: false,
   queueMappings: [],
   leafQueueNames: [],
   selectedMapping: '',
   customUserMappings: '',
   customGroupMappings: '',
   selectedLeafQueueNameForUsers: null,
   selectedLeafQueueNameForGroups: null,
   isQueueMappingsDirty: false,
   scheduler: null,
   isOperator: false,

   actions: {
     showMappingOptions: function(){
       this.resetMappingOptions();
       this.set('isShowing', true);
     },
     hideMappingOptions: function(){
       this.set('isShowing', false);
     },
     addQueueMapping: function(){
       this.addQueueMapping();
     },
     removeQueueMapping: function(qm){
       this.get('queueMappings').removeObject(qm);
     },
     toggleMappingOverride: function() {
       this.toggleProperty('mappingsOverrideEnable');
     },
     rollbackProp: function(prop, item) {
       if (prop === "queue_mappings") {
         var oldMappings = (item.changedAttributes()[prop][0])? item.changedAttributes()[prop][0].split(',') : [];
         this.set('queueMappings', oldMappings);
       }
       this.sendAction("rollbackProp", prop, item);
     }
   },

   resetMappingOptions: function(){
     this.set('selectedMapping', '');
     this.set('customUserMappings', '');
     this.set('customGroupMappings', '');
   },

   parseMappings: function(){
     var mappings = this.get('mappings') || null;
     if (mappings) {
       this.set('queueMappings', mappings.split(',').filter(function(mapping){
         return mapping !== "";
       }) || []);
     }
   }.observes('mappings').on('init'),

   extractLeafQueueNames: function(){
     var that = this;
     var queues = this.get('queues') || [];
     var leafQs = queues.filterBy('queues', null);
     leafQs.forEach(function(q){
       that.get('leafQueueNames').pushObject(q.get('name'));
     });
   }.observes('queues.length').on('init'),

   addQueueMapping: function(){
     var that = this;
     if(this.get('selectedMapping') !== ''){
       if(this.get('selectedMapping') !== 'u:%name:%qname' && this.get('selectedMapping') !== 'g:%name:%qname'){
         this.get('queueMappings').pushObject(this.get('selectedMapping'));
       }else{
         if(this.get('selectedMapping') === 'u:%name:%qname' && this.get('customUserMappings').trim() !== ''
          && this.get('selectedLeafQueueNameForUsers') !== null){
           this.addCustomQueueMappings(this.get('customUserMappings'), this.get('selectedLeafQueueNameForUsers'));
         }else if(this.get('selectedMapping') === 'g:%name:%qname' && this.get('customGroupMappings').trim() !== ''
          && this.get('selectedLeafQueueNameForGroups') !== null){
           this.addCustomQueueMappings(this.get('customGroupMappings'), this.get('selectedLeafQueueNameForGroups'));
         }
       }
       this.resetMappingOptions();
     }
   },

   queueMappingsDidChange: function(){
     var csMappings = this.get('queueMappings').join(',') || null;
     this.set('mappings', csMappings);
   }.observes('queueMappings', 'queueMappings.length', 'queueMappings.@each'),

   addCustomQueueMappings: function(csValues, selectedLeafQName){
     var that = this;
       csValues = csValues.trim() || '',
       userOrGroupNames = csValues.split(',') || [],
       mappingPattern = this.get('selectedMapping');

     userOrGroupNames.forEach(function(ugname){
       that.get('queueMappings').pushObject(mappingPattern.replace('%name', ugname).replace('%qname', selectedLeafQName));
     });
   },

   isCustomUserMapping: function(){
     return this.get('selectedMapping').trim() === 'u:%name:%qname';
   }.property('selectedMapping'),

   isCustomGroupMapping: function(){
     return this.get('selectedMapping').trim() === 'g:%name:%qname';
   }.property('selectedMapping'),

   radioButton: Em.View.extend({
     tagName: 'input',
     type: 'radio',
     attributeBindings: ['type', 'name', 'value', 'checked:checked:'],
     click: function(){
       this.set("selection", this.$().val());
     },
     checked: function(){
       return this.get("value") === this.get("selection");
     }.property('selection')
   }),

   isCollapsed: false,
   doExpandCollapse: function(){
     var that = this;
     this.$('#collapseQueueMappingsBtn').on('click', function(e){
       Ember.run.next(that, function(){
         this.toggleProperty('isCollapsed');
       });
     });
   }.on('didInsertElement'),

   destroyEventListeners: function() {
     this.$('#collapseQueueMappingsBtn').off('click');
   }.on('willDestroyElement'),

   isMappingsDirty: function() {
     return this.get('scheduler').changedAttributes().hasOwnProperty('queue_mappings');
   }.property('scheduler.queue_mappings'),

   isOverrideEnableDirty: function() {
     return this.get('scheduler').changedAttributes().hasOwnProperty('queue_mappings_override_enable');
   }.property('scheduler.queue_mappings_override_enable')
 });
