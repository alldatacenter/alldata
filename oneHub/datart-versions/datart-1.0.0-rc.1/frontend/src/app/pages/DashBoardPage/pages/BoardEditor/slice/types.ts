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
  BoardInfo,
  Dashboard,
  WidgetData,
  WidgetInfo,
} from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { Widget } from 'app/pages/DashBoardPage/types/widgetTypes';
import { SelectedItem } from 'app/types/ChartConfig';
import { StateWithHistory } from 'redux-undo';

export interface EditBoardState {
  stack: EditBoardStack;
  boardInfo: BoardInfo;
  widgetInfoRecord: Record<string, WidgetInfo>;
  widgetDataMap: Record<string, WidgetData | undefined>;
  selectedItemsMap: {
    selectedItems: Record<string, SelectedItem[]>;
  };
}
export interface HistoryEditBoard extends Omit<EditBoardState, 'stack'> {
  stack: StateWithHistory<EditBoardStack>;
}

export interface EditBoardStack {
  dashBoard: Dashboard;
  widgetRecord: Record<string, Widget>;
}
