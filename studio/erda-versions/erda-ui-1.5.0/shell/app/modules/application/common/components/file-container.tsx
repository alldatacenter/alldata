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
import { Spin } from 'antd';
import i18n from 'i18n';
import './file-container.scss';

interface IProps {
  children: any;
  name?: string | JSX.Element;
  className?: string;
  ops?: any;
  isEditing?: boolean;
}

const FileContainer = ({ children, name, className = '', ops, isEditing }: IProps) => {
  const [isSpinning, setSpin] = React.useState(true);
  React.useEffect(() => {
    let unMount = false;

    setTimeout(() => {
      if (!unMount) {
        setSpin(false);
      }
    }, 500);

    return () => {
      unMount = true;
    };
  }, []);

  const clsName = className.replace('undefined', '');

  return (
    <article className={`file-container ${clsName}`}>
      <div className="file-title font-bold">
        <span className="flex-1 nowrap">
          {isEditing ? i18n.t('edit') : null}
          {name}
        </span>
        <span className="file-ops">{ops}</span>
      </div>
      <div className="file-content">
        <Spin spinning={isSpinning} tip={`${i18n.t('dop:content loading')}...`} wrapperClassName="flex-1">
          {children}
        </Spin>
      </div>
    </article>
  );
};

export default FileContainer;
