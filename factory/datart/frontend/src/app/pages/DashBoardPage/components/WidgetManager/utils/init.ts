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
import { APP_CURRENT_VERSION } from 'app/migration/constants';
import { LAYOUT_COLS_MAP } from 'app/pages/DashBoardPage/constants';
import type {
  BoardType,
  RectConfig,
  WidgetType,
} from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { Widget } from 'app/pages/DashBoardPage/types/widgetTypes';
import type { ChartStyleConfig } from 'app/types/ChartConfig';
import { getInitialLocale } from 'locales/utils';
import { uuidv4 } from 'utils/utils';

export const initTitleTpl = () => {
  const titleTpl: ChartStyleConfig = {
    label: 'title.titleGroup',
    key: 'titleGroup',
    comType: 'group',
    rows: [
      {
        label: 'title.showTitle',
        key: 'showTitle',
        value: false,
        comType: 'switch',
      },
      {
        label: 'title.textAlign.textAlign',
        key: 'textAlign',
        value: 'left',
        comType: 'select',
        options: {
          translateItemLabel: true,
          items: [
            {
              label: '@global@.title.textAlign.left',
              key: 'left',
              value: 'left',
            },
            {
              label: '@global@.title.textAlign.center',
              key: 'center',
              value: 'center',
            },
          ],
        },
      },
      {
        label: 'title.font',
        key: 'font',
        value: FONT_DEFAULT,
        comType: 'font',
      },
    ],
  };
  return titleTpl;
};

export const initInteractionTpl = () => {
  return [
    {
      label: 'drillThrough.title',
      key: 'drillThrough',
      comType: 'checkboxModal',
      default: false,
      options: { modalSize: 'middle' },
      rows: [
        {
          label: 'drillThrough.title',
          key: 'setting',
          comType: 'interaction.drillThrough',
        },
      ],
    },
    {
      label: 'crossFiltering.title',
      key: 'crossFiltering',
      comType: 'checkboxModal',
      default: false,
      options: { modalSize: 'middle' },
      rows: [
        {
          label: 'crossFiltering.title',
          key: 'setting',
          comType: 'interaction.crossFiltering',
        },
      ],
    },
    {
      label: 'viewDetail.title',
      key: 'viewDetail',
      comType: 'checkboxModal',
      default: false,
      options: { modalSize: 'middle' },
      rows: [
        {
          label: 'viewDetail.title',
          key: 'setting',
          comType: 'interaction.viewDetail',
        },
      ],
    },
  ];
};

export const InteractionI18N = {
  zh: {
    drillThrough: {
      title: '跳转设置',
      rule: {
        title: '规则设置',
        tip: '分享页仅支持跳转到URL一种方式',
        addRule: '+ 点击新建',
        inputUrl: '请输入URL',
        header: {
          name: '名称',
          category: '类型',
          open: '打开方式',
          relation: '关系设置',
          operation: '操作',
          event: '交互事件',
        },
        event: {
          title: '交互事件',
          left: '左键单击',
          right: '右键菜单',
        },
        category: {
          title: '类型',
          jumpToChart: '跳转到图表',
          jumpToDashboard: '跳转到仪表板',
          jumpToUrl: '跳转到URL',
        },
        action: {
          title: '打开方式',
          redirect: '从当前页打开',
          window: '打开新窗口',
          dialog: '打开对话框',
        },
        reference: {
          title: '关联图表',
        },
        relation: {
          setting: '字段关系配置',
          title: '字段关系',
          auto: '自动',
          customize: '自定义',
          type: '类型',
          source: '源字段/变量',
          target: '目标字段/变量',
          addRelation: '+ 点击新建',
          field: '字段',
          variable: '变量',
          controller: '控制器',
        },
        operation: {
          delete: '删除',
        },
      },
    },
    crossFiltering: {
      title: '联动',
      event: {
        title: '交互事件',
        left: '左键单击',
        right: '右键菜单',
      },
      rule: {
        title: '规则设置',
        header: {
          relId: '关联图表',
          relation: '关系设置',
          operation: '操作',
        },
        operation: {
          delete: '删除',
        },
      },
    },
    viewDetail: {
      title: '查看数据',
      event: '交互事件',
      leftClick: '左键单击',
      rightClick: '右键菜单',
      field: '选择字段',
      all: '全部',
      customize: '自定义',
      summary: '汇总',
      details: '明细',
    },
  },
  en: {
    drillThrough: {
      title: 'Drill Through',
      rule: {
        title: 'Rule',
        tip: 'Drill through only support jump to url',
        addRule: '+ Add New',
        inputUrl: 'Please input url',
        header: {
          name: 'Name',
          category: 'Category',
          open: 'Open',
          relation: 'Relation',
          operation: 'Operation',
          event: 'Event',
        },
        event: {
          title: 'Event',
          left: 'Left',
          right: 'Right',
        },
        category: {
          title: 'Categroy',
          jumpToChart: 'Jump to Chart',
          jumpToDashboard: 'Jump to Dashboard',
          jumpToUrl: 'Jump to URL',
        },
        action: {
          title: 'Open',
          redirect: 'Redirect',
          window: 'Open New',
          dialog: 'Open Dialog',
        },
        reference: {
          title: 'Reference View',
        },
        relation: {
          setting: 'Field Relation Setting',
          title: 'Field Relation',
          auto: 'Auto',
          customize: 'Customize',
          type: 'Type',
          source: 'Source Field/Variable',
          target: 'Target Field/Variable',
          addRelation: '+ Add',
          field: 'Field',
          variable: 'Variable',
          controller: 'Controller',
        },
        operation: {
          delete: 'Delete',
        },
      },
    },
    crossFiltering: {
      title: 'Cross Filtering',
      event: {
        title: 'Event',
        left: 'Left',
        right: 'Right',
      },
      rule: {
        title: 'Rule',
        header: {
          relId: 'Reference Chart',
          relation: 'Relation',
          operation: 'Operation',
        },
        operation: {
          delete: 'Delete',
        },
      },
    },
    viewDetail: {
      title: 'View Detail',
      event: 'Interaction Event',
      leftClick: 'Left Click',
      rightClick: 'Right Click',
      field: 'Selected Fields',
      all: 'All',
      customize: 'Customize',
      summary: 'Summary',
      details: 'Details',
    },
  },
};

