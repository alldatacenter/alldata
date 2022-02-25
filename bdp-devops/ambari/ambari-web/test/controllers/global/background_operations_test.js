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

require('config');
require('utils/updater');
require('utils/ajax/ajax');

require('models/host_component');

require('controllers/global/background_operations_controller');
require('views/common/modal_popup');
require('utils/host_progress_popup');

describe('App.BackgroundOperationsController', function () {

  var controller = App.BackgroundOperationsController.create();

  describe('#getQueryParams', function () {
    /**
     * Predefined data
     *
     */
    App.set('clusterName', 'testName');

    var tests = Em.A([
      {
        levelInfo: Em.Object.create({
          name: 'REQUESTS_LIST',
          requestId: null,
          taskId: null,
          sync: false
        }),
        e: {
          name: 'background_operations.get_most_recent',
          successCallback: 'callBackForMostRecent',
          data: {
            'operationsCount': 10
          }
        },
        response: {items: []},
        m: '"Get Most Recent"'
      },
      {
        levelInfo: Em.Object.create({
          name: 'TASK_DETAILS',
          requestId: 1,
          taskId: 1
        }),
        e: {
          name: 'background_operations.get_by_task',
          successCallback: 'callBackFilteredByTask',
          data: {
            taskId: 1,
            requestId: 1
          }
        },
        response: {items: {Tasks: {request_id: 0}}},
        m: '"Filtered By task"'
      },
      {
        levelInfo: Em.Object.create({
          name: 'TASKS_LIST',
          requestId: 1,
          taskId: 1
        }),
        e: {
          name: 'background_operations.get_by_request',
          successCallback: 'callBackFilteredByRequest',
          data: {
            requestId: 1
          }
        },
        response: {items: {Requests: {id: 0}}},
        m: '"Filtered By Request (TASKS_LIST)"'
      },
      {
        levelInfo: Em.Object.create({
          name: 'HOSTS_LIST',
          requestId: 1,
          taskId: 1
        }),
        e: {
          name: 'background_operations.get_by_request',
          successCallback: 'callBackFilteredByRequest',
          data: {
            requestId: 1
          }
        },
        response: {items: {Requests: {id: 0}}},
        m: '"Filtered By Request (HOSTS_LIST)"'
      }
    ]);

    tests.forEach(function (test) {
      it(test.m, function () {
        controller.set('levelInfo', test.levelInfo);
        controller.set('services', [Em.Object.create({
          id: 1,
          previousTaskStatusMap: {}
        })]);
        var r = controller.getQueryParams();
        expect(r.name).to.equal(test.e.name);
        expect(r.successCallback).to.equal(test.e.successCallback);
        expect(r.data).to.eql(test.e.data);
      });
    });
  });

  describe('#isUpgradeRequest', function() {

    it('defines if request is upgrade task (true)', function() {
      expect(controller.isUpgradeRequest({Requests: {request_context: "upgrading"}})).to.be.true;
    });

    it('defines if request is upgrade task (true - with uppercase)', function() {
      expect(controller.isUpgradeRequest({Requests: {request_context: "UPGRADING"}})).to.be.true;
    });

    it('defines if request is downgrade task (true - with uppercase)', function() {
      expect(controller.isUpgradeRequest({Requests: {request_context: "downgrading"}})).to.be.true;
    });

    it('defines if request is upgrade task (false)', function() {
      expect(controller.isUpgradeRequest({Requests: {request_context: "install"}})).to.be.false;
    });

    it('defines if request is upgrade task (false - invalid param)', function() {
      expect(controller.isUpgradeRequest({Requests: {}})).to.be.false;
    });
  });

  describe('#callBackForMostRecent()', function () {

    it('No requests exists', function () {
      var data = {
        items: []
      };
      controller.callBackForMostRecent(data);
      expect(controller.get("runningOperationsCount")).to.equal(0);
      expect(controller.get("services.length")).to.equal(0);
    });
    it('One non-running request', function () {
      var data = {
        items: [
          {
            Requests: {
              id: 1,
              request_context: '',
              task_count: 0,
              aborted_task_count: 0,
              completed_task_count: 0,
              failed_task_count: 0,
              timed_out_task_count: 0,
              queued_task_count: 0
            }
          }
        ]
      };
      controller.callBackForMostRecent(data);
      expect(controller.get("runningOperationsCount")).to.equal(0);
      expect(controller.get("services").mapProperty('id')).to.eql([1]);
    });

    it('One request that is excluded', function () {
      var data = {
        items: [
          {
            Requests: {
              id: 1,
              request_context: 'upgrading'
            }
          }
        ]
      };
      controller.callBackForMostRecent(data);
      expect(controller.get("runningOperationsCount")).to.equal(0);
      expect(controller.get("services").mapProperty('id')).to.eql([]);
    });

    it('One running request', function () {
      var data = {
        items: [
          {
            Requests: {
              id: 1,
              request_context: '',
              request_status: 'IN_PROGRESS'
            }
          }
        ]
      };
      controller.callBackForMostRecent(data);
      expect(controller.get("runningOperationsCount")).to.equal(1);
      expect(controller.get("services").mapProperty('id')).to.eql([1]);
    });
    it('Two requests in order', function () {
      var data = {
        items: [
          {
            Requests: {
              id: 1,
              request_context: ''
            }
          },
          {
            Requests: {
              id: 2,
              request_context: ''
            }
          }
        ]
      };
      controller.callBackForMostRecent(data);
      expect(controller.get("runningOperationsCount")).to.equal(0);
      expect(controller.get("services").mapProperty('id')).to.eql([2, 1]);
    });
  });

  describe('#removeOldRequests()', function () {
    var testCases = [
      {
        title: 'No requests exist',
        content: {
          currentRequestIds: [],
          services: []
        },
        result: []
      },
      {
        title: 'One current request',
        content: {
          currentRequestIds: [1],
          services: [
            {id: 1}
          ]
        },
        result: [
          {id: 1}
        ]
      },
      {
        title: 'One old request',
        content: {
          currentRequestIds: [2],
          services: [
            {id: 1}
          ]
        },
        result: []
      },
      {
        title: 'One old request and one is current',
        content: {
          currentRequestIds: [2],
          services: [
            {id: 1},
            {id: 2}
          ]
        },
        result: [
          {id: 2}
        ]
      },
      {
        title: 'two old request and two current',
        content: {
          currentRequestIds: [3, 4],
          services: [
            {id: 1},
            {id: 2},
            {id: 3},
            {id: 4}
          ]
        },
        result: [
          {id: 3},
          {id: 4}
        ]
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        controller.set('services', test.content.services);
        controller.removeOldRequests(test.content.currentRequestIds);
        expect(JSON.stringify(controller.get('services'))).to.equal(JSON.stringify(test.result));
      });
    });
  });

  describe('#isOneHost()', function () {
    var testCases = [
      {
        title: 'inputs is null',
        inputs: null,
        result: false
      },
      {
        title: 'inputs is "null"',
        inputs: 'null',
        result: false
      },
      {
        title: 'inputs is empty object',
        inputs: '{}',
        result: false
      },
      {
        title: 'included_hosts is empty',
        inputs: '{"included_hosts": ""}',
        result: false
      },
      {
        title: 'included_hosts contain one host',
        inputs: '{"included_hosts": "host1"}',
        result: true
      },
      {
        title: 'included_hosts contain two hosts',
        inputs: '{"included_hosts": "host1,host2"}',
        result: false
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        expect(controller.isOneHost(test.inputs)).to.eql(test.result);
      });
    });
  });

  describe('#assignScheduleId()', function () {
    var testCases = [
      {
        title: 'isOneHost is false',
        content: {
          request: {
            Requests: {
              request_schedule: {
                schedule_id: 1
              },
              inputs: null
            }
          },
          requestParams: ''
        },
        result: 1
      },
      {
        title: 'isOneHost is true and requestContext is empty',
        content: {
          request: {
            Requests: {
              request_schedule: {
                schedule_id: 1
              },
              inputs: '{"included_hosts": "host1"}'
            }
          },
          requestParams: {
            requestContext: ''
          }
        },
        result: 1
      },
      {
        title: 'isOneHost is true and requestContext contains "Recommission"',
        content: {
          request: {
            Requests: {
              request_schedule: {
                schedule_id: 1
              },
              inputs: '{"included_hosts": "host1"}'
            }
          },
          requestParams: {
            requestContext: 'Recommission'
          }
        },
        result: null
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        controller.assignScheduleId(test.content.request, test.content.requestParams);
        expect(test.content.request.Requests.request_schedule.schedule_id).to.equal(test.result);
      });
    });
  });

  describe('#callBackFilteredByRequest()', function () {

    it('request haven\'t tasks and isRunning false', function () {
      var data = {
        Requests: {id: 1},
        tasks: []
      };
      var request = Em.Object.create({
        id: 1,
        previousTaskStatusMap: {},
        isRunning: false,
        progress: 0,
        status:''
      });
      controller.set('services', [request]);
      controller.callBackFilteredByRequest(data);
      expect(request.get('previousTaskStatusMap')).to.eql({});
      expect(request.get('hostsMap')).to.eql({});
      expect(request.get('isRunning')).to.equal(false);
    });

    it('request haven\'t tasks and isRunning true', function () {
      var data = {
        Requests: {id: 1},
        tasks: []
      };
      var request = Em.Object.create({
        id: 1,
        previousTaskStatusMap: {},
        isRunning: true,
        progress: 0,
        status:''
      });
      controller.set('services', [request]);
      controller.callBackFilteredByRequest(data);
      expect(request.get('previousTaskStatusMap')).to.eql({});
      expect(request.get('hostsMap')).to.eql({});
      expect(request.get('isRunning')).to.equal(true);
    });

    it('request has one completed task', function () {
      var data = {
        Requests: {id: 1},
        tasks: [
          {
            Tasks: {
              id: 1,
              host_name: 'host1',
              status: 'COMPLETED'
            }
          }
        ]
      };
      var request = Em.Object.create({
        id: 1,
        previousTaskStatusMap: {},
        isRunning: true,
        progress: 100,
        status:''
      });
      controller.set('services', [request]);
      controller.callBackFilteredByRequest(data);
      expect(request.get('previousTaskStatusMap')).to.eql({"1": "COMPLETED"});
      expect(request.get('hostsMap.host1.logTasks.length')).to.equal(1);
    });

    it('request has one completed task and one running task', function () {
      var data = {
        Requests: {id: 1},
        tasks: [
          {
            Tasks: {
              id: 1,
              host_name: 'host1',
              status: 'COMPLETED'
            }
          },
          {
            Tasks: {
              id: 2,
              host_name: 'host1',
              status: 'IN_PROGRESS'
            }
          }
        ]
      };
      var request = Em.Object.create({
        id: 1,
        previousTaskStatusMap: {},
        isRunning: true,
        progress: 100,
        status:''
      });
      controller.set('services', [request]);
      controller.callBackFilteredByRequest(data);
      expect(request.get('previousTaskStatusMap')).to.eql({"1": "COMPLETED", "2": "IN_PROGRESS"});
      expect(request.get('hostsMap.host1.logTasks.length')).to.equal(2);
      expect(request.get('isRunning')).to.equal(true);
    });
  });

  describe("#isInitLoading()", function () {

    it("should return false when not on HOSTS_LIST level", function() {
      controller.set('levelInfo', Em.Object.create({
        name: 'SERVICES_LIST'
      }));
      expect(controller.isInitLoading()).to.be.false;
    });

    it("should return false when no request found", function() {
      controller.set('levelInfo', Em.Object.create({
        name: 'HOSTS_LIST',
        requestId: 1
      }));
      controller.set('services', []);
      expect(controller.isInitLoading()).to.be.false;
    });

    it("should return false when request has hosts", function() {
      controller.set('levelInfo', Em.Object.create({
        name: 'HOSTS_LIST',
        requestId: 1
      }));
      controller.set('services', [Em.Object.create({
        id: 1,
        hostsLevelLoaded: true
      })]);
      expect(controller.isInitLoading()).to.be.false;
    });

    it("should return true when no request has no hosts", function() {
      controller.set('levelInfo', Em.Object.create({
        name: 'HOSTS_LIST',
        requestId: 1
      }));
      controller.set('services', [Em.Object.create({
        id: 1,
        hostsMap: {}
      })]);
      expect(controller.isInitLoading()).to.be.true;
    });
  });

  describe("#callBackFilteredByTask()", function () {
    var data = {
      Tasks: {
        request_id: 1,
        host_name: 'host1',
        id: 2,
        status: 'foo',
        stdout: 'bar',
        stderr: 'barfoo',
        command: 'cmd',
        custom_command_name: 'custom-cmd',
        structured_out: 'str-out',
        output_log: 'out-log',
        error_log: 'err-log'
      }
    };

    beforeEach(function() {
      sinon.stub(App, 'dateTime').returns(1);
    });

    afterEach(function() {
      App.dateTime.restore();
    });

    it("should set task info", function() {
      var task = {
        Tasks: {
          id: 2
        }
      };
      controller.set('services', [
        Em.Object.create({
          id: 1,
          hostsMap: {
            host1: {
              logTasks: [task]
            }
          }
        })
      ]);

      controller.callBackFilteredByTask(data);
      expect(task).to.be.eql({
        "Tasks": {
          "id": 2,
          "status": "foo",
          "stdout": "bar",
          "stderr": "barfoo",
          "command": "cmd",
          "custom_command_name": "custom-cmd",
          "structured_out": "str-out",
          "output_log": "out-log",
          "error_log": "err-log"
        }
      });
      expect(controller.get('serviceTimestamp')).to.be.equal(1);
    });
  });

  describe("#parseRequestContext()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'getRequestContextWithPrefix').returns({
        requestContext: 'CTX_WITH_PREFIX'
      });
    });

    afterEach(function() {
      controller.getRequestContextWithPrefix.restore();
    });

    it("no requestContext specified", function() {
      expect(controller.parseRequestContext()).to.be.eql({
        requestContext: Em.I18n.t('requestInfo.unspecified')
      });
    });

    it("requestContext specified", function() {
      expect(controller.parseRequestContext('CTX')).to.be.eql({
        requestContext: 'CTX'
      });
    });

    it("requestContext specified with prefix", function() {
      expect(controller.parseRequestContext(App.BackgroundOperationsController.CommandContexts.PREFIX)).to.be.eql({
        requestContext: 'CTX_WITH_PREFIX'
      });
    });
  });

  describe("#getRequestContextWithPrefix()", function () {

    beforeEach(function() {
      sinon.stub(App.format, 'role', function (arg) {
        return arg;
      })
    });

    afterEach(function() {
      App.format.role.restore();
    });

    it("custom command", function() {
      expect(controller.getRequestContextWithPrefix('prefix.foo.bar')).to.be.eql({
        requestContext: undefined,
        dependentService: 'bar',
        contextCommand: 'foo'
      });
    });
    it("STOP all services command", function() {
      expect(controller.getRequestContextWithPrefix('prefix.STOP.ALL_SERVICES')).to.be.eql({
        requestContext: Em.I18n.t("requestInfo.stop").format(Em.I18n.t('common.allServices')),
        dependentService: 'ALL_SERVICES',
        contextCommand: 'STOP'
      });
    });
    it("STOP one service command", function() {
      expect(controller.getRequestContextWithPrefix('prefix.STOP.S1')).to.be.eql({
        requestContext: Em.I18n.t("requestInfo.stop").format('S1'),
        dependentService: 'S1',
        contextCommand: 'STOP'
      });
    });
    it("ROLLING-RESTART service command", function() {
      expect(controller.getRequestContextWithPrefix('prefix.ROLLING-RESTART.S1.foo.bar')).to.be.eql({
        requestContext: Em.I18n.t("rollingrestart.rest.context").format('S1', 'foo', 'bar'),
        dependentService: 'S1',
        contextCommand: 'ROLLING-RESTART'
      });
    });
  });

  describe("#showPopup()", function () {

    beforeEach(function() {
      sinon.stub(App.router, 'get').returns({
        dataLoading: function() {
          return {
            done: Em.clb
          }
        }
      });
      sinon.stub(App.HostPopup, 'initPopup').returns(Em.Object.create());
      App.HostPopup.set('isBackgroundOperations', true);
    });

    afterEach(function() {
      App.router.get.restore();
      App.HostPopup.initPopup.restore();
    });

    it("App.updater.immediateRun should be called", function() {
      controller.showPopup();
      expect(App.updater.immediateRun.calledWith('requestMostRecent')).to.be.true;
    });

    it("popupView should be created and opened", function() {
      controller.set('popupView', null);
      controller.showPopup();
      expect(controller.get('popupView')).to.be.eql(Em.Object.create({
        isNotShowBgChecked: true
      }));
    });

    it("popupView should be restored and opened", function() {
      controller.set('popupView', Em.Object.create());
      controller.showPopup();
      expect(controller.get('popupView')).to.be.eql(Em.Object.create({
        isNotShowBgChecked: true,
        isOpen: true
      }));
    });
  });

  describe("#clear()", function () {

    it("operationsCount should be 10", function() {
      controller.clear();
      expect(controller.get('operationsCount')).to.be.equal(10);
    });
  });

  describe('#subscribeToUpdates', function() {
    beforeEach(function() {
      sinon.stub(controller, 'requestMostRecent', Em.clb);
      sinon.stub(App.StompClient, 'subscribe');
    });
    afterEach(function() {
      controller.requestMostRecent.restore();
      App.StompClient.subscribe.restore();
    });

    it('App.StompClient.subscribe should be called', function() {
      controller.subscribeToUpdates();
      expect(App.StompClient.subscribe.calledWith('/events/requests')).to.be.true;
    });
  });

  describe('#updateRequests', function() {
    beforeEach(function() {
      sinon.stub(controller, 'parseRequestContext').returns({
        requestContext: 'r1'
      });
      sinon.stub(controller, 'generateTasksMapOfRequest').returns({
        currentTaskStatusMap: {},
        hostsMap: {}
      });
      sinon.stub(controller, 'propertyDidChange');
    });
    afterEach(function() {
      controller.parseRequestContext.restore();
      controller.generateTasksMapOfRequest.restore();
      controller.propertyDidChange.restore();
    });

    it('should exit when request is upgrade', function() {
      controller.updateRequests({
        requestContext: 'upgrading'
      });
      expect(controller.parseRequestContext.called).to.be.false;
    });

    it('should add request to list', function() {
      controller.set('services', []);
      controller.updateRequests({
        progressPercent: 0,
        requestStatus: 'PENDING',
        startTime: 1,
        userName: 'admin',
        endTime: -1,
        requestId: 1,
        requestContext: '',
        Tasks: []
      });
      expect(controller.get('services').objectAt(0)).to.be.eql(Em.Object.create({
        id: 1,
        name: 'r1',
        displayName: 'r1',
        tasks: [],
        progress: 0,
        status: 'PENDING',
        isRunning: true,
        startTime: 1,
        userName: 'admin',
        endTime: -1,
        previousTaskStatusMap: {},
        hostsMap: {}
      }));
      expect(controller.propertyDidChange.calledWith('services')).to.be.true;
    });

    it('should update request status', function() {
      controller.set('services', [Em.Object.create({
        id: 1,
        name: 'r1',
        displayName: 'r1',
        tasks: [],
        progress: 0,
        status: 'PENDING',
        isRunning: true,
        startTime: 1,
        userName: '',
        endTime: -1,
        previousTaskStatusMap: {},
        hostsMap: {}
      })]);
      controller.updateRequests({
        progressPercent: 100,
        requestStatus: 'COMPLETED',
        startTime: 1,
        userName: 'admin',
        endTime: 1,
        requestId: 1,
        requestContext: '',
        Tasks: []
      });
      expect(controller.get('services').objectAt(0)).to.be.eql(Em.Object.create({
        id: 1,
        name: 'r1',
        displayName: 'r1',
        tasks: [],
        progress: 100,
        status: 'COMPLETED',
        isRunning: false,
        startTime: 1,
        userName: 'admin',
        endTime: 1,
        previousTaskStatusMap: {},
        hostsMap: {}
      }))
    });
  });

  describe('#generateTasksMapOfRequest', function() {

    it('should return tasks map', function() {
      var event = {
        Tasks: [
          {
            hostName: 'host1',
            id: 1,
            status: 'PENDING',
            requestId: 1
          },
          {
            hostName: 'host1',
            id: 2,
            status: 'PENDING',
            requestId: 1
          },
          {
            hostName: 'host2',
            id: 3,
            status: 'COMPLETED',
            requestId: 1
          }
        ]
      };
      expect(JSON.stringify(controller.generateTasksMapOfRequest(event, null).hostsMap)).to.be.equal(JSON.stringify({
        "host1": {
          "name": "host1",
          "publicName": "host1",
          "logTasks": [
            {
              "Tasks": {
                "status": "PENDING",
                "host_name": "host1",
                "id": 1,
                "request_id": 1
              }
            },
            {
              "Tasks": {
                "status": "PENDING",
                "host_name": "host1",
                "id": 2,
                "request_id": 1
              }
            }
          ],
          "isModified": true
        },
        "host2": {
          "name": "host2",
          "publicName": "host2",
          "logTasks": [{"Tasks": {"status": "COMPLETED", "host_name": "host2", "id": 3, "request_id": 1}}],
          "isModified": true
        }
      }));
      expect(controller.generateTasksMapOfRequest(event, null).currentTaskStatusMap).to.be.eql({
        "1": "PENDING",
        "2": "PENDING",
        "3": "COMPLETED"
      });
    });
  });

  describe('#convertTaskFromEventToApi', function() {

    it('should return converted task object', function() {
      expect(controller.convertTaskFromEventToApi({
        status: 'PENDING',
        hostName: 'host1',
        id: 1,
        requestId: 1
      })).to.be.eql({
          Tasks: {
            status: 'PENDING',
            host_name: 'host1',
            id: 1,
            request_id: 1
          }
        });
    });
  });

  describe('#handleTaskUpdates', function() {
    beforeEach(function() {
      sinon.stub(controller, 'updateTask');
      sinon.stub(App.StompClient, 'subscribe', function(arg1, clb) {
        clb({status: 'COMPLETED', id: 1});
      });
      sinon.stub(App.StompClient, 'unsubscribe');
    });
    afterEach(function() {
      controller.updateTask.restore();
      App.StompClient.subscribe.restore();
      App.StompClient.unsubscribe.restore();
    });

    it('should subscribe and when task is finished unsubscribe', function() {
      controller.set('services', [Em.Object.create({
        id: 1,
        previousTaskStatusMap: {
          1: 'PENDING'
        }
      })]);
      controller.set('levelInfo', Em.Object.create({
        name: 'TASK_DETAILS',
        requestId: 1,
        taskId: 1
      }));
      expect(App.StompClient.subscribe.calledWith('/events/tasks/1')).to.be.true;
      expect(controller.updateTask.calledOnce).to.be.true;
      expect(App.StompClient.unsubscribe.calledWith('/events/tasks/1')).to.be.true;
    });
  });

  describe('#updateTask', function() {

    it('should update task properties', function() {
      var event = {
        requestId: 1,
        id: 1,
        hostName: 'host1',
        status: 'COMPLETED',
        stdout: 'stdout',
        stderr: 'stderr',
        structured_out: 'structured_out',
        outLog: 'outLog',
        errorLog: 'errorLog'
      };
      controller.set('services', [Em.Object.create({
        id: 1,
        hostsMap: {
          host1: {
            logTasks: [
              {
                Tasks: {
                  id: 1,
                  requestId: 1,
                  hostName: 'host1'
                }
              }
            ]
          }
        }
      })]);
      controller.updateTask(event);
      expect(controller.get('services').objectAt(0).get('hostsMap')['host1'].logTasks[0]).to.be.eql({
        Tasks: {
          requestId: 1,
          id: 1,
          hostName: 'host1',
          status: 'COMPLETED',
          stdout: 'stdout',
          stderr: 'stderr',
          structured_out: 'structured_out',
          output_log: 'outLog',
          error_log: 'errorLog'
        }
      });
    });
  });
});
