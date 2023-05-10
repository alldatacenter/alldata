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

import i18n from 'i18n';
import { goTo, insertWhen } from 'common/utils';
import { filterMenu, MENU_SCOPE } from './util';
import React from 'react';
import { ErdaIcon } from 'common';
import { EMPTY_CLUSTER } from 'cmp/pages/cluster-manage/config';

export const getCmpMenu = (chosenCluster = EMPTY_CLUSTER) => {
  return filterMenu(
    [
      {
        key: 'cmpOverview',
        href: goTo.resolve.cmpRoot(),
        icon: <ErdaIcon type="data-display" />,
        text: i18n.t('cluster overview'),
        subtitle: i18n.t('Overview'),
      },
      {
        key: 'cmpResources',
        icon: <ErdaIcon type="data-all" />,
        href: goTo.resolve.cmpClusters(),
        text: i18n.t('cluster resource'),
        subtitle: i18n.t('Resource'),
        subMenu: [
          {
            key: 'cmpCluster',
            href: goTo.resolve.cmpClusters(),
            text: i18n.t('clusters'),
          },
          {
            key: 'cmpResourceRank',
            href: goTo.resolve.cmpResourceProjectRank(),
            text: i18n.t('resource rank'),
            prefix: `${goTo.resolve.cmpResourceRank()}/`,
          },
          {
            key: 'cmpCloudSource',
            href: goTo.resolve.cloudSource(),
            text: i18n.t('cloud source'),
          },
        ],
      },
      {
        key: 'containerResource',
        icon: <ErdaIcon type="cloud-container" />,
        href: goTo.resolve.cmpClustersContainer({ clusterName: chosenCluster }),
        text: i18n.t('container resource'),
        subtitle: i18n.t('container'),
        subMenu: [
          {
            key: 'clusterNodes',
            href: goTo.resolve.cmpClustersNodes({ clusterName: chosenCluster }),
            text: i18n.t('node'),
          },
          {
            key: 'clusterPod',
            href: goTo.resolve.cmpClustersPods({ clusterName: chosenCluster }),
            text: 'Pods',
          },
          {
            key: 'clusterWorkload',
            href: goTo.resolve.cmpClustersWorkload({ clusterName: chosenCluster }),
            text: i18n.t('cmp:Workload'),
          },
          {
            key: 'clusterNodes',
            href: goTo.resolve.cmpClustersEventLog({ clusterName: chosenCluster }),
            text: i18n.t('cmp:Event Log'),
          },
        ],
      },
      {
        key: 'cmpServices',
        icon: <ErdaIcon type="list-two" />,
        href: goTo.resolve.cmpDomain(),
        text: i18n.t('application resource'),
        subtitle: i18n.t('App'),
        subMenu: [
          {
            key: 'cmpResources',
            href: goTo.resolve.cmpDomain(), // '/cmp/domain',
            text: i18n.t('domain'),
          },
          {
            href: goTo.resolve.cmpServices(), // '/cmp/services',
            text: i18n.t('Service'),
          },
          {
            href: goTo.resolve.cmpJobs(), // '/cmp/jobs',
            text: i18n.t('task'),
          },
          ...insertWhen(!process.env.FOR_COMMUNITY, [
            {
              href: goTo.resolve.cmpAddon(), // '/cmp/addon',
              text: i18n.t('addon service'),
            },
          ]),
        ],
      },
      {
        key: 'cmpReport',
        href: goTo.resolve.cmpReport(), // '/cmp/report',
        icon: <ErdaIcon type="data-file" />,
        text: i18n.t('O & M report'),
        subtitle: i18n.t('Report'),
      },
      {
        key: 'cmpAlarm',
        href: goTo.resolve.cmpAlarm(), // '/cmp/alarm',
        icon: <ErdaIcon type="database-alert" />,
        text: i18n.t('O & M alarm'),
        subtitle: i18n.t('Alarm'),
        subMenu: [
          {
            text: i18n.t('alarm statistics'),
            href: goTo.resolve.cmpAlarmStatistics(), // '/cmp/alarm/statistics',
          },
          {
            text: i18n.t('alarm record'),
            href: goTo.resolve.cmpAlarmRecord(), // '/cmp/alarm/record',
          },
          {
            text: i18n.t('alarm strategy'),
            href: goTo.resolve.cmpAlarmStrategy(), // '/cmp/alarm/strategy',
          },
          {
            text: i18n.t('custom alarm'),
            href: goTo.resolve.cmpAlarmCustom(), // '/cmp/alarm/custom',
          },
        ],
      },
      {
        key: 'cmpDashboard',
        href: goTo.resolve.orgCustomDashboard(), // '/cmp/customDashboard',
        icon: <ErdaIcon type="dashboard-car" />,
        text: i18n.t('custom dashboard'),
        subtitle: i18n.t('Dashboard'),
      },
      // {
      //   key: 'cmpLog',
      //   href: goTo.resolve.cmpLog(), // '/cmp/log',
      //   icon: <IconLog />,
      //   text: i18n.t('log analysis'),
      //   subMenu: [
      //     {
      //       text: i18n.t('log query'),
      //       href: goTo.resolve.cmpLogQuery(), // '/cmp/log/query',
      //     },
      //     {
      //       text: i18n.t('analysis rule'),
      //       href: goTo.resolve.cmpLogRule(), // '/cmp/log/rule',
      //     },
      //   ],
      // },
    ],
    MENU_SCOPE.cmp,
  );
};
