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


moduleForModel('job', 'App.Job',{
  needs:['model:file']
});

test('file relationship', function() {
  expect(2);
  var Job = this.store().modelFor('job'),
      relationship = Ember.get(Job, 'relationshipsByName').get('pigScript');

  equal(relationship.kind, 'belongsTo');
  equal(relationship.type.typeKey, App.File.typeKey);
});

test('duration format',function() {
  expect(3);
  var job = this.subject();

  Em.run(function(){
    job.set('duration', 60);
  });

  equal(job.get('durationTime'),"1 min, 0 sec");

  Em.run(function(){
    job.set('duration', 60*60);
  });

  equal(job.get('durationTime'),"1 hrs, 0 min, 0 sec");

  Em.run(function(){
    job.set('duration', 60*60*24);
  });

  equal(job.get('durationTime'),"24 hrs, 0 min, 0 sec");
});

test('percentStatus',function() {
  expect(2);
  var job = this.subject();

  Em.run(function(){
    job.setProperties({'percentComplete': 50,'status':'RUNNING'});
  });

  equal(job.get('percentStatus'),50);

  Em.run(function(){
    job.set('status','FAILED');
  });

  equal(job.get('percentStatus'),100);

});

test('setting argumets array property',function () {
  var job = this.subject(),
      args = job.get('argumentsArray');

  ok(args.length === 0 && !job.get('templetonArguments'));

  Em.run(function(){
    job.set('argumentsArray',args.pushObject('test_agr') && args);
  });

  ok(job.get('argumentsArray').length == 1 && job.get('templetonArguments') == 'test_agr')
});

