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
import { throttle } from 'lodash';
import { ErdaIcon } from 'common';
import { useEffectOnce } from 'react-use';
import i18n from 'i18n';

import './index.scss';

const BackToTop = ({ containerId }: { containerId?: string }) => {
  const [visible, setVisible] = React.useState(false);
  const isMoving = React.useRef(false);
  const mainElement = React.useMemo(
    () => document.querySelector(containerId ? `#${containerId}` : '#main'),
    [containerId],
  );

  const handleScroll = React.useCallback(
    throttle(() => {
      const isV = mainElement ? mainElement.scrollTop * 1.25 > mainElement!.clientHeight : false;
      setVisible(isV);
    }, 300),
    [mainElement],
  );

  useEffectOnce(() => {
    mainElement && mainElement.addEventListener('scroll', handleScroll);
    return () => {
      mainElement && mainElement.removeEventListener('scroll', handleScroll);
    };
  });

  const onBackToTop = () => {
    if (isMoving.current) {
      return;
    }
    isMoving.current = true;
    mainElement!.scrollTo({ top: 0, behavior: 'smooth' });
    isMoving.current = false;
  };

  return visible ? (
    <Tooltip title={i18n.t('back to top')}>
      <ErdaIcon size="20" className="scroll-top-btn" type="huidaodingbu" onClick={onBackToTop} />
    </Tooltip>
  ) : null;
};

export default BackToTop;
