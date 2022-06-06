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

describe('App.MainConfigHistorySearchBoxView', function () {

  beforeEach(function () {
    view = App.MainConfigHistorySearchBoxView.create({
      controller: Em.Object.create({
        getSearchBoxSuggestions: function() {
          return {
            done: function(clb) {
              clb([{}]);
            }
          }
        }
      })
    });
    sinon.stub(view, 'rejectUsedValues').returns([]);
  });

  afterEach(function() {
    view.rejectUsedValues.restore();
  });

  describe('#valueMatches', function() {
    beforeEach(function() {
      sinon.stub(view, 'showHideClearButton');
      sinon.stub(view, 'getServiceVersionAvailableValues');
      sinon.stub(view, 'getConfigGroupAvailableValues');
      sinon.stub(view, 'getCreateTimeAvailableValues');
      sinon.stub(view, 'getAuthorAvailableValues');
      sinon.stub(view, 'getNotesAvailableValues');
    });
    afterEach(function() {
      view.showHideClearButton.restore();
      view.getServiceVersionAvailableValues.restore();
      view.getConfigGroupAvailableValues.restore();
      view.getCreateTimeAvailableValues.restore();
      view.getAuthorAvailableValues.restore();
      view.getNotesAvailableValues.restore();
    });

    it('showHideClearButton should be called', function() {
      view.valueMatches(Em.I18n.t('common.service'));
      expect(view.showHideClearButton.calledOnce).to.be.true;
    });

    it('getServiceVersionAvailableValues should be called', function() {
      view.valueMatches(Em.I18n.t('common.service'), '', Em.K);
      expect(view.getServiceVersionAvailableValues.calledWith(Em.I18n.t('common.service'))).to.be.true;
    });

    it('getConfigGroupAvailableValues should be called', function() {
      view.valueMatches(Em.I18n.t('common.configGroup'), '', Em.K);
      expect(view.getConfigGroupAvailableValues.calledWith(Em.I18n.t('common.configGroup'))).to.be.true;
    });

    it('getCreateTimeAvailableValues should be called', function() {
      view.valueMatches(Em.I18n.t('dashboard.configHistory.table.created.title'), '', Em.K);
      expect(view.getCreateTimeAvailableValues.calledWith(Em.I18n.t('dashboard.configHistory.table.created.title'))).to.be.true;
    });

    it('getAuthorAvailableValues should be called', function() {
      view.valueMatches(Em.I18n.t('common.author'), '', Em.K);
      expect(view.getAuthorAvailableValues.calledWith(Em.I18n.t('common.author'))).to.be.true;
    });

    it('getNotesAvailableValues should be called', function() {
      view.valueMatches(Em.I18n.t('common.notes'), '', Em.K);
      expect(view.getNotesAvailableValues.calledWith(Em.I18n.t('common.notes'))).to.be.true;
    });
  });

  describe('#getServiceVersionAvailableValues', function() {
    it('callback should be called', function() {
      var callback = sinon.spy();
      view.getServiceVersionAvailableValues('', callback);
      expect(callback.calledWith([], {preserveOrder: true})).to.be.true;
    });
  });

  describe('#getConfigGroupAvailableValues', function() {
    beforeEach(function() {
      sinon.stub(view, 'requestFacetSuggestions');
    });
    afterEach(function() {
      view.requestFacetSuggestions();
    });

    it('callback should be called', function() {
      var callback = sinon.spy();
      view.getConfigGroupAvailableValues('', callback);
      expect(callback.calledWith([])).to.be.true;
    });

    it('requestFacetSuggestions should be called', function() {
      var callback = sinon.spy();
      view.getConfigGroupAvailableValues('', callback);
      expect(view.requestFacetSuggestions.calledWith('', callback)).to.be.true;
    });
  });

  describe('#getCreateTimeAvailableValues', function() {
    it('callback should be called', function() {
      var callback = sinon.spy();
      view.getCreateTimeAvailableValues('', callback);
      expect(callback.calledWith([], {preserveOrder: true})).to.be.true;
    });
  });

  describe('#getAuthorAvailableValues', function() {
    beforeEach(function() {
      sinon.stub(view, 'requestFacetSuggestions');
    });
    afterEach(function() {
      view.requestFacetSuggestions();
    });

    it('callback should be called', function () {
      var callback = sinon.spy();
      view.getAuthorAvailableValues('', callback);
      expect(callback.calledWith([])).to.be.true;
    });

    it('requestFacetSuggestions should be called', function() {
      var callback = sinon.spy();
      view.getAuthorAvailableValues('', callback);
      expect(view.requestFacetSuggestions.calledWith('', callback)).to.be.true;
    });
  });

  describe('#getNotesAvailableValues', function() {
    beforeEach(function() {
      sinon.stub(view, 'requestFacetSuggestions');
    });
    afterEach(function() {
      view.requestFacetSuggestions();
    });

    it('callback should be called', function() {
      var callback = sinon.spy();
      view.getNotesAvailableValues('', callback);
      expect(callback.calledWith([])).to.be.true;
    });

    it('requestFacetSuggestions should be called', function() {
      var callback = sinon.spy();
      view.getNotesAvailableValues('', callback);
      expect(view.requestFacetSuggestions.calledWith('', callback)).to.be.true;
    });
  });

  describe('#mapLabelToValue', function() {
    beforeEach(function() {
      sinon.stub(view, 'computeCreateTimeRange').returns([1,2]);
      sinon.stub(App.StackService, 'find').returns([
        Em.Object.create({
          displayName: 's1',
          serviceName: 'S1'
        })
      ]);
    });
    afterEach(function() {
      view.computeCreateTimeRange.restore();
      App.StackService.find.restore();
    });

    it('should return value of State filter', function() {
      expect(view.mapLabelToValue('enabled', Em.I18n.t('alerts.table.state.enabled'))).to.be.equal(Em.I18n.t('alerts.table.state.enabled'));
    });

    it('should return value of createTime filter', function() {
      expect(view.mapLabelToValue('createTime', 'l1')).to.be.eql([1, 2]);
    });

    it('should return value of serviceVersion filter', function() {
      expect(view.mapLabelToValue('serviceVersion', 's1')).to.be.equal('S1');
    });
  });

  describe('#requestFacetSuggestions', function() {
    it('callback should be called', function() {
      var callback = sinon.spy();
      view.requestFacetSuggestions(view.get('keyFilterMap').mapProperty('label')[0], callback);
      expect(callback.calledWith([{}])).to.be.true;
    });
  });
});
