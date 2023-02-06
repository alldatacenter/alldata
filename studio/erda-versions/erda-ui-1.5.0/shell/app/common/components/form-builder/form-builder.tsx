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

/* eslint-disable no-param-reassign */
import React from 'react';
import { Form } from 'antd';
import { FormInstance, FormProps } from 'core/common/interface';
import ResizeObserver from 'rc-resize-observer';
import { throttle } from 'lodash';
import { IFieldType } from './fields';

export interface IContextType {
  realColumnNum?: number;
  parentColumnNum?: number;
  parentIsMultiColumn?: boolean;
  parentReadonly?: boolean;
  setFieldsInfo: (k: string, v: IFieldType[]) => void;
}
export interface IFormExtendType<T = any> extends FormInstance {
  validateFieldsAndScroll?: (scb: (values: T) => void, fcb?: (err?: Obj<ErrorEvent>) => void) => void;
  fieldsInfo?: { [key: string]: IFieldType[] };
}

export const FormContext = React.createContext<Nullable<IContextType>>(null);

interface IPureProps<T> extends IProps {
  form: FormInstance & IFormExtendType<T>;
}

interface IProps extends FormProps {
  /**
   * isMultiColumn: whether to use multiple column or not
   * columnNum: amount of column, only become effective when isMultiColumn is true
   * readonly: whether all the Form.Item in Form is readonly, default false.
   * else: the same as antd Form
   */
  children: React.ReactNode;
  isMultiColumn?: boolean;
  columnNum?: number;
  readonly?: boolean;
  layout?: 'horizontal' | 'vertical' | 'inline';
  onSubmit?: () => void;
}

const PureFormBuilder = <T extends Obj>({
  form,
  children,
  layout = 'vertical',
  isMultiColumn,
  columnNum,
  readonly,
  ...rest
}: IPureProps<T>) => {
  const [realColumnNum, setRealColumnNum] = React.useState<number | undefined>();

  form.validateFieldsAndScroll = (successCallBack, errorCallBack) => {
    form
      .validateFields()
      .then(successCallBack)
      .catch((err) => {
        errorCallBack?.(err);
        form.scrollToField(err.errorFields[0]?.name);
      });
  };

  form.fieldsInfo = {};

  const setFieldsInfo = (key: string, value: IFieldType[]) => {
    form?.fieldsInfo?.[key] && (form.fieldsInfo[key] = value);
  };

  const handleResize = throttle(({ width }: { width: number }) => {
    let columns = 1;
    if (width <= 400) {
      columns = 1;
    } else if (width < 600) {
      columns = 2;
    } else if (width < 1024) {
      columns = 3;
    } else if (width < 1440) {
      columns = 4;
    } else if (width < 1920) {
      columns = 6;
    } else {
      columns = 8;
    }
    setRealColumnNum(columns);
  }, 500);

  return (
    <ResizeObserver onResize={handleResize}>
      <div className="w-full erda-form-builder">
        <Form {...rest} form={form} layout={layout}>
          <FormContext.Provider
            value={{
              realColumnNum,
              parentIsMultiColumn: isMultiColumn,
              parentColumnNum: columnNum,
              parentReadonly: readonly,
              setFieldsInfo,
            }}
          >
            {children}
          </FormContext.Provider>
        </Form>
      </div>
    </ResizeObserver>
  );
};

const FormBuilder = React.forwardRef(
  <T extends Obj>({ children, ...rest }: IProps, ref: React.Ref<IFormExtendType<T>>) => {
    const [form] = Form.useForm() as [IFormExtendType<T>];

    React.useImperativeHandle(ref, () => form);

    return (
      <PureFormBuilder {...rest} form={form}>
        {children}
      </PureFormBuilder>
    );
  },
);

export { PureFormBuilder, FormBuilder };
