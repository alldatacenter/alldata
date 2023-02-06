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
import { Input } from 'antd';

const CP_INPUT = (props: CP_INPUT.Props) => {
  const { props: configProps, state: propsState, operations } = props;
  const [value, setValue] = React.useState(propsState?.value as string | undefined);
  const onChange = (e: any) => {
    setValue(e.target.value);
  };
  return <Input value={value} onChange={onChange} {...configProps} />;
};

export default CP_INPUT;
