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
import { Dropdown, Menu } from 'antd';
import { Icon as CustomIcon } from 'common';
import classNames from 'classnames';
import './index.scss';

interface IProps {
  children: React.ReactNode;
  limit?: number;
  ellipses?: React.ReactNode;
  className?: string;
}

const btnClassName = 'table-operations-btn';

const clone = (child: any) => {
  if (!React.isValidElement(child)) {
    return child;
  }
  let { className } = child.props;
  if (className) {
    className = className.replace(btnClassName, '');
  }
  return React.cloneElement(child, {
    className: classNames(btnClassName, className),
  });
};

/**
 * @param limit 展示个数
 * @param ellipses 自定义省略符号
 * @param children
 * @param className
 */
const OperateBtn = ({ children, limit = 3, ellipses, className }: IProps) => {
  const renderChild = () => {
    const childCount = React.Children.count(children);
    if (childCount <= limit) {
      return React.Children.map(children, clone);
    } else {
      const commonChildren: React.ReactElement[] = [];
      const dropChildren: React.ReactElement[] = [];
      React.Children.map(children, (child, index) => {
        if (index < limit - 1) {
          commonChildren.push(clone(child));
        } else {
          dropChildren.push(<Menu.Item key={index}>{clone(child)}</Menu.Item>);
        }
      });

      const menu = <Menu>{dropChildren}</Menu>;

      return (
        <>
          {commonChildren}
          <Dropdown overlayClassName="table-operate-dropdown" overlay={menu} placement="bottomRight">
            {ellipses || (
              <CustomIcon
                className="hover-active"
                type="more"
                onClick={(e) => {
                  e.stopPropagation();
                }}
              />
            )}
          </Dropdown>
        </>
      );
    }
  };
  const wrapClassName = classNames('table-operations operator-dropdown-wrap', className);
  return (
    <div className={wrapClassName} onClick={(e) => e.stopPropagation()}>
      {renderChild()}
    </div>
  );
};

export default OperateBtn;
