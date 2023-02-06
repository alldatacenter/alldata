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
import classnames from 'classnames';
import './group.scss';

const FormGroupComp = (p: any) => {
  const { fieldConfig, children } = p || {};
  const { key, componentProps } = fieldConfig || {};
  const {
    title,
    showDivider,
    indentation,
    expandable = false,
    defaultExpand = false,
    direction = 'column',
  } = componentProps || {};
  const [expandStatus, setExpandStatus] = React.useState(defaultExpand);
  const curShowDivider = expandable || showDivider;

  const cls = classnames({
    'border-bottom': curShowDivider,
    'cursor-pointer': expandable,
    'hover-active-bg': expandable,
  });

  const onClick = () => {
    if (expandable) {
      setExpandStatus(!expandStatus);
    }
  };

  return (
    <div className={`dice-form-group my-3 ${expandable && !expandStatus ? 'hide-children' : ''}`}>
      <div
        className={`dice-form-group-title text-sm font-bold py-1 px-0.5 flex justify-between items-center ${cls}`}
        onClick={onClick}
      >
        <span>{title || key}</span>
        {expandable ? <CustomIcon type="chevron-down" className="expand-icon" /> : null}
      </div>
      <div
        className={`dice-form-group-children ${indentation ? 'pl-4' : ''} ${
          direction === 'row' ? 'dice-form-group-children-row' : ''
        }`}
      >
        {children}
      </div>
    </div>
  );
};

export const FormGroup = () => React.memo((p: any) => <FormGroupComp {...p} />);

export const config = {
  name: 'formGroup',
  Component: FormGroup, // 某React组件，props中必须有value、onChange
};

export const formConfig = {
  formGroup: {
    name: '表单组',
    value: 'formGroup',
    fieldConfig: {
      componentProps: {
        key: 'componentProps',
        name: '组件配置',
        fields: [
          {
            label: '表单组key',
            key: 'key',
            type: 'input',
            component: 'input',
            labelTip: '字段唯一',
            disabled: true,
          },
          {
            label: '组名称',
            key: 'componentProps.title',
            type: 'input',
            component: 'input',
            componentProps: {
              placeholder: '请输入组名称',
            },
          },
          {
            label: '是否显示分割线',
            key: 'componentProps.showDivider',
            type: 'switch',
            component: 'switch',
          },
          {
            label: '是否缩进子表单',
            key: 'componentProps.indentation',
            type: 'switch',
            component: 'switch',
          },
          {
            label: '是否可折叠',
            key: 'componentProps.expandable',
            type: 'switch',
            component: 'switch',
            defaultValue: false,
          },
          {
            label: '默认展开',
            key: 'componentProps.defaultExpand',
            type: 'switch',
            component: 'switch',
            defaultValue: false,
          },
        ],
      },
    },
  },
};
