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
import { useUpdateEffect } from 'react-use';
import { InputSelect } from 'common';
import { useUpdate } from 'common/use-hooks';

const CP_INPUT_SELECT = (props: CP_INPUT_SELECT.Props) => {
  const { props: configProps, state: propsState, operations, execOperation } = props;
  const { options, visible = true, ...rest } = configProps || {};

  const [state, updater, update] = useUpdate({
    value: propsState.value || (undefined as string | undefined),
  });

  useUpdateEffect(() => {
    update({
      value: propsState.value || undefined,
    });
  }, [propsState]);

  useUpdateEffect(() => {
    operations?.onChange && execOperation(operations.onChange, { value: state.value });
  }, [state.value]);

  const loadData = (selectedOptions: any) => {
    operations?.onSelectOption && execOperation(operations?.onSelectOption, selectedOptions);
  };

  const onChange = (v: string) => {
    updater.value(v);
  };

  if (!visible) return null;
  return <InputSelect {...rest} options={options} onChange={onChange} value={state.value} onLoadData={loadData} />;
};

export default CP_INPUT_SELECT;
