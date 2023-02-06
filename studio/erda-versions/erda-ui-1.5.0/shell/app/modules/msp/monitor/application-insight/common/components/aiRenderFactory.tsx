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
import { ChartBaseFactory, SortBaseFactory } from 'monitor-common';

const filterKeyMapping = {
  AIOverview: 'filter_target_terminus_key',
  AIWeb: 'filter_target_terminus_key',
  AIRPC: 'filter_target_terminus_key',
  AIDataBase: 'filter_source_terminus_key',
  AICache: 'filter_source_terminus_key',
};

export const chartRender = (obj: any) => {
  if (!obj) return null;
  const filterKey = filterKeyMapping[obj.model] || 'filter_terminus_key';
  const filterKeyParam = { [filterKey]: 'terminusKey' };
  const reQuery = { ...(obj.query || {}), dependentKey: { start: 'startTimeMs', end: 'endTimeMs', ...filterKeyParam } };
  const { fetchApi: api, ...rest } = obj;
  const Chart = ChartBaseFactory.create({ ...rest });
  return (props: any) => {
    const { query, fetchApi, ...ownProps } = props;
    const reApi = fetchApi || api;
    const reFetchApi = reApi && `/api/spot/tmc/metrics/${reApi}`;
    return <Chart {...rest} {...ownProps} fetchApi={reFetchApi} query={{ ...query, ...reQuery }} />;
  };
};

export const sortRender = (obj: any) => {
  if (!obj) return null;

  const { query = {}, fetchApi, moduleName, ...rest } = obj;
  const filterKey = filterKeyMapping[moduleName] || 'filter_terminus_key';
  const filterKeyParam = { [filterKey]: 'terminusKey' };
  const SortPanel = SortBaseFactory.create({ moduleName, ...rest });
  query.dependentKey = { start: 'startTimeMs', end: 'endTimeMs', ...filterKeyParam };
  return (props: any) => {
    const { query: propsQuery } = props;
    return (
      <SortPanel
        {...props}
        {...rest}
        query={{ ...query, ...propsQuery }}
        fetchApi={`/api/spot/tmc/metrics/${fetchApi || ''}`}
      />
    );
  };
};
