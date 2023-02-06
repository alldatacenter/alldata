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

import React, { Component } from 'react';
import DiceYamlEditorItem, { IDiceYamlEditorItem } from './dice-yaml-editor-item';
import FlowItem from './flow-item';
import PipielineNode from './pipeline-node';
import { cloneDeep, findIndex, isEqualWith } from 'lodash';
import classnames from 'classnames';
import IDiceYamlEditorMoveView from './dice-yaml-editor-item-container';
import IDiceYamlCanvas from './dice-yaml-canvas';
import { getItemName, randomId, isEqualCustomizer } from '../yml-flow-util';
import diceFlowUtil from '../dice-flow-util';
// @ts-ignore
import './dice-yaml-editor.scss';
import { IDiceYamlEditorProps, INodeLine, DiceFlowType } from './dice-yaml-editor-type';

const PointComponent = {
  [DiceFlowType.EDITOR]: DiceYamlEditorItem,
  [DiceFlowType.DATA_MARKET]: FlowItem,
  [DiceFlowType.PIPELINE]: PipielineNode,
};

export default class DiceYamlEditor extends Component<IDiceYamlEditorProps, any> {
  state = {
    selectedItem: null,
    onMouseDownItem: null,
    mouseDownPosition: null,
    connectSiblingNode: true,
    points: [],
    maxWidth: 0,
    maxHeight: 0,
    scale: 1,
  };

  private groupMouseEnterIndex: number;

  private selectToLinkItem: any;

  private selectedLine: any;

  private pointComponent: any;

  private resizeTimeout: any;

  private newLine: {
    svgLine: any;
    line: INodeLine;
  };

  async UNSAFE_componentWillMount() {
    document.body.addEventListener('mouseup', this.onMouseUp);
    document.body.addEventListener('keyup', this.onKeyUp);
    window.addEventListener('resize', this.resize);
    this.loadData(this.props);
  }

  componentWillUnmount(): void {
    document.body.removeEventListener('mouseup', this.onMouseUp);
    document.body.removeEventListener('keyup', this.onKeyUp);
    window.removeEventListener('resize', this.resize);
    if (this.resizeTimeout) {
      clearTimeout(this.resizeTimeout);
    }
  }

  async UNSAFE_componentWillReceiveProps(nextProps: Readonly<IDiceYamlEditorProps>) {
    const isDataSourceChanged = !isEqualWith(nextProps.dataSource, this.props.dataSource, isEqualCustomizer);

    if (isDataSourceChanged || !nextProps.isSelectedItem) {
      this.loadData(nextProps);
    }
  }

  render() {
    const { selectedItem, points, onMouseDownItem, mouseDownPosition, maxWidth, maxHeight, scale } = this.state;
    const { type } = this.props;
    const className = classnames('yaml-editor-content', type === 'DATA_MARKET' ? 'data-market' : null);

    return (
      <div id="yaml-editor-content" className={className}>
        <IDiceYamlCanvas
          {...this.props}
          maxWidth={maxWidth}
          maxHeight={maxHeight}
          scale={scale || 1}
          pointComponent={this.pointComponent}
          selectedItem={selectedItem}
          dataSource={points}
          mouseDownPosition={mouseDownPosition}
        />
        <IDiceYamlEditorMoveView
          {...this.props}
          pointComponent={this.pointComponent}
          maxWidth={maxWidth}
          maxHeight={maxHeight}
          scale={scale || 1}
          points={points}
          onMouseDownOnItem={this.onMouseDownOnItem}
          onMouseUpItem={this.onMouseUpItem}
          addDepend={this.addDepend}
          addLink={this.addLink}
          clickItem={this.clickItem}
          selectedItem={selectedItem}
          groupMouseEnter={this.groupMouseEnter}
          groupMouseLeave={this.groupMouseLeave}
          onMouseDownItem={onMouseDownItem}
          mouseDownPosition={mouseDownPosition}
        />
      </div>
    );
  }

  private resize = () => {
    if (this.resizeTimeout) {
      clearTimeout(this.resizeTimeout);
      this.resizeTimeout = null;
    }

    this.resizeTimeout = setTimeout(() => {
      const result = this.calculateItemPosition(this.props);
      this.setState(
        {
          ...result,
        },
        () => {
          this.resizeTimeout = null;
        },
      );
    }, 300);
  };

