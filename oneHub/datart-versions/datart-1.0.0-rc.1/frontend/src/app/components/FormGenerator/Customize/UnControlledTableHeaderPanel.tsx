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
  ArrowDownOutlined,
  ArrowUpOutlined,
  CheckOutlined,
  DeleteOutlined,
  EditOutlined,
  RedoOutlined,
} from '@ant-design/icons';
import { Button, Col, Input, Row, Space, Table } from 'antd';
import { ChartDataSectionType } from 'app/constants';
import { ChartDataConfig, ChartStyleConfig } from 'app/types/ChartConfig';
import {
  getColumnRenderName,
  getUnusedHeaderRows,
} from 'app/utils/chartHelper';
import { DATARTSEPERATOR } from 'globalConstants';
import { FC, memo, useState } from 'react';
import styled from 'styled-components';
import { CloneValueDeep } from 'utils/object';
import { TableColumnsList } from '../../ChartGraph/BasicTableChart/types';
import { ItemLayoutProps } from '../types';
import { itemLayoutComparer } from '../utils';

const { Search } = Input;

const getFlattenHeaders = (dataConfigs: ChartDataConfig[] = []) => {
  const newDataConfigs = CloneValueDeep(dataConfigs);
  return newDataConfigs
    .filter(
      c =>
        ChartDataSectionType.Aggregate === c.type ||
        ChartDataSectionType.Group === c.type ||
        ChartDataSectionType.Mixed === c.type,
    )
    .flatMap(config => config.rows || []);
};

