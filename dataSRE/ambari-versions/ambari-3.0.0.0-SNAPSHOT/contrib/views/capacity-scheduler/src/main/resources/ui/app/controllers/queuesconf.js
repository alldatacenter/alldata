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

var _runState = 'RUNNING';
var _stopState = 'STOPPED';

App.CapschedQueuesconfController = Ember.Controller.extend({
  needs: ['capsched'],
  queues: Ember.computed.alias('controllers.capsched.queues'),
  isOperator: Ember.computed.alias('controllers.capsched.isOperator'),
  allNodeLabels: Ember.computed.alias('store.nodeLabels.content'),
  isRmOffline: Ember.computed.alias('store.isRmOffline'),
  isNodeLabelsEnabledByRM: Ember.computed.alias('store.isNodeLabelsEnabledByRM'),
  isNodeLabelsConfiguredByRM: Ember.computed.alias('store.isNodeLabelsConfiguredByRM'),
  allNodeLabelRecords: [],
  queuesNeedRefresh: Ember.computed.alias('store.queuesNeedRefresh'),
  isRefreshOrRestartNeeded: false,
  deletedQueues: Ember.computed.alias('store.deletedQueues'),

  actions: {
    addNewQueue: function() {
      this.set('newQueueName', '');
      this.set('showQueueNameInput', true);
      this.set('isCreatingNewQueue', true);
    },
    createNewQueue: function() {
      if (this.validateQueueName()) {
        return;
      }
      this.set('isCreatingNewQueue', false);
      var store = this.get('store'),
      queueName = this.get('newQueueName'),
      parentPath = this.get('selectedQueue.path'),
      queuePath = [parentPath, queueName].join('.'),
      depth = parentPath.split('.').length,
      leafQueueNames = store.getById('queue', parentPath.toLowerCase()).get('queuesArray'),
      newInLeaf = Ember.isEmpty(leafQueueNames),
      totalLeafCapacity,
      freeLeafCapacity,
      newQueue;

      this.send('clearCreateQueue');
      if (!newInLeaf) {
        totalLeafCapacity = leafQueueNames.reduce(function (capacity, qName) {
          return store.getById('queue', [parentPath, qName].join('.').toLowerCase()).get('capacity') + capacity;
        }, 0);
        freeLeafCapacity = (totalLeafCapacity < 100) ? 100 - totalLeafCapacity : 0;
      }
      var qCapacity = (newInLeaf) ? 100 : freeLeafCapacity;
      var requireRestart = (this.get('selectedQueue.isLeafQ') && !this.get('selectedQueue.isNewQueue'))? true : false;
      newQueue = store.createRecord('queue', {
        id: queuePath.toLowerCase(),
        name: queueName,
        path: queuePath,
        parentPath: parentPath,
        depth: depth,
        isNewQueue: true,
        capacity: qCapacity,
        maximum_capacity: 100,
        requireRestart: requireRestart
      });
      this.set('newQueue', newQueue);
      store.saveAndUpdateQueue(newQueue).then(Em.run.bind(this, 'saveAndUpdateQueueSuccess', newQueue));

      if (!this.get('queuesNeedRefresh').findBy('path', queuePath)) {
        this.get('queuesNeedRefresh').addObject({
          path: queuePath,
          name: queueName
        });
      }
    },
    clearCreateQueue: function() {
      this.set('newQueueName', '');
      this.set('showQueueNameInput', false);
      this.set('isInvalidQueueName', false);
      this.set('invalidQueueNameMessage', '');
      this.set('isCreatingNewQueue', false);
    },
    discardQueuesChanges: function() {
      var tempRefreshNeeded = this.get('isRefreshOrRestartNeeded');
      var tempRestart = this.get('isQueuesMustNeedRestart');
      var allQueues = this.get('queues');
      allQueues.forEach(function(qq){
        var qAttrs = qq.changedAttributes();
        for (var qProp in qAttrs) {
          if (qAttrs.hasOwnProperty(qProp)) {
            qq.set(qProp, qAttrs[qProp][0]);
          }
        }
        var labels = qq.get('labels');
        labels.forEach(function(lb){
          var lbAttrs = lb.changedAttributes();
          for (var lbProp in lbAttrs) {
            if (lbAttrs.hasOwnProperty(lbProp)) {
              lb.set(lbProp, lbAttrs[lbProp][0]);
            }
          }
        });
        //Setting root label capacities back to 100, if discard changes set root label capacity to 0.
        if (qq.get('id') === 'root') {
          qq.get('labels').setEach('capacity', 100);
        }
      });
      this.set('isRefreshOrRestartNeeded', tempRefreshNeeded);
      this.set('isQueuesMustNeedRestart', tempRestart);
    },
    stopQueue: function() {
      this.set('selectedQueue.state', _stopState);
    },
    startQueue: function() {
      this.set('selectedQueue.state', _runState);
    },
    deleteQueue: function() {
      var that = this;
      var delQ = this.get('selectedQueue');
      if (delQ.get('isNew')) {
        this.set('newQueue', null);
      }
      this.transitionToRoute('capsched.queuesconf.editqueue', delQ.get('parentPath').toLowerCase())
      .then(Em.run.schedule('afterRender', function () {
        that.get('store').recurceRemoveQueue(delQ);
      }));
    },
    showSaveConfigDialog: function(mode) {
      if (mode) {
        this.set('saveMode', mode);
      } else {
        this.set('saveMode', '');
      }
      this.set('isSaveConfigDialogOpen', true);
    },
    showConfirmDialog: function() {
      this.set('isConfirmDialogOpen', true);
    },
    saveCapSchedConfigs: function() {
      if (this.get('saveMode') === '') {
        var holder = {
          refresh: this.get('isRefreshOrRestartNeeded')
        };
        this.set('tempRefreshHolder', holder);
      } else {
        this.set('tempRefreshHolder', null);
      }
      return true;
    }
  },

  initEvents: function() {
    this.get('eventBus').subscribe('beforeSavingConfigs', function() {
      this.set("tempRefreshNeed", this.get('isRefreshOrRestartNeeded') || false);
      this.set("tempMustRestart", this.get('isQueuesMustNeedRestart') || false);
    }.bind(this));

    this.get('eventBus').subscribe('afterConfigsSaved', function(refresh) {
      if (refresh !== undefined) {
        this.set('isRefreshOrRestartNeeded', refresh);
        this.set('isQueuesMustNeedRestart', refresh);
      } else {
        this.set('isRefreshOrRestartNeeded', this.get('tempRefreshNeed'));
        this.set('isQueuesMustNeedRestart', this.get('tempMustRestart'));
      }
    }.bind(this));
  }.on('init'),

  teardownEvents: function() {
    this.get('eventBus').unsubscribe('beforeSavingConfigs');
    this.get('eventBus').unsubscribe('afterConfigsSaved');
  }.on('willDestroy'),

  isAnyQueueDirty: function() {
    return this.get('queues').isAny('isAnyDirty', true);
  }.property('queues.@each.isAnyDirty'),

  isQueuesNeedRefreshOrRestart: function() {
    var isNeed = this.get('isAnyQueueDirty') || this.get('needRefresh')
      || this.get('queuesRequireRestart') || this.get('isRefreshOrRestartNeeded');
    return isNeed;
  }.property('isAnyQueueDirty', 'needRefresh', 'queuesRequireRestart', 'isRefreshOrRestartNeeded', 'canNotSave'),

  isQueuesMustNeedRestart: false,

  forceRefreshOrRestartRequired: function() {
    return !this.get('isAnyQueueDirty') && (this.get('isRefreshOrRestartNeeded') || this.get('isQueuesMustNeedRestart'));
  }.property('isAnyQueueDirty', 'isRefreshOrRestartNeeded', 'isQueuesMustNeedRestart'),

  refreshRestartWatcher: function() {
    if (this.get('tempRefreshHolder')) {
      var holder = this.get('tempRefreshHolder');
      this.set('isRefreshOrRestartNeeded', holder.refresh||false);
      this.set('tempRefreshHolder', null);
    }
  }.observes('isRefreshOrRestartNeeded'),

  isDirtyQueuesNeedRefreshQsOp: function() {
    if (this.get('isAnyQueueDirty') || this.get('needRefresh') || this.get('isRefreshOrRestartNeeded')) {
      if (this.get('queuesRequireRestart')) {
        return false;
      }
      return true;
    } else {
      return false;
    }
  }.property('isAnyQueueDirty', 'needRefresh', 'queuesRequireRestart', 'isRefreshOrRestartNeeded', 'canNotSave'),

  isDirtyQueuesNeedRestartRmOp: function() {
    if (this.get('queuesRequireRestart') || this.get('isQueuesMustNeedRestart')) {
      return true;
    } else {
      return false;
    }
  }.property('isAnyQueueDirty', 'queuesRequireRestart', 'isQueuesMustNeedRestart', 'canNotSave'),

  showNeedRefreshOrRestartWarning: function() {
    return this.get('isQueuesNeedRefreshOrRestart') && !this.get('canNotSave');
  }.property('isQueuesNeedRefreshOrRestart', 'canNotSave'),

  selectedQueue: null,
  newQueue: null,
  newQueueName: '',
  showQueueNameInput: false,
  isInvalidQueueName: false,
  invalidQueueNameMessage: '',

  isCreatingNewQueue: false,
  isEditingQueueName: false,

  isCreaingOrRenamingQueue: function() {
    return this.get('isCreatingNewQueue')||this.get('isEditingQueueName');
  }.property('isCreatingNewQueue', 'isEditingQueueName'),

  validateQueueName: function() {
    var parentPath = this.get('selectedQueue.path'),
    queueName = this.get('newQueueName'),
    queuePath = [parentPath, queueName].join('.'),
    qAlreadyExists = (this.get('queues').findBy('name', queueName))? true : false;
    if (Ember.isBlank(queueName)) {
      this.set('isInvalidQueueName', true);
      this.set('invalidQueueNameMessage', 'Enter queue name');
    } else if (qAlreadyExists) {
      this.set('isInvalidQueueName', true);
      this.set('invalidQueueNameMessage', 'Queue already exists');
    } else if (queueName.indexOf(' ') > -1) {
      this.set('isInvalidQueueName', true);
      this.set('invalidQueueNameMessage', 'Queue name contains white spaces');
    } else {
      this.set('isInvalidQueueName', false);
      this.set('invalidQueueNameMessage', '');
    }
    return this.get('isInvalidQueueName');
  },

  queueNameDidChange: function() {
    this.validateQueueName();
  }.observes('newQueueName', 'newQueueName.length'),

  initNodeLabelRecords: function() {
    if (!this.get('isNodeLabelsEnabledByRM') || !this.get('isNodeLabelsConfiguredByRM') || this.get('isRmOffline')) {
      return;
    }
    this.setDefaultNodeLabelAccesses('root');
    this.checkNodeLabelsInvalidCapacity('root');
    var allQs = this.get('queues'),
    allLabels = this.get('allNodeLabels'),
    store = this.get('store'),
    records = [],
    nonAccessible = [],
    ctrl = this;
    allQs.forEach(function(queue) {
      nonAccessible = [];
      allLabels.forEach(function(label) {
        var qLabel = store.getById('label', [queue.get('path'), label.name].join('.'));
        if (qLabel) {
          if (!queue.get('labels').contains(qLabel)) {
            nonAccessible.pushObject(qLabel);
          }
          records.pushObject(qLabel);
        }
      });
      queue.set('nonAccessibleLabels', nonAccessible);
    });
    this.set('allNodeLabelRecords', records);
  }.on('init'),

  setDefaultNodeLabelAccesses: function(queuePath) {
    var allLabels = this.get('allNodeLabels'),
    store = this.get('store'),
    ctrl = this,
    queue = store.getById('queue', queuePath.toLowerCase()),
    parentQ = store.getById('queue', queue.get('parentPath').toLowerCase()),
    children = store.all('queue').filterBy('depth', queue.get('depth') + 1).filterBy('parentPath', queue.get('path'));

    if (Ember.isEmpty(queue.get('labels'))) {
      if (parentQ && parentQ.get('labels.length') > 0) {
        parentQ.get('labels').forEach(function(label) {
          var qlabel = store.getById('label', [queue.get('path'), label.get('name')].join('.'));
          if (qlabel) {
            queue.addQueueNodeLabel(qlabel);
          }
        });
      } else {
        allLabels.forEach(function(label) {
          var qlabel = store.getById('label', [queue.get('path'), label.name].join('.'));
          if (qlabel) {
            queue.addQueueNodeLabel(qlabel);
          }
        });
      }
    }

    if (queue.get('name') === 'root') {
      queue.get('labels').setEach('capacity', 100);
    }

    children.forEach(function(child) {
      ctrl.setDefaultNodeLabelAccesses(child.get('path'));
    });
  },

  checkNodeLabelsInvalidCapacity: function(queuePath) {
    var allLabels = this.get('allNodeLabels'),
    store = this.get('store'),
    ctrl = this,
    queue = store.getById('queue', queuePath.toLowerCase()),
    children = store.all('queue').filterBy('depth', queue.get('depth') + 1).filterBy('parentPath', queue.get('path'));

    allLabels.forEach(function(lab) {
      var total = 0;
      children.forEach(function(child) {
        var cLabel = child.get('labels').findBy('name', lab.name);
        if (cLabel) {
          total += cLabel.get('capacity');
        }
      });
      if (total !== 100) {
        children.forEach(function(child) {
          var cLabel = child.get('labels').findBy('name', lab.name);
          if (cLabel) {
            cLabel.set('overCapacity', true);
          }
        });
      }
    });

    children.forEach(function(child) {
      ctrl.checkNodeLabelsInvalidCapacity(child.get('path'));
    });
  },

  /**
   * Marks each queue in leaf with 'overCapacity' if sum if their capacity values is greater than 100.
   * @method capacityControl
   */
  capacityControl: function() {
    var paths = this.get('queues').getEach('parentPath').uniq();
    paths.forEach(function (path) {
      var leaf = this.get('queues').filterBy('parentPath', path),
      total = leaf.reduce(function (prev, queue) {
          return +queue.get('capacity') + prev;
        }, 0);
      leaf.setEach('overCapacity', total != 100);
    }.bind(this));
  }.observes('queues.length','queues.@each.capacity'),

  newQueuesNeedRestart: function() {
    return this.get('queues')
      .filterBy('isNewQueue', true)
      .filterBy('requireRestart', true);
  }.property('queues.length', 'queues.@each.isNewQueue', 'queues.@each.requireRestart'),

  hasNewQueuesNeedRestart: Ember.computed.notEmpty('newQueuesNeedRestart.[]'),

  queuesRequireRestart: Ember.computed.or('needRestart', 'hasNewQueuesNeedRestart', 'isQueuesMustNeedRestart'),

  disableRefreshBtn: function() {
    var disable = this.get('canNotSave') || this.get('queuesRequireRestart');
    return disable;
  }.property('canNotSave', 'queuesRequireRestart', 'isAnyQueueDirty'),

  restartWatcher: function() {
    var restart = this.get('needRestart') || this.get('hasNewQueuesNeedRestart');
    this.set('isQueuesMustNeedRestart', restart);
  }.observes('needRestart', 'hasNewQueuesNeedRestart'),

  dirtyQueueWatcher: function() {
    if (this.get('isAnyQueueDirty')) {
      this.set('isRefreshOrRestartNeeded', true);
    } else {
      this.set('isRefreshOrRestartNeeded', false);
    }
  }.observes('isAnyQueueDirty'),

  /**
   * True if newQueue is not empty.
   * @type {Boolean}
   */
  hasNewQueue: Ember.computed.bool('newQueue'),

  /**
   * True if some queue of desired configs was removed.
   * @type {Boolean}
   */
  hasDeletedQueues: Ember.computed.alias('store.hasDeletedQueues'),

  /**
   * Check properties for refresh requirement
   * @type {Boolean}
   */
  needRefreshProps: Ember.computed.any('hasChanges', 'hasNewQueues', 'hasDirtyNodeLabels'),

  dirtyNodeLabels: function() {
    return this.get('allNodeLabelRecords').filter(function (label) {
      return label.get('isDirty');
    });
  }.property('allNodeLabelRecords.@each.isDirty'),

  hasDirtyNodeLabels: Ember.computed.notEmpty('dirtyNodeLabels.[]'),

  /**
   * List of modified queues.
   * @type {Array}
   */
  dirtyQueues:function () {
    return this.get('queues').filter(function (q) {
      return q.get('isAnyDirty');
    });
  }.property('queues.@each.isAnyDirty'),

  /**
   * True if dirtyQueues is not empty.
   * @type {Boolean}
   */
  hasChanges: Ember.computed.notEmpty('dirtyQueues.[]'),

  /**
   * List of new queues.
   * @type {Array}
   */
  newQueues: Ember.computed.filterBy('queues', 'isNewQueue', true),

  /**
   * True if newQueues is not empty.
   * @type {Boolean}
   */
  hasNewQueues: Ember.computed.notEmpty('newQueues.[]'),

  /**
   * check if RM needs restart
   * @type {bool}
   */
  needRestart: Ember.computed.and('hasDeletedQueues', 'isOperator'),

  /**
   * check there is some changes for save
   * @type {bool}
   */
  needSave: Em.computed.any('needRestart', 'needRefresh'),

  /**
   * check if RM needs refresh
   * @type {bool}
   */
  needRefresh: Em.computed.and('needRefreshProps', 'noNeedRestart', 'isOperator'),

  /**
   * Inverted needRestart value.
   * @type {Boolean}
   */
  noNeedRestart: Em.computed.not('needRestart'),

  /**
   * check if can save configs
   * @type {bool}
   */
  canNotSave: Em.computed.any('hasOverCapacity', 'hasInvalidMaxCapacity', 'hasInvalidLabelMaxCapacity', 'hasIncompletedAddings', 'hasNotValid', 'hasNotValidLabels'),

  /**
   * List of not valid queues.
   * @type {Array}
   */
  notValid: Em.computed.filterBy('queues', 'isValid', false),

  /**
   * True if notValid is not empty.
   * @type {Boolean}
   */
  hasNotValid: Em.computed.notEmpty('notValid.[]'),

  /**
   * True if queues have not valid labels.
   * @type {Boolean}
   */
  hasNotValidLabels: function() {
    return this.get('queues').anyBy('hasNotValidLabels',true);
  }.property('queues.@each.hasNotValidLabels'),

  /**
   * List of queues with excess of capacity
   * @type {Array}
   */
  overCapacityQ: function () {
    return this.get('queues').filter(function (q) {
      return q.get('overCapacity');
    });
  }.property('queues.@each.overCapacity'),

  /**
   * True if overCapacityQ is not empty.
   * @type {Boolean}
   */
  hasOverCapacity: Em.computed.notEmpty('overCapacityQ.[]'),

  /**
   * True if any queue has invalid max capacity (if max_cappacity < capacity).
   * @type {Boolean}
   */
  hasInvalidMaxCapacity: function() {
    return this.get('queues').anyBy('isInvalidMaxCapacity', true);
  }.property('queues.@each.isInvalidMaxCapacity'),

  hasInvalidLabelMaxCapacity: function() {
    return this.get('queues').anyBy('isInvalidLabelMaxCapacity', true);
  }.property('queues.@each.isInvalidLabelMaxCapacity'),

  /**
   * List of queues with incomplete adding process
   * @type {[type]}
   */
  incompletedAddings: Em.computed.filterBy('queues', 'isNew', true),

  /**
   * True if uncompetedAddings is not empty.
   * @type {Boolean}
   */
  hasIncompletedAddings: Em.computed.notEmpty('incompletedAddings.[]'),

  /**
   * Represents queue run state. Returns true if state is null.
   * @return {Boolean}
   */
  isSelectedQRunning: function() {
    if (!this.get('selectedQueue.isNewQueue')) {
      return this.get('selectedQueue.state') === _runState || this.get('selectedQueue.state') === null;
    } else {
      return false;
    }
  }.property('selectedQueue.state', 'selectedQueue.isNewQueue'),

  isSelectedQStopped: function() {
    return this.get('selectedQueue.state') === _stopState;
  }.property('selectedQueue.state'),

  isSelectedQLeaf: function() {
    return this.get('selectedQueue.queues') === null;
  }.property('selectedQueue.queues'),

  canAddChildrenQueues: function() {
    return this.get('isRmOffline') || !this.get('isSelectedQLeaf') || this.get('isSelectedQueueRmStateNotRunning');
  }.property('isRmOffline', 'isSelectedQLeaf', 'isSelectedQueueRmStateNotRunning'),

   isRootQSelected: Ember.computed.match('selectedQueue.id', /^(root)$/),

   isSelectedQueueRmStateNotRunning: function() {
     return this.get('selectedQueue.isNewQueue') || this.get('selectedQueue.rmQueueState') === _stopState;
   }.property('selectedQueue.rmQueueState', 'selectedQueue.isNewQueue'),

   isSelectedQueueDeletable: function() {
     return !this.get('isRootQSelected') && (this.get('isRmOffline') || this.get('isSelectedQueueRmStateNotRunning'));
   }.property('isRootQSelected', 'isSelectedQueueRmStateNotRunning', 'isRmOffline'),

   configNote: Ember.computed.alias('store.configNote'),
   saveMode: '',

   isSaveConfigDialogOpen: false,
   isConfirmDialogOpen: false,

   saveAndUpdateQueueSuccess: function(newQ) {
     var parentPath = newQ.get('parentPath'),
     parentQ = this.store.getById('queue', parentPath.toLowerCase()),
     pQueues = parentQ.get('queues') ? parentQ.get('queues').split(",") : [];
     pQueues.addObject(newQ.get('name'));
     pQueues.sort();
     parentQ.set('queues', pQueues.join(","));
     this.set('newQueue', null);
   }
});
