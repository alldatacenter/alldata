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
import { ChartEditorProps } from 'app/components/ChartEditor';
import {
  ChartDataViewFieldCategory,
  ControllerFacadeTypes,
  DataViewFieldType,
} from 'app/constants';
import { BoardConfig } from 'app/pages/DashBoardPage/types/boardTypes';
import { Widget } from 'app/pages/DashBoardPage/types/widgetTypes';
import { Variable } from 'app/pages/MainPage/pages/VariablePage/slice/types';
import { ChartConfig, SelectedItem } from 'app/types/ChartConfig';
import { PendingChartDataRequestFilter } from 'app/types/ChartDataRequest';
import { ChartDatasetMeta } from 'app/types/ChartDataSet';
import ChartDataView from 'app/types/ChartDataView';
import { DeltaStatic } from 'quill';
import { Layout } from 'react-grid-layout';
import { IFontDefault } from '../../../../../../types';
import { ChartDataSectionField } from '../../../../../types/ChartConfig';
import { View } from '../../../../../types/View';
import { PageInfo } from '../../../../MainPage/pages/ViewPage/slice/types';
import {
  BorderStyleType,
  LAYOUT_COLS_KEYS,
  LAYOUT_COLS_MAP,
  ScaleModeType,
  TextAlignType,
} from '../../../constants';
import { ControllerConfig } from '../../BoardEditor/components/ControllerWidgetPanel/types';

export interface BoardState {
  boardRecord: Record<string, Dashboard>;
  boardInfoRecord: Record<string, BoardInfo>;
  widgetRecord: Record<string, Record<string, Widget>>;
  widgetInfoRecord: Record<string, Record<string, WidgetInfo>>;
  dataChartMap: Record<string, DataChart>;
  viewMap: Record<string, ChartDataView>; // View
  widgetDataMap: Record<string, WidgetData | undefined>;
  availableSourceFunctionsMap: Record<string, string[]>;
  selectedItems: Record<string, SelectedItem[]>;
}
// 应用内浏览，分享页模式，定时任务模式，编辑模式
export type VizRenderMode = 'read' | 'share' | 'schedule' | 'edit';

export interface Dashboard {
  id: string;
  name: string;
  orgId: string;
  parentId?: string | null;
  status: number;
  thumbnail: string;
  index?: number;
  config: BoardConfig;
  permissions?: any;
  queryVariables: Variable[];
}
export interface SaveDashboard extends Omit<Dashboard, 'config'> {
  config: string;
  subType: BoardType;
  widgetToCreate?: ServerWidget[];
  widgetToUpdate?: ServerWidget[];
  widgetToDelete?: string[];
}
export interface ServerDashboard extends Omit<Dashboard, 'config'> {
  config: string;
  views: View[];
  datacharts: ServerDatachart[];
  widgets: ServerWidget[];
}
export interface DashboardConfigBeta3 {
  version: string;
  background: BackgroundConfig;
  widgetDefaultSettings: {
    background: BackgroundConfig;
    boxShadow?: boolean;
  };
  maxWidgetIndex: number;
  initialQuery: boolean;
  hasQueryControl: boolean;
  hasResetControl: boolean;
  type: BoardType; //'auto','free'

  // auto
  allowOverlap: boolean;
  margin: [number, number];
  containerPadding: [number, number];
  mobileMargin: [number, number];
  mobileContainerPadding: [number, number];
  cols?: ColsType;
  // free
  width: number;
  height: number;
  gridStep: [number, number];
  scaleMode: ScaleModeType;
}

export type ColsKeyType = typeof LAYOUT_COLS_KEYS[number];
export const BoardTypes = ['auto', 'free'] as const;
BoardTypes.includes('auto');
export type BoardType = typeof BoardTypes[number];

export interface WidgetOfCopy extends Widget {
  selectedCopy?: boolean;
}
export interface ServerWidget extends Omit<Widget, 'config' | 'relations'> {
  config: string;
  relations: ServerRelation[];
}

