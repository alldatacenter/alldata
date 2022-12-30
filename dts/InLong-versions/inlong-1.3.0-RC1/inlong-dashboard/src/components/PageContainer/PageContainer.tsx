/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import React from 'react';
import { Link } from 'react-router-dom';
import { PageContainer as ProPageContainer, FooterToolbar } from '@ant-design/pro-layout';
import { Card } from 'antd';
import { useSelector } from '@/hooks';
import { State } from '@/models';
import Container from './Container';

export { FooterToolbar };

export type BreadcrumbItem = {
  name: string;
  path?: string;
};

export interface PageContainerProps {
  className?: string;
  // style
  style?: React.CSSProperties;
  breadcrumb?: BreadcrumbItem[];
  // Whether to automatically generate breadcrumbs for the current menu
  useDefaultBreadcrumb?: boolean;
  // Whether to automatically use Container to wrap the content area
  useDefaultContainer?: boolean;
  children?: React.ReactNode;
  footer?: React.ReactNode[];
}

export interface ContainerProps {
  className?: string;
  style?: object;
}

const PageContainer: React.FC<PageContainerProps> = ({
  className = '',
  style = {},
  breadcrumb = [],
  useDefaultBreadcrumb = true,
  useDefaultContainer = true,
  children,
  footer,
}) => {
  const currentMenu = useSelector<State, State['currentMenu']>(state => state.currentMenu);
  const { name, path } = currentMenu || ({} as any);

  // const defaultBreadcrumb = [{ name: 'Home', path: '/' }] as BreadcrumbItem[];
  const defaultBreadcrumb = [];
  if (name && path) {
    defaultBreadcrumb.push({ name, path });
  }

  const breadcrumbData = useDefaultBreadcrumb ? defaultBreadcrumb.concat(breadcrumb) : breadcrumb;

  return (
    <ProPageContainer
      className={className}
      style={style}
      header={{
        title: '',
        breadcrumb: breadcrumbData?.length && {
          routes: breadcrumbData.map(item => ({
            path: item.path,
            breadcrumbName: item.name,
          })),
          itemRender: (route, params, routes) => {
            const last = routes.indexOf(route) === routes.length - 1;
            return last ? (
              <span>{route.breadcrumbName}</span>
            ) : (
              <Link to={route.path}>{route.breadcrumbName}</Link>
            );
          },
        },
      }}
      footer={footer}
    >
      {useDefaultContainer ? (
        <Container>
          <Card>{children}</Card>
        </Container>
      ) : (
        children
      )}
    </ProPageContainer>
  );
};

export default PageContainer;
