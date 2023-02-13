// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import { ILineStyle } from 'application/common/components/dice-yaml-canvas';
import { IDiceYamlEditorItem } from './dice-yaml-editor-item';

export enum DiceFlowType {
  PIPELINE = 'PIPELINE',
  DATA_MARKET = 'DATA_MARKET',
  EDITOR = 'EDITOR',
}

export interface IMovingPoints {
  from: number;
  to: number;
}

export interface IDiceFlowPointComponent {
  meta: {
    ITEM_WIDTH: number;
    ITEM_HEIGHT: number;
    PADDING_TOP: number;
    PADDING_LEFT: number;
    ITEM_MARGIN_BOTTOM: number;
    ITEM_MARGIN_RIGHT: number;
    RX: number;
  };
}

export interface IDiceFlowDataSource {
  centerItems: IDiceYamlEditorItem[];
  dependsItems: IDiceYamlEditorItem[];
}

export interface IDiceYamlEditorProps {
  /**
   * 是否支持同级节点连接
   */
  connectSiblingNode?: boolean;
  /**
   * 图标类型
   */
  type: DiceFlowType;
  /**
   * 线的样式
   */
  lineStyle?: ILineStyle;
  /**
   * 是否选中过节点
   */
  isSelectedItem?: any;
  /**
   * 是否为编辑状态
   */
  editing: boolean;
  /**
   * 需要移动效果的点
   */
  movingPoints?: IMovingPoints[];
  /**
   * 数据源
   */
  dataSource: any;
  /**
   * 删除节点
   */
  deleteItem?: (service: IDiceYamlEditorItem) => void;
  /**
   * 点击节点
   */
  clickItem?: (service: IDiceYamlEditorItem, type?: string) => void;
  /**
   * 更新连接关系
   */
  updateConnect?: (fromService: string, toService: string) => void;
  /**
   * 创建新的节点, todo: 名字改下
   */
  getCreateView?: (service: any, parentName: string) => void;
}

export interface IPosition {
  x: number;
  y: number;
}

export interface IEditorPoint extends IDiceYamlEditorItem {
  id: number;
  groupIndex: number;
  index: number;
  status: 'moving' | 'normal';
  position: IPosition;
}

export interface IControlPointPolyline extends IPosition {
  a: string;
}

export interface IControlPointLine {
  from: IPosition;
  to: IPosition;
}

export interface INodeLine extends IControlPointLine {
  id: string;
  type: 'line' | 'polyline' | 'polylineAcrossPoint';
  fromPoint: IEditorPoint;
  toPoint?: IEditorPoint;
  controlPoint?: IControlPointPolyline[] | IControlPointLine;
}
