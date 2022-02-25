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
require('models/stack_service_component');
require('models/hosts');
require('controllers/wizard');
require('controllers/installer');
require('controllers/wizard/step9_controller');
require('utils/helper');
require('utils/ajax/ajax');
var testHelpers = require('test/helpers');

var modelSetup = require('test/init_model_test');
var c, obj;

function getController() {
  return App.WizardStep9Controller.create({
    content: {controllerName: '', cluster: {status: ''}},
    saveClusterStatus: Em.K,
    saveInstalledHosts: Em.K,
    togglePreviousSteps: Em.K,
    setFinishState: Em.K,
    changeParseHostInfo: Em.K,
    parseHostInfoPolling: Em.K,
    wizardController: Em.Object.create({
      requestsId: [],
      cluster: {oldRequestsId: []},
      getDBProperty: function(name) {
        return this.get(name);
      }
    })
  });
}

describe('App.InstallerStep9Controller', function () {

  beforeEach(function () {
    App.set('supports.skipComponentStartAfterInstall', false);
    modelSetup.setupStackServiceComponent();
    c = getController();
    obj = App.InstallerController.create();
    App.ajax.send.restore();
    sinon.stub(App.ajax, 'send', function() {
      return {
        then: function() { 
          return true;
        },
        retry: function() {
          return {
            then: Em.K,
            complete: Em.K
          };
        },
        complete: Em.K
      };
    });
  });

  afterEach(function () {
    modelSetup.cleanStackServiceComponent();
  });

  App.TestAliases.testAsComputedEqual(getController(), 'showRetry', 'content.cluster.status', 'INSTALL FAILED');

  describe('#isSubmitDisabled', function () {
    var tests = Em.A([
      {controllerName: 'addHostController', state: 'STARTED', e: false},
      {controllerName: 'addHostController', state: 'START_SKIPPED', e: false},
      {controllerName: 'addHostController', state: 'START FAILED', e: false},
      {controllerName: 'addHostController', state: 'INSTALL FAILED', e: false},
      {controllerName: 'addHostController', state: 'PENDING', e: true},
      {controllerName: 'addHostController', state: 'INSTALLED', e: true},
      {controllerName: 'addServiceController', state: 'STARTED', e: false},
      {controllerName: 'addServiceController', state: 'START_SKIPPED', e: false},
      {controllerName: 'addServiceController', state: 'START FAILED', e: false},
      {controllerName: 'addServiceController', state: 'INSTALL FAILED', e: false},
      {controllerName: 'addServiceController', state: 'PENDING', e: true},
      {controllerName: 'addServiceController', state: 'INSTALLED', e: true},
      {controllerName: 'installerController', state: 'STARTED', e: false},
      {controllerName: 'installerController', state: 'START_SKIPPED', e: false},
      {controllerName: 'installerController', state: 'START FAILED', e: false},
      {controllerName: 'installerController', state: 'INSTALL FAILED', e: true},
      {controllerName: 'installerController', state: 'INSTALLED', e: true},
      {controllerName: 'installerController', state: 'PENDING', e: true}
    ]);
    tests.forEach(function (test) {
      var controller = App.WizardStep9Controller.create({
        saveClusterStatus: Em.K,
        saveInstalledHosts: Em.K,
        content: {
          controllerName: test.controllerName,
          cluster: {
            status: test.state
          }
        }
      });
      it('controllerName is ' + test.controllerName + '; cluster status is ' + test.state + '; isSubmitDisabled should be ' + test.e, function () {
        expect(controller.get('isSubmitDisabled')).to.equal(test.e);
      });
    });

  });

  describe('#status', function () {
    var tests = Em.A([
      {
        hosts: [
          {status: 'failed'},
          {status: 'success'}
        ],
        isStepFailed: false,
        progress: '100',
        m: 'One host is failed',
        e: 'failed'
      },
      {
        hosts: [
          {status: 'warning'},
          {status: 'success'}
        ],
        m: 'One host is failed and step is not failed',
        isStepFailed: false,
        progress: '100',
        e: 'warning'
      },
      {
        hosts: [
          {status: 'warning'},
          {status: 'success'}
        ],
        m: 'One host is failed and step is failed',
        isStepFailed: true,
        progress: '100',
        e: 'failed'
      },
      {
        hosts: [
          {status: 'success'},
          {status: 'success'}
        ],
        m: 'All hosts are success and progress is 100',
        isStepFailed: false,
        progress: '100',
        e: 'success'
      },
      {
        hosts: [
          {status: 'success'},
          {status: 'success'}
        ],
        m: 'All hosts are success and progress is 50',
        isStepFailed: false,
        progress: '50',
        e: 'info'
      }
    ]);
    tests.forEach(function (test) {
      var controller = App.WizardStep9Controller.create({
        saveClusterStatus: Em.K,
        saveInstalledHosts: Em.K,
        hosts: test.hosts,
        isStepFailed: function () {
          return test.isStepFailed
        },
        progress: test.progress
      });
      controller.updateStatus();
      it(test.m, function () {
        expect(controller.get('status')).to.equal(test.e);
      });
    });
  });

  describe('#resetHostsForRetry', function () {
    var hosts = {'host1': Em.Object.create({status: 'failed', message: 'Failed'}), 'host2': Em.Object.create({status: 'success', message: 'Success'})};

    beforeEach(function () {
      c.reopen({content: {hosts: hosts}});
      c.resetHostsForRetry();
    });

    Object.keys(hosts).forEach(function (name) {
      if (hosts.hasOwnProperty(name)) {
        it(name + '.status', function () {
          expect(c.get('content.hosts')[name].get('status', 'pending')).to.equal('pending');
        });
        it(name + '.message', function () {
          expect(c.get('content.hosts')[name].get('message', 'Waiting')).to.equal('Waiting');
        });
      }
    });

  });

  describe('#setParseHostInfo', function () {
    var tasks = Em.A([
      Em.Object.create({
        Tasks: Em.Object.create({
          host_name: 'host1',
          status: 'PENDING'
        })
      }),
      Em.Object.create({
        Tasks: Em.Object.create({
          host_name: 'host1',
          status: 'PENDING'
        })
      })
    ]);
    var content = Em.Object.create({
      cluster: Em.Object.create({
        requestId: '11',
        status: 'PENDING'
      })
    });
    beforeEach(function(){
      c.set('content', content)
    });
    it('Should make parseHostInfo false"', function () {
      var polledData = Em.Object.create({
        tasks: tasks,
        Requests: Em.Object.create({
          id: '222'
        })
      });
      c.setParseHostInfo(polledData);
      expect(c.get('parseHostInfo')).to.be.false;
    });
    it('Should set polledData"', function () {
      var polledData = Em.Object.create({
        tasks: tasks,
        Requests: Em.Object.create({
          id: '11'
        })
      });
      c.setParseHostInfo(polledData);
      var expected = [
        {
          "Tasks": {
            "status": "PENDING",
            "host_name": "host1",
            "request_id": "11"
          }
        },
        {
          "Tasks": {
            "status": "PENDING",
            "host_name": "host1",
            "request_id": "11"
          }
        }
      ];
      var result = JSON.parse(JSON.stringify(c.get('polledData')));
      expect(result).to.eql(expected);
    });
    it('Should set progress for hosts"', function () {
      var polledData = Em.Object.create({
        tasks: tasks,
        Requests: Em.Object.create({
          id: '11'
        })
      });
      var hosts = Em.A([
        Em.Object.create({
          name: 'host1',
          logTasks: [
            {Tasks: {role: 'HDFS_CLIENT'}},
            {Tasks: {role: 'DATANODE'}}
          ],
          status: 'old_status',
          progress: '10',
          isNoTasksForInstall: true,
          e: {status: 'old_status', progress: '10'}
        }),
        Em.Object.create({
          name: 'host2',
          logTasks: [
            {Tasks: {role: 'HDFS_CLIENT'}}
          ],
          status: 'old_status',
          progress: '10',
          e: {status: 'success', progress: '100'}
        })
      ]);
      c.set('hosts', hosts);
      c.setParseHostInfo(polledData);
      var expected = [
        {
          "name": "host1",
          "logTasks": [
            {
              "Tasks": {
                "role": "HDFS_CLIENT",
                "status": "PENDING"
              }
            },
            {
              "Tasks": {
                "role": "DATANODE"
              }
            }
          ],
          "progress": "0",
          "isNoTasksForInstall": false,
          "e": {
            "status": "old_status",
            "progress": "10"
          },
          "status": "in_progress",
          "message": ""
        },
        {
          "name": "host2",
          "logTasks": [
            {
              "Tasks": {
                "role": "HDFS_CLIENT"
              }
            }
          ],
          "progress": "33",
          "e": {
            "status": "success",
            "progress": "100"
          },
          "status": "pending",
          "isNoTasksForInstall": true,
          "message": "Install complete (Waiting to start)"
        }
      ];

      var result = JSON.parse(JSON.stringify(c.get('hosts')));
      expect(result).to.eql(expected);
    });
  });

  var hostsForLoadAndRender = {
    'host1': {
      message: 'message1',
      status: 'unknown',
      progress: '1',
      logTasks: [
        {},
        {}
      ],
      bootStatus: 'REGISTERED'
    },
    'host2': {
      message: '',
      status: 'failed',
      progress: '1',
      logTasks: [
        {},
        {}
      ],
      bootStatus: ''
    },
    'host3': {
      message: '',
      status: 'waiting',
      progress: null,
      logTasks: [
        {},
        {}
      ],
      bootStatus: ''
    },
    'host4': {
      message: 'message4',
      status: null,
      progress: '10',
      logTasks: [
        {}
      ],
      bootStatus: 'REGISTERED'
    }
  };

  describe('#loadHosts', function () {

    beforeEach(function() {
      c.reopen({content: {hosts: hostsForLoadAndRender}});
      c.loadHosts();
    });

    it('Only REGISTERED hosts', function () {
      var loadedHosts = c.get('hosts');
      expect(loadedHosts.length).to.equal(2);
    });

    it('All hosts have progress 0', function () {
      var loadedHosts = c.get('hosts');
      expect(loadedHosts.everyProperty('progress', 0)).to.equal(true);
    });

    it('All host don\'t have logTasks', function () {
      var loadedHosts = c.get('hosts');
      expect(loadedHosts.everyProperty('logTasks.length', 0)).to.equal(true);
    });
  });

  describe('#isServicesStarted', function () {
    it('Should return false when server not started', function () {
      var polledData = Em.A([
        Em.Object.create({
          Tasks: Em.Object.create({
            status: 'PENDING'
          })
        }),
        Em.Object.create({
          Tasks: Em.Object.create({
            status: 'PENDING'
          })
        })
      ]);
      expect(c.isServicesStarted(polledData)).to.be.false;
    });
    it('Should return true when server started', function () {
      var polledData = Em.A([
        Em.Object.create({
          Tasks: Em.Object.create({
            status: 'NONE'
          })
        }),
        Em.Object.create({
          Tasks: Em.Object.create({
            status: 'NONE'
          })
        })
      ]);
      expect(c.isServicesStarted(polledData)).to.be.true;
    });
    it('Should return true when tasks completed', function () {
      var polledData = Em.A([
        Em.Object.create({
          Tasks: Em.Object.create({
            status: 'COMPLETED'
          })
        }),
        Em.Object.create({
          Tasks: Em.Object.create({
            status: 'COMPLETED'
          })
        })
      ]);
      expect(c.isServicesStarted(polledData)).to.be.true;
    });
  });

  describe('#setIsServicesInstalled', function () {

    Em.A([
      {
        m: 'Should return 100% completed',
        c: {
          status: 'failed',
          isPolling: true,
          hosts: Em.A([
            Em.Object.create({
              progress: 0
            })
          ])
        },
        polledData: Em.A([
          Em.Object.create({
            Tasks: Em.Object.create({
              status: 'NONE'
            })
          }),
          Em.Object.create({
            Tasks: Em.Object.create({
              status: 'NONE'
            })
          })
        ]),
        e: {
          progress: '100',
          isPolling: false
        }
      },
      {
        m: 'Should return 34% completed',
        c: {
          status: '',
          isPolling: true,
          hosts: Em.A([
            Em.Object.create({
              progress: 0
            })
          ]),
          content: Em.Object.create({
            controllerName: 'installerController'
          })
        },
        polledData: Em.A([
          Em.Object.create({
            Tasks: Em.Object.create({
              status: 'NONE'
            })
          }),
          Em.Object.create({
            Tasks: Em.Object.create({
              status: 'NONE'
            })
          })
        ]),
        e: {
          progress: '34',
          isPolling: true
        }
      }
    ]).forEach(function (test) {
      it(test.m, function () {
        c.setProperties(test.c);
        c.setIsServicesInstalled(test.polledData);
        expect(c.get('progress')).to.equal(test.e.progress);
        expect(c.get('isPolling')).to.be.equal(test.e.isPolling);
      });
    });

    describe('`skipComponentStartAfterInstall` is true', function () {

      var polledData = Em.A([
        Em.Object.create({
          Tasks: Em.Object.create({
            status: 'NONE'
          })
        }),
        Em.Object.create({
          Tasks: Em.Object.create({
            status: 'NONE'
          })
        })
      ]);

      var hosts = [
        Em.Object.create({status: '', message: '', progress: 0}),
        Em.Object.create({status: '', message: '', progress: 0}),
        Em.Object.create({status: '', message: '', progress: 0}),
        Em.Object.create({status: '', message: '', progress: 0})
      ];

      beforeEach(function () {
        sinon.stub(App, 'get').withArgs('supports.skipComponentStartAfterInstall').returns(true);
        sinon.stub(c, 'saveClusterStatus', Em.K);
        sinon.stub(c, 'saveInstalledHosts', Em.K);
        sinon.stub(c, 'changeParseHostInfo', Em.K);
        c.set('hosts', hosts);
        c.setIsServicesInstalled(polledData);
      });

      afterEach(function () {
        App.get.restore();
        c.saveClusterStatus.restore();
        c.saveInstalledHosts.restore();
        c.changeParseHostInfo.restore();
      });

      it('cluster status is valid', function () {
        var clusterStatus = c.saveClusterStatus.args[0][0];
        expect(clusterStatus.status).to.be.equal('START_SKIPPED');
        expect(clusterStatus.isCompleted).to.be.true;
      });

      it('each host status is `success`', function () {
        expect(c.get('hosts').everyProperty('status', 'success')).to.be.true;
      });

      it('each host progress is `100`', function () {
        expect(c.get('hosts').everyProperty('progress', '100')).to.be.true;
      });

      it('each host message is valid', function () {
        expect(c.get('hosts').everyProperty('message', Em.I18n.t('installer.step9.host.status.success'))).to.be.true;
      });

    });

  });

  describe('#launchStartServices', function () {
    beforeEach(function() {
      sinon.stub(App, 'get', function(k) {
        if (k === 'components.slaves') {
          return ["TASKTRACKER", "DATANODE", 
                  "JOURNALNODE", "ZKFC", 
                  "APP_TIMELINE_SERVER", 
                  "NODEMANAGER", 
                  "GANGLIA_MONITOR", 
                  "HBASE_REGIONSERVER", 
                  "SUPERVISOR", 
                  "FLUME_HANDLER"];
        }
        return true;
      });
    });
    afterEach(function() {
      App.get.restore();
    });
    var tests = [
      {
        expected: [],
        message: 'should return query',
        controllerName: 'addHostController',
        hosts: Em.A([
          Em.Object.create({
            name: 'h1'
          }),
          Em.Object.create({
            name: 'h2'
          })
        ])
      },
      {
        expected: [],
        message: 'should return server info',
        controllerName: 'addServiceController',
        services: Em.A([
          Em.Object.create({
            serviceName: 'OOZIE',
            isSelected: true,
            isInstalled: false
          }),
          Em.Object.create({
            serviceName: 'h2',
            isSelected: false,
            isInstalled: true
          })
        ])
      },
      {
        expected: [],
        message: 'should return default data',
        controllerName: 'addHostContro',
        hosts: Em.A([
          Em.Object.create({
            name: 'h1'
          }),
          Em.Object.create({
            name: 'h2'
          })
        ])
      }
    ];
    tests.forEach(function(test) {
      it(test.message, function () {
        var content = Em.Object.create({
          controllerName: test.controllerName,
          services: test.services
        });
        var wizardController = Em.Object.create({
          getDBProperty: function() {
            return test.hosts
          }
        });
        c.set('content', content);
        c.set('wizardController', wizardController);
        expect(c.launchStartServices(function(){})).to.be.true;
      });
    })
  });

  describe('#hostHasClientsOnly', function () {
    var tests = Em.A([
      {
        hosts: [
          Em.Object.create({
            hostName: 'host1',
            logTasks: [
              {Tasks: {role: 'HDFS_CLIENT'}},
              {Tasks: {role: 'DATANODE'}}
            ],
            status: 'old_status',
            progress: '10',
            e: {status: 'old_status', progress: '10'}
          }),
          Em.Object.create({
            hostName: 'host2',
            logTasks: [
              {Tasks: {role: 'HDFS_CLIENT'}}
            ],
            status: 'old_status',
            progress: '10',
            e: {status: 'success', progress: '100'}
          })
        ],
        jsonError: false
      },
      {
        hosts: [
          Em.Object.create({
            hostName: 'host1',
            logTasks: [
              {Tasks: {role: 'HDFS_CLIENT'}},
              {Tasks: {role: 'DATANODE'}}
            ],
            status: 'old_status',
            progress: '10',
            e: {status: 'success', progress: '100'}
          }),
          Em.Object.create({
            hostName: 'host2',
            logTasks: [
              {Tasks: {role: 'HDFS_CLIENT'}}
            ],
            status: 'old_status',
            progress: '10',
            e: {status: 'success', progress: '100'}
          })
        ],
        jsonError: true
      }
    ]);
    tests.forEach(function (test, index1) {
      test.hosts.forEach(function (host, index2) {
        it('#test ' + index1 + ' #host ' + index2, function () {
          c.reopen({hosts: test.hosts});
          c.hostHasClientsOnly(test.jsonError);
          expect(c.get('hosts').findProperty('hostName', host.hostName).get('status')).to.equal(host.e.status);
          expect(c.get('hosts').findProperty('hostName', host.hostName).get('progress')).to.equal(host.e.progress);
        });
      });
    });
  });

  describe('#onSuccessPerHost', function () {
    var tests = Em.A([
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({status: 'pending'}),
        actions: [],
        e: {status: 'success'},
        m: 'No tasks for host'
      },
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({status: 'info'}),
        actions: [
          {Tasks: {status: 'COMPLETED'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {status: 'success'},
        m: 'All Tasks COMPLETED and cluster status INSTALLED'
      },
      {
        cluster: {status: 'FAILED'},
        host: Em.Object.create({status: 'info'}),
        actions: [
          {Tasks: {status: 'COMPLETED'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {status: 'info'},
        m: 'All Tasks COMPLETED and cluster status FAILED'
      },
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({status: 'info'}),
        actions: [
          {Tasks: {status: 'FAILED'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {status: 'info'},
        m: 'Not all Tasks COMPLETED and cluster status INSTALLED'
      },
      {
        cluster: {status: 'FAILED'},
        host: Em.Object.create({status: 'info'}),
        actions: [
          {Tasks: {status: 'FAILED'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {status: 'info'},
        m: 'Not all Tasks COMPLETED and cluster status FAILED'
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        c.reopen({content: {cluster: {status: test.cluster.status}}});
        c.onSuccessPerHost(test.actions, test.host);
        expect(test.host.status).to.equal(test.e.status);
      });
    });
  });

  describe('#onErrorPerHost', function () {
    var tests = Em.A([
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({status: 'pending'}),
        actions: [],
        e: {status: 'pending'},
        isMasterFailed: false,
        m: 'No tasks for host'
      },
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({status: 'info'}),
        actions: [
          {Tasks: {status: 'FAILED'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {status: 'warning'},
        isMasterFailed: false,
        m: 'One Task FAILED and cluster status INSTALLED'
      },
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({status: 'info'}),
        actions: [
          {Tasks: {status: 'ABORTED'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {status: 'warning'},
        isMasterFailed: false,
        m: 'One Task ABORTED and cluster status INSTALLED'
      },
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({status: 'info'}),
        actions: [
          {Tasks: {status: 'TIMEDOUT'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {status: 'warning'},
        isMasterFailed: false,
        m: 'One Task TIMEDOUT and cluster status INSTALLED'
      },
      {
        cluster: {status: 'PENDING'},
        host: Em.Object.create({status: 'info'}),
        actions: [
          {Tasks: {status: 'FAILED'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {status: 'failed'},
        isMasterFailed: true,
        m: 'One Task FAILED and cluster status PENDING isMasterFailed true'
      },
      {
        cluster: {status: 'PENDING'},
        host: Em.Object.create({status: 'info'}),
        actions: [
          {Tasks: {status: 'COMPLETED'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {status: 'info'},
        isMasterFailed: false,
        m: 'One Task FAILED and cluster status PENDING isMasterFailed false'
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        c.reopen({content: {cluster: {status: test.cluster.status}}, isMasterFailed: function () {
          return test.isMasterFailed;
        }});
        c.onErrorPerHost(test.actions, test.host);
        expect(test.host.status).to.equal(test.e.status);
      });
    });
  });

  describe('#isMasterFailed', function () {

    beforeEach(function() {
      sinon.stub(App, 'get', function(k) {
        if (k === 'components.slaves') {
          return ["TASKTRACKER", "DATANODE", "JOURNALNODE", "ZKFC", "APP_TIMELINE_SERVER", "NODEMANAGER", "GANGLIA_MONITOR", "HBASE_REGIONSERVER", "SUPERVISOR", "FLUME_HANDLER"];
        }
        return Em.get(App, k);
      });
    });

    afterEach(function() {
      App.get.restore();
    });

    var tests = Em.A([
      {
        actions: [
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'DATANODE'}},
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'TASKTRACKER'}},
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'HBASE_REGIONSERVER'}},
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'GANGLIA_MONITOR'}},
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'SUPERVISOR'}}
        ],
        e: false,
        m: 'No one Master is failed'
      },
      {
        actions: [
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'NAMENODE'}},
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'TASKTRACKER'}},
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'HBASE_REGIONSERVER'}},
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'GANGLIA_MONITOR'}},
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'SUPERVISOR'}}
        ],
        e: true,
        m: 'One Master is failed'
      },
      {
        actions: [
          {Tasks: {command: 'PENDING', status: 'FAILED', role: 'NAMENODE'}},
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'TASKTRACKER'}},
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'HBASE_REGIONSERVER'}},
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'GANGLIA_MONITOR'}},
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'SUPERVISOR'}}
        ],
        e: false,
        m: 'one Master is failed but command is not install'
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        expect(c.isMasterFailed(test.actions)).to.equal(test.e);
      });
    });
  });

  describe('#onInProgressPerHost', function () {
    var tests = Em.A([
      {
        host: Em.Object.create({message: 'default_message'}),
        actions: [
          {Tasks: {status: 'COMPLETED'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {message: 'default_message', b: true},
        m: 'All Tasks COMPLETED'
      },
      {
        host: Em.Object.create({message: 'default_message'}),
        actions: [
          {Tasks: {status: 'IN_PROGRESS'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {message: 'default_message', b: false},
        m: 'One Task IN_PROGRESS'
      },
      {
        host: Em.Object.create({message: 'default_message'}),
        actions: [
          {Tasks: {status: 'QUEUED'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {message: 'default_message', b: false},
        m: 'One Task QUEUED'
      },
      {
        host: Em.Object.create({message: 'default_message'}),
        actions: [
          {Tasks: {status: 'PENDING'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {message: 'default_message', b: false},
        m: 'One Task PENDING'
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        c.onInProgressPerHost(test.actions, test.host);
        if (test.e.b) {
          expect(test.host.message).to.be.equal(test.e.message);
        }
        else {
          expect(test.host.message).to.be.not.equal(test.e.message);
        }
      });
    });
  });

  describe('#progressPerHost', function () {
    var tests = Em.A([
      {
        cluster: {status: 'PENDING'},
        host: Em.Object.create({progress: 0}),
        actions: {
          'COMPLETED': 2,
          'QUEUED': 2,
          'IN_PROGRESS': 1
        },
        e: {progress: 17},
        s: false,
        m: 'All types of status available. cluster status PENDING'
      },
      {
        cluster: {status: 'PENDING'},
        host: Em.Object.create({progress: 0}),
        actions: {},
        e: {progress: 33},
        s: false,
        m: 'No tasks available. cluster status PENDING'
      },
      {
        cluster: {status: 'PENDING'},
        host: Em.Object.create({progress: 0}),
        actions: {},
        e: {progress: 100},
        s: true,
        m: 'No tasks available. cluster status PENDING. skipComponentStartAfterInstall is true.'
      },
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({progress: 0}),
        actions: {},
        e: {progress: 100},
        m: 'No tasks available. cluster status INSTALLED'
      },
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({progress: 0}),
        actions: {
          'COMPLETED': 2,
          'QUEUED': 2,
          'IN_PROGRESS': 1
        },
        e: {progress: 66},
        s: false,
        m: 'All types of status available. cluster status INSTALLED'
      },
      {
        cluster: {status: 'FAILED'},
        host: Em.Object.create({progress: 0}),
        actions: {},
        e: {progress: 100},
        s: false,
        m: 'Cluster status is not PENDING or INSTALLED'
      },
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({progress: 0}),
        actions: {
          'COMPLETED': 150,
          'QUEUED': 0,
          'IN_PROGRESS': 1
        },
        e: {progress: 99},
        s: false,
        m: '150 tasks on host'
      },
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({progress: 0}),
        actions: {
          'COMPLETED': 498,
          'QUEUED': 1,
          'IN_PROGRESS': 1
        },
        e: {progress: 99},
        s: false,
        m: '500 tasks on host'
      },
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({progress: 0}),
        actions: {
          'COMPLETED': 150,
          'QUEUED': 0,
          'IN_PROGRESS': 0
        },
        e: {progress: 100},
        s: false,
        m: '100 tasks, 100 completed'
      },
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({progress: 0}),
        actions: {
          'COMPLETED': 1,
          'QUEUED': 0,
          'IN_PROGRESS': 0
        },
        e: {progress: 100},
        s: false,
        m: '1 task, 1 completed'
      }
    ]);
    tests.forEach(function (test) {
      describe(test.m, function () {

        beforeEach(function () {
          var actions = [];
          for (var prop in test.actions) {
            if (test.actions.hasOwnProperty(prop) && test.actions[prop]) {
              for (var i = 0; i < test.actions[prop]; i++) {
                actions.push({Tasks: {status: prop}});
              }
            }
          }
          c.reopen({content: {cluster: {status: test.cluster.status}}});
          App.set('supports.skipComponentStartAfterInstall', test.s);
          this.progress = c.progressPerHost(actions, test.host);
        });

        it('progress is ' + test.e.progress, function () {
          expect(this.progress).to.equal(test.e.progress);
        });

        it('host progress is ' + test.e.progress.toString(), function () {
          expect(test.host.progress).to.equal(test.e.progress.toString());
        });
      });
    });
  });

  describe('#clearStep', function () {

    beforeEach(function () {
      c.reopen({hosts: [{},{},{}]});
      c.clearStep();
    });

    it('hosts are empty', function () {
      expect(c.get('hosts.length')).to.equal(0);
    });
    it('status is `info`', function () {
      expect(c.get('status')).to.equal('info');
    });
    it('progress is 0', function () {
      expect(c.get('progress')).to.equal('0');
    });
    it('numPolls is 1', function () {
      expect(c.get('numPolls')).to.equal(1);
    });
  });

  describe('#replacePolledData', function () {
    it('replacing polled data', function () {
      c.reopen({polledData: [{},{},{}]});
      var newPolledData = [{}];
      c.replacePolledData(newPolledData);
      expect(c.get('polledData.length')).to.equal(newPolledData.length);
    });
  });

  describe('#isSuccess', function () {
    var tests = Em.A([
      {
        polledData: [
          {Tasks: {status: 'COMPLETED'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: true,
        m: 'All tasks are COMPLETED'
      },
      {
        polledData: [
          {Tasks: {status: 'COMPLETED'}},
          {Tasks: {status: 'FAILED'}}
        ],
        e: false,
        m: 'Not all tasks are COMPLETED'
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        expect(c.isSuccess(test.polledData)).to.equal(test.e);
      });
    });
  });

  describe('#isStepFailed', function () {

    beforeEach(function() {
      sinon.stub(App, 'get', function(k) {
        if (k === 'components.slaves') {
          return ["TASKTRACKER", "DATANODE", "JOURNALNODE", "ZKFC", "APP_TIMELINE_SERVER", "NODEMANAGER", "GANGLIA_MONITOR", "HBASE_REGIONSERVER", "SUPERVISOR", "FLUME_HANDLER"];
        }
        return Em.get(App, k);
      });
    });

    afterEach(function() {
      App.get.restore();
    });

    var tests = Em.A([
      {
        polledData: [
          {Tasks: {command: 'INSTALL', role: 'GANGLIA_MONITOR', status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL', role: 'GANGLIA_MONITOR', status: 'FAILED'}},
          {Tasks: {command: 'INSTALL', role: 'GANGLIA_MONITOR', status: 'PENDING'}}
        ],
        e: true,
        m: 'GANGLIA_MONITOR 2/3 failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL', role: 'GANGLIA_MONITOR', status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL', role: 'GANGLIA_MONITOR', status: 'PENDING'}},
          {Tasks: {command: 'INSTALL', role: 'GANGLIA_MONITOR', status: 'PENDING'}}
        ],
        e: false,
        m: 'GANGLIA_MONITOR 1/3 failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL', role: 'HBASE_REGIONSERVER', status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL', role: 'HBASE_REGIONSERVER', status: 'FAILED'}},
          {Tasks: {command: 'INSTALL', role: 'HBASE_REGIONSERVER', status: 'PENDING'}}
        ],
        e: true,
        m: 'HBASE_REGIONSERVER 2/3 failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL', role: 'HBASE_REGIONSERVER', status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL', role: 'HBASE_REGIONSERVER', status: 'PENDING'}},
          {Tasks: {command: 'INSTALL', role: 'HBASE_REGIONSERVER', status: 'PENDING'}}
        ],
        e: false,
        m: 'HBASE_REGIONSERVER 1/3 failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL', role: 'TASKTRACKER', status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL', role: 'TASKTRACKER', status: 'FAILED'}},
          {Tasks: {command: 'INSTALL', role: 'TASKTRACKER', status: 'PENDING'}}
        ],
        e: true,
        m: 'TASKTRACKER 2/3 failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL', role: 'TASKTRACKER', status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL', role: 'TASKTRACKER', status: 'PENDING'}},
          {Tasks: {command: 'INSTALL', role: 'TASKTRACKER', status: 'PENDING'}}
        ],
        e: false,
        m: 'TASKTRACKER 1/3 failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL', role: 'DATANODE', status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL', role: 'DATANODE', status: 'FAILED'}},
          {Tasks: {command: 'INSTALL', role: 'DATANODE', status: 'PENDING'}}
        ],
        e: true,
        m: 'DATANODE 2/3 failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL', role: 'DATANODE', status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL', role: 'DATANODE', status: 'PENDING'}},
          {Tasks: {command: 'INSTALL', role: 'DATANODE', status: 'PENDING'}}
        ],
        e: false,
        m: 'DATANODE 1/3 failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL', role: 'NAMENODE', status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL', role: 'DATANODE', status: 'PENDING'}},
          {Tasks: {command: 'INSTALL', role: 'DATANODE', status: 'PENDING'}}
        ],
        e: true,
        m: 'NAMENODE failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL', role: 'NAMENODE', status: 'PENDING'}},
          {Tasks: {command: 'INSTALL', role: 'DATANODE', status: 'PENDING'}},
          {Tasks: {command: 'INSTALL', role: 'DATANODE', status: 'PENDING'}}
        ],
        e: false,
        m: 'Nothing failed failed'
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        c.reopen({polledData: test.polledData});
        expect(c.isStepFailed()).to.equal(test.e);
      });
    });
  });

  describe('#setLogTasksStatePerHost', function () {
    var tests = Em.A([
      {
        tasksPerHost: [
          {Tasks: {id: 1, status: 'COMPLETED'}},
          {Tasks: {id: 2, status: 'COMPLETED'}}
        ],
        tasks: [],
        e: {m: 'COMPLETED', l: 2},
        m: 'host didn\'t have tasks and got 2 new'
      },
      {
        tasksPerHost: [
          {Tasks: {id: 1, status: 'COMPLETED'}},
          {Tasks: {id: 2, status: 'COMPLETED'}}
        ],
        tasks: [
          {Tasks: {id: 1, status: 'IN_PROGRESS'}},
          {Tasks: {id: 2, status: 'IN_PROGRESS'}}
        ],
        e: {m: 'COMPLETED', l: 2},
        m: 'host had 2 tasks and got both updated'
      },
      {
        tasksPerHost: [],
        tasks: [
          {Tasks: {id: 1, status: 'IN_PROGRESS'}},
          {Tasks: {id: 2, status: 'IN_PROGRESS'}}
        ],
        e: {m: 'IN_PROGRESS', l: 2},
        m: 'host had 2 tasks and didn\'t get updates'
      },
      {
        tasksPerHost: [
          {Tasks: {id: 1, status: 'COMPLETED'}},
          {Tasks: {id: 2, status: 'COMPLETED'}},
          {Tasks: {id: 3, status: 'COMPLETED'}}
        ],
        tasks: [
          {Tasks: {id: 1, status: 'IN_PROGRESS'}},
          {Tasks: {id: 2, status: 'IN_PROGRESS'}}
        ],
        e: {m: 'COMPLETED', l: 3},
        m: 'host had 2 tasks and got both updated and 1 new'
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        c.reopen({hosts: [Em.Object.create({logTasks: test.tasks})]});
        c.setLogTasksStatePerHost(test.tasksPerHost, c.get('hosts')[0]);
        var host = c.get('hosts')[0];
        expect(host.get('logTasks').everyProperty('Tasks.status', test.e.m)).to.equal(true);
        expect(host.get('logTasks.length')).to.equal(test.e.l);
      });
    });
  });

 // On completion of Start all services error callback function,
  // Cluster Status should be INSTALL FAILED
  // All progress bar on the screen should be finished (100%) with blue color.
  // Retry button should be enabled, next button should be disabled

  describe('#launchStartServicesErrorCallback', function () {

    it('Main progress bar on the screen should be finished (100%) with red color', function () {
      var hosts = Em.A([Em.Object.create({name: 'host1', progress: '33', status: 'info'}), Em.Object.create({name: 'host2', progress: '33', status: 'info'})]);
      c.reopen({hosts: hosts, content: {controllerName: 'installerController', cluster: {status: 'PENDING', name: 'c1'}}});
      c.launchStartServicesErrorCallback({status: 500, statusTesxt: 'Server Error'}, {}, '', {});
      expect(c.get('progress')).to.equal('100');
      expect(c.get('status')).to.equal('failed');
    });

    it('All Host progress bars on the screen should be finished (100%) with blue color', function () {
      var hosts = Em.A([Em.Object.create({name: 'host1', progress: '33', status: 'info'}), Em.Object.create({name: 'host2', progress: '33', status: 'info'})]);
      c.reopen({hosts: hosts, content: {controllerName: 'installerController', cluster: {status: 'PENDING', name: 'c1'}}});
      c.launchStartServicesErrorCallback({status: 500, statusTesxt: 'Server Error'}, {}, '', {});
      expect(c.get('hosts').everyProperty('progress', '100')).to.be.true;
      expect(c.get('hosts').everyProperty('status', 'info')).to.be.true;
    });

    it('Next button should be disabled', function () {
      var hosts = Em.A([Em.Object.create({name: 'host1', progress: '33', status: 'info'}), Em.Object.create({name: 'host2', progress: '33', status: 'info'})]);
      c.reopen({hosts: hosts, content: {controllerName: 'installerController', cluster: {status: 'PENDING', name: 'c1'}}});
      c.launchStartServicesErrorCallback({status: 500, statusTesxt: 'Server Error'}, {}, '', {});
      expect(c.get('isSubmitDisabled')).to.equal(true);
    });

  });

  describe('#submit', function () {

    beforeEach(function () {
      sinon.stub(App.router, 'send', Em.K);
    });

    afterEach(function () {
      App.router.send.restore();
    });

    it('should call App.router.send', function () {
      c.submit();
      expect(App.router.send.calledWith('next')).to.equal(true);
    });
  });

  describe('#back', function () {
    beforeEach(function () {
      sinon.stub(App.router, 'send', Em.K);
    });
    afterEach(function () {
      App.router.send.restore();
    });
    it('should call App.router.send', function () {
      c.reopen({isSubmitDisabled: false});
      c.back();
      expect(App.router.send.calledWith('back')).to.equal(true);
    });
    it('shouldn\'t call App.router.send', function () {
      c.reopen({isSubmitDisabled: true});
      c.back();
      expect(App.router.send.called).to.equal(false);
    });
  });

  describe('#loadStep', function () {
    beforeEach(function () {
      sinon.stub(c, 'clearStep', Em.K);
      sinon.stub(c, 'loadHosts', Em.K);
    });
    afterEach(function () {
      c.clearStep.restore();
      c.loadHosts.restore();
    });
    it('should call clearStep', function () {
      c.loadStep();
      expect(c.clearStep.calledOnce).to.equal(true);
    });
    it('should call loadHosts', function () {
      c.loadStep();
      expect(c.loadHosts.calledOnce).to.equal(true);
    });
  });

  describe('#startPolling', function () {
    beforeEach(function () {
      sinon.stub(c, 'reloadErrorCallback', Em.K);
      sinon.stub(c, 'doPolling', Em.K);
    });
    afterEach(function () {
      c.reloadErrorCallback.restore();
      c.doPolling.restore();
    });
    it('should set isSubmitDisabled to true', function () {
      c.set('isSubmitDisabled', false);
      c.startPolling();
      expect(c.get('isSubmitDisabled')).to.equal(true);
    });
    it('should call doPolling', function () {
      c.startPolling();
      expect(c.doPolling.calledOnce).to.equal(true);
    });
  });

  describe('#loadLogData', function () {

    beforeEach(function () {
      obj.reopen({
        cluster: {oldRequestsId: [1,2,3]},
        getDBProperty: function (name) {
          return this.get(name);
        }
      });
      c.reopen({wizardController: obj});
      sinon.stub(c, 'getLogsByRequest', Em.K);
    });

    afterEach(function () {
      c.getLogsByRequest.restore();
    });

    it('should call getLogsByRequest 1 time with 3', function () {
      c.loadLogData(true);
      expect(c.getLogsByRequest.calledWith(true, 3)).to.equal(true);
    });

  });

  describe('#loadCurrentTaskLog', function () {
    beforeEach(function () {
      sinon.stub(c, 'loadLogData', Em.K);
      c.set('wizardController', Em.Object.create({
        getDBProperty: Em.K
      }));
      sinon.stub(c, 'togglePreviousSteps', Em.K);
    });
    afterEach(function () {
      c.loadLogData.restore();
      c.togglePreviousSteps.restore();
    });

    it('shouldn\'t call App.ajax.send if no currentOpenTaskId', function () {
      c.set('currentOpenTaskId', null);
      c.loadCurrentTaskLog();
      var args = testHelpers.findAjaxRequest('name', 'background_operations.get_by_task');
      expect(args).not.exists;
    });

    it('should call App.ajax.send with provided data', function () {
      c.set('currentOpenTaskId', 1);
      c.set('currentOpenTaskRequestId', 2);
      c.set('content', {cluster: {name: 3}});
      c.loadCurrentTaskLog();
      var args = testHelpers.findAjaxRequest('name', 'background_operations.get_by_task');
      expect(args[0]).exists;
      expect(args[0].data).to.be.eql({taskId: 1, requestId: 2, clusterName: 3});
    });
  });

  describe('#loadCurrentTaskLogSuccessCallback', function () {

    beforeEach(function() {
      sinon.stub(c, 'getLogsByRequest', Em.K);
      sinon.stub(c, 'loadLogData', Em.K);
    });

    afterEach(function() {
      c.getLogsByRequest.restore();
      c.loadLogData.restore();
    });

    it('should increment logTasksChangesCounter', function () {
      c.set('logTasksChangesCounter', 0);
      c.loadCurrentTaskLogSuccessCallback();
      expect(c.get('logTasksChangesCounter')).to.equal(1);
    });
    it('should update stdout, stderr', function () {
      c.set('currentOpenTaskId', 1);
      c.reopen({
        hosts: [
          Em.Object.create({
            name: 'h1',
            logTasks: [
              {Tasks: {id: 1, stdout: '', stderr: ''}}
            ]
          })
        ]
      });
      var data = {Tasks: {host_name: 'h1', id: 1, stderr: 'stderr', stdout: 'stdout'}};
      c.loadCurrentTaskLogSuccessCallback(data);
      var t = c.get('hosts')[0].logTasks[0].Tasks;
      expect(t.stdout).to.equal('stdout');
      expect(t.stderr).to.equal('stderr');
    });
    it('shouldn\'t update stdout, stderr', function () {
      c.set('currentOpenTaskId', 1);
      c.reopen({
        hosts: [
          Em.Object.create({
            name: 'h1',
            logTasks: [
              {Tasks: {id: 2, stdout: '', stderr: ''}}
            ]
          })
        ]
      });
      var data = {Tasks: {host_name: 'h1', id: 1, stderr: 'stderr', stdout: 'stdout'}};
      c.loadCurrentTaskLogSuccessCallback(data);
      var t = c.get('hosts')[0].logTasks[0].Tasks;
      expect(t.stdout).to.equal('');
      expect(t.stderr).to.equal('');
    });
    it('shouldn\'t update stdout, stderr (2)', function () {
      c.set('currentOpenTaskId', 1);
      c.reopen({
        hosts: [
          Em.Object.create({
            name: 'h2',
            logTasks: [
              {Tasks: {id: 1, stdout: '', stderr: ''}}
            ]
          })
        ]
      });
      var data = {Tasks: {host_name: 'h1', id: 1, stderr: 'stderr', stdout: 'stdout'}};
      c.loadCurrentTaskLogSuccessCallback(data);
      var t = c.get('hosts')[0].logTasks[0].Tasks;
      expect(t.stdout).to.equal('');
      expect(t.stderr).to.equal('');
    });
  });

  describe('#loadCurrentTaskLogErrorCallback', function () {
    it('should set currentOpenTaskId to 0', function () {
      c.set('currentOpenTaskId', 123);
      c.loadCurrentTaskLogErrorCallback();
      expect(c.get('currentOpenTaskId')).to.equal(0);
    });
  });

  describe('#doPolling', function () {

    beforeEach(function () {
      sinon.stub(c, 'getLogsByRequest', Em.K);
      sinon.stub(c, 'togglePreviousSteps', Em.K);
    });

    afterEach(function () {
      c.getLogsByRequest.restore();
      c.togglePreviousSteps.restore();
    });

    it('should call getLogsByRequest', function () {
      c.set('content', {cluster: {requestId: 1}});
      c.doPolling();
      expect(c.getLogsByRequest.calledWith(true, 1)).to.equal(true);
    });

  });

  describe('#isAllComponentsInstalledErrorCallback', function () {
    beforeEach(function () {
      sinon.stub(c, 'saveClusterStatus', Em.K);
      sinon.stub(c, 'togglePreviousSteps', Em.K);
    });
    afterEach(function () {
      c.saveClusterStatus.restore();
      c.togglePreviousSteps.restore();
    });
    it('should call saveClusterStatus', function () {
      c.isAllComponentsInstalledErrorCallback({});
      expect(c.saveClusterStatus.calledOnce).to.equal(true);
    });
  });

  describe('#navigateStep', function () {

    beforeEach(function () {
      sinon.stub(c, 'togglePreviousSteps', Em.K);
      sinon.stub(c, 'loadStep', Em.K);
      sinon.stub(c, 'loadLogData', Em.K);
      sinon.stub(c, 'startPolling', Em.K);
    });

    afterEach(function () {
      c.togglePreviousSteps.restore();
      c.loadStep.restore();
      c.loadLogData.restore();
      c.startPolling.restore();
    });

    it('isCompleted = true, requestId = 1', function () {
      c.reopen({content: {cluster: {isCompleted: true, requestId: 1}}});
      c.navigateStep();
      expect(c.loadStep.calledOnce).to.equal(true);
      expect(c.loadLogData.calledWith(false)).to.equal(true);
      expect(c.get('progress')).to.equal('100');
    });
    it('isCompleted = false, requestId = 1, status = INSTALL FAILED', function () {
      c.reopen({content: {cluster: {status: 'INSTALL FAILED', isCompleted: false, requestId: 1}}});
      c.navigateStep();
      expect(c.loadStep.calledOnce).to.equal(true);
      expect(c.loadLogData.calledWith(false)).to.equal(true);
    });
    it('isCompleted = false, requestId = 1, status = START FAILED', function () {
      c.reopen({content: {cluster: {status: 'START FAILED', isCompleted: false, requestId: 1}}});
      c.navigateStep();
      expect(c.loadStep.calledOnce).to.equal(true);
      expect(c.loadLogData.calledWith(false)).to.equal(true);
    });
    it('isCompleted = false, requestId = 1, status = OTHER', function () {
      c.reopen({content: {cluster: {status: 'STARTED', isCompleted: false, requestId: 1}}});
      c.navigateStep();
      expect(c.loadStep.calledOnce).to.equal(true);
      expect(c.loadLogData.calledWith(true)).to.equal(true);
    });
  });

  describe('#launchStartServicesSuccessCallback', function () {
    beforeEach(function () {
      sinon.stub(App.clusterStatus, 'setClusterStatus', function() {
        return $.ajax();
      });
      sinon.stub(c, 'saveClusterStatus', Em.K);
      sinon.stub(c, 'doPolling', Em.K);
      sinon.stub(c, 'hostHasClientsOnly', Em.K);
    });
    afterEach(function () {
      c.saveClusterStatus.restore();
      c.doPolling.restore();
      c.hostHasClientsOnly.restore();
      App.clusterStatus.setClusterStatus.restore();
    });
    it('should call doPolling if some data were received', function () {
      c.launchStartServicesSuccessCallback({Requests: {id: 2}});
      expect(c.doPolling.calledOnce).to.equal(true);
    });
    Em.A([
        {
          m: 'Launch start service after install services completed',
          jsonData: {Requests: {id: 2}},
          e: {
            hostHasClientsOnly: false,
            clusterStatus: {
              status: 'INSTALLED',
              requestId: 2,
              isStartError: false,
              isCompleted: false
            }
          }
        },
        {
          jsonData: null,
          e: {
            hostHasClientsOnly: true,
            clusterStatus: {
              status: 'STARTED',
              isStartError: false,
              isCompleted: true
            },
            status: 'success',
            progress: '100'
          }
        }
      ]).forEach(function (test) {
        describe(test.m, function () {

          beforeEach(function () {
            c.launchStartServicesSuccessCallback(test.jsonData);
          });

          it('hostHasClientsOnly is called with valid arguments', function () {
            expect(c.hostHasClientsOnly.calledWith(test.e.hostHasClientsOnly)).to.equal(true);
          });

          it('saveClusterStatus is called with valid arguments', function () {
            expect(c.saveClusterStatus.calledWith(test.e.clusterStatus)).to.equal(true);
          });


          if (test.e.status) {
            it('status is valid', function () {
              expect(c.get('status')).to.equal(test.e.status);
            });
          }
          if (test.e.progress) {
            it('progress is valid', function () {
              expect(c.get('progress')).to.equal(test.e.progress);
            });
          }
        });
      });
  });

  describe('#isAllComponentsInstalledSuccessCallback', function () {

    var hosts = [
      Em.Object.create({name: 'h1', status: '', message: '', progress: ''}),
      Em.Object.create({name: 'h2', status: '', message: '', progress: ''})
    ];

    var jsonData = {
      items: [
        {
          Hosts: {
            host_state: 'HEARTBEAT_LOST',
            host_name: 'h1'
          },
          host_components: [
            {
              HostRoles: {
                component_name: 'c1'
              }
            },
            {
              HostRoles: {
                component_name: 'c2'
              }
            }
          ]
        },
        {
          Hosts: {
            host_state: 'HEARTBEAT_LOST',
            host_name: 'h2'
          },
          host_components: [
            {
              HostRoles: {
                component_name: 'c3'
              }
            },
            {
              HostRoles: {
                component_name: 'c4'
              }
            }
          ]
        }
      ]
    };

    beforeEach(function () {
      sinon.stub(c, 'changeParseHostInfo', Em.K);
      sinon.stub(c, 'launchStartServices', Em.K);
      sinon.stub(c, 'saveClusterStatus', Em.K);
      c.set('hosts', hosts);
      c.set('content', Em.Object.create({
        slaveComponentHosts: [
          {hosts: [{isInstalled: true, hostName: 'h1'}]},
          {hosts: [{isInstalled: false, hostName: 'h2'}]}
        ],
        masterComponentHosts: []
      }));
      c.isAllComponentsInstalledSuccessCallback(jsonData);
      this.clusterStatus = c.saveClusterStatus.args[0][0];
    });

    afterEach(function () {
      c.changeParseHostInfo.restore();
      c.launchStartServices.restore();
      c.saveClusterStatus.restore();
    });

    it('cluster status / status', function () {
      expect(this.clusterStatus.status).to.be.equal('INSTALL FAILED');
    });

    it('cluster status / isStartError', function () {
      expect(this.clusterStatus.isStartError).to.be.true;
    });

    it('cluster status / isCompleted', function () {
      expect(this.clusterStatus.isCompleted).to.be.false;
    });

    it('each host progress is 100', function () {
      expect(c.get('hosts').everyProperty('progress', '100')).to.be.true;
    });

    it('each host status is `heartbeat_lost`', function () {
      expect(c.get('hosts').everyProperty('status', 'heartbeat_lost')).to.be.true;
    });

    it('overall progress is 100', function () {
      expect(c.get('progress')).to.be.equal('100');
    });

  });

  describe('#loadDoServiceChecksFlagSuccessCallback', function () {

    var data = {
      RootServiceComponents: {
        properties: {
          'skip.service.checks': 'true'
        }
      }
    };

    it('skipServiceChecks should be true', function () {
      c.loadDoServiceChecksFlagSuccessCallback(data);
      expect(c.get('skipServiceChecks')).to.be.true;
    });

    it('skipServiceChecks should be false', function () {
      data.RootServiceComponents.properties['skip.service.checks'] = 'false';
      c.loadDoServiceChecksFlagSuccessCallback(data);
      expect(c.get('skipServiceChecks')).to.be.false;
    });

  });

  describe('#isNextButtonDisabled', function () {

    var cases = [
      {
        nextBtnClickInProgress: true,
        isSubmitDisabled: true,
        isNextButtonDisabled: true,
        description: 'button clicked, submit disabled',
        title: 'next button disabled'
      },
      {
        nextBtnClickInProgress: true,
        isSubmitDisabled: false,
        isNextButtonDisabled: true,
        description: 'button clicked, submit not disabled',
        title: 'next button disabled'
      },
      {
        nextBtnClickInProgress: false,
        isSubmitDisabled: true,
        isNextButtonDisabled: true,
        description: 'no button clicked, submit disabled',
        title: 'next button disabled'
      },
      {
        nextBtnClickInProgress: false,
        isSubmitDisabled: false,
        isNextButtonDisabled: false,
        description: 'no button clicked, submit not disabled',
        title: 'next button enabled'
      }
    ];

    cases.forEach(function (item) {

      describe(item.description, function () {

        beforeEach(function () {
          c.reopen({
            isSubmitDisabled: item.isSubmitDisabled
          });
          sinon.stub(App, 'get').withArgs('router.nextBtnClickInProgress').returns(item.nextBtnClickInProgress);
          c.propertyDidChange('isSubmitDisabled');
          c.propertyDidChange('App.router.nextBtnClickInProgress');
        });

        afterEach(function () {
          App.get.restore();
        });

        it(item.title, function () {
          expect(c.get('isNextButtonDisabled')).to.equal(item.isNextButtonDisabled);
        });

      });

    });

  });

});
