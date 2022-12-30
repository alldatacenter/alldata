/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Checkbox, Form, FormInstance, Select } from 'antd';
import { ModalForm, ModalFormProps } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { User } from 'app/slice/types';
import { DEFAULT_DEBOUNCE_WAIT } from 'globalConstants';
import debounce from 'lodash/debounce';
import { memo, ReactNode, useCallback, useMemo, useRef, useState } from 'react';
import { request2 } from 'utils/request';

interface ValueType {
  key?: string;
  label: ReactNode;
  value: string | number;
}

export const InviteForm = memo(
  ({ formProps, afterClose, ...modalProps }: ModalFormProps) => {
    const [options, setOptions] = useState<ValueType[]>([]);
    const formRef = useRef<FormInstance>();
    const t = useI18NPrefix('member.form');
    const tgv = useI18NPrefix('global.validation');

    const debouncedSearchUser = useMemo(() => {
      const searchUser = async (val: string) => {
        if (!val.trim()) {
          setOptions([]);
        } else {
          const { data } = await request2<User[]>(
            `/users/search?keyword=${val}`,
          );
          setOptions(
            data.map(({ email, username, name }) => ({
              key: username,
              value: email,
              label: `${name ? `[${name}]` : ''}${email}`,
            })),
          );
        }
      };
      return debounce(searchUser, DEFAULT_DEBOUNCE_WAIT);
    }, []);

    const filterOption = useCallback((keywords: string, option) => {
      const { key, value, label } = option;
      return (
        key?.includes(keywords) ||
        value.toString().includes(keywords) ||
        label?.toString().includes(keywords)
      );
    }, []);

    const onAfterClose = useCallback(() => {
      formRef.current?.resetFields();
      setOptions([]);
      afterClose && afterClose();
    }, [afterClose]);

    return (
      <ModalForm
        formProps={formProps}
        {...modalProps}
        afterClose={onAfterClose}
        ref={formRef}
      >
        <Form.Item
          name="emails"
          rules={[
            { required: true, message: `${t('email')}${tgv('required')}` },
          ]}
        >
          <Select<ValueType>
            mode="tags"
            placeholder={t('search')}
            options={options}
            filterOption={filterOption}
            onSearch={debouncedSearchUser}
          />
        </Form.Item>
        <Form.Item name="sendMail" valuePropName="checked" initialValue={true}>
          <Checkbox>{t('needConfirm')}</Checkbox>
        </Form.Item>
      </ModalForm>
    );
  },
);
