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

App.MainAdminStackMenuView = Em.CollectionView.extend({
  tagName: 'ul',
  classNames: ["nav", "nav-tabs", "background-text"],
  defaultRoute: 'services',

  content: function () {
    return [
      Em.Object.create({
        name: 'services',
        label: Em.I18n.t('common.stack'),
        routing: 'services'
      }),
      Em.Object.create({
        name: 'versions',
        label: Em.I18n.t('common.versions'),
        routing: 'versions',
        hidden: !App.get('stackVersionsAvailable')
      }),
      Em.Object.create({
        name: 'upgradeHistory',
        label: Em.I18n.t('common.upgrade.history'),
        routing: 'upgradeHistory',
        hidden: !App.get('upgradeHistoryAvailable')
      })
    ]
  }.property('App.stackVersionsAvailable'),

  didInsertElement: function () {
    this.activateView();
  },

  activateView: function () {
    var defaultRoute = App.router.get('currentState.name') || this.get('defaultRoute');
    $.each(this._childViews, function () {
      this.set('active', (this.get('content.routing') == defaultRoute) ? "active" : "");
    });
  },

  deactivateChildViews: function () {
    $.each(this._childViews, function () {
      this.set('active', "");
    });
  },

  itemViewClass: Em.View.extend({
    classNameBindings: ["active"],
    active: "",
    template: Ember.Handlebars.compile('{{#unless view.content.hidden}}<a {{action stackNavigate view.content.routing }} href="#"> {{unbound view.content.label}}</a>{{/unless}}')
  })
});
