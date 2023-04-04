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

import { TreeDataNode } from 'antd';
import { DataViewFieldType } from 'app/constants';
import { APP_CURRENT_VERSION } from 'app/migration/constants';
import { FONT_WEIGHT_MEDIUM, SPACE_UNIT } from 'styles/StyleConstants';
import { Nullable } from 'types';
import { isEmptyArray, isEqualObject } from 'utils/object';
import { getDiffParams, getTextWidth } from 'utils/utils';
import {
  ColumnCategories,
  DEFAULT_PREVIEW_SIZE,
  UNPERSISTED_ID_PREFIX,
  ViewViewModelStages,
} from './constants';
import {
  Column,
  ColumnRole,
  ColumnsModel,
  ColumnsProps,
  DatabaseSchema,
  HierarchyModel,
  Model,
  QueryResult,
  StructViewQueryProps,
  ViewType,
  ViewViewModel,
} from './slice/types';

export function generateEditingView(
  attrs?: Partial<ViewViewModel>,
): ViewViewModel {
  return {
    id: '',
    name: '',
    parentId: null,
    index: null,
    script: '',
    config: {},
    model: {
      version: APP_CURRENT_VERSION,
    },
    originVariables: [],
    variables: [],
    originColumnPermissions: [],
    columnPermissions: [],
    size: DEFAULT_PREVIEW_SIZE,
    touched: false,
    stage: ViewViewModelStages.Fresh,
    previewResults: [],
    error: '',
    fragment: '',
    ...attrs,
  };
}

export function generateNewEditingViewName(editingViews: ViewViewModel[]) {
  let name = '';
  if (editingViews) {
    const prefix = 'Untitled';
    const spliter = '-';
    let index = 0;
    const unpersistedNewViews = editingViews.filter(v =>
      v.id.includes(UNPERSISTED_ID_PREFIX),
    );

    if (unpersistedNewViews.length > 0) {
      index = Math.max(
        ...unpersistedNewViews.map(v => {
          const arr = v.name.split(spliter);
          return Number(arr[arr.length - 1]);
        }),
      );
    }
    name = `${prefix}${spliter}${index + 1}`;
  }
  return name;
}

export function isNewView(id: string | undefined): boolean {
  return id ? id.includes(UNPERSISTED_ID_PREFIX) : true;
}

export function transformQueryResultToModelAndDataSource(
  data: QueryResult,
  lastModel: HierarchyModel,
  viewType?: ViewType,
): {
  model: HierarchyModel;
  dataSource: object[];
} {
  const { rows = [], columns = [], reqColumns } = data || {};
  const newColumns = columns.reduce((obj, { name, type, primaryKey }) => {
    const hierarchyColumn = getHierarchyColumn(
      name,
      lastModel?.hierarchy || {},
    );

    let _name: any = [];
    if (viewType === 'STRUCT') {
      _name = reqColumns?.find(column => column.alias === name[0])?.column;
    } else {
      _name = name;
    }

    return {
      ...obj,
      [name]: {
        name: _name,
        type: hierarchyColumn?.type || type,
        primaryKey,
        category: hierarchyColumn?.category || ColumnCategories.UnCategorized, // FIXME: model 重构时一起改
      },
    };
  }, {});
  const dataSource = rows.map(arr =>
    arr.reduce((obj, val, index) => {
      const key = columns[index].name;
      return {
        ...obj,
        [key]: val,
      };
    }, {}),
  );
  return {
    model: { ...lastModel, columns: newColumns },
    dataSource,
  };
}

export function getHierarchyColumn(
  columnName: string,
  hierarchyModel: Model,
): Nullable<Column> {
  return Object.entries(hierarchyModel)
    .flatMap(([name, value]) => {
      if (!isEmptyArray(value.children)) {
        return value.children;
      }
      return value;
    })
    ?.find(col => col?.name === columnName);
}

