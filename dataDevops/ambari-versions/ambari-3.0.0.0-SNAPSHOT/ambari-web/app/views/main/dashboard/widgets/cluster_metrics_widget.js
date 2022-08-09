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

App.ClusterMetricsDashboardWidgetView = App.DashboardWidgetView.extend(App.ExportMetricsMixin, {

  templateName: require('templates/main/dashboard/widgets/cluster_metrics'),

  isClusterMetricsWidget: true,

  exportTargetView: Em.computed.alias('childViews.lastObject'),

  didInsertElement: function () {
    var self = this;
    App.tooltip(this.$('.corner-icon > .glyphicon-save'), {
      title: Em.I18n.t('common.export')
    });
  },

  willDestroyElement: function () {
    this.$('.corner-icon > .glyphicon-save').tooltip('destroy');
  }

});
