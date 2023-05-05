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
import { Collapse, Radio, Row, Col, Table, Empty, Input } from 'antd';
import i18n from 'i18n';
import './sla-select.scss';
import { slaAuthorizationMap, slaUnitMap } from 'apiManagePlatform/pages/access-manage/components/config';

interface IProps {
  defaultSelectKey?: number;
  dataSource: API_ACCESS.SlaItem[];
  onChange?: (data: number) => void;
}
const { Panel } = Collapse;

const SLASelect = ({ dataSource, onChange, defaultSelectKey }: IProps) => {
  const [activeKey, setActiveKey] = React.useState<number | undefined>(undefined);
  const [filter, setFilter] = React.useState<string>('');
  React.useEffect(() => {
    setActiveKey(defaultSelectKey);
  }, [defaultSelectKey]);
  const handleSearch = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFilter(e.target.value);
  };
  const handleChange = (key: string) => {
    if (key) {
      setActiveKey(+key);
      onChange && onChange(+key);
    }
  };
  const filterData = React.useMemo(() => {
    return dataSource.filter((item) => item.name.toLowerCase().includes(filter.toLocaleLowerCase()));
  }, [filter, dataSource]);
  return (
    <>
      <Input.Search
        onChange={handleSearch}
        className="mb-3"
        allowClear
        placeholder={i18n.t('filter by {name}', { name: i18n.t('SLA name') })}
      />
      {filterData.length ? (
        <Collapse
          className="sla-select"
          accordion
          expandIcon={({ panelKey }) => <Radio className="pt-1.5" checked={+panelKey === activeKey} />}
          onChange={handleChange}
          activeKey={activeKey}
        >
          {filterData.map((item) => {
            const limits = item.limits || [];
            const header = (
              <Row>
                <Col span={12}>
                  {i18n.t('SLA name')}: {item.name}
                </Col>
                <Col span={12}>
                  {i18n.t('authorization method')}: {slaAuthorizationMap[item.approval]?.name}
                </Col>
              </Row>
            );
            return (
              <Panel header={header} key={item.id}>
                <Table
                  pagination={false}
                  dataSource={limits}
                  scroll={limits.length > 4 ? { y: 150, x: 800 } : { x: 800 }}
                  columns={[
                    {
                      title: i18n.t('times'),
                      dataIndex: 'limit',
                      width: 320,
                    },
                    {
                      title: i18n.t('unit'),
                      dataIndex: 'unit',
                      render: (unit) => slaUnitMap[unit],
                    },
                  ]}
                />
              </Panel>
            );
          })}
        </Collapse>
      ) : (
        <div className="sla-select">
          <Empty />
        </div>
      )}
    </>
  );
};

export default SLASelect;
