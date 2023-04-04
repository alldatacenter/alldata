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
import { FONT_DEFAULT } from 'app/constants';
import {
  ORIGINAL_TYPE_MAP,
  TimeDefault,
} from 'app/pages/DashBoardPage/constants';
import type {
  WidgetActionListItem,
  widgetActionType,
  WidgetMeta,
  WidgetProto,
  WidgetToolkit,
} from 'app/pages/DashBoardPage/types/widgetTypes';
import { getJsonConfigs } from 'app/pages/DashBoardPage/utils';
import { WHITE } from 'styles/StyleConstants';
import { IFontDefault } from '../../../../../../types';
import { ITimeDefault } from '../../../types/widgetTypes';
import {
  initBackgroundTpl,
  initBorderTpl,
  initPaddingTpl,
  initTitleTpl,
  initWidgetName,
  PaddingI18N,
  TitleI18N,
  widgetTpl,
} from '../../WidgetManager/utils/init';

const initTimerTpl = () => {
  return {
    label: 'timer.timerGroup',
    key: 'timerGroup',
    comType: 'group',
    rows: [
      {
        label: 'timer.time',
        key: 'time',
        value: TimeDefault,
        comType: 'timerFormat',
      },
      {
        label: 'timer.font',
        key: 'font',
        value: { ...FONT_DEFAULT, fontSize: '20' },
        comType: 'font',
      },
    ],
  };
};
const timerI18N = {
  zh: {
    timerGroup: '时间配置',
    time: '时间',
    font: '字体',
  },
  en: {
    timerGroup: 'Timer Config',
    time: 'Time',
    font: 'Font',
  },
};
const NameI18N = {
  zh: '时钟',
  en: 'Timer',
};
export const widgetMeta: WidgetMeta = {
  icon: 'time-widget',
  originalType: ORIGINAL_TYPE_MAP.timer,
  canWrapped: true,
  controllable: false,
  linkable: false,
  canFullScreen: true,
  singleton: false,

  i18ns: [
    {
      lang: 'zh-CN',
      translation: {
        desc: 'timer',
        widgetName: NameI18N.zh,
        action: {},
        title: TitleI18N.zh,
        background: { backgroundGroup: '背景' },
        padding: PaddingI18N.zh,
        timer: timerI18N.zh,
        border: { borderGroup: '边框' },
      },
    },
    {
      lang: 'en-US',
      translation: {
        desc: 'timer',
        widgetName: NameI18N.en,
        action: {},
        title: TitleI18N.en,
        background: { backgroundGroup: 'Background' },
        padding: PaddingI18N.en,
        timer: timerI18N.en,
        border: { borderGroup: 'Border' },
      },
    },
  ],
};
export interface TimerWidgetToolKit extends WidgetToolkit {
  getTimer: (props) => {
    time: ITimeDefault;
    font: IFontDefault;
  };
}
export const widgetToolkit: TimerWidgetToolKit = {
  create: opt => {
    const widget = widgetTpl();
    widget.id = widgetMeta.originalType + widget.id;
    widget.parentId = opt.parentId || '';
    widget.viewIds = opt.viewIds || [];
    widget.relations = opt.relations || [];
    widget.config.originalType = widgetMeta.originalType;
    widget.config.type = 'media';
    widget.config.name = opt.name || '';
    widget.config.rect.height = 100;
    widget.config.pRect.height = 3;

    widget.config.customConfig.props = [
      { ...initTimerTpl() },
      { ...initTitleTpl() },
      { ...initBackgroundTpl(WHITE) },
      { ...initPaddingTpl() },
      { ...initBorderTpl() },
    ];

    return widget;
  },
  getName(key) {
    return initWidgetName(NameI18N, key);
  },
  edit() {},
  save() {},
  getDropDownList(...arg) {
    const list: WidgetActionListItem<widgetActionType>[] = [
      {
        key: 'edit',
        renderMode: ['edit'],
      },
      {
        key: 'delete',
        renderMode: ['edit'],
      },
      {
        key: 'lock',
        renderMode: ['edit'],
      },
      {
        key: 'group',
        renderMode: ['edit'],
      },
    ];
    return list;
  },
  getTimer(props) {
    const [time, font] = getJsonConfigs(
      props,
      ['timerGroup'],
      ['time', 'font'],
    ) as [ITimeDefault, IFontDefault];
    return {
      time,
      font,
    };
  },
  // lock() {},
  // unlock() {},
  // copy() {},
  // paste() {},
  // delete() {},
  // changeTitle() {},
  // getMeta() {},
  // getWidgetName() {},
  // //
};

const timerProto: WidgetProto = {
  originalType: widgetMeta.originalType,
  meta: widgetMeta,
  toolkit: widgetToolkit,
};
export const timerWidgetToolkit = widgetToolkit;
export default timerProto;
