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
import { Anchor } from 'antd';
import { OperationProps, PanelProps, IAnchorContainer } from 'core/common/interface';
import { IF, Title, TitleProps, Panel } from 'common';
import { map, isEmpty } from 'lodash';
import classnames from 'classnames';
import './index.scss';

interface ITitleProps {
  title?: string | React.ReactNode;
  icon?: React.ReactNode;
  operations?: OperationProps[];
  level?: number;
  tips?: string;
  prefixCls?: string;
  style?: React.CSSProperties;
  className?: string;
  showDivider?: boolean;
}

interface IContentProps {
  crossLine?: boolean;
  titleProps?: ITitleProps;
  showTitle?: boolean;
  panelProps?: PanelProps;
  getComp?: () => React.ReactNode | React.ReactNodeArray;
}

export interface IContentPanelProps extends IContentProps {
  title?: string | React.ReactNode;
}

export interface ILink extends IContentProps {
  linkProps: {
    title?: string | React.ReactNode;
    icon?: React.ReactNode;
  };
  key: string;
}

interface IProps {
  baseInfoConf?: IContentPanelProps;
  linkList?: ILink[];
  anchorContainer?: IAnchorContainer;
  children?: React.ReactNode;
}

const { Link } = Anchor;

const Content = (props: IContentProps) => {
  const { crossLine, titleProps, showTitle = true, panelProps, getComp } = props;
  const contentClass = classnames({
    'content-wrapper': true,
    'mt-2 px-3 pb-3': showTitle,
    'p-3': !showTitle,
    'border-top mt-2': crossLine,
  });
  return (
    <div className="title-box border-all bg-white">
      <IF check={showTitle}>
        <div className="title-wrapper px-3 pt-3">
          <Title {...(titleProps as TitleProps)} />
        </div>
      </IF>
      <div className={contentClass}>
        <IF check={!getComp}>
          <Panel {...panelProps} />
          <IF.ELSE />
          {getComp ? getComp() : <></>}
        </IF>
      </div>
    </div>
  );
};

const DetailsPanel = (props: IProps) => {
  const { baseInfoConf, linkList, anchorContainer, children } = props;

  const _baseInfoConf = {
    ...baseInfoConf,
    titleProps: {
      title: baseInfoConf?.title,
      level: 2,
      ...baseInfoConf?.titleProps,
    },
  };

  const container = React.useRef(anchorContainer || (document.getElementById('main') as IAnchorContainer));

  return (
    <div className="details-panel-template">
      <IF check={!isEmpty(baseInfoConf)}>
        <div className="base-info mb-3">
          <Content {...(_baseInfoConf as IContentProps)} />
        </div>
      </IF>
      {children}
      <IF check={!isEmpty(linkList)}>
        <Anchor getContainer={() => container?.current || document.body}>
          {map(linkList, (item) => {
            const { linkProps, key } = item;
            const { icon, title } = linkProps;
            const href = `#${key}`;
            return (
              <Link
                href={href}
                title={
                  <div className="anchor-link-title-icon">
                    {icon}
                    <span className="flex items-center">{title}</span>
                  </div>
                }
                key={href}
              />
            );
          })}
        </Anchor>

        {map(linkList, (item) => {
          const { linkProps, crossLine, titleProps, showTitle, panelProps, getComp, key } = item;
          const { icon, title } = linkProps;
          const _titleProps = {
            title,
            level: 2,
            icon: <div className="title-icon">{icon}</div>,
            ...titleProps,
          };
          return (
            <div id={key} className="pt-3">
              <Content
                crossLine={crossLine}
                titleProps={_titleProps}
                panelProps={panelProps}
                getComp={getComp}
                showTitle={showTitle}
              />
            </div>
          );
        })}
      </IF>
    </div>
  );
};

export default DetailsPanel;
