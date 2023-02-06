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
import './split-page.scss';

interface IProps {
  className?: string;
  children: JSX.Element | JSX.Element[];
}

export const SplitPage = ({ className, children }: IProps) => {
  return <div className={classnames('split-page', className)}>{children}</div>;
};

interface ILeftProps extends IProps {
  width?: number;
  fixedSplit?: boolean;
}
const SplitPageLeft = ({ className, width = 270, fixedSplit = false, children }: ILeftProps) => {
  return (
    <div
      className={classnames('split-page-left', fixedSplit && 'fixed-split', className)}
      style={{ width: `${width - 1}px` }}
    >
      {children}
    </div>
  );
};

interface IRightProps extends IProps {
  pl32?: boolean; // 为true时左右两侧padding一致
}
const SplitPageRight = ({ className, pl32 = false, children }: IRightProps) => {
  return <div className={classnames('split-page-right', className, pl32 && 'pl-cls')}>{children}</div>;
};

SplitPage.Left = SplitPageLeft;
SplitPage.Right = SplitPageRight;
