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
import memberLabelStore from 'common/stores/member-label';
import { useMount } from 'react-use';
import './member-label.scss';

export const MemberLabels = () => {
  const list = memberLabelStore.useStore((s) => s.memberLabels);
  const { getMemberLabels } = memberLabelStore.effects;

  useMount(() => {
    getMemberLabels();
  });

  return (
    <div className="member-label-list">
      {list.map(({ label, name }) => (
        <span key={label} className="label-item">
          {name}
        </span>
      ))}
    </div>
  );
};
