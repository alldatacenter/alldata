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

describe('App.mainAdminStackVersionsView', function () {
  var view;

  beforeEach(function () {
    view = App.MainAdminStackVersionsView.create({
      controller: {
        currentVersion: {
          stack_name: 'HDP',
          repository_version: "2.2.1.0",
          runningCheckRequests: []
        },
        load: Em.K
      }
    });
  });

  afterEach(function () {
    clearTimeout(view.get('updateTimer'));
    view.destroy();
  });

  describe("#filterBy()", function () {
    var versions = [
        Em.Object.create({
          status: "NOT_REQUIRED",
          repositoryVersion: "2.2.1.1",
          stackVersionType: 'HDP',
          hidden: false
        }),
        Em.Object.create({
          status: "INSTALLING",
          repositoryVersion: "2.2.1.1",
          stackVersionType: 'HDP',
          hidden: false
        }),
        Em.Object.create({
          status: "INSTALLED",
          repositoryVersion: "2.0.2.1",
          stackVersionType: 'HCP',
          isCompatible: true,
          hidden: false
        }),
        Em.Object.create({
          status: "INSTALLED",
          repositoryVersion: "2.0.2.2",
          stackVersionType: 'HCP',
          isCompatible: false,
          hidden: false
        }),
        Em.Object.create({
          status: "INSTALLED",
          repositoryVersion: "2.2.2.1",
          stackVersionType: 'HDP',
          hidden: false
        }),
        Em.Object.create({
          status: "INSTALLED",
          repositoryVersion: "2.2.3.1",
          displayName: 'HDP-2.2',
          stackVersionType: 'HDP',
          hidden: false
        }),
        Em.Object.create({
          status: "INSTALL_FAILED",
          repositoryVersion: "2.2.1.1",
          stackVersionType: 'HDP',
          hidden: false
        }),
        Em.Object.create({
          status: "OUT_OF_SYNC",
          repositoryVersion: "2.2.1.1",
          stackVersionType: 'HDP',
          hidden: false
        }),
        Em.Object.create({
          status: "UPGRADED",
          repositoryVersion: "2.2.1.1",
          stackVersionType: 'HDP',
          hidden: false
        }),
        Em.Object.create({
          status: "CURRENT",
          repositoryVersion: "2.2.1.1",
          stackVersionType: 'HDP',
          hidden: false
        })
      ],
      testCases = [
        {
          filter:  Em.Object.create({
            value: ''
          }),
          filteredVersions: [
            Em.Object.create({
              status: "NOT_REQUIRED",
              repositoryVersion: "2.2.1.1",
              stackVersionType: 'HDP',
              hidden: false
            }),
            Em.Object.create({
              status: "INSTALLING",
              repositoryVersion: "2.2.1.1",
              stackVersionType: 'HDP',
              hidden: false
            }),
            Em.Object.create({
              status: "INSTALLED",
              repositoryVersion: "2.0.2.1",
              stackVersionType: 'HCP',
              isCompatible: true,
              hidden: false
            }),
            Em.Object.create({
              status: "INSTALLED",
              repositoryVersion: "2.2.2.1",
              stackVersionType: 'HDP',
              hidden: false
            }),
            Em.Object.create({
              status: "INSTALLED",
              repositoryVersion: "2.2.3.1",
              displayName: 'HDP-2.2',
              stackVersionType: 'HDP',
              hidden: false
            }),
            Em.Object.create({
              status: "INSTALL_FAILED",
              repositoryVersion: "2.2.1.1",
              stackVersionType: 'HDP',
              hidden: false
            }),
            Em.Object.create({
              status: "OUT_OF_SYNC",
              repositoryVersion: "2.2.1.1",
              stackVersionType: 'HDP',
              hidden: false
            }),
            Em.Object.create({
              status: "UPGRADED",
              repositoryVersion: "2.2.1.1",
              stackVersionType: 'HDP',
              hidden: false
            }),
            Em.Object.create({
              status: "CURRENT",
              repositoryVersion: "2.2.1.1",
              stackVersionType: 'HDP',
              hidden: false
            })
          ]
        },
        {
          filter:  Em.Object.create({
            value: 'NOT_INSTALLED'
          }),
          filteredVersions: [
            Em.Object.create({
              status: "NOT_REQUIRED",
              repositoryVersion: "2.2.1.1",
              stackVersionType: 'HDP',
              hidden: false
            }),
            Em.Object.create({
              status: "INSTALLING",
              repositoryVersion: "2.2.1.1",
              stackVersionType: 'HDP',
              hidden: false
            }),
            Em.Object.create({
              status: "INSTALL_FAILED",
              repositoryVersion: "2.2.1.1",
              stackVersionType: 'HDP',
              hidden: false
            }),
            Em.Object.create({
              status: "OUT_OF_SYNC",
              repositoryVersion: "2.2.1.1",
              stackVersionType: 'HDP',
              hidden: false
            })
          ]
        },
        {
          filter:  Em.Object.create({
            value: 'INSTALLED'
          }),
          filteredVersions: [
            Em.Object.create({
              status: "INSTALLED",
              repositoryVersion: "2.0.2.1",
              stackVersionType: 'HCP',
              isCompatible: true,
              hidden: false
            })
          ]
        },
        {
          filter:  Em.Object.create({
            value: 'UPGRADE_READY'
          }),
          filteredVersions: [
            Em.Object.create({
              status: "INSTALLED",
              repositoryVersion: "2.2.2.1",
              stackVersionType: 'HDP',
              hidden: false
            })
          ]
        },
        {
          filter:  Em.Object.create({
            value: 'CURRENT'
          }),
          filteredVersions: [
            Em.Object.create({
              status: "CURRENT",
              repositoryVersion: "2.2.1.1",
              stackVersionType: 'HDP',
              hidden: false
            })
          ]
        },
        {
          filter:  Em.Object.create({
            value: 'UPGRADING'
          }),
          filteredVersions: [
            Em.Object.create({
              status: "INSTALLED",
              repositoryVersion: "2.2.3.1",
              displayName: 'HDP-2.2',
              stackVersionType: 'HDP',
              hidden: false
            })
          ]
        },
        {
          filter:  Em.Object.create({
            value: 'UPGRADED'
          }),
          filteredVersions: [
            Em.Object.create({
              status: "UPGRADED",
              repositoryVersion: "2.2.1.1",
              stackVersionType: 'HDP',
              hidden: false
            })
          ]
        },
        {
          noCurrentVersion: true,
          filter:  Em.Object.create({
            value: ''
          }),
          filteredVersions: versions.filterProperty('hidden', false),
          message: 'no current version'
        },
        {
          displayOlderVersions: true,
          filter:  Em.Object.create({
            value: ''
          }),
          filteredVersions: versions.filterProperty('hidden', false),
          message: 'display older versions'
        }
      ];

    var displayOlderVersions = false;

    beforeEach(function () {
      sinon.stub(App, 'get', function (key) {
        return key === 'supports.displayOlderVersions' ? displayOlderVersions : Em.get(App, key);
      });
      sinon.stub(view, 'isVersionUpgrading', function(v) {
        return v.get('displayName') === 'HDP-2.2';
      });
    });

    afterEach(function () {
      App.get.restore();
      view.isVersionUpgrading.restore();
    });

    testCases.forEach(function(t) {
      var msg = t.filter.get('value') || "All";
      it(t.message || "filter By " + msg, function () {
        displayOlderVersions = t.displayOlderVersions;
        view.set('controller.currentVersion', t.noCurrentVersion ? null : {
          repository_version: '2.2.1.1',
          stack_name: 'HDP'
        });
        expect(view.filterBy(versions, t.filter)).to.eql(t.filteredVersions);
      });
    });
  });

  describe("#didInsertElement()", function() {
    beforeEach(function () {
      sinon.stub(view, 'observesCategories', Em.K);
    });
    afterEach(function () {
      view.observesCategories.restore();
    });
    it("observesCategories is called once", function() {
      view.didInsertElement();
      expect(view.observesCategories.calledOnce).to.be.true;
    });
  });

  describe("#observesCategories()", function () {
    var mock = {format: Em.K};
    beforeEach(function () {
      sinon.stub(Em.I18n, 't').returns(mock);
      sinon.stub(mock, 'format').returns('label');
      sinon.stub(view, 'filterBy').returns([]);
      sinon.stub(view, 'filterVersions');
      view.set('categories', [
        Em.Object.create({
          labelKey: 'labelKey',
          value: 'value',
          isSelected: false
        })
      ]);
      view.observesCategories();
    });
    afterEach(function () {
      Em.I18n.t.restore();
      mock.format.restore();
      view.filterBy.restore();
      view.filterVersions.restore();
    });
    it("categories[0].label is updated", function () {
      expect(view.get('categories')[0].get('label')).to.equal('label');
    });
    it("filterVersions is called once", function () {
      expect(view.filterVersions.calledOnce).to.be.true;
    });
  });

  describe("#selectCategory()", function() {
    var event;
    beforeEach(function () {
      sinon.stub(view, 'filterVersions');
      event = {
        context: Em.Object.create({
          isSelected: false
        })
      };
      view.set('categories', [
        Em.Object.create({
          isSelected: true
        }),
        event.context
      ]);
      view.selectCategory(event);
    });
    afterEach(function () {
      view.filterVersions.restore();
    });
    it("categories[0].isSelected false", function() {
      expect(view.get('categories')[0].get('isSelected')).to.be.false;
    });
    it("isSelected is true", function() {
      expect(event.context.get('isSelected')).to.be.true;
    });
    it("filterVersions is called with correct data", function() {
      expect(view.filterVersions.calledWith(event.context)).to.be.true;
    });
  });

  describe("#filterVersions()", function() {
    beforeEach(function () {
      sinon.stub(view, 'filterBy').returns([{id: 1}]);
      sinon.stub(view, 'observesCategories', Em.K);
    });
    afterEach(function () {
      view.filterBy.restore();
      view.observesCategories.restore();
    });
    it("isVisible for repoVersion is updated", function() {
      view.set('repoVersions', [Em.Object.create({id: 1})]);
      view.filterVersions();
      expect(view.get('repoVersions')[0].get('isVisible')).to.be.true;
    });
  });

  describe('#isVersionUpgrading()', function() {

    beforeEach(function() {
      this.mock = sinon.stub(App.router, 'get');
    });

    afterEach(function() {
      this.mock.restore();
    });

    it('should return false, when version not being upgraded', function() {
      var version = Em.Object.create({
        displayName: 'HDP',
        repositoryVersion: 'HDP'
      });
      this.mock.returns(Em.Object.create({
        upgradeVersion: 'HDP-2',
        fromVersion: 'HDP-1'
      }));
      expect(view.isVersionUpgrading(version)).to.be.false;
    });

    it('should return true, when version being upgraded', function() {
      var version = Em.Object.create({
        displayName: 'HDP-2',
        repositoryVersion: 'HDP'
      });
      this.mock.returns(Em.Object.create({
        upgradeVersion: 'HDP-2',
        fromVersion: 'HDP-1'
      }));
      expect(view.isVersionUpgrading(version)).to.be.true;
    });

    it('should return true, when version being downgraded', function() {
      var version = Em.Object.create({
        displayName: 'HDP',
        repositoryVersion: 'HDP-1'
      });
      this.mock.returns(Em.Object.create({
        upgradeVersion: 'HDP-2',
        fromVersion: 'HDP-1'
      }));
      expect(view.isVersionUpgrading(version)).to.be.true;
    });
  });

  describe("#goToVersions()", function() {
    var data = {
      components: [{
        'RootServiceComponents': {
          'component_version': '1.9.0'
        }
      }, {
        'RootServiceComponents': {
          'component_version': '2.1.0'
        }
      }, {
        'RootServiceComponents': {
          'component_version': '2.0.0'
        }
      }]
    };
    before(function () {
      sinon.spy(App, 'showConfirmationPopup');
      sinon.stub(window.location, 'replace', Em.K);
    });
    after(function () {
      App.showConfirmationPopup.restore();
      window.location.replace.restore();
    });

    beforeEach(function () {
      App.ajax.send.restore();
      sinon.stub(App.ajax, 'send').returns({
        then: function(callback) {
          callback(data);
        }
      });
    });

    it("should go to link using the version retrieved by query", function() {
      var popup = view.goToVersions();
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      var args = testHelpers.findAjaxRequest('name', 'ambari.service.load_server_version');
      expect(args[0]).exists;
      expect(window.location.replace.calledWith('/views/ADMIN_VIEW/2.1.0/INSTANCE/#!/stackVersions')).to.be.true;
    });
  });

  describe("#goToVersionsCustomVersions", function() {
    var data = {
      components: [{
        'RootServiceComponents': {
          'component_version': '1.9.0'
        }
      }, {
        'RootServiceComponents': {
          'component_version': '2.1.0_MyBuild'
        }
      }, {
        'RootServiceComponents': {
          'component_version': '2.0.0'
        }
      }]
    };
    before(function () {
      sinon.spy(App, 'showConfirmationPopup');
      sinon.stub(window.location, 'replace', Em.K);
    });
    after(function () {
      App.showConfirmationPopup.restore();
      window.location.replace.restore();
    });

    beforeEach(function () {
      App.ajax.send.restore();
      sinon.stub(App.ajax, 'send').returns({
        then: function(callback) {
          callback(data);
        }
      });
    });

    it("should go to link using the version retrieved by query", function() {
      var popup = view.goToVersions();
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      var args = testHelpers.findAjaxRequest('name', 'ambari.service.load_server_version');
      expect(args[0]).exists;
      expect(window.location.replace.calledWith('/views/ADMIN_VIEW/2.1.0/INSTANCE/#!/stackVersions')).to.be.true;
    });
  });

  describe("#willInsertElement()", function() {
    beforeEach(function () {
      sinon.stub(view, 'poll', Em.K);
    });
    afterEach(function () {
      view.poll.restore();
    });
    it("poll is called once", function() {
      view.willInsertElement();
      expect(view.poll.calledOnce).to.be.true;
    });
  });

  describe("#willDestroyElement()", function() {
    var request;
    beforeEach(function () {
      request = {
        abort: Em.K
      };
      sinon.spy(window, 'clearTimeout');
      sinon.spy(request, 'abort');
      view.set('controller.runningCheckRequests', [request, request]);
      view.willDestroyElement();
    });
    afterEach(function () {
      window.clearTimeout.restore();
      request.abort.restore();
    });
    it("clearTimeout is called once", function() {
      expect(window.clearTimeout.calledOnce).to.be.true;
    });
    it("abort is called twice", function() {
      expect(request.abort.calledTwice).to.be.true;
    });
    it("runningCheckRequests is empty", function() {
      expect(view.get('controller.runningCheckRequests')).to.have.length(0);
    });
  });

  describe("#poll()", function() {
    beforeEach(function () {
      this.mock = sinon.stub(App.router, 'get');
      sinon.stub(view.get('controller'), 'load').returns({done: Em.K});
    });
    afterEach(function () {
      this.mock.restore();
      view.get('controller').load.restore();
    });
    it("update is working", function() {
      this.mock.returns(Em.Object.create({isWorking: true}));
      view.poll();
      expect(view.get('controller').load.calledOnce).to.be.true;
    });
    it("update is not working", function() {
      this.mock.returns(Em.Object.create({isWorking: false}));
      view.poll();
      expect(view.get('controller').load.called).to.be.false;
    });
  });
});
