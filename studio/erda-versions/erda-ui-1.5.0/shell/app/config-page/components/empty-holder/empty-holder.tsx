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
import { Icon as CustomIcon } from 'common';
import classnames from 'classnames';
import i18n from 'i18n';
import './empty-holder.scss';

export const EmptyHolder = (props: CP_EMPTY_HOLDER.Props) => {
  const { props: configProps } = props;
  const {
    visible = true,
    icon = 'empty',
    tip = i18n.t('common:no data'),
    relative = false,
    style = {},
    action = null,
    className = '',
    whiteBg = false,
    paddingY = false,
  } = configProps || {};

  const cls = classnames({
    'empty-holder': true,
    'multi-line': true,
    relative,
    'bg-white': whiteBg,
    'padding-vertical': paddingY,
  });

  if (!visible) {
    return null;
  }

  return (
    <div className={`${cls} ${className}`} style={style}>
      <CustomIcon type={icon} color />
      <span>
        {tip} <span className="action">{action}</span>
      </span>
    </div>
  );
};

export default EmptyHolder;
