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
require('views/main/service/service');

describe('App.MainDashboardServiceHealthView', function () {
  var view;

  beforeEach(function() {
    view = App.MainDashboardServiceHealthView.create({
      controller: Em.Object.create({
        content: Em.Object.create()
      }),
      service: Em.Object.create()
    });
  });

  describe('#updateToolTip', function() {

    it('should set data-original-title', function() {
      view.set('service.toolTipContent', 'foo');
      view.updateToolTip();
      expect(view.get('data-original-title')).to.be.equal('foo');
    });
  });

  describe('#startBlink', function() {

    it('blink should be true', function() {
      view.startBlink();
      expect(view.get('blink')).to.be.true;
    });
  });

  describe('#stopBlink', function() {

    it('blink should be false', function() {
      view.stopBlink();
      expect(view.get('blink')).to.be.false;
    });
  });

  describe('#healthStatus', function() {
    beforeEach(function() {
      this.mock = sinon.stub(App, 'get').returns([]);
      sinon.stub(view, 'stopBlink');
      sinon.stub(view, 'startBlink');
      view.set('service.serviceName', 'S1');
      view.set('service.passiveState', 'OFF');
    });
    afterEach(function() {
      view.stopBlink.restore();
      view.startBlink.restore();
      App.get.restore();
    });

    it('client only service', function() {
      this.mock.returns(['S1']);
      expect(view.get('healthStatus')).to.be.equal('glyphicon glyphicon-blackboard');
    });
    it('service in passive state', function() {
      view.set('service.passiveState', 'ON');
      expect(view.get('healthStatus')).to.be.equal('icon-medkit');
    });
    it('stopBlink should be called when service has green status', function() {
      view.set('service.healthStatus', 'green');
      expect(view.get('healthStatus')).to.be.equal('health-status-' + App.Service.Health.live);
      expect(view.stopBlink.calledOnce).to.be.true;
    });
    it('startBlink should be called when service has green-blinking status', function() {
      view.set('service.healthStatus', 'green-blinking');
      expect(view.get('healthStatus')).to.be.equal('health-status-' + App.Service.Health.live);
      expect(view.startBlink.calledOnce).to.be.true;
    });
    it('startBlink should be called when service has red-blinking status', function() {
      view.set('service.healthStatus', 'red-blinking');
      expect(view.get('healthStatus')).to.be.equal('health-status-' + App.Service.Health.dead);
      expect(view.startBlink.calledOnce).to.be.true;
    });
    it('service has yellow status', function() {
      view.set('service.healthStatus', 'yellow');
      expect(view.get('healthStatus')).to.be.equal('health-status-' + App.Service.Health.unknown);
    });
    it('stopBlink should be called when service has empty status', function() {
      view.set('service.healthStatus', '');
      expect(view.get('healthStatus')).to.be.equal('health-status-' + App.Service.Health.dead);
      expect(view.stopBlink.calledOnce).to.be.true;
    });
  });

  describe('#healthStatusClass', function() {
    beforeEach(function() {
      this.mock = sinon.stub(App, 'get').returns([]);
      view.set('service.serviceName', 'S1');
      view.set('service.passiveState', 'OFF');
    });
    afterEach(function() {
      App.get.restore();
    });

    it('should be empty when client only service', function() {
      this.mock.returns(['S1']);
      expect(view.get('healthStatusClass')).to.be.empty;
    });
    it('should be empty when service in passive state', function() {
      view.set('service.passiveState', 'ON');
      expect(view.get('healthStatusClass')).to.be.empty;
    });
    it('should be empty when healthStatus is empty', function() {
      view.set('service.healthStatus', '');
      expect(view.get('healthStatusClass')).to.be.empty;
    });
    it('service has green status', function() {
      view.set('service.healthStatus', 'green');
      expect(view.get('healthStatusClass')).to.be.equal(App.healthIconClassGreen);
    });
    it('service has red status', function() {
      view.set('service.healthStatus', 'red');
      expect(view.get('healthStatusClass')).to.be.equal(App.healthIconClassRed);
    });
    it('service has yellow status', function() {
      view.set('service.healthStatus', 'yellow');
      expect(view.get('healthStatusClass')).to.be.equal(App.healthIconClassYellow);
    });
  });

  describe('#didInsertElement', function() {
    beforeEach(function() {
      sinon.stub(view, 'updateToolTip');
      sinon.stub(App, 'tooltip');
      sinon.stub(view, 'doBlink');
      view.didInsertElement();
    });
    afterEach(function() {
      view.updateToolTip.restore();
      App.tooltip.restore();
      view.doBlink.restore();
    });

    it('updateToolTip should be called', function() {
      expect(view.updateToolTip.calledOnce).to.be.true;
    });
    it('App.tooltip should be called', function() {
      expect(App.tooltip.calledOnce).to.be.true;
    });
    it('doBlink should be called', function() {
      expect(view.doBlink.calledOnce).to.be.true;
    });
  });
});

