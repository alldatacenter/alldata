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
require('controllers/wizard/step10_controller');

var controller;

function getController() {
  return App.WizardStep10Controller.create({
    content: {cluster: {controllerName: '', status: 'INSTALL COMPLETE'}}
  });
}

describe('App.WizardStep10Controller', function () {

  beforeEach(function() {
    controller = getController();
  });

  afterEach(function() {
    controller.clearStep();
  });

  App.TestAliases.testAsComputedEqual(getController(), 'isAddServiceWizard', 'content.controllerName', 'addServiceController');

  describe('#clearStep', function() {
    it('should clear clusterInfo', function() {
      controller.get('clusterInfo').pushObject({});
      controller.clearStep();
      expect(controller.get('clusterInfo.length')).to.equal(0);
    });
  });

  describe('#loadStep', function() {
    beforeEach(function() {
      sinon.spy(controller, 'clearStep');
      sinon.stub(controller, 'loadRegisteredHosts', Em.K);
      sinon.stub(controller, 'loadInstalledHosts', Em.K);
      sinon.stub(controller, 'loadInstallTime', Em.K);
    });
    afterEach(function() {
      controller.clearStep.restore();
      controller.loadRegisteredHosts.restore();
      controller.loadInstalledHosts.restore();
      controller.loadInstallTime.restore();
    });
    it('should call clearStep', function() {
      controller.loadStep();
      expect(controller.clearStep.calledOnce).to.equal(true);
    });
    it('should call loadInstalledHosts', function() {
      controller.loadStep();
      expect(controller.loadInstalledHosts.calledOnce).to.equal(true);
    });
    it('should loadInstallTime if not installerController', function() {
      controller.set('content.controllerName', 'addServiceController');
      controller.loadStep();
      expect(controller.loadInstallTime.calledOnce).to.equal(true);
    });
    var testsForLoadInstallTime = Em.A([
      {
        loadMasterComponents: true,
        loadStartedServices: true,
        e: true
      },
      {
        loadMasterComponents: true,
        loadStartedServices: false,
        e: false
      },
      {
        loadMasterComponents: false,
        loadStartedServices: false,
        e: false
      },
      {
        loadMasterComponents: false,
        loadStartedServices: false,
        e: false
      }
    ]);
    testsForLoadInstallTime.forEach(function(test) {
      describe('loadMasterComponents: ' + test.loadMasterComponents.toString() + ' loadStartedServices: ' + test.loadStartedServices.toString(), function() {

        beforeEach(function () {
          controller.set('content.controllerName', 'installerController');
          sinon.stub(controller, 'loadMasterComponents', function() {return test.loadMasterComponents;});
          sinon.stub(controller, 'loadStartedServices', function() {return test.loadStartedServices;});
          controller.loadStep();
        });

        afterEach(function () {
          controller.loadMasterComponents.restore();
          controller.loadStartedServices.restore();
        });

        it('loadInstallTime was ' + (test.e ? '' : 'not') + ' called', function () {
          expect(controller.loadInstallTime.called).to.equal(test.e);
        });

      });
    });
  });

  describe('#loadInstalledHosts', function() {
    var tests = Em.A([
      {
        hosts: {
          'h1': Em.Object.create({status: 'success', tasks: []}),
          'h2': Em.Object.create({status: 'success', tasks: []}),
          'h3': Em.Object.create({status: 'success', tasks: []})
        },
        m: 'all success',
        e: Em.A([
          {id: 1, l: 3}
        ])
      },
      {
        hosts: {
          'h1': Em.Object.create({status: 'warning', tasks: []}),
          'h2': Em.Object.create({status: 'failed', tasks: []}),
          'h3': Em.Object.create({status: 'failed', tasks: []})
        },
        m: 'some failed, some warning',
        e: Em.A([
          {id: 2, l: 3}
        ])
      },
      {
        hosts: {
          'h1': Em.Object.create({status: 'failed', tasks: []}),
          'h2': Em.Object.create({status: 'success', tasks: []}),
          'h3': Em.Object.create({status: 'warning', tasks: []})
        },
        m: 'sone failed, some success, some warning',
        e: Em.A([
          {id: 1, l: 1},
          {id: 2, l: 2}
        ])
      }
    ]);
    tests.forEach(function(test) {
      describe(test.m, function() {

        beforeEach(function () {
          controller.set('content.hosts', test.hosts);
          controller.set('clusterInfo', Em.A([Em.Object.create({id: 1, status: []})]));
          controller.loadInstalledHosts();
        });

        test.e.forEach(function(ex) {
          it(JSON.stringify(test.e), function () {
            var displayStatement = controller.get('clusterInfo').findProperty('id', 1).get('status').findProperty('id', ex.id).get('displayStatement');
            expect(displayStatement.contains(ex.l)).to.equal(true);
          });
        });
      })
    });
    var testsForFailedTasks = Em.A([
      {
        hosts: {
          'h1': Em.Object.create({
            status: 'failed',
            tasks: [
              {Tasks: {status: 'FAILED'}},
              {Tasks: {status: 'FAILED'}}
            ]
          }),
          'h2': Em.Object.create({
            status: 'failed',
            tasks: [
              {Tasks: {status: 'FAILED'}}
            ]
          }),
          'h3': Em.Object.create({status: 'failed', tasks: []})
        },
        m: 'only failed tasks',
        e: Em.A([
          {st: 'failed', l: 3}
        ])
      },
      {
        hosts: {
          'h1': Em.Object.create({
            status: 'failed',
            tasks: [
              {Tasks: {status: 'TIMEDOUT'}}
            ]
          }),
          'h2': Em.Object.create({
            status: 'failed',
            tasks: [
              {Tasks: {status: 'TIMEDOUT'}}
            ]
          }),
          'h3': Em.Object.create({
            status: 'failed',
            tasks: [
              {Tasks: {status: 'TIMEDOUT'}}
            ]
          })
        },
        m: 'only timedout tasks',
        e: Em.A([
          {st: 'timedout', l: 3}
        ])
      },
      {
        hosts: {
          'h1': Em.Object.create({
            status: 'failed',
            tasks: []
          }),
          'h2': Em.Object.create({
            status: 'failed',
            tasks: []
          }),
          'h3': Em.Object.create({
            status: 'failed',
            tasks: [
              {Tasks: {status: 'ABORTED'}},
              {Tasks: {status: 'ABORTED'}},
              {Tasks: {status: 'ABORTED'}}
            ]
          })
        },
        m: 'only aborted tasks',
        e: Em.A([
          {st: 'aborted', l: 3}
        ])
      },
      {
        hosts: {
          'h1': Em.Object.create({
            status: 'warning',
            tasks: [
              {Tasks: {status: 'FAILED'}},
              {Tasks: {status: 'FAILED'}}
            ]
          }),
          'h2': Em.Object.create({
            status: 'warning',
            tasks: [
              {Tasks: {status: 'FAILED'}}
            ]
          }),
          'h3': Em.Object.create({status: 'warning', tasks: []})
        },
        m: 'only failed tasks, warning hosts',
        e: Em.A([
          {st: 'failed', l: 3}
        ])
      },
      {
        hosts: {
          'h1': Em.Object.create({
            status: 'warning',
            tasks: [
              {Tasks: {status: 'TIMEDOUT'}}
            ]
          }),
          'h2': Em.Object.create({
            status: 'warning',
            tasks: [
              {Tasks: {status: 'TIMEDOUT'}}
            ]
          }),
          'h3': Em.Object.create({
            status: 'warning',
            tasks: [
              {Tasks: {status: 'TIMEDOUT'}}
            ]
          })
        },
        m: 'only timedout tasks, warning hosts',
        e: Em.A([
          {st: 'timedout', l: 3}
        ])
      },
      {
        hosts: {
          'h1': Em.Object.create({
            status: 'warning',
            tasks: []
          }),
          'h2': Em.Object.create({
            status: 'warning',
            tasks: []
          }),
          'h3': Em.Object.create({
            status: 'warning',
            tasks: [
              {Tasks: {status: 'ABORTED'}},
              {Tasks: {status: 'ABORTED'}},
              {Tasks: {status: 'ABORTED'}}
            ]
          })
        },
        m: 'only aborted tasks, warning hosts',
        e: Em.A([
          {st: 'aborted', l: 3}
        ])
      }
    ]);
    testsForFailedTasks.forEach(function(test) {
      describe(test.m, function() {

        beforeEach(function () {
          controller.set('content.hosts', test.hosts);
          controller.set('clusterInfo', Em.A([Em.Object.create({id: 1, status: []})]));
          controller.loadInstalledHosts();
        });

        test.e.forEach(function(ex) {
          it(JSON.stringify(test.e), function () {
            var tasksWithNeededStatements = controller.get('clusterInfo').findProperty('id', 1).get('status').findProperty('id', 2).get('statements').filterProperty('status', ex.st);
            expect(tasksWithNeededStatements.length).to.equal(ex.l);
          });
        });
      })
    });
  });

  describe('#loadMasterComponent', function() {
    var tests = Em.A([
      {
        component: Em.Object.create({hostName: 'h1'}),
        e: 1
      },
      {
        component: Em.Object.create({}),
        e: 0
      }
    ]);

    tests.forEach(function(test) {
      it(test.component.get('hostName') ? 'Has hosNBame' : 'Doesn\'t have hostName', function() {
        controller.clearStep();
        controller.get('clusterInfo').pushObject(Em.Object.create({id: 2, status: []}));
        controller.loadMasterComponent(test.component);
        expect(controller.get('clusterInfo').findProperty('id', 2).get('status').length).to.equal(test.e);
      })
    });
  });

  describe('#loadStartedServices', function() {
    var tests = Em.A([
      {
        status: 'STARTED',
        e: {
          ids: [3, 4],
          r: true
        }
      },
      {
        status: 'FAILED',
        e: {
          ids: [3],
          r: false
        }
      },
      {
        status: 'START_SKIPPED',
        e: {
          ids: [3],
          r: false
        }
      }
    ]);
    tests.forEach(function(test) {
      it(test.status, function() {
        controller.set('content', {cluster: {status: test.status}});
        var r = controller.loadStartedServices();
        expect(r).to.equal(test.e.r);
        expect(controller.get('clusterInfo').mapProperty('id')).to.eql(test.e.ids);
      });
    });
  });

  describe('#loadInstallTime', function() {
    var tests = Em.A([
      {
        installTime: 123,
        e: [5]
      },
      {
        installTime: null,
        e: []
      }
    ]);

    tests.forEach(function(test) {
      it('Install time' + test.installTime ? ' available' : ' not available', function() {
        controller.set('content', {cluster: {installTime: test.installTime}});
        controller.loadInstallTime();
        expect(controller.get('clusterInfo').mapProperty('id')).to.eql(test.e);
      });
    });
  });

  describe('#calculateInstallTime', function () {
    it('from "9.21" to 9 minutes 12 seconds', function () {
      expect(controller.calculateInstallTime('9.21')).to.eql({minutes: 9, seconds: 12});
    });
    it('from "0" to 0 minutes 0 seconds', function () {
      expect(controller.calculateInstallTime('0')).to.eql({minutes: 0, seconds: 0});
    });
    it('from "10" to 10 minutes 0 seconds', function () {
      expect(controller.calculateInstallTime('10')).to.eql({minutes: 10, seconds: 0});
    });
    it('from "0.5" to 0 minutes 30 seconds', function () {
      expect(controller.calculateInstallTime('0.5')).to.eql({minutes: 0, seconds: 30});
    });
  });

  describe('#loadMasterComponents', function() {

    var components = Em.A(['NAMENODE','SECONDARY_NAMENODE','JOBTRACKER','HISTORYSERVER','RESOURCEMANAGER','HBASE_MASTER','HIVE_SERVER','OOZIE_SERVER','GANGLIA_SERVER']);

    d3.range(1, components.length).forEach(function(i) {
      d3.range(1, i).forEach(function(j) {
        var c = components.slice(0, j);
        it(c.join(', '), function() {
          var m = c.map(function(component){return {component: component, displayName: component, hostName: 'h1'};});
          controller.set('content.masterComponentHosts', m);
          controller.loadMasterComponents();
          expect(controller.get('clusterInfo').findProperty('id', 2).get('status').length).to.equal(m.length);
        });
      });
    });

  });

  describe('#loadRegisteredHosts', function() {
    var masterComponentHosts = [{hostName: 'h1'}, {hostName: 'h2'}, {hostName: 'h3'}],
      slaveComponentHosts = [{hosts: [{hostName: 'h1'}, {hostName: 'h4'}]}, {hosts: [{hostName: 'h2'}, {hostName: 'h5'}]}],
      hosts = [{hostName: 'h6'}, {hostName: 'h3'}, {hostName: 'h7'}];
    var obj;
    beforeEach(function () {
      controller.set('content.masterComponentHosts', masterComponentHosts);
      controller.set('content.slaveComponentHosts', slaveComponentHosts);
      controller.set('clusterInfo', []);
      sinon.stub(App.Host, 'find', function() {
        return hosts;
      });
      obj = controller.loadRegisteredHosts();
    });

    afterEach(function () {
      App.Host.find.restore();
    });

    it('id  = 1', function() {
      expect(obj.id).to.equal(1);
    });
    it('color = text-info', function () {
      expect(obj.color).to.equal('text-info');
    });
    it('displayStatement is valid', function () {
      expect(obj.displayStatement).to.equal(Em.I18n.t('installer.step10.hostsSummary').format(7));
    });
    it('status is []', function () {
      expect(obj.status).to.eql([]);
    });
    it('clusterInfo.firstObject is valid', function () {
      expect(controller.get('clusterInfo.firstObject')).to.eql(obj);
    });
  });

});
