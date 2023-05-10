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
import { TimeSelector, Copy } from 'common';
import { useUpdate } from 'common/use-hooks';
import authenticateStroe from '../../stores/authenticate';
import moment from 'moment';
import i18n from 'i18n';
import { Tooltip, Spin, Table, Select } from 'antd';
import { WithAuth, usePerm } from 'user/common';
import { useLoading } from 'core/stores/loading';
import monitorCommonStore from 'common/stores/monitorCommon';
import publisherStore from 'publisher/stores/publisher';
import { map } from 'lodash';

interface IProps {
  artifacts: PUBLISHER.IArtifacts;
}

const Authenticate = (props: IProps) => {
  const { artifacts } = props;
  const timeSpan = monitorCommonStore.useStore((s) => s.timeSpan);
  const publishItemMonitors = publisherStore.useStore((s) => s.publishItemMonitors);
  const artifactId = artifacts.id;
  const [loading] = useLoading(authenticateStroe, ['getList']);
  const publishOperationAuth = usePerm((s) => s.org.publisher.operation.pass);
  const [{ selectMonitorKey }, updater] = useUpdate({
    selectMonitorKey: Object.keys(publishItemMonitors)[0],
  });

  const monitorKey = React.useMemo(() => {
    const { ak, ai } = publishItemMonitors[selectMonitorKey] || {};
    return { ak, ai };
  }, [publishItemMonitors, selectMonitorKey]);

  const query = React.useMemo(() => {
    const { startTimeMs, endTimeMs } = timeSpan;
    return { artifactId, start: startTimeMs, end: endTimeMs, ...monitorKey };
  }, [timeSpan, artifactId, monitorKey]);
  const { getList, addBlackList, addErase } = authenticateStroe.effects;
  const list = authenticateStroe.useStore((s) => s.list);

  const getData = React.useCallback(
    (q: any) => {
      getList(q);
    },
    [getList],
  );

  React.useEffect(() => {
    getData(query);
  }, [getData, query]);

  const columns = [
    {
      title: i18n.t('user ID'),
      dataIndex: 'userId',
      render: (v: string) => (
        <Tooltip title={v}>
          <span className="cursor-copy" data-clipboard-tip={i18n.t('user ID')} data-clipboard-text={v}>
            {v}
          </span>
        </Tooltip>
      ),
    },
    {
      title: i18n.t('user name'),
      dataIndex: 'userName',
      render: (val: string) => (
        <span className="cursor-copy" data-clipboard-tip={i18n.t('user name')} data-clipboard-text={val}>
          {val}
        </span>
      ),
    },
    {
      title: i18n.t('device ID'),
      dataIndex: 'deviceNo',
      render: (v: string) => (
        <Tooltip title={v}>
          <span className="cursor-copy" data-clipboard-tip={i18n.t('device ID')} data-clipboard-text={v}>
            {v}
          </span>
        </Tooltip>
      ),
    },
    {
      title: i18n.t('last login time'),
      dataIndex: 'lastLoginTime',
      width: 200,
      render: (v: string) => (v ? moment(v).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: i18n.t('operation'),
      dataIndex: 'operation',
      width: 160,
      render: (_v: unknown, record: PUBLISHER.IAuthenticate) => {
        const { deviceNo } = record;
        return (
          <div className="table-operations">
            <WithAuth pass={publishOperationAuth}>
              <span
                className="table-operations-btn"
                onClick={() => {
                  addBlackList({ artifactId, ...record });
                }}
              >
                {i18n.t('publisher:blacklisted')}
              </span>
            </WithAuth>

            <WithAuth pass={publishOperationAuth}>
              <span
                className="table-operations-btn"
                onClick={() => {
                  addErase({ artifactId, deviceNo });
                }}
              >
                {i18n.t('publisher:erase data')}
              </span>
            </WithAuth>
          </div>
        );
      },
    },
  ];
  return (
    <div>
      <div className="flex items-center mb-4 justify-start">
        <TimeSelector className="ml-0" key="time-selector" inline disabledDate={() => false} />
        <Select
          value={selectMonitorKey}
          style={{ width: 200 }}
          className="ml-3"
          onChange={(k) => {
            updater.selectMonitorKey(k);
          }}
        >
          {map(publishItemMonitors, (_, key) => (
            <Select.Option key={key} value={key}>
              {key}
            </Select.Option>
          ))}
        </Select>
      </div>
      <Spin spinning={loading}>
        <Table rowKey={'userId'} columns={columns} dataSource={list} scroll={{ x: '100%' }} />
      </Spin>
      <Copy selector=".cursor-copy" />
    </div>
  );
};

export default Authenticate;
