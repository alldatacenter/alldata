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
import classNames from 'classnames';
import { map as _map } from 'lodash';
import { Spin, Tooltip } from 'antd';
import { EmptyHolder } from 'common';
import './trace-history-list.scss';

const TraceHistoryList = ({
  dataSource = [],
  isFetching,
  currentTraceRequestId,
  getTraceDetail,
  getTraceStatusDetail,
  setCurrentTraceRequestId,
  clearTraceStatusDetail,
  clearCurrentTraceRequestId,
  clearRequestTraceParams,
  setInputUrl,
  form,
}: any) => {
  if (!dataSource.length) {
    return <EmptyHolder />;
  }

  const handleClick = (requestId: string, url: string) => {
    const isNewId = currentTraceRequestId !== requestId;
    // 点击已选中的 item 取消选中，currentTraceRequestId 置空
    setCurrentTraceRequestId(isNewId ? requestId : '');
    const requestItem = dataSource.find((item) => item.requestId === requestId);
    const startTime = new Date(requestItem.updateTime).getTime();
    if (isNewId) {
      form.setFieldsValue({ url });
      setInputUrl(url);
      getTraceDetail({ requestId });
      getTraceStatusDetail({ requestId, startTime });
    } else {
      form.setFieldsValue({ url: '' });
      setInputUrl('');
      clearRequestTraceParams();
      clearCurrentTraceRequestId();
      clearTraceStatusDetail();
    }
  };

  const renderListItem = ({ requestId, url, createTime }: any) => {
    const isActive = currentTraceRequestId === requestId;
    return (
      <li
        onClick={() => handleClick(requestId, url)}
        className={classNames({
          'trace-history-list-item': true,
          active: isActive,
        })}
      >
        <span className="request-url nowrap">{url}</span>
        <span className="request-time">{createTime}</span>
      </li>
    );
  };

  const renderList = () => {
    return (
      <ul className="trace-history-list">
        {_map(dataSource, (item) => {
          const { requestId, url } = item;
          return (
            <Tooltip key={requestId} title={url}>
              {renderListItem(item)}
            </Tooltip>
          );
        })}
      </ul>
    );
  };

  return (
    <div className="trace-history">
      <Spin spinning={isFetching}>{renderList()}</Spin>
    </div>
  );
};

export default TraceHistoryList;
