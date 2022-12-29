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

import { Form, Radio, Space } from 'antd';
import { ChartStyleConfig } from 'app/types/ChartConfig';
import { updateBy } from 'app/utils/mutation';
import { FC, memo, useState } from 'react';
import styled from 'styled-components/macro';
import { InteractionMouseEvent } from '../../constants';
import { ItemLayoutProps } from '../../types';
import { itemLayoutComparer } from '../../utils';
import CrossFilteringRuleList from './CrossFilteringRuleList';
import { CrossFilteringInteractionRule, CrossFilteringSetting } from './types';

const CrossFilteringPanel: FC<ItemLayoutProps<ChartStyleConfig>> = memo(
  ({ ancestors, translate: t = title => title, data, onChange, context }) => {
    const [event, setEvent] = useState<CrossFilteringSetting['event']>(
      data.value?.event || InteractionMouseEvent.Left,
    );
    const [rules, setRules] = useState<CrossFilteringSetting['rules']>(
      data.value?.rules || [],
    );

    const handleEventChange = event => {
      handleSettingChange(event);
    };

    const handleSelectedRules = newRules => {
      handleSettingChange(undefined, newRules);
    };

    const handleUpdateRule = (id: string, prop: string, value: any) => {
      const updatorIndex = (rules || []).findIndex(r => r.id === id);
      if (updatorIndex > -1) {
        const newRules = updateBy(rules, draft => {
          draft![updatorIndex][prop] = value;
        });
        handleSettingChange(undefined, newRules);
      }
    };

    const handleSettingChange = (
      newEvent?: InteractionMouseEvent,
      newRules?: CrossFilteringInteractionRule[],
    ) => {
      let newSetting: CrossFilteringSetting = {
        event,
        rules,
      };
      if (newEvent) {
        newSetting.event = newEvent;
        setEvent(event);
      }
      if (newRules) {
        newSetting.rules = [...newRules];
        setRules([...newRules]);
      }
      onChange?.(ancestors, newSetting, false);
    };

    return (
      <StyledCrossFilteringPanel direction="vertical">
        <Form
          labelCol={{ offset: 2, span: 2 }}
          wrapperCol={{ span: 18 }}
          layout="horizontal"
          size="middle"
          initialValues={{ event }}
        >
          <Form.Item label={t('crossFiltering.event.title')} name="event">
            <Radio.Group
              options={[
                {
                  label: t('crossFiltering.event.left'),
                  value: InteractionMouseEvent.Left,
                },
                {
                  label: t('crossFiltering.event.right'),
                  value: InteractionMouseEvent.Right,
                },
              ]}
              onChange={e => handleEventChange(e.target.value)}
            />
          </Form.Item>
          <Form.Item label={t('crossFiltering.rule.title')} name="rule">
            <CrossFilteringRuleList
              widgetId={context?.widgetId}
              boardVizs={context?.boardVizs}
              dataview={context?.dataview}
              rules={rules}
              onRuleChange={handleUpdateRule}
              onSelectedRules={handleSelectedRules}
              translate={t}
            />
          </Form.Item>
        </Form>
      </StyledCrossFilteringPanel>
    );
  },
  itemLayoutComparer,
);

export default CrossFilteringPanel;

const StyledCrossFilteringPanel = styled(Space)`
  width: 100%;

  .ant-form .ant-form-item:last-child {
    margin-bottom: 0;
  }
`;
