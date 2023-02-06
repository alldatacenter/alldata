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
import { RadioTabs } from 'common';

const CP_RADIO_TABS = (props: CP_RADIO_TABS.Props) => {
  const { customOp = {}, execOperation, operations, state, props: configProps, data } = props;
  const { options: propsOptions, ...rest } = configProps || {};

  React.useEffect(() => {
    customOp.onStateChange?.(state);
  }, [state]);

  const onChange = (val?: string | number) => {
    customOp.onChange?.(val);
    operations?.onChange && execOperation(operations.onChange, { value: val });
  };

  const options = data?.options || propsOptions || [];

  return <RadioTabs {...rest} options={options} value={state?.value} onChange={onChange} />;
};

export default CP_RADIO_TABS;
