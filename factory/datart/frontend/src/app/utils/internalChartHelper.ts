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
  InteractionFieldRelation,
  InteractionRelationType,
} from 'app/components/FormGenerator/constants';
import {
  CustomizeRelation,
  InteractionRule,
  JumpToChartRule,
  JumpToDashboardRule,
  JumpToUrlRule,
} from 'app/components/FormGenerator/Customize/Interaction/types';
import {
  AggregateFieldActionType,
  ChartDataSectionType,
  ChartDataViewFieldCategory,
  DataViewFieldType,
} from 'app/constants';
import { ChartDrillOption } from 'app/models/ChartDrillOption';
import { handleDateLevelsName } from 'app/pages/ChartWorkbenchPage/components/ChartOperationPanel/utils';
import { VariableTypes } from 'app/pages/MainPage/pages/VariablePage/constants';
import { Variable } from 'app/pages/MainPage/pages/VariablePage/slice/types';
import {
  ChartConfig,
  ChartDataConfig,
  ChartDataSectionField,
  ChartStyleConfig,
  IntervalScaleNiceTicksResult,
} from 'app/types/ChartConfig';
import {
  ChartCommonConfig,
  ChartStyleConfigDTO,
} from 'app/types/ChartConfigDTO';
import { PendingChartDataRequestFilter } from 'app/types/ChartDataRequest';
import { ChartDataViewMeta } from 'app/types/ChartDataViewMeta';
import { IChartDrillOption } from 'app/types/ChartDrillOption';
import { FilterSqlOperator } from 'globalConstants';
import {
  CloneValueDeep,
  cond,
  curry,
  isEmpty,
  isEmptyArray,
  isInPairArrayRange,
  isNumerical,
  isNumericEqual,
  isPairArray,
  isUndefined,
  pipe,
} from 'utils/object';
import { getDrillableRows, round } from './chartHelper';

export const transferChartConfigs = (
  targetConfig?: ChartConfig,
  sourceConfig?: ChartConfig,
) => {
  return pipe(
    transferChartDataConfig,
    transferChartStyleConfig,
    transferChartSettingConfig,
    transferChartInteractionConfig,
  )(targetConfig, sourceConfig);
};

const transferChartStyleConfig = (
  targetConfig?: ChartConfig,
  sourceConfig?: ChartConfig,
): ChartConfig => {
  if (!targetConfig) {
    return sourceConfig!;
  }
  targetConfig.styles = mergeChartStyleConfigs(
    targetConfig?.styles,
    sourceConfig?.styles,
  );
  return targetConfig;
};

const transferChartInteractionConfig = (
  targetConfig?: ChartConfig,
  sourceConfig?: ChartConfig,
): ChartConfig => {
  if (!targetConfig) {
    return sourceConfig!;
  }
  targetConfig.interactions = mergeChartStyleConfigs(
    targetConfig?.interactions,
    sourceConfig?.interactions,
  );
  return targetConfig;
};

const transferChartSettingConfig = (
  targetConfig?: ChartConfig,
  sourceConfig?: ChartConfig,
): ChartConfig => {
  if (!targetConfig) {
    return sourceConfig!;
  }
  targetConfig.settings = mergeChartStyleConfigs(
    targetConfig?.settings,
    sourceConfig?.settings,
  );
  return targetConfig;
};

export const transferChartDataConfig = (
  targetConfig?: ChartConfig,
  sourceConfig?: ChartConfig,
): ChartConfig => {
  return pipe(
    ...[
      ChartDataSectionType.Group,
      ChartDataSectionType.Aggregate,
      ChartDataSectionType.Color,
      ChartDataSectionType.Info,
      ChartDataSectionType.Mixed,
      ChartDataSectionType.Size,
      ChartDataSectionType.Filter,
    ].map(type => curry(transferDataConfigImpl)(type)),
    ...[ChartDataSectionType.Mixed].map(type =>
      curry(transferMixedToNonMixed)(type),
    ),
    ...[ChartDataSectionType.Mixed].map(type =>
      curry(transferNonMixedToMixed)(type),
    ),
  )(targetConfig, sourceConfig);
};

