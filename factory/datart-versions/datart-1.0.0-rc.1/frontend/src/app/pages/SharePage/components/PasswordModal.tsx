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

import { Button, Form, FormInstance, Input, Modal } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { FC, memo, useRef } from 'react';

const INPUT_PASSWORD_KEY = 'password-input';

const PasswordModal: FC<{
  visible: boolean;
  onChange: (password: string) => void;
}> = memo(({ visible, onChange }) => {
  const t = useI18NPrefix(`share.modal`);
  const tg = useI18NPrefix(`global`);
  const formInstance = useRef<FormInstance>(null);

  return (
    <Modal
      title={t('password')}
      visible={visible}
      closable={false}
      footer={[
        <Button
          onClick={() => {
            formInstance?.current
              ?.validateFields()
              .then(() => {
                const useInput = formInstance?.current?.getFieldsValue([
                  INPUT_PASSWORD_KEY,
                ]);
                onChange(useInput?.[INPUT_PASSWORD_KEY]);
              })
              .catch(info => {
                return Promise.reject();
              });
          }}
          type="primary"
        >
          {tg('button.ok')}
        </Button>,
      ]}
    >
      <Form ref={formInstance}>
        <Form.Item
          label={t('password')}
          name={INPUT_PASSWORD_KEY}
          rules={[{ required: true }]}
        >
          <Input.Password placeholder={t('enterPassword')} />
        </Form.Item>
      </Form>
    </Modal>
  );
});

export default PasswordModal;
