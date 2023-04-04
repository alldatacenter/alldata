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

import { ControllerFacadeTypes, TimeFilterValueCategory } from 'app/constants';
import {
  ContainerItem,
  TabWidgetContent,
  WidgetOfCopy,
  WidgetType,
} from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { FilterSearchParamsWithMatch } from 'app/pages/MainPage/pages/VizPage/slice/types';
import { ChartsEventData } from 'app/types/Chart';
import { RelationFilterValue } from 'app/types/ChartConfig';
import ChartDataView from 'app/types/ChartDataView';
import { View } from 'app/types/View';
import {
  filterCurrentUsedComputedFields,
  mergeChartAndViewComputedField,
} from 'app/utils/chartHelper';
import { updateBy } from 'app/utils/mutation';
import { formatTime } from 'app/utils/time';
import {
  BOARD_COPY_CHART_SUFFIX,
  FilterSqlOperator,
  TIME_FORMATTER,
} from 'globalConstants';
import produce from 'immer';
import { CSSProperties } from 'react';
import { CloneValueDeep } from 'utils/object';
import { adaptBoardImageUrl, fillPx, getBackgroundImage } from '.';
import { initClientId } from '../components/WidgetManager/utils/init';
import { LAYOUT_COLS_MAP, ORIGINAL_TYPE_MAP } from '../constants';
import {
  BackgroundConfig,
  BoardType,
  BorderConfig,
  ChartWidgetContent,
  ControllerWidgetContent,
  DataChart,
  Relation,
  ServerRelation,
  ServerWidget,
  WidgetContentChartType,
  WidgetInfo,
  WidgetPadding,
} from '../pages/Board/slice/types';
import { StrControlTypes } from '../pages/BoardEditor/components/ControllerWidgetPanel/constants';
import { Widget, WidgetMapping } from '../types/widgetTypes';

export const VALUE_SPLITTER = '###';

// export const createInitWidgetConfig = (opt: {
//   type: WidgetType;
//   content: WidgetContent;
//   boardType: BoardType;
//   index?: number;
//   name?: string;
//   autoUpdate?: boolean;
//   frequency?: number;
// }): WidgetConf => {
//   return {
//     version: '',
//     type: opt.type,
//     index: opt.index || 0,
//     name: opt.name || '',
//     linkageConfig: {
//       open: false,
//       chartGroupColumns: [],
//     },
//     autoUpdate: opt.autoUpdate || false,
//     lock: false,
//     frequency: opt.frequency || 60, // 60秒
//     rect: createWidgetRect(opt.boardType, opt.type),
//     background:
//       opt.boardType === 'auto'
//         ? opt.type === 'query'
//           ? QueryButtonWidgetBackgroundDefault
//           : AutoBoardWidgetBackgroundDefault
//         : BackgroundDefault,
//     border: ['query', 'reset'].includes(opt.type)
//       ? ButtonBorderDefault
//       : BorderDefault,
//     content: opt.content,
//     nameConfig: {
//       show: true,
//       textAlign: 'left',
//       ...FontDefault,
//       color: opt.type === 'query' ? WHITE : G90,
//     } as any,
//     padding: createWidgetPadding(opt.type),
//   };
// };

export const createWidgetInfo = (id: string): WidgetInfo => {
  const widgetInfo: WidgetInfo = {
    id: id,
    loading: false,
    editing: false,
    inLinking: false,
    selected: false,
    errInfo: {} as WidgetInfo['errInfo'],
    rendered: false,
    pageInfo: {
      pageNo: 1,
    },
  };
  return widgetInfo;
};
export const createWidgetPadding = (widgetType: WidgetType) => {
  if (widgetType === 'button') {
    return {
      left: 0,
      right: 0,
      top: 0,
      bottom: 0,
    };
  } else if (widgetType === 'controller') {
    return {
      left: 8,
      right: 0,
      top: 0,
      bottom: 0,
    };
  }
  return {
    left: 8,
    right: 8,
    top: 8,
    bottom: 8,
  };
};

// export const createContainerWidgetContent = (type: ContainerWidgetType) => {
//   let content: ContainerWidgetContent = {
//     type: type,
//     itemMap: {},
//   };
//   switch (type) {
//     case 'tab':
//       content.tabConfig = {};
//       break;
//     case 'carousel':
//       content.carouselConfig = {};
//       break;
//     default:
//       break;
//   }
//   return content;
// };

