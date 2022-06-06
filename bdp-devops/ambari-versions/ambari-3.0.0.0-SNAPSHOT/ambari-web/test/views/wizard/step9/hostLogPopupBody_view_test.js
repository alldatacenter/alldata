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
require('views/wizard/step9/hostLogPopupBody_view');
var view;

function getView() {
  return App.WizardStep9HostLogPopupBodyView.create({
    parentView: Em.Object.create({
      host: Em.Object.create()
    })
  });
}

describe('App.WizardStep9HostLogPopupBodyView', function() {

  beforeEach(function() {
    view = getView();
  });

  App.TestAliases.testAsComputedAlias(getView(), 'isNoTasksScheduled', 'parentView.host.isNoTasksForInstall', 'boolean');

  describe('#isHeartbeatLost', function() {
    it('should depends on parentView.host.status', function() {
      view.set('parentView.host.status', 'success');
      expect(view.get('isHeartbeatLost')).to.equal(false);
      view.set('parentView.host.status', 'heartbeat_lost');
      expect(view.get('isHeartbeatLost')).to.equal(true);
    });
  });

  describe('#visibleTasks', function() {
    Em.A([
        {
          value: 'pending',
          f: ['pending', 'queued']
        },
        {
          value: 'in_progress',
          f: ['in_progress']
        },
        {
          value: 'failed',
          f: ['failed']
        },
        {
          value: 'completed',
          f: ['completed']
        },
        {
          value: 'aborted',
          f: ['aborted']
        },
        {
          value: 'timedout',
          f: ['timedout']
        },
        {
          value: 'all'
        }
      ]).forEach(function(test) {
        it(test.value, function() {
          view.reopen({
            category: Em.Object.create({value: test.value}),
            tasks: Em.A([
              {status: 'pending', isVisible: false},
              {status: 'queued', isVisible: false},
              {status: 'in_progress', isVisible: false},
              {status: 'failed', isVisible: false},
              {status: 'completed', isVisible: false},
              {status: 'aborted', isVisible: false},
              {status: 'timedout', isVisible: false}
            ])
          });
          view.visibleTasks();
          var visibleTasks = view.get('tasks').filter(function(task) {
            if (test.f) {
              return test.f.contains(task.status);
            }
            return true;
          });
          expect(visibleTasks.everyProperty('isVisible', true)).to.equal(true);
        });
    });
  });

  describe('#backToTaskList', function() {

    beforeEach(function () {
      sinon.stub(view, 'destroyClipBoard', Em.K);
    });

    afterEach(function () {
      view.destroyClipBoard.restore();
    });

    it('should call destroyClipBoard', function() {
      view.backToTaskList();
      expect(view.destroyClipBoard.calledOnce).to.equal(true);
    });
    it('should set isLogWrapHidden to true', function() {
      view.set('isLogWrapHidden', false);
      view.backToTaskList();
      expect(view.get('isLogWrapHidden')).to.equal(true);
    });
  });

  describe('#getStartedTasks', function() {
    it('should return tasks with some status', function() {
      var logTasks = Em.A([
        {Tasks: {}}, {Tasks: {status: 's'}}, {Tasks: {status: null}}, {Tasks: {status: 'v'}}
      ]);
      expect(view.getStartedTasks({logTasks: logTasks}).length).to.equal(2);
    });
  });

  describe('#openedTask', function() {
    it('should return currently open task', function() {
      var task = Em.Object.create({id: 2});
      view.reopen({
        tasks: Em.A([
          Em.Object.create({id: 1}),
          Em.Object.create({id: 3}),
          task,
          Em.Object.create({id: 4})
        ])
      });
      view.set('parentView.c', {currentOpenTaskId: 2});
      expect(view.get('openedTask.id')).to.equal(2);
    });
  });

  describe('#tasks', function() {
    var testTask = {
      Tasks: {
        status: 'init',
        id: 1,
        request_id: 2,
        role: 'PIG',
        stderr: 'stderr',
        stdout: 'stdout',
        host_name: 'host1',
        command: 'Cmd',
        command_detail: 'TEST SERVICE/COMPONENT_DESCRIPTION'
      }
    };

    beforeEach(function () {
      view.set('parentView.host.logTasks', [testTask]);
      this.t = view.get('tasks');
      this.first = this.t[0];
    });

    it('should map tasks', function() {
      expect(this.t.length).to.equal(1);
    });

    it('should map id', function() {
      expect(this.first.get('id')).to.equal(1);
    });

    it('should map requestId', function() {
      expect(this.first.get('requestId')).to.equal(2);
    });

    it('should map command', function() {
      expect(this.first.get('command')).to.equal('cmd');
    });

    it('should map commandDetail', function() {
      expect(this.first.get('commandDetail')).to.equal(' Test Component Description');
    });

    it('should map role', function() {
      expect(this.first.get('role')).to.equal('Pig');
    });

    it('should map stderr', function() {
      expect(this.first.get('stderr')).to.equal('stderr');
    });

    it('should map stdout', function() {
      expect(this.first.get('stdout')).to.equal('stdout');
    });

    it('should map isVisible', function() {
      expect(this.first.get('isVisible')).to.equal(true);
    });

    it('should map hostName', function() {
      expect(this.first.get('hostName')).to.equal('host1');
    });

    describe('icons', function () {

      Em.A([
        {
          status: 'pending',
          icon:'glyphicon glyphicon-cog'
        },
        {
          status: 'queued',
          icon:'glyphicon glyphicon-cog'
        },
        {
          status: 'in_progress',
          icon:'icon-cogs'
        },
        {
          status: 'completed',
          icon:'glyphicon glyphicon-ok'
        },
        {
          status: 'failed',
          icon:'glyphicon glyphicon-exclamation-sign'
        },
        {
          status: 'aborted',
          icon:'glyphicon glyphicon-minus'
        },
        {
          status: 'timedout',
          icon:'glyphicon glyphicon-time'
        }
      ]).forEach(function (test) {
        it(test.status + ' -> ' + test.icon, function () {
          var t = Em.copy(testTask);
          t.Tasks.status = test.status;
          view.set('parentView.host.logTasks', [t]);
          view.propertyDidChange('tasks');
          expect(view.get('tasks')[0].icon).to.equal(test.icon);
        });
      });

    });

  });

  describe('#toggleTaskLog', function() {

    beforeEach(function () {
      view.set('parentView.c', Em.Object.create({loadCurrentTaskLog: Em.K}));
      sinon.spy(view.get('parentView.c'), 'loadCurrentTaskLog');
    });

    afterEach(function () {
      view.get('parentView.c').loadCurrentTaskLog.restore();
    });

    describe('isLogWrapHidden is true', function () {

      var taskInfo = {
        id: 1,
        requestId: 2
      };

      beforeEach(function () {
        view.set('isLogWrapHidden', true);
        view.toggleTaskLog({context: taskInfo});
      });

      it('isLogWrapHidden is set false', function() {
        expect(view.get('isLogWrapHidden')).to.equal(false);
      });

      it('currentOpenTaskId is equal to id', function() {
        expect(view.get('parentView.c.currentOpenTaskId')).to.equal(taskInfo.id);
      });

      it('currentOpenTaskRequestId is equal to requestId', function() {
        expect(view.get('parentView.c.currentOpenTaskRequestId')).to.equal(taskInfo.requestId);
      });

      it('loadCurrentTaskLog called once', function() {
        expect(view.get('parentView.c').loadCurrentTaskLog.calledOnce).to.equal(true);
      });

    });

    describe('isLogWrapHidden is false', function () {

      var taskInfo = {};

      beforeEach(function () {
        view.set('isLogWrapHidden', false);
        view.toggleTaskLog({context: taskInfo});
      });

      it('isLogWrapHidden is set true', function() {
        expect(view.get('isLogWrapHidden')).to.equal(true);
      });

      it('currentOpenTaskId is 0', function() {
        expect(view.get('parentView.c.currentOpenTaskId')).to.equal(0);
      });

      it('currentOpenTaskRequestId is 0', function() {
        expect(view.get('parentView.c.currentOpenTaskRequestId')).to.equal(0);
      });

    });
  });

});
