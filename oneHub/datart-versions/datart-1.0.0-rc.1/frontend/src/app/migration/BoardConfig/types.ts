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
  BackgroundConfig,
  BorderConfig,
  JumpConfig,
  LinkageConfig,
  RectConfig,
  Relation,
  WidgetPadding,
  WidgetTitleConfig,
} from 'app/pages/DashBoardPage/pages/Board/slice/types';

export const WidgetTypesBeta3 = [
  'chart',
  'media',
  'container',
  'controller',
  'query',
  'reset',
] as const;
export type WidgetTypeBeta3 = typeof WidgetTypesBeta3[number];
export interface WidgetBeta3 {
  id: string;
  dashboardId: string;
  datachartId: string;
  relations: Relation[];
  viewIds: string[];
  config: WidgetConfBeta3;
  parentId?: string;
}
export interface WidgetConfBeta3 {
  version: string;
  index: number;
  tabId?: string; //记录在父容器tab的位置
  name: string;
  nameConfig: WidgetTitleConfig;
  padding: WidgetPadding;
  type: WidgetTypeBeta3;
  autoUpdate: boolean;
  frequency: number; // 定时同步频率
  rect: RectConfig; //desktop_rect
  lock: boolean; //Locking forbids dragging resizing
  mobileRect?: RectConfig; //mobile_rect 移动端适配
  background: BackgroundConfig;
  border: BorderConfig;
  // content: WidgetContent;
  content: any;
  tabIndex?: number; // 在tab 等容器widget里面的排序索引
  linkageConfig?: LinkageConfig; //联动设置
  jumpConfig?: JumpConfig; // 跳转 设置
}