export function getColumnWidthMap(
  model: { [key: string]: Omit<ColumnsProps, 'name'> },
  dataSource: object[],
) {
  const HEADER_PADDING = SPACE_UNIT * (2 + 1);
  const CELL_PADDING = SPACE_UNIT * (2 + 2);
  const ICON_WIDTH = 24;
  const ICON_MARGIN = SPACE_UNIT;

  return Object.keys(model).reduce((map, name) => {
    if (!map[name]) {
      // header width
      map[name] =
        getTextWidth(name, `${FONT_WEIGHT_MEDIUM}`) +
        HEADER_PADDING +
        ICON_WIDTH * 2 +
        ICON_MARGIN;
    }
    if (dataSource.length > 0) {
      map[name] = dataSource.reduce((width, o) => {
        // column width
        return Math.min(
          // MAX_RESULT_TABLE_COLUMN_WIDTH,
          Math.max(
            width,
            map[name],
            o[name] !== null && o[name] !== undefined
              ? getTextWidth(`${o[name]}`) + CELL_PADDING
              : 0,
          ),
        );
      }, 0);
    }
    return map;
  }, {});
}

export function comparePermissionChange<
  T extends { subjectId: string; variableId?: string; viewId?: string },
>(
  origin: T[],
  changed: T[],
  compareFunc: (originElement: T, changedElement: T) => boolean,
) {
  return (
    changed.length === origin.length &&
    changed.every(cp =>
      origin.find(
        op =>
          cp.subjectId === op.subjectId &&
          cp.variableId === op.variableId &&
          cp.viewId === op.viewId &&
          compareFunc(op, cp),
      ),
    )
  );
}

export function getSaveParamsFromViewModel(
  orgId: string,
  editingView: ViewViewModel,
  isUpdate?: boolean,
  database?: DatabaseSchema[],
  isSaveAs?: Boolean,
) {
  const {
    name,
    sourceId,
    parentId,
    script,
    model,
    config,
    originVariables,
    variables,
    originColumnPermissions,
    columnPermissions,
    index,
    type,
  } = editingView;

  if (isUpdate) {
    const { created, updated, deleted } = getDiffParams(
      [...originVariables],
      [...variables],
      (oe, ce) => oe.id === ce.id,
      (oe, ce) =>
        Object.entries(ce).some(([key, value]) => {
          if (key === 'relVariableSubjects') {
            return !comparePermissionChange(
              oe[key],
              value,
              (subOe, subCe) =>
                subOe.useDefaultValue === subCe.useDefaultValue &&
                subOe.value === subCe.value,
            );
          } else {
            return value !== oe[key];
          }
        }),
    );
    return {
      orgId,
      name,
      sourceId,
      parentId,
      isFolder: false,
      index,
      type,
      script:
        type === 'STRUCT'
          ? handleObjectScriptToString(
              script as StructViewQueryProps,
              database!,
            )
          : script,
      config: JSON.stringify(config),
      model: JSON.stringify(model),
      variablesToCreate: created,
      // 关联关系未改变传空值，服务端将不做处理
      variablesToUpdate: updated.map(uv => {
        const originVariable = originVariables.find(o => o.id === uv.id);
        return originVariable
          ? comparePermissionChange(
              originVariable['relVariableSubjects'],
              uv.relVariableSubjects,
              (oe, ce) =>
                oe.useDefaultValue === ce.useDefaultValue &&
                oe.value === ce.value,
            )
            ? { ...uv, relVariableSubjects: null }
            : uv
          : uv;
      }),
      variableToDelete: deleted.map(({ id }) => id),
      columnPermission: comparePermissionChange(
        originColumnPermissions,
        columnPermissions,
        (oe, ce) =>
          Array.from(oe.columnPermission).sort().join(',') ===
          Array.from(ce.columnPermission).sort().join(','),
      )
        ? null
        : columnPermissions.map(cp => ({
            ...cp,
            columnPermission: JSON.stringify(cp.columnPermission),
          })),
    };
  } else {
    return {
      orgId,
      name,
      sourceId,
      parentId,
      isFolder: false,
      index,
      type,
      script:
        type === 'STRUCT' && !isSaveAs
          ? handleObjectScriptToString(
              script as StructViewQueryProps,
              database!,
            )
          : script,
      config: JSON.stringify(config),
      model: JSON.stringify(model),
      variablesToCreate: variables,
      columnPermission: columnPermissions.map(cp => ({
        ...cp,
        columnPermission: JSON.stringify(cp.columnPermission),
      })),
    };
  }
}

