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

import type { WidgetProto } from '../../types/widgetTypes';

class WidgetManager {
  private static _instance: WidgetManager;
  private widgetProtoMap: Record<string, WidgetProto> = {};
  private constructor() {}

  public static getInstance() {
    if (!this._instance) {
      this._instance = new WidgetManager();
    }
    return this._instance;
  }

  public register(obj: {
    originalType: string;
    meta: WidgetProto['meta'];
    toolkit: WidgetProto['toolkit'];
  }) {
    if (this.widgetProtoMap[obj.originalType]) {
      console.warn(`Widget ${obj.originalType} already registered`);
    }
    this.widgetProtoMap[obj.originalType] = {
      originalType: obj.originalType,
      meta: obj.meta,
      toolkit: obj.toolkit,
    };
  }
  public allWidgetProto() {
    return this.widgetProtoMap;
  }
  public widgetProto(originalType: string) {
    return this.widgetProtoMap[originalType];
  }
  public meta(originalType: string) {
    return this.widgetProtoMap[originalType]?.meta;
  }

  public toolkit(originalType: string) {
    return this.widgetProtoMap[originalType]?.toolkit;
  }
}
export const widgetManagerInstance = WidgetManager.getInstance();
