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

import { Checkbox, Table, TableColumnProps } from 'antd';
import { LoadingMask } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import produce from 'immer';
import { Key, memo, useCallback, useMemo } from 'react';
import styled from 'styled-components/macro';
import { SPACE_TIMES } from 'styles/StyleConstants';
import { DefaultValue } from '../DefaultValue';
import { RowPermissionSubject, Variable } from '../slice/types';

interface SubjectFormProps {
  type: 'role' | 'member';
  visible: boolean;
  editingVariable: undefined | Variable;
  loading: boolean;
  listLoading: boolean;
  selectedRowKeys: Key[];
  rowPermissionSubjects: undefined | RowPermissionSubject[];
  onSelectedRowKeyChange: (rowKeys: Key[]) => void;
  onRowPermissionSubjectChange: (
    rowPermissionSubjects: undefined | RowPermissionSubject[],
  ) => void;
}

export const RowPermissionTable = memo(
  ({
    type,
    visible,
    editingVariable,
    loading,
    listLoading,
    selectedRowKeys,
    rowPermissionSubjects,
    onSelectedRowKeyChange,
    onRowPermissionSubjectChange,
  }: SubjectFormProps) => {
    const t = useI18NPrefix('variable');

    const checkUseDefaultValue = useCallback(
      id => e => {
        onRowPermissionSubjectChange(
          produce(rowPermissionSubjects, draft => {
            const permission = draft?.find(p => p.id === id)!;
            permission.useDefaultValue = e.target.checked;
          }),
        );
      },
      [rowPermissionSubjects, onRowPermissionSubjectChange],
    );

    const valueChange = useCallback(
      id => value => {
        onRowPermissionSubjectChange(
          produce(rowPermissionSubjects, draft => {
            const permission = draft?.find(p => p.id === id)!;
            permission.value = value;
          }),
        );
      },
      [rowPermissionSubjects, onRowPermissionSubjectChange],
    );

    const columns: TableColumnProps<RowPermissionSubject>[] = useMemo(
      () => [
        { dataIndex: 'name', title: t('name') },
        {
          title: t('useDefaultValue'),
          width: SPACE_TIMES(32),
          render: (_, record) => {
            return (
              <Checkbox
                checked={record.useDefaultValue}
                disabled={!selectedRowKeys.includes(record.id)}
                onChange={checkUseDefaultValue(record.id)}
              />
            );
          },
        },
        {
          title: t('value'),
          width: SPACE_TIMES(72),
          render: (_, record) =>
            editingVariable && (
              <DefaultValue
                dateFormat={editingVariable.dateFormat}
                type={editingVariable.valueType}
                expression={false}
                hasDateFormat={false}
                disabled={
                  !selectedRowKeys.includes(record.id) || record.useDefaultValue
                }
                value={record.value}
                onChange={valueChange(record.id)}
              />
            ),
        },
      ],
      [selectedRowKeys, editingVariable, checkUseDefaultValue, valueChange, t],
    );

    const rowClassName = useCallback(
      (record: RowPermissionSubject) => {
        return selectedRowKeys.includes(record.id) ? '' : 'disabled-row';
      },
      [selectedRowKeys],
    );

    return (
      <LoadingMask loading={loading}>
        <TableWrapper visible={visible}>
          <Table
            rowKey="id"
            size="small"
            columns={columns}
            loading={listLoading}
            dataSource={rowPermissionSubjects}
            rowClassName={rowClassName}
            rowSelection={{
              selectedRowKeys,
              onChange: onSelectedRowKeyChange,
            }}
            bordered
          />
        </TableWrapper>
      </LoadingMask>
    );
  },
);

const TableWrapper = styled.div<{ visible: boolean }>`
  display: ${p => (p.visible ? 'block' : 'none')};

  .disabled-row {
    color: ${p => p.theme.textColorDisabled};
    cursor: not-allowed;
    background-color: ${p => p.theme.bodyBackground};
  }
`;
