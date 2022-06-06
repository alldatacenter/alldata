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

describe('App.MainHostComboSearchBoxView', function () {

  beforeEach(function () {
    view = App.MainHostComboSearchBoxView.create({
      parentView: Em.Object.create(),
      controller: Em.Object.create({
        isComponentStateFacet: Em.K,
        isComplexHealthStatusFacet: Em.K
      })
    });
    window.visualSearch = {
      searchQuery: {
        values: function() {
          return [];
        },
        toJSON: Em.K
      },
      searchBox: {
        setQuery: Em.K
      }
    }
  });

  describe("#didInsertElement()", function() {

    beforeEach(function() {
      sinon.stub(view, 'initVS');
      sinon.stub(view, 'showHideClearButton');
      sinon.stub(view, 'restoreComboFilterQuery');
      view.didInsertElement();
    });
    afterEach(function() {
      view.initVS.restore();
      view.showHideClearButton.restore();
      view.restoreComboFilterQuery.restore();
    });

    it("initVS should be called", function() {
      expect(view.initVS.calledOnce).to.be.true;
    });

    it("showHideClearButton should be called", function() {
      expect(view.showHideClearButton.calledOnce).to.be.true;
    });

    it("restoreComboFilterQuery should be called", function() {
      expect(view.restoreComboFilterQuery.calledOnce).to.be.true;
    });
  });

  describe("#setupLabelMap", function () {

    beforeEach(function() {
      sinon.stub(view, 'setupLabelMap');
      sinon.stub(VS, 'init').returns({init: 'init'});
      view.initVS();
    });

    afterEach(function() {
      view.setupLabelMap.restore();
      VS.init.restore();
    });

    it("setupLabelMap should be called", function() {
      expect(view.setupLabelMap.calledOnce).to.be.true;
    });

    it("window.visualSearch should be set", function() {
      expect(window.visualSearch).to.be.eql({init: 'init'});
    });
  });

  describe("#search()", function () {

    beforeEach(function() {
      view.set('parentView', Em.Object.create({
        updateComboFilter: Em.K,
        controller: {name: 'ctrl1'}
      }));
      sinon.stub(view.get('parentView'), 'updateComboFilter');
      sinon.stub(view, 'createFilterConditions').returns([{}]);
      sinon.stub(App.db, 'setComboSearchQuery');
    });

    afterEach(function() {
      App.db.setComboSearchQuery.restore();
      view.createFilterConditions.restore();
      view.get('parentView').updateComboFilter.restore();
    });

    it("App.db.setComboSearchQuery should be called", function() {
      view.search('query', {});
      expect(App.db.setComboSearchQuery.calledWith('ctrl1', 'query')).to.be.true;
    });

    it("updateComboFilter should be called", function() {
      view.search('query', {});
      expect(view.get('parentView').updateComboFilter.calledWith([{}])).to.be.true;
    });
  });

  describe("#facetMatches()", function () {
    var container = {
      callback: Em.K
    };

    beforeEach(function() {
      sinon.stub(view, 'showHideClearButton');
      sinon.stub(view, 'getHostComponentList').returns([
        Em.Object.create({componentName: 'C1'})
      ]);
      this.mock = sinon.stub(view, 'getComponentStateFacets').returns([]);
      sinon.stub(view, 'filterOutOneFacetOnlyOptions', function(list) {
        return list;
      });
      sinon.stub(container, 'callback');
    });

    afterEach(function() {
      view.showHideClearButton.restore();
      view.getHostComponentList.restore();
      this.mock.restore();
      view.filterOutOneFacetOnlyOptions.restore();
      container.callback.restore();
    });

    it("showHideClearButton should be called", function() {
      view.facetMatches(container.callback);
      expect(view.showHideClearButton.calledOnce).to.be.true;
    });

    it("callback should be called", function() {
      view.facetMatches(container.callback);
      expect(container.callback.calledWith(
        [
          {label: 'Host Name', category: 'Host'},
          {label: 'IP', category: 'Host'},
          {label: 'Host Status', category: 'Host'},
          {label: 'Cores', category: 'Host'},
          {label: 'RAM', category: 'Host'},
          {label: 'Stack Version', category: 'Host'},
          {label: 'Version State', category: 'Host'},
          {label: 'Rack', category: 'Host'},
          {label: 'Service', category: 'Service'},
          Em.Object.create({componentName: 'C1'})
        ],
        {preserveOrder: true}
      )).to.be.true;
    });

    it("callback should be called, facets not empty", function() {
      this.mock.returns([{}]);
      view.facetMatches(container.callback);
      expect(container.callback.calledWith(
        [
          {label: 'Host Name', category: 'Host'},
          {label: 'IP', category: 'Host'},
          {label: 'Host Status', category: 'Host'},
          {label: 'Cores', category: 'Host'},
          {label: 'RAM', category: 'Host'},
          {label: 'Stack Version', category: 'Host'},
          {label: 'Version State', category: 'Host'},
          {label: 'Rack', category: 'Host'},
          {label: 'Service', category: 'Service'}
        ],
        {preserveOrder: true}
      )).to.be.true;
    });
  });

  describe("#valueMatches()", function () {
    var controller = {
      isComponentStateFacet: Em.K
    };
    var container = {
      callback: Em.K
    };

    beforeEach(function() {
      sinon.stub(view, 'showHideClearButton');
      this.mockRouter = sinon.stub(App.router, 'get');
      this.mockRouter.withArgs('mainHostComboSearchBoxController').returns(controller);
      this.mockRouter.withArgs('mainHostController.labelValueMap').returns({
        key1: 'hostName'
      });
      this.mockIsComp = sinon.stub(controller, 'isComponentStateFacet').returns(false);
      sinon.stub(container, 'callback');
      sinon.stub(view, 'searchByHostname');
      sinon.stub(view, 'searchByRack');
      sinon.stub(view, 'searchByVersion');
      sinon.stub(view, 'searchByVersionState');
      sinon.stub(view, 'searchByHealthClass');
      sinon.stub(view, 'searchByServices');
      sinon.stub(view, 'searchByComponentState');
    });

    afterEach(function() {
      view.showHideClearButton.restore();
      this.mockRouter.restore();
      this.mockIsComp.restore();
      container.callback.restore();
      view.searchByHostname.restore();
      view.searchByRack.restore();
      view.searchByVersion.restore();
      view.searchByVersionState.restore();
      view.searchByHealthClass.restore();
      view.searchByServices.restore();
      view.searchByComponentState.restore();
    });

    it("showHideClearButton should be called", function() {
      view.valueMatches('', '', container.callback);
      expect(view.showHideClearButton.calledOnce).to.be.true;
    });

    it("searchByHostname should be called", function() {
      view.valueMatches('key1', 'term1', container.callback);
      expect(view.searchByHostname.calledWith(
        'hostName', 'term1', 'key1', container.callback
      )).to.be.true;
    });

    it("searchByRack should be called", function() {
      view.valueMatches('rack', 'term1', container.callback);
      expect(view.searchByRack.calledWith('rack', container.callback)).to.be.true;
    });

    it("searchByVersion should be called", function() {
      view.valueMatches('version', 'term1', container.callback);
      expect(view.searchByVersion.calledWith('version', container.callback)).to.be.true;
    });

    it("searchByVersionState should be called", function() {
      view.valueMatches('versionState', 'term1', container.callback);
      expect(view.searchByVersionState.calledWith('versionState', container.callback, {
        key1: 'hostName'
      })).to.be.true;
    });

    it("searchByHealthClass should be called", function() {
      view.valueMatches('healthClass', 'term1', container.callback);
      expect(view.searchByHealthClass.calledWith('healthClass', container.callback, {
        key1: 'hostName'
      })).to.be.true;
    });

    it("searchByServices should be called", function() {
      view.valueMatches('services', 'term1', container.callback);
      expect(view.searchByServices.calledWith('services', container.callback)).to.be.true;
    });

    it("searchByComponentState should be called", function() {
      this.mockIsComp.returns(true);
      view.valueMatches('componentState', 'term1', container.callback);
      expect(view.searchByComponentState.calledWith('componentState', container.callback, {
        key1: 'hostName'
      })).to.be.true;
    });
  });

  describe("#searchByHostname()", function () {
    var controller = Em.Object.create({
      getPropertySuggestions: Em.K,
      currentSuggestion: ['sg1', 'sg2']
    });
    var container = {
      callback: Em.K
    };


    beforeEach(function() {
      sinon.stub(App.router, 'get').returns(controller);
      sinon.stub(controller, 'getPropertySuggestions').returns({
        done: function(callback) {
          callback();
        }
      });
      sinon.stub(container, 'callback');
      sinon.stub(visualSearch.searchQuery, 'values').returns(['sg1']);
    });

    afterEach(function() {
      App.router.get.restore();
      controller.getPropertySuggestions.restore();
      container.callback.restore();
      visualSearch.searchQuery.values.restore();
    });

    it("callback should be called", function() {
      view.searchByHostname('fv1', 'term1', 'f1', container.callback);
      expect(container.callback.calledWith(['sg2'], {preserveMatches: true})).to.be.true;
    });
  });

  describe("#searchByRack()", function () {
    var container = {
      callback: Em.K
    };

    beforeEach(function() {
      sinon.stub(container, 'callback');
      sinon.stub(visualSearch.searchQuery, 'values').returns(['r1']);
      sinon.stub(App.Host, 'find').returns([{rack: 'r1'}, {rack: 'r2'}]);
    });

    afterEach(function() {
      container.callback.restore();
      visualSearch.searchQuery.values.restore();
      App.Host.find.restore();
    });

    it("callback should be called", function() {
      view.searchByRack('f1', container.callback);
      expect(container.callback.calledWith(['r2'])).to.be.true;
    });
  });

  describe("#searchByVersion()", function () {
    var container = {
      callback: Em.K
    };

    beforeEach(function() {
      sinon.stub(container, 'callback');
      sinon.stub(visualSearch.searchQuery, 'values').returns(['v1']);
      sinon.stub(App.HostStackVersion, 'find').returns([
        {
          isVisible: true,
          displayName: 'v1'
        },
        {
          isVisible: true,
          displayName: 'v2'
        },
        {
          isVisible: false,
          displayName: 'v3'
        }
      ]);
    });

    afterEach(function() {
      container.callback.restore();
      visualSearch.searchQuery.values.restore();
      App.HostStackVersion.find.restore();
    });

    it("callback should be called", function() {
      view.searchByVersion('f1', container.callback);
      expect(container.callback.calledWith(['v2'])).to.be.true;
    });
  });

  describe("#searchByVersionState()", function () {
    var container = {
      callback: Em.K
    };

    beforeEach(function() {
      sinon.stub(container, 'callback');
      sinon.stub(visualSearch.searchQuery, 'values').returns(['INSTALLED']);
      sinon.stub(App.HostStackVersion, 'formatStatus', function(status) {
        return status;
      });
    });

    afterEach(function() {
      container.callback.restore();
      visualSearch.searchQuery.values.restore();
      App.HostStackVersion.formatStatus.restore();
    });

    it("callback should be called", function() {
      view.searchByVersionState('f1', container.callback, {});
      expect(container.callback.getCall(0).args[0]).to.be.eql([
        "INSTALLING",
        "INSTALL_FAILED",
        "OUT_OF_SYNC",
        "CURRENT",
        "UPGRADING",
        "UPGRADE_FAILED"
      ]);
    });
  });

  describe("#searchByHealthClass()", function () {
    var container = {
      callback: Em.K
    };

    beforeEach(function() {
      sinon.stub(container, 'callback');
      sinon.stub(visualSearch.searchQuery, 'values').returns(['c1']);
      view.set('healthStatusCategories', [
        {value: 'All'},
        {value: 'c1'},
        {value: 'c2'}
      ]);
    });

    afterEach(function() {
      container.callback.restore();
      visualSearch.searchQuery.values.restore();
    });

    it("callback should be called", function() {
      view.searchByHealthClass('f1', container.callback, {});
      expect(container.callback.getCall(0).args).to.be.eql([
        ['c2'], {preserveOrder: true}
      ]);
    });
  });

  describe("#searchByServices()", function () {
    var container = {
      callback: Em.K
    };

    beforeEach(function() {
      sinon.stub(container, 'callback');
      sinon.stub(visualSearch.searchQuery, 'values').returns(['s1']);
      sinon.stub(App.Service, 'find').returns([
        Em.Object.create({serviceName: 'S1'}),
        Em.Object.create({serviceName: 'S2'})
      ]);
    });

    afterEach(function() {
      container.callback.restore();
      visualSearch.searchQuery.values.restore();
      App.Service.find.restore();
    });

    it("callback should be called", function() {
      view.searchByServices('f1', container.callback);
      expect(container.callback.getCall(0).args).to.be.eql([
        ['S2'], {preserveOrder: true}
      ]);
    });
  });


  describe("#searchByComponentState()", function () {
    var container = {
      callback: Em.K
    };

    beforeEach(function() {
      sinon.stub(container, 'callback');
      this.mock = sinon.stub(view, 'getComponentStateFacets').returns(['cf1']);
      sinon.stub(App.HostComponentStatus, 'getStatusesList').returns([
        'UPGRADE_FAILED', 'ST1'
      ]);
      sinon.stub(App.HostComponentStatus, 'getTextStatus', function(status) {
        return status;
      });
    });

    afterEach(function() {
      container.callback.restore();
      this.mock.restore();
      App.HostComponentStatus.getStatusesList.restore();
      App.HostComponentStatus.getTextStatus.restore();
    });

    it("callback should be called, client", function() {
      view.searchByComponentState('client', container.callback, {});
      expect(container.callback.getCall(0).args).to.be.eql([
        ['All'], {preserveOrder: true}
      ]);
    });

    it("callback should be called", function() {
      view.searchByComponentState('f1', container.callback, {});
      expect(container.callback.getCall(0).args).to.be.eql([
        ['All'], {preserveOrder: true}
      ]);
    });

    it("callback should be called, empty state facets", function() {
      this.mock.returns([]);
      view.searchByComponentState('f1', container.callback, {});
      expect(container.callback.getCall(0).args).to.be.eql([
        [
          "All",
          "ST1",
          "Inservice",
          "Decommissioned",
          "Decommissioning",
          "RS Decommissioned",
          "Maintenance Mode On",
          "Maintenance Mode Off"
        ], {preserveOrder: true}
      ]);
    });
  });

  describe("#showErrMsg()", function () {

    it("errMsg should be set", function() {
      view.showErrMsg({attributes: {value: 'val1'}});
      expect(view.get('errMsg')).to.be.equal('val1 ' + Em.I18n.t('hosts.combo.search.invalidCategory'));
    });
  });

  describe("#clearErrMsg()", function () {

    it("errMsg should be empty", function() {
      view.clearErrMsg();
      expect(view.get('errMsg')).to.be.empty;
    });
  });

  describe("#getHostComponentList()", function () {
    var labelValueMap = {};

    beforeEach(function() {
      sinon.stub(App.MasterComponent, 'find').returns([
        Em.Object.create({totalCount: 0}),
        Em.Object.create({totalCount: 1}),
        Em.Object.create({totalCount: 1, displayName: 'mc1', componentName: 'MC1'})
      ]);
      sinon.stub(App.SlaveComponent, 'find').returns([
        Em.Object.create({totalCount: 0}),
        Em.Object.create({totalCount: 1}),
        Em.Object.create({totalCount: 1, displayName: 'sc1', componentName: 'SC1'})
      ]);
      sinon.stub(App.ClientComponent, 'find').returns([
        Em.Object.create({totalCount: 0}),
        Em.Object.create({totalCount: 1}),
        Em.Object.create({totalCount: 1, displayName: 'cc1', componentName: 'CC1'})
      ]);
      sinon.stub(App.router, 'get').returns(labelValueMap);
    });

    afterEach(function() {
      App.MasterComponent.find.restore();
      App.SlaveComponent.find.restore();
      App.ClientComponent.find.restore();
      App.router.get.restore();
    });

    it("should return sorted host-component list", function() {
      expect(view.getHostComponentList()).to.be.eql([
        {label: 'cc1', category: 'Component'},
        {label: 'mc1', category: 'Component'},
        {label: 'sc1', category: 'Component'}
      ]);
      expect(labelValueMap).to.be.eql({
        mc1: 'MC1',
        sc1: 'SC1',
        cc1: 'CC1'
      });
    });
  });

  describe("#getComponentStateFacets()", function () {

    beforeEach(function() {
      sinon.stub(visualSearch.searchQuery, 'toJSON').returns([
        {category: 'cat1', value: 'val'},
        {category: 'cat2', value: null},
        {category: 'cat3', value: 'All'}
      ]);
      sinon.stub(view, 'getHostComponentList').returns([{label: ''}]);
    });

    afterEach(function() {
      visualSearch.searchQuery.toJSON.restore();
      view.getHostComponentList.restore();
    });

    it("label='', includeAllValue=true", function() {
      expect(view.getComponentStateFacets(null, true)).to.be.empty;
    });

    it("label=cat2, includeAllValue=true", function() {
      expect(view.getComponentStateFacets([{label: 'cat2'}], true)).to.be.empty;
    });

    it("label=cat1, includeAllValue=true", function() {
      expect(view.getComponentStateFacets([{label: 'cat1'}], true)).to.be.eql([
        {category: 'cat1', value: 'val'}
      ]);
    });

    it("label=cat1, includeAllValue=false", function() {
      expect(view.getComponentStateFacets([{label: 'cat1'}], false)).to.be.eql([
        {category: 'cat1', value: 'val'}
      ]);
    });

    it("label=cat3, includeAllValue=false", function() {
      expect(view.getComponentStateFacets([{label: 'cat3'}], false)).to.be.empty;
    });
  });

  describe("#getFacetsByName()", function () {

    beforeEach(function() {
      sinon.stub(visualSearch.searchQuery, 'toJSON').returns([
        {category: 'f1'},
        {category: 'f2'}
      ]);
    });

    afterEach(function() {
      visualSearch.searchQuery.toJSON.restore();
    });

    it("should return facets", function() {
      expect(view.getFacetsByName('f1')).to.be.eql([{category: 'f1'}]);
    });
  });

  describe("#filterOutOneFacetOnlyOptions()", function () {

    beforeEach(function() {
      this.mock = sinon.stub(view, 'getFacetsByName');
    });

    afterEach(function() {
      view.getFacetsByName.restore();
    });

    it("should return empty, no facets", function() {
      this.mock.returns([]);
      expect(view.filterOutOneFacetOnlyOptions([])).to.be.empty;
    });

    it("should return empty", function() {
      this.mock.returns([{}]);
      expect(view.filterOutOneFacetOnlyOptions([{label: 'Cores'}])).to.be.empty;
    });

    it("should return options", function() {
      this.mock.returns([{}]);
      expect(view.filterOutOneFacetOnlyOptions([{label: 'f1'}])).to.be.eql([{label: 'f1'}]);
    });
  });

  describe("#setupLabelMap()", function () {
    var map = {
      'init': 'init'
    };

    beforeEach(function() {
      sinon.stub(App.router, 'get').returns(map);
    });

    afterEach(function() {
      App.router.get.restore();
    });

    it("should set map", function() {
      view.setupLabelMap();
      expect(map).to.be.eql({
        'init': 'init',
        'All': 'ALL',
        'Host Name': 'hostName',
        'IP': 'ip',
        'Host Status': 'healthClass',
        'Cores': 'cpu',
        'RAM': 'memoryFormatted',
        'Stack Version': 'version',
        'Version State': 'versionState',
        'Rack': 'rack',
        'Service': 'services',
        'Inservice': 'INSERVICE',
        'Decommissioned': 'DECOMMISSIONED',
        'Decommissioning': 'DECOMMISSIONING',
        'RS Decommissioned': 'RS_DECOMMISSIONED',
        'Maintenance Mode On': 'ON',
        'Maintenance Mode Off': 'OFF'
      });
    });
  });

  describe("#createFilterConditions()", function () {

    beforeEach(function() {
      sinon.stub(view, 'getFilterColumn').returns(1);
      sinon.stub(view, 'getFilterValue').returns('filterValue');
      sinon.stub(App.router, 'get').returns({'k1': 'services'});
      sinon.stub(view, 'createCondition').returns({
        skipFilter: false,
        iColumn: 1,
        value: 'val1',
        type: 't1'
      });
      view.set('serviceMap', {k2: 'val2'});
    });

    afterEach(function() {
      view.getFilterColumn.restore();
      view.getFilterValue.restore();
      App.router.get.restore();
      view.createCondition.restore();
    });

    it("empty models", function() {
      var searchCollection = {
        models: []
      };
      expect(view.createFilterConditions(searchCollection)).to.be.empty;
    });

    it("should return conditions, string value", function() {
      var searchCollection = {
        models: [
          {
            attributes: {
              category: 'k1',
              value: 'k2'
            }
          },
          {
            attributes: {
              category: 'k1',
              value: 'k2'
            }
          }
        ]
      };
      expect(JSON.stringify(view.createFilterConditions(searchCollection))).to.be.equal(JSON.stringify([
        {
          "skipFilter": false,
          "iColumn": 1,
          "value": [
            "val1",
            "filterValue"
          ],
          "type": "t1"
        }
      ]));
    });


    it("should return conditions, array value", function() {
      var searchCollection = {
        models: [
          {
            attributes: {
              category: 'cat1',
              value: ['val1']
            }
          },
          {
            attributes: {
              category: 'cat2',
              value: 'val2'
            }
          }
        ]
      };
      expect(JSON.stringify(view.createFilterConditions(searchCollection))).to.be.equal(JSON.stringify([
        {
          "skipFilter": false,
          "iColumn": 1,
          "value": [
            "val1",
            "filterValue"
          ],
          "type": "t1"
        }
      ]));
    });
  });

  describe("#createCondition()", function () {
    var testCases = [
      {
        category: 'cpu',
        iColumn: 1,
        filterValue: 'val1',
        expected: {
          skipFilter: false,
          iColumn: 1,
          value: 'val1',
          type: 'number'
        }
      },
      {
        category: 'memoryFormatted',
        iColumn: 2,
        filterValue: 'val2',
        expected: {
          skipFilter: false,
          iColumn: 2,
          value: 'val2',
          type: 'ambari-bandwidth'
        }
      },
      {
        category: 'cat1',
        iColumn: 3,
        filterValue: ['val2'],
        expected: {
          skipFilter: false,
          iColumn: 3,
          value: ['val2'],
          type: 'string'
        }
      }
    ];

    testCases.forEach(function(test) {
      it("category = " + test.category +
         " iColumn = " + test.iColumn +
         " filterValue = " + test.filterValue, function() {
        expect(view.createCondition(test.category, test.iColumn, test.filterValue)).to.be.eql(test.expected);
      });
    });
  });

  describe("#getFilterColumn()", function () {

    beforeEach(function() {
      this.mockComponentState = sinon.stub(view.get('controller'), 'isComponentStateFacet');
      this.mockComplexHealth = sinon.stub(view.get('controller'), 'isComplexHealthStatusFacet');
      sinon.stub(App.router, 'get').returns(['componentState', 'cat1']);
      view.set('healthStatusCategories', [{healthStatus: 'val1', column: 2}]);
    });

    afterEach(function() {
      this.mockComponentState.restore();
      this.mockComplexHealth.restore();
      App.router.get.restore();
    });

    it("should return componentState column", function() {
      this.mockComponentState.returns(true);
      expect(view.getFilterColumn('cat1', 'val1')).to.be.equal(0);
    });

    it("should return healthStatus column", function() {
      this.mockComplexHealth.returns(true);
      expect(view.getFilterColumn('cat1', 'val1')).to.be.equal(2);
    });

    it("should return custom column", function() {
      expect(view.getFilterColumn('cat1', 'val1')).to.be.equal(1);
    });
  });

  describe("#getFilterValue()", function () {

    beforeEach(function() {
      this.mockComponentState = sinon.stub(view.get('controller'), 'isComponentStateFacet');
      this.mockComplexHealth = sinon.stub(view.get('controller'), 'isComplexHealthStatusFacet');
      view.set('healthStatusCategories', [{healthStatus: 'val1', filterValue: 'filterValue'}]);
    });

    afterEach(function() {
      this.mockComponentState.restore();
      this.mockComplexHealth.restore();
    });

    it("should return componentState filter value", function() {
      this.mockComponentState.returns(true);
      expect(view.getFilterValue('cat1', 'val1')).to.be.equal('cat1:val1');
    });

    it("should return health filter value", function() {
      this.mockComplexHealth.returns(true);
      expect(view.getFilterValue('cat1', 'val1')).to.be.equal('filterValue');
    });

    it("should return other filter value", function() {
      expect(view.getFilterValue('cat1', 'val1')).to.be.equal('val1');
    });
  });
});