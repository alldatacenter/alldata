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
require('controllers/main/admin/stack_and_upgrade_controller');
require('utils/string_utils');
var testHelpers = require('test/helpers');

describe('App.MainAdminStackAndUpgradeController', function() {

  var controller = App.MainAdminStackAndUpgradeController.create({
    getDBProperty: Em.K,
    setDBProperty: Em.K,
    setDBProperties: Em.K,
    getDBProperties: Em.K
  });

  controller.removeObserver('App.upgradeState', controller, 'finish');


  describe("#realRepoUrl", function() {
    before(function () {
      this.mock = sinon.stub(App, 'get');
      this.mock.withArgs('apiPrefix').returns('apiPrefix');
    });
    after(function () {
      this.mock.restore();
    });
    it("should be valid", function() {
      var expected = 'apiPrefix/stacks?fields=versions/repository_versions/RepositoryVersions,' +
        'versions/repository_versions/operating_systems/*,versions/repository_versions/operating_systems/repositories/*';
      controller.propertyDidChange('realRepoUrl');
      expect(controller.get('realRepoUrl')).to.equal(expected);
    });
  });

  describe("#realStackUrl", function() {
    before(function () {
      this.mock = sinon.stub(App, 'get');
      this.mock.withArgs('apiPrefix').returns('apiPrefix')
        .withArgs('clusterName').returns('clusterName');
    });
    after(function () {
      this.mock.restore();
    });
    it("should be valid", function() {
      controller.propertyDidChange('realStackUrl');
      expect(controller.get('realStackUrl')).to.equal('apiPrefix/clusters/clusterName/stack_versions?fields=*,repository_versions/*,repository_versions/operating_systems/OperatingSystems/*,repository_versions/operating_systems/repositories/*');
    });
  });

  describe("#realUpdateUrl", function() {
    before(function () {
      this.mock = sinon.stub(App, 'get');
      this.mock.withArgs('apiPrefix').returns('apiPrefix')
        .withArgs('clusterName').returns('clusterName');
    });
    after(function () {
      this.mock.restore();
    });
    it("realUpdateUrl is valid", function() {
      controller.propertyDidChange('realUpdateUrl');
      expect(controller.get('realUpdateUrl')).to.equal('apiPrefix/clusters/clusterName/stack_versions?fields=ClusterStackVersions/*');
    });
  });

  describe("#requestStatus", function() {

    beforeEach(function() {
      this.mock = sinon.stub(App, 'get');
    });
    afterEach(function() {
      this.mock.restore();
    });

    it("App.upgradeSuspended is true", function() {
      this.mock.returns(true);
      controller.set('upgradeData', { Upgrade: {request_status: 'ABORTED'}});
      controller.propertyDidChange('requestStatus');
      expect(controller.get('requestStatus')).to.equal('SUSPENDED');
    });

    it("state not ABORTED", function() {
      this.mock.returns(false);
      controller.set('upgradeData', { Upgrade: {request_status: 'NOT_REQUIRED'}});
      controller.propertyDidChange('requestStatus');
      expect(controller.get('requestStatus')).to.equal('NOT_REQUIRED');
    });

    it("upgradeData is null", function() {
      this.mock.returns(false);
      controller.set('upgradeData', null);
      controller.propertyDidChange('requestStatus');
      expect(controller.get('requestStatus')).to.equal('INIT');
    });
  });

  describe("#load()", function() {
    beforeEach(function(){
      sinon.stub(controller, 'loadStackVersionsToModel').returns({
        done: Em.clb
      });
      sinon.stub(controller, 'loadRepoVersionsToModel').returns({
        done: Em.clb
      });
      sinon.stub(controller, 'loadCompatibleVersions').returns({
        done: Em.clb
      });
      sinon.stub(App.StackVersion, 'find').returns([Em.Object.create({
        state: 'CURRENT',
        repositoryVersion: {
          id: '1',
          stackVersionType: 'HDP',
          repositoryVersion: '2.2',
          displayName: 'HDP-2.2'
        }
      })]);
      controller.load();
    });
    afterEach(function(){
      controller.loadStackVersionsToModel.restore();
      controller.loadRepoVersionsToModel.restore();
      controller.loadCompatibleVersions.restore();
      App.StackVersion.find.restore();
    });
    it('loadStackVersionsToModel called with valid arguments', function () {
      expect(controller.loadStackVersionsToModel.calledWith(true)).to.be.true;
    });
    it('loadRepoVersionsToModel called once', function () {
      expect(controller.loadRepoVersionsToModel.calledOnce).to.be.true;
    });
    it('loadCompatibleVersions called once', function () {
      expect(controller.loadCompatibleVersions.calledOnce).to.be.true;
    });
    it('currentVersion is corrent', function () {
      expect(controller.get('currentVersion')).to.eql({
        "id": "1",
        "stack_name": 'HDP',
        "repository_version": "2.2",
        "repository_name": "HDP-2.2"
      });
    });
  });

  describe("#loadUpgradeData()", function() {

    it("get entire data", function() {
      controller.set('upgradeId', 1);
      controller.loadUpgradeData();
      var args = testHelpers.findAjaxRequest('name', 'admin.upgrade.data');
      expect(args[0]).to.exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].data).to.be.eql({
        id: 1
      });
    });
    it("get only state", function() {
      controller.set('upgradeId', 1);
      controller.loadUpgradeData(true);
      var args = testHelpers.findAjaxRequest('name', 'admin.upgrade.state');
      expect(args[0]).to.exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].data).to.be.eql({
        id: 1
      });
    });
    it("upgrade id is null", function() {
      controller.set('upgradeId', null);
      controller.loadUpgradeData();
      var args = testHelpers.findAjaxRequest('name', 'admin.upgrade.state');
      var args2 = testHelpers.findAjaxRequest('name', 'admin.upgrade.data');
      expect(args).to.not.exists;
      expect(args2).to.not.exists;
    });
  });

  describe("#loadUpgradeDataSuccessCallback()", function() {
    var retryCases = [
      {
        isRetryPendingInitial: true,
        status: 'ABORTED',
        isRetryPending: true,
        requestInProgress: true,
        title: 'retry request not yet applied'
      },
      {
        isRetryPendingInitial: true,
        status: 'UPGRADING',
        isRetryPending: false,
        requestInProgress: false,
        title: 'retry request applied'
      },
      {
        isRetryPendingInitial: false,
        status: 'ABORTED',
        isRetryPending: false,
        requestInProgress: true,
        title: 'no retry request sent'
      },
      {
        isRetryPendingInitial: false,
        status: 'UPGRADING',
        isRetryPending: false,
        requestInProgress: true,
        title: 'upgrade wasn\'t aborted'
      }
    ];
    beforeEach(function () {
      sinon.stub(controller, 'updateUpgradeData', Em.K);
      sinon.stub(controller, 'setDBProperty', Em.K);
      sinon.stub(controller, 'finish');
    });
    afterEach(function () {
      controller.updateUpgradeData.restore();
      controller.setDBProperty.restore();
      controller.finish.restore();
      App.set('upgradeState', 'NOT_REQUIRED');
    });

    it("correct data", function() {
      var data = {
        "Upgrade": {
          "request_status": "UPGRADED"
        },
        "upgrade_groups": [
          {
            "UpgradeGroup": {
              "id": 1
            },
            "upgrade_items": []
          }
        ]};
      controller.loadUpgradeDataSuccessCallback(data);
      expect(App.get('upgradeState')).to.equal('UPGRADED');
      expect(controller.updateUpgradeData.calledOnce).to.be.true;
      expect(controller.setDBProperty.calledWith('upgradeState', 'UPGRADED')).to.be.true;
    });

    it("data is null", function() {
      var data = null;
      controller.loadUpgradeDataSuccessCallback(data);
      expect(controller.updateUpgradeData.called).to.be.false;
      expect(controller.setDBProperty.called).to.be.false;
    });

    it("finish should be called", function() {
      var data = {
        "Upgrade": {
          "request_status": "COMPLETED"
        }
      };
      controller.loadUpgradeDataSuccessCallback(data);
      expect(controller.finish.calledOnce).to.be.true;
    });

    retryCases.forEach(function (item) {
      it(item.title, function () {
        var data = {
          "Upgrade": {
            "request_status": item.status
          }
        };
        controller.setProperties({
          isRetryPending: item.isRetryPendingInitial,
          requestInProgress: true
        });
        controller.loadUpgradeDataSuccessCallback(data);
        expect(controller.getProperties(['isRetryPending', 'requestInProgress'])).to.eql({
          isRetryPending: item.isRetryPending,
          requestInProgress: item.requestInProgress
        });
      });
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
    it("custom callback", function() {
      var item = Em.Object.create({
        request_id: 1,
        group_id: 2,
        stage_id: 3
      });
      controller.getUpgradeItem(item, 'customCallback');
      var args = testHelpers.findAjaxRequest('name', 'admin.upgrade.upgrade_item');
      expect(args[0]).to.exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].success).to.be.equal('customCallback');
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

  describe("#openUpgradeDialog()", function () {
    var mock = {
      observer: Em.K
    };
    beforeEach(function () {
      sinon.stub(App.router, 'transitionTo', Em.K);
      sinon.spy(mock, 'observer');
      this.mockAuthorized = sinon.stub(App, 'isAuthorized');
      Em.addObserver(App, 'upgradeSuspended', mock, 'observer');
    });
    afterEach(function () {
      App.router.transitionTo.restore();
      mock.observer.restore();
      this.mockAuthorized.restore();
      Em.removeObserver(App, 'upgradeSuspended', mock, 'observer');
    });

    it('should open dialog', function () {
      this.mockAuthorized.returns(true);
      controller.openUpgradeDialog();
      expect(App.router.transitionTo.calledWith('admin.stackUpgrade')).to.be.true;
    });

    it('upgradeSuspended should receive actual value', function () {
      this.mockAuthorized.returns(true);
      controller.openUpgradeDialog();
      expect(mock.observer.calledOnce).to.be.true;
    });

    it('should not open dialog', function () {
      this.mockAuthorized.returns(false);
      controller.openUpgradeDialog();
      expect(App.router.transitionTo.called).to.be.false;
    });

    it('should not open dialog, isWizardRestricted=true', function () {
      this.mockAuthorized.returns(true);
      controller.set('isWizardRestricted', true);
      controller.openUpgradeDialog();
      expect(App.router.transitionTo.called).to.be.false;
    });

    it('upgradeSuspended should not receive value', function () {
      this.mockAuthorized.returns(false);
      controller.openUpgradeDialog();
      expect(mock.observer.called).to.be.false;
    });

    it('upgradeSuspended should not receive value, isWizardRestricted=true', function () {
      this.mockAuthorized.returns(true);
      controller.set('isWizardRestricted', true);
      controller.openUpgradeDialog();
      expect(mock.observer.called).to.be.false;
    });
  });

  describe("#runPreUpgradeCheck()", function() {
    it("make ajax call", function() {
      controller.runPreUpgradeCheck(Em.Object.create({
        id: '1',
        repositoryVersion: '2.2',
        displayName: 'HDP-2.2',
        upgradeType: 'ROLLING',
        skipComponentFailures: false,
        skipSCFailures: false
      }));
      var args = testHelpers.findAjaxRequest('name', 'admin.upgrade.pre_upgrade_check');
      expect(args[0]).to.exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].data).to.be.eql({
        id: '1',
        value: '2.2',
        label: 'HDP-2.2',
        type: 'ROLLING',
        targetStack: "HDP-2.2",
        skipComponentFailures: 'false',
        skipSCFailures: 'false'
      });
    });
  });

  describe("#runPreUpgradeCheckSuccess()", function () {
    var cases = [
      {
        check: {
          "check": "Work-preserving RM/NM restart is enabled in YARN configs",
          "status": "FAIL",
          "reason": "FAIL",
          "failed_on": [],
          "check_type": "SERVICE"
        },
        showClusterCheckPopupCalledCount: 1,
        upgradeCalledCount: 0,
        title: 'popup is displayed if fails are present'
      },
      {
        check: {
          "check": "Configuration Merge Check",
          "status": "WARNING",
          "reason": "Conflict",
          "failed_on": [],
          "failed_detail": [
            {
              type: 't0',
              property: 'p0',
              current: 'c0',
              new_stack_value: 'n0',
              result_value: 'n0'
            },
            {
              type: 't1',
              property: 'p1',
              current: 'c1',
              new_stack_value: null,
              result_value: 'c1'
            },
            {
              type: 't2',
              property: 'p2',
              current: 'c2',
              new_stack_value: null,
              result_value: null
            }
          ],
          "check_type": "CLUSTER",
          "id": "CONFIG_MERGE"
        },
        showClusterCheckPopupCalledCount: 1,
        upgradeCalledCount: 0,
        configs: [
          {
            type: 't0',
            name: 'p0',
            currentValue: 'c0',
            recommendedValue: 'n0',
            resultingValue: 'n0',
            isDeprecated: false,
            wasModified: false,
            willBeRemoved: false
          },
          {
            type: 't1',
            name: 'p1',
            currentValue: 'c1',
            recommendedValue: Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.deprecated'),
            resultingValue: 'c1',
            isDeprecated: true,
            wasModified: false,
            willBeRemoved: false
          },
          {
            type: 't2',
            name: 'p2',
            currentValue: 'c2',
            recommendedValue: Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.deprecated'),
            resultingValue: Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.willBeRemoved'),
            isDeprecated: true,
            wasModified: false,
            willBeRemoved: true
          }
        ],
        title: 'popup is displayed if warnings are present; configs merge conflicts'
      },
      {
        check: {
          "check": "Work-preserving RM/NM restart is enabled in YARN configs",
          "status": "PASS",
          "reason": "OK",
          "failed_on": [],
          "check_type": "SERVICE"
        },
        showClusterCheckPopupCalledCount: 0,
        upgradeCalledCount: 1,
        title: 'upgrade is started if fails and warnings are absent'
      }
    ];
    beforeEach(function () {
      sinon.stub(App, 'showClusterCheckPopup', Em.K);
      sinon.stub(controller, 'upgrade', Em.K);
    });
    afterEach(function () {
      App.showClusterCheckPopup.restore();
      controller.upgrade.restore();
    });
    cases.forEach(function (item) {
      it(item.title, function () {
        controller.runPreUpgradeCheckSuccess(
          {
            items: [
              {
                UpgradeChecks: item.check
              }
            ]
          }, null, {
            label: 'name'
          }
        );
        expect(controller.upgrade.callCount).to.equal(item.upgradeCalledCount);
        expect(App.showClusterCheckPopup.callCount).to.equal(item.showClusterCheckPopupCalledCount);
      });
    });
  });

  describe("#initDBProperties()", function() {
    before(function () {
      this.mock = sinon.stub(controller, 'getDBProperties');
    });
    after(function () {
      this.mock.restore();
    });
    it("set string properties", function () {
      this.mock.returns({prop: 'string'});
      controller.initDBProperties();
      expect(controller.get('prop')).to.equal('string');
    });
    it("set number properties", function () {
      this.mock.returns({prop: 0});
      controller.initDBProperties();
      expect(controller.get('prop')).to.equal(0);
    });
    it("set boolean properties", function () {
      this.mock.returns({prop: false});
      controller.initDBProperties();
      expect(controller.get('prop')).to.be.false;
    });
    it("set undefined properties", function () {
      this.mock.returns({prop: undefined});
      controller.set('prop', 'value');
      controller.initDBProperties();
      expect(controller.get('prop')).to.equal('value');
    });
    it("set null properties", function () {
      this.mock.returns({prop: null});
      controller.set('prop', 'value');
      controller.initDBProperties();
      expect(controller.get('prop')).to.equal('value');
    });
  });

  describe("#init()", function() {
    before(function () {
      sinon.stub(controller, 'initDBProperties', Em.K);
    });
    after(function () {
      controller.initDBProperties.restore();
    });
    it("call initDBProperties", function () {
      controller.init();
      expect(controller.initDBProperties.calledOnce).to.be.true;
    });
  });

  describe("#upgrade()", function() {
    var callArgs;

    beforeEach(function () {
      sinon.stub(controller, 'setDBProperty', Em.K);
      controller.set('currentVersion', {
        repository_version: '2.2'
      });
      controller.upgrade({
        value: '2.2',
        label: 'HDP-2.2'
      });
      callArgs = testHelpers.findAjaxRequest('name', 'admin.upgrade.start')[0];
    });

    afterEach(function () {
      controller.setDBProperty.restore();
    });

    it("request-data is valid", function() {
      expect(callArgs.data).to.eql({"value": '2.2', "label": 'HDP-2.2'});
    });
    it('request-name is valid', function () {
      expect(callArgs.name).to.equal('admin.upgrade.start');
    });
    it('request-sender is valid', function () {
      expect(callArgs.sender).to.eql(controller);
    });
    it('callback is valid', function () {
      expect(callArgs.success).to.equal('upgradeSuccessCallback');
    });
    it('callback is called', function () {
      expect(callArgs.callback).to.be.called;
    });
    it('setDBProperty is called with valid data', function () {
      expect(controller.setDBProperty.calledWith('currentVersion', {
        repository_version: '2.2'
      })).to.be.true;
    });
  });

  describe("#upgradeSuccessCallback()", function() {

    beforeEach(function () {
      sinon.stub(App.clusterStatus, 'setClusterStatus', Em.K);
      sinon.stub(controller, 'openUpgradeDialog', Em.K);
      sinon.stub(controller, 'setDBProperties', Em.K);
      sinon.stub(controller, 'load', Em.K);
      var data = {
        resources: [
          {
            Upgrade: {
              request_id: 1
            }
          }
        ]
      };
      controller.upgradeSuccessCallback(data, {}, {label: 'HDP-2.2.1', isDowngrade: true});
    });

    afterEach(function () {
      App.clusterStatus.setClusterStatus.restore();
      controller.openUpgradeDialog.restore();
      controller.setDBProperties.restore();
      controller.load.restore();
    });

    it('load is called ocne', function() {
      expect(controller.load.calledOnce).to.be.true;
    });
    it('upgradeVersion is HDP-2.2.1', function() {
      expect(controller.get('upgradeVersion')).to.equal('HDP-2.2.1');
    });
    it('upgradeData is null', function() {
      expect(controller.get('upgradeData')).to.be.null;
    });
    it('isDowngrade is true', function() {
      expect(controller.get('isDowngrade')).to.be.true;
    });
    it('App.clusterStatus.setClusterStatus is called once', function() {
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });
    it('controller.openUpgradeDialog is called once', function() {
      expect(controller.openUpgradeDialog.calledOnce).to.be.true;
    });
  });

  describe("#updateUpgradeData()", function() {
    beforeEach(function () {
      sinon.stub(controller, 'initUpgradeData', Em.K);
    });
    afterEach(function () {
      controller.initUpgradeData.restore();
    });
    it("data loaded first time", function() {
      controller.set('upgradeData', null);
      controller.updateUpgradeData({});
      expect(controller.initUpgradeData.calledWith({})).to.be.true;
    });

    describe('upgradeData exists', function () {

      var groups;

      beforeEach(function() {
        var oldData = Em.Object.create({
          upgradeGroups: [
            Em.Object.create({
              group_id: 1,
              upgradeItems: [
                Em.Object.create({
                  stage_id: 1
                })
              ]
            }),
            Em.Object.create({
              group_id: 2,
              upgradeItems: [
                Em.Object.create({
                  stage_id: 2
                }),
                Em.Object.create({
                  stage_id: 3
                })
              ]
            })
          ]
        });
        var newData = {
          Upgrade: {
            request_id: 1
          },
          upgrade_groups: [
            {
              UpgradeGroup: {
                group_id: 1,
                status: 'COMPLETED',
                display_status: 'COMPLETED',
                progress_percent: 100,
                completed_task_count: 3
              },
              upgrade_items: [
                {
                  UpgradeItem: {
                    stage_id: 1,
                    status: 'COMPLETED',
                    display_status: 'COMPLETED',
                    progress_percent: 100
                  }
                }
              ]
            },
            {
              UpgradeGroup: {
                group_id: 2,
                status: 'ABORTED',
                display_status: 'ABORTED',
                progress_percent: 50,
                completed_task_count: 1
              },
              upgrade_items: [
                {
                  UpgradeItem: {
                    stage_id: 2,
                    status: 'ABORTED',
                    display_status: 'ABORTED',
                    progress_percent: 99
                  }
                },
                {
                  UpgradeItem: {
                    stage_id: 3,
                    status: 'PENDING',
                    display_status: 'PENDING',
                    progress_percent: 0
                  }
                }
              ]
            }
          ]
        };
        controller.set('upgradeData', oldData);
        controller.updateUpgradeData(newData);
        groups = controller.get('upgradeData.upgradeGroups');
      });

      describe("checking 1st group", function() {
        it('status is COMPLETED', function () {
          expect(groups[0].get('status')).to.equal('COMPLETED');
        });
        it('display_status is COMPLETED', function () {
          expect(groups[0].get('display_status')).to.equal('COMPLETED');
        });
        it('progress_percent is 100', function () {
          expect(groups[0].get('progress_percent')).to.equal(100);
        });
        it('completed_task_count = 3', function () {
          expect(groups[0].get('completed_task_count')).to.equal(3);
        });
        it('upgradeItems.0.status is COMPLETED', function () {
          expect(groups[0].get('upgradeItems')[0].get('status')).to.equal('COMPLETED');
        });
        it('upgradeItems.0.display_status is COMPLETED', function () {
          expect(groups[0].get('upgradeItems')[0].get('display_status')).to.equal('COMPLETED');
        });
        it('upgradeItems.0.progress_percent is 100', function () {
          expect(groups[0].get('upgradeItems')[0].get('progress_percent')).to.equal(100);
        });
        it('hasExpandableItems is true', function () {
          expect(groups[0].get('hasExpandableItems')).to.be.true;
        });
      });

      describe('checking 2nd group', function () {
        it('status is ABORTED', function () {
          expect(groups[1].get('status')).to.equal('ABORTED');
        });
        it('display_status is ABORTED', function () {
          expect(groups[1].get('display_status')).to.equal('ABORTED');
        });
        it('progress_percent is 50', function () {
          expect(groups[1].get('progress_percent')).to.equal(50);
        });
        it('completed_task_count = 1', function () {
          expect(groups[1].get('completed_task_count')).to.equal(1);
        });
        it('upgradeItems.[].status = ["ABORTED", "PENDING"]', function () {
          expect(groups[1].get('upgradeItems').mapProperty('status')).to.eql(['ABORTED', 'PENDING']);
        });
        it('upgradeItems.[].display_status = ["ABORTED", "PENDING"]', function () {
          expect(groups[1].get('upgradeItems').mapProperty('display_status')).to.eql(['ABORTED', 'PENDING']);
        });
        it('upgradeItems.[].progress_percent = [99, 0]', function () {
          expect(groups[1].get('upgradeItems').mapProperty('progress_percent')).to.eql([99, 0]);
        });
        it('hasExpandableItems is false', function () {
          expect(groups[1].get('hasExpandableItems')).to.be.false;
        });
      });

    });
  });

  describe("#initUpgradeData()", function() {
    beforeEach(function () {
      sinon.stub(controller, 'setDBProperty');
      sinon.stub(controller, 'formatMessages');
      var newData = {
        Upgrade: {
          request_id: 1,
          downgrade_allowed: false
        },
        upgrade_groups: [
          {
            UpgradeGroup: {
              group_id: 1
            },
            upgrade_items: [
              {
                UpgradeItem: {
                  stage_id: 1,
                  status: 'IN_PROGRESS'
                }
              },
              {
                UpgradeItem: {
                  stage_id: 2
                }
              }
            ]
          },
          {
            UpgradeGroup: {
              group_id: 2
            },
            upgrade_items: []
          },
          {
            UpgradeGroup: {
              group_id: 3
            },
            upgrade_items: [
              {
                UpgradeItem: {
                  stage_id: 3,
                  status: 'ABORTED'
                }
              },
              {
                UpgradeItem: {
                  stage_id: 4,
                  status: 'PENDING'
                }
              }
            ]
          }
        ]
      };
      controller.initUpgradeData(newData);
    });
    afterEach(function () {
      controller.setDBProperty.restore();
      controller.formatMessages.restore();
    });
    it("setDBProperty called with valid arguments", function() {
      expect(controller.setDBProperty.calledWith('downgradeAllowed', false)).to.be.true;
    });
    it('downgradeAllowed is false', function () {
      expect(controller.get('downgradeAllowed')).to.be.false;
    });
    it('upgradeData.Upgrade.request_id is 1', function () {
      expect(controller.get('upgradeData.Upgrade.request_id')).to.equal(1);
    });
    it('upgradeData.upgradeGroups contain valid data', function () {
      var groups = controller.get('upgradeData.upgradeGroups');
      expect(groups.mapProperty('group_id')).to.eql([3,2,1]);
      expect(groups[2].get('upgradeItems').mapProperty('stage_id')).to.eql([2,1]);
      expect(groups.mapProperty('hasExpandableItems')).to.eql([false, false, true]);
    });
  });

  describe("#confirmDowngrade()", function() {

    before(function () {
      sinon.spy(App, 'showConfirmationPopup');
      sinon.stub(controller, 'downgrade', Em.K);
    });

    after(function () {
      App.showConfirmationPopup.restore();
      controller.downgrade.restore();
    });

    it("show confirmation popup", function() {
      controller.set('currentVersion', Em.Object.create({
        repository_version: '2.2',
        repository_name: 'HDP-2.2'
      }));
      var popup = controller.confirmDowngrade();
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.downgrade.calledWith(Em.Object.create({
        repository_version: '2.2',
        repository_name: 'HDP-2.2'
      }))).to.be.true;
    });
  });

  describe("#upgradeOptions()", function() {
    var version = Em.Object.create({displayName: 'HDP-2.2'});
    beforeEach(function () {
      sinon.spy(App, 'showConfirmationFeedBackPopup');
      sinon.stub(controller, 'getSupportedUpgradeTypes').returns({
        done: function (callback) {
          callback([1]);
          return {
            always: function (alwaysCallback) {
              alwaysCallback();
              return {};
            }
          };
        }
      });
      sinon.stub(controller, 'runPreUpgradeCheck', Em.K);
      sinon.stub(App.RepositoryVersion, 'find').returns([
        Em.Object.create({
          status: 'CURRENT'
        })
      ]);
      controller.get('runningCheckRequests').clear();
    });

    afterEach(function () {
      App.showConfirmationFeedBackPopup.restore();
      controller.runPreUpgradeCheck.restore();
      controller.getSupportedUpgradeTypes.restore();
      controller.get('upgradeMethods').setEach('selected', false);
      App.RepositoryVersion.find.restore();
    });

    describe("show confirmation popup", function() {

      beforeEach(function () {
        controller.set('isDowngrade', false);
        this.popup = controller.upgradeOptions(false, version);
      });

      it('popup is shown', function () {
        expect(App.ModalPopup.show.calledOnce).to.be.true;
      });

      it('all upgradeMethods have isCheckRequestInProgress = true', function () {
        expect(controller.get('upgradeMethods').everyProperty('isCheckRequestInProgress')).to.be.true;
      });

      it('upgradeMethods no one is selected', function () {
        expect(controller.get('upgradeMethods').someProperty('selected')).to.be.false;
      });

      describe('#popup.onPrimary', function () {

        beforeEach(function () {
          controller.get('upgradeMethods')[0].set('selected', true);
          this.confirmPopup = this.popup.onPrimary();
        });

        it('showConfirmationFeedBackPopup is called once', function () {
          expect(App.showConfirmationFeedBackPopup.calledOnce).to.be.true;
        });

        describe('#confirmPopup.onPrimary', function () {
          beforeEach(function () {
            this.confirmPopup.onPrimary();
          });

          it('runPreUpgradeCheck is called with correct version', function () {
            expect(controller.runPreUpgradeCheck.calledWith(version)).to.be.true;
          });

          it('runningCheckRequests has 1 item', function () {
            expect(controller.get('runningCheckRequests')).to.have.length(1);
          });

        });

      });

      describe('#popup.disablePrimary', function () {

        beforeEach(function() {
          this.mock = sinon.stub(App, 'get');
        });

        afterEach(function() {
          App.get.restore();
        });

        it('should be disabled if no method is selected', function () {
          expect(this.popup.get('disablePrimary')).to.be.true;
        });

        it('should be disabled if preupgradecheck is supproted and isPrecheckFailed is true', function () {
          this.mock.returns(true);
          this.popup.set('selectedMethod', Em.Object.create({
            selected: true,
            isPrecheckFailed: true,
            isCheckRequestInProgress: false
          }));

          expect(this.popup.get('disablePrimary')).to.be.true;
        });

        it('should be disabled if preupgradecheck is supproted and isCheckRequestInProgress is true', function () {
          this.popup.set('selectedMethod', Em.Object.create({
            selected: true,
            isPrecheckFailed: false,
            isCheckRequestInProgress: true
          }));
          this.mock.returns(true);
          expect(this.popup.get('disablePrimary')).to.be.true;
        });

        it('should be enabled with preupgrade check', function () {
          this.popup.set('selectedMethod', Em.Object.create({
            selected: true,
            isPrecheckFailed: false,
            isCheckRequestInProgress: false
          }));
          this.mock.returns(true);
          expect(this.popup.get('disablePrimary')).to.be.false;
        });

        it('should be enabled without preupgrade check', function () {
          this.popup.set('selectedMethod', Em.Object.create({
            selected: true
          }));
          this.mock.returns(false);
          expect(this.popup.get('disablePrimary')).to.be.false;
        });

      });

    });

    describe("NOT show confirmation popup on Downgrade", function() {
      beforeEach(function () {
        controller.set('isDowngrade', true);
        controller.upgradeOptions(false, version);
      });

      it('runningCheckRequests has 1 item', function () {
        expect( controller.get('runningCheckRequests')).to.have.length(1);
      });

    });
  });

  describe("#confirmUpgrade()", function() {
    before(function () {
      sinon.stub(controller, 'upgradeOptions', Em.K);
    });
    after(function () {
      controller.upgradeOptions.restore();
    });
    it("show show upgrade options popup window", function() {
      var version = Em.Object.create({displayName: 'HDP-2.2'});
      controller.confirmUpgrade(version);
      expect(controller.upgradeOptions.calledWith(false, version)).to.be.true;
    });
  });

  describe("#downgrade", function() {
    beforeEach(function () {
      sinon.stub(controller, 'abortUpgrade').returns({
        done: function(callback) {callback()}
      });
      sinon.stub(controller, 'startDowngrade');
      controller.downgrade('versionInfo');
    });

    afterEach(function () {
      controller.abortUpgrade.restore();
      controller.startDowngrade.restore();
    });

    it('should run abortUpgrade', function() {
      expect(controller.abortUpgrade.calledOnce).to.be.true;
    });

  });

  describe("#startDowngrade()", function() {
    beforeEach(function () {
      sinon.stub(App.RepositoryVersion, 'find').returns([
        Em.Object.create({
          displayName: 'HDP-2.3',
          repositoryVersion: '2.3'
        })
      ]);
      controller.set('upgradeVersion', 'HDP-2.3');
      controller.set('upgradeType', 'NON_ROLLING');
      controller.startDowngrade(Em.Object.create({
        id: '1',
        repository_version: '2.2',
        repository_name: 'HDP-2.2'
      }));
      this.callArgs = testHelpers.findAjaxRequest('name', 'admin.downgrade.start')[0];
    });

    afterEach(function () {
      App.RepositoryVersion.find.restore();
    });

    it('request-data is valid', function () {
      expect(this.callArgs.data).to.eql({
        id: '1',
        value: '2.2',
        label: 'HDP-2.3',
        isDowngrade: true,
        upgradeType: "NON_ROLLING"
      });
    });
    it('request-name is valid', function () {
      expect(this.callArgs.name).to.be.equal('admin.downgrade.start');
    });
    it('request-sender is valid', function () {
      expect(this.callArgs.sender).to.be.eql(controller);
    });
    it('callback is valid', function () {
      expect(this.callArgs.success).to.be.equal('upgradeSuccessCallback');
    });
    it('callback is called', function () {
      expect(this.callArgs.callback).to.be.called;
    });
  });

  describe("#installRepoVersionPopup()", function () {
    before(function () {
      sinon.stub(controller, 'installRepoVersion', Em.K);
      sinon.stub(App.Service, 'find').returns(Em.Object.create({
        isLoaded: true
      }));
    });
    after(function () {
      controller.installRepoVersion.restore();
      App.Service.find.restore();
    });
    it("show confirmation popup for non standart and available services", function () {
      var repo = Em.Object.create({'displayName': 'HDP-2.2', isStandard: false, stackServices: [Em.Object.create({
        name: 'HDFS',
        isUpgradable: true,
        isAvailable: true
      })]});
      var popup = controller.installRepoVersionPopup(repo);
      popup.onPrimary();
      expect(controller.installRepoVersion.calledWith(repo)).to.be.true;
    });
    it("show pre-check popup for non standard and empty available services", function () {
      var repo = Em.Object.create({'displayName': 'HDP-2.2', isStandard: false, stackServices: []});
      var popup = controller.installRepoVersionPopup(repo);
      popup.onPrimary();
      expect(controller.installRepoVersion.calledWith(repo)).to.be.false;
    });
    it("show confirmation popup for standart", function () {
      var repo = Em.Object.create({'displayName': 'HDP-2.2', isStandard: true, stackServices: []});
      var popup = controller.installRepoVersionPopup(repo);
      popup.onPrimary();
      expect(controller.installRepoVersion.calledWith(repo)).to.be.true;
    });
  });

  describe("#installRepoVersion()", function () {

    it("make ajax call", function () {
      var repo = Em.Object.create({
        stackVersionType: 'HDP',
        stackVersionNumber: '2.2',
        repositoryVersion: '2.2.1',
        repoId: 1
      });
      controller.installRepoVersion(repo);
      var args = testHelpers.findAjaxRequest('name', 'admin.stack_version.install.repo_version');
      expect(args).to.exists;
    });
  });

  describe("#installRepoVersionSuccess()", function() {
    var mock = Em.Object.create({
      id: 1,
      defaultStatus: 'NOT_REQUIRED',
      stackVersion: {}
    });
    beforeEach(function () {
      sinon.spy(mock, 'set');
      sinon.stub(App.db, 'set', Em.K);
      sinon.stub(App.clusterStatus, 'setClusterStatus', Em.K);
      sinon.stub(App.RepositoryVersion, 'find').returns(mock);
      sinon.stub(App, 'showAlertPopup');
    });
    afterEach(function () {
      App.db.set.restore();
      App.clusterStatus.setClusterStatus.restore();
      App.RepositoryVersion.find.restore();
      mock.set.restore();
      App.showAlertPopup.restore();
    });

    it("App.showAlertPopup should be called", function() {
      expect(controller.installRepoVersionSuccess({statusText: 'timeout'}, {}, {id: 1})).to.be.false;
      expect(App.showAlertPopup.calledWith(
        Em.I18n.t('admin.stackVersions.upgrade.installPackage.fail.title'),
        Em.I18n.t('admin.stackVersions.upgrade.installPackage.fail.timeout')
      )).to.be.true;
    });

    it("data sdtored to the local db", function() {
      controller.installRepoVersionSuccess({Requests: {id: 1}}, {}, {id: 1});
      expect(App.db.set.calledWith('repoVersionInstall', 'id', [1])).to.be.true;
    });

    it('clusterStatus is updated', function () {
      controller.installRepoVersionSuccess({Requests: {id: 1}}, {}, {id: 1});
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });

    it('App.RepositoryVersion models have valid states', function () {
      controller.installRepoVersionSuccess({Requests: {id: 1}}, {}, {id: 1});
      expect(App.RepositoryVersion.find.calledWith(1)).to.be.true;
      expect(App.RepositoryVersion.find(1).get('defaultStatus')).to.equal('INSTALLING');
      expect(App.RepositoryVersion.find(1).get('stackVersion.state')).to.equal('INSTALLING');
    });
  });

  describe("#setUpgradeItemStatus()", function () {
    var item;
    beforeEach(function () {
      item = Em.Object.create({
        request_id: 1,
        stage_id: 1,
        group_id: 1
      });
      controller.setUpgradeItemStatus(item, 'PENDING');
      this.callArgs = testHelpers.findAjaxRequest('name', 'admin.upgrade.upgradeItem.setState')[0];
    });

    it('request-data is valid', function () {
      expect(this.callArgs.data).to.be.eql({upgradeId: 1, itemId: 1, groupId: 1, status: 'PENDING'});
    });
    it('request-name is valid', function () {
      expect(this.callArgs.name).to.be.equal('admin.upgrade.upgradeItem.setState');
    });
    it('request-sendeer is valid', function () {
      expect(this.callArgs.sender).to.be.eql(controller);
    });
    it('callback is called', function () {
      expect(this.callArgs.callback).to.be.called;
    });
    it('item.status is PENDING', function () {
      expect(item.get('status')).to.equal('PENDING');
    });
  });

  describe("#prepareRepoForSaving()", function () {
    it("prepare date for saving", function () {
      var repo = Em.Object.create({
        operatingSystems: [
          Em.Object.create({
            osType: "redhat6",
            isDisabled: Em.computed.not('isSelected'),
            repositories: [Em.Object.create({
                "baseUrl": "111121",
                "repoId": "HDP-2.2",
                "repoName": "HDP",
                hasError: false
            }),
              Em.Object.create({
                "baseUrl": "1",
                "repoId": "HDP-UTILS-1.1.0.20",
                "repoName": "HDP-UTILS",
                hasError: false
              })]
           })
        ]
      });
      var result = {
        "operating_systems": [
          {
            "OperatingSystems": {
              "os_type": "redhat6",
              "ambari_managed_repositories": true
            },
            "repositories": [
              {
                "Repositories": {
                  "base_url": "111121",
                  "repo_id": "HDP-2.2",
                  "repo_name": "HDP"
                }
              },
              {
                "Repositories": {
                  "base_url": "1",
                  "repo_id": "HDP-UTILS-1.1.0.20",
                  "repo_name": "HDP-UTILS"
                }
              }
            ]
          }
        ]};
      expect(controller.prepareRepoForSaving(repo)).to.eql(result);
    });
  });

  describe("#getStackVersionNumber()", function(){
    it("get stack version number", function(){
      var repo = Em.Object.create({
        "stackVersionType": 'HDP',
        "stackVersion": '2.3',
        "repositoryVersion": '2.2.1'
      });

      var stackVersion = controller.getStackVersionNumber(repo);
      expect(stackVersion).to.equal('2.3');
    });

    it("get default stack version number", function(){
      App.set('currentStackVersion', '1.2.3');
      var repo = Em.Object.create({
        "stackVersionType": 'HDP',
        "repositoryVersion": '2.2.1'
      });

      var stackVersion = controller.getStackVersionNumber(repo);
      expect(stackVersion).to.equal('1.2.3');
    });
  });

  describe("#saveRepoOS()", function() {
    before(function(){
      this.mock = sinon.stub(controller, 'validateRepoVersions');
      sinon.stub(controller, 'prepareRepoForSaving', Em.K);
    });
    after(function(){
      this.mock.restore();
      controller.prepareRepoForSaving.restore();
    });
    describe("validation errors present", function() {

      beforeEach(function () {
        this.mock.returns({
          done: function(callback) {callback([1]);}
        });
        controller.saveRepoOS(Em.Object.create({repoVersionId: 1}), true);
        this.args = testHelpers.findAjaxRequest('name', 'admin.stack_versions.edit.repo');
      });

      it('validateRepoVersions is called with valid arguments', function () {
        expect(controller.validateRepoVersions.calledWith(Em.Object.create({repoVersionId: 1}), true)).to.be.true;
      });

      it('prepareRepoForSaving is not called', function () {
        expect(controller.prepareRepoForSaving.called).to.be.false;
      });

      it('no requests are sent', function () {
        expect(this.args).to.not.exists;
      });
    });

    describe("no validation errors", function() {

      beforeEach(function () {
        this.mock.returns({
          done: function(callback) {callback([]);}
        });
        controller.saveRepoOS(Em.Object.create({repoVersionId: 1}), true);
      });
      it('validateRepoVersions is called with valid arguments', function () {
        expect(controller.validateRepoVersions.calledWith(Em.Object.create({repoVersionId: 1}), true)).to.be.true;
      });
      it('prepareRepoForSaving is called with valid arguments', function () {
        expect(controller.prepareRepoForSaving.calledWith(Em.Object.create({repoVersionId: 1}))).to.be.true;
      });
    });
  });

  describe("#validateRepoVersions()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'validationCall').returns({
        success: function() {
          return {error: Em.K}
        }
      });
      sinon.stub(controller, 'getStackVersionNumber').returns('v1')
    });

    afterEach(function() {
      controller.validationCall.restore();
      controller.getStackVersionNumber.restore();
    });


    it("validationCall should not be called", function () {
      controller.validateRepoVersions(Em.Object.create({repoVersionId: 1}), true);
      expect(controller.validationCall.called).to.be.false;
    });
    it("validationCall should be called", function () {
      var os = Em.Object.create({
        isSelected: true,
        repositories: [
          Em.Object.create()
        ]
      });
      var repo = Em.Object.create({
        repoVersionId: 1,
        operatingSystems: [ os ]
      });
      controller.validateRepoVersions(repo, false);
      expect(controller.validationCall.calledOnce).to.be.true;
    });
  });

  describe("#validationCall()", function () {

    it("App.ajax.send should be called", function() {
      controller.validationCall(Em.Object.create(), Em.Object.create(), 'v1');
      var args = testHelpers.findAjaxRequest('name', 'admin.stack_versions.validate.repo');
      expect(args[0]).to.exists;
    });
  });

  describe("#getUrl()", function() {
    beforeEach(function(){
      controller.reopen({
        realStackUrl: 'realStackUrl',
        realRepoUrl: 'realRepoUrl',
        realUpdateUrl: 'realUpdateUrl'
      });
    });

    it("full load is true, stack is null", function() {
      expect(controller.getUrl(null, true)).to.equal('realRepoUrl');
    });
    it("full load is true, stack is valid", function() {
      expect(controller.getUrl({}, true)).to.equal('realStackUrl');
    });
    it("full load is false, stack is valid", function() {
      expect(controller.getUrl({}, false)).to.equal('realUpdateUrl');
    });
  });

  describe("#loadStackVersionsToModel()", function () {
    before(function () {
      sinon.stub(App.HttpClient, 'get');
    });
    after(function () {
      App.HttpClient.get.restore();
    });
    it("HttpClient did get-request", function () {
      controller.loadStackVersionsToModel();
      expect(App.HttpClient.get.calledOnce).to.be.true;
    });
  });

  describe("#loadRepoVersionsToModel()", function () {
    before(function () {
      sinon.stub(App.HttpClient, 'get');
    });
    after(function () {
      App.HttpClient.get.restore();
    });
    it("HttpClient did get-request", function () {
      controller.loadRepoVersionsToModel();
      expect(App.HttpClient.get.calledOnce).to.be.true;
    });
  });

  describe('#currentVersionObserver()', function () {

    var cases = [
      {
        stackVersionType: 'HDP',
        repoVersion: '2.2.1.1.0-1',
        isStormMetricsSupported: false,
        title: 'HDP < 2.2.2'
      },
      {
        stackVersionType: 'HDP',
        repoVersion: '2.2.2.1.0-1',
        isStormMetricsSupported: true,
        title: 'HDP 2.2.2'
      },
      {
        stackVersionType: 'HDP',
        repoVersion: '2.2.3.1.0-1',
        isStormMetricsSupported: true,
        title: 'HDP > 2.2.2'
      },
      {
        stackVersionType: 'BIGTOP',
        repoVersion: '0.8.1.1.0-1',
        isStormMetricsSupported: true,
        title: 'not HDP'
      }
    ];

    afterEach(function () {
      App.RepositoryVersion.find.restore();
    });

    cases.forEach(function (item) {
      describe(item.title, function () {

        beforeEach(function () {
          sinon.stub(App.RepositoryVersion, 'find').returns([
            Em.Object.create({
              status: 'CURRENT',
              stackVersionType: item.stackVersionType
            })
          ]);
          controller.set('currentVersion', {
            repository_version: item.repoVersion
          });
        });

        it('isStormMetricsSupported is ' + (item.isStormMetricsSupported ? '' : 'not') + ' supported', function () {
          expect(App.get('isStormMetricsSupported')).to.equal(item.isStormMetricsSupported);
        });

      });
    });

  });

  describe('#updateFinalize', function () {

    beforeEach(function() {
      controller.set('isFinalizeItem', true);
      this.stub = sinon.stub(App, 'get');
    });

    afterEach(function () {
      this.stub.restore();
    });

    describe('should do ajax-request', function () {

      beforeEach(function () {
        this.stub.withArgs('upgradeState').returns('HOLDING');
        controller.updateFinalize();
        this.args = testHelpers.findAjaxRequest('name', 'admin.upgrade.finalizeContext');
      });

      it('request is sent', function () {
        expect(this.args[0]).to.exists;
      });

    });

    describe('shouldn\'t do ajax-request', function () {

      beforeEach(function () {
        this.stub.withArgs('upgradeState').returns('HOLDING_TIMEDOUT');
        controller.updateFinalize();
        this.args = testHelpers.findAjaxRequest('name', 'admin.upgrade.finalizeContext');
      });

      it('request is not sent', function () {
        expect(this.args).to.not.exists;
      });

      it('isFinalizeItem is false', function () {
        expect(controller.get('isFinalizeItem')).to.be.false;
      });

    });

  });

  describe('#updateFinalizeSuccessCallback', function () {

    it('data exists and Finalize should be true', function() {
      var data = {
        items: [
          {
            upgrade_groups: [
              {
                upgrade_items: [
                  {
                    UpgradeItem: {
                      context: controller.get('finalizeContext'),
                      status: "HOLDING"
                    }
                  }
                ]
              }
            ]
          }
        ]
      };
      controller.set('isFinalizeItem', false);
      controller.updateFinalizeSuccessCallback(data);
      expect(controller.get('isFinalizeItem')).to.be.true;
    });

    it('data exists and Finalize should be false', function() {
      var data = {
        upgrade_groups: [
          {
            upgrade_items: [
              {
                UpgradeItem: {
                  context: '!@#$%^&',
                  status: "HOLDING"
                }
              }
            ]
          }
        ]
      };
      controller.set('isFinalizeItem', true);
      controller.updateFinalizeSuccessCallback(data);
      expect(controller.get('isFinalizeItem')).to.be.false;
    });

    it('data doesn\'t exist', function() {
      var data = null;
      controller.set('isFinalizeItem', true);
      controller.updateFinalizeSuccessCallback(data);
      expect(controller.get('isFinalizeItem')).to.be.false;
    });

  });

  describe('#updateFinalizeErrorCallback', function () {

    it('should set isFinalizeItem to false', function () {
      controller.set('isFinalizeItem', true);
      controller.updateFinalizeErrorCallback();
      expect(controller.get('isFinalizeItem')).to.be.false;
    });

  });

  describe("#suspendUpgrade()", function() {
    beforeEach(function () {
      sinon.stub(controller, 'abortUpgradeWithSuspend').returns({
        done: Em.clb
      });
      sinon.stub(controller, 'setDBProperty', Em.K);
      sinon.stub(App.clusterStatus, 'setClusterStatus', Em.K);
      controller.suspendUpgrade();
    });
    afterEach(function () {
      controller.abortUpgradeWithSuspend.restore();
      controller.setDBProperty.restore();
      App.clusterStatus.setClusterStatus.restore();
    });
    it("upgrade aborted", function() {
      expect(controller.abortUpgradeWithSuspend.calledOnce).to.be.true;
    });
    it('App.upgradeState is ABORTED', function () {
      expect(App.get('upgradeState')).to.equal('ABORTED');
    });
    it('new upgradeState is saved to the localDB', function () {
      expect(controller.setDBProperty.calledWith('upgradeState', 'ABORTED')).to.be.true;
    });
    it('clusterStatus is updated', function () {
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });
  });

  describe("#resumeUpgrade()", function() {
    beforeEach(function () {
      sinon.stub(controller, 'retryUpgrade').returns({
        done: Em.clb
      });
      sinon.stub(controller, 'setDBProperty', Em.K);
      sinon.stub(App.clusterStatus, 'setClusterStatus', Em.K);
      controller.resumeUpgrade();
    });
    afterEach(function () {
      controller.retryUpgrade.restore();
      controller.setDBProperty.restore();
      App.clusterStatus.setClusterStatus.restore();
    });
    it("Upgrade is retrying", function() {
      expect(controller.retryUpgrade.calledOnce).to.be.true;
    });
    it('App.upgradeState is PENDING', function () {
      expect(App.get('upgradeState')).to.equal('PENDING');
    });
    it('new upgradeState is saved to the localDB', function () {
      expect(controller.setDBProperty.calledWith('upgradeState', 'PENDING')).to.be.true;
    });
    it('clusterStatus is updated', function () {
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });
  });

  describe("#runUpgradeMethodChecks()", function() {
    beforeEach(function () {
      sinon.stub(controller, 'runPreUpgradeCheckOnly');
    });
    afterEach(function () {
      controller.runPreUpgradeCheckOnly.restore();
      controller.get('upgradeMethods').setEach('allowed', true);
    });
    it("no allowed upgrade methods", function () {
      controller.get('upgradeMethods').setEach('allowed', false);
      controller.runUpgradeMethodChecks();
      expect(controller.runPreUpgradeCheckOnly.called).to.be.false;
    });
    it("Rolling method allowed", function () {
      controller.get('upgradeMethods').setEach('allowed', true);
      controller.runUpgradeMethodChecks(Em.Object.create({
        id: 1,
        repositoryVersion: '1.2',
        displayName: 'V1'
      }));
      expect(controller.runPreUpgradeCheckOnly.calledWith({
        id: 1,
        label: 'V1',
        type: 'ROLLING'
      })).to.be.true;
    });
  });

  describe("#restoreLastUpgrade()", function () {

    var data = {
      Upgrade: {
        associated_version: '1.1',
        request_id: 1,
        direction: 'UPGRADE',
        request_status: 'PENDING',
        upgrade_type: 'ROLLING',
        downgrade_allowed: true,
        skip_failures: true,
        suspended: false,
        skip_service_check_failures: true,
        to_version: '1'
      }
    };

    beforeEach(function () {
      sinon.stub(App.RepositoryVersion, 'find').returns([Em.Object.create({
        repositoryVersion: '1',
        displayName: 'HDP-1'
      })]);
      sinon.stub(controller, 'setDBProperties');
      sinon.stub(controller, 'loadRepoVersionsToModel', function () {
        return {
          done: function (callback) {
            callback();
          }
        }
      });
      sinon.stub(controller, 'setDBProperty');
      sinon.stub(controller, 'initDBProperties');
      sinon.stub(controller, 'loadUpgradeData');
      controller.restoreLastUpgrade(data);
    });
    afterEach(function () {
      App.RepositoryVersion.find.restore();
      controller.setDBProperties.restore();
      controller.loadRepoVersionsToModel.restore();
      controller.setDBProperty.restore();
      controller.initDBProperties.restore();
      controller.loadUpgradeData.restore();
    });
    it('proper data is saved to the localDB', function () {
      expect(controller.setDBProperties.getCall(0).args[0]).to.eql({
        toVersion: '1.1',
        upgradeId: 1,
        isDowngrade: false,
        upgradeState: 'PENDING',
        upgradeType: "ROLLING",
        isWizardRestricted: false,
        downgradeAllowed: true,
        isSuspended: false,
        upgradeTypeDisplayName: Em.I18n.t('admin.stackVersions.version.upgrade.upgradeOptions.RU.title'),
        failuresTolerance: {
          skipComponentFailures: true,
          skipSCFailures: true
        }
      });
    });
    it('models are saved', function () {
      expect(controller.loadRepoVersionsToModel.calledOnce).to.be.true;
    });
    it('initDBProperties is called', function () {
      expect(controller.initDBProperties.calledOnce).to.be.true;
    });
  });

  describe("#getServiceCheckItemSuccessCallback()", function() {
    var testCases = [
      {
        title: 'no tasks',
        data: {
          tasks: []
        },
        expected: {
          slaveComponentStructuredInfo: null,
          serviceCheckFailuresServicenames: []
        }
      },
      {
        title: 'no structured_out property',
        data: {
          tasks: [
            {
              Tasks: {}
            }
          ]
        },
        expected: {
          slaveComponentStructuredInfo: null,
          serviceCheckFailuresServicenames: []
        }
      },
      {
        title: 'no failures',
        data: {
          tasks: [
            {
              Tasks: {
                structured_out: {}
              }
            }
          ]
        },
        expected: {
          slaveComponentStructuredInfo: null,
          serviceCheckFailuresServicenames: []
        }
      },
      {
        title: 'service check failures',
        data: {
          tasks: [
            {
              Tasks: {
                structured_out: {
                  failures: {
                    service_check: ['HDSF', 'YARN']
                  }
                }
              }
            }
          ]
        },
        expected: {
          slaveComponentStructuredInfo: {
            hosts: [],
            host_detail: {}
          },
          serviceCheckFailuresServicenames: ['HDSF', 'YARN']
        }
      },
      {
        title: 'host-component failures',
        data: {
          tasks: [
            {
              Tasks: {
                structured_out: {
                  failures: {
                    service_check: ['HDSF'],
                    host_component: {
                      "host1": [
                        {
                          component: "DATANODE",
                          service: 'HDFS'
                        }
                      ]
                    }
                  }
                }
              }
            }
          ]
        },
        expected: {
          slaveComponentStructuredInfo: {
            hosts: ['host1'],
            host_detail: {
              "host1": [
                {
                  component: "DATANODE",
                  service: 'HDFS'
                }
              ]
            }
          },
          serviceCheckFailuresServicenames: ['HDSF']
        }
      }
    ];

    testCases.forEach(function(test) {
      it(test.title, function() {
        controller.set('slaveComponentStructuredInfo', null);
        controller.set('serviceCheckFailuresServicenames', []);
        controller.getServiceCheckItemSuccessCallback(test.data);
        expect(controller.get('serviceCheckFailuresServicenames')).eql(test.expected.serviceCheckFailuresServicenames);
        expect(controller.get('slaveComponentStructuredInfo')).eql(test.expected.slaveComponentStructuredInfo);
      });
    });
  });

  describe("#getSlaveComponentItemSuccessCallback()", function () {
    var testCases = [
      {
        title: 'no tasks',
        data: {
          tasks: []
        },
        expected: {
          slaveComponentStructuredInfo: null
        }
      },
      {
        title: 'structured_out property absent',
        data: {
          tasks: [
            {
              Tasks: {}
            }
          ]
        },
        expected: {
          slaveComponentStructuredInfo: null
        }
      },
      {
        title: 'structured_out property present',
        data: {
          tasks: [
            {
              Tasks: {
                "structured_out" : {
                  "hosts" : [
                    "host1"
                  ],
                  "host_detail" : {
                    "host1" : [
                      {
                        "service" : "FLUME",
                        "component" : "FLUME_HANDLER"
                      }
                    ]
                  }
                }
              }
            }
          ]
        },
        expected: {
          slaveComponentStructuredInfo: {
            "hosts" : [
              "host1"
            ],
            "host_detail" : {
              "host1" : [
                {
                  "service" : "FLUME",
                  "component" : "FLUME_HANDLER"
                }
              ]
            }
          }
        }
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        controller.set('slaveComponentStructuredInfo', null);
        controller.getSlaveComponentItemSuccessCallback(test.data);
        expect(controller.get('slaveComponentStructuredInfo')).eql(test.expected.slaveComponentStructuredInfo);
      });
    });
  });

  describe('#getConfigsWarnings', function () {

    var cases = [
      {
        configs: [],
        title: 'no warning'
      },
      {
        configsMergeWarning: {},
        configs: [],
        title: 'empty data'
      },
      {
        configsMergeWarning: {
          UpgradeChecks: {}
        },
        configs: [],
        title: 'incomplete data'
      },
      {
        configsMergeWarning: {
          UpgradeChecks: {
            failed_detail: {}
          }
        },
        configs: [],
        title: 'invalid data'
      },
      {
        configsMergeWarning: {
          UpgradeChecks: {
            failed_detail: []
          }
        },
        configs: [],
        title: 'empty configs array'
      },
      {
        configsMergeWarning: {
          UpgradeChecks: {
            status: 'FAIL',
            failed_detail: [
              {
                type: 't0',
                property: 'p0',
                current: 'c0',
                new_stack_value: 'n0',
                result_value: 'r0'
              },
              {
                type: 't1',
                property: 'p1',
                current: 'c1',
                new_stack_value: 'n1'
              },
              {
                type: 't2',
                property: 'p2',
                current: 'c2',
                result_value: 'r2'
              }
            ]
          }
        },
        configs: [],
        title: 'not a warning'
      },
      {
        configsMergeWarning: {
          UpgradeChecks: {
            status: 'WARNING',
            failed_detail: [
              {
                type: 't0',
                property: 'p0',
                current: 'c0',
                new_stack_value: 'n0',
                result_value: 'r0'
              },
              {
                type: 't1',
                property: 'p1',
                current: 'c1',
                new_stack_value: 'n1'
              },
              {
                type: 't2',
                property: 'p2',
                current: 'c2',
                result_value: 'r2'
              }
            ]
          }
        },
        configs: [
          {
            type: 't0',
            name: 'p0',
            serviceName: 'HDFS',
            currentValue: 'c0',
            recommendedValue: 'n0',
            isDeprecated: false,
            resultingValue: 'r0',
            wasModified: false,
            willBeRemoved: false
          },
          {
            type: 't1',
            name: 'p1',
            serviceName: 'HDFS',
            currentValue: 'c1',
            recommendedValue: 'n1',
            isDeprecated: false,
            resultingValue: Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.willBeRemoved'),
            wasModified: false,
            willBeRemoved: true
          },
          {
            type: 't2',
            name: 'p2',
            serviceName: 'HDFS',
            currentValue: 'c2',
            recommendedValue: Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.deprecated'),
            isDeprecated: true,
            resultingValue: 'r2',
            wasModified: false,
            willBeRemoved: false
          }
        ],
        title: 'normal case'
      },
      {
        configsMergeWarning: {
          UpgradeChecks: {
            status: 'WARNING',
            failed_detail: [
              {
                type: 't0',
                property: 'p0',
                current: 'c0',
                new_stack_value: 'n0',
                result_value: 'r0'
              },
              {
                type: 't1',
                property: 'p1',
                current: 'c1',
                new_stack_value: 'n1'
              },
              {
                type: 't2',
                property: 'p2',
                current: 'c2',
                result_value: 'r2'
              },
              {
                type: 't3',
                property: 'p3',
                current: 'c3',
                new_stack_value: 'c2',
                result_value: 'c3'
              }
            ]
          }
        },
        configs: [
          {
            type: 't0',
            name: 'p0',
            serviceName: 'HDFS',
            currentValue: 'c0',
            recommendedValue: 'n0',
            isDeprecated: false,
            resultingValue: 'r0',
            wasModified: false,
            willBeRemoved: false
          },
          {
            type: 't1',
            name: 'p1',
            serviceName: 'HDFS',
            currentValue: 'c1',
            recommendedValue: 'n1',
            isDeprecated: false,
            resultingValue: Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.willBeRemoved'),
            wasModified: false,
            willBeRemoved: true
          },
          {
            type: 't2',
            name: 'p2',
            serviceName: 'HDFS',
            currentValue: 'c2',
            recommendedValue: Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.deprecated'),
            isDeprecated: true,
            resultingValue: 'r2',
            wasModified: false,
            willBeRemoved: false
          },
          {
            "currentValue": "c3",
            "isDeprecated": false,
            "name": "p3",
            "recommendedValue": "c2",
            "resultingValue": "c3",
            "type": "t3",
            serviceName: 'HDFS',
            "wasModified": true,
            "willBeRemoved": false
          }
        ],
        title: 'should skip warning when current and result_value are the same'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        sinon.stub(App.configsCollection, 'getConfigByName').returns({serviceName: 'HDFS'});
        expect(controller.getConfigsWarnings(item.configsMergeWarning)).to.eql(item.configs);
        App.configsCollection.getConfigByName.restore();
      });
    });

  });

  describe('#runPreUpgradeCheckOnly', function () {

    var appGetMock,
      upgradeMethods = controller.get('upgradeMethods'),
      cases = [
      {
        supportsPreUpgradeCheck: false,
        ru: {
          isCheckComplete: true,
          isCheckRequestInProgress: false,
          action: 'a'
        },
        eu: {
          isCheckComplete: true,
          isCheckRequestInProgress: false,
          action: 'a'
        },
        ajaxCallCount: 0,
        runningCheckRequestsLength: 0,
        title: 'pre-upgrade checks not supported'
      },
      {
        supportsPreUpgradeCheck: true,
        ru: {
          isCheckComplete: false,
          isCheckRequestInProgress: true,
          action: ''
        },
        eu: {
          isCheckComplete: true,
          isCheckRequestInProgress: false,
          action: 'a'
        },
        ajaxCallCount: 1,
        type: 'ROLLING',
        runningCheckRequestsLength: 1,
        title: 'rolling upgrade'
      },
      {
        supportsPreUpgradeCheck: true,
        ru: {
          isCheckComplete: true,
          isCheckRequestInProgress: false,
          action: 'a'
        },
        eu: {
          isCheckComplete: false,
          isCheckRequestInProgress: true,
          action: ''
        },
        ajaxCallCount: 1,
        type: 'NON_ROLLING',
        runningCheckRequestsLength: 1,
        title: 'express upgrade'
      }
    ];

    beforeEach(function () {
      appGetMock = sinon.stub(App, 'get');
      controller.get('runningCheckRequests').clear();
      upgradeMethods.forEach(function (method) {
        method.setProperties({
          isCheckComplete: true,
          isCheckRequestInProgress: false,
          action: 'a'
        });
      });
      App.ajax.send.restore();
      sinon.stub(App.ajax, 'send').returns({});
    });

    afterEach(function () {
      appGetMock.restore();
    });

    cases.forEach(function (item) {
      describe(item.title, function () {
        var runningCheckRequests;
        beforeEach(function () {
          runningCheckRequests = controller.get('runningCheckRequests');
          appGetMock.returns(item.supportsPreUpgradeCheck);
          controller.runPreUpgradeCheckOnly({
            type: item.type
          });
          this.ajaxCalls = testHelpers.filterAjaxRequests('name', 'admin.upgrade.pre_upgrade_check');
        });

        it('ROLLING properties', function () {
          expect(upgradeMethods.findProperty('type', 'ROLLING').getProperties('isCheckComplete', 'isCheckRequestInProgress', 'action')).to.eql(item.ru);
        });

        it('NON_ROLLING properties', function () {
          expect(upgradeMethods.findProperty('type', 'NON_ROLLING').getProperties('isCheckComplete', 'isCheckRequestInProgress', 'action')).to.eql(item.eu);
        });

        it(item.ajaxCallCount + ' requests sent', function () {
          expect(this.ajaxCalls.length).to.be.equal(item.ajaxCallCount);
        });

        it('runningCheckRequests length is ' + item.runningCheckRequestsLength, function () {
          expect(runningCheckRequests).to.have.length(item.runningCheckRequestsLength);
        });

        if (item.runningCheckRequestsLength) {
          it('runningCheckRequests.type is ' + item.type, function () {
            expect(runningCheckRequests[0].type).to.equal(item.type);
          });
        }

      });
    });

  });


  describe("#openConfigsInNewWindow()", function () {

    var mock = {
      document: {
        write: function () {}
      },
      focus: function () {}
    };

    beforeEach(function(){
      sinon.stub(window, 'open', function () {
        return mock;
      });
      sinon.spy(mock.document, 'write');
      sinon.spy(mock, 'focus');
      controller.openConfigsInNewWindow({
        context: [
          {
            type: 'type1',
            name: 'name1',
            serviceName: 'S1',
            currentValue: 'currentValue1',
            recommendedValue: 'recommendedValue1',
            resultingValue: 'resultingValue1'
          },
          {
            type: 'type2',
            name: 'name2',
            serviceName: 'S2',
            currentValue: 'currentValue2',
            recommendedValue: 'recommendedValue2',
            resultingValue: 'resultingValue2'
          }
        ]
      });
    });

    afterEach(function(){
      window.open.restore();
      mock.document.write.restore();
      mock.focus.restore();
    });

    it('new window is open', function () {
      expect(window.open.calledOnce).to.be.true;
    });

    it('new window content is valid', function () {
      /*eslint-disable no-useless-concat */
      expect(mock.document.write.calledWith('<table style="text-align: left;"><thead><tr>' +
        '<th>' + Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.configType') + '</th>' +
        '<th>' + Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.serviceName') + '</th>' +
        '<th>' + Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.propertyName') + '</th>' +
        '<th>' + Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.currentValue') + '</th>' +
        '<th>' + Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.recommendedValue') + '</th>' +
        '<th>' + Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.resultingValue') + '</th>' +
        '</tr></thead><tbody>' +
        '<tr>' +
        '<td>' + 'type1' + '</td>' +
        '<td>' + 'S1' + '</td>' +
        '<td>' + 'name1' + '</td>' +
        '<td>' + 'currentValue1' + '</td>' +
        '<td>' + 'recommendedValue1' + '</td>' +
        '<td>' + 'resultingValue1' + '</td>' +
        '</tr>' +
        '<tr>' +
        '<td>' + 'type2' + '</td>' +
        '<td>' + 'S2' + '</td>' +
        '<td>' + 'name2' + '</td>' +
        '<td>' + 'currentValue2' + '</td>' +
        '<td>' + 'recommendedValue2' + '</td>' +
        '<td>' + 'resultingValue2' + '</td>' +
        '</tr></tbody></table>')).to.be.true;
      /*eslint-enable no-useless-concat */
    });

    it('document.focus is called once', function () {
      expect(mock.focus.calledOnce).to.be.true;
    });
  });

  describe("#formatMessages()", function () {

    it("item with malformed text", function() {
      var item = Em.Object.create({
        text: null
      });
      controller.formatMessages(item);
      expect(item.get('messages')).to.be.empty;
      expect(item.get('text')).to.be.null;
    });

    it("item with correct text", function() {
      var item = Em.Object.create({
        text: '[{"message":"msg1"},{"message":"msg2"}]'
      });
      controller.formatMessages(item);
      expect(item.get('messages')).to.be.eql(['msg1', 'msg2']);
      expect(item.get('text')).to.be.equal('msg1 msg2');
    });
  });

  describe("#getUpgradeItemSuccessCallback()", function () {

    beforeEach(function() {
      controller.set('upgradeData', Em.Object.create({
        upgradeGroups: [
          Em.Object.create({
            group_id: 'g1',
            upgradeItems: [Em.Object.create({
              stage_id: 'i1',
              tasks: [],
              isTasksLoaded: false
            })]
          })
        ]
      }));
    });

    it("group has different id", function() {
      var data = {UpgradeItem: {group_id: 'g2'}};
      controller.getUpgradeItemSuccessCallback(data);
      expect(controller.get('upgradeData.upgradeGroups')[0].get('upgradeItems')[0].get('isTasksLoaded')).to.be.false;
    });

    it("item has different id", function() {
      var data = {UpgradeItem: {group_id: 'g1', stage_id: 'i2'}};
      controller.getUpgradeItemSuccessCallback(data);
      expect(controller.get('upgradeData.upgradeGroups')[0].get('upgradeItems')[0].get('isTasksLoaded')).to.be.false;
    });

    it("item has no tasks", function() {
      var data = {
        UpgradeItem: {
          group_id: 'g1',
          stage_id: 'i1'
        },
        tasks: [
          {
            Tasks: {id: 't1'}
          }
        ]
      };
      controller.getUpgradeItemSuccessCallback(data);
      expect(controller.get('upgradeData.upgradeGroups')[0].get('upgradeItems')[0].get('isTasksLoaded')).to.be.true;
      expect(controller.get('upgradeData.upgradeGroups')[0].get('upgradeItems')[0].get('tasks')).to.not.be.empty;
    });

    it("item has tasks", function() {
      controller.get('upgradeData.upgradeGroups')[0].get('upgradeItems')[0].set('tasks', [
        Em.Object.create({
          id: 't1',
          status: 'OLD'
        })
      ]);
      var data = {
        UpgradeItem: {
          group_id: 'g1',
          stage_id: 'i1'
        },
        tasks: [
          {
            Tasks: {
              id: 't1',
              status: 'NEW'
            }
          }
        ]
      };
      controller.getUpgradeItemSuccessCallback(data);
      expect(controller.get('upgradeData.upgradeGroups')[0].get('upgradeItems')[0].get('isTasksLoaded')).to.be.true;
      expect(controller.get('upgradeData.upgradeGroups')[0].get('upgradeItems')[0].get('tasks')[0].get('status')).to.be.equal('NEW');
    });
  });

  describe("#abortUpgrade()", function () {

    it("isDowngrade is true", function() {
      controller.set('isDowngrade', true);
      controller.set('upgradeId', 1);
      controller.abortUpgrade();
      var args = testHelpers.findAjaxRequest('name', 'admin.upgrade.abort');
      expect(args[0]).to.be.eql({
        name: 'admin.upgrade.abort',
        sender: controller,
        data: {
          upgradeId: 1,
          isDowngrade: true
        },
        error: 'abortDowngradeErrorCallback'
      });
    });

    it("isDowngrade is false", function() {
      controller.set('isDowngrade', false);
      controller.set('upgradeId', 1);
      controller.abortUpgrade();
      var args = testHelpers.findAjaxRequest('name', 'admin.upgrade.abort');
      expect(args[0]).to.be.eql({
        name: 'admin.upgrade.abort',
        sender: controller,
        data: {
          upgradeId: 1,
          isDowngrade: false
        },
        error: 'abortUpgradeErrorCallback'
      });
    });
  });

  describe("#abortUpgradeWithSuspend()", function () {

    it("isDowngrade is true", function() {
      controller.set('isDowngrade', true);
      controller.set('upgradeId', 1);
      controller.abortUpgradeWithSuspend();
      var args = testHelpers.findAjaxRequest('name', 'admin.upgrade.suspend');
      expect(args[0]).to.be.eql({
        name: 'admin.upgrade.suspend',
        sender: controller,
        data: {
          upgradeId: 1,
          isDowngrade: true
        },
        error: 'abortDowngradeErrorCallback'
      });
    });

    it("isDowngrade is false", function() {
      controller.set('isDowngrade', false);
      controller.set('upgradeId', 1);
      controller.abortUpgradeWithSuspend();
      var args = testHelpers.findAjaxRequest('name', 'admin.upgrade.suspend');
      expect(args[0]).to.be.eql({
        name: 'admin.upgrade.suspend',
        sender: controller,
        data: {
          upgradeId: 1,
          isDowngrade: false
        },
        error: 'abortUpgradeErrorCallback'
      });
    });
  });

  describe("#abortUpgradeErrorCallback()", function () {
    var header = Em.I18n.t('admin.stackUpgrade.state.paused.fail.header');
    var body = Em.I18n.t('admin.stackUpgrade.state.paused.fail.body');

    beforeEach(function() {
      sinon.stub(App, 'showAlertPopup');
    });

    afterEach(function() {
      App.showAlertPopup.restore();
    });

    it("data is null", function() {
      controller.abortUpgradeErrorCallback(null);
      expect(App.showAlertPopup.calledWith(header, body)).to.be.true;
    });

    it("data.responseText is null", function() {
      controller.abortUpgradeErrorCallback({responseText: null});
      expect(App.showAlertPopup.calledWith(header, body)).to.be.true;
    });

    it("data.responseText is incorrect", function() {
      controller.abortUpgradeErrorCallback({responseText: "//"});
      expect(App.showAlertPopup.calledWith(header, body)).to.be.true;
    });

    it("data.responseText is correct", function() {
      controller.abortUpgradeErrorCallback({responseText: '{"message": "msg"}'});
      expect(App.showAlertPopup.calledWith(header, body + ' msg')).to.be.true;
    });
  });

  describe("#abortDowngradeErrorCallback()", function () {
    var header = Em.I18n.t('admin.stackDowngrade.state.paused.fail.header');
    var body = Em.I18n.t('admin.stackDowngrade.state.paused.fail.body');

    beforeEach(function() {
      sinon.stub(App, 'showAlertPopup');
    });

    afterEach(function() {
      App.showAlertPopup.restore();
    });

    it("data is null", function() {
      controller.abortDowngradeErrorCallback(null);
      expect(App.showAlertPopup.calledWith(header, body)).to.be.true;
    });

    it("data.responseText is null", function() {
      controller.abortDowngradeErrorCallback({responseText: null});
      expect(App.showAlertPopup.calledWith(header, body)).to.be.true;
    });

    it("data.responseText is incorrect", function() {
      controller.abortDowngradeErrorCallback({responseText: "//"});
      expect(App.showAlertPopup.calledWith(header, body)).to.be.true;
    });

    it("data.responseText is correct", function() {
      controller.abortDowngradeErrorCallback({responseText: '{"message": "msg"}'});
      expect(App.showAlertPopup.calledWith(header, body + ' msg')).to.be.true;
    });
  });

  describe("#retryUpgrade()", function () {

    it("requestInProgress should be true", function() {
      controller.retryUpgrade();
      expect(controller.get('requestInProgress')).to.be.true;
    });

    it("isRetryPending should be true", function() {
      controller.retryUpgrade();
      expect(controller.get('isRetryPending')).to.be.true;
    });

    it("App.ajax.send should be called", function() {
      controller.set('upgradeId', 1);
      controller.retryUpgrade();
      var args = testHelpers.findAjaxRequest('name', 'admin.upgrade.retry');
      expect(args[0]).to.be.eql({
        name: 'admin.upgrade.retry',
        sender: controller,
        data: {
          upgradeId: 1
        }
      });
    });
  });

  describe("#upgradeErrorCallback()", function () {
    var header = Em.I18n.t('admin.stackVersions.upgrade.start.fail.title');
    var body = "";

    beforeEach(function() {
      sinon.stub(App, 'showAlertPopup');
    });

    afterEach(function() {
      App.showAlertPopup.restore();
    });

    it("data is null", function() {
      controller.upgradeErrorCallback(null);
      expect(App.showAlertPopup.calledWith(header, body)).to.be.true;
    });

    it("data.responseText is null", function() {
      controller.upgradeErrorCallback({responseText: null});
      expect(App.showAlertPopup.calledWith(header, body)).to.be.true;
    });

    it("data.responseText is incorrect", function() {
      controller.upgradeErrorCallback({responseText: "//"});
      expect(App.showAlertPopup.calledWith(header, body)).to.be.true;
    });

    it("data.responseText is correct", function() {
      controller.upgradeErrorCallback({responseText: '{"message": "msg"}'});
      expect(App.showAlertPopup.calledWith(header, 'msg')).to.be.true;
    });
  });

  describe("#updateOptionsSuccessCallback()", function () {

    it("skipComponentFailures: true, skipSCFailures: true", function() {
      var params = {
        skipComponentFailures: 'true',
        skipSCFailures: 'true'
      };
      controller.updateOptionsSuccessCallback({}, {}, params);
      expect(controller.get('failuresTolerance')).to.be.eql(Em.Object.create({
        skipComponentFailures: true,
        skipSCFailures: true
      }));
    });

    it("skipComponentFailures: false, skipSCFailures: false", function() {
      var params = {
        skipComponentFailures: 'false',
        skipSCFailures: 'false'
      };
      controller.updateOptionsSuccessCallback({}, {}, params);
      expect(controller.get('failuresTolerance')).to.be.eql(Em.Object.create({
        skipComponentFailures: false,
        skipSCFailures: false
      }));
    });
  });

  describe("#openUpgradeOptions()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'upgradeOptions');
    });

    afterEach(function() {
      controller.upgradeOptions.restore();
    });

    it("upgradeOptions should not be called", function() {
      controller.set('isDowngrade', true);
      controller.openUpgradeOptions();
      expect(controller.upgradeOptions.called).to.be.false;
    });

    it("upgradeOptions should be called", function() {
      controller.set('isDowngrade', false);
      controller.openUpgradeOptions();
      expect(controller.upgradeOptions.calledWith(true, null)).to.be.true;
    });
  });

  describe("#getSupportedUpgradeTypes()", function () {

    beforeEach(function() {
      controller.getSupportedUpgradeTypes({});
    });

    it("App.ajax.send should be called", function() {
      var args = testHelpers.findAjaxRequest('name', 'admin.upgrade.get_supported_upgradeTypes');
      expect(args[0]).to.be.eql({
        name: "admin.upgrade.get_supported_upgradeTypes",
        sender: controller,
        data: {},
        success: "getSupportedUpgradeTypesSuccess",
        error: "getSupportedUpgradeTypesError"
      });
    });

    it("getSupportedUpgradeError should be empty", function() {
      expect(controller.get('getSupportedUpgradeError')).to.be.empty;
    });

    it("isUpgradeTypesLoaded should be false", function() {
      expect(controller.get('isUpgradeTypesLoaded')).to.be.false;
    });
  });

  describe("#getSupportedUpgradeTypesSuccess()", function () {
    var testCases = [
      {
        data: {
          items: []
        },
        expected: false
      },
      {
        data: {
          items: [{
            CompatibleRepositoryVersions: {}
          }]
        },
        expected: false
      },
      {
        data: {
          items: [{
            CompatibleRepositoryVersions: {
              upgrade_types: []
            }
          }]
        },
        expected: false
      },
      {
        data: {
          items: [{
            CompatibleRepositoryVersions: {
              upgrade_types: ['t1']
            }
          }]
        },
        expected: true
      }
    ];

    testCases.forEach(function(test) {
      it("data: " + JSON.stringify(test.data), function() {
        controller.set('upgradeMethods', [Em.Object.create({
          type: 't1',
          allowed: true
        })]);
        controller.getSupportedUpgradeTypesSuccess(test.data);
        expect(controller.get('upgradeMethods')[0].get('allowed')).to.be.equal(test.expected);
      });
    });
  });

  describe("#getSupportedUpgradeTypesError", function () {

    it("correct responseText", function() {
      controller.getSupportedUpgradeTypesError({responseText: JSON.stringify({
        message: 'error'
      })});
      expect(controller.get('getSupportedUpgradeError')).to.be.equal('error');
    });

    it("invalid responseText", function() {
      controller.getSupportedUpgradeTypesError({
        responseText: '',
        statusText: 'statusError'
      });
      expect(controller.get('getSupportedUpgradeError')).to.be.equal('statusError');
    });
  });

  describe("#runPreUpgradeCheckOnlySuccess()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'formatPreCheckMessage').returns({precheckResultsMessage: 'formatted'});
      sinon.stub(controller, 'updateSelectedMethod');
      sinon.stub(controller, 'addPrecheckMessageTooltip');
      controller.set('upgradeMethods', [Em.Object.create({
        type: 'ROLLING'
      })]);
    });

    afterEach(function() {
      controller.formatPreCheckMessage.restore();
      controller.updateSelectedMethod.restore();
      controller.addPrecheckMessageTooltip.restore();
    });

    it("failed check", function() {
      var data = {
        items: [
          {
            UpgradeChecks: {
              status: 'FAIL'
            }
          }
        ]
      };
      var properties = {
        precheckResultsTitle: Em.I18n.t('admin.stackVersions.version.upgrade.upgradeOptions.preCheck.msg.title'),
        precheckResultsData: data,
        isCheckComplete: true,
        action: 'openMessage',
        precheckResultsMessage: '',
        recheckResultsMessageClass: 'GREEN',
        isPrecheckFailed: false,
        precheckResultsMessageIconClass: 'glyphicon glyphicon-ok',
        bypassedFailures: false
      };
      controller.runPreUpgradeCheckOnlySuccess(data, {}, {type: 'ROLLING'});
      expect(controller.formatPreCheckMessage.calledWith('FAIL', data, properties)).to.be.true;
      var msg = controller.get('upgradeMethods').findProperty('type', 'ROLLING').get('precheckResultsMessage');
      expect(msg).to.be.equal('formatted');
    });

    it("success check", function() {
      var data = {
        items: []
      };
      controller.runPreUpgradeCheckOnlySuccess(data, {}, {type: 'ROLLING'});
      var msg = controller.get('upgradeMethods').findProperty('type', 'ROLLING').get('precheckResultsMessage');
      expect(msg).to.be.equal(Em.I18n.t('admin.stackVersions.version.upgrade.upgradeOptions.preCheck.allPassed'));
    });

    it("updateSelectedMethod should be called", function() {
      var data = {
        items: []
      };
      controller.runPreUpgradeCheckOnlySuccess(data, {}, {type: 'ROLLING'});
      expect(controller.updateSelectedMethod.calledWith(false)).to.be.true;
    });

    it("addPrecheckMessageTooltip should be called", function() {
      var data = {
        items: []
      };
      controller.runPreUpgradeCheckOnlySuccess(data, {}, {type: 'ROLLING'});
      expect(controller.addPrecheckMessageTooltip.calledOnce).to.be.true;
    });
  });

  describe("#formatPreCheckMessage()", function () {

    it("should return formatted message", function() {
      var data = {
        items: [{
          UpgradeChecks: {
            status: 'FAIL'
          }
        }]
      };
      expect(controller.formatPreCheckMessage('FAIL', data, {precheckResultsMessage: 'pre'})).to.be.eql({
        "precheckResultsMessage": "1 Required pre",
        "precheckResultsMessageClass": "RED",
        "isPrecheckFailed": true,
        "precheckResultsMessageIconClass": "glyphicon glyphicon-remove"
      });
    });
  });

  describe("#addPrecheckMessageTooltip()", function () {

    beforeEach(function() {
      sinon.stub(Em.run, 'later', function(ctx, callback) {
        callback();
      });
      sinon.stub(App, 'tooltip');
    });

    afterEach(function() {
      Em.run.later.restore();
      App.tooltip.restore();
    });

    it("App.tooltip should be called", function() {
      controller.addPrecheckMessageTooltip();
      expect(App.tooltip.calledOnce).to.be.true;
    });
  });

  describe("#runPreUpgradeCheckOnlyError()", function () {

    it("upgradeMethod should be updated", function() {
      controller.set('upgradeMethods', [Em.Object.create({
        type: 'ROLLING'
      })]);
      controller.runPreUpgradeCheckOnlyError({}, {}, '', {}, {type: 'ROLLING'});
      expect(JSON.stringify(controller.get('upgradeMethods').findProperty('type', 'ROLLING'))).to.be.equal(JSON.stringify({
        "type": "ROLLING",
        "precheckResultsMessage": Em.I18n.t('admin.stackVersions.version.upgrade.upgradeOptions.preCheck.msg.failed.link'),
        "precheckResultsTitle": Em.I18n.t('admin.stackVersions.version.upgrade.upgradeOptions.preCheck.msg.failed.title'),
        "precheckResultsMessageClass": "RED",
        "isPrecheckFailed": true,
        "precheckResultsMessageIconClass": "glyphicon glyphicon-warning-sign",
        "action": "rerunCheck"
      }));
    });
  });

  describe("#updateSelectedMethod()", function () {

    beforeEach(function() {
      controller.set('upgradeMethods', [
        Em.Object.create({
          type: 'ROLLING',
          isPrecheckFailed: false,
          selected: true
        }),
        Em.Object.create({
          type: 'NON_ROLLING',
          isPrecheckFailed: false,
          selected: true
        })
      ]);
    });

    it("should select upgrade method", function() {
      controller.set('upgradeType', 'ROLLING');
      controller.updateSelectedMethod(true);
      expect(controller.get('upgradeMethods').findProperty('type', 'ROLLING').get('selected')).to.be.true;
    });

    it("isPrecheckFailed true for ROLLING and NON_ROLLING", function() {
      controller.updateSelectedMethod(false);
      expect(controller.get('upgradeMethods').findProperty('type', 'ROLLING').get('selected')).to.be.true;
      expect(controller.get('upgradeMethods').findProperty('type', 'NON_ROLLING').get('selected')).to.be.true;
    });

    it("isPrecheckFailed false for ROLLING and NON_ROLLING", function() {
      controller.get('upgradeMethods').setEach('isPrecheckFailed', true);
      controller.updateSelectedMethod(false);
      expect(controller.get('upgradeMethods').findProperty('type', 'ROLLING').get('selected')).to.be.false;
      expect(controller.get('upgradeMethods').findProperty('type', 'NON_ROLLING').get('selected')).to.be.false;
    });
  });

  describe("#runPreUpgradeCheckError()", function () {

    it("requestInProgress should be false", function() {
      controller.runPreUpgradeCheckError();
      expect(controller.get('requestInProgress')).to.be.false;
    });
  });

  describe("#confirmRetryUpgrade()", function () {

    beforeEach(function() {
      sinon.spy(App, 'showConfirmationPopup');
      sinon.stub(controller, 'retryUpgrade');
    });

    afterEach(function() {
      App.showConfirmationPopup.restore();
      controller.retryUpgrade.restore();
    });

    it("App.showConfirmationPopup should be called", function() {
      var popup = controller.confirmRetryUpgrade(Em.Object.create());
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.retryUpgrade.calledOnce).to.be.true;
    });
  });

  describe("#confirmRetryDowngrade()", function () {

    beforeEach(function() {
      sinon.spy(App, 'showConfirmationPopup');
      sinon.stub(controller, 'retryUpgrade');
    });

    afterEach(function() {
      App.showConfirmationPopup.restore();
      controller.retryUpgrade.restore();
    });

    it("App.showConfirmationPopup should be called", function() {
      var popup = controller.confirmRetryDowngrade(Em.Object.create());
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.retryUpgrade.calledOnce).to.be.true;
    });
  });

  describe("#installRepoVersionError()", function () {
    var header = Em.I18n.t('admin.stackVersions.upgrade.installPackage.fail.title');
    var mock = Em.Object.create({
      id: 1,
      defaultStatus: 'NOT_REQUIRED',
      stackVersion: {}
    });

    beforeEach(function() {
      sinon.stub(App, 'showAlertPopup');
      sinon.stub(App.RepositoryVersion, 'find').returns(mock);
    });

    afterEach(function() {
      App.showAlertPopup.restore();
      App.RepositoryVersion.find.restore();
    });

    it("responseText is incorrect", function() {
      var data = {
        responseText: null
      };
      controller.installRepoVersionError(data, null, mock);
      expect(App.showAlertPopup.calledWith(header, "")).to.be.true;
    });

    it("statusText is timeout", function() {
      var data = {
        responseText: '',
        statusText: 'timeout'
      };
      controller.installRepoVersionError(data, null, mock);
      expect(App.showAlertPopup.calledWith(header, Em.I18n.t('admin.stackVersions.upgrade.installPackage.fail.timeout'))).to.be.true;
    });

    it("responseText is correct", function() {
      var data = {
        responseText: '{"message":"msg"}'
      };
      controller.installRepoVersionError(data, null, mock);
      expect(App.showAlertPopup.calledWith(header, 'msg')).to.be.true;
    });
  });

  describe("#showProgressPopup()", function () {
    var mock = {
      initPopup: Em.K
    };

    beforeEach(function() {
      sinon.stub(App.router, 'get').withArgs('highAvailabilityProgressPopupController').returns(mock);
      sinon.stub(mock, 'initPopup');
      sinon.stub(controller, 'getRepoVersionInstallId').returns([1]);
    });

    afterEach(function() {
      App.router.get.restore();
      mock.initPopup.restore();
      controller.getRepoVersionInstallId.restore();
    });

    it("initPopup should be called", function() {
      controller.showProgressPopup(Em.Object.create({displayName: 'v1'}));
      expect(mock.initPopup.calledWith(
        Em.I18n.t('admin.stackVersions.details.install.hosts.popup.title').format('v1'),
        [1],
        controller
      )).to.be.true;
    });
  });

  describe('#getRepoVersionInstallId', function() {
    beforeEach(function() {
      this.mockDB = sinon.stub(App.db, 'get');
      this.mockRequests = sinon.stub(App.router, 'get');
    });
    afterEach(function() {
      this.mockDB.restore();
      this.mockRequests.restore();
    });

    it('should return id from latest version install', function() {
      this.mockDB.returns(null);
      this.mockRequests.returns([Em.Object.create({
        name: 'Install version',
        id: 1
      })]);
      expect(controller.getRepoVersionInstallId()[0]).to.be.equal(1);
    });
    it('should return id from localDB', function() {
      this.mockDB.returns([2]);
      this.mockRequests.returns([Em.Object.create({
        name: 'Install version',
        id: 2
      })]);
      expect(controller.getRepoVersionInstallId()[0]).to.be.equal(2);
    });
    it('should return id from latest version install and ignore deprecated localDb value', function() {
      this.mockDB.returns([2]);
      this.mockRequests.returns([Em.Object.create({
        name: 'Install version',
        id: 3
      })]);
      expect(controller.getRepoVersionInstallId()[0]).to.be.equal(3);
    });
  });

  describe("#finish()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'setDBProperties', Em.K);
      sinon.stub(App.clusterStatus, 'setClusterStatus');
      sinon.stub(controller, 'initDBProperties');
      App.set('upgradeState', 'COMPLETED');
      controller.set('upgradeVersion', '');
    });

    afterEach(function() {
      controller.setDBProperties.restore();
      controller.initDBProperties.restore();
      App.clusterStatus.setClusterStatus.restore();
    });

    it("setDBProperties should be called", function() {
      controller.finish();
      expect(controller.setDBProperties.calledWith({
        fromVersion: undefined,
        upgradeId: undefined,
        upgradeState: 'NOT_REQUIRED',
        upgradeVersion: undefined,
        currentVersion: undefined,
        upgradeTypeDisplayName: undefined,
        upgradeType: undefined,
        isWizardRestricted: false,
        failuresTolerance: undefined,
        isDowngrade: undefined,
        downgradeAllowed: undefined
      })).to.be.true;
    });

    it("initDBProperties should be called", function() {
      controller.finish();
      expect(controller.initDBProperties).to.be.calledOnce;
    });

    it("App.clusterStatus.setClusterStatus should be called", function() {
      controller.finish();
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });

    it("upgradeState should be NOT_REQUIRED", function() {
      controller.finish();
      expect(App.get('upgradeState')).to.be.equal('NOT_REQUIRED');
    });

    it("currentStackVersion should be set", function() {
      controller.set('upgradeVersion', 'HDP-2.2');
      controller.finish();
      expect(controller.get('upgradeVersion')).to.be.equal('HDP-2.2');
    });
  });

  describe("#loadRepositories()", function () {

    beforeEach(function() {
      sinon.stub(App.router, 'get').withArgs('clusterController.isLoaded').returns(true);
    });

    afterEach(function() {
      App.router.get.restore();
    });

    it("App.ajax.send should be called", function() {
      App.set('currentStackVersion', 'HDP-2.2');
      controller.loadRepositories();
      var args = testHelpers.findAjaxRequest('name', 'cluster.load_repositories');
      expect(args[0]).to.be.eql({
        name: 'cluster.load_repositories',
        sender: controller,
        data: {
          stackName: 'HDP',
          stackVersion: '2.2'
        },
        success: 'loadRepositoriesSuccessCallback',
        error: 'loadRepositoriesErrorCallback'
      });
    });
  });

  describe("#loadRepositoriesSuccessCallback()", function () {

    it("allRepos should be set", function() {
      var data = {
        items: [
          {
            repositories: [
              {
                Repositories: {
                  os_type: 'redhat',
                  base_url: 'base_url',
                  repo_id: 'repo_id',
                  repo_name: 'repo_name',
                  stack_name: 'stack_name',
                  stack_version: 'stack_version'
                }
              }
            ]
          }
        ]
      };
      controller.loadRepositoriesSuccessCallback(data);
      expect(controller.get('allRepos')[0].name).to.be.equal('redhat');
      expect(JSON.stringify(controller.get('allRepos')[0].repositories[0])).to.be.equal(JSON.stringify({
        "baseUrl": "base_url",
        "osType": "redhat",
        "repoId": "repo_id",
        "repoName": "repo_name",
        "stackName": "stack_name",
        "stackVersion": "stack_version",
        "isFirst": true
      }));
    });
  });

  describe("#getServiceVersionFromRepo()", function () {

    var cases = [
      {
        currentStackName: 'HDP',
        currentStackVersionNumber: '2.6',
        repoVersions: [
          Em.Object.create({
            'id': '1',
            'stackVersionType': 'HDP',
            'stackVersionNumber': '2.5',
            'stackServices': [
              Em.Object.create({
                'name': 'HDFS',
                'latestVersion': '2.6.2'
              })
            ]
          }),
          Em.Object.create({
            'id': '2',
            'stackVersionType': 'HDP',
            'stackVersionNumber': '2.6',
            'stackServices': [
              Em.Object.create({
                'name': 'HDFS',
                'latestVersion': '2.7.3'
              })
            ]
          })
        ],
        services: [
          Em.Object.create({
            'serviceName': 'HDFS',
            'desiredRepositoryVersionId': '2'
          })
        ],
        stackServices: [
          Em.Object.create({
            'serviceName': 'HDFS',
            'stackName': 'HDP',
            'stackVersion': '2.6'
          })
        ],
        title: 'If multiple stacks should select the repo on the current stack',
        expected: {'HDFS': '2.7.3'}
      },

      {
        currentStackName: 'HDP',
        currentStackVersionNumber: '2.6',
        repoVersions : [
          Em.Object.create({
            'id': '1',
            'stackVersionType': 'HDP',
            'stackVersionNumber': '2.6',
            'stackServices': [
              Em.Object.create({
                'name': 'HDFS',
                'latestVersion': '2.6.5'
              })
            ]
          }),
          Em.Object.create({
            'id': '2',
            'stackVersionType': 'HDP',
            'stackVersionNumber': '2.6',
            'stackServices': [
              Em.Object.create({
                'name': 'HDFS',
                'latestVersion': '2.7.3'
              })
            ]
          })
        ],
        services: [
          Em.Object.create({
            'serviceName': 'HDFS',
            'desiredRepositoryVersionId': '2'
          })
        ],
        stackServices: [
          Em.Object.create({
            'serviceName': 'HDFS',
            'stackName': 'HDP',
            'stackVersion': '2.6'
          })
        ],
        title: 'If multiple repositories in the same stack should select by id',
        expected: {'HDFS': '2.7.3'}
      },

      {
        currentStackName: 'HDP',
        currentStackVersionNumber: '2.6',
        repoVersions : [
          Em.Object.create({
            'id': '1',
            'stackVersionType': 'HDP',
            'stackVersionNumber': '2.6',
            'isCurrent': false,
            'isStandard': true,
            'stackServices': [
              Em.Object.create({
                'name': 'HDFS',
                'latestVersion': '2.6.5'
              })
            ]
          }),
          Em.Object.create({
            'id': '2',
            'stackVersionType': 'HDP',
            'stackVersionNumber': '2.6',
            'isCurrent': false,
            'isStandard': true,
            'stackServices': [
              Em.Object.create({
                'name': 'HDFS',
                'latestVersion': '2.7.3'
              })
            ]
          })
        ],

        services: [
          Em.Object.create({
            'serviceName': 'YARN',
            'desiredRepositoryVersionId': '2'
          })
        ],

        stackServices: [
          Em.Object.create({
            'serviceName': 'HDFS',
            'stackName': 'HDP',
            'stackVersion': '2.6'
          })
        ],
        title: 'No standard repo in current state - nothing should be returned',
        expected: {'HDFS': ''}
      },
      {
        currentStackName: 'HDP',
        currentStackVersionNumber: '2.6',
        repoVersions : [
          Em.Object.create({
            'id': '1',
            'stackVersionType': 'HDP',
            'stackVersionNumber': '2.6',
            'isCurrent': true,
            'isStandard': true,
            'stackServices': [
              Em.Object.create({
                'name': 'HDFS',
                'latestVersion': '2.6.5'
              })
            ]
          }),
          Em.Object.create({
            'id': '2',
            'stackVersionType': 'HDP',
            'stackVersionNumber': '2.6',
            'isCurrent': false,
            'isStandard': true,
            'stackServices': [
              Em.Object.create({
                'name': 'HDFS',
                'latestVersion': '2.7.3'
              })
            ]
          })
        ],

        services: [
          Em.Object.create({
            'serviceName': 'YARN',
            'desiredRepositoryVersionId': '2'
          })
        ],

        stackServices: [
          Em.Object.create({
            'serviceName': 'HDFS',
            'stackName': 'HDP',
            'stackVersion': '2.6'
          })
        ],
        title: 'Service not installed - get version from current & standard repo',
        expected: {'HDFS': '2.6.5'}
      },

    ];

    afterEach(function() {
      App.get.restore();
      App.Service.find.restore();
      App.StackService.find.restore();
      App.RepositoryVersion.find.restore();
      App.router.get.restore();
      controller.set('serviceVersionsMap', {});
    });

    cases.forEach(function(item) {
      it(item.title, function() {
        sinon.stub(App, 'get').withArgs('currentStackName').returns(item.currentStackName).withArgs('currentStackVersionNumber').returns(item.currentStackVersionNumber);
        sinon.stub(App.Service, 'find').returns(item.services);
        sinon.stub(App.StackService, 'find').returns(item.stackServices);
        sinon.stub(App.RepositoryVersion, 'find').returns(item.repoVersions);
        sinon.stub(App.router, 'get').returns(true);
        controller.getServiceVersionFromRepo();
        expect(controller.get('serviceVersionsMap')).to.be.eql(item.expected);
      });
    });
  });

  describe("#loadCompatibleVersions()", function () {

    beforeEach(function() {
      sinon.stub(App, 'get').returns('stack');
    });

    afterEach(function() {
      App.get.restore();
    });

    it("App.ajax.send should be called", function() {
      controller.loadCompatibleVersions();
      var args = testHelpers.findAjaxRequest('name', 'admin.upgrade.get_compatible_versions');
      expect(args[0]).to.be.eql({
        name: 'admin.upgrade.get_compatible_versions',
        sender: controller,
        data: {
          stackName: 'stack',
          stackVersion: 'stack'
        },
        success: 'loadCompatibleVersionsSuccessCallback'
      });
    });
  });

  describe("#loadCompatibleVersionsSuccessCallback()", function () {
    var mock = [
      Em.Object.create({
        repositoryVersion: 'HDP-1',
        isCompatible: false
      }),
      Em.Object.create({
        repositoryVersion: 'HDP-2',
        isCompatible: false
      })
    ];

    beforeEach(function() {
      sinon.stub(App.RepositoryVersion, 'find').returns(mock);
    });

    afterEach(function() {
      App.RepositoryVersion.find.restore();
    });

    it("should set isCompatible property", function() {
      var data = {
        items: [
          {
            CompatibleRepositoryVersions: {
              repository_version: 'HDP-2'
            }
          }
        ]
      };
      controller.loadCompatibleVersionsSuccessCallback(data);
      expect(mock.mapProperty('isCompatible')).to.be.eql([false, true])
    });
  });
  
   describe('#confirmRevertPatchUpgrade', function() {
    beforeEach(function() {
      sinon.stub(App.RepositoryVersion, 'find').returns(Em.Object.create());
      sinon.stub(controller, 'getServicesToBeReverted');
    });
    afterEach(function() {
      App.RepositoryVersion.find.restore();
      controller.getServicesToBeReverted.restore();
    });

    it('App.ModalPopup.show should be called', function() {
      controller.confirmRevertPatchUpgrade(Em.Object.create());
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });
  });

  describe('#getServicesToBeReverted', function() {
    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns(Em.Object.create({isLoaded: true}));
    });
    afterEach(function() {
      App.Service.find.restore();
    });

    it('should return services which will be reverted', function() {
      var version = Em.Object.create({
        stackServices: [
          Em.Object.create({
            name: 'S1',
            isAvailable: false,
            displayName: 's1',
            latestVersion: '1.0'
          }),
          Em.Object.create({
            name: 'S2',
            isAvailable: true,
            displayName: 's2',
            latestVersion: '2.0'
          })
        ]
      });
      var currentStack = Em.Object.create({
        stackServices: [
          Em.Object.create({
            name: 'S2',
            latestVersion: '2.1'
          })
        ]
      });
      expect(controller.getServicesToBeReverted(version, currentStack)).to.be.eql([
        {
          displayName: 's2',
          fromVersion: '2.0',
          toVersion: '2.1'
        }
      ]);
    });
  });

  describe('#revertPatchUpgrade', function() {
    it('App.ajax.send should be called', function() {
      var version = Em.Object.create({
        repositoryVersion: '1.1',
        id: 2,
        displayName: '1.2',
        upgradeType: 'EXPRESS',
        stackVersion: Em.Object.create({
          revertUpgradeId: 1
        })
      });
      controller.revertPatchUpgrade(version);
      expect(controller.get('requestInProgress')).to.be.true;
      var args = testHelpers.findAjaxRequest('name', 'admin.upgrade.revert');
      expect(args[0]).to.exists;
      expect(args[0].data).to.be.eql({
        upgradeId: 1,
        isDowngrade: true,
        id: 2,
        value: '1.1',
        label: '1.2'
      });
      args[0].callback();
      expect(controller.get('requestInProgress')).to.be.false;
    });
  });

  describe('#getUpgradeDowngradeHeader', function() {

    it('should return downgrade header', function() {
      expect(controller.getUpgradeDowngradeHeader('t1', 'v1', true)).to.be.equal(
        Em.I18n.t('admin.stackUpgrade.dialog.downgrade.header').format('v1')
      );
    });
    it('should return patch upgrade header', function() {
      expect(controller.getUpgradeDowngradeHeader('t1', 'v1', false, Em.Object.create({isPatch: true}))).to.be.equal(
        Em.I18n.t('admin.stackUpgrade.dialog.upgrade.patch.header').format('t1', 'v1')
      );
    });
    it('should return maint upgrade header', function() {
      expect(controller.getUpgradeDowngradeHeader('t1', 'v1', false, Em.Object.create({isMaint: true}))).to.be.equal(
        Em.I18n.t('admin.stackUpgrade.dialog.upgrade.maint.header').format('t1', 'v1')
      );
    });
    it('should return upgrade header', function() {
      expect(controller.getUpgradeDowngradeHeader('t1', 'v1', false, false)).to.be.equal(
        Em.I18n.t('admin.stackUpgrade.dialog.upgrade.header').format('t1', 'v1')
      );
    });
  });
  
  describe('#confirmDiscardRepoVersion', function() {
    beforeEach(function() {
      sinon.stub(App, 'showConfirmationPopup', Em.clb);
      sinon.stub(controller, 'discardRepoVersion');
    });
    afterEach(function() {
      App.showConfirmationPopup.restore();
      controller.discardRepoVersion.restore();
    });

    it('discardRepoVersion should be called', function() {
      controller.confirmDiscardRepoVersion(Em.Object.create());
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      expect(controller.discardRepoVersion.calledWith(Em.Object.create())).to.be.true;
    });
  });

  describe('#discardRepoVersion', function() {

    it('App.ajax.send should be called', function() {
      var version = Em.Object.create({
        id: 2,
        stackVersionType: 'HDP',
        stackVersionNumber: '2.5'
      });
      controller.discardRepoVersion(version);
      expect(controller.get('requestInProgress')).to.be.true;
      var args = testHelpers.findAjaxRequest('name', 'admin.stack_versions.discard');
      expect(args[0]).to.exists;
      expect(args[0].data).to.be.eql({
        id: 2,
        stackName: 'HDP',
        stackVersion: '2.5'
      });
      args[0].callback();
      expect(controller.get('requestInProgress')).to.be.false;
    });
  });

  describe('#showUpgradeOptions', function () {
    before(function () {
      sinon.stub(controller, 'upgradeOptions', Em.K);
    });
    after(function () {
      controller.upgradeOptions.restore();
    });
    it("show upgrade options popup window", function() {
      var version = Em.Object.create({displayName: 'HDP-2.2'});
      controller.showUpgradeOptions(version);
      expect(controller.upgradeOptions.calledWith(false, version, true)).to.be.true;
    });

    it("runningCheckRequests has 1 item" + Em.I18n.t('common.dismiss'), function () {
      var version = Em.Object.create({displayName: 'HDP-2.2'});
      var popup = controller.upgradeOptions(false, version, true);
      expect( controller.get('runningCheckRequests')).to.have.length(1);
    })
  });
  
  describe('#removeOutOfSyncComponents', function() {
    beforeEach(function() {
      sinon.stub(App.RepositoryVersion, 'find').returns(Em.Object.create({
        stackVersion: {
          outOfSyncHosts: ['host1']
        }
      }));
      sinon.stub(App, 'get').returns({
        getKDCSessionState: Em.clb
      });
    });
    afterEach(function() {
      App.RepositoryVersion.find.restore();
      App.get.restore();
    });
    
    it('App.ajax.send should be called', function() {
      var modal = controller.removeOutOfSyncComponents({context: {repoId: 1}});
      modal.onPrimary();
      var args = testHelpers.findAjaxRequest('name', 'host.host_component.delete_components');
      expect(args[0]).to.exists;
      expect(args[0].data).to.be.eql({
        hosts: ['host1'],
        data: JSON.stringify({
          RequestInfo: {
            query: 'HostRoles/host_name.in(host1)&HostRoles/state=INSTALL_FAILED'
          }
        })
      });
    });
  });
  
  describe('#reinstallOutOfSyncComponents', function() {
    beforeEach(function() {
      sinon.stub(App.RepositoryVersion, 'find').returns(Em.Object.create({
        stackVersion: {
          outOfSyncHosts: ['host1']
        }
      }));
      sinon.stub(App, 'get').returns({
        getKDCSessionState: Em.clb
      });
    });
    afterEach(function() {
      App.RepositoryVersion.find.restore();
      App.get.restore();
    });
    
    it('App.ajax.send should be called', function() {
      var modal = controller.reinstallOutOfSyncComponents({context: {repoId: 1}});
      modal.onPrimary();
      var args = testHelpers.findAjaxRequest('name', 'common.host_components.update');
      expect(args[0]).to.exists;
      expect(args[0].data).to.be.eql({
        HostRoles: {
          state: 'INSTALLED'
        },
        query: 'HostRoles/host_name.in(host1)&HostRoles/state=INSTALL_FAILED',
        context: Em.I18n.t('hosts.host.maintainance.reinstallFailedComponents.context')
      });
    });
  });
});
