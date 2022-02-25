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


moduleForComponent('code-mirror', 'App.CodeMirrorComponent',{
  setup:function (container) {
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
  }
});

test('fill editor with script',function () {
  expect(1);
  var editor = this.subject(),
      store = this.container.lookup('store:main');

  this.append();

  Em.run(function () {
    var script = store.createRecord('file', {fileContent:'test_content'});
    editor.set('content',script);
    stop();
    script.save().then(function () {
      start();
      script.set('isFulfilled',true);
      equal(editor.get('codeMirror').getValue(),'test_content');
    });
  });
});

test('can toggle fullscreen mode',function  () {
  var editor = this.subject();
  this.append();

  Em.run(function() {
    var fs_state = editor.get('codeMirror').getOption('fullScreen');

    Em.run(function() {
      editor.toggleProperty('fullscreen');
    });

    Em.run.next(function() {
      equal(editor.get('codeMirror').getOption('fullScreen'),!fs_state);
    });
  });
});

test('sync between script and editor content', function() {
  expect(2);
  var editor = this.subject(),
      store = this.container.lookup('store:main');

  this.append();

  var script,
      cm = editor.get('codeMirror'),
      test_content = 'content',
      test_addition = 'addition',
      final_content = test_content + test_addition;

  Em.run(function() {
    script = store.createRecord('file', {fileContent:test_content});
  });

  Em.run(function () {
    editor.set('content',script);
    cm.setCursor({line:0,ch:test_content.length});
    cm.replaceRange(test_addition,{line:0,ch:test_content.length},{line:0,ch:test_content.length});
  });

  equal(cm.getCursor().ch,final_content.length,'script content updates when editor changes');

  Em.run(function() {
    script.set('fileContent',test_addition);
  });

  equal(cm.getValue(),test_addition, 'editor content updates when script changes');

});


