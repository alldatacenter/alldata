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
import { Radio, RadioChangeEvent } from 'antd';
import Topology from 'msp/env-overview/topology/pages/topology';
import ServiceList from 'msp/env-overview/service-list/pages';
import ErdaIcon from 'common/components/erda-icon';
import i18n from 'i18n';
const { Button: RadioButton, Group: RadioGroup } = Radio;

type ITab = 'topology' | 'serviceList';

const viewMap: { name: string; key: ITab; icon: string }[] = [
  {
    name: i18n.t('msp:topology'),
    key: 'topology',
    icon: 'topology',
  },
  {
    name: i18n.t('msp:service list'),
    key: 'serviceList',
    icon: 'list',
  },
];

const Comp: { [key in ITab]: () => JSX.Element } = {
  topology: Topology,
  serviceList: ServiceList,
};

const MicroServiceOverview = () => {
  const [view, setView] = React.useState(viewMap[0].key);
  const handleChangeView = (e: RadioChangeEvent) => {
    setView(e.target.value);
  };

  const Comps = Comp[view];
  return (
    <div className="h-full relative">
      <div className="flex justify-end absolute right-0">
        <RadioGroup defaultValue={viewMap[0].key} value={view} onChange={handleChangeView}>
          {viewMap.map((item) => {
            return (
              <RadioButton key={item.key} value={item.key}>
                <span className="flex items-center">
                  <ErdaIcon className="mr-1" type={item.icon} />
                  {item.name}
                </span>
              </RadioButton>
            );
          })}
        </RadioGroup>
      </div>
      <Comps />
    </div>
  );
};

export default MicroServiceOverview;
