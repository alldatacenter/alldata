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

import { Form, Radio, Select, Space } from 'antd';
import { ChartStyleConfig } from 'app/types/ChartConfig';
import { FC, memo, useState } from 'react';
import styled from 'styled-components/macro';
import { isEmptyArray } from 'utils/object';
import { InteractionFieldMapper, InteractionMouseEvent } from '../../constants';
import { ItemLayoutProps } from '../../types';
import { itemLayoutComparer } from '../../utils';
import { ViewDetailSetting } from './types';

const ViewDetailPanel: FC<ItemLayoutProps<ChartStyleConfig>> = memo(
  ({ ancestors, translate: t = title => title, data, context, onChange }) => {
    const [event, setEvent] = useState<ViewDetailSetting['event']>(
      data.value?.event || InteractionMouseEvent.Left,
    );
    const [mapper, setMapper] = useState<ViewDetailSetting['mapper']>(
      data?.value?.mapper,
    );
    const [customFields, setCustomFields] = useState<string[]>(
      data?.value?.[InteractionFieldMapper.Customize] || [],
    );

    const handleViewDetailEventChange = e => {
      const event = e.target.value;
      handleViewDetailSettingChange(event);
    };

    const handleViewDetailMapperChange = e => {
      const mapper = e.target.value;
      handleViewDetailSettingChange(undefined, mapper);
    };

    const handleViewDetailCustomFieldsChange = values => {
      handleViewDetailSettingChange(undefined, undefined, values);
    };

    const handleViewDetailSettingChange = (
      newEvent?: InteractionMouseEvent,
      newMapper?: InteractionFieldMapper,
      customFields?: string[],
    ) => {
      let newSetting: ViewDetailSetting = {
        event: newEvent || event,
        mapper: newMapper || mapper,
        [InteractionFieldMapper.Customize]: customFields,
      };
      if (newEvent) {
        setEvent(newEvent);
      }
      if (newMapper) {
        setMapper(newMapper);
      }
      if (!isEmptyArray(customFields)) {
        setCustomFields(customFields!);
      }
      onChange?.(ancestors, newSetting, false);
    };

    return (
      <StyledDrillThroughPanel direction="vertical">
        <Form
          labelCol={{ offset: 2, span: 2 }}
          wrapperCol={{ span: 18 }}
          layout="horizontal"
          size="middle"
          initialValues={{ event, mapper, custom: customFields }}
        >
          <Form.Item label={t('viewDetail.event')} name="event">
            <Radio.Group onChange={handleViewDetailEventChange}>
              <Radio value={InteractionMouseEvent.Left}>
                {t('viewDetail.leftClick')}
              </Radio>
              <Radio value={InteractionMouseEvent.Right}>
                {t('viewDetail.rightClick')}
              </Radio>
            </Radio.Group>
          </Form.Item>
          <Form.Item label={t('viewDetail.field')} name="mapper">
            <Radio.Group onChange={handleViewDetailMapperChange}>
              <Radio value={InteractionFieldMapper.All}>
                {t('viewDetail.all')}
              </Radio>
              <Radio value={InteractionFieldMapper.Customize}>
                {t('viewDetail.customize')}
              </Radio>
            </Radio.Group>
          </Form.Item>
          {mapper === InteractionFieldMapper.Customize && (
            <Form.Item label=" " colon={false} name="custom">
              <Select
                mode="multiple"
                optionFilterProp="children"
                allowClear
                onChange={handleViewDetailCustomFieldsChange}
              >
                {context?.dataview?.meta
                  ?.flatMap(f => {
                    if (f.role === 'hierachy') {
                      return f.children || [];
                    }
                    return f;
                  })
                  ?.map(f => {
                    return (
                      <Select.Option value={f.name}>{f.name}</Select.Option>
                    );
                  })}
              </Select>
            </Form.Item>
          )}
        </Form>
      </StyledDrillThroughPanel>
    );
  },
  itemLayoutComparer,
);

export default ViewDetailPanel;

const StyledDrillThroughPanel = styled(Space)`
  width: 100%;
`;
