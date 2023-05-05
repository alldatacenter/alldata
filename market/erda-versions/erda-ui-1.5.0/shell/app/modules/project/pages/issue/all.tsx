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
import { ISSUE_TYPE } from 'project/common/components/issue/issue-config';
import IssueProtocol from './issue-protocol';
import { RadioTabs } from 'common';
import { updateSearch } from 'common/utils';
import routeInfoStore from 'core/stores/route';
import i18n from 'i18n';

const AllIssue = () => {
  const { type: rIssueType } = routeInfoStore.useStore((s) => s.query);
  const [issueType, setIssueType] = React.useState(rIssueType || ISSUE_TYPE.ALL);
  const options = [
    { value: ISSUE_TYPE.ALL, label: i18n.t('dop:all issues') },
    { value: ISSUE_TYPE.REQUIREMENT, label: i18n.t('requirement') },
    { value: ISSUE_TYPE.TASK, label: i18n.t('task') },
    { value: ISSUE_TYPE.BUG, label: i18n.t('bug') },
  ];

  return (
    <div>
      <RadioTabs
        options={options}
        value={issueType}
        onChange={(v: string) => {
          updateSearch({ type: v }, { ignoreOrigin: true });
          setIssueType(v);
        }}
        className="mb-2"
      />
      <IssueProtocol key={issueType} issueType={issueType} />
    </div>
  );
};

export default AllIssue;
