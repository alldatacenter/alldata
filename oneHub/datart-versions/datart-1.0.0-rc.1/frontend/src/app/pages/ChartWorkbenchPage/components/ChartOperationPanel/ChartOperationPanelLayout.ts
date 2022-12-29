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

import { IJsonModel } from 'flexlayout-react';

export enum LayoutComponentType {
  CONTROL = 'ChartPresentControllerPanel',
  PRESENT = 'ChartPresentWrapper',
  VIEW = 'ChartDataViewPanel',
  CONFIG = 'ChartConfigPanel',
}

const layoutConfig: IJsonModel = {
  global: {
    tabEnableFloat: true,
    tabEnableClose: false,
    tabSetEnableTabStrip: false,
    splitterSize: 2,
  },
  layout: {
    type: 'row',
    id: 'container',
    children: [
      {
        type: 'tabset',
        id: 'model-dragbar',
        width: 256,
        children: [
          {
            type: 'tab',
            id: 'model-dragbar-component',
            component: LayoutComponentType.VIEW,
          },
        ],
      },
      {
        type: 'tabset',
        id: 'config',
        width: 360,
        children: [
          {
            type: 'tab',
            id: 'config-component',
            component: LayoutComponentType.CONFIG,
          },
        ],
      },
      {
        type: 'tabset',
        id: 'present',
        children: [
          {
            type: 'tab',
            id: 'present-wrapper',
            component: LayoutComponentType.PRESENT,
          },
        ],
      },
    ],
  },
};

export default layoutConfig;
