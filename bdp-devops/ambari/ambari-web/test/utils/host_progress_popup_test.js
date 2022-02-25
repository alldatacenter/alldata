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

var Ember = require('ember');
var App = require('app');

require('controllers/global/background_operations_controller');
require('views/common/modal_popup');
require('utils/helper');
require('utils/host_progress_popup');

describe('App.HostPopup', function () {

  var testTasks = [
    {
      t: [
        {
          Tasks: {
            status: 'COMPLETED',
            id: 2
          }
        },
        {
          Tasks: {
            status: 'COMPLETED',
            id: 3
          }
        },
        {
          Tasks: {
            status: 'COMPLETED',
            id: 1
          }
        }
      ],
      m: 'All COMPLETED',
      r: 'SUCCESS',
      p: 100,
      ids: [1,2,3]
    },
    {
      t: [
        {
          Tasks: {
            status: 'FAILED',
            id: 2
          }
        },
        {
          Tasks: {
            status: 'COMPLETED',
            id: 1
          }
        }
        ,
        {
          Tasks: {
            status: 'COMPLETED',
            id: 3
          }
        }
      ],
      m: 'One FAILED',
      r: 'FAILED',
      p: 100,
      ids: [1,2,3]
    },
    {
      t: [
        {
          Tasks: {
            status: 'ABORTED',
            id: 1
          }
        },
        {
          Tasks: {
            status: 'COMPLETED',
            id: 2
          }
        }
      ],
      m: 'One ABORTED',
      r: 'ABORTED',
      p: 100,
      ids: [1,2]
    },
    {
      t: [
        {
          Tasks: {
            status: 'TIMEDOUT',
            id: 3
          }
        },
        {
          Tasks: {
            status: 'COMPLETED',
            id: 1
          }
        }
      ],
      m: 'One TIMEDOUT',
      r: 'TIMEDOUT',
      p: 100,
      ids: [1,3]
    },
    {
      t: [
        {
          Tasks: {
            status: 'IN_PROGRESS',
            id: 1
          }
        },
        {
          Tasks: {
            status: 'COMPLETED',
            id: 2
          }
        }
      ],
      m: 'One IN_PROGRESS',
      r: 'IN_PROGRESS',
      p: 68,
      ids: [1,2]
    },
    {
      t: [
        {
          Tasks: {
            status: 'QUEUED',
            id: 2
          }
        },
        {
          Tasks: {
            status: 'COMPLETED',
            id: 3
          }
        }
      ],
      m: 'Something else',
      r: 'PENDING',
      p: 55,
      ids: [2,3]
    }
  ];

  var statusCases = [
    {
      status: 'FAILED',
      result: false
    },
    {
      status: 'ABORTED',
      result: false
    },
    {
      status: 'TIMEDOUT',
      result: false
    },
    {
      status: 'IN_PROGRESS',
      result: true
    },
    {
      status: 'COMPLETED',
      result: false
    },
    {
      status: 'PENDING',
      result: true
    }
  ];

  describe('#setSelectCount', function () {
    var itemsForStatusTest = [
      {
        title: 'Empty',
        data: [],
        result: [0, 0, 0, 0, 0, 0, 0]
      },
      {
        title: 'All Pending',
        data: [
          {status: 'pending'},
          {status: 'queued'}
        ],
        result: [2, 2, 0, 0, 0, 0, 0]
      },
      {
        title: 'All Completed',
        data: [
          {status: 'success'},
          {status: 'completed'}
        ],
        result: [2, 0, 0, 0, 2, 0, 0]
      },
      {
        title: 'All Failed',
        data: [
          {status: 'failed'},
          {status: 'failed'}
        ],
        result: [2, 0, 0, 2, 0, 0, 0]
      },
      {
        title: 'All InProgress',
        data: [
          {status: 'in_progress'},
          {status: 'in_progress'}
        ],
        result: [2, 0, 2, 0, 0, 0, 0]
      },
      {
        title: 'All Aborted',
        data: [
          {status: 'aborted'},
          {status: 'aborted'}
        ],
        result: [2, 0, 0, 0, 0, 2, 0]
      },
      {
        title: 'All Timedout',
        data: [
          {status: 'timedout'},
          {status: 'timedout'}
        ],
        result: [2, 0, 0, 0, 0, 0, 2]
      },
      {
        title: 'Every Category',
        data: [
          {status: 'pending'},
          {status: 'queued'},
          {status: 'success'},
          {status: 'completed'},
          {status: 'failed'},
          {status: 'in_progress'},
          {status: 'aborted'},
          {status: 'timedout'}
        ],
        result: [8, 2, 1, 1, 2, 1, 1]
      }
    ];
    var categories = [
      Ember.Object.create({value: 'all'}),
      Ember.Object.create({value: 'pending'}),
      Ember.Object.create({value: 'in_progress'}),
      Ember.Object.create({value: 'failed'}),
      Ember.Object.create({value: 'completed'}),
      Ember.Object.create({value: 'aborted'}),
      Ember.Object.create({value: 'timedout'})
    ];
    itemsForStatusTest.forEach(function(statusTest) {
      it(statusTest.title, function() {
        App.HostPopup.setSelectCount(statusTest.data, categories);
        expect(categories.mapProperty('count')).to.deep.equal(statusTest.result);
      });
    });
  });

  describe('#getStatus', function() {
    testTasks.forEach(function(testTask) {
      it(testTask.m, function() {
        expect(App.HostPopup.getStatus(testTask.t)[0]).to.equal(testTask.r);
      });
    });
  });

  describe('#getProgress', function() {
    testTasks.forEach(function(testTask) {
      it(testTask.m, function() {
        expect(App.HostPopup.getProgress(testTask.t)).to.equal(testTask.p);
      });
    });
  });

  describe('#isAbortableByStatus', function () {
    statusCases.forEach(function (item) {
      it('should return ' + item.result + ' for ' + item.status, function () {
        expect(App.HostPopup.isAbortableByStatus(item.status)).to.equal(item.result);
      });
    });
  });

  describe('#abortRequest', function () {
    beforeEach(function () {
      sinon.spy(App, 'showConfirmationPopup');
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
    });
    it('should show confirmation popup', function () {
      App.HostPopup.abortRequest(Em.Object.create({
        name: 'name'
      }));
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
    });
  });

  describe('#abortRequestSuccessCallback', function () {
    it('should open popup', function () {
      App.HostPopup.abortRequestSuccessCallback(null, null, {
        requestName: 'name',
        serviceInfo: Em.Object.create()
      });
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });
  });

  describe('#abortRequestErrorCallback', function () {
    var popup = App.HostPopup;
    beforeEach(function () {
      sinon.stub(App.ajax, 'get', function(k) {
        if (k === 'modalPopup') return null;
        return Em.get(App, k);
      });
    });
    afterEach(function () {
      App.ajax.get.restore();
    });
    it('should open popup', function () {
      popup.abortRequestErrorCallback({
        responseText: {
          message: 'message'
        },
        status: 404
      }, 'status', 'error', {
        url: 'url'
      }, {
        requestId: 0,
        serviceInfo: Em.Object.create()
      });
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });
    statusCases.forEach(function (item) {
      it('should set serviceInfo.isAbortable to' + item.result + ' if status is ' + item.status, function () {
        popup.abortRequestErrorCallback({
          responseText: {
            message: 'message'
          },
          status: 404
        }, 'status', 'error', {
          url: 'url'
        }, {
          requestId: 0,
          serviceInfo: Em.Object.create({
            status: item.status
          })
        });
        expect(App.HostPopup.isAbortableByStatus(item.status)).to.equal(item.result);
      });
    });
  });

  describe('#setBackgroundOperationHeader', function(){
    beforeEach(function (){
      sinon.stub(App.HostPopup, "get").returns(true);
      sinon.spy(App.HostPopup, "set");
      this.stub = sinon.stub(App.router, "get");
    });

    afterEach(function (){
      App.HostPopup.get.restore();
      App.HostPopup.set.restore();
      App.router.get.restore();
    });

    it("should display '2 Background Operations Running' when there are 2 background operations running", function(){
      this.stub.returns(2);
      App.HostPopup.setBackgroundOperationHeader(false);

      expect(App.HostPopup.set.calledWith("popupHeaderName", "2 Background Operations Running")).to.be.true;
    });

    it("should display '1 Background Operation Running' when there is 1 background operation running", function(){
      this.stub.returns(1);
      App.HostPopup.setBackgroundOperationHeader(false);

      expect(App.HostPopup.set.calledWith("popupHeaderName", "1 Background Operation Running")).to.be.true;
    });
  });

  describe('#_getHostsMap', function () {

    Em.A([
      {
        inputData: [
          {name: 's1', hostsMap: {h1: {}, h2: {}}},
          {name: 's2'}
        ],
        isBackgroundOperations: true,
        currentServiceId: null,
        serviceName: 's1',
        m: '`currentServiceId` is null, `serviceName` exists, `isBackgroundOperations` true, `hostsMap` exists',
        e: {h1: {}, h2: {}}
      },
      {
        inputData: [
          {name: 's1', hosts: [
            {name: 'h1'},
            {name: 'h2'}
          ]},
          {name: 's2'}
        ],
        isBackgroundOperations: true,
        currentServiceId: null,
        serviceName: 's1',
        m: '`currentServiceId` is null, `serviceName` exists, `isBackgroundOperations` true, `hosts` exists',
        e: {h1: {name: 'h1'}, h2: {name: 'h2'}}
      },
      {
        inputData: [
          {id: 1, hostsMap: {h1: {}, h2: {}}},
          {id: 2}
        ],
        isBackgroundOperations: true,
        currentServiceId: 1,
        serviceName: 's1',
        m: '`currentServiceId` is 1, `serviceName` exists, `isBackgroundOperations` true, `hostsMap` exists',
        e: {h1: {}, h2: {}}
      },
      {
        inputData: [
          {id: 1, hosts: [
            {name: 'h1'},
            {name: 'h2'}
          ]},
          {id: 2}
        ],
        isBackgroundOperations: true,
        currentServiceId: 1,
        serviceName: 's1',
        m: '`currentServiceId` is 1, `serviceName` exists, `isBackgroundOperations` true, `hosts` exists',
        e: {h1: {name: 'h1'}, h2: {name: 'h2'}}
      },

      {
        inputData: [
          {name: 's1', hostsMap: {h1: {}, h2: {}}},
          {name: 's2'}
        ],
        isBackgroundOperations: false,
        currentServiceId: null,
        serviceName: 's1',
        m: '`currentServiceId` is null, `serviceName` exists, `isBackgroundOperations` false, `hostsMap` exists',
        e: {h1: {}, h2: {}}
      },
      {
        inputData: [
          {name: 's1', hosts: [
            {name: 'h1'},
            {name: 'h2'}
          ]},
          {name: 's2'}
        ],
        isBackgroundOperations: false,
        currentServiceId: null,
        serviceName: 's1',
        m: '`currentServiceId` is null, `serviceName` exists, `isBackgroundOperations` false, `hosts` exists',
        e: {h1: {name: 'h1'}, h2: {name: 'h2'}}
      },
      {
        inputData: [
          {name: 's1', hostsMap: {h1: {}, h2: {}}}
        ],
        isBackgroundOperations: false,
        currentServiceId: 1,
        serviceName: 's1',
        m: '`currentServiceId` is 1, `serviceName` exists, `isBackgroundOperations` false, `hostsMap` exists',
        e: {h1: {}, h2: {}}
      },
      {
        inputData: [
          {name: 's1', hostsMap: {h1: {}, h2: {}}}
        ],
        isBackgroundOperations: false,
        currentServiceId: 1,
        serviceName: 's1',
        m: '`currentServiceId` is 1, `serviceName` exists, `isBackgroundOperations` false, `hosts` exists',
        e: {h1: {}, h2: {}}
      }
    ]).forEach(function (test) {

      it(test.m, function () {
        App.HostPopup.setProperties(test);
        expect(App.HostPopup._getHostsMap()).to.eql(test.e);
      });

    });

  });

  describe("#initPopup", function() {
    it("should reset the breadcrumbs", function() {
      App.HostPopup.initPopup("rootBreadcrumb", Em.Object.create({ services: [] }));

      expect(App.HostPopup.get("breadcrumbs")).to.deep.equal([{ label: "rootBreadcrumb" }]);
    });
  });
});
