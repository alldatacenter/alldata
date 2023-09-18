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

import {
  CaretRightOutlined,
  EyeInvisibleOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import { Tooltip } from 'antd';
import { Popup, ToolbarButton, Tree } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { APP_CURRENT_VERSION } from 'app/migration/constants';
import classnames from 'classnames';
import { memo, useCallback, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import styled from 'styled-components/macro';
import { FONT_FAMILY, FONT_SIZE_BASE } from 'styles/StyleConstants';
import { CloneValueDeep, isEmptyArray } from 'utils/object';
import { uuidv4 } from 'utils/utils';
import { selectRoles } from '../../../MemberPage/slice/selectors';
import { SubjectTypes } from '../../../PermissionPage/constants';
import { SchemaTable } from '../../components/SchemaTable';
import { ViewViewModelStages } from '../../constants';
import { useViewSlice } from '../../slice';
import { selectCurrentEditingViewAttr } from '../../slice/selectors';
import {
  Column,
  ColumnPermission,
  HierarchyModel,
  ViewViewModel,
} from '../../slice/types';

const ROW_KEY = 'DATART_ROW_KEY';

interface ResultsProps {
  height?: number;
  width?: number;
}

export const Results = memo(({ height = 0, width = 0 }: ResultsProps) => {
  const { actions } = useViewSlice();
  const dispatch = useDispatch();
  const viewId = useSelector(state =>
    selectCurrentEditingViewAttr(state, { name: 'id' }),
  ) as string;
  const model = useSelector(state =>
    selectCurrentEditingViewAttr(state, { name: 'model' }),
  ) as HierarchyModel;
  const columnPermissions = useSelector(state =>
    selectCurrentEditingViewAttr(state, { name: 'columnPermissions' }),
  ) as ColumnPermission[];
  const stage = useSelector(state =>
    selectCurrentEditingViewAttr(state, { name: 'stage' }),
  ) as ViewViewModelStages;
  const previewResults = useSelector(state =>
    selectCurrentEditingViewAttr(state, { name: 'previewResults' }),
  ) as ViewViewModel['previewResults'];

  const roles = useSelector(selectRoles);
  const t = useI18NPrefix('view');

  const dataSource = useMemo(
    () => previewResults.map(o => ({ ...o, [ROW_KEY]: uuidv4() })),
    [previewResults],
  );

  const modelChange = useCallback(
    (columnName: string, column: Omit<Column, 'name'>) =>
      (keyPath: string[]) => {
        let value;
        if (keyPath[0].includes('category')) {
          const category = keyPath[0].split('-')[1];
          value = { ...column, category };
        } else if (keyPath.includes('DATE')) {
          value = { ...column, type: keyPath[1], dateFormat: keyPath[0] };
        } else {
          value = { ...column, type: keyPath[0] };
        }
        const clonedHierarchyModel = CloneValueDeep(model.hierarchy || {});
        if (columnName in clonedHierarchyModel) {
          clonedHierarchyModel[columnName] = value;
        } else {
          Object.values(clonedHierarchyModel)
            .filter(col => !isEmptyArray(col.children))
            .forEach(col => {
              const targetChildColumnIndex = col.children!.findIndex(
                child => child.name === columnName,
              );
              if (targetChildColumnIndex > -1) {
                col.children![targetChildColumnIndex] = value;
              }
            });
        }

        dispatch(
          actions.changeCurrentEditingView({
            model: {
              ...model,
              hierarchy: clonedHierarchyModel,
              version: APP_CURRENT_VERSION,
            },
          }),
        );
      },
    [dispatch, actions, model],
  );

  const roleDropdownData = useMemo(
    () =>
      roles.map(({ id, name }) => ({
        key: id,
        title: name,
        value: id,
        isLeaf: true,
      })),
    [roles],
  );

  const checkRoleColumnPermission = useCallback(
    columnName => checkedKeys => {
      const fullPermissions = Object.keys(model?.columns || {});
      dispatch(
        actions.changeCurrentEditingView({
          columnPermissions: roleDropdownData.reduce<ColumnPermission[]>(
            (updated, { key }) => {
              const permission = columnPermissions.find(
                ({ subjectId }) => subjectId === key,
              );
              const checkOnCurrentRole = checkedKeys.includes(key);
              if (permission) {
                if (checkOnCurrentRole) {
                  const updatedColumnPermission = Array.from(
                    new Set(permission.columnPermission.concat(columnName)),
                  );
                  return fullPermissions.sort().join(',') !==
                    updatedColumnPermission.sort().join(',')
                    ? updated.concat({
                        ...permission,
                        columnPermission: updatedColumnPermission,
                      })
                    : updated;
                } else {
                  return updated.concat({
                    ...permission,
                    columnPermission: permission.columnPermission.filter(
                      c => c !== columnName,
                    ),
                  });
                }
              } else {
                return !checkOnCurrentRole
                  ? updated.concat({
                      id: uuidv4(),
                      viewId,
                      subjectId: key,
                      subjectType: SubjectTypes.Role,
                      columnPermission: fullPermissions.filter(
                        c => c !== columnName,
                      ),
                    })
                  : updated;
              }
            },
            [],
          ),
        }),
      );
    },
    [dispatch, actions, viewId, model, columnPermissions, roleDropdownData],
  );

  const getExtraHeaderActions = useCallback(
    (name: string, column: Omit<Column, 'name'>) => {
      // 没有记录相当于对所有字段都有权限
      const checkedKeys =
        columnPermissions.length > 0
          ? roleDropdownData.reduce<string[]>((selected, { key }) => {
              const permission = columnPermissions.find(
                ({ subjectId }) => subjectId === key,
              );
              if (permission) {
                return permission.columnPermission.includes(name)
                  ? selected.concat(key)
                  : selected;
              } else {
                return selected.concat(key);
              }
            }, [])
          : roleDropdownData.map(({ key }) => key);
      return [
        <Popup
          key={`${name}_columnpermission`}
          trigger={['click']}
          placement="bottomRight"
          content={
            <Tree
              className="check-list medium"
              treeData={roleDropdownData}
              checkedKeys={checkedKeys}
              loading={false}
              selectable={false}
              showIcon={false}
              onCheck={checkRoleColumnPermission(name)}
              blockNode
              checkable
            />
          }
        >
          <Tooltip title={t('columnPermission.title')}>
            <ToolbarButton
              size="small"
              iconSize={FONT_SIZE_BASE}
              icon={
                checkedKeys.length > 0 ? (
                  <EyeOutlined
                    className={classnames({
                      partial: checkedKeys.length !== roleDropdownData.length,
                    })}
                  />
                ) : (
                  <EyeInvisibleOutlined />
                )
              }
            />
          </Tooltip>
        </Popup>,
      ];
    },
    [columnPermissions, roleDropdownData, checkRoleColumnPermission, t],
  );

  const pagination = useMemo(
    () => ({
      defaultPageSize: 100,
      pageSizeOptions: ['100', '200', '500', '1000'],
    }),
    [],
  );

  return stage > ViewViewModelStages.Fresh ? (
    <TableWrapper>
      <SchemaTable
        height={height ? height - 96 : 0}
        width={width}
        model={model.columns || {}}
        hierarchy={model.hierarchy || {}}
        dataSource={dataSource}
        pagination={pagination}
        getExtraHeaderActions={getExtraHeaderActions}
        onSchemaTypeChange={modelChange}
        hasCategory
      />
    </TableWrapper>
  ) : (
    <InitialDesc>
      <p>
        {t('resultEmpty1')}
        <CaretRightOutlined />
        {t('resultEmpty2')}
      </p>
    </InitialDesc>
  );
});

const InitialDesc = styled.div`
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;

  p {
    color: ${p => p.theme.textColorLight};
  }
`;

const TableWrapper = styled.div`
  flex: 1;
  overflow: hidden;
  font-family: ${FONT_FAMILY};
  background-color: ${p => p.theme.componentBackground};
`;
