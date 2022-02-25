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

QUnit.module('integration/pages - index',{
  teardown: function () {
    App.reset();
  }
});

test('404 response on node labels', function () {
  var store = App.__container__.lookup('store:main');

  store.adapterFor('queue').ajax = function(url, type, hash) {
    var adapter = this;

    return new Ember.RSVP.Promise(function(resolve, reject) {
      hash = adapter.ajaxOptions(url, type, hash);

      hash.success = function(json) {
        if (url === "/data/nodeLabels.json") {
          Ember.run(null, reject, adapter.ajaxError({status:404}));
        } else {
          Ember.run(null, resolve, json);
        }
      };

      hash.error = function(jqXHR, textStatus, errorThrown) {
        Ember.run(null, reject, adapter.ajaxError(jqXHR));
      };

      Ember.$.ajax(hash);
    }, "App: QueueAdapter#ajax " + type + " to " + url);
  }

  visit('/');

  andThen(function () {
    deepEqual(store.get('nodeLabels.content'), [], 'store has empty node labels on 404');
  });

});
