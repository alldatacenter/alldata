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
  IMovingPoints,
  INodeLine,
  IPosition,
} from 'application/common/components/dice-yaml-editor-type';
import diceFLowUtil from 'application/common/dice-flow-util';
import { isEqualCustomizer, randomId, sortByLineType } from 'application/common/yml-flow-util';
import { isEqualWith } from 'lodash';
import React, { Component } from 'react';
// @ts-ignore
import Snap from 'snapsvg-cjs';

interface IProps {
  type: DiceFlowType;
  pointComponent: any;
  dataSource: any[];
  selectedItem?: IEditorPoint | null;
  movingPoints?: IMovingPoints[];
  editing: boolean;
  newLine?: INodeLine;
  scale: number;
  maxWidth: number;
  maxHeight: number;
  mouseDownPosition?: IPosition | null;
  lineStyle?: ILineStyle;
}

export interface ILineStyle {
  borderColor: string;
}

const BORDER_COLOR = '#999';
const SELECTED_COLOR = '#8c7acc';
const SIGNAL_COLOR = 'rgba(91, 69, 194, 1)';

export default class DiceYamlCanvas extends Component<IProps, any> {
  state = {
    svgLeft: 0,
    lines: [],
    width: 0,
    height: 0,
    originWidth: 0,
    originHeight: 0,
  };

  private snap: any;

  private selectedLine: any;

  private newLine: any;

  private linesCache: any[] = [];

  private circleCache: any[] = [];

  private animateTimeout: any;

  private pointsView: any;

  private resizeTimeout: any;

  private deletedMovingPoint: any[] = [];

  shouldComponentUpdate(nextProps: Readonly<IProps>, nextState: Readonly<any>): boolean {
    if (
      !isEqualWith(nextProps.dataSource, this.props.dataSource, isEqualCustomizer) ||
      nextProps.scale !== this.props.scale ||
      !isEqualWith(nextState, this.state)
    ) {
      return true;
    }

    return false;
  }

  UNSAFE_componentWillReceiveProps(nextProps: Readonly<IProps>): void {
    if (!isEqualWith(nextProps.movingPoints, this.props.movingPoints, isEqualCustomizer)) {
      this.getDeletedMovingPoints(nextProps.movingPoints, this.props.movingPoints);
    }

    if (
      !isEqualWith(nextProps.dataSource, this.props.dataSource, isEqualCustomizer) ||
      nextProps.scale !== this.props.scale
    ) {
      this.loadData(nextProps);
    }
  }

  componentDidMount(): void {
    this.pointsView = document.getElementById('points-view');
    if (this.pointsView) {
      this.pointsView.addEventListener('scroll', this.scroll);
    }
    window.addEventListener('resize', this.resize);
    document.body.addEventListener('mousemove', this.onMouseMove);
    if (!this.snap) {
      this.loadData(this.props);
    }
  }

  componentWillUnmount(): void {
    if (this.pointsView) {
      this.pointsView.removeEventListener('scroll', this.scroll);
    }
    if (this.animateTimeout) {
      clearTimeout(this.animateTimeout);
    }

    window.removeEventListener('resize', this.resize);
  }

  render() {
    const { maxWidth, type } = this.props;
    const { svgLeft, width, height, originWidth, originHeight } = this.state;
    let viewBox;
    let style;

    if (type === DiceFlowType.DATA_MARKET) {
      viewBox = `0 0 ${width || '0'} ${height || '0'}`;
    } else {
      style = { left: `${-svgLeft}px`, width: maxWidth, height: '100%' };
    }

    return (
      <svg
        id="yaml-editor-svg"
        viewBox={viewBox}
        width={type === DiceFlowType.DATA_MARKET ? originWidth : maxWidth}
        height={originHeight}
        style={style}
        className="yaml-editor-background-container"
      />
    );
  }

