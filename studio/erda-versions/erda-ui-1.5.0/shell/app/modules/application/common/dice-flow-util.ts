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

import {
  DiceFlowType,
  IEditorPoint,
  IDiceFlowDataSource,
  IPosition,
  INodeLine,
} from 'application/common/components/dice-yaml-editor-type';
import DiceYamlEditorItem, { IDiceYamlEditorItem } from 'application/common/components/dice-yaml-editor-item';
import PointComponentAbstract, { IPointInfo } from 'application/common/components/point-component-abstract';
import { linePosition, randomId } from 'application/common/yml-flow-util';
import { IArrowPosition, IFlowItem } from 'application/common/components/flow-item';
import i18n from 'i18n';

const PADDING = 48;
const MIN_RADIUS = 250;
// 偏移角度
// const offsetAngle = 0;
const offsetAngle = (1 / 18) * Math.PI;

const distanceBetweenPoints = (from: IPosition, to: IPosition) => {
  const dx = Math.abs(from.x - to.x);
  const dy = Math.abs(from.y - to.y);
  return Math.sqrt(dx ** 2 + dy ** 2);
};

/**
 * yml文件可视化编辑几点坐标计算
 */
const ymlFileData = (pointComponent: PointComponentAbstract<any, any>, dataSource: DiceYamlEditorItem[]) => {
  const $container = document.getElementById('yaml-editor-content');
  let width = 1000;
  const { info } = pointComponent;
  if (!info) {
    throw new Error(i18n.t('dop:visual nodes must declare static variable information'));
  }

  if ($container) {
    width = $container.offsetWidth;
  }
  const points: any[] = [];
  let maxWidth: any = 0;

  dataSource.forEach((point: any, index: number) => {
    const list: IEditorPoint[] = [];
    maxWidth = Math.max(point.length * (info.ITEM_WIDTH + info.ITEM_MARGIN_RIGHT + info.PADDING_LEFT), maxWidth);
    point.forEach((p: any, i: number) => {
      const position = {
        x: info.PADDING_LEFT + i * (info.ITEM_WIDTH + info.ITEM_MARGIN_RIGHT),
        y: info.PADDING_TOP + index * (info.ITEM_HEIGHT + info.ITEM_MARGIN_BOTTOM),
      };
      list.push({
        ...p,
        position,
        _position: {
          ...position,
        },
        id: `group(${index})item(${i})`,
        index: i,
        groupIndex: index,
        status: 'normal',
      });
    });

    points.push(list);
  });

  maxWidth = Math.max(maxWidth, width);
  return {
    maxWidth,
    points,
  };
};

/**
 * 计算数据集市的所有节点坐标
 */
