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
import { TagsRow, Panel, Ellipsis } from 'common';
import Text from '../text/text';
import { map } from 'lodash';

interface IField extends CP_PANEL.Field {
  valueItem?: (props: Obj) => any;
}

const CP_PANEL = (props: CP_PANEL.Props) => {
  const { props: configProps, data, execOperation } = props || {};
  const { visible = true, fields, ...rest } = configProps || {};

  if (!visible) return null;
  const curData = data?.data;
  const _fields: IField[] = map(fields, (item) => {
    const { renderType, operations, ...itemRest } = item;
    const reField: IField = { ...item };
    switch (renderType) {
      case 'ellipsis':
        reField.valueItem = (_p: Obj) => <Ellipsis title={_p.value} />;
        break;
      case 'tagsRow':
        {
          const { showCount = 2 } = itemRest;
          const onAdd = operations?.add && (() => execOperation(operations?.add));
          const onDelete = operations?.delete && ((record: Object) => execOperation(operations?.delete, record));
          reField.valueItem = () => (
            <TagsRow
              showCount={showCount}
              labels={curData?.[reField.valueKey] || []}
              onAdd={onAdd}
              onDelete={onDelete}
            />
          );
        }
        break;
      case 'linkText':
        {
          const _p: Obj = {};
          if (operations?.click) {
            _p.onClick = (e: MouseEvent) => {
              e.stopPropagation();
              execOperation(operations.click);
            };
          }
          reField.valueItem = (_props) => {
            return (
              <span className="fake-link" {..._p}>
                {_props.value}
              </span>
            );
          };
        }
        break;
      case 'copyText':
        reField.valueItem = (_props: Obj) => (
          <Text type="Text" props={{ renderType: 'copyText', value: { text: _props.value } }} />
        );
        break;
      default:
        break;
    }
    return reField;
  });
  return <Panel {...rest} fields={_fields} data={curData} />;
};
export default CP_PANEL;
