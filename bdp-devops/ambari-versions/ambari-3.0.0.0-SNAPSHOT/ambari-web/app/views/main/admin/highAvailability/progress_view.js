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

App.HighAvailabilityProgressPageView = Em.View.extend(App.wizardProgressPageViewMixin, {

  /**
   * @type {string}
   */
  notice: Em.I18n.t('admin.highAvailability.wizard.progressPage.notice.inProgress'),

  /**
   * @type {string}
   */
  noticeClass: 'alert alert-info',

  didInsertElement: function () {
    this.get('controller').loadStep();
  },

  /**
   * @type {string}
   */
  headerTitle: function () {
    var currentStep = App.router.get('highAvailabilityWizardController.currentStep');
    if (currentStep === 1) {
      return  Em.I18n.t('admin.highAvailability.wizard.rollback.header.title');
    } else {
      return  Em.I18n.t('admin.highAvailability.wizard.step' + currentStep + '.header');
    }
  }.property(),

  /**
   * @type {string}
   */
  noticeInProgress: function () {
    var currentStep = App.router.get('highAvailabilityWizardController.currentStep');
    if (currentStep === 1) {
      return  Em.I18n.t('admin.highAvailability.rollback.notice.inProgress');
    } else {
      return  Em.I18n.t('admin.highAvailability.wizard.progressPage.notice.inProgress');
    }
  }.property(),

  onStatusChange: function () {
    var status = this.get('controller.status');
    if (status === 'COMPLETED') {
      this.set('notice', this.get('noticeCompleted'));
      this.set('noticeClass', 'alert alert-success');
    } else if (status === 'FAILED') {
      this.set('notice', this.get('noticeFailed'));
      this.set('noticeClass', 'alert alert-danger');
    } else {
      this.set('notice', this.get('noticeInProgress'));
      this.set('noticeClass', 'alert alert-info');
    }
  }.observes('controller.status'),

  /**
   * @type {Em.View}
   */
  taskView: Em.View.extend({
    icon: '',
    iconColor: '',
    linkClass: '',

    didInsertElement: function () {
      this.onStatus();
      $('body').tooltip({
        selector: '[rel=tooltip]'
      });
    },

    barWidth: Em.computed.format('width: {0}%;', 'content.progress'),

    onStatus: function () {
      this.set('linkClass', Boolean(this.get('content.requestIds.length')) ? 'active-link' : 'active-text');
      if (this.get('content.status') === 'IN_PROGRESS') {
        this.set('icon', 'glyphicon glyphicon-cog');
        this.set('iconColor', 'text-info');
      } else if (this.get('content.status') === 'FAILED') {
        this.set('icon', 'glyphicon glyphicon-exclamation-sign');
        this.set('iconColor', 'text-danger');
      } else if (this.get('content.status') === 'COMPLETED') {
        this.set('icon', 'glyphicon glyphicon-ok');
        this.set('iconColor', 'text-success');
      } else {
        this.set('icon', 'glyphicon glyphicon-cog');
        this.set('iconColor', '');
        this.set('linkClass', 'not-active-link');
      }
    }.observes('content.status', 'content.hosts.length'),

    showProgressBar: Em.computed.equal('content.status', 'IN_PROGRESS'),

    hidePercent: Em.computed.equal('content.command', 'testDBConnection'),

    showDBTooltip: Em.computed.not('hidePercent')
  })
});
