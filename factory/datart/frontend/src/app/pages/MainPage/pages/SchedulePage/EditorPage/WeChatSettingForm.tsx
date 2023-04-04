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

import { Checkbox, Col, Form, Input, InputNumber, Row } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { FC } from 'react';
import { WECHART_FILE_TYPE_OPTIONS } from '../constants';

interface WeChatSettingFormProps {}
export const WeChatSettingForm: FC<WeChatSettingFormProps> = () => {
  const t = useI18NPrefix('schedule.editor.weChatSettingForm');

  return (
    <>
      <Form.Item
        label={t('RobotWebhookAddress')}
        name="webHookUrl"
        rules={[
          { required: true, message: t('RobotWebhookAddressIsRequired') },
        ]}
      >
        <Input />
      </Form.Item>
      <Row>
        <Col span={12}>
          <Form.Item
            labelCol={{ span: 10 }}
            label={t('fileType')}
            name="type"
            rules={[{ required: true }]}
          >
            <Checkbox.Group options={WECHART_FILE_TYPE_OPTIONS} />
          </Form.Item>
        </Col>
        <Col span={12}>
          <div className="image_width_form_item_wrapper">
            <Form.Item
              label={t('picWidth')}
              labelCol={{ span: 10 }}
              name="imageWidth"
              rules={[{ required: true }]}
            >
              <InputNumber min={100} />
            </Form.Item>
            <span className="image_width_unit">{t('px')}</span>
          </div>
        </Col>
      </Row>
    </>
  );
};
