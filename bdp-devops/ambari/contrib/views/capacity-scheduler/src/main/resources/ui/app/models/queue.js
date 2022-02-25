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

App.Label = DS.Model.extend({
  queue: DS.belongsTo('queue'),
  capacity: DS.attr('number', { defaultValue: 0 }),
  maximum_capacity: DS.attr('number', { defaultValue: 0 }),
  forQueue:function() {
    return this.get('id').substring(0,this.get('id').lastIndexOf('.'));
  }.property('id'),
  name:function() {
    return this.get('id').substr(this.get('id').lastIndexOf('.')+1);
  }.property('id'),

  overCapacity:false,
  isNotExist:function () {
    return this.get('store.nodeLabels.content').findBy('name',this.get('name')).notExist;
  }.property('store.nodeLabels.content.@each.notExist'),
  isDefault: function () {
    return this.get('queue.default_node_label_expression') === this.get('name');
  }.property('queue.default_node_label_expression'),
  setCapacity: function(cap) {
    this.set('capacity', cap);
  },
  setMaxCapacity: function(maxCap) {
    this.set('maximum_capacity', maxCap);
  },
  isDirtyLabelCapacity: false,
  isDirtyLabelMaxCapacity: false,
  absolute_capacity: 0
});

App.Scheduler = DS.Model.extend({
  maximum_am_resource_percent: DS.attr('number', { defaultValue: 0 }),
  maximum_applications: DS.attr('number', { defaultValue: 0 }),
  node_locality_delay: DS.attr('number', { defaultValue: 0 }),
  resource_calculator: DS.attr('string', { defaultValue: '' }),
  queue_mappings: DS.attr('string'),
  queue_mappings_override_enable: DS.attr('boolean'),
  isAnyDirty:Em.computed.alias('isDirty')
});


/**
 * Represents tagged of configuraion vresion.
 *
 */
App.Tag = DS.Model.extend({
  tag:DS.attr('string'),
  isCurrent:function () {
    return this.get('tag') === this.get('store.current_tag');
  }.property('store.current_tag'),
  changed:function () {
    return (this.get('tag').match(/version[1-9][0-9][0-9]+/))?moment(+this.get('tag').replace('version','')):this.get('tag');
  }.property('tag')
});

/**
 * Represents the queue.
 *
 */
