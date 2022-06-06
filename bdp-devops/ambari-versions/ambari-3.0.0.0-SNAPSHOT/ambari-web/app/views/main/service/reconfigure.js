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

App.MainServiceReconfigureView = Em.View.extend({

  templateName: require('templates/main/service/reconfigure')

});

App.StageLabelView = Em.View.extend({
  tagName: 'a',
  classNameBindings: ['removeLink'],
  attributeBindings: ['href'],
  href: '#',
  removeLink: null,
  didInsertElement: function() {
   this.onLink();
  },
  onLink: function() {
    this.set('removeLink', this.get('showLink') ? null : 'remove-link');
  }.observes('showLink'),
  command: null,
  click: function () {
    if (this.get('command') && this.get('showLink')) {
      this.showHostPopup(this.get('command'));
    }
  },

  showHostPopup: function (command) {
    var controller = this.get("controller");
    App.router.get('userSettingsController').dataLoading('show_bg').done(function (initValue) {
      var popupView = App.HostPopup.initPopup(command.get('label'), controller, false, command.get('requestId'));
      popupView.set('isNotShowBgChecked', !initValue);
    })
  },

  isStarted: Em.computed.alias('command.isStarted'),

  showLink: Em.computed.alias('command.showLink')

});

App.StageSuccessView = Em.View.extend({
  layout: Ember.Handlebars.compile('<i class="glyphicon glyphicon-ok glyphicon-large"></i> {{t common.done}}')
});

App.StageFailureView = Em.View.extend({
  layout: Ember.Handlebars.compile('<i class="glyphicon glyphicon-remove iglyphicon-large"></i> {{t common.failed}}')
});

App.StageInProgressView = Em.View.extend({
  command: null,
  classNames: ['progress'],
  template: Ember.Handlebars.compile('<div class="progress-bar progress-bar-striped active" {{bindAttr style="command.barWidth"}}></div>'),

  isStageCompleted: function () {
    return this.get('obj.progress') === 100 || this.get('controller.isStepCompleted');
  }.property('controller.isStepCompleted', 'obj.progress')

});