const transferDataConfigImpl = (
  sectionType: ChartDataSectionType,
  targetConfig?: ChartConfig,
  sourceConfig?: ChartConfig,
): ChartConfig => {
  const targetDataConfigs = targetConfig?.datas || [];
  const sourceDataConfigs = sourceConfig?.datas || [];
  const sourceSectionConfigRows = sourceDataConfigs
    .filter(c => c.type === sectionType)
    .flatMap(config => config.rows || []);
  const targetSectionConfigs = targetDataConfigs?.filter(
    c => c.type === sectionType,
  );

  while (
    Boolean(sourceSectionConfigRows?.length) &&
    Boolean(targetSectionConfigs?.length)
  ) {
    const row = sourceSectionConfigRows.shift();
    const minimalRowConfig = [...targetSectionConfigs]
      .filter(section => {
        return isUnderUpperBound(
          section?.limit,
          (section?.rows || []).length + 1,
        );
      })
      .sort(chartDataSectionRowLimitationComparer)?.[0];
    if (minimalRowConfig && row) {
      minimalRowConfig.rows = (minimalRowConfig.rows || []).concat([row]);
    }
  }
  return targetConfig!;
};

const transferNonMixedToMixed = (
  sectionType: ChartDataSectionType,
  targetConfig?: ChartConfig,
  sourceConfig?: ChartConfig,
): ChartConfig => {
  const targetDataConfigs = targetConfig?.datas || [];
  const sourceDataConfigs = sourceConfig?.datas || [];
  const sourceSectionConfigs = sourceDataConfigs.filter(
    c => c.type === sectionType,
  );
  const targetSectionConfigs = targetDataConfigs?.filter(
    c => c.type === sectionType,
  );

  if (
    isEmptyArray(sourceSectionConfigs) &&
    !isEmptyArray(targetSectionConfigs)
  ) {
    const allRows =
      sourceDataConfigs?.flatMap(config => config.rows || []) || [];
    const inUsedRows =
      targetDataConfigs?.flatMap(config => config.rows || []) || [];
    const notAssignedRows = allRows.filter(
      r => !inUsedRows.find(ur => ur?.uid === r?.uid),
    );
    while (Boolean(notAssignedRows?.length)) {
      const row = notAssignedRows.shift();
      const minimalRowConfig = [...targetSectionConfigs]
        .filter(section => {
          return isUnderUpperBound(
            section?.limit,
            (section?.rows || []).length + 1,
          );
        })
        .sort(chartDataSectionRowLimitationComparer)?.[0];
      if (minimalRowConfig && row) {
        minimalRowConfig.rows = (minimalRowConfig.rows || []).concat([row]);
      }
    }
  }

  return targetConfig!;
};

const transferMixedToNonMixed = (
  sectionType: ChartDataSectionType,
  targetConfig?: ChartConfig,
  sourceConfig?: ChartConfig,
): ChartConfig => {
  const targetDataConfigs = targetConfig?.datas || [];
  const sourceDataConfigs = sourceConfig?.datas || [];
  const sourceSectionConfigRows = sourceDataConfigs
    .filter(c => c.type === sectionType)
    .flatMap(config => config.rows || []);
  const targetSectionConfigs = targetDataConfigs?.filter(
    c => c.type === sectionType,
  );
  if (
    !isEmptyArray(sourceSectionConfigRows) &&
    isEmptyArray(targetSectionConfigs)
  ) {
    const dimensions = sourceSectionConfigRows?.filter(
      r =>
        r.type === DataViewFieldType.DATE ||
        r.type === DataViewFieldType.STRING,
    );
    const metrics = sourceSectionConfigRows?.filter(
      r => r.type === DataViewFieldType.NUMERIC,
    );

    while (Boolean(dimensions?.length)) {
      const groupTypeSections = targetDataConfigs?.filter(
        c => c.type === ChartDataSectionType.Group,
      );

      const row = dimensions.shift();
      const minimalRowConfig = [...groupTypeSections]
        .filter(section => {
          return isUnderUpperBound(
            section?.limit,
            (section?.rows || []).length + 1,
          );
        })
        .sort(chartDataSectionRowLimitationComparer)?.[0];
      if (minimalRowConfig && row) {
        minimalRowConfig.rows = (minimalRowConfig.rows || []).concat([row]);
      }
    }

    while (Boolean(metrics?.length)) {
      const aggTypeSections = targetDataConfigs?.filter(
        c => c.type === ChartDataSectionType.Aggregate,
      );

      const row = metrics.shift();
      const minimalRowConfig = [...aggTypeSections]
        .filter(section => {
          return isUnderUpperBound(
            section?.limit,
            (section?.rows || []).length + 1,
          );
        })
        .sort(chartDataSectionRowLimitationComparer)?.[0];
      if (minimalRowConfig && row) {
        minimalRowConfig.rows = (minimalRowConfig.rows || []).concat([row]);
      }
    }
  }

  return targetConfig!;
};

