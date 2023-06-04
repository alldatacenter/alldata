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
import { BoardType } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import {
  ITimeDefault,
  Widget,
} from 'app/pages/DashBoardPage/types/widgetTypes';
import { IFontDefault } from 'types';
import widgetManagerInstance from '../../../pages/DashBoardPage/components/WidgetManager';
import { RectConfig } from '../../../pages/DashBoardPage/pages/Board/slice/types';
import { WidgetBeta3 } from '../types';

const commonBeta4Convert = (newWidget: Widget, oldW: WidgetBeta3) => {
  newWidget.id = oldW.id;
  newWidget.config.index = oldW.config.index;
  newWidget.config.lock = oldW.config.lock;
  newWidget.config.rect = oldW.config.rect;
  newWidget.config.version = oldW.config.version;
  newWidget.dashboardId = oldW.dashboardId;
  // @ts-ignore
  newWidget.config.pRect = oldW.config.pRect;
  newWidget.config.content = oldW.config.content; //Todo
  newWidget.config.name = oldW.config.name;
  const oldConf = oldW.config;
  if (oldW.config.tabId) {
    newWidget.config.clientId = oldW.config.tabId;
  }

  if (oldW.config.mobileRect) {
    newWidget.config.mRect = oldW.config.mobileRect;
  }
  newWidget.config.customConfig.props?.forEach(prop => {
    // titleGroup name nameConfig
    if (prop.key === 'titleGroup') {
      const oNameConf = oldConf.nameConfig as any;
      prop.rows?.forEach(row => {
        if (row.key === 'showTitle') {
          row.value = oNameConf.show;
        }
        if (row.key === 'textAlign') {
          row.value = oNameConf.textAlign;
        }
        if (row.key === 'font') {
          row.value = {
            fontFamily: oNameConf.fontFamily,
            fontSize: oNameConf.fontSize,
            fontWeight: oNameConf.fontWeight,
            fontStyle: oNameConf.fontStyle,
            color: oNameConf.color,
          };
        }
      });
    }
    // paddingGroup
    if (prop.key === 'paddingGroup') {
      const oPad = oldConf.padding as any;
      prop.rows?.forEach(row => {
        if (row.key === 'top') {
          row.value = oPad.top;
        }
        if (row.key === 'right') {
          row.value = oPad.right;
        }
        if (row.key === 'bottom') {
          row.value = oPad.bottom;
        }
        if (row.key === 'left') {
          row.value = oPad.left;
        }
      });
    }
    // loopFetchGroup
    if (prop.key === 'loopFetchGroup') {
      prop.rows?.forEach(row => {
        if (row.key === 'enable') {
          row.value = oldConf.autoUpdate;
        }
        if (row.key === 'interval') {
          row.value = oldConf.frequency;
        }
      });
    }
    // backgroundGroup
    if (prop.key === 'backgroundGroup') {
      prop.rows?.forEach(row => {
        if (row.key === 'background') {
          row.value = oldConf.background;
        }
      });
    }
    // borderGroup
    if (prop.key === 'borderGroup') {
      prop.rows?.forEach(row => {
        if (row.key === 'border') {
          row.value = oldConf.border;
        }
      });
    }
  });
  return newWidget;
};
export const convertChartWidgetToBeta4 = (widget: WidgetBeta3) => {
  const subType = widget.config.content.type;
  let newWidget = {} as Widget;
  if (subType === 'dataChart') {
    newWidget = widgetManagerInstance
      .toolkit(ORIGINAL_TYPE_MAP.linkedChart)
      .create({ ...widget });
  } else {
    newWidget = widgetManagerInstance
      .toolkit(ORIGINAL_TYPE_MAP.ownedChart)
      .create({
        ...widget,
      });
  }

  newWidget = commonBeta4Convert(newWidget, widget);
  newWidget.config.jumpConfig = widget.config.jumpConfig;
  newWidget.config.linkageConfig = widget.config.linkageConfig;
  return newWidget;
};
export const convertContainerWidgetToBeta4 = (widget: WidgetBeta3) => {
  const subType = widget.config.content.type;
  if (subType === 'tab') {
    /**
       old data 
       {
         "itemMap": {
               "de693d55-04ef-497c-b465-a2420abe1e05": {
                   "tabId": "de693d55-04ef-497c-b465-a2420abe1e05",
                   "name": "私有图表_7",
                   "childWidgetId": "newWidget_0e970200-bc0e-4fc8-9de0-caea67e350f2",
                   "config": {}
               },
               "8f44ec1e-4602-4db1-9ec9-ab1ac8543251": {
                   "tabId": "8f44ec1e-4602-4db1-9ec9-ab1ac8543251",
                   "name": "图片_5",
                   "childWidgetId": "75e272ae570749468f94913cb9857203",
                   "config": {}
               }
           }
       }

       */
    /**
       new data 
       {
         "itemMap": {
            "client_b17323f7-8670-4c71-a43f-86a02359b2f5": {
                   "index": 1651831223742,
                    "name": "Image",
                    "tabId": "client_b17323f7-8670-4c71-a43f-86a02359b2f5",
                    "childWidgetId": "226c8f69c42b41109fdcfd84d7b8a2da"
          },
           "client_6cf95a9a-4052-4809-88f2-bad52aff1d24": {
                   "index": 1651833022936,
                   "name": "tab*",
                   "tabId": "client_6cf95a9a-4052-4809-88f2-bad52aff1d24",
                  "childWidgetId": "a4134a55-b79d-4518-92dc-0631678b98c6"
          }
      }
}

       */
    let newWidget = widgetManagerInstance.toolkit('tab').create({
      ...widget,
    });
    newWidget = commonBeta4Convert(newWidget, widget);

    const itemMap = newWidget.config.content?.itemMap;
    if (!itemMap) return newWidget;
    const tabItems = Object.values(itemMap) as any[];
    let newIndex = Number(Date.now());
    tabItems.forEach(item => {
      item.index = newIndex;
      delete item.config;
      newIndex++;
    });
    return newWidget;
  }
};
/**
 *
 *
 * @param {WidgetBeta3} widget
 * @return {*}
 */
