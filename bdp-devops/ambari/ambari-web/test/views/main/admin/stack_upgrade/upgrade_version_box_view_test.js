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
require('views/main/admin/stack_upgrade/upgrade_version_box_view');

describe('App.UpgradeVersionBoxView', function () {

  var view;

  beforeEach(function () {
    sinon.stub(App.db, 'setFilterConditions', Em.K);
    sinon.stub(App.db, 'getFilterConditions', function () {return [];});
    view = App.UpgradeVersionBoxView.create({
      initFilters: Em.K,
      isCurrentStackPresent: true,
      controller: Em.Object.create({
        upgrade: Em.K,
        getRepoVersionInstallId: Em.K,
        currentVersion: Em.Object.create()
      }),
      content: Em.Object.create(),
      parentView: Em.Object.create({
        repoVersions: []
      })
    });
  });

  afterEach(function () {
    App.db.setFilterConditions.restore();
    App.db.getFilterConditions.restore();
  });

  describe("#isUpgrading", function () {
    beforeEach(function() {
      view.set('controller.fromVersion', 'HDP-1');
    });
    afterEach(function () {
      App.set('upgradeState', 'NOT_REQUIRED');
    });
    it("wrong version", function () {
      App.set('upgradeState', 'IN_PROGRESS');
      view.set('controller.upgradeVersion', 'HDP-2.2.1');
      view.set('content.displayName', 'HDP-2.2.2');
      view.propertyDidChange('isUpgrading');
      expect(view.get('isUpgrading')).to.be.false;
    });
    it("correct version", function () {
      App.set('upgradeState', 'IN_PROGRESS');
      view.set('controller.upgradeVersion', 'HDP-2.2.2');
      view.set('content.displayName', 'HDP-2.2.2');
      view.propertyDidChange('isUpgrading');
      expect(view.get('isUpgrading')).to.be.true;
    });
    it("fromVersion correct", function () {
      App.set('upgradeState', 'IN_PROGRESS');
      view.set('controller.upgradeVersion', 'HDP-2.2.2');
      view.set('content.displayName', 'HDP-2.2.1');
      view.set('content.repositoryVersion', 'HDP-1');
      view.propertyDidChange('isUpgrading');
      expect(view.get('isUpgrading')).to.be.true;
    });
    it("upgradeState NOT_REQUIRED", function () {
      App.set('upgradeState', 'NOT_REQUIRED');
      view.set('controller.upgradeVersion', 'HDP-2.2.2');
      view.set('content.displayName', 'HDP-2.2.2');
      view.propertyDidChange('isUpgrading');
      expect(view.get('isUpgrading')).to.be.false;
    });
    it("upgradeState NOT_REQUIRED and wrong version", function () {
      App.set('upgradeState', 'NOT_REQUIRED');
      view.set('controller.upgradeVersion', 'HDP-2.2.2');
      view.set('content.displayName', 'HDP-2.2.1');
      view.propertyDidChange('isUpgrading');
      expect(view.get('isUpgrading')).to.be.false;
    });
  });

  describe("#installProgress", function () {

    beforeEach(function () {
      this.mockId = sinon.stub(view.get('controller'), 'getRepoVersionInstallId');
      this.mock = sinon.stub(App.router, 'get');
      App.set('testMode', false);
    });
    afterEach(function () {
      this.mockId.restore();
      this.mock.restore();
    });

    it("request id is not set", function () {
      this.mock.returns([]);
      this.mockId.returns(undefined);
      view.propertyDidChange('installProgress');
      expect(view.get('installProgress')).to.equal(0);
    });
    it("request absent", function () {
      this.mock.returns([]);
      this.mockId.returns([1]);
      view.propertyDidChange('installProgress');
      expect(view.get('installProgress')).to.equal(0);
    });
    it("request present", function () {
      this.mockId.returns([1]);
      this.mock.returns([Em.Object.create({progress: 100, id: 1})]);
      view.propertyDidChange('installProgress');
      expect(view.get('installProgress')).to.equal(100);
    });
  });

  describe("#versionClass", function () {
    it("status CURRENT", function () {
      view.set('content.status', 'CURRENT');
      view.propertyDidChange('versionClass');
      expect(view.get('versionClass')).to.equal('current-version-box');
    });
    it("status INSTALLED", function () {
      view.set('content.status', 'INSTALLED');
      view.propertyDidChange('versionClass');
      expect(view.get('versionClass')).to.equal('');
    });
  });

  describe("#isOutOfSync", function () {
    it("status OUT_OF_SYNC", function () {
      view.set('content.status', 'OUT_OF_SYNC');
      view.propertyDidChange('isOutOfSync');
      expect(view.get('isOutOfSync')).to.be.true;
    });
  });

  describe("#didInsertElement()", function () {
    beforeEach(function () {
      sinon.stub(App, 'tooltip').returns(1);
    });
    afterEach(function () {
      App.tooltip.restore();
    });
    it("init tooltips", function () {
      view.didInsertElement();
      expect(App.tooltip.callCount).to.equal(4);
    });
  });

  describe("#runAction()", function () {
    var hasClass = function () {
        return true;
      },
      jQueryMock;
    beforeEach(function () {
      sinon.stub(view.get('controller'), 'upgrade').returns(1);
      jQueryMock = sinon.stub(window, '$');
    });
    afterEach(function () {
      view.get('controller').upgrade.restore();
      jQueryMock.restore();
    });
    it("action = null", function () {
      view.set('stateElement.action', null);
      view.runAction({context: null});
      expect(view.get('controller').upgrade.called).to.be.false;
    });
    it("action = 'upgrade'", function () {
      view.set('content', 'content');
      view.runAction({context: 'upgrade'});
      expect(view.get('controller').upgrade.calledWith('content')).to.be.true;
    });
    it("action is taken from stateElement", function () {
      view.setProperties({
        'content': 'content',
        'stateElement.action': 'upgrade'
      });
      view.runAction();
      expect(view.get('controller').upgrade.calledWith('content')).to.be.true;
    });
    it("link is disabled", function () {
      jQueryMock.returns({
        hasClass: hasClass,
        parent: function () {
          return {
            hasClass: Em.K
          };
        }
      });
      view.runAction({
        context: 'upgrade',
        target: {}
      });
      expect(view.get('controller').upgrade.called).to.be.false;
    });
    it("link parent element is disabled", function () {
      jQueryMock.returns({
        height: Em.K,
        hasClass: Em.K,
        parent: function () {
          return {
            hasClass: hasClass
          };
        }
      });
      view.runAction({
        context: 'upgrade',
        target: {}
      });
      expect(view.get('controller').upgrade.called).to.be.false;
    });
  });
  
  describe("#getStackVersionNumber()", function(){
    it("get stack version number", function(){
      var repoRecord = Em.Object.create({
        operatingSystems: [
          Em.Object.create({
            osType: "redhat6",
            repositories: [Em.Object.create({
                "baseUrl": "111121",
                "repoId": "HDP-2.3",
                "repoName": "HDP",
                "stackVersion": "2.3",
                hasError: false
            }), Em.Object.create({
                "baseUrl": "1",
                "repoId": "HDP-UTILS-1.1.0.20",
                "repoName": "HDP-UTILS",
                "stackVersion": "2.3",
                hasError: false
              })]
           })
        ]
      });
      
      var stackVersionNumber = view.getStackVersionNumber(repoRecord);
      expect(stackVersionNumber).to.equal('2.3');
    });
  });
  
  describe("#editRepositories()", function () {

    describe('popup display', function () {
      var cases = [
        {
          isRepoUrlsEditDisabled: true,
          popupShowCallCount: 0,
          title: 'edit repo URLS disabled, popup shouldn\'t be shown'
        },
        {
          isRepoUrlsEditDisabled: false,
          popupShowCallCount: 1,
          title: 'edit repo URLS enabled, popup should be shown'
        }
      ];
      beforeEach(function () {
        sinon.stub(App.RepositoryVersion, 'find').returns(Em.Object.create({
          operatingSystems: []
        }));
      });
      afterEach(function () {
        App.RepositoryVersion.find.restore();
      });
      cases.forEach(function (item) {
        it(item.title, function () {
          view.reopen({
            isRepoUrlsEditDisabled: item.isRepoUrlsEditDisabled
          });
          view.editRepositories();
          expect(App.ModalPopup.show.callCount).to.equal(item.popupShowCallCount);
        });
      });
    });

    describe('skip base URL validation', function () {
      var checkbox,
        cases = [
          {
            checked: true,
            skipValidation: true,
            title: 'option is enabled'
          },
          {
            checked: false,
            skipValidation: false,
            title: 'option is disabled'
          }
        ];
      beforeEach(function () {
        sinon.stub(App.RepositoryVersion, 'find').returns(Em.Object.create({
          operatingSystems: [
            Em.Object.create({
              repositories: [
                Em.Object.create(),
                Em.Object.create({
                  skipValidation: true
                }),
                Em.Object.create({
                  skipValidation: false
                })
              ]
            }),
            Em.Object.create({
              repositories: [
                Em.Object.create(),
                Em.Object.create({
                  skipValidation: true
                }),
                Em.Object.create({
                  skipValidation: false
                })
              ]
            })
          ]
        }));
        App.ModalPopup.show.restore();
        sinon.stub(App.ModalPopup, 'show', function (popupOptions) {
          var body = popupOptions.bodyClass.create();
          return body.get('skipCheckBox').create({
            parentView: body
          });
        });
        view.reopen({
          isRepoUrlsEditDisabled: false
        });
        checkbox = view.editRepositories();
      });
      afterEach(function () {
        App.RepositoryVersion.find.restore();
      });
      cases.forEach(function (item) {
        it(item.title, function () {
          checkbox.set('checked', item.checked);
          checkbox.change();
          var reposByOS = checkbox.get('repo.operatingSystems').mapProperty('repositories'),
            result = reposByOS.map(function (repos) {
              return repos.everyProperty('skipValidation', item.skipValidation);
            });
          expect(result).to.eql([true, true]);
        });
      });
    });
  });

  describe("#showHosts()", function () {
    beforeEach(function () {
      view.set('content.stackVersion', Em.Object.create({supportsRevert: false}));
      view.set('content.stackServices', [Em.Object.create({isUpgradable: true})])
      sinon.stub(view, 'filterHostsByStack', Em.K);
    });
    afterEach(function () {
      view.filterHostsByStack.restore();
    });
    it("no hosts", function () {
      view.set('content', Em.Object.create({
        p1: []
      }));
      view.set('p1', []);
      view.showHosts({contexts: [
        {'property': 'p1'}
      ]});
      expect(App.ModalPopup.show.called).to.be.false;
    });
    it("one host", function () {
      view.set('content', Em.Object.create({
        p1: ['host1'],
        displayName: 'version'
      }));
      view.set('p1', ['host1']);
      var popup = view.showHosts({contexts: [
        {value: 1, 'property': 'p1'}
      ]});
      expect(App.ModalPopup.show.calledOnce).to.be.true;
      popup.onPrimary();
      expect(view.filterHostsByStack.calledWith('version', 1)).to.be.true;
    });
  });

  describe("#filterHostsByStack()", function () {
    var mock = {
      set: Em.K,
      filterByStack: Em.K
    };
    beforeEach(function () {
      sinon.stub(App.router, 'get').withArgs('mainHostController').returns(mock);
      sinon.stub(App.router, 'transitionTo', Em.K);
      sinon.spy(mock, 'set');
      sinon.spy(mock, 'filterByStack');
    });
    afterEach(function () {
      App.router.get.restore();
      App.router.transitionTo.restore();
      mock.set.restore();
      mock.filterByStack.restore();
    });
    it("version and state are valid", function () {
      view.filterHostsByStack('version', 'state');
      expect(mock.set.calledWith('showFilterConditionsFirstLoad', true)).to.be.true;
      expect(mock.set.calledWith('filterChangeHappened', true)).to.be.true;
      expect(mock.filterByStack.calledWith('version', 'state')).to.be.true;
      expect(App.router.transitionTo.calledWith('hosts.index')).to.be.true;
    });
    it("version is null", function () {
      view.filterHostsByStack(null, 'state');
      expect(mock.set.called).to.be.false;
      expect(mock.filterByStack.called).to.be.false;
      expect(App.router.transitionTo.called).to.be.false;
    });
    it("state is null", function () {
      view.filterHostsByStack('version', null);
      expect(mock.set.called).to.be.false;
      expect(mock.filterByStack.called).to.be.false;
      expect(App.router.transitionTo.called).to.be.false;
    });
    it("state and version are null", function () {
      view.filterHostsByStack(null, null);
      expect(mock.set.called).to.be.false;
      expect(mock.filterByStack.called).to.be.false;
      expect(App.router.transitionTo.called).to.be.false;
    });
  });

  describe('#stateElement', function () {

    var cases = [
      {
        inputData: {
          'content.status': 'CURRENT'
        },
        expected: {
          status: 'CURRENT',
          isLabel: true,
          text: Em.I18n.t('common.current'),
          class: 'label label-success'
        },
        title: 'current version'
      },
      {
        inputData: {
          'content.status': 'CURRENT',
          'content.isPatch': true,
          'isUpgrading': false,
          'content.stackVersion': Em.Object.create({
            'supportsRevert': false
          })
        },
        expected: {
          isLabel: true,
          text: Em.I18n.t('common.current'),
          class: 'label label-success'
        },
        title: 'current no-revertable patch version'
      },
      {
        inputData: {
          'content.status': 'CURRENT',
          'content.isPatch': true,
          'isUpgrading': false,
          'content.stackVersion': Em.Object.create({
            'supportsRevert': true
          })
        },
        expected: {
          isButtonGroup: true,
          text: Em.I18n.t('common.current'),
          action: null,
          buttons: [
            {
              text: Em.I18n.t('common.revert'),
              action: 'confirmRevertPatchUpgrade'
            }
          ]
        },
        title: 'current revertable patch version'
      },
      {
        inputData: {
          'content.status': 'NOT_REQUIRED',
          'controller.requestInProgress': false,
          'parentView.repoVersions': [
            Em.Object.create({
              status: 'NOT_REQUIRED'
            })
          ]
        },
        setup: function () {
          this.isAccessibleMock.withArgs('CLUSTER.UPGRADE_DOWNGRADE_STACK').returns(false);
          this.initMock.returns(false);
        },
        expected: {
          status: 'NOT_REQUIRED',
          isButton: false,
          isButtonGroup: true,
          buttons: [
            {
              "action": "confirmDiscardRepoVersion",
              "isDisabled": false,
              "text": Em.I18n.t('common.hide')
            }
          ],
          isDisabled: true
        },
        title: 'NOT_REQUIRED state, no admin access, no requests in progress'
      },
      {
        inputData: {
          'content.status': 'NOT_REQUIRED',
          'controller.requestInProgress': true,
          'parentView.repoVersions': [
            Em.Object.create({
              status: 'NOT_REQUIRED'
            })
          ]
        },
        setup: function () {
          this.isAccessibleMock.withArgs('CLUSTER.UPGRADE_DOWNGRADE_STACK').returns(false);
          this.initMock.returns(true);
        },
        expected: {
          status: 'NOT_REQUIRED',
          isButton: false,
          isButtonGroup: true,
          buttons: [
            {
              "action": "confirmDiscardRepoVersion",
              "isDisabled": true,
              "text": Em.I18n.t('common.hide')
            }
          ],
          isDisabled: true
        },
        title: 'NOT_REQUIRED state, no admin access, request in progress, not installation'
      },
      {
        inputData: {
          'content.status': 'INSTALL_FAILED',
          'controller.requestInProgress': true,
          'parentView.repoVersions': [
            Em.Object.create({
              status: 'INSTALL_FAILED'
            }),
            Em.Object.create({
              status: 'INSTALLING'
            })
          ],
          'controller.currentVersion': {
            repository_version: '2.2.0'
          },
          'content.repositoryVersion': '2.2.1',
          'controller.upgradeVersion': 'HDP-2.2.0',
          'content.displayName': 'HDP-2.2.1'
        },
        setup: function () {
          this.isAccessibleMock.withArgs('CLUSTER.UPGRADE_DOWNGRADE_STACK').returns(false);
          this.initMock.returns(true);
        },
        expected: {
          status: 'INSTALL_FAILED',
          isButton: true,
          text: Em.I18n.t('admin.stackVersions.version.reinstall'),
          action: 'installRepoVersionPopup',
        },
        title: 'INSTALL_FAILED state, no admin access, request in progress, another installation running'
      },
      {
        inputData: {
          'content.status': 'INSTALL_FAILED',
          'controller.requestInProgress': false,
          'parentView.repoVersions': [
            Em.Object.create({
              status: 'INSTALL_FAILED'
            }),
            Em.Object.create({
              status: 'INSTALLING'
            })
          ],
          'controller.currentVersion': {
            repository_version: '2.2.0'
          },
          'content.repositoryVersion': '2.2.1',
          'controller.upgradeVersion': 'HDP-2.2.0',
          'content.displayName': 'HDP-2.2.1'
        },
        setup: function () {
          this.isAccessibleMock.withArgs('CLUSTER.UPGRADE_DOWNGRADE_STACK').returns(false);
          this.initMock.returns(true);
        },
        expected: {
          status: 'INSTALL_FAILED',
          isButton: true,
          text: Em.I18n.t('admin.stackVersions.version.reinstall'),
          action: 'installRepoVersionPopup',
        },
        title: 'INSTALL_FAILED state, no admin access, no requests in progress, another installation running'
      },
      {
        inputData: {
          'content.status': 'OUT_OF_SYNC',
          'controller.requestInProgress': false,
          'parentView.repoVersions': [
            Em.Object.create({
              status: 'OUT_OF_SYNC'
            })
          ],
          'controller.currentVersion': {
            repository_version: '2.2.0'
          },
          'content.repositoryVersion': '2.2.1',
          'controller.upgradeVersion': 'HDP-2.2.0',
          'content.displayName': 'HDP-2.2.1'
        },
        setup: function () {
          this.isAccessibleMock.withArgs('CLUSTER.UPGRADE_DOWNGRADE_STACK').returns(true);
          this.initMock.returns(true);
          this.isDisabledMock.returns(false);
        },
        expected: {
          status: 'OUT_OF_SYNC',
          isButton: true,
          text: Em.I18n.t('admin.stackVersions.version.reinstall'),
          action: 'installRepoVersionPopup',
          isDisabled: false
        },
        title: 'OUT_OF_SYNC state, admin access, no requests in progress, no installation'
      },
      {
        inputData: {
          'content.status': 'OUT_OF_SYNC',
          'controller.requestInProgress': true,
          'parentView.repoVersions': [
            Em.Object.create({
              status: 'OUT_OF_SYNC'
            })
          ],
          'controller.currentVersion': {
            repository_version: '2.2.0'
          },
          'content.repositoryVersion': '2.2.1',
          'controller.upgradeVersion': 'HDP-2.2.0',
          'content.displayName': 'HDP-2.2.1'
        },
        setup: function () {
          this.isAccessibleMock.withArgs('CLUSTER.UPGRADE_DOWNGRADE_STACK').returns(true);
        },
        expected: {
          status: 'OUT_OF_SYNC',
          isButton: true,
          text: Em.I18n.t('admin.stackVersions.version.reinstall'),
          action: 'installRepoVersionPopup',
          isDisabled: true
        },
        title: 'OUT_OF_SYNC state, admin access, request in progress, no installation'
      },
      {
        inputData: {
          'content.status': 'INSTALLED',
          'controller.currentVersion': {
            repository_version: '2.2.1'
          },
          'content.repositoryVersion': '2.2.0',
          'controller.upgradeVersion': 'HDP-2.2.1',
          'content.displayName': 'HDP-2.2.0'
        },
        expected: {
          status: 'INSTALLED',
          isButtonGroup: true,
          iconClass: 'glyphicon glyphicon-ok',
          text: Em.I18n.t('common.installed'),
          action: null
        },
        title: 'installed version, earlier than current one'
      },
      {
        inputData: {
          'content.status': 'INSTALLED',
          'content.stackServices': [Em.Object.create({isUpgradable:true})],
          'controller.requestInProgress': true,
          'content.isPatch': true,
          'parentView.repoVersions': [
            Em.Object.create({
              status: 'INSTALLED'
            }),
            Em.Object.create({
              status: 'INSTALLING'
            })
          ],
          'controller.currentVersion': {
            repository_version: '2.2.0'
          },
          'content.repositoryVersion': '2.2.1',
          'controller.upgradeVersion': 'HDP-2.2.0',
          'content.displayName': 'HDP-2.2.1'
        },
        setup: function () {
          this.isAccessibleMock.withArgs('CLUSTER.UPGRADE_DOWNGRADE_STACK').returns(true);
          this.initMock.returns(true);
        },
        expected: {
          status: 'INSTALLED',
          isButtonGroup: true,
          buttons: [
            {
              "action": "installRepoVersionPopup",
              "isDisabled": true,
              "text": "Reinstall Packages",
            },
            {
              "action": "showUpgradeOptions",
              "isDisabled": true,
              "text": "Pre-Upgrade Check"
            },

            {
              "action": "confirmDiscardRepoVersion",
              "isDisabled": true,
              "text": Em.I18n.t('common.hide')
            }
          ],
          isDisabled: true
        },
        title: 'installed version, later than current one, admin access, request in progress, another installation running'
      },
      {
        inputData: {
          'content.status': 'INSTALLED',
          'controller.requestInProgress': false,
          'parentView.repoVersions': [
            Em.Object.create({
              status: 'INSTALLED'
            }),
            Em.Object.create({
              status: 'INSTALLING'
            })
          ],
          'controller.currentVersion': {
            repository_version: '2.2.0'
          },
          'content.repositoryVersion': '2.2.1',
          'controller.upgradeVersion': 'HDP-2.2.1',
          'content.displayName': 'HDP-2.2.0'
        },
        setup: function () {
          this.isAccessibleMock.withArgs('CLUSTER.UPGRADE_DOWNGRADE_STACK').returns(true);
        },
        expected: {
          status: 'INSTALLED',
          isButtonGroup: true,
          buttons: [],
          isDisabled: true
        },
        title: 'installed version, later than current one, admin access, no requests in progress, another installation running'
      },
      {
        inputData: {
          'content.status': 'INSTALLED',
          'controller.isDowngrade': true,
          'controller.upgradeVersion': 'HDP-2.2.1',
          'content.displayName': 'HDP-2.2.1'
        },
        setup: function () {
          this.getMock.withArgs('upgradeState').returns(null);
        },
        expected: {
          status: 'INSTALLED',
          isLink: true,
          action: 'openUpgradeDialog',
          iconClass: 'glyphicon glyphicon-cog',
          text: Em.I18n.t('admin.stackVersions.version.downgrade.running')
        },
        title: 'downgrading'
      },
      {
        inputData: {
          'content.status': 'INSTALLED',
          'isUpgrading': true,
          'controller.isDowngrade': false,
          'controller.upgradeVersion': 'HDP-2.2.1',
          'content.displayName': 'HDP-2.2.1'
        },
        setup: function () {
          this.getMock.withArgs('upgradeState').returns('IN_PROGRESS');
        },
        expected: {
          status: 'INSTALLED',
          isLink: true,
          action: 'openUpgradeDialog',
          iconClass: 'glyphicon glyphicon-cog',
          text: Em.I18n.t('admin.stackVersions.version.upgrade.running')
        },
        title: 'upgrading'
      },
      {
        inputData: {
          'content.status': 'UPGRADING',
          'isUpgrading': true,
          'controller.isDowngrade': false,
          'controller.upgradeVersion': 'HDP-2.2.1',
          'content.displayName': 'HDP-2.2.1'
        },
        setup: function () {
          this.getMock.withArgs('upgradeState').returns('HOLDING');
        },
        expected: {
          status: 'UPGRADING',
          isLink: true,
          action: 'openUpgradeDialog',
          iconClass: 'glyphicon glyphicon-pause',
          text: Em.I18n.t('admin.stackVersions.version.upgrade.pause')
        },
        title: 'upgrading, holding'
      },
      {
        inputData: {
          'content.status': 'UPGRADING',
          'isUpgrading': true,
          'controller.isDowngrade': false,
          'controller.upgradeVersion': 'HDP-2.2.1',
          'controller.isWizardRestricted': true,
          'content.displayName': 'HDP-2.2.1'
        },
        setup: function () {
          this.getMock.withArgs('upgradeState').returns('HOLDING');
        },
        expected: {
          isDisabled: true,
          status: 'UPGRADING',
          isLink: true,
          action: 'openUpgradeDialog',
          iconClass: 'glyphicon glyphicon-pause',
          text: Em.I18n.t('admin.stackVersions.version.upgrade.pause')
        },
        title: 'upgrading, holding, isWizardRestricted=true'
      },
      {
        inputData: {
          'content.status': 'UPGRADING',
          'isUpgrading': true,
          'controller.isDowngrade': false,
          'controller.upgradeVersion': 'HDP-2.2.1',
          'content.displayName': 'HDP-2.2.1'
        },
        setup: function () {
          this.getMock.withArgs('upgradeState').returns('HOLDING_FAILED');
        },
        expected: {
          status: 'UPGRADING',
          isLink: true,
          action: 'openUpgradeDialog',
          iconClass: 'glyphicon glyphicon-pause',
          text: Em.I18n.t('admin.stackVersions.version.upgrade.pause')
        },
        title: 'upgrading, holding failed'
      },
      {
        inputData: {
          'content.status': 'UPGRADING',
          'isUpgrading': true,
          'controller.isDowngrade': false,
          'controller.upgradeVersion': 'HDP-2.2.1',
          'content.displayName': 'HDP-2.2.1'
        },
        setup: function () {
          this.getMock.withArgs('upgradeState').returns('ABORTED');
        },
        expected: {
          status: 'UPGRADING',
          isLink: true,
          action: 'openUpgradeDialog',
          iconClass: 'glyphicon glyphicon-pause',
          text: Em.I18n.t('admin.stackVersions.version.upgrade.pause')
        },
        title: 'upgrading, upgrade aborted'
      },
      {
        inputData: {
          'content.status': 'UPGRADE_FAILED',
          'isUpgrading': true,
          'controller.isDowngrade': false,
          'controller.upgradeVersion': 'HDP-2.2.1',
          'content.displayName': 'HDP-2.2.1'
        },
        setup: function () {
          this.getMock.withArgs('upgradeState').returns('HOLDING_TIMEDOUT');
        },
        expected: {
          status: 'UPGRADE_FAILED',
          isLink: true,
          action: 'openUpgradeDialog',
          iconClass: 'glyphicon glyphicon-pause',
          text: Em.I18n.t('admin.stackVersions.version.upgrade.pause')
        },
        title: 'upgrade failed, holding finished on timeout'
      },
      {
        inputData: {
          'content.status': 'UPGRADE_FAILED',
          'isUpgrading': true,
          'controller.isDowngrade': true,
          'controller.upgradeVersion': 'HDP-2.2.1',
          'content.displayName': 'HDP-2.2.1'
        },
        setup: function () {
          this.getMock.withArgs('upgradeState').returns('HOLDING');
        },
        expected: {
          status: 'UPGRADE_FAILED',
          isLink: true,
          action: 'openUpgradeDialog',
          iconClass: 'glyphicon glyphicon-pause',
          text: Em.I18n.t('admin.stackVersions.version.downgrade.pause')
        },
        title: 'downgrading, holding'
      },
      {
        inputData: {
          'content.status': 'UPGRADED',
          'isUpgrading': true,
          'controller.isDowngrade': true,
          'controller.upgradeVersion': 'HDP-2.2.1',
          'content.displayName': 'HDP-2.2.1'
        },
        setup: function () {
          this.getMock.withArgs('upgradeState').returns('HOLDING_FAILED');
        },
        expected: {
          status: 'UPGRADED',
          isLink: true,
          action: 'openUpgradeDialog',
          iconClass: 'glyphicon glyphicon-pause',
          text: Em.I18n.t('admin.stackVersions.version.downgrade.pause')
        },
        title: 'downgrading, holding failed'
      },
      {
        inputData: {
          'content.status': 'UPGRADED',
          'isUpgrading': true,
          'controller.isDowngrade': true,
          'controller.upgradeVersion': 'HDP-2.2.1',
          'content.displayName': 'HDP-2.2.1'
        },
        setup: function () {
          this.getMock.withArgs('upgradeState').returns('ABORTED');
        },
        expected: {
          status: 'UPGRADED',
          isLink: true,
          action: 'openUpgradeDialog',
          iconClass: 'glyphicon glyphicon-pause',
          text: Em.I18n.t('admin.stackVersions.version.downgrade.pause')
        },
        title: 'downgrading, upgrade aborted'
      },
      {
        inputData: {
          'content.status': 'UPGRADED',
          'isUpgrading': true,
          'controller.isDowngrade': true,
          'controller.upgradeVersion': 'HDP-2.2.1',
          'content.displayName': 'HDP-2.2.1'
        },
        setup: function () {
          this.getMock.withArgs('upgradeState').returns('HOLDING_TIMEDOUT');
        },
        expected: {
          status: 'UPGRADED',
          isLink: true,
          action: 'openUpgradeDialog',
          iconClass: 'glyphicon glyphicon-pause',
          text: Em.I18n.t('admin.stackVersions.version.downgrade.pause')
        },
        title: 'downgrading, holding finished on timeout'
      },
      {
        inputData: {
          'content.status': 'UPGRADING',
          'isUpgrading': true,
          'controller.isDowngrade': false,
          'controller.requestInProgress': false,
          'parentView.repoVersions': []
        },
        setup: function () {
          this.getMock.withArgs('upgradeSuspended').returns('true');
        },
        expected: {
          status: 'UPGRADING',
          isButton: true,
          action: 'resumeUpgrade',
          text: Em.I18n.t('admin.stackUpgrade.dialog.resume'),
          isDisabled: true
        },
        title: 'upgrade suspended'
      },
      {
        inputData: {
          'content.status': 'UPGRADE_FAILED',
          'isUpgrading': true,
          'controller.isDowngrade': true,
          'controller.requestInProgress': true,
          'parentView.repoVersions': []
        },
        setup: function () {
          this.getMock.withArgs('upgradeSuspended').returns('true');
        },
        expected: {
          status: 'UPGRADE_FAILED',
          isButton: true,
          action: 'resumeUpgrade',
          text: Em.I18n.t('admin.stackUpgrade.dialog.resume.downgrade'),
          isDisabled: true
        },
        title: 'downgrade suspended, request in progress'
      }
    ];

    beforeEach(function () {
      this.getMock = sinon.stub(App, 'get');
      this.isAccessibleMock = sinon.stub(App, 'isAuthorized');
      this.initMock = sinon.stub(view, 'isDisabledOnInit');
      this.isDisabledMock = sinon.stub(view, 'isDisabledOnInstalled').returns(true);
    });
    afterEach(function () {
      this.getMock.restore();
      this.isAccessibleMock.restore();
      this.initMock.restore();
      this.isDisabledMock.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        if (item.setup) {
          item.setup.call(this);
        }
        view.reopen({
          isUpgrading: item.inputData.isUpgrading
        });
        view.setProperties(item.inputData);
        var result = view.get('stateElement').getProperties(Em.keys(item.expected));
        if (result.buttons) {
          result.buttons = result.buttons.toArray();
        }
        expect(result).to.eql(item.expected);
      });
    }, this);

  });

  describe('#isRepoUrlsEditDisabled', function () {

    var cases = [
      {
        status: 'INSTALLING',
        isUpgrading: false,
        isRepoUrlsEditDisabled: true,
        title: 'installing packages'
      },
      {
        status: 'UPGRADING',
        isUpgrading: true,
        isRepoUrlsEditDisabled: true,
        title: 'upgrading'
      },
      {
        status: 'INSTALLED',
        isUpgrading: true,
        isRepoUrlsEditDisabled: true,
        title: 'upgrading just started'
      },
      {
        status: 'NOT_REQUIRED',
        isUpgrading: false,
        isRepoUrlsEditDisabled: false,
        title: 'neither upgrading nor installing packages'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        view.reopen({
          isUpgrading: item.isUpgrading
        });
        view.set('content.status', item.status);
        expect(view.get('isRepoUrlsEditDisabled')).to.equal(item.isRepoUrlsEditDisabled);
      });
    });
  });

  describe("#isDisabledOnInit()", function () {
    var testCases = [
      {
        requestInProgress: true,
        upgradeIsRunning: true,
        upgradeSuspended: true,
        status: 'INSTALLED',
        isCompatible: true,
        isCurrentStackPresent: true,
        expected: true
      },
      {
        requestInProgress: false,
        upgradeIsRunning: true,
        upgradeSuspended: false,
        status: 'INSTALLED',
        isCompatible: true,
        isCurrentStackPresent: true,
        expected: true
      },
      {
        requestInProgress: false,
        upgradeIsRunning: false,
        upgradeSuspended: false,
        status: 'INSTALLING',
        isCompatible: true,
        isCurrentStackPresent: true,
        expected: true
      },
      {
        requestInProgress: false,
        upgradeIsRunning: true,
        upgradeSuspended: true,
        status: 'INSTALLED',
        isCompatible: false,
        isCurrentStackPresent: true,
        expected: true
      },
      {
        requestInProgress: false,
        upgradeIsRunning: true,
        upgradeSuspended: true,
        status: 'INSTALLED',
        isCompatible: true,
        isCurrentStackPresent: true,
        expected: false
      },
      {
        requestInProgress: false,
        upgradeIsRunning: false,
        upgradeSuspended: false,
        status: 'INSTALLED',
        isCompatible: true,
        isCurrentStackPresent: true,
        expected: false
      },
      {
        requestInProgress: false,
        upgradeIsRunning: false,
        upgradeSuspended: false,
        status: 'INSTALLED',
        isCompatible: true,
        isCurrentStackPresent: false,
        expected: true
      }
    ];

    beforeEach(function() {
      this.mock = sinon.stub(App, 'get');
    });

    afterEach(function() {
      this.mock.restore();
    });

    testCases.forEach(function(test) {
      it(" requestInProgress: " + test.requestInProgress +
         " upgradeIsRunning: " + test.upgradeIsRunning +
         " upgradeSuspended: " + test.upgradeSuspended +
         " status: " + test.status +
         " isCompatible: " + test.isCompatible +
         " isCurrentStackPresent: " + test.isCurrentStackPresent, function() {
        this.mock.withArgs('upgradeSuspended').returns(test.upgradeSuspended);
        this.mock.withArgs('upgradeIsRunning').returns(test.upgradeIsRunning);
        view.set('parentView.repoVersions', [Em.Object.create({
          status: test.status
        })]);
        view.set('isCurrentStackPresent', test.isCurrentStackPresent)
        view.set('controller.requestInProgress', test.requestInProgress);
        view.set('content.isCompatible', test.isCompatible);
        expect(view.isDisabledOnInit()).to.be.equal(test.expected);
      });
    });
  });

  describe("#isDisabledOnInstalled()", function () {

    beforeEach(function() {
      this.authorizedMock = sinon.stub(App, 'isAuthorized');
    });

    afterEach(function() {
      this.authorizedMock.restore();
    });

    var testCases = [
      {
        isAuthorized: false,
        requestInProgress: false,
        status: 'INSTALLED',
        isDowngrade: false,
        repositoryName: 'HDP-2.2',
        upgradeVersion: 'HDP-2.3',
        isCurrentStackPresent: true,
        expected: true
      },
      {
        isAuthorized: true,
        requestInProgress: true,
        status: 'INSTALLED',
        isDowngrade: false,
        repositoryName: 'HDP-2.2',
        upgradeVersion: 'HDP-2.3',
        isCurrentStackPresent: true,
        expected: true
      },
      {
        isAuthorized: true,
        requestInProgress: false,
        status: 'INSTALLING',
        isDowngrade: false,
        repositoryName: 'HDP-2.2',
        upgradeVersion: 'HDP-2.3',
        isCurrentStackPresent: true,
        expected: true
      },
      {
        isAuthorized: true,
        requestInProgress: false,
        status: 'INSTALLED',
        isDowngrade: true,
        repositoryName: 'HDP-2.2',
        upgradeVersion: 'HDP-2.2',
        isCurrentStackPresent: true,
        expected: true
      },
      {
        isAuthorized: true,
        requestInProgress: false,
        status: 'INSTALLED',
        isDowngrade: true,
        repositoryName: 'HDP-2.2',
        upgradeVersion: 'HDP-2.3',
        isCurrentStackPresent: true,
        expected: false
      },
      {
        isAuthorized: true,
        requestInProgress: false,
        status: 'INSTALLED',
        isDowngrade: false,
        repositoryName: 'HDP-2.2',
        upgradeVersion: 'HDP-2.2',
        isCurrentStackPresent: true,
        expected: false
      },
      {
        isAuthorized: true,
        requestInProgress: false,
        status: 'INSTALLED',
        isDowngrade: false,
        repositoryName: 'HDP-2.2',
        upgradeVersion: 'HDP-2.2',
        isCurrentStackPresent: false,
        expected: true
      }
    ];

    testCases.forEach(function(test) {
      it( "isAuthorized: " + test.isAuthorized +
          "requestInProgress: " + test.requestInProgress +
          "status: " + test.status +
          "isDowngrade: " + test.isDowngrade +
          "repositoryName: " + test.repositoryName +
          "upgradeVersion: " + test.upgradeVersion +
          "isCurrentStackPresent: " + test.isCurrentStackPresent, function() {
        this.authorizedMock.returns(test.isAuthorized);
        view.set('controller.requestInProgress', test.requestInProgress);
        view.set('parentView.repoVersions', [Em.Object.create({status: test.status})]);
        view.set('controller.isDowngrade', test.isDowngrade);
        view.set('controller.currentVersion.repository_name', test.repositoryName);
        view.set('controller.upgradeVersion', test.upgradeVersion);
        view.set('isCurrentStackPresent', test.isCurrentStackPresent);
        expect(view.isDisabledOnInstalled()).to.be.equal(test.expected);
      });
    });
  });

  describe('#processSuspendedState', function() {

    it('should process suspended state', function() {
      view.set('controller.requestInProgress', false);
      var element = Em.Object.create();
      view.processSuspendedState(element);
      expect(element).to.be.eql(Em.Object.create({
        "action": "resumeUpgrade",
        "isButton": true,
        "isDisabled": false,
        "text": Em.I18n.t('admin.stackUpgrade.dialog.resume')
      }));
    });
  });

  describe('#processUpgradingState', function() {

    beforeEach(function() {
      this.mock = sinon.stub(App, 'get');
    });
    afterEach(function() {
      this.mock.restore();
    });

    it('downgrade in HOLDING state', function() {
      this.mock.returns('HOLDING');
      view.set('controller.isDowngrade', true);
      var element = Em.Object.create();
      view.processUpgradingState(element);
      expect(element).to.be.eql(Em.Object.create({
        "action": "openUpgradeDialog",
        "iconClass": "glyphicon glyphicon-pause",
        "isLink": true,
        "text": Em.I18n.t('admin.stackVersions.version.downgrade.pause')
      }));
    });

    it('upgrade in HOLDING state', function() {
      this.mock.returns('HOLDING');
      view.set('controller.isDowngrade', false);
      var element = Em.Object.create();
      view.processUpgradingState(element);
      expect(element).to.be.eql(Em.Object.create({
        "action": "openUpgradeDialog",
        "iconClass": "glyphicon glyphicon-pause",
        "isLink": true,
        "text": Em.I18n.t('admin.stackVersions.version.upgrade.pause')
      }));
    });

    it('upgrade in running state', function() {
      this.mock.returns('UPGRADING');
      view.set('controller.isDowngrade', false);
      var element = Em.Object.create();
      view.processUpgradingState(element);
      expect(element).to.be.eql(Em.Object.create({
        "action": "openUpgradeDialog",
        "iconClass": "glyphicon glyphicon-cog",
        "isLink": true,
        "text": Em.I18n.t('admin.stackVersions.version.upgrade.running')
      }));
    });

    it('downgrade in running state', function() {
      this.mock.returns('UPGRADING');
      view.set('controller.isDowngrade', true);
      var element = Em.Object.create();
      view.processUpgradingState(element);
      expect(element).to.be.eql(Em.Object.create({
        "action": "openUpgradeDialog",
        "iconClass": "glyphicon glyphicon-cog",
        "isLink": true,
        "text": Em.I18n.t('admin.stackVersions.version.downgrade.running')
      }));
    });
  });

  describe('#processPreUpgradeState', function() {
    beforeEach(function() {
      sinon.stub(view, 'isDisabledOnInstalled').returns(false);
    });
    afterEach(function() {
      view.isDisabledOnInstalled.restore();
    });

    it('version lower than current and in INSTALLED state', function() {
      view.set('controller', Em.Object.create({
        currentVersion: Em.Object.create({
          repository_version: '2.0',
          stack_name: 'HDP'
        })
      }));
      view.set('content', Em.Object.create({
        status: 'INSTALLED',
        repositoryVersion: '2.0',
        stackVersionType: 'HDP'
      }));
      var element = Em.Object.create();
      view.processPreUpgradeState(element);
      expect(element).to.be.eql(Em.Object.create({
        "action": null,
        "iconClass": "glyphicon glyphicon-ok",
        "isButtonGroup": true,
        "text": "Installed"
      }));
    });

    it('version higher than current and in OUT_OF_SYNC state', function() {
      view.set('controller', Em.Object.create({
        currentVersion: Em.Object.create({
          repository_version: '2.0',
          stack_name: 'HDP'
        })
      }));
      view.set('content', Em.Object.create({
        status: 'OUT_OF_SYNC',
        repositoryVersion: '2.1',
        stackVersionType: 'HDP'
      }));
      var element = Em.Object.create({
        buttons: []
      });
      view.processPreUpgradeState(element);
      expect(JSON.stringify(element)).to.be.equal(JSON.stringify(Em.Object.create({
        "buttons": [],
        "isButton": true,
        "text": Em.I18n.t('admin.stackVersions.version.reinstall'),
        "action": 'installRepoVersionPopup',
        "isDisabled": false
      })));
    });

    it('version higher than current and in INSTALLED state hasnt services andis not patch or maint', function() {
      view.set('controller', Em.Object.create({
        currentVersion: Em.Object.create({
          repository_version: '2.0',
          stack_name: 'HDP'
        })
      }));
      view.set('content', Em.Object.create({
        status: 'INSTALLED',
        repositoryVersion: '2.1',
        stackVersionType: 'HDP',
        isPatch: false
      }));
      var element = Em.Object.create({
        buttons: []
      });
      view.processPreUpgradeState(element);
      expect(JSON.stringify(element)).to.be.equal(JSON.stringify(Em.Object.create({
        "buttons": [],
        "isButtonGroup": true,
        'iconClass': 'icon-ok',
        "text": Em.I18n.t('common.installed'),
        "isDisabled": false
      })));
    });

    it('version higher than current and in INSTALLED state hasnt services ant is patch', function() {
      view.set('controller', Em.Object.create({
        currentVersion: Em.Object.create({
          repository_version: '2.0',
          stack_name: 'HDP'
        })
      }));
      view.set('content', Em.Object.create({
        status: 'INSTALLED',
        repositoryVersion: '2.1',
        stackVersionType: 'HDP',
        isPatch: true
      }));
      var element = Em.Object.create({
        buttons: []
      });
      view.processPreUpgradeState(element);
      expect(JSON.stringify(element)).to.be.equal(JSON.stringify(Em.Object.create({
        "buttons": [
          {
           "text":Em.I18n.t('common.hide'),
           "action":"confirmDiscardRepoVersion",
           "isDisabled":false
          }
        ],
        "isButtonGroup": true,
        'iconClass': 'icon-ok',
        "text": Em.I18n.t('common.installed'),
        "isDisabled": false
      })));
    });
  });

  describe('#processNotRequiredState', function() {
    beforeEach(function() {
      sinon.stub(view, 'isDisabledOnInit').returns(false);
    });
    afterEach(function() {
      view.isDisabledOnInit.restore();
    });

    it('version in LOADING state', function() {
      view.set('controller', Em.Object.create({
        requestInProgressRepoId: 1
      }));
      view.set('content', Em.Object.create({
        status: 'NOT_REQUIRED',
        id: 1
      }));
      var element = Em.Object.create({
        buttons: []
      });
      view.processNotRequiredState(element);
      expect(JSON.stringify(element)).to.be.equal(JSON.stringify(Em.Object.create({
        "buttons": [
          {
            "text": Em.I18n.t('common.hide'),
            "action": "confirmDiscardRepoVersion",
            "isDisabled": false
          }
        ],
        "isSpinner": true,
        "class": "spinner",
        "isDisabled": false,
        "isButtonGroup": true,
        "isButton": false
      })));
    });

    it('version in NOT_REQUIRED state', function() {
      view.set('controller', Em.Object.create({
        requestInProgressRepoId: 1
      }));
      view.set('content', Em.Object.create({
        status: 'NOT_REQUIRED',
        id: 2
      }));
      var element = Em.Object.create({
        buttons: []
      });
      view.processNotRequiredState(element);
      expect(JSON.stringify(element)).to.be.equal(JSON.stringify(Em.Object.create({
        "buttons": [
          {
            "text": Em.I18n.t('common.hide'),
            "action": "confirmDiscardRepoVersion",
            "isDisabled": false
          }
        ],
        "isButton": false,
        "text": Em.I18n.t('admin.stackVersions.version.installNow'),
        "action": 'installRepoVersionPopup',
        "isDisabled": false,
        "isButtonGroup": true
      })));
    });
  });
});
