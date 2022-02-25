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

var run = Ember.run;

var setupStore = function(options) {
    var env = {};
    options = options || {};

    var container = env.container = new Ember.Container();

    var adapter = env.adapter = (options.adapter || DS.Adapter);
    delete options.adapter;

    for (var prop in options) {
      container.register('model:' + prop, options[prop]);
    }

    container.register('store:main', App.ApplicationStore.extend({
      adapter: adapter
    }));

    container.register('serializer:queue', App.QueueSerializer);
    container.register('serializer:scheduler', App.SchedulerSerializer);
    container.register('serializer:label', App.SchedulerSerializer);
    container.register('serializer:tag', App.TagSerializer);

    container.injection('serializer', 'store', 'store:main');

    env.queueSerializer = container.lookup('serializer:queue');
    env.schedulerSerializer = container.lookup('serializer:scheduler');
    env.labelSerializer = container.lookup('serializer:label');
    env.tagSerializer = container.lookup('serializer:tag');

    env.store = container.lookup('store:main');
    env.adapter = env.store.get('defaultAdapter');

    return env;
};

var createStore = function(options) {
    return setupStore(options).store;
};

QUnit.module("integration/serializers", {
  setup: function (options) {
    store = createStore({queue: App.Queue,label:App.Label,adapter:App.QueueAdapter});
  },
  teardown: function() {
    store = null;
  }
});

test('serialize accessible-node-labels property', function () {

  expect(4);

  var queueSerializer = store.serializerFor('queue'),
      rootQueue,
      storeLabels,
      label1,
      label2,
      json;

  run(function () {
    store.set('nodeLabels',[{name:'label1'}, {name:'label2'}]);
  });

  run(function () {
    label1 = store.push('label', {'id':'root.label1','name':'label1'});
    label2 = store.push('label', {'id':'root.label2','name':'label2'});
    rootQueue = store.push('queue', {
      'id':'root',
      'path':'root',
      'labelsEnabled':true,
      '_accessAllLabels':true,
      'labels':['root.label1', 'root.label2']
    });
  });

  json = queueSerializer.serialize(rootQueue);

  equal(json['yarn.scheduler.capacity.root.accessible-node-labels'],'*');

  run(function () {
    rootQueue.set('_accessAllLabels',false);
  });

  json = queueSerializer.serialize(rootQueue);

  equal(json['yarn.scheduler.capacity.root.accessible-node-labels'],'label1,label2');

  run(function () {
    rootQueue.get('labels').clear();
  });

  json = queueSerializer.serialize(rootQueue);

  equal(json['yarn.scheduler.capacity.root.accessible-node-labels'],'');

  run(function () {
    rootQueue.set('labelsEnabled',false);
  });

  json = queueSerializer.serialize(rootQueue);

  equal(json['yarn.scheduler.capacity.root.accessible-node-labels'],undefined);

});