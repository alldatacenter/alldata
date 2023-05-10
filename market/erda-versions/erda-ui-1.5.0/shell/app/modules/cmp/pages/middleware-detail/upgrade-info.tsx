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
import { Spin, Button, Radio, Table, Tooltip } from 'antd';
import { map, isEmpty } from 'lodash';
import { RadioChangeEvent, ColumnProps } from 'core/common/interface';
import UpgradeModal from 'cmp/pages/middleware-dashboard/upgrade-modal';
import middlewareDashboardStore from 'cmp/stores/middleware-dashboard';
import { useLoading } from 'core/stores/loading';
import { Holder, Icon } from 'common';
import './index.scss';

interface IProps {
  data: Merge<MIDDLEWARE_DASHBOARD.IMiddleBase, { name: string }>;
}

const translateData = (data: { [k: string]: string }) => {
  const arr = [];
  // eslint-disable-next-line guard-for-in
  for (const key in data) {
    arr.push({
      key,
      value: data[key],
    });
  }
  return arr;
};

const TableView = React.memo(({ data }: { data: any }) => {
  const columns: Array<ColumnProps<any>> = [
    {
      title: 'Key',
      dataIndex: 'key',
    },
    {
      title: 'Value',
      dataIndex: 'value',
    },
  ];

  return <Table columns={columns} dataSource={translateData(data)} pagination={false} scroll={{ x: '100%' }} />;
});

const TextView = React.memo(({ data }: { data: Record<string, any> }) => {
  return (
    <div className="text-view">
      <Holder when={isEmpty(data)}>
        {map(data, (value, key) => {
          return (
            <p key={key}>
              {key}: {value}
            </p>
          );
        })}
      </Holder>
    </div>
  );
});

const UpgradeInfo = ({ data }: IProps) => {
  const [visible, setVisible] = React.useState(false);
  const [mode, setMode] = React.useState('key-value');
  const { config = {} } = middlewareDashboardStore.useStore((s) => s.addonConfig);
  const [isLoading] = useLoading(middlewareDashboardStore, ['getConfig']);
  const viewCompMap = React.useMemo(() => {
    return {
      'key-value': <TableView data={config} />,
      text: <TextView data={config} />,
    };
  }, [config]);
  const fetchData = () => {
    middlewareDashboardStore.effects.getConfig({ addonID: data.addonID });
  };
  return (
    <Spin spinning={isLoading}>
      <div className="upgrade-info mb-8">
        <div className="flex justify-between items-center">
          <span className="title font-medium">
            {i18n.t('default:configuration information')}
            <Tooltip title={i18n.t('cmp:please fill in the real configuration information')}>
              <Icon className="ml-2" type="tishi" />
            </Tooltip>
          </span>
          <div>
            <Button
              className="mr-2"
              onClick={() => {
                setVisible(true);
              }}
            >
              {i18n.t('default:edit')}
            </Button>
            <Radio.Group
              value={mode}
              onChange={(e: RadioChangeEvent) => {
                setMode(e.target.value);
              }}
            >
              <Radio.Button value="key-value">{i18n.t('default:configuration mode')}</Radio.Button>
              <Radio.Button value="text">{i18n.t('default:text mode')}</Radio.Button>
            </Radio.Group>
          </div>
        </div>
        {viewCompMap[mode]}
        <UpgradeModal
          visible={visible}
          formData={data}
          dataSource={config}
          onCancel={() => {
            setVisible(false);
          }}
          afterSubmit={fetchData}
        />
      </div>
    </Spin>
  );
};

export default React.memo(UpgradeInfo);
