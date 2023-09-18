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
  ChartDataViewFieldCategory,
  ControllerFacadeTypes,
  DataViewFieldType,
  TimeFilterValueCategory,
} from 'app/constants';
import {
  DataChart,
  RelatedView,
} from 'app/pages/DashBoardPage/pages/Board/slice/types';
import {
  ControllerConfig,
  ControllerDate,
} from 'app/pages/DashBoardPage/pages/BoardEditor/components/ControllerWidgetPanel/types';
import { ChartDataConfig } from 'app/types/ChartConfig';
import ChartDataView from 'app/types/ChartDataView';
import { FilterSqlOperator } from 'globalConstants';
import moment from 'moment';
import {
  adaptBoardImageUrl,
  adjustRangeDataEndValue,
  convertImageUrl,
  dateFormatObj,
  fillPx,
  getBackgroundImage,
  getBoardChartRequests,
  getChartGroupColumns,
  getControllerDateValues,
  getDataChartRequestParams,
  getRGBAColor,
  getTheWidgetFiltersAndParams,
  getWidgetControlValues,
} from '..';
import { BOARD_FILE_IMG_PREFIX } from '../../constants';

const oldBoardId = 'xxxBoardIdXxx555';
const boardId = 'xxxBoardIdXxx666';
describe('dashboard.utils.index', () => {
  it(' should convertImageUrl get url', () => {
    const str1 = '';
    const str2 = 'http://www.qq.png';
    expect(convertImageUrl(str1)).toBe(str1);
    expect(convertImageUrl(str2)).toBe(str2);

    const str3 = BOARD_FILE_IMG_PREFIX + 'www.pan';
    const origin = window.location.origin;
    expect(convertImageUrl(str3)).toBe(`${origin}/${str3}`);
  });
  it('should getBackgroundImage ', () => {
    const str1 = '';
    const str2 = '123';
    expect(getBackgroundImage(str1)).toBe('none');
    expect(getBackgroundImage(str2)).toBe(`url(${convertImageUrl(str2)})`);
  });

  it('should adaptBoardImageUrl', () => {
    const str1 = `resources/image/dashboard/${oldBoardId}/fileID123`;
    const str2 = `resources/image/dashboard/${boardId}/fileID123`;
    const str3 = `resources/image/chart/343/fileID123`;
    const boardId2 = ``;
    expect(adaptBoardImageUrl(str1, boardId)).toBe(str2);
    expect(adaptBoardImageUrl(str3, boardId)).toBe(str3);
    expect(adaptBoardImageUrl(str1, boardId2)).toBe(str1);
  });

  it('should fillPx', () => {
    expect(fillPx(0)).toBe(0);
    expect(fillPx(2)).toBe('2px');
  });

  it('should getRGBAColor', () => {
    const val = `rgba(0, 0, 0, 1)`;
    expect(getRGBAColor('')).toBe(val);
    expect(getRGBAColor(null)).toBe(val);
    expect(getRGBAColor(undefined)).toBe(val);
    expect(getRGBAColor(0)).toBe(val);
    const color = {
      rgb: { r: 10, g: 111, b: 123, a: 0.3 },
    };
    const tColor = `rgba(10, 111, 123, 0.3)`;
    expect(getRGBAColor(color)).toBe(tColor);
    expect(getRGBAColor('#ccc')).toBe('#ccc');
  });
});
describe('should getDataChartRequestParams', () => {
  const opt = {
    pageInfo: {
      countTotal: true,
      pageNo: 2,
      pageSize: 100,
    },
  };
  const view = {
    config:
      '{"concurrencyControl":true,"concurrencyControlMode":"DIRTYREAD","cache":false,"cacheExpires":0}',
    createBy: '96f0c5013921456b9937ba528ba5b266',
    createTime: '2022-02-18 18:34:31',
    description: null,
    id: 'viewId123',
    index: 2,
    isFolder: false,
    model: '',
    name: 'aiqiyi',
    orgId: '90fa5a6c58fc45d9bf684e2177690d5b',
    parentId: null,
    permission: null,
    script: 'SELECT * from aiqiyi',
    sourceId: 'f7c09ac922ba4e75866c22fcfed5884d',
    status: 1,
    updateBy: null,
    updateTime: null,
    meta: [
      {
        type: 'NUMERIC',
        category: 'field',
        id: 'good_comment',
      },
      {
        type: 'STRING',
        category: 'field',
        id: 'director',
      },
      {
        type: 'STRING',
        category: 'field',
        id: 'actor',
        path: ['actor'],
      },
      {
        type: 'STRING',
        category: 'field',
        id: 'type',
      },
      {
        type: 'NUMERIC',
        category: 'field',
        id: 'total',
      },
      {
        type: 'NUMERIC',
        category: 'field',
        id: 'bad_commet',
      },
      {
        type: 'STRING',
        category: 'field',
        id: 'ID',
      },
      {
        type: 'NUMERIC',
        category: 'field',
        id: 'amt_play',
      },
      {
        type: 'STRING',
        category: 'field',
        id: 'release_year',
      },
      {
        type: 'NUMERIC',
        category: 'field',
        id: 'section',
      },
      {
        type: 'STRING',
        category: 'field',
        id: 'year',
      },
      {
        type: 'STRING',
        category: 'field',
        id: 'name',
      },
      {
        type: 'NUMERIC',
        category: 'field',
        id: 'num_score',
      },
      {
        type: 'STRING',
        category: 'field',
        id: 'release_time',
      },
      {
        type: 'STRING',
        category: 'field',
        id: 'area',
      },
      {
        type: 'STRING',
        category: 'field',
        id: 'status',
      },
      {
        type: 'STRING',
        category: 'field',
        id: 'month',
      },
      {
        type: 'NUMERIC',
        category: 'field',
        id: 'score',
      },
      {
        type: 'STRING',
        category: 'field',
        id: 'day',
      },
      {
        type: 'STRING',
        category: 'field',
        id: 'language',
      },
    ],
    computedFields: [],
  };
  view.meta = view.meta.map(v => {
    return {
      ...v,
      name: v.id,
      path: [v.id],
    };
  });
  it('should chart no filter', () => {
    let dataChart = {
      id: 'dataChartId123',
      name: 'mingxi-table',
      viewId: 'viewId123',
      orgId: '90fa5a6c58fc45d9bf684e2177690d5b',
      config: {
        chartConfig: {
          datas: [
            {
              actions: {
                NUMERIC: ['aggregate', 'alias', 'format', 'sortable'],
                STRING: ['alias', 'sortable'],
                DATE: ['alias', 'sortable'],
              },
              label: 'mixed',
              key: 'mixed',
              required: true,
              type: 'mixed',
              rows: [
                {
                  id: '665bc9f0-355c-442b-87be-efb39b0d47b5',
                  index: 0,
                  uid: '665bc9f0-355c-442b-87be-efb39b0d47b5',
                  colName: 'total',
                  category: 'field',
                  type: 'NUMERIC',
                  aggregate: 'SUM',
                },
                {
                  uid: 'e4a13a2f-3984-4eea-ba38-34b80b83c63c',
                  colName: 'actor',
                  category: 'field',
                  type: 'STRING',
                },
              ],
            },
            {
              label: 'filter',
              key: 'filter',
              type: 'filter',
              disableAggregate: true,
            },
          ],
          styles: [],
          settings: [
            {
              label: 'paging.title',
              key: 'paging',
              comType: 'group',
              rows: [
                {
                  label: 'paging.pageSize',
                  key: 'pageSize',
                  default: 100,
                  comType: 'inputNumber',
                  options: {
                    needRefresh: true,
                    step: 1,
                    min: 0,
                  },
                  watcher: {
                    deps: ['enablePaging'],
                  },
                  value: 100,
                },
              ],
            },
          ],
          i18ns: [],
        },
        chartGraphId: 'mingxi-table',
        computedFields: [],
        aggregation: true,
      },
      status: 1,
      description: '',
    };

    const viewConfig = JSON.parse(view.config);

    const res = getDataChartRequestParams({
      dataChart: dataChart as unknown as DataChart,
      view: view as unknown as ChartDataView,
      option: opt as any,
    });
    expect(res.viewId).toBe(dataChart.viewId);
    expect(res.filters).toEqual([]);
    expect(res.cache).toEqual(viewConfig.cache);
    expect(res.cacheExpires).toEqual(viewConfig.cacheExpires);
    expect(res.concurrencyControl).toEqual(viewConfig.concurrencyControl);
    expect(res.concurrencyControlMode).toEqual(
      viewConfig.concurrencyControlMode,
    );
  });
  it('should chart has filter', () => {
    const dataChart = {
      id: 'dataChartId123',
      name: 'mingxi-table',
      viewId: 'viewId123',
      orgId: '90fa5a6c58fc45d9bf684e2177690d5b',
      config: {
        chartConfig: {
          datas: [
            {
              label: 'mixed',
              key: 'mixed',
              required: true,
              type: 'mixed',
              rows: [
                {
                  id: '665bc9f0-355c-442b-87be-efb39b0d47b5',
                  index: 0,
                  uid: '665bc9f0-355c-442b-87be-efb39b0d47b5',
                  colName: 'total',
                  category: 'field',
                  type: 'NUMERIC',
                  aggregate: 'SUM',
                },
                {
                  uid: 'e4a13a2f-3984-4eea-ba38-34b80b83c63c',
                  colName: 'actor',
                  category: 'field',
                  type: 'STRING',
                },
              ],
            },
            {
              allowSameField: true,
              disableAggregate: true,
              actions: {
                NUMERIC: ['filter'],
                STRING: ['filter'],
                DATE: ['filter'],
              },
              label: 'filter',
              key: 'filter',
              type: 'filter',
              rows: [
                {
                  uid: 'fd58a4af-cc81-42c9-9255-fd9a7865f03e',
                  colName: 'actor',
                  category: 'field',
                  type: 'STRING',
                  aggregate: 'NONE',
                  filter: {
                    visibility: 'show',
                    condition: {
                      name: 'actor',
                      type: 2,
                      value: [
                        {
                          key: '丁勇岱',
                          label: '丁勇岱',
                          isSelected: true,
                        },
                      ],
                      visualType: 'STRING',
                      operator: 'IN',
                    },
                    facade: 'multiDropdownList',
                    width: 'auto',
                  },
                },
              ],
            },
          ],
          styles: [],
          settings: [],
          i18ns: [],
        },
        chartGraphId: 'mingxi-table',
        computedFields: [],
        aggregation: true,
      },
      status: 1,
      description: '',
    };
    const targetFilter = [
      {
        aggOperator: null,
        column: ['actor'],
        sqlOperator: 'IN',
        values: [
          {
            value: '丁勇岱',
            valueType: 'STRING',
          },
        ],
      },
    ];

    const res = getDataChartRequestParams({
      dataChart: dataChart as DataChart,
      view: view as unknown as ChartDataView,
      option: opt as any,
    });
    expect(res.viewId).toBe(dataChart.viewId);
    expect(res.filters).toEqual(targetFilter);
  });
});
describe('getChartGroupColumns', () => {
  it('should datas is undefined', () => {
    const datas = undefined;
    expect(getChartGroupColumns(datas)).toEqual([]);
  });
  it('should GROUP and COLOR', () => {
    const datas: ChartDataConfig[] = [
      {
        label: 'dimension',
        key: 'dimension',
        required: true,
        type: 'group',
        limit: 1,
        rows: [
          {
            uid: 'e1630ee5-ba5d-441d-9cd1-7e6bfac54781',
            colName: '城市',
            category: 'field',
            type: 'STRING',
          },
        ],
      },
      {
        label: 'metrics',
        key: 'metrics',
        required: true,
        type: 'aggregate',
        limit: [1, 999],
        rows: [
          {
            uid: '76696cb5-b286-4441-aa94-5023f616c790',
            colName: 'GDP（亿元）',
            category: 'field',
            type: 'NUMERIC',
            aggregate: 'SUM',
          },
        ],
      },
      {
        label: 'filter',
        key: 'filter',
        type: 'filter',
        allowSameField: true,
      },
      {
        actions: {
          STRING: ['alias', 'colorize'],
        },
        label: 'colorize',
        key: 'color',
        type: 'color',
        limit: [0, 1],
        rows: [
          {
            uid: '2d6a8030-2e66-468f-829f-a0bda470c051',
            colName: '地区',
            category: 'field',
            type: 'STRING',
          },
        ],
      },
      {
        label: 'info',
        key: 'info',
        type: 'info',
      },
    ] as ChartDataConfig[];
    const columns = [
      {
        uid: 'e1630ee5-ba5d-441d-9cd1-7e6bfac54781',
        colName: '城市',
        category: 'field',
        type: 'STRING',
      },
      {
        uid: '2d6a8030-2e66-468f-829f-a0bda470c051',
        colName: '地区',
        category: 'field',
        type: 'STRING',
      },
    ];
    expect(getChartGroupColumns(datas)).toEqual(columns);
  });

  it('should mixed', () => {
    const datas: ChartDataConfig[] = [
      {
        label: 'mixed',
        key: 'mixed',
        required: true,
        type: 'mixed',
        rows: [
          {
            uid: 'a2b004cb-dc90-4766-bdb7-d31817f87549',
            colName: 'name_level2',
            category: 'field',
            type: 'STRING',
          },
          {
            uid: '8d5d6af1-22a1-46b8-b64d-9030bc54a59d',
            colName: 'name_level1',
            category: 'field',
            type: 'STRING',
          },
          {
            uid: 'dec8e9de-1dc2-4d7f-b76a-068a081909dd',
            colName: '总访问次数',
            category: 'field',
            type: 'NUMERIC',
            aggregate: 'SUM',
          },
        ],
      },
    ] as ChartDataConfig[];
    const columns = [
      {
        uid: 'a2b004cb-dc90-4766-bdb7-d31817f87549',
        colName: 'name_level2',
        category: 'field',
        type: 'STRING',
      },
      {
        uid: '8d5d6af1-22a1-46b8-b64d-9030bc54a59d',
        colName: 'name_level1',
        category: 'field',
        type: 'STRING',
      },
    ];
    expect(getChartGroupColumns(datas)).toEqual(columns);
  });
});
describe('getTheWidgetFiltersAndParams', () => {
  it.skip('should has Params', () => {
    const obj = {
      chartWidget: {
        config: {
          version: '1.0.0-beta.2',
          type: 'chart',
          index: 1,
          name: '私有图表_1',
          linkageConfig: {
            open: false,
            chartGroupColumns: [],
          },
          autoUpdate: false,
          jumpConfig: {
            targetType: 'INTERNAL',
            target: {
              id: '171e52cec2f842588596ce8c55c139be',
              relId: '88ca31a509fb42f8bdb4728b2a9a6e0b',
              relType: 'DASHBOARD',
            },
            filter: {
              filterId: 'a53182d9f3a04f20baee2c933164d493',
              filterLabel: 'dee ',
            },
            field: {
              jumpFieldName: '城市',
            },
            open: true,
          },
        },

        dashboardId: '6a1bded77a424fb3abb18c84a6af2d7b',
        datachartId:
          'widget_6a1bded77a424fb3abb18c84a6af2d7b_a8c7191e276d41cba67743041c650ee3',
        id: 'a8c7191e276d41cba67743041c650ee3',
        parentId: '',
        relations: [],
        viewIds: ['3ca2a12f09c84c8ca1a5714fc6fa44d8'],
      },
      widgetMap: {
        '60540b62ff7c4c61b229b977a2485d9e': {
          config: {
            version: '1.0.0-beta.2',
            type: 'controller',
            name: 'slider',
            linkageConfig: {
              open: false,
              chartGroupColumns: [],
            },

            content: {
              type: 'dropdownList',
              relatedViews: [
                {
                  viewId: '3ca2a12f09c84c8ca1a5714fc6fa44d8',
                  relatedCategory: 'variable',
                  fieldValue: 'area1',
                  fieldValueType: 'FRAGMENT',
                },
              ],
              name: 'slider',
              config: {
                controllerValues: ['山东'],
                valueOptions: [],
                valueOptionType: 'common',
                assistViewFields: [
                  '3ca2a12f09c84c8ca1a5714fc6fa44d8',
                  '地区',
                  '地区',
                ],
                sqlOperator: 'NE',
                visibility: {
                  visibilityType: 'show',
                },
              },
            },
          },

          dashboardId: '6a1bded77a424fb3abb18c84a6af2d7b',
          datachartId: null,
          id: '60540b62ff7c4c61b229b977a2485d9e',
          parentId: '',
          permission: null,
          relations: [
            {
              config: {
                type: 'controlToWidget',
                controlToWidget: {
                  widgetRelatedViewIds: ['3ca2a12f09c84c8ca1a5714fc6fa44d8'],
                },
              },
              id: '9f6124b9bee048058728931b7671536d',
              permission: null,
              sourceId: '60540b62ff7c4c61b229b977a2485d9e',
              targetId: 'a8c7191e276d41cba67743041c650ee3',
            },
          ],
          viewIds: ['3ca2a12f09c84c8ca1a5714fc6fa44d8'],
        },
        '8c4c4262912f49c2a02da7057c9ca730': {
          config: {
            version: '1.0.0-beta.2',
            type: 'controller',
            index: 10,
            name: '日期',
            linkageConfig: {
              open: false,
              chartGroupColumns: [],
            },

            content: {
              type: 'time',
              relatedViews: [
                {
                  viewId: '3ca2a12f09c84c8ca1a5714fc6fa44d8',
                  relatedCategory: 'field',
                  fieldValue: '日期',
                  fieldValueType: 'DATE',
                },
              ],
              name: '日期',
              config: {
                controllerValues: [],
                valueOptions: [],
                controllerDate: {
                  pickerType: 'date',
                  startTime: {
                    relativeOrExact: 'exact',
                    exactValue: '2022-03-01 00:00:00',
                  },
                },
                sqlOperator: 'NE',
                visibility: {
                  visibilityType: 'show',
                },
              },
            },
          },

          dashboardId: '6a1bded77a424fb3abb18c84a6af2d7b',
          datachartId: null,
          id: '8c4c4262912f49c2a02da7057c9ca730',
          parentId: '',
          permission: null,
          relations: [
            {
              config: {
                type: 'controlToWidget',
                controlToWidget: {
                  widgetRelatedViewIds: ['3ca2a12f09c84c8ca1a5714fc6fa44d8'],
                },
              },
              createBy: null,
              createTime: null,
              id: '065903d542ac4f7ca3270f07d8515265',
              permission: null,
              sourceId: '8c4c4262912f49c2a02da7057c9ca730',
              targetId: 'a8c7191e276d41cba67743041c650ee3',
              updateBy: null,
              updateTime: null,
            },
          ],
          updateBy: '96f0c5013921456b9937ba528ba5b266',
          updateTime: '2022-03-19 10:55:34',
          viewIds: [],
        },
        a8c7191e276d41cba67743041c650ee3: {
          config: {
            version: '1.0.0-beta.2',
            type: 'chart',
            index: 1,
            name: '私有图表_1',
            linkageConfig: {
              open: false,
              chartGroupColumns: [],
            },

            content: {
              type: 'widgetChart',
            },

            jumpConfig: {
              targetType: 'INTERNAL',
              target: {
                id: '171e52cec2f842588596ce8c55c139be',
                relId: '88ca31a509fb42f8bdb4728b2a9a6e0b',
                relType: 'DASHBOARD',
              },
              filter: {
                filterId: 'a53182d9f3a04f20baee2c933164d493',
                filterLabel: 'dee ',
              },
              field: {
                jumpFieldName: '城市',
              },
              open: true,
            },
          },
          dashboardId: '6a1bded77a424fb3abb18c84a6af2d7b',
          datachartId:
            'widget_6a1bded77a424fb3abb18c84a6af2d7b_a8c7191e276d41cba67743041c650ee3',
          id: 'a8c7191e276d41cba67743041c650ee3',
          parentId: '',
          permission: null,
          relations: [],
          viewIds: ['3ca2a12f09c84c8ca1a5714fc6fa44d8'],
        },
      },
    };
    const res = {
      filterParams: [
        {
          aggOperator: null,
          column: ['日期'],
          sqlOperator: 'NE',
          values: [
            {
              value: '2022-03-01 00:00:00',
              valueType: 'DATE',
            },
          ],
        },
      ],
      variableParams: {
        area1: ['山东'],
      },
    };
    expect(getTheWidgetFiltersAndParams(obj as any)).toEqual(res);
  });

  it.skip('should no Params ', () => {
    const obj = {
      chartWidget: {
        config: {
          version: '1.0.0-beta.2',
          type: 'chart',
          name: '私有图表_5',
          linkageConfig: {
            open: false,
            chartGroupColumns: [],
          },

          content: {
            type: 'widgetChart',
          },
        },
        createBy: '96f0c5013921456b9937ba528ba5b266',
        createTime: '2022-03-18 15:57:44',
        dashboardId: 'b3f707ebc67643fb8e56c853f302d836',
        datachartId:
          'widget_b3f707ebc67643fb8e56c853f302d836_8ad4845fe5724acfa27a8e910da90fb4',
        id: '8ad4845fe5724acfa27a8e910da90fb4',
        parentId: '',
        permission: null,
        relations: [],
        updateBy: '96f0c5013921456b9937ba528ba5b266',
        updateTime: '2022-03-19 11:00:12',
        viewIds: ['64f14c71b487424eb165f0304e77a28e'],
      },
      widgetMap: {
        '3af65d6dc4f14150851db50ef39a52b7': {
          config: {
            version: '1.0.0-beta.2',
            type: 'controller',
            name: '12',
            linkageConfig: {
              open: false,
              chartGroupColumns: [],
            },

            content: {
              type: 'dropdownList',
              relatedViews: [
                {
                  viewId: '64f14c71b487424eb165f0304e77a28e',
                  relatedCategory: 'field',
                  fieldValue: 'name_level1',
                  fieldValueType: 'STRING',
                },
              ],
              name: '12',
              config: {
                controllerValues: ['线上渠道'],
                valueOptions: [],
                valueOptionType: 'common',
                assistViewFields: [
                  '64f14c71b487424eb165f0304e77a28e',
                  'name_level1',
                  'name_level1',
                ],
                sqlOperator: 'NE',
                visibility: {
                  visibilityType: 'show',
                },
              },
            },
          },
          dashboardId: 'b3f707ebc67643fb8e56c853f302d836',
          datachartId: null,
          id: '3af65d6dc4f14150851db50ef39a52b7',
          parentId: '',
          permission: null,
          relations: [
            {
              config: {
                type: 'controlToWidget',
                controlToWidget: {
                  widgetRelatedViewIds: ['64f14c71b487424eb165f0304e77a28e'],
                },
              },

              id: '24b55e185a864691ac76a70688f757eb',
              permission: null,
              sourceId: '3af65d6dc4f14150851db50ef39a52b7',
              targetId: '8ad4845fe5724acfa27a8e910da90fb4',
            },
          ],
          viewIds: ['64f14c71b487424eb165f0304e77a28e'],
        },
        '8ad4845fe5724acfa27a8e910da90fb4': {
          config: {
            version: '1.0.0-beta.2',
            type: 'chart',
            index: 5,
            name: '私有图表_5',
            linkageConfig: {
              open: false,
              chartGroupColumns: [],
            },
            content: {
              type: 'widgetChart',
            },
          },
          dashboardId: 'b3f707ebc67643fb8e56c853f302d836',
          datachartId:
            'widget_b3f707ebc67643fb8e56c853f302d836_8ad4845fe5724acfa27a8e910da90fb4',
          id: '8ad4845fe5724acfa27a8e910da90fb4',
          parentId: '',
          permission: null,
          relations: [],
          viewIds: ['64f14c71b487424eb165f0304e77a28e'],
        },
      },
    };

    const res = {
      filterParams: [
        {
          aggOperator: null,
          column: ['name_level1'],
          sqlOperator: 'NE',
          values: [
            {
              value: '线上渠道',
              valueType: 'STRING',
            },
          ],
        },
      ],
      variableParams: {},
    };
    expect(getTheWidgetFiltersAndParams(obj as any)).toEqual(res);
  });
});
describe('getWidgetControlValues', () => {
  test('control DropdownList value', () => {
    const type = ControllerFacadeTypes.DropdownList;
    const relatedViewItem: RelatedView = {
      viewId: '3ca2a12f09c84c8ca1a5714fc6fa44d8',
      relatedCategory: ChartDataViewFieldCategory.Field,
      fieldValue: '地区',
      fieldValueType: DataViewFieldType.STRING,
    };
    const config: ControllerConfig = {
      required: false,
      controllerValues: ['山东'],
      valueOptions: [],
      valueOptionType: 'common',
      assistViewFields: ['3ca2a12f09c84c8ca1a5714fc6fa44d8', '地区', '地区'],
      sqlOperator: FilterSqlOperator.Equal,
      visibility: {
        visibilityType: 'show',
      },
    };
    const opt = { type, relatedViewItem, config };
    const res = [
      {
        value: '山东',
        valueType: 'STRING',
      },
    ];
    expect(getWidgetControlValues(opt)).toEqual(res);
  });
  test('control rangeValue value', () => {
    const type = ControllerFacadeTypes.RangeValue;
    const relatedViewItem: RelatedView = {
      viewId: '3ca2a12f09c84c8ca1a5714fc6fa44d8',
      relatedCategory: ChartDataViewFieldCategory.Field,
      fieldValue: 'GDP第一产业（亿元）',
      fieldValueType: DataViewFieldType.NUMERIC,
    };

    const config: ControllerConfig = {
      required: false,
      controllerValues: [1, 10000],
      valueOptions: [],
      valueOptionType: 'common',
      assistViewFields: ['3ca2a12f09c84c8ca1a5714fc6fa44d8', '地区', '地区'],
      sqlOperator: FilterSqlOperator.Between,
      visibility: {
        visibilityType: 'show',
      },
    };

    const opt = { type, relatedViewItem, config };
    const res = [
      {
        value: 1,
        valueType: 'NUMERIC',
      },
      {
        value: 10000,
        valueType: 'NUMERIC',
      },
    ];
    expect(getWidgetControlValues(opt)).toEqual(res);
  });
  test('control RangeTime value', () => {
    const type = ControllerFacadeTypes.RangeTime;
    const relatedViewItem: RelatedView = {
      viewId: '3ca2a12f09c84c8ca1a5714fc6fa44d8',
      relatedCategory: ChartDataViewFieldCategory.Field,
      fieldValue: '日期',
      fieldValueType: DataViewFieldType.DATE,
    };

    const config: ControllerConfig = {
      required: false,
      controllerValues: [],
      valueOptions: [],
      controllerDate: {
        pickerType: 'date',
        startTime: {
          relativeOrExact: TimeFilterValueCategory.Exact,
          exactValue: '2021-09-01 00:00:00',
        },
        endTime: {
          relativeOrExact: TimeFilterValueCategory.Exact,
          exactValue: '2022-01-15 00:00:00',
        },
      },
      assistViewFields: [],
      valueOptionType: 'common',
      sqlOperator: FilterSqlOperator.Between,
      visibility: {
        visibilityType: 'show',
      },
    };

    const opt = { type, relatedViewItem, config };
    const res = [
      {
        value: '2021-09-01',
        valueType: 'DATE',
      },
      {
        value: '2022-01-16',
        valueType: 'DATE',
      },
    ];
    expect(getWidgetControlValues(opt)).toEqual(res);
  });
});
describe('getControllerDateValues', () => {
  interface Params {
    controlType: ControllerFacadeTypes;
    filterDate: ControllerDate;
    execute?: boolean;
  }
  it('should startTime.relativeOrExact Exact', () => {
    const obj = {
      mmm: '2',
      controlType: 'time',
      filterDate: {
        pickerType: 'date',
        startTime: {
          relativeOrExact: 'exact',
          exactValue: '2022-03-01 00:00:00',
        },
      },
    } as Params;
    const res = ['2022-03-01 00:00:00', ''];
    expect(getControllerDateValues(obj)).toEqual(res);
  });
  it('should startTime.relativeOrExact relative', () => {
    const obj = {
      controlType: 'time',
      filterDate: {
        pickerType: 'date',
        startTime: {
          relativeOrExact: 'relative',
          relativeValue: {
            amount: 1,
            unit: 'd',
            direction: '-',
          },
        },
      },
      execute: true,
    } as Params;
    const time = moment()
      .add('-1', 'd')
      .startOf('d')
      .format(dateFormatObj[obj.filterDate.pickerType]);
    const res = [time, ''];
    expect(getControllerDateValues(obj)).toEqual(res);
  });
  it('should endTime.relativeOrExact Exact and relative ', () => {
    // no execute
    const obj = {
      controlType: 'rangeTime',
      filterDate: {
        pickerType: 'date',
        startTime: {
          relativeOrExact: 'exact',
          exactValue: '2022-03-01 00:00:00',
        },
        endTime: {
          relativeOrExact: 'exact',
          exactValue: '2022-03-18 00:00:00',
          relativeValue: {
            amount: 1,
            unit: 'd',
            direction: '-',
          },
        },
      },
    } as Params;
    const res = ['2022-03-01 00:00:00', '2022-03-18 00:00:00'];
    expect(getControllerDateValues(obj)).toEqual(res);
    // has execute
    const obj2 = {
      controlType: 'rangeTime',
      filterDate: {
        pickerType: 'date',
        startTime: {
          relativeOrExact: 'exact',
          exactValue: '2022-03-01 00:00:00',
        },
        endTime: {
          relativeOrExact: 'relative',
          exactValue: '2022-03-20 00:00:00',
          relativeValue: {
            amount: 1,
            unit: 'd',
            direction: '-',
          },
        },
      },
      execute: true,
    } as Params;
    const time = moment()
      .add('-1', 'd')
      .add(1, 'd')
      .startOf('d')
      .format(dateFormatObj[obj2.filterDate.pickerType]);
    const res2 = ['2022-03-01', time];

    expect(getControllerDateValues(obj2)).toEqual(res2);
  });
});

