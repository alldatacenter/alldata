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
import { ISSUE_TYPE_MAP } from 'project/common/components/issue/issue-config';
import projectLabelStore from 'project/stores/label';
import { renderLabels } from 'project/common/components/issue/issue-labels';

import './title-label.scss';

interface IProps {
  data: ISSUE.Issue;
  onClick?: () => void;
}

const TitleLabel = (props: IProps) => {
  const { data, onClick } = props;
  const { title, labels, type } = data;
  const labelList = projectLabelStore.useStore((s) => s.list);

  const colorMap = {} as Obj;
  labelList.forEach((l) => {
    colorMap[l.name] = l.color;
  });
  return (
    <div className="issue-title-label w-full cursor-pointer pl-2 flex items-center" onClick={onClick}>
      {ISSUE_TYPE_MAP[type]?.icon}
      <div className="issue-title-text">
        <div className="nowrap">{title}</div>
        {renderLabels(colorMap, labels, 'issue-text-label', 2)}
      </div>
    </div>
  );
};

export default TitleLabel;
