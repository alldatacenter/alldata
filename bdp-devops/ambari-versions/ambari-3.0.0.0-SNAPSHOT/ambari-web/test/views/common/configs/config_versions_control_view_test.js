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
require('views/common/configs/config_versions_control_view');
var testHelpers = require('test/helpers');

describe('App.ConfigVersionsControlView', function () {

  var view = App.ConfigVersionsControlView.create({
    controller: Em.Object.create({
      loadSelectedVersion: Em.K,
      loadStep: Em.K,
      loadCompareVersionConfigs: Em.K,
      onLoadOverrides: Em.K,
      stepConfigs: []
    }),
    displayedServiceVersion: Em.Object.create(),
    serviceVersions: []
  });

  App.TestAliases.testAsComputedAlias(view, 'serviceName', 'controller.content.serviceName', 'string');

  describe('#switchVersion()', function () {
    var event = {
      contexts: [2]
    };
    beforeEach(function(){
      sinon.spy(view.get('controller'), 'loadSelectedVersion');
    });
    afterEach(function(){
      view.get('controller').loadSelectedVersion.restore();
    });
    it('Choose not displayed version', function () {
      view.set('serviceVersions', [
        Em.Object.create({version: 1, isDisplayed: true}),
        Em.Object.create({version: 2})
      ]);
      view.switchVersion(event);
      expect(view.get('serviceVersions').mapProperty('isDisplayed')).to.eql([false, true]);
      expect(view.get('controller').loadSelectedVersion.calledWith(2)).to.be.true;
    });

    it('Choose displayed version', function () {
      view.set('serviceVersions', [
        Em.Object.create({version: 1}),
        Em.Object.create({version: 2, isDisplayed: true})
      ]);
      view.switchVersion(event);
      expect(view.get('controller').loadSelectedVersion.called).to.be.false;
    });
  });

  describe('#compare()', function () {
    beforeEach(function(){
      sinon.stub(view.get('controller'), 'loadCompareVersionConfigs').returns({
        done: Em.clb
      });
      sinon.spy(view.get('controller'), 'onLoadOverrides');
    });
    afterEach(function(){
      view.get('controller').loadCompareVersionConfigs.restore();
      view.get('controller').onLoadOverrides.restore();
    });
    it('should set compareServiceVersion', function () {
      view.compare({contexts: [Em.Object.create({version: 1})]});

      expect(view.get('controller.compareServiceVersion')).to.eql(Em.Object.create({version: 1}));
      expect(view.get('controller').loadCompareVersionConfigs.calledOnce).to.be.true;
      expect(view.get('controller').onLoadOverrides.calledOnce).to.be.true;
    });
  });

  describe('#makeCurrent()', function () {
    beforeEach(function () {
      App.ModalPopup.show.restore();
      sinon.stub(App.ModalPopup, 'show', function (options) {
        options.onPrimary.call(Em.Object.create({
          serviceConfigNote: 'note',
          hide: Em.K
        }));
      });
      sinon.stub(view, 'sendRevertCall', Em.K);
    });
    afterEach(function () {
      view.sendRevertCall.restore();
    });
    it('context passed', function () {
      view.set('displayedServiceVersion', Em.Object.create({
        version: 1,
        serviceName: 'S1'
      }));

      view.makeCurrent();

      expect(App.ModalPopup.show.calledOnce).to.be.true;
      expect(view.sendRevertCall.calledWith(Em.Object.create({
        version: 1,
        serviceName: 'S1',
        serviceConfigNote: 'note'
      }))).to.be.true;
    });
  });

  describe('#sendRevertCall()', function () {

    beforeEach(function () {
      view.sendRevertCall(Em.Object.create());
    });

    it('request is sent', function () {
      var args = testHelpers.findAjaxRequest('name', 'service.serviceConfigVersion.revert');
      expect(args).exists;
    });
  });

  describe('#sendRevertCallSuccess()', function () {
    beforeEach(function () {
      sinon.spy(view.get('controller'), 'loadStep');
    });
    afterEach(function () {
      view.get('controller').loadStep.restore();
    });

    it('loadStep is called', function () {
      view.sendRevertCallSuccess();
      expect(view.get('controller').loadStep.calledOnce).to.be.true;
    });
  });
});
