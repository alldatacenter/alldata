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

moduleFor('controller:queue', 'App.QueueController',
  {
  needs:[ 'controller:queues' ],
    teardown:function (container) {
      App.reset();
    }
  },
  function (container) {
    container.register('model:queue', App.Queue);
    container.register('model:label', App.Label);
    container.register('store:main', App.ApplicationStore);
    container.register('adapter:application', DS.FixtureAdapter);
    container.register('serializer:application', DS.RESTSerializer);
  }
);

test('can get parentQueue',function () {
  var controller = this.subject(),
      store = this.container.lookup('store:main'),
      queues;


  Em.run(function() {
    queues = store.pushMany('queue',[
      {id:'root.a',parentPath:'root',path:'root.A'},
      {id:'root.a.b',parentPath:'root.a',path:'root.A.B'}
    ]);
    controller.set('store', store );
    controller.set('model', queues.objectAt(1) );
  });

  deepEqual(controller.get('parentQueue'),queues.objectAt(0));
});