function chartDataSectionRowLimitationComparer(prev, next) {
  if (
    reachLowerBoundCount(prev?.limit, prev?.rows?.length) !==
    reachLowerBoundCount(next?.limit, next?.rows?.length)
  ) {
    return (
      reachLowerBoundCount(next?.limit, next?.rows?.length) -
      reachLowerBoundCount(prev?.limit, prev?.rows?.length)
    );
  }
  return (prev?.rows?.length || 0) - (next?.rows?.length || 0);
}

export function isInRange(limit?: ChartDataConfig['limit'], count: number = 0) {
  return cond(
    [isEmpty, true],
    [isNumerical, curry(isNumericEqual)(count)],
    [isPairArray, curry(isInPairArrayRange)(count)],
  )(limit, true);
}

export function isUnderUpperBound(
  limit?: ChartDataConfig['limit'],
  count: number = 0,
) {
  return cond(
    [isEmpty, true],
    [isNumerical, limit => limit >= +count],
    [isPairArray, limit => count <= +limit[1]],
  )(limit, true);
}

export function reachLowerBoundCount(
  limit?: ChartDataConfig['limit'],
  count: number = 0,
) {
  return cond(
    [isEmpty, 0],
    [isNumerical, limit => limit - count],
    [isPairArray, limit => +limit[0] - count],
  )(limit, 0);
}

export function getColumnRenderOriginName(c?: ChartDataSectionField) {
  if (!c) {
    return '[unknown]';
  }
  if (c.aggregate === AggregateFieldActionType.None) {
    return c.colName;
  }
  if (c.aggregate) {
    return `${c.aggregate}(${c.colName})`;
  }
  if (c.category === ChartDataViewFieldCategory.DateLevelComputedField) {
    return handleDateLevelsName({ ...c, name: c.colName });
  }
  return c.colName;
}

export function diffHeaderRows(
  oldRows: Array<{ colName: string }>,
  newRows: Array<{ colName: string }>,
) {
  if (oldRows?.length !== newRows?.length) {
    return true;
  }
  const oldNames = oldRows.map(r => r.colName).sort();
  const newNames = newRows.map(r => r.colName).sort();
  if (oldNames.toString() !== newNames.toString()) {
    return true;
  }

  return false;
}

export function flattenHeaderRowsWithoutGroupRow<
  T extends {
    isGroup?: boolean;
    children?: T[];
  },
>(groupedHeaderRow: T) {
  const childRows = (groupedHeaderRow.children || []).flatMap(child =>
    flattenHeaderRowsWithoutGroupRow(child),
  );
  if (groupedHeaderRow.isGroup) {
    return childRows;
  }
  return [groupedHeaderRow].concat(childRows);
}

export function transformMeta(model?: string) {
  if (!model) {
    return undefined;
  }
  const jsonObj = JSON.parse(model || '{}');
  const HierarchyModel = 'hierarchy' in jsonObj ? jsonObj.hierarchy : jsonObj;
  return Object.keys(HierarchyModel || {}).flatMap(colKey => {
    const column = HierarchyModel[colKey];
    if (!isEmptyArray(column?.children)) {
      return column.children.map(c => ({
        ...c,
        path: c.path,
        category: ChartDataViewFieldCategory.Field,
      }));
    }
    return {
      ...column,
      path: column.path || colKey,
      category: ChartDataViewFieldCategory.Field,
    };
  });
}

export function transformHierarchyMeta(model?: string): ChartDataViewMeta[] {
  if (!model) {
    return [];
  }
  const modelObj = JSON.parse(model || '{}');
  const hierarchyMeta = !Object.keys(modelObj?.hierarchy || {}).length
    ? modelObj.columns
    : modelObj.hierarchy;

  return Object.keys(hierarchyMeta || {}).map(key => {
    return getMeta(key, hierarchyMeta?.[key]);
  });
}

function getMeta(key, column) {
  let children;
  let isHierarchy = false;
  if (!isEmptyArray(column?.children)) {
    isHierarchy = true;
    children = column?.children.map(child => getMeta(child?.name, child));
  }
  return {
    ...column,
    subType: column?.category,
    category: isHierarchy
      ? ChartDataViewFieldCategory.Hierarchy
      : ChartDataViewFieldCategory.Field,
    children: children,
  };
}

