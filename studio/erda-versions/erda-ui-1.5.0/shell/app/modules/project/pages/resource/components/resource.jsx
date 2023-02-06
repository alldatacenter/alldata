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
import { Breadcrumb } from 'antd';
import { IF, TimeSelector, ErdaIcon } from 'common';
import { isEmpty } from 'lodash';
import ChartList from '../containers/chart';
import ServiceList from '../containers/service-list';
import projectStore from 'app/modules/project/stores/project';
import monitorCommonStore from 'common/stores/monitorCommon';
import './resource.scss';
import appStore from 'application/stores/application';
import routeInfoStore from 'core/stores/route';

class ProjectResource extends React.PureComponent {
  constructor(props) {
    super(props);
    const {
      params: { projectId, appId },
      applicationName,
      projectName,
    } = props;
    let paths = [];
    let startLevel = '';
    if (appId) {
      // 根据路由参数若是有appId，则层级从runtime开始
      applicationName && (paths = [{ q: appId, name: applicationName }]);
      startLevel = 'runtime';
    } else if (projectId) {
      // 若有projectId，则层级从application开始
      projectName && (paths = [{ q: projectId, name: projectName }]);
      startLevel = 'application';
    }
    this.state = {
      paths,
      projectName,
      applicationName,
      startLevel,
    };
  }

  static getDerivedStateFromProps(nextProps, prevState) {
    const {
      params: { projectId, appId },
      projectName,
      applicationName,
    } = nextProps;
    const { startLevel } = prevState;
    if (startLevel === 'runtime' && applicationName && applicationName !== prevState.applicationName) {
      return { paths: [{ q: appId, name: applicationName }], applicationName };
    } else if (startLevel === 'application' && projectName && projectName !== prevState.projectName) {
      return { paths: [{ q: projectId, name: projectName }], projectName };
    }
    return null;
  }

  into = (p) => {
    this.setState({
      paths: this.state.paths.concat(p),
    });
  };

  backTo = (depth) => {
    this.setState({
      paths: this.state.paths.slice(0, depth + 1),
    });
  };

  render() {
    const { paths, startLevel } = this.state;
    const { timeSpan } = this.props;
    return (
      <div className="project-resource">
        <Breadcrumb
          separator={<ErdaIcon className="text-xs align-middle" type="right" size="14px" />}
          className="path-breadcrumb"
        >
          {paths.map((p, i) => {
            const isLast = i === paths.length - 1;
            return (
              <Breadcrumb.Item
                key={i}
                className={isLast ? '' : 'hover-active'}
                onClick={() => {
                  if (!isLast) {
                    this.backTo(i);
                  }
                }}
              >
                {p.name}
              </Breadcrumb.Item>
            );
          })}
        </Breadcrumb>
        <IF check={!isEmpty(paths)}>
          <ServiceList into={this.into} paths={paths} startLevel={startLevel} />
          <div className="project-resource-time">
            <TimeSelector defaultTime={24} />
          </div>
          <ChartList paths={paths} startLevel={startLevel} timeSpan={timeSpan} />
        </IF>
      </div>
    );
  }
}

export default (props) => {
  const info = projectStore.useStore((s) => s.info);
  const params = routeInfoStore.useStore((s) => s.params);
  const { name: applicationName } = appStore.useStore((s) => s.detail);
  const timeSpan = monitorCommonStore.useStore((s) => s.timeSpan);
  return (
    <ProjectResource
      {...props}
      projectName={info.name}
      applicationName={applicationName}
      params={params}
      timeSpan={timeSpan}
    />
  );
};
