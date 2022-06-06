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

App.ServiceConfigContainerView = Em.ContainerView.extend({

  view: null,

  lazyLoading: null,
  
  pushViewTimeout: null,

  didInsertElement: function () {
    if (this.get('controller.isInstallWizard')) {
      this.selectedServiceObserver();
    }
  },

  pushView: function () {
    if (this.get('controller.selectedService')) {
      var self = this;
      var controllerRoute = 'App.router.' + this.get('controller.name');
      if (!this.get('view')) {
        this.get('childViews').pushObject(App.ServiceConfigView.create({
          templateName: require('templates/common/configs/service_config_wizard'),
          controllerBinding: controllerRoute,
          isNotEditableBinding: controllerRoute + '.isNotEditable',
          filterBinding: controllerRoute + '.filter',
          columnsBinding: controllerRoute + '.filterColumns',
          selectedServiceBinding: controllerRoute + '.selectedService',
          actionsStacked: function () {
            return this.get('controller.isInstallWizard') && this.get('supportsConfigLayout');
          }.property('controller.isInstallWizard', 'supportsConfigLayout'),
          serviceConfigsByCategoryView: Em.ContainerView.create(),
          willDestroyElement: function () {
            $('.loading').append(Em.I18n.t('common.loading.eclipses'));
          },
          didInsertElement: function () {
            $('.loading').empty();
            this._super();
          },
          setActiveTab: function (event) {
            if (event.context.get('isHiddenByFilter')) return false;
            this.get('tabs').forEach(function (tab) {
              tab.set('isActive', false);
            });
            var currentTab = event.context;
            currentTab.set('isActive', true);
          }
        }));
      }
      else {
        this.get('childViews').pushObject(this.get('view'));
      }
      var categoriesToPush = [];
      this.get('controller.selectedService.configCategories').forEach(function (item) {

        var categoryView = item.get('isCustomView') ? item.get('customView') : App.ServiceConfigsByCategoryView;
        if (categoryView !== null) {
          categoriesToPush.pushObject(categoryView.extend({
            category: item,
            controllerBinding: controllerRoute,
            canEditBinding: 'parentView.canEdit',
            serviceBinding: controllerRoute + '.selectedService',
            serviceConfigsBinding: controllerRoute + '.selectedService.configs',
            supportsHostOverridesBinding: 'parentView.supportsHostOverrides'
          }));
        }
      });
      this.set('lazyLoading', lazyLoading.run({
        destination: self.get('childViews.lastObject.serviceConfigsByCategoryView.childViews'),
        source: categoriesToPush,
        initSize: 3,
        chunkSize: 3,
        delay: 200,
        context: this
      }));
    }
  },

  selectedServiceObserver: function () {
    if (this.get('childViews.length')) {
      var view = this.get('childViews.firstObject');
      if (view.get('serviceConfigsByCategoryView.childViews.length')) {
        view.get('serviceConfigsByCategoryView.childViews').clear();
      }
      view.removeFromParent();
      this.set('view', view);
    }
    //terminate lazy loading when switch service
    if (this.get('lazyLoading')) lazyLoading.terminate(this.get('lazyLoading'));
    this.pushViewAfterRecommendation();
  }.observes('controller.selectedService', 'controller.selectedService.redrawConfigs'),

  pushViewAfterRecommendation: function() {
    if (this.get('controller.isRecommendedLoaded')) {
      this.pushView();
    } else {
      clearTimeout(this.get('pushViewTimeout'));
      this.set('pushViewTimeout', setTimeout(() => this.pushViewAfterRecommendation(), 300));
    }
  }

});
