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


moduleFor('controller:scriptJob', 'App.ScriptJobController',{},function (container) {
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

test('filename prefix',function () {
  var _this = this,
      controller,
      store = this.container.lookup('store:main'),
      testId = 'job_1234567890000_0000';

  Em.run(function () {
    controller = _this.subject({
      model: store.createRecord('job',{jobId:testId})
    });
    ok(controller.get('suggestedFilenamePrefix') == testId);
  });

});

test('get script content',function () {
  var _this = this,
      controller,
      store = this.container.lookup('store:main');

  Em.run(function () {
    controller = _this.subject({
      model: store.createRecord('job',{pigScript:store.createRecord('file',{fileContent:'test_content'})})
    });
    controller.get('scriptContents').then(function (scriptContents) {
      ok(controller.get('scriptContents.isFulfilled') && controller.get('scriptContents.fileContent') == 'test_content','file loaded');
    });
    ok(controller.get('scriptContents.isPending'), 'file is loading');
  });

});

test('toggle fullscreen', function () {
  var controller = this.subject();

  Em.run(function () {
    controller.send('fullscreen');
    ok(controller.get('fullscreen'));
  });
});
