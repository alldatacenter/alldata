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

export const chartRender = (obj: any) => {
  if (!obj) return null;
  const reQuery = {
    ...(obj.query || {}),
    dependentKey: {
      start: 'startTimeMs',
      end: 'endTimeMs',
      filter_tk: 'terminusKey',
    },
    filter_type: 'mobile',
  };
  const { fetchApi: api, ...rest } = obj;
  return (props: any) => {
    const { query, fetchApi, ...ownProps } = props;
    const { chartName } = ownProps || {};
    const Chart = ChartBaseFactory.create({ chartName, ...rest });
    const reApi = fetchApi || api;
    const reFetchApi = reApi && `/api/spot/tmc/metrics/${reApi}`;
    return <Chart {...rest} {...ownProps} fetchApi={reFetchApi} query={{ ...query, ...reQuery }} />;
  };
};

export const sortRender = (obj: any) => {
  if (!obj) return null;
  const { query = {}, fetchApi, ...rest } = obj;
  const SortPanel = SortBaseFactory.create({ ...rest });
  query.dependentKey = { start: 'startTimeMs', end: 'endTimeMs', filter_tk: 'terminusKey' };
  query.filter_type = 'mobile';
  return (props: any) => (
    <SortPanel {...props} {...rest} query={query} fetchApi={`/api/spot/tmc/metrics/${fetchApi || ''}`} />
  );
};
