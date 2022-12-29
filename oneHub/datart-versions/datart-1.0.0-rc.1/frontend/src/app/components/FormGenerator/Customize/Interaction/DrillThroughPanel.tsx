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

import { Button, Form, Space } from 'antd';
import { ChartStyleConfig } from 'app/types/ChartConfig';
import { updateBy } from 'app/utils/mutation';
import { FC, memo, useState } from 'react';
import styled from 'styled-components/macro';
import { uuidv4 } from 'utils/utils';
import { ItemLayoutProps } from '../../types';
import { itemLayoutComparer } from '../../utils';
import RuleList from './RuleList';
import { DrillThroughSetting, InteractionRule } from './types';

const DrillThroughPanel: FC<ItemLayoutProps<ChartStyleConfig>> = memo(
  ({ ancestors, translate: t = title => title, data, onChange, context }) => {
    const [drillThroughRules, setDrillThroughRules] = useState<
      DrillThroughSetting['rules']
    >(data.value?.rules || []);

    const handleAddRule = () => {
      const newRules = (drillThroughRules || []).concat([
        {
          id: uuidv4(),
        },
      ]);
      handleDrillThroughSettingChange(newRules);
    };

    const handleDeleteRule = (id: string) => {
      const newRules = drillThroughRules?.filter(r => r.id !== id);
      handleDrillThroughSettingChange(newRules);
    };

    const handleUpdateRule = (id: string, prop: string, value: any) => {
      const updatorIndex = (drillThroughRules || []).findIndex(
        r => r.id === id,
      );
      if (updatorIndex > -1) {
        const newRules = updateBy(drillThroughRules, draft => {
          draft![updatorIndex][prop] = value;
        });
        handleDrillThroughSettingChange(newRules);
      }
    };

    const handleDrillThroughSettingChange = (newRules?: InteractionRule[]) => {
      let newSetting: DrillThroughSetting = {
        rules: drillThroughRules,
      };
      if (newRules) {
        newSetting.rules = [...newRules];
        setDrillThroughRules([...newRules]);
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
        >
          <Form.Item
            label={t('drillThrough.rule.title')}
            name="rule"
            tooltip={t('drillThrough.rule.tip')}
          >
            <Button type="link" onClick={handleAddRule}>
              {t('drillThrough.rule.addRule')}
            </Button>
            <RuleList
              vizs={context?.vizs}
              dataview={context?.dataview}
              rules={drillThroughRules}
              onRuleChange={handleUpdateRule}
              onDeleteRule={handleDeleteRule}
              translate={t}
            />
          </Form.Item>
        </Form>
      </StyledDrillThroughPanel>
    );
  },
  itemLayoutComparer,
);

export default DrillThroughPanel;

const StyledDrillThroughPanel = styled(Space)`
  width: 100%;

  .ant-form .ant-form-item:last-child {
    margin-bottom: 0;
  }
`;
