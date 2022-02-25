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

App.PigView = Em.View.extend({
  hideScript:true,
  selectedBinding: 'controller.category',
  navs: [
    {name:'scripts',url:'pig',label: Em.I18n.t('scripts.scripts'),icon:'fa-file-code-o'},
    {name:'udfs',url:'pig.udfs',label:Em.I18n.t('udfs.udfs'),icon:'fa-plug'},
    {name:'history',url:'pig.history',label:Em.I18n.t('common.history'),icon:'fa-clock-o'}
  ],

  initToolip:function () {
    this.$('button.close_script').tooltip({title:Em.I18n.t('common.close'),placement:'bottom',container:'body'});
  }.on('didInsertElement'),

  showSideBar:function () {
    Em.run.later(this, function (show) {
      this.$('.nav-script-wrap').toggleClass('in',show);
    },!!this.get('controller.activeScript'),250);
  }.observes('controller.activeScript')
});

App.NavItemsView = Ember.CollectionView.extend({
  tagName: 'div',
  classNames:['list-group', 'nav-main'],
  content: [],
  mouseEnter:function  (argument) {
    this.get('parentView').$('.nav-script-wrap').addClass('reveal');
  },
  mouseLeave:function  (argument) {
    this.get('parentView').$('.nav-script-wrap').removeClass('reveal');
  },
  itemViewClass: Ember.Component.extend({
    tagName: 'a',
    layout: Em.Handlebars.compile(
      '<i class="fa fa-fw fa-2x {{unbound view.content.icon}}"></i> '+
      '<span>{{view.content.label}}</span>'
    ),
    classNames: ['list-group-item pig-nav-item text-left'],
    classNameBindings: ['isActive:active'],
    action: 'gotoSection',
    click: function() {
      this.sendAction('action',this.get('content'));
    },
    isActive: function () {
      return this.get('content.name') === this.get('parentView.parentView.selected');
    }.property('content.name', 'parentView.parentView.selected')
  })
});
