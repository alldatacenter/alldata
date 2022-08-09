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
var testHelpers = require('test/helpers');
var LZString = require('utils/lz-string');

describe('App.Persist', function () {
  var mixin;

  beforeEach(function () {
    mixin = Em.Object.create(App.Persist, {
      additionalData: {}
    });
  });

  describe('#getUserPref', function() {

    it('App.ajax.send should be called', function() {
      mixin.getUserPref('foo');
      var args = testHelpers.findAjaxRequest('name', 'persist.get');
      expect(args[0]).to.be.eql({
        name: 'persist.get',
        sender: mixin,
        data: {
          key: 'foo',
          data: {}
        },
        success: 'getUserPrefSuccessCallback',
        error: 'getUserPrefErrorCallback'
      });
    });
  });

  describe('#getDecompressedData', function() {

    it('App.ajax.send should be called', function() {
      mixin.getDecompressedData('foo');
      var args = testHelpers.findAjaxRequest('name', 'persist.get.text');
      expect(args[0]).to.be.eql({
        name: 'persist.get.text',
        sender: mixin,
        data: {
          key: 'foo'
        }
      });
    });
  });

  describe('#post', function() {

    it('App.ajax.send should be called', function() {
      mixin.post({"foo": "bar"});
      var args = testHelpers.findAjaxRequest('name', 'persist.post');
      expect(args[0]).to.be.eql({
        'name': 'persist.post',
        'sender': mixin,
        'beforeSend': 'postUserPrefBeforeSend',
        'data': {
          'keyValuePair': {"foo": "bar"}
        },
        'success': 'postUserPrefSuccessCallback',
        'error': 'postUserPrefErrorCallback'
      });
    });
  });

  describe('#postUserPref', function() {
    beforeEach(function() {
      sinon.stub(mixin, 'post');
      this.mockAuthorize = sinon.stub(App, 'isAuthorized');
    });
    afterEach(function() {
      mixin.post.restore();
      this.mockAuthorize.restore();
    });

    it('post should be called when authorized', function() {
      this.mockAuthorize.withArgs('CLUSTER.MANAGE_USER_PERSISTED_DATA').returns(true);
      mixin.postUserPref('foo', {"foo": "bar"});
      expect(mixin.post.calledWith({'foo': '{"foo":"bar"}'})).to.be.true;
    });

    it('post should not be called when authorized', function() {
      this.mockAuthorize.withArgs('CLUSTER.MANAGE_USER_PERSISTED_DATA').returns(false);
      mixin.postUserPref('foo', {"foo": "bar"});
      expect(mixin.post.called).to.be.false;
    });
  });

  describe('#postCompressedData', function() {
    beforeEach(function() {
      sinon.stub(mixin, 'post');
      sinon.stub(LZString, 'compressToBase64', function(args) {return args;})
    });
    afterEach(function() {
      mixin.post.restore();
      LZString.compressToBase64.restore();
    });

    it('post should be called with object value', function() {
      mixin.postCompressedData('foo', {"foo": "bar"});
      expect(mixin.post.calledWith({'foo': '{"foo":"bar"}'})).to.be.true;
    });
    it('post should be called with empty value', function() {
      mixin.postCompressedData('foo', null);
      expect(mixin.post.calledWith({'foo': ''})).to.be.true;
    });
  });
});
