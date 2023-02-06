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
import { Tooltip } from 'antd';
import { ErdaIcon } from 'common';

export interface TitleProps {
  title: string | React.ElementType | JSX.Element;
  tip?: string | React.ElementType | JSX.Element;
  operations?: Array<TitleOperate | React.ReactNode>;
  level?: 1 | 2 | 3;
  mt?: 0 | 8 | 16;
  mb?: 0 | 8 | 16;
  showDivider?: boolean;
  style?: React.CSSProperties;
  tipStyle?: React.CSSProperties;
}

interface TitleOperate {
  title: React.ReactNode;
}

const Title = ({
  title,
  tip,
  tipStyle,
  operations,
  level = 1,
  mt,
  mb,
  showDivider = level === 1,
  ...restProps
}: TitleProps) => {
  const sizeList = ['', 'text-lg', 'text-base', 'text-sm'];
  const _mt = mt ? `mt-${mt / 4}` : '';
  const _mb = mb ? `mb-${mb / 4}` : '';
  const levelToClass = {
    1: `h-12 mb-${_mb || 4} ${_mt}`, // h:48px mb:16px
    2: `h-7 mb-${_mb || 2} ${_mt}`, // h:28px mb:8px
    3: 'h-5', // h:20px
  };
  return (
    <div
      {...restProps}
      className={`ec-title w-full flex items-center justify-between ${levelToClass[level]} ${
        showDivider ? 'border-bottom' : ''
      }`}
    >
      <div className="inline-flex items-center font-medium mr-2">
        <div className={sizeList[level]}>{title}</div>
        {tip ? (
          <Tooltip title={tip} overlayInnerStyle={tipStyle}>
            <ErdaIcon type="help" className="text-base ml-1" />
          </Tooltip>
        ) : null}
      </div>
      <div className="flex-1 inline-flex items-center justify-end">
        {(operations || []).map((item: TitleOperate | React.ReactNode, index: number) => {
          if (React.isValidElement(item)) {
            return (
              <span key={index} className={index > 0 ? 'ml-1' : ''}>
                {item}
              </span>
            );
          } else {
            return (item as TitleOperate)?.title ? (
              <span key={index} className={index > 0 ? 'ml-1' : ''}>
                {(item as TitleOperate).title}
              </span>
            ) : (
              ''
            );
          }
        })}
      </div>
    </div>
  );
};

export default Title;
