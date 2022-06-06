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
require('views/main/host/details/host_component_views/nodemanager_view');

describe('App.NodeManagerComponentView', function () {
  var view = App.NodeManagerComponentView.create({
    content: {
      service: {
        hostComponents: [
          Em.Object.create({
            componentName: 'RESOURCEMANAGER'
          })
        ]
      },
      hostName: 'host1'
    }
  });

  describe("#setDesiredAdminState()", function () {
    beforeEach(function () {
      sinon.stub(view, 'setStatusAs', Em.K);
      sinon.stub(view, 'getDecommissionStatus', Em.K);
    });
    afterEach(function () {
      view.setStatusAs.restore();
      view.getDecommissionStatus.restore();
    });
    it("state INSERVICE", function () {
      view.setDesiredAdminState('INSERVICE');
      expect(view.setStatusAs.calledWith('INSERVICE')).to.be.true;
    });
    it("state DECOMMISSIONED", function () {
      view.setDesiredAdminState('DECOMMISSIONED');
      expect(view.getDecommissionStatus.calledOnce).to.be.true;
    });
  });

  describe("#setDecommissionStatus()", function () {
    beforeEach(function () {
      sinon.stub(view, 'setStatusAs', Em.K);
    });
    afterEach(function () {
      view.setStatusAs.restore();
    });
    it("rm_metrics null", function () {
      view.setDecommissionStatus({rm_metrics: null});
      expect(view.setStatusAs.calledWith('DECOMMISSIONED')).to.be.true;
    });
    it("RM contains host", function () {
      var curObj = {rm_metrics: {
        cluster: {
          nodeManagers: [{
            HostName: 'host1'
          }]
        }
      }};
      view.setDecommissionStatus(curObj);
      expect(view.setStatusAs.calledWith('DECOMMISSIONING')).to.be.true;
    });
    it("RM does not contain host", function () {
      var curObj = {rm_metrics: {
        cluster: {
          nodeManagers: []
        }
      }};
      view.setDecommissionStatus(curObj);
      expect(view.setStatusAs.calledWith('DECOMMISSIONED')).to.be.true;
    });
  });
});
