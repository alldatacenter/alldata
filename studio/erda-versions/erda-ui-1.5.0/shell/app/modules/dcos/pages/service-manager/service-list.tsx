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

import { Table, Tooltip, Badge } from 'antd';
import i18n from 'i18n';
import { Copy, Icon as CustomIcon, IF } from 'common';
import { get, set, remove, round } from 'lodash';
import { getFormatter } from 'charts/utils/formatter';
import React from 'react';
import { getBrowserInfo } from 'common/utils';
import { ColumnProps } from 'core/common/interface';
import { useInstanceOperation } from 'app/modules/cmp/common/components/instance-operation';
import './service-list.scss';

const statusConfig = {
  Unknown: { text: i18n.t('unknown'), state: 'warning' },
  CONTAINER: {
    Stopped: { text: i18n.t('ready'), state: 'error' },
    Killed: { text: 'Killed', state: 'error' },
    Failed: { text: i18n.t('startup failed'), state: 'error' },
    Starting: { text: i18n.t('executing'), state: 'processing' },
    Unknown: { text: i18n.t('failed'), state: 'warning' },
    UnHealthy: { text: i18n.t('error'), state: 'warning' },
    Healthy: { text: i18n.t('cmp:health'), state: 'success' },
    Finished: { text: i18n.t('unknown'), state: 'default' },
    Running: { text: i18n.t('running'), state: 'processing' },
    OOM: { text: 'OOM', state: 'warning' },
  },
  RUNTIME: {
    Init: { text: i18n.t('initializing'), state: 'processing' },
    Progressing: { text: i18n.t('deploying'), state: 'processing' },
    UnHealthy: { text: i18n.t('unhealthy'), state: 'warning' },
    Healthy: { text: i18n.t('cmp:health'), state: 'success' },
  },
  SERVICE: {
    Progressing: { text: i18n.t('deploying'), state: 'processing' },
    UnHealthy: { text: i18n.t('unhealthy'), state: 'warning' },
    Healthy: { text: i18n.t('cmp:health'), state: 'success' },
  },
};

const compareClass = (rate: number) => {
  if (rate > 100) {
    // 超过百分百
    return 'color-crash';
  } else if (rate > 60 && rate < 80) {
    return 'text-warning';
  } else if (rate >= 80) {
    return 'text-danger';
  }
  return 'text-success';
};

const countPercent = (used: number, total: number) => {
  const percent = total && round((used / total) * 100, 2);
  const statusClass = compareClass(percent || 0);
  return { percent, statusClass };
};

const getImageText = (text: string) => {
  const headTxt = text.substr(0, 5);
  const tailTxt = text.substr(5);
  return (
    <div className="image-txt-container truncate">
      <span className="head">{headTxt}</span>
      <span className={`tail nowrap ${getBrowserInfo().isSafari ? 'hack-safari' : ''}`}>{tailTxt}</span>
    </div>
  );
};

const getMetricsInfo = (obj: any, key: string) => obj.metrics && obj.metrics[key];

const ProgressItem = (percent: number, used: number, total: number, unit: string, color: string) => (
  <Tooltip
    overlayClassName="service-table-progress"
    title={() => {
      const _used = unit ? `${used} ${unit}` : getFormatter('STORAGE').format(used, 2);
      const _total = unit ? `${total} ${unit}` : getFormatter('STORAGE').format(total, 2);
      return (
        <div className="table-tooltip">
          {`${i18n.t('cmp:usage amount')}：`}
          <span className={color}>{_used}</span> <br />
          {`${i18n.t('cmp:assignment')}：${_total}`}
        </div>
      );
    }}
  >
    <div className="table-percent">{`${percent}%`}</div>
  </Tooltip>
);

interface IProps {
  containerList: object[];
  serviceList: object[];
  depth: number;
  haveMetrics: boolean;
  haveHost: boolean;
  haveStatus?: boolean;
  extraQuery: {
    filter_cluster_name: string;
  };
  into: (data: { q: string; name: string }) => void;
}

interface IInstance {
  [prop: string]: any;
  containerId: string;
  id: string;
  status: string;
  host: string;
}

