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
import { isEmpty } from 'lodash';
import { Row, Col, Input, Button } from 'antd';
import { IF } from 'common';
import { TimeSelectWithStore } from 'msp/components/time-select';
import apiMonitorFilterStore from '../../stores/filter';
import i18n from 'i18n';

import './filterNav.scss';
import gatewayStore from 'msp/stores/gateway';

interface IProps {
  isNeedStatusFilters?: boolean;
  updateFields: () => void;
  resetFields: () => void;
}

const FilterNav = ({ isNeedStatusFilters = true, updateFields, resetFields }: IProps) => {
  const [runtimeEntryData] = gatewayStore.useStore((s) => [s.runtimeEntryData]);
  const searchFields = apiMonitorFilterStore.useStore((s) => s.searchFields);
  const { filter_hts, filter_upfs, filter_dsrv, filter_dapp } = searchFields;
  const { updateSearchFields } = apiMonitorFilterStore.reducers;

  React.useEffect(() => {
    if (!isEmpty(runtimeEntryData)) {
      const { diceApp, services } = runtimeEntryData;
      const serviceListKeys = Object.keys(services);
      if (!filter_dapp && !filter_dsrv) {
        updateSearchFields({ filter_dapp: diceApp, filter_dsrv: serviceListKeys[0] });
        setTimeout(() => {
          // 当从runtime进入时，图表组件第一次初始化时会拿filter初始值call一次后端，等到runtimeEntryData返回后updateFields时再call一次,
          // 两次间隔时间太短，可能造成前一次覆盖后一次的问题，由于call api封装太深很难使用takeLatest的saga，所以在这里强制等待。
          // TODO 后续转为报表工具后考虑优化
          // TODO 现在如果查询条件不变就无法再次查询需要优化
          updateFields();
        }, 300);
      }
    }
  }, [filter_dapp, filter_dsrv, runtimeEntryData, updateFields, updateSearchFields]);

  const resetAll = () => {
    updateSearchFields({ filter_dapp: undefined, filter_dsrv: undefined });
    resetFields();
  };

  return (
    <div className="api-monitor-filter-nav mb-4">
      <div className="search-container flex justify-between items-center mb-4">
        <div className="search-fields">
          <Row gutter={10} type="flex" justify="end" className="filter-row">
            <IF check={isNeedStatusFilters}>
              <Col span={4}>
                <Input
                  placeholder={i18n.t('msp:http status code')}
                  value={filter_hts}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                    updateSearchFields({ filter_hts: e.target.value });
                  }}
                />
              </Col>
              <Col span={4}>
                <Input
                  placeholder={i18n.t('msp:backend status code')}
                  value={filter_upfs}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                    updateSearchFields({ filter_upfs: e.target.value });
                  }}
                />
              </Col>
            </IF>
          </Row>
        </div>
      </div>
      <div className="filter-container flex justify-between items-center">
        <TimeSelectWithStore />
        <div className="search-actions flex justify-between items-center ml-5">
          <Button className="mr-2" onClick={resetAll}>
            {i18n.t('reset')}
          </Button>
          <Button type="primary" ghost onClick={updateFields}>
            {i18n.t('search')}
          </Button>
        </div>
      </div>
    </div>
  );
};

export default FilterNav;