const dataMarket = (pointComponent: PointComponentAbstract<any, any>, dataSource: IDiceFlowDataSource) => {
  const $container = document.getElementById('yaml-editor-content');
  const { centerItems, dependsItems } = dataSource;
  const { info } = pointComponent;
  let width = 500;
  let height = 500;
  let minTop = 0;
  let minLeft = 0;
  let maxTop = 0;
  let maxLeft = 0;
  if ($container) {
    width = $container.offsetWidth;
    height = $container.offsetHeight;
  }
  if (!info) {
    throw new Error(i18n.t('dop:visual nodes must declare static variable information'));
  }
  const points: any[] = [];
  let centerPoint: IFlowItem;

  const averageAngle = dependsItems.length > 1 ? (2 * Math.PI) / dependsItems.length : 0;
  let b = averageAngle ? (10 + info.ITEM_WIDTH / 2) / Math.sin(averageAngle / 2) : MIN_RADIUS;
  if (b < MIN_RADIUS) {
    b = MIN_RADIUS;
  }
  const a = 1.2 * b;

  centerItems.forEach((point: IDiceYamlEditorItem) => {
    points.push(setCenterPosition(point, width, height, info, 0));

    centerPoint = points[0];
  });

  dependsItems.forEach((point: IDiceYamlEditorItem, index: number) => {
    const angle = Math.abs(averageAngle * index + offsetAngle);
    let x = 0;
    let y = 0;
    if ((angle >= 0 && angle < Math.PI / 2) || (angle > (Math.PI * 3) / 2 && angle <= Math.PI * 2)) {
      x = (a * b) / Math.sqrt(b ** 2 + a ** 2 * Math.tan(angle) ** 2);
      y = (a * b * Math.tan(angle)) / Math.sqrt(b ** 2 + a ** 2 * Math.tan(angle) ** 2);
    } else if (angle > Math.PI / 2 && angle < (Math.PI * 3) / 2) {
      x = -(a * b) / Math.sqrt(b ** 2 + a ** 2 * Math.tan(angle) ** 2);
      y = -(a * b * Math.tan(angle)) / Math.sqrt(b ** 2 + a ** 2 * Math.tan(angle) ** 2);
    } else if (angle === Math.PI / 2) {
      y = b;
      x = 0;
    } else if (angle === (3 * Math.PI) / 2) {
      x = 0;
      y = -b;
    }

    const position = {
      x: centerPoint.position.x + x,
      y: centerPoint.position.y + y,
    };

    const status = getArrowDirection(position, centerPoint.position, info);
    const arrowPosition = getArrowPosition(status, position, info);

    minTop = Math.min(minTop, position.y);
    minLeft = Math.min(minLeft, position.x);
    maxTop = Math.max(maxTop, position.y);
    maxLeft = Math.max(maxLeft, position.x);

    points.push({
      ...point,
      position,
      status,
      arrowPosition: [arrowPosition],
      _position: {
        ...position,
      },
      id: `depends(${index})`,
    });
  });

  const contentWidth = Math.abs(minLeft) + info.ITEM_WIDTH + PADDING + maxLeft;
  const contentHeight = Math.abs(minTop) + info.ITEM_HEIGHT + PADDING + maxTop;
  const scaleY = Math.abs(height / contentHeight);
  const scaleX = Math.abs(width / contentWidth);

  let scale = Math.min(scaleY, scaleX);
  scale = scale > 1 ? 1 : scale;

  points.forEach((point: any, index: number) => {
    if (minLeft < 0) {
      // eslint-disable-next-line no-param-reassign
      point.position.x = point.position.x + Math.abs(minLeft) + PADDING / 2;
      // eslint-disable-next-line no-param-reassign
      point._position.x = point.position.x;
    }
    if (minTop < 0) {
      // eslint-disable-next-line no-param-reassign
      point.position.y = point.position.y + Math.abs(minTop) + PADDING / 2;
      // eslint-disable-next-line no-param-reassign
      point._position.y = point.position.y;
    }

    if (index === 0) {
      // eslint-disable-next-line no-param-reassign
      point.arrowPosition = getCenterPointArrowsPosition(info, 0, point.position);
      // eslint-disable-next-line no-param-reassign
      point.depNum = dependsItems.length;
    } else {
      const arrowPosition = getArrowPosition(point.status, point.position, info);
      // eslint-disable-next-line no-param-reassign
      point.arrowPosition = [arrowPosition];
      // eslint-disable-next-line no-param-reassign
      point.depNum = 0;
    }
  });

  return {
    scale,
    maxWidth: width / scale,
    maxHeight: height / scale,
    points: [points],
  };
};

/**
 * 获取箭头朝向
 */
const getArrowDirection = (from: IPosition, to: IPosition, info: IPointInfo) => {
  const onLeft = from.x < to.x && from.x + info.ITEM_WIDTH < to.x;
  const showBottom = from.x + info.ITEM_WIDTH > to.x && from.x <= to.x + info.ITEM_WIDTH;
  const onRight = from.x > to.x && to.x + info.ITEM_WIDTH < from.x;
  const onTop = from.y < to.y;
  const sameX = from.x === to.x;
  const sameY = from.y === to.y;

  if (onLeft) {
    return 'right';
  } else if (onRight) {
    return 'left';
  } else if (onTop && showBottom) {
    return 'bottom';
  } else if (!onTop && showBottom) {
    return 'top';
  } else if (sameY && onTop) {
    return 'bottom';
  } else if (sameY && !onTop) {
    return 'top';
  } else if (sameX && onLeft) {
    return 'right';
  } else if (sameX && !onLeft) {
    return 'left';
  }

  return '';
};

/**
 * 计算中点坐标
 */
const setCenterPosition = (point: any, width: number, height: number, info: any, offset: number) => {
  const position = {
    x: width / 2 - info.ITEM_WIDTH / 2 - info.PADDING_LEFT,
    y: height / 2 - info.ITEM_HEIGHT / 2 - info.PADDING_TOP + offset,
  };
  return {
    ...point,
    position,
    _position: {
      ...position,
    },
    id: `center(${0})`,
    status: 'center',
    arrowPosition: getCenterPointArrowsPosition(info, offset, position),
  };
};

/**
 * 计算中点箭头坐标
 */
const getCenterPointArrowsPosition = (info: any, offset: number, position: any) => {
  return [
    {
      direction: 'top',
      x: position.x + info.ITEM_WIDTH / 2,
      y: position.y + offset,
    },
    {
      direction: 'left',
      x: position.x,
      y: position.y + info.ITEM_HEIGHT / 2 + offset,
    },
    {
      direction: 'right',
      x: position.x + info.ITEM_WIDTH,
      y: position.y + info.ITEM_HEIGHT / 2 + offset,
    },
    {
      direction: 'bottom',
      x: position.x + info.ITEM_WIDTH / 2 + offset,
      y: position.y + info.ITEM_HEIGHT,
    },
  ];
};

/**
 * 获取箭头坐标
 */
