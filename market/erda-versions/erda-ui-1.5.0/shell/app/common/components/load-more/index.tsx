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

import { debounce, throttle } from 'lodash';
import React from 'react';
import { isPromise } from 'common/utils';
import { Spin } from 'antd';
import i18n from 'i18n';

interface IProps {
  getContainer?: () => HTMLElement | undefined;
  hasMore?: boolean;
  isLoading: boolean;
  initialLoad?: boolean;
  triggerBy?: string;
  threshold?: number;
  load: () => any;
}

class LoadMore extends React.Component<IProps> {
  eventType: string;

  target: Element;

  eventTarget: Element;

  targetDom: Element;

  threshold: number;

  lazyCheck = debounce(() => this.checkHeight(), 100);

  onScroll = throttle(() => {
    if (!this.props.isLoading) {
      const { scrollHeight, scrollTop, clientHeight } = this.targetDom;
      if (scrollHeight - scrollTop - clientHeight < this.threshold && this.props.hasMore) {
        this.load();
      }
    }
  }, 100);

  // 滚动加载时chrome下Content Download时间会有2-3s的延长
  // see https://stackoverflow.com/questions/47524205/random-high-content-download-time-in-chrome
  fkChrome = (e: WheelEvent) => {
    if (e.deltaY === 1) {
      e.preventDefault();
    }
  };

  // if not has scrollBar then load more
  checkHeight = () => {
    const { scrollHeight, clientHeight } = this.targetDom;
    const { isLoading, hasMore } = this.props;
    const hasScrollBar = scrollHeight > clientHeight;
    if (!hasScrollBar && hasMore && !isLoading) {
      this.load();
    }
  };

  componentDidMount() {
    // chrome v56 后会默认给 scroll 监听事件设置 passive: ture 以表明不会 preventDefault
    // 目的是跳过浏览器检测 handler 中是否有 preventDefault 的步骤
    // see https://juejin.im/post/5ad804c1f265da504547fe68
    window.addEventListener('mousewheel', this.fkChrome as EventListenerOrEventListenerObject, { passive: false });
    const { initialLoad = false } = this.props;
    this.init();
    if (initialLoad) {
      return this.load();
    }
    this.attachEvent();
  }

  componentWillUnmount() {
    this.detachEvent();
    window.removeEventListener('mousewheel', this.fkChrome as EventListenerOrEventListenerObject);
  }

  componentDidUpdate(prevProps: IProps) {
    // 第一次：开始监听
    if (this.props.hasMore && !prevProps.hasMore) {
      this.attachEvent();
    }
    // 最后一次：移除监听
    if (!this.props.hasMore && prevProps.hasMore) {
      this.detachEvent();
    }

    // 每次请求完成且渲染后，checkHeight
    if (!this.props.isLoading && prevProps.isLoading && this.props.hasMore) {
      // fix issue when initialLoad is false and screen is too big and hasMore is true, but only one page displayed.
      this.checkHeight();
    }
  }

  init = () => {
    const { triggerBy = 'scroll', threshold = 300, getContainer } = this.props;
    this.eventType = triggerBy;
    this.threshold = threshold;
    this.target = (getContainer && getContainer()) || document.querySelectorAll('#main')[0];
    this.targetDom = this.target || document.documentElement || document.body;
    this.eventTarget = this.target || window;
  };

  // detachEvent before load and reAttach after get data
  load = () => {
    this.props.load();
  };

  attachEvent = () => {
    this.eventTarget.addEventListener(this.eventType, this.onScroll);
    window.addEventListener('resize', this.lazyCheck);
  };

  detachEvent = () => {
    this.eventTarget.removeEventListener(this.eventType, this.onScroll);
    window.removeEventListener('resize', this.lazyCheck);
  };

  render() {
    return this.props.isLoading ? (
      <Spin tip={`${i18n.t('loading')}...`}>
        <div style={{ minHeight: '60px' }} />
      </Spin>
    ) : null;
  }
}

export default LoadMore;