function ServiceList({
  containerList,
  serviceList,
  depth,
  into,
  haveMetrics,
  haveHost = true,
  haveStatus = true,
  extraQuery,
}: IProps) {
  const [renderOp, drawer] = useInstanceOperation<IInstance>({
    log: true,
    console: true,
    monitor: true,
    getProps(type, record) {
      return {
        console: {
          host: record.host_private_addr || record.host,
          clusterName: extraQuery.filter_cluster_name,
        },
        log: {
          fetchApi: '/api/orgCenter/logs',
          extraQuery: { clusterName: record.clusterName },
        },
        monitor: {
          api: '/api/orgCenter/metrics',
          extraQuery,
        },
      }[type];
    },
  });

  let list = [];
  let cols = [];
  if (depth && depth < 5) {
    list = serviceList;
    // 设一个id作为rocord的key，避免切换rowKey时新数据还没拿到引起rowKey和record的key不一致
    list.forEach((item: any, i: number) => {
      set(item, 'id', item.id || item.name || i);
    });
    const titleMap = ['', 'PROJECT', 'APPLICATION', 'RUNTIME', 'SERVICE', 'CONTAINER'];
    const titleCnMap = {
      '': '',
      PROJECT: i18n.t('project'),
      APPLICATION: i18n.t('application'),
      RUNTIME: i18n.t('application instance'),
      SERVICE: i18n.t('microService'),
      CONTAINER: 'CONTAINER',
    };
    const iconMap = ['', 'project', 'wenjianjia', 'fengchao', 'atom'];

    cols = [
      {
        title: titleCnMap[titleMap[depth]],
        dataIndex: 'id',
        key: 'id',
        // width: 320,
        render: (text: string, record: any) => (
          <span className="font-bold hover-table-text" onClick={() => into({ q: text, name: record.name })}>
            <CustomIcon type={iconMap[depth]} />
            {record.name}
          </span>
        ),
      },
      {
        title: i18n.t('number of instance'),
        dataIndex: 'instance',
        key: 'instance',
        width: 176,
      },
      {
        title: 'CPU',
        dataIndex: 'cpu',
        key: 'cpu',
        width: 125,
        sorter: (a: any, b: any) => {
          if (!haveMetrics) return Number(a.cpu) - Number(b.cpu);
          const use_a = getMetricsInfo(a, 'cpuUsagePercent') || 0;
          const use_b = getMetricsInfo(b, 'cpuUsagePercent') || 0;
          return Number(use_a / a.cpu) - Number(use_b / b.cpu);
        },
        render: (total: number, record: any) => {
          if (!haveMetrics) return `${total} ${i18n.t('core')}`;
          const parsedTotal = total * 1000;
          const used = +(getMetricsInfo(record, 'cpuUsagePercent') || 0).toFixed(4) * 1000;
          const { percent, statusClass } = countPercent(used, parsedTotal);
          return ProgressItem(percent, round(used, 2), parsedTotal, i18n.t('millicore'), statusClass);
        },
      },
      {
        title: i18n.t('memory'),
        dataIndex: 'memory',
        key: 'memory',
        width: 125,
        sorter: (a: any, b: any) => {
          if (!haveMetrics) return Number(a.memory) - Number(b.memory);
          const use_a = getMetricsInfo(a, 'memUsage') || 0;
          const use_b = getMetricsInfo(b, 'memUsage') || 0;
          return Number(use_a / 1048576 / a.memory) - Number(use_b / 1048576 / b.memory);
        },
        render: (total: number, record: any) => {
          if (!haveMetrics) return `${total} MB`;
          const used = +((getMetricsInfo(record, 'memUsage') || 0) / 1048576).toFixed(2);
          const { percent, statusClass } = countPercent(used, total);
          return ProgressItem(percent, used, total, 'MB', statusClass);
        },
      },
      {
        title: i18n.t('disk'),
        dataIndex: 'disk',
        key: 'disk',
        width: 125,
        sorter: (a: any, b: any) => Number(a.disk) - Number(b.disk),
        render: (size: string) => getFormatter('STORAGE', 'MB').format(size),
      },
      {
        title: i18n.t('status'),
        dataIndex: 'unhealthy',
        width: 100,
        align: 'center',
        render: (num: number) => (
          <span>
            <IF check={!!num}>
              <Badge status="error" /> {num}
              <IF.ELSE />
              <Badge status="success" />
            </IF>
          </span>
        ),
      },
    ];
    // if (includes(['RUNTIME', 'SERVICE'], titleMap[depth])) { // runtime和service级别，需要展示状态
    //   (cols as any[]).push({
    //     title: '状态',
    //     dataIndex: 'status',
    //     width: 100,
    //     align: 'center',
    //     render: (text: string) => {
    //       const stateObj = get(statusConfig, `${titleMap[depth]}.${text}`) || statusConfig.Unknown;
    //       return <Tooltip title={text || 'Unknown'}><Badge status={stateObj.state} /></Tooltip>;
    //     },
    //   });
    // }
  } else {
    list = containerList;
    cols = [
      {
        title: 'IP',
        key: 'ip_addr',
        width: 120,
        sorter: (a: any, b: any) =>
          Number((a.ip_addr || a.ipAddress || '').replace(/\./g, '')) -
          Number((b.ip_addr || b.ipAddress || '').replace(/\./g, '')),
        render: (record: any) => record.ip_addr || record.ipAddress || i18n.t('cmp:no ip address'),
      },
      haveHost
        ? {
            title: i18n.t('cmp:host address'),
            key: 'host_private_addr',
            width: 120,
            render: (record: any) => record.host_private_addr || record.host || i18n.t('cmp:no host address'),
          }
        : null,
      {
        title: i18n.t('image'),
        key: 'image',
        // width: 400,
        className: 'item-image',
        render: (record: any) => {
          const text = record.image_name || record.image;
          if (!text) {
            return null;
          }
          return (
            <Tooltip title={`${i18n.t('click to copy')}:${text}`} overlayClassName="tooltip-word-break">
              <span
                className="image-name for-copy-image"
                data-clipboard-tip={i18n.t('image name')}
                data-clipboard-text={text}
              >
                {getImageText(text)}
              </span>
            </Tooltip>
          );
        },
      },
      {
        title: 'CPU',
        dataIndex: 'cpu',
        key: 'cpu',
        width: 120,
        sorter: (a: any, b: any) => {
          if (!haveMetrics) return Number(a.cpu) - Number(b.cpu);
          const use_a = getMetricsInfo(a, 'cpuUsagePercent') || 0;
          const use_b = getMetricsInfo(b, 'cpuUsagePercent') || 0;
          return Number(use_a / a.cpu) - Number(use_b / b.cpu);
        },
        render: (total: number, record: any) => {
          if (!haveMetrics) return total;
          const parsedTotal = total * 1000;
          const used = +(getMetricsInfo(record, 'cpuUsagePercent') || 0).toFixed(4) * 1000;
          const { percent, statusClass } = countPercent(used, parsedTotal);
          return ProgressItem(percent, round(used, 2), parsedTotal, i18n.t('millicore'), statusClass);
        },
      },
      {
        title: i18n.t('memory'),
        dataIndex: 'memory',
        key: 'memory',
        width: 120,
        sorter: (a: any, b: any) => {
          if (!haveMetrics) return Number(a.memory) - Number(b.memory);
          const use_a = getMetricsInfo(a, 'memUsage') || 0;
          const use_b = getMetricsInfo(b, 'memUsage') || 0;
          return Number(use_a / 1048576 / a.memory) - Number(use_b / 1048576 / b.memory);
        },
        render: (total: number, record: any) => {
          if (!haveMetrics) return getFormatter('STORAGE', 'MB').format(total);
          const used = getMetricsInfo(record, 'memUsage') || 0;
          const { percent, statusClass } = countPercent(used, total);
          return ProgressItem(percent, used, total, '', statusClass);
        },
      },
      {
        title: i18n.t('disk'),
        dataIndex: 'disk',
        key: 'disk',
        width: 120,
        sorter: (a: any, b: any) => Number(a.disk) - Number(b.disk),
        render: (size: number) => getFormatter('STORAGE', 'MB').format(size),
      },
      // TODO: 集群组件目前无状态，3.5暂时去除，后续提供后打开
      haveStatus
        ? {
            title: i18n.t('status'),
            dataIndex: 'status',
            key: 'status',
            width: 100,
            align: 'center',
            render: (text: string) => {
              const stateObj = get(statusConfig, `CONTAINER.${text}`) || statusConfig.Unknown;
              return (
                <Tooltip title={text || 'Unknown'}>
                  <Badge status={stateObj.state} />
                </Tooltip>
              );
            },
          }
        : null,
      {
        title: i18n.t('operations'),
        key: 'operation',
        width: 180,
        render: renderOp,
      },
    ];
    if (depth === 3) {
      cols.splice(2, {
        title: i18n.t('number of instance'),
        dataIndex: 'instance',
        key: 'instance',
      } as any);
    }
  }
  remove(cols as any, (item) => item === null);

  return (
    <div className="service-table">
      <Table
        rowKey={(record: any, i: number) => `${i}${record.id}`}
        pagination={false}
        columns={cols as Array<ColumnProps<any>>}
        dataSource={list}
        scroll={{ x: 1100 }}
      />
      <Copy selector=".for-copy-image" />
      {drawer}
    </div>
  );
}

export default ServiceList;