  private loadData(props: IDiceYamlEditorProps) {
    this.pointComponent = PointComponent[props.type];
    const result = this.calculateItemPosition(props);
    this.setState({
      ...result,
      connectSiblingNode: props.connectSiblingNode,
    });
  }

  /**
   * 计算所有节点位置信息
   */
  private calculateItemPosition = (props: any): any => {
    const { dataSource, type } = props;
    if (!this.pointComponent) {
      return;
    }
    return diceFlowUtil.calculateItemPositionByType(type, this.pointComponent, dataSource);
  };

  private addDepend = (item: any, e: any) => {
    const { editing } = this.props;
    const { info } = this.pointComponent;
    if (!editing) return;
    this.setState({
      selectedItem: item,
      mouseDownPosition: {
        x: e.clientX - (item.position.x + info.ITEM_WIDTH / 2),
        y: e.clientY - (item.position.y + info.ITEM_HEIGHT),
      },
    });
    e.stopPropagation();
    return false;
  };

  private onKeyUp = (e: any) => {
    const { points } = this.state;
    if (this.selectedLine && e.keyCode === 8) {
      const { fromPoint } = this.selectedLine.point;
      const { toPoint } = this.selectedLine.point;
      // @ts-ignore
      const lineTo = points[fromPoint.groupIndex][fromPoint.index].lineTo || [];

      const index = findIndex(lineTo, (o) => {
        return o === toPoint.name;
      });
      lineTo.splice(index, 1);
      this.setState({
        points: cloneDeep(points),
      });
      this.selectedLine.line.remove();
      this.selectedLine = null;
    }
  };

  private onMouseUp = (e: any): boolean => {
    const { getCreateView, updateConnect } = this.props;
    const { points, onMouseDownItem, mouseDownPosition, selectedItem, connectSiblingNode }: any = this.state;
    const { info } = this.pointComponent;

    if (this.groupMouseEnterIndex) {
      // 连接某个组
      if (this.groupMouseEnterIndex - selectedItem.groupIndex !== 1) {
        this.reset();
        return true;
      }
      const item: any = points[this.groupMouseEnterIndex];
      const name = getItemName();
      const newItem: any = {
        name,
        id: randomId(),
        title: name,
        status: 'new',
        groupIndex: this.groupMouseEnterIndex,
        content: () => null,
      };
      const point: any = points[selectedItem.groupIndex][selectedItem.index];
      point.lineTo = [...(point.lineTo || []), newItem.name];

      getCreateView && getCreateView(newItem, point.name);
      newItem.index = item ? item.length : 0;
      if (item && item.length) {
        newItem.position = {
          x: item[item.length - 1].position.x + info.ITEM_WIDTH + info.ITEM_MARGIN_RIGHT - 1,
          y: item[item.length - 1].position.y,
        };
        item.push(newItem);
      } else {
        points[newItem.groupIndex] = [];
        newItem.position = {
          x: info.PADDING_LEFT,
          y: (newItem.groupIndex + 1) * info.ITEM_HEIGHT + newItem.groupIndex * info.ITEM_MARGIN_BOTTOM,
        };
        points[newItem.groupIndex].push(newItem);
      }

      this.setState(
        {
          selectedItem: null,
          points: cloneDeep(points),
        },
        () => {
          if (this.newLine) {
            this.newLine.svgLine.remove();
          }
        },
      );
      this.reset();
    } else if (this.selectToLinkItem && selectedItem) {
      const linked =
        selectedItem.lineTo && selectedItem.lineTo.find((item: any) => item === this.selectToLinkItem.name);
      // 连接某个节点
      if (!connectSiblingNode && (this.selectToLinkItem.groupIndex <= selectedItem.groupIndex || linked)) {
        this.reset();
        return true;
      }

      const point: any = points[selectedItem.groupIndex][selectedItem.index];
      point.lineTo = [...(point.lineTo || []), ...this.selectToLinkItem.name];

      updateConnect && updateConnect(selectedItem.name, this.selectToLinkItem.name);
      this.setState({
        selectedItem: null,
        points: cloneDeep(points),
      });
    } else if (onMouseDownItem) {
      const groupIndex = Math.round(
        (e.clientY - mouseDownPosition.y - info.PADDING_TOP) / (info.ITEM_HEIGHT + info.ITEM_MARGIN_BOTTOM),
      );

      if (groupIndex === onMouseDownItem.groupIndex || groupIndex === 0) {
        this.setState({
          onMouseDownItem: null,
        });
        return true;
      }
      let itemIndex = 0;

      if (points[groupIndex] && points[groupIndex].length) {
        itemIndex = points[groupIndex].length;
      }

      this.onMouseUpItem(groupIndex, itemIndex, e);
      this.reset();
    } else if (selectedItem) {
      this.reset();
    }

    return true;
  };

