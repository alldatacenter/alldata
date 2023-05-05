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

import classnames from 'classnames';
import React from 'react';
import { Icon as CustomIcon } from 'common';
import i18n from 'i18n';
import './index.scss';

interface IEmptyProps {
  tip?: string;
  icon?: string | JSX.Element;
  relative?: boolean;
  style?: object;
  action?: JSX.Element | null;
  className?: string;
}
export const EmptyHolder = ({
  icon = 'empty',
  tip = i18n.t('common:no data'),
  relative = false,
  style = {},
  action = null,
  className = '',
}: IEmptyProps) => {
  const cls = classnames({
    'empty-holder': true,
    'multi-line': true,
    relative,
  });
  return (
    <div className={`${cls} ${className}`} style={style}>
      {typeof icon === 'string' ? <CustomIcon className="w-full" type={icon} color /> : <div>{icon}</div>}
      <span>
        {tip} <span className="action">{action}</span>
      </span>
    </div>
  );
};

export default EmptyHolder;
