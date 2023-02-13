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

import { Table, Tooltip } from 'antd';
import { Copy, Icon as CustomIcon } from 'common';
import { isEmpty } from 'lodash';
import { getBrowserInfo } from 'common/utils';
import { getFormatter } from 'charts/utils/formatter';
import React from 'react';
import { titleCnMap, titleMap, iconMap } from './config';
import i18n from 'i18n';

import './service-list.scss';

const getImageText = (text) => {
  if (!text) return '';
  const headTxt = text.substr(0, 5);
  const tailTxt = text.substr(5);
  return (
    <div className="image-txt-container truncate">
      <span className="head">{headTxt}</span>
      <span className={`tail nowrap ${getBrowserInfo().isSafari ? 'hack-safari' : ''}`}>{tailTxt}</span>
    </div>
  );
};

const ServiceList = ({ serviceList, depth, into, isFetching, startLevel }) => {
  let cols = [];
  // 某一层id可能一致
  const _list = isEmpty(serviceList) ? [] : serviceList;
  const list = _list.map((item, i) => ({ ...item, id: item.id || item.name || i }));
  let curTitleMap = titleMap;
  let curIconMap = iconMap;
  let totalDepth = 4;
  if (startLevel === 'runtime') {
    curTitleMap = titleMap.slice(1);
    curIconMap = iconMap.slice(1);
    totalDepth = 3;
  }
  const CPU_MEM = [
    {
      title: 'CPU',
      dataIndex: 'cpu',
      key: 'cpu',
      width: 120,
      sorter: (a, b) => Number(a.cpu) - Number(b.cpu),
    },
    {
      title: i18n.t('memory'),
      dataIndex: 'memory',
      key: 'memory',
      width: 120,
      sorter: (a, b) => Number(a.memory) - Number(b.memory),
      render: (size) => getFormatter('STORAGE', 'MB').format(size),
    },
  ];

  if (depth < totalDepth) {
    cols = [
      {
        title: titleCnMap[curTitleMap[depth]],
        dataIndex: 'id',
        key: 'id',
        // width: 420,
        render: (text, record) => (
          <span className="into-link" onClick={() => into({ q: text, name: record.name })}>
            <CustomIcon type={curIconMap[depth]} />
            {record.name}
          </span>
        ),
      },
      {
        title: i18n.t('number of instance'),
        dataIndex: 'instance',
        width: 176,
        sorter: (a, b) => Number(a.instance) - Number(b.instance),
      },
      ...CPU_MEM,
    ];
  } else {
    cols = [
      {
        title: 'IP',
        dataIndex: 'ip_addr',
        key: 'ip_addr',
        width: 120,
        sorter: (c, d) =>
          Number((c.ip_addr || c.ipAddress || '').replace(/\./g, '')) -
          Number((d.ip_addr || d.ipAddress || '').replace(/\./g, '')),
        render: (_ip, record) => record.ip_addr || record.ipAddress || i18n.t('dop:no ip address'),
      },
      {
        title: i18n.t('dop:host address'),
        dataIndex: 'host_private_addr',
        key: 'host_private_addr',
        width: 120,
        render: (_host, record) => record.host_private_addr || record.host || i18n.t('dop:no host address'),
      },
      {
        title: i18n.t('image'),
        dataIndex: 'image',
        key: 'image',
        // className: 'item-image',
        // width: 400,
        render: (text = '') => {
          return (
            text && (
              <Tooltip title={`${i18n.t('click to copy')}：${text}`} overlayClassName="tooltip-word-break">
                <span
                  className="image-name cursor-copy"
                  data-clipboard-tip={i18n.t('dop:image name')}
                  data-clipboard-text={text}
                >
                  {getImageText(text)}
                </span>
              </Tooltip>
            )
          );
        },
      },
      ...CPU_MEM,
    ];
    cols[4] = {
      title: i18n.t('memory'),
      dataIndex: 'memory',
      key: 'memory',
      width: 120,
      sorter: (a, b) => Number(a.memory) - Number(b.memory),
      render: (size) => getFormatter('STORAGE', 'MB').format(size),
    };
  }
  return (
    <div className="monitor-service-table has-into-link">
      <Table
        rowKey={(record, i) => `${i}${record.id}`}
        pagination={false}
        loading={isFetching}
        columns={cols}
        dataSource={list}
        scroll={{ x: 800 }}
      />
      <Copy selector=".cursor-copy" />
    </div>
  );
};

export default ServiceList;