export function getUpdatedChartStyleValue(tEle: any, sEle: any) {
  switch (typeof tEle) {
    /*case 'bigint':
      if (typeof sEle === 'bigint') return sEle;
      break;*/
    case 'boolean':
      if (typeof sEle === 'boolean') return sEle;
      break;
    case 'number':
    case 'string':
      if (typeof sEle === 'number' || typeof sEle === 'string') return sEle;
      break;
    case 'object':
      if (tEle === null) {
        return sEle;
      } else if (Array.isArray(tEle) && Array.isArray(sEle)) {
        return sEle;
      } else if (
        Object.prototype.toString.call(tEle) === '[object Object]' &&
        Object.prototype.toString.call(sEle) === '[object Object]'
      ) {
        return sEle;
      }
      break;
    case 'undefined':
      return sEle;
    default:
      if (typeof tEle === typeof sEle) {
        return sEle;
      }
  }
  return tEle;
}

export function mergeChartStyleConfigs(
  target?: ChartStyleConfig[],
  source?: ChartStyleConfigDTO[],
  options = { useDefault: true },
): ChartStyleConfig[] | undefined {
  if (isEmptyArray(target)) {
    return target;
  }
  if (isEmptyArray(source) && !options?.useDefault) {
    return target;
  }
  for (let index = 0; index < target?.length!; index++) {
    const tEle: any = target?.[index];
    if (!tEle) {
      continue;
    }

    // options.useDefault
    if (isUndefined(tEle['value']) && options?.useDefault) {
      tEle['value'] = tEle?.['default'];
    }

    const sEle =
      'key' in tEle ? source?.find(s => s?.key === tEle.key) : source?.[index];

    if (!isUndefined(sEle?.['value'])) {
      tEle['value'] = getUpdatedChartStyleValue(tEle['value'], sEle?.['value']);
    }

    if (!isUndefined(sEle?.['disabled'])) {
      tEle['disabled'] = sEle?.['disabled'];
    }

    if (tEle?.template && !isEmptyArray(sEle?.rows)) {
      const template = tEle.template;
      tEle['rows'] = sEle?.rows?.map(row => {
        const tRows = CloneValueDeep(template?.rows);
        const newRow = {
          ...template,
          /**
           * template overwrite principle:
           * 1. child key allow change by row key, eg. key is field uuid.
           * 2. child value allow change by row
           * 3. child disabled status allow change by row
           * 4. child action should be use template row action
           */
          key: row.key,
          rows: mergeChartStyleConfigs(tRows, row?.rows, options),
        };
        if (!isUndefined(row?.value)) {
          newRow.value = getUpdatedChartStyleValue(row.value, newRow?.value);
        }
        if (!isUndefined(row?.disabled)) {
          newRow.disabled = row.disabled;
        }
        return newRow;
      });
    } else if (!isEmptyArray(tEle?.rows)) {
      tEle['rows'] = mergeChartStyleConfigs(tEle.rows, sEle?.rows, options);
    } else if (sEle && !isEmptyArray(sEle?.rows)) {
      // Note: we merge all rows data when target rows is empty
      tEle['rows'] = sEle?.rows;
    }
  }
  return target;
}

export function mergeChartDataConfigs<
  T extends
    | { key?: string; rows?: ChartDataSectionField[]; drillable?: boolean }
    | undefined
    | null,
>(target?: T[], source?: T[]) {
  if (isEmptyArray(target) || isEmptyArray(source)) {
    return target;
  }
  return (target || []).map(tEle => {
    const sEle = (source || []).find(s => s?.key === tEle?.key);
    if (sEle) {
      return Object.assign(
        {},
        tEle,
        {
          rows: sEle?.rows,
        },
        !isUndefined(sEle?.drillable) ? { drillable: sEle?.drillable } : {},
      );
    }
    return tEle;
  });
}

export function getRequiredGroupedSections(dataConfig?) {
  return (
    dataConfig
      ?.filter(
        c =>
          c.type === ChartDataSectionType.Group ||
          c.type === ChartDataSectionType.Color,
      )
      .filter(c => !!c.required) || []
  );
}

export function getRequiredAggregatedSections(dataConfigs?) {
  return (
    dataConfigs
      ?.filter(
        c =>
          c.type === ChartDataSectionType.Aggregate ||
          c.type === ChartDataSectionType.Size,
      )
      .filter(c => !!c.required) || []
  );
}

// @deprecate 兼容 impala 聚合函数小写问题
// TODO(Stephen): should be remove and test in RC.1
export const filterSqlOperatorName = (requestParams, widgetData) => {
  const sqlOperatorNameList = requestParams.aggregators.map(aggConfig =>
    aggConfig.sqlOperator?.toLocaleLowerCase(),
  );
  if (!sqlOperatorNameList?.length) return widgetData;
  widgetData?.columns?.forEach(item => {
    const index = item.name.indexOf('(');
    const sqlOperatorName = item.name.slice(0, index);
    sqlOperatorNameList.includes(sqlOperatorName) &&
      (item.name =
        sqlOperatorName.toLocaleUpperCase() + item.name.slice(index));
  });
  return widgetData;
};

