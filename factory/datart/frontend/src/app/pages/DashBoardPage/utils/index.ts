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
  ChartDataSectionType,
  ChartDataViewFieldCategory,
  ControllerFacadeTypes,
  DataViewFieldType,
  TimeFilterValueCategory,
} from 'app/constants';
import { migrateChartConfig } from 'app/migration';
import { ChartDataRequestBuilder } from 'app/models/ChartDataRequestBuilder';
import { ChartDrillOption } from 'app/models/ChartDrillOption';
import { RelatedView } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { ChartDataConfig, ChartDataSectionField } from 'app/types/ChartConfig';
import { ChartDetailConfigDTO } from 'app/types/ChartConfigDTO';
import {
  ChartDataRequestFilter,
  PendingChartDataRequestFilter,
} from 'app/types/ChartDataRequest';
import ChartDataView from 'app/types/ChartDataView';
import { convertToChartConfigDTO } from 'app/utils/ChartDtoHelper';
import { findPathByNameInMeta, getStyles } from 'app/utils/chartHelper';
import { getTime, splitRangerDateFilters } from 'app/utils/time';
import {
  DATE_FORMATTER,
  FilterSqlOperator,
  TIME_FORMATTER,
} from 'globalConstants';
import moment from 'moment';
import { CloneValueDeep } from 'utils/object';
import { boardDrillManager } from '../components/BoardDrillManager/BoardDrillManager';
import { BOARD_FILE_IMG_PREFIX } from '../constants';
import {
  BoardLinkFilter,
  ControllerWidgetContent,
  DataChart,
  getDataOption,
  WidgetInfo,
} from '../pages/Board/slice/types';
import {
  ControllerConfig,
  ControllerDate,
} from '../pages/BoardEditor/components/ControllerWidgetPanel/types';
import { Widget } from '../types/widgetTypes';
import { DateControllerTypes } from './../pages/BoardEditor/components/ControllerWidgetPanel/constants';
import { PickerType } from './../pages/BoardEditor/components/ControllerWidgetPanel/types';
import { getLinkedColumn } from './widget';

export const dateFormatObj = {
  week: 'YYYY-WW',
  year: 'YYYY',
  month: 'YYYY-MM',
  dateTime: TIME_FORMATTER,
  date: DATE_FORMATTER,
  quarter: 'YYYY-Q',
};

export const convertImageUrl = (urlKey: string = ''): string => {
  if (urlKey.startsWith(BOARD_FILE_IMG_PREFIX)) {
    return `${window.location.origin}/${urlKey}`;
  }
  return urlKey;
};
export const getBackgroundImage = (url: string = ''): string => {
  return url ? `url(${convertImageUrl(url)})` : 'none';
};

/**
 * @description '为了server 复制board 副本，原有board资源文件 和新副本资源文件 脱离关系 不受影响'
 * 将当前前端渲染环境 id 替换掉原有的id ，原来的和当前的相等不受影响
 */
export const adaptBoardImageUrl = (url: string = '', curBoardId: string) => {
  const splitter = BOARD_FILE_IMG_PREFIX;
  if (!url.startsWith(splitter)) return url;
  if (!curBoardId) return url;
  const originalBoardId = url.split(splitter)[1].split('/')[0];
  const nextUrl = url.replace(originalBoardId, curBoardId);
  return nextUrl;
};
export const fillPx = (num: number) => {
  return num ? num + 'px' : num;
};
export const getRGBAColor = color => {
  if (!color) {
    return `rgba(0, 0, 0, 1)`;
  }
  if (color && color?.rgb) {
    const { r, g, b, a } = color.rgb;
    return `rgba(${r}, ${g}, ${b}, ${a})`;
  } else {
    return color;
  }
};

export const getDataChartRequestParams = (obj: {
  dataChart: DataChart;
  view: ChartDataView;
  drillOption?: ChartDrillOption;
  tempFilters?: PendingChartDataRequestFilter[];
  option;
}) => {
  const { dataChart, view, option, drillOption, tempFilters } = obj;
  const migratedChartConfig = migrateChartConfig(
    CloneValueDeep(dataChart?.config) as ChartDetailConfigDTO,
  );

  const { datas, settings } = convertToChartConfigDTO(
    migratedChartConfig as ChartDetailConfigDTO,
  );

  const builder = new ChartDataRequestBuilder(
    {
      ...view,
      computedFields: dataChart.config.computedFields || [],
    },
    datas,
    settings,
    {},
    false,
    dataChart?.config?.aggregation,
  );
  let requestParams = builder
    .addExtraSorters((option?.sorters as any) || [])
    .addDrillOption(drillOption)
    .addRuntimeFilters(tempFilters || [])
    .build();
  return requestParams;
};

