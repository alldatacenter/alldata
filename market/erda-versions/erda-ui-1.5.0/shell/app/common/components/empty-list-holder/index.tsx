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

// TODO: merge with EmptyHolder
const EmptyListHolder = ({ icon = 'empty-s', tip = i18n.t('common:no data'), style = {}, action = null }) => {
  const cls = classnames({
    'empty-holder': true,
    'multi-line': true,
    'empty-list': true,
  });
  return (
    <div className={cls} style={style}>
      <CustomIcon type={icon} color />
      <span>
        {tip} <span className="action">{action}</span>
      </span>
    </div>
  );
};

export default EmptyListHolder;
