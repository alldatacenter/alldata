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

import { Tooltip, Card, Alert } from 'antd';
import React from 'react';
import { Avatar, UserInfo, TagsRow } from 'common';
import { ISSUE_ICON } from '../../common/components/issue/issue-config';
import { useDrag } from 'react-dnd';
import classnames from 'classnames';
import permStore from 'user/stores/permission';
import './milestone-item.scss';
import i18n from 'i18n';

interface IProps {
  item: ISSUE.Epic;
  onClickItem: (task: ISSUE.Epic) => any;
}

export default ({ item, onClickItem }: IProps) => {
  const dragAuth = permStore.useStore((s) => s.project.epic.edit.pass);
  const [{ isDragging }, drag] = useDrag({
    item: { type: 'milestone', data: item },
    collect: (monitor) => ({
      isDragging: monitor.isDragging(),
    }),
    canDrag: () => {
      return dragAuth;
    },
  });

  const cls = classnames({
    'drag-wrap': true,
    dragging: isDragging,
    'milestone-info-card': true,
    rounded: true,
    'hover-active-bg': true,
    'border-all': true,
  });

  return (
    <>
      {isDragging && (
        <Alert
          message={i18n.t(
            'dop:It can only be dragged to the end of the corresponding month. Please go to the details page for better modification.',
          )}
          type="info"
          showIcon
        />
      )}
      <div key={item.id} onClick={() => onClickItem(item)} ref={drag}>
        <Card className={`shallow-shadow ${cls}`}>
          <div className="milestone-item-container milestone-info-card-content cursor-pointer">
            <div className="milestone-item nowrap lt">
              <Tooltip placement="top" title={item.title}>
                <span className="milestone-title">
                  {ISSUE_ICON.issue.EPIC}
                  {item.title}
                </span>
              </Tooltip>
              <span />
              {item.labels && (
                <TagsRow
                  labels={item.labels.map((l) => ({ label: l, color: 'red' }))}
                  showCount={2}
                  containerClassName="ml-2"
                />
              )}
            </div>
            <div className="rt">
              <div className="milestone-item">
                <Avatar wrapClassName="user-name" showName name={<UserInfo id={item.assignee} />} />
              </div>
            </div>
          </div>
        </Card>
      </div>
    </>
  );
};
