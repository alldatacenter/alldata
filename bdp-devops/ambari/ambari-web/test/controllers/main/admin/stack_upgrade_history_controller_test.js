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
require('controllers/main/admin/stack_upgrade_history_controller');
require('utils/string_utils');
var testHelpers = require('test/helpers');
describe('App.MainAdminStackUpgradeHistoryController', function() {

  var controller = App.MainAdminStackUpgradeHistoryController.create({

  });

  describe("#upgradeHistoryUrl", function() {
    before(function () {
      this.mock = sinon.stub(App, 'get');
      this.mock.withArgs('apiPrefix').returns('apiPrefix')
        .withArgs('clusterName').returns('clusterName');
    });
    after(function () {
      this.mock.restore();
    });
    it("should be valid", function() {
      controller.propertyDidChange('upgradeHistoryUrl');
      expect(controller.get('upgradeHistoryUrl')).to.equal('apiPrefix/clusters/clusterName/upgrades?fields=Upgrade');
    });
  });

  describe("#requestStatus", function() {
    beforeEach(function() {
      this.mock = sinon.stub(App, 'get');
    });
    afterEach(function() {
      this.mock.restore();
    });
    it("state should be what the record states", function() {
      this.mock.returns(false);
      controller.set('upgradeData', { Upgrade: {request_status: 'COMPLETED'}});
      controller.propertyDidChange('requestStatus');
      expect(controller.get('requestStatus')).to.equal('COMPLETED');
    });

    it("upgradeData is null", function() {
      this.mock.returns(false);
      controller.set('upgradeData', null);
      controller.propertyDidChange('requestStatus');
      expect(controller.get('requestStatus')).to.be.empty;
    });
  });

  describe("#loadStackUpgradeRecord()", function() {
    it("get upgrade record data", function() {
      controller.set('currentUpgradeRecord', Em.Object.create({'requestId':1, 'direction':'DOWNGRADE'}));
      controller.loadStackUpgradeRecord();
      var args = testHelpers.findAjaxRequest('name', 'admin.upgrade.data');
      expect(args[0]).to.exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].data).to.be.eql({
        id: 1
      });
    });
  });

  describe("#loadUpgradeRecordSuccessCallback()", function() {
    it("correct data", function() {
      var data = {
        "Upgrade": {
          "request_status": "COMPLETED"
        },
        "upgrade_groups": [
          {
            "UpgradeGroup": {
              "id": 1
            },
            "upgrade_items": []
          }
        ]};
      controller.loadUpgradeRecordSuccessCallback(data);
      expect(controller.get('upgradeData')).to.be.not.null;
    });

    it("data is null", function() {
      var data = null;
      controller.set('upgradeData', null)
      controller.loadUpgradeRecordSuccessCallback(data);
      expect(controller.get('upgradeData')).to.be.null;
    });
  });

  describe("#getUpgradeItem()", function() {
    it("default callback", function() {
      var item = Em.Object.create({
        request_id: 1,
        group_id: 2,
        stage_id: 3
      });
      controller.getUpgradeItem(item);
      var args = testHelpers.findAjaxRequest('name', 'admin.upgrade.upgrade_item');
      expect(args[0]).to.exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].success).to.be.equal('getUpgradeItemSuccessCallback');
      expect(args[0].data).to.be.eql({
        upgradeId: 1,
        groupId: 2,
        stageId: 3
      });
    });
  });

  describe("#getUpgradeTask()", function() {

    it("default callback", function() {
      var task = Em.Object.create({
        request_id: 1,
        group_id: 2,
        stage_id: 3,
        id: 4
      });
      controller.getUpgradeTask(task);
      var args = testHelpers.findAjaxRequest('name', 'admin.upgrade.upgrade_task');
      expect(args[0]).to.exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].success).to.be.equal('getUpgradeTaskSuccessCallback');
      expect(args[0].data).to.be.eql({
        upgradeId: 1,
        groupId: 2,
        stageId: 3,
        taskId: 4,
        task: task
      });
    });
  });

  describe('#getUpgradeTaskSuccessCallback', function() {

    it('should update volatile properties', function() {
      var data = {
        Tasks: {
          status: 'IN_PROGRESS',
          id: 1,
          stderr: 'Error',
          error_log: '',
          host_name: 'host1',
          output_log: '',
          stdout: ''
        }
      };
      var params = {
        task: Em.Object.create()
      };
      controller.getUpgradeTaskSuccessCallback(data, {}, params);
      expect(params.task).to.be.eql(Em.Object.create({
        status: 'IN_PROGRESS',
        stderr: 'Error',
        error_log: '',
        host_name: 'host1',
        output_log: '',
        stdout: ''
      }))
    });
  });
});