describe('App.ComponentLiveTextView', function () {
  var view;

  beforeEach(function() {
    view = App.ComponentLiveTextView.create();
  });

  describe('#color', function() {
    it('color should be true when liveComponents equal to 0 and totalComponents not equal to 0', function() {
      view.setProperties({
        liveComponents: 0,
        totalComponents: 1
      });
      expect(view.get('color')).to.be.true;
    });
    it('color should be false when liveComponents not equal to 0 and totalComponents not equal to 0', function() {
      view.setProperties({
        liveComponents: 1,
        totalComponents: 1
      });
      expect(view.get('color')).to.be.false;
    });
    it('color should be false when liveComponents equal 0 and totalComponents equal to 0', function() {
      view.setProperties({
        liveComponents: 0,
        totalComponents: 0
      });
      expect(view.get('color')).to.be.false;
    });
  });
});

describe('App.MainDashboardServiceView', function () {
  var view;

  function getView() {
    return App.MainDashboardServiceView.create({
      controller: Em.Object.create(),
      service: Em.Object.create()
    });
  }

  beforeEach(function() {
    view = getView();
  });

  describe('#data', function() {

    it('should return data', function() {
      view.set('serviceName', 'S1');
      view.set('controller.data', {
        S1: {id: 'S1'}
      });
      expect(view.get('data')).to.be.eql({id: 'S1'});
    });
  });

  describe('#clients', function() {

    it('should return clients', function() {
      view.set('serviceName', 'HDFS');
      view.set('service.hostComponents', [
        Em.Object.create({
          isClient: true
        }),
        Em.Object.create({
          isClient: true
        })
      ]);
      expect(view.get('clients')).to.be.eql({
        title: view.t('dashboard.services.hdfs.clients').format(2),
        component: Em.Object.create({
          isClient: true
        })
      });
    });
  });

  describe('#isServiceComponentCreated', function() {
    beforeEach(function() {
      sinon.stub(App.MasterComponent, 'find').returns([{componentName: 'M1'}]);
      sinon.stub(App.ClientComponent, 'find').returns([{componentName: 'C1'}]);
      sinon.stub(App.SlaveComponent, 'find').returns([{componentName: 'S1'}]);
    });
    afterEach(function() {
      App.MasterComponent.find.restore();
      App.ClientComponent.find.restore();
      App.SlaveComponent.find.restore();
    });

    it('client should be created', function() {
      expect(view.isServiceComponentCreated('C1')).to.be.true;
    });
    it('master should be created', function() {
      expect(view.isServiceComponentCreated('M1')).to.be.true;
    });
    it('slave should be created', function() {
      expect(view.isServiceComponentCreated('S1')).to.be.true;
    });
  });

  App.TestAliases.testAsComputedGt(getView(), 'hasMultipleMasterGroups', 'parentView.mastersObj.length', 1);
});
