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

function _fetchTagged(adapter, store, type, sinceToken) {
  var promise = adapter.findAllTagged(store, type, sinceToken),
      serializer = store.serializerFor('queue'),
      label = "DS: Handle Adapter#findAllTagged of " + type;


  return Em.RSVP.Promise.cast(promise, label).then(function(adapterPayload) {
    var config = serializer.normalizePayload(adapterPayload.items[0].properties);
    var v = (store.get('current_tag') !== store.get('tag'))?adapterPayload.items[0].version:'';

    store.set('tag',store.get('current_tag'));

    if (Em.isEmpty(config) || !Em.isArray(config.queue)) {
      return;
    }

    store.all('queue').filterBy('isNewQueue',true).forEach(function (q) {
      q.store.get('nodeLabels').forEach(function (nl) {
        var label = q.store.getById('label',[q.get('id'),nl].join('.'));
        if (label) {
          label.unloadRecord();
        }
      });
      q.unloadRecord();
    });

    store.findAll('queue').then(function (queues) {
      store.set('deletedQueues',[]);
      queues.forEach(function  (queue) {
        var new_version = config.queue.findBy('id',queue.id);
        if (new_version) {
          new_version['isNewQueue'] = queue.get('isNewQueue');
          store.findByIds('label',new_version.labels).then(function(labels) {
            labels.forEach(function (label){
              var props = config.label.findBy('id',label.get('id'));
              label.eachAttribute(function (attr,meta) {
                this.set(attr, props[attr]);
              },label);
            });
            queue.get('labels').clear().pushObjects(labels);
            queue.set('version',v);
          });
          queue.eachAttribute(function (attr,meta) {
            if (meta.type === 'boolean') {
              this.set(attr, (new_version[attr] === 'false' || !new_version[attr])?false:true);
            } else {
              this.set(attr, new_version[attr]);
            }
          },queue);
        } else {
          if (Em.isEmpty(store.get('deletedQueues').findBy('path',queue.get('path')))) {
            store.recurceRemoveQueue(queue);
          }
        }
        config.queue.removeObject(new_version);
      });

      config.label.forEach(function (label) {
        if (!store.hasRecordForId('label',label.id)) {
          store.push('label',label);
        }
      });

      config.queue.setEach('isNewQueue',true);
      store.pushMany(type,config.queue);
      config.queue.forEach(function(item) {
        store.recordForId('queue',item.id).set('version',v);
      });
      store.didUpdateAll(type);
      return store.recordForId('scheduler','scheduler');
    }).then(function (scheduler) {
      var props = config.scheduler.objectAt(0);

      scheduler.eachAttribute(function (attr,meta) {
        if (meta.type === 'boolean') {
          this.set(attr, (props[attr] === 'false' || !props[attr])?false:true);
        } else {
          this.set(attr, props[attr]);
        }
      },scheduler);

      scheduler.set('version',v);
    });

  }, null, "DS: Extract payload of findAll " + type);
}

function _fillRmQueueStateIntoQueues(data, store) {
  var parsed = JSON.parse(data),
  queuesNeedRefresh = store.get('queuesNeedRefresh'),
  rootQInfo = parsed['scheduler']['schedulerInfo'];
  var queueStates = _getRmQueueStates(rootQInfo, 'root', []);
  store.all('queue').forEach(function(queue) {
    var qInfo = queueStates.findBy('path', queue.get('path'));
    if (qInfo && qInfo.state) {
      queue.set('rmQueueState', qInfo.state);
      if (queuesNeedRefresh.findBy('path', queue.get('path'))) {
        queuesNeedRefresh.removeObject(queuesNeedRefresh.findBy('path', queue.get('path')));
      }
    } else {
      if (!queuesNeedRefresh.findBy('path', queue.get('path'))) {
        queuesNeedRefresh.addObject({
          path: queue.get('path'),
          name: queue.get('name')
        });
      }
    }
  });
}

function _getRmQueueStates(queueInfo, qPath, qStates) {
  var qObj = {
    name: queueInfo.queueName,
    path: qPath.toLowerCase(),
    state: queueInfo.state || 'RUNNING'
  };
  qStates.addObject(qObj);
  if (queueInfo.queues) {
    var children = queueInfo.queues.queue || [];
    children.forEach(function(child) {
      var cPath = [qPath, child.queueName].join('.');
      return _getRmQueueStates(child, cPath, qStates);
    });
  }
  return qStates;
}

