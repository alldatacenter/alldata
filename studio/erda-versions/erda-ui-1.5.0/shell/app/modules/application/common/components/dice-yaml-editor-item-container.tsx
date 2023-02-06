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

import { DiceFlowType, IEditorPoint } from 'application/common/components/dice-yaml-editor-type';
import { IDiceYamlEditorItem } from 'application/common/components/dice-yaml-editor-item';
import PointComponentAbstract from 'application/common/components/point-component-abstract';
import React from 'react';
import classnames from 'classnames';
import { isEqual } from 'lodash';

interface IDiceYamlEditorMoveViewProps {
  className?: string | null;
  onMouseDownItem: any;
  mouseDownPosition: any;
  points: any[];
  selectedItem: any;
  type: string;
  readonly?: boolean;
  editing?: boolean;
  isSelectedItem?: boolean;
  scale: number;
  maxWidth: number;
  maxHeight: number;
  pointComponent: PointComponentAbstract<any, any>;
  onMouseUpItem: (groupIndex: number, itemIndex: number, e: any) => void;
  onMouseDownOnItem: (item: any, e: any) => void;
  addLink: (item: IDiceYamlEditorItem, e: any) => void;
  addDepend: (item: any, e: any) => void;
  clickItem: (item: IDiceYamlEditorItem, type?: string) => void;
  groupMouseEnter: (groupIndex: number) => void;
  groupMouseLeave: () => void;
  deleteItem?: (service: IDiceYamlEditorItem) => void;
}

const Group = (props: any) => {
  return <div {...props}>{props.children}</div>;
};

export default class extends React.PureComponent<IDiceYamlEditorMoveViewProps, any> {
  state = {
    groupIndex: -1,
    x: 0,
    y: 0,
    selectedItemKey: null,
  };

  private inRender = false;

  private groupIndex: number;

  private selectedItem: any;

  static getDerivedStateFromProps(nextProps: any, prevState: any) {
    if (
      nextProps.onMouseDownItem &&
      (nextProps.onMouseDownItem.position.y !== prevState.y || nextProps.onMouseDownItem.position.x !== prevState.x)
    ) {
      return {
        scale: nextProps.scale,
        maxWidth: nextProps.maxWidth,
        maxHeight: nextProps.maxHeight,
        selectedItemKey: nextProps.isSelectedItem ? prevState.selectedItemKey : null,
        x: nextProps.onMouseDownItem.position.x,
        y: nextProps.onMouseDownItem.position.y,
      };
    }

    if (
      nextProps.maxWidth !== prevState.maxWidth ||
      nextProps.scale !== prevState.scale ||
      !isEqual(nextProps.points, prevState.points) ||
      nextProps.maxHeight !== prevState.maxHeight
    ) {
      return {
        scale: nextProps.scale,
        maxWidth: nextProps.maxWidth,
        maxHeight: nextProps.maxHeight,
      };
    }

    return null;
  }

  componentDidMount(): void {
    const { readonly, type } = this.props;
    if (!readonly || type === DiceFlowType.EDITOR) {
      const $el: any = document.getElementById('points-view');
      const $addEl: any = document.querySelector('.addons-container');
      document.body.addEventListener('mousemove', this.onMouseMove);
      $el.addEventListener('mouseup', this.onMouseUp);
      $addEl && $addEl.addEventListener('mousedown', this.onMouseDown);
    }
  }

  componentWillUnmount(): void {
    const { readonly, type } = this.props;
    if (!readonly || type === DiceFlowType.EDITOR) {
      const $el: any = document.getElementById('points-view');
      const $addEl: any = document.querySelector('.addons-container');
      document.body.removeEventListener('mousemove', this.onMouseMove);
      $el.removeEventListener('mouseup', this.onMouseUp);
      $addEl && $addEl.addEventListener('mousedown', this.onMouseDown);
    }
  }

  render(): React.ReactNode {
    const { x, y, groupIndex } = this.state;
    const {
      onMouseDownItem,
      points,
      groupMouseEnter,
      groupMouseLeave,
      isSelectedItem,
      pointComponent,
      scale,
      type,
      maxHeight,
      maxWidth,
    } = this.props;
    const style: any = {
      left: `${x}px`,
      top: `${y}px`,
    };

    const Component: any = pointComponent;
    const groupClassName = groupIndex === points.length ? 'drop-over dice-yaml-group' : 'dice-yaml-group';
    const containerClass = classnames(
      this.props.className,
      'item-container-view',
      isSelectedItem ? 'auto-z-index' : null,
      type === DiceFlowType.DATA_MARKET ? 'scale-view' : null,
      type === DiceFlowType.PIPELINE ? 'pipeline-view' : null,
    );

    let viewStyle: any = {
      width: maxWidth,
      height: maxHeight || '100%',
    };
    if (type === DiceFlowType.DATA_MARKET) {
      viewStyle = {
        transform: `scale(${scale})`,
        transformOrigin: '0 0',
      };
    }

    let newGroup = null;

    if (type === DiceFlowType.EDITOR) {
      newGroup = (
        <React.Fragment>
          <Group
            className={groupClassName}
            onMouseOver={() => groupMouseEnter(points.length)}
            onMouseLeave={groupMouseLeave}
          >
            <div className="new-point" />
          </Group>
          {onMouseDownItem ? <Component className="item-moving" style={style} item={onMouseDownItem} /> : null}
        </React.Fragment>
      );
    }

    return (
      <div id="points-view" style={viewStyle} className={containerClass}>
        {this.renderGroup()}
        {newGroup}
      </div>
    );
  }

