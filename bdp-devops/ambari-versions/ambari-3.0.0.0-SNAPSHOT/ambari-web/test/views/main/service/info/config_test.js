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
require('views/main/service/info/configs');
var batchUtils = require('utils/batch_scheduled_requests');

describe('App.MainServiceInfoConfigsView', function () {

  var view;

  beforeEach(function () {
    view = App.MainServiceInfoConfigsView.create({
      controller: Em.Object.create({
        loadStep: Em.K,
        clearStep: Em.K,
        content: Em.Object.create()
      })
    });
  });

  describe("#resetConfigTabSelection()", function () {
    var tab = Em.Object.create({
      serviceName: 'S1',
      isActive: true
    });

    beforeEach(function () {
      sinon.stub(App.Tab, 'find').returns([tab]);
      sinon.stub(view, 'updateComponentInformation');
    });

    afterEach(function () {
      App.Tab.find.restore();
      view.updateComponentInformation.restore();
    });

    it("isActive should be false", function () {
      view.set('controller.content', Em.Object.create({serviceName: 'S1'}));
      view.resetConfigTabSelection();
      expect(tab.get('isActive')).to.be.false;
    });
  });

  describe('#updateComponentInformation', function () {

    var testCases = [
      {
        title: 'if components absent then counters should be 0',
        content: {
          restartRequiredHostsAndComponents: {}
        },
        result: {
          componentsCount: 0,
          hostsCount: 0
        }
      },
      {
        title: 'if host doesn\'t have components then hostsCount should be 1 and componentsCount should be 0',
        content: {
          restartRequiredHostsAndComponents: {
            host1: []
          }
        },
        result: {
          componentsCount: 0,
          hostsCount: 1
        }
      },
      {
        title: 'if host has 1 component then hostsCount should be 1 and componentsCount should be 1',
        content: {
          restartRequiredHostsAndComponents: {
            host1: [{}]
          }
        },
        result: {
          componentsCount: 1,
          hostsCount: 1
        }
      }
    ];
    testCases.forEach(function (test) {
      it(test.title, function () {
        view.set('controller.content', test.content);
        view.updateComponentInformation();
        expect(view.get('componentsCount')).to.equal(test.result.componentsCount);
        expect(view.get('hostsCount')).to.equal(test.result.hostsCount);
      });
    });
  });

  describe("#didInsertElement()", function() {
    var mock = {
      isLoading: function () {
        return {
          done: function (callback) {
            callback();
          }
        }
      }
    };

    beforeEach(function() {
      sinon.stub(App.router, 'get').returns(mock);
      sinon.stub(view.get('controller'), 'loadStep');
      sinon.stub(view, 'resetConfigTabSelection');
      view.didInsertElement();
    });
    afterEach(function() {
      App.router.get.restore();
      view.get('controller').loadStep.restore();
      view.resetConfigTabSelection.restore();
    });

    it("loadStep should be called", function() {
      expect(view.get('controller').loadStep.calledOnce).to.be.true;
    });

    it("resetConfigTabSelection should be called", function() {
      expect(view.resetConfigTabSelection.calledOnce).to.be.true;
    });
  });

  describe("#willDestroyElement()", function() {

    beforeEach(function() {
      sinon.stub(view.get('controller'), 'clearStep');
    });
    afterEach(function() {
      view.get('controller').clearStep.restore();
    });

    it("resetConfigTabSelection should be called", function() {
      view.willDestroyElement();
      expect(view.get('controller').clearStep.calledOnce).to.be.true;
    });
  });

  describe("#rollingRestartSlaveComponentName", function() {

    beforeEach(function() {
      sinon.stub(batchUtils, 'getRollingRestartComponentName', function(input) {
        return input;
      });
    });
    afterEach(function() {
      batchUtils.getRollingRestartComponentName.restore();
    });

    it("should return service name", function() {
      view.set('controller.content.serviceName', 'S1');
      view.propertyDidChange('rollingRestartSlaveComponentName');
      expect(view.get('rollingRestartSlaveComponentName')).to.equal('S1');
    });
  });

  describe("#rollingRestartActionName", function() {

    beforeEach(function() {
      sinon.stub(App.format, 'role', function(input) {
        return input;
      });
    });
    afterEach(function() {
      App.format.role.restore();
    });

    it("should return action name", function() {
      view.reopen({
        rollingRestartSlaveComponentName: 'C1'
      });
      view.propertyDidChange('rollingRestartActionName');
      expect(view.get('rollingRestartActionName')).to.equal(Em.I18n.t('rollingrestart.dialog.title').format('C1'));
    });

    it("should return empty", function() {
      view.reopen({
        rollingRestartSlaveComponentName: null
      });
      view.propertyDidChange('rollingRestartActionName');
      expect(view.get('rollingRestartActionName')).to.be.empty;
    });
  });
});
