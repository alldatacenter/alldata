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

import './index.scss';

interface ISection {
  title?: string | React.ReactNode;
  titleExtra?: JSX.Element | null;
  titleOperate?: JSX.Element | null;
  children: JSX.Element;
  desc?: string | React.ReactNode;
  highlight?: string;
  titleProps?: object;
  descProps?: object;
}

const ConfigSection = ({
  title,
  titleOperate,
  titleExtra = null,
  titleProps = {},
  desc,
  descProps = {},
  highlight,
  children,
}: ISection): JSX.Element => {
  const highlightColor = highlight ? `highlight-${highlight}` : '';
  return (
    <React.Fragment>
      <div className="mb-3">
        <div className="config-section-title flex justify-between items-center" {...titleProps}>
          <span className={`name font-medium ${highlightColor}`}>{title}</span>
          {titleExtra}
        </div>
        <div className={`config-section-desc ${highlightColor}`} {...descProps}>
          {desc}
        </div>
      </div>
      <div className="config-section-content">
        {titleOperate ? <div className="operate">{titleOperate}</div> : null}
        {children}
      </div>
    </React.Fragment>
  );
};

interface IProps {
  sectionList: ISection[];
  className?: string;
}
const ConfigLayout = ({ sectionList, className = '' }: IProps): JSX.Element => {
  return (
    <div className={`config-body ${className}`}>
      {sectionList.map(({ children, ...rest }: ISection, i) => (
        <ConfigSection key={i} {...rest}>
          {children}
        </ConfigSection>
      ))}
    </div>
  );
};

export default ConfigLayout;
