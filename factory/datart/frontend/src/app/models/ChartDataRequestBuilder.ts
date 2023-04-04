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
  AggregateFieldActionType,
  ChartDataSectionType,
  ChartDataViewFieldCategory,
  DataViewFieldType,
  FilterConditionType,
  SortActionType,
} from 'app/constants';
import {
  ChartDataConfig,
  ChartDataSectionField,
  RelationFilterValue,
} from 'app/types/ChartConfig';
import { ChartStyleConfigDTO } from 'app/types/ChartConfigDTO';
import {
  ChartDataRequest,
  ChartDataRequestFilter,
  PendingChartDataRequestFilter,
} from 'app/types/ChartDataRequest';
import { ChartDatasetPageInfo } from 'app/types/ChartDataSet';
import ChartDataView from 'app/types/ChartDataView';
import { ChartDataViewMeta } from 'app/types/ChartDataViewMeta';
import { IChartDrillOption } from 'app/types/ChartDrillOption';
import {
  findPathByNameInMeta,
  getAllColumnInMeta,
  getRuntimeDateLevelFields,
  getValue,
} from 'app/utils/chartHelper';
import { transformToViewConfig } from 'app/utils/internalChartHelper';
import {
  formatTime,
  getTime,
  recommendTimeRangeConverter,
  splitRangerDateFilters,
} from 'app/utils/time';
import {
  FilterSqlOperator,
  RUNTIME_FILTER_KEY,
  TIME_FORMATTER,
} from 'globalConstants';
import {
  isEmptyArray,
  isEmptyString,
  isEqualObject,
  IsKeyIn,
  UniqWith,
} from 'utils/object';
import { DrillMode } from './ChartDrillOption';

export class ChartDataRequestBuilder {
  extraSorters: ChartDataRequest['orders'] = [];
  extraRuntimeFilters: ChartDataRequestFilter[] = [];
  chartDataConfigs: ChartDataConfig[];
  chartSettingConfigs;
  pageInfo;
  dataView;
  script: boolean;
  aggregation?: boolean;
  drillOption?: IChartDrillOption;
  variableParams?: Record<string, any[]>;

  constructor(
    dataView: Pick<ChartDataView, 'id' | 'computedFields' | 'type' | 'meta'> & {
      config: string | object;
    },
    dataConfigs?: ChartDataConfig[],
    settingConfigs?: ChartStyleConfigDTO[],
    pageInfo?: ChartDatasetPageInfo,
    script?: boolean,
    aggregation?: boolean,
  ) {
    this.dataView = dataView;
    this.chartDataConfigs = dataConfigs || [];
    this.chartSettingConfigs = settingConfigs || [];
    this.pageInfo = pageInfo || {};
    this.script = script || false;
    this.aggregation = aggregation;
  }
  public addExtraSorters(sorters: ChartDataRequest['orders'] = []) {
    if (!isEmptyArray(sorters)) {
      this.extraSorters = this.extraSorters.concat(sorters!);
    }
    return this;
  }

  public addDrillOption(drillOption?: IChartDrillOption) {
    this.drillOption = drillOption;
    return this;
  }

  public addRuntimeFilters(filters: PendingChartDataRequestFilter[] = []) {
    if (!isEmptyArray(filters)) {
      this.extraRuntimeFilters = filters.map(v => {
        return {
          ...v,
          column: this.buildColumnName({ v, colName: v.column }),
        };
      });
    }
    return this;
  }

  public addVariableParams(params?: Record<string, any[]>) {
    if (params) {
      this.variableParams = params;
    }
    return this;
  }

  private buildAggregators() {
    if (this.aggregation === false) {
      return [];
    }

    const aggColumns = this.chartDataConfigs.reduce<ChartDataSectionField[]>(
      (acc, cur) => {
        if (!cur.rows) {
          return acc;
        }
        if (
          cur.type === ChartDataSectionType.Aggregate ||
          cur.type === ChartDataSectionType.Size ||
          cur.type === ChartDataSectionType.Info
        ) {
          return acc.concat(cur.rows);
        }

        if (
          cur.type === ChartDataSectionType.Mixed &&
          cur.rows?.findIndex(v => v.type === DataViewFieldType.NUMERIC) !== -1
        ) {
          return acc.concat(
            cur.rows.filter(v => v.type === DataViewFieldType.NUMERIC),
          );
        }
        return acc;
      },
      [],
    );

    return UniqWith(
      aggColumns.map(aggCol => ({
        alias: this.buildAliasName(aggCol),
        column: this.buildColumnName(aggCol),
        sqlOperator: aggCol.aggregate!,
      })),
      (a, b) =>
        isEqualObject(a.column, b.column) && a.sqlOperator === b.sqlOperator,
    );
  }