export const createChartWidgetContent = (subType: WidgetContentChartType) => {
  let content: ChartWidgetContent = {
    type: subType,
  };
  return content;
};

export const getWidgetInfoMapByServer = (widgetMap: Record<string, Widget>) => {
  const widgetInfoMap = {};
  Object.values(widgetMap).forEach(item => {
    widgetInfoMap[item.id] = createWidgetInfo(item.id);
  });
  return widgetInfoMap;
};

export const adjustWidgetsToBoard = (args: {
  widgets: Widget[];
  boardType: BoardType;
  boardId: string;
  layouts?: ReactGridLayout.Layout[];
}) => {
  const { widgets, boardType, layouts } = args;

  if (boardType === 'auto') {
    return updateAutoWidgetsRect(widgets, layouts || []);
  } else if (boardType === 'free') {
    return updateFreeWidgetsRect(widgets);
  }
  return widgets;
};

export const updateAutoWidgetsRect = (
  widgets: Widget[],
  layouts: ReactGridLayout.Layout[],
): Widget[] => {
  const upDatedWidgets: Widget[] = [];
  const dashWidgetRectYs = layouts.map(ele => ele.y);
  let widgetsCount = dashWidgetRectYs.length;
  let itemYs = [...dashWidgetRectYs];
  widgets.forEach(widget => {
    const itemX =
      (widgetsCount * widget.config.pRect.width) % LAYOUT_COLS_MAP.lg;
    const itemY = Math.max(...itemYs, 0);
    const nextRect = {
      ...widget.config.pRect,
      x: itemX,
      y: itemY,
    };
    widget = produce(widget, draft => {
      draft.config.pRect = nextRect;
    });
    upDatedWidgets.push(widget);
    widgetsCount++;
    itemYs.push(itemY);
  });
  return upDatedWidgets;
};

export const updateFreeWidgetsRect = (widgets: Widget[]) => {
  const upDatedWidgets: Widget[] = [];
  let diffValue = 0; // 避免完全重叠
  widgets.forEach(widget => {
    widget = produce(widget, draft => {
      draft.config.rect.x = draft.config.rect.x + diffValue;
      draft.config.rect.y = draft.config.rect.y + diffValue;
    });
    diffValue += 40;
    upDatedWidgets.push(widget);
  });
  return upDatedWidgets;
};

/**
 * @param widgetRecord 现有的widget
 * @param widgetIds 之前原有的widgetIds
 * @description ''
 */
export const createToSaveWidgetGroup = (
  widgets: Widget[],
  widgetIds: string[],
) => {
  widgets = updateBy(widgets, draft => {
    draft.forEach(v => {
      if (
        v.config.type === 'chart' &&
        v.config?.content?.dataChart?.config?.computedFields
      ) {
        v.config.content.dataChart.config.computedFields =
          filterCurrentUsedComputedFields(
            v.config?.content?.dataChart?.config?.chartConfig,
            v.config?.content?.dataChart?.config?.computedFields.filter(
              v => !v.isViewComputedFields,
            ),
          );
      }
    });
  });
  const curWidgetIds = widgets.map(widget => widget.id);

  // 删除的
  const widgetToDelete = widgetIds.filter(id => !curWidgetIds.includes(id));
  // 新增的
  // widgetToCreate
  const widgetToCreate: ServerWidget[] = widgets
    .filter(widget => {
      return !widgetIds.includes(widget.id);
    })
    .map(widget => convertWidgetToSave(widget));
  // 原有的 需要更新的
  const widgetToUpdate: ServerWidget[] = widgets
    .filter(widget => {
      return widgetIds.includes(widget.id);
    })
    .map(widget => convertWidgetToSave(widget));

  return {
    widgetToCreate,
    widgetToUpdate,
    widgetToDelete,
  };
};

export const convertWidgetToSave = (widget: Widget): ServerWidget => {
  return {
    ...widget,
    config: JSON.stringify(widget.config),
    relations: convertWidgetRelationsToSave(widget.relations) || [],
  };
};

export const convertWidgetRelationsToSave = (
  relations: Relation[] = [],
): ServerRelation[] => {
  return relations.map(relation => {
    return { ...relation, config: JSON.stringify(relation.config) };
  });
};

