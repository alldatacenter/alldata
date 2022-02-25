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

/**
 * Popover for configs widgets
 * Usage:
 * <code>
 *  didInsertElement: function () {
 *    this._super();
 *    this.initPopover();
 *  }
 *  </code>
 * @type {Em.Mixin}
 */
App.WidgetPopoverSupport = Em.Mixin.create({

  /**
   * Should popover be on the page
   * @type {boolean}
   */
  isPopoverEnabled: true,

  /**
   * Where popover should be displayed - top|left|right|bottom
   * popover to left if config is located at the right most sub-section of the right most section.
   *
   * @type {string}
   */
  popoverPlacement: function () {
    return this.get('section.isLastColumn') && this.get('subSection.isLastColumn') ? 'left' : 'right';
  }.property('section.isLastColumn', 'subSection.isLastColumn'),

  initPopover: function () {
    if (this.get('isPopoverEnabled') !== false) {
      this.destroyPopover();
      var leftPopoverTemplate = '<div class="popover config-widget-left-popover"><div class="arrow"></div><div class="popover-inner"><h3 class="popover-title"></h3><div class="popover-content"><p></p></div></div></div>',
        isWidget = !Em.isEmpty(this.$('.original-widget')),
        popoverSelector = isWidget ? this.$('.original-widget') : this.$('.input-group');

      App.popover(popoverSelector, {
        template: this.get('popoverPlacement') === 'left'? leftPopoverTemplate : undefined,
        title: Em.I18n.t('installer.controls.serviceConfigPopover.title').format(
          this.get('configLabel'),
          this.get('configLabel') === this.get('config.name') ? '' : this.get('config.name')
        ),
        content: this.get('config.description'),
        placement: this.get('popoverPlacement'),
        trigger: 'hover',
        html: true,
        delay: {
          show: 1000,
          hide: 0
        }
      });
      this.on('willDestroyElement', this, this.destroyPopover);
    }
  },

  /**
   * Destroy popover after config becomes hidden
   */
  destroyPopover: function () {
    this.movePopover('destroy');
  },

  /**
   * Hide popover on config state changing (from widget-view to raw-mode and from raw-mode to widget-view)
   */
  hidePopover: function () {
    this.movePopover('hide');
  }.observes('config.showAsTextBox'),

  movePopover: function (action) {
    var popoverSelector = Em.isEmpty(this.$('.original-widget')) ? this.$('.input-group') : this.$('.original-widget');
    if (popoverSelector) {
      this.$(popoverSelector).popover(action)
    }
  }

});
