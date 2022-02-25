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

App.RecurceQueuesComponent = Em.View.extend({
  templateName: 'components/queueListItem',
  depth:0,
  parent:'',
  parentIsDeleted:false,
  queues: Ember.computed.union('controller.arrangedContent', 'controller.store.deletedQueues'),
  leaf:function () {
    return this.get('queues')
      .filterBy('depth',this.get('depth'))
      .filterBy('parentPath',this.get('parent'));
  }.property('depth','parent','controller.content.length','controller.content.@each.name'),
  childDepth:function () {
    return this.get('leaf.firstObject.depth')+1;
  }.property('depth'),
  didInsertElement:function () {
    Ember.run.scheduleOnce('afterRender', null, this.setFirstAndLast, this);
  },
  setFirstAndLast:function (item) {
    var items = item.$().parents('.queue-list').find('.list-group-item');
    items.first().addClass('first');
    items.last().addClass('last');
  },
  capacityBarView: Em.View.extend({

      classNameBindings:[':progress-bar','queue.overCapacity:progress-bar-danger:progress-bar-success'],

      attributeBindings:['capacityWidth:style'],

      /**
       * Formatting pattern.
       * @type {String}
       */
      pattern:'width: %@%;',

      /**
       * Alias for parentView.capacityValue.
       * @type {String}
       */
      value:Em.computed.alias('queue.capacity'),

      /**
       * Formats pattern whit value.
       * @return {String}
       */
      capacityWidth: function(c,o) {
        return  this.get('pattern').fmt((+this.get('value')<=100)?this.get('value'):100);
      }.property('value')
    })
});
