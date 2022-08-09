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

App.ServicesConfigView = Em.View.extend({

  templateName: require('templates/common/configs/services_config'),

  classNames: ['accordion'],

  didInsertElement: function () {
    if (!this.get('controller.isInstallWizard')) {
      this.get('controller').loadStep();
    } else {
      this.get('controller').selectProperService();
      this.set('controller.selectedService.isActive', true);
      this.get('controller').selectedServiceObserver();
      Em.run.next(this, function () {
        this.enableRightArrow();
      });
    }
  },
  
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