  private buildAliasName(c) {
    if (c.aggregate === AggregateFieldActionType.None) {
      return c.colName;
    }
    if (c.aggregate) {
      return `${c.aggregate}(${c.colName})`;
    }
    return c.colName;
  }

  private buildColumnName(col) {
    const row = findPathByNameInMeta(this.dataView.meta, col.colName);
    if (row) {
      return row?.path || [];
    } else {
      return [col.colName];
    }
  }

  private buildGroups() {
    /**
     * If aggregation is off, do not add values to gruop
     */
    if (this.aggregation === false) {
      return [];
    }
    const groupColumns = this.chartDataConfigs.reduce<ChartDataSectionField[]>(
      (acc, cur) => {
        if (isEmptyArray(cur.rows)) {
          return acc;
        }
        if (cur.type === ChartDataSectionType.Color) {
          return acc.concat(cur.rows || []);
        }
        if (cur.type === ChartDataSectionType.Group) {
          const rows = getRuntimeDateLevelFields(cur.rows);

          if (cur.drillable) {
            if (this.isInValidDrillOption()) {
              return acc.concat(rows?.[0] || []);
            }
            return acc.concat(
              rows?.filter(field => {
                return Boolean(
                  this.drillOption
                    ?.getCurrentFields()
                    ?.some(df => df.uid === field.uid),
                );
              }) || [],
            );
          }
          return acc.concat(rows || []);
        }
        if (cur.type === ChartDataSectionType.Mixed) {
          const dateAndStringFields = cur.rows?.filter(v =>
            [DataViewFieldType.DATE, DataViewFieldType.STRING].includes(v.type),
          );
          //zh: 判断数据中是否含有 DATE 和 STRING 类型 en: Determine whether the data contains DATE and STRING types
          return acc.concat(dateAndStringFields || []);
        }
        return acc;
      },
      [],
    );
    const newGroupColumns = groupColumns.map(groupCol => ({
      alias: this.buildAliasName(groupCol),
      column: this.buildColumnName(groupCol),
    }));
    return UniqWith(newGroupColumns, (a, b) =>
      isEqualObject(a.column, b.column),
    );
  }

  private buildFilters(): ChartDataRequestFilter[] {
    const fields: ChartDataSectionField[] = this.chartDataConfigs
      .reduce<ChartDataSectionField[]>((acc, cur) => {
        if (!cur.rows || cur.type !== ChartDataSectionType.Filter) {
          return acc;
        }
        return acc.concat(cur.rows);
      }, [])
      .filter(col => Boolean(col.filter?.condition))
      .filter(col => {
        if (
          col.filter?.condition?.operator === FilterSqlOperator.Null ||
          col.filter?.condition?.operator === FilterSqlOperator.NotNull
        ) {
          return true;
        } else if (Array.isArray(col.filter?.condition?.value)) {
          return Boolean(col.filter?.condition?.value?.length);
        } else if (
          [
            FilterSqlOperator.Contain,
            FilterSqlOperator.NotContain,
            FilterSqlOperator.Equal,
            FilterSqlOperator.NotEqual,
            FilterSqlOperator.In,
            FilterSqlOperator.NotIn,
            FilterSqlOperator.PrefixContain,
            FilterSqlOperator.NotPrefixContain,
            FilterSqlOperator.SuffixContain,
            FilterSqlOperator.NotSuffixContain,
          ].includes(col.filter?.condition?.operator as FilterSqlOperator)
        ) {
          return !isEmptyString(col.filter?.condition?.value);
        } else {
          return true;
        }
      })
      .map(col => col);

    return this.normalizeFilters(fields)
      .concat(this.normalizeDrillFilters())
      .concat(this.normalizeRuntimeFilters());
  }

