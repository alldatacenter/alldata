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

function getView() {
  return App.MainHostStackVersionsView.create({
    filteredCount: 0,
    totalCount: 0
  });
}
var view;
describe('App.MainHostStackVersionsView', function() {

  beforeEach(function () {
    view = getView();
  });

  App.TestAliases.testAsComputedAlias(getView(), 'host', 'App.router.mainHostDetailsController.content', 'object');

  App.TestAliases.testAsComputedFilterBy(getView(), 'content', 'host.stackVersions', 'isVisible', true);

  describe("#filteredContentInfo", function () {
    it("formatted with filteredCount and totalCount", function () {
      view.set('filteredCount', 1);
      view.set('totalCount', 2);
      view.propertyDidChange('filteredContentInfo');
      expect(view.get('filteredContentInfo')).to.eql(Em.I18n.t('hosts.host.stackVersions.table.filteredInfo').format(1, 2));
    });
  });

  describe("#showInstallProgress()", function () {
    var mock = {
      showProgressPopup: Em.K
    };

    beforeEach(function() {
      sinon.stub(App.router, 'get').returns(mock);
      sinon.stub(mock, 'showProgressPopup');
    });

    afterEach(function() {
      App.router.get.restore();
      mock.showProgressPopup.restore();
    });

    it("showProgressPopup should be called", function() {
      view.showInstallProgress({context: {}});
      expect(mock.showProgressPopup.calledWith({})).to.be.true;
    });
  });

  describe("#outOfSyncInfo", function () {
    var outOfSyncInfo;

    beforeEach(function() {
      outOfSyncInfo = view.get('outOfSyncInfo').create()
    });

    describe("#didInsertElement()", function () {

      beforeEach(function() {
        sinon.stub(App, 'tooltip');
      });

      afterEach(function() {
        App.tooltip.restore();
      });

      it("App.tooltip should be called", function() {
        outOfSyncInfo.didInsertElement();
        expect(App.tooltip.calledOnce).to.be.true;
      });
    });

    describe("#willDestroyElement()", function () {
      var mock = {
        tooltip: Em.K
      };

      beforeEach(function() {
        sinon.stub(window, '$').returns(mock);
        sinon.spy(mock, 'tooltip');
      });

      afterEach(function() {
        window.$.restore();
        mock.tooltip.restore();
      });

      it("tooltip should be destroyed", function() {
        outOfSyncInfo.willDestroyElement();
        expect(mock.tooltip.calledWith('destroy')).to.be.true;
      });
    });
  });
});
