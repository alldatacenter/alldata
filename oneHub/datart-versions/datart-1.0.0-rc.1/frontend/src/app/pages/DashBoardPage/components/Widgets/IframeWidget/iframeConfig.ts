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
import { ORIGINAL_TYPE_MAP } from 'app/pages/DashBoardPage/constants';
import type {
  WidgetActionListItem,
  widgetActionType,
  WidgetMeta,
  WidgetProto,
  WidgetToolkit,
} from 'app/pages/DashBoardPage/types/widgetTypes';
import { getJsonConfigs } from 'app/pages/DashBoardPage/utils';
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

const initIframeTpl = () => {
  return {
    label: 'iframe.iframeGroup',
    key: 'iframeGroup',
    comType: 'group',
    rows: [
      {
        label: 'iframe.src',
        key: 'src',
        value: '/login', //https://www.oschina.net/p/datart, http://www.retech.cc/product/datart
        comType: 'input',
      },
    ],
  };
};
const iframeI18N = {
  zh: {
    iframeGroup: '嵌入页配置',
    src: '嵌入地址', //资源？
  },
  en: {
    iframeGroup: 'Iframe Config',
    src: 'URL', // Source?
  },
};
const NameI18N = {
  zh: '嵌入页',
  en: 'Embed',
};
export const widgetMeta: WidgetMeta = {
  icon: 'embed-widget',
  originalType: ORIGINAL_TYPE_MAP.iframe,
  canWrapped: true,
  controllable: false,
  linkable: false,
  canFullScreen: true,
  singleton: false,

  i18ns: [
    {
      lang: 'zh-CN',
      translation: {
        desc: 'iframe',
        widgetName: NameI18N.zh,
        action: {},
        title: TitleI18N.zh,
        iframe: iframeI18N.zh,
        background: { backgroundGroup: '背景' },
        padding: PaddingI18N.zh,
        border: { borderGroup: '边框' },
      },
    },
    {
      lang: 'en-US',
      translation: {
        desc: 'iframe',
        widgetName: NameI18N.en,
        action: {},
        title: TitleI18N.en,
        iframe: iframeI18N.en,
        background: { backgroundGroup: 'Background' },
        padding: PaddingI18N.en,
        border: { borderGroup: 'Border' },
      },
    },
  ],
};
export interface IframeWidgetToolKit extends WidgetToolkit {
  getIframe: (props) => {
    src: string;
  };
}
const widgetToolkit: IframeWidgetToolKit = {
  create: opt => {
    const widget = widgetTpl();
    widget.id = widgetMeta.originalType + widget.id;
    widget.parentId = opt.parentId || '';
    widget.datachartId = opt.datachartId || '';
    widget.viewIds = opt.viewIds || [];
    widget.relations = opt.relations || [];
    widget.config.originalType = widgetMeta.originalType;
    widget.config.type = 'media';
    widget.config.name = opt.name || '';

    widget.config.customConfig.props = [
      { ...initIframeTpl() },
      { ...initTitleTpl() },
      { ...initBackgroundTpl() },
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
  getIframe(props) {
    const [src] = getJsonConfigs(props, ['iframeGroup'], ['src']);
    return {
      src,
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
// export const getWidgetIframe = props => {
//   const [src] = getJsonConfigs(props, ['iframeGroup'], ['src']);
//   return {
//     src,
//   };
// };
const iframeProto: WidgetProto = {
  originalType: widgetMeta.originalType,
  meta: widgetMeta,
  toolkit: widgetToolkit,
};
export const iframeWidgetToolKit = widgetToolkit;
export default iframeProto;