  private normalizeFilters = (fields: ChartDataSectionField[]) => {
    const _timeConverter = (visualType, value, dateFormat = TIME_FORMATTER) => {
      if (visualType !== 'DATE') {
        return value;
      }
      if (Boolean(value) && typeof value === 'object' && 'unit' in value) {
        const time = getTime(+(value.direction + value.amount), value.unit)(
          value.unit,
          value.isStart,
        );
        return formatTime(time, dateFormat);
      }
      return formatTime(value, dateFormat);
    };

    const _transformFieldValues = (field: ChartDataSectionField) => {
      const conditionValue = field.filter?.condition?.value;
      const dateFormat = field.dateFormat;
      if (!conditionValue) {
        return null;
      }
      if (Array.isArray(conditionValue)) {
        return conditionValue
          .map(v => {
            if (IsKeyIn(v as RelationFilterValue, 'key')) {
              const listItem = v as RelationFilterValue;
              if (!listItem.isSelected) {
                return undefined;
              }
              return {
                value: listItem.key,
                valueType: field.type,
              };
            } else {
              return {
                value: _timeConverter(
                  field.filter?.condition?.visualType,
                  v,
                  dateFormat,
                ),
                valueType: field.type,
              };
            }
          })
          .filter(Boolean) as any[];
      }
      if (
        field?.filter?.condition?.type === FilterConditionType.RecommendTime
      ) {
        const timeRange = recommendTimeRangeConverter(
          conditionValue,
          dateFormat,
        );
        return timeRange.map(t => ({
          value: t,
          valueType: field.type,
        }));
      }
      return [
        {
          value: _timeConverter(
            field.filter?.condition?.visualType,
            conditionValue,
            dateFormat,
          ),
          valueType: field.type,
        },
      ];
    };
    const filters = fields
      .map(field => {
        if (
          field.filter?.condition?.operator === FilterSqlOperator.In ||
          field.filter?.condition?.operator === FilterSqlOperator.NotIn
        ) {
          if (isEmptyArray(_transformFieldValues(field))) {
            return null;
          }
        }
        return {
          aggOperator:
            field.aggregate === AggregateFieldActionType.None
              ? null
              : field.aggregate,
          column: this.buildColumnName(field),
          sqlOperator: field.filter?.condition?.operator!,
          values: _transformFieldValues(field) || [],
        };
      })
      .filter(Boolean) as ChartDataRequestFilter[];
    return splitRangerDateFilters(filters);
  };

  private normalizeDrillFilters(): ChartDataRequestFilter[] {
    return (this.drillOption
      ?.getAllDrillDownFields()
      .filter(field => Boolean(field.condition))
      .map(f => {
        return {
          aggOperator: null,
          column: this.buildColumnName({ colName: f.condition?.name! }),
          sqlOperator: f.condition?.operator! as FilterSqlOperator,
          values: [
            { value: f.condition?.value as string, valueType: 'STRING' },
          ],
        };
      }) || []) as ChartDataRequestFilter[];
  }

  private normalizeRuntimeFilters(): ChartDataRequestFilter[] {
    return (this.chartDataConfigs || [])
      .filter(c => c.type === ChartDataSectionType.Filter)
      .flatMap(c => {
        return c[RUNTIME_FILTER_KEY] || [];
      })
      .map(v => {
        return {
          ...v,
          column: this.buildColumnName({ colName: v.column }),
        };
      })
      .concat(this.extraRuntimeFilters);
  }

  private buildOrders() {
    const sortColumns = this.chartDataConfigs
      .reduce<ChartDataSectionField[]>((acc, cur) => {
        if (!cur.rows) {
          return acc;
        }
        if (
          cur.type === ChartDataSectionType.Aggregate ||
          cur.type === ChartDataSectionType.Mixed
        ) {
          return acc.concat(cur.rows);
        }
        if (cur.type === ChartDataSectionType.Group) {
          const rows = getRuntimeDateLevelFields(cur.rows);

          if (cur.drillable) {
            if (this.isInValidDrillOption()) {
              return acc.concat(cur.rows?.[0] || []);
            }
            return acc.concat(
              rows?.filter(field => {
                return Boolean(
                  this.drillOption
                    ?.getCurrentFields()
                    ?.some(df => df.uid === field.uid),
                );
              }) || [],
            );
          }
          return acc.concat(rows || []);
        }
        return acc;
      }, [])
      .filter(
        col =>
          col?.sort?.type &&
          [SortActionType.ASC, SortActionType.DESC].includes(col?.sort?.type),
      );

    const originalSorters = sortColumns.map(aggCol => ({
      column: this.buildColumnName(aggCol),
      operator: aggCol.sort?.type!,
      aggOperator: aggCol.aggregate,
    }));

    const _extraSorters = this.extraSorters
      ?.filter(({ column, operator }) => column && operator)
      .map(v => {
        return {
          ...v,
          column: this.buildColumnName({ colName: v.column }),
        };
      });

    if (!isEmptyArray(_extraSorters)) {
      return _extraSorters;
    }
    return originalSorters.filter(sorter => Boolean(sorter?.operator));
  }

  private buildPageInfo() {
    const settingStyles = this.chartSettingConfigs;
    const pageSize = getValue(settingStyles, ['paging', 'pageSize']);
    const enablePaging = getValue(settingStyles, ['paging', 'enablePaging']);
    return {
      countTotal: !!enablePaging,
      pageNo: this.pageInfo?.pageNo,
      pageSize: pageSize || 1000,
    };
  }

  private buildFunctionColumns() {
    const computedFields = getRuntimeDateLevelFields(
      this.dataView.computedFields,
    );
    const fieldsNameList = (this.chartDataConfigs || [])
      .flatMap(config => getRuntimeDateLevelFields(config.rows) || [])
      .flatMap(row => row?.colName || []);
    const currentUsedComputedFields = computedFields?.filter(v =>
      fieldsNameList.includes(v.name),
    );

    return (currentUsedComputedFields || []).map(f => ({
      alias: f.name!,
      snippet: f.expression,
    }));
  }

