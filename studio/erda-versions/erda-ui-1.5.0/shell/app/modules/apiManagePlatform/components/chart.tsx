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

import React from 'react';
import { Spin } from 'antd';
import { CommonRangePicker, BoardGrid } from 'common';
import { useUpdate } from 'common/use-hooks';
import { getDashboard } from 'apiManagePlatform/services/api-access';
import { get, isString, merge, reduce, values, isEmpty } from 'lodash';
import { getTimeSpan } from 'common/utils';
import { getVariableStr } from 'cmp/common/utils';
import { createLoadDataFn } from 'cmp/common/custom-dashboard/data-loader';

interface IState {
  layout: Array<Record<string, any>>;
  boardConfig: Array<Record<string, any>>;
  timeSpan: ITimeSpan;
  loading: boolean;
}

interface IProps {
  type: API_ACCESS.DashboardType;
  extraQuery?: Record<string, any>;
}

const Chart = ({ type, extraQuery = {} }: IProps) => {
  const [{ layout, boardConfig, timeSpan, loading }, updater] = useUpdate<IState>({
    layout: [],
    boardConfig: [],
    timeSpan: getTimeSpan(),
    loading: false,
  });
  React.useEffect(() => {
    updater.loading(true);
    getDashboard({ type })
      .then(({ data }: any) => updater.boardConfig(data.viewConfig || []))
      .finally(() => {
        updater.loading(false);
      });
  }, [type, updater]);
  React.useEffect(() => {
    updater.timeSpan(getTimeSpan());
  }, [extraQuery, updater]);
  const query = React.useMemo(() => {
    return {
      ...extraQuery,
      start: timeSpan.startTimeMs,
      end: timeSpan.endTimeMs,
    };
  }, [extraQuery, timeSpan.endTimeMs, timeSpan.startTimeMs]);
  React.useEffect(() => {
    const { start, end, ...restQuery } = query;
    const flag = !isEmpty(restQuery) && values(restQuery).every((t) => !!t);
    if (!flag) {
      return;
    }
    const _layout = boardConfig.map((viewItem) => {
      const filters = get(viewItem, 'view.api.extraData.filters');
      const _viewItem = merge({}, viewItem, {
        view: {
          api: {
            query: {
              start,
              end,
              ...reduce(
                filters,
                (acc, { value, method, tag }) => {
                  const matchQuery = isString(value) ? getVariableStr(value) : undefined;
                  return {
                    ...acc,
                    [`${method}_${tag}`]: matchQuery ? restQuery[matchQuery] : value.split(','),
                  };
                },
                {},
              ),
            },
          },
        },
      });
      const { api, chartType } = _viewItem.view as any;

      return merge({}, viewItem, { view: { loadData: createLoadDataFn(api, chartType) } });
    });
    updater.layout(_layout);
  }, [boardConfig, query, updater]);

  return (
    <Spin spinning={loading}>
      <CommonRangePicker
        className="mb-3"
        defaultTime={[timeSpan.startTimeMs, timeSpan.endTimeMs]}
        onOk={(v) => updater.timeSpan(v)}
      />
      <BoardGrid.Pure layout={layout} />
    </Spin>
  );
};

export default Chart;
