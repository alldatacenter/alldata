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
require('views/main/service/info/menu');


describe('App.MainServiceInfoMenuView', function () {
  var view;

  beforeEach(function () {
    view = App.MainServiceInfoMenuView.create({
      controller: Em.Object.create()
    });
  });

  describe("#content", function() {

    it("heatmapTab and configTab are false", function() {
      view.setProperties({
        configTab: false,
        heatmapTab: false
      });
      view.propertyDidChange('content');
      expect(view.get('content').mapProperty('id')).to.eql(['summary-service-tab']);
    });

    it("heatmapTab - false, configTab - true", function() {
      view.setProperties({
        configTab: true,
        heatmapTab: false
      });
      view.propertyDidChange('content');
      expect(view.get('content').mapProperty('id')).to.eql(['summary-service-tab', 'configs-service-tab']);
    });

    it("heatmapTab - true, configTab - false", function() {
      view.setProperties({
        configTab: false,
        heatmapTab: true
      });
      view.propertyDidChange('content');
      expect(view.get('content').mapProperty('id')).to.eql(['summary-service-tab', 'heatmap-service-tab']);
    });

    it("heatmapTab - true, configTab - true", function() {
      view.setProperties({
        configTab: true,
        heatmapTab: true
      });
      view.propertyDidChange('content');
      expect(view.get('content').mapProperty('id')).to.eql(['summary-service-tab', 'heatmap-service-tab', 'configs-service-tab']);
    });
  });

  describe("#activateView()", function() {
    it("_childViews should be active", function() {
      view.set('_childViews', [
        Em.Object.create({active: '', content: {routing: 'login'}})
      ]);
      view.activateView();
      expect(view.get('_childViews')[0].get('active')).to.equal('active');
    });
  });

  describe("#deactivateChildViews()", function() {
    it("_childViews should be deactivated", function() {
      view.set('_childViews', [
        Em.Object.create({active: 'active'}),
        Em.Object.create({active: 'active'})
      ]);
      view.deactivateChildViews();
      expect(view.get('_childViews')[0].get('active')).to.be.empty;
      expect(view.get('_childViews')[1].get('active')).to.be.empty;
    });
  });
});
