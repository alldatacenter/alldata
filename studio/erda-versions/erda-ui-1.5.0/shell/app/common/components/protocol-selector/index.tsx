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

const { Option } = Select;

const HTTP_PREFIX = 'http://';
const HTTPS_PREFIX = 'https://';

interface IProps {
  value: string;
  onChange: (params: string) => void;
}

// TODO: remove this component for only one place used
const ProtocolSelector = ({ value, onChange }: IProps) => {
  return (
    <Select value={value} onChange={onChange} style={{ width: 94 }}>
      <Option value={HTTP_PREFIX}>http://</Option>
      <Option value={HTTPS_PREFIX}>https://</Option>
    </Select>
  );
};

export default ProtocolSelector;
