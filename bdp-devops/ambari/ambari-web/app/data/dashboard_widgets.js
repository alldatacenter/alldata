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

module.exports = [
  {
    id: 1,
    viewName: 'NameNodeHeapPieChartView',
    sourceName: 'HDFS',
    title: Em.I18n.t('dashboard.widgets.NameNodeHeap'),
    threshold: [80, 90],
    groupName: 'nn'
  },
  {
    id: 2,
    viewName: 'NameNodeCapacityPieChartView',
    sourceName: 'HDFS',
    title: Em.I18n.t('dashboard.widgets.HDFSDiskUsage'),
    threshold: [85, 95]
  },
  {
    id: 3,
    viewName: 'NameNodeCpuPieChartView',
    sourceName: 'HDFS',
    title: Em.I18n.t('dashboard.widgets.NameNodeCpu'),
    threshold: [90, 95],
    groupName: 'nn'
  },
  {
    id: 4,
    viewName: 'DataNodeUpView',
    sourceName: 'HDFS',
    title: Em.I18n.t('dashboard.widgets.DataNodeUp'),
    threshold: [80, 90]
  },
  {
    id: 5,
    viewName: 'NameNodeRpcView',
    sourceName: 'HDFS',
    title: Em.I18n.t('dashboard.widgets.NameNodeRpc'),
    threshold: [1000, 3000],
    groupName: 'nn'
  },
  {
    id: 6,
    viewName: 'ChartClusterMetricsMemoryWidgetView',
    sourceName: 'HOST_METRICS',
    title: Em.I18n.t('dashboard.clusterMetrics.memory'),
    threshold: []
  },
  {
    id: 7,
    viewName: 'ChartClusterMetricsNetworkWidgetView',
    sourceName: 'HOST_METRICS',
    title: Em.I18n.t('dashboard.clusterMetrics.network'),
    threshold: []
  },
  {
    id: 8,
    viewName: 'ChartClusterMetricsCPUWidgetView',
    sourceName: 'HOST_METRICS',
    title: Em.I18n.t('dashboard.clusterMetrics.cpu'),
    threshold: []
  },
  {
    id: 9,
    viewName: 'ChartClusterMetricsLoadWidgetView',
    sourceName: 'HOST_METRICS',
    title: Em.I18n.t('dashboard.clusterMetrics.load'),
    threshold: []
  },
  {
    id: 10,
    viewName: 'NameNodeUptimeView',
    sourceName: 'HDFS',
    title: Em.I18n.t('dashboard.widgets.NameNodeUptime'),
    threshold: [],
    groupName: 'nn'
  },
  {
    id: 11,
    viewName: 'HDFSLinksView',
    sourceName: 'HDFS',
    title: Em.I18n.t('dashboard.widgets.HDFSLinks'),
    threshold: [],
    groupName: 'nn',
    isHiddenByDefault: true
  },
  {
    id: 12,
    viewName: 'HBaseLinksView',
    sourceName: 'HBASE',
    title: Em.I18n.t('dashboard.widgets.HBaseLinks'),
    threshold: [],
    isHiddenByDefault: true
  },
  {
    id: 13,
    viewName: 'HBaseMasterHeapPieChartView',
    sourceName: 'HBASE',
    title: Em.I18n.t('dashboard.widgets.HBaseMasterHeap'),
    threshold: [70, 90]
  },
  {
    id: 14,
    viewName: 'HBaseAverageLoadView',
    sourceName: 'HBASE',
    title: Em.I18n.t('dashboard.widgets.HBaseAverageLoad'),
    threshold: [150, 250]
  },
  {
    id: 15,
    viewName: 'HBaseRegionsInTransitionView',
    sourceName: 'HBASE',
    title: Em.I18n.t('dashboard.widgets.HBaseRegionsInTransition'),
    threshold: [3, 10]
  },
  {
    id: 16,
    viewName: 'HBaseMasterUptimeView',
    sourceName: 'HBASE',
    title: Em.I18n.t('dashboard.widgets.HBaseMasterUptime'),
    threshold: []
  },
  {
    id: 17,
    viewName: 'ResourceManagerHeapPieChartView',
    sourceName: 'YARN',
    title: Em.I18n.t('dashboard.widgets.ResourceManagerHeap'),
    threshold: [70, 90]
  },
  {
    id: 18,
    viewName: 'ResourceManagerUptimeView',
    sourceName: 'YARN',
    title: Em.I18n.t('dashboard.widgets.ResourceManagerUptime'),
    threshold: [],
    isHiddenByDefault: true
  },
  {
    id: 19,
    viewName: 'NodeManagersLiveView',
    sourceName: 'YARN',
    title: Em.I18n.t('dashboard.widgets.NodeManagersLive'),
    threshold: [50, 75]
  },
  {
    id: 20,
    viewName: 'YARNMemoryPieChartView',
    sourceName: 'YARN',
    title: Em.I18n.t('dashboard.widgets.YARNMemory'),
    threshold: [50, 75],
    isHiddenByDefault: true
  },
  {
    id: 21,
    viewName: 'SuperVisorUpView',
    sourceName: 'STORM',
    title: Em.I18n.t('dashboard.widgets.SuperVisorUp'),
    threshold: [85, 95]
  },
  {
    id: 22,
    viewName: 'FlumeAgentUpView',
    sourceName: 'FLUME',
    title: Em.I18n.t('dashboard.widgets.FlumeAgentUp'),
    threshold: [85, 95]
  },
  {
    id: 23,
    viewName: 'YARNLinksView',
    sourceName: 'YARN',
    title: Em.I18n.t('dashboard.widgets.YARNLinks'),
    threshold: [],
    isHiddenByDefault: true
  },
  {
    id: 24,
    viewName: 'HawqSegmentUpView',
    sourceName: 'HAWQ',
    title: Em.I18n.t('dashboard.widgets.HawqSegmentUp'),
    threshold: [75, 90]
  },
  {
    id: 25,
    viewName: 'PxfUpView',
    sourceName: 'PXF',
    title: Em.I18n.t('dashboard.widgets.PxfUp'),
    threshold: []
  },
  {
    id: 26,
    viewName: 'YarnContainersView',
    sourceName: 'YARN',
    title: Em.I18n.t('dashboard.widgets.YarnContainers'),
    threshold: []
  },
];