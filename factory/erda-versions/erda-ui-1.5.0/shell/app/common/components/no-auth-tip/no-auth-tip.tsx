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
import classnames from 'classnames';
import { Tooltip } from 'antd';
import i18n from 'i18n';

const NoAuthTip = ({ children, tip = i18n.t('common:no permission') }: any): any => {
  if (!children) return null;
  const childrenWithProps = React.Children.map(children, (child) => {
    const ele = React.cloneElement(child, {
      className: classnames(child.props.className, 'not-allowed'),
      onClick: undefined,
      disabled: true,
    });
    return <Tooltip title={tip}>{ele}</Tooltip>;
  });
  return childrenWithProps;
};

export default NoAuthTip;
