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

import { Form, Input } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { WidgetActionContext } from 'app/pages/DashBoardPage/components/ActionProvider/WidgetActionProvider';
import { Widget } from 'app/pages/DashBoardPage/types/widgetTypes';
import debounce from 'lodash/debounce';
import { FC, memo, useContext, useEffect, useMemo } from 'react';
import { isEmptyArray } from 'utils/object';
import { Group } from '../SettingPanel';

export const NameSet: FC<{ wid: string; name: string; boardVizs: Widget[] }> =
  memo(({ wid, name, boardVizs }) => {
    const { onUpdateWidgetConfigByKey } = useContext(WidgetActionContext);
    const t = useI18NPrefix(`viz.board.setting`);
    const [form] = Form.useForm();

    const boardAllWidgetNames = useMemo(() => {
      return (boardVizs || [])
        .filter(bvz => bvz?.id !== wid)
        .map(bvz => bvz?.config?.name)
        .filter(Boolean);
    }, [boardVizs, wid]);

    useEffect(() => {
      form.setFieldsValue({ widgetName: name });
    }, [form, name]);

    const handleOnFieldsChange = useMemo(
      () =>
        debounce(
          fields => {
            if (isEmptyArray(fields?.[0].errors) && fields?.[0].value) {
              onUpdateWidgetConfigByKey({
                wid: wid,
                key: 'name',
                val: fields?.[0].value,
              });
            }
          },
          300,
          {
            leading: false,
          },
        ),
      [onUpdateWidgetConfigByKey, wid],
    );

    return (
      <Group>
        <Form
          form={form}
          layout="vertical"
          initialValues={{ widgetName: name }}
          onFieldsChange={handleOnFieldsChange}
        >
          <Form.Item
            name="widgetName"
            label={t('widget') + t('title')}
            rules={[
              {
                required: true,
                message: t('requiredWidgetName'),
              },
              () => ({
                validator(_, value) {
                  if (
                    value &&
                    boardAllWidgetNames?.some(name => name === value)
                  ) {
                    return Promise.reject(new Error(t('duplicateWidgetName')));
                  } else {
                    return Promise.resolve();
                  }
                },
              }),
            ]}
          >
            <Input className="datart-ant-input" />
          </Form.Item>
        </Form>
      </Group>
    );
  });
