/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { CloudDownloadOutlined } from '@ant-design/icons';
import { Badge, Tooltip, TooltipProps } from 'antd';
import { Popup } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import useMount from 'app/hooks/useMount';
import { FC, ReactElement, useEffect, useMemo, useState } from 'react';
import { DownloadTask, DownloadTaskState } from '../../slice/types';
import { DownloadList } from './DownloadList';
import { OnLoadTasksType } from './types';
interface DownloadListPopupProps {
  tooltipProps?: TooltipProps;
  polling: boolean;
  renderDom?: ReactElement;
  onLoadTasks: OnLoadTasksType<any>;
  setPolling: (polling: boolean) => void;
  onDownloadFile: (task: DownloadTask) => void;
}
const DOWNLOAD_POLLING_INTERVAL = 5000;
export const DownloadListPopup: FC<DownloadListPopupProps> = ({
  tooltipProps,
  polling,
  renderDom,
  setPolling,
  onLoadTasks,
  onDownloadFile,
}) => {
  const [tasks, setTasks] = useState<DownloadTask[]>([]);
  const t = useI18NPrefix('main.nav');

  const downloadableNum = useMemo(() => {
    return (tasks || []).filter(v => v.status === DownloadTaskState.DONE)
      .length;
  }, [tasks]);

  useEffect(() => {
    let id;
    if (polling && typeof id !== 'number') {
      onLoadTasks().then(({ isNeedStopPolling, data }) => {
        setTasks(data);
        if (!isNeedStopPolling) {
          id = setInterval(() => {
            onLoadTasks().then(({ isNeedStopPolling, data }) => {
              setTasks(data);
              if (isNeedStopPolling) {
                clearInterval(id);
                setPolling(false);
              }
            });
          }, DOWNLOAD_POLLING_INTERVAL);
        } else {
          setPolling(false);
        }
      });
    } else if (typeof id === 'number') {
      typeof id === 'number' && clearInterval(id);
    }
    return () => {
      typeof id === 'number' && clearInterval(id);
    };
  }, [polling, setPolling, onLoadTasks]);
  useMount(() => {
    setPolling(true);
  });

  return (
    <Popup
      content={<DownloadList onDownloadFile={onDownloadFile} tasks={tasks} />}
      trigger={['click']}
      placement="rightBottom"
    >
      <li>
        <Tooltip
          title={t('download.title')}
          placement="right"
          {...tooltipProps}
        >
          <Badge count={downloadableNum}>
            {renderDom || <CloudDownloadOutlined style={{ fontSize: 20 }} />}
          </Badge>
        </Tooltip>
      </li>
    </Popup>
  );
};

export type { OnLoadTasksType } from './types';
