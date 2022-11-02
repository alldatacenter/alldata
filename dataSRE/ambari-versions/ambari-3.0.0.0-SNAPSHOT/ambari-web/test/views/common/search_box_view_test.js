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
require('views/common/search_box_view');

describe('App.SearchBoxView', function () {
  var view;

  beforeEach(function () {
    view = App.SearchBoxView.create({
      parentView: Em.Object.create({
        updateComboFilter: Em.K,
        controller: {
          name: 'ctrl1'
        }
      }),
      keyFilterMap: [
        {
          label: 'l1',
          key: 'key1',
          type: 'type1',
          column: 1
        }
      ]
    });
  });

  describe('#didInsertElement', function() {
    beforeEach(function() {
      sinon.stub(view, 'initVS');
      sinon.stub(view, 'restoreComboFilterQuery');
      sinon.stub(view, 'showHideClearButton');
      sinon.stub(view, 'initOpenVSButton');
      view.didInsertElement();
    });
    afterEach(function() {
      view.initVS.restore();
      view.restoreComboFilterQuery.restore();
      view.showHideClearButton.restore();
      view.initOpenVSButton.restore();
    });

    it('initVS should be called', function() {
      expect(view.initVS.calledOnce).to.be.true;
    });

    it('restoreComboFilterQuery should be called', function() {
      expect(view.restoreComboFilterQuery.calledOnce).to.be.true;
    });

    it('showHideClearButton should be called', function() {
      expect(view.showHideClearButton.calledOnce).to.be.true;
    });

    it('initOpenVSButton should be called', function() {
      expect(view.initOpenVSButton.calledOnce).to.be.true;
    });
  });

  describe('#showErrMsg', function() {
    it('should set errMsg', function() {
      view.showErrMsg({attributes: {value: 'val1'}});
      expect(view.get('errMsg')).to.be.equal('val1 ' + Em.I18n.t('hosts.combo.search.invalidCategory'));
    });
  });

  describe('#clearErrMsg', function() {
    it('should set errMsg', function() {
      view.set('errMsg', 'aaa');
      view.clearErrMsg();
      expect(view.get('errMsg')).to.be.empty;
    });
  });

  describe('#initVS', function() {
    beforeEach(function() {
      sinon.stub(VS, 'init');
    });
    afterEach(function() {
      VS.init.restore();
    });

    it('VS.init should be called', function() {
      view.initVS();
      expect(VS.init.calledOnce).to.be.true;
    });
  });

  describe('#search', function() {
    beforeEach(function() {
      sinon.stub(view, 'clearErrMsg');
      sinon.stub(view, 'showHideClearButton');
      sinon.stub(view, 'findInvalidFacet').returns({});
      sinon.stub(view, 'showErrMsg');
      sinon.stub(App.db, 'setComboSearchQuery');
      sinon.stub(view, 'createFilterConditions').returns([]);
      sinon.stub(view.get('parentView'), 'updateComboFilter');
      view.search('query', []);
    });
    afterEach(function() {
      view.clearErrMsg.restore();
      view.showHideClearButton.restore();
      view.findInvalidFacet.restore();
      view.showErrMsg.restore();
      App.db.setComboSearchQuery.restore();
      view.createFilterConditions.restore();
      view.get('parentView').updateComboFilter.restore();
    });

    it('clearErrMsg should be called', function() {
      expect(view.clearErrMsg.calledOnce).to.be.true;
    });

    it('showHideClearButton should be called', function() {
      expect(view.showHideClearButton.calledOnce).to.be.true;
    });

    it('showErrMsg should be called', function() {
      expect(view.showErrMsg.calledOnce).to.be.true;
    });

    it('App.db.setComboSearchQuery should be called', function() {
      expect(App.db.setComboSearchQuery.calledWith('ctrl1', 'query')).to.be.true;
    });

    it('createFilterConditions should be called', function() {
      expect(view.createFilterConditions.calledWith([])).to.be.true;
    });

    it('updateComboFilter should be called', function() {
      expect(view.get('parentView').updateComboFilter.calledWith([])).to.be.true;
    });
  });

  describe('#facetMatches', function() {
    it('callback should be called', function() {
      var callback = sinon.spy();
      view.facetMatches(callback);
      expect(callback.calledWith(view.get('keyFilterMap').mapProperty('label'), {preserveOrder: true})).to.be.true;
    });
  });

  describe('#findInvalidFacet', function() {
    it('should return invalid facet', function() {
      var searchCollection = {
        models: [
          {
            attributes: {
              category: 'wrong'
            }
          },
          {
            attributes: {
              category: view.get('keyFilterMap').mapProperty('label')[0]
            }
          }
        ]
      };
      expect(view.findInvalidFacet(searchCollection)).to.be.eql(searchCollection.models[0]);
    });
  });

  describe('#restoreComboFilterQuery', function() {
    beforeEach(function() {
      sinon.stub(App.db, 'getComboSearchQuery').returns('query');
      visualSearch = {
        searchBox: {
          setQuery: sinon.spy()
        }
      };
    });
    afterEach(function() {
      App.db.getComboSearchQuery.restore();
    });

    it('visualSearch.searchBox.setQuery should be called', function() {
      view.restoreComboFilterQuery();
      expect(visualSearch.searchBox.setQuery.calledWith('query')).to.be.true;
    });
  });

  describe('#createFilterConditions', function() {

    it('should return empty when no correct category passed', function() {
      var searchCollection = {
        models: [
          {
            attributes: {
              category: 'wrong'
            }
          }
        ]
      };
      expect(view.createFilterConditions(searchCollection)).to.be.empty;
    });

    it('should return filter condition when correct category passed', function() {
      var searchCollection = {
        models: [
          {
            attributes: {
              category: view.get('keyFilterMap').mapProperty('label')[0],
              value: 'val1'
            }
          }
        ]
      };
      expect(view.createFilterConditions(searchCollection)).to.be.eql([
        {
          "iColumn": 1,
          "skipFilter": false,
          "type": "type1",
          "value": 'val1'
        }
      ]);
    });
  });

  describe('#mapLabelToValue', function() {
    it('should return label', function() {
      expect(view.mapLabelToValue('cat1', 'l1')).to.be.equal('l1');
    });
  });
});
