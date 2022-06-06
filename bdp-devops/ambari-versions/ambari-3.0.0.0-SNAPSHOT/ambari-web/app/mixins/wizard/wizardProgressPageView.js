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
 * Mixin for wizard view for showing command progress on wizard pages
 * This should
 * @type {Ember.Mixin}
 */
App.wizardProgressPageViewMixin = Em.Mixin.create({

  /**
   * Following computed property needs to be overridden by the view implementing this mixin
   */

  currentStep: '',

  /**
   * Following computed property needs to be overridden by the view implementing this mixin
   */

  headerTitle: function () {

  }.property(),

  submitButtonText: Em.I18n.t('common.next'),

  noticeCompleted: Em.I18n.t('wizard.progressPage.notice.completed'),

  noticeFailed: Em.computed.ifThenElse('controller.isSingleRequestPage', Em.I18n.t('wizard.singleRequest.progressPage.notice.failed'), Em.I18n.t('wizard.progressPage.notice.failed')),

  /**
   * @noticeInProgress: Following computed property needs to be overridden to show the label text while the commands
   * on the page are progressing
   */
  noticeInProgress: function () {

  }.property(),

  /**
   * @showBackButton: Override this property to show back button on the wizard progress page
   */
  showBackButton: false,

  /**
   * Following computed property needs to be overridden by the view implementing this mixin
   */
  notice: '',

  noticeClass: 'alert alert-info',

  /**
   * Class to define task label width
   * @type {String}
   */
  labelWidth: 'col-md-4',

  onStatusChange: function () {
    var status = this.get('controller.status');
    if (status === 'COMPLETED') {
      this.set('notice', this.get('noticeCompleted'));
      this.set('noticeClass', 'alert alert-success');
    } else if (status === 'FAILED') {
      this.set('notice', this.get('noticeFailed'));
      this.set('noticeClass', 'alert alert-error');
    } else {
      this.set('notice', this.get('noticeInProgress'));
      this.set('noticeClass', 'alert alert-info');
    }
  }.observes('controller.status'),

  taskView: Em.View.extend({
    icon: '',
    iconColor: '',
    linkClass: '',

    didInsertElement: function () {
      this.onStatus();
    },

    barWidth: Em.computed.format('width: {0}%;', 'content.progress'),

    onStatus: function () {
      var linkClass = !!this.get('content.requestIds.length') ? 'active-link' : 'active-text';
      this.set('linkClass', linkClass);
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
    }.observes('content.status', 'content.hosts.length','content.requestIds'),

    showProgressBar: Em.computed.equal('content.status', 'IN_PROGRESS')
  })

});


