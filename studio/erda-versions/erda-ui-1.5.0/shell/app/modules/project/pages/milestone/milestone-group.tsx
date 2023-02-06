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

import { Timeline } from 'antd';
import moment from 'moment';
import React from 'react';
import MilestoneItem from './milestone-item';
import { useDrop } from 'react-dnd';
import issueStore from 'project/stores/issues';
import './milestone-group.scss';

interface IProps {
  ele: Array<string | ISSUE.Epic[]>;
  reload: () => void;
  onClickItem: (task: ISSUE.Epic) => void;
}

export default ({ ele, onClickItem, reload }: IProps) => {
  const [date, list] = ele;
  let timePoint;
  const targetDate = moment(date as string);
  const now = moment();
  if (targetDate.isBefore(now, 'month')) {
    timePoint = 'gray';
  } else if (targetDate.isSame(now, 'month')) {
    timePoint = 'green';
  } else {
    timePoint = 'blue';
  }
  const { updateIssue } = issueStore.effects;
  const [{ isOver }, drop] = useDrop({
    accept: 'milestone',
    drop: (item: any) => {
      if (moment(item.data.planFinishedAt).format('YYYY-MM') === date) {
        return;
      }
      const planFinishedAt = moment(date).endOf('month').format();
      updateIssue({ ...item.data, planFinishedAt });
      reload();
    },
    collect: (monitor) => ({ isOver: monitor.isOver() }),
  });

  return (
    <div ref={drop}>
      <Timeline.Item key={date} color={timePoint}>
        <h3>{date}</h3>
        <div className={`milestone-module ${isOver ? 'milestone-drag-over' : ''}`}>
          {list.map((item: any) => {
            return <MilestoneItem key={item.id} item={item} onClickItem={onClickItem} />;
          })}
        </div>
      </Timeline.Item>
    </div>
  );
};
