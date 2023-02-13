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
import { Drawer } from 'antd';
import i18n from 'i18n';
import { IF } from 'common';
import { useUpdate } from 'common/use-hooks';
import Terminal from 'dcos/common/containers/terminal';
import ContainerLog from 'runtime/common/logs/containers/container-log';
import ResourceUsageCharts from 'monitor-common/components/resource-usage/resource-usage-charts';

import './instance-operation.scss';

export enum OPERATION {
  CONSOLE = 'console',
  LOG = 'log',
  MONITOR = 'monitor',
}

interface Instance {
  [k: string]: any;
  instanceId?: string;
  containerId?: string;
  host?: string;
  clusterName?: string;
  hostIP?: string;
}

interface IProps<T> {
  console?: boolean;
  log?: boolean;
  monitor?: boolean;
  getProps: (type: OPERATION, record: T) => Obj;
}

/**
 * 实例操作集合
 * @param console 是否有控制台
 * @param log 是否有日志
 * @param monitor 是否有监控
 * @param getProps(type, record) 返回组件要额外传递的props
 *
 * @return [renderOp, drawer]
 */
export function useInstanceOperation<T extends Instance>({
  console: _console,
  log,
  monitor,
  getProps,
}: IProps<T>): [(record: T) => JSX.Element, JSX.Element] {
  const [state, updater, update] = useUpdate({
    visible: false,
    slideTitle: '',
    content: null,
    delayRender: null,
  });

  const openSlidePanel = (type: OPERATION, record: T) => {
    const props = getProps(type, record) || {};
    switch (type) {
      case OPERATION.CONSOLE: {
        update({
          visible: true,
          slideTitle: i18n.t('console'),
          content: (
            <Terminal
              containerId={record.containerId}
              host={record.hostIP}
              clusterName={record.clusterName}
              instanceTerminal
              {...props}
            />
          ),
        });
        break;
      }
      case OPERATION.MONITOR: {
        update({
          visible: true,
          slideTitle: i18n.t('monitor'),
          delayRender: () => (
            <ResourceUsageCharts
              instance={record}
              extraQuery={{ filter_cluster_name: record.clusterName }}
              {...props}
            />
          ),
        });
        break;
      }
      case OPERATION.LOG: {
        update({
          visible: true,
          slideTitle: i18n.t('cmp:container log'),
          content: <ContainerLog instance={{ ...record }} {...props} />,
        });
        break;
      }
      default:
    }
  };

  const renderOp = (record: T) => {
    return (
      <div className="table-operations">
        <IF check={_console}>
          <span className="table-operations-btn" onClick={() => openSlidePanel(OPERATION.CONSOLE, { ...record })}>
            {i18n.t('console')}
          </span>
        </IF>
        <IF check={monitor}>
          <span className="table-operations-btn" onClick={() => openSlidePanel(OPERATION.MONITOR, { ...record })}>
            {i18n.t('container monitor')}
          </span>
        </IF>
        <IF check={log}>
          <span className="table-operations-btn" onClick={() => openSlidePanel(OPERATION.LOG, { ...record })}>
            {i18n.t('log')}
          </span>
        </IF>
      </div>
    );
  };

  const drawerComp = React.useMemo(() => {
    const afterVisibleChange = (visible: boolean) => {
      if (visible && state.delayRender) {
        updater.content(state.delayRender);
      }
    };

    const closeSlidePanel = () => {
      update({ visible: false, slideTitle: '', content: null, delayRender: null });
    };
    return (
      <Drawer
        width="80%"
        title={state.slideTitle}
        visible={state.visible}
        destroyOnClose
        onClose={closeSlidePanel}
        afterVisibleChange={afterVisibleChange}
      >
        {state.content}
      </Drawer>
    );
  }, [state.content, state.delayRender, state.slideTitle, state.visible, update, updater]);

  return [renderOp, drawerComp];
}
