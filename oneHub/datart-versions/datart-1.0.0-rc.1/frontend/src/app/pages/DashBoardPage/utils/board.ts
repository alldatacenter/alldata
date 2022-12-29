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
import { migrateBoardConfig } from 'app/migration/BoardConfig/migrateBoardConfig';
import migrateChartConfig from 'app/migration/ChartConfig/migrateChartConfig';
import migrationViewConfig from 'app/migration/ViewConfig/migrationViewConfig';
import beginViewModelMigration from 'app/migration/ViewConfig/migrationViewModelConfig';
import {
  BoardInfo,
  BoardType,
  ColsKeyType,
  Dashboard,
  DashboardConfigBeta3,
  DataChart,
  DeviceType,
  ServerDashboard,
  ServerDatachart,
} from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { ChartDataView } from 'app/types/ChartDataView';
import { View } from 'app/types/View';
import {
  createDateLevelComputedFieldForConfigComputedFields,
  mergeChartAndViewComputedField,
} from 'app/utils/chartHelper';
import { transformMeta } from 'app/utils/internalChartHelper';
import { adaptBoardImageUrl } from '.';
import { BoardConfigValue } from '../components/BoardProvider/BoardConfigProvider';
import {
  AutoBoardWidgetBackgroundDefault,
  BackgroundDefault,
  LAYOUT_COLS_MAP,
  MIN_MARGIN,
  MIN_PADDING,
} from '../constants';
import { BoardConfig } from '../types/boardTypes';
import { Widget } from '../types/widgetTypes';
import { initAutoBoardConfig } from './autoBoard';
import { initFreeBoardConfig } from './freeBoard';

export const getDashBoardByResBoard = (data: ServerDashboard): Dashboard => {
  return {
    id: data.id,
    queryVariables: data.queryVariables,
    name: data.name,
    orgId: data.orgId,
    parentId: data.parentId,
    status: data.status,
    thumbnail: data.thumbnail,
    index: data.index,
    config: preprocessBoardConfig(migrateBoardConfig(data.config), data.id),
    permissions: data.permissions,
  };
};
export const preprocessBoardConfig = (config: BoardConfig, boardId: string) => {
  config.jsonConfig.props.forEach(item => {
    if (item.key === 'background') {
      const rowsValue = item?.rows?.[0]?.value;
      if (rowsValue?.image) {
        rowsValue.image = adaptBoardImageUrl(rowsValue.image, boardId);
      }
    }
  });
  return config;
};
export const getScheduleBoardInfo = (
  boardInfo: BoardInfo,
  widgetMap: Record<string, Widget>,
) => {
  let newBoardInfo: BoardInfo = { ...boardInfo };
  const needFetchItems = Object.values(widgetMap)
    .filter(widget => {
      if (widget.viewIds && widget.viewIds.length > 0) {
        return true;
      }
      return false;
    })
    .map(widget => widget.id);

  newBoardInfo.needFetchItems = needFetchItems;

  return newBoardInfo;
};

export const getInitBoardInfo = (obj: {
  id: string;
  widgetIds?: string[];
  controllerWidgets?: Widget[];
}) => {
  //
  const boardInfo: BoardInfo = {
    id: obj.id,
    saving: false,
    loading: false,
    dragging: false,
    editing: false,
    visible: true,
    showBlockMask: true,
    fullScreenItemId: '',
    layouts: [],
    isDroppable: false,
    clipboardWidgetMap: {},
    widgetIds: obj.widgetIds || [],
    controllerPanel: {
      type: 'hide',
      widgetId: '',
    },

    linkFilter: [],

    deviceType: DeviceType.Desktop,
    needFetchItems: [],
    hasFetchItems: [],
    boardWidthHeight: [0, 0],
    originControllerWidgets: obj.controllerWidgets || [],
  };
  return boardInfo;
};

