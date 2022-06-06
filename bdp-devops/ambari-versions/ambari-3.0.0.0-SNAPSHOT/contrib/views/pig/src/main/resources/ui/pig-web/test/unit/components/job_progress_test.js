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


moduleForComponent('job-progress', 'App.JobProgressComponent', {
  setup:function (container) {
    container.register('model:file', App.File);
    container.register('model:job', App.Job);
    container.register('store:main', DS.Store);
  }
});

test('update progress bar', function () {
  var job,
      bar,
      wrap,
      store = this.container.lookup('store:main');

  Em.run(function() {
    job = store.createRecord('job',{percentComplete:0});
  });

  wrap = this.subject({job:job});
  this.append();
  bar = wrap.$().find('.progress-bar');

  ok((100 * parseFloat(bar.css('width')) / parseFloat(bar.parent().css('width'))) == 0);

  Em.run(function() {
    job.set('percentComplete',100);
  });

  andThen(function() {
    ok((100 * parseFloat(bar.css('width')) / parseFloat(bar.parent().css('width'))) == 100);
  });

});


