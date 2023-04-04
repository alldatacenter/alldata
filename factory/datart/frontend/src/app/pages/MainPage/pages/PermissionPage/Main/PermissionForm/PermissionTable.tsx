/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { SearchOutlined } from '@ant-design/icons';
import { Col, Input, Row, Table, TableColumnProps } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { useSearchAndExpand } from 'app/hooks/useSearchAndExpand';
import { memo, useEffect, useMemo } from 'react';
import styled from 'styled-components/macro';
import { listToTree } from 'utils/utils';
import {
  PermissionLevels,
  ResourceTypes,
  SubjectTypes,
  Viewpoints,
  VizResourceSubTypes,
} from '../../constants';
import {
  DataSourceTreeNode,
  DataSourceViewModel,
  Privilege,
} from '../../slice/types';
import { PrivilegeSetting } from './PrivilegeSetting';
import { getPrivilegeSettingWidth, setTreeDataWithPrivilege } from './utils';

interface PermissionTableProps {
  viewpoint: Viewpoints;
  viewpointType: ResourceTypes | SubjectTypes;
  dataSourceType: ResourceTypes | SubjectTypes;
  vizSubTypes?: VizResourceSubTypes;
  dataSource: DataSourceViewModel[] | undefined;
  resourceLoading: boolean;
  privileges: Privilege[] | undefined;
  onPrivilegeChange: (
    treeData: DataSourceTreeNode[],
  ) => (
    record: DataSourceTreeNode,
    newPermission: PermissionLevels[],
    index: number,
    base: PermissionLevels,
  ) => void;
}

export const PermissionTable = memo(
  ({
    viewpoint,
    viewpointType,
    dataSourceType,
    dataSource,
    resourceLoading,
    privileges,
    onPrivilegeChange,
    vizSubTypes,
  }: PermissionTableProps) => {
    const t = useI18NPrefix('permission');

    const treeData = useMemo(() => {
      if (dataSource && privileges) {
        const originTreeData = listToTree(
          dataSource,
          null,
          [],
        ) as DataSourceTreeNode[];
        return setTreeDataWithPrivilege(
          originTreeData,
          [...privileges],
          viewpoint,
          viewpointType,
          dataSourceType,
          vizSubTypes,
        );
      } else {
        return [];
      }
    }, [
      viewpoint,
      viewpointType,
      dataSourceType,
      dataSource,
      privileges,
      vizSubTypes,
    ]);

    const {
      filteredData,
      expandedRowKeys,
      onExpand,
      debouncedSearch,
      setExpandedRowKeys,
    } = useSearchAndExpand(treeData, (keywords, d) =>
      d.name.includes(keywords),
    );

    useEffect(() => {
      if (dataSource?.length && viewpoint === Viewpoints.Subject) {
        setExpandedRowKeys([dataSource[0].id]);
      }
    }, [viewpoint, dataSource, setExpandedRowKeys]);

    const privilegeChange = useMemo(
      () => onPrivilegeChange(treeData),
      [treeData, onPrivilegeChange],
    );

    const columns = useMemo(() => {
      const columns: TableColumnProps<DataSourceTreeNode>[] = [
        {
          dataIndex: 'name',
          title:
            viewpoint === Viewpoints.Resource
              ? t('subjectName')
              : t('resourceName'),
        },
        {
          title: t('privileges'),
          align: 'center' as const,
          width: getPrivilegeSettingWidth(
            viewpoint,
            viewpointType,
            dataSourceType,
            vizSubTypes,
          ),
          render: (_, record) => (
            <PrivilegeSetting
              record={record}
              viewpoint={viewpoint}
              viewpointType={viewpointType}
              dataSourceType={dataSourceType}
              onChange={privilegeChange}
              vizSubTypes={vizSubTypes}
            />
          ),
        },
      ];
      return columns;
    }, [
      viewpoint,
      viewpointType,
      dataSourceType,
      privilegeChange,
      t,
      vizSubTypes,
    ]);

    return (
      <>
        <Toolbar>
          <Col span={12}>
            <Input
              placeholder={t('searchResources')}
              prefix={<SearchOutlined className="icon" />}
              bordered={false}
              onChange={debouncedSearch}
            />
          </Col>
        </Toolbar>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={filteredData}
          loading={resourceLoading}
          pagination={false}
          size="middle"
          expandable={{
            expandedRowKeys,
            onExpandedRowsChange: onExpand,
          }}
          bordered
        />
      </>
    );
  },
);

const Toolbar = styled(Row)`
  .icon {
    color: ${p => p.theme.textColorLight};
  }
`;
