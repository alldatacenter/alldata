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

import { slice, values, map, keys, isEmpty } from 'lodash';
import i18n from 'i18n';
import React from 'react';
import { IF, MonitorChart, TimeSelector } from 'common';

import './monitor-chart-panel.scss';

interface IProps {
  resourceType: string;
  resourceId: string;
  metrics: any;
  commonChartQuery?: object;
  defaultTime?: number;
}

const MonitorChartPanel = (props: IProps) => {
  const [showNumber, setShowNumber] = React.useState(4);
  const { resourceType, resourceId, metrics, commonChartQuery = {}, defaultTime } = props;
  if (!resourceType || !resourceId) {
    return null;
  }
  const displayMetrics = slice(values(metrics), 0, showNumber);
  return (
    <>
      <div className="monitor-chart-time-container">
        <TimeSelector defaultTime={defaultTime} />
      </div>
      <div className="monitor-chart-panel">
        {map(displayMetrics, ({ parameters, name: metricKey, title, metricName, unit, unitType }) => {
          const chartQuery = {
            ...commonChartQuery,
            fetchMetricKey: metricKey,
          };
          if (!isEmpty(parameters)) {
            map(Object.keys(parameters), (key) => {
              chartQuery[key] = parameters[key];
            });
          }
          return (
            <div className="monitor-chart-cell spin-full-height" key={metricKey}>
              <MonitorChart
                {...{ resourceType, resourceId }}
                title={title || metricName}
                metricKey={metricKey}
                metricUnitType={unitType}
                metricUnit={unit}
                chartQuery={chartQuery}
              />
            </div>
          );
        })}
        <IF check={keys(metrics).length > showNumber}>
          <div className="show-all" onClick={() => setShowNumber((prevNumber) => prevNumber + 4)}>
            {i18n.t('load more')}
          </div>
        </IF>
      </div>
    </>
  );
};

export default MonitorChartPanel;
