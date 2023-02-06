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
const themeColor = '#6A549E';
module.exports.themeColor = themeColor;

module.exports.getLessTheme = () => {
  return {
    '@primary-color': themeColor,
    '@success-color': 'rgba(0, 183, 121, 1)',
    '@error-color': 'rgba(223, 52, 9, 1)',
    '@warning-color': 'rgba(254, 171, 0, 1)',
    '@link-color': themeColor,
    '@progress-remaining-color': '#E1E7FF',
    '@font-size-base': '14px',
    '@input-height-base': '32px',
    '@input-height-lg': '36px',
    '@input-height-sm': '28px',
    '@btn-height-base': '32px',
    '@btn-height-lg': '36px',
    '@btn-height-sm': '28px',
    '@btn-padding-sm': '0 11px;',
    '@border-radius-base': '3px;',
    '@font-family':
      '"Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif',
  };
};

module.exports.getScssTheme = () => {
  return `
  $color-primary: #6A549E;
  $color-primary-light: #6D5AE2;
  $color-primary-dark: #5340C8;
  `;
};
