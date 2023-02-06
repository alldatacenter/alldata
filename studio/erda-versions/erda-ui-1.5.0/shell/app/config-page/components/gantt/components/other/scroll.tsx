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

import React, { SyntheticEvent, useRef, useEffect } from 'react';
import './scroll.scss';

export const VerticalScroll: React.FC<{
  scroll: number;
  height: number;
  scrollHeight: number;
  topOffset: number;
  rtl: boolean;
  onScroll: (event: SyntheticEvent<HTMLDivElement>) => void;
}> = ({ scroll, height, scrollHeight, topOffset, rtl, onScroll }) => {
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scroll;
    }
  }, [scroll]);

  return (
    <div
      style={{
        height,
        marginTop: topOffset,
        marginLeft: rtl ? '' : '-8px',
      }}
      className={'erda-gantt-vertical-scroll'}
      onScroll={onScroll}
      ref={scrollRef}
    >
      <div style={{ height: scrollHeight, width: 1 }} />
    </div>
  );
};

interface IProps {
  scroll: number;
  width: number;
  offset: number;
  rtl: boolean;
  onScroll: (event: SyntheticEvent<HTMLDivElement>) => void;
}
export const HorizontalScroll = React.forwardRef(({ scroll, width, offset, rtl, onScroll }: IProps, scrollRef: any) => {
  useEffect(() => {
    if (scrollRef.current) {
      // set scrollLeft to show the scroll bar
      // eslint-disable-next-line no-param-reassign
      scrollRef.current.scrollLeft = scroll;
    }
  }, [scroll, scrollRef]);

  return (
    <div
      dir="ltr"
      style={{
        width: `calc(100% - ${offset}px)`,
        margin: rtl ? `0px ${offset}px 0px 0px` : `0px 0px 0px ${offset}px`,
      }}
      className={'erda-gantt-horizontal-scroll'}
      onScroll={onScroll}
      ref={scrollRef}
    >
      <div style={{ width, height: 8 }} />
    </div>
  );
});
