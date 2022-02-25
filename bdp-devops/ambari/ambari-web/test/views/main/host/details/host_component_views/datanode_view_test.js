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
require('views/main/host/details/host_component_views/datanode_view');
var testHelpers = require('test/helpers');

describe('App.DataNodeComponentView', function () {
  var view = App.DataNodeComponentView.create({
    content: {
      hostName: 'host1'
    }
  });

  describe("#getDNDecommissionStatus()", function () {

    beforeEach(function () {
      this.stub = sinon.stub(App.HDFSService, 'find');
    });

    afterEach(function () {
      this.stub.restore();
    });

    it("snameNode absent and no activeNameNode", function () {
      this.stub.returns([
        Em.Object.create({
          snameNode: null,
          activeNameNodes: [],
          nameNode: {hostName: 'host1'}
        })
      ]);
      view.getDNDecommissionStatus();
      var args = testHelpers.findAjaxRequest('name', 'host.host_component.decommission_status_datanode');
      expect(args[0]).exists;
      expect(args[0].data).to.be.eql({
        "hostNames": "host1",
        "componentName": "NAMENODE"
      });
    });

    it("snameNode present and no activeNameNode", function () {
      this.stub.returns([
        Em.Object.create({
          snameNode: {},
          activeNameNodes: [],
          nameNode: {hostName: 'host1'}
        })
      ]);
      view.getDNDecommissionStatus();
      var args = testHelpers.findAjaxRequest('name', 'host.host_component.decommission_status_datanode');
      expect(args[0]).exists;
      expect(args[0].data).to.be.eql({
        "hostNames": "host1",
        "componentName": "NAMENODE"
      });
    });

    it("snameNode absent and activeNameNode valid", function () {
      this.stub.returns([
        Em.Object.create({
          snameNode: null,
          activeNameNodes: [{hostName: 'host2'}],
          nameNode: {hostName: 'host1'}
        })
      ]);
      view.getDNDecommissionStatus();
      var args = testHelpers.findAjaxRequest('name', 'host.host_component.decommission_status_datanode');
      expect(args[0]).exists;
      expect(args[0].data).to.be.eql({
        "hostNames": "host2",
        "componentName": "NAMENODE"
      });
    });

  });

  describe("#getDNDecommissionStatusSuccessCallback()", function () {
    beforeEach(function () {
      sinon.stub(view, 'computeStatus', Em.K);
      view.set('decommissionedStatusObject', null);
    });
    afterEach(function () {
      view.computeStatus.restore();
    });
    it("metric null", function () {
      var data = {
        items: null
      };
      view.getDNDecommissionStatusSuccessCallback(data);
      expect(view.computeStatus.calledOnce).to.be.false;
    });
    it("metric valid", function () {
      var data = {
        items: [
          {
            metrics: {
              dfs: {
                namenode: "status"
              }
            }
          }
        ]
      };
      view.getDNDecommissionStatusSuccessCallback(data);
      expect(view.computeStatus.calledOnce).to.be.true;
    });
  });

  describe("#getDNDecommissionStatusErrorCallback()", function () {
    it("reset to null", function () {
      expect(view.getDNDecommissionStatusErrorCallback()).to.be.null;
      expect(view.get('decommissionedStatusObject')).to.be.null;
    });
  });

  describe("#loadComponentDecommissionStatus()", function () {
    before(function () {
      sinon.stub(view, 'getDNDecommissionStatus', Em.K);
    });
    after(function () {
      view.getDNDecommissionStatus.restore();
    });
    it("call getDNDecommissionStatus()", function () {
      view.loadComponentDecommissionStatus();
      expect(view.getDNDecommissionStatus.calledOnce).to.be.true;
    });
  });

  describe("#setDesiredAdminState()", function () {
    before(function () {
      sinon.stub(view, 'setStatusAs', Em.K);
    });
    after(function () {
      view.setStatusAs.restore();
    });
    it("call getDNDecommissionStatus()", function () {
      view.setDesiredAdminState('status');
      expect(view.setStatusAs.calledWith('status')).to.be.true;
    });
  });

  describe("#computeStatus()", function () {
    beforeEach(function () {
      sinon.stub(view, 'getDesiredAdminState', Em.K);
      sinon.stub(view, 'setStatusAs', Em.K);
      this.stub = sinon.stub(App, 'get');
    });
    afterEach(function () {
      view.getDesiredAdminState.restore();
      view.setStatusAs.restore();
      this.stub.restore();
    });
    var testCases = [
      {
        title: 'No live nodes',
        data: [],
        result: {
          getDesiredAdminStateCalled: true,
          status: "",
          setStatusAsCalled: false
        }
      },
      {
        title: 'Live nodes In Service',
        data: [
          {
            "LiveNodes": {
              "host1": {
                "adminState": "In Service"
              }
            }
          },
          {
            "LiveNodes": {
              "host1": {
                "adminState": "In Service"
              }
            }
          }
        ],
        result: {
          getDesiredAdminStateCalled: false,
          status: "INSERVICE",
          setStatusAsCalled: true
        }
      },
      {
        title: 'Live nodes In Progress',
        data: [
          {
            "LiveNodes": {
              "host1": {
                "adminState": "Decommission In Progress"
              }
            }
          },
          {
            "LiveNodes": {
              "host1": {
                "adminState": "Decommission In Progress"
              }
            }
          }
        ],
        result: {
          getDesiredAdminStateCalled: false,
          status: "DECOMMISSIONING",
          setStatusAsCalled: true
        }
      },
      {
        title: 'Live nodes In Progress with some In Service',
        data: [
          {
            "LiveNodes": {
              "host1": {
                "adminState": "Decommission In Progress"
              }
            }
          },
          {
            "LiveNodes": {
              "host1": {
                "adminState": "Decommission In Progress"
              }
            }
          },
          {
            "LiveNodes": {
              "host1": {
                "adminState": "In Service"
              }
            }
          }
        ],
        result: {
          getDesiredAdminStateCalled: false,
          status: "DECOMMISSIONING",
          setStatusAsCalled: true
        }
      },
      {
        title: 'Live nodes In Progress with some Decommissioned',
        data: [
          {
            "LiveNodes": {
              "host1": {
                "adminState": "Decommission In Progress"
              }
            }
          },
          {
            "LiveNodes": {
              "host1": {
                "adminState": "Decommission In Progress"
              }
            }
          },
          {
            "LiveNodes": {
              "host1": {
                "adminState": "Decommissioned"
              }
            }
          }
        ],
        result: {
          getDesiredAdminStateCalled: false,
          status: "DECOMMISSIONING",
          setStatusAsCalled: true
        }
      },
      {
        title: 'Live nodes Decommissioned',
        data: [
          {
            "LiveNodes": {
              "host1": {
                "adminState": "Decommissioned"
              }
            }
          },
          {
            "LiveNodes": {
              "host1": {
                "adminState": "Decommissioned"
              }
            }
          }
        ],
        result: {
          getDesiredAdminStateCalled: false,
          status: "DECOMMISSIONED",
          setStatusAsCalled: true
        }
      }
    ];
    testCases.forEach(function (test) {
      it(test.title, function () {
        view.computeStatus(test.data);
        expect(view.getDesiredAdminState.called).to.equal(test.result.getDesiredAdminStateCalled);
        expect(view.setStatusAs.calledWith(test.result.status)).to.equal(test.result.setStatusAsCalled);
      });
    }, this);
    it("empty data", function () {
      view.computeStatus([]);
      expect(view.getDesiredAdminState.called).to.be.true;
      expect(view.setStatusAs.called).to.be.false;
    });
  });

});
