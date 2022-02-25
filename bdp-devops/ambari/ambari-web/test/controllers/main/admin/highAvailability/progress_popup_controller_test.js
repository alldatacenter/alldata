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

require('controllers/main/admin/highAvailability/progress_popup_controller');
var testHelpers = require('test/helpers');

describe('App.HighAvailabilityProgressPopupController', function () {

  var controller;

  beforeEach(function () {
    controller = App.HighAvailabilityProgressPopupController.create();
  });

  after(function () {
    controller.destroy();
  });

  describe('#startTaskPolling', function () {

    describe('should start task polling', function () {

      beforeEach(function () {
        controller.startTaskPolling(1, 2);
      });

      it('isTaskPolling = true', function () {
        expect(controller.get('isTaskPolling')).to.be.true;
      });

      it('taskInfo.id = 2', function () {
        expect(controller.get('taskInfo.id')).to.be.equal(2);
      });

      it('taskInfo.requestId = 1', function () {
        expect(controller.get('taskInfo.requestId')).to.be.equal(1);
      });

      it('App.updater.run is called once', function () {
        expect(App.updater.run.calledOnce).to.be.true;
      });

      it('App.updater.immediateRun is called once', function () {
        expect(App.updater.immediateRun.calledOnce).to.be.true;
      });

    });

  });

  describe('#stopTaskPolling', function () {

    it('should stop task polling', function () {
      controller.stopTaskPolling();
      expect(controller.get('isTaskPolling')).to.be.false;
    });

  });

  describe('#updateTask', function () {

    it('should send polling request', function () {
      controller.updateTask();
      var args = testHelpers.findAjaxRequest('name', 'background_operations.get_by_task');
      expect(args).to.exists;
    });

  });

  describe('#updateTaskSuccessCallback', function () {

    beforeEach(function () {
      controller.reopen({
        taskInfo: {}
      });
    });

    var cases = [
        {
          status: 'FAILED',
          isTaskPolling: false
        },
        {
          status: 'COMPLETED',
          isTaskPolling: false
        },
        {
          status: 'TIMEDOUT',
          isTaskPolling: false
        },
        {
          status: 'ABORTED',
          isTaskPolling: false
        },
        {
          status: 'QUEUED',
          isTaskPolling: true
        },
        {
          status: 'IN_PROGRESS',
          isTaskPolling: true
        }
      ],
      tasks = {
        stderr: 'error',
        stdout: 'output',
        output_log: 'output-log.txt',
        error_log: 'error-log.txt'
      },
      title = '{0}polling task if it\'s status is {1}';

    cases.forEach(function (item) {
      var message = title.format(item.isTaskPolling ? '' : 'not ', item.status);
      describe(message, function () {

        beforeEach(function () {
          controller.updateTaskSuccessCallback({
            Tasks: $.extend(tasks, {
              status: item.status
            })
          });
        });

        it('stderr is valid', function () {
          expect(controller.get('taskInfo.stderr')).to.equal('error');
        });

        it('stdout is valid', function () {
          expect(controller.get('taskInfo.stdout')).to.equal('output');
        });

        it('outputLog is valid', function () {
          expect(controller.get('taskInfo.outputLog')).to.equal('output-log.txt');
        });

        it('errorLog is valid', function () {
          expect(controller.get('taskInfo.errorLog')).to.equal('error-log.txt');
        });

        it('isTaskPolling is valid', function () {
          expect(controller.get('isTaskPolling')).to.equal(item.isTaskPolling);
        });

      });
    });

  });

  describe('#getHosts', function () {

    var cases = [
      {
        name: 'background_operations.get_by_request',
        success: 'onGetHostsSuccess',
        title: 'default background operation polling'
      },
      {
        stageId: 0,
        name: 'common.request.polling',
        stageIdPassed: '0',
        successCallback: 's',
        success: 's',
        title: 'polling by stage, stageId = 0'
      },
      {
        stageId: 1,
        name: 'common.request.polling',
        stageIdPassed: 1,
        successCallback: null,
        success: 'onGetHostsSuccess',
        title: 'polling by stage'
      }
    ];

    cases.forEach(function (item) {
      describe(item.title, function () {

        beforeEach(function () {
          controller.setProperties({
            requestIds: [1, 2],
            stageId: item.stageId
          });
          controller.getHosts(item.successCallback);
          this.bgArgs = testHelpers.filterAjaxRequests('name', 'background_operations.get_by_request');
          this.pollingArgs = testHelpers.filterAjaxRequests('name', 'common.request.polling');
          this.args = item.name === 'background_operations.get_by_request' ? this.bgArgs : this.pollingArgs;
        });

        it('two requests are sent', function () {
          expect(this.args.length).to.be.equal(2);
        });

        it('1st call name is valid', function () {
          expect(this.args[0][0].name).to.equal(item.name);
        });

        it('2nd call name is valid', function () {
          expect(this.args[1][0].name).to.equal(item.name);
        });

        it('1st stageId is valid', function () {
          expect(this.args[0][0].data.stageId).to.eql(item.stageIdPassed);
        });

        it('2nd stageId is valid', function () {
          expect(this.args[1][0].data.stageId).to.eql(item.stageIdPassed);
        });

        it('success callback for first request', function () {
          expect(this.args[0][0].success).to.equal(item.success);
        });

        it('success callback for second request', function () {
          expect(this.args[1][0].success).to.equal(item.success);
        });

      });
    });

  });

  describe("#initPopup()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'getHosts');
      sinon.stub(controller, 'setProperties');
    });

    afterEach(function() {
      controller.getHosts.restore();
      controller.setProperties.restore();
    });

    it("App.ModalPopup.show should be called", function() {
      controller.initPopup(null, null, null, true, null);
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });

    it("setProperties should be called", function() {
      controller.initPopup('popupTitle', [], {}, false, 1);
      expect(controller.setProperties.calledWith({
        progressController: {},
        popupTitle: 'popupTitle',
        requestIds: [],
        hostsData: [],
        stageId: 1
      })).to.be.true;
    });

    it("getHosts should be called", function() {
      controller.initPopup(null, null, null, false, null);
      expect(controller.getHosts.calledOnce).to.be.true;
    });
  });

  describe("#onGetHostsSuccess()", function () {
    var spinner = Em.Object.create({
      hide: Em.K
    });

    beforeEach(function() {
      sinon.stub(controller, 'calculateHostsData');
      sinon.stub(App.HostPopup, 'initPopup');
      sinon.stub(controller, 'isRequestRunning').returns(true);
      sinon.stub(controller, 'addObserver');
      sinon.stub(controller, 'doPolling');
      sinon.stub(spinner, 'hide');
      controller.setProperties({
        requestIds: [1],
        hostsData: [],
        popupTitle: 'popupTitle',
        spinnerPopup: spinner,
        progressController: {
          name: 'mainAdminStackAndUpgradeController'
        }
      });
      controller.onGetHostsSuccess({});
    });

    afterEach(function() {
      controller.calculateHostsData.restore();
      App.HostPopup.initPopup.restore();
      controller.isRequestRunning.restore();
      controller.addObserver.restore();
      controller.doPolling.restore();
      spinner.hide.restore();
    });

    it("calculateHostsData should be called", function() {
      expect(controller.calculateHostsData.calledWith([{}])).to.be.true;
    });

    it("App.HostPopup.initPopup should be called", function() {
      expect(App.HostPopup.initPopup.calledWith('popupTitle', controller)).to.be.true;
    });

    it("addObserver should be called", function() {
      expect(controller.addObserver.calledWith('progressController.logs.length', controller, 'getDataFromProgressController')).to.be.true;
    });

    it("spinnerPopup.hide should be called", function() {
      expect(spinner.hide.calledOnce).to.be.true;
    });

    it("doPolling should be called", function() {
      expect(controller.doPolling.calledOnce).to.be.true;
    });
  });

  describe("#calculateHostsData()", function () {

    beforeEach(function() {
      sinon.stub(App, 'dateTime').returns('dateTime');
      sinon.stub(controller, 'isRequestRunning').returns(false);
      sinon.stub(controller, 'removeObserver');
    });

    afterEach(function() {
      App.dateTime.restore();
      controller.isRequestRunning.restore();
      controller.removeObserver.restore();
    });

    it("calculate data", function() {
      var data = [
        {
          tasks: [
            {
              Tasks: {
                name: 't1',
                host_name: 'host1'
              }
            },
            {
              Tasks: {
                name: 't2',
                host_name: 'host1'
              }
            }
          ]
        }
      ];
      controller.setProperties({
        requestIds: [1],
        popupTitle: 'popupTitle'
      });
      controller.calculateHostsData(data);
      expect(controller.get('services')).to.be.eql([
        {
          "id": 1,
          "name": "popupTitle",
          "hosts": [
            {
              "name": "host1",
              "publicName": "host1",
              "logTasks": [
                {
                  "Tasks": {
                    "name": "t1",
                    "host_name": "host1"
                  }
                },
                {
                  "Tasks": {
                    "name": "t2",
                    "host_name": "host1"
                  }
                }
              ]
            }
          ]
        }
      ]);
      expect(controller.get('serviceTimestamp')).to.be.equal('dateTime');
      expect(controller.removeObserver.calledWith('progressController.logs.length', controller, 'getDataFromProgressController')).to.be.true;
    });
  });

  describe("#getDataFromProgressController()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'calculateHostsData');
    });

    afterEach(function() {
      controller.calculateHostsData.restore();
    });

    it("empty logs", function() {
      controller.set('progressController', Em.Object.create({
        logs: []
      }));
      controller.getDataFromProgressController();
      expect(controller.calculateHostsData.calledOnce).to.be.false;
    });

    it("filtered logs", function() {
      controller.setProperties({
        progressController: Em.Object.create({
          logs: [
            {
              Tasks: {
                stage_id: 1,
                request_id: 1
              }
            }
          ]
        }),
        stageId: 1,
        hostsData: [
          {
            Requests: {
              id: 1
            }
          }
        ]
      });
      controller.getDataFromProgressController();
      expect(controller.calculateHostsData.calledWith([
        {
          Requests: {
            id: 1
          },
          tasks: [
            {
              Tasks: {
                stage_id: 1,
                request_id: 1
              }
            }
          ]
        }
      ])).to.be.true;
    });
  });

  describe("#isRequestRunning()", function () {
    var testCases = [
      {
        data: [
          {
            Requests: {
              task_count: 1,
              aborted_task_count: 1,
              completed_task_count: 0,
              failed_task_count: 0,
              timed_out_task_count: 0,
              queued_task_count: 0
            }
          }
        ],
        expected: false
      },
      {
        data: [
          {
            Requests: {
              task_count: 1,
              aborted_task_count: 0,
              completed_task_count: 1,
              failed_task_count: 0,
              timed_out_task_count: 0,
              queued_task_count: 0
            }
          }
        ],
        expected: false
      },
      {
        data: [
          {
            Requests: {
              task_count: 1,
              aborted_task_count: 0,
              completed_task_count: 0,
              failed_task_count: 1,
              timed_out_task_count: 0,
              queued_task_count: 0
            }
          }
        ],
        expected: false
      },
      {
        data: [
          {
            Requests: {
              task_count: 1,
              aborted_task_count: 0,
              completed_task_count: 0,
              failed_task_count: 0,
              timed_out_task_count: 1,
              queued_task_count: 0
            }
          }
        ],
        expected: false
      },
      {
        data: [
          {
            Requests: {
              task_count: 1,
              aborted_task_count: 0,
              completed_task_count: 0,
              failed_task_count: 0,
              timed_out_task_count: 0,
              queued_task_count: 1
            }
          }
        ],
        expected: true
      },
      {
        data: [
          {
            Requests: {
              task_count: 1,
              aborted_task_count: 0,
              completed_task_count: 0,
              failed_task_count: 0,
              timed_out_task_count: 0,
              queued_task_count: 0
            }
          }
        ],
        expected: true
      }
    ];

    testCases.forEach(function(test) {
      it("request: " + JSON.stringify(test.data), function() {
        expect(controller.isRequestRunning(test.data)).to.be.equal(test.expected);
      });
    });
  });

});
