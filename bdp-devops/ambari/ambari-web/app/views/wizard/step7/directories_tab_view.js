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

App.DirectoriesTabOnStep7View = Em.View.extend({

  templateName: require('templates/wizard/step7/directories_tab'),
  
  services: [],

  setServices: function () {
    var services = [];
    var directoriesTabs = App.Tab.find().filterProperty('themeName', 'directories');
    var stepConfigs = this.get('controller.stepConfigs');
    var lastSelectedService = this.get('controller.tabs').findProperty('isActive').get('selectedServiceName')
    stepConfigs.forEach(function (stepConfig) {
      var tab = directoriesTabs.findProperty('serviceName', stepConfig.get('serviceName'));
      if (tab) {
        services.push(tab);
      }
    });
    this.set('services', services);
    this.get('services').setEach('isActive', false);
    if (lastSelectedService) {
      this.set('controller.selectedService', stepConfigs.findProperty('serviceName', lastSelectedService));
    } else {
      this.set('controller.selectedService', stepConfigs[0]);
    }
    this.get('controller').selectedServiceObserver();
  }.observes('controller.stepConfigs'),

  selectService: function (event) {
    this.set('controller.filter', '');
    this.get('controller.filterColumns').setEach('selected', false);
    this.get('services').setEach('isActive', false);
    event.context.set('isActive', true);
    this.set('controller.selectedService', this.get('controller.stepConfigs').findProperty('serviceName', event.context.get('serviceName')));
    this.get('controller.tabs').findProperty('isActive', true).set('selectedServiceName', event.context.get('serviceName'));
  },

  didInsertElement: function () {
    this.setServices();
    Em.run.next(this, function () {
      this.enableRightArrow();
    });
  },

  configsView: App.ServiceConfigView.extend({
    templateName: require('templates/common/configs/service_config_wizard'),
    selectedServiceBinding: 'controller.selectedService',
    filterBinding: 'controller.filter',
    columnsBinding: 'controller.filterColumns',
    hideTabHeaders: true,
    supportsConfigLayout: true,
    themeTemplate: require('templates/wizard/step7/directories_theme_layout'),
    tabs: function () {
      var tabs = App.Tab.find().filterProperty('themeName', 'directories').filterProperty('serviceName', this.get('controller.selectedService.serviceName'));
      return tabs;
    }.property('controller.selectedService.serviceName'),
    updateFilterCounters: function () {
      this.set('isAllConfigsHidden', this.get('tabs').everyProperty('allSectionsAreHiddenByFilter', true));
    }.observes('tabs.@each.allSectionsAreHiddenByFilter')
  }),

  isLeftArrowDisabled: true,

  isRightArrowDisabled: true,
  
  isNavArrowsHidden: Em.computed.and('isLeftArrowDisabled', 'isRightArrowDisabled'),

  enableRightArrow: function () {
    var container = $(this.get('element')).find('.tabs-container');
    var content = container.find('ul');
    this.set('isRightArrowDisabled', container.width() >= content.width());
  },

  getScrollInterval: function () {
    var INTERVAL = 300;
    var container = $(this.get('element')).find('.tabs-container');
    var content = container.find('ul');
    var gap = content.width() - container.width();
    var gapLeft = gap%INTERVAL;
    var totalScrollsNamber = Math.floor(gap/INTERVAL) || 1;
    return INTERVAL + Math.round(gapLeft/totalScrollsNamber) + 1;
  },

  scrollTabsLeft: function () {
    if (!this.get('isLeftArrowDisabled')) this.scrollTabs('left');
  },

  scrollTabsRight: function () {
    if (!this.get('isRightArrowDisabled')) this.scrollTabs('right');
  },

  scrollTabs: function (dir) {
    var container = $(this.get('element')).find('.tabs-container');
    var content = container.find('ul');
    var interval = this.getScrollInterval();
    this.set('isLeftArrowDisabled', dir === 'left' && interval >= container.scrollLeft());
    this.set('isRightArrowDisabled', dir === 'right' && content.width() - container.width() <= container.scrollLeft() + interval);
    container.animate({
      scrollLeft: (dir === 'left' ?  '-' : '+') + '=' + interval + 'px'
    });
  }

});

App.DirectoriesLayoutCategoryView = Em.View.extend(App.ConfigOverridable, {
  isCollapsed: false,
  toggleCollapsed: function () {
    this.$('.panel-body').toggle('blind', 500);
    this.toggleProperty('isCollapsed');
  }
});
