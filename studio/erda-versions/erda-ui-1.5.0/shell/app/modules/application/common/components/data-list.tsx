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
import { Select, Input, Button, Spin } from 'antd';
import { isEmpty, get } from 'lodash';
import { LoadMore, Holder, Icon as CustomIcon } from 'common';
import { connectCube } from 'common/utils';
import { useEffectOnce, useUpdateEffect } from 'react-use';
import i18n from 'i18n';
import dataTaskStore from 'application/stores/dataTask';

import './data-list.scss';

interface IProps {
  compName: string;
  businessScope: { businessDomain: any; dataDomains: any[]; marketDomains: any[] };
  getBusinessScope: Function;
  clearBusinessScope: Function;
  getList: any;
  isFetching: boolean;
  list: any[];
  listPaging: IPaging;
  pageConfig: { iconType: string; domainName: string; domainPlaceholder: string };
  onItemClick: (params: any) => Function;
}

const { Option } = Select;

const DataList = (props: IProps) => {
  const [searchKey, setSearchKey] = React.useState('');
  const [selectedDomain, setSelectedDomain] = React.useState();
  const { businessScope, isFetching, list: businessProcessList, listPaging, pageConfig, onItemClick, getList } = props;
  const { businessDomain = {}, dataDomains = [], marketDomains = [] } = businessScope;
  const { iconType, domainName, domainPlaceholder } = pageConfig;
  const domainSource = domainName === 'dataDomain' ? dataDomains : marketDomains;

  const onSearch = (_event: any, isReset?: boolean) => {
    getList({
      [domainName]: isReset ? undefined : selectedDomain,
      businessDomain: businessDomain.enName,
      searchKey: isReset ? '' : searchKey,
      pageNo: 1,
    });
  };

  useEffectOnce(() => {
    const { getBusinessScope, clearBusinessScope, compName } = props;
    getBusinessScope({ compName });
    return () => {
      clearBusinessScope({ compName });
    };
  });

  useUpdateEffect(() => {
    !isEmpty(props.businessScope) && onSearch(undefined, true);
  }, [props.businessScope]);

  const onReset = () => {
    setSearchKey('');
    setSelectedDomain(undefined);
    onSearch(undefined, true);
  };

  const onSearchKeyChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const value = get(event, 'target.value');
    setSearchKey(value);
  };

  const onSelectDataDomain = (value: string) => {
    setSelectedDomain(value);
  };

  const getDataList = () => {
    // load more has to return a promise
    return getList({ [domainName]: selectedDomain, businessDomain: businessDomain.enName, searchKey });
  };

  return (
    <div className="data-list">
      <section className="header flex justify-between items-center">
        <div className="header-left">
          <Select
            className="data-select"
            placeholder={i18n.t('dop:select a business domain')}
            value={!isEmpty(businessDomain) ? businessDomain.enName : ''}
          >
            {!isEmpty(businessDomain) && <Option value={businessDomain.enName}>{businessDomain.cnName}</Option>}
          </Select>
          <Select
            className="data-select"
            placeholder={domainPlaceholder}
            value={selectedDomain}
            onChange={onSelectDataDomain}
          >
            {domainSource.map((domain: any) => (
              <Option key={domain.enName} value={domain.enName}>
                {domain.cnName}
              </Option>
            ))}
          </Select>
          <Input
            className="data-select"
            value={searchKey}
            onChange={onSearchKeyChange}
            placeholder={i18n.t('dop:chinese/english search for input form')}
            onPressEnter={onSearch}
          />
        </div>
        <div className="header-right">
          <Button onClick={onReset}>{i18n.t('reset')}</Button>
          <Button type="primary" ghost onClick={onSearch}>
            {i18n.t('search')}
          </Button>
        </div>
      </section>
      <section className="data-list-content">
        <Spin spinning={isFetching}>
          <Holder when={!businessProcessList.length}>
            <ul>
              {businessProcessList.map((item: any) => {
                return (
                  <li key={item.enName} className="model-item">
                    <div className="item-container" onClick={() => onItemClick(item)}>
                      <div className="item-header">
                        <CustomIcon type={iconType} />
                        <span>{item.cnName}</span>
                        <span>{item.enName}</span>
                      </div>
                      <div className="item-footer flex justify-between items-center">
                        <span className="nowrap">{item.desc}</span>
                        <span className="item-table nowrap">{item.table}</span>
                      </div>
                    </div>
                  </li>
                );
              })}
            </ul>
          </Holder>
          <LoadMore load={getDataList} hasMore={listPaging.hasMore} initialLoad={false} isLoading={isFetching} />
        </Spin>
      </section>
    </div>
  );
};

const Mapper = () => {
  const { getBusinessScope } = dataTaskStore.effects;
  const { clearBusinessScope } = dataTaskStore.reducers;

  return {
    getBusinessScope,
    clearBusinessScope,
  };
};

const DataListWrapper = connectCube(DataList, Mapper);
export { DataListWrapper as DataList };
