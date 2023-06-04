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
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import {
  Button,
  Card,
  message,
  Popconfirm,
  Space,
  Table,
  TableColumnProps,
  Tag,
  Tooltip,
} from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { CommonFormTypes, TIME_FORMATTER } from 'globalConstants';
import { Moment } from 'moment';
import { Key, useCallback, useEffect, useMemo, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import styled from 'styled-components/macro';
import {
  BORDER_RADIUS,
  FONT_SIZE_TITLE,
  FONT_WEIGHT_MEDIUM,
  INFO,
  LINE_HEIGHT_TITLE,
  SPACE_LG,
  SPACE_MD,
  WARNING,
} from 'styles/StyleConstants';
import { request2 } from 'utils/request';
import { errorHandle, getDiffParams } from 'utils/utils';
import { selectOrgId } from '../../slice/selectors';
import { useMemberSlice } from '../MemberPage/slice';
import { getMembers, getRoles } from '../MemberPage/slice/thunks';
import { VariableScopes, VariableTypes, VariableValueTypes } from './constants';
import { useVariableSlice } from './slice';
import {
  selectDeleteVariablesLoading,
  selectSaveVariableLoading,
  selectVariableListLoading,
  selectVariables,
} from './slice/selectors';
import {
  addVariable,
  deleteVariable,
  editVariable,
  getVariables,
} from './slice/thunks';
import {
  RowPermission,
  RowPermissionRaw,
  Variable,
  VariableViewModel,
} from './slice/types';
import { SubjectForm } from './SubjectForm';
import { VariableFormModel } from './types';
import { VariableForm } from './VariableForm';

export function VariablePage() {
  useMemberSlice();
  useVariableSlice();
  const [formType, setFormType] = useState(CommonFormTypes.Add);
  const [formVisible, setFormVisible] = useState(false);
  const [editingVariable, setEditingVariable] = useState<undefined | Variable>(
    void 0,
  );
  const [rowPermissions, setRowPermissions] = useState<
    undefined | RowPermission[]
  >(void 0);
  const [rowPermissionLoading, setRowPermissionLoading] = useState(false);
  const [subjectFormVisible, setSubjectFormVisible] = useState(false);
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([]);
  const [updateRowPermissionLoading, setUpdateRowPermissionLoading] =
    useState(false);
  const dispatch = useDispatch();
  const variables = useSelector(selectVariables);
  const listLoading = useSelector(selectVariableListLoading);
  const saveLoading = useSelector(selectSaveVariableLoading);
  const deleteVariablesLoading = useSelector(selectDeleteVariablesLoading);
  const orgId = useSelector(selectOrgId);
  const t = useI18NPrefix('variable');
  const tg = useI18NPrefix('global');

  useEffect(() => {
    dispatch(getVariables(orgId));
    dispatch(getMembers(orgId));
    dispatch(getRoles(orgId));
  }, [dispatch, orgId]);

  const showAddForm = useCallback(() => {
    setFormType(CommonFormTypes.Add);
    setFormVisible(true);
  }, []);

  const hideForm = useCallback(() => {
    setFormVisible(false);
  }, []);

  const afterFormClose = useCallback(() => {
    setEditingVariable(void 0);
  }, []);

  const showEditForm = useCallback(
    id => () => {
      setEditingVariable(variables.find(v => v.id === id));
      setFormType(CommonFormTypes.Edit);
      setFormVisible(true);
    },
    [variables],
  );

  const showSubjectForm = useCallback(
    id => async () => {
      const variable = variables.find(v => v.id === id)!;
      setEditingVariable(variable);
      setSubjectFormVisible(true);

      setRowPermissionLoading(true);
      const { data } = await request2<RowPermissionRaw[]>(
        `/variables/value?variableId=${id}`,
      );
      setRowPermissions(
        data.map(d => ({
          ...d,
          value: d.value && JSON.parse(d.value),
        })),
      );
      setRowPermissionLoading(false);
    },
    [variables],
  );

  const hideSubjectForm = useCallback(() => {
    setSubjectFormVisible(false);
  }, []);

  const del = useCallback(
    id => () => {
      dispatch(
        deleteVariable({
          ids: [id],
          resolve: () => {
            message.success(tg('operation.deleteSuccess'));
          },
        }),
      );
    },
    [dispatch, tg],
  );

  const delSelectedVariables = useCallback(() => {
    dispatch(
      deleteVariable({
        ids: selectedRowKeys as string[],
        resolve: () => {
          message.success(tg('operation.deleteSuccess'));
          setSelectedRowKeys([]);
        },
      }),
    );
  }, [dispatch, selectedRowKeys, tg]);

  const save = useCallback(
    (values: VariableFormModel) => {
      let defaultValue: any = values.defaultValue;
      if (values.valueType === VariableValueTypes.Date && !values.expression) {
        defaultValue = values.defaultValue.map(d =>
          (d as Moment).format(values.dateFormat),
        );
      }

      try {
        if (defaultValue !== void 0 && defaultValue !== null) {
          defaultValue = JSON.stringify(defaultValue);
        }
      } catch (error) {
        errorHandle(error);
        throw error;
      }

      if (formType === CommonFormTypes.Add) {
        dispatch(
          addVariable({
            variable: { ...values, orgId, defaultValue },
            resolve: () => {
              hideForm();
            },
          }),
        );
      } else {
        dispatch(
          editVariable({
            variable: { ...editingVariable!, ...values, defaultValue },
            resolve: () => {
              hideForm();
              message.success(tg('operation.updateSuccess'));
            },
          }),
        );
      }
    },
    [dispatch, formType, orgId, editingVariable, hideForm, tg],
  );

  const saveRelations = useCallback(
    async (changedRowPermissions: RowPermission[]) => {
      let changedRowPermissionsRaw = changedRowPermissions.map(cr => {
        const dateFormat =
          variables.find(v => v.id === cr.variableId)?.dateFormat ||
          TIME_FORMATTER;
        return {
          ...cr,
          value:
            cr.value &&
            (editingVariable?.valueType === VariableValueTypes.Date
              ? cr.value.map(m => (m as Moment).format(dateFormat))
              : cr.value),
        };
      });

      if (rowPermissions) {
        const { created, updated, deleted } = getDiffParams(
          [...rowPermissions],
          changedRowPermissionsRaw,
          (oe, ce) =>
            oe.subjectId === ce.subjectId && oe.variableId === ce.variableId,
          (oe, ce) =>
            oe.useDefaultValue !== ce.useDefaultValue || oe.value !== ce.value,
        );

        if (created.length > 0 || updated.length > 0 || deleted.length > 0) {
          setUpdateRowPermissionLoading(true);
          await request2<null>({
            url: '/variables/rel',
            method: 'PUT',
            data: {
              relToCreate: created.map(r => ({
                ...r,
                value: JSON.stringify(r.value),
              })),
              relToUpdate: updated.map(r => ({
                ...r,
                value: JSON.stringify(r.value),
              })),
              relToDelete: deleted.map(({ id }) => id),
            },
          });
          message.success(tg('operation.updateSuccess'));
          setUpdateRowPermissionLoading(false);
          setSubjectFormVisible(false);
        } else {
          setSubjectFormVisible(false);
        }
      }
    },
    [rowPermissions, editingVariable, tg, variables],
  );

  const columns: TableColumnProps<VariableViewModel>[] = useMemo(
    () => [
      { dataIndex: 'name', title: t('name') },
      { dataIndex: 'label', title: t('label') },
      {
        dataIndex: 'type',
        title: t('type'),
        render: (_, record) => (
          <Tag
            color={record.type === VariableTypes.Permission ? WARNING : INFO}
          >
            {t(`variableType.${record.type.toLowerCase()}`)}
          </Tag>
        ),
      },
      {
        dataIndex: 'valueType',
        title: t('valueType'),
        render: (_, record) =>
          t(`variableValueType.${record.valueType.toLowerCase()}`),
      },
      {
        title: tg('title.action'),
        align: 'center',
        width: 140,
        render: (_, record) => (
          <Actions>
            {record.type === VariableTypes.Permission && (
              <Tooltip title={t('related')}>
                <Button
                  type="link"
                  icon={<TeamOutlined />}
                  onClick={showSubjectForm(record.id)}
                />
              </Tooltip>
            )}
            <Tooltip title={tg('button.edit')}>
              <Button
                type="link"
                icon={<EditOutlined />}
                onClick={showEditForm(record.id)}
              />
            </Tooltip>
            <Tooltip title={tg('button.delete')}>
              <Popconfirm
                title={tg('operation.deleteConfirm')}
                onConfirm={del(record.id)}
              >
                <Button type="link" icon={<DeleteOutlined />} />
              </Popconfirm>
            </Tooltip>
          </Actions>
        ),
      },
    ],
    [del, showEditForm, showSubjectForm, t, tg],
  );

  const pagination = useMemo(
    () => ({ pageSize: 20, pageSizeOptions: ['20', '50', '100'] }),
    [],
  );

  return (
    <Wrapper>
      <Card>
        <TableHeader>
          <h3>{t('title')}</h3>
          <Toolbar>
            {selectedRowKeys.length > 0 && (
              <Popconfirm
                title={t('deleteAllConfirm')}
                onConfirm={delSelectedVariables}
              >
                <Button
                  icon={<DeleteOutlined />}
                  loading={deleteVariablesLoading}
                >
                  {t('deleteAll')}
                </Button>
              </Popconfirm>
            )}
            <Button
              icon={<PlusOutlined />}
              type="primary"
              onClick={showAddForm}
            >
              {tg('button.create')}
            </Button>
          </Toolbar>
        </TableHeader>
        <Table
          rowKey="id"
          size="small"
          dataSource={variables}
          columns={columns}
          loading={listLoading}
          rowSelection={{ selectedRowKeys, onChange: setSelectedRowKeys }}
          pagination={pagination}
        />
        <VariableForm
          scope={VariableScopes.Public}
          orgId={orgId}
          editingVariable={editingVariable}
          visible={formVisible}
          title={t('public')}
          type={formType}
          confirmLoading={saveLoading}
          onSave={save}
          onCancel={hideForm}
          afterClose={afterFormClose}
          keyboard={false}
          maskClosable={false}
        />
        <SubjectForm
          scope={VariableScopes.Public}
          editingVariable={editingVariable}
          loading={rowPermissionLoading}
          rowPermissions={rowPermissions}
          visible={subjectFormVisible}
          confirmLoading={updateRowPermissionLoading}
          onSave={saveRelations}
          onCancel={hideSubjectForm}
          afterClose={afterFormClose}
          keyboard={false}
          maskClosable={false}
        />
      </Card>
    </Wrapper>
  );
}

const Wrapper = styled.div`
  flex: 1;
  padding: ${SPACE_LG};

  .ant-card {
    background-color: ${p => p.theme.componentBackground};
    border-radius: ${BORDER_RADIUS};
    box-shadow: ${p => p.theme.shadow1};

    .ant-card-body {
      padding: 0 ${SPACE_LG};
    }
  }
`;

const TableHeader = styled.div`
  display: flex;
  align-items: center;
  padding: ${SPACE_MD} 0;

  h3 {
    flex: 1;
    font-size: ${FONT_SIZE_TITLE};
    font-weight: ${FONT_WEIGHT_MEDIUM};
    line-height: ${LINE_HEIGHT_TITLE};
  }
`;

const Toolbar = styled(Space)`
  flex-shrink: 0;
`;

const Actions = styled(Space)`
  display: flex;
  justify-content: flex-end;
`;
