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

var lazyLoading = require('utils/lazy_loading');

App.ConfigCategoryContainerView = Em.ContainerView.extend(App.LoadingOverlaySupport, {

  lazyLoading: null,

  categories: [],

  classNames: ['accordion'],

  fieldToObserve: 'controller.recommendationsInProgress',

  handleFieldChangesOnDidInsert: false,

  didInsertElement: function () {
    this.pushViews();
    this._super();
  },

  willDestroyElement: function () {
    if (this.get('lazyLoading')) {
      lazyLoading.terminate(this.get('lazyLoading'));
    }
    this._super();
  },

  /**
   * extra calls for <code>handleFieldChanges</code>:
   * <ul>
   *  <li>child views are pushed to this (lazy loading is used)</li>
   *  <li>tab is switched</li>
   * </ul>
   */
  checkOverlay: function () {
    this.handleFieldChanges();
  }.observes('isLoaded', 'controller.activeTab.id', 'controller.activeTab.isRendered'),

  pushViews: function () {
    var self = this;
    // Create view with loading-overlay. For some unknown reasons overlay can't be added to the container view
    var categoriesViews = [
      Em.View.extend({
        template: Em.Handlebars.compile('<div class="loading-overlay"></div>')
      }).create()
    ];
    var categories = this.get('categories');
    if (!categories) return;
    categories.forEach(function (category) {
      var viewClass = category.isCustomView ? category.customView : App.ServiceConfigsByCategoryView;
      categoriesViews.push(viewClass.create({
        category: category,
        canEdit: self.get("canEdit"),
        service: self.get("service"),
        serviceConfigs: self.get("serviceConfigs"),
        supportsHostOverrides: self.get("supportsHostOverrides")
      }));
    });
    this.set('lazyLoading', lazyLoading.run({
      destination: self.get('childViews'),
      source: categoriesViews,
      initSize: 3,
      chunkSize: 3,
      delay: 200,
      context: this
    }));
  }

});
