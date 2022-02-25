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

App.CapschedQueuesconfEditqueueController = Ember.Controller.extend({
  needs: ['capsched', 'capschedQueuesconf'],
  isOperator: Ember.computed.alias('controllers.capsched.isOperator'),
  isNotOperator: Ember.computed.not('isOperator'),
  scheduler: Ember.computed.alias('controllers.capsched.content'),
  allQueues: Ember.computed.alias('controllers.capsched.queues'),
  isNodeLabelsEnabledByRM: Ember.computed.alias('store.isNodeLabelsEnabledByRM'),
  isNodeLabelsConfiguredByRM: Ember.computed.alias('store.isNodeLabelsConfiguredByRM'),
  isRmOffline: Ember.computed.alias('store.isRmOffline'),
  queuesNeedRefresh: Ember.computed.alias('store.queuesNeedRefresh'),
  precision: 2,
  isNodeLabelsEnabledAndConfiguredByRM: Ember.computed.and('isNodeLabelsEnabledByRM', 'isNodeLabelsConfiguredByRM'),
  isRefreshOrRestartNeeded: Ember.computed.alias('controllers.capschedQueuesconf.isRefreshOrRestartNeeded'),

  isRangerEnabledForYarn: function() {
    var isRanger = this.get('controllers.capsched.isRangerEnabledForYarn');
    if (isRanger == null || typeof isRanger == 'undefined') {
      return false;
    }
    isRanger = isRanger.toLowerCase();
    if (isRanger == 'yes' || isRanger == 'true') {
      return true;
    }
    return false;
  }.property('controllers.capsched.isRangerEnabledForYarn'),

  isPreemptionEnabledByYarn: function() {
    var isEnabled = this.get('controllers.capsched.isPreemptionEnabledByYarn');
    if (isEnabled === 'true') {
      return true;
    }
    return false;
  }.property('controllers.capsched.isPreemptionEnabledByYarn'),

  actions: {
    toggleProperty: function (property, target) {
      target = target || this;
      target.toggleProperty(property);
    },
    mouseUp: function(){
      return false;
    },
    editQueueName: function() {
      this.set('enableEditQName', true);
      this.set('updatedQName', this.get('content.name'));
      this.set('controllers.capschedQueuesconf.isEditingQueueName', true);
    },
    cancelQNameEdit: function() {
      this.set('enableEditQName', false);
      this.set('isInvalidQName', false);
      this.set('invalidQNameMessage', '');
      this.set('controllers.capschedQueuesconf.isEditingQueueName', false);
    },
    renameQueue: function() {
      if (this.validateQName()) {
        return;
      }
      this.set('controllers.capschedQueuesconf.isEditingQueueName', false);
      this.set('content.name', this.get('updatedQName'));
      this.set('enableEditQName', false);
    },
    rollbackPreemptionProps: function() {
      this.send('rollbackProp', 'disable_preemption', this.get('content'));
      this.send('rollbackProp', 'isPreemptionInherited', this.get('content'));
      this.set('content.isPreemptionOverriden', false);
    },
    rollbackProp: function(prop, item) {
      var tempRefreshNeeded = this.get('isRefreshOrRestartNeeded');
      var attributes = item.changedAttributes();
      if (attributes.hasOwnProperty(prop)) {
        item.set(prop, attributes[prop][0]);
      }
      this.set('isRefreshOrRestartNeeded', tempRefreshNeeded);
      this.afterRollbackProp();
    }
  },

  afterRollbackProp: function() {
    var isAnyQDirty = this.get('allQueues').isAny('isAnyDirty', true);
    if (isAnyQDirty) {
      this.set('isRefreshOrRestartNeeded', true);
    } else {
      this.set('isRefreshOrRestartNeeded', false);
    }
  },

  updatedQName: '',
  enableEditQName: false,
  isInvalidQName: false,
  invalidQNameMessage: '',

  validateQName: function() {
    var qName = this.get('updatedQName'),
    originalQName = this.get('content.name'),
    qParentPath = this.get('content.parentPath'),
    qPath = [qParentPath, qName].join('.'),
    qAlreadyExists = (this.get('allQueues').findBy('name', qName))? true : false;
    if (Ember.isBlank(qName)) {
      this.set('isInvalidQName', true);
      this.set('invalidQNameMessage', 'Enter queue name');
    } else if (qAlreadyExists && qName !== originalQName) {
      this.set('isInvalidQName', true);
      this.set('invalidQNameMessage', 'Queue already exists');
    } else if (qName.indexOf(' ') > -1) {
      this.set('isInvalidQName', true);
      this.set('invalidQNameMessage', 'Queue name contains white spaces');
    } else {
      this.set('isInvalidQName', false);
      this.set('invalidQNameMessage', '');
    }
    return this.get('isInvalidQName');
  },

  qNameDidChage: function() {
    this.validateQName();
  }.observes('updatedQName', 'updatedQName.length'),

  absoluteClusterCapacity: function() {
    return this.get('content.absolute_capacity');
  }.property('content', 'content.absolute_capacity'),

  absoluteClusterBarWidth: function() {
    var absCap = this.get('absoluteClusterCapacity');
    return this.get('widthPattern').fmt(absCap);
  }.property('absoluteClusterCapacity'),

  /**
   * Collection of modified fields in queue.
   * @type {Object} - { [fileldName] : {Boolean} }
   */
  queueDirtyFields: {},

  isQueueDirty: Ember.computed.bool('content.isDirty'),

  /**
   * Possible values for ordering policy
   * @type {Array}
   */
  orderingPolicyValues: [
    {label: 'FIFO', value: 'fifo'},
    {label: 'Fair', value: 'fair'}
  ],

  /**
   * Returns true if queue is root.
   * @type {Boolean}
   */
   isRoot: Ember.computed.match('content.id', /^(root)$/),

   /**
    * Returns true if queue is default.
    * @type {Boolean}
    */
   isDefaultQ: Ember.computed.match('content.id', /^(root.default)$/),

   /**
    * Represents queue run state. Returns true if state is null.
    * @return {Boolean}
    */
   isRunning: function() {
     return this.get('content.state') == _runState || this.get('content.state') == null;
   }.property('content.state'),

   /**
    * Current ordering policy value of queue.
    * @param  {String} key
    * @param  {String} value
    * @return {String}
    */
   currentOP: function (key, val) {
     if (arguments.length > 1) {
       if (!this.get('isFairOP')) {
         this.send('rollbackProp', 'enable_size_based_weight', this.get('content'));
       }
       this.set('content.ordering_policy', val || null);
     }
     return this.get('content.ordering_policy') || 'fifo';
   }.property('content.ordering_policy'),

   /**
    * Does ordering policy is equal to 'fair'
    * @type {Boolean}
    */
   isFairOP: Ember.computed.equal('content.ordering_policy', 'fair'),

   /**
    * Returns maximum applications for a queue if defined,
    * else the inherited value
    */
   maximumApplications: function(key, val) {
    if (arguments.length > 1) {
      this.set('content.maximum_applications', val);
    }
    var queueMaxApps = this.get('content.maximum_applications');
    if (queueMaxApps) {
      return queueMaxApps;
    } else {
      return this.getInheritedMaximumApplications();
    }
   }.property('content.maximum_applications', 'scheduler.maximum_applications'),

   isMaximumApplicationsInherited: function() {
     if (this.get('content.maximum_applications')) {
       return false;
     } else {
       return true;
     }
   }.property('content.maximum_applications'),

   /**
    * Returns inherited maximum applications for a queue
    */
   getInheritedMaximumApplications: function() {
     var parentQ = this.store.getById('queue', this.get('content.parentPath').toLowerCase());
     while (parentQ !== null) {
       if (parentQ.get('maximum_applications')) {
         return parentQ.get('maximum_applications');
       }
       parentQ = this.store.getById('queue', parentQ.get('parentPath').toLowerCase());
     }
     return this.get('scheduler.maximum_applications');
   },

   /**
    * Returns maximum AM resource percent for a queue if defined,
    * else the inherited value (for all queues)
    */
   maximumAMResourcePercent: function(key, val) {
     if (arguments.length > 1) {
       this.set('content.maximum_am_resource_percent', val);
     }
     var qMaxAmPercent = this.get('content.maximum_am_resource_percent');
     if (qMaxAmPercent) {
       return qMaxAmPercent;
     } else {
       return this.getInheritedMaxAmResourcePercent();
     }
   }.property('content.maximum_am_resource_percent', 'scheduler.maximum_am_resource_percent'),

   isMaxAmResourcePercentInherited: function() {
     if (this.get('content.maximum_am_resource_percent')) {
       return false;
     } else {
       return true;
     }
   }.property('content.maximum_am_resource_percent'),

   /**
    * Returns inherited maximum am resource percent for a queue
    */
   getInheritedMaxAmResourcePercent: function() {
     var parentQ = this.store.getById('queue', this.get('content.parentPath').toLowerCase());
     while (parentQ !== null) {
       if (parentQ.get('maximum_am_resource_percent')) {
         return parentQ.get('maximum_am_resource_percent');
       }
       parentQ = this.store.getById('queue', parentQ.get('parentPath').toLowerCase());
     }
     return this.get('scheduler.maximum_am_resource_percent');
   },

   isAnyQueueResourcesDirty: function() {
     var changedAttrs = this.get('content').changedAttributes();
     return changedAttrs.hasOwnProperty('user_limit_factor') || changedAttrs.hasOwnProperty('minimum_user_limit_percent')
      || changedAttrs.hasOwnProperty('maximum_applications') || changedAttrs.hasOwnProperty('maximum_am_resource_percent')
      || changedAttrs.hasOwnProperty('ordering_policy') || changedAttrs.hasOwnProperty('enable_size_based_weight');
   }.property(
     'content.user_limit_factor',
     'content.minimum_user_limit_percent',
     'content.maximum_applications',
     'content.maximum_am_resource_percent',
     'content.ordering_policy',
     'content.enable_size_based_weight'
   ),

   /**
    * Sets ACL value to '*' or ' ' and returns '*' and 'custom' respectively.
    * @param  {String} key   - ACL attribute
    * @param  {String} value - ACL value
    * @return {String}
    */
   handleAcl: function (key, value) {
     if (value) {
       this.set(key, (value === '*')? '*' : ' ');
     }
     return (this.get(key) === '*' || this.get(key) == null) ? '*' : 'custom';
   },

   /**
    * Queue's acl_administer_queue property can be set to '*' (everyone) or ' ' (nobody) thru this property.
    *
    * @param  {String} key
    * @param  {String} value
    * @return {String} - '*' if equal to '*' or 'custom' in other case.
    */
   acl_administer_queue: function (key, value) {
     return this.handleAcl('content.acl_administer_queue', value);
   }.property('content.acl_administer_queue'),

   /**
    * Returns true if acl_administer_queue is set to '*'
    * @type {Boolean}
    */
   aaq_anyone: Ember.computed.equal('acl_administer_queue', '*'),

   /**
    * Returns effective permission of the current queue to perform administrative functions on this queue.
    */
    aaq_effective_permission: function(key, value){
      return this.getEffectivePermission('acl_administer_queue');
    }.property('content.acl_administer_queue'),

    /**
     * Queue's acl_submit_applications property can be set to '*' (everyone) or ' ' (nobody) thru this property.
     *
     * @param  {String} key
     * @param  {String} value
     * @return {String} - '*' if equal to '*' or 'custom' in other case.
     */
    acl_submit_applications: function (key, value) {
      return this.handleAcl('content.acl_submit_applications', value);
    }.property('content.acl_submit_applications'),

    /**
     * Returns true if acl_submit_applications is set to '*'
     * @type {Boolean}
     */
    asa_anyone:Ember.computed.equal('acl_submit_applications', '*'),

    /**
     * Returns effective permission of the current queue to submit application.
     */
    asa_effective_permission: function(key, value){
      return this.getEffectivePermission('acl_submit_applications');
    }.property('content.acl_submit_applications'),

    /**
     * Returns effective permission of the current queue.
     */
    getEffectivePermission: function(permissionType){
      var effectivePermission,
      users = [],
      groups = [],
      currentPermissions = this.getPermissions(permissionType);
      for(var i = 0; i < currentPermissions.length; i++){
        var permission = currentPermissions[i];
        if (permission === '*') {
          return '*';
        } else if (permission.trim() === '') {
          effectivePermission = '';
        } else {
          var usersAndGroups = permission.split(' ');
          this.fillUsersAndGroups(users, usersAndGroups[0]);
          if (usersAndGroups.length === 2) {
            this.fillUsersAndGroups(groups, usersAndGroups[1]);
          }
        }
      }
      if(users.length > 0 || groups.length > 0){
        effectivePermission = users.join(',') + ' ' + groups.join(',');
      }
      return effectivePermission;
    },

    /**
     * Removes duplicate users or groups.
     */
    fillUsersAndGroups: function(usersOrGroups, list){
      var splitted = list.split(',');
      splitted.forEach(function(item){
        if(usersOrGroups.indexOf(item) === -1){
          usersOrGroups.push(item);
        }
      });
    },

    /**
     * Returns array of permissions from root to leaf.
     */
    getPermissions: function(permissionType){
      var currentQ = this.get('content'),
      permissions = [];
      while (currentQ !== null) {
        if (currentQ.get(permissionType) !== null) {
          permissions.push(currentQ.get(permissionType));
        } else {
          permissions.push('*');
        }
        currentQ = this.store.getById('queue', currentQ.get('parentPath').toLowerCase());
      }
      permissions.reverse();//root permission at the 0th position.
      return permissions;
    },

    isAnyAccessControlListDirty: function() {
      var chagedAttrs = this.get('content').changedAttributes();
      return chagedAttrs.hasOwnProperty('acl_administer_queue') || chagedAttrs.hasOwnProperty('acl_submit_applications');
    }.property('content.acl_submit_applications', 'content.acl_administer_queue'),

    /**
     * Array of children queues.
     * @return {Array}
     */
    childrenQueues: function () {
      return this.get('allQueues')
        .filterBy('depth', this.get('content.depth') + 1)
        .filterBy('parentPath', this.get('content.path'));
    }.property('allQueues.length', 'content.path', 'content.parentPath'),

    /**
     * Parent of current queue.
     * @return {App.Queue}
     */
    parentQueue: function () {
      return this.store.getById('queue', this.get('content.parentPath').toLowerCase());
    }.property('content.parentPath'),

    /*
     * Returns true if the current queue is a leaf queue
     */
    isLeafQ: function() {
      return this.get('content.queues') == null;
    }.property('allQueues.length', 'content.queues'),

    childrenQueuesTotalCapacity: function() {
      var childrenQs = this.get('childrenQueues'),
      totalCapacity = 0;
      childrenQs.forEach(function(currentQ){
        if (typeof currentQ.get('capacity') === 'number') {
          totalCapacity += currentQ.get('capacity');
        }
      });
      return parseFloat(totalCapacity.toFixed(this.get('precision')));
    }.property('childrenQueues.length', 'childrenQueues.@each.capacity'),

    widthPattern: 'width: %@%',

    warnInvalidCapacity: function() {
      var totalCap = this.get('childrenQueuesTotalCapacity');
      if (totalCap > 100 || totalCap < 100) {
        return true;
      }
      return false;
    }.property('childrenQueuesTotalCapacity'),

    totalCapacityBarWidth: function() {
      var totalCap = this.get('childrenQueuesTotalCapacity');
      if (totalCap > 100) {
        totalCap = 100;
      }
      return this.get('widthPattern').fmt(totalCap);
    }.property('childrenQueuesTotalCapacity'),

    isAnyChildrenQueueCapacityDirty: function() {
      if (this.get('isLeafQ')) {
        return false;
      }
      return this.get('queueDirtyFields.queues') || this.get('childrenQueues').anyBy('isDirtyCapacity', true)
        || this.get('childrenQueues').anyBy('isDirtyMaxCapacity', true) || this.get('warnInvalidCapacity');
    }.property('content', 'content.queues', 'childrenQueues.@each.isDirtyCapacity', 'childrenQueues.@each.isDirtyMaxCapacity', 'warnInvalidCapacity'),

   /**
    * Adds observers for each queue attribute.
    * @method dirtyObserver
    */
   dirtyObserver: function () {
     this.get('content.constructor.transformedAttributes.keys.list').forEach(function(item) {
       this.addObserver('content.' + item, this, 'propertyBecomeDirty');
     }.bind(this));
   }.observes('content'),

   /**
    * Adds modified queue fileds to q queueDirtyFields collection.
    * @param  {String} controller
    * @param  {String} property
    * @method propertyBecomeDirty
    */
   propertyBecomeDirty: function (controller, property) {
     var queueProp = property.split('.').objectAt(1);
     this.set('queueDirtyFields.' + queueProp, this.get('content').changedAttributes().hasOwnProperty(queueProp));
   },

   //Node Labels
   allNodeLabels: Ember.computed.alias('store.nodeLabels.content'),
   queueNodeLabels: Ember.computed.alias('content.sortedLabels'),
   hasQueueLabels: Ember.computed.gt('content.sortedLabels.length', 0),
   nonAccessibleLabels: Ember.computed.alias('content.nonAccessibleLabels'),
   allLabelsForQueue: Ember.computed.union('queueNodeLabels', 'nonAccessibleLabels'),
   sortLabelsBy: ['name'],
   sortedAllLabelsForQueue: Ember.computed.sort('allLabelsForQueue', 'sortLabelsBy'),
   hasAnyNodeLabelsForQueue: Ember.computed.gt('allLabelsForQueue.length', 0),

   accessibleLabelNames: function() {
     var labels = this.get('queueNodeLabels'),
     len = this.get('queueNodeLabels.length'),
     labelNames = ['None'];
     if (len > 0) {
       labelNames = labels.map(function(label){
         return label.get('name');
       });
     }
     return labelNames.join(", ");
   }.property('content', 'queueNodeLabels.[]'),

   childrenQueueLabels: function() {
     var childrenQs = this.get('childrenQueues'),
     allNodeLabels = this.get('allNodeLabels'),
     chidrenQLabels = [],
     ctrl = this;
     allNodeLabels.forEach(function(label) {
       var labelName = label.name,
       labelGroup = {
         labelName: labelName,
         childrenQueueLabels: []
       };
       childrenQs.forEach(function(queue) {
         var qLabel = ctrl.store.getById('label', [queue.get('path'), labelName].join('.')),
         parentQ = ctrl.store.getById('queue', queue.get('parentPath').toLowerCase());
         var cql = {
           label: qLabel,
           queue: queue,
           parentQueue: parentQ
         };
         labelGroup.childrenQueueLabels.pushObject(cql);
       });
       if (labelGroup.childrenQueueLabels.length > 0) {
         chidrenQLabels.pushObject(labelGroup);
       }
     });
     return chidrenQLabels;
   }.property('childrenQueues.length', 'childrenQueues.@each.labels.[]', 'content.labels.length'),

   hasChildrenQueueLabels: Ember.computed.gt('childrenQueueLabels.length', 0),

   //Default Node Label
   initDefaultLabelOptions: function() {
     var optionsObj = [{label: 'None', value: null}],
     labels = this.get('queueNodeLabels'),
     len = this.get('queueNodeLabels.length');
     if (len > 0) {
       labels.forEach(function(lb){
         var obj = {label: lb.get('name'), value: lb.get('name')};
         optionsObj.pushObject(obj);
       });
     }
     this.set('defaultNodeLabelOptions', optionsObj);
   }.observes('queueNodeLabels.[]').on('init'),

   defaultNodeLabelOptions: [],

   isDefaultNodeLabelInherited: function() {
     if (this.get('content.default_node_label_expression') !== null) {
       return false;
     } else {
       return true;
     }
   }.property('content.default_node_label_expression'),

   queueDefaultNodeLabelExpression: function(key, value) {
     if (arguments.length > 1) {
       if (value !== null) {
         this.set('content.default_node_label_expression', value);
       } else {
         this.set('content.default_node_label_expression', null);
       }
     }
     if (this.get('content.default_node_label_expression') !== null) {
       return this.get('content.default_node_label_expression');
     } else {
       return this.getDefaultNodeLabelExpressionInherited();
     }
   }.property('content.default_node_label_expression'),

   getDefaultNodeLabelExpressionInherited: function() {
     var store = this.get('store'),
     currentQ = this.get('content'),
     parentQ = store.getById('queue', currentQ.get('parentPath').toLowerCase()),
     dnlexpr = null;
     while (parentQ !== null) {
       if (parentQ.get('default_node_label_expression') !== null) {
         return parentQ.get('default_node_label_expression');
       }
       parentQ = store.getById('queue', parentQ.get('parentPath').toLowerCase());
     }
     return dnlexpr;
   },

   selectedDefaultNodeLabel: function() {
     var labels = this.get('queueNodeLabels');
     var dnle = this.get('content.default_node_label_expression') || this.getDefaultNodeLabelExpressionInherited();
     if (dnle !== null && labels.findBy('name', dnle)) {
       return dnle;
     }
     return 'None';
   }.property('content.default_node_label_expression'),

   isValidDefaultNodeLabel: function() {
     return this.get('selectedDefaultNodeLabel') !== 'None';
   }.property('selectedDefaultNodeLabel'),

   childrenLabelsForQueue: function() {
     var childrenLabels = [];
     this.get('childrenQueues').forEach(function(child){
       child.get('labels').forEach(function(label){
         childrenLabels.addObject(label);
       });
     });
     return childrenLabels;
   }.property('childrenQueues.length', 'childrenQueues.@each.labels.[]'),

   warnInvalidTotalLabelCapacity: false,

   isAnyChildrenQueueLabelDirty: function() {
     var changedAttrs = this.get('content').changedAttributes();
     if (this.get('content.queues') === null) {
       return changedAttrs.hasOwnProperty('default_node_label_expression');
     }
     return this.get('childrenQueues').anyBy('isLabelsDirty', true) || changedAttrs.hasOwnProperty('default_node_label_expression')
      || this.get('childrenLabelsForQueue').anyBy('isDirtyLabelCapacity', true) || this.get('childrenLabelsForQueue').anyBy('isDirtyLabelMaxCapacity', true)
      || this.get('warnInvalidTotalLabelCapacity');
   }.property('content', 'childrenQueues.@each.isLabelsDirty', 'childrenLabelsForQueue.@each.isDirtyLabelCapacity', 'childrenLabelsForQueue.@each.isDirtyLabelMaxCapacity', 'content.default_node_label_expression', 'warnInvalidTotalLabelCapacity'),

   //Preemption
   isPreemptionSupported: Ember.computed.alias('store.isPreemptionSupported'),
   currentStack: Ember.computed.alias('store.stackId'),

   isPreemptionOverriden: function(key, value) {
     if (arguments.length > 1) {
       this.set('content.isPreemptionOverriden', value);
     }
     return this.get('content.isPreemptionOverriden');
   }.property('content.isPreemptionOverriden'),

   isPreemptionInherited: function() {
     return this.get('content.isPreemptionInherited');
   }.property('content.disable_preemption', 'content.isPreemptionInherited'),

   queueDisablePreemption: function() {
     if (!this.get('isPreemptionInherited')) {
       return (this.get('content.disable_preemption')==='true')?true:false;
     } else {
       return this.getInheritedQueuePreemption();
     }
   }.property('content.disable_preemption', 'content.isPreemptionInherited'),

   getInheritedQueuePreemption: function() {
     var store = this.get('store'),
     currentQ = this.get('content'),
     parentQ = store.getById('queue', currentQ.get('parentPath').toLowerCase()),
     preemption = '';
     while (parentQ !== null) {
       if (!parentQ.get('isPreemptionInherited')) {
         preemption = parentQ.get('disable_preemption');
         return (preemption==='true')?true:false;
       }
       parentQ = store.getById('queue', parentQ.get('parentPath').toLowerCase());
     }
     return preemption;
   },

   isQueuePreemptionDirty: function() {
     return this.get('content').changedAttributes().hasOwnProperty('disable_preemption');
   }.property('content.disable_preemption', 'content.isPreemptionInherited'),

   doOverridePreemption: function(key, value) {
     if (value) {
       this.set('content.isPreemptionInherited', false);
       this.set('content.disable_preemption', (value === 'disable')?'true':'false');
     }
     if (this.get('content.isPreemptionInherited')) {
       return '';
     } else {
       return (this.get('content.disable_preemption')==='true')?'disable':'enable';
     }
   }.property('content.disable_preemption', 'content.isPreemptionInherited'),

   preemptionOverrideWatcher: function() {
     var override = this.get('content.isPreemptionOverriden'),
     wasInheritedInitially = (this.get('content').changedAttributes().hasOwnProperty('isPreemptionInherited')
      && this.get('content').changedAttributes()['isPreemptionInherited'][0] === true);
     if (override === false && wasInheritedInitially) {
       this.set('content.isPreemptionInherited', true);
       this.set('content.disable_preemption', '');
     }
   }.observes('content.isPreemptionOverriden')
});
