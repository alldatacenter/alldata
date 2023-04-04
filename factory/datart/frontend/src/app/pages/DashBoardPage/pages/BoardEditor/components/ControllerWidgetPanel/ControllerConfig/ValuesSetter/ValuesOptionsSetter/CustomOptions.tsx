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
import { Button, FormInstance, Space } from 'antd';
import { DragSortEditTable } from 'app/components/DragSortEditTable';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { RelationFilterValue } from 'app/types/ChartConfig';
import React, { memo, useCallback, useEffect, useState } from 'react';
import styled from 'styled-components/macro';
import { ControllerConfig } from '../../../types';

export interface CustomOptionsProps {
  form: FormInstance<{ config: ControllerConfig }> | undefined;
  fieldRowData: RelationFilterValue[];
  getControllerConfig: () => ControllerConfig;
}
export const CustomOptions: React.FC<CustomOptionsProps> = memo(
  ({ fieldRowData, form, getControllerConfig }) => {
    const [rows, setRows] = useState<RelationFilterValue[]>([]);
    const t = useI18NPrefix(`viz.control`);
    const onChangeFilterOptions = useCallback(
      (rows: RelationFilterValue[]) => {
        setRows(rows);
        const config = getControllerConfig();
        const valueOptions = [...rows.slice()];

        form?.setFieldsValue({
          config: {
            ...config,
            valueOptions: valueOptions,
          },
        });
      },
      [form, getControllerConfig],
    );

    useEffect(() => {
      const valueOptions = getControllerConfig()?.valueOptions || [];
      setRows(valueOptions);
    }, [form, getControllerConfig]);

    const handleRowStateUpdate = useCallback(
      (row: RelationFilterValue) => {
        const newRows = [...rows];
        const targetIndex = newRows.findIndex(r => r.index === row.index);

        if (row.childIndex !== undefined) {
          newRows[targetIndex]?.children?.splice(row.childIndex, 1, row);
        } else {
          newRows.splice(targetIndex, 1, row);
        }
        onChangeFilterOptions(newRows);
      },
      [onChangeFilterOptions, rows],
    );

    const handleAdd = useCallback(() => {
      const newKey = rows?.length || 0;
      const newRow: RelationFilterValue = {
        index: newKey,
        key: String(newKey),
        label: String(newKey),
        isSelected: false,
      };
      const currentRows = rows?.concat([newRow]);
      onChangeFilterOptions(currentRows);
    }, [onChangeFilterOptions, rows]);

    const addRowByField = useCallback(() => {
      onChangeFilterOptions(fieldRowData);
    }, [onChangeFilterOptions, fieldRowData]);

    const handleDelete = (key: React.Key) => {
      const currentRows = rows.filter(r => r.key !== key);
      onChangeFilterOptions(currentRows);
    };
    const columns = [
      {
        title: t('value'),
        dataIndex: 'key',
        width: '30%',
        sorter: (rowA, rowB) => {
          return String(rowA.key).localeCompare(rowB.key);
        },
        editable: true,
      },
      {
        title: t('label'),
        dataIndex: 'label',
        width: '40%',
        sorter: (rowA, rowB) => {
          return String(rowA.key).localeCompare(rowB.key);
        },
        editable: true,
      },
      {
        title: t('action'),
        dataIndex: 'action',
        width: '30%',
        render: (_, record: RelationFilterValue) => (
          <Space>
            <a
              href="#!"
              style={{ color: record.isSelected ? 'red' : '' }}
              onClick={() => {
                handleRowStateUpdate({
                  ...record,
                  isSelected: !record.isSelected,
                });
              }}
            >
              {record.isSelected ? t('unsetDefault') : t('setDefault')}
            </a>

            <a href="#!" onClick={() => handleDelete(record.key)}>
              {t('delete')}
            </a>
          </Space>
        ),
      },
    ];

    const columnsWithCell = columns.map(col => {
      if (!col.editable) {
        return col;
      }

      return {
        ...col,
        onCell: (record: RelationFilterValue) => ({
          record,
          editable: col.editable,
          dataIndex: col.dataIndex,
          title: col.title,
          handleSave: handleRowStateUpdate,
        }),
      };
    });
    const moveRow = useCallback(
      (dragIndex, hoverIndex) => {
        const dragRow = rows[dragIndex];
        const newRows = rows.slice();
        newRows.splice(dragIndex, 1);
        newRows.splice(hoverIndex, 0, dragRow);
        setRows([...newRows]);
      },
      [rows],
    );
    return (
      <Wrapper>
        <div>
          <Space>
            <Button onClick={addRowByField}>{t('populate')}</Button>
            <Button onClick={handleAdd} type="primary">
              +
            </Button>
          </Space>
          <DragSortEditTable
            style={{ marginTop: 10 }}
            scroll={{ y: 240 }}
            dataSource={rows}
            size="small"
            bordered
            rowKey={(r: RelationFilterValue) => `${r.key}-${r.label}`}
            columns={columnsWithCell}
            pagination={false}
            onRow={(_, index) =>
              ({
                index,
                moveRow,
              } as any)
            }
          />
        </div>
      </Wrapper>
    );
  },
);
const Wrapper = styled.div`
  display: block;
`;