export const getChartGroupColumns = (datas: ChartDataConfig[] | undefined) => {
  const chartDataConfigs = datas;
  if (!chartDataConfigs) return [] as ChartDataSectionField[];
  const groupTypes = [ChartDataSectionType.Group, ChartDataSectionType.Color];
  const groupColumns = chartDataConfigs.reduce<ChartDataSectionField[]>(
    (acc, cur) => {
      if (!cur.rows) {
        return acc;
      }
      if (groupTypes.includes(cur.type as any)) {
        return acc.concat(cur.rows);
      }
      if (cur.type === ChartDataSectionType.Mixed) {
        return acc.concat(
          cur.rows.filter(({ type }) => type === DataViewFieldType.STRING),
        );
      }
      return acc;
    },
    [],
  );

  return groupColumns;
};

export function getTheWidgetFiltersAndParams<
  T extends ChartDataRequestFilter | PendingChartDataRequestFilter,
>(obj: {
  chartWidget: Widget;
  widgetMap: Record<string, Widget>;
  params: Record<string, string[]> | undefined;
  view?: ChartDataView;
}): {
  filterParams: T[];
  variableParams: Record<string, any[]>;
} {
  // TODO chart 本身携带了变量，board没有相关配置的时候要拿到 chart本身的 变量值 Params
  const { chartWidget, widgetMap, view } = obj;
  const controllerWidgets = Object.values(widgetMap).filter(
    widget => widget.config.type === 'controller',
  );

  let filterParams: T[] = [];
  let variableParams: Record<string, any[]> = {};

  controllerWidgets.forEach(filterWidget => {
    const hasRelation = filterWidget.relations.find(
      re => re.targetId === chartWidget.id,
    );
    if (!hasRelation) return;

    const content = filterWidget.config.content as ControllerWidgetContent;
    const { relatedViews, config: controllerConfig, type } = content;
    const relatedViewItem = relatedViews
      .filter(view => view.fieldValue)
      .find(view => view.viewId === chartWidget?.viewIds?.[0]);
    if (!relatedViewItem) return;

    const values = getWidgetControlValues({
      type,
      relatedViewItem,
      config: controllerConfig,
    });

    if (!values) {
      return;
    }

    // 关联变量逻辑
    if (
      relatedViewItem.relatedCategory === ChartDataViewFieldCategory.Variable
    ) {
      const curValues = values.map(item => String(item.value));

      // range类型 控制器关联两个变量的情况 relatedViewItem.fieldValue [string,string]
      if (Array.isArray(relatedViewItem.fieldValue)) {
        let key1 = String(relatedViewItem.fieldValue?.[0]);
        let key2 = String(relatedViewItem.fieldValue?.[1]);

        //
        variableParams[key1] = [curValues?.[0]];
        variableParams[key2] = [curValues?.[1]];
      } else {
        const key = String(relatedViewItem.fieldValue);

        //单个变量的取值逻辑 不限制为1个
        variableParams[key] = curValues;
      }
    }

    // 关联字段 逻辑
    if (relatedViewItem.relatedCategory === ChartDataViewFieldCategory.Field) {
      let path = view ? ([] as string[]) : '';
      if (view) {
        const row = findPathByNameInMeta(
          view?.meta,
          relatedViewItem.fieldValue,
        );
        path = row?.path || [];
      } else {
        path = relatedViewItem.fieldValue as string;
      }
      const filter = {
        aggOperator: null,
        column: path,
        sqlOperator: controllerConfig.sqlOperator,
        values: values,
      };
      filterParams.push(filter as T);
    }
  });
  // filter 去重
  filterParams = getDistinctFiltersByColumn(filterParams);

  const res = {
    filterParams: filterParams,
    variableParams: variableParams,
  };
  return res;
}

