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
var testHelpers = require('test/helpers');
require('utils/polling');

describe('App.Poll', function () {

  var poll;

  beforeEach(function () {
    poll = App.Poll.create();
  });

  describe('#isCompleted', function () {

    var cases = [
      {
        isError: true,
        isSuccess: false,
        isCompleted: true,
        title: 'error'
      },
      {
        isError: false,
        isSuccess: true,
        isCompleted: true,
        title: 'success'
      },
      {
        isError: false,
        isSuccess: false,
        isCompleted: false,
        title: 'incomplete'
      }
    ];

    cases.forEach(function (item) {

      it(item.title, function () {
        poll.setProperties({
          isError: item.isError,
          isSuccess: item.isSuccess
        });
        expect(poll.get('isCompleted')).to.equal(item.isCompleted);
      });

    });

  });

  describe('#showLink', function () {

    var cases = [
      {
        isPolling: true,
        isStarted: false,
        showLink: true,
        title: 'polling'
      },
      {
        isPolling: false,
        isStarted: true,
        showLink: true,
        title: 'started'
      },
      {
        isPolling: false,
        isStarted: false,
        showLink: false,
        title: 'not polling, not started'
      }
    ];

    cases.forEach(function (item) {

      it(item.title, function () {
        poll.setProperties({
          isPolling: item.isPolling,
          isStarted: item.isStarted
        });
        expect(poll.get('showLink')).to.equal(item.showLink);
      });

    });

  });

  describe('#start', function () {

    var cases = [
      {
        setRequestIdCallCount: 1,
        startPollingCallCount: 0,
        title: 'request id not defined'
      },
      {
        requestId: null,
        setRequestIdCallCount: 1,
        startPollingCallCount: 0,
        title: 'request id is null'
      },
      {
        requestId: 0,
        setRequestIdCallCount: 0,
        startPollingCallCount: 1,
        title: 'request id is zero'
      },
      {
        requestId: 1,
        setRequestIdCallCount: 0,
        startPollingCallCount: 1,
        title: 'request id is non-zero'
      }
    ];

    cases.forEach(function (item) {

      describe(item.title, function () {

        beforeEach(function () {
          sinon.stub(poll, 'setRequestId', Em.K);
          sinon.stub(poll, 'startPolling', Em.K);
          poll.set('requestId', item.requestId);
          poll.start();
        });

        afterEach(function () {
          poll.setRequestId.restore();
          poll.startPolling.restore();
        });

        it('set request id', function () {
          expect(poll.setRequestId.callCount).to.equal(item.setRequestIdCallCount);
        });

        it('start polling', function () {
          expect(poll.startPolling.callCount).to.equal(item.startPollingCallCount);
        });

      });

    });

  });

  describe('#setRequestId', function () {

    var ajaxObj;

    beforeEach(function () {
      $.ajax.restore();
      sinon.stub($, 'ajax', function (obj) {
        return obj;
      });
    });

    it('AJAX request parameters', function () {
      var ajaxProps = {
        url: '/default',
        data: {}
      };
      poll.setProperties(ajaxProps);
      poll.setRequestId();
      ajaxObj = $.ajax.firstCall.args[0];
      expect(Em.Object.create(ajaxObj).getProperties(['url', 'data'])).to.eql(ajaxProps);
    });

    describe('#success', function () {

      var cases = [
        {
          data: undefined,
          isSuccess: true,
          isError: false,
          requestId: 1,
          doPollingCallCount: 0,
          title: 'data not defined'
        },
        {
          data: 'null',
          isSuccess: true,
          isError: false,
          requestId: 1,
          doPollingCallCount: 0,
          title: 'null data'
        },
        {
          data: '',
          isSuccess: true,
          isError: false,
          requestId: 1,
          doPollingCallCount: 0,
          title: 'empty data'
        },
        {
          data: '{}',
          isSuccess: false,
          isError: true,
          requestId: undefined,
          doPollingCallCount: 1,
          title: 'empty object'
        },
        {
          data: '{"Requests":null}',
          isSuccess: false,
          isError: true,
          requestId: null,
          doPollingCallCount: 1,
          title: 'no requests info'
        },
        {
          data: '{"Requests":{}}',
          isSuccess: false,
          isError: true,
          requestId: undefined,
          doPollingCallCount: 1,
          title: 'empty requests info'
        },
        {
          data: '{"Requests":{"name":"r0"}}',
          isSuccess: false,
          isError: true,
          requestId: undefined,
          doPollingCallCount: 1,
          title: 'no request id'
        },
        {
          data: '{"Requests":{"id":0}}',
          isSuccess: false,
          isError: true,
          requestId: 0,
          doPollingCallCount: 1,
          title: 'request id available'
        }
      ];

      cases.forEach(function (item) {

        describe(item.title, function () {

          var ajaxObject;

          beforeEach(function () {
            sinon.stub(poll, 'doPolling', Em.K);
            poll.setProperties({
              isSuccess: false,
              isError: true,
              requestId: 1
            });
            poll.setRequestId();
            ajaxObject = $.ajax.firstCall.args[0];
            ajaxObject.success(item.data);
          });

          afterEach(function () {
            poll.doPolling.restore();
          });

          it('isSuccess', function () {
            expect(poll.get('isSuccess')).to.equal(item.isSuccess);
          });

          it('isError', function () {
            expect(poll.get('isError')).to.equal(item.isError);
          });

          it('requestId', function () {
            expect(poll.get('requestId')).to.equal(item.requestId);
          });

          it('doPolling call', function () {
            expect(poll.doPolling.callCount).to.equal(item.doPollingCallCount);
          });

        });

      });

    });

    describe('#error', function () {

      beforeEach(function () {
        poll.setProperties({
          isSuccess: true,
          isError: false
        });
        poll.setRequestId();
        ajaxObj = $.ajax.firstCall.args[0];
        ajaxObj.error();
      });

      it('isSuccess', function () {
        expect(poll.get('isSuccess')).to.be.false;
      });

      it('isError', function () {
        expect(poll.get('isError')).to.be.true;
      });

    });

  });

  describe('#updateTaskLog', function () {

    beforeEach(function () {
      sinon.stub(poll, 'pollTaskLog', Em.K);
      poll.set('currentTaskId', 0);
      poll.updateTaskLog(1);
    });

    afterEach(function () {
      poll.pollTaskLog.restore();
    });

    it('should change task id', function () {
      expect(poll.get('currentTaskId')).to.equal(1);
    });

    it('should poll task log', function () {
      expect(poll.pollTaskLog.calledOnce).to.be.true;
    });

  });

  describe('#doPolling', function () {

    var cases = [
      {
        requestId: undefined,
        startPollingCallCount: 0,
        title: 'request id is undefined'
      },
      {
        requestId: null,
        startPollingCallCount: 0,
        title: 'request id is null'
      },
      {
        requestId: 0,
        startPollingCallCount: 1,
        title: 'request id is 0'
      },
      {
        requestId: 1,
        startPollingCallCount: 1,
        title: 'request id is gra than 0'
      }
    ];

    beforeEach(function () {
      sinon.stub(poll, 'startPolling', Em.K);
    });

    afterEach(function () {
      poll.startPolling.restore();
    });

    cases.forEach(function (item) {

      it(item.title, function () {
        poll.set('requestId', item.requestId);
        poll.doPolling();
        expect(poll.startPolling.callCount).to.equal(item.startPollingCallCount);
      });

    });

  });

  describe('#pollTaskLog', function () {

    var cases = [
      {
        currentTaskId: undefined,
        ajaxCallArguments: undefined,
        title: 'current task id is undefined'
      },
      {
        currentTaskId: null,
        ajaxCallArguments: undefined,
        title: 'current task id is null'
      },
      {
        currentTaskId: 0,
        ajaxCallArguments: [{
          name: 'background_operations.get_by_task',
          data: {
            requestId: 0,
            taskId: 0
          },
          success: 'pollTaskLogSuccessCallback'
        }],
        title: 'current task id is 0'
      },
      {
        currentTaskId: 1,
        ajaxCallArguments: [{
          name: 'background_operations.get_by_task',
          data: {
            requestId: 0,
            taskId: 1
          },
          success: 'pollTaskLogSuccessCallback'
        }],
        title: 'current task id is more than 0'
      }
    ];

    cases.forEach(function (item) {

      it(item.title, function () {
        poll.setProperties({
          requestId: 0,
          currentTaskId: item.currentTaskId
        });
        poll.pollTaskLog();
        if (item.ajaxCallArguments) {
          item.ajaxCallArguments[0].sender = poll;
        }
        expect(testHelpers.findAjaxRequest('name', 'background_operations.get_by_task')).to.eql(item.ajaxCallArguments);
      });

    });

  });

  describe('#pollTaskLogSuccessCallback', function () {

    it('polled data', function () {
      poll.set('polledData', [
        {
          Tasks: {
            id: 0
          }
        },
        {
          Tasks: {
            id: 1
          }
        },
        {
          Tasks: {
            id: 2
          }
        }
      ]);
      poll.pollTaskLogSuccessCallback({
        Tasks: {
          id: 1,
          stdout: 'stdout',
          stderr: 'stderr'
        }
      });
      expect(poll.get('polledData').toArray()).to.eql([
        {
          Tasks: {
            id: 0
          }
        },
        {
          Tasks: {
            id: 1,
            stdout: 'stdout',
            stderr: 'stderr'
          }
        },
        {
          Tasks: {
            id: 2
          }
        }
      ]);
    });

  });

  describe('#startPolling', function () {

    var cases = [
      {
        requestId: undefined,
        ajaxCallArguments: undefined,
        result: false,
        pollTaskLogCallCount: 0,
        title: 'request id is undefined'
      },
      {
        requestId: null,
        ajaxCallArguments: undefined,
        result: false,
        pollTaskLogCallCount: 0,
        title: 'request id is null'
      },
      {
        requestId: 0,
        ajaxCallArguments: [{
          name: 'background_operations.get_by_request',
          data: {
            requestId: 0
          },
          success: 'reloadSuccessCallback',
          error: 'reloadErrorCallback'
        }],
        result: true,
        pollTaskLogCallCount: 1,
        title: 'request id is 0'
      },
      {
        requestId: 1,
        ajaxCallArguments: [{
          name: 'background_operations.get_by_request',
          data: {
            requestId: 1
          },
          success: 'reloadSuccessCallback',
          error: 'reloadErrorCallback'
        }],
        result: true,
        pollTaskLogCallCount: 1,
        title: 'request id is more than 0'
      }
    ];

    cases.forEach(function (item) {

      var result;

      describe(item.title, function () {

        beforeEach(function () {
          sinon.stub(poll, 'pollTaskLog', Em.K);
          poll.set('requestId', item.requestId);
          result = poll.startPolling();
          if (item.ajaxCallArguments) {
            item.ajaxCallArguments[0].sender = poll;
            item.ajaxCallArguments[0].data.callback = poll.startPolling;
          }
        });

        afterEach(function () {
          poll.pollTaskLog.restore();
        });

        it('AJAX request', function () {
          expect(testHelpers.findAjaxRequest('name', 'background_operations.get_by_request')).to.eql(item.ajaxCallArguments);
        });

        it('result', function () {
          expect(result).to.equal(item.result);
        });

        it('pollTaskLog', function () {
          expect(poll.pollTaskLog.callCount).to.equal(item.pollTaskLogCallCount);
        });

      });

    });

  });

  describe('#reloadErrorCallback', function () {

    var cases = [
      {
        status: undefined,
        isSuccess: false,
        isError: false,
        title: 'status is undefined'
      },
      {
        status: null,
        isSuccess: false,
        isError: false,
        title: 'status is null'
      },
      {
        status: 0,
        isSuccess: false,
        isError: false,
        title: 'status is 0'
      },
      {
        status: 200,
        isSuccess: true,
        isError: false,
        title: 'success'
      },
      {
        status: 404,
        isSuccess: false,
        isError: true,
        title: 'error'
      }
    ];

    cases.forEach(function (item) {

      it(item.title, function () {
        poll.setProperties({
          isSuccess: item.isSuccess,
          isError: false
        });
        poll.reloadErrorCallback({
          status: item.status
        }, null, null, null, {});
        expect(poll.get('isError')).to.equal(item.isError);
      });

    });

  });

  describe('#replacePolledData', function () {

    var cases = [
      {
        currentTaskId: undefined,
        data: [],
        result: [],
        title: 'current task id is undefined'
      },
      {
        currentTaskId: null,
        data: [],
        result: [],
        title: 'current task id is null'
      },
      {
        currentTaskId: 0,
        polledData: [
          {
            Tasks: {
              id: 0
            }
          },
          {
            Tasks: {
              id: 1
            }
          }
        ],
        data: [
          {
            Tasks: {
              id: 1
            }
          }
        ],
        result: [
          {
            Tasks: {
              id: 1
            }
          }
        ],
        title: 'current task id is 0, no corresponding task passed'
      },
      {
        currentTaskId: 1,
        polledData: [
          {
            Tasks: {
              id: 0
            }
          }
        ],
        data: [
          {
            Tasks: {
              id: 0
            }
          },
          {
            Tasks: {
              id: 1
            }
          }
        ],
        result: [
          {
            Tasks: {
              id: 0
            }
          },
          {
            Tasks: {
              id: 1
            }
          }
        ],
        title: 'current task id is more than 0, no corresponding task set'
      },
      {
        currentTaskId: 2,
        polledData: [
          {
            Tasks: {
              id: 0
            }
          },
          {
            Tasks: {
              id: 2,
              stdout: 'stdout',
              stderr: 'stderr'
            }
          }
        ],
        data: [
          {
            Tasks: {
              id: 0
            }
          },
          {
            Tasks: {
              id: 2
            }
          },
          {
            Tasks: {
              id: 3
            }
          }
        ],
        result: [
          {
            Tasks: {
              id: 0
            }
          },
          {
            Tasks: {
              id: 2,
              stdout: 'stdout',
              stderr: 'stderr'
            }
          },
          {
            Tasks: {
              id: 3
            }
          }
        ],
        title: 'current task id is more than 0, corresponding task set and passed'
      },
      {
        currentTaskId: 3,
        polledData: [
          {
            Tasks: {
              id: 0
            }
          }
        ],
        data: [
          {
            Tasks: {
              id: 1
            }
          }
        ],
        result: [
          {
            Tasks: {
              id: 1
            }
          }
        ],
        title: 'current task id is more than 0, corresponding task neither set nor passed'
      }
    ];

    cases.forEach(function (item) {

      it(item.title, function () {
        poll.setProperties({
          currentTaskId: item.currentTaskId,
          polledData: item.polledData || null
        });
        poll.replacePolledData(item.data);
        expect(poll.get('polledData').toArray()).to.eql(item.result);
      });

    });

  });

  describe('#calculateProgressByTasks', function () {

    var cases = [
      {
        tasksData: [
          {
            Tasks: {
              status: 'QUEUED'
            }
          },
          {
            Tasks: {
              status: 'QUEUED'
            }
          }
        ],
        result: 9,
        title: 'all tasks pending'
      },
      {
        tasksData: [
          {
            Tasks: {
              status: 'IN_PROGRESS'
            }
          },
          {
            Tasks: {
              status: 'IN_PROGRESS'
            }
          }
        ],
        result: 35,
        title: 'all tasks in progress'
      },
      {
        tasksData: [
          {
            Tasks: {
              status: 'COMPLETED'
            }
          },
          {
            Tasks: {
              status: 'COMPLETED'
            }
          }
        ],
        result: 100,
        title: 'all tasks completed'
      },
      {
        tasksData: [
          {
            Tasks: {
              status: 'FAILED'
            }
          },
          {
            Tasks: {
              status: 'FAILED'
            }
          }
        ],
        result: 100,
        title: 'all tasks failed'
      },
      {
        tasksData: [
          {
            Tasks: {
              status: 'ABORTED'
            }
          },
          {
            Tasks: {
              status: 'ABORTED'
            }
          }
        ],
        result: 100,
        title: 'all tasks aborted'
      },
      {
        tasksData: [
          {
            Tasks: {
              status: 'TIMEDOUT'
            }
          },
          {
            Tasks: {
              status: 'TIMEDOUT'
            }
          }
        ],
        result: 100,
        title: 'all tasks timed out'
      },
      {
        tasksData: [
          {
            Tasks: {
              status: 'QUEUED'
            }
          },
          {
            Tasks: {
              status: 'COMPLETED'
            }
          }
        ],
        result: 55,
        title: 'pending and finished tasks'
      },
      {
        tasksData: [
          {
            Tasks: {
              status: 'IN_PROGRESS'
            }
          },
          {
            Tasks: {
              status: 'FAILED'
            }
          }
        ],
        result: 68,
        title: 'running and finished tasks'
      },
      {
        tasksData: [
          {
            Tasks: {
              status: 'IN_PROGRESS'
            }
          },
          {
            Tasks: {
              status: 'QUEUED'
            }
          }
        ],
        result: 22,
        title: 'running and pending tasks'
      },
      {
        tasksData: [
          {
            Tasks: {
              status: 'IN_PROGRESS'
            }
          },
          {
            Tasks: {
              status: 'QUEUED'
            }
          },
          {
            Tasks: {
              status: 'ABORTED'
            }
          }
        ],
        result: 48,
        title: 'running, pending and finished tasks'
      }
    ];

    cases.forEach(function (item) {

      it(item.title, function () {
        expect(poll.calculateProgressByTasks(item.tasksData)).to.equal(item.result);
      });

    });

  });

  describe('#isPollingFinished', function () {

    var cases = [
      {
        polledData: [
          {
            Tasks: {
              status: 'QUEUED'
            }
          },
          {
            Tasks: {
              status: 'COMPLETED'
            }
          }
        ],
        isPollingFinished: false,
        isSuccess: false,
        isError: false,
        title: 'some queued tasks'
      },
      {
        polledData: [
          {
            Tasks: {
              status: 'IN_PROGRESS'
            }
          },
          {
            Tasks: {
              status: 'COMPLETED'
            }
          }
        ],
        isPollingFinished: false,
        isSuccess: false,
        isError: false,
        title: 'some running tasks'
      },
      {
        polledData: [
          {
            Tasks: {
              status: 'PENDING'
            }
          },
          {
            Tasks: {
              status: 'COMPLETED'
            }
          }
        ],
        isPollingFinished: false,
        isSuccess: false,
        isError: false,
        title: 'some pending tasks'
      },
      {
        polledData: [
          {
            Tasks: {
              status: 'FAILED'
            }
          },
          {
            Tasks: {
              status: 'COMPLETED'
            }
          }
        ],
        isPollingFinished: true,
        isSuccess: false,
        isError: true,
        title: 'all tasks finished, some failed'
      },
      {
        polledData: [
          {
            Tasks: {
              status: 'ABORTED'
            }
          },
          {
            Tasks: {
              status: 'COMPLETED'
            }
          }
        ],
        isPollingFinished: true,
        isSuccess: false,
        isError: true,
        title: 'all tasks finished, some aborted'
      },
      {
        polledData: [
          {
            Tasks: {
              status: 'ABORTED'
            }
          },
          {
            Tasks: {
              status: 'COMPLETED'
            }
          }
        ],
        isPollingFinished: true,
        isSuccess: false,
        isError: true,
        title: 'all tasks finished, some timed out'
      },
      {
        polledData: [
          {
            Tasks: {
              status: 'COMPLETED'
            }
          },
          {
            Tasks: {
              status: 'COMPLETED'
            }
          }
        ],
        isPollingFinished: true,
        isSuccess: true,
        isError: false,
        title: 'all tasks finished successfully'
      }
    ];

    cases.forEach(function (item) {

      describe(item.title, function () {

        var result;

        beforeEach(function () {
          poll.setProperties({
            isSuccess: false,
            isError: false
          });
          result = poll.isPollingFinished(item.polledData);
        });

        it('isPollingFinished', function () {
          expect(result).to.equal(item.isPollingFinished);
        });

        it('isSuccess', function () {
          expect(poll.get('isSuccess')).to.equal(item.isSuccess);
        });

        it('isError', function () {
          expect(poll.get('isError')).to.equal(item.isError);
        });

      });

    });

  });

  describe('#parseInfo', function () {

    var cases = [
      {
        polledData: {
          Requests: {
            id: 1
          }
        },
        replacePolledDataCallCount: 0,
        progress: '0',
        result: false,
        title: 'no corresponding request data'
      },
      {
        polledData: {
          tasks: []
        },
        replacePolledDataCallCount: 1,
        progress: '100',
        result: false,
        title: 'no request id info'
      },
      {
        polledData: {
          Requests: {
            id: 0
          },
          tasks: []
        },
        replacePolledDataCallCount: 1,
        progress: '100',
        result: false,
        title: 'no tasks'
      },
      {
        polledData: {
          Requests: {
            id: 0
          },
          tasks: [
            {
              Tasks: {
                status: 'PENDING'
              }
            },
            {
              Tasks: {
                status: 'COMPLETED'
              }
            }
          ]
        },
        replacePolledDataCallCount: 1,
        progress: '100',
        result: false,
        title: 'not all tasks finished'
      },
      {
        polledData: {
          Requests: {
            id: 0
          },
          tasks: [
            {
              Tasks: {
                status: 'FAILED'
              }
            },
            {
              Tasks: {
                status: 'COMPLETED'
              }
            }
          ]
        },
        replacePolledDataCallCount: 1,
        progress: '100',
        result: true,
        title: 'all tasks finished'
      }
    ];

    cases.forEach(function (item) {

      describe(item.title, function () {

        var result;

        beforeEach(function () {
          sinon.stub(poll, 'replacePolledData', Em.K);
          sinon.stub(poll, 'calculateProgressByTasks').returns(100);
          sinon.stub(poll, 'isPollingFinished', function (data) {
            return data.length > 0 && !(data.someProperty('Tasks.status', 'QUEUED') || data.someProperty('Tasks.status', 'IN_PROGRESS')
              || data.someProperty('Tasks.status', 'PENDING'));
          });
          poll.setProperties({
            requestId: 0,
            progress: '0'
          });
          result = poll.parseInfo(item.polledData);
        });

        afterEach(function () {
          poll.replacePolledData.restore();
          poll.calculateProgressByTasks.restore();
          poll.isPollingFinished.restore();
        });

        it('replacePolledData call', function () {
          expect(poll.replacePolledData.callCount).to.equal(item.replacePolledDataCallCount);
        });

        if (item.replacePolledDataCallCount) {
          it('replacePolledData argument', function () {
            expect(poll.replacePolledData.firstCall.args).to.eql([item.polledData.tasks]);
          });
        }

        it('progress', function () {
          expect(poll.get('progress')).to.equal(item.progress);
        });

        it('result', function () {
          expect(result).to.equal(item.result);
        });

      });

    });

  });

});
