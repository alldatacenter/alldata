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
import { ErdaIcon } from 'common';
import { OperationAction } from 'config-page/utils';

const Icon = (props: CP_ICON.Props) => {
  const { props: configProps, operations, execOperation, customOp } = props;
  const { iconType, visible, hoverActive, ...extraProps } = configProps || {};

  if (visible === false) return null;

  const onClick = () => {
    const curOp = operations?.click;
    if (curOp) {
      execOperation(curOp);
      if (customOp && customOp[curOp.key]) {
        customOp[curOp.key](curOp);
      }
    }
  };

  const cls = `${operations?.click || hoverActive ? 'hover-active' : ''}`;

  return (
    <OperationAction operation={operations?.click} onClick={onClick}>
      <ErdaIcon isConfigPageIcon type={iconType} {...extraProps} className={`${cls} ${extraProps?.className}`} />
    </OperationAction>
  );
};

export default Icon;
