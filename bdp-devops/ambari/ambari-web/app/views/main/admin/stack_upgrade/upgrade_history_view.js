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
var date = require('utils/date/date');
const arrayUtils = require('utils/array_utils');

App.MainAdminStackUpgradeHistoryView = App.TableView.extend(App.TableServerViewMixin, {

  controllerBinding: 'App.router.mainAdminStackUpgradeHistoryController',

  templateName: require('templates/main/admin/stack_upgrade/upgrade_history'),

  summary: [],

  isReady: false,

  categories: [
    Em.Object.create({
      labelKey: 'admin.stackVersions.upgradeHistory.filter.all',
      value: 'ALL',
      isSelected: true
    }),
    Em.Object.create({
      labelKey: 'admin.stackVersions.upgradeHistory.filter.upgrade',
      value: 'UPGRADE',
      isSelected: false
    }),
    Em.Object.create({
      labelKey: 'admin.stackVersions.upgradeHistory.filter.downgrade',
      value: 'DOWNGRADE',
      isSelected: false
    }),
    Em.Object.create({
      labelKey: 'admin.stackVersions.upgradeHistory.filter.successful.upgrade',
      value: 'UPGRADE_COMPLETED',
      isSelected: false
    }),
    Em.Object.create({
      labelKey: 'admin.stackVersions.upgradeHistory.filter.aborted.upgrade',
      value: 'UPGRADE_ABORTED',
      isSelected: false
    }),
    Em.Object.create({
      labelKey: 'admin.stackVersions.upgradeHistory.filter.failed.upgrade',
      value: 'UPGRADE_FAILED',
      isSelected: false
    }),
    Em.Object.create({
      labelKey: 'admin.stackVersions.upgradeHistory.filter.successful.downgrade',
      value: 'DOWNGRADE_COMPLETED',
      isSelected: false
    }),
    Em.Object.create({
      labelKey: 'admin.stackVersions.upgradeHistory.filter.aborted.downgrade',
      value: 'DOWNGRADE_ABORTED',
      isSelected: false
    }),
    Em.Object.create({
      labelKey: 'admin.stackVersions.upgradeHistory.filter.failed.downgrade',
      value: 'DOWNGRADE_FAILED',
      isSelected: false
    })
  ],
  upgradeMethods: [
    Em.Object.create({
      displayName: Em.I18n.t('common.rolling'),
      type: 'ROLLING'
    }),
    Em.Object.create({
      displayName: Em.I18n.t('common.express'),
      type: 'NON_ROLLING'
    }),
    Em.Object.create({
      displayName: Em.I18n.t('common.hostOrdered'),
      type: 'HOST_ORDERED'
    })
  ],

  /**
   * @type {object}
   */
  selectedCategory: Em.computed.findBy('categories', 'isSelected', true),

  filteredCount: Em.computed.alias('filteredContent.length'),

  /**
   * displaying content filtered by upgrade type and upgrade status.
   */
  filteredContent: function () {
    var filterValue = 'ALL';
    var category = this.get('selectedCategory');
    if (category) {
      filterValue = category.get('value');
    }
    return this.filterBy(filterValue).reverse();
  }.property('selectedCategory'),

  /**
   * sort and slice received content by pagination parameters
   */
  pageContent: function () {
    var content = this.get('filteredContent').toArray();
    content = this.processForDisplay(content);
    content = content.slice(this.get('startIndex') - 1, this.get('endIndex'));
    return content;
  }.property('filteredContent', 'startIndex', 'endIndex'),

  processForDisplay: function (content) {
    var upgradeMethods = this.get('upgradeMethods');
    var repoVersions = App.RepositoryVersion.find();
    return content.map(function(item) {
      var versions = item.get('versions');
      var repoVersion = repoVersions.findProperty('repositoryVersion', item.get('associatedVersion'));
      var method = upgradeMethods.findProperty('type', item.get('upgradeType'));
      return {
        idHref: '#' + item.get('upgradeId'),
        id: item.get('upgradeId'),
        repositoryName: repoVersion.get('displayName'),
        repositoryType: repoVersion.get('type').toLowerCase().capitalize(),
        directionLabel: item.get('direction') === 'UPGRADE' ? Em.I18n.t('common.upgrade') : Em.I18n.t('common.downgrade'),
        upgradeTypeLabel: method ? method.get('displayName') : method,
        startTimeLabel: date.startTime(App.dateTimeWithTimeZone(item.get('startTime'))),
        endTimeLabel: date.endTime(App.dateTimeWithTimeZone(item.get('endTime'))),
        duration: date.durationSummary(item.get('startTime'), item.get('endTime')),
        displayStatus: item.get('displayStatus'),
        stackUpgradeHistoryItem: item,
        services: this.getRepoServicesForDisplay(versions)
      };
    }, this);
  },

  getRepoServicesForDisplay: function(versions) {
    return Object.keys(versions).map(function(serviceName) {
      var displayName = App.RepositoryVersion.find(versions[serviceName].from_repository_id).get('stackServices').findProperty('name', serviceName).get('displayName');
      return {
        name: serviceName,
        displayName: displayName,
        fromVersion: versions[serviceName].from_repository_version,
        toVersion: versions[serviceName].to_repository_version
      }
    });
  },

  paginationLeftClass: function () {
    if (this.get("startIndex") > 1) {
      return "paginate_previous";
    }
    return "paginate_disabled_previous";
  }.property("startIndex", 'filteredCount'),

  /**
   * Determines how display "next"-link - as link or text
   * @type {string}
   */
  paginationRightClass: function () {
    if (this.get("endIndex") < this.get("filteredCount")) {
      return "paginate_next";
    }
    return "paginate_disabled_next";
  }.property("endIndex", 'filteredCount'),

  /**
   * Show previous-page if user not in the first page
   * @method previousPage
   */
  previousPage: function () {
    if (this.get('paginationLeftClass') === 'paginate_previous') {
      this._super();
    }
  },

  /**
   * Show next-page if user not in the last page
   * @method nextPage
   */
  nextPage: function () {
    if (this.get('paginationRightClass') === 'paginate_next') {
      this._super();
    }
  },

  willInsertElement: function () {
    var self = this;
    this.get('controller').loadStackUpgradeHistoryToModel().done(function () {
      self.populateUpgradeHistorySummary();
    });
  },

  didInsertElement: function () {
    this.observesCategories();
    this.$(".accordion").on("show.bs.collapse hide.bs.collapse", function (e) {
      $(e.target).siblings(".accordion-heading").find("i.accordion-toggle").toggleClass('icon-caret-right icon-caret-down');
    });

    Em.run.later(this, function(){
      App.tooltip($('.widest-column span'));
    }, 1000)
  },

  observesCategories: function () {
    this.get('categories').forEach(function (category) {
      var label = Em.I18n.t(category.labelKey).format(this.filterBy(category.value).length);
      category.set('label', label)
    }, this);
  }.observes('isReady'),

  filterBy: function (filterValue) {
    if ('ALL' == filterValue) {
      var all_records = App.StackUpgradeHistory.find();
      return all_records.toArray();
    } else {
      var tokens = filterValue.split('_');
      var direction_token = null;
      var status_token = null;

      if (tokens.length == 1) {
        direction_token = tokens[0]
      } else if (tokens.length > 1) {
        direction_token = tokens[0];
        status_token = tokens[1];
      }

      var result = [];
      App.StackUpgradeHistory.find().forEach(function (item) {
        var direction = item.get('direction');
        if (direction == direction_token) {
          if (status_token != null) {
            //only for the given status
            var status = item.get('requestStatus');
            if (status == status_token) {
              result.push(item)
            }
          } else {
            //regardless status
            result.push(item)
          }
        }
      }, this);
      return result
    }
  },

  selectCategory: function (event) {
    this.get('categories').filterProperty('isSelected').setEach('isSelected', false);
    event.context.set('isSelected', true);
  },

  populateUpgradeHistorySummary: function () {
    this.set('isReady', false);
    var result = [
      Em.Object.create({
        direction: 'UPGRADE',
        label: Em.I18n.t('common.upgrade'),
        hasSuccess: false,
        success: 0,
        hasAbort: false,
        abort: 0
      }),
      Em.Object.create({
        direction: 'DOWNGRADE',
        label: Em.I18n.t('common.downgrade'),
        hasSuccess: false,
        success: 0,
        hasAbort: false,
        abort: 0
      })
    ];

    App.StackUpgradeHistory.find().forEach(function (item) {
      var direction = item.get('direction');
      var status = item.get('requestStatus');
      if ('UPGRADE' == direction) {
        if ('COMPLETED' == status) {
          result[0].set('success', result[0].get('success') + 1);
        } else if ('ABORTED' == status) {
          result[0].set('abort', result[0].get('abort') + 1);
        }
      } else if ('DOWNGRADE' == direction) {
        if ('COMPLETED' == status) {
          result[1].set('success', result[1].get('success') + 1);
        } else if ('ABORTED' == status) {
          result[1].set('abort', result[1].get('abort') + 1);
        }
      }
    }, this);

    result[0].set('hasSuccess', result[0].get('success') > 0);
    result[1].set('hasSuccess', result[1].get('success') > 0);
    result[0].set('hasAbort', result[0].get('abort') > 0);
    result[1].set('hasAbort', result[1].get('abort') > 0);

    this.set('summary', result);
    this.set('isReady', true);
  },

  showUpgradeHistoryRecord: function (event) {
    var record = event.context;
    var direction = App.format.normalizeName(record.get('direction'));
    var associatedVersion = record.get('associatedVersion');
    var type = this.get('upgradeMethods').findProperty('type', record.get('upgradeType'));
    var displayName = type ? type.get('displayName') : App.format.normalizeName(record.get('upgradeType'));
    const i18nKeySuffix = direction.toLowerCase() === 'upgrade' ? 'upgrade' : 'downgrade';

    this.get('controller').set('currentUpgradeRecord', record);

    App.ModalPopup.show({
      classNames: ['wide-modal-wrapper'],
      modalDialogClasses: ['modal-xlg'],
      header: Em.I18n.t(`admin.stackVersions.upgradeHistory.record.title.${i18nKeySuffix}`).format(displayName, direction, associatedVersion),
      bodyClass: App.MainAdminStackUpgradeHistoryDetailsView,
      primary: Em.I18n.t('common.dismiss'),
      secondary: null,
      didInsertElement: function () {
        this._super();
        this.fitHeight();
        this.fitInnerHeight();
      },
      fitInnerHeight: function () {
        var block = this.$().find('.modal .modal-body');
        var scrollable = this.$().find('.modal .scrollable-block');
        scrollable.css('max-height', Number(block.css('max-height').slice(0, -2)) - block.height());
        block.css('max-height', 'none');
      }
    });
  }
});
