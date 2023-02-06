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
import i18n from 'i18n';
import { Timeline, Drawer, Tooltip } from 'antd';
import buildStore from 'application/stores/build';
import { useLoading } from 'core/stores/loading';
import { isEmpty } from 'lodash';
import { useEffectOnce, useUpdateEffect } from 'react-use';
import { EmptyHolder, Title, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import './pipeline-log.scss';

let timer: any;
const DURATION = 10000; // 10秒刷新一次

const delayGetList = (fn: Function, duration = DURATION) => {
  timer = setTimeout(fn, duration);
};
interface IProps {
  isBuilding: boolean;
  resourceId: string;
  resourceType: string;
  className?: string;
}

const colorMap = {
  success: 'green',
  error: 'red',
  info: 'gray',
};

const PipelineLog = ({ isBuilding = false, resourceId, resourceType, className = '' }: IProps) => {
  const { getPipelineLog } = buildStore.effects;
  const { clearPipelineLog } = buildStore.reducers;
  const pipelineLog = buildStore.useStore((s) => s.pipelineLog);
  const [isFetching] = useLoading(buildStore, ['getPipelineLog']);
  const [{ detailLog, detailVis }, , update] = useUpdate({
    detailLog: '',
    detailVis: false,
  });

  useEffectOnce(() => {
    return () => {
      clearTimeout(timer);
      clearPipelineLog();
    };
  });

  const getList = () => {
    clearTimeout(timer);
    if (detailVis) return;
    resourceId &&
      getPipelineLog({ resourceId, resourceType }).then(() => {
        if (isBuilding) {
          timer = setTimeout(getList, DURATION);
        }
      });
  };

  React.useEffect(() => {
    if (resourceId) {
      getList();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [resourceId]);

  useUpdateEffect(() => {
    (!detailVis || isBuilding) && delayGetList(getList); // 详情关闭，或状态改变后，启动刷新
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [detailVis, isBuilding]);

  const logOperation = [
    {
      title: isFetching ? (
        <ErdaIcon type="loading" color="black-400" spin className="mr-1.5" />
      ) : (
        <Tooltip
          title={
            isBuilding
              ? `${i18n.t('dop:refresh every {time}, click to refresh now', {
                  time: `${DURATION / 1000} ${i18n.t('common:second(s)')}`,
                })}`
              : i18n.t('refresh')
          }
        >
          <ErdaIcon
            color="black-400"
            size="18"
            type="redo"
            className="mr-1 cursor-pointer"
            onClick={() => delayGetList(getList, 0)}
          />
        </Tooltip>
      ),
    },
  ];

  return (
    <div className={`pipeline-log ${className}`}>
      <Title
        title={i18n.t('deployment log')}
        className="my-3"
        level={2}
        showDivider={false}
        operations={logOperation}
      />
      {isEmpty(pipelineLog) ? (
        <EmptyHolder relative />
      ) : (
        <Timeline>
          {pipelineLog.map((item, index) => {
            const { occurrenceTime, humanLog, primevalLog, level } = item;
            return (
              <Timeline.Item key={`${String(index)}-${occurrenceTime}`} color={colorMap[level]}>
                <div className={'pipeline-log-time'}>
                  <div className="mb-2">{occurrenceTime}</div>
                  <div className="pipeline-log-title flex items-start">
                    <span className="flex-1">{humanLog}</span>
                    <span
                      className="text-primary cursor-pointer ml-2"
                      onClick={() => update({ detailVis: true, detailLog: primevalLog })}
                    >
                      {i18n.t('check detail')}
                    </span>
                  </div>
                </div>
              </Timeline.Item>
            );
          })}
        </Timeline>
      )}
      <Drawer
        title={i18n.t('detail')}
        visible={detailVis}
        width={800}
        onClose={() => {
          update({ detailVis: false, detailLog: '' });
        }}
      >
        <div className="pipeline-log-detail">{detailLog}</div>
      </Drawer>
    </div>
  );
};

export default PipelineLog;