// 获取当前echart坐标轴区域的宽度
export function getAxisLengthByConfig(config: ChartCommonConfig) {
  const { chart, xAxis, yAxis, grid, series, yAxisNames, horizon } = config;
  const axisOpts = !horizon ? xAxis : yAxis;
  // datart 布局配置分为百分比和像素
  const getPositionLengthInfo = (
    positionConfig: string | number,
  ): {
    length: number;
    type: 'percent' | 'px';
  } => {
    if (typeof positionConfig === 'string') {
      const lengthPercentInt = parseInt(positionConfig.replace('%', ''), 10);
      if (isNaN(lengthPercentInt)) {
        throw new Error(`${positionConfig} is not a number`);
      }
      return {
        length: lengthPercentInt / 100,
        type: 'percent',
      };
    }
    return {
      length: positionConfig,
      type: 'px',
    };
  };

  // 获取坐标轴宽度
  const getAxisWidth = (YAxisLength: number): number => {
    return (Array.isArray(axisOpts) ? axisOpts : [axisOpts]).reduce(
      (prev, item) => {
        const { fontSize, show } = item.axisLabel;
        // 预留一个字符长度
        const axisLabelMaxWidth = show ? (YAxisLength + 1) * fontSize : 0;
        prev += axisLabelMaxWidth;
        return prev;
      },
      0,
    );
  };

  const { containerLabel, left, right } = grid;

  // 找到轴上最大的数字长度
  let foundMaxAxisLength = 0;

  if (containerLabel && !horizon) {
    foundMaxAxisLength = series.reduce((prev, sery) => {
      sery?.data?.forEach(item => {
        yAxisNames.forEach(name => {
          if (item.name === name) {
            const yNumStr = `${item[0]}`;
            if (yNumStr.length > prev) {
              prev = yNumStr.length;
            }
          }
        });
      });
      return prev;
    }, 0);
  }

  const axisLabelMaxWidth = getAxisWidth(foundMaxAxisLength);

  const left_ = getPositionLengthInfo(left);
  const right_ = getPositionLengthInfo(right);

  const containerWidth = chart?.getWidth() || 0;

  // 左右边距
  const leftWidth =
    left_.type === 'px' ? left_.length : containerWidth * left_.length;
  const rightWidth =
    right_.type === 'px' ? right_.length : containerWidth * right_.length;

  // 坐标轴区域宽度 = 容器宽度 - 最大字符所占长度 - 左右边距
  return containerWidth - axisLabelMaxWidth - leftWidth - rightWidth;
}

export const transformToViewConfig = (
  viewConfig?: string | object,
): {
  cache?: boolean;
  cacheExpires?: number;
  concurrencyControl?: boolean;
  concurrencyControlMode?: string;
} => {
  let viewConfigMap = viewConfig;
  if (typeof viewConfig === 'string') {
    viewConfigMap = JSON.parse(viewConfig || '{}');
  }
  const fields = [
    'cache',
    'cacheExpires',
    'concurrencyControl',
    'concurrencyControlMode',
  ];
  return fields.reduce((acc, cur) => {
    acc[cur] = viewConfigMap?.[cur];
    return acc;
  }, {});
};

export const buildDragItem = (item, children: any[] = []) => {
  return {
    colName: item?.name,
    type: item?.type,
    subType: item?.subType,
    category: item?.category,
    dateFormat: item?.dateFormat,
    children: children.map(c => buildDragItem(c)),
  };
};

/**
 * Get all Drill Paths
 *
 * @param {ChartDataConfig[]} configs
 * @return {*}  {ChartDataSectionField[]}
 */
const getDrillPaths = (
  configs?: ChartDataConfig[],
): ChartDataSectionField[] => {
  return (configs || [])
    ?.filter(c => c.type === ChartDataSectionType.Group)
    ?.filter(d => Boolean(d.drillable))
    ?.flatMap(r => r.rows || []);
};

/**
 * Create or Update Chart Drill Option
 *
 * @param {ChartDataConfig[]} [datas]
 * @param {IChartDrillOption} [drillOption]
 * @return {*}  {(IChartDrillOption | undefined)}
 */
export const getChartDrillOption = (
  datas?: ChartDataConfig[],
  drillOption?: IChartDrillOption,
  isClearAll?: boolean,
): IChartDrillOption | undefined => {
  const newDrillPaths = getDrillPaths(datas);
  if (isEmptyArray(newDrillPaths)) {
    return undefined;
  }
  if (
    !isEmptyArray(newDrillPaths) &&
    drillOption
      ?.getAllFields()
      ?.map(p => p.uid)
      .join('-') !== newDrillPaths.map(p => p.uid).join('-')
  ) {
    return new ChartDrillOption(newDrillPaths);
  }
  if (isClearAll) {
    drillOption?.clearAll();
  }
  return drillOption;
};

