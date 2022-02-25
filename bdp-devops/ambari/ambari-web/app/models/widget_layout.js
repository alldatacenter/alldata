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

App.WidgetLayout = DS.Model.extend({
  layoutName: DS.attr('string'),
  displayName: DS.attr('string'),
  sectionName: DS.attr('string'),
  widgets: DS.hasMany('App.Widget'),
  scope: DS.attr('string'),
  user: DS.attr('string'),

  nameServiceId: function () {
    return this.get('layoutName').split('_nameservice_')[1] || '';
  }.property('layoutName')
});


App.WidgetLayout.FIXTURES = [];
