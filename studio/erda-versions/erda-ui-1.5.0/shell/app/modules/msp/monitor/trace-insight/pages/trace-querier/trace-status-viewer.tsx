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
import { Tooltip } from 'antd';
import { isEmpty } from 'lodash';
import { Icon as CustomIcon, EmptyHolder, ErdaIcon } from 'common';
import { notify } from 'common/utils';
import PureTraceDetail from './trace-detail-new';
import i18n from 'i18n';
import './trace-status-viewer.scss';

const TraceStatusViewer = ({
  traceStatusDetail,
  cancelTraceStatus,
  spanDetailContent,
  traceDetailContent,
  isTraceDetailContentFetching,
  getSpanDetailContent,
}: any) => {
  if (isEmpty(traceStatusDetail)) {
    return <EmptyHolder />;
  }

  const { requestId, status, statusName } = traceStatusDetail;

  const handleCancelRequestPending = (id: string) => {
    try {
      cancelTraceStatus({ requestId: id });
    } catch (error) {
      notify('error', error);
    }
  };

  // 状态，0：初始化、1：成功、2：失败、3：主动取消 对应 statusNodeList 数组下标
  const statusNodeList = [
    <div className="request-status-wp pending">
      <span className="request-status-text flex">
        <ErdaIcon className="mr-2" type="loading" size="16" /> {statusName}
      </span>
      <Tooltip title={i18n.t('cancel')}>
        <span
          className="request-status-btn"
          onClick={(e) => {
            e.stopPropagation();
            handleCancelRequestPending(requestId);
          }}
        >
          <ErdaIcon className="mt-1" type="pause-one" size="20px" />
        </span>
      </Tooltip>
    </div>,
    <PureTraceDetail
      spanDetailContent={spanDetailContent}
      traceDetailContent={traceDetailContent}
      isTraceDetailContentFetching={isTraceDetailContentFetching}
      getSpanDetailContent={getSpanDetailContent}
    />,
    <div className="request-status-wp failure">
      <span className="request-status-text">
        <CustomIcon className="failure" type="guanbi-fill" /> {statusName}
      </span>
    </div>,
    <div className="request-status-wp cancel">
      <span className="flex items-center request-status-text">
        <ErdaIcon size="16" className="mr-1" type="reduce-one" /> {statusName}
      </span>
    </div>,
  ];
  return <div className="trace-status-viewer">{statusNodeList[status]}</div>;
};

export default TraceStatusViewer;
