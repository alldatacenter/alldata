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
import { Select } from 'antd';
import { map } from 'lodash';
import i18n from 'i18n';

const { Option } = Select;

interface IProps {
  clusterList: ORG_CLUSTER.ICluster[];
  onChange: (args: string) => void;
}
export const ClusterSelector = ({ clusterList, onChange }: IProps) => {
  const [selected, setSelected] = React.useState('');
  const list = [{ name: '', displayName: i18n.t('all clusters') }, ...clusterList];
  const changeCluster = (val: string) => {
    setSelected(val);
    onChange(val);
  };
  return (
    <Select onChange={changeCluster} value={selected} style={{ width: 200 }}>
      {map(list, ({ name, displayName }) => (
        <Option key={name} value={name}>
          {displayName || name}
        </Option>
      ))}
    </Select>
  );
};
