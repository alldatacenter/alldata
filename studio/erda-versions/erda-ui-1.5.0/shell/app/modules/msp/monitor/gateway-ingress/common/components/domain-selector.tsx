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
import { map } from 'lodash';
import { Select } from 'antd';
import gatewayIngressCommonStore from 'app/modules/msp/monitor/gateway-ingress/stores/common';
import i18n from 'i18n';

import './domain-selector.scss';

const { Option } = Select;

const DomainSelector = () => {
  const [domainList, chosenDomain] = gatewayIngressCommonStore.useStore((s) => [s.domainList, s.chosenDomain]);
  const { getDomainList } = gatewayIngressCommonStore.effects;
  const { changeChosenDomain } = gatewayIngressCommonStore.reducers;
  React.useEffect(() => {
    getDomainList();
  }, [getDomainList]);
  const onChange = (val: string) => {
    changeChosenDomain({ chosenDomain: val });
  };
  return (
    <Select
      allowClear
      showSearch
      className="gi-domain-selector"
      value={chosenDomain}
      dropdownMatchSelectWidth={false}
      onChange={onChange}
      placeholder={i18n.t('msp:please select a domain')}
    >
      {map(domainList, (domain) => (
        <Option value={domain} key={domain}>
          {domain}
        </Option>
      ))}
    </Select>
  );
};

export default DomainSelector;