export const convertMediaWidgetToBeta4 = (widget: WidgetBeta3) => {
  const subType = widget.config.content.type;
  if (subType === 'image') {
    let newWidget = widgetManagerInstance.toolkit('image').create({
      ...widget,
    });
    newWidget = commonBeta4Convert(newWidget, widget);
    newWidget.config.content = {};
    return newWidget;
  }
  if (subType === 'richText') {
    /**
     * old data
    {
    "content": {
        "type": "richText",
        "richTextConfig": {
            "content": {
                "ops": [
                    {
                        "insert": "\n"
                    }
                ]
            }
        }
    }
    }  
     */
    let newWidget = widgetManagerInstance.toolkit('richText').create({
      ...widget,
    });
    newWidget = commonBeta4Convert(newWidget, widget);
    // getWidgetIframe;
    newWidget.config.content = {
      richText: newWidget.config.content.richTextConfig,
    };
    return newWidget;
  }
  if (subType === 'iframe') {
    /**
     * old data
    {
    "content": {
        "type": "iframe",
        "iframeConfig": {
            "src": "https://ant.design/components/overview-cn/"
        }
    }
}
    */
    let newWidget = widgetManagerInstance.toolkit('iframe').create({
      ...widget,
    });
    newWidget = commonBeta4Convert(newWidget, widget);
    const oldConf = widget.config.content.iframeConfig;
    newWidget.config.customConfig.props?.forEach(prop => {
      // iframeGroup
      if (prop.key === 'iframeGroup') {
        prop.rows?.forEach(row => {
          if (row.key === 'src') {
            row.value = oldConf.src;
          }
        });
      }
    });
    newWidget.config.content = {};
    return newWidget;
  }

  if (subType === 'video') {
    let newWidget = widgetManagerInstance.toolkit('video').create({
      ...widget,
    });
    newWidget = commonBeta4Convert(newWidget, widget);
    newWidget.config.content = {};
    return newWidget;
  }
  if (subType === 'timer') {
    // return {
    //   label: 'timer.timerGroup',
    //   key: 'timerGroup',
    //   comType: 'group',
    //   rows: [
    //     {
    //       label: 'timer.time',
    //       key: 'time',
    //       value: TimeDefault,
    //       comType: 'timerFormat',
    //     },
    //     {
    //       label: 'timer.font',
    //       key: 'font',
    //       value: { ...FontDefault, fontSize: '20' },
    //       comType: 'font',
    //     },
    //   ],
    // };
    /**
    * old data
    "content": {
        "type": "timer",
        "timerConfig": {
            "time": {
                "timeFormat": "YYYY-MM-DD HH:mm:ss",
                "timeDuration": 1000
            },
            "font": {
                "fontFamily": "PingFang SC",
                "fontSize": "18",
                "fontWeight": "bolder",
                "fontStyle": "normal",
                "color": ""
            }
        }
    }
  */
    let newWidget = widgetManagerInstance.toolkit('timer').create({
      ...widget,
    });
    newWidget = commonBeta4Convert(newWidget, widget);

    const oldConf = widget.config.content.timerConfig;

    newWidget.config.customConfig.props?.forEach(prop => {
      // timerGroup
      if (!oldConf) return;
      if (prop.key === 'timerGroup') {
        prop.rows?.forEach(row => {
          if (row.key === 'time') {
            const newVal: ITimeDefault = {
              format: oldConf.time.timeFormat,
              duration: oldConf.time.timeDuration,
            };
            row.value = newVal;
          }

          if (row.key === 'font') {
            const newVal: IFontDefault = oldConf.font;
            row.value = newVal;
          }
        });
      }
    });
    newWidget.config.content = {};

    return newWidget;
  }
};