App.Queue = DS.Model.extend({
  labels: DS.hasMany('label'),

  labelsEnabled: false,

  sortBy:['name'],
  sortedLabels:Em.computed.sort('labels','sortBy'),

  hasNotValidLabels: function(attribute){
    return this.get('labels').anyBy('isValid',false);
  }.property('labels.@each.isValid'),

  _accessAllLabels:DS.attr('boolean'),
  accessAllLabels:function (key,val) {
    var labels = this.get('store.nodeLabels').map(function(label) {
      return this.store.getById('label',[this.get('id'),label.name].join('.'));
    }.bind(this));

    if (arguments.length > 1) {
      this.set('_accessAllLabels',val);

      if (this.get('_accessAllLabels')) {
          labels.forEach(function (lb) {
            var containsByParent = (Em.isEmpty(this.get('parentPath')))?true:this.store.getById('queue',this.get('parentPath').toLowerCase()).get('labels').findBy('name',lb.get('name'));
            if (!this.get('labels').contains(lb) && !!containsByParent) {
              this.get('labels').pushObject(lb);
            }
          }.bind(this));
          this.notifyPropertyChange('labels');
      }
      else {
        this.get('labels').clear();
      }
    }

    return this.get('_accessAllLabels');
  }.property('_accessAllLabels','labels'),

  labelsAccessWatcher:function () {
    Em.run.scheduleOnce('sync',this,'unsetAccessAllIfNeed');
  }.observes('labels','_accessAllLabels'),

  unsetAccessAllIfNeed:function () {
    if (!this.get('isDeleted') && this.get('labels.length') != this.get('store.nodeLabels.length')) {
      this.set('_accessAllLabels',false);
    }
  },

  clearLabels:function () {
    if (!this.get('labelsEnabled')) {
      this.recurseClearLabels(false);
    }
  }.observes('labelsEnabled'),

  recurseClearLabels:function(leaveEnabled) {
    this.set('accessAllLabels',false);
    this.get('labels').clear();
    this.notifyPropertyChange('labels');
    this.store.all('queue').filterBy('parentPath',this.get('path')).forEach(function (child) {
      if (!Em.isNone(leaveEnabled)) {
        child.set('labelsEnabled',leaveEnabled);
      }
      child.recurseClearLabels(leaveEnabled);
    });
  },

  /**
   * @param  {App.Label} label ralated to queue. All labels with it's name will be removed from child queues.
   * @method recurseRemoveLabel
   */
  recurseRemoveLabel:function(label) {
    label = this.get('labels').findBy('name',label.get('name'));
    if (label) {
      this.get('labels').removeObject(label);
      this.notifyPropertyChange('labels');
      this.store.all('queue').filterBy('parentPath',this.get('path')).forEach(function (child) {
        child.recurseRemoveLabel(label);
      }.bind(this));
    }
  },

  removeQueueNodeLabel: function(qLabel) {
    qLabel = this.get('labels').findBy('name', qLabel.get('name'));
    if (qLabel) {
      if (this.get('labels').contains(qLabel)) {
        this.get('labels').removeObject(qLabel);
      }
      if (!this.get('nonAccessibleLabels').contains(qLabel)) {
        this.get('nonAccessibleLabels').addObject(qLabel);
      }
      qLabel.setCapacity(0);
      qLabel.setMaxCapacity(100);
      this.notifyPropertyChange('labels');
      this.notifyPropertyChange('nonAccessibleLabels');
    }
  },

  recursiveRemoveChildQueueLabels: function(qLabel) {
    qLabel = this.get('labels').findBy('name', qLabel.get('name'));
    if (qLabel) {
      this.removeQueueNodeLabel(qLabel);
      var childrenQs = this.store.all('queue').filterBy('depth', this.get('depth') + 1).filterBy('parentPath', this.get('path'));
      childrenQs.forEach(function(child){
        child.recursiveRemoveChildQueueLabels(qLabel);
      }.bind(this));
    }
  },

  addQueueNodeLabel: function(qLabel) {
    qLabel = this.store.getById('label', [this.get('path'), qLabel.get('name')].join('.'));
    if (!this.get('labels').contains(qLabel)) {
      this.get('labels').addObject(qLabel);
    }
    if (this.get('nonAccessibleLabels').contains(qLabel)) {
      this.get('nonAccessibleLabels').removeObject(qLabel);
    }
    this.notifyPropertyChange('labels');
    this.notifyPropertyChange('nonAccessibleLabels');
  },

  recursiveAddChildQueueLabels: function(qLabel) {
    qLabel = this.store.getById('label', [this.get('path'), qLabel.get('name')].join('.'));
    this.addQueueNodeLabel(qLabel);
    var childrenQs = this.store.all('queue').filterBy('depth', this.get('depth') + 1).filterBy('parentPath', this.get('path'));
    childrenQs.forEach(function(child) {
      child.recursiveAddChildQueueLabels(qLabel);
    }.bind(this));
  },

  isAnyDirty: function () {
    return this.get('isDirty') || !Em.isEmpty(this.get('labels').findBy('isDirty',true)) || this.get('isLabelsDirty');
  }.property('isDirty','labels.@each.isDirty','initialLabels','isLabelsDirty'),

  initialLabels:[],
  labelsLoad:function() {
    this.set('labelsEnabled',this._data.labelsEnabled);
    this.set('initialLabels',this.get('labels').mapBy('id'));

    //Setting root label capacity to 100
    if (this.get('id') === 'root') {
      this.get('labels').forEach(function(label){
        label.set('capacity', 100);
      });
    }
  }.on('didLoad','didUpdate','didCreate'),

  isLabelsDirty:function () {
    var il = this.get('initialLabels').sort();
    var cl = this.get('labels').mapBy('id').sort();
    return !((il.length == cl.length) && il.every(function(element, index) {
      return element === cl[index];
    }));
  }.property('initialLabels', 'labels.[]', '_accessAllLabels'),

  noCapacity:Em.computed.equal('capacity',0),

  name: DS.attr('string'),
  parentPath: DS.attr('string'),
  depth: DS.attr('number'),
  path: DS.attr('string'),

  // queue props
  state: DS.attr('string', { defaultValue: 'RUNNING' }),
  acl_administer_queue: DS.attr('string', { defaultValue: '*' }),
  acl_submit_applications: DS.attr('string', { defaultValue: '*' }),
  ordering_policy: DS.attr('string', { defaultValue: 'fifo' }),
  enable_size_based_weight: DS.attr('boolean', { defaultValue: false }),
  default_node_label_expression: DS.attr('string'),

  capacity: DS.attr('number', { defaultValue: 0 }),
  maximum_capacity: DS.attr('number', { defaultValue: 0 }),
  absolute_capacity: 0,
  isDirtyCapacity: false,
  isDirtyMaxCapacity: false,

  user_limit_factor: DS.attr('number', { defaultValue: 1 }),
  minimum_user_limit_percent: DS.attr('number', { defaultValue: 100 }),
  maximum_applications: DS.attr('number', { defaultValue: null }),
  maximum_am_resource_percent: DS.attr('number', { defaultValue: null }),
  priority: DS.attr('number', {defaultValue: 0}),
  maximum_allocation_mb:DS.attr('number'),
  maximum_allocation_vcores:DS.attr('number'),
  maximum_application_lifetime:DS.attr('number'),
  default_application_lifetime:DS.attr('number'),

  disable_preemption: DS.attr('string', {defaultValue: ''}),
  isPreemptionInherited: DS.attr('boolean', {defaultValue: true}),
  isPreemptionOverriden: false,

  queues: DS.attr('string'),
  queuesArray:function (key,val) {
    var qrray;
    if (arguments.length > 1) {
      if (typeof val === 'object' && val.hasOwnProperty('exclude')) {
        qrray = (this.get('queues'))?this.get('queues').split(','):[];
        this.set('queues',qrray.removeObject(val.exclude).join(',') || null);
      } else {
        this.set('queues',val.sort().join(',') || null);
      }
    }
    return (this.get('queues'))?this.get('queues').split(','):[];
  }.property('queues'),

  _overCapacity:false,
  overCapacity:function(key,val) {
    if (arguments.length > 1) {
      this.set('_overCapacity',val);
    }

    return this.get('_overCapacity') || !Em.isEmpty(this.get('labels').filterBy('overCapacity'));
  }.property('_overCapacity','labels.@each.overCapacity'),

  childrenQueues: function() {
    var queuesArray = this.get('queuesArray');
    return this.store.all('queue')
      .filterBy('depth', this.get('depth') + 1)
      .filterBy('parentPath', this.get('path'))
      .filter(function(queue) {
        return queuesArray.contains(queue.get('name'));
      });
  }.property('queues'),

  isInvalidMaxCapacity: false,
  isInvalidLabelMaxCapacity: false,

  nonAccessibleLabels: [],

  //queue state from resource manager
  rmQueueState: 'UNKNOWN',

  //new queue flag
  isNewQueue:DS.attr('boolean', {defaultValue: false}),

  version:null,
  clearTag:function () {
    this.set('version', null);
  }.observes(
    'name',
    'parentPath',
    'depth',
    'path',
    'state',
    'acl_administer_queue',
    'acl_submit_applications',
    'capacity',
    'maximum_capacity',
    'unfunded_capacity',
    'user_limit_factor',
    'minimum_user_limit_percent',
    'maximum_applications',
    'maximum_am_resource_percent',
    'ordering_policy',
    'queues',
    'enable_size_based_weight',
    'labels.@each.capacity',
    'labels.@each.maximum_capacity'
  ),

  clearDefaultNodeLabel: function () {
    if (Em.isEmpty(this.get('labels').findBy('name',this.get('default_node_label_expression')))) {
      this.set('default_node_label_expression',null);
    }
  }.observes('labels','default_node_label_expression'),

  isLeafQ: function() {
    return this.get('queues') === null;
  }.property('queues'),

  /**
   * To reset the maximum_application_lifetime and default_application_lifetime if current Q is no longer Leaf Queue 
   */
  watchChangeLeafQueue: function () {
    if (this.get('isLeafQ') == false) {
      this.set('maximum_application_lifetime', null);
      this.set('default_application_lifetime', null);
    }
  }.observes('isLeafQ')
});