  /**
   * 计算已经不存在点
   */
  private getDeletedMovingPoints(nextPoints?: any[], prevPoints?: any[]) {
    if (nextPoints && prevPoints) {
      prevPoints.forEach((p: any) => {
        // const result = nextPoints.find((np: any) => {
        //   if (np.from === p.from && np.to === p.to) {
        //     return np;
        //   }
        //   return null;
        // });

        // if (!result) {
        // fix 流水线蚂蚁点问题
        this.deletedMovingPoint.push(p);
        // }
      });
    }
  }

  private resize = () => {
    if (this.resizeTimeout) {
      clearTimeout(this.resizeTimeout);
      this.resizeTimeout = null;
    }

    this.resizeTimeout = setTimeout(() => {
      this.loadData(this.props);
      this.resizeTimeout = null;
    }, 300);
  };

  private loadData(props: IProps) {
    const { scale, movingPoints = [] }: any = props;
    const $container: any = document.getElementById('yaml-editor-content');
    let width;
    let height;
    if ($container) {
      width = $container.offsetWidth / scale;
      height = $container.offsetHeight / scale;
    }

    const lines = this.calculateAllLinePosition(props.dataSource);
    this.setState(
      {
        lines,
        width,
        height,
        originWidth: $container ? $container.offsetWidth : '100%',
        originHeight: $container ? $container.offsetHeight : '100%',
      },
      () => {
        if (!this.snap) {
          this.snap = Snap('#yaml-editor-svg');
        }

        if (movingPoints && !movingPoints.length) {
          this.circleCache.forEach((item: any) => {
            item.circle.remove();
            item.animate && item.animate.stop();
          });
          this.circleCache = [];
        }

        // 从缓存中找出已经不存在的点删除掉
        if (this.deletedMovingPoint.length) {
          const preDeletedIndex: number[] = [];
          this.circleCache.forEach((item: any, index: number) => {
            this.deletedMovingPoint.forEach((p: any) => {
              if (item.point.fromPoint.data.id === p.from && item.point.toPoint.data.id === p.to) {
                item.animate.stop();
                item.circle.remove();
                preDeletedIndex.push(index);
              }
            });
          });

          preDeletedIndex.forEach((i: number) => {
            this.circleCache.splice(i, 1);
          });

          this.deletedMovingPoint = [];
        }

        this.reset();
        this.resetLines();
      },
    );
  }

  private reset = () => {
    if (this.newLine) {
      this.newLine.svgLine.remove();
      this.newLine = null;
    }
  };

  private resetLines = () => {
    const { lines } = this.state;
    this.setState(
      {
        lines: lines.filter((item: any) => item.status !== 'add'),
      },
      () => {
        this.iteratorPoints();
      },
    );
  };

  private iteratorPoints = () => {
    const { lines } = this.state;
    if (!this.snap) {
      return;
    }

    this.linesCache.forEach(({ line }: any) => {
      line.remove();
    });

    this.linesCache = [];

    lines.forEach((point: any) => {
      this.drawLine(point);
    });
  };

  private animation(item: any, path: any, point: any) {
    if (!item.animate) {
      const length = Snap.path.getTotalLength(path);
      const time = length <= 60 ? 2000 : Math.floor((length / 60) * 1000);
      const snapAnimate = Snap.animate(
        0,
        length,
        (val: any) => {
          const p = Snap.path.getPointAtLength(path, val); // 根据path长度变化获取坐标
          const m = new Snap.Matrix();
          m.translate(p.x - point.from.x - 1, p.y - point.from.y);
          item.circle.transform(m);
          // @ts-ignore
        },
        time,
        window.mina.easeout(),
        () => {
          this.animateTimeout = setTimeout(() => {
            // eslint-disable-next-line no-param-reassign
            delete item.animate;
            this.animation(item, path, point);
          }, 1000);
        },
      );
      // eslint-disable-next-line no-param-reassign
      item.animate = snapAnimate;
    }
  }