export function transformModelToViewModel(
  data,
  database: DatabaseSchema[] | null,
  tempViewModel?: object,
): ViewViewModel {
  const {
    config,
    model,
    variables,
    relVariableSubjects,
    relSubjectColumns,
    ...rest
  } = data;

  return {
    ...tempViewModel,
    ...rest,
    config: JSON.parse(config),
    model: JSON.parse(model),
    originVariables: variables.map(v => ({ ...v, relVariableSubjects })),
    variables: variables.map(v => ({ ...v, relVariableSubjects })),
    originColumnPermissions: relSubjectColumns.map(r => ({
      ...r,
      columnPermission: JSON.parse(r.columnPermission),
    })),
    columnPermissions: relSubjectColumns.map(r => ({
      ...r,
      columnPermission: JSON.parse(r.columnPermission),
    })),
  };
}

export const dataModelColumnSorter = (prev: Column, next: Column): number => {
  const columnTypePriority = {
    [DataViewFieldType.DATE]: 1,
    [DataViewFieldType.STRING]: 1,
    [DataViewFieldType.NUMERIC]: 2,
  };
  const hierarchyPriority = {
    [ColumnRole.Hierarchy]: 10,
    [ColumnRole.Role]: 100,
  };
  const calcPriority = (column: Column) => {
    return (
      columnTypePriority[column?.type || DataViewFieldType.STRING] *
      hierarchyPriority[column?.role || ColumnRole.Role]
    );
  };
  return (
    calcPriority(prev) - calcPriority(next) ||
    (prev?.name || '').localeCompare(next?.name || '')
  );
};

export const diffMergeHierarchyModel = (
  model: HierarchyModel,
  viewType: ViewType,
) => {
  const hierarchy = model?.hierarchy || {};
  const columns = model?.columns || {};

  const allHierarchyColumnNames = Object.keys(hierarchy).flatMap(name => {
    if (!isEmptyArray(hierarchy[name].children)) {
      return hierarchy[name].children!.map(child => child.name);
    }
    return name;
  });
  const additionalObjs = Object.keys(columns).reduce((acc, name) => {
    if (allHierarchyColumnNames.includes(name)) {
      return acc;
    }
    acc[name] = columns[name];
    return acc;
  }, {});
  let newHierarchy = Object.keys(hierarchy).reduce((acc, name) => {
    if (name in columns) {
      acc[name] = hierarchy[name];
    } else if (!isEmptyArray(hierarchy[name]?.children)) {
      const hierarchyColumn = hierarchy[name];
      hierarchyColumn.children = hierarchyColumn.children?.filter(child =>
        Object.keys(columns).includes(child.name),
      );
      if (hierarchyColumn.children?.length) {
        acc[name] = hierarchyColumn;
      }
    }
    return acc;
  }, additionalObjs);
  newHierarchy = addPathToHierarchyStructureAndChangeName(
    newHierarchy,
    viewType,
  );
  model.hierarchy = newHierarchy;
  return model;
};

export function addPathToHierarchyStructureAndChangeName(
  hierarchy: ColumnsModel,
  viewType: ViewType,
): Model {
  if (!hierarchy) {
    return hierarchy;
  }
  const _hierarchy = Object.keys(hierarchy).reduce((acc, name) => {
    acc[name] = hierarchy[name];
    if (acc[name].children) {
      acc[name].children.forEach((children, i) => {
        if (!children['path']) {
          acc[name].children![i]['path'] = Array.isArray(children.name)
            ? children.name
            : viewType === 'STRUCT'
            ? children.name && JSON.parse(children.name)
            : [children.name];

          acc[name].children![i]['name'] =
            viewType === 'STRUCT'
              ? children.name && JSON.parse(children.name).join('.')
              : children.name;
        }
      });
    } else if (!acc[name]['path']) {
      acc[name]['path'] = Array.isArray(acc[name]['name'])
        ? acc[name]['name']
        : viewType === 'STRUCT'
        ? acc[name]['name'] && JSON.parse(acc[name]['name'])
        : [name];

      acc[name]['name'] = name;
    }
    return acc;
  }, {});

  return _hierarchy;
}

