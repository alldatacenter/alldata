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
import { getClass } from 'config-page/utils';
import { TextBlockInfo } from 'common';

const TextBlock = (props: CP_TEXT_BLOCK.Props) => {
  const { cId, props: configProps, extra, operations, execOperation, data } = props;
  const { size = 'normal', visible = true, ...rest } = configProps || {};

  if (!visible) return null;

  return (
    <div className={`cp-text-block p-3 ${getClass(configProps)}`}>
      <TextBlockInfo {...rest} {...data?.data} size={size} extra={extra} />
    </div>
  );
};

export default TextBlock;
