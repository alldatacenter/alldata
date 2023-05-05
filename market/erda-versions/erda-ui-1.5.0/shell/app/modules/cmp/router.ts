// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import getDcosRouter from 'dcos/router';
import i18n from 'i18n';
import { map } from 'lodash';
import { mqTabs } from 'dcos/pages/service-manager/mq-manager/index';
import { redisTabs } from 'dcos/pages/service-manager/redis-manager/index';
import { rdsTabs } from 'dcos/pages/service-manager/rds-manager/index';
import { TYPE_K8S_AND_EDAS } from 'cmp/pages/cluster-manage/config';
import { EditStrategyPageName, AddStrategyPageName } from 'cmp/common/alarm-strategy/strategy-form';
import ClusterSelector from 'cmp/pages/cluster-container/cluster-selector';

export const getOrgProjectTabs = () => [
  {
    key: 'member',
    name: i18n.t('cmp:member management'),
  },
];

const middlewareTabs = [
  { key: 'monitor', name: i18n.t('monitor detail') },
  { key: 'resource', name: i18n.t('resource detail') },
  { key: 'detail', name: i18n.t('basic information') },
];

const clusterDetailTabs = (params: Obj) => {
  const clusterType = params.breadcrumbInfoMap.cmpCluster?.type;
  return TYPE_K8S_AND_EDAS.includes(clusterType)
    ? [
        { key: 'nodes', name: i18n.t('cmp:Node List') },
        { key: 'pods', name: i18n.t('cmp:Pods List') },
        { key: 'workload', name: i18n.t('cmp:Workload') },
        { key: 'event-log', name: i18n.t('cmp:Event Log') },
        { key: 'detail', name: i18n.t('cmp:Basic Information') },
      ]
    : [{ key: 'detail', name: i18n.t('cmp:Basic Information') }];
};

const kubernetesTabs = [
  { key: 'base', name: i18n.t('cmp:Basic Information') },
  { key: 'pod', name: i18n.t('cmp:related pod information') },
  { key: 'detail', name: i18n.t('detail') },
];

const resourceRankTabs = [
  { key: 'project', name: i18n.t('cmp:by project') },
  { key: 'owner', name: i18n.t('cmp:by owner') },
];

