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

import { IPosition } from 'application/common/components/dice-yaml-editor-type';
import React from 'react';
import { Icon as CustomIcon, IF } from 'common';
import classnames from 'classnames';
import { Tooltip } from 'antd';
import './flow-item.scss';
import PointComponentAbstract from 'application/common/components/point-component-abstract';

export interface IFlowItem {
  id: number;
  title?: string;
  data: any;
  name: string;
  icon?: any;
  depNum?: number;
  disabled: boolean;
  status: 'center' | 'left' | 'right' | 'top' | 'bottom';
  lineTo: string[];
  position: IPosition;
  _position: IPosition;
  arrowPosition: IArrowPosition[];
}

export interface IArrowPosition extends IPosition {
  direction: 'left' | 'right' | 'top' | 'bottom';
}

export interface IDiceYamlEditorItemProps {
  className?: string;
  style?: any;
  pointType?: 'all' | 'top' | 'bottom' | 'none';
  item: IFlowItem;
  scale: number;
  maxWidth: number;
  onClick?: (item: IFlowItem, type?: IFlowItem['status']) => void;
}

interface IRelationItem {
  sourceAttr: string;
  relAttr: string;
  pk: boolean;
}

export default class DiceYamlEditorItem extends PointComponentAbstract<IDiceYamlEditorItemProps, any> {
  static info = {
    ITEM_WIDTH: 210,
    ITEM_HEIGHT: 74,
    ITEM_MARGIN_BOTTOM: 60,
    ITEM_MARGIN_RIGHT: 60,
    PADDING_LEFT: 0,
    PADDING_TOP: -16,
    ICON_WIDTH: 12,
    RX: 10,
  };

  info = DiceYamlEditorItem.info;

  private interval: any;

  componentWillUnmount(): void {
    if (this.interval) {
      clearInterval(this.interval);
      this.interval = null;
    }
  }

  render() {
    const { item, className, onClick } = this.props;
    let titleContent = null;
    const nameContent = (
      <div className="yaml-editor-item-alias">
        <span className="yaml-editor-item-alias-text">{item.title || '-'}</span>
      </div>
    );
    if (item.name) {
      titleContent = (
        <div className="yaml-editor-pipeline-item-title">
          <Tooltip title={item.name}>{item.name}</Tooltip>
        </div>
      );
    }

    const mergedClassNames = classnames('flow-item', className);

    let icon = <CustomIcon className="table-icon" type="table" />;
    let extra = null;

    if (item.status === 'center') {
      icon = <CustomIcon className="table-icon" type="radar-chart" />;
    } else {
      extra = (
        <ul className="relation-list mt-2">
          {item.data.relations.map(({ sourceAttr, relAttr, pk }: IRelationItem) => (
            <Tooltip title={`${sourceAttr}: ${relAttr}`}>
              <li className="relation-item flex justify-between items-center mt-2" key={sourceAttr}>
                <span className="relation-text nowrap">{`${sourceAttr}: ${relAttr}`}</span>
                <IF check={pk}>
                  <CustomIcon className="relation-icon" type="key" />
                </IF>
              </li>
            </Tooltip>
          ))}
        </ul>
      );
    }

    const style = {
      left: item.position.x,
      top: item.position.y,
    };

    return (
      <React.Fragment>
        <div style={style} onClick={() => onClick && onClick(item.data, item.status)} className={mergedClassNames}>
          <span className="yaml-editor-item-title-name w-full">
            {titleContent}
            {nameContent}
            {extra}
          </span>
          {icon}
          {this.renderPoints()}
        </div>
      </React.Fragment>
    );
  }

  private renderPoints = (): any => {
    const { item } = this.props;
    const baseStyle = {
      width: this.info.ICON_WIDTH,
      height: this.info.ICON_WIDTH,
    };
    const leftStyle = {
      ...baseStyle,
      left: -baseStyle.width / 2,
      top: this.info.ITEM_HEIGHT / 2 - baseStyle.height / 2,
    };
    const rightStyle = {
      ...baseStyle,
      right: -baseStyle.width / 2,
      top: this.info.ITEM_HEIGHT / 2 - baseStyle.height / 2,
    };
    const topStyle = {
      ...baseStyle,
      left: (this.info.ITEM_WIDTH - baseStyle.width) / 2,
      top: -baseStyle.height / 2,
    };
    const bottomStyle = {
      ...baseStyle,
      left: (this.info.ITEM_WIDTH - baseStyle.width) / 2,
      bottom: -baseStyle.height / 2,
    };

    switch (item.status) {
      case 'center':
        // 一个在右侧，两个在左右，三个在上左右
        return (
          <div>
            <span style={rightStyle} className="item-point right-point">
              <CustomIcon type="caret-top" />
            </span>
            <IF check={item.depNum > 1}>
              <span style={leftStyle} className="item-point left-point">
                <CustomIcon type="caret-top" />
              </span>
            </IF>
            <IF check={item.depNum > 2}>
              <span style={topStyle} className="item-point top-point">
                <CustomIcon type="caret-top" />
              </span>
            </IF>
            <IF check={item.depNum > 3}>
              <span style={bottomStyle} className="item-point">
                <CustomIcon type="caret-top bottom-point" />
              </span>
            </IF>
          </div>
        );
      case 'top':
        return (
          <span style={topStyle} className="item-point top-point">
            <CustomIcon type="caret-top" />
          </span>
        );
      case 'bottom':
        return (
          <span style={bottomStyle} className="item-point bottom-point">
            <CustomIcon type="caret-top" />
          </span>
        );
      case 'left':
        return (
          <span style={leftStyle} className="item-point left-point">
            <CustomIcon type="caret-top" />
          </span>
        );
      case 'right':
        return (
          <span style={rightStyle} className="item-point right-point">
            <CustomIcon type="caret-top" />
          </span>
        );
      default:
        return null;
    }
  };
}
