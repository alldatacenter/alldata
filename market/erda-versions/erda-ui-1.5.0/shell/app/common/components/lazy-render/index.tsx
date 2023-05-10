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
import 'intersection-observer';

interface IProps {
  minHeight?: string;
  children: any;
}

interface IState {
  render: boolean;
}
class LazyRender extends React.PureComponent<IProps, IState> {
  state = {
    render: false,
  };

  private intersectionObserver: IntersectionObserver;

  private wrapDom: HTMLDivElement | null;

  componentDidMount() {
    if (this.wrapDom) {
      this.intersectionObserver = new IntersectionObserver((entries) => {
        // 如果不可见，就返回
        if (!entries[0].isIntersecting) return;
        this.setState({ render: true });
        // 已渲染就停止监听
        this.intersectionObserver.disconnect();
      });

      // 开始观察
      this.intersectionObserver.observe(this.wrapDom);
    }
  }

  componentWillUnmount() {
    this.intersectionObserver.disconnect();
  }

  render() {
    // 需要给一个最小高度，避免内容无高度时所有wrap都可见时，惰性加载无意义
    const { children, minHeight = '60px' } = this.props;
    const { render } = this.state;

    return (
      <div
        style={{ minHeight }}
        ref={(ref) => {
          this.wrapDom = ref;
        }}
      >
        {render ? children : null}
      </div>
    );
  }
}

export default LazyRender;
