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

var App = require('app');

var _promise = function (controller, url, output) {
  return new Ember.RSVP.Promise(function(resolve,reject){
    return Em.$.getJSON(url).then(function (data) {
      resolve(data.file);
    },function (error) {
      var response = (error.responseJSON)?error.responseJSON:{};
      reject(response.message);
      if (error.status != 404) {
        controller.send('showAlert', {'message': Em.I18n.t('job.alert.promise_error',
          {status:response.status, message:response.message}), status:'error', trace: response.trace});
      }
    });
  });
};

App.FileHandler = Ember.Mixin.create({
  fileProxy:function (url) {
    var promise,
        host = this.store.adapterFor('application').get('host'),
        namespace = this.store.adapterFor('application').get('namespace');

    url = [host, namespace, url].join('/');
    promise = _promise(this, url,'stdout');

    return Ember.ObjectProxy.extend(Ember.PromiseProxyMixin).create({
      promise: promise
    });
  },
  downloadFile:function (file,saveAs) {
    return this.fileSaver.save(file, "application/json", saveAs);
  }
});

App.inject('controller', 'fileSaver', 'lib:fileSaver');
