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
import { isNil } from 'lodash';

export interface IProps {
  triggerBy: any;
  toPageTop?: boolean;
}

/**
 * This is a component which should be placed at a scrollable component
 * once prop triggerBy changes, it will automatically scroll to top(when toPageTop is true)
 * or the position it placed
 * @export
 * @param {IProps} props
 */
function SwitchAutoScroll(props: IProps) {
  const positionRef = React.useRef(null);
  React.useEffect(() => {
    if (!isNil(props.triggerBy)) {
      if (props.toPageTop) {
        const mainDom = document.querySelector('#main');
        mainDom && (mainDom.scrollTop = 0);
      } else if (positionRef.current) {
        (positionRef.current as any).scrollIntoView();
      }
    }
  }, [props.toPageTop, props.triggerBy]);

  return props.toPageTop ? null : <div ref={positionRef} />;
}

export default SwitchAutoScroll;
