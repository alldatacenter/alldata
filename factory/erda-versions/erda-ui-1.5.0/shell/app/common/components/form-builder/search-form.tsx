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
import { FormBuilder, IFormExtendType } from './form-builder';
import { Fields, IFieldType } from './fields';
import { map } from 'lodash';
import { Button } from 'antd';
import i18n from 'i18n';
import './search-form.scss';

/* *
 * SearchForm is a form with search and reset buttons.
 * fields: fields in Fields
 * actions: buttons you wanna render, default: search button and reset button,
 * onSubmit and onReset: click action of search button and reset button
 * */
interface IProps {
  fields: IFieldType[];
  actions?: React.ReactElement[];
  onReset?: (value: Obj) => void;
  onSubmit?: (value: Obj) => void;
  className?: string;
}

const SearchForm = ({ fields, actions, onReset, onSubmit, className }: IProps) => {
  const searchFormRef = React.useRef<IFormExtendType>(null);

  const handleSubmit = () => {
    const formRef = searchFormRef.current;
    onSubmit?.(formRef?.getFieldsValue());
  };

  const handleReset = () => {
    const formRef = searchFormRef.current;
    onReset?.(formRef?.getFieldsValue());

    setTimeout(() => {
      formRef?.resetFields();
    });
  };

  const actionFields = [
    {
      wrapperClassName: 'flex-1 max-w-full flex justify-end items-end',
      required: false,
      isHoldLabel: false,
      readonly: {
        className: 'text-right pt-2 overflow-hidden',
        renderItem: (
          <>
            {actions ? (
              map(actions, (action, idx) => {
                return (
                  <span key={idx} className="ml-2 first:ml-0">
                    {action}
                  </span>
                );
              })
            ) : (
              <>
                <Button onClick={handleReset} type="ghost">
                  {i18n.t('reset')}
                </Button>
                <Button className="ml-2" onClick={handleSubmit} type="primary" ghost>
                  {i18n.t('dop:search')}
                </Button>
              </>
            )}
          </>
        ),
      },
    },
  ];

  const realFields = React.useMemo(() => {
    return map(fields, (field) => {
      return {
        required: false,
        ...field,
      };
    });
  }, [fields]);

  return (
    <FormBuilder isMultiColumn ref={searchFormRef} size="small" className={`${className} erda-search-form`}>
      <Fields fields={realFields.concat(actionFields)} />
    </FormBuilder>
  );
};

export default SearchForm;