export const TitleI18N = {
  zh: {
    titleGroup: '标题格式',
    title: '标题',
    showTitle: '显示标题',
    font: '字体',
    textAlign: {
      textAlign: '对齐方式',
      left: '左对齐',
      center: '居中',
    },
  },
  en: {
    titleGroup: 'Title Format',
    title: 'Title',
    showTitle: 'Show Title',
    font: 'Font',
    textAlign: {
      textAlign: 'Align',
      left: 'Left',
      center: 'Center',
    },
  },
};
export const initPaddingTpl = () => {
  const paddingTpl: ChartStyleConfig = {
    label: 'padding.paddingGroup',
    key: 'paddingGroup',
    comType: 'group',
    rows: [
      {
        label: 'padding.top',
        key: 'top',
        value: 8,
        comType: 'inputNumber',
      },
      {
        label: 'padding.bottom',
        key: 'bottom',
        value: 8,
        comType: 'inputNumber',
      },
      {
        label: 'padding.left',
        key: 'left',
        value: 8,
        comType: 'inputNumber',
      },
      {
        label: 'padding.right',
        key: 'right',
        value: 8,
        comType: 'inputNumber',
      },
    ],
  };
  return paddingTpl;
};

export const PaddingI18N = {
  zh: {
    paddingGroup: '内边距',
    top: '上',
    bottom: '下',
    left: '左',
    right: '右',
  },
  en: {
    paddingGroup: 'Padding',
    top: 'Top',
    bottom: 'Bottom',
    left: 'Left',
    right: 'Right',
  },
};
export const initLoopFetchTpl = () => {
  const loopFetchTpl: ChartStyleConfig = {
    label: 'loopFetch.loopFetchGroup',
    key: 'loopFetchGroup',
    comType: 'group',
    rows: [
      {
        label: 'loopFetch.enable',
        key: 'enable',
        value: false,
        comType: 'switch',
      },
      {
        label: 'loopFetch.interval',
        key: 'interval',
        value: 60, //60s
        comType: 'inputNumber',
      },
    ],
  };
  return loopFetchTpl;
};

export const LoopFetchI18N = {
  zh: {
    loopFetchGroup: '自动刷新数据',
    enable: '启用',
    interval: '间隔(s)',
  },
  en: {
    loopFetchGroup: 'Loop Fetch',
    enable: 'Enable',
    interval: 'Interval (s)',
  },
};
export const ImmediateQueryI18N = {
  zh: {
    immediateQueryGroup: '立即查询',
    enable: '启用',
  },
  en: {
    immediateQueryGroup: 'ImmediateQuery',
    enable: 'Enable',
  },
};
export const initBackgroundTpl = (color?: string) => {
  const backgroundTpl: ChartStyleConfig = {
    label: 'background.backgroundGroup',
    key: 'backgroundGroup',
    comType: 'group',
    rows: [
      {
        label: 'background.background',
        key: 'background',
        value: {
          color: color || 'transparent', // TODO 根据当前主题色配置
          image: '',
          size: '100% 100%',
          repeat: 'no-repeat',
        },
        comType: 'background',
      },
    ],
  };
  return backgroundTpl;
};
export const initBorderTpl = () => {
  const borderTpl: ChartStyleConfig = {
    label: 'border.borderGroup',
    key: 'borderGroup',
    comType: 'group',
    rows: [
      {
        label: 'border.border',
        key: 'border',
        value: {
          color: 'transparent', // TODO 根据当前主题色配置
          width: 0,
          style: 'solid',
          radius: 0,
        },
        comType: 'widgetBorder',
      },
    ],
  };
  return borderTpl;
};

// TODO(Stephen): to be check if width/height setting same as free widget?
export const initAutoWidgetRect = (): RectConfig => ({
  x: 0,
  y: 0,
  width: LAYOUT_COLS_MAP.lg / 2, // NOTE: auto board use grid system, the total is 12, default is half panel, means 6
  height: LAYOUT_COLS_MAP.lg / 2,
});
export const initFreeWidgetRect = (): RectConfig => ({
  x: Math.ceil(Math.random() * 200),
  y: Math.ceil(Math.random() * 200),
  width: 400,
  height: 300,
});

export const widgetTpl = (): Widget => {
  return {
    id: uuidv4(),
    dashboardId: '',
    datachartId: '',
    relations: [],
    viewIds: [],
    parentId: '',
    config: {
      clientId: initClientId(),
      version: APP_CURRENT_VERSION,
      index: 0,
      name: '',
      boardType: '' as BoardType,
      type: '' as WidgetType,
      originalType: '',

      // visible: true,
      lock: false,
      content: {} as any,
      //
      rect: initFreeWidgetRect(),
      pRect: initAutoWidgetRect(),
      mRect: undefined,
      customConfig: {
        props: [],
      },
    },
  };
};

export const initClientId = () => {
  return 'client_' + uuidv4();
};
export const initWidgetName = (i18nMap: object, local?: string) => {
  if (local && i18nMap[local]) {
    return i18nMap[local];
  } else {
    let key = getInitialLocale();
    if (i18nMap[key]) return i18nMap[key];
    return Object.values(i18nMap)?.[0];
  }
};
