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

import React, { PureComponent } from 'react';
import { map, uniqueId } from 'lodash';
import classnames from 'classnames';
import './property-view.scss';

interface IPropertyViewProps {
  dataSource: any;
  className?: string;
}

export default class extends PureComponent<IPropertyViewProps, any> {
  render() {
    const { dataSource, className } = this.props;
    return <div className={classnames(className, 'property-view')}>{this.renderDataSource(dataSource)}</div>;
  }

  private renderDataSource(dataSource: any) {
    const contents: any[] = [];
    const type = dataSource instanceof Array ? 'array' : typeof dataSource;
    switch (type) {
      case 'array':
        contents.push(this.renderArray(dataSource));
        break;
      case 'object':
        contents.push(this.renderObject(dataSource));
        break;
      default:
        contents.push(this.renderValue(dataSource));
        break;
    }

    return contents.map((item) => item);
  }

  private renderObject(dataSource: object) {
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
          <span className={isObject ? 'object-key' : ''}>{key}: </span>
          <span className={isObject ? 'object-value' : ''}>{this.renderDataSource(value)}</span>
        </div>
      );
    });
  }

  private renderArray(dataSource: object) {
    return map(dataSource, (value: any, key: string) => {
      return <div key={uniqueId(`pv-array-${key}`)}>{this.renderDataSource(value)}</div>;
    });
  }

  private renderValue(value: any) {
    return <span key={uniqueId(`pv-v-${value}`)}>{value}</span>;
  }
}