export interface WidgetTitleConfig {
  title: string;
  showTitle: boolean;
  textAlign?: TextAlignType;
  font: IFontDefault;
}
export interface LinkageConfig {
  open: boolean;
  chartGroupColumns: ChartDataSectionField[];
}
export interface JumpConfigTarget {
  id: string;
  relId: string;
  relType: string;
}
export interface JumpConfigFilter {
  filterId: string;
  filterLabel: string;
  filterType: string;
  filterValue?: string;
}
export interface JumpConfigField {
  jumpFieldName: string;
}
export interface JumpConfig {
  open: boolean;
  targetType: JumpTargetType;
  URL: string;
  queryName: string;
  field: JumpConfigField;
  target: JumpConfigTarget;
  filter: JumpConfigFilter;
}
export type JumpTargetType = 'INTERNAL' | 'URL';
export interface WidgetPadding {
  left?: number;
  right?: number;
  top?: number;
  bottom?: number;
}
// 组件原始类型

// 组件 view model

export interface WidgetInfo {
  id: string;
  loading: boolean;
  editing: boolean;
  rendered: boolean;
  inLinking: boolean; //是否在触发联动
  selected: boolean;
  pageInfo: Partial<PageInfo>;
  errInfo: Record<WidgetErrorType, string>;
  parameters?: any;
  linkInfo?: WidgetLinkInfo;
}
export interface WidgetData {
  id: string;
  columns?: ChartDatasetMeta[];
  name?: string;
  rows?: string[][];
  pageInfo?: Partial<PageInfo>;
}

//
export const RenderTypes = [
  'rerender',
  'clear',
  'refresh',
  'resize',
  'loading',
  'select',
  'flush',
] as const;
export type RenderType = typeof RenderTypes[number];

export interface Relation {
  targetId: string;
  config: RelationConfig;
  sourceId: string;
  id?: string;
}
/**
 * @controlToWidget Controller associated widgets
 * @controlToControl Controller associated Controller visible cascade
 * @widgetToWidget widget inOther WidgetContainer linkage
 * */
export interface RelationConfig {
  type: RelationConfigType;
  controlToWidget?: {
    widgetRelatedViewIds: string[];
  };
  widgetToWidget?: {
    sameView: boolean;
    triggerColumn: string;
    linkerColumn: string;
  };
}
export type RelationConfigType =
  | 'controlToWidget' // control - ChartFetch will del
  | 'controlToChartFetch' // control - ChartFetch
  | 'controlToControl' // control - control -visible  will del
  | 'controlToControlVisible' // control - control -visible
  | 'controlToControlCascade' // control - control -Cascade
  | 'widgetToWidget' // linkage will del
  | 'chartToChartLinkage'; // linkage
export interface RelatedView {
  viewId: string;
  relatedCategory: ChartDataViewFieldCategory;
  fieldValue: string | number | Date | undefined | string[] | number[] | Date[];
  fieldValueType: DataViewFieldType | undefined;
}
export interface ServerRelation extends Omit<Relation, 'config'> {
  config: string;
}

/*
 * 通用
 */

export interface ChartWidgetContent {
  type: WidgetContentChartType;
  dataChart?: DataChart;
}
export interface BoardBtnContent {
  type: any;
}
// 媒体组件配置
export type MediaWidgetContent = {
  type: MediaWidgetType;
  richTextConfig?: {
    content: DeltaStatic;
  };
  timerConfig?: {
    time: {
      timeDuration: number; // 定时器刷新时间
      timeFormat: string; //
    };
    font: {
      color: string;
      fontFamily: string; //'PingFang SC';
      fontSize: string; //'24';
      fontStyle: string; //'normal';
      fontWeight: string; //'normal';
    };
  };
  imageConfig?: {
    type: 'WIDGET_SIZE' | 'IMAGE_RATIO';
    src: string;
  };
  videoConfig?: {
    src: string | undefined;
  };
  iframeConfig?: {
    src: string | undefined;
  };
};
// 容器组件配置
// export type ContainerWidgetContent = {
//   type: ContainerWidgetType;
//   itemMap: Record<string, ContainerItem>;
//   tabConfig?: any;
//   carouselConfig?: any;
// };
export type TabWidgetContent = {
  itemMap: Record<string, ContainerItem>;
};