export function buildAntdTreeNodeModel<T extends TreeDataNode & { value: any }>(
  ancestors: string[] = [],
  nodeName: string,
  children?: T[],
  isLeaf?: boolean,
): T {
  const TREE_HIERARCHY_SEPERATOR = String.fromCharCode(0);
  const fullNames = ancestors.concat(nodeName);
  return {
    key: fullNames.join(TREE_HIERARCHY_SEPERATOR),
    title: nodeName,
    value: fullNames,
    children,
    isLeaf,
  } as any;
}

export function buildRequestColumns(tableJSON: StructViewQueryProps) {
  const columns: any = [];
  tableJSON.columns.forEach((v, i) => {
    const table = tableJSON.table || [];
    columns.push({
      alias: [...table, v].join('.'),
      column: [...table, v],
    });
  });
  tableJSON.joins.forEach(join => {
    const table = join.table || [];
    join.columns?.forEach(column => {
      columns.push({
        alias: [...table, column].join('.'),
        column: [...table, column],
      });
    });
  });
  return columns;
}

export function findAllColumnsOrIsCheckAll(
  tableJSON: { table?: string[]; columns?: string[] },
  database: DatabaseSchema[],
): { columns?: string[]; isCheckAll: boolean } {
  const { table = [], columns } = tableJSON;

  if (table.length === 1) {
    const foundColumns = database?.[0]?.tables
      ?.find(v => v.tableName === table[0])
      ?.['columns'].map(v => v.name[0]);

    return {
      columns: foundColumns,
      isCheckAll: isEqualObject(foundColumns, columns),
    };
  }

  const foundColumns = database
    ?.find(v => v.dbName === table[0])
    ?.tables?.find(v => v.tableName === table[1])
    ?.['columns'].map(v => v.name);

  return {
    columns: foundColumns,
    isCheckAll: isEqualObject(foundColumns, columns),
  };
}

export function handleStringScriptToObject(
  script: string,
  database: DatabaseSchema[] | null,
) {
  if (!database) {
    return script;
  }
  try {
    const scriptJSON = JSON.parse(script);
    const { columns } = findAllColumnsOrIsCheckAll(scriptJSON, database);

    return {
      ...scriptJSON,
      columns:
        JSON.parse(scriptJSON.columns) === 'all'
          ? columns
          : JSON.parse(scriptJSON.columns),
      joins: scriptJSON.joins.map(join => {
        const { columns } = findAllColumnsOrIsCheckAll(join, database);
        return {
          ...join,
          columns:
            JSON.parse(join.columns) === 'all'
              ? columns
              : JSON.parse(join.columns),
        };
      }),
    };
  } catch (err) {
    return script;
  }
}

export function handleObjectScriptToString(
  structure: StructViewQueryProps,
  database: DatabaseSchema[],
) {
  try {
    const { isCheckAll } = findAllColumnsOrIsCheckAll(structure!, database);

    const script = JSON.stringify({
      ...structure,
      columns: JSON.stringify(isCheckAll ? 'all' : structure?.columns),
      joins: structure?.joins.map(j => {
        const { isCheckAll } = findAllColumnsOrIsCheckAll(j, database);
        return {
          ...j,
          columns: JSON.stringify(isCheckAll ? 'all' : j?.columns),
        };
      }),
    });

    return script;
  } catch (err) {
    throw err;
  }
}

export const getTableAllColumns = (
  joinTableName: Array<string>,
  currentDatabaseSchemas: DatabaseSchema[],
): string[] | [] => {
  if (!currentDatabaseSchemas) {
    return [];
  }

  const column =
    joinTableName.length === 1
      ? currentDatabaseSchemas[0].tables
          .find(v => v.tableName === joinTableName[0])
          ?.columns.map(v => {
            return v.name[0];
          })
      : currentDatabaseSchemas
          ?.find(v => v.dbName === joinTableName?.[0])
          ?.tables.find(v => v.tableName === joinTableName[1])
          ?.columns.map(v => {
            return v.name[0];
          });
  return column || [];
};
