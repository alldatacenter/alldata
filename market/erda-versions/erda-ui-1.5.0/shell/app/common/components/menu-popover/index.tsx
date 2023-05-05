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
import { Popover } from 'antd';
import { ErdaIcon } from 'common';

import './index.scss';

interface IProps {
  styleName?: string;
  trigger?: 'hover' | 'click';
  iconStyle?: 'big-more-icon' | 'sm-more-icon';
  placement?:
    | 'top'
    | 'left'
    | 'right'
    | 'bottom'
    | 'topLeft'
    | 'topRight'
    | 'bottomLeft'
    | 'bottomRight'
    | 'leftTop'
    | 'leftBottom'
    | 'rightTop'
    | 'rightBottom';
  content: (setVisible: Function) => React.ReactElement;
}

const MenuPopover = ({ content, styleName, placement, iconStyle = 'sm-more-icon', trigger }: IProps) => {
  const [visible, setVisible] = React.useState(false);

  return (
    <Popover
      placement={placement || 'bottom'}
      visible={visible}
      overlayClassName={`menu-popover ${styleName || ''}`}
      content={content(setVisible)}
      trigger={trigger || 'click'}
      onVisibleChange={setVisible}
    >
      <ErdaIcon type="more" className={`${iconStyle} hover-active`} onClick={(e) => e.stopPropagation()} />
    </Popover>
  );
};

export default MenuPopover;
