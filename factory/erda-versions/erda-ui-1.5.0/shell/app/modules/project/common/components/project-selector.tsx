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
import { LoadMoreSelector, Icon as CustomIcon } from 'common';
import { goTo } from 'common/utils';
import { map } from 'lodash';
import { Tooltip } from 'antd';
import { getJoinedProjects } from 'user/services/user';
import routeInfoStore from 'core/stores/route';
import projectStore from 'project/stores/project';
import './project-selector.scss';

interface IProps {
  [pro: string]: any;
  value: string | number;
  onClickItem: (arg?: any) => void;
  projectId?: string;
}

const ProjectItem = (project: PROJECT.Detail) => {
  return (
    <Tooltip key={project.id} title={project.name}>
      {project.displayName || project.name}
    </Tooltip>
  );
};
const noop = () => {};

export const ProjectSelector = (props: IProps) => {
  const getData = (_q: any = {}) => {
    return getJoinedProjects(_q).then((res: any) => res.data);
  };

  return (
    <LoadMoreSelector
      getData={getData}
      dataFormatter={({ list, total }) => ({
        total,
        list: map(list, (item) => ({ ...item, label: item.displayName || item.name, value: item.id })),
      })}
      optionRender={ProjectItem}
      {...props}
    />
  );
};

const headProjectRender = (val: any = {}) => {
  const curProject = projectStore.getState((s) => s.info);
  const name = val.displayName || val.name || curProject.displayName || curProject.name || '';
  return (
    <div className="head-project-name">
      <span className="nowrap text-base font-bold" title={name}>
        {name}
      </span>
      <CustomIcon className="caret" type="caret-down" />
    </div>
  );
};

export const HeadProjectSelector = () => {
  const { projectId } = routeInfoStore.useStore((s) => s.params);
  return (
    <div className="head-project-selector mt-2">
      <ProjectSelector
        valueItemRender={headProjectRender}
        value={projectId}
        onClickItem={(project: PROJECT.Detail) => {
          // 切换project
          goTo(goTo.pages.project, { projectId: project.id });
        }}
      />
    </div>
  );
};