export const convertToWidgetMap = (widgets: Widget[]) => {
  return widgets.reduce((acc, cur) => {
    acc[cur.id] = cur;
    return acc;
  }, {} as Record<string, Widget>);
};

export const createWidgetInfoMap = (widgets: Widget[]) => {
  return widgets.reduce((acc, cur) => {
    acc[cur.id] = createWidgetInfo(cur.id);
    return acc;
  }, {} as Record<string, WidgetInfo>);
};

export const convertWrapChartWidget = (params: {
  widgetMap: Record<string, Widget>;
  dataChartMap: Record<string, DataChart>;
  viewMap: Record<string, ChartDataView>;
}) => {
  const { widgetMap, dataChartMap } = params;
  const widgets = Object.values(widgetMap).map(widget => {
    if (widget.config.originalType !== ORIGINAL_TYPE_MAP.ownedChart) {
      return widget;
    }
    // widgetChart wrapChartWidget
    const dataChart = dataChartMap[widget.datachartId];
    const newWidget = produce(widget, draft => {
      draft.datachartId = '';
      (draft.config.content as ChartWidgetContent).dataChart = dataChart;
    });
    return newWidget;
  });
  return widgets;
};

/**
 * @param ''
 * @description 'get all filter widget of board'
 */
export const getAllControlWidget = (widgetMap: Record<string, Widget>) => {
  const controlWidgetMap = Object.values(widgetMap)
    .filter(widget => widget.config.type === 'controller')
    .reduce((acc, cur) => {
      acc[cur.id] = cur;
      return acc;
    }, {} as Record<string, Widget>);
  return controlWidgetMap;
};
export const getOtherStringControlWidgets = (
  allWidgets: Widget[],
  widgetId: string | undefined,
) => {
  const allFilterWidgets = allWidgets.filter(ele => {
    if (ele.config.type !== 'controller') {
      return false;
    }
    const content = ele.config.content as ControllerWidgetContent;
    return StrControlTypes.includes(content.type);
  });
  if (!widgetId) {
    return allFilterWidgets;
  } else {
    return allFilterWidgets.filter(ele => ele.id !== widgetId);
  }
};

/**
 * @param ''
 * @description 'get showing controller by all filterWidget of board'
 */
export const getVisibleControlWidgetIds = (
  controlWidgetMap: Record<string, Widget>,
) => {
  const widgets = Object.values(controlWidgetMap);
  const visibleWidgets = getNoHiddenControllers(widgets);
  const visibleFreeWidgetIds = visibleWidgets
    .sort((a, b) => a.config.index - b.config.index)
    .map(w => w.id);
  return {
    visibleFreeWidgetIds,
  };
};

export const getLayoutWidgets = (widgetMap: Record<string, Widget>) => {
  const noSubWidgets = Object.values(widgetMap).filter(w => !w.parentId);
  const layoutWidgets = getNoHiddenControllers(noSubWidgets);
  return layoutWidgets;
};

export const getNoHiddenControllers = (widgets: Widget[]) => {
  const noHiddenControlWidgets = widgets.filter(w => {
    if (w.config.type !== 'controller') {
      return true;
    }
    const content = w.config.content as ControllerWidgetContent;
    const visibility = content.config.visibility;
    if (visibility.visibilityType === 'show') {
      return true;
    }
    if (visibility.visibilityType === 'hide') {
      return false;
    }
    if (visibility.visibilityType === 'condition') {
      const condition = content.config.visibility.condition;
      if (condition) {
        const {
          dependentControllerId: dependentFilterId,
          relation,
          value: targetValue,
        } = condition;
        const dependWidget = widgets.find(
          widget => widget.id === dependentFilterId,
        );
        if (!dependWidget) {
          return false;
        }
        const content = dependWidget.config.content as ControllerWidgetContent;
        const dependWidgetValue = content.config.controllerValues?.[0];
        if (relation === FilterSqlOperator.Equal) {
          return targetValue === dependWidgetValue;
        }
        if (relation === FilterSqlOperator.NotEqual) {
          return targetValue !== dependWidgetValue;
        }
        return false;
      }
      return false;
    }
    return false;
  });
  return noHiddenControlWidgets;
};

export const getNeedRefreshWidgetsByController = (controller: Widget) => {
  const relations = controller.relations;
  const widgetIds = relations
    .filter(ele => ele.config.type === 'controlToWidget')
    .map(ele => ele.targetId);
  return widgetIds;
};
export const getCascadeControllers = (controller: Widget) => {
  const relations = controller.relations;
  const ids = relations
    .filter(ele => ele.config.type === 'controlToControlCascade')
    .map(ele => ele.targetId);
  return ids;
};

