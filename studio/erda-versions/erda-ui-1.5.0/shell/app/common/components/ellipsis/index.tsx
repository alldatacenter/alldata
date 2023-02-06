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

import React, { useState, useRef, useCallback } from 'react';
import { Tooltip } from 'antd';
import { AbstractTooltipProps } from 'core/common/interface';

const TOOLTIP_MOUSE_ENTER_DELAY = 100;

export interface EllipsisProps extends AbstractTooltipProps {
  title?: React.ReactNode | RenderFunction;
  overlay?: React.ReactNode | RenderFunction;
  className?: string;
}

declare type RenderFunction = () => React.ReactNode;

const Ellipsis = (props: EllipsisProps) => {
  const { title, placement = 'top', className = '', style, ...restProps } = props;
  const itemRef = useRef<HTMLDivElement>(null);
  const [enableToolTip, setEnableTooltip] = useState(false);
  const enterDelayTimerRef = useRef<number>();
  const [width, setWidth] = React.useState(0);

  const handleMouseEnter = useCallback(() => {
    if (enterDelayTimerRef.current) {
      window.clearTimeout(enterDelayTimerRef.current);
      enterDelayTimerRef.current = undefined;
    }
    enterDelayTimerRef.current = window.setTimeout(() => {
      const { clientWidth = 0, scrollWidth = 0, offsetWidth = 0 } = itemRef.current ?? {};
      if (offsetWidth !== width) {
        setWidth(offsetWidth);
        // 某些场景 ... 的宽度会被折叠，+1 来判断
        if (scrollWidth > clientWidth + 1) {
          setEnableTooltip(true);
        } else {
          setEnableTooltip(false);
        }
      }
    }, TOOLTIP_MOUSE_ENTER_DELAY);
  }, [width]);

  const EllipsisInner = (
    <div className={`truncate ${className}`} style={style} ref={itemRef} onMouseEnter={handleMouseEnter}>
      {title}
    </div>
  );

  return (
    <Tooltip
      destroyTooltipOnHide
      title={enableToolTip ? title : ''}
      placement={placement}
      {...restProps}
      mouseEnterDelay={0.2}
    >
      {EllipsisInner}
    </Tooltip>
  );
};

export default Ellipsis;
