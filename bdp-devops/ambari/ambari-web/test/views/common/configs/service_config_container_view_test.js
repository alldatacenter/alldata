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
require('views/common/chart/pie');
require('views/common/configs/services_config');

describe('App.ServiceConfigContainerView', function () {

  var view;

  beforeEach(function () {
    view = App.ServiceConfigContainerView.create({
      filter: ''
    });
  });

  describe('#pushView', function () {
    it('shouldn\'t be launched before selectedService is set', function () {
      view.set('controller', {});
      view.pushView();
      expect(view.get('childViews')).to.be.empty;
    });
  });

  describe('#selectedServiceObserver', function () {

    it('should add a child view', function () {
      view.set('controller', Em.Object.create({
        selectedService: {
          configCategories: [],
          configs: []
        },
        isRecommendedLoaded: true
      }));
      expect(view.get('childViews')).to.have.length(1);
    });

    it('should set controller for the view', function () {
      view.set('controller', Em.Object.create({
        name: 'controller',
        selectedService: {
          configCategories: [],
          configs: []
        },
        isRecommendedLoaded: true
      }));
      expect(view.get('childViews.firstObject.controller.name')).to.equal('controller');
    });

    it('should add config categories', function () {
      view.set('controller', Em.Object.create({
        selectedService: {
          configCategories: [Em.Object.create(), Em.Object.create()],
          configs: []
        },
        isRecommendedLoaded: true
      }));
      expect(view.get('childViews.firstObject.serviceConfigsByCategoryView.childViews')).to.have.length(2);
    });

  });

});
