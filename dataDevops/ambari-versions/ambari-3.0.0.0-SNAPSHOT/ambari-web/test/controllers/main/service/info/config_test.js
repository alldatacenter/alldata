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
require('controllers/main/service/info/configs');
var batchUtils = require('utils/batch_scheduled_requests');
var mainServiceInfoConfigsController = null;
var testHelpers = require('test/helpers');

function getController() {
  return App.MainServiceInfoConfigsController.create({
    dependentServiceNames: [],
    loadDependentConfigs: function () {
      return {done: Em.K}
    },
    loadConfigTheme: function () {
      return $.Deferred().resolve().promise();
    }
  });
}

describe("App.MainServiceInfoConfigsController", function () {

  beforeEach(function () {
    sinon.stub(App.themesMapper, 'generateAdvancedTabs').returns(Em.K);
    sinon.stub(App.router.get('mainController'), 'startPolling');
    sinon.stub(App.router.get('mainController'), 'stopPolling');
    mainServiceInfoConfigsController = getController();
  });

  App.TestAliases.testAsComputedAlias(getController(), 'serviceConfigs', 'App.config.preDefinedServiceConfigs', 'array');

  afterEach(function() {
    App.themesMapper.generateAdvancedTabs.restore();
    App.router.get('mainController').startPolling.restore();
    App.router.get('mainController').stopPolling.restore();
  });

  describe("#getHash", function () {

    var tests = [
      {
        msg: "properties only used for ui purpose should be excluded from hash",
        configs: [
          Em.Object.create({
            id: "hive.llap.daemon.task.scheduler.enable.preemption",
            isRequiredByAgent: true,
            isFinal: false,
            value: ''
          }),
          Em.Object.create({
            id: "ambari.copy.hive.llap.daemon.num.executors",
            isRequiredByAgent: false,
            isFinal: false,
            value: ''
          })
        ],
        result: JSON.stringify({
          'hive.llap.daemon.task.scheduler.enable.preemption': {
            value: '',
            overrides: [],
            isFinal: false
          }
        })
      },
      {
        msg: "properties should be sorted in alphabetical order",
        configs: [
          Em.Object.create({
            id: "b.b",
            isRequiredByAgent: true,
            isFinal: false,
            value: ''
          }),
          Em.Object.create({
            id: "b.a",
            isRequiredByAgent: true,
            isFinal: false,
            value: ''
          }),
          Em.Object.create({
            id: "b.c",
            isRequiredByAgent: true,
            isFinal: false,
            value: ''
          }),
          Em.Object.create({
            id: "a.b",
            isRequiredByAgent: true,
            isFinal: false,
            value: ''
          })
        ],
        result: JSON.stringify({
          'a.b': {
            value: '',
            overrides: [],
            isFinal: false
          },
          'b.a': {
            value: '',
            overrides: [],
            isFinal: false
          },
          'b.b': {
            value: '',
            overrides: [],
            isFinal: false
          },
          'b.c': {
            value: '',
            overrides: [],
            isFinal: false
          }
        })
      },{
        msg: "properties without id should be sorted with",
        configs: [
          Em.Object.create({
            isRequiredByAgent: true,
            isFinal: false,
            value: '',
            name: 'name',
            filename: 'filename'
          }),
          Em.Object.create({
            id: "a",
            isRequiredByAgent: true,
            isFinal: false,
            value: ''
          })
        ],
        result: JSON.stringify({
          'a': {
            value: '',
            overrides: [],
            isFinal: false
          },
          'name__filename': {
            value: '',
            overrides: [],
            isFinal: false
          }
        })
      }
    ];

    afterEach(function () {
      mainServiceInfoConfigsController.set('selectedService', '');
    });

    tests.forEach(function (t) {
      it(t.msg, function () {
        mainServiceInfoConfigsController.set('selectedService', {configs: t.configs});
        expect(mainServiceInfoConfigsController.getHash()).to.equal(t.result);
      });
    });
  });


  describe("#showSavePopup", function () {
    var tests = [
      {
        transitionCallback: false,
        callback: false,
        action: "onSave",
        m: "save configs without transitionCallback/callback",
        results: [
          {
            method: "restartServicePopup",
            called: true
          }
        ]
      },
      {
        transitionCallback: true,
        callback: true,
        action: "onSave",
        m: "save configs with transitionCallback/callback",
        results: [
          {
            method: "restartServicePopup",
            called: true
          }
        ]
      },
      {
        transitionCallback: false,
        callback: false,
        action: "onDiscard",
        m: "discard changes without transitionCallback/callback",
        results: [
          {
            method: "restartServicePopup",
            called: false
          }
        ]
      },
      {
        transitionCallback: false,
        callback: true,
        action: "onDiscard",
        m: "discard changes with callback",
        results: [
          {
            method: "restartServicePopup",
            called: false
          },
          {
            method: "callback",
            called: true
          },
          {
            field: "hash",
            value: "hash"
          }
        ]
      },
      {
        transitionCallback: true,
        callback: false,
        action: "onDiscard",
        m: "discard changes with transitionCallback",
        results: [
          {
            method: "restartServicePopup",
            called: false
          },
          {
            method: "transitionCallback",
            called: true
          }
        ]
      }
    ];

    beforeEach(function () {
      mainServiceInfoConfigsController.reopen({
        passwordConfigsAreChanged: false
      });
      sinon.stub(mainServiceInfoConfigsController, "get", function(key) {
        return key === 'isSubmitDisabled' ? false : Em.get(mainServiceInfoConfigsController, key);
      });
      sinon.stub(mainServiceInfoConfigsController, "restartServicePopup", Em.K);
      sinon.stub(mainServiceInfoConfigsController, "getHash", function () {
        return "hash"
      });
      sinon.stub(mainServiceInfoConfigsController, 'trackRequest');
    });

    afterEach(function () {
      mainServiceInfoConfigsController.get.restore();
      mainServiceInfoConfigsController.restartServicePopup.restore();
      mainServiceInfoConfigsController.getHash.restore();
      mainServiceInfoConfigsController.trackRequest.restore();
    });

    tests.forEach(function (t) {
      t.results.forEach(function (r) {
        describe(t.m + " " + r.method + " " + r.field, function () {

          beforeEach(function () {
            if (t.callback) {
              t.callback = sinon.stub();
            }
            if (t.transitionCallback) {
              t.transitionCallback = sinon.stub();
            }
            mainServiceInfoConfigsController.showSavePopup(t.transitionCallback, t.callback)[t.action]();
          });


          if (r.method) {
            if (r.method === 'callback') {
              it('callback is ' + (r.called ? '' : 'not') + ' called once', function () {
                expect(t.callback.calledOnce).to.equal(r.called);
              });
            }
            else {
              if (r.method === 'transitionCallback') {
                it('transitionCallback is ' + (r.called ? '' : 'not') + ' called once', function () {
                  expect(t.transitionCallback.calledOnce).to.equal(r.called);
                });
              }
              else {
                it(r.method + ' is ' + (r.called ? '' : 'not') + ' called once', function () {
                  expect(mainServiceInfoConfigsController[r.method].calledOnce).to.equal(r.called);
                });
              }
            }
          }
          else {
            if (r.field) {
              it(r.field + ' is equal to ' + r.value, function () {
                expect(mainServiceInfoConfigsController.get(r.field)).to.equal(r.value);
              });

            }
          }
        }, this);
      });
    }, this);
  });

  describe("#hasUnsavedChanges", function () {
    var cases = [
      {
        hash: null,
        hasUnsavedChanges: false,
        title: 'configs not rendered'
      },
      {
        hash: 'hash1',
        hasUnsavedChanges: true,
        title: 'with unsaved'
      },
      {
        hash: 'hash',
        hasUnsavedChanges: false,
        title: 'without unsaved'
      }
    ];

    beforeEach(function () {
      sinon.stub(mainServiceInfoConfigsController, "getHash", function () {
        return "hash"
      });
    });
    afterEach(function () {
      mainServiceInfoConfigsController.getHash.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        mainServiceInfoConfigsController.set('hash', item.hash);
        expect(mainServiceInfoConfigsController.hasUnsavedChanges()).to.equal(item.hasUnsavedChanges);
      });
    });
  });

  describe("#showComponentsShouldBeRestarted", function () {

    var tests = [
      {
        input: {
          context: {
            restartRequiredHostsAndComponents: {
              'publicHostName1': ['TaskTracker'],
              'publicHostName2': ['JobTracker', 'TaskTracker']
            }
          }
        },
        components: "2 TaskTrackers, 1 JobTracker",
        text: Em.I18n.t('service.service.config.restartService.shouldBeRestarted').format(Em.I18n.t('common.components'))
      },
      {
        input: {
          context: {
            restartRequiredHostsAndComponents: {
              'publicHostName1': ['TaskTracker']
            }
          }
        },
        components: "1 TaskTracker",
        text: Em.I18n.t('service.service.config.restartService.shouldBeRestarted').format(Em.I18n.t('common.component'))
      }
    ];

    beforeEach(function () {
      sinon.stub(mainServiceInfoConfigsController, "showItemsShouldBeRestarted", Em.K);
    });
    afterEach(function () {
      mainServiceInfoConfigsController.showItemsShouldBeRestarted.restore();
    });

    tests.forEach(function (t) {
      it("trigger showItemsShouldBeRestarted popup with components", function () {
        mainServiceInfoConfigsController.showComponentsShouldBeRestarted(t.input);
        expect(mainServiceInfoConfigsController.showItemsShouldBeRestarted.calledWith(t.components, t.text)).to.equal(true);
      });
    });
  });

  describe("#showHostsShouldBeRestarted", function () {

    var tests = [
      {
        input: {
          context: {
            restartRequiredHostsAndComponents: {
              'publicHostName1': ['TaskTracker'],
              'publicHostName2': ['JobTracker', 'TaskTracker']
            }
          }
        },
        hosts: "publicHostName1, publicHostName2",
        text: Em.I18n.t('service.service.config.restartService.shouldBeRestarted').format(Em.I18n.t('common.hosts'))
      },
      {
        input: {
          context: {
            restartRequiredHostsAndComponents: {
              'publicHostName1': ['TaskTracker']
            }
          }
        },
        hosts: "publicHostName1",
        text: Em.I18n.t('service.service.config.restartService.shouldBeRestarted').format(Em.I18n.t('common.host'))
      }
    ];

    beforeEach(function () {
      sinon.stub(mainServiceInfoConfigsController, "showItemsShouldBeRestarted", Em.K);
    });
    afterEach(function () {
      mainServiceInfoConfigsController.showItemsShouldBeRestarted.restore();
    });

    tests.forEach(function (t) {
      it("trigger showItemsShouldBeRestarted popup with hosts", function () {
        mainServiceInfoConfigsController.showHostsShouldBeRestarted(t.input);
        expect(mainServiceInfoConfigsController.showItemsShouldBeRestarted.calledWith(t.hosts, t.text)).to.equal(true);
      });
    });
  });

  describe("#rollingRestartStaleConfigSlaveComponents", function () {
    var tests = [
      {
        componentName: {
          context: "ComponentName"
        },
        displayName: "displayName",
        passiveState: "ON"
      },
      {
        componentName: {
          context: "ComponentName1"
        },
        displayName: "displayName1",
        passiveState: "OFF"
      }
    ];

    beforeEach(function () {
      mainServiceInfoConfigsController.set("content", {displayName: "", passiveState: ""});
      sinon.stub(batchUtils, "launchHostComponentRollingRestart", Em.K);
    });
    afterEach(function () {
      batchUtils.launchHostComponentRollingRestart.restore();
    });
    tests.forEach(function (t) {
      it("trigger rollingRestartStaleConfigSlaveComponents", function () {
        mainServiceInfoConfigsController.set("content.displayName", t.displayName);
        mainServiceInfoConfigsController.set("content.passiveState", t.passiveState);
        mainServiceInfoConfigsController.rollingRestartStaleConfigSlaveComponents(t.componentName);
        expect(batchUtils.launchHostComponentRollingRestart.calledWith(t.componentName.context, t.displayName, t.passiveState === "ON", true)).to.equal(true);
      });
    });
  });

  describe("#restartAllStaleConfigComponents", function () {

    beforeEach(function () {
      sinon.stub(batchUtils, "restartAllServiceHostComponents", Em.K);
    });

    afterEach(function () {
      batchUtils.restartAllServiceHostComponents.restore();
    });

    it("trigger restartAllServiceHostComponents", function () {
      mainServiceInfoConfigsController.restartAllStaleConfigComponents().onPrimary();
      expect(batchUtils.restartAllServiceHostComponents.calledOnce).to.equal(true);
    });

    describe("trigger check last check point warning before triggering restartAllServiceHostComponents", function () {
      var mainConfigsControllerHdfsStarted = App.MainServiceInfoConfigsController.create({
        content: {
          serviceName: "HDFS",
          hostComponents: [{
            componentName: 'NAMENODE',
            workStatus: 'STARTED'
          }],
          restartRequiredHostsAndComponents: {
            "host1": ['NameNode'],
            "host2": ['DataNode', 'ZooKeeper']
          }
        }
      });
      var mainServiceItemController = App.MainServiceItemController.create({});

      beforeEach(function () {
        sinon.stub(mainServiceItemController, 'checkNnLastCheckpointTime', function() {
          return true;
        });
        sinon.stub(App.router, 'get', function(k) {
          if ('mainServiceItemController' === k) {
            return mainServiceItemController;
          }
          return Em.get(App.router, k);
        });
        mainConfigsControllerHdfsStarted.restartAllStaleConfigComponents();
      });

      afterEach(function () {
        mainServiceItemController.checkNnLastCheckpointTime.restore();
        App.router.get.restore();
      });

      it('checkNnLastCheckpointTime is called once', function () {
        expect(mainServiceItemController.checkNnLastCheckpointTime.calledOnce).to.equal(true);
      });


    });
  });

  describe("#doCancel", function () {
    beforeEach(function () {
      sinon.stub(Em.run, 'once', Em.K);
      sinon.stub(mainServiceInfoConfigsController, 'loadSelectedVersion');
      sinon.spy(mainServiceInfoConfigsController, 'clearRecommendations');
      mainServiceInfoConfigsController.set('groupsToSave', { HDFS: 'my cool group'});
      mainServiceInfoConfigsController.set('recommendations', Em.A([{name: 'prop_1'}]));
      mainServiceInfoConfigsController.doCancel();
    });
    afterEach(function () {
      Em.run.once.restore();
      mainServiceInfoConfigsController.loadSelectedVersion.restore();
      mainServiceInfoConfigsController.clearRecommendations.restore();
    });

    it("should launch recommendations cleanup", function() {
      expect(mainServiceInfoConfigsController.clearRecommendations.calledOnce).to.be.true;
    });

    it("should clear dependent configs", function() {
      expect(App.isEmptyObject(mainServiceInfoConfigsController.get('recommendations'))).to.be.true;
    });
  });

  describe("#putChangedConfigurations", function () {
      var sc = [
      Em.Object.create({
        configs: [
          Em.Object.create({
            name: '_heapsize',
            value: '1024m'
          }),
          Em.Object.create({
            name: '_newsize',
            value: '1024m'
          }),
          Em.Object.create({
            name: '_maxnewsize',
            value: '1024m'
          })
        ]
      })
    ],
    scExc = [
      Em.Object.create({
        configs: [
          Em.Object.create({
            name: 'hadoop_heapsize',
            value: '1024m'
          }),
          Em.Object.create({
            name: 'yarn_heapsize',
            value: '1024m'
          }),
          Em.Object.create({
            name: 'nodemanager_heapsize',
            value: '1024m'
          }),
          Em.Object.create({
            name: 'resourcemanager_heapsize',
            value: '1024m'
          }),
          Em.Object.create({
            name: 'apptimelineserver_heapsize',
            value: '1024m'
          }),
          Em.Object.create({
            name: 'jobhistory_heapsize',
            value: '1024m'
          })
        ]
      })
    ];
    beforeEach(function () {
      sinon.stub(App.router, 'getClusterName', function() {
        return 'clName';
      });
    });
    afterEach(function () {
      App.router.getClusterName.restore();
    });
    it("ajax request to put cluster cfg", function () {
      mainServiceInfoConfigsController.set('stepConfigs', sc);
      mainServiceInfoConfigsController.putChangedConfigurations([]);
      var args = testHelpers.findAjaxRequest('name', 'common.across.services.configurations');
      expect(args[0]).exists;
    });
    it('values should be parsed', function () {
      mainServiceInfoConfigsController.set('stepConfigs', sc);
      mainServiceInfoConfigsController.putChangedConfigurations([]);
      expect(mainServiceInfoConfigsController.get('stepConfigs')[0].get('configs').mapProperty('value').uniq()).to.eql(['1024m']);
    });
    it('values should not be parsed', function () {
      mainServiceInfoConfigsController.set('stepConfigs', scExc);
      mainServiceInfoConfigsController.putChangedConfigurations([]);
      expect(mainServiceInfoConfigsController.get('stepConfigs')[0].get('configs').mapProperty('value').uniq()).to.eql(['1024m']);
    });
  });

  describe("#isDirChanged", function() {

    describe("when service name is HDFS", function() {
      beforeEach(function() {
        mainServiceInfoConfigsController.set('content', Ember.Object.create ({ serviceName: 'HDFS' }));
      });

      describe("for hadoop 2", function() {

        var tests = [
          {
            it: "should set dirChanged to false if none of the properties exist",
            expect: false,
            config: Ember.Object.create ({})
          },
          {
            it: "should set dirChanged to true if dfs.namenode.name.dir is not default",
            expect: true,
            config: Ember.Object.create ({
              name: 'dfs.namenode.name.dir',
              isNotDefaultValue: true
            })
          },
          {
            it: "should set dirChanged to false if dfs.namenode.name.dir is default",
            expect: false,
            config: Ember.Object.create ({
              name: 'dfs.namenode.name.dir',
              isNotDefaultValue: false
            })
          },
          {
            it: "should set dirChanged to true if dfs.namenode.checkpoint.dir is not default",
            expect: true,
            config: Ember.Object.create ({
              name: 'dfs.namenode.checkpoint.dir',
              isNotDefaultValue: true
            })
          },
          {
            it: "should set dirChanged to false if dfs.namenode.checkpoint.dir is default",
            expect: false,
            config: Ember.Object.create ({
              name: 'dfs.namenode.checkpoint.dir',
              isNotDefaultValue: false
            })
          },
          {
            it: "should set dirChanged to true if dfs.datanode.data.dir is not default",
            expect: true,
            config: Ember.Object.create ({
              name: 'dfs.datanode.data.dir',
              isNotDefaultValue: true
            })
          },
          {
            it: "should set dirChanged to false if dfs.datanode.data.dir is default",
            expect: false,
            config: Ember.Object.create ({
              name: 'dfs.datanode.data.dir',
              isNotDefaultValue: false
            })
          }
        ];

        beforeEach(function() {
          sinon.stub(App, 'get').returns(true);
        });

        afterEach(function() {
          App.get.restore();
        });

        tests.forEach(function(test) {
          it(test.it, function() {
            mainServiceInfoConfigsController.set('stepConfigs', [Ember.Object.create ({ configs: [test.config], serviceName: 'HDFS' })]);
            expect(mainServiceInfoConfigsController.isDirChanged()).to.equal(test.expect);
          })
        });
      });
    });

  });

  describe("#formatConfigValues", function () {
    var t = {
      configs: [
        Em.Object.create({ name: "p1", value: " v1 v1 ", displayType: "" }),
        Em.Object.create({ name: "p2", value: true, displayType: "" }),
        Em.Object.create({ name: "p3", value: " d1 ", displayType: "directory" }),
        Em.Object.create({ name: "p4", value: " d1 d2 d3 ", displayType: "directories" }),
        Em.Object.create({ name: "p5", value: " v1 ", displayType: "password" }),
        Em.Object.create({ name: "p6", value: " v ", displayType: "host" }),
        Em.Object.create({ name: "javax.jdo.option.ConnectionURL", value: " v1 ", displayType: "string" }),
        Em.Object.create({ name: "oozie.service.JPAService.jdbc.url", value: " v1 ", displayType: "string" })
      ],
      result: [
        Em.Object.create({ name: "p1", value: " v1 v1", displayType: "" }),
        Em.Object.create({ name: "p2", value: "true", displayType: "" }),
        Em.Object.create({ name: "p3", value: "d1", displayType: "directory" }),
        Em.Object.create({ name: "p4", value: "d1,d2,d3", displayType: "directories" }),
        Em.Object.create({ name: "p5", value: " v1 ", displayType: "password" }),
        Em.Object.create({ name: "p6", value: "v", displayType: "host" }),
        Em.Object.create({ name: "javax.jdo.option.ConnectionURL", value: " v1", displayType: "string" }),
        Em.Object.create({ name: "oozie.service.JPAService.jdbc.url", value: " v1", displayType: "string" })
      ]
    };

    it("format config values", function () {
      mainServiceInfoConfigsController.formatConfigValues(t.configs);
      expect(t.configs).to.deep.equal(t.result);
    });

  });

  describe("#checkOverrideProperty", function () {
    var tests = [{
      overrideToAdd: {
        name: "name1",
        filename: "filename1"
      },
      componentConfig: {
        configs: [
          {
            name: "name1",
            filename: "filename2"
          },
          {
            name: "name1",
            filename: "filename1"
          }
        ]
      },
      add: true,
      m: "add property"
    },
      {
        overrideToAdd: {
          name: "name1"
        },
        componentConfig: {
          configs: [
            {
              name: "name2"
            }
          ]
        },
        add: false,
        m: "don't add property, different names"
      },
      {
        overrideToAdd: {
          name: "name1",
          filename: "filename1"
        },
        componentConfig: {
          configs: [
            {
              name: "name1",
              filename: "filename2"
            }
          ]
        },
        add: false,
        m: "don't add property, different filenames"
      },
      {
        overrideToAdd: null,
        componentConfig: {},
        add: false,
        m: "don't add property, overrideToAdd is null"
      }];

    beforeEach(function() {
      sinon.stub(App.config,"createOverride", Em.K)
    });
    afterEach(function() {
      App.config.createOverride.restore();
    });
    tests.forEach(function(t) {
      it(t.m, function() {
        mainServiceInfoConfigsController.set("overrideToAdd", t.overrideToAdd);
        mainServiceInfoConfigsController.checkOverrideProperty(t.componentConfig);
        if(t.add) {
          expect(App.config.createOverride.calledWith(t.overrideToAdd)).to.equal(true);
          expect(mainServiceInfoConfigsController.get("overrideToAdd")).to.equal(null);
        } else {
          expect(App.config.createOverride.calledOnce).to.equal(false);
        }
      });
    });
  });

  describe("#trackRequest()", function () {
    after(function(){
      mainServiceInfoConfigsController.get('requestsInProgress').clear();
    });
    it("should set requestsInProgress", function () {
      var dfd = $.Deferred();
      mainServiceInfoConfigsController.get('requestsInProgress').clear();
      mainServiceInfoConfigsController.trackRequest(dfd);
      expect(mainServiceInfoConfigsController.get('requestsInProgress')[0]).to.eql(
        {
          request: dfd,
          id: 0,
          status: 'pending',
          completed: false
        }
      );
    });
    it('should update request status when it become resolved', function() {
      var request = $.Deferred();
      mainServiceInfoConfigsController.get('requestsInProgress').clear();
      mainServiceInfoConfigsController.trackRequest(request);
      expect(mainServiceInfoConfigsController.get('requestsInProgress')[0]).to.eql({
        request: request,
        id: 0,
        status: 'pending',
        completed: false
      });
      request.resolve();
      expect(mainServiceInfoConfigsController.get('requestsInProgress')[0]).to.eql({
        request: request,
        id: 0,
        status: 'resolved',
        completed: true
      });
    });
  });

  describe('#trackRequestChain', function() {
    beforeEach(function() {
      mainServiceInfoConfigsController.get('requestsInProgress').clear();
    });
    it('should set 2 requests in to requestsInProgress list', function() {
      mainServiceInfoConfigsController.trackRequestChain($.Deferred());
      expect(mainServiceInfoConfigsController.get('requestsInProgress')).to.have.length(2);
    });
    it('should update status for both requests when tracked requests become resolved', function() {
      var request = $.Deferred(),
          requests;
      mainServiceInfoConfigsController.trackRequestChain(request);
      requests = mainServiceInfoConfigsController.get('requestsInProgress');
      assert.deepEqual(requests.mapProperty('status'), ['pending', 'pending'], 'initial statuses');
      assert.deepEqual(requests.mapProperty('completed'), [false, false], 'initial completed');
      request.reject();
      assert.deepEqual(requests.mapProperty('status'), ['rejected', 'resolved'], 'update status when rejected');
      assert.deepEqual(requests.mapProperty('completed'), [true, true], 'initial complete are false');
    });
  });

  describe('#abortRequests', function() {
    var pendingRequest, finishedRequest;

    beforeEach(function() {
      mainServiceInfoConfigsController.get('requestsInProgress').clear();
      finishedRequest = {
        abort: sinon.spy(),
        readyState: 4,
        state: sinon.spy(),
        always: sinon.spy()
      };
      pendingRequest = {
        abort: sinon.spy(),
        readyState: 0,
        state: sinon.spy(),
        always: sinon.spy()
      };
    });

    it('should clear requests when abort called', function() {
      mainServiceInfoConfigsController.trackRequest($.Deferred());
      mainServiceInfoConfigsController.abortRequests();
      expect(mainServiceInfoConfigsController.get('requestsInProgress')).to.have.length(0);
    });

    it('should abort requests which are not finished', function() {
      mainServiceInfoConfigsController.trackRequest(pendingRequest);
      mainServiceInfoConfigsController.trackRequest(finishedRequest);
      mainServiceInfoConfigsController.abortRequests();
      expect(pendingRequest.abort.calledOnce).to.be.true;
      expect(finishedRequest.abort.calledOnce).to.be.false;
    });
  });

  describe("#setCompareDefaultGroupConfig", function() {
    beforeEach(function() {
      sinon.stub(mainServiceInfoConfigsController, "getComparisonConfig").returns("compConfig");
      sinon.stub(mainServiceInfoConfigsController, "getMockComparisonConfig").returns("mockConfig");
      sinon.stub(mainServiceInfoConfigsController, "hasCompareDiffs").returns(true);
    });
    afterEach(function() {
      mainServiceInfoConfigsController.getComparisonConfig.restore();
      mainServiceInfoConfigsController.getMockComparisonConfig.restore();
      mainServiceInfoConfigsController.hasCompareDiffs.restore();
    });
    it("empty service config passed, expect that setCompareDefaultGroupConfig will not run anything", function() {
      expect(mainServiceInfoConfigsController.setCompareDefaultGroupConfig({}).compareConfigs.length).to.equal(0);
    });
    it("empty service config and comparison passed, expect that setCompareDefaultGroupConfig will not run anything", function() {
      expect(mainServiceInfoConfigsController.setCompareDefaultGroupConfig({},{}).compareConfigs).to.eql(["compConfig"]);
    });
    it("expect that serviceConfig.compareConfigs will be getMockComparisonConfig", function() {
      expect(mainServiceInfoConfigsController.setCompareDefaultGroupConfig({isUserProperty: true}, null)).to.eql({compareConfigs: ["mockConfig"], isUserProperty: true, isComparison: true, hasCompareDiffs: true});
    });
    it("expect that serviceConfig.compareConfigs will be getComparisonConfig", function() {
      expect(mainServiceInfoConfigsController.setCompareDefaultGroupConfig({isUserProperty: true}, {})).to.eql({compareConfigs: ["compConfig"], isUserProperty: true, isComparison: true, hasCompareDiffs: true});
    });
    it("expect that serviceConfig.compareConfigs will be getComparisonConfig (2)", function() {
      expect(mainServiceInfoConfigsController.setCompareDefaultGroupConfig({isReconfigurable: true}, {})).to.eql({compareConfigs: ["compConfig"], isReconfigurable: true, isComparison: true, hasCompareDiffs: true});
    });
    it("expect that serviceConfig.compareConfigs will be getComparisonConfig (3)", function() {
      expect(mainServiceInfoConfigsController.setCompareDefaultGroupConfig({isReconfigurable: true, isMock: true}, {})).to.eql({compareConfigs: ["compConfig"], isReconfigurable: true, isMock: true, isComparison: true, hasCompareDiffs: true});
    });
    it("property was created during upgrade and have no comparison, compare with 'Undefined' value should be created", function() {
      expect(mainServiceInfoConfigsController.setCompareDefaultGroupConfig({name: 'prop1', isUserProperty: false}, null)).to.eql({
        name: 'prop1', isUserProperty: false, compareConfigs: ["mockConfig"],
        isComparison: true, hasCompareDiffs: true
      });
    });
  });

  describe('#showSaveConfigsPopup', function () {

    var bodyView;

    describe('#bodyClass', function () {
      beforeEach(function() {
        sinon.stub(App.StackService, 'find').returns([{dependentServiceNames: []}]);
        // default implementation
        bodyView = mainServiceInfoConfigsController.showSaveConfigsPopup().get('bodyClass').create({
          parentView: Em.View.create()
        });
      });

      afterEach(function() {
        App.StackService.find.restore();
      });

      describe('#componentsFilterSuccessCallback', function () {
        it('check components with unknown state', function () {
          bodyView = mainServiceInfoConfigsController.showSaveConfigsPopup('', true, '', {}, '', 'unknown', '').get('bodyClass').create({
            didInsertElement: Em.K,
            parentView: Em.View.create()
          });
          bodyView.componentsFilterSuccessCallback({
            items: [
              {
                ServiceComponentInfo: {
                  total_count: 4,
                  started_count: 2,
                  installed_count: 1,
                  component_name: 'c1'
                },
                host_components: [
                  {HostRoles: {host_name: 'h1'}}
                ]
              }
            ]
          });
          var unknownHosts = bodyView.get('unknownHosts');
          expect(unknownHosts.length).to.equal(1);
          expect(unknownHosts[0]).to.eql({name: 'h1', components: 'C1'});
        });
      });
    });
  });

  describe('#errorsCount', function () {

    it('should ignore configs with isInDefaultTheme=false', function () {

      mainServiceInfoConfigsController.reopen({selectedService: Em.Object.create({
        configsWithErrors: Em.A([
          Em.Object.create({isInDefaultTheme: true}),
          Em.Object.create({isInDefaultTheme: null})
        ])
      })});

      expect(mainServiceInfoConfigsController.get('errorsCount')).to.equal(1);

    });

  });

  describe('#_onLoadComplete', function () {

    beforeEach(function () {
      sinon.stub(Em.run, 'next', Em.K);
      mainServiceInfoConfigsController.setProperties({
        dataIsLoaded: false,
        versionLoaded: false,
        isInit: true
      });
    });

    afterEach(function () {
      Em.run.next.restore();
    });

    it('should update flags', function () {

      mainServiceInfoConfigsController._onLoadComplete();
      expect(mainServiceInfoConfigsController.get('dataIsLoaded')).to.be.true;
      expect(mainServiceInfoConfigsController.get('versionLoaded')).to.be.true;
      expect(mainServiceInfoConfigsController.get('isInit')).to.be.false;

    });

  });

  describe('#hasCompareDiffs', function () {

    it('should return false for `password`-configs', function () {

      var hasCompareDiffs = mainServiceInfoConfigsController.hasCompareDiffs({displayType: 'password'}, {});
      expect(hasCompareDiffs).to.be.false;

    });

  });

  describe('#getServicesDependencies', function() {
    var createService = function(serviceName, dependencies) {
      return Em.Object.create({
        serviceName: serviceName,
        dependentServiceNames: dependencies || []
      });
    };
    var stackServices = [
      createService('STORM', ['RANGER', 'ATLAS', 'ZOOKEEPER']),
      createService('RANGER', ['HIVE', 'HDFS']),
      createService('HIVE', ['YARN']),
      createService('ZOOKEEPER', ['HDFS']),
      createService('ATLAS'),
      createService('HDFS', ['ZOOKEEPER']),
      createService('YARN', ['HIVE'])
    ];
    beforeEach(function() {
      sinon.stub(App.StackService, 'find', function(serviceName) {
        return stackServices.findProperty('serviceName', serviceName);
      });
    });
    afterEach(function() {
      App.StackService.find.restore();
    });

    it('should returns all service dependencies STORM service', function() {
      var result = mainServiceInfoConfigsController.getServicesDependencies('STORM');
      expect(result).to.be.eql(['RANGER', 'ATLAS', 'ZOOKEEPER', 'HIVE', 'HDFS', 'YARN']);
    });

    it('should returns all service dependencies for ATLAS', function() {
      var result = mainServiceInfoConfigsController.getServicesDependencies('ATLAS');
      expect(result).to.be.eql([]);
    });

    it('should returns all service dependencies for RANGER', function() {
      var result = mainServiceInfoConfigsController.getServicesDependencies('RANGER');
      expect(result).to.be.eql(['HIVE', 'HDFS', 'YARN', 'ZOOKEEPER']);
    });

    it('should returns all service dependencies for YARN', function() {
      var result = mainServiceInfoConfigsController.getServicesDependencies('YARN');
      expect(result).to.be.eql(['HIVE']);
    });
  });
});
