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
require('views/main/host/host_alerts_view');

var view;

describe('App.MainHostAlertsView', function () {

  beforeEach(function () {
    view = App.MainHostAlertsView.create({
      controller: Em.Object.create(),
      parentView: Em.Object.create({
        controller: Em.Object.create()
      })
    });
  });

  describe('#content', function () {
    var cases = [
      {
        m: 'return empty array',
        c: null,
        r: []
      },
      {
        m: 'return empty array',
        c: undefined,
        r: []
      },
      {
        m: 'sort CRITICAL and WARNING to be first',
        c: [
            Em.Object.create({
              state: 'OK'
            }),
            Em.Object.create({
              state: 'WARNING'
            }),
            Em.Object.create({
              state: 'CRITICAL'
            }),
            Em.Object.create({
              state: 'OK'
            })
        ],
        r: [
          Em.Object.create({
            state: 'CRITICAL'
          }),
          Em.Object.create({
            state: 'WARNING'
          }),
          Em.Object.create({
            state: 'OK'
          }),
          Em.Object.create({
            state: 'OK'
          })
        ]
      },
      {
        m: 'sort CRITICAL and WARNING to be first',
        c: [
          Em.Object.create({
            state: 'OTHER'
          }),
          Em.Object.create({
            state: 'WARNING'
          }),
          Em.Object.create({
            state: 'OK'
          }),
          Em.Object.create({
            state: 'CRITICAL'
          })
        ],
        r: [
          Em.Object.create({
            state: 'CRITICAL'
          }),
          Em.Object.create({
            state: 'WARNING'
          }),
          Em.Object.create({
            state: 'OK'
          }),
          Em.Object.create({
            state: 'OTHER'
          })
        ]
      }
    ];

    cases.forEach(function(test){
      it('should ' + test.m, function () {
        view.set('controller.content', test.c);
        expect(view.get('content')).eql(test.r);
      });
    });

  });

  describe("#willInsertElement()", function() {
    var mock = {
      loadAlertInstancesByHost: Em.K
    };

    beforeEach(function() {
      sinon.stub(App.router, 'get').returns(mock);
      sinon.spy(mock, 'loadAlertInstancesByHost');
      sinon.stub(App.router, 'set');
      view.set('parentView.controller.content', Em.Object.create({
        hostName: 'host1'
      }));
      sinon.stub(App.db, 'getSortingStatuses').returns([
        {
          name: "state",
          status: "sorting"
        }
      ]);
      sinon.stub(App.db, 'setSortingStatuses');
    });
    afterEach(function() {
      mock.loadAlertInstancesByHost.restore();
      App.router.get.restore();
      App.router.set.restore();
      App.db.getSortingStatuses.restore();
      App.db.setSortingStatuses.restore();
    });

    it("loadAlertInstancesByHost should be called", function() {
      view.willInsertElement();
      expect(mock.loadAlertInstancesByHost.calledWith('host1')).to.be.true;
    });

    it("isUpdating should be true", function() {
      view.willInsertElement();
      expect(App.router.set.calledWith('mainAlertInstancesController.isUpdating', true)).to.be.true;
    });

    it("App.db.setSortingStatuses should be called", function() {
      view.set('controller.name', 'ctrl1');
      view.willInsertElement();
      expect(App.db.setSortingStatuses.calledWith('ctrl1', [
        {
          name: "state",
          status: "sorting"
        },
        {
          name: "state",
          status: "sorting_asc"
        }
      ])).to.be.true;
    });
  });

  describe("#didInsertElement()", function() {

    beforeEach(function() {
      sinon.spy(view, 'tooltipsUpdater');
    });
    afterEach(function() {
      view.tooltipsUpdater.restore();
    });

    it("tooltipsUpdater should be called", function() {
      view.didInsertElement();
      expect(view.tooltipsUpdater.calledOnce).to.be.true;
    });
  });

  describe("#paginationLeftClass", function() {

    it("startIndex is 2", function() {
      view.set('startIndex', 2);
      expect(view.get('paginationLeftClass')).to.equal('paginate_previous');
    });

    it("startIndex is 1", function() {
      view.set('startIndex', 1);
      expect(view.get('paginationLeftClass')).to.equal('paginate_disabled_previous');
    });

    it("startIndex is 0", function() {
      view.set('startIndex', 0);
      expect(view.get('paginationLeftClass')).to.equal('paginate_disabled_previous');
    });
  });

  describe("#paginationRightClass", function() {

    it("endIndex more than filteredCount", function() {
      view.reopen({
        endIndex: 4,
        filteredCount: 3
      });
      expect(view.get('paginationRightClass')).to.equal('paginate_disabled_next');
    });

    it("endIndex equal to filteredCount", function() {
      view.reopen({
        endIndex: 4,
        filteredCount: 4
      });
      expect(view.get('paginationRightClass')).to.equal('paginate_disabled_next');
    });

    it("endIndex less than filteredCount", function() {
      view.reopen({
        endIndex: 3,
        filteredCount: 4
      });
      view.propertyDidChange('paginationRightClass');
      expect(view.get('paginationRightClass')).to.equal('paginate_next');
    });
  });

  describe("#clearFilters()", function() {
    var mock = {
      clearFilter: Em.K
    };

    beforeEach(function() {
      sinon.spy(mock, 'clearFilter');
    });
    afterEach(function() {
      mock.clearFilter.restore();
    });

    it("clearFilter should be called", function() {
      view.reopen({
        'childViews': [mock]
      });
      view.clearFilters();
      expect(view.get('filterConditions')).to.be.empty;
      expect(mock.clearFilter.calledOnce).to.be.true;
    });
  });

  describe("#willDestroyElement()", function() {
    var mock = {
      tooltip: Em.K
    };

    beforeEach(function() {
      sinon.stub(view, '$').returns(mock);
      sinon.spy(mock, 'tooltip');
    });
    afterEach(function() {
      view.$.restore();
      mock.tooltip.restore();
    });

    it("tooltip should be called", function() {
      view.willDestroyElement();
      expect(view.$.calledWith('.timeago, .alert-text')).to.be.true;
      expect(mock.tooltip.calledWith('destroy')).to.be.true;
    });
  });

});
