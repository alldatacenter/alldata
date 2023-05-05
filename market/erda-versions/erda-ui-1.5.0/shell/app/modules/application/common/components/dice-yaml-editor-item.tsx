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
import { Dropdown, Menu } from 'antd';
import classnames from 'classnames';
import { Icon as CustomIcon, ErdaIcon } from 'common';
import PointComponentAbstract from './point-component-abstract';
import i18n from 'i18n';

export interface IDiceYamlEditorItem {
  id: number;
  title?: string;
  data: any;
  name: string;
  icon?: any;
  status?: string;
  allowMove?: boolean;
  // 只有为false时，才不能删除节点
  allowDelete?: boolean;
  lineTo: string[];
  editView?: React.ReactNode;
  createView?: React.ReactNode;
  content?: () => string | React.ReactNode;
}

export interface IDiceYamlEditorItemProps {
  className?: string;
  style?: any;
  pointType?: 'all' | 'top' | 'bottom' | 'none';
  activePoint?: boolean;
  editing: boolean;
  selectedItem: any;
  item: IDiceYamlEditorItem;
  onMouseDown?: (item: IDiceYamlEditorItem, e: any) => void;
  deleteItem?: (service: IDiceYamlEditorItem) => void;
  addLink?: (item: IDiceYamlEditorItem, e: any) => void;
  addDepend?: (item: IDiceYamlEditorItem, e: any) => void;
  onClick?: (item: IDiceYamlEditorItem) => void;
}

export default class DiceYamlEditorItem extends PointComponentAbstract<IDiceYamlEditorItemProps, any> {
  static info = {
    ITEM_WIDTH: 280,
    ITEM_HEIGHT: 170,
    ITEM_MARGIN_BOTTOM: 60,
    ITEM_MARGIN_RIGHT: 60,
    PADDING_LEFT: 40,
    PADDING_TOP: 78,
    ICON_WIDTH: 12,
    RX: 20,
  };

  info = DiceYamlEditorItem.info;

  render() {
    const { deleteItem, item, addLink, onMouseDown, style, editing, selectedItem } = this.props;
    const className = selectedItem ? 'selected-item' : null;
    let titleContent = null;
    let nameContent = null;
    if (item.title) {
      titleContent = <div className="yaml-editor-item-title">{item.name}</div>;
    }
    if (item.name) {
      nameContent = <div className="yaml-editor-item-name">{item.title}</div>;
    } else {
      titleContent = <div className="yaml-editor-item-one-title">{item.name || item.title}</div>;
    }

    let menu;
    if (item.allowDelete === false || item.status === 'new') {
      menu = (
        <Menu>
          <Menu.Item onClick={() => this.onClick(item)}>{i18n.t('edit')}</Menu.Item>
        </Menu>
      );
    } else {
      menu = (
        <Menu>
          <Menu.Item onClick={() => this.onClick(item)}>{i18n.t('edit')}</Menu.Item>
          <Menu.Item onClick={() => deleteItem && deleteItem(item)}>{i18n.t('delete')}</Menu.Item>
        </Menu>
      );
    }

    return (
      <div
        key={item.id}
        style={style}
        onMouseOver={(e) => addLink && addLink(item, e)}
        className={classnames('yaml-editor-item', className)}
      >
        <div className={classnames('yaml-item-title-container', onMouseDown ? 'yaml-item-title-container-move' : null)}>
          <span className="yaml-editor-item-icon">
            <CustomIcon type={item.icon} color />
          </span>
          <span className="yaml-editor-item-title-name" onClick={() => this.onClick(item)}>
            {titleContent}
            {nameContent}
          </span>
          {editing ? (
            <Dropdown trigger={['click']} overlay={menu}>
              <ErdaIcon type="more1" className="icon-ellipsis mr-0" />
            </Dropdown>
          ) : null}
        </div>
        <div className="yaml-editor-item-content" onClick={() => this.onClick(item)}>
          {item.content && item.content()}
        </div>
        {this.renderPoints()}
      </div>
    );
  }

  private onClick = (data: any) => {
    const { onClick, item } = this.props;
    if (item.status === 'new') {
      return;
    }
    onClick && onClick(data);
  };

  private renderPoints = (): any => {
    const { pointType, activePoint, addDepend, item } = this.props;

    let activePointClass = null;
    if (activePoint) {
      activePointClass = 'active-point';
    }

    switch (pointType) {
      case 'none':
        return null;
      case 'top':
        return (
          <span className={`item-point top-point ${activePointClass}`}>
            <CustomIcon type="caret-top" />
          </span>
        );
      case 'bottom':
        return (
          <span
            onMouseDown={(e) => addDepend && addDepend(item, e)}
            className={`item-point bottom-point ${activePointClass}`}
          >
            <CustomIcon type="caret-top" />
          </span>
        );
      default:
        return (
          <div>
            <span className={`item-point top-point ${activePointClass}`}>
              <CustomIcon type="caret-top" />
            </span>
            <span
              onMouseDown={(e) => addDepend && addDepend(item, e)}
              className={`item-point bottom-point ${activePointClass}`}
            >
              <CustomIcon type="caret-top" />
            </span>
          </div>
        );
    }
  };
}
