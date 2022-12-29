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

import { Button, Space } from 'antd';
import DragSortEditTable from 'app/components/DragSortEditTable';
import useI18NPrefix, { I18NComponentProps } from 'app/hooks/useI18NPrefix';
import ChartFilterCondition, {
  ConditionBuilder,
} from 'app/models/ChartFilterCondition';
import { RelationFilterValue } from 'app/types/ChartConfig';
import ChartDataView from 'app/types/ChartDataView';
import { getDistinctFields } from 'app/utils/fetch';
import { FilterSqlOperator } from 'globalConstants';
import { FC, memo, useCallback, useEffect, useState } from 'react';

const CategoryConditionEditableTable: FC<
  {
    condition?: ChartFilterCondition;
    dataView?: ChartDataView;
    onConditionChange: (condition: ChartFilterCondition) => void;
    fetchDataByField?: (fieldId) => Promise<string[]>;
  } & I18NComponentProps
> = memo(
  ({
    i18nPrefix,
    condition,
    dataView,
    onConditionChange,
    fetchDataByField,
  }) => {
    const t = useI18NPrefix(i18nPrefix);
    const [rows, setRows] = useState<RelationFilterValue[]>([]);

    useEffect(() => {
      if (Array.isArray(condition?.value)) {
        setRows(condition?.value as RelationFilterValue[]);
      } else {
        setRows([]);
      }
    }, [condition?.value]);

    const columns = [
      {
        title: t('tableHeaderKey'),
        dataIndex: 'key',
        width: '40%',
        sorter: (rowA, rowB) => {
          return String(rowA.key).localeCompare(rowB.key);
        },
        editable: true,
      },
      {
        title: t('tableHeaderLabel'),
        dataIndex: 'label',
        width: '40%',
        sorter: (rowA, rowB) => {
          return String(rowA.key).localeCompare(rowB.key);
        },
        editable: true,
      },
      {
        title: t('tableHeaderAction'),
        dataIndex: 'action',
        width: 80,
        render: (_, record: RelationFilterValue) => (
          <Space>
            {!record.isSelected && (
              <a
                href="#!"
                onClick={() =>
                  handleRowStateUpdate({ ...record, isSelected: true })
                }
              >
                {t('setDefault')}
              </a>
            )}
            {record.isSelected && (
              <a
                href="#!"
                onClick={() =>
                  handleRowStateUpdate({ ...record, isSelected: false })
                }
              >
                {t('setUnDefault')}
              </a>
            )}
            <a href="#!" onClick={() => handleDelete(record.key)}>
              {t('deleteRow')}
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

    const handleFilterConditionChange = currentVales => {
      setRows([...currentVales]);
      const filter = new ConditionBuilder(condition)
        .setOperator(FilterSqlOperator.In)
        .setValue(currentVales)
        .asCustomize();
      onConditionChange(filter);
    };

    const handleDelete = (key: React.Key) => {
      const currentRows = rows.filter(r => r.key !== key);
      handleFilterConditionChange(currentRows);
    };

    const handleAdd = () => {
      const newKey = rows?.length + 1;
      const newRow: RelationFilterValue = {
        key: String(newKey),
        label: String(newKey),
        isSelected: true,
      };
      const currentRows = rows.concat([newRow]);
      handleFilterConditionChange(currentRows);
    };

    const handleRowStateUpdate = (row: RelationFilterValue) => {
      const newRows = [...rows];
      const targetIndex = newRows.findIndex(r => r.key === row.key);
      newRows.splice(targetIndex, 1, row);
      handleFilterConditionChange(newRows);
    };

    const handleFetchDataFromField = field => async () => {
      if (fetchDataByField) {
        const dataset = await fetchNewDataset(dataView?.id!, field, dataView);
        const newRows = convertToList(dataset?.rows, []);
        setRows(newRows);
        handleFilterConditionChange(newRows);
      }
    };

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

    const fetchNewDataset = async (viewId, colName: string, dataView) => {
      const fieldDataset = await getDistinctFields(
        viewId,
        [colName],
        dataView,
        undefined,
      );
      return fieldDataset;
    };

    const convertToList = (collection, selectedKeys) => {
      const items: string[] = (collection || []).flatMap(c => c);
      const uniqueKeys = Array.from(new Set(items));
      return uniqueKeys.map(item => ({
        key: item,
        label: item,
        isSelected: selectedKeys.includes(item),
      }));
    };

    return (
      <div>
        <Space>
          <Button onClick={handleAdd} type="primary">
            {t('addRow')}
          </Button>
          <Button onClick={handleFetchDataFromField(condition?.name)}>
            {t('AddFromFields')}
          </Button>
        </Space>
        <DragSortEditTable
          style={{ marginTop: 10 }}
          dataSource={rows}
          size="small"
          bordered
          rowKey={(r: RelationFilterValue) => `${r.key}-${r.label}`}
          columns={columnsWithCell}
          onRow={(_, index) =>
            ({
              index,
              moveRow,
            } as any)
          }
        />
      </div>
    );
  },
);

export default CategoryConditionEditableTable;
