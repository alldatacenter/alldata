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
require('views/main/host/details/host_component_view');

var hostComponentView;

function getView() {
  return App.HostComponentView.create({
    startBlinking: function(){},
    doBlinking: function(){},
    getDesiredAdminState: function(){return $.ajax({});},
    content: Em.Object.create({
      componentName: 'component'
    }),
    controller: Em.Object.create({
      installClients: sinon.spy()
    })
  });
}

describe('App.HostComponentView', function() {

  beforeEach(function() {
    hostComponentView = getView();
  });

  App.TestAliases.testAsComputedNotEqual(getView(), 'isRestartComponentDisabled', 'workStatus', App.HostComponentStatus.started);

  describe('#isDisabled', function() {

    var tests = Em.A([
      {
        parentView: {content: {healthClass: 'health-status-DEAD-YELLOW'}},
        noActionAvailable: '',
        isRestartComponentDisabled: true,
        e: true
      },
      {
        parentView: {content: {healthClass: 'another-class'}},
        noActionAvailable: '',
        isRestartComponentDisabled: true,
        e: false
      },
      {
        parentView: {content: {healthClass: 'another-class'}},
        noActionAvailable: 'hidden',
        isRestartComponentDisabled: true,
        e: true
      },
      {
        parentView: {content: {healthClass: 'another-class'}},
        noActionAvailable: 'hidden',
        isRestartComponentDisabled: false,
        e: false
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        hostComponentView = App.HostComponentView.create({
          startBlinking: function(){},
          doBlinking: function(){},
          parentView: test.parentView,
          noActionAvailable: test.noActionAvailable,
          isRestartComponentDisabled: test.isRestartComponentDisabled
        });
        expect(hostComponentView.get('isDisabled')).to.equal(test.e);
      });
    });

  });

  App.TestAliases.testAsComputedEqual(getView(), 'isUpgradeFailed', 'workStatus', App.HostComponentStatus.upgrade_failed);

  App.TestAliases.testAsComputedEqual(getView(), 'isInstallFailed', 'workStatus', App.HostComponentStatus.install_failed);

  App.TestAliases.testAsComputedEqual(getView(), 'isStop', 'workStatus', App.HostComponentStatus.stopped);

  App.TestAliases.testAsComputedEqual(getView(), 'isInstalling', 'workStatus', App.HostComponentStatus.installing);

  App.TestAliases.testAsComputedEqual(getView(), 'isInit', 'workStatus', App.HostComponentStatus.init);

  App.TestAliases.testAsComputedExistsIn(getView(), 'isInProgress', 'workStatus', [App.HostComponentStatus.stopping, App.HostComponentStatus.starting]);

  App.TestAliases.testAsComputedExistsIn(getView(), 'withoutActions', 'workStatus', [App.HostComponentStatus.starting, App.HostComponentStatus.stopping, App.HostComponentStatus.unknown, App.HostComponentStatus.disabled]);

  App.TestAliases.testAsComputedExistsIn(getView(), 'isStart', 'workStatus', [App.HostComponentStatus.started, App.HostComponentStatus.starting]);

  App.TestAliases.testAsComputedIfThenElse(getView(), 'noActionAvailable', 'withoutActions', 'hidden', '');

  App.TestAliases.testAsComputedEqual(getView(), 'isActive', 'content.passiveState', 'OFF');

  describe('#isDeleteComponentDisabled', function() {
    var configs=[
    {
      properties: {
        'hive_database': 'Existing MYSQL Database'
      },
      tag: 'version2',
      type: 'hive-env'
    }
    ];
    beforeEach(function() {
      this.mock = sinon.stub(App.StackServiceComponent, 'find');
      sinon.stub(App.HostComponent, 'getCount').returns(1);
    });
    afterEach(function() {
      this.mock.restore();
      App.HostComponent.getCount.restore();
    });

    it('delete is disabled because min cardinality 1', function() {
      this.mock.returns(Em.Object.create({minToInstall: 1}));
      hostComponentView.get('content').set('componentName', 'C1');
      hostComponentView.propertyDidChange('isDeleteComponentDisabled');
      expect(hostComponentView.get('isDeleteComponentDisabled')).to.be.true;
    });

    it('delete is disabled because min cardinality 0 and status INSTALLED', function() {
      this.mock.returns(Em.Object.create({minToInstall: 0}));
      hostComponentView.get('content').set('workStatus', 'INIT');
      hostComponentView.propertyDidChange('isDeleteComponentDisabled');
      expect(hostComponentView.get('isDeleteComponentDisabled')).to.be.false;
    });

    it('delete is enabled because min cardinality 0 and status STARTED', function() {
      this.mock.returns(Em.Object.create({minToInstall: 0}));
      hostComponentView.get('content').set('workStatus', 'STARTED');
      hostComponentView.propertyDidChange('isDeleteComponentDisabled');
      expect(hostComponentView.get('isDeleteComponentDisabled')).to.be.true;
    });
    
    it('delete is enabled because mysql server is stopped and hive is using external database', function() {
      App.db.setConfigs(configs);
      this.mock.returns(Em.Object.create({minToInstall: 0}));
      hostComponentView.get('content').set('componentName', 'MYSQL_SERVER');
      hostComponentView.get('content').set('workStatus', 'STOPPED');
      hostComponentView.propertyDidChange('isDeleteComponentDisabled');
      expect(hostComponentView.get('isDeleteComponentDisabled')).to.be.true;
    });
  });

  describe('#statusClass', function() {

    var tests = Em.A([
      {
        workStatus: App.HostComponentStatus.install_failed,
        passiveState: 'OFF',
        e: 'health-status-color-red glyphicon glyphicon-cog'
      },
      {
        workStatus: App.HostComponentStatus.installing,
        passiveState: 'OFF',
        e: 'health-status-color-blue glyphicon glyphicon-cog'
      },
      {
        workStatus: 'STARTED',
        passiveState: 'ON',
        e: 'health-status-started'
      },
      {
        workStatus: 'STARTED',
        passiveState: 'IMPLIED',
        e: 'health-status-started'
      },
      {
        workStatus: 'STARTED',
        passiveState: 'OFF',
        e: 'health-status-started'
      },
      {
        workStatus: App.HostComponentStatus.stopped,
        passiveState: 'OFF',
        isClient: true,
        e: 'health-status-started'
      }
    ]);

    tests.forEach(function(test) {
      it(test.workStatus + ' ' + test.passiveState, function() {
        hostComponentView = App.HostComponentView.create({
          startBlinking: Em.K,
          doBlinking: Em.K,
          getDesiredAdminState: function(){return $.ajax({});},
          content: Em.Object.create()
        });
        hostComponentView.get('content').set('isClient', test.isClient);
        hostComponentView.get('content').set('workStatus', test.workStatus);
        hostComponentView.get('content').set('passiveState', test.passiveState);
        expect(hostComponentView.get('statusClass')).to.equal(test.e);
      });
    });

  });

  describe('#statusIconClass', function() {
    var tests = Em.A([
      {s: 'health-status-started', e: App.healthIconClassGreen},
      {s: 'health-status-starting', e: App.healthIconClassGreen},
      {s: 'health-status-installed', e: App.healthIconClassRed},
      {s: 'health-status-stopping', e: App.healthIconClassRed},
      {s: 'health-status-unknown', e: App.healthIconClassYellow},
      {s: 'health-status-DEAD-ORANGE', e: App.healthIconClassOrange},
      {s: 'other', e: ''}
    ]);

    tests.forEach(function(test) {
      it(test.s, function() {
        hostComponentView.reopen({statusClass: test.s});
        expect(hostComponentView.get('statusIconClass')).to.equal(test.e);
      })
    });
  });

  describe('#slaveCustomCommands', function() {

    var content = [
      {
        componentName: 'SLAVE_COMPONENT',
        hostName: '01'
      },
      {
        componentName: 'NOT_SLAVE_COMPONENT',
        hostName: '02'
      }
    ];
    before(function() {
      sinon.stub(App.StackServiceComponent, 'find', function() {
        return Em.Object.create({
          componentName: 'SLAVE_COMPONENT',
          isSlave: true,
          customCommands: ['SLAVE_CUSTOM_COMMAND']
        });
      });
     sinon.stub(App.HostComponentActionMap, 'getMap', function () {
        return {
          SLAVE_CUSTOM_COMMAND: {
            customCommand: 'SLAVE_CUSTOM_COMMAND',
            cssClass: 'glyphicon-play-circle',
            label: 'Custom Command',
            context: 'Custom Command',
            isHidden: false,
            disabled: false
          }
        }
      });
    });

    it('Should get custom commands for slaves', function() {
      hostComponentView.set('content', content);
      expect(hostComponentView.get('customCommands')).to.have.length(1);
    });

    after(function() {
      App.StackServiceComponent.find.restore();
      App.HostComponentActionMap.getMap.restore();
    });
  });

  describe('#getCustomCommandLabel', function() {

    beforeEach(function () {
      sinon.stub(App.HostComponentActionMap, 'getMap', function () {
        return {
          MASTER_CUSTOM_COMMAND: {
            action: 'executeCustomCommand',
            cssClass: 'glyphicon-play-circle',
            isHidden: false,
            disabled: false
          },
          REFRESHQUEUES: {
            action: 'refreshYarnQueues',
            customCommand: 'REFRESHQUEUES',
            context : Em.I18n.t('services.service.actions.run.yarnRefreshQueues.context'),
            label: Em.I18n.t('services.service.actions.run.yarnRefreshQueues.menu'),
            cssClass: 'glyphicon glyphicon-refresh',
            disabled: false
          }
        }
      });
    });
    afterEach(function() {
      App.HostComponentActionMap.getMap.restore();
    });

    var tests = Em.A([
      {
        msg: 'Component not present in `App.HostComponentActionMap.getMap()` should have a default valid label',
        command: 'CUSTOM',
        e: Em.I18n.t('services.service.actions.run.executeCustomCommand.menu').format('Custom')
      },
      {
        msg: 'Component present in `App.HostComponentActionMap.getMap()` with no label should have a default valid label',
        command: 'MASTER_CUSTOM_COMMAND',
        e: Em.I18n.t('services.service.actions.run.executeCustomCommand.menu').format('Master Custom Command')
      },
      {
        msg: 'Component present in `App.HostComponentActionMap.getMap()` with label should have a custom valid label',
        command: 'REFRESHQUEUES',
        e: Em.I18n.t('services.service.actions.run.yarnRefreshQueues.menu')
      }
    ]);

    tests.forEach(function(test) {
      it(test.msg, function() {
        expect(hostComponentView.getCustomCommandLabel(test.command)).to.equal(test.e);
      })
    });
  });

  App.TestAliases.testAsComputedExistsInByKey(getView(), 'isDeletableComponent', 'content.componentName', 'App.components.deletable', ['DATANODE', 'HDFS_CLIENT', 'NFS_GATEWAY'])

  describe("#isMoveComponentDisabled", function() {
    beforeEach(function(){
      sinon.stub(App.HostComponent, 'find').returns([
        Em.Object.create({componentName: 'C1', hostName: 'host1'}),
        Em.Object.create({componentName: 'C1', hostName: 'host2'}),
        Em.Object.create({componentName: 'C2', hostName: 'host1'})
      ]);
    });
    afterEach(function(){
      App.HostComponent.find.restore();
    });
    it("component is not movable", function() {
      App.set('allHostNames', ['host1', 'host2']);
      hostComponentView.set('content.componentName', 'C1');
      hostComponentView.propertyDidChange('isMoveComponentDisabled');
      expect(hostComponentView.get('isMoveComponentDisabled')).to.be.true;
    });
    it("component movable", function() {
      App.set('allHostNames', ['host1', 'host2']);
      hostComponentView.set('content.componentName', 'C2');
      hostComponentView.propertyDidChange('isMoveComponentDisabled');
      expect(hostComponentView.get('isMoveComponentDisabled')).to.be.false;
    });
  });

  describe("#runningComponentCounter()", function() {
    beforeEach(function(){
      sinon.stub(App.HostComponent, 'find').returns([
        Em.Object.create({componentName: 'C1', workStatus: 'STARTED'}),
        Em.Object.create({componentName: 'C2', workStatus: 'INSTALLED'})
      ]);
    });
    afterEach(function(){
      App.HostComponent.find.restore();
    });
    it("running components present", function() {
      hostComponentView.set('content.componentName', 'C1');
      expect(hostComponentView.runningComponentCounter()).to.equal(1);
    });
    it("running components absent", function() {
      hostComponentView.set('content.componentName', 'C2');
      expect(hostComponentView.runningComponentCounter()).to.equal(0);
    });
  });

  describe("#isReassignable", function() {
    beforeEach(function(){
      this.mock = sinon.stub(App, 'get');
      this.mock.withArgs('components.reassignable').returns(['C1'])
    });
    afterEach(function(){
      this.mock.restore();
    });
    it("component reassignable and count is 2", function() {
      this.mock.withArgs('allHostNames.length').returns(2);
      hostComponentView.set('content.componentName', 'C1');
      hostComponentView.propertyDidChange('isReassignable');
      expect(hostComponentView.get('isReassignable')).to.be.true;
    });
    it("component reassignable and count is 1", function() {
      this.mock.withArgs('allHostNames.length').returns(1);
      hostComponentView.set('content.componentName', 'C1');
      hostComponentView.propertyDidChange('isReassignable');
      expect(hostComponentView.get('isReassignable')).to.be.false;
    });
    it("component is not reassignable", function() {
      hostComponentView.set('content.componentName', 'C2');
      hostComponentView.propertyDidChange('isReassignable');
      expect(hostComponentView.get('isReassignable')).to.be.false;
    });
  });

  App.TestAliases.testAsComputedExistsInByKey(getView(), 'isRestartableComponent', 'content.componentName', 'App.components.restartable', ['DATANODE', 'JOURNALNODE', 'NAMENODE']);

  App.TestAliases.testAsComputedExistsInByKey(getView(), 'isRefreshConfigsAllowed', 'content.componentName', 'App.components.refreshConfigsAllowed', ['FLUME_HANDLER']);

  describe("#isRefreshConfigsAllowed", function() {
    beforeEach(function(){
      sinon.stub(App, 'get').returns(['C1']);
    });
    afterEach(function(){
      App.get.restore();
    });
    it("component deletable", function() {
      hostComponentView.set('content.componentName', 'C1');
      hostComponentView.propertyDidChange('isRefreshConfigsAllowed');
      expect(hostComponentView.get('isRefreshConfigsAllowed')).to.be.true;
    });
    it("component is not deletable", function() {
      hostComponentView.set('content.componentName', 'C2');
      hostComponentView.propertyDidChange('isRefreshConfigsAllowed');
      expect(hostComponentView.get('isRefreshConfigsAllowed')).to.be.false;
    });
  });

  describe('#clientCustomCommands', function() {
    beforeEach(function() {
      sinon.stub(App.StackServiceComponent, 'find').returns(Em.Object.create({
        customCommands: ['COMMAND1']
      }));
    });
    afterEach(function() {
      App.StackServiceComponent.find.restore();
    });

    it('should return list of commands', function() {
      hostComponentView.propertyDidChange('clientCustomCommands');
      expect(hostComponentView.get('clientCustomCommands')).to.be.eql([{
        command: 'COMMAND1',
        label: Em.I18n.t('services.service.actions.run.executeCustomCommand.menu').format('COMMAND1')
      }]);
    });
  
    it('should return empty list of commands', function() {
      hostComponentView.set('content.componentName', 'KERBEROS_CLIENT');
      hostComponentView.propertyDidChange('clientCustomCommands');
      expect(hostComponentView.get('clientCustomCommands')).to.be.empty;
    });
  });

  describe('#installClient', function() {

    it('installClients should be called', function() {
      hostComponentView.installClient();
      expect(hostComponentView.get('controller').installClients.calledOnce).to.be.true;
    });
  });
  
  describe('#typeDisplay', function() {
    
    it('should return label for master component', function() {
      hostComponentView.set('content.isMaster', true);
      hostComponentView.propertyDidChange('typeDisplay');
      expect(hostComponentView.get('typeDisplay')).to.be.equal(Em.I18n.t('common.master'));
    });
  
    it('should return label for slave component', function() {
      hostComponentView.set('content.isSlave', true);
      hostComponentView.propertyDidChange('typeDisplay');
      expect(hostComponentView.get('typeDisplay')).to.be.equal(Em.I18n.t('common.slave'));
    });
  
    it('should return label for client component', function() {
      hostComponentView.set('content.isClient', true);
      hostComponentView.propertyDidChange('typeDisplay');
      expect(hostComponentView.get('typeDisplay')).to.be.equal(Em.I18n.t('common.client'));
    });
  });
  
  describe('#maintenanceTooltip', function() {
    beforeEach(function() {
      hostComponentView.get('content').setProperties({
        service: {
          serviceName: 'S1'
        },
        displayName: 's1',
        host: {
          hostName: 'host1'
        }
      });
    });
    
    it('should return label for IMPLIED_FROM_SERVICE', function() {
      hostComponentView.set('content.passiveState', 'IMPLIED_FROM_SERVICE');
      hostComponentView.propertyDidChange('maintenanceTooltip');
      expect(hostComponentView.get('maintenanceTooltip')).to.be.equal(
        Em.I18n.t('passiveState.disabled.impliedFromHighLevel').format('s1', 'S1')
      );
    });
  
    it('should return label for IMPLIED_FROM_HOST', function() {
      hostComponentView.set('content.passiveState', 'IMPLIED_FROM_HOST');
      hostComponentView.propertyDidChange('maintenanceTooltip');
      expect(hostComponentView.get('maintenanceTooltip')).to.be.equal(
        Em.I18n.t('passiveState.disabled.impliedFromHighLevel').format('s1', 'host1')
      );
    });
  
    it('should return label for IMPLIED_FROM_SERVICE_AND_HOST', function() {
      hostComponentView.set('content.passiveState', 'IMPLIED_FROM_SERVICE_AND_HOST');
      hostComponentView.propertyDidChange('maintenanceTooltip');
      expect(hostComponentView.get('maintenanceTooltip')).to.be.equal(
        Em.I18n.t('passiveState.disabled.impliedFromServiceAndHost').format('s1', 'S1', 'host1')
      );
    });
  
    it('should return label for unknown', function() {
      hostComponentView.set('content.passiveState', '');
      hostComponentView.propertyDidChange('maintenanceTooltip');
      expect(hostComponentView.get('maintenanceTooltip')).to.be.empty;
    });
  });
  
  describe('#meetsCustomCommandReq', function() {
    var component = Em.Object.create({cardinality: "1"});
    beforeEach(function() {
      sinon.stub(hostComponentView, 'runningComponentCounter').returns(true);
      this.mock = sinon.stub(App.HostComponent, 'getCount');
      hostComponentView.set('excludedMasterCommands', ['command1']);
    });
    afterEach(function() {
      hostComponentView.runningComponentCounter.restore();
      this.mock.restore();
    });
    
    it('should return false when command excluded', function() {
      expect(hostComponentView.meetsCustomCommandReq(component, 'command1')).to.be.false;
    });
  
    it('should return true when cardinality !== 1', function() {
      expect(hostComponentView.meetsCustomCommandReq(component, 'command2')).to.be.true;
    });
  
    it('should return false when total count > 2 and running', function() {
      component.set('cardinality', '0+');
      hostComponentView.set('workStatus', App.HostComponentStatus.stopped);
      this.mock.returns(2);
      expect(hostComponentView.meetsCustomCommandReq(component, 'command2')).to.be.false;
    });
  
    it('should return false when total count = 1', function() {
      component.set('cardinality', '0+');
      hostComponentView.set('workStatus', App.HostComponentStatus.stopped);
      this.mock.returns(1);
      expect(hostComponentView.meetsCustomCommandReq(component, 'command2')).to.be.false;
    });
  });

});
