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
var date = require('utils/date/date');
var numberUtils = require('utils/number_utils');

require('/views/main/service/services/yarn');

function getView(options) {
  return App.MainDashboardServiceYARNView.create(options || {});
}

describe('App.MainDashboardServiceYARNView', function () {
  var view;

  beforeEach(function() {
    view = getView({service: Em.Object.create()});
  });

  App.TestAliases.testAsComputedCountBasedMessage(getView(), 'nodeManagerText', 'service.nodeManagersTotal', '', Em.I18n.t('services.service.summary.viewHost'), Em.I18n.t('services.service.summary.viewHosts'));

  App.TestAliases.testAsComputedGt(getView(), 'hasManyYarnClients', 'service.installedClients', 1);

  App.TestAliases.testAsComputedFormatNa(getView(), '_nmActive', 'service.nodeManagersCountActive');
  App.TestAliases.testAsComputedFormatNa(getView(), '_nmLost', 'service.nodeManagersCountLost');
  App.TestAliases.testAsComputedFormatNa(getView(), '_nmUnhealthy', 'service.nodeManagersCountUnhealthy');
  App.TestAliases.testAsComputedFormatNa(getView(), '_nmRebooted', 'service.nodeManagersCountRebooted');
  App.TestAliases.testAsComputedFormatNa(getView(), '_nmDecom', 'service.nodeManagersCountDecommissioned');

  App.TestAliases.testAsComputedFormatNa(getView(), '_allocated', 'service.containersAllocated');
  App.TestAliases.testAsComputedFormatNa(getView(), '_pending', 'service.containersPending');
  App.TestAliases.testAsComputedFormatNa(getView(), '_reserved', 'service.containersReserved');

  App.TestAliases.testAsComputedFormatNa(getView(), '_appsSubmitted', 'service.appsSubmitted');
  App.TestAliases.testAsComputedFormatNa(getView(), '_appsRunning', 'service.appsRunning');
  App.TestAliases.testAsComputedFormatNa(getView(), '_appsPending', 'service.appsPending');
  App.TestAliases.testAsComputedFormatNa(getView(), '_appsCompleted', 'service.appsCompleted');
  App.TestAliases.testAsComputedFormatNa(getView(), '_appsKilled', 'service.appsKilled');
  App.TestAliases.testAsComputedFormatNa(getView(), '_appsFailed', 'service.appsFailed');

  App.TestAliases.testAsComputedFormatNa(getView(), '_queuesCountFormatted', 'service.queuesCount');

  describe("#nodeUptime", function() {

    beforeEach(function() {
      sinon.stub(App, 'dateTime').returns(10);
      sinon.stub(date, 'timingFormat').returns('11');
    });
    afterEach(function() {
      App.dateTime.restore();
      date.timingFormat.restore();
    });

    it("resourceManagerStartTime is 0", function() {
      view.set('service.resourceManagerStartTime', 0);
      view.propertyDidChange('nodeUptime');
      expect(view.get('nodeUptime')).to.be.equal(view.t('services.service.summary.notRunning'));
    });

    it("resourceManagerStartTime is -1", function() {
      view.set('service.resourceManagerStartTime', -1);
      view.propertyDidChange('nodeUptime');
      expect(view.get('nodeUptime')).to.be.equal(view.t('services.service.summary.notRunning'));
    });

    it("resourceManagerStartTime is 1", function() {
      view.set('service.resourceManagerStartTime', 1);
      view.propertyDidChange('nodeUptime');
      expect(view.get('nodeUptime')).to.be.equal(view.t('dashboard.services.uptime').format('11'));
      expect(date.timingFormat.calledWith(9)).to.be.true;
    });

    it("resourceManagerStartTime is 11", function() {
      view.set('service.resourceManagerStartTime', 11);
      view.propertyDidChange('nodeUptime');
      expect(view.get('nodeUptime')).to.be.equal(view.t('dashboard.services.uptime').format('11'));
      expect(date.timingFormat.calledWith(0)).to.be.true;
    });
  });

  describe("#didInsertElement()", function() {

    beforeEach(function() {
      sinon.stub(App, 'tooltip');
    });
    afterEach(function() {
      App.tooltip.restore();
    });

    it("App.tooltip should be called", function() {
      view.didInsertElement();
      expect(App.tooltip.calledTwice).to.be.true;
    });
  });

  describe("#willDestroyElement()", function() {
    var mock = {
      tooltip: Em.K
    };

    beforeEach(function() {
      sinon.stub(window, '$').returns(mock);
      sinon.stub(mock, 'tooltip');
    });
    afterEach(function() {
      window.$.restore();
      mock.tooltip.restore();
    });

    it("tooltip destroy should be called", function() {
      view.willDestroyElement();
      expect(mock.tooltip.calledWith('destroy')).to.be.true;
    });
  });

});
