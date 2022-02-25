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


moduleFor('controller:pig', 'App.PigController', {
  needs:['controller:scriptEdit','controller:script','model:script'],
  setup:function () {
    var store = this.container.lookup('store:main');
    var _this = this;
    Ember.run(function() {
      store.createRecord('script',{pigScript:store.createRecord('file')});
      _this.subject({
        model: store.all('script')
      })
    });
  }
},function (container) {
  container.register('model:file', App.File);
  container.register('model:script', App.Script);
  container.register('store:main', DS.Store);
  container.register('adapter:application', DS.FixtureAdapter);
  container.register('serializer:application', DS.RESTSerializer);
  container.register('transform:boolean', DS.BooleanTransform);
  container.register('transform:date',    DS.DateTransform);
  container.register('transform:number',  DS.NumberTransform);
  container.register('transform:string',  DS.StringTransform);
  container.register('transform:isodate',  App.IsodateTransform);
});

test('Can get active Script after active Script Id script is set', function () {
  var pig = this.subject();
  var script = pig.get('content.firstObject');

  Ember.run(function() {
    pig.set('activeScriptId', script.get('id'));
    deepEqual(script, pig.get('activeScript'), 'script is set');
  });
});

test('Can\'t save Script while ranaming', function () {
  var pig = this.subject();
  var se = pig.get('controllers.scriptEdit');
  var script = pig.get('content.firstObject');

  Em.run(function () {
    stop();
    script.save().then(function () {
      start();
      se.set('isRenaming',true);
      pig.set('activeScriptId', script.get('id'));
      equal(pig.get('saveEnabled'),false,'save is disabled')
    });
  })
});

test('scriptDirty property test', function () {
  var pig = this.subject();
  var script = pig.get('content.firstObject');

  Em.run(function () {
    stop();
    script.save().then(function () {
      start();
      pig.set('activeScriptId', script.get('id'));
      script.set('templetonArguments','test_agrument');
      equal(pig.get('scriptDirty'),true,'scriptDirty is True when Script is modified');
      script.set('pigScript.fileContent','test_content');
      equal(pig.get('scriptDirty'),true,'scriptDirty is True when both Script and File is modified');
      script.rollback();
      equal(pig.get('scriptDirty'),true,'scriptDirty is True when File is modified');
      script.get('pigScript').then(function (file) {
        file.rollback();
        equal(pig.get('scriptDirty'),false,'scriptDirty is False when File and Script is not modified');
      });
    });
  })
});