App.ApplicationStore = DS.Store.extend({

  adapter: App.QueueAdapter,

  configNote:'',

  clusterName: '',

  tag: '',

  current_tag: '',

  stackId: '',

  isPreemptionSupported: function() {
    var stackId = this.get('stackId'),
    stackVersion = stackId.substr(stackId.indexOf('-') + 1);
    if (stackVersion >= 2.3) {
      return true;
    }
    return false;
  }.property('stackId'),

  isPriorityUtilizationSupported: function() {
    var stackId = this.get('stackId');
    var stackVersion = stackId.substr(stackId.indexOf('-') + 1);
    if (stackVersion >= 2.6) {
      return true;
    }
    return false;
  }.property('stackId'),

  hasDeletedQueues:Em.computed.notEmpty('deletedQueues.[]'),

  deletedQueues:[],

  queuesNeedRefresh: [],

  lastSavedConfigXML: '',

  setLastSavedConfigXML: function() {
    this.set('lastSavedConfigXML', this.buildConfig('xml'));
  },

  buildConfig: function (fmt) {
    var records = [],
        config = '',
        props,
        serializer = this.serializerFor('queue');

    records.pushObjects(this.all('scheduler').toArray());
    records.pushObjects(this.all('queue').toArray());
    props = serializer.serializeConfig(records).properties;

    if (fmt === 'txt') {
      Object.keys(props).forEach(function (propKey) {
        config += propKey + '=' + props[propKey] + '\n';
      });
    } else if (fmt === 'xml') {
      config += '<?xml version="1.0"?>\n<configuration>\n';
      Object.keys(props).forEach(function (propKey) {
        config += '  <property>\n' +
        '    <name>' + propKey + '</name>\n' +
        '    <value>' + props[propKey] + '</value>\n' +
        '  </property>\n';
      });
      config += "</configuration>"
    }

    return config;
  },

  recurceRemoveQueue: function (queue) {
    if (Em.isEmpty(queue)) {
      return;
    } else {
      queue.get('queuesArray').forEach(function (queueName) {
        this.recurceRemoveQueue(this.getById('queue',[queue.get('path'),queueName].join('.').toLowerCase()));
      }.bind(this));

      if (!queue.get('isNewQueue')){
        this.get('deletedQueues').pushObject(this.buildDeletedQueue(queue));
      }

      if (this.get('queuesNeedRefresh').findBy('path', queue.get('path'))) {
        this.get('queuesNeedRefresh').removeObject(this.get('queuesNeedRefresh').findBy('path', queue.get('path')));
      }
    }
    this.all('queue').findBy('path',queue.get('parentPath')).set('queuesArray',{'exclude':queue.get('name')});
    return queue.destroyRecord();

  },

  buildDeletedQueue: function (queue) {
    var deletedQueue = queue.serialize({clone:true});
    delete deletedQueue.id;

    Em.merge(deletedQueue,{
      'isDeletedQueue': true,
      'changedAttributes': queue.changedAttributes(),
      'isLabelsDirty': queue.get('isLabelsDirty'),
      'labelsEnabled': queue.labelsEnabled
    });

    deletedQueue.changedAttributes.labels = [queue.get('initialLabels').sort(),queue.get('labels').mapBy('id').sort()];

    return Em.Object.create(deletedQueue);
  },

  saveAndUpdateQueue: function(record, updates){
    return record.save().then(function (queue) {
      if (updates) {
        queue.eachAttribute(function (attr,meta) {
          if (updates.changedAttributes.hasOwnProperty(attr)) {
            this.set(attr, updates.changedAttributes[attr].objectAt(1));
          }
        },queue);

        queue.eachRelationship(function (relationship,meta) {
          queue.get(relationship).clear();
          if (updates.changedAttributes.hasOwnProperty(relationship)) {
            updates.changedAttributes[relationship][1].forEach(function (label) {
              queue.get('labels').pushObject(queue.store.recordForId('label', [queue.get('path'),label.split('.').get('lastObject')].join('.')));
            });
          } else {
            this.set(relationship, updates.get(relationship));
          }

          queue.notifyPropertyChange('labels');
        },queue);

      }
      return queue;
    }.bind(this));
  },

  createFromDeleted: function (deletedQueue) {
    var newQueue = this.createRecord('queue', {
      id: [deletedQueue.parentPath,deletedQueue.name].join('.').toLowerCase(),
      name: deletedQueue.name,
      parentPath: deletedQueue.parentPath,
      depth: deletedQueue.parentPath.split('.').length
    });

    this.get('deletedQueues').removeObject(deletedQueue);

    newQueue.eachAttribute(function (attr,meta) {
      if (deletedQueue.changedAttributes.hasOwnProperty(attr)) {
        this.set(attr, deletedQueue.changedAttributes[attr].objectAt(0));
      } else {
        this.set(attr, deletedQueue.get(attr));
      }
    },newQueue);

    newQueue.eachRelationship(function (relationship,meta) {
      if (deletedQueue.changedAttributes.hasOwnProperty(relationship)) {
        deletedQueue.changedAttributes[relationship][0].forEach(function (label) {
          newQueue.get('labels').pushObject(newQueue.store.recordForId('label', label));
        });
      } else {
        this.set(relationship, deletedQueue.get(relationship));
      }
    },newQueue);

    newQueue.notifyPropertyChange('labels');

    newQueue.set('labelsEnabled',deletedQueue.labelsEnabled);

    return newQueue;
  },

  copyFromDeleted: function (deletedQueue, parent, name) {
    var newQueue = this.createRecord('queue', {
      id: [parent,name].join('.').toLowerCase(),
      name: name,
      path: [parent,name].join('.'),
      parentPath: parent,
      depth: parent.split('.').length
    });

    newQueue.eachAttribute(function (attr,meta) {
      if (!newQueue.changedAttributes().hasOwnProperty(attr)) {
        if (deletedQueue.changedAttributes.hasOwnProperty(attr)) {
          this.set(attr, deletedQueue.changedAttributes[attr].objectAt(0));
        } else {
          this.set(attr, deletedQueue[attr]);
        }
      }
    },newQueue);

    newQueue.set('isNewQueue',true);

    newQueue.eachRelationship(function (relationship,meta) {
      if (deletedQueue.changedAttributes.hasOwnProperty(relationship)) {
        deletedQueue.changedAttributes[relationship][0].forEach(function (label) {
          newQueue.get('labels').pushObject(newQueue.store.recordForId('label', [newQueue.get('path'),label.split('.').get('lastObject')].join('.')));
        });
      } else {
        this.set(relationship, deletedQueue.get(relationship));
      }
    },newQueue);

    newQueue.notifyPropertyChange('labels');

    newQueue.set('labelsEnabled',deletedQueue.labelsEnabled);

    return newQueue;
  },

  nodeLabels: function () {
    var adapter = this.get('defaultAdapter'),
        store = this,
        promise = new Ember.RSVP.Promise(function(resolve, reject) {
          adapter.getNodeLabels(store).then(function(data) {
            store.set('isRmOffline',false);
            resolve(data);
          }, function() {
            store.set('isRmOffline',true);
            resolve([]);
          });
        });

    return Ember.ArrayProxy.extend(Ember.PromiseProxyMixin).create({
      promise: promise
    });
  }.property(),

  isRmOffline:false,

  isNodeLabelsEnabledByRM: false,

  isNodeLabelsConfiguredByRM: false,

  isInitialized: Ember.computed.and('tag', 'clusterName'),

  relaunchCapSched: function (opt) {
    return this.get('defaultAdapter').relaunchCapSched(opt);
  },

  flushPendingSave: function() {
    var pending = this._pendingSave.slice(),
        newPending = [[]],
        notLabel = false,
        isDeleteOperation = false;


    if (pending.length == 1 || pending.isEvery('firstObject.isNew',true)) {
      this._super();
      return;
    }

    pending.forEach(function (tuple) {
      var record = tuple[0], resolver = tuple[1];

      newPending[0].push(record);
      //resolve previous resolver to fire susscess callback
      if (record.constructor.typeKey !== 'label') {
        if (newPending[1]) {
          newPending[1].resolve(true);
        }
        newPending[1] = resolver;
        notLabel = true;
      } else {
        resolver.resolve(true);
      }
      if (record.get('isDeleted')) {
        isDeleteOperation = true;
      }
    });
    if (notLabel && !isDeleteOperation) {
      this._pendingSave = [newPending];
    }
    this._super();
  },
  didSaveRecord: function(record, data) {
    if (Em.isArray(record)) {
      for (var i = 0; i < record.length; i++) {
        this._super(record[i],data.findBy('id',record[i].id));
      }
    } else {
      this._super(record, data);
    }
  },
  recordWasError: function(record) {
    if (Em.isArray(record)) {
      for (var i = 0; i < record.length; i++) {
        record[i].adapterDidError();
      }
    } else {
      record.adapterDidError();
    }
  },
  fetchTagged: function(type, tag) {
    var adapter = this.adapterFor(type),
        sinceToken = this.typeMapFor(type).metadata.since;

    this.set('tag',tag);

    return _fetchTagged(adapter, this, type, sinceToken);
  },
  checkOperator:function () {
    return this.get('defaultAdapter').getPrivilege();
  },
  checkCluster:function () {
    return this.get('defaultAdapter').checkCluster(this);
  },
  getRmSchedulerConfigInfo: function() {
    var store = this,
    adapter = this.get('defaultAdapter');
    return new Ember.RSVP.Promise(function(resolve, reject) {
      adapter.getRmSchedulerConfigInfo().then(function(data) {
        if (data) {
          _fillRmQueueStateIntoQueues(data, store);
        }
        store.set('isRmOffline', false);
        resolve([]);
      }, function() {
        store.set('isRmOffline', true);
        resolve([]);
      }).finally(function() {
        store.pollRmSchedulerConfigInfo();
      });
    });
  },
  pollRmSchedulerConfigInfo: function() {
    var store = this;
    //Poll getRmSchedulerConfigInfo every 1 minute.
    Ember.run.later(store, function() {
      store.getRmSchedulerConfigInfo();
    }, 60000);
  }
});