export const getWidgetControlValues = (opt: {
  type: ControllerFacadeTypes;
  relatedViewItem: RelatedView;
  config: ControllerConfig;
}):
  | false
  | {
      value: any;
      valueType: string;
    }[] => {
  const { type, relatedViewItem, config } = opt;
  const valueType = relatedViewItem.fieldValueType;

  if (DateControllerTypes.includes(type)) {
    if (!config?.controllerDate) {
      return false;
    }
    const timeValues = getControllerDateValues({
      controlType: type,
      filterDate: config.controllerDate!,
      execute: true,
    });

    const values = timeValues
      .filter(ele => !!ele)
      .map(ele => {
        const item = {
          value: ele,
          valueType: valueType || 'DATE',
        };
        return item;
      });
    return values[0] ? values : false;
  } else {
    if (!config?.controllerValues?.[0]) {
      return false;
    }

    const values = config.controllerValues
      .filter(ele => {
        if (typeof ele === 'number') {
          return true;
        }
        if (typeof ele === 'string' && ele.trim() !== '') {
          return true;
        }
        return false;
      })
      .map(ele => {
        const item = {
          value: typeof ele === 'string' ? ele.trim() : ele,
          valueType: valueType || 'STRING',
        };
        return item;
      });
    return values[0] ? values : false;
  }
};

