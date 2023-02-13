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
import { goTo } from 'common/utils';
import { Icon as CustomIcon } from 'common';
import { Button } from 'antd';
import i18n from 'i18n';

import './index.scss';

interface IProps {
  name?: string;
  children: any;
}
interface IState {
  hasError: boolean;
  errorTime: number;
}
class ErrorBoundary extends React.Component<IProps, IState> {
  state = {
    hasError: false,
    errorTime: Date.now(),
  };

  static getDerivedStateFromProps(_nextProps: IProps, preState: IState): Partial<IState> | null {
    // 渲染children报错时如果重置，可能下一次渲染children又会报错，导致死循环
    // 如果距离上次报错不足100ms，认为是同一个报错，但是如果children渲染时长超过100ms，可能还是有问题
    const now = Date.now();
    if (now - preState.errorTime > 100) {
      return {
        hasError: false,
      };
    }
    return null;
  }

  componentDidCatch(error: Error) {
    this.setState({
      hasError: true,
      errorTime: Date.now(),
    });
    // eslint-disable-next-line no-console
    console.error('error:', error);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="error-boundary">
          <CustomIcon type="web-error" />
          <div className="error-desc">
            <span>
              {i18n.t('common:sorry')}，{this.props.name || i18n.t('common:error occurred')}
            </span>
            <Button size="large" type="primary" onClick={() => goTo('/')}>
              {i18n.t('back to home')}
            </Button>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}

export default ErrorBoundary;
