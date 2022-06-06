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
require('messages');
require('views/main/alert_definitions_view');

var view;

function getView() {
  return App.MainAlertDefinitionsView.create({
    controller: Em.Object.create()
  });
}

describe('App.MainAlertDefinitionsView', function () {

  beforeEach(function () {
    view = getView();
    sinon.stub(App.db, 'setFilterConditions', Em.K);
    sinon.stub(App.db, 'getFilterConditions').returns([]);
    sinon.stub(App.db, 'getDisplayLength', Em.K);
    sinon.stub(App.db, 'setStartIndex', Em.K);
    sinon.stub(view, 'initFilters', Em.K);
  });

  afterEach(function () {
    App.db.setFilterConditions.restore();
    App.db.getFilterConditions.restore();
    App.db.getDisplayLength.restore();
    App.db.setStartIndex.restore();
    view.initFilters.restore();
  });

  App.TestAliases.testAsComputedAlias(getView(), 'totalCount', 'content.length', 'number');

  describe('#willInsertElement', function () {

    beforeEach(function(){
      sinon.stub(view, 'clearFilterConditionsFromLocalStorage', Em.K);
      sinon.stub(App.db, 'getSortingStatuses').returns([
        {
          name: "summary",
          status: "sorting"
        }
      ]);
      sinon.stub(App.db, 'setSortingStatuses');
    });

    afterEach(function(){
      view.clearFilterConditionsFromLocalStorage.restore();
      App.db.getSortingStatuses.restore();
      App.db.setSortingStatuses.restore();
    });

    it('should call clearFilterCondition if controller.showFilterConditionsFirstLoad is false', function () {
      view.set('controller', {showFilterConditionsFirstLoad: false, content: []});
      view.willInsertElement();
      expect(view.clearFilterConditionsFromLocalStorage.calledOnce).to.be.true;
    });

    it('should not call clearFilterCondition if controller.showFilterConditionsFirstLoad is true', function () {
      view.set('controller', {showFilterConditionsFirstLoad: true, content: []});
      view.willInsertElement();
      expect(view.clearFilterConditionsFromLocalStorage.calledOnce).to.be.false;
    });

    it('showFilterConditionsFirstLoad is true', function () {
      view.set('controller', {showFilterConditionsFirstLoad: true, name: 'ctrl1'});
      view.willInsertElement();
      expect(App.db.setSortingStatuses.calledWith('ctrl1',
        [
          {
            name: "summary",
            status: "sorting"
          },
          {
            name: "summary",
            status: "sorting_desc"
          }
        ])).to.be.true;
    });
  });

  describe("#didInsertElement()", function () {

    beforeEach(function() {
      sinon.stub(Em.run, 'next', Em.clb);
      sinon.stub(view, 'contentObsOnce');
      sinon.stub(view, 'tooltipsUpdater');
      view.didInsertElement();
    });

    afterEach(function() {
      Em.run.next.restore();
      view.contentObsOnce.restore();
      view.tooltipsUpdater.restore();
    });

    it("isInitialRendering should be false", function() {
      expect(view.get('isInitialRendering')).to.be.false;
    });

    it("contentObsOnce should be called", function() {
      expect(view.contentObsOnce.calledOnce).to.be.true;
    });

    it("tooltipsUpdater should be called", function() {
      expect(view.tooltipsUpdater.calledOnce).to.be.true;
    });
  });

  describe("#willDestroyElement()", function () {
    var container = {
      tooltip: Em.K
    };

    beforeEach(function() {
      sinon.stub(window, '$').returns(container);
      sinon.stub(view, 'removeObserver');
      sinon.stub(container, 'tooltip');
      view.willDestroyElement();
    });

    afterEach(function() {
      window.$.restore();
      view.removeObserver.restore();
      container.tooltip.restore();
    });

    it("tooltip should be destroyed", function() {
      expect(container.tooltip.calledWith('destroy')).to.be.true;
    });

    it("removeObserver should be called", function() {
      expect(view.removeObserver.calledWith('pageContent.length', view, 'tooltipsUpdater')).to.be.true;
    });
  });

  describe("#saveStartIndex()", function () {

    it("App.db.setStartIndex should be called", function() {
      view.set('controller.name', 'ctrl1');
      view.set('startIndex', 1);
      view.saveStartIndex();
      expect(App.db.setStartIndex.calledWith('ctrl1', 1)).to.be.true;
    });
  });

  describe("#clearStartIndex()", function () {

    it("App.db.setStartIndex should be called", function() {
      view.set('controller.name', 'ctrl1');
      view.clearStartIndex();
      expect(App.db.setStartIndex.calledWith('ctrl1', null)).to.be.true;
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

  describe("#previousPage()", function () {

    beforeEach(function() {
      sinon.stub(view, 'tooltipsUpdater');
    });

    afterEach(function() {
      view.tooltipsUpdater.restore();
    });

    it("tooltipsUpdater should be called", function() {
      view.previousPage();
      expect(view.tooltipsUpdater.calledOnce).to.be.true;
    });
  });

  describe("#nextPage()", function () {

    beforeEach(function() {
      sinon.stub(view, 'tooltipsUpdater');
    });

    afterEach(function() {
      view.tooltipsUpdater.restore();
    });

    it("tooltipsUpdater should be called", function() {
      view.nextPage();
      expect(view.tooltipsUpdater.calledOnce).to.be.true;
    });
  });

  describe("#tooltipsUpdater", function () {

    beforeEach(function() {
      sinon.stub(Em.run, 'next', function(context, callback) {
        callback();
      });
      sinon.stub(App, 'tooltip');
    });

    afterEach(function() {
      Em.run.next.restore();
      App.tooltip.restore();
    });

    it("App.tooltip should be called", function() {
      view.tooltipsUpdater();
      expect(App.tooltip.calledOnce).to.be.true;
    });
  });

  describe("#updateFilter()", function () {

    beforeEach(function() {
      sinon.stub(view, 'tooltipsUpdater');
    });

    afterEach(function() {
      view.tooltipsUpdater.restore();
    });

    it("tooltipsUpdater should be called", function() {
      view.updateFilter(1, 'val1', 'type1');
      expect(view.tooltipsUpdater.calledOnce).to.be.true;
    });
  });

  describe('#contentObsOnce', function () {
    var toArray = function () {
        return [{}];
      },
      cases = [
        {
          controllerContent: null,
          isAlertsLoaded: false,
          content: [],
          title: 'no content in controller, alerts data not loaded'
        },
        {
          controllerContent: null,
          isAlertsLoaded: true,
          content: [],
          title: 'no content in controller, alerts data loaded'
        },
        {
          controllerContent: {
            toArray: toArray
          },
          isAlertsLoaded: false,
          content: [],
          title: 'content set in controller, alerts data not loaded'
        },
        {
          controllerContent: {
            toArray: toArray
          },
          isAlertsLoaded: true,
          content: [{}],
          title: 'content set in controller, alerts data loaded'
        }
      ];

    cases.forEach(function (item) {
      describe(item.title, function () {
        beforeEach(function () {
          var controller = {
            content: item.controllerContent
          };
          sinon.stub(App.AlertDefinition, 'getSortDefinitionsByStatus', function () {
            return Em.K;
          });
          sinon.stub(view, 'contentObs', Em.K);
          sinon.stub(App, 'get', function (key) {
            if (key === 'router.clusterController.isAlertsLoaded') {
              return item.isAlertsLoaded;
            }
            return Em.get(App, key);
          });
          view.set('controller', controller);
          view.contentObsOnce();
        });

        afterEach(function () {
          view.contentObs.restore();
          App.get.restore();
          App.AlertDefinition.getSortDefinitionsByStatus.restore();
        });

        it('view.content', function () {
          expect(view.get('content')).to.eql(item.content);
        });
      });
    });
  });

});
