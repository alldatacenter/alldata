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

interface IProps {
  className?: string;
  percent?: number;
  width?: number;
  stroke: string[];
  strokeWidth?: number;
}

/**
 * @description convert the angle to radians
 * @param angle
 */
const angleToRadian = (angle: number) => angle * (Math.PI / 180);

const getSideLengthByWidth = (width: number) => width / Math.sin(angleToRadian(60));

const numberFixed = (num: number) => +num.toFixed(2);

interface IPathStyleReturn {
  pathStyle: string;
  point: { x: number; y: number }[];
  side: {
    start: { point: { x: number; y: number }; value: number };
    end: { point: { x: number; y: number }; value: number };
  }[];
}

/**
 * @description background path
 */
const getPathStyles = (width: number, strokeWidth: number): IPathStyleReturn => {
  const sin60 = Math.sin(angleToRadian(60));
  const sl = width / 2 / sin60;
  const unitValue = 100 / 6;
  // the coordinates of points in a hexagon
  const centerTopPoint = { x: numberFixed((width + strokeWidth) / 2), y: strokeWidth };
  const rightTopPoint = { x: numberFixed(width + strokeWidth), y: numberFixed(sl / 2 + strokeWidth) };
  const rightBottomPoint = { x: numberFixed(width + strokeWidth), y: numberFixed(sl * 1.5 + strokeWidth) };
  const centerBottomPoint = { x: numberFixed((width + strokeWidth) / 2), y: numberFixed(2 * sl + strokeWidth) };
  const leftBottomPoint = { x: numberFixed(strokeWidth / 2), y: numberFixed(sl * 1.5 + strokeWidth) };
  const leftTopPoint = { x: numberFixed(strokeWidth / 2), y: numberFixed(sl / 2 + strokeWidth) };
  const pathStyle = `M${centerTopPoint.x} ${centerTopPoint.y} L${rightTopPoint.x} ${rightTopPoint.y} L${rightBottomPoint.x} ${rightBottomPoint.y} L${centerBottomPoint.x} ${centerBottomPoint.y} L${leftBottomPoint.x} ${leftBottomPoint.y} L${leftTopPoint.x} ${leftTopPoint.y} L${centerTopPoint.x} ${centerTopPoint.y}`;
  return {
    pathStyle,
    point: [centerTopPoint, rightTopPoint, rightBottomPoint, centerBottomPoint, leftBottomPoint, leftTopPoint],
    side: [
      {
        start: {
          point: centerTopPoint,
          value: 0,
        },
        end: {
          point: rightTopPoint,
          value: unitValue,
        },
      },
      {
        start: {
          point: rightTopPoint,
          value: unitValue,
        },
        end: {
          point: rightBottomPoint,
          value: unitValue * 2,
        },
      },
      {
        start: {
          point: rightBottomPoint,
          value: unitValue * 2,
        },
        end: {
          point: centerBottomPoint,
          value: unitValue * 3,
        },
      },
      {
        start: {
          point: centerBottomPoint,
          value: unitValue * 3,
        },
        end: {
          point: leftBottomPoint,
          value: unitValue * 4,
        },
      },
      {
        start: {
          point: leftBottomPoint,
          value: unitValue * 4,
        },
        end: {
          point: leftTopPoint,
          value: unitValue * 5,
        },
      },
      {
        start: {
          point: leftTopPoint,
          value: unitValue * 5,
        },
        end: {
          point: centerTopPoint,
          value: 100,
        },
      },
    ],
  };
};

/**
 * @description front path
 */
const getPercentPathStyle = (percent: number, side: IPathStyleReturn['side'], pathStyle: string) => {
  let path = `M${side[0].start.point.x} ${side[0].start.point.y}`;
  if (percent >= 100) {
    return pathStyle;
  } else if (percent === 0) {
    return path;
  }
  // calculates the interval of the endpoints
  const index = side.findIndex((item) => {
    const { start, end } = item;
    return start.value <= percent && percent < end.value;
  });
  const unitValue = 100 / 6;
  const endPoint = { ...side[index].end.point };
  const startPoint = { ...side[index].start.point };
  const newEndPoint = endPoint;
  const realPercent = (percent - index * unitValue) / unitValue;
  newEndPoint.x = +(startPoint.x - (startPoint.x - endPoint.x) * realPercent).toFixed(2);
  newEndPoint.y = +(startPoint.y + (endPoint.y - startPoint.y) * realPercent).toFixed(2);
  side.forEach((item, i) => {
    if (i < index) {
      path = `${path} L${item.end.point.x} ${item.end.point.y}`;
    }
  });
  path = `${path} L${newEndPoint.x} ${newEndPoint.y}`;
  return path;
};

const Hexagon: React.FC<IProps> = ({ width = 100, stroke, percent = 0, strokeWidth = 5, className, children }) => {
  const sideLength = getSideLengthByWidth((width + 2 * strokeWidth) / 2);
  const { pathStyle, side, point } = React.useMemo(() => {
    return getPathStyles(width, strokeWidth);
  }, [width, strokeWidth]);
  const percentPath = React.useMemo(() => getPercentPathStyle(percent, side, pathStyle), [percent, side, pathStyle]);
  const clipPath = React.useMemo(() => {
    // polygon(31px 2px, 62px 19px, 62px 54px, 31px 72px, 1px 54px, 1px 19px, 31px 2px)
    const polygon = `polygon(${point
      .map((item) => {
        return `${item.x}px ${item.y}px`;
      })
      .join(', ')})`;
    return { clipPath: polygon };
  }, [point]);
  const [bgColor, frontColor] = stroke;
  const wrapperWidth = width + 2 * strokeWidth;
  const wrapperHeight = 2 * sideLength;
  return (
    <div className="relative">
      <div className="shadow-wrapper absolute top-0 left-0" style={{ width: wrapperWidth, height: wrapperHeight }}>
        <div className="shadow h-full w-full" style={clipPath} />
      </div>
      <svg className={`hexagon-progress ${className || ''}`} width={wrapperWidth} height={2 * sideLength}>
        <path
          className="path fill-path"
          stopOpacity="0"
          strokeWidth={strokeWidth}
          fillOpacity="0.1"
          fill="#ffffff"
          d={getPathStyles(width - 4, strokeWidth).pathStyle}
        />
        <path className="path" stroke={bgColor} strokeWidth={strokeWidth} fillOpacity="0" d={pathStyle} />
        <path className="path" stroke={frontColor} strokeWidth={strokeWidth} fillOpacity="0" d={percentPath} />
      </svg>
      {children ? (
        <div className="absolute top-0 left-0" style={{ width: wrapperWidth, height: wrapperHeight }}>
          {children}
        </div>
      ) : null}
    </div>
  );
};

export default Hexagon;
