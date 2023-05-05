// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.
import React from 'react';
import './index.scss';

// sync with tailwind css
const themeColor = {
  primary: '#6a549e',
  'primary-800': 'rgba(106, 84, 158, 0.8)',
  normal: '#000000cc', // color-dark-8: rgba(0, 0, 0, .8)
  sub: '#00000099', // color-dark-6: rgba(0, 0, 0, .6)
  // desc: '#0000007f', // color-dark-5: rgba(0, 0, 0, .5)
  desc: '#00000066', // color-dark-4: rgba(0, 0, 0, .4)
  icon: '#00000066', // color-dark-3: rgba(0, 0, 0, .3)
  disabled: '#00000066', // color-dark-3: rgba(0, 0, 0, .3)
  holder: '#00000033', // color-dark-3: rgba(0, 0, 0, .2)
  red: '#df3409',
  danger: '#df3409',
  blue: '#0567ff',
  info: '#0567ff',
  yellow: '#feab00',
  warning: '#feab00',
  green: '#34b37e',
  success: '#34b37e',
  orange: '#f47201',
  purple: '#6a549e',
  cyan: '#5bd6d0ff',
  gray: '#666666',
  brightgray: '#eaeaea',
  darkgray: '#999999',
  grey: '#f5f5f5',
  layout: '#f0eef5',
  white: '#ffffff',
  lotion: '#fcfcfc',
  cultured: '#f6f4f9',
  magnolia: '#f2f1fc',
  mask: 'rgba(0,0,0,0.45)',
  black: 'rgba(0,0,0,1)',
  'black-200': 'rgba(0,0,0,0.2)',
  'black-300': 'rgba(0,0,0,0.3)',
  'black-400': 'rgba(0,0,0,0.4)',
  'black-600': 'rgba(0,0,0,0.6)',
  'black-800': 'rgba(0,0,0,0.8)',
  'light-primary': '#6a549e19', // rgba($primary, .1)
  'shallow-primary': '#6a549e99', // rgba($primary, .6)
  'light-gray': '#bbbbbb',
  'dark-8': '#000000cc',
  'dark-6': '#00000066',
  'dark-2': '#00000033',
  'dark-1': '#00000019',
  'dark-04': '#0000000a',
  'dark-02': '#00000005',
  'white-8': '#ffffffcc',
  'log-font': '#c2c1d0',
  'log-bg': '#3c444f',
  'light-border': 'rgba(222,222,222,0.5)',
  'light-active': '#6a549e0f', // rgba($primary, .06)
  currentColor: 'currentColor',
};

type IconColor = keyof typeof themeColor;

export const iconMap = {
  lock: 'lock',
  unlock: 'unlock',
  time: 'time',
  'application-one': 'application-one',
  user: 'user',
  'link-cloud-sucess': 'link-cloud-success',
  'link-cloud-faild': 'link-cloud-failed',
  'category-management': 'category-management',
  'list-numbers': 'list-numbers',
  'api-app': 'api-app',
  'double-right': 'double-right',
  'application-menu': 'application-menu',
  help: 'help',
  plus: 'tj1',
  moreOne: 'more-one',
  CPU: 'CPU',
  GPU: 'GPU',
  cipan: 'cipan',
  cluster: 'cluster',
  version: 'version',
  machine: 'machine',
  type: 'type',
  management: 'management',
  'create-time': 'create-time',
  renwu: 'renwu',
  xuqiu: 'xuqiu',
  quexian: 'quexian',
  zhongdengnandu: 'zhongdengnandu',
  nan: 'nan',
  rongyi: 'rongyi',
};

interface IErdaIcon {
  className?: string;
  type: string; // unique identification of icon
  style?: React.CSSProperties;
  width?: string; // with of svg, and it's more priority than size
  height?: string; // height of svg, and it's more priority than size
  spin?: boolean; // use infinite rotate animation like loading icon, the default value is false
  size?: string | number; // size of svg with default value of 1rem. Use width and height if width-to-height ratio is not 1
  fill?: string; // color of svg fill area, and it's more priority than color
  stroke?: string; // color of svg stroke, and it's more priority than color
  color?: IconColor; // color of svg
  rtl?: boolean; // acoustic image, the default value is from left to right
  onClick?: React.MouseEventHandler;
  opacity?: number;
  isConfigPageIcon?: boolean;
}

const ErdaIcon = ({ type, fill, color, stroke, className, isConfigPageIcon, ...rest }: IErdaIcon) => {
  return (
    // @ts-ignore iconpark component
    <iconpark-icon
      name={!isConfigPageIcon ? type : iconMap[type]}
      fill={themeColor[fill || 'currentColor']}
      color={themeColor[color || 'currentColor']}
      stroke={themeColor[stroke || 'currentColor']}
      class={className}
      {...rest}
    />
  );
};

export default ErdaIcon;
