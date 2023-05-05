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
import { Form, Avatar } from 'antd';
import { forEach, map, isPlainObject, get } from 'lodash';

interface IReadonlyProps {
  fieldsList: IField[];
  data?: any;
}
interface IField {
  label?: string;
  name?: string;
  viewType?: string;
  itemProps?: {
    type?: string;
    [proName: string]: any;
  };
  getComp?: (o?: any) => any;
}

const ReadonlyForm = ({ fieldsList, data }: IReadonlyProps) => {
  const readonlyView: JSX.Element[] = [];
  forEach(fieldsList, (item, index) => {
    const { itemProps, label, name, viewType, getComp } = item;
    if (!(itemProps && itemProps.type === 'hidden') && label) {
      const value = name === undefined && getComp ? getComp() : get(data, name || '');
      if (value !== undefined && value !== null && value !== '') {
        if (viewType === 'image') {
          readonlyView.push(
            <Form.Item label={label} key={index}>
              <Avatar shape="square" src={value} size={100} />
            </Form.Item>,
          );
        } else {
          readonlyView.push(
            <Form.Item label={label} key={index}>
              <p style={{ wordBreak: 'break-all' }}>
                {Array.isArray(value) ? (
                  map(value, (v: string, i: number) => (
                    <span key={`${i}${v}`}>
                      &nbsp;&nbsp;{isPlainObject(v) ? JSON.stringify(v) : v.toString()}
                      <br />
                    </span>
                  ))
                ) : (
                  <>&nbsp;&nbsp;{value !== undefined ? value.toString() : undefined}</>
                )}
              </p>
            </Form.Item>,
          );
        }
      }
    }
  });
  return <Form layout="vertical">{readonlyView}</Form>;
};

export default ReadonlyForm;
