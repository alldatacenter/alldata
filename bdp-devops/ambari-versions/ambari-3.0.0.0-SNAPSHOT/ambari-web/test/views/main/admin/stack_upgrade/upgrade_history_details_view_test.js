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
require('views/main/admin/stack_upgrade/upgrade_history_details_view');

describe('App.MainAdminStackUpgradeHistoryDetailsView', function () {
  var view;

  beforeEach(function () {
    view = App.MainAdminStackUpgradeHistoryDetailsView.create({
      controller: {
        currentUpgradeRecord: App.StackUpgradeHistory.createRecord({
          'requestId':1,
          'direction':'DOWNGRADE',
        }),
      }
    });
  });

  afterEach(function () {
    view.destroy();
  });

  describe("#overallProgress", function () {
    it("progress is 1.9", function () {
      view.set('controller.upgradeData', {
        Upgrade: {
          progress_percent: 1.9
        }
      });
      expect(view.get('overallProgress')).to.equal(1);
    });
    it("progress is 1", function () {
      view.set('controller.upgradeData', {
        Upgrade: {
          progress_percent: 1
        }
      });
      expect(view.get('overallProgress')).to.equal(1);
    });
  });

  describe("#willInsertElement()", function() {
    beforeEach(function () {
      sinon.spy(view.get('controller'), 'loadStackUpgradeRecord');
    });
    afterEach(function () {
      view.get('controller').loadStackUpgradeRecord.restore();
    });
    it("load data by controller is called once", function() {
      view.set('controller.currentUpgradeRecord', App.StackUpgradeHistory.createRecord({
        'requestId':1,
        'direction':'DOWNGRADE',
      }));
      view.willInsertElement();
      expect(view.get('controller').loadStackUpgradeRecord.calledOnce).to.be.true;
    });
  });

  describe("#willDestroyElement()", function () {
    it("reset ready flag", function () {
      view.set('isReady', true);
      view.willDestroyElement();
      expect(view.get('isReady')).to.be.false;
    });
  });

  describe("#upgradeStatusLabel", function () {
    beforeEach(function () {
      Em.setFullPath(view, 'controller.upgradeData.Upgrade', {});
    });

    [
      {
        data: {
          status: 'QUEUED',
          isDowngrade: false
        },
        result: Em.I18n.t('admin.stackUpgrade.state.inProgress')
      },
      {
        data: {
          status: 'PENDING',
          isDowngrade: false
        },
        result: Em.I18n.t('admin.stackUpgrade.state.inProgress')
      },
      {
        data: {
          status: 'IN_PROGRESS',
          isDowngrade: false
        },
        result: Em.I18n.t('admin.stackUpgrade.state.inProgress')
      },
      {
        data: {
          status: 'COMPLETED',
          isDowngrade: false
        },
        result: Em.I18n.t('admin.stackUpgrade.state.completed')
      },
      {
        data: {
          status: 'ABORTED',
          isDowngrade: false
        },
        result: Em.I18n.t('admin.stackUpgrade.state.paused')
      },
      {
        data: {
          status: 'TIMEDOUT',
          isDowngrade: false
        },
        result: Em.I18n.t('admin.stackUpgrade.state.paused')
      },
      {
        data: {
          status: 'FAILED',
          isDowngrade: false
        },
        result: Em.I18n.t('admin.stackUpgrade.state.paused')
      },
      {
        data: {
          status: 'HOLDING_FAILED',
          isDowngrade: false
        },
        result: Em.I18n.t('admin.stackUpgrade.state.paused')
      },
      {
        data: {
          status: 'HOLDING_TIMEDOUT',
          isDowngrade: false
        },
        result: Em.I18n.t('admin.stackUpgrade.state.paused')
      },
      {
        data: {
          status: 'HOLDING',
          isDowngrade: false
        },
        result: Em.I18n.t('admin.stackUpgrade.state.paused')
      },
      {
        data: {
          status: '',
          isDowngrade: false
        },
        result: ''
      },
      {
        data: {
          status: 'QUEUED',
          isDowngrade: true
        },
        result: Em.I18n.t('admin.stackUpgrade.state.inProgress.downgrade')
      },
      {
        data: {
          status: 'PENDING',
          isDowngrade: true
        },
        result: Em.I18n.t('admin.stackUpgrade.state.inProgress.downgrade')
      },
      {
        data: {
          status: 'IN_PROGRESS',
          isDowngrade: true
        },
        result: Em.I18n.t('admin.stackUpgrade.state.inProgress.downgrade')
      },
      {
        data: {
          status: 'COMPLETED',
          isDowngrade: true
        },
        result: Em.I18n.t('admin.stackUpgrade.state.completed.downgrade')
      },
      {
        data: {
          status: 'ABORTED',
          isDowngrade: true
        },
        result: Em.I18n.t('admin.stackUpgrade.state.paused.downgrade')
      },
      {
        data: {
          status: 'TIMEDOUT',
          isDowngrade: true
        },
        result: Em.I18n.t('admin.stackUpgrade.state.paused.downgrade')
      },
      {
        data: {
          status: 'FAILED',
          isDowngrade: true
        },
        result: Em.I18n.t('admin.stackUpgrade.state.paused.downgrade')
      },
      {
        data: {
          status: 'HOLDING_FAILED',
          isDowngrade: true
        },
        result: Em.I18n.t('admin.stackUpgrade.state.paused.downgrade')
      },
      {
        data: {
          status: 'HOLDING_TIMEDOUT',
          isDowngrade: true
        },
        result: Em.I18n.t('admin.stackUpgrade.state.paused.downgrade')
      },
      {
        data: {
          status: 'HOLDING',
          isDowngrade: true
        },
        result: Em.I18n.t('admin.stackUpgrade.state.paused.downgrade')
      }
    ].forEach(function (test) {
        it('status = ' + test.data.status + ", isDowngrade = " + test.data.isDowngrade, function () {
          view.set('controller.isDowngrade', test.data.isDowngrade);
          view.set('controller.upgradeData.Upgrade.request_status', test.data.status);
          view.propertyDidChange('upgradeStatusLabel');
          expect(view.get('upgradeStatusLabel')).to.equal(test.result);
        });
      });
  });
});