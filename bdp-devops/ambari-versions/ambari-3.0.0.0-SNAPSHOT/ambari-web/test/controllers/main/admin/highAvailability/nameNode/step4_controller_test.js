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
var testHelpers = require('test/helpers');


describe('App.HighAvailabilityWizardStep4Controller', function() {
  var controller;

  beforeEach(function() {
    controller = App.HighAvailabilityWizardStep4Controller.create({
      content: Em.Object.create()
    });
  });
  
  describe('#checkNnCheckPointStatus', function() {
    beforeEach(function() {
      this.clock = sinon.useFakeTimers();
      sinon.stub(controller, 'pullCheckPointStatus');
    });

    afterEach(function() {
      this.clock.restore();
      controller.pullCheckPointStatus.restore();
    });

    var tests = [
      {
        responseData: {
          HostRoles: { desired_state: 'STARTED' }
        },
        m: 'NameNode started, Safemode off, no journal node transaction. Polling should be performed and isNameNodeStarted should be true',
        e: {
          isPollingCalled: true,
          isNameNodeStarted: true,
          isNextEnabled: false
        }
      },
      {
        responseData: {
          HostRoles: { desired_state: 'STARTED' },
          metrics: { dfs: { namenode: {
            Safemode: 'ON',
            JournalTransactionInfo: "{\"LastAppliedOrWrittenTxId\":\"4\",\"MostRecentCheckpointTxId\":\"2\"}"
          }}}
        },
        m: 'NameNode started, Safemode on, journal node transaction invalid. Polling should be performed and isNameNodeStarted should be true',
        e: {
          isPollingCalled: true,
          isNameNodeStarted: true,
          isNextEnabled: false
        }
      },
      {
        responseData: {
          HostRoles: { desired_state: 'INSTALLED' },
          metrics: { dfs: { namenode: {
            Safemode: 'ON',
            JournalTransactionInfo: "{\"LastAppliedOrWrittenTxId\":\"15\",\"MostRecentCheckpointTxId\":\"14\"}"
          }}}
        },
        m: 'NameNode not started, Safemode on, journal node transaction present. Polling should not be performed and isNameNodeStarted should be false',
        e: {
          isPollingCalled: false,
          isNameNodeStarted: false,
          isNextEnabled: true
        }
      },
      {
        responseData: {
          HostRoles: { desired_state: 'STARTED' },
          metrics: { dfs: { namenode: {
            Safemode: "",
            JournalTransactionInfo: "{\"LastAppliedOrWrittenTxId\":\"15\",\"MostRecentCheckpointTxId\":\"14\"}"
          }}}
        },
        m: 'NameNode started, Safemode off, journal node transaction present. Polling should not be performed and isNameNodeStarted should be true',
        e: {
          isPollingCalled: true,
          isNameNodeStarted: true,
          isNextEnabled: false
        }
      }
    ];

    tests.forEach(function(test) {
      describe(test.m, function() {

        beforeEach(function () {
          controller.set('isNameNodeStarted', !test.e.isNameNodeStarted);
          controller.checkNnCheckPointStatus(test.responseData);
          this.clock.tick(controller.get('POLL_INTERVAL'));
        });
        it('isNameNodeStarted is ' + test.e.isNameNodeStarted, function () {
          expect(controller.get('isNameNodeStarted')).to.be.equal(test.e.isNameNodeStarted);
        });
        it('isNextEnabled is ' + test.e.isNextEnabled, function () {
          expect(controller.get('isNextEnabled')).to.be.equal(test.e.isNextEnabled);
        });
        it('pullCheckPointStatus is ' + (test.e.isPollingCalled ? '' : 'not') + ' called', function () {
          expect(controller.pullCheckPointStatus.called).to.be.equal(test.e.isPollingCalled);
        });
      });
    });
  });

  describe('#pullCheckPointStatus', function() {

    it('App.ajax.send should be called', function() {
      controller.set('content.masterComponentHosts', [
        {
          component: 'NAMENODE',
          isInstalled: true,
          hostName: 'host1'
        }
      ]);
      controller.pullCheckPointStatus();
      var args = testHelpers.findAjaxRequest('name', 'admin.high_availability.getNnCheckPointStatus');
      expect(args[0]).to.be.eql({
        name: 'admin.high_availability.getNnCheckPointStatus',
        sender: controller,
        data: {
          hostName: 'host1'
        },
        success: 'checkNnCheckPointStatus'
      });
    });
  });

  describe('#done', function() {
    var mock = {
      getKDCSessionState: Em.clb
    };

    beforeEach(function() {
      sinon.spy(mock, 'getKDCSessionState');
      sinon.stub(App, 'get').returns(mock);
      sinon.stub(App.router, 'send');
    });
    afterEach(function() {
      App.get.restore();
      App.router.send.restore();
      mock.getKDCSessionState.restore();
    });

    it('App.router.send should not be called', function() {
      controller.set('isNextEnabled', false);
      controller.done();
      expect(mock.getKDCSessionState.called).to.be.false;
      expect(App.router.send.called).to.be.false;
    });

    it('App.router.send should be called', function() {
      controller.set('isNextEnabled', true);
      controller.done();
      expect(mock.getKDCSessionState.calledOnce).to.be.true;
      expect(App.router.send.calledOnce).to.be.true;
    });
  });
});

