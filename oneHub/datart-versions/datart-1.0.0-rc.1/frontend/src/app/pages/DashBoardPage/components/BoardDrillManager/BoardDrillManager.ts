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
import { ChartDrillOption } from 'app/models/ChartDrillOption';
type WidgetDrillMap = Record<string, ChartDrillOption | undefined>;
export const EDIT_PREFIX = '@EDIT@';
export class BoardDrillManager {
  private static _manager: BoardDrillManager;
  private boardMap: Record<string, WidgetDrillMap> = {};
  private constructor() {}

  public static getInstance() {
    if (!this._manager) {
      this._manager = new BoardDrillManager();
    }
    return this._manager;
  }

  public getWidgetDrill(obj: { bid: string; wid: string }) {
    const { bid, wid } = obj;
    const widgetDrill = this.boardMap?.[bid]?.[wid];
    return widgetDrill;
  }
  public setWidgetDrill(obj: {
    bid: string;
    wid: string;
    drillOption?: ChartDrillOption;
  }) {
    const { bid, wid, drillOption } = obj;

    const boardMap = this.boardMap?.[bid];
    if (!boardMap) {
      this.boardMap[bid] = { [wid]: undefined };
    }
    if (!drillOption) {
      delete this.boardMap?.[bid]?.[wid];
      return;
    }
    this.boardMap[bid][wid] = drillOption;
  }

  public clearMapByBoardId(bid: string) {
    delete this.boardMap[bid];
  }
}

export const boardDrillManager = BoardDrillManager.getInstance();