  private renderGroup = () => {
    const { selectedItemKey, groupIndex } = this.state;
    const {
      points,
      pointComponent,
      readonly,
      scale,
      maxWidth,
      maxHeight,
      editing,
      onMouseDownOnItem,
      deleteItem,
      groupMouseEnter,
      groupMouseLeave,
      isSelectedItem,
      type,
    } = this.props;

    let hadNewItem: IEditorPoint;
    const Component: any = pointComponent;
    return points.map((item: any, pointGroupIndex: number) => {
      let group = [];
      let pointType: 'all' | 'bottom' | 'top' = 'all';
      if (pointGroupIndex === 0) {
        pointType = 'bottom';
      }

      if (readonly && pointGroupIndex === points.length - 1) {
        pointType = 'top';
      }

      let hadSelectedItem = false;
      if (!hadNewItem) {
        hadNewItem = item.find((i: any) => i.status === 'new');
      }
      group = item.map((subItem: IEditorPoint) => {
        const key = subItem.name + (subItem.data ? subItem.data.type || '' : '');
        // @ts-ignore
        const className = hadNewItem ? hadNewItem.status === subItem.status : selectedItemKey === key;
        if (className) {
          hadSelectedItem = true;
        }
        return (
          <Component
            scale={scale}
            maxWidth={maxWidth}
            maxHeight={maxHeight}
            editing={editing}
            selectedItem={className}
            key={subItem.id}
            onMouseDown={onMouseDownOnItem}
            addLink={this.addLink}
            addDepend={this.addDepend}
            deleteItem={deleteItem}
            pointType={pointType}
            onClick={this.clickItem}
            item={subItem}
          />
        );
      });

      const groupClassName =
        groupIndex !== -1 && groupIndex === pointGroupIndex ? 'drop-over dice-yaml-group' : 'dice-yaml-group';
      const selectedClass = hadSelectedItem && isSelectedItem ? 'selected-group' : null;

      if (type === DiceFlowType.EDITOR || type === DiceFlowType.PIPELINE) {
        return (
          <Group
            key={String(pointGroupIndex)}
            className={classnames(groupClassName, selectedClass)}
            onMouseOver={() => groupMouseEnter(pointGroupIndex)}
            onMouseLeave={groupMouseLeave}
          >
            {group}
            <div className="new-point" />
          </Group>
        );
      }
      return group;
    });
  };

  private clickItem = (item: any, type?: string) => {
    const { clickItem } = this.props;
    const key = item.name + (item.data ? item.data.type || '' : '');
    this.setState({
      selectedItemKey: key,
    });
    clickItem(item, type);
  };

  private onMouseMove = (e: any) => {
    const { mouseDownPosition, onMouseDownItem, readonly, editing, pointComponent, type } = this.props;
    const { info } = pointComponent;

    if (readonly || editing || type === DiceFlowType.PIPELINE) {
      return;
    }
    if (onMouseDownItem && !this.inRender) {
      const groupIndex = Math.round(
        // @ts-ignore
        (e.clientY - mouseDownPosition.y - info.PADDING_TOP) / (info.ITEM_HEIGHT + info.ITEM_MARGIN_BOTTOM),
      );
      this.inRender = true;
      this.setState(
        {
          groupIndex,
          x: e.clientX - mouseDownPosition.x,
          y: e.clientY - mouseDownPosition.y,
        },
        () => {
          this.inRender = false;
        },
      );
    } else if (!onMouseDownItem && this.groupIndex !== -1 && !this.selectedItem) {
      this.setState({
        groupIndex: -1,
        x: 0,
        y: 0,
      });
    }
  };

  private onMouseUp = () => {
    const { groupIndex } = this.state;
    let index = groupIndex;
    if (groupIndex !== -1) {
      index = -1;
      this.setState({
        groupIndex: -1,
      });
    }
    this.setState({
      groupIndex: index,
    });
    return true;
  };

  private addLink = (item: any, e: any) => {
    e.stopPropagation();
    const { addLink } = this.props;

    addLink(item, e);
  };

  private onMouseDown = () => {
    const { editing } = this.props;
    if (!editing) return;
    this.setState({
      selectedItemKey: null,
    });
  };

  private addDepend = (item: any, e: any) => {
    const { addDepend, editing } = this.props;
    if (!editing) return;
    this.selectedItem = item;
    this.setState({
      selectedItemKey: null,
      groupIndex: item.groupIndex + 1,
    });
    addDepend(item, e);
  };
}
