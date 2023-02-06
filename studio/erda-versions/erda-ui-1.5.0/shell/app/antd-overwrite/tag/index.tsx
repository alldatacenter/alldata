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
import Tag, { TagProps } from 'antd/es/tag';

// Regular expression for hexadecimal colors such as #ffffff or #fff
const colorRegex = /^#([0-9a-fA-f]{3}|[0-9a-fA-f]{6})$/;

function WrappedTag({ color, ...props }: TagProps) {
  const rest: { color?: string; style?: object } = {};
  if (color && colorRegex.test(color.toLowerCase())) {
    const rgbColor = colorToRgb(color);
    const style = {
      backgroundColor: `rgb(${mixWhite(rgbColor, 0.1)})`,
      color: `rgb(${rgbColor})`,
      borderColor: `rgb(${rgbColor}, 0.2)`,
    };
    rest.style = { ...style, ...props.style };
  } else {
    rest.color = color;
  }
  return <Tag {...rest} {...props} />;
}

/**
 * The color with transparency is mixed with white
 * @param color
 * @param transparency
 */
const mixWhite = (color: string, transparency: number) => {
  return color
    .split(',')
    .map((item: string) => Number(item) * transparency + 255 * (1 - transparency))
    .join(',');
};

/**
 * Hexadecimal color is converted to RGB format
 * @param color
 */
const colorToRgb = (color: string) => {
  let sColor = color.toLowerCase();
  if (sColor.length === 4) {
    let sColorNew = '#';
    for (let i = 1; i < 4; i += 1) {
      sColorNew += sColor.slice(i, i + 1).concat(sColor.slice(i, i + 1));
    }
    sColor = sColorNew;
  }

  const sColorChange = [];
  for (let i = 1; i < 7; i += 2) {
    sColorChange.push(parseInt(`0x${sColor.slice(i, i + 2)}`, 16));
  }
  return sColorChange.join(',');
};

export default WrappedTag;
