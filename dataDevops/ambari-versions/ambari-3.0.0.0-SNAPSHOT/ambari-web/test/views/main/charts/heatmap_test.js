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
require('views/main/charts/heatmap');

describe('App.MainChartsHeatmapView', function () {

  var view;

  beforeEach(function () {
    view = App.MainChartsHeatmapView.create({
      controller: Em.Object.create({
        clearActiveWidgetLayout: Em.K,
        loadPageData: Em.K
      })
    });
  });


  describe("#didInsertElement()", function () {
    beforeEach(function () {
      sinon.spy(view.get('controller'), 'loadPageData');
    });
    afterEach(function () {
      view.get('controller').loadPageData.restore();
    });
    it("loadPageData is called once", function () {
      view.didInsertElement();
      expect(view.get('controller').loadPageData.calledOnce).to.be.true;
    });
  });

  describe("#willDestroyElement()", function () {
    beforeEach(function () {
      sinon.spy(view.get('controller'), 'clearActiveWidgetLayout');
    });
    afterEach(function () {
      view.get('controller').clearActiveWidgetLayout.restore();
    });
    it("clearActiveWidgetLayout is called once", function () {
      view.willDestroyElement();
      expect(view.get('controller').clearActiveWidgetLayout.calledOnce).to.be.true;
    });
  });

});