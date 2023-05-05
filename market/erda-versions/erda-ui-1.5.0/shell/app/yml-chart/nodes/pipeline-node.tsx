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
import { Icon as CustomIcon } from 'common';
import { Dropdown, Menu } from 'antd';
import { map, uniqueId } from 'lodash';
import { i18nMap } from '../common/pipeline-node-drawer';
import i18n from 'i18n';
import './pipeline-node.scss';

export interface IProps {
  data: Obj;
  editing: boolean;
  onClickNode: (data: any, arg: any) => void;
  onDeleteNode: (data: any) => void;
}

const noop = () => {};
export const PipelineNode = (props: IProps) => {
  const { data, editing, onClickNode = noop, onDeleteNode = noop, ...rest } = props;

  const menu = (
    <Menu
      onClick={({ domEvent, key }: any) => {
        domEvent && domEvent.stopPropagation();
        if (key === 'delete') {
          onDeleteNode(data);
        }
      }}
    >
      <Menu.Item key="delete">{i18n.t('delete')}</Menu.Item>
    </Menu>
  );

  const onClick = () => {
    onClickNode(data, { editing, ...rest });
  };

  return (
    <div className="yml-chart-node pipeline-node flex flex-col justify-center" onClick={onClick}>
      <div className={'pipeline-title py-3'}>
        <div className="title-icon mr-3">
          {data.logoUrl ? (
            <img src={data.logoUrl} alt="logo" />
          ) : (
            <CustomIcon type="wfw" color className="w-full h-full" />
          )}
        </div>
        <div className="title-txt flex flex-col justify-center text-normal">
          <span className="mb-1 nowrap text-base font-bold name">{data.displayName || data.type}</span>
          <span className="nowrap text-xs type">{data.alias}</span>
        </div>
        {editing ? (
          <div>
            <Dropdown trigger={['click']} overlay={menu}>
              <CustomIcon type="more" onClick={(e) => e.stopPropagation()} />
            </Dropdown>
          </div>
        ) : null}
      </div>
      <div className="pipeline-content flex-1 py-3">{data.description}</div>
    </div>
  );
};

const renderDataSource = (dataSource: any) => {
  const contents: any[] = [];
  const type = dataSource instanceof Array ? 'array' : typeof dataSource;
  switch (type) {
    case 'array':
      contents.push(renderArray(dataSource));
      break;
    case 'object':
      contents.push(renderObject(dataSource));
      break;
    default:
      contents.push(renderValue(dataSource));
      break;
  }

  return contents.map((item) => item);
};

const renderObject = (dataSource: object) => {
  return map(dataSource, (value: any, key: string) => {
    const type = value instanceof Array ? 'array' : typeof value;
    let isObject = false;
    if (type === 'array' && (!value || (value && value.length === 0))) {
      return null;
    } else if (!value && value !== '') {
      return null;
    }

    switch (type) {
      case 'array':
      case 'object':
        isObject = true;
        break;
      default:
        break;
    }
    return (
      <div key={uniqueId(`pv-obj-${key}`)}>
        <span className={isObject ? 'object-key' : ''}>{i18nMap[key] || key}: </span>
        <span className={isObject ? 'object-value' : ''}>{renderDataSource(value)}</span>
      </div>
    );
  });
};

const renderArray = (dataSource: object) => {
  return map(dataSource, (value: any, key: string) => {
    return <div key={uniqueId(`pv-array-${key}`)}>{renderDataSource(value)}</div>;
  });
};

const renderValue = (value: any) => {
  return <span key={uniqueId(`pv-v-${value}`)}>{value}</span>;
};
