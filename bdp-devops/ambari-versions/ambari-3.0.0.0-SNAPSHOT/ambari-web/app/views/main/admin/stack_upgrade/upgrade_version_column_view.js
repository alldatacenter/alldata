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

App.UpgradeVersionColumnView = App.UpgradeVersionBoxView.extend({
  templateName: require('templates/main/admin/stack_upgrade/upgrade_version_column'),
  isVersionColumnView: true,
  classNames: ['version-column', 'version'],

  /**
   * Indicates whether block for version type should be displayed.
   * True if any of available versions is of PATCH, MAINT or SERVICE type
   * @type {Boolean}
   */
  displayVersionTypeBlock: false,

  currentLabelClass: 'btn btn-primary',

  didInsertElement: function () {
    App.tooltip($('.out-of-sync-badge'), {title: Em.I18n.t('hosts.host.stackVersions.status.out_of_sync')});
    App.tooltip($('.not-upgradable'));
    if (!this.get('content.isCompatible')) {
      App.tooltip(this.$(".repo-version-tooltip"), {
        title: Em.I18n.t('admin.stackVersions.version.noCompatible.tooltip')
      });
    }
    this.adjustColumnWidth();
  },

  adjustColumnWidth: function () {
    //set the width of each version column dynamically
    const reposCount = App.RepositoryVersion.find().filterProperty('isVisible').get('length'),
      widthFactor = reposCount > 3 ? 0.19 : 0.31,
      columnBorderWidth = 1, // from ambari-web/app/styles/stack_versions.less
      columnPadding = 5, // from ambari-web/app/styles/stack_versions.less
      columnWrapperPadding = 7, // from ambari-web/app/styles/stack_versions.less
      width = $('.versions-slides').width() * widthFactor + 2 * (columnBorderWidth + columnPadding + columnWrapperPadding);
    $('.version-column.version').width(width);
    $('.versions-slides-bar').width(width * reposCount + columnWrapperPadding);
  }.observes('parentView.repoVersions.@each.isVisible'),

  services: function () {
    const originalServices = this.get('content.stackServices');
    // sort the services in the order the same as service menu
    return App.Service.find().map(service => {

      const stackService = originalServices.findProperty('name', service.get('serviceName')),
        isAvailable = this.isStackServiceAvailable(stackService);
      let notUpgradable = false,
        notUpgradableTitle = '';
      if (!stackService) {
        console.error(`${stackService} definition does not exist in the stack.`);
        notUpgradable = true;
        notUpgradableTitle = Em.I18n.t('admin.stackVersions.version.service.notSupported');
      } else {
        notUpgradable = this.getNotUpgradable(isAvailable, stackService.get('isUpgradable'));
        if (notUpgradable) {
          notUpgradableTitle = Em.I18n.t('admin.stackVersions.version.service.notUpgradable');
        }
      }

      return Em.Object.create({
        displayName: service.get('displayName'),
        name: service.get('serviceName'),
        latestVersion: stackService ? stackService.get('latestVersion') : '',
        isVersionInvisible: !stackService,
        notUpgradable,
        notUpgradableTitle,
        isAvailable
      });
    });
  }.property(),

  getNotUpgradable: function (isAvailable, isUpgradable) {
    return this.get('content.isMaint') && !this.get('isUpgrading') && this.get('content.status') !== 'CURRENT' && isAvailable && !isUpgradable;
  },


  /**
   * @param {Em.Object} stackService
   * @returns {boolean}
   */
  isStackServiceAvailable: function (stackService) {
    var self = this;
    if (!stackService) {
      return false;
    }
    if (this.get('content.isCurrent')) {
      var originalService = App.Service.find(stackService.get('name'));
      return stackService.get('isAvailable') && originalService.get('desiredRepositoryVersionId') === this.get('content.id');
    }
    else {
      return stackService.get('isAvailable')
    }
  },

  /**
   * on click handler for "show details" link
   */
  openVersionBoxPopup: function (event) {
    var content = this.get('content');
    var parentView = this.get('parentView');

    return App.ModalPopup.show({
      classNames: ['version-box-popup'],
      bodyClass: App.UpgradeVersionBoxView.extend({
        classNames: ['version-box-in-popup'],
        content: content,
        parentView: parentView
      }),
      header: Em.I18n.t('admin.stackVersions.version.column.showDetails.title'),
      primary: Em.I18n.t('common.dismiss'),
      secondary: null
    });
  }
});
