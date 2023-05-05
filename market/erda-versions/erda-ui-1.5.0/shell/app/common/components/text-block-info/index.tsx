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
import { Tooltip } from 'antd';
import classnames from 'classnames';
import './index.scss';

export interface TextBlockInfoProps {
  size?: 'small' | 'normal';
  main: string;
  sub?: string;
  desc?: string;
  tip?: string;
  style?: Obj;
  className?: string;
  extra?: string | React.ElementType | JSX.Element;
  align?: 'center' | 'left' | 'right';
}

const TextBlockInfo = (props: TextBlockInfoProps) => {
  const { align = 'left', className = '', main, size = 'normal', sub, desc, tip, extra, style } = props;

  const alignClsMap = {
    container: {
      left: 'text-left',
      right: 'text-right',
      center: 'text-center',
    },
    desc: {
      left: 'justify-start',
      right: 'justify-end',
      center: 'justify-center',
    },
  };

  return (
    <div
      className={`erda-text-block-info ${size} flex flex-col ${className} ${alignClsMap.container[align]}`}
      style={style}
    >
      <div className={'main-text truncate'}>{main}</div>
      {sub ? <div className={'sub-text'}>{sub}</div> : null}
      {desc ? (
        <div className={`desc-text flex items-center w-full ${alignClsMap.desc[align]}`}>
          <span className="truncate">{desc}</span>
          {tip ? (
            <Tooltip title={tip}>
              <ErdaIcon type="help" className="ml-1" />
            </Tooltip>
          ) : null}
        </div>
      ) : null}
      {extra ? <div>{extra}</div> : null}
    </div>
  );
};

export default TextBlockInfo;
