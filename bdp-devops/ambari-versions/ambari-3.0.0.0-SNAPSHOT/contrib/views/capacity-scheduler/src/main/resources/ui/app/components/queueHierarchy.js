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

 App.QueueHierarchyComponent = Ember.Component.extend({
   layoutName: 'components/queueHierarchy',
   depth:0,
   parent:'',

   leafQs: function () {
     return this.get('queues')
       .filterBy('depth', this.get('depth'))
       .filterBy('parentPath', this.get('parent'));
   }.property('depth', 'parent', 'queues.length', 'queues.@each.name'),

   cildrenQueues: function() {
     var leafQs = this.get('leafQs'),
      deltedQs = this.get('deletedQs');

     var deletedAtDepth = deltedQs
      .filterBy('depth', this.get('depth'))
      .filterBy('parentPath', this.get('parent'));

     return leafQs.pushObjects(deletedAtDepth);
   }.property('leafQs.length', 'deletedQs.[]'),

   childDepth: function () {
     return this.get('leafQs.firstObject.depth') + 1;
   }.property('depth'),

   didInsertElement: function () {
     Ember.run.scheduleOnce('afterRender', null, this.setFirstAndLast, this);
   },

   setFirstAndLast: function (item) {
     var items = item.$().parents('.queue-hierarchy').find('.list-group-item');
     items.first().addClass('first');
     items.last().addClass('last');
   }

 });
