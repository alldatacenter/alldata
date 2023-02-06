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
import { Tooltip } from 'antd';
import { map, get, groupBy, sortBy } from 'lodash';
import clusterDashboardStore from 'dcos/stores/dashboard';
import './label-selector.scss';

interface IProps {
  labelOptions?: Array<{
    [p: string]: any;
    name: string;
    value: string;
    desc?: string;
    group: string;
    groupName: string;
    groupLevel: number;
  }>;
  value?: string[];
  onChange?: (data: string[]) => void;
}

const LabelSelector = React.forwardRef(({ labelOptions, value = [], onChange }: IProps, ref) => {
  const nodeLabels = clusterDashboardStore.useStore((s) => s.nodeLabels);
  const { getNodeLabels } = clusterDashboardStore.effects;
  const [labelList, setLabelList] = React.useState([] as any[]);

  React.useEffect(() => {
    labelOptions === undefined && getNodeLabels(); // 无options，则请求
  }, [getNodeLabels, labelOptions]);

  React.useEffect(() => {
    if (labelOptions === undefined) {
      setLabelList(nodeLabels.filter((l) => !l.isPrefix).map((l) => ({ ...l, value: l.label, name: l.label })));
    } else {
      setLabelList(labelOptions);
    }
  }, [labelOptions, nodeLabels]);

  // label 以group分组显示
  const gorupLabelObj = groupBy(labelList, 'group');
  let groupList = [] as any[];
  map(gorupLabelObj, (item, key) => {
    groupList.push({
      name: get(item, '[0].groupName', key),
      groupLevel: get(item, '[0].groupLevel', 100),
      children: [...item],
    });
  });
  groupList = sortBy(groupList, 'groupLevel');

  const handleChange = (item: string, isActive: boolean) => {
    const newList = isActive ? value.filter((label) => label !== item) : value.concat(item);
    onChange && onChange(newList);
  };

  return (
    <div ref={ref} className="label-selector-container">
      {map(groupList, (item: any) => {
        const { name, children } = item;
        return (
          <div className="label-group" key={name}>
            <span className="label-group-name">{name}: </span>
            {map(children, (cItem: any) => {
              const isActive = value.includes(cItem.value);
              return (
                <span
                  key={cItem.value}
                  className={`cursor-pointer ${isActive ? 'tag-primary' : 'tag-default'}`}
                  onClick={() => handleChange(cItem.value, isActive)}
                >
                  {cItem.desc ? <Tooltip title={cItem.desc}>{cItem.name}</Tooltip> : cItem.name}
                </span>
              );
            })}
          </div>
        );
      })}
    </div>
  );
});

export default LabelSelector;
