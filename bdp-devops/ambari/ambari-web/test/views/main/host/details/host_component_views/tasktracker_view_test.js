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
require('views/main/host/details/host_component_views/tasktracker_view');

describe('App.TaskTrackerComponentView', function () {
  var view = App.TaskTrackerComponentView.create({
    content: {
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
    it("INSERVICE state)", function () {
      view.setDesiredAdminState('INSERVICE');
      expect(view.setStatusAs.calledWith('INSERVICE')).to.be.true;
    });
    it("DECOMMISSIONED state)", function () {
      view.setDesiredAdminState('DECOMMISSIONED');
      expect(view.getDecommissionStatus.calledOnce).to.be.true;
    });
  });

  describe("#setDecommissionStatus()", function() {
    beforeEach(function () {
      sinon.stub(view, 'setStatusAs', Em.K);
    });
    afterEach(function () {
      view.setStatusAs.restore();
    });
    it("data is null", function() {
      view.setDecommissionStatus(null);
      expect(view.setStatusAs.called).to.be.false;
    });
    it("AliveNodes is null", function() {
      view.setDecommissionStatus({"AliveNodes": null});
      expect(view.setStatusAs.called).to.be.false;
    });
    it("AliveNodes contain current host", function() {
      view.setDecommissionStatus({"AliveNodes": [
        {
          "hostname": "host1"
        }
      ]});
      expect(view.setStatusAs.calledWith("DECOMMISSIONING")).to.be.true;
    });
    it("AliveNodes does not contain current host", function() {
      view.setDecommissionStatus({"AliveNodes": []});
      expect(view.setStatusAs.calledWith('DECOMMISSIONED')).to.be.true;
    });
  });
});
