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

var ajaxQueue;

describe('App.ajaxQueue', function () {

  beforeEach(function() {
    ajaxQueue = App.ajaxQueue.create();
    sinon.spy(ajaxQueue, 'runNextRequest');
    sinon.spy(ajaxQueue, 'finishedCallback');
  });

  afterEach(function() {
    ajaxQueue.clear();
    ajaxQueue.runNextRequest.restore();
    ajaxQueue.finishedCallback.restore();
  });

  describe('#clear', function() {
    it('should clear queue', function() {
      ajaxQueue.addRequest({name:'some', sender: Em.Object.create()});
      ajaxQueue.clear();
      expect(ajaxQueue.get('queue.length')).to.equal(0);
    });
  });

  describe('#addRequest', function() {
    it('should add request', function() {
      ajaxQueue.addRequest({name:'some', sender: Em.Object.create()});
      expect(ajaxQueue.get('queue.length')).to.equal(1);
    });
    it('should throw `name` error', function() {
      expect(function() {ajaxQueue.addRequest({name:'', sender: Em.Object.create()})}).to.throw(Error);
    });
    it('should throw `sender` error', function() {
      expect(function() {ajaxQueue.addRequest({name:'some', sender: {}})}).to.throw(Error);
    });
  });

  describe('#addRequests', function() {
    it('should add requests', function() {
      ajaxQueue.addRequests(Em.A([
        {name:'some', sender: Em.Object.create()},
        {name:'some2', sender: Em.Object.create()}
      ]));
      expect(ajaxQueue.get('queue.length')).to.equal(2);
    });

    it('should throw `name` error', function() {
      expect(function() {ajaxQueue.addRequests(Em.A([
        {name:'some', sender: Em.Object.create()},
        {name:'', sender: Em.Object.create()}
      ]));}).to.throw(Error);
    });

    it('should throw `sender` error', function() {
      expect(function() {ajaxQueue.addRequests(Em.A([
        {name:'some', sender: Em.Object.create()},
        {name:'some2', sender: {}}
      ]));}).to.throw(Error);
    });

  });

  describe('#start', function() {
    it('should call runNextRequest', function() {
      ajaxQueue.start();
      expect(ajaxQueue.runNextRequest.called).to.equal(true);
    });
  });

  describe('#runNextRequest', function() {
    it('for empty queue App.ajax.send shouldn\'t be called', function() {
      ajaxQueue.clear();
      ajaxQueue.runNextRequest();
      expect(App.ajax.send.called).to.equal(false); // eslint-disable-line mocha-cleanup/disallowed-usage
    });
    it('when queue is empty finishedCallback should be called', function() {
      ajaxQueue.clear();
      ajaxQueue.runNextRequest();
      expect(ajaxQueue.finishedCallback.called).to.equal(true);
    });
  });

});
