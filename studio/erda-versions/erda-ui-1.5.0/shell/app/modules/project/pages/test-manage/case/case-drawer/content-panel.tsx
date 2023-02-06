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
import './content-panel.scss';
import { ErdaIcon } from 'common';

interface IProps {
  title: React.ReactNode;
  children: React.ReactNode;
  mode?: 'common' | 'add' | 'edit' | 'upload';
  className?: string;
  append?: React.ReactNode;
  loading?: boolean;

  onClick?: () => void;
}

const noop = () => {};

const ContentPanel = ({
  title,
  children,
  className = '',
  loading = false,
  append,
  onClick = noop,
  mode = 'common',
}: IProps) => {
  const delimiter = <span className="text-holder mx-2">|</span>;
  const typeIcon = {
    edit: (
      <span onClick={onClick} onMouseDown={(e) => e.preventDefault()} className="flex text-desc hover-active">
        <ErdaIcon type="edit1" className="mr-1" />
        {i18n.t('edit')}
      </span>
    ),
    add: (
      <span onClick={onClick} onMouseDown={(e) => e.preventDefault()} className="flex text-desc hover-active">
        <ErdaIcon type="plus" className="mr-1" />
        {i18n.t('common:add')}
      </span>
    ),
    upload: (
      <span onClick={onClick} onMouseDown={(e) => e.preventDefault()} className="flex text-desc hover-active">
        <ErdaIcon type="upload" className="mr-1" />
        {i18n.t('upload')}
      </span>
    ),
  };
  return (
    <div className={`content-panel ${className}`}>
      <Spin spinning={loading}>
        <div className="flex items-center title justify-start mb-2">
          <span>{title}</span>
          {mode !== 'common' ? (
            <>
              {delimiter}
              {typeIcon[mode]}
            </>
          ) : null}
          {append ? (
            <>
              {delimiter}
              {append}
            </>
          ) : null}
        </div>
        <div className="content">{children}</div>
      </Spin>
    </div>
  );
};

export default ContentPanel;
