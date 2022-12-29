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
  Widget,
  WidgetCreateProps,
  WidgetMeta,
} from 'app/pages/DashBoardPage/types/widgetTypes';
import { WHITE } from 'styles/StyleConstants';
import widgetManagerInstance from '../../WidgetManager';
import {
  initBackgroundTpl,
  initBorderTpl,
  initInteractionTpl,
  initLoopFetchTpl,
  initPaddingTpl,
  initTitleTpl,
  InteractionI18N,
  LoopFetchI18N,
  PaddingI18N,
  TitleI18N,
  widgetTpl,
} from '../../WidgetManager/utils/init';

export const getMeta = (opt: {
  icon: any;
  widgetTypeId: string;

  zh: {
    desc: string;
    widgetName: string;
  };
  en: {
    desc: string;
    widgetName: string;
  };
}) => {
  const meta: WidgetMeta = {
    icon: opt.icon,
    originalType: opt.widgetTypeId,
    canWrapped: true,
    controllable: true,
    linkable: true,
    singleton: false,

    canFullScreen: true,

    i18ns: [
      {
        lang: 'zh-CN',
        translation: {
          ...InteractionI18N.zh,
          desc: opt.zh.desc,
          widgetName: opt.zh.widgetName,
          action: {},
          title: TitleI18N.zh,
          background: { backgroundGroup: '背景' },
          padding: PaddingI18N.zh,
          loopFetch: LoopFetchI18N.zh,
          border: { borderGroup: '边框' },
        },
      },
      {
        lang: 'en-US',
        translation: {
          ...InteractionI18N.en,
          desc: opt.en.desc,
          widgetName: opt.en.widgetName,
          action: {},
          title: TitleI18N.en,
          background: { backgroundGroup: 'Background' },
          padding: PaddingI18N.en,
          loopFetch: LoopFetchI18N.en,
          border: { borderGroup: 'Border' },
        },
      },
    ],
  };
  return meta;
};

export const dataChartCreator = (opt: WidgetCreateProps) => {
  const widget = widgetTpl();
  widget.parentId = opt.parentId || '';
  widget.datachartId = opt.datachartId || '';
  widget.viewIds = opt.viewIds || [];
  widget.relations = opt.relations || [];
  widget.config.type = 'chart';

  widget.config.content.dataChart = opt.content; // DataChart
  widget.config.name = opt.name || opt.content?.name;
  widget.config.customConfig.props = [
    { ...initTitleTpl() },
    { ...initLoopFetchTpl() },
    { ...initPaddingTpl() },
    { ...initBackgroundTpl(opt.boardType === 'auto' ? WHITE : '') },
    { ...initBorderTpl() },
  ];
  widget.config.customConfig.props.forEach(ele => {
    if (ele.key === 'titleGroup') {
      ele.rows?.forEach(row => {
        if (row.key === 'showTitle') {
          row.value = true;
        }
      });
    }
  });
  widget.config.customConfig.interactions = [...initInteractionTpl()];

  return widget;
};
export const getCanLinkageWidgets = (widgets: Widget[]) => {
  const canLinkWidgets = widgets.filter(widget => {
    const linkable = widgetManagerInstance.meta(
      widget.config.originalType,
    ).linkable;
    if (!linkable) return false;
    if (widget.viewIds.length === 0) return false;
    return true;
  });
  return canLinkWidgets;
};
