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

import { map, get, keys } from 'lodash';
import { BoardGrid } from 'common';
import React from 'react';
import { colorMap } from 'project/utils/test-case';
import i18n from 'i18n';
import testPlanStore from 'project/stores/test-plan';

const nameMaps = {
  init: i18n.t('dop:not performed'),
  block: i18n.t('dop:blocking'),
  fail: i18n.t('dop:not passed'),
  succ: i18n.t('dop:pass'),
};

const ChartsResult = () => {
  const [s, setS] = React.useState(0);
  const { relsCount = {} } = testPlanStore.useStore((state) => state.planReport);

  React.useEffect(() => {
    setTimeout(() => setS(1), 0);
  });

  const layout = React.useMemo(() => {
    const nameMap = {};
    const color: string[] = [];
    const names: string[] = [];
    const data: Array<{ name: string; value: number }> = [];
    map(keys(nameMaps), (key) => {
      const value = get(relsCount, key);
      if (value) {
        const name = nameMaps[key];
        nameMap[key] = nameMaps[key];
        color.push(colorMap[name]);
        names.push(name);
        data.push({ name, value });
      }
    });
    const staticData = {
      legendData: names,
      metricData: [{ name: i18n.t('dop:results of the'), data }],
      extraOption: {
        color,
      },
    };
    return [
      {
        w: 24,
        h: 10,
        x: 0,
        y: 0,
        i: 'view-pie',
        moved: false,
        static: false,
        view: {
          name: i18n.t('dop:use case execution result distribution'),
          chartType: 'chart:pie',
          // hideHeader: true,
          staticData,
          config: {
            option: {
              legend: {
                orient: 'horizontal',
                x: 'center',
                y: 'bottom',
              },
              color,
              series: [
                {
                  center: ['50%', '45%'],
                  radius: ['0%', '55%'],
                  label: {
                    formatter: '{b}: {c} ({d}%)',
                  },
                },
              ],
            },
          },
        },
      },
    ];
  }, [relsCount]);

  if (s === 0) {
    return null;
  }
  return <BoardGrid.Pure layout={layout} />;
};

export default ChartsResult;