export const buildClickEventBaseFilters = (
  rowDatas?: Record<string, any>[],
  rule?: InteractionRule,
  drillOption?: IChartDrillOption,
  dataConfigs?: ChartDataConfig[],
): PendingChartDataRequestFilter[] => {
  const groupConfigs: ChartDataSectionField[] = getDrillableRows(
    dataConfigs || [],
    drillOption,
  );
  const colorConfigs = (dataConfigs || [])
    .filter(c => c.type === ChartDataSectionType.Color)
    .flatMap(config => config.rows || []);

  const mixConfigs = (dataConfigs || [])
    .filter(c => c.type === ChartDataSectionType.Mixed)
    .flatMap(config => config.rows || []);

  return groupConfigs
    .concat(colorConfigs)
    .concat(mixConfigs)
    .reduce<PendingChartDataRequestFilter[]>((acc, c) => {
      const filterValues = rowDatas
        ?.map(rowData => {
          return rowData?.[c.colName];
        })
        ?.filter(value => !isEmpty(value))
        ?.map(value => ({ value, valueType: c.type }));

      if (isEmptyArray(filterValues) || isEmpty(c.colName)) {
        return acc;
      }
      const filter = {
        aggOperator: null,
        sqlOperator: FilterSqlOperator.In,
        column: c.colName,
        values: filterValues,
      };
      acc.push(filter);
      return acc;
    }, []);
};

export const getJumpFiltersByInteractionRule = (
  clickEventFilters: PendingChartDataRequestFilter[] = [],
  chartFilters: PendingChartDataRequestFilter[] = [],
  variableFilters: PendingChartDataRequestFilter[] = [],
  rule?: InteractionRule,
): Record<string, string | any> => {
  return clickEventFilters
    .concat(chartFilters)
    .concat(variableFilters)
    .map(f => {
      if (isEmpty(f)) {
        return null;
      }
      if (f?.sqlOperator !== FilterSqlOperator.In) {
        return null;
      }
      const jumpRule = rule?.[rule.category!] as
        | JumpToDashboardRule
        | JumpToUrlRule;
      if (isEmpty(jumpRule)) {
        return null;
      }
      if (jumpRule?.['relation'] === InteractionFieldRelation.Auto) {
        return f;
      } else {
        const customizeRelations: CustomizeRelation[] = jumpRule?.[
          InteractionFieldRelation.Customize
        ]?.filter(r => r.type === InteractionRelationType.Field);
        if (isEmptyArray(customizeRelations)) {
          return null;
        }
        const targetRelation = customizeRelations?.find(
          r => r.source === f?.column && r?.target,
        );
        if (isEmpty(targetRelation)) {
          return null;
        }
        return Object.assign({}, f, {
          column: targetRelation?.target,
        }) as PendingChartDataRequestFilter;
      }
    })
    .filter(Boolean)
    .reduce((acc, cur) => {
      if (cur?.column) {
        const currentValues = cur?.values?.map(v => v.value) || [];
        const column = cur.column;

        if (column in acc) {
          const oldValues: string[] = acc[column] || [];
          if (isEmptyArray(oldValues)) {
            acc[column] = currentValues;
          } else {
            acc[column] = currentValues.filter(cv => oldValues.includes(cv));
          }
        } else {
          acc[column] = currentValues;
        }
      }
      return acc;
    }, {});
};

export const filterFiltersByInteractionRule = (
  rule?: InteractionRule,
  ...filters: PendingChartDataRequestFilter[]
): PendingChartDataRequestFilter[] => {
  if (!rule) {
    return filters;
  }
  return (filters || [])
    .map(f => {
      if (isEmpty(f)) {
        return null;
      }
      if (f?.sqlOperator !== FilterSqlOperator.In) {
        return null;
      }
      if (rule?.['relation'] === InteractionFieldRelation.Auto) {
        return f;
      } else {
        const customizeRelations: CustomizeRelation[] = rule?.[
          InteractionFieldRelation.Customize
        ]?.filter(r => r.type === InteractionRelationType.Field);
        if (isEmptyArray(customizeRelations)) {
          return null;
        }
        const targetRelation = customizeRelations?.find(
          r => r.source === f?.column && r?.target,
        );
        if (isEmpty(targetRelation)) {
          return null;
        }
        return Object.assign({}, f, {
          column: targetRelation?.target,
        }) as PendingChartDataRequestFilter;
      }
    })
    .filter(Boolean) as PendingChartDataRequestFilter[];
};

