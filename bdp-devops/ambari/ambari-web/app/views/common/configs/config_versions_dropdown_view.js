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

App.ConfigVersionsDropdownView = Em.View.extend({
  templateName: require('templates/common/configs/config_versions_dropdown'),
  classNames: ['btn-group'],

  searchLabel: Em.I18n.t('common.search'),

  /**
   * if true then it's secondary dropdown in Compare Mode
   * @type {boolean}
   */
  isSecondary: false,
  serviceVersions: [],
  filterValue: '',
  isCompareMode: false,
  displayedServiceVersion: Em.computed.findBy('serviceVersions', 'isDisplayed', true),

  didInsertElement: function() {
    this.$().on("shown.bs.dropdown", () => {
      const versionsBlock = $(this).find('.versions-list');
      if (versionsBlock.height() < versionsBlock.prop('scrollHeight')) {
        versionsBlock.addClass('bottom-shadow');
      } else {
        versionsBlock.removeClass('bottom-shadow');
      }
      App.tooltip(this.$('[data-toggle="tooltip"]'));
    });
  },

  willDestroyElement: function() {
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  mainClickAction: function (event) {
    if (this.get('isSecondary')) {
      this.get('parentView').compare(event);
    } else {
      this.get('parentView').switchPrimaryInCompare(event);
    }
  },

  filteredServiceVersions: function() {
    return this.get('serviceVersions').filter((serviceVersion) => {
      if (!this.get('filterValue').trim()) return true;
      const searchString = Em.I18n.t('common.version') + ' ' + serviceVersion.get('version') + ' ' + serviceVersion.get('notes');
      return searchString.toLowerCase().indexOf(this.get('filterValue').trim().toLowerCase()) !== -1;
    });
  }.property('serviceVersions.length', 'filterValue')
});
