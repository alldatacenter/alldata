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


moduleForModel('script', 'App.Script',{
  needs:['model:file']
});

test('setting argumets array property',function () {
  var script = this.subject(),
      args = script.get('argumentsArray');

  ok(args.length === 0 && !script.get('templetonArguments'));

  Em.run(function(){
    script.set('argumentsArray',args.pushObject('test_agr') && args);
  });

  ok(script.get('argumentsArray').length == 1 && script.get('templetonArguments') == 'test_agr')
});

test('file relationship', function() {
  var Script = this.store().modelFor('script'),
      relationship = Ember.get(Script, 'relationshipsByName').get('pigScript');

  equal(relationship.kind, 'belongsTo');
  equal(relationship.type.typeKey, App.File.typeKey);
});

test('blank title test',function () {
  expect(4);
  var script = this.subject();

  Em.run(function(){
    script.set('title','');
  });

  ok(script.get('isBlankTitle'),'script with empty string title is falsy');

  Em.run(function(){
    script.set('title',' ');
  });

  ok(script.get('isBlankTitle'),'script with whitespase title is falsy');

  Em.run(function(){
    script.set('title','\t');
  });

  ok(script.get('isBlankTitle'),'script with tabulation title is falsy');

  Em.run(function(){
    script.set('title','test_title');
  });

  ok(!script.get('isBlankTitle'),'script with non-whitespace title is truthy');
});