export const getInitBoardConfigBeta3 = (boardType?: BoardType) => {
  const dashboardConfig: DashboardConfigBeta3 = {
    type: boardType || 'auto',
    version: '',
    background: BackgroundDefault,
    widgetDefaultSettings: {
      background: AutoBoardWidgetBackgroundDefault,
      boxShadow: false,
    },
    maxWidgetIndex: 0,
    initialQuery: true,
    hasQueryControl: false,
    hasResetControl: false,

    // auto
    allowOverlap: false,
    margin: [16, 16], //0-100
    containerPadding: [16, 16], //0-100
    cols: LAYOUT_COLS_MAP, //2-48    step 2
    mobileMargin: [MIN_MARGIN, MIN_MARGIN],
    mobileContainerPadding: [MIN_PADDING, MIN_PADDING],
    // free
    width: 1920,
    height: 1080,
    gridStep: [10, 10],
    scaleMode: 'scaleWidth',
  };
  return dashboardConfig;
};
export const getInitBoardConfig = (boardType?: BoardType) => {
  if (boardType === 'auto') {
    return initAutoBoardConfig();
  } else {
    return initFreeBoardConfig();
  }
};
// dataCharts
export const getDataChartsByServer = (
  serverDataCharts: ServerDatachart[],
  view: View[],
) => {
  const dataCharts: DataChart[] = serverDataCharts.map(item => {
    item.config = migrateChartConfig(item.config);

    const config = JSON.parse(item.config || '{}');
    const viewComputerFields =
      JSON.parse(view.find(v => v.id === item.viewId)?.model || '{}')
        ?.computedFields || [];

    config.computedFields = mergeChartAndViewComputedField(
      viewComputerFields,
      config.computedFields,
    );

    return {
      ...item,
      config,
    };
  });

  return dataCharts;
};
export const getDataChartMap = (dataCharts: DataChart[]) => {
  return dataCharts.reduce((acc, cur) => {
    acc[cur.id] = cur;
    return acc;
  }, {} as Record<string, DataChart>);
};

export const getChartDataView = (views: View[], dataCharts: DataChart[]) => {
  const viewViews: ChartDataView[] = [];
  views.forEach(view => {
    const dataChart = dataCharts.find(dc => dc.viewId === view.id);

    if (view) {
      view = migrationViewConfig(view);
    }
    if (view?.model) {
      view.model = beginViewModelMigration(view.model, view.type);
    }
    const meta = transformMeta(view.model);
    const viewComputedFields =
      JSON.parse(view.model || '{}').computedFields || [];
    const computedFields = createDateLevelComputedFieldForConfigComputedFields(
      meta,
      mergeChartAndViewComputedField(
        viewComputedFields,
        dataChart?.config.computedFields,
      ),
    );
    let viewView = {
      ...view,
      meta,
      model: '',
      computedFields,
    };
    viewViews.push(viewView);
  });
  return viewViews;
};

export const getBoardMarginPadding = (
  boardConfig: BoardConfigValue,
  colsKey: ColsKeyType,
) => {
  const { margin, padding, mMargin, mPadding } = boardConfig;
  const isMobile = colsKey === 'sm';
  return isMobile
    ? {
        curMargin: mMargin || [MIN_MARGIN, MIN_MARGIN],
        curPadding: mPadding || [MIN_PADDING, MIN_PADDING],
      }
    : {
        curMargin: margin,
        curPadding: padding,
      };
};

export const isElView = (el, inTimer?: boolean) => {
  let bool = false;
  if (!el) return false;
  let rect = el?.getBoundingClientRect();

  let { top, bottom, x, y, left, right } = rect;

  // freeBoard widget in storyPlayer return true
  const isFreeWidgetInStoryPlayer = top + bottom + x + y + left + right === 0;
  if (isFreeWidgetInStoryPlayer && !inTimer) return true;

  // top 元素顶端到可见区域顶端的距离
  // bottom 元素底部端到可见区域顶端的距离
  var viewHeight = window.innerHeight || document.documentElement.clientHeight; // 浏览器可见区域高度。

  if (top < viewHeight && bottom > 0) {
    bool = true;
  } else if (top >= viewHeight || bottom <= 0) {
    bool = false;
  }
  return bool;
};
