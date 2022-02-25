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
var uiEffects = require('utils/ui_effects');
var testHelpers = require('test/helpers');

require('mixins/main/host/details/host_components/decommissionable');

var decommissionable,
  view,
  textCases = [
    {
      available: true,
      text: Em.I18n.t('common.decommission')
    },
    {
      available: false,
      text: Em.I18n.t('common.recommission')
    }
  ];

describe('App.Decommissionable', function () {

  beforeEach(function () {
    decommissionable = Em.Object.create(App.Decommissionable);
    decommissionable.set('$', Em.K);
  });

  describe('#decommissionView.text', function () {

    beforeEach(function () {
      view = decommissionable.decommissionView.create();
      view.reopen({
        parentView: decommissionable
      });
    });

    textCases.forEach(function (item) {
      it('should be ' + item.text, function () {
        view.set('parentView.isComponentDecommissionAvailable', item.available);
        expect(view.get('text')).to.equal(item.text);
      });
    });

  });

  describe("#isComponentDecommissionDisable", function() {

    beforeEach(function() {
      decommissionable.set('componentForCheckDecommission', 'MC1');
      decommissionable.set('content', Em.Object.create({service: Em.Object.create()}));
    });

    it("masterComponent is absent, service workStatus is STARTED", function() {
      decommissionable.set('content.service.hostComponents', []);
      decommissionable.set('content.service.workStatus', App.HostComponentStatus.started);
      expect(decommissionable.get('isComponentDecommissionDisable')).to.be.false;
    });

    it("masterComponent is absent, service workStatus is STOPPED", function() {
      decommissionable.set('content.service.hostComponents', []);
      decommissionable.set('content.service.workStatus', App.HostComponentStatus.stopped);
      expect(decommissionable.get('isComponentDecommissionDisable')).to.be.true;
    });

    it("masterComponent is present, component workStatus is STARTED", function() {
      decommissionable.set('content.service.hostComponents', [Em.Object.create({
        componentName: 'MC1',
        workStatus: App.HostComponentStatus.started
      })]);
      decommissionable.set('content.service.workStatus', App.HostComponentStatus.started);
      expect(decommissionable.get('isComponentDecommissionDisable')).to.be.false;
    });

    it("masterComponent is present, component workStatus is STOPPED", function() {
      decommissionable.set('content.service.hostComponents', [Em.Object.create({
        componentName: 'MC1',
        workStatus: App.HostComponentStatus.stopped
      })]);
      decommissionable.set('content.service.workStatus', App.HostComponentStatus.started);
      expect(decommissionable.get('isComponentDecommissionDisable')).to.be.true;
    });
  });

  describe("#isRestartableComponent", function() {

    beforeEach(function() {
      sinon.stub(App, 'get').returns(['C2']);
    });
    afterEach(function() {
      App.get.restore();
    });

    var testCases = [
      {
        input: {
          isDecomissionable: false,
          componentName: 'C1'
        },
        expected: false
      },
      {
        input: {
          isDecomissionable: true,
          componentName: 'C1'
        },
        expected: false
      },
      {
        input: {
          isDecomissionable: false,
          componentName: 'C2'
        },
        expected: false
      },
      {
        input: {
          isDecomissionable: true,
          componentName: 'C2'
        },
        expected: true
      }
    ];

    testCases.forEach(function(test) {
      it("isDecomissionable = " + test.input.isDecomissionable + "; " +
         "componentName=" + test.input.componentName, function() {
        decommissionable.setProperties({
          isComponentDecommissionAvailable: test.input.isDecomissionable,
          content: Em.Object.create({
            componentName: test.input.componentName
          })
        });
        expect(decommissionable.get('isRestartableComponent')).to.equal(test.expected);
      });
    }, this);
  });

  describe("#decommissionTooltipMessage", function() {

    beforeEach(function() {
      sinon.stub(App.format, 'role').returns('role');
    });
    afterEach(function() {
      App.format.role.restore();
    });

    var testCases = [
      {
        input: {
          isComponentDecommissionDisable: false,
          isComponentRecommissionAvailable: false,
          isComponentDecommissionAvailable: false
        },
        expected: ''
      },
      {
        input: {
          isComponentDecommissionDisable: true,
          isComponentRecommissionAvailable: false,
          isComponentDecommissionAvailable: false
        },
        expected: ''
      },
      {
        input: {
          isComponentDecommissionDisable: true,
          isComponentRecommissionAvailable: true,
          isComponentDecommissionAvailable: false
        },
        expected: Em.I18n.t('hosts.decommission.tooltip.warning').format(Em.I18n.t('common.recommission'), 'role')
      },
      {
        input: {
          isComponentDecommissionDisable: true,
          isComponentRecommissionAvailable: false,
          isComponentDecommissionAvailable: true
        },
        expected: Em.I18n.t('hosts.decommission.tooltip.warning').format(Em.I18n.t('common.decommission'), 'role')
      }
    ];

    testCases.forEach(function(test) {
      it("isComponentDecommissionDisable = " + test.input.isComponentDecommissionDisable + '; ' +
         "isComponentRecommissionAvailable = " + test.input.isComponentRecommissionAvailable + '; ' +
         "isComponentDecommissionAvailable = " + test.input.isComponentDecommissionAvailable
        , function() {
        decommissionable.reopen({
          isComponentDecommissionDisable: test.input.isComponentDecommissionDisable,
          isComponentRecommissionAvailable: test.input.isComponentRecommissionAvailable,
          isComponentDecommissionAvailable: test.input.isComponentDecommissionAvailable
        });
        expect(decommissionable.get('decommissionTooltipMessage')).to.equal(test.expected);
      });
    }, this);
  });

  describe("#loadComponentDecommissionStatus()", function() {

    beforeEach(function() {
      sinon.stub(decommissionable, 'getDesiredAdminState');
    });
    afterEach(function() {
      decommissionable.getDesiredAdminState.restore();
    });

    it("getDesiredAdminState should be called", function() {
      decommissionable.loadComponentDecommissionStatus();
      expect(decommissionable.getDesiredAdminState.calledOnce).to.be.true;
    });
  });

  describe("#getDesiredAdminStateSuccessCallback()", function() {

    beforeEach(function() {
      sinon.stub(decommissionable, 'setDesiredAdminState');
    });
    afterEach(function() {
      decommissionable.setDesiredAdminState.restore();
    });

    it("desired_admin_state is null", function() {
      var response = {
        HostRoles: {
          desired_admin_state: null
        }
      };

      expect(decommissionable.getDesiredAdminStateSuccessCallback(response)).to.be.null;
      expect(decommissionable.setDesiredAdminState.called).to.be.false;
    });

    it("setDesiredAdminState should be called", function() {
      var response = {
        HostRoles: {
          desired_admin_state: "STATUS"
        }
      };

      expect(decommissionable.getDesiredAdminStateSuccessCallback(response)).to.equal('STATUS');
      expect(decommissionable.setDesiredAdminState.calledWith('STATUS')).to.be.true;
    });
  });

  describe("#getDecommissionStatus()", function() {

    it("App.ajax.send should be called", function() {
      decommissionable.setProperties({
        componentForCheckDecommission: 'C1',
        content: Em.Object.create({
          hostName: 'host1',
          service: Em.Object.create({
            serviceName: 'S1'
          })
        })
      });
      decommissionable.getDecommissionStatus();
      var args = testHelpers.findAjaxRequest('name', 'host.host_component.decommission_status');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(decommissionable);
      expect(args[0].data).to.be.eql({
        hostName: 'host1',
        componentName: 'C1',
        serviceName: 'S1'
      });
    });
  });

  describe("#getDecommissionStatusSuccessCallback()", function() {

    beforeEach(function() {
      sinon.stub(decommissionable, 'setDecommissionStatus');
    });
    afterEach(function() {
      decommissionable.setDecommissionStatus.restore();
    });

    it("ServiceComponentInfo is null", function() {
      var response = {
        ServiceComponentInfo: null
      };

      expect(decommissionable.getDecommissionStatusSuccessCallback(response)).to.be.null;
      expect(decommissionable.setDecommissionStatus.called).to.be.false;
    });

    it("setDecommissionStatus should be called", function() {
      var response = {
        ServiceComponentInfo: {},
        host_components: [{
          HostRoles: {
            state: 'STATE'
          }
        }]
      };
      var statusObject = {
        component_state: 'STATE'
      };

      expect(decommissionable.getDecommissionStatusSuccessCallback(response)).to.eql(statusObject);
      expect(decommissionable.setDecommissionStatus.calledWith(statusObject)).to.be.true;
    });
  });

  describe("#setStatusAs()", function() {

    beforeEach(function() {
      sinon.stub(decommissionable, 'startBlinking');
    });
    afterEach(function() {
      decommissionable.startBlinking.restore();
    });

    describe("#INSERVICE status", function() {
      beforeEach(function() {
        decommissionable.set('isStart', false);
        decommissionable.setStatusAs('INSERVICE');
      });

      it("isComponentRecommissionAvailable should be false", function() {
        expect(decommissionable.get('isComponentRecommissionAvailable')).to.be.false;
      });
      it("isComponentDecommissioning should be false", function() {
        expect(decommissionable.get('isComponentDecommissioning')).to.be.false;
      });
      it("isComponentDecommissionAvailable should be false", function() {
        expect(decommissionable.get('isComponentDecommissionAvailable')).to.be.false;
      });
    });

    describe("#DECOMMISSIONING status", function() {
      beforeEach(function() {
        decommissionable.setStatusAs('DECOMMISSIONING')
      });

      it("isComponentRecommissionAvailable should be true", function() {
        expect(decommissionable.get('isComponentRecommissionAvailable')).to.be.true;
      });
      it("isComponentDecommissioning should be true", function() {
        expect(decommissionable.get('isComponentDecommissioning')).to.be.true;
      });
      it("isComponentDecommissionAvailable should be false", function() {
        expect(decommissionable.get('isComponentDecommissionAvailable')).to.be.false;
      });
    });

    describe("#DECOMMISSIONED status", function() {
      beforeEach(function() {
        decommissionable.setStatusAs('DECOMMISSIONED')
      });

      it("isComponentRecommissionAvailable should be true", function() {
        expect(decommissionable.get('isComponentRecommissionAvailable')).to.be.true;
      });
      it("isComponentDecommissioning should be false", function() {
        expect(decommissionable.get('isComponentDecommissioning')).to.be.false;
      });
      it("isComponentDecommissionAvailable should be false", function() {
        expect(decommissionable.get('isComponentDecommissionAvailable')).to.be.false;
      });
    });

    describe("#RS_DECOMMISSIONED status", function() {
      beforeEach(function() {
        decommissionable.set('isStart', false);
        decommissionable.setStatusAs('RS_DECOMMISSIONED');
      });

      it("isComponentRecommissionAvailable should be true", function() {
        expect(decommissionable.get('isComponentRecommissionAvailable')).to.be.true;
      });
      it("isComponentDecommissioning should be false", function() {
        expect(decommissionable.get('isComponentDecommissioning')).to.be.false;
      });
      it("isComponentDecommissionAvailable should be false", function() {
        expect(decommissionable.get('isComponentDecommissionAvailable')).to.be.false;
      });
    });
  });


  describe("#doBlinking()", function() {

    beforeEach(function() {
      sinon.stub(decommissionable, 'pulsate');
      sinon.spy(decommissionable, 'doBlinking');
      sinon.stub(decommissionable, 'startBlinking');
    });
    afterEach(function() {
      decommissionable.pulsate.restore();
      decommissionable.doBlinking.restore();
      decommissionable.startBlinking.restore();
    });

    it("INSTALLED status", function() {
      decommissionable.setProperties({
        workStatus: 'INSTALLED',
        content: Em.Object.create()
      });
      decommissionable.doBlinking();
      expect(decommissionable.pulsate.called).to.be.false;
    });

    it("STARTED status, content is null", function() {
      decommissionable.setProperties({
        workStatus: 'STARTED',
        content: null
      });
      decommissionable.doBlinking();
      expect(decommissionable.pulsate.called).to.be.false;
    });

    it("STARTED status, isDecommissioning = false", function() {
      decommissionable.setProperties({
        workStatus: 'STARTED',
        content: Em.Object.create(),
        isDecommissioning: false
      });
      decommissionable.doBlinking();
      expect(decommissionable.pulsate.called).to.be.false;
    });

    it("STARTED status, isDecommissioning = true, isBlinking = true", function() {
      decommissionable.setProperties({
        workStatus: 'STARTED',
        isDecommissioning: true,
        isBlinking: true,
        content: Em.Object.create()
      });
      decommissionable.doBlinking();
      expect(decommissionable.pulsate.called).to.be.false;
    });

    it("STARTED status, isDecommissioning = true, isBlinking = false", function() {
      decommissionable.setProperties({
        workStatus: 'STARTED',
        isDecommissioning: true,
        isBlinking: false,
        content: Em.Object.create()
      });
      decommissionable.doBlinking();
      expect(decommissionable.get('isBlinking')).to.be.true;
      expect(decommissionable.pulsate.calledOnce).to.be.true;
    });
  });

  describe("#pulsate()", function() {

    beforeEach(function() {
      sinon.stub(uiEffects, 'pulsate', function(node, time, callback) {
        callback();
      });
      sinon.stub(decommissionable, 'doBlinking');
      decommissionable.pulsate();
    });
    afterEach(function() {
      uiEffects.pulsate.restore();
      decommissionable.doBlinking.restore();
    });

    it("uiEffects.pulsate should be called", function() {
      expect(uiEffects.pulsate.calledOnce).to.be.true;
    });

    it("doBlinking should be called", function() {
      expect(decommissionable.doBlinking.calledOnce).to.be.true;
    });

    it("isBlinking should be false", function() {
      expect(decommissionable.get('isBlinking')).to.be.false;
    });
  });

  describe("#startBlinking()", function() {

    beforeEach(function() {
      sinon.stub(decommissionable, '$').returns({stop: Em.K, css: Em.K});
      sinon.stub(decommissionable, 'doBlinking');
    });
    afterEach(function() {
      decommissionable.$.restore();
      decommissionable.doBlinking.restore();
    });

    it("doBlinking should be called", function() {
      decommissionable.startBlinking();
      expect(decommissionable.doBlinking.calledOnce).to.be.true;
    });
  });
});
