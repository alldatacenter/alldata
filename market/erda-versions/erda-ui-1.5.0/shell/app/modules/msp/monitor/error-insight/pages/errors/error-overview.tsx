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

/* eslint-disable react-hooks/exhaustive-deps */
import React from 'react';
import { map } from 'lodash';
import { commonChartRender } from 'monitor-common';
import { groupHandler } from 'common/utils/chart-utils';
import ErrorCard from './error-card';
import ErrorFilters from './error-filters';
import routeInfoStore from 'core/stores/route';
import monitorErrorStore from 'error-insight/stores/error';
import monitorCommonStore from 'common/stores/monitorCommon';
import { useLoading } from 'core/stores/loading';
import { Pagination, Spin } from 'antd';
import { EmptyHolder } from 'common';
import i18n from 'i18n';

import './error-overview.scss';

const pageSize = 20;

const errorChartConfig = {
  fetchApi: '/api/tmc/metrics/error_count/histogram',
  query: { sum: 'count' },
  dataHandler: groupHandler('sum.count'),
  moduleName: 'monitorErrors',
  titleText: i18n.t('msp:error statistics'),
  chartName: 'error_count',
};

const ErrorChart = commonChartRender(errorChartConfig) as any;

const ErrorOverview = () => {
  const timeSpan = monitorCommonStore.useStore((s) => s.globalTimeSelectSpan.range);
  const [loading] = useLoading(monitorErrorStore, ['getErrorsList']);
  const errors = monitorErrorStore.useStore((s) => s.errors);
  const { getErrorsList } = monitorErrorStore.effects;
  const { clearMonitorErrors } = monitorErrorStore.reducers;
  const { projectId, terminusKey, env } = routeInfoStore.useStore((s) => s.params);
  const [pageNo, setPageNo] = React.useState(1);

  React.useEffect(() => {
    clearMonitorErrors();
    const { startTimeMs, endTimeMs } = timeSpan;
    getErrorsList({ startTime: startTimeMs, endTime: endTimeMs, scopeId: terminusKey });
    setPageNo(1);
  }, [terminusKey, timeSpan]);

  const handleChangePage = (page: number) => {
    setPageNo(page || 1);
  };

  const total = errors?.length || 0;
  const currentPageList = React.useMemo(() => {
    return (errors || []).slice((pageNo - 1) * pageSize, pageNo * pageSize);
  }, [errors, pageNo]);
  // 当env为空时，不可查询
  // shell/app/modules/msp/monitor/monitor-common/components/chartFactory.tsx
  // 临时处理：上面引用的 chartFactory 有个循环渲染的 bug， 受影响的目前只有这一处：
  const query = {
    query: { filter_workspace: env, filter_project_id: projectId, filter_terminus_key: terminusKey },
  };
  return (
    <div className="error-overview">
      <Spin spinning={loading}>
        <ErrorFilters />
        <ErrorChart {...query} />
        <div className="page-total">{`${i18n.t('msp:total number of errors')}：${total}`}</div>
        {map(currentPageList, (err, i) => (
          <ErrorCard key={i} data={err} />
        ))}
        {total ? (
          <div className="mt-4 flex items-center flex-wrap justify-end">
            <Pagination current={pageNo} pageSize={pageSize} total={total} onChange={handleChangePage} />
          </div>
        ) : (
          <EmptyHolder relative />
        )}
      </Spin>
    </div>
  );
};

export default ErrorOverview;
