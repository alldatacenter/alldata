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
require('utils/db');
require('views/common/filter_view');
require('views/common/sort_view');
require('mixins');
require('mixins/common/persist');
require('views/common/table_view');

function getView() {
  return App.TableView.create({
    controller: Em.Object.create()
  });
}

describe('App.TableView', function () {

  var view;

  beforeEach(function() {
    App.db.cleanUp();
    sinon.stub(App.db, 'setFilterConditions', Em.K);
  });

  afterEach(function() {
    App.db.cleanUp();
    App.db.setFilterConditions.restore();
  });

  App.TestAliases.testAsComputedAlias(getView(), 'filteredCount', 'filteredContent.length', 'number');

  App.TestAliases.testAsComputedAlias(getView(), 'totalCount', 'content.length', 'number');

  describe('#init', function() {

    it('should set filterConditions on instance', function() {
      var tableView = App.TableView.create();
      expect(tableView.get('filterConditions')).to.be.not.equal(App.TableView.prototype.filterConditions);
    });

  });

  describe('#updatePaging', function() {

    beforeEach(function() {
      view = App.TableView.create(App.Persist, {
        controller: Em.Object.create({}),
        displayLength: 10,
        startIndex: 1,
        content: d3.range(1, 100),
        filteredContent: d3.range(1, 100),
        filtersUsedCalc: function() {},
        filter: function() {}
      });
      view.clearFilters();
      view.updateFilter();
    });

    it('should set "startIndex" to 0 if "filteredContent" is empty', function() {
      view.set('filteredContent', []);
      expect(view.get('startIndex')).to.equal(0);
    });

    it('should set "startIndex" to 1 if "filteredContent" is not empty', function() {
      view.set('filteredContent', d3.range(1, 10));
      expect(view.get('startIndex')).to.equal(1);
    });

  });

  describe('#endIndex', function() {

    beforeEach(function() {
      view = App.TableView.create(App.Persist, {
        controller: Em.Object.create({}),
        displayLength: 10,
        startIndex: 1,
        content: d3.range(1, 100),
        filteredContent: d3.range(1, 100),
        filtersUsedCalc: function() {},
        filter: function() {}
      });
      view.clearFilters();
      view.updateFilter();
    });

    it('should be recalculated if "startIndex" was changed', function() {
      view.set('startIndex', 2);
      expect(view.get('endIndex')).to.equal(11);
    });

    it('should be recalculated if "displayLength" was changed', function() {
      view.set('displayLength', 5);
      expect(view.get('endIndex')).to.equal(5);
    });

    it('should be recalculated (but not changed) if "filteredContent" was changed (and "filterContent.length" is more than "startIndex + displayLength")', function() {
      var endIndexBefore = view.get('endIndex');
      view.set('filteredContent', d3.range(2,100));
      expect(view.get('endIndex')).to.equal(endIndexBefore);
    });

    it('should be recalculated (and changed) if "filteredContent" was changed (and "filterContent.length" is less than "startIndex + displayLength")', function() {
      var endIndexBefore = view.get('endIndex');
      var indx = 4;
      view.set('filteredContent', d3.range(1,indx));
      expect(view.get('endIndex')).to.not.equal(endIndexBefore);
      expect(view.get('endIndex')).to.equal(indx - 1);
    });

  });

  describe('#pageContent', function() {

    beforeEach(function() {
      view = App.TableView.create(App.Persist, {
        controller: Em.Object.create({}),
        displayLength: 10,
        startIndex: 1,
        content: d3.range(1, 100),
        filteredContent: d3.range(1, 100),
        endIndex: 10,
        filtersUsedCalc: function() {},
        filter: function() {}
      });
      view.clearFilters();
      view.updateFilter();
    });

    it('should be recalculated if "startIndex" was changed', function() {
      view.set('startIndex', 2);
      expect(view.get('pageContent').length).to.equal(9);
    });

    it('should be recalculated if "endIndex" was changed', function() {
      view.set('endIndex', 5);
      expect(view.get('pageContent').length).to.equal(5);
    });

    it('should be recalculated if "filteredContent" was changed', function() {
      var pageContentBefore = view.get('pageContent');
      view.set('filteredContent', d3.range(2,100));
      expect(view.get('pageContent').length).to.equal(pageContentBefore.length);
      expect(view.get('pageContent')).to.not.eql(pageContentBefore);
    });

  });

  describe('#clearFilters', function() {

    it('should set "filterConditions" to empty array', function() {
      view.clearFilters();
      expect(view.get('filterConditions')).to.eql([]);
    });

  });

  describe('#filtersUsedCalc', function() {

    beforeEach(function() {
      view = App.TableView.create(App.Persist, {
        controller: Em.Object.create({}),
        displayLength: 10,
        startIndex: 1,
        content: d3.range(1, 100),
        filteredContent: d3.range(1, 100),
        endIndex: 10,
        filter: function() {}
      });
    });

    it('should set "filtersUsed" to false if "filterConditions" is empty array', function() {
      view.set('filterConditions', []);
      view.filtersUsedCalc();
      expect(view.get('filtersUsed')).to.equal(false);
    });

    it('should set "filtersUsed" to false if each value in "filterConditions" is empty', function() {
      view.set('filterConditions', [{value:''}, {value:''}]);
      view.filtersUsedCalc();
      expect(view.get('filtersUsed')).to.equal(false);
    });

    it('should set "filtersUsed" to true if one or more values in "filterConditions" are not empty', function() {
      view.set('filterConditions', [{value:''}, {value:'lol'}]);
      view.filtersUsedCalc();
      expect(view.get('filtersUsed')).to.equal(true);
    });

  });

  describe('#nextPage', function() {

    beforeEach(function() {
      view = App.TableView.create(App.Persist, {
        controller: Em.Object.create({}),
        displayLength: 10,
        startIndex: 1,
        content: d3.range(1, 100),
        filteredContent: d3.range(1, 100),
        endIndex: 10,
        filter: function() {}
      });
    });

    it('should set "startIndex" if "filteredContent.length is greater than "startIndex" + "displayLength"', function() {
      var oldStartIndex = view.get('startIndex');
      var displayLength = 50;
      view.set('displayLength', displayLength);
      view.nextPage();
      expect(view.get('startIndex')).to.equal(oldStartIndex + displayLength);
    });

    it('should not set "startIndex" if "filteredContent.length is equal to "startIndex" + "displayLength"', function() {
      var oldStartIndex = view.get('startIndex');
      var displayLength = 99;
      view.set('displayLength', displayLength);
      view.nextPage();
      expect(view.get('startIndex')).to.equal(oldStartIndex);
    });

    it('should not set "startIndex" if "filteredContent.length is less than "startIndex" + "displayLength"', function() {
      var oldStartIndex = view.get('startIndex');
      var displayLength = 100;
      view.set('displayLength', displayLength);
      view.nextPage();
      expect(view.get('startIndex')).to.equal(oldStartIndex);
    });

  });

  describe('#previousPage', function() {

    beforeEach(function() {
      view = App.TableView.create(App.Persist, {
        controller: Em.Object.create({}),
        displayLength: 10,
        startIndex: 50,
        content: d3.range(1, 100),
        filteredContent: d3.range(1, 100),
        endIndex: 60,
        filter: function() {}
      });
    });

    it('should set "startIndex" to 1', function() {
      var displayLength = 50;
      view.set('displayLength', displayLength);
      view.previousPage();
      expect(view.get('startIndex')).to.equal(1);
    });

    it('should not set "startIndex" to 40', function() {
      view.set('startIndex', 50);
      var displayLength = 10;
      view.set('displayLength', displayLength);
      view.previousPage();
      expect(view.get('startIndex')).to.equal(40);
    });

  });

  describe("#showFilteredContent", function() {
    beforeEach(function() {
      view = App.TableView.create({});
    });

    it('hide clear filters link', function () {
      view.set('filterConditions', []);
      expect(view.get('showFilteredContent')).to.be.false;
    });

    it('shows clear filters link', function () {
      view.set('filterConditions', [{value: "1"}]);
      expect(view.get('showFilteredContent')).to.be.true;
    });

    it('shows clear filters link for array filter', function () {
      view.set('filterConditions', [{value: ["1", "2"]}]);
      expect(view.get('showFilteredContent')).to.be.true;
    });
  });

  describe('#filter', function () {

    var cases = [
      {
        filterConditions: [
          {
            iColumn: 1,
            type: 'string',
            value: 'v0'
          }
        ],
        content: [
          Em.Object.create({
            c0: 'v0'
          }),
          Em.Object.create({
            c1: 'v1'
          })
        ],
        filteredContent: [],
        title: 'no matches'
      },
      {
        filterConditions: [
          {
            iColumn: 0,
            type: 'string',
            value: 'v1'
          }
        ],
        content: [
          Em.Object.create({
            c0: 'v1'
          }),
          Em.Object.create({
            c0: 'v11'
          }),
          Em.Object.create({
            c1: 'v01'
          })
        ],
        filteredContent: [
          Em.Object.create({
            c0: 'v1'
          }),
          Em.Object.create({
            c0: 'v11'
          })
        ],
        title: 'matches present'
      },
      {
        filterConditions: [],
        content: [
          Em.Object.create({
            c0: 'v0'
          }),
          Em.Object.create({
            c1: 'v1'
          })
        ],
        filteredContent: [
          Em.Object.create({
            c0: 'v0'
          }),
          Em.Object.create({
            c1: 'v1'
          })
        ],
        title: 'no filter conditions'
      },
      {
        filterConditions: [],
        filteredContent: [],
        title: 'no filter conditions, no content'
      }
    ];

    beforeEach(function () {
      view = App.TableView.create({
        colPropAssoc: ['c0', 'c1']
      });
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        view.setProperties({
          filterConditions: item.filterConditions,
          content: item.content
        });
        view.filter();
        expect(view.get('filteredContent')).to.eql(item.filteredContent);
      });
    });

  });

  describe('#clearStartIndex', function() {

    beforeEach(function() {
      view = getView();
    });

    it('should reset start index', function() {
      view.set('controller.resetStartIndex', false);
      view.set('controller.startIndex', 11);
      expect(view.clearStartIndex()).to.be.true;
      expect(view.get('controller.resetStartIndex')).to.be.true;
    });

    it('should not reset start index', function() {
      view.set('controller.resetStartIndex', false);
      view.set('controller.startIndex', 1);
      expect(view.clearStartIndex()).to.be.false;
      expect(view.get('controller.resetStartIndex')).to.be.false;
    });
  });

});
