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
import { ChartBaseFactory } from 'monitor-common';

export const chartRender = (obj: any) => {
  if (!obj) return null;
  const reQuery = { ...(obj.query || {}), dependentKey: { start: 'startTimeMs', end: 'endTimeMs' } };
  const { fetchApi: api, isCustomApi = false, ...rest } = obj;
  const Chart = ChartBaseFactory.create({ ...rest });
  return (props: any) => {
    const { query, fetchApi, ...ownProps } = props;
    const reApi = fetchApi || api;
    const reFetchApi = isCustomApi ? reApi : reApi && `/api/spot/metrics/charts/${reApi}`;
    return <Chart {...rest} {...ownProps} fetchApi={reFetchApi} query={{ ...query, ...reQuery }} />;
  };
};
