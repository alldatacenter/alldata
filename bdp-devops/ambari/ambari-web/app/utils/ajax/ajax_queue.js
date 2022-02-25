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

/**
 * Simple util for executing ajax-requests queue
 *
 * @uses App.ajax
 * @type {Em.Object}
 *
 * Don't add requests directly to <code>queue</code>!
 * Bad example:
 * <code>
 *  var q = App.ajax.Queue.create();
 *  q.get('queue').pushObject({...}); // Don't do this!!!!
 * </code>
 *
 * Don't call <code>runNextRequest</code> directly!
 * Bad example:
 * <code>
 *  var q = App.ajax.Queue.create();
 *  q.runNextRequest(); // Don't do this!!!!
 * </code>
 *
 * Usage example 1:
 * <code>
 *  var q = App.ajax.Queue.create();
 *  q.addRequest({
 *    name: 'some_request',
 *    sender: this,
 *    success: 'someFunc'
 *  }).addRequest({
 *    name: 'some_request2',
 *    sender: this,
 *    success: 'someFunc2'
 *  }).start();
 * </code>
 *
 * Usage example 2:
 * <code>
 *  var q = App.ajax.Queue.create();
 *  q.addRequest({
 *    name: 'some_request',
 *    sender: this,
 *    success: 'someFunc'
 *  });
 *  q.addRequest({
 *    name: 'some_request2',
 *    sender: this,
 *    success: 'someFunc2'
 *  });
 *  q.start();
 * </code>
 *
 * Usage example 3:
 * <code>
 *  var q = App.ajax.Queue.create();
 *  q.addRequests(Em.A([{
 *    name: 'some_request',
 *    sender: this,
 *    success: 'someFunc'
 *  },
 *  {
 *    name: 'some_request2',
 *    sender: this,
 *    success: 'someFunc2'
 *  }]));
 *  q.start();
 * </code>
 */
App.ajaxQueue = Em.Object.extend({

  /**
   * List of requests
   * @type {Object[]}
   */
  queue: Em.A([]),

  /**
   * About query executing if some request failed
   * @type {bool}
   */
  abortOnError: true,

  /**
   * Function called after queue is complete
   * @type {callback}
   */
  finishedCallback: Em.K,

  /**
   * Add request to the <code>queue</code>
   * @param {Object} request object that uses in <code>App.ajax.send</code>
   * @return {App.ajaxQueue}
   * @method addRequest
   */
  addRequest: function(request) {
    Em.assert('Each ajax-request should has non-blank `name`', !Em.isBlank(Em.get(request, 'name')));
    Em.assert('Each ajax-request should has object `sender`', Em.typeOf(Em.get(request, 'sender')) !== 'object');
    this.get('queue').pushObject(request);
    return this;
  },

  /**
   * Add requests to the <code>queue</code>
   * @param {Object[]} requests list of objects that uses in <code>App.ajax.send</code>
   * @return {App.ajaxQueue}
   * @method addRequests
   */
  addRequests: function(requests) {
    requests.map(function(request) {
      this.addRequest(request);
    }, this);
    return this;
  },

  /**
   * Enter point to start requests executing
   * @method start
   */
  start: function() {
    this.runNextRequest();
  },

  /**
   * Execute first request from the <code>queue</code>
   * @method runNextRequest
   */
  runNextRequest: function() {
    var queue = this.get('queue');
    if (queue.length === 0) {
      this.finishedCallback();
      return;
    }
    var r = App.ajax.send(queue.shift());
    this.propertyDidChange('queue');
    if (r) {
      r.complete(this._complete.bind(this));
    }
    else {
      if (this.get('abortOnError')) {
        this.clear();
      }
      else {
        this.runNextRequest();
      }
    }
  },

  _complete: function(xhr) {
    if(xhr.status>=200 && xhr.status <= 299) {
      this.runNextRequest();
    }
    else {
      if (this.get('abortOnError')) {
        this.clear();
      }
      else {
        this.runNextRequest();
      }
    }
  },

  /**
   * Remove all requests from <code>queue</code>
   * @method clear
   */
  clear: function() {
    this.get('queue').clear();
  }

});
