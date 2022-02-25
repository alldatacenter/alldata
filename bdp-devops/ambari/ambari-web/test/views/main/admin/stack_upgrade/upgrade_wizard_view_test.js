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
require('views/main/admin/stack_upgrade/upgrade_wizard_view');
var testHelpers = require('test/helpers');

function getView() {
  var v = App.upgradeWizardView.create({
    failedStatuses: ['FAILED']
  });
  v.reopen({
    controller: Em.Object.create({
      finalizeContext: 'Confirm Finalize',
      upgradeData: Em.Object.create(),
      loadUpgradeData: Em.K,
      setUpgradeItemStatus: Em.K,
      getUpgradeItem: function () {
        return {complete: Em.K};
      }
    })
  });
  v.removeObserver('App.clusterName', v, 'startPolling');
  return v;
}

describe('App.upgradeWizardView', function () {
  var view;

  beforeEach(function () {
    App.ajax.send.restore();
    sinon.stub(App.ajax, 'send').returns({
      complete: Em.clb
    });
    view = getView();
  });

  afterEach(function () {
    clearTimeout(view.get('upgradeItemTimer'));
    view.destroy();
  });

  App.TestAliases.testAsComputedOr(getView(), 'isManualProceedDisabled', ['!isManualDone', 'controller.requestInProgress']);

  App.TestAliases.testAsComputedEqualProperties(getView(), 'isFinalizeItem', 'manualItem.context', 'controller.finalizeContext');

  describe("#upgradeGroups", function () {
    it("upgradeGroups is null", function () {
      view.set('controller.upgradeData.upgradeGroups', null);
      view.propertyDidChange('upgradeGroups');
      expect(view.get('upgradeGroups')).to.be.empty;
    });
    it("upgradeGroups is valid", function () {
      view.set('controller.upgradeData.upgradeGroups', [Em.Object.create()]);
      view.propertyDidChange('upgradeGroups');
      expect(view.get('upgradeGroups')).to.not.be.empty;
    });
  });

  describe("#activeGroup", function () {
    it("status is updated", function () {
      view.reopen({
        activeStatuses: ['IN_PROGRESS'],
        upgradeGroups: [Em.Object.create({
          status: 'IN_PROGRESS'
        })]
      });
      view.propertyDidChange('activeGroup');
      expect(view.get('activeGroup')).to.eql(Em.Object.create({
        status: 'IN_PROGRESS'
      }));
    });
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

  describe("#startPolling()", function () {
    beforeEach(function () {
      sinon.stub(view.get('controller'), 'loadUpgradeData', function () {
        return {
          done: Em.clb
        }
      });
      sinon.stub(view, 'doPolling', Em.K);
      view.set('isLoaded', false);
    });
    afterEach(function () {
      view.get('controller').loadUpgradeData.restore();
      view.doPolling.restore();
    });
    it("clusterName is null", function () {
      App.set('clusterName', null);
      view.startPolling();
      expect(view.doPolling.called).to.be.false;
      expect(view.get('isLoaded')).to.be.false;
    });
    it("clusterName set", function () {
      App.set('clusterName', 'c1');
      view.startPolling();
      expect(view.get('controller').loadUpgradeData.calledOnce).to.be.true;
      expect(view.doPolling.calledOnce).to.be.true;
      expect(view.get('isLoaded')).to.be.true;
    });
  });

  describe("#willInsertElement()", function () {
    beforeEach(function () {
      sinon.stub(view, 'startPolling', Em.K);
    });
    afterEach(function () {
      view.startPolling.restore();
    });
    it("call startPolling()", function () {
      view.willInsertElement();
      expect(view.startPolling.calledOnce).to.be.true;
    });
  });

  describe("#willDestroyElement()", function () {
    it("call startPolling()", function () {
      view.set('isLoaded', true);
      view.willDestroyElement();
      expect(view.get('isLoaded')).to.be.false;
    });
  });

  describe("#doPolling()", function () {
    beforeEach(function () {
      sinon.stub(view.get('controller'), 'loadUpgradeData', function () {
        return {
          done: Em.clb
        }
      });
      sinon.spy(view, 'doPolling');
      this.clock = sinon.useFakeTimers();
      view.doPolling();
      this.clock.tick(App.bgOperationsUpdateInterval);
    });
    afterEach(function () {
      view.get('controller').loadUpgradeData.restore();
      view.doPolling.restore();
      this.clock.restore();
    });
    it("loadUpgradeData is called once", function () {
      expect(view.get('controller').loadUpgradeData.calledOnce).to.be.true;
    });
    it("doPolling is called twice", function () {
      expect(view.doPolling.calledTwice).to.be.true;
    });
  });

  describe("#continue()", function () {
    beforeEach(function () {
      sinon.stub(view.get('controller'), 'setUpgradeItemStatus', Em.K);
      view.continue({context: Em.Object.create({'status': 'HOLDING_FAILED'})});
    });
    afterEach(function () {
      view.get('controller').setUpgradeItemStatus.restore();
    });
    it("setUpgradeItemStatus is called with correct data", function () {
      expect(view.get('controller').setUpgradeItemStatus.calledWith(Em.Object.create({'status': 'HOLDING_FAILED'}), 'FAILED')).to.be.true;
    });
    it("isDetailsOpened is false", function () {
      expect(view.get('isDetailsOpened')).to.be.false;
    });
  });

  describe("#complete()", function () {
    beforeEach(function () {
      sinon.stub(view.get('controller'), 'setUpgradeItemStatus', Em.K);
      view.complete({context: Em.Object.create({'status': 'FAILED'})});
    });
    afterEach(function () {
      view.get('controller').setUpgradeItemStatus.restore();
    });
    it("setUpgradeItemStatus is called with correct data", function () {
      expect(view.get('controller').setUpgradeItemStatus.calledWith(Em.Object.create({'status': 'FAILED'}), 'COMPLETED')).to.be.true;
    });
    it("isManualDone is false", function () {
      expect(view.get('isManualDone')).to.be.false;
    });
  });

  describe("#retry()", function () {
    beforeEach(function () {
      sinon.stub(view.get('controller'), 'setUpgradeItemStatus', Em.K);
      view.retry({context: Em.Object.create({'status': 'FAILED'})});
    });
    afterEach(function () {
      view.get('controller').setUpgradeItemStatus.restore();
    });
    it("setUpgradeItemStatus is called with correct data", function () {
      expect(view.get('controller').setUpgradeItemStatus.calledWith(Em.Object.create({'status': 'FAILED'}), 'PENDING')).to.be.true;
    });
    it("isDetailsOpened is false", function () {
      expect(view.get('isDetailsOpened')).to.be.false;
    });
  });

  describe("#manualItem", function () {

    beforeEach(function () {
      view.reopen({
        activeGroup: Em.Object.create()
      });
    });

    it("no running item", function () {
      view.set('activeGroup.upgradeItems', []);
      view.propertyDidChange('manualItem');
      expect(view.get('manualItem')).to.be.undefined;
    });
    it("running item present", function () {
      view.set('activeGroup.upgradeItems', [Em.Object.create({status: 'HOLDING'})]);
      view.propertyDidChange('manualItem');
      expect(view.get('manualItem')).to.be.eql(Em.Object.create({status: 'HOLDING'}));
    });
  });

  describe("#resetManualDone()", function() {
    it("isManualDone is set to false", function() {
      view.set('isManualDone', true);
      view.propertyDidChange('manualItem');
      expect(view.get('isManualDone')).to.be.false;
    });
  });

  describe("#failedItem", function () {

    beforeEach(function () {
      view.reopen({
        activeGroup: Em.Object.create()
      });
    });

    it("no running item", function () {
      view.set('activeGroup.upgradeItems', []);
      view.propertyDidChange('failedItem');
      expect(view.get('failedItem')).to.be.undefined;
    });
    it("running item present", function () {
      view.set('activeGroup.upgradeItems', [Em.Object.create({status: 'FAILED'})]);
      view.propertyDidChange('failedItem');
      expect(view.get('failedItem')).to.be.eql(Em.Object.create({status: 'FAILED'}));
    });
  });

  describe("#runningItem", function () {

    beforeEach(function () {
      view.reopen({
        activeGroup: Em.Object.create()
      });
    });

    it("no running item", function () {
      view.set('activeGroup.upgradeItems', []);
      view.propertyDidChange('runningItem');
      expect(view.get('runningItem')).to.be.undefined;
    });
    it("running item present", function () {
      view.set('activeGroup.upgradeItems', [
        Em.Object.create({status: 'IN_PROGRESS'})
      ]);
      view.propertyDidChange('runningItem');
      expect(view.get('runningItem')).to.be.eql(Em.Object.create({status: 'IN_PROGRESS'}));
    });
  });

  describe("#noActiveItem", function () {
    beforeEach(function () {
      this.mock = sinon.stub(App, 'get');
    });
    afterEach(function () {
      this.mock.restore();
    });
    [
      {
        data: {
          failedItem: null,
          runningItem: null,
          manualItem: null,
          upgradeState: 'COMPLETED'
        },
        result: false
      },
      {
        data: {
          failedItem: null,
          runningItem: null,
          manualItem: null,
          upgradeState: 'NOT_REQUIRED'
        },
        result: false
      },
      {
        data: {
          failedItem: null,
          runningItem: null,
          manualItem: null,
          upgradeState: 'ABORTED'
        },
        result: false
      },
      {
        data: {
          failedItem: null,
          runningItem: null,
          manualItem: null,
          upgradeState: 'IN_PROGRESS'
        },
        result: true
      },
      {
        data: {
          failedItem: {},
          runningItem: null,
          manualItem: null,
          upgradeState: 'IN_PROGRESS'
        },
        result: false
      },
      {
        data: {
          failedItem: null,
          runningItem: {},
          manualItem: null,
          upgradeState: 'IN_PROGRESS'
        },
        result: false
      },
      {
        data: {
          failedItem: null,
          runningItem: null,
          manualItem: {},
          upgradeState: 'IN_PROGRESS'
        },
        result: false
      }
    ].forEach(function (test) {
        it("failedItem = " + test.data.failedItem +
        ";runningItem = " + test.data.runningItem +
        ";manualItem = " + test.data.manualItem +
        ";upgradeState = " + test.data.upgradeState, function () {
          view.reopen(test.data);
          this.mock.withArgs('upgradeState').returns(test.data.upgradeState);
          view.propertyDidChange('noActiveItem');
          expect(view.get('noActiveItem')).to.equal(test.result);
        });
      });
  });

  describe("#isHoldingState", function () {
    var testCases = [
      {
        data: {
          failedItem: {status: 'PENDING'}
        },
        result: false
      },
      {
        data: {
          failedItem: {status: 'HOLDING_FAILED'}
        },
        result: true
      },
      {
        data: {
          failedItem: {status: 'HOLDING_TIMED_OUT'}
        },
        result: true
      }
    ];
    testCases.forEach(function (test) {
      it('failedItem - ' + test.data.failedItem, function () {
        view.reopen({
          failedItem: test.data.failedItem
        });
        view.propertyDidChange('isHoldingState');
        expect(view.get('isHoldingState')).to.equal(test.result);
      });
    });
  });

  App.TestAliases.testAsComputedAnd(getView(), 'isDowngradeAvailable', ['!controller.isDowngrade', 'controller.downgradeAllowed', '!controller.cantBeStarted']);

  describe("#taskDetails", function () {
    it("runningItem present", function () {
      view.reopen({
        runningItem: Em.Object.create({
          tasks: [{status: "IN_PROGRESS"}]
        })
      });
      view.propertyDidChange('taskDetails');
      expect(view.get('taskDetails')).to.eql({status: "IN_PROGRESS"});
    });
    it("failedItem present", function () {
      view.reopen({
        failedItem: Em.Object.create({
          tasks: [Em.Object.create({status: "FAILED"})]
        }),
        failedStatuses: ['FAILED'],
        runningItem: null
      });
      view.propertyDidChange('taskDetails');
      expect(view.get('taskDetails').get('status')).to.equal('FAILED');
    });
    it("failedItem and runningItem are absent", function () {
      view.reopen({
        failedItem: null,
        runningItem: null
      });
      view.propertyDidChange('taskDetails');
      expect(view.get('taskDetails')).to.be.null;
    });
  });

  describe("#toggleDetails()", function () {
    beforeEach(function () {
      sinon.stub(view, 'toggleProperty', Em.K);
    });
    afterEach(function () {
      view.toggleProperty.restore();
    });
    it("isDetailsOpened is toggled", function () {
      view.toggleDetails();
      expect(view.toggleProperty.calledWith('isDetailsOpened')).to.be.true;
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
        result: Em.I18n.t('admin.stackUpgrade.state.init')
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

  describe("#doUpgradeItemPolling()", function () {
    beforeEach(function () {
      sinon.stub(view.get('controller'), 'getUpgradeItem', function () {
        return {
          complete: Em.clb
        }
      });
      sinon.spy(view, 'doUpgradeItemPolling');
      this.clock = sinon.useFakeTimers();
    });

    afterEach(function () {
      view.get('controller').getUpgradeItem.restore();
      view.doUpgradeItemPolling.restore();
      this.clock.restore();
    });

    it("running item details", function () {
      view.reopen({
        runningItem: {},
        failedItem: null
      });
      view.set('isDetailsOpened', true);
      //doUpgradeItemPolling triggered by observer
      expect(view.get('controller').getUpgradeItem.calledOnce).to.be.true;
      this.clock.tick(App.bgOperationsUpdateInterval);
      expect(view.doUpgradeItemPolling.calledTwice).to.be.true;
    });

    it("failed item details", function () {
      view.reopen({
        failedItem: {},
        runningItem: null
      });
      view.set('isDetailsOpened', true);
      expect(view.get('controller').getUpgradeItem.calledOnce).to.be.true;
      this.clock.tick(App.bgOperationsUpdateInterval);
      expect(view.doUpgradeItemPolling.calledTwice).to.be.true;
    });

    it("details not opened", function () {
      view.set('isDetailsOpened', false);
      //doUpgradeItemPolling triggered by observer
      expect(view.get('controller').getUpgradeItem.called).to.be.false;
    });

  });

  describe('#getSkippedServiceChecks()', function () {

    var cases = [
      {
        isFinalizeItem: false,
        areSkippedServiceChecksLoaded: true,
        ajaxSendCallCount: 0,
        areSkippedServiceChecksLoadedResult: false,
        title: 'no ajax request should be sent away from Finalize step'
      },
      {
        isFinalizeItem: true,
        areSkippedServiceChecksLoaded: true,
        ajaxSendCallCount: 0,
        areSkippedServiceChecksLoadedResult: true,
        title: 'no ajax request should be sent if data is already loaded'
      },
      {
        isFinalizeItem: true,
        areSkippedServiceChecksLoaded: false,
        ajaxSendCallCount: 1,
        areSkippedServiceChecksLoadedResult: true,
        title: 'ajax request should be sent on Finalize step'
      }
    ];

    beforeEach(function () {
      view.set('controller.upgradeId', 1);
    });

    cases.forEach(function (item) {
      describe(item.title, function () {

        beforeEach(function () {
          view.set('controller.areSkippedServiceChecksLoaded', item.areSkippedServiceChecksLoaded);
          view.reopen({
            isFinalizeItem: item.isFinalizeItem
          });
          view.propertyDidChange('isFinalizeItem');
        });

        it('areSkippedServiceChecksLoaded is ' + item.areSkippedServiceChecksLoaded, function () {
          expect(view.get('controller.areSkippedServiceChecksLoaded')).to.equal(item.areSkippedServiceChecksLoadedResult);
        });

        if (item.ajaxSendCallCount) {
          it('request is sent ' + item.ajaxSendCallCount + ' times', function (){
            var args = testHelpers.findAjaxRequest('name', 'admin.upgrade.service_checks');
            expect(args).to.have.property('length').equal(item.ajaxSendCallCount);
          });

          it('upgradeId is 1', function () {
            var args = testHelpers.findAjaxRequest('name', 'admin.upgrade.service_checks');
            expect(args[0].data.upgradeId).to.equal(1);
          });
        }
        else {
          it('request is not sent times', function () {
            var args = testHelpers.findAjaxRequest('name', 'admin.upgrade.service_checks');
            expect(args).not.exists;
          });
        }
      });
    });

  });

  describe('#getSkippedServiceChecksSuccessCallback()', function () {

    var data = {
      items: [
        {
          upgrade_items: [
            {
              tasks: [
                {
                  Tasks: {
                    command_detail: 'SERVICE_CHECK HDFS'
                  }
                }
              ]
            }
          ]
        },
        {
          upgrade_items: [
            {
              tasks: [
                {
                  Tasks: {
                    command_detail: 'SERVICE_CHECK ZOOKEEPER'
                  }
                }
              ]
            },
            {
              tasks: [
                {
                  Tasks: {
                    command_detail: 'SERVICE_CHECK YARN'
                  }
                }
              ]
            },
            {},
            {
              tasks: []
            },
            {
              tasks: [
                {
                  Tasks: null
                },
                {
                  Tasks: {
                    command_detail: 'SERVICE_CHECK HIVE'
                  }
                }
              ]
            },
            {
              tasks: [
                {
                  Tasks: {
                    command_detail: null
                  }
                },
                {
                  Tasks: {
                    command_detail: 'SERVICE_CHECK YARN'
                  }
                }
              ]
            },
            {
              tasks: [
                {
                  Tasks: {
                    command_detail: 'RESTART HDFS'
                  }
                }
              ]
            }
          ]
        }
      ]
    };

    it('should set an array of service names from tha latest service checks step', function () {
      view.getSkippedServiceChecksSuccessCallback(data);
      expect(view.get('controller.skippedServiceChecks')).to.eql(['ZooKeeper', 'YARN', 'Hive']);
    });

  });

  describe("#failedHostsMessage", function() {
    it("is formatted with slaveComponentStructuredInfo", function() {
      view.set('controller.slaveComponentStructuredInfo', {
        hosts: ['host1']
      });
      view.propertyDidChange('failedHostsMessage');
      expect(view.get('failedHostsMessage')).to.equal(Em.I18n.t('admin.stackUpgrade.failedHosts.showHosts').format(1));
    });
  });

  describe("#getSlaveComponentItem()", function() {
    beforeEach(function () {
      sinon.stub(view.get('controller'), 'getUpgradeItem', function () {
        return {
          complete: Em.clb
        }
      });
      view.set('controller.areSlaveComponentFailuresHostsLoaded', false);
    });
    afterEach(function () {
      view.get('controller').getUpgradeItem.restore();
    });

    it("isSlaveComponentFailuresItem is false", function() {
      view.reopen({
        isSlaveComponentFailuresItem: false
      });
      view.getSlaveComponentItem();
      expect(view.get('controller.areSlaveComponentFailuresHostsLoaded')).to.be.false;
    });
    it("isSlaveComponentFailuresItem is true", function() {
      view.reopen({
        isSlaveComponentFailuresItem: true
      });
      view.getSlaveComponentItem();
      expect(view.get('controller').getUpgradeItem.calledOnce).to.be.true;
      expect(view.get('controller.areSlaveComponentFailuresHostsLoaded')).to.be.true;
    });
  });
  it("isSlaveComponentFailuresItem must be true when status is OUT_OF_SYNC", function () {
    view.reopen({
      activeStatuses: ['OUT_OF_SYNC'],
      upgradeGroups: [Em.Object.create({
        status: 'OUT_OF_SYNC'
      })]
    });
    view.propertyDidChange('activeGroup');
    view.set('activeGroup.upgradeItems', [Em.Object.create({
      context: view.get("controller.slaveFailuresContext"),
      status: 'OUT_OF_SYNC'
    })]);

    expect(view.get('isSlaveComponentFailuresItem')).to.be.true;
    expect(view.get('isOutOfSync')).to.be.true;
  });

  describe("#getServiceCheckItem()", function() {
    beforeEach(function () {
      sinon.stub(view.get('controller'), 'getUpgradeItem', function () {
        return {
          complete: Em.clb
        }
      });
      view.set('controller.areServiceCheckFailuresServicenamesLoaded', false);
    });
    afterEach(function () {
      view.get('controller').getUpgradeItem.restore();
    });

    it("isServiceCheckFailuresItem is false", function() {
      view.reopen({
        isServiceCheckFailuresItem: false
      });
      view.getServiceCheckItem();
      expect(view.get('controller.areServiceCheckFailuresServicenamesLoaded')).to.be.false;
    });
    it("isServiceCheckFailuresItem is true", function() {
      view.reopen({
        isServiceCheckFailuresItem: true
      });
      view.getServiceCheckItem();
      expect(view.get('controller').getUpgradeItem.calledOnce).to.be.true;
      expect(view.get('controller.areServiceCheckFailuresServicenamesLoaded')).to.be.true;
    });
  });

  describe("#canSkipFailedItem()", function() {
    beforeEach(function () {
      view.reopen({'failedItem': Em.Object.create({skippable: true}) });
      view.set('controller.upgradeData.Upgrade', {associated_version: '2.1.1'});
      var findResult = [Em.Object.create({repositoryVersion: '2.1.1', isPatch: true})];
      sinon.stub(App.RepositoryVersion, 'find', function(){
        return findResult;
      });
    });

    afterEach(function () {
      App.RepositoryVersion.find.restore();
    })
    it("Should return true if can not find upgrade", function () {
      view.propertyDidChange('canSkipFailedItem');
      expect(view.get('canSkipFailedItem')).to.be.true;
    });

    it("Should return false if upgrade is patch or maint and item is final", function () {
      view.reopen({
        isFinalizeItem: true
      });
      view.propertyDidChange('canSkipFailedItem');
      expect(view.get('canSkipFailedItem')).to.be.false;
    });

    it("Should return true if upgrade is patch or maint and item is not final", function () {

      view.reopen({
        isFinalizeItem: false
      });
      view.propertyDidChange('canSkipFailedItem');

      expect(view.get('canSkipFailedItem')).to.be.true;
    });

  });

});