export const getFreeWidgetStyle = (widget: Widget) => {
  const widgetConf = widget.config;
  const rect = widgetConf.rect;
  let widgetStyle: CSSProperties = {
    position: 'absolute',
    left: fillPx(rect.x),
    top: fillPx(rect.y),
    display: 'flex',
    flexDirection: 'column',
    width: fillPx(rect.width),
    height: fillPx(rect.height),
    zIndex: widgetConf.index,
    // transform: `translate(${rect.x}px, ${rect.y}px)`,
    transformOrigin: ' 0 0',
  };
  return widgetStyle;
};

// getWidgetStyle end
// get some css start
export const getBackgroundCss = (bg: BackgroundConfig) => {
  let css: CSSProperties = {
    backgroundColor: bg.color,
    backgroundImage: getBackgroundImage(bg.image),
    backgroundRepeat: bg.repeat,
    backgroundSize: bg.size,
  };
  return css;
};

export const getBorderCss = (bd: BorderConfig) => {
  let css: CSSProperties = {
    borderColor: bd?.color,
    borderStyle: bd?.style,
    borderWidth: fillPx(bd?.width),
    borderRadius: fillPx(bd?.radius),
  };
  return css;
};

export const getPaddingCss = (pd: WidgetPadding) => {
  let css: CSSProperties = {
    paddingTop: pd?.top,
    paddingLeft: pd?.left,
    paddingBottom: pd?.bottom,
    paddingRight: pd?.right,
  };
  return css;
};

export const getWidgetSomeStyle = (opt: {
  background: BackgroundConfig;
  padding: WidgetPadding;
  border: BorderConfig;
}) => {
  const backgroundCss = getBackgroundCss(opt.background);
  const paddingCss = getPaddingCss(opt.padding as WidgetPadding);
  const borderCss = getBorderCss(opt.border as BorderConfig);
  let style: CSSProperties = {
    ...backgroundCss,
    ...paddingCss,
    ...borderCss,
  };
  return style;
};

export const getLinkedColumn = (
  targetWidgetId: string,
  triggerWidget: Widget,
) => {
  const relations = triggerWidget.relations;
  const relation = relations.find(item => item.targetId === targetWidgetId);

  return (
    relation?.config?.widgetToWidget?.linkerColumn ||
    relation?.config?.widgetToWidget?.triggerColumn ||
    ''
  );
};

