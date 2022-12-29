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
import { BLACK, WHITE } from 'styles/StyleConstants';
export const defaultPalette = [
  '#FAFAFA',
  '#9E9E9E',
  '#E3F2FD',
  '#FFF3E0',
  '#FFEBEE',
  '#E0F2F1',
  '#E8F5E9',
  '#FFF8E1',
  '#EDE7F6',
  '#FCE4EC',
  '#EFEBE9',
  '#F5F5F5',
  '#757575',
  '#BBDEFB',
  '#FFE0B2',
  '#FFCDD2',
  '#B2DFDB',
  '#C8E6C9',
  '#FFECB3',
  '#D1C4E9',
  '#F8BBD0',
  '#D7CCC8',
  '#EEEEEE',
  '#616161',
  '#64B5F6',
  '#FFB74D',
  '#E57373',
  '#64FFDA',
  '#81C784',
  '#FFD740',
  '#9575CD',
  '#FF4081',
  '#A1887F',
  '#E0E0E0',
  '#424242',
  '#1976D2',
  '#F57C00',
  '#D32F2F',
  '#1DE9B6',
  '#388E3C',
  '#FFC400',
  '#512DA8',
  '#F50057',
  '#5D4037',
  '#BDBDBD',
  '#212121',
  '#0D47A1',
  '#E65100',
  '#B71C1C',
  '#00BFA5',
  '#1B5E20',
  '#FFAB00',
  '#311B92',
  '#C51162',
  '#3E2723',
];
export const colorThemes = [
  {
    id: 'default',
    colors: [
      '#448aff',
      '#ffab40',
      '#ff5252',
      '#a7ffeb',
      '#4caf50',
      '#ffecb3',
      '#7c4dff',
      '#f8bbd0',
      '#795548',
      '#f5f5f5',
    ],
    en: {
      title: 'Default',
    },
    zh: {
      title: '默认',
    },
  },
  {
    id: 'default20',
    colors: [
      '#448aff',
      '#e3f2fd',
      '#ffab40',
      '#fff3e0',
      '#4caf50',
      '#b9f6ca',
      '#ffd740',
      '#ffecb3',
      '#009688',
      '#a7ffeb',
      '#ff5252',
      '#ffcdd2',
      '#9e9e9e',
      '#f5f5f5',
      '#FF4081',
      '#f8bbd0',
      '#7c4dff',
      '#b388ff',
      '#795548',
      '#d7ccc8',
    ],
    en: {
      title: 'Default 20',
    },
    zh: {
      title: '默认20色',
    },
  },
  {
    id: 'spectrum',
    colors: [
      '#16b4bb',
      '#3f28c9',
      '#e17315',
      '#cf167d',
      '#7d6ff9',
      '#40e15c',
      '#2068e7',
      '#5a20a2',
      '#d8b509',
      '#bf5b0e',
      '#217d59',
      '#8ced43',
    ],
    en: {
      title: 'Spectrum',
    },
    zh: {
      title: 'Spectrum',
    },
  },
  {
    id: 'retrometro',
    colors: [
      '#b33dc6',
      '#27aeef',
      '#87bc45',
      '#bdcf32',
      '#ede15b',
      '#edbf33',
      '#ef9b20',
      '#f46a9b',
      '#ea5545',
    ],
    en: {
      title: 'Retro Metro',
    },
    zh: {
      title: 'Retro Metro',
    },
  },
  {
    id: 'dutchfield',
    colors: [
      '#e60049',
      '#0bb4ff',
      '#50e991',
      '#e6d800',
      '#9b19f5',
      '#ffa300',
      '#dc0ab4',
      '#b3d4ff',
      '#00bfa0',
    ],
    en: {
      title: 'Dutch Field',
    },
    zh: {
      title: 'Dutch Field',
    },
  },
  {
    id: 'rivernights',
    colors: [
      '#b30000',
      '#7c1158',
      '#4421af',
      '#1a53ff',
      '#0d88e6',
      '#00b7c7',
      '#5ad45a',
      '#8be04e',
      '#ebdc78',
    ],
    en: {
      title: 'River Nights',
    },
    zh: {
      title: 'River Nights',
    },
  },
  {
    id: 'springpastels',
    colors: [
      '#fd7f6f',
      '#7eb0d5',
      '#b2e061',
      '#bd7ebe',
      '#ffb55a',
      '#ffee65',
      '#beb9db',
      '#fdcce5',
      '#8bd3c7',
    ],
    en: {
      title: 'Spring Pastels',
    },
    zh: {
      title: 'Spring Pastels',
    },
  },
  {
    id: 'echarts',
    colors: [
      '#5470c6',
      '#91cc75',
      '#fac858',
      '#ee6666',
      '#73c0de',
      '#3ba272',
      '#fc8452',
      '#9a60b4',
      '#ea7ccc',
    ],
    en: {
      title: 'Echarts',
    },
    zh: {
      title: 'Echarts',
    },
  },
  {
    id: 'vintage',
    colors: [
      '#d87c7c',
      '#919e8b',
      '#d7ab82',
      '#6e7074',
      '#61a0a8',
      '#efa18d',
      '#787464',
      '#cc7e63',
      '#724e58',
      '#4b565b',
    ],
    en: {
      title: 'Vintage',
    },
    zh: {
      title: '怀旧',
    },
  },
  {
    id: 'dark',
    colors: [
      '#dd6b66',
      '#759aa0',
      '#e69d87',
      '#8dc1a9',
      '#ea7e53',
      '#eedd78',
      '#73a373',
      '#73b9bc',
      '#7289ab',
      '#91ca8c',
      '#f49f42',
    ],
    en: {
      title: 'Dark',
    },
    zh: {
      title: '暗色',
    },
  },
  {
    id: 'westeros',
    colors: ['#516b91', '#59c4e6', '#edafda', '#93b7e3', '#a5e7f0', '#cbb0e3'],
    en: {
      title: 'Westeros',
    },
    zh: {
      title: 'Westeros',
    },
  },
  {
    id: 'essos',
    colors: ['#893448', '#d95850', '#eb8146', '#ffb248', '#f2d643', '#ebdba4'],
    en: {
      title: 'Essos',
    },
    zh: {
      title: 'Essos',
    },
  },
  {
    id: 'wonderland',
    colors: ['#4ea397', '#22c3aa', '#7bd9a5', '#d0648a', '#f58db2', '#f2b3c9'],
    en: {
      title: 'Wonderland',
    },
    zh: {
      title: 'Wonderland',
    },
  },
  {
    id: 'walden',
    colors: ['#3fb1e3', '#6be6c1', '#626c91', '#a0a7e6', '#c4ebad', '#96dee8'],
    en: {
      title: 'Walden',
    },
    zh: {
      title: 'Walden',
    },
  },
  {
    id: 'chalk',
    colors: [
      '#fc97af',
      '#87f7cf',
      '#f7f494',
      '#72ccff',
      '#f7c5a0',
      '#d4a4eb',
      '#d2f5a6',
      '#76f2f2',
    ],
    en: {
      title: 'Chalk',
    },
    zh: {
      title: '粉笔',
    },
  },
  {
    id: 'infographic',
    colors: [
      '#c1232b',
      '#27727b',
      '#fcce10',
      '#e87c25',
      '#b5c334',
      '#fe8463',
      '#9bca63',
      '#fad860',
      '#f3a43b',
      '#60c0dd',
      '#d7504b',
      '#c6e579',
      '#f4e001',
      '#f0805a',
      '#26c0c0',
    ],
    en: {
      title: 'Infographic',
    },
    zh: {
      title: '信息图',
    },
  },
  {
    id: 'macarons',
    colors: [
      '#2ec7c9',
      '#b6a2de',
      '#5ab1ef',
      '#ffb980',
      '#d87a80',
      '#8d98b3',
      '#e5cf0d',
      '#97b552',
      '#95706d',
      '#dc69aa',
      '#07a2a4',
      '#9a7fd1',
      '#588dd5',
      '#f5994e',
      '#c05050',
      '#59678c',
      '#c9ab00',
      '#7eb00a',
      '#6f5553',
      '#c14089',
    ],
    en: {
      title: 'Macarons',
    },
    zh: {
      title: '马卡龙',
    },
  },
  {
    id: 'roma',
    colors: [
      '#e01f54',
      '#001852',
      '#f5e8c8',
      '#b8d2c7',
      '#c6b38e',
      '#a4d8c2',
      '#f3d999',
      '#d3758f',
      '#dcc392',
      '#2e4783',
      '#82b6e9',
      '#ff6347',
      '#a092f1',
      '#0a915d',
      '#eaf889',
      '#6699ff',
      '#ff6666',
      '#3cb371',
      '#d5b158',
      '#38b6b6',
    ],
    en: {
      title: 'Roma',
    },
    zh: {
      title: '罗马',
    },
  },
  {
    id: 'shine',
    colors: [
      '#c12e34',
      '#e6b600',
      '#0098d9',
      '#2b821d',
      '#005eaa',
      '#339ca8',
      '#cda819',
      '#32a487',
    ],
    en: {
      title: 'Shine',
    },
    zh: {
      title: '阳光',
    },
  },
  {
    id: 'purplepassion',
    colors: ['#9b8bba', '#e098c7', '#8fd3e8', '#71669e', '#cc70af', '#7cb4cc'],
    en: {
      title: 'Purple Passion',
    },
    zh: {
      title: '热情',
    },
  },
];
export const defaultThemes = [
  WHITE,
  BLACK,
  ...colorThemes[0].colors.slice(0, colorThemes[0].colors.length - 1),
];
