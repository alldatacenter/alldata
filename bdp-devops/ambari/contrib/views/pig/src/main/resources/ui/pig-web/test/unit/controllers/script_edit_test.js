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


moduleFor('controller:scriptEdit', 'App.ScriptEditController', {
  needs:['controller:script'],
  setup:function () {
    var store = this.container.lookup('store:main');
    var _this = this;
    Ember.run(function() {
      _this.subject({
        model: store.createRecord('script',{pigScript:store.createRecord('file')})
      });
    });
  }
},function (container) {
  container.register('model:file', App.File);
  container.register('model:script', App.Script);
  container.register('model:job', App.Job);
  container.register('store:main', DS.Store);
  container.register('adapter:application', DS.FixtureAdapter);
  container.register('serializer:application', DS.RESTSerializer);
  container.register('transform:boolean', DS.BooleanTransform);
  container.register('transform:date', DS.DateTransform);
  container.register('transform:number', DS.NumberTransform);
  container.register('transform:string', DS.StringTransform);
  container.register('transform:isodate', App.IsodateTransform);
});

test('script parameters parsing test',function () {
  var controller = this.subject();

  Em.run(function () {
    controller.set('content.pigScript.fileContent','%test_param%');
    equal(controller.get('pigParams.length'),1,'can set pig parameter');
    equal(controller.get('pigParams.firstObject.title'), 'test_param', 'pig parameter parsed right');
    controller.set('content.pigScript.fileContent','%test_param% %test_param%');
    equal(controller.get('pigParams.length'),1,'controller has no pig parameter duplicates');
  });

});

test('run job without parameters',function () {
  expect(3);
  var controller = this.subject();
  var store = this.container.lookup('store:main');

  Em.run(function () {

    var file = store.createRecord('file', {
      id:1,
      fileContent:'test_content'
    });

    var script = store.createRecord('script',{
      id:1,
      templetonArguments:'-test_argument',
      title:'test_script',
      pigScript:file
    });

    controller.set('model',script);
    controller.set('store',store);

    stop();

    controller.prepareJob('execute',[file,script]).then(function (job) {
      deepEqual(job.toJSON(),{
        title: 'test_script',
        jobType: null,
        pigScript: '1',
        scriptId: 1,
        templetonArguments: '-test_argument',
        dateStarted: undefined,
        duration: null,
        forcedContent: null,
        jobId: null,
        owner: null,
        percentComplete: null,
        sourceFile: null,
        sourceFileContent: null,
        status: null,
        statusDir: null
      },'execute processed correctly');

      return controller.prepareJob('explain',[file,script]);
    }).then(function (job) {
      deepEqual(job.toJSON(),{
        title: 'Explain: "test_script"',
        jobType: 'explain',
        pigScript: null,
        scriptId: 1,
        templetonArguments: '',
        dateStarted: undefined,
        duration: null,
        forcedContent: 'explain -script ${sourceFile}',
        jobId: null,
        owner: null,
        percentComplete: null,
        sourceFile: '1',
        sourceFileContent: null,
        status: null,
        statusDir: null
      },'explain processed correctly');

      return controller.prepareJob('syntax_check',[file,script]);
    }).then(function (job) {
      deepEqual(job.toJSON(),{
        title: 'Syntax check: "test_script"',
        jobType: 'syntax_check',
        pigScript: '1',
        scriptId: 1,
        templetonArguments: '-test_argument\t-check',
        dateStarted: undefined,
        duration: null,
        forcedContent: null,
        jobId: null,
        owner: null,
        percentComplete: null,
        sourceFile: null,
        sourceFileContent: null,
        status: null,
        statusDir: null
      },'syntax check processed correctly');
      start();
    });
  });
});

