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
import { Form, Row, Col } from 'antd';
import { map, some, isBoolean, has } from 'lodash';
import { FormItemProps } from 'core/common/interface';
import i18n from 'i18n';
import { FormContext } from './form-builder';
import type { IContextType } from './form-builder';
import ReadonlyField from './readonly-field';

const { Item } = Form;

/**
 * Fields is a Form.Item list, so it must be wrapped in FormBuilder which is a Form container.
 *
 * fields: {
 *  type: your component, like Input, Select etc.
 *  customProps: props of your component.
 *  wrapperClassName: the style class name of Item's wrapper.
 *  isHoldLabel: whether to occupy a position when label is empty, default true.
 *  readonly: whether to use readonly Item, it can bool or object,
 *            when object , you can set renderItem to render a
 *            customized component you want.
 *            weight: fields' item's > Fields' > FormBuilder's
 * },
 * fid: Fields id, must be unique.
 * isMultiColumn: whether use multiple column or not.
 * columnNum: amount of column, only become effective when isMultiColumn is true. default Adaptive.
 * readonly: whether all Form.Items in Fields is readonly, default false.
 *
 * else : The same as antd Form.Item.
 *
 *
 * example:
 *
 *  <FormBuilder>
 *    <Fields
 *      fields={[{ type: Input, customProps: { maxLength: 10 } }]}
 *      fid="basic-fields"
 *    />
 *  </FormBuilder>
 * */
export interface IFieldType extends FormItemProps {
  type?: React.ElementType;
  customProps?: React.ComponentProps<any>;
  wrapperClassName?: string;
  colSpan?: number;
  isHoldLabel?: boolean;
  readonly?:
    | boolean
    | {
        renderItem: React.ReactNode;
        style?: React.CSSProperties;
        className?: string;
      };
}

interface IProps {
  fields: IFieldType[];
  fid?: string;
  isMultiColumn?: boolean;
  columnNum?: number;
  readonly?: boolean;
}

export const Fields: React.MemoExoticComponent<
  ({ fields, isMultiColumn, columnNum, readonly, fid }: IProps) => JSX.Element
> = React.memo(({ fields = [], isMultiColumn, columnNum, readonly, fid }: IProps) => {
  const getColumn = (contextProps: IContextType) => {
    if (isMultiColumn || (isMultiColumn === undefined && contextProps.parentIsMultiColumn)) {
      if (columnNum) return columnNum;
      if (contextProps.parentColumnNum) return contextProps.parentColumnNum;
      return contextProps.realColumnNum;
    }
    return 1;
  };

  return (
    <FormContext.Consumer>
      {(contextProps) => {
        if (!contextProps) return null;
        fid && contextProps.setFieldsInfo(fid, fields);
        const fieldRealColumnNum = getColumn(contextProps);
        if (!fieldRealColumnNum) return null;
        return (
          <Row gutter={[20, 0]}>
            {map(fields, (item, idx) => {
              const {
                type: Comp,
                customProps = {},
                required = true,
                rules = [],
                readonly: itemReadonly,
                className,
                wrapperClassName,
                label,
                isHoldLabel = true,
                colSpan,
                ...rest
              } = item;
              const afterAddRequiredRules =
                required && !some(rules, (rule) => has(rule, 'required'))
                  ? [{ required: true, message: i18n.t('{label} can not be empty', { label }) }, ...rules]
                  : rules;
              const isRealReadOnly =
                (itemReadonly !== undefined
                  ? itemReadonly
                  : readonly !== undefined
                  ? readonly
                  : contextProps?.parentReadonly) || false;
              const realReadData = isBoolean(isRealReadOnly) ? null : isRealReadOnly;
              return (
                <Col span={colSpan || 24 / fieldRealColumnNum} key={idx} className={wrapperClassName}>
                  <Item
                    label={label || (isHoldLabel ? <div /> : null)}
                    colon={!!label}
                    required={required}
                    rules={afterAddRequiredRules}
                    className={`${label ? '' : 'no-label'} ${className || ''}`}
                    style={{ marginBottom: 6 }}
                    initialValue={customProps.defaultValue}
                    {...rest}
                  >
                    {isRealReadOnly ? (
                      <ReadonlyField
                        {...customProps}
                        {...realReadData}
                        renderData={realReadData && realReadData.renderItem}
                      />
                    ) : (
                      Comp && <Comp {...customProps} />
                    )}
                  </Item>
                </Col>
              );
            })}
          </Row>
        );
      }}
    </FormContext.Consumer>
  );
});
