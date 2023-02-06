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

import { useUpdate } from 'common/use-hooks';
import { EmptyListHolder } from 'common';
import { isEmpty, groupBy, sortBy, filter } from 'lodash';
import moment from 'moment';
import { Timeline } from 'antd';
import React from 'react';
import './milestone-table.scss';
import MilestoneGroup from './milestone-group';

interface IProps {
  iterationDetail?: ITERATION.Detail;
  epic: ISSUE.Epic[];
  paging: IPaging;
  reload: () => void;
  onClickItem: (task: ISSUE.Epic) => void;
}

export default ({ epic, onClickItem, reload }: IProps) => {
  const [
    {
      groupedData,
      // iterationMap,
    },
    updater,
  ] = useUpdate({
    groupedData: epic,
  });

  React.useEffect(() => {
    updater.groupedData(epic);
  }, [epic, updater]);

  const filterColData = filter(groupedData, (ele) => {
    return ele.planFinishedAt !== null;
  });
  const sortColData = sortBy(filterColData, (item) => {
    return moment(item.planFinishedAt).valueOf();
  });
  const groupColData = Object.entries(
    groupBy(sortColData, (ele) => {
      return moment(ele.planFinishedAt).format('YYYY-MM');
    }),
  );

  return (
    <div className="milestone-timeline-container">
      <Timeline>
        {isEmpty(groupColData) ? (
          <EmptyListHolder />
        ) : (
          groupColData.map((ele) => <MilestoneGroup reload={reload} ele={ele} onClickItem={onClickItem} key={ele[0]} />)
        )}
      </Timeline>
    </div>
  );
};
