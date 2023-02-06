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
import { MarkdownEditor } from 'common';
import i18n from 'i18n';

const CP_MARKDOWN_EDITOR = (props: CP_MARKDOWN_EDITOR.Props) => {
  const { props: configProps, state, operations, execOperation } = props || {};
  const [value, setValue] = React.useState(state.value);

  const { visible, ...rest } = configProps || {};

  React.useEffect(() => {
    setValue(state.value);
  }, [state.value]);

  const onChange = (val: string) => {
    setValue(val);
  };

  const onSubmit = operations?.submit
    ? () => {
        execOperation(operations.submit, { value });
      }
    : undefined;

  if (visible === false) return null;

  return <MarkdownEditor {...rest} value={value} onChange={onChange} operationBtns={[
    {
      text: i18n.t('dop:post comment'),
      type: 'primary',
      onClick: () => onSubmit?.()
    },
  ]} />;
};

export default CP_MARKDOWN_EDITOR;
