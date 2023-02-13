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
  ArrowLeftOutlined,
  DatabaseOutlined,
  TableOutlined,
} from '@ant-design/icons';
import { Button, Checkbox, Divider, Empty, Input, Menu, Popover } from 'antd';
import { MenuListItem, Tree } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { useSearchAndExpand } from 'app/hooks/useSearchAndExpand';
import classnames from 'classnames';
import { DEFAULT_DEBOUNCE_WAIT } from 'globalConstants';
import { darken, getLuminance, lighten } from 'polished';
import { memo, useCallback, useEffect, useMemo, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import styled from 'styled-components';
import {
  FONT_WEIGHT_MEDIUM,
  SPACE_SM,
  SPACE_TIMES,
  SPACE_XS,
} from 'styles/StyleConstants';
import { selectSources } from '../../../../SourcePage/slice/selectors';
import { Source } from '../../../../SourcePage/slice/types';
import { selectAllSourceDatabaseSchemas } from '../../../slice/selectors';
import { getSchemaBySourceId } from '../../../slice/thunks';
import {
  DatabaseSchema,
  JoinTableProps,
  StructViewQueryProps,
} from '../../../slice/types';
import { buildAntdTreeNodeModel, getTableAllColumns } from '../../../utils';

const CheckboxGroup = Checkbox.Group;

interface SelectDataSourceProps {
  type?: 'MAIN' | 'JOINS';
  renderType?: 'READONLY' | 'MANAGE';
  structure?: StructViewQueryProps;
  sourceId?: string;
  joinTable?: JoinTableProps;
  allowManage: boolean;
  onChange?: (data: any, type) => void;
}

const SelectDataSource = memo(
  ({
    type,
    renderType = 'MANAGE',
    structure,
    sourceId,
    joinTable,
    allowManage,
    onChange,
  }: SelectDataSourceProps) => {
    const dispatch = useDispatch();
    const propsSources = useSelector(selectSources);
    const allDatabaseSchemas = useSelector(selectAllSourceDatabaseSchemas);
    const t = useI18NPrefix(`view.structView`);

    const [currentSources, setCurrentSources] = useState<Source | null>(null);
    const [selectedTableSchema, setSelectedTableSchema] = useState<any>(
      structure && renderType === 'READONLY'
        ? { table: structure.table, columns: structure.columns }
        : null,
    );
    const [sources, setSources] = useState<Source[]>(propsSources);
    const [visible, setVisible] = useState<boolean>(false);

    const currentTableAllColumns = useMemo(() => {
      if (type === 'JOINS') {
        return getTableAllColumns(
          joinTable?.table || [],
          allDatabaseSchemas[sourceId!],
        );
      } else {
        return getTableAllColumns(
          structure?.table || [],
          allDatabaseSchemas[sourceId!],
        );
      }
    }, [
      allDatabaseSchemas,
      sourceId,
      structure?.table,
      joinTable?.table,
      type,
    ]);

    const handleCurrentSources = useCallback(
      ({ key }) => {
        const selectSources = sources?.[key];
        setCurrentSources(selectSources);
        dispatch(getSchemaBySourceId(selectSources.id));
      },
      [sources, dispatch],
    );

    const filterSources = useCallback(
      e => {
        const searchValue = e.target.value;
        if (searchValue) {
          setSources(sources.filter(v => v.name.includes(searchValue)));
        } else {
          setSources(propsSources);
        }
      },
      [sources, propsSources],
    );

    const buildTableNode = useCallback((database: DatabaseSchema) => {
      const children =
        database?.tables?.map(table => {
          return buildTableColumnNode([database.dbName], table);
        }) || [];
      return buildAntdTreeNodeModel([], database.dbName, children, false);
    }, []);

    const buildTableColumnNode = (ancestors: string[] = [], table) => {
      return {
        columns: table.columns,
        ...buildAntdTreeNodeModel(ancestors, table.tableName),
      };
    };

    const renderIcon = useCallback(({ value }) => {
      if (Array.isArray(value)) {
        switch (value.length) {
          case 1:
            return <DatabaseOutlined className="list-icon" />;
          case 2:
            return <TableOutlined className="list-icon" />;
        }
      }
    }, []);

    const databaseTreeModel = useMemo(() => {
      if (currentSources?.id) {
        const databaseSchemas = allDatabaseSchemas[currentSources.id];
        if (databaseSchemas?.length === 1) {
          return databaseSchemas[0].tables.map(v =>
            buildTableColumnNode([databaseSchemas[0].dbName], v),
          );
        } else {
          return (databaseSchemas || []).map(v => buildTableNode(v));
        }
      }
      return [];
    }, [buildTableNode, currentSources, allDatabaseSchemas]);

    const { filteredData: tableSchema, debouncedSearch: tableSchemaSearch } =
      useSearchAndExpand(
        databaseTreeModel,
        (keywords, data: any) => (data.title as string).includes(keywords),
        DEFAULT_DEBOUNCE_WAIT,
        true,
      );

    const handleColumnCheck = useCallback(
      list => {
        setSelectedTableSchema({
          ...selectedTableSchema,
          columns: list,
        });
        onChange?.({ columns: list }, type);
      },
      [onChange, selectedTableSchema, type],
    );

    const handleCheckAllColumns = useCallback(
      (checked, allColumn) => {
        const checkedList = checked ? allColumn : [];

        onChange?.({ columns: checkedList }, type);
        setSelectedTableSchema({
          ...selectedTableSchema,
          columns: checkedList,
        });
      },
      [onChange, type, selectedTableSchema],
    );

    const handleTableSelect = useCallback(
      (key, { node }) => {
        if (node.children) {
          return;
        }

        const databaseSchemas = allDatabaseSchemas[currentSources!.id];
        const nodeList = node.value;
        const sheetName = node.value[node.value.length - 1];
        const columns = node.columns.map(v => v.name[0]);
        const tableSchema: any = {
          table: databaseSchemas.length === 1 ? [sheetName] : nodeList,
          columns: columns,
          sourceId: currentSources!.id,
        };

        if (type === 'JOINS') {
          delete tableSchema.sourceId;
        }
        setSelectedTableSchema(tableSchema);
        onChange?.(tableSchema, type);
        setVisible(false);
      },
      [type, onChange, allDatabaseSchemas, currentSources],
    );

    const handleVisibleChange = useCallback(visible => {
      setVisible(visible);
    }, []);

    useEffect(() => {
      setSources(propsSources);
    }, [propsSources]);

    useEffect(() => {
      if (renderType === 'READONLY') {
        const leftContainer = joinTable?.conditions?.[0].left;

        if (leftContainer) {
          if (structure?.table?.every(val => leftContainer?.includes(val))) {
            setSelectedTableSchema({
              table: structure?.['table'],
            });
          } else {
            structure?.joins.forEach(v => {
              if (v.table?.every(val => leftContainer?.includes(val))) {
                setSelectedTableSchema({
                  table: v?.['table'],
                });
              }
            });
          }
        }
      }
    }, [structure, renderType, joinTable?.conditions]);

    useEffect(() => {
      if (type === 'JOINS') {
        setCurrentSources(sources.find(v => v.id === sourceId) || null);
      }
    }, [sourceId, sources, type]);

    useEffect(() => {
      if (type === 'MAIN' && structure?.table.length) {
        setCurrentSources(sources.find(v => v.id === sourceId) || null);
        setSelectedTableSchema({
          table: structure.table,
          columns: structure.columns,
        });
      }

      if (type === 'JOINS' && joinTable?.table) {
        setSelectedTableSchema({
          table: joinTable['table'],
          columns: joinTable['columns'],
        });
      }
    }, [structure, type, joinTable, sources, sourceId]);

    return (
      <>
        <Popover
          trigger={['click']}
          placement="bottomLeft"
          overlayClassName="datart-popup"
          visible={allowManage && visible}
          onVisibleChange={
            renderType === 'MANAGE' ? handleVisibleChange : undefined
          }
          content={
            currentSources ? (
              <PopoverBody>
                <ListHeader>
                  <ArrowLeftOutlined
                    onClick={() =>
                      type === 'JOINS' ? null : setCurrentSources(null)
                    }
                  />
                  <h4>{currentSources.name}</h4>
                </ListHeader>
                <SearchBox>
                  <Input
                    placeholder={t('searchTable')}
                    onChange={tableSchemaSearch}
                  />
                </SearchBox>
                <DatabaseTableList>
                  <Tree
                    autoExpandParent
                    defaultExpandParent
                    className="medium without-indent"
                    loading={!tableSchema}
                    icon={renderIcon}
                    treeData={tableSchema}
                    onSelect={handleTableSelect}
                  />
                </DatabaseTableList>
              </PopoverBody>
            ) : (
              <PopoverBody>
                <SearchBox>
                  <Input
                    placeholder={t('searchSource')}
                    onChange={filterSources}
                  />
                </SearchBox>
                <SourceList>
                  <Menu
                    prefixCls="ant-dropdown-menu"
                    onClick={handleCurrentSources}
                  >
                    {sources && sources.length > 0 ? (
                      sources.map((v, i) => {
                        return (
                          <MenuListItem
                            key={i}
                            prefix={<DatabaseOutlined className="list-icon" />}
                          >
                            {v.name}
                          </MenuListItem>
                        );
                      })
                    ) : (
                      <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />
                    )}
                  </Menu>
                </SourceList>
              </PopoverBody>
            )
          }
        >
          <TableButton
            type="primary"
            className={classnames({
              'with-columns': selectedTableSchema && renderType === 'MANAGE',
            })}
          >
            {selectedTableSchema
              ? selectedTableSchema.table[selectedTableSchema.table.length - 1]
              : t('selectTable')}
          </TableButton>
        </Popover>

        {selectedTableSchema && renderType === 'MANAGE' && (
          <Popover
            trigger={['click']}
            placement="bottomLeft"
            content={
              <PopoverBody>
                <Checkbox
                  indeterminate={
                    !!selectedTableSchema?.columns.length &&
                    selectedTableSchema?.columns.length <
                      currentTableAllColumns.length
                  }
                  onChange={e => {
                    allowManage &&
                      handleCheckAllColumns(
                        e.target.checked,
                        currentTableAllColumns,
                      );
                  }}
                  checked={
                    selectedTableSchema?.columns.length ===
                    currentTableAllColumns.length
                  }
                >
                  {t('all')}
                </Checkbox>
                <SmallDivider />
                <ColumnList
                  value={selectedTableSchema?.columns}
                  onChange={allowManage ? handleColumnCheck : undefined}
                  options={currentTableAllColumns}
                />
              </PopoverBody>
            }
          >
            <ColumnButton type="primary" icon={<TableOutlined />} />
          </Popover>
        )}
      </>
    );
  },
);

export default SelectDataSource;

const PopoverBody = styled.div`
  display: flex;
  flex-direction: column;
  max-height: 400px;

  .list-icon {
    color: ${p => p.theme.textColorDisabled};
  }
`;

const ListHeader = styled.div`
  display: flex;
  flex-shrink: 0;
  align-items: center;
  padding: ${SPACE_XS} ${SPACE_SM};
  border-bottom: 1px solid ${p => p.theme.borderColorSplit};

  h4 {
    padding: 0 ${SPACE_SM};
    font-weight: ${FONT_WEIGHT_MEDIUM};
    color: ${p => p.theme.textColorSnd};
  }
`;

const SearchBox = styled.div`
  flex-shrink: 0;
  padding: ${SPACE_XS} ${SPACE_SM};
`;

const SourceList = styled.div`
  flex: 1;
  overflow-y: auto;
`;

const DatabaseTableList = styled.div`
  flex: 1;
  padding: 0 0 ${SPACE_XS};
  overflow-y: auto;
`;

const SmallDivider = styled(Divider)`
  margin: ${SPACE_XS} 0;
`;

const ColumnList = styled(CheckboxGroup)`
  display: flex;
  flex-direction: column;

  .ant-checkbox-group-item {
    padding: ${SPACE_TIMES(0.5)} 0;
  }
`;

const TableButton = styled(Button)`
  &.with-columns {
    border-right: 1px solid
      ${p =>
        getLuminance(p.theme.primary) > 0.5
          ? darken(0.1, p.theme.primary)
          : lighten(0.1, p.theme.primary)} !important;
    border-radius: 2px 0 0 2px !important;
  }
`;

const ColumnButton = styled(Button)`
  border-left: 0 !important;
  border-radius: 0 2px 2px 0 !important;
`;
