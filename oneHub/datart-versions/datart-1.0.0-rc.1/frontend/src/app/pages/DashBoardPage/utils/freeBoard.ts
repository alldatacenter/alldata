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
import { APP_CURRENT_VERSION } from 'app/migration/constants';
import { BoardConfig } from '../types/boardTypes';

export const initFreeBoardConfig = () => {
  const config: BoardConfig = {
    type: 'free',
    version: APP_CURRENT_VERSION,

    jsonConfig: {
      props: [
        {
          label: 'basic.basic',
          key: 'basic',
          comType: 'group',
          rows: [
            {
              label: 'basic.initialQuery',
              key: 'initialQuery',
              default: true,
              value: true,
              comType: 'switch',
            },
            // TODO Step 步长
            // {
            //   label: 'basic.Step',
            //   key: 'Step',
            //   default: 1,
            //   value: 5,
            //   comType: 'inputNumber',
            // },
            {
              label: 'basic.scaleMode.scaleMode',
              key: 'scaleMode',
              default: 'scaleWidth',
              value: 'scaleWidth',
              comType: 'select',
              options: {
                translateItemLabel: true,
                items: [
                  {
                    label: '@global@.basic.scaleMode.scaleWidth',
                    key: 'scaleWidth',
                    value: 'scaleWidth',
                  },
                  {
                    label: '@global@.basic.scaleMode.scaleHeight',
                    key: 'scaleHeight',
                    value: 'scaleHeight',
                  },
                  {
                    label: '@global@.basic.scaleMode.scaleFull',
                    key: 'scaleFull',
                    value: 'scaleFull',
                  },
                  {
                    label: '@global@.basic.scaleMode.noScale',
                    key: 'noScale',
                    value: 'noScale',
                  },
                ],
              },
            },
          ],
        },

        {
          label: 'size.size',
          key: 'size',
          comType: 'group',
          rows: [
            {
              label: 'size.width',
              key: 'width',
              default: 1920,
              value: 1920,
              comType: 'inputNumber',
            },
            {
              label: 'size.height',
              key: 'height',
              default: 1080,
              value: 1080,
              comType: 'inputNumber',
            },
          ],
        },
        {
          label: 'background.background',
          key: 'background',
          comType: 'group',
          rows: [
            {
              label: 'background.background',
              key: 'background',
              default: {
                color: 'transparent', // TODO 根据当前主题色配置
                image: '',
                size: '100% 100%',
                repeat: 'no-repeat',
              },
              value: {
                color: 'transparent', // TODO 根据当前主题色配置
                image: '',
                size: '100% 100%',
                repeat: 'no-repeat',
              },
              comType: 'background',
            },
          ],
        },
      ],
      i18ns: [
        {
          lang: 'zh-CN',
          translation: {
            basic: {
              basic: '基础',
              initialQuery: '开启初始化查询',
              scaleMode: {
                scaleMode: '缩放模式',
                scaleWidth: '等比宽度缩放',
                scaleHeight: '等比高度缩放',
                scaleFull: '全屏缩放',
                noScale: '不缩放',
              },
            },
            size: {
              size: '尺寸',
              width: '宽度',
              height: '高度',
            },

            background: {
              background: '背景',
            },
          },
        },
        {
          lang: 'en-US',
          translation: {
            basic: {
              basic: 'Basic',
              initialQuery: 'Open Init Query',
              scaleMode: {
                scaleMode: 'Scale Mode',
                scaleWidth: 'Scale Width',
                scaleHeight: 'Scale Height',
                scaleFull: 'Scale Full',
                noScale: 'No Scale',
              },
            },
            size: {
              size: 'Size',
              width: 'Width',
              height: 'Height',
            },
            space: {
              space: 'Space',
              paddingTB: 'Padding TB',
              paddingLR: 'Padding LR',
              marginTB: 'Margin TB',
              marginLR: 'Margin LR',
            },
            background: {
              background: 'Background',
            },
          },
        },
      ],
    },
  };
  return config;
};
