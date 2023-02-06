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

import { getTraceDetail, getSpanDetail } from '../services/trace-detail';
import { isEmpty } from 'lodash';
import traceConvert from '../common/utils/traceConvert';
import { createStore } from 'core/cube';

const transformTrace = (trace: MONITOR_TRACE.ITrace) => {
  if (isEmpty(trace)) return {};
  const traceDetail = traceConvert(trace);
  traceDetail.spans?.forEach((i) => {
    // eslint-disable-next-line
    i.isExpand = i.isShow = true;
  });
  return traceDetail;
};

interface IState {
  traceDetail: MONITOR_TRACE.ITraceDetail;
  spanDetail: {
    visible: boolean;
    span: any;
  };
}

const initState = {
  traceDetail: {},
  spanDetail: {
    visible: false,
    span: {},
  },
} as IState;

const traceDetail = createStore({
  name: 'traceDetail',
  state: initState,
  effects: {
    async getTraceDetail({ call, update, getParams }, payload: Omit<MONITOR_TRACE.IQuerySpan, 'scopeId'>) {
      const { terminusKey } = getParams();
      const response = await call(getTraceDetail, { ...payload, scopeId: terminusKey });
      update({ traceDetail: transformTrace(response) as MONITOR_TRACE.ITraceDetail });
    },
    async getSpanDetail({ call, update }, payload) {
      const response = await call(getSpanDetail, payload);
      const annotations = response.span.annotations || [];
      // 接口返回timestamp为毫秒，duration为微秒，统一为微秒
      const spanDetail = {
        ...response,
        annotations: annotations.map((item: any) => {
          return { ...item, timestamp: item.timestamp * 1000 };
        }),
      };
      update({ spanDetail });
    },
  },
});

export default traceDetail;