// TODO chart widget
export const getWidgetMap = (
  widgets: Widget[],
  dataCharts: DataChart[],
  boardType: BoardType,
  serverViews: View[],
  filterSearchParamsMap?: FilterSearchParamsWithMatch,
) => {
  const filterSearchParams = filterSearchParamsMap?.params,
    isMatchByName = filterSearchParamsMap?.isMatchByName;
  const dataChartMap = dataCharts.reduce((acc, cur) => {
    acc[cur.id] = cur;
    return acc;
  }, {} as Record<string, DataChart>);
  const widgetMap = widgets.reduce((acc, cur) => {
    // issues #601
    const chartViewId = dataChartMap[cur.datachartId]?.viewId;
    const viewIds = chartViewId ? [chartViewId] : cur.viewIds;
    const viewComputerFields =
      JSON.parse(serverViews.find(v => v.id === viewIds[0])?.model || '{}')
        ?.computedFields || [];
    if (cur.config.type === 'chart' && cur.config?.content?.dataChart?.config) {
      cur.config.content.dataChart.config.computedFields =
        mergeChartAndViewComputedField(
          viewComputerFields,
          cur.config?.content?.dataChart?.config?.computedFields,
        );
    }

    acc[cur.id] = {
      ...cur,
      viewIds,
    };
    return acc;
  }, {} as Record<string, Widget>);

  const wrappedDataCharts: DataChart[] = [];
  const controllerWidgets: Widget[] = []; // use for reset button
  const widgetList = Object.values(widgetMap);

  // 处理 controller config visibility依赖关系 id, url参数修改filter
  widgetList
    .filter(w => w.config.type === 'controller')
    .forEach(widget => {
      const content = widget.config.content as ControllerWidgetContent;
      // 根据 url参数修改filter 默认值
      if (filterSearchParams) {
        const paramsKey = Object.keys(filterSearchParams);
        const matchKey = isMatchByName ? widget.config.name : widget.id;
        if (paramsKey.includes(matchKey)) {
          const _value = isMatchByName
            ? filterSearchParams[widget.config.name]
            : filterSearchParams[widget.id];
          switch (content?.type) {
            case ControllerFacadeTypes.RangeTime:
              if (
                content.config.controllerDate &&
                content.config.controllerDate?.startTime &&
                content.config.controllerDate?.endTime
              ) {
                content.config.controllerDate.startTime.exactValue =
                  _value?.[0];
                content.config.controllerDate.endTime.exactValue = _value?.[0];
              }
              break;

            case ControllerFacadeTypes.Time:
              content.config.controllerDate = {
                ...(content.config.controllerDate as any),
                startTime: {
                  relativeOrExact: TimeFilterValueCategory.Exact,
                  exactValue: formatTime(_value as any, TIME_FORMATTER),
                },
              };
              break;
            default:
              content.config.controllerValues = _value || [];
              break;
          }
        }
      }

      // 通过widget.relation 那里面的 targetId确定 关联controllerWidget 的真实ID
      const { visibilityType: visibility, condition } =
        content.config.visibility;
      const { relations } = widget;
      if (visibility === 'condition' && condition) {
        const dependentFilterId = relations
          .filter(re => re.config.type === 'controlToControl')
          .map(re => re.targetId)?.[0];
        if (dependentFilterId) {
          condition.dependentControllerId = dependentFilterId;
        }
      }
      controllerWidgets.push(widget);
    });

  // 处理 自有 chart widgetControl
  widgetList
    .filter(w => w.config.originalType === ORIGINAL_TYPE_MAP.ownedChart)
    .forEach(widget => {
      let dataChart = (widget.config.content as any).dataChart as DataChart;
      if (dataChart) {
        wrappedDataCharts.push(dataChart!);
      }
      widget.datachartId = dataChart?.id;
    });

  // 处理 widget包含关系 tab Widget 被包含的 widget.parentId 不为空
  widgetList
    .filter(w => w.parentId)
    .forEach(widget => {
      const parentWidgetId = widget.parentId!;
      const parentWidget = widgetMap[parentWidgetId];
      if (!parentWidget) {
        widget.parentId = '';
        return;
      }
      if (parentWidget.config.originalType !== ORIGINAL_TYPE_MAP.tab) {
        return;
      }
      const tabContent = parentWidget.config.content as TabWidgetContent;
      if (!tabContent.itemMap) {
        widget.parentId = '';
        return;
      }

      const targetTabItem = tabContent.itemMap?.[widget.config.clientId];
      if (!targetTabItem) {
        widget.parentId = '';
        return;
      }
      targetTabItem.childWidgetId = widget.id;
    });
  // clear Group children
  widgetList
    .filter(w => w.config.originalType === ORIGINAL_TYPE_MAP.group)
    .forEach(widget => {
      widget.config.children = [];
    });
  // set Group children
  widgetList
    .filter(w => w.parentId)
    .forEach(widget => {
      const parentWidgetId = widget.parentId!;
      const parentWidget = widgetMap[parentWidgetId];
      if (!parentWidget) {
        widget.parentId = '';
        return;
      }
      if (parentWidget.config.originalType !== ORIGINAL_TYPE_MAP.group) {
        return;
      }
      if (!Array.isArray(parentWidget.config.children)) {
        parentWidget.config.children = [];
      }
      parentWidget.config.children.push(widget.id);
    });
  // preprocess widget
  widgetList.forEach(widget => {
    widget.config.boardType = boardType;
    widget.config.customConfig.props?.forEach(item => {
      if (item.key === 'backgroundGroup') {
        const rowsValue = item?.rows?.[0]?.value;
        if (rowsValue?.image) {
          rowsValue.image = adaptBoardImageUrl(
            rowsValue.image,
            widget.dashboardId,
          );
        }
      }
    });
  });

  return {
    widgetMap,
    wrappedDataCharts,
    controllerWidgets,
  };
};

export const getValueByRowData = (
  data: ChartsEventData | undefined,
  fieldName: string,
) => {
  // TODO: Not used for now, you can delete it
  let toCaseField = JSON.parse(fieldName).join('.');

  return data?.rowData[toCaseField];
};