export const getLinkFiltersByInteractionRule = (
  clickEventFilters: PendingChartDataRequestFilter[] = [],
  chartFilters: PendingChartDataRequestFilter[] = [],
  variableFilters: PendingChartDataRequestFilter[] = [],
  rule?: InteractionRule,
): Record<string, string | any> => {
  const ruleFilters = filterFiltersByInteractionRule(
    rule,
    ...clickEventFilters,
    ...chartFilters,
    ...variableFilters,
  );
  return ruleFilters.reduce((acc, cur) => {
    if (cur?.column) {
      const currentValues = cur?.values?.map(v => v.value) || [];
      const column = cur.column!;
      if (column! in acc) {
        const oldValues: string[] = acc[column] || [];
        if (isEmptyArray(oldValues)) {
          acc[column] = currentValues;
        } else {
          acc[column] = currentValues.filter(cv => oldValues.includes(cv));
        }
      } else {
        acc[column] = currentValues;
      }
    }
    return acc;
  }, {});
};

export const getJumpOperationFiltersByInteractionRule = (
  clickEventFilters: PendingChartDataRequestFilter[] = [],
  chartFilters: PendingChartDataRequestFilter[] = [],
  rule?: InteractionRule,
): PendingChartDataRequestFilter[] => {
  return clickEventFilters
    .concat(chartFilters)
    .reduce<PendingChartDataRequestFilter[]>((acc, f) => {
      if (isEmpty(f)) {
        return acc;
      }
      const jumpRule = rule?.[rule.category!] as
        | JumpToChartRule
        | JumpToDashboardRule;
      if (isEmpty(jumpRule)) {
        return acc;
      }
      if (jumpRule?.['relation'] === InteractionFieldRelation.Auto) {
        return acc.concat(f);
      } else {
        const customizeRelations: CustomizeRelation[] = jumpRule?.[
          InteractionFieldRelation.Customize
        ]?.filter(r => r.type === InteractionRelationType.Field);
        if (isEmptyArray(customizeRelations)) {
          return acc;
        }

        const targetRelation = customizeRelations?.find(
          r => r.source === f?.column && r?.target,
        );
        if (isEmpty(targetRelation)) {
          return acc;
        }
        return acc.concat(
          Object.assign({}, f, {
            column: targetRelation?.target,
          }),
        );
      }
    }, []);
};

export const filterVariablesByInteractionRule = (
  rule?: InteractionRule,
  variables?: Record<string, any[]>,
) => {
  if (rule?.[rule.category!]?.['relation'] === InteractionFieldRelation.Auto) {
    return undefined;
  }
  const customizeRelations: CustomizeRelation[] =
    rule?.[rule.category!]?.[InteractionFieldRelation.Customize]?.filter(
      r => r.type === InteractionRelationType.Variable,
    ) || [];
  if (isEmptyArray(customizeRelations)) {
    return undefined;
  }
  return Object.keys(variables || {}).reduce((acc, cur) => {
    if (customizeRelations.some(r => r.source === cur)) {
      acc[cur] = variables?.[cur];
    }
    return acc;
  }, {});
};

export const getVariablesByInteractionRule = (
  queryVariables?: Variable[],
  rule?: InteractionRule,
): Record<string, any[]> | undefined => {
  if (rule?.[rule.category!]?.['relation'] === InteractionFieldRelation.Auto) {
    return undefined;
  }
  const customizeRelations: CustomizeRelation[] =
    rule?.[rule.category!]?.[InteractionFieldRelation.Customize]?.filter(
      r => r.type === InteractionRelationType.Variable,
    ) || [];

  if (isEmptyArray(customizeRelations)) {
    return undefined;
  }
  return customizeRelations?.reduce((acc, cur) => {
    const sourceVariableValueStr = queryVariables
      ?.filter(v => v.type === VariableTypes.Query)
      ?.find(v => v.name === cur.source)?.defaultValue;
    if (sourceVariableValueStr && cur.target) {
      acc[cur.target] = JSON.parse(sourceVariableValueStr || '{}');
      return acc;
    }
    return acc;
  }, {});
};

export const variableToFilter = (
  queryVariables?: Record<string, any[]>,
): PendingChartDataRequestFilter[] => {
  return Object.entries(queryVariables || {}).map(([k, v]) => {
    return {
      sqlOperator: FilterSqlOperator.In,
      column: k,
      values: v?.map(value => ({ value, valueType: 'STRING' })),
    };
  });
};

