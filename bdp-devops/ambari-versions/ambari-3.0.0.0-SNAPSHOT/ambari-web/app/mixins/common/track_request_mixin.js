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

App.TrackRequestMixin = Em.Mixin.create({

  requestsInProgress: [],

  init: function() {
    this.set('requestsInProgress', []);
    this._super([].slice.call(arguments));
  },

  /**
   * register request to view to track his progress
   * @param {$.ajax} request
   * @method trackRequest
   */
  trackRequest: function (request) {
    var requestId = this.get('requestsInProgress').length;
    var self = this;
    this.get('requestsInProgress').pushObject({
      request: request,
      id: requestId,
      status: Em.tryInvoke(request, 'state'),
      completed: ['resolved', 'rejected'].contains(Em.tryInvoke(request, 'state'))
    });
    request.always(function() {
      var requestInProgress = self.get('requestsInProgress').findProperty('id', requestId) || {};
      Em.setProperties(requestInProgress, {
        completed: true,
        status: Em.tryInvoke(request, 'state')
      });
    });
  },

  /**
   * This method used to put promise to requests queue which is waiting for another request to be put in tracking queue
   * after tracking request promise will be completed. Basically it used for places where trackRequest called after
   * tracked promise gets resolved.
   *
   * @param {$.ajax} request
   */
  trackRequestChain: function(request) {
    var dfd = $.Deferred();
    request.always(function() {
      dfd.resolve();
    });
    this.trackRequest(request);
    this.trackRequest(dfd);
  },

  /**
   * abort all tracked requests
   * @method abortRequest
   */
  abortRequests: function () {
    this.get('requestsInProgress').forEach(function(r) {
      var request = r.request;
      if (request && request.readyState !== undefined && request.readyState !== 4) {
        request.abort();
      }
    });
    this.get('requestsInProgress').clear();
  }

});