// execute=true 要触发查询 发起请求 计算相对时间的绝对时间
export const getControllerDateValues = (obj: {
  controlType: ControllerFacadeTypes;
  filterDate: ControllerDate;
  execute?: boolean;
}) => {
  const { endTime, startTime, pickerType } = obj.filterDate;
  let timeValues: [string, string] = ['', ''];
  if (startTime.relativeOrExact === TimeFilterValueCategory.Exact) {
    timeValues[0] = startTime.exactValue as string;
  } else {
    const { amount, unit, direction } = startTime.relativeValue!;
    const time = getTime(+(direction + amount), unit)(unit, true);
    timeValues[0] = time.format(TIME_FORMATTER);
  }
  if (endTime) {
    //end 精确时间
    if (endTime.relativeOrExact === TimeFilterValueCategory.Exact) {
      timeValues[1] = endTime.exactValue as string;
      if (obj.execute) {
        timeValues[1] = adjustRangeDataEndValue(
          pickerType,
          endTime.exactValue as string,
        );
      } else {
        timeValues[1] = endTime.exactValue as string;
      }
    } else {
      // end 相对时间
      const { amount, unit, direction } = endTime.relativeValue!;
      const isStart = !obj.execute;
      const time = getTime(+(direction + amount), unit)(unit, isStart);
      timeValues[1] = time.format(TIME_FORMATTER);
    }
  }

  if (obj.execute) {
    timeValues.forEach((v, i) => {
      timeValues[i] = v ? moment(v).format(dateFormatObj[pickerType]) : v;
    });
  }

  return timeValues;
};
export const adjustRangeDataEndValue = (
  pickerType: PickerType,
  timeValue: string,
) => {
  if (!timeValue) {
    return timeValue;
  }
  let adjustTime = moment(timeValue);
  switch (pickerType) {
    case 'dateTime':
      // 比较特殊 不做增值处理
      break;
    case 'date':
      adjustTime = adjustTime.add(1, 'days').startOf('days');
      break;
    case 'month':
      adjustTime = adjustTime.add(1, 'months').startOf('months');
      break;
    case 'quarter':
      adjustTime = adjustTime.add(1, 'quarters').startOf('quarters');
      break;
    case 'week':
      adjustTime = adjustTime.add(1, 'weeks').startOf('week');
      break;
    case 'year':
      adjustTime = adjustTime.add(1, 'years').startOf('years');
      break;
    default:
      break;
  }
  let end = adjustTime.format(TIME_FORMATTER);
  return end;
};
export const getChartWidgetRequestParams = (obj: {
  widgetId: string;
  widgetMap: Record<string, Widget>;
  widgetInfo: WidgetInfo | undefined;
  option: getDataOption | undefined;
  viewMap: Record<string, ChartDataView>;
  dataChartMap: Record<string, DataChart>;
  boardLinkFilters?: BoardLinkFilter[];
  drillOption: ChartDrillOption | undefined;
}) => {
  const {
    widgetId,
    widgetMap,
    viewMap,
    widgetInfo,
    dataChartMap,
    option,
    boardLinkFilters,
    drillOption,
  } = obj;
  if (!widgetId) return null;
  const curWidget = widgetMap[widgetId];
  if (!curWidget) return null;
  if (curWidget.config.type !== 'chart') return null;
  if (!curWidget.datachartId) return null;
  const dataChart = dataChartMap[curWidget.datachartId];
  if (!dataChart) {
    // errorHandle(`can\`t find Chart ${curWidget.datachartId}`);
    return null;
  }
  // 有可能有的chart 没有viewId 例如富文本chart,有时候没有 viewId，不用取相关请求参数
  if (!dataChart.viewId) return null;

  const chartDataView = viewMap[dataChart?.viewId];

  let requestParams = getDataChartRequestParams({
    dataChart,
    view: chartDataView,
    option: option,
    drillOption,
    tempFilters: widgetInfo?.linkInfo?.tempFilters,
  });
  const { filterParams, variableParams } =
    getTheWidgetFiltersAndParams<ChartDataRequestFilter>({
      chartWidget: curWidget,
      widgetMap,
      params: requestParams.params,
      view: chartDataView,
    });

  // 全局过滤 filter
  // TODO
  requestParams.filters = requestParams.filters.concat(filterParams);

  // 联动 过滤
  if (boardLinkFilters) {
    const linkFilters: ChartDataRequestFilter[] = [];
    const links = boardLinkFilters.filter(
      link => link.linkerWidgetId === curWidget.id,
    );

    links.forEach(link => {
      const { triggerValue, triggerWidgetId } = link;
      const triggerWidget = widgetMap[triggerWidgetId];
      const linkColumn = getLinkedColumn(link.linkerWidgetId, triggerWidget);

      const filter: ChartDataRequestFilter = {
        aggOperator: null,
        column: JSON.parse(linkColumn),
        sqlOperator: FilterSqlOperator.In,
        values: [{ value: triggerValue, valueType: DataViewFieldType.STRING }],
      };
      linkFilters.push(filter);
    });
    requestParams.filters = requestParams.filters.concat(linkFilters);
  }

  // splitRangerDateFilters
  requestParams.filters = splitRangerDateFilters(requestParams.filters);

  // 变量
  if (variableParams) {
    requestParams.params = variableParams;
  }
  if (widgetInfo) {
    const { pageInfo } = widgetInfo;
    if (requestParams.pageInfo) {
      requestParams.pageInfo.pageNo = pageInfo.pageNo;
    }
  }
  if (option) {
    const { pageInfo } = option;
    if (requestParams.pageInfo && pageInfo?.pageNo) {
      requestParams.pageInfo.pageNo = pageInfo?.pageNo;
    }
  }
  return requestParams;
};
export const getBoardChartRequests = (params: {
  widgetMap: Record<string, Widget>;
  viewMap: Record<string, ChartDataView>;
  dataChartMap: Record<string, DataChart>;
}) => {
  const { widgetMap, viewMap, dataChartMap } = params;
  const chartWidgetIds = Object.values(widgetMap)
    .filter(w => w.config.type === 'chart')
    .map(w => w.id);

  const chartRequest = chartWidgetIds
    .map(widgetId => {
      const isWidget = widgetMap[widgetId].datachartId.indexOf('widget') !== -1;
      const boardId = widgetMap[widgetId].dashboardId;
      const drillOption = boardDrillManager.getWidgetDrill({
        bid: boardId,
        wid: widgetId,
      });
      return {
        ...getChartWidgetRequestParams({
          widgetId,
          widgetMap,
          viewMap,
          option: undefined,
          widgetInfo: undefined,
          dataChartMap,
          drillOption,
        }),
        ...{
          vizName: widgetMap[widgetId].config.name,
          vizId: isWidget
            ? widgetMap[widgetId].id
            : widgetMap[widgetId].datachartId,
          analytics: false,
          vizType: isWidget ? 'widget' : 'dataChart',
        },
      };
    })

    .filter(res => {
      if (res) {
        return true;
      }
      return false;
    });
  return chartRequest;
};
//  filter 去重
export const getDistinctFiltersByColumn = filter => {
  if (!filter) {
    return [];
  }
  const filterMap: Record<string, any> = {};
  filter.forEach(item => {
    filterMap[item.column] = item;
  });
  return Object.values(filterMap);
};

export const getJsonConfigs = getStyles;