export interface ContainerItem {
  index: number;
  name: string;
  tabId: string;
  childWidgetId: string;
}
// 控制器组件配置
export interface ControllerWidgetContent {
  type: ControllerFacadeTypes;
  name: string;
  relatedViews: RelatedView[];
  config: ControllerConfig;
}

export const WidgetTypes = [
  'chart',
  'media',
  'container',
  'controller',
  'button',
  'group',
] as const;
export type WidgetType = typeof WidgetTypes[number];

export declare const ContainerWidgetTypes: ['tab', 'carousel'];

export type LightWidgetType =
  | ContainerWidgetType
  | MediaWidgetType
  | ControllerFacadeTypes;

export type ContainerWidgetType = typeof ContainerWidgetTypes[number];

/**
 * widgetChart 属于board 内部 配置存在widget 表内,
 * 没有真实的datachartId
 */
export const ChartWidgetTypes = ['dataChart', 'widgetChart'] as const;
export type WidgetContentChartType = typeof ChartWidgetTypes[number];
export declare const MediaWidgetTypes: [
  'richText',
  'timer',
  'image',
  'video',
  'iframe',
];

export type MediaWidgetType = typeof MediaWidgetTypes[number];

export interface RectConfig {
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface BackgroundConfig {
  color: string;
  image: string;
  size: 'auto' | 'contain' | 'cover' | '100% 100%';
  repeat: 'repeat' | 'repeat-x' | 'repeat-y' | 'no-repeat';
}

export interface LineConfig {
  width: number;
  style: BorderStyleType;
  color: string;
}

export interface BorderConfig extends LineConfig {
  radius: number;
}

//

export interface DataChart {
  config: DataChartConfig;
  description: string;
  id: string;
  name: string;
  orgId?: string;
  projectId?: string;
  publish?: boolean; //？
  type?: string; //?
  viewId: string;
  view?: any;
  status: any;
}
export interface DataChartConfig {
  aggregation: boolean | undefined;
  chartConfig: ChartConfig;
  chartGraphId: string;
  computedFields: any[];
  sampleData?: any; // for template
}

export type ColsType = typeof LAYOUT_COLS_MAP;

// Dashboard view model
export interface BoardInfo {
  id: string;
  saving: boolean;
  loading: boolean;
  dragging?: boolean;
  editing: boolean;
  visible: boolean; // 当组件容器不显示的时候不应该请求数据
  fullScreenItemId: string; // 全屏状态
  showBlockMask: boolean; //?
  isDroppable: boolean;
  clipboardWidgetMap: Record<string, WidgetOfCopy>;
  layouts: Layout[];
  deviceType: DeviceType; // deviceType for autoBoard defaultValue = desktop
  widgetIds: string[]; // board保存的时候 区分那些是删除的，哪些是新增的
  controllerPanel: WidgetControllerPanelParams; //
  linkFilter: BoardLinkFilter[];
  chartEditorProps?: ChartEditorProps;
  needFetchItems: string[];
  hasFetchItems: string[];
  boardWidthHeight: [number, number];
  originControllerWidgets: Widget[]; // use for reset button
}
export enum DeviceType {
  Desktop = 'Desktop',
  Mobile = 'Mobile',
}
export interface BoardLinkFilter {
  triggerWidgetId: string;
  triggerValue: string;
  triggerDataChartId: string;
  linkerWidgetId: string;
}
/**
 * @description 'filter'
 */

export interface WidgetControllerPanelParams {
  type: 'add' | 'edit' | 'hide';
  widgetId: string;
  controllerType?: ControllerFacadeTypes;
}

export interface ServerDatachart extends Omit<DataChart, 'config'> {
  config: string;
}

export interface getDataOption {
  pageInfo?: Partial<PageInfo>;
  sorters?: Array<{ column: string; operator?: string; aggOperator?: string }>;
}

export type WidgetErrorType = 'request' | 'interaction';

export type WidgetLinkInfo = {
  sourceWidgetId?: string;
  filters?: PendingChartDataRequestFilter[];
  tempFilters?: PendingChartDataRequestFilter[];
  variables?: Record<string, any[]>;
};