export const convertBtnWidgetToBeta4 = (widget: WidgetBeta3) => {
  const subType = widget.config.content.type;
  let newWidget = {} as Widget;
  if (subType === 'query') {
    newWidget = widgetManagerInstance.toolkit('queryBtn').create({ ...widget });
  } else {
    newWidget = widgetManagerInstance.toolkit('resetBtn').create({ ...widget });
  }
  newWidget = commonBeta4Convert(newWidget, widget);
  return newWidget;
};

export const convertControllerToBeta4 = (widget: WidgetBeta3) => {
  const subType = widget.config.content.type;
  let newWidget = {} as Widget;
  newWidget = widgetManagerInstance.toolkit(subType).create({ ...widget });
  newWidget = commonBeta4Convert(newWidget, widget);
  return newWidget;
};
export const convertToBeta4AutoWidget = (
  boardType: BoardType,
  widget?: Widget,
) => {
  if (!widget) return undefined;
  if (boardType === 'free') return widget;
  if (!widget.config.pRect) {
    widget.config.pRect = widget.config.rect;
    const newRect: RectConfig = {
      x: Math.ceil(Math.random() * 200),
      y: Math.ceil(Math.random() * 200),
      width: 400,
      height: 300,
    };
    widget.config.rect = newRect;
  }
  return widget;
};
/**
 *
 *
 * @param {WidgetBeta3} widget
 * @return {*}
 */
export const convertWidgetToBeta4 = (widget: WidgetBeta3) => {
  const widgetType = widget.config.type;

  // chart
  if (widgetType === 'chart') {
    return convertChartWidgetToBeta4(widget);
  }
  // media
  if (widgetType === 'media') {
    return convertMediaWidgetToBeta4(widget);
  }
  // container
  if (widgetType === 'container') {
    return convertContainerWidgetToBeta4(widget);
  }
  // query
  if (widgetType === 'query' || widgetType === 'reset') {
    return convertBtnWidgetToBeta4(widget);
  }

  // reset
  if (widgetType === 'controller') {
    return convertControllerToBeta4(widget);
  }
};
