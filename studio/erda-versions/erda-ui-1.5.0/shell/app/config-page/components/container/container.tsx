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
import './container.scss';

export const Container = (props: CP_CONTAINER.Props) => {
  const { children, props: configProps } = props || {};
  const {
    direction = 'column',
    spaceSize = 'small',
    contentSetting,
    className,
    whiteBg = false,
    isTopHead = false,
    fullHeight = false,
    flexHeight = false,
    visible = true,
    border = false,
    startAlign = false,
    scrollAuto = false,
    overflowHidden = false,
    onClick,
  } = configProps || {};

  if (!visible) return null;
  const tempClass = {
    'dice-cp': true,
    container: true,
    ...(direction ? { [direction]: true } : {}),

    ...(contentSetting ? { [contentSetting]: true } : {}),
    'top-button-group': isTopHead,
    'mt-0': isTopHead,
    'bg-white': whiteBg,
    'start-align': startAlign,
    'h-full': fullHeight,
    'with-border': border,
    'flex-height': flexHeight,
    'scroll-auto': scrollAuto,
    'overflow-hidden': overflowHidden,
    [`space-${spaceSize}`]: true,
  };

  if (className) tempClass[className] = true;
  const containerClass = classnames(tempClass);

  return (
    <div className={containerClass} {...(onClick ? { onClick } : {})}>
      {children}
    </div>
  );
};

export const RowContainer = (props: CP_CONTAINER.Props) => {
  return <Container {...props} props={{ ...(props.props || {}), direction: 'row' }} />;
};

export const LRContainer = (props: CP_CONTAINER.Props) => {
  const { left, right, props: configProps, ...rest } = props;
  const { contentSetting = '', leftProportion = 1, rightProportion } = configProps || {};
  const leftCls = classnames({
    left: true,
    [`flex-${leftProportion}`]: contentSetting !== 'start',
  });
  return (
    <Container {...rest} props={{ ...(props.props || {}), direction: 'row' }}>
      <div className={leftCls}>{left}</div>
      <div className={`right ${rightProportion ? `flex-${rightProportion}` : ''}`}>{right}</div>
    </Container>
  );
};
