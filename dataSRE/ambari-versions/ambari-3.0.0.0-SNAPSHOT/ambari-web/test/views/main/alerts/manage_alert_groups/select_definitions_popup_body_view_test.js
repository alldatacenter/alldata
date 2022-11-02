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
require('views/main/alerts/manage_alert_groups/select_definitions_popup_body_view');


var view;

describe('App.SelectDefinitionsPopupBodyView', function () {

  beforeEach(function () {
    view = App.SelectDefinitionsPopupBodyView.create({
      parentView: Em.Object.create()
    });
    sinon.stub(App.db, 'setFilterConditions', Em.K);
    sinon.stub(App.db, 'getFilterConditions', function () {
      return [];
    });
    sinon.stub(view, 'initFilters', Em.K)
  });

  afterEach(function () {
    App.db.setFilterConditions.restore();
    App.db.getFilterConditions.restore();
    view.initFilters.restore();
  });

  describe("#filteredContentObs()", function () {
    beforeEach(function () {
      sinon.stub(Em.run, 'once', Em.K);
    });
    afterEach(function () {
      Em.run.once.restore();
    });

    it("Em.run.once should be called", function () {
      view.filteredContentObs();
      expect(Em.run.once.calledWith(view)).to.be.true;
    });
  });

  describe("#filteredContentObsOnce()", function () {
    beforeEach(function () {
      sinon.stub(view, 'filterDefs', Em.K);
    });
    afterEach(function () {
      view.filterDefs.restore();
    });
    it("filteredContent should be set", function () {
      view.set('parentView.availableDefs', [
        {
          filtered: true
        },
        {
          filtered: false
        }
      ]);
      view.filteredContentObsOnce();
      expect(view.get('filteredContent')).to.eql([{
        filtered: true
      }]);
    });
  });

  describe("#didInsertElement()", function () {
    beforeEach(function () {
      sinon.stub(view, 'filterDefs', Em.K);
      sinon.stub(view, 'filteredContentObsOnce', Em.K);
      view.set('initialDefs', [
        Em.Object.create({filtered: true}),
        Em.Object.create({filtered: false})
      ]);
      view.didInsertElement();
    });
    afterEach(function () {
      view.filterDefs.restore();
      view.filteredContentObsOnce.restore();
    });
    it("each availableDefs filtered is updated", function () {
      expect(view.get('parentView.availableDefs').mapProperty('filtered')).to.eql([true, true]);
    });
    it("parentView.isLoaded is true", function () {
      expect(view.get('parentView.isLoaded')).to.be.true;
    });
    it("filteredContentObsOnce is called once", function () {
      expect(view.filteredContentObsOnce.calledOnce).to.be.true;
    });
  });

  describe("#filterDefs()", function () {
    var testCases = [
      {
        title: 'should be filtered',
        data: {
          defs: [
            Em.Object.create({
              filtered: false
            })
          ],
          filterComponent: null,
          filterService: null,
          showOnlySelectedDefs: false
        },
        result: [true]
      },
      {
        title: 'should be filtered by serviceName',
        data: {
          defs: [
            Em.Object.create({
              filtered: false,
              serviceName: 'S1'
            })
          ],
          filterComponent: null,
          filterService: Em.Object.create({serviceName: 'S1'}),
          showOnlySelectedDefs: false
        },
        result: [true]
      },
      {
        title: 'fail to be filtered by serviceName',
        data: {
          defs: [
            Em.Object.create({
              filtered: false,
              serviceName: 'S1'
            })
          ],
          filterComponent: null,
          filterService: Em.Object.create({serviceName: 'S2'}),
          showOnlySelectedDefs: false
        },
        result: [false]
      },
      {
        title: 'should be filtered by componentName',
        data: {
          defs: [
            Em.Object.create({
              filtered: false,
              componentName: 'C1'
            })
          ],
          filterComponent: Em.Object.create({componentName: 'C1'}),
          filterService: null,
          showOnlySelectedDefs: false
        },
        result: [true]
      },
      {
        title: 'fail to be filtered by componentName',
        data: {
          defs: [
            Em.Object.create({
              filtered: false,
              componentName: 'C1'
            })
          ],
          filterComponent: Em.Object.create({componentName: 'C2'}),
          filterService: null,
          showOnlySelectedDefs: false
        },
        result: [false]
      },
      {
        title: 'should be filtered by showOnlySelectedDefs',
        data: {
          defs: [
            Em.Object.create({
              filtered: false,
              selected: true
            })
          ],
          filterComponent: null,
          filterService: null,
          showOnlySelectedDefs: true
        },
        result: [true]
      },
      {
        title: 'fail to be filtered by showOnlySelectedDefs',
        data: {
          defs: [
            Em.Object.create({
              filtered: false,
              selected: false
            })
          ],
          filterComponent: null,
          filterService: null,
          showOnlySelectedDefs: true
        },
        result: [false]
      },
      {
        title: 'should be filtered by all filters',
        data: {
          defs: [
            Em.Object.create({
              filtered: false,
              componentName: 'C1',
              serviceName: 'S1',
              selected: true
            })
          ],
          filterComponent: Em.Object.create({componentName: 'C1'}),
          filterService: Em.Object.create({serviceName: 'S1'}),
          showOnlySelectedDefs: true
        },
        result: [true]
      }
    ];
    testCases.forEach(function (test) {
      describe(test.title, function () {

        beforeEach(function () {
          view.set('parentView.availableDefs', test.data.defs);
          view.set('showOnlySelectedDefs', test.data.showOnlySelectedDefs);
          view.set('filterComponent', test.data.filterComponent);
          view.set('filterService', test.data.filterService);

          view.filterDefs();
        });

        it('availableDefs.@each.filtered is ' + test.result, function () {
          expect(view.get('parentView.availableDefs').mapProperty('filtered')).to.eql(test.result);
        });

        it('startIndex is 1', function () {
          expect(view.get('startIndex')).to.equal(1);
        });

      });
    });
  });

  describe("#defSelectMessage", function () {
    beforeEach(function () {
      sinon.stub(view, 'filterDefs', Em.K);
    });
    afterEach(function () {
      view.filterDefs.restore();
    });
    it("is formatted with parentView.availableDefs", function () {
      view.set('parentView.availableDefs', [
        {selected: true},
        {selected: false}
      ]);
      expect(view.get('defSelectMessage')).to.equal(Em.I18n.t('alerts.actions.manage_alert_groups_popup.selectDefsDialog.selectedDefsLink').format(1, 2));
    });
  });

  describe("#selectFilterComponent()", function() {
    beforeEach(function () {
      sinon.stub(view, 'filterDefs', Em.K);
    });
    afterEach(function () {
      view.filterDefs.restore();
    });

    it("event is null", function() {
      view.set('filterComponent', null);
      view.selectFilterComponent(null);
      expect(view.get('filterComponent')).to.be.null;
    });
    it("componentName is empty", function() {
      view.set('filterComponent', null);
      view.selectFilterComponent({context: Em.Object.create({componentName: ""})});
      expect(view.get('filterComponent')).to.be.null;
    });
    it("filterComponent is null", function() {
      var context = Em.Object.create({componentName: "C1"});
      view.set('filterComponent', null);
      view.selectFilterComponent({context: context});
      expect(view.get('filterComponent')).to.eql(context);
      expect(view.get('filterComponent.selected')).to.be.true;
    });
    it("filterComponent exist", function() {
      var context = Em.Object.create({componentName: "C1"});
      var filterComponent = Em.Object.create();
      view.set('filterComponent', filterComponent);
      view.selectFilterComponent({context: context});
      expect(view.get('filterComponent')).to.eql(context);
      expect(view.get('filterComponent.selected')).to.be.true;
      expect(filterComponent.get('selected')).to.be.false;
    });
    it("the same filterComponent selected", function() {
      var context = Em.Object.create({componentName: "C1"});
      var filterComponent = Em.Object.create({componentName: 'C1'});
      view.set('filterComponent', filterComponent);
      view.selectFilterComponent({context: context});
      expect(view.get('filterComponent')).to.be.null;
      expect(filterComponent.get('selected')).to.be.false;
    });
  });

  describe("#selectFilterService()", function() {
    beforeEach(function () {
      sinon.stub(view, 'filterDefs', Em.K);
    });
    afterEach(function () {
      view.filterDefs.restore();
    });

    it("event is null", function() {
      view.set('filterService', null);
      view.selectFilterService(null);
      expect(view.get('filterService')).to.be.null;
    });
    it("serviceName is empty", function() {
      view.set('filterService', null);
      view.selectFilterService({context: Em.Object.create({serviceName: ""})});
      expect(view.get('filterService')).to.be.null;
    });
    it("filterService is null", function() {
      var context = Em.Object.create({serviceName: "C1"});
      view.set('filterService', null);
      view.selectFilterService({context: context});
      expect(view.get('filterService')).to.eql(context);
      expect(view.get('filterService.selected')).to.be.true;
    });
    it("filterService exist", function() {
      var context = Em.Object.create({serviceName: "C1"});
      var filterService = Em.Object.create();
      view.set('filterService', filterService);
      view.selectFilterService({context: context});
      expect(view.get('filterService')).to.eql(context);
      expect(view.get('filterService.selected')).to.be.true;
      expect(filterService.get('selected')).to.be.false;
    });
    it("the same filterService selected", function() {
      var context = Em.Object.create({serviceName: "C1"});
      var filterService = Em.Object.create({serviceName: 'C1'});
      view.set('filterService', filterService);
      view.selectFilterService({context: context});
      expect(view.get('filterService')).to.be.null;
      expect(filterService.get('selected')).to.be.false;
    });
  });

  describe("#toggleSelectAllDefs()", function() {
    beforeEach(function () {
      sinon.stub(view, 'filterDefs', Em.K);
    });
    afterEach(function () {
      view.filterDefs.restore();
    });

    it("allDefsSelected is false", function() {
      view.set('parentView.availableDefs', [
        Em.Object.create({filtered: true, selected: true}),
        Em.Object.create({filtered: false, selected: false})
      ]);
      view.set('allDefsSelected', false);
      view.toggleSelectAllDefs();
      expect(view.get('parentView.availableDefs').mapProperty('selected')).to.eql([false, false]);
    });
    it("allDefsSelected is true", function() {
      view.set('parentView.availableDefs', [
        Em.Object.create({filtered: false, selected: true}),
        Em.Object.create({filtered: true, selected: false})
      ]);
      view.set('allDefsSelected', true);
      view.toggleSelectAllDefs();
      expect(view.get('parentView.availableDefs').mapProperty('selected')).to.eql([true, true]);
    });
  });

  describe("#toggleShowSelectedDefs()", function() {
    var filterComponent;
    var filterService;
    beforeEach(function () {
      sinon.stub(view, 'filterDefs', Em.K);
      view.set('showOnlySelectedDefs', true);
      filterComponent = Em.Object.create();
      filterService = Em.Object.create();
      view.set('filterComponent', filterComponent);
      view.set('filterService', filterService);
      view.toggleShowSelectedDefs();
    });
    afterEach(function () {
      view.filterDefs.restore();
    });

    it("filterComponent.selected is false", function() {
      expect(filterComponent.get('selected')).to.be.false;
    });

    it("filterService.selected is false", function() {
      expect(filterService.get('selected')).to.be.false;
    });

    it("filterComponent is null", function() {
      expect(view.get('filterComponent')).to.be.null;
    });

    it("filterService is null", function() {
      expect(view.get('filterService')).to.be.null;
    });

    it("showOnlySelectedDefs is false", function() {
      expect(view.get('showOnlySelectedDefs')).to.be.false;
    });
  });
});
