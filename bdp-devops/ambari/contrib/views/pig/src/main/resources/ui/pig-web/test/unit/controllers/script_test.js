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


moduleFor('controller:script', 'App.ScriptController',
  {
    needs:['controller:pig','controller:scriptEdit'],
    teardown:function (container) {
      container.lookup('store:main').unloadAll('job');
      container.lookup('store:main').unloadAll('script');
      container.lookup('store:main').unloadAll('file');
    }
  },
  function (container) {
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
  }
);

App.Job.reopenClass({
  FIXTURES: [
    { id: 1, status: 'SUBMITTED', scriptId:1, title: 'test_job'},
    { id: 2, status: 'SUBMITTED', scriptId:1, title: 'test_job'},
    { id: 3, status: 'SUBMITTED', scriptId:2, title: 'test_job'}
  ]
});

App.Script.reopenClass({
  FIXTURES: [
    { id: 1 },
    { id: 2 },
    { id: 3 }
  ]
});

test('polling starts when activeJobs is set',function () {
  expect(1);
  var controller = this.subject(),
      store = this.container.lookup('store:main');
  Em.run(function () {
    stop();
    store.find('job',1).then(function (job) {
      start();
      controller.set('activeJobs',[job]);
      ok(controller.get('pollster.timer'));
    });
  });
});

test('reset activeJobs when activeScript id changes',function () {
  expect(1);
  var controller = this.subject(),
      store = this.container.lookup('store:main');
  var pig = controller.get('controllers.pig');


  var scripts = store.find('script');
  var jobs = store.find('job');
  Em.run(function () {
    stop();
    Em.RSVP.all([scripts,jobs]).then(function () {
      start();
      var initialJobs = jobs.filterBy('scriptId',1);

      pig.set('model',scripts);
      pig.set('activeScriptId',1);
      controller.set('activeJobs',initialJobs);

      pig.set('activeScriptId',2);

      ok(Em.isEmpty(controller.get('activeJobs')));
    });
  });
});

test('tabs test',function () {
  expect(5);
  var controller = this.subject(),
      store = this.container.lookup('store:main'),
      pig = controller.get('controllers.pig');

  stop();
  store.find('job').then(function (jobs) {
    start();
    Em.run(function () {
      ok(controller.get('tabs.length')==2,'start with two tabs');

      controller.get('activeJobs').pushObject(jobs.objectAt(0));
      stop();
      Em.run.next(function () {
        start();
        ok(controller.get('tabs.length') == 3, 'added new tab for active job');

        jobs.objectAt(0).set('status','RUNNING');
        ok(controller.get('jobTabs.firstObject.label').toUpperCase().match(/RUNNING$/), 'label updates on job status change');

        controller.get('activeJobs').pushObject(jobs.objectAt(1));
        stop();
        Em.run.next(function () {
          start();
          ok(controller.get('tabs.length') == 4, 'added one more tab for active job');

          pig.set('activeScriptId',2);

          stop();
          Em.run.next(function () {
            start();
            ok(controller.get('tabs.length') == 2, 'reset tabs on script change');
          })
        });
      });
    });
  });
});
