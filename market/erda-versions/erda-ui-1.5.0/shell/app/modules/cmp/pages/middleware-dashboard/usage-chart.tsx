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

import { BoardGrid } from 'common';
import i18n from 'i18n';
import { map } from 'lodash';
import React from 'react';
import { useMount } from 'react-use';
import middlewareDashboardStore from '../../stores/middleware-dashboard';
import './index.scss';

export const AddonUsageChart = () => {
  const [addonUsage, addonDailyUsage, overview] = middlewareDashboardStore.useStore((s) => [
    s.addonUsage,
    s.addonDailyUsage,
    s.overview,
  ]);
  const { getAddonUsage, getAddonDailyUsage } = middlewareDashboardStore.effects;

  useMount(() => {
    getAddonUsage();
    getAddonDailyUsage();
  });

  const cpuAndMemUsage = React.useMemo(() => {
    return ['cpu', 'mem'].map((type) => {
      const legendData: string[] = [];
      const series = {
        name: i18n.t('cmp:usage proportion'),
        data: [] as any[],
        radius: '60%',
      };
      const metricData = [];
      map(addonUsage, (v, k) => {
        legendData.push(k);
        series.data.push({ value: v[type], name: k });
      });
      if (legendData.length) {
        metricData.push(series);
      }
      return {
        legendData,
        metricData,
      };
    });
  }, [addonUsage]);

  const dailyUsage = React.useMemo(() => {
    const data = {
      xData: addonDailyUsage.abscissa,
      metricData: [] as any[],
    };
    if (addonDailyUsage.abscissa && addonDailyUsage.abscissa.length) {
      ['cpu', 'mem'].forEach((type, i) => {
        data.metricData.push({
          name: type,
          type: 'line',
          axisIndex: i,
          data: addonDailyUsage.resource.map((l) => l[type]),
        });
      });
    }
    return data;
  }, [addonDailyUsage]);

  const layout = [
    {
      w: 8,
      h: 9,
      x: 0,
      y: 0,
      i: 'cpu',
      moved: false,
      static: false,
      view: {
        title: `${i18n.t('cmp:cpu usage proportion')}(${i18n.t('dop:total')}: ${overview.cpu} Core)`,
        chartType: 'chart:pie',
        hideReload: true,
        staticData: cpuAndMemUsage[0],
        config: {
          option: {
            legend: {
              show: false,
            },
            tooltip: {
              formatter: '{a} <br/>{b}: {c} Core ({d}%)',
              confine: true,
            },
          },
        },
      },
    },
    {
      w: 8,
      h: 9,
      x: 8,
      y: 0,
      i: 'mem',
      moved: false,
      static: false,
      view: {
        title: `${i18n.t('cmp:memory usage proportion')}(${i18n.t('dop:total')}: ${overview.mem} GiB)`,
        chartType: 'chart:pie',
        hideReload: true,
        staticData: cpuAndMemUsage[1],
        config: {
          option: {
            legend: {
              show: false,
            },
            tooltip: {
              formatter: '{a} <br/>{b}: {c} GiB ({d}%)',
              confine: true,
            },
          },
        },
      },
    },
    {
      w: 8,
      h: 9,
      x: 16,
      y: 0,
      i: 'daily',
      moved: false,
      static: false,
      view: {
        title: i18n.t('cmp:daily addon usage'),
        chartType: 'chart:line',
        hideReload: true,
        staticData: dailyUsage,
        config: {
          option: {
            yAxis: [
              {
                type: 'value',
                name: 'CPU(Core)',
              },
              {
                type: 'value',
                name: 'Memory(GiB)',
                nameTextStyle: {
                  padding: [0, 10, 0, 0],
                },
                splitLine: {
                  show: false,
                },
              },
            ],
          },
        },
      },
    },
  ];

  return <BoardGrid.Pure layout={layout} />;
};
