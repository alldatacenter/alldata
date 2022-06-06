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

App.LinkDashboardWidgetView = App.DashboardWidgetView.extend({

  componentName: null,

  component: function() {
    return App.HostComponent.find().findProperty('componentName', this.get('componentName'));
  }.property(),

  port: null,

  modelField: null,

  webUrl: null,

  didInsertElement: function() {
    this._super();
    this.addObserver('model.' + this.get('modelField'), this, this.calc);
  },
  calc: function() {
    this.set('webUrl', this.calcWebUrl());
  },
  calcWebUrl: function() {
    if (this.get('model') && this.get('model').get(this.get('modelField'))) {
      return "http://" + this.get('model').get(this.get('modelField')).get('publicHostName') + ':' + this.get('port');
    }
    return '';
  }
});
