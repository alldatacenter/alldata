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

import React, { useState } from 'react';
import i18n from 'i18n';
import { map } from 'lodash';
import { Row, Col, Select, Input, Spin, Table } from 'antd';
import { IF } from 'common';
import { goTo } from 'common/utils';
import { getFormatter } from 'app/charts/utils/formatter';
import { useMount, useDebounce } from 'react-use';
import { useLoading } from 'core/stores/loading';
import middlewareDashboardStore from '../../stores/middleware-dashboard';
import { AddonUsageChart } from './usage-chart';
import { ColumnProps } from 'core/common/interface';

import './index.scss';

const { Option } = Select;
const { Search } = Input;

const envOptions = [
  { cnName: i18n.t('all'), enName: 'ALL' },
  { cnName: i18n.t('develop'), enName: 'DEV' },
  { cnName: i18n.t('test'), enName: 'TEST' },
  { cnName: i18n.t('staging'), enName: 'STAGING' },
  { cnName: i18n.t('production'), enName: 'PROD' },
].map(({ cnName, enName }) => (
  <Option key={enName} value={enName}>
    {cnName}
  </Option>
));

const MiddlewareDashboard = () => {
  const [projectList, middlewares, middelwaresPaging] = middlewareDashboardStore.useStore((s) => [
    s.projectList,
    s.middlewares,
    s.middelwaresPaging,
  ]);
  const { getProjects, getMiddlewares, getAddonUsage, getAddonDailyUsage } = middlewareDashboardStore.effects;
  const { clearMiddlewares } = middlewareDashboardStore.reducers;
  const [getProjectsLoading, getMiddlewaresLoading] = useLoading(middlewareDashboardStore, [
    'getProjects',
    'getMiddlewares',
  ]);

  const [workspace, setWorkspace] = useState('ALL');
  const [projectId, setProjectId] = useState();
  const [addonName, setAddonName] = useState(undefined as string | undefined);
  const [ip, setIp] = useState(undefined as string | undefined);

  useDebounce(
    () => {
      const searchQuery = { workspace: workspace === 'ALL' ? undefined : workspace, projectId, addonName, ip };
      getMiddlewares(searchQuery);
      getAddonUsage(searchQuery);
      getAddonDailyUsage(searchQuery);
      return clearMiddlewares;
    },
    600,
    [workspace, projectId, addonName, ip, getMiddlewares, clearMiddlewares, getAddonUsage, getAddonDailyUsage],
  );

  const handleEnvChange = (value: string) => {
    setWorkspace(value);
  };

  const handleProjectChange = (value: string) => {
    setProjectId(value);
  };

  const handleSearchProjects = (searchKey?: string) => {
    getProjects(searchKey);
  };

  const handleSearchAddon = (searchKey?: string) => {
    setAddonName(searchKey || undefined);
  };

  const handleSearchIp = (searchKey?: string) => {
    setIp(searchKey || undefined);
  };

  useMount(() => {
    handleSearchProjects();
    getAddonUsage();
    getAddonDailyUsage();
  });

  // const overviewItemNameMap = {
  //   cpu: 'CPU',
  //   mem: i18n.t('memory'),
  //   nodes: i18n.t('node'),
  // };

  const handleTableChange = (pagination: any) => {
    const { current, pageSize: page_Size } = pagination;
    getMiddlewares({
      workspace: workspace === 'ALL' ? undefined : workspace,
      projectId,
      addonName,
      pageNo: current,
      pageSize: page_Size,
    });
  };

  const middlewareCols: Array<ColumnProps<MIDDLEWARE_DASHBOARD.IMiddlewareDetail>> = [
    {
      title: i18n.t('addon'),
      dataIndex: 'name',
      key: 'name',
      width: '35%',
      render: (value: string) => <span className="hover-text font-bold">{value}</span>,
    },
    {
      title: i18n.t('cluster'),
      dataIndex: 'clusterName',
      key: 'clusterName',
      width: '20%',
    },
    {
      title: i18n.t('project'),
      dataIndex: 'projectName',
      key: 'projectName',
      width: '20%',
    },
    {
      title: 'CPU',
      dataIndex: 'cpu',
      key: 'cpu',
      width: '10%',
      sorter: (a: MIDDLEWARE_DASHBOARD.IMiddlewareDetail, b: MIDDLEWARE_DASHBOARD.IMiddlewareDetail) => a.cpu - b.cpu,
    },
    {
      title: i18n.t('memory'),
      dataIndex: 'mem',
      key: 'mem',
      width: '10%',
      render: (value: number) => getFormatter('CAPACITY', 'MB').format(value),
      sorter: (a: MIDDLEWARE_DASHBOARD.IMiddlewareDetail, b: MIDDLEWARE_DASHBOARD.IMiddlewareDetail) => a.mem - b.mem,
    },
    {
      title: i18n.t('node'),
      dataIndex: 'nodes',
      key: 'nodes',
      width: 80,
      sorter: (a: MIDDLEWARE_DASHBOARD.IMiddlewareDetail, b: MIDDLEWARE_DASHBOARD.IMiddlewareDetail) =>
        a.nodes - b.nodes,
    },
    {
      title: i18n.t('cmp:number of references'),
      dataIndex: 'attachCount',
      key: 'attachCount',
      width: 200,
      sorter: (a: MIDDLEWARE_DASHBOARD.IMiddlewareDetail, b: MIDDLEWARE_DASHBOARD.IMiddlewareDetail) =>
        a.attachCount - b.attachCount,
    },
  ];
  const { pageNo, pageSize, total } = middelwaresPaging;

  return (
    <>
      <div className="middleware-dashboard-content">
        <div className="middleware-dashboard-top">
          <div className="filter-group-ct mb-8">
            <Row gutter={20}>
              <Col span={6} className="filter-item">
                <div className="filter-item-label">{i18n.t('environment')}</div>
                <Select className="filter-item-content" value={workspace} onChange={handleEnvChange}>
                  {envOptions}
                </Select>
              </Col>
              <Col span={6} className="filter-item">
                <div className="filter-item-label">{i18n.t('project')}</div>
                <Select
                  showSearch
                  className="filter-item-content"
                  allowClear
                  value={projectId}
                  placeholder={i18n.t('search by keyword')}
                  notFoundContent={
                    <IF check={getProjectsLoading}>
                      <Spin size="small" />
                    </IF>
                  }
                  filterOption={false}
                  onSearch={handleSearchProjects}
                  onChange={handleProjectChange}
                >
                  {map(projectList, (d) => (
                    <Option key={d.id}>{d.name}</Option>
                  ))}
                </Select>
              </Col>
              <Col span={6} className="filter-item">
                <div className="filter-item-label">Addon</div>
                <Search
                  allowClear
                  className="filter-item-content"
                  placeholder={i18n.t('search by Addon name or ID')}
                  onChange={(e) => handleSearchAddon(e.target.value)}
                />
              </Col>
              <Col span={6} className="filter-item">
                <div className="filter-item-label">IP</div>
                <Search
                  allowClear
                  className="filter-item-content"
                  placeholder={i18n.t('cmp:search by container IP')}
                  onChange={(e) => handleSearchIp(e.target.value)}
                />
              </Col>
            </Row>
          </div>
          {/* <Row className="middleware-overview-ct mb-4" type="flex" justify="space-between" gutter={50}>
          {
            map(overview, (v, k) => (
              <Col span={8} key={k}>
                <div className="middleware-overview-item border-all">
                  <div className="title mb-5">{overviewItemNameMap[k]}</div>
                  <div className="num">{v}</div>
                </div>
              </Col>
            ))
          }
        </Row> */}
          <AddonUsageChart />
        </div>
      </div>
      <Table
        className="cursor-pointer"
        rowKey="instanceId"
        columns={middlewareCols}
        dataSource={middlewares}
        loading={getMiddlewaresLoading}
        pagination={{
          current: pageNo,
          pageSize,
          total,
        }}
        onRow={({ instanceId }: MIDDLEWARE_DASHBOARD.IMiddlewareDetail) => ({
          onClick: () => {
            goTo(`./${instanceId}/monitor`);
          },
        })}
        onChange={handleTableChange}
        scroll={{ x: '100%' }}
      />
    </>
  );
};

export default MiddlewareDashboard;