describe('adjustRangeDataEndValue', () => {
  it('should timeValue is null', () => {
    expect(adjustRangeDataEndValue('date', '')).toEqual('');
  });
  it('should pickerType=dateTime', () => {
    expect(adjustRangeDataEndValue('dateTime', '2022-03-01 01:01:23')).toEqual(
      '2022-03-01 01:01:23',
    );
  });
  it('should pickerType=date', () => {
    expect(adjustRangeDataEndValue('date', '2022-03-01 01:01:23')).toEqual(
      '2022-03-02 00:00:00',
    );
  });
  it('should pickerType=month', () => {
    expect(adjustRangeDataEndValue('month', '2022-03-01 01:01:23')).toEqual(
      '2022-04-01 00:00:00',
    );
  });
  it('should pickerType=quarter', () => {
    expect(adjustRangeDataEndValue('quarter', '2022-03-01 01:01:23')).toEqual(
      '2022-04-01 00:00:00',
    );
    expect(adjustRangeDataEndValue('quarter', '2022-04-01 01:01:23')).toEqual(
      '2022-07-01 00:00:00',
    );
  });
  it('should pickerType=week', () => {
    expect(adjustRangeDataEndValue('week', '2022-03-28 00:00:00')).toEqual(
      '2022-04-03 00:00:00',
    );
  });
  it('should pickerType=year', () => {
    expect(adjustRangeDataEndValue('year', '2022-03-28 00:00:00')).toEqual(
      '2023-01-01 00:00:00',
    );
  });
});

