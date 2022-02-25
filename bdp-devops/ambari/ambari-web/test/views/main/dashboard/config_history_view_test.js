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
require('views/main/dashboard/config_history_view');
require('utils/load_timer');

describe('App.MainConfigHistoryView', function() {
  var view = App.MainConfigHistoryView.create({
    totalCount: 0,
    filteredCount: 0
  });
  view.reopen({
    controller: Em.Object.create({
      name: 'mainConfigHistoryController11',
      paginationProps: [
        {
          name: 'displayLength'
        },
        {
          name: 'startIndex'
        }
      ],
      subscribeToUpdates: Em.K,
      unsubscribeOfUpdates: Em.K,
      load: function () {
        return {done: Em.K};
      },
      colPropAssoc: []
    })
  });
  view.removeObserver('controller.resetStartIndex', view, 'resetStartIndex');

  describe("#filteredContentInfo", function () {
    it("is formatted with filteredCount and totalCount", function () {
      view.set('filteredCount', 1);
      view.set('totalCount', 2);
      view.propertyDidChange('filteredContentInfo');
      expect(view.get('filteredContentInfo')).to.eql(Em.I18n.t('tableView.filters.filteredConfigVersionInfo').format(1, 2));
    });
  });

  describe("#ConfigVersionView", function () {
    var subView;
    before(function () {
      subView = view.get('ConfigVersionView').create({
        parentView: view
      });

      sinon.stub(App, 'tooltip', Em.K);
    });
    after(function () {
      App.tooltip.restore();
    });
    it("call didInsertElement()", function () {
      subView.didInsertElement();
      expect(App.tooltip.calledOnce).to.be.true;
    });
    it("call toggleShowLessStatus()", function () {
      subView.set('showLessNotes', true);
      subView.toggleShowLessStatus();
      expect(subView.get('showLessNotes')).to.be.false;
    });

    describe("#isServiceLinkDisable", function () {
      beforeEach(function () {
        subView.set('content', Em.Object.create());
        this.hasKerberos = sinon.stub(App.Service, 'find');
      });
      afterEach(function () {
        App.Service.find.restore();
      });
      it("should be true for deleted kerberos groups", function () {
        subView.set('content.serviceName', 'KERBEROS');
        this.hasKerberos.returns([]);
        expect(subView.get('isServiceLinkDisabled')).to.be.true;
      });
      it("should be false for deleted kerberos groups", function () {
        subView.set('content.serviceName', 'KERBEROS');
        subView.set('content.isConfigGroupDeleted', false);
        this.hasKerberos.returns([{serviceName: 'KERBEROS'}]);
        expect(subView.get('isServiceLinkDisabled')).to.be.false;
      });
      it("should be true if group is deleted", function () {
        subView.set('content.serviceName', 'KERBEROS');
        subView.set('content.isConfigGroupDeleted', true);
        this.hasKerberos.returns([{serviceName: 'KERBEROS'}]);
        expect(subView.get('isServiceLinkDisabled')).to.be.true;
      });
    });
  });

  describe('#didInsertElement()', function() {

    beforeEach(function () {
      sinon.stub(view, 'addObserver', Em.K);
      sinon.stub(view.get('controller'), 'subscribeToUpdates');
      view.didInsertElement();
    });

    afterEach(function () {
      view.get('controller').subscribeToUpdates.restore();
      view.addObserver.restore();
    });

    it('addObserver is called twice', function() {
      expect(view.addObserver.calledTwice).to.be.true;
    });

    it('isInitialRendering is true', function() {
      expect(view.get('isInitialRendering')).to.be.true;
    });

    it('subscribeToUpdates should be called', function() {
      expect(view.get('controller').subscribeToUpdates.calledOnce).to.be.true;
    });
  });

  describe('#willDestroyElement()', function() {
    beforeEach(function () {
      sinon.stub(view.get('controller'), 'unsubscribeOfUpdates');
    });
    afterEach(function () {
      view.get('controller').unsubscribeOfUpdates.restore();
    });

    it('unsubscribeOfUpdates should be called', function() {
      view.willDestroyElement();
      expect(view.get('controller').unsubscribeOfUpdates.calledOnce).to.be.true;
    });
  });

  describe('#refresh()', function() {

    beforeEach(function () {
      sinon.spy(view.get('controller'), 'load');
      view.refresh();
    });

    afterEach(function () {
      view.get('controller').load.restore();
    });

    it('filteringComplete is false', function() {
      expect(view.get('filteringComplete')).to.be.false;
    });

    it('controller.load is called once', function() {
      expect(view.get('controller').load.calledOnce).to.be.true;
    });
  });

  describe("#refreshDone()", function () {
    beforeEach(function () {
      sinon.stub(view, 'propertyDidChange', Em.K);
      view.set('filteringComplete', false);
      view.set('controller.resetStartIndex', true);
      view.refreshDone();
    });
    afterEach(function () {
      view.propertyDidChange.restore();
    });
    it("filteringComplete is true", function () {
      expect(view.get('filteringComplete')).to.be.true;
    });
    it("controller.resetStartIndex is false", function () {
      expect(view.get('controller.resetStartIndex')).to.be.false;
    });
  });
});
