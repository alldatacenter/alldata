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

require('mappers/socket/upgrade_state_mapper');

describe('App.upgradeStateMapper', function () {

  describe('#map', function() {
    var mainAdminStackAndUpgradeController = Em.Object.create({
      restoreLastUpgrade: sinon.spy(),
      setDBProperty: sinon.spy(),
      upgradeId: 1
    });
    beforeEach(function() {
      sinon.stub(App.router, 'get').returns(mainAdminStackAndUpgradeController)
    });
    afterEach(function() {
      App.router.get.restore();
    });

    it('should call restoreLastUpgrade on CREATE event', function() {
      var event = {
        type: 'CREATE',
        associated_version: "2.5.4.0-121",
        cluster_id: 2,
        direction: "DOWNGRADE",
        downgrade_allowed: false,
        end_time: -1,
        progress_percent: 0,
        request_id: 26,
        request_status: "PENDING",
        revert_allowed: false,
        skip_failures: false,
        skip_service_check_failures: false,
        start_time: -1,
        suspended: false,
        upgrade_id: 56,
        upgrade_type: "NON_ROLLING"
      };
      App.upgradeStateMapper.map(event);
      expect(mainAdminStackAndUpgradeController.restoreLastUpgrade.getCall(0).args[0]).to.be.eql({
        Upgrade: event
      });
    });

    it('should set upgrade state on UPDATE event', function() {
      var event = {
        end_time: -1,
        progress_percent: 0,
        request_id: 1,
        request_status: "PENDING",
        start_time: -1,
        suspended: true,
        type: "UPDATE"
      };
      App.upgradeStateMapper.map(event);
      expect(App.get('upgradeState')).to.be.equal('PENDING');
      expect(mainAdminStackAndUpgradeController.get('isSuspended')).to.be.true;
      expect(mainAdminStackAndUpgradeController.setDBProperty.calledWith('upgradeState', 'PENDING')).to.be.true;
      expect(mainAdminStackAndUpgradeController.setDBProperty.calledWith('isSuspended', true)).to.be.true;
    });
  });
});
