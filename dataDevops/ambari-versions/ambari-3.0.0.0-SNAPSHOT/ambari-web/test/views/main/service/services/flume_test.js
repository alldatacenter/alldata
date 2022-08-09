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
require('/views/main/service/services/flume');

describe('App.MainDashboardServiceFlumeView', function () {
  var view;

  beforeEach(function() {
    view = App.MainDashboardServiceFlumeView.create({
      service: Em.Object.create({
        agents: []
      })
    });
  });

  describe("#content", function() {

    it("should return content", function() {
      view.set('service.agents', [
        {hostName: 'host1'},
        {hostName: 'host2'},
        {hostName: 'host2'}
      ]);
      view.propertyDidChange('content');
      expect(view.get('content').mapProperty('hostName')).to.be.eql(['host1', 'host2']);
    });
  });

  describe("#summaryHeader", function() {

    beforeEach(function() {
      this.mock = sinon.stub(App.FlumeService, 'find');
    });
    afterEach(function() {
      this.mock.restore();
    });

    it("single host", function() {
      this.mock.returns([
        Em.Object.create({
          agents: [{}]
        })
      ]);
      view.set('service.flumeHandlersTotal', 1);
      view.propertyDidChange('summaryHeader');
      expect(view.get('summaryHeader')).to.be.equal(view.t("dashboard.services.flume.summary.title").format(1, "", 1, ""));
    });

    it("multiple hosts", function() {
      this.mock.returns([
        Em.Object.create({
          agents: [{}, {}]
        })
      ]);
      view.set('service.flumeHandlersTotal', 2);
      view.propertyDidChange('summaryHeader');
      expect(view.get('summaryHeader')).to.be.equal(view.t("dashboard.services.flume.summary.title").format(2, "s", 2, "s"));
    });
  });

  describe("#didInsertElement()", function() {
    var mock = {
      on: function(a1, a2, callback) {
        callback();
      }
    };

    beforeEach(function() {
      sinon.stub(view, 'filter');
      sinon.stub(view, 'setDropdownPosition');
      sinon.stub(mock, 'on');
      sinon.stub(view, '$').returns(mock);
      view.didInsertElement();
    });
    afterEach(function() {
      view.filter.restore();
      view.setDropdownPosition();
      mock.on.restore();
      view.$.restore();
    });

    it("filter should be called", function() {
      expect(view.filter.calledOnce).to.be.true;
    });

    it("setDropdownPosition should be called", function() {
      expect(view.filter.calledOnce).to.be.true;
    });
  });

  describe("#willDestroyElement()", function() {
    var mock = {
      off: Em.K
    };

    beforeEach(function() {
      sinon.stub(mock, 'off');
      sinon.stub(view, '$').returns(mock);
      view.willDestroyElement();
    });
    afterEach(function() {
      mock.off.restore();
      view.$.restore();
    });

    it("off should be called", function() {
      expect(mock.off.calledOnce).to.be.true;
    });
  });

  describe("#setActionsDropdownClasses()", function() {

    it("should disable dropdown", function() {
      view.reopen({
        content: [
          Em.Object.create({
            agents: [
              Em.Object.create({status: 'RUNNING'}),
              Em.Object.create({status: 'NOT_RUNNING'})
            ]
          })
        ]
      });
      view.setActionsDropdownClasses();
      var agents = view.get('content.0.agents');
      expect(agents[0].get('isStartAgentDisabled')).to.be.true;
      expect(agents[0].get('isStopAgentDisabled')).to.be.false;
      expect(agents[1].get('isStartAgentDisabled')).to.be.false;
      expect(agents[1].get('isStopAgentDisabled')).to.be.true;
    });
  });

  describe("#updateFlumeAgentsCount()", function() {

    it("should update flumeAgentsCount", function() {
      view.set('service', Em.Object.create({
        agents: [{}]
      }));
      view.updateFlumeAgentsCount();
      expect(view.get('flumeAgentsCount')).to.be.equal(1);
    });
  });

  describe("#showAgentInfo()", function() {

    var host;

    beforeEach(function() {
      host = {hostName: 'host1'};
      sinon.stub(view, 'setAgentMetrics');
      view.showAgentInfo(host);
    });
    afterEach(function() {
      view.setAgentMetrics.restore();
    });

    it('setAgentMetrics should be called', function() {
      expect(view.setAgentMetrics.calledWith(host)).to.be.true;
    });

    it('proper host should be selected', function() {
      expect(view.get('selectedHost')).to.be.eql(host);
    });

    it('proper row should be highlighted', function() {
      expect(Em.get(host, 'isActive')).to.be.true;
    });
  });
});