const getArrowPosition = (type: string, position: IPosition, info: IPointInfo) => {
  switch (type) {
    case 'left':
      return {
        direction: 'left',
        x: position.x,
        y: position.y + info.ITEM_HEIGHT / 2,
      };
    case 'right':
      return {
        direction: 'right',
        x: position.x + info.ITEM_WIDTH,
        y: position.y + info.ITEM_HEIGHT / 2,
      };
    case 'top':
      return {
        direction: 'top',
        x: position.x + info.ITEM_WIDTH / 2,
        y: position.y,
      };
    default:
      return {
        direction: 'bottom',
        x: position.x + info.ITEM_WIDTH / 2,
        y: position.y + info.ITEM_HEIGHT,
      };
  }
};

const ymlFileLines = (pointComponent: PointComponentAbstract<any, any>, points: any[]) => {
  let allLines: any[] = [];
  points.forEach((point: any, index: number) => {
    if (index !== points.length - 1) {
      const lines = calculateLinePosition(point, points.slice(index + 1, points.length), pointComponent);
      allLines = allLines.concat(lines);
    }
  });

  return allLines;
};

/**
 * 设置线的类型和控制点坐标
 */
const calculateLinePosition = (
  currentPoint: IEditorPoint[],
  group: IEditorPoint[][],
  component: PointComponentAbstract<any, any>,
) => {
  const lines: INodeLine[] = [];
  currentPoint.forEach((point: IEditorPoint) => {
    group.forEach((nextPoint: IEditorPoint[]) => {
      nextPoint.forEach((np: IEditorPoint) => {
        const item: any = {
          id: randomId(),
          fromPoint: point,
          toPoint: np,
          ...linePosition.line(point, np, component),
        };
        if (
          (point.lineTo && point.lineTo.includes(np.name)) ||
          (point.lineTo && point.lineTo[0] === 'all' && np.groupIndex - point.groupIndex === 1)
        ) {
          item.type = np.groupIndex - point.groupIndex === 1 && np.index === point.index ? 'line' : 'polyline';
          if (np.groupIndex - point.groupIndex !== 1) {
            item.type = 'polylineAcrossPoint';
          }

          if (item.type !== 'line') {
            item.controlPoint = linePosition[item.type](item, component);
          }
          lines.push(item);
        }
      });
    });
  });

  return lines;
};

/**
 * 从中点四个箭头选择距离最近的箭头连接
 */
const selectCenterPointArrowToConnect = (to: IFlowItem, centerPoint: IFlowItem, pointComponent: any) => {
  const centerArrowPoint: any = {
    point: null,
    distance: 0,
  };
  const [fromArrowPoint] = to.arrowPosition;
  centerPoint.arrowPosition.forEach((arrow: IArrowPosition) => {
    const distance = distanceBetweenPoints(fromArrowPoint, arrow);
    if (distance < centerArrowPoint.distance || centerArrowPoint.distance === 0) {
      centerArrowPoint.distance = distance;
      centerArrowPoint.point = arrow;
    }
  });

  const line: any = {
    from: centerArrowPoint.point,
    to: fromArrowPoint,
    type: 'polyline',
  };

  line.controlPoint = linePosition[centerArrowPoint.point.direction][fromArrowPoint.direction](
    {
      from: line.from,
      to: line.to,
    },
    pointComponent,
  );

  return line;
};

/**
 * 计算数据集市节点坐标
 */
const dataMarketLines = (pointComponent: PointComponentAbstract<any, any>, points: any[][]) => {
  const allLines: any[] = [];

  if (points.length && points[0].length) {
    // 中点
    const centerPoint: IFlowItem = points[0][0];
    points[0].forEach((point: IFlowItem, index: number) => {
      if (index > 0) {
        const line = selectCenterPointArrowToConnect(point, centerPoint, pointComponent);

        const item: any = {
          id: randomId(),
          fromPoint: centerPoint,
          toPoint: point,
          type: line.controlPoint ? 'polylineAcrossPoint' : 'line',
          ...line,
        };
        allLines.push(item);
      }
    });
  }

  return allLines;
};

export default {
  calculateItemPositionByType(type: DiceFlowType, pointComponent: any, dataSource: any) {
    if (this[`${type}_ITEM_POSITION`] && dataSource) {
      return this[`${type}_ITEM_POSITION`](pointComponent, dataSource);
    }

    return [];
  },
  calculateLinePositionByType(type: DiceFlowType, pointComponent: any, dataSource: any) {
    if (this[`${type}_LINE_POSITION`] && dataSource) {
      return this[`${type}_LINE_POSITION`](pointComponent, dataSource);
    }

    return [];
  },
  PIPELINE_ITEM_POSITION: ymlFileData,
  EDITOR_ITEM_POSITION: ymlFileData,
  DATA_MARKET_ITEM_POSITION: dataMarket,
  PIPELINE_LINE_POSITION: ymlFileLines,
  EDITOR_LINE_POSITION: ymlFileLines,
  DATA_MARKET_LINE_POSITION: dataMarketLines,
};