  private drawSignal = (point: any, path: any) => {
    const { movingPoints } = this.props;
    if (!movingPoints || (movingPoints && !movingPoints.length)) {
      return;
    }
    const result = movingPoints
      ? movingPoints.find((p: any) => {
          return p.from === point.fromPoint.data.id && p.to === point.toPoint.data.id;
        })
      : null;
    const existCircle = this.circleCache.find((item: any) => item.point.id === point.id);
    const index = this.circleCache.findIndex((item: any) => item.point.id === point.id);
    if (result) {
      if (!existCircle) {
        const circle = this.snap.paper.circle(point.from.x + 1, point.from.y, 2);
        circle.attr({
          fill: SIGNAL_COLOR,
        });

        const item = {
          point,
          circle,
        };
        this.animation(item, path, point);
        this.circleCache.push(item);
      } else {
        this.animation(existCircle, path, point);
      }
    } else if (existCircle && existCircle.circle) {
      existCircle.circle.remove();
      this.circleCache.splice(index, 1);
    }
  };

  private drawLine = (point: any) => {
    const { lineStyle } = this.props;

    let line: any;
    const M = `M${point.from.x},${point.from.y}`;
    if (point.type === 'curve') {
      const { controlPoint } = point;
      const Q = `Q${controlPoint[0].x},${controlPoint[0].y},${controlPoint[1].x},${controlPoint[1].y}`;
      const T = `T${point.to.x},${point.to.y}`;
      const path = `${M} ${Q} ${T}`;
      line = this.snap.paper.path(path);
    } else if (point.type === 'polylineAcrossPoint' || point.type === 'polyline') {
      let path = M;

      point.controlPoint.forEach((p: any) => {
        path += ` L ${p.x}, ${p.y}`;
        path += ` a ${p.a}`;
      });

      path += ` L ${point.to.x}, ${point.to.y}`;
      line = this.snap.paper.path(path);
    } else {
      const path = `${M} L${point.to.x},${point.to.y}`;
      line = this.snap.paper.path(path);
    }
    const len: number = line.getTotalLength();
    line.attr({
      stroke: lineStyle ? lineStyle.borderColor : BORDER_COLOR,
      fill: 'transparent',
      strokeWidth: 1,
    });

    line.click(() => {
      if (this.selectedLine) {
        this.selectedLine.line.attr({
          strokeWidth: 1,
          stroke: lineStyle ? lineStyle.borderColor : BORDER_COLOR,
        });
      }

      this.selectedLine = {
        len,
        point,
        line,
      };
      line.attr({
        stroke: SELECTED_COLOR,
      });
    });

    this.drawSignal(point, line);
    this.linesCache.push({
      point,
      line,
    });
  };

  private calculateAllLinePosition = (points: any[]) => {
    const { pointComponent, type } = this.props;
    const allLines: any[] = diceFLowUtil.calculateLinePositionByType(type, pointComponent, points);

    return sortByLineType(allLines);
  };

  private scroll = (e: any) => {
    e.stopPropagation();

    this.setState({
      svgLeft: this.pointsView.scrollLeft,
    });
  };

  private onMouseMove = (e: any) => {
    const { pointComponent, selectedItem, mouseDownPosition = { x: 0, y: 0 }, editing, lineStyle } = this.props;
    const { info } = pointComponent;
    if (selectedItem && editing && mouseDownPosition) {
      if (!this.newLine) {
        const line: INodeLine = {
          id: randomId(),
          fromPoint: selectedItem,
          type: 'line',
          from: {
            // @ts-ignore
            x: selectedItem.position.x + info.ITEM_WIDTH / 2,
            // @ts-ignore
            y: selectedItem.position.y + info.ITEM_HEIGHT,
          },
          to: {
            x: e.clientX - mouseDownPosition.x,
            y: e.clientY - mouseDownPosition.y,
          },
        };

        const svgLine = this.snap.paper.line(line.from.x, line.from.y, line.to.x, line.to.y);
        svgLine.attr({
          stroke: lineStyle ? lineStyle.borderColor : BORDER_COLOR,
          fill: 'transparent',
          strokeWidth: 1,
        });

        this.newLine = {
          svgLine,
          line,
        };
      } else {
        this.newLine.svgLine.attr({
          x2: e.clientX - mouseDownPosition.x,
          y2: e.clientY - mouseDownPosition.y,
        });
      }
    }
  };
}