const UnControlledTableHeaderPanel: FC<ItemLayoutProps<ChartStyleConfig>> =
  memo(
    ({
      ancestors,
      translate: t = title => title,
      data,
      onChange,
      dataConfigs,
    }) => {
      const [selectedRowUids, setSelectedRowUids] = useState<string[]>([]);
      const [myData, setMyData] = useState(() => CloneValueDeep(data));
      const [tableDataSource, setTableDataSource] = useState<
        TableColumnsList[]
      >(() => {
        const originalFlattenHeaderRows = getFlattenHeaders(dataConfigs);
        const currentHeaderRows: TableColumnsList[] = myData?.value || [];
        const unusedHeaderRows = getUnusedHeaderRows(
          originalFlattenHeaderRows || [],
          currentHeaderRows,
        );
        return currentHeaderRows.concat(unusedHeaderRows);
      });

      const mergeRowToGroup = () => {
        if (selectedRowUids.length === 0) {
          return;
        }
        const lineageRowUids = selectedRowUids.map(uid =>
          getAncestorRowUids(undefined, uid, tableDataSource),
        );
        const noDuplicateLineageRows =
          mergeSameLineageAncesterRows(lineageRowUids);
        const ancestorsRows = makeSameLinageRows(noDuplicateLineageRows);
        const newDataSource = groupTreeNode(ancestorsRows, tableDataSource);
        setSelectedRowUids([]);
        handleConfigChange([...newDataSource]);
      };

      const mergeSameLineageAncesterRows = lineageRowUids => {
        const allRowKeys = lineageRowUids.map((lr: string[]) =>
          lr.join(DATARTSEPERATOR),
        );
        return lineageRowUids.reduce((acc, next) => {
          const key = next.join(DATARTSEPERATOR);
          if (
            allRowKeys.some(k => k.includes(key) && k.length !== key.length)
          ) {
            return acc;
          }
          return acc.concat([next]);
        }, []);
      };

      const makeSameLinageRows = rowAncestors => {
        if (rowAncestors && rowAncestors.length === 0) {
          return [];
        }
        const theSortestLength = Math.min(...rowAncestors.map(ra => ra.length));
        let ancestorGeneration = 0;
        for (let i = 0; i < theSortestLength; i++) {
          const ancestor = rowAncestors[0][i];
          if (rowAncestors.every(a => a[i] === ancestor)) {
            ancestorGeneration = i;
          } else {
            break;
          }
        }
        return rowAncestors
          .map(ra => ra.slice(0, ancestorGeneration + 1))
          .reduce((acc, next) => {
            const key = next.join(DATARTSEPERATOR);
            const allRowKeys = acc.map(lr => lr.join(DATARTSEPERATOR));
            if (allRowKeys.includes(key)) {
              return acc;
            }
            return acc.concat([next]);
          }, []);
      };

      const getAncestorRowUids = (parentUid, rowUid, treeRows) => {
        if (treeRows.find(tr => tr.uid === rowUid)) {
          return !!parentUid ? [parentUid, rowUid] : [rowUid];
        }
        return treeRows.reduce((acc, next) => {
          return acc.concat(
            getAncestorRowUids(next.uid, rowUid, next.children || []),
          );
        }, []);
      };

      const groupTreeNode = (rowAncestors, collection) => {
        if (rowAncestors && rowAncestors.length < 1) {
          return collection;
        }

        const rows = collection || [];
        const linageGeneration = rowAncestors[0].length - 1;
        if (linageGeneration === 0) {
          const mergedKeys = rowAncestors.flatMap(ra => ra);
          return mergeBrotherRows(mergedKeys, rows);
        } else {
          const ancestor = rowAncestors[0][0];
          const subRowAncestors = rowAncestors.map(ra => ra.slice(1));
          const childRow = rows.find(c => c.colName === ancestor);
          childRow.children = groupTreeNode(subRowAncestors, childRow.children);
          return rows;
        }
      };

      const mergeBrotherRows = (
        mergeKeys: string[],
        rows: TableColumnsList[],
      ) => {
        const selectedRows = rows.filter(r => mergeKeys.includes(r.uid!));
        const restRows = rows.filter(r => !mergeKeys.includes(r.uid!));
        const insertIndex = rows.findIndex(r => r.uid === mergeKeys[0]);
        const groupRowUid = selectedRows.map(d => d.uid).join(DATARTSEPERATOR);
        const groupRow = {
          uid: groupRowUid,
          colName: groupRowUid,
          label: t('table.header.newName'),
          isGroup: true,
          children: selectedRows,
        };
        if (!restRows.find(rr => rr.uid === groupRowUid)) {
          restRows.splice(insertIndex, 0, groupRow);
        }
        return restRows;
      };

      const handleRowMoveUp = () => {
        selectedRowUids.forEach(rowUid => {
          const brotherRows = findRowBrothers(rowUid, tableDataSource);
          const idx = brotherRows.findIndex(s => s.uid === rowUid);
          if (idx < 1) {
            return;
          }
          const temp = brotherRows[idx - 1];
          brotherRows[idx - 1] = brotherRows[idx];
          brotherRows[idx] = temp;
        });
        handleConfigChange([...tableDataSource]);
      };

      const handleRowMoveDown = () => {
        selectedRowUids.forEach(uid => {
          const brotherRows = findRowBrothers(uid, tableDataSource);
          const idx = brotherRows.findIndex(s => s.uid === uid);
          if (idx >= brotherRows.length - 1) {
            return;
          }
          const temp = brotherRows[idx];
          brotherRows[idx] = brotherRows[idx + 1];
          brotherRows[idx + 1] = temp;
          handleConfigChange([...tableDataSource]);
        });
      };

      const handleRollback = () => {
        const originalFlattenHeaders = getFlattenHeaders(dataConfigs);
        myData.value = [];
        setTableDataSource(originalFlattenHeaders);
        setMyData(myData);
        onChange?.(ancestors, myData);
      };

      const handleTableRowChange = rowUid => style => prop => (_, value) => {
        const brotherRows = findRowBrothers(rowUid, tableDataSource);
        const row = brotherRows.find(r => r.uid === rowUid);

        if (!row) {
          return;
        }
        if (style) {
          row.style = Object.assign({}, row.style, {
            ...row.style,
            [prop]: value,
          });
        } else {
          row[prop] = value;
        }
        handleConfigChange([...tableDataSource]);
      };

      const handleDeleteGroupRow = rowUid => {
        const brotherRows = findRowBrothers(rowUid, tableDataSource);
        const idx = brotherRows.findIndex(s => s.uid === rowUid);
        brotherRows.splice(idx, 1, ...(brotherRows[idx].children || []));
        handleConfigChange([...tableDataSource]);
      };

      const handleConfigChange = (dataSource: TableColumnsList[]) => {
        myData.value = dataSource;
        setTableDataSource(dataSource);
        setMyData(myData);
        onChange?.(ancestors, myData);
      };

      const findRowBrothers = (uid, rows) => {
        let row = rows.find(r => r.uid === uid);
        if (!!row) {
          return rows;
        }
        let subRows = [];
        for (let i = 0; i < rows.length; i++) {
          subRows = findRowBrothers(uid, rows[i].children || []);
          if (!!subRows && subRows.length > 0) {
            break;
          }
        }
        return subRows;
      };

      const tableColumnsSettings = [
        {
          title: t('table.header.columnName'),
          dataIndex: 'colName',
          key: 'colName',
          render: (_, record) => {
            const { label, isGroup, uid } = record;
            return isGroup ? (
              <>
                <DeleteOutlined
                  style={{ marginRight: 10 }}
                  onClick={_ => handleDeleteGroupRow(uid)}
                />
                <EditableLabel
                  label={label}
                  onChange={value =>
                    handleTableRowChange(uid)(undefined)('label')([], value)
                  }
                />
              </>
            ) : (
              getColumnRenderName(record)
            );
          },
        },
      ];

      const rowSelection = {
        selectedRowKeys: selectedRowUids,
        onChange: (selectedRowKeys: any[]) => {
          setSelectedRowUids(selectedRowKeys);
        },
      };

      return (
        <StyledUnControlledTableHeaderPanel direction="vertical">
          <Row gutter={24}>
            <Col span={20}>
              <Space>
                <Button
                  disabled={selectedRowUids.length === 0}
                  type="primary"
                  onClick={mergeRowToGroup}
                >
                  {t('table.header.merge')}
                </Button>
                <Button
                  disabled={selectedRowUids.length === 0}
                  icon={<ArrowUpOutlined />}
                  onClick={handleRowMoveUp}
                >
                  {t('table.header.moveUp')}
                </Button>
                <Button
                  disabled={selectedRowUids.length === 0}
                  icon={<ArrowDownOutlined />}
                  onClick={handleRowMoveDown}
                >
                  {t('table.header.moveDown')}
                </Button>
              </Space>
            </Col>
            <Col span={4}>
              <Row justify="end" align="middle">
                <Button icon={<RedoOutlined />} onClick={handleRollback}>
                  {t('table.header.reset')}
                </Button>
              </Row>
            </Col>
          </Row>
          <Row gutter={24}>
            <Col span={24}>
              <Table
                size="small"
                bordered={true}
                pagination={false}
                {...myData}
                rowKey={record => record.uid!}
                columns={tableColumnsSettings}
                dataSource={tableDataSource}
                rowSelection={rowSelection}
              />
            </Col>
          </Row>
        </StyledUnControlledTableHeaderPanel>
      );
    },
    itemLayoutComparer,
  );

const EditableLabel: FC<{
  label: string;
  editable?: Boolean;
  onChange: (value: string) => void;
}> = memo(({ label, editable = true, onChange }) => {
  const [isEditing, setIsEditing] = useState(false);

  const render = () => {
    if (!editable) {
      return <span>{label}</span>;
    }
    return isEditing ? (
      <Search
        enterButton={<CheckOutlined />}
        placeholder={label}
        size="small"
        onSearch={value => {
          if (!!value) {
            setIsEditing(false);
            onChange(value);
          }
        }}
      />
    ) : (
      <>
        <span>{label}</span>
        <Button
          type="text"
          size="small"
          icon={<EditOutlined />}
          onClick={() => setIsEditing(true)}
        ></Button>
      </>
    );
  };

  return <StyledEditableLabel>{render()}</StyledEditableLabel>;
});

const StyledEditableLabel = styled.div`
  display: inline-block;
`;

const StyledUnControlledTableHeaderPanel = styled(Space)`
  width: 100%;
  margin-top: 10px;
`;

export default UnControlledTableHeaderPanel;
