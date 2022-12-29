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

export const PIVOT_THEME_SELECT = [
  {
    label: 'theme.type.default',
    value: 0,
  },
  {
    label: 'theme.type.gray',
    value: 1,
  },
  {
    label: 'theme.type.colorful',
    value: 2,
  },
];

export const PIVOT_THEME_LIST = [
  [
    '#000000',
    '#f7faff',
    '#E1EAFE',
    '#E1EAFE',
    '#CCDBFD',
    '#2C60D3',
    '#0000EE',
    '#326EF4',
    '#FFFFFF',
    '#EBF2FF',
    '#D6E3FE',
    '#3471F9',
    '#3471F9',
    '#282B33',
    '#121826',
  ],
  [
    '#000000',
    '#FcFcFd',
    '#F4F5F7',
    '#F3F4F6',
    '#E7E8EA',
    '#CECFD1',
    '#A9AAAB',
    '#616162',
    '#FFFFFF',
    '#F2F2F2',
    '#E8E6E6',
    '#D1D4DC',
    '#BEC2CB',
    '#282B33',
    '#121826',
  ],
  [
    '#FFFFFF',
    '#F4F7FE',
    '#DDE7FD',
    '#3471F9',
    '#2C60D3',
    '#2C60D3',
    '#0000EE',
    '#326EF4',
    '#FFFFFF',
    '#E0E9FE',
    '#5286F9',
    '#5286F9',
    '#3471F9',
    '#282B33',
    '#121826',
  ],
];

/**
 * 这只是个栗子！
 * */
// eslint-disable-next-line @typescript-eslint/no-unused-vars
const dictionaries = [
  '#FFFFFF', // 0: 标题字体颜色
  '#F8F5FE', // 1: 奇行颜色
  '#EDE1FD', // 2: 划上和选中数据背景颜色
  '#873BF4', // 3: 表头背景色
  '#7232CF', // 4: 划上和选中表头背景颜色
  '#7232CF', // 5: 刷选遮罩样式背景颜色
  '#7232CF', // 6: 链接文本颜色
  '#AB76F7', // 7: 划上分割线颜色
  '#FFFFFF', // 8: 背景色(包括整体背景色，数据背景色) 此处用来代替偶行染色
  '#DDC7FC', // 9: 数据单元格边框颜色
  '#9858F5', // 10: 头表格边框颜色
  '#B98EF8', // 11: 垂直分割线
  '#873BF4', // 12: 水平分割线
  '#282B33', // 13: 数据单元格文字颜色
  '#121826', // 14: 行标题颜色
];
