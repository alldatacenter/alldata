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

App.ManageJournalNodeProgressPageView = Em.View.extend(App.wizardProgressPageViewMixin, {

  didInsertElement: function () {
    this.get('controller').loadStep();
  },

  headerTitle: function () {
    var currentStep = App.router.get('manageJournalNodeWizardController.currentStep');
    return  Em.I18n.t('admin.manageJournalNode.wizard.step' + currentStep + '.header');
  }.property(),

  noticeInProgress: function () {
    var currentStep = App.router.get('manageJournalNodeWizardController.currentStep');
    return  Em.I18n.t('admin.manageJournalNode.wizard.step' + currentStep + '.notice.inProgress');
  }.property(),

  notice: Em.I18n.t('admin.manageJournalNode.wizard.progressPage.notice.inProgress')
});
