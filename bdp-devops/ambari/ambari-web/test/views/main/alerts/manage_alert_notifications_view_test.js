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

var view;

describe('App.ManageAlertNotificationsView', function () {

  beforeEach(function () {

    view = App.ManageAlertNotificationsView.create({
      controller: Em.Object.create()
    });

  });

  describe('#showEmailDetails', function () {

    Em.A([
      {
        selectedAlertNotification: {type: 'SNMP'},
        e: false
      },
      {
        selectedAlertNotification: {type: 'EMAIL'},
        e: true
      }
    ]).forEach(function (test, i) {
      it('test ' + (i + 1), function () {
        view.set('controller.selectedAlertNotification', test.selectedAlertNotification);
        expect(view.get('showEmailDetails')).to.equal(test.e);
      });
    });

  });

  describe('#showSNMPDetails', function () {

    Em.A([
      {
        selectedAlertNotification: {type: 'SNMP'},
        e: true
      },
      {
        selectedAlertNotification: {type: 'EMAIL'},
        e: false
      }
    ]).forEach(function (test, i) {
      it('test ' + (i + 1), function () {
        view.set('controller.selectedAlertNotification', test.selectedAlertNotification);
        expect(view.get('showSNMPDetails')).to.equal(test.e);
      });
    });

  });

  describe("#selectedAlertNotificationGroups", function () {
    it("should contain group names", function () {
      view.set('controller', Em.Object.create({
        selectedAlertNotification: Em.Object.create({
          groups: [
            Em.Object.create({
              displayName: 'g1'
            }),
            Em.Object.create({
              displayName: 'g2'
            })
          ]
        })
      }));
      expect(view.get('selectedAlertNotificationGroups')).to.equal('g1, g2');
    });
  });

  describe("#email", function () {
    it("should return ambari.dispatch.recipients", function () {
      view.set('controller', Em.Object.create({
        selectedAlertNotification: Em.Object.create({
          properties: {
            'ambari.dispatch.recipients': 1
          }
        })
      }));
      expect(view.get('email')).to.equal(1);
    });
  });

  describe("#severities", function () {
    it("should return list of states", function () {
      view.set('controller', Em.Object.create({
        selectedAlertNotification: Em.Object.create({
          alertStates: ['st1', 'st2']
        })
      }));
      expect(view.get('severities')).to.equal('st1, st2');
    });
  });

  describe("#onAlertNotificationSelect()", function () {

    beforeEach(function () {
      sinon.stub(Em.run, 'later');
      view.removeObserver('selectedAlertNotification', view, 'onAlertNotificationSelect');
      view.set('controller', Em.Object.create({selectedAlertNotification: null}));
    });

    afterEach(function() {
      Em.run.later.restore();
    });

    it("selectedAlertNotification is null", function () {
      view.set('selectedAlertNotification', null);
      view.onAlertNotificationSelect();
      expect(view.get('selectedAlertNotification')).to.be.null;
      expect(view.get('controller.selectedAlertNotification')).to.be.null;
    });

    it("selectedAlertNotification is empty array", function () {
      view.set('selectedAlertNotification', []);
      view.onAlertNotificationSelect();
      expect(view.get('selectedAlertNotification')).to.be.empty;
      expect(view.get('controller.selectedAlertNotification')).to.be.null;
    });

    it("selectedAlertNotification is array with single element", function () {
      view.set('selectedAlertNotification', [1]);
      view.onAlertNotificationSelect();
      expect(view.get('selectedAlertNotification')).to.eql([1]);
      expect(view.get('controller.selectedAlertNotification')).to.equal(1);
    });

    it("selectedAlertNotification is array with two elements", function () {
      view.set('selectedAlertNotification', [1, 2]);
      view.onAlertNotificationSelect();
      expect(view.get('selectedAlertNotification')).to.equal(2);
      expect(view.get('controller.selectedAlertNotification')).to.equal(2);
    });
  });

  describe("#willInsertElement()", function () {

    beforeEach(function () {
      view.set('controller', Em.Object.create({loadAlertNotifications: Em.K}));
      sinon.spy(view.get('controller'), 'loadAlertNotifications');
    });
    afterEach(function () {
      view.get('controller').loadAlertNotifications.restore();
    });

    it("loadAlertNotifications should be called", function () {
      view.willInsertElement();
      expect(view.get('controller').loadAlertNotifications.calledOnce).to.be.true;
    });
  });

  describe("#didInsertElement()", function () {

    beforeEach(function () {
      sinon.stub(view, 'onLoad');
    });
    afterEach(function () {
      view.onLoad.restore();
    });

    it("loadAlertNotifications should be called", function () {
      view.didInsertElement();
      expect(view.onLoad.calledOnce).to.be.true;
    });
  });

  describe("#onLoad()", function () {

    beforeEach(function () {
      view.removeObserver('controller.isLoaded', view, 'onLoad');
      view.set('controller', Em.Object.create());
      sinon.stub(Em.run, 'later', function (context, callback) {
        callback();
      });
      sinon.stub(App, 'tooltip');
      this.clock = sinon.useFakeTimers();
    });
    afterEach(function () {
      Em.run.later.restore();
      App.tooltip.restore();
      this.clock.restore();
    });

    it("controller.isLoaded is false", function () {
      view.set('controller.isLoaded', false);
      view.onLoad();
      expect(Em.run.later.called).to.be.false;
    });

    describe("controller.isLoaded is true, alertNotifications is null", function () {

      beforeEach(function () {
        view.set('controller.isLoaded', true);
        view.set('controller.alertNotifications', null);
      });

      it("Em.run.later should be called", function () {
        view.onLoad();
        expect(Em.run.later.calledOnce).to.be.true;
      });

      it("App.tooltip should be called twice", function () {
        view.onLoad();
        this.clock.tick(50);
        expect(App.tooltip.calledOnce).to.be.true;
      });

      it("selectedAlertNotification should be null", function () {
        view.onLoad();
        expect(view.get('selectedAlertNotification')).to.be.null;
      });
    });

    describe("controller.isLoaded is true, alertNotifications is array", function () {

      beforeEach(function () {
        view.set('controller.isLoaded', true);
        view.set('controller.alertNotifications', [{}]);
      });

      it("Em.run.later should be called twice", function () {
        view.onLoad();
        expect(Em.run.later.calledTwice).to.be.true;
      });

      it("App.tooltip should be called twice", function () {
        view.onLoad();
        this.clock.tick(100);
        expect(App.tooltip.calledTwice).to.be.true;
      });

      it("selectedAlertNotification should be object", function () {
        view.onLoad();
        expect(view.get('selectedAlertNotification')).to.eql({});
      });

    });
  });
});
