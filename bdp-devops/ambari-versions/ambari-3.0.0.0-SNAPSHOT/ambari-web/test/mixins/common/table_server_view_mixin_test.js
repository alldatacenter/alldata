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
require('mixins/common/table_server_view_mixin');
require('utils/load_timer');

describe('App.MainConfigHistoryView', function() {
  var view = Em.View.create(App.TableServerViewMixin, {
    filteredCount: 0,
    totalCount: 0,
    content: [],
    filteredContent: [],
    refresh: Em.K,
    saveFilterConditions: Em.K,
    controller: Em.Object.create({
      name: 'mainConfigHistoryController',
      paginationProps: [
        {
          name: 'displayLength',
          value: '25'
        },
        {
          name: 'startIndex',
          value: 0
        }
      ]
    })
  });

  describe('#pageContent', function() {
    beforeEach(function(){
      view.propertyDidChange('pageContent');
    });
    it('filtered content is empty', function() {
      view.set('filteredContent', []);
      expect(view.get('pageContent')).to.be.empty;
    });
    it('filtered content contain one item', function() {
      view.set('filteredCount', 1);
      view.set('filteredContent', [Em.Object.create()]);

      expect(view.get('pageContent')).to.eql([Em.Object.create()]);
    });
    it('filtered content contain two unsorted items', function() {
      view.set('filteredCount', 2);
      view.set('filteredContent', [
        Em.Object.create({index:2}),
        Em.Object.create({index:1})
      ]);

      expect(view.get('pageContent')).to.eql([
        Em.Object.create({index:1}),
        Em.Object.create({index:2})
      ]);
    });
  });

  describe('#updatePagination', function() {
    beforeEach(function () {
      sinon.spy(view, 'refresh');
      sinon.stub(App.db, 'setDisplayLength', Em.K);
      sinon.stub(App.db, 'setStartIndex', Em.K);
    });
    afterEach(function () {
      view.refresh.restore();
      App.db.setStartIndex.restore();
      App.db.setDisplayLength.restore();
    });

    describe('displayLength is correct', function() {
      beforeEach(function () {
        view.set('displayLength', '50');
        view.set('startIndex', null);
        view.updatePagination();
      });

      it('refresh is called once', function () {
        expect(view.refresh.calledOnce).to.be.true;
      });
      it('setStartIndex is called once', function () {
        expect(App.db.setStartIndex.called).to.be.false;
      });
      it('setDisplayLength is called with correct arguments', function () {
        expect(App.db.setDisplayLength.calledWith('mainConfigHistoryController', '50')).to.be.true;
      });
    });

    describe('startIndex is correct', function() {

      beforeEach(function () {
        view.set('displayLength', null);
        view.set('startIndex', 10);
        view.updatePagination();
      });
      it('refresh is called once', function () {
        expect(view.refresh.calledOnce).to.be.true;
      });
      it('setStartIndex is called with valid arguments', function () {
        expect(App.db.setStartIndex.calledWith('mainConfigHistoryController', 10)).to.be.true;
      });
      it('setDisplayLength is not called', function () {
        expect(App.db.setDisplayLength.called).to.be.false;
      });
    });

    describe('displayLength and startIndex are correct', function() {
      beforeEach(function () {
        view.set('displayLength', '100');
        view.set('startIndex', 20);
        view.updatePagination();
      });

      it('refresh is called once', function () {
        expect(view.refresh.calledOnce).to.be.true;
      });
      it('setStartIndex is called with valid arguments', function () {
        expect(App.db.setStartIndex.calledWith('mainConfigHistoryController', 20)).to.be.true;
      });
      it('setDisplayLength is called with valid arguments', function () {
        expect(App.db.setDisplayLength.calledWith('mainConfigHistoryController', '100')).to.be.true;
      });
    });

    describe('displayLength and startIndex are null', function() {
      beforeEach(function () {
        view.set('displayLength', null);
        view.set('startIndex', null);
        view.updatePagination();
      });
      it('refresh is called once', function () {
        expect(view.refresh.calledOnce).to.be.true;
      });
      it('setStartIndex is not called', function () {
      expect(App.db.setStartIndex.called).to.be.false;
      });
      it('setDisplayLength is not called', function () {
      expect(App.db.setDisplayLength.called).to.be.false;
      });
    });
  });

  describe('#updateFilter()', function() {
    beforeEach(function () {
      sinon.stub(view, 'saveFilterConditions', Em.K);
      sinon.stub(view, 'refresh', Em.K);
      sinon.stub(view, 'resetStartIndex');
      sinon.spy(view, 'updateFilter');
      this.clock = sinon.useFakeTimers();
    });
    afterEach(function () {
      view.saveFilterConditions.restore();
      view.updateFilter.restore();
      view.resetStartIndex.restore();
      view.refresh.restore();
      this.clock.restore();
    });
    it('filteringComplete is false', function() {


      view.set('filteringComplete', false);
      view.updateFilter(1, '1', 'string');
      expect(view.get('controller.resetStartIndex')).to.be.false;
      expect(view.saveFilterConditions.called).to.be.false;
      view.set('filteringComplete', true);
      this.clock.tick(view.get('filterWaitingTime'));
      expect(view.updateFilter.calledWith(1, '1', 'string')).to.be.true;

    });
    it('filteringComplete is true', function() {
      view.set('filteringComplete', true);

      view.updateFilter(1, '1', 'string');
      expect(view.get('controller.resetStartIndex')).to.be.true;
      expect(view.saveFilterConditions.calledWith(1, '1', 'string', false)).to.be.true;
      expect(view.refresh.calledOnce).to.be.true;
    });

    it('clear filters - refresh() clears timer', function () {

      //clear filters simulation
      view.set('filteringComplete', false);
      view.updateFilter(0, '', 'string');

      //filters cleared success
      view.updaterSuccessCb();

      //timeout in updateFilter() runs out
      this.clock.tick(view.get('filterWaitingTime'));

      //should not call update filter again
      expect(view.updateFilter.calledOnce).to.be.true;
    })
  });

  describe('#resetStartIndex()', function() {
    beforeEach(function () {
      sinon.stub(view, 'updatePagination');
      sinon.spy(view, 'saveStartIndex');
    });
    afterEach(function () {
      view.saveStartIndex.restore();
      view.updatePagination.restore();
    });
    it('resetStartIndex is false and filteredCount is 0', function() {
      view.set('filteredCount', 0);
      view.set('controller.resetStartIndex', false);
      view.set('startIndex', 0);
      view.resetStartIndex();
      expect(view.get('startIndex')).to.equal(0);
      expect(view.saveStartIndex.called).to.be.false;
      expect(view.updatePagination.called).to.be.false;
    });
    it('resetStartIndex is true and filteredCount is 0', function() {
      view.set('filteredCount', 0);
      view.set('controller.resetStartIndex', true);
      view.set('startIndex', 0);
      view.resetStartIndex();
      expect(view.get('startIndex')).to.equal(0);
      expect(view.saveStartIndex.called).to.be.false;
      expect(view.updatePagination.called).to.be.false;
    });
    it('resetStartIndex is false and filteredCount is 5', function() {
      view.set('filteredCount', 5);
      view.set('controller.resetStartIndex', false);
      view.set('startIndex', 0);
      view.resetStartIndex();
      expect(view.get('startIndex')).to.equal(0);
      expect(view.saveStartIndex.called).to.be.false;
      expect(view.updatePagination.called).to.be.false;
    });
    it('resetStartIndex is true and filteredCount is 5', function() {
      view.set('controller.resetStartIndex', true);
      view.set('filteredCount', 5);
      view.set('startIndex', 0);
      view.resetStartIndex();
      expect(view.get('startIndex')).to.equal(1);
      expect(view.saveStartIndex.called).to.be.true;
      expect(view.updatePagination.called).to.be.true;
    });
  });

  describe("#updaterSuccessCb()", function () {
    beforeEach(function () {
      sinon.stub(view, 'propertyDidChange');
      view.set('filteringComplete', false);
      view.updaterSuccessCb();
    });
    afterEach(function () {
      view.propertyDidChange.restore();
    });
    it('pageContent is forced to be recalculated', function () {
      expect(view.propertyDidChange.calledWith('pageContent')).to.be.true;
    });
    it('filteringComplete is updated', function () {
      expect(view.get('filteringComplete')).to.be.true;
    });
  });
});
