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


describe('App.HighAvailabilityWizardStep6Controller', function() {
  var controller;

  beforeEach(function() {
    controller = App.HighAvailabilityWizardStep6Controller.create({
      content: Em.Object.create()
    });
  });

  describe('#loadStep', function() {
    beforeEach(function() {
      sinon.stub(controller, 'pullCheckPointStatus');
      controller.loadStep();
    });
    afterEach(function() {
      controller.pullCheckPointStatus.restore();
    });

    it('status should be waiting', function() {
      expect(controller.get('status')).to.be.equal('waiting');
    });

    it('isNextEnabled should be false', function() {
      expect(controller.get('isNextEnabled')).to.be.false;
    });

    it('pullCheckPointStatus should be called', function() {
      expect(controller.pullCheckPointStatus.calledOnce).to.be.true;
    });
  });

  describe('#pullCheckPointStatus', function() {
    beforeEach(function() {
      sinon.stub(controller, 'pullEachJnStatus');
      controller.set('content.masterComponentHosts', [
        {
          component: 'JOURNALNODE',
          hostName: 'host1'
        },
        {
          component: 'JOURNALNODE',
          hostName: 'host2'
        }
      ]);
      controller.pullCheckPointStatus();
    });
    afterEach(function() {
      controller.pullEachJnStatus.restore();
    });

    it('initJnCounter should be 0', function() {
      expect(controller.get('initJnCounter')).to.be.equal(0);
    });

    it('requestsCounter should be 0', function() {
      expect(controller.get('requestsCounter')).to.be.equal(0);
    });

    it('hasStoppedJNs should be 0', function() {
      expect(controller.get('hasStoppedJNs')).to.be.false;
    });

    it('pullEachJnStatus should be called', function() {
      expect(controller.pullEachJnStatus.calledTwice).to.be.true;
    });
  });

  describe('#pullEachJnStatus', function() {

    it('App.ajax.send should be called', function() {
      controller.pullEachJnStatus('host1');
      var args = testHelpers.findAjaxRequest('name', 'admin.high_availability.getJnCheckPointStatus');
      expect(args[0]).to.be.eql({
        name: 'admin.high_availability.getJnCheckPointStatus',
        sender: controller,
        data: {
          hostName: 'host1'
        },
        success: 'checkJnCheckPointStatus'
      });
    });
  });

  describe('#checkJnCheckPointStatus', function() {
    beforeEach(function() {
      sinon.stub(controller, 'resolveJnCheckPointStatus');
    });
    afterEach(function() {
      controller.resolveJnCheckPointStatus.restore();
    });

    it('should have stopped JournalNodes when metrics absent', function() {
      controller.set('requestsCounter', 0);
      controller.checkJnCheckPointStatus({});
      expect(controller.get('hasStoppedJNs')).to.be.true;
      expect(controller.resolveJnCheckPointStatus.called).to.be.false;
    });

    it('should have init JournalNodes when metrics present', function() {
      controller.set('requestsCounter', controller.MINIMAL_JOURNALNODE_COUNT - 1);
      controller.set('content.nameServiceId', 'foo');
      controller.checkJnCheckPointStatus({
        metrics: {
          dfs: {
            journalnode: {
              journalsStatus: JSON.stringify({foo: {
                Formatted: 'true'
              }})
            }
          }
        }
      });
      expect(controller.get('hasStoppedJNs')).to.be.false;
      expect(controller.get('initJnCounter')).to.be.equal(1);
      expect(controller.resolveJnCheckPointStatus.calledWith(1)).to.be.true;
    });
  });

  describe('#resolveJnCheckPointStatus', function() {
    beforeEach(function() {
      sinon.stub(controller, 'pullCheckPointStatus');
      this.clock = sinon.useFakeTimers();
    });
    afterEach(function() {
      controller.pullCheckPointStatus.restore();
      this.clock.restore();
    });

    it('status should be done when all JournalNodes initialized', function() {
      controller.resolveJnCheckPointStatus(controller.MINIMAL_JOURNALNODE_COUNT);
      expect(controller.get('status')).to.be.equal('done');
      expect(controller.get('isNextEnabled')).to.be.true;
    });

    it('status should be waiting when all JournalNodes waiting', function() {
      controller.resolveJnCheckPointStatus(1);
      expect(controller.get('status')).to.be.equal('waiting');
      expect(controller.get('isNextEnabled')).to.be.false;
      this.clock.tick(controller.POLL_INTERVAL);
      expect(controller.pullCheckPointStatus.calledOnce).to.be.true;
    });

    it('status should be journalnode_stopped when all JournalNodes stopped', function() {
      controller.set('hasStoppedJNs', true);
      controller.resolveJnCheckPointStatus(1);
      expect(controller.get('status')).to.be.equal('journalnode_stopped');
      expect(controller.get('isNextEnabled')).to.be.false;
      this.clock.tick(controller.POLL_INTERVAL);
      expect(controller.pullCheckPointStatus.calledOnce).to.be.true;
    });
  });

  describe('#done', function() {
    beforeEach(function() {
      sinon.stub(App.router, 'send');
    });
    afterEach(function() {
      App.router.send.restore();
    });

    it('App.router.send should be called', function() {
      controller.set('isNextEnabled', true);
      controller.done();
      expect(App.router.send.calledWith('next')).to.be.true;
    });

    it('App.router.send should not be called', function() {
      controller.set('isNextEnabled', false);
      controller.done();
      expect(App.router.send.called).to.be.false;
    });
  });
});
