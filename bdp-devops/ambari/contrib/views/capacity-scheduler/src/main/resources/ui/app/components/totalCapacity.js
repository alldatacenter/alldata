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

App.TotalCapacityComponent = Ember.Component.extend({
  layoutName:'components/totalCapacity',

  // TAKEN VALUES

  /**
   * Speaks for itself.
   * @type {App.Queue}
   */
  currentQueue:null,

  /**
   * All queues in store.
   * @type {DS.RecordArray}
   */
  allQueues:[],

  /**
   * All queues in store sorted by App.QueuesController
   * @type {Array}
   */
  allQueuesArranged:[],

  /**
   * User admin status.
   * @type {Boolean}
   */
  isOperator:false,

  /**
   * Target for rollbackProp action.
   * @type {String}
   */
  rollbackProp:'',

  // ACTIONS

  actions:{
    toggleProperty:function (property,target) {
      target = target || this;
      target.toggleProperty(property);
    },
    rollbackProp:function (prop, item) {
      this.sendAction('rollbackProp', prop, item);
    },
    toggleDefaultLabel:function (queue, label) {
      if (queue.get('default_node_label_expression') === label.get('name')) {
        queue.set('default_node_label_expression',null);
      } else {
        queue.set('default_node_label_expression',label.get('name'));
      }
    },
    toggleLabel:function (labelName, queue) {
      var q = queue || this.get('currentQueue'),
          labelRecord = q.store.getById('label',[q.get('path'),labelName].join('.').toLowerCase());

      if (q.get('labels').contains(labelRecord)) {
        q.recurseRemoveLabel(labelRecord);
      } else {
        q.get('labels').pushObject(labelRecord);
        q.notifyPropertyChange('labels');
      }
    }
  },

  // COMPUTED PROPERTIES

  /**
   * Path for current queue parent.
   * @type {String}
   */
  currentPrPath:Em.computed.alias('currentQueue.parentPath'),

  /**
   * Current queue parent.
   * @type {App.Queue}
   */
  parentQueue:function () {
    return this.allQueues.findBy('path',this.get('currentPrPath'));
  }.property('allQueues','currentPrPath'),

  /**
   * Leaf queues.
   * @type {Array}
   */
  leafQueues:function () {
    return this.allQueuesArranged.filterBy('parentPath',this.get('currentPrPath')).filterBy('isNew',false);
  }.property('allQueuesArranged.length','currentPrPath'),

  /**
   * Array of leaf queues capacities.
   * @type {Array}
   */
  leafQueuesCapacity: Ember.computed.map('leafQueues.@each.capacity', function (queue) {
    return +queue.get('capacity');
  }),

  /**
   * Sum of leaf queues capacities.
   * @type {Number}
   */
  totalCapacity: function() {
    var leafCaps = this.get('leafQueuesCapacity');
    var total = leafCaps.reduce(function(prev, cap) {
      return prev + cap;
    }, 0);
    total = parseFloat(total.toFixed(3));
    return total;
  }.property('leafQueues', 'leafQueues.length', 'leafQueues.@each.capacity'),

  /**
   * Node labels stored in store.
   * @type {Array}
   */
  nodeLabels:Ember.computed.alias('currentQueue.store.nodeLabels.content'),

  /**
   * Array of arrays of node labels.
   * Made to split node labels in several rows.
   *
   * @return {Array}
   */
  arrangedNodeLabels:function () {
    var llength = this.get('nodeLabels.length'),
        cols = (llength % 4 === 1)?3:4,
        arranged = [];

    this.get('nodeLabels').forEach(function (l, idx, all) {
      if (idx%cols === 0) {
        arranged.pushObject([l]);
      } else {
        arranged.get('lastObject').pushObject(l)
      }
    });

    return arranged;
  }.property('nodeLabels.length'),

  // OBSERVABLES

  /**
   * Marks currentQueue with 'isCurrent' flag.
   * @method currentInLeaf
   */
  currentInLeaf:function () {
    var queues = this.get('allQueues');
    queues.setEach('isCurrent',false);
    if(!this.get('currentQueue.currentState.stateName').match(/root.deleted/)) {
      this.get('currentQueue').set('isCurrent',true);
    }
  }.observes('leafQueues','currentQueue').on('init'),

  // CHILD VIEWS

  /**
   * Toggle buttons for node labels.
   * @type {Em.View}
   */
  nodeLabelsToggles: Em.View.extend(Ember.ViewTargetActionSupport,{

    // TAKEN VALUES

    /**
     * Name of node label.
     * @type {[type]}
     */
    labelName:null,
    /**
     * Node label's existence flag.
     * @type {Boolean}
     */
    notExist:false,

    /**
     * Queue related to label.
     * @type {App.Queue}
     */
    queue:null,

    /**
     * Queue's leaf.
     * @type {Array}
     */
    leaf:null,

    // VIEW'S ATTRIBUTES

    tagName:'label',

    classNames:['btn','btn-default'],

    classNameBindings:['isActive:active','warning','lockedByParent:disabled','notExist:not-exist'],

    action:'toggleLabel',

    click:function () {
      if (this.get('parentView.isOperator')) {
        this.triggerAction({
          actionContext: [this.get('labelName'),this.get('queue')]
        });
        Em.run.next(function() {
          this.notifyPropertyChange('currentLabel');
        }.bind(this));
      }
    },

    /**
     * Current capacity value of current label.
     * Update only when value realy changes.
     * @type {Number}
     */
    currentCapacity:null,

    // COMPUTED PROPERTIES

    /**
     * Returns true if parent queue has no access to label with 'labelName'.
     * @return {Boolean}
     */
    lockedByParent:function () {
      var parent = this.get('controller.parentQueue');
      return (Em.isEmpty(parent))?false:!parent.get('labels').findBy('name',this.get('labelName'));
    }.property('controller.parentQueue'),

    /**
     * Returns true if current queue has access to label with 'labelName'.
     * @return {Boolean}
     */
    isActive:function () {
      return  !this.get('queue.labels.isDestroying') && this.get('queue.labels').mapBy('name').contains(this.get('labelName'));
    }.property('queue.labels.[]'),

    /**
     * Label record of type App.Label related to current queue.
     * @return {App.Label}
     */
    currentLabel:function () {
      return !this.get('queue.labels.isDestroying') && this.get('queue.labels').findBy('name',this.get('labelName'));
    }.property('labelName','queue.labels.length'),

    /**
     * Label records from leaf queues, that has label with 'labelName'.
     * @return {Array}
     */
    labels:function () {
      return this.get('leaf').map(function (item) {
        return item.get('labels').findBy('name',this.get('labelName'));
      }.bind(this)).compact();
    }.property('leaf.@each.labels.[]'),

    /**
     * Sum of label records from leaf queues, that has label with 'labelName'.
     * @return {Number}
     */
    capacityValue:function () {
      var total = this.get('labels').reduce(function (prev, label) {
        return prev + ((label && label.get('capacity'))?+label.get('capacity'):0);
      }, 0);
      total = parseFloat(total.toFixed(3));
      return total;
    }.property('labels.@each.capacity','labels.[]'),

    /**
     * Returns true if total capacity of node labels in leaf are greater than 100.
     * @type {Boolean}
     */
    warning:Em.computed('capacityValue',function() {
      return this.get('capacityValue') != 100;
    }),

    // OBSERVABLES

    /**
     * Marks labels with 'overCapacity' mark if they can't have such capacity value.
     * @method capacityWatcher
     */
    capacityWatcher:function () {
      Em.run.next(this,function () {
        if (!!this.get('labels')) {
          this.get('labels').setEach('overCapacity',this.get('warning'));
        }
      });
    }.observes('warning').on('didInsertElement'),

    /**
     * Initializes tooltip on button when inserted to DOM.
     * @method capacityTooltip
     */
    capacityTooltip:function () {
      this.$().tooltip({
        title: function  () {
          return this.get('capacityValue') + '';
        }.bind(this),
        container:"#"+this.elementId,
        animation:false
      });
    }.on('didInsertElement'),

    /**
     * Marks buttons with 'first' and 'last' classes when inserted to DOM.
     * @method setFirstAndLast
     */
    setFirstAndLast:function (item) {
      var items = this.$().parents('.labels-toggle').find('label');
      items.first().addClass('first');
      items.last().addClass('last');
    }.on('didInsertElement'),

    /**
     * Timer information with callback that hides tooltip.
     * @type {Array}
     */
    isShownTimer:null,

    /**
     * Triggers tooltip when capacity on labels changes.
     * @return {[type]} [description]
     */
    triggerCapacityTooltip:function() {
      if (!(Em.isNone(this.$()) || !this.get('queue.labelsEnabled') || Em.isNone(this.get('currentLabel')))
          && this.get('currentLabel.capacity') != this.get('currentCapacity')) {
        Em.run.scheduleOnce('afterRender', this, 'showCapacityTooltip');
      }
    }.observes('currentLabel.capacity'),

    /**
     * Shows tooltip when label's capacity value changes.
     * @method showCapacityTooltip
     */
    showCapacityTooltip: function() {
      if (this._state === 'inDOM') {
        this.$().tooltip('show');
        this.set('currentCapacity',this.get('currentLabel.capacity'));
        Em.run.cancel(this.get('isShownTimer'));
        this.set('isShownTimer',Em.run.later(this,function() {
          if (this.$()) this.$().tooltip('hide');
        },500));
      }
    },

    /**
     * Destrioy tooltips when element destroys.
     * @method willClearRender
     */
    willClearRender:function() {
      this.$().tooltip('destroy');
    },

    // CHILD VIEWS

    /**
     * Bar that shows total capcity value on node label.
     * @type {Em.View}
     */
    nodeLabelsBar:Em.View.extend({

      classNameBindings:[':progress-bar'],

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
      value:Em.computed.alias('parentView.capacityValue'),

      /**
       * Formats pattern whit value.
       * @return {String}
       */
      capacityWidth: function(c,o) {
        return  this.get('pattern').fmt((+this.get('value')<=100)?this.get('value'):100);
      }.property('value')
    }),

    /**
     * Bar that shows current label capcity on node label.
     * @type {Em.View}
     */
    currentQueueBar:Em.View.extend({

      classNameBindings:[':progress-bar',':ghost'],

      attributeBindings:['capacityWidth:style'],

      /**
       * Formatting pattern.
       * @type {String}
       */
      pattern:'width: %@%;',

      /**
       * Alias for parentView.currentLabel.capacity.
       * @type {String}
       */
      value:Em.computed.alias('parentView.currentLabel.capacity'),

      capacityWidth: function(c,o) {
        return  this.get('pattern').fmt((+this.get('value')<=100)?this.get('value'):0);
      }.property('parentView.currentLabel.capacity','parentView.queue.labels.length')
    })
  })
});

App.CapacityEditFormView = Em.View.extend({
  dirty_capacity:function () {
    return this.get('controller.content').changedAttributes().hasOwnProperty('capacity');
  }.property('controller.content.capacity'),
  dirty_maxcapacity:function () {
    return this.get('controller.content').changedAttributes().hasOwnProperty('maximum_capacity');
  }.property('controller.content.maximum_capacity')
});