  private reset = () => {
    this.groupMouseEnterIndex = 0;
    this.selectToLinkItem = null;
    if (this.newLine) {
      this.newLine.svgLine.remove();
    }

    this.setState({
      selectedItem: null,
    });
  };

  private onMouseUpItem = (groupIndex: number, itemIndex: number, e: any) => {
    e.stopPropagation();
    const { onMouseDownItem, points }: any = this.state;
    const { info } = this.pointComponent;
    if (!onMouseDownItem) {
      this.reset();
      return;
    }

    const newItem: any = cloneDeep(onMouseDownItem);
    newItem.id = randomId();
    newItem.groupIndex = groupIndex;
    newItem.index = itemIndex > 0 ? itemIndex : 0;
    newItem.position = {
      x: info.PADDING_LEFT + itemIndex * (info.ITEM_WIDTH + info.ITEM_MARGIN_RIGHT),
      y: info.PADDING_TOP + groupIndex * (info.ITEM_HEIGHT + info.ITEM_MARGIN_BOTTOM),
    };
    newItem._position = {
      x: info.PADDING_LEFT + itemIndex * (info.ITEM_WIDTH + info.ITEM_MARGIN_RIGHT),
      y: info.PADDING_TOP + groupIndex * (info.ITEM_HEIGHT + info.ITEM_MARGIN_BOTTOM),
    };

    if (points[onMouseDownItem.groupIndex] instanceof Array) {
      points[onMouseDownItem.groupIndex].splice(onMouseDownItem.index, 1);
    } else {
      points.splice(onMouseDownItem.groupIndex, 1);
    }

    if (points[groupIndex] && points[groupIndex] instanceof Array) {
      points[groupIndex].splice(itemIndex > 0 ? itemIndex : 0, 0, newItem);
    } else {
      points.splice(groupIndex, 0, newItem);
    }

    // 删除空的组
    if (points[onMouseDownItem.groupIndex] instanceof Array) {
      if (!points[onMouseDownItem.groupIndex].length) {
        points.splice(onMouseDownItem.groupIndex, 1);
      }
    }

    const result = this.calculateItemPosition({
      ...this.props,
      dataSource: points,
    });

    this.setState({
      ...result,
      onMouseDownItem: null,
    });
    this.reset();
  };

  private addLink = (item: IDiceYamlEditorItem, e: any) => {
    const { selectedItem } = this.state;
    const { editing } = this.props;
    if (!editing) return;
    e.stopPropagation();
    if (selectedItem) {
      this.groupMouseEnterIndex = 0;
      this.selectToLinkItem = item;
    } else {
      this.selectToLinkItem = null;
    }
  };

  private clickItem = (item: IDiceYamlEditorItem, type?: string) => {
    const { clickItem } = this.props;

    if (clickItem) {
      clickItem(item, type);
    }
  };

  private onMouseDownOnItem = (item: any, e: any) => {
    e.stopPropagation();
    const { editing } = this.props;
    if (!editing) return;

    if (item.allowMove === undefined || item.allowMove === true) {
      this.setState({
        onMouseDownItem: item,
        mouseDownPosition: {
          x: e.clientX - item.position.x,
          y: e.clientY - item.position.y,
        },
      });
    }
  };

  private groupMouseEnter = (groupIndex: number) => {
    const { selectedItem } = this.state;
    const { editing } = this.props;
    if (!editing) return;
    if (selectedItem) {
      this.groupMouseEnterIndex = groupIndex;
    }
  };

  private groupMouseLeave = () => {
    const { selectedItem } = this.state;
    if (selectedItem) {
      this.groupMouseEnterIndex = -1;
    }
  };
}