// NOTE There are functions from Echarts, but there are changes.
// NOTE from echarts/src/scale/Interval.ts.
export function calcNiceExtent(
  extent: [number, number],
  splitNumber: number = 5,
): IntervalScaleNiceTicksResult {
  if (extent[0] === extent[1]) {
    if (extent[0] !== 0) {
      const expandSize = extent[0];
      extent[1] += expandSize / 2;
      extent[0] -= expandSize / 2;
    } else {
      extent[1] = 1;
    }
  }
  const span = extent[1] - extent[0];
  if (!isFinite(span)) {
    return { extent } as IntervalScaleNiceTicksResult;
  }
  if (span < 0) {
    extent.reverse();
  }
  const result = intervalScaleNiceTicks(extent, splitNumber);
  extent = result.extent;
  const interval = result.interval;
  extent[0] = round(Math.floor(extent[0] / interval) * interval);
  extent[1] = round(Math.ceil(extent[1] / interval) * interval);
  return { ...result, extent };
}

// NOTE from echarts/src/scale/helper.ts
function clamp(
  niceTickExtent: [number, number],
  idx: number,
  extent: [number, number],
): void {
  niceTickExtent[idx] = Math.max(
    Math.min(niceTickExtent[idx], extent[1]),
    extent[0],
  );
}

function getIntervalPrecision(interval: number) {
  interval = +interval;
  if (isNaN(interval)) {
    return 0;
  }
  if (interval > 1e-14) {
    let e2 = 1;
    for (let i = 0; i < 15; i++, e2 *= 10) {
      if (Math.round(interval * e2) / e2 === interval) {
        return i;
      }
    }
  }
  const str = interval.toString().toLowerCase();
  const eIndex = str.indexOf('e');
  const exp = eIndex > 0 ? +str.slice(eIndex + 1) : 0;
  const significandPartLen = eIndex > 0 ? eIndex : str.length;
  const dotIndex = str.indexOf('.');
  const decimalPartLen = dotIndex < 0 ? 0 : significandPartLen - 1 - dotIndex;
  return Math.max(0, decimalPartLen - exp) + 2;
}

function intervalScaleNiceTicks(
  extent: [number, number],
  splitNumber: number,
  minInterval?: number,
  maxInterval?: number,
): IntervalScaleNiceTicksResult {
  const result = {} as IntervalScaleNiceTicksResult;
  const span = extent[1] - extent[0];

  let interval = (result.interval = nice(span / splitNumber, true));

  if (minInterval != null && interval < minInterval) {
    interval = result.interval = minInterval;
  }
  if (maxInterval != null && interval > maxInterval) {
    interval = result.interval = maxInterval;
  }

  const precision = (result.intervalPrecision = getIntervalPrecision(interval));

  const niceTickExtent = (result.niceTickExtent = [
    round(Math.ceil(extent[0] / interval) * interval, precision),
    round(Math.floor(extent[1] / interval) * interval, precision),
  ]);
  !isFinite(niceTickExtent[0]) && (niceTickExtent[0] = extent[0]);
  !isFinite(niceTickExtent[1]) && (niceTickExtent[1] = extent[1]);
  clamp(niceTickExtent, 0, extent);
  clamp(niceTickExtent, 1, extent);
  if (niceTickExtent[0] > niceTickExtent[1]) {
    niceTickExtent[0] = niceTickExtent[1];
  }
  result.extent = extent;
  return result;
}

// NOTE from echarts/src/util/number.ts
function quantityExponent(val: number): number {
  if (val === 0) {
    return 0;
  }
  let exp = Math.floor(Math.log(val) / Math.LN10);
  if (val / Math.pow(10, exp) >= 10) {
    exp++;
  }
  return exp;
}

function nice(val, round7) {
  const exponent = quantityExponent(val);
  const exp10 = Math.pow(10, exponent);
  const f = val / exp10;
  let nf;
  if (round7) {
    if (f < 1.5) {
      nf = 1;
    } else if (f < 2.5) {
      nf = 2;
    } else if (f < 4) {
      nf = 3;
    } else if (f < 7) {
      nf = 5;
    } else {
      nf = 10;
    }
  } else {
    if (f < 1) {
      nf = 1;
    } else if (f < 2) {
      nf = 2;
    } else if (f < 3) {
      nf = 3;
    } else if (f < 5) {
      nf = 5;
    } else {
      nf = 10;
    }
  }
  val = nf * exp10;
  return exponent >= -20 ? +val.toFixed(exponent < 0 ? -exponent : 0) : val;
}