function getCmpRouter(): RouteConfigItem[] {
  return [
    {
      path: 'cmp',
      mark: 'cmp',
      toMark: 'orgIndex',
      routes: [
        ...getDcosRouter(),
        {
          path: 'clusters',
          breadcrumbName: i18n.t('clusters'),
          routes: [
            {
              layout: { noWrapper: true },
              getComp: (cb) => cb(import('app/modules/cmp/pages/cluster-manage')),
            },
            {
              path: 'addCluster',
              pageName: i18n.t('cmp:cluster deployment'),
              getComp: (cb) => cb(import('app/modules/cmp/pages/cluster-manage/deploy-cluster')),
            },
            {
              path: 'history',
              pageName: i18n.t('cmp:operation history'),
              getComp: (cb) => cb(import('app/modules/cmp/pages/cluster-manage/operation-history'), 'OperationHistory'),
            },
            {
              path: ':clusterName',
              mark: 'clusterDetail',
              routes: [
                {
                  path: 'detail',
                  breadcrumbName: `${i18n.t('cluster detail')}({params.clusterName})`,
                  routes: [
                    {
                      getComp: (cb) => cb(import('app/modules/cmp/pages/cluster-manage/cluster-detail')),
                    },
                  ],
                },
                {
                  path: 'biCharts',
                  mark: 'biCharts',
                  routes: [
                    {
                      path: ':chartUniqId',
                      mark: 'clusterAlarmReport',
                      routes: [
                        {
                          path: ':processId',
                          breadcrumbName: i18n.t('cmp:process details'),
                          getComp: (cb) => cb(import('dcos/pages/alarm-report/processDetail')),
                        },
                        {
                          breadcrumbName: i18n.t('cmp:alarm data report'),
                          getComp: (cb) => cb(import('dcos/pages/alarm-report')),
                        },
                      ],
                    },
                  ],
                },
              ],
            },
          ],
        },
        {
          path: 'container/:clusterName',
          mark: 'clusterContainer',
          routes: [
            {
              path: 'nodes',
              pageNameInfo: ClusterSelector,
              breadcrumbName: `${i18n.t('node')}({params.clusterName})`,
              routes: [
                {
                  layout: { noWrapper: true },
                  getComp: (cb) => cb(import('app/modules/cmp/pages/cluster-container/cluster-nodes')),
                },
                {
                  path: ':nodeId/detail',
                  breadcrumbName: i18n.t('cmp:node detail'),
                  getComp: (cb) => cb(import('app/modules/cmp/pages/cluster-container/cluster-nodes-detail')),
                },
              ],
            },
            {
              path: 'pods',
              pageNameInfo: ClusterSelector,
              breadcrumbName: 'Pods({params.clusterName})',
              routes: [
                {
                  layout: { noWrapper: true },
                  getComp: (cb) => cb(import('app/modules/cmp/pages/cluster-container/cluster-pods')),
                },
                {
                  path: ':podId/detail',
                  breadcrumbName: i18n.t('cmp:pod detail'),
                  getComp: (cb) => cb(import('app/modules/cmp/pages/cluster-container/cluster-pod-detail')),
                },
              ],
            },
            {
              path: 'workload',
              pageNameInfo: ClusterSelector,
              breadcrumbName: `${i18n.t('cmp:Workload')}({params.clusterName})`,
              routes: [
                {
                  layout: { noWrapper: true },
                  getComp: (cb) => cb(import('app/modules/cmp/pages/cluster-container/cluster-workload')),
                },
                {
                  path: ':workloadId/detail',
                  breadcrumbName: i18n.t('cmp:workload detail'),
                  getComp: (cb) => cb(import('app/modules/cmp/pages/cluster-container/cluster-workload-detail')),
                },
              ],
            },
            {
              path: 'event-log',
              pageNameInfo: ClusterSelector,
              breadcrumbName: `${i18n.t('cmp:Event Log')}({params.clusterName})`,
              routes: [
                {
                  layout: { noWrapper: true },
                  getComp: (cb) => cb(import('app/modules/cmp/pages/cluster-container/cluster-event-log')),
                },
              ],
            },
          ],
        },

        {
          path: 'resource-rank',
          routes: [
            {
              path: ':rankType',
              breadcrumbName: i18n.t('resource rank'),
              tabs: resourceRankTabs,
              layout: { noWrapper: true },
              ignoreTabQuery: true,
              getComp: (cb) =>
                cb(import('app/modules/dcos/pages/cluster-dashboard/resources-summary'), 'ResourceTable'),
            },
          ],
        },
        {
          path: 'domain',
          breadcrumbName: i18n.t('domain'),
          getComp: (cb) => cb(import('app/modules/cmp/pages/domain-manage')),
        },
        {
          path: 'services',
          breadcrumbName: i18n.t('Service'),
          getComp: (cb) => cb(import('dcos/pages/service-manager')),
        },
        {
          path: 'jobs',
          breadcrumbName: i18n.t('task'),
          getComp: (cb) => cb(import('app/modules/cmp/pages/tasks/job')),
        },
        {
          path: 'report',
          breadcrumbName: i18n.t('O & M report'),
          routes: [
            {
              path: ':taskId',
              breadcrumbName: '{opReportName}',
              layout: { fullHeight: true },
              getComp: (cb) => cb(import('app/modules/cmp/pages/alarm-report/report-records')),
            },
            {
              getComp: (cb) => cb(import('app/modules/cmp/pages/alarm-report')),
            },
          ],
        },
        {
          path: 'customDashboard',
          breadcrumbName: i18n.t('custom dashboard'),
          routes: [
            {
              path: 'add',
              breadcrumbName: i18n.t('cmp:new O & M dashboard'),
              layout: { fullHeight: true },
              getComp: (cb) => cb(import('cmp/pages/alarm-report/custom-dashboard/custom-dashboard')),
            },
            {
              path: ':dashboardId',
              breadcrumbName: '{dashboardName}',
              layout: { fullHeight: true },
              getComp: (cb) => cb(import('cmp/pages/alarm-report/custom-dashboard/custom-dashboard')),
            },
            {
              getComp: (cb) => cb(import('cmp/pages/alarm-report/custom-dashboard')),
            },
          ],
        },
        {
          path: 'addon',
          routes: [
            {
              breadcrumbName: i18n.t('addon service'),
              getComp: (cb) => cb(import('app/modules/cmp/pages/middleware-dashboard')),
              layout: {
                noWrapper: true,
              },
            },
            {
              path: ':instanceId',
              breadcrumbName: i18n.t('addon detail'),
              routes: [
                {
                  path: 'monitor',
                  tabs: middlewareTabs,
                  alwaysShowTabKey: 'monitor',
                  getComp: (cb) => cb(import('cmp/pages/middleware-detail/monitor')),
                },
                {
                  path: 'resource',
                  tabs: middlewareTabs,
                  alwaysShowTabKey: 'resource',
                  getComp: (cb) => cb(import('app/modules/cmp/pages/middleware-detail/resource')),
                },
                {
                  path: 'detail',
                  tabs: middlewareTabs,
                  alwaysShowTabKey: 'detail',
                  getComp: (cb) => cb(import('app/modules/cmp/pages/middleware-detail/detail')),
                },
              ],
            },
          ],
        },
        {
          path: 'cloudSource',
          breadcrumbName: i18n.t('cloud source'),
          routes: [
            {
              getComp: (cb) => cb(import('app/modules/cmp/pages/cloud-source')),
              layout: {
                noWrapper: true,
              },
            },
            {
              path: 'accounts',
              breadcrumbName: i18n.t('cloud accounts'),
              getComp: (cb) => cb(import('app/modules/cmp/pages/cloud-accounts')),
            },
            {
              path: 'ecs',
              breadcrumbName: 'ECS',
              getComp: (cb) => cb(import('app/modules/cmp/pages/computing/ecs')),
            },
            {
              path: 'vpc',
              breadcrumbName: 'VPC',
              routes: [
                {
                  getComp: (cb) => cb(import('app/modules/cmp/pages/networks/vpc')),
                },
                {
                  path: ':vpcID',
                  routes: [
                    {
                      path: 'vsw',
                      breadcrumbName: 'vsw({params.vpcID})',
                      getComp: (cb) => cb(import('app/modules/cmp/pages/networks/vsw')),
                    },
                  ],
                },
              ],
            },
            {
              path: 'oss',
              breadcrumbName: 'OSS',
              getComp: (cb) => cb(import('app/modules/cmp/pages/storage/oss')),
            },
            {
              path: 'mq',
              breadcrumbName: 'MQ',
              routes: [
                {
                  getComp: (cb) => cb(import('dcos/pages/service-manager/mq')),
                },
                {
                  path: ':mqID/:tabKey',
                  getComp: (cb) => cb(import('dcos/pages/service-manager/mq-manager/index')),
                  breadcrumbName: '{params.mqID}',
                  tabs: map(mqTabs),
                },
              ],
            },
            {
              path: 'rds',
              breadcrumbName: 'RDS',
              routes: [
                {
                  getComp: (cb) => cb(import('dcos/pages/service-manager/rds')),
                },
                {
                  path: ':rdsID/:tabKey',
                  getComp: (cb) => cb(import('dcos/pages/service-manager/rds-manager/index')),
                  breadcrumbName: '{params.rdsID}',
                  tabs: map(rdsTabs),
                },
              ],
            },
            {
              path: 'redis',
              breadcrumbName: 'Redis',
              routes: [
                {
                  getComp: (cb) => cb(import('dcos/pages/service-manager/redis')),
                },
                {
                  path: ':redisID/:tabKey',
                  getComp: (cb) => cb(import('dcos/pages/service-manager/redis-manager/index')),
                  breadcrumbName: '{params.redisID}',
                  tabs: map(redisTabs),
                },
              ],
            },
          ],
        },
        {
          path: 'alarm',
          routes: [
            {
              path: 'report',
              routes: [
                {
                  path: ':clusterName',
                  routes: [
                    {
                      path: ':chartUniqId',
                      routes: [
                        {
                          path: ':processId',
                          breadcrumbName: i18n.t('process detail'),
                          getComp: (cb) => cb(import('dcos/pages/alarm-report/processDetail')),
                        },
                        {
                          breadcrumbName: i18n.t('alarm data report'),
                          getComp: (cb) => cb(import('dcos/pages/alarm-report')),
                        },
                      ],
                    },
                  ],
                },
              ],
            },
            {
              path: 'record',
              breadcrumbName: i18n.t('alarm record'),
              routes: [
                {
                  path: ':recordId',
                  breadcrumbName: '{alarmRecordName}',
                  getComp: (cb) => cb(import('app/modules/cmp/pages/alarm-record/detail')),
                },
                {
                  getComp: (cb) => cb(import('app/modules/cmp/pages/alarm-record')),
                },
              ],
            },
            {
              path: 'statistics',
              breadcrumbName: i18n.t('alarm statistics'),
              getComp: (cb) => cb(import('app/modules/cmp/pages/alarm-analyze')),
              layout: {
                noWrapper: true,
              },
            },
            {
              path: 'strategy',
              breadcrumbName: i18n.t('alarm strategy'),
              routes: [
                {
                  getComp: (cb) => cb(import('app/modules/cmp/pages/alarm-strategy')),
                },
                {
                  path: 'add-strategy',
                  pageNameInfo: AddStrategyPageName,
                  breadcrumbName: i18n.t('cmp:new alarm strategy'),
                  getComp: (cb) => cb(import('app/modules/cmp/pages/alarm-strategy/cmp-stratege')),
                },
                {
                  path: 'edit-strategy/:id',
                  breadcrumbName: i18n.t('cmp:edit alarm strategy'),
                  pageNameInfo: EditStrategyPageName,
                  getComp: (cb) => cb(import('app/modules/cmp/pages/alarm-strategy/cmp-stratege')),
                },
              ],
            },
            {
              path: 'custom',
              breadcrumbName: i18n.t('custom alarm'),
              routes: [
                {
                  path: ':dashboardId',
                  breadcrumbName: '{dashboardName}',
                  layout: { fullHeight: true },
                  getComp: (cb) => cb(import('cmp/pages/alarm-report/custom-dashboard/custom-dashboard')),
                },
                {
                  getComp: (cb) => cb(import('app/modules/cmp/pages/custom-alarm')),
                },
              ],
            },
            {
              path: 'pod',
              breadcrumbName: i18n.t('pod detail'),
              getComp: (cb) => cb(import('app/modules/cmp/pages/pod-detail')),
            },
            {
              path: 'middleware-chart',
              breadcrumbName: i18n.t('middleware chart'),
              getComp: (cb) => cb(import('app/modules/cmp/pages/middleware-chart')),
            },
          ],
        },
      ],
    },
  ];
}

export default getCmpRouter;