export function cloneWidgets(args: {
  widgets: WidgetOfCopy[];
  dataChartMap: Record<string, DataChart>;
  newWidgetMapping: WidgetMapping;
}) {
  const newDataCharts: DataChart[] = [];
  const newWidgets: Widget[] = [];
  const { widgets, dataChartMap, newWidgetMapping } = args;
  widgets.forEach(widget => {
    const newWidget = CloneValueDeep(widget);
    delete newWidget.selectedCopy;
    newWidget.id = newWidgetMapping[newWidget.id]?.newId;
    newWidget.parentId = newWidgetMapping[widget.parentId]?.newId || '';
    newWidget.config.clientId =
      newWidgetMapping[widget.id]?.newClientId || initClientId();
    newWidget.relations = [];
    newWidget.config.name += BOARD_COPY_CHART_SUFFIX;
    // group
    newWidget.config.children = newWidget.config.children?.map(id => {
      return newWidgetMapping[id].newId;
    });
    // tab
    if (newWidget.config.type === 'container') {
      const content = newWidget.config.content as TabWidgetContent;
      const itemList = Object.values(content.itemMap);
      const newItemMap = itemList.reduce((acc, cur) => {
        const newTabId =
          newWidgetMapping[cur.childWidgetId]?.newClientId || initClientId();
        acc[newTabId] = {
          index: cur.index,
          name: cur.name,
          tabId: newTabId,
          childWidgetId: newWidgetMapping[cur.childWidgetId]?.newId || '',
        };
        return acc;
      }, {} as Record<string, ContainerItem>);
      content.itemMap = newItemMap;
    }
    //chart
    if (newWidget.config.type === 'chart') {
      let dataChart = dataChartMap[newWidget.datachartId];
      const newDataChart: DataChart = CloneValueDeep({
        ...dataChart,
        id: dataChart.id + Date.now() + BOARD_COPY_CHART_SUFFIX,
      });
      newWidget.config.originalType = ORIGINAL_TYPE_MAP.ownedChart;
      newWidget.datachartId = newDataChart.id;
      newDataCharts.push(newDataChart);
      // TODO fix

      // dispatch(boardActions.setDataChartToMap([newDataChart]));
    }
    newWidgets.push(newWidget);
  });
  return {
    newDataCharts,
    newWidgets,
  };
}
/**
 * @describe list[grandpa dad son...] to List[{id:'',parentId:''}]
 * @param collection [[grandpa dad son,....]]
 * @returns [{id:'',parentId:'',value:''}....]
 */
export const handleRowDataForTree = collection => {
  let obj = {};
  collection?.forEach(v => {
    v.forEach((val, ind) => {
      if (!obj[val] || obj[val]?.isLeaf) {
        obj[val] = {
          id: val,
          parentId: ind ? v[ind - 1] : null,
        };
      }
    });
  });
  return Object.values(obj);
};

export const convertListToTree = (
  list,
  parentId: null | string = null,
): any[] => {
  if (!list) {
    return list;
  }
  const treeNodes: any[] = [];
  const childrenList: any = [];
  list.forEach(o => {
    if (o['parentId'] === parentId) {
      treeNodes.push({
        id: o['id'],
        parentId: o['parentId'],
        key: o['id'],
        title: o['label'] || o['id'],
      });
    } else {
      childrenList.push(o);
    }
  });

  return treeNodes.map(node => {
    const children = convertListToTree(childrenList, node.id);
    return children?.length ? { ...node, children } : { ...node, isLeaf: true };
  });
};

export const convertToTree = (col, buildingMethod) => {
  if (!col && !col.length) {
    return col;
  }

  let data: RelationFilterValue[] = [];
  let copyCol = CloneValueDeep(col);
  let emptyParentList = ['null', 'undefined', 'false'];

  if (buildingMethod === 'byParent') {
    const parent = copyCol?.find(
      v => !v[v.length - 1] || emptyParentList.includes(v[v.length - 1]),
    ) || [null];
    data = convertListToTree(
      copyCol?.map(v => {
        return {
          id: v[0],
          parentId: v[v.length - 1],
          label: v.length > 2 ? v[1] : v[0],
        };
      }),
      parent[parent.length - 1],
    );
  } else {
    data = convertListToTree(handleRowDataForTree(copyCol));
  }

  return data;
};
