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
import { useEffectOnce } from 'react-use';
import classnames from 'classnames';
import { ErdaIcon } from 'common';

interface IDiceYamlEditorDrawerProps {
  title: string | null;
  visible: boolean;
  onClose: () => void;
  width?: number;
  readonly: boolean;
  className?: string | null;
  content?: React.ReactNode;
}
const DRAWER_TOP = 276;
const APP_HEADER = 94;
const PADDING_BOTTOM = 16;
const READONLY_PADDING_BOTTOM = 16;

const DiceYamlEditorDrawer = (props: IDiceYamlEditorDrawerProps) => {
  const [position, setPosition] = React.useState('absolute');
  const [scrollTop, setScrollTop] = React.useState(0);
  const el: any = React.useRef(null as any);
  const { title, visible, width = 360, onClose, content, readonly } = props;
  let drawerContent: any;

  const keyup = (e: any) => {
    if (e.keyCode === 27) {
      onClose();
    }
  };

  const onScroll = () => {
    if (!el.current) return;
    if (el.current.scrollTop >= DRAWER_TOP - APP_HEADER) {
      let newPosition = 'fixed';
      if (el.current.scrollTop + el.current.offsetHeight >= el.current.scrollHeight) {
        newPosition = 'fixed-bottom';
      }
      setPosition(newPosition);
      setScrollTop(el.current.scrollTop);
    } else {
      setPosition('absolute');
      setScrollTop(el.current.scrollTop);
    }
  };

  useEffectOnce(() => {
    el.current = document.getElementById('main');
    document.body.addEventListener('keyup', keyup);
    if (el.current) {
      el.current.addEventListener('scroll', onScroll);
    }
    return () => {
      document.body.removeEventListener('keyup', keyup);
      if (el.current) {
        el.current.removeEventListener('scroll', onScroll);
      }
    };
  });

  React.useEffect(() => {
    setTimeout(() => {
      if (drawerContent) {
        drawerContent.scrollTop = 0;
      }
    }, 50);
  }, [drawerContent, visible]);

  let style: any = {
    width: `${width}px`,
    height: `calc(100vh - ${DRAWER_TOP}px)`,
    display: 'none',
  };
  if (visible && el.current) {
    style = {
      width: `${width}px`,
      height: `calc(100vh - ${DRAWER_TOP - el.current.scrollTop}px)`,
      display: 'block',
      opacity: 1,
      top: DRAWER_TOP - scrollTop,
    };

    if (position === 'fixed') {
      style.top = APP_HEADER;
      style.height = `calc(100vh - ${APP_HEADER}px)`;
    } else if (position === 'fixed-bottom') {
      style.top = APP_HEADER;
      style.height = `calc(100vh - ${APP_HEADER + (readonly ? READONLY_PADDING_BOTTOM : PADDING_BOTTOM)}px)`;
    }
  }

  const className = classnames(props.className, 'yaml-editor-drawer-content');
  return (
    <div style={style} className="yaml-editor-drawer">
      <div className="yaml-editor-drawer-title">
        {title}
        <ErdaIcon type="close" onClick={onClose} className="yaml-editor-drawer-close" />
      </div>
      <div
        ref={(e: any) => {
          drawerContent = e;
        }}
        className={className}
      >
        {content}
      </div>
    </div>
  );
};

export default DiceYamlEditorDrawer;
