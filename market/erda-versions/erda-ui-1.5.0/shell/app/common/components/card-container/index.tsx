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
import { EmptyHolder } from '../empty-holder';
import { ErdaIcon } from 'common';
import './index.scss';

export interface CardContainerProps {
  title: string | React.ElementType | JSX.Element;
  tip?: string | { text: string; style: Obj } | Array<{ text: string; style: Obj } | string>;
  className?: string;
  operation?: React.ReactNode;
  holderWhen?: boolean;
  style?: React.CSSProperties;
  children: React.ReactChild | React.ReactChild[] | null;
}

const CardContainer = ({ title, tip, className, operation, holderWhen, style, children }: CardContainerProps) => {
  const TipComp = Array.isArray(tip) ? (
    <div>
      {tip.map((tipItem, idx) => {
        let text = tipItem;
        let textStyle: Obj = {};
        if (typeof tipItem !== 'string') {
          text = tipItem?.text;
          textStyle = tipItem?.style || {};
        }
        return (
          <div key={idx} className="mb-2" style={textStyle}>
            {text}
          </div>
        );
      })}
    </div>
  ) : (
    tip
  );

  return (
    <div className={`ec-card-container flex flex-col ${className || ''}`} style={style}>
      {title || operation ? (
        <div className="h-8 flex items-center justify-between leading-8">
          {title ? (
            <div className="font-medium inline-flex items-center">
              {title}
              {tip ? (
                <Tooltip title={TipComp} overlayStyle={{ maxWidth: 600 }}>
                  <ErdaIcon type="help" size="16" className="ml-1" />
                </Tooltip>
              ) : null}
            </div>
          ) : null}
          {operation ? <div>{operation}</div> : null}
        </div>
      ) : null}
      {holderWhen ? <EmptyHolder relative /> : children}
    </div>
  );
};

CardContainer.ChartContainer = ({ children, ...rest }: CardContainerProps) => {
  return (
    <CardContainer {...rest}>
      <div className="ec-chart-container">{children}</div>
    </CardContainer>
  );
};

export default CardContainer;
