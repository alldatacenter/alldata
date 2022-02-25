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

App.ServiceConfigLayoutTabView = Em.View.extend(App.ConfigOverridable, App.LoadingOverlaySupport, {

  /**
   * Determines if view is editable
   * It true - show all control-elements (undo, override, finalize etc) for each widget
   * If false - no widgets control-elements will be shown
   * Bound from template
   * @type {boolean}
   */
  canEdit: true,

  /**
   * view need some time to prepare data to display it correct
   * before that it's better not to show anything
   * @type {boolean}
   */
  dataIsReady: false,

  /**
   * @type {App.Service}
   */
  service: Em.computed.alias('controller.selectedService'),

  templateName: function() {
    var customTemplate = this.get('customTemplate');
    return customTemplate ? customTemplate : require('templates/common/configs/service_config_layout_tab');
  }.property('customTemplate'),

  customTemplate: null,

  fieldToObserve: 'controller.recommendationsInProgress',

  classNames: ['enhanced-config-tab-content'],

  checkOverlay: function () {
    this.handleFieldChanges();
  }.observes('controller.activeTab.id', 'controller.activeTab.isRendered'),

  /**
   * Prepare configs for render
   * <code>subsection.configs</code> is an array of App.StackConfigProperty, but not App.ConfigProperty,
   * so proper config-properties should be linked to the subsections.
   * @method prepareConfigProperties
   */
  prepareConfigProperties: function () {
    var self = this;
    this.get('content.sectionRows').forEach(function (row) {
      row.forEach(function (section) {
        section.get('subsectionRows').forEach(function (subRow) {
          subRow.forEach(function (subsection) {
            self.setConfigsToContainer(subsection);
            subsection.get('subSectionTabs').forEach(function (subSectionTab) {
              self.setConfigsToContainer(subSectionTab);
            });
          });
        });
      });
    });
  },

  /**
   * changes active subsection tab
   * @param event
   */
  setActiveSubTab: function(event) {
    if (!event.context || !event.context.get('isVisible')) {
      return false;
    }
    try {
      event.context.get('subSection.subSectionTabs').setEach('isActive', false);
      event.context.set('isActive', true);
    } catch (e) {
      console.error('Can\'t update active subsection tab');
    }
  },

  didInsertElement: function () {
    this.set('dataIsReady', false);
    this.set('content.isConfigsPrepared', false);
    this._super();
    if (this.get('controller.isCompareMode')) {
      this.get('parentView').filterEnhancedConfigs();
    }
    this.set('content.isConfigsPrepared', true);
    this.set('dataIsReady', true);
    this._super(...arguments);
  }

});
