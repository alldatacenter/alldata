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
import moment from 'moment';
import { map, groupBy } from 'lodash';
import { Timeline, Tooltip } from 'antd';
import { Avatar, IF } from 'common';
import { actionMap } from '../config';
import i18n from 'i18n';

import './activity.scss';

const TimelineItem = Timeline.Item;
const { ELSE } = IF;
const noop = () => {};

// 根据不同动态类型获取内容
const getContent = ({ action }: { context: any; action: any }) => {
  let content = null;
  let extraItems = null;

  switch (action) {
    // 项目动态
    case 'P_ADD':
    case 'P_DEL':
    case 'P_MOD':
    case 'P_MEMBER_ADD':
    case 'P_MEMBER_DEL':
    case 'P_MEMBER_MOD': {
      break;
    }
    // 应用动态
    case 'A_ADD':
    case 'A_DEL':
    case 'A_MOD':
    case 'A_MEMBER_ADD':
    case 'A_MEMBER_DEL':
    case 'A_MEMBER_MOD': {
      break;
    }
    // runtime 动态
    case 'R_ADD':
    case 'R_DEL':
    case 'R_MOD': {
      break;
    }
    // 部署动态
    case 'R_DEPLOY_START':
    case 'R_DEPLOY_OK':
    case 'R_DEPLOY_FAIL':
    case 'R_DEPLOY_CANCEL': {
      break;
    }
    // 构建动态
    case 'B_CREATE':
    case 'B_START':
    case 'B_CANCEL':
    case 'B_FAILED':
    case 'B_END': {
      break;
    }
    // 推送代码
    case 'G_PUSH': {
      break;
    }
    default:
      content = null;
      extraItems = null;
  }

  return { content, extraItems };
};

interface IActivity {
  [prop: string]: any;
  operator: any;
  action: any;
  context: any;
}

interface IProps {
  list: any[];
  userMap: any;
  isLoading: boolean;
  hasMore?: boolean;
  customProps?: any;
  CustomCard?: any; // ({ activity }: { activity: IActivity }) => JSX.Element | null;
  loadMore?: () => void;
}

export const ActiveCard = ({ operator, action, context }: IActivity) => {
  const { content, extraItems } = getContent({ action, context });
  let avatar = '';
  let nick = '';
  let name = '';

  if (operator) {
    avatar = operator.avatar;
    nick = operator.nick || '';
    name = operator.name || '';
  } else if (context.action === 'G_PUSH') {
    avatar = context.pusher.avatar;
    nick = context.pusher.name;
    name = context.pusher.name;
  }
  return (
    <div className="active-card">
      <div className="active-header">
        <Avatar size={32} name={nick} url={avatar} />
        <div className="info">
          <Tooltip title={name}>
            <span className="name mr-2 font-medium">{nick}</span>
          </Tooltip>
          <span>{actionMap[action]}</span>
        </div>
      </div>
      <div className="extra-items">{extraItems}</div>
      {content ? <div className="active-content">{content}</div> : null}
    </div>
  );
};

export const TimelineActivity = ({
  list,
  userMap,
  isLoading = false,
  hasMore = false,
  loadMore = noop,
  CustomCard,
}: IProps) => {
  // 依据每一条动态的 timeStamp 区分出时间范围
  const groupActivityList = groupBy(
    map(list, ({ timeStamp, ...rest }) => ({ ...rest, timeStamp, timeRange: moment(timeStamp).format('YYYY-MM-DD') })),
    'timeRange',
  );
  const ranges = Object.keys(groupActivityList);

  return (
    <Timeline className="activity-timeline" pending={isLoading ? `${i18n.t('dop:loading')}...` : false}>
      {map(ranges, (range, i) => {
        return (
          <TimelineItem key={i}>
            <div className="time tc2">{range}</div>
            <div className="list">
              {map(groupActivityList[range], (activity) => {
                const { id } = activity;
                const props = { activity, key: id, userMap };
                const Comp = CustomCard || ActiveCard;
                return <Comp {...props} key={id} />;
              })}
            </div>
          </TimelineItem>
        );
      })}
      <IF check={hasMore && !isLoading}>
        <TimelineItem key="key-load" className="load-more">
          <a onClick={() => loadMore()}>{i18n.t('load more')}</a>
        </TimelineItem>
        <ELSE />
        <TimelineItem />
      </IF>
    </Timeline>
  );
};
