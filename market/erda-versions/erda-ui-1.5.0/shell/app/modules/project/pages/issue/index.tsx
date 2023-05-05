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
import routeInfoStore from 'core/stores/route';
import Requirement from './requirement';
import Task from './task';
import Bug from './bug';
import AllIssue from './all';
import Board from './board';

const issueTypeMap = {
  all: {
    Comp: AllIssue,
  },
  requirement: {
    Comp: Requirement,
  },
  task: {
    Comp: Task,
  },
  bug: {
    Comp: Bug,
  },
  board: {
    Comp: Board,
  },
};

const Issues = () => {
  const issueType = routeInfoStore.useStore((s) => s.params.issueType);
  const { Comp } = issueTypeMap[issueType] || {};
  return <>{Comp && <Comp />}</>;
};

export default Issues;
