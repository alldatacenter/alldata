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
import PureTraceDetail from '../components/trace-detail';
import traceDetailStore from 'trace-insight/stores/trace-detail';
import { useLoading } from 'core/stores/loading';
import routeInfoStore from 'core/stores/route';
import { useMount } from 'react-use';

interface IProps {
  traceId?: string;
}

const TraceDetail = ({ traceId }: IProps) => {
  const params = routeInfoStore.useStore((s) => s.params);
  const [trace, spanDetail] = traceDetailStore.useStore((s) => [s.traceDetail, s.spanDetail]);
  const { getTraceDetail, getSpanDetail: viewSpanDetail } = traceDetailStore.effects;
  const [isFetching] = useLoading(traceDetailStore, ['getTraceDetail']);

  useMount(() => {
    getTraceDetail({ traceId: traceId || params.traceId });
  });

  const props = {
    trace,
    spanDetail,
    getTraceDetail,
    viewSpanDetail,
    isFetching,
    params,
    traceId,
  };

  return <PureTraceDetail {...props} />;
};

export default TraceDetail;
