/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

App.FlumeAgentMetricsSectionView = Em.View.extend(App.TimeRangeMixin, {

  templateName: require('templates/main/service/info/metrics/flume/flume_agent_metrics_section'),

  index: 0,

  metricTypeKey: '',

  metricView: null,

  metricViewData: null,

  id: Em.computed.format('metric{0}', 'index'),

  toggleIndex: Em.computed.format('#{0}', 'id'),

  header: function () {
    var metricType = Em.I18n.t('services.service.info.metrics.flume.' + this.get('metricTypeKey')).format(Em.I18n.t('common.metrics'));
    return metricType + ' - ' + this.get('metricViewData.agent.hostName');
  }.property('metricTypeKey', 'metricViewData.agent.hostName')

});