describe('getBoardChartRequests', () => {
  it.skip('should timeValue is null', () => {
    const obj = {
      widgetMap: {
        '24f59d7da2e84687a2a3fb465a10f819': {
          config: {
            version: '1.0.0-beta.2',
            type: 'chart',
            content: {
              type: 'dataChart',
            },
          },

          dashboardId: '6a1bded77a424fb3abb18c84a6af2d7b',
          datachartId: '2421674d25f740809433ace249506624',
          id: '24f59d7da2e84687a2a3fb465a10f819',
          relations: [],
          viewIds: ['836614b7c86042cdbd38fc40da270846'],
        },
        '26b234e597fe475daeb7dc29b7bfe455': {
          config: {
            version: '1.0.0-beta.2',
            type: 'controller',
            content: {
              type: 'checkboxGroup',
              relatedViews: [
                {
                  viewId: '836614b7c86042cdbd38fc40da270846',
                  relatedCategory: 'field',
                  fieldValue: 'name_level1',
                  fieldValueType: 'STRING',
                },
              ],
              name: 'ww',
              config: {
                controllerValues: ['线上渠道', '新媒体营销'],
                valueOptions: [],
                valueOptionType: 'common',
                assistViewFields: [
                  '64f14c71b487424eb165f0304e77a28e',
                  'name_level1',
                  'name_level1',
                ],
                sqlOperator: 'IN',
                visibility: {
                  visibilityType: 'show',
                },
              },
            },
          },
          dashboardId: '6a1bded77a424fb3abb18c84a6af2d7b',
          datachartId: null,
          id: '26b234e597fe475daeb7dc29b7bfe455',
          relations: [
            {
              config: {
                type: 'controlToWidget',
                controlToWidget: {
                  widgetRelatedViewIds: ['836614b7c86042cdbd38fc40da270846'],
                },
              },
              createBy: null,
              createTime: null,
              id: '1e301113262940a7805b2c99e4d9266b',
              permission: null,
              sourceId: '26b234e597fe475daeb7dc29b7bfe455',
              targetId: '24f59d7da2e84687a2a3fb465a10f819',
              updateBy: null,
              updateTime: null,
            },
          ],
          updateBy: '96f0c5013921456b9937ba528ba5b266',
          updateTime: '2022-03-19 14:34:25',
          viewIds: ['64f14c71b487424eb165f0304e77a28e'],
        },
        '60540b62ff7c4c61b229b977a2485d9e': {
          config: {
            version: '1.0.0-beta.2',
            type: 'controller',
            name: 'slider',
            content: {
              type: 'dropdownList',
              relatedViews: [
                {
                  viewId: '3ca2a12f09c84c8ca1a5714fc6fa44d8',
                  relatedCategory: 'variable',
                  fieldValue: 'area1',
                  fieldValueType: 'FRAGMENT',
                },
              ],
              name: 'slider',
              config: {
                controllerValues: ['山东'],
                valueOptions: [],
                valueOptionType: 'common',
                assistViewFields: [
                  '3ca2a12f09c84c8ca1a5714fc6fa44d8',
                  '地区',
                  '地区',
                ],
                sqlOperator: 'NE',
                visibility: {
                  visibilityType: 'show',
                },
              },
            },
          },
          dashboardId: '6a1bded77a424fb3abb18c84a6af2d7b',
          datachartId: null,
          id: '60540b62ff7c4c61b229b977a2485d9e',
          relations: [
            {
              config: {
                type: 'controlToWidget',
                controlToWidget: {
                  widgetRelatedViewIds: ['3ca2a12f09c84c8ca1a5714fc6fa44d8'],
                },
              },
              createBy: null,
              createTime: null,
              id: 'bda3b1ad14ac4e26adb5cdd0ae1df4f8',
              permission: null,
              sourceId: '60540b62ff7c4c61b229b977a2485d9e',
              targetId: 'a8c7191e276d41cba67743041c650ee3',
              updateBy: null,
              updateTime: null,
            },
          ],
          updateBy: '96f0c5013921456b9937ba528ba5b266',
          updateTime: '2022-03-19 14:34:25',
          viewIds: ['3ca2a12f09c84c8ca1a5714fc6fa44d8'],
        },
        a8c7191e276d41cba67743041c650ee3: {
          config: {
            version: '1.0.0-beta.2',
            type: 'chart',
            name: '私有图表_1',
            content: {
              type: 'widgetChart',
            },

            jumpConfig: {
              targetType: 'INTERNAL',
              target: {
                id: '171e52cec2f842588596ce8c55c139be',
                relId: '88ca31a509fb42f8bdb4728b2a9a6e0b',
                relType: 'DASHBOARD',
              },
              filter: {
                filterId: 'a53182d9f3a04f20baee2c933164d493',
                filterLabel: 'dee ',
              },
              field: {
                jumpFieldName: '城市',
              },
              open: true,
            },
          },

          dashboardId: '6a1bded77a424fb3abb18c84a6af2d7b',
          datachartId:
            'widget_6a1bded77a424fb3abb18c84a6af2d7b_a8c7191e276d41cba67743041c650ee3',
          id: 'a8c7191e276d41cba67743041c650ee3',
          relations: [],
          viewIds: ['3ca2a12f09c84c8ca1a5714fc6fa44d8'],
        },
      },
      viewMap: {
        '3ca2a12f09c84c8ca1a5714fc6fa44d8': {
          config:
            '{"concurrencyControl":true,"concurrencyControlMode":"DIRTYREAD","cache":false,"cacheExpires":0}',

          id: '3ca2a12f09c84c8ca1a5714fc6fa44d8',
          index: null,
          isFolder: false,
          model: '',
          name: 'gdp-view',
          orgId: '90fa5a6c58fc45d9bf684e2177690d5b',
          parentId: null,
          permission: null,
          script: 'SELECT * FROM GDP WHERE 地区 IN ($gdp_area1$)',
          sourceId: 'c65e293daae04233abe7d254d730fa22',
          status: 1,
          meta: [
            {
              type: 'NUMERIC',
              category: 'field',
              id: 'GDP（亿元）',
            },
            {
              type: 'DATE',
              category: 'field',
              id: '日期',
            },
            {
              type: 'NUMERIC',
              category: 'field',
              id: 'GDP第一产业（亿元）',
            },
            {
              type: 'NUMERIC',
              category: 'field',
              id: 'GDP第三产业（亿元）',
            },
            {
              type: 'STRING',
              category: 'field',
              id: '地区',
            },
            {
              type: 'STRING',
              category: 'field',
              id: '年份',
            },
            {
              type: 'NUMERIC',
              category: 'field',
              id: '人均GDP（元）',
            },
            {
              type: 'NUMERIC',
              category: 'field',
              id: 'GDP指数（上年=100）',
            },
            {
              type: 'STRING',
              category: 'field',
              id: '城市',
            },
            {
              type: 'NUMERIC',
              category: 'field',
              id: 'GDP第二产业（亿元）',
            },
          ],
          computedFields: [],
        },
        '836614b7c86042cdbd38fc40da270846': {
          config:
            '{"concurrencyControl":true,"concurrencyControlMode":"DIRTYREAD","cache":false,"cacheExpires":0}',
          createBy: '96f0c5013921456b9937ba528ba5b266',
          createTime: '2021-11-08 11:20:29',
          description: null,
          id: '836614b7c86042cdbd38fc40da270846',
          index: null,
          isFolder: false,
          model: '',
          name: '渠道view2',
          orgId: '90fa5a6c58fc45d9bf684e2177690d5b',
          parentId: null,
          permission: null,
          script: 'SELECT * FROM dad ',
          sourceId: 'c65e293daae04233abe7d254d730fa22',
          status: 1,
          updateBy: '96f0c5013921456b9937ba528ba5b266',
          updateTime: '2022-01-19 14:19:23',
          meta: [
            {
              type: 'STRING',
              category: 'field',
              id: 'name_level1',
            },
            {
              type: 'NUMERIC',
              category: 'field',
              id: '总停留时间',
            },
            {
              type: 'STRING',
              category: 'field',
              id: 'name_level2',
            },
            {
              type: 'NUMERIC',
              category: 'field',
              id: '总调出次数',
            },
            {
              type: 'STRING',
              category: 'field',
              id: 'name_level3',
            },
            {
              type: 'STRING',
              category: 'field',
              id: 'platform',
            },
            {
              type: 'NUMERIC',
              category: 'field',
              id: '总访问次数',
            },
            {
              type: 'STRING',
              category: 'field',
              id: 'QD_id',
            },
            {
              type: 'NUMERIC',
              category: 'field',
              id: '总页数',
            },
          ],
          computedFields: [],
        },
        '64f14c71b487424eb165f0304e77a28e': {
          config:
            '{"expensiveQuery":true,"concurrencyControl":true,"concurrencyControlMode":"DIRTYREAD","cache":false,"cacheExpires":0}',
          createBy: '96f0c5013921456b9937ba528ba5b266',
          createTime: '2021-10-18 20:14:15',
          description: null,
          id: '64f14c71b487424eb165f0304e77a28e',
          index: null,
          isFolder: false,
          model: '',
          name: '渠道数据',
          orgId: '90fa5a6c58fc45d9bf684e2177690d5b',
          parentId: null,
          permission: null,
          script: 'SELECT * FROM dad',
          sourceId: 'c65e293daae04233abe7d254d730fa22',
          status: 1,
          updateBy: '96f0c5013921456b9937ba528ba5b266',
          updateTime: '2022-03-01 10:00:59',
          meta: [
            {
              type: 'STRING',
              category: 'field',
              id: 'name_level1',
            },
            {
              type: 'NUMERIC',
              category: 'field',
              id: '总停留时间',
            },
            {
              type: 'STRING',
              category: 'field',
              id: 'name_level2',
            },
            {
              type: 'NUMERIC',
              category: 'field',
              id: '总调出次数',
            },
            {
              type: 'STRING',
              category: 'field',
              id: 'name_level3',
            },
            {
              type: 'STRING',
              category: 'field',
              id: 'platform',
            },
            {
              type: 'NUMERIC',
              category: 'field',
              id: '总访问次数',
            },
            {
              type: 'STRING',
              category: 'field',
              id: 'QD_id',
            },
            {
              type: 'NUMERIC',
              category: 'field',
              id: '总页数',
            },
          ],
          computedFields: [],
        },
      },
      dataChartMap: {
        widget_6a1bded77a424fb3abb18c84a6af2d7b_a8c7191e276d41cba67743041c650ee3:
          {
            id: 'widget_6a1bded77a424fb3abb18c84a6af2d7b_a8c7191e276d41cba67743041c650ee3',
            name: '',
            viewId: '3ca2a12f09c84c8ca1a5714fc6fa44d8',
            orgId: '90fa5a6c58fc45d9bf684e2177690d5b',
            config: {
              chartConfig: {
                datas: [
                  {
                    label: 'dimension',
                    key: 'dimension',
                    required: true,
                    type: 'group',
                    limit: 1,
                    rows: [
                      {
                        uid: 'e1630ee5-ba5d-441d-9cd1-7e6bfac54781',
                        colName: '城市',
                        category: 'field',
                        type: 'STRING',
                      },
                    ],
                  },
                  {
                    label: 'metrics',
                    key: 'metrics',
                    required: true,
                    type: 'aggregate',
                    limit: [1, 999],
                    rows: [
                      {
                        uid: '76696cb5-b286-4441-aa94-5023f616c790',
                        colName: 'GDP（亿元）',
                        category: 'field',
                        type: 'NUMERIC',
                        aggregate: 'SUM',
                      },
                    ],
                  },
                  {
                    label: 'filter',
                    key: 'filter',
                    type: 'filter',
                    allowSameField: true,
                  },
                  {
                    label: 'colorize',
                    key: 'color',
                    type: 'color',
                    limit: [0, 1],
                  },
                  {
                    label: 'info',
                    key: 'info',
                    type: 'info',
                  },
                ],
                settings: [
                  {
                    label: 'viz.palette.setting.paging.title',
                    key: 'paging',
                    comType: 'group',
                    rows: [
                      {
                        label: 'viz.palette.setting.paging.pageSize',
                        key: 'pageSize',
                        default: 1000,
                        comType: 'inputNumber',
                        options: {
                          needRefresh: true,
                          step: 1,
                          min: 0,
                        },
                        value: 100,
                      },
                    ],
                  },
                ],
                i18ns: [],
              },
              chartGraphId: 'stack-area-chart',
              computedFields: [],
              aggregation: true,
            },
            status: 1,
            description: '',
          },
        '2421674d25f740809433ace249506624': {
          config: {
            aggregation: true,
            chartConfig: {
              datas: [
                {
                  label: 'dimension',
                  key: 'dimension',
                  required: true,
                  type: 'group',
                  limit: [0, 1],
                  actions: {
                    NUMERIC: ['alias', 'colorize', 'sortable'],
                    STRING: ['alias', 'colorize', 'sortable'],
                  },
                  rows: [
                    {
                      uid: '3e320dd9-6a7e-44a6-91bd-06e27bc33e79',
                      colName: 'name_level2',
                      category: 'field',
                      type: 'STRING',
                    },
                  ],
                },
                {
                  label: 'metrics',
                  key: 'metrics',
                  required: true,
                  type: 'aggregate',
                  limit: [1, 999],
                  rows: [
                    {
                      uid: 'f978df76-e5df-4cbe-8221-7fb7eb1c7f93',
                      colName: '总停留时间',
                      category: 'field',
                      type: 'NUMERIC',
                      aggregate: 'SUM',
                    },
                  ],
                },
                {
                  allowSameField: true,
                  disableAggregate: false,
                  actions: {
                    NUMERIC: ['filter'],
                    STRING: ['filter'],
                    DATE: ['filter'],
                  },
                  label: 'filter',
                  key: 'filter',
                  type: 'filter',
                  rows: [
                    {
                      uid: '1a323cde-9ba5-47b0-9000-6001bff781a2',
                      colName: 'name_level3',
                      category: 'field',
                      type: 'STRING',
                      aggregate: 'NONE',
                      filter: {
                        visibility: 'hide',
                        condition: {
                          name: 'name_level3',
                          type: 2,
                          value: [
                            {
                              key: 'PC',
                              label: 'PC',
                              isSelected: true,
                            },
                          ],
                          visualType: 'STRING',
                          operator: 'IN',
                        },
                        width: 'auto',
                      },
                    },
                  ],
                },
                {
                  label: 'info',
                  key: 'info',
                  type: 'info',
                },
              ],
              styles: [],
              settings: [
                {
                  label: 'viz.palette.setting.paging.title',
                  key: 'paging',
                  comType: 'group',
                  rows: [
                    {
                      label: 'viz.palette.setting.paging.pageSize',
                      key: 'pageSize',
                      value: 1000,
                      comType: 'inputNumber',
                      rows: [],
                    },
                  ],
                },
              ],
            },
            chartGraphId: 'pie-chart',
            computedFields: [],
          },

          id: '2421674d25f740809433ace249506624',
          name: "de'de",
          orgId: '90fa5a6c58fc45d9bf684e2177690d5b',
          permission: null,
          status: 1,
          thumbnail: null,

          viewId: '836614b7c86042cdbd38fc40da270846',
        },
      },
    };
    const res = [
      {
        viewId: '836614b7c86042cdbd38fc40da270846',
        aggregators: [
          {
            alias: 'SUM(总停留时间)',
            column: ['总停留时间'],
            sqlOperator: 'SUM',
          },
        ],
        groups: [
          {
            alias: 'name_level2',
            column: ['name_level2'],
          },
        ],
        filters: [
          {
            aggOperator: null,
            column: ['name_level3'],
            sqlOperator: 'IN',
            values: [
              {
                value: 'PC',
                valueType: 'STRING',
              },
            ],
          },
          {
            aggOperator: null,
            column: ['name_level1'],
            sqlOperator: 'IN',
            values: [
              {
                value: '线上渠道',
                valueType: 'STRING',
              },
              {
                value: '新媒体营销',
                valueType: 'STRING',
              },
            ],
          },
        ],
        orders: [],
        pageInfo: {
          countTotal: false,
          pageSize: 1000,
        },
        functionColumns: [],
        columns: [],
        script: false,
        cache: false,
        cacheExpires: 0,
        concurrencyControl: true,
        concurrencyControlMode: 'DIRTYREAD',
        params: {},
        vizName: undefined,
        vizId: '2421674d25f740809433ace249506624',
        analytics: false,
        vizType: 'dataChart',
      },
      {
        viewId: '3ca2a12f09c84c8ca1a5714fc6fa44d8',
        aggregators: [
          {
            alias: 'SUM(GDP（亿元）)',
            column: ['GDP（亿元）'],
            sqlOperator: 'SUM',
          },
        ],
        groups: [
          {
            alias: '城市',
            column: ['城市'],
          },
        ],
        filters: [],
        orders: [],
        pageInfo: {
          countTotal: false,
          pageSize: 100,
        },
        functionColumns: [],
        columns: [],
        script: false,
        cache: false,
        cacheExpires: 0,
        concurrencyControl: true,
        concurrencyControlMode: 'DIRTYREAD',
        params: {
          area1: ['山东'],
        },
        vizName: '私有图表_1',
        vizId: 'a8c7191e276d41cba67743041c650ee3',
        analytics: false,
        vizType: 'widget',
      },
    ];
    expect(getBoardChartRequests(obj as any)).toEqual(res);
  });
});
