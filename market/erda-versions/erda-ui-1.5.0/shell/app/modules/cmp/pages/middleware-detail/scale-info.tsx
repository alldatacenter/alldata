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
import { Spin, Button, Col, Row, Tooltip } from 'antd';
import { get } from 'lodash';
import ScaleModal from 'cmp/pages/middleware-dashboard/scale-modal';
import middlewareDashboardStore from 'cmp/stores/middleware-dashboard';
import { useLoading } from 'core/stores/loading';

interface IProps {
  data: Merge<MIDDLEWARE_DASHBOARD.IMiddleBase, { name: string; projectID: string; projectName: string }>;
}

const ScaleInfo = ({ data }: IProps) => {
  const [visible, setVisible] = React.useState(false);
  const addonConfig = middlewareDashboardStore.getState((s) => s.addonConfig);
  const [isLoading] = useLoading(middlewareDashboardStore, ['getConfig']);
  const baseInfo = React.useMemo(() => {
    const { mem, nodes, cpu } = addonConfig;
    const { name } = data;
    return { mem, nodes, cpu, name };
  }, [addonConfig, data]);

  const items = [
    {
      title: i18n.t('default:name'),
      dataIndex: 'name',
      render: (text: string) => <Tooltip title={text}>{text}</Tooltip>,
    },
    {
      title: i18n.t('cmp:number of nodes'),
      dataIndex: 'nodes',
    },
    {
      title: `CPU(${i18n.t('default:core')})`,
      dataIndex: 'cpu',
    },
    {
      title: 'MEM(MB)',
      dataIndex: 'mem',
    },
  ];
  const fetchData = () => {
    middlewareDashboardStore.effects.getConfig({ addonID: data.addonID });
  };
  return (
    <Spin spinning={isLoading}>
      <div className="scale-info mb-8">
        <div className="flex justify-between items-center">
          <span className="title font-medium">{i18n.t('cmp:resource information')}</span>
          <Button
            onClick={() => {
              setVisible(true);
            }}
          >
            {i18n.t('default:edit')}
          </Button>
        </div>
        <Row>
          {items.map((item) => {
            const text = get(baseInfo, item.dataIndex, '--');
            return (
              <Col span={6} key={item.title}>
                <div className="param-k nowrap">{item.title}</div>
                <div className="param-v nowrap">{item.render ? item.render(text) : text}</div>
              </Col>
            );
          })}
        </Row>
        <ScaleModal
          visible={visible}
          formData={{ ...baseInfo, ...data }}
          onCancel={() => setVisible(false)}
          afterSubmit={fetchData}
        />
      </div>
    </Spin>
  );
};

export default React.memo(ScaleInfo);
