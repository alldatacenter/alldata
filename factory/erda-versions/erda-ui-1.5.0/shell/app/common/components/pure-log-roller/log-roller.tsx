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

import { throttle } from 'lodash';
import { Button, Tooltip } from 'antd';
import React from 'react';
import LogContent from './log-content';
import i18n from 'i18n';
import { ErdaIcon } from 'common';
import './log-roller.scss';

interface IProps {
  content: string | object[];
  rolling: boolean;
  backwardLoading: boolean;
  hasLogs: boolean;
  searchOnce?: boolean;
  extraButton?: JSX.Element;
  CustomLogContent?: typeof React.Component;
  onStartRolling: () => void;
  onGoToBottom: () => void;
  onCancelRolling: () => void;
  onGoToTop: () => void;
  onShowDownloadModal: () => void;
  transformContent?: () => string;
}

interface IState {
  fullScreen?: boolean;
}
export class LogRoller extends React.Component<IProps, IState> {
  prevDistanceToTop: number;

  direction: 'up' | 'down';

  preElm: any;

  throttleScroll = throttle(() => this.onScroll(), 100);

  constructor(props: IProps) {
    super(props);
    this.state = {
      fullScreen: false,
    };
    this.prevDistanceToTop = 0;
  }

  onScroll() {
    const { rolling, onCancelRolling, onGoToTop } = this.props;
    const distanceToBottom = this.preElm.scrollHeight - this.preElm.scrollTop - this.preElm.clientHeight;
    const distanceToTop = this.preElm.scrollTop;

    this.direction = distanceToTop > this.prevDistanceToTop ? 'down' : 'up';
    if (distanceToBottom > 10 && rolling) {
      onCancelRolling(); // 日志bottom !==0，取消自动rolling
    }
    // 向上移动顶部，并且移动前的距离不为 0 时，拉取日志
    if (this.direction === 'up' && this.preElm.scrollTop === 0 && this.prevDistanceToTop !== 0) {
      onGoToTop(); // 往上移动
    }
    this.prevDistanceToTop = distanceToTop;
  }

  toggleRolling = () => {
    const { rolling, onStartRolling, onCancelRolling } = this.props;
    if (rolling) {
      onCancelRolling();
    } else {
      onStartRolling();
    }
  };

  changeSize = () => {
    this.setState({
      fullScreen: !this.state.fullScreen,
    });
  };

  render() {
    const {
      content,
      rolling,
      hasLogs,
      onGoToTop,
      onGoToBottom,
      backwardLoading,
      CustomLogContent,
      extraButton,
      transformContent,
      searchOnce,
      onShowDownloadModal,
    } = this.props;
    const { fullScreen } = this.state;
    let logContent = rolling ? (
      <div className="flex">
        Loading... <ErdaIcon className="ml-1" type="loading" spin />
      </div>
    ) : (
      <span>No Log Currently</span>
    );
    const ContentComp = CustomLogContent || LogContent;
    if (content && content.length) {
      logContent = (
        <div className="log-content-wrap" onScroll={this.throttleScroll}>
          {backwardLoading ? <ErdaIcon type="loading" spin className="log-state top" /> : null}
          <div
            ref={(ref) => {
              this.preElm = ref;
            }}
            className="log-content"
          >
            <ContentComp {...this.props} logs={content} transformContent={transformContent} />
          </div>
          {rolling ? <ErdaIcon type="loading" spin className="log-state bottom" /> : null}
        </div>
      );
    }
    return (
      <div className={`log-roller darken${fullScreen ? ' show-max' : ''}`}>
        {logContent}
        <div className={`log-control log-top-controls ${extraButton ? '' : 'no-switch'}`}>
          {extraButton || null}
          {hasLogs && (
            <Tooltip title={i18n.t('common:download log')}>
              <Button onClick={onShowDownloadModal} type="ghost">
                {i18n.t('common:download log')}
              </Button>
            </Tooltip>
          )}
          <Button onClick={this.changeSize} type="ghost">
            {fullScreen ? i18n.t('default:exit full screen') : i18n.t('default:full screen')}
          </Button>
        </div>
        <div className="log-control btn-line-rtl">
          <Button onClick={() => onGoToBottom()} type="ghost">
            {i18n.t('back to bottom')}
          </Button>
          <Button onClick={() => onGoToTop()} type="ghost">
            {i18n.t('back to top')}
          </Button>
          {!searchOnce && (
            <Button onClick={this.toggleRolling} type="ghost">
              {rolling ? i18n.t('default:pause') : i18n.t('default:start')}
            </Button>
          )}
        </div>
      </div>
    );
  }
}
