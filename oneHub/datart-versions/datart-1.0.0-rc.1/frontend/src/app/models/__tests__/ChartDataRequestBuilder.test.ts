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
} from 'app/constants';
import { getChartDrillOption } from 'app/utils/internalChartHelper';
import { FilterSqlOperator, RECOMMEND_TIME } from 'globalConstants';
import moment from 'moment';
import { ChartDataRequestBuilder } from '../ChartDataRequestBuilder';

describe('ChartDataRequestBuild Test', () => {
  test('should get builder with default values', () => {
    const dataView = { id: 'view-id' } as any;
    const enableScript = false;
    const enableAggregation = false;

    const builder = new ChartDataRequestBuilder(
      dataView,
      undefined,
      undefined,
      undefined,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams).toEqual({
      aggregators: [],
      columns: [],
      filters: [],
      functionColumns: [],
      groups: [],
      orders: [],
      pageInfo: {
        countTotal: false,
        pageNo: undefined,
        pageSize: 1000,
      },
      script: false,
      viewId: 'view-id',
    });
  });

  test('should get aggregators with enabled aggregation', () => {
    const dataView = { id: 'view-id' } as any;
    const chartDataConfigs = [
      {
        type: ChartDataSectionType.Aggregate,
        key: 'aggregation',
        rows: [
          {
            colName: 'amount',
            aggregate: AggregateFieldActionType.Avg,
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.Field as any,
          },
          {
            colName: 'sub-amount',
            aggregate: AggregateFieldActionType.Sum,
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.ComputedField as any,
          },
        ],
      },
      {
        type: ChartDataSectionType.Aggregate,
        key: 'aggregation',
      },
      {
        type: ChartDataSectionType.Size,
        key: 'size',
        rows: [
          {
            colName: 'total',
            aggregate: AggregateFieldActionType.Count,
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.ComputedField as any,
          },
        ],
      },
      {
        type: ChartDataSectionType.Info,
        key: 'info',
        rows: [
          {
            colName: 'sex',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Variable as any,
          },
        ],
      },
      {
        type: ChartDataSectionType.Mixed,
        key: 'info',
        rows: [
          {
            colName: 'sex',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Variable as any,
          },
          {
            colName: 'age',
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.Variable as any,
          },
        ],
      },
      {
        type: ChartDataSectionType.Group,
        key: 'unknown',
        rows: [
          {
            colName: 'name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Variable as any,
          },
        ],
      },
    ];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.aggregators).toEqual([
      { alias: 'AVG(amount)', column: ['amount'], sqlOperator: 'AVG' },
      { alias: 'SUM(sub-amount)', column: ['sub-amount'], sqlOperator: 'SUM' },
      { alias: 'COUNT(total)', column: ['total'], sqlOperator: 'COUNT' },
      { alias: 'sex', column: ['sex'], sqlOperator: undefined },
      { alias: 'age', column: ['age'], sqlOperator: undefined },
    ]);
  });

  test('should get aggregators with enabled aggregation for struct view', () => {
    const dataView = {
      id: 'view-id',
      type: 'STRUCT',
      meta: [{ path: ['dad', 'amount'], name: 'dad.amount' }],
    } as any;
    const chartDataConfigs = [
      {
        type: ChartDataSectionType.Aggregate,
        key: 'aggregation',
        rows: [
          {
            colName: 'dad.amount',
            aggregate: AggregateFieldActionType.Avg,
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.Field as any,
          },
          {
            colName: 'sub-amount',
            aggregate: AggregateFieldActionType.Sum,
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.ComputedField as any,
          },
        ],
      },
      {
        type: ChartDataSectionType.Aggregate,
        key: 'aggregation',
      },
      {
        type: ChartDataSectionType.Size,
        key: 'size',
        rows: [
          {
            colName: 'total',
            aggregate: AggregateFieldActionType.Count,
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.ComputedField as any,
          },
        ],
      },
      {
        type: ChartDataSectionType.Info,
        key: 'info',
        rows: [
          {
            colName: 'sex',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Variable as any,
          },
        ],
      },
      {
        type: ChartDataSectionType.Mixed,
        key: 'info',
        rows: [
          {
            colName: 'sex',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Variable as any,
          },
          {
            colName: 'age',
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.Variable as any,
          },
        ],
      },
      {
        type: ChartDataSectionType.Group,
        key: 'unknown',
        rows: [
          {
            colName: 'dad.name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Variable as any,
          },
        ],
      },
    ];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.aggregators).toEqual([
      {
        alias: 'AVG(dad.amount)',
        column: ['dad', 'amount'],
        sqlOperator: 'AVG',
      },
      {
        alias: 'SUM(sub-amount)',
        column: ['sub-amount'],
        sqlOperator: 'SUM',
      },
      {
        alias: 'COUNT(total)',
        column: ['total'],
        sqlOperator: 'COUNT',
      },
      { alias: 'sex', column: ['sex'], sqlOperator: undefined },
      { alias: 'age', column: ['age'], sqlOperator: undefined },
    ]);
  });

  test('should not get aggregators with disable aggregation', () => {
    const dataView = { id: 'view-id' } as any;
    const chartDataConfigs = [
      {
        type: ChartDataSectionType.Aggregate,
        key: 'aggregation',
        rows: [
          {
            colName: 'amount',
            aggregate: AggregateFieldActionType.Avg,
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.Field as any,
          },
        ],
      },
    ];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = false;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.aggregators).toEqual([]);
  });

  test('should unique aggregators with colName and aggregation', () => {
    const dataView = { id: 'view-id' } as any;
    const chartDataConfigs = [
      {
        type: ChartDataSectionType.Aggregate,
        key: 'aggregation',
        rows: [
          {
            colName: 'amount',
            aggregate: AggregateFieldActionType.Avg,
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.Field as any,
          },
        ],
      },
      {
        type: ChartDataSectionType.Size,
        key: 'size',
        rows: [
          {
            colName: 'amount',
            aggregate: AggregateFieldActionType.Avg,
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.Field as any,
          },
        ],
      },
    ];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.aggregators).toEqual([
      { alias: 'AVG(amount)', column: ['amount'], sqlOperator: 'AVG' },
    ]);
  });

  test('should unique aggregators with colName and aggregation for struct view', () => {
    const dataView = {
      id: 'view-id',
      type: 'STRUCT',
      meta: [{ name: 'dad.amount', path: ['dad', 'amount'] }],
    } as any;
    const chartDataConfigs = [
      {
        type: ChartDataSectionType.Aggregate,
        key: 'aggregation',
        rows: [
          {
            colName: 'dad.amount',
            aggregate: AggregateFieldActionType.Avg,
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.Field as any,
          },
        ],
      },
      {
        type: ChartDataSectionType.Size,
        key: 'size',
        rows: [
          {
            colName: 'dad.amount',
            aggregate: AggregateFieldActionType.Avg,
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.Field as any,
          },
        ],
      },
    ];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.aggregators).toEqual([
      {
        alias: 'AVG(dad.amount)',
        column: ['dad', 'amount'],
        sqlOperator: 'AVG',
      },
    ]);
  });

  test('should get groups', () => {
    const dataView = { id: 'view-id' } as any;
    const chartDataConfigs = [
      {
        type: ChartDataSectionType.Group,
        key: 'aggregation',
      },
      {
        type: ChartDataSectionType.Group,
        key: 'aggregation',
        rows: [
          {
            colName: 'name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
          },
        ],
      },
      {
        type: ChartDataSectionType.Color,
        key: 'aggregation',
        rows: [
          {
            colName: 'age',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
          },
        ],
      },
      {
        type: ChartDataSectionType.Mixed,
        key: 'address',
        rows: [
          {
            colName: 'address',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
          },
          {
            colName: 'post',
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.Field as any,
          },
        ],
      },
    ];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.groups).toEqual([
      { column: ['name'], alias: 'name' },
      { column: ['age'], alias: 'age' },
      { column: ['address'], alias: 'address' },
    ]);

    const enableAggregation2 = false;

    const builder2 = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation2,
    );
    const requestParams2 = builder2.build();

    expect(requestParams2.groups).toEqual([]);
  });

  test('should get groups for struct view', () => {
    const dataView = {
      id: 'view-id',
      type: 'STRUCT',
      meta: [
        { path: ['dad', 'name'], name: 'dad.name' },
        { path: ['dad', 'age'], name: 'dad.age' },
        { path: ['dad', 'address'], name: 'dad.address' },
      ],
    } as any;
    const chartDataConfigs = [
      {
        type: ChartDataSectionType.Group,
        key: 'aggregation',
      },
      {
        type: ChartDataSectionType.Group,
        key: 'aggregation',
        rows: [
          {
            colName: 'dad.name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
          },
        ],
      },
      {
        type: ChartDataSectionType.Color,
        key: 'aggregation',
        rows: [
          {
            colName: 'dad.age',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
          },
        ],
      },
      {
        type: ChartDataSectionType.Mixed,
        key: 'address',
        rows: [
          {
            colName: 'dad.address',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
          },
          {
            colName: 'dad.post',
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.Field as any,
          },
        ],
      },
    ];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.groups).toEqual([
      { column: ['dad', 'name'], alias: 'dad.name' },
      { column: ['dad', 'age'], alias: 'dad.age' },
      { column: ['dad', 'address'], alias: 'dad.address' },
    ]);

    const enableAggregation2 = false;

    const builder2 = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation2,
    );
    const requestParams2 = builder2.build();

    expect(requestParams2.groups).toEqual([]);
  });

  test('should get filters with meta info', () => {
    const dataView = {
      id: 'view-id',
      meta: [
        { path: ['name'], name: 'name' },
        { path: ['address'], name: 'address' },
        { path: ['family'], name: 'family' },
        { path: ['born'], name: 'born' },
        { path: ['birthday'], name: 'birthday' },
      ],
    } as any;
    const chartDataConfigs = [
      {
        type: ChartDataSectionType.Filter,
        key: 'filter1',
      },
      {
        type: ChartDataSectionType.Filter,
        key: 'filter2',
        rows: [
          {
            colName: 'name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
            aggregate: AggregateFieldActionType.None,
            filter: {
              condition: {
                name: 'filter-1',
                type: FilterConditionType.List,
                operator: FilterSqlOperator.In,
                visualType: 'STRING',
                value: ['a', 'b'],
              },
            },
          },
        ],
      },
      {
        type: ChartDataSectionType.Filter,
        key: 'filter3',
        rows: [
          {
            colName: 'name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
            aggregate: AggregateFieldActionType.Avg,
            filter: {
              condition: {
                name: 'filter-1',
                type: FilterConditionType.List,
                operator: FilterSqlOperator.In,
                visualType: 'STRING',
                value: ['a', 'b'],
              },
            },
          },
          {
            colName: 'name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
            filter: {
              condition: {
                name: 'name-not-null',
                type: FilterConditionType.Filter,
                operator: FilterSqlOperator.NotNull,
                visualType: 'STRING',
              },
            },
          },
          {
            colName: 'name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
            filter: {
              condition: {
                name: 'name-is-null',
                type: FilterConditionType.Filter,
                operator: FilterSqlOperator.Null,
                visualType: 'STRING',
              },
            },
          },
          {
            colName: 'address',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
            filter: {
              condition: {
                name: 'address-not-in',
                type: FilterConditionType.List,
                operator: FilterSqlOperator.NotIn,
                visualType: 'STRING',
                value: ['a', 'b'],
              },
            },
          },
          {
            colName: 'address',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
            filter: {
              condition: {
                name: 'address-in',
                type: FilterConditionType.List,
                operator: FilterSqlOperator.In,
                visualType: 'STRING',
              },
            },
          },
          {
            colName: 'family',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
            filter: {
              condition: {
                name: 'family-list',
                type: FilterConditionType.List,
                operator: FilterSqlOperator.In,
                visualType: 'STRING',
                value: [
                  { key: 'a', isSelected: true },
                  { key: 'b', isSelected: false },
                ],
              },
            },
          },
          {
            colName: 'born',
            type: DataViewFieldType.DATE,
            category: ChartDataViewFieldCategory.Field as any,
            filter: {
              condition: {
                name: 'born-date',
                type: FilterConditionType.Time,
                operator: FilterSqlOperator.In,
                visualType: 'DATE',
                value: ['2022-03-16'],
              },
            },
          },
          {
            colName: 'born',
            type: DataViewFieldType.DATE,
            category: ChartDataViewFieldCategory.Field as any,
            filter: {
              condition: {
                name: 'address-time',
                type: FilterConditionType.Time,
                operator: FilterSqlOperator.In,
                visualType: 'DATE',
                value: [{ unit: 'd', amount: 1, direction: '-' }],
              },
            },
          },
          {
            colName: 'born',
            type: DataViewFieldType.DATE,
            category: ChartDataViewFieldCategory.Field as any,
            filter: {
              condition: {
                name: 'born-recommend',
                type: FilterConditionType.RecommendTime,
                operator: FilterSqlOperator.In,
                visualType: 'DATE',
                value: RECOMMEND_TIME.TODAY,
              },
            },
          },
          {
            colName: 'birthday',
            type: DataViewFieldType.DATE,
            category: ChartDataViewFieldCategory.Field as any,
            filter: {
              condition: {
                name: 'born-recommend',
                type: FilterConditionType.RangeTime,
                operator: FilterSqlOperator.In,
                visualType: 'DATE',
                value: RECOMMEND_TIME.TODAY,
              },
            },
          },
          {
            colName: 'born',
            type: DataViewFieldType.DATE,
            category: ChartDataViewFieldCategory.Field as any,
            filter: {
              condition: {
                name: 'born-recommend',
                type: FilterConditionType.RecommendTime,
                operator: FilterSqlOperator.In,
                visualType: 'DATE',
                value: RECOMMEND_TIME.TODAY,
              },
            },
          },
        ],
      },
    ] as any;
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;
    const today = moment().format('YYYY-MM-DD');

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.filters).toEqual([
      {
        aggOperator: null,
        column: ['name'],
        sqlOperator: 'IN',
        values: [
          { value: 'a', valueType: 'STRING' },
          { value: 'b', valueType: 'STRING' },
        ],
      },
      {
        aggOperator: 'AVG',
        column: ['name'],
        sqlOperator: 'IN',
        values: [
          { value: 'a', valueType: 'STRING' },
          { value: 'b', valueType: 'STRING' },
        ],
      },
      {
        aggOperator: undefined,
        column: ['name'],
        sqlOperator: 'NOT_NULL',
        values: [],
      },
      {
        aggOperator: undefined,
        column: ['name'],
        sqlOperator: 'IS_NULL',
        values: [],
      },
      {
        aggOperator: undefined,
        column: ['address'],
        sqlOperator: 'NOT_IN',
        values: [
          { value: 'a', valueType: 'STRING' },
          { value: 'b', valueType: 'STRING' },
        ],
      },
      {
        aggOperator: undefined,
        column: ['family'],
        sqlOperator: 'IN',
        values: [{ value: 'a', valueType: 'STRING' }],
      },
      {
        aggOperator: undefined,
        column: ['born'],
        sqlOperator: 'IN',
        values: [{ value: '2022-03-16 00:00:00', valueType: 'DATE' }],
      },
      {
        aggOperator: undefined,
        column: ['born'],
        sqlOperator: 'IN',
        values: [{ value: `${today} 00:00:00`, valueType: 'DATE' }],
      },
      {
        aggOperator: undefined,
        column: ['born'],
        sqlOperator: 'IN',
        values: [
          { value: `${today} 00:00:00`, valueType: 'DATE' },
          { value: `${today} 23:59:59`, valueType: 'DATE' },
        ],
      },
      {
        aggOperator: undefined,
        column: ['birthday'],
        sqlOperator: 'IN',
        values: [{ value: 'Invalid date', valueType: 'DATE' }],
      },
      {
        aggOperator: undefined,
        column: ['born'],
        sqlOperator: 'IN',
        values: [
          { value: `${today} 00:00:00`, valueType: 'DATE' },
          { value: `${today} 23:59:59`, valueType: 'DATE' },
        ],
      },
    ]);
  });

  test('should get filters for struct view', () => {
    const dataView = {
      id: 'view-id',
      type: 'STRUCT',
      meta: [
        { path: ['dad', 'name'], name: 'dad.name' },
        { path: ['dad', 'age'], name: 'dad.age' },
        { path: ['dad', 'address'], name: 'dad.address' },
        { path: ['dad', 'family'], name: 'dad.family' },
        { path: ['dad', 'born'], name: 'dad.born' },
        { path: ['dad', 'birthday'], name: 'dad.birthday' },
      ],
    } as any;
    const chartDataConfigs = [
      {
        type: ChartDataSectionType.Filter,
        key: 'filter1',
      },
      {
        type: ChartDataSectionType.Filter,
        key: 'filter2',
        rows: [
          {
            colName: 'dad.name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
            aggregate: AggregateFieldActionType.None,
            filter: {
              condition: {
                name: 'filter-1',
                type: FilterConditionType.List,
                operator: FilterSqlOperator.In,
                visualType: 'STRING',
                value: ['a', 'b'],
              },
            },
          },
        ],
      },
      {
        type: ChartDataSectionType.Filter,
        key: 'filter3',
        rows: [
          {
            colName: 'dad.name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
            aggregate: AggregateFieldActionType.Avg,
            filter: {
              condition: {
                name: 'filter-1',
                type: FilterConditionType.List,
                operator: FilterSqlOperator.In,
                visualType: 'STRING',
                value: ['a', 'b'],
              },
            },
          },
          {
            colName: 'dad.name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
            filter: {
              condition: {
                name: 'name-not-null',
                type: FilterConditionType.Filter,
                operator: FilterSqlOperator.NotNull,
                visualType: 'STRING',
              },
            },
          },
          {
            colName: 'dad.name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
            filter: {
              condition: {
                name: 'name-is-null',
                type: FilterConditionType.Filter,
                operator: FilterSqlOperator.Null,
                visualType: 'STRING',
              },
            },
          },
          {
            colName: 'dad.address',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
            filter: {
              condition: {
                name: 'address-not-in',
                type: FilterConditionType.List,
                operator: FilterSqlOperator.NotIn,
                visualType: 'STRING',
                value: ['a', 'b'],
              },
            },
          },
          {
            colName: 'dad.address',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
            filter: {
              condition: {
                name: 'address-in',
                type: FilterConditionType.List,
                operator: FilterSqlOperator.In,
                visualType: 'STRING',
              },
            },
          },
          {
            colName: 'dad.family',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
            filter: {
              condition: {
                name: 'family-list',
                type: FilterConditionType.List,
                operator: FilterSqlOperator.In,
                visualType: 'STRING',
                value: [
                  { key: 'a', isSelected: true },
                  { key: 'b', isSelected: false },
                ],
              },
            },
          },
          {
            colName: 'dad.born',
            type: DataViewFieldType.DATE,
            category: ChartDataViewFieldCategory.Field as any,
            filter: {
              condition: {
                name: 'born-date',
                type: FilterConditionType.Time,
                operator: FilterSqlOperator.In,
                visualType: 'DATE',
                value: ['2022-03-16'],
              },
            },
          },
          {
            colName: 'dad.born',
            type: DataViewFieldType.DATE,
            category: ChartDataViewFieldCategory.Field as any,
            filter: {
              condition: {
                name: 'address-time',
                type: FilterConditionType.Time,
                operator: FilterSqlOperator.In,
                visualType: 'DATE',
                value: [{ unit: 'd', amount: 1, direction: '-' }],
              },
            },
          },
          {
            colName: 'dad.born',
            type: DataViewFieldType.DATE,
            category: ChartDataViewFieldCategory.Field as any,
            filter: {
              condition: {
                name: 'born-recommend',
                type: FilterConditionType.RecommendTime,
                operator: FilterSqlOperator.In,
                visualType: 'DATE',
                value: RECOMMEND_TIME.TODAY,
              },
            },
          },
          {
            colName: 'dad.birthday',
            type: DataViewFieldType.DATE,
            category: ChartDataViewFieldCategory.Field as any,
            filter: {
              condition: {
                name: 'born-recommend',
                type: FilterConditionType.RangeTime,
                operator: FilterSqlOperator.In,
                visualType: 'DATE',
                value: RECOMMEND_TIME.TODAY,
              },
            },
          },
          {
            colName: 'dad.born',
            type: DataViewFieldType.DATE,
            category: ChartDataViewFieldCategory.Field as any,
            filter: {
              condition: {
                name: 'born-recommend',
                type: FilterConditionType.RecommendTime,
                operator: FilterSqlOperator.In,
                visualType: 'DATE',
                value: RECOMMEND_TIME.TODAY,
              },
            },
          },
        ],
      },
    ] as any;
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;
    const today = moment().format('YYYY-MM-DD');

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.filters).toEqual([
      {
        aggOperator: null,
        column: ['dad', 'name'],
        sqlOperator: 'IN',
        values: [
          { value: 'a', valueType: 'STRING' },
          { value: 'b', valueType: 'STRING' },
        ],
      },
      {
        aggOperator: 'AVG',
        column: ['dad', 'name'],
        sqlOperator: 'IN',
        values: [
          { value: 'a', valueType: 'STRING' },
          { value: 'b', valueType: 'STRING' },
        ],
      },
      {
        aggOperator: undefined,
        column: ['dad', 'name'],
        sqlOperator: 'NOT_NULL',
        values: [],
      },
      {
        aggOperator: undefined,
        column: ['dad', 'name'],
        sqlOperator: 'IS_NULL',
        values: [],
      },
      {
        aggOperator: undefined,
        column: ['dad', 'address'],
        sqlOperator: 'NOT_IN',
        values: [
          { value: 'a', valueType: 'STRING' },
          { value: 'b', valueType: 'STRING' },
        ],
      },
      {
        aggOperator: undefined,
        column: ['dad', 'family'],
        sqlOperator: 'IN',
        values: [{ value: 'a', valueType: 'STRING' }],
      },
      {
        aggOperator: undefined,
        column: ['dad', 'born'],
        sqlOperator: 'IN',
        values: [{ value: '2022-03-16 00:00:00', valueType: 'DATE' }],
      },
      {
        aggOperator: undefined,
        column: ['dad', 'born'],
        sqlOperator: 'IN',
        values: [{ value: `${today} 00:00:00`, valueType: 'DATE' }],
      },
      {
        aggOperator: undefined,
        column: ['dad', 'born'],
        sqlOperator: 'IN',
        values: [
          { value: `${today} 00:00:00`, valueType: 'DATE' },
          { value: `${today} 23:59:59`, valueType: 'DATE' },
        ],
      },
      {
        aggOperator: undefined,
        column: ['dad', 'birthday'],
        sqlOperator: 'IN',
        values: [{ value: 'Invalid date', valueType: 'DATE' }],
      },
      {
        aggOperator: undefined,
        column: ['dad', 'born'],
        sqlOperator: 'IN',
        values: [
          { value: `${today} 00:00:00`, valueType: 'DATE' },
          { value: `${today} 23:59:59`, valueType: 'DATE' },
        ],
      },
    ]);
  });

  test('should get orders', () => {
    const dataView = { id: 'view-id' } as any;
    const chartDataConfigs = [
      {
        type: ChartDataSectionType.Aggregate,
        key: 'aggregationL',
      },
      {
        type: ChartDataSectionType.Aggregate,
        key: 'aggregationR',
        rows: [
          {
            colName: 'age',
            aggregate: AggregateFieldActionType.Avg,
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
            sort: {
              type: 'ASC',
            },
          },
        ],
      },
      {
        type: ChartDataSectionType.Group,
        key: 'group',
        rows: [
          {
            colName: 'first-name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
            sort: {
              type: 'ASC',
            },
          },
          {
            colName: 'last-name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.ComputedField as any,
            sort: {
              type: 'DESC',
            },
          },
          {
            colName: 'middle-name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.ComputedField as any,
            sort: {
              type: 'CUSTOMIZE',
            },
          },
        ],
      },
      {
        type: ChartDataSectionType.Mixed,
        key: 'info',
        rows: [
          {
            colName: 'address',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.ComputedField as any,
            sort: {
              type: 'DESC',
            },
          },
        ],
      },
    ] as any;
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.orders).toEqual([
      {
        column: ['age'],
        aggOperator: 'AVG',
        operator: 'ASC',
      },
      { column: ['first-name'], operator: 'ASC', aggOperator: undefined },
      { column: ['last-name'], operator: 'DESC', aggOperator: undefined },
      { column: ['address'], operator: 'DESC', aggOperator: undefined },
    ]);
  });

  test('should get orders for struct view', () => {
    const dataView = {
      id: 'view-id',
      type: 'STRUCT',
      meta: [
        { path: ['dad', 'age'], name: 'dad.age' },
        { path: ['dad', 'first-name'], name: 'dad.first-name' },
        { path: ['dad', 'last-name'], name: 'dad.last-name' },
        { path: ['address'], name: 'address' },
      ],
    } as any;
    const chartDataConfigs = [
      {
        type: ChartDataSectionType.Aggregate,
        key: 'aggregationL',
      },
      {
        type: ChartDataSectionType.Aggregate,
        key: 'aggregationR',
        rows: [
          {
            colName: 'dad.age',
            aggregate: AggregateFieldActionType.Avg,
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
            sort: {
              type: 'ASC',
            },
          },
        ],
      },
      {
        type: ChartDataSectionType.Group,
        key: 'group',
        rows: [
          {
            colName: 'dad.first-name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
            sort: {
              type: 'ASC',
            },
          },
          {
            colName: 'last-name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.ComputedField as any,
            sort: {
              type: 'DESC',
            },
          },
          {
            colName: 'middle-name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.ComputedField as any,
            sort: {
              type: 'CUSTOMIZE',
            },
          },
        ],
      },
      {
        type: ChartDataSectionType.Mixed,
        key: 'info',
        rows: [
          {
            colName: 'address',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.ComputedField as any,
            sort: {
              type: 'DESC',
            },
          },
        ],
      },
    ] as any;
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.orders).toEqual([
      {
        column: ['dad', 'age'],
        aggOperator: 'AVG',
        operator: 'ASC',
      },
      {
        column: ['dad', 'first-name'],
        operator: 'ASC',
        aggOperator: undefined,
      },
      {
        column: ['last-name'],
        operator: 'DESC',
        aggOperator: undefined,
      },
      { column: ['address'], operator: 'DESC', aggOperator: undefined },
    ]);
  });

  test('should get orders with unique extra sorters', () => {
    const dataView = {
      id: 'view-id',
      meta: [
        { path: ['fore-name'], name: 'fore-name' },
        { path: ['age'], name: 'age' },
      ],
    } as any;
    const chartDataConfigs = [
      {
        type: ChartDataSectionType.Aggregate,
        key: 'aggregationR',
        rows: [
          {
            colName: 'age',
            aggregate: AggregateFieldActionType.Avg,
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
            sort: {
              type: 'ASC',
            },
          },
        ],
      },
      {
        type: ChartDataSectionType.Group,
        key: 'group',
        rows: [
          {
            colName: 'first-name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field as any,
            sort: {
              type: 'ASC',
            },
          },
          {
            colName: 'last-name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.ComputedField as any,
            sort: {
              type: 'DESC',
            },
          },
          {
            colName: 'middle-name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.ComputedField as any,
            sort: {
              type: 'CUSTOMIZE',
            },
          },
        ],
      },
      {
        type: ChartDataSectionType.Mixed,
        key: 'info',
        rows: [
          {
            colName: 'address',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.ComputedField as any,
            sort: {
              type: 'DESC',
            },
          },
        ],
      },
    ] as any;
    const extraSorters = [
      {
        column: 'age',
        aggOperator: 'AVG',
        operator: 'ASC',
      },
      { column: 'fore-name', operator: 'ASC', aggOperator: undefined },
    ] as any;
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    builder.addExtraSorters(extraSorters);
    const requestParams = builder.build();

    expect(requestParams.orders).toEqual([
      {
        column: ['age'],
        aggOperator: 'AVG',
        operator: 'ASC',
      },
      { column: ['fore-name'], operator: 'ASC', aggOperator: undefined },
    ]);

    const extraSorters2: any = null;
    builder.addExtraSorters(extraSorters2);

    expect(requestParams.orders).toEqual([
      {
        column: ['age'],
        aggOperator: 'AVG',
        operator: 'ASC',
      },
      { column: ['fore-name'], operator: 'ASC', aggOperator: undefined },
    ]);
  });

  test('should get pageInfo from setting config', () => {
    const dataView = { id: 'view-id' } as any;
    const chartDataConfigs = [];
    const chartSettingConfigs = [
      {
        key: 'paging',
        rows: [
          {
            key: 'enablePaging',
            value: true,
          },
          {
            key: 'pageSize',
            value: 1024,
          },
        ],
      },
    ];
    const pageInfo = { pageNo: 1 };
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.pageInfo).toEqual({
      countTotal: true,
      pageNo: 1,
      pageSize: 1024,
    });
  });

  test('should computed functions', () => {
    const dataView = {
      id: 'view-id',
      computedFields: [
        { name: 'f1', expression: '[a' },
        { name: 'f2', expression: '[b]' },
        { name: 'f3', expression: '' },
      ],
    } as any;
    const chartDataConfigs = [
      {
        key: 'dimension',

        rows: [{ colName: 'f1' }, { colName: 'f2' }, { colName: 'f3' }],
      },
    ] as any;
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.functionColumns).toEqual([
      { alias: 'f1', snippet: '[a' },
      { alias: 'f2', snippet: '[b]' },
      { alias: 'f3', snippet: '' },
    ]);
  });

  test('should computed functions for struct view', () => {
    const dataView = {
      id: 'view-id',
      type: 'STRUCT',
      computedFields: [
        { name: 'f1', expression: '[dad].[a]' },
        { name: 'f2', expression: '[dad].[b]' },
        { name: 'f3', expression: '' },
      ],
    } as any;
    const chartDataConfigs = [
      {
        key: 'DATA',
        rows: [{ colName: 'f1' }, { colName: 'f2' }, { colName: 'f3' }],
      },
    ] as any;
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.functionColumns).toEqual([
      { alias: 'f1', snippet: '[dad].[a]' },
      { alias: 'f2', snippet: '[dad].[b]' },
      { alias: 'f3', snippet: '' },
    ]);
  });

  test('should get view config', () => {
    const viewConfig = {
      cache: false,
      cacheExpires: '',
      concurrencyControl: false,
      concurrencyControlMode: 'a',
    };
    const dataView = { config: JSON.stringify(viewConfig) } as any;
    const chartDataConfigs = [];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.cache).toEqual(viewConfig.cache);
    expect(requestParams.cacheExpires).toEqual(viewConfig.cacheExpires);
    expect(requestParams.concurrencyControl).toEqual(
      viewConfig.concurrencyControl,
    );
    expect(requestParams.concurrencyControlMode).toEqual(
      viewConfig.concurrencyControlMode,
    );
  });

  test('should get view config when config is a object', () => {
    const viewConfig = {
      cache: false,
      cacheExpires: '',
      concurrencyControl: false,
      concurrencyControlMode: 'a',
    };
    const dataView = {
      computedFields: [],
      id: '1',
      config: viewConfig,
    };
    const chartDataConfigs = [];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.cache).toEqual(viewConfig.cache);
    expect(requestParams.cacheExpires).toEqual(viewConfig.cacheExpires);
    expect(requestParams.concurrencyControl).toEqual(
      viewConfig.concurrencyControl,
    );
    expect(requestParams.concurrencyControlMode).toEqual(
      viewConfig.concurrencyControlMode,
    );
  });

  test('should get select columns', () => {
    const dataView = { id: 'view-id' } as any;
    const chartDataConfigs = [
      {
        type: ChartDataSectionType.Aggregate,
        key: 'aggregation',
        rows: [
          {
            colName: 'amount',
            type: '',
            category: '',
          },
        ],
      },
      {
        type: ChartDataSectionType.Size,
        key: 'size',
        rows: [
          {
            colName: 'total',
            type: '',
            category: '',
          },
        ],
      },
      {
        type: ChartDataSectionType.Info,
        key: 'info',
        rows: [
          {
            colName: 'sex',
            type: '',
            category: '',
          },
        ],
      },
      {
        type: ChartDataSectionType.Color,
        key: 'info',
        rows: [
          {
            colName: 'sex',
            type: '',
            category: '',
          },
        ],
      },
      {
        type: ChartDataSectionType.Group,
        key: 'GROUP',
        rows: [
          {
            colName: 'name',
            type: '',
            category: '',
          },
        ],
      },
      {
        type: ChartDataSectionType.Mixed,
        key: 'MIXED',
        rows: [
          {
            colName: 'name',
            type: '',
            category: '',
          },
        ],
      },
      {
        type: ChartDataSectionType.Filter,
        key: 'filter',
        rows: [
          {
            colName: 'filter',
            type: '',
            category: '',
          },
        ],
      },
    ] as any;
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = false;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.columns).toEqual([
      { alias: 'amount', column: ['amount'] },
      { alias: 'total', column: ['total'] },
      { alias: 'sex', column: ['sex'] },
      { alias: 'sex', column: ['sex'] },
      { alias: 'name', column: ['name'] },
      { alias: 'name', column: ['name'] },
    ]);
  });

  test('should get select columns for struct view', () => {
    const dataView = {
      id: 'view-id',
      type: 'STRUCT',
      meta: [
        { path: ['dad', 'amount'], name: 'dad.amount' },
        { path: ['dad', 'total'], name: 'dad.total' },
        { path: ['dad', 'sex'], name: 'dad.sex' },
        { path: ['dad', 'name'], name: 'dad.name' },
      ],
    } as any;
    const chartDataConfigs = [
      {
        type: ChartDataSectionType.Aggregate,
        key: 'aggregation',
        rows: [
          {
            colName: 'dad.amount',
            type: '',
            category: ChartDataViewFieldCategory.Field,
          },
        ],
      },
      {
        type: ChartDataSectionType.Size,
        key: 'size',
        rows: [
          {
            colName: 'dad.total',
            type: '',
            category: ChartDataViewFieldCategory.Field,
          },
        ],
      },
      {
        type: ChartDataSectionType.Info,
        key: 'info',
        rows: [
          {
            colName: 'dad.sex',
            type: '',
            category: ChartDataViewFieldCategory.Field,
          },
        ],
      },
      {
        type: ChartDataSectionType.Color,
        key: 'info',
        rows: [
          {
            colName: 'dad.sex',
            type: '',
            category: ChartDataViewFieldCategory.Field,
          },
        ],
      },
      {
        type: ChartDataSectionType.Group,
        key: 'GROUP',
        rows: [
          {
            colName: 'dad.name',
            type: '',
            category: ChartDataViewFieldCategory.Field,
          },
        ],
      },
      {
        type: ChartDataSectionType.Mixed,
        key: 'MIXED',
        rows: [
          {
            colName: 'dad.name',
            type: '',
            category: ChartDataViewFieldCategory.Field,
          },
        ],
      },
      {
        type: ChartDataSectionType.Filter,
        key: 'filter',
        rows: [
          {
            colName: 'dad.filter',
            type: '',
            category: ChartDataViewFieldCategory.Field,
          },
        ],
      },
    ] as any;
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = false;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.columns).toEqual([
      { alias: 'dad.amount', column: ['dad', 'amount'] },
      { alias: 'dad.total', column: ['dad', 'total'] },
      { alias: 'dad.sex', column: ['dad', 'sex'] },
      { alias: 'dad.sex', column: ['dad', 'sex'] },
      { alias: 'dad.name', column: ['dad', 'name'] },
      { alias: 'dad.name', column: ['dad', 'name'] },
    ]);
  });

  test('should get select columns with drill option', () => {
    const dataView = { id: 'view-id' } as any;
    const chartDataConfigs = [
      {
        type: ChartDataSectionType.Group,
        key: 'GROUP',
        drillable: true,
        rows: [
          {
            uid: 'group-r1',
            colName: 'group-r1',
            id: 'group-r1',
            type: '',
            category: '',
          },
          {
            uid: 'group-r2',
            colName: 'group-r2',
            id: 'group-r2',
            type: '',
            category: '',
          },
        ],
      },
      {
        type: ChartDataSectionType.Aggregate,
        key: 'aggregation',
        rows: [
          {
            colName: 'amount',
            id: 'amount',
            type: '',
            category: '',
          },
        ],
      },
      {
        type: ChartDataSectionType.Size,
        key: 'size',
        rows: [
          {
            colName: 'size',
            id: 'size',
            type: '',
            category: '',
          },
        ],
      },
      {
        type: ChartDataSectionType.Info,
        key: 'info',
        rows: [
          {
            colName: 'info',
            id: 'info',
            type: '',
            category: '',
          },
        ],
      },
      {
        type: ChartDataSectionType.Color,
        key: 'color',
        rows: [
          {
            colName: 'color',
            id: 'color',
            type: '',
            category: '',
          },
        ],
      },
      {
        type: ChartDataSectionType.Mixed,
        key: 'MIXED',
        rows: [
          {
            colName: 'mix',
            id: 'mix',
            type: '',
            category: '',
          },
        ],
      },
      {
        type: ChartDataSectionType.Filter,
        key: 'filter',
        rows: [
          {
            colName: 'filter',
            id: 'filter',
            type: '',
            category: '',
          },
        ],
      },
    ] as any;
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = false;
    const drillOption = getChartDrillOption(chartDataConfigs);
    drillOption?.drillDown();

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    ).addDrillOption(drillOption);
    const requestParams = builder.build();

    expect(requestParams.columns).toEqual([
      { alias: 'group-r2', column: ['group-r2'] },
      { alias: 'amount', column: ['amount'] },
      { alias: 'size', column: ['size'] },
      { alias: 'info', column: ['info'] },
      { alias: 'color', column: ['color'] },
      { alias: 'mix', column: ['mix'] },
    ]);
  });
});
