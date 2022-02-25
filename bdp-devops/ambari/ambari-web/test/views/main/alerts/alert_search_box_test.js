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

describe('App.MainAlertDefinitionDetailsView', function () {

  beforeEach(function () {
    view = App.MainAlertDefinitionSearchBoxView.create({
      controller: Em.Object.create()
    });
    sinon.stub(view, 'rejectUsedValues').returns([]);
  });
  afterEach(function() {
    view.rejectUsedValues.restore();
  });

  describe('#valueMatches', function() {
    beforeEach(function() {
      sinon.stub(view, 'showHideClearButton');
      sinon.stub(view, 'getSummaryAvailableValues');
      sinon.stub(view, 'getLabelAvailableValues');
      sinon.stub(view, 'getServiceAvailableValues');
      sinon.stub(view, 'getTriggeredAvailableValues');
      sinon.stub(view, 'getEnabledAvailableValues');
      sinon.stub(view, 'getGroupsAvailableValues');
    });
    afterEach(function() {
      view.showHideClearButton.restore();
      view.getSummaryAvailableValues.restore();
      view.getLabelAvailableValues.restore();
      view.getServiceAvailableValues.restore();
      view.getTriggeredAvailableValues.restore();
      view.getEnabledAvailableValues.restore();
      view.getGroupsAvailableValues.restore();
    });

    it('showHideClearButton should be called', function() {
      view.valueMatches('Status');
      expect(view.showHideClearButton.calledOnce).to.be.true;
    });

    it('getSummaryAvailableValues should be called', function() {
      view.valueMatches('Status', '', Em.K);
      expect(view.getSummaryAvailableValues.calledWith('Status')).to.be.true;
    });

    it('getLabelAvailableValues should be called', function() {
      view.valueMatches('Alert Definition Name', '', Em.K);
      expect(view.getLabelAvailableValues.calledWith('Alert Definition Name')).to.be.true;
    });

    it('getServiceAvailableValues should be called', function() {
      view.valueMatches('Service', '', Em.K);
      expect(view.getServiceAvailableValues.calledWith('Service')).to.be.true;
    });

    it('getTriggeredAvailableValues should be called', function() {
      view.valueMatches('Last Status Changed', '', Em.K);
      expect(view.getTriggeredAvailableValues.calledWith('Last Status Changed')).to.be.true;
    });

    it('getEnabledAvailableValues should be called', function() {
      view.valueMatches('State', '', Em.K);
      expect(view.getEnabledAvailableValues.calledWith('State')).to.be.true;
    });

    it('getGroupsAvailableValues should be called', function() {
      view.valueMatches('Group', '', Em.K);
      expect(view.getGroupsAvailableValues.calledWith('Group')).to.be.true;
    });
  });

  describe('#getSummaryAvailableValues', function() {
    it('callback should be called', function() {
      var callback = sinon.spy();
      view.getSummaryAvailableValues('', callback);
      expect(callback.calledWith([], {preserveOrder: true})).to.be.true;
    });
  });

  describe('#getLabelAvailableValues', function() {
    it('callback should be called', function() {
      var callback = sinon.spy();
      view.getLabelAvailableValues('', callback);
      expect(callback.calledWith([])).to.be.true;
    });
  });

  describe('#getServiceAvailableValues', function() {
    it('callback should be called', function() {
      var callback = sinon.spy();
      view.getServiceAvailableValues('', callback);
      expect(callback.calledWith([])).to.be.true;
    });
  });

  describe('#getTriggeredAvailableValues', function() {
    it('callback should be called', function() {
      var callback = sinon.spy();
      view.getTriggeredAvailableValues('', callback);
      expect(callback.calledWith([], {preserveOrder: true})).to.be.true;
    });
  });

  describe('#getEnabledAvailableValues', function() {
    it('callback should be called', function() {
      var callback = sinon.spy();
      view.getEnabledAvailableValues('', callback);
      expect(callback.calledWith([], {preserveOrder: true})).to.be.true;
    });
  });

  describe('#getGroupsAvailableValues', function() {
    beforeEach(function() {
      sinon.stub(App.AlertGroup, 'find').returns([
        Em.Object.create({
          id: 1,
          displayName: 'g1'
        })
      ]);
    });
    afterEach(function() {
      App.AlertGroup.find.restore();
    });

    it('callback should be called', function() {
      var callback = sinon.spy();
      view.getGroupsAvailableValues('', callback);
      expect(view.get('groupsNameIdMap')).to.be.eql({
        'g1': 1
      });
      expect(callback.calledWith([])).to.be.true;
    });
  });

  describe('#mapLabelToValue', function() {
    it('should return value of State filter', function() {
      expect(view.mapLabelToValue('enabled', Em.I18n.t('alerts.table.state.enabled'))).to.be.equal('enabled');
    });

    it('should return value of Group filter', function() {
      view.set('groupsNameIdMap', {'l1': 1});
      expect(view.mapLabelToValue('groups', 'l1')).to.be.equal(1);
    });

    it('should return value of filter', function() {
      expect(view.mapLabelToValue('cat', 'l1')).to.be.equal('l1');
    });
  });
});
