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
require('models/host_component');

describe('App.HostComponent', function() {

  App.store.safeLoad(App.HostComponent, {
    id: 'COMP_host',
    component_name: 'COMP1'
  });
  var hc = App.HostComponent.find('COMP_host');

  describe('#getStatusesList', function() {
    it('allowed statuses', function() {
      var statuses = ["STARTED","STARTING","INSTALLED","STOPPING","INSTALL_FAILED","INSTALLING","UPGRADE_FAILED","UNKNOWN","DISABLED","INIT"];
      expect(App.HostComponentStatus.getStatusesList()).to.include.members(statuses);
      expect(statuses).to.include.members(App.HostComponentStatus.getStatusesList());
    });
  });

  describe('#isClient', function() {

    beforeEach(function () {
      sinon.stub(App.get('components.clients'), 'contains', Em.K);
      hc.propertyDidChange('isClient');
      hc.get('isClient');
    });

    afterEach(function () {
      App.get('components.clients').contains.restore();
    });

    it('components.clients is called with correct data', function() {
      expect(App.get('components.clients').contains.calledWith('COMP1')).to.be.true;
    });
  });

  describe('#isMaster', function() {

    beforeEach(function () {
      sinon.stub(App.get('components.masters'), 'contains', Em.K);
      hc.propertyDidChange('isMaster');
      hc.get('isMaster');
    });

    afterEach(function () {
      App.get('components.masters').contains.restore();
    });

    it('components.masters is called with correct data', function() {
      expect(App.get('components.masters').contains.calledWith('COMP1')).to.be.true;
    });
  });

  describe('#isSlave', function() {

    beforeEach(function () {
      sinon.stub(App.get('components.slaves'), 'contains', Em.K);
      hc.propertyDidChange('isSlave');
      hc.get('isSlave');
    });

    afterEach(function () {
      App.get('components.slaves').contains.restore();
    });

    it('components.slaves is called with correct data', function() {
      expect(App.get('components.slaves').contains.calledWith('COMP1')).to.be.true;
    });
  });

  describe('#isDeletable', function() {

    beforeEach(function () {
      sinon.stub(App.get('components.deletable'), 'contains', Em.K);
      hc.propertyDidChange('isDeletable');
      hc.get('isDeletable');
    });

    afterEach(function () {
      App.get('components.deletable').contains.restore();
    });

    it('components.deletable is called with correct data', function() {
      expect(App.get('components.deletable').contains.calledWith('COMP1')).to.be.true;
    });
  });

  App.TestAliases.testAsComputedIfThenElse(hc, 'passiveTooltip', 'isActive', '', Em.I18n.t('hosts.component.passive.mode'));

  App.TestAliases.testAsComputedExistsIn(hc, 'isRunning', 'workStatus', ['STARTED', 'STARTING']);

  describe('#isDecommissioning', function() {
    var mock = [];
    beforeEach(function () {
      sinon.stub(App.HDFSService, 'find', function () {
        return mock;
      })
    });
    afterEach(function () {
      App.HDFSService.find.restore();
    });
    it('component name is not DATANODE', function() {
      hc.propertyDidChange('isDecommissioning');
      expect(hc.get('isDecommissioning')).to.be.false;
    });
    it('component name is DATANODE but no HDFS service', function() {
      hc.set('componentName', 'DATANODE');
      hc.propertyDidChange('isDecommissioning');
      expect(hc.get('isDecommissioning')).to.be.false;
    });
    it('HDFS has no decommission DataNodes', function() {
      hc.set('componentName', 'DATANODE');
      mock.push(Em.Object.create({
        decommissionDataNodes: []
      }));
      hc.propertyDidChange('isDecommissioning');
      expect(hc.get('isDecommissioning')).to.be.false;
    });
    it('HDFS has decommission DataNodes', function() {
      hc.set('componentName', 'DATANODE');
      hc.set('hostName', 'host1');
      mock.clear();
      mock.push(Em.Object.create({
        decommissionDataNodes: [{hostName: 'host1'}]
      }));
      hc.propertyDidChange('isDecommissioning');
      expect(hc.get('isDecommissioning')).to.be.true;
    });
  });

  App.TestAliases.testAsComputedIfThenElse(hc, 'passiveTooltip', 'isActive', '', Em.I18n.t('hosts.component.passive.mode'));

  describe('#isActive', function() {
    it('passiveState is OFF', function() {
      hc.set('passiveState', "OFF");
      hc.propertyDidChange('isActive');
      expect(hc.get('isActive')).to.be.true;
    });
    it('passiveState is IMPLIED_FROM_HOST', function() {
      hc.set('passiveState', "IMPLIED_FROM_HOST");
      hc.set('host', {
        passiveState: 'OFF'
      });
      hc.propertyDidChange('isActive');
      expect(hc.get('isActive')).to.be.true;
    });
    it('passiveState is IMPLIED_FROM_SERVICE', function() {
      hc.set('passiveState', "IMPLIED_FROM_SERVICE");
      hc.set('service', {
        passiveState: 'OFF'
      });
      hc.propertyDidChange('isActive');
      expect(hc.get('isActive')).to.be.true;
    });
    it('passiveState is IMPLIED_FROM_SERVICE_AND_HOST', function() {
      hc.set('passiveState', "IMPLIED_FROM_SERVICE_AND_HOST");
      hc.set('host', {
        passiveState: 'OFF'
      });
      hc.set('service', {
        passiveState: 'OFF'
      });
      hc.propertyDidChange('isActive');
      expect(hc.get('isActive')).to.be.true;
    });
  });

  describe('#statusClass', function() {
    it('isActive is false', function() {
      hc.reopen({
        isActive: false
      });
      hc.propertyDidChange('statusClass');
      expect(hc.get('statusClass')).to.equal('icon-medkit');
    });
    it('isActive is true', function() {
      var status = 'INSTALLED';
      hc.set('isActive', true);
      hc.set('workStatus', status);
      hc.propertyDidChange('statusClass');
      expect(hc.get('statusClass')).to.equal(status);
    });
  });

  App.TestAliases.testAsComputedGetByKey(hc, 'statusIconClass', 'statusIconClassMap', 'statusClass', {defaultValue: '', map: {
    STARTED: App.healthIconClassGreen,
    STARTING: App.healthIconClassGreen,
    INSTALLED: App.healthIconClassRed,
    STOPPING: App.healthIconClassRed,
    UNKNOWN: App.healthIconClassYellow
  }});

  describe('#componentTextStatus', function () {
    before(function () {
      sinon.stub(App.HostComponentStatus, 'getTextStatus', Em.K);
    });
    after(function () {
      App.HostComponentStatus.getTextStatus.restore();
    });
    it('componentTextStatus should be changed', function () {
      var status = 'INSTALLED';
      hc.set('workStatus', status);
      hc.propertyDidChange('componentTextStatus');
      hc.get('componentTextStatus');
      expect(App.HostComponentStatus.getTextStatus.calledWith(status)).to.be.true;
    });
  });

  describe("#getCount", function () {
    var testCases = [
      {
        t: 'unknown component',
        data: {
          componentName: 'CC',
          type: 'totalCount',
          stackComponent: Em.Object.create()
        },
        result: 0
      },
      {
        t: 'master component',
        data: {
          componentName: 'C1',
          type: 'totalCount',
          stackComponent: Em.Object.create({componentCategory: 'MASTER'})
        },
        result: 3
      },
      {
        t: 'slave component',
        data: {
          componentName: 'C1',
          type: 'installedCount',
          stackComponent: Em.Object.create({componentCategory: 'SLAVE'})
        },
        result: 4
      },
      {
        t: 'client component',
        data: {
          componentName: 'C1',
          type: 'startedCount',
          stackComponent: Em.Object.create({componentCategory: 'CLIENT'})
        },
        result: 5
      },
      {
        t: 'client component, unknown type',
        data: {
          componentName: 'C1',
          type: 'unknownCount',
          stackComponent: Em.Object.create({componentCategory: 'CLIENT'})
        },
        result: 0
      }
    ];

    beforeEach(function () {
      this.mock = sinon.stub(App.StackServiceComponent, 'find');
      sinon.stub(App.MasterComponent, 'find').returns(Em.Object.create({totalCount: 3}));
      sinon.stub(App.SlaveComponent, 'find').returns(Em.Object.create({installedCount: 4}));
      sinon.stub(App.ClientComponent, 'find').returns(Em.Object.create({startedCount: 5, unknownCount: null}));
    });
    afterEach(function () {
      this.mock.restore();
      App.MasterComponent.find.restore();
      App.SlaveComponent.find.restore();
      App.ClientComponent.find.restore();
    });

    testCases.forEach(function (test) {
      it(test.t, function () {
        this.mock.returns(test.data.stackComponent);
        expect(App.HostComponent.getCount(test.data.componentName, test.data.type)).to.equal(test.result);
      });
    });
  });

  App.TestAliases.testAsComputedExistsIn(hc, 'isNotInstalled', 'workStatus', ['INIT', 'INSTALL_FAILED']);

  App.TestAliases.testAsComputedTruncate(hc, 'serviceDisplayName', 'service.displayName', 14, 11);
  App.TestAliases.testAsComputedTruncate(hc, 'getDisplayName', 'displayName', 30, 25);
  App.TestAliases.testAsComputedTruncate(hc, 'getDisplayNameAdvanced', 'displayNameAdvanced', 30, 25);

  describe("#serviceDisplayName",function(){
    var testCases = [
      {
        testName: 'for service.displayName of length < 14',
        serviceDisplayName: 'abc',
        result: 'abc'
      },
      {
        testName:'for service.displayName of length = 14',
        serviceDisplayName: '12345678901234',
        result: '12345678901234'
      },
      {
        testName:'for service.displayName of length > 14',
        serviceDisplayName: '123456789012345',
        result: '12345678901...'
      }
    ];

    testCases.forEach(function(test){
      it(test.testName, function(){
        hc.set('service',Em.Object.create({displayName:test.serviceDisplayName}));
        expect(hc.get('serviceDisplayName')).to.equal(test.result);
      });
    });
  });
});
