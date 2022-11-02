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
require('utils/http_client');

describe('App.HttpClient', function () {

  describe('#defaultErrorHandler', function () {

    var cases = [
      {
        isAsserted: false,
        title: 'no response text'
      },
      {
        responseText: null,
        title: 'empty response text'
      },
      {
        responseText: 404,
        title: 'invalid response text (number)'
      },
      {
        responseText: 'error',
        title: 'invalid response text (string)'
      },
      {
        responseText: '{error}',
        title: 'malformed response text (incorrect literal)'
      },
      {
        responseText: '{error: 404}',
        title: 'malformed response text (no parentheses)'
      },
      {
        responseText: '{\'error\': 404}',
        title: 'malformed response text (incorrect parentheses)'
      },
      {
        responseText: '{"error": 404}',
        title: 'valid response text'
      }
    ];

    cases.forEach(function (item) {

      describe(item.title, function () {

        var jqXHR = {
          responseText: item.responseText
        };

        beforeEach(function () {
          sinon.stub(App.ajax, 'defaultErrorHandler', Em.K);
          sinon.spy(Em, 'assert');
          App.HttpClient.defaultErrorHandler(jqXHR, '', '', 'http://localhost');
        });

        afterEach(function () {
          App.ajax.defaultErrorHandler.restore();
          Em.assert.restore();
        });

        it('default error handler call', function () {
          expect(App.ajax.defaultErrorHandler.calledOnce).to.be.true;
        });

        it('default error handler arguments', function () {
          expect(App.ajax.defaultErrorHandler.firstCall.args).to.eql([jqXHR, 'http://localhost']);
        });

        it('no console error assertion', function () {
          expect(Em.assert.threw()).to.be.false;
        });

      });

    });

  });

  describe('#request', function () {

    var errorHandler = Em.K,
      ajaxOptions = {
        params: 'property=value'
      },
      mapper = {},
      cases = [
        {
          url: 'url',
          generatedUrl: 'url?_=1000',
          errorHandler: null,
          passedErrorHandler: App.HttpClient.defaultErrorHandler,
          isGetAsPost: false,
          method: 'GET',
          setRequestHeaderCallCount: 0,
          params: null,
          title: 'no request parameters, default error handler'
        },
        {
          url: 'url?property0=value0&property1=value1',
          generatedUrl: 'url?property0=value0&property1=value1&_=1000',
          errorHandler: errorHandler,
          passedErrorHandler: errorHandler,
          isGetAsPost: true,
          method: 'POST',
          setRequestHeaderCallCount: 2,
          params: '{"RequestInfo":{"query":"property=value"}}',
          title: 'request parameters passed, POST request'
        }
      ];

    cases.forEach(function (item) {

      describe(item.title, function () {

        beforeEach(function () {
          sinon.stub(XMLHttpRequest.prototype, 'open', Em.K);
          sinon.stub(XMLHttpRequest.prototype, 'setRequestHeader', Em.K);
          sinon.stub(XMLHttpRequest.prototype, 'send', Em.K);
          sinon.stub(App, 'dateTime').returns(1000);
          sinon.stub(App.HttpClient, 'onReady', Em.K);
          App.HttpClient.request(item.url, ajaxOptions, mapper, item.errorHandler, item.isGetAsPost);
        });

        afterEach(function () {
          XMLHttpRequest.prototype.open.restore();
          XMLHttpRequest.prototype.setRequestHeader.restore();
          XMLHttpRequest.prototype.send.restore();
          App.dateTime.restore();
          App.HttpClient.onReady.restore();
        });

        it('request method', function () {
          expect(XMLHttpRequest.prototype.open.firstCall.args[0]).to.equal(item.method);
        });

        it('request URL', function () {
          expect(XMLHttpRequest.prototype.open.firstCall.args[1]).to.equal(item.generatedUrl);
        });

        it('setting request headers', function () {
          expect(XMLHttpRequest.prototype.setRequestHeader.callCount).to.equal(item.setRequestHeaderCallCount);
        });

        it('request params', function () {
          expect(XMLHttpRequest.prototype.send.firstCall.args[0]).to.equal(item.params);
        });

        it('onReady callback: ajaxOptions', function () {
          expect(App.HttpClient.onReady.firstCall.args[2]).to.eql(ajaxOptions);
        });

        it('onReady callback: mapper', function () {
          expect(App.HttpClient.onReady.firstCall.args[3]).to.eql(mapper);
        });

        it('onReady callback: errorHandler', function () {
          expect(App.HttpClient.onReady.firstCall.args[4]).to.eql(item.passedErrorHandler);
        });

        it('onReady callback: url', function () {
          expect(App.HttpClient.onReady.firstCall.args[5]).to.equal(item.url);
        });

      });

    });

  });

  describe('#onReady', function () {

    var clock,
      xhr = {
        responseText: '{"property": "value"}',
        statusText: 'status',
        abort: Em.K
      },
      ajaxOptions = {
        complete: Em.K
      },
      mapper = {
        map: Em.K
      },
      mock = {
        errorHandler: Em.K
      },
      cases = [
        {
          readyState: 4,
          status: 200,
          isCommitError: false,
          commitCallCount: 1,
          mapCallCount: 1,
          completeCallCount: 1,
          abortCallCount: 1,
          errorHandlerCallCount: 0,
          onReadyCallCount: 1,
          title: 'successful request'
        },
        {
          readyState: 4,
          status: 200,
          isCommitError: true,
          commitCallCount: 1,
          mapCallCount: 1,
          completeCallCount: 1,
          abortCallCount: 1,
          errorHandlerCallCount: 0,
          onReadyCallCount: 1,
          title: 'successful request, App.store.commit error'
        },
        {
          readyState: 4,
          status: 404,
          isCommitError: false,
          commitCallCount: 0,
          mapCallCount: 0,
          completeCallCount: 0,
          abortCallCount: 0,
          errorHandlerCallCount: 1,
          onReadyCallCount: 1,
          title: 'failed request'
        },
        {
          readyState: 3,
          status: 200,
          isCommitError: false,
          commitCallCount: 1,
          mapCallCount: 1,
          completeCallCount: 1,
          abortCallCount: 1,
          errorHandlerCallCount: 0,
          onReadyCallCount: 2,
          title: 'incomplete request, later successful'
        },
        {
          readyState: 3,
          status: 404,
          isCommitError: false,
          commitCallCount: 0,
          mapCallCount: 0,
          completeCallCount: 0,
          abortCallCount: 0,
          errorHandlerCallCount: 1,
          onReadyCallCount: 2,
          title: 'incomplete request, later failed'
        }
      ];

    cases.forEach(function (item) {

      describe(item.title, function () {

        beforeEach(function () {
          clock = sinon.useFakeTimers();
          sinon.spy(xhr, 'abort');
          sinon.spy(mapper, 'map');
          sinon.spy(mock, 'errorHandler');
          sinon.spy(ajaxOptions, 'complete');
          sinon.spy(App.HttpClient, 'onReady');
          xhr.readyState = item.readyState;
          xhr.status = item.status;
          App.HttpClient.onReady(xhr, null, ajaxOptions, mapper, mock.errorHandler, 'url');
          clock.tick(10);
          xhr.readyState = 4;
          clock.tick(10);
        });

        afterEach(function () {
          clock.restore();
          xhr.abort.restore();
          mapper.map.restore();
          mock.errorHandler.restore();
          ajaxOptions.complete.restore();
          App.HttpClient.onReady.restore();
        });

        it('mapping data', function () {
          expect(mapper.map.callCount).to.equal(item.mapCallCount);
        });

        if (item.mapCallCount) {
          it('mapped data', function () {
            expect(mapper.map.alwaysCalledWith({
              property: 'value'
            })).to.be.true;
          });
        }

        it('complete callback call', function () {
          expect(ajaxOptions.complete.callCount).to.equal(item.completeCallCount);
        });

        if (item.completeCallCount) {
          it('complete callback context', function () {
            expect(ajaxOptions.complete.alwaysCalledOn(App.HttpClient)).to.be.true;
          });
        }

        it('abort request', function () {
          expect(xhr.abort.callCount).to.equal(item.abortCallCount);
        });

        it('error handler call', function () {
          expect(mock.errorHandler.callCount).to.equal(item.errorHandlerCallCount);
        });

        if (item.errorHandlerCallCount) {
          it('error handler arguments', function () {
            expect(mock.errorHandler.alwaysCalledWith(xhr, 'error', 'status', 'url')).to.be.true;
          });
        }

        it('onReady iterations number', function () {
          expect(App.HttpClient.onReady.callCount).to.equal(item.onReadyCallCount);
        });

      });

    });

  });

  describe('#get', function () {

    var mapper = {},
      cases = [
        {
          data: {
            error: Em.clb
          },
          errorHandler: Em.K,
          passedErrorHandler: Em.K,
          isGetAsPost: false,
          title: 'custom error handler'
        },
        {
          data: {
            error: Em.clb,
            doGetAsPost: true
          },
          interval: 1,
          passedErrorHandler: Em.clb,
          isGetAsPost: true,
          title: 'error handler from data, interval provided, POST request'
        }
      ];

    cases.forEach(function (item) {

      describe(item.title, function () {

        beforeEach(function () {
          sinon.stub(App.HttpClient, 'request', Em.K);
          sinon.stub($, 'periodic', function (options, callback) {
            callback();
          });
          App.HttpClient.get('url', mapper, item.data, item.errorHandler, item.interval);
        });

        afterEach(function () {
          App.HttpClient.request.restore();
          $.periodic.restore();
        });

        it('request call', function () {
          expect(App.HttpClient.request.calledOnce).to.be.true;
        });

        it('request arguments', function () {
          expect(App.HttpClient.request.firstCall.args).to.eql(['url', item.data, mapper, item.passedErrorHandler, item.isGetAsPost]);
        });

      });

    });

  });

  describe('#post', function () {

    var args = ['url', {}, {}, Em.K, 1];

    beforeEach(function () {
      sinon.stub(App.HttpClient, 'get', Em.K);
      App.HttpClient.post.apply(App.HttpClient, args);
    });

    afterEach(function () {
      App.HttpClient.get.restore();
    });

    it('should call get method', function () {
      expect(App.HttpClient.get.calledOnce).to.be.true;
    });

    it('get method arguments', function () {
      expect(App.HttpClient.get.firstCall.args).to.eql(args);
    });

  });

});