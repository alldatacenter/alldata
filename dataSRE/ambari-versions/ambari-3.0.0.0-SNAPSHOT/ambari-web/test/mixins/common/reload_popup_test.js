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

require('mixins/common/reload_popup');

describe('App.ReloadPopupMixin', function () {

  var obj;

  beforeEach(function () {
    obj = Em.Object.create(App.ReloadPopupMixin);
  });

  describe('#popupText', function () {
    var cases = [
      {
        result: Em.I18n.t('app.reloadPopup.text'),
        title: 'should show modal popup with default message'
      },
      {
        text: 'text',
        result: 'text',
        title: 'should show modal popup with custom message'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        expect(obj.popupText(item.text)).to.equal(item.result);
      });
    });

  });

  describe('#closeReloadPopup', function () {

    it('should hide modal popup', function () {
      obj.showReloadPopup();
      obj.closeReloadPopup();
      expect(obj.get('reloadPopup')).to.be.null;
    });

  });

  describe('#reloadSuccessCallback', function () {

    it('should hide modal popup', function () {
      obj.showReloadPopup();
      obj.reloadSuccessCallback();
      expect(obj.get('reloadPopup')).to.be.null;
    });

  });

  describe('#reloadErrorCallback', function () {

    var clock,
      cases = [
        {
          args: [{status: 404}, null, null, {}, {shouldUseDefaultHandler: true}],
          closeReloadPopupCallCount: 1,
          defaultErrorHandlerCallCount: 1,
          showReloadPopupCallCount: 0,
          isCallbackCalled: false,
          title: 'status received, default error handler'
        },
        {
          args: [{status: 404}, null, null, {}, {}],
          closeReloadPopupCallCount: 1,
          defaultErrorHandlerCallCount: 0,
          showReloadPopupCallCount: 0,
          isCallbackCalled: false,
          title: 'status received, no default error handler'
        },
        {
          args: [{status: 0}, null, null, {}, {}],
          closeReloadPopupCallCount: 0,
          defaultErrorHandlerCallCount: 0,
          showReloadPopupCallCount: 1,
          isCallbackCalled: false,
          title: 'no status received, no callback'
        },
        {
          args: [{status: 0}, null, null, {}, {callback: Em.K, timeout: 2000}],
          timeout: 1999,
          closeReloadPopupCallCount: 0,
          defaultErrorHandlerCallCount: 0,
          showReloadPopupCallCount: 1,
          isCallbackCalled: false,
          title: 'no status received, callback specified, custom timeout, not enough time passed'
        },
        {
          args: [{status: 0}, null, null, {}, {callback: Em.K}],
          timeout: 999,
          closeReloadPopupCallCount: 0,
          defaultErrorHandlerCallCount: 0,
          showReloadPopupCallCount: 1,
          isCallbackCalled: false,
          title: 'no status received, callback specified, default timeout, not enough time passed'
        },
        {
          args: [{status: 0}, null, null, {}, {callback: Em.K, args: [{}], timeout: 2000}],
          timeout: 2000,
          closeReloadPopupCallCount: 0,
          defaultErrorHandlerCallCount: 0,
          showReloadPopupCallCount: 1,
          isCallbackCalled: true,
          callbackArgs: [{}],
          title: 'no status received, callback with arguments specified, custom timeout, enough time passed'
        },
        {
          args: [{status: 0}, null, null, {}, {callback: Em.K}],
          timeout: 1000,
          closeReloadPopupCallCount: 0,
          defaultErrorHandlerCallCount: 0,
          showReloadPopupCallCount: 1,
          isCallbackCalled: true,
          callbackArgs: [],
          title: 'no status received, callback with no arguments specified, default timeout, enough time passed'
        }
      ];

    cases.forEach(function (item) {
      describe(item.title, function () {

        beforeEach(function () {
          sinon.stub(obj, 'closeReloadPopup', Em.K);
          sinon.stub(App.ajax, 'defaultErrorHandler', Em.K);
          sinon.stub(obj, 'showReloadPopup', Em.K);
          sinon.stub(App, 'get').withArgs('timeout').returns(1000);
          if (item.args[4].callback) {
            sinon.spy(item.args[4], 'callback');
          }
          clock = sinon.useFakeTimers();
          obj.reloadErrorCallback.apply(obj, item.args);
          clock.tick(item.timeout);
        });

        afterEach(function () {
          obj.closeReloadPopup.restore();
          App.ajax.defaultErrorHandler.restore();
          obj.showReloadPopup.restore();
          App.get.restore();
          if (item.args[4].callback) {
            item.args[4].callback.restore();
          }
          clock.restore();
        });

        it('closeReloadPopup call', function () {
          expect(obj.closeReloadPopup.callCount).to.equal(item.closeReloadPopupCallCount);
        });
        it('defaultErrorHandler call', function () {
          expect(App.ajax.defaultErrorHandler.callCount).to.equal(item.defaultErrorHandlerCallCount);
        });
        it('showReloadPopup call', function () {
          expect(obj.showReloadPopup.callCount).to.equal(item.showReloadPopupCallCount);
        });

        if (item.isCallbackCalled) {
          it('callback call', function () {
            expect(item.args[4].callback.calledOnce).to.be.true;
          });
          it('callback context', function () {
            expect(item.args[4].callback.calledOn(obj)).to.be.true;
          });
          it('callback arguments', function () {
            expect(item.args[4].callback.firstCall.args).to.eql(item.callbackArgs);
          });
        }

      });
    });

  });

});