  private buildSelectColumns() {
    const selectColumns = this.chartDataConfigs.reduce<ChartDataSectionField[]>(
      (acc, cur) => {
        if (!cur.rows) {
          return acc;
        }

        if (this.aggregation === false) {
          if (
            cur.type === ChartDataSectionType.Color ||
            cur.type === ChartDataSectionType.Aggregate ||
            cur.type === ChartDataSectionType.Size ||
            cur.type === ChartDataSectionType.Info ||
            cur.type === ChartDataSectionType.Mixed
          ) {
            return acc.concat(cur.rows);
          } else if (cur.type === ChartDataSectionType.Group) {
            if (cur.drillable) {
              if (this.isInValidDrillOption()) {
                return acc.concat(cur.rows?.[0] || []);
              }
              return acc.concat(
                cur.rows?.filter(field => {
                  return Boolean(
                    this.drillOption
                      ?.getCurrentFields()
                      ?.some(df => df.uid === field.uid),
                  );
                }) || [],
              );
            } else {
              return acc.concat(cur.rows);
            }
          }
        }

        return acc;
      },
      [],
    );
    return selectColumns.map(col => {
      return {
        alias: this.buildAliasName(col),
        column: this.buildColumnName(col),
      };
    });
  }

  private buildDetailColumns() {
    const selectColumns = this.chartDataConfigs.reduce<ChartDataSectionField[]>(
      (acc, cur) => {
        if (!cur.rows) {
          return acc;
        }
        if (cur.drillable) {
          if (this.isInValidDrillOption()) {
            return acc.concat(cur.rows?.[0] || []);
          }
          return acc.concat(
            cur.rows?.filter(field => {
              return Boolean(
                this.drillOption
                  ?.getCurrentFields()
                  ?.some(df => df.uid === field.uid),
              );
            }) || [],
          );
        } else {
          return acc.concat(cur.rows);
        }
      },
      [],
    );

    return selectColumns
      ?.filter(
        c => c.category !== ChartDataViewFieldCategory.AggregateComputedField,
      )
      ?.reduce<ChartDataSectionField[]>((acc, cur) => {
        if (acc.find(x => x?.colName === cur.colName)) {
          return acc;
        }
        return acc.concat(cur);
      }, [])
      ?.map(col => {
        return {
          alias: this.buildAliasName(col),
          column: this.buildColumnName(col),
        };
      });
  }

  private buildViewConfigs() {
    return transformToViewConfig(this.dataView?.config);
  }

  private isInValidDrillOption() {
    return (
      !this.drillOption ||
      this.drillOption?.mode === DrillMode.Normal ||
      !this.drillOption?.getCurrentFields()
    );
  }

  private removeInvalidFilter(filters: ChartDataRequestFilter[]) {
    const dataViewFieldsNames = (
      getAllColumnInMeta(this.dataView?.meta) as ChartDataViewMeta[]
    )
      .concat(this.dataView?.computedFields || [])
      .map(c => c?.name);

    return (filters || []).filter(f => {
      return dataViewFieldsNames.includes(f.column.join('.'));
    });
  }

  public build(): ChartDataRequest {
    const validFilters = this.removeInvalidFilter(this.buildFilters());
    return {
      ...this.buildViewConfigs(),
      viewId: this.dataView?.id,
      aggregators: this.buildAggregators(),
      groups: this.buildGroups(),
      filters: validFilters,
      orders: this.buildOrders(),
      pageInfo: this.buildPageInfo(),
      functionColumns: this.buildFunctionColumns(),
      columns: this.buildSelectColumns(),
      script: this.script,
      params: this.variableParams,
    };
  }

  public buildDetails(): ChartDataRequest {
    const validFilters = this.removeInvalidFilter(
      this.buildFilters().filter(f => !f.aggOperator),
    );
    return {
      ...this.buildViewConfigs(),
      viewId: this.dataView?.id,
      aggregators: [],
      groups: [],
      filters: validFilters,
      orders: [],
      pageInfo: this.buildPageInfo(),
      functionColumns: this.buildFunctionColumns(),
      columns: this.buildDetailColumns(),
      script: this.script,
      params: this.variableParams,
    };
  }

  public getColNameStringFilter(): PendingChartDataRequestFilter[] {
    return this.buildFilters().map(v => {
      const row = getAllColumnInMeta(this.dataView.meta)?.find(val =>
        isEqualObject(val.path, v.column),
      );
      return {
        ...v,
        column: row?.name || '',
      };
    });
  }
}
