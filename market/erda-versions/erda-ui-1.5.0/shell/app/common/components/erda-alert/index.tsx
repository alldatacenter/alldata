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
import { Alert, AlertProps } from 'antd';
import './index.scss';

interface IProps {
  message: string | JSX.Element | React.ElementType;
  /** key for remember close state */
  showOnceKey?: string;
  className?: string;
  type?: AlertProps['type'];
}

const ErdaAlert = ({ type = 'info', message, showOnceKey, className }: IProps) => {
  const alertList = JSON.parse(localStorage.getItem('erda-alert-list') || '{}');
  const [isHidden, setIsHidden] = React.useState(showOnceKey ? alertList[showOnceKey] : false);
  const afterClose = () => {
    setIsHidden('true');
    if (showOnceKey) {
      alertList[showOnceKey] = 'true';
      localStorage.setItem('erda-alert-list', JSON.stringify(alertList));
    }
  };

  return !isHidden ? (
    <Alert
      type={type}
      className={`erda-alert py-2 px-4 mb-4 ${className || ''}`}
      message={
        <>
          <ErdaIcon type="message" className="erda-alert-icon mr-2" />
          {message}
        </>
      }
      closeText={<ErdaIcon type="close" className="hover-active" />}
      afterClose={afterClose}
    />
  ) : null;
};

export default ErdaAlert;
