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

App.NameNodeHeapPieChartView = App.PieChartDashboardWidgetView.extend(App.NameNodeWidgetMixin, {

  modelValueMax: Em.computed.getByKey('model.jvmMemoryHeapMaxValues', 'hostName'),
  modelValueUsed: Em.computed.getByKey('model.jvmMemoryHeapUsedValues', 'hostName'),
  widgetHtmlId: Em.computed.format('widget-nn-heap-{0}', 'subGroupId'),

  getUsed: function() {
    return this.get('modelValueUsed') / (1024 * 1024) || 0;
  },

  getMax: function() {
    return this.get('modelValueMax') / (1024 * 1024) || 0;
  },

  didInsertElement: function() {
    this._super();
    this.calc();
  }
});