test('run job with parameters',function () {
  expect(3);
  var controller = this.subject();
  var store = this.container.lookup('store:main');

  Em.run(function () {

    var file = store.createRecord('file',{
      id:1,
      fileContent:'test_content %test_parameter%'
    });

    var script = store.createRecord('script',{
      id:1,
      templetonArguments:'-test_argument',
      title:'test_script',
      pigScript:file
    });

    controller.get('pigParams').pushObject(Em.Object.create({param:'%test_parameter%',value:'test_parameter_value',title:'test_parameter'}));
    controller.set('model',script);
    controller.set('store',store);

    stop();

    controller.prepareJob('execute',[file,script]).then(function (job) {
      deepEqual(job.toJSON(),{
        title: 'test_script',
        jobType: null,
        pigScript: null,
        scriptId: 1,
        templetonArguments: '-test_argument',
        dateStarted: undefined,
        duration: null,
        forcedContent: 'test_content test_parameter_value',
        jobId: null,
        owner: null,
        percentComplete: null,
        sourceFile: null,
        sourceFileContent: null,
        status: null,
        statusDir: null
      },'execute with parameters processed correctly');

      return controller.prepareJob('explain',[file,script]);
    }).then(function (job) {
      deepEqual(job.toJSON(),{
        title: 'Explain: "test_script"',
        jobType: 'explain',
        pigScript: null,
        scriptId: 1,
        templetonArguments: '',
        dateStarted: undefined,
        duration: null,
        forcedContent: 'explain -script ${sourceFile}',
        jobId: null,
        owner: null,
        percentComplete: null,
        sourceFile: null,
        sourceFileContent: 'test_content test_parameter_value',
        status: null,
        statusDir: null
      },'explain with parameters processed correctly');

      return controller.prepareJob('syntax_check',[file,script]);
    }).then(function (job) {
      deepEqual(job.toJSON(),{
        title: 'Syntax check: "test_script"',
        jobType: 'syntax_check',
        pigScript: null,
        scriptId: 1,
        templetonArguments: '-test_argument\t-check',
        dateStarted: undefined,
        duration: null,
        forcedContent: 'test_content test_parameter_value',
        jobId: null,
        owner: null,
        percentComplete: null,
        sourceFile: null,
        sourceFileContent: null,
        status: null,
        statusDir: null
      },'syntax check with parameters processed correctly');
      start();
    });
  });
});

test('rename test',function () {
  expect(3);
  var controller = this.subject();
  var script = controller.get('content');
  stop();

  Em.run(function () {
    var original_title = 'test_title';

    script.set('title',original_title);

    script.save().then(function (script) {
      start();
      controller.send('rename','ask');

      ok(controller.get('oldTitle') == original_title && controller.get('isRenaming'), 'start renaming ok');

      script.set('title','wrong_title');

      controller.send('rename','cancel');

      ok(script.get('title') == original_title && controller.get('oldTitle') == '' && !controller.get('isRenaming'),'undo renaming ok');

      controller.send('rename','ask');
      script.set('title','right_title');
      stop();
      controller.send('rename','right_title');

      script.didUpdate = function () {
        start();
        ok(script.get('title') == 'right_title' && controller.get('oldTitle') == '' && !controller.get('isRenaming'),'rename done');
      };
    });
  });
});

test('add templeton arguments',function () {
  expect(4);
  var controller = this.subject();
  var script = controller.get('content');

  Em.run(function () {
    var arg1 = '-test_argument',
        arg2 = '-other_test_argument';

    controller.set('tmpArgument',arg1);
    controller.send('addArgument',arg1);
    ok(script.get('templetonArguments') == arg1 && controller.get('tmpArgument') == '','can add argument');

    controller.set('tmpArgument',arg1);
    controller.send('addArgument',arg1);
    ok(script.get('templetonArguments') == arg1 && controller.get('tmpArgument') == arg1,'can\'t add duplicates');

    controller.set('tmpArgument',arg2);
    controller.send('addArgument',arg2);
    ok((script.get('templetonArguments') == arg1+'\t'+arg2 || script.get('templetonArguments') == arg2+'\t'+arg1) && controller.get('tmpArgument') == '','can add more arguments');

    controller.send('removeArgument',arg2);
    ok(script.get('templetonArguments') == arg1, 'can remove argument');
  });
});
