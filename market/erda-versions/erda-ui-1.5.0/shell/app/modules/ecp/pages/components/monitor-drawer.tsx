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
import ResourceUsageCharts from 'monitor-common/components/resource-usage/resource-usage-charts';
import Terminal from 'dcos/common/containers/terminal';
import ContainerLog from 'runtime/common/logs/containers/container-log';
import { useUpdate } from 'common/use-hooks';
import i18n from 'i18n';

export enum OPERATION {
  TERMINAL = 'terminal',
  LOG = 'log',
  MONITOR = 'monitor',
}
interface IMonitorDrawer {
  visible: boolean;
  data: MACHINE_MANAGE.IMonitorInfo;
  onClose: () => void;
}
export const MonitorDrawer = (props: IMonitorDrawer) => {
  const [{ slideTitle, content, delayRender }, updater, update] = useUpdate({
    slideTitle: '',
    content: null,
    delayRender: null,
  });
  const { visible, onClose, data: record } = props;

  const { instance, type } = record;

  const openSlidePanel = React.useCallback(() => {
    switch (type) {
      case OPERATION.TERMINAL: {
        update({
          slideTitle: i18n.t('console'),
          content: (
            <Terminal
              containerId={instance.containerId}
              host={instance.hostIP}
              clusterName={instance.clusterName}
              instanceTerminal
              {...record}
            />
          ),
        });
        break;
      }
      case OPERATION.MONITOR: {
        update({
          slideTitle: i18n.t('monitor'),
          delayRender: () => <ResourceUsageCharts {...record} instance={instance} />,
        });
        break;
      }
      case OPERATION.LOG: {
        update({
          slideTitle: i18n.t('cmp:container log'),
          content: <ContainerLog {...record} instance={{ ...instance }} />,
        });
        break;
      }
      default:
    }
  }, [instance, record, type, update]);

  React.useEffect(() => {
    if (visible) {
      openSlidePanel();
    }
  }, [openSlidePanel, visible]);

  React.useEffect(() => {
    if (visible && delayRender) {
      updater.content(delayRender);
      updater.delayRender(null);
    }
  }, [delayRender, updater, visible]);

  return (
    <Drawer width="80%" visible={visible} title={slideTitle} destroyOnClose onClose={onClose}>
      {content}
    </Drawer>
  );
};
