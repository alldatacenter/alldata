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

describe('BulkOperationsController', function () {

  var hostController;

  describe('#bulkOperation', function() {

    beforeEach(function() {
      hostController = App.BulkOperationsController.create({});
      sinon.stub(hostController, 'bulkOperationForHostsRestart', Em.K);
      sinon.stub(hostController, 'bulkOperationForHosts', Em.K);
      sinon.stub(hostController, 'bulkOperationForHostsReinstall', Em.K);
      sinon.stub(hostController, 'bulkOperationForHostComponentsRestart', Em.K);
      sinon.stub(hostController, 'bulkOperationForHostComponentsDecommission', Em.K);
      sinon.stub(hostController, 'bulkOperationForHostComponents', Em.K);
      sinon.stub(hostController, 'bulkOperationForHostComponentsAdd', Em.K);
      sinon.stub(hostController, 'bulkOperationForHostsPassiveState', Em.K);
    });

    afterEach(function() {
      hostController.bulkOperationForHosts.restore();
      hostController.bulkOperationForHostsRestart.restore();
      hostController.bulkOperationForHostsReinstall.restore();
      hostController.bulkOperationForHostComponentsRestart.restore();
      hostController.bulkOperationForHostComponentsDecommission.restore();
      hostController.bulkOperationForHostComponents.restore();
      hostController.bulkOperationForHostComponentsAdd.restore();
      hostController.bulkOperationForHostsPassiveState.restore();

    });

    it('RESTART for hosts', function() {
      var operationData = {
        action: 'RESTART'
      };
      hostController.bulkOperation(operationData, []);
      expect(hostController.bulkOperationForHostsRestart.calledOnce).to.equal(true);
    });

    it('START for hosts', function() {
      var operationData = {
        action: 'STARTED'
      };
      hostController.bulkOperation(operationData, []);
      expect(hostController.bulkOperationForHosts.calledOnce).to.equal(true);
    });

    it('STOP for hosts', function() {
      var operationData = {
        action: 'INSTALLED'
      };
      hostController.bulkOperation(operationData, []);
      expect(hostController.bulkOperationForHosts.calledOnce).to.equal(true);
    });

    it('REINSTALL for hosts', function() {
      var operationData = {
        action: 'REINSTALL'
      };
      hostController.bulkOperation(operationData, []);
      expect(hostController.bulkOperationForHostsReinstall.calledOnce).to.equal(true);
    });

    it('PASSIVE_STATE for hosts', function() {
      var operationData = {
        action: 'PASSIVE_STATE'
      };
      hostController.bulkOperation(operationData, []);
      expect(hostController.bulkOperationForHostsPassiveState.calledOnce).to.equal(true);
    });

    it('RESTART for hostComponents', function() {
      var operationData = {
        action: 'RESTART',
        componentNameFormatted: 'DataNodes'
      };
      hostController.bulkOperation(operationData, []);
      expect(hostController.bulkOperationForHostComponentsRestart.calledOnce).to.equal(true);
    });

    it('START for hostComponents', function() {
      var operationData = {
        action: 'STARTED',
        componentNameFormatted: 'DataNodes'
      };
      hostController.bulkOperation(operationData, []);
      expect(hostController.bulkOperationForHostComponents.calledOnce).to.equal(true);
    });

    it('STOP for hostComponents', function() {
      var operationData = {
        action: 'INSTALLED',
        componentNameFormatted: 'DataNodes'
      };
      hostController.bulkOperation(operationData, []);
      expect(hostController.bulkOperationForHostComponents.calledOnce).to.equal(true);
    });

    it('ADD for hostComponents', function() {
      var operationData = {
        action: 'ADD',
        componentNameFormatted: 'DataNodes'
      };
      hostController.bulkOperation(operationData, []);
      expect(hostController.bulkOperationForHostComponentsAdd.calledOnce).to.equal(true);
    });

    it('DECOMMISSION for hostComponents', function() {
      var operationData = {
        action: 'DECOMMISSION',
        componentNameFormatted: 'DataNodes'
      };
      hostController.bulkOperation(operationData, []);
      expect(hostController.bulkOperationForHostComponentsDecommission.calledOnce).to.equal(true);
    });

    it('RECOMMISSION for hostComponents', function() {
      var operationData = {
        action: 'DECOMMISSION_OFF',
        componentNameFormatted: 'DataNodes'
      };
      hostController.bulkOperation(operationData, []);
      expect(hostController.bulkOperationForHostComponentsDecommission.calledOnce).to.equal(true);
    });

  });

  describe('#warnBeforeDecommissionSuccess()', function () {
    var mock = {
      showHbaseActiveWarning: Em.K,
      checkRegionServerState: Em.K
    };
    beforeEach(function () {
      hostController = App.BulkOperationsController.create({});
      sinon.stub(App.router, 'get', function () {
        return mock;
      });
      sinon.spy(mock, 'showHbaseActiveWarning');
      sinon.spy(mock, 'checkRegionServerState');
    });
    afterEach(function () {
      App.router.get.restore();
      mock.showHbaseActiveWarning.restore();
      mock.checkRegionServerState.restore();
    });

    it('items length more than 0', function () {
      hostController.warnBeforeDecommissionSuccess({items: [1]}, {}, {});
      expect(mock.showHbaseActiveWarning.calledOnce).to.be.true;
    });
    it('items length equal 0', function () {
      hostController.warnBeforeDecommissionSuccess({items: []}, {}, {hostNames: 'host1'});
      expect(mock.checkRegionServerState.calledWith('host1')).to.be.true;
    });
  });

});
